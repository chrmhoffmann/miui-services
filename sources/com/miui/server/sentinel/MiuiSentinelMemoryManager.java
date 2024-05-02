package com.miui.server.sentinel;

import android.content.Context;
import android.os.FileUtils;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.SystemProperties;
import android.util.Slog;
import com.android.server.am.ActivityManagerService;
import com.android.server.am.ProcessRecord;
import com.android.server.am.ProcessUtils;
import com.android.server.am.ScoutMemoryError;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import miui.mqsas.sdk.MQSEventManagerDelegate;
import miui.mqsas.sdk.event.HeapLeakEvent;
import miui.mqsas.sdk.event.RssLeakEvent;
/* loaded from: classes.dex */
public class MiuiSentinelMemoryManager {
    private static final int END_TRACK = 4;
    private static final int END_TRACK_SIGNAL = 51;
    private static final String HANDLER_NAME = "sentineMemoryWork";
    private static final String MEMLEAK_DIR = "/data/miuilog/stability/memleak/heapleak";
    private static final int REPORT_NATIVEHEAP_LEAKTOMQS = 6;
    private static final int RESUME_LEAK = 5;
    private static final int START_FD_TRACK = 2;
    private static final int START_THREAD_TRACK = 3;
    private static final int START_TRACK = 1;
    private static final int START_TRACK_SIGNAL = 50;
    private static final String SYSPROP_ENABLE_TRACK_MALLOC = "persist.track.malloc.enable";
    private static final int SYSTEM_SERVER_MAX_JAVAHEAP = 400000;
    private static final int SYSTEM_SERVER_MAX_NATIVEHEAP = 300000;
    private static final String TAG = "MiuiSentinelMemoryManager";
    private static final int TOTAL_RSS_LIMIT = 4194304;
    private static MiuiSentinelMemoryManager miuiSentinelMemoryManager;
    private ConcurrentHashMap<String, Integer> EventList = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, NativeHeapUsageInfo> TrackList = new ConcurrentHashMap<>();
    private Context mContext;
    private HandlerThread mMiuiSentineThread;
    private volatile MiuiSentineHandler mSentineHandler;
    private ActivityManagerService mService;
    public static final boolean DEBUG = SystemProperties.getBoolean("debug.sys.mss", false);
    private static final String SYSPROP_ENABLE_SENTINEL_MEMORY_MONITOR = "persist.sys.debug.enable_sentinel_memory_monitor";
    public static final boolean ENABLE_SENTINEL_MEMORY_MONITOR = SystemProperties.getBoolean(SYSPROP_ENABLE_SENTINEL_MEMORY_MONITOR, false);
    private static final String SYSPROP_ENABLE_MQS_REPORT = "persist.sys.debug.enable_mqs_report";
    public static final boolean ENABLE_MQS_REPORT = SystemProperties.getBoolean(SYSPROP_ENABLE_MQS_REPORT, false);
    private static final HashSet<String> DIALOG_APP_LIST = new HashSet<String>() { // from class: com.miui.server.sentinel.MiuiSentinelMemoryManager.1
        {
            add("com.miui.miwallpaper");
            add("com.android.systemui");
            add("com.example.memleaktesttool");
        }
    };

    /* loaded from: classes.dex */
    enum Action {
        START_TRACK,
        REPORT_TRACK
    }

