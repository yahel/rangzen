package org.denovogroup.rangzen.locationtracking;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import org.denovogroup.rangzen.R;
import org.denovogroup.rangzen.backend.NetworkHandler;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Liran on 8/31/2015.
 *
 * This service is the main handler for all location tracking events in the app, it will sample location updates
 * in a fixed interval and broadcast them to a server defined by the NetworkHandler class, every item is first
 * being saved into a local memory cache and then being sent to the server, if the sending process was successful
 * the item will be removed from the cache. This lazy sending mechanism ensure that all the data will be sent to
 * the server eventually.
 *
 * Since it is impossible to set exact time periods to receive location updates (the system may send a lot more
 * causing data overflow), a timer is responsible for sending the last received location in a fixed interval.
 * The system will only update this location reference but will not send it on its own.
 */
public class TrackingService extends Service implements LocationListener {

    private LocationManager locationManager;
    private Timer locationUpdateTimer = new Timer();

    private static final String LOG_TAG = "TrackingService";
    private static final int SECOND = 1000;
    private static final int UPDATE_TIME_INTERVAL = 10 * SECOND;
    private static final float UPDATE_DISTANCE_INTERVAL = 0; // this is the real limit on location updates, min time is just a hint
    private static final int NOTIFICATION_ID = R.string.TrackingServiceNotification;

    private static TrackedLocation lastLocationSent;
    private static TrackedLocation lastLocationUpdate;
    private static boolean isFlushing = false;
    private static NotificationManager notificationManager;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //Avoid returning START_STICKY so the service can be stopped by dismissing the notification
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(LOG_TAG, "Tracking service started");
        notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        showNotification();

        locationManager = (LocationManager) getSystemService(Service.LOCATION_SERVICE);
        /*Register for updates from the specified location provider, all incoming transmission will be handled
          by the service acting as the listener*/
        //TODO maybe instead of forcing GPS as a provider i should use get best provider
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, UPDATE_TIME_INTERVAL, UPDATE_DISTANCE_INTERVAL, this);

        /*Since it is impossible to time when we will get updates from the location provider, we will force
        the rhythm at sampling and sending to server */
        locationUpdateTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                /* Check if last update was already sent (i.e the device couldn't get another update by the time the timer interval
                 ended) and save it if not */
                if (lastLocationUpdate != null && (lastLocationSent == null || lastLocationUpdate.timestamp > lastLocationSent.timestamp)) {
                    saveToCache(lastLocationUpdate);
                }
                //try to send everything in local memory to the server
                flushCache();
            }
        }, 0, UPDATE_TIME_INTERVAL);
    }

    private void showNotification() {
        Notification notification = new Notification.Builder(this)
                .setSmallIcon(R.drawable.ic_launcher)
                .setTicker(getString(R.string.TrackingServiceStarting))
                .setWhen(System.currentTimeMillis())
                .setContentTitle(getString(R.string.TrackingServiceNotificationTitle))
                .setContentText(getString(R.string.TrackingServiceNotification))
                .build();

        // Send the notification.
        startForeground(NOTIFICATION_ID, notification);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        locationManager.removeUpdates(this);
        stopForeground(true);
        Log.d(LOG_TAG, "Tracking service stopped");

        notificationManager.cancel(NOTIFICATION_ID);
        locationUpdateTimer.cancel();
    }



    @Override
    public void onLocationChanged(Location location) {
        //update current location
        lastLocationUpdate = new TrackedLocation(location.getLatitude(), location.getLongitude(), location.getTime());
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        if(status == LocationProvider.AVAILABLE){
            flushCache();
        }
    }

    @Override
    public void onProviderEnabled(String provider) {
        flushCache();
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    /** Attempt to send data stored on cache to the server, this will clean any item which was succesfully sent
     *
     * @return true if all items have been sent and cache is now empty, false otherwise
     */
    private boolean flushCache(){
        if(NetworkHandler.isNetworkConnected() && !isFlushing){
            isFlushing = true;
            LocationCacheHandler cacheHandler = LocationCacheHandler.getInstance(getApplicationContext());
            Cursor cursor = cacheHandler.getCursor();
            if(cursor != null){
                cursor.moveToFirst();
                while(!cursor.isAfterLast()) {
                    TrackedLocation trackedLocation = new TrackedLocation(
                            cursor.getDouble(cursor.getColumnIndex(LocationCacheHandler.LATITUDE_COL)),
                            cursor.getDouble(cursor.getColumnIndex(LocationCacheHandler.LONGITUDE_COL)),
                            cursor.getLong(cursor.getColumnIndex(LocationCacheHandler.TIMESTAMP_COL))
                    );
                    if (sendToServer(trackedLocation)) {
                        cacheHandler.removeLocation(trackedLocation);
                        lastLocationSent = trackedLocation;
                    }
                    cursor.moveToNext();
                }
                cursor.close();
                isFlushing = false;

                if(cacheHandler.getCacheSize() <= 0) {
                    return true;
                }
            }
            isFlushing = false;
        }
        return false;
    }

    /** Request the NetworkHandler to send the object to server
     *
     * @param trackedLocation the object to be sent
     * @return true is object has been sent succesfuly, false otherwise
     */
    private boolean sendToServer(TrackedLocation trackedLocation){
        NetworkHandler dbHandler = NetworkHandler.getInstance(getApplicationContext());
        if(NetworkHandler.isNetworkConnected() && trackedLocation != null && dbHandler != null){
            //Log.d(LOG_TAG,"sending to server");
            if(dbHandler.sendLocation(trackedLocation)){
                //Log.d(LOG_TAG,"data sent");
                return true;
            }
        }
        //Log.d(LOG_TAG,"couldnt send to server");
        return false;
    }

    /** Saves a tracking object to local storage
     *
     * @param trackedLocation to be saved into storage
     */
    private void saveToCache(TrackedLocation trackedLocation){
        if(trackedLocation != null) {
            LocationCacheHandler cacheHandler = LocationCacheHandler.getInstance(getApplicationContext());
            cacheHandler.insertLocation(trackedLocation);
        }
    }
}
