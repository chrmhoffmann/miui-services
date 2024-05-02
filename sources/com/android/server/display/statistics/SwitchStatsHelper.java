package com.android.server.display.statistics;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.SystemProperties;
import android.provider.MiuiSettings;
import android.provider.Settings;
import android.text.TextUtils;
import com.android.internal.os.BackgroundThread;
import com.android.server.app.GameManagerServiceStubImpl;
import com.android.server.display.statistics.BrightnessEvent;
import com.android.server.display.statistics.SwitchStatsHelper;
import java.util.ArrayList;
import java.util.List;
import miui.util.FeatureParser;
/* loaded from: classes.dex */
public class SwitchStatsHelper {
    public static final String DC_BACK_LIGHT_SWITCH = "dc_back_light";
    private static final String IS_SMART_FPS = "is_smart_fps";
    private static final String MIUI_SCREEN_COMPAT = "miui_screen_compat";
    private static final boolean SUPPORT_RESOLUTION_SWITCH;
    private static final boolean SUPPORT_SMART_FPS = FeatureParser.getBoolean("support_smart_fps", false);
    public static final String USER_REFRESH_RATE = "user_refresh_rate";
    private static SwitchStatsHelper mInstance;
    private static int[] mScreenResolutionSupported;
    private boolean mAdaptiveSleepEnable;
    private boolean mAutoBrightnessSettingsEnable;
    private Context mContext;
    private boolean mDarkModeSettingsEnable;
    private boolean mDcBacklightSettingsEnable;
    private boolean mDozeAlwaysOn;
    private boolean mReadModeSettingsEnable;
    private int mRefreshRateFromDeviceFeature;
    private ContentResolver mResolver;
    private int mScreenColorLevel;
    private boolean mScreenCompat;
    private int mScreenOptimizeSettingsMode;
    private boolean mScreenTrueToneEnable;
    private boolean mSmartRefreshRateEnable;
    private boolean mSunlightSettingsEnable;
    private boolean mSupportAdaptiveSleep;
    private int mUserRefreshRate;
    private ArrayList<BrightnessEvent.SwitchStatEntry> mSwitchStats = new ArrayList<>();
    private Handler mBgHandler = new Handler(BackgroundThread.getHandler().getLooper());
    private SettingsObserver mSettingsObserver = new SettingsObserver(this.mBgHandler);

    static {
        boolean z = false;
        int[] intArray = FeatureParser.getIntArray("screen_resolution_supported");
        mScreenResolutionSupported = intArray;
        if (intArray != null && intArray.length > 1) {
            z = true;
        }
        SUPPORT_RESOLUTION_SWITCH = z;
    }

    public SwitchStatsHelper(Context context) {
        this.mContext = context;
        this.mResolver = this.mContext.getContentResolver();
        readConfigFromDeviceFeature();
        loadSmartSwitches();
        registerSettingsObserver();
    }

