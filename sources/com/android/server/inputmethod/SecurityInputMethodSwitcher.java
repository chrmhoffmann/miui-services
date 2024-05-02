package com.android.server.inputmethod;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.pm.UserInfo;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.SystemProperties;
import android.os.UserManager;
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
public class SecurityInputMethodSwitcher extends BaseInputMethodSwitcher {
    public static final boolean DEBUG = true;
    public static final String MIUI_SEC_INPUT_METHOD_APP_PKG_NAME = "com.miui.securityinputmethod";
    public static final boolean SUPPORT_SEC_INPUT_METHOD;
    public static final String TAG = "SecurityInputMethodSwitcher";
    private boolean mSecEnabled;
    private SettingsObserver mSettingsObserver;

    static {
        boolean z = false;
        if (SystemProperties.getInt("ro.miui.has_security_keyboard", 0) == 1 && !Build.IS_GLOBAL_BUILD) {
            z = true;
        }
        SUPPORT_SEC_INPUT_METHOD = z;
    }

    public void onSystemRunningLocked() {
        if (!SUPPORT_SEC_INPUT_METHOD) {
            return;
        }
        this.mSettingsObserver = new SettingsObserver(InputMethodManagerServiceImpl.getInstance().mHandler);
        int parentUserId = getParentUserId(this.mService.mSettings.getCurrentUserId());
        this.mSettingsObserver.registerContentObserverLocked(parentUserId);
        this.mSecEnabled = isSecEnabled(parentUserId);
    }

    public void onSwitchUserLocked(int newUserId) {
        if (!SUPPORT_SEC_INPUT_METHOD) {
            return;
        }
        int parentUserId = getParentUserId(newUserId);
        this.mSettingsObserver.registerContentObserverLocked(parentUserId);
        this.mSecEnabled = isSecEnabled(parentUserId);
    }

    public boolean mayChangeInputMethodLocked(EditorInfo attribute) {
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
                this.mService.setInputMethodLocked(this.mService.getSelectedMethodIdLocked(), -1);
                return true;
            } else if (!switchFromSecInput) {
                return false;
            } else {
                String selectedInputMethod = this.mService.mSettings.getSelectedInputMethod();
                if (TextUtils.isEmpty(selectedInputMethod)) {
                    Slog.i(TAG, "something is weired, maybe the input method app are uninstalled");
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
                    this.mService.setInputMethodLocked(this.mService.getSelectedMethodIdLocked(), -1);
                    return true;
                }
            }
        } else {
            Slog.w(TAG, "IMMS_IMPL has not init");
            return false;
        }
    }

    public boolean shouldHideImeSwitcherLocked() {
        return (SUPPORT_SEC_INPUT_METHOD && isSecMethodLocked(this.mService.getSelectedMethodIdLocked())) || (InputMethodManagerServiceImpl.getInstance().mBindingController != null && InputMethodManagerServiceImpl.getInstance().mBindingController.getCurMethod() == null);
    }

    public void removeMethod(List<InputMethodSubtypeSwitchingController.ImeSubtypeListItem> imList) {
        if (!SUPPORT_SEC_INPUT_METHOD || imList == null || imList.size() == 0) {
            return;
        }
        Iterator<InputMethodSubtypeSwitchingController.ImeSubtypeListItem> iterator = imList.iterator();
        while (iterator.hasNext()) {
            InputMethodSubtypeSwitchingController.ImeSubtypeListItem imeSubtypeListItem = iterator.next();
            InputMethodInfo imi = imeSubtypeListItem.mImi;
            if (TextUtils.equals(imi.getPackageName(), "com.miui.securityinputmethod")) {
                iterator.remove();
            }
        }
    }

    public List<InputMethodInfo> filterMethodLocked(List<InputMethodInfo> methodInfos) {
        if (!SUPPORT_SEC_INPUT_METHOD) {
            return methodInfos;
        }
        ArrayList<InputMethodInfo> noSecMethodInfos = new ArrayList<>();
        if (methodInfos != null) {
            for (InputMethodInfo methodInfo : methodInfos) {
                if (!isSecMethodLocked(methodInfo.getId())) {
                    noSecMethodInfos.add(methodInfo);
                }
            }
        }
        return noSecMethodInfos;
    }

    /* loaded from: classes.dex */
    class SettingsObserver extends ContentObserver {
        boolean mRegistered = false;
        int mUserId;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        SettingsObserver(Handler handler) {
            super(handler);
            SecurityInputMethodSwitcher.this = this$0;
        }

        void registerContentObserverLocked(int userId) {
            if (!SecurityInputMethodSwitcher.SUPPORT_SEC_INPUT_METHOD) {
                return;
            }
            if (this.mRegistered && this.mUserId == userId) {
                return;
            }
            ContentResolver resolver = SecurityInputMethodSwitcher.this.mService.mContext.getContentResolver();
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
            synchronized (SecurityInputMethodSwitcher.this.mService.mMethodMap) {
                if (secIMEUri.equals(uri)) {
                    SecurityInputMethodSwitcher securityInputMethodSwitcher = SecurityInputMethodSwitcher.this;
                    int parentUserId = securityInputMethodSwitcher.getParentUserId(securityInputMethodSwitcher.mService.mSettings.getCurrentUserId());
                    SecurityInputMethodSwitcher securityInputMethodSwitcher2 = SecurityInputMethodSwitcher.this;
                    securityInputMethodSwitcher2.mSecEnabled = securityInputMethodSwitcher2.isSecEnabled(parentUserId);
                    SecurityInputMethodSwitcher.this.updateFromSettingsLocked();
                    Slog.d(SecurityInputMethodSwitcher.TAG, "enable status change: " + SecurityInputMethodSwitcher.this.mSecEnabled);
                }
            }
        }
    }

    public boolean isSecEnabled(int userId) {
        return Settings.Secure.getIntForUser(this.mService.mContext.getContentResolver(), "enable_miui_security_ime", 1, userId) != 0;
    }

    public int getParentUserId(int userId) {
        if (userId == 0) {
            return userId;
        }
        UserManager userManager = UserManager.get(this.mService.mContext);
        UserInfo parentUserInfo = userManager.getProfileParent(userId);
        return parentUserInfo != null ? parentUserInfo.id : userId;
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

    public void updateFromSettingsLocked() {
        if (this.mService.getSelectedMethodIdLocked() != null && !this.mSecEnabled && isSecMethodLocked(this.mService.getSelectedMethodIdLocked())) {
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
