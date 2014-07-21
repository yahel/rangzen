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

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver; 
import android.content.Context;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.app.Service;
import android.os.Bundle;
import android.os.IBinder;

import java.io.IOException;
import java.io.OptionalDataException;
import java.io.StreamCorruptedException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.Date;

/**
 * Core service of the Rangzen app. Started at startup, remains alive
 * indefinitely to perform the background tasks of Rangzen.
 */
public class RangzenService extends Service {
  /** Host of the experiment server. */
  public static final String EXPERIMENT_SERVER_HOSTNAME = "http://s.rangzen.io";

  /** Listening port of the experiment server. */
  public static final int EXPERIMENT_SERVER_PORT = 1337;

  /** 
   * String designating the provider we want to use (GPS) for location. 
   * We need the accuracy GPS provides - network based location is accurate to
   * within like a mile, according to the docs.
   */
  private static final String LOCATION_GPS_PROVIDER = LocationManager.GPS_PROVIDER;

  /** Time between location updates in milliseconds - 1 minute. */
  private static final long LOCATION_UPDATE_INTERVAL = 1000 * 60 * 1;

  /**
   * Minimum moved distance between location updates. We want a new location
   * even if we haven't moved, so we set it to 0.
   * */
  private static final float LOCATION_UPDATE_DISTANCE_MINIMUM = 0;

  /** The running instance of RangzenService. */
  protected static RangzenService sRangzenServiceInstance;

  /** For app-local broadcast and broadcast reception. */
  private LocalBroadcastManager mLocalBroadcastManager;

  /** Executes the background thread periodically. */
  private ScheduledExecutorService mScheduleTaskExecutor; 

  /** The time at which this instance of the service was started. */
  private Date mStartTime;

  /** The number of times that backgroundTasks() has been called. */
  private int mBackgroundTaskRunCount;

  /** A handle to the Android location manager. */
  private LocationManager mLocationManager;

  /** Handle to Rangzen location storage provider. */
  private LocationStore mLocationStore;

  /** Handle to Rangzen key-value storage provider. */
  private StorageBase mStore;

  /** Client for contacting experimental server. */
  private ExperimentClient mExperimentClient;

  /** Handle to Rangzen contacts getter/storer. */
  private ContactsGetterStorer mContactsGetterStorer;

  /** Display intro screen = service does nothing. */
  public static final String EXP_STATE_START = "START";
  /** Opted-in = but not yet registered with server. */
  public static final String EXP_STATE_NOT_YET_REGISTERED = "NOT_REGISTERED";
  /** Registered = GPS/BT on. */
  public static final String EXP_STATE_ON = "ON";
  /** GPS is off; service waits for GPS to turn back on. */
  public static final String EXP_STATE_PAUSED_NO_GPS = "PAUSED_NO_GPS";
  /** Bluetooth is off; service records location =  */
  public static final String EXP_STATE_PAUSED_NO_BLUETOOTH = "PAUSED_NO_BLUETOOTH";
  /** User opted out after opting in. Display intro screen. waits for BT to turn back on. */
  public static final String EXP_STATE_OPTED_OUT = "OPTED_OUT";
  /** The key under which the experiment state is stored in the StorageBase. */
  public static final String EXPERIMENT_STATE_KEY = "EXPERIMENT_STATE_KEY";

  /** 
   * If registration fails, we store the way it failed under this key.
   * 
   * TODO(lerner): Add status codes for failure reasons.
   * TODO(lerner): Retry differently given each status code.
  */
  public static final String REGISTRATION_FAILURE_REASON_KEY = "REGISRATION_FAILURE_REASON_KEY";

  /** Error code indicating a NoSuchAlgorithmException was rasied by the Contacts getter. */
  private static final String REGISTRATION_FAILED_BAD_ALGORITHM = "NoAlgorithmException";
  /** Contacts weren't stored even after the ContactsGetterStorer tried. */
  private static final String REGISTRATION_FAILED_NULL_CONTACTS = "Null contacts after retrieval";
  /** Phone ID was null. */
  private static final String REGISTRATION_FAILED_NULL_PHONE_ID = "Null phone id";
  /** Success in registration. */
  private static final String REGISTRATION_SUCCESS = "Success";
  /** Got contacts and phoneid, but contacting the server failed, or it answered "failed". */
  private static final String REGISTRATION_FAILED_SERVER_NOT_OK = "Server response not OK";

