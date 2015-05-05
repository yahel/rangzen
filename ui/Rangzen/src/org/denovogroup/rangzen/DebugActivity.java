package org.denovogroup.rangzen;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class DebugActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.debug);
        updateDeviceAreaBox();
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

    private void updateDeviceAreaBox() {
      PeerManager pm = PeerManager.getInstance(this); 
      List<Peer> peers = pm.getPeers();
      String peerString = "# of nearby peers: " + peers.size() + "\n";
      for (Peer p : peers) {
        peerString += p.toString() + "\n";
      }
      TextView peerTextView = (TextView) findViewById(R.id.device_area);
      if (peerTextView != null) {
        peerTextView.setText(peerString);
      } else {
        Toast.makeText(this, "Couldn't find device_area text box " + R.id.device_area, Toast.LENGTH_SHORT).show();
      }
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
          updateDeviceAreaBox();
        }

        return super.onOptionsItemSelected(item);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.debug_menu, menu);
        return true;
    }

}
