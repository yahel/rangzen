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

import android.app.Activity;
import android.content.BroadcastReceiver; 
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONObject;


/**
 * Main UI activity for experimental Rangzen app. Interfaces with user
 * permitting them to send/receive messages, manage friends, etc.
 */
public class MainActivity extends Activity {
  /** Tag displayed in Android Log messages logged from this class. */
  public static final String TAG = "RangzenMainActivity";

  /**
   * Sets the content view to the main activity UI and spawns the message
   * receiving service.
   *
   * @param savedInstanceState If the activity is being re-initialized,
   * this contains the Bundle saved using onSaveInstanceState last time.
   * Not used.
   * @see android.app.Activity
   */
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    // Spawn Rangzen Service.
    Log.i(TAG, "Starting Rangzen Service.");
    Intent rangzenServiceIntent = new Intent(this, RangzenService.class);
    startService(rangzenServiceIntent);
  }

  /**
   * Registers a receiver for Rangzen messages receieved and broadcasted
   * by the message receiving service.
   */
  @Override
  protected void onResume() {
    super.onResume();
    // TODO(lerner): Register the appropriate handlers, ensure that the right
    // services are running.
  }

  /**
   * Unregister our message broadcast receiver while we're not foregrounded.
   */
  @Override
  protected void onPause() {
    super.onPause();
    // TODO(lerner): Unregister the appropriate handlers.
  }

  /**
   * Populates the options menu
   * @param menu The menu to be populated.
   * @return Whether or not to display the menu.
   */
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.main, menu);
    return true;
  }

  /**
   * Takes user entered message from UI text entry field and sends it.
   *
   * @param view The view that triggered sendMessage (probably a button).
   */
  public void sendMessage(View view) {
    EditText editMessage = (EditText) findViewById(R.id.edit_message);
    String message = editMessage.getText().toString();
    Log.i(TAG, "sendMessage is obsolete: " + message);
  }

  /**
   * Displays a message in the UI.
   *
   * @param message The message to be displayed.
   */
  private void displayMessage(String message) {
    TextView displayMessageView = (TextView) findViewById(R.id.display_message);
    displayMessageView.setText(message);
  }

}
