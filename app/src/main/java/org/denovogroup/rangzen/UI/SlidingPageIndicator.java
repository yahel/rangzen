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

package org.denovogroup.rangzen.ui;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Display;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.viewpagerindicator.LinePageIndicator;
import com.viewpagerindicator.PageIndicator;

import org.denovogroup.rangzen.R;
import org.denovogroup.rangzen.backend.FriendStore;
import org.denovogroup.rangzen.backend.RangzenService;
import org.denovogroup.rangzen.backend.StorageBase;

/**
 * This class creates the LinePageIndicator, which are the moving lines on the
 * bottom of the screen for different tabs. These are both created on the
 * simple_lines.xml file which is a layout that is merged with the introductory
 * fragments giving them the indicator and sliding properties. This class also
 * creates the ViewPager and also ensures the introduction only is shown once.
 */
public class SlidingPageIndicator extends FragmentActivity {

    /** Shown in Android log. */
    private static final String TAG = "SlidingPageIndicator";

    /** 
     * URI scheme for Rangzen protocol. A Rangzen friending URI looks like
     * rangzen://<base64 encode of the bytes of our public ID>
     */
    public static final String QR_FRIENDING_SCHEME = "rangzen://";

    /**
     * Name of the file where the boolean remembering if the user has already
     * seen the introduction is stored.
     */
    public static final String PREFS_NAME = "rememberPagesSeen";
    /**
     * Name of the file in internal memory where the user's qr code bitmap is
     * stored.
     */
    private static final String FILENAME = "qrcode";

    IntroductionFragmentAdapter mAdapter;
    ViewPager mPager;
    PageIndicator mIndicator;

    /**
     * This is the first activity of the application and it checks to see if the
     * introduction was already shown. If it was then the Rangzen feed is shown.
     * If it was not, then introduction is handled.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!fileExists(FILENAME)) {
          FriendStore store = new FriendStore(this, StorageBase.ENCRYPTION_DEFAULT);
          String publicID = store.getPublicDeviceIDString();
          if (publicID != null) {
            new CreateQRCode().execute(QR_FRIENDING_SCHEME + publicID);
          } else {
            Log.wtf(TAG, "PublicID is null on call to FriendStore.getPublicDeviceIDString()");
          }
        }

        // Start the RangzenService.
        Intent serviceIntent = new Intent(this, RangzenService.class);
        startService(serviceIntent);

        SharedPreferences settings = getSharedPreferences(
                SlidingPageIndicator.PREFS_NAME, 0);
        boolean hasLoggedIn = settings.getBoolean("hasLoggedIn", false);

        if (hasLoggedIn) {
            Log.i(TAG, "Has logged in.");
            Intent intent = new Intent();
            intent.setClass(SlidingPageIndicator.this, Opener.class);
            startActivity(intent);
            SlidingPageIndicator.this.finish();
        }
        setContentView(R.layout.simple_lines);

        mAdapter = new IntroductionFragmentAdapter(getSupportFragmentManager());

        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setAdapter(mAdapter);

        LinePageIndicator indicator = (LinePageIndicator) findViewById(R.id.indicator);
        mIndicator = indicator;
        indicator.setViewPager(mPager);
        mPager.setPageTransformer(true, new DepthPageTransformer());
        mPager.setOffscreenPageLimit(2);

        final float density = getResources().getDisplayMetrics().density;
        indicator.setSelectedColor(0xFFFFFFFF);
        indicator.setUnselectedColor(0xFF888888);
        indicator.setStrokeWidth(4 * density);
        indicator.setLineWidth(30 * density);
    }

    /**
     * This is the button that ends the slideshow of images in the introduction
     * of the app. It uses finish() in order to not allow the back button to
     * return to that activity.
     * 
     * @param v
     *            They view that contains the button being clicked.
     */
    public void linLayoutButton(View v) {

        Intent intent = new Intent();
        intent.setClass(v.getContext(), Opener.class);
        startActivity(intent);
        SharedPreferences settings = getSharedPreferences(
                SlidingPageIndicator.PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();

        editor.putBoolean("hasLoggedIn", true);
        editor.commit();
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    /**
     * Checks internal storage for a file called 'qrcode'
     * 
     * @param filename
     *            name of the file being looked for
     * @return If the file exists in internal memory.
     */
    public boolean fileExists(String filename) {
        File file = getFileStreamPath(filename);
        if (file == null || !file.exists()) {
            return false;
        }
        return true;
    }

    /**
     * Async task that creates the personal QR code and stores it as a file in
     * internal memory.
     *
     */
    private class CreateQRCode extends AsyncTask<String, Integer, Integer> {

        BitMatrix bitmap = null;

        /**
         * This creates a QR code with the user's QRCode content information
         * that is the size of the screen.
         * 
         * @params String that contains the user's QRCode content.
         */
        @Override
        protected Integer doInBackground(String... params) {
            WindowManager manager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
            Display display = manager.getDefaultDisplay();
            Point point = new Point();
            display.getSize(point);
            int width = point.x;
            int height = point.y;
            int tmp = width;
            width = height;
            height = tmp;
            Log.i(TAG, Integer.toString(point.x));
            Log.i(TAG, Integer.toString(point.y));

            QRCodeWriter qrCodeEncoder = new QRCodeWriter();

            Log.i(TAG, "file does not exist");

            try {
                bitmap = qrCodeEncoder.encode(params[0], BarcodeFormat.QR_CODE,
                        width, height);
            } catch (WriterException e) {
                e.printStackTrace();
            }

            return 1;
        }

        /**
         * Puts the bitmap into memory in internal storage in the file name
         * 'qrcode'
         */
        @Override
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);
            Bitmap bmp = toBitmap(bitmap);
            FileOutputStream out = null;

            try {
                out = openFileOutput(FILENAME, Context.MODE_PRIVATE);
                bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (out != null) {
                        out.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Writes the given Matrix on a new Bitmap object.
     * 
     * @param matrix
     *            the matrix to write.
     * @return the new {@link Bitmap}-object.
     */
    public Bitmap toBitmap(BitMatrix matrix) {
        int height = matrix.getHeight();
        int width = matrix.getWidth();
        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        try {
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bmp.setPixel(x, y, matrix.get(x, y) ? Color.BLACK
                            : Color.WHITE);
                }
            }
        } catch (IllegalStateException e) {
            Log.i(TAG, "immutable bitmap");
        }

        return bmp;
    }
}
