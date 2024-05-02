package com.android.server.wm;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.provider.MiuiSettings;
import android.provider.Settings;
import android.server.am.SplitScreenReporter;
import android.sizecompat.MiuiSizeCompatManager;
import android.text.TextUtils;
import android.util.AtomicFile;
import android.util.Pair;
import android.util.RotationUtils;
import android.util.Slog;
import android.util.SparseArray;
import android.util.TypedXmlPullParser;
import android.util.Xml;
import android.widget.Toast;
import com.android.internal.os.BackgroundThread;
import com.android.internal.protolog.ProtoLogGroup;
import com.android.internal.util.XmlUtils;
import com.android.server.DisplayThread;
import com.android.server.MiuiBgThread;
import com.android.server.MiuiFgThread;
import com.android.server.wm.ActivityRecord;
import com.android.server.wm.MiuiOrientationImpl;
import com.miui.base.MiuiStubRegistry;
import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import miui.app.MiuiFreeFormManager;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes.dex */
public class MiuiOrientationImpl implements MiuiOrientationStub {
    private static boolean ENABLED = false;
    private static final int FOLD_DEVICE_TYPE = 2;
    private static boolean IS_FOLD_SCREEN_DEVICE = false;
    private static final int KEY_INDEX = 0;
    private static final String KEY_SMART_ROTATION = "miui_smart_rotation";
    private static final String MIUI_ORIENTATION_ENABLE = "ro.config.miui_orientation_enable";
    private static final String MUILTDISPLAY_TYPE = "persist.sys.muiltdisplay_type";
    private static final int ORIENTATION_ARRAY_LENGTH = 2;
    private static final String ORIENTATION_ATTR_PACKAGE_NAME = "name";
    private static final String ORIENTATION_ATTR_PACKAGE_VALUE = "value";
    private static final String ORIENTATION_COMMIT_TAG = "miui_orientations";
    private static final String ORIENTATION_FILE_PATH = "system/miui_orientation_settings.xml";
    private static final int ORIENTATION_INDEX = 1;
    private static final String ORIENTATION_TAG = "orientation-settings";
    private static final String ORIENTATION_TAG_PACKAGE = "package";
    private static final String ORIENTATION_USER_SETTINGS_REMOVE = "--remove";
    public static final int POLICY_FULLSCREEN_BY_ALLOW_LIST = 16;
    public static final int POLICY_FULLSCREEN_BY_BLOCK_LIST = 32;
    public static final int POLICY_FULLSCREEN_CAMERA_ROTATE = 512;
    public static final int POLICY_FULLSCREEN_NOT_ROTATE_APP = 64;
    public static final int POLICY_FULLSCREEN_NOT_ROTATE_SENSOR = 128;
    public static final int POLICY_FULLSCREEN_RESUME_CAMERA_ROTATE = 256;
    public static final int POLICY_FULLSCREEN_UPDATE_CONFIG = 1024;
    public static final int POLICY_FULL_SCREEN_INVALID = -1;
    public static final int POLICY_FULL_SCREEN_NOT_RELAUNCH = 1;
    public static final int POLICY_FULL_SCREEN_RELAUNCH = 2;
    public static final int POLICY_FULL_SCREEN_RELAUNCH_INTERACTIVE = 4;
    public static final int POLICY_FULL_SCREEN_TOAST = 8;
    private static final int SCREEN_ROTATION_INNER = 1;
    private static final int SCREEN_ROTATION_OUTER = 2;
    private static final int SCREEN_ROTATION_PAD = 3;
    private static final String TAG = "MiuiOrientationImpl";
    private Toast mCompatLandToast;
    private Context mContext;
    private boolean mDemoMode;
    private int mDemoModePolicy;
    private AtomicFile mFile;
    private Handler mFileHandler;
    private FullScreen3AppCameraStrategy mFullScreen3AppCameraStrategy;
    private FullScreenPackageManager mFullScreenPackageManager;
    private Handler mHandler;
    private MiuiSettingsObserver mMiuiSettingsObserver;
    private ActivityTaskManagerServiceImpl mServiceImpl;
    private int mShowRotationSuggestion;
    private boolean mSmartRotationEnabled;
    private boolean mToastVisible;
    private static final String MIUI_OPTIMIZATION = "persist.sys.miui_optimization";
    private static final String MIUI_CTS = "ro.miui.cts";
    private static boolean IS_MIUI_OPTIMIZATION = SystemProperties.getBoolean(MIUI_OPTIMIZATION, !SplitScreenReporter.ACTION_ENTER_SPLIT.equals(SystemProperties.get(MIUI_CTS)));
    private final Map<String, Integer> mPackagesMapBySystem = new ConcurrentHashMap();
    private final Map<String, Integer> mComponentMapBySystem = new ConcurrentHashMap();
    private Map<String, Integer> mPackagesMapByUserSettings = new ConcurrentHashMap();
    private Map<String, FullRule> mEmbeddedFullRules = new HashMap();
    private Runnable mWriteSettingsRunnable = new Runnable() { // from class: com.android.server.wm.MiuiOrientationImpl.2
        @Override // java.lang.Runnable
        public void run() {
            MiuiOrientationImpl.writeSettings(MiuiOrientationImpl.this.mFile, MiuiOrientationImpl.this.mPackagesMapByUserSettings);
        }
    };

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<MiuiOrientationImpl> {

        /* compiled from: MiuiOrientationImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final MiuiOrientationImpl INSTANCE = new MiuiOrientationImpl();
        }

        public MiuiOrientationImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public MiuiOrientationImpl provideNewInstance() {
            return new MiuiOrientationImpl();
        }
    }

    static {
        boolean z = true;
        if (SystemProperties.getInt(MUILTDISPLAY_TYPE, 0) != 2) {
            z = false;
        }
        IS_FOLD_SCREEN_DEVICE = z;
        ENABLED = SystemProperties.getBoolean(MIUI_ORIENTATION_ENABLE, false);
    }

    public MiuiOrientationImpl() {
        this.mSmartRotationEnabled = ENABLED && IS_MIUI_OPTIMIZATION;
    }

    public static MiuiOrientationImpl getInstance() {
        return (MiuiOrientationImpl) MiuiOrientationStub.get();
    }

    public void init(Context context, ActivityTaskManagerServiceImpl serviceImpl) {
        this.mContext = context;
        this.mServiceImpl = serviceImpl;
        this.mHandler = new Handler(MiuiFgThread.getHandler().getLooper());
        this.mFileHandler = new Handler(MiuiBgThread.getHandler().getLooper());
        this.mMiuiSettingsObserver = new MiuiSettingsObserver(this.mHandler);
        this.mFullScreenPackageManager = new FullScreenPackageManager(context, serviceImpl);
        this.mFullScreen3AppCameraStrategy = new FullScreen3AppCameraStrategy();
        this.mCompatLandToast = Toast.makeText(this.mContext, "", 1);
        this.mFile = getSettingsFile();
        this.mShowRotationSuggestion = this.mContext.getResources().getInteger(285933647);
    }

    public void onSystemReady() {
        this.mMiuiSettingsObserver.observe();
        Map<String, Integer> settings = readSettings(this.mFile);
        if (settings != null && !settings.isEmpty()) {
            this.mPackagesMapByUserSettings.putAll(settings);
        }
    }

    /* loaded from: classes.dex */
    public class MiuiSettingsObserver extends ContentObserver {
        ContentResolver resolver;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        MiuiSettingsObserver(Handler handler) {
            super(handler);
            MiuiOrientationImpl.this = r1;
            this.resolver = r1.mContext.getContentResolver();
        }

        void observe() {
            this.resolver.registerContentObserver(Settings.System.getUriFor(MiuiOrientationImpl.KEY_SMART_ROTATION), false, this, -1);
            onChange(false, Settings.System.getUriFor(MiuiOrientationImpl.KEY_SMART_ROTATION));
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            if (Settings.System.getUriFor(MiuiOrientationImpl.KEY_SMART_ROTATION).equals(uri)) {
                boolean isEnabled = MiuiSettings.System.getBooleanForUser(this.resolver, MiuiOrientationImpl.KEY_SMART_ROTATION, MiuiOrientationImpl.ENABLED, -2);
                MiuiOrientationImpl.this.mSmartRotationEnabled = MiuiOrientationImpl.ENABLED && MiuiOrientationImpl.IS_MIUI_OPTIMIZATION && isEnabled;
                Slog.d(MiuiOrientationImpl.TAG, "onChanged " + MiuiOrientationImpl.this.mSmartRotationEnabled);
            }
        }
    }

    public void setUserSettings(PrintWriter pw, String pkgName, String value) {
        this.mFullScreenPackageManager.setUserSettings(pkgName, value);
    }

    public void setUserSettings(PrintWriter pw, String[] pkgNames, String value) {
        this.mFullScreenPackageManager.setUserSettings(pkgNames, value);
    }

    public FullScreenPackageManager getPackageManager() {
        return this.mFullScreenPackageManager;
    }

    public boolean isEnabled() {
        return this.mSmartRotationEnabled;
    }

    public int getShowRotationSuggestion() {
        return this.mShowRotationSuggestion;
    }

    public int getOrientationMode(ActivityRecord r, int orientation) {
        int mode;
        if ((!isEnabled() && !IS_TABLET) || r.mAtmService.mWindowManager.mPolicy.isDisplayFolded() || isEmbedded(r)) {
            return -1;
        }
        String packageName = r.mActivityComponent.getPackageName();
        if (this.mDemoMode) {
            mode = 1;
        } else if (this.mServiceImpl.inMiuiGameSizeCompat(r.packageName)) {
            mode = -1;
        } else if (IS_TABLET && isLandEnabledForPackage(packageName)) {
            mode = 1;
        } else {
            mode = getOrientationMode(r, packageName);
        }
        if (ProtoLogGroup.WM_DEBUG_ORIENTATION.isLogToLogcat()) {
            Slog.d(TAG, "getOrientationMode packageName=" + packageName + " mode=" + fullScreenModeToString(mode));
        }
        return mode;
    }

    public void setEmbeddedFullRuleData(Map<String, Pair<String, Boolean>> fullRuleMap) {
        this.mEmbeddedFullRules.clear();
        fullRuleMap.forEach(new BiConsumer<String, Pair<String, Boolean>>() { // from class: com.android.server.wm.MiuiOrientationImpl.1
            public void accept(String pkg, Pair<String, Boolean> pair) {
                String[] rules = ((String) pair.first).split(":");
                boolean nra = false;
                boolean nrs = false;
                boolean cr = true;
                boolean rcr = true;
                boolean nr = false;
                boolean r = false;
                boolean ri = false;
                for (String str : rules) {
                    char c = 65535;
                    switch (str.hashCode()) {
                        case 114:
                            if (str.equals(FoldablePackagePolicy.POLICY_VALUE_RESTART_LIST)) {
                                c = 5;
                                break;
                            }
                            break;
                        case 3183:
                            if (str.equals("cr")) {
                                c = 2;
                                break;
                            }
                            break;
                        case 3524:
                            if (str.equals("nr")) {
                                c = 4;
                                break;
                            }
                            break;
                        case 3639:
                            if (str.equals("ri")) {
                                c = 6;
                                break;
                            }
                            break;
                        case 109341:
                            if (str.equals("nra")) {
                                c = 0;
                                break;
                            }
                            break;
                        case 109359:
                            if (str.equals("nrs")) {
                                c = 1;
                                break;
                            }
                            break;
                        case 112737:
                            if (str.equals("rcr")) {
                                c = 3;
                                break;
                            }
                            break;
                    }
                    switch (c) {
                        case 0:
                            nra = true;
                            break;
                        case 1:
                            nrs = true;
                            break;
                        case 2:
                            cr = false;
                            break;
                        case 3:
                            rcr = false;
                            break;
                        case 4:
                            nr = true;
                            break;
                        case 5:
                            r = true;
                            break;
                        case 6:
                            ri = true;
                            break;
                        default:
                            Slog.d(MiuiOrientationImpl.TAG, "Unknown options for setEmbeddedFullRule :" + str);
                            break;
                    }
                }
                MiuiOrientationImpl.this.mEmbeddedFullRules.put(pkg, new FullRule(pkg, ((Boolean) pair.second).booleanValue(), nra, nrs, cr, rcr, nr, r, ri));
            }
        });
    }

    public boolean isNeedUpdateConfig(ActivityRecord r) {
        int policy;
        return r != null && isEnabled() && !IS_TABLET && !r.mAtmService.mWindowManager.mPolicy.isDisplayFolded() && !r.inMultiWindowMode() && !isEmbedded(r) && !isFixedAspectRatio(r.mActivityComponent.getPackageName()) && -1 != (policy = this.mFullScreenPackageManager.getOrientationPolicy(r.mActivityComponent.getPackageName())) && (policy & 1024) != 0;
    }

    public boolean isRotate3AppCameraInFullScreen(ActivityRecord r) {
        boolean z = false;
        if ((isEnabled() || IS_TABLET) && !isEmbedded(r)) {
            if ((isNeedCameraRotate(r) && !r.mAtmService.mWindowManager.mPolicy.isDisplayFolded()) || isNeedCameraRotateInPad(r)) {
                z = true;
            }
            boolean rotate3AppCamera = z;
            return rotate3AppCamera;
        }
        return false;
    }

    public boolean isFixedAspectRatio(String packageName) {
        return !TextUtils.isEmpty(packageName) && this.mServiceImpl.getAspectRatio(packageName) > MiuiFreeformPinManagerService.EDGE_AREA;
    }

    private boolean isFixedAspectRatioInPad(ActivityRecord r) {
        try {
            return MiuiSizeCompatManager.getMiuiSizeCompatAppRatio(r.mActivityComponent.getPackageName()) > MiuiFreeformPinManagerService.EDGE_AREA;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean isEmbedded(ActivityRecord r) {
        return MiuiEmbeddingWindowServiceStub.get().isEmbeddingEnabledForPackage(r.mActivityComponent.getPackageName());
    }

    private boolean isLandEnabledForPackage(String packageName) {
        FullRule fullRule = this.mEmbeddedFullRules.get(packageName);
        if (fullRule != null) {
            return fullRule.mEnable;
        }
        return false;
    }

    public boolean isNeedRotateWhenCameraResume(ActivityRecord r) {
        int policy;
        return (IS_TABLET || -1 == (policy = this.mFullScreenPackageManager.getOrientationPolicy(r.mActivityComponent.getPackageName())) || (policy & 256) == 0) ? false : true;
    }

    public boolean isNeedCameraRotate(ActivityRecord r) {
        if (isInBlackList(r) || IS_TABLET) {
            return false;
        }
        int policy = this.mFullScreenPackageManager.getOrientationPolicy(r.mActivityComponent.getPackageName());
        return isFixedAspectRatio(r.mActivityComponent.getPackageName()) || !(-1 == policy || (policy & 512) == 0);
    }

    public boolean isNeedCameraRotateInPad(ActivityRecord r) {
        if (!IS_TABLET) {
            return false;
        }
        String packageName = r.mActivityComponent.getPackageName();
        FullRule fullRule = this.mEmbeddedFullRules.get(packageName);
        boolean isAppSupportCameraPreview = isFixedAspectRatioInPad(r) && MiuiEmbeddingWindowServiceStub.get().isAppSupportCameraPreview(packageName);
        return (fullRule != null && fullRule.mCr) || isAppSupportCameraPreview;
    }

    public boolean isNeedRotateWhenCameraResumeInPad(ActivityRecord r) {
        if (!IS_TABLET || !isNeedCameraRotateInPad(r)) {
            return false;
        }
        FullRule fullRule = this.mEmbeddedFullRules.get(r.mActivityComponent.getPackageName());
        return (fullRule != null && fullRule.mRcr) || isFixedAspectRatioInPad(r);
    }

    private boolean isInBlackListFromPolicy(int policy) {
        return (-1 == policy || (policy & 32) == 0) ? false : true;
    }

    private boolean isInBlackList(ActivityRecord r) {
        return getOrientationMode(r, r.mActivityComponent.getPackageName()) == -1;
    }

    private int getOrientationMode(ActivityRecord r, String packageName) {
        int activityPolicy = this.mFullScreenPackageManager.getOrientationPolicyByComponent(r.mActivityComponent.flattenToShortString());
        if (-1 != activityPolicy) {
            if ((activityPolicy & 32) != 0) {
                return -1;
            }
            return 1;
        }
        int packagePolicy = this.mFullScreenPackageManager.getOrientationPolicy(packageName);
        if (isInBlackListFromPolicy(packagePolicy)) {
            return -1;
        }
        if (isFixedAspectRatio(packageName)) {
            return 2;
        }
        int mode = this.mFullScreenPackageManager.getOrientationMode(packageName);
        return mode;
    }

    private int getRelaunchModeFromPolicy(int policy) {
        if ((policy & 1) != 0) {
            return 2;
        }
        if ((policy & 2) != 0) {
            return 1;
        }
        if ((policy & 4) == 0) {
            return 0;
        }
        return 4;
    }

    private int getRelaunchModeFromBundle(String packageName) {
        if (!IS_TABLET || !isLandEnabledForPackage(packageName)) {
            return 0;
        }
        FullRule fullRule = this.mEmbeddedFullRules.get(packageName);
        if (fullRule.mNr) {
            return 2;
        }
        if (fullRule.mR) {
            return 1;
        }
        if (!fullRule.mRi) {
            return 0;
        }
        return 4;
    }

    public int getRelaunchMode(ActivityRecord r) {
        String packageName = r.mActivityComponent.getPackageName();
        int relaunchMode = getRelaunchModeFromBundle(packageName);
        if (!isEnabled() || IS_TABLET || r.mAtmService.mWindowManager.mPolicy.isDisplayFolded() || r.inMultiWindowMode() || isEmbedded(r) || isFixedAspectRatio(packageName)) {
            return relaunchMode;
        }
        if (this.mDemoMode) {
            return getRelaunchModeFromPolicy(this.mDemoModePolicy);
        }
        int policy = this.mFullScreenPackageManager.getOrientationPolicyByComponent(r.mActivityComponent.flattenToShortString());
        if (-1 != policy) {
            return getRelaunchModeFromPolicy(policy);
        }
        int policy2 = this.mFullScreenPackageManager.getOrientationPolicy(packageName);
        if (-1 != policy2) {
            return getRelaunchModeFromPolicy(policy2);
        }
        return relaunchMode;
    }

    private int getRotationOptionsFromPolicy(int policy) {
        int rotationOptions = 0;
        if ((policy & 128) != 0) {
            rotationOptions = 0 | 1;
        }
        if ((policy & 64) != 0) {
            return rotationOptions | 2;
        }
        return rotationOptions;
    }

    private int getRotationOptionsFromBundle(String packageName) {
        int rotationOptions = 0;
        if (!IS_TABLET || !isLandEnabledForPackage(packageName)) {
            return 0;
        }
        FullRule fullRule = this.mEmbeddedFullRules.get(packageName);
        if (fullRule.mNrs) {
            rotationOptions = 0 | 1;
        }
        if (fullRule.mNra) {
            return rotationOptions | 2;
        }
        return rotationOptions;
    }

    public int getRotationOptions(ActivityRecord r) {
        String packageName = r.mActivityComponent.getPackageName();
        int rotationOptions = getRotationOptionsFromBundle(packageName);
        if (!isEnabled() || IS_TABLET || r.mAtmService.mWindowManager.mPolicy.isDisplayFolded() || r.inMultiWindowMode() || isEmbedded(r) || isFixedAspectRatio(packageName)) {
            return rotationOptions;
        }
        if (this.mDemoMode) {
            return getRotationOptionsFromPolicy(this.mDemoModePolicy);
        }
        int policy = this.mFullScreenPackageManager.getOrientationPolicyByComponent(r.mActivityComponent.flattenToShortString());
        if (-1 != policy) {
            return getRotationOptionsFromPolicy(policy);
        }
        int policy2 = this.mFullScreenPackageManager.getOrientationPolicy(r.mActivityComponent.getPackageName());
        if (-1 != policy2) {
            return getRotationOptionsFromPolicy(policy2);
        }
        return rotationOptions;
    }

    private boolean retrieveScreenOrientationInner(ActivityRecord activityRecord, ActivityInfo activityInfo, Bundle appMetaData) {
        String screenInnerOrientation = "unset";
        if (appMetaData != null) {
            screenInnerOrientation = appMetaData.getString("miui.screenInnerOrientation", screenInnerOrientation);
        }
        int appSpecOrientation = screenOrientationFromString(screenInnerOrientation);
        String str = "invalid";
        if (activityInfo.metaData != null) {
            str = activityInfo.metaData.getString("miui.screenInnerOrientation", str);
        }
        String activityScreenOrientation = str;
        int activitySpecOrientation = screenOrientationFromString(activityScreenOrientation);
        if (activitySpecOrientation >= -2) {
            activityRecord.mScreenOrientationInner = activitySpecOrientation;
            return true;
        } else if (appSpecOrientation != -2) {
            activityRecord.mScreenOrientationInner = appSpecOrientation;
            return true;
        } else {
            return false;
        }
    }

    public void retrieveScreenOrientation(ActivityRecord activityRecord) {
        String appScreenOrientation;
        String activityScreenOrientation;
        ActivityInfo activityInfo = activityRecord.intent.resolveActivityInfo(activityRecord.mAtmService.mContext.getPackageManager(), 128);
        if (activityInfo == null) {
            return;
        }
        Bundle appMetaData = activityInfo.applicationInfo.metaData;
        if (retrieveScreenOrientationInner(activityRecord, activityInfo, appMetaData)) {
            return;
        }
        if (appMetaData != null) {
            appScreenOrientation = appMetaData.getString("miui.screenOrientation", "unset");
        } else {
            appScreenOrientation = "unset";
        }
        if (activityInfo.metaData != null) {
            activityScreenOrientation = activityInfo.metaData.getString("miui.screenOrientation", "unset");
        } else {
            activityScreenOrientation = "unset";
        }
        if (!activityScreenOrientation.equals("unset")) {
            SparseArray<Integer> activitySpecOrientation = parseAppRequestOrientation(activityScreenOrientation);
            for (int i = 0; i < activitySpecOrientation.size(); i++) {
                int key = activitySpecOrientation.keyAt(i);
                setOrientationToActivityRecord(activityRecord, key, activitySpecOrientation);
            }
        } else if (!appScreenOrientation.equals("unset")) {
            SparseArray<Integer> appSpecOrientation = parseAppRequestOrientation(appScreenOrientation);
            for (int i2 = 0; i2 < appSpecOrientation.size(); i2++) {
                int key2 = appSpecOrientation.keyAt(i2);
                setOrientationToActivityRecord(activityRecord, key2, appSpecOrientation);
            }
        }
    }

    private void setOrientationToActivityRecord(ActivityRecord r, int key, SparseArray<Integer> activitySpecOrientation) {
        switch (key) {
            case 1:
                r.mScreenOrientationInner = activitySpecOrientation.get(key).intValue();
                return;
            case 2:
                r.mScreenOrientationOuter = activitySpecOrientation.get(key).intValue();
                return;
            case 3:
                r.mScreenOrientationPad = activitySpecOrientation.get(key).intValue();
                return;
            default:
                return;
        }
    }

    private boolean needShowToast(ActivityRecord r) {
        boolean z = false;
        if (!isEnabled() || IS_TABLET || r.mAtmService.mWindowManager.mPolicy.isDisplayFolded() || r.inMultiWindowMode() || isEmbedded(r) || isFixedAspectRatio(r.mActivityComponent.getPackageName()) || !r.isState(ActivityRecord.State.RESUMED) || !ActivityInfo.isFixedOrientationPortrait(r.mOriginRequestOrientation) || r.mDisplayContent.getRotation() == 0) {
            return false;
        }
        if (this.mDemoMode) {
            return (this.mDemoModePolicy & 8) != 0;
        }
        int policy = this.mFullScreenPackageManager.getOrientationPolicyByComponent(r.mActivityComponent.flattenToShortString());
        if (-1 != policy) {
            if ((policy & 8) != 0) {
                z = true;
            }
            boolean showToast = z;
            return showToast;
        }
        int policy2 = this.mFullScreenPackageManager.getOrientationPolicy(r.mActivityComponent.getPackageName());
        if (-1 == policy2) {
            return false;
        }
        if ((policy2 & 8) != 0) {
            z = true;
        }
        boolean showToast2 = z;
        return showToast2;
    }

    public void showToastWhenLandscapeIfNeed(ActivityRecord activityRecord) {
        if (needShowToast(activityRecord) && getOrientationMode(activityRecord, activityRecord.mOriginRequestOrientation) == 1) {
            DisplayThread.getHandler().post(new Runnable() { // from class: com.android.server.wm.MiuiOrientationImpl$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    MiuiOrientationImpl.this.m1805x78dcbfb8();
                }
            });
        } else if (isEnabled() && this.mToastVisible && !activityRecord.inMultiWindowMode() && activityRecord.isState(ActivityRecord.State.RESUMED)) {
            DisplayThread.getHandler().post(new Runnable() { // from class: com.android.server.wm.MiuiOrientationImpl$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    MiuiOrientationImpl.this.m1806x5e1e2e79();
                }
            });
        }
    }

    /* renamed from: lambda$showToastWhenLandscapeIfNeed$0$com-android-server-wm-MiuiOrientationImpl */
    public /* synthetic */ void m1805x78dcbfb8() {
        this.mCompatLandToast.setText(286196347);
        this.mToastVisible = true;
        this.mCompatLandToast.show();
    }

    /* renamed from: lambda$showToastWhenLandscapeIfNeed$1$com-android-server-wm-MiuiOrientationImpl */
    public /* synthetic */ void m1806x5e1e2e79() {
        this.mToastVisible = false;
        this.mCompatLandToast.cancel();
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    public static int screenOrientationFromString(String orientation) {
        char c;
        switch (orientation.hashCode()) {
            case -1883156447:
                if (orientation.equals("sensorLandscape")) {
                    c = '\b';
                    break;
                }
                c = 65535;
                break;
            case -1711546887:
                if (orientation.equals("reverseLandscape")) {
                    c = '\n';
                    break;
                }
                c = 65535;
                break;
            case -1626174665:
                if (orientation.equals("unspecified")) {
                    c = 1;
                    break;
                }
                c = 65535;
                break;
            case -1392832198:
                if (orientation.equals("behind")) {
                    c = 5;
                    break;
                }
                c = 65535;
                break;
            case -1097452790:
                if (orientation.equals("locked")) {
                    c = 16;
                    break;
                }
                c = 65535;
                break;
            case -905948230:
                if (orientation.equals("sensor")) {
                    c = 6;
                    break;
                }
                c = 65535;
                break;
            case -900318416:
                if (orientation.equals("userLandscape")) {
                    c = '\r';
                    break;
                }
                c = 65535;
                break;
            case -804324567:
                if (orientation.equals("fullSensor")) {
                    c = '\f';
                    break;
                }
                c = 65535;
                break;
            case -757567331:
                if (orientation.equals("reversePortrait")) {
                    c = 11;
                    break;
                }
                c = 65535;
                break;
            case -486008459:
                if (orientation.equals("sensorPortrait")) {
                    c = '\t';
                    break;
                }
                c = 65535;
                break;
            case -38662010:
                if (orientation.equals("userPortrait")) {
                    c = 14;
                    break;
                }
                c = 65535;
                break;
            case 3599307:
                if (orientation.equals("user")) {
                    c = 4;
                    break;
                }
                c = 65535;
                break;
            case 111442729:
                if (orientation.equals("unset")) {
                    c = 0;
                    break;
                }
                c = 65535;
                break;
            case 729267099:
                if (orientation.equals("portrait")) {
                    c = 3;
                    break;
                }
                c = 65535;
                break;
            case 1331077882:
                if (orientation.equals("fullUser")) {
                    c = 15;
                    break;
                }
                c = 65535;
                break;
            case 1430647483:
                if (orientation.equals("landscape")) {
                    c = 2;
                    break;
                }
                c = 65535;
                break;
            case 1553288379:
                if (orientation.equals("nosensor")) {
                    c = 7;
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
                return -2;
            case 1:
                return -1;
            case 2:
                return 0;
            case 3:
                return 1;
            case 4:
                return 2;
            case 5:
                return 3;
            case 6:
                return 4;
            case 7:
                return 5;
            case '\b':
                return 6;
            case '\t':
                return 7;
            case '\n':
                return 8;
            case 11:
                return 9;
            case '\f':
                return 10;
            case '\r':
                return 11;
            case 14:
                return 12;
            case 15:
                return 13;
            case 16:
                return 14;
            default:
                return -3;
        }
    }

    public void update3appCameraWhenResume(ActivityRecord r) {
        if (r == null) {
            return;
        }
        if ((!isEnabled() && !IS_TABLET) || isEmbedded(r) || MiuiFreeFormManager.openCameraInFreeForm(r.packageName)) {
            return;
        }
        this.mFullScreen3AppCameraStrategy.update3appCameraRotate(r);
        this.mFullScreen3AppCameraStrategy.update3appCameraRotation(r, r.mDisplayContent.getRotation(), false, false);
    }

    public void update3appCameraWhenRotateOrFold(ActivityRecord r, int rotation, boolean isRotated, boolean isFoldChanged) {
        if (r == null) {
            return;
        }
        if ((!isEnabled() && !IS_TABLET) || isEmbedded(r) || MiuiFreeFormManager.openCameraInFreeForm(r.packageName)) {
            return;
        }
        this.mFullScreen3AppCameraStrategy.update3appCameraRotation(r, rotation, isRotated, isFoldChanged);
    }

    public void setUserRotationLockedForSettings(int userRotationMode, int userRotation, boolean isFolded) {
        if (MiuiOrientationStub.get().isEnabled() && IS_FOLD) {
            ContentResolver res = this.mContext.getContentResolver();
            if (isFolded) {
                Settings.System.putIntForUser(res, "accelerometer_rotation_outer", userRotationMode, -2);
            } else {
                Settings.System.putIntForUser(res, "accelerometer_rotation_inner", userRotationMode, -2);
            }
        }
    }

    public int getUserRotationLockedFromSettings(boolean isFolded) {
        ContentResolver resolver = this.mContext.getContentResolver();
        return isFolded ? Settings.System.getIntForUser(resolver, "accelerometer_rotation_outer", 0, -2) != 0 ? 0 : 1 : Settings.System.getIntForUser(resolver, "accelerometer_rotation_inner", 1, -2) != 0 ? 0 : 1;
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args, int opti, boolean dumpAll, boolean dumpClient, String dumpPackage) {
        if (!isEnabled()) {
            pw.println("miuiSmartRotation not enabled");
            return;
        }
        pw.println("miuiSmartRotation PACKAGE SETTINGS MANAGER");
        this.mFullScreenPackageManager.dump(pw, "");
        pw.println();
    }

    public void gotoDemoDebugMode(PrintWriter pw, String value) {
        if (!isEnabled()) {
            pw.println("miuiSmartRotation not enabled");
            return;
        }
        boolean z = !this.mDemoMode;
        this.mDemoMode = z;
        this.mDemoModePolicy = 0;
        if (z) {
            this.mDemoModePolicy = parseFullScreenPolicy(value);
        }
        pw.println("goto or leave debug mode=" + this.mDemoMode + " policy=" + fullScreenPolicyToString(this.mDemoModePolicy));
    }

    private static AtomicFile getSettingsFile() {
        File settingsFile = new File(Environment.getDataDirectory(), ORIENTATION_FILE_PATH);
        return new AtomicFile(settingsFile, ORIENTATION_COMMIT_TAG);
    }

    private static HashMap<String, Integer> readSettings(AtomicFile file) {
        TypedXmlPullParser parser;
        int type;
        try {
            InputStream stream = file.openRead();
            boolean success = false;
            HashMap<String, Integer> settings = new HashMap<>();
            try {
                try {
                    try {
                        parser = Xml.resolvePullParser(stream);
                        while (true) {
                            type = parser.next();
                            if (type == 2 || type == 1) {
                                break;
                            }
                        }
                    } catch (IOException | IllegalStateException | IndexOutOfBoundsException | NullPointerException | NumberFormatException | XmlPullParserException e) {
                        Slog.w(TAG, "Failed parsing " + e);
                        stream.close();
                    }
                } catch (IOException ignored) {
                    ignored.printStackTrace();
                }
                if (type != 2) {
                    throw new IllegalStateException("no start tag found");
                }
                int outerDepth = parser.getDepth();
                while (true) {
                    int type2 = parser.next();
                    if (type2 == 1 || (type2 == 3 && parser.getDepth() <= outerDepth)) {
                        break;
                    } else if (type2 != 3 && type2 != 4) {
                        String tagName = parser.getName();
                        if (tagName.equals(ORIENTATION_TAG_PACKAGE)) {
                            readPackage(parser, settings);
                        } else {
                            Slog.w(TAG, "Unknown element under <orientation-settings>: " + parser.getName());
                            XmlUtils.skipCurrentTag(parser);
                        }
                    }
                }
                success = true;
                stream.close();
                if (!success) {
                    settings.clear();
                }
                Slog.d(TAG, "read orientation settings: " + success);
                return settings;
            } catch (Throwable th) {
                try {
                    stream.close();
                } catch (IOException ignored2) {
                    ignored2.printStackTrace();
                }
                throw th;
            }
        } catch (IOException e2) {
            Slog.i(TAG, "No existing orientation settings, starting empty");
            return null;
        }
    }

    private static int getIntAttribute(TypedXmlPullParser parser, String name, int defaultValue) {
        return parser.getAttributeInt((String) null, name, defaultValue);
    }

    private static void readPackage(TypedXmlPullParser parser, HashMap<String, Integer> settings) throws NumberFormatException, XmlPullParserException, IOException {
        String name = parser.getAttributeValue((String) null, "name");
        if (name != null) {
            int value = getIntAttribute(parser, "value", 0);
            settings.put(name, Integer.valueOf(value));
        }
        XmlUtils.skipCurrentTag(parser);
    }

    /* JADX WARN: Code restructure failed: missing block: B:17:0x006c, code lost:
        if (0 == 0) goto L13;
     */
    /* JADX WARN: Code restructure failed: missing block: B:18:0x006f, code lost:
        android.util.Slog.d(com.android.server.wm.MiuiOrientationImpl.TAG, "write orientation settings: " + r4);
     */
    /* JADX WARN: Code restructure failed: missing block: B:19:0x0085, code lost:
        return;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public static void writeSettings(android.util.AtomicFile r12, java.util.Map<java.lang.String, java.lang.Integer> r13) {
        /*
            java.lang.String r0 = "package"
            java.lang.String r1 = "orientation-settings"
            java.lang.String r2 = "MiuiOrientationImpl"
            java.io.FileOutputStream r3 = r12.startWrite()     // Catch: java.io.IOException -> L90
            r4 = 0
            android.util.TypedXmlSerializer r5 = android.util.Xml.resolveSerializer(r3)     // Catch: java.lang.Throwable -> L63 java.io.IOException -> L65
            r6 = 1
            java.lang.Boolean r6 = java.lang.Boolean.valueOf(r6)     // Catch: java.lang.Throwable -> L63 java.io.IOException -> L65
            r7 = 0
            r5.startDocument(r7, r6)     // Catch: java.lang.Throwable -> L63 java.io.IOException -> L65
            r5.startTag(r7, r1)     // Catch: java.lang.Throwable -> L63 java.io.IOException -> L65
            java.util.Set r6 = r13.entrySet()     // Catch: java.lang.Throwable -> L63 java.io.IOException -> L65
            java.util.Iterator r6 = r6.iterator()     // Catch: java.lang.Throwable -> L63 java.io.IOException -> L65
        L24:
            boolean r8 = r6.hasNext()     // Catch: java.lang.Throwable -> L63 java.io.IOException -> L65
            if (r8 == 0) goto L52
            java.lang.Object r8 = r6.next()     // Catch: java.lang.Throwable -> L63 java.io.IOException -> L65
            java.util.Map$Entry r8 = (java.util.Map.Entry) r8     // Catch: java.lang.Throwable -> L63 java.io.IOException -> L65
            java.lang.Object r9 = r8.getKey()     // Catch: java.lang.Throwable -> L63 java.io.IOException -> L65
            java.lang.String r9 = (java.lang.String) r9     // Catch: java.lang.Throwable -> L63 java.io.IOException -> L65
            java.lang.Object r10 = r8.getValue()     // Catch: java.lang.Throwable -> L63 java.io.IOException -> L65
            java.lang.Integer r10 = (java.lang.Integer) r10     // Catch: java.lang.Throwable -> L63 java.io.IOException -> L65
            int r10 = r10.intValue()     // Catch: java.lang.Throwable -> L63 java.io.IOException -> L65
            r5.startTag(r7, r0)     // Catch: java.lang.Throwable -> L63 java.io.IOException -> L65
            java.lang.String r11 = "name"
            r5.attribute(r7, r11, r9)     // Catch: java.lang.Throwable -> L63 java.io.IOException -> L65
            java.lang.String r11 = "value"
            r5.attributeInt(r7, r11, r10)     // Catch: java.lang.Throwable -> L63 java.io.IOException -> L65
            r5.endTag(r7, r0)     // Catch: java.lang.Throwable -> L63 java.io.IOException -> L65
            goto L24
        L52:
            r5.endTag(r7, r1)     // Catch: java.lang.Throwable -> L63 java.io.IOException -> L65
            r5.endDocument()     // Catch: java.lang.Throwable -> L63 java.io.IOException -> L65
            r4 = 1
            if (r4 == 0) goto L5f
        L5b:
            r12.finishWrite(r3)
            goto L6f
        L5f:
            r12.failWrite(r3)
            goto L6f
        L63:
            r0 = move-exception
            goto L86
        L65:
            r0 = move-exception
            java.lang.String r1 = "Failed to write orientation user settings."
            android.util.Slog.w(r2, r1, r0)     // Catch: java.lang.Throwable -> L63
            if (r4 == 0) goto L5f
            goto L5b
        L6f:
            java.lang.StringBuilder r0 = new java.lang.StringBuilder
            r0.<init>()
            java.lang.String r1 = "write orientation settings: "
            java.lang.StringBuilder r0 = r0.append(r1)
            java.lang.StringBuilder r0 = r0.append(r4)
            java.lang.String r0 = r0.toString()
            android.util.Slog.d(r2, r0)
            return
        L86:
            if (r4 == 0) goto L8c
            r12.finishWrite(r3)
            goto L8f
        L8c:
            r12.failWrite(r3)
        L8f:
            throw r0
        L90:
            r0 = move-exception
            java.lang.StringBuilder r1 = new java.lang.StringBuilder
            r1.<init>()
            java.lang.String r3 = "Failed to write orientation settings: "
            java.lang.StringBuilder r1 = r1.append(r3)
            java.lang.StringBuilder r1 = r1.append(r0)
            java.lang.String r1 = r1.toString()
            android.util.Slog.w(r2, r1)
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wm.MiuiOrientationImpl.writeSettings(android.util.AtomicFile, java.util.Map):void");
    }

    /* loaded from: classes.dex */
    public class FullScreenPackageManager {
        private Context mContext;
        private ActivityTaskManagerServiceImpl mServiceImpl;
        final Consumer<ConcurrentHashMap<String, String>> mFullScreenPackagelistCallback = new Consumer() { // from class: com.android.server.wm.MiuiOrientationImpl$FullScreenPackageManager$$ExternalSyntheticLambda0
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                MiuiOrientationImpl.FullScreenPackageManager.this.m1809xdf8e70f1((ConcurrentHashMap) obj);
            }
        };
        final Consumer<ConcurrentHashMap<String, String>> mFullScreenComponentCallback = new Consumer() { // from class: com.android.server.wm.MiuiOrientationImpl$FullScreenPackageManager$$ExternalSyntheticLambda1
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                MiuiOrientationImpl.FullScreenPackageManager.this.m1810x7bfc6d50((ConcurrentHashMap) obj);
            }
        };

        public FullScreenPackageManager(Context context, ActivityTaskManagerServiceImpl serviceImpl) {
            MiuiOrientationImpl.this = this$0;
            this.mContext = context;
            this.mServiceImpl = serviceImpl;
        }

        /* renamed from: lambda$new$1$com-android-server-wm-MiuiOrientationImpl$FullScreenPackageManager */
        public /* synthetic */ void m1809xdf8e70f1(ConcurrentHashMap map) {
            MiuiOrientationImpl.this.mPackagesMapBySystem.clear();
            map.forEach(new BiConsumer() { // from class: com.android.server.wm.MiuiOrientationImpl$FullScreenPackageManager$$ExternalSyntheticLambda2
                @Override // java.util.function.BiConsumer
                public final void accept(Object obj, Object obj2) {
                    MiuiOrientationImpl.FullScreenPackageManager.this.m1808x43207492((String) obj, (String) obj2);
                }
            });
        }

        /* renamed from: lambda$new$0$com-android-server-wm-MiuiOrientationImpl$FullScreenPackageManager */
        public /* synthetic */ void m1808x43207492(String key, String value) {
            MiuiOrientationImpl.this.mPackagesMapBySystem.put(key, Integer.valueOf(MiuiOrientationImpl.parseFullScreenPolicy(value)));
        }

        /* renamed from: lambda$new$2$com-android-server-wm-MiuiOrientationImpl$FullScreenPackageManager */
        public /* synthetic */ void m1810x7bfc6d50(ConcurrentHashMap map) {
            MiuiOrientationImpl.this.mComponentMapBySystem.clear();
            map.forEach(new BiConsumer<String, String>() { // from class: com.android.server.wm.MiuiOrientationImpl.FullScreenPackageManager.1
                public void accept(String key, String value) {
                    MiuiOrientationImpl.this.mComponentMapBySystem.put(key, Integer.valueOf(MiuiOrientationImpl.parseFullScreenPolicy(value)));
                }
            });
        }

        public void dump(PrintWriter pw, String prefix) {
            String innerPrefix = prefix + "  ";
            if (!MiuiOrientationImpl.this.mComponentMapBySystem.isEmpty()) {
                pw.println(prefix + "SmartRotationConfig(Activity)");
                for (Map.Entry<String, Integer> entry : MiuiOrientationImpl.this.mComponentMapBySystem.entrySet()) {
                    pw.println(innerPrefix + "[" + entry.getKey() + "] " + MiuiOrientationImpl.fullScreenPolicyToString(entry.getValue().intValue()));
                }
            }
            pw.println();
            if (!MiuiOrientationImpl.this.mPackagesMapBySystem.isEmpty()) {
                pw.println(prefix + "SmartRotationConfig(Package)");
                for (Map.Entry<String, Integer> entry2 : MiuiOrientationImpl.this.mPackagesMapBySystem.entrySet()) {
                    pw.println(innerPrefix + "[" + entry2.getKey() + "] " + MiuiOrientationImpl.fullScreenPolicyToString(entry2.getValue().intValue()));
                }
            }
            pw.println();
            if (MiuiOrientationImpl.this.mPackagesMapByUserSettings != null && !MiuiOrientationImpl.this.mPackagesMapByUserSettings.isEmpty()) {
                pw.println(prefix + "SmartRotationConfig(UserSetting)");
                for (Map.Entry<String, Integer> entry3 : MiuiOrientationImpl.this.mPackagesMapByUserSettings.entrySet()) {
                    pw.println(innerPrefix + "[" + entry3.getKey() + "] " + MiuiOrientationImpl.fullScreenPolicyToString(entry3.getValue().intValue()));
                }
            }
        }

        public int getOrientationMode(String packageName) {
            int modeByUserSettings = getOrientationModeByUserSettings(packageName);
            return modeByUserSettings == -1 ? getOrientationModeBySystem(packageName) : modeByUserSettings;
        }

        public int getOrientationPolicyByComponent(String packageName) {
            Object value = MiuiOrientationImpl.this.mComponentMapBySystem.get(packageName);
            if (value instanceof Integer) {
                return ((Integer) value).intValue();
            }
            return -1;
        }

        public int getOrientationModeBySystem(String packageName) {
            Object value = MiuiOrientationImpl.this.mPackagesMapBySystem.get(packageName);
            return value instanceof Integer ? 1 : -1;
        }

        public int getOrientationModeByUserSettings(String packageName) {
            if (MiuiOrientationImpl.this.mPackagesMapByUserSettings != null) {
                Object value = MiuiOrientationImpl.this.mPackagesMapByUserSettings.get(packageName);
                if (value instanceof Integer) {
                    return 1;
                }
                return -1;
            }
            return -1;
        }

        public int getOrientationPolicy(String packageName) {
            int modeByUserSettings = getOrientationPolicyByUserSettings(packageName);
            return modeByUserSettings == -1 ? getOrientationPolicyBySystem(packageName) : modeByUserSettings;
        }

        public int getOrientationPolicyBySystem(String packageName) {
            Object value = MiuiOrientationImpl.this.mPackagesMapBySystem.get(packageName);
            if (value instanceof Integer) {
                return ((Integer) value).intValue();
            }
            return -1;
        }

        public int getOrientationPolicyByUserSettings(String packageName) {
            if (MiuiOrientationImpl.this.mPackagesMapByUserSettings != null) {
                Object value = MiuiOrientationImpl.this.mPackagesMapByUserSettings.get(packageName);
                if (value instanceof Integer) {
                    return ((Integer) value).intValue();
                }
                return -1;
            }
            return -1;
        }

        public void clearUserSettings() {
            if (MiuiOrientationImpl.this.mPackagesMapByUserSettings != null) {
                MiuiOrientationImpl.this.mPackagesMapByUserSettings.clear();
            }
        }

        public void setUserSettings(String pkgName, String value) {
            if (!TextUtils.isEmpty(value) && value.equals(MiuiOrientationImpl.ORIENTATION_USER_SETTINGS_REMOVE)) {
                MiuiOrientationImpl.this.mPackagesMapByUserSettings.remove(pkgName);
            } else if (TextUtils.isEmpty(value)) {
                MiuiOrientationImpl.this.mPackagesMapByUserSettings.put(pkgName, 0);
            } else {
                int policy = MiuiOrientationImpl.parseFullScreenPolicy(value);
                MiuiOrientationImpl.this.mPackagesMapByUserSettings.put(pkgName, Integer.valueOf(policy));
            }
            MiuiOrientationImpl.this.mFileHandler.removeCallbacks(MiuiOrientationImpl.this.mWriteSettingsRunnable);
            MiuiOrientationImpl.this.mFileHandler.post(MiuiOrientationImpl.this.mWriteSettingsRunnable);
        }

        public void setUserSettings(String[] pkgNames, String value) {
            int policy = 0;
            HashMap hashMap = new HashMap();
            List<String> tmpPolicyRemoveList = new ArrayList<>();
            int i = 0;
            if (!TextUtils.isEmpty(value) && value.equals(MiuiOrientationImpl.ORIENTATION_USER_SETTINGS_REMOVE)) {
                int length = pkgNames.length;
                while (i < length) {
                    String packageName = pkgNames[i];
                    tmpPolicyRemoveList.add(packageName);
                    i++;
                }
            } else {
                if (!TextUtils.isEmpty(value)) {
                    policy = MiuiOrientationImpl.parseFullScreenPolicy(value);
                }
                int length2 = pkgNames.length;
                while (i < length2) {
                    String packageName2 = pkgNames[i];
                    hashMap.put(packageName2, Integer.valueOf(policy));
                    i++;
                }
            }
            for (String name : tmpPolicyRemoveList) {
                MiuiOrientationImpl.this.mPackagesMapByUserSettings.remove(name);
            }
            MiuiOrientationImpl.this.mPackagesMapByUserSettings.putAll(hashMap);
            MiuiOrientationImpl.this.mFileHandler.removeCallbacks(MiuiOrientationImpl.this.mWriteSettingsRunnable);
            MiuiOrientationImpl.this.mFileHandler.post(MiuiOrientationImpl.this.mWriteSettingsRunnable);
        }
    }

    /* loaded from: classes.dex */
    public class FullScreen3AppCameraStrategy {
        private static final int DEFAULT_CAMERA_ROTATION = 0;
        private static final String EMBEDDED_PROP_3APP_CAMERA_ROTATION = "persist.vendor.device.orientation";
        private static final String EMBEDDED_PROP_3APP_ROTATE_CAMERA = "persist.vendor.multiwin.3appcam";
        private static final int MSG_SET_CAMERA_ROTATE = 1001;
        private static final int MSG_SET_CAMERA_ROTATION = 1002;
        private final Handler mBgHandler = new Handler(BackgroundThread.getHandler().getLooper()) { // from class: com.android.server.wm.MiuiOrientationImpl.FullScreen3AppCameraStrategy.1
            @Override // android.os.Handler
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case FullScreen3AppCameraStrategy.MSG_SET_CAMERA_ROTATE /* 1001 */:
                        try {
                            Slog.d(MiuiOrientationImpl.TAG, "SET persist.vendor.multiwin.3appcam :" + msg.obj);
                            SystemProperties.set(FullScreen3AppCameraStrategy.EMBEDDED_PROP_3APP_ROTATE_CAMERA, String.valueOf(msg.obj));
                            return;
                        } catch (RuntimeException e) {
                            Slog.e(MiuiOrientationImpl.TAG, "Exception persist.vendor.multiwin.3appcam e: ", e);
                            return;
                        }
                    case FullScreen3AppCameraStrategy.MSG_SET_CAMERA_ROTATION /* 1002 */:
                        try {
                            Slog.d(MiuiOrientationImpl.TAG, "SET persist.vendor.device.orientation :" + msg.obj);
                            SystemProperties.set(FullScreen3AppCameraStrategy.EMBEDDED_PROP_3APP_CAMERA_ROTATION, String.valueOf(msg.obj));
                            return;
                        } catch (RuntimeException e2) {
                            Slog.e(MiuiOrientationImpl.TAG, "Exception persist.vendor.device.orientation e: ", e2);
                            return;
                        }
                    default:
                        return;
                }
            }
        };
        private int mLastInitRotation;
        private boolean mLastSetCameraRotation;
        private boolean mLastSetRotateCamera;

