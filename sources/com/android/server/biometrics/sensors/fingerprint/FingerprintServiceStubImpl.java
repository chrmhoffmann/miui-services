package com.android.server.biometrics.sensors.fingerprint;

import android.app.ActivityThread;
import android.app.Application;
import android.content.Context;
import android.content.pm.UserInfo;
import android.hardware.fingerprint.Fingerprint;
import android.os.Binder;
import android.os.PowerManager;
import android.os.UserManager;
import android.util.Log;
import com.miui.base.MiuiStubRegistry;
import java.util.ArrayList;
/* loaded from: classes.dex */
public class FingerprintServiceStubImpl implements FingerprintServiceStub {
    private static final String TAG = "FingerprintServiceStubImpl";
    Fingerprint mFingerIdentifer;
    ArrayList<Byte> mFingerToken;
    public long mOpId = -1;
    public String mOpPackage = "";

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<FingerprintServiceStubImpl> {

        /* compiled from: FingerprintServiceStubImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final FingerprintServiceStubImpl INSTANCE = new FingerprintServiceStubImpl();
        }

        public FingerprintServiceStubImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public FingerprintServiceStubImpl provideNewInstance() {
            return new FingerprintServiceStubImpl();
        }
    }

    public boolean isScreenOn(Context context) {
        PowerManager powerManager = (PowerManager) context.getSystemService(PowerManager.class);
        return powerManager.isInteractive();
    }

    public void saveAuthenticateConfig(long opId, String opPackageName) {
        this.mOpId = opId;
        this.mOpPackage = opPackageName;
    }

    public long getOpId() {
        long opId = this.mOpId;
        return opId;
    }

    public String getOpPackageName() {
        String opPackage = this.mOpPackage;
        return opPackage;
    }

    public void saveAuthenResultLocal(Fingerprint identifier, ArrayList<Byte> token) {
        this.mFingerIdentifer = identifier;
        this.mFingerToken = token;
    }

    public Fingerprint getIdentifier() {
        return this.mFingerIdentifer;
    }

    public ArrayList<Byte> getToken() {
        return this.mFingerToken;
    }

    public void clearSavedAuthenResult() {
        this.mFingerIdentifer = null;
        this.mFingerToken = null;
    }

    public boolean isFingerDataSharer(int userId) {
        UserInfo uinfo = null;
        Application app = ActivityThread.currentApplication();
        UserManager manager = (UserManager) app.getSystemService(UserManager.class);
        long token = Binder.clearCallingIdentity();
        try {
            try {
                uinfo = manager.getUserInfo(userId);
            } catch (Exception ex) {
                Log.d(TAG, ex.getMessage());
            }
            if (uinfo != null) {
                if (uinfo.name == null) {
                    Log.d(TAG, "calling from anonymous user, id = " + userId);
                } else {
                    String str = uinfo.name;
                    char c = 65535;
                    switch (str.hashCode()) {
                        case -1695516786:
                            if (str.equals("XSpace")) {
                                c = 2;
                                break;
                            }
                            break;
                        case -946401722:
                            if (str.equals("child_model")) {
                                c = 1;
                                break;
                            }
                            break;
                        case 2135952422:
                            if (str.equals("security space")) {
                                c = 0;
                                break;
                            }
                            break;
                    }
                    switch (c) {
                        case 0:
                        case 1:
                        case 2:
                            Log.d(TAG, "calling from " + uinfo.name + ", id = " + userId);
                            return true;
                    }
                }
                if (uinfo.isManagedProfile()) {
                    Log.d(TAG, "calling from managed-profile, id =  " + userId + ", owned by " + uinfo.profileGroupId);
                    return true;
                }
                Log.d(TAG, uinfo.toFullString());
            }
            return false;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public int getcurrentUserId(int groupId) {
        return getMiuiGroupId(groupId);
    }

    public int getMiuiGroupId(int userId) {
        if (userId == 0 || !isFingerDataSharer(userId)) {
            return userId;
        }
        return 0;
    }
}
