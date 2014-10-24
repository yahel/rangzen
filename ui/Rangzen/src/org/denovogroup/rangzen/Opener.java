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
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
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
    private static boolean mHasStored = false;
    private static int mPosition = 0;
    private static boolean mFirstTime = true;
    private static Stack<Integer> mPrevPosition = new Stack<Integer>();
    private static final String TAG = "Opener";

    /** Initialize the contents of the activities menu. */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
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
        if (!mHasStored) {
            storeTempMessages();
        }
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
    }

    private void storeTempMessages() {
        MessageStore messageStore = new MessageStore(this,
                StorageBase.ENCRYPTION_DEFAULT);

        messageStore
                .addMessage(
                        "Crowds Swell, Tensions Surge As Hong Kong Leader Seeks End To Protests | ",
                        1);
        messageStore
                .addMessage(
                        "150 Protesters Disrupt Coal And Rail Operations In Southeast Australia ",
                        1.5f);
        messageStore
                .addMessage(
                        "Police Violence In Naples When Protests Erupt Over European Bank Policies ",
                        2);

        messageStore
                .addMessage(
                        "Mothers Challenge Social Cleansing As London's Housing Conflicts Sharpen ",
                        .5f);
        messageStore
                .addMessage(
                        "Europe Versus Facebook: Privacy Activists Sue Social Media Giant Over Data Breach #occupy ",
                        .5f);
        messageStore
                .addMessage(
                        "#occupy @occupy happening right now by sproul on UCB campus berkeley ",
                        .5f);

        Log.d(TAG, "seventh " + messageStore.addMessage("Test7s", .5f));
        Log.d(TAG, "eighth " + messageStore.addMessage("Test8", .5f));
        Log.d(TAG, "9 " + messageStore.addMessage("test9", .5f));

        messageStore
                .addMessage(
                        "Pentagon Supplies School Districts with Assault Rifles, Grenade Launchers, M.R.A.P.'s",
                        2);
        Log.d(TAG, "11 " + messageStore.addMessage("Test11", .5f));
        Log.d(TAG, "12 " + messageStore.addMessage("Test12", .5f));
        mHasStored = true;
    }

    /**
     * This allows the icon that shows a DrawerLayout is present to retract when
     * the layout disappears.
     */
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerListener.syncState();
        if (mFirstTime) {
            Log.d("Opener", "first time post create");
            Fragment needAdd = new ListFragmentOrganizer();
            Bundle b = new Bundle();
            b.putSerializable("whichScreen",
                    ListFragmentOrganizer.FragmentType.FEED);
            needAdd.setArguments(b);
            FragmentManager fragmentManager = getSupportFragmentManager();

            FragmentTransaction ft = fragmentManager.beginTransaction();

            ft.replace(R.id.mainContent, needAdd);

            ft.commit();
            mFirstTime = false;
            selectItem(0);
            mPosition = 0;
        }
    }

    @Override
    public void onBackPressed() {
        // if (!mPrevPosition.empty()) {
        // mPosition = mPrevPosition.pop();
        // setTitle(mPosition);
        // } else {
        // mFirstTime = true;
        // }
        super.onBackPressed();
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
        String[] sidebar = getResources().getStringArray(R.array.sidebar);
        setTitle(sidebar[position]);
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

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        setTitle("Feed");
        int titleId = getResources().getIdentifier("action_bar_title",
                "id", "android");
        TextView abTitle = (TextView) findViewById(titleId);
        abTitle.setTextColor(Color.WHITE);
        mPosition = 0;
        makeTitleBold(0);
        switch (requestCode) {
        case 1:
            Log.d(TAG, "Post activity closed");
            if (resultCode == 1) {
                Log.d(TAG, "result was a 1");
                mListView = (ListView) findViewById(R.id.drawerList);
                mSidebarAdapter.notifyDataSetChanged();
                ListView listView = (ListView) findViewById(android.R.id.list);
                FeedListAdapter adapt = (FeedListAdapter) listView.getAdapter();
                adapt.notifyDataSetChanged();                
            }
            break;
        case 2:
            //will need to handle the input of a qr code of the Rangzen format
            break;
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
        if (mPosition == position) {
            return;
        }
        // To not mess up the stack with other activities vs fragments
        if (position != 1 || position != 2) {
            // mPrevPosition.add(mPosition);
            mPosition = position;
        }
        if (position == 0) {
            needAdd = new ListFragmentOrganizer();
            Bundle b = new Bundle();
            b.putSerializable("whichScreen",
                    ListFragmentOrganizer.FragmentType.FEED);
            needAdd.setArguments(b);

        } else if (position == 1) {
            Intent intent = new Intent();
            intent.setClass(this, PostActivity.class);
            startActivityForResult(intent, 1);
            return;
        } else if (position == 2) {
            Intent intent = new Intent();
            intent.setClass(this, QRCodeViewPager.class);
            startActivityForResult(intent, 2);
            return;
            // } else if (position == 3) {
            // needAdd = new MapsActivity();
            // return;
        } else {
            needAdd = new FragmentOrganizer();
            Bundle b = new Bundle();
            b.putSerializable("whichScreen", FragmentType.SECONDABOUT);
            needAdd.setArguments(b);
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
}
