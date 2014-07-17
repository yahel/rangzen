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

import com.github.kevinsawicki.http.HttpRequest;
import com.github.kevinsawicki.http.HttpRequest.HttpRequestException;

import com.google.gson.Gson;

import android.os.AsyncTask;
import android.util.Log;

/**
 * Class that interfaces with the experimental server, enabling the app to
 * send and receive information about its friends, location and exchanges.
 *
 * TODO(lerner): Add utility methods which block, perform the request, then return
 * the results of the request in an easy form.
 * TODO(lerner): Add utility methods which parse the output of the server.
 */
public class ExperimentClient extends AsyncTask<String, Integer, String> {
  /** Included in Android Log messages. */
  private final static String TAG = "ExperimentClient";

  /** URL-ending for registering a new phone. */
  private static final String REGISTER_PHONE_RESOURCE = "register_phone";
  /** URL-ending for getting nearby phones. */
  private static final String GET_NEARBY_PHONES_RESOURCE = "get_nearby_phones";
  /** URL-ending for retrieving my friends. */
  private static final String GET_FRIENDS_RESOURCE = "get_friends";
  /** URL-ending for sending a location to the server. */
  private static final String UPDATE_LOCATIONS_RESOURCE = "update_locations";
  /** URL-ending for getting the locations I've previously reported. */
  private static final String GET_PREVIOUS_LOCATIONS_RESOURCE = "get_previous_locations";
  /** URL-ending for reporting an exchange to the server. */
  private static final String UPDATE_EXCHANGE_RESOURCE = "update_exchange";
  /** URL-ending for getting the exchanges I've previously reported. */
  private static final String GET_PREVIOUS_EXCHANGES_RESOURCE = "get_previous_exchanges";
  /** URL-ending for resetting all data on the server. */
  private static final String RESET_RESOURCE = "reset";

  /** The server's name/IP. */
  private String host;

  /** The server's port. */
  private int port;

  /** GSON object for conversions between JSON and Java objects. */
  private final Gson gson = new Gson();

  /**
   * Create a new experiment client to talk to the server at the given host/port.
   *
   * @param host The hostname/URL/IP of the server.
   * @param port The port on which the server is listening.
   */
  public ExperimentClient(String host, int port) {
    this.host = host;
    this.port = port;
  }

  /**
   * Class representing a simple yes/no response from the server, parseable
   * from the JSON that the server returns.
   */
  /* package */ class SimpleResponse {
    /** The status code returned by the server. */
    private String status;

    /**
     * True if the status == "ok", false otherwise.
     *
     * @return True if status of this response is "ok".
     */
    public boolean OK() {
      return "ok".equals(status);
    }
  }

  /**
   * Class representing a simple payload where the only parameter we must provide
   * is our phoneid.
   */
  private class SimplePayload {
    /** The local phone's ID. */
    private String phoneid;

    /** 
     * Create a new simple payload with just the given phone ID. 
     *
     * @param phoneid The local phone's ID.
     */
    public SimplePayload(String phoneid) {
      this.phoneid = phoneid;
    }
  }

  /**
   * Represents the data we send to the server when registering a new phone.
   */
  private class RegistrationPayload {
    /** The local phone's ID. */
    private String phoneid;
    /** The local phone's friends. */
    private String[] friends;

    /**
     * Create a new payload for registering this phone. 
     *
     * @param phoneid The local phone's ID to register with the server.
     * @param friends The local phone's friends.
     */
    /* package */ RegistrationPayload(String phoneid, String[] friends) {
      this.phoneid = phoneid;
      this.friends = friends;
    }
  }

  /**
   * Register the given phoneid (with its corresponding friends) with the server.
   *
   * @param phoneid The phone ID to register.
   * @param friends The friends to register as friends of the given ID.
   */
  public void register(String phoneid, String[] friends) {
    RegistrationPayload registrationData = new RegistrationPayload(phoneid, friends);
    String json = gson.toJson(registrationData);
    execute(REGISTER_PHONE_RESOURCE, json);
  }

  /**
   * Class representing a list of friends returned from the server.
   */
  /* package */ class GetFriendsResponse {
    /** Status code of the request (ok or failed). */
    private String status;
    /** List of friend device IDs. */
    private String[] friends;

    /**
     * True if the status == "ok", false otherwise.
     *
     * @return True if status of this response is "ok".
     */
    public boolean OK() {
      return "ok".equals(status);
    }

    /**
     * Get the friends returned in this response.
     *
     * @return The list of friends given by the server in this response.
     */
    public String[] getFriends() {
      return friends;
    }
  }

  /**
   * Tell the client to make a request to get the friends of the given device.
   *
   * @param phoneid The phone ID whose friends we're retrieving.
   */
  public void getFriends(String phoneid) {
    SimplePayload payload = new SimplePayload(phoneid);
    String json = gson.toJson(payload);
    execute(GET_FRIENDS_RESOURCE, json);
  }

  /**
   * Payload for a request to insert one or more locations.
   */
  private class UpdateLocationsPayload {
    /** ID of the phone for whom we're giving locations. */
    private String phoneid;
    /** Locations that phone has been. */
    private SerializableLocation[] locations;

    /**
     * Create a new payload for a request to add locations. 
     *
     * @param phoneid ID of the phone for whom we're giving locations.
     * @param locations The locations the phone has been.
     */
    public UpdateLocationsPayload(String phoneid, SerializableLocation[] locations) {
      this.phoneid = phoneid;
      this.locations = locations;
    }
  }

