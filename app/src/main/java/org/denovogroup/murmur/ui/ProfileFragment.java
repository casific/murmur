package org.denovogroup.murmur.ui;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import org.denovogroup.murmur.R;
import org.denovogroup.murmur.backend.*;
import org.denovogroup.murmur.backend.SecurityManager;

/**
 * Created by Liran on 12/30/2015.
 *
 * This fragment display the user basic profile features such as the pseudonym
 * and the QR code
 */
public class ProfileFragment extends Fragment implements ActionMode.Callback{

    EditText pseudonym;
    ImageView qrCode;
    ActionMode actionMode;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle(R.string.drawer_menu_profile);

        View view = inflater.inflate(R.layout.profile_fragment, container, false);

        pseudonym = (EditText) view.findViewById(R.id.pseudonym);
        qrCode = (ImageView) view.findViewById(R.id.qr_code);

        pseudonym.setText(SecurityManager.getCurrentPseudonym(getActivity()));
        pseudonym.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    actionMode = ((AppCompatActivity) getActivity()).startSupportActionMode(ProfileFragment.this);
                    actionMode.setCustomView(LayoutInflater.from(getActivity()).inflate(R.layout.actionmode_title, null, false));
                } else {
                    pseudonym.setText(SecurityManager.getCurrentPseudonym(getActivity()));
                    if (actionMode != null) actionMode.finish();
                }
            }
        });

        setupQRCodeDisplay();

        return view;
    }

    private void setupQRCodeDisplay(){
        AsyncTask<Void,Void, Bitmap> setupQRCode = new AsyncTask<Void, Void, Bitmap>() {
            @Override
            protected Bitmap doInBackground(Void... params) {
                return getQRCodeFromPublicId();
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                super.onPostExecute(bitmap);
                if(bitmap != null && qrCode != null){
                    qrCode.setImageBitmap(bitmap);
                    qrCode.invalidate();
                }
            }
        };
        setupQRCode.execute();
    }

    private Bitmap getQRCodeFromPublicId(){
        QRCodeWriter writer = new QRCodeWriter();
        int qrSizeInDp = Utils.dpToPx(250, getActivity());

        try {
            FriendStore store = FriendStore.getInstance(getActivity());
            BitMatrix matrix = writer.encode(getString(R.string.qr_code_prefix)+store.getPublicDeviceIDString(getActivity(), StorageBase.ENCRYPTION_DEFAULT), BarcodeFormat.QR_CODE, qrSizeInDp, qrSizeInDp);
            int width = matrix.getWidth();
            int height = matrix.getHeight();
            int[] pixels = new int[width * height];

            for (int y = 0; y < height; y++)
            {
                int offset = y * width;
                for (int x = 0; x < width; x++)
                {
                    pixels[offset + x] = matrix.get(x, y) ? Color.BLACK : Color.WHITE;
                }
            }

            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height);

            return bitmap;
        } catch (WriterException e) {
            e.printStackTrace();
        }

        return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        mode.getMenuInflater().inflate(R.menu.actionmode_approve_cancel ,menu);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()){
            case R.id.approve:
                String nickname = (pseudonym.getText().length() > 0) ?  pseudonym.getText().toString() : "";
                SecurityManager.setCurrentPseudonym(getActivity(), nickname);
                mode.finish();
                break;
        }
        return false;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        pseudonym.clearFocus();
        InputMethodManager inputMethodManager = (InputMethodManager)  getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        inflater.inflate(R.menu.blank_menu, menu);
    }
}
