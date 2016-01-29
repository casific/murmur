package org.denovogroup.murmur.ui;

import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.SeekBar;

import org.denovogroup.murmur.R;
import org.denovogroup.murmur.backend.*;
import org.denovogroup.murmur.backend.SecurityManager;

/**
 * Created by Liran on 1/4/2016.
 *
 * A fragment allowing user to set its own settings
 */
public class SettingsFragment extends Fragment implements SeekBar.OnSeekBarChangeListener, RadioGroup.OnCheckedChangeListener, CompoundButton.OnCheckedChangeListener, TextWatcher {

    SecurityProfile currentProfile;

    View generalBlock;
    View networkBlock;
    View feedBlock;
    View contactsBlock;
    View exchangeBlock;

    RadioGroup profilesGroup;
    SwitchCompat timestampSwitch;
    SwitchCompat trustSwitch;
    SwitchCompat locationSwitch;
    SwitchCompat pseudonymSwitch;

    EditText selfDestEditText;
    EditText restrictedEditText;
    //EditText maxFeedEditText;
    SwitchCompat autodelSwitch;
    SeekBar autodelTrustSeekbar;
    EditText autodelTrustEditText;
    SeekBar autodelAgeSeekbar;
    EditText autodelAgeEditText;
    SwitchCompat addViaPhoneSwitch;
    SwitchCompat addViaQRSwitch;
    EditText maxMessagesPerExchangeEditText;
    EditText timeoutEditText;
    EditText macEditText;
    private Button deviceSettings;

