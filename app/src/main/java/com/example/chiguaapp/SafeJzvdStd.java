package com.example.chiguaapp;

import android.content.Context;
import android.util.AttributeSet;

import cn.jzvd.JZUtils;
import cn.jzvd.Jzvd;
import cn.jzvd.JzvdStd;

/**
 * 保留全屏横向快进/快退，禁用竖向亮度/音量手势，避免右侧误触闪退。
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
        if (screen != Jzvd.SCREEN_FULLSCREEN) {
            super.touchActionMove(x, y);
            return;
        }

        if (mDownX > JZUtils.getScreenWidth(getContext()) || mDownY < JZUtils.getStatusBarHeight(getContext())) {
            return;
        }

        float deltaX = x - mDownX;
        float deltaY = y - mDownY;
        float absDeltaX = Math.abs(deltaX);
        float absDeltaY = Math.abs(deltaY);

        if (!mChangePosition && !mChangeVolume && !mChangeBrightness) {
            if (absDeltaX > THRESHOLD && absDeltaX >= absDeltaY) {
                cancelProgressTimer();
                if (state != STATE_ERROR) {
                    mChangePosition = true;
                    mGestureDownPosition = getCurrentPositionWhenPlaying();
                }
            } else if (absDeltaY > THRESHOLD) {
                // 全屏下禁用竖向亮度/音量手势，避免误触。
                return;
            }
        }

        if (mChangePosition) {
            long totalTimeDuration = getDuration();
            if (PROGRESS_DRAG_RATE <= 0) {
                PROGRESS_DRAG_RATE = 1f;
            }
            mSeekTimePosition = (long) (mGestureDownPosition + deltaX * totalTimeDuration / (mScreenWidth * PROGRESS_DRAG_RATE));
            if (mSeekTimePosition > totalTimeDuration) mSeekTimePosition = totalTimeDuration;
            if (mSeekTimePosition < 0) mSeekTimePosition = 0;
            String seekTime = JZUtils.stringForTime(mSeekTimePosition);
            String totalTime = JZUtils.stringForTime(totalTimeDuration);
            showProgressDialog(deltaX, seekTime, mSeekTimePosition, totalTime, totalTimeDuration);
        }
    }
}
