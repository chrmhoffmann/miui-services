package com.miui.server.greeze;

import android.os.SystemProperties;
import android.util.Slog;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
/* loaded from: classes.dex */
public class FreezeUtils {
    static boolean DEBUG = SystemProperties.getBoolean("persist.sys.gz.debug", false);
    static boolean DEBUG_CHECK_FREEZE = true;
    static boolean DEBUG_CHECK_THAW = true;
    private static final String FREEAER_FREEAE = "/sys/fs/cgroup/frozen/cgroup.freeze";
    private static final String FREEZER_CGROUP_FROZEN = "/sys/fs/cgroup/frozen";
    private static final String FREEZER_CGROUP_THAWED = "/sys/fs/cgroup/unfrozen";
    private static final String FREEZER_FROZEN_PORCS = "/sys/fs/cgroup/frozen/cgroup.procs";
    private static final String FREEZER_ROOT_PATH = "/sys/fs/cgroup";
    private static final int FREEZE_ACTION = 1;
    private static final String TAG = "FreezeUtils";
    private static final int UNFREEZE_ACTION = 0;
    private static final String V1_FREEZER_CGROUP_FROZEN = "/sys/fs/cgroup/freezer/perf/frozen";
    private static final String V1_FREEZER_CGROUP_THAWED = "/sys/fs/cgroup/freezer/perf/thawed";
    private static final String V1_FREEZER_FROZEN_PORCS = "/sys/fs/cgroup/freezer/perf/frozen/cgroup.procs";
    private static final String V1_FREEZER_FROZEN_TASKS = "/sys/fs/cgroup/freezer/perf/frozen/tasks";
    private static final String V1_FREEZER_ROOT_PATH = "/sys/fs/cgroup/freezer";
    private static final String V1_FREEZER_THAWED_PORCS = "/sys/fs/cgroup/freezer/perf/thawed/cgroup.procs";
    private static final String V1_FREEZER_THAWED_TASKS = "/sys/fs/cgroup/freezer/perf/thawed/tasks";

    public static boolean isFreezerEnable() {
        File groupFrozen = new File(FREEZER_CGROUP_FROZEN);
        File groupThawed = new File(FREEZER_CGROUP_THAWED);
        return groupFrozen.exists() && groupThawed.exists();
    }

    public static List<Integer> getFrozenPids() {
        List<Integer> pids = new ArrayList<>();
        String path = GreezeManagerService.mCgroupV1Flag ? V1_FREEZER_FROZEN_PORCS : FREEZER_FROZEN_PORCS;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(path));
            while (true) {
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                try {
                    pids.add(Integer.valueOf(Integer.parseInt(line)));
                } catch (NumberFormatException e) {
                    Slog.w(TAG, "Failed to parse " + path + " line: " + line, e);
                }
            }
            reader.close();
        } catch (IOException e2) {
            Slog.w(TAG, "Failed to get frozen pids", e2);
        }
        return pids;
    }

    public static List<Integer> getFrozonTids() {
        List<Integer> tids = new ArrayList<>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(V1_FREEZER_FROZEN_TASKS));
            while (true) {
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                try {
                    tids.add(Integer.valueOf(Integer.parseInt(line)));
                } catch (NumberFormatException e) {
                    Slog.w(TAG, "Failed to parse /sys/fs/cgroup/freezer/perf/frozen/tasks line: " + line, e);
                }
            }
            reader.close();
        } catch (IOException e2) {
            Slog.w(TAG, "Failed to get frozen tids", e2);
        }
        return tids;
    }

    public static boolean isFrozonPid(int pid) {
        return getFrozenPids().contains(Integer.valueOf(pid));
    }

    public static boolean isAllFrozon(int[] pids) {
        List<Integer> frozen = getFrozenPids();
        for (int pid : pids) {
            if (!frozen.contains(Integer.valueOf(pid))) {
                return false;
            }
        }
        return true;
    }

    static boolean isFrozonTid(int tid) {
        return getFrozonTids().contains(Integer.valueOf(tid));
    }

    public static boolean freezePid(int pid, int uid) {
        if (DEBUG) {
            Slog.d(TAG, "Freeze pid " + pid);
        }
        boolean done = setFreezeAction(pid, uid, true);
        return done;
    }

    private static boolean setFreezeAction(int pid, int uid, boolean allow) {
        String path = "/sys/fs/cgroup/uid_" + uid + "/pid_" + pid + "/cgroup.freeze";
        try {
            PrintWriter writer = new PrintWriter(path);
            if (allow) {
                writer.write(Integer.toString(1));
            } else {
                writer.write(Integer.toString(0));
            }
            writer.close();
            return true;
        } catch (IOException e) {
            Slog.d(TAG, "Failed to write to " + path + " type = " + allow);
            return false;
        }
    }

    public static boolean freezePid(int pid) {
        if (DEBUG) {
            Slog.d(TAG, "Freeze pid " + pid);
        }
        boolean done = writeNode(V1_FREEZER_FROZEN_PORCS, pid);
        if (DEBUG_CHECK_FREEZE && done && !isFrozonPid(pid)) {
            Slog.w(TAG, "Failed to thaw pid " + pid + ", it's still thawed!");
            return false;
        }
        return done;
    }

    public static boolean freezeTid(int tid) {
        if (DEBUG) {
            Slog.d(TAG, "Freeze tid " + tid);
        }
        return writeNode(V1_FREEZER_FROZEN_TASKS, tid);
    }

    public static boolean thawPid(int pid, int uid) {
        if (DEBUG) {
            Slog.d(TAG, "Thaw pid " + pid);
        }
        boolean done = setFreezeAction(pid, uid, false);
        return done;
    }

    public static boolean thawPid(int pid) {
        if (DEBUG) {
            Slog.d(TAG, "Thaw pid " + pid);
        }
        boolean done = writeNode(V1_FREEZER_THAWED_PORCS, pid);
        if (DEBUG_CHECK_THAW && done && isFrozonPid(pid)) {
            Slog.w(TAG, "Failed to thaw pid " + pid + ", it's still frozen!");
            return false;
        }
        return done;
    }

    public static boolean thawTid(int tid) {
        if (DEBUG) {
            Slog.d(TAG, "Thaw tid " + tid);
        }
        return writeNode(V1_FREEZER_THAWED_TASKS, tid);
    }

    public static boolean writeFreezeValue() {
        return writeNode(FREEAER_FREEAE, 1);
    }

    private static boolean writeNode(String path, int val) {
        try {
            PrintWriter writer = new PrintWriter(path);
            writer.write(Integer.toString(val));
            if (DEBUG) {
                Slog.d(TAG, "Wrote to " + path + " with value " + val);
            }
            writer.close();
            return true;
        } catch (IOException e) {
            Slog.w(TAG, "Failed to write to " + path + " with value " + val, e);
            return false;
        }
    }
}
