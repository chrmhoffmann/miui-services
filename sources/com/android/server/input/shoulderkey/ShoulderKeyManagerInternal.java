package com.android.server.input.shoulderkey;

import android.view.KeyEvent;
import android.view.MotionEvent;
/* loaded from: classes.dex */
public interface ShoulderKeyManagerInternal {
    void handleShoulderKeyEvent(KeyEvent keyEvent);

    void notifyTouchMotionEvent(MotionEvent motionEvent);

    void onUserSwitch();

    void setShoulderKeySwitchStatus(int i, boolean z);

    void systemReady();

    void updateScreenState(boolean z);
}