        FullScreen3AppCameraStrategy() {
            MiuiOrientationImpl.this = this$0;
            BackgroundThread.get();
        }

        public void update3appCameraRotate(ActivityRecord r) {
            int rotate = 0;
            boolean rotateCamera = (MiuiOrientationImpl.this.isNeedCameraRotate(r) && !r.mAtmService.mWindowManager.mPolicy.isDisplayFolded()) || MiuiOrientationImpl.this.isNeedCameraRotateInPad(r);
            if (this.mLastSetRotateCamera == rotateCamera) {
                return;
            }
            if (rotateCamera) {
                rotate = 1;
            }
            Message message = Message.obtain(this.mBgHandler, MSG_SET_CAMERA_ROTATE, Integer.valueOf(rotate));
            message.sendToTarget();
            this.mLastSetRotateCamera = rotateCamera;
        }

        public void update3appCameraRotation(ActivityRecord r, int rotation, boolean isRotated, boolean isFoldChanged) {
            boolean z = true;
            int i = 0;
            boolean shouldRotate = (MiuiOrientationImpl.this.isNeedCameraRotate(r) && !r.mAtmService.mWindowManager.mPolicy.isDisplayFolded()) || MiuiOrientationImpl.this.isNeedCameraRotateInPad(r);
            if (shouldRotate || isFoldChanged) {
                if (!shouldRotate || (!isRotated && !MiuiOrientationImpl.this.isNeedRotateWhenCameraResume(r) && !MiuiOrientationImpl.this.isFixedAspectRatio(r.mActivityComponent.getPackageName()) && !MiuiOrientationImpl.this.isNeedRotateWhenCameraResumeInPad(r))) {
                    z = false;
                }
                this.mLastSetCameraRotation = z;
                if (!isRotated && !isFoldChanged) {
                    this.mLastInitRotation = rotation;
                }
                if (z) {
                    i = rotation;
                }
                int rotationToSet = i;
                if (needRecalculateCameraRotation(r) && (isRotated || isFoldChanged)) {
                    rotationToSet = RotationUtils.deltaRotation(this.mLastInitRotation, rotationToSet);
                }
                Message message = Message.obtain(this.mBgHandler, MSG_SET_CAMERA_ROTATION, Integer.valueOf(rotationToSet));
                message.sendToTarget();
            }
        }

