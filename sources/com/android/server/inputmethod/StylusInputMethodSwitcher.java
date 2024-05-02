package com.android.server.inputmethod;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.database.ContentObserver;
import android.hardware.input.InputManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Slog;
import android.view.InputDevice;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodInfo;
import com.android.server.inputmethod.InputMethodSubtypeSwitchingController;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import miui.os.Build;
import miui.util.FeatureParser;
/* loaded from: classes.dex */
public class StylusInputMethodSwitcher extends BaseInputMethodSwitcher {
    public static final boolean DEBUG = true;
    private static final String ENABLE_STYLUS_HAND_WRITING = "stylus_handwriting_enable";
    public static final String HAND_WRITING_KEYBOARD_TYPE = "hand_writing_keyboard_type";
    private static final String MIUI_STYLUS_INPUT_METHOD_APP_PKG_NAME = "com.miui.handwriting";
    private static final boolean SUPPORT_STYLUS_INPUT_METHOD;
    public static final String TAG = "StylusInputMethodSwitcher";
    private SettingsObserver mSettingsObserver;
    private boolean mStylusEnabled;

    static {
        boolean z = false;
        if (!Build.IS_INTERNATIONAL_BUILD && FeatureParser.getBoolean("support_stylus_gesture", false)) {
            z = true;
        }
        SUPPORT_STYLUS_INPUT_METHOD = z;
    }

    public void onSystemRunningLocked() {
        if (!SUPPORT_STYLUS_INPUT_METHOD) {
            return;
        }
        SettingsObserver settingsObserver = new SettingsObserver(InputMethodManagerServiceImpl.getInstance().mHandler);
        this.mSettingsObserver = settingsObserver;
        settingsObserver.registerContentObserverLocked(this.mService.mSettings.getCurrentUserId());
        boolean z = true;
        if (Settings.System.getIntForUser(this.mService.mContext.getContentResolver(), ENABLE_STYLUS_HAND_WRITING, 1, this.mService.mSettings.getCurrentUserId()) == 0) {
            z = false;
        }
        this.mStylusEnabled = z;
    }

    public void onSwitchUserLocked(int newUserId) {
        if (!SUPPORT_STYLUS_INPUT_METHOD) {
            return;
        }
        this.mSettingsObserver.registerContentObserverLocked(newUserId);
        boolean z = true;
        if (Settings.System.getIntForUser(this.mService.mContext.getContentResolver(), ENABLE_STYLUS_HAND_WRITING, 1, newUserId) == 0) {
            z = false;
        }
        this.mStylusEnabled = z;
    }

