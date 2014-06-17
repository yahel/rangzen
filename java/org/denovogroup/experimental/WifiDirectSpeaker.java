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
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ChannelListener;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
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
  public String PING_STRING = "ping";
  public byte[] PING_BYTES = PING_STRING.getBytes();
  public int RANGZEN_PORT = 23985;
  public int MAX_PACKET_SIZE = 1500;

  /** 
   * An enum designating possible states (disconnected, connecting, connected)
   * that the speaker can be in.
   */
  public enum ConnectionState {
    NOT_CONNECTED, 
    CONNECTION_IN_PROGRESS,
    CONNECTED
  }

  /** The current state of connectivity the Speaker believes us to be in. */
  private ConnectionState connectionState;

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

  /** 
   * This flag is set to true by the overlying app when it wants to look for
   * peers.
   */
  private boolean seekingDesired = false;

  /** The WifiP2pDevice representing the local device. */
  private WifiP2pDevice localDevice = null;

  /** Peer device we're attempting to speak with; null if nobody is selected. */
  private WifiP2pDevice selectedPeerDevice = null;

  /** 
   * Set by a handler for WIFI_P2P_DISCOVERY_CHANGED_ACTION, this should be
   * true only when the Android framework is actually seeking peers. 
   */
  private boolean seeking;

  /**
   * Most recent WifiP2p connection info object returned by WifiP2p subsystem.
   * This is null at startup and is set to null whenever the connection is lost.
   */
  private WifiP2pInfo currentConnectionInfo;

  /** If we're not the group owner, the IP address of the remote peer. */
  private InetSocketAddress remoteAddress;

  /** The channel (UDP socket) for incoming/outgoing messages. */
  private DatagramChannel udpChannel;

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
  public WifiDirectSpeaker(Context context, PeerManager peerManager, 
                           WifiDirectFrameworkGetter frameworkGetter) {
    super();
    // TODO(lerner): Figure out which context we should be using.
    this.context = context;
    // this.manager = (WifiP2pManager) context.getSystemService(Context.WIFI_P2P_SERVICE);
    this.manager = frameworkGetter.getWifiP2pManagerInstance(context);
    // TODO(lerner): Create our own looper that doesn't run in the main thread.
    this.looper = context.getMainLooper();
    Log.d(TAG, "Initializing Wifi P2P Channel...");
    this.channel = manager.initialize(context, looper, mChannelListener);
    Log.d(TAG, "Finished initializing Wifi P2P Channel.");
    this.mPeerManager = peerManager;

    IntentFilter intentFilter = new IntentFilter();
    intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
    intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
    intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
    intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
    intentFilter.addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION);
    context.registerReceiver(this, intentFilter);

    if (udpChannel == null) {
      try {
        udpChannel = DatagramChannel.open();
        udpChannel.socket().bind(new InetSocketAddress(RANGZEN_PORT));
        udpChannel.configureBlocking(false);
      } catch (SocketException e) {
        Log.d(TAG, "Couldn't create datagram socket: " + e);
        udpChannel = null;
      } catch (IOException e) {
        Log.e(TAG, 
              "IOException while opening/configuring non-blocking UDP channel: " + e);
      }
    }

    Log.d(TAG, "Finished creating WifiDirectSpeaker.");
  }

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
   * Request that the WifiDirectSpeaker begin to search for peers. The speaker
   * will continue to look for peers until explicitly stopped or a connection
   * to another peer is started.
   */
  public void seekPeers() {
    // Log.d(TAG, "Seeking peers has been requested - calling manager.discoverPeers()");
    manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
      @Override
      public void onSuccess() {
        Log.d(TAG, "Discovery initiated");
      }
      @Override
      public void onFailure(int reasonCode) {
        Log.d(TAG, "Discovery failed: " + reasonCode);
      }
    });
  }

  public void stopSeekingPeers() {
    manager.stopPeerDiscovery(channel, new WifiP2pManager.ActionListener() {
      @Override
      public void onSuccess() {
        Log.d(TAG, "Discovery stopped successfully.");
      }
      @Override
      public void onFailure(int reasonCode) {
        Log.d(TAG, "Failed to stop peer discovery? Reason: " + reasonCode);
      }
    });
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
      Log.d(TAG, "Adding peer " + peer);  
      mPeerManager.addPeer(peer);
    }
    Log.v(TAG, "P2P peers changed");
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
      connectionState = ConnectionState.CONNECTED;
      Log.i(TAG, "Wifi P2P connected");

      // Request that a WifiP2pInfo object be delivered to our connection
      // info listener, which will store the info object in currentConnectionInfo.
      manager.requestConnectionInfo(channel, mConnectionInfoListener);
    } else {
      // It's a disconnect
      // TODO(lerner): Dispatch a notification about this?
      currentConnectionInfo = null;
      remoteAddress = null;
      connectionState = ConnectionState.NOT_CONNECTED;

      Log.i(TAG, "Wifi P2P disconnected");
    }
  }
  
  /**
   * This handles events that notify us that the WifiP2pDevice object
   * representing the local device has changed.
   */
  private void onWifiP2pThisDeviceChanged(Context context, Intent intent) {
    localDevice = (WifiP2pDevice) intent.getParcelableExtra(
        WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
    Log.v(TAG, "Wifi P2P this device changed action received; local device is now: " + localDevice);
  }

  /**
   * Receive events noting when Android has started or stopped looking
   * for Wifi P2P peers.
   */
  private void onWifiP2pDiscoveryChanged(Context context, Intent intent) {
    int seeking = intent.getIntExtra(WifiP2pManager.EXTRA_DISCOVERY_STATE, -1);
    if (seeking == WifiP2pManager.WIFI_P2P_DISCOVERY_STARTED) {
      Log.d(TAG, "Device is seeking Wifi Direct peers.");
      this.seeking = true;
    } else if (seeking == WifiP2pManager.WIFI_P2P_DISCOVERY_STOPPED) {
      Log.d(TAG, "Device is NOT seeking Wifi Direct peers.");
      this.seeking = false;
    } else {
      Log.wtf(TAG, "Discovery changed event didn't have an EXTRA_DISCOVERY_STATE?!");
    }
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
    } else if (WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION.equals(action)) {
      onWifiP2pDiscoveryChanged(context, intent);
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
      Log.w(TAG, "Communication with WifiP2pManager framework lost!");
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
      currentConnectionInfo = info;
      connectionState = ConnectionState.CONNECTED;
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
      // Actual handling of these peers is performed directly when the
      // peers changed event is raised, rather than indirectly here after
      // a request an a callback.
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
   * who are already connected.
   */
  public void tasks() {
    if (connectionState == ConnectionState.NOT_CONNECTED) {
      if (selectedPeerDevice != null) {
        connectToPeerDevice(selectedPeerDevice);
      } else if (!seeking && seekingDesired) {
        seekPeers();
      } else if (seeking && !seekingDesired) {
        stopSeekingPeers();
      }
    } else if (connectionState == ConnectionState.CONNECTION_IN_PROGRESS) {
      // In this state, we've invited another peer to connect to us but we haven't
      // yet successfully connected or failed to do so.
      //
      // TODO(lerner): Time out the connection after a while.
      Log.v(TAG, "Waiting on connection to selected peer " + selectedPeerDevice);
    } else if (connectionState == ConnectionState.CONNECTED) {
      if ( currentConnectionInfo == null) {
        Log.wtf(TAG, "connectionState of CONNECTED but no current connection info!");
        return;
      }
      listenForPing();
      pingOtherDevice();
      // if (currentConnectionInfo.isGroupOwner) {
      //   listenForPing();
      // } else {
      //   // pingGroupOwner();
      //   pingOtherDevice();
      // }
    }
    
    // Log.d(TAG, "Finished with WifiDirectSpeaker tasks.");
    // TODO(lerner): Instead of pinging, deal with messages in queues to various
    // peers.
    // If there's a different peer with messages waiting, connect to that peer.
    // Otherwise, either we stay connected to the same peer or 
    // WifiP2pDevice nextPeerDevice = getPeerDeviceWithQueuedMessages();
    // if (nextPeerDevice == null) {
    //   //disconnectFromCurrentPeer();
    // } else if (nextPeerDevice.equals(selectedPeerDevice)) {
    //   selectedPeerDevice = nextPeerDevice;
    //   connectToPeerDevice(nextPeerDevice);
    // }
  }

  /** 
   * Tell the WifiDirectSpeaker whether it is allowed to seek peers while
   * not connected.
   *
   * @param seekingDesired True if peer discovery is permitted.
   */
  public void setSeekingDesired(boolean seekingDesired) {
    this.seekingDesired = seekingDesired;
  }
  
  /**
   * Select a peer as the desired communication partner.
   *
   * @param peer The peer to which the speaker should attempt to connect and
   * communicate with.
   */
  public void selectPeer(Peer peer) {
    WifiP2pDevice device = peer.network.wifiP2pDevice;
    selectedPeerDevice = device;
    // if (selectedPeerDevice == null) {
    //   Log.d(TAG, "selecting " + peer + " to connect and start pinging.");
    //   WifiP2pDevice device = peer.network.wifiP2pDevice;
    //   connectToPeerDevice(device);
    //   selectedPeerDevice = device;
    // }
  }

  /**
   * Listen (in a non-blocking fashion) for a ping.
   */
  private void listenForPing() {
    ByteBuffer packet = tryReceivePacket();
    if (packet != null) {
      Log.d(TAG, "Received a packet, it says: " + (new String(packet.array())).substring(0,PING_STRING.length()));
    }
  }

  /**
   * Initiate a Wifi Direct connection to the given peer.
   */
  private void connectToPeerDevice(WifiP2pDevice device) {
    WifiP2pConfig config = new WifiP2pConfig();
    config.deviceAddress = device.deviceAddress;

    Log.d(TAG, "calling manager.connect on: " + config.deviceAddress);

    connectionState = ConnectionState.CONNECTION_IN_PROGRESS;

    manager.connect(channel, config, new ActionListener() {
      @Override
      public void onSuccess() {
        Log.i(TAG, "Connection was requested successfully (we are not yet connected).");
      }

      @Override
      public void onFailure(int reason) {
        Log.w(TAG, "Connect request rejectd with error code: " + reason);
      }
    });
  }

  /**
   * Attempts to send a packet containing the bytes of the given message
   * over udp to the other node in the connection.
   *
   * TODO(lerner): Encode the string more intelligently.
   *
   * @param message A string to be sent to the connected peer.
   * @return True if the message seems to have been sent without incident,
   * false in the event of an exception/failure.
   */
  private boolean sendMessageToRemotePeer(String message) {
    if (!isConnected()) {
      return false;
    }

    InetSocketAddress destination;
    if (!isGroupOwner()) {
      destination = new InetSocketAddress(currentConnectionInfo.groupOwnerAddress,
                                          RANGZEN_PORT);
    } else if (isGroupOwner() && remoteAddress != null) {
      destination = new InetSocketAddress(remoteAddress.getAddress(),
                                          RANGZEN_PORT);
    } else {
      Log.d(TAG, "We're the group owner and we don't know the remote address.");
      return false;
    }
    ByteBuffer data = ByteBuffer.wrap(message.getBytes());

    // byte[] dataPulledOut = new byte[PING_BYTES.length];
    // data.get(dataPulledOut);
    Log.d(TAG, "Created a packet with contents: " + new String(data.array()));

    try {
      udpChannel.send(data, destination);
      Log.i(TAG, "Sent packet to " + destination);
      return true;
    } catch (ClosedChannelException e) {
      Log.e(TAG, "Closed channel exception: " + e);
      return false;
    } catch (IOException e) {
      Log.e(TAG, "Couldn't send packet over socket: " + e);
      return false;
    }
  }
  /**
   * Attempts to send a packet containing the bytes of the given message
   * over udp to the owner of the group.
   *
   * TODO(lerner): Encode the string more intelligently.
   *
   * @param message A string to be sent to the group owner.
   * @return True if the message seems to have been sent without incident,
   * false in the event of an exception/failure.
   */
  private boolean sendMessageToGroupOwner(String message) {
    InetSocketAddress destination = 
      new InetSocketAddress(currentConnectionInfo.groupOwnerAddress,
          RANGZEN_PORT);
    ByteBuffer data = ByteBuffer.wrap(message.getBytes());

    // byte[] dataPulledOut = new byte[PING_BYTES.length];
    // data.get(dataPulledOut);
    Log.d(TAG, "Created a packet with contents: " + new String(data.array()));

    try {
      udpChannel.send(data, destination);
      Log.i(TAG, "Sent packet to " + destination);
      return true;
    } catch (ClosedChannelException e) {
      Log.e(TAG, "Closed channel exception: " + e);
      return false;
    } catch (IOException e) {
      Log.e(TAG, "Couldn't send packet over socket: " + e);
      return false;
    }
  }

  /**
   * Check the socket for the next recieved packet and return it if one exists.
   *
   * @return A single packet's content as a byte buffer, or null if no datagrams
   * have been received since the last call or an exception occurred.
   */
  private ByteBuffer tryReceivePacket() {
    try { 
      ByteBuffer packet = ByteBuffer.allocate(MAX_PACKET_SIZE); 
      packet.clear();
      InetSocketAddress remoteAddress = (InetSocketAddress) udpChannel.receive(packet);
      if (remoteAddress != null) {
        // Log.d(TAG, "Received a packet, it says: " + new String(packet.array()));
        Log.d(TAG, "Received a packet from: " + remoteAddress);
        this.remoteAddress = remoteAddress;
        return packet;
      } else {
        return null;
      }
    } catch (ClosedChannelException e) {
      Log.e(TAG, "Channel closed when called receive on udpChannel: " + e);
      return null;
    } catch (IOException e) {
      Log.e(TAG, "IOException while receiving from udpChannel: "  + e);
      return null;
    }
  }

  /**
   * Check whether we're currently connected to a Wifi Direct network,
   * have a connection info object for that connection.
   *
   * @return True if we're in the connected state and have connection info
   * for the connection.
   */
  private boolean isConnected() {
    if (connectionState != ConnectionState.CONNECTED) {
      Log.e(TAG, "Check that we are in CONNECTED state failed.");
      return false;
    } else if (currentConnectionInfo == null) {
      Log.e(TAG, "Check that we have connection info available failed.");
      return false;
    } else {
      return true;
    }
  }
  
  /**
   * Check if we're the group owner of a connection.
   *
   * @return True if we're the owner of a group, false otherwise.
   */
  private boolean isGroupOwner() {
    if (currentConnectionInfo == null) {
      return false;
    } else if (currentConnectionInfo.isGroupOwner) {
      return true;
    } else {
      return false;
    }
  }

  /**
   * This exception is raised when an operation requiring a connected peer
   * is attempted but the device is not currently connected to a peer.
   */
  public class NoConnectedPeerException extends Exception {
    public NoConnectedPeerException(String message) {
     super(message);
    }
  }

  /**
   * Send a ping to the group owner, if we are already connected. Raises an
   * exception if not already connected.
   */
  private void pingGroupOwner() {
    if (!isConnected() || isGroupOwner()) {
      return;
    } else {
      sendMessageToGroupOwner(PING_STRING);
    }
  }

  /**
   * Send a ping to the other device in the connection, if we know its 
   * address.
   */
  private void pingOtherDevice() {
    sendMessageToRemotePeer(PING_STRING);
    // if (isConnected() && isGroupOwner()) {
    //   sendMessageToRemotePeer(PING_STRING);
    // } else if (isConnected() && !isGroupOwner()) {
    //   sendMessageToGroupOwner(PING_STRING);
    // }
  }
}
