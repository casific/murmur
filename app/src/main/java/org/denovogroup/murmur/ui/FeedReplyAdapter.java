package org.denovogroup.murmur.ui;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.text.Html;
import android.text.Spannable;
import android.text.style.BackgroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

import org.denovogroup.murmur.R;
import org.denovogroup.murmur.backend.*;

/**
 * Created by Liran on 12/30/2015.
 */
public class FeedReplyAdapter extends CursorAdapter {

    private int message_colIndex;
    private int timestamp_colIndex;
    private int pseudonym_colIndex;

    private ViewHolder viewHolder;
    SecurityProfile securityProfile;
    ViewGroup parent;

    private String[] highlight;

    String[] colors = new String[]{
            "#ef9a9a",
            "#b39ddb",
            "#9fa8da",
            "#c5e1a5",
            "#c5e1a5",
            "#ffe082",
            "#ffab91"
    };

    public FeedReplyAdapter(Context context, Cursor c) {
        super(context, c, false);
        initAdapter(context, c);
    }

    private void initAdapter(Context context, Cursor cursor){
        securityProfile = org.denovogroup.murmur.backend.SecurityManager.getCurrentProfile(context);

        message_colIndex = cursor.getColumnIndex(MessageStore.COL_MESSAGE);
        pseudonym_colIndex = cursor.getColumnIndex(MessageStore.COL_PSEUDONYM);
        timestamp_colIndex = cursor.getColumnIndex(MessageStore.COL_TIMESTAMP);
    }

    public void setHighlight(String[] highlight) {
        this.highlight = highlight;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        this.parent = parent;
        View convertView = LayoutInflater.from(context).inflate(R.layout.feed_item_reply, parent, false);

        viewHolder = new ViewHolder();
        viewHolder.deadSpace = convertView.findViewById(R.id.feed_reply_item_dead_space);
        viewHolder.colorTab = convertView.findViewById(R.id.feed_reply_item_color_tab);
        viewHolder.pseudonym = (TextView) convertView.findViewById(R.id.feed_reply_item_pseudonym);
        viewHolder.time = (TextView) convertView.findViewById(R.id.feed_reply_item_time);
        viewHolder.message = (TextView) convertView.findViewById(R.id.feed_reply_item_message);

        convertView.setTag(viewHolder);

        return convertView;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        viewHolder = (ViewHolder) view.getTag();

        int position = cursor.getPosition();
        int poolSize = colors.length;
        int offset = (int) Math.floor(position/poolSize);

        if(position == 0){
            viewHolder.deadSpace.getLayoutParams().width = 0;
            viewHolder.deadSpace.invalidate();
            viewHolder.colorTab.setBackground(new ColorDrawable(Color.parseColor(colors[0])));
        } else {
            for (int i = poolSize; i > 0; i--) {
                if ((position - offset * poolSize) % i == 0) {
                    int innerOffset = offset > 0 ? -1 : 0;
                    viewHolder.deadSpace.getLayoutParams().width = viewHolder.colorTab.getLayoutParams().width * (i+innerOffset);
                    viewHolder.deadSpace.invalidate();
                    viewHolder.colorTab.setBackground(new ColorDrawable(Color.parseColor(colors[i+innerOffset])));
                    break;
                }
            }
        }

        setHashtagLinks(viewHolder.message, cursor.getString(message_colIndex));
        String pseudonymString = securityProfile.isPseudonyms() ? cursor.getString(pseudonym_colIndex) : "";
        viewHolder.pseudonym.setText(pseudonymString.length() > 0 ? "@" + pseudonymString : "");

        //get timestamp in proper format
        long timstamp = cursor.getLong(timestamp_colIndex);
        int age = timstamp > 0 ? Utils.convertTimestampToRelativeHours(timstamp) : -1;
        String timestampString = null;
        if(securityProfile.isTimestamp() && age >= 0) {
            if(age == 0) {
                timestampString = context.getString(R.string.just_now);
            }else {
                timestampString = (age < 24) ? age + context.getString(R.string.h_ago) : ((int) Math.floor(age / 24f)) + context.getString(R.string.d_ago);
            }
        }

        viewHolder.time.setText(timestampString != null ? timestampString : "");
    }

    private static class ViewHolder{
        View deadSpace;
        View colorTab;
        TextView pseudonym;
        TextView time;
        TextView message;
    }

    private void setHashtagLinks(TextView textView, String source){

        String hashtaggedMessage = source;

        //String hexColor = String.format("#%06X", (0xFFFFFF & linkColor));

        /*List<String> hashtags = Utils.getHashtags(source);
        for(String hashtag : hashtags){
            String textBefore = hashtaggedMessage.substring(0,hashtaggedMessage.indexOf(hashtag));
            String textAfter = hashtaggedMessage.substring(hashtaggedMessage.indexOf(hashtag)+hashtag.length());
            if(selectionMode){
                hashtaggedMessage = textBefore+"<font color="+hexColor+">"+hashtag+"</font>"+textAfter;
            } else {
                hashtaggedMessage = textBefore+"<a href=\"org.denovogroup.murmur://hashtag/"+hashtag+"/\">"+hashtag+"</a>"+textAfter;
            }*
        }
        if(!selectionMode){
            //fix <a href> extended to the end of the line making entire following text clickable
            if(hashtaggedMessage.indexOf("</a>") == hashtaggedMessage.length()-4){
                hashtaggedMessage += " ";
            }
        }*/

        Spannable spannable = (Spannable) Html.fromHtml(hashtaggedMessage);

        if(highlight != null) {
            for (String str : highlight) {
                if (source.toLowerCase().contains(str.toLowerCase())) {
                    int startoffset = 0;
                    String digest = source.toLowerCase();

                    while (digest.contains(str.toLowerCase())) {
                        int digestStart = digest.indexOf(str.toLowerCase());
                        int start = startoffset + digestStart;
                        int end = start + str.length();
                        spannable.setSpan(new BackgroundColorSpan(Color.parseColor("#FFFF02"))
                                , start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        digest = digest.substring(digestStart + str.length());
                        startoffset = end;
                    }
                }
            }
        }

        textView.setText(spannable);
        /*textView.setText(selectionMode ? spannable : removeUnderlines(spannable));

        if(!selectionMode) {
            textView.setLinkTextColor(linkColor);
            textView.setLinksClickable(true);
            textView.setMovementMethod(LinkMovementMethod.getInstance());
        }*/
    }
}
