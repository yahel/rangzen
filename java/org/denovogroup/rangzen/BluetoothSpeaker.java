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

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

/**
 * Handles interaction with Android's Bluetooth subsystem.
 */
public class BluetoothSpeaker {
  /** Logging tag, attached to all Android log messages. */
  private static final String TAG = "BluetoothSpeaker";

  /** The expected size of peer exchanges. */
  public static final int EXCHANGE_SIZE = 1024 * 30;

  /** Constant int passed to request to enable Bluetooth, required by Android. */
  public static final int REQUEST_ENABLE_BT = 54321;

  /** SDP name for creating Rangzen service on listening socket. */
  private static final String SDP_NAME = "RANGZEN_SDP_NAME";

  /** Payload for exchange. */
  private static byte[] mPayload;

  /** A UUID for this particular device. */
  private UUID mThisDeviceUUID;
  
  /** A handle, retrieved from the OS, to the Wifi Direct framework. */
  private BluetoothAdapter mBluetoothAdapter;

  /** A handle to a server socket which receives connections from remote BT peers. */
  private BluetoothServerSocket mServerSocket;

  /** Thread which calls accept on the server socket. */
  private Thread mConnectionAcceptingThread;

  /** Context of the Rangzen Service. */
  private Context mContext;

  /** Receives Bluetooth related broadcasts. */
  private BluetoothBroadcastReceiver mBluetoothBroadcastReceiver;

  /**
   * @param context A context, from which to access the Bluetooth subsystem.
   * @param peerManager The app's PeerManager instance.
   */
  public BluetoothSpeaker(Context context, PeerManager peerManager) {
    super();

    mPayload = new byte[EXCHANGE_SIZE];
    for (int i=0; i < EXCHANGE_SIZE; i++) {
      mPayload[i] = (byte) i;
    }

    this.mContext = context;
    this.mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    this.mBluetoothBroadcastReceiver = new BluetoothBroadcastReceiver(context);
    this.mThisDeviceUUID = getUUIDFromMACAddress(mBluetoothAdapter.getAddress());
    Log.i(TAG, "This device's UUID is " + mThisDeviceUUID.toString());

    if (mBluetoothAdapter == null) {
      // TODO (lerner): Tell the server that this device doesn't do Bluetooth.
      // TODO (lerner): Opt this device out of data collection?
      Log.e(TAG, "Device doesn't support Bluetooth.");
      return;
    } else if (!mBluetoothAdapter.isEnabled()) {
      Log.d(TAG, "Got a non-null Bluetooth Adapter but it's not enabled.");
       // TODO(lerner): This is contrary to Android user experience, which
       // states that apps should never turn on Bluetooth without explicit
       // user interaction. We should instead prompt the user with the
       // Android built-in intent for asking to turn on Bluetooth.
      if (mBluetoothAdapter.enable()) {
        Log.i(TAG, "Enabling Bluetooth.");
      } else {
        Log.e(TAG, "Attempt to enable Bluetooth returned false.");
      }

    } 
    if (mBluetoothAdapter.isEnabled()) {
      try { 
        createListeningSocket();
        spawnConnectionAcceptingThread();
      } catch (IOException e) {
        Log.e(TAG, "Failed to create listening BT server socket. " + e);
        Log.e(TAG, "Can't receive incoming connections.");
      }
    }

    Log.d(TAG, "Finished creating BluetoothSpeaker.");
  }

  /**
   * Retrieve a BluetoothDevice corresponding to the given address.
   *
   * @return A BluetoothDevice with the given address.
   */
  public BluetoothDevice getDevice(String address) {
    if (mBluetoothAdapter != null) {
      return mBluetoothAdapter.getRemoteDevice(address);
    } else {
      return null;
    }

  }
  /**
   * Retrieve our Bluetooth MAC address.
   *
   * @return The Bluetooth MAC address of this device.
   */
  public String getAddress() {
    if (mBluetoothAdapter != null) {
      return mBluetoothAdapter.getAddress();
    } else {
      return null;
    }

  }

  /**
   * Creates a thread which listens on the BluetoothSpeaker's
   * BluetoothServerSocket as long as Bluetooth reamins on, accepting any
   * incoming connections and completing exchanges with them.
   *
   * If Bluetooth is turned off (or an exception occurs for any other reason 
   * while accepting a new connection), the thread dies and will be restarted
   * later when Bluetooth is on again.
   */
  private void spawnConnectionAcceptingThread() {
    mConnectionAcceptingThread = new Thread() {
      @Override
      public void run() {
        while (true) {
          try {
            acceptConnection();
          } catch (IOException e) {
            Log.e(TAG, "IOException while accepting/responding to a connection" + e);
            if (!mBluetoothAdapter.isEnabled()) {
              Log.e(TAG, "Bluetooth adapter is disabled; not accepting connections.");
              mServerSocket = null;
              return;
            }
          }
        }
      }
    };
    mConnectionAcceptingThread.start();
  }
  
