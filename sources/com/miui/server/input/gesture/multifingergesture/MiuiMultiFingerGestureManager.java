package com.miui.server.input.gesture.multifingergesture;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.SystemProperties;
import android.provider.MiuiSettings;
import android.provider.Settings;
import android.server.am.SplitScreenReporter;
import android.text.TextUtils;
import android.util.Slog;
import android.view.MotionEvent;
import com.miui.server.input.gesture.MiuiGestureListener;
import com.miui.server.input.gesture.MiuiGestureMonitor;
import com.miui.server.input.gesture.multifingergesture.MiuiMultiFingerGestureManager;
import com.miui.server.input.gesture.multifingergesture.gesture.BaseMiuiMultiFingerGesture;
import com.miui.server.input.gesture.multifingergesture.gesture.impl.MiuiThreeFingerDownGesture;
import com.miui.server.input.gesture.multifingergesture.gesture.impl.MiuiThreeFingerHorizontalGesture;
import com.miui.server.input.gesture.multifingergesture.gesture.impl.MiuiThreeFingerLongPressGesture;
import com.miui.server.input.util.ShortCutActionsUtils;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;
/* loaded from: classes.dex */
public class MiuiMultiFingerGestureManager implements MiuiGestureListener {
    private static final String TAG = "MiuiMultiFingerGestureManager";
    private boolean mBootCompleted;
    private final Context mContext;
    private boolean mContinueSendEvent;
    private int mCurrentUserId;
    private boolean mDeviceProvisioned;
    private boolean mEnable;
    private BooleanSupplier mGetKeyguardActiveFunction;
    private final Handler mHandler;
    private boolean mIsRegister;
    private boolean mIsScreenOn;
    private final MiuiGestureMonitor mMiuiGestureMonitor;
    private final MiuiSettingsObserver mMiuiSettingsObserver;
    private boolean mNeedCancel;
    private final Set<String> mNotNeedHapticFeedbackFunction = new HashSet();
    private final List<BaseMiuiMultiFingerGesture> mAllMultiFingerGestureList = new ArrayList();

    public MiuiMultiFingerGestureManager(Context context, Handler mHandler) {
        this.mContext = context;
        this.mHandler = mHandler;
        this.mMiuiGestureMonitor = MiuiGestureMonitor.getInstance(context);
        initAllGesture();
        MiuiSettingsObserver miuiSettingsObserver = new MiuiSettingsObserver(mHandler);
        this.mMiuiSettingsObserver = miuiSettingsObserver;
        miuiSettingsObserver.observe();
        updateSettings();
        registerOnConfigBroadcast();
        initNotNeedHapticFeedbackFunction();
    }

    private void initNotNeedHapticFeedbackFunction() {
        this.mNotNeedHapticFeedbackFunction.add("screen_shot");
        this.mNotNeedHapticFeedbackFunction.add("partial_screen_shot");
        this.mNotNeedHapticFeedbackFunction.add("dump_log");
    }

