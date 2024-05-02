package com.android.server.backup;

import android.app.backup.BackupHelper;
import android.content.Context;
import com.miui.base.MiuiStubRegistry;
import miui.stepcounter.backup.StepBackupHelper;
/* loaded from: classes.dex */
public class SystemBackupAgentImpl implements SystemBackupAgentStub {
    private static final String STEP_COUNTER_HELPER = "step_counter";

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<SystemBackupAgentImpl> {

        /* compiled from: SystemBackupAgentImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final SystemBackupAgentImpl INSTANCE = new SystemBackupAgentImpl();
        }

        public SystemBackupAgentImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public SystemBackupAgentImpl provideNewInstance() {
            return new SystemBackupAgentImpl();
        }
    }

    public String getHelperName() {
        return STEP_COUNTER_HELPER;
    }

    public BackupHelper createBackupHelper(Context context) {
        return new StepBackupHelper(context);
    }
}
