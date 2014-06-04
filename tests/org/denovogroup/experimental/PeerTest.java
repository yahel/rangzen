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

import org.denovogroup.experimental.Peer;
import org.denovogroup.experimental.PeerNetwork;

import android.net.wifi.p2p.WifiP2pDevice;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
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

import java.util.Date;

/**
 * Unit tests for Rangzen's Peer class
 */
@Config(manifest="./apps/experimental/AndroidManifest.xml", resourceDir="../../res/org/denovogroup/experimental/res")
@RunWith(RobolectricTestRunner.class)
public class PeerTest {
  private Peer peer;
  private Peer sameDevicePeer;
  private Peer differentDevicePeer;

  private static final String addr1 = "00:11:22:33:44:55";
  private static final String addr2 = "99:88:77:66:55:44";

  @Before
  public void setUp() {
    // Create two peers with networks pointing to the same MAC address.
    WifiP2pDevice d1 = new WifiP2pDevice();
    d1.deviceAddress = addr1; 
    WifiP2pDevice d2 = new WifiP2pDevice();
    d2.deviceAddress = addr1;
    PeerNetwork pn1 = new PeerNetwork(d1);
    peer = new Peer(pn1);
    PeerNetwork pn2 = new PeerNetwork(d2);
    sameDevicePeer = new Peer(pn2);

    // And create one peer pointing to another address.
    WifiP2pDevice d3 = new WifiP2pDevice();
    d3.deviceAddress = addr2;
    PeerNetwork pn3 = new PeerNetwork(d3);
    differentDevicePeer = new Peer(pn3);
  }

  /**
   * Test that a newly created Peer has a lastSeen date of (approximately)
   * the time it was created.
   */
  @Test
  public void newPeerHasRecentLastSeen() {
    Date timeBefore = new Date();
    Peer p = new Peer(new PeerNetwork());
    Date timeAfter = new Date();

    assertFalse("Last seen time not after creation", p.getLastSeen().after(timeAfter));
    assertFalse("Last seen time not before creation", p.getLastSeen().before(timeBefore));
  }

  /**
   * Touching sets lastSeen date of time called.
   */
  @Test
  public void touchedPeerHasRecentLastSeen() {
    Date timeBefore = new Date();
    peer.touch();
    Date timeAfter = new Date();

    assertFalse("Last seen time not after touch", peer.getLastSeen().after(timeAfter));
    assertFalse("Last seen time not before touch", peer.getLastSeen().before(timeBefore));
  }

  /**
   * Touching with a specific date sets the date to exactly that date.
   */
  @Test
  public void touchSpecificDate() {
    Date dateToSet = new Date(123456789);
    peer.touch(dateToSet);

    assertTrue(peer.getLastSeen().equals(dateToSet));
  }

  /**
   * Tests that two peers with the same network device are equal.
   *
   * Can't run this test because it requires calling into the Android API
   * to create WifiP2pDevices that are the same/different since that's the
   * criteria on which comparisons of PeerNetworks are based.
   */
  @Test
  public void peerEquality() {
    assertTrue("Same network device but peers not equal.", 
            peer.equals(sameDevicePeer));
    assertFalse("Different network device but peers equal.", 
            peer.equals(differentDevicePeer));
  }

  /**
   * Tests that the clone of a Peer is equal to it and its duplicates.
   */
  @Test
  public void cloneEquality() {
    assertTrue("Peer not .equals() to its clone.", 
            peer.equals(peer.clone()));
    assertTrue("Same network device peer not equal to clone of peer.",
            sameDevicePeer.equals(peer.clone()));

  }

}