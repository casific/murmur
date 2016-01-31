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
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;

/**
 * Created by Liran on 1/4/2016.
 *
 * A linear layout which support setEnabled calls
 */
public class DisableableLinearLayout extends LinearLayout {

    boolean enabled = true;
    SparseArray<Boolean> originalState = new SparseArray<>();

    public DisableableLinearLayout(Context context) {
        super(context);
    }

    public DisableableLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DisableableLinearLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setEnabled(boolean enabled){
        if(this.enabled != enabled) {
            if (enabled) {
                //restoreOriginalChildState();
            } else {
                //storeOriginalChildState(this,enabled);
            }
            this.enabled = enabled;
            handleEnabledStateChange();
        }
    }

    private void handleEnabledStateChange(){
        invalidate();
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if(!enabled) {
            //canvas.drawColor(Color.parseColor("#77FFFFFF"));
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return !enabled;
    }

    private void storeOriginalChildState(View view, boolean enable) {
        if(view instanceof EditText || view instanceof Button ||
                view instanceof ImageButton || view instanceof CompoundButton ||
                view instanceof CustomSeekbar || view instanceof SeekBar){

            originalState.put(view.getId(), view.isEnabled());
            view.setEnabled(enable);
            view.setAlpha(enable ? 1f : 0.5f);

        } else if(view instanceof ViewGroup){
            int childCount = ((ViewGroup) view).getChildCount();
            for(int i=0; i<childCount; i++){
                storeOriginalChildState(((ViewGroup) view).getChildAt(i), enable);
            }
        }
    }

    private void restoreOriginalChildState(){
        for(int i=0; i<originalState.size(); i++){
            int id = originalState.keyAt(i);
            View child = findViewById(id);
            boolean enable = originalState.get(id);
            if(child != null){
                child.setEnabled(enable);
                child.setAlpha(enable ? 1f : 0.5f);
            }

        }

        originalState.clear();
    }
}
