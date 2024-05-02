package com.android.server.padkeyboard;

import android.content.Context;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Slog;
import com.android.server.wm.MiuiFreeformPinManagerService;
import java.io.PrintWriter;
/* loaded from: classes.dex */
public class AngleStateController {
    private static final String TAG = "AngleStateController";
    private Context mContext;
    private AngleState mCurrentState;
    private PowerManager mPowerManager;
    private volatile boolean mShouldIgnoreKeyboard = false;
    private volatile boolean mLidOpen = true;
    private volatile int mKbLevel = 2;
    private volatile boolean mTabletOpen = true;
    private volatile boolean mIdentityPass = true;

    /* loaded from: classes.dex */
    public static abstract class AngleState extends Enum<AngleState> {
        private static final /* synthetic */ AngleState[] $VALUES;
        public static final AngleState BACK_STATE;
        public static final AngleState CLOSE_STATE;
        public static final AngleState NO_WORK_STATE_1;
        public static final AngleState NO_WORK_STATE_2;
        public static final AngleState WORK_STATE_1;
        public static final AngleState WORK_STATE_2;
        protected float lower;
        protected OnChangeListener onChangeListener;
        protected float upper;

        /* loaded from: classes.dex */
        public interface OnChangeListener {
            void onChange(boolean z);
        }

        public abstract boolean isCurrentState(float f);

        public abstract AngleState toNextState(float f);

        /* renamed from: com.android.server.padkeyboard.AngleStateController$AngleState$1 */
        /* loaded from: classes.dex */
        final class AnonymousClass1 extends AngleState {
            private AnonymousClass1(String str, int i, float lower, float upper) {
                super(str, i, lower, upper);
            }

            @Override // com.android.server.padkeyboard.AngleStateController.AngleState
            public boolean isCurrentState(float angle) {
                return this.lower <= angle && angle < this.upper;
            }

            @Override // com.android.server.padkeyboard.AngleStateController.AngleState
            public AngleState toNextState(float angle) {
                if (angle >= this.upper && angle <= 360.0f - this.upper) {
                    onChange(angle >= this.upper);
                    return NO_WORK_STATE_1.toNextState(angle);
                }
                return this;
            }
        }

        public static AngleState valueOf(String name) {
            return (AngleState) Enum.valueOf(AngleState.class, name);
        }

        public static AngleState[] values() {
            return (AngleState[]) $VALUES.clone();
        }

        static {
            AnonymousClass1 anonymousClass1 = new AnonymousClass1("CLOSE_STATE", 0, MiuiFreeformPinManagerService.EDGE_AREA, 5.0f);
            CLOSE_STATE = anonymousClass1;
            AnonymousClass2 anonymousClass2 = new AnonymousClass2("NO_WORK_STATE_1", 1, 5.0f, 15.0f);
            NO_WORK_STATE_1 = anonymousClass2;
            AnonymousClass3 anonymousClass3 = new AnonymousClass3("WORK_STATE_1", 2, 15.0f, 90.0f);
            WORK_STATE_1 = anonymousClass3;
            AnonymousClass4 anonymousClass4 = new AnonymousClass4("WORK_STATE_2", 3, 90.0f, 185.0f);
            WORK_STATE_2 = anonymousClass4;
            AnonymousClass5 anonymousClass5 = new AnonymousClass5("NO_WORK_STATE_2", 4, 185.0f, 355.0f);
            NO_WORK_STATE_2 = anonymousClass5;
            AnonymousClass6 anonymousClass6 = new AnonymousClass6("BACK_STATE", 5, 355.0f, 360.0f);
            BACK_STATE = anonymousClass6;
            $VALUES = new AngleState[]{anonymousClass1, anonymousClass2, anonymousClass3, anonymousClass4, anonymousClass5, anonymousClass6};
        }

        /* renamed from: com.android.server.padkeyboard.AngleStateController$AngleState$2 */
        /* loaded from: classes.dex */
        final class AnonymousClass2 extends AngleState {
            private AnonymousClass2(String str, int i, float lower, float upper) {
                super(str, i, lower, upper);
            }

            @Override // com.android.server.padkeyboard.AngleStateController.AngleState
            public boolean isCurrentState(float angle) {
                return this.lower <= angle && angle < this.upper;
            }

