package com.android.server.inputmethod;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.SystemProperties;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Slog;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodInfo;
import com.android.server.inputmethod.InputMethodSubtypeSwitchingController;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import miui.os.Build;
/* loaded from: classes.dex */
public class MiuiSecurityInputMethodHelper {
    public static final boolean DEBUG = true;
    public static final String MIUI_SEC_INPUT_METHOD_APP_PKG_NAME = "com.miui.securityinputmethod";
    public static final int NUMBER_PASSWORD = 18;
    public static final boolean SUPPORT_SEC_INPUT_METHOD;
    public static final String TAG = "MiuiSecurityInputMethodHelper";
    public static final int TEXT_MASK = 4095;
    public static final int TEXT_PASSWORD = 129;
    public static final int TEXT_VISIBLE_PASSWORD = 145;
    public static final int TEXT_WEB_PASSWORD = 225;
    public static final int WEB_EDIT_TEXT = 160;
    private boolean mSecEnabled;
    private InputMethodManagerService mService;
    private SettingsObserver mSettingsObserver;

    static {
        boolean z = false;
        if (SystemProperties.getInt("ro.miui.has_security_keyboard", 0) == 1 && !Build.IS_GLOBAL_BUILD) {
            z = true;
        }
        SUPPORT_SEC_INPUT_METHOD = z;
    }

    public MiuiSecurityInputMethodHelper(InputMethodManagerService service) {
        this.mService = service;
    }

    void onSystemRunningLocked() {
        SettingsObserver settingsObserver = new SettingsObserver(InputMethodManagerServiceImpl.getInstance().mHandler);
        this.mSettingsObserver = settingsObserver;
        settingsObserver.registerContentObserverLocked(this.mService.mSettings.getCurrentUserId());
        boolean z = true;
        if (Settings.Secure.getIntForUser(this.mService.mContext.getContentResolver(), "enable_miui_security_ime", 1, this.mService.mSettings.getCurrentUserId()) == 0) {
            z = false;
        }
        this.mSecEnabled = z;
    }

    void onSwitchUserLocked(int newUserId) {
        this.mSettingsObserver.registerContentObserverLocked(newUserId);
        boolean z = true;
        if (Settings.Secure.getIntForUser(this.mService.mContext.getContentResolver(), "enable_miui_security_ime", 1, this.mService.mSettings.getCurrentUserId()) == 0) {
            z = false;
        }
        this.mSecEnabled = z;
    }

    /* loaded from: classes.dex */
    class SettingsObserver extends ContentObserver {
        boolean mRegistered = false;
        int mUserId;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        SettingsObserver(Handler handler) {
            super(handler);
            MiuiSecurityInputMethodHelper.this = this$0;
        }

        void registerContentObserverLocked(int userId) {
            if (!MiuiSecurityInputMethodHelper.SUPPORT_SEC_INPUT_METHOD) {
                return;
            }
            if (this.mRegistered && this.mUserId == userId) {
                return;
            }
            ContentResolver resolver = MiuiSecurityInputMethodHelper.this.mService.mContext.getContentResolver();
            if (this.mRegistered) {
                resolver.unregisterContentObserver(this);
                this.mRegistered = false;
            }
            if (this.mUserId != userId) {
                this.mUserId = userId;
            }
            resolver.registerContentObserver(Settings.Secure.getUriFor("enable_miui_security_ime"), false, this, userId);
            this.mRegistered = true;
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            Uri secIMEUri = Settings.Secure.getUriFor("enable_miui_security_ime");
            synchronized (MiuiSecurityInputMethodHelper.this.mService.mMethodMap) {
                if (secIMEUri.equals(uri)) {
                    MiuiSecurityInputMethodHelper miuiSecurityInputMethodHelper = MiuiSecurityInputMethodHelper.this;
                    boolean z = true;
                    if (Settings.Secure.getIntForUser(miuiSecurityInputMethodHelper.mService.mContext.getContentResolver(), "enable_miui_security_ime", 1, MiuiSecurityInputMethodHelper.this.mService.mSettings.getCurrentUserId()) == 0) {
                        z = false;
                    }
                    miuiSecurityInputMethodHelper.mSecEnabled = z;
                    MiuiSecurityInputMethodHelper.this.updateFromSettingsLocked();
                    Slog.d(MiuiSecurityInputMethodHelper.TAG, "enable status change: " + MiuiSecurityInputMethodHelper.this.mSecEnabled);
                }
            }
        }
    }

