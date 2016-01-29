package org.denovogroup.murmur.ui;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.style.BackgroundColorSpan;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.denovogroup.murmur.R;
import org.denovogroup.murmur.backend.MessageStore;
import org.denovogroup.murmur.backend.SecurityProfile;
import org.denovogroup.murmur.backend.Utils;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Liran on 12/27/2015.
 *
 * The adapter in charge of converting raw sql data to visible items in a list view it is attached to
 */
public class FeedAdapter extends CursorAdapter {

    private int text_colIndex;
    private int trust_colIndex;
    private int priority_colIndex;
    private int liked_colIndex;
    private int pseudonym_colIndex;
    private int timestamp_colIndex;
    private int timebound_colIndex;
    private int read_colIndex;
    private int location_colIndex;
    private int parent_colIndex;
    private int messageId_colIndex;
    private int favorite_colIndex;
    private int checked_colIndex;
    private int restricted_colIndex;

    private boolean selectionMode;

    private SecurityProfile currentProfile;
    private FeedAdapterCallbacks callbacks;
    private ViewHolder viewHolder;

    private ListView listView;

    int linkColor;
    int normalBgColor;
    int checkedBgColor;

    private String[] highlight;
    private String hexlinkColor;

    public interface FeedAdapterCallbacks{
        void onUpvote(String message, int oldPriority);
        void onDownvote(String message, int oldPriority);
        void onNavigate(String message, String latxLon);
        void onReply(String messageId, String sender);
        void onFavorite(String message, boolean isFavoriteBefore);
    }

    public FeedAdapter(Context context, Cursor cursor, boolean selectionMode, FeedAdapterCallbacks callbacks) {
        super(context, cursor, false);

        this.selectionMode = selectionMode;
        this.callbacks = callbacks;
        init(context, cursor);
    }

    public FeedAdapter(Context context, Cursor cursor, boolean selectionMode) {
        this(context, cursor, selectionMode, null);
    }

    public void setHighlight(String[] highlight) {
        this.highlight = highlight;
    }

