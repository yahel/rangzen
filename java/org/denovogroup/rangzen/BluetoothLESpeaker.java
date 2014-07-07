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

import android.R;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAdapter.LeScanCallback;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothProfile.ServiceListener;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.os.Looper;
import android.os.Parcelable;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.lang.StringBuilder;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;

/**
 * This class handles using Bluetooth Low Energy to send and receive messages
 * from nearby peers.
 */
public class BluetoothLESpeaker extends BroadcastReceiver {
  /** A string to send as the contents of a ping. */
  public static final String PING_STRING = "ping";

  /** A counter of number of pings sent. */
  private int pingCount = 0;

  /** Constant int passed to request to enable Bluetooth, required by Android. */
  public static final int REQUEST_ENABLE_BT = 54321;

  /** UUID for the Rangzen Bluetooth Low Energy Service. */
  public static final UUID SERVICE_UUID = UUID.fromString("53aaedf2-9a7e-4c72-81c7-e2f6ccb6d730");
  
  /** 
   * A default int value to be returned when getIntExtra fails to find
   * the requested key.
   */
  public static final int DEFAULT_EXTRA_INT = -1;

  /** 
   * An enum designating possible states (disconnected, connecting, connected)
   * that the speaker can be in.
   */
  public enum ConnectionState {
    NOT_CONNECTED, 
    CONNECTION_IN_PROGRESS,
    CONNECTED
  }

  /** A handle, retrieved from the OS, to the Wifi Direct framework. */
  private BluetoothAdapter mBluetoothAdapter;

  /** A handle to a Bluetooth Manager, which is a system service. */
  private BluetoothManager mBluetoothManager;

  

  /** Object representing the Bluetooth LE Gatt Service we advertsie for Rangzen. */
  private static final int SERVICE_TYPE = BluetoothGattService.SERVICE_TYPE_PRIMARY;
  private static final BluetoothGattService sGattService = new BluetoothGattService(SERVICE_UUID, 
                                                                                   SERVICE_TYPE);

  /** BT Profile Proxy for GattServer profile. */
  private static BluetoothGattServer mGattServerProxy;

  /** Context of the Rangzen Service. */
  private Context context;

  /** Rangzen Peer Manager handle. */
  private PeerManager mPeerManager;

  /** Queue of messages per Peer. */
  private Map<Peer, Queue<String>> messageQueues = new HashMap<Peer, Queue<String>>();

  /** Peer device we're attempting to speak with; null if nobody is selected. */
  private Peer selectedPeer = null;

  /** Logging tag, attached to all Android log messages. */
  private static final String TAG = "BluetoothSpeaker";

  /**
   * Create a new BluetoothLESpeaker.
   *
   * @param context The context to be used to fetch system resources.
   * @param peerManager The system's PeerManager instance.
   */
  public BluetoothLESpeaker(Context context, PeerManager peerManager) {
    super();

    // TODO(lerner): Figure out which context we should be using. I don't
    // understand which context is the appropriate context to be using: either
    // the application context, the main activity's context, or the
    // RangzenService's context. Most likely the correct answer is the Rangzen
    // Service's context, since we're instantiated by the RangzenService.
    this.context = context;

    this.mPeerManager = peerManager;


    // Register WifiDirectSpeaker to receive various events from the OS 
    // Wifi Direct subsystem.
    IntentFilter intentFilter = new IntentFilter();
    intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
    intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
    context.registerReceiver(this, intentFilter);


    mBluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
    mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    if (mBluetoothAdapter == null) {
      // Device does not support Bluetooth
      Log.e(TAG, "Device doesn't support Bluetooth.");
      return;
    } else if (!mBluetoothAdapter.isEnabled()) {
       // TODO(lerner): This is contrary to Android user experience, which
       // states that apps should never turn on Bluetooth without explicit
       // user interaction. We should instead prompt the user with the
       // Android built-in intent for asking to turn on Bluetooth.
      mBluetoothAdapter.enable();
    }

    // This method always seems to return false in my experience, and a forum
    // post at https://code.google.com/p/android/issues/detail?id=58582 suggests
    // it might not work at all?
    // // Ask for a GattServer proxy object.
    // if (mBluetoothAdapter.getProfileProxy(context, mProxyReceiver, BluetoothProfile.GATT_SERVER)) {
    //   Log.d(TAG, "Asked successfully for profile proxy for GATT_SERVER.");
    // } else {
    //   Log.d(TAG, "Asked unsuccessfully for profile proxy for GATT_SERVER.");
    // }

    // Another possible method of attempting to run a Gatt Server.
    mGattServerProxy = mBluetoothManager.openGattServer(context, mGattServerCallback);
    if (mGattServerProxy != null) {
      Log.i(TAG, "Opened Gatt Server, adding service.");
      mGattServerProxy.addService(sGattService);
    } else {
      Log.e(TAG, "Opened Gatt Server but got back a null server proxy object.");
    }

    // mBluetoothAdapter.startLeScan(mLeScanCallback);
    //
    Log.d(TAG, "Finished creating WifiDirectSpeaker.");
  }

