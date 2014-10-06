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

import android.content.Context;
import android.net.wifi.p2p.WifiP2pDevice;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import java.util.Random;

/**
 * Unit tests for methods from PeerManager that determine whether we should
 * listen or initiate exchanges based on relative MAC address values.
 *
 * TODO(lerner): Merge these into PeerManagerTest when the test surgery is complete.
 */
@Config(manifest="./apps/rangzen/AndroidManifest.xml", 
        emulateSdk=18, 
        resourceDir="../../res/org/denovogroup/rangzen/res")
@RunWith(RobolectricTestRunner.class)
public class MacOrderingTests {

  /**
   * Create a MainActivity, use it as a context to fetch an instance of
   * PeerManager.
   */
  @Before
  public void setUp() {
  }


  /**
   * Test that PeerManager.startsWithAOneBit() works correctly.
   */
  @Test
  public void testBitTest() {
    byte[] bytes = new byte[1];
    bytes[0] = 1;
    assertTrue(PeerManager.startsWithAOneBit(bytes));
    assertFalse(PeerManager.startsWithAOneBit(new byte[0]));
    assertFalse(PeerManager.startsWithAOneBit(null));
    bytes[0] = 2;
    assertFalse(PeerManager.startsWithAOneBit(bytes));
    bytes = new byte[32];
    bytes[0] = 2;
    assertFalse(PeerManager.startsWithAOneBit(bytes));
    bytes[0] = 73;
    assertTrue(PeerManager.startsWithAOneBit(bytes));
    bytes[0] = 0x41;
    assertTrue(PeerManager.startsWithAOneBit(bytes));
    bytes[0] = 0x40;
    assertFalse(PeerManager.startsWithAOneBit(bytes));
  }

  /**
   * Test the PeerManager.concatAndHash() function.
   */
  @Test
  public void testConcatenateAndHash() throws NoSuchAlgorithmException, 
                                              UnsupportedEncodingException {
    String a = "AA:BB:CC:DD:EE:FF";
    String b = "11:22:33:44:55:66";
    // Calculated by hand with
    // echo -n "AA:BB:CC:DD:EE:FF11:22:33:44:55:66" | shasum5.12 -a 256 
    final String answer = "8e59ef44ffae50a61824c63959bdfb101fd7c6b5af2a0303567c3607faee28ec";
    assertEquals(answer, toHexString(PeerManager.concatAndHash(a, b)));
    final String answer2 = "418013cf17de68c194621f90b52c988f7cc9765694969bff786bb274e42003f8";
    assertEquals(answer2, toHexString(PeerManager.concatAndHash(b, a)));
  }


  /**
   * Shows the work involved in discovering that between two addresses, one of them
   * should initiate and it is the case that it does according to 
   * PeerManager.whichInitiates().
   */
  @Test
  public void testWhichInitiates() throws NoSuchAlgorithmException, 
                                          UnsupportedEncodingException {
    String a = "AA:BB:CC:DD:EE:FF";
    String b = "11:22:33:44:55:66";

    // a > b, so the algorithm will concatenate b + a.
    assertTrue(a.compareTo(b) > 0);

    // sha256(b + a) starts with the byte 41, which is odd so b should initiate.
    assertEquals("418013cf17de68c194621f90b52c988f7cc9765694969bff786bb274e42003f8",
                 toHexString(PeerManager.concatAndHash(b, a)));

    // 41 starts with a 1 bit.
    assertTrue(PeerManager.startsWithAOneBit(PeerManager.concatAndHash(b,a)));

    // So:
    assertEquals(b, PeerManager.whichInitiates(a,b));

    assertNull(PeerManager.whichInitiates(a,null));
    assertNull(PeerManager.whichInitiates(null, b));
    assertNull(PeerManager.whichInitiates(null, null));
  }

  // Utility methods for the tests.
  final protected static char[] hexArray = 
                {'0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f'};
  private static String toHexString(byte[] bytes) {
    char[] hexChars = new char[bytes.length*2];
    int v;

    for(int j=0; j < bytes.length; j++) {
      v = bytes[j] & 0xFF;
      hexChars[j*2] = hexArray[v>>>4];
      hexChars[j*2 + 1] = hexArray[v & 0x0F];
    }

    return new String(hexChars);
  }
  
}