    public void updateFromSettingsLocked() {
        if (this.mService.getSelectedMethodIdLocked() != null && !this.mSecEnabled && isSecMethodLocked(this.mService.getSelectedMethodIdLocked())) {
            this.mService.clearClientSessionsLocked();
            this.mService.unbindCurrentClientLocked(2);
        }
    }

    boolean mayChangeInputMethodLocked(EditorInfo attribute) {
        if (!SUPPORT_SEC_INPUT_METHOD) {
            return false;
        }
        if (this.mService.getSelectedMethodIdLocked() == null) {
            Slog.w(TAG, "input_service has no current_method_id");
            return false;
        } else if (attribute == null) {
            Slog.w(TAG, "editor_info is null, we cannot judge");
            return false;
        } else if (InputMethodManagerServiceImpl.getInstance().mBindingController != null) {
            InputMethodInfo curMethodInfo = (InputMethodInfo) this.mService.mMethodMap.get(this.mService.getSelectedMethodIdLocked());
            if (curMethodInfo == null) {
                Slog.w(TAG, "fail to find current_method_info in the map");
                return false;
            }
            boolean switchToSecInput = isPasswdInputType(attribute.inputType) && !isSecMethodLocked(this.mService.getSelectedMethodIdLocked()) && this.mSecEnabled && !TextUtils.isEmpty(getSecMethodIdLocked()) && !isEditorInDefaultImeApp(attribute);
            boolean switchFromSecInput = isSecMethodLocked(this.mService.getSelectedMethodIdLocked()) && (!this.mSecEnabled || !isPasswdInputType(attribute.inputType) || isEditorInDefaultImeApp(attribute));
            if (switchToSecInput) {
                String secInputMethodId = getSecMethodIdLocked();
                if (TextUtils.isEmpty(secInputMethodId)) {
                    Slog.w(TAG, "fail to find secure_input_method in input_method_list");
                    return false;
                }
                InputMethodManagerServiceImpl.getInstance().mBindingController.setSelectedMethodId(secInputMethodId);
                this.mService.clearClientSessionsLocked();
                this.mService.unbindCurrentClientLocked(2);
                InputMethodManagerServiceImpl.getInstance().mBindingController.unbindCurrentMethod();
                InputMethodManagerService inputMethodManagerService = this.mService;
                inputMethodManagerService.setInputMethodLocked(inputMethodManagerService.getSelectedMethodIdLocked(), -1);
                return true;
            } else if (!switchFromSecInput) {
                return false;
            } else {
                String selectedInputMethod = this.mService.mSettings.getSelectedInputMethod();
                if (TextUtils.isEmpty(selectedInputMethod)) {
                    Slog.w(TAG, "something is weired, maybe the input method app are uninstalled");
                    InputMethodInfo imi = InputMethodUtils.getMostApplicableDefaultIME(this.mService.mSettings.getEnabledInputMethodListLocked());
                    if (imi == null || TextUtils.equals(imi.getPackageName(), "com.miui.securityinputmethod")) {
                        Slog.w(TAG, "fail to find a most applicable default ime");
                        List<InputMethodInfo> imiList = this.mService.mSettings.getEnabledInputMethodListLocked();
                        if (imiList == null || imiList.size() == 0) {
                            Slog.w(TAG, "there is no enabled method list");
                            return false;
                        }
                        Iterator<InputMethodInfo> it = imiList.iterator();
                        while (true) {
                            if (!it.hasNext()) {
                                break;
                            }
                            InputMethodInfo inputMethodInfo = it.next();
                            if (!TextUtils.equals(inputMethodInfo.getPackageName(), "com.miui.securityinputmethod")) {
                                selectedInputMethod = inputMethodInfo.getId();
                                break;
                            }
                        }
                    }
                }
                if (TextUtils.isEmpty(selectedInputMethod)) {
                    Slog.w(TAG, "finally, we still fail to find default input method");
                    return false;
                } else if (TextUtils.equals(this.mService.getSelectedMethodIdLocked(), selectedInputMethod)) {
                    Slog.w(TAG, "It looks like there is only miui_sec_input_method in the system");
                    return false;
                } else {
                    InputMethodManagerServiceImpl.getInstance().mBindingController.setSelectedMethodId(selectedInputMethod);
                    this.mService.clearClientSessionsLocked();
                    this.mService.unbindCurrentClientLocked(2);
                    InputMethodManagerServiceImpl.getInstance().mBindingController.unbindCurrentMethod();
                    InputMethodManagerService inputMethodManagerService2 = this.mService;
                    inputMethodManagerService2.setInputMethodLocked(inputMethodManagerService2.getSelectedMethodIdLocked(), -1);
                    return true;
                }
            }
        } else {
            Slog.w(TAG, "IMMS_IMPL has not init");
            return false;
        }
    }

