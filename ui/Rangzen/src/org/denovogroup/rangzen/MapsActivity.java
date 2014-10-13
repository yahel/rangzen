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

import org.denovogroup.rangzen.FragmentOrganizer.FragmentType;
import org.denovogroup.rangzen.RangeSeekBar.OnRangeSeekBarChangeListener;

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
import android.widget.AdapterView;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.StateListDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
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
public class MapsActivity extends Fragment implements
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener, OnClickListener {

    /** Displayed in Android Log messages. */
    private static final String TAG = "MapsActivity";

    /**
     * The fragment that contains the map itself. It is the second most top
     * fragment in its parent FrameLayout.
     */
    private SupportMapFragment mMapFragment;

    /**
     * Variable that will last the length of the activity, so the map will not
     * be centered gain while the activity is up.
     */
    private static boolean mHasCentered = false;

    /**
     * The map object itself that is inside of the SupportMapFragment.
     */
    private GoogleMap mMap;
    /**
     * Used to connect and disconnect from Google Location Services and
     * locations.
     */
    private LocationClient mLocationClient;
    /**
     * The transparent fragment. It is the bottom most layer of the FrameLayout
     * but is brought to the top if created.
     */
    private Fragment mTransparent;
    /**
     * This is the object that manages all of the Fragments visible and
     * invisible, and adds, replaces or removes fragments.
     */
    private FragmentManager mFragmentManager;

    /**
     * Handle to Rangzen storage manager.
     */
    private StorageBase mStore;

    /** Used to determine if the slider is on or off. */
    private static boolean mIsSliderOn = false;

    /** RangeBar slider that will determine how many points will be shown. */
    private RangeSeekBar<Double> mPolyLineRange;

    /**
     * Define a request code to send to Google Play services This code is
     * returned in Activity.onActivityResult
     */
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;

    /**
     * Polyline ArrayList in order to hold all polyline options as the next one
     * is built.
     */
    private ArrayList<Polyline> mPolylineArray = new ArrayList<Polyline>();

    /** Size of the current polyline. */
    private int mSizeofPolyline = 0;

    /** Number of running Async Tasks. */
    private static int mNumAsyncTasks = 0;

    /** List of exchanges for this current map. */
    private ArrayList<Marker> mMarkers = new ArrayList<Marker>();

    // /** Helps with determining what functionality the back button should
    // have. */
    // private static boolean mAboutShowing = false;

    /** Percent change that the slider should change by. */
    private final int mIntegerPercentChange = 10;

    private Bitmap bitmap1;
    private Bitmap bitmap2;
    private Bitmap bitmap3;
    private Bitmap bitmap4;
    private Bitmap bitmap5;
    private Bitmap bitmap1a;
    private Bitmap bitmap2a;
    private Bitmap bitmap3a;
    private Bitmap bitmap4a;
    private Bitmap bitmap5a;

    private View view;

    /**
     * Sets up the initial FragmentManager and if there is no savedInstanceState
     * for this app then new fragments are created for the map interface.
     * 
     * @param savedInstanceState
     *            This is the memory of the last state of this application.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // setContentView(R.layout.master);
        view = inflater.inflate(R.layout.master, container, false);
        mLocationClient = new LocationClient(getActivity(), this, this);
        // createButtonImage(R.id.ib, R.drawable.abouticon19,
        // R.drawable.pressedinfo, false, 1, view);
        createButtonImage(R.id.button22, R.drawable.slider,
                R.drawable.sliderpressed, false, 2, view);
        createButtonImage(R.id.refresh, R.drawable.refresh,
                R.drawable.refreshpressed, false, 3, view);
        createButtonImage(R.id.leftArrow, R.drawable.rightarrow,
                R.drawable.rightarrowpressed, true, 4, view);
        createButtonImage(R.id.rightArrow, R.drawable.rightarrow,
                R.drawable.rightarrowpressed, false, 5, view);

        if (savedInstanceState == null) {

            mStore = new StorageBase(getActivity(),
                    StorageBase.ENCRYPTION_DEFAULT);
            mLocationStore = new LocationStore(getActivity(),
                    StorageBase.ENCRYPTION_NONE);
            mMapFragment = (SupportMapFragment) SupportMapFragment
                    .newInstance();
            // mMapFragment.setRetainInstance(true);
            mMap = mMapFragment.getMap();
            mFragmentManager = getActivity().getSupportFragmentManager();
            mFragmentManager.beginTransaction()
                    .replace(R.id.mapHolder, mMapFragment).commit();
            // createTransparentFragment();
        }

        if (mMapFragment == null) {
            setUpMapIfNeeded();
            if (mMapFragment == null) {
                Toast.makeText(getActivity(), "map fragment was null but creatinga new one", Toast.LENGTH_SHORT).show();
                mMapFragment = (SupportMapFragment) SupportMapFragment
                        .newInstance();
                // mMapFragment.setRetainInstance(true);
                mMap = mMapFragment.getMap();
                mFragmentManager = getActivity().getSupportFragmentManager();
                mFragmentManager.beginTransaction()
                        .replace(R.id.mapHolder, mMapFragment).commit();
            }
            mMap.setMyLocationEnabled(true);
        }
        createSlider();
        return view;
    }

    /** This will be used to debug the location services being turned on and off. */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        // TODO (jesus) Find out why the location services is not coming back on.
        super.onActivityCreated(savedInstanceState);
        setUpMapIfNeeded();
        if (mMap != null) { 
            //mMap.setMyLocationEnabled(true);
        }
    }

    /**
     * Creates a clickable about icon on the google maps interface. This icon
     * needs to be scaled down and the icon itself is created in the xml file,
     * "master.xml".
     */
    private void createButtonImage(int buttonId, int notPressedImage,
            int pressedImage, boolean invert, int count, View view) {
        ImageButton button = (ImageButton) view.findViewById(buttonId);
        button.bringToFront();

        Bitmap icon = BitmapFactory.decodeResource(getResources(),
                notPressedImage);
        Bitmap icon2 = BitmapFactory.decodeResource(getResources(),
                pressedImage);

        if (invert) {
            Matrix matrix = new Matrix();
            matrix.postRotate(180);
            icon = Bitmap.createBitmap(icon, 0, 0, icon.getWidth(),
                    icon.getHeight(), matrix, true);

            Matrix matrix2 = new Matrix();
            matrix2.postRotate(180);
            icon2 = Bitmap.createBitmap(icon2, 0, 0, icon2.getWidth(),
                    icon2.getHeight(), matrix2, true);
        }

        StateListDrawable states = new StateListDrawable();
        states.addState(new int[] { android.R.attr.state_pressed },
                new BitmapDrawable(icon2));
        states.addState(new int[] { android.R.attr.state_focused },
                new BitmapDrawable(icon2));
        states.addState(new int[] {}, new BitmapDrawable(icon));

        button.setBackgroundDrawable(states);

        if (count == 1) {
            bitmap1 = icon;
            bitmap1a = icon2;
        }
        if (count == 2) {
            bitmap2 = icon;
            bitmap2a = icon2;
        }
        if (count == 3) {
            bitmap3 = icon;
            bitmap3a = icon2;
        }
        if (count == 4) {
            bitmap4 = icon;
            bitmap4a = icon2;
        }
        if (count == 5) {
            bitmap5 = icon;
            bitmap5a = icon2;
        }
        button.setOnClickListener(this);
    }

    /**
     * Manually garbage collect and unbind the drawables in this map frame so
     * they can be collected.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        mHasCentered = false;
        unbindDrawables(getActivity().findViewById(R.id.mapFrame));
        System.gc();
    }

    /**
     * Recursively go through the views in this view and unbind their
     * backgrounds so they can be garbage collected.
     * 
     * @param view
     *            In this case it will be map frame, the ultimate parent UI
     *            frame.
     */
    private void unbindDrawables(View view) {
        if (view.getBackground() != null) {
            view.getBackground().setCallback(null);
        }
        if (view instanceof ViewGroup && !(view instanceof AdapterView)) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                unbindDrawables(((ViewGroup) view).getChildAt(i));
            }
            ((ViewGroup) view).removeAllViews();
        }
    }

    /**
     * Initialize the slider and its on click listener with a min and max range
     * (taken care of edge cases with no locations stored).
     */
    private void createSlider() {
        mPolyLineRange = new RangeSeekBar<Double>(0d, 1d, getActivity());
        OnRangeSeekBarChangeListener<Double> change = new OnRangeSeekBarChangeListener<Double>() {

            @Override
            public void onRangeSeekBarValuesChanged(RangeSeekBar<?> bar,
                    Double minValue, Double maxValue) {
                // TODO Auto-generated method stub
                mLocationStore = new LocationStore(getActivity(),
                        StorageBase.ENCRYPTION_DEFAULT);
                int size = mLocationStore.getMostRecentSequenceNumber();
                if (maxValue != -1 && maxValue != minValue
                        && minValue < maxValue && minValue != -1) {
                    int lowDraw = (int) (minValue * size);
                    int highDraw = (int) (maxValue * size);
                    if (lowDraw == 0) {
                        lowDraw++;
                    }
                    if (highDraw == 0) {
                        highDraw++;
                    }
                    drawPoints(lowDraw, highDraw, 0);
                }
            }
        };
        mPolyLineRange.setOnRangeSeekBarChangeListener(change);
        LinearLayout slider = (LinearLayout) view.findViewById(R.id.slider);
        slider.addView(mPolyLineRange);
    }

    /**
     * Will only be called in OnCreate and also the app needs to have been
     * destroyed in order for the method to be called again.
     */
    private void centerMap() {
        Location location = mLocationClient.getLastLocation();
        LatLng latLng;
        if (location == null) {
            latLng = new LatLng(0, 0);
        } else {
            latLng = new LatLng(location.getLatitude(), location.getLongitude());

        }

        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng,
                15);
        setUpMapIfNeeded();
        if (mMap != null) {
            if (!mHasCentered) {
                mMap.animateCamera(cameraUpdate);
                mHasCentered = true;
            }
            //Toast.makeText(getActivity(), "location enabled in centermap", Toast.LENGTH_SHORT).show();
            mMap.setMyLocationEnabled(true);
            mMap.setBuildingsEnabled(true);
        }
    }

    //(TODO) Jesus - Put this functionality in other parts of the app.
