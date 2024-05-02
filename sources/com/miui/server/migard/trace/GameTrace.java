package com.miui.server.migard.trace;

import android.os.SystemClock;
import android.os.SystemProperties;
import android.server.am.SplitScreenReporter;
import android.util.Slog;
import com.miui.server.migard.utils.FileUtils;
import java.io.File;
import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
/* loaded from: classes.dex */
public class GameTrace {
    private static final String ADD_KERNEL_CATEGORY = "addKernelCategory";
    private static final String ADD_KERNEL_CATEGORY_PATH = "addKernelCategoryPath";
    private static final String AES_ALGO = "aes";
    private static final int ATRACE_TAG_ACTIVITY_MANAGER = 64;
    private static final int ATRACE_TAG_ADB = 4194304;
    private static final int ATRACE_TAG_AIDL = 16777216;
    private static final int ATRACE_TAG_AUDIO = 256;
    private static final int ATRACE_TAG_BIONIC = 65536;
    private static final int ATRACE_TAG_CAMERA = 1024;
    private static final int ATRACE_TAG_DALVIK = 16384;
    private static final int ATRACE_TAG_DATABASE = 1048576;
    private static final int ATRACE_TAG_GRAPHICS = 2;
    private static final int ATRACE_TAG_HAL = 2048;
    private static final int ATRACE_TAG_INPUT = 4;
    private static final int ATRACE_TAG_NETWORK = 2097152;
    private static final int ATRACE_TAG_NEVER = 0;
    private static final int ATRACE_TAG_NNAPI = 33554432;
    private static final int ATRACE_TAG_PACKAGE_MANAGER = 262144;
    private static final int ATRACE_TAG_POWER = 131072;
    private static final int ATRACE_TAG_RESOURCES = 8192;
    private static final int ATRACE_TAG_RRO = 67108864;
    private static final int ATRACE_TAG_RS = 32768;
    private static final int ATRACE_TAG_SYNC_MANAGER = 128;
    private static final int ATRACE_TAG_SYSTEM_SERVER = 524288;
    private static final int ATRACE_TAG_VIBRATOR = 8388608;
    private static final int ATRACE_TAG_VIDEO = 512;
    private static final int ATRACE_TAG_VIEW = 8;
    private static final int ATRACE_TAG_WEBVIEW = 16;
    private static final int ATRACE_TAG_WINDOW_MANAGER = 32;
    private static final String CATETORY_KEY = "category";
    private static final String COMMANDS_AS_ROOT_FILE_PATH = "/data/system/migard/game_trace/root_commands";
    private static final String CURRENT_TRACER_PATH = "current_tracer";
    private static final String DEBUG_FS_PATH = "/sys/kernel/debug/tracing/";
    private static final String DETAILS_KEY = "details";
    private static final String ENABLED_KEY = "enabled";
    private static final String ENABLE_KERNEL_CATEGORY_PATH = "enableKernelCategoryPath";
    private static final String FTRACE_FILTER_PATH = "set_ftrace_filter";
    private static final int MAX_TRACE_SIZE = 5;
    private static final String NON_ALGO = "non";
    private static final String PATH_KEY = "path";
    private static final String REMOVE_KERNEL_CATEGORY_PATH = "removeKernelCategoryPath";
    private static final String SET_BUFFER_SIZE = "setBufferSize";
    private static final String SET_CRYPTO_ALGO = "setCryptoAlgo";
    private static final String SET_TRACE_DIR = "setTraceDir";
    private static final String TAG = "GameTrace";
    private static final int TRACE_BUFFER_SIZE = 2048;
    private static final String TRACE_BUFFER_SIZE_PATH = "buffer_size_kb";
    private static final String TRACE_CLOCK_PATH = "trace_clock";
    private static final String TRACE_FILE_SAVE_PATH = "/data/system/migard/game_trace";
    private static final String TRACE_FS_PATH = "/sys/kernel/tracing/";
    private static final String TRACE_MARKER_PATH = "trace_marker";
    private static final String TRACE_OVERWRITE_PATH = "options/overwrite";
    private static final String TRACE_PATH = "trace";
    private static final String TRACE_PRINT_TGID_PATH = "options/print-tgid";
    private static final String TRACE_RECORD_TGID_PATH = "options/record-tgid";
    private static final String TRACE_TAGS_PROPERTY = "debug.atrace.tags.enableflags";
    private static final String TRACING_ON_PATH = "tracing_on";
    private static final String USER_INITIATED_PROPERTY = "debug.atrace.user_initiated";
    private static final String XOR_ALGO = "xor";
    private static GameTrace sInstance = new GameTrace();
    private boolean isTraceSupported;
    private int mTraceBufferSizeKB;
    private String mTraceFolder;
    private final HashMap<String, TracingCategory> mCategories = new HashMap<>();
    private boolean isAsync = false;
    private String mTraceDir = null;
    private String mTraceCryptoAlgo = null;
    private final String mModuleName = SystemProperties.get("ro.product.model", "");
    private volatile boolean isGameTraceRunning = false;

