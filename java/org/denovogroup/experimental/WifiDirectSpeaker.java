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

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ChannelListener;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

/**
 * This class communicates with the WifiP2pManager which Android exposes
 * in order to manage and communicate with Wifi Direct peers. It searches
 * for peers, manages connections to those peers, and sends and receives
 * packets from those peers. 
 */
public class WifiDirectSpeaker extends BroadcastReceiver {
  /** A handle, retrieved from the OS, to the Wifi Direct framework. */
  private WifiP2pManager manager;

  /** Communication link to the Wifi Direct framework. */
  private Channel channel;

  /** Rangzen peer manager instance. */
  private PeerManager mPeerManager;

  /** Context to retrieve a WifiP2pManager from the Wifi Direct subsystem. */
  private Context context;

  /** Queue of messages per Peer. */
  private Map<WifiP2pDevice, Queue<String>> messageQueues = 
          new HashMap<WifiP2pDevice, Queue<String>>();

  /** Peer device we're attempting to speak with; null if nobody is selected. */
  private WifiP2pDevice selectedPeerDevice;

  /** Most recent WifiP2p connection info object returned by WifiP2p subsystem. */
  private WifiP2pInfo currentConnectionInfo;

  /** 
   * Whether we're currently in a Wifi Direct network with another peer. 
   * This starts false and changes only when the WifiP2p framework fires a
   * connection info changed event (our callback for that event sets isConnected
   * appropriately.
   */
  private boolean isConnected = false;

  /** 
   * The looper that runs the onReceive() loop to handle Wifi Direct framework
   * events.
   */
  private Looper looper;

  /** Logging tag, attached to all Android log messages. */
  private static final String TAG = "WifiDirectSpeaker";

  /**
   * @param context A context, from which to access the Wifi Direct subsystem.
   */
  public WifiDirectSpeaker(Context context) {
    super();
    this.context = context;
    this.manager = (WifiP2pManager) context.getSystemService(Context.WIFI_P2P_SERVICE);
    // TODO(lerner): Create our own looper that doesn't run in the main thread.
    this.looper = context.getMainLooper();
    this.channel = manager.initialize(context, looper, mChannelListener);
    this.mPeerManager = PeerManager.getInstance(context);
    Log.d(TAG, "Initialized, listening");
  }

  /**
   * This method is called to trigger sending/receiving activities that are
   * initiated by the app, as opposed to those triggered by callbacks from
   * the OS. 
   *
   */

  /**
   * Request that the given data be sent to the given peer device via 
   * Wifi Direct.
   *
   * @param message The message to be sent.
   * @param destination The remote Wifi Direct peer to whom to send the message.
   */
  public void send(String message, WifiP2pDevice destination) {
    // TODO(lerner): What happens if the destination is null? This works
    // but there's a queue for null now?
    Queue<String> queue = getDeviceMessageQueue(destination);
    queue.add(message);
  }

  /**
   * Get the message queue for the given peer device. Lazily creates an empty
   * queue if one does not yet exist.
   *
   * @param device The Wifi Direct peer device whose queue will be retrieved.
   */
  private Queue getDeviceMessageQueue(WifiP2pDevice device) {
    if (messageQueues.containsKey(device)) {
      return messageQueues.get(device);
    } else {
      Queue newQueue = new ArrayDeque<String>();
      messageQueues.put(device, newQueue);
      return newQueue;
    }
  }

  /**
   * Called in a loop to receive packets from open sockets to connected peers.
   * When packets are received, they are forwarded to the appropriate
   * PeerNetwork object for the peer corresponding to the device that sent the
   * data.
   */
  private void receive() {
    // TODO(lerner): Select over open sockets.
    // TODO(lerner): Determine correct Peer/PeerNetwork for received packets.
  }