    private void init(Context context , Cursor cursor){
        linkColor = context.getResources().getColor(R.color.app_purple);
        hexlinkColor = String.format("#%06X", (0xFFFFFF & linkColor));
        normalBgColor = context.getResources().getColor(R.color.list_item_bg);
        checkedBgColor = context.getResources().getColor(R.color.list_item_bg_checked);

        currentProfile = org.denovogroup.murmur.backend.SecurityManager.getCurrentProfile(context);

        text_colIndex = cursor.getColumnIndexOrThrow(MessageStore.COL_MESSAGE);
        trust_colIndex = cursor.getColumnIndexOrThrow(MessageStore.COL_TRUST);
        priority_colIndex = cursor.getColumnIndexOrThrow(MessageStore.COL_LIKES);
        liked_colIndex = cursor.getColumnIndexOrThrow(MessageStore.COL_LIKED);
        pseudonym_colIndex = cursor.getColumnIndexOrThrow(MessageStore.COL_PSEUDONYM);
        timestamp_colIndex = cursor.getColumnIndexOrThrow(MessageStore.COL_TIMESTAMP);
        read_colIndex = cursor.getColumnIndexOrThrow(MessageStore.COL_READ);
        timebound_colIndex = cursor.getColumnIndexOrThrow(MessageStore.COL_EXPIRE);
        location_colIndex = cursor.getColumnIndexOrThrow(MessageStore.COL_LATLONG);
        parent_colIndex = cursor.getColumnIndexOrThrow(MessageStore.COL_PARENT);
        messageId_colIndex = cursor.getColumnIndexOrThrow(MessageStore.COL_MESSAGE_ID);
        favorite_colIndex = cursor.getColumnIndexOrThrow(MessageStore.COL_FAVIRITE);
        checked_colIndex =  cursor.getColumnIndexOrThrow(MessageStore.COL_CHECKED);
        restricted_colIndex =  cursor.getColumnIndexOrThrow(MessageStore.COL_MIN_CONTACTS_FOR_HOP);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {

        listView = (ListView) parent;

        View convertView = LayoutInflater.from(context).inflate(
                selectionMode ? R.layout.feed_list_item_w_checkbox : R.layout.feed_list_item, parent, false);

        viewHolder = new ViewHolder();
        viewHolder.itemContainer = convertView.findViewById(R.id.feed_item);
        viewHolder.pseudonym = (TextView) convertView.findViewById(R.id.feed_item_pseudonym);
        viewHolder.time = (TextView) convertView.findViewById(R.id.feed_item_time);
        viewHolder.message = (TextView) convertView.findViewById(R.id.feed_item_message);
        viewHolder.connection = (TextView) convertView.findViewById(R.id.feed_item_connection);
        viewHolder.navigate = (TextView) convertView.findViewById(R.id.feed_item_navigate);
        viewHolder.likes = (TextView) convertView.findViewById(R.id.feed_item_likes);
        viewHolder.favorite = (TextView) convertView.findViewById(R.id.feed_item_favorite);
        viewHolder.replies = (TextView) convertView.findViewById(R.id.feed_item_replies);
        viewHolder.restricted = (TextView) convertView.findViewById(R.id.feed_item_restricted);

        viewHolder.checkbox = (CheckBox) convertView.findViewById(R.id.feed_item_checkbox);
        convertView.setTag(viewHolder);

        return convertView;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        viewHolder = (ViewHolder) view.getTag();

        //set content
        if(currentProfile.isPseudonyms()){
            String pseudonym = cursor.getString(pseudonym_colIndex);
            viewHolder.pseudonym.setText(pseudonym != null && pseudonym.length() > 0 ? "@"+cursor.getString(pseudonym_colIndex) : "");
        }

        long timebound = cursor.getLong(timebound_colIndex);
        long timstamp = cursor.getLong(timestamp_colIndex);

        String timeboundString = (timebound > 0) ?  context.getString(R.string.self_destruct_in)+" "+Utils.convertTimestampToRelativeHoursRound(timstamp + timebound)+context.getString(R.string.hours_short) : null;

        int age = timstamp > 0 ? Utils.convertTimestampToRelativeHours(timstamp) : -1;
        String timestampString = null;
        if(currentProfile.isTimestamp() && age >= 0) {
            if(age == 0) {
                timestampString = context.getString(R.string.just_now);
            }else {
                timestampString = (age < 24) ? age + context.getString(R.string.h_ago) : ((int) Math.floor(age / 24f)) + context.getString(R.string.d_ago);
            }
        }

        if(timeboundString != null && timestampString != null){
            viewHolder.time.setText(timestampString+" | "+timeboundString);
        } else if(timeboundString != null){
            viewHolder.time.setText(timeboundString);
        } else if(timestampString != null){
            viewHolder.time.setText(timestampString);
        } else {
            viewHolder.time.setText("");
        }

        viewHolder.connection.setVisibility(currentProfile.isUseTrust() ? View.VISIBLE : View.INVISIBLE);
        viewHolder.connection.setText(currentProfile.isUseTrust() ?
                String.valueOf(Math.round(100 * cursor.getFloat(trust_colIndex))) : "");

        viewHolder.likes.setText(String.valueOf(cursor.getInt(priority_colIndex)));
        viewHolder.likes.setActivated(cursor.getInt(liked_colIndex) == MessageStore.TRUE);

        setHashtagLinks(viewHolder.message, cursor.getString(text_colIndex));

        boolean hasLocation = false;
        if(currentProfile.isShareLocation()){
            String location = cursor.getString(location_colIndex);
            hasLocation = (location != null && location.indexOf(" ")> 0);
        }
        viewHolder.navigate.setVisibility(hasLocation ? View.VISIBLE : View.GONE);

        viewHolder.replies.setText(String.valueOf(MessageStore.getInstance(context).getCommentCount(cursor.getString(messageId_colIndex))));

        viewHolder.favorite.setActivated(cursor.getInt(favorite_colIndex) == MessageStore.TRUE);

        viewHolder.restricted.setVisibility(cursor.getInt(restricted_colIndex) > 0 ? View.VISIBLE : View.INVISIBLE);

        //set callbacks to all views that require a callback
        if(selectionMode) {
            boolean isChecked = cursor.getInt(checked_colIndex) == MessageStore.TRUE;
           viewHolder.checkbox.setChecked(isChecked);
            viewHolder.checkbox.setClickable(false);
            viewHolder.itemContainer.setBackground(new ColorDrawable(isChecked ? checkedBgColor : normalBgColor));
        } else {
            viewHolder.favorite.setOnClickListener(clickListener);
            viewHolder.navigate.setOnClickListener(clickListener);
            viewHolder.likes.setOnClickListener(clickListener);
            viewHolder.replies.setOnClickListener(clickListener);
        }
    }

    public void setAdapterCallbacks(FeedAdapterCallbacks callbacks){
        this.callbacks = callbacks;
    }

    /** set hashtag links to any hashtag in the supplied text and assign the text to the supplied
     * text view, clicking a hashtag will call new instance of the activity with intent filter of
     * //hashtag
     * @param textView
     * @param source
     */
    private void setHashtagLinks(TextView textView, String source){

        StringBuilder hashtaggedMessageBuilder = new StringBuilder();
        SpannableString spannableString = new SpannableString(source);
        Linkify.addLinks(spannableString, Linkify.WEB_URLS | Linkify.PHONE_NUMBERS);

        String hashtaggedMessage = source;
        Set<String> hashtags = Utils.getHashtags(source);
        for(String hashtag : hashtags){
            String textBefore = hashtaggedMessage.substring(0,hashtaggedMessage.indexOf(hashtag));
            String textAfter = hashtaggedMessage.substring(hashtaggedMessage.indexOf(hashtag)+hashtag.length());
            if(selectionMode){
                hashtaggedMessage = textBefore+"<font color="+hexlinkColor+">"+hashtag+"</font>"+textAfter;
            } else {
                hashtaggedMessage = textBefore+"<a href=\"murmur://org.denovogroup.murmur/hashtag/"+hashtag+"/\">"+hashtag+"</a>"+textAfter;
            }
            Linkify.addLinks(spannableString, Pattern.compile(hashtag), "murmur://", new Linkify.MatchFilter() {
                @Override
                public boolean acceptMatch(CharSequence s, int start, int end) {
                    return !AlreadyInSpan(new SpannableString(s), start, end);
                }
            }, new Linkify.TransformFilter() {
                @Override
                public String transformUrl(Matcher match, String url) {
                    return "murmur://org.denovogroup.murmur/hashtag/"+url;
                }
            });
        }
        if(highlight != null) {
            for (String str : highlight) {
                if (source.toLowerCase().contains(str.toLowerCase())) {
                    int startoffset = 0;
                    String digest = source.toLowerCase();

                    while (str.length() > 0 && digest.contains(str.toLowerCase())) {
                        int digestStart = digest.indexOf(str.toLowerCase());
                        int start = startoffset + digestStart;
                        int end = start + str.length();
                        spannableString.setSpan(new BackgroundColorSpan(Color.parseColor("#FFFF02"))
                                , start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        digest = digest.substring(digestStart + str.length());
                        startoffset = end;
                    }
                }
            }
        }

        textView.setText(removeUnderlines(spannableString));

        textView.setLinkTextColor(linkColor);
        textView.setLinksClickable(!selectionMode);
        textView.setMovementMethod(selectionMode ? null : LinkMovementMethod.getInstance());
    }

    public static boolean AlreadyInSpan(Spannable text, int start, int end)
    {
        URLSpan[] spans = text.getSpans(start, end, URLSpan.class);
        return spans.length > 0;
    }
    public static Spannable removeUnderlines(Spannable p_Text) {
        URLSpan[] spans = p_Text.getSpans(0, p_Text.length(), URLSpan.class);
        for (URLSpan span : spans) {
            int start = p_Text.getSpanStart(span);
            int end = p_Text.getSpanEnd(span);
            p_Text.removeSpan(span);
            span = new URLSpanNoUnderline(span.getURL());
            p_Text.setSpan(span, start, end, 0);
        }
        return p_Text;
    }

    View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(listView == null || callbacks == null) return;

            // get index of clicked item
            int containerIndex = -1;

            for(int i=0; i<listView.getChildCount();i++){
                if(listView.getChildAt(i).findViewById(v.getId()) == v){
                    containerIndex = i;
                    break;
                }
            }
            if(containerIndex < 0) return;

            int cursorPosition = getCursor().getPosition();
            getCursor().moveToPosition(listView.getFirstVisiblePosition()+containerIndex);

            String message = getCursor().getString(text_colIndex);
            boolean isFavorite = getCursor().getInt(favorite_colIndex) == MessageStore.TRUE;
            boolean isLiked = getCursor().getInt(liked_colIndex) == MessageStore.TRUE;
            int priority = getCursor().getInt(priority_colIndex);
            String location = getCursor().getString(location_colIndex);
            String messageId = getCursor().getString(messageId_colIndex);
            String pseudonym = getCursor().getString(pseudonym_colIndex);

            getCursor().moveToPosition(cursorPosition);

            switch (v.getId()){
                case R.id.feed_item_favorite:
                    callbacks.onFavorite(message, isFavorite);
                    break;
                case R.id.feed_item_likes:
                    if(isLiked){
                        callbacks.onDownvote(message, priority);
                    } else {
                        callbacks.onUpvote(message, priority);
                    }
                    break;
                case R.id.feed_item_navigate:
                    callbacks.onNavigate(message,location);
                    break;
                case R.id.feed_item_replies:
                    callbacks.onReply(messageId, pseudonym);
                    break;
            }
        }
    };

    public void setSelectionMode(boolean selectionModeOn){
        selectionMode = selectionModeOn;
        if(listView != null) {
            int childCount = listView.getChildCount();
            for (int i = 0; i < childCount; i++) {
                listView.getChildAt(i).invalidate();
            }
        }
    }

    /**
     * This is used to recycle the views and increase speed of scrolling. This
     * is held by the row object that keeps references to the views so that they
     * do not have to be looked up every time they are populated or reshown.
     */
    static class ViewHolder {
        private View itemContainer;
        private TextView pseudonym;
        private TextView time;
        private TextView message;
        private TextView connection;
        private TextView navigate;
        private TextView likes;
        private TextView replies;
        private TextView favorite;
        private TextView restricted;
        private CheckBox checkbox;
    }
}
