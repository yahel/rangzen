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

import org.denovogroup.rangzen.FragmentOrganizer.FragmentType;
import org.denovogroup.rangzen.RangzenService;
import org.denovogroup.rangzen.StorageBase;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.CursorJoiner.Result;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.viewpagerindicator.LinePageIndicator;
import com.viewpagerindicator.PageIndicator;

/**
 * This class creates the LinePageIndicator, which are the moving lines on the
 * bottom of the screen for different tabs. These are both created on the
 * simple_lines.xml file which is a layout that is merged with the introductory
 * fragments giving them the indicator and sliding properties. This class also
 * creates the ViewPager and also ensures the introduction only is shown once.
 */
public class QRCodeViewPager extends FragmentActivity {

    /** Shown in Android log. */
    private static final String TAG = "SlidingPageIndicator";

    /**
     * Give your SharedPreferences file a name and save it to a static variable.
     */
    public static final String INTENT = "rememberPagesSeen";

    private static final String SCAN_ACTION = "com.google.zxing.client.android.SCAN";

    private StorageBase mStore;

    QRPagesAdapter mAdapter;
    ViewPager mPager;
    PageIndicator mIndicator;

    /**
     * This is the first activity of the application and it checks to see if the
     * introduction was already shown. If it was then the maps interface is
     * brought up. If it was not, then introduction is handled.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mStore = new StorageBase(this, StorageBase.ENCRYPTION_DEFAULT);
        String state = getExperimentState();
        if (state == null) {
            Log.i(TAG, "Initializing experiment state to EXP_STATE_START.");
            mStore.put(RangzenService.EXPERIMENT_STATE_KEY,
                    RangzenService.EXP_STATE_START);
        } else {
            Log.i(TAG, "Creating SlidingPageIndicator, state is " + state);
        }

        Intent intent = getIntent();
        SharedPreferences settings = getSharedPreferences(
                QRCodeViewPager.INTENT, 0);
        SharedPreferences.Editor editor = settings.edit();

        editor.putBoolean("returnResult",
                intent != null && SCAN_ACTION.equals(intent.getAction()));
        editor.commit();

        setContentView(R.layout.qr_simple_lines);

        mAdapter = new QRPagesAdapter(getSupportFragmentManager());

        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setAdapter(mAdapter);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    /**
     * Retrieve the current experiment state. Sets the experiment state to START
     * if none is set already.
     */
    private String getExperimentState() {
        return mStore.get(RangzenService.EXPERIMENT_STATE_KEY);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (mPager.getCurrentItem() == 1) {
            CameraFragment cam = (CameraFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.pager);
            com.google.zxing.Result result = cam.getResult();
            if (result != null) {
                switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_CENTER:
                    cam.handleResult(result);
                    return true;
                case KeyEvent.KEYCODE_BACK:
                    cam.reset();
                    return true;
                }
            }
        }
        return super.onKeyDown(keyCode, event);
    }

}
