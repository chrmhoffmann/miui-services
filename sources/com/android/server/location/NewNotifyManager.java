package com.android.server.location;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.util.Log;
import java.util.Locale;
/* loaded from: classes.dex */
public class NewNotifyManager {
    private static final String TAG = "GnssNps-NotifyManager";
    private final String CHANNEL_ID = "GPS_STATUS_MONITOR_ID";
    private final int NOTIFY_ID = 1;
    private PendingIntent intentToSettings;
    private Context mAppContext;
    private Notification.Builder mBuilder;
    private NotificationManager mNotificationManager;

    public NewNotifyManager(Context context) {
        this.mAppContext = context;
        this.mNotificationManager = (NotificationManager) context.getSystemService("notification");
        this.mBuilder = new Notification.Builder(this.mAppContext, "GPS_STATUS_MONITOR_ID");
        initNotifyChannel();
        Intent intent = new Intent();
        intent.setClassName("com.android.settings", "com.android.settings.Settings$LocationSettingsActivity");
        intent.addFlags(268435456);
        this.intentToSettings = PendingIntent.getActivity(this.mAppContext, 0, intent, 201326592);
    }

    public void removeNotification() {
        Log.d(TAG, "removeNotification");
        try {
            NotificationManager notificationManager = this.mNotificationManager;
            if (notificationManager != null) {
                notificationManager.cancelAll();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void showNavigationNotification(String packageName) {
        if (!isChineseLanguage()) {
            Log.d(TAG, "show notification only in CHINESE language");
            return;
        }
        PackageManager pm = this.mAppContext.getPackageManager();
        try {
            ApplicationInfo ai = pm.getApplicationInfo(packageName, 0);
            String appName = ai.loadLabel(pm).toString();
            Drawable draw = pm.getApplicationIcon(packageName);
            Bitmap iconBmp = getAppIcon(draw);
            if (iconBmp == null) {
                iconBmp = BitmapFactory.decodeResource(this.mAppContext.getResources(), 285737107);
            }
            this.mBuilder.setLargeIcon(iconBmp);
            this.mBuilder.setSmallIcon(285737108);
            try {
                this.mBuilder.setContentTitle(appName + this.mAppContext.getString(286196373)).setContentText(this.mAppContext.getString(286196372)).setContentIntent(this.intentToSettings).setOngoing(true).setAutoCancel(true);
                this.mNotificationManager.notify(1, this.mBuilder.build());
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (PackageManager.NameNotFoundException e2) {
            Log.w(TAG, "No such application for this name:" + packageName);
        }
    }

    private Bitmap getAppIcon(Drawable draw) {
        if (draw instanceof BitmapDrawable) {
            return ((BitmapDrawable) draw).getBitmap();
        }
        if (draw instanceof AdaptiveIconDrawable) {
            Drawable backgroundDr = ((AdaptiveIconDrawable) draw).getBackground();
            Drawable foregroundDr = ((AdaptiveIconDrawable) draw).getForeground();
            Drawable[] drr = {backgroundDr, foregroundDr};
            LayerDrawable layerDrawable = new LayerDrawable(drr);
            int width = layerDrawable.getIntrinsicWidth();
            int height = layerDrawable.getIntrinsicHeight();
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            layerDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            layerDrawable.draw(canvas);
            return bitmap;
        }
        return null;
    }

    private boolean isChineseLanguage() {
        String language = Locale.getDefault().toString();
        return language.endsWith("zh_CN");
    }

    public void initNotifyChannel() {
        if (this.mNotificationManager == null) {
            return;
        }
        String description = this.mAppContext.getString(286196375);
        NotificationChannel channel = new NotificationChannel("GPS_STATUS_MONITOR_ID", description, 2);
        channel.setLockscreenVisibility(1);
        channel.setSound(null, null);
        channel.enableVibration(false);
        channel.setVibrationPattern(new long[]{0});
        this.mNotificationManager.createNotificationChannel(channel);
    }
}
