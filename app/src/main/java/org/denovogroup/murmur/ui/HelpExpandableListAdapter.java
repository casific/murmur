package org.denovogroup.murmur.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import org.denovogroup.murmur.R;
import org.denovogroup.murmur.backend.Utils;

import java.util.List;
import java.util.Map;

/**
 * Created by Liran on 1/11/2016.
 */
public class HelpExpandableListAdapter extends BaseExpandableListAdapter {

    Context context;
    List<String> headers;
    Map<String,List<String>> data;

    ImageGetter imageGetter;

    public HelpExpandableListAdapter(Context context, List<String> headers, Map<String, List<String>> data) {
        this.context = context;
        this.data = data;
        this.headers = headers;
        this.imageGetter = new ImageGetter();
    }

    @Override
    public int getGroupCount() {
        return data.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return data.get(headers.get(groupPosition)).size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return headers.get(groupPosition);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return data.get(headers.get(groupPosition)).get(childPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {

        if(convertView == null){
            convertView = LayoutInflater.from(context).inflate(R.layout.help_title, parent, false);
        }
        ((TextView) convertView.findViewById(R.id.help_title)).setText((String) getGroup(groupPosition));
        convertView.findViewById(R.id.shadow).setVisibility(isExpanded ? View.GONE : View.VISIBLE);

        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {

        if(convertView == null){
            convertView = LayoutInflater.from(context).inflate(R.layout.help_body, parent, false);
        }

        convertView.measure(View.MeasureSpec.UNSPECIFIED,View.MeasureSpec.UNSPECIFIED);

        ((TextView) convertView.findViewById(R.id.help_body)).setText(Html.fromHtml((String)getChild(groupPosition, childPosition),imageGetter, null));

        convertView.findViewById(R.id.shadow).setVisibility(isLastChild ? View.VISIBLE : View.GONE);

        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return false;
    }

    private class ImageGetter implements Html.ImageGetter {

        @Override
        public Drawable getDrawable(String source) {
            int id;
            if (source.equals("icon_demo.png")) {
                id = R.drawable.icon_demo;
            }
            else {
                return null;
            }

            Drawable d = ContextCompat.getDrawable(context, id);

            int containerSize = Utils.dpToPx(270, context);
            float whR = d.getIntrinsicWidth()/((float)d.getIntrinsicHeight());

            if(d != null) {
                d.setBounds(0, 0, containerSize, (int)(containerSize/whR));
            }
            return d;
        }
    }
}
