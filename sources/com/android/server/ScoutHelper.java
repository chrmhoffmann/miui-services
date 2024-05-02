package com.android.server;

import android.os.FileUtils;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.perfdebug.MessageMonitor;
import android.server.am.SplitScreenReporter;
import android.text.TextUtils;
import android.util.Slog;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import miui.mqsas.IMQSNative;
import miui.os.Build;
/* loaded from: classes.dex */
public class ScoutHelper {
    public static final String ACTION_CAT = "cat";
    public static final String ACTION_CP = "cp";
    public static final String ACTION_DMABUF_DUMP = "dmabuf_dump";
    public static final String ACTION_DUMPSYS = "dumpsys";
    public static final String ACTION_LOGCAT = "logcat";
    public static final String ACTION_MV = "mv";
    public static final String ACTION_PS = "ps";
    public static final String ACTION_TOP = "top";
    public static final String BINDER_ASYNC_GKI = "MIUI    pending async transaction";
    public static final String BINDER_ASYNC_MIUI = "pending async transaction";
    public static final String BINDER_CONTEXT = "context";
    public static final String BINDER_DUR = "duration:";
    public static final String BINDER_FS_PATH_GKI = "/dev/binderfs/binder_logs/proc/";
    public static final String BINDER_FS_PATH_MIUI = "/dev/binderfs/binder_logs/proc_transaction/";
    public static final int BINDER_FULL_KILL_SCORE_ADJ_MIN = 200;
    public static final String BINDER_INCOMING_GKI = "MIUI    incoming transaction";
    public static final String BINDER_INCOMING_MIUI = "incoming transaction";
    public static final String BINDER_OUTGOING_GKI = "MIUI    outgoing transaction";
    public static final String BINDER_OUTGOING_MIUI = "outgoing transaction";
    public static final int BINDER_WAITTIME_THRESHOLD = 2;
    public static final int CALL_TYPE_APP = 1;
    public static final int CALL_TYPE_SYSTEM = 0;
    private static String CONSOLE_RAMOOPS_0_PATH = null;
    private static String CONSOLE_RAMOOPS_PATH = null;
    private static final boolean DEBUG = false;
    public static final int DEFAULT_RUN_COMMAND_TIMEOUT = 60;
    public static final boolean DISABLE_AOSP_ANR_TRACE_POLICY;
    public static final String FILE_DIR_MQSAS = "/data/mqsas/";
    public static final String FILE_DIR_SCOUT = "scout";
    public static final String FILE_DIR_STABILITY = "/data/miuilog/stability";
    public static final boolean IS_INTERNATIONAL_BUILD;
    public static final int JAVA_PROCESS = 1;
    private static final String MQSASD = "miui.mqsas.IMQSNative";
    public static final String MQS_PSTORE_DIR = "/data/mqsas/temp/pstore/";
    public static final int NATIVE_PROCESS = 2;
    public static final int OOM_SCORE_ADJ_MAX = 1000;
    public static final int OOM_SCORE_ADJ_MIN = -1000;
    public static final boolean PANIC_ANR_D_THREAD;
    public static final boolean SCOUT_BINDER_GKI;
    public static final boolean SYSRQ_ANR_D_THREAD;
    public static final int SYSTEM_ADJ = -900;
    private static final String TAG = "ScoutHelper";
    public static final int UNKNOW_PROCESS = 0;
    private static IMQSNative mDaemon;
    private static SimpleDateFormat sOfflineLogDateFormat;
    public static final boolean ENABLED_SCOUT = SystemProperties.getBoolean("persist.sys.miui_scout_enable", false);
    public static final boolean ENABLED_SCOUT_DEBUG = SystemProperties.getBoolean("persist.sys.miui_scout_debug", false);
    public static final boolean BINDER_FULL_KILL_PROC = SystemProperties.getBoolean("persist.sys.miui_scout_binder_full_kill_process", false);
    public static final boolean PANIC_D_THREAD = SystemProperties.getBoolean("persist.sys.panicOnWatchdog_D_state", false);

