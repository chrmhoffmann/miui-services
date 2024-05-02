package com.android.server.display.statistics;

import android.app.ActivityManager;
import android.app.ActivityThread;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Slog;
import com.android.server.DarkModeOneTrackHelper;
import com.android.server.MiuiBatteryStatsService;
import com.android.server.MiuiBgThread;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import miui.process.ForegroundInfo;
import miui.process.IForegroundWindowListener;
import miui.process.ProcessManager;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
/* loaded from: classes.dex */
public class OneTrackFoldStateHelper {
    private static final String APP_ID = "31000000437";
    private static final String EVENT_NAME = "fold_state";
    private static final int FLAG_NON_ANONYMOUS = 2;
    private static final int FLAG_NOT_LIMITED_BY_USER_EXPERIENCE_PLAN = 1;
    private static final int MSG_ON_FOLD_CHANGED = 1;
    private static final int MSG_ON_SCREEN_ON_OFF = 2;
    private static final int MSG_ON_WINDOW_CHANGED = 3;
    private static final String ONETRACK_ACTION = "onetrack.action.TRACK_EVENT";
    private static final String ONETRACK_PACKAGE_NAME = "com.miui.analytics";
    private static final int ONE_SECOND = 1000;
    private static final String ONE_TRACE_FOLD_TIMES = "one_trace_fold_times";
    private static final long ONE_TRACE_INTERNAL = 21600000;
    private static final long ONE_TRACE_INTERNAL_DEBUG = 360000;
    private static final String ONE_TRACE_SEP = "-";
    private static final String PACKAGE = "android";
    private static final String TAG = "OneTrackFoldStateHelper";
    private static final int TEN_SECONDS_INTERNAL = 10000;
    private static final int TWO_SECONDS_INTERNAL = 2000;
    private static volatile OneTrackFoldStateHelper sInstance;
    private ActivityManager mActivityManager;
    private Context mApplicationContext;
    private BroadcastReceiver mBroadcastReceiver;
    private String mCurPkgName;
    private boolean mFolded;
    private int mFoldingIntervalGreaterThanTenSeconds;
    private int mFoldingIntervalGreaterThanTwoSecondsAndLessThanTenSeconds;
    private int mFoldingIntervalLessThanTwoSeconds;
    private Handler mHandler;
    private long mLastReportTime;
    private PowerManager mPowerManager;
    private long mReportInternal;
    private ContentResolver mResolver;
    private boolean mScreenOn;
    private long mScreenOnStartTime;
    private long mScreenOnStartTimeLarge;
    private long mScreenOnStartTimeSmall;
    private long mStartRecordTime;
    private IForegroundWindowListener mWindowListener;
    private static final boolean DEBUG = SystemProperties.getBoolean("persist.track.fold.state", false);
    private static final boolean IS_INTERNATIONAL_BUILD = SystemProperties.get("ro.product.mod_device", "").contains("_global");
    private int mReportTimes = -1;
    private Map<String, int[]> mAppsTimeMap = new HashMap();

    public static OneTrackFoldStateHelper getInstance() {
        if (sInstance == null) {
            synchronized (OneTrackFoldStateHelper.class) {
                if (sInstance == null) {
                    sInstance = new OneTrackFoldStateHelper();
                }
            }
        }
        return sInstance;
    }

    public void oneTrackFoldState(boolean folded) {
        if (DEBUG) {
            Slog.d(TAG, "oneTrackFoldState folded=" + folded);
        }
        Message message = Message.obtain();
        message.what = 1;
        message.obj = Boolean.valueOf(folded);
        this.mHandler.sendMessage(message);
    }

