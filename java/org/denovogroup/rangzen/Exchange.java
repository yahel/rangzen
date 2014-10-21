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

import android.os.AsyncTask;
import android.util.Log;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
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
  /** Character stream that wraps the input stream. */
  private InputStreamReader inReader;
  /** Output stream connected to the remote communication partner. */
  private OutputStream out;
  /** A callback to report the result of an exchange. */
  private ExchangeCallback callback;
  /** 
   * Whether to start the exchange with the first message or wait for the other side
   * to begin the exchange.
   */
  private boolean asInitiator;

  /** The messages received from the remote peer. */
  private Set<MessageStore.Message> receivedMessages = null;
  
  /** The number of friends in common with the remote peer. */
  private int commonFriends = -1;

  /** First message received from Alice, if we're not the initiator. */
  private String firstMessageReceived;
  /** Message received from Bob, if we're the initiator. */
  private String secondMessageReceived;

  /** Messages used in demonstration of this class for testing. */
  /* package */ static final String FIRST_DEMO_MESSAGE  = "I'm the initiator";
  /* package */ static final String SECOND_DEMO_MESSAGE = "Not the initiator";

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
    this.inReader = new InputStreamReader(in);
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
   * Initiate the exchange (as Alice) by sending the first protocol message.
   */
  private void sendFirstMessage() throws IOException {
    // TODO(lerner): Send an actual message with friends and messages in it.
    out.write(FIRST_DEMO_MESSAGE.getBytes());
  }

  /**
   * Reply to Alice as Bob with the second protocol message.
   */
  private void sendSecondMessage() throws IOException {
    // TODO(lerner): Send an actual message with friends and messages in it.
    out.write(SECOND_DEMO_MESSAGE.getBytes());
  }

  /**
   * Wait for Alice's first message, parsing it when received.
   */
  private void waitForFirstMessage() throws IOException {
    // TODO(lerner): Use an actually reasonable message format, or implement
    // an RPC interface (might be too cumbersome).
    // TODO(lerner): Use a StringBuilder style thing here.
    String receivedSoFar = "";
    int charsReceived = 0;
    char[] received = new char[FIRST_DEMO_MESSAGE.length()];
    do {
      charsReceived += inReader.read(received);
      receivedSoFar += new String(received);
    } while (charsReceived != FIRST_DEMO_MESSAGE.length());
    Log.i(TAG, "Received " + charsReceived + " characters: " + receivedSoFar);
    firstMessageReceived = receivedSoFar;
  }

  /**
   * Wait for Bob's reply, parsing it when received. 
   */
  private void waitForSecondMessage() throws IOException {
    // TODO(lerner): Use an actually reasonable message format, or implement
    // an RPC interface (might be too cumbersome).
    // TODO(lerner): Use a StringBuilder style thing here.
    String receivedSoFar = "";
    int charsReceived = 0;
    char[] received = new char[SECOND_DEMO_MESSAGE.length()];
    do {
      charsReceived += inReader.read(received);
      receivedSoFar += new String(received);
    } while (charsReceived != SECOND_DEMO_MESSAGE.length());
    Log.i(TAG, "Received " + charsReceived + " characters: " + receivedSoFar);
    secondMessageReceived = receivedSoFar;
  }

  @Override
  protected Status doInBackground(Boolean... UNUSED) {
    // In this version of the exchange there's no crypto, so the messages don't
    // depend on each other at all.
    try {
      if (asInitiator) {
        sendFirstMessage();
        waitForSecondMessage();
      } else {
        waitForFirstMessage();
        sendSecondMessage();
      }
    } catch (IOException e) {
      Log.e(TAG, "Error having exchange: " + e);
      errorMessage = e.toString();
      setExchangeStatus(Status.ERROR);
      return status;
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
  public Set<MessageStore.Message> getReceivedMessages() {
    if (getExchangeStatus() == Status.SUCCESS) {
      return receivedMessages;
    } else {
      return null;
    }
  }
}
