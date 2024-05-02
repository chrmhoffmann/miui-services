package com.android.server;
/* loaded from: classes.dex */
public class DarkModeAppSettingsInfo {
    public static final int INTERCEPT_RELAUNCH_ALWAYS = 1;
    public static final int INTERCEPT_RELAUNCH_NEVER = 2;
    public static final int INTERCEPT_RELAUNCH_UNKNOWN = 0;
    public static final int OVERRIDE_ENABLE_CLOSE = 2;
    public static final int OVERRIDE_ENABLE_NO = 0;
    public static final int OVERRIDE_ENABLE_OPEN = 1;
    private int adaptStat;
    private boolean defaultEnable;
    private boolean forceDarkOrigin;
    private int forceDarkSplashScreen;
    private int interceptRelaunch;
    private int overrideEnableValue;
    private String packageName;
    private boolean showInSettings;

    public String getPackageName() {
        return this.packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public boolean isShowInSettings() {
        return this.showInSettings;
    }

    public void setShowInSettings(boolean showInSettings) {
        this.showInSettings = showInSettings;
    }

    public boolean isDefaultEnable() {
        return this.defaultEnable;
    }

    public void setDefaultEnable(boolean defaultEnable) {
        this.defaultEnable = defaultEnable;
    }

    public int getAdaptStat() {
        return this.adaptStat;
    }

    public void setAdaptStat(int adaptStat) {
        this.adaptStat = adaptStat;
    }

    public int getOverrideEnableValue() {
        return this.overrideEnableValue;
    }

    public void setOverrideEnableValue(int overrideEnableValue) {
        this.overrideEnableValue = overrideEnableValue;
    }

    public boolean isForceDarkOrigin() {
        return this.forceDarkOrigin;
    }

    public void setForceDarkOrigin(boolean forceDarkOrigin) {
        this.forceDarkOrigin = forceDarkOrigin;
    }

    public int getForceDarkSplashScreen() {
        return this.forceDarkSplashScreen;
    }

    public void setForceDarkSplashScreen(int forceDarkSplashScreen) {
        this.forceDarkSplashScreen = forceDarkSplashScreen;
    }

    public int getInterceptRelaunch() {
        return this.interceptRelaunch;
    }

    public void setInterceptRelaunch(int interceptRelaunch) {
        this.interceptRelaunch = interceptRelaunch;
    }

    public String toString() {
        return "DarkModeAppSettingsInfo{packageName='" + this.packageName + "', showInSettings=" + this.showInSettings + ", defaultEnable=" + this.defaultEnable + ", overrideEnableValue=" + this.overrideEnableValue + ", forceDarkOrigin=" + this.forceDarkOrigin + ", adaptStat=" + this.adaptStat + ", forceDarkSplashScreen=" + this.forceDarkSplashScreen + ", interceptRelaunch=" + this.interceptRelaunch + '}';
    }
}