    private GameTrace() {
        setBufferSize(2048);
        initTracingCategories();
        initTraceFolder();
    }

    public static GameTrace getInstance() {
        return sInstance;
    }

    public void start(boolean async) {
        this.isAsync = async;
        ArrayList<String> categories = new ArrayList<>();
        for (String category : this.mCategories.keySet()) {
            categories.add(category);
        }
        start(categories, async);
    }

    public void start(ArrayList<String> categories, boolean async) {
        this.isAsync = async;
        if (!this.isTraceSupported || categories == null || categories.size() == 0) {
            return;
        }
        if (this.isGameTraceRunning) {
            Slog.w(TAG, "start game trace failed, game trace is already running.");
            return;
        }
        this.isGameTraceRunning = true;
        setUserInitiatedTraceProperty(true);
        int tags = 0;
        for (int i = 0; i < categories.size(); i++) {
            TracingCategory tempCategory = this.mCategories.get(categories.get(i));
            if (tempCategory != null) {
                if (tempCategory.getTags() != 0) {
                    tags |= tempCategory.getTags();
                } else {
                    setupKernelCategories(tempCategory.getTracePathMap());
                }
            }
        }
        setTraceOverwriteEnable(async);
        setupUserspaceCategories(tags);
        applyTraceBufferSize();
        setClock();
        setPrintTgidEnableIfPresent(true);
        clearKernelTraceFuncs();
        setTraceEnabled(true);
        clearTrace();
        writeClockSyncMarker();
    }

    private void clearOldTrace() {
        Slog.i(TAG, "start clearOldTrace.");
        List<File> list = getFileSort(TRACE_FILE_SAVE_PATH);
        for (int i = 0; i < list.size(); i++) {
            if (i >= 5) {
                list.get(i).delete();
            }
        }
    }

    private List<File> getFileSort(String path) {
        List<File> list = getFiles(path, new ArrayList());
        if (list != null && list.size() > 0) {
            Collections.sort(list, new Comparator<File>() { // from class: com.miui.server.migard.trace.GameTrace.1
                public int compare(File file, File newFile) {
                    if (file.lastModified() < newFile.lastModified()) {
                        return 1;
                    }
                    if (file.lastModified() == newFile.lastModified()) {
                        return 0;
                    }
                    return -1;
                }
            });
        }
        return list;
    }

    private List<File> getFiles(String realpath, List<File> files) {
        File realFile = new File(realpath);
        if (realFile.isDirectory()) {
            File[] subfiles = realFile.listFiles();
            for (File file : subfiles) {
                if (file.isDirectory()) {
                    getFiles(file.getAbsolutePath(), files);
                } else {
                    files.add(file);
                }
            }
        }
        return files;
    }

    private String checkTraceDir(String path) {
        Slog.i(TAG, "checkTraceDir path:" + path);
        if (path != null) {
            File dir = new File(path);
            Slog.i(TAG, "dir.exists: " + dir.exists() + " dir: " + dir);
            if (!dir.exists()) {
                Slog.i(TAG, "path: " + path);
                dir.mkdirs();
                return null;
            }
        }
        return null;
    }