    /* loaded from: classes.dex */
    public final class MiuiSentineHandler extends Handler {
        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public MiuiSentineHandler(Looper looper) {
            super(looper, null);
            MiuiSentinelMemoryManager.this = r1;
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            if (MiuiSentinelMemoryManager.DEBUG) {
                Slog.d(MiuiSentinelMemoryManager.TAG, "information has been received and message.what = " + msg.what + "msg.when = " + msg.getWhen() + "System.currentTimeMillis = " + System.currentTimeMillis());
            }
            switch (msg.what) {
                case 1:
                    Slog.d(MiuiSentinelMemoryManager.TAG, "begin stack track");
                    NativeHeapUsageInfo nativeheapinfo = (NativeHeapUsageInfo) msg.obj;
                    MiuiSentinelMemoryManager.this.handlerTriggerTrack(nativeheapinfo.getPid(), Action.START_TRACK);
                    sendMessage(nativeheapinfo, 4, 30000L);
                    return;
                case 2:
                    Slog.d(MiuiSentinelMemoryManager.TAG, "begin fd track");
                    return;
                case 3:
                    Slog.d(MiuiSentinelMemoryManager.TAG, "begin thread track");
                    return;
                case 4:
                    Slog.d(MiuiSentinelMemoryManager.TAG, "end stack track, ready report event");
                    NativeHeapUsageInfo heapUsageInfo = (NativeHeapUsageInfo) msg.obj;
                    MiuiSentinelMemoryManager.this.handlerTriggerTrack(heapUsageInfo.getPid(), Action.REPORT_TRACK);
                    return;
                case 5:
                    if (msg.obj instanceof RssUsageInfo) {
                        RssUsageInfo rssUsageInfo = (RssUsageInfo) msg.obj;
                        int adj = ProcessUtils.getCurAdjByPid(rssUsageInfo.getPid());
                        MiuiSentinelMemoryManager.this.resumeMemLeak(rssUsageInfo.getPid(), adj, rssUsageInfo.getRssSize(), rssUsageInfo.getName(), "RSS is too large, leak occurred", rssUsageInfo.getMaxIncrease());
                        return;
                    } else if (msg.obj instanceof NativeHeapUsageInfo) {
                        NativeHeapUsageInfo nativeHeapUsageInfo = (NativeHeapUsageInfo) msg.obj;
                        int adj2 = ProcessUtils.getCurAdjByPid(nativeHeapUsageInfo.getPid());
                        MiuiSentinelMemoryManager.this.resumeMemLeak(nativeHeapUsageInfo.getPid(), adj2, nativeHeapUsageInfo.getNativeHeapSize(), nativeHeapUsageInfo.getName(), "Native Heap is too large, leak occurred", "NativeHeap");
                        return;
                    } else if (msg.obj instanceof JavaHeapUsageInfo) {
                        JavaHeapUsageInfo javaHeapUsageInfo = (JavaHeapUsageInfo) msg.obj;
                        int adj3 = ProcessUtils.getCurAdjByPid(javaHeapUsageInfo.getPid());
                        MiuiSentinelMemoryManager.this.resumeMemLeak(javaHeapUsageInfo.getPid(), adj3, javaHeapUsageInfo.getJavaHeapSize(), javaHeapUsageInfo.getName(), "Java Heap is too large, leak occurred", "JavaHeap");
                        return;
                    } else {
                        return;
                    }
                case 6:
                    NativeHeapUsageInfo heapInfo = (NativeHeapUsageInfo) msg.obj;
                    String subject = heapInfo.getName() + "Native Heap Leak";
                    Slog.d(MiuiSentinelMemoryManager.TAG, "ready report event, heapinfo ：" + heapInfo.toString());
                    MiuiSentinelMemoryManager.this.reportHeapLeakEvent(18, subject, heapInfo.getStackTrace(), heapInfo.getPid(), heapInfo.getName(), heapInfo.getNativeHeapSize());
                    sendMessage(heapInfo, 5, 0L);
                    return;
                default:
                    Slog.d(MiuiSentinelMemoryManager.TAG, "Unknown message");
                    return;
            }
        }

        private void sendMessage(NativeHeapUsageInfo nativeHeapUsageInfo, int number, long timeout) {
            Message message = MiuiSentinelMemoryManager.miuiSentinelMemoryManager.mSentineHandler.obtainMessage();
            message.what = number;
            message.obj = nativeHeapUsageInfo;
            MiuiSentinelMemoryManager.miuiSentinelMemoryManager.mSentineHandler.sendMessageDelayed(message, timeout);
            if (MiuiSentinelMemoryManager.DEBUG) {
                Slog.d(MiuiSentinelMemoryManager.TAG, "msg.what = " + message.what + "send message time = " + System.currentTimeMillis());
            }
        }
    }

    public static MiuiSentinelMemoryManager getInstance() {
        if (miuiSentinelMemoryManager == null) {
            miuiSentinelMemoryManager = new MiuiSentinelMemoryManager();
        }
        return miuiSentinelMemoryManager;
    }

    public void init(ActivityManagerService mService, Context mContext) {
        this.mService = mService;
        this.mContext = mContext;
        ScoutMemoryError.getInstance().init(mService, mContext);
    }

    private MiuiSentinelMemoryManager() {
        HandlerThread handlerThread = new HandlerThread(HANDLER_NAME);
        this.mMiuiSentineThread = handlerThread;
        handlerThread.start();
        this.mSentineHandler = new MiuiSentineHandler(this.mMiuiSentineThread.getLooper());
        Slog.d(TAG, "MiuiSentinelMemoryManager init");
    }

