package org.denovogroup.murmur.ui;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.readystatesoftware.systembartint.SystemBarTintManager;

import org.denovogroup.murmur.R;
import org.denovogroup.murmur.backend.*;
import org.denovogroup.murmur.backend.SecurityManager;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;

/**
 * Created by Liran on 12/27/2015.
 *
 * The main activity used by the app for most actions and navigational purposes
 */
public class MainActivity extends AppCompatActivity implements DrawerActivityHelper, FeedFragment.FeedFragmentCallbacks {

    public static final String TAG = "MainActivity";

    //setting key
    public static final String PREF_FILE = "settings";
    public static final String WIFI_NAME = "wifiname"; //TODO use this
    public static final String IS_APP_ENABLED = "isEnabled";

    int selectedDrawerItem = R.id.drawer_menu_feed;

    DrawerLayout drawerLayout;
    ViewGroup drawerMenu;
    ViewGroup advancedDrawerMenu;
    ActionBarDrawerToggle drawerToggle;
    View contentHolder;
    Toolbar toolbar;
    CheckBox advancedToggle;

    boolean showAdvanced = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main_activity);

        toolbar = (Toolbar) findViewById(R.id.appToolbar);
        setSupportActionBar(toolbar);

        if (Build.VERSION.SDK_INT >= 19) {
            toolbar.setPadding(0, getStatusBarHeight(), 0, 0);

            SystemBarTintManager tintManager = new SystemBarTintManager(this);
            tintManager.setStatusBarTintEnabled(true);
            tintManager.setNavigationBarTintEnabled(true);
            tintManager.setTintColor(getResources().getColor(R.color.statusbar_tint));
        }

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        toolbar.setTitleTextColor(Color.WHITE);

        drawerLayout = (DrawerLayout) findViewById(R.id.drawerLayout);

        initDrawerMenu();

        contentHolder = findViewById(R.id.mainContent);

        if (savedInstanceState == null) {
            //get any hashtag passed data from previous click events
            Uri data = getIntent().getData();
            getIntent().setData(null);

            Fragment frag = new FeedFragment();
            Bundle args = new Bundle();
            if (data != null) {
                int hashTagIndex = data.toString().indexOf("#");
                if(hashTagIndex > -1) {
                    String hashtag = data.toString().substring(hashTagIndex, data.toString().length());
                    args.putString(FeedFragment.HASHTAG, hashtag);
                }
            }
            frag.setArguments(args);

            getSupportFragmentManager().beginTransaction().replace(contentHolder.getId(), frag).commit();

            View selectedItem = drawerMenu.findViewById(selectedDrawerItem);
            if (selectedItem != null) selectedItem.setActivated(true);
        }

        //start Murmur service if necessary
        SharedPreferences pref = getSharedPreferences(PREF_FILE, MODE_PRIVATE);
        if (pref.getBoolean(IS_APP_ENABLED, true)) {
            Intent startServiceIntent = new Intent(this, MurmurService.class);
            startService(startServiceIntent);
        } else {
            Toast.makeText(this, R.string.offline_mode_toast, Toast.LENGTH_LONG).show();
        }

        if (Intent.ACTION_SEND.equals(getIntent().getAction()) && ("text/plain".equals(getIntent().getType()) || getIntent().getType() == null)) {
            try {
                Intent intent = new Intent(this, PostActivity.class);
                intent.putExtra(PostActivity.MESSAGE_BODY, getIntent().getStringExtra(Intent.EXTRA_TEXT));
                startActivityForResult(intent, FeedFragment.REQ_CODE_MESSAGE);
            } catch (Exception e){}
        }
    }

    private void initDrawerMenu() {
        if (drawerLayout == null) return;
        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.open, R.string.close) {
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                SharedPreferences pref = getSharedPreferences(PREF_FILE, MODE_PRIVATE);
                ((SwitchCompat) drawerMenu.findViewById(R.id.drawer_menu_offline_mode)).setChecked(!pref.getBoolean(IS_APP_ENABLED, true));
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
            }
        };
        drawerLayout.setDrawerListener(drawerToggle);

        drawerMenu = (ViewGroup) findViewById(R.id.drawer_menu);
        advancedDrawerMenu = (ViewGroup) findViewById(R.id.drawer_menu_advanced_list);
        advancedToggle = (CheckBox) findViewById(R.id.drawer_menu_advanced);

        advancedToggle.setChecked(showAdvanced);

        if (Build.VERSION.SDK_INT >= 19) {
            drawerMenu.setPadding(drawerMenu.getPaddingLeft(), (drawerMenu.getPaddingTop() + getStatusBarHeight()), drawerMenu.getPaddingRight(), drawerMenu.getPaddingBottom());
        }

        int childcount = drawerMenu.getChildCount();
        for (int i = 0; i < childcount; i++) {
            View v = drawerMenu.getChildAt(i);
            if (v instanceof TextView) v.setOnClickListener(drawerMenuClickListener);
        }

        int visibility = ((CheckBox) findViewById(R.id.drawer_menu_advanced)).isChecked() ? View.VISIBLE : View.GONE;
        childcount = advancedDrawerMenu.getChildCount();
        for (int i = 0; i < childcount; i++) {
            View child = advancedDrawerMenu.getChildAt(i);
            child.setVisibility(visibility);
            if (child instanceof TextView) child.setOnClickListener(drawerMenuClickListener);
        }

        SharedPreferences pref = getSharedPreferences(PREF_FILE, MODE_PRIVATE);
        ((SwitchCompat) drawerMenu.findViewById(R.id.drawer_menu_offline_mode)).setChecked(!pref.getBoolean(IS_APP_ENABLED, true));

        ((SwitchCompat) drawerMenu.findViewById(R.id.drawer_menu_offline_mode)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences pref = getSharedPreferences(PREF_FILE, MODE_PRIVATE);
                pref.edit().putBoolean(IS_APP_ENABLED, !isChecked).commit();

                Intent serviceIntent = new Intent(MainActivity.this, MurmurService.class);
                if (isChecked) {
                    stopService(serviceIntent);
                } else {
                    startService(serviceIntent);
                }
            }
        });
    }

    /**
     * A click listener to handle all drawer menu items clicks
     */
    private OnClickListener drawerMenuClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {

            switch (v.getId()) {
                case R.id.drawer_menu_share_app:
                case R.id.drawer_menu_export_feed:
                case R.id.drawer_menu_offline_mode:
                case R.id.drawer_menu_reset:

                    // dont change activated state
                    break;
                case R.id.drawer_menu_advanced:
                    // dont change activated state
                    showAdvanced = !showAdvanced;
                    advancedToggle.setChecked(showAdvanced);
                    int visibility = showAdvanced ? View.VISIBLE : View.GONE;
                    int advancedChildcount = advancedDrawerMenu.getChildCount();
                    for (int i = 0; i < advancedChildcount; i++) {
                        View child = advancedDrawerMenu.getChildAt(i);
                        child.setVisibility(visibility);
                    }
                    if (showAdvanced) {
                        final ScrollView scroller = (ScrollView) findViewById(R.id.drawer_menu_scroll);
                        scroller.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                scroller.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                                scroller.smoothScrollTo(0, scroller.getMeasuredHeight());
                            }
                        }, 400);
                    }
                    break;
                default:
                    int childcount = drawerMenu.getChildCount();
                    for (int i = 0; i < childcount; i++) {
                        View child = drawerMenu.getChildAt(i);
                        child.setActivated(child == v);
                    }
                    childcount = advancedDrawerMenu.getChildCount();
                    for (int i = 0; i < childcount; i++) {
                        View child = advancedDrawerMenu.getChildAt(i);
                        child.setActivated(child == v);
                    }
                    break;
            }

            Fragment frag = null;

            switch (v.getId()) {
                case R.id.drawer_menu_contact:
                    frag = new ContactsFragment();
                    break;
                case R.id.drawer_menu_export_feed:
                    exportFeed();
                    break;
                case R.id.drawer_menu_feed:
                    frag = new FeedFragment();
                    break;
                case R.id.drawer_menu_profile:
                    frag = new ProfileFragment();
                    break;
                case R.id.drawer_menu_reset:
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this)
                            .setTitle(R.string.reset_dialog_title)
                            .setMessage(R.string.reset_app_message)
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    resetApplication();
                                    dialog.dismiss();
                                    /*AppDialog.Builder builder2 = new AppDialog.Builder(MainActivity.this)
                                            .setTitle(R.string.confirm_reset_dialog_title)
                                            .setMessage(R.string.confirm_reset_app_message)
                                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    //reset app
                                                    resetApplication();
                                                    dialog.dismiss();
                                                }
                                            })
                                            .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    dialog.dismiss();
                                                }
                                            });
                                    builder2.show();*/
                                }
                            })
                            .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });

                    AlertDialog alertdialog = builder.create();
                    DialogStyler.styleAndShow(MainActivity.this, alertdialog);
                    break;
                case R.id.drawer_menu_settings:
                    frag = new SettingsFragment();
                    break;
                case R.id.drawer_menu_starred:
                    frag = new StarredFragment();
                    break;
                case R.id.drawer_menu_info:
                    frag = new InfoFragment();
                    break;
                case R.id.drawer_menu_share_app:
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("text/plain");
                    shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Sent with Murmur");
                    shareIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_app_promo) + " \n" + getString(R.string.apk_url));
                    startActivity(Intent.createChooser(shareIntent, getString(R.string.share_using)));
                    break;
                case R.id.drawer_menu_help:
                    frag = new HelpFragment();
                    break;
                case R.id.drawer_menu_debug:
                    //TODO debug only
                    frag = new DebugFragment();
                    break;
            }

            if (frag != null &&
                    getSupportFragmentManager().findFragmentById(contentHolder.getId()).getClass() != frag.getClass()) {

                drawerLayout.closeDrawers();

                Spinner spinner = (Spinner) toolbar.findViewById(R.id.sortSpinner);
                if (spinner != null) spinner.setVisibility(View.INVISIBLE);
                getSupportFragmentManager().beginTransaction().replace(contentHolder.getId(), frag).commit();
            }
        }
    };

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (drawerToggle.onOptionsItemSelected(item)) {
            //open or close drawer menu
            return true;
        }

        // Handle your other action bar items
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i(TAG, "Got activity result");
        super.onActivityResult(requestCode, resultCode, data);
        Fragment fragment = getSupportFragmentManager().findFragmentById(contentHolder.getId());
        if (fragment != null && fragment instanceof ContactsFragment) {
            fragment.onActivityResult(requestCode, resultCode, data);
        }
    }

    public Toolbar getToolbar() {
        return toolbar;
    }

    @Override
    public ActionBarDrawerToggle getDrawerToggle() {
        return drawerToggle;
    }

    @Override
    public void onFeedItemExpand(String messageId) {
        Fragment fragment = new ExpandedMessageFragment();
        Bundle args = new Bundle();
        args.putString(ExpandedMessageFragment.MESSAGE_ID_KEY, messageId);
        fragment.setArguments(args);
        getSupportFragmentManager().beginTransaction()
                .replace(contentHolder.getId(), fragment, null).addToBackStack(null).commit();
    }

    private void exportFeed() {

        AsyncTask<Void, Void, Uri> exportTask = new AsyncTask<Void, Void, Uri>() {
            @Override
            protected Uri doInBackground(Void... params) {
                Cursor cursor = MessageStore.getInstance(MainActivity.this).getMessagesCursor(false, false, 1000);
                cursor.moveToFirst();

                int messageColIndex = cursor.getColumnIndex(MessageStore.COL_MESSAGE);
                int timestampColIndex = cursor.getColumnIndex(MessageStore.COL_TIMESTAMP);
                int trustColIndex = cursor.getColumnIndex(MessageStore.COL_TRUST);
                int likesColIndex = cursor.getColumnIndex(MessageStore.COL_LIKES);
                int pseudoColIndex = cursor.getColumnIndex(MessageStore.COL_PSEUDONYM);
                int restrictedColIndex = cursor.getColumnIndex(MessageStore.COL_MIN_CONTACTS_FOR_HOP);
                int locationColIndex = cursor.getColumnIndex(MessageStore.COL_LATLONG);

                File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + File.separator + getString(R.string.export_subdiractory));
                if (!dir.exists()) {
                    dir.mkdirs();
                }

                File file = new File(dir, "export" + Utils.convertTimestampToDateStringCompact(false, System.currentTimeMillis()) + ".csv");

                try {
                    FileWriter fos = new FileWriter(file);
                    BufferedWriter bos = new BufferedWriter(fos);

                    SecurityProfile profile = SecurityManager.getCurrentProfile(MainActivity.this);

                    final String newLine = System.getProperty("line.separator");

                    String colTitlesLine = ""
                            + (profile.isTimestamp() ? "\"" + MessageStore.COL_TIMESTAMP + "\"," : "")
                            + "\"" + MessageStore.COL_TRUST + "\","
                            + "\"" + MessageStore.COL_LIKES + "\","
                            + (profile.isPseudonyms() ? "\"" + MessageStore.COL_PSEUDONYM + "\"," : "")
                            + (profile.isShareLocation() ? "\"" + MessageStore.COL_LATLONG + "\"," : "")
                            + "\"" + MessageStore.COL_MIN_CONTACTS_FOR_HOP + "\","
                            + "\"" + MessageStore.COL_MESSAGE + "\"";

                    bos.write(colTitlesLine);
                    bos.write(newLine);

                    while (!cursor.isAfterLast()) {
                        String line = ""
                                + (profile.isTimestamp() ? "\"" + (Utils.convertTimestampToDateStringCompact(false, cursor.getLong(timestampColIndex)) + "\",") : "")
                                + "\"" + (cursor.getFloat(trustColIndex) * 100) + "\","
                                + "\"" + (cursor.getInt(likesColIndex)) + "\","
                                + (profile.isPseudonyms() ? "\"" + (cursor.getString(pseudoColIndex)) + "\"," : "")
                                + (profile.isShareLocation() ? "\"" + (cursor.getString(locationColIndex) != null ? cursor.getString(locationColIndex) : "") + "\"," : "")
                                + "\"" + (cursor.getInt(restrictedColIndex) > 0) + "\","
                                + "\"" + formatMessageForCSV(cursor.getString(messageColIndex)) + "\"";
                        bos.write(line);
                        bos.write(newLine); //due to a bug in windows notepad text will be displayed as a long string instead of multiline, this is a note-pad specific problem
                        cursor.moveToNext();
                    }
                    cursor.close();
                    bos.flush();
                    bos.close();
                    return Uri.fromFile(file);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }

            AlertDialog dialog;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle(R.string.export);
                builder.setMessage(R.string.export_message);
                builder.setCancelable(false);
                ProgressBar bar = new ProgressBar(MainActivity.this);
                builder.setView(bar);
                dialog = builder.create();
                DialogStyler.styleAndShow(MainActivity.this, dialog);
            }

            @Override
            protected void onPostExecute(Uri fileUri) {
                super.onPostExecute(fileUri);
                if (dialog != null) dialog.dismiss();
                Toast.makeText(MainActivity.this, fileUri != null ? getString(R.string.export_successful) : getString(R.string.export_failed), Toast.LENGTH_LONG).show();

                //force file scanner to scan this file so it will show immediately in storage by pc
                if (fileUri != null) {
                    final Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                    intent.setData(fileUri);
                    getApplicationContext().sendBroadcast(intent);
                }
            }
        };

        exportTask.execute();
    }

    private String formatMessageForCSV(String rawString) {
        String csvString = rawString.replaceAll("[\r\n\"]", " ");
        return csvString;
    }

    @Override
    public void onBackPressed() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(contentHolder.getId());
        if (fragment instanceof FragmentBackHandler) {
            boolean shouldBreak = ((FragmentBackHandler) fragment).onBackPressed();
            if (shouldBreak) return;
        }
        if(fragment instanceof FeedFragment)
            super.onBackPressed();
        else
        {
            Fragment frag = new FeedFragment();
            drawerLayout.closeDrawers();
            Spinner spinner = (Spinner) toolbar.findViewById(R.id.sortSpinner);
            if (spinner != null) spinner.setVisibility(View.INVISIBLE);
            getSupportFragmentManager().beginTransaction().replace(contentHolder.getId(), frag).commit();
        }

    }

    private void resetApplication() {
        MessageStore.getInstance(MainActivity.this).purgeStore();
        FriendStore.getInstance(MainActivity.this).purgeStore();
        org.denovogroup.murmur.backend.SecurityManager.getInstance().clearProfileData(MainActivity.this);

        Intent intent = new Intent(MainActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    public int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    @Override
    protected boolean onPrepareOptionsPanel(View view, Menu menu) {
        if (menu != null) {
            if (menu.getClass().getSimpleName().equals("MenuBuilder")) {
                try {
                    Method m = menu.getClass().getDeclaredMethod(
                            "setOptionalIconsVisible", Boolean.TYPE);
                    m.setAccessible(true);
                    m.invoke(menu, true);
                } catch (NoSuchMethodException e) {
                } catch (Exception e) {
                }
            }
        }
        return super.onPrepareOptionsPanel(view, menu);
    }
}
