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
