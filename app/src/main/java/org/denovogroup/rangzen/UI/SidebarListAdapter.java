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

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.denovogroup.rangzen.R;

public class SidebarListAdapter extends BaseAdapter {

    /** Activity context passed into the ListAdapter to get resources. */
    private Context mContext;
    /** Array of string items in the sidebar. */
    private String[] mSidebarItems;

    /**
     * I currently have not found the icons for this, but this is a list of
     * icons to be used in the NavigationDrawer.
     * 
     */
    private int[] mSidebarImages = { R.drawable.ic_action_view_as_list,
            R.drawable.ic_action_new_event_light,
            R.drawable.ic_action_add_person, R.drawable.ic_action_about };

    /**
     * sets sidebar to the list of the names of the ListView that is in the
     * NavigationDrawer.
     * 
     * @param context
     *            The context of the activity that spawned this class.
     */
    public SidebarListAdapter(Context context) {
        this.mContext = context;
        mSidebarItems = context.getResources().getStringArray(R.array.sidebar);
    }

    @Override
    public int getCount() {
        return mSidebarItems.length;
    }

    /**
     * Returns the name of the item in the ListView of the NavigationDrawer at
     * this position.
     */
    @Override
    public Object getItem(int position) {
        return mSidebarItems[position];
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
        View row = null;
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) mContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            row = inflater.inflate(R.layout.custom_row, parent, false);
        } else {
            row = convertView;
        }
        TextView rowText = (TextView) row.findViewById(R.id.textView1);
        rowText.setTextSize(17);
        // L T ? ? setPadding(Left, Top, ?, ?) This is a personal reminder that
        // the fields in set padding are Left, Top, then either right or bottom.
        rowText.setPadding(50, 26, 0, 0);
        rowText.setTextColor(row.getResources().getColor(R.color.whitish));
        ImageView rowImage = (ImageView) row.findViewById(R.id.imageView1);
        rowText.setText(mSidebarItems[position]);
        rowImage.setImageResource(mSidebarImages[position]);
        return row;
    }
}
