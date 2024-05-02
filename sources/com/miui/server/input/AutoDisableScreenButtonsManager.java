package com.miui.server.input;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.MiuiSettings;
import android.provider.Settings;
import android.util.Log;
import android.util.Slog;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Toast;
import com.android.server.padkeyboard.usb.UsbKeyboardUtil;
import com.android.server.wm.WindowProcessUtils;
import com.miui.enterprise.RestrictionsHelper;
import java.util.HashMap;
import miui.securityspace.CrossUserUtils;
import miui.util.AutoDisableScreenButtonsHelper;
import miui.util.SmartCoverManager;
import miui.view.AutoDisableScreenbuttonsFloatView;
/* loaded from: classes.dex */
public class AutoDisableScreenButtonsManager {
    private static final int ENABLE_KEY_PRESS_INTERVAL = 2000;
    private static final String PREF_ADSB_NOT_SHOW_PROMPTS = "ADSB_NOT_SHOW_PROMPTS";
    private static final ComponentName SettingsActionComponent = ComponentName.unflattenFromString("com.android.settings/.AutoDisableScreenButtonsAppListSettingsActivity");
    private static final String TAG = "AutoDisableScreenButtonsManager";
    private static final int TMP_DISABLE_BUTTON = 2;
    private static AutoDisableScreenButtonsManager sAutoDisableScreenButtonsManager;
    private String mCloudConfig;
    private Context mContext;
    private AutoDisableScreenbuttonsFloatView mFloatView;
    private int mScreenButtonPressedKeyCode;
    private long mScreenButtonPressedTime;
    private boolean mScreenButtonsDisabled;
    private boolean mScreenButtonsTmpDisabled;
    private long mToastShowTime;
    private String mUserSetting;
    private int mCurUserId = 0;
    private boolean mTwice = false;
    private boolean mStatusBarVisibleOld = true;
    private Handler mHandler = new Handler();
    private final Object mLock = new Object();

    public AutoDisableScreenButtonsManager(Context context, Handler handler) {
        this.mContext = context;
        resetButtonsStatus();
        DisableButtonsSettingsObserver observer = new DisableButtonsSettingsObserver(handler);
        observer.observe();
        sAutoDisableScreenButtonsManager = this;
    }

    public void onStatusBarVisibilityChange(final boolean visible) {
        if (visible != this.mStatusBarVisibleOld) {
            this.mHandler.post(new Runnable() { // from class: com.miui.server.input.AutoDisableScreenButtonsManager.1
                @Override // java.lang.Runnable
                public void run() {
                    if (visible) {
                        if (AutoDisableScreenButtonsManager.this.mScreenButtonsTmpDisabled) {
                            AutoDisableScreenButtonsManager.this.saveTmpDisableButtonsStatus(false);
                        }
                        if (AutoDisableScreenButtonsManager.this.mFloatView != null) {
                            AutoDisableScreenButtonsManager.this.mFloatView.dismiss();
                        }
                    } else {
                        String packageName = AutoDisableScreenButtonsManager.getRunningTopActivity();
                        int flag = AutoDisableScreenButtonsHelper.getAppFlag(AutoDisableScreenButtonsManager.this.mContext, packageName);
                        if (flag == 2) {
                            AutoDisableScreenButtonsManager.this.saveTmpDisableButtonsStatus(true);
                        } else if (flag == 1) {
                            AutoDisableScreenButtonsManager.this.showFloat();
                        }
                    }
                    AutoDisableScreenButtonsManager.this.mStatusBarVisibleOld = visible;
                }
            });
        }
    }

    public static void onStatusBarVisibilityChangeStatic(boolean visible) {
        AutoDisableScreenButtonsManager autoDisableScreenButtonsManager = sAutoDisableScreenButtonsManager;
        if (autoDisableScreenButtonsManager != null) {
            autoDisableScreenButtonsManager.onStatusBarVisibilityChange(visible);
        }
    }

