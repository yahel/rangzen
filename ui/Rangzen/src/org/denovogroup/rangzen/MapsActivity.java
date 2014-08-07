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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.StateListDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
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
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
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
import com.google.android.gms.maps.model.Polyline;
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

    /** Displayed in Android Log messages. */
    private static final String TAG = "MapsActivity";

    /**
     * The fragment that contains the map itself. It is the second most top
     * fragment in its parent FrameLayout.
     */
    private SupportMapFragment mapFragment;

    /**
     * Variable that will last the length of the activity, so the map will not
     * be centered gain while the activity is up.
     */
    private static boolean hasCentered = false;

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
     * This is the object that manages all of the Fragments visible and
     * invisible, and adds, replaces or removes fragments.
     */
    private FragmentManager fragmentManager;

    /**
     * Handle to Rangzen storage manager.
     */
    private StorageBase mStore;

    /** Used to determine if the slider is on or off. */
    private static boolean isSliderOn = false;

    /** RangeBar slider that will determine how many points will be shown. */
    private RangeSeekBar polyLineRange;

    /**
     * Define a request code to send to Google Play services This code is
     * returned in Activity.onActivityResult
     */
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;

    /**
     * Polyline ArrayList in order to hold all polyline options as the next one
     * is built.
     */
    private static ArrayList<Polyline> array = new ArrayList<Polyline>();

    /** Size of the current polyline. */
    private int sizePoly = 0;

    /** Number of running Async Tasks. */
    private static int numAsync = 0;

    /** List of exchanges for this current map. */
    private ArrayList<Marker> markers = new ArrayList<Marker>();

    /** Helps with determining what functionality the back button should have. */
    private static boolean aboutShowing = false;

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
        createButtonImage(R.id.ib, R.drawable.abouticon19,
                R.drawable.pressedinfo, false);
        createButtonImage(R.id.button22, R.drawable.slider,
                R.drawable.sliderpressed, false);
        createButtonImage(R.id.refresh, R.drawable.refresh,
                R.drawable.refreshpressed, false);
        createButtonImage(R.id.leftArrow, R.drawable.rightarrow,
                R.drawable.rightarrowpressed, true);
        createButtonImage(R.id.rightArrow, R.drawable.rightarrow,
                R.drawable.rightarrowpressed, false);

        if (savedInstanceState == null) {
            mStore = new StorageBase(this, StorageBase.ENCRYPTION_DEFAULT);
            mLocationStore = new LocationStore(this,
                    StorageBase.ENCRYPTION_NONE);
            mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            mapFragment = (SupportMapFragment) SupportMapFragment.newInstance();
            mapFragment.setRetainInstance(true);
            map = mapFragment.getMap();
            fragmentManager = getSupportFragmentManager();
            fragmentManager.beginTransaction()
                    .replace(R.id.mapHolder, mapFragment).commit();
            createTransparentFragment();
        }
        createSlider();
    }

    /**
     * Creates a clickable about icon on the google maps interface. This icon
     * needs to be scaled down and the icon itself is created in the xml file,
     * "master.xml".
     */
    private void createButtonImage(int buttonId, int notPressedImage,
            int pressedImage, boolean invert) {
        ImageButton button = (ImageButton) findViewById(buttonId);
        button.bringToFront();

        Bitmap icon = BitmapFactory.decodeResource(getResources(),
                notPressedImage);
        Bitmap icon2 = BitmapFactory.decodeResource(getResources(),
                pressedImage);
        int width = (int) getPixels(85);
        int height = (int) getPixels(85);

        Bitmap resized = Bitmap.createScaledBitmap(icon, width, height, true);
        Bitmap pressed = Bitmap.createScaledBitmap(icon2, width, height, true);

        if (invert) {
            Matrix matrix = new Matrix();
            matrix.postRotate(180);
            resized = Bitmap.createBitmap(resized, 0, 0, resized.getWidth(),
                    resized.getHeight(), matrix, true);

            Matrix matrix2 = new Matrix();
            matrix2.postRotate(180);
            pressed = Bitmap.createBitmap(pressed, 0, 0, pressed.getWidth(),
                    pressed.getHeight(), matrix2, true);
        }

        BitmapDrawable res = new BitmapDrawable(resized);
        BitmapDrawable pre = new BitmapDrawable(pressed);

        StateListDrawable states = new StateListDrawable();
        states.addState(new int[] { android.R.attr.state_pressed }, pre);
        states.addState(new int[] { android.R.attr.state_focused }, pre);
        states.addState(new int[] {}, res);

        button.setBackgroundDrawable(states);
    }

    /**
     * This the is the slider button OnClickListener, it turns the slider
     * invisible and visible.
     * 
     * @param v
     *            The image button itself.
     */
    public void sSliderButton(View v) {
        LinearLayout slider = (LinearLayout) findViewById(R.id.slider);
        if (!isSliderOn || slider.getVisibility() == View.INVISIBLE) {
            slider.setVisibility(View.VISIBLE);
            isSliderOn = true;
        } else {
            slider.setVisibility(View.INVISIBLE);
            isSliderOn = false;
        }
    }

    /**
     * Redraw all of the points that the phone contains.
     * 
     * @param v
     *            The refresh button itself.
     */
    public void sRefresh(View v) {
        drawPoints(-1, -1, 0);
        mLocationStore = new LocationStore(this, StorageBase.ENCRYPTION_DEFAULT);
        int size = mLocationStore.getMostRecentSequenceNumber();
        polyLineRange.setSelectedMaxValue(size);
        polyLineRange.setSelectedMinValue(0);
    }

    /**
     * The left arrow that will show with the slider to show the next x many
     * points in the past not yet being shown.
     * 
     * @param v
     *            The left arrow button.
     */
    public void sLeftArrow(View v) {
        int min = polyLineRange.getSelectedMinValue();
        int max = polyLineRange.getSelectedMaxValue();
        mLocationStore = new LocationStore(this, StorageBase.ENCRYPTION_DEFAULT);
        int size = mLocationStore.getMostRecentSequenceNumber();
        double change = (double) size * .1;
        if (min > change) {
            drawPoints((Integer.valueOf((int) (min - change))), polyLineRange.getSelectedMaxValue(), -1);
            double doubly2 = ((double) min - change) / (double) size;
            polyLineRange.setNormalizedMinValue(doubly2);
        }
    }

    /**
     * The right arrow that will show with the slider to show the next x many
     * points in the future not yet being shown.
     * 
     * @param v
     *            The right arrow button.
     */
    public void sRightArrow(View v) {
        int max = polyLineRange.getSelectedMaxValue();
        int min = polyLineRange.getSelectedMinValue();
        mLocationStore = new LocationStore(this, StorageBase.ENCRYPTION_DEFAULT);
        int size = mLocationStore.getMostRecentSequenceNumber();
        double change = (double) size * .1;
        Log.d(TAG, "value of change = " + String.valueOf(change));
        if (max + change < size) {
            drawPoints(polyLineRange.getSelectedMinValue(), (Integer.valueOf((int) (max + change))), -1);
            double doubly2 = ((double) max + change) / (double) size;
            polyLineRange.setNormalizedMaxValue(doubly2);
        }
    }

    /**
     * Initialize the slider and its on click listener with a min and max range
     * (taken care of edge cases with no locations stored).
     */
    private void createSlider() {
        int size = 0;
        mLocationStore = new LocationStore(this, StorageBase.ENCRYPTION_DEFAULT);
        size = mLocationStore.getMostRecentSequenceNumber();
        polyLineRange = new RangeSeekBar(1, size, this);
        polyLineRange
                .setOnRangeSeekBarChangeListener(new RangeSeekBar.OnRangeSeekBarChangeListener() {

                    @Override
                    public void onRangeSeekBarValuesChanged(RangeSeekBar bar,
                            Integer minValue, Integer maxValue) {
                        if (maxValue != -1 && maxValue != minValue
                                && minValue < maxValue && minValue != -1) {
                            drawPoints(minValue, maxValue, 0);
                        }
                    }
                });

        LinearLayout slider = (LinearLayout) findViewById(R.id.slider);
        slider.addView(polyLineRange);
    }

    /**
     * Will only be called in OnCreate and also the app needs to have been
     * destroyed in order for the method to be called again.
     */
    private void centerMap() {
        Location location = locationClient.getLastLocation();
        LatLng latLng;
        if (location == null) {
            latLng = new LatLng(0, 0);
        } else {
            latLng = new LatLng(location.getLatitude(), location.getLongitude());

        }

        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng,
                15);
        setUpMapIfNeeded();
        if (map != null) {
            if (!hasCentered) {
                map.animateCamera(cameraUpdate);
                hasCentered = true;
            }
            map.setMyLocationEnabled(true);
            map.setBuildingsEnabled(true);
        }
    }

    /**
     * Method that finds the actual amount of pixels necessary for shifts of
     * textViews. Borrowed from
     * http://stackoverflow.com/questions/2406449/does-setwidthint
     * -pixels-use-dip-or-px
     * 
     * @param dip
     *            Density-Independent length
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
     * @param v
     *            The View for the deny button.
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
        ft.addToBackStack("info");
        aboutShowing = true;
        ft.commit();
    }

    /**
     * This is for the about icon, when the about icon is pressed then the user
     * can press the back button on their phone to get back to the map.
     */
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (aboutShowing) {
            findViewById(R.id.infoHolder).setClickable(false);
            aboutShowing = false;
        } else {
            Intent setIntent = new Intent(Intent.ACTION_MAIN);
            setIntent.addCategory(Intent.CATEGORY_HOME);
            setIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(setIntent);
        }
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
     * sOptOut creates a Dialog Interface that asks if they are sure they want
     * to opt out.
     */
    public void sOptOut(View v) {
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                case DialogInterface.BUTTON_POSITIVE:
                    // Yes button clicked
                    hasCentered = false;
                    SharedPreferences settings = getSharedPreferences(
                            SlidingPageIndicator.PREFS_NAME, 0);
                    SharedPreferences.Editor editor = settings.edit();

                    editor.putBoolean("hasLoggedIn", false);
                    editor.commit();

                    editor.putBoolean("transparent", false);
                    editor.commit();

                    Log.i(TAG,
                            "Experiment state is now EXP_STATE_NOT_YET_REGISTERED.");
                    mStore.put(RangzenService.EXPERIMENT_STATE_KEY,
                            RangzenService.EXP_STATE_OPTED_OUT);

                    // Stop Rangzen Service.
                    Intent rangzenServiceIntent = new Intent(
                            getApplicationContext(), RangzenService.class);
                    stopService(rangzenServiceIntent);

                    Intent intent = new Intent(getApplicationContext(),
                            SlidingPageIndicator.class);
                    startActivity(intent);
                    finish();

                    break;
                case DialogInterface.BUTTON_NEGATIVE:
                    break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Are you sure?")
                .setNegativeButton("No", dialogClickListener)
                .setPositiveButton("Yes", dialogClickListener).show();
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
        if (!hasCentered) {
            drawPoints(-1, -1, 0);
            centerMap();
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

    @Override
    protected void onPause() {
        map.setMyLocationEnabled(false);
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
        if (map != null) {
            map.setMyLocationEnabled(true);
        } else {
            Toast.makeText(this, "map was null in onResume", Toast.LENGTH_SHORT)
                    .show();
        }
        registerForLocationUpdates();
    }

    /** A handle to the Android location manager. */
    private LocationManager mLocationManager;

    /**
     * String designating the provider we want to use (GPS) for location. We
     * need the accuracy GPS provides - network based location is accurate to
     * within like a mile, according to the docs.
     */
    private static final String LOCATION_GPS_PROVIDER = LocationManager.PASSIVE_PROVIDER;

    /** Time between location updates in milliseconds - 1 minute. */
    private static final long LOCATION_UPDATE_INTERVAL = 5000 * 60 * 1;

    /**
     * Minimum moved distance between location updates. We want a new location
     * even if we haven't moved, so we set it to 0.
     * */
    private static final float LOCATION_UPDATE_DISTANCE_MINIMUM = 0;

    /** Handle to Rangzen location storage provider. */
    private LocationStore mLocationStore;

    private void registerForLocationUpdates() {
        if (mLocationManager == null) {
            Log.e(TAG,
                    "Can't register for location updates; location manager is null.");
            return;
        }
        mLocationManager.requestLocationUpdates(LOCATION_GPS_PROVIDER,
                LOCATION_UPDATE_INTERVAL, LOCATION_UPDATE_DISTANCE_MINIMUM,
                mLocationListener);
        Log.i(TAG, "Registered for location every " + LOCATION_UPDATE_INTERVAL
                + "ms");
    }

    /**
     * Called from OnCreate in order to only draw the previous points once.
     * 
     * @param maxValue
     *            The max value to draw points to.
     * @param minValue
     *            The min value of the points to include on the map.
     */
    private void drawPoints(Integer minValue, Integer maxValue, Integer isArrow) {
        numAsync++;
        ProgressBar pb = (ProgressBar) findViewById(R.id.progress);
        pb.setVisibility(View.VISIBLE);
        new DrawPointsThread().execute(minValue, maxValue, isArrow);
    }

    /**
     * Will add a polyline between previous and current points on a location
     * change, if the distance between them is large enough.
     */
    private LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
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
            Log.d(TAG, "Provider " + provider + " status changed to status "
                    + status);
        }

    };

    /**
     * Determines if the distance between two LatLng points is large enough
     * using the haversine method.
     * 
     * @param prev
     *            previous saved LatLng point.
     * @param current
     *            current saved LatLng point.
     * @return true if the distance is large enough, false otherwise.
     */
    private boolean distanceAllows(LatLng prev, LatLng current) {
        if (haversine(prev.latitude, prev.longitude, current.latitude,
                current.longitude) > 10d) {
            return true;
        }
        return false;
    }

    /**
     * Used to determine if the points are far enough away to be worth drawing a
     * polyline.
     * 
     * @param lat1
     *            - Latitude of previous point.
     * @param lon1
     *            - Longitude of previous point.
     * @param lat2
     *            - Latitude of current point.
     * @param lon2
     *            - Longitude of current point.
     * @return true if far enough away, false if not.
     */
    public double haversine(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        lat1 = Math.toRadians(lat1);
        lat2 = Math.toRadians(lat2);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.sin(dLon / 2)
                * Math.sin(dLon / 2) * Math.cos(lat1) * Math.cos(lat2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return radius * c;
    }

    /** Radius of the Erf (Earth). */
    private int radius = 6371000;

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play
     * services APK is correctly installed) and the map has not already been
     * instantiated. This will ensure that we only ever manipulate the map once
     * when it is not null.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the
        // map.
        if (map == null) {
            mapFragment = ((SupportMapFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.mapHolder));
            if (mapFragment != null) {
                map = mapFragment.getMap();
            }
        }
    }

    public class DrawPointsThread extends AsyncTask<Integer, Integer, Integer> {

        private ArrayList<Polyline> polylines = new ArrayList<Polyline>();
        private ArrayList<PolylineOptions> options = new ArrayList<PolylineOptions>();
        private PolylineOptions polyline;
        private List<Exchange> ownExchanges;
        private ArrayList<Marker> ownMarkers = new ArrayList<Marker>();
        private int size = -1;
        private long lowerTimeBound;
        private long upperTimeBound;
        private boolean isArrow = false;

        @Override
        protected Integer doInBackground(Integer... integers) {

            polyline = new PolylineOptions();
            setUpMapIfNeeded();
            List<SerializableLocation> locations;
            LatLng prevLL = null;
            try {
                if (integers[2] == -1) {
                    isArrow = true;
                }
                if (integers[0] == -1 && integers[1] == -1) {
                    locations = mLocationStore.getAllLocations();
                } else {
                    locations = mLocationStore.getLocations(integers[0],
                            integers[1]);
                }
                size = locations.size();
                if (map != null) {
                    for (int i = 0; i < size; i++) {
                        SerializableLocation current = locations.get(i);
                        if (i == 0) {
                            lowerTimeBound = current.time;
                        }
                        if (i == size - 1) {
                            upperTimeBound = current.time;
                        }
                        LatLng currentLL = new LatLng(current.latitude,
                                current.longitude);
                        if (prevLL == null) {
                            prevLL = currentLL;
                        }
                        if (distanceAllows(prevLL, currentLL)) {
                            if (sizePoly > 50) {
                                options.add(polyline);
                                polyline = new PolylineOptions();
                                sizePoly = 0;
                            }
                            polyline.add(prevLL, currentLL).width(4)
                                    .color(Color.BLUE).visible(true);
                            sizePoly++;
                        }
                        prevLL = currentLL;
                    }
                }
                // Catch remaining points under max allowed in one poly.
                if (sizePoly > 0) {
                    options.add(polyline);
                    polyline = new PolylineOptions();
                    sizePoly = 0;
                }
                ExchangeStore exchangeStore = new ExchangeStore(
                        getApplicationContext(), StorageBase.ENCRYPTION_DEFAULT);
                ownExchanges = exchangeStore.getAllExchanges();
                Log.d(TAG, "size " + ownExchanges.size());

            } catch (ClassNotFoundException | IOException e) {
                Log.e(TAG, "Not able to make polyLine on Async!");
                e.printStackTrace();
            }
            return 1;
        }

        @Override
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);
            if (numAsync == 1) {
                polylines = new ArrayList<Polyline>();
                ownMarkers = new ArrayList<Marker>();
                for (PolylineOptions poly : options) {
                    polylines.add(map.addPolyline(poly));
                }
                for (Exchange exchange : ownExchanges) {
                    if (exchange.start_time > lowerTimeBound
                            && exchange.end_time < upperTimeBound) {
                        MarkerOptions marker = new MarkerOptions();
                        LatLng exStart = new LatLng(
                                exchange.start_location.latitude,
                                exchange.start_location.longitude);
                        marker.position(exStart);
                        ownMarkers.add(map.addMarker(marker));
                    }
                }
                if (!isArrow) {
                    for (Polyline poly : array) {
                        poly.remove();
                    }
                    for (Marker marker : markers) {
                        marker.remove();
                    }
                    array = polylines;
                    markers = ownMarkers;
                } else {
                    for (Polyline polyline : polylines) {
                        array.add(polyline);
                    }
                    for (Marker marker : ownMarkers) {
                        markers.add(marker);
                    }
                }
            }
            Toast.makeText(getApplicationContext(),
                    "number of points being shown = " + size,
                    Toast.LENGTH_SHORT).show();
            // TODO (Jesus) Finish the Async race... condition.
            numAsync -= 1;
            ProgressBar pb = (ProgressBar) findViewById(R.id.progress);
            pb.setVisibility(View.INVISIBLE);
        }
    }
}