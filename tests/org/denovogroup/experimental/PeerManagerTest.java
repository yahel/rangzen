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

import org.denovogroup.experimental.MainActivity;
import org.denovogroup.experimental.Peer;
import org.denovogroup.experimental.PeerNetwork;

import android.content.Context;
import android.net.wifi.p2p.WifiP2pDevice;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Robolectric.clickOn;
import static org.robolectric.Robolectric.shadowOf;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowIntent;

import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import java.util.Random;

/**
 * Unit tests for Rangzen's PeerManager class
 */
@Config(manifest="./apps/experimental/AndroidManifest.xml", resourceDir="../../res/org/denovogroup/experimental/res")
@RunWith(RobolectricTestRunner.class)
public class PeerManagerTest {
  /** The instance of PeerManager we're testing. */
  private PeerManager manager;

  /** Distinct MAC address counter. */
  private static int lastOctet = 0; 
  private static int secondToLastOctet = 0; 

  /**
   * An instance of MainActivity, used as a context to get an instance
   * of PeerManager
   */
  private MainActivity activity;


  /**
   * Create a MainActivity, use it as a context to fetch an instance of
   * PeerManager.
   */
  @Before
  public void setUp() {
    activity = Robolectric.buildActivity(MainActivity.class).create().get();
    manager = PeerManager.getInstance(activity);
  }

  /**
   * Clear the peer list to have it squeaky clean for the next test.
   */
  @After
  public void tearDown() {
    manager.forgetAllPeers();
  }

  /**
   * Test that calling forgetAllPeers empties the list.
   */
  public void testForgetAllPeers() {
    List<Peer> newPeers = new ArrayList<Peer>();
    newPeers.add(randomWifiP2pPeer());
    newPeers.add(randomWifiP2pPeer());
    newPeers.add(randomWifiP2pPeer());
    newPeers.add(newPeers.get(0));

    int addedCount = manager.addPeers(newPeers);
    manager.forgetAllPeers();
    assertEquals("After forgetAllPeers, the peer list is not empty",
            manager.getPeers().size(), 0);
  }

  /**
   * getInstance() should always return the same instance of PeerManager.
   */
  @Test
  public void getInstanceGetsSameInstance() {
    PeerManager p1 = PeerManager.getInstance(activity);
    PeerManager p2 = PeerManager.getInstance(activity);
    // Initial and second call same.
    assertEquals("First and second calls to PeerManager.getInstance() " +
                 "don't return the same object.", manager, p1);
    // Two subsequent calls equal.
    assertEquals("Second and third calls to PeerManager.getInstance() " + 
                 "don't return the same object.", p1, p2);
  }

  /**
   * Check that new peers are actually added to the list.
   */
  @Test
  public void addPeersTest() {
    // Get baseline list of peers (might not be empty).
    List<Peer> originalPeers = manager.getPeers();
    int originalLength = originalPeers.size();

    // Create some random new peers and add them.
    List<Peer> newPeers = new ArrayList<Peer>();
    newPeers.add(randomWifiP2pPeer());
    newPeers.add(randomWifiP2pPeer());
    newPeers.add(randomWifiP2pPeer());
    newPeers.add(newPeers.get(0));
    int addedCount = manager.addPeers(newPeers);
    assertEquals("Adding 3 random peers doesn't report adding 3 peers.",
            addedCount, 3);
    
    // Test that all the peers we added are in the list.
    List<Peer> gottenPeers = manager.getPeers();
    for (Peer p : newPeers) {
      assertTrue("New peer not known after addPeer().", 
              manager.isKnownPeer(p));
      assertNotNull("New peer is null in peer list after addPeers().",
              manager.getCanonicalPeer(p));
    }
    int gottenLength = gottenPeers.size();
    assertEquals("After addPeers() of 3 peers, list is not 3 longer.",
            gottenLength, originalLength + 3);
    
    // Adding the same peers doesn't add any more since they're duplicates.
    addedCount = manager.addPeers(newPeers);
    assertEquals("Adding duplicates claimed to add peers.", 
            addedCount, 0);
    assertEquals("Adding duplicates resulted in peer list getting longer.",
            gottenLength, manager.getPeers().size());
  }

