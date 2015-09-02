package org.denovogroup.rangzen.locationtracking;

/**
 * Created by Liran on 8/31/2015.
 *
 * This is a simple object to hold important location data until it will be passed to the server
 */
public class TrackedLocation{

    public double latitude;
    public double longitude;
    public long timestamp;

    public TrackedLocation(double latitude, double longitude, long timestamp) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
    }
}
