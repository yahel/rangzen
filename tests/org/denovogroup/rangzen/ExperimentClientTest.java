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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;

import org.denovogroup.rangzen.MainActivity;
import org.denovogroup.rangzen.StorageBase;

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

import android.location.Location;

import com.google.gson.Gson;

/**
 */
@Config(manifest="./apps/rangzen/AndroidManifest.xml", 
        emulateSdk=18, 
        resourceDir="../../res/org/denovogroup/rangzen/res")
@RunWith(RobolectricTestRunner.class)
public class ExperimentClientTest {
  /** Instance of ExperimentClient we're testing. */
  private ExperimentClient client;

  /** Example device IDs to register. */
  private static final String EXAMPLE_ID1 = "11:11:11:11:11:11";
  private static final String EXAMPLE_ID2 = "22:22:22:22:22:22";
  private static final String EXAMPLE_ID3 = "33:33:33:33:33:33";
  private static final String EXAMPLE_ID4 = "44:44:44:44:44:44";
  private static final String EXAMPLE_ID5 = "55:55:55:55:55:55";
  private static final String EXAMPLE_ID6 = "66:66:66:66:66:66";
  private static final String EXAMPLE_ID7 = "77:77:77:77:77:77";

  /** Example time values. */
  private static final long START_TIME_0 = 0;
  private static final long START_TIME_1 = 1;
  private static final long END_TIME_1 = 1;
  private static final long END_TIME_2 = 2;

  /** Friends! */
  private String[] friends1  = {"alice", "bob"};

  /** No friends :( */
  private String[] emptyFriends = {};

  /** GSON for parsing JSON responses. */
  private Gson gson = new Gson();

  @Before
  public void setUp() throws InterruptedException, ExecutionException {
    client = new ExperimentClient("http://localhost", 1337);
    resetServer();
  }

  /**
   * Reset the server (delete all data it's received so far.
   */
  private void resetServer() throws InterruptedException, ExecutionException {
    client.resetServer();
    String response = client.get();
    ExperimentClient.SimpleResponse simpleResponse;
    simpleResponse = gson.fromJson(response, ExperimentClient.SimpleResponse.class);
    assertTrue(simpleResponse.OK());
  }

  /**
   * Tests that timeouts work.
   */
  @Test
  public void testTimeouts()  throws InterruptedException, ExecutionException {
    client = new ExperimentClient("BAD", 5555);
    client.resetServer();
    String response = client.get();
    assertNull(response);
  }

  /**
   * Tests that we can register a phone with a couple friends, and that we get
   * back the same friends when we call getFriends() subsequently.
   */
  @Test
  public void testRegisterAndGetFriends() throws InterruptedException, ExecutionException {
    // Register.
    client.register(EXAMPLE_ID1, friends1);
    assertTrue(client.registrationWasSuccessful());

    // Check that friends are the same we registered.
    client.getFriends(EXAMPLE_ID1);
    String response = client.get();
    ExperimentClient.GetFriendsResponse friendsResponse;
    friendsResponse = gson.fromJson(response, ExperimentClient.GetFriendsResponse.class);
    assertNotNull(friendsResponse);
    assertTrue(friendsResponse.OK());
    assertEquals(friends1.length, friendsResponse.getFriends().length);
    String[] friends = friendsResponse.getFriends();
    for (int i=0; i<friends.length; i++) {
      assertEquals(friends[i], friends1[i]);
    }
  }

  /**
   * Tests that we can register a phone with an empty list of friends.
   */
  @Test
  public void testRegisterNoFriends() throws InterruptedException, ExecutionException {
    client.register(EXAMPLE_ID2, emptyFriends);
    assertTrue(client.registrationWasSuccessful());

    // Check that friends are the same we registered.
    client.getFriends(EXAMPLE_ID2);
    String response = client.get();
    ExperimentClient.GetFriendsResponse friendsResponse;
    friendsResponse = gson.fromJson(response, ExperimentClient.GetFriendsResponse.class);
    assertNotNull(friendsResponse);
    assertTrue(friendsResponse.OK());
    assertEquals(emptyFriends.length, friendsResponse.getFriends().length);
  }

  /**
   * Test that we can send locations to the server, and get them back.
   */
  @Test
  public void testUpdateLocation() throws InterruptedException, ExecutionException {
    Location location0 = new Location("Provider1");
    Location location1 = new Location("Provider2");
    Location location2 = new Location("Provider3");
    SerializableLocation sl0 = new SerializableLocation(location0);
    SerializableLocation sl1 = new SerializableLocation(location1);
    SerializableLocation sl2 = new SerializableLocation(location2);
    SerializableLocation[] locations = {sl0, sl1, sl2};

    client.register(EXAMPLE_ID3, friends1);
    assertTrue(client.registrationWasSuccessful());

    client.updateLocations(EXAMPLE_ID3, locations);
    assertTrue(client.updateLocationsWasSuccessful());

    client.getPreviousLocations(EXAMPLE_ID3);
    String response = client.get();
    ExperimentClient.GetPreviousLocationsResponse locationsResponse;
    locationsResponse = gson.fromJson(response,  ExperimentClient.GetPreviousLocationsResponse.class);
    assertNotNull(locationsResponse);
    SerializableLocation[] gottenLocations = locationsResponse.getLocations();
    assertEquals(locations.length, gottenLocations.length);
    for (int i=0; i<gottenLocations.length; i++) {
      assertEquals(locations[i], gottenLocations[i]);
    }
  }