    private String checkCryptoAlgo(String algo) {
        if (algo.equals(XOR_ALGO) || algo.equals(AES_ALGO)) {
            Slog.i(TAG, "checkCryptoAlgo algo: " + algo);
            return algo;
        }
        return NON_ALGO;
    }

    public void stop(boolean zip) {
        File mTracePath = new File(TRACE_FILE_SAVE_PATH);
        String str = this.mTraceDir;
        if (str != null) {
            stop(str, zip);
            return;
        }
        String name = generateTraceName();
        clearOldTrace();
        stop(TRACE_FILE_SAVE_PATH, zip);
        FileUtils.delOldZipFile(mTracePath);
        FileUtils.readAllSystraceToZip(TRACE_FILE_SAVE_PATH, TRACE_FILE_SAVE_PATH, name);
        FileUtils.delTmpTraceFile(mTracePath);
    }

    public void dump(boolean zip) {
        String str = this.mTraceDir;
        if (str != null) {
            dump(str, zip);
        } else {
            dump(TRACE_FILE_SAVE_PATH, zip);
        }
    }

    public void stop(String dirPath, boolean zip) {
        if (!this.isGameTraceRunning) {
            Slog.w(TAG, "stop game trace failed, game trace is not running.");
            return;
        }
        setTraceEnabled(false);
        if (dirPath != null && dirPath != "" && !this.isAsync) {
            Slog.i(TAG, "isAsync: " + this.isAsync);
            dump(dirPath, zip);
        }
        cleanUpUserspaceCategories();
        cleanUpKernelCategories();
        this.isGameTraceRunning = false;
        this.mTraceDir = null;
    }

