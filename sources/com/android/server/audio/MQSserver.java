package com.android.server.audio;

import android.content.Context;
import android.content.Intent;
import android.os.Process;
import android.os.SystemProperties;
import android.server.am.SplitScreenReporter;
import android.util.Log;
import android.util.Slog;
import com.android.server.MiuiBatteryStatsService;
import com.android.server.display.TemperatureController;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Calendar;
import org.json.JSONObject;
/* loaded from: classes.dex */
public class MQSserver extends Thread {
    private static final String ANDROID_VERSION = SystemProperties.get("ro.build.version.release", "");
    private static final int FLAG_NON_ANONYMOUS = 2;
    private static final int MAX_LEN = 512;
    private static final String MQS_COUNT = "onetrack_count";
    private static final String MQS_MODULE_ID = "mqs_audio_data_21031000";
    private static final String TAG = "MQSserver";
    private static final String XLOG_DEV = "/dev/xlog";
    private static volatile MQSserver mMQSserver;
    private int day;
    private FileInputStream fis;
    private ArrayList<String> list = new ArrayList<>();
    private Context mContext;
    private volatile boolean mStopRequst;
    private int month;
    private int year;

    private MQSserver(Context context) {
        this.mContext = context;
    }

    public static MQSserver getInstance(Context context) {
        if (mMQSserver == null) {
            synchronized (MQSserver.class) {
                if (mMQSserver == null) {
                    mMQSserver = new MQSserver(context);
                    if (mMQSserver.init()) {
                        mMQSserver.start();
                    }
                }
            }
        }
        return mMQSserver;
    }

    public boolean needToReport() {
        Calendar calendar = Calendar.getInstance();
        if (this.year == calendar.get(1) && this.month == calendar.get(2) + 1 && this.day == calendar.get(5)) {
            return false;
        }
        this.year = calendar.get(1);
        this.month = calendar.get(2) + 1;
        this.day = calendar.get(5);
        return true;
    }