        private boolean needRecalculateCameraRotation(ActivityRecord r) {
            return MiuiOrientationImpl.this.isNeedCameraRotate(r) && !MiuiOrientationImpl.this.isNeedRotateWhenCameraResume(r) && !MiuiOrientationImpl.this.isFixedAspectRatio(r.mActivityComponent.getPackageName()) && !MiuiOrientationImpl.this.isNeedRotateWhenCameraResumeInPad(r);
        }
    }

    /* loaded from: classes.dex */
    public class FullRule {
        boolean mCr;
        boolean mEnable;
        boolean mNr;
        boolean mNra;
        boolean mNrs;
        String mPkg;
        boolean mR;
        boolean mRcr;
        boolean mRi;

        public FullRule(String mPkg, boolean mEnable, boolean mNra, boolean mNrs, boolean mCr, boolean mRcr, boolean mNr, boolean mR, boolean mRi) {
            MiuiOrientationImpl.this = this$0;
            this.mPkg = mPkg;
            this.mEnable = mEnable;
            this.mNra = mNra;
            this.mNrs = mNrs;
            this.mCr = mCr;
            this.mRcr = mRcr;
            this.mNr = mNr;
            this.mR = mR;
            this.mRi = mRi;
        }
    }

    static String fullScreenModeToString(int mode) {
        switch (mode) {
            case -1:
                return "unspecified";
            case 0:
            default:
                return "UnKnown(" + mode + ")";
            case 1:
                return "landscape";
            case 2:
                return "fixedorientation";
        }
    }

    static SparseArray parseAppRequestOrientation(String value) {
        String[] orientationStr = new String[0];
        SparseArray<Integer> orientationArray = new SparseArray<>();
        if (!TextUtils.isEmpty(value)) {
            orientationStr = value.split(",");
        }
        for (String str : orientationStr) {
            String[] orientationForDiffDevice = str.split("\\.");
            if (orientationForDiffDevice.length == 2) {
                orientationArray.append(Integer.parseInt(orientationForDiffDevice[0]), Integer.valueOf(screenOrientationFromString(orientationForDiffDevice[1])));
            }
        }
        return orientationArray;
    }

    static int parseFullScreenPolicy(String value) {
        String[] policys = new String[0];
        int policy = 0;
        if (!TextUtils.isEmpty(value)) {
            policys = value.split(":");
        }
        for (String str : policys) {
            policy |= fullScreenStringToPolicy(str);
        }
        return policy;
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    static int fullScreenStringToPolicy(String policyStr) {
        char c;
        switch (policyStr.hashCode()) {
            case 98:
                if (policyStr.equals(FoldablePackagePolicy.POLICY_VALUE_BLOCK_LIST)) {
                    c = 5;
                    break;
                }
                c = 65535;
                break;
            case 114:
                if (policyStr.equals(FoldablePackagePolicy.POLICY_VALUE_RESTART_LIST)) {
                    c = 1;
                    break;
                }
                c = 65535;
                break;
            case 116:
                if (policyStr.equals("t")) {
                    c = 3;
                    break;
                }
                c = 65535;
                break;
            case 119:
                if (policyStr.equals(FoldablePackagePolicy.POLICY_VALUE_ALLOW_LIST)) {
                    c = 4;
                    break;
                }
                c = 65535;
                break;
            case 3183:
                if (policyStr.equals("cr")) {
                    c = '\t';
                    break;
                }
                c = 65535;
                break;
            case 3524:
                if (policyStr.equals("nr")) {
                    c = 0;
                    break;
                }
                c = 65535;
                break;
            case 3639:
                if (policyStr.equals("ri")) {
                    c = 2;
                    break;
                }
                c = 65535;
                break;
            case 3726:
                if (policyStr.equals("uc")) {
                    c = '\n';
                    break;
                }
                c = 65535;
                break;
            case 109341:
                if (policyStr.equals("nra")) {
                    c = 6;
                    break;
                }
                c = 65535;
                break;
            case 109359:
                if (policyStr.equals("nrs")) {
                    c = 7;
                    break;
                }
                c = 65535;
                break;
            case 112737:
                if (policyStr.equals("rcr")) {
                    c = '\b';
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
                return 1;
            case 1:
                return 2;
            case 2:
                return 4;
            case 3:
                return 8;
            case 4:
                return 16;
            case 5:
                return 32;
            case 6:
                return 64;
            case 7:
                return 128;
            case '\b':
                return 256;
            case '\t':
                return 512;
            case '\n':
                return 1024;
            default:
                return 0;
        }
    }

    static String fullScreenPolicyToString(int policy) {
        StringBuilder sb = new StringBuilder();
        if ((policy & 1) != 0) {
            sb.append("nr:");
        }
        if ((policy & 2) != 0) {
            sb.append("r:");
        }
        if ((policy & 4) != 0) {
            sb.append("ri:");
        }
        if ((policy & 8) != 0) {
            sb.append("t:");
        }
        if ((policy & 16) != 0) {
            sb.append("w:");
        }
        if ((policy & 32) != 0) {
            sb.append("b:");
        }
        if ((policy & 64) != 0) {
            sb.append("nra:");
        }
        if ((policy & 128) != 0) {
            sb.append("nrs:");
        }
        if ((policy & 256) != 0) {
            sb.append("rcr:");
        }
        if ((policy & 512) != 0) {
            sb.append("cr:");
        }
        if ((policy & 1024) != 0) {
            sb.append("uc:");
        }
        if (sb.length() > 0) {
            return sb.deleteCharAt(sb.length() - 1).toString();
        }
        return sb.toString();
    }
}