  /**
   * Receives events indicating whether Wifi Direct is enabled or disabled.
   */
  private void onWifiP2pStateChanged(Context context, Intent intent) {
    int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
    if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
      Log.d(TAG, "Wifi Direct enabled");
      // Wifi Direct mode is enabled
      // TODO(lerner): Do something since it's enabled?
    } else {
      Log.d(TAG, "Wifi Direct disabled");
      // Wifi Direct mode is disabled
      // TODO(lerner): Do something since it's disabled?
    }
  }

  /**
   * Called when the WifiP2pManager notifies the Speaker that new peers are
   * available. This method extracts the actual list of peers from the 
   * intent, creates or retrieves canonical Peer objects for each, and 
   * then adds those peers to the PeerManager.
   *
   * @param context Context passed to onReceive, forwarded to this method.
   * @param intent An intent containing the list of new Wifi Direct devices as
   * an extra.
   */
  private void onWifiP2pPeersChanged(Context context, Intent intent) {
    WifiP2pDeviceList peerDevices = (WifiP2pDeviceList)
            intent.getParcelableExtra(WifiP2pManager.EXTRA_P2P_DEVICE_LIST);
    for (WifiP2pDevice device : peerDevices.getDeviceList()) {
      Peer peer = getCanonicalPeerByDevice(device);
      mPeerManager.addPeer(peer);
    }
    Log.d(TAG, "P2P peers changed");
  }

  /**
   * Called when the status of a Wifi Direct connection with a peer changes.
   * Updates the speaker's information on connection status.
   */
  private void onWifiP2pConnectionChanged(Context context, Intent intent) {
    if (manager == null) {
      return;
    }

    NetworkInfo info = (NetworkInfo) 
            intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

    if (info.isConnected()) {
      // We are connected with the other device, request connection
      // info to find group owner IP
      // TODO(lerner): Do something when connection changes
      Log.d(TAG, "Wifi P2P connection changed, is connected");
      isConnected = true;
      manager.requestConnectionInfo(channel, mConnectionInfoListener);
    } else {
      // It's a disconnect
      // TODO(lerner): Dispatch a notification about this.
      isConnected = false;
      Log.d(TAG, "Wifi P2P disconnected");
      }
  }
  
  /**
   * TODO(lerner) Write here.
   */
  private void onWifiP2pThisDeviceChanged(Context context, Intent intent) {
    WifiP2pDevice changedDevice = (WifiP2pDevice) intent.getParcelableExtra(
        WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
    Log.d(TAG, "Wifi P2P this device changed action received");
  }

  /**
   * Handle incoming messages. This class only handles the four types of
   * broadcasts sent by WifiP2pManager. Dispatch messages to their appropriate
   * handlers.
   *
   * @see android.content.BroadcastReceiver#onReceive(android.content.Context,
   * android.content.Intent)
   */
  @Override
  public void onReceive(Context context, Intent intent) {
    String action = intent.getAction();
    if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
      onWifiP2pStateChanged(context, intent);
    } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
      onWifiP2pPeersChanged(context, intent);
    } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
      onWifiP2pConnectionChanged(context, intent);
    } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
      onWifiP2pThisDeviceChanged(context, intent);
    } else {
      // TODO(lerner): This shouldn't happen, exception?
    }
  }

  /**
   * Respond to events raised by loss of communication with the Wifi Direct
   * framework in the Android OS.
   */
  private ChannelListener mChannelListener = new ChannelListener() {
    @Override
    public void onChannelDisconnected() {
      Log.d(TAG, "Communication with WifiP2pManager framework lost!");
      // TODO(lerner): Respond to this fact with some ameliorating action.
    }
  };

  /**
   * Receives the response to a request for connection info from the Wifi
   * Direct framework in the Android OS.
   */
  private ConnectionInfoListener mConnectionInfoListener = new ConnectionInfoListener() {
    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo info) {
      Log.d(TAG, "Connection info available: " + info);
      // TODO(lerner): Store this connection info in the connection table.
      // TODO(lerner): Potentially trigger waiting network actions.
    }
  };

  /**
   * Receives requested peer list from the OS Wifi Direct framework and
   * forwards those peers to the PeerManager.
   */
  private PeerListListener mPeerListListener = new PeerListListener() {
    @Override
    public void onPeersAvailable(WifiP2pDeviceList peerDevices) {
      Log.d(TAG, "New wifi direct peer devices available" + peerDevices);
      // TODO(lerner): Transform these into Peer objects and forward them
      // to the PeerManager.
    }
  };

  /**
   * Provided when a request is made to the WifiP2pManager. Receives "yes"
   * or "no" notification from the framework about whether the command issued
   * has been successfully initiated.
   */
  // private WifiP2pManager.ActionListener 

  /**
   * Determine the canonical Peer for a given device. The canonical Peer for
   * a device is the instance of Peer located in PeerManager's peer list.
   *
   * @param device The Wifi Direct device we want to learn the Peer for.
   * @return The canonical Peer instance for the peer reachable at the given 
   * device, or null if no such peer exists.
   */
  private Peer getCanonicalPeerByDevice(WifiP2pDevice device) {
    return mPeerManager.getCanonicalPeer(new Peer(new PeerNetwork(device)));
  }

  /**
   * Return a peer device for which there are queued messages, or null if
   * all sending queues are empty.
   *
   * @return A WifiP2pDevice with outgoing messages queued, or null if none exists.
   */
  WifiP2pDevice getPeerDeviceWithQueuedMessages() {
    for (WifiP2pDevice device : messageQueues.keySet()) {
      if (!getDeviceMessageQueue(device).isEmpty()) {
        return device;
      }
    }
    return null;
  }

  /**
   * The "main loop" for WifiDirectSpeaker, in effect. Proceeds with the tasks
   * that must be performed by the Speaker, including connecting to peers for
   * whom outgoing messages are waiting, sending messages over sockets to peers
   * who are already connected, and selecting new peers to connect to.
   */
  public void sendTasks() {
    if (isConnected) {
      try {
        sendToCurrentPeerDevice();
      } catch (NoConnectedPeerException e) {
        Log.e(TAG, e.toString());
      } // TODO(lerner): Catch IO exceptions that might occur here?
    }
    
    // If there's a different peer with messages waiting, connect to that peer.
    // Otherwise, either we stay connected to the same peer or 
    WifiP2pDevice nextPeerDevice = getPeerDeviceWithQueuedMessages();
    if (nextPeerDevice == null) {
      disconnectFromCurrentPeer();
    } else if (nextPeerDevice.equals(selectedPeerDevice) {
      selectedPeerDevice = nextPeerDevice;
      connectToPeerDevice(nextPeerDevice);
    }
  }

  /**
   * Initiate a Wifi Direct connection to the given peer.
   */
  private void connectToPeerDevice(WifiP2pDevice device) {
    WifiP2pConfig config = new WifiP2pConfig();
    config.deviceAddress = device.deviceAddress;

    manager.connect(channel, config, new ActionListener() {
      @Override
      public void onSuccess() {
        // WiFiDirectBroadcastReceiver will notify us. Ignore for now.
      }

      @Override
      public void onFailure(int reason) {
        Log.d(TAG, "Connect failed. " + reason);
        // Toast.makeText(WiFiDirectActivity.this, "Connect failed." + reason,
        //   Toast.LENGTH_SHORT).show();
      }
    });
  }

  /**
   * Open a socket and send currently queued messages to the currently connected
   * peer. If no peer is currently connected, raise an exception.
   *
   * @return The number of messages sent.
   */
  private int sendToCurrentPeerDevice() throws NoConnectedPeerException {
    if (!isConnected) {
      throw new NoConnectedPeerException(
        "Attempt to send to current peer while no peer connected.");
    } else if (currentConnectionInfo == null) {
      throw new NoConnectedPeerException(
        "Attempt to send to current peer but connection info null.");
    } else {
      // Get the IP address of the other node from the currentConnectionInfo.
      // Create a socket to the other node.
      // Send some/all of the queued messages.
      // Close the socket.
      // Return # messages sent.
      return 0;
    }
    
  }

  public class NoConnectedPeerException extends Exception {
    public NoConnectedPeerException(String message) {
      super(message);
    }
  }
}