    public static SwitchStatsHelper getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new SwitchStatsHelper(context);
        }
        return mInstance;
    }

    private void loadSmartSwitches() {
        boolean z = false;
        if (SUPPORT_SMART_FPS) {
            boolean z2 = Settings.System.getInt(this.mResolver, IS_SMART_FPS, -1) == -1;
            this.mSmartRefreshRateEnable = z2;
            if (z2) {
                Settings.System.putIntForUser(this.mResolver, IS_SMART_FPS, 1, -2);
            }
        }
        if (SUPPORT_RESOLUTION_SWITCH) {
            if (Settings.System.getInt(this.mResolver, MIUI_SCREEN_COMPAT, -1) == -1) {
                z = true;
            }
            this.mScreenCompat = z;
            if (z) {
                Settings.System.putIntForUser(this.mResolver, MIUI_SCREEN_COMPAT, 1, -2);
            }
        }
    }

    protected void registerSettingsObserver() {
        this.mResolver.registerContentObserver(Settings.System.getUriFor("screen_brightness_mode"), false, this.mSettingsObserver, -1);
        this.mResolver.registerContentObserver(Settings.System.getUriFor("sunlight_mode"), false, this.mSettingsObserver, -1);
        this.mResolver.registerContentObserver(Settings.System.getUriFor("screen_optimize_mode"), false, this.mSettingsObserver, -1);
        this.mResolver.registerContentObserver(Settings.System.getUriFor(DC_BACK_LIGHT_SWITCH), false, this.mSettingsObserver, -1);
        this.mResolver.registerContentObserver(Settings.Secure.getUriFor("ui_night_mode"), false, this.mSettingsObserver, -1);
        this.mResolver.registerContentObserver(Settings.System.getUriFor("screen_color_level"), false, this.mSettingsObserver, -1);
        this.mResolver.registerContentObserver(Settings.System.getUriFor("screen_paper_mode_enabled"), false, this.mSettingsObserver, -1);
        this.mResolver.registerContentObserver(Settings.System.getUriFor(USER_REFRESH_RATE), false, this.mSettingsObserver, -1);
        this.mResolver.registerContentObserver(Settings.System.getUriFor(IS_SMART_FPS), false, this.mSettingsObserver, -1);
        this.mResolver.registerContentObserver(Settings.Secure.getUriFor("doze_always_on"), false, this.mSettingsObserver, -1);
        this.mResolver.registerContentObserver(Settings.System.getUriFor("screen_true_tone"), false, this.mSettingsObserver, -1);
        this.mResolver.registerContentObserver(Settings.System.getUriFor(MIUI_SCREEN_COMPAT), false, this.mSettingsObserver, -1);
        this.mResolver.registerContentObserver(Settings.Secure.getUriFor("adaptive_sleep"), false, this.mSettingsObserver, -1);
        loadSettings();
    }

    private void loadSettings() {
        boolean z = false;
        this.mDcBacklightSettingsEnable = Settings.System.getIntForUser(this.mResolver, DC_BACK_LIGHT_SWITCH, -1, -2) == 1;
        this.mDarkModeSettingsEnable = Settings.Secure.getIntForUser(this.mResolver, "ui_night_mode", -1, -2) == 2;
        this.mSunlightSettingsEnable = Settings.System.getIntForUser(this.mResolver, "sunlight_mode", 0, -2) == 1;
        this.mAutoBrightnessSettingsEnable = Settings.System.getIntForUser(this.mResolver, "screen_brightness_mode", 0, -2) == 1;
        this.mScreenOptimizeSettingsMode = Settings.System.getIntForUser(this.mResolver, "screen_optimize_mode", MiuiSettings.ScreenEffect.DEFAULT_SCREEN_OPTIMIZE_MODE, -2);
        this.mReadModeSettingsEnable = Settings.System.getIntForUser(this.mResolver, "screen_paper_mode_enabled", 0, -2) != 0;
        this.mScreenColorLevel = Settings.System.getIntForUser(this.mResolver, "screen_color_level", -1, -2);
        this.mUserRefreshRate = Settings.System.getIntForUser(this.mResolver, USER_REFRESH_RATE, -1, -2);
        this.mSmartRefreshRateEnable = Settings.System.getIntForUser(this.mResolver, IS_SMART_FPS, -1, -2) == 1;
        this.mDozeAlwaysOn = Settings.Secure.getIntForUser(this.mResolver, "doze_always_on", -1, -2) == 1;
        this.mScreenTrueToneEnable = Settings.System.getIntForUser(this.mResolver, "screen_true_tone", 0, -2) == 1;
        this.mScreenCompat = Settings.System.getIntForUser(this.mResolver, MIUI_SCREEN_COMPAT, -1, -2) == 1;
        if (Settings.System.getIntForUser(this.mResolver, "adaptive_sleep", -1, -2) == 1) {
            z = true;
        }
        this.mAdaptiveSleepEnable = z;
    }

    private void readConfigFromDeviceFeature() {
        this.mSupportAdaptiveSleep = this.mContext.getResources().getBoolean(17891343);
        this.mRefreshRateFromDeviceFeature = FeatureParser.getInteger("defaultFps", -1);
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    public void handleSettingsChangeEvent(Uri uri) {
        char c;
        String lastPathSegment = uri.getLastPathSegment();
        boolean z = true;
        switch (lastPathSegment.hashCode()) {
            case -1763718536:
                if (lastPathSegment.equals("sunlight_mode")) {
                    c = 2;
                    break;
                }
                c = 65535;
                break;
            case -1187891250:
                if (lastPathSegment.equals("adaptive_sleep")) {
                    c = '\f';
                    break;
                }
                c = 65535;
                break;
            case -1168424008:
                if (lastPathSegment.equals(USER_REFRESH_RATE)) {
                    c = 7;
                    break;
                }
                c = 65535;
                break;
            case -1111615120:
                if (lastPathSegment.equals("screen_true_tone")) {
                    c = '\n';
                    break;
                }
                c = 65535;
                break;
            case -879722010:
                if (lastPathSegment.equals(MIUI_SCREEN_COMPAT)) {
                    c = 11;
                    break;
                }
                c = 65535;
                break;
            case -693072130:
                if (lastPathSegment.equals("screen_brightness_mode")) {
                    c = 3;
                    break;
                }
                c = 65535;
                break;
            case -101820922:
                if (lastPathSegment.equals("doze_always_on")) {
                    c = '\t';
                    break;
                }
                c = 65535;
                break;
            case 140109694:
                if (lastPathSegment.equals(DC_BACK_LIGHT_SWITCH)) {
                    c = 0;
                    break;
                }
                c = 65535;
                break;
            case 671593557:
                if (lastPathSegment.equals("screen_color_level")) {
                    c = 6;
                    break;
                }
                c = 65535;
                break;
            case 1186889717:
                if (lastPathSegment.equals("ui_night_mode")) {
                    c = 1;
                    break;
                }
                c = 65535;
                break;
            case 1540120734:
                if (lastPathSegment.equals(IS_SMART_FPS)) {
                    c = '\b';
                    break;
                }
                c = 65535;
                break;
            case 1962624818:
                if (lastPathSegment.equals("screen_optimize_mode")) {
                    c = 4;
                    break;
                }
                c = 65535;
                break;
            case 2119453483:
                if (lastPathSegment.equals("screen_paper_mode_enabled")) {
                    c = 5;
                    break;
                }
                c = 65535;
                break;
            default:
                c = 65535;
                break;
        }
        switch (c) {
            case 0:
                if (Settings.System.getIntForUser(this.mResolver, DC_BACK_LIGHT_SWITCH, -1, -2) != 1) {
                    z = false;
                }
                this.mDcBacklightSettingsEnable = z;
                return;
            case 1:
                if (Settings.Secure.getIntForUser(this.mResolver, "ui_night_mode", -1, -2) != 2) {
                    z = false;
                }
                this.mDarkModeSettingsEnable = z;
                return;
            case 2:
                if (Settings.System.getIntForUser(this.mResolver, "sunlight_mode", 0, -2) != 1) {
                    z = false;
                }
                this.mSunlightSettingsEnable = z;
                return;
            case 3:
                if (Settings.System.getIntForUser(this.mResolver, "screen_brightness_mode", 0, -2) != 1) {
                    z = false;
                }
                this.mAutoBrightnessSettingsEnable = z;
                return;
            case 4:
                this.mScreenOptimizeSettingsMode = Settings.System.getIntForUser(this.mResolver, "screen_optimize_mode", MiuiSettings.ScreenEffect.DEFAULT_SCREEN_OPTIMIZE_MODE, -2);
                return;
            case 5:
                if (Settings.System.getIntForUser(this.mResolver, "screen_paper_mode_enabled", 0, -2) == 0) {
                    z = false;
                }
                this.mReadModeSettingsEnable = z;
                return;
            case 6:
                this.mScreenColorLevel = Settings.System.getIntForUser(this.mResolver, "screen_color_level", -1, -2);
                return;
            case 7:
                this.mUserRefreshRate = Settings.System.getIntForUser(this.mResolver, USER_REFRESH_RATE, -1, -2);
                return;
            case '\b':
                if (Settings.System.getIntForUser(this.mResolver, IS_SMART_FPS, -1, -2) != 1) {
                    z = false;
                }
                this.mSmartRefreshRateEnable = z;
                return;
            case '\t':
                if (Settings.Secure.getIntForUser(this.mResolver, "doze_always_on", -1, -2) != 1) {
                    z = false;
                }
                this.mDozeAlwaysOn = z;
                return;
            case '\n':
                if (Settings.System.getIntForUser(this.mResolver, "screen_true_tone", 0, -2) != 1) {
                    z = false;
                }
                this.mScreenTrueToneEnable = z;
                return;
            case 11:
                if (Settings.System.getIntForUser(this.mResolver, MIUI_SCREEN_COMPAT, -1, -2) != 1) {
                    z = false;
                }
                this.mScreenCompat = z;
                return;
            case '\f':
                if (Settings.Secure.getIntForUser(this.mResolver, "adaptive_sleep", -1, -2) != 1) {
                    z = false;
                }
                this.mAdaptiveSleepEnable = z;
                return;
            default:
                return;
        }
    }

    public boolean isDcBacklightSettingsEnable() {
        return this.mDcBacklightSettingsEnable;
    }

    public boolean isSunlightSettingsEnable() {
        return this.mSunlightSettingsEnable;
    }

    public boolean isDarkModeSettingsEnable() {
        return this.mDarkModeSettingsEnable;
    }

    public boolean isAutoBrightnessSettingsEnable() {
        return this.mAutoBrightnessSettingsEnable;
    }

    public boolean isReadModeSettingsEnable() {
        return this.mReadModeSettingsEnable;
    }

    public int getScreenOptimizeSettingsMode() {
        return this.mScreenOptimizeSettingsMode;
    }

    public List<BrightnessEvent.SwitchStatEntry> getAllSwitchStats() {
        updateSwitchStatsValue();
        return this.mSwitchStats;
    }

    public void updateSwitchStatsValue() {
        this.mSwitchStats.clear();
        BrightnessEvent.SwitchStatEntry statEvent = new BrightnessEvent.SwitchStatEntry(0, "screen_brightness_mode", this.mAutoBrightnessSettingsEnable);
        this.mSwitchStats.add(statEvent);
        BrightnessEvent.SwitchStatEntry statEvent2 = new BrightnessEvent.SwitchStatEntry(0, DC_BACK_LIGHT_SWITCH, this.mDcBacklightSettingsEnable);
        this.mSwitchStats.add(statEvent2);
        BrightnessEvent.SwitchStatEntry statEvent3 = new BrightnessEvent.SwitchStatEntry(0, "ui_night_mode", this.mDarkModeSettingsEnable);
        this.mSwitchStats.add(statEvent3);
        BrightnessEvent.SwitchStatEntry statEvent4 = new BrightnessEvent.SwitchStatEntry(0, "sunlight_mode", this.mSunlightSettingsEnable && !this.mAutoBrightnessSettingsEnable);
        this.mSwitchStats.add(statEvent4);
        BrightnessEvent.SwitchStatEntry statEvent5 = new BrightnessEvent.SwitchStatEntry(1, "screen_optimize_mode", this.mScreenOptimizeSettingsMode);
        this.mSwitchStats.add(statEvent5);
        BrightnessEvent.SwitchStatEntry statEvent6 = new BrightnessEvent.SwitchStatEntry(0, "screen_paper_mode_enabled", this.mReadModeSettingsEnable);
        this.mSwitchStats.add(statEvent6);
        BrightnessEvent.SwitchStatEntry statEvent7 = new BrightnessEvent.SwitchStatEntry(1, "screen_color_level", getColorLevelCode(this.mScreenColorLevel));
        this.mSwitchStats.add(statEvent7);
        BrightnessEvent.SwitchStatEntry statEvent8 = new BrightnessEvent.SwitchStatEntry(1, "dynamic_refresh_rate", getCurrentRefreshRate());
        this.mSwitchStats.add(statEvent8);
        BrightnessEvent.SwitchStatEntry statEvent9 = new BrightnessEvent.SwitchStatEntry(0, "doze_always_on", this.mDozeAlwaysOn);
        this.mSwitchStats.add(statEvent9);
        BrightnessEvent.SwitchStatEntry statEvent10 = new BrightnessEvent.SwitchStatEntry(0, "screen_true_tone", this.mScreenTrueToneEnable);
        this.mSwitchStats.add(statEvent10);
        BrightnessEvent.SwitchStatEntry statEvent11 = new BrightnessEvent.SwitchStatEntry(1, "screen_resolution", getCurrentScreenResolution());
        this.mSwitchStats.add(statEvent11);
        BrightnessEvent.SwitchStatEntry statEvent12 = new BrightnessEvent.SwitchStatEntry(0, MIUI_SCREEN_COMPAT, getCurrentScreenCompat());
        this.mSwitchStats.add(statEvent12);
        if (this.mSupportAdaptiveSleep) {
            BrightnessEvent.SwitchStatEntry statEvent13 = new BrightnessEvent.SwitchStatEntry(0, "adaptive_sleep", this.mAdaptiveSleepEnable);
            this.mSwitchStats.add(statEvent13);
        }
        if (SystemProperties.getInt("persist.sys.muiltdisplay_type", 0) == 2) {
            BrightnessEvent.SwitchStatEntry statEvent14 = new BrightnessEvent.SwitchStatEntry(1, "close_lid_display_setting", getCurrentCloseLidDisplaySetting());
            this.mSwitchStats.add(statEvent14);
        }
    }

    private int getCurrentRefreshRate() {
        if (SUPPORT_SMART_FPS && this.mSmartRefreshRateEnable) {
            return -120;
        }
        boolean defaultFps = SystemProperties.getBoolean("ro.vendor.fps.switch.default", false);
        if (!defaultFps) {
            return SystemProperties.getInt("persist.vendor.dfps.level", -1);
        }
        int i = this.mUserRefreshRate;
        return i != -1 ? i : this.mRefreshRateFromDeviceFeature;
    }

    private int getCurrentScreenResolution() {
        String screenResolution = SystemProperties.get(GameManagerServiceStubImpl.MIUI_RESOLUTION, (String) null);
        if (!TextUtils.isEmpty(screenResolution)) {
            return Integer.valueOf(screenResolution.split(",")[0]).intValue();
        }
        return 1440;
    }

    private int getCurrentCloseLidDisplaySetting() {
        return Settings.System.getInt(this.mContext.getContentResolver(), "close_lid_display_setting", 1);
    }

    private boolean getCurrentScreenCompat() {
        boolean isQhd = false;
        if (SUPPORT_RESOLUTION_SWITCH) {
            int currentScreenResolution = getCurrentScreenResolution();
            int[] iArr = mScreenResolutionSupported;
            isQhd = currentScreenResolution == Math.max(iArr[0], iArr[1]);
        }
        return isQhd && this.mScreenCompat;
    }

    private int getColorLevelCode(int mScreenColorLevel) {
        int validRGB = 16777215 & mScreenColorLevel;
        int value_R = validRGB >> 16;
        int value_G = (validRGB >> 8) & 255;
        int value_B = validRGB & 255;
        if (mScreenColorLevel != -1) {
            if (value_R == value_G && value_G == value_B) {
                return 0;
            }
            return value_R > value_G ? value_R > value_B ? 1 : 3 : value_B < value_G ? 2 : 3;
        }
        return 0;
    }

    /* loaded from: classes.dex */
    public final class SettingsObserver extends ContentObserver {
        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public SettingsObserver(Handler handler) {
            super(handler);
            SwitchStatsHelper.this = r1;
        }

        /* renamed from: lambda$onChange$0$com-android-server-display-statistics-SwitchStatsHelper$SettingsObserver */
        public /* synthetic */ void m754x307bd2af(Uri uri) {
            SwitchStatsHelper.this.handleSettingsChangeEvent(uri);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, final Uri uri) {
            SwitchStatsHelper.this.mBgHandler.post(new Runnable() { // from class: com.android.server.display.statistics.SwitchStatsHelper$SettingsObserver$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    SwitchStatsHelper.SettingsObserver.this.m754x307bd2af(uri);
                }
            });
        }
    }
}
