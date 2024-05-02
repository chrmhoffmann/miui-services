package com.miui.server.stability;

import android.os.FileUtils;
import android.util.Slog;
import com.android.server.MiuiBgThread;
import com.android.server.ScoutHelper;
import java.io.File;
import java.util.TreeSet;
/* loaded from: classes.dex */
public class DumpSysInfoUtil {
    public static final String FILE_DIR_HANGLOG = "hanglog";
    public static final String FILE_DIR_STABILITY = "/data/miuilog/stability/";
    private static final int MAX_FREEZE_SCREEN_STUCK_FILE = 3;
    private static final String MQSASD = "miui.mqsas.IMQSNative";
    private static final String TAG = "DumpSysInfoUtil";

    public static void captureDumpLog() {
        MiuiBgThread.getHandler().post(new Runnable() { // from class: com.miui.server.stability.DumpSysInfoUtil$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                DumpSysInfoUtil.lambda$captureDumpLog$0();
            }
        });
    }

    public static /* synthetic */ void lambda$captureDumpLog$0() {
        try {
            deleteDumpSysFile();
            ScoutHelper.Action action = new ScoutHelper.Action();
            action.addActionAndParam(ScoutHelper.ACTION_DUMPSYS, "SurfaceFlinger");
            action.addActionAndParam(ScoutHelper.ACTION_DUMPSYS, "activity activities");
            action.addActionAndParam(ScoutHelper.ACTION_DUMPSYS, "window");
            action.addActionAndParam(ScoutHelper.ACTION_DUMPSYS, "input");
            File dumpsysLogPath = getDumpSysFilePath();
            if (dumpsysLogPath != null) {
                ScoutHelper.dumpOfflineLog("Freeze_Screen_Stuck", action, FILE_DIR_HANGLOG, dumpsysLogPath.getAbsolutePath() + "/");
            }
        } catch (Exception exception) {
            Slog.e(TAG, "crash in the captureDumpLog()");
            exception.printStackTrace();
        }
    }

    private static void deleteDumpSysFile() {
        File[] listFiles;
        try {
            File hanglog = getDumpSysFilePath();
            TreeSet<File> existinglog = new TreeSet<>();
            if (hanglog != null && hanglog.exists() && hanglog.listFiles().length >= 3) {
                for (File file : hanglog.listFiles()) {
                    existinglog.add(file);
                }
                existinglog.pollFirst().delete();
            }
        } catch (Exception exception) {
            Slog.e(TAG, "crash in the deleteDumpSysFile()");
            exception.printStackTrace();
        }
    }

    private static File getDumpSysFilePath() {
        try {
            File stabilityLog = new File(FILE_DIR_STABILITY);
            File dumpsyslog = new File(stabilityLog, FILE_DIR_HANGLOG);
            if (!dumpsyslog.exists()) {
                if (!dumpsyslog.mkdirs()) {
                    Slog.e(TAG, "Cannot create dumpsyslog dir");
                    return null;
                }
                FileUtils.setPermissions(dumpsyslog.toString(), 511, -1, -1);
                Slog.d(TAG, "mkdir dumpsyslog dir");
            }
            return dumpsyslog;
        } catch (Exception exception) {
            Slog.e(TAG, "crash in the getDumpSysFilePath()");
            exception.printStackTrace();
            return null;
        }
    }
}