  /**
   * Create a listening Bluetooth socket and listen for Rangzen connections.
   */
  private void createListeningSocket() throws IOException {
      mServerSocket = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(SDP_NAME, mThisDeviceUUID);
      Log.i(TAG, String.format("Listening (insecure RFCOMM) - name <%s>, UUID <%s>.",
                               SDP_NAME, mThisDeviceUUID));
  }

    /**
   * Accept a connection to the BluetoothServerSocket we've set up.
   * @throws IOException
   */
  private void acceptConnection() throws IOException {
    if (mServerSocket == null) {
      throw new IOException("ServerSocket is null, not trying to accept().");
    } else if (!mBluetoothAdapter.isEnabled()) {
      throw new IOException("Bluetooth adapter is disabled, not trying to accept().");
    }
    Log.i(TAG, "Calling mServerSocket.accept()");
    BluetoothSocket socket = mServerSocket.accept();
    Log.i(TAG, "Accepted socket from " + socket.getRemoteDevice());
    Log.i(TAG, "Accepted socket connected? " + socket.isConnected());
    // If accept() returns, we're connected!

    receiveExchangeWithSocket(socket);
  }

  /**
   * Called periodically by the background tasks method of the Rangzen Service.
   * Recreates the listening socket and thread if they're not active. The listening
   * thread might be inactive if Bluetooth was turned off previously.
   */
  public void tasks() {
    Log.v(TAG, "Starting BluetoothSpeaker tasks.");
    if (mServerSocket == null         && mBluetoothAdapter != null &&
        mBluetoothAdapter.isEnabled() && !mConnectionAcceptingThread.isAlive()) {
      try { 
        Log.v(TAG, "No ServerSocket, creating a new one.");
        createListeningSocket();
        spawnConnectionAcceptingThread();
      } catch (IOException e) {
        Log.e(TAG, "Tasks: failed to create listening BT server socket. " + e);
        Log.e(TAG, "Can't receive incoming connections.");
      }
    }
  }

  /**
   * Convert buffer to string. 
   *
   * TODO(lerner): Improve encoding. Currently uses default encoding and 10 
   * bytes, whatever they are, ignoring any terminators.
   *
   * @return 10 bytes of the array, rendered as characters in the default encoding.
   */
  private String bufferToString(char[] charArray) {
    return new String(charArray, 0, 10);
  }

  /**
   * Equivalent to calling connectAndStartExchange with the MAC address
   * of the given peer.
   *
   * @param peer A peer, which must be a Bluetooth based peer, with whom we are
   * asked to have an exchange.
   * @throws IOException
   */
  public Exchange connectAndStartExchange(Peer peer) throws IOException {
    if (peer.getNetwork().getNetworkType() == PeerNetwork.BLUETOOTH_TYPE) {
      return connectAndStartExchange(peer.getNetwork().getBluetoothDevice());
    } else {
      Log.e(TAG, String.format("Can't connect to peer %s - not a BT Peer", peer));
      return null;
    }
  }

  /**
   * Immediately make an attempt to connect to a BluetoothDevice with the address
   * given. The UUID to connect to will be generated as a type-3 UUID from the
   * bytes of the MAC address given. If the connection is successful, immediately
   * attempts to have an exchange of data with the remote host.
   *
   * @param address The Bluetooth MAC of the device to connect to.
   * @throws IOException
   */
  public Exchange connectAndStartExchange(String address) throws IOException {
    Log.v(TAG, "Connecting to address " + address + "; converting to device");
    if (BluetoothAdapter.checkBluetoothAddress(address)) {
      BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
      Log.v(TAG, "Calling connect on device " + device);
      return connectAndStartExchange(device);
    } else {
      Log.e(TAG, "Invalid address " + address + " in connectToPeerDevice.");
      throw new IllegalArgumentException("Illegal address: " + address);
    }
  }
  
  /**
   * Connect to the given Bluetooth Device and have an exchange with it, if
   * successful.
   *
   * @param device The device to connect to.
   * @throws IOException 
   */
  private Exchange connectAndStartExchange(BluetoothDevice device) throws IOException {
    Log.i(TAG, "Attempting to connect to " + device);
    if (device == null) {
      Log.e(TAG, "Device is null in connectAndStartExchange, can't have exchange.");
      return null;
    }
    BluetoothSocket socket;
    UUID remoteUUID = getUUIDFromMACAddress(device.getAddress());
    socket = device.createInsecureRfcommSocketToServiceRecord(remoteUUID);
    Log.d(TAG, "Created a socket to " + device);

    // BluetoothSocket.connect() blocks until it connects, so we don't have
    // to worry about being in the middle of a connection attempt or anything.
    socket.connect();
    Log.i(TAG, "Connect returned! We are connected to " + device);
    Log.d(TAG, "connect()ing socket is connected? " + socket.isConnected());
    return startExchangeWithSocket(socket);
  }