    static {
        SYSRQ_ANR_D_THREAD = SystemProperties.getBoolean("persist.sys.sysrqOnAnr_D_state", false) || SystemProperties.getBoolean("persist.sys.panicOnAnr_D_state", false);
        PANIC_ANR_D_THREAD = SystemProperties.getBoolean("persist.sys.panicOnAnr_D_state", false);
        SCOUT_BINDER_GKI = SystemProperties.getBoolean("persist.sys.scout_binder_gki", false);
        DISABLE_AOSP_ANR_TRACE_POLICY = SystemProperties.getBoolean("persist.sys.disable_aosp_anr_policy", false);
        IS_INTERNATIONAL_BUILD = Build.IS_INTERNATIONAL_BUILD;
        CONSOLE_RAMOOPS_PATH = "/sys/fs/pstore/console-ramoops";
        CONSOLE_RAMOOPS_0_PATH = "/sys/fs/pstore/console-ramoops-0";
    }

    private static IMQSNative getmDaemon() {
        if (mDaemon == null) {
            IMQSNative asInterface = IMQSNative.Stub.asInterface(ServiceManager.getService(MQSASD));
            mDaemon = asInterface;
            if (asInterface == null) {
                Slog.e(TAG, "mqsasd not available!");
            }
        }
        return mDaemon;
    }

    /* loaded from: classes.dex */
    public static class Action {
        private List<String> actions = new ArrayList();
        private List<String> params = new ArrayList();
        private List<String> includeFiles = new ArrayList();

        public String toString() {
            return "Action{actions=" + this.actions + ", params=" + this.params + ", includeFiles=" + this.includeFiles + '}';
        }

        public void addActionAndParam(String action, String param) {
            this.actions.add(action);
            if (param == null) {
                param = "";
            }
            this.params.add(param);
        }

        public void addIncludeFile(String path) {
            if (TextUtils.isEmpty(path)) {
                return;
            }
            this.includeFiles.add(path);
        }

        public void addIncludeFiles(List<String> includeFiles) {
            if (includeFiles != null && includeFiles.size() > 0) {
                for (String path : includeFiles) {
                    addIncludeFile(path);
                }
            }
        }

        public void clearIncludeFiles() {
            List<String> list = this.includeFiles;
            if (list != null) {
                list.clear();
            }
        }
    }

    /* loaded from: classes.dex */
    public static class ScoutBinderInfo {
        private StringBuilder mBinderTransInfo;
        private int mCallType;
        private int mFromPid;
        private boolean mHasDThread;
        private int mPid;
        private String mTag;

        public ScoutBinderInfo(int mPid, int mCallType, String mTag) {
            this(mPid, 0, mCallType, mTag);
        }

        public ScoutBinderInfo(int mPid, int mFromPid, int mCallType, String mTag) {
            this.mHasDThread = false;
            StringBuilder sb = new StringBuilder();
            this.mBinderTransInfo = sb;
            this.mPid = mPid;
            this.mFromPid = mFromPid;
            this.mTag = mTag;
            this.mCallType = mCallType;
            sb.append("Binder Tracsaction Info:\n");
        }

        public void setDThreadState(boolean mHasDThread) {
            this.mHasDThread = mHasDThread;
        }

        public boolean getDThreadState() {
            return this.mHasDThread;
        }

        public void addBinderTransInfo(String sInfo) {
            this.mBinderTransInfo.append(sInfo + "\n");
        }

        public String getBinderTransInfo() {
            return this.mBinderTransInfo.toString();
        }

        public int getPid() {
            return this.mPid;
        }

        public int getFromPid() {
            return this.mFromPid;
        }

        public int getCallType() {
            return this.mCallType;
        }

        public String getTag() {
            return this.mTag;
        }
    }

