package com.miui.server.rtboost;

import android.content.Context;
import android.os.Binder;
import android.os.Bundle;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.MiuiProcess;
import android.os.Process;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.ShellCallback;
import android.os.ShellCommand;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Trace;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Log;
import android.util.Slog;
import com.android.internal.util.DumpUtils;
import com.android.server.LocalServices;
import com.android.server.am.ActivityManagerService;
import com.android.server.am.SystemPressureController;
import com.android.server.display.TemperatureController;
import com.android.server.wm.RealTimeModeControllerImpl;
import com.android.server.wm.RealTimeModeControllerStub;
import com.android.server.wm.SchedBoostGesturesEvent;
import com.android.server.wm.WindowManagerService;
import com.android.server.wm.WindowProcessListener;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Objects;
import miui.util.FeatureParser;
import miui.util.ReflectionUtils;
/* loaded from: classes.dex */
public class SchedBoostService extends Binder implements SchedBoostManagerInternal {
    private static int[] ALL_CPU_CORES = null;
    private static final String BOOST_METHOD_NAME = "perfLockAcquire";
    private static final String BOOST_STOP_NAME = "perfLockRelease";
    public static final boolean DEBUG = RealTimeModeControllerImpl.DEBUG;
    private static final int DEFAULT_UCLAMP_MAX = 1024;
    private static final int DEFAULT_UCLAMP_MIN = 0;
    private static final int DEFAULT_UCLAMP_UPPER = 614;
    private static final boolean ENABLE_RTMODE_UCLAMP;
    private static final boolean IS_MTK_DEVICE;
    public static final boolean IS_SERVICE_ENABLED = true;
    public static final boolean IS_TEMP_LIMIT_ENABLED;
    public static final boolean IS_UCLAMP_ENABLED;
    public static final boolean IS_UIGROUP_ENABLED;
    public static final String SERVICE_NAME = "SchedBoostService";
    public static final String TAG = "SchedBoost";
    public static final int THREAD_GROUP_UI = 10;
    private static final String THREAD_NAME = "SchedBoostServiceTh";
    private static final int THREAD_PRIORITY_HIGHEST = -20;
    private static boolean isInited;
    private static boolean isNormalPolicy;
    private static Class<?> mBoostClass;
    private static Method mStartBoost;
    private static Method mStopBoost;
    private Context mContext;
    private Handler mHandler;
    private HandlerThread mHandlerThread;
    private SchedBoostGesturesEvent mSchedBoostGesturesEvent;
    private boolean mSimpleNotNeedSched;
    private WindowManagerService mWMS;
    private int TASK_UCLAMP_MIN = SystemProperties.getInt("persist.sys.speedui_uclamp_min", (int) DEFAULT_UCLAMP_UPPER);
    private int TASK_UCLAMP_MAX = SystemProperties.getInt("persist.sys.speedui_uclamp_max", 1024);
    private Object mPerf = null;
    private int[] mDefultQComPerfList = {1082130432, 1500, 1115701248, 0, 1120010240, 1};
    private int[] mQcomCpuAndGpuBoost = {1077936129, 1, 1086324736, 1, 1082146816, 4095, 1082147072, 4095, 1082130432, 1700, 1082130688, 1700, 1115701248, 0, 1120010240, 1};
    private BoostingPackageMap mBoostingMap = new BoostingPackageMap();
    private final ArraySet<Integer> mAlwaysRtTids = new ArraySet<>();
    private Method mMethodSetAffinity = null;
    private Method mMethodUclampTask = null;
    private String mPreBoostProcessName = null;
    private int mHomeRenderThreadId = 0;

    static {
        boolean z = FeatureParser.getBoolean("is_mediatek", false);
        IS_MTK_DEVICE = z;
        boolean z2 = SystemProperties.getBoolean("persist.sys.enable_rtmode_uclamp", false);
        ENABLE_RTMODE_UCLAMP = z2;
        IS_UCLAMP_ENABLED = z && z2;
        IS_UIGROUP_ENABLED = SystemProperties.getBoolean("persist.sys.enable_setuicgroup", false);
        IS_TEMP_LIMIT_ENABLED = SystemProperties.getBoolean("persist.sys.enable_templimit", false);
        ALL_CPU_CORES = null;
        mBoostClass = null;
        mStartBoost = null;
        mStopBoost = null;
    }

