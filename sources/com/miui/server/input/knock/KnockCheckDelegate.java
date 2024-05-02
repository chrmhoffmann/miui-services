package com.miui.server.input.knock;

import android.content.Context;
import android.util.Slog;
import android.view.MotionEvent;
import com.miui.server.input.knock.view.KnockGesturePathView;
import java.util.ArrayList;
import java.util.List;
/* loaded from: classes.dex */
public class KnockCheckDelegate {
    private static final String TAG = "KnockCheckDelegate";
    private KnockGestureChecker mCurrentChecker;
    private KnockGesturePathView mKnockGesturePathView;
    private boolean mMultipleTouch = false;
    private List<KnockGestureChecker> mListeners = new ArrayList();

    public KnockCheckDelegate(Context context) {
        this.mKnockGesturePathView = new KnockGesturePathView(context);
    }

    public void registerKnockChecker(KnockGestureChecker knockGestureChecker) {
        if (this.mListeners.contains(knockGestureChecker)) {
            Slog.w(TAG, "repeat register knock checker :" + knockGestureChecker.getClass().getSimpleName());
        }
        if (!this.mListeners.contains(knockGestureChecker)) {
            knockGestureChecker.setKnockPathListener(this.mKnockGesturePathView);
            this.mListeners.add(knockGestureChecker);
        }
    }

    public void onTouchEvent(MotionEvent event) {
        this.mKnockGesturePathView.onPointerEvent(event);
        if (event.getAction() == 0) {
            resetState();
            this.mMultipleTouch = false;
        } else if (event.getActionMasked() == 5) {
            this.mMultipleTouch = true;
        }
        if (this.mMultipleTouch) {
            return;
        }
        KnockGestureChecker knockGestureChecker = this.mCurrentChecker;
        if (knockGestureChecker != null) {
            knockGestureChecker.onTouchEvent(event);
        } else if (this.mListeners != null) {
            for (int i = 0; i < this.mListeners.size(); i++) {
                KnockGestureChecker knockGestureChecker2 = this.mListeners.get(i);
                if (knockGestureChecker2.continueCheck()) {
                    knockGestureChecker2.onTouchEvent(event);
                    if (knockGestureChecker2.checkOnlyOneGesture()) {
                        this.mCurrentChecker = knockGestureChecker2;
                        return;
                    }
                }
            }
        }
    }

    private void resetState() {
        this.mCurrentChecker = null;
        for (int i = 0; i < this.mListeners.size(); i++) {
            this.mListeners.get(i).resetState();
        }
    }

    public void updateScreenState(boolean screenOn) {
        if (!screenOn) {
            this.mKnockGesturePathView.removeViewImmediate();
        }
    }

    public void onScreenSizeChanged() {
        for (int i = 0; i < this.mListeners.size(); i++) {
            this.mListeners.get(i).onScreenSizeChanged();
        }
    }
}