  private BluetoothGattServerCallback mGattServerCallback = new BluetoothGattServerCallback() {
    @Override
    public void onServiceAdded(int status, BluetoothGattService service) {
      if (status == BluetoothGatt.GATT_SUCCESS) { 
        Log.i(TAG, "Successfully added Gatt Service!");
      } else if (status == BluetoothGatt.GATT_FAILURE) {
        Log.e(TAG, "Failure adding Gatt Service!");
      } else {
        Log.e(TAG, "Status code not GATT_SUCCESS or GATT_FAILURE adding Gatt Service: " + status);
      }
    }
  };

  private ServiceListener mProxyReceiver = new ServiceListener() {
    @Override
    public void onServiceConnected(int profile, BluetoothProfile proxy) {
      if (profile == BluetoothProfile.GATT_SERVER) {
        mGattServerProxy = (BluetoothGattServer) proxy;
        Log.i(TAG, "GattServer proxy connected.");
    } else {
        Log.wtf(TAG, "Got a non GattServer proxy in BluetoothLE service listener.");
      }
    }

    @Override
    public void onServiceDisconnected(int profile) {
      if (profile == BluetoothProfile.GATT_SERVER) {
        mGattServerProxy = null;
        Log.i(TAG, "GattServer proxy disconnected.");
      } else {
        Log.wtf(TAG, "Informed of non-GattServer service disconnection in BTLE service listener.");
      }
    }
    
  };

  private boolean deviceSupportsBluetooth() {
    return mBluetoothAdapter != null;
  }

  private LeScanCallback mLeScanCallback = new LeScanCallback() {
    @Override
    public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
      Log.d(TAG, "Found a BluetoothDevice from LE scan: " + device);
    }
  };

  /**
   * Unimplemented (data will not actually be sent!).
   * TODO(lerner): Enqueue messages into per-device-queues and send them
   * when we're connected to those devices.
   *
   * Request that the given data be sent to the given peer device via Bluetooth LE.
   *
   * @param message The message to be sent.
   * @param destination The remote peer to whom to send the message.
   */
  public void send(String message, Peer destination) {
    // TODO(lerner): What happens if the destination is null? This works
    // but there's a queue for null now?
    // Queue<String> queue = getDeviceMessageQueue(destination);
    // queue.add(message);
  }

  /**
   * Get the message queue for the given peer device. Lazily creates an empty
   * queue if one does not yet exist.
   *
   * @param device The Peer whose queue will be retrieved.
   */
  private Queue getPeerMessageQueue(Peer device) {
    if (messageQueues.containsKey(device)) {
      return messageQueues.get(device);
    } else {
      Queue newQueue = new ArrayDeque<String>();
      messageQueues.put(device, newQueue);
      return newQueue;
    }
  }


  /**
   * Handler for the BluetoothAdapter.ACTION_STATE_CHANGED event.
   */
  private void onBluetoothActionStateChanged(Context context, Intent intent) {
    Log.d(TAG, "Bluetooth state changed.");
  }

  /**
   * Handle incoming messages about Bluetooth LE events by dispatching them
   * to different utility methods.
   *
   * @see android.content.BroadcastReceiver#onReceive(android.content.Context,
   * android.content.Intent)
   */
  @Override
  public void onReceive(Context context, Intent intent) {
    String action = intent.getAction();
    if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
      onBluetoothActionStateChanged(context, intent);
    } else {
      // TODO(lerner): This shouldn't happen, exception?
      Log.wtf(TAG, "Received an event we weren't expecting: " + action);
    }
  }

  /**
   * Determine the canonical Peer for a given device. The canonical Peer for
   * a device is the instance of Peer located in PeerManager's peer list.
   *
   * @param device The Wifi Direct device we want to learn the Peer for.
   * @return The canonical Peer instance for the peer reachable at the given 
   * device, or null if no such peer exists.
   */
  private Peer getCanonicalPeerByDevice(BluetoothDevice device) {
    // TODO(lerner): Awaits rebase over BTLEPeerNetwork addition.
    // return mPeerManager.getCanonicalPeer(new Peer(new BluetoothLEPeerNetwork(device)));
    return null;
  }

  /**
   * Return a peer device for which there are queued messages, or null if
   * all sending queues are empty.
   *
   * @return A Peer with outgoing messages queued, or null there aren't
   * any messages waiting.
   */
  private Peer getPeerDeviceWithQueuedMessages() {
    for (Peer peer : messageQueues.keySet()) {
      if (!getPeerMessageQueue(peer).isEmpty()) {
        return peer;
      }
    }
    return null;
  }

  public void tasks() {
  }

  /**
   * Select a peer as the desired communication partner.
   *
   * @param peer The peer to which the speaker should attempt to connect and
   * communicate with.
   */
  public void selectPeer(Peer peer) {
    selectedPeer = peer;
  }

  /**
   * Convert buffer to string. (Assumes nul termination, for now.)
   * TODO(lerner): Improve encoding.
   */
  private String bufferToString(ByteBuffer buf) {
    StringBuilder sb = new StringBuilder("");
    byte b;
    while ((b = buf.get()) != 0) {
      sb.append(b);
    }
    return sb.toString();
  }
}