    public SchedBoostService(Context context) {
        this.mContext = context;
        if (MiuiProcess.PROPERTY_CPU_CORE_COUNT <= 0) {
            return;
        }
        ALL_CPU_CORES = new int[MiuiProcess.PROPERTY_CPU_CORE_COUNT];
        for (int i = 0; i < MiuiProcess.PROPERTY_CPU_CORE_COUNT; i++) {
            ALL_CPU_CORES[i] = i;
        }
        this.mWMS = ServiceManager.getService("window");
        if (RealTimeModeControllerImpl.ENABLE_RT_MODE && init()) {
            HandlerThread handlerThread = new HandlerThread(THREAD_NAME) { // from class: com.miui.server.rtboost.SchedBoostService.1
                @Override // android.os.HandlerThread
                protected void onLooperPrepared() {
                    super.onLooperPrepared();
                    int tid = getThreadId();
                    ActivityManagerService.scheduleAsFifoPriority(tid, true);
                }
            };
            this.mHandlerThread = handlerThread;
            handlerThread.start();
            this.mHandler = new Handler(this.mHandlerThread.getLooper());
            LocalServices.addService(SchedBoostManagerInternal.class, new LocalService());
            RealTimeModeControllerStub.get().init(this.mContext);
            RealTimeModeControllerStub.get().setWindowManager(this.mWMS);
            SchedBoostGesturesEvent schedBoostGesturesEvent = new SchedBoostGesturesEvent(this.mHandlerThread.getLooper());
            this.mSchedBoostGesturesEvent = schedBoostGesturesEvent;
            schedBoostGesturesEvent.init(context);
            this.mSchedBoostGesturesEvent.setGesturesEventListener(new SchedBoostGesturesEvent.GesturesEventListener() { // from class: com.miui.server.rtboost.SchedBoostService.2
                @Override // com.android.server.wm.SchedBoostGesturesEvent.GesturesEventListener
                public void onFling(int durationMs) {
                    RealTimeModeControllerImpl.get().onFling(durationMs);
                }

                @Override // com.android.server.wm.SchedBoostGesturesEvent.GesturesEventListener
                public void onScroll(boolean started) {
                    Trace.traceBegin(4L, "onScroll");
                    RealTimeModeControllerImpl.get().onScroll(started);
                    Trace.traceEnd(4L);
                }

                @Override // com.android.server.wm.SchedBoostGesturesEvent.GesturesEventListener
                public void onDown() {
                    Trace.traceBegin(4L, "onDown");
                    RealTimeModeControllerImpl.get().onDown();
                    Trace.traceEnd(4L);
                }

                @Override // com.android.server.wm.SchedBoostGesturesEvent.GesturesEventListener
                public void onMove() {
                    Trace.traceBegin(4L, "onMove");
                    Trace.traceEnd(4L);
                }
            });
            SystemPressureController.getInstance().registerThermalTempListener(new SystemPressureController.ThermalTempListener() { // from class: com.miui.server.rtboost.SchedBoostService.3
                @Override // com.android.server.am.SystemPressureController.ThermalTempListener
                public void onThermalTempChange(int temp) {
                    SchedBoostService.this.changeSchedBoostPolicy(temp);
                }
            });
        }
    }

