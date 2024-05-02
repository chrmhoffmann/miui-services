package com.android.server.policy;
/* loaded from: classes.dex */
public abstract class AbstractKeyguardServiceDelegate {
    public abstract void OnDoubleClickHome();

    protected abstract void enableUserActivity(boolean z);

    public abstract boolean isShowing();

    public abstract boolean isShowingAndNotHidden();

    public abstract void keyguardDone();

    public abstract void onScreenTurnedOnWithoutListener();

    public abstract boolean onWakeKeyWhenKeyguardShowingTq(int i, boolean z);

    public abstract void pokeWakelock();
}