  /** Current state of the experiment. */
  private String experimentState;

  /** Android Log Tag. */
  private static String TAG = "RangzenService";

  /**
   * Called whenever the service is requested to start. If the service
   * is already running, this does /not/ create a new instance of the service.
   * Rather, onStartCommand is called again on the existing instance.
   *
   * @param intent The intent passed to startService to start this service.
   * @param flags Flags about the request to start the service.
   * @param startid A unique integer representing this request to start.
   * @see android.app.Service
   */
  @Override
  public int onStartCommand(Intent intent, int flags, int startid) {
    Log.i(TAG, "RangzenService onStartCommand.");

    experimentState = getExperimentState();
  
    // Returning START_STICKY causes Android to leave the service running
    // even when the foreground activity is closed.
    return START_STICKY;
  }

  /**
   * Called the first time the service is started.
   *
   * @see android.app.Service
   */
  @Override
  public void onCreate() {
    Log.i(TAG, "RangzenService created.");
    sRangzenServiceInstance = this;

    mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);

    mExperimentClient = new ExperimentClient(EXPERIMENT_SERVER_HOSTNAME, 
                                             EXPERIMENT_SERVER_PORT);

    mStartTime = new Date();
    mBackgroundTaskRunCount = 0;

    mLocationStore = new LocationStore(this, StorageBase.ENCRYPTION_DEFAULT);

    mStore = new StorageBase(this, StorageBase.ENCRYPTION_DEFAULT);

    mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    registerForLocationUpdates();