    private boolean init() {
        Method tryFindMethodBestMatch = ReflectionUtils.tryFindMethodBestMatch(Process.class, "setSchedAffinity", new Class[]{Integer.TYPE, int[].class});
        this.mMethodSetAffinity = tryFindMethodBestMatch;
        if (tryFindMethodBestMatch == null) {
            return false;
        }
        this.mMethodUclampTask = ReflectionUtils.tryFindMethodBestMatch(Process.class, "setTaskUclamp", new Class[]{Integer.TYPE, Integer.TYPE, Integer.TYPE});
        synchronized (SchedBoostService.class) {
            if (!isInited) {
                try {
                    Class<?> cls = Class.forName("android.util.BoostFramework");
                    mBoostClass = cls;
                    if (cls != null) {
                        mStartBoost = cls.getMethod(BOOST_METHOD_NAME, Integer.TYPE, int[].class);
                        mStopBoost = mBoostClass.getMethod(BOOST_STOP_NAME, new Class[0]);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                isInited = true;
            }
        }
        try {
            Class<?> cls2 = mBoostClass;
            if (cls2 != null) {
                Constructor constructor = cls2.getConstructor(new Class[0]);
                constructor.setAccessible(true);
                this.mPerf = constructor.newInstance(new Object[0]);
            }
        } catch (Exception e2) {
            e2.printStackTrace();
        }
        return true;
    }

    @Override // com.miui.server.rtboost.SchedBoostManagerInternal
    public void schedProcessBoost(WindowProcessListener proc, String procName, int pid, int rtid, int schedMode, long timeout) {
    }

    @Override // com.miui.server.rtboost.SchedBoostManagerInternal
    public void setHomeRenderThreadTid(int tid) {
        this.mHomeRenderThreadId = tid;
    }

    @Override // com.miui.server.rtboost.SchedBoostManagerInternal
    public void enableSchedBoost(boolean enable) {
        Slog.w(TAG, Binder.getCallingPid() + " set sched boost enable:" + enable);
        this.mSimpleNotNeedSched = !enable;
    }

    @Override // com.miui.server.rtboost.SchedBoostManagerInternal
    public void beginSchedThreads(int[] tids, long duration, String procName, int mode) {
        if (DEBUG) {
            Slog.d(TAG, "beginSchedThreads called, mode: " + mode + ", procName :" + procName);
        }
        Handler handler = this.mHandler;
        if (handler == null) {
            return;
        }
        if (mode == 3) {
            handler.obtainSetThreadsAlwaysFIFOMsg(tids).sendToTarget();
        } else if (duration > 0 && duration <= 6000) {
            handler.obtainBeginSchedThreadsMsg(tids, duration, procName, mode).sendToTarget();
        }
    }

    public void handleBeginSchedThread(int[] tids, long duration, String procName, int mode) {
        String str = this.mPreBoostProcessName;
        if (str != null && !TextUtils.equals(str, procName)) {
            ArrayList<BoostThreadInfo> list = this.mBoostingMap.getList();
            if (list != null && list.size() > 0) {
                for (int i = 0; i < list.size(); i++) {
                    BoostThreadInfo threadInfo = list.get(i);
                    if (threadInfo.mode == 4 && mode < threadInfo.mode) {
                        return;
                    }
                    this.mHandler.removeResetThreadsMsg(threadInfo);
                    this.mHandler.obtainResetSchedThreadsMsg(threadInfo).sendToTarget();
                }
            }
            this.mBoostingMap.clear();
        }
        this.mPreBoostProcessName = procName;
        ArrayList<BoostThreadInfo> curList = getCurrentBoostThreadsList(tids, duration, procName, mode);
        if (curList != null) {
            int curListLength = curList.size();
            for (int i2 = 0; i2 < curListLength; i2++) {
                this.mHandler.obtainSchedThreadsMsg(curList.get(i2)).sendToTarget();
            }
        }
    }

    public void handleSchedThread(BoostThreadInfo threadInfo) {
        if (threadInfo == null) {
            Slog.e(TAG, String.format("handleSchedThreads err: threadInfo null object", new Object[0]));
            return;
        }
        this.mHandler.removeResetThreadsMsg(threadInfo);
        Message resetMsg = this.mHandler.obtainResetSchedThreadsMsg(threadInfo);
        this.mHandler.sendMessageDelayed(resetMsg, threadInfo.duration);
        if (threadInfo.duration == 0) {
            return;
        }
        if (threadInfo.mode == 1 || threadInfo.mode == 5 || threadInfo.mode == 4) {
            startBoostInternal((int) threadInfo.duration, this.mDefultQComPerfList);
        } else if (threadInfo.mode == 6) {
            startBoostInternal((int) threadInfo.duration, this.mQcomCpuAndGpuBoost);
        }
        int tid = threadInfo.tid;
        long boostStartTime = threadInfo.boostStartTime;
        if (boostStartTime != 0) {
            if (DEBUG) {
                Slog.d(TAG, String.format("handleSchedThreads continue: threads: %s", Integer.valueOf(tid)));
                return;
            }
            return;
        }
        threadInfo.boostStartTime = SystemClock.uptimeMillis();
        threadInfo.savedPriority = MiuiProcess.getThreadPriority(threadInfo.tid, TAG);
        boolean forkPriority = false;
        int[] coresIndex = MiuiProcess.BIG_CORES_INDEX;
        if (this.mHomeRenderThreadId == threadInfo.tid) {
            forkPriority = true;
        }
        if (MiuiProcess.BIG_PRIME_CORES_INDEX != null && threadInfo.mode == 0) {
            coresIndex = MiuiProcess.BIG_PRIME_CORES_INDEX;
        }
        setAffinityAndPriority(threadInfo, true, forkPriority, coresIndex);
        setTaskUclamp(tid, this.TASK_UCLAMP_MIN, this.TASK_UCLAMP_MAX);
        setTaskGroup(tid, 10);
        threadInfo.boostPriority = MiuiProcess.getThreadPriority(threadInfo.tid, TAG);
        if (DEBUG) {
            Slog.d(TAG, String.format("handleSchedThreads begin: threads: %s, procName: %s, mode: %s, aff:%s", Integer.valueOf(tid), threadInfo.procName, Integer.valueOf(threadInfo.mode), Arrays.toString(coresIndex)));
        }
        Trace.asyncTraceBegin(64L, "SchedBoost: " + threadInfo.procName, threadInfo.tid);
    }

    /* JADX WARN: Code restructure failed: missing block: B:37:0x00bc, code lost:
        android.util.Slog.d(com.miui.server.rtboost.SchedBoostService.TAG, "reset tid " + r2 + " group to foreground");
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public void handleResetSchedThread(com.miui.server.rtboost.SchedBoostService.BoostThreadInfo r15) {
        /*
            Method dump skipped, instructions count: 339
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.miui.server.rtboost.SchedBoostService.handleResetSchedThread(com.miui.server.rtboost.SchedBoostService$BoostThreadInfo):void");
    }

    public void handleSetThreadsAlwaysFIFO(int[] tids) {
        for (int tid : tids) {
            if (tid > 0) {
                if (DEBUG) {
                    Slog.d(TAG, String.format("handleSetThreadsAlwaysFIFO, threads: %s", Integer.valueOf(tid)));
                }
                ActivityManagerService.scheduleAsFifoPriority(tid, true);
                this.mAlwaysRtTids.add(Integer.valueOf(tid));
            }
        }
    }

    /* loaded from: classes.dex */
    public final class Handler extends android.os.Handler {
        private final int MSG_SCHED_THREADS = 1;
        private final int MSG_RESET_SCHED_THREADS = 2;
        private final int MSG_BEGIN_SCHED_THREADS = 3;
        private final int MSG_SET_THREAD_ALWAYS_FIFO = 4;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public Handler(Looper looper) {
            super(looper);
            SchedBoostService.this = r1;
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1:
                    BoostThreadInfo threadInfo = (BoostThreadInfo) msg.obj;
                    SchedBoostService.this.handleSchedThread(threadInfo);
                    return;
                case 2:
                    BoostThreadInfo threadInfo2 = (BoostThreadInfo) msg.obj;
                    SchedBoostService.this.handleResetSchedThread(threadInfo2);
                    return;
                case 3:
                    Bundle bundle = msg.getData();
                    String procName = bundle.getString(TemperatureController.STRATEGY_NAME);
                    int[] tids = bundle.getIntArray("tids");
                    int mode = bundle.getInt("mode");
                    long duration = bundle.getLong("timeout");
                    Trace.traceBegin(64L, "beginSchedThread, mode: " + mode);
                    SchedBoostService.this.handleBeginSchedThread(tids, duration, procName, mode);
                    Trace.traceEnd(64L);
                    return;
                case 4:
                    int[] tids2 = msg.getData().getIntArray("tids");
                    if (tids2 != null && tids2.length > 0) {
                        SchedBoostService.this.handleSetThreadsAlwaysFIFO(tids2);
                        return;
                    }
                    return;
                default:
                    return;
            }
        }

        public Message obtainSchedThreadsMsg(BoostThreadInfo threadInfo) {
            Objects.requireNonNull(this);
            Message msg = obtainMessage(1, threadInfo);
            msg.obj = threadInfo;
            return msg;
        }

        public Message obtainResetSchedThreadsMsg(BoostThreadInfo threadInfo) {
            Objects.requireNonNull(this);
            Message msg = obtainMessage(2, threadInfo);
            msg.obj = threadInfo;
            return msg;
        }

        public Message obtainBeginSchedThreadsMsg(int[] tids, long duration, String procName, int mode) {
            Objects.requireNonNull(this);
            Message msg = obtainMessage(3);
            Bundle data = new Bundle();
            data.putString(TemperatureController.STRATEGY_NAME, procName);
            data.putIntArray("tids", tids);
            data.putInt("mode", mode);
            data.putLong("timeout", duration);
            msg.setData(data);
            return msg;
        }

        public Message obtainSetThreadsAlwaysFIFOMsg(int[] tids) {
            Objects.requireNonNull(this);
            Message msg = obtainMessage(4);
            Bundle data = new Bundle();
            data.putIntArray("tids", tids);
            msg.setData(data);
            return msg;
        }

        public void removeResetThreadsMsg(BoostThreadInfo threadInfo) {
            Objects.requireNonNull(this);
            removeEqualMessages(2, threadInfo);
        }
    }

    private ArrayList<BoostThreadInfo> getCurrentBoostThreadsList(int[] tids, long duration, String procName, int mode) {
        ArrayList<BoostThreadInfo> curList = new ArrayList<>();
        for (int tid : tids) {
            if (this.mAlwaysRtTids.contains(Integer.valueOf(tid))) {
                if (DEBUG) {
                    Slog.d(TAG, "already rttids:" + this.mAlwaysRtTids.toString() + ",tid :" + tid);
                }
            } else {
                BoostThreadInfo threadInfo = new BoostThreadInfo(tid, duration, procName, mode);
                this.mBoostingMap.put(threadInfo);
                curList.add(threadInfo);
            }
        }
        return curList;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class LocalService implements SchedBoostManagerInternal {
        private LocalService() {
            SchedBoostService.this = r1;
        }

        @Override // com.miui.server.rtboost.SchedBoostManagerInternal
        public void schedProcessBoost(WindowProcessListener proc, String procName, int pid, int rtid, int schedMode, long timeout) {
            SchedBoostService.this.schedProcessBoost(proc, procName, pid, rtid, schedMode, timeout);
        }

        @Override // com.miui.server.rtboost.SchedBoostManagerInternal
        public void enableSchedBoost(boolean enable) {
            SchedBoostService.this.enableSchedBoost(enable);
        }

        @Override // com.miui.server.rtboost.SchedBoostManagerInternal
        public void beginSchedThreads(int[] tids, long duration, String procName, int mode) {
            SchedBoostService.this.beginSchedThreads(tids, duration, procName, mode);
        }

        @Override // com.miui.server.rtboost.SchedBoostManagerInternal
        public void setHomeRenderThreadTid(int tid) {
            SchedBoostService.this.setHomeRenderThreadTid(tid);
        }

        @Override // com.miui.server.rtboost.SchedBoostManagerInternal
        public boolean checkThreadBoost(int tid) {
            return SchedBoostService.this.checkThreadBoost(tid);
        }

        @Override // com.miui.server.rtboost.SchedBoostManagerInternal
        public void setThreadSavedPriority(int[] tid, int prio) {
            SchedBoostService.this.setThreadSavedPriority(tid, prio);
        }
    }

    /* loaded from: classes.dex */
    public class BoostThreadInfo {
        private static final int DEFAULT_SAVED_PRIO = 0;
        private static final int DEFAULT_START_TIME = 0;
        int boostPriority;
        long boostStartTime;
        long duration;
        int mode;
        String procName;
        int savedPriority;
        int tid;

        public BoostThreadInfo(int tid, long duration, String procName, int mode) {
            SchedBoostService.this = r1;
            this.tid = tid;
            this.duration = duration;
            this.procName = procName;
            this.mode = mode;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof BoostThreadInfo)) {
                return false;
            }
            BoostThreadInfo threadInfo = (BoostThreadInfo) obj;
            return threadInfo.tid == this.tid;
        }

        public void dump(PrintWriter pw, String[] args) {
            pw.println("#" + this.procName + " tid:" + this.tid + " duration: " + this.duration + " boostStartTime: " + this.boostStartTime + " mode: " + this.mode + " savedPriority: " + this.savedPriority + " boostPriority: " + this.boostPriority);
        }
    }