    public boolean filterMessages(SocketPacket socketPacket) {
        String procname = socketPacket.getProcess_name();
        String type = socketPacket.getEvent_type();
        StringBuilder sb = new StringBuilder();
        sb.append(type).append("#").append(procname);
        if (DEBUG) {
            Slog.e(TAG, "filtermessages item:" + sb.toString());
        }
        if (this.EventList.get(sb.toString()) == null) {
            this.EventList.put(sb.toString(), 1);
            return false;
        } else if (this.EventList.get(sb.toString()).intValue() >= 3) {
            return true;
        } else {
            this.EventList.put(sb.toString(), Integer.valueOf(this.EventList.get(sb.toString()).intValue() + 1));
            return false;
        }
    }

    public void judgmentRssLeakException(SocketPacket socketPacket) {
        RssUsageInfo rssUsageInfo = getRssinfo(socketPacket);
        long rssSize = rssUsageInfo.getRssSize();
        double percentage = (rssSize * 1.0d) / 4194304.0d;
        Slog.w(TAG, "percentage rate：" + percentage);
        if (percentage >= 0.6d) {
            StringBuilder sb = new StringBuilder();
            sb.append("RSS Leak in pid (" + rssUsageInfo.getPid() + ")" + rssUsageInfo.getName());
            sb.append("RSS size:" + rssUsageInfo.getRssSize() + "Max Increase:" + rssUsageInfo.getMaxIncrease());
            removeEventList(socketPacket);
            if (isEnableMqsReport()) {
                reportRssLeakEvent(416, sb.toString(), rssUsageInfo);
            }
            sendMessage(rssUsageInfo, 5);
        }
    }

    public void judgmentNativeHeapLeakException(SocketPacket socketPacket) {
        NativeHeapUsageInfo nativeHeapUsageInfo = getNativeHeapinfo(socketPacket);
        if (nativeHeapUsageInfo.getName().equals("system_server")) {
            if (nativeHeapUsageInfo.getNativeHeapSize() > 300000) {
                StringBuilder sb = new StringBuilder();
                sb.append(nativeHeapUsageInfo.getName()).append("#").append(nativeHeapUsageInfo.getPid());
                this.TrackList.put(sb.toString(), nativeHeapUsageInfo);
                sendMessage(nativeHeapUsageInfo, 1);
            }
        } else if (MiuiSentinelService.getAppNativeheapWhiteList().get(nativeHeapUsageInfo.getName()) != null) {
            long limit = MiuiSentinelService.getAppNativeheapWhiteList().get(nativeHeapUsageInfo.getName()).intValue();
            Slog.e(TAG, "app limit: " + limit);
            if (socketPacket.getGrowsize() > limit) {
                StringBuilder sb2 = new StringBuilder();
                sb2.append(nativeHeapUsageInfo.getName()).append("#").append(nativeHeapUsageInfo.getPid());
                this.TrackList.put(sb2.toString(), nativeHeapUsageInfo);
                sendMessage(nativeHeapUsageInfo, 1);
            }
        }
    }

    public void judgmentJavaHeapLeakException(SocketPacket socketPacket) {
        JavaHeapUsageInfo javaHeapUsageInfo = getJavaHeapinfo(socketPacket);
        if (javaHeapUsageInfo.getName().equals("system_server")) {
            if (socketPacket.getGrowsize() > 400000) {
                sendMessage(javaHeapUsageInfo, 5);
                return;
            }
            return;
        }
        long limit = MiuiSentinelService.getAppJavaheapWhiteList().get(javaHeapUsageInfo.getName()).intValue();
        if (socketPacket.getGrowsize() > limit) {
            sendMessage(javaHeapUsageInfo, 5);
        }
    }

    private void reportRssLeakEvent(int type, String subject, RssUsageInfo rssUsageInfo) {
        RssLeakEvent event = new RssLeakEvent();
        event.setType(type);
        event.setPid(rssUsageInfo.getPid());
        event.setPackageName(rssUsageInfo.getName());
        event.setProcessName(rssUsageInfo.getName());
        event.setTimeStamp(System.currentTimeMillis());
        event.setSummary(subject);
        event.setDetails(subject);
        event.setmGrowSize(rssUsageInfo.getRssSize());
        event.setmMaxIncrease(rssUsageInfo.getMaxIncrease());
        MQSEventManagerDelegate.getInstance().reportRssLeakEvent(event);
    }

    public void reportHeapLeakEvent(int type, String subject, String stacktrace, int pid, String procname, long growsize) {
        HeapLeakEvent event = new HeapLeakEvent();
        event.setType(type);
        event.setPid(pid);
        event.setPackageName(procname);
        event.setProcessName(procname);
        event.setTimeStamp(System.currentTimeMillis());
        event.setSummary(subject);
        event.setDetails(stacktrace);
        event.setmGrowSize(growsize);
        MQSEventManagerDelegate.getInstance().reportHeapLeakEvent(event);
    }

