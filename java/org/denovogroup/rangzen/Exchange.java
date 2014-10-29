/*
 * Copyright (c) 2014, De Novo Group
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its
 * contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.denovogroup.rangzen;

import com.squareup.wire.Wire;
import com.squareup.wire.Message;

import android.os.AsyncTask;
import android.util.Log;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Performs a single exchange between Rangzen peers as an AsyncTask.
 */
public class Exchange extends AsyncTask<Boolean, Integer, Exchange.Status>{
  /** Store of friends to use in this exchange. */
  private FriendStore friendStore;
  /** Store of messages to use in this exchange. */
  private MessageStore messageStore;
  /** Input stream connected to the remote communication partner. */
  private InputStream in;
  /** Output stream connected to the remote communication partner. */
  private OutputStream out;
  /** A callback to report the result of an exchange. */
  private ExchangeCallback callback;
  /** 
   * Whether to start the exchange with the first message or wait for the other side
   * to begin the exchange.
   */
  private boolean asInitiator;
  
  /** The number of friends in common with the remote peer. */
  private int commonFriends = -1;

  /** Messages received from remote party. */
  private CleartextMessages mMessagesReceived;

  /** Friends received from remote party. */
  private CleartextFriends mFriendsReceived;

  /** Send up to this many messages (top priority) from the message store. */
  private static final int NUM_MESSAGES_TO_SEND = 100;

  /** Enum indicating status of the Exchange. */
  enum Status {
    IN_PROGRESS,
    SUCCESS,
    ERROR
  }
  /**
   * Whether the exchange has completed successfully. Starts false and remains
   * false until the exchange completes, if it ever does. Remains true thereafter.
   * Set in doInBackground and checked in onPostExecute().
   */
  private Status status = Status.IN_PROGRESS;

  /** 
   * An error message, if any, explaning why the exchange isn't successful.
   * Set to null upon success.
   */
  private String errorMessage = "Not yet complete.";

  /** Included with Android log messages. */
  private static final String TAG = "Exchange";

  /** Synchronized getter for status. */
  private synchronized Status getExchangeStatus() {
    return status;
  }
  /** Synchronized setter for status. */
  private synchronized void setExchangeStatus(Status status) {
    this.status = status;
  }

  /**
   * Create a new exchange which will communicate over the given Input/Output
   * streams and use the given context to access storage for messages/friends.
   *
   * @param in An input stream which delivers a stream of data from the remote peer.
   * @param in An output stream which delivers a stream of data to the remote peer.
   * @param friendStore A store of friends to use in the friend-exchange protocol.
   * @param messageStore A store of messages to exchange with the remote peer.
   */
  public Exchange(InputStream in, OutputStream out, boolean asInitiator, 
                  FriendStore friendStore, MessageStore messageStore, 
                  ExchangeCallback callback) throws IllegalArgumentException {
    this.in = in;
    this.out = out;
    this.friendStore = friendStore;
    this.messageStore = messageStore;
    this.asInitiator = asInitiator;
    this.callback = callback;

    // TODO(lerner): Probalby best to throw exceptions here, since these are fatal.
    // There's no point in trying to have an exchange without someone to talk to
    // or friends/messages to talk about.
    
    if (in == null) {
      throw new IllegalArgumentException("Input stream for exchange is null.");
    }
    if (out == null) {
      throw new IllegalArgumentException("Output stream for exchange is null.");
    }
    if (friendStore == null) {
      throw new IllegalArgumentException("Friend store for exchange is null.");
    }
    if (messageStore == null) {
      throw new IllegalArgumentException("Message store for exchange is null.");
    }
    if (callback == null) {
      // I log this as a warning because not providing callbacks is a thing, it's
      // just an illogical thing here in all likelihood.
      // But I throw an exception because it simply isn't reasonable to pass null
      // unless we change the architecture of exchanges.
      Log.w(TAG, "No callback provided for exchange - nothing would happen locally!");
      throw new IllegalArgumentException("No callback provided for exchange.");
    }
  }

