package com.android.server;

import android.app.AppOpsManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.util.Log;
import com.miui.base.MiuiStubRegistry;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
/* loaded from: classes.dex */
public class BluetoothManagerServiceImpl implements BluetoothManagerServiceStub {
    static final String ANDROID_PACKAGE = "android";
    static final int BD_ADDR_NO_COLON_LEN = 12;
    static final int BD_ADDR_WITH_COLON_LEN = 17;
    private static final int CHECK_PERMISSION_MS = 100;
    private static final boolean DBG = true;
    static final String ENABLE_BT_FLAG = "Settings.Global.ENABLE_BT_FLAG";
    static final String ENABLE_BT_RECORD = "Settings.Global.ENABLE_BLUETOOTH_RECORD";
    static final String MASK_BD_ADDR_NO_COLON_PREFIX = "000000";
    static final String MASK_BD_ADDR_WITH_COLON_DEFAULT = "00:00:00:00:00:00";
    static final String MASK_BD_ADDR_WITH_COLON_PREFIX = "00:00:00:";
    static final String NFC_PACKAGE = "com.android.nfc";
    static final int NOTIFICATION_ID_ = -100100;
    static final String SETTINGS_PACKAGE = "com.android.settings";
    static final String SYSTEM_UI_PACKAGE = "com.android.systemui";
    private static final String TAG = "BluetoothManagerServiceImpl";
    private static long mOpenCheckTime;
    private static long mOpenStartTime;
    ArrayList<String> mSystemPackage = new ArrayList<>();
    private String mRecordEnableBt = "0#0#0,";
    private HashMap<String, String> mPackageTime = new HashMap<>();
    private LinkedList<String> mPackageList = new LinkedList<>();
    private HashMap<String, String> mPackageAction = new HashMap<>();

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<BluetoothManagerServiceImpl> {

        /* compiled from: BluetoothManagerServiceImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final BluetoothManagerServiceImpl INSTANCE = new BluetoothManagerServiceImpl();
        }

        public BluetoothManagerServiceImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public BluetoothManagerServiceImpl provideNewInstance() {
            return new BluetoothManagerServiceImpl();
        }
    }

    public BluetoothManagerServiceImpl() {
        this.mSystemPackage.add("com.android.systemui");
        this.mSystemPackage.add(ANDROID_PACKAGE);
        this.mSystemPackage.add(SETTINGS_PACKAGE);
        this.mSystemPackage.add(NFC_PACKAGE);
    }

    public String getMaskDeviceAddress(String address) {
        if (address != null) {
            if (address.length() >= 17) {
                return MASK_BD_ADDR_WITH_COLON_PREFIX + address.substring(MASK_BD_ADDR_WITH_COLON_PREFIX.length());
            }
            if (address.length() >= 12) {
                return MASK_BD_ADDR_NO_COLON_PREFIX + address.substring(MASK_BD_ADDR_NO_COLON_PREFIX.length());
            }
            return MASK_BD_ADDR_WITH_COLON_DEFAULT;
        }
        return MASK_BD_ADDR_WITH_COLON_DEFAULT;
    }

    public void enableStartTime() {
        mOpenStartTime = System.currentTimeMillis();
    }

    public void createNotification(Context context, String packageName, boolean isEnable) {
        String title;
        String content;
        try {
            String appName = getAppNameByPackageName(context, packageName);
            Log.d(TAG, "packageName = " + packageName + " appName = " + appName);
            Settings.Global.putString(context.getContentResolver(), ENABLE_BT_FLAG, "true");
            if (this.mSystemPackage.contains(packageName)) {
                Log.d(TAG, "package is system package,do not recode");
                return;
            }
            String time = getTime();
            Log.d(TAG, "now time = " + time);
            NotificationManager manager = (NotificationManager) context.getSystemService("notification");
            if (isEnable) {
                title = context.getString(286195839);
                content = context.getString(286195838, appName);
                initRecordInfo(context, packageName, "open");
                Log.d(TAG, "check enable bt time");
                long currentTimeMillis = System.currentTimeMillis();
                mOpenCheckTime = currentTimeMillis;
                if (currentTimeMillis - mOpenStartTime > 100) {
                    Log.d(TAG, "do not notification ");
                    return;
                }
            } else {
                title = context.getString(286195837);
                content = context.getString(286195836, appName);
                initRecordInfo(context, packageName, "close");
            }
            NotificationChannel notificationChannel = new NotificationChannel("BluetoothManagerServiceInjector", appName, 4);
            notificationChannel.setDescription(appName);
            manager.createNotificationChannel(notificationChannel);
            new Notification.Builder(context, "BluetoothManagerServiceInjector").setContentTitle(title).setContentText(content).setWhen(System.currentTimeMillis()).setSmallIcon(17301632).setTimeoutAfter(5000L).build();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initRecordInfo(Context context, String packageName, String action) {
        Log.d(TAG, "initRecordInfo: ");
        try {
            this.mPackageList.clear();
            this.mPackageTime.clear();
            this.mRecordEnableBt = Settings.Global.getString(context.getContentResolver(), ENABLE_BT_RECORD);
            Log.d(TAG, "mRecordEnableBt: " + this.mRecordEnableBt);
            String str = this.mRecordEnableBt;
            if (str == null) {
                String currentTime = getTime();
                Log.d(TAG, "currentTime = " + currentTime);
                this.mRecordEnableBt = packageName + "#" + currentTime + "#" + action + ",";
                this.mPackageTime.put(packageName, currentTime);
                this.mPackageAction.put(packageName, action);
                this.mPackageList.add(packageName);
                Settings.Global.putString(context.getContentResolver(), ENABLE_BT_RECORD, this.mRecordEnableBt);
                Log.d(TAG, "mRecordEnableBt = " + this.mRecordEnableBt);
                return;
            }
            String[] strSplit = str.split(",");
            for (String str2 : strSplit) {
                String[] tempSplit = str2.split("#");
                if (tempSplit.length == 3) {
                    Log.d(TAG, "initRecordInfo: " + tempSplit[0] + ", " + tempSplit[1] + ", " + tempSplit[2]);
                    this.mPackageTime.put(tempSplit[0], tempSplit[1]);
                    this.mPackageAction.put(tempSplit[0], tempSplit[2]);
                    this.mPackageList.add(tempSplit[0]);
                } else {
                    Log.e(TAG, "initRecordInfo Shouldn't be here!");
                }
            }
            String currentTime2 = getTime();
            if (this.mPackageList.contains(packageName)) {
                Log.d(TAG, "contain packagename = " + packageName);
                this.mPackageList.remove(packageName);
            }
            this.mPackageTime.put(packageName, currentTime2);
            this.mPackageAction.put(packageName, action);
            this.mPackageList.addFirst(packageName);
            writeToSettingsGlobal(context);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void writeToSettingsGlobal(Context context) {
        Log.d(TAG, "writeToSettingsGlobal");
        try {
            this.mRecordEnableBt = "";
            for (int i = 0; i < 10 && i < this.mPackageList.size(); i++) {
                String pkgName = this.mPackageList.get(i);
                String timeStamp = this.mPackageTime.get(pkgName);
                String action = this.mPackageAction.get(pkgName);
                if (this.mPackageTime != null) {
                    this.mRecordEnableBt += pkgName + "#" + timeStamp + "#" + action + ",";
                }
            }
            Log.d(TAG, " final mRecordEnableBt = " + this.mRecordEnableBt);
            Settings.Global.putString(context.getContentResolver(), ENABLE_BT_RECORD, this.mRecordEnableBt);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getAppNameByPackageName(Context context, String packageName) {
        try {
            PackageManager pm = context.getPackageManager();
            String appName = pm.getApplicationLabel(pm.getApplicationInfo(packageName, 128)).toString();
            return appName;
        } catch (Exception e) {
            e.printStackTrace();
            return packageName;
        }
    }

    public static String getAppName(Context context) {
        try {
            PackageManager packageManager = context.getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
            int labelRes = packageInfo.applicationInfo.labelRes;
            return context.getResources().getString(labelRes);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String getTime() {
        Date date = new Date();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String time = df.format(date);
        return time;
    }

    public int checkBluetoothPermission(AppOpsManager appOps, int callingUid, String packageName) {
        if (appOps != null) {
            return appOps.checkOpNoThrow(10002, callingUid, packageName);
        }
        return 0;
    }
}