    public boolean handleDisableButtons(int keyCode, boolean down, boolean disableForSingleKey, boolean disableForLidClose, KeyEvent event) {
        boolean isVirtual = event.getDevice() == null || event.getDevice().isVirtual();
        boolean isVirtualHardKey = (event.getFlags() & 64) != 0;
        if (RestrictionsHelper.hasKeyCodeRestriction(this.mContext, keyCode, CrossUserUtils.getCurrentUserId())) {
            return true;
        }
        switch (keyCode) {
            case 3:
            case 84:
                break;
            default:
                return false;
            case 4:
            case UsbKeyboardUtil.COMMAND_READ_KEYBOARD /* 82 */:
            case 187:
                if (disableForSingleKey && !isVirtual) {
                    Slog.i(TAG, "disableForSingleKey keyCode:" + keyCode);
                    return true;
                }
                break;
        }
        if (isVirtual && !isVirtualHardKey) {
            return false;
        }
        if (disableForLidClose && SmartCoverManager.deviceDisableKeysWhenLidClose()) {
            Slog.i(TAG, "disableForLidClose keyCode:" + keyCode);
            return true;
        } else if (!screenButtonsInterceptKey(keyCode, down)) {
            return false;
        } else {
            Slog.i(TAG, "screenButtonsDisabled keyCode:" + keyCode);
            return true;
        }
    }

    public boolean screenButtonsInterceptKey(int keycode, boolean down) {
        if (!isScreenButtonsDisabled()) {
            return false;
        }
        if (down) {
            long time = SystemClock.elapsedRealtime();
            if (time - this.mScreenButtonPressedTime < 2000 && this.mScreenButtonPressedKeyCode == keycode && this.mTwice) {
                this.mTwice = false;
                resetButtonsStatus();
                return false;
            }
            this.mScreenButtonPressedTime = time;
            this.mScreenButtonPressedKeyCode = keycode;
            this.mTwice = true;
            if (time - this.mToastShowTime > 2000) {
                this.mToastShowTime = time;
                showToast(this.mContext.getString(286195807), this.mHandler);
            }
        }
        return true;
    }

    public void onUserSwitch(int uid) {
        if (this.mCurUserId != uid) {
            this.mCurUserId = uid;
            updateSettings();
        }
    }

    public boolean isScreenButtonsDisabled() {
        return this.mScreenButtonsDisabled || this.mScreenButtonsTmpDisabled;
    }

    private void showToast(boolean enabled, Handler h) {
        showToast(this.mContext.getString(enabled ? 286195801 : 286195800), h);
    }

    private void showToast(final CharSequence text, Handler h) {
        if (h != null) {
            h.post(new Runnable() { // from class: com.miui.server.input.AutoDisableScreenButtonsManager.2
                @Override // java.lang.Runnable
                public void run() {
                    AutoDisableScreenButtonsManager.this.showToastInner(text);
                }
            });
        } else {
            showToastInner(text);
        }
    }

    public void showToastInner(CharSequence text) {
        Toast toast = Toast.makeText(this.mContext, text, 0);
        toast.setType(2006);
        toast.show();
    }

    public void showFloat() {
        Log.d(TAG, "showing auto disable screen buttons float window...");
        if (this.mFloatView == null) {
            AutoDisableScreenbuttonsFloatView inflate = AutoDisableScreenbuttonsFloatView.inflate(this.mContext);
            this.mFloatView = inflate;
            inflate.setOnClickListener(new View.OnClickListener() { // from class: com.miui.server.input.AutoDisableScreenButtonsManager.3
                @Override // android.view.View.OnClickListener
                public void onClick(View v) {
                    AutoDisableScreenButtonsManager.this.mFloatView.dismiss();
                    AutoDisableScreenButtonsManager.this.saveTmpDisableButtonsStatus(true);
                    AutoDisableScreenButtonsManager.this.showPromptsIfNeeds();
                }
            });
            this.mFloatView.setOnLongClickListener(new View.OnLongClickListener() { // from class: com.miui.server.input.AutoDisableScreenButtonsManager.4
                @Override // android.view.View.OnLongClickListener
                public boolean onLongClick(View v) {
                    AutoDisableScreenButtonsManager.this.mFloatView.dismiss();
                    AutoDisableScreenButtonsManager.this.showSettings();
                    return true;
                }
            });
        }
        this.mFloatView.show();
    }

