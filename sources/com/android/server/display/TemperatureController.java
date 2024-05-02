package com.android.server.display;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.FileObserver;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Slog;
import android.util.Spline;
import com.android.server.ScoutHelper;
import com.android.server.display.MiuiDisplayCloudController;
import com.android.server.wm.MiuiFreeformPinManagerService;
import com.miui.server.input.util.MiuiCustomizeShortCutUtils;
import com.miui.server.sptm.SpeedTestModeServiceImpl;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
/* loaded from: classes.dex */
public class TemperatureController {
    private static final boolean DEBUG = false;
    public static final float RATE_DIMING = 0.007354082f;
    private static final int STARTING_TEMPERATURE = 35000;
    public static final String STRATEGY_AVERAGE_TIMES = "average_times";
    public static final String STRATEGY_BRIGHTNESS = "brightness";
    public static final String STRATEGY_DOWN_THRESHOLD = "down_threshold";
    public static final String STRATEGY_NAME = "name";
    public static final String STRATEGY_TEMPERATURE = "temperature";
    public static final String STRATEGY_UP_THRESHOLD = "up_threshold";
    private static final String TAG = "TemperatureController";
    private static final String TEMPERATURE_PATH = "/sys/class/thermal/thermal_message/board_sensor_temp";
    private static final String THERMAL_CONFIG_ID_PATH = "/sys/class/thermal/thermal_message/sconfig";
    private static final String THERMAL_PATH = "/sys/class/thermal";
    private static final int UPDATE_CONFIG = 3;
    private static final int UPDATE_MAX_BRIGHTNESS_LIMIT = 4;
    private static final int UPDATE_STRATEGY = 2;
    private static final int UPDATE_TEMPERATURE = 1;
    private boolean mAnimating;
    private Context mContext;
    private Strategy mCurrentStrategy;
    private Strategy mDefaultMode;
    private DisplayPowerController mDisplayPowerController;
    private TemperatureControllerHandler mHandler;
    private final float mMaxBrightness;
    private MaxBrightnessObserver mMaxBrightnessObserver;
    private MiuiDisplayCloudController mMiuiDisplayCloudController;
    private boolean mPowerPerformanceMode;
    private ContentResolver mResolver;
    private SettingsObserver mSettingsObserver;
    private ThermalConfigIdObserver mThermalConfigIdObserver;
    private String mThermalCurStatePath;
    private final int THERMAL_ID_DEFAULT = 0;
    private final int THERMAL_ID_EXTREME = 2;
    private final int THERMAL_ID_HUANJI = 3;
    private final int THERMAL_ID_VIDEOCHAT = 4;
    private final int THERMAL_ID_ABNORMAL = 5;
    private final int THERMAL_ID_NIGHTVIDEO = 6;
    private final int THERMAL_ID_DOLBYVISION = 7;
    private final int THERMAL_ID_INCALL = 8;
    private final int THERMAL_ID_GAME = 9;
    private final int THERMAL_ID_EVALUATION = 10;
    private final int THERMAL_ID_CPU_CLASS0 = 11;
    private final int THERMAL_ID_CAMERA = 12;
    private final int THERMAL_ID_PUBG = 13;
    private final int THERMAL_ID_YOUTUBE = 14;
    private final int THERMAL_ID_ARVR = 15;
    private final int THERMAL_ID_GAME2 = 16;
    private final int THERMAL_ID_CAMERA_4K_VIDEO = 17;
    private final int THERMAL_ID_CAMERA_8K_VIDEO = 18;
    private final int THERMAL_ID_NAVIGATION = 19;
    private final int THERMAL_ID_GAME_NORMAL = 20;
    private final int THERMAL_ID_VIDEO = 21;
    private final int THERMAL_ID_DEMO = 22;
    private final int THERMAL_ID_YUANSHEN = 23;
    private final int THERMAL_ID_SPTM = 24;
    private final int THERMAL_ID_MAX = 25;
    private final int THERMAL_ID_INCREASE = 100;
    private final int THERMAL_ID_PERINCREASE = 30;
    private final int THERMAL_ID_ALL_48_IECINCREASE = ScoutHelper.BINDER_FULL_KILL_SCORE_ADJ_MIN;
    private final int THERMAL_ID_IECINCREASE = 500;
    private final int THERMAL_ID_CGAME = -1;
    private final int THERMAL_ID_PER_CGAME = -2;
    private final int THERMAL_ID_FOLD_CGAME = -3;
    private final int THERMAL_ID_PER_FOLD_CGAME = -4;
    private final Map<String, Integer> mThermalStrategyNameMap = (Map) Stream.of((Object[]) new Object[][]{new Object[]{"DEFAULT", 0}, new Object[]{"EXTREME", 2}, new Object[]{"HUANJI", 3}, new Object[]{"VIDEOCHAT", 4}, new Object[]{"ABNORMAL", 5}, new Object[]{"NIGHTVIDEO", 6}, new Object[]{"DOLBYVISION", 7}, new Object[]{"INCALL", 8}, new Object[]{"GAME", 9}, new Object[]{"EVALUATION", 10}, new Object[]{"CPU_CLASS0", 11}, new Object[]{"CAMERA", 12}, new Object[]{"PUBG", 13}, new Object[]{"YOUTUBE", 14}, new Object[]{"ARVR", 15}, new Object[]{"TGAME", 16}, new Object[]{"CAMERA_4K_VIDEO", 17}, new Object[]{"CAMERA_8K_VIDEO", 18}, new Object[]{"NAVIGATION", 19}, new Object[]{"GAME_NORMAL", 20}, new Object[]{"VIDEO", 21}, new Object[]{"DEMO", 22}, new Object[]{"YUANSHEN", 23}, new Object[]{SpeedTestModeServiceImpl.TAG, 24}, new Object[]{"MAX", 25}, new Object[]{"INCREASE", 100}, new Object[]{"PERINCREASE", 30}, new Object[]{"ALL_48_IECINCREASE", Integer.valueOf((int) ScoutHelper.BINDER_FULL_KILL_SCORE_ADJ_MIN)}, new Object[]{"IECINCREASE", 500}, new Object[]{"CGAME", -1}, new Object[]{"PER_CGAME", -2}, new Object[]{"FOLD_CGAME", -3}, new Object[]{"PER_FOLD_CGAME", -4}}).collect(Collectors.toMap(new Function() { // from class: com.android.server.display.TemperatureController$$ExternalSyntheticLambda0
        @Override // java.util.function.Function
        public final Object apply(Object obj) {
            return TemperatureController.lambda$new$0((Object[]) obj);
        }
    }, new Function() { // from class: com.android.server.display.TemperatureController$$ExternalSyntheticLambda1
        @Override // java.util.function.Function
        public final Object apply(Object obj) {
            return TemperatureController.lambda$new$1((Object[]) obj);
        }
    }));
    private int mThermalId = 0;
    private float mThermalMaxBrightnessPercent = 1.0f;
    private final float THERMAL_MAX_CUR_STATE = 200.0f;
    private float mLastTemperature = MiuiFreeformPinManagerService.EDGE_AREA;
    private final Deque<Float> mQueue = new ArrayDeque();
    private final Map<Integer, Strategy> mStrategyMap = new HashMap();
    private MiuiDisplayCloudController.Observer mObserver = new MiuiDisplayCloudController.Observer() { // from class: com.android.server.display.TemperatureController.1
        @Override // com.android.server.display.MiuiDisplayCloudController.Observer
        public void update() {
            TemperatureController.this.scheduleUpdateConfig();
        }
    };

