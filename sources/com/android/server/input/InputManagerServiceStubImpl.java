package com.android.server.input;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.SystemProperties;
import android.provider.MiuiSettings;
import android.provider.Settings;
import android.server.am.SplitScreenReporter;
import android.text.TextUtils;
import android.util.Slog;
import android.view.InputDevice;
import com.android.server.LocalServices;
import com.android.server.input.config.InputCommonConfig;
import com.android.server.input.config.InputDebugConfig;
import com.android.server.padkeyboard.MiuiPadKeyboardManager;
import com.android.server.policy.MiuiInputLog;
import com.android.server.policy.MiuiKeyShortcutManager;
import com.miui.base.MiuiStubRegistry;
import com.miui.server.input.PadManager;
import com.miui.server.input.util.MiuiCustomizeShortCutUtils;
import com.miui.server.magicpointer.MagicPointerCallbacks;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
/* loaded from: classes.dex */
public class InputManagerServiceStubImpl implements InputManagerServiceStub {
    private static final boolean BERSERK_MODE = SystemProperties.getBoolean("persist.berserk.mode.support", false);
    private static final String INPUT_LOG_LEVEL = "input_log_level";
    private static final String KEY_GAME_BOOSTER = "gb_boosting";
    private static final List<String> MIUI_INPUTMETHOD;
    private static final String SHOW_TOUCHES_PREVENTRECORDER = "show_touches_preventrecord";
    private Context mContext;
    private Handler mHandler;
    private MagicPointerCallbacks mMagicPointerCallbacks;
    private MiuiCustomizeShortCutUtils mMiuiCustomizeShortCutUtils;
    private int mPointerLocationShow;
    private long mPtr;
    private int mShowTouches;
    private int mShowTouchesPreventRecord;
    private final String TAG = "InputManagerServiceStubImpl";
    private int mInputDebugLevel = 0;
    private int mSetDebugLevel = 0;
    private int mFromSetInputLevel = 0;
    private boolean mCustomizeInputMethod = false;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<InputManagerServiceStubImpl> {

        /* compiled from: InputManagerServiceStubImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final InputManagerServiceStubImpl INSTANCE = new InputManagerServiceStubImpl();
        }

        public InputManagerServiceStubImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public InputManagerServiceStubImpl provideNewInstance() {
            return new InputManagerServiceStubImpl();
        }
    }

    static {
        ArrayList arrayList = new ArrayList();
        MIUI_INPUTMETHOD = arrayList;
        arrayList.add("com.baidu.input_mi");
        arrayList.add("com.sohu.inputmethod.sogou.xiaomi");
        arrayList.add("com.iflytek.inputmethod.miui");
    }

    public void init(Context context, Handler handler, long ptr) {
        Slog.d("InputManagerServiceStubImpl", "init");
        this.mContext = context;
        this.mHandler = handler;
        this.mPtr = ptr;
        switchPadMode(isPad());
    }

    public void registerStub() {
        Slog.d("InputManagerServiceStubImpl", "registerStub");
        registerSynergyModeSettingObserver();
        updateSynergyModeFromSettings();
        registerPointerLocationSettingObserver();
        updatePointerLocationFromSettings();
        registerTouchesPreventRecordSettingObserver();
        updateTouchesPreventRecorderFromSettings();
        registerDefaultInputMethodSettingObserver();
        updateDefaultInputMethodFromSettings();
        registerInputLevelSelect();
        updateFromInputLevelSetting();
        registerMiuiOptimizationObserver();
        if (PadManager.getInstance().isPad()) {
            registerMouseGestureSettingObserver();
            updateMouseGestureSettings();
        }
        if (BERSERK_MODE) {
            registerGameMode();
        }
    }

    public long getPtr() {
        return this.mPtr;
    }

    public boolean dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        Slog.d("InputManagerServiceStubImpl", "dump InputManagerServiceStubImpl");
        if (args.length == 2 && "debuglog".equals(args[0])) {
            try {
                pw.println("open Input debuglog, " + args[0] + " " + args[1]);
                this.mInputDebugLevel = Integer.parseInt(args[1]);
                InputDebugConfig inputDebugConfig = InputDebugConfig.getInstance();
                inputDebugConfig.setInputDebugFromDump(this.mInputDebugLevel);
                inputDebugConfig.flushToNative();
                MiuiInputLog.getInstance().setLogLevel(this.mInputDebugLevel);
                return true;
            } catch (ClassCastException e) {
                pw.println("open Reader and Dispatcher debuglog, ClassCastException!!!");
            }
        } else {
            String dumpStr = MiInputManager.getInstance().dump();
            pw.println(dumpStr);
        }
        return false;
    }

    public void setShowTouchLevelFromSettings(int showTouchesSwitch) {
        this.mShowTouches = showTouchesSwitch;
        updateDebugLevel();
    }

    public void updateFromUserSwitch() {
        updateSynergyModeFromSettings();
        updatePointerLocationFromSettings();
        updateTouchesPreventRecorderFromSettings();
        updateDefaultInputMethodFromSettings();
        updateFromInputLevelSetting();
        if (BERSERK_MODE) {
            updateOnewayModeFromSettings();
        }
    }

    public void notifySwitch(long whenNanos, int switchValues, int switchMask) {
        Slog.d("InputManagerServiceStubImpl", "notifySwitch: values=" + Integer.toHexString(switchValues) + ", mask=" + Integer.toHexString(switchMask));
    }

    public InputDevice[] filterKeyboardDeviceIfNeeded(InputDevice[] inputDevices) {
        MiuiPadKeyboardManager miuiPadKeyboardManager = (MiuiPadKeyboardManager) LocalServices.getService(MiuiPadKeyboardManager.class);
        if (miuiPadKeyboardManager == null) {
            return inputDevices;
        }
        InputDevice[] newInputDevices = miuiPadKeyboardManager.removeKeyboardDevicesIfNeeded(inputDevices);
        return newInputDevices;
    }

    public boolean interceptKeyboardNotification(Context context, List<InputDevice> keyboardsMissingLayout) {
        MiuiPadKeyboardManager miuiPadKeyboardManager = (MiuiPadKeyboardManager) LocalServices.getService(MiuiPadKeyboardManager.class);
        if (miuiPadKeyboardManager == null) {
            return false;
        }
        for (InputDevice device : keyboardsMissingLayout) {
            if (device != null && miuiPadKeyboardManager.getKeyboardDeviceId() == device.getId()) {
                Slog.i("InputManagerServiceStubImpl", "intercept notification for usb keyboard while screen is not on");
                return true;
            }
        }
        return false;
    }

    public void notifyTabletSwitchChanged(boolean tabletOpen) {
        MiuiPadKeyboardManager miuiPadKeyboardManager = (MiuiPadKeyboardManager) LocalServices.getService(MiuiPadKeyboardManager.class);
        if (miuiPadKeyboardManager != null) {
            miuiPadKeyboardManager.notifyTabletSwitchChanged(tabletOpen);
        }
    }

    private void registerSynergyModeSettingObserver() {
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("synergy_mode"), false, new ContentObserver(this.mHandler) { // from class: com.android.server.input.InputManagerServiceStubImpl.1
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                InputManagerServiceStubImpl.this.updateSynergyModeFromSettings();
            }
        }, -1);
    }

    public void updateSynergyModeFromSettings() {
        int synergyMode = Settings.Secure.getInt(this.mContext.getContentResolver(), "synergy_mode", 0);
        setSynergyMode(synergyMode);
    }

    private void setSynergyMode(int synergyMode) {
        Slog.d("InputManagerServiceStubImpl", "SynergyMode changed, mode = " + synergyMode);
        InputCommonConfig inputCommonConfig = InputCommonConfig.getInstance();
        inputCommonConfig.setSynergyMode(synergyMode);
        inputCommonConfig.flushToNative();
    }

    public void updateMouseGestureSettings() {
        boolean z = false;
        int mouseNaturalScroll = Settings.Secure.getInt(this.mContext.getContentResolver(), "mouse_gesture_naturalscroll", 0);
        if (mouseNaturalScroll != 0) {
            z = true;
        }
        switchMouseNaturalScrollStatus(z);
    }

    private void registerMouseGestureSettingObserver() {
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("mouse_gesture_naturalscroll"), false, new ContentObserver(this.mHandler) { // from class: com.android.server.input.InputManagerServiceStubImpl.2
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                InputManagerServiceStubImpl.this.updateMouseGestureSettings();
            }
        }, -1);
    }

    private void switchMouseNaturalScrollStatus(boolean mouseNaturalScrollStatus) {
        InputCommonConfig inputCommonConfig = InputCommonConfig.getInstance();
        inputCommonConfig.setMouseNaturalScrollStatus(mouseNaturalScrollStatus);
        inputCommonConfig.flushToNative();
    }

    public void switchPadMode(boolean padMode) {
        InputCommonConfig inputCommonConfig = InputCommonConfig.getInstance();
        inputCommonConfig.setPadMode(padMode);
        inputCommonConfig.flushToNative();
    }

    public void setInputMethodStatus(boolean shown) {
        InputCommonConfig inputCommonConfig = InputCommonConfig.getInstance();
        inputCommonConfig.setInputMethodStatus(shown, this.mCustomizeInputMethod);
        inputCommonConfig.flushToNative();
    }

    public void registerMiuiOptimizationObserver() {
        ContentObserver observer = new ContentObserver(null) { // from class: com.android.server.input.InputManagerServiceStubImpl.3
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                int i = 1;
                boolean isCtsMode = !SystemProperties.getBoolean("persist.sys.miui_optimization", !SplitScreenReporter.ACTION_ENTER_SPLIT.equals(SystemProperties.get("ro.miui.cts")));
                Slog.i("InputManagerServiceStubImpl", "ctsMode  is:" + isCtsMode);
                InputManagerServiceStubImpl.this.setCtsMode(isCtsMode);
                InputManagerServiceStubImpl.this.switchPadMode(!isCtsMode && PadManager.getInstance().isPad());
                if (InputManagerServiceStubImpl.this.isPad()) {
                    ContentResolver contentResolver = InputManagerServiceStubImpl.this.mContext.getContentResolver();
                    if (isCtsMode) {
                        i = 0;
                    }
                    Settings.System.putIntForUser(contentResolver, MiuiKeyShortcutManager.IS_CUSTOM_SHORTCUTS_EFFECTIVE, i, -2);
                }
            }
        };
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor(MiuiSettings.Secure.MIUI_OPTIMIZATION), false, observer, -2);
        observer.onChange(false);
    }

    public void setCtsMode(boolean ctsMode) {
        Slog.d("InputManagerServiceStubImpl", "CtsMode changed, mode = " + ctsMode);
        InputCommonConfig inputCommonConfig = InputCommonConfig.getInstance();
        inputCommonConfig.setCtsMode(ctsMode);
        inputCommonConfig.flushToNative();
    }

    private void updateDebugLevel() {
        int i = this.mShowTouches | this.mPointerLocationShow | this.mShowTouchesPreventRecord;
        this.mSetDebugLevel = i;
        int i2 = this.mFromSetInputLevel;
        if (i2 < 2) {
            this.mSetDebugLevel = i | i2;
        }
        InputDebugConfig inputDebugConfig = InputDebugConfig.getInstance();
        inputDebugConfig.setInputDispatcherMajor(this.mInputDebugLevel, this.mSetDebugLevel);
        inputDebugConfig.flushToNative();
        MiuiInputLog.getInstance().setLogLevel(this.mSetDebugLevel);
    }

    private void registerPointerLocationSettingObserver() {
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("pointer_location"), true, new ContentObserver(this.mHandler) { // from class: com.android.server.input.InputManagerServiceStubImpl.4
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                InputManagerServiceStubImpl.this.updatePointerLocationFromSettings();
            }
        }, -1);
    }

    public void updatePointerLocationFromSettings() {
        this.mPointerLocationShow = getPointerLocationSettings(0);
        updateDebugLevel();
    }

    private int getPointerLocationSettings(int defaultValue) {
        try {
            int result = Settings.System.getIntForUser(this.mContext.getContentResolver(), "pointer_location", -2);
            return result;
        } catch (Settings.SettingNotFoundException snfe) {
            snfe.printStackTrace();
            return defaultValue;
        }
    }

    private void registerTouchesPreventRecordSettingObserver() {
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(SHOW_TOUCHES_PREVENTRECORDER), true, new ContentObserver(this.mHandler) { // from class: com.android.server.input.InputManagerServiceStubImpl.5
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                InputManagerServiceStubImpl.this.updateTouchesPreventRecorderFromSettings();
            }
        }, -1);
    }

    public void updateTouchesPreventRecorderFromSettings() {
        this.mShowTouchesPreventRecord = getShowTouchesPreventRecordSetting(0);
        updateDebugLevel();
    }

    private int getShowTouchesPreventRecordSetting(int defaultValue) {
        try {
            int result = Settings.System.getIntForUser(this.mContext.getContentResolver(), SHOW_TOUCHES_PREVENTRECORDER, -2);
            return result;
        } catch (Settings.SettingNotFoundException snfe) {
            snfe.printStackTrace();
            return defaultValue;
        }
    }

    public boolean isPad() {
        return PadManager.getInstance().isPad();
    }

    private void registerDefaultInputMethodSettingObserver() {
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("default_input_method"), false, new ContentObserver(this.mHandler) { // from class: com.android.server.input.InputManagerServiceStubImpl.6
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                InputManagerServiceStubImpl.this.updateDefaultInputMethodFromSettings();
            }
        }, -1);
    }

    public void updateDefaultInputMethodFromSettings() {
        String inputMethodId = Settings.Secure.getString(this.mContext.getContentResolver(), "default_input_method");
        String defaultInputMethod = "";
        if (!TextUtils.isEmpty(inputMethodId) && inputMethodId.contains("/")) {
            defaultInputMethod = inputMethodId.substring(0, inputMethodId.indexOf(47));
        }
        this.mCustomizeInputMethod = MIUI_INPUTMETHOD.contains(defaultInputMethod);
    }

    private void registerInputLevelSelect() {
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(INPUT_LOG_LEVEL), false, new ContentObserver(this.mHandler) { // from class: com.android.server.input.InputManagerServiceStubImpl.7
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                InputManagerServiceStubImpl.this.updateFromInputLevelSetting();
            }
        }, -1);
    }

    public void updateFromInputLevelSetting() {
        this.mFromSetInputLevel = Settings.System.getIntForUser(this.mContext.getContentResolver(), INPUT_LOG_LEVEL, 0, -2);
        InputDebugConfig inputDebugConfig = InputDebugConfig.getInstance();
        inputDebugConfig.setInputDebugFromDump(this.mFromSetInputLevel | this.mSetDebugLevel);
        inputDebugConfig.flushToNative();
    }

    public boolean jumpPermissionCheck(String permission, int uid) {
        if ("android.permission.INJECT_EVENTS".equals(permission) && uid == 1002) {
            Slog.d("InputManagerServiceStubImpl", "INJECT_EVENTS Permission Denial, bypass BLUETOOTH_UID!");
            return true;
        }
        return false;
    }

    private void registerGameMode() {
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor(KEY_GAME_BOOSTER), false, new ContentObserver(this.mHandler) { // from class: com.android.server.input.InputManagerServiceStubImpl.8
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                InputManagerServiceStubImpl.this.updateOnewayModeFromSettings();
            }
        }, -1);
    }

    public void updateOnewayModeFromSettings() {
        boolean z = false;
        boolean isGameMode = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), KEY_GAME_BOOSTER, 0, -2) == 1;
        boolean isCtsMode = !SystemProperties.getBoolean("persist.sys.miui_optimization", !SplitScreenReporter.ACTION_ENTER_SPLIT.equals(SystemProperties.get("ro.miui.cts")));
        InputCommonConfig inputCommonConfig = InputCommonConfig.getInstance();
        if (isGameMode && !isCtsMode) {
            z = true;
        }
        inputCommonConfig.setOnewayMode(z);
        inputCommonConfig.flushToNative();
    }

    public void setMagicPointerCallbacks(MagicPointerCallbacks callbacks) {
        this.mMagicPointerCallbacks = callbacks;
    }

    public void updateMagicPointerPosition(float pointerX, float pointerY) {
        MagicPointerCallbacks magicPointerCallbacks = this.mMagicPointerCallbacks;
        if (magicPointerCallbacks != null) {
            magicPointerCallbacks.updateMagicPointerPosition(pointerX, pointerY);
        }
    }

    public void setMagicPointerVisibility(boolean visibility) {
        MagicPointerCallbacks magicPointerCallbacks = this.mMagicPointerCallbacks;
        if (magicPointerCallbacks != null) {
            magicPointerCallbacks.setMagicPointerVisibility(visibility);
        }
    }

    public boolean isMagicPointerEnabled() {
        MagicPointerCallbacks magicPointerCallbacks = this.mMagicPointerCallbacks;
        if (magicPointerCallbacks != null) {
            return magicPointerCallbacks.isMagicPointerEnabled();
        }
        return false;
    }
}