    public static Boolean CheckDState(String tag, int pid) {
        Exception e;
        Throwable th;
        String str;
        String threadState;
        String str2 = "/";
        boolean result = false;
        try {
            String taskPath = "/proc/" + String.valueOf(pid) + "/task";
            File taskDir = new File(taskPath);
            File[] threadDirs = taskDir.listFiles();
            String str3 = "/stat";
            boolean result2 = false;
            String str4 = "/stack";
            try {
                if (threadDirs != null) {
                    int length = threadDirs.length;
                    int i = 0;
                    while (i < length) {
                        File threadDir = threadDirs[i];
                        int i2 = length;
                        File[] threadDirs2 = threadDirs;
                        int i3 = i;
                        String str5 = str4;
                        BufferedReader threadStat = new BufferedReader(new FileReader(taskPath + str2 + threadDir.getName() + str3));
                        String statInfo = threadStat.readLine();
                        String str6 = str3;
                        String threadName = statInfo.substring(statInfo.indexOf(40) + 1, statInfo.indexOf(41));
                        String threadState2 = statInfo.substring(statInfo.indexOf(41) + 2).split("\\s+")[0];
                        if (!threadState2.equals("D") && !threadState2.equals("Z") && !threadState2.equals("T")) {
                            str = str2;
                            threadState = str5;
                            threadStat.close();
                            str4 = threadState;
                            length = i2;
                            threadDirs = threadDirs2;
                            str3 = str6;
                            i = i3 + 1;
                            str2 = str;
                        }
                        Slog.d(tag, "Pid(" + pid + ") have " + threadState2 + " state thead(tid:" + threadDir.getName() + " name:" + threadName + ")");
                        str = str2;
                        threadState = str5;
                        BufferedReader threadStack = new BufferedReader(new FileReader(taskPath + str2 + threadDir.getName() + threadState));
                        while (true) {
                            try {
                                String stackLineInfo = threadStack.readLine();
                                if (stackLineInfo == null) {
                                    break;
                                }
                                Slog.d(tag, stackLineInfo);
                            } catch (Throwable th2) {
                                try {
                                    threadStack.close();
                                } catch (Throwable th3) {
                                    th2.addSuppressed(th3);
                                }
                                throw th2;
                            }
                        }
                        threadStack.close();
                        result2 = true;
                        threadStat.close();
                        str4 = threadState;
                        length = i2;
                        threadDirs = threadDirs2;
                        str3 = str6;
                        i = i3 + 1;
                        str2 = str;
                    }
                    result = result2;
                } else {
                    String stackPath = "/proc/" + String.valueOf(pid) + str4;
                    String statPath = "/proc/" + String.valueOf(pid) + str3;
                    BufferedReader procStat = new BufferedReader(new FileReader(statPath));
                    try {
                        String statInfo2 = procStat.readLine();
                        try {
                            String procName = statInfo2.substring(statInfo2.indexOf(40) + 1, statInfo2.indexOf(41));
                            String procState = statInfo2.substring(statInfo2.indexOf(41) + 2).split("\\s+")[0];
                            if (procState.equals("D") || procState.equals("Z") || procState.equals("T")) {
                                Slog.d(tag, "Pid(" + pid + ") have " + procState + " state in main thread ( name:" + procName + ")");
                                BufferedReader procStack = new BufferedReader(new FileReader(stackPath));
                                while (true) {
                                    String stackLineInfo2 = procStack.readLine();
                                    if (stackLineInfo2 == null) {
                                        break;
                                    }
                                    Slog.d(tag, stackLineInfo2);
                                }
                                procStack.close();
                            }
                            procStat.close();
                            result = false;
                        } catch (Throwable th4) {
                            th = th4;
                            procStat.close();
                            throw th;
                        }
                    } catch (Throwable th5) {
                        th = th5;
                    }
                }
            } catch (Exception e2) {
                e = e2;
                result = false;
                Slog.w(tag, "Failed to read thread stat: " + e.toString());
                return Boolean.valueOf(result);
            }
        } catch (Exception e3) {
            e = e3;
        }
        return Boolean.valueOf(result);
    }

    public static File getBinderLogFile(String tag, int pid) {
        File fileBinderMIUI = new File(BINDER_FS_PATH_MIUI + String.valueOf(pid));
        File fileBinderGKI = new File(BINDER_FS_PATH_GKI + String.valueOf(pid));
        if (fileBinderMIUI.exists()) {
            return fileBinderMIUI;
        }
        if (SCOUT_BINDER_GKI && fileBinderGKI.exists()) {
            return fileBinderGKI;
        }
        Slog.w(tag, "gki binder logfs or miui binder logfs are not exist");
        return null;
    }

    public static boolean addPidtoList(String tag, int pid, ArrayList<Integer> javaProcess, ArrayList<Integer> nativeProcess) {
        int adj = getOomAdjOfPid(tag, pid);
        int isJavaOrNativeProcess = checkIsJavaOrNativeProcess(adj);
        if (isJavaOrNativeProcess == 0) {
            return false;
        }
        if (isJavaOrNativeProcess == 1 && !javaProcess.contains(Integer.valueOf(pid))) {
            javaProcess.add(Integer.valueOf(pid));
            return true;
        } else if (isJavaOrNativeProcess != 2 || nativeProcess.contains(Integer.valueOf(pid))) {
            return false;
        } else {
            nativeProcess.add(Integer.valueOf(pid));
            return true;
        }
    }