    public boolean checkList(String Name, String Audio_Event) {
        for (int i = 0; i < this.list.size(); i++) {
            try {
                String str = this.list.get(i);
                JSONObject data_json = new JSONObject(str);
                String name = data_json.optString(TemperatureController.STRATEGY_NAME);
                String audio_event = data_json.optString("audio_event");
                if (name.equals(Name) && audio_event.equals(Audio_Event)) {
                    return true;
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.i(TAG, "erroe for checkList");
                return false;
            }
        }
        return false;
    }

    public boolean addList(String str) {
        try {
            JSONObject data_json = new JSONObject(str);
            data_json.put(MQS_COUNT, SplitScreenReporter.ACTION_ENTER_SPLIT);
            String newstr = data_json.toString();
            Log.d(TAG, "add count to List : " + newstr);
            this.list.add(newstr);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            Log.i(TAG, "erroe for addList");
            return false;
        }
    }

    public boolean updateCount(String Name, String Audio_Event) {
        Exception e;
        String cn;
        Log.d(TAG, "enter updateCount" + Name);
        for (int i = 0; i < this.list.size(); i++) {
            try {
                String str = this.list.get(i);
                JSONObject data_json = new JSONObject(str);
                String name = data_json.optString(TemperatureController.STRATEGY_NAME);
                String audio_event = data_json.optString("audio_event");
                if (name.equals(Name) && audio_event.equals(Audio_Event)) {
                    int index = this.list.indexOf(str);
                    this.list.remove(str);
                    String cn2 = data_json.optString(MQS_COUNT, "empty");
                    Log.d(TAG, "read count frome file: " + cn2);
                    if (cn2.equals("empty")) {
                        cn = SplitScreenReporter.ACTION_ENTER_SPLIT;
                    } else {
                        int count = Integer.parseInt(cn2);
                        cn = String.valueOf(count + 1);
                    }
                    data_json.put(MQS_COUNT, cn);
                    String str2 = data_json.toString();
                    try {
                        this.list.add(index, str2);
                        Log.d(TAG, "update RBI json: " + str2);
                        return true;
                    } catch (Exception e2) {
                        e = e2;
                        e.printStackTrace();
                        Log.i(TAG, "erroe for updateCount");
                        return false;
                    }
                }
            } catch (Exception e3) {
                e = e3;
            }
        }
        return false;
    }

    public boolean init() {
        File file = new File(XLOG_DEV);
        try {
            Log.d(TAG, "file exists " + file.exists() + " " + XLOG_DEV);
            Log.d(TAG, "file can read " + file.canRead() + " " + XLOG_DEV);
            Log.d(TAG, "file is file " + file.isFile() + " " + XLOG_DEV);
            Log.d(TAG, "file is isDirectory " + file.isDirectory() + " " + XLOG_DEV);
            if (file.exists() && file.canRead()) {
                this.fis = new FileInputStream(file);
                file.delete();
                this.mStopRequst = false;
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.i(TAG, "erroe for opening /dev/xlog");
        }
        file.delete();
        return false;
    }

    @Override // java.lang.Thread, java.lang.Runnable
    public void run() {
        Process.setThreadPriority(-19);
        try {
            byte[] buf = new byte[512];
            while (!this.mStopRequst) {
                int len = this.fis.read(buf);
                String str = new String(buf, 0, len);
                Log.i(TAG, "getData len: " + len + " content: " + str);
                JSONObject data_json = new JSONObject(str);
                String name = data_json.optString(TemperatureController.STRATEGY_NAME);
                String audio_event = data_json.optString("audio_event");
                if (len > 512) {
                    Log.e(TAG, "the length is out of range: " + str.length());
                } else if (checkList(name, audio_event)) {
                    updateCount(name, audio_event);
                    Log.d(TAG, "updateCount: name: " + name + "/audio_event : " + audio_event);
                } else {
                    addList(str);
                }
                if (needToReport()) {
                    Slog.d(TAG, "needToReport year: " + this.year + "month: " + this.month + "day: " + this.day);
                    reportData();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "erroe for reading  /dev/xlog");
        }
        try {
            this.fis.close();
        } catch (Exception e2) {
            e2.printStackTrace();
            Log.e(TAG, "erroe for close  /dev/xlog");
        }
    }

    public void setStop() {
        Log.d(TAG, "setStop request");
        this.mStopRequst = true;
    }

    private void reportData() {
        for (int i = 0; i < this.list.size(); i++) {
            String str = this.list.get(i);
            onetrack_report(str);
        }
        this.list.clear();
    }

    private void onetrack_report(String data) {
        String event_context;
        Log.d(TAG, "onetrack_report: " + data);
        try {
            JSONObject data_json = new JSONObject(data);
            String event_name = data_json.getString(TemperatureController.STRATEGY_NAME);
            if (!isReportXiaomiServer() && data_json.has(TemperatureController.STRATEGY_NAME) && !isAlowedEventReportInternationalRegion(event_name)) {
                Log.d(TAG, "This event is not reported in the international version");
                return;
            }
            Intent intent = new Intent(MiuiBatteryStatsService.TrackBatteryUsbInfo.ACTION_TRACK_EVENT);
            if (isReportXiaomiServer()) {
                if (data_json.has("audio_event")) {
                    event_context = data_json.getString("audio_event");
                    intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_APP_ID, "31000000086");
                } else if (data_json.has("haptic_event")) {
                    event_context = data_json.getString("haptic_event");
                    intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_APP_ID, "31000000089");
                } else {
                    return;
                }
            } else if (data_json.has("audio_event")) {
                event_context = data_json.getString("audio_event");
                intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_APP_ID, "31000000962");
                intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_PACKAGE, "com.mi.global.multimedia");
                intent.putExtra("PROJECT_ID", "mi-multimedia-global");
                intent.putExtra("TOPIC", "mqs_multimedia");
                intent.putExtra("PRIVATE_KEY_ID", "c5b2b941d0b2f19780459b7e48ffce62303edf28");
                Log.d(TAG, "This event is reported in the international version");
            } else {
                return;
            }
            String count = data_json.getString(MQS_COUNT);
            intent.setPackage(MiuiBatteryStatsService.TrackBatteryUsbInfo.ANALYTICS_PACKAGE);
            intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_EVENT_NAME, event_name);
            intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_PACKAGE, "android");
            intent.putExtra(MQS_COUNT, count);
            JSONObject context_json = new JSONObject(event_context);
            for (int i = 0; i < context_json.names().length(); i++) {
                String key = context_json.names().getString(i);
                String value = context_json.getString(key);
                intent.putExtra(key, value);
                Log.d(TAG, "key: " + key + " ,value: " + value);
            }
            intent.setFlags(2);
            Log.d(TAG, "startService: " + intent.toString());
            this.mContext.startService(intent);
        } catch (Exception e) {
            e.printStackTrace();
            Log.i(TAG, "erroe for reportData");
        }
    }

    private boolean isReportXiaomiServer() {
        String region = SystemProperties.get("ro.miui.region", "");
        Slog.i(TAG, "the region is :" + region);
        return region.equals("CN") || region.equals("RU");
    }

    private boolean isAlowedEventReportInternationalRegion(String event_name) {
        return event_name.equals("headphones") || event_name.equals("voicecall_and_voip");
    }
}
