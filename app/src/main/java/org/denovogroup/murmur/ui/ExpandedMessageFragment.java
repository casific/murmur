package org.denovogroup.murmur.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;

import org.denovogroup.murmur.R;
import org.denovogroup.murmur.backend.MessageStore;
import org.denovogroup.murmur.backend.SearchHelper;
import org.denovogroup.murmur.backend.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Liran on 12/30/2015.
 *
 * A fragment which show a parent message along with a list of all the replies associated with this
 * parent message.
 * when creating this fragment MESSAGE_ID_KEY must be passed as an argument with the messageId of
 * the parent message
 */
public class ExpandedMessageFragment extends Fragment implements TextWatcher, FragmentBackHandler{

    public static final String MESSAGE_ID_KEY = "messageId";

    private static final int REQ_CODE_MESSAGE = 100;

    String messageId;
    String sender;

    ListView parentMessage;
    ListView listView;
    Spinner sortSpinner;

    private boolean inSearchMode = false;
    private EditText searchView;

    Menu menu;

    private String query = "";

    private enum sortOption{
        NEWEST, OLDEST, MOST_ENDORSED, LEAST_ENDORSED, MOST_CONNECTED, LEAST_CONNECTED
    }

    private sortOption currentSort = sortOption.NEWEST;

    private List<Object[]> sortOptions = new ArrayList<Object[]>(){{
        add(new Object[]{R.drawable.sort_spinner_newest, R.string.sort_opt_newest, sortOption.NEWEST});
        add(new Object[]{R.drawable.sort_spinner_oldest,R.string.sort_opt_oldest, sortOption.OLDEST});
        add(new Object[]{R.drawable.sort_spinner_most_endorsed,R.string.sort_opt_mostendorsed, sortOption.MOST_ENDORSED});
        add(new Object[]{R.drawable.sort_spinner_least_endorsed,R.string.sort_opt_leastendorsed, sortOption.LEAST_ENDORSED});
        add(new Object[]{R.drawable.sort_spinner_most_connected,R.string.sort_opt_mostconnected, sortOption.MOST_CONNECTED});
        add(new Object[]{R.drawable.sort_spinner_least_connected,R.string.sort_opt_leastconnected, sortOption.LEAST_CONNECTED});
    }};

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
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

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        ((DrawerActivityHelper) getActivity()).getDrawerToggle().setDrawerIndicatorEnabled(false);
        ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle(R.string.comments);

        messageId = getArguments().getString(MESSAGE_ID_KEY);

        View view = inflater.inflate(R.layout.expanded_message_fragment, container,false);

        Cursor message = MessageStore.getInstance(getActivity()).getMessageById(messageId);
        message.moveToFirst();
        sender = message.getString(message.getColumnIndex(MessageStore.COL_PSEUDONYM));
        message.close();

        parentMessage = (ListView) view.findViewById(R.id.expanded_item);
        parentMessage.setAdapter(new FeedAdapter(getActivity(), MessageStore.getInstance(getActivity()).getMessageById(messageId), false, feedAdapterCallbacks));

        listView = (ListView) view.findViewById(R.id.listView);
        listView.setAdapter(new FeedReplyAdapter(getActivity(), getCursor()));

        searchView = (EditText) ((MainActivity) getActivity()).getToolbar().findViewById(R.id.searchView);

