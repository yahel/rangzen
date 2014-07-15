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

import android.content.BroadcastReceiver; 
import android.content.Context;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.WpsInfo;
import android.app.Service;
import android.os.Bundle;
import android.os.IBinder;

import java.io.IOException;
import java.io.OptionalDataException;
import java.io.StreamCorruptedException;
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

    mStartTime = new Date();
    mBackgroundTaskRunCount = 0;

    mLocationStore = new LocationStore(this, StorageBase.ENCRYPTION_DEFAULT);

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
