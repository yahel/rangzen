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

import android.app.Service;
import android.content.BroadcastReceiver; 
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Date;

/**
 * This module exposes an API for the application to find out the current 
 * list of peers, to request that new peers be sought, and to request that
 * peers be garbage collected. The API and all the behavior of this class are 
 * independent of underlying protocols for those peers. 
 */
public class PeerManager {
  /** 
   * A static variable to hold the active instance of PeerManager so that other
   * app components can call methods in its API.
   */
  private static PeerManager sPeerManager;

  // TODO(lerner): I suspect we want to convert this to a Set eventually, since
  // that best represents the set of peers we can see (unordered, each is
  // unique).
  /** The most recent, up-to-date list of peers. */
  private List<Peer> mCurrentPeers;

  /** For app-local intent broadcasting/receiving of peer events. */
  private LocalBroadcastManager mBroadcastManager;

  /** Displayed in Android Monitor logs. */
  private static String TAG = "RangzenPeerManager";

  /** The WifiDirectSpeaker for the app. */
  private static WifiDirectSpeaker mWifiDirectSpeaker;

  /**
   * Private constructor. Use PeerManager.getInstance() to obtain the app's
   * instance of the class.
   *
   * @param context A context object from the app.
   */
  private PeerManager(Context context) {
    mCurrentPeers = new ArrayList<Peer>();
    mBroadcastManager = LocalBroadcastManager.getInstance(context); 

    // WifiDirectSpeaker needs the context in order to retrieve system 
    // Wifi P2p resources.
    WifiDirectFrameworkGetter frameworkGetter = new WifiDirectFrameworkGetter();
    mWifiDirectSpeaker = new WifiDirectSpeaker(context, this, frameworkGetter);
  }

  /**
   * Obtain the current instance of PeerManager.
   *
   * @param context A context object from the app.
   * @return The app's instance of PeerManager.
   */
  public static PeerManager getInstance(Context context) {
    if (sPeerManager == null) {
      Log.d(TAG, "Created instance of PeerManager");
      sPeerManager = new PeerManager(context);
    }
    return sPeerManager;
  }

  /** 
   * The length of time (in milliseconds) we consider peers valid.
   */
  public static final long PEER_TIMEOUT = 300 * 1000;

  /**
   * Ask the PeerManager to start the process of looking for new remote
   * peers to communicate with. This method returns immediately. Callers
   * should listen for peer list events if they wish to act on peers when
   * the list is updated.
   */
  public void seekPeers() {
    Log.v(TAG, "Setting OK to seek peers on WifiDirectSpeaker.");
    // TODO(lerner); Ask for new peers to be sought!
    // This will look something like:
    //
    // <consult policy>
    // for each protocol:
    //   if <policySaysOk>
    //     protocol.seekPeers();
    if (mWifiDirectSpeaker != null) {
      mWifiDirectSpeaker.setSeekingDesired(true);
    }
  }
  
  /**
   * This method garbage runs the peer garbage collector on all peers that
   * should be garbage collected. It runs synchronously and returns when done,
   * but should be very fast (deciding whether to garbage collect a peer is
   * not a complicated action).
  */
  public synchronized void garbageCollectPeers() {
    for (Peer p : mCurrentPeers) {
      if (shouldGarbageCollectPeer(p)) {
        garbageCollectPeer(p);
      }
    } 
  }

  /**
   * Check whether a peer is already in the peer list. 
   *
   * Peer equality is based on whether their PeerNetworks refer to the same
   * destinations, so two peers might be .equals() even if not ==.
   *
   * @param peer The Peer to find in the list.
   * @return True if the peer is in the list, false otherwise.
   * @see org.denovogroup.experimental.Peer
   */
  public synchronized boolean isKnownPeer(Peer peer) {
    return mCurrentPeers.contains(peer);
  }

