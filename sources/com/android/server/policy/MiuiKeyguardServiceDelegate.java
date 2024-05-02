package com.android.server.policy;

import android.os.PowerManager;
import android.os.SystemClock;
import com.android.server.policy.keyguard.KeyguardServiceDelegate;
/* loaded from: classes.dex */
public class MiuiKeyguardServiceDelegate extends AbstractKeyguardServiceDelegate {
    protected KeyguardServiceDelegate mKeyguardDelegate;
    protected PhoneWindowManager mPhoneWindowManager;
    protected PowerManager mPowerManager;

    public MiuiKeyguardServiceDelegate(PhoneWindowManager phoneWindowManager, KeyguardServiceDelegate keyguardDelegate, PowerManager powerManager) {
        this.mPhoneWindowManager = phoneWindowManager;
        this.mKeyguardDelegate = keyguardDelegate;
        this.mPowerManager = powerManager;
    }

    @Override // com.android.server.policy.AbstractKeyguardServiceDelegate
    public void enableUserActivity(boolean value) {
    }

    @Override // com.android.server.policy.AbstractKeyguardServiceDelegate
    public boolean isShowing() {
        return this.mKeyguardDelegate.isShowing();
    }

    @Override // com.android.server.policy.AbstractKeyguardServiceDelegate
    public boolean isShowingAndNotHidden() {
        return isShowing() && !this.mPhoneWindowManager.isKeyguardOccluded();
    }

    @Override // com.android.server.policy.AbstractKeyguardServiceDelegate
    public void keyguardDone() {
    }

    @Override // com.android.server.policy.AbstractKeyguardServiceDelegate
    public boolean onWakeKeyWhenKeyguardShowingTq(int keyCode, boolean isDocked) {
        return false;
    }

    @Override // com.android.server.policy.AbstractKeyguardServiceDelegate
    public void onScreenTurnedOnWithoutListener() {
        KeyguardServiceDelegate keyguardServiceDelegate = this.mKeyguardDelegate;
        if (keyguardServiceDelegate != null) {
            keyguardServiceDelegate.onScreenTurnedOn();
        }
    }

    @Override // com.android.server.policy.AbstractKeyguardServiceDelegate
    public void pokeWakelock() {
        this.mPowerManager.userActivity(SystemClock.uptimeMillis(), true);
    }

    @Override // com.android.server.policy.AbstractKeyguardServiceDelegate
    public void OnDoubleClickHome() {
    }
}
