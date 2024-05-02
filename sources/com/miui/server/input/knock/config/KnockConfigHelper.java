package com.miui.server.input.knock.config;

import android.graphics.Rect;
import android.text.TextUtils;
import android.util.Slog;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import libcore.io.IoUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
/* loaded from: classes.dex */
public class KnockConfigHelper {
    private static final String ConfigFilePath = "/vendor/etc/knock-config.json";
    private static final String TAG = "KnockConfigHelper";
    private static volatile KnockConfigHelper sCloudConfigHelper = null;
    private static final List<KnockConfig> mConfigList = new ArrayList();

    private KnockConfigHelper() {
    }

    public static KnockConfigHelper getInstance() {
        if (sCloudConfigHelper == null) {
            synchronized (KnockConfigHelper.class) {
                if (sCloudConfigHelper == null) {
                    sCloudConfigHelper = new KnockConfigHelper();
                }
            }
        }
        return sCloudConfigHelper;
    }

    public void initConfig() {
        parseKnockConfig();
    }

    private StringBuilder parseFile(File configFile) {
        if (configFile == null || !configFile.exists()) {
            return null;
        }
        StringBuilder stringBuilder = new StringBuilder();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(configFile));
            while (true) {
                String line = reader.readLine();
                if (line == null) {
                    return stringBuilder;
                }
                stringBuilder.append(line);
            }
        } catch (Exception e) {
            Slog.w(TAG, e);
            return null;
        } finally {
            IoUtils.closeQuietly(reader);
        }
    }

    private void parseKnockConfig() {
        StringBuilder stringBuilder = parseFile(new File(ConfigFilePath));
        if (stringBuilder == null) {
            return;
        }
        String fileContent = stringBuilder.toString();
        if (TextUtils.isEmpty(fileContent)) {
            return;
        }
        try {
            JSONObject knockConfigJson = new JSONObject(fileContent);
            JSONArray knockConfigArray = knockConfigJson.getJSONArray("knockConfigs");
            for (int i = 0; i < knockConfigArray.length(); i++) {
                JSONObject jsonObjectTemp = (JSONObject) knockConfigArray.get(i);
                KnockConfig knockConfig = new KnockConfig();
                knockConfig.displayVersion = jsonObjectTemp.getInt("displayVersion");
                knockConfig.deviceProperty = jsonObjectTemp.getInt("deviceProperty");
                knockConfig.localAlgorithmPath = jsonObjectTemp.getString("localAlgorithmPath");
                knockConfig.knockRegion = new Rect(0, 0, 0, 0);
                String knockRegion = jsonObjectTemp.getString("knockRegion");
                if (!knockRegion.isEmpty()) {
                    String[] regionArray = knockRegion.split(",");
                    if (regionArray.length == 4) {
                        knockConfig.knockRegion.left = Integer.parseInt(regionArray[0]);
                        knockConfig.knockRegion.top = Integer.parseInt(regionArray[1]);
                        knockConfig.knockRegion.right = Integer.parseInt(regionArray[2]);
                        knockConfig.knockRegion.bottom = Integer.parseInt(regionArray[3]);
                    }
                }
                String deviceX = jsonObjectTemp.getString("deviceX");
                knockConfig.deviceX = new int[]{1, 1};
                if (!deviceX.isEmpty()) {
                    String[] deviceArray = deviceX.split(",");
                    if (deviceArray.length == 2) {
                        knockConfig.deviceX[0] = Integer.parseInt(deviceArray[0]);
                        knockConfig.deviceX[1] = Integer.parseInt(deviceArray[1]);
                    }
                }
                knockConfig.knockScoreThreshold = (float) jsonObjectTemp.getDouble("knockScoreThreshold");
                String deviceName = jsonObjectTemp.getString("deviceName");
                if (!TextUtils.isEmpty(deviceName)) {
                    knockConfig.deviceName = Arrays.asList(deviceName.split(","));
                }
                knockConfig.useFrame = jsonObjectTemp.getInt("useFrame");
                knockConfig.quickMoveSpeed = (float) jsonObjectTemp.getDouble("quickMoveSpeed");
                String sensorThreshold = jsonObjectTemp.getString("sensorThreshold");
                knockConfig.sensorThreshold = new int[]{0, 0, 0, 0};
                if (!sensorThreshold.isEmpty()) {
                    String[] sensorThresholdArray = sensorThreshold.split(",");
                    if (sensorThresholdArray.length == 4) {
                        knockConfig.sensorThreshold[0] = Integer.parseInt(sensorThresholdArray[0]);
                        knockConfig.sensorThreshold[1] = Integer.parseInt(sensorThresholdArray[1]);
                        knockConfig.sensorThreshold[2] = Integer.parseInt(sensorThresholdArray[2]);
                        knockConfig.sensorThreshold[3] = Integer.parseInt(sensorThresholdArray[3]);
                    }
                }
                mConfigList.add(knockConfig);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public boolean isDeviceSupport() {
        return mConfigList.size() > 0;
    }

    /* JADX WARN: Removed duplicated region for block: B:5:0x000d  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public com.miui.server.input.knock.config.KnockConfig getKnockConfig(java.util.List<com.miui.server.input.knock.config.filter.KnockConfigFilter> r6) {
        /*
            r5 = this;
            java.util.List<com.miui.server.input.knock.config.KnockConfig> r0 = com.miui.server.input.knock.config.KnockConfigHelper.mConfigList
            java.util.Iterator r1 = r6.iterator()
        L6:
            boolean r2 = r1.hasNext()
            r3 = 0
            if (r2 == 0) goto L22
            java.lang.Object r2 = r1.next()
            com.miui.server.input.knock.config.filter.KnockConfigFilter r2 = (com.miui.server.input.knock.config.filter.KnockConfigFilter) r2
            java.util.List r0 = r2.knockConfigFilter(r0)
            if (r0 == 0) goto L21
            int r4 = r0.size()
            if (r4 != 0) goto L20
            goto L21
        L20:
            goto L6
        L21:
            return r3
        L22:
            int r1 = r0.size()
            r2 = 1
            if (r1 != r2) goto L31
            r1 = 0
            java.lang.Object r1 = r0.get(r1)
            com.miui.server.input.knock.config.KnockConfig r1 = (com.miui.server.input.knock.config.KnockConfig) r1
            return r1
        L31:
            return r3
        */
        throw new UnsupportedOperationException("Method not decompiled: com.miui.server.input.knock.config.KnockConfigHelper.getKnockConfig(java.util.List):com.miui.server.input.knock.config.KnockConfig");
    }
}
