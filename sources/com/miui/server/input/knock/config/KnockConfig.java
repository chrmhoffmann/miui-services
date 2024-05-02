package com.miui.server.input.knock.config;

import android.graphics.Rect;
import java.util.List;
/* loaded from: classes.dex */
public class KnockConfig {
    public List<String> deviceName;
    public int deviceProperty;
    public int[] deviceX;
    public int displayVersion;
    public Rect knockRegion;
    public float knockScoreThreshold;
    public String localAlgorithmPath;
    public float quickMoveSpeed;
    public int[] sensorThreshold;
    public int useFrame;

    public String toString() {
        int[] iArr;
        String knockConfig = "KnockConfig{displayVersion=" + this.displayVersion + ", deviceProperty=" + this.deviceProperty + ", localAlgorithmPath='" + this.localAlgorithmPath + "', knockRegion=" + this.knockRegion + ", knockScoreThreshold=" + this.knockScoreThreshold + ", deviceName='" + this.deviceName + "', useFrame = " + this.useFrame + ", quickMoveSpeed = " + this.quickMoveSpeed + ", sensorThreshold = [";
        for (int sensor : this.sensorThreshold) {
            knockConfig = knockConfig + sensor + ",";
        }
        return knockConfig + "]}";
    }
}