    public boolean mayChangeInputMethodLocked(EditorInfo attribute) {
        Bundle extras;
        if (!SUPPORT_STYLUS_INPUT_METHOD) {
            return false;
        }
        if (this.mService.getSelectedMethodIdLocked() == null) {
            Slog.w("StylusInputMethodSwitcher", "input_service has no current_method_id");
            return false;
        } else if (attribute != null) {
            InputMethodInfo curMethodInfo = (InputMethodInfo) this.mService.mMethodMap.get(this.mService.getSelectedMethodIdLocked());
            if (curMethodInfo == null) {
                Slog.w("StylusInputMethodSwitcher", "fail to find current_method_info in the map");
                return false;
            } else if (InputMethodManagerServiceImpl.getInstance().mBindingController == null) {
                Slog.w("StylusInputMethodSwitcher", "IMMS_IMPL has not init");
                return false;
            } else if (isPasswdInputType(attribute.inputType) || (extras = attribute.extras) == null) {
                return false;
            } else {
                int realToolType = extras.getInt(HAND_WRITING_KEYBOARD_TYPE, 0);
                boolean switchToStylusInput = !isStylusMethodLocked(this.mService.getSelectedMethodIdLocked()) && isStylusDeviceConnected() && this.mStylusEnabled && realToolType == 2 && !TextUtils.isEmpty(getStylusMethodIdLocked()) && !isEditorInDefaultImeApp(attribute);
                boolean switchFromStylusInput = isStylusMethodLocked(this.mService.getSelectedMethodIdLocked()) && (realToolType == 1 || !this.mStylusEnabled || !isStylusDeviceConnected()) && !TextUtils.isEmpty(getStylusMethodIdLocked()) && !isEditorInDefaultImeApp(attribute);
                if (switchToStylusInput) {
                    String stylusMethodId = getStylusMethodIdLocked();
                    if (TextUtils.isEmpty(stylusMethodId)) {
                        Slog.w("StylusInputMethodSwitcher", "fail to find stylus_input_method in input_method_list");
                        return false;
                    }
                    InputMethodManagerServiceImpl.getInstance().mBindingController.setSelectedMethodId(stylusMethodId);
                    this.mService.clearClientSessionsLocked();
                    this.mService.unbindCurrentClientLocked(2);
                    InputMethodManagerServiceImpl.getInstance().mBindingController.unbindCurrentMethod();
                    this.mService.setInputMethodLocked(this.mService.getSelectedMethodIdLocked(), -1);
                    return true;
                } else if (!switchFromStylusInput) {
                    return false;
                } else {
                    String selectedInputMethod = this.mService.mSettings.getSelectedInputMethod();
                    if (TextUtils.isEmpty(selectedInputMethod)) {
                        Slog.w("StylusInputMethodSwitcher", "something is weired, maybe the input method app are uninstalled");
                        InputMethodInfo imi = InputMethodUtils.getMostApplicableDefaultIME(this.mService.mSettings.getEnabledInputMethodListLocked());
                        if (imi == null || TextUtils.equals(imi.getPackageName(), MIUI_STYLUS_INPUT_METHOD_APP_PKG_NAME)) {
                            Slog.w("StylusInputMethodSwitcher", "fail to find a most applicable default ime");
                            List<InputMethodInfo> imiList = this.mService.mSettings.getEnabledInputMethodListLocked();
                            if (imiList == null || imiList.size() == 0) {
                                Slog.w("StylusInputMethodSwitcher", "there is no enabled method list");
                                return false;
                            }
                            Iterator<InputMethodInfo> it = imiList.iterator();
                            while (true) {
                                if (!it.hasNext()) {
                                    break;
                                }
                                InputMethodInfo inputMethodInfo = it.next();
                                if (!TextUtils.equals(inputMethodInfo.getPackageName(), MIUI_STYLUS_INPUT_METHOD_APP_PKG_NAME)) {
                                    selectedInputMethod = inputMethodInfo.getId();
                                    break;
                                }
                            }
                        }
                    }
                    if (TextUtils.isEmpty(selectedInputMethod)) {
                        Slog.w("StylusInputMethodSwitcher", "finally, we still fail to find default input method");
                        return false;
                    } else if (TextUtils.equals(this.mService.getSelectedMethodIdLocked(), selectedInputMethod)) {
                        Slog.w("StylusInputMethodSwitcher", "It looks like there is only miui_stylus_input_method in the system");
                        return false;
                    } else {
                        InputMethodManagerServiceImpl.getInstance().mBindingController.setSelectedMethodId(selectedInputMethod);
                        this.mService.clearClientSessionsLocked();
                        this.mService.unbindCurrentClientLocked(2);
                        InputMethodManagerServiceImpl.getInstance().mBindingController.unbindCurrentMethod();
                        this.mService.setInputMethodLocked(this.mService.getSelectedMethodIdLocked(), -1);
                        return true;
                    }
                }
            }
        } else {
            Slog.w("StylusInputMethodSwitcher", "editor_info is null, we cannot judge");
            return false;
        }
    }

    private boolean isStylusDeviceConnected() {
        InputManager inputManager = (InputManager) this.mService.mContext.getSystemService("input");
        int[] inputDeviceIds = inputManager.getInputDeviceIds();
        for (int inputDeviceId : inputDeviceIds) {
            InputDevice inputDevice = inputManager.getInputDevice(inputDeviceId);
            if (isXiaomiStylus(inputDevice)) {
                return true;
            }
        }
        return false;
    }

    private boolean isXiaomiStylus(InputDevice inputDevice) {
        return inputDevice != null && inputDevice.isXiaomiStylus() > 0;
    }

    public boolean shouldHideImeSwitcherLocked() {
        return (SUPPORT_STYLUS_INPUT_METHOD && isStylusMethodLocked(this.mService.getSelectedMethodIdLocked())) || (InputMethodManagerServiceImpl.getInstance().mBindingController != null && InputMethodManagerServiceImpl.getInstance().mBindingController.getCurMethod() == null);
    }

