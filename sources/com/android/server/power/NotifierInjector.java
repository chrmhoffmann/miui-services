package com.android.server.power;

import android.content.Context;
import android.content.Intent;
import android.hardware.input.InputManagerInternal;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManagerInternal;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Slog;
import android.view.IWindowManager;
import com.android.server.LocalServices;
import java.io.PrintWriter;
/* loaded from: classes.dex */
public class NotifierInjector {
    private static final String RESTORE_BRIGHTNESS_BROADCAST_ACTION = "miui.intent.action.RESTORE_BRIGHTNESS";
    private static final String RESTORE_BRIGHTNESS_BROADCAST_PERMISSION = "miui.permission.restore_brightness";
    private static final String TAG = "PowerManagerNotifierInjector";
    private boolean mAccelerometerRotationEnabled;
    private boolean mBeingHangUp;
    private final Context mContext;
    private boolean mDisableRotationDueToHangUp;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final InputManagerInternal mInputManagerInternal = (InputManagerInternal) LocalServices.getService(InputManagerInternal.class);
    private IWindowManager mWindowManagerService = IWindowManager.Stub.asInterface(ServiceManager.getService("window"));

    public NotifierInjector(Context context) {
        this.mContext = context;
        updateAccelerometerRotationLocked();
    }

    protected void updateRotationOffState(boolean hangUp) {
        if (hangUp) {
            try {
                if (this.mAccelerometerRotationEnabled) {
                    this.mDisableRotationDueToHangUp = true;
                    this.mWindowManagerService.freezeRotation(-1);
                    Slog.d(TAG, "updateRotationOffState: disable accelerometer rotation.");
                }
            } catch (RemoteException e) {
                Slog.w(TAG, "updateRotationOffState: " + e.toString());
                return;
            }
        }
        if (!hangUp && this.mDisableRotationDueToHangUp && !this.mAccelerometerRotationEnabled) {
            Slog.d(TAG, "updateRotationOffState: enable accelerometer rotation.");
            this.mDisableRotationDueToHangUp = false;
            this.mWindowManagerService.thawRotation();
        }
    }

    public void updateAccelerometerRotationLocked() {
        boolean z = false;
        if (Settings.System.getIntForUser(this.mContext.getContentResolver(), "accelerometer_rotation", 0, -2) == 1) {
            z = true;
        }
        this.mAccelerometerRotationEnabled = z;
    }

    public void onWakefulnessInHangUp(final boolean hangUp, int wakefulness) {
        if (this.mBeingHangUp != hangUp) {
            this.mBeingHangUp = hangUp;
            Slog.d(TAG, "onWakefulnessInHangUp: " + (this.mBeingHangUp ? "disable " : "enable ") + "input event.");
            boolean interactive = PowerManagerInternal.isInteractive(wakefulness);
            this.mInputManagerInternal.setInteractive(!this.mBeingHangUp && interactive);
            this.mHandler.post(new Runnable() { // from class: com.android.server.power.NotifierInjector.1
                @Override // java.lang.Runnable
                public void run() {
                    NotifierInjector.this.updateRotationOffState(hangUp);
                    NotifierInjector.this.sendHangupBroadcast(hangUp);
                }
            });
        }
    }

    protected void sendHangupBroadcast(boolean hangup) {
        Intent intent = new Intent("miui.intent.action.HANG_UP_CHANGED");
        intent.addFlags(1342177280);
        intent.putExtra("hang_up_enable", hangup);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL, "com.miui.permission.HANG_UP_CHANGED", null);
    }

    public void sendBroadcastRestoreBrightness() {
        this.mHandler.post(new Runnable() { // from class: com.android.server.power.NotifierInjector.2
            @Override // java.lang.Runnable
            public void run() {
                Intent intent = new Intent(NotifierInjector.RESTORE_BRIGHTNESS_BROADCAST_ACTION);
                NotifierInjector.this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL, NotifierInjector.RESTORE_BRIGHTNESS_BROADCAST_PERMISSION);
            }
        });
    }

    public void dump(PrintWriter pw) {
        pw.println("mBeingHangUp: " + this.mBeingHangUp);
        pw.println("mDisableRotationDueToHangUp: " + this.mDisableRotationDueToHangUp);
    }
}
