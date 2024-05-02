package com.android.server.am;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Trace;
import android.server.am.SplitScreenReporter;
import android.text.TextUtils;
import android.util.BoostFramework;
import android.util.Slog;
import android.util.SparseArray;
import com.android.internal.os.SomeArgs;
import com.android.internal.util.MemInfoReader;
import com.android.server.LocalServices;
import com.android.server.MiuiBatteryStatsService;
import com.android.server.ServiceThread;
import com.android.server.am.AppStateManager;
import com.android.server.am.CameraBoostAndMemoryTrackManager;
import com.android.server.camera.CameraJsonParser;
import com.android.server.pm.BackgroundDexOptService;
import com.miui.server.stability.ScoutDisplayMemoryManager;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import miui.os.Build;
import org.json.JSONException;
import org.json.JSONObject;
/* loaded from: classes.dex */
public class CameraBooster {
    private static final int BOOST_CAMERA = 2;
    private static final String CAM3RD_BOOST_ENABLE = "3rdcam_boost_enable";
    private static final String CAM3RD_BOOST_THRESHOLD = "3rdcam_boost_threshold";
    private static final double CAMERA_BOOST_THRESHOLD_PERCENT = 0.25d;
    private static final String CAM_BOOST_EARLY_ENABLE = "cam_boost_early_enable";
    private static final String CAM_BOOST_ENABLE = "cam_boost_enable";
    private static final String CAM_BOOST_ENABLE_COMPACT = "mms_camcpt_enable";
    private static final String CAM_BOOST_EXT_MEM = "cam_boost_ext_mem";
    private static final String CAM_BOOST_OPT_ENABLE = "cam_boost_opt_enable";
    private static final String CAM_BOOST_RECLAIM_THRESHOLD = "cam_boost_reclaim";
    private static final String CAM_BOOST_THRESHOLD = "cam_boost_threshold";
    private static final String CAM_RECLAIM_ENABLE = "cam_reclaim_enable";
    private static final String CAM_RECORD_RECLAIM_THRESHOLD = "cam_record_reclaim";
    static final byte CLEAR_BLACK_LIST = 3;
    static final byte CLEAR_WHITE_LIST = 2;
    private static final String ENABLE_ADJ_SWAP_FREE_PERCENT = "adj_swap_support";
    private static final String INHIBIT_3RD_SUPPORT = "inhibit_3rdprocs_enable";
    private static final String INHIBIT_APP_LIST = "inhibit_app_list";
    private static final String INHIBIT_NATIVE_LIST = "inhibit_native_list";
    private static final String INHIBIT_SUPPORT = "inhibit_procs_enable";
    private static final String INHIBIT_WHITE_LIST = "inhibit_white_list";
    private static final String INTERCEPT_MS = "intercept_restart_time";
    private static final String INTERCEPT_RESTART_LIST = "intercept_restart_list";
    private static final String KILL_3RDLOWER_ADJ_THRESHOLD = "3rd_lowAdj_threshold";
    private static final String KILL_ADJ_THRESHOLD = "adj_threshold";
    private static final String KILL_BLACK_LIST = "black_list";
    private static final String KILL_DURATION = "kill_start_duration";
    private static final String KILL_FORCESTOP_ENABLE = "cam_boost_forcestop_enable";
    private static final String KILL_HIGHPRIO_SYSAPP_THRESHOLD = "kill_highprio_sysapp_threshlod";
    private static final int KILL_HOME_PROCESS = 3;
    private static final String KILL_LOWERADJ_FREE_THRESHOLD = "lowerAdj_freeMem_threshold";
    private static final String KILL_LOWER_ADJ_THRESHOLD = "lowAdj_threshold";
    private static final String KILL_LOWPRIO_SYSAPP_THRESHOLD = "kill_lowprio_sysapp_threshlod";
    private static final String KILL_PERCEPTIBLE_LIST = "perceptible_list";
    private static final String KILL_PROTECT_LIST = "protect_list";
    private static final String KILL_SKIP_TASK = "skip_task";
    private static final String KILL_SKIP_TASK_LOWER = "skip_task_lower";
    private static final String KILL_START_BLACK_LIST = "start_black_list";
    private static final String KILL_TAG = "kill_tag";
    private static final String KILL_WHITE_LIST = "white_list";
    private static final String LMKD_PERCEPTIBLE_SUPPORT = "lmkd_perceptible_support";
    static final byte LMK_ADJ_SWAP_PERCENT = 23;
    static final byte LMK_CAMERA_MODE = 24;
    static final byte LMK_FORCE_CHECK_LIST = 20;
    static final byte LMK_RECLAIM_FOR_CAMERA = 25;
    static final byte LMK_SET_BLACKLIST = 22;
    static final byte LMK_SET_PERCEPTIBLE_LIST = 27;
    static final byte LMK_SET_PROTECT_LIST = 26;
    static final byte LMK_SET_PSI_LEVEL = 28;
    static final byte LMK_SET_WHITELIST = 21;
    static final byte LMK_TARGET = 0;
    private static final int MODE_CAMERA_OPEN = 159;
    private static final int MODE_RECORD_VIDEO = 162;
    private static final String ONESHOT_CAMERA_SCENE = "com.android.camera/.OneShotCamera";
    private static final String OOM_LEVEL = "oom_level";
    private static final String PACKAGE_NAME_CAMERA = "com.android.camera";
    private static final String PACKAGE_NAME_HOME = "com.miui.home";
    static final byte PACKET_MAX_SIZE = 52;
    private static final String PERCEPTIBLE_SUPPORT = "perceptible_support";
    private static final String PSI_LEVEL = "psi_level";
    private static final int RECENT_TASK_CNT = 20;
    private static final String SKIP_TASK_LIST = "skip_task_list";
    static final byte START_CHECK_LIST = 0;
    static final byte STOP_CHECK_LIST = 1;
    private static final String SUPPORT_OOM_UPDATE = "oom_update_support";
    private static final String TAG = "CameraBooster";
    private static final String TRIM_MEM_ENABLE = "trim_memory_support";
    private static final int TYPE_BLACK_KILL = 2;
    private static final int TYPE_CAMERA_LAUNCH_BOOST = 201;
    private static final int TYPE_KILL = 0;
    private static final int TYPE_PERCEPTIBLE_KILL = 8;
    private static final int TYPE_PROTECT_KILL = 4;
    private static final int TYPE_SYS_KILL = 1;
    private static final int TYPE_WHITE_KILL = 16;
    private static final int UPDATE_CAMERA_STATE = 1;
    private static final String UPDATE_STATE_DELAY_MS = "update_state_delay_ms";
    private ActivityManagerService mActivityManagerService;
    private CameraAffinityController mCameraAffinityController;
    private CameraBoostAndMemoryTrackManager mCameraBoostAndMemoryTrackManager;
    private ServiceThread mCameraThread;
    private Context mContext;
    private CameraBoostHandler mHandler;
    private boolean mIsCameraForeground;
    private long mKillStartTime;
    private ProcessManagerService mProcessManagerService;
    private OutputStream sLmkdOutputStream;
    private LocalSocket sLmkdSocket;
    private long startTime;
    private static final String DEVICE = Build.DEVICE.toLowerCase();
    private static final boolean CAM_BOOST_DEBUG = SystemProperties.getBoolean("persist.sys.miui.camera.boost.debug", false);
    private static final String DEFAULT_OOM_LEVEL = SystemProperties.get("sys.lmk.minfree_levels", "");
    private Map<Integer, Integer> mProcessCpuSetMap = new ConcurrentHashMap();
    private Map<String, Long> mCpuConsume = new ConcurrentHashMap();
    private final Object sLock = new Object();
    private SmartPowerService mSmartPowerService = SmartPowerService.getInstance();
    private CameraJsonParser mCameraParser = new CameraJsonParser();
    private BoostFramework mPerfBoost = new BoostFramework();

