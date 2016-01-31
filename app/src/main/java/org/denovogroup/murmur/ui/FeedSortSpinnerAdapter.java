/*
* Copyright (c) 2016, De Novo Group
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
* AND ANY EXPRES S OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
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
package org.denovogroup.murmur.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.denovogroup.murmur.R;

import java.util.List;

/**
 * Created by Liran on 12/27/2015.
 *
 * An array adapter used for the feed sort options spinner, accepting any array of objects conditioned
 * that two first fields are resource id of the icon to be used and the resource id of the string to display
 */
public class FeedSortSpinnerAdapter extends ArrayAdapter<Object[]> {

    private boolean useDarkAsset = false;

    private static final int RESOURCE = R.layout.feed_sort_spinner_item_idle;
    private static final int RESOURCE_DARK = R.layout.feed_sort_spinner_item_idle_dark;
    private static final int RESOURCE_DROPDOWN = R.layout.feed_sort_spinner_item;
    private static final int TEXTVIEW_RESOURCE = R.id.item_text;

    public FeedSortSpinnerAdapter(Context context, List<Object[]> data) {
        super(context, RESOURCE, TEXTVIEW_RESOURCE, data);
    }

    public FeedSortSpinnerAdapter(Context context, List<Object[]> data, boolean useDarkAsset) {
        super(context, RESOURCE, TEXTVIEW_RESOURCE, data);
        this.useDarkAsset = useDarkAsset;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {

        if(convertView == null) convertView = LayoutInflater.from(getContext()).inflate(RESOURCE_DROPDOWN, parent, false);

        Object[] currentItem = getItem(position);

        ((ImageView) convertView.findViewById(R.id.item_icon)).setImageResource((int)currentItem[0]);
        ((TextView) convertView.findViewById(R.id.item_text)).setText(getContext().getString((int)currentItem[1]));

        return convertView;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(convertView == null) convertView = LayoutInflater.from(getContext()).inflate(useDarkAsset ? RESOURCE_DARK : RESOURCE, parent, false);

        Object[] currentItem = getItem(position);

        ((TextView) convertView.findViewById(R.id.item_text)).setText(getContext().getString((int)currentItem[1]));

        return convertView;
    }
}