    public void dump(String dirPath, boolean zip) {
        Slog.w(TAG, "dump cameinv dirPath: " + dirPath);
        File traceFile = new File(this.mTraceFolder + TRACE_PATH);
        if (!traceFile.exists()) {
            return;
        }
        FileInputStream in = null;
        try {
            try {
                in = new FileInputStream(traceFile);
                String name = generateTraceName();
                FileUtils.readSysToFile(in, dirPath, name);
                try {
                    in.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } catch (Throwable th) {
                if (in != null) {
                    try {
                        in.close();
                    } catch (Exception e2) {
                        e2.printStackTrace();
                    }
                }
                throw th;
            }
        } catch (Exception e3) {
            e3.printStackTrace();
            if (in != null) {
                try {
                    in.close();
                } catch (Exception e4) {
                    e4.printStackTrace();
                }
            }
        }
        clearTrace();
    }

    public void configTrace(String json) {
    }

    private void addKernelCategory(JSONObject content) {
        try {
            HashMap<String, Boolean> map = new HashMap<>();
            String category = content.getString(CATETORY_KEY);
            JSONArray details = content.getJSONArray(DETAILS_KEY);
            for (int i = 0; i < details.length(); i++) {
                JSONObject temp = details.getJSONObject(i);
                if (temp != null) {
                    String path = temp.getString(PATH_KEY);
                    boolean enabled = temp.getBoolean(ENABLED_KEY);
                    map.put(path, Boolean.valueOf(enabled));
                }
            }
            addKernelCategory(category, map);
        } catch (JSONException e) {
            Slog.e(TAG, "parse config failed, add Kernel Category failed.");
        }
    }

    private void addKernelCategoryPath(JSONObject content) {
        try {
            String category = content.getString(CATETORY_KEY);
            String path = content.getString(PATH_KEY);
            boolean enabled = content.getBoolean(ENABLED_KEY);
            addKernelCategoryPath(category, path, enabled);
        } catch (JSONException e) {
            Slog.e(TAG, "parse config failed, add kernel category path failed.");
        }
    }

    private void enableKernelCategoryPath(JSONObject content) {
        try {
            String category = content.getString(CATETORY_KEY);
            String path = content.getString(PATH_KEY);
            boolean enabled = content.getBoolean(ENABLED_KEY);
            enableKernelCategoryPath(category, path, enabled);
        } catch (JSONException e) {
            Slog.e(TAG, "parse config failed, enable kernel category path failed.");
        }
    }

    private void removeKernelCategoryPath(JSONObject content) {
        try {
            String category = content.getString(CATETORY_KEY);
            String path = content.getString(PATH_KEY);
            removeKernelCategoryPath(category, path);
        } catch (JSONException e) {
            Slog.e(TAG, "parse config failed, remove kernel category path failed.");
        }
    }

    public void setBufferSize(int size) {
        if (this.isGameTraceRunning) {
            Slog.w(TAG, "cannot set buffer size, while game trace running.");
        } else {
            this.mTraceBufferSizeKB = size;
        }
    }

    public void addKernelCategory(String category, HashMap<String, Boolean> details) {
        if (this.isGameTraceRunning) {
            Slog.w(TAG, "cannot add kernel category, while game trace running.");
        } else if (category != null && category.length() > 0 && details != null) {
            this.mCategories.put(category, new TracingCategory(category, 0, details));
        } else {
            Slog.e(TAG, "add kernel category failed, check your parameters.");
        }
    }

    public void addKernelCategoryPath(String category, String path, boolean enabled) {
        if (this.isGameTraceRunning) {
            Slog.w(TAG, "cannot add kernel category path, while game trace running.");
        } else {
            setCategoryPathEnabled(category, path, enabled, false);
        }
    }

    public void enableKernelCategoryPath(String category, String path, boolean enabled) {
        if (this.isGameTraceRunning) {
            Slog.w(TAG, "cannot enable kernel category path, while game trace running.");
        } else {
            setCategoryPathEnabled(category, path, enabled, true);
        }
    }

    public void removeKernelCategoryPath(String category, String path) {
        if (this.isGameTraceRunning) {
            Slog.w(TAG, "cannot remove kernel category path, while game trace running.");
        } else if (category == null || path == null) {
            Slog.e(TAG, "remove kernel category path failed, check your parameters.");
        } else {
            TracingCategory tempCategory = this.mCategories.get(category);
            if (tempCategory == null) {
                Slog.e(TAG, "remove kernel category path failed, unknown category.");
            } else {
                tempCategory.removePath(path);
            }
        }
    }

    private void setCategoryPathEnabled(String category, String path, boolean enabled, boolean modify) {
        if (category == null || path == null) {
            Slog.e(TAG, "remove kernel category path failed, check your parameters.");
            return;
        }
        TracingCategory tempCategory = this.mCategories.get(category);
        if (tempCategory == null) {
            Slog.e(TAG, "remove kernel category path failed, unknown category.");
        } else if (modify) {
            if (!tempCategory.setPathEnabled(path, enabled)) {
                Slog.e(TAG, "enable kernel category path failed, unknown path.");
            }
        } else {
            tempCategory.addPath(path, enabled);
        }
    }

    private void resetBufferSize() {
        this.mTraceBufferSizeKB = 2048;
        setKernelContent(TRACE_BUFFER_SIZE_PATH, SplitScreenReporter.ACTION_ENTER_SPLIT);
    }

    private void writeClockSyncMarker() {
        File markerFile = new File(this.mTraceFolder + TRACE_MARKER_PATH);
        if (markerFile.exists() && markerFile.canWrite()) {
            float realTime = ((float) System.currentTimeMillis()) / 1000.0f;
            setKernelContent(TRACE_MARKER_PATH, "trace_event_clock_sync: realtime_ts=" + realTime);
            float monoTime = ((float) SystemClock.elapsedRealtime()) / 1000.0f;
            setKernelContent(TRACE_MARKER_PATH, "trace_event_clock_sync: parent_ts=" + monoTime);
        }
    }

    private void clearKernelTraceFuncs() {
        File cfile = new File(this.mTraceFolder + CURRENT_TRACER_PATH);
        if (cfile.exists()) {
            setKernelContent(CURRENT_TRACER_PATH, "nop");
        }
        File ffile = new File(this.mTraceFolder + FTRACE_FILTER_PATH);
        if (ffile.exists()) {
            FileUtils.truncateSysFile(this.mTraceFolder + FTRACE_FILTER_PATH);
        }
    }

    private void setPrintTgidEnableIfPresent(boolean enable) {
        File pfile = new File(this.mTraceFolder + TRACE_PRINT_TGID_PATH);
        if (pfile.exists()) {
            setKernelOptionEnable(TRACE_PRINT_TGID_PATH, enable);
        }
        File rfile = new File(this.mTraceFolder + TRACE_RECORD_TGID_PATH);
        if (rfile.exists()) {
            setKernelOptionEnable(TRACE_RECORD_TGID_PATH, enable);
        }
    }

    private void setClock() {
        String newClock;
        String str = FileUtils.readFromSys(this.mTraceFolder + TRACE_CLOCK_PATH);
        if (str.contains("boot")) {
            newClock = "boot";
        } else if (str.contains("mono")) {
            newClock = "mono";
        } else {
            newClock = "global";
        }
        if (str.equals(newClock)) {
            return;
        }
        setKernelContent(TRACE_CLOCK_PATH, newClock);
    }

    private void setupKernelCategories(HashMap<String, Boolean> categories) {
        for (Map.Entry<String, Boolean> entry : categories.entrySet()) {
            FileUtils.writeToSys(this.mTraceFolder + entry.getKey(), entry.getValue().booleanValue() ? SplitScreenReporter.ACTION_ENTER_SPLIT : "0");
        }
    }

    private void applyTraceBufferSize() {
        setKernelContent(TRACE_BUFFER_SIZE_PATH, Integer.toString(this.mTraceBufferSizeKB));
    }

    private String generateTraceName() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        String dateString = formatter.format(new Date());
        return dateString + "_trace";
    }