  /**
   * Check that a new peer is actually added to the list.
   */
  @Test
  public void addOnePeerTest() {
    // Baseline list of peers, maybe not empty.
    List<Peer> originalPeers = manager.getPeers();
    int originalLength = originalPeers.size();

    // Create a new, random peer and add it. 
    Peer newPeer = randomWifiP2pPeer();
    boolean added = manager.addPeer(newPeer);
    assertTrue("PeerManager claimed not to add a new random peer.", added);
    
    // New peer list should have one more peer, and it should include the newly
    // added peer.
    List<Peer> gottenPeers = manager.getPeers();
    assertTrue("Added peer isn't known after addPeer().", 
            manager.isKnownPeer(newPeer));
    assertNotNull("Peer is null when fetched after addPeer().",
            manager.getCanonicalPeer(newPeer));
    int gottenLength = gottenPeers.size();
    assertEquals("Adding random peer w/ addPeer() doesn't enlarge peer list by 1.",
            gottenLength, originalLength + 1);

    // Add the new peer again. It's a duplicate, so the list shouldn't change.
    boolean addedAgain = manager.addPeer(newPeer);
    assertFalse("PeerManager claimed to add duplicate peer.", addedAgain);
    List<Peer> newGottenPeers = manager.getPeers();
    assertTrue("New peer list after addPeer() on dupe isn't same size.",
            gottenLength == newGottenPeers.size());
  }

  /**
   * It's common to have multiple Peer objects refer to the 
   * same peer. This tests that the functions that detect these duplicates
   * work.
   */
  @Test
  public void equivalentPeersDetected() {
    // Create two peers backed by the same MAC address (but distinct 
    // PeerNetworks and underlying WifiP2pDevices).
    String addr = distinctMACAddress();
    WifiP2pDevice d1 = new WifiP2pDevice();
    d1.deviceAddress = addr;
    WifiP2pDevice d2 = new WifiP2pDevice();
    d2.deviceAddress = addr;
    Peer p1 = new Peer(new PeerNetwork(d1));
    Peer p2 = new Peer(new PeerNetwork(d2));
    assertTrue("2 peers with same MAC aren't .equals()!", p1.equals(p2));

    // Check that p2 is known if p1 is added.
    manager.addPeer(p1);
    assertTrue("p2 not known after p1 added.", manager.isKnownPeer(p2));
    assertTrue("p1 not known after p1 added.", manager.isKnownPeer(p1));

    // And that the Peer retrieved when seeking either peer is .equals() to the
    // other equivalent peer.
    assertNotNull("p2 not in peer list after p1 added.", 
            manager.getCanonicalPeer(p2));
    assertNotNull("p1 not in peer list after p1 added.", 
            manager.getCanonicalPeer(p1));
    assertTrue("p2 in peer list but not equal to p1 after p1 added.",
            manager.getCanonicalPeer(p2).equals(p1));
    assertTrue("p1 in peer list but not equal to p2 after p1 added.",
            manager.getCanonicalPeer(p1).equals(p2));
  }

  /**
   * Check that peers added again have their last seen time updated.
   */
  @Test
  public void repeatPeersGetTouched() throws Exception {
    String addr = distinctMACAddress();
    WifiP2pDevice d1 = new WifiP2pDevice();
    d1.deviceAddress = addr;
    WifiP2pDevice d2 = new WifiP2pDevice();
    d2.deviceAddress = addr;
    Peer newPeer = new Peer(new PeerNetwork(d1));
    Peer dupePeer = new Peer(new PeerNetwork(d2));

    boolean added = manager.addPeer(newPeer);
    Date timeAdded = new Date();

    Thread.sleep(200); 
    manager.addPeer(newPeer);
    List<Peer> peers = manager.getPeers();
    Peer retrievedPeer = manager.getCanonicalPeer(newPeer);
    assertFalse("Peer last seen out of date after adding same peer object.",
            retrievedPeer.getLastSeen().before(new Date()));

    Thread.sleep(200); 
    manager.addPeer(dupePeer);
    peers = manager.getPeers();
    retrievedPeer = manager.getCanonicalPeer(newPeer);
    assertFalse("Peer last seen out of date after adding dupe peer.",
            retrievedPeer.getLastSeen().before(new Date()));
  }
   
  /**
   * Ensure that the right sequence of MAC addrs comes out of
   * distinctMACAddress().
   */
  @Test
  public void testDistinctMACAddress() {
    assertEquals("First distinctMACAddress is wrong",
            distinctMACAddress(), "AA:BB:CC:DD:00:01");
    assertEquals("Second distinctMACAddress is wrong",
            distinctMACAddress(), "AA:BB:CC:DD:00:02");
    assertNotEquals("Two distinctMACAddresses in a row are the same",
            distinctMACAddress(), distinctMACAddress());
  }

  /**
   * Return a series of distinct MAC addresses.
   */
  private String distinctMACAddress() {
    String macAddr = "AA:BB:CC:DD:";
    lastOctet++;
    if (lastOctet > 255) {
      secondToLastOctet++;
      lastOctet = 0;
    } 
    return String.format(macAddr + "%02X:%02X", secondToLastOctet, lastOctet);
  }
    
  /**
   * Return a new peer backed by a WifiP2pDevice that has a random MAC address.
   */
  private Peer randomWifiP2pPeer() {
    WifiP2pDevice d = new WifiP2pDevice();
    d.deviceAddress = distinctMACAddress();
    return new Peer(new PeerNetwork(d));
  }
  
}