    public static boolean checkAsyncBinderCallPidList(int anrToPid, int anrFromPid, ScoutBinderInfo info, ArrayList<Integer> javaProcess, ArrayList<Integer> nativeProcess) {
        int i = anrToPid;
        String str = "from";
        String str2 = ":";
        String str3 = "to";
        boolean isBlockSystem = false;
        String tag = info.getTag();
        Slog.w(tag, "Check async binder info with Broadcast ANR  Pid=" + i + " SystemPid=" + anrFromPid);
        File fileBinderReader = getBinderLogFile(tag, i);
        if (fileBinderReader == null) {
            return false;
        }
        try {
            BufferedReader BinderReader = new BufferedReader(new FileReader(fileBinderReader));
            while (true) {
                String binderTransactionInfo = BinderReader.readLine();
                if (binderTransactionInfo == null) {
                    break;
                } else if (binderTransactionInfo.startsWith(BINDER_ASYNC_MIUI) || binderTransactionInfo.startsWith(BINDER_ASYNC_GKI)) {
                    int fromPid = Integer.parseInt(binderTransactionInfo.split(str3)[0].split(str)[1].split(str2)[0].trim());
                    int fromTid = Integer.parseInt(binderTransactionInfo.split(str3)[0].split(str)[1].split(str2)[1].trim());
                    int toPid = Integer.parseInt(binderTransactionInfo.split(str3)[1].split(str2)[0].trim());
                    String str4 = str;
                    String str5 = str3;
                    Integer.parseInt(binderTransactionInfo.split(str3)[1].split(BINDER_CONTEXT)[0].split(str2)[1].trim());
                    String str6 = str2;
                    double waitTime = Double.valueOf(binderTransactionInfo.split(BINDER_DUR)[1].replace("s", "").trim()).doubleValue();
                    if (fromPid == anrFromPid && toPid == i) {
                        Slog.w(tag, "Binder Info:" + binderTransactionInfo);
                        info.addBinderTransInfo(binderTransactionInfo);
                        if (waitTime > 2.0d && fromPid == fromTid) {
                            isBlockSystem = true;
                        }
                        if (addPidtoList(tag, toPid, javaProcess, nativeProcess)) {
                            checkBinderCallPidList(toPid, info, javaProcess, nativeProcess);
                        }
                    }
                    i = anrToPid;
                    str3 = str5;
                    str2 = str6;
                    str = str4;
                }
            }
            BinderReader.close();
        } catch (Exception e) {
            e.printStackTrace();
            Slog.w(tag, "Read binder Proc async transaction Error:", e);
        }
        return isBlockSystem;
    }

