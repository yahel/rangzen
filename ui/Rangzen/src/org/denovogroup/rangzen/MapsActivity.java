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

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

/**
 * This class is in control of all Google maps related activity except for
 * allowing the fragment to maintain itself on orientation changes. This class
 * is also in charge of programmatic features including onClickListeners for the
 * map interface.
 */
public class MapsActivity extends FragmentActivity implements
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener {

    private SupportMapFragment mapFragment;
    private GoogleMap map;
    private LocationClient locationClient;
    private FragmentTransaction ft;
    private Fragment transparent;
    private LinearLayout transparentFragment;
    private FrameLayout root;
    private Location myLocation;
    private int locationSetupNeeded = 1;
    private FragmentManager fragmentManager;
    private int flag;
    private LatLng prev;
    private ImageButton about;

    /**
     * Define a request code to send to Google Play services This code is
     * returned in Activity.onActivityResult
     */
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;

    /**
     * Sets up the initial FragmentManager and if there is no savedInstanceState
     * for this app then new fragments are created for the map interface.
     * 
     * @param savedInstanceState
     *            This is the memory of the last state of this application.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.master);
        locationClient = new LocationClient(this, this, this);

        if (savedInstanceState == null) {
            mapFragment = new MapFrag();
            mapFragment.setRetainInstance(true);
            fragmentManager = getSupportFragmentManager();
            ft = fragmentManager.beginTransaction();
            fragmentManager.beginTransaction()
                    .replace(R.id.mapHolder, mapFragment).commit();
            createTransparentFragment();
            createAboutIcon();
            // ft.commit();
        }
    }

    /**
     * Creates the clickable about icon on the google maps interface. This icon
     * needs to be scaled down and the icon itself is created in the xml file,
     * "master.xml"
     */
    private void createAboutIcon() {
        about = (ImageButton) findViewById(R.id.ib);
        Bitmap icon = BitmapFactory.decodeResource(getResources(),
                R.drawable.abouticon19);
        Bitmap icon2 = BitmapFactory.decodeResource(getResources(),
                R.drawable.pressedinfo);
        int width = 140;
        int height = 140;

        Bitmap resized = Bitmap.createScaledBitmap(icon, width, height, true);
        Bitmap pressed = Bitmap.createScaledBitmap(icon2, width, height, true);

        BitmapDrawable res = new BitmapDrawable(resized);
        BitmapDrawable pre = new BitmapDrawable(pressed);

        StateListDrawable states = new StateListDrawable();
        states.addState(new int[] { android.R.attr.state_pressed }, pre);
        states.addState(new int[] { android.R.attr.state_focused }, pre);
        states.addState(new int[] {}, res);

        about.setBackgroundDrawable(states);
    }

    public void s(View v) {
        Toast.makeText(this, "ib clicked", Toast.LENGTH_SHORT).show();
        Fragment info = new IntroductionFragment();
        Bundle b = new Bundle();
        b.putInt("whichScreen", 5);
        info.setArguments(b);
        findViewById(R.id.infoHolder).setClickable(true);
        fragmentManager.beginTransaction().add(R.id.infoHolder, info)
                .addToBackStack("info").commit();

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        findViewById(R.id.infoHolder).setClickable(false);
    }

    /**
     * Stores into sharedPreferences that the transparent page has already been
     * seen. This also adds the transparent fragment into a linear layout in
     * master.xml
     */
    private void createTransparentFragment() {
        // checking to create transparency
        SharedPreferences settings = getSharedPreferences(
                SlidingPageIndicator.PREFS_NAME, 0);
        // Get "hasLoggedIn" value. If the value doesn't exist yet false is
        // returned
        boolean hasSeentransparent = settings.getBoolean("transparent", false);

        if (!hasSeentransparent) {
            // //set has seen transparency in a shared save preference
            SharedPreferences settingsGot = getSharedPreferences(
                    SlidingPageIndicator.PREFS_NAME, 0);
            SharedPreferences.Editor editor = settingsGot.edit();
            editor.putBoolean("transparent", true);
            editor.commit();
            transparent = new IntroductionFragment();
            Bundle b2 = new Bundle();
            b2.putInt("whichScreen", 4);
            transparent.setArguments(b2);
            fragmentManager.beginTransaction()
                    .add(R.id.transparentHolder, transparent).commit();
        }
    }

    /**
     * sButton is the onClickListener for the View, "button" that exists in the
     * xml file "master.xml". It has been assigned through xml.
     * 
     * @param v
     *            The button is itself a view being passed in.
     */
    public void sButton(View v) {
        Toast.makeText(this, "click", Toast.LENGTH_SHORT).show();
        placeMarkers();
    }

    /**
     * This is the onClickListener of the LinearLayout that holds the
     * transparent fragment. This detects a click and makes the transparent
     * fragment become invisible.
     * 
     * @param v
     *            v is the LinearLayout that contains the transparent fragment.
     */
    public void sLayout(View v) {
        v.setVisibility(View.INVISIBLE);
    }

    /**
     * Called when Rangzen is first visible and this connects to the google
     * services.
     */
    @Override
    protected void onStart() {
        super.onStart();
        // connect the client
        if (isGooglePlayServicesAvailable()) {
            locationClient.connect();
        }
    }

    /**
     * PlaceMarkers finds the saved markers of Rangzen user contact using
     * Barath's data storage API and places the markers onto the map.
     */
    private void placeMarkers() {
        Marker marker = map.addMarker((new MarkerOptions().position(new LatLng(
                myLocation.getLatitude(), myLocation.getLongitude()))));
    }

    /**
     * Called when the Activity is no longer visible.
     */
    @Override
    protected void onStop() {
        // Disconnecting the client invalidates it.
        locationClient.disconnect();
        super.onStop();
    }

    /**
     * Handle results returned to the FragmentActivity by Google Play services.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Decide what to do based on the original request code
        switch (requestCode) {

        case CONNECTION_FAILURE_RESOLUTION_REQUEST:
            /*
             * If the result code is Activity.RESULT_OK, try to connect again
             */
            switch (resultCode) {
            case Activity.RESULT_OK:
                locationClient.connect();
                break;
            }

        }
    }

    /**
     * This will add points to memory using Barath's data storage and will
     * constantly keep a line on the map of saved movements.
     * 
     * @param location
     *            Location is the current location of the user when they have
     *            moved.
     */
    public void onLocationChanged(Location location) {

        LatLng current = new LatLng(location.getLatitude(),
                location.getLongitude());

        if (flag == 0) // when the first update comes, we have no previous
                       // points,hence this
        {
            prev = current;
            flag = 1;
        }
        CameraUpdate update = CameraUpdateFactory.newLatLngZoom(current, 16);
        map.animateCamera(update);
        map.addPolyline((new PolylineOptions()).add(prev, current).width(6)
                .color(Color.BLUE).visible(true));
        prev = current;
        current = null;
    }

    /**
     * Checks to see if GooglePlayServices is available; if it is then it
     * returns true, if not then an error is given and it returns false.
     * 
     * @return True or False depending on the readyness of google play services.
     */
    private boolean isGooglePlayServicesAvailable() {
        // Check that Google Play services is available
        int resultCode = GooglePlayServicesUtil
                .isGooglePlayServicesAvailable(this);
        // If Google Play services is available
        if (ConnectionResult.SUCCESS == resultCode) {
            Log.d("Location Updates", "Google Play services is available.");
            return true;
        } else {
            // Get the error dialog from Google Play services
            Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(
                    resultCode, this, CONNECTION_FAILURE_RESOLUTION_REQUEST);

            // If Google Play services can provide an error dialog
            if (errorDialog != null) {
                // Create a new DialogFragment for the error dialog
                ErrorDialogFragment errorFragment = new ErrorDialogFragment();
                errorFragment.setDialog(errorDialog);
                errorFragment.show(getSupportFragmentManager(),
                        "Location Updates");
            }

            return false;
        }
    }

    /**
     * Called by Location Services when the request to connect the client
     * finishes successfully. At this point, you can request the current
     * location or start periodic updates
     */
    @Override
    public void onConnected(Bundle dataBundle) {
        if (locationSetupNeeded == 1) {
            Location location = locationClient.getLastLocation();
            LatLng latLng = new LatLng(location.getLatitude(),
                    location.getLongitude());
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(
                    latLng, 15);
            map = mapFragment.getMap();
            if (map != null) {
                map.animateCamera(cameraUpdate);
                map.setMyLocationEnabled(true);
                map.setBuildingsEnabled(true);

            }
            if (location != null) {
                myLocation = location;
            }
        }
        locationSetupNeeded = 0;
    }

    /** Define a DialogFragment that displays the error dialog */
    public static class ErrorDialogFragment extends DialogFragment {

        /** Global field to contain the error dialog */
        private Dialog mDialog;

        /** Default constructor. Sets the dialog field to null */
        public ErrorDialogFragment() {
            super();
            mDialog = null;
        }

        /** Set the dialog to display */
        public void setDialog(Dialog dialog) {
            mDialog = dialog;
        }

        /** Return a Dialog to the DialogFragment. */
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return mDialog;
        }
    }

    /**
     * Unimplemented methods that need to be here for implementing other
     * classes.
     */
    @Override
    public void onConnectionFailed(ConnectionResult arg0) {

    }

    /**
     * Unimplemented methods that need to be here for implementing other
     * classes.
     */
    @Override
    public void onDisconnected() {

    }

}
