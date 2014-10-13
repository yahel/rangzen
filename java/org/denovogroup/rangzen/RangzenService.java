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
import android.bluetooth.BluetoothSocket;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.content.Context;
import android.content.Intent;
import android.app.Service;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.Date;
import java.util.List;

/**
 * Core service of the Rangzen app. Started at startup, remains alive
 * indefinitely to perform the background tasks of Rangzen.
 */
public class RangzenService extends Service {
    /** The running instance of RangzenService. */
    protected static RangzenService sRangzenServiceInstance;

    /** For app-local broadcast and broadcast reception. */
    private LocalBroadcastManager mLocalBroadcastManager;

    /** Executes the background thread periodically. */
    private ScheduledExecutorService mScheduleTaskExecutor;

    /** Cancellable scheduling of backgroundTasks. */
    private ScheduledFuture mBackgroundExecution;

    /** Handle to app's PeerManager. */
    private PeerManager mPeerManager;

    /** The time at which this instance of the service was started. */
    private Date mStartTime;

    /** The number of times that backgroundTasks() has been called. */
    private int mBackgroundTaskRunCount = 0;

    /** Handle to Rangzen key-value storage provider. */
    private StorageBase mStore;

    /** Wifi Direct Speaker used for Wifi Direct name based RSVP. */
    private WifiDirectSpeaker mWifiDirectSpeaker;

    /** Whether we're currently attempting a connection to another device over BT. */
    private boolean connecting = false;

    /** The BluetoothSpeaker for the app. */
    private static BluetoothSpeaker mBluetoothSpeaker;

    /** When announcing our address over Wifi Direct name, prefix this string to our MAC. */
    public final static String RSVP_PREFIX = "RANGZEN-";

    /** Android Log Tag. */
    private final static String TAG = "RangzenService";

    /**
     * Called whenever the service is requested to start. If the service is
     * already running, this does /not/ create a new instance of the service.
     * Rather, onStartCommand is called again on the existing instance.
     * 
     * @param intent
     *            The intent passed to startService to start this service.
     * @param flags
     *            Flags about the request to start the service.
     * @param startid
     *            A unique integer representing this request to start.
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

        mPeerManager = PeerManager.getInstance(this);
        mBluetoothSpeaker = new BluetoothSpeaker(this, mPeerManager);
        mPeerManager.setBluetoothSpeaker(mBluetoothSpeaker);

        mStartTime = new Date();

        mStore = new StorageBase(this, StorageBase.ENCRYPTION_DEFAULT);

        mWifiDirectSpeaker = new WifiDirectSpeaker(this, 
                                                   mPeerManager, 
                                                   mBluetoothSpeaker,
                                                   new WifiDirectFrameworkGetter());

        String btAddress = mBluetoothSpeaker.getAddress();
        mWifiDirectSpeaker.setWifiDirectUserFriendlyName(RSVP_PREFIX + btAddress);
        mWifiDirectSpeaker.setmSeekingDesired(true);

        // Schedule the background task thread to run occasionally.
        mScheduleTaskExecutor = Executors.newScheduledThreadPool(1);
        // TODO(lerner): Decide if 1 second is an appropriate time interval for
        // the tasks.
        mBackgroundExecution = mScheduleTaskExecutor.scheduleAtFixedRate(new Runnable() {
            public void run() {
                backgroundTasks();
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    /**
     * Called when the service is destroyed.
     */
    public void onDestroy() {
      mBackgroundExecution.cancel(true);
      return;
    }

    /**
     * Method called periodically on a background thread to perform Rangzen's
     * background tasks.
     */
    public void backgroundTasks() {
        Log.v(TAG, "Background Tasks Started");

        PeerManager peerManager = PeerManager.getInstance(getApplicationContext());
        peerManager.tasks();
        mBluetoothSpeaker.tasks();
        mWifiDirectSpeaker.tasks();

        List<Peer> peers = peerManager.getPeers();
        if (!connecting && peers.size() > 0) {
          Peer peer = peers.get(0);
          connectTo(peer);
        }
        mBackgroundTaskRunCount++;


        Log.v(TAG, "Background Tasks Finished");
    }

    /**
     * Demo method for how we might use the BluetoothSpeaker.connect() call.
     *
     * @param peer The peer we want to talk to.
     */
    public void connectTo(Peer peer) {
      // Based on MAC addresses, only one device starts exchanges bewteen any
      // pair of devices. Don't start one if we're not the chosen one in this pair.
      PeerManager peerManager = PeerManager.getInstance(this);
      try {
        if (!peerManager.thisDeviceSpeaksTo(peer)) {
          return; 
        }
      } catch (NoSuchAlgorithmException e) {
        Log.e(TAG, "No such algorithm for hashing in thisDeviceSpeaksTo!? " + e);
        return;
      } catch (UnsupportedEncodingException e) {
        Log.e(TAG, "Unsupported encoding exception in thisDeviceSpeaksTo!?" + e);
        return;
      }
      connecting = true;
      Log.i(TAG, "Starting to connect to " + peer.toString());
      mBluetoothSpeaker.connect(peer, new PeerConnectionCallback() {
        @Override
        public void success(BluetoothSocket socket) {
          Log.i(TAG, "Callback says we're connected to " + socket.getRemoteDevice().toString());
          if (socket.isConnected()) {
            Log.i(TAG, "In fact, the socket says it's connected.");
          } else {
            Log.w(TAG, "But the socket claims not to be connected!");
          }
          try {
            socket.close();
          } catch (IOException e) {
            Log.e(TAG, "IOException while closing BluetoothSocket: " + e);
          }
          connecting = false;
        }
        @Override
        public void failure(String reason) {
          Log.i(TAG, "Callback says we failed to connect: " + reason);
          connecting = false;
        }
      });
    }

    /**
     * Check whether any network connection (Wifi/Cell) is available according
     * to the OS's connectivity service.
     * 
     * @return True if any network connection seems to be available, false
     *         otherwise.
     */
    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = cm.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    /**
     * Return true if Bluetooth is turned on, false otherwise.
     * 
     * @return Whether Bluetooth is enabled.
     */
    private boolean isBluetoothOn() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            return false;
        } else {
            return adapter.isEnabled();
        }
    }

    /**
     * Return the number of times that background tasks have been executed since
     * the service was started.
     * 
     * @return The number of times backgroundTasks() has been called.
     */
    public int getBackgroundTasksRunCount() {
        return mBackgroundTaskRunCount;
    }
  
    /**
     * Get the time at which this instance of the service was started.
     * 
     * @return A Date representing the time at which the service was started.
     */
    public Date getServiceStartTime() {
        return mStartTime;
    }

    /**
     * This method has to be implemented on a service, but I haven't written the
     * service with binding in mind. Unsure what would happen if it were used
     * this way.
     * 
     * @param intent
     *            The intent used to bind the service (passed to
     *            Context.bindService(). Extras included in the intent will not
     *            be visible here.
     * @return A communication channel to the service. This implementation just
     *         returns null.
     * @see android.app.Service
     * 
     */
    @Override
    public IBinder onBind(Intent intent) {
        // This service is not meant to be used through binding.
        return null;
    }
}
