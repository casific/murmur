package org.denovogroup.murmur.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.v7.app.AlertDialog;
import android.view.View;

/**
 * Created by Liran on 1/14/2016.
 */
public abstract class DialogStyler {

    public static void styleAndShow(Context context, AlertDialog dialog){
        //change background
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.WHITE));
        //change size
        /*android.view.WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
        params.width = android.view.WindowManager.LayoutParams.MATCH_PARENT;
        dialog.getWindow().setAttributes(params);*/

        dialog.show();

        //attempt to set the divider invisible (same as the bg)
        int titleDividerId = context.getResources().getIdentifier("titleDivider", "id", "android");
        View titleDivider = dialog.findViewById(titleDividerId);
        if (titleDivider != null)
            titleDivider.setBackgroundColor(Color.WHITE);
    }
}
