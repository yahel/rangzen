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
  formUri = "http://lacamas.cs.washington.edu:5984/acra-rangzen-test/_design/acra-storage/_update/report",
  reportType = org.acra.sender.HttpSender.Type.JSON,
  httpMethod = org.acra.sender.HttpSender.Method.PUT,
  formUriBasicAuthLogin="rangzenReporter",
  formUriBasicAuthPassword="rangzenReporterPassword",

  mode = ReportingInteractionMode.TOAST,
  resToastText = 1
)

public class RangzenApplication extends Application {

  private static String TAG = "RangzenApplication";

  @Override
  public final void onCreate() {
    super.onCreate();

    // This is a hack due to the fact that the strings aren't constants anymore?
    Log.i(TAG, "Initializing ACRA");
    ACRA.init(this);
    ACRA.getConfig().setResToastText(R.string.crash_toast_text); 
    Log.i(TAG, "Done Initializing ACRA");
  }
}
