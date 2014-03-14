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

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.support.v4.content.LocalBroadcastManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 * Background service which listens for incoming messages and broadcasts
 * them to the rest of the app.
 */
public class MessageReceiveService extends Service {
  private DatagramSocket mListenSocket;

  private static final String TAG = "MessageReceiveService";
  public static final String LOCAL_INTERFACE_NAME = "10.0.2.15";
  public static final int MAX_DATAGRAM_LENGTH = 1500;

  /**
   * Called whenever the service is requested to start. Spawns
   * a new thread which listens for new messages.
   *
   * @param intent The intent passed to startService to start this service.
   * @param flags Flags about the request to start the service.
   * @param startid A unique integer representing this request to start.
   * @see android.app.Service
   */
  @Override
  public int onStartCommand(Intent intent, int flags, int startid) {
    
    // I think this might actually spawn multiple threads if the 
    // service is asked to start multiple times, which is probably not
    // the correct behavior. Not sure the right way to manage it
    // and make sure there is only ever one thread.
    // TODO(lerner): make sure we only have one receiving thread at a time
    Log.i(TAG, "starting message receive loop");
    Thread t = new Thread(new Runnable() {
      public void run() {
        receiveMessageLoop();
      }
    });
    t.start();

    // Ask Android to shut the service down if the app is killed.
    // This is probably not the correct behavior but handling the
    // app shutting down presumably requires some logic queueing up 
    // and saving the messages we receive, which will be implemented
    // later on.
    // TODO(lerner): manage the lifecycle of this service correctly
    return START_NOT_STICKY;
  }

  /**
   * Utility method that creates and returns a local listening UDP socket.
   *
   * @return A socket listening on the local interface, or null if exceptions
   * occurred.
   */
  private DatagramSocket getListeningSocket() {
    try {
      InetAddress localAddr = InetAddress.getByName(LOCAL_INTERFACE_NAME);
      int port = MessageSendIntentService.DESTINATION_PORT;
      return new DatagramSocket(port, localAddr);
    } catch (SocketException e) {
      Log.e(TAG, "Failed to open listening socket", e);
      return null;
    } catch (UnknownHostException e) {
      Log.e(TAG, "Couldn't resolve local interface name", e);
      return null;
    }
  }

  /**
   * A loop (to be run in a separate thread) which opens and listens on a 
   * socket for messages and then broadcasts their receipt to the rest of
   * the app.
   */
  private void receiveMessageLoop() {
    Log.i(TAG, "started message receive loop");

    mListenSocket = getListeningSocket();
    if (mListenSocket == null) {
      return;
    }

    // This never terminates. I suppose it should terminate whenever the app
    // no longer wants to listen for messages, but I'm not sure when that 
    // would be.
    // TODO(lerner): manage the lifecycle of this loop and its thread.
    while (true) {
      byte[] receiveBuffer = new byte[MAX_DATAGRAM_LENGTH];
      DatagramPacket receivedPacket =
              new DatagramPacket(receiveBuffer, receiveBuffer.length);
      try {
        mListenSocket.receive(receivedPacket);
        byte[] bytes = receivedPacket.getData();
        String packetJSONString = 
                new String(bytes, 0, receivedPacket.getLength());
        JSONObject packetJSONObject = new JSONObject(packetJSONString);
        String message = packetJSONObject.getString("message");
        Log.i(TAG, "received message: " + message);

        // Locally (within-app) broadcast receipt of message
        Intent broadcastIntent = new Intent(MainActivity.MESSAGE_RECEIVED);
        broadcastIntent.putExtra(MainActivity.MESSAGE_EXTRA, message);
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);
      } catch (IOException e) {
        Log.e(TAG, "IO problem while unpacking packets", e);
      } catch (JSONException e) {
        Log.e(TAG, "Payload of datagram wasn't legit JSON", e);
      }
    }
  }


  /**
   * This method has to be implemented on a service, but I haven't written
   * the service with binding in mind. Unsure what would happen if it were
   * used this way.
   *
   * @param intent The intent used to bind the service (passed to
   * Context.bindService(). Extras included in the intent will not be visible
   * here.
   * @return A communication channel to the service. This implementation just
   * returns null.
   * @see android.app.Service
   *
   */
  @Override
  public IBinder onBind(Intent intent) {
    // This service is not meant to be used through binding
    return null;
  }

}
