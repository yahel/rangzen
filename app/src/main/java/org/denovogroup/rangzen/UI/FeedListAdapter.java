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

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import org.denovogroup.rangzen.R;
import org.denovogroup.rangzen.backend.MessageStore;
import org.denovogroup.rangzen.backend.StorageBase;

public class FeedListAdapter extends BaseAdapter {

    /** Activity context passed in to the FeedListAdapter. */
    private Context mContext;
    /** Message store to be used to get the messages and trust score. */
    private MessageStore mMessageStore;

    /**
     * Holds references to views so that findViewById() is not needed to be
     * called so many times.
     */
    private ViewHolder mViewHolder;

    /**
     * Sets the feed text fields to be their values from messages from memory.
     * This finds the correct message at what position and populates recycled
     * views.
     * 
     * @param context
     *            The context of the activity that spawned this class.
     */
    public FeedListAdapter(Context context) {
        this.mContext = context;
    }

    @Override
    public int getCount() {
        mMessageStore = new MessageStore((Activity) mContext,
                StorageBase.ENCRYPTION_DEFAULT);
        return mMessageStore.getMessageCount();
    }

    /**
     * Returns the name of the item in the ListView of the NavigationDrawer at
     * this position.
     */
    @Override
    public Object getItem(int position) {
        return "No Name";
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    /**
     * Navigates the treemap and finds the correct message from memory to
     * display at this position in the feed, then returns the row's view object,
     * fully populated with information.
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
        MessageStore messageStore = new MessageStore((Activity) mContext,
                StorageBase.ENCRYPTION_DEFAULT);
        MessageStore.Message message = messageStore.getKthMessage(position);
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) mContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.feed_row, parent, false);

            mViewHolder = new ViewHolder();
            mViewHolder.mUpvoteView = (TextView) convertView
                    .findViewById(R.id.upvoteView);
            mViewHolder.mHashtagView = (TextView) convertView
                    .findViewById(R.id.hashtagView);

            convertView.setTag(mViewHolder);
        } else {
            mViewHolder = (ViewHolder) convertView.getTag();
        }
        mViewHolder.mHashtagView.setText(message.getMessage());
        mViewHolder.mUpvoteView.setText(Double.toString(message.getPriority()));

        return convertView;
    }

    /**
     * This is used to recycle the views and increase speed of scrolling. This
     * is held by the row object that keeps references to the views so that they
     * do not have to be looked up every time they are populated or reshown.
     */
    static class ViewHolder {
        /** The view object that holds the hashtag for this current row item. */
        private TextView mHashtagView;
        /**
         * The view object that holds the trust score for this current row item.
         */
        private TextView mUpvoteView;

    }
}
