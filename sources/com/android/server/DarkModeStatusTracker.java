package com.android.server;

import android.app.AlarmManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.MiuiSettings;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Slog;
import android.util.TimeUtils;
import com.miui.darkmode.DarkModeAppData;
import com.miui.darkmode.DarkModeAppDetailInfo;
import com.miui.server.smartpower.SmartPowerPolicyManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/* loaded from: classes.dex */
public class DarkModeStatusTracker {
    public static boolean DEBUG = false;
    private static final long ONE_MINUTE = 60000;
    private static volatile DarkModeStatusTracker sInstance;
    private final String TAG = "DarkModeStatusTracker";
    private AlarmManager.OnAlarmListener mAlarmListener = new AlarmManager.OnAlarmListener() { // from class: com.android.server.DarkModeStatusTracker.2
        @Override // android.app.AlarmManager.OnAlarmListener
        public void onAlarm() {
            DarkModeStatusTracker.this.uploadDarkModeStatusEvent(DarkModeOneTrackHelper.TIP_DARK_MODE_STATUS);
            DarkModeStatusTracker.this.uploadDarkModeStatusEvent(DarkModeOneTrackHelper.TIP_APP_STATUS);
            DarkModeStatusTracker.this.setUploadDarkModeSwitchAlarm(false);
        }
    };
    private AlarmManager mAlarmManager;
    private Context mContext;
    private Handler mDarkModeStatusHandler;
    private DarkModeStauesEvent mDarkModeSwitchEvent;
    private ForceDarkAppListManager mForceDarkAppListManager;
    private PackageManager mPackageManager;
    private ContentObserver mSettingsObserver;

    public static DarkModeStatusTracker getIntance() {
        if (sInstance == null) {
            synchronized (DarkModeStatusTracker.class) {
                if (sInstance == null) {
                    sInstance = new DarkModeStatusTracker();
                }
            }
        }
        return sInstance;
    }

    public void init(Context context, ForceDarkAppListManager forceDarkAppListManager) {
        this.mContext = context;
        this.mDarkModeStatusHandler = new Handler(MiuiBgThread.getHandler().getLooper());
        this.mSettingsObserver = new SettingsObserver(this.mDarkModeStatusHandler);
        this.mAlarmManager = (AlarmManager) this.mContext.getSystemService("alarm");
        this.mPackageManager = this.mContext.getPackageManager();
        this.mForceDarkAppListManager = forceDarkAppListManager;
        registerSettingsObserver();
        setUploadDarkModeSwitchAlarm(true);
        DarkModeOneTrackHelper.setDataDisableRegion(DarkModeSuggestProvider.getInstance().updateCloudDataForDisableRegion(this.mContext));
    }

