package com.yorhp.luckmoney.fc.WindowManager.view;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;


import androidx.annotation.Nullable;

/**
 * Created by mq on 2018/8/26 下午5:55
 * mqcoder90@gmail.com
 */

public class SmallWindowView extends LinearLayout {

    public SmallWindowView(Context context) {
        this(context, null);
    }

    public SmallWindowView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SmallWindowView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
    }

    /**
     * 设置宽和高的属性
     *
     * @param height
     * @param width
     */
    public void setHW(int height, int width) {
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(width, height);
        setLayoutParams(params);
    }
}