    private OneTrackFoldStateHelper() {
        this.mReportInternal = DEBUG ? ONE_TRACE_INTERNAL_DEBUG : ONE_TRACE_INTERNAL;
        this.mApplicationContext = ActivityThread.currentActivityThread().getApplication();
        this.mHandler = new OneTrackFoldStateHelperHandler(MiuiBgThread.get().getLooper());
        this.mBroadcastReceiver = new BroadcastReceiver() { // from class: com.android.server.display.statistics.OneTrackFoldStateHelper.1
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                if (OneTrackFoldStateHelper.DEBUG) {
                    Slog.d(OneTrackFoldStateHelper.TAG, "onReceive intent=" + intent);
                }
                Message message = Message.obtain();
                message.what = 2;
                message.obj = intent;
                OneTrackFoldStateHelper.this.mHandler.sendMessage(message);
            }
        };
        this.mWindowListener = new IForegroundWindowListener.Stub() { // from class: com.android.server.display.statistics.OneTrackFoldStateHelper.2
            public void onForegroundWindowChanged(ForegroundInfo foregroundInfo) {
                if (OneTrackFoldStateHelper.DEBUG) {
                    Slog.d(OneTrackFoldStateHelper.TAG, "onForegroundWindowChanged pkgname=" + foregroundInfo.mForegroundPackageName);
                }
                Message message = Message.obtain();
                message.what = 3;
                message.obj = foregroundInfo;
                OneTrackFoldStateHelper.this.mHandler.sendMessage(message);
            }
        };
        registerReceiverScreenOnOff();
        registerForegroundWindowListener();
    }

    private void registerReceiverScreenOnOff() {
        this.mActivityManager = (ActivityManager) this.mApplicationContext.getSystemService("activity");
        PowerManager powerManager = (PowerManager) this.mApplicationContext.getSystemService("power");
        this.mPowerManager = powerManager;
        if (powerManager != null) {
            this.mScreenOn = powerManager.isScreenOn();
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.SCREEN_OFF");
        filter.addAction("android.intent.action.USER_PRESENT");
        this.mApplicationContext.registerReceiver(this.mBroadcastReceiver, filter);
    }

    private void registerForegroundWindowListener() {
        ProcessManager.registerForegroundWindowListener(this.mWindowListener);
    }

    public void handleFoldChanged(boolean folded) {
        if (DEBUG) {
            Slog.d(TAG, "handleFoldChanged folded=" + folded);
        }
        ContentResolver contentResolver = this.mApplicationContext.getContentResolver();
        this.mResolver = contentResolver;
        if (this.mReportTimes == -1) {
            this.mFolded = folded;
            initFoldData(contentResolver);
            long j = 0;
            long currentTimeMillis = this.mScreenOn ? System.currentTimeMillis() : 0L;
            this.mScreenOnStartTime = currentTimeMillis;
            boolean z = this.mScreenOn;
            this.mScreenOnStartTimeSmall = (!z || !this.mFolded) ? 0L : currentTimeMillis;
            if (z && !this.mFolded) {
                j = currentTimeMillis;
            }
            this.mScreenOnStartTimeLarge = j;
        } else if (this.mFolded == folded) {
            Slog.e(TAG, "oneTrackFoldState no change mFolded=" + this.mFolded);
        } else {
            this.mFolded = folded;
            computeFoldData();
            computeAppTime();
            reportOneTrack();
            saveToSettings(this.mResolver);
        }
    }

    public void handleScreenOnOff(Intent intent) {
        String action = intent.getAction();
        if (DEBUG) {
            Slog.d(TAG, "handleScreenOnOff onReceive action=" + action);
        }
        if ("android.intent.action.USER_PRESENT".equals(action)) {
            onScreenStateChanged(true);
        } else if ("android.intent.action.SCREEN_OFF".equals(action)) {
            onScreenStateChanged(false);
        }
    }

    public void handleWindowChanged(ForegroundInfo foregroundInfo) {
        String packageName = foregroundInfo.mForegroundPackageName;
        if (DEBUG) {
            Slog.d(TAG, "handleWindowChanged pkgName=" + packageName + ", mCurPkgName=" + this.mCurPkgName);
        }
        if (!TextUtils.equals(this.mCurPkgName, packageName)) {
            computeAppTime();
            reportOneTrack();
            this.mCurPkgName = packageName;
        }
    }

    private void initFoldData(ContentResolver resolver) {
        String timesString = Settings.Global.getString(resolver, ONE_TRACE_FOLD_TIMES);
        if (!TextUtils.isEmpty(timesString)) {
            String[] timesInt = timesString.split(ONE_TRACE_SEP);
            if (timesInt != null && timesInt.length == 5) {
                this.mReportTimes = Integer.parseInt(timesInt[0]);
                this.mFoldingIntervalLessThanTwoSeconds = Integer.parseInt(timesInt[1]);
                this.mFoldingIntervalGreaterThanTwoSecondsAndLessThanTenSeconds = Integer.parseInt(timesInt[2]);
                this.mFoldingIntervalGreaterThanTenSeconds = Integer.parseInt(timesInt[3]);
                this.mStartRecordTime = Long.parseLong(timesInt[4]);
            } else {
                Slog.e(TAG, "initFoldData parse args error");
                this.mReportTimes = 0;
            }
        } else {
            Slog.e(TAG, "initFoldData timesString is null");
            this.mReportTimes = 0;
        }
        this.mCurPkgName = getTopAppName();
        Slog.d(TAG, "initFoldData timesString=" + timesString + ", mReportTimes=" + this.mReportTimes + "(" + this.mFoldingIntervalLessThanTwoSeconds + "|" + this.mFoldingIntervalGreaterThanTwoSecondsAndLessThanTenSeconds + "|" + this.mFoldingIntervalGreaterThanTenSeconds + "), mStartRecordTime=" + this.mStartRecordTime + ", mCurPkgName=" + this.mCurPkgName);
    }

    private void computeFoldData() {
        long now = System.currentTimeMillis();
        if (this.mStartRecordTime == 0) {
            Slog.e(TAG, "computeFoldData init mStartRecordTime to " + now);
            this.mStartRecordTime = now;
        }
        long interval = now - this.mLastReportTime;
        this.mLastReportTime = now;
        this.mReportTimes++;
        if (interval <= 2000) {
            this.mFoldingIntervalLessThanTwoSeconds++;
        } else if (interval <= 10000) {
            this.mFoldingIntervalGreaterThanTwoSecondsAndLessThanTenSeconds++;
        } else {
            this.mFoldingIntervalGreaterThanTenSeconds++;
        }
        if (DEBUG) {
            Slog.d(TAG, "computeFoldData mFolded=" + this.mFolded + ", mReportTimes=" + this.mReportTimes + "(" + this.mFoldingIntervalLessThanTwoSeconds + "|" + this.mFoldingIntervalGreaterThanTwoSecondsAndLessThanTenSeconds + "|" + this.mFoldingIntervalGreaterThanTenSeconds + "), now=" + now + ", interval=" + interval);
        }
    }

    private void saveToSettings(ContentResolver resolver) {
        StringBuilder record = new StringBuilder();
        record.append(this.mReportTimes).append(ONE_TRACE_SEP).append(this.mFoldingIntervalLessThanTwoSeconds).append(ONE_TRACE_SEP).append(this.mFoldingIntervalGreaterThanTwoSecondsAndLessThanTenSeconds).append(ONE_TRACE_SEP).append(this.mFoldingIntervalGreaterThanTenSeconds).append(ONE_TRACE_SEP).append(this.mStartRecordTime);
        Settings.Global.putString(resolver, ONE_TRACE_FOLD_TIMES, record.toString());
    }

    private void reportOneTrack() {
        long now = System.currentTimeMillis();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss");
        String startTime = simpleDateFormat.format(new Date(this.mStartRecordTime));
        String endTime = simpleDateFormat.format(new Date(now));
        Intent intent = new Intent("onetrack.action.TRACK_EVENT");
        if (now - this.mStartRecordTime < this.mReportInternal) {
            return;
        }
        intent.setPackage("com.miui.analytics").putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_APP_ID, APP_ID).putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_PACKAGE, PACKAGE).putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_EVENT_NAME, EVENT_NAME).putExtra("fold_start_time", startTime).putExtra("fold_end_time", endTime).putExtra("fold_times", this.mReportTimes).putExtra("fold_interval_2", this.mFoldingIntervalLessThanTwoSeconds).putExtra("fold_interval_2_10", this.mFoldingIntervalGreaterThanTwoSecondsAndLessThanTenSeconds).putExtra("fold_interval_10", this.mFoldingIntervalGreaterThanTenSeconds);
        addAppDataToIntent(intent);
        if (!IS_INTERNATIONAL_BUILD) {
            intent.setFlags(3);
        }
        Slog.d(TAG, "reportOneTrack data: " + intent.getExtras());
        try {
            this.mApplicationContext.startServiceAsUser(intent, UserHandle.CURRENT);
            this.mStartRecordTime = now;
            this.mReportTimes = 0;
            this.mFoldingIntervalLessThanTwoSeconds = 0;
            this.mFoldingIntervalGreaterThanTwoSecondsAndLessThanTenSeconds = 0;
            this.mFoldingIntervalGreaterThanTenSeconds = 0;
            this.mAppsTimeMap.clear();
        } catch (Exception e) {
            Slog.e(TAG, "reportOneTrack Failed to upload!");
        }
    }

    private void addAppDataToIntent(Intent intent) {
        if (this.mAppsTimeMap.size() < 1) {
            Slog.d(TAG, "addAppDataToIntent mAppsTimeMap size < 1, return.");
            return;
        }
        JSONArray allAppDatas = new JSONArray();
        for (Map.Entry<String, int[]> entry : this.mAppsTimeMap.entrySet()) {
            try {
                if (!TextUtils.isEmpty(entry.getKey())) {
                    JSONObject appData = new JSONObject();
                    appData.put(DarkModeOneTrackHelper.PARAM_VALUE_APP_NAME, entry.getKey());
                    appData.put("small", entry.getValue()[0]);
                    appData.put("large", entry.getValue()[1]);
                    allAppDatas.put(appData);
                }
            } catch (JSONException e) {
                Slog.e(TAG, "addAppDataToIntent JSONException e=" + e);
            }
        }
        intent.putExtra("app_use_screen_time", allAppDatas.toString());
        if (DEBUG) {
            Slog.d(TAG, "addAppDataToIntent intent.getExtras=" + intent.getExtras());
        }
    }

    private void onScreenStateChanged(boolean screenOn) {
        if (this.mScreenOn == screenOn) {
            return;
        }
        long j = 0;
        if (screenOn) {
            this.mCurPkgName = getTopAppName();
            long currentTimeMillis = System.currentTimeMillis();
            this.mScreenOnStartTime = currentTimeMillis;
            boolean z = this.mFolded;
            this.mScreenOnStartTimeSmall = z ? currentTimeMillis : 0L;
            if (!z) {
                j = currentTimeMillis;
            }
            this.mScreenOnStartTimeLarge = j;
            if (DEBUG) {
                Slog.d(TAG, "onScreenStateChanged screen on, visible app: " + this.mCurPkgName);
            }
        } else {
            computeAppTime();
            reportOneTrack();
            this.mScreenOnStartTime = 0L;
            this.mScreenOnStartTimeSmall = 0L;
            this.mScreenOnStartTimeLarge = 0L;
        }
        this.mScreenOn = screenOn;
    }

    private String getTopAppName() {
        try {
            if (this.mActivityManager.getRunningTasks(1) == null || this.mActivityManager.getRunningTasks(1).get(0) == null) {
                return "";
            }
            ComponentName cn = this.mActivityManager.getRunningTasks(1).get(0).topActivity;
            String packageName = cn.getPackageName();
            return packageName;
        } catch (IndexOutOfBoundsException e) {
            Slog.e(TAG, "getTopAppName exception:" + e.toString());
            return "";
        } catch (Exception e2) {
            Slog.e(TAG, "getTopAppName exception:" + e2.toString());
            return "";
        }
    }

    private void computeAppTime() {
        if (!this.mScreenOn) {
            return;
        }
        long now = System.currentTimeMillis();
        int[] value = this.mAppsTimeMap.get(this.mCurPkgName);
        if (value == null) {
            value = new int[2];
            this.mAppsTimeMap.put(this.mCurPkgName, value);
        }
        if (this.mFolded) {
            long j = this.mScreenOnStartTimeSmall;
            if (j == 0) {
                long j2 = this.mScreenOnStartTimeLarge;
                if (j2 != 0) {
                    value[1] = value[1] + (((int) (now - j2)) / 1000);
                    this.mScreenOnStartTimeSmall = now;
                    this.mScreenOnStartTimeLarge = 0L;
                }
            }
            if (j != 0 && this.mScreenOnStartTimeLarge == 0) {
                value[0] = value[0] + (((int) (now - j)) / 1000);
                this.mScreenOnStartTimeSmall = now;
            }
        } else {
            long j3 = this.mScreenOnStartTimeLarge;
            if (j3 == 0) {
                long j4 = this.mScreenOnStartTimeSmall;
                if (j4 != 0) {
                    value[0] = value[0] + (((int) (now - j4)) / 1000);
                    this.mScreenOnStartTimeLarge = now;
                    this.mScreenOnStartTimeSmall = 0L;
                }
            }
            if (j3 != 0 && this.mScreenOnStartTimeSmall == 0) {
                value[1] = value[1] + (((int) (now - j3)) / 1000);
                this.mScreenOnStartTimeLarge = now;
            }
        }
        if (DEBUG) {
            Slog.d(TAG, "computeAppTime " + this.mCurPkgName + " time(" + value[0] + "|" + value[1] + ")");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class OneTrackFoldStateHelperHandler extends Handler {
        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public OneTrackFoldStateHelperHandler(Looper looper) {
            super(looper, null);
            OneTrackFoldStateHelper.this = r1;
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    OneTrackFoldStateHelper.this.handleFoldChanged(((Boolean) msg.obj).booleanValue());
                    return;
                case 2:
                    OneTrackFoldStateHelper.this.handleScreenOnOff((Intent) msg.obj);
                    return;
                case 3:
                    OneTrackFoldStateHelper.this.handleWindowChanged((ForegroundInfo) msg.obj);
                    return;
                default:
                    return;
            }
        }
    }
}