    private void setupUserspaceCategories(int tags) {
        String value = "0x" + Integer.toHexString(tags);
        SystemProperties.set(TRACE_TAGS_PROPERTY, value);
    }

    private void setUserInitiatedTraceProperty(boolean enabled) {
        SystemProperties.set(USER_INITIATED_PROPERTY, enabled ? SplitScreenReporter.ACTION_ENTER_SPLIT : "");
    }

    private void setTraceEnabled(boolean enabled) {
        setKernelOptionEnable(TRACING_ON_PATH, enabled);
    }

    private void setTraceOverwriteEnable(boolean enabled) {
        setKernelOptionEnable(TRACE_OVERWRITE_PATH, enabled);
    }

    private void setKernelOptionEnable(String path, boolean enabled) {
        FileUtils.writeToSys(this.mTraceFolder + path, enabled ? SplitScreenReporter.ACTION_ENTER_SPLIT : "0");
    }

    private void setKernelContent(String path, String content) {
        FileUtils.writeToSys(this.mTraceFolder + path, content);
    }

    private void cleanUpUserspaceCategories() {
        setupUserspaceCategories(0);
    }

    private void disableKernelCategories(ArrayList<String> categories) {
        for (int i = 0; i < categories.size(); i++) {
            FileUtils.writeToSys(categories.get(i), "0");
        }
    }

    private void disableAllKernelCategories() {
        for (Map.Entry<String, TracingCategory> entry : this.mCategories.entrySet()) {
            TracingCategory tempCategory = entry.getValue();
            if (tempCategory != null && tempCategory.getTags() == 0) {
                disableKernelCategories(tempCategory.getTracePathList());
            }
        }
    }

    private void cleanUpKernelCategories() {
        disableAllKernelCategories();
        setTraceOverwriteEnable(true);
        resetBufferSize();
        setPrintTgidEnableIfPresent(false);
        clearKernelTraceFuncs();
        setUserInitiatedTraceProperty(false);
    }

    private void clearTrace() {
        FileUtils.truncateSysFile(this.mTraceFolder + TRACE_PATH);
    }