    public RssUsageInfo getRssinfo(SocketPacket socketPacket) {
        RssUsageInfo info = new RssUsageInfo();
        String[] split = socketPacket.getProcess_name().split("#");
        info.setName(split[0]);
        info.setPid(Integer.parseInt(split[1]));
        info.setRssSize(socketPacket.getGrowsize());
        socketPacket.getData();
        return info;
    }

    public NativeHeapUsageInfo getNativeHeapinfo(SocketPacket socketPacket) {
        NativeHeapUsageInfo info = new NativeHeapUsageInfo();
        String[] split = socketPacket.getProcess_name().split("#");
        info.setName(split[0]);
        info.setPid(Integer.parseInt(split[1]));
        info.setNativeHeapSize(socketPacket.getGrowsize());
        return info;
    }

    public JavaHeapUsageInfo getJavaHeapinfo(SocketPacket socketPacket) {
        JavaHeapUsageInfo info = new JavaHeapUsageInfo();
        String[] split = socketPacket.getProcess_name().split("#");
        info.setName(split[0]);
        info.setPid(Integer.parseInt(split[1]));
        info.setJavaHeapSize(socketPacket.getGrowsize());
        info.setStackTrace("");
        return info;
    }

    public void resumeMemLeak(int pid, int adj, long size, String name, String reason, String type) {
        if (!MiuiSentinelUtils.isLaboratoryTest()) {
            if (DIALOG_APP_LIST.contains(name)) {
                ProcessRecord app = ProcessUtils.getProcessRecordByPid(pid);
                ScoutMemoryError.getInstance().showAppMemoryErrorDialog(app, name + "(" + pid + ") used" + size + "kB" + reason);
            }
        } else if (pid > 0 && Process.getThreadGroupLeader(pid) == pid) {
            if (adj == -900 && pid == Process.myPid()) {
                Slog.e(TAG, "system_server(" + pid + ") use " + type + size + "kb too many occurring" + reason);
                throw new RuntimeException("system_server (" + pid + ") used " + size + "kB " + reason);
            }
            if (this.mService == null || adj <= -900 || adj >= 1001) {
                Slog.e(TAG, "Kill " + name + "(" + pid + "), Used " + size + "kB " + reason);
                Process.killProcess(pid);
                return;
            }
            ProcessRecord app2 = ProcessUtils.getProcessRecordByPid(pid);
            String appReason = name + "(" + pid + ") used" + size + "kB" + reason;
            boolean KillAction = false;
            if (ScoutMemoryError.getInstance().scheduleCrashApp(app2, appReason)) {
                KillAction = true;
            }
            if (!KillAction) {
                appReason = "Kill" + name + "(" + pid + ") used" + size + "kB" + reason;
                Process.killProcess(pid);
            }
            Slog.e(TAG, appReason);
        } else {
            Slog.e(TAG, name + "(" + pid + ") is invalid");
        }
    }

    private void sendMessage(RssUsageInfo rssUsageInfo, int number) {
        Message message = miuiSentinelMemoryManager.mSentineHandler.obtainMessage();
        message.what = number;
        message.obj = rssUsageInfo;
        miuiSentinelMemoryManager.mSentineHandler.sendMessage(message);
        if (DEBUG) {
            Slog.d(TAG, "msg.what = " + message.what + "msg.obj = " + message.obj.toString());
        }
    }

    public void sendMessage(NativeHeapUsageInfo nativeHeapUsageInfo, int number) {
        Message message = miuiSentinelMemoryManager.mSentineHandler.obtainMessage();
        message.what = number;
        message.obj = nativeHeapUsageInfo;
        miuiSentinelMemoryManager.mSentineHandler.sendMessage(message);
        if (DEBUG) {
            Slog.d(TAG, "msg.what = " + message.what + "msg.obj = " + message.obj.toString());
        }
    }

    private void sendMessage(JavaHeapUsageInfo javaHeapUsageInfo, int number) {
        Message message = miuiSentinelMemoryManager.mSentineHandler.obtainMessage();
        message.what = number;
        message.obj = javaHeapUsageInfo;
        miuiSentinelMemoryManager.mSentineHandler.sendMessage(message);
        if (DEBUG) {
            Slog.d(TAG, "msg.what = " + message.what + "msg.obj = " + message.obj.toString());
        }
    }

    public boolean isEnableSentinelMemoryMonitor() {
        return ENABLE_SENTINEL_MEMORY_MONITOR;
    }

