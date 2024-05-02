package com.android.server.input;

import android.content.Context;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.provider.MiuiSettings;
import android.server.am.SplitScreenReporter;
import android.util.Slog;
import android.util.SparseArray;
import android.view.KeyboardShortcutInfo;
import com.android.server.am.ActivityManagerServiceImpl;
import com.android.server.input.overscroller.ScrollerOptimizationConfigProvider;
import com.android.server.padkeyboard.MiuiPadKeyboardManager;
import com.miui.server.input.PadManager;
import com.miui.server.input.edgesuppression.EdgeSuppressionManager;
import com.miui.server.input.util.MiuiCustomizeShortCutUtils;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import miui.hardware.input.IAppScrollerOptimizationConfigChangedListener;
import miui.hardware.input.IMiuiInputManager;
import miui.hardware.input.IShortcutSettingsChangedListener;
import miui.hardware.input.MiuiKeyboardStatus;
import miui.util.ITouchFeature;
/* loaded from: classes.dex */
public class MiuiInputManagerService extends IMiuiInputManager.Stub {
    private static final String TAG = "MiuiInputManagerService";
    private final Context mContext;
    private EdgeSuppressionManager mEdgeSuppressionManager;
    private MiuiCustomizeShortCutUtils mMiuiCustomizeShortCutUtils;
    private MiuiPadKeyboardManager mMiuiPadKeyboardManager;
    private final Object mMiuiShortcutSettingsLock = new Object();
    private final SparseArray<ShortcutListenerRecord> mShortcutListeners = new SparseArray<>();
    private final ArrayList<ShortcutListenerRecord> mShortcutListenersToNotify = new ArrayList<>();
    private final Handler mHandler = new H(MiuiInputThread.getThread().getLooper());

    public MiuiInputManagerService(Context context) {
        this.mContext = context;
        if (PadManager.getInstance().isPad()) {
            this.mMiuiCustomizeShortCutUtils = MiuiCustomizeShortCutUtils.getInstance(context);
            this.mMiuiPadKeyboardManager = MiuiPadKeyboardManager.getKeyboardManager(context);
        }
        if (ITouchFeature.getInstance().hasSupportEdgeMode()) {
            this.mEdgeSuppressionManager = EdgeSuppressionManager.getInstance(context);
        }
    }

    public void updateKeyboardShortcut(KeyboardShortcutInfo info, int type) {
        MiuiCustomizeShortCutUtils miuiCustomizeShortCutUtils = this.mMiuiCustomizeShortCutUtils;
        if (miuiCustomizeShortCutUtils != null) {
            miuiCustomizeShortCutUtils.updateKeyboardShortcut(info, type);
        } else {
            Slog.e(TAG, "Can't update Keyboard ShortcutKey because Not Support");
        }
    }

    public List<KeyboardShortcutInfo> getKeyboardShortcut() {
        MiuiCustomizeShortCutUtils miuiCustomizeShortCutUtils = this.mMiuiCustomizeShortCutUtils;
        if (miuiCustomizeShortCutUtils != null) {
            return miuiCustomizeShortCutUtils.getKeyboardShortcutInfo();
        }
        Slog.e(TAG, "Can't get Keyboard ShortcutKey because Not Support");
        return null;
    }

    public String getMiKeyboardStatus() {
        if (this.mMiuiPadKeyboardManager != null && PadManager.getInstance().isPad()) {
            this.mMiuiPadKeyboardManager = MiuiPadKeyboardManager.getKeyboardManager(this.mContext);
        }
        MiuiPadKeyboardManager miuiPadKeyboardManager = this.mMiuiPadKeyboardManager;
        if (miuiPadKeyboardManager != null) {
            return getKeyboardStatus(miuiPadKeyboardManager.getKeyboardStatus());
        }
        Slog.e(TAG, "Get Keyboard Status fail because Not Support");
        return null;
    }

    public int[] getEdgeSuppressionSize(boolean isAbsolute) {
        EdgeSuppressionManager edgeSuppressionManager = this.mEdgeSuppressionManager;
        if (edgeSuppressionManager != null) {
            return isAbsolute ? edgeSuppressionManager.getAbsoluteLevel() : edgeSuppressionManager.getConditionLevel();
        }
        Slog.e(TAG, "Can't get EdgeSuppression Info because not Support");
        return null;
    }

    public boolean setCursorPosition(float x, float y) {
        Slog.i(TAG, "setCursorPosition(" + x + ", " + y + ")");
        return MiInputManager.getInstance().setCursorPosition(x, y);
    }

    public boolean hideCursor() {
        Slog.i(TAG, "hideCursor");
        return MiInputManager.getInstance().hideCursor();
    }

    public String[] getAppScrollerOptimizationConfig(String packageName) {
        return ScrollerOptimizationConfigProvider.getInstance().getAppScrollerOptimizationConfigAndSwitchState(packageName);
    }

    public void registerAppScrollerOptimizationConfigListener(String packageName, IAppScrollerOptimizationConfigChangedListener listener) {
        ScrollerOptimizationConfigProvider.getInstance().registerAppScrollerOptimizationConfigListener(packageName, listener);
    }

