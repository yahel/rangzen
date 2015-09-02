package org.denovogroup.rangzen.backend;

import com.parse.ParseException;
import com.parse.ParseInstallation;
import com.parse.ParseObject;

import org.denovogroup.rangzen.LocationTracking.TrackedLocation;

/**
 * Created by Liran on 8/31/2015.
 *
 * This Class is responsible for sending requests to the database server, at the time of writing
 * the database is parse which handle all the broadcasting logic on its own.
 *
 */
public class DatabaseHandler {

    private static DatabaseHandler instance;

    private static final String USERID_KEY = "Userid";
    private static final String LONGITUDE_KEY = "Longitude";
    private static final String LATITUDE_KEY = "Latitude";
    private static final String TIME_KEY = "Time";

    public static DatabaseHandler getInstance(){
        if(instance == null){
            instance = new DatabaseHandler();
        }
        return instance;
    }

    /** Send location data to the server
     *
     * @param trackedLocation the object to be sent
     * @return true if object was sent and received, false otherwise
     */
    public boolean sendLocation(TrackedLocation trackedLocation){
        if(trackedLocation != null){
            ParseObject testObject = new ParseObject("LocationObject");
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
}
