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
import org.denovogroup.experimental.WifiDirectSpeaker;

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
public class WifiDirectSpeakerTest {
  /** The instance of WifiDirectSpeaker we're testing. */
  private WifiDirectSpeaker speaker;

  /**
   * An instance of MainActivity, used as a context to get an instance
   * of WifiDirectSpeaker.
   */
  private MainActivity activity;

  /**
   * Create a MainActivity, use it as a context to create an instance of
   * WifiDirectSpeaker.
   */
  @Before
  public void setUp() {
    activity = Robolectric.buildActivity(MainActivity.class).create().get();
  }

  /**
   * Post-test actions.
   */
  @After
  public void tearDown() {
  }
  
  @Test
  public void onPeersChangedTest() { 
    // Get

  }

  /**
   * Empty passing test to "pass" the whole suite.
   */
  @Test
  public void succeeds() { }
}