    public static boolean checkBinderCallPidList(int Pid, ScoutBinderInfo info, ArrayList<Integer> javaProcess, ArrayList<Integer> nativeProcess) {
        Exception e;
        Throwable th;
        int fromPid;
        boolean isBlockSystem;
        int fromTid;
        String str;
        int toPid;
        String str2;
        int callType;
        boolean isCheckAppDThread;
        int adj = Pid;
        String str3 = "from";
        String str4 = ":";
        String str5 = "to";
        boolean isBlockSystem2 = false;
        String tag = info.getTag();
        int isJavaOrNativeProcess = info.getCallType();
        boolean isCheckAppDThread2 = isJavaOrNativeProcess == 1 && SYSRQ_ANR_D_THREAD;
        boolean isCheckSystemDThread = isJavaOrNativeProcess == 0 && PANIC_D_THREAD;
        if ((isCheckAppDThread2 || isCheckSystemDThread) && info != null && CheckDState(tag, adj).booleanValue()) {
            info.setDThreadState(true);
        }
        File fileBinderReader = getBinderLogFile(tag, adj);
        if (fileBinderReader == null) {
            return false;
        }
        try {
            BufferedReader BinderReader = new BufferedReader(new FileReader(fileBinderReader));
            while (true) {
                try {
                    String binderTransactionInfo = BinderReader.readLine();
                    if (binderTransactionInfo == null) {
                        boolean isBlockSystem3 = isBlockSystem2;
                        try {
                            BinderReader.close();
                            return isBlockSystem3;
                        } catch (Exception e2) {
                            e = e2;
                            isBlockSystem2 = isBlockSystem3;
                            e.printStackTrace();
                            Slog.w(tag, "Read binder Proc transaction Error:", e);
                            return isBlockSystem2;
                        }
                    }
                    try {
                        try {
                            if (!binderTransactionInfo.startsWith(BINDER_OUTGOING_MIUI)) {
                                try {
                                    if (!binderTransactionInfo.startsWith(BINDER_OUTGOING_GKI)) {
                                        continue;
                                    }
                                } catch (Throwable th2) {
                                    th = th2;
                                    try {
                                        BinderReader.close();
                                        throw th;
                                    }
                                }
                            }
                            Integer.parseInt(binderTransactionInfo.split(str5)[1].split(BINDER_CONTEXT)[0].split(str4)[1].trim());
                            String str6 = str4;
                            double waitTime = Double.valueOf(binderTransactionInfo.split(BINDER_DUR)[1].replace("s", "").trim()).doubleValue();
                            if (fromPid != adj || toPid <= 0 || toPid == adj) {
                                isCheckAppDThread = isCheckAppDThread2;
                            } else {
                                Slog.w(tag, "Binder Info:" + binderTransactionInfo);
                                info.addBinderTransInfo(binderTransactionInfo);
                                int adj2 = getOomAdjOfPid(tag, toPid);
                                int isJavaOrNativeProcess2 = checkIsJavaOrNativeProcess(adj2);
                                if (isJavaOrNativeProcess2 == 0) {
                                    adj = Pid;
                                    isBlockSystem2 = isBlockSystem;
                                    str3 = str;
                                    str5 = str2;
                                    isJavaOrNativeProcess = callType;
                                    str4 = str6;
                                } else {
                                    isCheckAppDThread = isCheckAppDThread2;
                                    if (isJavaOrNativeProcess2 == 1) {
                                        try {
                                            if (!javaProcess.contains(Integer.valueOf(toPid))) {
                                                boolean isBlockSystem4 = (adj2 == -900 && fromPid == fromTid && waitTime > 2.0d) ? true : isBlockSystem;
                                                try {
                                                    javaProcess.add(Integer.valueOf(toPid));
                                                    checkBinderCallPidList(toPid, info, javaProcess, nativeProcess);
                                                    isBlockSystem2 = isBlockSystem4;
                                                    adj = Pid;
                                                    str3 = str;
                                                    str5 = str2;
                                                    isJavaOrNativeProcess = callType;
                                                    str4 = str6;
                                                    isCheckAppDThread2 = isCheckAppDThread;
                                                } catch (Throwable th3) {
                                                    th = th3;
                                                    BinderReader.close();
                                                    throw th;
                                                }
                                            }
                                        } catch (Throwable th4) {
                                            th = th4;
                                        }
                                    }
                                    if (isJavaOrNativeProcess2 == 2 && !nativeProcess.contains(Integer.valueOf(toPid))) {
                                        boolean isBlockSystem5 = (fromPid != fromTid || waitTime <= 2.0d) ? isBlockSystem : true;
                                        try {
                                            nativeProcess.add(Integer.valueOf(toPid));
                                            checkBinderCallPidList(toPid, info, javaProcess, nativeProcess);
                                            isBlockSystem2 = isBlockSystem5;
                                            adj = Pid;
                                            str3 = str;
                                            str5 = str2;
                                            isJavaOrNativeProcess = callType;
                                            str4 = str6;
                                            isCheckAppDThread2 = isCheckAppDThread;
                                        } catch (Throwable th5) {
                                            th = th5;
                                            BinderReader.close();
                                            throw th;
                                        }
                                    }
                                }
                            }
                            isBlockSystem2 = isBlockSystem;
                            adj = Pid;
                            str3 = str;
                            str5 = str2;
                            isJavaOrNativeProcess = callType;
                            str4 = str6;
                            isCheckAppDThread2 = isCheckAppDThread;
                        } catch (Throwable th6) {
                            th = th6;
                        }
                        fromTid = Integer.parseInt(binderTransactionInfo.split(str5)[0].split(str3)[1].split(str4)[1].trim());
                        str = str3;
                        toPid = Integer.parseInt(binderTransactionInfo.split(str5)[1].split(str4)[0].trim());
                        str2 = str5;
                        callType = isJavaOrNativeProcess;
                    } catch (Throwable th7) {
                        th = th7;
                    }
                    fromPid = Integer.parseInt(binderTransactionInfo.split(str5)[0].split(str3)[1].split(str4)[0].trim());
                    isBlockSystem = isBlockSystem2;
                } catch (Throwable th8) {
                    th = th8;
                }
            }
        } catch (Exception e3) {
            e = e3;
        }
    }

