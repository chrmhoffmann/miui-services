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
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.os.UserHandle;
import android.util.Log;
import java.util.Locale;
/* loaded from: classes.dex */
public class GnssEventHandler {
    private static final String TAG = "GnssNps";
    private static GnssEventHandler mInstance;
    private Context mAppContext;
    private NotifyManager mNotifyManager = new NotifyManager();

    public static synchronized GnssEventHandler getInstance(Context context) {
        GnssEventHandler gnssEventHandler;
        synchronized (GnssEventHandler.class) {
            if (mInstance == null) {
                mInstance = new GnssEventHandler(context);
            }
            gnssEventHandler = mInstance;
        }
        return gnssEventHandler;
    }

    private GnssEventHandler(Context context) {
        this.mAppContext = context;
    }

    public void handleStart() {
        this.mNotifyManager.initNotification();
        this.mNotifyManager.showNotification();
    }

    public void handleStop() {
        this.mNotifyManager.removeNotification();
    }

    public void handleFix() {
        Log.d(TAG, "fixed");
        this.mNotifyManager.removeNotification();
    }

    public void handleLose() {
        Log.d(TAG, "lose location");
        this.mNotifyManager.showNotification();
    }

    public void handleRecover() {
        Log.d(TAG, "fix again");
        this.mNotifyManager.removeNotification();
    }

    public void handleCallerName(String name) {
        Log.d(TAG, "APP caller name");
        this.mNotifyManager.updateCallerName(name);
    }

    public boolean isChineseLanguage() {
        String language = Locale.getDefault().toString();
        return language.endsWith("zh_CN");
    }

    /* loaded from: classes.dex */
    public class NotifyManager {
        private Notification.Builder mBuilder;
        private NotificationManager mNotificationManager;
        private final String CHANNEL_ID = "GPS_STATUS_MONITOR_ID";
        private String packageName = null;
        private CharSequence appName = null;
        private boolean mmock = false;
        private boolean mnoise = false;

        public NotifyManager() {
            GnssEventHandler.this = this$0;
        }

        public void initNotification() {
            this.mNotificationManager = (NotificationManager) GnssEventHandler.this.mAppContext.getSystemService("notification");
            this.mBuilder = new Notification.Builder(GnssEventHandler.this.mAppContext, "GPS_STATUS_MONITOR_ID");
            constructNotification();
        }

        private void constructNotification() {
            String description = GnssEventHandler.this.mAppContext.getString(286196375);
            Intent intent = new Intent();
            intent.setClassName("com.android.settings", "com.android.settings.Settings$LocationSettingsActivity");
            intent.addFlags(268435456);
            PendingIntent pendingIntent = PendingIntent.getActivity(GnssEventHandler.this.mAppContext, 0, intent, 201326592);
            NotificationChannel channel = new NotificationChannel("GPS_STATUS_MONITOR_ID", description, 2);
            channel.setLockscreenVisibility(1);
            channel.setSound(null, null);
            channel.enableVibration(false);
            channel.setVibrationPattern(new long[]{0});
            BitmapDrawable draw = null;
            Bitmap iconBmp = null;
            PackageManager pm = GnssEventHandler.this.mAppContext.getPackageManager();
            try {
                draw = pm.getApplicationIcon(this.packageName);
                ApplicationInfo ai = pm.getApplicationInfo(this.packageName, 0);
                this.appName = ai.loadLabel(pm);
            } catch (PackageManager.NameNotFoundException e) {
                Log.w(GnssEventHandler.TAG, "No such package for this name!");
            }
            if (draw instanceof BitmapDrawable) {
                iconBmp = draw.getBitmap();
            }
            if (iconBmp == null) {
                Log.w(GnssEventHandler.TAG, "iconBmp is null, using default");
                this.mBuilder.setLargeIcon(BitmapFactory.decodeResource(GnssEventHandler.this.mAppContext.getResources(), 285737107));
            } else {
                this.mBuilder.setLargeIcon(iconBmp);
            }
            Bundle arg = new Bundle();
            arg.putParcelable("miui.appIcon", Icon.createWithResource(GnssEventHandler.this.mAppContext, 285737108));
            try {
                this.mNotificationManager.createNotificationChannel(channel);
                if (this.mmock || this.mnoise) {
                    if (this.mnoise) {
                        this.mBuilder.setContentTitle(GnssEventHandler.this.mAppContext.getString(286196378)).addExtras(arg).setSmallIcon(285737108).setContentText(GnssEventHandler.this.mAppContext.getString(286196372)).setContentIntent(pendingIntent).setOngoing(true).setAutoCancel(true);
                    } else {
                        this.mBuilder.setContentTitle(GnssEventHandler.this.mAppContext.getString(286196376)).addExtras(arg).setSmallIcon(285737108).setContentText(GnssEventHandler.this.mAppContext.getString(286196372)).setContentIntent(pendingIntent).setOngoing(true).setAutoCancel(true);
                    }
                } else {
                    this.mBuilder.setContentTitle(((Object) this.appName) + GnssEventHandler.this.mAppContext.getString(286196373)).addExtras(arg).setSmallIcon(285737108).setContentText(GnssEventHandler.this.mAppContext.getString(286196372)).setContentIntent(pendingIntent).setOngoing(true).setAutoCancel(true);
                }
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }

        public void showNotification() {
            if (!GnssEventHandler.this.isChineseLanguage()) {
                Log.d(GnssEventHandler.TAG, "show notification only in CHINESE language");
            } else if (this.packageName == null) {
                Log.d(GnssEventHandler.TAG, "no package name presence");
            } else {
                if (this.mmock) {
                    this.appName = "gps server";
                }
                if (this.mnoise) {
                    this.appName = "gps server";
                }
                if (this.appName == null) {
                    Log.d(GnssEventHandler.TAG, "no app name presence");
                    return;
                }
                Log.d(GnssEventHandler.TAG, "showNotification");
                try {
                    this.mNotificationManager.notifyAsUser(null, 285737108, this.mBuilder.build(), UserHandle.ALL);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        public void updateCallerName(String name) {
            Log.d(GnssEventHandler.TAG, "updateCallerName=" + name);
            if (name.contains("gps is mock") && !this.mmock) {
                this.mmock = true;
                GnssEventHandler.this.mNotifyManager.initNotification();
                GnssEventHandler.this.mNotifyManager.showNotification();
            } else if (name.contains("remove mock") && this.mmock) {
                GnssEventHandler.this.mNotifyManager.removeNotification();
                this.mmock = false;
            } else if (name.contains("noise_environment") && !this.mnoise) {
                this.mnoise = true;
                GnssEventHandler.this.mNotifyManager.initNotification();
                GnssEventHandler.this.mNotifyManager.showNotification();
            } else if (name.contains("normal_environment") && this.mnoise) {
                this.mnoise = false;
                GnssEventHandler.this.mNotifyManager.removeNotification();
            }
            this.packageName = name;
        }

        public void removeNotification() {
            Log.d(GnssEventHandler.TAG, "removeNotification");
            try {
                if (this.mNotificationManager == null) {
                    Log.d(GnssEventHandler.TAG, "mNotificationManager is null");
                    initNotification();
                }
                NotificationManager notificationManager = this.mNotificationManager;
                if (notificationManager != null) {
                    notificationManager.cancelAsUser(null, 285737108, UserHandle.ALL);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