    public static /* synthetic */ String lambda$new$0(Object[] data) {
        return (String) data[0];
    }

    public static /* synthetic */ Integer lambda$new$1(Object[] data) {
        return (Integer) data[1];
    }

    public TemperatureController(Context context, Looper looper, DisplayPowerController displayPowerController, MiuiDisplayCloudController miuiDisplayCloudController) {
        boolean z = false;
        this.mMiuiDisplayCloudController = miuiDisplayCloudController;
        PowerManager pm = (PowerManager) context.getSystemService(PowerManager.class);
        this.mMaxBrightness = pm.getBrightnessConstraint(1);
        this.mHandler = new TemperatureControllerHandler(looper);
        this.mDisplayPowerController = displayPowerController;
        this.mContext = context;
        this.mResolver = context.getContentResolver();
        this.mSettingsObserver = new SettingsObserver(this.mHandler);
        this.mResolver.registerContentObserver(Settings.System.getUriFor("POWER_PERFORMANCE_MODE_OPEN"), false, this.mSettingsObserver, -1);
        this.mPowerPerformanceMode = Settings.System.getInt(context.getContentResolver(), "POWER_PERFORMANCE_MODE_OPEN", 0) == 1 ? true : z;
        ThermalConfigIdObserver thermalConfigIdObserver = new ThermalConfigIdObserver(new File(THERMAL_CONFIG_ID_PATH));
        this.mThermalConfigIdObserver = thermalConfigIdObserver;
        thermalConfigIdObserver.startWatching();
        String findThermalLimitMaxBrightnessNode = findThermalLimitMaxBrightnessNode();
        this.mThermalCurStatePath = findThermalLimitMaxBrightnessNode;
        if (findThermalLimitMaxBrightnessNode != null) {
            MaxBrightnessObserver maxBrightnessObserver = new MaxBrightnessObserver(new File(this.mThermalCurStatePath));
            this.mMaxBrightnessObserver = maxBrightnessObserver;
            maxBrightnessObserver.startWatching();
        }
        this.mMiuiDisplayCloudController.registerObserver(this.mObserver);
        scheduleUpdateConfig();
        scheduleUpdateTemperature();
    }

