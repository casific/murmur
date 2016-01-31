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

import android.bluetooth.BluetoothAdapter;
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
    private View errorView;

    private TextWatcher watcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if(callbacks != null) callbacks.onMacChanged(s.toString().toUpperCase());
            if(errorView != null) errorView.setVisibility(s != null && BluetoothAdapter.checkBluetoothAddress(s.toString().toUpperCase()) ? View.INVISIBLE : View.VISIBLE);
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
            errorView = view.findViewById(R.id.error_message);
        }

        return view;
    }

    private void handleMacInput(View view) {
        input = ((EditText) view.findViewById(R.id.mac_input));
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
    public void onResume() {
        super.onResume();
        if(input != null ) input.addTextChangedListener(watcher);
    }

    @Override
    public void onPause() {
        super.onPause();
        if(input != null ) input.removeTextChangedListener(watcher);
    }
}
