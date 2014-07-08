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

import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.model.GraphUser;
import com.facebook.widget.LoginButton;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;

/**
 * This class manages the behavior for all of the introductory fragments as well
 * as the behavior of the transparent fragment before the maps. It is in control
 * of text fields, positioning, as well as any Facebook activities.
 */

public class IntroductionFragment extends Fragment {
    private RelativeLayout rl;
    Bitmap bd;
    private static final String TAG = "MainFragment";
    private UiLifecycleHelper uiHelper;
    /** This is the specific Rangzen font. */
    // Typeface myTypeface = Typeface.createFromAsset(getActivity().getAssets(),
    // "fonts/zwod.ttf");
    Typeface zwodTypeFace;

    /**
     * These were an attempt to speed up the fragment sliding, I don't know if
     * they are necessary at all.
     */
    BitmapDrawable first;
    BitmapDrawable second;
    BitmapDrawable third;
    BitmapDrawable fourth;
    ImageView firstI;
    ImageView secondI;
    ImageView thirdI;
    ImageView fourthI;
    LoginButton authButton;

    /**
     * This method controls five fragment options, with different cases for
     * each, mostly it returns a formatted fragment, but it also creates the
     * UIHelper.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        Bundle b = getArguments();
        int whichScreen = b.getInt("whichScreen");
        uiHelper = new UiLifecycleHelper(getActivity(), callback);
        uiHelper.onCreate(savedInstanceState);
        zwodTypeFace = Typeface.createFromAsset(getActivity().getAssets(),
                "fonts/zwod.ttf");
        switch (whichScreen) {
        case 0:
            View view = (View) inflater.inflate(R.layout.firstintro, container,
                    false);
            rl = (RelativeLayout) view.findViewById(R.id.firstIntro);
            // bd = makeDrawable(R.drawable.newyorkyyy);

            if (first == null) {
                showFullScreenImage(R.drawable.newyorkyyyyy, 1);
                // showFullScreenImage(R.drawable.brightskyyy, 1);
            } else {
                firstI.setBackgroundDrawable(first);
            }
            makeRanzenTextView();
            makeBottomTextView("Freedom to Change");
            return view;

        case 1:
            View view1 = inflater.inflate(R.layout.secondintro, container,
                    false);
            rl = (RelativeLayout) view1.findViewById(R.id.secondIntro);
            // bd = makeDrawable(R.drawable.brightcityyyyyyyyyyyy);

            if (second == null) {
                // showFullScreenImage(R.drawable.newyorkyyyyy, 2);
                showFullScreenImage(R.drawable.hands, 2);
            } else {
                secondI.setBackgroundDrawable(second);
            }

            makeRanzenTextView();
            makeBottomTextView("Pass other Rangzen users and share messages");
            return view1;

        case 2:
            View view2 = inflater
                    .inflate(R.layout.thirdintro, container, false);
            rl = (RelativeLayout) view2.findViewById(R.id.thirdIntro);
            // bd = makeDrawable(R.drawable.soccer);

            if (third == null) {
                showFullScreenImage(R.drawable.soccer, 3);
            } else {
                thirdI.setBackgroundDrawable(second);
            }

            makeRanzenTextView();
            makeBottomTextView("Be completely anonymous");
            return view2;

        case 3:

            View view3 = (View) inflater.inflate(R.layout.fourthintro,
                    container, false);
            rl = (RelativeLayout) view3.findViewById(R.id.fourthIntro);
            if (third == null) {
                showFullScreenImage(R.drawable.blue, 3);
            } else {
                thirdI.setBackgroundDrawable(second);
            }
            authButton = (LoginButton) view3.findViewById(R.id.authButton);
            authButton.setFragment(this);
            authButton.setReadPermissions(Arrays
                    .asList("user_friends", "email"));
            makeRanzenTextView();
            authButton.bringToFront();
            view3.findViewById(R.id.facebookHolder).bringToFront();
            authButton.setClickable(false);
            return view3;
        case 4:

            View view4 = inflater.inflate(R.layout.transparent, container,
                    false);
            LinearLayout linLayoutOfView4 = (LinearLayout) view4;
            TextView bigT = new TextView(getActivity());
            TextView littleT = new TextView(getActivity());
            linLayoutOfView4.addView(bigT);
            linLayoutOfView4.addView(littleT);
            bigT.setTypeface(zwodTypeFace);
            littleT.setTypeface(zwodTypeFace);
            final float scale = getResources().getDisplayMetrics().density;
            int topBoxpixels = (int) (450 * scale + 0.5f);
            int textSize = (int) (20 * scale + .5f);
            bigT.setGravity(Gravity.CENTER);
            littleT.setGravity(Gravity.CENTER);
            bigT.setTextSize(textSize);
            bigT.setTextColor(Color.WHITE);
            littleT.setTextColor(Color.BLACK);
            littleT.setTextSize(textSize);
            bigT.setHeight(topBoxpixels);
            bigT.setText("Top text");
            littleT.setText("bottom text");
            return view4;

        case 5:

            View view5 = (View) inflater.inflate(R.layout.info, container,
                    false);
            return view5;

        default:
            return null;
        }

    }

    /**
     * Makes the textView on the bottom of the introductory fragments.
     * 
     * @param string
     *            The specific message that will be shown.
     */
    private void makeBottomTextView(String string) {
        TextView tv = new TextView(getActivity());
        tv.setTypeface(zwodTypeFace);
        tv.setText(string);
        tv.setTextSize(20);
        rl.addView(tv);
        tv.setTextColor(Color.WHITE);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
        layoutParams.setMargins(0, 920, 0, 0); // (L, T, ?, ?)
        tv.setLayoutParams(layoutParams);
        tv.setWidth(750);
        tv.setGravity(Gravity.CENTER);
    }

