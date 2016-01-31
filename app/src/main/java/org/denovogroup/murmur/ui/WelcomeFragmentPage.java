package org.denovogroup.murmur.ui;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import org.denovogroup.murmur.R;

/**
 * Created by Liran on 1/14/2016.
 */
public class WelcomeFragmentPage extends Fragment{

    public static final String IMAGE_SRC = "image";
    public static final String HANDLE_MAC_INPUT = "mac_input";

    private MacInputCallbacks callbacks;
    private EditText input;

    private TextWatcher watcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if(callbacks != null) callbacks.onMacChanged(s.toString());
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    };

    public interface MacInputCallbacks{
        void onMacChanged(String mac);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        int src = getArguments().getInt(IMAGE_SRC);

        View view = inflater.inflate(src, container, false);

        if(getArguments().containsKey(HANDLE_MAC_INPUT)){
            handleMacInput(view);
        }

        return view;
    }

    private void handleMacInput(View view) {
        input = ((EditText) view.findViewById(R.id.mac_input));
        input.addTextChangedListener(watcher);
        view.findViewById(R.id.check_device_settings).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Intent i = new Intent();
                i.setAction(Intent.ACTION_MAIN);
                i.setComponent(new ComponentName("com.android.settings", "com.android.settings.deviceinfo.Status"));
                startActivity(i);
            }
        });
    }

    public void setCallbacks(MacInputCallbacks callbacks) {
        this.callbacks = callbacks;
    }

    @Override
    public void onPause() {
        super.onPause();
        if(input != null ) input.removeTextChangedListener(watcher);
    }
}
