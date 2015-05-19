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

import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import java.util.List;

import org.denovogroup.rangzen.MessageStore.Message;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;

/**
 * This class is meant to be an organizer for the list views that will be
 * present in Rangzen which are the friends list and feed.
 */
public class ListFragmentOrganizer extends ListFragment {

    /**
     * There are two list Fragments in the UI, the feed and possibly the friends
     * page.
     */
    enum FragmentType {
        FEED, SAVED
    }

    private static final String TAG = "ListFragmentOrganizer";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        Bundle b = getArguments();
        FragmentType whichScreen = (FragmentType) b
                .getSerializable("whichScreen");
        View view = (View) inflater.inflate(R.layout.feed, container, false);

        ListView listView = (ListView) view.findViewById(android.R.id.list);

        MessageStore m = new MessageStore(getActivity(),
                StorageBase.ENCRYPTION_DEFAULT);

        if (whichScreen == FragmentType.FEED) {
            List<Message> messages = m.getAllMessages(
                    MessageStore.NOT_SAVED_MESSAGES, null);
            FeedListAdapter mFeedListAdaper = new FeedListAdapter(
                    getActivity(), R.layout.feed_row, messages);
            listView.setAdapter(mFeedListAdaper);
        }
        if (whichScreen == FragmentType.SAVED) {
            List<Message> messages = m.getAllMessages(
                    MessageStore.SAVED_MESSAGES, null);
            SavedFeedListAdapter f = new SavedFeedListAdapter(getActivity(),
                    R.layout.feed_row_save, messages);
            listView.setAdapter(f);
        }

        return view;
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
