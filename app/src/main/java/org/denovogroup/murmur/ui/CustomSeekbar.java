package org.denovogroup.murmur.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.util.AttributeSet;
import android.widget.SeekBar;

import org.denovogroup.murmur.R;

/**
 * Created by Liran on 1/5/2016.
 */
public class CustomSeekbar extends SeekBar {

    int tintColor;

    public CustomSeekbar(Context context) {
        super(context);
        getTintColor(context);
    }

    public CustomSeekbar(Context context, AttributeSet attrs) {
        super(context, attrs);
        getTintColor(context);
    }

    public CustomSeekbar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        getTintColor(context);
    }

    private void getTintColor(Context context){
        tintColor = context.getResources().getColor(R.color.app_yellow);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);



        Drawable thumb = getThumb();
        if (thumb != null) thumb.setColorFilter(isEnabled() ? tintColor : Color.GRAY, PorterDuff.Mode.SRC_ATOP);
        Drawable progDrawable = getProgressDrawable();
        if (progDrawable != null && progDrawable instanceof LayerDrawable) {

            Drawable progClip = ((LayerDrawable) progDrawable).findDrawableByLayerId(android.R.id.progress);
            if (progClip != null) progClip.setColorFilter(isEnabled() ? tintColor : Color.GRAY, PorterDuff.Mode.SRC_ATOP);
        }
    }
}
