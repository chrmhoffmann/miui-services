package com.miui.server.stability;

import android.os.SystemProperties;
import android.util.Slog;
import com.android.server.ScoutHelper;
import miui.mqsas.scout.ScoutUtils;
/* loaded from: classes.dex */
public class ScoutLibraryTestManager {
    public static final String PROPERTIES_APP_MTBF_TEST = "persist.sys.debug.app.mtbf_test";
    private static final String TAG = "ScoutLibraryTestManager";
    private static boolean debug = ScoutHelper.ENABLED_SCOUT_DEBUG;
    private static ScoutLibraryTestManager scoutLibraryTestManager;

    private native void enableCoreDump();

    public static ScoutLibraryTestManager getInstance() {
        if (scoutLibraryTestManager == null) {
            scoutLibraryTestManager = new ScoutLibraryTestManager();
        }
        return scoutLibraryTestManager;
    }

    private ScoutLibraryTestManager() {
    }

    public void init() {
        if (ScoutUtils.REBOOT_COREDUMP) {
            enableCoreDump();
            Slog.d(TAG, "enable system_server CoreDump");
        }
        SystemProperties.set(PROPERTIES_APP_MTBF_TEST, "true");
    }
}
