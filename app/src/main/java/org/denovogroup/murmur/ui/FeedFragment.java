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

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import org.denovogroup.murmur.R;
import org.denovogroup.murmur.backend.*;
import org.denovogroup.murmur.backend.SecurityManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Liran on 12/27/2015.
 *
 * The fragment which display the message feed overview
 */
public class FeedFragment extends Fragment implements View.OnClickListener, TextWatcher, FragmentBackHandler{

    public static final int REQ_CODE_MESSAGE = 100;
    public static final int REQ_CODE_SEARCH = 101;
    public static final String HASHTAG = "hashtag";

    private static final int MAX_NEW_MESSAGES_DISPLAY = 1000;

    private boolean inSearchMode = false;
    private boolean inSelectionMode = false;
    private boolean selectAll = false;

    private ListView feedListView;
    private Button newPostButton;

    private ViewGroup newMessagesNotification;
    private TextView newMessagesNotification_text;
    private Button newMessagesNotification_button;
    private Spinner sortSpinner;
    private TextView leftText;
    private sortOption currentSort = sortOption.NEWEST;
    private EditText searchView;

    Menu menu;

    private String query = "";

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        query = s.toString();
        FeedAdapter adapter = new FeedAdapter(getActivity(), getCursor(), false, feedAdapterCallbacks);
        if(SearchHelper.searchToSQL(query) == null) {
            adapter.setHighlight(Utils.getKeywords(query));
        }
        feedListView.setAdapter(adapter);

    }

    @Override
    public void afterTextChanged(Editable s) {}

    private enum sortOption{
        NEWEST, OLDEST, MOST_ENDORSED, LEAST_ENDORSED, MOST_CONNECTED, LEAST_CONNECTED
    }

    private List<Object[]> sortOptions = new ArrayList<Object[]>(){{
        add(new Object[]{R.drawable.sort_spinner_newest, R.string.sort_opt_newest, sortOption.NEWEST});
        add(new Object[]{R.drawable.sort_spinner_oldest,R.string.sort_opt_oldest, sortOption.OLDEST});
        add(new Object[]{R.drawable.sort_spinner_most_endorsed,R.string.sort_opt_mostendorsed, sortOption.MOST_ENDORSED});
        add(new Object[]{R.drawable.sort_spinner_least_endorsed,R.string.sort_opt_leastendorsed, sortOption.LEAST_ENDORSED});
        add(new Object[]{R.drawable.sort_spinner_most_connected,R.string.sort_opt_mostconnected, sortOption.MOST_CONNECTED});
        add(new Object[]{R.drawable.sort_spinner_least_connected,R.string.sort_opt_leastconnected, sortOption.LEAST_CONNECTED});
    }};

    // Create reciever object
    private BroadcastReceiver receiver;

    // Set When broadcast event will fire.
    private IntentFilter filter = new IntentFilter(MessageStore.NEW_MESSAGE);

    /**
     * This is the broadcast receiver object that I am registering. I created a
     * new class in order to override onReceive functionality.
     *
     * @author jesus
     *
     */
    public class MessageEventReceiver extends BroadcastReceiver {

        /**
         * When the receiver is activated then that means a message has been
         * added to the message store, (either by the user or by the active
         * services). The reason that the instanceof check is necessary is
         * because there are two possible routes of activity:
         *
         * 1) The previous/current fragment viewed could have been the about
         * fragment, if it was then the focused fragment is not a
         * ListFragmentOrganizer and when the user returns to the feed then the
         * feed will check its own data set and not crash.
         *
         * 2) The previous/current fragment is the feed, it needs to be notified
         * immediately that there was a change in the underlying dataset.
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            setPendingUnreadMessagesDisplay();
        }
    }

    public interface FeedFragmentCallbacks{
        void onFeedItemExpand(String messageId);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(R.string.drawer_menu_feed);

        searchView = (EditText) ((MainActivity)getActivity()).getToolbar().findViewById(R.id.searchView);

        leftText = (TextView) ((MainActivity) getActivity()).getToolbar().findViewById(R.id.leftText);

        initSortSpinner();

        View v = inflater.inflate(R.layout.feed_fragment, container, false);

        feedListView = (ListView) v.findViewById(R.id.feed_listView);
        newPostButton = (Button) v.findViewById(R.id.new_post_button);
            newPostButton.setOnClickListener(this);
        newMessagesNotification = (ViewGroup) v.findViewById(R.id.new_message_notification);
        newMessagesNotification_text = (TextView) v.findViewById(R.id.new_message_notification_desc);
        newMessagesNotification_button = (Button) v.findViewById(R.id.new_message_notification_btn);
            newMessagesNotification_button.setOnClickListener(this);

        MessageStore.getInstance(getActivity()).setAllAsRead();

        setListView();

        Bundle args = getArguments();
        if(args != null && args.containsKey(HASHTAG)){
           searchHashTagFromClick(args.getString(HASHTAG));
            args.remove(HASHTAG);
        }

        return v;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.new_post_button:
                Intent intent = new Intent(getActivity(), org.denovogroup.murmur.ui.PostActivity.class);
                startActivityForResult(intent, REQ_CODE_MESSAGE);
                break;
            case R.id.new_message_notification_btn:
                //TODO
                break;
        }
    }

    private void initSortSpinner(){
        if(getActivity() instanceof MainActivity) {
            sortSpinner = (Spinner) ((MainActivity) getActivity()).getToolbar().findViewById(R.id.sortSpinner);
            sortSpinner.setAdapter(new FeedSortSpinnerAdapter(getActivity(), sortOptions, inSearchMode));
            for(int i=0; i<sortOptions.size();i++){
                if(sortOptions.get(i)[2] == currentSort){
                    sortSpinner.setSelection(i);
                    break;
                }
            }
            sortSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    currentSort = (sortOption) sortOptions.get(position)[2];

                    MessageStore store = MessageStore.getInstance(getActivity());

                    switch(currentSort){
                        case LEAST_CONNECTED:
                            store.setSortOption(new String[]{MessageStore.COL_TRUST}, true);
                            break;
                        case MOST_CONNECTED:
                            store.setSortOption(new String[]{MessageStore.COL_TRUST}, false);
                            break;
                        case LEAST_ENDORSED:
                            store.setSortOption(new String[]{MessageStore.COL_LIKES}, true);
                            break;
                        case MOST_ENDORSED:
                            store.setSortOption(new String[]{MessageStore.COL_LIKES}, false);
                            break;
                        case NEWEST:
                            store.setSortOption(new String[]{MessageStore.COL_ROWID}, false);
                            break;
                        case OLDEST:
                            store.setSortOption(new String[]{MessageStore.COL_ROWID}, true);
                            break;
                    }

                    feedListView.setAdapter(new FeedAdapter(getActivity(), getCursor(), inSelectionMode, feedAdapterCallbacks));
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    // do nothing
                }
            });
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.feed_fragment_menu, menu);
        this.menu = menu;

        //Setup the search view
        /*MenuItem searchItem = menu.findItem(R.id.search);
        searchView = (SearchView) searchItem.getActionView();
        setSearchView(searchView);*/

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);


    }

    /** callback handler from clicks on buttons inside the feed list view */
    FeedAdapter.FeedAdapterCallbacks feedAdapterCallbacks = new FeedAdapter.FeedAdapterCallbacks() {
        @Override
        public void onUpvote(String message, int oldPriority) {
            MessageStore.getInstance(getActivity()).likeMessage(
                    message,
                    true);
            swapCursor();
        }

        @Override
        public void onDownvote(String message, int oldPriority) {
            MessageStore.getInstance(getActivity()).likeMessage(
                    message,
                    false);
            swapCursor();
        }

        @Override
        public void onFavorite(String message, boolean isFavoriteBefore) {
            MessageStore.getInstance(getActivity()).favoriteMessage(
                    message,
                    !isFavoriteBefore);
            swapCursor();
        }

        @Override
        public void onNavigate(String message, String latxLon) {
            double lat = Double.parseDouble(latxLon.substring(0, latxLon.indexOf(" ")));
            double lon = Double.parseDouble(latxLon.substring(latxLon.indexOf(" ") + 1));

            Uri gmmIntentUri = Uri.parse("geo:"+lat+","+lon);
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");
            if (mapIntent.resolveActivity(getActivity().getPackageManager()) != null) {
                getActivity().startActivity(mapIntent);
            }
        }

        @Override
        public void onReply(String parentId, String sender) {
            if (MessageStore.getInstance(getActivity()).getCommentCount(parentId) <= 0) {
                Intent intent = new Intent(getActivity(), PostActivity.class);
                intent.putExtra(PostActivity.MESSAGE_PARENT, parentId);
                if(sender != null && sender.length() > 0) intent.putExtra(PostActivity.MESSAGE_BODY, "@"+sender+" ");
                startActivityForResult(intent, REQ_CODE_MESSAGE);
            } else if (getActivity() instanceof FeedFragmentCallbacks) {
                ((FeedFragmentCallbacks) getActivity()).onFeedItemExpand(parentId);
            }
        }
    };

    private Cursor getCursor(){
        String sqlQuery = SearchHelper.searchToSQL(query);
        return (sqlQuery != null) ?
                MessageStore.getInstance(getActivity()).getMessagesByQuery(sqlQuery) :
                MessageStore.getInstance(getActivity()).getMessagesContainingCursor(query, false, false, -1);
    }

    private void swapCursor(){

        int offsetFromTop = 0;
        int firstVisiblePosition = Math.max(0, feedListView.getFirstVisiblePosition());
        if(feedListView.getChildCount() > 0) {
            offsetFromTop = feedListView.getChildAt(0).getTop();
        }

        CursorAdapter newAdapter = ((CursorAdapter) feedListView.getAdapter());
        newAdapter.swapCursor(getCursor());
        if(SearchHelper.searchToSQL(query) == null) {
            ((FeedAdapter) newAdapter).setHighlight(Utils.getKeywords(query));
        }
        feedListView.setAdapter(newAdapter);

        feedListView.setSelectionFromTop(firstVisiblePosition, offsetFromTop);
    }

    private void setListView() {
        feedListView.setAdapter(new FeedAdapter(getActivity(), getCursor(), inSelectionMode, feedAdapterCallbacks));
        if(inSelectionMode) {
            setListInSelectionMode();
        } else {
            setListInDisplayMode();
        }
    }

    AdapterView.OnItemLongClickListener longClickListener = new AdapterView.OnItemLongClickListener() {
        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
            setListInSelectionMode();
            return false;
        }
    };

    private void setListInSelectionMode(){

        inSelectionMode = true;

        ((FeedAdapter) feedListView.getAdapter()).setSelectionMode(true);
        swapCursor();
        feedListView.setOnItemLongClickListener(null);
        feedListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Cursor c = ((CursorAdapter) feedListView.getAdapter()).getCursor();
                c.moveToPosition(position);
                boolean isChecked = c.getInt(c.getColumnIndex(MessageStore.COL_CHECKED)) == MessageStore.TRUE;
                String message = c.getString(c.getColumnIndex(MessageStore.COL_MESSAGE));

                MessageStore.getInstance(getActivity()).checkMessage(message, !isChecked);
                swapCursor();

                Cursor checkedCursor = MessageStore.getInstance(getActivity()).getCheckedMessages();
                int checkedCount = checkedCursor.getCount();
                updateSelectAll();
                ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(checkedCount <= 99 ? String.valueOf(checkedCount) : "+99");

                if (menu != null) {
                    menu.findItem(R.id.action_delete).setEnabled(checkedCount > 0);

                    boolean canDeleteTrust = false;
                    boolean canDeleteLikes = false;
                    boolean canDeleteSender = false;
                    boolean canDeleteExchange = false;
                    boolean canDeleteTree = false;

                    if (checkedCount == 1) {
                        checkedCursor.moveToFirst();
                        String sender = checkedCursor.getString(checkedCursor.getColumnIndex(MessageStore.COL_PSEUDONYM));

                        if (sender != null)
                            canDeleteSender = MessageStore.getInstance(getActivity()).getMessagesBySenderCount(sender) > 0;

                        String exchange = checkedCursor.getString(checkedCursor.getColumnIndex(MessageStore.COL_EXCHANGE));

                        if (exchange != null)
                            canDeleteExchange = MessageStore.getInstance(getActivity()).getMessagesByExchangeCount(exchange) > 0;

                        float trust = checkedCursor.getFloat(checkedCursor.getColumnIndex(MessageStore.COL_TRUST));

                        canDeleteTrust = SecurityManager.getCurrentProfile(getActivity()).isUseTrust() &&MessageStore.getInstance(getActivity()).getMessagesByTrustCount(trust) > 0;

                        int likes = checkedCursor.getInt(checkedCursor.getColumnIndex(MessageStore.COL_LIKES));

                        canDeleteLikes = MessageStore.getInstance(getActivity()).getMessagesByLikeCount(likes) > 0;

                        String treeId = checkedCursor.getString(checkedCursor.getColumnIndex(MessageStore.COL_MESSAGE_ID));
                        canDeleteTree = treeId != null && MessageStore.getInstance(getActivity()).getCommentCount(treeId) > 0;
                    }

                    menu.findItem(R.id.action_delete_by_connection).setEnabled(checkedCount == 1 && canDeleteTrust);
                    menu.findItem(R.id.action_delete_by_exchange).setEnabled(checkedCount == 1 && canDeleteExchange);
                    menu.findItem(R.id.action_delete_from_sender).setEnabled(checkedCount == 1 && canDeleteSender);
                    menu.findItem(R.id.action_delete_tree).setEnabled(checkedCount == 1 && canDeleteTree);
                    menu.findItem(R.id.action_retweet).setEnabled(checkedCount == 1);
                    menu.findItem(R.id.action_share).setEnabled(checkedCount == 1);
                }
                checkedCursor.close();
            }
        });
        setActionbar();

        newPostButton.setVisibility(View.INVISIBLE);
    }

    private void setListInDisplayMode(){
        inSelectionMode = false;
        MessageStore.getInstance(getActivity()).checkAllMessages(false, true);
                ((FeedAdapter) feedListView.getAdapter()).setSelectionMode(false);
        feedListView.setOnItemLongClickListener(longClickListener);
        feedListView.setOnItemClickListener(null/*new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //go to expanded view
                if (getActivity() instanceof FeedFragmentCallbacks) {

                    Cursor c = ((CursorAdapter) parent.getAdapter()).getCursor();
                    c.moveToPosition(position);
                    String messageId = c.getString(c.getColumnIndex(MessageStore.COL_MESSAGE_ID));

                    //if (MessageStore.getInstance(getActivity()).getCommentCount(messageId) > 0) {
                    ((FeedFragmentCallbacks) getActivity()).onFeedItemExpand(messageId);
                    //}
                }
            }
        }*/);
        swapCursor();
        setActionbar();
        newPostButton.setVisibility(View.VISIBLE);
    }

    private void setActionbar(){
        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if(actionBar != null) {
            Drawable actionbarBg;
            if(inSelectionMode){
                actionbarBg = new ColorDrawable(getActivity().getResources().getColor(R.color.toolbar_grey));
            } else if(inSearchMode) {
                actionbarBg = new ColorDrawable(getActivity().getResources().getColor(android.R.color.white));
            } else {
                if(Build.VERSION.SDK_INT >= 21){
                    actionbarBg = getResources().getDrawable(R.drawable.actionbar_default_bg, null);
                } else {
                    actionbarBg = getResources().getDrawable(R.drawable.actionbar_default_bg);
                }
            }
            actionBar.setBackgroundDrawable(actionbarBg);
            actionBar.setTitle(inSelectionMode ? R.string.empty_string : (inSearchMode ? R.string.empty_string : R.string.drawer_menu_feed));
        }
        if(menu != null) {
            menu.setGroupVisible(R.id.checked_only_actions, inSelectionMode);
            menu.findItem(R.id.search).setVisible(!inSearchMode && !inSelectionMode);

            Cursor checkedCursor = MessageStore.getInstance(getActivity()).getCheckedMessages();
            int checkedCount = checkedCursor.getCount();
            if(inSelectionMode)
                ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(checkedCount <= 99 ? String.valueOf(checkedCount) : "+99");

            menu.findItem(R.id.action_delete).setEnabled(checkedCount > 0);
            boolean canDeleteTrust = false;
            boolean canDeleteLikes = false;
            boolean canDeleteSender = false;
            boolean canDeleteExchange = false;
            boolean canDeleteTree = false;

            if(checkedCount == 1){
                checkedCursor.moveToFirst();
                String sender = checkedCursor.getString(checkedCursor.getColumnIndex(MessageStore.COL_PSEUDONYM));

                if(sender != null) canDeleteSender = MessageStore.getInstance(getActivity()).getMessagesBySenderCount(sender) > 0;

                String exchange = checkedCursor.getString(checkedCursor.getColumnIndex(MessageStore.COL_EXCHANGE));

                if(exchange != null) canDeleteExchange = MessageStore.getInstance(getActivity()).getMessagesByExchangeCount(exchange) > 0;

                float trust = checkedCursor.getFloat(checkedCursor.getColumnIndex(MessageStore.COL_TRUST));

                canDeleteTrust = SecurityManager.getCurrentProfile(getActivity()).isUseTrust() && MessageStore.getInstance(getActivity()).getMessagesByTrustCount(trust) > 0;

                int likes = checkedCursor.getInt(checkedCursor.getColumnIndex(MessageStore.COL_LIKES));

                canDeleteLikes = MessageStore.getInstance(getActivity()).getMessagesByLikeCount(likes) > 0;

                String treeId = checkedCursor.getString(checkedCursor.getColumnIndex(MessageStore.COL_MESSAGE_ID));
                canDeleteTree = treeId != null && MessageStore.getInstance(getActivity()).getCommentCount(treeId) > 0;
            }

            checkedCursor.close();

            menu.findItem(R.id.action_delete_by_connection).setEnabled(checkedCount == 1 && canDeleteTrust);
            menu.findItem(R.id.action_delete_by_exchange).setEnabled(checkedCount == 1 && canDeleteExchange);
            menu.findItem(R.id.action_delete_from_sender).setEnabled(checkedCount == 1 && canDeleteSender);
            menu.findItem(R.id.action_delete_tree).setEnabled(checkedCount == 1 && canDeleteTree);
            menu.findItem(R.id.action_retweet).setEnabled(checkedCount == 1);
            menu.findItem(R.id.action_share).setEnabled(checkedCount == 1);
        }

        if(searchView != null){
            searchView.setVisibility(inSearchMode && !inSelectionMode ? View.VISIBLE : View.GONE);
            searchView.removeTextChangedListener(this);
            searchView.setText(query);
            if(inSearchMode && !inSelectionMode){
                searchView.addTextChangedListener(this);
                searchView.requestFocus();
                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(searchView, InputMethodManager.SHOW_IMPLICIT);
            } else if(!inSelectionMode){
                query = "";
                searchView.removeTextChangedListener(this);
                searchView.setText("");
                //reset the list to its normal state
                swapCursor();
            }
        }

        ((DrawerActivityHelper) getActivity()).getDrawerToggle().setDrawerIndicatorEnabled(!(inSearchMode || inSelectionMode));
        ((DrawerActivityHelper) getActivity()).getDrawerToggle().syncState();

        if(actionBar != null) {
            if(inSelectionMode && inSearchMode){
                ((DrawerActivityHelper) getActivity()).getDrawerToggle().setDrawerIndicatorEnabled(true);
                ((DrawerActivityHelper) getActivity()).getDrawerToggle().syncState();
                ((DrawerActivityHelper) getActivity()).getDrawerToggle().setDrawerIndicatorEnabled(false);
                ((DrawerActivityHelper) getActivity()).getDrawerToggle().syncState();
            } else if (inSearchMode && !inSelectionMode) {
                actionBar.setHomeAsUpIndicator(R.drawable.ic_close_dark);
            }
        }

        if(sortSpinner != null) {
            sortSpinner.setVisibility(inSelectionMode ? View.GONE : View.VISIBLE);
            initSortSpinner();
        }
        if(leftText != null){
            updateSelectAll();
            leftText.setVisibility(inSelectionMode ? View.VISIBLE : View.GONE);
            leftText.setOnClickListener(inSelectionMode ? new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String sqlQuery = SearchHelper.searchToSQL(query);
                    if (sqlQuery != null) {
                        MessageStore.getInstance(getActivity()).checkAllQueriedMessages(!selectAll, sqlQuery);
                    } else {
                        MessageStore.getInstance(getActivity()).checkAllMessagesContaining(!selectAll, query);
                    }
                    selectAll = !selectAll;
                    swapCursor();

                    setActionbar();
                }
            } : null);
        }
    }

    private void updateSelectAll() {
        //TODO: Danielk Should this contain replies as well?
        int checkedCount = MessageStore.getInstance(getActivity()).getCheckedMessages().getCount();
        long totalCount = getCursor().getCount();
        selectAll = checkedCount == totalCount;
        if(leftText != null)
        {
            if (inSelectionMode)
                leftText.setText(!selectAll ? R.string.select_all : R.string.deselect_all);
            else
                leftText.setText(R.string.empty_string);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        final Cursor checkedMessages = MessageStore.getInstance(getActivity()).getCheckedMessages();
        checkedMessages.moveToFirst();
        AlertDialog.Builder dialog = null;

        switch (item.getItemId()){
            case android.R.id.home:
                if(inSelectionMode){
                    setListInDisplayMode();
                } else if(inSearchMode){
                    InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    if(searchView != null) imm.hideSoftInputFromWindow(searchView.getWindowToken(), 0);
                    inSearchMode = false;
                    setActionbar();
                    break;
                } else if(!inSelectionMode){
                    inSearchMode = false;
                    setActionbar();
                    break;
                }

                ActionBarDrawerToggle toogle = ((DrawerActivityHelper) getActivity()).getDrawerToggle();
                if(!toogle.isDrawerIndicatorEnabled()){
                    setListInDisplayMode();
                }
                break;
            case R.id.search:
                inSearchMode = true;
                setActionbar();
                break;
            case R.id.action_retweet:
                Intent intent = new Intent(getActivity(), PostActivity.class);
                intent.putExtra(PostActivity.MESSAGE_BODY, checkedMessages.getString(checkedMessages.getColumnIndex(MessageStore.COL_MESSAGE)));
                setListInDisplayMode();
                getActivity().startActivityForResult(intent, REQ_CODE_MESSAGE);
                break;
            case R.id.action_share:
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, getString(R.string.share_prefix));
                shareIntent.putExtra(Intent.EXTRA_TEXT, checkedMessages.getString(checkedMessages.getColumnIndex(MessageStore.COL_MESSAGE)));
                getActivity().startActivity(Intent.createChooser(shareIntent, getString(R.string.share_using)));
                break;
            case R.id.action_delete:
                dialog = new AlertDialog.Builder(getActivity());
                dialog.setTitle(R.string.delete_dialog_title);
                dialog.setMessage(getString(R.string.delete_dialog_message1) + " " + checkedMessages.getCount() + " " + getString(R.string.delete_dialog_message2));
                dialog.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                dialog.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        MessageStore.getInstance(getActivity()).removeCheckedMessage();
                        setListInDisplayMode();
                        dialog.dismiss();
                    }
                });
                break;
            case R.id.action_delete_by_connection:
                final float trust = checkedMessages.getFloat(checkedMessages.getColumnIndex(MessageStore.COL_TRUST));
                dialog = new AlertDialog.Builder(getActivity());
                dialog.setTitle(R.string.delete_dialog_title);
                dialog.setMessage(getString(R.string.delete_dialog_message1) + " "
                        + MessageStore.getInstance(getActivity()).getMessagesByTrustCount(trust) + " " + getString(R.string.delete_dialog_message2));
                dialog.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                dialog.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        MessageStore.getInstance(getActivity()).deleteByTrust(
                                trust
                        );
                        setListInDisplayMode();
                        dialog.dismiss();
                    }
                });
                break;
            case R.id.action_delete_by_exchange:
                final String exchange = checkedMessages.getString(checkedMessages.getColumnIndex(MessageStore.COL_EXCHANGE));
                dialog = new AlertDialog.Builder(getActivity());
                dialog.setTitle(R.string.delete_dialog_title);
                dialog.setMessage(getString(R.string.delete_dialog_message1) + " "
                        + MessageStore.getInstance(getActivity()).getMessagesByExchangeCount(exchange) + " " + getString(R.string.delete_dialog_message2));
                dialog.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                dialog.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        MessageStore.getInstance(getActivity()).deleteByExchange(exchange);
                        setListInDisplayMode();
                        dialog.dismiss();
                    }
                });
                break;
            case R.id.action_delete_from_sender:
                final String senderName = checkedMessages.getString(checkedMessages.getColumnIndex(MessageStore.COL_PSEUDONYM));
                dialog = new AlertDialog.Builder(getActivity());
                dialog.setTitle(R.string.delete_dialog_title);
                dialog.setMessage(getString(R.string.delete_dialog_message1) + " "
                        + MessageStore.getInstance(getActivity()).getMessagesBySenderCount(senderName) + " " + getString(R.string.delete_dialog_message2));
                dialog.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                dialog.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        MessageStore.getInstance(getActivity()).deleteBySender(
                                senderName
                        );
                        setListInDisplayMode();
                        dialog.dismiss();
                    }
                });
                break;
            case R.id.action_delete_tree:
                final String treeId = checkedMessages.getString(checkedMessages.getColumnIndex(MessageStore.COL_MESSAGE_ID));
                dialog = new AlertDialog.Builder(getActivity());
                dialog.setTitle(R.string.delete_dialog_title);
                dialog.setMessage(getString(R.string.delete_dialog_message1) + " "
                        + (MessageStore.getInstance(getActivity()).getCommentCount(treeId)+1) + " " + getString(R.string.delete_dialog_message2));
                dialog.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                dialog.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        MessageStore.getInstance(getActivity()).deleteTree(treeId);
                        setListInDisplayMode();
                        dialog.dismiss();
                    }
                });
                break;
        }

        if(dialog != null){
            AlertDialog alertdialog = dialog.create();
            DialogStyler.styleAndShow(getActivity(), alertdialog);
        }
        checkedMessages.close();
        return super.onOptionsItemSelected(item);
    }

    /** display a notification bar showing how many unread messages there are in the store */
    private void setPendingUnreadMessagesDisplay(){
        long unreadCount = MessageStore.getInstance(getActivity()).getUnreadCount();
        if(newMessagesNotification != null){
            if(unreadCount > 0) {
                String countString = ((unreadCount <= MAX_NEW_MESSAGES_DISPLAY) ? unreadCount +" "+getString(unreadCount > 1 ? R.string.new_messages_notification_desc : R.string.new_message_notification_desc) : "+"+MAX_NEW_MESSAGES_DISPLAY) +"\n("+ ExchangeHistoryTracker.getInstance().getExchangeHistory()+" "+getString(ExchangeHistoryTracker.getInstance().getExchangeHistory() > 1 ? R.string.exchanges : R.string.exchange)+")";

                ((TextView)newMessagesNotification.findViewById(R.id.new_message_notification_desc)).setText(countString);
                newMessagesNotification.setVisibility(View.VISIBLE);
                newMessagesNotification.findViewById(R.id.new_message_notification_btn).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //mark all the messages as read
                        MessageStore.getInstance(getActivity()).setAllAsRead();
                        setPendingUnreadMessagesDisplay();

                        swapCursor();
                        setPendingUnreadMessagesDisplay();
                    }
                });
            } else {
                newMessagesNotification.setVisibility(View.GONE);
            }

        }
    }

    @Override
    public void onResume() {
        super.onResume();

        MessageStore.getInstance(getActivity()).setAllAsRead();
        setPendingUnreadMessagesDisplay();
        swapCursor();

        receiver = new MessageEventReceiver();
        getActivity().registerReceiver(receiver, filter);
    }

    @Override
    public void onPause() {
        super.onPause();

        if(receiver != null){
            getActivity().unregisterReceiver(receiver);
            receiver = null;
        }

        //mark all the messages as read
        MessageStore.getInstance(getActivity()).setAllAsRead();
        setPendingUnreadMessagesDisplay();
        swapCursor();
    }

    @Override
    public void onActivityResult(int reqCode, int resCode, Intent data) {
        super.onActivityResult(reqCode, resCode, data);
        if(resCode == Activity.RESULT_OK) {
            switch (reqCode){
                case REQ_CODE_MESSAGE:
                    //mark all the messages as read
                    MessageStore.getInstance(getActivity()).setAllAsRead();
                    setPendingUnreadMessagesDisplay();

                    swapCursor();
                    setPendingUnreadMessagesDisplay();
                    break;
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(searchView != null) searchView.removeTextChangedListener(this);
        MessageStore.getInstance(getActivity()).setAllAsRead();
    }

    @Override
    public boolean onBackPressed() {
        if(inSelectionMode){
            setListInDisplayMode();
            return true;
        } else if(inSearchMode){
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            if(searchView != null) imm.hideSoftInputFromWindow(searchView.getWindowToken(), 0);
            inSearchMode = false;
            setActionbar();
            return true;
        }
        return false;
    }

    /** Set the the Actionbar search view with the hashtag supplied and run the
     * default search method.
     *
     * @param hashtag The hashtag to search for
     */
    private void searchHashTagFromClick(String hashtag){
        query = hashtag;
        inSearchMode = true;
        setActionbar();

        searchView.clearFocus();
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(searchView.getWindowToken(), 0);
        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }
}