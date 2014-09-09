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

import org.denovogroup.rangzen.FragmentOrganizer.FragmentType;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView.FindListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
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
        Log.d("Opener", "first " + messageStore.addMessage("Test1", 1));
        Log.d("Opener", "second " + messageStore.addMessage("Test2", 1.5f));
        Log.d("Opener", "third " + messageStore.addMessage("Test3", 2));

        Log.d("Opener", "fourth " + messageStore.addMessage("Test4", .5f));
        Log.d("Opener", "fifth " + messageStore.addMessage("Test5", .5f));
        Log.d("Opener", "sixth " + messageStore.addMessage("Test6", .5f));

        Log.d("Opener", "seventh " + messageStore.addMessage("Test7s", .5f));
        Log.d("Opener", "eighth " + messageStore.addMessage("Test8", .5f));
        Log.d("Opener", "9 " + messageStore.addMessage("test9", .5f));

        Log.d("Opener", "10 " + messageStore.addMessage("Test10", 2));
        Log.d("Opener", "11 " + messageStore.addMessage("Test11", .5f));
        Log.d("Opener", "12 " + messageStore.addMessage("Test12", .5f));
        Log.d("Opener", "tree size = " + messageStore.getTopK(12).size());
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
        if (mCurrentTextView == null) {
            Fragment needAdd = new ListFragmentOrganizer();
            Bundle b = new Bundle();
            b.putSerializable("whichScreen",
                    ListFragmentOrganizer.FragmentType.FEED);
            needAdd.setArguments(b);
            FragmentManager fragmentManager = getSupportFragmentManager();

            FragmentTransaction ft = fragmentManager.beginTransaction();

            ft.replace(R.id.mainContent, needAdd);

            ft.commit();
            selectItem(0);
        }
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
            startActivity(intent);
            return;
        } else if (position == 2) {
            // TODO (Jesus) For the prototype need to create an add friend page
            // add friend. This is probably no longer necessary at all.
        } else if (position == 3) {
            // TODO (Jesus) MAP
            // the user can view friends.
            needAdd = new MapsActivity();
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
        
        ft.addToBackStack(null);

        ft.commit();
    }

    
}
