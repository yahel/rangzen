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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Robolectric.clickOn;
import static org.robolectric.Robolectric.shadowOf;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;

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

import android.content.Context;

/**
 * A class with simple tests that always pass to demonstrate that Robolectric
 * tests can be run.
 */
@Config(manifest="./apps/rangzen/AndroidManifest.xml", 
        emulateSdk=18, 
        resourceDir="../../res/org/denovogroup/rangzen/res")
@RunWith(RobolectricTestRunner.class)
public class BluetoothSpeakerTest {
  /** Instance of BluetoothSpeaker to test. */
  BluetoothSpeaker speaker;

  /** Runs before each test. */
  @Before
  public void setUp() {
  }   

  @Test
  public void testBTMACRegex() {
    assertTrue(BluetoothSpeaker.looksLikeBluetoothAddress("AA:BB:CC:DD:EE:FF"));
    assertTrue(BluetoothSpeaker.looksLikeBluetoothAddress("aa:bb:cc:dd:ee:ff"));
    assertTrue(BluetoothSpeaker.looksLikeBluetoothAddress("11:22:33:44:55:66"));
    assertTrue(BluetoothSpeaker.looksLikeBluetoothAddress("A1:B2:C3:D4:E5:F6"));
    assertTrue(BluetoothSpeaker.looksLikeBluetoothAddress("a1:b2:c3:d4:e5:f6"));
    assertTrue(BluetoothSpeaker.looksLikeBluetoothAddress("A1:b2:C3:d4:e5:f6"));
    assertTrue(BluetoothSpeaker.looksLikeBluetoothAddress("AA-BB-CC-DD-EE-FF"));
    assertTrue(BluetoothSpeaker.looksLikeBluetoothAddress("aa-bb-cc-dd-ee-ff"));
    assertTrue(BluetoothSpeaker.looksLikeBluetoothAddress("11-22-33-44-55-66"));
    assertTrue(BluetoothSpeaker.looksLikeBluetoothAddress("A1-B2-C3-D4-E5-F6"));
    assertTrue(BluetoothSpeaker.looksLikeBluetoothAddress("a1-b2-c3-d4-e5-f6"));
    assertTrue(BluetoothSpeaker.looksLikeBluetoothAddress("A1-b2-C3-d4-e5-f6"));
    assertTrue(BluetoothSpeaker.looksLikeBluetoothAddress("A1:b2:C3-d4-e5-f6"));

    assertFalse(BluetoothSpeaker.looksLikeBluetoothAddress("NO"));
    assertFalse(BluetoothSpeaker.looksLikeBluetoothAddress("AA:CC:EE:GG:II:KK"));
    assertFalse(BluetoothSpeaker.looksLikeBluetoothAddress("11:AA:CC:EE:GG:II:KK"));
  }
  
  @Test
  public void testReservedMACTest() {
    assertTrue(BluetoothSpeaker.isReservedMACAddress("00:00:5E:01:02:03"));
    assertTrue(BluetoothSpeaker.isReservedMACAddress("01:00:5E:01:02:03"));
    assertTrue(BluetoothSpeaker.isReservedMACAddress("02:00:5E:01:02:03"));
    assertTrue(BluetoothSpeaker.isReservedMACAddress("03:00:5E:01:02:03"));
    assertTrue(BluetoothSpeaker.isReservedMACAddress("FF:FF:FF:FF:FF:FF"));
    assertTrue(BluetoothSpeaker.isReservedMACAddress("00-00-5E-01-02-03"));
    assertTrue(BluetoothSpeaker.isReservedMACAddress("01-00-5E-01-02-03"));
    assertTrue(BluetoothSpeaker.isReservedMACAddress("02-00-5E-01-02-03"));
    assertTrue(BluetoothSpeaker.isReservedMACAddress("03-00-5E-01-02-03"));
    assertTrue(BluetoothSpeaker.isReservedMACAddress("FF-FF-FF-FF-FF-FF"));

    assertFalse(BluetoothSpeaker.isReservedMACAddress("Not a mac address"));
    assertFalse(BluetoothSpeaker.isReservedMACAddress("01:02:03:04:05:06"));
    assertFalse(BluetoothSpeaker.isReservedMACAddress("01-02-03-04-05-06"));
  }

}
