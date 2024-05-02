package com.android.server.wm;

import android.app.ActivityOptions;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.AudioSystem;
import android.os.Binder;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.RemoteException;
import android.util.Slog;
import android.view.Display;
import android.view.DisplayInfo;
import android.view.Surface;
import com.android.server.LocalServices;
import com.android.server.am.PreloadAppControllerImpl;
import com.android.server.wm.ActivityRecord;
import com.android.server.wm.ActivityStarter;
import com.miui.base.MiuiStubRegistry;
import com.miui.base.MiuiStubUtil;
import com.miui.server.greeze.GreezeManagerService;
import com.miui.server.greeze.GreezeServiceUtils;
import com.miui.server.process.ProcessManagerInternal;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import miui.greeze.IGreezeCallback;
import miui.process.RunningProcessInfo;
import miui.util.ReflectionUtils;
/* loaded from: classes.dex */
public class PreloadStateManagerImpl implements PreloadStateManagerStub {
    public static final String ADVANCE_KILL_PRELOADAPP_REASON = "advance_kill_preloadApp";
    public static final int DELAY_STOP_PRELOAD_ACTIVITIES_MSG = 199;
    public static final String MUTE_PRELOAD_UID = "mute_preload_uid=";
    private static final String PERMISSION_ACTIVITY_NAME = "permission.ui.GrantPermissionsActivity";
    private static final String PRELOAD_DISPLAY_NAME = "PreloadDisplay";
    public static final String REMOVE_MUTE_PRELOAD_UID = "remove_mute_preload_uid=";
    private static final String TAG = "PreloadStateManagerImpl";
    public static final String TIMEOUT_KILL_PRELOADAPP_REASON = "timeout_kill_preloadApp";
    private static ActivityTaskManagerService mService;
    private static PreloadStateHandler sHandler;
    private static VirtualDisplay sHorizontalPreloadDisplay;
    private static VirtualDisplay sPreloadDisplay;
    private static ProcessManagerInternal sProcessManagerInternal;
    private static final Object sEnableAudioLock = new Object();
    private static Map<Integer, UidInfo> sPreloadUidInfos = new ConcurrentHashMap();
    private static List<Integer> sWaitEnableAudioUids = new ArrayList();
    private static Set<String> sHorizontalPacakges = new HashSet();
    private static boolean sPreloadAppStarted = false;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<PreloadStateManagerImpl> {

        /* compiled from: PreloadStateManagerImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final PreloadStateManagerImpl INSTANCE = new PreloadStateManagerImpl();
        }

        public PreloadStateManagerImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public PreloadStateManagerImpl provideNewInstance() {
            return new PreloadStateManagerImpl();
        }
    }

    public static PreloadStateManagerImpl getInstance() {
        return (PreloadStateManagerImpl) MiuiStubUtil.getImpl(PreloadStateManagerStub.class);
    }

    public static boolean checkEnablePreload() {
        try {
            ReflectionUtils.findMethodBestMatch(Task.class, "removeStopPreloadActivityMsg", new Class[0]);
            return true;
        } catch (Exception e) {
            if (PreloadAppControllerImpl.DEBUG) {
                Slog.e(TAG, "preloadApp lack of some changes", e);
            }
            return false;
        }
    }

    public void init(ActivityTaskManagerService mService2) {
        mService = mService2;
        sHandler = new PreloadStateHandler(mService2.mH.getLooper());
        sHorizontalPacakges.add("com.tencent.tmgp.sgame");
        sHorizontalPacakges.add("com.tencent.tmgp.pubgmhd");
    }

    public void switchAppPreloadState(ActivityRecord r) {
        if (PreloadAppControllerImpl.DEBUG) {
            Slog.w(TAG, "First start preloadApp to VirtualDisplay activity uid:" + r.getUid() + " callingUid=" + Binder.getCallingUid() + "pid=" + Binder.getCallingPid());
        }
        sPreloadUidInfos.put(Integer.valueOf(r.getUid()), new UidInfo());
        disableAudio(true, r.getUid());
    }

    public void switchAppNormalState(ActivityRecord r) {
        if (PreloadAppControllerImpl.DEBUG) {
            Slog.w(TAG, "move preloadApp to defaultDisplay uid:" + r.getUid() + "callingUid=" + Binder.getCallingUid() + "pid=" + Binder.getCallingPid());
        }
        List<RunningProcessInfo> runningProcessInfos = getRunningProcessInfos();
        boolean procIsRunninng = false;
        for (RunningProcessInfo info : runningProcessInfos) {
            if (info != null && info.mUid == r.getUid()) {
                procIsRunninng = true;
                setSchedAffinity(info.mPid, PreloadAppControllerImpl.sDefaultSchedAffinity);
            }
        }
        PreloadAppControllerImpl preloadController = PreloadAppControllerImpl.getInstance();
        if (procIsRunninng && preloadController != null) {
            preloadController.onPreloadAppStarted(r.getUid(), r.info != null ? r.info.packageName : "", r.app != null ? r.app.getPid() : -1);
        }
        sHandler.removeMessages(2, sPreloadUidInfos.get(Integer.valueOf(r.getUid())));
        sHandler.removeMessages(1, sPreloadUidInfos.get(Integer.valueOf(r.getUid())));
        sPreloadUidInfos.remove(Integer.valueOf(r.getUid()));
        if (GreezeManagerService.isEnable()) {
            GreezeManagerService.getService().thawUids(new int[]{r.getUid()}, GreezeServiceUtils.GREEZER_MODULE_PRELOAD, "thaw preloadApp");
        }
        synchronized (sEnableAudioLock) {
            sWaitEnableAudioUids.add(Integer.valueOf(r.getUid()));
        }
    }

    private List<RunningProcessInfo> getRunningProcessInfos() {
        if (sProcessManagerInternal == null) {
            sProcessManagerInternal = (ProcessManagerInternal) LocalServices.getService(ProcessManagerInternal.class);
        }
        return sProcessManagerInternal.getAllRunningProcessInfo();
    }

    public static void disableAudio(boolean disable, int uid) {
        if (PreloadAppControllerImpl.DEBUG) {
            Slog.w(TAG, "preloadApp enableAudio " + disable + " uid=" + uid);
        }
        if (disable) {
            AudioSystem.setParameters(MUTE_PRELOAD_UID + uid);
        } else {
            AudioSystem.setParameters(REMOVE_MUTE_PRELOAD_UID + uid);
        }
    }

    public Task resetInTask(Task inTask) {
        if (sPreloadAppStarted) {
            return null;
        }
        return inTask;
    }

    private boolean setSchedAffinity(int pid, int[] schedAffinity) {
        Method method = ReflectionUtils.tryFindMethodBestMatch(Process.class, "setSchedAffinity", new Object[]{Integer.valueOf(pid), schedAffinity});
        if (method == null) {
            return false;
        }
        try {
            method.invoke(Process.class, Integer.valueOf(pid), schedAffinity);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (PreloadAppControllerImpl.DEBUG) {
            StringBuilder sb = new StringBuilder();
            for (int i : schedAffinity) {
                sb.append(i);
            }
            Slog.w(TAG, "preloadApp reset cpuset pid:" + pid + " schedAffinity:" + ((Object) sb));
        }
        return true;
    }

    public boolean alreadyPreload(int uid) {
        return sPreloadUidInfos.containsKey(Integer.valueOf(uid));
    }

    public boolean isPreloadDisplay(Display display) {
        if (display == null) {
            return false;
        }
        return isPreloadDisplayId(display.getDisplayId());
    }

    public boolean isPreloadDisplayContent(DisplayContent displayContent) {
        if (displayContent == null) {
            return false;
        }
        return isPreloadDisplayId(displayContent.getDisplayId());
    }

    public boolean isPreloadDisplayId(int displayId) {
        VirtualDisplay virtualDisplay = sHorizontalPreloadDisplay;
        if (virtualDisplay == null || virtualDisplay.getDisplay().getDisplayId() != displayId) {
            VirtualDisplay virtualDisplay2 = sPreloadDisplay;
            return virtualDisplay2 != null && virtualDisplay2.getDisplay().getDisplayId() == displayId;
        }
        return true;
    }

    public int replaceIdForPreloadApp(int displayId) {
        if (isPreloadDisplayId(displayId) && alreadyPreload(Binder.getCallingUid())) {
            return 0;
        }
        return displayId;
    }

    public boolean needNotRelaunch(int displayId, String packagename) {
        if (isPreloadDisplayId(displayId) && !PreloadAppControllerImpl.inRelaunchBlackList(packagename)) {
            return true;
        }
        return false;
    }

    public ActivityOptions startActivityInjector(ActivityStarter.Request request, ActivityRecord r, ActivityRecord sourceRecord, ActivityOptions options, Task inTask) {
        int realCallingUid;
        sPreloadAppStarted = false;
        if (request.realCallingUid != -1) {
            realCallingUid = request.realCallingUid;
        } else {
            realCallingUid = Binder.getCallingUid();
        }
        if ((alreadyPreload(r.getUid()) && (sourceRecord == null || alreadyPreload(sourceRecord.getUid()))) || alreadyPreload(realCallingUid)) {
            if (PreloadAppControllerImpl.DEBUG) {
                Slog.w(TAG, "set preloadApp launch PreloadDisplay r=" + r + " uid=" + r.getUid() + " realCallingUid=" + realCallingUid);
            }
            if (options == null) {
                options = ActivityOptions.makeBasic();
            }
            options.setLaunchDisplayId(getOrCreatePreloadDisplayId(r.info.packageName));
        }
        if (needReparent(r, sourceRecord, request, inTask)) {
            if (options == null) {
                options = ActivityOptions.makeBasic();
            }
            sPreloadAppStarted = true;
            switchAppNormalState(r);
            options.setLaunchDisplayId(0);
        } else if (r.getUid() > 1000 && options != null && isPreloadDisplayId(options.getLaunchDisplayId()) && !alreadyPreload(r.getUid())) {
            switchAppPreloadState(r);
        }
        return options;
    }

    public boolean needReparent(ActivityRecord r, ActivityRecord sourceRecord, ActivityStarter.Request request, Task inTask) {
        if (!alreadyPreload(r.getUid())) {
            return false;
        }
        if (sourceRecord != null) {
            return !alreadyPreload(sourceRecord.getUid());
        } else if (!"startActivityFromRecents".equals(request.reason)) {
            return false;
        } else {
            if (inTask != null) {
                inTask.inRecents = false;
            }
            return true;
        }
    }

    public void onMoveStackToDefaultDisplay(int displayId, Task targetStack, ActivityRecord intentActivity, boolean noAnimation, ActivityOptions options) {
        if (isPreloadDisplayId(displayId) && !isPreloadDisplayId(targetStack.getDisplayId())) {
            if (PreloadAppControllerImpl.DEBUG) {
                Slog.w(TAG, "preloadApp move task to DefaultDisplay uid=" + intentActivity.getUid() + ";packageName=" + intentActivity.packageName);
            }
            onPreloadStackHasReparent(targetStack, intentActivity, noAnimation, options);
        }
    }

    private void onPreloadStackHasReparent(Task targetStack, ActivityRecord intentActivity, boolean noAnimation, ActivityOptions options) {
        mService.mTaskSupervisor.mRecentTasks.add(intentActivity.getTask());
        if (intentActivity.app != null) {
            intentActivity.app.mIsPreloaded = false;
        }
        try {
            Method method = ReflectionUtils.findMethodBestMatch(Task.class, "removeStopPreloadActivityMsg", new Class[0]);
            method.invoke(targetStack, new Object[0]);
        } catch (Exception e) {
            if (PreloadAppControllerImpl.DEBUG) {
                Slog.e(TAG, "preloadApp removeStopPreloadActivityMsg reflect Exception:", e);
            }
        }
        if (!noAnimation) {
            if (intentActivity.isState(ActivityRecord.State.RESUMED)) {
                intentActivity.setState(ActivityRecord.State.PAUSED, "preload activity reset state");
                intentActivity.setVisible(false);
            }
            intentActivity.updateOptionsLocked(options);
            intentActivity.mDisplayContent.prepareAppTransition(3);
        }
    }

    public boolean stopPreloadActivity(ActivityRecord r, ActivityRecord prev, Task task) {
        if (r == null) {
            ActivityRecord top = task.getTopNonFinishingActivity();
            if (top == null) {
                return false;
            }
            r = top;
        }
        if (r.getName().contains(PERMISSION_ACTIVITY_NAME) && prev != null) {
            r = prev;
        }
        if (!alreadyPreload(r.getUid())) {
            return false;
        }
        if (PreloadAppControllerImpl.DEBUG) {
            Slog.w(TAG, "preloadApp mReadyPauseActivity=" + r);
        }
        synchronized (mService.getGlobalLock()) {
            r.makeInvisible();
        }
        return true;
    }

    public boolean stackOnParentChanged(DisplayContent display, boolean forceStop) {
        if (display == null || !isPreloadDisplayId(display.getDisplayId())) {
            return false;
        }
        return forceStop;
    }

    public boolean hidePreloadActivity(ActivityRecord next, boolean forceStop) {
        if (forceStop && alreadyPreload(next.getUid())) {
            return true;
        }
        return false;
    }

    public void resumeTopActivityInjector(ActivityRecord next, Handler handler) {
        if (alreadyPreload(next.getUid()) && !handler.hasMessages(DELAY_STOP_PRELOAD_ACTIVITIES_MSG)) {
            long stopTimeout = PreloadAppControllerImpl.queryStopTimeoutFromUid(next.getUid());
            handler.sendEmptyMessageDelayed(DELAY_STOP_PRELOAD_ACTIVITIES_MSG, stopTimeout);
            if (PreloadAppControllerImpl.DEBUG) {
                Slog.w(TAG, "preloadApp uid=" + next.getUid() + " pid=" + next.getPid() + " will stop after:" + stopTimeout);
            }
        }
    }

    public void checkEnableAudio() {
        synchronized (sEnableAudioLock) {
            if (sWaitEnableAudioUids.size() == 0) {
                return;
            }
            for (Integer num : sWaitEnableAudioUids) {
                int uid = num.intValue();
                Message message = sHandler.obtainMessage();
                message.arg1 = uid;
                message.what = 4;
                sHandler.sendMessageDelayed(message, 100L);
            }
            sWaitEnableAudioUids.clear();
        }
    }

    public void removePreloadTask(int displayId, ActivityRecord r) {
        Task task;
        if (isPreloadDisplayId(displayId) && (task = r.getTask()) != null) {
            mService.mTaskSupervisor.removeTask(task, false, true, "remove preloadApp task");
        }
    }

    public int adjustDisplayId(Session session, int displayId) {
        if (alreadyPreload(session.mUid) && displayId == 0) {
            int displayId2 = getOrCreatePreloadDisplayId();
            Slog.w(TAG, "preloadApp adjustDisplayId displayId=" + displayId2);
            return displayId2;
        }
        return displayId;
    }

    public void reparentPreloadStack(Task targetStack, ActivityRecord intentActivity, boolean noAnimation, ActivityOptions options) {
        if (isPreloadDisplayId(targetStack.getDisplayId()) && !alreadyPreload(intentActivity.getUid())) {
            if (PreloadAppControllerImpl.DEBUG) {
                Slog.w(TAG, "preloadApp reparentPreloadStack " + intentActivity.getUid() + ";packageName=" + intentActivity.packageName);
            }
            mService.mRootWindowContainer.moveRootTaskToDisplay(targetStack.getRootTaskId(), 0, true);
            onPreloadStackHasReparent(targetStack, intentActivity, noAnimation, options);
        }
    }

    public int repositionDisplayContent(int position, Object displayContent) {
        if ((displayContent instanceof DisplayContent) && isPreloadDisplayId(((DisplayContent) displayContent).getDisplayId())) {
            Slog.w(TAG, "preloadApp need reposition display content");
            return 0;
        }
        return position;
    }

    public boolean notReplaceDisplayContent(int displayId, WindowToken wToken) {
        if (isPreloadDisplayId(displayId) && wToken.getDisplayContent() != null && displayId != wToken.getDisplayContent().getDisplayId()) {
            Slog.w(TAG, "preloadApp need replace display content");
            return false;
        }
        return true;
    }

    public void onAttachApplication(WindowProcessController app) {
        if (!alreadyPreload(app.mUid)) {
            return;
        }
        setSchedAffinity(app.getPid(), PreloadAppControllerImpl.querySchedAffinityFromUid(app.mUid));
        UidInfo info = sPreloadUidInfos.get(Integer.valueOf(app.mUid));
        if (info == null || info.uid < 0) {
            app.mIsPreloaded = true;
            info = onFirstProcAttachForUid(app.mUid);
        }
        if (GreezeManagerService.isEnable()) {
            delayFreezeAppForUid(info);
        }
    }

    private void delayFreezeAppForUid(UidInfo info) {
        Message msgFreeze = sHandler.obtainMessage(1);
        msgFreeze.obj = info;
        long freezeTimeout = PreloadAppControllerImpl.queryFreezeTimeoutFromUid(info.uid);
        sHandler.sendMessageDelayed(msgFreeze, freezeTimeout);
        if (PreloadAppControllerImpl.DEBUG) {
            Slog.w(TAG, "preloadApp onAttachApplication uid " + info.uid + " freezeTimeout=" + freezeTimeout);
        }
    }

    private UidInfo onFirstProcAttachForUid(int uid) {
        UidInfo info = new UidInfo(uid);
        sPreloadUidInfos.put(Integer.valueOf(uid), info);
        sHandler.removeMessages(2, sPreloadUidInfos.get(Integer.valueOf(uid)));
        Message msg = sHandler.obtainMessage(2);
        msg.obj = info;
        long killTimeout = PreloadAppControllerImpl.queryKillTimeoutFromUid(uid);
        sHandler.sendMessageDelayed(msg, killTimeout);
        if (PreloadAppControllerImpl.DEBUG) {
            Slog.w(TAG, "preloadApp onFirstProcAttach uid " + uid + " killTimeout=" + killTimeout);
        }
        return info;
    }

    public static int getOrCreatePreloadDisplayId() {
        if (sPreloadDisplay == null) {
            synchronized (PreloadStateManagerImpl.class) {
                sPreloadDisplay = createPreloadDisplay(0);
                GreezeManagerService service = GreezeManagerService.getService();
                if (service != null) {
                    try {
                        service.registerCallback(new GreezeCallback(), GreezeServiceUtils.GREEZER_MODULE_PRELOAD);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return sPreloadDisplay.getDisplay().getDisplayId();
    }

    public static int getOrCreatePreloadDisplayId(String packageName) {
        if (sHorizontalPacakges.contains(packageName)) {
            if (sHorizontalPreloadDisplay == null) {
                synchronized (PreloadStateManagerImpl.class) {
                    sHorizontalPreloadDisplay = createPreloadDisplay(1);
                }
            }
            return sHorizontalPreloadDisplay.getDisplay().getDisplayId();
        }
        return getOrCreatePreloadDisplayId();
    }

    public int getPreloadDisplayId() {
        VirtualDisplay virtualDisplay = sPreloadDisplay;
        if (virtualDisplay == null) {
            return -1;
        }
        return virtualDisplay.getDisplay().getDisplayId();
    }

    public static void onPreloadAppKilled(int uid) {
        PreloadAppControllerImpl controller = PreloadAppControllerImpl.getInstance();
        if (controller != null) {
            controller.onPreloadAppKilled(uid);
        }
        sPreloadUidInfos.remove(Integer.valueOf(uid));
        disableAudio(false, uid);
        if (PreloadAppControllerImpl.DEBUG) {
            Slog.w(TAG, "preloadApp is Killed, remove uid " + uid);
        }
        sHandler.removeMessages(2, sPreloadUidInfos.get(Integer.valueOf(uid)));
    }

    private static VirtualDisplay createPreloadDisplay(int orientation) {
        DisplayInfo info = new DisplayInfo();
        Surface surface = new Surface();
        DisplayManager displayManager = (DisplayManager) mService.mContext.getSystemService(DisplayManager.class);
        displayManager.getDisplay(0).getDisplayInfo(info);
        long origId = Binder.clearCallingIdentity();
        if (orientation == 1) {
            int h = info.logicalHeight;
            info.logicalHeight = info.logicalWidth;
            info.logicalWidth = h;
        }
        VirtualDisplay display = displayManager.createVirtualDisplay(PRELOAD_DISPLAY_NAME, info.logicalWidth, info.logicalHeight, info.logicalDensityDpi, surface, 4);
        Binder.restoreCallingIdentity(origId);
        return display;
    }

    public static void enableAudio(int uid) {
        Message enableMsg = Message.obtain();
        enableMsg.arg1 = uid;
        enableMsg.what = 3;
        sHandler.sendMessageDelayed(enableMsg, 300L);
    }

    public static void killPreloadApp(String packageName, int uid) {
        ((ProcessManagerInternal) LocalServices.getService(ProcessManagerInternal.class)).forceStopPackage(packageName, 0, ADVANCE_KILL_PRELOADAPP_REASON);
        onPreloadAppKilled(uid);
    }

    /* loaded from: classes.dex */
    public static class UidInfo {
        public int uid;

        public UidInfo() {
            this.uid = -1;
        }

        public UidInfo(int uid) {
            this.uid = -1;
            this.uid = uid;
        }

        public int hashCode() {
            return this.uid;
        }

        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            UidInfo info = (UidInfo) obj;
            if (this.uid != info.uid) {
                return false;
            }
            return true;
        }
    }