    public void updateConfig(Map<String, Map<String, Object>> configMap) {
        this.mStrategyMap.clear();
        for (Map.Entry<String, Map<String, Object>> entry : configMap.entrySet()) {
            String name = entry.getKey();
            if (name != null) {
                Map<String, Object> item = entry.getValue();
                Object brightness = item.get("brightness");
                Object temperature = item.get(STRATEGY_TEMPERATURE);
                Object downThreshold = item.get(STRATEGY_DOWN_THRESHOLD);
                Object upThreshold = item.get(STRATEGY_UP_THRESHOLD);
                Object averageTimes = item.get(STRATEGY_AVERAGE_TIMES);
                if (brightness != null && temperature != null && downThreshold != null && upThreshold != null && averageTimes != null) {
                    Spline spline = Spline.createSpline(getFloatArray((String) temperature), getFloatArray((String) brightness));
                    Strategy strategy = new Strategy(spline, ((Integer) averageTimes).intValue(), ((Integer) downThreshold).intValue(), ((Integer) upThreshold).intValue());
                    this.mStrategyMap.put(this.mThermalStrategyNameMap.get(name), strategy);
                }
            }
        }
        this.mDefaultMode = this.mStrategyMap.get(0);
        this.mCurrentStrategy = this.mStrategyMap.getOrDefault(Integer.valueOf(this.mThermalId), this.mDefaultMode);
    }

    public void scheduleUpdateConfig() {
        Message msg = this.mHandler.obtainMessage(3);
        msg.obj = new HashMap(this.mMiuiDisplayCloudController.getTemperatureControllerMapper());
        this.mHandler.removeMessages(3);
        this.mHandler.sendMessage(msg);
    }

    public void scheduleUpdateTemperature() {
        this.mHandler.removeMessages(1);
        this.mHandler.sendEmptyMessageDelayed(1, 10000L);
    }

