package com.android.server.locksettings;

import android.security.MiuiLockPatternUtils;
import com.miui.base.MiuiStubRegistry;
/* loaded from: classes.dex */
public class LockSettingsImpl extends LockSettingsStub {
    public void savePrivacyPasswordPattern(String pattern, String filename, int userId) {
        MiuiLockPatternUtils.savePrivacyPasswordPattern(pattern, filename, userId);
    }

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<LockSettingsImpl> {

        /* compiled from: LockSettingsImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final LockSettingsImpl INSTANCE = new LockSettingsImpl();
        }

        public LockSettingsImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public LockSettingsImpl provideNewInstance() {
            return new LockSettingsImpl();
        }
    }

    public boolean checkPrivacyPasswordPattern(String pattern, String filename, int userId) {
        return MiuiLockPatternUtils.checkPrivacyPasswordPattern(pattern, filename, userId);
    }
}