    /**
     * This takes the image, caches it, resizes it and then adds it to the
     * relative layout.
     * 
     * @param picture
     *            The location of the picture e.g - R.drawable.xxxx
     * @param position
     *            Which slide is currently asking to display the image.
     */
    private void showFullScreenImage(int picture, int position) {
        Display display = getActivity().getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;

        Bitmap bd = decodeSampledBitmapFromResource(getResources(), picture,
                width, height);

        ImageView iv = new ImageView(getActivity());
        // BitmapDrawable bd = BitmapDrawable(picture, bd);
        BitmapDrawable ob = new BitmapDrawable(bd);
        iv.setBackgroundDrawable(ob);
        rl.addView(iv, new LinearLayout.LayoutParams(width, height));

        if (position == 1) {
            first = ob;
            firstI = iv;
        } else if (position == 2) {
            secondI = iv;
            second = ob;
        } else if (position == 3) {
            thirdI = iv;
            third = ob;
        } else if (position == 4) {
            fourthI = iv;
            fourth = ob;
        }
    }

    /**
     * Finds the drawable in the resources and determines how much data will
     * need to be decoded into a bitmap to reduce the number of operations done
     * on the bitmap for resizing
     * 
     * @param res
     *            getResources()
     * @param resId
     *            R.drawable.xxxx
     * @param reqWidth
     *            The width of the screen
     * @param reqHeight
     *            The height of the screen
     * @return Returns the bitmap calculated to the screen size
     */
    public static Bitmap decodeSampledBitmapFromResource(Resources res,
            int resId, int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, resId, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth,
                reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(res, resId, options);
    }

    /**
     * Does scaling math to determine proper scaling of the image.
     * 
     * @param options
     *            Allows the caller to query the set without allocating memory
     *            for the pixels
     * @param reqWidth
     *            The screen width
     * @param reqHeight
     *            Screen Height
     * @return Proper Ratio???
     */
    public static int calculateInSampleSize(BitmapFactory.Options options,
            int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and
            // keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    /**
     * This just creates the Rangzen textView at the top of the introductory
     * slides.
     */
    private void makeRanzenTextView() {
        TextView tv = new TextView(getActivity());
        Typeface myTypeface = Typeface.createFromAsset(getActivity()
                .getAssets(), "fonts/zwod.ttf");
        tv.setTypeface(myTypeface);
        tv.setText("Rangzen");
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(55);
        rl.addView(tv);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
        layoutParams.setMargins(0, 150, 0, 0); // (L, T, ?, ?)
        tv.setLayoutParams(layoutParams);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public void onPause() {
        super.onPause();
        uiHelper.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        uiHelper.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        uiHelper.onSaveInstanceState(outState);
    }

    /**
     * For scenarios where the main activity is launched and user session is not
     * null, the session state change notification may not be triggered. Trigger
     * it if it's open/closed.
     */
    @Override
    public void onResume() {
        getActivity().setRequestedOrientation(
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        super.onResume();
        uiHelper.onResume();
        Session session = Session.getActiveSession();
        if (session != null && (session.isOpened() || session.isClosed())) {
            onSessionStateChange(session, session.getState(), null);
        }

        uiHelper.onResume();
    }

    /**
     * This is immediately after the facebook activity has ended. It then puts
     * the data that facebook has given into the next acitivity.
     * 
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        // TODO Figure out exactly how to send friend's list?

        super.onActivityResult(requestCode, resultCode, data);
        uiHelper.onActivityResult(requestCode, resultCode, data);

        Session.getActiveSession().onActivityResult(getActivity(), requestCode,
                resultCode, data);
        Request.newMeRequest(Session.getActiveSession(),
                new Request.GraphUserCallback() {
                    @Override
                    public void onCompleted(GraphUser user, Response response) {
                        if (user != null) {
                            // User has successfully logged in, save this
                            // information
                            // We need an Editor object to make preference
                            // changes.
                            SharedPreferences settings = getActivity()
                                    .getSharedPreferences(
                                            SlidingPageIndicator.PREFS_NAME, 0);
                            SharedPreferences.Editor editor = settings.edit();

                            editor.putBoolean("hasLoggedIn", true);
                            editor.commit();

                            Intent inetnt = new Intent(getActivity(),
                                    MapsActivity.class);
                            inetnt.putExtra("Fb_id", user.getId());
                            inetnt.putExtra("user_name", user.getName());
                            startActivity(inetnt);
                            getActivity().finish();
                        }
                    }
                }).executeAsync();

    }

    @Override
    public void onStart() {
        super.onStart();
    }

    private void onSessionStateChange(Session session, SessionState state,
            Exception exception) {
        if (state.isOpened()) {
            Log.i(TAG, "Logged in...");

        } else if (state.isClosed()) {
            Log.i(TAG, "Logged out...");
        }
    }

    /** Provides asynchronous notification of Session state changes. */
    private Session.StatusCallback callback = new Session.StatusCallback() {
        @Override
        public void call(Session session, SessionState state,
                Exception exception) {
            onSessionStateChange(session, state, exception);
        }

    };

}