    public void removeMethod(List<InputMethodSubtypeSwitchingController.ImeSubtypeListItem> imList) {
        if (!SUPPORT_STYLUS_INPUT_METHOD || imList == null || imList.size() == 0) {
            return;
        }
        Iterator<InputMethodSubtypeSwitchingController.ImeSubtypeListItem> iterator = imList.iterator();
        while (iterator.hasNext()) {
            InputMethodSubtypeSwitchingController.ImeSubtypeListItem imeSubtypeListItem = iterator.next();
            InputMethodInfo imi = imeSubtypeListItem.mImi;
            if (TextUtils.equals(imi.getPackageName(), MIUI_STYLUS_INPUT_METHOD_APP_PKG_NAME)) {
                iterator.remove();
            }
        }
    }

    public List<InputMethodInfo> filterMethodLocked(List<InputMethodInfo> methodInfos) {
        if (!SUPPORT_STYLUS_INPUT_METHOD) {
            return methodInfos;
        }
        ArrayList<InputMethodInfo> noStylusMethodInfos = new ArrayList<>();
        if (methodInfos != null) {
            for (InputMethodInfo methodInfo : methodInfos) {
                if (!isStylusMethodLocked(methodInfo.getId())) {
                    noStylusMethodInfos.add(methodInfo);
                }
            }
        }
        return noStylusMethodInfos;
    }

    /* loaded from: classes.dex */
    class SettingsObserver extends ContentObserver {
        boolean mRegistered = false;
        int mUserId;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        SettingsObserver(Handler handler) {
            super(handler);
            StylusInputMethodSwitcher.this = this$0;
        }

        void registerContentObserverLocked(int userId) {
            if (!StylusInputMethodSwitcher.SUPPORT_STYLUS_INPUT_METHOD) {
                return;
            }
            if (this.mRegistered && this.mUserId == userId) {
                return;
            }
            ContentResolver resolver = StylusInputMethodSwitcher.this.mService.mContext.getContentResolver();
            if (this.mRegistered) {
                resolver.unregisterContentObserver(this);
                this.mRegistered = false;
            }
            if (this.mUserId != userId) {
                this.mUserId = userId;
            }
            resolver.registerContentObserver(Settings.System.getUriFor(StylusInputMethodSwitcher.ENABLE_STYLUS_HAND_WRITING), false, this, userId);
            this.mRegistered = true;
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            Uri stylusIMEUri = Settings.System.getUriFor(StylusInputMethodSwitcher.ENABLE_STYLUS_HAND_WRITING);
            synchronized (StylusInputMethodSwitcher.this.mService.mMethodMap) {
                if (stylusIMEUri.equals(uri)) {
                    StylusInputMethodSwitcher stylusInputMethodSwitcher = StylusInputMethodSwitcher.this;
                    boolean z = true;
                    if (Settings.System.getIntForUser(stylusInputMethodSwitcher.mService.mContext.getContentResolver(), StylusInputMethodSwitcher.ENABLE_STYLUS_HAND_WRITING, 1, StylusInputMethodSwitcher.this.mService.mSettings.getCurrentUserId()) == 0) {
                        z = false;
                    }
                    stylusInputMethodSwitcher.mStylusEnabled = z;
                    StylusInputMethodSwitcher.this.updateFromSettingsLocked();
                    Slog.d("StylusInputMethodSwitcher", "enable status change: " + StylusInputMethodSwitcher.this.mStylusEnabled);
                }
            }
        }
    }

    private boolean isStylusMethodLocked(String methodId) {
        InputMethodInfo imi = (InputMethodInfo) this.mService.mMethodMap.get(methodId);
        return imi != null && TextUtils.equals(imi.getPackageName(), MIUI_STYLUS_INPUT_METHOD_APP_PKG_NAME);
    }

    private String getStylusMethodIdLocked() {
        for (Map.Entry<String, InputMethodInfo> entry : this.mService.mMethodMap.entrySet()) {
            if (isStylusMethodLocked(entry.getKey())) {
                return entry.getKey();
            }
        }
        return null;
    }

    public void updateFromSettingsLocked() {
        if (this.mService.getSelectedMethodIdLocked() != null && !this.mStylusEnabled && isStylusMethodLocked(this.mService.getSelectedMethodIdLocked())) {
            this.mService.clearClientSessionsLocked();
            this.mService.unbindCurrentClientLocked(2);
        }
    }

    private boolean isEditorInDefaultImeApp(EditorInfo editor) {
        ComponentName cn;
        String pkg = editor.packageName;
        String defaultIme = this.mService.mSettings.getSelectedInputMethod();
        return !TextUtils.isEmpty(defaultIme) && (cn = ComponentName.unflattenFromString(defaultIme)) != null && TextUtils.equals(pkg, cn.getPackageName());
    }
}
