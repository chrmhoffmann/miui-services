package com.android.server.display;

import android.os.FileObserver;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Slog;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import miui.util.FeatureParser;
/* loaded from: classes.dex */
public class ThermalObserver {
    private static boolean DEBUG = false;
    private static final float DISABLE_GALLERY_HDR_TEMPERATURE = 37000.0f;
    private static final float ENABLE_GALLERY_TEMPERATURE = 35000.0f;
    private static final int MSG_UPDATE_TEMP = 1;
    private static final String TAG = "ThermalObserver";
    private static final String TEMPERATURE_PATH = "/sys/class/thermal/thermal_message/board_sensor_temp";
    private static final boolean mSupportGalleryHDR = FeatureParser.getBoolean("support_gallery_hdr", false);
    private float mCurrentTemperature = -1.0f;
    private DisplayPowerControllerImpl mDisplayPowerControllerImpl;
    private TemperatureControllerHandler mHandler;
    private TemperatureObserver mTemperatureObserver;

    public ThermalObserver(Looper looper, DisplayPowerControllerImpl impl) {
        this.mHandler = new TemperatureControllerHandler(looper);
        this.mDisplayPowerControllerImpl = impl;
        TemperatureObserver temperatureObserver = new TemperatureObserver(new File(TEMPERATURE_PATH));
        this.mTemperatureObserver = temperatureObserver;
        if (mSupportGalleryHDR) {
            temperatureObserver.startWatching();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class TemperatureObserver extends FileObserver {
        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public TemperatureObserver(File file) {
            super(file);
            ThermalObserver.this = this$0;
        }

        @Override // android.os.FileObserver
        public void onEvent(int event, String path) {
            int type = event & 4095;
            if (type == 2) {
                ThermalObserver.this.mHandler.removeMessages(1);
                ThermalObserver.this.mHandler.sendEmptyMessage(1);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class TemperatureControllerHandler extends Handler {
        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public TemperatureControllerHandler(Looper looper) {
            super(looper, null, true);
            ThermalObserver.this = r2;
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    ThermalObserver.this.updateTemperature();
                    return;
                default:
                    return;
            }
        }
    }

    public void updateTemperature() {
        float boardTemperature = getBoardTemperature();
        this.mCurrentTemperature = boardTemperature;
        if (boardTemperature >= DISABLE_GALLERY_HDR_TEMPERATURE) {
            Slog.i(TAG, "Gallery hdr is disable by thermal.");
            this.mDisplayPowerControllerImpl.updateGalleryHdrThermalThrottler(true);
        } else if (boardTemperature <= ENABLE_GALLERY_TEMPERATURE) {
            Slog.i(TAG, "Temperature control has been released.");
            this.mDisplayPowerControllerImpl.updateGalleryHdrThermalThrottler(false);
        }
    }

    private static String readSysNodeInfo(String nodePath) {
        File file = new File(nodePath);
        if (!file.exists() || file.length() <= 0) {
            return null;
        }
        StringBuilder info = new StringBuilder();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(nodePath));
            String temp = reader.readLine();
            while (temp != null) {
                info.append(temp);
                temp = reader.readLine();
                if (temp == null) {
                    break;
                }
                info.append("\n");
            }
            reader.close();
        } catch (IOException e) {
        }
        return info.toString();
    }

    private float getBoardTemperature() {
        String node = readSysNodeInfo(TEMPERATURE_PATH);
        if (node != null) {
            try {
                if (DEBUG) {
                    Slog.d(TAG, "read BoardTemp node: " + Float.parseFloat(node));
                }
                return Float.parseFloat(node);
            } catch (NumberFormatException e) {
                Slog.e(TAG, "read BoardTemp error node: " + node);
                return -1.0f;
            }
        }
        return -1.0f;
    }

    public void dump(PrintWriter pw) {
        pw.println();
        pw.println("Thermal Observer State:");
        pw.println("  mCurrentTemperature=" + this.mCurrentTemperature);
        DEBUG = DisplayDebugConfig.DEBUG_DMS;
    }
}
