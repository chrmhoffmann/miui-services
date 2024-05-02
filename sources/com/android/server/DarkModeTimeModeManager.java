package com.android.server;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Slog;
import android.widget.Toast;
import com.android.server.am.BroadcastQueueImpl;
import com.miui.server.SecurityManagerService;
import miui.hardware.display.DisplayFeatureManager;
import miui.util.FeatureParser;
/* loaded from: classes.dex */
public class DarkModeTimeModeManager {
    public static final String DARK_MODE_ENABLE = "dark_mode_enable";
    public static final int SCREEN_DARKMODE = 38;
    public static final boolean SUPPORT_DARK_MODE_NOTIFY = FeatureParser.getBoolean("support_dark_mode_notify", false);
    private static final String TAG = "DarkModeTimeModeManager";
    private AlarmManager.OnAlarmListener mAlarmListener = new AlarmManager.OnAlarmListener() { // from class: com.android.server.DarkModeTimeModeManager.3
        @Override // android.app.AlarmManager.OnAlarmListener
        public void onAlarm() {
            Slog.i(DarkModeTimeModeManager.TAG, "update suntime on alarm");
            DarkModeTimeModeHelper.updateDarkModeSuntime(DarkModeTimeModeManager.this.mContext);
            DarkModeTimeModeHelper.sendDarkModeSunTimeBroadcast(DarkModeTimeModeManager.this.mContext);
        }
    };
    private AlarmManager mAlarmManager;
    private LocatedCityChangeObserver mCityObserver;
    private Context mContext;
    private ContentObserver mDarkModeObserver;
    private DarkModeSuggestProvider mDarkModeSuggestProvider;
    private DarkModeTimeModeReceiver mDarkModeTimeModeReceiver;
    private DarkModeTimeTypeObserver mDarkModeTimeTypeObserver;

    public DarkModeTimeModeManager() {
    }