  /**
   * Update the last seen time of the peer.
   *
   * It is possible (indeed, frequent) to have two different peer objects
   * which logically refer to the same peer. This updates the canonical copy -
   * the one stored in the peer list - as well as the copy passed in.
   *
   * @param peer The Peer to update.
   */
  private synchronized void touchPeer(Peer peer) {
    Peer copyInList = getCanonicalPeer(peer);
    if (copyInList != null) {
      copyInList.touch();
    } 
   if (peer != null) {
      peer.touch();
    }
  }

  /**
   * If the peer given exists in the peer list, return it.
   *
   * Peer equality is based on whether their PeerNetworks refer to the same
   * destinations, so two peers might be .equals() even if not ==.
   *
   * @param peer The peer to find in the list.
   * @return The equivalent peer, or null if the peer is not found in the list.
   * @see org.denovogroup.experimental.Peer
   */
  public synchronized Peer getCanonicalPeer(Peer peerDesired) {
    if (peerDesired == null) {
      return null;
    }
    for (Peer peerInList : mCurrentPeers) {
      if (peerDesired.equals(peerInList)) {
        return peerInList;
      }
    }
    return peerDesired;
  }

  /**
   * Check whether a peer is considered old enough to consider it unlikely
   * to return.
   */
  private synchronized boolean shouldGarbageCollectPeer(Peer peer) {
    Date lastSeen = peer.getLastSeen();
    Date now = new Date();

    long msSinceSeen = now.getTime() - lastSeen.getTime();

    // TODO(lerner): Use a more sophisticated mechanism than a simple
    // time threshold since last seen. For example, we may not want to evict
    // anyone if we haven't scanned for a while (or, maybe we do).
    return msSinceSeen > PEER_TIMEOUT;
  }

  /**
   * Invalidates a peer and removes it from the PeerManager's list of current
   * peers.
   */
  private synchronized void garbageCollectPeer(Peer peer) {
    mCurrentPeers.remove(peer);
  }

  /**
   * Get a snapshot of the current list of peers. Peers are not guaranteed 
   * to be reachable or still in existence, and the snapshot may be outdated
   * as peers are sought.
   *
   * @return A copy of the list of currently known peers.
   */
  public synchronized List<Peer> getPeers() {
    List<Peer> copy = new ArrayList<Peer>();
    for (Peer p : mCurrentPeers) {
      copy.add(p);
    }
    return copy;
  }

  /**
   * Add list of peers to the current peers list, just as though they were
   * added individually with newPeer().
   *
   * @return The number of non-duplicate peers added.
   */
  public synchronized int addPeers(List<Peer> newPeers) {
    int nonDuplicateCount = 0;
    for (Peer p : newPeers) {
      if (addPeer(p)) {
        nonDuplicateCount++;
      }
    }
    return nonDuplicateCount;
  }

  /**
   * Remove all peers from the peer list.
   */
  public synchronized void forgetAllPeers() {
    mCurrentPeers.clear();
  }

  /**
   * Add a peer to the current list of peers. Used internally to add peers
   * discovered, but also can be called externally to add peers which use
   * very asynchronous mechanisms (e.g. SD card) which do not support
   * automatic discovery.
   *
   * @return True if the peer was added, false if the peer was a duplicate
   * and thus was already in the list.
   */
  public synchronized boolean addPeer(Peer p) {
    if (isKnownPeer(p)) {
      touchPeer(p);
      return false;
    } else {
      mCurrentPeers.add(p);
      return true;
    }
  }

  /**
   * Run tasks, e.g. garbage collection of peers, speaker tasks, etc.
   */
  public void tasks() {
    mWifiDirectSpeaker.tasks();

    // We ask to connect and ping every time but the speaker will ignore 
    // subsequent requestse since it has a peer device selected.
    // TODO(lerner): Don't just constantly ask to connect from here.
    if (mCurrentPeers.size() > 0) {
      Log.v(TAG, "Found at least 1 peer, connecting to it.");
      Peer peer = mCurrentPeers.get(0);
      // mWifiDirectSpeaker.connectAndPingPeer(peer);
      mWifiDirectSpeaker.selectPeer(peer);
    }
    // Log.v(TAG, "Finished with PeerManager tasks.");
  }
  
}
