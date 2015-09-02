package org.denovogroup.rangzen.ui;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.AttributeSet;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import org.denovogroup.rangzen.R;

/** This activity crates the message sending page. It also handles back button. */
public class PostActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle arg0) {
        super.onCreate(arg0);
        setContentView(R.layout.fragment_holder);
        if(getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
            getActionBar().setTitle("New Post");
            /*int titleId = getResources().getIdentifier("action_bar_title", "id", "android");
            TextView abTitle = (TextView) findViewById(titleId);
            abTitle.setTextColor(Color.WHITE);*/
        }
        Fragment fragment = new FragmentOrganizer();
        Bundle b = new Bundle();
        b.putSerializable("whichScreen", FragmentOrganizer.FragmentType.POST);
        fragment.setArguments(b);
//        getFragmentManager().beginTransaction().replace(R.id.firstFrame, fragment).commit();
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
            InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            EditText mEditText = (EditText) findViewById(R.id.editText1);
            mgr.hideSoftInputFromWindow(mEditText.getWindowToken(), 0);
            getActionBar().setTitle("Feed");
            //setResult(0);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        EditText mEditText = (EditText) findViewById(R.id.editText1);
        mgr.hideSoftInputFromWindow(mEditText.getWindowToken(), 0);
    }
}
