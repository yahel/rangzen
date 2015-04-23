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

import org.acra.*;
import org.acra.annotation.*;

import android.app.Application;
import android.util.Log;


@ReportsCrashes(
  formKey = "",  // Required for backwards compatibility.
  formUri = "http://s.rangzen.io:5984/acra-rangzen/_design/acra-storage/_update/report",
  reportType = org.acra.sender.HttpSender.Type.JSON,
  httpMethod = org.acra.sender.HttpSender.Method.PUT,
  formUriBasicAuthLogin="rangzenReporter",
  formUriBasicAuthPassword="rangzenReporterPassword",

  mode = ReportingInteractionMode.TOAST,
  resToastText = 1
)
/**
 * The purpose of this class is to initialize ACRA, which is responsible for 
 * sending crash reports to our servers when the application encounters an
 * unhandled exception. ACRA can also be used to transmit bug reports and
 * debugging info on other conditions.
 */
public class RangzenApplication extends Application {

  /** Output in logcat messages. */
  private static String TAG = "RangzenApplication";

  @Override
  public final void onCreate() {
    super.onCreate();

    Log.d(TAG, "Initializing ACRA");
    ACRA.init(this);
    // This is a hack due to the fact that strings from XML aren't constants anymore
    // which means that it's hard to put them in the @ReportsCrashes annotation above.
    ACRA.getConfig().setResToastText(R.string.crash_toast_text); 
    Log.d(TAG, "Done Initializing ACRA");
  }
}
