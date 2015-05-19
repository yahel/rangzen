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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.denovogroup.rangzen.MessageStore.Message;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.database.DataSetObserver;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TextView.BufferType;

public class FeedListAdapter extends ArrayAdapter<Message> {

    HashMap<String, Integer> mIdMap = new HashMap<String, Integer>();

    protected final static String TAG = "FeedListAdapter";

    protected int[] a = { R.drawable.ic_action_important_yellow,
            R.drawable.ic_action_repeat_green, R.drawable.ic_action_discard_red };

    protected int[] b = { R.drawable.ic_action_important,
            R.drawable.ic_action_repeat, R.drawable.ic_action_discard };

    /**
     * Holds references to views so that findViewById() is not needed to be
     * called so many times.
     */
    protected ViewHolder mVH;
    

    /**
     * Sets the feed text fields to be their values from messages from memory.
     * This finds the correct message at what position and populates recycled
     * views.
     * 
     * @param context
     *            The context of the activity that spawned this class.
     */
    public FeedListAdapter(Context context, int resource, List<Message> items) {
        super(context, resource, items);
    }

    public FeedListAdapter(Context context, int resource) {
        super(context, resource);
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

        StorageBase s = new StorageBase(getContext(),
                StorageBase.ENCRYPTION_DEFAULT);
        View v = convertView;

        if (v == null) {
            LayoutInflater inflater = (LayoutInflater) getContext()
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = inflater.inflate(R.layout.feed_row, parent, false);

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
        ImageButton[] ib = { mVH.mFavorite, mVH.mRetweet, mVH.mTrash };
        String[] saveRetweet = { Opener.SAVE, Opener.RETWEET, "" };

        for (int i = 0; i < ib.length; i++) {
            if (s.getInt(saveRetweet[i] + m.getMessage(), 0) == 0) {
                ib[i].setImageResource(b[i]);
            } else {
                ib[i].setImageResource(a[i]);
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
            TextView t = (TextView) tv;
            Log.d(TAG, t.getText().toString());
            Spanned s = (Spanned) t.getText();
            int start = s.getSpanStart(this);
            int end = s.getSpanEnd(this);
            Log.d(TAG, "onClick [" + s.subSequence(start, end) + "]");
            Intent intent = new Intent();
            intent.setClass(getContext(), SearchableActivity.class);
            intent.setAction(Intent.ACTION_SEARCH);
            intent.putExtra(SearchManager.QUERY, s.subSequence(start, end).toString());
            getContext().startActivity(intent);
        }

        @Override
        public void updateDrawState(TextPaint ds) {
            super.updateDrawState(ds);
            ds.setUnderlineText(false);
        }
    }

    String getType() {
        return TAG;
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
    
    public void refresh() {
        MessageStore m = new MessageStore(getContext(),
                StorageBase.ENCRYPTION_DEFAULT);
        List<Message> messages = m.getAllMessages(
                MessageStore.NOT_SAVED_MESSAGES, null);
        Log.d(TAG, "get count = " + Integer.toString(getCount()));
        clear();
        if (messages.size() > 0) {
            Log.d(TAG, "messages size = " + Integer.toString(messages.size()));
            addAll(messages);
        }
    }

    /**
     * This is used to recycle the views and increase speed of scrolling. This
     * is held by the row object that keeps references to the views so that they
     * do not have to be looked up every time they are populated or reshown.
     */
    protected static class ViewHolder {
        /** The view object that holds the hashtag for this current row item. */
        protected TextView mHashtagView;
        /**
         * The view object that holds the trust score for this current row item.
         */
        protected TextView mUpvoteView;

        protected ImageButton mFavorite;
        protected ImageButton mTrash;
        protected ImageButton mRetweet;
    }

}
