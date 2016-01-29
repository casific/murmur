package org.denovogroup.murmur.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by Liran on 1/14/2016.
 */
public class WelcomeFragmentPage extends Fragment{

    public static final String IMAGE_SRC = "image";

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        int src = getArguments().getInt(IMAGE_SRC);

        View view = inflater.inflate(src, container, false);

        return view;
    }
}