    int autodelMaxTrust = 100;
    int autodelMaxAge = 365;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(R.string.drawer_menu_settings);
        ((AppCompatActivity) getActivity()).getSupportActionBar().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.app_dark_purple)));

        View view = inflater.inflate(R.layout.settings_fragment, container,false);

        currentProfile = org.denovogroup.murmur.backend.SecurityManager.getCurrentProfile(getActivity());

        generalBlock = view.findViewById(R.id.general_block);
        networkBlock = view.findViewById(R.id.network_block);
        networkBlock.setVisibility((Build.VERSION.SDK_INT >= 23) ? View.VISIBLE : View.GONE);
        feedBlock = view.findViewById(R.id.feed_block);
        contactsBlock = view.findViewById(R.id.contacts_block);
        exchangeBlock = view.findViewById(R.id.exchange_block);

        //enableBlocks();

        profilesGroup = (RadioGroup) view.findViewById(R.id.radiogroup_profiles);
        timestampSwitch = (SwitchCompat) view.findViewById(R.id.switch_timestamp);
        trustSwitch = (SwitchCompat) view.findViewById(R.id.switch_trust);
        locationSwitch = (SwitchCompat) view.findViewById(R.id.switch_location_tagging);
        pseudonymSwitch = (SwitchCompat) view.findViewById(R.id.switch_pseudonym);
        selfDestEditText = (EditText) view.findViewById(R.id.edit_self_dest);
        restrictedEditText = (EditText) view.findViewById(R.id.edit_restricted);
        //maxFeedEditText = (EditText) view.findViewById(R.id.edit_max_messages);
        autodelSwitch = (SwitchCompat) view.findViewById(R.id.switch_auto_delete);
        autodelTrustSeekbar = (SeekBar) view.findViewById(R.id.seekbar_autodelete_trust);
        autodelTrustSeekbar.setMax(autodelMaxTrust);
        autodelAgeSeekbar = (SeekBar) view.findViewById(R.id.seekbar_autodelete_age);
        autodelAgeSeekbar.setMax(autodelMaxAge);
        autodelTrustEditText = (EditText) view.findViewById(R.id.edit_autodelete_trust);
        autodelAgeEditText = (EditText) view.findViewById(R.id.edit_autodelete_age);
        addViaPhoneSwitch = (SwitchCompat) view.findViewById(R.id.switch_add_via_phone);
        addViaQRSwitch = (SwitchCompat) view.findViewById(R.id.switch_add_via_qr);
        maxMessagesPerExchangeEditText = (EditText) view.findViewById(R.id.edit_max_messages_p_exchange);
        timeoutEditText = (EditText) view.findViewById(R.id.edit_timeout_p_exchange);
        macEditText = (EditText) view.findViewById(R.id.edit_mac);
        deviceSettings = (Button) view.findViewById(R.id.device_settings);

        deviceSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Intent i = new Intent();
                i.setAction(Intent.ACTION_MAIN);
                i.setComponent(new ComponentName("com.android.settings", "com.android.settings.deviceinfo.Status"));
                startActivity(i);
            }
        });

        setupView();

        return view;
    }

    private void setupView(){
        timeoutEditText.removeTextChangedListener(this);
        autodelTrustEditText.removeTextChangedListener(this);
        autodelAgeEditText.removeTextChangedListener(this);
        selfDestEditText.removeTextChangedListener(this);
        //maxFeedEditText.removeTextChangedListener(this);
        maxMessagesPerExchangeEditText.removeTextChangedListener(this);
        restrictedEditText.removeTextChangedListener(this);
        macEditText.removeTextChangedListener(this);

        profilesGroup.setOnCheckedChangeListener(null);
        profilesGroup.check(currentProfile.getName());
        profilesGroup.setOnCheckedChangeListener(this);

        autodelTrustSeekbar.setOnSeekBarChangeListener(null);
        autodelTrustSeekbar.setProgress(Math.round(autodelMaxTrust * currentProfile.getAutodeleteTrust()));
        autodelTrustEditText.setText(String.valueOf(Math.round(autodelMaxTrust * currentProfile.getAutodeleteTrust())));
        autodelTrustEditText.addTextChangedListener(this);
        autodelTrustSeekbar.setOnSeekBarChangeListener(this);

        autodelAgeSeekbar.setOnSeekBarChangeListener(null);
        autodelAgeSeekbar.setProgress(currentProfile.getAutodeleteAge());
        autodelAgeEditText.setText(String.valueOf(currentProfile.getAutodeleteAge()));
        autodelAgeEditText.addTextChangedListener(this);
        autodelAgeSeekbar.setOnSeekBarChangeListener(this);

        timestampSwitch.setOnCheckedChangeListener(null);
        timestampSwitch.setChecked(currentProfile.isTimestamp());
        timestampSwitch.setOnCheckedChangeListener(this);

        trustSwitch.setOnCheckedChangeListener(null);
        trustSwitch.setChecked(currentProfile.isUseTrust());
        trustSwitch.setOnCheckedChangeListener(this);

        locationSwitch.setOnCheckedChangeListener(null);
        locationSwitch.setChecked(currentProfile.isShareLocation());
        locationSwitch.setOnCheckedChangeListener(this);

        pseudonymSwitch.setOnCheckedChangeListener(null);
        pseudonymSwitch.setChecked(currentProfile.isPseudonyms());
        pseudonymSwitch.setOnCheckedChangeListener(this);

        selfDestEditText.setText(String.valueOf(currentProfile.getTimeboundPeriod()));
        selfDestEditText.addTextChangedListener(this);

        restrictedEditText.setText(String.valueOf(currentProfile.getMinContactsForHop()));
        restrictedEditText.addTextChangedListener(this);

        //maxFeedEditText.setText(String.valueOf(currentProfile.getFeedSize()));
        //maxFeedEditText.addTextChangedListener(this);

        autodelTrustEditText.setEnabled(currentProfile.isAutodelete());
        autodelAgeEditText.setEnabled(currentProfile.isAutodelete());
        autodelTrustSeekbar.setEnabled(currentProfile.isAutodelete());
        autodelAgeSeekbar.setEnabled(currentProfile.isAutodelete());

        addViaPhoneSwitch.setOnCheckedChangeListener(null);
        addViaPhoneSwitch.setChecked(currentProfile.isFriendsViaBook());
        addViaPhoneSwitch.setOnCheckedChangeListener(this);

        addViaQRSwitch.setOnCheckedChangeListener(null);
        addViaQRSwitch.setChecked(currentProfile.isFriendsViaQR());
        addViaQRSwitch.setOnCheckedChangeListener(this);

        maxMessagesPerExchangeEditText.setText(String.valueOf(currentProfile.getMaxMessages()));
        maxMessagesPerExchangeEditText.addTextChangedListener(this);

        timeoutEditText.setText(String.valueOf(currentProfile.getCooldown()));
        timeoutEditText.addTextChangedListener(this);

        String mac = SecurityManager.getStoredMAC(getActivity());
        if(mac != null && mac.length() > 0){
            macEditText.setText(mac);
            macEditText.setTextColor(getResources().getColor(BluetoothAdapter.checkBluetoothAddress(mac) ? android.R.color.black : android.R.color.holo_red_dark));
        } else {
            macEditText.clearComposingText();
            macEditText.setTextColor(getResources().getColor(android.R.color.black));
        }
        macEditText.addTextChangedListener(this);

    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        switch (seekBar.getId()){
            case R.id.seekbar_autodelete_trust:
                if(fromUser){
                    currentProfile.setAutodeleteTrust(progress/100f);
                    autodelTrustEditText.removeTextChangedListener(this);
                    autodelTrustEditText.setText(String.valueOf(progress));
                    autodelTrustEditText.addTextChangedListener(this);
                }
                break;
            case R.id.seekbar_autodelete_age:
                if(fromUser) {
                    currentProfile.setAutodeleteAge(progress);
                    autodelAgeEditText.removeTextChangedListener(this);
                    autodelAgeEditText.setText(String.valueOf(progress));
                    autodelAgeEditText.addTextChangedListener(this);
                }
                break;
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {}

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {}

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        boolean isCustom = true;
        switch(checkedId){
            case R.id.radio_profile_strict:
                SecurityManager.setCurrentProfile(getActivity(), SecurityManager.getInstance().getProfile(R.id.radio_profile_strict));
                currentProfile = SecurityManager.getCurrentProfile(getActivity());
                isCustom = false;
                break;
            case R.id.radio_profile_flexible:
                SecurityManager.setCurrentProfile(getActivity(), SecurityManager.getInstance().getProfile(R.id.radio_profile_flexible));
                currentProfile = SecurityManager.getCurrentProfile(getActivity());
                isCustom = false;
                break;
            case R.id.radio_profile_custom:
                // do nothing
                break;
        }

        if(isCustom){
            currentProfile.setName(R.id.radio_profile_custom);
            SecurityManager.setCurrentProfile(getActivity(),currentProfile);
        }

        //enableBlocks();

        setupView();
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch(buttonView.getId()){
            case R.id.switch_timestamp:
                currentProfile.setTimestamp(isChecked);
                break;
            case R.id.switch_trust:
                currentProfile.setUseTrust(isChecked);
                break;
            case R.id.switch_location_tagging:
                currentProfile.setShareLocation(isChecked);
                break;
            case R.id.switch_pseudonym:
                currentProfile.setPseudonyms(isChecked);
                break;
            case R.id.switch_auto_delete:
                currentProfile.setAutodelete(isChecked);
                break;
            case R.id.switch_add_via_phone:
                currentProfile.setFriendsViaBook(isChecked);
                if(!isChecked && !currentProfile.isFriendsViaQR()){
                    currentProfile.setFriendsViaQR(true);
                }
                break;
            case R.id.switch_add_via_qr:
                currentProfile.setFriendsViaQR(isChecked);
                if(!isChecked && !currentProfile.isFriendsViaBook()){
                    currentProfile.setFriendsViaBook(true);
                }
                break;
        }

        currentProfile.setName(R.id.radio_profile_custom);
        SecurityManager.setCurrentProfile(getActivity(), currentProfile);

        setupView();
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        View v = getActivity().getCurrentFocus();
        if(v == null) return;

        boolean reload = true;

        int valueAsInt = 0;
        try{
            valueAsInt = Integer.parseInt(s.toString());
        } catch (NumberFormatException e){}

        switch(v.getId()){
            case R.id.edit_autodelete_age:
                if(valueAsInt > autodelMaxAge){
                    valueAsInt = autodelMaxAge;
                    autodelAgeEditText.setText(String.valueOf(valueAsInt));
                    autodelAgeEditText.selectAll();
                }
                autodelAgeSeekbar.setProgress(valueAsInt);
                currentProfile.setAutodeleteAge(valueAsInt);
                break;
            case R.id.edit_autodelete_trust:
                if(valueAsInt > autodelMaxTrust){
                    valueAsInt = autodelMaxAge;
                    autodelTrustEditText.setText(String.valueOf(valueAsInt));
                    autodelAgeEditText.selectAll();
                }
                autodelAgeSeekbar.setProgress(valueAsInt);
                currentProfile.setAutodeleteTrust(valueAsInt / 100f);
                break;
            case R.id.edit_self_dest:
                currentProfile.setTimeboundPeriod(valueAsInt);
                break;
            case R.id.edit_restricted:
                if(valueAsInt > 50){
                    valueAsInt = 50;
                    restrictedEditText.setText(String.valueOf(valueAsInt));
                    restrictedEditText.selectAll();
                }
                currentProfile.setMinContactsForHop(Math.max(1,valueAsInt));
                break;
            /*case R.id.edit_max_messages:
                currentProfile.setFeedSize(valueAsInt);
                break;*/
            case R.id.edit_max_messages_p_exchange:
                currentProfile.setMaxMessages(Math.max(1, valueAsInt));
                break;
            case R.id.edit_timeout_p_exchange:
                currentProfile.setCooldown(valueAsInt);
                break;
            case R.id.edit_mac:
                String mac = s != null ? s.toString() : "";
                SecurityManager.setStoredMAC(getActivity(), mac);
                break;
            default:
                reload = false;
                break;
        }

        if(reload) {
            currentProfile.setName(R.id.radio_profile_custom);
            SecurityManager.setCurrentProfile(getActivity(), currentProfile);

            setupView();
        }

        if(v instanceof EditText) fixCursorPosition((EditText) v);
    }

    @Override
    public void afterTextChanged(Editable s) {
    }

    private void enableBlocks(){
        boolean enable = currentProfile.getName() == R.id.radio_profile_custom;

        generalBlock.setEnabled(enable);
        feedBlock.setEnabled(enable);
        contactsBlock.setEnabled(enable);
        exchangeBlock.setEnabled(enable);
    }

    private void fixCursorPosition(EditText editText){
        if(editText.getText() == null) return;

        boolean isSinglePoint = editText.getSelectionStart() == editText.getSelectionEnd();
        boolean isAppearingAtEnd = editText.getSelectionStart() == 0;
        if(isSinglePoint && isAppearingAtEnd){
            editText.setSelection(editText.length());
        }
    }

    @Override
    public void onDestroyView() {
        Drawable actionbarBg;
        if(Build.VERSION.SDK_INT >= 21){
            actionbarBg = getResources().getDrawable(R.drawable.actionbar_default_bg, null);
        } else {
            actionbarBg = getResources().getDrawable(R.drawable.actionbar_default_bg);
        }
        ((AppCompatActivity) getActivity()).getSupportActionBar().setBackgroundDrawable(actionbarBg);
        super.onDestroyView();
    }
}