    /* loaded from: classes.dex */
    public class CameraBoostHandler extends Handler {
        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public CameraBoostHandler(Looper looper) {
            super(looper, null, true);
            CameraBooster.this = this$0;
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    SomeArgs args = (SomeArgs) msg.obj;
                    boolean isForeground = ((Boolean) args.arg1).booleanValue();
                    boolean isMiuiCamera = ((Boolean) args.arg2).booleanValue();
                    if (isForeground) {
                        CameraBooster.this.boostCameraIfNeeded(0L, isMiuiCamera);
                    }
                    CameraBooster.this.updateCameraForegroundState(isForeground);
                    CameraBooster.this.runCamReqCompaction(isForeground);
                    return;
                case 2:
                    SomeArgs sa = (SomeArgs) msg.obj;
                    boolean isMiui = ((Boolean) sa.arg2).booleanValue();
                    long memThreshold = ((Long) sa.arg3).longValue();
                    CameraBooster.this.boostCameraIfNeeded(memThreshold, isMiui);
                    return;
                case 3:
                    CameraBooster.this.killProcess((ProcessRecord) msg.obj);
                    return;
                default:
                    return;
            }
        }
    }

    public CameraBooster(ProcessManagerService processManagerService, ActivityManagerService ams, ServiceThread thread, Context context) {
        this.mProcessManagerService = processManagerService;
        this.mActivityManagerService = ams;
        this.mContext = context;
        ServiceThread serviceThread = new ServiceThread(TAG, 0, false);
        this.mCameraThread = serviceThread;
        serviceThread.start();
        this.mHandler = new CameraBoostHandler(this.mCameraThread.getLooper());
        this.mCameraAffinityController = new CameraAffinityController(thread);
    }

    private void doOtherTasks(final boolean isCameraForeground, final String caller) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.am.CameraBooster$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                CameraBooster.this.m358lambda$doOtherTasks$0$comandroidserveramCameraBooster(isCameraForeground, caller);
            }
        });
    }

    /* renamed from: lambda$doOtherTasks$0$com-android-server-am-CameraBooster */
    public /* synthetic */ void m358lambda$doOtherTasks$0$comandroidserveramCameraBooster(boolean isCameraForeground, String caller) {
        this.mCameraAffinityController.notifySetProcsAffinity(isCameraForeground);
        if (isCameraForeground) {
            BackgroundDexOptService.pauseBgDexOpt("camera foreground");
        } else {
            BackgroundDexOptService.resumeBgDexOpt("camera foreground");
        }
        dumpProcessCpuStat(isCameraForeground);
        updateInhibitProcessGroup(isCameraForeground, caller);
    }

    private void updateState(int what, boolean isCameraForeground, boolean isMiuiCamera, long delayms, long memThreshold) {
        this.mIsCameraForeground = isCameraForeground;
        SomeArgs args = SomeArgs.obtain();
        args.arg1 = Boolean.valueOf(isCameraForeground);
        args.arg2 = Boolean.valueOf(isMiuiCamera);
        args.arg3 = Long.valueOf(memThreshold);
        Message msg = this.mHandler.obtainMessage(what, args);
        this.mHandler.removeMessages(what);
        this.mHandler.sendMessageDelayed(msg, delayms);
    }

    public void notifyCameraForegroundState(boolean isCameraForeground, String caller, ComponentName comp) {
        long delayms;
        boolean isMiuiCamera = judgeCameraScene(caller, comp);
        if (!this.mCameraParser.getSupportValue(CAM3RD_BOOST_ENABLE) && !isMiuiCamera) {
            Slog.w(TAG, "not support 3rd camera, caller : " + caller);
            return;
        }
        Slog.i(TAG, "notifyCameraForegroundState isCameraForeground: " + isCameraForeground + ", caller : " + caller);
        if (!isMiuiCamera) {
            if (isCameraForeground) {
                updateState(2, isCameraForeground, isMiuiCamera, 0L, 0L);
                return;
            }
            return;
        }
        if (isCameraForeground) {
            this.startTime = SystemClock.uptimeMillis();
            delayms = 0;
        } else {
            long delayms2 = this.mCameraParser.getThresholdValue(UPDATE_STATE_DELAY_MS, 5000L);
            delayms = delayms2;
        }
        updateState(1, isCameraForeground, isMiuiCamera, delayms, 0L);
        doOtherTasks(isCameraForeground, caller);
    }

    public void boostCameraByThreshold(long memThreshold) {
        if (memThreshold < 0) {
            Slog.w(TAG, "invalid args " + memThreshold);
            return;
        }
        if (this.mCameraParser.getSupportValue(CAM_BOOST_EARLY_ENABLE)) {
            BoostFramework boostFramework = this.mPerfBoost;
            if (boostFramework != null) {
                boostFramework.perfHintAcqRel(-1, 4225, "com.android.camera", -1, (int) TYPE_CAMERA_LAUNCH_BOOST);
            }
        } else if (memThreshold == 0) {
            return;
        }
        updateState(2, true, true, 0L, memThreshold);
    }

    public void updateCameraBoosterCloudData(double version, String jsonStr) {
        this.mCameraParser.updateCameraBoosterCloudData(version, jsonStr);
    }

    public void updateCameraForegroundState(boolean isCameraForeground) {
        updateForegroundState(isCameraForeground);
        ScoutDisplayMemoryManager.getInstance().updateCameraForegroundState(isCameraForeground);
        if (!isCameraForeground) {
            updateCheckListState(1);
            updateCheckListState(2);
        }
        if (this.mCameraParser.getSupportValue(SUPPORT_OOM_UPDATE)) {
            updateOOMLevel(isCameraForeground);
        } else if (this.mCameraParser.getSupportValue(ENABLE_ADJ_SWAP_FREE_PERCENT)) {
            adjSwapPercentage(isCameraForeground);
        }
    }

    private void updatePsiLevel() {
        String psilevel = this.mCameraParser.getOomValue(PSI_LEVEL, "");
        String[] psi = psilevel.split(",");
        if (psi.length == 0) {
            return;
        }
        ByteBuffer buf = ByteBuffer.allocate((psi.length + 1) * 4);
        buf.putInt(28);
        for (String str : psi) {
            try {
                buf.putInt(Integer.parseInt(str));
            } catch (Exception e) {
                Slog.w(TAG, "fail to set psi level " + e);
                return;
            }
        }
        writeLmkd(buf);
    }

    private boolean openLmkdSocket() {
        if (this.sLmkdSocket != null) {
            return true;
        }
        try {
            LocalSocket localSocket = new LocalSocket(3);
            this.sLmkdSocket = localSocket;
            localSocket.connect(new LocalSocketAddress("lmkd", LocalSocketAddress.Namespace.RESERVED));
            this.sLmkdOutputStream = this.sLmkdSocket.getOutputStream();
            updateCheckListState(3);
            updateBlackList();
            updatePsiLevel();
            return true;
        } catch (IOException e) {
            Slog.e(TAG, "lmk socket open failed");
            this.sLmkdSocket = null;
            return false;
        }
    }

    public void writeLmkd(ByteBuffer buf) {
        synchronized (this.sLock) {
            if (this.sLmkdSocket == null && !openLmkdSocket()) {
                Slog.w(TAG, "fail to open socket");
                return;
            }
            try {
                this.sLmkdOutputStream.write(buf.array(), 0, buf.position());
            } catch (IOException e) {
                Slog.w(TAG, "Error writing to lowmemorykiller socket");
                try {
                    this.sLmkdSocket.close();
                } catch (IOException e2) {
                    Slog.e(TAG, "Error close to lowmemorykiller socket");
                }
                this.sLmkdSocket = null;
            }
        }
    }

    private void updateList(String tasks, int type) {
        if (CAM_BOOST_DEBUG) {
            Slog.i(TAG, "updateList: " + tasks + " " + type);
        }
        byte[] data = tasks.getBytes(StandardCharsets.UTF_8);
        if (data.length <= 0 || data.length > 52) {
            return;
        }
        ByteBuffer buf = ByteBuffer.allocate(data.length + 4);
        buf.putInt(type);
        buf.put(data);
        writeLmkd(buf);
    }

    private void updateBlackList() {
        List<String> sCameraLmkdBlackList = this.mCameraParser.getConfigList(KILL_BLACK_LIST);
        if (sCameraLmkdBlackList == null) {
            return;
        }
        for (String proc : sCameraLmkdBlackList) {
            updateList(proc, 22);
        }
    }

    private void updateForegroundState(boolean isCameraForeground) {
        if (CAM_BOOST_DEBUG) {
            Slog.i(TAG, "updateForegroundState: " + isCameraForeground);
        }
        ByteBuffer buf = ByteBuffer.allocate(8);
        boolean islmkdPerceptSupport = this.mCameraParser.getSupportValue(LMKD_PERCEPTIBLE_SUPPORT);
        int value = isCameraForeground ? islmkdPerceptSupport ? 2 : 1 : 0;
        buf.putInt(24);
        buf.putInt(value);
        writeLmkd(buf);
    }

    private void updateOOMLevel(boolean isCameraForeground) {
        String minfreeLevel;
        String str = DEFAULT_OOM_LEVEL;
        if (TextUtils.isEmpty(str)) {
            return;
        }
        if (CAM_BOOST_DEBUG) {
            Slog.i(TAG, "updateOOMLevel: " + isCameraForeground);
            Slog.i(TAG, "TotalMemMb: " + Process.getTotalMemory());
        }
        if (isCameraForeground) {
            minfreeLevel = this.mCameraParser.getOomValue(OOM_LEVEL, str);
        } else {
            minfreeLevel = DEFAULT_OOM_LEVEL;
        }
        String[] adjLevel = minfreeLevel.split(",");
        if (adjLevel.length == 0) {
            return;
        }
        ByteBuffer buf = ByteBuffer.allocate(((adjLevel.length * 2) + 1) * 4);
        buf.putInt(0);
        for (String str2 : adjLevel) {
            try {
                String[] temp = str2.split(":");
                buf.putInt(Integer.parseInt(temp[0]));
                buf.putInt(Integer.parseInt(temp[1]));
            } catch (Exception e) {
                Slog.w(TAG, "fail to recovery minfree level " + e);
                return;
            }
        }
        writeLmkd(buf);
    }

    private void updateCheckListState(int state) {
        if (CAM_BOOST_DEBUG) {
            Slog.i(TAG, "updateCheckListState: " + state);
        }
        ByteBuffer buf = ByteBuffer.allocate(8);
        buf.putInt(20);
        buf.putInt(state);
        writeLmkd(buf);
    }

    private void adjSwapPercentage(boolean isCameraForeground) {
        if (CAM_BOOST_DEBUG) {
            Slog.i(TAG, "adjSwapPercentage: E " + isCameraForeground);
        }
        ByteBuffer buf = ByteBuffer.allocate(8);
        buf.putInt(23);
        buf.putInt(isCameraForeground ? 1 : 0);
        writeLmkd(buf);
    }

    public void boostCameraIfNeeded(long memThreshold, boolean isMiuiCamera) {
        if (!this.mCameraParser.getSupportValue(CAM_BOOST_ENABLE)) {
            return;
        }
        long duration = SystemClock.uptimeMillis() - this.mKillStartTime;
        long kill_duration = this.mCameraParser.getThresholdValue(KILL_DURATION, 2000L);
        if (duration < kill_duration) {
            Slog.i(TAG, "twice kill duration is lower than threshold, duration: " + duration + ", threshold: " + kill_duration);
            return;
        }
        Slog.i(TAG, "camera boost: " + memThreshold + ", memThreshold: " + memThreshold);
        this.mKillStartTime = SystemClock.uptimeMillis();
        if (this.mCameraParser.getSupportValue(CAM_BOOST_OPT_ENABLE)) {
            boostCameraWithProtect(memThreshold, isMiuiCamera);
        } else {
            boostCameraWithoutProtect(memThreshold);
        }
    }

    private void boostCameraWithoutProtect(long memThreshold) {
        long freeMemory = Process.getFreeMemory() / 1024;
        if ((memThreshold > 0 && freeMemory <= memThreshold) || (memThreshold <= 0 && freeMemory <= getCameraMemThreshold())) {
            final List<String> sCameraStartBlackList = this.mCameraParser.getConfigList(KILL_START_BLACK_LIST);
            if (sCameraStartBlackList != null) {
                List<ProcessRecord> blacklist_procs = getMatchedProcessList(new Comparable<ProcessRecord>() { // from class: com.android.server.am.CameraBooster.1
                    public int compareTo(ProcessRecord app) {
                        if (sCameraStartBlackList.contains(app.processName)) {
                            return 0;
                        }
                        return 1;
                    }
                }, this.mCameraParser.getConfigList(KILL_WHITE_LIST));
                int nr = blacklist_procs.size();
                for (int i = 0; i < nr; i++) {
                    killProcess(blacklist_procs.get(i));
                }
            }
            final List<String> protectlist = this.mCameraParser.getConfigList(KILL_PROTECT_LIST);
            List<ProcessRecord> procs = getMatchedProcessList(new Comparable<ProcessRecord>() { // from class: com.android.server.am.CameraBooster.2
                public int compareTo(ProcessRecord app) {
                    List list = protectlist;
                    return ((list == null || !list.contains(app.info.packageName)) && r3 <= app.mState.getSetAdj()) ? 0 : 1;
                }
            }, this.mCameraParser.getConfigList(KILL_WHITE_LIST));
            int N = procs.size();
            int willFree = ProcessUtils.getTotalPss(getPidsForProc(procs));
            Slog.i(TAG, "boost camera with free mem:" + freeMemory + "KB, kill " + N + " processes, will free memory:" + willFree + "KB");
            for (int i2 = 0; i2 < N; i2++) {
                killProcess(procs.get(i2));
            }
        }
    }

    private boolean judgeCameraScene(String callingPkg, ComponentName comp) {
        boolean isMiuiCamera = TextUtils.equals("com.android.camera", callingPkg);
        if (isMiuiCamera && comp != null && TextUtils.equals(comp.flattenToShortString(), ONESHOT_CAMERA_SCENE)) {
            Slog.i(TAG, "Oneshot Camera scene, using 3rd params");
            return false;
        }
        return isMiuiCamera;
    }

    private void boostCameraWithProtect(long memThreshold, boolean isMiuiCamera) {
        long cameraThreshold;
        long cameraThreshold2;
        long freeMemory;
        int willFree;
        long gap;
        Trace.traceBegin(1024L, "boostCamera");
        long freeMemory2 = Process.getFreeMemory() / 1024;
        long cameraThreshold3 = getCameraMemThreshold();
        long totalMemory = Process.getTotalMemory() / 1024;
        long threshold = this.mCameraParser.getThresholdValue(CAM_BOOST_THRESHOLD, 0L);
        if (memThreshold > 0) {
            cameraThreshold3 = memThreshold;
        } else if (threshold > 0 && threshold > cameraThreshold3) {
            cameraThreshold3 = threshold;
        }
        if (isMiuiCamera) {
            cameraThreshold = cameraThreshold3;
        } else {
            cameraThreshold = this.mCameraParser.getThresholdValue(CAM3RD_BOOST_THRESHOLD, cameraThreshold3);
        }
        Slog.i(TAG, "freeMemory: " + freeMemory2 + " cameraThreshold: " + cameraThreshold + ", isMiuiCamera: " + isMiuiCamera);
        if (memThreshold == 0 && !this.mCameraParser.getSupportValue(CAM_RECLAIM_ENABLE)) {
            reclaimMemoryForCamera(MODE_CAMERA_OPEN);
        }
        Map<Integer, List<ProcessRecord>> procs = new ConcurrentHashMap<>();
        generateMap(procs);
        List<String> task = getCameraBoostKillList(memThreshold, isMiuiCamera, procs);
        if (freeMemory2 <= cameraThreshold) {
            cameraThreshold2 = cameraThreshold;
            long gap2 = (cameraThreshold - freeMemory2) + this.mCameraParser.getThresholdValue(CAM_BOOST_EXT_MEM, 0L);
            if (memThreshold <= 0) {
                gap = gap2;
            } else {
                long gap3 = memThreshold - freeMemory2;
                gap = gap3;
            }
            int willFree2 = 0 + killNormalProcess(procs, gap, task);
            freeMemory = freeMemory2;
            int tag = (int) this.mCameraParser.getThresholdValue(KILL_TAG, 0L);
            if (willFree2 < gap && (tag & 1) == 1) {
                Slog.i(TAG, "begin kill system app processes");
                willFree2 += killSystemProcess(procs.get(1), gap - willFree2, task);
            }
            if (willFree2 < gap && (tag & 2) == 2) {
                Slog.i(TAG, "begin kill black processes");
                willFree2 += killProcessDeeply(procs.get(2), gap - willFree2);
            }
            if (willFree2 < gap && (tag & 4) == 4) {
                Slog.i(TAG, "begin kill protect processes");
                willFree2 += killProcessDeeply(procs.get(4), gap - willFree2);
            }
            if (willFree2 < gap && (tag & 8) == 8) {
                Slog.i(TAG, "begin kill perceptible processes");
                willFree = willFree2 + killPerceptibleProcess(procs.get(8), gap - willFree2);
            } else {
                willFree = willFree2;
            }
        } else {
            cameraThreshold2 = cameraThreshold;
            freeMemory = freeMemory2;
            willFree = 0;
        }
        if (this.mCameraParser.getSupportValue(CAM_RECLAIM_ENABLE)) {
            if (memThreshold > 0) {
                reclaimMemoryForCamera(MODE_RECORD_VIDEO);
            } else {
                reclaimMemoryForCamera(MODE_CAMERA_OPEN);
            }
        } else {
            trimProcessMemory(isMiuiCamera);
        }
        sendCameraBoostAndMemoryMsg(totalMemory, freeMemory, cameraThreshold2, willFree);
        Trace.traceEnd(1024L);
    }

    private HashSet<String> convertToPackageName(List<ProcessRecord> procs) {
        HashSet<String> name = new HashSet<>();
        if (procs == null || procs.size() == 0) {
            return name;
        }
        for (ProcessRecord app : procs) {
            name.add(app.info.packageName);
        }
        return name;
    }

    private HashSet<String> convertToProcessName(List<ProcessRecord> procs) {
        HashSet<String> name = new HashSet<>();
        if (procs == null || procs.size() == 0) {
            return name;
        }
        for (ProcessRecord app : procs) {
            name.add(app.processName);
        }
        return name;
    }

    private void generateMap(Map<Integer, List<ProcessRecord>> procs) {
        List<ProcessRecord> killProcs = new ArrayList<>();
        List<ProcessRecord> killSysProcs = new ArrayList<>();
        List<ProcessRecord> blackProcs = new ArrayList<>();
        List<ProcessRecord> protectProcs = new ArrayList<>();
        List<ProcessRecord> perceptibleProcs = new ArrayList<>();
        List<ProcessRecord> whiteProcs = new ArrayList<>();
        procs.put(0, killProcs);
        procs.put(1, killSysProcs);
        procs.put(2, blackProcs);
        procs.put(4, protectProcs);
        procs.put(8, perceptibleProcs);
        procs.put(16, whiteProcs);
    }

    private int killNormalProcess(Map<Integer, List<ProcessRecord>> procList, long gap, List<String> task) {
        int willFree = 0;
        int kill = 0;
        int stop = 0;
        List<ProcessRecord> procs = procList.get(0);
        if (procs != null && procs.size() != 0) {
            sortKillList(procs, task);
            int N = procs.size();
            try {
                HashSet<String> list = convertToPackageName(procList.get(4));
                list.addAll(convertToPackageName(procList.get(8)));
                list.addAll(convertToPackageName(procList.get(16)));
                for (int i = 0; i < N; i++) {
                    ProcessRecord app = procs.get(i);
                    if (app != null) {
                        if (this.mCameraParser.getSupportValue(KILL_FORCESTOP_ENABLE) && couldBeStoppedByCameraBoost(app, list)) {
                            if (CAM_BOOST_DEBUG) {
                                Slog.i(TAG, "force stop app name: " + app.processName + " pkg: " + app.info.packageName);
                            }
                            this.mProcessManagerService.getProcessKiller().forceStopPackage(app, "camera boost", false);
                            stop++;
                        } else {
                            killProcess(app);
                            kill++;
                        }
                        willFree = (int) (willFree + app.mProfile.getLastPss());
                    }
                    if (willFree >= gap) {
                        break;
                    }
                }
                Slog.i(TAG, "boost camera kill " + kill + " , stop " + stop + " processes, will free memory:" + willFree + "KB");
            } catch (Exception e) {
                Slog.w(TAG, "stop boost camera for exception: " + e);
            }
            return willFree;
        }
        return 0;
    }

    private int killSystemProcess(List<ProcessRecord> list, long gap, List<String> task) {
        if (list != null && list.size() != 0) {
            long highprio_thres = this.mCameraParser.getThresholdValue(KILL_HIGHPRIO_SYSAPP_THRESHOLD, 0L);
            long lowprio_thres = this.mCameraParser.getThresholdValue(KILL_LOWPRIO_SYSAPP_THRESHOLD, 0L);
            sortKillList(list, task);
            int N = list.size();
            int index = 0;
            int willFree = 0;
            int willFree2 = 0;
            while (willFree < gap && index < N) {
                try {
                    ProcessRecord app = list.get(index);
                    index++;
                    if (app != null && (app.mState.getSetAdj() <= 200 || app.mProfile.getLastPss() >= lowprio_thres)) {
                        if (app.mState.getSetAdj() > 200 || app.mProfile.getLastPss() >= highprio_thres) {
                            willFree = (int) (willFree + app.mProfile.getLastPss());
                            killProcess(app);
                            willFree2++;
                        }
                    }
                } catch (Exception e) {
                    Slog.w(TAG, "killProcess for exception: " + e);
                }
            }
            Slog.i(TAG, "kill " + willFree2 + " system processes, will free memory:" + willFree + "KB");
            return willFree;
        }
        return 0;
    }

    private int killPerceptibleProcess(List<ProcessRecord> list, long gap) {
        ProcessRecord app;
        if (list == null || list.size() == 0) {
            return 0;
        }
        int N = list.size();
        sortOtherList(list);
        int willFree = 0;
        int willFree2 = 0;
        int kill = 0;
        while (willFree < gap && willFree2 < N) {
            try {
                app = list.get(kill);
            } catch (Exception e) {
                Slog.w(TAG, "killProcess for exception: " + e);
            }
            if (app != null) {
                long threshold = this.mCameraParser.getPerceptibleKillValue(app.processName);
                if (app != null && threshold > 0 && app.mProfile.getLastPss() > threshold) {
                    willFree = (int) (willFree + app.mProfile.getLastPss());
                    kill++;
                    if (TextUtils.equals(app.processName, "com.miui.home")) {
                        Message msg = this.mHandler.obtainMessage(3, app);
                        this.mHandler.removeMessages(3);
                        this.mHandler.sendMessageDelayed(msg, 1000L);
                        willFree = (int) (willFree + app.mProfile.getLastPss());
                    } else {
                        killProcess(app);
                    }
                }
                willFree2++;
            }
        }
        Slog.i(TAG, "kill " + kill + " processes, will free memory:" + willFree + "KB");
        return willFree;
    }

    private int killProcessDeeply(List<ProcessRecord> list, long gap) {
        int kill = 0;
        int willFree = 0;
        if (list == null || list.size() == 0) {
            return 0;
        }
        int N = list.size();
        sortOtherList(list);
        for (int index = 0; willFree < gap && index < N; index++) {
            try {
                ProcessRecord app = list.get(kill);
                if (app != null && app.mProfile.getLastPss() > 0) {
                    willFree = (int) (willFree + app.mProfile.getLastPss());
                    killProcess(app);
                    kill++;
                }
            } catch (Exception e) {
                Slog.w(TAG, "killProcess for exception: " + e);
            }
        }
        Slog.i(TAG, "kill " + kill + " processes, will free memory:" + willFree + "KB");
        return willFree;
    }

    private void doSendList(Map<Integer, List<ProcessRecord>> procList, int procType, String listType, int cmd) {
        HashSet<String> list = convertToProcessName(procList.get(Integer.valueOf(procType)));
        list.addAll(this.mCameraParser.getConfigList(listType));
        Iterator<String> it = list.iterator();
        while (it.hasNext()) {
            String proc = it.next();
            updateList(proc, cmd);
        }
    }

    private void sendListToLmkd(Map<Integer, List<ProcessRecord>> procList) {
        updateCheckListState(1);
        updateCheckListState(2);
        if (this.mCameraParser.getSupportValue(LMKD_PERCEPTIBLE_SUPPORT)) {
            doSendList(procList, 4, KILL_PROTECT_LIST, 26);
            doSendList(procList, 8, KILL_PERCEPTIBLE_LIST, 27);
            doSendList(procList, 16, KILL_WHITE_LIST, 21);
        } else {
            doSendList(procList, 8, KILL_PERCEPTIBLE_LIST, 21);
            doSendList(procList, 16, KILL_WHITE_LIST, 21);
        }
        updateCheckListState(0);
    }

    public void reclaimMemoryForCamera(final int mode) {
        final long memThreshold;
        if (!this.mCameraParser.getSupportValue(CAM_RECLAIM_ENABLE)) {
            long post_delay_ms = 0;
            if (mode == MODE_CAMERA_OPEN) {
                post_delay_ms = 1000;
            }
            this.mHandler.postDelayed(new Runnable() { // from class: com.android.server.am.CameraBooster.3
                @Override // java.lang.Runnable
                public void run() {
                    ByteBuffer buf = ByteBuffer.allocate(8);
                    buf.putInt(25);
                    buf.putInt(mode);
                    CameraBooster.this.writeLmkd(buf);
                }
            }, post_delay_ms);
            return;
        }
        String str = DEVICE;
        if (!str.equals("star") && !str.equals("mars") && !str.equals("mona")) {
            return;
        }
        if (mode == MODE_RECORD_VIDEO) {
            memThreshold = this.mCameraParser.getThresholdValue(CAM_RECORD_RECLAIM_THRESHOLD, 2097152L);
        } else if (mode == MODE_CAMERA_OPEN) {
            memThreshold = this.mCameraParser.getThresholdValue(CAM_BOOST_RECLAIM_THRESHOLD, 1297152L);
        } else {
            Slog.i(TAG, "not supported condition: " + mode);
            return;
        }
        this.mHandler.post(new Runnable() { // from class: com.android.server.am.CameraBooster.4
            @Override // java.lang.Runnable
            public void run() {
                long freeKb = Process.getFreeMemory() / 1024;
                if (freeKb <= memThreshold) {
                    Slog.i(CameraBooster.TAG, "reclaimMemoryForCamera freeMemory: " + freeKb + " cameraThreshold: " + memThreshold);
                    if (mode == CameraBooster.MODE_RECORD_VIDEO) {
                        SystemProperties.set("sys.lmkd.memory.start_reclaim_in_video_record", SplitScreenReporter.ACTION_ENTER_SPLIT);
                    }
                    if (mode == CameraBooster.MODE_CAMERA_OPEN) {
                        SystemProperties.set("sys.lmkd.memory.start_reclaim_in_cam_boost", SplitScreenReporter.ACTION_ENTER_SPLIT);
                    }
                }
            }
        });
    }

    private void sendCameraBoostAndMemoryMsg(long totalMemory, long freeMemory, long cameraThreshold, int willFree) {
        final List<String> dataList = new ArrayList<>();
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_EVENT_NAME, "key_camera_memory");
            jsonObject.put("total_memory", totalMemory);
            jsonObject.put("free_memory", freeMemory);
            jsonObject.put("camera_threshold", cameraThreshold);
            jsonObject.put("will_free", willFree);
            dataList.add(jsonObject.toString());
        } catch (JSONException e) {
            e.printStackTrace();
            Slog.e(TAG, "JSONException!");
        }
        if (this.mCameraBoostAndMemoryTrackManager == null) {
            this.mCameraBoostAndMemoryTrackManager = new CameraBoostAndMemoryTrackManager(this.mContext);
        }
        this.mCameraBoostAndMemoryTrackManager.registListener(new CameraBoostAndMemoryTrackManager.OneTrackListener() { // from class: com.android.server.am.CameraBooster$$ExternalSyntheticLambda3
            @Override // com.android.server.am.CameraBoostAndMemoryTrackManager.OneTrackListener
            public final void onConnect(ServiceConnection serviceConnection) {
                CameraBooster.this.m360xf41dcaf6(dataList, serviceConnection);
            }
        });
    }

    /* renamed from: lambda$sendCameraBoostAndMemoryMsg$2$com-android-server-am-CameraBooster */
    public /* synthetic */ void m360xf41dcaf6(final List dataList, ServiceConnection serviceConnection) {
        new Thread(new Runnable() { // from class: com.android.server.am.CameraBooster$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                CameraBooster.this.m359xe075f775(dataList);
            }
        }).start();
    }

    /* renamed from: lambda$sendCameraBoostAndMemoryMsg$1$com-android-server-am-CameraBooster */
    public /* synthetic */ void m359xe075f775(List dataList) {
        CameraBoostAndMemoryTrackManager cameraBoostAndMemoryTrackManager = this.mCameraBoostAndMemoryTrackManager;
        if (cameraBoostAndMemoryTrackManager != null) {
            cameraBoostAndMemoryTrackManager.track(dataList);
        }
    }

    public void runCamReqCompaction(boolean isForeground) {
        if (!this.mCameraParser.getSupportValue(CAM_BOOST_ENABLE_COMPACT)) {
            if (CAM_BOOST_DEBUG) {
                Slog.i(TAG, "skip camera compact");
                return;
            }
            return;
        }
        MiuiMemoryServiceInternal memService = (MiuiMemoryServiceInternal) LocalServices.getService(MiuiMemoryServiceInternal.class);
        if (memService == null) {
            return;
        }
        if (isForeground) {
            if (CAM_BOOST_DEBUG) {
                Slog.i(TAG, "run camera procs compact");
            }
            memService.runProcsCompaction(0);
            return;
        }
        if (CAM_BOOST_DEBUG) {
            Slog.i(TAG, "interrupt camera procs compact");
        }
        memService.interruptProcsCompaction();
    }

    public long getCameraMemThreshold() {
        return (long) ((Process.getTotalMemory() / 1024) * CAMERA_BOOST_THRESHOLD_PERCENT);
    }

    private boolean isForegroundServices(ProcessRecord proc) {
        boolean hasForegroundServices;
        synchronized (this.mActivityManagerService) {
            hasForegroundServices = proc.mServices.hasForegroundServices();
        }
        return hasForegroundServices;
    }

    private void trimProcessMemory(boolean isMiuiCamera) {
        if (isMiuiCamera && this.mCameraParser.getSupportValue(TRIM_MEM_ENABLE)) {
            synchronized (this.mActivityManagerService) {
                int NP = this.mActivityManagerService.mProcessList.getProcessNamesLOSP().getMap().size();
                for (int ip = 0; ip < NP; ip++) {
                    SparseArray<ProcessRecord> apps = (SparseArray) this.mActivityManagerService.mProcessList.getProcessNamesLOSP().getMap().valueAt(ip);
                    int NA = apps.size();
                    for (int ia = 0; ia < NA; ia++) {
                        ProcessRecord app = apps.valueAt(ia);
                        if (app != null && !isSystemApp(app) && app.mState.getSetAdj() >= 100 && this.mProcessManagerService.isTrimMemoryEnable(app.info.packageName)) {
                            this.mProcessManagerService.getProcessKiller().trimMemory(app, false);
                            if (CAM_BOOST_DEBUG) {
                                Slog.i(TAG, "trimMemory :  " + app.processName + " (" + app.info.packageName + ")");
                            }
                        }
                    }
                }
            }
        }
    }

    private List<ProcessRecord> getMatchedProcessList(Comparable<ProcessRecord> condition, List<String> whitelist) {
        ProcessRecord app;
        ArrayList<ProcessRecord> procs = new ArrayList<>();
        ArrayList<AppStateManager.AppState> appStateList = this.mSmartPowerService.getAllAppState();
        Iterator<AppStateManager.AppState> it = appStateList.iterator();
        while (it.hasNext()) {
            AppStateManager.AppState appState = it.next();
            if (appState != null) {
                Iterator<AppStateManager.AppState.RunningProcess> it2 = appState.getRunningProcessList().iterator();
                while (it2.hasNext()) {
                    AppStateManager.AppState.RunningProcess runningProc = it2.next();
                    if (runningProc != null && !runningProc.isKilled() && (app = runningProc.getProcessRecord()) != null && !app.isPersistent() && (whitelist == null || !whitelist.contains(app.processName))) {
                        if (app.isRemoved() || condition.compareTo(app) == 0) {
                            procs.add(app);
                        }
                    }
                }
            }
        }
        return procs;
    }

    private boolean isWhiteProcess(List<String> whiteProcs, List<String> task, ProcessRecord app) {
        if (whiteProcs != null && whiteProcs.contains(app.processName)) {
            return true;
        }
        if (task != null && task.contains(app.info.packageName)) {
            return true;
        }
        return false;
    }

    private boolean isPerceptibleProcess(ProcessRecord app, boolean isVisible, boolean isPerceptible, int flag, List<String> perceptibleProces) {
        if (!this.mCameraParser.getSupportValue(PERCEPTIBLE_SUPPORT)) {
            return false;
        }
        if (perceptibleProces != null && perceptibleProces.contains(app.processName)) {
            return true;
        }
        return app.mState.getSetAdj() <= 600 && (isPerceptible || isVisible || isForegroundServices(app) || this.mSmartPowerService.isProcessWhiteList(flag, app.info.packageName, app.processName));
    }

    private boolean getTaskListToKill(int adjThreshold, int skip_task_num, Map<Integer, List<ProcessRecord>> procs, List<String> taskStack) {
        List<String> whiteProcs = this.mCameraParser.getConfigList(KILL_WHITE_LIST);
        List<String> protectProcs = this.mCameraParser.getConfigList(KILL_PROTECT_LIST);
        List<String> blackProcs = this.mCameraParser.getConfigList(KILL_BLACK_LIST);
        List<String> perceptibleProcs = this.mCameraParser.getConfigList(KILL_PERCEPTIBLE_LIST);
        List<String> recentTask = collectRecentTask(taskStack, skip_task_num);
        ArrayList<AppStateManager.AppState> appStateList = this.mSmartPowerService.getAllAppState();
        Integer num = 0;
        if (appStateList == null) {
            return false;
        }
        Iterator<AppStateManager.AppState> it = appStateList.iterator();
        while (it.hasNext()) {
            AppStateManager.AppState appState = it.next();
            if (appState != null) {
                boolean isVisible = appState.isVsible();
                Iterator<AppStateManager.AppState.RunningProcess> it2 = appState.getRunningProcessList().iterator();
                while (it2.hasNext()) {
                    AppStateManager.AppState.RunningProcess runningProc = it2.next();
                    if (runningProc == null) {
                        blackProcs = blackProcs;
                    } else if (!runningProc.isKilled()) {
                        ProcessRecord app = runningProc.getProcessRecord();
                        if (app == null) {
                            whiteProcs = whiteProcs;
                            blackProcs = blackProcs;
                        } else if (!app.isPersistent()) {
                            if (app.isRemoved()) {
                                procs.get(num).add(app);
                            } else {
                                if (blackProcs != null && blackProcs.contains(app.processName)) {
                                    procs.get(2).add(app);
                                }
                                if (!isWhiteProcess(whiteProcs, recentTask, app)) {
                                    List<String> whiteProcs2 = whiteProcs;
                                    List<String> blackProcs2 = blackProcs;
                                    Integer num2 = num;
                                    if (isPerceptibleProcess(app, isVisible, runningProc.isProcessPerceptible(), ProcessCleanerBase.SMART_POWER_PROTECT_APP_FLAGS, perceptibleProcs)) {
                                        procs.get(8).add(app);
                                        num = num2;
                                        whiteProcs = whiteProcs2;
                                        blackProcs = blackProcs2;
                                    } else if (app.mState.getSetAdj() < adjThreshold) {
                                        num = num2;
                                        whiteProcs = whiteProcs2;
                                        blackProcs = blackProcs2;
                                    } else if (protectProcs != null && protectProcs.contains(app.processName)) {
                                        procs.get(4).add(app);
                                        num = num2;
                                        whiteProcs = whiteProcs2;
                                        blackProcs = blackProcs2;
                                    } else if (isSystemApp(app)) {
                                        procs.get(1).add(app);
                                        num = num2;
                                        whiteProcs = whiteProcs2;
                                        blackProcs = blackProcs2;
                                    } else {
                                        procs.get(num2).add(app);
                                        num = num2;
                                        whiteProcs = whiteProcs2;
                                        blackProcs = blackProcs2;
                                    }
                                } else {
                                    procs.get(16).add(app);
                                }
                            }
                        }
                    }
                }
                blackProcs = blackProcs;
            }
        }
        return true;
    }

    private List<String> collectRecentTask(List<String> taskStack, int skip_task_num) {
        Throwable th;
        List<String> list = new ArrayList<>();
        synchronized (this.mActivityManagerService) {
            try {
                List<ActivityManager.RunningTaskInfo> tasks = this.mActivityManagerService.getTasks(20);
                int tasksize = tasks.size();
                List<String> skip_task_list = this.mCameraParser.getConfigList(SKIP_TASK_LIST);
                int index_lockApp = 0;
                int index_lockApp2 = 0;
                for (ActivityManager.RunningTaskInfo task : tasks) {
                    String packageName = task.topActivity.getPackageName();
                    try {
                        if (index_lockApp2 < tasksize && index_lockApp2 < skip_task_num) {
                            try {
                                String basePkg = task.baseActivity.getPackageName();
                                if (!TextUtils.equals(packageName, basePkg)) {
                                    if (CAM_BOOST_DEBUG) {
                                        Slog.i(TAG, "recent task from baseActiivity add " + basePkg);
                                    }
                                    list.add(basePkg);
                                }
                                if (skip_task_list == null || !skip_task_list.contains(packageName)) {
                                    if (CAM_BOOST_DEBUG) {
                                        Slog.i(TAG, "recent task add " + packageName);
                                    }
                                    list.add(packageName);
                                }
                            } catch (Throwable th2) {
                                th = th2;
                                throw th;
                            }
                        }
                        taskStack.add(packageName);
                        index_lockApp2++;
                    } catch (Throwable th3) {
                        th = th3;
                        throw th;
                    }
                    if (index_lockApp < 2) {
                        try {
                            if (this.mProcessManagerService.isLockedApplication(packageName, task.userId)) {
                                if (CAM_BOOST_DEBUG) {
                                    Slog.i(TAG, "add to whitelist from lock app: " + packageName);
                                }
                                list.add(packageName);
                                index_lockApp++;
                            }
                        } catch (RemoteException e) {
                            Slog.i(TAG, "isLockedApplication RemoteException :" + e);
                        }
                    }
                    if (CAM_BOOST_DEBUG) {
                        Slog.i(TAG, "task stack add " + packageName);
                    }
                }
                return list;
            } catch (Throwable th4) {
                th = th4;
            }
        }
    }

    private List<String> getCameraBoostKillList(long memThreshold, boolean isMiuiCamera, Map<Integer, List<ProcessRecord>> procs) {
        boolean MemFreeIsLow = false;
        MemInfoReader minfo = new MemInfoReader();
        minfo.readMemInfo();
        long freeKb = minfo.getFreeSizeKb();
        if (freeKb < this.mCameraParser.getThresholdValue(KILL_LOWERADJ_FREE_THRESHOLD, 0L)) {
            MemFreeIsLow = true;
        }
        int adjThreshold = (int) this.mCameraParser.getThresholdValue(KILL_ADJ_THRESHOLD, 800L);
        int skip_task_num = (int) this.mCameraParser.getThresholdValue(KILL_SKIP_TASK, 4L);
        if (memThreshold > 0 || MemFreeIsLow || this.mCameraParser.getSupportValue(CAM_RECLAIM_ENABLE)) {
            adjThreshold = (int) this.mCameraParser.getThresholdValue(isMiuiCamera ? KILL_LOWER_ADJ_THRESHOLD : KILL_3RDLOWER_ADJ_THRESHOLD, 701L);
            skip_task_num = (int) this.mCameraParser.getThresholdValue(KILL_SKIP_TASK_LOWER, 1L);
            if (!isMiuiCamera) {
                skip_task_num++;
            }
        }
        Slog.i(TAG, "freeKb = " + freeKb + " adjThreshold = " + adjThreshold + " skip_task_num = " + skip_task_num);
        List<String> taskStack = new ArrayList<>();
        if (!getTaskListToKill(adjThreshold, skip_task_num, procs, taskStack)) {
            Slog.i(TAG, "getTaskListToKill fail");
        }
        if (isMiuiCamera) {
            sendListToLmkd(procs);
        }
        return taskStack;
    }

    private void dumpProcsList(List<ProcessRecord> procs) {
        if (!CAM_BOOST_DEBUG || procs == null) {
            return;
        }
        for (ProcessRecord app : procs) {
            Slog.i(TAG, "dumpProcsList process: " + app.processName + "(" + app.info.packageName + ") pid: " + app.getPid() + ", adj : " + app.mState.getSetAdj() + ", pss : " + app.mProfile.getLastPss());
        }
    }

    private void sortKillList(List<ProcessRecord> procs, final List<String> tasks) {
        if (procs == null || tasks == null) {
            return;
        }
        try {
            Collections.sort(procs, new Comparator<ProcessRecord>() { // from class: com.android.server.am.CameraBooster.5
                public int compare(ProcessRecord b1, ProcessRecord b2) {
                    if (b1.mState.getSetAdj() > 700 && b2.mState.getSetAdj() > 700) {
                        if (tasks.contains(b1.info.packageName) && tasks.contains(b2.info.packageName)) {
                            return tasks.indexOf(b2.info.packageName) - tasks.indexOf(b1.info.packageName);
                        }
                        if (!tasks.contains(b1.info.packageName) && !tasks.contains(b2.info.packageName)) {
                            return (int) (b2.mProfile.getLastPss() - b1.mProfile.getLastPss());
                        }
                        return tasks.contains(b1.info.packageName) ? 1 : -1;
                    } else if (b1.mState.getSetAdj() <= 700 && b2.mState.getSetAdj() <= 700) {
                        return (int) (b2.mProfile.getLastPss() - b1.mProfile.getLastPss());
                    } else {
                        return b2.mState.getSetAdj() - b1.mState.getSetAdj();
                    }
                }
            });
            dumpProcsList(procs);
        } catch (Exception e) {
            Slog.w(TAG, "sortKillList for exception: " + e);
        }
    }

    private void sortOtherList(List<ProcessRecord> procs) {
        if (procs == null) {
            return;
        }
        try {
            Collections.sort(procs, new Comparator<ProcessRecord>() { // from class: com.android.server.am.CameraBooster.6
                public int compare(ProcessRecord b1, ProcessRecord b2) {
                    return (int) (b2.mProfile.getLastPss() - b1.mProfile.getLastPss());
                }
            });
            dumpProcsList(procs);
        } catch (Exception e) {
            Slog.w(TAG, "sortOtherList for exception: " + e);
        }
    }

    private boolean couldBeStoppedByCameraBoost(ProcessRecord r, HashSet<String> white) {
        if (r.getWindowProcessController().isInterestingToUser()) {
            return false;
        }
        return white == null || !white.contains(r.info.packageName);
    }

    private int[] getPidsForProc(List<ProcessRecord> procs) {
        int[] pids = null;
        synchronized (this.mActivityManagerService) {
            if (procs != null) {
                if (!procs.isEmpty()) {
                    int size = procs.size();
                    pids = new int[size];
                    for (int i = 0; i < size; i++) {
                        ProcessRecord app = procs.get(i);
                        if (app != null && app.getPid() != 0) {
                            pids[i] = app.getPid();
                        }
                    }
                }
            }
        }
        return pids;
    }

    private void getInhibitedAppProcessList(HashSet<String> inhibitedlist, String calingPkg, HashSet<Integer> pidsForNormal, HashSet<Integer> pidsForPercept) {
        ProcessRecord app;
        HashSet<Integer> pids;
        boolean is3rdSupport;
        CameraBooster cameraBooster = this;
        HashSet<Integer> pids2 = new HashSet<>();
        boolean is3rdSupport2 = cameraBooster.mCameraParser.getSupportValue(INHIBIT_3RD_SUPPORT);
        inhibitedlist.remove(calingPkg);
        List<String> white = cameraBooster.mCameraParser.getConfigList(INHIBIT_WHITE_LIST);
        ArrayList<AppStateManager.AppState> appStateList = cameraBooster.mSmartPowerService.getAllAppState();
        Iterator<AppStateManager.AppState> it = appStateList.iterator();
        while (it.hasNext()) {
            AppStateManager.AppState appState = it.next();
            if (appState != null) {
                boolean isVisible = appState.isVsible();
                Iterator<AppStateManager.AppState.RunningProcess> it2 = appState.getRunningProcessList().iterator();
                while (it2.hasNext()) {
                    AppStateManager.AppState.RunningProcess runningProc = it2.next();
                    if (runningProc == null) {
                        cameraBooster = this;
                        pids2 = pids2;
                    } else if (!runningProc.isKilled() && (app = runningProc.getProcessRecord()) != null && (white == null || (!white.contains(app.processName) && !white.contains(app.info.packageName)))) {
                        if (inhibitedlist.contains(runningProc.getPackageName())) {
                            pidsForNormal.add(Integer.valueOf(runningProc.getPid()));
                            if (CAM_BOOST_DEBUG) {
                                Slog.i(TAG, "inhibited list process: " + runningProc.toString());
                            }
                        } else {
                            if (!is3rdSupport2) {
                                pids = pids2;
                                is3rdSupport = is3rdSupport2;
                            } else if (cameraBooster.isSystemApp(app)) {
                                cameraBooster = this;
                                pids2 = pids2;
                            } else if (!TextUtils.equals(calingPkg, app.info.packageName)) {
                                if (runningProc.isProcessPerceptible() || isVisible || cameraBooster.isForegroundServices(app)) {
                                    pids = pids2;
                                    is3rdSupport = is3rdSupport2;
                                } else {
                                    pids = pids2;
                                    is3rdSupport = is3rdSupport2;
                                    if (!cameraBooster.mSmartPowerService.isProcessWhiteList(ProcessCleanerBase.SMART_POWER_PROTECT_APP_FLAGS, app.info.packageName, app.processName)) {
                                        if (CAM_BOOST_DEBUG) {
                                            Slog.i(TAG, "inhibited 3rd process: " + app.processName + "(" + app.info.packageName + ") pid: " + app.getPid());
                                        }
                                        pidsForNormal.add(Integer.valueOf(app.getPid()));
                                    }
                                }
                                if (CAM_BOOST_DEBUG) {
                                    Slog.i(TAG, "inhibit for perceptible process : " + runningProc.toString());
                                }
                                pidsForPercept.add(Integer.valueOf(app.getPid()));
                            }
                            cameraBooster = this;
                            pids2 = pids;
                            is3rdSupport2 = is3rdSupport;
                        }
                    }
                }
                cameraBooster = this;
                pids2 = pids2;
            }
        }
    }

    private boolean isSystemApp(ProcessRecord app) {
        return (app == null || app.info == null || (app.info.flags & 129) == 0) ? false : true;
    }

    private void getInhibitedProcsList(String calingPkg, HashSet<Integer> pidsForNormal, HashSet<Integer> pidsForPercept) {
        HashSet<String> inhibitList = new HashSet<>(this.mCameraParser.getConfigList(INHIBIT_APP_LIST));
        getInhibitedAppProcessList(inhibitList, calingPkg, pidsForNormal, pidsForPercept);
        List<String> sCameraInhibitNativeList = this.mCameraParser.getConfigList(INHIBIT_NATIVE_LIST);
        if (sCameraInhibitNativeList != null) {
            String[] nativeInhibitList = (String[]) sCameraInhibitNativeList.toArray(new String[sCameraInhibitNativeList.size()]);
            int[] nativePids = Process.getPidsForCommands(nativeInhibitList);
            if (nativePids != null) {
                for (int i : nativePids) {
                    pidsForNormal.add(Integer.valueOf(i));
                }
            }
        }
    }

    private void setInhibitProcessCgroup(HashSet<Integer> pids, boolean isPerceptible) {
        Iterator<Integer> it = pids.iterator();
        while (it.hasNext()) {
            int pid = it.next().intValue();
            try {
                Integer mProcGrp = Integer.valueOf(Process.getCpusetThreadGroup(pid));
                this.mProcessCpuSetMap.put(Integer.valueOf(pid), mProcGrp);
                int i = 1;
                if (mProcGrp.intValue() == 0) {
                    if (!isPerceptible) {
                        i = 2;
                    }
                } else if (isPerceptible) {
                    i = 0;
                }
                int group = i;
                Process.setCameraBackgroundCpusetGroup(pid, group);
                Slog.i(TAG, "cam_sched: move pid  :" + pid + ", of mProcGrp : " + mProcGrp + " to " + group + ", isPerceptible: " + isPerceptible + "(0: cb, 1: cb/b, 2:cb/l)");
            } catch (Exception e) {
                Slog.w(TAG, "cam_sched: fail find " + pid + " and move process to camera background " + e);
            }
        }
    }

    private void updateInhibitProcessGroup(boolean isCameraForeground, String foregroundPkg) {
        if (!this.mCameraParser.getSupportValue(INHIBIT_SUPPORT)) {
            return;
        }
        Trace.traceBegin(1024L, "CameraInhibitProcess_" + (isCameraForeground ? "F" : "B"));
        if (isCameraForeground) {
            if (this.mProcessCpuSetMap.isEmpty()) {
                HashSet<Integer> pidsForNormal = new HashSet<>();
                HashSet<Integer> pidsForPercept = new HashSet<>();
                getInhibitedProcsList(foregroundPkg, pidsForNormal, pidsForPercept);
                setInhibitProcessCgroup(pidsForNormal, false);
                setInhibitProcessCgroup(pidsForPercept, true);
            } else {
                Slog.w(TAG, "cam_sched: the sched pid has not restore finish yet ");
            }
        } else {
            Iterator<Map.Entry<Integer, Integer>> it = this.mProcessCpuSetMap.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Integer, Integer> item = it.next();
                Integer restore_pid = item.getKey();
                Integer mProcGrp = item.getValue();
                it.remove();
                try {
                    Integer curGrp = Integer.valueOf(Process.getCpusetThreadGroup(restore_pid.intValue()));
                    Slog.i(TAG, "cam_sched: restore_pid : " + restore_pid + ", curGrp : " + curGrp);
                    if (curGrp.intValue() == 9) {
                        if (mProcGrp != null && mProcGrp.intValue() >= -1 && mProcGrp.intValue() <= 5) {
                            if (mProcGrp.intValue() == 1) {
                                Process.setProcessGroup(restore_pid.intValue(), -1);
                            } else {
                                Process.setProcessGroup(restore_pid.intValue(), mProcGrp.intValue());
                            }
                        } else {
                            Slog.i(TAG, "cam_sched: lost processgroup info, so move to default processgroup");
                            Process.setProcessGroup(restore_pid.intValue(), -1);
                        }
                        Slog.i(TAG, "cam_sched: restore pid: " + restore_pid + ", from camera-background to origin cpuset cgroup :" + mProcGrp);
                    }
                } catch (Exception e) {
                    Slog.w(TAG, "cam_sched: fail restore ProcessGroup: " + e);
                }
            }
        }
        Trace.traceEnd(1024L);
    }

    public boolean interceptAppRestartIfNeeded(String processName, String type) {
        long now = SystemClock.uptimeMillis();
        long intercept_time = this.mCameraParser.getThresholdValue(INTERCEPT_MS, 5000L);
        if (intercept_time > 0 && now - this.startTime < intercept_time) {
            List<String> cameraRestartBlackList = this.mCameraParser.getConfigList(INTERCEPT_RESTART_LIST);
            if (!type.contains("activity") && cameraRestartBlackList != null && cameraRestartBlackList.contains(processName)) {
                return true;
            }
            return false;
        }
        return false;
    }

    private void dumpProcessCpuStat(boolean isForeground) {
        ProcessRecord app;
        long time;
        ArrayList<AppStateManager.AppState> appStateList = this.mSmartPowerService.getAllAppState();
        if (appStateList == null) {
            return;
        }
        try {
            Map<String, Long> result = new LinkedHashMap<>();
            Iterator<AppStateManager.AppState> it = appStateList.iterator();
            while (it.hasNext()) {
                AppStateManager.AppState appState = it.next();
                if (appState != null) {
                    Iterator<AppStateManager.AppState.RunningProcess> it2 = appState.getRunningProcessList().iterator();
                    while (it2.hasNext()) {
                        AppStateManager.AppState.RunningProcess runningProc = it2.next();
                        if (runningProc != null && !runningProc.isKilled() && (app = runningProc.getProcessRecord()) != null) {
                            if (isForeground) {
                                this.mCpuConsume.put(app.processName, Long.valueOf(app.getCpuTime()));
                            } else {
                                if (this.mCpuConsume.containsKey(app.processName) && this.mCpuConsume.get(app.processName) != null) {
                                    time = app.getCpuTime() - this.mCpuConsume.get(app.processName).longValue();
                                } else {
                                    time = app.getCpuTime();
                                }
                                result.put(app.processName, Long.valueOf(time));
                            }
                        }
                    }
                }
            }
            if (isForeground || result.size() == 0) {
                return;
            }
            final Map<String, Long> map = new LinkedHashMap<>();
            result.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).limit(10L).forEach(new Consumer() { // from class: com.android.server.am.CameraBooster$$ExternalSyntheticLambda0
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    map.put((String) r2.getKey(), (Long) ((Map.Entry) obj).getValue());
                }
            });
            for (Map.Entry<String, Long> entry : map.entrySet()) {
                Slog.i(TAG, "process: " + entry.getKey() + " spent " + entry.getValue() + " ms during camera foreground");
            }
            this.mCpuConsume.clear();
        } catch (Exception e) {
            Slog.w(TAG, "dumpProcessCpuStat fail : " + e);
        }
    }

    public void notifyCameraPostProcessState() {
        Slog.i(TAG, "notifyCameraPostProcessState");
        if (this.mIsCameraForeground) {
            return;
        }
        updateState(1, false, true, 1500L, 0L);
    }

    public void killProcess(ProcessRecord app) {
        if (app != null) {
            Slog.i(TAG, "kill app name: " + app.processName + " pkg: " + app.info.packageName + " pss: " + app.mProfile.getLastPss());
            this.mProcessManagerService.getProcessKiller().killApplication(app, "camera boost", false);
        }
    }
}
