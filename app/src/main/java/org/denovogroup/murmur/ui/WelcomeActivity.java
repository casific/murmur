package org.denovogroup.murmur.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;

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

    int[] images = new int[]{R.layout.intro_1, R.layout.intro_2, R.layout.intro_3};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.welcome_activity);

        pager = (ViewPager) findViewById(R.id.welcome_pager);
        pagerMarkers = (RadioGroup) findViewById(R.id.pager_ind);

        findViewById(R.id.skip).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToMain();
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
            Fragment frag = new WelcomeFragmentPage();
            Bundle args = new Bundle();
            args.putInt(WelcomeFragmentPage.IMAGE_SRC, images[position]);
            frag.setArguments(args);
            return frag;
        }

        @Override
        public int getCount() {
            return images.length;
        }
    }

    private void setAutopage(){
        if(pagingTimer != null) pagingTimer.cancel();
        pagingTimer = new Timer();
        pagingTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(pager.getCurrentItem() == pager.getAdapter().getCount()-1){
                            goToMain();
                        } else {
                            pager.setCurrentItem(pager.getCurrentItem()+1, true);
                        }
                    }
                });
            }
        }, PAGE_DURATION,PAGE_DURATION);
    }
}
