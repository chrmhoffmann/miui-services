package com.android.server.multiwin.animation;

import android.animation.ObjectAnimator;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import com.android.server.multiwin.MiuiMultiWinUtils;
import com.android.server.multiwin.animation.interpolator.SharpCurveInterpolator;
import com.android.server.wm.MiuiFreeformPinManagerService;
/* loaded from: classes.dex */
public class MiuiMultiWinSplitBarController {
    private static final String TAG = "MiuiMultiWinSplitBarController";
    public static boolean mIsLandScape;
    public static View mSplitBar;
    private float mLastToAlpha;
    private int mOriginalMargin;
    private ObjectAnimator mSplitBarAlphaAnimator;
    private int mSwapMargin;

    public MiuiMultiWinSplitBarController(View splitBar, boolean isLandScape) {
        mSplitBar = splitBar;
        mIsLandScape = isLandScape;
    }

    public void hideSplitBar() {
        mSplitBar.setAlpha(MiuiFreeformPinManagerService.EDGE_AREA);
    }

    public void hideSplitBarWithAnimation() {
        playSplitBarAnimation(MiuiFreeformPinManagerService.EDGE_AREA, 20L);
    }

    private void playSplitBarAnimation(float toAlpha, long duration) {
        if (mSplitBar == null) {
            Log.w(TAG, "playSplitBarAnimation failed, cause mSplitBar is null!");
        } else if (MiuiMultiWinUtils.floatEquals(this.mLastToAlpha, toAlpha)) {
            Log.i(TAG, "don't need to start mSplitBarAlphaAnimator, cause mLastToAlpha is equal to toAlpha");
        } else {
            this.mLastToAlpha = toAlpha;
            ObjectAnimator objectAnimator = this.mSplitBarAlphaAnimator;
            if (objectAnimator != null && objectAnimator.isStarted()) {
                this.mSplitBarAlphaAnimator.cancel();
            }
            ObjectAnimator ofFloat = ObjectAnimator.ofFloat(mSplitBar, View.ALPHA, mSplitBar.getAlpha(), toAlpha);
            this.mSplitBarAlphaAnimator = ofFloat;
            ofFloat.setDuration(duration);
            this.mSplitBarAlphaAnimator.setInterpolator(new SharpCurveInterpolator());
            this.mSplitBarAlphaAnimator.start();
        }
    }

    public void setMargins(int originalMargin, int swapMargin) {
        this.mOriginalMargin = originalMargin;
        this.mSwapMargin = swapMargin;
    }

    public void showSplitBar() {
        mSplitBar.setAlpha(1.0f);
    }

    public void showSplitBarWithAnimation() {
        playSplitBarAnimation(1.0f, 100L);
    }

    public void updateMargin(boolean isUpdateToSwapMargin) {
        if (mSplitBar == null) {
            Log.w(TAG, "updateMargin failed, cause mSplitBar is null!");
            return;
        }
        int v0 = isUpdateToSwapMargin ? this.mSwapMargin : this.mOriginalMargin;
        Log.i(TAG, "updateMargin debug, isUpdateToSwapMargin " + isUpdateToSwapMargin + " v0 " + v0);
        ViewGroup.LayoutParams lp = mSplitBar.getLayoutParams();
        if (!(lp instanceof RelativeLayout.LayoutParams)) {
            Log.w(TAG, "updateMargin failed, cause mSplitBar's lp is not instance of RelativeLayout.LayoutParams!");
            return;
        }
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) lp;
        if (mIsLandScape) {
            params.leftMargin = v0;
        } else {
            params.topMargin = v0;
        }
        mSplitBar.setLayoutParams(params);
    }

    public static void freeformDropTranslation(float fromTranslation, Object translationObj) {
        if (mSplitBar == null) {
            Log.w(TAG, "updateTranslation failed, cause mSplitBar is null!");
        } else if (!(translationObj instanceof Float)) {
        } else {
            float toTranslation = ((Float) translationObj).floatValue();
            if (mIsLandScape) {
                mSplitBar.setTranslationX((toTranslation - fromTranslation) * 2.0f);
            } else {
                mSplitBar.setTranslationY((toTranslation - fromTranslation) * 2.0f);
            }
            mSplitBar.invalidate();
        }
    }

    public void releaseSplitBar() {
        mSplitBar = null;
    }
}