    public static void checkBinderThreadFull(int Pid, ScoutBinderInfo info, TreeMap<Integer, Integer> inPidMap, ArrayList<Integer> javaProcess, ArrayList<Integer> nativeProcess) {
        Exception e;
        Throwable th;
        String str;
        TreeMap<Integer, Integer> treeMap = inPidMap;
        String str2 = "from";
        String str3 = ":";
        String tag = info.getTag();
        File fileBinderReader = getBinderLogFile(tag, Pid);
        if (fileBinderReader == null) {
            return;
        }
        try {
            BufferedReader BinderReader = new BufferedReader(new FileReader(fileBinderReader));
            while (true) {
                try {
                    try {
                        String binderTransactionInfo = BinderReader.readLine();
                        if (binderTransactionInfo != null) {
                            if (binderTransactionInfo.startsWith(BINDER_INCOMING_MIUI) || binderTransactionInfo.startsWith(BINDER_INCOMING_GKI)) {
                                int fromPid = Integer.parseInt(binderTransactionInfo.split("to")[0].split(str2)[1].split(str3)[0].trim());
                                Integer.parseInt(binderTransactionInfo.split("to")[0].split(str2)[1].split(str3)[1].trim());
                                Integer.parseInt(binderTransactionInfo.split("to")[1].split(str3)[0].trim());
                                Integer.parseInt(binderTransactionInfo.split("to")[1].split(BINDER_CONTEXT)[0].split(str3)[1].trim());
                                Double.valueOf(binderTransactionInfo.split(BINDER_DUR)[1].replace("s", "").trim()).doubleValue();
                                String str4 = str2;
                                Slog.w(tag, "Binder Info:" + binderTransactionInfo);
                                try {
                                    info.addBinderTransInfo(binderTransactionInfo);
                                    if (fromPid < 1) {
                                        str = str3;
                                    } else if (treeMap.containsKey(Integer.valueOf(fromPid))) {
                                        str = str3;
                                        treeMap.put(Integer.valueOf(fromPid), Integer.valueOf(treeMap.get(Integer.valueOf(fromPid)).intValue() + 1));
                                    } else {
                                        str = str3;
                                        treeMap.put(Integer.valueOf(fromPid), 1);
                                        try {
                                            addPidtoList(tag, fromPid, javaProcess, nativeProcess);
                                        } catch (Throwable th2) {
                                            th = th2;
                                            Throwable th3 = th;
                                            BinderReader.close();
                                            throw th3;
                                        }
                                    }
                                    treeMap = inPidMap;
                                    str2 = str4;
                                    str3 = str;
                                } catch (Throwable th4) {
                                    th = th4;
                                    Throwable th32 = th;
                                    BinderReader.close();
                                    throw th32;
                                }
                            }
                        } else {
                            BinderReader.close();
                            return;
                        }
                    } catch (Exception e2) {
                        e = e2;
                        e.printStackTrace();
                        Slog.w(tag, "Read binder Proc transaction Error:", e);
                        return;
                    }
                } catch (Throwable th5) {
                    th = th5;
                }
            }
        } catch (Exception e3) {
            e = e3;
        }
    }

    public static String resumeBinderThreadFull(String tag, TreeMap<Integer, Integer> inPidMap) {
        if (ENABLED_SCOUT_DEBUG) {
            Slog.d(tag, "Debug: resumeBinderFull");
        }
        List<Map.Entry<Integer, Integer>> list = new ArrayList<>(inPidMap.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<Integer, Integer>>() { // from class: com.android.server.ScoutHelper.1
            public int compare(Map.Entry<Integer, Integer> o1, Map.Entry<Integer, Integer> o2) {
                return o2.getValue().intValue() - o1.getValue().intValue();
            }
        });
        StringBuilder minfo = new StringBuilder();
        minfo.append("Incoming Binder Procsss Info:");
        boolean isKilled = false;
        for (Map.Entry<Integer, Integer> mapping : list) {
            int inPid = mapping.getKey().intValue();
            int count = mapping.getValue().intValue();
            int oomAdj = getOomAdjOfPid(tag, inPid);
            if (BINDER_FULL_KILL_PROC && oomAdj >= 200 && !isKilled && inPid > 0) {
                Slog.w(tag, "Pid(" + inPid + ") adj(" + oomAdj + ") is Killed, Because it use " + count + " binder thread of System_server");
                Process.killProcess(inPid);
                isKilled = true;
            }
            minfo.append("\nPid " + mapping.getKey() + "(adj : " + oomAdj + ") count " + mapping.getValue());
        }
        Slog.d(tag, minfo.toString());
        return minfo.toString();
    }