  /**
   * Participate in a fake Rangzen exchange by sending data to an echo server
   * and expecting to receive it back.
   *
   * @param socket The socket over which to have the communication.
   */
  private Exchange startExchangeWithSocket(BluetoothSocket socket) throws IOException {
    byte[] received;
    OutputStream output = socket.getOutputStream();
    InputStream input = socket.getInputStream();
    if (socket.isConnected()) {
      Date startTime = new Date();

      Log.i(TAG, "Writing as connect()er in exchange");
      output.write(mPayload);
      
      Date firstSendingDoneTime = new Date();

      Log.i(TAG, "Listening for echo.");
      received = readExactlyNumberBytesFromStream(input, EXCHANGE_SIZE);

      Date echoReceivedTime = new Date();
      if (Arrays.equals(mPayload, received)) {
        Log.i(TAG, "Done reading, payload and received were equal.");
      } else {
        Log.e(TAG, "Done reading, payload and received were DIFFERENT.");
        throw new IOException("Payload and received bytes were different!");
      }

      Log.i(TAG, "Sending final message.");
      output.write(mPayload);

      socket.close();
      Date endTime = new Date();

      Log.i(TAG, String.format("Had exchange (I spoke first) with %s from %s to %s (%d ms elapsed).", 
                               socket.getRemoteDevice(), startTime, endTime, 
                               endTime.getTime() - startTime.getTime()));
      // TODO(lerner): Add the locations into this exchange before it's stored
      // at the server.
      return new Exchange(getAddress(), 
                          socket.getRemoteDevice().getAddress(),
                          Exchange.PROTOCOL_BLUETOOTH, 
                          startTime.getTime(),
                          endTime.getTime(),
                          null,
                          null);
    } else {
      Log.e(TAG, "We connected() but the socket isn't connected.");
      return null;
    }
  }

  /**
   * Participate in a fake Rangzen exchange by acting as an echo server for one
   * round of communciation.
   *
   * @param socket The socket over which to have the communication.
   */
  private void receiveExchangeWithSocket(BluetoothSocket socket) throws IOException {
    byte[] received;
    byte[] secondReceived;
    InputStream input = socket.getInputStream();
    OutputStream output = socket.getOutputStream();
    if (socket.isConnected()) {
      Date startTime = new Date();

      Log.i(TAG, "Listening for first message.");
      received = readExactlyNumberBytesFromStream(input, EXCHANGE_SIZE);
      if (Arrays.equals(mPayload, received)) {
        Log.i(TAG, "Done reading, payload and received were equal.");
      } else {
        Log.e(TAG, "Done reading, payload and received where DIFFERENT.");
      }

      Date firstReceiveDoneTime = new Date();

      Log.i(TAG, "Echoing data to remote in response.");
      output.write(mPayload);
      Date echoSentTime = new Date();

      Log.i(TAG, "Receiving second message.");
      secondReceived = readExactlyNumberBytesFromStream(input, EXCHANGE_SIZE);
      if (Arrays.equals(mPayload, received)) {
        Log.i(TAG, "Done reading, payload and received were equal.");
      } else {
        Log.e(TAG, "Done reading, payload and received where DIFFERENT.");
      }
      socket.close();
      Date endTime = new Date();

      Log.i(TAG, String.format("Had exchange (I echoed) with %s from %s to %s (%d ms elapsed).", 
                               socket.getRemoteDevice(), startTime, endTime, 
                               endTime.getTime() - startTime.getTime()));
    } else {
      Log.e(TAG, "We accepted a connection but the socket isn't connected.");
    }
  }

  /**
   * Read the given stream until exactly the requested number of bytes have been
   * received, then return the bytes.
   *
   * @param stream The input stream to read from.
   * @param count The number of bytes to read from the stream.
   * @return An array containing the bytes that were read.
   */ 
  private byte[] readExactlyNumberBytesFromStream(InputStream stream, int count) throws IOException {
    byte[] received = new byte[count];
    int bytesReceived = 0;
    while (bytesReceived != received.length) {
      bytesReceived += stream.read(received, bytesReceived, received.length - bytesReceived);
    }
    return received;
  }

  /**
   * Use the bytes of the given address to generate a type-3 UUID.
   *
   * @param address The MAC address to be converted into a UUID.
   * @return A UUID corresponding to the MAC address given.
   */
  private UUID getUUIDFromMACAddress(String address) {
    if (address == null) {
      return null;
    } else {
      return UUID.nameUUIDFromBytes(address.getBytes());
    }
  }
}
