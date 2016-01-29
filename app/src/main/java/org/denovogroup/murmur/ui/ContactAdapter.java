package org.denovogroup.murmur.ui;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.text.Html;
import android.text.Spannable;
import android.text.style.BackgroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.denovogroup.murmur.R;
import org.denovogroup.murmur.backend.FriendStore;

/**
 * Created by Liran on 1/3/2016.
 */
public class ContactAdapter extends CursorAdapter {

    boolean selectionMode = false;

    int name_colIndex;
    int number_colIndex;
    int addedVia_colIndex;
    int checked_colIndex;

    /**
     * Holds references to views so that findViewById() is not needed to be
     * called so many times.
     */
    ViewHolder viewHolder;

    private String[] highlight;

    public ContactAdapter(Context context, Cursor c, boolean inSelectionMode) {
        super(context, c, false);
        selectionMode = inSelectionMode;
        init(context,c);
    }

    private void init(Context context,Cursor cursor) {

        name_colIndex = cursor.getColumnIndexOrThrow(FriendStore.COL_DISPLAY_NAME);
        number_colIndex = cursor.getColumnIndexOrThrow(FriendStore.COL_NUMBER);
        addedVia_colIndex = cursor.getColumnIndexOrThrow(FriendStore.COL_ADDED_VIA);
        checked_colIndex = cursor.getColumnIndexOrThrow(FriendStore.COL_CHECKED);
    }

    public void setHighlight(String[] highlight) {
        this.highlight = highlight;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {

        View convertView = LayoutInflater.from(context).inflate(selectionMode ? R.layout.contact_list_item_selection : R.layout.contact_list_item, parent, false);

        viewHolder = new ViewHolder();
        viewHolder.mCheckBox = (CheckBox) convertView.findViewById(R.id.checkbox);
        viewHolder.mIconView = (ImageView) convertView.findViewById(R.id.item_icon);
        viewHolder.mIndexView = (TextView) convertView.findViewById(R.id.char_index);
        viewHolder.mNameView = (TextView) convertView.findViewById(R.id.item_text);
        viewHolder.mDivider = convertView.findViewById(R.id.item_divider);
        convertView.setTag(viewHolder);

        return convertView;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        viewHolder = (ViewHolder) view.getTag();

        boolean addedViaPhone = cursor.getInt(addedVia_colIndex) == FriendStore.ADDED_VIA_PHONE;

        if(viewHolder.mCheckBox != null){
            viewHolder.mCheckBox.setChecked(cursor.getInt(checked_colIndex) == FriendStore.TRUE);
            viewHolder.mCheckBox.setFocusable(false);
        }
        viewHolder.mIconView.setImageResource(addedViaPhone ? R.drawable.ic_phonebook_dark : R.drawable.ic_qr_dark);

        setHashtagLinks(viewHolder.mNameView, cursor.getString(name_colIndex));

        char currentIndex = cursor.getString(name_colIndex).toUpperCase().charAt(0);
        char prevIndex = '^';
        char nextIndex = '^';

        if(cursor.getPosition() > 0 && cursor.moveToPrevious()){
            prevIndex = cursor.getString(name_colIndex).toUpperCase().charAt(0);
            cursor.moveToNext();
        }

        if(cursor.moveToNext()){
            nextIndex = cursor.getString(name_colIndex).toUpperCase().charAt(0);
            cursor.moveToPrevious();
        }

        if(viewHolder.mIndexView != null){
            viewHolder.mIndexView.setText(currentIndex+"");
            viewHolder.mIndexView.setVisibility((currentIndex != prevIndex) ? View.VISIBLE : View.INVISIBLE);
        }

        viewHolder.mDivider.setVisibility(currentIndex != nextIndex ? View.VISIBLE : View.INVISIBLE);
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

                    while (str.length() > 0 && digest.contains(str.toLowerCase())) {
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

    static class ViewHolder {
        /**
         * The view object that holds the icon for this current row item.
         */
        private ImageView mIconView;
        /**
         * The view object that holds the char index for this current row item
         */
        private TextView mIndexView;
        /**
         * The view object that holds the name for this current row item.
         */
        private TextView mNameView;
        /**
         * The view object that holds the check to delete for this current row item.
         */
        private CheckBox mCheckBox;

        private View mDivider;
    }

    public boolean isSelectionMode() {
        return selectionMode;
    }

    public void setSelectionMode(boolean selectionMode) {
        this.selectionMode = selectionMode;
    }
}