  /**
   * Get friends from the FriendStore, encode them as a CleartextFriends protobuf
   * object, and write that Message out to the output stream.
   *
   * TODO(lerner): Limit the number of friends used.
   */
  private void sendFriends() {
    List<String> friends = new ArrayList<String>();
    friends.addAll(friendStore.getAllFriends());
    CleartextFriends friendsMessage = new CleartextFriends.Builder()
                                                          .friends(friends)
                                                          .build();
    lengthValueWrite(out, friendsMessage); 
  }

  /**
   * Get messages from the MessageSTore, encode them as a CleartextMessages protobuf
   * object, and write that Message out to the output stream.
   */
  private void sendMessages() {
    // TODO(lerner): This is really ugly. I think we should add a Message protobuf
    // that is independent of CleartextMessages and have MessageStore return those.
    List<CleartextMessages.RangzenMessage> messages = 
                      new ArrayList<CleartextMessages.RangzenMessage>();
    for (int k=0; k<NUM_MESSAGES_TO_SEND; k++) {
      MessageStore.Message messageFromStore = messageStore.getKthMessage(k);
      if (messageFromStore == null) {
        break;
      }
      CleartextMessages.RangzenMessage messageForProtobuf;
      messageForProtobuf = new CleartextMessages.RangzenMessage
        .Builder()
        .text(messageFromStore.getMessage())
        .priority((double) messageFromStore.getPriority())
        .build();
      messages.add(messageForProtobuf);
    }
    CleartextMessages messagesMessage = new CleartextMessages.Builder()
                                                             .messages(messages)
                                                             .build();
    lengthValueWrite(out, messagesMessage); 
  }

  /**
   * Receive friends from the remote device.
   */
  private void receiveFriends() {
    CleartextFriends mFriendsReceived = lengthValueRead(in, CleartextFriends.class);
    this.mFriendsReceived = mFriendsReceived;
  }

  /**
   * Receive messages from the remote device.
   */
  private void receiveMessages() {
    CleartextMessages mMessagesReceived = lengthValueRead(in, CleartextMessages.class);
    this.mMessagesReceived = mMessagesReceived;
  }

  @Override
  protected Status doInBackground(Boolean... UNUSED) {
    // In this version of the exchange there's no crypto, so the messages don't
    // depend on each other at all.
    if (asInitiator) {
      sendFriends();
      sendMessages();
      receiveFriends();
      receiveMessages();
    } else {
      receiveFriends();
      receiveMessages();
      sendFriends();
      sendMessages();
    }
    setExchangeStatus(Status.SUCCESS);
    return status; // onPostExecute will now be called().
  }

  /**
   * Intended to provide updates on the progress of an asynchronous task.
   * Unimplemented.
   *
   * TODO(lerner): Implement this for the sake of internals (user probably doesn't
   * need to see a progress bar or anything).
   */
  @Override
  protected void onProgressUpdate(Integer... progress) { 
    // Unimplemented.
  }

  /**
   * After the exchange is complete, whether success or failure, this method handles
   * calling back to the callback with the results.
   */
  @Override
  protected void onPostExecute(Status success) {
    if (callback == null) {
      Log.w(TAG, "No callback provided to exchange.");
      return;
    }
    if (getExchangeStatus() == Status.SUCCESS) {
      callback.success(this);
      return;

    } else {
      callback.failure(this, errorMessage);
      return;
    }
  }

  /**
   * Get the number of friends in common with the other peer in the exchange.
   * -1 if the number of common friends is not yet known.
   *
   * Returns -1 until the exchange has completed, since it seems to me that exchanges
   * should probably be viewed as atomic by outside code.
   *
   * @return The number of friends in common with the remote peer, or -1 if the
   * number is not yet known because the exchage hasn't completed yet or has failed.
   */
  public int getCommonFriends() {
    if (getExchangeStatus() == Status.SUCCESS) {
      return commonFriends;
    } else {
      return -1;
    }
  }