    /* loaded from: classes.dex */
    public class BoostingPackageMap {
        private final ArrayList<BoostThreadInfo> infoList;
        private final HashMap<Integer, BoostThreadInfo> mMap;

        private BoostingPackageMap() {
            SchedBoostService.this = r1;
            this.mMap = new HashMap<>();
            this.infoList = new ArrayList<>();
        }

        public ArrayList<BoostThreadInfo> getList() {
            return this.infoList;
        }

        public void put(BoostThreadInfo threadInfo) {
            synchronized (this.mMap) {
                if (this.infoList.contains(threadInfo)) {
                    BoostThreadInfo oldThreadInfo = this.mMap.get(Integer.valueOf(threadInfo.tid));
                    threadInfo.boostStartTime = oldThreadInfo.boostStartTime;
                    threadInfo.savedPriority = oldThreadInfo.savedPriority;
                    threadInfo.boostPriority = oldThreadInfo.boostPriority;
                    removeByTid(oldThreadInfo.tid);
                }
                this.infoList.add(threadInfo);
                this.mMap.put(Integer.valueOf(threadInfo.tid), threadInfo);
            }
        }

        public void removeByTid(int tid) {
            synchronized (this.mMap) {
                BoostThreadInfo threadInfo = this.mMap.remove(Integer.valueOf(tid));
                if (threadInfo != null) {
                    this.infoList.remove(threadInfo);
                }
            }
        }

