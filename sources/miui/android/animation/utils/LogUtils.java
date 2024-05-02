package miui.android.animation.utils;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
/* loaded from: classes.dex */
public class LogUtils {
    private static final String COMMA = ", ";
    private static volatile boolean sIsLogEnabled;
    private static final Handler sLogHandler;
    private static final Map<Integer, String> sTag = new ConcurrentHashMap();
    private static final HandlerThread sThread;

    static {
        HandlerThread handlerThread = new HandlerThread("LogThread");
        sThread = handlerThread;
        handlerThread.start();
        sLogHandler = new Handler(handlerThread.getLooper()) { // from class: miui.android.animation.utils.LogUtils.1
            @Override // android.os.Handler
            public void handleMessage(Message msg) {
                if (msg.what == 0) {
                    Log.d((String) LogUtils.sTag.get(Integer.valueOf(msg.arg1)), "thread log, " + ((String) msg.obj));
                }
                msg.obj = null;
            }
        };
    }

    public static void logThread(String tag, String log) {
        Message msg = sLogHandler.obtainMessage(0);
        msg.obj = log;
        msg.arg1 = tag.hashCode();
        sTag.put(Integer.valueOf(msg.arg1), tag);
        msg.sendToTarget();
    }

    private LogUtils() {
    }

    public static void getLogEnableInfo() {
        String logLevel = "";
        try {
            logLevel = CommonUtils.readProp("log.tag.folme.level");
            logLevel = logLevel == null ? "" : logLevel;
        } catch (Exception e) {
            Log.i(CommonUtils.TAG, "can not access property log.tag.folme.level, no log", e);
        }
        Log.d(CommonUtils.TAG, "logLevel = " + logLevel);
        sIsLogEnabled = logLevel.equals("D");
    }

    public static boolean isLogEnabled() {
        return sIsLogEnabled;
    }

    public static void debug(String message, Object... objArray) {
        if (!sIsLogEnabled) {
            return;
        }
        if (objArray.length > 0) {
            StringBuilder sb = new StringBuilder(COMMA);
            int initLength = sb.length();
            for (Object obj : objArray) {
                if (sb.length() > initLength) {
                    sb.append(COMMA);
                }
                sb.append(obj);
            }
            Log.i(CommonUtils.TAG, message + sb.toString());
            return;
        }
        Log.i(CommonUtils.TAG, message);
    }

    public static String getStackTrace(int length) {
        StackTraceElement[] traces = Thread.currentThread().getStackTrace();
        int count = Math.min(traces.length, length + 4);
        return Arrays.toString(Arrays.asList(traces).subList(3, count).toArray());
    }
}