    private String findThermalLimitMaxBrightnessNode() {
        File thermalFile = new File(THERMAL_PATH);
        File[] coolingDeviceFiles = thermalFile.listFiles(new FileFilter() { // from class: com.android.server.display.TemperatureController.2
            @Override // java.io.FileFilter
            public boolean accept(File pathname) {
                return pathname.getName().startsWith("cooling_device");
            }
        });
        for (File f : coolingDeviceFiles) {
            File[] targetFile = f.listFiles(new FileFilter() { // from class: com.android.server.display.TemperatureController.3
                @Override // java.io.FileFilter
                public boolean accept(File pathname) {
                    if (pathname.getName().equals(MiuiCustomizeShortCutUtils.ATTRIBUTE_TYPE)) {
                        String result = TemperatureController.readSysNodeInfo(pathname.getAbsolutePath());
                        return result.equals("brightness0-clone");
                    }
                    return false;
                }
            });
            if (targetFile.length > 0) {
                return targetFile[0].getParent() + "/cur_state";
            }
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class ThermalConfigIdObserver extends FileObserver {
        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public ThermalConfigIdObserver(File file) {
            super(file);
            TemperatureController.this = this$0;
        }

        @Override // android.os.FileObserver
        public void onEvent(int event, String path) {
            int type = event & 4095;
            if (type == 8) {
                TemperatureController.this.mHandler.removeMessages(2);
                TemperatureController.this.mHandler.sendEmptyMessageDelayed(2, 200L);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class MaxBrightnessObserver extends FileObserver {
        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public MaxBrightnessObserver(File file) {
            super(file);
            TemperatureController.this = this$0;
        }

        @Override // android.os.FileObserver
        public void onEvent(int event, String path) {
            int type = event & 4095;
            if (type == 2) {
                TemperatureController.this.mHandler.removeMessages(4);
                TemperatureController.this.mHandler.sendEmptyMessage(4);
            }
        }
    }

    /* loaded from: classes.dex */
    public final class TemperatureControllerHandler extends Handler {
        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public TemperatureControllerHandler(Looper looper) {
            super(looper, null, true);
            TemperatureController.this = r2;
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    TemperatureController.this.updateTemperature();
                    TemperatureController.this.scheduleUpdateTemperature();
                    return;
                case 2:
                    TemperatureController.this.updateStrategy();
                    return;
                case 3:
                    Map<String, Map<String, Object>> configMap = (Map) msg.obj;
                    TemperatureController.this.updateConfig(configMap);
                    return;
                case 4:
                    TemperatureController.this.updateMaxBrightnessLimit();
                    return;
                default:
                    return;
            }
        }
    }

    public void updateTemperature() {
        if (this.mCurrentStrategy == null) {
            return;
        }
        float temperature = getBoardTemperture();
        float aveTemperature = getAverageTemperature(temperature);
        if (temperature - this.mLastTemperature > this.mCurrentStrategy.mDownThreshold) {
            Slog.d(TAG, "mTemperature: " + temperature + ", mLastTemperature: " + this.mLastTemperature + ", heating: " + (temperature - this.mLastTemperature));
            this.mLastTemperature = temperature;
            setupAnimation();
            updateBrightness(STARTING_TEMPERATURE);
        } else if (this.mLastTemperature - aveTemperature > this.mCurrentStrategy.mUpThreshold && aveTemperature > temperature) {
            Slog.d(TAG, "mTemperature: " + temperature + ", mLastTemperature: " + this.mLastTemperature + ", aveTemperature: " + aveTemperature + ", cool down: " + (this.mLastTemperature - aveTemperature));
            setupAnimation();
            this.mLastTemperature = aveTemperature;
            updateBrightness(STARTING_TEMPERATURE - this.mCurrentStrategy.mUpThreshold);
        }
    }

    public void updateStrategy() {
        int i;
        String id = readSysNodeInfo(THERMAL_CONFIG_ID_PATH);
        if (id != null) {
            try {
                this.mThermalId = Integer.parseInt(id);
            } catch (NumberFormatException e) {
                Slog.e(TAG, "updateStrategy error id: " + id);
            }
        }
        if (this.mPowerPerformanceMode && (i = this.mThermalId) >= 30) {
            this.mThermalId = i - 30;
        }
        this.mCurrentStrategy = this.mStrategyMap.getOrDefault(Integer.valueOf(this.mThermalId), this.mDefaultMode);
        updateBrightness(STARTING_TEMPERATURE);
    }

    public void updateMaxBrightnessLimit() {
        String data = readSysNodeInfo(this.mThermalCurStatePath);
        if (data != null) {
            try {
                float curState = Float.parseFloat(data);
                this.mThermalMaxBrightnessPercent = curState / 200.0f;
            } catch (NumberFormatException e) {
                Slog.e(TAG, "updateMaxBrightnessLimit error curState: " + data);
            }
        }
    }

    private void updateBrightness(int startingTemperature) {
        if (this.mLastTemperature >= startingTemperature) {
            this.mDisplayPowerController.updateBrightness();
        }
    }

    private float getAverageTemperature(float t) {
        this.mQueue.add(Float.valueOf(t));
        float count = MiuiFreeformPinManagerService.EDGE_AREA;
        for (Float f : this.mQueue) {
            float f2 = f.floatValue();
            count += f2;
        }
        float result = count / this.mQueue.size();
        if (this.mQueue.size() - 1 > this.mCurrentStrategy.mTimes) {
            this.mQueue.removeFirst();
        }
        return result;
    }

    private float getMaxBrightnessPercent() {
        if (this.mCurrentStrategy == null) {
            return 1.0f;
        }
        if (SystemProperties.getInt("dbg.dms.temp_close", 0) != 0) {
            Slog.d(TAG, "temp_close");
            return 1.0f;
        }
        float percent = this.mCurrentStrategy.mSpline.interpolate(this.mLastTemperature) / 200.0f;
        float f = this.mThermalMaxBrightnessPercent;
        if (f < percent) {
            return f;
        }
        return percent;
    }

    public float getTemperatureControlBrightness() {
        float percent = getMaxBrightnessPercent();
        return this.mMaxBrightness * percent;
    }

    private float[] getFloatArray(String value) {
        String[] arrayString = value.split(",");
        float[] arrayFloat = new float[arrayString.length];
        for (int i = 0; i < arrayString.length; i++) {
            try {
                arrayFloat[i] = Float.parseFloat(arrayString[i]);
            } catch (NumberFormatException e) {
            }
        }
        return arrayFloat;
    }

    public static String readSysNodeInfo(String nodePath) {
        File file = new File(nodePath);
        if (!file.exists() || file.length() <= 0) {
            return null;
        }
        StringBuilder info = new StringBuilder();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(nodePath));
            boolean firstIn = true;
            while (true) {
                String temp = reader.readLine();
                if (temp == null) {
                    break;
                }
                if (firstIn) {
                    firstIn = false;
                } else {
                    info.append("\n");
                }
                info.append(temp);
            }
            reader.close();
        } catch (IOException e) {
        }
        return info.toString();
    }

    private float getBoardTemperture() {
        String node = readSysNodeInfo(TEMPERATURE_PATH);
        if (node != null) {
            try {
                return Float.parseFloat(node);
            } catch (NumberFormatException e) {
                Slog.e(TAG, "read BoardTemp error node: " + node);
                return -1.0f;
            }
        }
        return -1.0f;
    }

    public boolean getAnimating() {
        return this.mAnimating;
    }

    public void resetAnimation() {
        this.mAnimating = false;
    }

    private void setupAnimation() {
        float curBrightness = this.mDisplayPowerController.getScreenBrightnessSetting();
        if (curBrightness > getTemperatureControlBrightness()) {
            this.mAnimating = true;
        }
    }

    /* loaded from: classes.dex */
    public class Strategy {
        public int mDownThreshold;
        public Spline mSpline;
        public int mTimes;
        public int mUpThreshold;

        public Strategy(Spline spline, int times, int downThreshold, int upThreshold) {
            TemperatureController.this = r1;
            this.mSpline = spline;
            this.mTimes = times;
            this.mDownThreshold = downThreshold;
            this.mUpThreshold = upThreshold;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class SettingsObserver extends ContentObserver {
        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public SettingsObserver(Handler handler) {
            super(handler);
            TemperatureController.this = r1;
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            char c;
            String lastPathSegment = uri.getLastPathSegment();
            boolean z = false;
            switch (lastPathSegment.hashCode()) {
                case 800908829:
                    if (lastPathSegment.equals("POWER_PERFORMANCE_MODE_OPEN")) {
                        c = 0;
                        break;
                    }
                default:
                    c = 65535;
                    break;
            }
            switch (c) {
                case 0:
                    TemperatureController temperatureController = TemperatureController.this;
                    if (Settings.System.getInt(temperatureController.mContext.getContentResolver(), "POWER_PERFORMANCE_MODE_OPEN", 0) == 1) {
                        z = true;
                    }
                    temperatureController.mPowerPerformanceMode = z;
                    return;
                default:
                    return;
            }
        }
    }
}
