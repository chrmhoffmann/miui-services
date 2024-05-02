package com.android.server;

import android.app.ActivityThread;
import android.app.Notification;
import android.app.NotificationManager;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.nfc.NfcAdapter;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.os.UEventObserver;
import android.os.UserHandle;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Slog;
import android.widget.Toast;
import com.android.internal.notification.SystemNotificationChannels;
import com.miui.base.MiuiStubRegistry;
import com.miui.server.MiuiFboService;
import com.miui.server.SecurityManagerService;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import miui.os.Build;
import miui.util.IMiCharge;
import miuix.appcompat.R;
import miuix.appcompat.app.AlertDialog;
/* loaded from: classes.dex */
public class MiuiBatteryServiceImpl implements MiuiBatteryServiceStub {
    public static final String KEY_FAST_CHARGE_ENABLED = "key_fast_charge_enabled";
    private volatile BluetoothA2dp mA2dp;
    private Context mContext;
    ContentObserver mFastChargeObserver;
    private BatteryHandler mHandler;
    private volatile BluetoothHeadset mHeadset;
    private final String TAG = "MiuiBatteryServiceImpl";
    private final boolean DEBUG = SystemProperties.getBoolean("persist.sys.debug_impl", false);
    private boolean mIsSatisfyTempSocCondition = false;
    private boolean mIsSatisfyTimeRegionCondition = false;
    private int BtConnectedCount = 0;
    private IMiCharge mMiCharge = IMiCharge.getInstance();
    private MiuiFboService miuiFboService = MiuiFboService.getInstance();
    public final String[] SUPPORT_COUNTRY = {"IT", "FR", "ES", "DE", "PL", "GB"};
    private final BluetoothProfile.ServiceListener mProfileServiceListener = new BluetoothProfile.ServiceListener() { // from class: com.android.server.MiuiBatteryServiceImpl.5
        @Override // android.bluetooth.BluetoothProfile.ServiceListener
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            switch (profile) {
                case 1:
                    MiuiBatteryServiceImpl.this.mHeadset = (BluetoothHeadset) proxy;
                    return;
                case 2:
                    MiuiBatteryServiceImpl.this.mA2dp = (BluetoothA2dp) proxy;
                    return;
                default:
                    return;
            }
        }

