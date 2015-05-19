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

import java.util.ArrayList;
import java.util.List;

import org.denovogroup.rangzen.MessageStore.Message;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.SearchManager;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.Handler;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTabHost;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NotificationCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * This class is the manager of all of the fragments that are clickable in the
 * sidebar. The sidebar itself is also programmed in this class and pulls new
 * activities or switches main fragment views.
 */
public class Opener extends ActionBarActivity implements OnItemClickListener {

    private DrawerLayout mDrawerLayout;
    private ListView mListView;
    private ActionBarDrawerToggle mDrawerListener;
    private SidebarListAdapter mSidebarAdapter;
    private static TextView mCurrentTextView;
    private static boolean mFirstTime = true;
    private static final String TAG = "Opener";
    
    public static final String SAVE = "SAVE";
    public static final String RETWEET = "RETWEET";

    private FragmentTabHost mTabHost;

    // Create reciever object
    private BroadcastReceiver receiver = new NewMessageReceiver();

    // Set When broadcast event will fire.
    private IntentFilter filter = new IntentFilter(MessageStore.NEW_MESSAGE);
    public final static int POSTED_MESSAGE = 999;

    private final static int QR = 10;
    private final static int MESSAGE = 20;
    private final static int UPVOTE = 1;
    private final static int DOWNVOTE = 0;

