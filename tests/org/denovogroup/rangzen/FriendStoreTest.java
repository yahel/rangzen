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
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
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

/**
 * Unit tests for Rangzen's MessageStore class
 */
@Config(manifest="./apps/experimentalApp/AndroidManifest.xml", 
        emulateSdk=18, 
        resourceDir="../../ui/Rangzen/res")
@RunWith(RobolectricTestRunner.class)
public class FriendStoreTest {
  /** The instance of MessageStore we're using for tests. */
  private FriendStore store;

  /** The app instance we're using to pass to MessageStore. */
  private SlidingPageIndicator activity;

  /** Some friends we store/retrieve. */
  private String friend1;
  private String friend2;
  private String friend3;
  private String friend4;

  private Set<String> testFriendSet;

  @Before
  public void setUp() {
    activity = Robolectric.buildActivity(SlidingPageIndicator.class).create().get();
    store = new FriendStore(activity, StorageBase.ENCRYPTION_DEFAULT);
    friend1 = "Friend one's ID";
    friend2 = "Friend two's ID";
    friend3 = "Friend three's ID";
    friend4 = "Friend four's ID";

    testFriendSet = new HashSet<String>();
    testFriendSet.add(friend1);
    testFriendSet.add(friend2);
    testFriendSet.add(friend3);
    testFriendSet.add(friend4);
  }

  /**
   * Tests that we can store friends and get them all back.
   */
  @Test
  public void storeFriends() {
    // Start empty, adding friends adds to count of friends.
    assertEquals(0, store.getAllFriends().size());
    assertTrue(store.getAllFriends().isEmpty());
    assertTrue(store.addFriend(friend1));
    assertEquals(1, store.getAllFriends().size());
    assertTrue(store.addFriend(friend2));
    assertEquals(2, store.getAllFriends().size());
    
    // Duplicate friend isn't stored.
    assertFalse(store.addFriend(friend2));
    assertEquals(2, store.getAllFriends().size());

    assertTrue(store.addFriend(friend3));
    assertEquals(3, store.getAllFriends().size());
    assertTrue(store.addFriend(friend4));
    assertEquals(4, store.getAllFriends().size());

    Set<String> friends = store.getAllFriends();
    for (String friend : friends) {
      assertTrue(testFriendSet.contains(friend));
    }
  }

  /**
   * Tests that we can delete friends after storing them.
   */
  @Test
  public void deleteFriends() {
    // Add 4 friends
    assertTrue(store.addFriend(friend1));
    assertTrue(store.addFriend(friend2));
    assertTrue(store.addFriend(friend3));
    assertTrue(store.addFriend(friend4));
    assertEquals(4, store.getAllFriends().size());

    // Delete one. Now there are 3.
    assertTrue(store.deleteFriend(friend1));
    assertEquals(3, store.getAllFriends().size());

    // Delete another one. Now there are 2.
    assertTrue(store.deleteFriend(friend2));
    assertEquals(2, store.getAllFriends().size());

    // Delete the same one. Fails, and there are still 2.
    assertFalse(store.deleteFriend(friend2));
    assertEquals(2, store.getAllFriends().size());

    // Delete the rest, set is empty. 
    assertTrue(store.deleteFriend(friend3));
    assertTrue(store.deleteFriend(friend4));
    assertTrue(store.getAllFriends().isEmpty());
  }

  /**
   * Test and demonstrate some behaviors of the base64 encoding/decoding utiliyt
   * methods.
   */
  @Test
  public void testBase64EncodeDecode() throws UnsupportedEncodingException {
    String TEST = "TEST";
    // Identity.
    assertEquals(TEST, FriendStore.bytesToBase64(FriendStore.base64ToBytes(TEST)));
    // Hand chosen random example.
    assertEquals("Zm9vYmFyYmF6", FriendStore.bytesToBase64("foobarbaz".getBytes("utf-8")));

    // Corner cases.
    // Empty string's bytes encode to empty string in base64.
    assertEquals("", FriendStore.bytesToBase64("".getBytes("utf-8")));
    // Base64 empty string decodes to empty array of bytes.
    assertTrue(Arrays.equals(new byte[]{}, FriendStore.base64ToBytes("")));
    // Null's encoding is...

    assertNull(FriendStore.base64ToBytes(null));
    assertNull(FriendStore.bytesToBase64(null));
  }
}