    // Schedule the background task thread to run occasionally.
    mScheduleTaskExecutor = Executors.newScheduledThreadPool(1);
    mScheduleTaskExecutor.scheduleWithFixedDelay(new Runnable() {
      public void run() {
        backgroundTasks();
      }
    }, 0, 1, TimeUnit.SECONDS); 
  }

  /**
   * Method called periodically on a background thread to perform
   * Rangzen's background tasks.
   */
  public void backgroundTasks() {

    experimentState = getExperimentState();
    if (experimentState.equals(EXP_STATE_NOT_YET_REGISTERED)) {
      String status = mStore.get(REGISTRATION_FAILURE_REASON_KEY);
      if (status == null) {
        String newStatus = registerWithExperimentServer();
        mStore.put(REGISTRATION_FAILURE_REASON_KEY, newStatus);
        if (newStatus == REGISTRATION_SUCCESS) {
          mStore.put(EXPERIMENT_STATE_KEY, EXP_STATE_ON);
        }
      }
    }

    mBackgroundTaskRunCount++;

    PeerManager.getInstance(getApplicationContext()).tasks(); 
    
    Log.v(TAG, "Background Tasks Finished");
  }

  /**
   * Return the number of times that background tasks have been executed
   * since the service was started.
   *
   * @return The number of times backgroundTasks() has been called.
   */
  public int getBackgroundTasksRunCount() {
    return mBackgroundTaskRunCount;
  }

  /**
   * Return the current state of the experiment, as stored in the StorageBase.
   *
   * @return The current experiment state from among the choices defined in this
   * class (START, PRE_REGISTRATION, ON, etc.)
   */
  public String getExperimentState() {
    String state = mStore.get(EXPERIMENT_STATE_KEY);
    if (state == null) {
      Log.wtf(TAG, "Experiment state should never be null in RangzenService!");
      return EXP_STATE_START;
    } else {
      return state;
    }

  }

  /**
   * Register with the experiment server.
   */
  private String registerWithExperimentServer() {
    try { 
      if (mContactsGetterStorer == null) {
        mContactsGetterStorer = new ContactsGetterStorer(this);
      }
    } catch (NoSuchAlgorithmException e) {
       Log.e(TAG, "Couldn't create ContactsGetterStorer, aborting registration. " + e);
       return REGISTRATION_FAILED_BAD_ALGORITHM;
    }

    Set<String> contacts = mContactsGetterStorer.getObfuscatedPhoneNumbers();
    if (contacts == null) {
      Log.i(TAG, "Contacts are null; retrieving and storing them.");
      mContactsGetterStorer.retrieveAndStoreContacts();
    }

    contacts = mContactsGetterStorer.getObfuscatedPhoneNumbers();
    if (contacts == null) {
      Log.e(TAG, "Couldn't retrieve contacts, not even an empty list.");
      return REGISTRATION_FAILED_NULL_CONTACTS;
    } else {
      Log.i(TAG, String.format("Retrieved %d contacts.", contacts.size()));
    }

    String phoneid = getPhoneID();
    if (phoneid == null) {
      return REGISTRATION_FAILED_NULL_PHONE_ID;
    } 

    mExperimentClient.register(phoneid, contacts.toArray(new String[0])); 
    if (mExperimentClient.registrationWasSuccessful()) {
      Log.i(TAG, String.format("id: %s registered successfully with %d friends.",
                               phoneid, contacts.size()));
      return REGISTRATION_SUCCESS;
    } else {
      Log.e(TAG, "Registration failed, server answer was not OK!");
      return REGISTRATION_FAILED_SERVER_NOT_OK;
    }
  }

  /**
   * Get an ID for this phone which can be used to register and identify the phone
   * to the server and other phones. Currently, a Bluetooth MAC address is use.
   *
   * @return A string to be used as the ID for this phone.
   */
  private String getPhoneID() {
    // TODO(lerner): Move this to the BluetoothSpeaker.
    // TODO(lerner): Ensure this can't fail.
    Log.v(TAG, "Getting default adapter");
    BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
    Log.v(TAG, "Got default adapter, it's" + adapter);
    if (adapter != null) {
      Log.i(TAG, "Getting adapter address");
      String address = adapter.getAddress();
      Log.i(TAG, "Got adapter address");
      return adapter.getAddress();
    } else {
      Log.e(TAG, "Device doesn't support Bluetooth, can't get address for phone ID.");
      return null;
    }
  }

  /**
   * Register to receive regular updates of the phone's location.
   */
  private void registerForLocationUpdates() {
     if (mLocationManager == null) { 
       Log.e(TAG, "Can't register for location updates; location manager is null.");
       return;
     }
     mLocationManager.requestLocationUpdates(LOCATION_GPS_PROVIDER,
                                             LOCATION_UPDATE_INTERVAL,
                                             LOCATION_UPDATE_DISTANCE_MINIMUM,
                                             mLocationListener);
     Log.i(TAG, "Registered for location every " + LOCATION_UPDATE_INTERVAL + "ms");
  }

  private LocationListener mLocationListener = new LocationListener() {
    @Override
    public void onLocationChanged(Location location) {
      Log.d(TAG, "Got location: " + location);
      SerializableLocation serializableLocation = new SerializableLocation(location);
      mLocationStore.addLocation(serializableLocation);

      List<SerializableLocation> locations;
      try {
        // TODO(lerner): Report to the server if we're failing at storing locations?
        locations = mLocationStore.getAllLocations();
        for (SerializableLocation sl : locations) {
          Log.d(TAG, sl.toString());
        }
      } catch (IOException | ClassNotFoundException e) {
        Log.e(TAG, "Not able to store location!");
        e.printStackTrace();
      }
    }

    @Override
    public void onProviderDisabled(String provider) {
      Log.d(TAG, "Provider disabled: " + provider);
    }

    @Override
    public void onProviderEnabled(String provider) {
      Log.d(TAG, "Provider enabled: " + provider);

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
      Log.d(TAG, "Provider " + provider + " status changed to status " + status);
    }

  };

  
  /**
   * Get the time at which this instance of the service was started.
   *
   * @return A Date representing the time at which the service was started.
   */
  public Date getServiceStartTime() {
    return mStartTime;
  }

  /**
   * This method has to be implemented on a service, but I haven't written
   * the service with binding in mind. Unsure what would happen if it were
   * used this way.
   *
   * @param intent The intent used to bind the service (passed to
   * Context.bindService(). Extras included in the intent will not be visible
   * here.
   * @return A communication channel to the service. This implementation just
   * returns null.
   * @see android.app.Service
   *
   */
  @Override
  public IBinder onBind(Intent intent) {
    // This service is not meant to be used through binding.
    return null;
  }
}
