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
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
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
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.apache.log4j.Logger;
import org.denovogroup.murmur.R;
import org.denovogroup.murmur.backend.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Liran on 12/27/2015.
 *
 * The fragment which display the user contacts overview
 */
public class ContactsFragment extends Fragment implements View.OnClickListener, TextWatcher, FragmentBackHandler{

    private static final String TAG = "ContactsFragment";
    private static final Logger log = Logger.getLogger(TAG);
    private static final int PICK_CONTACT = 100;

    private boolean inSearchMode = false;
    private boolean inSelectionMode = false;
    private boolean selectAll = false;

    private ListView contactListView;

    private Spinner sortSpinner;
    private TextView leftText;
    private sortOption currentSort = sortOption.NEWEST;
    private EditText searchView;

    private String publicKey = "";

    Menu menu;

    private String query = "";

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        query = s.toString();
        ContactAdapter adapter = new ContactAdapter(getActivity(), getCursor(), inSelectionMode);
        if(SearchHelper.searchToSQL(query) == null) {
            adapter.setHighlight(Utils.getKeywords(query));
        }
        contactListView.setAdapter(adapter);
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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(R.string.drawer_menu_contact);

        searchView = (EditText) ((MainActivity)getActivity()).getToolbar().findViewById(R.id.searchView);

        leftText = (TextView) ((MainActivity) getActivity()).getToolbar().findViewById(R.id.leftText);

        initSortSpinner();
        sortSpinner.setVisibility(View.GONE);

        View v = inflater.inflate(R.layout.contact_fragment, container, false);

        contactListView = (ListView) v.findViewById(R.id.listView);