  /**
   * Tell the client to make a request to send locations to the server.
   */
  public void updateLocations(String phoneid, SerializableLocation[] locations) {
    UpdateLocationsPayload payload = new UpdateLocationsPayload(phoneid, locations);
    String json = gson.toJson(payload);
    execute(UPDATE_LOCATIONS_RESOURCE, json);
  }

  /**
   * Tell the client to make a request to send the given exchange to the server.
   *
   * @param exchange The exchange we're reporting.
   */
  public void updateExchange(Exchange exchange) {
    String json = gson.toJson(exchange);
    execute(UPDATE_EXCHANGE_RESOURCE, json);
  }
  
  /**
   * Represents a response from the server to a request for previous exchanges.
   */
  /* package */ class GetPreviousExchangesResponse {
    /** The status code returned by the server. */
    private String status;
    /** Exchanges we've previously uploaded. */
    private Exchange[] exchanges;

    /**
     * True if the status == "ok", false otherwise.
     *
     * @return True if status of this response is "ok".
     */
    public boolean OK() {
      return "ok".equals(status);
    }

    /** 
     * Get the exchanges included in this response.
     *
     * @return The exchanges the server returned to us.
     */
    public Exchange[] getExchanges() {
      return exchanges;
    }
  }

  /**
   * Tell the client to make a request to get previous exchanges we've reported.
   *
   * @param exchange The phone we want the exchanges for.
   */
  public void getPreviousExchanges(String phoneid) {
    SimplePayload payload = new SimplePayload(phoneid);
    String json = gson.toJson(payload);
    execute(GET_PREVIOUS_EXCHANGES_RESOURCE, json);
  }

  /** 
   * Represents a response to a request for previous locations.
   */
  /* package */ class GetPreviousLocationsResponse {
    /** The status code returned by the server. */
    private String status;
    /** The locations we've been. */
    private SerializableLocation[] locations;

    /**
     * True if the status == "ok", false otherwise.
     *
     * @return True if status of this response is "ok".
     */
    public boolean OK() {
      return "ok".equals(status);
    }

    /**
     * Get the locations included in the response.
     *
     * @return The locations the server returned to us.
     */
    public SerializableLocation[] getLocations() {
      return locations;
    }
  }

  /**
   * Tell the client to make a request to get previous locations we've reported.
   *
   * @param exchange The phone we want the locations for.
   */
  public void getPreviousLocations(String phoneid) {
    SimplePayload payload = new SimplePayload(phoneid);
    String json = gson.toJson(payload);
    execute(GET_PREVIOUS_LOCATIONS_RESOURCE, json);
  }

  /**
   * Payload for a request to get nearby phones.
   */
  private class GetNearbyPhonesPayload {
    /** Phone ID of the device making the request. */
    private String phoneid;
    /** Radius in kilometers that we're interested in. */
    private float distance;

    /**
     * Create a new payload for getting nearby phones.
     *
     * @param phoneid The local phone's ID.
     * @param distance The radius in km of the area where we want to know about other phones.
     */
    public GetNearbyPhonesPayload(String phoneid, float distance) {
      this.phoneid = phoneid;
      this.distance = distance;
    }
  }

  /**
   * Represents a response to a request for nearby phones.
   */
  /* package */ class GetNearbyPhonesResponse {
    /** The status code returned by the server. */
    private String status;
    /** The phones within the radius that was requested. */
    private String[] phones;

    /**
     * True if the status == "ok", false otherwise.
     *
     * @return True if status of this response is "ok".
     */
    public boolean OK() {
      return "ok".equals(status);
    }

    /**
     * Get the phones returned by the server that are within the radius requested.
     */
    public String[] getPhones() {
      return phones;
    }
  }

  /**
   * Request that the client ask the server for all phones within the given distance
   */
  public void getNearbyPhones(String phoneid, float distance) {
    GetNearbyPhonesPayload payload = new GetNearbyPhonesPayload(phoneid, distance);
    String json = gson.toJson(payload);
    execute(GET_NEARBY_PHONES_RESOURCE, json);
  }

  /**
   * Request that the client ask the server to delete all its data.
   * Used for testing idempotency.
   */
  public void resetServer() {
    execute(RESET_RESOURCE, "{}");
  }

  /**
   * This class makes all requests asynchronously as an AsyncTask. This is the
   * method which actually performs the request. It returns a String representing
   * the entire result, which can be obtained by calling ExperimentClient.get()
   * after starting a request with any of the above public methods (such as 
   * getNearbyPhones() or register()).
   */
  @Override
  protected String doInBackground(String... methodAndPayload) {
    String method = methodAndPayload[0];
    String payload = methodAndPayload[1];
    String url = String.format("%s:%d/%s", host, port, method);
    try {
      HttpRequest request =  HttpRequest.post(url)
                                        .acceptJson()
                                        .contentType(HttpRequest.CONTENT_TYPE_JSON)
                                        .send(payload);
      StringBuilder responseBody = new StringBuilder();
      request.receive(responseBody);
      return responseBody.toString();
    } catch (HttpRequestException exception) {
      return null;
    }
  }

  /**
   * This method receives updates on the amount of data retrieved. Currently
   * it only receives one single update at the end of the download of the request.
   */
  @Override
  protected void onProgressUpdate(Integer... progress) { }

  /**
   * This method is called after doInBackground is finished updating. It currently
   * does nothing. To retrieve the data gotten by a request, call ExperimentClient.get().
   */
  @Override
  protected void onPostExecute(String responseBody) {}
}
