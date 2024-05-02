package com.android.server.camera;

import android.os.Process;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Slog;
import com.android.server.display.TemperatureController;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
/* loaded from: classes.dex */
public class CameraJsonParser {
    private static final boolean CAM_BOOST_DEBUG = SystemProperties.getBoolean("persist.sys.miui.camera.boost.debug", false);
    private static final String CONFIG_JSON_DEF_PATH = "/system_ext/etc/camerabooster.json";
    private static final String CONFIG_JSON_PATH = "/odm/etc/camera/camerabooster.json";
    private static final String KILL_PERCEPTIBLE_LIST = "perceptible_list";
    private static final String TAG = "CameraJsonParser";
    private static final int TYPE_BOOLEAN = 1;
    private static final int TYPE_LONG = 3;
    private static final int TYPE_STRING = 2;
    private Map<String, List<String>> mListConfig = new ConcurrentHashMap();
    private Map<String, Boolean> mSupportConfig = new ConcurrentHashMap();
    private Map<String, String> mOomConfig = new ConcurrentHashMap();
    private Map<String, Long> mThresholdConfig = new ConcurrentHashMap();
    private Map<String, Long> mPerceptKillConfig = new ConcurrentHashMap();
    private double mCloudVersion = 1.0d;
    private final Object mConfigLock = new Object();

    public CameraJsonParser() {
        parseJson();
        parsePerceptibleConfig();
        dumpJsonInfo();
    }

    public void updateCameraBoosterCloudData(double version, String jsonStr) {
        if (this.mCloudVersion >= version) {
            Slog.d(TAG, "there is no need to update cloud data, " + this.mCloudVersion + " >= " + version);
            return;
        }
        this.mCloudVersion = version;
        beginConfig(jsonStr);
        dumpJsonInfo();
    }

    public List<String> getConfigList(String key) {
        synchronized (this.mConfigLock) {
            if (this.mListConfig.containsKey(key)) {
                return this.mListConfig.get(key);
            }
            return new ArrayList();
        }
    }

    public boolean getSupportValue(String key) {
        synchronized (this.mConfigLock) {
            if (this.mSupportConfig.containsKey(key)) {
                return this.mSupportConfig.get(key).booleanValue();
            }
            return false;
        }
    }

    public long getThresholdValue(String key, long defValue) {
        synchronized (this.mConfigLock) {
            if (this.mThresholdConfig.containsKey(key)) {
                return this.mThresholdConfig.get(key).longValue();
            }
            return defValue;
        }
    }

    public String getOomValue(String key, String defValue) {
        synchronized (this.mConfigLock) {
            if (this.mOomConfig.containsKey(key)) {
                return this.mOomConfig.get(key);
            }
            return defValue;
        }
    }

    public long getPerceptibleKillValue(String key) {
        synchronized (this.mConfigLock) {
            if (this.mPerceptKillConfig.containsKey(key)) {
                return this.mPerceptKillConfig.get(key).longValue();
            }
            return 0L;
        }
    }

    private void parsePerceptibleConfig() {
        List<String> list = getConfigList(KILL_PERCEPTIBLE_LIST);
        List<String> perceptibleList = new ArrayList<>();
        if (list == null || list.size() == 0) {
            return;
        }
        for (String config : list) {
            int index = 0;
            String processName = null;
            long threshold = 0;
            StringTokenizer st = new StringTokenizer(config, "-");
            while (st.hasMoreTokens()) {
                String value = st.nextToken();
                if (!TextUtils.isEmpty(value)) {
                    if (index == 0) {
                        processName = value;
                    } else if (index == 1) {
                        threshold = Integer.parseInt(value);
                    }
                    index++;
                }
            }
            if (!TextUtils.isEmpty(processName)) {
                perceptibleList.add(processName);
                if (threshold > 0) {
                    this.mPerceptKillConfig.put(processName, Long.valueOf(threshold));
                }
            }
        }
        this.mListConfig.put(KILL_PERCEPTIBLE_LIST, perceptibleList);
    }

    private void beginConfig(String content) {
        if (content == null || TextUtils.isEmpty(content)) {
            Slog.w(TAG, "json file not found or read fail!");
            return;
        }
        synchronized (this.mConfigLock) {
            parseConfigListLocked(content, "");
            parseOtherLocked(content, "");
            long TotalMemMb = Process.getTotalMemory() / 1048576;
            if (TotalMemMb < 6144) {
                parseConfigListLocked(content, "_lowmem");
                parseOtherLocked(content, "_lowmem");
            }
            String area = SystemProperties.get("ro.miui.build.region", "cn");
            String tag = "_" + area;
            parseConfigListLocked(content, tag);
            parseOtherLocked(content, tag);
        }
    }

