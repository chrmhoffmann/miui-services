package com.android.server.input.overscroller;

import android.util.Slog;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.json.JSONException;
import org.json.JSONObject;
/* loaded from: classes.dex */
public class ScrollerOptimizationConfigProviderUtils {
    public static final String APP_LIST_NAME = "appList";
    private static final String FLING_VELOCITY_THRESHOLD = "flingVelocityThreshold";
    private static final String FLY_WHEEL = "flywheel";
    private static final String FLY_WHEEL_PARAM_1 = "flywheelParam1";
    private static final String FLY_WHEEL_PARAM_2 = "flywheelParam2";
    private static final String FLY_WHEEL_PARAM_3 = "flywheelParam3";
    private static final String FLY_WHEEL_TIME_INTERVAL_THRESHOLD = "flywheelTimeIntervalThreshold";
    private static final String FLY_WHEEL_VELOCITY_THRESHOLD_1 = "flywheelVelocityThreshold1";
    private static final String FLY_WHEEL_VELOCITY_THRESHOLD_2 = "flywheelVelocityThreshold2";
    private static final String FRICTION = "friction";
    private static final String OPTIMIZE_ENABLE = "isOptimizeEnable";
    public static final String PACKAGE_NAME = "packageName";
    private static final String TAG = ScrollerOptimizationConfigProviderUtils.class.getSimpleName();
    private static final String VELOCITY_THRESHOLD = "velocityThreshold";

    public static String parseGeneralConfig(JSONObject jsonAll) throws JSONException {
        JSONObject jsonGeneralConfig = new JSONObject();
        if (jsonAll.has(OPTIMIZE_ENABLE)) {
            int isOptimizeEnable = jsonAll.getInt(OPTIMIZE_ENABLE);
            jsonGeneralConfig.put(OPTIMIZE_ENABLE, isOptimizeEnable);
        }
        if (jsonAll.has(FRICTION)) {
            double friction = jsonAll.getDouble(FRICTION);
            jsonGeneralConfig.put(FRICTION, friction);
        }
        if (jsonAll.has(VELOCITY_THRESHOLD)) {
            int velocityThreshold = jsonAll.getInt(VELOCITY_THRESHOLD);
            jsonGeneralConfig.put(VELOCITY_THRESHOLD, velocityThreshold);
        }
        if (jsonAll.has(FLY_WHEEL)) {
            int flywheel = jsonAll.getInt(FLY_WHEEL);
            jsonGeneralConfig.put(FLY_WHEEL, flywheel);
        }
        if (jsonAll.has(FLY_WHEEL_TIME_INTERVAL_THRESHOLD)) {
            int flywheelTimeIntervalThreshold = jsonAll.getInt(FLY_WHEEL_TIME_INTERVAL_THRESHOLD);
            jsonGeneralConfig.put(FLY_WHEEL_TIME_INTERVAL_THRESHOLD, flywheelTimeIntervalThreshold);
        }
        if (jsonAll.has(FLY_WHEEL_VELOCITY_THRESHOLD_1)) {
            int flywheelVelocityThreshold1 = jsonAll.getInt(FLY_WHEEL_VELOCITY_THRESHOLD_1);
            jsonGeneralConfig.put(FLY_WHEEL_VELOCITY_THRESHOLD_1, flywheelVelocityThreshold1);
        }
        if (jsonAll.has(FLY_WHEEL_VELOCITY_THRESHOLD_2)) {
            int flywheelVelocityThreshold2 = jsonAll.getInt(FLY_WHEEL_VELOCITY_THRESHOLD_2);
            jsonGeneralConfig.put(FLY_WHEEL_VELOCITY_THRESHOLD_2, flywheelVelocityThreshold2);
        }
        if (jsonAll.has(FLY_WHEEL_PARAM_1)) {
            double flywheelParam1 = jsonAll.getDouble(FLY_WHEEL_PARAM_1);
            jsonGeneralConfig.put(FLY_WHEEL_PARAM_1, flywheelParam1);
        }
        if (jsonAll.has(FLY_WHEEL_PARAM_2)) {
            double flywheelParam2 = jsonAll.getDouble(FLY_WHEEL_PARAM_2);
            jsonGeneralConfig.put(FLY_WHEEL_PARAM_2, flywheelParam2);
        }
        if (jsonAll.has(FLY_WHEEL_PARAM_3)) {
            int flywheelParam3 = jsonAll.getInt(FLY_WHEEL_PARAM_3);
            jsonGeneralConfig.put(FLY_WHEEL_PARAM_3, flywheelParam3);
        }
        if (jsonAll.has(FLING_VELOCITY_THRESHOLD)) {
            int flingVelocityThreshold = jsonAll.getInt(FLING_VELOCITY_THRESHOLD);
            jsonGeneralConfig.put(FLING_VELOCITY_THRESHOLD, flingVelocityThreshold);
        }
        return jsonGeneralConfig.toString();
    }

    public static String readLocalFile(String filePath) {
        StringBuffer stringBuffer = new StringBuffer();
        File file = new File(filePath);
        if (file.exists()) {
            InputStream inputStream = null;
            try {
                try {
                    try {
                        inputStream = new FileInputStream(file);
                        byte[] buffer = new byte[1024];
                        while (true) {
                            int lenth = inputStream.read(buffer);
                            if (lenth == -1) {
                                break;
                            }
                            stringBuffer.append(new String(buffer, 0, lenth));
                        }
                        inputStream.close();
                    } catch (IOException e) {
                        Slog.e(TAG, "exception when readLocalFile: ", e);
                        inputStream.close();
                    }
                } catch (Throwable th) {
                    try {
                        inputStream.close();
                    } catch (IOException e2) {
                        Slog.e(TAG, "exception when readLocalFile: ", e2);
                    }
                    throw th;
                }
            } catch (IOException e3) {
                Slog.e(TAG, "exception when readLocalFile: ", e3);
            }
        }
        return stringBuffer.toString();
    }

    public static JSONObject getLocalFileConfig(String filePath) {
        JSONObject jsonObject = new JSONObject();
        String configString = readLocalFile(filePath);
        try {
            JSONObject jsonObject2 = new JSONObject(configString);
            return jsonObject2;
        } catch (JSONException e) {
            Slog.e(TAG, "exception when getLocalFileConfig: ", e);
            return jsonObject;
        }
    }
}