            @Override // com.android.server.padkeyboard.AngleStateController.AngleState
            public AngleState toNextState(float angle) {
                boolean z = true;
                if (angle >= this.upper) {
                    if (angle < this.upper) {
                        z = false;
                    }
                    onChange(z);
                    return WORK_STATE_1.toNextState(angle);
                } else if (angle < this.lower) {
                    if (angle < this.upper) {
                        z = false;
                    }
                    onChange(z);
                    return CLOSE_STATE.toNextState(angle);
                } else {
                    return this;
                }
            }
        }

        /* renamed from: com.android.server.padkeyboard.AngleStateController$AngleState$3 */
        /* loaded from: classes.dex */
        final class AnonymousClass3 extends AngleState {
            private AnonymousClass3(String str, int i, float lower, float upper) {
                super(str, i, lower, upper);
            }

            @Override // com.android.server.padkeyboard.AngleStateController.AngleState
            public boolean isCurrentState(float angle) {
                return this.lower <= angle && angle < this.upper;
            }

            @Override // com.android.server.padkeyboard.AngleStateController.AngleState
            public AngleState toNextState(float angle) {
                boolean z = true;
                if (angle >= this.upper) {
                    if (angle < this.upper) {
                        z = false;
                    }
                    onChange(z);
                    return WORK_STATE_2.toNextState(angle);
                } else if (angle < this.lower) {
                    if (angle < this.upper) {
                        z = false;
                    }
                    onChange(z);
                    return NO_WORK_STATE_1.toNextState(angle);
                } else {
                    return this;
                }
            }
        }

        /* renamed from: com.android.server.padkeyboard.AngleStateController$AngleState$4 */
        /* loaded from: classes.dex */
        final class AnonymousClass4 extends AngleState {
            private AnonymousClass4(String str, int i, float lower, float upper) {
                super(str, i, lower, upper);
            }

            @Override // com.android.server.padkeyboard.AngleStateController.AngleState
            public boolean isCurrentState(float angle) {
                return this.lower <= angle && angle < this.upper;
            }

            @Override // com.android.server.padkeyboard.AngleStateController.AngleState
            public AngleState toNextState(float angle) {
                boolean z = true;
                if (angle >= this.upper) {
                    if (angle < this.upper) {
                        z = false;
                    }
                    onChange(z);
                    return NO_WORK_STATE_2.toNextState(angle);
                } else if (angle < this.lower) {
                    if (angle < this.upper) {
                        z = false;
                    }
                    onChange(z);
                    return WORK_STATE_1.toNextState(angle);
                } else {
                    return this;
                }
            }
        }

        /* renamed from: com.android.server.padkeyboard.AngleStateController$AngleState$5 */
        /* loaded from: classes.dex */
        final class AnonymousClass5 extends AngleState {
            private AnonymousClass5(String str, int i, float lower, float upper) {
                super(str, i, lower, upper);
            }

            @Override // com.android.server.padkeyboard.AngleStateController.AngleState
            public boolean isCurrentState(float angle) {
                return this.lower <= angle && angle < this.upper;
            }

            @Override // com.android.server.padkeyboard.AngleStateController.AngleState
            public AngleState toNextState(float angle) {
                boolean z = true;
                if (angle >= this.upper) {
                    if (angle < this.upper) {
                        z = false;
                    }
                    onChange(z);
                    return BACK_STATE.toNextState(angle);
                } else if (angle < this.lower) {
                    if (angle < this.upper) {
                        z = false;
                    }
                    onChange(z);
                    return WORK_STATE_2.toNextState(angle);
                } else {
                    return this;
                }
            }
        }

        /* renamed from: com.android.server.padkeyboard.AngleStateController$AngleState$6 */
        /* loaded from: classes.dex */
        final class AnonymousClass6 extends AngleState {
            private AnonymousClass6(String str, int i, float lower, float upper) {
                super(str, i, lower, upper);
            }

            @Override // com.android.server.padkeyboard.AngleStateController.AngleState
            public boolean isCurrentState(float angle) {
                return this.lower <= angle && angle <= this.upper;
            }

            @Override // com.android.server.padkeyboard.AngleStateController.AngleState
            public AngleState toNextState(float angle) {
                if (angle < this.lower && angle > 360.0f - this.lower) {
                    onChange(angle >= this.upper);
                    return NO_WORK_STATE_2.toNextState(angle);
                }
                return this;
            }
        }

