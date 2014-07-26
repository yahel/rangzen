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

import java.io.IOException;
import java.io.OptionalDataException;
import java.io.StreamCorruptedException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import org.denovogroup.rangzen.MainActivity;
import org.denovogroup.rangzen.MessageStore;

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

import android.location.Location;

/**
 * Unit tests for Rangzen's MessageStore class
 */
@Config(manifest="./apps/rangzen/AndroidManifest.xml", 
        emulateSdk=18, 
        resourceDir="../../res/org/denovogroup/rangzen/res")
@RunWith(RobolectricTestRunner.class)
public class ExchangeStoreTest {
  /** The instance of MessageStore we're using for tests. */
  private ExchangeStore store;

  /** The app instance we're using to pass to MessageStore. */
  private MainActivity activity;

  /** An exchange we store/retrieve. */
  private Exchange ex;

  /** Some locations we store/retrieve in the exchange. */
  private Location loc1;
  private Location loc2;
  private SerializableLocation serialLoc1;
  private SerializableLocation serialLoc2;

  /** The values we have in the locations. */
  private static final double lat1 = 12.3;
  private static final double lat2 = 23.4;

  private static final String PROVIDER1 = "Provider1";
  private static final String PROVIDER2 = "Provider2";

  private static final String PHONE_ID = "123";
  private static final String PEERPHONE_ID = "456";
  private static final String PROTOCOL = "Bluetooth";
  private static final long START_TIME = 7; 
  private static final long END_TIME = 8; 

  @Before
  public void setUp() {
    activity = Robolectric.buildActivity(MainActivity.class).create().get();
    store = new ExchangeStore(activity, StorageBase.ENCRYPTION_NONE);

    loc1 = new Location(PROVIDER1);
    loc2 = new Location(PROVIDER2);
    loc1.setLatitude(lat1);
    loc2.setLatitude(lat2);
    serialLoc1 = new SerializableLocation(loc1);
    serialLoc2 = new SerializableLocation(loc2);

    ex = new Exchange(PHONE_ID, PEERPHONE_ID, PROTOCOL, START_TIME, END_TIME, serialLoc1, serialLoc2);
  }

  /**
   * Tests that we can store an exchange and get it back.
   */
  @Test
  public void storeExchanges() throws StreamCorruptedException, OptionalDataException, 
         IOException, ClassNotFoundException {
    assertEquals(0, store.getAllExchanges().size());

    assertTrue(store.addExchange(ex));
    assertEquals(1, store.getAllExchanges().size());

    List<Exchange> exchanges = store.getAllExchanges();
    for (Exchange exchange : exchanges) {
      assertEquals(ex, exchange);
    }
  }
}
