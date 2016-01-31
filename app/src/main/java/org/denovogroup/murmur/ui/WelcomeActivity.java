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
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import org.denovogroup.murmur.R;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Liran on 1/6/2016.
 */
public class WelcomeActivity extends AppCompatActivity {

    private static final int AUTO_PAGE_RETURN_DELAY = 6000;
    private static final int PAGE_DURATION = 4000;

    ViewPager pager;
    RadioGroup pagerMarkers;
    Timer pagingTimer;

    String enteredMAC;

    View next;

    int[] images = Build.VERSION.SDK_INT >=23 ? new int[]{R.layout.intro_1, R.layout.intro_2, R.layout.intro_3, R.layout.intro_4} :
            new int[]{R.layout.intro_1, R.layout.intro_2, R.layout.intro_3};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.welcome_activity);

        pager = (ViewPager) findViewById(R.id.welcome_pager);
        pagerMarkers = (RadioGroup) findViewById(R.id.pager_ind);

        next = findViewById(R.id.skip);

        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*if (pagingTimer != null) pagingTimer.cancel();
                pagingTimer = new Timer();
                pagingTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        setAutopage();
                    }
                }, AUTO_PAGE_RETURN_DELAY);*/

                if (Build.VERSION.SDK_INT >= 23 && pager.getCurrentItem() == pager.getAdapter().getCount() - 1) {
                    if(enteredMAC != null && BluetoothAdapter.checkBluetoothAddress(enteredMAC)) {
                        org.denovogroup.murmur.backend.SecurityManager.setStoredMAC(WelcomeActivity.this, enteredMAC);
                        goToMain();
                    } else {
                        next.setEnabled(false);
                        return;
                    }
                }

                changePageRunnable.run();
            }
        });

        /*String prefName = "initializes";
        String prefProperty = "initializes";

        SharedPreferences prefFile = getSharedPreferences(prefName, Context.MODE_PRIVATE);

        if(prefFile.contains(prefProperty)){
            goToMain();
        } else {
            prefFile.edit().putBoolean(prefProperty,true).commit();
            initPaging();
        }*/
        initPaging(); // commented logic was moved to splash activity so if we got this far we need to page

        pager.addOnPageChangeListener(pageChangeListener);
    }

    private void goToMain(){
        if(pagingTimer != null) pagingTimer.cancel();
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void initPaging() {
        for(Integer integer : images){
            RadioButton button = new RadioButton(this);
            button.setButtonDrawable(R.drawable.pager_indicator);
            pagerMarkers.addView(button);
        }

        if(images.length > 0) ((RadioButton) pagerMarkers.getChildAt(0)).setChecked(true);
        FragmentStatePagerAdapter adapter = new PagerAdapter(getSupportFragmentManager());
        pager.setAdapter(adapter);
        pager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                ((RadioButton) pagerMarkers.getChildAt(position)).setChecked(true);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });

        pager.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (pagingTimer != null) pagingTimer.cancel();
                pagingTimer = new Timer();
                pagingTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        setAutopage();
                    }
                }, AUTO_PAGE_RETURN_DELAY);
                return false;
            }
        });

        setAutopage();
    }

    private class PagerAdapter extends FragmentStatePagerAdapter{

        public PagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            WelcomeFragmentPage frag = new WelcomeFragmentPage();
            Bundle args = new Bundle();
            args.putInt(WelcomeFragmentPage.IMAGE_SRC, images[position]);
            if(Build.VERSION.SDK_INT >= 23 && position == getCount()-1){
                args.putBoolean(WelcomeFragmentPage.HANDLE_MAC_INPUT, true);
                frag.setCallbacks(new WelcomeFragmentPage.MacInputCallbacks() {
                    @Override
                    public void onMacChanged(String mac) {
                        enteredMAC = mac;
                        next.setEnabled(
                                pager.getCurrentItem() != getCount() - 1 ||
                                        (enteredMAC != null && BluetoothAdapter.checkBluetoothAddress(enteredMAC.toUpperCase())));
                    }
                });
            }
            frag.setArguments(args);
            return frag;
        }

        @Override
        public int getCount() {
            return images.length;
        }
    }

    private ViewPager.OnPageChangeListener pageChangeListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            if(state == ViewPager.SCROLL_STATE_IDLE) {
                if (Build.VERSION.SDK_INT >= 23 && pager.getCurrentItem() == images.length - 1) {
                    ((TextView)next).setText(R.string.skip_welcome2);
                    next.setEnabled(false);
                } else {
                    ((TextView)next).setText(R.string.skip_welcome);
                    next.setEnabled(true);
                }
            }
        }
    };

    private void setAutopage(){
        /*if(pagingTimer != null) pagingTimer.cancel();
        pagingTimer = new Timer();
        pagingTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(changePageRunnable);
            }
        }, PAGE_DURATION,PAGE_DURATION);*/
    }

    private Runnable changePageRunnable = new Runnable() {
        @Override
        public void run() {
            if(pager.getCurrentItem() == pager.getAdapter().getCount()-1){
                if(Build.VERSION.SDK_INT < 23){
                    goToMain();
                }
            } else {
                pager.setCurrentItem(pager.getCurrentItem()+1, true);
            }
        }
    };
}
