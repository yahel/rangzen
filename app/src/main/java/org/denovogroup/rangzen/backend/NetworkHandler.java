package org.denovogroup.rangzen.backend;

import android.app.Service;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.parse.ParseException;
import com.parse.ParseInstallation;
import com.parse.ParseObject;

import org.denovogroup.rangzen.locationtracking.TrackedLocation;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

/**
 * Created by Liran on 8/31/2015.
 *
 * This Class is responsible for sending requests to the database server, at the time of writing
 * the database is parse which handle all the broadcasting logic on its own.
 *
 */
public class NetworkHandler {

    private static NetworkHandler instance;
    private static Context context;

    //general keys
    private static final String USERID_KEY = "Userid";
    private static final String TIME_KEY = "Time";

    //Location tracking keys
    private static final String LONGITUDE_KEY = "Longitude";
    private static final String LATITUDE_KEY = "Latitude";

    public static NetworkHandler getInstance(Context ctx){
        if(instance == null){
            instance = new NetworkHandler();
        }
        context = ctx;
        return instance;
    }

    /** check if the device is currently connected to an internet service such as WiFi and GSM
     *
     * @return true is connected, false otherwise
     */
    public static boolean isNetworkConnected(){
        if(context != null) {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Service.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = cm.getActiveNetworkInfo();
            return (networkInfo != null && networkInfo.isConnected());
        }
        return false;
    }

    /** Send location data to the server
     *
     * @param trackedLocation the object to be sent
     * @return true if object was sent and received, false otherwise
     */
    public boolean sendLocation(TrackedLocation trackedLocation){
        if(trackedLocation != null){
            ParseObject testObject = new ParseObject("LocationTracking");
            testObject.put(USERID_KEY, ParseInstallation.getCurrentInstallation());
            testObject.put(LONGITUDE_KEY, trackedLocation.longitude);
            testObject.put(LATITUDE_KEY, trackedLocation.latitude);
            testObject.put(TIME_KEY, trackedLocation.timestamp);
            try {
                testObject.save();
                return true;
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    /** send an event report to the server, even though input is JSONObject you should use
     * EventReportsMaker class to create a properly formatted report before using this method
     *
     * @param report json object retreived from EventReportsMaker class
     * @return true if the report was sent and received, false otherwise
     */
    public boolean sendEventReport(JSONObject report){

        if(report != null){
            try {
                //Convert to parse object
                ParseObject testObject = new ParseObject("EventTracking");
                Iterator<?> keys = report.keys();
                while(keys.hasNext()) {
                    String key = (String)keys.next();
                    testObject.put(key, report.get(key));
                }
                testObject.save();
                return true;
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        return false;
    }
}
