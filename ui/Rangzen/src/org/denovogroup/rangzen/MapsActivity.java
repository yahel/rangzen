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
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.StateListDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.drive.internal.e;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.LocationSource;
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
        GooglePlayServicesClient.OnConnectionFailedListener, LocationListener,
        LocationSource {

    /** Displayed in Android Log messages. */
    private static final String TAG = "MapsActivity";

    /**
     * The fragment that contains the map itself. It is the second most top
     * fragment in its parent FrameLayout.
     */
    private SupportMapFragment mapFragment;
    /**
     * The map object itself that is inside of the SupportMapFragment.
     */
    private GoogleMap map;
    /**
     * Used to connect and disconnect from Google Location Services and
     * locations.
     */
    private LocationClient locationClient;
    /**
     * The transparent fragment. It is the bottom most layer of the FrameLayout
     * but is brought to the top if created.
     */
    private Fragment transparent;
    /**
     * Saving the users current location of the user when OnConnected is called.
     */
    private Location myLocation;
    /**
     * This is the object that manages all of the Fragments visible and
     * invisible, and adds, replaces or removes fragments.
     */
    private FragmentManager fragmentManager;

    /**
     * Handle to Rangzen storage manager.
     */
    private StorageBase mStore;

    /**
     * Used in OnLocationChanged to indicate that a first location has not yet
     * been stored so make the current location into the first location.
     */
    private int flag;
    /**
     * Used in OnLocationChanged to save the previous location in order to
     * connect a small Polyline between the previous and the current points.
     */
    private LatLng prev;
    /**
     * Stores a references to the about icon in order to bring it to the front
     * of the FrameLayout.
     */
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
        // getWindow().requestFeature(Window.FEATURE_ACTION_BAR);

        mStore = new StorageBase(this, StorageBase.ENCRYPTION_DEFAULT);

        setContentView(R.layout.master);
        locationClient = new LocationClient(this, this, this);
        createAboutIcon();
        // action = getActionBar();
        // action.hide();

        if (savedInstanceState == null) {
            mapFragment = new MapFrag();
            mapFragment.setRetainInstance(true);
            fragmentManager = getSupportFragmentManager();
            fragmentManager.beginTransaction()
                    .replace(R.id.mapHolder, mapFragment).commit();
            createTransparentFragment();
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
        int width = (int) getPixels(75);
        int height = (int) getPixels(75);

        Bitmap resized = Bitmap.createScaledBitmap(icon, width, height, true);
        Bitmap pressed = Bitmap.createScaledBitmap(icon2, width, height, true);

        BitmapDrawable res = new BitmapDrawable(resized);
        BitmapDrawable pre = new BitmapDrawable(pressed);

        StateListDrawable states = new StateListDrawable();
        states.addState(new int[] { android.R.attr.state_pressed }, pre);
        states.addState(new int[] { android.R.attr.state_focused }, pre);
        states.addState(new int[] {}, res);

        about.setBackgroundDrawable(states);
        about.bringToFront();
    }
    
    /**
     * Method that finds the actual amount of pixels necessary for shifts of
     * textViews. Borrowed from
     * http://stackoverflow.com/questions/2406449/does-setwidthint
     * -pixels-use-dip-or-px
     * @param dip Density-Independent length
     */
    private float getPixels(int dip) {
        Resources r = getResources();
        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip,
                r.getDisplayMetrics());
        return px;
    }

    /**
     * The onClickListener of the Deny button on the transparent page. Used to
     * restart the introduction slides if they do not accept.
     * 
     * @param v The View for the deny button.
     */
    public void sDeny(View v) {
        Log.i(TAG, "Denied permission.");
        Intent intent = new Intent();
        intent.setClass(this, SlidingPageIndicator.class);
        startActivity(intent);
        finish();
    }

    /**
     * Make sure that the introduction and the transparent page will never be
     * seen again and go through to the map.
     * 
     * @param v
     *            - View for the accept button
     */
    public void sAccept(View v) {
        Log.i(TAG, "Accepted permission.");
        transparent.getView().setClickable(false);
        transparent.getView().setVisibility(View.INVISIBLE);
        ViewGroup vg = (ViewGroup) transparent.getView().getParent();
        vg.setClickable(false);
        vg.setVisibility(View.INVISIBLE);
        ViewGroup vgParent = (ViewGroup) vg.getParent();
        vgParent.removeView(vg);
        //findViewById(R.id.accept).setClickable(false);
        //findViewById(R.id.deny).setClickable(false);
        
        SharedPreferences settings = getSharedPreferences(
                SlidingPageIndicator.PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();

        editor.putBoolean("hasLoggedIn", true);
        editor.commit();

        editor.putBoolean("transparent", true);
        editor.commit();

        Log.i(TAG, "Experiment state is now EXP_STATE_NOT_YET_REGISTERED.");
        mStore.put(RangzenService.EXPERIMENT_STATE_KEY, 
                   RangzenService.EXP_STATE_NOT_YET_REGISTERED);

        // Spawn Rangzen Service.
        Log.i(TAG, "Permission granted - Starting Rangzen Service.");
        Intent rangzenServiceIntent = new Intent(this, RangzenService.class);
        startService(rangzenServiceIntent);
    }


    /**
     * OnClickListener for the about icon, this handles the creation of a new
     * fragment and pressing back button to get back to the map.
     * 
     * @param v
     *            The view that contains the about icon.
     */
    public void s(View v) {
        Fragment info = new IntroductionFragment();
        Bundle b = new Bundle();
        b.putInt("whichScreen", 5);
        info.setArguments(b);
        findViewById(R.id.infoHolder).setClickable(true);
        fragmentManager = getSupportFragmentManager();
        FragmentTransaction ft = fragmentManager.beginTransaction();
        ft.add(R.id.infoHolder, info);
        //ft.hide(mapFragment);
        ft.addToBackStack("info");
        ft.commit();
        // action.show();
        // action.setTitle("About Rangzen");
        // action.setIcon(R.drawable.ic_action_back);
    }

    /**
     * This is for the about icon, when the about icon is pressed then the user
     * can press the back button on their phone to get back to the map.
     */
    @Override
    public void onBackPressed() {
        // SharedPreferences settings = getSharedPreferences(
        // SlidingPageIndicator.PREFS_NAME, 0);
        // boolean hasAcceptedTransparent = settings.getBoolean("transparent",
        // false);
        // if (!hasAcceptedTransparent) {
        // return;
        // }
        super.onBackPressed();
        findViewById(R.id.infoHolder).setClickable(false);
        // action.hide();
    }

    /**
     * This is for when the screen configuration changes, then the markers and
     * the info button must be brought back into focus.
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Toast.makeText(this, "orientation changed", Toast.LENGTH_SHORT).show();
        if (about != null) {
            about.bringToFront();
        }
        // for (int i = 0; i < locationStore.getAllLocations().size(); i++) {
        // onLocationChanged(locationStore)
        // }

    }

    /**
     * Stores into sharedPreferences that the transparent page has already been
     * seen. This also adds the transparent fragment into a linear layout in
     * master.xml.
     */
    private void createTransparentFragment() {
        // Checking to create transparency.
        SharedPreferences settings = getSharedPreferences(
                SlidingPageIndicator.PREFS_NAME, 0);
        // Get "transparent" value. If the value doesn't exist yet false is
        // returned
        boolean hasSeentransparent = settings.getBoolean("transparent", false);

        if (!hasSeentransparent) {
            // //set has seen transparency in a shared save preference
            transparent = new IntroductionFragment();
            Bundle b2 = new Bundle();
            b2.putInt("whichScreen", 6);
            transparent.setArguments(b2);
            findViewById(R.id.transparentHolder).bringToFront();
            fragmentManager.beginTransaction()
                    .replace(R.id.transparentHolder, transparent).commit();
        }
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
        Toast.makeText(this, "location changed", Toast.LENGTH_SHORT).show();

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
        Location location = locationClient.getLastLocation();
        if (location != null) {
          LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
          CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng,
              15);
          if (mapFragment != null) {
            map = mapFragment.getMap();
          }
          if (map != null) {
            map.animateCamera(cameraUpdate);
            map.setMyLocationEnabled(true);
            map.setBuildingsEnabled(true);

          }
          myLocation = location;
        }
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

    /**
     * Unimplemented methods that need to be here for implementing other
     * classes.
     */
    @Override
    public void activate(OnLocationChangedListener listener) {
    }

    /**
     * Unimplemented methods that need to be here for implementing other
     * classes.
     */
    @Override
    public void deactivate() {
    }

    /**
     * Unimplemented methods that need to be here for implementing other
     * classes.
     */
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    /**
     * Unimplemented methods that need to be here for implementing other
     * classes.
     */
    @Override
    public void onProviderEnabled(String provider) {
    }

    /**
     * Unimplemented methods that need to be here for implementing other
     * classes.
     */
    @Override
    public void onProviderDisabled(String provider) {
    }

}
