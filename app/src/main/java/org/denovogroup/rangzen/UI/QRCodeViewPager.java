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

import org.denovogroup.rangzen.R;
import org.denovogroup.rangzen.backend.StorageBase;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.widget.TextView;

import com.viewpagerindicator.PageIndicator;

/**
 * Creates a ViewPager for the apps QR code and the apps QR code reader. Added
 * functionality will be a page that goes before the QR pages to show how to add
 * friends.
 */
public class QRCodeViewPager extends FragmentActivity {

    /** Shown in Android log. */
    private static final String TAG = "QRCodeViewPager";

    /**
     * Give your SharedPreferences file a name and save it to a static variable.
     */
    public static final String INTENT = "rememberPagesSeen";

    private static final String SCAN_ACTION = "com.google.zxing.client.android.SCAN";
    
    /** String constant for the boolean put in memory for type of intent for CameraFragment. */
    private static final String cameraIntent = "CameraIntent";

    private StorageBase mStore;

    // QRPagesAdapter mAdapter;
    ViewPager mPager;
    PageIndicator mIndicator;

    /**
     * 
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mStore = new StorageBase(this, StorageBase.ENCRYPTION_DEFAULT);
        if(getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
            getActionBar().setTitle("Add Friend");
            /*int titleId = getResources().getIdentifier("action_bar_title", "id",
                    "android");
            TextView abTitle = (TextView) findViewById(titleId);
            abTitle.setTextColor(Color.WHITE);*/
        }

        Intent intent = getIntent();
        if (intent != null && SCAN_ACTION.equals(intent.getAction())) {
            mStore.putInt(cameraIntent, 1);
        } else {
            mStore.putInt(cameraIntent, 0);
        }

        setContentView(R.layout.qr_simple_lines);

        // mAdapter = new QRPagesAdapter(getSupportFragmentManager());

        mPager = (ViewPager) findViewById(R.id.pager);
        // mPager.setAdapter(mAdapter);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (mPager.getCurrentItem() == 0) {
            // CameraFragment cam = (CameraFragment) mAdapter
            //         .getRegisteredFragment(0);
            // com.google.zxing.Result result = cam.getResult();
            // if (result != null) {
            //     switch (keyCode) {
            //     case KeyEvent.KEYCODE_DPAD_CENTER:
            //         // cam.handleResult(result);
            //         return true;
            //     case KeyEvent.KEYCODE_BACK:
            //         // cam.reset();
            //         return true;
            //     }
            // } else {
          if (keyCode == KeyEvent.KEYCODE_BACK) {
              if(getActionBar() != null) {
                  getActionBar().setTitle("Feed");
               /*   int titleId = getResources().getIdentifier(
                          "action_bar_title", "id", "android");
                  TextView abTitle = (TextView) findViewById(titleId);
                  abTitle.setTextColor(Color.WHITE);*/
              }
            finish();
          }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        // Respond to the action bar's Up/Home button
        case android.R.id.home:
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
