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

package org.denovogroup.rangzen.ui;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;

import org.denovogroup.rangzen.R;

/**
 * This class is meant to be an organizer for the list views that will be
 * present in Rangzen which are the friends list and feed.
 */
public class ListFragmentOrganizer extends ListFragment {

    /**
     * There are two list Fragments in the ui, the feed and possibly the friends
     * page.
     */
    enum FragmentType {
        FEED, FRIENDS
    }

    /** Creates and populates the content for the feed fragment. */
    private FeedListAdapter mFeedListAdaper;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        Log.d("Opener", "feed's on create was called");
        Bundle b = getArguments();
        FragmentType whichScreen = (FragmentType) b
                .getSerializable("whichScreen");
        switch (whichScreen) {
        case FEED:
            View view = (View) inflater
                    .inflate(R.layout.feed, container, false);

            ImageView iv = (ImageView) view.findViewById(R.id.normal_image);

            Display display = getActivity().getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            int width = size.x;
            int height = (int) (size.y);

            Bitmap bd = FragmentOrganizer.decodeSampledBitmapFromResource(getResources(), R.drawable.firstb,
                    width, height);
            BitmapDrawable ob = new BitmapDrawable(bd);
            iv.setBackgroundDrawable(ob);
           

            ListView listView = (ListView) view.findViewById(android.R.id.list);
            mFeedListAdaper = new FeedListAdapter(getActivity());
            listView.setAdapter(mFeedListAdaper);
            return view;
        case FRIENDS:
            View view2 = inflater.inflate(R.layout.feed_row, container, false);
            return view2;
        default:
            return null;
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        // TODO (Jesus) A lot of getting the correct hashtags... this may be
        // intensive? Idk how else to do it.
        super.onListItemClick(l, v, position, id);
    }

    @Override
    public void onDestroyView() {
        ListFragmentOrganizer f = (ListFragmentOrganizer) getFragmentManager()
                .findFragmentById(R.layout.feed);
        if (f != null) {
            try {
                getFragmentManager().beginTransaction().remove(f).commit();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        super.onDestroyView();
    }
}