//    /**
//     * The onClickListener of the Deny button on the transparent page. Used to
//     * restart the introduction slides if they do not accept.
//     * 
//     * @param v
//     *            The View for the deny button.
//     */
//    public void sDeny(View v) {
//        Log.i(TAG, "Denied permission.");
//        Intent intent = new Intent();
//        mHasCentered = false;
//        intent.setClass(this, SlidingPageIndicator.class);
//        startActivity(intent);
//        finish();
//    }

//    /**
//     * Make sure that the introduction and the transparent page will never be
//     * seen again and go through to the map.
//     * 
//     * @param v
//     *            - View for the accept button
//     */
//    public void sAccept(View v) {
//        Log.i(TAG, "Accepted permission.");
//        mTransparent.getView().setClickable(false);
//        mTransparent.getView().setVisibility(View.INVISIBLE);
//        ViewGroup vg = (ViewGroup) mTransparent.getView().getParent();
//        vg.setClickable(false);
//        vg.setVisibility(View.INVISIBLE);
//        ViewGroup vgParent = (ViewGroup) vg.getParent();
//        vgParent.removeView(vg);
//
//        SharedPreferences settings = getSharedPreferences(
//                SlidingPageIndicator.PREFS_NAME, 0);
//        SharedPreferences.Editor editor = settings.edit();
//
//        editor.putBoolean("hasLoggedIn", true);
//        editor.commit();
//
//        editor.putBoolean("transparent", true);
//        editor.commit();
//
//        Log.i(TAG, "Experiment state is now EXP_STATE_NOT_YET_REGISTERED.");
//        mStore = new StorageBase(this, StorageBase.ENCRYPTION_DEFAULT);
//        mStore.put(RangzenService.EXPERIMENT_STATE_KEY,
//                RangzenService.EXP_STATE_NOT_YET_REGISTERED);
//
//        // Spawn Rangzen Service.
//        Log.i(TAG, "Permission granted - Starting Rangzen Service.");
//        Intent rangzenServiceIntent = new Intent(this, RangzenService.class);
//        getActivity().startService(rangzenServiceIntent);
//    }

    /**
     * OnClickListener for the about icon, this handles the creation of a new
     * fragment and pressing back button to get back to the map.
     * 
     * @param v
     *            The view that contains the about icon.
     */
    public void s(View v) {
        Fragment info = new FragmentOrganizer();
        Bundle b = new Bundle();
        b.putSerializable("whichScreen", FragmentType.SECONDABOUT);
        info.setArguments(b);
        getActivity().findViewById(R.id.infoHolder).setClickable(true);
        mFragmentManager = getActivity().getSupportFragmentManager();
        FragmentTransaction ft = mFragmentManager.beginTransaction();
        ft.add(R.id.infoHolder, info);
        ft.addToBackStack("info");
        ft.commit();
    }

    /**
     * Stores into sharedPreferences that the transparent page has already been
     * seen. This also adds the transparent fragment into a linear layout in
     * master.xml.
     */
    private void createTransparentFragment() {
        // Checking to create transparency.
        SharedPreferences settings = getActivity().getSharedPreferences(
                SlidingPageIndicator.PREFS_NAME, 0);
        // Get "transparent" value. If the value doesn't exist yet false is
        // returned
        boolean hasSeentransparent = settings.getBoolean("transparent", false);

        if (!hasSeentransparent) {
            // //set has seen transparency in a shared save preference
            mTransparent = new FragmentOrganizer();
            Bundle b2 = new Bundle();
            b2.putSerializable("whichScreen", FragmentType.TRANSPARENT);
            mTransparent.setArguments(b2);
            getActivity().findViewById(R.id.transparentHolder).bringToFront();
            mFragmentManager.beginTransaction()
                    .replace(R.id.transparentHolder, mTransparent).commit();
        }
    }

    /**
     * Called when Rangzen is first visible and this connects to the google
     * services.
     */
    @Override
    public void onStart() {
        super.onStart();
        // connect the client
        if (isGooglePlayServicesAvailable()) {
            mLocationClient.connect();
        }
    }

    /**
     * Called when the Activity is no longer visible.
     */
    @Override
    public void onStop() {
        // Disconnecting the client invalidates it.
        mLocationClient.disconnect();
        setUpMapIfNeeded();
        //mMap.setMyLocationEnabled(false);
        super.onStop();
    }

    /** Kill all the bitmaps. */
    public void recycleBitmaps() {
        if (bitmap1 != null && bitmap3 != null && bitmap5 != null
                && bitmap2a != null) {
            bitmap1.recycle();
            bitmap1a.recycle();
            bitmap2.recycle();
            bitmap2a.recycle();
            bitmap3.recycle();
            bitmap3a.recycle();
            bitmap4.recycle();
            bitmap4a.recycle();
            bitmap5.recycle();
            bitmap5a.recycle();

            bitmap1 = null;
            bitmap1a = null;
            bitmap2 = null;
            bitmap2a = null;
            bitmap3 = null;
            bitmap3a = null;
            bitmap4 = null;
            bitmap4a = null;
            bitmap5 = null;
            bitmap5a = null;
        }
    }

    /**
     * Handle results returned to the FragmentActivity by Google Play services.
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Decide what to do based on the original request code
        switch (requestCode) {

        case CONNECTION_FAILURE_RESOLUTION_REQUEST:
            /*
             * If the result code is Activity.RESULT_OK, try to connect again
             */
            switch (resultCode) {
            case Activity.RESULT_OK:
                mLocationClient.connect();
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
                .isGooglePlayServicesAvailable(getActivity());
        // If Google Play services is available
        if (ConnectionResult.SUCCESS == resultCode) {
            return true;
        } else {
            // Get the error dialog from Google Play services
            Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(
                    resultCode, getActivity(),
                    CONNECTION_FAILURE_RESOLUTION_REQUEST);

            // If Google Play services can provide an error dialog
            if (errorDialog != null) {
                // Create a new DialogFragment for the error dialog
                ErrorDialogFragment errorFragment = new ErrorDialogFragment();
                errorFragment.setDialog(errorDialog);
                errorFragment.show(getActivity().getSupportFragmentManager(),
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
        if (!mHasCentered) {
            drawPoints(-1, -1, 0);
            centerMap();
        }
        setUpMapIfNeeded();
        
        mMap.setMyLocationEnabled(true);
    }

    /**
     * Called by Location Services when the request to connect the client
     * finishes successfully. At this point, you can request the current
     * location or start periodic updates
     */
    @Override
    public void onDisconnected() {
        if (!mHasCentered) {
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

    @Override
    public void onPause() {
        //mMap.setMyLocationEnabled(false);
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        setUpMapIfNeeded();
        if (mMap != null) {
            mMap.setMyLocationEnabled(true);
        } else {
            Toast.makeText(getActivity(), "map was null in onResume",
                    Toast.LENGTH_SHORT).show();
        }
    }

    /** Time between location updates in milliseconds - 1 minute. */
    private static final long LOCATION_UPDATE_INTERVAL = 5000 * 60 * 1;

    /**
     * Minimum moved distance between location updates. We want a new location
     * even if we haven't moved, so we set it to 0.
     * */
    private static final float LOCATION_UPDATE_DISTANCE_MINIMUM = 0;

    /** Handle to Rangzen location storage provider. */
    private LocationStore mLocationStore;

    /**
     * Called from OnCreate in order to only draw the previous points once.
     * 
     * @param maxValue
     *            The max value to draw points to.
     * @param minValue
     *            The min value of the points to include on the map.
     */
    private void drawPoints(Integer minValue, Integer maxValue, Integer isArrow) {
        mNumAsyncTasks++;
        ProgressBar pb = (ProgressBar) getActivity()
                .findViewById(R.id.progress);
        pb.setVisibility(View.VISIBLE);
        new DrawPointsThread().execute(minValue, maxValue, isArrow);
    }

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
        if (mMap == null) {
            mMapFragment = ((SupportMapFragment) getActivity()
                    .getSupportFragmentManager().findFragmentById(
                            R.id.mapHolder));
            if (mMapFragment != null) {
                mMap = mMapFragment.getMap();
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
        private LocationStore mLocationStore = new LocationStore(getActivity(),
                StorageBase.ENCRYPTION_DEFAULT);

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
                if (mMap != null) {
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
                            if (mSizeofPolyline > 50) {
                                options.add(polyline);
                                polyline = new PolylineOptions();
                                mSizeofPolyline = 0;
                            }
                            polyline.add(prevLL, currentLL).width(4)
                                    .color(Color.BLUE).visible(true);
                            mSizeofPolyline++;
                        }
                        prevLL = currentLL;
                    }
                }
                // Catch remaining points under max allowed in one poly.
                if (mSizeofPolyline > 0) {
                    options.add(polyline);
                    polyline = new PolylineOptions();
                    mSizeofPolyline = 0;
                }
                // This used to retrieve exchanges from the ExchangeStore,
                // which has been removed from the app.
                ownExchanges = new ArrayList<Exchange>();
                Log.d(TAG, "size of own exchanges" + ownExchanges.size());

            } catch (ClassNotFoundException | IOException e) {
                Log.e(TAG, "Not able to make polyLine on Async!");
                e.printStackTrace();
            }
            return 1;
        }

        @Override
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);
            if (mNumAsyncTasks == 1) {
                polylines = new ArrayList<Polyline>();
                ownMarkers = new ArrayList<Marker>();
                for (PolylineOptions poly : options) {
                    polylines.add(mMap.addPolyline(poly));
                }
                for (Exchange exchange : ownExchanges) {
                    if (exchange.start_time > lowerTimeBound
                            && exchange.end_time < upperTimeBound) {
                        MarkerOptions marker = new MarkerOptions();
                        LatLng exStart = new LatLng(
                                exchange.start_location.latitude,
                                exchange.start_location.longitude);
                        marker.position(exStart);
                        ownMarkers.add(mMap.addMarker(marker));
                    }
                }
                if (!isArrow) {
                    for (Polyline poly : mPolylineArray) {
                        poly.remove();
                    }
                    for (Marker marker : mMarkers) {
                        marker.remove();
                    }
                    mPolylineArray = polylines;
                    mMarkers = ownMarkers;
                } else {
                    for (Polyline polyline : polylines) {
                        mPolylineArray.add(polyline);
                    }
                    for (Marker marker : ownMarkers) {
                        mMarkers.add(marker);
                    }
                }
            }
            Toast.makeText(getActivity(),
                    "Number of points being shown = " + size,
                    Toast.LENGTH_SHORT).show();
            // TODO (Jesus) Finish the Async race... condition.
            mNumAsyncTasks -= 1;
            ProgressBar pb = (ProgressBar) getActivity().findViewById(
                    R.id.progress);
            if (pb != null) {
                pb.setVisibility(View.INVISIBLE);
            }
        }
    }

    @Override
    public void onClick(View v) {
        int viewID = v.getId();
        if (viewID == R.id.refresh) {
            sRefresh(v);
            return;
        } else if (viewID == R.id.leftArrow) {
            sLeftArrow(v);
        } else if (viewID == R.id.rightArrow) {
            sRightArrow(v);
        } else if (viewID == R.id.button22) {
            sSliderButton(v);
        } else {
            return;
            // if viewID == R.id.ib
            // s(v);
        }
    }

    /**
     * Redraw all of the points that the phone contains.
     * 
     * @param v
     *            The refresh button itself.
     */
    public void sRefresh(View v) {
        mLocationStore = new LocationStore(getActivity(),
                StorageBase.ENCRYPTION_DEFAULT);
        int size = mLocationStore.getMostRecentSequenceNumber();
        if (size != 0) {
            drawPoints(-1, -1, 0);
            mPolyLineRange.setNormalizedMaxValue(1);
            mPolyLineRange.setNormalizedMinValue(0);
        }
    }

    /**
     * This the is the slider button OnClickListener, it turns the slider
     * invisible and visible.
     * 
     * @param v
     *            The image button itself.
     */
    public void sSliderButton(View v) {
        LinearLayout slider = (LinearLayout) view.findViewById(R.id.slider);
        if (!mIsSliderOn || slider.getVisibility() == View.INVISIBLE) {
            slider.setVisibility(View.VISIBLE);
            mIsSliderOn = true;
        } else {
            slider.setVisibility(View.INVISIBLE);
            mIsSliderOn = false;
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
        double max = mPolyLineRange.getSelectedMaxValue();
        double min = mPolyLineRange.getSelectedMinValue();
        mLocationStore = new LocationStore(getActivity(),
                StorageBase.ENCRYPTION_DEFAULT);
        int size = mLocationStore.getMostRecentSequenceNumber();
        double percentChange = ((double) mIntegerPercentChange / 100.0);
        double highDrawPercent = (max + percentChange);

        int highDraw = (int) (highDrawPercent * size);
        if (highDraw == 0) {
            highDraw++;
        }
        int lowDraw = (int) (min * size);
        if (lowDraw == 0) {
            lowDraw++;
        }
        if (size == -1) {
            return;
        }

        if (max + percentChange >= 1) {
            drawPoints(lowDraw, size, -1);
            mPolyLineRange.setNormalizedMaxValue(size);
        } else {
            drawPoints(lowDraw, highDraw, -1);
            mPolyLineRange.setNormalizedMaxValue(highDrawPercent);
        }
    }

    /**
     * The left arrow that will show with the slider to show the next x many
     * points in the past not yet being shown.
     * 
     * @param v
     *            The left arrow button.
     */
    public void sLeftArrow(View v) {
        double min = mPolyLineRange.getSelectedMinValue();
        double max = mPolyLineRange.getSelectedMaxValue();
        mLocationStore = new LocationStore(getActivity(),
                StorageBase.ENCRYPTION_DEFAULT);
        int size = mLocationStore.getMostRecentSequenceNumber();
        double percentChange = ((double) mIntegerPercentChange / 100.0);
        double lowDrawPercent = (min - percentChange);
        Log.d(TAG, "value of min =" + min);
        Log.d(TAG, "value of max = " + max);
        Log.d(TAG, "value of size = " + size);
        Log.d(TAG, "value of change = " + percentChange);
        Log.d(TAG, "value of lowDraw =" + lowDrawPercent);

        if (size == -1) {
            return;
        }

        int highDraw = (int) (max * size);
        if (highDraw == 0) {
            highDraw++;
        }
        int lowDraw = (int) (lowDrawPercent * size);
        if (lowDraw == 0) {
            lowDraw++;
        }

        if (lowDrawPercent <= 0) {
            drawPoints(1, highDraw, -1);
            mPolyLineRange.setNormalizedMinValue(0);
        } else {
            drawPoints(lowDraw, highDraw, -1);
            mPolyLineRange.setNormalizedMinValue(lowDrawPercent);
        }
    }
}
