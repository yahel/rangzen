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
import android.bluetooth.BluetoothDevice;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.app.Service;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.Date;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

/**
 * Core service of the Rangzen app. Started at startup, remains alive
 * indefinitely to perform the background tasks of Rangzen.
 */
public class RangzenService extends Service implements
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener {
    /** Host of the experiment server. */
    public static final String EXPERIMENT_SERVER_HOSTNAME = "http://s.rangzen.io";

    /** Listening port of the experiment server. */
    public static final int EXPERIMENT_SERVER_PORT = 1337;

    /** Time between location updates in milliseconds - 1 minute. */
    private static final long LOCATION_UPDATE_INTERVAL = 1000 * 60 * 1;

    /** Minimum interval in milliseconds we want to receive location fixes. */
    private static final long LOCATION_FASTEST_INTERVAL = 1000 * 60 * 1;

    /**
     * Minimum moved distance between location updates. We want a new location
     * even if we haven't moved, so we set it to 0.
     * */
    private static final float LOCATION_UPDATE_DISTANCE_MINIMUM = 0;

    /** The running instance of RangzenService. */
    protected static RangzenService sRangzenServiceInstance;

    /** Last time we sent our locations to the server. */
    private Date lastLocationUpdate;

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
    private int mBackgroundTaskRunCount;

    /** A request specifying our desires for location updates. */
    private LocationRequest mLocationRequest;

    /** Client to the Google Play location API. */
    private LocationClient mLocationClient;

    /** Handle to Rangzen location storage provider. */
    private LocationStore mLocationStore;

    /** Handle to Rangzen exchange storage provider. */
    private ExchangeStore mExchangeStore;

    /** Handle to Rangzen key-value storage provider. */
    private StorageBase mStore;

    /** Handle to Rangzen contacts getter/storer. */
    private ContactsGetterStorer mContactsGetterStorer;

    /** Display intro screen = service does nothing. */
    public static final String EXP_STATE_START = "START";
    /** Opted-in and registered with server, but not yet determined ON/PAUSED. */
    public static final String EXP_STATE_REGISTERED = "REGISTERED";
    /** Opted-in = but not yet registered with server. */
    public static final String EXP_STATE_NOT_YET_REGISTERED = "NOT_REGISTERED";
    /** Registered = GPS/BT on. */
    public static final String EXP_STATE_ON = "ON";
    /** GPS is off; service waits for GPS to turn back on. */
    public static final String EXP_STATE_PAUSED_NO_LOCATION = "PAUSED_NO_LOCATION";
    /**
     * Bluetooth is off; service records location, waits for BT to turn back on.
     */
    public static final String EXP_STATE_PAUSED_NO_BLUETOOTH = "PAUSED_NO_BLUETOOTH";
    /** User opted out after opting in. Display intro screen. */
    public static final String EXP_STATE_OPTED_OUT = "OPTED_OUT";
    /** The key under which the experiment state is stored in the StorageBase. */
    public static final String EXPERIMENT_STATE_KEY = "EXPERIMENT_STATE_KEY";

    /**
     * The key under which the higher numbered location had has been transmitted
     * is stored.
     */
    public static final String HIGHEST_LOCATION_UPLOADED_KEY = "HIGHEST_LOCATION";

    /** If registration fails, we store the way it failed under this key. */
    public static final String REGISTRATION_FAILURE_REASON_KEY = "REGISRATION_FAILURE_REASON_KEY";

    /**
     * Error code indicating a NoSuchAlgorithmException was rasied by the
     * Contacts getter.
     */
    private static final String REGISTRATION_FAILED_BAD_ALGORITHM = "NoAlgorithmException";
    /** Contacts weren't stored even after the ContactsGetterStorer tried. */
    private static final String REGISTRATION_FAILED_NULL_CONTACTS = "Null contacts after retrieval";
    /** Phone ID was null. */
    private static final String REGISTRATION_FAILED_TIMEOUT = "Timeout during HTTP";
    /** Phone ID was null. */
    private static final String REGISTRATION_FAILED_NULL_PHONE_ID = "Null phone id";
    /** Success in registration. */
    private static final String REGISTRATION_SUCCESS = "Success";
    /**
     * Got contacts and phoneid, but contacting the server failed, or it
     * answered "failed".
     */
    private static final String REGISTRATION_FAILED_SERVER_NOT_OK = "Server response not OK";

    /**
     * Whether we've updated nearby phones since our last location upload.
     * Starts true since we should wait until our first upload to check for
     * nearby phones.
     */
    private boolean haveRecentNearbyPhones = true;

    /** Current state of the experiment. */
    private String experimentState;

    /** True if location services are currently available, false otherwise. */
    private boolean mPlayServicesConnected = false;

    /** The BluetoothSpeaker for the app. */
    private static BluetoothSpeaker mBluetoothSpeaker;

    /** Android Log Tag. */
    private static String TAG = "RangzenService";

    /**
     * Distance in km of phones we want to know about from the server.
     * TODO(lerner): Decide on an appropriate value for this.
     */
    private static final float NEARBY_DISTANCE = (float) 0.25;

    /**
     * Key for storing the number of the highest exchange sequence number we've
     * uploaded.
     */
    private static final String HIGHEST_EXCHANGE_UPLOADED_KEY = "HIGHEST_EXCHANGE_KEY";

    /** Maximum number of locations or exchanges to upload at once. */
    private static final int UPLOAD_BATCH_SIZE = 100;

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
        mLocationStore = new LocationStore(this, StorageBase.ENCRYPTION_DEFAULT);
        mExchangeStore = new ExchangeStore(this, StorageBase.ENCRYPTION_DEFAULT);

        mPeerManager = PeerManager.getInstance(this);
        mPeerManager.setLocationStore(mLocationStore);
        mPeerManager.setExchangeStore(mExchangeStore);
        mBluetoothSpeaker = new BluetoothSpeaker(this, mPeerManager);
        mPeerManager.setBluetoothSpeaker(mBluetoothSpeaker);

        mStartTime = new Date();
        mBackgroundTaskRunCount = 0;

        mStore = new StorageBase(this, StorageBase.ENCRYPTION_DEFAULT);

        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(LOCATION_UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(LOCATION_FASTEST_INTERVAL);
        mLocationClient = new LocationClient(this, this, this);
        mLocationClient.connect();

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

        attemptRegistrationIfNecessary();
        if (isOptedInAndRegistered()) {
            updateLiveExperimentState();

            try {
                uploadUnsentLocations();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                Log.e(TAG,
                        "Uploading all unsent locations resulted in an exception: "
                                + e);
            }
            try {
                uploadUnsentExchanges();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                Log.e(TAG,
                        "Uploading all unsent exchanges resulted in an exception: "
                                + e);
            }

            if (!haveRecentNearbyPhones) {
                Log.d(TAG,
                        "Don't have nearby phones since last location update; asking!");
                getNearbyPhones();
            }
        }

        PeerManager.getInstance(getApplicationContext()).tasks();

        mBackgroundTaskRunCount++;
        Log.v(TAG, "Current experiment state: " + getExperimentState());
        Log.v(TAG, "Background Tasks Finished");
    }

    /**
     * Attempt to upload up to 100 locations that haven't been sent to the
     * server.
     * 
     * @throws ClassNotFoundException
     * @throws IOException
     * @throws OptionalDataException
     * @throws StreamCorruptedException
     */
    private void uploadUnsentLocations() throws StreamCorruptedException,
            OptionalDataException, IOException, ClassNotFoundException {
        if (!isNetworkAvailable()) {
            Log.d(TAG, "Not attempting to upload since network not available.");
            return;
        }
        int latestLocation = mLocationStore.getMostRecentSequenceNumber();
        int highestSentLocation = mStore.getInt(HIGHEST_LOCATION_UPLOADED_KEY,
                LocationStore.MIN_SEQUENCE_NUMBER - 1);
        // Do nothing if no locations are stored or all locations have been
        // uploaded.
        if (latestLocation == LocationStore.NO_SEQUENCE_STORED
                || highestSentLocation == latestLocation) {
            return;
        }

        int start = highestSentLocation + 1;
        int end = start + UPLOAD_BATCH_SIZE;
        if (end > latestLocation) {
            end = latestLocation;
        }

        List<SerializableLocation> locationsToSend = mLocationStore
                .getLocations(start, end);
        SerializableLocation[] locations = locationsToSend
                .toArray(new SerializableLocation[0]);

        ExperimentClient client = new ExperimentClient(
                EXPERIMENT_SERVER_HOSTNAME, EXPERIMENT_SERVER_PORT);
        client.updateLocations(getPhoneID(), locations);
        if (client.updateLocationsWasSuccessful()) {
            Log.d(TAG, String.format("Uploaded locations %d-%d", start, end));
            mStore.putInt(HIGHEST_LOCATION_UPLOADED_KEY, end);
            haveRecentNearbyPhones = false;
        } else {
            Log.e(TAG, String.format("Failed to upload locations %d-%d", start,
                    end));
        }
    }

    /**
     * Attempt to upload up to 100 exchanges that haven't been sent to the
     * server yet.
     * 
     * @throws ClassNotFoundException
     * @throws IOException
     * @throws OptionalDataException
     * @throws StreamCorruptedException
     */
    private void uploadUnsentExchanges() throws StreamCorruptedException,
            OptionalDataException, IOException, ClassNotFoundException {
        if (!isNetworkAvailable()) {
            Log.d(TAG, "Not attempting to upload since network not available.");
            return;
        }
        int latestExchange = mExchangeStore.getMostRecentSequenceNumber();
        int highestSentExchange = mStore.getInt(HIGHEST_EXCHANGE_UPLOADED_KEY,
                ExchangeStore.MIN_SEQUENCE_NUMBER - 1);
        // Do nothing if no exchanges are stored or all exchanges have been
        // uploaded.
        if (latestExchange == ExchangeStore.NO_SEQUENCE_STORED
                || highestSentExchange == latestExchange) {
            return;
        }

        int start = highestSentExchange + 1;
        int end = start + UPLOAD_BATCH_SIZE;
        if (end > latestExchange) {
            end = latestExchange;
        }

        // TODO(lerner): The server should support uploading multiple exchanges.
        // At that point we switch this over to using that API.
        // List<Exchange> exchangesToSend = mExchangeStore.getExchanges(start,
        // end);
        // Exchange[] exchanges = exchangesToSend.toArray(new Exchange[0]);

        for (int sequence = start; sequence <= end; sequence++) {
            Exchange exchange = mExchangeStore.getExchanges(sequence, sequence)
                    .get(0);
            ExperimentClient client = new ExperimentClient(
                    EXPERIMENT_SERVER_HOSTNAME, EXPERIMENT_SERVER_PORT);
            client.updateExchange(exchange);
            if (client.updateExchangeWasSuccessful()) {
                Log.d(TAG, String.format("Uploaded exchange %d", sequence));
                mStore.putInt(HIGHEST_EXCHANGE_UPLOADED_KEY, sequence);
            } else {
                Log.e(TAG,
                        String.format("Failed to upload exchange %d", sequence));
            }
        }
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
     * Check whether we're pre-registration and haven't previously failed in an
     * unrecoverable way. If so, try to register with the server by calling
     * registerWithExperimentServer().
     */
    private void attemptRegistrationIfNecessary() {
        if (isNotYetRegistered() && !hadUnrecoverableRegistrationFailure()) {
            // TODO(lerner): Exponential backoff instead of spamming.

            // Attempt registration and store the resulting status.
            // If it's a success, set experiment state to REGISTERED.
            putRegistrationFailureStatus(registerWithExperimentServer());
            if (REGISTRATION_SUCCESS.equals(getRegistrationFailureStatus())) {
                putExperimentState(EXP_STATE_REGISTERED);
            }
        }
    }

    /**
     * Store the status of the last registration attempt.
     * 
     * @param status
     *            The status of the registration attempt that is to be stored.
     */
    private void putRegistrationFailureStatus(String status) {
        mStore.put(REGISTRATION_FAILURE_REASON_KEY, status);
    }

    /**
     * Get the status of the last registration attempt, or null if no
     * registration attempt has been previously made.
     * 
     * @return The status of the last registration attempt, or null if no
     *         registration attempts have been recorded previously.
     */
    private String getRegistrationFailureStatus() {
        return mStore.get(REGISTRATION_FAILURE_REASON_KEY);
    }

    /**
     * Check whether we're in the NOT_YET_REGISTERED state.
     * 
     * @return True if we're in the NOT_YET_REGISTERED state, false otherwise.
     */
    private boolean isNotYetRegistered() {
        return EXP_STATE_NOT_YET_REGISTERED.equals(getExperimentState());
    }

    /**
     * Check whether we're previously has an unrecoverable registration failure.
     * 
     * @return True if we've had a previous unrecoverable registration failure,
     *         false otherwise.
     */
    private boolean hadUnrecoverableRegistrationFailure() {
        String registrationStatus = mStore.get(REGISTRATION_FAILURE_REASON_KEY);
        return !(REGISTRATION_FAILED_SERVER_NOT_OK.equals(registrationStatus) || registrationStatus == null);
    }

    /**
     * Check whether the current experiment state is ON.
     * 
     * @return True if the experiment is ON, false otherwise.
     */
    private boolean isExperimentOn() {
        experimentState = getExperimentState();
        return EXP_STATE_ON.equals(experimentState);
    }

    /**
     * Check whether we've previously registered successfully with the server
     * and that the user has opted in (and not opted out).
     * 
     * @return True if the user has opted in (and not out) and we've
     *         successfully registered with the server.
     */
    private boolean isOptedInAndRegistered() {
        experimentState = getExperimentState();
        return experimentState.equals(EXP_STATE_REGISTERED)
                || experimentState.equals(EXP_STATE_ON)
                || experimentState.equals(EXP_STATE_PAUSED_NO_BLUETOOTH)
                || experimentState.equals(EXP_STATE_PAUSED_NO_LOCATION);
    }

    /**
     * Set the experiment state to ON, unless required services like location
     * providers (GPS) or network (Bluetooth) are not available, in whcih case
     * the experiment state is set to PAUSED_NO_<SERVICE> where <SERVICE> is the
     * highest priority service which is disabled. (Location is higher priority
     * than network.
     */
    private void updateLiveExperimentState() {
        // If we're in START, OPTED_OUT or NOT_YET_REGISTERED, this method does
        // nothing. It only toggles between ON/PAUSED states.
        String experimentState = getExperimentState();
        if (experimentState.equals(EXP_STATE_START)
                || experimentState.equals(EXP_STATE_OPTED_OUT)
                || experimentState.equals(EXP_STATE_NOT_YET_REGISTERED)) {
            return;
        }
        if (isPlayServicesConnected() && isBluetoothOn()) {
            mStore.put(EXPERIMENT_STATE_KEY, EXP_STATE_ON);
            Log.v(TAG, "Experiment state is now " + EXP_STATE_ON);
        } else if (!isPlayServicesConnected()) {
            mStore.put(EXPERIMENT_STATE_KEY, EXP_STATE_PAUSED_NO_LOCATION);
            Log.v(TAG, "Experiment state is now "
                    + EXP_STATE_PAUSED_NO_LOCATION);
        } else if (!isBluetoothOn()) {
            mStore.put(EXPERIMENT_STATE_KEY, EXP_STATE_PAUSED_NO_BLUETOOTH);
            Log.v(TAG, "Experiment state is now "
                    + EXP_STATE_PAUSED_NO_BLUETOOTH);
        }
    }

    /**
     * Return true if the GPS_PROVIDER is available, false otherwise.
     * 
     * @return Whether the GPS location provider is available.
     */
    private boolean isPlayServicesConnected() {
        return mPlayServicesConnected;
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
     * Return the current state of the experiment, as stored in the StorageBase.
     * 
     * @return The current experiment state from among the choices defined in
     *         this class (START, PRE_REGISTRATION, ON, etc.)
     */
    public String getExperimentState() {
        String state = mStore.get(EXPERIMENT_STATE_KEY);
        if (state == null) {
            Log.wtf(TAG,
                    "Experiment state should never be null in RangzenService!");
            return EXP_STATE_START;
        } else {
            return state;
        }
    }

    /**
     * Store the current state of the experiment in the storage base.
     * 
     * @param state
     *            The experiment state value to set.
     */
    public void putExperimentState(String state) {
        mStore.put(EXPERIMENT_STATE_KEY, state);
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
            Log.e(TAG,
                    "Couldn't create ContactsGetterStorer, aborting registration. "
                            + e);
            return REGISTRATION_FAILED_BAD_ALGORITHM;
        }

        Set<String> contacts = mContactsGetterStorer
                .getObfuscatedPhoneNumbers();
        if (contacts == null) {
            Log.d(TAG, "Contacts are null; retrieving and storing them.");
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
            Log.e(TAG, "Coudln't get phone ID, aborting registration.");
            return REGISTRATION_FAILED_NULL_PHONE_ID;
        }

        ExperimentClient client = new ExperimentClient(
                EXPERIMENT_SERVER_HOSTNAME, EXPERIMENT_SERVER_PORT);
        client.register(phoneid, contacts.toArray(new String[0]));
        if (client.registrationWasSuccessful()) {
            Log.i(TAG, String.format(
                    "id: %s registered successfully with %d friends.", phoneid,
                    contacts.size()));
            return REGISTRATION_SUCCESS;
        } else {
            Log.e(TAG, "Registration failed, server answer was not OK!");
            return REGISTRATION_FAILED_SERVER_NOT_OK;
        }
    }

    /**
     * Get an ID for this phone which can be used to register and identify the
     * phone to the server and other phones. Currently, a Bluetooth MAC address
     * is use.
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
            String address = adapter.getAddress();
            return adapter.getAddress();
        } else {
            Log.e(TAG,
                    "Device doesn't support Bluetooth, can't get address for phone ID.");
            return null;
        }
    }

    /**
     * Register to receive regular updates of the phone's location.
     */
    private void registerForLocationUpdates() {
        mLocationClient.requestLocationUpdates(mLocationRequest,
                mLocationListener);
        Log.i(TAG, "Registered for location every " + LOCATION_UPDATE_INTERVAL
                + "ms");
    }

    /**
     * Ask the server for nearby phones, storing them as peers in the peer
     * manager.
     */
    private void getNearbyPhones() {
        Log.d(TAG, "Experiment is on, getting nearby phones from server.");
        ExperimentClient getPhonesClient;
        getPhonesClient = new ExperimentClient(EXPERIMENT_SERVER_HOSTNAME,
                EXPERIMENT_SERVER_PORT);
        getPhonesClient.getNearbyPhones(getPhoneID(), NEARBY_DISTANCE);
        String[] addresses = getPhonesClient.getNearbyPhonesResult();
        if (addresses != null) {
            if (addresses.length == 0) {
                Log.d(TAG, "Addresses is: []");
            }
            for (String address : addresses) {
                Log.d(TAG, "Got address " + address);
            }
            List<Peer> peers = new ArrayList<Peer>();
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            for (String address : addresses) {
                BluetoothDevice device = adapter.getRemoteDevice(address);
                Peer peer = mPeerManager.getCanonicalPeer(new Peer(
                        new BluetoothPeerNetwork(device)));
                peers.add(peer);
                Log.d(TAG, "Adding peer to PeerManager: " + peer);
            }
            mPeerManager.addPeers(peers);
        } else {
            Log.e(TAG,
                    "Nearby phones was null - something went wrong asking the server for them.");
        }
        haveRecentNearbyPhones = true;
    }

    /**
     * Callback for location updates. Stores locations retrieved in the location
     * store.
     */
    private LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            // Store the new location.
            Log.d(TAG, "Got location: " + location);
            SerializableLocation serializableLocation = new SerializableLocation(
                    location);
            mLocationStore.addLocation(serializableLocation);
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
     * Called by Location Services when the request to connect the client
     * finishes successfully. At this point, you can request the current
     * location or start periodic updates
     */
    @Override
    public void onConnected(Bundle dataBundle) {
        Log.d(TAG, "Google play services connected.");
        mPlayServicesConnected = true;
        registerForLocationUpdates();
    }

    /**
     * Called when the connection to the Google Play Services fails.
     */
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        mPlayServicesConnected = false;
        Log.e(TAG, "Google player services connection failed.");
    }

    /**
     * Called when Google Play Services disconnects.
     */
    @Override
    public void onDisconnected() {
        mPlayServicesConnected = false;
        Log.e(TAG, "Google player services disconnected.");
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
