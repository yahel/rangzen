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

import org.denovogroup.rangzen.ui.FragmentOrganizer.FragmentType;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;

import com.viewpagerindicator.IconPagerAdapter;

/**
 * This creates the individual fragments in the introduction. This is
 * specifically for the ViewPager class that enables sliding of fragments. This
 * is necessary to provide a list format for the ViewPager.
 */
class IntroductionFragmentAdapter extends FragmentPagerAdapter implements
        IconPagerAdapter {
    
    /** Number of slides in the introduction. */
    private int mCount = 3;

    public IntroductionFragmentAdapter(FragmentManager fm) {
        super(fm);
    }
    
    @Override
    public void destroyItem(View collection, int position, Object o) {
        View view = (View)o;
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
        if (position == 0) {
            type = FragmentType.FIRSTINTRO;
        } else if (position == 1) {
            type = FragmentType.SECONDINTRO;
        } else {
            type = FragmentType.THIRDINTRO;
        }
        position = position % mCount;
        Bundle b = new Bundle();
        b.putSerializable("whichScreen", type);
        Fragment fragment = new FragmentOrganizer();
        fragment.setArguments(b);
        return fragment;
    }

    @Override
    public int getCount() {
        return mCount;
    }

    @Override
    public int getIconResId(int index) {
        return 0;
    }

}
