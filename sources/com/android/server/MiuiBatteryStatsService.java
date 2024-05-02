package com.android.server;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.FileUtils;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UEventObserver;
import android.os.UserHandle;
import android.server.am.SplitScreenReporter;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.AtomicFile;
import android.util.Slog;
import com.android.internal.util.ArrayUtils;
import com.android.server.am.ActivityManagerServiceImpl;
import com.android.server.am.BroadcastQueueImpl;
import com.android.server.display.TemperatureController;
import com.miui.server.MiuiDfcService;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import miui.os.Build;
import miui.util.IMiCharge;
/* loaded from: classes.dex */
public class MiuiBatteryStatsService {
    public static final String ADJUST_VOLTAGE = "miui.intent.action.ADJUST_VOLTAGE";
    public static final String ADJUST_VOLTAGE_EXTRA = "miui.intent.extra.ADJUST_VOLTAGE";
    private static final long FIVEMIN = 300000;
    public static final String LIMIT_TIME = "miui.intent.action.LIMIT_TIME";
    private static final long TENMIN = 600000;
    public static final String UPDATE_BATTERY_DATA = "miui.intent.action.UPDATE_BATTERY_DATA";
    private final boolean DEBUG;
    private AlarmManager mAlarmManager;
    private boolean mBootCompleted;
    private final Context mContext;
    private int mLastBatteryTemp;
    private int mLastBatteryVoltage;
    private boolean mLastPlugged;
    private PendingIntent mPendingIntent;
    private PendingIntent mPendingIntentLimitTime;
    private boolean mSupportedCellVolt;
    private boolean mSupportedSB;
    private static final long DAY = SystemProperties.getInt("persist.sys.report_time", 86400) * 1000;
    private static volatile MiuiBatteryStatsService INSTANCE = null;
    private final String TAG = "MiuiBatteryStatsService";
    private long mChargeStartTime = 0;
    private long mChargeEndTime = 0;
    private long mFullChargeStartTime = 0;
    private long mFullChargeEndTime = 0;
    private long mScreenOnChargingStart = 0;
    private long mScreenOnTime = 0;
    private int mChargeStartCapacity = 0;
    private int mChargeEndCapacity = 0;
    private int mLastBatteryStatus = -1;
    private int mLastSoc = -1;
    private int mChargeMaxTemp = 0;
    private int mChargeMinTemp = 0;
    private int mDischargingCount = 0;
    private boolean mIsScreenOn = false;
    private boolean mStartRecordDischarging = false;
    private boolean mIsHandleIntermittentCharge = false;
    private BatteryTempVoltageTimeInfo mBatteryInfoFull = new BatteryTempVoltageTimeInfo("Full");
    private BatteryTempVoltageTimeInfo mBatteryInfoNormal = new BatteryTempVoltageTimeInfo("Normal");
    private BatteryTempSocTimeInfo mBatteryTempSocTime = new BatteryTempSocTimeInfo();
    private final BatteryStatsHandler mHandler = new BatteryStatsHandler(MiuiBgThread.get().getLooper());
    private MiuiAppUsageStats mAppUsageStats = new MiuiAppUsageStats();
    private boolean mIsTablet = Arrays.asList(SystemProperties.get("ro.build.characteristics", "")).contains("tablet");

    /* renamed from: -$$Nest$fgetDEBUG */
    static /* bridge */ /* synthetic */ boolean m103$$Nest$fgetDEBUG(MiuiBatteryStatsService miuiBatteryStatsService) {
        return miuiBatteryStatsService.DEBUG;
    }