    private List<String> initDarkModeAppList() {
        DarkModeAppData mDarkModeAppData = this.mForceDarkAppListManager.getDarkModeAppList(0L, UserHandle.myUserId());
        List<DarkModeAppDetailInfo> appInfoList = mDarkModeAppData.getDarkModeAppDetailInfoList();
        List<String> appList = new ArrayList<>();
        if (!appInfoList.isEmpty()) {
            for (DarkModeAppDetailInfo info : appInfoList) {
                try {
                    Map<String, String> appStatus = new HashMap<>();
                    String appName = this.mPackageManager.getPackageInfo(info.getPkgName(), 0).applicationInfo.loadLabel(this.mPackageManager).toString();
                    appStatus.put(DarkModeOneTrackHelper.PARAM_VALUE_APP_PKG, info.getPkgName());
                    appStatus.put(DarkModeOneTrackHelper.PARAM_VALUE_APP_NAME, appName);
                    appStatus.put(DarkModeOneTrackHelper.PARAM_VALUE_APP_SWITCH_STATUS, info.isEnabled() ? DarkModeOneTrackHelper.PARAM_VALUE_OPEN : DarkModeOneTrackHelper.PARAM_VALUE_CLOSE);
                    appList.add(appStatus.toString());
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
        return appList;
    }

    private void registerSettingsObserver() {
        ContentResolver observer = this.mContext.getContentResolver();
        observer.registerContentObserver(Settings.System.getUriFor(DarkModeTimeModeManager.DARK_MODE_ENABLE), false, this.mSettingsObserver);
        observer.registerContentObserver(Settings.System.getUriFor("dark_mode_time_enable"), false, this.mSettingsObserver);
        observer.registerContentObserver(Settings.System.getUriFor("dark_mode_time_type"), false, this.mSettingsObserver);
        observer.registerContentObserver(Settings.System.getUriFor("dark_mode_contrast_enable"), false, this.mSettingsObserver);
        observer.registerContentObserver(Settings.System.getUriFor("darken_wallpaper_under_dark_mode"), false, this.mSettingsObserver);
        observer.registerContentObserver(Settings.System.getUriFor("last_app_dark_mode_pkg"), false, this.mSettingsObserver);
        observer.registerContentObserver(MiuiSettings.SettingsCloudData.getCloudDataNotifyUri(), false, new ContentObserver(MiuiBgThread.getHandler()) { // from class: com.android.server.DarkModeStatusTracker.1
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                super.onChange(selfChange);
                DarkModeOneTrackHelper.setDataDisableRegion(DarkModeSuggestProvider.getInstance().updateCloudDataForDisableRegion(DarkModeStatusTracker.this.mContext));
            }
        });
    }

    public void uploadDarkModeStatusEvent(String eventTip) {
        DarkModeStauesEvent updateSwitchEvent = updateSwitchEvent(eventTip);
        this.mDarkModeSwitchEvent = updateSwitchEvent;
        uploadSwitchToOnetrack(this.mContext, updateSwitchEvent);
        if (DEBUG) {
            Slog.d("DarkModeStatusTracker", "uploadDarkModeStatusEvent " + this.mDarkModeSwitchEvent.toString());
        }
    }

    private DarkModeStauesEvent updateSwitchEvent(String tip) {
        if (TextUtils.isEmpty(tip)) {
            return null;
        }
        DarkModeStauesEvent event = new DarkModeStauesEvent().setTip(tip);
        char c = 65535;
        switch (tip.hashCode()) {
            case -1558836096:
                if (tip.equals(DarkModeOneTrackHelper.TIP_DARK_MODE_STATUS)) {
                    c = 0;
                    break;
                }
                break;
            case -1548332969:
                if (tip.equals(DarkModeOneTrackHelper.TIP_TIME_MODE_SETTING)) {
                    c = 2;
                    break;
                }
                break;
            case -857771494:
                if (tip.equals(DarkModeOneTrackHelper.TIP_WALL_PAPER_SETTING)) {
                    c = 3;
                    break;
                }
                break;
            case -847267704:
                if (tip.equals(DarkModeOneTrackHelper.TIP_DARK_MODE_SETTING)) {
                    c = 6;
                    break;
                }
                break;
            case -177713122:
                if (tip.equals(DarkModeOneTrackHelper.TIP_APP_SETTING)) {
                    c = 5;
                    break;
                }
                break;
            case 928676759:
                if (tip.equals(DarkModeOneTrackHelper.TIP_APP_STATUS)) {
                    c = 1;
                    break;
                }
                break;
            case 1629741340:
                if (tip.equals(DarkModeOneTrackHelper.TIP_CONTRAST_SETTING)) {
                    c = 4;
                    break;
                }
                break;
            case 1650747551:
                if (tip.equals(DarkModeOneTrackHelper.TIP_AUTO_SWITCH)) {
                    c = 7;
                    break;
                }
                break;
        }
        switch (c) {
            case 0:
                updateDarkModeStatus(event);
                break;
            case 1:
                updateAppStatus(event);
                break;
            case 2:
                updateTimeModeSetting(event);
                break;
            case 3:
                updateWallPaperSetting(event);
                break;
            case 4:
                updateContrastSetting(event);
                break;
            case 5:
                updateAppSetting(event);
                break;
            case 6:
                updateDarkModeSetting(event);
                break;
            case 7:
                updateAutoSwitch(event);
                break;
        }
        return event;
    }

    private void updateDarkModeStatus(DarkModeStauesEvent event) {
        event.setEventName(DarkModeOneTrackHelper.EVENT_NAME_STATUS);
        boolean isDarkModeEnable = DarkModeTimeModeHelper.isDarkModeEnable(this.mContext);
        String str = DarkModeOneTrackHelper.PARAM_VALUE_OPEN;
        event.setDarkModeStatus(isDarkModeEnable ? str : DarkModeOneTrackHelper.PARAM_VALUE_CLOSE);
        putTimeModeStatus(event);
        if (DarkModeTimeModeHelper.isDarkModeOpen(this.mContext)) {
            event.setWallPaperStatus(DarkModeTimeModeHelper.isDarkWallpaperModeEnable(this.mContext) ? str : DarkModeOneTrackHelper.PARAM_VALUE_CLOSE);
            if (!DarkModeTimeModeHelper.isDarkModeContrastEnable(this.mContext)) {
                str = DarkModeOneTrackHelper.PARAM_VALUE_CLOSE;
            }
            event.setContrastStatus(str);
        }
    }

    private void updateAppStatus(DarkModeStauesEvent event) {
        if (DarkModeTimeModeHelper.isDarkModeOpen(this.mContext)) {
            event.setEventName(DarkModeOneTrackHelper.EVENT_NAME_STATUS);
            event.setAppList(initDarkModeAppList());
        }
    }

    private void updateTimeModeSetting(DarkModeStauesEvent event) {
        event.setEventName(DarkModeOneTrackHelper.EVENT_NAME_SETTING);
        putTimeModeStatus(event);
        if (DarkModeTimeModeHelper.isDarkModeTimeEnable(this.mContext) && DarkModeTimeModeHelper.isSuntimeType(this.mContext)) {
            event.setSettingChannel(Settings.System.getString(this.mContext.getContentResolver(), "open_sun_time_channel"));
        }
    }

    private void updateWallPaperSetting(DarkModeStauesEvent event) {
        event.setEventName(DarkModeOneTrackHelper.EVENT_NAME_SETTING);
        if (DarkModeTimeModeHelper.isDarkModeOpen(this.mContext)) {
            event.setWallPaperStatus(DarkModeTimeModeHelper.isDarkWallpaperModeEnable(this.mContext) ? DarkModeOneTrackHelper.PARAM_VALUE_OPEN : DarkModeOneTrackHelper.PARAM_VALUE_CLOSE);
        }
    }

    private void updateContrastSetting(DarkModeStauesEvent event) {
        event.setEventName(DarkModeOneTrackHelper.EVENT_NAME_SETTING);
        if (DarkModeTimeModeHelper.isDarkModeOpen(this.mContext)) {
            event.setContrastStatus(DarkModeTimeModeHelper.isDarkModeContrastEnable(this.mContext) ? DarkModeOneTrackHelper.PARAM_VALUE_OPEN : DarkModeOneTrackHelper.PARAM_VALUE_CLOSE);
        }
    }

    private void updateAppSetting(DarkModeStauesEvent event) {
        event.setEventName(DarkModeOneTrackHelper.EVENT_NAME_SETTING);
        String appSetting = Settings.System.getString(this.mContext.getContentResolver(), "last_app_dark_mode_pkg");
        if (appSetting == null) {
            Slog.i("DarkModeStatusTracker", "get app setting error");
            return;
        }
        String[] appInfo = appSetting.split(":");
        if (appInfo == null || appInfo.length != 2) {
            Slog.i("DarkModeStatusTracker", "get app setting info error");
            return;
        }
        String appPkg = appInfo[0];
        String appEnable = appInfo[1];
        String appName = null;
        try {
            appName = this.mPackageManager.getPackageInfo(appPkg, 0).applicationInfo.loadLabel(this.mPackageManager).toString();
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        event.setAppName(appName);
        event.setAppPkg(appPkg);
        event.setAppEnable("true".equals(appEnable) ? DarkModeOneTrackHelper.PARAM_VALUE_OPEN : DarkModeOneTrackHelper.PARAM_VALUE_CLOSE);
    }

    private void updateDarkModeSetting(DarkModeStauesEvent event) {
        event.setEventName(DarkModeOneTrackHelper.EVENT_NAME_SETTING);
        event.setDarkModeStatus(DarkModeTimeModeHelper.isDarkModeEnable(this.mContext) ? DarkModeOneTrackHelper.PARAM_VALUE_OPEN : DarkModeOneTrackHelper.PARAM_VALUE_CLOSE);
        event.setSettingChannel(Settings.System.getStringForUser(this.mContext.getContentResolver(), "open_dark_mode_channel", 0));
    }

    private void updateAutoSwitch(DarkModeStauesEvent event) {
        event.setEventName(DarkModeOneTrackHelper.EVENT_NAME_AUTO_SWITCH);
        event.setAutoSwitch(DarkModeTimeModeHelper.isDarkModeEnable(this.mContext) ? DarkModeOneTrackHelper.PARAM_VALUE_SWITCH_OPEN : DarkModeOneTrackHelper.PARAM_VALUE_SWITCH_CLOSE);
    }

    private void putTimeModeStatus(DarkModeStauesEvent event) {
        event.setTimeModeStatus(DarkModeTimeModeHelper.isDarkModeTimeEnable(this.mContext) ? DarkModeOneTrackHelper.PARAM_VALUE_OPEN : DarkModeOneTrackHelper.PARAM_VALUE_CLOSE);
        if (DarkModeTimeModeHelper.isDarkModeTimeEnable(this.mContext)) {
            event.setTimeModePattern(DarkModeTimeModeHelper.isSuntimeType(this.mContext) ? DarkModeOneTrackHelper.PARAM_VALUE_TWILIGHT : DarkModeOneTrackHelper.PARAM_VALUE_CUSTOM);
            int startTime = DarkModeTimeModeHelper.isSuntimeType(this.mContext) ? DarkModeTimeModeHelper.getSunSetTime(this.mContext) : DarkModeTimeModeHelper.getDarkModeStartTime(this.mContext);
            int endTime = DarkModeTimeModeHelper.isSuntimeType(this.mContext) ? DarkModeTimeModeHelper.getSunRiseTime(this.mContext) : DarkModeTimeModeHelper.getDarkModeEndTime(this.mContext);
            event.setBeginTime(DarkModeTimeModeHelper.getTimeInString(startTime));
            event.setEndTime(DarkModeTimeModeHelper.getTimeInString(endTime));
        }
    }

    private void uploadSwitchToOnetrack(Context context, DarkModeEvent event) {
        if (event != null) {
            DarkModeOneTrackHelper.uploadToOneTrack(context, event);
        }
    }

    public void setUploadDarkModeSwitchAlarm(boolean init) {
        long j;
        long nowTime = System.currentTimeMillis();
        if (DEBUG) {
            j = 60000;
        } else {
            j = init ? SmartPowerPolicyManager.UPDATE_USAGESTATS_DURATION : 86400000L;
        }
        long nextTime = j + nowTime;
        this.mAlarmManager.setExact(1, nextTime, "upload_dark_mode_switch", this.mAlarmListener, this.mDarkModeStatusHandler);
        if (DEBUG) {
            Slog.d("DarkModeStatusTracker", "setUploadDarkModeSwitchAlarm nextTime = " + TimeUtils.formatDuration(nextTime));
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class SettingsObserver extends ContentObserver {
        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public SettingsObserver(Handler handler) {
            super(handler);
            DarkModeStatusTracker.this = this$0;
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            if (uri == null) {
                return;
            }
            super.onChange(selfChange, uri);
            Slog.i("DarkModeStatusTracker", "uri = " + uri.getLastPathSegment());
            String lastPathSegment = uri.getLastPathSegment();
            char c = 65535;
            switch (lastPathSegment.hashCode()) {
                case -1456256958:
                    if (lastPathSegment.equals("dark_mode_time_enable")) {
                        c = 1;
                        break;
                    }
                    break;
                case -1268415507:
                    if (lastPathSegment.equals("dark_mode_contrast_enable")) {
                        c = 4;
                        break;
                    }
                    break;
                case -715052970:
                    if (lastPathSegment.equals(DarkModeTimeModeManager.DARK_MODE_ENABLE)) {
                        c = 0;
                        break;
                    }
                    break;
                case 1230086728:
                    if (lastPathSegment.equals("darken_wallpaper_under_dark_mode")) {
                        c = 3;
                        break;
                    }
                    break;
                case 1563186617:
                    if (lastPathSegment.equals("dark_mode_time_type")) {
                        c = 2;
                        break;
                    }
                    break;
                case 2003212498:
                    if (lastPathSegment.equals("last_app_dark_mode_pkg")) {
                        c = 5;
                        break;
                    }
                    break;
            }
            switch (c) {
                case 0:
                    if (Settings.System.getIntForUser(DarkModeStatusTracker.this.mContext.getContentResolver(), "dark_mode_enable_by_setting", 0, 0) == 0) {
                        DarkModeStatusTracker.this.uploadDarkModeStatusEvent(DarkModeOneTrackHelper.TIP_AUTO_SWITCH);
                        return;
                    } else {
                        DarkModeStatusTracker.this.uploadDarkModeStatusEvent(DarkModeOneTrackHelper.TIP_DARK_MODE_SETTING);
                        return;
                    }
                case 1:
                    DarkModeStatusTracker.this.uploadDarkModeStatusEvent(DarkModeOneTrackHelper.TIP_TIME_MODE_SETTING);
                    return;
                case 2:
                    DarkModeStatusTracker.this.uploadDarkModeStatusEvent(DarkModeOneTrackHelper.TIP_TIME_MODE_SETTING);
                    return;
                case 3:
                    DarkModeStatusTracker.this.uploadDarkModeStatusEvent(DarkModeOneTrackHelper.TIP_WALL_PAPER_SETTING);
                    return;
                case 4:
                    DarkModeStatusTracker.this.uploadDarkModeStatusEvent(DarkModeOneTrackHelper.TIP_CONTRAST_SETTING);
                    return;
                case 5:
                    DarkModeStatusTracker.this.uploadDarkModeStatusEvent(DarkModeOneTrackHelper.TIP_APP_SETTING);
                    return;
                default:
                    return;
            }
        }
    }
}
