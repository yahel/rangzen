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

import org.denovogroup.rangzen.MessageStore.Message;

import android.app.Activity;
import android.content.Context;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.TextView.BufferType;

public class FeedListAdapter extends BaseAdapter {

    /** Activity context passed in to the FeedListAdapter. */
    private Context mContext;
    /** Message store to be used to get the messages and trust score. */
    private MessageStore mMessageStore;

    protected final static String TAG = "FeedListAdapter";

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
        Message message = messageStore.getKthMessage(position, 0);
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

        mViewHolder.mHashtagView.setMovementMethod(LinkMovementMethod
                .getInstance());
        mViewHolder.mHashtagView.setText(applySpan(message.getMessage()),
                BufferType.SPANNABLE);

        mViewHolder.mUpvoteView.setText(Integer.toString((int) (100 * message
                .getPriority())));

        return convertView;
    }

    /**
     * Creates a span object for use in a spannable string in the list view for
     * the feed. It removes the underline usually in a span and has a custom
     * onClickListener.
     * 
     * @author jesus
     * 
     */
    class InnerSpan extends ClickableSpan {

        public void onClick(View tv) {
            Log.d(TAG, "spannable click");
        }

        @Override
        public void updateDrawState(TextPaint ds) {
            super.updateDrawState(ds);
            ds.setUnderlineText(false);
        }
    }

    /**
     * Creates a SpannableString object for the TextView from the feed. It
     * applies multiple spans (for the possibly multiple) hashtags in a feed
     * message.
     * 
     * @param feedText
     *            - Text in a feed ListView item.
     * @return String to be placed in ListView TextView.
     */
    protected SpannableString applySpan(String feedText) {
        SpannableString spannable = new SpannableString(feedText);
        final String spannableString = spannable.toString();
        int start = 0;
        int end = 0;
        while (true) {
            ClickableSpan spany = new InnerSpan();
            start = spannableString.indexOf("#", end);

            if (start < 0) {
                break;
            }

            end = spannableString.indexOf(" ", start);
            if (end < 0) {
                end = spannableString.length();
            }
            spannable.setSpan(spany, start, end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return spannable;
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
