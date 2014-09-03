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

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.servalproject.shell.Shell;
import org.servalproject.system.CoreTask;
import org.servalproject.system.WifiAdhocControl;
import org.servalproject.system.WifiAdhocNetwork;
import org.servalproject.system.WifiApControl;
import org.servalproject.system.WifiApNetwork;
import org.servalproject.system.WifiControl;
import org.servalproject.system.WifiControl.Completion;
import org.servalproject.system.WifiControl.CompletionReason;

import android.content.Context;
import android.content.res.AssetManager;
import android.net.wifi.WifiManager;
import android.util.Log;

public class AdhocController {

  /** A context for accessing system resources. */
  private Context mContext;

  /** Instance of WifiControl used to manipulate Wifi Adhoc. */
  private WifiControl mWifiControl;

  /**
   * When testAdhoc is called, this boolean is set true or false depending on
   * whether testAdhoc determined that adhoc worked or didn't.  
   */
  private boolean adhocSupported;

  /**
   * Controller for becoming an AP.
   */
  private WifiApControl mWifiApControl;

  /** Instance of WifiAdhocControl used to manipulate WifiAdhoc. */
  private WifiAdhocControl mWifiAdhocControl;

  /** Included in Android monitor log messages. */
  private static String TAG = "AdhocController";

  /**
   * Create a new AdhocController which accesses system resources using the
   * given Context.
   *
   * @param context A context for accessing system resources.
   */
  public AdhocController(Context context) {
    this.mContext = context;
    this.mWifiControl = new WifiControl(mContext);
    WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
    this.mWifiApControl = WifiApControl.getApControl(wifiManager);
  }

  /**
   * Check whether being an adhoc AP is supported on this device according
   * to the assessment of Serval Mesh's WifiApControl.
   *
   * @return True if being an AP is supported, false otherwise.
   */
  public boolean isApModeSupported() {
    return WifiApControl.isApSupported();
  }

  /**
   * Attempt to become an AP.
   */
  public void activateApMode() {
    WifiApNetwork network = mWifiApControl.getDefaultNetwork();
    if (network != null && network.config != null) {
      Log.i(TAG, "Calling connectAp");
      mWifiControl.connectAp(network.config, null);
      Log.i(TAG, "Called connectAp");
    } else {
      Log.e(TAG, "Network or its config was null (network was: " + network + ")");
    }

  }

  /**
   * Activate Adhoc Mode.
   */
  public void activateAdhocMode() {
    // WifiAdhocNetwork network = WifiAdhocControl.getDefaultNetwork();
    if (network == null) {
      Log.e(TAG, "Attempted to connect to adhoc network that doesn't exist or something?");
      return;
    }
    mWifiControl.connectAdhoc(network, new Completion() {
      @Override
      public void onFinished(CompletionReason reason) {
        if (reason == CompletionReason.Success) {
          Log.i(TAG, "Connecting adhoc completed successfully!");
        } else if (reason == CompletionReason.Cancelled) {
          Log.w(TAG, "Connecting adhoc was cancelled.");
        } else {
          Log.e(TAG, "Connecting adhoc failed.");
        }
      }
    });
  }


  /**
   * A file called serval.zip is included in the assets/ directory of the APK
   * for Rangzen. It includes binaries for iw, ifconfig, iwconfig, etc., as 
   * well as a large number of scripts (.edify and .detect) which are used by
   * Serval Mesh's ChipsetDetection code to help identify the chipset of the 
   * phone. This method extracts serval.zip to storage so those scripts are
   * where Serval Mesh expects them to be.
   *
   * @return True if serval.zip was extracted successfully, false otherwise.
   */
  public boolean extractServalZip() {
    Log.v(TAG, "Extracting serval.zip");
    try { 
      AssetManager m = mContext.getAssets();
      CoreTask coretask = new CoreTask();
      coretask.extractZip(m.open("serval.zip"), new File(coretask.DATA_FILE_PATH));
      Log.i(TAG, "Extracted serval.zip to " + coretask.DATA_FILE_PATH);
      return true;
    } catch (IOException e) {
      Log.e(TAG, "IOException extracting serval.zip: " + e);
      return false;
    }
  }

  /**
   * Determine whether WifiAdhocControl thinks that Adhoc is supported.
   *
   * @return True if adhoc is supported, false otherwise.
   */
  public boolean isAdhocSupported() {
    return WifiAdhocControl.isAdhocSupported();
  }

  /**
   * Determine whether adhoc works on this device, including detecting and 
   * remembering the device's chipset.
   *
   * TODO(lerner): This method is void because we have to turn off wifi in order
   * to test adhoc using WifiControl for some reason I don't understand. That 
   * means calling testAdhoc() is asynchronous and I can't easily return its
   * return value.
   */
  public void testAdhoc() {
    // I found that testAdhoc would fail because the WifiControl's currentState 
    // wasn't empty (see WifiControl:1380) if I didn't call mWifiControl.off()
    // first.
    //
    // TODO(lerner): Initialize things in a smarter way so that we don't have
    // to turn off wifi to do this?
    mWifiControl.off(new Completion() {

      @Override
      public void onFinished(CompletionReason reason) {
        try { 
          Shell shell = Shell.startRootShell();
          if (mWifiControl.testAdhoc(shell, null)) {
            Log.i(TAG, "testAdhoc succeeded.");
            adhocSupported = true;
          } else {
            Log.e(TAG, "testAdhoc failed.");
            adhocSupported = false;
          }
        } catch (IOException e) {
          Log.e(TAG, "IOException while testing whether Adhoc is available!: " + e);
        }
        Log.i(TAG, "Adhoc supported? " + isAdhocSupported());
      }
    });
  }
  
}
