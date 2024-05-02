package com.android.server.wm;

import android.content.Context;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Slog;
/* loaded from: classes.dex */
public final class MiuiWindowStateEx implements IMiuiWindowStateEx {
    private static boolean DimDebug = true;
    private static final String TAG = "MiuiWindowStateEx";
    private static PowerManager.WakeLock mDimWakeLock;
    private long clickScreenTime;
    private PowerManager.WakeLock mInteruptCallWakeLock;
    private PowerManager.WakeLock mNotificationWakeLock;
    private PowerManager mPowerManager;
    private long mScreenOffTimeout;
    final WindowManagerService mService;
    private final WindowState mWinState;

    public MiuiWindowStateEx(WindowManagerService service, Object w) {
        this.mService = service;
        this.mWinState = (WindowState) w;
    }

    public void useCloudDimConfigIfNeeded(Context context, WindowStateAnimator winAnimator, boolean shouldRelayout) {
        PowerManager.WakeLock wakeLock;
        CharSequence title = this.mWinState.getWindowTag();
        if (title != null) {
            String name = title.toString();
            if (!this.mWinState.mIsDimWindow) {
                wakeUpIfNeeded(context, name, winAnimator, shouldRelayout);
            } else if (this.mWinState.mIsDimWindow) {
                if (!shouldRelayout && winAnimator.hasSurface() && !this.mWinState.mAnimatingExit && (wakeLock = mDimWakeLock) != null && wakeLock.isHeld()) {
                    this.mWinState.mAttrs.userActivityTimeout = -1L;
                    if (DimDebug) {
                        Slog.d(TAG, "keep the dim wakelock in relayoutwindow!!!name = " + name);
                    }
                }
                if (shouldRelayout) {
                    useCloudDimConfigLocked(context, name);
                }
            }
        }
    }

    public void useCloudDimConfigLocked(Context context, String name) {
        int attrFlags = this.mWinState.mAttrs.flags;
        if ((4194304 & attrFlags) == 0 && this.mService.isKeyguardLocked()) {
            return;
        }
        if (isCloudDimWindowingMode()) {
            this.mWinState.mAttrs.userActivityTimeout = this.mWinState.getUserActivitiyTime(name);
            if (this.mPowerManager == null) {
                this.mPowerManager = (PowerManager) context.getSystemService("power");
            }
            if (mDimWakeLock == null) {
                PowerManager.WakeLock newWakeLock = this.mPowerManager.newWakeLock(6, "DIM-WAKELOCK-FROM-COULD");
                mDimWakeLock = newWakeLock;
                newWakeLock.setReferenceCounted(false);
            }
            if (this.mInteruptCallWakeLock == null) {
                PowerManager.WakeLock newWakeLock2 = this.mPowerManager.newWakeLock(805306378, "WAKEUP-WHEN-INTERRUPT-CALL");
                this.mInteruptCallWakeLock = newWakeLock2;
                newWakeLock2.setReferenceCounted(false);
            }
            if (isHoldScreenOnWindow()) {
                mDimWakeLock.acquire();
            } else {
                long screenOffTimeout = this.mService.getScreenOffTimeout();
                this.mScreenOffTimeout = screenOffTimeout;
                mDimWakeLock.acquire(screenOffTimeout);
                this.clickScreenTime = SystemClock.elapsedRealtime();
            }
            if (DimDebug) {
                Slog.d(TAG, "get the dim wakelock, and the mScreenOffTimeout = " + this.mScreenOffTimeout);
                return;
            }
            return;
        }
        if (DimDebug) {
            Slog.d(TAG, "win.mAttrs.userActivityTimeout =-1");
        }
        this.mWinState.mAttrs.userActivityTimeout = -1L;
        PowerManager.WakeLock wakeLock = mDimWakeLock;
        if (wakeLock != null && wakeLock.isHeld()) {
            mDimWakeLock.release();
        }
    }

    public void resetUserActivityTime() {
        if (this.mWinState.mIsDimWindow && !this.mService.isCameraOpen()) {
            CharSequence title = this.mWinState.getWindowTag();
            if (title != null) {
                this.mWinState.mAttrs.userActivityTimeout = -1L;
                PowerManager.WakeLock wakeLock = mDimWakeLock;
                if (wakeLock != null && wakeLock.isHeld()) {
                    long delay = SystemClock.elapsedRealtime() - this.clickScreenTime;
                    if (!this.mService.isDeviceGoingToSleep() && !this.mService.isKeyguardLocked() && (delay < this.mScreenOffTimeout || isHoldScreenOnWindow())) {
                        this.mInteruptCallWakeLock.acquire();
                        this.mInteruptCallWakeLock.release();
                    }
                    mDimWakeLock.release();
                }
            }
        }
    }

    public void setUserActivityTime(Context context) {
        CharSequence title;
        if (!this.mWinState.mIsDimWindow) {
            PowerManager.WakeLock wakeLock = this.mNotificationWakeLock;
            if (wakeLock != null && wakeLock.isHeld()) {
                this.mNotificationWakeLock.release();
            }
        } else if (!this.mService.isCameraOpen() && (title = this.mWinState.getWindowTag()) != null) {
            String name = title.toString();
            useCloudDimConfigLocked(context, name);
        }
    }

    public void wakeUpIfNeeded(Context context, String name, WindowStateAnimator winAnimator, boolean shouldRelayout) {
        PowerManager.WakeLock wakeLock;
        if ("NotificationShade".equals(name)) {
            if (this.mPowerManager == null) {
                this.mPowerManager = (PowerManager) context.getSystemService("power");
            }
            if (this.mNotificationWakeLock == null) {
                PowerManager.WakeLock newWakeLock = this.mPowerManager.newWakeLock(268435466, "WAKEUP-FROM-NOTIFICATION");
                this.mNotificationWakeLock = newWakeLock;
                newWakeLock.setReferenceCounted(false);
            }
            if (!shouldRelayout && !winAnimator.hasSurface() && !this.mWinState.mAnimatingExit && this.mNotificationWakeLock.isHeld()) {
                this.mNotificationWakeLock.release();
            }
            if (!this.mService.isDeviceGoingToSleep() && !this.mService.isKeyguardLocked() && shouldRelayout && winAnimator.hasSurface() && !this.mWinState.mAnimatingExit && (wakeLock = mDimWakeLock) != null && wakeLock.isHeld() && !this.mNotificationWakeLock.isHeld()) {
                this.mNotificationWakeLock.acquire();
            }
        }
    }

    public void setScreenOffTimeOutDelay() {
        if (!isHoldScreenOnWindow() && mDimWakeLock != null && isCloudDimWindowingMode()) {
            mDimWakeLock.acquire(this.mScreenOffTimeout);
            this.clickScreenTime = SystemClock.elapsedRealtime();
        }
    }

    protected boolean isHoldScreenOnWindow() {
        int attrFlags = this.mWinState.mAttrs.flags;
        if (this.mWinState.mDimAssist && (attrFlags & 128) != 0) {
            return true;
        }
        return false;
    }

    protected boolean isCloudDimWindowingMode() {
        if (this.mWinState.getWindowingMode() != 5 && this.mWinState.getWindowingMode() != 3 && this.mWinState.getWindowingMode() != 4) {
            return true;
        }
        return false;
    }
}