    public boolean showPromptsIfNeeds() {
        Object obj = AutoDisableScreenButtonsHelper.getValue(this.mContext, PREF_ADSB_NOT_SHOW_PROMPTS);
        boolean notShow = obj == null ? false : ((Boolean) obj).booleanValue();
        if (notShow) {
            return false;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this.mContext);
        AlertDialog adlg = builder.setTitle(286195806).setMessage(286195803).setCancelable(true).setPositiveButton(286195805, new DialogInterface.OnClickListener() { // from class: com.miui.server.input.AutoDisableScreenButtonsManager.5
            @Override // android.content.DialogInterface.OnClickListener
            public void onClick(DialogInterface dialog, int whichButton) {
                AutoDisableScreenButtonsHelper.setValue(AutoDisableScreenButtonsManager.this.mContext, AutoDisableScreenButtonsManager.PREF_ADSB_NOT_SHOW_PROMPTS, true);
            }
        }).create();
        adlg.getWindow().setType(2003);
        adlg.show();
        return true;
    }

    public void showSettings() {
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.setComponent(SettingsActionComponent);
        intent.setFlags(268435456);
        try {
            this.mContext.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "start activity exception, component = " + SettingsActionComponent);
        }
    }

    private void saveDisableButtonsStatus(boolean disable) {
        this.mScreenButtonsDisabled = disable;
        Settings.Secure.putIntForUser(this.mContext.getContentResolver(), "screen_buttons_state", disable ? 1 : 0, this.mCurUserId);
    }

    public void saveTmpDisableButtonsStatus(boolean tmp) {
        this.mScreenButtonsTmpDisabled = tmp;
        if (this.mScreenButtonsDisabled) {
            return;
        }
        Settings.Secure.putIntForUser(this.mContext.getContentResolver(), "screen_buttons_state", tmp ? 2 : 0, this.mCurUserId);
    }

    private void resetButtonsStatus() {
        saveDisableButtonsStatus(false);
        this.mScreenButtonsTmpDisabled = false;
    }

    public void resetTmpButtonsStatus() {
        this.mScreenButtonsTmpDisabled = false;
    }

    public static String getRunningTopActivity() {
        HashMap<String, Object> topActivityInfo = WindowProcessUtils.getTopRunningActivityInfo();
        if (topActivityInfo == null) {
            return null;
        }
        return (String) topActivityInfo.get("packageName");
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class DisableButtonsSettingsObserver extends ContentObserver {
        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public DisableButtonsSettingsObserver(Handler handler) {
            super(handler);
            AutoDisableScreenButtonsManager.this = r1;
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange) {
            AutoDisableScreenButtonsManager.this.updateSettings();
        }

        void observe() {
            ContentResolver resolver = AutoDisableScreenButtonsManager.this.mContext.getContentResolver();
            resolver.registerContentObserver(Settings.Secure.getUriFor("screen_buttons_state"), false, this, -1);
            resolver.registerContentObserver(Settings.System.getUriFor("auto_disable_screen_button"), false, this, -1);
            resolver.registerContentObserver(Settings.System.getUriFor("auto_disable_screen_button_cloud_setting"), false, this, -1);
            onChange(false);
        }
    }

    public void updateSettings() {
        ContentResolver resolver = this.mContext.getContentResolver();
        synchronized (this.mLock) {
            int btnState = Settings.Secure.getIntForUser(resolver, "screen_buttons_state", 0, this.mCurUserId);
            switch (btnState) {
                case 0:
                    this.mScreenButtonsDisabled = false;
                    break;
                case 1:
                    this.mScreenButtonsDisabled = true;
                    break;
            }
            String userSetting = MiuiSettings.System.getStringForUser(resolver, "auto_disable_screen_button", this.mCurUserId);
            if (userSetting != null && !userSetting.equals(this.mUserSetting)) {
                this.mUserSetting = userSetting;
                AutoDisableScreenButtonsHelper.updateUserJson(userSetting);
            }
            String cloudConfig = Settings.System.getString(resolver, "auto_disable_screen_button_cloud_setting");
            if (cloudConfig != null && !cloudConfig.equals(this.mCloudConfig)) {
                this.mCloudConfig = cloudConfig;
                AutoDisableScreenButtonsHelper.updateCloudJson(cloudConfig);
            }
        }
    }
}
