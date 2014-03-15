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
package org.denovogroup.experimental;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import org.json.JSONObject;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

/**
 * A class that sends messages over UDP in the background
 * as an IntentService.
 */
public class MessageSendIntentService extends IntentService {
  public static final int DESTINATION_PORT = 51689;
  public static final String DESTINATION_ADDRESS = "10.0.2.2";

  public static final String TAG = "MessageSendIntentService";

  public MessageSendIntentService() {
    super("MessageSendIntentService");
  }

  /**
   * Called when this class is asked to handle an intent.
   * The message to be sent should be stored as a String extra
   * in the Intent under the key MESSAGE_TO_SEND.
   *
   * @param intent The Intent passed to startService() to invoke this class.
   * @see android.app.IntentService
   */
  @Override
  protected void onHandleIntent(Intent intent) {
    String message = intent.getStringExtra(MainActivity.MESSAGE_EXTRA);
    if (message == null) {
      message = "";
    }
    Log.d(TAG, "No message found in extras of intent passed to onHandleIntent");

    // We transmit the message as a JSON object consisting of a number of
    // key/value pairs. An example message might be the string:
    //
    // {"message": "this is the message content"}
    //
    // To do so, we create a Map with a single k/v pair, namedly:
    // key "message" -> value "this is the message content"
    // 
    // TODO(lerner): Create a class that represents protocol messages and
    // can serialize/deserialize itself to JSON/other formats. Use it here
    // and in message receive service.
    Map<String, String> keyValuePairs = new HashMap<String, String>();
    keyValuePairs.put("message", message);
    String jsonString = (new JSONObject(keyValuePairs)).toString();

    Log.i(TAG, "sending message " + message);

    DatagramSocket socket;
    try {
      socket = new DatagramSocket();
    } catch (SocketException e) {
      // Right now, if we can't create the socket, we just log the failure
      // and give up sending the message. This is not correct - presumably
      // the caller wants to know what happened, we might retry, etc.
      //
      // TODO(lerner): Creating socket failed, retry/notify/give up?
      Log.e(TAG, "Couldn't create socket.", e);
      return;
    }
    DatagramPacket packet = packDatagramPacket(jsonString,
                                               DESTINATION_ADDRESS,
                                               DESTINATION_PORT);
    try {
      socket.send(packet);
    } catch (IOException e) {
      // TODO(lerner): Sending packet failed, retry/notify/give up?
      Log.e(TAG, "Failure sending packet", e);
    }
  }

  /**
   * Utility method that creates a DatagramPacket with the given contents
   * addressed to the given destination host and port.
   *
   * @param payload  The payload of the packet, as a string.
   * @param dest     The destination address (hostname or IP).
   * @param destPort The destination port.
   * @return Returns a UDP packet with the given destination port,
   * destination host and payload, or null if the remote host couldn't
   * be resolved by DNS.
   */
  private DatagramPacket packDatagramPacket(String payload, 
                                            String destHost, 
                                            int destPort) {
    try {
      InetAddress destIP = InetAddress.getByName(destHost);
      int payloadLength = payload.length();
      byte[] payloadByteArray = payload.getBytes();
      DatagramPacket packet = new DatagramPacket(payloadByteArray, 
                                                 payloadLength,
                                                 destIP,
                                                 destPort);
      return packet;
    } catch (UnknownHostException e) {
      // DNS failed, shouldn't happen with raw IP
      Log.e(TAG, "Can't resolve hostname "+DESTINATION_ADDRESS, e);
      return null;
    }
  }

}
