package org.denovogroup.rangzen;

import android.app.Activity;
import android.app.ListActivity;
import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

public class SearchableActivity extends ListActivity {
    
    private static String TAG = "SearchableActivity";
    
    public void onCreate(Bundle savedInstanceState) {
        Log.e(TAG, "searchableActivity");
        super.onCreate(savedInstanceState);
        handleIntent(getIntent());
     } 

     public void onNewIntent(Intent intent) {
        setIntent(intent);
        handleIntent(intent);
     } 

     public void onListItemClick(ListView l,
        View v, int position, long id) {
        // call detail activity for clicked entry
     } 

     private void handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
           String query =
                 intent.getStringExtra(SearchManager.QUERY);
           doSearch(query);
        }
     }    

     private void doSearch(String queryStr) {
     // get a Cursor, prepare the ListAdapter
     // and set it
     }

}
