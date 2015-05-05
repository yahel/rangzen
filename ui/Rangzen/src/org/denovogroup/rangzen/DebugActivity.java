package org.denovogroup.rangzen;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

public class DebugActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setUpActionBar();
    }

    private void setUpActionBar() {
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setTitle("Debug");
        int titleId = getResources().getIdentifier("action_bar_title", "id",
                "android");
        TextView abTitle = (TextView) findViewById(titleId);
        abTitle.setTextColor(Color.WHITE);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Respond to the action bar's Up/Home button
        if (item.getItemId() == android.R.id.home) {
            getActionBar().setTitle("Feed");
            finish();
            return true;
        }
        if (item.getItemId() == R.id.refresh) {
            // (TODO) Adam
            Toast.makeText(this, "Todo Adam", Toast.LENGTH_SHORT).show();
        }

        return super.onOptionsItemSelected(item);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.debug_menu, menu);
        return true;
    }

}