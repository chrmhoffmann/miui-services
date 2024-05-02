package com.android.server.notification;
/* loaded from: classes.dex */
public class VibRateLimiter {
    private static final long NOTIFICATION_VIBRATION_TIME_RATE = 15000;
    private long mLastVibNotificationMillis = 0;

    public boolean shouldRateLimitVib(long now) {
        long millisSinceLast = now - this.mLastVibNotificationMillis;
        if (millisSinceLast < NOTIFICATION_VIBRATION_TIME_RATE) {
            return true;
        }
        this.mLastVibNotificationMillis = now;
        return false;
    }
}