        private AngleState(String str, int i, float lower, float upper) {
            super(str, i);
            this.upper = upper;
            this.lower = lower;
        }

        public void onChange(boolean toUpper) {
            OnChangeListener onChangeListener = this.onChangeListener;
            if (onChangeListener != null) {
                onChangeListener.onChange(toUpper);
            }
        }

        public void setOnChangeListener(OnChangeListener onChangeListener) {
            this.onChangeListener = onChangeListener;
        }
    }

    public AngleStateController(Context context) {
        this.mContext = context;
        this.mPowerManager = (PowerManager) context.getSystemService("power");
        initState();
    }

    public void updateAngleState(float angle) {
        if (Float.isNaN(angle)) {
            return;
        }
        if (!this.mLidOpen) {
            angle = MiuiFreeformPinManagerService.EDGE_AREA;
        }
        if (!this.mTabletOpen) {
            angle = 360.0f;
        }
        if (this.mCurrentState == null) {
            initCurrentState();
        }
        if (!this.mCurrentState.isCurrentState(angle)) {
            this.mCurrentState = this.mCurrentState.toNextState(angle);
        }
    }

    private void initCurrentState() {
        this.mCurrentState = AngleState.CLOSE_STATE;
    }

    private void initState() {
        AngleState.CLOSE_STATE.setOnChangeListener(new AngleState.OnChangeListener() { // from class: com.android.server.padkeyboard.AngleStateController$$ExternalSyntheticLambda0
            @Override // com.android.server.padkeyboard.AngleStateController.AngleState.OnChangeListener
            public final void onChange(boolean z) {
                AngleStateController.this.m1196xfaf4d999(z);
            }
        });
        AngleState.NO_WORK_STATE_1.setOnChangeListener(new AngleState.OnChangeListener() { // from class: com.android.server.padkeyboard.AngleStateController$$ExternalSyntheticLambda1
            @Override // com.android.server.padkeyboard.AngleStateController.AngleState.OnChangeListener
            public final void onChange(boolean z) {
                AngleStateController.this.m1197x882f8b1a(z);
            }
        });
        AngleState.WORK_STATE_1.setOnChangeListener(new AngleState.OnChangeListener() { // from class: com.android.server.padkeyboard.AngleStateController$$ExternalSyntheticLambda2
            @Override // com.android.server.padkeyboard.AngleStateController.AngleState.OnChangeListener
            public final void onChange(boolean z) {
                AngleStateController.this.m1198x156a3c9b(z);
            }
        });
        AngleState.WORK_STATE_2.setOnChangeListener(new AngleState.OnChangeListener() { // from class: com.android.server.padkeyboard.AngleStateController$$ExternalSyntheticLambda3
            @Override // com.android.server.padkeyboard.AngleStateController.AngleState.OnChangeListener
            public final void onChange(boolean z) {
                AngleStateController.this.m1199xa2a4ee1c(z);
            }
        });
        AngleState.NO_WORK_STATE_2.setOnChangeListener(new AngleState.OnChangeListener() { // from class: com.android.server.padkeyboard.AngleStateController$$ExternalSyntheticLambda4
            @Override // com.android.server.padkeyboard.AngleStateController.AngleState.OnChangeListener
            public final void onChange(boolean z) {
                AngleStateController.this.m1200x2fdf9f9d(z);
            }
        });
        AngleState.BACK_STATE.setOnChangeListener(new AngleState.OnChangeListener() { // from class: com.android.server.padkeyboard.AngleStateController$$ExternalSyntheticLambda5
            @Override // com.android.server.padkeyboard.AngleStateController.AngleState.OnChangeListener
            public final void onChange(boolean z) {
                AngleStateController.this.m1201xbd1a511e(z);
            }
        });
    }

    /* renamed from: lambda$initState$0$com-android-server-padkeyboard-AngleStateController */
    public /* synthetic */ void m1196xfaf4d999(boolean toUpper) {
        Slog.i(TAG, "AngleState CLOSE_STATE onchange toUpper: " + toUpper);
        this.mShouldIgnoreKeyboard = true;
    }

    /* renamed from: lambda$initState$1$com-android-server-padkeyboard-AngleStateController */
    public /* synthetic */ void m1197x882f8b1a(boolean toUpper) {
        Slog.i(TAG, "AngleState NO_WORK_STATE_1 onchange toUpper: " + toUpper);
        this.mShouldIgnoreKeyboard = !toUpper;
    }

