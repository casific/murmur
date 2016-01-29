package org.denovogroup.murmur.ui;

import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.denovogroup.murmur.R;

/**
 * Created by Liran on 1/11/2016.
 */
public class InfoFragment extends Fragment{

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(R.string.drawer_menu_info);

        ((AppCompatActivity) getActivity()).getSupportActionBar().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.app_dark_purple)));

        View v = inflater.inflate(R.layout.info_fragment, container, false);

        String version = "0.0";
        try {
            version = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(),0).versionName +" ("+getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(),0).versionCode+")";
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        ((TextView) v.findViewById(R.id.version)).setText("v"+version);

        return v;
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