    public static void printfProcBinderInfo(int Pid, String tag) {
        File fileBinderReader = getBinderLogFile(TAG, Pid);
        if (fileBinderReader == null) {
            return;
        }
        Slog.w(tag, "Pid " + Pid + " Binder Info:");
        try {
            BufferedReader BinderReader = new BufferedReader(new FileReader(fileBinderReader));
            while (true) {
                String binderTransactionInfo = BinderReader.readLine();
                if (binderTransactionInfo != null) {
                    if (SCOUT_BINDER_GKI) {
                        if (binderTransactionInfo.startsWith("MIUI")) {
                            Slog.w(tag, binderTransactionInfo);
                        }
                    } else {
                        Slog.w(tag, binderTransactionInfo);
                    }
                } else {
                    BinderReader.close();
                    return;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Slog.w(TAG, "Read binder Proc transaction Error:", e);
        }
    }

    public static int checkIsJavaOrNativeProcess(int adj) {
        if (adj == -1000) {
            return 2;
        }
        return (adj < -900 || adj > 1000) ? 0 : 1;
    }

    public static int getOomAdjOfPid(String tag, int Pid) {
        int adj = -1000;
        try {
            BufferedReader adjReader = new BufferedReader(new FileReader("/proc/" + String.valueOf(Pid) + "/oom_score_adj"));
            adj = Integer.parseInt(adjReader.readLine());
            adjReader.close();
        } catch (Exception e) {
            Slog.w(tag, "Check is java or native process Error:", e);
        }
        return adj;
    }

    public static boolean isEnabelPanicDThread(String tag) {
        return PANIC_D_THREAD && isDebugpolicyed(tag);
    }

    public static boolean isDebugpolicyed(String tag) {
        String dp = SystemProperties.get("ro.boot.dp", "unknown");
        if (dp.equals("0xB") || dp.equals(SplitScreenReporter.ACTION_ENTER_SPLIT) || dp.equals(SplitScreenReporter.ACTION_EXIT_SPLIT)) {
            Slog.i(tag, "this device has falshed Debugpolicy");
            return true;
        }
        Slog.i(tag, "this device didn't flash Debugpolicy");
        return false;
    }

    public static void doSysRqInterface(char c) {
        try {
            FileWriter sysrq_trigger = new FileWriter("/proc/sysrq-trigger");
            sysrq_trigger.write(c);
            sysrq_trigger.close();
        } catch (IOException e) {
            Slog.w(TAG, "Failed to write to /proc/sysrq-trigger", e);
        }
    }

    public static void runCommand(String action, String params, int timeout) {
        int cmd_timeout;
        Slog.e(TAG, "runCommand action " + action + " params " + params);
        IMQSNative mClient = getmDaemon();
        if (mClient == null) {
            Slog.e(TAG, "runCommand no mqsasd!");
        } else if (action == null || params == null) {
            Slog.e(TAG, "runCommand Wrong parameters!");
        } else {
            if (timeout < 60) {
                cmd_timeout = 60;
            } else {
                cmd_timeout = timeout;
            }
            try {
                int result = mClient.runCommand(action, params, cmd_timeout);
                if (result < 0) {
                    Slog.e(TAG, "runCommanxd Fail result = " + result);
                }
                Slog.e(TAG, "runCommanxd result = " + result);
            } catch (Exception e) {
                Slog.e(TAG, "runCommanxd Exception " + e.toString());
                e.printStackTrace();
            }
        }
    }

    public static void captureLog(String type, String headline, List<String> actions, List<String> params, boolean offline, int id, boolean upload, String where, List<String> includeFiles) {
        String where2;
        IMQSNative mClient = getmDaemon();
        if (mClient == null) {
            Slog.e(TAG, "CaptureLog no mqsasd!");
        } else if (TextUtils.isEmpty(type) || TextUtils.isEmpty(headline)) {
            Slog.e(TAG, "CaptureLog type or headline is null!");
        } else {
            if (where == null) {
                Slog.d(TAG, "CaptureLog where is null!");
                where2 = "";
            } else {
                where2 = where;
            }
            try {
                mClient.captureLog(type, headline, actions, params, offline, id, upload, where2, includeFiles, false);
            } catch (RemoteException e) {
                Slog.e(TAG, "CaptureLog failed!", e);
            } catch (Exception e2) {
                Slog.e(TAG, "CaptureLog failed! unknown error", e2);
            }
        }
    }

    public static void dumpOfflineLog(String reason, Action action, String type, String where) {
        try {
            if (sOfflineLogDateFormat == null) {
                sOfflineLogDateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS");
            }
            String formattedDate = sOfflineLogDateFormat.format(new Date());
            String fileDir = where + File.separator;
            String fileName = reason + "_" + formattedDate;
            File offlineLogDir = new File(fileDir);
            if (!offlineLogDir.exists() && !offlineLogDir.mkdirs()) {
                Slog.e(TAG, "Cannot create " + fileDir);
                return;
            }
            Slog.e(TAG, "dumpOfflineLog reason:" + reason + " action=" + action + " type=" + type + " where=" + where);
            captureLog(type, fileName, action.actions, action.params, true, 1, false, fileDir, action.includeFiles);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void copyRamoopsFileToMqs() {
        File ramoopsFile = new File(CONSOLE_RAMOOPS_PATH);
        File ramoops_0File = new File(CONSOLE_RAMOOPS_0_PATH);
        if ((ramoopsFile.exists() || ramoops_0File.exists()) && SystemProperties.getInt("sys.system_server.start_count", 1) == 1) {
            File lastKmsgFile = ramoopsFile.exists() ? ramoopsFile : ramoops_0File;
            try {
                FileOutputStream fos = new FileOutputStream(getMqsRamoopsFile());
                try {
                    FileInputStream ramoopsInput = new FileInputStream(lastKmsgFile);
                    FileUtils.copy(ramoopsInput, fos);
                    ramoopsInput.close();
                    fos.close();
                } catch (Throwable th) {
                    try {
                        fos.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                    throw th;
                }
            } catch (IOException e) {
                Slog.w(TAG, "IOException: copyRamoopsFileToMqs fail");
                e.printStackTrace();
            } catch (Exception e2) {
                Slog.w(TAG, "UNKown Exception: copyRamoopsFileToMqs fail");
                e2.printStackTrace();
            }
        }
    }

    private static File getMqsRamoopsFile() {
        File mqsFsDir = new File(MQS_PSTORE_DIR);
        try {
            if (!mqsFsDir.exists() && !mqsFsDir.isDirectory()) {
                mqsFsDir.mkdirs();
                FileUtils.setPermissions(mqsFsDir, 508, -1, -1);
            }
            return new File(mqsFsDir.getAbsolutePath(), "console-ramoops");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static StackTraceElement[] getMiuiStackTraceByTid(int tid) {
        try {
            Class<?> clazz = Class.forName("dalvik.system.VMStack");
            Method method = clazz.getDeclaredMethod("getMiuiStackTraceByTid", Integer.TYPE);
            method.setAccessible(true);
            StackTraceElement[] st = (StackTraceElement[]) method.invoke(null, Integer.valueOf(tid));
            if (ENABLED_SCOUT_DEBUG) {
                Slog.d(TAG, "getMiuiStackTraceByTid tid:" + tid);
            }
            return st;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void dumpUithreadPeriodHistoryMessage(long anrTime, int duration) {
        try {
            MessageMonitor monitor = UiThread.get().getLooper().getMessageMonitor();
            if (monitor == null) {
                Slog.w("MIUIScout ANR", "Can't dumpPeriodHistoryMessage because of null MessageMonitor");
            } else {
                List<String> historymsg = monitor.getHistoryMsgInfoStringInPeriod(anrTime, duration);
                for (int i = 0; i < historymsg.size(); i++) {
                    Slog.d("MIUIScout ANR", "get period history msg from android.ui:" + historymsg.get(i));
                }
            }
        } catch (Exception e) {
            Slog.w("MIUIScout ANR", "AnrScout failed to get period history msg", e);
        }
        UiThread.get().getLooper().printLoopInfo(5);
    }
}