    private void initTracingCategories() {
        this.mCategories.put("gfx", new TracingCategory("gfx", 2));
        this.mCategories.put("input", new TracingCategory("input", 4));
        this.mCategories.put("view", new TracingCategory("view", 8));
        this.mCategories.put("webview", new TracingCategory("webview", 16));
        this.mCategories.put("wm", new TracingCategory("wm", 32));
        this.mCategories.put("am", new TracingCategory("am", 64));
        this.mCategories.put("sm", new TracingCategory("sm", 128));
        this.mCategories.put("audio", new TracingCategory("audio", 256));
        this.mCategories.put("video", new TracingCategory("video", 512));
        this.mCategories.put("camera", new TracingCategory("camera", 1024));
        this.mCategories.put("hal", new TracingCategory("hal", 2048));
        this.mCategories.put("res", new TracingCategory("res", 8192));
        this.mCategories.put("dalvik", new TracingCategory("dalvik", 16384));
        this.mCategories.put("rs", new TracingCategory("rs", 32768));
        this.mCategories.put("bionic", new TracingCategory("bionic", 65536));
        this.mCategories.put("power", new TracingCategory("power", ATRACE_TAG_POWER));
        this.mCategories.put("pm", new TracingCategory("pm", 262144));
        this.mCategories.put("ss", new TracingCategory("ss", 524288));
        this.mCategories.put("database", new TracingCategory("database", ATRACE_TAG_DATABASE));
        this.mCategories.put("network", new TracingCategory("network", 2097152));
        this.mCategories.put("adb", new TracingCategory("adb", 4194304));
        this.mCategories.put("vibrator", new TracingCategory("vibrator", ATRACE_TAG_VIBRATOR));
        this.mCategories.put("aidl", new TracingCategory("aidl", ATRACE_TAG_AIDL));
        this.mCategories.put("nnapi", new TracingCategory("nnapi", ATRACE_TAG_NNAPI));
        this.mCategories.put("rro", new TracingCategory("rro", 67108864));
        TracingCategory temp = new TracingCategory("sched", 0);
        temp.addPath("events/sched/sched_switch/enable", true);
        temp.addPath("events/sched/sched_wakeup/enable", true);
        temp.addPath("events/sched/sched_waking/enable", false);
        temp.addPath("events/sched/sched_blocked_reason/enable", false);
        temp.addPath("events/sched/sched_pi_setprio/enable", false);
        temp.addPath("events/sched/sched_process_exit/enable", false);
        temp.addPath("events/cgroup/enable", false);
        temp.addPath("events/oom/oom_score_adj_update/enable", false);
        temp.addPath("events/task/task_rename/enable", false);
        temp.addPath("events/task/task_newtask/enable", false);
        this.mCategories.put("sched", temp);
        TracingCategory temp2 = new TracingCategory("irq", 0);
        temp2.addPath("events/irq/enable", false);
        temp2.addPath("events/ipi/enable", false);
        this.mCategories.put("irq", temp2);
        TracingCategory temp3 = new TracingCategory("freq", 0);
        temp3.addPath("events/power/cpu_frequency/enable", true);
        temp3.addPath("events/power/clock_set_rate/enable", false);
        temp3.addPath("events/power/clock_disable/enable", false);
        temp3.addPath("events/power/clock_enable/enable", false);
        temp3.addPath("events/clk/clk_set_rate/enable", false);
        temp3.addPath("events/clk/clk_disable/enable", false);
        temp3.addPath("events/clk/clk_enable/enable", false);
        temp3.addPath("events/power/cpu_frequency_limits/enable", false);
        temp3.addPath("events/power/suspend_resume/enable", false);
        temp3.addPath("events/cpuhp/cpuhp_enter/enable", false);
        temp3.addPath("events/cpuhp/cpuhp_exit/enable", false);
        this.mCategories.put("freq", temp3);
        TracingCategory temp4 = new TracingCategory("idle", 0);
        temp4.addPath("events/power/cpu_idle/enable", true);
        this.mCategories.put("idle", temp4);
        TracingCategory temp5 = new TracingCategory("disk", 0);
        temp5.addPath("events/block/block_rq_issue/enable", true);
        temp5.addPath("events/block/block_rq_complete/enable", true);
        temp5.addPath("events/f2fs/f2fs_sync_file_enter/enable", false);
        temp5.addPath("events/f2fs/f2fs_sync_file_exit/enable", false);
        temp5.addPath("events/f2fs/f2fs_write_begin/enable", false);
        temp5.addPath("events/f2fs/f2fs_write_end/enable", false);
        temp5.addPath("events/ext4/ext4_da_write_begin/enable", false);
        temp5.addPath("events/ext4/ext4_da_write_end/enable", false);
        temp5.addPath("events/ext4/ext4_sync_file_enter/enable", false);
        temp5.addPath("events/ext4/ext4_sync_file_exit/enable", false);
        this.mCategories.put("disk", temp5);
        TracingCategory temp6 = new TracingCategory("sync", 0);
        temp6.addPath("events/dma_fence/enable", false);
        this.mCategories.put("sync", temp6);
        TracingCategory temp7 = new TracingCategory("memreclaim", 0);
        temp7.addPath("events/vmscan/mm_vmscan_direct_reclaim_begin/enable", true);
        temp7.addPath("events/vmscan/mm_vmscan_direct_reclaim_end/enable", true);
        temp7.addPath("events/vmscan/mm_vmscan_kswapd_wake/enable", true);
        temp7.addPath("events/vmscan/mm_vmscan_kswapd_sleep/enable", true);
        this.mCategories.put("memreclaim", temp7);
        TracingCategory temp8 = new TracingCategory("binder_driver", 0);
        temp8.addPath("events/binder/binder_transaction/enable", true);
        temp8.addPath("events/binder/binder_transaction_received/enable", true);
        temp8.addPath("events/binder/binder_transaction_alloc_buf/enable", true);
        temp8.addPath("events/binder/binder_set_priority/enable", false);
        this.mCategories.put("binder_driver", temp8);
        TracingCategory temp9 = new TracingCategory("binder_lock", 0);
        temp9.addPath("events/binder/binder_lock/enable", false);
        temp9.addPath("events/binder/binder_locked/enable", false);
        temp9.addPath("events/binder/binder_unlock/enable", false);
        this.mCategories.put("binder_lock", temp9);
        TracingCategory temp10 = new TracingCategory("memory", 0);
        temp10.addPath("events/kmem/rss_stat/enable", false);
        this.mCategories.put("memory", temp10);
        TracingCategory temp11 = new TracingCategory("thermal", 0);
        temp11.addPath("events/thermal/thermal_temperature/enable", false);
        temp11.addPath("events/thermal/cdev_update/enable", false);
        this.mCategories.put("thermal", temp11);
    }

