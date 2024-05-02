package com.android.server.location.gnss.operators;

import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Environment;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.util.Log;
import com.android.server.FgThread;
import com.miui.base.MiuiStubRegistry;
import java.io.File;
import java.util.List;
/* loaded from: classes.dex */
public class GnssForTelcelCustomImpl implements GnssForTelcelCustomStub {
    private static final boolean IS_MX_TELCEL_REGION = "mx_telcel".equalsIgnoreCase(SystemProperties.get("ro.miui.customized.region"));
    private static final String PKG_NAME_CMT = "com.controlmovil.telcel";
    private static final String SP_CHILD_DIR = "system";
    private static final String SP_CHILD_FILE = "gnss_cmt.xml";
    private static final String SP_KEY_CMT_PERMISSION = "key_cmt";
    private static final String TAG = "GnssForTelcelCustomImpl";
    private final File mCmtSpFile = new File(new File(Environment.getDataDirectory(), SP_CHILD_DIR), SP_CHILD_FILE);
    private boolean mIsCmtHasDoPermission;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<GnssForTelcelCustomImpl> {

        /* compiled from: GnssForTelcelCustomImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final GnssForTelcelCustomImpl INSTANCE = new GnssForTelcelCustomImpl();
        }

        public GnssForTelcelCustomImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public GnssForTelcelCustomImpl provideNewInstance() {
            return new GnssForTelcelCustomImpl();
        }
    }

    public void init(Context context) {
        if (IS_MX_TELCEL_REGION) {
            Log.d(TAG, "is mx region");
            final DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService("device_policy");
            Context directBootContext = context.createDeviceProtectedStorageContext();
            SharedPreferences sp = directBootContext.getSharedPreferences(this.mCmtSpFile, 0);
            this.mIsCmtHasDoPermission = sp.getBoolean(SP_KEY_CMT_PERMISSION, false);
            Log.d(TAG, "CMT has device owner or admin permission when boot start:" + this.mIsCmtHasDoPermission);
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.app.action.DEVICE_OWNER_CHANGED");
            intentFilter.addAction("android.app.action.DEVICE_POLICY_MANAGER_STATE_CHANGED");
            context.registerReceiverAsUser(new BroadcastReceiver() { // from class: com.android.server.location.gnss.operators.GnssForTelcelCustomImpl.1
                @Override // android.content.BroadcastReceiver
                public void onReceive(Context context2, Intent intent) {
                    String action = intent.getAction();
                    if (action == null) {
                        return;
                    }
                    GnssForTelcelCustomImpl.this.isCmtHasDeviceAdmin(context2, dpm);
                }
            }, UserHandle.ALL, intentFilter, null, FgThread.getHandler());
        }
    }

    public void isCmtHasDeviceAdmin(Context context, DevicePolicyManager dpm) {
        this.mIsCmtHasDoPermission = false;
        if (!dpm.isDeviceOwnerApp(PKG_NAME_CMT)) {
            List<ComponentName> list = dpm.getActiveAdmins();
            if (list == null) {
                return;
            }
            for (ComponentName componentName : list) {
                if (componentName != null && PKG_NAME_CMT.equals(componentName.getPackageName())) {
                    this.mIsCmtHasDoPermission = true;
                }
            }
            Log.d(TAG, "receive device admin changed action , mx cmt has permission:" + this.mIsCmtHasDoPermission);
        } else {
            this.mIsCmtHasDoPermission = true;
            Log.d(TAG, "receive device owner changed action , mx cmt has permission:" + this.mIsCmtHasDoPermission);
        }
        SharedPreferences.Editor editor = context.getSharedPreferences(this.mCmtSpFile, 0).edit();
        editor.putBoolean(SP_KEY_CMT_PERMISSION, this.mIsCmtHasDoPermission);
        editor.commit();
    }

    public boolean isCmtHasPermission() {
        return this.mIsCmtHasDoPermission;
    }
}