        public void clear() {
            synchronized (this.mMap) {
                this.mMap.clear();
                this.infoList.clear();
            }
        }

        public BoostThreadInfo getByTid(int tid) {
            BoostThreadInfo boostThreadInfo;
            synchronized (this.mMap) {
                boostThreadInfo = this.mMap.get(Integer.valueOf(tid));
            }
            return boostThreadInfo;
        }
    }

    private void setAffinityAndPriority(BoostThreadInfo threadInfo, boolean isFifo, boolean forkPriority, int[] coresIndex) {
        int tid = threadInfo.tid;
        boolean setFifo = false;
        if (isFifo) {
            if (isNormalPolicy) {
                setFifo = MiuiProcess.setThreadPriority(tid, (int) THREAD_PRIORITY_HIGHEST, TAG);
            } else if (forkPriority) {
                setFifo = ActivityManagerService.scheduleAsFifoAndForkPriority(tid, true);
            } else {
                setFifo = ActivityManagerService.scheduleAsFifoPriority(tid, true);
            }
        } else {
            int curPrio = MiuiProcess.getThreadPriority(threadInfo.tid, TAG);
            if (!isNormalPolicy || curPrio == threadInfo.boostPriority) {
                setFifo = ActivityManagerService.scheduleAsRegularPriority(tid, true);
                MiuiProcess.setThreadPriority(tid, threadInfo.savedPriority, TAG);
            }
        }
        if (setFifo) {
            setSchedAffinity(tid, coresIndex);
        }
        if (DEBUG) {
            Slog.d(TAG, String.format("setAffinityAndPriority, tid: %s, isFifo: %s, forkPriority: %s, coresIndex: %s", Integer.valueOf(tid), Boolean.valueOf(isFifo), Boolean.valueOf(forkPriority), Arrays.toString(coresIndex)));
        }
    }

