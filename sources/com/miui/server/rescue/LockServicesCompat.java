package com.miui.server.rescue;

import android.content.Context;
import android.os.ServiceManager;
import android.text.TextUtils;
import android.util.Slog;
import com.android.internal.widget.ILockSettings;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockscreenCredential;
import com.android.internal.widget.VerifyCredentialResponse;
/* loaded from: classes.dex */
public class LockServicesCompat {
    private LockPatternUtils mLockPatternUtils;
    private ILockSettings mLockSettingsService;

    public LockServicesCompat(Context context) {
        this.mLockPatternUtils = new LockPatternUtils(context);
    }

    private ILockSettings getLockSettings() {
        if (this.mLockSettingsService == null) {
            ILockSettings service = ILockSettings.Stub.asInterface(ServiceManager.getService("lock_settings"));
            this.mLockSettingsService = service;
        }
        ILockSettings service2 = this.mLockSettingsService;
        return service2;
    }

    private LockscreenCredential getCredential(String password, int currentUserId) {
        if (TextUtils.isEmpty(password)) {
            return LockscreenCredential.createNone();
        }
        if (this.mLockPatternUtils.isLockPasswordEnabled(currentUserId)) {
            int quality = this.mLockPatternUtils.getKeyguardStoredPasswordQuality(currentUserId);
            if (LockPatternUtils.isQualityAlphabeticPassword(quality)) {
                return LockscreenCredential.createPassword(password);
            }
            return LockscreenCredential.createPin(password);
        } else if (this.mLockPatternUtils.isLockPatternEnabled(currentUserId)) {
            return LockscreenCredential.createPattern(LockPatternUtils.byteArrayToPattern(password.getBytes()));
        } else {
            return LockscreenCredential.createPassword(password);
        }
    }

    public int verifyCredentials(String pin, int challenge, int userId) {
        if (!this.mLockPatternUtils.isSecure(userId) && pin.equals("nopasswd")) {
            return 0;
        }
        LockscreenCredential lkCredential = getCredential(pin, userId);
        return verifyCredential(lkCredential, challenge, userId);
    }

    private int verifyCredential(LockscreenCredential credential, int challenge, int userId) {
        try {
            VerifyCredentialResponse response = getLockSettings().verifyCredential(credential, challenge, userId);
            if (response.getResponseCode() == 0) {
                return 0;
            }
            if (response.getResponseCode() != 1) {
                return -1;
            }
            Slog.e("BrokenScreenRescueService", "LockscreenService lock the service because of too many passsword errors.\nPlease wait " + (response.getTimeout() / 1000) + " seconds and try again!");
            return response.getTimeout();
        } catch (Exception re) {
            Slog.e("BrokenScreenRescueService", "failed to verify credential", re);
            return -1;
        }
    }
}
