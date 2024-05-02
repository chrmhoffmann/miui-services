package com.android.server.location;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Environment;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import com.android.server.location.gnss.GnssLocationProviderStub;
import com.miui.base.MiuiStubRegistry;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
/* loaded from: classes.dex */
public class GnssLocationProviderImpl implements GnssLocationProviderStub {
    private static final String ACTION_MODEM_LOCATION = "NFW GPS LOCATION FROM Modem";
    private static final String CALLER_PACKAGE_NAME_ACTION = "com.xiaomi.bsp.gps.nps.callerName";
    private static final String CLASS_PACKAGE_CONNECTIVITY_NPI = "com.xiaomi.bsp.gps.nps";
    private static final String EXTRA_NPS_NEW_EVENT = "com.xiaomi.bsp.gps.nps.NewEvent";
    private static final String EXTRA_NPS_PACKAGE_NAME = "com.xiaomi.bsp.gps.nps.pkgNname";
    private static final String GET_EVENT_ACTION = "com.xiaomi.bsp.gps.nps.GetEvent";
    private static final String MIUI_NFW_PROXY_APP = "com.lbe.security.miui";
    private static final String RECEIVER_GNSS_CALLER_NAME_EVENT = "com.xiaomi.bsp.gps.nps.GnssCallerNameEventReceiver";
    private static final String RECEIVER_GNSS_EVENT = "com.xiaomi.bsp.gps.nps.GnssEventReceiver";
    private static final String STR_LOCATIONFILE = "locationinformation.txt";
    private static final String XM_HP_LOCATION = "xiaomi_high_precise_location";
    private static final int XM_HP_LOCATION_OFF = 2;
    private static final File mLocationFile;
    private static final Object mLocationInformationLock;
    private Context mContext;
    private static final String TAG = "GnssLocationProviderImpl";
    private static final boolean DEBUG = Log.isLoggable(TAG, 3);
    private boolean mEnableSendingState = false;
    private boolean mEdgnssUiSwitch = false;
    private boolean mNoise = false;
    private GnssEventHandler mGnssEventHandler = null;
    private NewGnssEventHandler newGnssEventHandler = null;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<GnssLocationProviderImpl> {

        /* compiled from: GnssLocationProviderImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final GnssLocationProviderImpl INSTANCE = new GnssLocationProviderImpl();
        }

        public GnssLocationProviderImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public GnssLocationProviderImpl provideNewInstance() {
            return new GnssLocationProviderImpl();
        }
    }

    static {
        Object obj = new Object();
        mLocationInformationLock = obj;
        synchronized (obj) {
            File file = new File(getSystemDir(), STR_LOCATIONFILE);
            file.delete();
            try {
                file.createNewFile();
            } catch (IOException e) {
                loge("IO error occurred!");
            }
            mLocationFile = new File(getSystemDir(), STR_LOCATIONFILE);
        }
    }

    public void init(Context context) {
        this.mContext = context;
    }

    public boolean getSendingSwitch() {
        return this.mEnableSendingState;
    }

    public void setSendingSwitch(boolean newValue) {
        this.mEnableSendingState = newValue;
    }

    public String getConfig(Properties properties, String config, String defaultConfig) {
        return properties.getProperty(config, defaultConfig);
    }

    public void notifyState(int event) {
        logd("gps now on " + event);
        deliverIntent(this.mContext, event);
    }

    public void notifyStateWithPackageName(String packageName, int event) {
        logd("notify state, event:" + event + ", " + packageName);
        deliverIntentWithPackageName(this.mContext, packageName, event);
    }

    public void notifyCallerName(String pkgName) {
        logd("caller name: " + pkgName);
        Context context = this.mContext;
        if (context != null) {
            deliverCallerNameIntent(context, pkgName);
            this.mEdgnssUiSwitch = isEdgnssSwitchOn(this.mContext);
            logd("Edgnss Switch now is " + this.mEdgnssUiSwitch);
        }
    }

    private boolean isChineseLanguage() {
        String language = Locale.getDefault().toString();
        return language.endsWith("zh_CN");
    }

    private void packageOnReceive(String packageName) {
        List<String> mIgnoreNotifyPackage = new ArrayList<>(Arrays.asList("android", "com.xiaomi.location.fused"));
        if (this.mGnssEventHandler == null) {
            this.mGnssEventHandler = GnssEventHandler.getInstance(this.mContext);
        }
        Log.d(TAG, "receive caller pkg name =" + packageName);
        if (TextUtils.isEmpty(packageName) || mIgnoreNotifyPackage.contains(packageName)) {
            return;
        }
        this.mGnssEventHandler.handleCallerName(packageName);
    }

    private void packageEventOnReceive(int event, String packageName) {
        if (this.mGnssEventHandler == null) {
            this.mGnssEventHandler = GnssEventHandler.getInstance(this.mContext);
        }
        if (this.newGnssEventHandler == null) {
            this.newGnssEventHandler = NewGnssEventHandler.getInstance(this.mContext);
        }
        if (event == 0) {
            return;
        }
        Log.d(TAG, "receive event " + event + (packageName == null ? "" : "," + packageName));
        if (packageName != null && !packageName.trim().equals("")) {
            switch (event) {
                case 3:
                case 5:
                    this.newGnssEventHandler.handlerUpdateFixStatus(true);
                    return;
                case 4:
                    this.newGnssEventHandler.handlerUpdateFixStatus(false);
                    return;
                case 6:
                    this.newGnssEventHandler.handleStart(packageName);
                    return;
                case 7:
                    this.newGnssEventHandler.handleStop(packageName);
                    return;
                case 8:
                    this.newGnssEventHandler.handleUpdateGnssStatus();
                    return;
                default:
                    return;
            }
        }
        switch (event) {
            case 1:
            case 3:
            case 4:
            case 5:
            default:
                return;
            case 2:
                this.mGnssEventHandler.handleStop();
                return;
        }
    }

    private void deliverIntent(Context context, int event) {
        if (context == null || !isChineseLanguage()) {
            return;
        }
        packageEventOnReceive(event, null);
    }

    private void deliverIntentWithPackageName(Context context, String packageName, int event) {
        if (context == null || !isChineseLanguage()) {
            return;
        }
        packageEventOnReceive(event, packageName);
    }

    private void deliverCallerNameIntent(Context context, String packageName) {
        if (context == null || !isChineseLanguage()) {
            return;
        }
        packageOnReceive(packageName);
    }

    public void writeLocationInformation(String event) {
        logd("writeLocationInformation:" + event);
        synchronized (mLocationInformationLock) {
            String info = getCurrentTime() + ": " + event + "\n";
            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(getLocationFilePath(), true));
                try {
                    writer.write(info);
                    writer.close();
                } catch (Throwable th) {
                    try {
                        writer.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                    throw th;
                }
            } catch (IOException e) {
                loge("IO exception");
            }
        }
    }

    public StringBuilder loadLocationInformation() {
        StringBuilder stirngBuidler;
        String readLine;
        logd("loadLocationInformation");
        synchronized (mLocationInformationLock) {
            try {
                BufferedReader reader = new BufferedReader(new FileReader(mLocationFile));
                try {
                    stirngBuidler = new StringBuilder();
                    String line = "LocationInformation:";
                    do {
                        stirngBuidler.append(line).append("\n");
                        readLine = reader.readLine();
                        line = readLine;
                    } while (readLine != null);
                    reader.close();
                } catch (Throwable th) {
                    try {
                        reader.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                    throw th;
                }
            } catch (IOException e) {
                loge("IO exception");
                return new StringBuilder();
            }
        }
        return stirngBuidler;
    }

    private String getLocationFilePath() {
        String absolutePath;
        synchronized (mLocationInformationLock) {
            absolutePath = mLocationFile.getAbsolutePath();
        }
        return absolutePath;
    }

    private String getCurrentTime() {
        long mNow = System.currentTimeMillis();
        StringBuilder sb = new StringBuilder();
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(mNow);
        sb.append(String.format("%tm-%td %tH:%tM:%tS.%tL", c, c, c, c, c, c));
        return sb.toString();
    }

    private static File getSystemDir() {
        return new File(Environment.getDataDirectory(), "system");
    }

    public void setNfwProxyAppConfig(Properties properties, String config) {
        String country = SystemProperties.get("ro.boot.hwc");
        if (!"CN".equals(country)) {
            properties.setProperty(config, MIUI_NFW_PROXY_APP);
            Log.d(TAG, "NFW app is com.lbe.security.miui");
        }
    }

    public boolean hasLocationPermission(PackageManager packageManager, String pkgName, Context context) {
        int flags = packageManager.getPermissionFlags("com.miui.securitycenter.permission.modem_location", pkgName, context.getUser());
        loge("flags in nfw app is " + flags);
        return packageManager.checkPermission("com.miui.securitycenter.permission.modem_location", pkgName) == 0 || (flags & 2) == 0;
    }

    public void sendNfwbroadcast(Context context) {
        Intent intent = new Intent(ACTION_MODEM_LOCATION);
        context.sendBroadcastAsUser(intent, UserHandle.ALL);
    }

    private boolean isEdgnssSwitchOn(Context context) {
        return (context == null || Settings.Secure.getInt(context.getContentResolver(), XM_HP_LOCATION, 2) == 2) ? false : true;
    }

    public void ifNoiseEnvironment(String nmea) {
        if (!nmea.contains("PQWM1")) {
            return;
        }
        String[] nmeaSplit = nmea.split(",");
        if (nmeaSplit[0].indexOf("PQWM1") == -1 || nmeaSplit.length < 3 || TextUtils.isEmpty(nmeaSplit[9])) {
            return;
        }
        int value = Integer.parseInt(nmeaSplit[9]);
        if ((value != 18 && value != -12) || this.mNoise) {
            if (this.mNoise && value != 18 && value != -12) {
                this.mNoise = false;
                notifyCallerName("normal_environment");
                return;
            }
            return;
        }
        notifyCallerName("noise_environment");
        this.mNoise = true;
    }

    public String precisionProcessByType(String nmea) {
        ifNoiseEnvironment(nmea);
        if (!this.mEdgnssUiSwitch) {
            return nmea;
        }
        String[] nmeaSplit = nmea.split(",");
        if (nmeaSplit[0].indexOf("GGA") != -1 && !TextUtils.isEmpty(nmeaSplit[2])) {
            nmeaSplit[2] = precisionProcess(nmeaSplit[2]);
            nmeaSplit[4] = precisionProcess(nmeaSplit[4]);
        }
        if (nmeaSplit[0].indexOf("RMC") != -1 && !TextUtils.isEmpty(nmeaSplit[3])) {
            nmeaSplit[3] = precisionProcess(nmeaSplit[3]);
            nmeaSplit[5] = precisionProcess(nmeaSplit[5]);
        }
        if (nmeaSplit[0].indexOf("GLL") != -1 && !TextUtils.isEmpty(nmeaSplit[1])) {
            nmeaSplit[1] = precisionProcess(nmeaSplit[1]);
            nmeaSplit[3] = precisionProcess(nmeaSplit[3]);
        }
        String nmeaEnd = String.join(",", nmeaSplit);
        return nmeaEnd;
    }

    private String precisionProcess(String data) {
        int leng = data.lastIndexOf(46);
        if (leng == -1) {
            return data;
        }
        String dataNeedProcessed = data.substring(leng - 2);
        StringBuilder sf = new StringBuilder(2);
        String dataInvariant = data.substring(0, leng - 2);
        sf.append(dataInvariant);
        if (dataNeedProcessed.substring(0, 1).equals("0")) {
            sf.append("0");
        }
        BigDecimal bigDecimal = BigDecimal.valueOf(Double.valueOf(dataNeedProcessed).doubleValue() / 60.0d).setScale(5, 1);
        double dataProcessed = bigDecimal.multiply(new BigDecimal(60)).doubleValue();
        sf.append(dataProcessed);
        return sf.toString();
    }

    public Location getLowerAccLocation(Location location) {
        if (!this.mEdgnssUiSwitch) {
            return location;
        }
        location.setLatitude(dataFormat(location.getLatitude()));
        location.setLongitude(dataFormat(location.getLongitude()));
        return location;
    }

    private double dataFormat(double data) {
        try {
            DecimalFormat df = new DecimalFormat("0.00000");
            String doubleNumAsString = df.format(data);
            return Double.valueOf(doubleNumAsString.replace(",", ".")).doubleValue();
        } catch (Exception e) {
            loge("Exception here, print locale: " + Locale.getDefault());
            return data;
        }
    }

    private static void loge(String string) {
        Log.e(TAG, string);
    }

    private void logd(String string) {
        if (DEBUG) {
            Log.d(TAG, string);
        }
    }
}
