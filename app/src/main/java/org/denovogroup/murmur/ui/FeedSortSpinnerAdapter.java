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
