package com.android.server.policy;

import android.content.Context;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.VibrationEffect;
import android.view.IWindowManager;
import android.view.KeyEvent;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.power.PowerManagerServiceStub;
import com.miui.base.MiuiStubRegistry;
import com.miui.server.input.PadManager;
import com.xiaomi.mirror.MirrorManager;
import java.util.ArrayList;
import java.util.List;
import miui.os.DeviceFeature;
import miui.util.HapticFeedbackUtil;
/* loaded from: classes.dex */
public class PhoneWindowManagerStubImpl implements PhoneWindowManagerStub {
    private static final List<String> DELIVE_META_APPS;
    private static final boolean SUPPORT_FOD = SystemProperties.getBoolean("ro.hardware.fp.fod", false);
    static final String TAG = "PhoneWindowManagerStubImpl";
    private int SUB_DISPLAY_ID = 2;
    private DisplayTurnoverManager mDisplayTurnoverManager;
    private WindowManagerPolicy.WindowState mFocusedWindow;
    HapticFeedbackUtil mHapticFeedbackUtil;
    private PowerManagerServiceStub mPowerManagerServiceImpl;
    private boolean mSupportAOD;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<PhoneWindowManagerStubImpl> {

        /* compiled from: PhoneWindowManagerStubImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final PhoneWindowManagerStubImpl INSTANCE = new PhoneWindowManagerStubImpl();
        }

        public PhoneWindowManagerStubImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public PhoneWindowManagerStubImpl provideNewInstance() {
            return new PhoneWindowManagerStubImpl();
        }
    }

    static {
        ArrayList arrayList = new ArrayList();
        DELIVE_META_APPS = arrayList;
        arrayList.add("com.ss.android.lark.kami");
    }

    public void init(Context context) {
        this.mSupportAOD = context.getResources().getBoolean(17891611);
        this.mPowerManagerServiceImpl = PowerManagerServiceStub.get();
        if (DeviceFeature.IS_SUBSCREEN_DEVICE) {
            this.mDisplayTurnoverManager = new DisplayTurnoverManager(context);
        }
        this.mHapticFeedbackUtil = new HapticFeedbackUtil(context, true);
    }

    public boolean shouldDispatchInputWhenNonInteractive(int keyCode) {
        if (!this.mSupportAOD || keyCode != 354) {
            return SUPPORT_FOD && (keyCode == 0 || keyCode == 354);
        }
        return true;
    }

    public boolean shouldMoveDisplayToTop(int keyCode) {
        if (DeviceFeature.IS_FOLD_DEVICE && this.mSupportAOD && keyCode == 354) {
            return false;
        }
        return true;
    }

    public boolean isInHangUpState() {
        return this.mPowerManagerServiceImpl.isInHangUpState();
    }

    public void setForcedDisplayDensityForUser(IWindowManager windowManager) {
        if (DeviceFeature.IS_SUBSCREEN_DEVICE) {
            try {
                windowManager.setForcedDisplayDensityForUser(this.SUB_DISPLAY_ID, 240, -2);
            } catch (RemoteException e) {
            }
        }
    }

    public void systemBooted() {
        DisplayTurnoverManager displayTurnoverManager;
        if (DeviceFeature.IS_SUBSCREEN_DEVICE && (displayTurnoverManager = this.mDisplayTurnoverManager) != null) {
            displayTurnoverManager.systemReady();
        }
    }

    public boolean interceptKeyBeforeQueueing(KeyEvent event) {
        if (DeviceFeature.IS_SUBSCREEN_DEVICE && this.mDisplayTurnoverManager != null && event != null && event.isWakeKey() && event.getDisplayId() == this.SUB_DISPLAY_ID) {
            if (event.getAction() == 0) {
                this.mDisplayTurnoverManager.switchSubDisplayPowerState(true, "DOUBLE_CLICK");
            }
            return true;
        }
        return false;
    }

    public VibrationEffect convertToMiuiHapticFeedback(int hapticFeedbackConstantId) {
        return this.mHapticFeedbackUtil.convertToMiuiHapticFeedback(hapticFeedbackConstantId);
    }

    public boolean interceptWakeKey(KeyEvent event) {
        if (!MirrorManager.get().isWorking() || !event.isWakeKey()) {
            return event.isCanceled();
        }
        return event.getKeyCode() == 82;
    }

    public void setFocusedWindow(WindowManagerPolicy.WindowState focusedWindow) {
        this.mFocusedWindow = focusedWindow;
    }

    public boolean isPad() {
        return PadManager.getInstance().isPad();
    }

    public boolean interceptKeyWithMeta() {
        WindowManagerPolicy.WindowState windowState = this.mFocusedWindow;
        return windowState == null || !DELIVE_META_APPS.contains(windowState.getOwningPackage());
    }
}