        @Override // android.bluetooth.BluetoothProfile.ServiceListener
        public void onServiceDisconnected(int profile) {
            switch (profile) {
                case 1:
                    MiuiBatteryServiceImpl.this.mHeadset = null;
                    return;
                case 2:
                    MiuiBatteryServiceImpl.this.mA2dp = null;
                    return;
                default:
                    return;
            }
        }
    };
    private boolean mSupportWirelessCharge = this.mMiCharge.isWirelessChargingSupported();
    private boolean mSupportSB = this.mMiCharge.isFunctionSupported("smart_batt");

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<MiuiBatteryServiceImpl> {

        /* compiled from: MiuiBatteryServiceImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final MiuiBatteryServiceImpl INSTANCE = new MiuiBatteryServiceImpl();
        }

        public MiuiBatteryServiceImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public MiuiBatteryServiceImpl provideNewInstance() {
            return new MiuiBatteryServiceImpl();
        }
    }

    public void init(Context context) {
        this.mContext = context;
        this.mHandler = new BatteryHandler(MiuiFgThread.get().getLooper());
        this.mFastChargeObserver = new ContentObserver(new Handler()) { // from class: com.android.server.MiuiBatteryServiceImpl.1
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                String socDecimal = MiuiBatteryServiceImpl.this.mMiCharge.getSocDecimal();
                String socDecimalRate = MiuiBatteryServiceImpl.this.mMiCharge.getSocDecimalRate();
                if (!TextUtils.isEmpty(socDecimal) && !TextUtils.isEmpty(socDecimalRate)) {
                    int socDecimalValue = MiuiBatteryServiceImpl.this.mHandler.parseInt(socDecimal);
                    int socDecimalRateVaule = MiuiBatteryServiceImpl.this.mHandler.parseInt(socDecimalRate);
                    MiuiBatteryServiceImpl.this.mHandler.sendMessage(4, socDecimalValue, socDecimalRateVaule);
                }
            }
        };
        BroadcastReceiver batteryChangedReceiver = new BroadcastReceiver() { // from class: com.android.server.MiuiBatteryServiceImpl.2
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                int plugType = intent.getIntExtra("plugged", -1);
                int batteryLevel = intent.getIntExtra("level", -1);
                boolean plugged = plugType != 0;
                if (MiuiBatteryServiceImpl.this.mSupportWirelessCharge && !plugged && MiuiBatteryServiceImpl.this.mMiCharge.getWirelessChargingStatus() == 0) {
                    MiuiBatteryServiceImpl.this.mHandler.sendMessage(0, batteryLevel);
                }
            }
        };
        this.mContext.registerReceiver(batteryChangedReceiver, new IntentFilter("android.intent.action.BATTERY_CHANGED"));
        String chargeType = this.mMiCharge.getBatteryChargeType();
        if (chargeType != null && chargeType.length() > 0 && !SystemProperties.getBoolean("persist.vendor.charge.oneTrack", false)) {
            MiuiBatteryStatsService.getInstance(context);
        }
        if ((SystemProperties.get("ro.product.device", "").startsWith("mona") || SystemProperties.get("ro.product.device", "").startsWith("thor")) && !Build.IS_INTERNATIONAL_BUILD) {
            MiuiBatteryAuthentic.getInstance(context);
        }
        if (SystemProperties.get("ro.product.mod_device", "").startsWith("star") || SystemProperties.get("ro.product.mod_device", "").startsWith("mars")) {
            int initBtConnectCount = initBtConnectCount();
            this.BtConnectedCount = initBtConnectCount;
            if (initBtConnectCount > 0) {
                this.mHandler.sendMessageDelayed(10, true, 1000L);
            }
            BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() { // from class: com.android.server.MiuiBatteryServiceImpl.3
                @Override // android.content.BroadcastReceiver
                public void onReceive(Context context2, Intent intent) {
                    String action = intent.getAction();
                    if ("android.bluetooth.adapter.action.STATE_CHANGED".equals(action)) {
                        int value = intent.getIntExtra("android.bluetooth.adapter.extra.STATE", 0);
                        if (value == 15 || value == 10) {
                            MiuiBatteryServiceImpl.this.BtConnectedCount = 0;
                            MiuiBatteryServiceImpl.this.mHandler.sendMessage(10, false);
                        }
                    } else if ("android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED".equals(action) || "android.bluetooth.headset.profile.action.CONNECTION_STATE_CHANGED".equals(action)) {
                        int state = intent.getIntExtra("android.bluetooth.profile.extra.STATE", -1);
                        int preState = intent.getIntExtra("android.bluetooth.profile.extra.PREVIOUS_STATE", -1);
                        if (state == 2) {
                            MiuiBatteryServiceImpl.this.BtConnectedCount++;
                            MiuiBatteryServiceImpl.this.mHandler.sendMessage(10, true);
                        } else if (state == 0 && preState != 1) {
                            MiuiBatteryServiceImpl.this.BtConnectedCount--;
                            if (MiuiBatteryServiceImpl.this.BtConnectedCount <= 0) {
                                MiuiBatteryServiceImpl.this.BtConnectedCount = 0;
                                MiuiBatteryServiceImpl.this.mHandler.sendMessage(10, false);
                            }
                        }
                    }
                }
            };
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED");
            filter.addAction("android.bluetooth.headset.profile.action.CONNECTION_STATE_CHANGED");
            filter.addAction("android.bluetooth.adapter.action.STATE_CHANGED");
            this.mContext.registerReceiver(bluetoothReceiver, filter);
        }
        if (this.mSupportSB) {
            BroadcastReceiver updateBattVolIntentReceiver = new BroadcastReceiver() { // from class: com.android.server.MiuiBatteryServiceImpl.4
                @Override // android.content.BroadcastReceiver
                public void onReceive(Context context2, Intent intent) {
                    String action = intent.getAction();
                    if (MiuiBatteryServiceImpl.this.DEBUG) {
                        Slog.d("MiuiBatteryServiceImpl", "action = " + action);
                    }
                    if ("android.intent.action.BOOT_COMPLETED".equals(action) || MiuiBatteryStatsService.UPDATE_BATTERY_DATA.equals(action)) {
                        MiuiBatteryServiceImpl.this.mHandler.sendMessageDelayed(15, 0L);
                    } else if (MiuiBatteryStatsService.ADJUST_VOLTAGE.equals(action)) {
                        int smartBatt = MiuiBatteryServiceImpl.this.mHandler.getSBState();
                        MiuiBatteryServiceImpl.this.mIsSatisfyTempSocCondition = intent.getBooleanExtra(MiuiBatteryStatsService.ADJUST_VOLTAGE_EXTRA, false);
                        if (MiuiBatteryServiceImpl.this.mIsSatisfyTempSocCondition && smartBatt != 15) {
                            MiuiBatteryServiceImpl.this.mMiCharge.setSBState(15);
                        } else if (!MiuiBatteryServiceImpl.this.mIsSatisfyTempSocCondition && smartBatt == 15) {
                            if (MiuiBatteryServiceImpl.this.mIsSatisfyTimeRegionCondition) {
                                MiuiBatteryServiceImpl.this.mMiCharge.setSBState(10);
                            } else {
                                MiuiBatteryServiceImpl.this.mMiCharge.setSBState(0);
                            }
                        }
                    }
                }
            };
            IntentFilter filter2 = new IntentFilter(MiuiBatteryStatsService.UPDATE_BATTERY_DATA);
            filter2.addAction("android.intent.action.BOOT_COMPLETED");
            filter2.addAction(MiuiBatteryStatsService.ADJUST_VOLTAGE);
            this.mContext.registerReceiver(updateBattVolIntentReceiver, filter2);
        }
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor(KEY_FAST_CHARGE_ENABLED), false, this.mFastChargeObserver);
    }

    public void setBatteryStatusWithFbo(int batteryStatus, int batteryLevel, int batteryTemperature) {
        this.miuiFboService.setBatteryInfos(batteryStatus, batteryLevel, batteryTemperature);
        if (this.miuiFboService.getGlobalSwitch() && (batteryStatus <= 0 || batteryLevel < 70 || batteryTemperature >= 500)) {
            MiuiFboService.sendMessage(MiuiFboService.STOP, 3, 0L);
        } else if (this.miuiFboService.getNativeIsRunning() && this.miuiFboService.getGlobalSwitch() && batteryTemperature > 400) {
            MiuiFboService.sendMessage(MiuiFboService.STOPDUETOBATTERYTEMPERATURE, 6, 0L);
        } else if (!this.miuiFboService.getNativeIsRunning() && this.miuiFboService.getGlobalSwitch() && !this.miuiFboService.getDueToScreenWait() && batteryTemperature < 350) {
            MiuiFboService.sendMessage(MiuiFboService.CONTINUE, 2, 0L);
        }
    }

    private int initBtConnectCount() {
        int a2dpCount = 0;
        int headsetCount = 0;
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter != null) {
            btAdapter.getProfileProxy(this.mContext, this.mProfileServiceListener, 2);
            btAdapter.getProfileProxy(this.mContext, this.mProfileServiceListener, 1);
        }
        if (this.mA2dp != null) {
            a2dpCount = this.mA2dp.getConnectedDevices().size();
        }
        if (this.mHeadset != null) {
            headsetCount = this.mHeadset.getConnectedDevices().size();
        }
        return a2dpCount + headsetCount;
    }

    /* loaded from: classes.dex */
    public class BatteryHandler extends Handler {
        private static final String ACTION_REVERSE_PEN_CHARGE_STATE = "miui.intent.action.ACTION_PEN_REVERSE_CHARGE_STATE";
        private static final String ACTION_TYPE_C_HIGH_TEMP = "miui.intent.action.ACTION_TYPE_C_HIGH_TEMP";
        private static final String ACTION_WIRELESS_CHARGING = "miui.intent.action.ACTION_WIRELESS_CHARGING";
        private static final String ACTION_WIRELESS_CHG_WARNING_ACTIVITY = "miui.intent.action.ACTIVITY_WIRELESS_CHG_WARNING";
        private static final String ACTION_WIRELESS_FW_UPDATE = "miui.intent.action.ACTION_WIRELESS_FW_UPDATE";
        private static final String CONNECTOR_TEMP_EVENT = "POWER_SUPPLY_CONNECTOR_TEMP";
        private static final String EXTRA_CAR_CHG = "miui.intent.extra.CAR_CHARGE";
        private static final String EXTRA_POWER_MAX = "miui.intent.extra.POWER_MAX";
        private static final String EXTRA_REVERSE_PEN_CHARGE_STATE = "miui.intent.extra.ACTION_PEN_REVERSE_CHARGE_STATE";
        private static final String EXTRA_REVERSE_PEN_SOC = "miui.intent.extra.REVERSE_PEN_SOC";
        private static final String EXTRA_TYPE_C_HIGH_TEMP = "miui.intent.extra.EXTRA_TYPE_C_HIGH_TEMP";
        private static final String EXTRA_WIRELESS_CHARGING = "miui.intent.extra.WIRELESS_CHARGING";
        private static final String EXTRA_WIRELESS_FW_UPDATE = "miui.intent.extra.EXTRA_WIRELESS_FW_UPDATE";
        private static final String HAPTIC_STATE = "haptic_feedback_disable";
        private static final String HVDCP3_TYPE_EVENT = "POWER_SUPPLY_HVDCP3_TYPE";
        static final int MSG_ADJUST_VOLTAGE = 15;
        static final int MSG_BATTERY_CHANGED = 0;
        static final int MSG_BLUETOOTH_CHANGED = 10;
        static final int MSG_CONNECTOR_TEMP = 16;
        static final int MSG_HVDCP3_DETECT = 2;
        static final int MSG_NFC_DISABLED = 9;
        static final int MSG_NFC_ENABLED = 8;
        static final int MSG_POWER_OFF = 14;
        static final int MSG_QUICKCHARGE_DETECT = 3;
        static final int MSG_REVERSE_PEN_CHG_STATE = 12;
        static final int MSG_SHUTDOWN_DELAY = 7;
        static final int MSG_SHUTDOWN_DELAY_WARNING = 13;
        static final int MSG_SOC_DECIMAL = 4;
        static final int MSG_WIRELESS_CHARGE_CLOSE = 5;
        static final int MSG_WIRELESS_CHARGE_OPEN = 6;
        static final int MSG_WIRELESS_FW_STATE = 11;
        static final int MSG_WIRELESS_TX = 1;
        private static final String NFC_CLOED = "nfc_closd_from_wirelss";
        private static final String QUICK_CHARGE_TYPE_EVENT = "POWER_SUPPLY_QUICK_CHARGE_TYPE";
        private static final int REDUCE_FULL_CHARGE_VBATT_LOW_LEVEL = 10;
        private static final int REDUCE_FULL_CHARGE_VBATT_MEDIUM_LEVEL = 15;
        private static final int RESET_FULL_CHARGE_VBATT = 0;
        private static final int RETRY_UPDATE_DELAY = 60000;
        private static final String REVERSE_CHG_MODE_EVENT = "POWER_SUPPLY_REVERSE_CHG_MODE";
        private static final String REVERSE_CHG_STATE_EVENT = "POWER_SUPPLY_REVERSE_CHG_STATE";
        private static final String REVERSE_PEN_CHG_STATE_EVENT = "POWER_SUPPLY_REVERSE_PEN_CHG_STATE";
        private static final String SHUTDOWN_DELAY_EVENT = "POWER_SUPPLY_SHUTDOWN_DELAY";
        private static final String SOC_DECIMAL_EVENT = "POWER_SUPPLY_SOC_DECIMAL";
        private static final String SOC_DECIMAL_RATE_EVENT = "POWER_SUPPLY_SOC_DECIMAL_RATE";
        private static final int UPDATE_DELAY = 1000;
        private static final int WIRELESS_AUTO_CLOSED_STATE = 1;
        private static final int WIRELESS_CHG_ERROR_STATE = 2;
        private static final int WIRELESS_LOW_BATTERY_LEVEL_STATE = 4;
        private static final int WIRELESS_NO_ERROR_STATE = 0;
        private static final int WIRELESS_OTHER_WIRELESS_CHG_STATE = 3;
        private static final String WIRELESS_REVERSE_CHARGING = "wireless_reverse_charging";
        private static final String WIRELESS_TX_TYPE_EVENT = "POWER_SUPPLY_TX_ADAPTER";
        private static final String WLS_FW_STATE_EVENT = "POWER_SUPPLY_WLS_FW_STATE";
        private int mChargingNotificationId;
        private boolean mClosedNfcFromCharging;
        private final ContentResolver mContentResolver;
        private Date mEndHighTempDate;
        private boolean mHapticState;
        private int mLastCloseReason;
        private int mLastConnectorTemp;
        private int mLastHvdcpType;
        private int mLastOpenStatus;
        private int mLastPenReverseChargeState;
        private int mLastQuickChargeType;
        private int mLastShutdownDelay;
        private int mLastWirelessFwState;
        private int mLastWirelessTxType;
        private NfcAdapter mNfcAdapter;
        private Date mStartHithTempDate;
        private final UEventObserver mUEventObserver;
        private boolean mUpdateSocDecimal;
        private boolean mShowEnableNfc = false;
        private boolean mShowDisableNfc = false;
        private boolean mIsReverseWirelessCharge = false;
        private boolean mRetryAfterOneMin = false;
        private int mCount = 0;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public BatteryHandler(Looper looper) {
            super(looper);
            MiuiBatteryServiceImpl.this = this$0;
            initChargeStatus();
            initBatteryAuthentic();
            this.mContentResolver = this$0.mContext.getContentResolver();
            BatteryUEventObserver batteryUEventObserver = new BatteryUEventObserver();
            this.mUEventObserver = batteryUEventObserver;
            batteryUEventObserver.startObserving(WIRELESS_TX_TYPE_EVENT);
            batteryUEventObserver.startObserving(HVDCP3_TYPE_EVENT);
            batteryUEventObserver.startObserving(QUICK_CHARGE_TYPE_EVENT);
            batteryUEventObserver.startObserving(SOC_DECIMAL_EVENT);
            batteryUEventObserver.startObserving(REVERSE_CHG_STATE_EVENT);
            batteryUEventObserver.startObserving(REVERSE_CHG_MODE_EVENT);
            batteryUEventObserver.startObserving(SHUTDOWN_DELAY_EVENT);
            batteryUEventObserver.startObserving(WLS_FW_STATE_EVENT);
            batteryUEventObserver.startObserving(REVERSE_PEN_CHG_STATE_EVENT);
            batteryUEventObserver.startObserving(CONNECTOR_TEMP_EVENT);
        }

        private void sendSocDecimaBroadcast() {
            String socDecimal = MiuiBatteryServiceImpl.this.mMiCharge.getSocDecimal();
            String socDecimalRate = MiuiBatteryServiceImpl.this.mMiCharge.getSocDecimalRate();
            if (socDecimal != null && socDecimal.length() != 0 && socDecimalRate != null && socDecimalRate.length() != 0) {
                int socDecimalValue = parseInt(socDecimal);
                int socDecimalRateVaule = parseInt(socDecimalRate);
                sendMessage(4, socDecimalValue, socDecimalRateVaule);
            }
        }

        private void initChargeStatus() {
            int txAdapterValue;
            int quickChargeValue;
            String quickChargeType = MiuiBatteryServiceImpl.this.mMiCharge.getQuickChargeType();
            String txAdapter = MiuiBatteryServiceImpl.this.mMiCharge.getTxAdapt();
            Slog.d("MiuiBatteryServiceImpl", "quickChargeType = " + quickChargeType + " txAdapter = " + txAdapter);
            if (quickChargeType != null && quickChargeType.length() != 0 && (quickChargeValue = parseInt(quickChargeType)) > 0) {
                this.mLastQuickChargeType = quickChargeValue;
                sendMessage(3, quickChargeValue);
                if (quickChargeValue >= 3) {
                    sendSocDecimaBroadcast();
                }
            }
            if (txAdapter != null && txAdapter.length() != 0 && (txAdapterValue = parseInt(txAdapter)) > 0) {
                this.mLastWirelessTxType = txAdapterValue;
                sendMessage(1, txAdapterValue);
            }
        }

        private void initBatteryAuthentic() {
            String batteryAuthentic;
            if (SystemProperties.get("ro.product.name", "").startsWith("nabu") && (batteryAuthentic = MiuiBatteryServiceImpl.this.mMiCharge.getBatteryAuthentic()) != null && batteryAuthentic.length() != 0) {
                int batteryAuthenticValue = parseInt(batteryAuthentic);
                if (batteryAuthenticValue == 0) {
                    sendMessageDelayed(13, 30000L);
                }
            }
        }

        private boolean isSupportControlHaptic() {
            return SystemProperties.get("ro.product.device", "").startsWith("mayfly") || SystemProperties.getBoolean("persist.vendor.revchg.shutmotor", false);
        }

        public void sendMessage(int what, boolean arg) {
            removeMessages(what);
            Message m = Message.obtain(this, what);
            m.arg1 = arg ? 1 : 0;
            sendMessage(m);
        }

        public void sendMessage(int what, int arg1) {
            removeMessages(what);
            Message m = Message.obtain(this, what);
            m.arg1 = arg1;
            sendMessage(m);
        }

        public void sendMessage(int what, int arg1, int arg2) {
            removeMessages(what);
            Message m = Message.obtain(this, what);
            m.arg1 = arg1;
            m.arg2 = arg2;
            sendMessage(m);
        }

        public void sendMessageDelayed(int what, long delayMillis) {
            removeMessages(what);
            Message m = Message.obtain(this, what);
            sendMessageDelayed(m, delayMillis);
        }

        public void sendMessageDelayed(int what, boolean arg, long delayMillis) {
            removeMessages(what);
            Message m = Message.obtain(this, what);
            m.arg1 = arg ? 1 : 0;
            sendMessageDelayed(m, delayMillis);
        }

        public int parseInt(String argument) {
            try {
                return Integer.parseInt(argument);
            } catch (NumberFormatException e) {
                Slog.e("MiuiBatteryServiceImpl", "Invalid integer argument " + argument);
                return -1;
            }
        }

        private String getCurrentDate() {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM-dd");
            Date date = new Date(System.currentTimeMillis());
            String today = simpleDateFormat.format(date);
            return today;
        }

        private Date parseDate(String date) throws ParseException {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM-dd");
            return simpleDateFormat.parse(date);
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes.dex */
        public final class BatteryUEventObserver extends UEventObserver {
            private BatteryUEventObserver() {
                BatteryHandler.this = r1;
            }

            public void onUEvent(UEventObserver.UEvent event) {
                int connectorTemp;
                int penReverseChargeState;
                int wirelessFwState;
                int shutdownDelay;
                int quickChargeType;
                int hvdcpType;
                int wirelessTxType;
                if (event.get(BatteryHandler.WIRELESS_TX_TYPE_EVENT) != null && (wirelessTxType = BatteryHandler.this.parseInt(event.get(BatteryHandler.WIRELESS_TX_TYPE_EVENT))) != BatteryHandler.this.mLastWirelessTxType) {
                    Slog.d("MiuiBatteryServiceImpl", "Wireless_tx_type = " + wirelessTxType + " mLastWireless_tx_type = " + BatteryHandler.this.mLastWirelessTxType);
                    BatteryHandler.this.mLastWirelessTxType = wirelessTxType;
                    BatteryHandler.this.sendMessage(1, wirelessTxType);
                }
                if (event.get(BatteryHandler.HVDCP3_TYPE_EVENT) != null && (hvdcpType = BatteryHandler.this.parseInt(event.get(BatteryHandler.HVDCP3_TYPE_EVENT))) != BatteryHandler.this.mLastHvdcpType) {
                    Slog.d("MiuiBatteryServiceImpl", "HVDCP type = " + hvdcpType + " Last HVDCP type = " + BatteryHandler.this.mLastHvdcpType);
                    BatteryHandler.this.mLastHvdcpType = hvdcpType;
                    BatteryHandler.this.sendMessage(2, hvdcpType);
                }
                if (event.get(BatteryHandler.QUICK_CHARGE_TYPE_EVENT) != null && (quickChargeType = BatteryHandler.this.parseInt(event.get(BatteryHandler.QUICK_CHARGE_TYPE_EVENT))) != BatteryHandler.this.mLastQuickChargeType) {
                    Slog.i("MiuiBatteryServiceImpl", "Quick Charge type = " + quickChargeType + " Last Quick Charge type = " + BatteryHandler.this.mLastQuickChargeType);
                    BatteryHandler.this.mLastQuickChargeType = quickChargeType;
                    BatteryHandler.this.sendMessage(3, quickChargeType);
                    BatteryHandler.this.mUpdateSocDecimal = true;
                }
                if (event.get(BatteryHandler.SOC_DECIMAL_EVENT) != null && BatteryHandler.this.mLastQuickChargeType >= 3 && BatteryHandler.this.mUpdateSocDecimal) {
                    int socDecimal = BatteryHandler.this.parseInt(event.get(BatteryHandler.SOC_DECIMAL_EVENT));
                    int socDecimalRate = BatteryHandler.this.parseInt(event.get(BatteryHandler.SOC_DECIMAL_RATE_EVENT));
                    Slog.i("MiuiBatteryServiceImpl", "socDecimal = " + socDecimal + " socDecimalRate = " + socDecimalRate);
                    BatteryHandler.this.sendMessage(4, socDecimal, socDecimalRate);
                    BatteryHandler.this.mUpdateSocDecimal = false;
                }
                if (event.get(BatteryHandler.REVERSE_CHG_STATE_EVENT) != null) {
                    int closeReason = BatteryHandler.this.parseInt(event.get(BatteryHandler.REVERSE_CHG_STATE_EVENT));
                    if (MiuiBatteryServiceImpl.this.mSupportWirelessCharge && closeReason != BatteryHandler.this.mLastCloseReason) {
                        Slog.d("MiuiBatteryServiceImpl", "Wireless Reverse Charging Closed Reason  = " + closeReason + " Last Wireless Reverse charging closed reason = " + BatteryHandler.this.mLastCloseReason);
                        BatteryHandler.this.mLastCloseReason = closeReason;
                        BatteryHandler.this.sendMessage(5, closeReason);
                    }
                }
                if (event.get(BatteryHandler.REVERSE_CHG_MODE_EVENT) != null) {
                    int openStatus = BatteryHandler.this.parseInt(event.get(BatteryHandler.REVERSE_CHG_MODE_EVENT));
                    if (MiuiBatteryServiceImpl.this.mSupportWirelessCharge && openStatus != BatteryHandler.this.mLastOpenStatus) {
                        Slog.d("MiuiBatteryServiceImpl", "Wireless Reverse Charing status  = " + openStatus + " Last Wireless Reverse Charing status = " + BatteryHandler.this.mLastOpenStatus);
                        BatteryHandler.this.mLastOpenStatus = openStatus;
                        BatteryHandler.this.sendMessage(6, openStatus);
                    }
                }
                if (event.get(BatteryHandler.SHUTDOWN_DELAY_EVENT) != null && (shutdownDelay = BatteryHandler.this.parseInt(event.get(BatteryHandler.SHUTDOWN_DELAY_EVENT))) != BatteryHandler.this.mLastShutdownDelay) {
                    Slog.d("MiuiBatteryServiceImpl", "shutdown delay status  = " + shutdownDelay + " Last shutdown delay status = " + BatteryHandler.this.mLastShutdownDelay);
                    BatteryHandler.this.mLastShutdownDelay = shutdownDelay;
                    BatteryHandler.this.sendMessage(7, shutdownDelay);
                }
                if (event.get(BatteryHandler.WLS_FW_STATE_EVENT) != null && (wirelessFwState = BatteryHandler.this.parseInt(event.get(BatteryHandler.WLS_FW_STATE_EVENT))) != BatteryHandler.this.mLastWirelessFwState) {
                    Slog.d("MiuiBatteryServiceImpl", "wireless fw update status  = " + wirelessFwState + " Last wireless fw update status = " + BatteryHandler.this.mLastWirelessFwState);
                    BatteryHandler.this.mLastWirelessFwState = wirelessFwState;
                    BatteryHandler.this.sendMessage(11, wirelessFwState);
                }
                if (event.get(BatteryHandler.REVERSE_PEN_CHG_STATE_EVENT) != null && (penReverseChargeState = BatteryHandler.this.parseInt(event.get(BatteryHandler.REVERSE_PEN_CHG_STATE_EVENT))) != BatteryHandler.this.mLastPenReverseChargeState) {
                    Slog.d("MiuiBatteryServiceImpl", "current pen reverse charge state = " + penReverseChargeState + " Last pen reverse charge state = " + BatteryHandler.this.mLastPenReverseChargeState);
                    BatteryHandler.this.mLastPenReverseChargeState = penReverseChargeState;
                    BatteryHandler.this.sendMessage(12, penReverseChargeState);
                }
                if (event.get(BatteryHandler.CONNECTOR_TEMP_EVENT) != null && (connectorTemp = BatteryHandler.this.parseInt(event.get(BatteryHandler.CONNECTOR_TEMP_EVENT))) != BatteryHandler.this.mLastConnectorTemp) {
                    Slog.d("MiuiBatteryServiceImpl", "currenet connector temp = " + connectorTemp + " Last currenet connector temp = " + BatteryHandler.this.mLastConnectorTemp);
                    BatteryHandler.this.mLastConnectorTemp = connectorTemp;
                    if (connectorTemp > 650) {
                        BatteryHandler.this.sendMessage(16, connectorTemp);
                    }
                }
            }
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            NfcAdapter nfcAdapter;
            int i = 8;
            boolean z = true;
            switch (msg.what) {
                case 0:
                    int wirelessTxType = msg.arg1;
                    shouldCloseWirelessReverseCharging(wirelessTxType);
                    return;
                case 1:
                    int wirelessTxType2 = msg.arg1;
                    Intent wirelessTxTypeIntent = new Intent("miui.intent.action.ACTION_WIRELESS_TX_TYPE");
                    wirelessTxTypeIntent.putExtra("miui.intent.extra.wireless_tx_type", wirelessTxType2);
                    sendStickyBroadcast(wirelessTxTypeIntent);
                    this.mIsReverseWirelessCharge = false;
                    this.mRetryAfterOneMin = false;
                    return;
                case 2:
                    Intent hvdcpTypeIntent = new Intent("miui.intent.action.ACTION_HVDCP_TYPE");
                    hvdcpTypeIntent.putExtra("miui.intent.extra.hvdcp_type", msg.arg1);
                    sendStickyBroadcast(hvdcpTypeIntent);
                    return;
                case 3:
                    Intent quickChargeTypeIntent = new Intent("miui.intent.action.ACTION_QUICK_CHARGE_TYPE");
                    quickChargeTypeIntent.putExtra("miui.intent.extra.quick_charge_type", msg.arg1);
                    quickChargeTypeIntent.putExtra(EXTRA_POWER_MAX, getChargingPowerMax());
                    quickChargeTypeIntent.putExtra(EXTRA_CAR_CHG, getCarChargingType());
                    sendStickyBroadcast(quickChargeTypeIntent);
                    if (msg.arg1 >= 3) {
                        sendSocDecimaBroadcast();
                        return;
                    }
                    return;
                case 4:
                    Intent socDecimalIntent = new Intent("miui.intent.action.ACTION_SOC_DECIMAL");
                    socDecimalIntent.putExtra("miui.intent.extra.soc_decimal", msg.arg1);
                    socDecimalIntent.putExtra("miui.intent.extra.soc_decimal_rate", msg.arg2);
                    sendStickyBroadcast(socDecimalIntent);
                    return;
                case 5:
                    int openStatus = msg.arg1;
                    if (openStatus == 1) {
                        updateWirelessReverseChargingNotification(1);
                        return;
                    } else if (openStatus == 2) {
                        updateWirelessReverseChargingNotification(2);
                        return;
                    } else if (openStatus == 3) {
                        showWirelessCharingWarningDialog();
                        return;
                    } else {
                        return;
                    }
                case 6:
                    int openStatus2 = msg.arg1;
                    if (openStatus2 > 0) {
                        updateWirelessReverseChargingNotification(0);
                    }
                    this.mIsReverseWirelessCharge = true;
                    if (openStatus2 > 0) {
                        i = 9;
                    }
                    sendMessage(i, 0);
                    return;
                case 7:
                    Intent shutdownDelayIntent = new Intent("miui.intent.action.ACTION_SHUTDOWN_DELAY");
                    shutdownDelayIntent.putExtra("miui.intent.extra.shutdown_delay", msg.arg1);
                    sendStickyBroadcast(shutdownDelayIntent);
                    return;
                case 8:
                    int txType = msg.arg1;
                    this.mClosedNfcFromCharging = Settings.Global.getInt(this.mContentResolver, NFC_CLOED, 0) > 0;
                    try {
                        this.mNfcAdapter = NfcAdapter.getNfcAdapter(MiuiBatteryServiceImpl.this.mContext);
                    } catch (UnsupportedOperationException e) {
                        Slog.e("MiuiBatteryServiceImpl", "Get NFC failed");
                    }
                    if (txType == 0 && this.mClosedNfcFromCharging && (nfcAdapter = this.mNfcAdapter) != null && !nfcAdapter.isEnabled()) {
                        if (this.mNfcAdapter.enable()) {
                            Slog.d("MiuiBatteryServiceImpl", "try to open NFC " + this.mCount + " times success");
                            this.mClosedNfcFromCharging = false;
                            this.mShowEnableNfc = true;
                            this.mCount = 0;
                            Settings.Global.putInt(this.mContentResolver, NFC_CLOED, 0);
                        } else {
                            int i2 = this.mCount;
                            if (i2 < 3) {
                                this.mCount = i2 + 1;
                                sendMessageDelayed(8, 1000L);
                            } else {
                                Slog.d("MiuiBatteryServiceImpl", "open NFC failed");
                                this.mCount = 0;
                                if (!this.mRetryAfterOneMin) {
                                    sendMessageDelayed(8, SecurityManagerService.LOCK_TIME_OUT);
                                    this.mRetryAfterOneMin = true;
                                }
                            }
                        }
                    }
                    if (!this.mIsReverseWirelessCharge) {
                        if (this.mShowEnableNfc) {
                            Toast.makeText(MiuiBatteryServiceImpl.this.mContext, 286196180, 0).show();
                            this.mShowEnableNfc = false;
                            return;
                        }
                        return;
                    } else if (isSupportControlHaptic()) {
                        if (Settings.System.getInt(this.mContentResolver, HAPTIC_STATE, 0) <= 0) {
                            z = false;
                        }
                        this.mHapticState = z;
                        if (z) {
                            Settings.System.putInt(this.mContentResolver, HAPTIC_STATE, 0);
                        }
                        Toast.makeText(MiuiBatteryServiceImpl.this.mContext, 286196181, 0).show();
                        return;
                    } else if (this.mShowEnableNfc) {
                        Toast.makeText(MiuiBatteryServiceImpl.this.mContext, 286196182, 0).show();
                        this.mShowEnableNfc = false;
                        return;
                    } else {
                        return;
                    }
                case 9:
                    try {
                        this.mNfcAdapter = NfcAdapter.getNfcAdapter(MiuiBatteryServiceImpl.this.mContext);
                    } catch (UnsupportedOperationException e2) {
                        Slog.e("MiuiBatteryServiceImpl", "Get NFC failed");
                    }
                    NfcAdapter nfcAdapter2 = this.mNfcAdapter;
                    if (nfcAdapter2 != null && nfcAdapter2.isEnabled()) {
                        if (this.mNfcAdapter.disable()) {
                            Settings.Global.putInt(this.mContentResolver, NFC_CLOED, 1);
                            this.mClosedNfcFromCharging = true;
                            this.mShowDisableNfc = true;
                        } else {
                            Slog.e("MiuiBatteryServiceImpl", "close NFC failed");
                        }
                    }
                    if (!this.mIsReverseWirelessCharge) {
                        if (this.mShowDisableNfc) {
                            Toast.makeText(MiuiBatteryServiceImpl.this.mContext, 286196163, 0).show();
                            this.mShowDisableNfc = false;
                            return;
                        }
                        return;
                    } else if (isSupportControlHaptic()) {
                        Toast.makeText(MiuiBatteryServiceImpl.this.mContext, 286196164, 0).show();
                        return;
                    } else if (this.mShowDisableNfc) {
                        Toast.makeText(MiuiBatteryServiceImpl.this.mContext, 286196165, 0).show();
                        this.mShowDisableNfc = false;
                        return;
                    } else {
                        return;
                    }
                case 10:
                    int bluetoothState = msg.arg1;
                    MiuiBatteryServiceImpl.this.mMiCharge.setBtTransferStartState(bluetoothState);
                    return;
                case 11:
                    Intent wirelessFwIntent = new Intent(ACTION_WIRELESS_FW_UPDATE);
                    wirelessFwIntent.putExtra(EXTRA_WIRELESS_FW_UPDATE, msg.arg1);
                    sendStickyBroadcast(wirelessFwIntent);
                    return;
                case 12:
                    Intent penReverseChgStateIntent = new Intent(ACTION_REVERSE_PEN_CHARGE_STATE);
                    penReverseChgStateIntent.putExtra(EXTRA_REVERSE_PEN_CHARGE_STATE, msg.arg1);
                    penReverseChgStateIntent.putExtra(EXTRA_REVERSE_PEN_SOC, getPSValue());
                    sendStickyBroadcast(penReverseChgStateIntent);
                    return;
                case 13:
                    showPowerOffWarningDialog();
                    sendMessageDelayed(14, 30000L);
                    return;
                case 14:
                    Intent intent = new Intent("com.android.internal.intent.action.REQUEST_SHUTDOWN");
                    intent.putExtra("android.intent.extra.KEY_CONFIRM", false);
                    intent.setFlags(268435456);
                    MiuiBatteryServiceImpl.this.mContext.startActivityAsUser(intent, UserHandle.CURRENT);
                    return;
                case 15:
                    try {
                        adjustVoltage();
                        return;
                    } catch (ParseException e3) {
                        e3.printStackTrace();
                        return;
                    }
                case 16:
                    Intent connectorTempIntent = new Intent(ACTION_TYPE_C_HIGH_TEMP);
                    connectorTempIntent.putExtra(EXTRA_TYPE_C_HIGH_TEMP, msg.arg1);
                    sendStickyBroadcast(connectorTempIntent);
                    return;
                default:
                    Slog.d("MiuiBatteryServiceImpl", "NO Message");
                    return;
            }
        }

        private int getChargingPowerMax() {
            String powerMax = MiuiBatteryServiceImpl.this.mMiCharge.getChargingPowerMax();
            if (powerMax != null && powerMax.length() != 0) {
                return parseInt(powerMax);
            }
            return -1;
        }

        private int getCarChargingType() {
            String carCharging = MiuiBatteryServiceImpl.this.mMiCharge.getCarChargingType();
            if (carCharging != null && carCharging.length() != 0) {
                return parseInt(carCharging);
            }
            return -1;
        }

        private int getPSValue() {
            String penSoc = MiuiBatteryServiceImpl.this.mMiCharge.getPSValue();
            if (penSoc != null && penSoc.length() != 0) {
                return parseInt(penSoc);
            }
            return -1;
        }

        public int getSBState() {
            String smartBatt = MiuiBatteryServiceImpl.this.mMiCharge.getSBState();
            if (smartBatt != null && smartBatt.length() != 0) {
                return parseInt(smartBatt);
            }
            return -1;
        }

        private void sendStickyBroadcast(Intent intent) {
            intent.addFlags(822083584);
            MiuiBatteryServiceImpl.this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
        }

        private void showWirelessCharingWarningDialog() {
            Intent dialogIntent = new Intent(ACTION_WIRELESS_CHG_WARNING_ACTIVITY);
            dialogIntent.addFlags(268435456);
            dialogIntent.putExtra("plugstatus", 4);
            MiuiBatteryServiceImpl.this.mContext.startActivity(dialogIntent);
            sendUpdateStatusBroadCast(1);
        }

        private void sendUpdateStatusBroadCast(int status) {
            Intent intent = new Intent(ACTION_WIRELESS_CHARGING);
            intent.addFlags(822083584);
            intent.putExtra(EXTRA_WIRELESS_CHARGING, status);
            MiuiBatteryServiceImpl.this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
        }

        private void updateWirelessReverseChargingNotification(int closedReason) {
            int messageRes = 0;
            Resources r = MiuiBatteryServiceImpl.this.mContext.getResources();
            CharSequence title = r.getText(286196684);
            NotificationManager notificationManager = (NotificationManager) MiuiBatteryServiceImpl.this.mContext.getSystemService("notification");
            if (notificationManager == null) {
                Slog.d("MiuiBatteryServiceImpl", "get notification service failed");
                return;
            }
            if (closedReason == 1) {
                messageRes = 286196681;
            } else if (closedReason == 4) {
                messageRes = 286196682;
            } else if (closedReason == 2) {
                messageRes = 286196683;
            }
            int i = this.mChargingNotificationId;
            if (i != 0) {
                notificationManager.cancelAsUser(null, i, UserHandle.ALL);
                Slog.d("MiuiBatteryServiceImpl", "Clear notification");
                this.mChargingNotificationId = 0;
            }
            if (messageRes != 0) {
                Notification.Builder builder = new Notification.Builder(MiuiBatteryServiceImpl.this.mContext, SystemNotificationChannels.USB).setSmallIcon(17303631).setWhen(0L).setOngoing(false).setTicker(title).setDefaults(0).setColor(MiuiBatteryServiceImpl.this.mContext.getColor(17170460)).setContentTitle(title).setVisibility(1);
                if (closedReason == 4) {
                    String messageString = r.getString(messageRes, NumberFormat.getPercentInstance().format(Settings.Global.getInt(this.mContentResolver, WIRELESS_REVERSE_CHARGING, 30) / 100.0f));
                    builder.setContentText(messageString);
                } else {
                    CharSequence message = r.getText(messageRes);
                    builder.setContentText(message);
                }
                Notification notification = builder.build();
                notificationManager.notifyAsUser(null, messageRes, notification, UserHandle.ALL);
                this.mChargingNotificationId = messageRes;
            }
            if (closedReason == 4) {
                MiuiBatteryServiceImpl.this.mMiCharge.setWirelessChargingEnabled(false);
            } else if (closedReason == 0) {
                return;
            }
            sendUpdateStatusBroadCast(1);
        }

        private void shouldCloseWirelessReverseCharging(int batteryLevel) {
            if (batteryLevel < Settings.Global.getInt(this.mContentResolver, WIRELESS_REVERSE_CHARGING, 30)) {
                updateWirelessReverseChargingNotification(4);
            }
        }

        private void showPowerOffWarningDialog() {
            AlertDialog powerOffDialog = new AlertDialog.Builder(ActivityThread.currentActivityThread().getSystemUiContext(), R.style.AlertDialog_Theme_DayNight).setCancelable(false).setTitle(286195822).setMessage(MiuiBatteryServiceImpl.this.mContext.getResources().getQuantityString(286064640, 30, 30)).setPositiveButton(286195821, new DialogInterface.OnClickListener() { // from class: com.android.server.MiuiBatteryServiceImpl.BatteryHandler.1
                @Override // android.content.DialogInterface.OnClickListener
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            }).create();
            powerOffDialog.getWindow().setType(2010);
            powerOffDialog.show();
        }

        private void adjustVoltage() throws ParseException {
            int smartBatt = getSBState();
            if (MiuiBatteryServiceImpl.this.DEBUG) {
                Slog.d("MiuiBatteryServiceImpl", "smartBatt = " + smartBatt);
            }
            if (isDateOfHighTemp() && isInTragetCountry() && smartBatt == 0) {
                MiuiBatteryServiceImpl.this.mIsSatisfyTimeRegionCondition = true;
                MiuiBatteryServiceImpl.this.mMiCharge.setSBState(10);
            } else if ((!isDateOfHighTemp() || !isInTragetCountry()) && smartBatt == 10) {
                MiuiBatteryServiceImpl.this.mIsSatisfyTimeRegionCondition = false;
                MiuiBatteryServiceImpl.this.mMiCharge.setSBState(0);
            }
        }

        private boolean isDateOfHighTemp() throws ParseException {
            this.mStartHithTempDate = parseDate("06-15");
            this.mEndHighTempDate = parseDate("09-15");
            Date currentDate = parseDate(getCurrentDate());
            if (MiuiBatteryServiceImpl.this.DEBUG) {
                Slog.d("MiuiBatteryServiceImpl", "currentDate = " + currentDate);
            }
            if (currentDate.getTime() >= this.mStartHithTempDate.getTime() && currentDate.getTime() <= this.mEndHighTempDate.getTime()) {
                return true;
            }
            return false;
        }

        private boolean isInChina() {
            TelephonyManager tel = (TelephonyManager) MiuiBatteryServiceImpl.this.mContext.getSystemService("phone");
            String networkOperator = tel.getNetworkOperator();
            if (!TextUtils.isEmpty(networkOperator)) {
                if (MiuiBatteryServiceImpl.this.DEBUG) {
                    Slog.d("MiuiBatteryServiceImpl", "networkOperator = " + networkOperator);
                }
                return networkOperator.startsWith("460");
            }
            return false;
        }

        private boolean isInTragetCountry() {
            if (isInChina()) {
                return true;
            }
            if (!SystemProperties.get("ro.product.mod_device", "").startsWith("taoyao") && !SystemProperties.getBoolean("persist.vendor.domain.charge", false)) {
                return false;
            }
            String value = SystemProperties.get("ro.miui.region", "");
            return Arrays.asList(MiuiBatteryServiceImpl.this.SUPPORT_COUNTRY).contains(value);
        }
    }
}