    /* renamed from: lambda$initState$2$com-android-server-padkeyboard-AngleStateController */
    public /* synthetic */ void m1198x156a3c9b(boolean toUpper) {
        Slog.i(TAG, "AngleState WORK_STATE_1 onchange toUpper: " + toUpper);
        this.mShouldIgnoreKeyboard = !toUpper;
    }

    /* renamed from: lambda$initState$3$com-android-server-padkeyboard-AngleStateController */
    public /* synthetic */ void m1199xa2a4ee1c(boolean toUpper) {
        Slog.i(TAG, "AngleState WORK_STATE_2 onchange toUpper: " + toUpper);
        this.mShouldIgnoreKeyboard = toUpper;
    }

    /* renamed from: lambda$initState$4$com-android-server-padkeyboard-AngleStateController */
    public /* synthetic */ void m1200x2fdf9f9d(boolean toUpper) {
        Slog.i(TAG, "AngleState NO_WORK_STATE_2 onchange toUpper: " + toUpper);
        this.mShouldIgnoreKeyboard = toUpper;
    }

    /* renamed from: lambda$initState$5$com-android-server-padkeyboard-AngleStateController */
    public /* synthetic */ void m1201xbd1a511e(boolean toUpper) {
        Slog.i(TAG, "AngleState BACK_STATE onchange toUpper: " + toUpper);
        this.mShouldIgnoreKeyboard = true;
    }

    public boolean shouldIgnoreKeyboard() {
        if (!this.mIdentityPass) {
            return true;
        }
        if (this.mKbLevel == 2) {
            return false;
        }
        return !this.mLidOpen || this.mShouldIgnoreKeyboard;
    }

    public boolean shouldIgnoreKeyboardForIIC() {
        return !this.mIdentityPass || !this.mLidOpen || !this.mTabletOpen || this.mShouldIgnoreKeyboard;
    }

    public boolean isWorkState() {
        return !this.mShouldIgnoreKeyboard;
    }

    public boolean getLidStatus() {
        return this.mLidOpen;
    }

    public boolean getTabletStatus() {
        return this.mTabletOpen;
    }

    public boolean getIdentityStatus() {
        return this.mIdentityPass;
    }

    public void setKbLevel(int kbLevel, boolean shouldWakeUp) {
        this.mKbLevel = kbLevel;
        if (shouldWakeUp && this.mKbLevel == 2 && this.mLidOpen && this.mTabletOpen) {
            this.mPowerManager.wakeUp(SystemClock.uptimeMillis(), 0, "usb_keyboard_attach");
        }
    }

    public void wakeUpIfNeed(boolean shouldWakeUp) {
        if (MiuiIICKeyboardManager.supportPadKeyboard() && shouldWakeUp && this.mLidOpen && this.mTabletOpen) {
            this.mPowerManager.wakeUp(SystemClock.uptimeMillis(), 0, "miui_keyboard_attach");
        }
    }

    public void notifyLidSwitchChanged(boolean lidOpen) {
        this.mLidOpen = lidOpen;
        Slog.i(TAG, "notifyLidSwitchChanged: " + lidOpen);
    }

    public void notifyTabletSwitchChanged(boolean tabletOpen) {
        this.mTabletOpen = tabletOpen;
        Slog.i(TAG, "notifyTabletSwitchChanged: " + tabletOpen);
    }

    public void setIdentityState(boolean identityPass) {
        Slog.i(TAG, "Keyboard authentication status: " + identityPass);
        this.mIdentityPass = identityPass;
    }

    public void resetAngleStatus() {
        this.mShouldIgnoreKeyboard = false;
    }

    public void dump(String prefix, PrintWriter pw) {
        pw.print(prefix);
        pw.print("mCurrentState=");
        pw.println(this.mCurrentState);
        pw.print(prefix);
        pw.print("mIdentityPass=");
        pw.println(this.mIdentityPass);
        pw.print(prefix);
        pw.print("mLidOpen=");
        pw.println(this.mLidOpen);
        pw.print(prefix);
        pw.print("mTabletOpen=");
        pw.println(this.mTabletOpen);
        pw.print(prefix);
        pw.print("mShouldIgnoreKeyboard=");
        pw.println(this.mShouldIgnoreKeyboard);
    }
}
