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

import java.util.Stack;

import org.denovogroup.rangzen.FragmentOrganizer.FragmentType;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NotificationCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageView;
import android.support.v4.app.FragmentTabHost;

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

    private FragmentTabHost mTabHost;

    // Create reciever object
    private BroadcastReceiver receiver = new NewMessageReceiver();

    // Set When broadcast event will fire.
    private IntentFilter filter = new IntentFilter(MessageStore.NEW_MESSAGE);

    private final static int QR = 10;
    private final static int MESSAGE = 20;

    private final static int UPVOTE = 1;
    private final static int DOWNVOTE = 0;

    /** Initialize the contents of the activities menu. */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);

        MessageStore messageStore = new MessageStore(this,
                StorageBase.ENCRYPTION_DEFAULT);

        messageStore
                .addMessage(
                        "This is the Rangzen message feed. Messages in the ether will appear here.",
                        1L);

        messageStore.saveMessage("hello.", 1L);

        messageStore
                .addMessage(
                        "This is the Rangzen message feed. Messages in the ether will appear here2.",
                        0L);

        messageStore
                .addMessage(
                        "This is the Rangzen message feed. Messages in the ether will appear here3.",
                        1L);

        messageStore
                .addMessage(
                        "This is the Rangzen message feed. Messages in the ether will appear here4.",
                        0L);
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

    private void setUpTabHost() {

        mTabHost = (FragmentTabHost) findViewById(android.R.id.tabhost);
        mTabHost.setup(this, getSupportFragmentManager(), R.id.tabcontent);

        Bundle b = new Bundle();
        b.putSerializable("whichScreen",
                ListFragmentOrganizer.FragmentType.FEED);
        Bundle b2 = new Bundle();
        b2.putSerializable("whichScreen",
                ListFragmentOrganizer.FragmentType.NEW);
        Bundle b3 = new Bundle();
        b3.putSerializable("whichScreen",
                ListFragmentOrganizer.FragmentType.SAVED);

        mTabHost.addTab(mTabHost.newTabSpec("Trust").setIndicator("Trust"),
                ListFragmentOrganizer.class, b);
        mTabHost.addTab(mTabHost.newTabSpec("New").setIndicator("New"),
                ListFragmentOrganizer.class, b2);
        mTabHost.addTab(mTabHost.newTabSpec("Saved").setIndicator("Saved"),
                ListFragmentOrganizer.class, b3);
    }

    /**
     * This allows the icon that shows a DrawerLayout is present to retract when
     * the layout disappears.
     */
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerListener.syncState();
        switchToFeed();
    }

    /**
     * Switches current open fragment to feed fragment. Call this method after
     * closing an activity.
     */
    public void switchToFeed() {
        Log.d("Opener", "Switching to feed fragment.");
        Fragment needAdd = new ListFragmentOrganizer();
        Bundle b = new Bundle();
        b.putSerializable("whichScreen",
                ListFragmentOrganizer.FragmentType.FEED);
        needAdd.setArguments(b);
        FragmentManager fragmentManager = getSupportFragmentManager();

        FragmentTransaction ft = fragmentManager.beginTransaction();

        ft.replace(R.id.mainContent, needAdd);

        ft.commitAllowingStateLoss();
        mFirstTime = false;
        selectItem(0);
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
            Fragment feed = getSupportFragmentManager().findFragmentById(
                    R.id.mainContent);
            if (feed instanceof ListFragmentOrganizer) {
                ListFragmentOrganizer org = (ListFragmentOrganizer) feed;
                FeedListAdapter adapt = (FeedListAdapter) org.getListView()
                        .getAdapter();
                adapt.notifyDataSetChanged();
            }
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
        Fragment needAdd = null;
        if (position == 0) {
            needAdd = new ListFragmentOrganizer();
            Bundle b = new Bundle();
            b.putSerializable("whichScreen",
                    ListFragmentOrganizer.FragmentType.FEED);
            needAdd.setArguments(b);

        } else if (position == 1) {
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
            startActivityForResult(intent, MESSAGE); //reverts action bar back to feed
            return;
        } else {
            //debug screen
            Intent intent = new Intent();
            intent.setClass(this, DebugActivity.class);
            startActivityForResult(intent, MESSAGE);
            return;
        }
        makeTitleBold(position);
        FragmentManager fragmentManager = getSupportFragmentManager();

        FragmentTransaction ft = fragmentManager.beginTransaction();

        ft.replace(R.id.mainContent, needAdd);

        if (!mFirstTime) {
            Log.d("Opener", "added to backstack");
            ft.addToBackStack(null);
        }
        mFirstTime = false;
        ft.commit();
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
        registerReceiver(receiver, filter);
        Log.i(TAG, "Registered receiver");
    }

    /**
     * A custom broadcast receiver object that receives a signal whenever a new
     * message is added to the phone. When the new message is received then
     * 
     */
    public class NewMessageReceiver extends BroadcastReceiver {

        /**
         * When the receiver is activated then that means a message has been
         * added to the message store, (either by the user or by the active
         * services).
         * 
         * If the message is a NEW_MESSAGE and not SAVE_MESSAGE then create a notification.
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
     * Find the adapter and call its notifyDataSetChanged method if a
     * FeedListOrganizer is being shown. The reason that the instanceof check is
     * necessary is because there are two possible routes of activity:
     * 
     * 1) The previous/current fragment viewed could have been the about
     * fragment, if it was then the focused fragment is not a
     * ListFragmentOrganizer and when the user returns to the feed then the feed
     * will check its own data set and not crash.
     * 
     * 2) The previous/current fragment is the feed, it needs to be notified
     * immediately that there was a change in the underlying dataset.
     */
    private void notifyDataSetChanged() {
        Fragment feed = getSupportFragmentManager().findFragmentById(
                R.id.mainContent);
        if (feed instanceof ListFragmentOrganizer) {
            ListFragmentOrganizer org = (ListFragmentOrganizer) feed;
            FeedListAdapter adapt = (FeedListAdapter) org.getListView()
                    .getAdapter();
            adapt.notifyDataSetChanged();
        }
    }
    
    private void createNotification() {
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
                this).setSmallIcon(R.mipmap.ic_launcher)
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
     * This is an onclick listener created in feed_row.xml or feed_row_save.xml.
     * It's a button that allows the user to up vote a message.
     * 
     * @param view
     *            - This is the button image view for the upvote.
     */
    public void upVote(View view) {
        handleVoting(UPVOTE, view);
    }

    /**
     * This is an onclick listener created in feed_row.xml or feed_row_save.xml.
     * It's a button that allows the user to down vote a message.
     * 
     * @param view
     *            - This is the button image view for the downvote.
     */
    public void downVote(View view) {
        handleVoting(DOWNVOTE, view);
    }

    /**
     * Finds the textviews containing the up/down vote score and the textview
     * for the message. It places a cap and a floor on the score that a message
     * can be and increments the score by some number.
     * 
     * @param i
     *            - Constant indicating up vote or down vote.
     * @param view
     *            - The up/down vote image button view.
     */
    private void handleVoting(int i, View view) {
        /**
         * The view that we have is to a specific instance of a feed element. We
         * cannot call findViewById here because it would give a reference to
         * only the first instance of a feed element. To get around this I sift
         * through the layers of xml.
         */
        ViewGroup vg = (ViewGroup) view.getParent();
        ViewGroup vg2 = (ViewGroup) vg.getParent();
        ViewGroup vg3 = (ViewGroup) vg2.getParent();
        LinearLayout l = (LinearLayout) vg3.getChildAt(2);

        TextView upvoteView = (TextView) vg2.getChildAt(1);
        TextView hashtagView = (TextView) l.getChildAt(0);

        ImageView iv = (ImageView) view;
        MessageStore messageStore = new MessageStore(this,
                StorageBase.ENCRYPTION_DEFAULT);

        if (i == 1) {
            Toast.makeText(this, "upVote", Toast.LENGTH_SHORT).show();
            iv.setImageResource(R.drawable.uparrowgreen);
            LinearLayout l2 = (LinearLayout) vg2.getChildAt(2);
            ImageButton ib = (ImageButton) l2.getChildAt(0);
            ib.setImageResource(R.drawable.downarrow);
        } else {
            Toast.makeText(this, "downVote", Toast.LENGTH_SHORT).show();
            iv.setImageResource(R.drawable.downarrowred);
            LinearLayout l2 = (LinearLayout) vg2.getChildAt(0);
            ImageButton ib = (ImageButton) l2.getChildAt(0);
            ib.setImageResource(R.drawable.uparrow);
        }

        String text = hashtagView.getText().toString();

        if (100 * messageStore.getPriority((text)) < 86 && i == 1) { // max
            messageStore.updatePriority(text,
                    messageStore.getPriority((text)) + .15);
        } else if (100 * messageStore.getPriority((text)) > 15 && i == 0) { // min
            messageStore.updatePriority(text,
                    messageStore.getPriority((text)) - .15);
        } else {
            return;
        }
        upvoteView.setText(Integer.toString((int) (100 * messageStore
                .getPriority(text))));
    }

    /**
     * This is an onclick listener created in feed_row.xml. It's a button that
     * allows the user to save a message.
     * 
     * @param view
     *            - This is the button view.
     */
    public void onSave(View view) {
        ViewGroup vg = (ViewGroup) view.getParent(); // root
        LinearLayout l = (LinearLayout) vg.getChildAt(0); // id-messageAndScore
        LinearLayout l2 = (LinearLayout) l.getChildAt(2); // id-hashtagHolder

        TextView hashtagView = (TextView) l2.getChildAt(0);
        MessageStore messageStore = new MessageStore(this,
                StorageBase.ENCRYPTION_DEFAULT);
        String text = hashtagView.getText().toString();
        double p = messageStore.getPriority((text));

        messageStore.saveMessage(text, p);
    }

}
