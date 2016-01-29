package org.denovogroup.murmur.ui;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.readystatesoftware.systembartint.SystemBarTintManager;

import org.denovogroup.murmur.R;
import org.denovogroup.murmur.backend.*;
import org.denovogroup.murmur.backend.SecurityManager;

import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Created by Liran on 12/29/2015.
 */
public class PostActivity extends AppCompatActivity {
    public static final String MESSAGE_BODY = "MESSAGE_BODY";
    public static final String MESSAGE_PARENT = "MESSAGE_PARENT";

    TextView characterCounter;
    private int maxChars = 140;
    private EditText messageBox;
    String messageBody = "";
    String messageParent = null;
    private int timebound = -1;

    LocationManager manager;
    Location myLocation;
    List<String> providers;

    View shareLocationButton;
    View timeboundButton;
    View restrictButton;

    MenuItem send;

    TextWatcher watcher;

    LocationListener listener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            if (myLocation == null) {
                myLocation = location;
            } else {
                pickBestLocation(location, myLocation);
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onProviderDisabled(String provider) {
        }
    };

    @Override
    protected void onCreate(Bundle arg0) {
        super.onCreate(arg0);

        manager = (LocationManager) getSystemService(LOCATION_SERVICE);
        providers = manager.getAllProviders();
        for(String provider : providers) {
            manager.requestLocationUpdates(provider, 0, 0, listener);
        }

        if(getIntent().hasExtra(MESSAGE_BODY)){
            messageBody = getIntent().getStringExtra(MESSAGE_BODY);
        }

        setContentView(R.layout.post_activity);

        Toolbar toolbar = (Toolbar) findViewById(R.id.appToolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_close_dark);
        getSupportActionBar().setTitle(R.string.new_post);

        if(Build.VERSION.SDK_INT >= 19) {
            //toolbar.setPadding(0, getStatusBarHeight(), 0, 0);

            SystemBarTintManager tintManager = new SystemBarTintManager(this);
            tintManager.setStatusBarTintEnabled(true);
            tintManager.setNavigationBarTintEnabled(true);
            tintManager.setTintColor(getResources().getColor(R.color.statusbar_tint));
        }

        if(getIntent().hasExtra(MESSAGE_PARENT)){
            messageParent = getIntent().getStringExtra(MESSAGE_PARENT);
            getSupportActionBar().setTitle(R.string.reply);
        }

        messageBox = (EditText) findViewById(R.id.editText1);

        characterCounter = (TextView) findViewById(R.id.character_count);

        shareLocationButton = findViewById(R.id.action_share_location);
        shareLocationButton.setVisibility((org.denovogroup.murmur.backend.SecurityManager.getCurrentProfile(this).isShareLocation()) ? View.VISIBLE : View.GONE);
        shareLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.setActivated(!v.isActivated());
            }
        });

        InputMethodManager imm = (InputMethodManager)
                getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,
                InputMethodManager.HIDE_IMPLICIT_ONLY);


        restrictButton = findViewById(R.id.action_restricted);
        restrictButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.setActivated(!v.isActivated());
            }
        });

        timeboundButton = findViewById(R.id.action_timebound);
        timeboundButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(v.isActivated()){
                    timebound = -1;
                    v.setActivated(false);
                    return;
                }

                AlertDialog.Builder builder = new AlertDialog.Builder(PostActivity.this);
                builder.setTitle(R.string.timebound_dialog_title);

                final ViewGroup viewGroup = (ViewGroup) getLayoutInflater().inflate(R.layout.progressbar_dialog, null, false);
                builder.setView(viewGroup);
                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        timebound = ((SeekBar) viewGroup.findViewById(R.id.seeker)).getProgress() + 1;
                        timeboundButton.setActivated(timebound > 0);
                        dialog.dismiss();
                    }
                });
                builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.setOnShowListener(new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface dialog) {
                        final SeekBar seekBar = (SeekBar) viewGroup.findViewById(R.id.seeker);
                        seekBar.setMax(239);
                        int initialValue = timebound > 0 ? timebound : SecurityManager.getCurrentProfile(PostActivity.this).getTimeboundPeriod();
                        seekBar.setProgress(initialValue - 1);
                        ((TextView) viewGroup.findViewById(R.id.unit_id)).setText(R.string.hours_short);
                        final EditText seekBarTv = (EditText) viewGroup.findViewById(R.id.seeker_text);
                        seekBarTv.setText(String.valueOf(seekBar.getProgress() + 1));
                        seekBarTv.selectAll();
                        seekBarTv.requestFocus();

                        //InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                        //imm.hideSoftInputFromWindow(seekBarTv.getWindowToken(), 0);

                        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                            @Override
                            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                                if (fromUser) {
                                    seekBarTv.removeTextChangedListener(watcher);
                                    seekBarTv.setText(String.valueOf(progress + 1));
                                    seekBarTv.selectAll();
                                    seekBarTv.requestFocus();
                                    seekBarTv.addTextChangedListener(watcher);
                                }
                            }

                            @Override
                            public void onStartTrackingTouch(SeekBar seekBar) {

                            }

                            @Override
                            public void onStopTrackingTouch(SeekBar seekBar) {

                            }
                        });

                        seekBarTv.addTextChangedListener(watcher = new TextWatcher() {
                            @Override
                            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                            @Override
                            public void onTextChanged(CharSequence s, int start, int before, int count) {
                                int value = 0;
                                final SeekBar seekBar = (SeekBar) viewGroup.findViewById(R.id.seeker);
                                try {
                                    value = Integer.parseInt(s.toString());
                                    if (value > seekBar.getMax() + 1) {
                                        value = seekBar.getMax() + 1;
                                        seekBarTv.setText(String.valueOf(value));
                                        seekBarTv.selectAll();
                                    }
                                    if(value < 1)
                                    {
                                        value = 1;
                                        seekBarTv.setText(String.valueOf(value));
                                        seekBarTv.selectAll();
                                    }
                                } catch (NumberFormatException e){
                                    value = 1;
                                    seekBarTv.setText(String.valueOf(value));
                                    seekBarTv.selectAll();
                                }
                                seekBar.setProgress(value - 1);
                            }

                            @Override
                            public void afterTextChanged(Editable s) {}
                        });
                    }
                });
                dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        messageBox.requestFocus();
                        InputMethodManager imm = (InputMethodManager)
                                getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,
                                InputMethodManager.HIDE_IMPLICIT_ONLY);
                    }
                });

                DialogStyler.styleAndShow(PostActivity.this, dialog);

                // Set title divider color
                int titleDividerId = getResources().getIdentifier("titleDivider", "id", "android");
                View titleDivider = dialog.findViewById(titleDividerId);
                if (titleDivider != null)
                    titleDivider.setBackgroundColor(getResources().getColor(R.color.app_yellow));
            }
        });

        messageBox.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before,
                                      int count) {

                messageBody = s.toString();
                if (characterCounter != null) {
                    int charCount = maxChars - messageBody.length();
                    characterCounter.setText(String.valueOf(charCount));
                    characterCounter.setTextColor(charCount >= 0 ? Color.parseColor("#939393") : Color.RED);
                }

                if (send != null) {
                    send.setEnabled(isTextValid(s.toString()));
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }

        });

        messageBox.setText(messageBody);
        markHashtags(messageBox, messageBox.getText().toString());

        if(send != null) send.setEnabled(isTextValid(messageBody));

        messageBox.setSelection(messageBox.getText().length());
    }

    @Override
    protected void onPause() {
        super.onPause();
        InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        EditText mEditText = (EditText) findViewById(R.id.editText1);
        mgr.hideSoftInputFromWindow(mEditText.getWindowToken(), 0);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.post_message_menu, menu);

        send = menu.findItem(R.id.send);
        send.setEnabled(isTextValid(messageBody));

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.send:
                /**
                 * Stores the text of the TextView in the MessageStore with
                 * default priority 1.0f. Displays a Toast upon completion and
                 * exits the Activity.
                 *
                 * @param v
                 *            The view which is clicked - in this case, the Button.
                 */
                MessageStore messageStore = MessageStore.getInstance(PostActivity.this);
                float trust = 1.0f;
                int priority = 0;
                SecurityProfile currentProfile = org.denovogroup.murmur.backend.SecurityManager.getCurrentProfile(PostActivity.this);
                String pseudonym = currentProfile.isPseudonyms() ?
                        SecurityManager.getCurrentPseudonym(PostActivity.this) : "";
                long timestamp = (currentProfile.isTimestamp() || timebound > 0) ?
                        System.currentTimeMillis() : 0;


                Location myLocation = null;

                if (shareLocationButton.isActivated()) {
                    LocationManager manager = (LocationManager) getSystemService(LOCATION_SERVICE);
                    if (myLocation == null) {
                        for (String provider : providers) {
                            if (myLocation == null) {
                                myLocation = manager.getLastKnownLocation(provider);
                                continue;
                            }
                            myLocation = pickBestLocation(manager.getLastKnownLocation(provider), myLocation);
                        }
                    }

                    if (myLocation == null) {
                        Toast.makeText(PostActivity.this, R.string.no_gps_error, Toast.LENGTH_LONG).show();
                        break;
                    }
                }

                Random random = new Random();
                long idLong = System.nanoTime() * (1 + random.nextInt());
                String messageId = Base64.encodeToString(Crypto.encodeString(String.valueOf(idLong)), Base64.NO_WRAP);

                messageStore.addMessage(PostActivity.this, messageId, messageBody, trust, priority, pseudonym, timestamp, true, TimeUnit.HOURS.toMillis(timebound), myLocation, messageParent, true, restrictButton.isActivated() ? currentProfile.getMinContactsForHop() : 0, 0, null, messageParent);
                Toast.makeText(PostActivity.this, "Message sent!",
                        Toast.LENGTH_SHORT).show();
                ExchangeHistoryTracker.getInstance().cleanHistory(null);
                MessageStore.getInstance(PostActivity.this).updateStoreVersion();

                PostActivity.this.setResult(Activity.RESULT_OK);
                PostActivity.this.finish();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private boolean isTextValid(String text){
        boolean valid = false;
        if(text != null && text.length() <= maxChars && text.length() > 0) {
            for (char c : text.toCharArray()) {
                if ((c != ' ') && (c != '\n')) {
                    valid = true;
                    break;
                }
            }
        }
        return valid;
    }

    private Location pickBestLocation(Location locationA, Location locationB){

        if(locationA == null && locationB == null){
            return null;
        } else if(locationA == null){
            return locationB;
        } else if(locationB == null){
            return locationA;
        }

        int maxTimeDiff = 1000;
        long timeDiff = Math.abs(locationA.getTime() - locationB.getTime());

        if(locationB.getAccuracy() < locationA.getAccuracy()) {
            //B has better accuracy (lower radius)
            // compare time taken to determine if not too old
            if (timeDiff <= maxTimeDiff) return locationB;
        } else {
            //Even though A is more accurate it is too outdated so b is better choice
            if(locationB.getTime() - locationA.getTime() >= maxTimeDiff){
                return locationB;
            }
        }
        return locationA;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        manager.removeUpdates(listener);
    }

    /** set color to any hashtag in the supplied text and assign the text to the supplied
     * text view, clicking a hashtag will call new instance of the activity with intent filter of
     *
     * @param textView
     * @param source
     */
    private void markHashtags(TextView textView, String source){

        String hashtaggedMessage = source;

        String hexColor = String.format("#%06X", (0xFFFFFF & getResources().getColor(R.color.app_purple)));

        Set<String> hashtags = Utils.getHashtags(source);
        for(String hashtag : hashtags){
            String textBefore = hashtaggedMessage.substring(0,hashtaggedMessage.indexOf(hashtag));
            String textAfter = hashtaggedMessage.substring(hashtaggedMessage.indexOf(hashtag)+hashtag.length());
                hashtaggedMessage = textBefore+"<font color="+hexColor+">"+hashtag+"</font>"+textAfter;
        }

        textView.setText(Html.fromHtml(hashtaggedMessage));
    }

    public int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    private void fixCursorPosition(EditText editText){
        if(editText.getText() == null) return;

        boolean isSinglePoint = editText.getSelectionStart() == editText.getSelectionEnd();
        boolean isAppearingAtEnd = editText.getSelectionStart() == 0;
        if(isSinglePoint && isAppearingAtEnd){
            editText.setSelection(editText.length());
        }
    }
}