  /**
   * Get the messages we received from the remote peer. 
   *
   * Returns null until the exchange has completed, since it seems to me that exchanges
   * should probably be viewed as atomic by outside code.
   *
   * TODO(lerner): Are there any attacks involving giving multiple copies of the
   * same message? Do we take the one with the highest or lowest priority? Or 
   * abandon the exchange?
   *
   * @return The set of messages received from the remote peer, or null if we
   * the exchange hasn't completed yet or the exchange failed.
   */ 
  public CleartextMessages getReceivedMessages() {
    if (getExchangeStatus() == Status.SUCCESS) {
      return mMessagesReceived;
    } else {
      return null;
    }
  }

  /**
   * Get the friends we received from the remote peer. 
   *
   * Returns null until the exchange has completed, since it seems to me that exchanges
   * should probably be viewed as atomic by outside code.
   *
   * TODO(lerner): This is only a thing with cleartext friends. Later, with crypto
   * there's no way to know who the friends are.
   *
   * @return The set of friends received from the remote peer, or null if we
   * the exchange hasn't completed yet or the exchange failed.
   */ 
  public CleartextFriends getReceivedFriends() {
    if (getExchangeStatus() == Status.SUCCESS) {
      return mFriendsReceived;
    } else {
      return null;
    }
  }

  /** Instance of Wire to encode/decode messages. */
  private static Wire wire = new Wire();

  /**
   * Take a Wire protobuf Message and encode it in a byte[] as:
   *
   * [length, value]
   *
   * where length is the length in bytes encoded as a 4 byte integer
   * and value are the bytes produced by Message.toByteArray().
   * 
   * @param m A message to encode in length/value format.
   * @return A ByteBuffer containing the encoded bytes of the message and its length.
   */
  /* package */ static ByteBuffer lengthValueEncode(Message m) {
    byte[] value = m.toByteArray();

    ByteBuffer encoded = ByteBuffer.allocate(Integer.SIZE/Byte.SIZE + value.length);
    encoded.order(ByteOrder.BIG_ENDIAN);   // Network byte order.
    encoded.putInt(value.length);
    encoded.put(value);

    return encoded;
  }

  /**
   * Send the given message, encoded as length-value, on the given output stream.
   *
   * @param outputStream The output stream to write the Message to.
   * @param m A message to write.
   * @return True if the write succeeds, false otherwise.
   */
  public static boolean lengthValueWrite(OutputStream outputStream, Message m) {
    if (outputStream == null || m == null) {
      return false;
    }
    try {
      byte[] encodedMessage = Exchange.lengthValueEncode(m).array();
      outputStream.write(encodedMessage);
      return true;
    } catch (IOException e) {
      Log.e(TAG, "Length/value write failed with exception: " + e);
      return false;
    }
  }

  /**
   * Decode a Message of the given type from the input stream and return it.
   * This method is much like a delimited read from Google's Protobufs.
   *
   * @param inputStream An input stream to read a Message from.
   * @param messageClass The type of Message to read.
   * @return The message recovered from the stream, or null if an error occurs.
   */
  public static <T extends Message> T lengthValueRead(InputStream inputStream, 
                                                      Class<T> messageClass) {
    int length = popLength(inputStream);
    if (length < 0) {
      return null;
    }
    byte[] messageBytes = new byte[length];
    T recoveredMessage;
    try {
      inputStream.read(messageBytes);
      recoveredMessage = wire.parseFrom(messageBytes, messageClass);
    } catch (IOException e) {
      Log.e(TAG, "IOException parsing message bytes: " + e);
      return null;
    }
    return recoveredMessage;
  }

  /**
   * Take the output of lengthValueEncode() and decode it to a Message of the
   * given type.
   */
  /* package */ static int popLength(InputStream stream) {
    byte[] lengthBytes = new byte[Integer.SIZE/Byte.SIZE];
    try {
      stream.read(lengthBytes);
    } catch (IOException e) {
      Log.e(TAG, "IOException popping length from input stream: " + e);
      return -1;
    }
    ByteBuffer buffer = ByteBuffer.wrap(lengthBytes);
    buffer.order(ByteOrder.BIG_ENDIAN);   // Network byte order.
    return buffer.getInt();
  }
}
