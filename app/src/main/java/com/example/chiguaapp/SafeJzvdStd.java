package com.example.chiguaapp;

import android.content.Context;
import android.util.AttributeSet;

import cn.jzvd.Jzvd;
import cn.jzvd.JzvdStd;

/**
 * 关闭全屏滑动手势，避免误触右侧音量/左侧亮度逻辑，保持点击控制条与全屏按钮可用。
 */
public class SafeJzvdStd extends JzvdStd {
    public SafeJzvdStd(Context context) {
        super(context);
    }

    public SafeJzvdStd(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void touchActionMove(float x, float y) {
        if (screen == Jzvd.SCREEN_FULLSCREEN) {
            return;
        }
        super.touchActionMove(x, y);
    }
}
