package com.miui.server;

import android.app.ActivityManager;
import android.app.AppGlobals;
import android.app.backup.IFullBackupRestoreObserver;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.IPackageManager;
import android.content.pm.IPackageStatsObserver;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageStats;
import android.content.pm.SuspendDialogInfo;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.FileUtils;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.os.PersistableBundle;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Slog;
import com.android.internal.content.PackageMonitor;
import com.android.server.SystemService;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import libcore.io.IoBridge;
import miui.app.backup.BackupFileResolver;
import miui.app.backup.BackupManager;
import miui.app.backup.IBackupManager;
import miui.app.backup.IBackupServiceStateObserver;
import miui.app.backup.IPackageBackupRestoreObserver;
/* loaded from: classes.dex */
public class BackupManagerService extends IBackupManager.Stub {
    private static final int COMPONENT_ENABLED_STATE_NONE = -1;
    public static final int FD_CLOSE = -2;
    public static final int FD_NONE = -1;
    private static final int PID_NONE = -1;
    private static final String TAG = "Backup:BackupManagerService";
    private ActivityManager mActivityManager;
    private IPackageBackupRestoreObserver mBackupRestoreObserver;
    private Context mContext;
    private long mCurrentCompletedSize;
    private long mCurrentTotalSize;
    private int mCurrentWorkingFeature;
    private String mCurrentWorkingPkg;
    private String mEncryptedPwd;
    private String mEncryptedPwdInBakFile;
    private Handler mHandler;
    private HandlerThread mHandlerThread;
    private int mLastError;
    private int mPackageLastEnableState;
    private PackageManager mPackageManager;
    private String mPreviousWorkingPkg;
    private int mProgType;
    private String mPwd;
    private boolean mShouldSkipData;
    private HashMap<String, Boolean> mNeedBeKilledPkgs = new HashMap<>();
    private RemoteCallbackList<IBackupServiceStateObserver> mStateObservers = new RemoteCallbackList<>();
    private int mOwnerPid = -1;
    private IBinder mICaller = null;
    private DeathLinker mDeathLinker = new DeathLinker();
    private AtomicBoolean mTaskLatch = null;
    private volatile int mCallerFd = -1;
    private boolean mIsCanceling = false;
    private ParcelFileDescriptor mOutputFile = null;
    private int mState = 0;
    private int mAppUserId = 0;
    private final AtomicBoolean mPkgChangingLock = new AtomicBoolean(false);
    PackageMonitor mPackageMonitor = new PackageMonitor() { // from class: com.miui.server.BackupManagerService.1
        public boolean onPackageChanged(String packageName, int uid, String[] components) {
            if (packageName != null && packageName.equals(BackupManagerService.this.mCurrentWorkingPkg)) {
                synchronized (BackupManagerService.this.mPkgChangingLock) {
                    if (BackupManagerService.this.mPkgChangingLock.get()) {
                        BackupManagerService.this.mPkgChangingLock.set(false);
                        BackupManagerService.this.mPkgChangingLock.notify();
                    }
                }
                return true;
            }
            return true;
        }

        public void onPackagesSuspended(String[] packages) {
            super.onPackagesSuspended(packages);
            if (packages != null) {
                for (String name : packages) {
                    if (name.equals(BackupManagerService.this.mCurrentWorkingPkg)) {
                        synchronized (BackupManagerService.this.mPkgChangingLock) {
                            if (BackupManagerService.this.mPkgChangingLock.get()) {
                                BackupManagerService.this.mPkgChangingLock.set(false);
                                BackupManagerService.this.mPkgChangingLock.notify();
                            }
                        }
                    }
                }
            }
        }
    };
    private IPackageStatsObserver mPackageStatsObserver = new IPackageStatsObserver() { // from class: com.miui.server.BackupManagerService.4
        public void onGetStatsCompleted(PackageStats pStats, boolean succeeded) throws RemoteException {
            String pkg = pStats.packageName;
            try {
                PackageInfo pi = BackupManagerService.this.mPackageManager.getPackageInfoAsUser(pkg, 0, BackupManagerService.this.mAppUserId);
                if (BackupManager.isSysAppForBackup(BackupManagerService.this.mContext, pkg)) {
                    BackupManagerService.this.mCurrentTotalSize = pStats.dataSize;
                } else {
                    BackupManagerService.this.mCurrentTotalSize = new File(pi.applicationInfo.sourceDir).length() + pStats.dataSize;
                }
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }

        public IBinder asBinder() {
            return null;
        }
    };
    private IPackageManager mPackageManagerBinder = AppGlobals.getPackageManager();

    /* loaded from: classes.dex */
    public static final class Lifecycle extends SystemService {
        private final BackupManagerService mService;

        public Lifecycle(Context context) {
            super(context);
            this.mService = new BackupManagerService(context);
        }

        public void onStart() {
            Slog.i(BackupManagerService.TAG, "init, onStart");
            publishBinderService("MiuiBackup", this.mService);
        }

        public void onBootPhase(int phase) {
            if (phase == 550) {
                try {
                    this.mService.restoreLastPackageEnableState(BackupManagerService.getPackageEnableStateFile());
                } catch (Exception e) {
                    Slog.e(BackupManagerService.TAG, "restoreLastPackageEnableState", e);
                }
            }
        }
    }

    public BackupManagerService(Context context) {
        this.mContext = context;
        this.mPackageManager = context.getPackageManager();
        this.mActivityManager = (ActivityManager) context.getSystemService("activity");
        HandlerThread handlerThread = new HandlerThread("MiuiBackup", 10);
        this.mHandlerThread = handlerThread;
        handlerThread.start();
        this.mHandler = new BackupHandler(this.mHandlerThread.getLooper());
    }

    public void backupPackage(ParcelFileDescriptor outFileDescriptor, ParcelFileDescriptor readSide, String pkg, int feature, String pwd, String encryptedPwd, boolean includeApk, boolean forceBackup, boolean shouldSkipData, boolean isXSpace, IPackageBackupRestoreObserver observer) throws RemoteException {
        this.mBackupRestoreObserver = observer;
        int pid = Binder.getCallingPid();
        if (pid != this.mOwnerPid) {
            Slog.e(TAG, "You must acquire first to use the backup or restore service");
            errorOccur(9);
        } else if (this.mICaller == null) {
            Slog.e(TAG, "Caller is null You must acquire first with a binder");
            errorOccur(9);
        } else {
            if (!TextUtils.isEmpty(pwd) && !TextUtils.isEmpty(encryptedPwd)) {
                this.mPwd = pwd;
                this.mEncryptedPwd = encryptedPwd;
            }
            String defaultIme = getDefaultIme(this.mContext);
            boolean isSystemApp = BackupManager.isSysAppForBackup(this.mContext, pkg);
            Slog.v(TAG, "backupPackage: pkg=" + pkg + " feature=" + feature + " includeApk=" + includeApk + " shouldSkipData=" + shouldSkipData + " isXSpace=" + isXSpace + " isSystemApp=" + isSystemApp);
            int i = isXSpace ? 999 : 0;
            this.mAppUserId = i;
            this.mPackageLastEnableState = -1;
            this.mCurrentWorkingPkg = pkg;
            if (!isSystemApp) {
                disablePackageAndWait(pkg, i);
            }
            this.mOutputFile = outFileDescriptor;
            this.mShouldSkipData = shouldSkipData;
            this.mCurrentWorkingFeature = feature;
            this.mLastError = 0;
            this.mProgType = 0;
            this.mState = 1;
            this.mCurrentCompletedSize = 0L;
            this.mCurrentTotalSize = -1L;
            BackupManagerServiceProxy.getPackageSizeInfo(this.mContext, this.mPackageManager, pkg, this.mAppUserId, this.mPackageStatsObserver);
            synchronized (this) {
                this.mTaskLatch = new AtomicBoolean(false);
                this.mCallerFd = outFileDescriptor.getFd();
                Slog.d(TAG, "backupPackage, MIUI FD is " + this.mCallerFd);
            }
            synchronized (this.mTaskLatch) {
                if (this.mOwnerPid != -1) {
                    BackupManagerServiceProxy.fullBackup(outFileDescriptor, new String[]{pkg}, includeApk);
                } else {
                    errorOccur(10);
                }
                Slog.d(TAG, "backupPackage finish, MIUI FD is " + this.mCallerFd);
                this.mTaskLatch.set(true);
                this.mTaskLatch.notifyAll();
            }
            if (!isSystemApp) {
                enablePackage(pkg, this.mAppUserId, defaultIme);
            }
            this.mPwd = null;
            this.mEncryptedPwd = null;
            this.mTaskLatch = null;
            this.mCallerFd = -1;
            this.mOutputFile = null;
            this.mProgType = 0;
            this.mState = 0;
            this.mPackageLastEnableState = -1;
            this.mCurrentTotalSize = -1L;
            this.mCurrentCompletedSize = 0L;
            this.mPreviousWorkingPkg = this.mCurrentWorkingPkg;
            this.mAppUserId = 0;
        }
    }

    @Deprecated
    public void setFutureTask(List<String> packages) {
    }

    private boolean isApplicationInstalled(String packageName, int userId) {
        List<PackageInfo> installedList = this.mPackageManager.getInstalledPackagesAsUser(0, userId);
        boolean isInstalled = false;
        int i = 0;
        while (true) {
            if (i >= installedList.size()) {
                break;
            } else if (!installedList.get(i).packageName.equals(packageName)) {
                i++;
            } else {
                isInstalled = true;
                break;
            }
        }
        Slog.d(TAG, "isApplicationInstalled, packageName:" + packageName + " isInstalled:" + isInstalled);
        return isInstalled;
    }

    public void startConfirmationUi(final int token, String action) throws RemoteException {
        Handler handler = this.mHandler;
        Runnable runnable = new Runnable() { // from class: com.miui.server.BackupManagerService.2
            @Override // java.lang.Runnable
            public void run() {
                android.app.backup.IBackupManager bm = ServiceManager.getService("backup");
                try {
                    bm.acknowledgeFullBackupOrRestore(token, true, "", BackupManagerService.this.mPwd, new FullBackupRestoreObserver());
                } catch (RemoteException e) {
                    Slog.e(BackupManagerService.TAG, "acknowledgeFullBackupOrRestore failed", e);
                }
            }
        };
        String str = this.mPreviousWorkingPkg;
        handler.postDelayed(runnable, (str == null || !str.equals(this.mCurrentWorkingPkg)) ? 100L : 1500L);
    }

    /* JADX WARN: Removed duplicated region for block: B:29:0x0038 A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public void writeMiuiBackupHeader(android.os.ParcelFileDescriptor r9) {
        /*
            r8 = this;
            r0 = 0
            java.io.FileOutputStream r1 = new java.io.FileOutputStream     // Catch: java.lang.Throwable -> L26 java.io.IOException -> L2b
            java.io.FileDescriptor r2 = r9.getFileDescriptor()     // Catch: java.lang.Throwable -> L26 java.io.IOException -> L2b
            r1.<init>(r2)     // Catch: java.lang.Throwable -> L26 java.io.IOException -> L2b
            android.content.Context r2 = r8.mContext     // Catch: java.io.IOException -> L24 java.lang.Throwable -> L35
            java.lang.String r3 = r8.mCurrentWorkingPkg     // Catch: java.io.IOException -> L24 java.lang.Throwable -> L35
            int r4 = r8.mCurrentWorkingFeature     // Catch: java.io.IOException -> L24 java.lang.Throwable -> L35
            java.lang.String r5 = r8.mEncryptedPwd     // Catch: java.io.IOException -> L24 java.lang.Throwable -> L35
            int r6 = r8.mAppUserId     // Catch: java.io.IOException -> L24 java.lang.Throwable -> L35
            miui.app.backup.BackupFileResolver.writeMiuiHeader(r1, r2, r3, r4, r5, r6)     // Catch: java.io.IOException -> L24 java.lang.Throwable -> L35
            r1.close()     // Catch: java.io.IOException -> L1d
            return
        L1d:
            r0 = move-exception
            java.lang.RuntimeException r2 = new java.lang.RuntimeException
            r2.<init>(r0)
            throw r2
        L24:
            r0 = move-exception
            goto L2f
        L26:
            r1 = move-exception
            r7 = r1
            r1 = r0
            r0 = r7
            goto L36
        L2b:
            r1 = move-exception
            r7 = r1
            r1 = r0
            r0 = r7
        L2f:
            java.lang.RuntimeException r2 = new java.lang.RuntimeException     // Catch: java.lang.Throwable -> L35
            r2.<init>(r0)     // Catch: java.lang.Throwable -> L35
            throw r2     // Catch: java.lang.Throwable -> L35
        L35:
            r0 = move-exception
        L36:
            if (r1 == 0) goto L43
            r1.close()     // Catch: java.io.IOException -> L3c
            goto L43
        L3c:
            r0 = move-exception
            java.lang.RuntimeException r2 = new java.lang.RuntimeException
            r2.<init>(r0)
            throw r2
        L43:
            throw r0
        */
        throw new UnsupportedOperationException("Method not decompiled: com.miui.server.BackupManagerService.writeMiuiBackupHeader(android.os.ParcelFileDescriptor):void");
    }

