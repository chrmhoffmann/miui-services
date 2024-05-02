package com.android.server.wm;

import android.app.servertransaction.BoundsCompatInfoChangeItem;
import android.app.servertransaction.BoundsCompatStub;
import android.app.servertransaction.ClientTransaction;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Slog;
import com.miui.base.MiuiStubRegistry;
/* loaded from: classes.dex */
public class ProcessCompatController implements ProcessCompatControllerStub {
    private static final String TAG = "ProcessCompat";
    private ActivityTaskManagerService mAtm;
    private float mFixedAspectRatio;
    private WindowProcessController mProcess;
    private int mState = 0;
    private Rect mBounds = new Rect(0, 0, 1, 1);

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<ProcessCompatController> {

        /* compiled from: ProcessCompatController$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final ProcessCompatController INSTANCE = new ProcessCompatController();
        }

        public ProcessCompatController provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public ProcessCompatController provideNewInstance() {
            return new ProcessCompatController();
        }
    }

    public void initBoundsCompatController(ActivityTaskManagerService atm, WindowProcessController process) {
        this.mAtm = atm;
        this.mProcess = process;
        String packageName = process.mInfo.packageName;
        this.mFixedAspectRatio = ActivityTaskManagerServiceStub.get().getAspectRatio(packageName);
        updateState();
    }

    public Configuration getCompatConfiguration(Configuration config) {
        if (isFixedAspectRatioEnabled()) {
            return BoundsCompatStub.get().getCompatConfiguration(config, this.mFixedAspectRatio);
        }
        return config;
    }

    public boolean isFixedAspectRatioEnabled() {
        return BoundsCompatStub.get().isFixedAspectRatioModeEnabled(this.mState);
    }

    public void sendCompatState() {
        if (this.mProcess.getThread() == null) {
            return;
        }
        ClientTransaction transaction = ClientTransaction.obtain(this.mProcess.getThread(), (IBinder) null);
        transaction.addCallback(BoundsCompatInfoChangeItem.obtain(this.mState, this.mBounds));
        try {
            this.mAtm.getLifecycleManager().scheduleTransaction(transaction);
        } catch (RemoteException e) {
        }
    }

    public void updateConfiguration(Configuration config) {
        updateState();
    }

    public void onSetThread() {
        if (this.mState != 0) {
            sendCompatState();
        }
    }

    private void updateState() {
        int newState;
        int newState2 = this.mState;
        if (canUseFixedAspectRatio()) {
            newState = newState2 | 8;
            Slog.i(TAG, "Set " + this.mProcess.mName + " fixed-aspect-ratio " + this.mFixedAspectRatio);
        } else {
            newState = newState2 & (-9);
        }
        if (this.mState != newState) {
            Slog.i(TAG, "Update " + this.mProcess.mName + " comapt state " + this.mState + "->" + newState);
            this.mState = newState;
            sendCompatState();
        }
    }

    public boolean canUseFixedAspectRatio() {
        if (this.mFixedAspectRatio <= MiuiFreeformPinManagerService.EDGE_AREA || this.mProcess.mUid < 10000 || ActivityTaskManagerServiceStub.get().shouldNotApplyAspectRatio() || this.mAtm.mWindowManager.mPolicy.isDisplayFolded()) {
            return false;
        }
        return true;
    }
}
