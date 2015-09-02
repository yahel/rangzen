/*
 * Copyright (c) 2014, De Novo Group
 * All Rights Reserved.
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
 * This software is provided by the copyright holders and contributors "as is"
 * and any express or implied warranties, including, but not limited to, the
 * implied warranties of merchantability and fitness for a particular purpose
 * are disclaimed. In no event shall the copyright holder or contributors be
 * liable for any direct, indirect, incidental, special, exemplary, or
 * consequential damages (including, but not limited to, procurement of
 * substitute goods or services; loss of use, data, or profits; or business
 * interruption) however caused and on any theory of liability, whether in
 * contract, strict liability, or tort (including negligence or otherwise)
 * arising in any way out of the use of this software, even if advised of the
 * possibility of such damage.
 */

package org.denovogroup.rangzen.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.app.Activity;

import org.denovogroup.rangzen.R;

/** 
 * This class is an activity that displays the info page before Maps
 * this activity is located before the MapsActivity.
 */
public class AboutPage extends Activity {
    /**
     *  This changes the text in the info layout's button to be "Continue"
     *  and shows the info page before the accept or deny page.
     */
    @Override
    protected void onCreate(Bundle arg0) {
        super.onCreate(arg0);
        LinearLayout lin = (LinearLayout) View.inflate(this, R.layout.info, null);
        setContentView(R.layout.modifiedabout);
        Button button = (Button) findViewById(R.id.continueBeforeMaps);
        button.setText("Continue");
        button.setOnClickListener(new View.OnClickListener() { 
            @Override
            public void onClick(View v) {
                // TODO(Jesus): Update this activity to current needs.
                // Doesn't need any buttons or anything, should have different text.
                // Might also just want to remove it entirely?
                //
                // Intent intent = new Intent();
                // intent.setClass(v.getContext(), MapsActivity.class);
                // startActivity(intent);
                // finish();
            }
        });
    }
}
