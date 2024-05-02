package com.android.server.policy;

import android.app.KeyguardManager;
import android.content.Context;
import android.os.Handler;
import android.os.PowerManager;
import android.view.KeyEvent;
import com.android.server.LocalServices;
import com.android.server.input.InputFeatureSupport;
import com.android.server.padkeyboard.MiuiIICKeyboardManager;
import com.android.server.padkeyboard.MiuiPadKeyboardManager;
import com.android.server.policy.WindowManagerPolicy;
import com.miui.server.input.PadManager;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
/* loaded from: classes.dex */
public class MiuiKeyInterceptExtend {
    private static final String TAG = "MiuiKeyInterceptExtend";
    private final Context mContext;
    private int mCurrentUserId;
    private Handler mHandler;
    private int mKeyPressed;
    private int mKeyPressing;
    private KeyguardManager mKeyguardManager;
    private final Object mLock = new Object();
    private final PowerManager mPowerManager;
    private boolean mShortcutTriggered;
    private boolean mSingleKeyUse;
    private List<String> mSkipInterceptWindows;
    private static volatile MiuiKeyInterceptExtend INSTANCE = null;
    public static final Set<Integer> SHORTCUTS = new HashSet<Integer>() { // from class: com.android.server.policy.MiuiKeyInterceptExtend.1
        {
            add(82);
            add(187);
            add(117);
            add(118);
            add(57);
            add(58);
            add(3);
        }
    };
    public static final Set<Integer> COMBINATION_SHORTCUTS_ALT = new HashSet<Integer>() { // from class: com.android.server.policy.MiuiKeyInterceptExtend.2
        {
            add(134);
            add(61);
        }
    };
    public static final Set<Integer> COMBINATION_SHORTCUTS_META = new HashSet<Integer>() { // from class: com.android.server.policy.MiuiKeyInterceptExtend.3
        {
            add(37);
            add(47);
            add(40);
            add(32);
            add(33);
            add(19);
            add(20);
        }
    };

    private MiuiKeyInterceptExtend(Handler handler, Context context, PowerManager powerManager, int currentUserId, boolean singleKeyUse) {
        this.mHandler = handler;
        this.mContext = context;
        this.mPowerManager = powerManager;
        this.mCurrentUserId = currentUserId;
        this.mSingleKeyUse = singleKeyUse;
        LocalServices.addService(InputFeatureSupport.class, new LocalService());
    }

    public static MiuiKeyInterceptExtend getInstance(Handler handler, Context context, PowerManager powerManager, int currentUserId, boolean singleKeyUse) {
        if (INSTANCE == null) {
            synchronized (MiuiKeyInterceptExtend.class) {
                if (INSTANCE == null) {
                    INSTANCE = new MiuiKeyInterceptExtend(handler, context, powerManager, currentUserId, singleKeyUse);
                }
            }
        }
        return INSTANCE;
    }

    public void setCurrentUserId(int currentUserId) {
        this.mCurrentUserId = currentUserId;
    }

    public void setSingleKeyUse(boolean singleKeyUse) {
        this.mSingleKeyUse = singleKeyUse;
    }

    public boolean interceptKey(WindowManagerPolicy.WindowState focusedWin, KeyEvent event, int policyFlags, boolean isScreenOn) {
        int keyCode = event.getKeyCode();
        boolean down = event.getAction() == 0;
        if (PadManager.getInstance().isPad() && !down && keyCode == 115) {
            PadManager.getInstance().setCapsLockStatus(event.isCapsLockOn());
        }
        if (MiuiIICKeyboardManager.supportPadKeyboard() && keyCode == 115 && !down) {
            MiuiPadKeyboardManager.getKeyboardManager(this.mContext).setCapsLockLight(event.isCapsLockOn());
        }
        return false;
    }

    public boolean interceptKeyBeforeDispatching(KeyEvent event) {
        if (MiuiIICKeyboardManager.supportPadKeyboard()) {
            PadManager.getInstance().statusMediaFunction(event);
            if (PadManager.getInstance().adjustBrightnessFromKeycode(event)) {
                return true;
            }
            return false;
        }
        return false;
    }

    public boolean shouldSkipInterceptLocked(WindowManagerPolicy.WindowState focusedWin) {
        synchronized (this.mLock) {
            if (focusedWin != null) {
                List<String> list = this.mSkipInterceptWindows;
                if (list != null && list.size() > 0) {
                    for (String windowName : this.mSkipInterceptWindows) {
                        if (focusedWin.toString().contains(windowName)) {
                            return true;
                        }
                    }
                    return false;
                }
            }
            return false;
        }
    }

    public boolean skipInterceptKeyLocked(KeyEvent event, WindowManagerPolicy.WindowState windowState) {
        return event.getDevice() != null && event.getDevice().isExternal() && event.getDevice().isFullKeyboard() && shouldSkipInterceptLocked(windowState);
    }

    private boolean handleKeyCombination(boolean down, KeyEvent event) {
        return false;
    }

    public void updateSkipInterceptWindowList(List<String> list, String reason) {
        synchronized (this.mLock) {
            this.mSkipInterceptWindows = list;
            if (list != null) {
                MiuiInputLog.defaults("skip policy intercept because: " + reason);
                for (String windowName : list) {
                    MiuiInputLog.defaults("set window skip policy intercept : " + windowName);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class LocalService implements InputFeatureSupport {
        LocalService() {
            MiuiKeyInterceptExtend.this = this$0;
        }

        @Override // com.android.server.input.InputFeatureSupport
        public void setSkipInterceptWindowList(List<String> list, String reason) {
            MiuiKeyInterceptExtend.this.updateSkipInterceptWindowList(list, reason);
        }
    }
}