    public void readMiuiBackupHeader(ParcelFileDescriptor inFileDescriptor) {
        InputStream in = null;
        try {
            InputStream in2 = new FileInputStream(inFileDescriptor.getFileDescriptor());
            BackupFileResolver.BackupFileMiuiHeader header = BackupFileResolver.readMiuiHeader(in2);
            if (header == null || header.version != 2) {
                Slog.e(TAG, "readMiuiBackupHeader is error, header=" + header);
            } else {
                this.mCurrentWorkingPkg = header.packageName;
                this.mCurrentWorkingFeature = header.featureId;
                this.mAppUserId = header.userId;
                this.mEncryptedPwdInBakFile = header.isEncrypted ? null : header.encryptedPwd;
                boolean isSystemApp = BackupManager.isSysAppForBackup(this.mContext, this.mCurrentWorkingPkg);
                Slog.d(TAG, "readMiuiBackupHeader, BackupFileMiuiHeader:" + header + " isSystemApp:" + isSystemApp + " mAppUserId:" + this.mAppUserId);
                if (!isSystemApp) {
                    disablePackageAndWait(this.mCurrentWorkingPkg, this.mAppUserId);
                }
            }
            try {
                in2.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } catch (Throwable e2) {
            if (0 != 0) {
                try {
                    in.close();
                } catch (IOException e3) {
                    throw new RuntimeException(e3);
                }
            }
            throw e2;
        }
    }

    public void addCompletedSize(long size) {
        IPackageBackupRestoreObserver iPackageBackupRestoreObserver;
        long j = this.mCurrentCompletedSize + size;
        this.mCurrentCompletedSize = j;
        if (this.mProgType == 0 && (iPackageBackupRestoreObserver = this.mBackupRestoreObserver) != null) {
            try {
                iPackageBackupRestoreObserver.onCustomProgressChange(this.mCurrentWorkingPkg, this.mCurrentWorkingFeature, 0, j, this.mCurrentTotalSize);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public void setIsNeedBeKilled(String pkg, boolean isNeedBeKilled) throws RemoteException {
        Slog.d(TAG, "setIsNeedBeKilled, pkg=" + pkg + ", isNeedBeKilled=" + isNeedBeKilled);
        this.mNeedBeKilledPkgs.put(pkg, Boolean.valueOf(isNeedBeKilled));
    }

    public boolean isNeedBeKilled(String pkg) throws RemoteException {
        Boolean isKilled = this.mNeedBeKilledPkgs.get(pkg);
        if (isKilled != null) {
            return isKilled.booleanValue();
        }
        return true;
    }

    public boolean isRunningFromMiui(int fd) throws RemoteException {
        return this.mCallerFd == fd;
    }

    public boolean isServiceIdle() {
        return this.mState == 0;
    }

    public void setCustomProgress(int progType, int prog, int total) throws RemoteException {
        this.mProgType = progType;
        IPackageBackupRestoreObserver iPackageBackupRestoreObserver = this.mBackupRestoreObserver;
        if (iPackageBackupRestoreObserver != null) {
            iPackageBackupRestoreObserver.onCustomProgressChange(this.mCurrentWorkingPkg, this.mCurrentWorkingFeature, progType, prog, total);
        }
    }

    private static String readHeaderLine(InputStream in) throws IOException {
        StringBuilder buffer = new StringBuilder(80);
        while (true) {
            int c = in.read();
            if (c < 0 || c == 10) {
                break;
            }
            buffer.append((char) c);
        }
        return buffer.toString();
    }

    public void restoreFile(ParcelFileDescriptor bakFd, String pwd, boolean forceBackup, IPackageBackupRestoreObserver observer) throws RemoteException {
        this.mBackupRestoreObserver = observer;
        int pid = getCallingPid();
        if (pid != this.mOwnerPid) {
            Slog.e(TAG, "You must acquire first to use the backup or restore service");
            errorOccur(9);
        } else if (this.mICaller == null) {
            Slog.e(TAG, "Caller is null You must acquire first with a binder");
            errorOccur(9);
        } else {
            String defaultIme = getDefaultIme(this.mContext);
            this.mPwd = pwd;
            this.mLastError = 0;
            this.mProgType = 0;
            this.mPackageLastEnableState = -1;
            this.mState = 2;
            this.mCurrentTotalSize = bakFd.getStatSize();
            this.mCurrentCompletedSize = 0L;
            synchronized (this) {
                this.mTaskLatch = new AtomicBoolean(false);
                this.mCallerFd = bakFd.getFd();
                Slog.d(TAG, "restoreFile, MIUI FD is " + this.mCallerFd);
            }
            synchronized (this.mTaskLatch) {
                if (this.mOwnerPid != -1) {
                    BackupManagerServiceProxy.fullRestore(bakFd);
                } else {
                    errorOccur(10);
                }
                Slog.d(TAG, "restoreFile finish, MIUI FD is " + this.mCallerFd);
                this.mTaskLatch.set(true);
                this.mTaskLatch.notifyAll();
            }
            if (!BackupManager.isSysAppForBackup(this.mContext, this.mCurrentWorkingPkg)) {
                enablePackage(this.mCurrentWorkingPkg, this.mAppUserId, defaultIme);
            }
            this.mPwd = null;
            this.mEncryptedPwd = null;
            this.mTaskLatch = null;
            this.mCallerFd = -1;
            this.mProgType = 0;
            this.mState = 0;
            this.mPackageLastEnableState = -1;
            this.mCurrentTotalSize = -1L;
            this.mPreviousWorkingPkg = this.mCurrentWorkingPkg;
            this.mAppUserId = 0;
        }
    }

    public boolean acquire(IBackupServiceStateObserver stateObserver, IBinder iCaller) throws RemoteException {
        if (iCaller == null) {
            Slog.w(TAG, "caller should not be null");
            return false;
        }
        Slog.d(TAG, "Client tries to acquire service. CallingPid=" + Binder.getCallingPid() + " mOwnerPid=" + this.mOwnerPid);
        synchronized (this) {
            Slog.d(TAG, "Client acquire service. ");
            if (this.mOwnerPid == -1) {
                this.mOwnerPid = Binder.getCallingPid();
                this.mICaller = iCaller;
                iCaller.linkToDeath(this.mDeathLinker, 0);
                return true;
            }
            if (stateObserver != null) {
                this.mStateObservers.register(stateObserver);
            }
            return false;
        }
    }

    public void release(IBackupServiceStateObserver stateObserver) throws RemoteException {
        Slog.d(TAG, "Client tries to release service. CallingPid=" + Binder.getCallingPid() + " mOwnerPid=" + this.mOwnerPid);
        synchronized (this) {
            Slog.d(TAG, "Client release service. Start canceling...");
            if (stateObserver != null) {
                this.mStateObservers.unregister(stateObserver);
            }
            int pid = Binder.getCallingPid();
            if (pid == this.mOwnerPid) {
                this.mIsCanceling = true;
                scheduleReleaseResource();
                waitForTheLastWorkingTask();
                this.mIsCanceling = false;
                this.mOwnerPid = -1;
                this.mICaller.unlinkToDeath(this.mDeathLinker, 0);
                this.mICaller = null;
                this.mPreviousWorkingPkg = null;
                try {
                    broadcastServiceIdle();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
            Slog.d(TAG, "Client release service. Cancel completed!");
        }
    }

    public void scheduleReleaseResource() {
        AtomicBoolean atomicBoolean = this.mTaskLatch;
        if (atomicBoolean != null && !atomicBoolean.get()) {
            if (Build.VERSION.SDK_INT >= 26) {
                try {
                    BackupManagerServiceProxy.fullCancel();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                closeBackupWriteStream(this.mOutputFile);
                return;
            }
            releaseBackupWriteStream(this.mOutputFile);
        }
    }

    public void broadcastServiceIdle() throws RemoteException {
        synchronized (this) {
            int cnt = this.mStateObservers.beginBroadcast();
            for (int i = 0; i < cnt; i++) {
                this.mStateObservers.getBroadcastItem(i).onServiceStateIdle();
            }
            this.mStateObservers.finishBroadcast();
        }
    }

    public void waitForTheLastWorkingTask() {
        AtomicBoolean atomicBoolean = this.mTaskLatch;
        if (atomicBoolean != null) {
            synchronized (atomicBoolean) {
                while (true) {
                    AtomicBoolean atomicBoolean2 = this.mTaskLatch;
                    if (atomicBoolean2 == null || atomicBoolean2.get()) {
                        break;
                    }
                    try {
                        this.mTaskLatch.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private boolean checkPackageAvailable(String pkg, int userId) {
        if (!isApplicationInstalled(pkg, userId) || BackupManagerServiceProxy.isPackageStateProtected(this.mPackageManager, pkg, userId)) {
            return false;
        }
        return true;
    }

    private void enablePackage(String pkg, int userId, final String defaultIme) {
        if (!checkPackageAvailable(pkg, userId)) {
            return;
        }
        if (this.mPackageLastEnableState == -1) {
            this.mPackageLastEnableState = 0;
        }
        Slog.d(TAG, "enablePackage, pkg:" + pkg + " userId:" + userId + ", state:" + this.mPackageLastEnableState + ", defaultIme:" + defaultIme);
        if (isDefaultIme(pkg, defaultIme)) {
            setApplicationEnabledSetting(pkg, userId, this.mPackageLastEnableState, 0);
            this.mHandler.postDelayed(new Runnable() { // from class: com.miui.server.BackupManagerService.3
                @Override // java.lang.Runnable
                public void run() {
                    Settings.Secure.putString(BackupManagerService.this.mContext.getContentResolver(), "default_input_method", defaultIme);
                }
            }, 2000L);
            return;
        }
        setApplicationEnabledSetting(pkg, userId, this.mPackageLastEnableState, 1);
        File pkgStateFile = getPackageEnableStateFile();
        pkgStateFile.delete();
    }

    private void disablePackageAndWait(String pkg, int userId) {
        if (!checkPackageAvailable(pkg, userId)) {
            return;
        }
        int applicationSetting = getApplicationEnabledSetting(pkg, userId);
        if (this.mPackageLastEnableState == -1) {
            this.mPackageLastEnableState = applicationSetting;
        }
        Slog.d(TAG, "disablePackageAndWait, pkg:" + pkg + " userId:" + userId + ", state:" + applicationSetting + ", lastState:" + this.mPackageLastEnableState);
        saveCurrentPackageEnableState(getPackageEnableStateFile(), pkg, this.mPackageLastEnableState, userId);
        if (applicationSetting == 2) {
            return;
        }
        try {
            PackageMonitor packageMonitor = this.mPackageMonitor;
            Context context = this.mContext;
            packageMonitor.register(context, context.getMainLooper(), false);
            long waitStartTime = SystemClock.elapsedRealtime();
            synchronized (this.mPkgChangingLock) {
                try {
                    this.mPkgChangingLock.set(true);
                    setApplicationEnabledSetting(pkg, userId, 2, 0);
                    this.mPkgChangingLock.wait(5000L);
                    this.mPkgChangingLock.set(false);
                } catch (InterruptedException e) {
                    Slog.e(TAG, "mPkgChangingLock wait error", e);
                }
            }
            Slog.i(TAG, "setApplicationEnabledSetting wait time=" + (SystemClock.elapsedRealtime() - waitStartTime) + ", pkg=" + pkg);
            this.mPackageMonitor.unregister();
            ((ActivityManager) this.mContext.getSystemService("activity")).forceStopPackage(pkg);
            waitUntilAppKilled(pkg);
        } catch (Throwable th) {
            this.mPackageMonitor.unregister();
            throw th;
        }
    }

    private void waitUntilAppKilled(String pkg) {
        boolean killed;
        int round = 0;
        ActivityManager am = (ActivityManager) this.mContext.getSystemService("activity");
        while (true) {
            killed = true;
            List<ActivityManager.RunningAppProcessInfo> procInfos = am.getRunningAppProcesses();
            Iterator<ActivityManager.RunningAppProcessInfo> it = procInfos.iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                ActivityManager.RunningAppProcessInfo procInfo = it.next();
                if (procInfo.processName.equals(pkg) || procInfo.processName.startsWith(pkg + ":")) {
                    if (this.mAppUserId == UserHandle.getUserId(procInfo.uid)) {
                        killed = false;
                        break;
                    }
                }
            }
            if (killed) {
                break;
            }
            try {
                Thread.sleep(500L);
                int round2 = round + 1;
                if (round >= 20) {
                    break;
                }
                round = round2;
            } catch (InterruptedException e) {
                Slog.e(TAG, "interrupted while waiting", e);
            }
        }
        if (killed) {
            Slog.i(TAG, "app: " + pkg + " is killed. continue our routine.");
        } else {
            Slog.w(TAG, "continue while app: " + pkg + " is still alive!");
        }
    }

    public static File getPackageEnableStateFile() {
        File systemDir = new File(Environment.getDataDirectory(), "system");
        File pkgStateFile = new File(systemDir, "backup_pkg_enable_state");
        return pkgStateFile;
    }

    public static File getCachedInstallFile() {
        File sysDir = new File(Environment.getDataDirectory(), "system");
        File cachedFile = new File(sysDir, "restoring_cached_file");
        return cachedFile;
    }

    private void saveCurrentPackageEnableState(File pkgStateFile, String pkg, int state, int userId) {
        FileOutputStream out = null;
        try {
            try {
                try {
                    out = new FileOutputStream(pkgStateFile);
                    StringBuilder sb = new StringBuilder(pkg);
                    sb.append(" ").append(state);
                    sb.append(" ").append(userId);
                    out.write(sb.toString().getBytes());
                    out.close();
                } catch (IOException e) {
                    Slog.e(TAG, "IOException", e);
                    if (out != null) {
                        out.close();
                    }
                }
            } catch (Throwable th) {
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e2) {
                        Slog.e(TAG, "IOException", e2);
                    }
                }
                throw th;
            }
        } catch (IOException e3) {
            Slog.e(TAG, "IOException", e3);
        }
    }

    public void restoreLastPackageEnableState(File pkgStateFile) {
        IOException e;
        File cachedFile = getCachedInstallFile();
        if (cachedFile.exists()) {
            cachedFile.delete();
        }
        if (pkgStateFile.exists()) {
            FileInputStream in = null;
            String pkg = null;
            int state = Integer.MIN_VALUE;
            int userId = 0;
            try {
            } catch (Throwable th) {
                th = th;
            }
            try {
                try {
                } catch (IOException e2) {
                    Slog.e(TAG, "IOEception", e2);
                }
                try {
                    FileInputStream in2 = new FileInputStream(pkgStateFile);
                    ArrayList<Byte> buffer = new ArrayList<>();
                    while (true) {
                        int c = in2.read();
                        if (c < 0) {
                            break;
                        }
                        buffer.add(Byte.valueOf((byte) c));
                    }
                    byte[] bytes = new byte[buffer.size()];
                    for (int i = 0; i < buffer.size(); i++) {
                        bytes[i] = buffer.get(i).byteValue();
                    }
                    String[] ss = new String(bytes).split(" ");
                    if (ss.length == 2) {
                        pkg = ss[0];
                        try {
                            state = Integer.parseInt(ss[1]);
                        } catch (NumberFormatException e3) {
                        }
                    } else if (ss.length == 3) {
                        pkg = ss[0];
                        try {
                            state = Integer.parseInt(ss[1]);
                            userId = Integer.parseInt(ss[2]);
                        } catch (NumberFormatException e4) {
                        }
                    }
                    in2.close();
                } catch (IOException e5) {
                    e = e5;
                    Slog.e(TAG, "IOException", e);
                    if (0 != 0) {
                        in.close();
                    }
                    if (pkg == null) {
                    }
                    Slog.e(TAG, "backup_pkg_enable_state file broken");
                }
            } catch (IOException e6) {
                e = e6;
            } catch (Throwable th2) {
                th = th2;
                Throwable th3 = th;
                if (0 != 0) {
                    try {
                        in.close();
                    } catch (IOException e7) {
                        Slog.e(TAG, "IOEception", e7);
                    }
                }
                throw th3;
            }
            if (pkg == null && state != Integer.MIN_VALUE) {
                Slog.v(TAG, "Unfinished backup package found, restore it's enable state");
                String defaultIme = getDefaultIme(this.mContext);
                this.mPackageLastEnableState = state;
                enablePackage(pkg, userId, defaultIme);
                return;
            }
            Slog.e(TAG, "backup_pkg_enable_state file broken");
        }
    }

    public void onApkInstalled() {
        if (!BackupManager.isSysAppForBackup(this.mContext, this.mCurrentWorkingPkg)) {
            Slog.d(TAG, "onApkInstalled, mCurrentWorkingPkg:" + this.mCurrentWorkingPkg);
            disablePackageAndWait(this.mCurrentWorkingPkg, this.mAppUserId);
        }
    }

    public void errorOccur(int err) throws RemoteException {
        if (this.mLastError == 0) {
            this.mLastError = err;
            IPackageBackupRestoreObserver iPackageBackupRestoreObserver = this.mBackupRestoreObserver;
            if (iPackageBackupRestoreObserver != null) {
                iPackageBackupRestoreObserver.onError(this.mCurrentWorkingPkg, this.mCurrentWorkingFeature, err);
            }
        }
    }

    public String getCurrentRunningPackage() throws RemoteException {
        return this.mCurrentWorkingPkg;
    }

    public int getCurrentWorkingFeature() throws RemoteException {
        return this.mCurrentWorkingFeature;
    }

    public int getState() throws RemoteException {
        return this.mState;
    }

    public int getBackupTimeoutScale() throws RemoteException {
        if (BackupManager.isProgRecordApp(this.mCurrentWorkingPkg, this.mCurrentWorkingFeature)) {
            return 6;
        }
        return 1;
    }

    public boolean shouldSkipData() {
        return this.mShouldSkipData;
    }

    public int getAppUserId() {
        return this.mAppUserId;
    }

    /* loaded from: classes.dex */
    private class FullBackupRestoreObserver extends IFullBackupRestoreObserver.Stub {
        private FullBackupRestoreObserver() {
            BackupManagerService.this = r1;
        }

        public void onStartBackup() throws RemoteException {
        }

        public void onBackupPackage(String name) throws RemoteException {
            if (!TextUtils.isEmpty(BackupManagerService.this.mCurrentWorkingPkg)) {
                BackupManagerService.this.mCurrentWorkingPkg.equals(name);
            }
            if (BackupManagerService.this.mBackupRestoreObserver != null) {
                BackupManagerService.this.mBackupRestoreObserver.onBackupStart(BackupManagerService.this.mCurrentWorkingPkg, BackupManagerService.this.mCurrentWorkingFeature);
            }
        }

        public void onEndBackup() throws RemoteException {
            if (BackupManagerService.this.mBackupRestoreObserver != null) {
                BackupManagerService.this.mBackupRestoreObserver.onBackupEnd(BackupManagerService.this.mCurrentWorkingPkg, BackupManagerService.this.mCurrentWorkingFeature);
            }
        }

        public void onStartRestore() throws RemoteException {
        }

        public void onRestorePackage(String name) throws RemoteException {
            if (BackupManagerService.this.mBackupRestoreObserver != null) {
                BackupManagerService.this.mBackupRestoreObserver.onRestoreStart(BackupManagerService.this.mCurrentWorkingPkg, BackupManagerService.this.mCurrentWorkingFeature);
            }
        }

        public void onEndRestore() throws RemoteException {
            if (BackupManagerService.this.mBackupRestoreObserver != null) {
                BackupManagerService.this.mBackupRestoreObserver.onRestoreEnd(BackupManagerService.this.mCurrentWorkingPkg, BackupManagerService.this.mCurrentWorkingFeature);
            }
        }

        public void onTimeout() throws RemoteException {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class BackupHandler extends Handler {
        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        private BackupHandler(Looper looper) {
            super(looper);
            BackupManagerService.this = r1;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class DeathLinker implements IBinder.DeathRecipient {
        private DeathLinker() {
            BackupManagerService.this = r1;
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            Slog.d(BackupManagerService.TAG, "Client binder has died. Start canceling...");
            BackupManagerService.this.mIsCanceling = true;
            BackupManagerService.this.scheduleReleaseResource();
            BackupManagerService.this.waitForTheLastWorkingTask();
            BackupManagerService.this.mIsCanceling = false;
            BackupManagerService.this.mOwnerPid = -1;
            BackupManagerService.this.mICaller.unlinkToDeath(BackupManagerService.this.mDeathLinker, 0);
            BackupManagerService.this.mICaller = null;
            BackupManagerService.this.mPreviousWorkingPkg = null;
            try {
                BackupManagerService.this.broadcastServiceIdle();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            Slog.d(BackupManagerService.TAG, "Client binder has died. Cancel completed!");
        }
    }

    private String getDefaultIme(Context context) {
        String defaultIme = Settings.Secure.getString(context.getContentResolver(), "default_input_method");
        return defaultIme;
    }

    private boolean isDefaultIme(String pkg, String defaultIme) {
        ComponentName cn;
        if (pkg != null && defaultIme != null && (cn = ComponentName.unflattenFromString(defaultIme)) != null && TextUtils.equals(pkg, cn.getPackageName())) {
            return true;
        }
        return false;
    }

    public void delCacheBackup() {
        int uid = Binder.getCallingUid();
        if (uid == 6102 || uid == 6100) {
            File file = new File("/cache/backup");
            FileUtils.deleteContents(file);
        }
    }

    public boolean isCanceling() throws RemoteException {
        return this.mIsCanceling;
    }

    private void closeBackupWriteStream(ParcelFileDescriptor outputFile) {
        if (outputFile != null) {
            try {
                IoBridge.closeAndSignalBlockedThreads(outputFile.getFileDescriptor());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void releaseBackupWriteStream(final ParcelFileDescriptor outputFile) {
        if (outputFile != null) {
            this.mHandler.post(new Runnable() { // from class: com.miui.server.BackupManagerService$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    BackupManagerService.lambda$releaseBackupWriteStream$0(outputFile);
                }
            });
        }
    }

    public static /* synthetic */ void lambda$releaseBackupWriteStream$0(ParcelFileDescriptor outputFile) {
        byte[] b = new byte[1024];
        FileInputStream fis = new FileInputStream(outputFile.getFileDescriptor());
        do {
            try {
                try {
                    try {
                    } catch (IOException e) {
                        Slog.e(TAG, "releaseBackupReadStream", e);
                        fis.close();
                    }
                } catch (Throwable th) {
                    try {
                        fis.close();
                    } catch (IOException e2) {
                        Slog.e(TAG, "IOException", e2);
                    }
                    throw th;
                }
            } catch (IOException e3) {
                Slog.e(TAG, "IOException", e3);
                return;
            }
        } while (fis.read(b) > 0);
        fis.close();
    }

    private int getApplicationEnabledSetting(String packageName, int userId) {
        try {
            return this.mPackageManagerBinder.getApplicationEnabledSetting(packageName, userId);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    private void setApplicationEnabledSetting(String packageName, int userId, int newState, int flags) {
        long token = Binder.clearCallingIdentity();
        boolean suspend = newState == 2;
        try {
            try {
                this.mPackageManagerBinder.setPackagesSuspendedAsUser(new String[]{packageName}, suspend, (PersistableBundle) null, (PersistableBundle) null, new SuspendDialogInfo.Builder().setMessage(this.mContext.getResources().getString(286195820)).build(), this.mContext.getOpPackageName(), userId);
                Slog.d(TAG, "packageName " + packageName + " setPackagesSuspended suspend: " + suspend);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }
}
