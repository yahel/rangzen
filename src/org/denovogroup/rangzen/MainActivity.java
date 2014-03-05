package org.denovogroup.rangzen;

import android.os.Bundle;

import android.util.Log;
import android.view.View;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONObject;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    
    public void sendMessage(View view) {
    	EditText editMessage = (EditText) findViewById(R.id.edit_message);
    	String message = editMessage.getText().toString();
		Log.i("sendMessage", "sending message "+message);

    	Intent sendIntent = new Intent(this, MessageSendIntentService.class);
    	sendIntent.putExtra(MessageSendIntentService.MESSAGE_TO_SEND, message);
    	startService(sendIntent);
    }
    
    private void displayMessage(String message) {
    	TextView displayMessageView = (TextView) findViewById(R.id.display_message);
    	displayMessageView.setText(message);
    }
    
}
