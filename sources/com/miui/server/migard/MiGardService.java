package com.miui.server.migard;

import android.content.Context;
import android.os.Binder;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.ShellCallback;
import android.util.Singleton;
import com.android.internal.util.DumpUtils;
import com.android.server.LocalServices;
import com.miui.server.migard.memory.GameMemoryCleaner;
import com.miui.server.migard.memory.GameMemoryCleanerConfig;
import com.miui.server.migard.trace.GameTrace;
import com.miui.server.migard.utils.LogUtils;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import miui.migard.IMiGard;
/* loaded from: classes.dex */
public class MiGardService extends IMiGard.Stub {
    private static final Singleton<IMiGard> IMiGardSingleton = new Singleton<IMiGard>() { // from class: com.miui.server.migard.MiGardService.1
        public IMiGard create() {
            IBinder b = ServiceManager.getService(MiGardService.SERVICE_NAME);
            IMiGard service = IMiGard.Stub.asInterface(b);
            return service;
        }
    };
    public static final String JOYOSE_NAME = "xiaomi.joyose";
    public static final String MIGARD_DATA_PATH = "/data/system/migard";
    public static final String SERVICE_NAME = "migard";
    public static final String TAG = "MiGardService";
    private final Context mContext;
    GameMemoryCleaner mMemCleaner;

    private MiGardService(Context context) {
        this.mContext = context;
        LocalServices.addService(MiGardInternal.class, new LocalService());
        this.mMemCleaner = new GameMemoryCleaner(context);
        context.registerReceiver(ScreenStatusManager.getInstance(), ScreenStatusManager.getInstance().getFilter());
    }

    public static MiGardService getService() {
        return (MiGardService) IMiGardSingleton.get();
    }

    public static void startService(Context context) {
        ServiceManager.addService(SERVICE_NAME, new MiGardService(context));
    }

    public void startDefaultTrace(boolean async) {
        if (checkPermission(Binder.getCallingUid())) {
            GameTrace.getInstance().start(async);
        }
    }

    public void startTrace(String[] categories, boolean async) {
        if (checkPermission(Binder.getCallingUid())) {
            ArrayList<String> categoryList = new ArrayList<>();
            for (String c : categories) {
                categoryList.add(c);
            }
            GameTrace.getInstance().start(categoryList, async);
        }
    }

    public void stopTrace(boolean zip) {
        if (checkPermission(Binder.getCallingUid())) {
            GameTrace.getInstance().stop(zip);
        }
    }

    public void dumpTrace(boolean zip) {
        if (checkPermission(Binder.getCallingUid())) {
            GameTrace.getInstance().dump(zip);
        }
    }

    public void configTrace(String json) {
        if (checkPermission(Binder.getCallingUid())) {
            GameTrace.getInstance().configTrace(json);
        }
    }

    public void setTraceBufferSize(int size) {
        if (checkPermission(Binder.getCallingUid())) {
            GameTrace.getInstance().setBufferSize(size);
        }
    }

    public void addGameCleanUserProtectList(final List<String> list, final boolean append) {
        if (checkPermission(Binder.getCallingUid())) {
            this.mMemCleaner.runOnThread(new Runnable() { // from class: com.miui.server.migard.MiGardService$$ExternalSyntheticLambda4
                @Override // java.lang.Runnable
                public final void run() {
                    GameMemoryCleanerConfig.getInstance().addUserProtectList(list, append);
                }
            });
        }
    }

    public void removeGameCleanUserProtectList(final List<String> list) {
        if (checkPermission(Binder.getCallingUid())) {
            this.mMemCleaner.runOnThread(new Runnable() { // from class: com.miui.server.migard.MiGardService$$ExternalSyntheticLambda5
                @Override // java.lang.Runnable
                public final void run() {
                    GameMemoryCleanerConfig.getInstance().removeUserProtectList(list);
                }
            });
        }
    }

    public void configGameMemoryCleaner(final String game, final String json) {
        if (checkPermission(Binder.getCallingUid())) {
            this.mMemCleaner.runOnThread(new Runnable() { // from class: com.miui.server.migard.MiGardService$$ExternalSyntheticLambda3
                @Override // java.lang.Runnable
                public final void run() {
                    GameMemoryCleanerConfig.getInstance().configFromCloudControl(game, json);
                }
            });
        }
    }

    public void reclaimBackgroundMemory() {
        if (checkPermission(Binder.getCallingUid())) {
            this.mMemCleaner.reclaimBackgroundMemory();
        }
    }

    public void configGameList(List<String> list) {
        if (checkPermission(Binder.getCallingUid())) {
            this.mMemCleaner.addGameList(list);
        }
    }

    public void configKillerWhiteList(final List<String> list) {
        if (checkPermission(Binder.getCallingUid())) {
            this.mMemCleaner.runOnThread(new Runnable() { // from class: com.miui.server.migard.MiGardService$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    GameMemoryCleanerConfig.getInstance().addKillerCommonWhilteList(list);
                }
            });
        }
    }

    public void configCompactorWhiteList(final List<String> list) {
        if (checkPermission(Binder.getCallingUid())) {
            this.mMemCleaner.runOnThread(new Runnable() { // from class: com.miui.server.migard.MiGardService$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    GameMemoryCleanerConfig.getInstance().addCompactorCommonWhiteList(list);
                }
            });
        }
    }

    public void configPowerWhiteList(final String pkgList) {
        if (checkPermission(Binder.getCallingUid())) {
            this.mMemCleaner.runOnThread(new Runnable() { // from class: com.miui.server.migard.MiGardService$$ExternalSyntheticLambda2
                @Override // java.lang.Runnable
                public final void run() {
                    GameMemoryCleanerConfig.getInstance().updatePowerWhiteList(pkgList);
                }
            });
        }
    }

    private boolean checkPermission(int uid) {
        return uid == 1000;
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (!DumpUtils.checkDumpPermission(this.mContext, TAG, pw)) {
            return;
        }
        this.mMemCleaner.dump(pw);
    }

    /* JADX WARN: Multi-variable type inference failed */
    public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) {
        new MiGardShellCommand(this).exec(this, in, out, err, args, callback, resultReceiver);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class LocalService extends MiGardInternal {
        private LocalService() {
            MiGardService.this = r1;
        }

        @Override // com.miui.server.migard.MiGardInternal
        public void onProcessStart(int uid, int pid, String pkg, String caller) {
            MiGardService.this.mMemCleaner.onProcessStart(uid, pid, pkg, caller);
        }

        @Override // com.miui.server.migard.MiGardInternal
        public void onProcessKilled(int uid, int pid, String pkg, String reason) {
            MiGardService.this.mMemCleaner.onProcessKilled(pkg, reason);
        }

        @Override // com.miui.server.migard.MiGardInternal
        public void onVpnConnected(String user, boolean connected) {
            if (user != null && !user.isEmpty()) {
                LogUtils.d(MiGardService.TAG, "on vpn established, user=" + user);
                MiGardService.this.mMemCleaner.onVpnConnected(user, connected);
            }
        }
    }
}