    /** Initialize the contents of the activities menu. */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);

        MenuItem searchItem = menu.findItem(R.id.menu_search);
        SearchManager searchManager = (SearchManager) getSystemService(SEARCH_SERVICE);
        if (searchItem != null) {
            SearchView searchView = (SearchView) searchItem.getActionView();
            if (searchView != null) {
                Log.e(TAG, "SearchView Not Null");
                searchView.setSearchableInfo(searchManager
                        .getSearchableInfo(getComponentName()));
            } else 
                Log.e(TAG, "SearchView Null");
            searchView.setIconifiedByDefault(false);
        }
        return true;
    }

    /**
     * This method initializes the listView of the drawer layout and the layout
     * itself.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.drawer_layout);
        addMessagesToStore();
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawerLayout);
        // activityRootView = drawerLayout;
        mListView = (ListView) findViewById(R.id.drawerList);
        mSidebarAdapter = new SidebarListAdapter(this);
        mListView.setAdapter(mSidebarAdapter);
        mListView.setOnItemClickListener(this);
        mDrawerListener = new ActionBarDrawerToggle(this, mDrawerLayout,
                R.drawable.ic_drawer, R.string.open, R.string.close) {

            @Override
            public boolean onOptionsItemSelected(MenuItem item) {
                return super.onOptionsItemSelected(item);
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                InputMethodManager inputMethodManager = (InputMethodManager) getApplication()
                        .getSystemService(Activity.INPUT_METHOD_SERVICE);
                inputMethodManager.hideSoftInputFromWindow(getWindow()
                        .getCurrentFocus().getWindowToken(), 0);
            }

        };

        mDrawerLayout.setDrawerListener(mDrawerListener);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, Gravity.LEFT);

        setUpTabHost();
    }
    
    private void addMessagesToStore() {
        MessageStore messageStore = new MessageStore(this,
                StorageBase.ENCRYPTION_DEFAULT);

        messageStore
                .addMessage(
                        "This is the Rangzen message feed. Messages in the ether will appear here.",
                        1L);
    }

    private void setUpTabHost() {

        mTabHost = (FragmentTabHost) findViewById(android.R.id.tabhost);
        mTabHost.setup(this, getSupportFragmentManager(), R.id.tabcontent);

        Bundle b = new Bundle();
        b.putSerializable("whichScreen",
                ListFragmentOrganizer.FragmentType.FEED);
        Bundle b3 = new Bundle();
        b3.putSerializable("whichScreen",
                ListFragmentOrganizer.FragmentType.SAVED);

        mTabHost.addTab(mTabHost.newTabSpec("Trust").setIndicator("Trust"),
                ListFragmentOrganizer.class, b);
        mTabHost.addTab(mTabHost.newTabSpec("Favorite")
                .setIndicator("Favorite"), ListFragmentOrganizer.class, b3);
    }

    /**
     * This allows the icon that shows a DrawerLayout is present to retract when
     * the layout disappears.
     */
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerListener.syncState();
    }

    /**
     * This lets the DrawerListener know that it will display itself when the
     * icon in the ActionBar is clicked.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerListener.onOptionsItemSelected(item)) {
            return true;
        }
        if (item.getItemId() == R.id.new_post) {
            showFragment(1);
        }
        if (item.getItemId() == R.id.refresh) {
            notifyDataSetChanged();
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * This handles orientation changes.
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerListener.onConfigurationChanged(newConfig);
    }

    /**
     * This displays the correct fragment at item position, and it also closes
     * the navigation drawer when an option has been chosen.
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,
            long id) {
        if (position != 1) {
            selectItem(position);
        }
        showFragment(position);
        mDrawerLayout.closeDrawers();
    }

    /**
     * This highlights the option that was just chosen and makes the title of
     * the page the fragment that was chosen.
     */
    public void selectItem(int position) {
        mListView.setItemChecked(position, true);
        if (position != 2) {
            String[] sidebar = getResources().getStringArray(R.array.sidebar);
            setTitle(sidebar[position]);
        } else {
            int titleId = getResources().getIdentifier("action_bar_title",
                    "id", "android");
            TextView abTitle = (TextView) findViewById(titleId);
            abTitle.setTextColor(Color.WHITE);
        }
    }

    /**
     * Sets the title located in the ActionBar to be the title of the chosen
     * fragment.
     * 
     * @param title
     *            The title of the page the user has chosen to navigate to.
     */
    public void setTitle(String title) {
        getSupportActionBar().setTitle(title);
        int titleId = getResources().getIdentifier("action_bar_title", "id",
                "android");
        TextView abTitle = (TextView) findViewById(titleId);
        abTitle.setTextColor(Color.WHITE);
    }

    public void makeTitleBold(int position) {
        View view = mListView.getChildAt(position);
        if (mCurrentTextView == null) {
            mCurrentTextView = (TextView) view.findViewById(R.id.textView1);
        }
        mCurrentTextView.setTypeface(null, Typeface.NORMAL);
        mCurrentTextView.setTextSize(17);
        mCurrentTextView = (TextView) view.findViewById(R.id.textView1);

        mCurrentTextView.setTypeface(null, Typeface.BOLD);
        mCurrentTextView.setTextSize(19);
    }

    /**
     * Called whenever any activity launched from this activity exits. For
     * example, this is called when returning from the QR code activity,
     * providing us with the QR code (if any) that was scanned.
     * 
     * @see Activity.onActivityResult
     */
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        Log.i(TAG, "Got activity result back in Opener!");

        int titleId = getResources().getIdentifier("action_bar_title", "id",
                "android");
        TextView abTitle = (TextView) findViewById(titleId);
        abTitle.setTextColor(Color.WHITE);
        abTitle.setText("Feed");

        // Check whether the activity that returned was the QR code activity,
        // and whether it succeeded.
        if (requestCode == QR && resultCode == RESULT_OK) {
            // Grab the string extra containing the QR code that was scanned.
            FriendStore fs = new FriendStore(this,
                    StorageBase.ENCRYPTION_DEFAULT);
            String code = intent
                    .getStringExtra(com.google.zxing.client.android.CaptureActivity.CODE_CONTENTS_EXTRA_KEY);
            // Convert the code into a public Rangzen ID.
            byte[] publicIDBytes = FriendStore.getPublicIDFromQR(code);
            Log.i(TAG, "In Opener, received intent with code " + code);

            // Try to add the friend to the FriendStore, if they're not null.
            if (publicIDBytes != null) {
                boolean wasAdded = fs.addFriendBytes(publicIDBytes);
                Log.i(TAG, "Now have " + fs.getAllFriends().size()
                        + " friends.");
                if (wasAdded) {
                    Toast.makeText(this, "Friend Added", Toast.LENGTH_SHORT)
                            .show();
                } else {
                    Toast.makeText(this, "Already Friends", Toast.LENGTH_SHORT)
                            .show();
                }
            } else {
                // This can happen if the URI is well-formed (rangzen://<stuff>)
                // but the
                // stuff isn't valid base64, since we get here based on the
                // scheme but
                // not a check of the contents of the URI.
                Log.i(TAG,
                        "Opener got back a supposed rangzen scheme code that didn't process to produce a public id:"
                                + code);
                Toast.makeText(this, "Invalid Friend Code", Toast.LENGTH_SHORT)
                        .show();
            }
        }
        if (requestCode == POSTED_MESSAGE ) {
            notifyDataSetChanged();
        }
    }

    /**
     * This handles the instantiation of all of the fragments when they are
     * chosen. This also transforms the title of the item in the list to bold
     * and a larger font.
     * 
     * @param position
     *            The specific position in the ListView of the current fragment
     *            chosen.
     */
    public void showFragment(int position) {
        if (position == 1) {
            Intent intent = new Intent();
            intent.setClass(this, PostActivity.class);
            startActivityForResult(intent, MESSAGE);
            return;
        } else if (position == 2) {
            Intent intent = new Intent("com.google.zxing.client.android.SCAN");
            intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
            startActivityForResult(intent, QR);
            return;
        } else if (position == 3) {
            Intent intent = new Intent();
            intent.setClass(this, InfoActivity.class);
            startActivityForResult(intent, MESSAGE); // reverts action bar back
                                                     // to feed
            return;
        } else {
            // debug screen
            Intent intent = new Intent();
            intent.setClass(this, DebugActivity.class);
            startActivityForResult(intent, MESSAGE);
            return;
        }
    }

    public SidebarListAdapter getSidebarAdapter() {
        return mSidebarAdapter;
    }

    /**
     * Overriden in order to unregister the receiver, if this is not done then
     * the app crashes.
     */
    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
        Log.i(TAG, "Unregistered receiver");
    }

    /**
     * Creates a new instance of the NewMessageReceiver and registers it every
     * time that the app is available/brought to the user.
     */
    @Override
    protected void onResume() {
        super.onResume();
        notifyDataSetChanged();
        registerReceiver(receiver, filter);
        notifyDataSetChanged();
        Log.i(TAG, "Registered receiver");
    }

    /**
     * This is the broadcast receiver object that I am registering. I created a
     * new class in order to override onReceive functionality.
     * 
     * @author jesus
     * 
     */
    public class NewMessageReceiver extends BroadcastReceiver {

        /**
         * When the receiver is activated then that means a message has been
         * added to the message store, (either by the user or by the active
         * services). The reason that the instanceof check is necessary is
         * because there are two possible routes of activity:
         * 
         * 1) The previous/current fragment viewed could have been the about
         * fragment, if it was then the focused fragment is not a
         * ListFragmentOrganizer and when the user returns to the feed then the
         * feed will check its own data set and not crash.
         * 
<<<<<<< HEAD
<<<<<<< HEAD
         * 2) The previous/current fragment is the feed, it needs to be notified
         * immediately that there was a change in the underlying dataset.
=======
         * If the message is a NEW_MESSAGE and not SAVE_MESSAGE then create a
         * notification.
>>>>>>> Added remove, but it creates an error, fixed the saved count and regular count, fixed rows keeping their button clicks.
=======
         * If the message is a NEW_MESSAGE and not SAVE_MESSAGE then create a
         * notification.
>>>>>>> refs/remotes/origin/garcia43_favorite
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() == MessageStore.NEW_MESSAGE) {
                notifyDataSetChanged();
                createNotification();
            } else {
                notifyDataSetChanged();
            }
        }
    }

    /**
     * Find the adapter and call its notifyDataSetChanged method.
     */
    private void notifyDataSetChanged() {
        Fragment feed = getSupportFragmentManager().findFragmentById(
                R.id.tabcontent);
        if (feed instanceof ListFragmentOrganizer) {
            Log.d(TAG, "inside instanceof");
            ListFragmentOrganizer org = (ListFragmentOrganizer) feed;
            FeedListAdapter adapt = (FeedListAdapter) org.getListView()
                    .getAdapter();

            adapt.refresh();
            adapt.notifyDataSetChanged();
        }
    }

    private void createNotification() {
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
                this).setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("New Message")
                .setContentText("You've received new messages.").setNumber(1);

        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();

        String[] events = new String[6];
        // Sets a title for the Inbox in expanded layout
        inboxStyle.setBigContentTitle("Event tracker details:");

        for (int i = 0; i < events.length; i++) {

            inboxStyle.addLine(events[i]);
        }
        mBuilder.setStyle(inboxStyle);
        Intent resultIntent = new Intent(this, Opener.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(Opener.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0,
                PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        mNotificationManager.notify(0, mBuilder.build());
    }

    /**
     * This is an onclick listener created in feed_row.xml. It's a button that
     * allows the user to save a message.
     * 
     * @param view
     *            - This is the button view.
     */
    public void onSave(View view) {

        ImageView iv = (ImageView) view;
        iv.setAdjustViewBounds(true);
        iv.setImageResource(R.drawable.ic_action_important_yellow);

        ViewGroup vg = (ViewGroup) view.getParent(); // rel layout
        ViewGroup vg2 = (ViewGroup) vg.getParent(); // root
        TextView hashtagView = (TextView) vg2.getChildAt(0); // id-messageAndScore

        MessageStore messageStore = new MessageStore(this,
                StorageBase.ENCRYPTION_DEFAULT);
        String text = hashtagView.getText().toString();
        double p = messageStore.getPriority((text));

        messageStore.saveMessage(text, p);

        StorageBase m = new StorageBase(this, StorageBase.ENCRYPTION_DEFAULT);
        m.putInt(SAVE + hashtagView.getText().toString(), 1);
    }

    /**
     * This is an onclick listener created in feed_row.xml. It's a button that
     * allows the user to delete a message.
     * 
     * @param view
     *            - This is the button view.
     */
    public void onDelete(View view) {
        ViewGroup vg = (ViewGroup) view.getParent(); // rel layout
        RelativeLayout vg2 = (RelativeLayout) vg.getParent(); // root
        TextView hashtagView = (TextView) vg2.getChildAt(0); // id-messageAndScore
        final String item = hashtagView.getText().toString();

        ImageView iv = (ImageView) view;
        iv.setImageResource(R.drawable.ic_action_discard_red);

        Fragment feed = getSupportFragmentManager().findFragmentById(
                R.id.tabcontent);
        if (feed instanceof ListFragmentOrganizer) {
            ListFragmentOrganizer org = (ListFragmentOrganizer) feed;
            final FeedListAdapter adapt = (FeedListAdapter) org.getListView()
                    .getAdapter();

            Animation anim = AnimationUtils.loadAnimation(this,
                    android.R.anim.slide_out_right);
            anim.setDuration(1000);

            anim.setAnimationListener(new Animation.AnimationListener() {

                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    MessageStore ms = new MessageStore(getApplicationContext(),
                            StorageBase.ENCRYPTION_DEFAULT);
                    ms.deleteMessage(item);
                    adapt.refresh();
                    adapt.notifyDataSetChanged();
                }
            });
            vg2.startAnimation(anim);
        }
    }

    /**
     * This is an onclick listener created in feed_row.xml. It's a button that
     * allows the user to retweet a message.
     * 
     * @param view
     *            - This is the button view.
     */
    public void onRetweet(View view) {
        ViewGroup vg = (ViewGroup) view.getParent(); // rel layout
        final RelativeLayout vg2 = (RelativeLayout) vg.getParent(); // root
        final TextView hashtagView = (TextView) vg2.getChildAt(0); // id-messageAndScore
        
        ImageButton iv = (ImageButton) view;
        iv.setAdjustViewBounds(true);
        iv.setImageResource(R.drawable.ic_action_repeat_green);
        StorageBase m = new StorageBase(this, StorageBase.ENCRYPTION_DEFAULT);
        m.putInt(RETWEET + hashtagView.getText().toString(), 1);
    }

}