        view.findViewById(R.id.new_post_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), PostActivity.class);
                intent.putExtra(PostActivity.MESSAGE_PARENT, messageId);
                if(sender != null && sender.length() > 0) intent.putExtra(PostActivity.MESSAGE_BODY, "@"+sender+" ");
                startActivityForResult(intent, REQ_CODE_MESSAGE);
            }
        });

        initSortSpinner();

        return view;
    }

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
            Intent intent = new Intent(getActivity(), PostActivity.class);
            intent.putExtra(PostActivity.MESSAGE_PARENT, parentId);
            if(sender != null && sender.length() > 0) intent.putExtra(PostActivity.MESSAGE_BODY, "@"+sender+" ");
            startActivityForResult(intent, REQ_CODE_MESSAGE);
        }

        @Override
        public void onFavorite(String message, boolean isFavoriteBefore) {
            MessageStore.getInstance(getActivity()).favoriteMessage(
                    message,
                    !isFavoriteBefore);
            swapCursor();
        }

        private void swapCursor(){
            parentMessage.setAdapter(new FeedAdapter(getActivity(), MessageStore.getInstance(getActivity()).getMessageById(messageId), false, feedAdapterCallbacks));
        }
    };

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode == Activity.RESULT_OK){
            switch(requestCode){
                case REQ_CODE_MESSAGE:
                    swapCursor();
                    break;
            }
        }
    }

    private void swapCursor(){

        int offsetFromTop = 0;
        int firstVisiblePosition = Math.max(0, listView.getFirstVisiblePosition());
        if(listView.getChildCount() > 0) {
            offsetFromTop = listView.getChildAt(0).getTop();
        }

        CursorAdapter newAdapter = ((CursorAdapter) listView.getAdapter());
        newAdapter.swapCursor(getCursor());
        if(SearchHelper.searchToSQL(query) == null) {
            ((FeedReplyAdapter) newAdapter).setHighlight(Utils.getKeywords(query));
        }
        listView.setAdapter(newAdapter);

        listView.setSelectionFromTop(firstVisiblePosition, offsetFromTop);
        parentMessage.setAdapter(new FeedAdapter(getActivity(), MessageStore.getInstance(getActivity()).getMessageById(messageId), false, feedAdapterCallbacks));
    }

    private void initSortSpinner(){
        if(getActivity() instanceof MainActivity) {
            sortSpinner = (Spinner) ((MainActivity) getActivity()).getToolbar().findViewById(R.id.sortSpinner);
            sortSpinner.setVisibility(View.VISIBLE);
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

                    swapCursor();
                    listView.setSelectionFromTop(0,0);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    // do nothing
                }
            });
        }
    }

    public void setSearchView(){
        if(searchView == null) return;

        if(getActivity() instanceof DrawerActivityHelper){
            ((DrawerActivityHelper) getActivity()).getDrawerToggle().setDrawerIndicatorEnabled(!inSearchMode);
        }

        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if(actionBar != null) {
            actionBar.setBackgroundDrawable(new ColorDrawable(getActivity().getResources().getColor(inSearchMode ? android.R.color.white : R.color.app_purple)));
            actionBar.setTitle(inSearchMode ? R.string.empty_string : R.string.comments);

            if(inSearchMode) actionBar.setHomeAsUpIndicator(R.drawable.ic_close_dark);
        }

        if(menu != null){
            menu.findItem(R.id.search).setVisible(!inSearchMode);
        }

        initSortSpinner();

        searchView.setVisibility(inSearchMode ? View.VISIBLE : View.GONE);
        searchView.setText(query);
        if(inSearchMode){
            searchView.addTextChangedListener(this);
            searchView.requestFocus();
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(searchView, InputMethodManager.SHOW_IMPLICIT);
        } else {
            query = "";
            searchView.removeTextChangedListener(this);
            searchView.setText("");
        }

        //reset the list to its normal state
        swapCursor();
    }

    private Cursor getCursor(){
        String sqlQuery = SearchHelper.searchToSQL(query);
        return (sqlQuery != null) ?
                MessageStore.getInstance(getActivity()).getCommentsByQuery(messageId, sqlQuery) :
                MessageStore.getInstance(getActivity()).getCommentsContaining(messageId, query);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        query = "";
        inSearchMode = false;
        setSearchView();
        if(searchView != null) searchView.removeTextChangedListener(this);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        query = s.toString();
        FeedReplyAdapter adapter = new FeedReplyAdapter(getActivity(), getCursor());
        if(SearchHelper.searchToSQL(query) == null) {
            adapter.setHighlight(Utils.getKeywords(query));
        }
        listView.setAdapter(adapter);
    }

    @Override
    public void afterTextChanged(Editable s) {}

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (inSearchMode) {
                    query = "";
                    InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (searchView != null)
                        imm.hideSoftInputFromWindow(searchView.getWindowToken(), 0);
                    inSearchMode = false;
                    setSearchView();
                    return true;
                } else {
                    getActivity().getSupportFragmentManager().popBackStack();
                }
                break;
            case R.id.search:
                inSearchMode = true;
                setSearchView();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if(getActivity() instanceof DrawerActivityHelper){
            ((DrawerActivityHelper) getActivity()).getDrawerToggle().setDrawerIndicatorEnabled(true);
        }
    }

    @Override
    public boolean onBackPressed() {
        if (inSearchMode) {
            query = "";
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (searchView != null)
                imm.hideSoftInputFromWindow(searchView.getWindowToken(), 0);
            inSearchMode = false;
            setSearchView();
            return true;
        }
        return false;
    }
}