    private void registerOnConfigBroadcast() {
        this.mContext.registerReceiver(new BroadcastReceiver() { // from class: com.miui.server.input.gesture.multifingergesture.MiuiMultiFingerGestureManager.1
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                MiuiMultiFingerGestureManager.this.onConfigChange();
            }
        }, new IntentFilter("android.intent.action.CONFIGURATION_CHANGED"));
    }

    private void initAllGesture() {
        this.mAllMultiFingerGestureList.add(new MiuiThreeFingerDownGesture(this.mContext, this.mHandler, this));
        this.mAllMultiFingerGestureList.add(new MiuiThreeFingerLongPressGesture(this.mContext, this.mHandler, this));
        this.mAllMultiFingerGestureList.add(new MiuiThreeFingerHorizontalGesture(this.mContext, this.mHandler, this));
        this.mAllMultiFingerGestureList.add(new MiuiThreeFingerHorizontalGesture.MiuiThreeFingerHorizontalLTRGesture(this.mContext, this.mHandler, this));
        this.mAllMultiFingerGestureList.add(new MiuiThreeFingerHorizontalGesture.MiuiThreeFingerHorizontalRTLGesture(this.mContext, this.mHandler, this));
    }

    private void updateAllGestureStatus(final MiuiMultiFingerGestureStatus newStatus, final BaseMiuiMultiFingerGesture except) {
        this.mAllMultiFingerGestureList.forEach(new Consumer() { // from class: com.miui.server.input.gesture.multifingergesture.MiuiMultiFingerGestureManager$$ExternalSyntheticLambda0
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                MiuiMultiFingerGestureManager.lambda$updateAllGestureStatus$0(BaseMiuiMultiFingerGesture.this, newStatus, (BaseMiuiMultiFingerGesture) obj);
            }
        });
    }

    public static /* synthetic */ void lambda$updateAllGestureStatus$0(BaseMiuiMultiFingerGesture except, MiuiMultiFingerGestureStatus newStatus, BaseMiuiMultiFingerGesture multiFingerGesture) {
        MiuiMultiFingerGestureStatus status;
        if (multiFingerGesture == except || (status = multiFingerGesture.getStatus()) == newStatus || status == MiuiMultiFingerGestureStatus.NONE) {
            return;
        }
        multiFingerGesture.changeStatus(newStatus);
    }

    private void initAllGestureStatus() {
        this.mAllMultiFingerGestureList.forEach(new Consumer() { // from class: com.miui.server.input.gesture.multifingergesture.MiuiMultiFingerGestureManager$$ExternalSyntheticLambda3
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                MiuiMultiFingerGestureManager.lambda$initAllGestureStatus$1((BaseMiuiMultiFingerGesture) obj);
            }
        });
    }

    public static /* synthetic */ void lambda$initAllGestureStatus$1(BaseMiuiMultiFingerGesture gesture) {
        MiuiMultiFingerGestureStatus status = gesture.getStatus();
        if (status == MiuiMultiFingerGestureStatus.NONE) {
            return;
        }
        gesture.changeStatus(MiuiMultiFingerGestureStatus.READY);
    }

    @Override // com.miui.server.input.gesture.MiuiGestureListener
    public void onPointerEvent(MotionEvent event) {
        if (!checkNecessaryCondition()) {
            return;
        }
        switch (event.getAction()) {
            case 3:
                Slog.d(TAG, "Receive a cancel event, all gesture will fail.");
                updateAllGestureStatus(MiuiMultiFingerGestureStatus.FAIL, null);
                this.mContinueSendEvent = false;
                return;
            case 0:
                initAllGestureStatus();
                this.mContinueSendEvent = true;
                break;
        }
        if (!this.mContinueSendEvent) {
            return;
        }
        sendEventToGestures(event);
    }

    private boolean checkNecessaryCondition() {
        return this.mDeviceProvisioned && this.mIsScreenOn && this.mEnable;
    }

    private void sendEventToGestures(MotionEvent event) {
        int count = 0;
        this.mNeedCancel = true;
        for (BaseMiuiMultiFingerGesture gesture : this.mAllMultiFingerGestureList) {
            switch (AnonymousClass2.$SwitchMap$com$miui$server$input$gesture$multifingergesture$MiuiMultiFingerGestureStatus[gesture.getStatus().ordinal()]) {
                case 1:
                    gestureStatusIsReady(event, gesture);
                    break;
                case 2:
                    gestureStatusIsDetecting(event, gesture);
                    break;
            }
            MiuiMultiFingerGestureStatus status = gesture.getStatus();
            if (status == MiuiMultiFingerGestureStatus.SUCCESS) {
                return;
            }
            if (status == MiuiMultiFingerGestureStatus.FAIL || status == MiuiMultiFingerGestureStatus.NONE) {
                count++;
            }
        }
        if (count == this.mAllMultiFingerGestureList.size()) {
            this.mContinueSendEvent = false;
        }
    }

    /* renamed from: com.miui.server.input.gesture.multifingergesture.MiuiMultiFingerGestureManager$2 */
    /* loaded from: classes.dex */
    public static /* synthetic */ class AnonymousClass2 {
        static final /* synthetic */ int[] $SwitchMap$com$miui$server$input$gesture$multifingergesture$MiuiMultiFingerGestureStatus;

        static {
            int[] iArr = new int[MiuiMultiFingerGestureStatus.values().length];
            $SwitchMap$com$miui$server$input$gesture$multifingergesture$MiuiMultiFingerGestureStatus = iArr;
            try {
                iArr[MiuiMultiFingerGestureStatus.READY.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$miui$server$input$gesture$multifingergesture$MiuiMultiFingerGestureStatus[MiuiMultiFingerGestureStatus.DETECTING.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
        }
    }

    public void checkSuccess(BaseMiuiMultiFingerGesture gesture) {
        this.mContinueSendEvent = false;
        updateAllGestureStatus(MiuiMultiFingerGestureStatus.FAIL, gesture);
        if (!checkBootCompleted()) {
            return;
        }
        triggerFunction(gesture);
    }

    private void gestureStatusIsDetecting(MotionEvent event, BaseMiuiMultiFingerGesture gesture) {
        if (event.getPointerCount() == gesture.getFunctionNeedFingerNum()) {
            gesture.onTouchEvent(event);
        } else {
            gesture.changeStatus(MiuiMultiFingerGestureStatus.FAIL);
        }
    }

    private void gestureStatusIsReady(MotionEvent event, BaseMiuiMultiFingerGesture gesture) {
        if (event.getPointerCount() != gesture.getFunctionNeedFingerNum()) {
            return;
        }
        if (!gesture.preCondition()) {
            gesture.changeStatus(MiuiMultiFingerGestureStatus.FAIL);
            Slog.i(TAG, gesture.getGestureKey() + " init fail, because pre condition.");
            return;
        }
        gesture.initGesture(event);
        if (gesture.getStatus() == MiuiMultiFingerGestureStatus.DETECTING) {
            checkCts();
            gesture.onTouchEvent(event);
        }
    }

    private boolean checkBootCompleted() {
        if (!this.mBootCompleted) {
            boolean z = SystemProperties.getBoolean("sys.boot_completed", false);
            this.mBootCompleted = z;
            return z;
        }
        return true;
    }

    private void triggerFunction(BaseMiuiMultiFingerGesture gesture) {
        boolean notNeedHapticFeedback = this.mNotNeedHapticFeedbackFunction.contains(gesture.getGestureFunction());
        ShortCutActionsUtils.getInstance(this.mContext).triggerFunction(gesture.getGestureFunction(), gesture.getGestureKey(), null, !notNeedHapticFeedback);
    }

    private void checkCts() {
        if (!this.mNeedCancel) {
            return;
        }
        if (!isCts()) {
            this.mMiuiGestureMonitor.pilferPointers();
            Slog.d(TAG, "Pilfer pointers because the cts is not running.");
        } else {
            Slog.d(TAG, "Not pilfer pointers because the cts is running.");
        }
        this.mNeedCancel = false;
    }

    private boolean isCts() {
        return !SystemProperties.getBoolean("persist.sys.miui_optimization", !SplitScreenReporter.ACTION_ENTER_SPLIT.equals(SystemProperties.get("ro.miui.cts")));
    }

    public void updateScreenState(boolean screenOn) {
        this.mIsScreenOn = screenOn;
    }

    public void onUserSwitch(int newUserId) {
        if (this.mCurrentUserId != newUserId) {
            this.mCurrentUserId = newUserId;
            updateSettings();
        }
    }

    public void initKeyguardActiveFunction(BooleanSupplier getKeyguardActiveFunction) {
        this.mGetKeyguardActiveFunction = getKeyguardActiveFunction;
    }

    public boolean isKeyguardActive() {
        BooleanSupplier booleanSupplier = this.mGetKeyguardActiveFunction;
        if (booleanSupplier == null) {
            return false;
        }
        return booleanSupplier.getAsBoolean();
    }

    public boolean isEmpty(String function) {
        return TextUtils.isEmpty(function) || "none".equals(function);
    }

    private void updateSettings() {
        Settings.System.putInt(this.mContext.getContentResolver(), "enable_three_gesture", 1);
        MiuiSettingsObserver miuiSettingsObserver = this.mMiuiSettingsObserver;
        if (miuiSettingsObserver == null) {
            return;
        }
        miuiSettingsObserver.onChange(false, Settings.System.getUriFor("enable_three_gesture"));
        this.mMiuiSettingsObserver.onChange(false, Settings.Global.getUriFor("device_provisioned"));
        this.mAllMultiFingerGestureList.forEach(new Consumer() { // from class: com.miui.server.input.gesture.multifingergesture.MiuiMultiFingerGestureManager$$ExternalSyntheticLambda4
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                MiuiMultiFingerGestureManager.this.m2212x9227cffd((BaseMiuiMultiFingerGesture) obj);
            }
        });
    }

    /* renamed from: lambda$updateSettings$2$com-miui-server-input-gesture-multifingergesture-MiuiMultiFingerGestureManager */
    public /* synthetic */ void m2212x9227cffd(BaseMiuiMultiFingerGesture gesture) {
        this.mMiuiSettingsObserver.onChange(false, Settings.System.getUriFor(gesture.getGestureKey()));
    }

    public void dump(final String prefix, final PrintWriter pw) {
        pw.print("    ");
        pw.println(TAG);
        this.mAllMultiFingerGestureList.forEach(new Consumer() { // from class: com.miui.server.input.gesture.multifingergesture.MiuiMultiFingerGestureManager$$ExternalSyntheticLambda1
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                MiuiMultiFingerGestureManager.lambda$dump$3(pw, prefix, (BaseMiuiMultiFingerGesture) obj);
            }
        });
        pw.println(prefix + "mBootCompleted = " + this.mBootCompleted);
        pw.println(prefix + "mDeviceProvisioned = " + this.mDeviceProvisioned);
        pw.println(prefix + "mIsScreenOn = " + this.mIsScreenOn);
        pw.println(prefix + "mEnable = " + this.mEnable);
        pw.println(prefix + "isCTS = " + isCts());
    }

    public static /* synthetic */ void lambda$dump$3(PrintWriter pw, String prefix, BaseMiuiMultiFingerGesture gesture) {
        pw.print(prefix);
        pw.println("gestureName=" + gesture.getClass().getSimpleName());
        pw.print(prefix);
        pw.println("gestureKey=" + gesture.getGestureKey());
        pw.print(prefix);
        pw.println("gestureFunction=" + gesture.getGestureFunction());
        pw.println();
    }

    public void onConfigChange() {
        this.mAllMultiFingerGestureList.forEach(new Consumer() { // from class: com.miui.server.input.gesture.multifingergesture.MiuiMultiFingerGestureManager$$ExternalSyntheticLambda2
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ((BaseMiuiMultiFingerGesture) obj).onConfigChange();
            }
        });
    }

    /* loaded from: classes.dex */
    public class MiuiSettingsObserver extends ContentObserver {
        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        MiuiSettingsObserver(Handler handler) {
            super(handler);
            MiuiMultiFingerGestureManager.this = r1;
        }

        void observe() {
            final ContentResolver resolver = MiuiMultiFingerGestureManager.this.mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor("enable_three_gesture"), false, this, -1);
            resolver.registerContentObserver(Settings.Global.getUriFor("device_provisioned"), false, this, -1);
            MiuiMultiFingerGestureManager.this.mAllMultiFingerGestureList.forEach(new Consumer() { // from class: com.miui.server.input.gesture.multifingergesture.MiuiMultiFingerGestureManager$MiuiSettingsObserver$$ExternalSyntheticLambda1
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    MiuiMultiFingerGestureManager.MiuiSettingsObserver.this.m2213x789beb04(resolver, (BaseMiuiMultiFingerGesture) obj);
                }
            });
        }

        /* renamed from: lambda$observe$0$com-miui-server-input-gesture-multifingergesture-MiuiMultiFingerGestureManager$MiuiSettingsObserver */
        public /* synthetic */ void m2213x789beb04(ContentResolver resolver, BaseMiuiMultiFingerGesture baseMiuiMultiFingerGesture) {
            resolver.registerContentObserver(Settings.System.getUriFor(baseMiuiMultiFingerGesture.getGestureKey()), false, this, -1);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, final Uri uri) {
            boolean z = false;
            if (Settings.System.getUriFor("enable_three_gesture").equals(uri)) {
                MiuiMultiFingerGestureManager miuiMultiFingerGestureManager = MiuiMultiFingerGestureManager.this;
                if (Settings.System.getInt(miuiMultiFingerGestureManager.mContext.getContentResolver(), "enable_three_gesture", 1) == 1) {
                    z = true;
                }
                miuiMultiFingerGestureManager.mEnable = z;
                Slog.d(MiuiMultiFingerGestureManager.TAG, "enable_three_gesture_key :" + MiuiMultiFingerGestureManager.this.mEnable);
            } else if (Settings.Global.getUriFor("device_provisioned").equals(uri)) {
                MiuiMultiFingerGestureManager miuiMultiFingerGestureManager2 = MiuiMultiFingerGestureManager.this;
                if (Settings.Global.getInt(miuiMultiFingerGestureManager2.mContext.getContentResolver(), "device_provisioned", 0) != 0) {
                    z = true;
                }
                miuiMultiFingerGestureManager2.mDeviceProvisioned = z;
            } else {
                Optional<BaseMiuiMultiFingerGesture> result = MiuiMultiFingerGestureManager.this.mAllMultiFingerGestureList.stream().filter(new Predicate() { // from class: com.miui.server.input.gesture.multifingergesture.MiuiMultiFingerGestureManager$MiuiSettingsObserver$$ExternalSyntheticLambda0
                    @Override // java.util.function.Predicate
                    public final boolean test(Object obj) {
                        boolean equals;
                        equals = Settings.System.getUriFor(((BaseMiuiMultiFingerGesture) obj).getGestureKey()).equals(uri);
                        return equals;
                    }
                }).findFirst();
                if (!result.isPresent()) {
                    return;
                }
                BaseMiuiMultiFingerGesture baseMiuimultiFingerGesture = result.get();
                String function = MiuiSettings.Key.getKeyAndGestureShortcutFunction(MiuiMultiFingerGestureManager.this.mContext, baseMiuimultiFingerGesture.getGestureKey());
                baseMiuimultiFingerGesture.setGestureFunction(function);
                Slog.d(MiuiMultiFingerGestureManager.TAG, baseMiuimultiFingerGesture.getGestureKey() + " :" + function);
                updateGestures(baseMiuimultiFingerGesture);
            }
        }

        private void updateGestures(BaseMiuiMultiFingerGesture gesture) {
            gesture.changeStatus(MiuiMultiFingerGestureManager.this.isEmpty(gesture.getGestureFunction()) ? MiuiMultiFingerGestureStatus.NONE : MiuiMultiFingerGestureStatus.READY);
            byte num = 0;
            for (BaseMiuiMultiFingerGesture baseMiuiMultiFingerGesture : MiuiMultiFingerGestureManager.this.mAllMultiFingerGestureList) {
                if (!MiuiMultiFingerGestureManager.this.isEmpty(baseMiuiMultiFingerGesture.getGestureFunction())) {
                    num = (byte) (num + 1);
                }
            }
            if (!MiuiMultiFingerGestureManager.this.mIsRegister && num > 0) {
                Slog.d(MiuiMultiFingerGestureManager.TAG, "The gesture has been add,register pointer event listener.");
                MiuiMultiFingerGestureManager.this.mMiuiGestureMonitor.registerPointerEventListener(MiuiMultiFingerGestureManager.this);
                MiuiMultiFingerGestureManager.this.mIsRegister = true;
            } else if (MiuiMultiFingerGestureManager.this.mIsRegister && num == 0) {
                Slog.d(MiuiMultiFingerGestureManager.TAG, "The gestures has been all removed, unregister pointer event listener.");
                MiuiMultiFingerGestureManager.this.mMiuiGestureMonitor.unregisterPointerEventListener(MiuiMultiFingerGestureManager.this);
                MiuiMultiFingerGestureManager.this.mIsRegister = false;
            }
        }
    }
}