    public static MiuiBatteryStatsService getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (MiuiBatteryStatsService.class) {
                if (INSTANCE == null) {
                    INSTANCE = new MiuiBatteryStatsService(context);
                }
            }
        }
        return INSTANCE;
    }

    public MiuiBatteryStatsService(Context context) {
        boolean z = SystemProperties.getBoolean("persist.sys.debug_stats", false);
        this.DEBUG = z;
        this.mContext = context;
        BroadcastReceiver stateChangedReceiver = new BroadcastReceiver() { // from class: com.android.server.MiuiBatteryStatsService.1
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                String action = intent.getAction();
                if (MiuiBatteryStatsService.this.DEBUG) {
                    Slog.d("MiuiBatteryStatsService", "action = " + action);
                }
                if (!"android.intent.action.BATTERY_CHANGED".equals(action)) {
                    if (MiuiBatteryStatsService.LIMIT_TIME.equals(action)) {
                        MiuiBatteryStatsService.this.mHandler.handleSlowCharge();
                        return;
                    } else if (MiuiDfcService.ACTION_TEST_USB_STATE.equals(action)) {
                        boolean configured = intent.getBooleanExtra("configured", false);
                        if (configured) {
                            MiuiBatteryStatsService.this.mHandler.sendMessageDelayed(3, intent, ActivityManagerServiceImpl.BOOST_DURATION);
                            return;
                        }
                        return;
                    } else if (MiuiBatteryStatsService.UPDATE_BATTERY_DATA.equals(action)) {
                        MiuiBatteryStatsService.this.mHandler.sendMessageDelayed(0, 0L);
                        MiuiBatteryStatsService.this.mHandler.sendMessageDelayed(7, 0L);
                        MiuiBatteryStatsService.this.mHandler.sendMessageDelayed(8, 0L);
                        MiuiBatteryStatsService.this.mAlarmManager.setExactAndAllowWhileIdle(2, SystemClock.elapsedRealtime() + MiuiBatteryStatsService.DAY, MiuiBatteryStatsService.this.mPendingIntent);
                        return;
                    } else if ("miui.intent.action.ACTION_SHUTDOWN_DELAY".equals(action)) {
                        if (intent.getIntExtra("miui.intent.extra.shutdown_delay", -1) == 1) {
                            MiuiBatteryStatsService.this.mHandler.sendMessageDelayed(5, false, 0L);
                            return;
                        }
                        return;
                    } else if ("android.intent.action.BOOT_COMPLETED".equals(action)) {
                        MiuiBatteryStatsService.this.mBootCompleted = true;
                        MiuiBatteryStatsService.this.mHandler.sendMessageDelayed(10, 0L);
                        return;
                    } else if ("android.intent.action.ACTION_SHUTDOWN".equals(action)) {
                        MiuiBatteryStatsService.this.mHandler.sendMessageDelayed(9, 0L);
                        return;
                    } else if ("android.intent.action.SCREEN_ON".equals(action)) {
                        MiuiBatteryStatsService.this.mIsScreenOn = true;
                        if (MiuiBatteryStatsService.this.mScreenOnChargingStart == 0 && MiuiBatteryStatsService.this.mLastPlugged) {
                            MiuiBatteryStatsService.this.mScreenOnChargingStart = System.currentTimeMillis();
                            return;
                        }
                        return;
                    } else if ("android.intent.action.SCREEN_OFF".equals(action)) {
                        MiuiBatteryStatsService.this.mIsScreenOn = false;
                        if (MiuiBatteryStatsService.this.mScreenOnChargingStart != 0 && MiuiBatteryStatsService.this.mLastPlugged) {
                            MiuiBatteryStatsService.this.mScreenOnTime += System.currentTimeMillis() - MiuiBatteryStatsService.this.mScreenOnChargingStart;
                            MiuiBatteryStatsService.this.mScreenOnChargingStart = 0L;
                            return;
                        }
                        return;
                    } else {
                        return;
                    }
                }
                int plugType = intent.getIntExtra("plugged", -1);
                int batteryStatus = intent.getIntExtra(DarkModeOneTrackHelper.EVENT_NAME_STATUS, 1);
                int batteryTemp = intent.getIntExtra(TemperatureController.STRATEGY_TEMPERATURE, 0);
                int batteryVoltage = intent.getIntExtra("voltage", 0);
                int soc = intent.getIntExtra("level", -1);
                boolean plugged = plugType > 0;
                if ((plugged && !MiuiBatteryStatsService.this.mLastPlugged) || (!plugged && MiuiBatteryStatsService.this.mLastPlugged)) {
                    if (plugged) {
                        MiuiBatteryStatsService.this.mChargeMaxTemp = batteryTemp;
                        MiuiBatteryStatsService.this.mChargeMinTemp = batteryTemp;
                        MiuiBatteryStatsService.this.mChargeStartTime = System.currentTimeMillis();
                        MiuiBatteryStatsService.this.mChargeStartCapacity = soc;
                        if (MiuiBatteryStatsService.this.mIsScreenOn) {
                            MiuiBatteryStatsService.this.mScreenOnChargingStart = System.currentTimeMillis();
                        }
                    } else {
                        MiuiBatteryStatsService.this.mChargeEndTime = System.currentTimeMillis();
                        MiuiBatteryStatsService.this.mChargeEndCapacity = intent.getIntExtra("level", -1);
                        if (MiuiBatteryStatsService.this.mScreenOnChargingStart != 0) {
                            MiuiBatteryStatsService.this.mScreenOnTime += System.currentTimeMillis() - MiuiBatteryStatsService.this.mScreenOnChargingStart;
                            MiuiBatteryStatsService.this.mScreenOnChargingStart = 0L;
                        }
                    }
                    MiuiBatteryStatsService.this.mHandler.updateChargeInfo(plugged);
                    if (!MiuiBatteryStatsService.this.mIsTablet) {
                        if (MiuiBatteryStatsService.this.DEBUG) {
                            Slog.d("MiuiBatteryStatsService", "plugType = " + plugType);
                        }
                        if (plugType == 1) {
                            MiuiBatteryStatsService.this.mHandler.sendMessageDelayed(12, 5000L);
                        } else if (plugType <= 0 && MiuiBatteryStatsService.this.mPendingIntentLimitTime != null) {
                            MiuiBatteryStatsService.this.mAlarmManager.cancel(MiuiBatteryStatsService.this.mPendingIntentLimitTime);
                        }
                    }
                }
                if (MiuiBatteryStatsService.this.mFullChargeStartTime == 0 && batteryStatus == 5 && MiuiBatteryStatsService.this.mLastBatteryStatus != 5) {
                    MiuiBatteryStatsService.this.mFullChargeStartTime = System.currentTimeMillis();
                } else if (batteryStatus != 5 && MiuiBatteryStatsService.this.mLastBatteryStatus == 5) {
                    MiuiBatteryStatsService.this.mFullChargeEndTime = System.currentTimeMillis();
                }
                if (MiuiBatteryStatsService.this.DEBUG) {
                    Slog.d("MiuiBatteryStatsService", "mLastBatteryTemp = " + MiuiBatteryStatsService.this.mLastBatteryTemp + ", batteryTemp = " + batteryTemp);
                }
                if (batteryTemp != MiuiBatteryStatsService.this.mLastBatteryTemp || batteryVoltage != MiuiBatteryStatsService.this.mLastBatteryVoltage || batteryStatus != MiuiBatteryStatsService.this.mLastBatteryStatus) {
                    if (MiuiBatteryStatsService.this.DEBUG) {
                        Slog.d("MiuiBatteryStatsService", "mLastBatteryVoltage = " + MiuiBatteryStatsService.this.mLastBatteryVoltage + ", batteryVoltage = " + batteryVoltage);
                    }
                    if (batteryTemp != MiuiBatteryStatsService.this.mLastBatteryTemp) {
                        MiuiBatteryStatsService.this.mHandler.sendMessageDelayed(6, intent, 0L);
                    }
                    if (batteryStatus == 5) {
                        MiuiBatteryStatsService.this.mBatteryInfoFull.collectTime(batteryTemp, batteryVoltage, batteryStatus);
                    } else {
                        MiuiBatteryStatsService.this.mBatteryInfoNormal.collectTime(batteryTemp, batteryVoltage, batteryStatus);
                    }
                }
                if (MiuiBatteryStatsService.this.mBootCompleted && (batteryTemp != MiuiBatteryStatsService.this.mLastBatteryTemp || soc != MiuiBatteryStatsService.this.mLastSoc)) {
                    if (MiuiBatteryStatsService.this.mSupportedSB) {
                        if (MiuiBatteryStatsService.this.DEBUG) {
                            Slog.d("MiuiBatteryStatsService", "mLastSoc=" + MiuiBatteryStatsService.this.mLastSoc + " soc=" + soc + " System.currentTimeMillis()=" + MiuiBatteryStatsService.this.mHandler.formatTime(System.currentTimeMillis()) + " stopCollectTempSocTime=" + MiuiBatteryStatsService.this.mHandler.formatTime(MiuiBatteryStatsService.this.mBatteryTempSocTime.stopCollectTempSocTime));
                        }
                        if (MiuiBatteryStatsService.this.mBatteryTempSocTime.stopCollectTempSocTime != 0) {
                            if (System.currentTimeMillis() < MiuiBatteryStatsService.this.mBatteryTempSocTime.stopCollectTempSocTime) {
                                MiuiBatteryStatsService.this.mBatteryTempSocTime.collectTempSocData(batteryTemp, soc);
                            } else {
                                MiuiBatteryStatsService.this.mBatteryTempSocTime.processTempSocData();
                            }
                        } else {
                            MiuiBatteryStatsService.this.mBatteryTempSocTime.collectTempSocData(batteryTemp, soc);
                        }
                    }
                    if (MiuiBatteryStatsService.this.mSupportedCellVolt) {
                        MiuiBatteryStatsService.this.mHandler.handleSreiesDeltaVoltage(batteryTemp, soc);
                    }
                }
                if (plugged && !MiuiBatteryStatsService.this.mLastPlugged && !MiuiBatteryStatsService.this.mStartRecordDischarging) {
                    if (MiuiBatteryStatsService.this.DEBUG) {
                        Slog.d("MiuiBatteryStatsService", "5s start mDischargingCount = " + MiuiBatteryStatsService.this.mDischargingCount);
                    }
                    MiuiBatteryStatsService.this.mHandler.sendMessageDelayed(13, 5000L);
                    MiuiBatteryStatsService.this.mStartRecordDischarging = true;
                    MiuiBatteryStatsService.this.mIsHandleIntermittentCharge = true;
                }
                if (!plugged && MiuiBatteryStatsService.this.mLastPlugged && !MiuiBatteryStatsService.this.mIsHandleIntermittentCharge) {
                    MiuiBatteryStatsService.this.mStartRecordDischarging = false;
                }
                if (MiuiBatteryStatsService.this.mStartRecordDischarging && batteryStatus == 3) {
                    MiuiBatteryStatsService.this.mDischargingCount++;
                    if (MiuiBatteryStatsService.this.DEBUG) {
                        Slog.d("MiuiBatteryStatsService", "record mDischargingCount = " + MiuiBatteryStatsService.this.mDischargingCount);
                    }
                }
                if (soc != MiuiBatteryStatsService.this.mLastSoc) {
                    MiuiBatteryStatsService.this.mHandler.handleNonlinearChangeOfCapacity(soc);
                }
                MiuiBatteryStatsService.this.mLastBatteryStatus = batteryStatus;
                MiuiBatteryStatsService.this.mLastPlugged = plugged;
                MiuiBatteryStatsService.this.mLastBatteryVoltage = batteryVoltage;
                MiuiBatteryStatsService.this.mLastBatteryTemp = batteryTemp;
                MiuiBatteryStatsService.this.mLastSoc = soc;
            }
        };
        IntentFilter filter = new IntentFilter(MiuiDfcService.ACTION_TEST_USB_STATE);
        filter.addAction("android.intent.action.BATTERY_CHANGED");
        filter.addAction(UPDATE_BATTERY_DATA);
        filter.addAction("miui.intent.action.ACTION_SHUTDOWN_DELAY");
        filter.addAction("android.intent.action.ACTION_SHUTDOWN");
        filter.addAction("android.intent.action.BOOT_COMPLETED");
        filter.addAction("android.intent.action.SCREEN_OFF");
        filter.addAction("android.intent.action.SCREEN_ON");
        filter.addAction(LIMIT_TIME);
        context.registerReceiver(stateChangedReceiver, filter);
        Intent intent = new Intent(UPDATE_BATTERY_DATA);
        intent.setFlags(1073741824);
        this.mPendingIntent = PendingIntent.getBroadcast(context, 0, intent, BroadcastQueueImpl.FLAG_IMMUTABLE);
        Intent limitTime = new Intent(LIMIT_TIME);
        limitTime.addFlags(1073741824);
        this.mPendingIntentLimitTime = PendingIntent.getBroadcast(context, 0, limitTime, BroadcastQueueImpl.FLAG_IMMUTABLE);
        this.mAlarmManager = (AlarmManager) context.getSystemService("alarm");
        if (z) {
            Slog.d("MiuiBatteryStatsService", "DAY = " + DAY);
        }
        this.mAlarmManager.setExactAndAllowWhileIdle(2, SystemClock.elapsedRealtime(), this.mPendingIntent);
    }

    /* loaded from: classes.dex */
    public class BatteryStatsHandler extends Handler {
        public static final int MSG_INTERMITTENT_CHARGE = 13;
        public static final int MSG_RESTORE_LAST_STATE = 10;
        public static final int MSG_SAVE_BATT_INFO = 9;
        public static final int MSG_SEND_BATTERY_EXCEPTION_TIME = 8;
        public static final int MSG_SEND_BATTERY_TEMP = 7;
        public static final int MSG_SLOW_CHARGE = 12;
        public static final int MSG_UPDATE_BATTERY_HEALTH = 0;
        public static final int MSG_UPDATE_BATTERY_TEMP = 6;
        public static final int MSG_UPDATE_CHARGE = 1;
        public static final int MSG_UPDATE_CHARGE_ACTION = 4;
        public static final int MSG_UPDATE_POWER_OFF_DELAY = 5;
        public static final int MSG_UPDATE_USB_FUNCTION = 3;
        public static final int MSG_UPDATE_VBUS_DISALE = 11;
        public static final int MSG_UPDATE_WIRELESS_REVERSE_CHARGE = 2;
        private static final String POEER_50 = "50";
        private static final String POWER_0 = "0";
        private static final String POWER_10 = "10";
        private static final String POWER_15 = "15";
        private static final String POWER_18 = "18";
        private static final String POWER_20 = "20";
        private static final String POWER_22_5 = "22.5";
        private static final String POWER_27 = "27";
        private static final String POWER_2_5 = "2.5";
        private static final String POWER_30 = "30";
        private static final String POWER_5 = "5";
        private static final String POWER_7_5 = "7.5";
        private static final String REVERSE_CHG_MODE_EVENT = "POWER_SUPPLY_REVERSE_CHG_MODE";
        private static final String VBUS_DISABLE_EVENT = "POWER_SUPPLY_VBUS_DISABLE";
        private int mLastOpenStatus;
        private int mLastVbusDisable;
        private final UEventObserver mUEventObserver;
        private String mChargeType = null;
        private String mTxAdapter = null;
        private int mDeltaV = 0;
        private boolean mIsDeltaVAssigned = false;
        private HashMap<String, String> mChargePowerHashMap = new HashMap<>();
        private ArrayMap<String, BatteryTempInfo> mBatteryTemps = new ArrayMap<>();
        private IMiCharge mMiCharge = IMiCharge.getInstance();
        private String[] TX_ADAPT_POWER = {POWER_2_5, POWER_5, POWER_5, POWER_0, POWER_5, POWER_10, POWER_10, POWER_10, POWER_20, POWER_20, POWER_20, POWER_30, POWER_30, POEER_50};
        private String[] BATTER_HEALTH_PARAMS = {TrackBatteryUsbInfo.CYCLE_COUNT, TrackBatteryUsbInfo.SOH, TrackBatteryUsbInfo.CHARGE_FULL, TrackBatteryUsbInfo.BATT_AUTH};
        private String[] CHARGE_PARAMS = {TrackBatteryUsbInfo.CHARGE_TYPE, TrackBatteryUsbInfo.USB_VOLTAGE, TrackBatteryUsbInfo.USB_CURRENT, TrackBatteryUsbInfo.CHARGE_POWER, TrackBatteryUsbInfo.TX_ADAPTER, TrackBatteryUsbInfo.PD_AUTHENTICATION, TrackBatteryUsbInfo.PD_APDO_MAX, TrackBatteryUsbInfo.VBAT, TrackBatteryUsbInfo.CAPCAITY, TrackBatteryUsbInfo.IBAT, TrackBatteryUsbInfo.TBAT, TrackBatteryUsbInfo.BATT_RESISTANCE, TrackBatteryUsbInfo.BATT_THERMAL_LEVEL};
        private String[] WIRELESS_REVERSE_CHARGE_PARAMS = {TrackBatteryUsbInfo.WIRELESS_REVERSE_CHARGE};
        private String[] TEMP_COLLECTED_SCENE = {"wiredOpenFFC", "wirelessOpenFFC", "wiredNotOpenFFC", "wirelessNotOpenFFC", "releaseCharge", "reverseWirelessCharge"};
        private String[] SLOW_CHARGE_TYPE = {"wiredRecognizedUsbFloat", "wiredNormalSlowCharge", "wiredSlowCharge"};

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public BatteryStatsHandler(Looper looper) {
            super(looper);
            MiuiBatteryStatsService.this = this$0;
            initWiredChargePower();
            initBatteryTemp();
            BatteryUEventObserver batteryUEventObserver = new BatteryUEventObserver();
            this.mUEventObserver = batteryUEventObserver;
            batteryUEventObserver.startObserving(REVERSE_CHG_MODE_EVENT);
            batteryUEventObserver.startObserving(VBUS_DISABLE_EVENT);
            this$0.mSupportedSB = this.mMiCharge.isFunctionSupported("smart_batt");
            this$0.mSupportedCellVolt = this.mMiCharge.isFunctionSupported("cell1_volt");
        }

        private void initWiredChargePower() {
            this.mChargePowerHashMap.put("USB", POWER_2_5);
            this.mChargePowerHashMap.put("OCP", POWER_2_5);
            this.mChargePowerHashMap.put("USB_DCP", POWER_7_5);
            this.mChargePowerHashMap.put("USB_CDP", POWER_7_5);
            this.mChargePowerHashMap.put("USB_FLOAT", POWER_5);
            this.mChargePowerHashMap.put("USB_HVDCP", POWER_15);
            this.mChargePowerHashMap.put("USB_HVDCP_3P5", POWER_22_5);
        }

        private void initBatteryTemp() {
            int i = 0;
            while (true) {
                String[] strArr = this.TEMP_COLLECTED_SCENE;
                if (i < strArr.length) {
                    BatteryTempInfo batteryTempInfo = new BatteryTempInfo(strArr[i]);
                    this.mBatteryTemps.put(this.TEMP_COLLECTED_SCENE[i], batteryTempInfo);
                    i++;
                } else {
                    return;
                }
            }
        }

        private int getChargingPowerMax() {
            String powerMax = this.mMiCharge.getChargingPowerMax();
            if (powerMax != null && powerMax.length() != 0) {
                return parseInt(powerMax);
            }
            return -1;
        }

        private String getBatteryChargeType() {
            return this.mMiCharge.getBatteryChargeType();
        }

        public void updateChargeInfo(boolean plugged) {
            if (plugged) {
                sendMessageDelayed(1, MiuiBatteryStatsService.FIVEMIN);
                return;
            }
            removeMessages(1);
            sendMessageDelayed(4, 0L);
        }

        public void limitTimeForSlowCharge() {
            if ("USB_FLOAT".equals(getBatteryChargeType())) {
                handleSlowCharge();
            } else if (getChargingPowerMax() < 96) {
                MiuiBatteryStatsService.this.mAlarmManager.setExactAndAllowWhileIdle(2, SystemClock.elapsedRealtime() + MiuiBatteryStatsService.TENMIN, MiuiBatteryStatsService.this.mPendingIntentLimitTime);
            } else {
                MiuiBatteryStatsService.this.mAlarmManager.setExactAndAllowWhileIdle(2, SystemClock.elapsedRealtime() + MiuiBatteryStatsService.FIVEMIN, MiuiBatteryStatsService.this.mPendingIntentLimitTime);
            }
        }

        public String formatTime(long timestamp) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            if (timestamp > 0) {
                return sdf.format(new Date(timestamp));
            }
            return "None";
        }

        private String getTimeHours(long totalMilliSeconds) {
            if (totalMilliSeconds > 0) {
                long totalSeconds = totalMilliSeconds / 1000;
                long currentSecond = totalSeconds % 60;
                long totalMinutes = totalSeconds / 60;
                long currentMinute = totalMinutes % 60;
                long totalHour = totalMinutes / 60;
                long currentHour = totalHour % 24;
                return currentHour + "h" + currentMinute + "min" + currentSecond + "s";
            }
            return "None";
        }

        public void calculateTemp(BatteryTempInfo t, int temp) {
            if (t.getMaxTemp() < temp) {
                t.setMaxTemp(temp);
            }
            if (t.getMinTemp() > temp) {
                t.setMinTemp(temp);
            }
            t.setTotalTemp(temp);
            t.setDataCount();
        }

        public void sendMessage(int what, int arg1) {
            removeMessages(what);
            Message m = Message.obtain(this, what);
            m.arg1 = arg1;
            sendMessage(m);
        }

        public void sendMessageDelayed(int what, Object arg, long delayMillis) {
            removeMessages(what);
            Message m = Message.obtain(this, what);
            m.obj = arg;
            sendMessageDelayed(m, delayMillis);
        }

        public void sendMessageDelayed(int what, boolean arg, long delayMillis) {
            removeMessages(what);
            Message m = Message.obtain(this, what);
            m.arg1 = arg ? 1 : 0;
            sendMessageDelayed(m, delayMillis);
        }

        public void sendMessageDelayed(int what, long delayMillis) {
            removeMessages(what);
            Message m = Message.obtain(this, what);
            sendMessageDelayed(m, delayMillis);
        }

        public int parseInt(String argument) {
            try {
                if (TextUtils.isEmpty(argument)) {
                    Slog.e("MiuiBatteryStatsService", "argument = " + argument);
                    return -1;
                }
                return Integer.parseInt(argument);
            } catch (NumberFormatException e) {
                Slog.e("MiuiBatteryStatsService", "Invalid integer argument " + argument);
                return -1;
            }
        }

        private void sendOneTrackInfo(Bundle params, String id, String event) {
            if (Build.IS_INTERNATIONAL_BUILD) {
                return;
            }
            Intent intent = new Intent(TrackBatteryUsbInfo.ACTION_TRACK_EVENT);
            intent.setPackage(TrackBatteryUsbInfo.ANALYTICS_PACKAGE);
            intent.putExtra(TrackBatteryUsbInfo.PARAM_APP_ID, id);
            intent.putExtra(TrackBatteryUsbInfo.PARAM_EVENT_NAME, event);
            intent.putExtra(TrackBatteryUsbInfo.PARAM_PACKAGE, "Android");
            intent.putExtras(params);
            if (!Build.IS_INTERNATIONAL_BUILD) {
                intent.setFlags(2);
            }
            try {
                MiuiBatteryStatsService.this.mContext.startServiceAsUser(intent, UserHandle.CURRENT);
            } catch (IllegalStateException e) {
                Slog.e("MiuiBatteryStatsService", "Start one-Track service failed", e);
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes.dex */
        public final class BatteryUEventObserver extends UEventObserver {
            private BatteryUEventObserver() {
                BatteryStatsHandler.this = r1;
            }

            public void onUEvent(UEventObserver.UEvent event) {
                int vbusDisable;
                int openStatus;
                if (event.get(BatteryStatsHandler.REVERSE_CHG_MODE_EVENT) != null && (openStatus = BatteryStatsHandler.this.parseInt(event.get(BatteryStatsHandler.REVERSE_CHG_MODE_EVENT))) != BatteryStatsHandler.this.mLastOpenStatus) {
                    BatteryStatsHandler.this.mLastOpenStatus = openStatus;
                    BatteryStatsHandler.this.sendMessageDelayed(2, 0L);
                }
                if (event.get(BatteryStatsHandler.VBUS_DISABLE_EVENT) != null && (vbusDisable = BatteryStatsHandler.this.parseInt(event.get(BatteryStatsHandler.VBUS_DISABLE_EVENT))) != BatteryStatsHandler.this.mLastVbusDisable) {
                    BatteryStatsHandler.this.mLastVbusDisable = vbusDisable;
                    BatteryStatsHandler.this.sendMessage(11, vbusDisable);
                }
            }
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    handleBatteryHealth();
                    return;
                case 1:
                    handleCharge();
                    return;
                case 2:
                    handleWirelessReverseCharge();
                    return;
                case 3:
                    Intent i = (Intent) msg.obj;
                    handleUsbFunction(i);
                    return;
                case 4:
                    handleChargeAction();
                    return;
                case 5:
                    boolean z = true;
                    if (msg.arg1 != 1) {
                        z = false;
                    }
                    boolean value = z;
                    handlePowerOff(value);
                    return;
                case 6:
                    Intent j = (Intent) msg.obj;
                    handleBatteryTemp(j);
                    return;
                case 7:
                    sendBatteryTempData();
                    return;
                case 8:
                    sendBatteryTempVoltageTimeData(MiuiBatteryStatsService.this.mBatteryInfoNormal);
                    sendBatteryTempVoltageTimeData(MiuiBatteryStatsService.this.mBatteryInfoFull);
                    return;
                case 9:
                    MiuiBatteryStatsService.this.mBatteryTempSocTime.writeDataToFile();
                    return;
                case 10:
                    MiuiBatteryStatsService.this.mBatteryTempSocTime.readDataFromFile();
                    MiuiBatteryStatsService.this.mBatteryTempSocTime.sendAdjustVolBroadcast();
                    return;
                case 11:
                    handleVbusDisable(msg.arg1);
                    return;
                case 12:
                    limitTimeForSlowCharge();
                    return;
                case 13:
                    handleIntermittentCharge();
                    return;
                default:
                    Slog.d("MiuiBatteryStatsService", "no message to handle");
                    return;
            }
        }

        private void handleBatteryHealth() {
            Bundle params = new Bundle();
            int i = 0;
            while (true) {
                String[] strArr = this.BATTER_HEALTH_PARAMS;
                if (i < strArr.length) {
                    try {
                        String str = strArr[i];
                        char c = 65535;
                        switch (str.hashCode()) {
                            case -1141549079:
                                if (str.equals(TrackBatteryUsbInfo.CYCLE_COUNT)) {
                                    c = 0;
                                    break;
                                }
                                break;
                            case 114060:
                                if (str.equals(TrackBatteryUsbInfo.SOH)) {
                                    c = 1;
                                    break;
                                }
                                break;
                            case 1321447147:
                                if (str.equals(TrackBatteryUsbInfo.BATT_AUTH)) {
                                    c = 3;
                                    break;
                                }
                                break;
                            case 2135596528:
                                if (str.equals(TrackBatteryUsbInfo.CHARGE_FULL)) {
                                    c = 2;
                                    break;
                                }
                                break;
                        }
                        switch (c) {
                            case 0:
                                params.putString(TrackBatteryUsbInfo.CYCLE_COUNT, this.mMiCharge.getBatteryCycleCount());
                                continue;
                            case 1:
                                String value = this.mMiCharge.getBatterySoh();
                                if (value == null || value.length() == 0) {
                                    value = "-1";
                                }
                                params.putString(TrackBatteryUsbInfo.SOH, value);
                                continue;
                            case 2:
                                params.putString(TrackBatteryUsbInfo.CHARGE_FULL, this.mMiCharge.getBatteryChargeFull());
                                continue;
                            case 3:
                                params.putString(TrackBatteryUsbInfo.BATT_AUTH, this.mMiCharge.getBatteryAuthentic());
                                continue;
                            default:
                                Slog.d("MiuiBatteryStatsService", "nothing to handle battery health");
                                continue;
                        }
                    } catch (Exception e) {
                        Slog.e("MiuiBatteryStatsService", "read file about battery health error " + e);
                    }
                    i++;
                } else {
                    if (MiuiBatteryStatsService.this.DEBUG) {
                        Slog.d("MiuiBatteryStatsService", "BATTER_HEALTH_PARAMS params = " + params);
                    }
                    sendOneTrackInfo(params, TrackBatteryUsbInfo.BATTERY_APP_ID, TrackBatteryUsbInfo.BATTERY_HEALTH_EVENT);
                    return;
                }
            }
        }

        /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
            jadx.core.utils.exceptions.JadxRuntimeException: Unreachable block: B:129:0x02ae
            	at jadx.core.dex.visitors.blocks.BlockProcessor.checkForUnreachableBlocks(BlockProcessor.java:92)
            	at jadx.core.dex.visitors.blocks.BlockProcessor.processBlocksTree(BlockProcessor.java:52)
            	at jadx.core.dex.visitors.blocks.BlockProcessor.visit(BlockProcessor.java:44)
            */
        private void handleCharge() {
            /*
                Method dump skipped, instructions count: 982
                To view this dump add '--comments-level debug' option
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.server.MiuiBatteryStatsService.BatteryStatsHandler.handleCharge():void");
        }

        private void handleWirelessReverseCharge() {
            Bundle params = new Bundle();
            int i = 0;
            while (true) {
                String[] strArr = this.WIRELESS_REVERSE_CHARGE_PARAMS;
                if (i < strArr.length) {
                    try {
                        String str = strArr[i];
                        char c = 65535;
                        switch (str.hashCode()) {
                            case -1888476095:
                                if (str.equals(TrackBatteryUsbInfo.WIRELESS_REVERSE_CHARGE)) {
                                    c = 0;
                                    break;
                                }
                                break;
                        }
                        switch (c) {
                            case 0:
                                String value = this.mMiCharge.getWirelessReverseStatus();
                                params.putString(TrackBatteryUsbInfo.WIRELESS_REVERSE_CHARGE, value);
                                continue;
                            default:
                                Slog.d("MiuiBatteryStatsService", "no wireless reverse charge params to handle");
                                continue;
                        }
                    } catch (Exception e) {
                        Slog.e("MiuiBatteryStatsService", "read wireless reverse charge file error " + e);
                    }
                    i++;
                } else {
                    if (MiuiBatteryStatsService.this.DEBUG) {
                        Slog.d("MiuiBatteryStatsService", "Wireless Reverse Charge params =  " + params);
                    }
                    sendOneTrackInfo(params, TrackBatteryUsbInfo.BATTERY_APP_ID, TrackBatteryUsbInfo.WIRELESS_REVERSE_CHARGE_EVENT);
                    return;
                }
            }
        }

        private void handleChargeAction() {
            Bundle params = new Bundle();
            if (this.mChargeType != null) {
                params.putString(TrackBatteryUsbInfo.CHARGE_START_TIME, formatTime(MiuiBatteryStatsService.this.mChargeStartTime));
                params.putString(TrackBatteryUsbInfo.CHARGE_END_TIME, formatTime(MiuiBatteryStatsService.this.mChargeEndTime));
                params.putString(TrackBatteryUsbInfo.FULL_CHARGE_START_TIME, formatTime(MiuiBatteryStatsService.this.mFullChargeStartTime));
                params.putString(TrackBatteryUsbInfo.FULL_CHARGE_END_TIME, formatTime(MiuiBatteryStatsService.this.mFullChargeEndTime));
                params.putInt(TrackBatteryUsbInfo.CHARGE_START_CAPACITY, MiuiBatteryStatsService.this.mChargeStartCapacity);
                params.putInt(TrackBatteryUsbInfo.CHARGE_END_CAPACITY, MiuiBatteryStatsService.this.mChargeEndCapacity);
                params.putString(TrackBatteryUsbInfo.CHARGE_TOTAL_TIME, getTimeHours(MiuiBatteryStatsService.this.mChargeEndTime - MiuiBatteryStatsService.this.mChargeStartTime));
                params.putString(TrackBatteryUsbInfo.FULL_CHARGE_TOTAL_TIME, getTimeHours(MiuiBatteryStatsService.this.mFullChargeEndTime - MiuiBatteryStatsService.this.mFullChargeStartTime));
                params.putString(TrackBatteryUsbInfo.CHARGE_TYPE, this.mChargeType);
                params.putString(TrackBatteryUsbInfo.TX_ADAPTER, this.mTxAdapter);
                params.putStringArrayList(TrackBatteryUsbInfo.APP_TIME, MiuiBatteryStatsService.this.mAppUsageStats.getTop3Apps(MiuiBatteryStatsService.this.mContext, MiuiBatteryStatsService.this.mChargeStartTime, MiuiBatteryStatsService.this.mChargeEndTime));
                params.putInt(TrackBatteryUsbInfo.CHARGE_BAT_MAX_TEMP, MiuiBatteryStatsService.this.mChargeMaxTemp);
                params.putInt(TrackBatteryUsbInfo.CHARGE_BAT_MIN_TEMP, MiuiBatteryStatsService.this.mChargeMinTemp);
                params.putString(TrackBatteryUsbInfo.SCREEN_ON_TIME, getTimeHours(MiuiBatteryStatsService.this.mScreenOnTime));
                params.putString(TrackBatteryUsbInfo.SCREEN_OFF_TIME, getTimeHours((MiuiBatteryStatsService.this.mChargeEndTime - MiuiBatteryStatsService.this.mChargeStartTime) - MiuiBatteryStatsService.this.mScreenOnTime));
                if (MiuiBatteryStatsService.this.DEBUG) {
                    Slog.d("MiuiBatteryStatsService", "Charge Action params =  " + params);
                }
                sendOneTrackInfo(params, TrackBatteryUsbInfo.BATTERY_APP_ID, TrackBatteryUsbInfo.BATTERY_CHARGE_ACTION_EVENT);
                this.mChargeType = null;
                MiuiBatteryStatsService.this.mFullChargeStartTime = 0L;
                MiuiBatteryStatsService.this.mFullChargeEndTime = 0L;
                MiuiBatteryStatsService.this.mChargeMaxTemp = 0;
                MiuiBatteryStatsService.this.mChargeMinTemp = 0;
                MiuiBatteryStatsService.this.mScreenOnTime = 0L;
            }
        }

        private void handlePowerOff(boolean shutdown_start) {
            Bundle params = new Bundle();
            if (!shutdown_start) {
                params.putInt(TrackBatteryUsbInfo.SHUTDOWN_DELAY, 1);
                params.putString(TrackBatteryUsbInfo.VBAT, this.mMiCharge.getBatteryVbat());
                params.putString(TrackBatteryUsbInfo.TBAT, this.mMiCharge.getBatteryTbat());
                params.putString(TrackBatteryUsbInfo.IBAT, this.mMiCharge.getBatteryIbat());
                MiuiBatteryStatsService.this.mHandler.sendMessageDelayed(5, true, 35000L);
            } else {
                params.putInt(TrackBatteryUsbInfo.SHUTDOWN_DELAY, 2);
            }
            if (MiuiBatteryStatsService.this.DEBUG) {
                Slog.d("MiuiBatteryStatsService", "Power Off Action params =  " + params);
            }
            sendOneTrackInfo(params, TrackBatteryUsbInfo.BATTERY_APP_ID, TrackBatteryUsbInfo.BATTERY_POWER_OFF_EVENT);
        }

        private void handleVbusDisable(int vbusDisable) {
            Bundle params = new Bundle();
            params.putInt("vbus_disable", vbusDisable);
            sendOneTrackInfo(params, TrackBatteryUsbInfo.BATTERY_APP_ID, "vbus_disable");
            if (MiuiBatteryStatsService.this.DEBUG) {
                Slog.d("MiuiBatteryStatsService", "VBUS params = " + params);
            }
        }

        public void handleSreiesDeltaVoltage(int temp, int soc) {
            Bundle params = new Bundle();
            if (temp < 200 || temp > 350 || soc < 35 || soc > 80) {
                if (this.mIsDeltaVAssigned) {
                    params.putInt("series_delta_voltage", this.mDeltaV);
                    if (MiuiBatteryStatsService.this.DEBUG) {
                        Slog.d("MiuiBatteryStatsService", "series delta voltage params =  " + params);
                    }
                    sendOneTrackInfo(params, TrackBatteryUsbInfo.BATTERY_APP_ID, "series_delta_voltage");
                    this.mDeltaV = 0;
                    this.mIsDeltaVAssigned = false;
                    return;
                }
                return;
            }
            int ibat = parseInt(this.mMiCharge.getBatteryIbat());
            int vCell1 = parseInt(this.mMiCharge.getMiChargePath("cell1_volt"));
            int vCell2 = parseInt(this.mMiCharge.getMiChargePath("cell2_volt"));
            if (ibat <= 100000 && vCell1 >= 3850 && vCell1 <= 4100 && vCell2 >= 3850 && vCell2 <= 4100) {
                int currentV = Math.abs(vCell1 - vCell2);
                if (currentV > this.mDeltaV) {
                    this.mDeltaV = currentV;
                }
                if (MiuiBatteryStatsService.this.DEBUG) {
                    Slog.d("MiuiBatteryStatsService", "cell1_volt =  " + vCell1 + ", cell2_volt = " + vCell2 + ", mDeltaV = " + this.mDeltaV);
                }
                this.mIsDeltaVAssigned = true;
            }
        }

        private void handleUsbFunction(Intent intent) {
            Bundle params = new Bundle();
            params.putInt(TrackBatteryUsbInfo.DATA_UNLOCK, intent.getBooleanExtra("unlocked", false) ? 1 : 0);
            params.putInt(TrackBatteryUsbInfo.USB32, this.mMiCharge.isUSB32() ? 1 : 0);
            String usb_function = SystemProperties.get("sys.usb.config", "none");
            params.putString(TrackBatteryUsbInfo.USB_FUNCTION, usb_function);
            if (MiuiBatteryStatsService.this.DEBUG) {
                Slog.d("MiuiBatteryStatsService", "Usb function params =  " + params);
            }
            sendOneTrackInfo(params, TrackBatteryUsbInfo.USB_APP_ID, TrackBatteryUsbInfo.USB_DEVICE_EVENT);
        }

        private void handleBatteryTemp(Intent intent) {
            int currentPlugType = intent.getIntExtra("plugged", -1);
            int currentChargeStatus = intent.getIntExtra(DarkModeOneTrackHelper.EVENT_NAME_STATUS, 1);
            int currentTemp = intent.getIntExtra(TemperatureController.STRATEGY_TEMPERATURE, 0);
            int wirelessReverseValue = 0;
            int fastChargeModeValue = 0;
            String wirelessReverseStatus = this.mMiCharge.getWirelessReverseStatus();
            if (wirelessReverseStatus != null && wirelessReverseStatus.length() != 0) {
                wirelessReverseValue = parseInt(wirelessReverseStatus);
            }
            String fastChargeStatus = this.mMiCharge.getFastChargeModeStatus();
            if (fastChargeStatus != null && fastChargeStatus.length() != 0) {
                fastChargeModeValue = parseInt(fastChargeStatus);
            }
            if (MiuiBatteryStatsService.this.DEBUG) {
                Slog.d("MiuiBatteryStatsService", "currentPlugType = " + currentPlugType + " currentChargeStatus = " + currentChargeStatus + " currentTemp = " + currentTemp + " wirelessReverseStatus = " + wirelessReverseValue + " fastChargeStatus = " + fastChargeModeValue);
            }
            if (currentPlugType > 0) {
                if (MiuiBatteryStatsService.this.mChargeMaxTemp < currentTemp) {
                    MiuiBatteryStatsService.this.mChargeMaxTemp = currentTemp;
                }
                if (MiuiBatteryStatsService.this.mChargeMinTemp > currentTemp) {
                    MiuiBatteryStatsService.this.mChargeMinTemp = currentTemp;
                }
            }
            if (currentChargeStatus == 2 && ((currentPlugType == 1 || currentPlugType == 2) && fastChargeModeValue == 1)) {
                calculateTemp(this.mBatteryTemps.get(this.TEMP_COLLECTED_SCENE[0]), currentTemp);
            }
            if (currentChargeStatus == 2 && currentPlugType == 4 && fastChargeModeValue == 1) {
                calculateTemp(this.mBatteryTemps.get(this.TEMP_COLLECTED_SCENE[1]), currentTemp);
            }
            if (currentChargeStatus == 2 && ((currentPlugType == 1 || currentPlugType == 2) && fastChargeModeValue == 0)) {
                calculateTemp(this.mBatteryTemps.get(this.TEMP_COLLECTED_SCENE[2]), currentTemp);
            }
            if (currentChargeStatus == 2 && currentPlugType == 4 && fastChargeModeValue == 0) {
                calculateTemp(this.mBatteryTemps.get(this.TEMP_COLLECTED_SCENE[3]), currentTemp);
            }
            if (wirelessReverseValue == 0 && currentChargeStatus == 3) {
                calculateTemp(this.mBatteryTemps.get(this.TEMP_COLLECTED_SCENE[4]), currentTemp);
            }
            if (wirelessReverseValue == 1) {
                calculateTemp(this.mBatteryTemps.get(this.TEMP_COLLECTED_SCENE[5]), currentTemp);
            }
        }

        private void sendBatteryTempData() {
            int i = 0;
            while (true) {
                String[] strArr = this.TEMP_COLLECTED_SCENE;
                if (i < strArr.length) {
                    BatteryTempInfo batteryTempInfo = this.mBatteryTemps.get(strArr[i]);
                    if (!this.TEMP_COLLECTED_SCENE[i].equals(batteryTempInfo.getCondition())) {
                        return;
                    }
                    if (batteryTempInfo.getDataCount() != 0) {
                        batteryTempInfo.setAverageTemp();
                        Bundle params = new Bundle();
                        params.putString(TrackBatteryUsbInfo.SCENE, batteryTempInfo.getCondition());
                        params.putInt(TrackBatteryUsbInfo.MAXTEMP, batteryTempInfo.getMaxTemp());
                        params.putInt(TrackBatteryUsbInfo.MINTEMP, batteryTempInfo.getMinTemp());
                        params.putInt(TrackBatteryUsbInfo.AVETEMP, batteryTempInfo.getAverageTemp());
                        sendOneTrackInfo(params, TrackBatteryUsbInfo.BATTERY_APP_ID, TrackBatteryUsbInfo.BATTERY_TEMP_EVENT);
                        batteryTempInfo.reset();
                        if (MiuiBatteryStatsService.this.DEBUG) {
                            Slog.d("MiuiBatteryStatsService", this.TEMP_COLLECTED_SCENE[i] + " params =  " + params);
                        }
                    }
                    i++;
                } else {
                    return;
                }
            }
        }

        private void sendBatteryTempVoltageTimeData(BatteryTempVoltageTimeInfo info) {
            info.stopTime();
            Bundle params = new Bundle();
            params.putString(TrackBatteryUsbInfo.HIGH_TEMP_TIME, getTimeHours(info.getHighTempTotalTime()));
            params.putString(TrackBatteryUsbInfo.HIGH_VOLTAGE_TIME, getTimeHours(info.getHighVoltageTotalTime()));
            params.putString(TrackBatteryUsbInfo.HIGH_TEMP_VOLTAGE_TIME, getTimeHours(info.getHighTempVoltageTotalTime()));
            params.putString(TrackBatteryUsbInfo.FULL_CHARGE_TOTAL_TIME, getTimeHours(info.getFullChargeTotalTime()));
            params.putString(TrackBatteryUsbInfo.CHARGE_STATUS, info.getCondition());
            sendOneTrackInfo(params, TrackBatteryUsbInfo.BATTERY_APP_ID, TrackBatteryUsbInfo.BATTERY_HIGH_TEMP_VOLTAGE);
            if (MiuiBatteryStatsService.this.DEBUG) {
                Slog.d("MiuiBatteryStatsService", info.getCondition() + " params =  " + params);
            }
            info.clearTime();
        }

        public void handleSlowCharge() {
            Bundle params = new Bundle();
            int battTempValue = 0;
            int thermalLevelValue = 0;
            int quickChargeTypeValue = 0;
            int chargeCurrentSocValue = 0;
            String chargeCurrentSoc = this.mMiCharge.getBatteryCapacity();
            if (!TextUtils.isEmpty(chargeCurrentSoc)) {
                chargeCurrentSocValue = parseInt(chargeCurrentSoc);
            }
            if (MiuiBatteryStatsService.this.DEBUG) {
                Slog.d("MiuiBatteryStatsService", "chargeCurrentSoc = " + chargeCurrentSocValue + ", chargeStartSoc = " + MiuiBatteryStatsService.this.mChargeStartCapacity);
            }
            if ("USB_FLOAT".equals(getBatteryChargeType())) {
                params.putString(TrackBatteryUsbInfo.SLOW_CHARGE_TYPE, this.SLOW_CHARGE_TYPE[0]);
            } else if (chargeCurrentSocValue - MiuiBatteryStatsService.this.mChargeStartCapacity < 3) {
                String pdVerified = this.mMiCharge.getPdAuthentication();
                String quickChargeType = this.mMiCharge.getQuickChargeType();
                if (!TextUtils.isEmpty(quickChargeType)) {
                    quickChargeTypeValue = parseInt(quickChargeType);
                }
                String thermalLevel = this.mMiCharge.getBatteryThermaLevel();
                if (!TextUtils.isEmpty(thermalLevel)) {
                    thermalLevelValue = parseInt(thermalLevel);
                }
                String battTemp = this.mMiCharge.getBatteryTbat();
                if (!TextUtils.isEmpty(battTemp)) {
                    battTempValue = parseInt(battTemp);
                }
                if (MiuiBatteryStatsService.this.DEBUG) {
                    Slog.d("MiuiBatteryStatsService", "pdVerified = " + pdVerified + ", quickChargeTypeValue = " + quickChargeTypeValue + ", thermalLevelValue = " + thermalLevelValue + ", battTempValue = " + battTempValue);
                }
                if (SplitScreenReporter.ACTION_ENTER_SPLIT.equals(pdVerified) && (battTempValue > 470 || thermalLevelValue > 9)) {
                    params.putString(TrackBatteryUsbInfo.SLOW_CHARGE_TYPE, this.SLOW_CHARGE_TYPE[1]);
                } else if (quickChargeTypeValue >= 3 && chargeCurrentSocValue < 80 && thermalLevelValue < 8 && battTempValue < 470) {
                    params.putString(TrackBatteryUsbInfo.SLOW_CHARGE_TYPE, this.SLOW_CHARGE_TYPE[2]);
                }
            }
            if (params.size() != 0) {
                if (MiuiBatteryStatsService.this.DEBUG) {
                    Slog.d("MiuiBatteryStatsService", "Slow Charge params = " + params);
                }
                sendOneTrackInfo(params, TrackBatteryUsbInfo.BATTERY_APP_ID, TrackBatteryUsbInfo.SLOW_CHARGE_EVENT);
            }
        }

        private void handleIntermittentCharge() {
            Bundle params = new Bundle();
            Slog.d("MiuiBatteryStatsService", "mDischargingCount = " + MiuiBatteryStatsService.this.mDischargingCount);
            if (MiuiBatteryStatsService.this.mDischargingCount >= 3) {
                params.putString("intermittent_charge", "intermittentCharge");
                if (MiuiBatteryStatsService.this.DEBUG) {
                    Slog.d("MiuiBatteryStatsService", "Intermittent Charge params = " + params);
                }
                sendOneTrackInfo(params, TrackBatteryUsbInfo.BATTERY_APP_ID, "intermittent_charge");
            }
            MiuiBatteryStatsService.this.mDischargingCount = 0;
            MiuiBatteryStatsService.this.mIsHandleIntermittentCharge = false;
        }

        public void handleNonlinearChangeOfCapacity(int currentSoc) {
            Bundle params = new Bundle();
            int capacityChangeValue = currentSoc - MiuiBatteryStatsService.this.mLastSoc;
            if (MiuiBatteryStatsService.this.DEBUG) {
                Slog.d("MiuiBatteryStatsService", "currentSoc = " + currentSoc + " mLastSoc = " + MiuiBatteryStatsService.this.mLastSoc + " capacityChangeValue = " + capacityChangeValue);
            }
            if (Math.abs(capacityChangeValue) > 1) {
                params.putInt(TrackBatteryUsbInfo.CAPACITY_CHANGE_VALUE, capacityChangeValue);
                if (MiuiBatteryStatsService.this.DEBUG) {
                    Slog.d("MiuiBatteryStatsService", "Nonlinear Change Of Capacity params = " + params);
                }
                sendOneTrackInfo(params, TrackBatteryUsbInfo.BATTERY_APP_ID, TrackBatteryUsbInfo.CAPACITY_NONLINEAR_CHANGE_EVENT);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class BatteryTempSocTimeInfo {
        private long totalTimeIn38C;
        private long totalTimeIn43C;
        private long totalTimeInHighSoc;
        private final String BATTERT_INFO_FILE = "/data/system/battery-info.txt";
        private final String IS_SATISFY_TEMP_SOC_CONDITION = "mIsSatisfyTempSocCondition=";
        private final String STOP_COLLECT_DATA_TIME = "stopCollectTempSocTime=";
        private final String TOTAL_TIME_IN38 = "totalTimeIn38C=";
        private final String TOTAL_TIME_IN43 = "totalTimeIn43C=";
        private final String TOTAL_TIME_IN_HIGH_SOC = "totalTimeInHighSoc=";
        private final int COLLECT_DAYS = 7;
        private long startTimeIn38C = 0;
        private long startTimeIn43C = 0;
        private long startTimeInHighSoc = 0;
        private long stopCollectTempSocTime = 0;
        private boolean mIsSatisfyTempSocCondition = false;
        private List<List<String>> allDaysData = new ArrayList();

        BatteryTempSocTimeInfo() {
            MiuiBatteryStatsService.this = this$0;
        }

        public void collectTempSocData(int temp, int soc) {
            if (this.stopCollectTempSocTime == 0) {
                this.stopCollectTempSocTime = System.currentTimeMillis() + 86400000;
                if (MiuiBatteryStatsService.this.DEBUG) {
                    Slog.d("MiuiBatteryStatsService", "stopCollectTempSocTime=" + MiuiBatteryStatsService.this.mHandler.formatTime(this.stopCollectTempSocTime));
                }
            }
            if (temp >= 380) {
                this.startTimeIn38C = System.currentTimeMillis();
            }
            if (temp >= 430) {
                this.startTimeIn43C = System.currentTimeMillis();
            }
            if (soc >= 99) {
                this.startTimeInHighSoc = System.currentTimeMillis();
            }
            if (temp < 380 && this.startTimeIn38C != 0) {
                this.totalTimeIn38C += System.currentTimeMillis() - this.startTimeIn38C;
                this.startTimeIn38C = 0L;
            }
            if (temp < 430 && this.startTimeIn43C != 0) {
                this.totalTimeIn43C += System.currentTimeMillis() - this.startTimeIn43C;
                this.startTimeIn43C = 0L;
            }
            if (soc < 99 && this.startTimeInHighSoc != 0) {
                this.totalTimeInHighSoc += System.currentTimeMillis() - this.startTimeInHighSoc;
                this.startTimeInHighSoc = 0L;
            }
        }

        public void processTempSocData() {
            stopCollectTempSocData();
            if (this.allDaysData.size() < 7) {
                setDataToList();
                if (MiuiBatteryStatsService.this.DEBUG) {
                    Slog.d("MiuiBatteryStatsService", "allDaysData.size()=" + this.allDaysData.size());
                }
            }
            if (this.allDaysData.size() == 7) {
                boolean isBattTempContinueAbove38C = getTotalTime(0) >= 324000000;
                boolean isBattTempContinueAbove43C = getTotalTime(1) >= 252000000;
                boolean isSocContinueAbove99 = getTotalTime(2) >= 360000000;
                if (MiuiBatteryStatsService.this.DEBUG) {
                    Slog.d("MiuiBatteryStatsService", "Time of battery temp above 38c in 7 Days is " + getTotalTime(0) + "(ms), battery temp above 43c in 7 Days is " + getTotalTime(1) + "(ms), soc above 99 in 7 Days is " + getTotalTime(2) + "(ms)");
                }
                if (isBattTempContinueAbove38C || isBattTempContinueAbove43C || isSocContinueAbove99) {
                    this.mIsSatisfyTempSocCondition = true;
                    this.allDaysData.clear();
                } else {
                    this.mIsSatisfyTempSocCondition = false;
                    this.allDaysData.remove(0);
                }
                sendAdjustVolBroadcast();
            }
            clearData();
        }

        public void stopCollectTempSocData() {
            if (this.startTimeIn38C != 0) {
                this.totalTimeIn38C += System.currentTimeMillis() - this.startTimeIn38C;
                this.startTimeIn38C = 0L;
            }
            if (this.startTimeIn43C != 0) {
                this.totalTimeIn43C += System.currentTimeMillis() - this.startTimeIn43C;
                this.startTimeIn43C = 0L;
            }
            if (this.startTimeInHighSoc != 0) {
                this.totalTimeInHighSoc += System.currentTimeMillis() - this.startTimeInHighSoc;
                this.startTimeInHighSoc = 0L;
            }
        }

        public void setDataToList() {
            List<String> dataList = new ArrayList<>();
            dataList.add(parseString(this.totalTimeIn38C));
            dataList.add(parseString(this.totalTimeIn43C));
            dataList.add(parseString(this.totalTimeInHighSoc));
            this.allDaysData.add(dataList);
            if (MiuiBatteryStatsService.this.DEBUG) {
                Slog.d("MiuiBatteryStatsService", "allDaysData=" + this.allDaysData);
            }
        }

        public void sendAdjustVolBroadcast() {
            Intent intent = new Intent(MiuiBatteryStatsService.ADJUST_VOLTAGE);
            intent.addFlags(1073741824);
            intent.putExtra(MiuiBatteryStatsService.ADJUST_VOLTAGE_EXTRA, this.mIsSatisfyTempSocCondition);
            Slog.i("MiuiBatteryStatsService", "send broadcast adjust voltage according to temp and soc, mIsSatisfyTempSocCondition = " + this.mIsSatisfyTempSocCondition);
            MiuiBatteryStatsService.this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
        }

        public void clearData() {
            this.totalTimeIn38C = 0L;
            this.totalTimeIn43C = 0L;
            this.totalTimeInHighSoc = 0L;
            this.stopCollectTempSocTime = 0L;
        }

        private long getTotalTime(int index) {
            long sum = 0;
            for (int i = 0; i < this.allDaysData.size(); i++) {
                sum += parseLong(this.allDaysData.get(i).get(index));
            }
            return sum;
        }

        public void writeDataToFile() {
            stopCollectTempSocData();
            AtomicFile file = new AtomicFile(new File("/data/system/battery-info.txt"));
            StringBuilder lines = new StringBuilder();
            lines.append("mIsSatisfyTempSocCondition=").append(this.mIsSatisfyTempSocCondition).append("\n");
            lines.append("stopCollectTempSocTime=").append(this.stopCollectTempSocTime).append("\n");
            lines.append("totalTimeIn38C=").append(this.totalTimeIn38C).append("\n");
            lines.append("totalTimeIn43C=").append(this.totalTimeIn43C).append("\n");
            lines.append("totalTimeInHighSoc=").append(this.totalTimeInHighSoc).append("\n");
            for (List list : this.allDaysData) {
                for (int i = 0; i < list.size(); i++) {
                    lines.append((Object) list.get(i)).append(",");
                }
                lines.append("\n");
            }
            FileOutputStream fos = null;
            try {
                fos = file.startWrite();
                fos.write(lines.toString().getBytes());
                file.finishWrite(fos);
            } catch (IOException e) {
                file.failWrite(fos);
                Slog.e("MiuiBatteryStatsService", "failed to write battery_info.txt : " + e);
            }
        }

        public void readDataFromFile() {
            File battInfoFile = new File("/data/system/battery-info.txt");
            if (!battInfoFile.exists()) {
                Slog.i("MiuiBatteryStatsService", "Can't find battery info file");
                return;
            }
            try {
                String text = FileUtils.readTextFile(battInfoFile, 0, null);
                String[] lines = text.split("\n");
                for (int i = ArrayUtils.size(lines) - 1; i >= 0; i--) {
                    if (!TextUtils.isEmpty(lines[i])) {
                        if (lines[i].startsWith("mIsSatisfyTempSocCondition=")) {
                            this.mIsSatisfyTempSocCondition = Boolean.parseBoolean(lines[i].substring("mIsSatisfyTempSocCondition=".length()));
                        }
                        if (lines[i].startsWith("stopCollectTempSocTime=")) {
                            this.stopCollectTempSocTime = parseLong(lines[i].substring("stopCollectTempSocTime=".length()));
                        } else if (lines[i].startsWith("totalTimeIn38C=")) {
                            this.totalTimeIn38C = parseLong(lines[i].substring("totalTimeIn38C=".length()));
                        } else if (lines[i].startsWith("totalTimeIn43C=")) {
                            this.totalTimeIn43C = parseLong(lines[i].substring("totalTimeIn43C=".length()));
                        } else if (!lines[i].startsWith("totalTimeInHighSoc=")) {
                            if (ArrayUtils.size(lines[i].split(",")) == 3) {
                                String[] listOfCurrentline = lines[i].split(",");
                                this.allDaysData.add(Arrays.asList(listOfCurrentline));
                            }
                        } else {
                            this.totalTimeInHighSoc = parseLong(lines[i].substring("totalTimeInHighSoc=".length()));
                        }
                    }
                }
                if (MiuiBatteryStatsService.this.DEBUG) {
                    Slog.d("MiuiBatteryStatsService", "mIsSatisfyTempSocCondition=" + this.mIsSatisfyTempSocCondition + ", stopCollectTempSocTime=" + MiuiBatteryStatsService.this.mHandler.formatTime(this.stopCollectTempSocTime) + ", totalTimeIn38C=" + this.totalTimeIn38C + ", totalTimeIn43C=" + this.totalTimeIn43C + ", totalTimeInHighSoc=" + this.totalTimeInHighSoc + ", allDaysData=" + this.allDaysData);
                }
            } catch (IOException e) {
                Slog.e("MiuiBatteryStatsService", "can't read file : " + e);
            }
        }

        private String parseString(long time) {
            return String.valueOf(time);
        }

        private long parseLong(String time) {
            try {
                return Long.parseLong(time);
            } catch (NumberFormatException e) {
                Slog.e("MiuiBatteryStatsService", "Invalid string time " + time);
                return 0L;
            }
        }
    }

    /* loaded from: classes.dex */
    public class BatteryTempVoltageTimeInfo {
        private String condition;
        private long startHighTempTime = 0;
        private long startHighVoltageTime = 0;
        private long startHighTempVoltageTime = 0;
        private long startFullChargeTime = 0;
        private long highTempTotalTime = 0;
        private long highVoltageTotalTime = 0;
        private long fullChargeTotalTime = 0;
        private long highTempVoltageTotalTime = 0;

        public BatteryTempVoltageTimeInfo(String status) {
            MiuiBatteryStatsService.this = this$0;
            this.condition = status;
        }

        public void collectTime(int temp, int voltage, int chargeStatus) {
            if (temp > 400 && this.startHighTempTime == 0) {
                this.startHighTempTime = System.currentTimeMillis();
            }
            if (voltage > 4100 && this.startHighVoltageTime == 0) {
                this.startHighVoltageTime = System.currentTimeMillis();
            }
            if (temp > 400 && voltage > 4100 && this.startHighTempVoltageTime == 0) {
                this.startHighTempVoltageTime = System.currentTimeMillis();
            }
            if (chargeStatus == 5 && this.startFullChargeTime == 0) {
                this.startFullChargeTime = System.currentTimeMillis();
            }
            if (temp <= 400 && this.startHighTempTime != 0) {
                this.highTempTotalTime += System.currentTimeMillis() - this.startHighTempTime;
                this.startHighTempTime = 0L;
            }
            if (voltage <= 4100 && this.startHighVoltageTime != 0) {
                this.highVoltageTotalTime += System.currentTimeMillis() - this.startHighVoltageTime;
                this.startHighVoltageTime = 0L;
            }
            if ((temp <= 400 || voltage <= 4100) && this.startHighTempVoltageTime != 0) {
                this.highTempVoltageTotalTime += System.currentTimeMillis() - this.startHighTempVoltageTime;
                this.startHighTempVoltageTime = 0L;
            }
            if (chargeStatus != 5 && this.startFullChargeTime != 0) {
                this.fullChargeTotalTime += System.currentTimeMillis() - this.startFullChargeTime;
                this.startFullChargeTime = 0L;
            }
        }

        public void stopTime() {
            if (this.startHighTempTime != 0) {
                this.highTempTotalTime += System.currentTimeMillis() - this.startHighTempTime;
                this.startHighTempTime = 0L;
            }
            if (this.startHighVoltageTime != 0) {
                this.highVoltageTotalTime += System.currentTimeMillis() - this.startHighVoltageTime;
                this.startHighVoltageTime = 0L;
            }
            if (this.startHighTempVoltageTime != 0) {
                this.highTempVoltageTotalTime += System.currentTimeMillis() - this.startHighTempVoltageTime;
                this.startHighTempVoltageTime = 0L;
            }
            if (this.startFullChargeTime != 0) {
                this.fullChargeTotalTime += System.currentTimeMillis() - this.startFullChargeTime;
                this.startFullChargeTime = 0L;
            }
        }

        public void clearTime() {
            this.highTempTotalTime = 0L;
            this.highVoltageTotalTime = 0L;
            this.highTempVoltageTotalTime = 0L;
            this.fullChargeTotalTime = 0L;
        }

        public long getHighTempTotalTime() {
            return this.highTempTotalTime;
        }

        public long getHighVoltageTotalTime() {
            return this.highVoltageTotalTime;
        }

        public long getHighTempVoltageTotalTime() {
            return this.highTempVoltageTotalTime;
        }

        public long getFullChargeTotalTime() {
            return this.fullChargeTotalTime;
        }

        public String getCondition() {
            return this.condition;
        }
    }

    /* loaded from: classes.dex */
    public class BatteryTempInfo {
        private String condition;
        private int totalTemp = 0;
        private int dataCount = 0;
        private int maxTemp = -300;
        private int minTemp = 600;
        private int averageTemp = 0;

        public BatteryTempInfo(String condition) {
            MiuiBatteryStatsService.this = this$0;
            this.condition = condition;
        }

        public void reset() {
            this.totalTemp = 0;
            this.dataCount = 0;
            this.maxTemp = -300;
            this.minTemp = 600;
            this.averageTemp = 0;
        }

        public int getTotalTemp() {
            return this.totalTemp;
        }

        public void setTotalTemp(int temp) {
            this.totalTemp += temp;
        }

        public int getDataCount() {
            return this.dataCount;
        }

        public void setDataCount() {
            this.dataCount++;
        }

        public int getMaxTemp() {
            return this.maxTemp;
        }

        public void setMaxTemp(int maxTemp) {
            this.maxTemp = maxTemp;
        }

        public int getMinTemp() {
            return this.minTemp;
        }

        public void setMinTemp(int minTemp) {
            this.minTemp = minTemp;
        }

        public int getAverageTemp() {
            return this.averageTemp;
        }

        public void setAverageTemp() {
            this.averageTemp = this.totalTemp / this.dataCount;
        }

        public String getCondition() {
            return this.condition;
        }
    }

    /* loaded from: classes.dex */
    class TrackBatteryUsbInfo {
        public static final String ACTION_TRACK_EVENT = "onetrack.action.TRACK_EVENT";
        public static final String ANALYTICS_PACKAGE = "com.miui.analytics";
        public static final String APP_TIME = "top3_app";
        public static final String AVETEMP = "ave_temp";
        public static final String BATTERY_APP_ID = "31000000094";
        public static final String BATTERY_CHARGE_ACTION_EVENT = "charge_action";
        public static final String BATTERY_CHARGE_EVENT = "charge";
        public static final String BATTERY_HEALTH_EVENT = "battery_health";
        public static final String BATTERY_HIGH_TEMP_VOLTAGE = "battery_high_temp_voltage";
        public static final String BATTERY_POWER_OFF_EVENT = "power_off";
        public static final String BATTERY_TEMP_EVENT = "battery_temp";
        public static final String BATT_AUTH = "battery_authentic";
        public static final String BATT_RESISTANCE = "resistance";
        public static final String BATT_THERMAL_LEVEL = "thermal_level";
        public static final String CAPACITY_CHANGE_VALUE = "capacity_change_value";
        public static final String CAPACITY_NONLINEAR_CHANGE_EVENT = "nonlinear_change_of_capacity";
        public static final String CAPCAITY = "capacity";
        public static final String CHARGE_BAT_MAX_TEMP = "charge_battery_max_temp";
        public static final String CHARGE_BAT_MIN_TEMP = "charge_battery_min_temp";
        public static final String CHARGE_END_CAPACITY = "charge_end_capacity";
        public static final String CHARGE_END_TIME = "charge_end_time";
        public static final String CHARGE_FULL = "charger_full";
        public static final String CHARGE_POWER = "charge_power";
        public static final String CHARGE_START_CAPACITY = "charge_start_capacity";
        public static final String CHARGE_START_TIME = "charge_start_time";
        public static final String CHARGE_STATUS = "charge_status";
        public static final String CHARGE_TOTAL_TIME = "charge_total_time";
        public static final String CHARGE_TYPE = "charger_type";
        public static final String CYCLE_COUNT = "cyclecount";
        public static final String DATA_UNLOCK = "data_unlock";
        public static final int FLAG_NON_ANONYMOUS = 2;
        public static final String FULL_CHARGE_END_TIME = "full_charge_end_time";
        public static final String FULL_CHARGE_START_TIME = "full_charge_start_time";
        public static final String FULL_CHARGE_TOTAL_TIME = "full_charge_total_time";
        public static final String HIGH_TEMP_TIME = "high_temp_time";
        public static final String HIGH_TEMP_VOLTAGE_TIME = "high_temp_voltage_time";
        public static final String HIGH_VOLTAGE_TIME = "high_voltage_time";
        public static final String IBAT = "ibat";
        public static final String INTERMITTENT_CHARGE = "intermittent_charge";
        public static final String INTERMITTENT_CHARGE_EVENT = "intermittent_charge";
        public static final String MAXTEMP = "max_temp";
        public static final String MINTEMP = "min_temp";
        public static final String PARAM_APP_ID = "APP_ID";
        public static final String PARAM_EVENT_NAME = "EVENT_NAME";
        public static final String PARAM_PACKAGE = "PACKAGE";
        public static final String PD_APDO_MAX = "pd_apdoMax";
        public static final String PD_AUTHENTICATION = "pd_authentication";
        public static final String SCENE = "battery_charge_discharge_state";
        public static final String SCREEN_OFF_TIME = "screen_off_time";
        public static final String SCREEN_ON_TIME = "screen_on_time";
        public static final String SERIES_DELTA_VOLTAGE = "series_delta_voltage";
        public static final String SERIES_DELTA_VOLTAGE_EVENT = "series_delta_voltage";
        public static final String SHUTDOWN_DELAY = "shutdown_delay";
        public static final String SLOW_CHARGE_EVENT = "slow_charge";
        public static final String SLOW_CHARGE_TYPE = "slow_charge_type";
        public static final String SOH = "soh";
        public static final String TBAT = "Tbat";
        public static final String TX_ADAPTER = "tx_adapter";
        public static final String USB32 = "USB32";
        public static final String USB_APP_ID = "31000000092";
        public static final String USB_CURRENT = "usb_current";
        public static final String USB_DEVICE_EVENT = "usb_deivce";
        public static final String USB_FUNCTION = "usb_function";
        public static final String USB_VOLTAGE = "usb_voltage";
        public static final String VBAT = "vbat";
        public static final String VBUS_DISABLE = "vbus_disable";
        public static final String VBUS_DISABLE_EVENT = "vbus_disable";
        public static final String WIRELESS_REVERSE_CHARGE = "wireless_reverse_enable";
        public static final String WIRELESS_REVERSE_CHARGE_EVENT = "wireless_reverse_charge";

        TrackBatteryUsbInfo() {
            MiuiBatteryStatsService.this = this$0;
        }
    }
}
