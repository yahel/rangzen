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

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.denovogroup.rangzen.RangzenService;
import org.denovogroup.rangzen.StorageBase;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.ActionMode;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.viewpagerindicator.LinePageIndicator;
import com.viewpagerindicator.PageIndicator;

import org.servalproject.shell.Shell;
import org.servalproject.system.Chipset;
import org.servalproject.system.ChipsetDetection;
import org.servalproject.system.CoreTask;
import org.servalproject.system.LogOutput;
import org.servalproject.system.WifiAdhocControl;
import org.servalproject.system.WifiAdhocNetwork;
import org.servalproject.system.WifiApControl;
import org.servalproject.system.WifiControl;
import org.servalproject.system.WifiControl.Completion;
import org.servalproject.system.WifiControl.CompletionReason;
import org.servalproject.system.WifiMode;

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

    public WifiControl wifiControl;

    
    /**
     * Give your SharedPreferences file a name and save it to a static variable.
     */
    public static final String PREFS_NAME = "rememberPagesSeen";

    private StorageBase mStore; 

    IntroductionFragmentAdapter mAdapter;
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

        WifiAdhocNetwork.sContext = this;
        WifiApControl.sContext = this;
        CoreTask.sContext = this;
        WifiMode.sContext = this;

        Thread t = new Thread() {
          @Override
          public void run() {
            Log.v(TAG, "Extracting serval.zip");
            try { 
              Shell shell = Shell.startRootShell();
              AssetManager m = SlidingPageIndicator.this.getAssets();
              CoreTask coretask = new CoreTask();
              coretask.extractZip(m.open("serval.zip"), new File(coretask.DATA_FILE_PATH));
              Log.i(TAG, "Extracted serval.zip to " + coretask.DATA_FILE_PATH);
            } catch (IOException e) {
              Log.e(TAG, "IOException extracting serval.zip: " + e);
            }
            ChipsetDetection.context = getApplicationContext();
            ChipsetDetection cd = ChipsetDetection.getDetection();

            Set<Chipset> chipsets = cd.getDetectedChipsets();
            Log.i(TAG, "Detected chipsets: " + chipsets);

            wifiControl = new WifiControl(SlidingPageIndicator.this);
            wifiControl.off(new Completion() {
              @Override
              public void onFinished(CompletionReason reason) {
                try { 
                  Shell shell = Shell.startRootShell();
                  if (wifiControl.testAdhoc(shell, null)) {
                    Log.i(TAG, "testAdhoc succeeded.");
                  } else {
                    Log.e(TAG, "testAdhoc failed.");
                  }
                } catch (IOException e) {
                  Log.e(TAG, "IOException while testing whether Adhoc is available!: " + e);
                }
                if (WifiAdhocControl.isAdhocSupported()) {
                  Log.i(TAG, "Adhoc wifi is supported according to Serval Mesh.");
                } else {
                  Log.e(TAG, "Adhoc wifi NOT supported according to Serval Mesh.");
                }
              }
            });
          }
        };
        t.start();

        mStore = new StorageBase(this, StorageBase.ENCRYPTION_DEFAULT);
        String state = getExperimentState();
        if (state == null) {
          Log.i(TAG, "Initializing experiment state to EXP_STATE_START.");
          mStore.put(RangzenService.EXPERIMENT_STATE_KEY, RangzenService.EXP_STATE_START);
        } else {
          Log.i(TAG, "Creating SlidingPageIndicator, state is " + state);
        }

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
     * This is an onClickListener for a LinearLayout that covers the Facebook
     * LoginButton, it detects a click on the Facebook Button and if there is
     * internet service then the click will go through, if not then there will
     * be no facebook activity created.
     * 
     * @param v
     *            The Linear Layout that holds the Facebook LoginButton
     */
    public void linLayoutButton(View v) {
        Fragment fragment = new FragmentOrganizer();
        Bundle b = new Bundle();
        b.putSerializable("whichScreen", FragmentType.FIRSTABOUT);
        fragment.setArguments(b);
        FragmentManager fm = (FragmentManager) getSupportFragmentManager();
        fm.beginTransaction().add(R.id.firstAbout, fragment).addToBackStack("about").commit();
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    /**
     * Retrieve the current experiment state. Sets the experiment state to
     * START if none is set already.
     */
    private String getExperimentState() {
      return mStore.get(RangzenService.EXPERIMENT_STATE_KEY);
    }
}
