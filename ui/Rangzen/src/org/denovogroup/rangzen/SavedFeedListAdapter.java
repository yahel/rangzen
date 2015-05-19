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

import java.util.List;

import org.denovogroup.rangzen.FeedListAdapter.ViewHolder;
import org.denovogroup.rangzen.MessageStore.Message;

import android.app.Activity;
import android.content.Context;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.TextView.BufferType;

/**
 * This class extends FeedListAdapter and has all of the same functionality as
 * that class except in two places. Getting the count of the messages to show
 * now returns the number of saved messages and the messages that are going to
 * be shown are only saved messages.
 * 
 * @author jesus
 * 
 */
public class SavedFeedListAdapter extends FeedListAdapter {

    public SavedFeedListAdapter(Context context, int resource,
            List<Message> items) {
        super(context, resource, items);
    }
    
    protected final static String TAGG = "SavedFeedListAdapter";

    /**
     * Navigates the treemap and finds the correct message from memory to
     * display at this position in the feed, then returns the row's view object,
     * fully populated with information. This only shows saved messages.
     * 
     * @param position
     *            The current row index in the feed.
     * @param convertView
     *            The view object that contains the row, or null is one has not
     *            been initialized.
     * @param parent
     *            The parent of convertView.
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        
        StorageBase s = new StorageBase(getContext(),
                StorageBase.ENCRYPTION_DEFAULT);
        View v = convertView;

        if (v == null) {
            LayoutInflater inflater = (LayoutInflater) getContext()
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = inflater.inflate(R.layout.feed_row_save, parent, false);

            mVH = new ViewHolder();
            mVH.mUpvoteView = (TextView) v.findViewById(R.id.upvoteView);
            mVH.mHashtagView = (TextView) v.findViewById(R.id.hashtagView);

            mVH.mFavorite = (ImageButton) v.findViewById(R.id.saveButton);
            mVH.mTrash = (ImageButton) v.findViewById(R.id.eraseButton);
            mVH.mRetweet = (ImageButton) v.findViewById(R.id.retweetButton);

            v.setTag(mVH);
        } else {
            mVH = (ViewHolder) v.getTag();
        }

        Message m = getItem(position);
        ImageButton[] ib = {mVH.mRetweet,mVH.mTrash };
        String[] saveRetweet = { Opener.RETWEET, "" };
        for (int i = 0; i < ib.length; i++) {
            if (s.getInt(saveRetweet[i] + m.getMessage(), 0) == 0) {
                ib[i].setImageResource(b[i + 1]);
            } else {
                ib[i].setImageResource(a[i + 1]);
            }
        }

        mVH.mHashtagView.setMovementMethod(LinkMovementMethod.getInstance());
        mVH.mHashtagView.setText(applySpan(m.getMessage()),
                BufferType.SPANNABLE);
        mVH.mUpvoteView
                .setText(Integer.toString((int) (100 * m.getPriority())));

        v.setId(position);
        return v;
    }

}