    private void setSchedAffinity(int pid, int[] cores) {
        Method method = this.mMethodSetAffinity;
        if (method != null) {
            try {
                method.invoke(null, Integer.valueOf(pid), cores);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void setTaskUclamp(int tid, int perfIdx, int maxIdx) {
        Method method;
        if (IS_UCLAMP_ENABLED && (method = this.mMethodUclampTask) != null) {
            try {
                method.invoke(null, Integer.valueOf(tid), Integer.valueOf(perfIdx), Integer.valueOf(maxIdx));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void setTaskGroup(int tid, int group) {
        if (IS_UIGROUP_ENABLED) {
            Slog.i(TAG, "setTaskGroup: " + tid + ", " + group);
            try {
                Process.setThreadGroupAndCpuset(tid, group);
                Slog.i(TAG, String.format("setTaskGroup: Check cpuset of tid: %s,group= %s", Integer.valueOf(tid), Integer.valueOf(Process.getCpusetThreadGroup(tid))));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private boolean startBoostInternal(int duration, int[] params) {
        Log.d(TAG, "startBoostInternal " + duration);
        if (this.mPerf == null) {
            Log.d(TAG, "not init boost pref");
            return false;
        }
        try {
            if (mStartBoost != null) {
                Log.d(TAG, "ready to boost");
                mStartBoost.setAccessible(true);
                mStartBoost.invoke(this.mPerf, Integer.valueOf(duration), params);
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "start boost exception " + e);
            e.printStackTrace();
        }
        return false;
    }

    private void stopBoostInternal() {
        Log.d(TAG, "stopBoostInternal:");
        if (this.mPerf == null) {
            Log.d(TAG, "not init boost pref, not need to stop");
            return;
        }
        try {
            if (mStopBoost != null) {
                Log.d(TAG, "ready to stop boost");
                mStopBoost.setAccessible(true);
                mStopBoost.invoke(this.mPerf, new Object[0]);
            }
        } catch (Exception e) {
            Log.e(TAG, "stop boost exception " + e);
            e.printStackTrace();
        }
    }

    public void changeSchedBoostPolicy(int temp) {
        if (!RealTimeModeControllerImpl.ENABLE_RT_MODE || !IS_TEMP_LIMIT_ENABLED) {
            return;
        }
        if (temp >= 42 && !isNormalPolicy) {
            isNormalPolicy = true;
            Slog.d(TAG, String.format("onThermalTempChange, set sched policy OTHER", new Object[0]));
        } else if (temp <= 39 && isNormalPolicy) {
            isNormalPolicy = false;
            Slog.d(TAG, String.format("onThermalTempChange, set sched policy FIFO", new Object[0]));
        }
    }

    @Override // com.miui.server.rtboost.SchedBoostManagerInternal
    public boolean checkThreadBoost(int tid) {
        if (this.mBoostingMap.getByTid(tid) != null) {
            return true;
        }
        return false;
    }

    @Override // com.miui.server.rtboost.SchedBoostManagerInternal
    public void setThreadSavedPriority(int[] tid, int prio) {
        if (tid.length > 0) {
            for (int threadTid : tid) {
                BoostThreadInfo info = this.mBoostingMap.getByTid(threadTid);
                if (info != null) {
                    if (DEBUG) {
                        Log.d(TAG, "setThreadSavedPriority tid: " + threadTid + ", before: " + info.savedPriority + ", after: " + prio);
                    }
                    info.savedPriority = prio;
                }
            }
        }
    }

    private void dump(PrintWriter pw, String[] args) {
        StringBuilder append = new StringBuilder().append("IS_MTK_DEVICE: ");
        boolean z = IS_MTK_DEVICE;
        pw.println(append.append(z).toString());
        if (z) {
            pw.println("ENABLE_RTMODE_UCLAMP: " + ENABLE_RTMODE_UCLAMP);
            pw.println("TASK_UCLAMP_MIN: " + this.TASK_UCLAMP_MIN);
            pw.println("TASK_UCLAMP_MIN: " + this.TASK_UCLAMP_MAX);
        }
        pw.println("mPreBoostProcessName: " + this.mPreBoostProcessName);
        pw.println("currently isNormalPolicy: " + isNormalPolicy);
        pw.println("AlwaysRtTids: ");
        synchronized (this.mAlwaysRtTids) {
            Iterator<Integer> it = this.mAlwaysRtTids.iterator();
            while (it.hasNext()) {
                int tid = it.next().intValue();
                pw.println("    " + tid);
            }
        }
        pw.println("Boosting Threads: ");
        synchronized (this.mBoostingMap) {
            Iterator it2 = this.mBoostingMap.infoList.iterator();
            while (it2.hasNext()) {
                BoostThreadInfo info = (BoostThreadInfo) it2.next();
                info.dump(pw, args);
            }
        }
    }

    @Override // android.os.Binder
    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (!DumpUtils.checkDumpPermission(this.mContext, TAG, pw)) {
            return;
        }
        pw.println("sched boost (SchedBoostService):");
        try {
            RealTimeModeControllerImpl.get();
            RealTimeModeControllerImpl.dump(pw, args);
            dump(pw, args);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) throws RemoteException {
        new SchedBoostShellCommand(this).exec(this, in, out, err, args, callback, resultReceiver);
    }

    /* loaded from: classes.dex */
    static class SchedBoostShellCommand extends ShellCommand {
        SchedBoostService mService;

        SchedBoostShellCommand(SchedBoostService service) {
            this.mService = service;
        }

        public int onCommand(String cmd) {
            FileDescriptor fd = getOutFileDescriptor();
            PrintWriter pw = getOutPrintWriter();
            String[] args = getAllArgs();
            if (cmd == null) {
                return handleDefaultCommands(cmd);
            }
            try {
                this.mService.dump(fd, pw, args);
                return -1;
            } catch (Exception e) {
                pw.println(e);
                return -1;
            }
        }

        public void onHelp() {
            PrintWriter pw = getOutPrintWriter();
            pw.println("sched boost (SchedBoostService) commands:");
            pw.println();
        }
    }
}