    /* loaded from: classes.dex */
    public static final class PreloadStateHandler extends Handler {
        static final int ENABLE_AUDIO_MSG = 4;
        static final int FREEZE_PROCESS_MSG = 1;
        static final int KILL_PROCESS_MSG = 2;
        static final int ON_KILL_ENABLE_AUDIO_MSG = 3;

        public PreloadStateHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    UidInfo info = (UidInfo) msg.obj;
                    if (info != null && PreloadStateManagerImpl.getInstance().alreadyPreload(info.uid)) {
                        if (PreloadAppControllerImpl.DEBUG) {
                            Slog.w(PreloadStateManagerImpl.TAG, "freeze preloadApp uid " + info.uid);
                        }
                        GreezeManagerService.getService().freezeUids(new int[]{info.uid}, 0L, GreezeServiceUtils.GREEZER_MODULE_PRELOAD, "freeze preloadApp", false);
                        GreezeManagerService.getService().queryBinderState(info.uid);
                        return;
                    }
                    return;
                case 2:
                    UidInfo info2 = (UidInfo) msg.obj;
                    if (info2 != null && PreloadStateManagerImpl.getInstance().alreadyPreload(info2.uid)) {
                        PreloadStateManagerImpl.sPreloadUidInfos.put(Integer.valueOf(info2.uid), new UidInfo());
                        String packageName = PreloadAppControllerImpl.getPackageName(info2.uid);
                        if (packageName != null) {
                            Slog.e(PreloadStateManagerImpl.TAG, "timeout kill preloadApp " + packageName);
                            ((ProcessManagerInternal) LocalServices.getService(ProcessManagerInternal.class)).forceStopPackage(packageName, 0, PreloadStateManagerImpl.TIMEOUT_KILL_PRELOADAPP_REASON);
                            return;
                        }
                        Slog.e(PreloadStateManagerImpl.TAG, "preloadApp kill fail because packagename is null");
                        return;
                    }
                    return;
                case 3:
                    int uid = msg.arg1;
                    if (PreloadStateManagerImpl.sPreloadUidInfos.containsKey(Integer.valueOf(uid)) && ((UidInfo) PreloadStateManagerImpl.sPreloadUidInfos.get(Integer.valueOf(uid))).uid < 0) {
                        PreloadStateManagerImpl.onPreloadAppKilled(uid);
                        return;
                    }
                    return;
                case 4:
                    PreloadStateManagerImpl.disableAudio(false, msg.arg1);
                    return;
                default:
                    return;
            }
        }
    }

    /* loaded from: classes.dex */
    public static class GreezeCallback extends IGreezeCallback.Stub {
        private GreezeCallback() {
        }

        public void reportSignal(int uid, int pid, long now) {
            if (PreloadStateManagerImpl.sPreloadUidInfos.containsKey(Integer.valueOf(uid)) && PreloadAppControllerImpl.DEBUG) {
                Slog.w(PreloadStateManagerImpl.TAG, "preloadApp reportSignal remove uid " + uid);
            }
        }

        public void reportNet(int uid, long now) throws RemoteException {
        }

        public void reportBinderTrans(int dstUid, int dstPid, int callerUid, int callerPid, int callerTid, boolean isOneway, long now, long buffer) throws RemoteException {
            GreezeManagerService.getService().thawUids(new int[]{dstUid}, GreezeServiceUtils.GREEZER_MODULE_PRELOAD, "preloadApp receive frozen binder trans: dstUid=" + dstUid + " dstPid=" + dstPid + " callerUid=" + callerUid + " callerPid=" + callerPid + " callerTid=" + callerTid + " oneway=" + isOneway);
        }

        public void reportBinderState(int uid, int pid, int tid, int binderState, long now) throws RemoteException {
            switch (binderState) {
                case 0:
                default:
                    return;
                case 1:
                    GreezeManagerService.getService().thawUids(new int[]{uid}, GreezeServiceUtils.GREEZER_MODULE_PRELOAD, "preloadApp receive binder state: uid=" + uid + " pid=" + pid + " tid=" + tid);
                    return;
                case 2:
                case 3:
                case 4:
                    GreezeManagerService.getService().thawPids(new int[]{pid}, GreezeServiceUtils.GREEZER_MODULE_PRELOAD, "preloadApp receive binder state: uid=" + uid + " pid=" + pid + " tid=" + tid);
                    return;
            }
        }

        public void serviceReady(boolean ready) throws RemoteException {
        }

        public void thawedByOther(int uid, int pid, int module) throws RemoteException {
        }
    }

    public void dump(PrintWriter pw) {
        pw.println("preload enable:" + PreloadAppControllerImpl.isEnable());
        pw.println("preload debug:" + PreloadAppControllerImpl.DEBUG);
        pw.println("preload display:" + sPreloadDisplay);
        for (Integer uid : sPreloadUidInfos.keySet()) {
            pw.println("already preload uid=" + uid);
        }
    }
}