    public DarkModeTimeModeManager(final Context context) {
        this.mContext = context;
        IntentFilter intentFilter = new IntentFilter();
        addAction(intentFilter, context);
        DarkModeTimeModeReceiver darkModeTimeModeReceiver = new DarkModeTimeModeReceiver(new DarkModeTimeModeManager());
        this.mDarkModeTimeModeReceiver = darkModeTimeModeReceiver;
        context.registerReceiver(darkModeTimeModeReceiver, intentFilter);
        this.mAlarmManager = (AlarmManager) context.getSystemService("alarm");
        this.mDarkModeTimeTypeObserver = new DarkModeTimeTypeObserver(new Handler(), context);
        context.getContentResolver().registerContentObserver(Settings.System.getUriFor("dark_mode_time_type"), false, this.mDarkModeTimeTypeObserver);
        if (DarkModeTimeModeHelper.getDarkModeSuggestCount(context) < 3) {
            DarkModeSuggestProvider darkModeSuggestProvider = DarkModeSuggestProvider.getInstance();
            this.mDarkModeSuggestProvider = darkModeSuggestProvider;
            darkModeSuggestProvider.registerDataObserver(context);
        }
        this.mDarkModeObserver = new ContentObserver(new Handler()) { // from class: com.android.server.DarkModeTimeModeManager.1
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                super.onChange(selfChange);
                if (DarkModeTimeModeManager.SUPPORT_DARK_MODE_NOTIFY) {
                    DisplayFeatureManager.getInstance().setScreenEffect(38, DarkModeTimeModeHelper.isDarkModeEnable(context) ? 1 : 0);
                }
            }
        };
        context.getContentResolver().registerContentObserver(Settings.System.getUriFor(DARK_MODE_ENABLE), false, this.mDarkModeObserver);
    }

    private void addAction(IntentFilter it, Context context) {
        it.addAction("miui.action.intent.DARK_MODE_TIME_ON");
        it.addAction("miui.action.intent.DARK_MODE_TIME_OFF");
        it.addAction("miui.action.intent.DARK_MODE_TIME_MODE");
        it.addAction("miui.action.intent.DARK_MODE_SUGGEST_MESSAGE");
        it.addAction("miui.action.intent.DARK_MODE_SUGGEST_ENABLE");
        it.addAction("android.intent.action.DATE_CHANGED");
        it.addAction("android.intent.action.TIME_SET");
        it.addAction("android.intent.action.TIMEZONE_CHANGED");
        if (DarkModeTimeModeHelper.getDarkModeSuggestCount(context) < 3 || DarkModeStatusTracker.DEBUG) {
            it.addAction("android.intent.action.USER_PRESENT");
        }
    }

    public void onBootPhase(Context context) {
        updateDarkModeTimeModeStatus(context);
    }

    public void updateDarkModeTimeModeStatus(final Context context) {
        this.mAlarmManager.cancel(this.mAlarmListener);
        if (!DarkModeTimeModeHelper.isDarkModeTimeEnable(context)) {
            return;
        }
        if (DarkModeTimeModeHelper.isSuntimeType(context)) {
            if (DarkModeTimeModeHelper.isInternationalVersion()) {
                DarkModeTimeModeHelper.sendDarkModeSunTimeBroadcast(context);
                return;
            }
            registerObserver(context);
            MiuiBgThread.getHandler().post(new Runnable() { // from class: com.android.server.DarkModeTimeModeManager.2
                @Override // java.lang.Runnable
                public void run() {
                    DarkModeTimeModeHelper.updateDarkModeSuntime(context);
                    DarkModeTimeModeHelper.sendDarkModeSunTimeBroadcast(context);
                }
            });
            long nextTime = System.currentTimeMillis() + 43200000;
            this.mAlarmManager.setExact(1, nextTime, "update_suntime", this.mAlarmListener, MiuiBgThread.getHandler());
            return;
        }
        unRegisterObserver(context);
    }

    private void registerObserver(Context context) {
        try {
            Slog.i(TAG, "register CityChangeObserver success");
            this.mCityObserver = new LocatedCityChangeObserver(MiuiBgThread.getHandler(), context);
            context.getContentResolver().registerContentObserver(Uri.parse(DarkModeSunTimeHelper.WEATHER_CITY_URI_STRING), false, this.mCityObserver);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void unRegisterObserver(Context context) {
        try {
            Slog.i(TAG, "unregister CityChangeObserver success");
            if (this.mCityObserver == null) {
                return;
            }
            context.getContentResolver().unregisterContentObserver(this.mCityObserver);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean canShowDarkModeSuggestNotifocation(Context context) {
        if (DarkModeStatusTracker.DEBUG) {
            return true;
        }
        if (!DarkModeTimeModeHelper.isDarkModeSuggestEnable(context)) {
            Slog.i(TAG, "not get suggest from cloud");
            return false;
        } else if (DarkModeTimeModeHelper.getDarkModeSuggestCount(context) >= 3) {
            Slog.i(TAG, "count >= 3");
            return false;
        } else if (DarkModeTimeModeHelper.isDarkModeOpen(context)) {
            Slog.i(TAG, "darkMode is open");
            return false;
        } else if (!DarkModeTimeModeHelper.isInNight(context)) {
            Slog.i(TAG, "not in night");
            return false;
        } else if (DarkModeTimeModeHelper.getNowTimeInMills() - DarkModeTimeModeHelper.getLastSuggestTime(context) < 604800000) {
            Slog.i(TAG, "less than 7 days ago");
            return false;
        } else if (DarkModeTimeModeHelper.isOnHome(context)) {
            return true;
        } else {
            Slog.i(TAG, "not on home");
            return false;
        }
    }

    public void showDarkModeSuggestNotification(Context context) {
        Slog.i(TAG, "showDarkModeSuggestNotification");
        initNotification(context);
        DarkModeTimeModeHelper.setDarkModeSuggestCount(context, DarkModeTimeModeHelper.getDarkModeSuggestCount(context) + 1);
        DarkModeSuggestProvider.getInstance().unRegisterDataObserver(context);
        DarkModeTimeModeHelper.setLastSuggestTime(context, DarkModeTimeModeHelper.getNowTimeInMills());
        DarkModeStauesEvent event = new DarkModeStauesEvent().setEventName(DarkModeOneTrackHelper.EVENT_NAME_SUGGEST).setTip("").setSuggest(1);
        DarkModeOneTrackHelper.uploadToOneTrack(context, event);
    }

    private void initNotification(Context context) {
        String title = context.getResources().getString(286196128);
        String message = context.getResources().getString(286196127);
        Intent intentSettings = new Intent("miui.action.intent.DARK_MODE_SUGGEST_MESSAGE");
        PendingIntent pendingIntentSettings = PendingIntent.getBroadcast(context, 0, intentSettings, BroadcastQueueImpl.FLAG_IMMUTABLE);
        Intent intentEnable = new Intent("miui.action.intent.DARK_MODE_SUGGEST_ENABLE");
        PendingIntent pendingIntentEnable = PendingIntent.getBroadcast(context, 0, intentEnable, BroadcastQueueImpl.FLAG_IMMUTABLE);
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(NotificationManager.class);
        creatNoticifationChannel("dark_mode_suggest_id", "dark_mode", 4, "des", notificationManager);
        Bundle arg = new Bundle();
        arg.putParcelable("miui.appIcon", Icon.createWithResource(context, 285737067));
        arg.putBoolean("miui.showAction", true);
        Notification notification = new Notification.Builder(context, "dark_mode_suggest_id").addExtras(arg).setSmallIcon(285737067).setContentTitle(title).setContentText(message).setStyle(new Notification.BigTextStyle().bigText(message)).setContentIntent(pendingIntentSettings).addAction(285737005, context.getResources().getString(286196126), pendingIntentEnable).setAutoCancel(true).setTimeoutAfter(5000L).build();
        notificationManager.notifyAsUser(TAG, 1, notification, UserHandle.ALL);
    }

    private void creatNoticifationChannel(String channelId, CharSequence name, int importance, String description, NotificationManager manager) {
        NotificationChannel channel = new NotificationChannel(channelId, name, importance);
        channel.setDescription(description);
        manager.createNotificationChannel(channel);
    }

    public void showDarkModeSuggestToast(Context context) {
        Settings.System.putString(context.getContentResolver(), "open_sun_time_channel", DarkModeOneTrackHelper.PARAM_VALUE_CHANNEL_NOTIFY);
        updateButtonStatus(context);
        DarkModeStauesEvent event = new DarkModeStauesEvent().setEventName(DarkModeOneTrackHelper.EVENT_NAME_SUGGEST).setTip("").setSuggestEnable(1);
        DarkModeOneTrackHelper.uploadToOneTrack(context, event);
        DarkModeTimeModeHelper.setDarkModeSuggestCount(context, 3);
        Toast.makeText(context, context.getResources().getString(286196129), 0).show();
    }

    private void updateButtonStatus(Context context) {
        DarkModeTimeModeHelper.setDarkModeTimeEnable(context, true);
        DarkModeTimeModeHelper.setDarkModeAutoTimeEnable(context, false);
        DarkModeTimeModeHelper.setSunRiseSunSetMode(context, true);
        DarkModeTimeModeHelper.setDarkModeTimeType(context, 2);
    }

    public void enterSettingsFromNotification(final Context context) {
        Settings.System.putInt(context.getContentResolver(), "enter_setting_by_notification", 1);
        Intent intent = new Intent("android.settings.DISPLAY_SETTINGS");
        intent.setFlags(268435456);
        context.startActivity(intent);
        DarkModeTimeModeHelper.setDarkModeSuggestCount(context, 3);
        DarkModeStauesEvent event = new DarkModeStauesEvent().setEventName(DarkModeOneTrackHelper.EVENT_NAME_SUGGEST).setTip("").setSuggestClick(1);
        DarkModeOneTrackHelper.uploadToOneTrack(context, event);
        MiuiBgThread.getHandler().postDelayed(new Runnable() { // from class: com.android.server.DarkModeTimeModeManager.4
            @Override // java.lang.Runnable
            public void run() {
                if (DarkModeTimeModeHelper.isDarkModeOpen(context)) {
                    DarkModeStauesEvent event2 = new DarkModeStauesEvent().setEventName(DarkModeOneTrackHelper.EVENT_NAME_SUGGEST).setTip("").setSuggestOpenInSetting(1);
                    DarkModeOneTrackHelper.uploadToOneTrack(context, event2);
                }
                Settings.System.putInt(context.getContentResolver(), "enter_setting_by_notification", 0);
            }
        }, SecurityManagerService.LOCK_TIME_OUT);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class DarkModeTimeTypeObserver extends ContentObserver {
        private final String TAG = "DarkModeTimeTypeObserver";
        private Context mContext;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public DarkModeTimeTypeObserver(Handler handler, Context context) {
            super(handler);
            DarkModeTimeModeManager.this = r1;
            this.mContext = context;
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange) {
            Slog.i("DarkModeTimeTypeObserver", "onChange");
            super.onChange(selfChange);
            Context context = this.mContext;
            if (context == null) {
                return;
            }
            DarkModeTimeModeManager.this.updateDarkModeTimeModeStatus(context);
        }
    }

    /* loaded from: classes.dex */
    public class LocatedCityChangeObserver extends ContentObserver {
        private static final String TAG = "LocatedCityChangeObserver";
        private Context mContext;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public LocatedCityChangeObserver(Handler handler, Context context) {
            super(handler);
            DarkModeTimeModeManager.this = r1;
            this.mContext = context;
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            Slog.i(TAG, "onChange");
            super.onChange(selfChange, uri);
            Context context = this.mContext;
            if (context != null) {
                DarkModeTimeModeHelper.updateDarkModeSuntime(context);
                DarkModeTimeModeHelper.sendDarkModeSunTimeBroadcast(this.mContext);
            }
        }
    }
}
