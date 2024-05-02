package com.android.server.am;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Slog;
import java.lang.reflect.Method;
import miui.util.ReflectionUtils;
/* loaded from: classes.dex */
public class ProcessGameCleaner {
    private static final String TAG = "ProcessGameCleaner";
    private ActivityManagerService mAms;
    private H mHandler;
    private Method mMethodUpdateOomForGame;
    private ProcessManagerService mPms;
    private SystemPressureController mSystemPressureCtl;
    private static int HOT_START_FOR_GAME_DALAYED = 0;
    private static int CLOD_START_FOR_GAME_DALAYED = 30000;

    public ProcessGameCleaner(ActivityManagerService ams) {
        this.mAms = ams;
    }

    public void onSystemReady(ProcessManagerService pms, Looper myLooper) {
        this.mMethodUpdateOomForGame = ReflectionUtils.tryFindMethodExact(this.mAms.getClass(), "updateOomForGame", new Class[]{Boolean.TYPE});
        this.mSystemPressureCtl = SystemPressureController.getInstance();
        this.mPms = pms;
        this.mHandler = new H(myLooper);
    }

    public void foregroundInfoChanged(String pckName, int pid) {
        if (this.mMethodUpdateOomForGame == null || this.mAms == null) {
            return;
        }
        this.mHandler.removeMessages(1);
        if (this.mSystemPressureCtl.isGameApp(pckName)) {
            Message msg = this.mHandler.obtainMessage(1);
            boolean isClodApp = isColdStartApp(pid);
            msg.obj = true;
            this.mHandler.sendMessageDelayed(msg, isClodApp ? CLOD_START_FOR_GAME_DALAYED : HOT_START_FOR_GAME_DALAYED);
            return;
        }
        Message msg2 = this.mHandler.obtainMessage(1);
        msg2.obj = false;
        this.mHandler.sendMessage(msg2);
    }

    private boolean isColdStartApp(int pid) {
        ProcessRecord proc = this.mPms.getProcessRecordByPid(pid);
        if (proc.mState.getLastTopTime() == 0) {
            return true;
        }
        return false;
    }

    public void uploadLmkdOomForGame(boolean isEnable) {
        try {
            this.mMethodUpdateOomForGame.invoke(this.mAms, Boolean.valueOf(isEnable));
        } catch (Exception e) {
            Slog.w(TAG, "game oom update error!");
        }
    }

    /* loaded from: classes.dex */
    public class H extends Handler {
        static final int MSG_MEMORY_CLEAN_FOR_GAME = 1;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public H(Looper looper) {
            super(looper);
            ProcessGameCleaner.this = r1;
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1:
                    ProcessGameCleaner.this.uploadLmkdOomForGame(((Boolean) msg.obj).booleanValue());
                    return;
                default:
                    return;
            }
        }
    }
}
