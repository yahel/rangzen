package org.denovogroup.rangzen.UI;

import java.util.List;

import org.denovogroup.rangzen.R;
import org.denovogroup.rangzen.backend.MessageStore;
import org.denovogroup.rangzen.backend.MessageStore.Message;
import org.denovogroup.rangzen.backend.StorageBase;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListView;

public class SearchableActivity extends ActionBarActivity {

    private static String TAG = "SearchableActivity";

    public void onCreate(Bundle savedInstanceState) {
        Log.e(TAG, "searchableActivity");
        super.onCreate(savedInstanceState);
        setUpActionBar();
        handleIntent(getIntent());
        setContentView(R.layout.feed);

        // Get the intent, verify the action and get the query
        Intent intent = getIntent();
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            doSearch(query);
            ListView lv = (ListView) findViewById(android.R.id.list);
            List<Message> messages = new MessageStore(this,
                    StorageBase.ENCRYPTION_DEFAULT).getAllMessages(
                    MessageStore.SEARCHED_MESSAGES, query);

            lv.setAdapter(new FeedListAdapter(this,
                    R.layout.feed_row_search, messages));
        }

        LinearLayout listCarrier = (LinearLayout) findViewById(R.id.listCarrier);
        listCarrier.removeViewAt(0);

    }

    public void onNewIntent(Intent intent) {
        setIntent(intent);
        handleIntent(intent);
    }

    public void onListItemClick(ListView l, View v, int position, long id) {
        // call detail activity for clicked entry
    }

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            doSearch(query);
        }
    }

    private void doSearch(String queryStr) {
        // get a Cursor, prepare the ListAdapter
        // and set it
    }

    /** Initialize the contents of the activities menu. */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the options menu from XML
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.search_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        // Respond to the action bar's Up/Home button
        case android.R.id.home:
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setUpActionBar() {
        if(getActionBar()!=null){
            getActionBar().setDisplayHomeAsUpEnabled(true);
            getActionBar().setTitle("Search Hashtags");
            /*int titleId = getResources().getIdentifier("action_bar_title", "id",
                    "android");
            TextView abTitle = (TextView) findViewById(titleId);
            abTitle.setTextColor(Color.WHITE);*/
        }
    }

}
