package com.android.server.backup;

import android.app.ActivityManager;
import android.app.AppGlobals;
import android.app.IActivityManager;
import android.app.IBackupAgent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import android.util.Slog;
import com.android.server.backup.fullbackup.FullBackupEngine;
import com.android.server.backup.fullbackup.PerformAdbBackupTask;
import com.android.server.backup.restore.FullRestoreEngine;
import com.android.server.backup.utils.FullBackupUtils;
import com.android.server.wm.ActivityStarterInjector;
import com.miui.base.MiuiStubRegistry;
import com.miui.server.BackupProxyHelper;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import libcore.io.IoBridge;
import miui.app.backup.BackupManager;
import miui.app.backup.IBackupManager;
import miui.os.Build;
/* loaded from: classes.dex */
public class BackupManagerServiceInjector implements BackupManagerServiceStub {
    private static final int INSTALL_ALL_WHITELIST_RESTRICTED_PERMISSIONS = 4194304;
    private static final int INSTALL_FULL_APP = 16384;
    private static final int INSTALL_REASON_USER = 4;
    private static final String TAG = "Backup:BackupManagerServiceInjector";
    private static final String XMSF_PKG_NAME = "com.xiaomi.xmsf";
    private static HashMap<IBinder, DeathLinker> sBinderDeathLinker = new HashMap<>();

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<BackupManagerServiceInjector> {

        /* compiled from: BackupManagerServiceInjector$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final BackupManagerServiceInjector INSTANCE = new BackupManagerServiceInjector();
        }

        public BackupManagerServiceInjector provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public BackupManagerServiceInjector provideNewInstance() {
            return new BackupManagerServiceInjector();
        }
    }

    public boolean skipConfirmationUi(int token, String action) {
        IBackupManager bm = ServiceManager.getService("MiuiBackup");
        try {
            bm.startConfirmationUi(token, action);
            return true;
        } catch (RemoteException e) {
            Slog.e(TAG, "confirmation failed", e);
            return false;
        }
    }

    public void errorOccur(int errCode, int fd) {
        IBackupManager bm = ServiceManager.getService("MiuiBackup");
        try {
            if (bm.isRunningFromMiui(fd)) {
                bm.errorOccur(errCode);
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "errorOccur failed", e);
        }
    }

    public void errorOccur(int errCode, InputStream inputStream) {
        if (inputStream instanceof FileInputStream) {
            FileInputStream fileInputStream = (FileInputStream) inputStream;
            try {
                int fd = fileInputStream.getFD().getInt$();
                errorOccur(errCode, fd);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void writeMiuiBackupHeader(ParcelFileDescriptor out, int fd) {
        IBackupManager bm = ServiceManager.getService("MiuiBackup");
        try {
            if (bm.isRunningFromMiui(fd)) {
                bm.writeMiuiBackupHeader(out);
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "writeMiuiBackupHeader failed", e);
        }
    }

    public void readMiuiBackupHeader(ParcelFileDescriptor in, int fd) {
        IBackupManager bm = ServiceManager.getService("MiuiBackup");
        try {
            if (bm.isRunningFromMiui(fd)) {
                bm.readMiuiBackupHeader(in);
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "readMiuiBackupHeader failed", e);
        }
    }

    public void addRestoredSize(long size, int fd) {
        IBackupManager bm = ServiceManager.getService("MiuiBackup");
        try {
            if (bm.isRunningFromMiui(fd)) {
                bm.addCompletedSize(size);
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "addRestoredSize failed", e);
        }
    }

    public void onApkInstalled(int fd) {
        IBackupManager bm = ServiceManager.getService("MiuiBackup");
        try {
            if (bm.isRunningFromMiui(fd)) {
                bm.onApkInstalled();
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "onApkInstalled failed", e);
        }
    }

    /* loaded from: classes.dex */
    public class DeathLinker implements IBinder.DeathRecipient {
        private IBinder mAgentBinder;
        private int mCallerFd;
        private ParcelFileDescriptor mOutPipe;
        private int mToken = -1;

        public DeathLinker(IBinder agentBinder, int fd, ParcelFileDescriptor outPipe) {
            BackupManagerServiceInjector.this = r1;
            this.mAgentBinder = agentBinder;
            this.mCallerFd = fd;
            this.mOutPipe = outPipe;
        }

        public void setToken(int token) {
            this.mToken = token;
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            tearDownPipes();
            android.app.backup.IBackupManager bm = ServiceManager.getService("backup");
            try {
                bm.opComplete(this.mToken, 0L);
            } catch (RemoteException e) {
                Slog.e(BackupManagerServiceInjector.TAG, "binderDied failed", e);
            }
            BackupManagerServiceInjector.this.errorOccur(8, this.mCallerFd);
        }

        private void tearDownPipes() {
            ParcelFileDescriptor parcelFileDescriptor;
            IBackupManager bm = ServiceManager.getService("MiuiBackup");
            try {
                if (bm.isRunningFromMiui(this.mCallerFd) && (parcelFileDescriptor = this.mOutPipe) != null) {
                    try {
                        IoBridge.closeAndSignalBlockedThreads(parcelFileDescriptor.getFileDescriptor());
                        this.mOutPipe = null;
                    } catch (IOException e) {
                        Slog.w(BackupManagerServiceInjector.TAG, "Couldn't close agent pipes", e);
                    }
                }
            } catch (RemoteException e2) {
                Slog.e(BackupManagerServiceInjector.TAG, "errorOccur failed", e2);
            }
        }
    }

    public boolean needUpdateToken(IBackupAgent backupAgent, int token) {
        boolean needUpdateToken = false;
        if (backupAgent != null) {
            needUpdateToken = backupAgent.asBinder().isBinderAlive();
            DeathLinker deathLinker = sBinderDeathLinker.get(backupAgent.asBinder());
            if (deathLinker != null) {
                deathLinker.setToken(token);
            }
        }
        return needUpdateToken;
    }

    public void linkToDeath(IBackupAgent backupAgent, int fd, ParcelFileDescriptor outPipe) {
        if (backupAgent != null) {
            IBinder agentBinder = backupAgent.asBinder();
            DeathLinker deathLinker = new DeathLinker(agentBinder, fd, outPipe);
            sBinderDeathLinker.put(agentBinder, deathLinker);
            try {
                agentBinder.linkToDeath(deathLinker, 0);
            } catch (RemoteException e) {
                Slog.e(TAG, "linkToDeath failed", e);
            }
        }
    }

    public void unlinkToDeath(IBackupAgent backupAgent) {
        if (backupAgent != null) {
            IBinder agentBinder = backupAgent.asBinder();
            agentBinder.unlinkToDeath(sBinderDeathLinker.get(agentBinder), 0);
            sBinderDeathLinker.remove(agentBinder);
        }
    }

    public boolean isRunningFromMiui(int fd) {
        IBackupManager bm = ServiceManager.getService("MiuiBackup");
        try {
            if (bm.isRunningFromMiui(fd)) {
                return true;
            }
            return false;
        } catch (RemoteException e) {
            Slog.e(TAG, "isRunningFromMiui error", e);
            return false;
        }
    }

    public boolean isRunningFromMiui(InputStream inputStream) {
        if (inputStream instanceof FileInputStream) {
            FileInputStream fileInputStream = (FileInputStream) inputStream;
            try {
                int fd = fileInputStream.getFD().getInt$();
                return isRunningFromMiui(fd);
            } catch (IOException e) {
                Slog.e(TAG, "isRunningFromMiui error", e);
                return false;
            }
        }
        return false;
    }

    public boolean isSysAppRunningFromMiui(PackageInfo info, int fd) {
        return BackupManager.isSysAppForBackup(info) && isRunningFromMiui(fd);
    }

    public boolean isForceAllowBackup(PackageInfo info, int fd) {
        if (!Build.IS_INTERNATIONAL_BUILD || BackupManager.isSysAppForBackup(info)) {
            return isRunningFromMiui(fd);
        }
        return false;
    }

    public int getAppUserId(int fd, int def) {
        IBackupManager bm = ServiceManager.getService("MiuiBackup");
        try {
            if (bm.isRunningFromMiui(fd)) {
                return bm.getAppUserId();
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "getAppUserId failed", e);
        }
        return def;
    }

    public ApplicationInfo getApplicationInfo(Context context, String pkgName, int fd, int userIdDef) throws PackageManager.NameNotFoundException {
        IBackupManager bm = ServiceManager.getService("MiuiBackup");
        PackageManager pm = context.getPackageManager();
        try {
            if (bm.isRunningFromMiui(fd)) {
                int userId = bm.getAppUserId();
                if (BackupManager.isSysAppForBackup(context, pkgName)) {
                    return pm.getApplicationInfoAsUser(pkgName, 1024, userId);
                }
                return pm.getApplicationInfoAsUser(pkgName, 0, userId);
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "getApplicationInfo failed", e);
        }
        return pm.getApplicationInfoAsUser(pkgName, 0, userIdDef);
    }

    public PackageInfo getPackageInfo(Context context, String pkgName, int fd) throws PackageManager.NameNotFoundException {
        IBackupManager bm = ServiceManager.getService("MiuiBackup");
        PackageManager pm = context.getPackageManager();
        try {
            if (bm.isRunningFromMiui(fd)) {
                int userId = bm.getAppUserId();
                if (BackupManager.isSysAppForBackup(context, pkgName)) {
                    return pm.getPackageInfoAsUser(pkgName, 134218752, userId);
                }
                return pm.getPackageInfoAsUser(pkgName, ActivityStarterInjector.FLAG_ASSOCIATED_SETTINGS_AV, userId);
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "getPackageInfo failed", e);
        }
        return pm.getPackageInfo(pkgName, ActivityStarterInjector.FLAG_ASSOCIATED_SETTINGS_AV);
    }

    public boolean isNeedBeKilled(String pkg, int fd) {
        IBackupManager bm = ServiceManager.getService("MiuiBackup");
        try {
            if (!bm.isRunningFromMiui(fd)) {
                return true;
            }
            boolean is = bm.isNeedBeKilled(pkg);
            return is;
        } catch (RemoteException e) {
            Slog.e(TAG, "isNeedBeKilled failed", e);
            return true;
        }
    }

    public void restoreFileEnd(UserBackupManagerService thiz, IBackupAgent agent, android.app.backup.IBackupManager backupManagerBinder, int fd, Handler backupHandler, int restoreTimeoutMsg) {
        RemoteException e;
        if (agent != null) {
            IBackupManager bm = ServiceManager.getService("MiuiBackup");
            try {
                if (bm.isRunningFromMiui(fd)) {
                    int token = thiz.generateRandomIntegerToken();
                    backupHandler.removeMessages(restoreTimeoutMsg);
                    prepareOperationTimeout(thiz, token, 300000L, null, 1, fd);
                    agent.doRestoreFile((ParcelFileDescriptor) null, 0L, 0, BackupManager.DOMAIN_END, (String) null, 0L, 0L, token, backupManagerBinder);
                    try {
                        if (needUpdateToken(agent, token)) {
                            try {
                                thiz.waitUntilOperationComplete(token);
                            } catch (RemoteException e2) {
                                e = e2;
                                Slog.e(TAG, "restoreFileEnd failed", e);
                            }
                        }
                    } catch (RemoteException e3) {
                        e = e3;
                        Slog.e(TAG, "restoreFileEnd failed", e);
                    }
                }
            } catch (RemoteException e4) {
                e = e4;
            }
        }
    }

    public void routeSocketDataToOutput(ParcelFileDescriptor inPipe, OutputStream out, int outFd) throws IOException {
        IBackupManager bm = ServiceManager.getService("MiuiBackup");
        FileInputStream raw = null;
        DataInputStream in = null;
        try {
            try {
                try {
                    if (!bm.isRunningFromMiui(outFd)) {
                        FullBackupUtils.routeSocketDataToOutput(inPipe, out);
                    } else if (!bm.isCanceling()) {
                        raw = new FileInputStream(inPipe.getFileDescriptor());
                        in = new DataInputStream(raw);
                        byte[] buffer = new byte[32768];
                        while (true) {
                            int readInt = in.readInt();
                            int chunkTotal = readInt;
                            if (readInt <= 0) {
                                break;
                            }
                            while (chunkTotal > 0) {
                                int toRead = chunkTotal > buffer.length ? buffer.length : chunkTotal;
                                int nRead = in.read(buffer, 0, toRead);
                                if (nRead < 0) {
                                    Slog.e(TAG, "Unexpectedly reached end of file while reading data");
                                    throw new EOFException();
                                }
                                out.write(buffer, 0, nRead);
                                bm.addCompletedSize(nRead);
                                chunkTotal -= nRead;
                            }
                        }
                    } else {
                        if (0 != 0) {
                            raw.close();
                        }
                        if (0 == 0) {
                            return;
                        }
                        in.close();
                        return;
                    }
                    if (raw != null) {
                        raw.close();
                    }
                    if (in == null) {
                        return;
                    }
                } catch (ArrayIndexOutOfBoundsException e) {
                    Slog.e(TAG, "routeSocketDataToOutput failed", e);
                    throw new IOException();
                }
            } catch (RemoteException | InterruptedIOException e2) {
                Slog.e(TAG, "routeSocketDataToOutput failed", e2);
                if (0 != 0) {
                    raw.close();
                }
                if (0 == 0) {
                    return;
                }
            }
            in.close();
        } catch (Throwable th) {
            if (0 != 0) {
                raw.close();
            }
            if (0 != 0) {
                in.close();
            }
            throw th;
        }
    }

    public void setInputFileDescriptor(FullRestoreEngine engine, int fd) {
        if (engine != null) {
            engine.mInputFD = fd;
        }
    }

    public void setOutputFileDescriptor(FullBackupEngine engine, int fd) {
        if (engine != null) {
            engine.mOutputFD = fd;
        }
    }

    public void setOutputFileDescriptor(PerformAdbBackupTask task, ParcelFileDescriptor fileDescriptor) {
        int fd;
        if (task != null) {
            try {
                fd = fileDescriptor.getFd();
            } catch (Exception e) {
                Slog.e(TAG, "setOutputFileDescriptor failed", e);
                fd = -2;
            }
            task.mOutputFD = fd;
        }
    }

    public boolean tearDownAgentAndKill(IActivityManager activityManager, ApplicationInfo appInfo, int fd) {
        boolean handle = false;
        IBackupManager bm = ServiceManager.getService("MiuiBackup");
        try {
            if (!bm.isRunningFromMiui(fd) || isNeedBeKilled(appInfo.packageName, fd)) {
                return false;
            }
            handle = true;
            activityManager.unbindBackupAgent(appInfo);
            return true;
        } catch (RemoteException e) {
            Slog.e(TAG, "isNeedBeKilled failed", e);
            return handle;
        }
    }

    public void prepareOperationTimeout(UserBackupManagerService thiz, int token, long interval, BackupRestoreTask callback, int operationType, int fd) {
        IBackupManager bm = ServiceManager.getService("MiuiBackup");
        int backupTimeoutScale = 1;
        try {
            if (bm.isRunningFromMiui(fd)) {
                backupTimeoutScale = bm.getBackupTimeoutScale();
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "prepareOperationTimeout failed", e);
        }
        Slog.d(TAG, "prepareOperationTimeout backupTimeoutScale = " + backupTimeoutScale);
        thiz.prepareOperationTimeout(token, backupTimeoutScale * interval, callback, operationType);
    }

    public void doFullBackup(IBackupAgent agent, ParcelFileDescriptor pipe, long quotaBytes, int token, android.app.backup.IBackupManager backupManagerBinder, int transportFlags, int fd) throws RemoteException {
        IBackupManager bm = ServiceManager.getService("MiuiBackup");
        if (!bm.isRunningFromMiui(fd) || !bm.shouldSkipData()) {
            agent.doFullBackup(pipe, quotaBytes, token, backupManagerBinder, transportFlags);
            return;
        }
        Slog.i(TAG, "skip app data");
        FileOutputStream out = null;
        try {
            try {
                try {
                    out = new FileOutputStream(pipe.getFileDescriptor());
                    byte[] buf = new byte[4];
                    out.write(buf);
                    out.close();
                } catch (IOException e) {
                    Slog.e(TAG, "Unable to finalize backup stream!");
                    if (out != null) {
                        out.close();
                    }
                }
            } catch (Throwable th) {
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e2) {
                        e2.printStackTrace();
                    }
                }
                throw th;
            }
        } catch (IOException e3) {
            e3.printStackTrace();
        }
        backupManagerBinder.opComplete(token, 0L);
    }

    public void waitingBeforeGetAgent() {
        try {
            Thread.sleep(200L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public boolean isCanceling(int fd) {
        IBackupManager bm = ServiceManager.getService("MiuiBackup");
        try {
            if (bm.isRunningFromMiui(fd)) {
                return bm.isCanceling();
            }
            return false;
        } catch (RemoteException e) {
            Slog.e(TAG, "isCanceling error", e);
            return false;
        }
    }

    public void cancelBackups(UserBackupManagerService thiz) {
        ApplicationInfo mTargetAppInfo;
        long oldToken = Binder.clearCallingIdentity();
        try {
            List<Integer> operationsToCancel = new ArrayList<>();
            OperationStorage operationStorage = thiz.getOperationStorage();
            Set<Integer> backupWaitOperations = operationStorage.operationTokensForOpType(0);
            Set<Integer> restoreWaitOperations = operationStorage.operationTokensForOpType(1);
            operationsToCancel.addAll(backupWaitOperations);
            operationsToCancel.addAll(restoreWaitOperations);
            for (Integer token : operationsToCancel) {
                thiz.handleCancel(token.intValue(), true);
            }
            try {
                IBackupManager bm = ServiceManager.getService("MiuiBackup");
                String targetPkj = bm.getCurrentRunningPackage();
                Context context = thiz.getContext();
                PackageManager pm = context.getPackageManager();
                Slog.i(TAG, "sBinderDeathLinker size:" + sBinderDeathLinker.size());
                if (!BackupManager.isSysAppForBackup(context, targetPkj)) {
                    mTargetAppInfo = pm.getApplicationInfoAsUser(targetPkj, 0, thiz.getUserId());
                } else {
                    mTargetAppInfo = pm.getApplicationInfoAsUser(targetPkj, 1024, thiz.getUserId());
                }
                thiz.tearDownAgentAndKill(mTargetAppInfo);
            } catch (Exception e) {
                Slog.e(TAG, "cancelBackups error", e);
            }
        } finally {
            Binder.restoreCallingIdentity(oldToken);
        }
    }

    public boolean isXSpaceUser(int userId) {
        return userId == 999;
    }

    public boolean isXSpaceUserRunning() {
        try {
            IActivityManager activityManager = ActivityManager.getService();
            return activityManager.isUserRunning(999, 0);
        } catch (RemoteException e) {
            return false;
        }
    }

    public boolean installExistingPackageAsUser(String pkgName, int userId) {
        try {
            IPackageManager packageManager = AppGlobals.getPackageManager();
            if (BackupProxyHelper.isAppInXSpace(packageManager, pkgName)) {
                Slog.d(TAG, "has been installed, pkgName:" + pkgName + ", userId:" + userId);
                return true;
            }
            if (BackupProxyHelper.isMiPushRequired(packageManager, pkgName) && !BackupProxyHelper.isAppInXSpace(packageManager, XMSF_PKG_NAME)) {
                packageManager.installExistingPackageAsUser(XMSF_PKG_NAME, userId, 4210688, 4, (List) null);
                Slog.d(TAG, "Require XMSF, auto installed! ");
            }
            int result = packageManager.installExistingPackageAsUser(pkgName, userId, 4210688, 4, (List) null);
            return result == 1;
        } catch (RemoteException e) {
            Slog.e(TAG, "Unable to install package: " + pkgName, e);
            return false;
        }
    }

    public int hookUserIdForXSpace(int userID) {
        if (isXSpaceUser(userID)) {
            return 0;
        }
        return userID;
    }

    public void registerHuanjiReceiver(UserBackupManagerService service, Context context) {
        Log.i(TAG, "registerHuanjiReceiver.");
        new HuanjiReceiver(service).register(context);
    }

    /* loaded from: classes.dex */
    public static class HuanjiReceiver extends BroadcastReceiver {
        static final String ACTION_HUANJI_END_RESTORE = "com.miui.huanji.END_RESTORE";
        static final String ACTION_HUANJI_RESTORE = "com.miui.huanji.START_RESTORE";
        private static final String TAG = "HuanjiReceiver";
        private UserBackupManagerService mUserBackupManagerService;

        public HuanjiReceiver(UserBackupManagerService userBackupManagerService) {
            this.mUserBackupManagerService = userBackupManagerService;
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) {
                return;
            }
            Log.i(TAG, "get action: " + action);
            char c = 65535;
            switch (action.hashCode()) {
                case -1157256499:
                    if (action.equals(ACTION_HUANJI_RESTORE)) {
                        c = 0;
                        break;
                    }
                    break;
                case 2016774470:
                    if (action.equals(ACTION_HUANJI_END_RESTORE)) {
                        c = 1;
                        break;
                    }
                    break;
            }
            switch (c) {
                case 0:
                    this.mUserBackupManagerService.setSetupComplete(true);
                    return;
                case 1:
                    this.mUserBackupManagerService.setSetupComplete(false);
                    return;
                default:
                    return;
            }
        }

        public void register(Context context) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(ACTION_HUANJI_RESTORE);
            filter.addAction(ACTION_HUANJI_END_RESTORE);
            context.registerReceiver(this, filter, "android.permission.BACKUP", null);
        }

        public void unregister(Context context) {
            context.unregisterReceiver(this);
        }
    }
}