  /**
   * Test that we can send exchanges to the server, and get them back.
   */
  @Test
  public void testUpdateExchanges() throws InterruptedException, ExecutionException {
    Location location0 = new Location("Provider1");
    Location location1 = new Location("Provider2");
    Location location2 = new Location("Provider3");
    Location location3 = new Location("Provider4");
    SerializableLocation sl0 = new SerializableLocation(location0);
    SerializableLocation sl1 = new SerializableLocation(location1);
    SerializableLocation sl2 = new SerializableLocation(location2);
    SerializableLocation sl3 = new SerializableLocation(location3);
    Exchange exchange0 = new Exchange(EXAMPLE_ID4, EXAMPLE_ID5, Exchange.PROTOCOL_BLUETOOTH, START_TIME_0, END_TIME_1, sl0, sl1);
    Exchange exchange1 = new Exchange(EXAMPLE_ID4, EXAMPLE_ID5, Exchange.PROTOCOL_BLUETOOTH, START_TIME_0, END_TIME_2, sl3, sl2);
    Exchange exchange2 = new Exchange(EXAMPLE_ID5, EXAMPLE_ID4, Exchange.PROTOCOL_BLUETOOTH, START_TIME_1, END_TIME_2, sl2, sl3);

    // Register two new clients.
    client.register(EXAMPLE_ID4, friends1);
    assertTrue(client.registrationWasSuccessful());
    client.register(EXAMPLE_ID5, friends1);
    assertTrue(client.registrationWasSuccessful());

    // Upload some exchanges for them and make sure we can get them back.
    client.updateExchange(exchange0);
    assertTrue(client.updateExchangeWasSuccessful());

    client.getPreviousExchanges(EXAMPLE_ID4);
    String response = client.get();
    ExperimentClient.GetPreviousExchangesResponse exchangesResponse;
    exchangesResponse = gson.fromJson(response, ExperimentClient.GetPreviousExchangesResponse.class);
    assertNotNull(exchangesResponse);
    assertTrue(exchangesResponse.OK());
    Exchange[] gottenExchanges = exchangesResponse.getExchanges();
    assertEquals(1, gottenExchanges.length);
    assertEquals(exchange0, gottenExchanges[0]);

    client.updateExchange(exchange1);
    assertTrue(client.updateExchangeWasSuccessful());

    client.getPreviousExchanges(EXAMPLE_ID4);
    response = client.get();
    exchangesResponse = gson.fromJson(response, ExperimentClient.GetPreviousExchangesResponse.class);
    assertNotNull(exchangesResponse);
    assertTrue(exchangesResponse.OK());
    gottenExchanges = exchangesResponse.getExchanges();
    assertEquals(2, gottenExchanges.length);
    assertEquals(exchange0, gottenExchanges[0]);
    assertEquals(exchange1, gottenExchanges[1]);
  }


  /**
   * Test that phones that are close enough nearby are detected.
   */
  @Test
  public void testGetNearbyPhones() throws InterruptedException, ExecutionException {
    Location location0 = new Location("Provider1");
    Location location1 = new Location("Provider2");
    SerializableLocation sl0 = new SerializableLocation(location0);
    SerializableLocation sl1 = new SerializableLocation(location1);
    sl0.latitude = 45.0;
    sl0.longitude = 45.0;
    sl1.latitude = 55.0;
    sl1.longitude = 55.0;

    SerializableLocation[] locs0 = {sl0};
    SerializableLocation[] locs1 = {sl1};

    // Register two new clients.
    client.register(EXAMPLE_ID6, friends1);
    assertTrue(client.registrationWasSuccessful());
    client.register(EXAMPLE_ID7, friends1);
    assertTrue(client.registrationWasSuccessful());

    client.updateLocations(EXAMPLE_ID6, locs0);
    client.updateLocations(EXAMPLE_ID7, locs1);

    final int RADIUS_TOO_SMALL = 1;
    final int RADIUS_ALSO_TOO_SMALL = 10;
    final int RADIUS_LARGE_ENOUGH = 100000; 
    assertEquals(0, getPhones(EXAMPLE_ID6, RADIUS_TOO_SMALL).length);
    assertEquals(0, getPhones(EXAMPLE_ID6, RADIUS_ALSO_TOO_SMALL).length);
    String[] phones = getPhones(EXAMPLE_ID6, RADIUS_LARGE_ENOUGH);
    assertEquals(1, phones.length);
    assertEquals(EXAMPLE_ID7, phones[0]);
  } 

  /**
   * Utility method that performs a getNearbyPhones request.
   */
  private String[] getPhones(String phoneid, int radius) 
                            throws InterruptedException, ExecutionException {
    client.getNearbyPhones(phoneid, radius);
    String response = client.get();
    ExperimentClient.GetNearbyPhonesResponse phonesResponse;
    phonesResponse = gson.fromJson(response, ExperimentClient.GetNearbyPhonesResponse.class);
    assertNotNull(phonesResponse);
    assertTrue(phonesResponse.OK());
    return phonesResponse.getPhones();
  }
}
