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

import java.util.Collection;
import java.util.TreeMap;
import java.util.Map.Entry;
import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class FeedListAdapter extends BaseAdapter {

    /** Activity context passed in to the FeedListAdapter. */
    private Context mContext;
    /** Array of test messages created inside of Strings.xml. */
    private String[] mMessages;
    /** Array of test Trust Scores created inside of Strings.xml. */
    private String[] mUpvoteValues;
    /** Array of test dates of message creation created inside of Strings.xml. */
    private String[] dates;
    /** Message store to be used to get the messages and trust score. */
    private MessageStore mMessageStore;
    /**
     * Holds references to views so that findViewById() is not needed to be
     * called so many times.
     */
    private ViewHolder mViewHolder;

    /** Maximum number of characters that can be sent in a message. */
    private int mMaxCharacters = 140;

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

        // (TODO (Jesus) This is not the correct way to get the actual values.
        mMessages = context.getResources().getStringArray(R.array.messages);
        mUpvoteValues = context.getResources().getStringArray(
                R.array.upvoteValues);
        dates = context.getResources().getStringArray(R.array.dates);
        MessageStore messageStore = new MessageStore((Activity) context,
                StorageBase.ENCRYPTION_DEFAULT);
        // comments = context.getResources().getStringArray(R.array.comments);
    }

    @Override
    public int getCount() {
        return mMessages.length;
    }

    /**
     * Returns the name of the item in the ListView of the NavigationDrawer at
     * this position.
     */
    @Override
    public Object getItem(int position) {
        return mMessages[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    /**
     * This populates the listView at specified positions with the text that
     * belongs in them and it also populates the images of that row.
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        mMessageStore = new MessageStore((Activity) mContext,
                StorageBase.ENCRYPTION_DEFAULT);
        TreeMap<Float, Collection<String>> tree = mMessageStore.getTopK(position + 1);
        tree.size();
        int i = 0;
        String realMessage = "No message";
        float trustScore = 3;
        Log.d("MapsActivity", "VALUE of size of tree = " + tree.size());
        Log.d("MapsActivity", "VALUE of POSITION = " + position);
//        for (Entry<Float, String> e : tree.entrySet()) {
//            Log.d("MapsActivity", "VALUE of ENTRY VALUE = " + e.getValue());
//            Log.d("MapsActivity", "VALUE of ENTRY KEY = " + e.getKey());
//            Log.d("MapsActivity", "VALUE of I = " + i);
//            // if (i == position) {
//            // Log.d("MapsActivity", "value of position = " + position);
//            // Log.d("MapsActivity", "value of i = " + i);
//            // Log.d("MapsActivity", "value of realMessage = " + e.getValue());
//            // Log.d("MapsActivity", "value of trustScore = " + e.getKey());
//            // realMessage = e.getValue();
//            // trustScore = e.getKey();
//            // break;
//            // }
//            i++;
//        }
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) mContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.feed_row, parent, false);

            mViewHolder = new ViewHolder();
            mViewHolder.dateView = (TextView) convertView
                    .findViewById(R.id.dateView);
            mViewHolder.upvoteView = (TextView) convertView
                    .findViewById(R.id.upvoteView);
            mViewHolder.hashtagView = (TextView) convertView
                    .findViewById(R.id.hashtagView);

            convertView.setTag(mViewHolder);
        } else {
            mViewHolder = (ViewHolder) convertView.getTag();
        }

        // TODO (jesus) this is not the correct way to implement, may need a
        // null check in the future.
        // viewHolder.commentView.setText(comments[position]);
        mViewHolder.hashtagView.setText(realMessage);
        mViewHolder.upvoteView.setText(String.valueOf(trustScore));
        return convertView;
    }

    static class ViewHolder {
        /** The view object that holds the hashtag for this current row item. */
        private TextView hashtagView;
        /**
         * The view object that holds the date of creation for this current row
         * item.
         */
        private TextView dateView;
        /**
         * The view object that holds the trust score for this current row item.
         */
        private TextView upvoteView;

    }
}
