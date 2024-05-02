package com.android.server.wm;

import android.os.SystemClock;
import android.util.proto.ProtoOutputStream;
import android.view.SurfaceControl;
import com.android.server.wm.SurfaceAnimator;
import java.io.PrintWriter;
/* loaded from: classes.dex */
class MiuiFreeFormAnimationAdapter implements AnimationAdapter {
    private SurfaceAnimator.OnAnimationFinishedCallback mCapturedFinishCallback;
    public SurfaceControl mCapturedLeash;
    public final Object mLock = new Object();
    int mType = 0;

    public void onFreeFormAnimationFinished() {
        synchronized (this.mLock) {
            SurfaceAnimator.OnAnimationFinishedCallback onAnimationFinishedCallback = this.mCapturedFinishCallback;
            if (onAnimationFinishedCallback != null) {
                onAnimationFinishedCallback.onAnimationFinished(this.mType, this);
                this.mCapturedFinishCallback = null;
                this.mCapturedLeash = null;
                this.mType = 0;
            }
        }
    }

    public boolean getShowWallpaper() {
        return false;
    }

    public void startAnimation(SurfaceControl animationLeash, SurfaceControl.Transaction t, int type, SurfaceAnimator.OnAnimationFinishedCallback finishCallback) {
        synchronized (this.mLock) {
            this.mCapturedLeash = animationLeash;
            this.mCapturedFinishCallback = finishCallback;
            this.mType = type;
        }
    }

    public void onAnimationCancelled(SurfaceControl animationLeash) {
        onFreeFormAnimationFinished();
    }

    public long getDurationHint() {
        return 0L;
    }

    public long getStatusBarTransitionsStartTime() {
        return SystemClock.uptimeMillis();
    }

    public void dump(PrintWriter pw, String prefix) {
    }

    public void dumpDebug(ProtoOutputStream proto) {
    }
}
