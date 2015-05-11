package org.denovogroup.rangzen;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

/** This activity crates the message sending page. It also handles back button. */
public class InfoActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle arg0) {
        super.onCreate(arg0);
        setContentView(R.layout.fragment_holder);
        setUpActionBar();
        createFragment();
        
        Log.i(TAG, "Attempting to learn build time and show it...");
        TextView buildTimeView = (TextView) findViewById(R.id.buildTime); 
        if (buildTimeView != null) {
          String buildTime = getBuildTime(); 
          Log.i(TAG, "Build time view is not null. Build time is: " + buildTime);
          if (buildTime != null) {
            buildTimeView.setText(buildTime);
          } else {
            buildTimeView.setText("Not able to learn build time from APK.");
          }
        } else {
          Log.i(TAG, "Build time view was null.");
        }

    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.info_menu, menu);
        return true;
    }
    
    private void setUpActionBar() {
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setTitle("Info");
        int titleId = getResources().getIdentifier("action_bar_title", "id",
                "android");
        TextView abTitle = (TextView) findViewById(titleId);
        abTitle.setTextColor(Color.WHITE);
    }

    private void createFragment() {
        Fragment fragment = new FragmentOrganizer();
        Bundle b = new Bundle();
        b.putSerializable("whichScreen", FragmentOrganizer.FragmentType.SECONDABOUT);
        fragment.setArguments(b);
        FragmentManager fragmentManager = getSupportFragmentManager();

        FragmentTransaction ft = fragmentManager.beginTransaction();

        ft.replace(R.id.relativeLayout, fragment);

        ft.commit();
    }

    @Override
    public View onCreateView(View parent, String name, Context context,
            AttributeSet attrs) {
        return super.onCreateView(parent, name, context, attrs);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        // Respond to the action bar's Up/Home button
        case android.R.id.home:
            getActionBar().setTitle("Feed");
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);

    }
}
