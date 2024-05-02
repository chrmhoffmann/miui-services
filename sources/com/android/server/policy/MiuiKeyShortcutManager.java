package com.android.server.policy;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.XmlResourceParser;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.MiuiSettings;
import android.provider.Settings;
import android.server.am.SplitScreenReporter;
import android.text.TextUtils;
import android.util.Slog;
import android.view.ViewConfiguration;
import com.android.internal.content.PackageMonitor;
import com.android.internal.util.XmlUtils;
import com.android.server.LocalServices;
import com.android.server.policy.MiuiShortcutObserver;
import com.miui.server.input.util.ShortCutActionsUtils;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import miui.os.Build;
import miui.util.FeatureParser;
/* loaded from: classes.dex */
public class MiuiKeyShortcutManager {
    public static final String ACTION_COTA_CARRIER = "com.android.updater.action.COTA_CARRIER";
    public static final String ACTION_KEYBOARD = "keyboard:";
    private static final String DEVICE_IRIS_KDDI = "XIG02";
    private static final String DEVICE_REGION_RUSSIA = "ru";
    public static final int DOUBLE_TAP_HOME_NOTHING = 0;
    public static final int DOUBLE_TAP_HOME_RECENT_PANEL = 1;
    public static final int FUNCTION_DEFAULT = -1;
    public static final int FUNCTION_DISABLE = 0;
    public static final int FUNCTION_ENABLE = 1;
    public static final String IS_CUSTOM_SHORTCUTS_EFFECTIVE = "is_custom_shortcut_effective";
    private static final String IS_PRODUCT_MOD_DEVICE = "ro.product.mod_device";
    private static final String KEY_IS_IN_MIUI_SOS_MODE = "key_is_in_miui_sos_mode";
    private static final String KEY_MIUI_SOS_ENABLE = "key_miui_sos_enable";
    private static final int KS_FEATURE_DEFAULT = 1;
    private static final long LONG_PRESS_POWER_KEY_TIMEOUT = 3000;
    private static final String MIUI_SETTINGS_PACKAGE = "com.android.settings";
    public static final String PACKAGE_SMART_HOME = "com.miui.smarthomeplus";
    private static final String TAG = "MiuiKeyShortcutManager";
    public static final int VOICE_ASSIST_GUIDE_DISABLE = 0;
    public static final int VOICE_ASSIST_GUIDE_ENABLE = 1;
    public static final int VOICE_ASSIST_GUIDE_MAX_COUNT = 2;
    private static volatile MiuiKeyShortcutManager sInstance;
    private boolean mAOSPAssistantLongPressHomeEnabled;
    private ConfigFileLoader mConfigFileLoader;
    private final ContentResolver mContentResolver;
    private final Context mContext;
    public int mFingerPrintNavCenterAction;
    private String mFivePressPowerLaunchGoogleSos;
    private boolean mFivePressPowerLaunchSos;
    private final Handler mHandler;
    private boolean mIsCtsMode;
    private boolean mIsOnPcMode;
    private boolean mIsOnSosMode;
    private final KeyCombinationManager mKeyCombinationManager;
    public String mLongPressMenuKeyWhenLock;
    public boolean mLongPressPowerKeyLaunchSmartHome;
    public boolean mLongPressPowerKeyLaunchXiaoAi;
    public int mLongPressPowerTime;
    private MiuiOtherSettingsObserver mMiuiOtherSettingsObserver;
    private PackageMonitor mPackageMonitor;
    public boolean mPressToAppSwitch;
    private final SingleKeyGestureDetector mSingleKeyGestureDetector;
    public boolean mSingleKeyUse;
    private final boolean mSupportGoogleSos;
    public boolean mVolumeKeyLaunchCamera;
    private static final boolean SUPPORT_EDGE_TOUCH_VOLUME = FeatureParser.getBoolean("support_edge_touch_volume", false);
    public static final boolean SUPPORT_GOOGLE_RSA_PROTOCOL = FeatureParser.getBoolean("support_google_rsa_protocol", false);
    public static final String CURRENT_DEVICE_REGION = SystemProperties.get("ro.miui.build.region", "CN");
    public boolean mEnableCustomShortcutKey = true;
    public int mDoubleTapOnHomeBehavior = 0;
    private final HashMap<String, MiuiCombinationRule> mCombinationRuleHashMap = new HashMap<>();
    private final HashMap<String, MiuiSingleKeyRule> mSingleKeyRuleHashMap = new HashMap<>();
    private final HashMap<String, MiuiGestureRule> mGestureRuleHashMap = new HashMap<>();
    public int mXiaoaiPowerGuideFlag = -1;
    public int mGoogleAssistantGuideFlag = -1;
    private int mRSAGuideStatus = -1;
    private final Set<Integer> mInitForUsers = new HashSet();
    BroadcastReceiver mCotaDeviceReceiver = new BroadcastReceiver() { // from class: com.android.server.policy.MiuiKeyShortcutManager.2
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            Slog.d(MiuiKeyShortcutManager.TAG, "receive cota carrier prop update");
            MiuiKeyShortcutManager.this.mMiuiOtherSettingsObserver.initObserver();
            ShortCutActionsUtils.getInstance(MiuiKeyShortcutManager.this.mContext).setCotaDeviceRegion();
        }
    };
    private WindowManagerPolicy mWindowManagerPolicy = (WindowManagerPolicy) LocalServices.getService(WindowManagerPolicy.class);
    private int mCurrentUserId = UserHandle.myUserId();

    public static MiuiKeyShortcutManager getInstance(Context context, Handler handler, KeyCombinationManager keyCombinationManager, SingleKeyGestureDetector singleKeyGestureDetector) {
        if (sInstance == null) {
            synchronized (MiuiKeyShortcutManager.class) {
                if (sInstance == null) {
                    sInstance = new MiuiKeyShortcutManager(context, handler, keyCombinationManager, singleKeyGestureDetector);
                }
            }
        }
        return sInstance;
    }

    private void registerPackageChangeReceivers() {
        PackageMonitor packageMonitor = new PackageMonitor() { // from class: com.android.server.policy.MiuiKeyShortcutManager.1
            public void onPackageDataCleared(String packageName, int uid) {
                if (MiuiKeyShortcutManager.MIUI_SETTINGS_PACKAGE.equals(packageName)) {
                    Slog.i(MiuiKeyShortcutManager.TAG, "onPackageDataCleared packageName=" + packageName + " uid=" + uid);
                    MiuiKeyShortcutManager.this.resetMiuiShortcutSettings();
                }
                super.onPackageDataCleared(packageName, uid);
            }
        };
        this.mPackageMonitor = packageMonitor;
        packageMonitor.register(this.mContext, (Looper) null, UserHandle.ALL, true);
    }

    public void resetMiuiShortcutSettings() {
        this.mCombinationRuleHashMap.forEach(new BiConsumer() { // from class: com.android.server.policy.MiuiKeyShortcutManager$$ExternalSyntheticLambda0
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                String str = (String) obj;
                ((MiuiCombinationRule) obj2).getObserver().onDestroy();
            }
        });
        this.mSingleKeyRuleHashMap.forEach(new BiConsumer() { // from class: com.android.server.policy.MiuiKeyShortcutManager$$ExternalSyntheticLambda1
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                String str = (String) obj;
                ((MiuiSingleKeyRule) obj2).getObserver().onDestroy();
            }
        });
        this.mGestureRuleHashMap.forEach(new BiConsumer() { // from class: com.android.server.policy.MiuiKeyShortcutManager$$ExternalSyntheticLambda2
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                String str = (String) obj;
                ((MiuiGestureRule) obj2).getObserver().onDestroy();
            }
        });
        this.mCombinationRuleHashMap.clear();
        this.mSingleKeyRuleHashMap.clear();
        this.mGestureRuleHashMap.clear();
        this.mConfigFileLoader.setResetMiuiShortcutSettings(true);
        this.mConfigFileLoader.start();
        resetOtherSettingsProvider();
        this.mMiuiOtherSettingsObserver.onChange(false);
    }

    private void resetOtherSettingsProvider() {
        Settings.System.putStringForUser(this.mContext.getContentResolver(), "long_press_power_launch_xiaoai", null, this.mCurrentUserId);
    }

    private MiuiKeyShortcutManager(Context context, Handler handler, KeyCombinationManager keyCombinationManager, SingleKeyGestureDetector singleKeyGestureDetector) {
        this.mHandler = handler;
        this.mContext = context;
        ContentResolver contentResolver = context.getContentResolver();
        this.mContentResolver = contentResolver;
        this.mKeyCombinationManager = keyCombinationManager;
        this.mSingleKeyGestureDetector = singleKeyGestureDetector;
        this.mSupportGoogleSos = context.getResources().getBoolean(17891624);
        MiuiOtherSettingsObserver miuiOtherSettingsObserver = new MiuiOtherSettingsObserver(handler);
        this.mMiuiOtherSettingsObserver = miuiOtherSettingsObserver;
        miuiOtherSettingsObserver.initObserver();
        ConfigFileLoader configFileLoader = new ConfigFileLoader();
        this.mConfigFileLoader = configFileLoader;
        configFileLoader.start();
        Settings.Secure.putInt(contentResolver, "support_gesture_shortcut_settings", 1);
        this.mLongPressPowerTime = context.getResources().getInteger(285933644);
        registerPackageChangeReceivers();
        registerOtherSettingsBroadcastReceiver();
        registerEnableCustomShortcutKeyObserver();
        updateEnableCustomShortcutFromSettings();
    }

    private void registerOtherSettingsBroadcastReceiver() {
        if (Build.IS_INTERNATIONAL_BUILD) {
            IntentFilter filterCotaDevice = new IntentFilter();
            filterCotaDevice.addAction(ACTION_COTA_CARRIER);
            this.mContext.registerReceiver(this.mCotaDeviceReceiver, filterCotaDevice);
        }
    }

    public boolean getEnableKsFeature() {
        return this.mEnableCustomShortcutKey;
    }

    private void registerEnableCustomShortcutKeyObserver() {
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(IS_CUSTOM_SHORTCUTS_EFFECTIVE), false, new ContentObserver(this.mHandler) { // from class: com.android.server.policy.MiuiKeyShortcutManager.3
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                MiuiKeyShortcutManager.this.updateEnableCustomShortcutFromSettings();
            }
        }, -2);
    }

    public void updateEnableCustomShortcutFromSettings() {
        boolean z = true;
        if (Settings.System.getIntForUser(this.mContext.getContentResolver(), IS_CUSTOM_SHORTCUTS_EFFECTIVE, 1, -2) != 1) {
            z = false;
        }
        this.mEnableCustomShortcutKey = z;
    }

    public boolean getVolumeKeyLaunchCamera() {
        return this.mVolumeKeyLaunchCamera;
    }

    public int getRSAGuideStatus() {
        return this.mRSAGuideStatus;
    }

    public boolean supportRSARegion() {
        return Build.IS_INTERNATIONAL_BUILD && !DEVICE_REGION_RUSSIA.equals(CURRENT_DEVICE_REGION);
    }

    public boolean supportAOSPTriggerFunction(int keyCode) {
        if (this.mIsCtsMode && 3 == keyCode) {
            return this.mAOSPAssistantLongPressHomeEnabled;
        }
        return true;
    }

    public void onUserSwitch(int currentUserId) {
        this.mCurrentUserId = currentUserId;
        for (Map.Entry<String, MiuiCombinationRule> entry : this.mCombinationRuleHashMap.entrySet()) {
            entry.getValue().getObserver().onChange(false);
        }
        for (Map.Entry<String, MiuiSingleKeyRule> entry2 : this.mSingleKeyRuleHashMap.entrySet()) {
            entry2.getValue().getObserver().onChange(false);
        }
        for (Map.Entry<String, MiuiGestureRule> entry3 : this.mGestureRuleHashMap.entrySet()) {
            entry3.getValue().getObserver().onChange(false);
        }
        this.mMiuiOtherSettingsObserver.onChange(false);
        if (!this.mInitForUsers.contains(Integer.valueOf(currentUserId))) {
            this.mConfigFileLoader.start();
        }
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    public boolean needDelayPowerKey() {
        char c;
        String function = getFunction("double_click_power_key");
        switch (function.hashCode()) {
            case -1534821982:
                if (function.equals("google_pay")) {
                    c = 2;
                    break;
                }
                c = 65535;
                break;
            case -1406946787:
                if (function.equals("au_pay")) {
                    c = 1;
                    break;
                }
                c = 65535;
                break;
            case -1074479227:
                if (function.equals("mi_pay")) {
                    c = 0;
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
            case 1:
            case 2:
                return true;
            default:
                return false;
        }
    }

    public String getFunction(String action) {
        String function = "none";
        if (!this.mIsOnPcMode || !"long_press_power_key".equals(action)) {
            if (this.mCombinationRuleHashMap.get(action) != null) {
                function = this.mCombinationRuleHashMap.get(action).getFunction();
            } else if (this.mSingleKeyRuleHashMap.get(action) != null) {
                function = this.mSingleKeyRuleHashMap.get(action).getFunction();
            }
            return function == null ? "none" : function;
        }
        return "none";
    }

    public void setLongPressPowerTime() {
        this.mSingleKeyGestureDetector.setLongPressTimeout(getPowerLongPressTimeOut());
    }

    public long getPowerLongPressTimeOut() {
        if (isEmptyLongPressPowerFunction() && isDisabledXiaoAiGuide() && isDisabledGoogleAssistantGuide()) {
            return ViewConfiguration.get(this.mContext).getDeviceGlobalActionKeyTimeout();
        }
        return 3000L;
    }

    private boolean isEmptyLongPressPowerFunction() {
        return TextUtils.isEmpty(getFunction("long_press_power_key")) || "none".equals(getFunction("long_press_power_key")) || !this.mWindowManagerPolicy.isUserSetupComplete();
    }

    private boolean isDisabledXiaoAiGuide() {
        return this.mIsOnPcMode || this.mXiaoaiPowerGuideFlag != 1 || !this.mWindowManagerPolicy.isUserSetupComplete();
    }

    public boolean isDisabledGoogleAssistantGuide() {
        return this.mGoogleAssistantGuideFlag != 1 || !this.mWindowManagerPolicy.isUserSetupComplete();
    }

    public boolean isSosCanBeTrigger() {
        return this.mFivePressPowerLaunchSos && !this.mIsOnSosMode;
    }

    public boolean isGoogleSosEnable() {
        String str;
        return this.mSupportGoogleSos && ((str = this.mFivePressPowerLaunchGoogleSos) == null || SplitScreenReporter.ACTION_ENTER_SPLIT.equals(str));
    }

    public boolean isSoftBankCustom() {
        return "lilac_jp_sb_global".equals(SystemProperties.get(IS_PRODUCT_MOD_DEVICE, "")) && "jp_sb".equals(SystemProperties.get("ro.miui.customized.region", ""));
    }

    public boolean isCtsMode() {
        return this.mIsCtsMode;
    }

    /* loaded from: classes.dex */
    public class ConfigFileLoader implements MiuiShortcutObserver.MiuiShortcutListener {
        static final String ATTRIBUTE_ACTION = "action";
        static final String ATTRIBUTE_COMBATIONKEY = "combationkey";
        static final String ATTRIBUTE_FUNCTION = "function";
        static final String ATTRIBUTE_ISGLOBAL = "isglobal";
        static final String ATTRIBUTE_PRIMARYKEY = "primarykey";
        static final String ATTRIBUTE_TYPE = "type";
        static final String CONFIG_FILE_NAME = "miuishortcutkeymap";
        static final String CONFIG_FILE_NAME_EXTRA = "miuishortcutkeymap_extra";
        static final int REGION_ALL = 3;
        static final int REGION_CN = 0;
        static final int REGION_GLOBAL = 1;
        static final String TAG_SHORTCUT = "shortcutkey";
        static final String TAG_SHORTCUTS = "shortcutkeys";
        static final String TYPE_COMBINATION = "combination";
        static final String TYPE_DOUBLECLICK = "doubleclick";
        static final String TYPE_GESTURE = "gesture";
        static final String TYPE_LONGPRESS = "longpress";
        private boolean mResetMiuiShortcutSettings;

        ConfigFileLoader() {
            MiuiKeyShortcutManager.this = this$0;
        }

        void start() {
            loadShortcutkeys(CONFIG_FILE_NAME_EXTRA);
            loadShortcutkeys(CONFIG_FILE_NAME);
            MiuiKeyShortcutManager.this.mInitForUsers.add(Integer.valueOf(MiuiKeyShortcutManager.this.mCurrentUserId));
            this.mResetMiuiShortcutSettings = false;
        }

        /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
        void loadShortcutkeys(String fileName) {
            try {
                int resId = MiuiKeyShortcutManager.this.mContext.getResources().getIdentifier(fileName, "xml", "android.miui");
                if (resId == 0) {
                    Slog.i(MiuiKeyShortcutManager.TAG, "resId = 0, file = " + fileName);
                    return;
                }
                XmlResourceParser xmlResourceParser = MiuiKeyShortcutManager.this.mContext.getResources().getXml(resId);
                if (xmlResourceParser == null) {
                    Slog.i(MiuiKeyShortcutManager.TAG, "xmlResourceParser is null, file = " + fileName);
                    return;
                }
                Slog.i(MiuiKeyShortcutManager.TAG, "xmlResourceParser is not null, file = " + fileName);
                XmlUtils.beginDocument(xmlResourceParser, TAG_SHORTCUTS);
                while (true) {
                    XmlUtils.nextElement(xmlResourceParser);
                    char c = 3;
                    if (xmlResourceParser.getEventType() != 3 && TAG_SHORTCUT.equals(xmlResourceParser.getName())) {
                        String primaryKey = xmlResourceParser.getAttributeValue(null, ATTRIBUTE_PRIMARYKEY);
                        String combationKey = xmlResourceParser.getAttributeValue(null, ATTRIBUTE_COMBATIONKEY);
                        String action = xmlResourceParser.getAttributeValue(null, "action");
                        String function = xmlResourceParser.getAttributeValue(null, ATTRIBUTE_FUNCTION);
                        String isGlobal = xmlResourceParser.getAttributeValue(null, ATTRIBUTE_ISGLOBAL);
                        String type = xmlResourceParser.getAttributeValue(null, "type");
                        switch (type.hashCode()) {
                            case -1614281641:
                                if (type.equals(TYPE_DOUBLECLICK)) {
                                    c = 2;
                                    break;
                                }
                                c = 65535;
                                break;
                            case -649053489:
                                if (type.equals(TYPE_COMBINATION)) {
                                    c = 0;
                                    break;
                                }
                                c = 65535;
                                break;
                            case -75080375:
                                if (type.equals(TYPE_GESTURE)) {
                                    break;
                                }
                                c = 65535;
                                break;
                            case 143756103:
                                if (type.equals(TYPE_LONGPRESS)) {
                                    c = 1;
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
                                if (MiuiKeyShortcutManager.this.mCombinationRuleHashMap.get(action) == null && MiuiKeyShortcutManager.this.mHandler != null) {
                                    initCombinationRule(primaryKey, combationKey, action);
                                }
                                setDefaultFunction(((MiuiCombinationRule) MiuiKeyShortcutManager.this.mCombinationRuleHashMap.get(action)).getFunction(), action, function, isGlobal);
                                break;
                            case 1:
                                if (MiuiKeyShortcutManager.this.mSingleKeyRuleHashMap.get(action) == null && MiuiKeyShortcutManager.this.mHandler != null) {
                                    initSingleKeyLongPress(primaryKey, action);
                                }
                                setDefaultFunction(((MiuiSingleKeyRule) MiuiKeyShortcutManager.this.mSingleKeyRuleHashMap.get(action)).getFunction(), action, function, isGlobal);
                                break;
                            case 2:
                                if (MiuiKeyShortcutManager.this.mSingleKeyRuleHashMap.get(action) == null && MiuiKeyShortcutManager.this.mHandler != null) {
                                    initSingleKeyMultiPress(primaryKey, action);
                                }
                                setDefaultFunction(((MiuiSingleKeyRule) MiuiKeyShortcutManager.this.mSingleKeyRuleHashMap.get(action)).getFunction(), action, function, isGlobal);
                                break;
                            case 3:
                                if (MiuiKeyShortcutManager.this.mGestureRuleHashMap.get(action) == null) {
                                    MiuiGestureRule miuiGestureRule = new MiuiGestureRule(MiuiKeyShortcutManager.this.mHandler, MiuiKeyShortcutManager.this.mContentResolver, action);
                                    MiuiKeyShortcutManager.this.mGestureRuleHashMap.put(action, miuiGestureRule);
                                    miuiGestureRule.getObserver().registerShortcutListener(this);
                                }
                                setDefaultFunction(((MiuiGestureRule) MiuiKeyShortcutManager.this.mGestureRuleHashMap.get(action)).getFunction(), action, function, isGlobal);
                                break;
                        }
                    }
                }
                initOtherFunction();
                Slog.i(MiuiKeyShortcutManager.TAG, "Load Successed, mCombinationRules size = " + MiuiKeyShortcutManager.this.mCombinationRuleHashMap.size() + ", mSingleKeyRules size = " + MiuiKeyShortcutManager.this.mSingleKeyRuleHashMap.size() + ", mGestureRules size = " + MiuiKeyShortcutManager.this.mGestureRuleHashMap.size());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void initOtherFunction() {
            setDefaultFunction(MiuiKeyShortcutManager.this.mFivePressPowerLaunchGoogleSos, "emergency_gesture_enabled", null, SplitScreenReporter.ACTION_ENTER_SPLIT);
            setDefaultFunction(Settings.Secure.getStringForUser(MiuiKeyShortcutManager.this.mContentResolver, "emergency_gesture_sound_enabled", MiuiKeyShortcutManager.this.mCurrentUserId), "emergency_gesture_sound_enabled", null, SplitScreenReporter.ACTION_ENTER_SPLIT);
        }

        private void setDefaultFunction(String curttenFunction, String action, String function, String isGlobal) {
            boolean legalData = isLegalData(isGlobal);
            if (this.mResetMiuiShortcutSettings && legalData) {
                Settings.System.putStringForUser(MiuiKeyShortcutManager.this.mContext.getContentResolver(), action, "none", MiuiKeyShortcutManager.this.mCurrentUserId);
            }
            if ("split_screen".equals(curttenFunction)) {
                Settings.System.putStringForUser(MiuiKeyShortcutManager.this.mContentResolver, action, "none", MiuiKeyShortcutManager.this.mCurrentUserId);
            }
            if (legalData) {
                if ((this.mResetMiuiShortcutSettings || curttenFunction == null) && !hasCustomizedFunction(action, function) && function != null) {
                    Slog.i(MiuiKeyShortcutManager.TAG, "Loaded ShortcutKey action = " + action + ", function = " + function);
                    if (isFeasibleFunction(function, MiuiKeyShortcutManager.this.mContext)) {
                        Settings.System.putStringForUser(MiuiKeyShortcutManager.this.mContentResolver, action, function, MiuiKeyShortcutManager.this.mCurrentUserId);
                    }
                }
            }
        }

        /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
        private boolean isFeasibleFunction(String function, Context context) {
            char c;
            switch (function.hashCode()) {
                case -1074479227:
                    if (function.equals("mi_pay")) {
                        c = 0;
                        break;
                    }
                    c = 65535;
                    break;
                case 596487045:
                    if (function.equals("launch_voice_assistant")) {
                        c = 1;
                        break;
                    }
                    c = 65535;
                    break;
                case 2134385967:
                    if (function.equals("partial_screen_shot")) {
                        c = 2;
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
                    return MiuiSettings.Key.isTSMClientInstalled(context);
                case 1:
                    return MiuiSettings.System.isXiaoAiExist(context);
                case 2:
                    return FeatureParser.getBoolean("is_support_partial_screenshot", true);
                default:
                    return true;
            }
        }

        private void initCombinationRule(String primaryKey, String combationKey, String action) {
            MiuiCombinationRule combinationRule = new MiuiCombinationRule(Integer.parseInt(primaryKey), Integer.parseInt(combationKey), MiuiKeyShortcutManager.this.mHandler, MiuiKeyShortcutManager.this.mContentResolver, action) { // from class: com.android.server.policy.MiuiKeyShortcutManager.ConfigFileLoader.1
                void execute() {
                    ShortCutActionsUtils.getInstance(MiuiKeyShortcutManager.this.mContext).triggerFunction(getFunction(), getAction(), null, false);
                }

                void cancel() {
                }
            };
            MiuiKeyShortcutManager.this.mCombinationRuleHashMap.put(action, combinationRule);
            combinationRule.getObserver().registerShortcutListener(this);
        }

        private void initSingleKeyLongPress(String primaryKey, String action) {
            MiuiSingleKeyRule singleKeyRule = new MiuiSingleKeyRule(Integer.parseInt(primaryKey), 2, MiuiKeyShortcutManager.this.mHandler, MiuiKeyShortcutManager.this.mContentResolver, action) { // from class: com.android.server.policy.MiuiKeyShortcutManager.ConfigFileLoader.2
                void onLongPress(long eventTime) {
                    super.onLongPress(eventTime);
                    ShortCutActionsUtils.getInstance(MiuiKeyShortcutManager.this.mContext).triggerFunction(getFunction(), getAction(), null, false);
                }
            };
            MiuiKeyShortcutManager.this.mSingleKeyRuleHashMap.put(action, singleKeyRule);
            singleKeyRule.getObserver().registerShortcutListener(this);
        }

        private void initSingleKeyMultiPress(String primaryKey, String action) {
            MiuiSingleKeyRule multiPressRule = new MiuiSingleKeyRule(Integer.parseInt(primaryKey), 0, MiuiKeyShortcutManager.this.mHandler, MiuiKeyShortcutManager.this.mContentResolver, action) { // from class: com.android.server.policy.MiuiKeyShortcutManager.ConfigFileLoader.3
                void onMultiPress(long downTime, int count) {
                    super.onMultiPress(downTime, count);
                    if (count == 2) {
                        ShortCutActionsUtils.getInstance(MiuiKeyShortcutManager.this.mContext).triggerFunction(getFunction(), getAction(), null, false);
                    }
                }
            };
            multiPressRule.getObserver().registerShortcutListener(this);
            MiuiKeyShortcutManager.this.mSingleKeyRuleHashMap.put(action, multiPressRule);
        }

        private boolean hasCustomizedFunction(String action, String function) {
            if (MiuiKeyShortcutManager.DEVICE_IRIS_KDDI.equals(Build.DEVICE)) {
                if ("double_click_power_key".equals(action)) {
                    Settings.System.putStringForUser(MiuiKeyShortcutManager.this.mContentResolver, action, "au_pay", MiuiKeyShortcutManager.this.mCurrentUserId);
                    return true;
                } else if ("emergency_gesture_enabled".equals(action)) {
                    Settings.Secure.putIntForUser(MiuiKeyShortcutManager.this.mContentResolver, action, 0, MiuiKeyShortcutManager.this.mCurrentUserId);
                    return true;
                }
            } else if (MiuiKeyShortcutManager.this.isSoftBankCustom()) {
                if ("emergency_gesture_enabled".equals(action)) {
                    Settings.Secure.putIntForUser(MiuiKeyShortcutManager.this.mContentResolver, action, 0, MiuiKeyShortcutManager.this.mCurrentUserId);
                    return true;
                } else if ("emergency_gesture_sound_enabled".equals(action)) {
                    Settings.Secure.putIntForUser(MiuiKeyShortcutManager.this.mContentResolver, action, 1, MiuiKeyShortcutManager.this.mCurrentUserId);
                    return true;
                }
            }
            return "long_press_power_key".equals(action) && "launch_google_search".equals(function) && MiuiKeyShortcutManager.SUPPORT_GOOGLE_RSA_PROTOCOL && !MiuiKeyShortcutManager.this.supportRSARegion();
        }

        boolean isLegalData(String isGlobal) {
            try {
                int regionId = Integer.parseInt(isGlobal);
                return (!Build.IS_INTERNATIONAL_BUILD && regionId == 0) || (regionId == 1 && Build.IS_INTERNATIONAL_BUILD) || regionId == 3;
            } catch (NumberFormatException e) {
                Slog.e(MiuiKeyShortcutManager.TAG, "The globalId is error!");
                return false;
            }
        }

        @Override // com.android.server.policy.MiuiShortcutObserver.MiuiShortcutListener
        public void onCombinationChanged(MiuiCombinationRule rule) {
            if ("key_combination_power_volume_down".equals(rule.getAction())) {
                if ("screen_shot".equals(rule.getFunction())) {
                    if (MiuiKeyShortcutManager.this.mWindowManagerPolicy instanceof BaseMiuiPhoneWindowManager) {
                        MiuiKeyShortcutManager.this.mWindowManagerPolicy.addPowerVolumeDownRule();
                    }
                } else if (MiuiKeyShortcutManager.this.mWindowManagerPolicy instanceof BaseMiuiPhoneWindowManager) {
                    MiuiKeyShortcutManager.this.mWindowManagerPolicy.removePowerVolumeDownRule();
                }
            }
        }

        @Override // com.android.server.policy.MiuiShortcutObserver.MiuiShortcutListener
        public void onSingleChanged(MiuiSingleKeyRule rule) {
            if ("long_press_power_key".equals(rule.getAction())) {
                MiuiKeyShortcutManager.this.setLongPressPowerTime();
            }
        }

        @Override // com.android.server.policy.MiuiShortcutObserver.MiuiShortcutListener
        public void onGestureChanged(MiuiGestureRule rule) {
        }

        public void setResetMiuiShortcutSettings(boolean resetMiuiShortcutSettings) {
            this.mResetMiuiShortcutSettings = resetMiuiShortcutSettings;
        }
    }

    /* loaded from: classes.dex */
    public class MiuiOtherSettingsObserver extends ContentObserver {
        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public MiuiOtherSettingsObserver(Handler handler) {
            super(handler);
            MiuiKeyShortcutManager.this = this$0;
        }

        public void initObserver() {
            MiuiKeyShortcutManager.this.mContentResolver.registerContentObserver(Settings.System.getUriFor("long_press_power_launch_xiaoai"), false, this, -1);
            MiuiKeyShortcutManager.this.mContentResolver.registerContentObserver(Settings.System.getUriFor("long_press_power_launch_smarthome"), false, this, -1);
            MiuiKeyShortcutManager.this.mContentResolver.registerContentObserver(Settings.System.getUriFor("volumekey_launch_camera"), false, this, -1);
            MiuiKeyShortcutManager.this.mContentResolver.registerContentObserver(Settings.System.getUriFor("screen_key_press_app_switch"), false, this, -1);
            MiuiKeyShortcutManager.this.mContentResolver.registerContentObserver(Settings.System.getUriFor("fingerprint_nav_center_action"), false, this, -1);
            MiuiKeyShortcutManager.this.mContentResolver.registerContentObserver(Settings.System.getUriFor("single_key_use_enable"), false, this, -1);
            MiuiKeyShortcutManager.this.mContentResolver.registerContentObserver(Settings.System.getUriFor("long_press_menu_key_when_lock"), false, this, -1);
            if (!Build.IS_GLOBAL_BUILD) {
                MiuiKeyShortcutManager.this.mContentResolver.registerContentObserver(Settings.System.getUriFor("xiaoai_power_guide"), false, this, -1);
                onChange(false, Settings.System.getUriFor("xiaoai_power_guide"));
            }
            if (ShortCutActionsUtils.getInstance(MiuiKeyShortcutManager.this.mContext).isNeedShowGoogleGuideForOperator()) {
                MiuiKeyShortcutManager.this.mContentResolver.registerContentObserver(Settings.System.getUriFor("google_assistant_guide"), false, this, -1);
                onChange(false, Settings.System.getUriFor("google_assistant_guide"));
            }
            if (MiuiKeyShortcutManager.SUPPORT_GOOGLE_RSA_PROTOCOL && MiuiKeyShortcutManager.this.supportRSARegion()) {
                MiuiKeyShortcutManager.this.mContentResolver.registerContentObserver(Settings.System.getUriFor("global_power_guide"), false, this, -1);
                onChange(false, Settings.System.getUriFor("global_power_guide"));
            }
            MiuiKeyShortcutManager.this.mContentResolver.registerContentObserver(Settings.Secure.getUriFor("user_setup_complete"), false, this, -1);
            MiuiKeyShortcutManager.this.mContentResolver.registerContentObserver(Settings.System.getUriFor("pc_mode_open"), false, this, -1);
            MiuiKeyShortcutManager.this.mContentResolver.registerContentObserver(Settings.System.getUriFor("long_press_power_launch_smarthome"), false, this, -1);
            MiuiKeyShortcutManager.this.mContentResolver.registerContentObserver(Settings.Secure.getUriFor("emergency_gesture_enabled"), false, this, -1);
            MiuiKeyShortcutManager.this.mContentResolver.registerContentObserver(Settings.Secure.getUriFor(MiuiKeyShortcutManager.KEY_MIUI_SOS_ENABLE), false, this, -1);
            MiuiKeyShortcutManager.this.mContentResolver.registerContentObserver(Settings.Secure.getUriFor(MiuiKeyShortcutManager.KEY_IS_IN_MIUI_SOS_MODE), false, this, -1);
            MiuiKeyShortcutManager.this.mContentResolver.registerContentObserver(Settings.Secure.getUriFor("assist_long_press_home_enabled"), false, this, -1);
            MiuiKeyShortcutManager.this.mContentResolver.registerContentObserver(Settings.Secure.getUriFor(MiuiSettings.Secure.MIUI_OPTIMIZATION), false, this, -1);
            onChange(false);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            boolean z = false;
            if (Settings.System.getUriFor("long_press_power_launch_xiaoai").equals(uri)) {
                MiuiKeyShortcutManager miuiKeyShortcutManager = MiuiKeyShortcutManager.this;
                if (Settings.System.getIntForUser(miuiKeyShortcutManager.mContentResolver, "long_press_power_launch_xiaoai", 0, MiuiKeyShortcutManager.this.mCurrentUserId) == 1) {
                    z = true;
                }
                miuiKeyShortcutManager.mLongPressPowerKeyLaunchXiaoAi = z;
                if (Build.IS_GLOBAL_BUILD) {
                    Settings.System.putStringForUser(MiuiKeyShortcutManager.this.mContentResolver, "long_press_power_key", MiuiKeyShortcutManager.this.mLongPressPowerKeyLaunchXiaoAi ? "launch_google_search" : "none", MiuiKeyShortcutManager.this.mCurrentUserId);
                } else if (MiuiKeyShortcutManager.this.mLongPressPowerKeyLaunchXiaoAi) {
                    Settings.System.putStringForUser(MiuiKeyShortcutManager.this.mContentResolver, "long_press_power_key", "launch_voice_assistant", MiuiKeyShortcutManager.this.mCurrentUserId);
                }
            } else if (Settings.System.getUriFor("long_press_power_launch_smarthome").equals(uri)) {
                MiuiKeyShortcutManager miuiKeyShortcutManager2 = MiuiKeyShortcutManager.this;
                if (Settings.System.getIntForUser(miuiKeyShortcutManager2.mContentResolver, "long_press_power_launch_smarthome", 0, MiuiKeyShortcutManager.this.mCurrentUserId) == 1) {
                    z = true;
                }
                miuiKeyShortcutManager2.mLongPressPowerKeyLaunchSmartHome = z;
                if (MiuiKeyShortcutManager.this.mLongPressPowerKeyLaunchSmartHome) {
                    Settings.System.putStringForUser(MiuiKeyShortcutManager.this.mContentResolver, "long_press_power_key", "launch_smarthome", MiuiKeyShortcutManager.this.mCurrentUserId);
                }
            } else if (Settings.System.getUriFor("volumekey_launch_camera").equals(uri)) {
                MiuiKeyShortcutManager miuiKeyShortcutManager3 = MiuiKeyShortcutManager.this;
                if (!MiuiKeyShortcutManager.SUPPORT_EDGE_TOUCH_VOLUME && Settings.System.getIntForUser(MiuiKeyShortcutManager.this.mContentResolver, "volumekey_launch_camera", 0, MiuiKeyShortcutManager.this.mCurrentUserId) == 1) {
                    z = true;
                }
                miuiKeyShortcutManager3.mVolumeKeyLaunchCamera = z;
            } else if (Settings.System.getUriFor("screen_key_press_app_switch").equals(uri)) {
                MiuiKeyShortcutManager miuiKeyShortcutManager4 = MiuiKeyShortcutManager.this;
                if (Settings.System.getIntForUser(miuiKeyShortcutManager4.mContentResolver, "screen_key_press_app_switch", 1, MiuiKeyShortcutManager.this.mCurrentUserId) != 0) {
                    z = true;
                }
                miuiKeyShortcutManager4.mPressToAppSwitch = z;
            } else if (Settings.System.getUriFor("fingerprint_nav_center_action").equals(uri)) {
                MiuiKeyShortcutManager miuiKeyShortcutManager5 = MiuiKeyShortcutManager.this;
                miuiKeyShortcutManager5.mFingerPrintNavCenterAction = Settings.System.getIntForUser(miuiKeyShortcutManager5.mContentResolver, "fingerprint_nav_center_action", -1, MiuiKeyShortcutManager.this.mCurrentUserId);
            } else if (Settings.System.getUriFor("single_key_use_enable").equals(uri)) {
                MiuiKeyShortcutManager miuiKeyShortcutManager6 = MiuiKeyShortcutManager.this;
                if (Settings.System.getIntForUser(miuiKeyShortcutManager6.mContentResolver, "single_key_use_enable", 0, MiuiKeyShortcutManager.this.mCurrentUserId) == 1) {
                    z = true;
                }
                miuiKeyShortcutManager6.mSingleKeyUse = z;
                MiuiKeyShortcutManager miuiKeyShortcutManager7 = MiuiKeyShortcutManager.this;
                miuiKeyShortcutManager7.mDoubleTapOnHomeBehavior = miuiKeyShortcutManager7.mSingleKeyUse ? 1 : 0;
            } else if (Settings.System.getUriFor("long_press_menu_key_when_lock").equals(uri)) {
                MiuiKeyShortcutManager miuiKeyShortcutManager8 = MiuiKeyShortcutManager.this;
                miuiKeyShortcutManager8.mLongPressMenuKeyWhenLock = MiuiSettings.Key.getKeyAndGestureShortcutFunction(miuiKeyShortcutManager8.mContext, "long_press_menu_key_when_lock");
            } else if (Settings.System.getUriFor("xiaoai_power_guide").equals(uri)) {
                MiuiKeyShortcutManager miuiKeyShortcutManager9 = MiuiKeyShortcutManager.this;
                miuiKeyShortcutManager9.mXiaoaiPowerGuideFlag = Settings.System.getIntForUser(miuiKeyShortcutManager9.mContentResolver, "xiaoai_power_guide", 1, MiuiKeyShortcutManager.this.mCurrentUserId);
                MiuiKeyShortcutManager.this.setLongPressPowerTime();
            } else if (Settings.Secure.getUriFor("user_setup_complete").equals(uri)) {
                MiuiKeyShortcutManager.this.setLongPressPowerTime();
            } else if (Settings.System.getUriFor("pc_mode_open").equals(uri)) {
                MiuiKeyShortcutManager miuiKeyShortcutManager10 = MiuiKeyShortcutManager.this;
                if (Settings.System.getIntForUser(miuiKeyShortcutManager10.mContentResolver, "pc_mode_open", 0, MiuiKeyShortcutManager.this.mCurrentUserId) == 1) {
                    z = true;
                }
                miuiKeyShortcutManager10.mIsOnPcMode = z;
                MiuiKeyShortcutManager.this.setLongPressPowerTime();
            } else if (Settings.Secure.getUriFor("emergency_gesture_enabled").equals(uri)) {
                MiuiKeyShortcutManager miuiKeyShortcutManager11 = MiuiKeyShortcutManager.this;
                miuiKeyShortcutManager11.mFivePressPowerLaunchGoogleSos = Settings.Secure.getStringForUser(miuiKeyShortcutManager11.mContentResolver, "emergency_gesture_enabled", MiuiKeyShortcutManager.this.mCurrentUserId);
            } else if (Settings.Secure.getUriFor(MiuiKeyShortcutManager.KEY_MIUI_SOS_ENABLE).equals(uri)) {
                MiuiKeyShortcutManager miuiKeyShortcutManager12 = MiuiKeyShortcutManager.this;
                if (Settings.Secure.getIntForUser(miuiKeyShortcutManager12.mContentResolver, MiuiKeyShortcutManager.KEY_MIUI_SOS_ENABLE, 0, MiuiKeyShortcutManager.this.mCurrentUserId) == 1) {
                    z = true;
                }
                miuiKeyShortcutManager12.mFivePressPowerLaunchSos = z;
            } else if (Settings.Secure.getUriFor(MiuiKeyShortcutManager.KEY_IS_IN_MIUI_SOS_MODE).equals(uri)) {
                MiuiKeyShortcutManager miuiKeyShortcutManager13 = MiuiKeyShortcutManager.this;
                if (Settings.Secure.getIntForUser(miuiKeyShortcutManager13.mContentResolver, MiuiKeyShortcutManager.KEY_IS_IN_MIUI_SOS_MODE, 0, MiuiKeyShortcutManager.this.mCurrentUserId) == 1) {
                    z = true;
                }
                miuiKeyShortcutManager13.mIsOnSosMode = z;
            } else if (Settings.System.getUriFor("google_assistant_guide").equals(uri)) {
                MiuiKeyShortcutManager miuiKeyShortcutManager14 = MiuiKeyShortcutManager.this;
                miuiKeyShortcutManager14.mGoogleAssistantGuideFlag = Settings.System.getIntForUser(miuiKeyShortcutManager14.mContentResolver, "google_assistant_guide", 1, MiuiKeyShortcutManager.this.mCurrentUserId);
                MiuiKeyShortcutManager.this.setLongPressPowerTime();
            } else if (Settings.System.getUriFor("global_power_guide").equals(uri)) {
                MiuiKeyShortcutManager miuiKeyShortcutManager15 = MiuiKeyShortcutManager.this;
                miuiKeyShortcutManager15.mRSAGuideStatus = Settings.System.getIntForUser(miuiKeyShortcutManager15.mContentResolver, "global_power_guide", 1, MiuiKeyShortcutManager.this.mCurrentUserId);
            } else if (Settings.Secure.getUriFor("assist_long_press_home_enabled").equals(uri)) {
                MiuiKeyShortcutManager miuiKeyShortcutManager16 = MiuiKeyShortcutManager.this;
                if (Settings.Secure.getInt(miuiKeyShortcutManager16.mContentResolver, "assist_long_press_home_enabled", -1) == 1) {
                    z = true;
                }
                miuiKeyShortcutManager16.mAOSPAssistantLongPressHomeEnabled = z;
            } else if (Settings.Secure.getUriFor(MiuiSettings.Secure.MIUI_OPTIMIZATION).equals(uri)) {
                MiuiKeyShortcutManager.this.mIsCtsMode = !SystemProperties.getBoolean("persist.sys.miui_optimization", !SplitScreenReporter.ACTION_ENTER_SPLIT.equals(SystemProperties.get("ro.miui.cts")));
            }
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange) {
            MiuiKeyShortcutManager miuiKeyShortcutManager = MiuiKeyShortcutManager.this;
            boolean z = false;
            miuiKeyShortcutManager.mLongPressPowerKeyLaunchXiaoAi = Settings.System.getIntForUser(miuiKeyShortcutManager.mContentResolver, "long_press_power_launch_xiaoai", 0, MiuiKeyShortcutManager.this.mCurrentUserId) == 1;
            MiuiKeyShortcutManager.this.mVolumeKeyLaunchCamera = !MiuiKeyShortcutManager.SUPPORT_EDGE_TOUCH_VOLUME && Settings.System.getIntForUser(MiuiKeyShortcutManager.this.mContentResolver, "volumekey_launch_camera", 0, MiuiKeyShortcutManager.this.mCurrentUserId) == 1;
            MiuiKeyShortcutManager miuiKeyShortcutManager2 = MiuiKeyShortcutManager.this;
            miuiKeyShortcutManager2.mPressToAppSwitch = Settings.System.getIntForUser(miuiKeyShortcutManager2.mContentResolver, "screen_key_press_app_switch", 1, MiuiKeyShortcutManager.this.mCurrentUserId) != 0;
            MiuiKeyShortcutManager miuiKeyShortcutManager3 = MiuiKeyShortcutManager.this;
            miuiKeyShortcutManager3.mFingerPrintNavCenterAction = Settings.System.getIntForUser(miuiKeyShortcutManager3.mContentResolver, "fingerprint_nav_center_action", -1, MiuiKeyShortcutManager.this.mCurrentUserId);
            MiuiKeyShortcutManager miuiKeyShortcutManager4 = MiuiKeyShortcutManager.this;
            miuiKeyShortcutManager4.mSingleKeyUse = Settings.System.getIntForUser(miuiKeyShortcutManager4.mContentResolver, "single_key_use_enable", 0, MiuiKeyShortcutManager.this.mCurrentUserId) == 1;
            MiuiKeyShortcutManager miuiKeyShortcutManager5 = MiuiKeyShortcutManager.this;
            miuiKeyShortcutManager5.mDoubleTapOnHomeBehavior = miuiKeyShortcutManager5.mSingleKeyUse ? 1 : 0;
            MiuiKeyShortcutManager miuiKeyShortcutManager6 = MiuiKeyShortcutManager.this;
            miuiKeyShortcutManager6.mLongPressMenuKeyWhenLock = MiuiSettings.Key.getKeyAndGestureShortcutFunction(miuiKeyShortcutManager6.mContext, "long_press_menu_key_when_lock");
            if (!Build.IS_GLOBAL_BUILD) {
                MiuiKeyShortcutManager miuiKeyShortcutManager7 = MiuiKeyShortcutManager.this;
                miuiKeyShortcutManager7.mXiaoaiPowerGuideFlag = Settings.System.getIntForUser(miuiKeyShortcutManager7.mContentResolver, "xiaoai_power_guide", 1, MiuiKeyShortcutManager.this.mCurrentUserId);
                MiuiKeyShortcutManager.this.setLongPressPowerTime();
            }
            if (ShortCutActionsUtils.getInstance(MiuiKeyShortcutManager.this.mContext).isNeedShowGoogleGuideForOperator()) {
                MiuiKeyShortcutManager miuiKeyShortcutManager8 = MiuiKeyShortcutManager.this;
                miuiKeyShortcutManager8.mGoogleAssistantGuideFlag = Settings.System.getIntForUser(miuiKeyShortcutManager8.mContentResolver, "google_assistant_guide", 1, MiuiKeyShortcutManager.this.mCurrentUserId);
                MiuiKeyShortcutManager.this.setLongPressPowerTime();
            }
            MiuiKeyShortcutManager miuiKeyShortcutManager9 = MiuiKeyShortcutManager.this;
            miuiKeyShortcutManager9.mRSAGuideStatus = Settings.System.getIntForUser(miuiKeyShortcutManager9.mContentResolver, "global_power_guide", 1, MiuiKeyShortcutManager.this.mCurrentUserId);
            MiuiKeyShortcutManager miuiKeyShortcutManager10 = MiuiKeyShortcutManager.this;
            miuiKeyShortcutManager10.mFivePressPowerLaunchGoogleSos = Settings.Secure.getStringForUser(miuiKeyShortcutManager10.mContentResolver, "emergency_gesture_enabled", MiuiKeyShortcutManager.this.mCurrentUserId);
            MiuiKeyShortcutManager miuiKeyShortcutManager11 = MiuiKeyShortcutManager.this;
            miuiKeyShortcutManager11.mFivePressPowerLaunchSos = Settings.Secure.getIntForUser(miuiKeyShortcutManager11.mContentResolver, MiuiKeyShortcutManager.KEY_MIUI_SOS_ENABLE, 0, MiuiKeyShortcutManager.this.mCurrentUserId) == 1;
            MiuiKeyShortcutManager miuiKeyShortcutManager12 = MiuiKeyShortcutManager.this;
            miuiKeyShortcutManager12.mIsOnSosMode = Settings.Secure.getIntForUser(miuiKeyShortcutManager12.mContentResolver, MiuiKeyShortcutManager.KEY_IS_IN_MIUI_SOS_MODE, 0, MiuiKeyShortcutManager.this.mCurrentUserId) == 1;
            MiuiKeyShortcutManager miuiKeyShortcutManager13 = MiuiKeyShortcutManager.this;
            if (Settings.Secure.getInt(miuiKeyShortcutManager13.mContentResolver, "assist_long_press_home_enabled", -1) == 1) {
                z = true;
            }
            miuiKeyShortcutManager13.mAOSPAssistantLongPressHomeEnabled = z;
            MiuiKeyShortcutManager.this.mIsCtsMode = !SystemProperties.getBoolean("persist.sys.miui_optimization", !SplitScreenReporter.ACTION_ENTER_SPLIT.equals(SystemProperties.get("ro.miui.cts")));
            super.onChange(selfChange);
        }
    }

    public void dump(String prefix, PrintWriter pw) {
        pw.print(prefix);
        pw.println(TAG);
        String prefix2 = prefix + "  ";
        pw.print(prefix2);
        pw.print("mCombinationPowerHome=");
        pw.println(getFunction("key_combination_power_home"));
        pw.print(prefix2);
        pw.print("mCombinationPowerBack=");
        pw.println(getFunction("key_combination_power_back"));
        pw.print(prefix2);
        pw.print("mCombinationPowerMenu=");
        pw.println(getFunction("key_combination_power_menu"));
        pw.print(prefix2);
        pw.print("mLongPressHome=");
        pw.println(getFunction("long_press_home_key"));
        pw.print(prefix2);
        pw.print("mLongPressBack=");
        pw.println(getFunction("long_press_back_key"));
        pw.print(prefix2);
        pw.print("mLongPressMenu=");
        pw.println(getFunction("long_press_menu_key"));
        pw.print(prefix2);
        pw.print("mLongPressPower=");
        pw.println(getFunction("long_press_power_key"));
        pw.print(prefix2);
        pw.print("mDoubleClickPower=");
        pw.println(getFunction("double_click_power_key"));
        pw.print(prefix2);
        pw.print("mVolumeKeyLaunchCamera=");
        pw.println(this.mVolumeKeyLaunchCamera);
        pw.print(prefix2);
        pw.print("mPressToAppSwitch=");
        pw.println(this.mPressToAppSwitch);
        pw.print(prefix2);
        pw.print("mLongPressPowerTime=");
        pw.println(this.mLongPressPowerTime);
        pw.print(prefix2);
        pw.print("isSosCanBeTrigger=");
        pw.println(isSosCanBeTrigger());
        pw.print(prefix2);
        pw.print("isGoogleSosEnable=");
        pw.println(isGoogleSosEnable());
        pw.print("mFingerPrintNavCenterAction=");
        pw.println(this.mFingerPrintNavCenterAction);
        pw.print("mEnableCustomShortcutKey =");
        pw.println(this.mEnableCustomShortcutKey);
        pw.print(prefix2);
        pw.print("mAOSPAssistantLongPressHomeEnabled=");
        pw.println(this.mAOSPAssistantLongPressHomeEnabled);
        pw.print(prefix2);
        pw.print("mIsCtsMode=");
        pw.println(this.mIsCtsMode);
    }
}