    public boolean isEnableMqsReport() {
        return ENABLE_MQS_REPORT;
    }

    public void removeEventList(SocketPacket socketPacket) {
        StringBuilder sb = new StringBuilder();
        String procname = socketPacket.getProcess_name();
        String type = socketPacket.getEvent_type();
        sb.append(type).append("#").append(procname);
        this.EventList.remove(sb.toString());
    }

    public boolean handlerTriggerTrack(int pid, Action action) {
        if (pid <= 0) {
            Slog.e(TAG, "Failed to enable Track! pid is invalid");
            return false;
        }
        if (MiuiSentinelUtils.isEnaleTrack()) {
            String mapsPath = "/proc/" + String.valueOf(pid) + "/maps";
            if (isLibraryExist(mapsPath)) {
                switch (AnonymousClass2.$SwitchMap$com$miui$server$sentinel$MiuiSentinelMemoryManager$Action[action.ordinal()]) {
                    case 1:
                        Slog.e(TAG, "begin pid(" + pid + ") Track");
                        Process.sendSignal(pid, 50);
                        break;
                    case 2:
                        Slog.e(TAG, "report pid(" + pid + ") Track");
                        Process.sendSignal(pid, END_TRACK_SIGNAL);
                        break;
                }
                Slog.e(TAG, "enable track malloc sucess!");
                return true;
            }
        }
        return false;
    }

    /* renamed from: com.miui.server.sentinel.MiuiSentinelMemoryManager$2 */
    /* loaded from: classes.dex */
    public static /* synthetic */ class AnonymousClass2 {
        static final /* synthetic */ int[] $SwitchMap$com$miui$server$sentinel$MiuiSentinelMemoryManager$Action;

        static {
            int[] iArr = new int[Action.values().length];
            $SwitchMap$com$miui$server$sentinel$MiuiSentinelMemoryManager$Action = iArr;
            try {
                iArr[Action.START_TRACK.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$miui$server$sentinel$MiuiSentinelMemoryManager$Action[Action.REPORT_TRACK.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
        }
    }

    private boolean isLibraryExist(String path) {
        BufferedReader reader;
        String mapsInfo;
        StringBuilder libraryName = new StringBuilder();
        libraryName.append("lib" + SystemProperties.get(SYSPROP_ENABLE_TRACK_MALLOC) + ".so");
        try {
            reader = new BufferedReader(new FileReader(path));
        } catch (IOException e) {
            e.printStackTrace();
        }
        do {
            mapsInfo = reader.readLine();
            if (mapsInfo == null) {
                reader.close();
                Slog.e(TAG, path + " not found track's library: " + libraryName.toString());
                return false;
            }
        } while (!mapsInfo.contains(libraryName));
        Slog.e(TAG, path + " found track's library");
        reader.close();
        return true;
    }

    public void outPutTrackLog(TrackPacket trackPacket) throws IOException {
        File trackDir = new File(MEMLEAK_DIR);
        if (!trackDir.exists()) {
            if (!trackDir.mkdirs()) {
                Slog.e(TAG, "cannot create memleak Dir", new Throwable());
            }
            FileUtils.setPermissions(trackDir, 508, -1, -1);
        }
        String dirSuffix = MiuiSentinelUtils.getFormatDateTime(System.currentTimeMillis());
        String dirname = trackPacket.getPid() + "_" + trackPacket.getProcess_name() + "_" + dirSuffix;
        File exceptionDir = new File(trackDir, dirname);
        if (!exceptionDir.exists()) {
            if (!exceptionDir.mkdirs()) {
                Slog.e(TAG, "cannot create exceptionDir", new Throwable());
            }
            FileUtils.setPermissions(exceptionDir, 508, -1, -1);
        }
        String filename = trackPacket.getPid() + "_" + trackPacket.getProcess_name() + "_" + dirSuffix + "info.txt";
        File trackfile = new File(exceptionDir, filename);
        if (!trackfile.exists()) {
            if (!trackfile.createNewFile()) {
                Slog.e(TAG, "cannot create leakfile", new Throwable());
            }
            FileUtils.setPermissions(trackfile.getAbsolutePath(), 508, -1, -1);
        }
        try {
            FileWriter writers = new FileWriter(trackfile, true);
            writers.write(trackPacket.getData());
            writers.close();
        } catch (IOException e) {
            Slog.w(TAG, "Unable to write Track Stack to file", new Throwable());
        }
    }

    public ConcurrentHashMap<String, NativeHeapUsageInfo> getTrackList() {
        return this.TrackList;
    }
}