    private void initTraceFolder() {
        this.isTraceSupported = true;
        File traceFile = new File("/sys/kernel/tracing");
        File debugFile = new File("/sys/kernel/debug/tracing");
        if (traceFile.exists()) {
            this.mTraceFolder = TRACE_FS_PATH;
        } else if (debugFile.exists()) {
            this.mTraceFolder = DEBUG_FS_PATH;
        } else {
            this.isTraceSupported = false;
        }
    }

    /* loaded from: classes.dex */
    public static class TracingCategory {
        String name;
        HashMap<String, Boolean> pathConfig;
        int tags;

        TracingCategory(String name, int tags) {
            this.name = name;
            this.tags = tags;
            this.pathConfig = new HashMap<>();
        }

        TracingCategory(String name, int tags, HashMap<String, Boolean> pathConfig) {
            this.name = name;
            this.tags = tags;
            this.pathConfig = pathConfig;
        }

        void addPath(String path) {
            addPath(path, false);
        }

        void addPath(String path, boolean on) {
            this.pathConfig.put(path, Boolean.valueOf(on));
        }

        boolean setPathEnabled(String path, boolean on) {
            if (this.pathConfig.containsKey(path)) {
                this.pathConfig.put(path, Boolean.valueOf(on));
                return true;
            }
            return false;
        }

        void removePath(String path) {
            this.pathConfig.remove(path);
        }

        int getTags() {
            return this.tags;
        }

        HashMap<String, Boolean> getTracePathMap() {
            return this.pathConfig;
        }

        ArrayList<String> getEnabledTracePathList() {
            ArrayList<String> pathList = new ArrayList<>();
            for (Map.Entry<String, Boolean> entry : this.pathConfig.entrySet()) {
                if (entry.getValue().booleanValue()) {
                    pathList.add(entry.getKey());
                }
            }
            return pathList;
        }

        ArrayList<String> getTracePathList() {
            ArrayList<String> pathList = new ArrayList<>();
            for (String p : this.pathConfig.keySet()) {
                pathList.add(p);
            }
            return pathList;
        }
    }
}