    private void parseJson() {
        if (checkFile(CONFIG_JSON_DEF_PATH)) {
            Slog.i(TAG, "the default json file path is : /system_ext/etc/camerabooster.json");
            String content = readJSONFileToString(CONFIG_JSON_DEF_PATH);
            beginConfig(content);
        }
        if (checkFile(CONFIG_JSON_PATH)) {
            Slog.i(TAG, "the odm json file path is : /odm/etc/camera/camerabooster.json");
            String content2 = readJSONFileToString(CONFIG_JSON_PATH);
            beginConfig(content2);
        }
    }

    private boolean checkFile(String path) {
        if (TextUtils.isEmpty(path)) {
            return false;
        }
        File file = new File(path);
        return file.exists() && file.isFile() && file.canRead();
    }

    private String readJSONFileToString(String name) {
        final StringBuilder builder = new StringBuilder();
        try {
            Stream<String> stream = Files.lines(Paths.get(name, new String[0]), StandardCharsets.UTF_8);
            stream.forEach(new Consumer() { // from class: com.android.server.camera.CameraJsonParser$$ExternalSyntheticLambda0
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    builder.append((String) obj).append("\n");
                }
            });
            if (stream != null) {
                stream.close();
            }
        } catch (IOException e) {
            Slog.e(TAG, "IOException");
        }
        return builder.toString();
    }

    private void parseConfigListLocked(String content, String endTag) {
        try {
            String tag = "configs" + endTag;
            JSONObject jsonObject = new JSONObject(content);
            if (jsonObject.has(tag)) {
                JSONArray arrays = jsonObject.optJSONArray(tag);
                for (int i = 0; i < arrays.length(); i++) {
                    JSONObject obj = arrays.optJSONObject(i);
                    if (obj != null) {
                        String name = obj.optString(TemperatureController.STRATEGY_NAME);
                        if (obj.has("config")) {
                            JSONArray values = obj.optJSONArray("config");
                            List<String> list = new ArrayList<>();
                            for (int j = 0; j < values.length(); j++) {
                                String pkg = values.getString(j);
                                list.add(pkg);
                            }
                            this.mListConfig.put(name, list);
                        }
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void parseOtherInternal(String tag, int type, JSONObject jsonObject) {
        if (jsonObject.has(tag)) {
            JSONObject obj = jsonObject.optJSONObject(tag);
            Iterator it = obj.keys();
            while (it.hasNext()) {
                String key = it.next();
                if (1 == type) {
                    boolean value = obj.optBoolean(key);
                    this.mSupportConfig.put(key, Boolean.valueOf(value));
                } else if (2 == type) {
                    String value2 = obj.optString(key);
                    this.mOomConfig.put(key, value2);
                } else if (3 == type) {
                    Long value3 = Long.valueOf(obj.optLong(key));
                    this.mThresholdConfig.put(key, value3);
                }
            }
        }
    }

    private void parseOtherLocked(String content, String endTag) {
        try {
            JSONObject obj = new JSONObject(content);
            String tag = "support" + endTag;
            parseOtherInternal(tag, 1, obj);
            String tag2 = "oom" + endTag;
            parseOtherInternal(tag2, 2, obj);
            String tag3 = "threshold" + endTag;
            parseOtherInternal(tag3, 3, obj);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void dumpJsonInfo() {
        if (CAM_BOOST_DEBUG) {
            for (Map.Entry<String, List<String>> item : this.mListConfig.entrySet()) {
                String name = item.getKey();
                List<String> list = item.getValue();
                Slog.i(TAG, "name : " + name);
                for (int i = 0; i < list.size(); i++) {
                    Slog.i(TAG, "value : " + list.get(i));
                }
            }
            for (Map.Entry<String, Boolean> item2 : this.mSupportConfig.entrySet()) {
                String key = item2.getKey();
                boolean value = item2.getValue().booleanValue();
                Slog.i(TAG, "key : " + key + ", value : " + value);
            }
            for (Map.Entry<String, String> item3 : this.mOomConfig.entrySet()) {
                String key2 = item3.getKey();
                String value2 = item3.getValue();
                Slog.i(TAG, "key : " + key2 + ", value : " + value2);
            }
            for (Map.Entry<String, Long> item4 : this.mThresholdConfig.entrySet()) {
                String key3 = item4.getKey();
                Long value3 = item4.getValue();
                Slog.i(TAG, "key : " + key3 + ", value : " + value3);
            }
            for (Map.Entry<String, Long> item5 : this.mPerceptKillConfig.entrySet()) {
                String key4 = item5.getKey();
                Long value4 = item5.getValue();
                Slog.i(TAG, "key : " + key4 + ", value : " + value4);
            }
            Slog.i(TAG, "current cloud data version is: " + this.mCloudVersion);
        }
    }
}
