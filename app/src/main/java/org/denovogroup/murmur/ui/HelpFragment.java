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
    Map<String, List<String>> data;

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

        //make hot topics

        headers = new ArrayList<>();
        data = new HashMap<>();

        String header1 = getString(R.string.help_title1);
        List<String> body1 = new ArrayList<>();
        body1.add(getString(R.string.help_content1));
        headers.add(header1);
        data.put(header1, body1);

        String header2 = getString(R.string.help_title2);
        List<String> body2 = new ArrayList<>();
        body2.add(getString(R.string.help_content2));
        headers.add(header2);
        data.put(header2, body2);

        String header3 = getString(R.string.help_title3);
        List<String> body3 = new ArrayList<>();
        body3.add(getString(R.string.help_content3));
        headers.add(header3);
        data.put(header3, body3);

        if(partial) return;
        //make all other stuff

        String header4 = getString(R.string.help_title4);
        List<String> body4 = new ArrayList<>();
        body4.add(getString(R.string.help_content4));
        headers.add(header4);
        data.put(header4, body4);

        String header5 = getString(R.string.help_title5);
        List<String> body5 = new ArrayList<>();
        body5.add(getString(R.string.help_content5));
        headers.add(header5);
        data.put(header5, body5);

        String header6 = getString(R.string.help_title6);
        List<String> body6 = new ArrayList<>();
        body6.add(getString(R.string.help_content6));
        headers.add(header6);
        data.put(header6, body6);

        String header7 = getString(R.string.help_title7);
        List<String> body7 = new ArrayList<>();
        body7.add(getString(R.string.help_content7));
        headers.add(header7);
        data.put(header7, body7);

        String header8 = getString(R.string.help_title8);
        List<String> body8 = new ArrayList<>();
        body8.add(getString(R.string.help_content8));
        headers.add(header8);
        data.put(header8, body8);

        String header9 = getString(R.string.help_title9);
        List<String> body9 = new ArrayList<>();
        body9.add(getString(R.string.help_content9));
        headers.add(header9);
        data.put(header9, body9);

        String header10 = getString(R.string.help_title10);
        List<String> body10 = new ArrayList<>();
        body10.add(getString(R.string.help_content10));
        headers.add(header10);
        data.put(header10, body10);

        String header11 = getString(R.string.help_title11);
        List<String> body11 = new ArrayList<>();
        body11.add(getString(R.string.help_content11));
        headers.add(header11);
        data.put(header11, body11);

        String header12 = getString(R.string.help_title12);
        List<String> body12 = new ArrayList<>();
        body12.add(getString(R.string.help_content12));
        headers.add(header12);
        data.put(header12, body12);

        String header13 = getString(R.string.help_title13);
        List<String> body13 = new ArrayList<>();
        body13.add(getString(R.string.help_content13));
        headers.add(header13);
        data.put(header13, body13);

        String header14 = getString(R.string.help_title14);
        List<String> body14 = new ArrayList<>();
        body14.add(getString(R.string.help_content14));
        headers.add(header14);
        data.put(header14, body14);

        String header15 = getString(R.string.help_title15);
        List<String> body15 = new ArrayList<>();
        body15.add(getString(R.string.help_content15));
        headers.add(header15);
        data.put(header15, body15);

        String header16 = getString(R.string.help_title16);
        List<String> body16 = new ArrayList<>();
        body16.add(getString(R.string.help_content16));
        headers.add(header16);
        data.put(header16, body16);

        String header17 = getString(R.string.help_title17);
        List<String> body17 = new ArrayList<>();
        body17.add(getString(R.string.help_content17));
        headers.add(header17);
        data.put(header17, body17);

        String header18 = getString(R.string.help_title18);
        List<String> body18 = new ArrayList<>();
        body18.add(getString(R.string.help_content18));
        headers.add(header18);
        data.put(header18, body18);

        String header19 = getString(R.string.help_title19);
        List<String> body19 = new ArrayList<>();
        body19.add(getString(R.string.help_content19));
        headers.add(header19);
        data.put(header19, body19);
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
