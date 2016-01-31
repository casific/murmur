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