    private String getKeyboardStatus(MiuiKeyboardStatus status) {
        StringBuilder result = new StringBuilder("Keyboard is Normal");
        if (status.shouldIgnoreKeyboard()) {
            result.setLength(0);
            if (!status.isAngleStatusWork()) {
                result.append("AngleStatus Exception ");
            } else if (!status.isAuthStatus()) {
                result.append("AuthStatus Exception ");
            } else if (!status.isLidStatus()) {
                result.append("LidStatus Exception ");
            } else if (!status.isTabletStatus()) {
                result.append("TabletStatus Exception ");
            } else if (!status.isConnected()) {
                result.append("The Keyboard is disConnect ");
            }
        }
        result.append(", MCU:").append(status.getMCUVersion()).append(", Keyboard:").append(status.getKeyboardVersion());
        return result.toString();
    }

    public boolean putStringForUser(String action, String function) {
        if (!ActivityManagerServiceImpl.getInstance().isSystemApp(Binder.getCallingPid())) {
            Slog.e(TAG, "Not Support normal application interaction gesture shortcut");
            return false;
        }
        String targetPackage = ActivityManagerServiceImpl.getInstance().getPackageNameForPid(Binder.getCallingPid());
        Slog.i(TAG, "Changed Action:" + action + ",new Function:" + function + ",because for " + targetPackage);
        long origId = Binder.clearCallingIdentity();
        MiuiSettings.System.putStringForUser(this.mContext.getContentResolver(), action, function, -2);
        sendSettingsChangedMessage(1, action, function);
        Binder.restoreCallingIdentity(origId);
        return true;
    }

    public void registerShortcutChangedListener(IShortcutSettingsChangedListener listener) {
        if (listener == null || !ActivityManagerServiceImpl.getInstance().isSystemApp(Binder.getCallingPid())) {
            Slog.e(TAG, "Not Support normal application interaction gesture shortcut");
            return;
        }
        synchronized (this.mMiuiShortcutSettingsLock) {
            int callingPid = Binder.getCallingPid();
            if (this.mShortcutListeners.get(callingPid) != null) {
                throw new IllegalArgumentException("Can't register repeat listener to MiuiShortcutSettings");
            }
            ShortcutListenerRecord shortcutListenerRecord = new ShortcutListenerRecord(listener, callingPid);
            try {
                IBinder binder = listener.asBinder();
                binder.linkToDeath(shortcutListenerRecord, 0);
                this.mShortcutListeners.put(callingPid, shortcutListenerRecord);
            } catch (RemoteException ex) {
                throw new IllegalStateException(ex);
            }
        }
    }

    public void unregisterShortcutChangedListener() {
        if (!ActivityManagerServiceImpl.getInstance().isSystemApp(Binder.getCallingPid())) {
            Slog.e(TAG, "Not Support normal application interaction gesture shortcut");
        } else {
            removeListenerFromSystem(Binder.getCallingPid());
        }
    }

    public void removeListenerFromSystem(int pid) {
        synchronized (this.mMiuiShortcutSettingsLock) {
            this.mShortcutListeners.delete(pid);
        }
    }

    private void sendSettingsChangedMessage(int messageWhat, String action, String newFunction) {
        Bundle bundle = new Bundle();
        bundle.putString(SplitScreenReporter.STR_ACTION, action);
        bundle.putString("function", newFunction);
        Message msg = this.mHandler.obtainMessage(messageWhat);
        msg.setData(bundle);
        msg.sendToTarget();
    }

    /* loaded from: classes.dex */
    public class ShortcutListenerRecord implements IBinder.DeathRecipient {
        private final IShortcutSettingsChangedListener mChangedListener;
        private final int mPid;

        ShortcutListenerRecord(IShortcutSettingsChangedListener settingsChangedListener, int pid) {
            MiuiInputManagerService.this = r1;
            this.mChangedListener = settingsChangedListener;
            this.mPid = pid;
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            MiuiInputManagerService.this.removeListenerFromSystem(this.mPid);
        }

        public void notifyShortcutSettingsChanged(String action, String function) {
            try {
                this.mChangedListener.onSettingsChanged(action, function);
            } catch (RemoteException ex) {
                Slog.w(MiuiInputManagerService.TAG, "Failed to notify process " + this.mPid + " that Settings Changed, assuming it died.", ex);
                binderDied();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class H extends Handler {
        private static final String DATA_ACTION = "action";
        private static final String DATA_FUNCTION = "function";
        private static final int MSG_NOTIFY_LISTENER = 1;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public H(Looper looper) {
            super(looper);
            MiuiInputManagerService.this = this$0;
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            Bundle bundle = msg.getData();
            String action = bundle.getString("action", "");
            String function = bundle.getString(DATA_FUNCTION, "");
            if (msg.what == 1) {
                MiuiInputManagerService.this.mShortcutListenersToNotify.clear();
                synchronized (MiuiInputManagerService.this.mMiuiShortcutSettingsLock) {
                    for (int i = 0; i < MiuiInputManagerService.this.mShortcutListeners.size(); i++) {
                        MiuiInputManagerService.this.mShortcutListenersToNotify.add((ShortcutListenerRecord) MiuiInputManagerService.this.mShortcutListeners.valueAt(i));
                    }
                }
                Iterator it = MiuiInputManagerService.this.mShortcutListenersToNotify.iterator();
                while (it.hasNext()) {
                    ShortcutListenerRecord record = (ShortcutListenerRecord) it.next();
                    record.notifyShortcutSettingsChanged(action, function);
                }
            }
        }
    }
}
