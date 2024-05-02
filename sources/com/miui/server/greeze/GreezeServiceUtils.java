package com.miui.server.greeze;

import android.app.ActivityManager;
import android.os.UserHandle;
import android.util.ArraySet;
import android.util.Slog;
import android.util.SparseArray;
import android.view.inputmethod.InputMethodInfo;
import com.android.server.LocalServices;
import com.android.server.am.ActivityManagerService;
import com.android.server.inputmethod.InputMethodManagerInternal;
import com.miui.server.process.ProcessManagerInternal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import miui.process.ActiveUidInfo;
import miui.process.ProcessManager;
import miui.process.RunningProcessInfo;
/* loaded from: classes.dex */
public class GreezeServiceUtils {
    public static int GREEZER_MODULE_UNKNOWN = 0;
    public static int GREEZER_MODULE_POWER = 1;
    public static int GREEZER_MODULE_PERFORMANCE = 2;
    public static int GREEZER_MODULE_GAME = 3;
    public static int GREEZER_MODULE_PRELOAD = 4;
    public static int GREEZER_MODULE_ALL = 9999;
    public static String TAG = "GreezeServiceUtils";

    public static Set<Integer> getAudioUid() {
        Set<Integer> activeUids = new ArraySet<>();
        try {
            List<ActiveUidInfo> uidInfos = ProcessManager.getActiveUidInfo(3);
            for (ActiveUidInfo info : uidInfos) {
                activeUids.add(Integer.valueOf(info.uid));
            }
        } catch (Exception e) {
            Slog.w(TAG, "Failed to get active audio info from ProcessManager", e);
        }
        return activeUids;
    }

    public static Set<Integer> getIMEUid() {
        Set<Integer> uids = new ArraySet<>();
        try {
            InputMethodManagerInternal imm = (InputMethodManagerInternal) LocalServices.getService(InputMethodManagerInternal.class);
            List<InputMethodInfo> inputMetheds = imm.getInputMethodListAsUser(UserHandle.myUserId());
            for (InputMethodInfo info : inputMetheds) {
                uids.add(Integer.valueOf(info.getServiceInfo().applicationInfo.uid));
            }
        } catch (Exception e) {
            Slog.w(TAG, "Failed to get IME Uid from InputMethodManagerInternal", e);
        }
        return uids;
    }

    public static SparseArray<List<RunningProcess>> getUidMap() {
        SparseArray<List<RunningProcess>> uidMap = new SparseArray<>();
        List<RunningProcess> list = getProcessList();
        for (RunningProcess proc : list) {
            int uid = proc.uid;
            List<RunningProcess> procs = uidMap.get(uid);
            if (procs == null) {
                procs = new ArrayList();
                uidMap.put(uid, procs);
            }
            procs.add(proc);
        }
        return uidMap;
    }

    public static List<RunningProcess> getProcessList() {
        List<RunningProcess> procs = new ArrayList<>();
        try {
            ProcessManagerInternal pmi = (ProcessManagerInternal) LocalServices.getService(ProcessManagerInternal.class);
            List<RunningProcessInfo> list = pmi.getAllRunningProcessInfo();
            for (RunningProcessInfo info : list) {
                if (info != null) {
                    procs.add(new RunningProcess(info));
                }
            }
        } catch (Exception e) {
            Slog.w(TAG, "Failed to get RunningProcessInfo from ProcessManager", e);
        }
        return procs;
    }

    public static List<RunningProcess> getProcessListFromAMS(ActivityManagerService ams) {
        List<RunningProcess> procs = new ArrayList<>();
        try {
            List<ActivityManager.RunningAppProcessInfo> list = ams.getRunningAppProcesses();
            for (ActivityManager.RunningAppProcessInfo info : list) {
                if (info != null) {
                    procs.add(new RunningProcess(info));
                }
            }
        } catch (Exception e) {
            Slog.w(TAG, "Failed to get RunningProcessInfo from ProcessManager", e);
        }
        return procs;
    }
}
