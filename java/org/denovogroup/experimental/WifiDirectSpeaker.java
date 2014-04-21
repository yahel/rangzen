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
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ChannelListener;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.os.Looper;
import android.util.Log;

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
   * Request that the given data be sent to the given Peer via Wifi Direct.
   */
  public void send(String message, Peer destination) {
    // TODO(lerner): Send or enqueue the message.
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
   * Updates the Speaker's internal peer table with the new info (e.g. about
   * whether we are connected to the peer or not).
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
      manager.requestConnectionInfo(channel, mConnectionInfoListener);
    } else {
      // It's a disconnect
      // TODO(lerner): Dispatch a notification about this.
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
}