    private boolean isSecMethodLocked(String methodId) {
        InputMethodInfo imi = (InputMethodInfo) this.mService.mMethodMap.get(methodId);
        return imi != null && TextUtils.equals(imi.getPackageName(), "com.miui.securityinputmethod");
    }

    private String getSecMethodIdLocked() {
        for (Map.Entry<String, InputMethodInfo> entry : this.mService.mMethodMap.entrySet()) {
            if (isSecMethodLocked(entry.getKey())) {
                return entry.getKey();
            }
        }
        return null;
    }

    boolean shouldHideImeSwitcherLocked() {
        return (SUPPORT_SEC_INPUT_METHOD && isSecMethodLocked(this.mService.getSelectedMethodIdLocked())) || (InputMethodManagerServiceImpl.getInstance().mBindingController != null && InputMethodManagerServiceImpl.getInstance().mBindingController.getCurMethod() == null);
    }

    void removeSecMethod(List<InputMethodSubtypeSwitchingController.ImeSubtypeListItem> imList) {
        if (SUPPORT_SEC_INPUT_METHOD && imList != null && imList.size() > 0) {
            for (InputMethodSubtypeSwitchingController.ImeSubtypeListItem imeSubtypeListItem : imList) {
                InputMethodInfo imi = imeSubtypeListItem.mImi;
                if (TextUtils.equals(imi.getPackageName(), "com.miui.securityinputmethod")) {
                    imList.remove(imeSubtypeListItem);
                    return;
                }
            }
        }
    }

    ArrayList<InputMethodInfo> filterSecMethodLocked(ArrayList<InputMethodInfo> methodInfos) {
        if (methodInfos != null && SUPPORT_SEC_INPUT_METHOD) {
            Iterator<InputMethodInfo> it = methodInfos.iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                InputMethodInfo methodInfo = it.next();
                if (isSecMethodLocked(methodInfo.getId())) {
                    methodInfos.remove(methodInfo);
                    break;
                }
            }
        }
        return methodInfos;
    }

    private static boolean isPasswdInputType(int inputType) {
        return (inputType & 160) == 160 ? (inputType & 4095) == 225 : (inputType & 4095) == 129 || (inputType & 4095) == 145 || (inputType & 4095) == 18;
    }

    private boolean isEditorInDefaultImeApp(EditorInfo editor) {
        ComponentName cn;
        String pkg = editor.packageName;
        String defaultIme = this.mService.mSettings.getSelectedInputMethod();
        return !TextUtils.isEmpty(defaultIme) && (cn = ComponentName.unflattenFromString(defaultIme)) != null && TextUtils.equals(pkg, cn.getPackageName());
    }
}