        setListView();
        return v;
    }

    @Override
    public void onClick(View v) {
    }

    private void initSortSpinner(){
        if(getActivity() instanceof MainActivity) {
            sortSpinner = (Spinner) ((MainActivity) getActivity()).getToolbar().findViewById(R.id.sortSpinner);
            /*sortSpinner.setAdapter(new FeedSortSpinnerAdapter(getActivity(), sortOptions, inSearchMode));
            for(int i=0; i<sortOptions.size();i++){
                if(sortOptions.get(i)[2] == currentSort){
                    sortSpinner.setSelection(i);
                    break;
                }
            }*/
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.contact_fragment, menu);
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
        setActionbar();
    }

    private Cursor getCursor(){
        return FriendStore.getInstance(getActivity()).getFriendsCursor(query);
    }

    private void swapCursor(){

        int offsetFromTop = 0;
        int firstVisiblePosition = Math.max(0, contactListView.getFirstVisiblePosition());
        if(contactListView.getChildCount() > 0) {
            offsetFromTop = contactListView.getChildAt(0).getTop();
        }

        CursorAdapter newAdapter = ((CursorAdapter) contactListView.getAdapter());
        newAdapter.swapCursor(getCursor());
        if(SearchHelper.searchToSQL(query) == null) {
            ((ContactAdapter) newAdapter).setHighlight(Utils.getKeywords(query));
        }
        contactListView.setAdapter(newAdapter);

        contactListView.setSelectionFromTop(firstVisiblePosition, offsetFromTop);
    }

    private void setListView(){
        contactListView.setAdapter(new ContactAdapter(getActivity(), getCursor(), inSelectionMode));
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

        ((ContactAdapter)contactListView.getAdapter()).setSelectionMode(inSelectionMode);
        swapCursor();
        contactListView.setOnItemLongClickListener(null);
        contactListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Cursor c = ((CursorAdapter) contactListView.getAdapter()).getCursor();
                c.moveToPosition(position);
                boolean isChecked = c.getInt(c.getColumnIndex(FriendStore.COL_CHECKED)) == FriendStore.TRUE;
                String publicKey = c.getString(c.getColumnIndex(FriendStore.COL_PUBLIC_KEY));

                FriendStore.getInstance(getActivity()).setChecked(publicKey, !isChecked);
                swapCursor();

                int checkedCount = (int) FriendStore.getInstance(getActivity()).getCheckedCount();

                ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(checkedCount <= 99 ? String.valueOf(checkedCount) : "+99");
                updateSelectAll();
                if (menu != null) menu.findItem(R.id.action_delete).setEnabled(checkedCount > 0);

            }
        });
        setActionbar();
    }

    private void setListInDisplayMode(){
        inSelectionMode = false;
        FriendStore.getInstance(getActivity()).setCheckedAll(false, null);
        ((ContactAdapter) contactListView.getAdapter()).setSelectionMode(false);
        contactListView.setOnItemLongClickListener(longClickListener);

        contactListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(final AdapterView<?> parent, View view, int position, long id) {
                final Cursor cursor = ((CursorAdapter) parent.getAdapter()).getCursor();
                cursor.moveToPosition(position);
                publicKey = cursor.getString(cursor.getColumnIndex(FriendStore.COL_PUBLIC_KEY));

                boolean isFromPhone = cursor.getInt(cursor.getColumnIndex(FriendStore.COL_ADDED_VIA)) == FriendStore.ADDED_VIA_PHONE;

                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
                dialogBuilder.setTitle(R.string.conf_friend_dialog_title);
                final View dialogView = getActivity().getLayoutInflater().inflate(isFromPhone ? R.layout.add_friend_from_phonebook_dialog : R.layout.add_friend_from_qr_dialog, null);
                dialogBuilder.setView(dialogView);
                dialogBuilder.setCancelable(false);
                dialogBuilder.setPositiveButton(android.R.string.ok, null);
                dialogBuilder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                final AlertDialog alertdialog = dialogBuilder.create();
                alertdialog.setOnShowListener(new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface dialog) {

                        final EditText nameInput = (EditText) dialogView.findViewById(R.id.name);
                        nameInput.setText(cursor.getString(cursor.getColumnIndex(FriendStore.COL_DISPLAY_NAME)));

                        final EditText numberInput = (EditText) dialogView.findViewById(R.id.phone_number);
                        if (numberInput != null) {
                            numberInput.setText(cursor.getString(cursor.getColumnIndex(FriendStore.COL_NUMBER)));
                        }

                        alertdialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                String name = nameInput.getText().toString();
                                if (name.length() == 0) return;

                                String number = numberInput == null ? null : numberInput.getText().toString();
                                FriendStore.getInstance(getActivity()).editFriend(
                                        publicKey,
                                        name,
                                        number
                                );

                                ((ContactAdapter) parent.getAdapter()).changeCursor(FriendStore.getInstance(getActivity()).getFriendsCursor(query));
                                ((ContactAdapter) parent.getAdapter()).notifyDataSetChanged();
                                alertdialog.dismiss();
                            }
                        });
                    }
                });
                DialogStyler.styleAndShow(getActivity(), alertdialog);
            }
        });
        swapCursor();
        setActionbar();
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
            actionBar.setTitle(inSelectionMode ? R.string.empty_string : (inSearchMode ? R.string.empty_string : R.string.drawer_menu_contact));
        }
        if(menu != null) {
            menu.findItem(R.id.search).setVisible(!inSearchMode && !inSelectionMode);
            menu.findItem(R.id.action_delete).setVisible(inSelectionMode);

            menu.findItem(R.id.action_delete).setEnabled(FriendStore.getInstance(getActivity()).getCheckedCount() > 0);
            menu.findItem(R.id.add_friend).setVisible(!inSelectionMode && !inSearchMode);
        }

        if(searchView != null){
            searchView.setVisibility(inSearchMode && !inSelectionMode ? View.VISIBLE : View.GONE);
            searchView.removeTextChangedListener(this);
            searchView.setText(query);
            if(inSearchMode){
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
            sortSpinner.setVisibility(View.GONE);
            //initSortSpinner();
        }
        if(leftText != null){
            updateSelectAll();
            leftText.setVisibility(inSelectionMode ? View.VISIBLE : View.GONE);
            leftText.setOnClickListener(inSelectionMode ? new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    FriendStore.getInstance(getActivity()).setCheckedAll(!selectAll, query);
                    selectAll = !selectAll;
                    swapCursor();

                    int checkedCount = (int) FriendStore.getInstance(getActivity()).getCheckedCount();
                    ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(checkedCount <= 99 ? String.valueOf(checkedCount) : "+99");
                    updateSelectAll();
                }
            } : null);
        }
    }

    private void updateSelectAll() {
        //TODO: Danielk Should this contain replies as well?
        int checkedCount = (int) FriendStore.getInstance(getActivity()).getCheckedCount();
        int totalCount =  getCursor().getCount();
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

        int checkedCount = (int) FriendStore.getInstance(getActivity()).getCheckedCount();
        AlertDialog.Builder dialog = null;

        switch (item.getItemId()){
            case android.R.id.home:
                if(inSelectionMode) {
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
            case R.id.action_delete:
                dialog = new AlertDialog.Builder(getActivity());
                dialog.setTitle(R.string.delete_dialog_title);
                dialog.setMessage(getString(R.string.delete_dialog_message1) + " " + checkedCount + " " + getString(R.string.delete_dialog_message2));
                dialog.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                dialog.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        FriendStore.getInstance(getActivity()).deleteChecked();
                        setListInDisplayMode();
                        dialog.dismiss();
                    }
                });
                break;
            case R.id.add_friend:
                startAddFriend();
                break;
        }

        if(dialog != null){
            AlertDialog alertdialog = dialog.create();
            DialogStyler.styleAndShow(getActivity(), alertdialog);
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Called whenever any activity launched from this activity exits. For
     * example, this is called when returning from the QR code activity,
     * providing us with the QR code (if any) that was scanned.
     *
     */
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        log.info( "Got activity result back in ContactFragment!");

        // Check whether the activity that returned was the QR code activity,
        // and whether it succeeded.
        IntentResult intentResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
        log.debug( "result " + intentResult);
        if(intentResult != null){
            // Grab the string extra containing the QR code that was scanned.
            final FriendStore fs = FriendStore.getInstance(getActivity());
            String code = intentResult.getContents();
            // Convert the code into a public Murmur ID.
            if(code != null && !code.contains(getString(R.string.qr_code_prefix))){
                Toast.makeText(getActivity(), R.string.qr_badqr_error,Toast.LENGTH_SHORT).show();
                return;
            }
            final byte[] publicIDBytes = intentResult.getRawBytes();

            // Try to add the friend to the FriendStore, if they're not null.
            if (publicIDBytes != null) {
                AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity());
                dialog.setTitle(R.string.conf_friend_dialog_title);
                final View dialogView = getActivity().getLayoutInflater().inflate(R.layout.add_friend_from_qr_dialog, null);
                dialog.setView(dialogView);
                dialog.setCancelable(false);
                dialog.setPositiveButton(android.R.string.ok, null);
                dialog.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                final AlertDialog alertdialog = dialog.create();
                alertdialog.setOnShowListener(new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface dialog) {
                        alertdialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                EditText userInput = (EditText) dialogView.findViewById(R.id.name);
                                String name = userInput.getText().toString();

                                if (name.length() == 0) return;

                                boolean wasAdded = fs.addFriendBytes(name, publicIDBytes, FriendStore.ADDED_VIA_QR, null);

                                log.info( "Now have " + fs.getAllFriends().size()
                                        + " contacts.");
                                if (wasAdded) {
                                    Toast.makeText(getActivity(), R.string.contact_add_conf, Toast.LENGTH_SHORT)
                                            .show();
                                } else {
                                    Toast.makeText(getActivity(), R.string.contact_exist, Toast.LENGTH_SHORT)
                                            .show();

                                }
                                alertdialog.dismiss();
                                query = "";
                                contactListView.setAdapter(new ContactAdapter(getActivity(), FriendStore.getInstance(getActivity()).getFriendsCursor(null), false));
                            }
                        });
                    }
                });
                DialogStyler.styleAndShow(getActivity(), alertdialog);
            } else {
                // This can happen if the URI is well-formed (murmur://<stuff>)
                // but the
                // stuff isn't valid base64, since we get here based on the
                // scheme but
                // not a check of the contents of the URI.
                log.info(
                        "Opener got back a supposed murmur scheme code that didn't process to produce a public id:"
                                + code);
                Toast.makeText(getActivity(), "Invalid Friend Code", Toast.LENGTH_SHORT)
                        .show();
            }
        } else {
            if(requestCode == PICK_CONTACT && resultCode == Activity.RESULT_OK && intent.getData() != null){
                Uri contactItemUri = intent.getData();
                //get cursor to browse contacts
                Cursor contactCursor = getActivity().getContentResolver().query(contactItemUri, null, null, null, null);
                if(contactCursor != null && contactCursor.getCount() > 0){
                    contactCursor.moveToFirst();
                    String contactName = contactCursor.getString(contactCursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                    if(contactCursor.getInt(contactCursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0){
                        String id = contactCursor.getString(contactCursor.getColumnIndex(ContactsContract.Contacts._ID));

                        //get cursor t browse all numbers associated with said contact
                        Cursor phoneCursor = getActivity().getContentResolver().query(
                                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                null,
                                ContactsContract.CommonDataKinds.Phone.CONTACT_ID +" = ?",
                                new String[]{id}, null);

                        if(phoneCursor != null){
                            phoneCursor.moveToFirst();
                            int phonesCount = phoneCursor.getCount();
                            boolean requireReformating = true;

                            boolean wasAdded = false;

                            while(!phoneCursor.isAfterLast()){
                                String unformattedNumber = phoneCursor.getString(phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                                String normalizedNumber = phoneCursor.getString(phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER));

                                String formattedNumber = (normalizedNumber != null && normalizedNumber.length() > 0) ? normalizedNumber : formatNumber(unformattedNumber);
                                String noneNullValue = formattedNumber != null ? formattedNumber : unformattedNumber;

                                if(phonesCount > 1 && phoneCursor.getPosition() < phoneCursor.getCount()-1){
                                    phoneCursor.moveToNext();
                                    String nextFormattedNumber = formatNumber(phoneCursor.getString(phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)));
                                    if(nextFormattedNumber != null && nextFormattedNumber.equals(formattedNumber)){
                                        requireReformating = false;
                                        continue;
                                    }
                                    phoneCursor.moveToPrevious();
                                }

                                byte[] encryptedNumber = Crypto.encodeString(noneNullValue);

                                String adjustedName = contactName;
                                if(phonesCount > 1 && requireReformating){
                                    if(contactName.length()+5 > 25){
                                        adjustedName = adjustedName.substring(0,20)+" "+noneNullValue.substring(Math.max(1,noneNullValue.length()-4));
                                    } else {
                                        adjustedName = adjustedName+" "+noneNullValue.substring(Math.max(1,noneNullValue.length()-4));
                                    }
                                }

                                if(encryptedNumber != null) {
                                    FriendStore fs = FriendStore.getInstance(getActivity());
                                    if(fs.addFriendBytes(adjustedName, encryptedNumber, FriendStore.ADDED_VIA_PHONE, noneNullValue)){
                                        wasAdded = true;
                                    };
                                    log.info("Now have " + fs.getAllFriends().size()
                                            + " contacts.");
                                }

                                requireReformating = true;
                                phoneCursor.moveToNext();
                            }

                            if (wasAdded) {
                                Toast.makeText(getActivity(), R.string.contact_add_conf, Toast.LENGTH_SHORT)
                                        .show();
                            } else {
                                Toast.makeText(getActivity(), R.string.contact_exist, Toast.LENGTH_SHORT)
                                        .show();
                            }
                        }
                        if(phoneCursor != null) contactCursor.close();

                        query = "";
                        contactListView.setAdapter(new ContactAdapter(getActivity(), FriendStore.getInstance(getActivity()).getFriendsCursor(null),false));
                    } else {
                        Toast.makeText(getActivity(), R.string.contact_add_fail, Toast.LENGTH_SHORT).show();
                    }
                }
                if(contactCursor != null) contactCursor.close();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(searchView != null) searchView.removeTextChangedListener(this);
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

    public void startAddFriend(){
        SecurityProfile profile = org.denovogroup.murmur.backend.SecurityManager.getCurrentProfile(getActivity());
        if(profile.isFriendsViaBook() ^ profile.isFriendsViaQR()){
            if(profile.isFriendsViaBook()){
                openPhonebook();
            } else {

                openScanner();
            }
        } else {

            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.add_friend_dialog_title)
                    .setMessage(R.string.add_friend_dialog_body)
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
            if (org.denovogroup.murmur.backend.SecurityManager.getCurrentProfile(getActivity()).isFriendsViaQR()) {
                dialogBuilder.setNeutralButton(R.string.add_friend_qrcode, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        openScanner();
                    }
                });
            }
            if (org.denovogroup.murmur.backend.SecurityManager.getCurrentProfile(getActivity()).isFriendsViaBook()) {
                dialogBuilder.setPositiveButton(R.string.add_friend_phonebook, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        openPhonebook();
                    }
                });
            }
            AlertDialog alertdialog = dialogBuilder.create();
            DialogStyler.styleAndShow(getActivity(), alertdialog);
        }
    }

    private void openScanner(){
        IntentIntegrator integrator = new IntentIntegrator(getActivity());
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES);
        integrator.setPrompt(getString(R.string.qr_scanner_message));
        integrator.setBeepEnabled(false);
        integrator.initiateScan();
    }

    private void openPhonebook(){
        Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
        startActivityForResult(intent, PICK_CONTACT);
    }

    private String formatNumber(String unformattedNumber){
        TelephonyManager tm = (TelephonyManager)getActivity().getSystemService(Context.TELEPHONY_SERVICE);
        String countryCode = "us";//tm.getSimCountryIso();

        String formattedNumber;
        if(Build.VERSION.SDK_INT >= 21) {
            formattedNumber = PhoneNumberUtils.formatNumberToE164(unformattedNumber, countryCode);
        } else {
            formattedNumber = PhoneNumberUtils.formatNumber(unformattedNumber);
        }
        if(formattedNumber == null){
            formattedNumber = unformattedNumber.replaceAll("[-,+]","");
        }
        return formattedNumber;
    }
}