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

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;

import org.denovogroup.murmur.R;
import org.denovogroup.murmur.backend.ConfigureLog4J;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

/**
 * Created by Liran on 1/11/2016.
 */
public class HelpFragment extends Fragment{

    private ExpandableListView listView;

    List<String> headers;
    List<String> data;

    boolean partial = true;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        final View view = inflater.inflate(R.layout.help_fragment, container, false);

        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(R.string.drawer_menu_help);

        listView = (ExpandableListView) view.findViewById(R.id.listView);

        ViewGroup footer = (ViewGroup) LayoutInflater.from(getActivity()).inflate(R.layout.help_list_footer, null, false);
        footer.findViewById(R.id.feedback_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Intent intent = new Intent(getActivity(),FeedbackActivity.class);
                //startActivity(intent);
                openEmailSendingForm(true);
            }
        });

        View more = footer.findViewById(R.id.more_button);
        more.setVisibility(partial ? View.VISIBLE : View.GONE);
        more.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                partial = false;
                prepareHelpContent();
                v.setVisibility(View.GONE);
                listView.setAdapter(new HelpExpandableListAdapter(getActivity(), headers, data));
            }
        });

        listView.addFooterView(footer);

        prepareHelpContent();
        listView.setAdapter(new HelpExpandableListAdapter(getActivity(), headers, data));

        return view;
    }

    private void prepareHelpContent() {
        List<String> tempHeaders = Arrays.asList(getActivity().getResources().getStringArray(R.array.help_content_titles));
        headers = new ArrayList<>(tempHeaders);
        List<String> tempData = Arrays.asList(getActivity().getResources().getStringArray(R.array.help_content_body));
        data = new ArrayList<>(tempData);

        int maxItems = 3;
        if(partial){
            int headerSize = headers.size();
            for(int i= headerSize-1; i>maxItems; i--){
                headers.remove(i);
                data.remove(i);
            }
        }
    }

    private void openEmailSendingForm(boolean includeLog){
        Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", getString(R.string.feedback_email), null));
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.feedback_title));
        intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.feedback_intro));


        /*if(includeLog) {
            File log_filename = new File(Environment.getExternalStorageDirectory() + "/device_log.txt");
            log_filename.delete();

            //get device info
            String userData = "";

            try {
                PackageInfo info = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
                userData += "Application: Ranzgen v" + info.versionName + " (" + info.versionCode + ")\n";
            } catch (PackageManager.NameNotFoundException e) {
            }

            userData += "OS version: " + android.os.Build.VERSION.SDK_INT + "\n";
            userData += "Device: " + android.os.Build.DEVICE + "\n";
            userData += "Model: " + android.os.Build.MODEL + " (" + android.os.Build.PRODUCT + ")\n";

            try {
                log_filename.createNewFile();
                BufferedWriter writer = new BufferedWriter(new FileWriter(log_filename, true));
                writer.write(userData);
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                //get log from device
                String cmd = "logcat -d -f" + log_filename.getAbsolutePath();
                Runtime.getRuntime().exec(cmd);
            } catch (IOException e) {
                e.printStackTrace();
            }

            intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(log_filename));
        }*/

        if(includeLog) {
            File log_filename = new File(Environment.getExternalStorageDirectory() + "/"+ ConfigureLog4J.LOG_FILE);

            //get device info
            String userData = "";

            try {
                PackageInfo info = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
                userData += "Application: Murmur v" + info.versionName + " (" + info.versionCode + ")\n";
            } catch (PackageManager.NameNotFoundException e) {
            }

            userData += "OS version: " + android.os.Build.VERSION.SDK_INT + "\n";
            userData += "Device: " + android.os.Build.DEVICE + "\n";
            userData += "Model: " + android.os.Build.MODEL + " (" + android.os.Build.PRODUCT + ")\n";

            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(log_filename, true));
                writer.write(userData);
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            File zippedLog = new File(Environment.getExternalStorageDirectory() + "/"+ ConfigureLog4J.LOG_FILE+"zip");

            createZipFile(log_filename, zippedLog);

            intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(zippedLog));
        }

        startActivity(Intent.createChooser(intent, "Send mail using..."));
    }

    private void createZipFile(File source, File target) {
        byte[] buffer = new byte[2048];
        try {
            if(target.exists())
            {
                if(!target.delete())
                    Log.i("HelpFragment", "cant remove " + target.getAbsolutePath());
            }
            GZIPOutputStream gzipOutputStream =
                    new GZIPOutputStream(new FileOutputStream(target));
            FileInputStream in =
                    new FileInputStream(source);

            int read;
            while ((read = in.read(buffer)) > 0) {
                gzipOutputStream.write(buffer, 0, read);
            }

            in.close();
            gzipOutputStream.finish();
            gzipOutputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
