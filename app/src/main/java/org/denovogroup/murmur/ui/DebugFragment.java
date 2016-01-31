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

import android.app.ActivityManager;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import org.denovogroup.murmur.R;
import org.denovogroup.murmur.backend.*;
import org.denovogroup.murmur.backend.SecurityManager;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

/**
 * Created by Liran on 1/19/2016.
 */
public class DebugFragment extends Fragment {

    Timer refreshTimer;

    ListView listView;
    TextView serviceTV;
    TextView connectingTV;
    TextView seekingTV;
    TextView seekingWasLongAgoTV;

    List<String[]> peers = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        if(BluetoothAdapter.getDefaultAdapter() != null) {
            if(Build.VERSION.SDK_INT >= 23) {
                ((MainActivity) getActivity()).getSupportActionBar().setTitle(SecurityManager.getStoredMAC(getActivity()));
            } else{
                ((MainActivity) getActivity()).getSupportActionBar().setTitle(BluetoothAdapter.getDefaultAdapter().getAddress());
            }
        } else {
            ((MainActivity) getActivity()).getSupportActionBar().setTitle("Incompatible device");
        }

        View view = inflater.inflate(R.layout.debug_fragment, container, false);

        listView = (ListView) view.findViewById(R.id.listView);
        serviceTV = (TextView) view.findViewById(R.id.textView_service);
        connectingTV = (TextView) view.findViewById(R.id.textView_connecting);
        seekingTV = (TextView) view.findViewById(R.id.textView_seeking);
        seekingWasLongAgoTV = (TextView) view.findViewById(R.id.textView_seekingLongAgo);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        if(refreshTimer != null) refreshTimer.cancel();
        refreshTimer = new Timer();
        refreshTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        PeerManager manager = PeerManager.getInstance(getActivity().getApplicationContext());
                        WifiDirectSpeaker wifiDirectSpeaker = WifiDirectSpeaker.getInstance();

                        boolean serviceOn = isMyServiceRunning(MurmurService.class);

                        serviceTV.setText(serviceOn ? "Service running" : "Service offline");
                        MurmurService serviceRef = serviceOn ? MurmurService.getInstance() : null;
                        String connecting = (serviceRef != null) ? serviceRef.getConnecting() : null;
                        serviceRef = null;
                        connectingTV.setText("Connecting to:"+connecting);
                        List<Peer> peersList = manager.getPeers();
                        Boolean seeking = wifiDirectSpeaker.getSeeking();
                        seekingTV.setText("Seeking:"+seeking);
                        Boolean seekingLongAgo = wifiDirectSpeaker.lastSeekingWasLongAgo();
                        seekingWasLongAgoTV.setText("Seeking long ago:"+seekingLongAgo);

                        peers.clear();
                        for (Peer peer : peersList) {
                            String[] peerStr = new String[4];
                            peerStr[0] = peer.toString();
                            try {
                                peerStr[1] = manager.thisDeviceSpeaksTo(peer) ? "true" : null;
                            } catch (UnsupportedEncodingException | NoSuchAlgorithmException e) {
                            }

                            ExchangeHistoryTracker.ExchangeHistoryItem history = ExchangeHistoryTracker.getInstance().getHistoryItem(peer.address);

                            int backoff = 0;

                            String lastExchange = null;
                            if (history != null) {
                                backoff = Math.min(MurmurService.BACKOFF_MAX,
                                        (int) (Math.pow(2, history.getAttempts()) * MurmurService.BACKOFF_FOR_ATTEMPT_MILLIS));

                                lastExchange = String.valueOf(history.getLastExchangeTime());
                            }

                            String backoffString = null;
                            if (backoff > 0 && MurmurService.USE_BACKOFF) {
                                backoffString = TimeUnit.MILLISECONDS.toMinutes(backoff) + ":" + (TimeUnit.MILLISECONDS.toSeconds(backoff) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(backoff)));
                            }

                            peerStr[2] = backoffString;
                            peerStr[3] = lastExchange;
                            peers.add(peerStr);
                        }
                        listView.setAdapter(new DebugAdapter(getActivity(), peers, MurmurService.direction, MurmurService.remoteAddress));
                    }
                });
            }
        }, 1000, 1000);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d("liran","stopping refresh debug timer");
        if(refreshTimer != null) refreshTimer.cancel();
        refreshTimer = null;
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getActivity().getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
