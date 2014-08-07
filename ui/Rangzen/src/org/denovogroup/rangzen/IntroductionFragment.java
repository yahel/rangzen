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

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.TypedValue;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * This class manages the behavior for all of the introductory fragments as well
 * as the behavior of the transparent fragment before the maps. It is in control
 * of text fields, positioning, as well as any Facebook activities.
 */
public class IntroductionFragment extends Fragment {
    private RelativeLayout currentRelativeLayout;
    private FrameLayout currentFrameLayout;
    private ImageView iv;
    private static final String TAG = "MainFragment";
    // Typeface zwodTypeFace;

    /**
     * These were an attempt to speed up the fragment sliding, I don't know if
     * they are necessary at all.
     */
    Bitmap first;
    Bitmap second;
    Bitmap third;
    Bitmap fourth;
    ImageView firstI;
    ImageView secondI;
    ImageView thirdI;
    ImageView fourthI;
    FrameLayout fl;

    /**
     * This is the amount in density pixels that the title of the app will be
     * from the top of the screen.
     */
    private int marginFromTop = 50;

    /**
     * This method controls five fragment options, with different cases for
     * each, mostly it returns a formatted fragment, but it also creates the
     * UIHelper.
     * 
     * @param inflater
     *            A tool used to get the java code of a layout in XML.
     * @param container
     *            This fragments containing frame.
     * @param savedInstanceState
     *            The memory of previous instances of this fragment.
     * @return returns the layout (fragment), already formatted to be displayed.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        Bundle b = getArguments();
        int whichScreen = b.getInt("whichScreen");
        switch (whichScreen) {
        case 0:
            View view = (View) inflater.inflate(R.layout.firstintro, container,
                    false);
            currentRelativeLayout = (RelativeLayout) view
                    .findViewById(R.id.firstIntro);
            currentFrameLayout = (FrameLayout) view.findViewById(R.id.firstFrame);
            iv = (ImageView) view.findViewById(R.id.imageView1);

            showFullScreenImage(R.drawable.newyorkyyyyy, 1);
            makeRanzenTextView();
            TextView tv = (TextView) view.findViewById(R.id.textView1);
            tv.bringToFront();
            Button cont = (Button) view.findViewById(R.id.contButton1);
            cont.setText("Continue");
            cont.setTextColor(Color.WHITE);
            cont.bringToFront();
            currentRelativeLayout.bringToFront();
            return view;

        case 1:
            View view1 = inflater.inflate(R.layout.secondintro, container,
                    false);
            currentRelativeLayout = (RelativeLayout) view1
                    .findViewById(R.id.secondIntro);
            currentFrameLayout = (FrameLayout) view1.findViewById(R.id.secondFrame);
            iv = (ImageView) view1.findViewById(R.id.imageView2);
            showFullScreenImage(R.drawable.hands, 2);

            makeRanzenTextView();

            Button cont1 = (Button) view1.findViewById(R.id.contButton2);
            cont1.setText("Continue");
            cont1.setTextColor(Color.WHITE);
            cont1.bringToFront();
            TextView tv2 = (TextView) view1.findViewById(R.id.textView2);
            tv2.bringToFront();
            currentRelativeLayout.bringToFront();
            return view1;

        case 2:
            View view2 = inflater
                    .inflate(R.layout.thirdintro, container, false);
            currentRelativeLayout = (RelativeLayout) view2
                    .findViewById(R.id.thirdIntro);
            currentFrameLayout = (FrameLayout) view2.findViewById(R.id.thirdFrame);
            iv = (ImageView) view2.findViewById(R.id.imageView3);
            
            showFullScreenImage(R.drawable.soccer, 3);

            makeRanzenTextView();
            Button cont2 = (Button) view2.findViewById(R.id.contButton3);
            cont2.setText("Continue");
            cont2.setTextColor(Color.WHITE);
            cont2.bringToFront();

            TextView tv3 = (TextView) view2.findViewById(R.id.textView3);
            tv3.bringToFront();
            currentRelativeLayout.bringToFront();

            return view2;

        case 5:

            View view5 = (View) inflater.inflate(R.layout.info, container,
                    false);
            return view5;

        case 6:

            View view6 = (View) inflater.inflate(R.layout.permissions,
                    container, false);
            view6.setSoundEffectsEnabled(false);
            return view6;
        default:
            return null;
        }

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
        BitmapDrawable ob = new BitmapDrawable(bd);
        iv.setBackgroundDrawable(ob);
        if (position == 1) {
            first = bd;
            firstI = iv;
        } else if (position == 2) {
            secondI = iv;
            second = bd;
        } else if (position == 3) {
            thirdI = iv;
            third = bd;
        } else if (position == 4) {
            fourthI = iv;
            fourth = bd;
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
        // Typeface myTypeface = Typeface.createFromAsset(getActivity()
        // .getAssets(), "fonts/zwod.ttf");
        // tv.setTypeface(myTypeface);
        tv.setText("Rangzen");
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(55);
        currentRelativeLayout.addView(tv);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
        layoutParams.setMargins(0, (int) getPixels(marginFromTop), 0, 0); // (L,
                                                                          // T,
                                                                          // ?,
                                                                          // ?)
        tv.setLayoutParams(layoutParams);
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
     * For scenarios where the main activity is launched and user session is not
     * null, the session state change notification may not be triggered. Trigger
     * it if it's open/closed.
     */
    @Override
    public void onResume() {
        super.onResume();
    }
}
