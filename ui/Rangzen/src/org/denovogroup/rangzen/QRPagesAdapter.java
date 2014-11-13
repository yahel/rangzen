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

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;

import com.viewpagerindicator.IconPagerAdapter;

/**
 * This adapts a list of the two QR pages into a fragment list suited for a
 * ViewPager. The order of the pages is:
 * 1) QR-Reading
 * 2) QR-Writing
 * This was done to avoid some of the ~100 frame lag of creating the QR code for the phone.
 */
class QRPagesAdapter extends FragmentPagerAdapter implements IconPagerAdapter {

    /** Number of slides in the QR Section. */
    private int mCount = 2;
    /** This is used in order to access fragments from the ViewPager activity.*/
    SparseArray<Fragment> registeredFragments = new SparseArray<Fragment>();

    public QRPagesAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public void destroyItem(View collection, int position, Object o) {
        View view = (View) o;
        ((ViewPager) collection).removeView(view);
        view = null;
    }

    /**
     * Creates a new instance of the fragment at position position.
     * 
     * @param position
     *            The position of the fragment currently being created.
     */
    @Override
    public Fragment getItem(int position) {
        Enum type = null;
        Fragment fragment = null;
        if (position == 1) {
            type = FragmentType.QRRead;
            position = position % mCount;
            Bundle b = new Bundle();
            b.putSerializable("whichScreen", type);
            fragment = new FragmentOrganizer();
            fragment.setArguments(b);
            registeredFragments.put(position, fragment);
            return fragment;
        } else {
            return new Fragment();
            //Fragment cam = new CameraFragment();
            //CaptureFragment cam = new CaptureFragment();
//            Fragment fragmenty = (Fragment) fr;
//            registeredFragments.put(position, fragmenty);
//            return fragmenty;
        }
    }

    @Override
    public int getCount() {
        return mCount;
    }

    @Override
    public int getIconResId(int index) {
        return 0;
    }

    /** This is used in order to access fragments from the ViewPager activity.*/
    public Fragment getRegisteredFragment(int position) {
        return registeredFragments.get(position);
    }

}
