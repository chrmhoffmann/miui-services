package com.android.server.location;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.MiuiSettings;
import android.util.Log;
import com.android.server.MiuiBatteryStatsService;
import com.android.server.am.BroadcastQueueImpl;
import com.android.server.location.GnssCollectData;
import com.android.server.location.gnss.GnssCollectDataStub;
import com.android.server.location.gnss.hal.GnssPowerOptimizeStub;
import com.miui.base.MiuiStubRegistry;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import miui.mqsas.sdk.MQSEventManagerDelegate;
import miui.os.Build;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
/* loaded from: classes.dex */
public class GnssCollectDataImpl implements GnssCollectDataStub {
    private static final String ACTION_COLLECT_DATA = "action collect data";
    private static final String CLOUDGPOKEY = "GpoVersion";
    private static final String CLOUDKEY = "enabled";
    private static final String CLOUDKEYSUPL = "enableCaict";
    private static final String CLOUDMODULE = "bigdata";
    private static final String CLOUD_MODULE_RTK = "gnssRtk";
    private static final String CN_MCC = "460";
    private static final String COLLECT_DATA_PATH = "/data/mqsas/gps/gps-strength";
    private static final boolean DEBUG = false;
    private static final String GNSS_MQS_SWITCH = "persist.sys.mqs.gps";
    public static final int INFO_B1CN0_TOP4 = 9;
    public static final int INFO_L1CN0_TOP4 = 7;
    public static final int INFO_L5CN0_TOP4 = 8;
    public static final int INFO_NMEA_PQWP6 = 6;
    private static final String IS_COLLECT = "1";
    private static final String RTK_SWITCH = "persist.sys.mqs.gps.rtk";
    public static final int STATE_FIX = 2;
    public static final int STATE_INIT = 0;
    public static final int STATE_LOSE = 4;
    public static final int STATE_SAVE = 5;
    public static final int STATE_START = 1;
    public static final int STATE_STOP = 3;
    public static final int STATE_UNKNOWN = 100;
    private static final String SUPL_SWITCH = "persist.sys.mqs.gps.supl";
    private static final String TAG = "GnssCD";
    private boolean hasStartUploadData;
    private Context mContext;
    private GnssCollectDataDbDao mGnssCollectDataDbDao;
    private Handler mHandler;
    private HandlerThread mHandlerThread;
    public static int mCurrentState = 100;
    private static String mMqsGpsModuleId = "mqs_gps_data_63921000";
    private static final boolean IS_STABLE_VERSION = Build.IS_STABLE_VERSION;
    private MQSEventManagerDelegate mMqsEventManagerDelegate = MQSEventManagerDelegate.getInstance();
    private GnssSessionInfo mSessionInfo = new GnssSessionInfo();
    private int UPLOAD_REPEAT_TIME = 86400000;
    private JSONArray mJsonArray = new JSONArray();
    private boolean mIsCnSim = false;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() { // from class: com.android.server.location.GnssCollectDataImpl.3
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            if (GnssCollectDataImpl.ACTION_COLLECT_DATA.equals(intent.getAction())) {
                GnssCollectDataImpl.this.startUploadGnssData(context);
            }
        }
    };

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<GnssCollectDataImpl> {

        /* compiled from: GnssCollectDataImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final GnssCollectDataImpl INSTANCE = new GnssCollectDataImpl();
        }

        public GnssCollectDataImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public GnssCollectDataImpl provideNewInstance() {
            return new GnssCollectDataImpl();
        }
    }

    private boolean allowCollect() {
        return "1".equals(SystemProperties.get(GNSS_MQS_SWITCH, "1"));
    }

    public boolean getSuplState() {
        String suplstate = SystemProperties.get(SUPL_SWITCH, "0");
        return "1".equals(suplstate);
    }

    public void setCnSimInserted(String mccMnc) {
        this.mIsCnSim = mccMnc.startsWith(CN_MCC);
    }

    public boolean isCnSimInserted() {
        return this.mIsCnSim;
    }

    private String packToJsonArray() {
        JSONException e;
        JSONObject jsonObj = new JSONObject();
        if (this.mSessionInfo.getTtff() < 0) {
            Log.d(TAG, "abnormal data");
            return null;
        }
        long startTime = this.mSessionInfo.getStartTimeInHour();
        long TTFF = this.mSessionInfo.getTtff();
        long runTime = this.mSessionInfo.getRunTime();
        int loseTimes = this.mSessionInfo.getLoseTimes();
        int SAPNumber = this.mSessionInfo.getSapTimes();
        int PDRNumber = this.mSessionInfo.getPdrTimes();
        int totalNumber = this.mSessionInfo.getAllFixTimes();
        double L1Top4MeanCn0 = this.mSessionInfo.getL1Top4Cn0Mean();
        double L5Top4MeanCn0 = this.mSessionInfo.getL5Top4Cn0Mean();
        double B1Top4MeanCn0 = this.mSessionInfo.getB1Top4Cn0Mean();
        try {
            GnssCollectDataDbDao gnssCollectDataDbDao = GnssCollectDataDbDao.getInstance(this.mContext);
            this.mGnssCollectDataDbDao = gnssCollectDataDbDao;
            try {
                gnssCollectDataDbDao.insertGnssCollectData(startTime, TTFF, runTime, loseTimes, SAPNumber, PDRNumber, totalNumber, L1Top4MeanCn0, L5Top4MeanCn0, B1Top4MeanCn0);
                try {
                    jsonObj.put(GnssCollectData.CollectDbEntry.COLUMN_NAME_STARTTIME, startTime);
                    try {
                        jsonObj.put(GnssCollectData.CollectDbEntry.COLUMN_NAME_TTFF, TTFF);
                        try {
                            jsonObj.put(GnssCollectData.CollectDbEntry.COLUMN_NAME_RUNTIME, runTime);
                            try {
                                jsonObj.put(GnssCollectData.CollectDbEntry.COLUMN_NAME_LOSETIMES, loseTimes);
                                try {
                                    jsonObj.put(GnssCollectData.CollectDbEntry.COLUMN_NAME_SAPNUMBER, SAPNumber);
                                    try {
                                        jsonObj.put(GnssCollectData.CollectDbEntry.COLUMN_NAME_PDRNUMBER, PDRNumber);
                                        try {
                                            jsonObj.put(GnssCollectData.CollectDbEntry.COLUMN_NAME_TOTALNUMBER, totalNumber);
                                            try {
                                                jsonObj.put(GnssCollectData.CollectDbEntry.COLUMN_NAME_L1TOP4MEANCN0, L1Top4MeanCn0);
                                            } catch (JSONException e2) {
                                                e = e2;
                                            }
                                        } catch (JSONException e3) {
                                            e = e3;
                                        }
                                    } catch (JSONException e4) {
                                        e = e4;
                                    }
                                } catch (JSONException e5) {
                                    e = e5;
                                }
                            } catch (JSONException e6) {
                                e = e6;
                            }
                        } catch (JSONException e7) {
                            e = e7;
                        }
                        try {
                            jsonObj.put(GnssCollectData.CollectDbEntry.COLUMN_NAME_L5TOP4MEANCN0, L5Top4MeanCn0);
                            jsonObj.put(GnssCollectData.CollectDbEntry.COLUMN_NAME_B1TOP4MEANCN0, B1Top4MeanCn0);
                            this.mJsonArray.put(jsonObj);
                            String jsonString = this.mJsonArray.toString();
                            return jsonString;
                        } catch (JSONException e8) {
                            e = e8;
                            Log.e(TAG, "JSON exception " + e);
                            return null;
                        }
                    } catch (JSONException e9) {
                        e = e9;
                    }
                } catch (JSONException e10) {
                    e = e10;
                }
            } catch (JSONException e11) {
                e = e11;
            }
        } catch (JSONException e12) {
            e = e12;
        }
    }

    private void saveToFile(String messageToFile) {
        FileOutputStream out = null;
        try {
            try {
                try {
                    File bigdataFile = new File(COLLECT_DATA_PATH);
                    if (bigdataFile.exists()) {
                        long fileSize = bigdataFile.length() / 1024;
                        if (fileSize > 5) {
                            bigdataFile.delete();
                            bigdataFile.getParentFile().mkdirs();
                            bigdataFile.createNewFile();
                        }
                    } else {
                        bigdataFile.getParentFile().mkdirs();
                        bigdataFile.createNewFile();
                    }
                    out = new FileOutputStream(bigdataFile, true);
                    out.write(messageToFile.getBytes());
                    out.close();
                } catch (Exception e) {
                    e.printStackTrace();
                    if (out != null) {
                        out.close();
                    }
                }
            } catch (IOException e2) {
                e2.printStackTrace();
            }
        } catch (Throwable th) {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e3) {
                    e3.printStackTrace();
                }
            }
            throw th;
        }
    }

    public void saveLog() {
        String output = packToJsonArray();
        if (!this.mSessionInfo.checkValidity()) {
            return;
        }
        if (this.mContext == null || output == null) {
            Log.d(TAG, "mContext == null || output == null");
            return;
        }
        if (this.mJsonArray.length() > 50) {
            startUploadGnssData(this.mContext);
            this.mGnssCollectDataDbDao.deleteAllGnssCollectData();
            this.mJsonArray = new JSONArray();
        }
        if (!this.hasStartUploadData) {
            this.hasStartUploadData = true;
            setAlarm(this.mContext, ACTION_COLLECT_DATA);
        }
    }

    public void startUploadGnssData(Context context) {
        if (this.mJsonArray.length() > 0) {
            try {
                Intent intent = new Intent(MiuiBatteryStatsService.TrackBatteryUsbInfo.ACTION_TRACK_EVENT);
                intent.setPackage(MiuiBatteryStatsService.TrackBatteryUsbInfo.ANALYTICS_PACKAGE);
                if (Build.IS_INTERNATIONAL_BUILD) {
                    intent.setFlags(1);
                } else {
                    intent.setFlags(3);
                }
                intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_APP_ID, "2882303761518758754");
                intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_EVENT_NAME, GnssCollectData.CollectDbEntry.TABLE_NAME);
                intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_PACKAGE, MiuiBatteryStatsService.TrackBatteryUsbInfo.ANALYTICS_PACKAGE);
                Bundle params = new Bundle();
                params.putStringArrayList("STtest", this.mGnssCollectDataDbDao.filterStartTime());
                params.putStringArrayList("TTFFtest", this.mGnssCollectDataDbDao.filterTTFF());
                params.putStringArrayList("RTtest", this.mGnssCollectDataDbDao.filterRunTime());
                params.putStringArrayList("LTtest", this.mGnssCollectDataDbDao.filterLoseTimes());
                params.putStringArrayList("SPTtest", this.mGnssCollectDataDbDao.sumSPT());
                params.putStringArrayList("L1test", this.mGnssCollectDataDbDao.filterL1Top4MeanCn0());
                params.putStringArrayList("L5test", this.mGnssCollectDataDbDao.filterL5Top4MeanCn0());
                params.putStringArrayList("B1test", this.mGnssCollectDataDbDao.filterB1Top4MeanCn0());
                intent.putExtras(params);
                context.startService(intent);
                saveToFile(params.toString());
                Log.d(TAG, "send to oneTrack & file");
            } catch (Exception e) {
                Log.e(TAG, "unexpected error when send GNSS event to onetrack");
                e.printStackTrace();
            }
            this.mGnssCollectDataDbDao.deleteAllGnssCollectData();
            this.mJsonArray = new JSONArray();
        }
    }

    public String getCurrentTime() {
        long mNow = System.currentTimeMillis();
        StringBuilder sb = new StringBuilder();
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(mNow);
        sb.append(String.format("%tm-%td %tH:%tM:%tS.%tL", c, c, c, c, c, c));
        return sb.toString();
    }

    private void sendMessage(int message, Object obj) {
        Handler handler = this.mHandler;
        if (handler == null) {
            Log.e(TAG, "mhandler is null  ");
            return;
        }
        Message lMessage = Message.obtain(handler, message, obj);
        this.mHandler.sendMessage(lMessage);
    }

    private boolean isL5Sv(float carrierFreq) {
        return ((double) carrierFreq) >= 1.164E9d && ((double) carrierFreq) <= 1.189E9d;
    }

    private boolean isB1Sv(float carrierFreq) {
        return ((double) carrierFreq) >= 1.559E9d && ((double) carrierFreq) <= 1.564E9d;
    }

    public void savePoint(int type, float[] cn0s, int numSv, float[] svCarrierFreQs) {
        if (!allowCollect()) {
            Log.d(TAG, "no GnssCD enabled");
        } else if (10 == type) {
            saveL5Cn0(numSv, cn0s, svCarrierFreQs);
            saveB1Cn0(numSv, cn0s, svCarrierFreQs);
            if (numSv == 0 || cn0s == null || cn0s.length == 0 || cn0s.length < numSv || numSv < 4) {
                return;
            }
            float[] cn0Array = Arrays.copyOf(cn0s, numSv);
            Arrays.sort(cn0Array);
            if (cn0Array[numSv - 4] > 0.0d) {
                double top4AvgCn0 = 0.0d;
                for (int i = numSv - 4; i < numSv; i++) {
                    top4AvgCn0 += cn0Array[i];
                }
                sendMessage(7, Double.valueOf(top4AvgCn0 / 4.0d));
            }
        }
    }

    private void saveL5Cn0(int svCount, float[] cn0s, float[] svCarrierFreqs) {
        if (svCount == 0 || cn0s == null || cn0s.length == 0 || cn0s.length < svCount || svCarrierFreqs == null || svCarrierFreqs.length == 0 || svCarrierFreqs.length < svCount) {
            return;
        }
        ArrayList<Float> CnoL5Array = new ArrayList<>();
        for (int i = 0; i < svCount; i++) {
            if (isL5Sv(svCarrierFreqs[i])) {
                CnoL5Array.add(Float.valueOf(cn0s[i]));
            }
        }
        int i2 = CnoL5Array.size();
        if (i2 == 0 || CnoL5Array.size() < 4) {
            return;
        }
        int numSvL5 = CnoL5Array.size();
        Collections.sort(CnoL5Array);
        if (CnoL5Array.get(numSvL5 - 4).floatValue() > 0.0d) {
            double top4AvgCn0 = 0.0d;
            for (int i3 = numSvL5 - 4; i3 < numSvL5; i3++) {
                top4AvgCn0 += CnoL5Array.get(i3).floatValue();
            }
            sendMessage(8, Double.valueOf(top4AvgCn0 / 4.0d));
        }
    }

    private void saveB1Cn0(int svCount, float[] cn0s, float[] svCarrierFreQs) {
        if (svCount == 0 || cn0s == null || cn0s.length == 0 || cn0s.length < svCount || svCarrierFreQs == null || svCarrierFreQs.length == 0 || svCarrierFreQs.length < svCount) {
            return;
        }
        ArrayList<Float> CnoB1Array = new ArrayList<>();
        for (int i = 0; i < svCount; i++) {
            if (isB1Sv(svCarrierFreQs[i])) {
                CnoB1Array.add(Float.valueOf(cn0s[i]));
            }
        }
        int i2 = CnoB1Array.size();
        if (i2 == 0 || CnoB1Array.size() < 4) {
            return;
        }
        int numSvB1 = CnoB1Array.size();
        Collections.sort(CnoB1Array);
        if (CnoB1Array.get(numSvB1 - 4).floatValue() > 0.0d) {
            double top4AvgCn0 = 0.0d;
            for (int i3 = numSvB1 - 4; i3 < numSvB1; i3++) {
                top4AvgCn0 += CnoB1Array.get(i3).floatValue();
            }
            sendMessage(9, Double.valueOf(top4AvgCn0 / 4.0d));
        }
    }

    public void savePoint(int type, String extraInfo, Context context) {
        if (!allowCollect()) {
            Log.d(TAG, "no GnssCD enabled");
        } else if (type == 0) {
            this.mContext = context;
            savePoint(0, extraInfo);
            Log.d(TAG, "register listener");
            registerControlListener();
        }
    }

    public void savePoint(int type, String extraInfo) {
        if (!allowCollect()) {
            Log.d(TAG, "no GnssCD enabled");
        } else if (type == 0) {
            startHandlerThread();
            setCurrentState(type);
        } else if (1 == type) {
            int i = mCurrentState;
            if (i == 0 || i == 3 || i == 5) {
                this.mSessionInfo.newSessionReset();
                sendMessage(type, extraInfo);
            }
        } else if (2 != type) {
            if (3 == type) {
                int i2 = mCurrentState;
                if (i2 == 1 || i2 == 2 || i2 == 4) {
                    sendMessage(type, extraInfo);
                }
            } else if (4 == type) {
                int i3 = mCurrentState;
                if (i3 == 2 || i3 == 4) {
                    sendMessage(type, extraInfo);
                }
            } else if (6 == type && extraInfo != null && extraInfo.startsWith("$PQWP6")) {
                sendMessage(type, extraInfo);
            }
        } else if (mCurrentState == 1) {
            sendMessage(type, extraInfo);
        }
    }

    private void registerControlListener() {
        Context context = this.mContext;
        if (context == null) {
            Log.e(TAG, "no context");
        } else {
            context.getContentResolver().registerContentObserver(MiuiSettings.SettingsCloudData.getCloudDataNotifyUri(), true, new ContentObserver(null) { // from class: com.android.server.location.GnssCollectDataImpl.1
                @Override // android.database.ContentObserver
                public void onChange(boolean selfChange) {
                    GnssCollectDataImpl.this.updateControlState();
                }
            });
        }
    }

    public void updateControlState() {
        Log.d(TAG, "got rule changed");
        String newState = MiuiSettings.SettingsCloudData.getCloudDataString(this.mContext.getContentResolver(), CLOUDMODULE, CLOUDKEY, "1");
        String newsuplState = MiuiSettings.SettingsCloudData.getCloudDataString(this.mContext.getContentResolver(), CLOUDMODULE, CLOUDKEYSUPL, "0");
        SystemProperties.set(GNSS_MQS_SWITCH, newState);
        SystemProperties.set(SUPL_SWITCH, newsuplState);
        String rtkNewState = MiuiSettings.SettingsCloudData.getCloudDataString(this.mContext.getContentResolver(), CLOUD_MODULE_RTK, CLOUDKEY, "ON");
        SystemProperties.set(RTK_SWITCH, rtkNewState);
        updateGpoCloudConfig();
    }

    private void updateGpoCloudConfig() {
        int gpoNewVersion = MiuiSettings.SettingsCloudData.getCloudDataInt(this.mContext.getContentResolver(), CLOUDMODULE, CLOUDGPOKEY, -1);
        Log.i(TAG, "Got GpoVersion Cloud Config ? " + (gpoNewVersion >= 0));
        if (gpoNewVersion >= 0) {
            GnssPowerOptimizeStub.getInstance().setGpoVersionValue(gpoNewVersion);
        }
    }

    private void startHandlerThread() {
        HandlerThread handlerThread = new HandlerThread("GnssCD thread");
        this.mHandlerThread = handlerThread;
        handlerThread.start();
        this.mHandler = new Handler(this.mHandlerThread.getLooper()) { // from class: com.android.server.location.GnssCollectDataImpl.2
            @Override // android.os.Handler
            public void handleMessage(Message msg) {
                int message = msg.what;
                switch (message) {
                    case 1:
                        GnssCollectDataImpl.this.saveStartStatus();
                        GnssCollectDataImpl.this.setCurrentState(1);
                        return;
                    case 2:
                        GnssCollectDataImpl.this.saveFixStatus();
                        GnssCollectDataImpl.this.setCurrentState(2);
                        return;
                    case 3:
                        GnssCollectDataImpl.this.saveStopStatus();
                        GnssCollectDataImpl.this.setCurrentState(3);
                        GnssCollectDataImpl.this.saveState();
                        return;
                    case 4:
                        GnssCollectDataImpl.this.saveLoseStatus();
                        GnssCollectDataImpl.this.setCurrentState(4);
                        return;
                    case 5:
                        GnssCollectDataImpl.this.saveLog();
                        GnssCollectDataImpl.this.setCurrentState(5);
                        return;
                    case 6:
                        GnssCollectDataImpl.this.parseNmea((String) msg.obj);
                        return;
                    case 7:
                        GnssCollectDataImpl.this.setL1Cn0(((Double) msg.obj).doubleValue());
                        return;
                    case 8:
                        GnssCollectDataImpl.this.setL5Cn0(((Double) msg.obj).doubleValue());
                        return;
                    case 9:
                        GnssCollectDataImpl.this.setB1Cn0(((Double) msg.obj).doubleValue());
                        return;
                    default:
                        return;
                }
            }
        };
    }

    public void setCurrentState(int s) {
        mCurrentState = s;
    }

    public void saveStartStatus() {
        this.mSessionInfo.setStart();
    }

    public void saveFixStatus() {
        this.mSessionInfo.setTtffAuto();
    }

    public void saveLoseStatus() {
        this.mSessionInfo.setLostTimes();
    }

    public void saveStopStatus() {
        this.mSessionInfo.setEnd();
    }

    public void saveState() {
        sendMessage(5, null);
    }

    public void parseNmea(String nmea) {
        this.mSessionInfo.parseNmea(nmea);
    }

    public void setL1Cn0(double cn0) {
        this.mSessionInfo.setL1Cn0(cn0);
    }

    public void setL5Cn0(double cn0) {
        this.mSessionInfo.setL5Cn0(cn0);
    }

    public void setB1Cn0(double cn0) {
        this.mSessionInfo.setB1Cn0(cn0);
    }

    private void setAlarm(Context context, String action) {
        if (context == null || action == null) {
            Log.d(TAG, "context || action == null");
            return;
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction(action);
        context.registerReceiver(this.mReceiver, filter);
        try {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService("alarm");
            Intent i = new Intent(action);
            PendingIntent p = PendingIntent.getBroadcast(context, 0, i, BroadcastQueueImpl.FLAG_IMMUTABLE);
            long elapsedRealtime = SystemClock.elapsedRealtime();
            int i2 = this.UPLOAD_REPEAT_TIME;
            alarmManager.setRepeating(2, elapsedRealtime + i2, i2, p);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
