package com.android.server.pm;

import android.content.Context;
import android.content.IIntentReceiver;
import android.content.IIntentSender;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.IPackageDeleteObserver2;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInstaller;
import android.content.pm.VersionedPackage;
import android.content.pm.parsing.ApkLiteParseUtils;
import android.content.pm.parsing.PackageLite;
import android.content.pm.parsing.result.ParseResult;
import android.content.pm.parsing.result.ParseTypeImpl;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Slog;
import android.util.SparseArray;
import com.android.internal.content.InstallLocationUtils;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import libcore.io.IoUtils;
/* loaded from: classes.dex */
class InstallerUtil {
    private static final String TAG = "InstallerUtil";

    InstallerUtil() {
    }

    /* loaded from: classes.dex */
    private static class PackageDeleteObserver extends IPackageDeleteObserver2.Stub {
        boolean finished;
        boolean result;

        private PackageDeleteObserver() {
        }

        public void onUserActionRequired(Intent intent) throws RemoteException {
        }

        public void onPackageDeleted(String packageName, int returnCode, String msg) throws RemoteException {
            synchronized (this) {
                boolean z = true;
                this.finished = true;
                if (returnCode != 1) {
                    z = false;
                }
                this.result = z;
                notifyAll();
            }
        }
    }

    static boolean deleteApp(IPackageManager pm, String pkgName, boolean keepData) {
        int flags = 2;
        if (keepData) {
            flags = 2 | 1;
        }
        PackageDeleteObserver obs = new PackageDeleteObserver();
        try {
            pm.deletePackageVersioned(new VersionedPackage(pkgName, -1), obs, 0, flags);
            synchronized (obs) {
                while (!obs.finished) {
                    try {
                        obs.wait();
                    } catch (InterruptedException e) {
                    }
                }
            }
            return obs.result;
        } catch (RemoteException | SecurityException e2) {
            Slog.e(TAG, "Failed to delete " + pkgName, e2);
            return false;
        }
    }

    static List<File> installApps(Context context, List<File> codePathList) {
        ArrayList<File> installedApps = new ArrayList<>();
        if (codePathList.isEmpty()) {
            return installedApps;
        }
        SparseArray<File> activeSessions = new SparseArray<>(codePathList.size());
        long waitDuration = 0;
        LocalIntentReceiver receiver = new LocalIntentReceiver(codePathList.size());
        for (File codePath : codePathList) {
            int sessionId = installOne(context, receiver, codePath);
            if (sessionId > 0) {
                activeSessions.put(sessionId, codePath);
                waitDuration += TimeUnit.MINUTES.toMillis(2L);
            }
        }
        long start = SystemClock.uptimeMillis();
        while (true) {
            int status = 0;
            if (activeSessions.size() <= 0 || SystemClock.uptimeMillis() - start >= waitDuration) {
                break;
            }
            SystemClock.sleep(1000L);
            for (Intent intent : receiver.getResultsNoWait()) {
                int sessionId2 = intent.getIntExtra("android.content.pm.extra.SESSION_ID", status);
                if (activeSessions.indexOfKey(sessionId2) >= 0) {
                    File file = (File) activeSessions.removeReturnOld(sessionId2);
                    int status2 = intent.getIntExtra("android.content.pm.extra.STATUS", 1);
                    if (status2 == 0) {
                        installedApps.add(file);
                        status = 0;
                    } else {
                        String errorMsg = intent.getStringExtra("android.content.pm.extra.STATUS_MESSAGE");
                        Slog.d(TAG, "Failed to install " + file + ":  error code=" + status2 + ", msg=" + errorMsg);
                        waitDuration = waitDuration;
                        status = 0;
                    }
                }
            }
        }
        for (int i = 0; i < activeSessions.size(); i++) {
            int sessionId3 = activeSessions.keyAt(0);
            Slog.e(TAG, "Failed to install " + activeSessions.get(sessionId3) + ": timeout, sessionId=" + sessionId3);
        }
        return installedApps;
    }

    private static int installOne(Context context, LocalIntentReceiver receiver, File codePath) {
        try {
            PackageLite pkgLite = parsePackageLite(codePath);
            if (pkgLite == null) {
                Slog.e(TAG, "Failed to installOne: " + codePath);
                return -4;
            }
            PackageInstaller.SessionParams sessionParams = makeSessionParams(pkgLite);
            int sessionId = doCreateSession(context, sessionParams);
            if (sessionId <= 0) {
                return -1;
            }
            for (String splitCodePath : pkgLite.getAllApkPaths()) {
                File splitFile = new File(splitCodePath);
                if (!doWriteSession(context, splitFile.getName(), splitFile, sessionId)) {
                    doAandonSession(context, sessionId);
                    return -1;
                }
            }
            if (!doCommitSession(context, sessionId, receiver.getIntentSender())) {
                return -1;
            }
            return sessionId;
        } catch (Exception e) {
            Slog.e(TAG, "Failed to install " + codePath, e);
            return -1;
        }
    }

    private static boolean doWriteSession(Context context, String name, File apkFile, int sessionId) {
        PackageInstaller.Session session;
        Exception e;
        ParcelFileDescriptor pfd = null;
        try {
            ParcelFileDescriptor pfd2 = ParcelFileDescriptor.open(apkFile, 268435456);
            try {
                session = context.getPackageManager().getPackageInstaller().openSession(sessionId);
                try {
                    session.write(name, 0L, pfd2.getStatSize(), pfd2);
                    IoUtils.closeQuietly(pfd2);
                    IoUtils.closeQuietly(session);
                    return true;
                } catch (Exception e2) {
                    e = e2;
                    pfd = pfd2;
                    try {
                        Slog.e(TAG, "doWriteSession failed: ", e);
                        IoUtils.closeQuietly(pfd);
                        IoUtils.closeQuietly(session);
                        return false;
                    } catch (Throwable th) {
                        th = th;
                        IoUtils.closeQuietly(pfd);
                        IoUtils.closeQuietly(session);
                        throw th;
                    }
                } catch (Throwable th2) {
                    th = th2;
                    pfd = pfd2;
                    IoUtils.closeQuietly(pfd);
                    IoUtils.closeQuietly(session);
                    throw th;
                }
            } catch (Exception e3) {
                session = null;
                e = e3;
                pfd = pfd2;
            } catch (Throwable th3) {
                session = null;
                th = th3;
                pfd = pfd2;
            }
        } catch (Exception e4) {
            session = null;
            e = e4;
        } catch (Throwable th4) {
            session = null;
            th = th4;
        }
    }

    private static boolean doCommitSession(Context context, int sessionId, IntentSender target) {
        PackageInstaller packageInstaller = context.getPackageManager().getPackageInstaller();
        PackageInstaller.Session session = null;
        try {
            try {
                session = packageInstaller.openSession(sessionId);
                session.commit(target);
                return true;
            } catch (Exception e) {
                Slog.e(TAG, "doCommitSession failed: ", e);
                IoUtils.closeQuietly(session);
                return false;
            }
        } finally {
            IoUtils.closeQuietly(session);
        }
    }

    @Deprecated
    public static Map<File, Integer> installAppList(Context context, Collection<File> apkFileList) {
        Map<File, Integer> installedResult = new HashMap<>();
        if (apkFileList.isEmpty()) {
            return installedResult;
        }
        SparseArray<File> sessions = new SparseArray<>();
        LocalIntentReceiver receiver = new LocalIntentReceiver(apkFileList.size());
        HashSet<String> packages = new HashSet<>();
        for (File apkFile : apkFileList) {
            PackageLite apkLite = parsePackageLite(apkFile);
            if (apkLite == null) {
                Slog.e(TAG, "Failed to installAppList: " + apkFile);
            } else if (packages.contains(apkLite.getPackageName())) {
                Slog.e(TAG, "Failed to installApp: " + apkFile + ", duplicate package name, version: " + apkLite.getVersionCode());
            } else {
                packages.add(apkLite.getPackageName());
                PackageInstaller.SessionParams sessionParams = makeSessionParams(apkLite);
                int sessionId = doCreateSession(context, sessionParams);
                if (sessionId != 0 && doCommitSession(context, apkFile, sessionId, receiver.getIntentSender())) {
                    sessions.put(sessionId, apkFile);
                }
            }
        }
        int size = sessions.size();
        while (size > 0) {
            Intent result = receiver.getResult();
            int sessionId2 = result.getIntExtra("android.content.pm.extra.SESSION_ID", 0);
            if (sessions.indexOfKey(sessionId2) < 0) {
                Slog.i(TAG, "InstallApp received invalid sessionId:" + sessionId2);
            } else {
                int status = result.getIntExtra("android.content.pm.extra.STATUS", 1);
                installedResult.put(sessions.get(sessionId2), Integer.valueOf(status));
                if (status != 0) {
                    String packageName = result.getStringExtra("android.content.pm.extra.PACKAGE_NAME");
                    String errorMsg = result.getStringExtra("android.content.pm.extra.STATUS_MESSAGE");
                    Slog.e(TAG, "InstallApp failed for id:" + sessionId2 + " pkg:" + packageName + " status:" + status + " msg:" + errorMsg);
                }
                size--;
            }
        }
        return installedResult;
    }

    private static int doCreateSession(Context context, PackageInstaller.SessionParams sessionParams) {
        PackageInstaller packageInstaller = context.getPackageManager().getPackageInstaller();
        try {
            int sessionId = packageInstaller.createSession(sessionParams);
            return sessionId;
        } catch (IOException e) {
            Slog.e(TAG, "doCreateSession failed: ", e);
            return 0;
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:26:0x007c A[Catch: all -> 0x008a, TRY_LEAVE, TryCatch #3 {all -> 0x008a, blocks: (B:12:0x0056, B:24:0x0075, B:26:0x007c), top: B:32:0x000d }] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private static boolean doCommitSession(android.content.Context r16, java.io.File r17, int r18, android.content.IntentSender r19) {
        /*
            r1 = r17
            java.lang.String r2 = "InstallerUtil"
            r3 = 0
            r4 = 0
            long r5 = android.os.Binder.clearCallingIdentity()
            r0 = 268435456(0x10000000, float:2.524355E-29)
            r7 = 0
            android.os.ParcelFileDescriptor r0 = android.os.ParcelFileDescriptor.open(r1, r0)     // Catch: java.lang.Throwable -> L6a java.lang.Exception -> L70
            r4 = r0
            if (r4 != 0) goto L35
            java.lang.StringBuilder r0 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> L6a java.lang.Exception -> L70
            r0.<init>()     // Catch: java.lang.Throwable -> L6a java.lang.Exception -> L70
            java.lang.String r8 = "doWriteSession failed: can't open "
            java.lang.StringBuilder r0 = r0.append(r8)     // Catch: java.lang.Throwable -> L6a java.lang.Exception -> L70
            java.lang.StringBuilder r0 = r0.append(r1)     // Catch: java.lang.Throwable -> L6a java.lang.Exception -> L70
            java.lang.String r0 = r0.toString()     // Catch: java.lang.Throwable -> L6a java.lang.Exception -> L70
            android.util.Slog.e(r2, r0)     // Catch: java.lang.Throwable -> L6a java.lang.Exception -> L70
            libcore.io.IoUtils.closeQuietly(r4)
            libcore.io.IoUtils.closeQuietly(r3)
            android.os.Binder.restoreCallingIdentity(r5)
            return r7
        L35:
            android.content.pm.PackageManager r0 = r16.getPackageManager()     // Catch: java.lang.Throwable -> L6a java.lang.Exception -> L70
            android.content.pm.PackageInstaller r0 = r0.getPackageInstaller()     // Catch: java.lang.Throwable -> L6a java.lang.Exception -> L70
            r15 = r18
            android.content.pm.PackageInstaller$Session r0 = r0.openSession(r15)     // Catch: java.lang.Throwable -> L66 java.lang.Exception -> L68
            r3 = r0
            java.lang.String r9 = "base.apk"
            r10 = 0
            long r12 = r4.getStatSize()     // Catch: java.lang.Throwable -> L66 java.lang.Exception -> L68
            r8 = r3
            r14 = r4
            r8.write(r9, r10, r12, r14)     // Catch: java.lang.Throwable -> L66 java.lang.Exception -> L68
            r4.close()     // Catch: java.lang.Throwable -> L66 java.lang.Exception -> L68
            r8 = r19
            r3.commit(r8)     // Catch: java.lang.Exception -> L64 java.lang.Throwable -> L8a
            r0 = 1
            libcore.io.IoUtils.closeQuietly(r4)
            libcore.io.IoUtils.closeQuietly(r3)
            android.os.Binder.restoreCallingIdentity(r5)
            return r0
        L64:
            r0 = move-exception
            goto L75
        L66:
            r0 = move-exception
            goto L6d
        L68:
            r0 = move-exception
            goto L73
        L6a:
            r0 = move-exception
            r15 = r18
        L6d:
            r8 = r19
            goto L8b
        L70:
            r0 = move-exception
            r15 = r18
        L73:
            r8 = r19
        L75:
            java.lang.String r9 = "doWriteSession failed"
            android.util.Slog.e(r2, r9, r0)     // Catch: java.lang.Throwable -> L8a
            if (r3 == 0) goto L7f
            r3.abandon()     // Catch: java.lang.Throwable -> L8a
        L7f:
            libcore.io.IoUtils.closeQuietly(r4)
            libcore.io.IoUtils.closeQuietly(r3)
            android.os.Binder.restoreCallingIdentity(r5)
            return r7
        L8a:
            r0 = move-exception
        L8b:
            libcore.io.IoUtils.closeQuietly(r4)
            libcore.io.IoUtils.closeQuietly(r3)
            android.os.Binder.restoreCallingIdentity(r5)
            throw r0
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.pm.InstallerUtil.doCommitSession(android.content.Context, java.io.File, int, android.content.IntentSender):boolean");
    }

    private static void doAandonSession(Context context, int sessionId) {
        PackageInstaller packageInstaller = context.getPackageManager().getPackageInstaller();
        PackageInstaller.Session session = null;
        try {
            try {
                session = packageInstaller.openSession(sessionId);
                session.abandon();
            } catch (IOException e) {
                Slog.e(TAG, "doAandonSession failed: ", e);
            }
        } finally {
            IoUtils.closeQuietly(session);
        }
    }

    private static PackageLite parsePackageLite(File codePath) {
        ParseTypeImpl input = ParseTypeImpl.forDefaultParsing();
        ParseResult<PackageLite> result = ApkLiteParseUtils.parsePackageLite(input, codePath, 0);
        if (result.isError()) {
            Slog.e(TAG, "Failed to parsePackageLite: " + codePath + " error: " + result.getErrorMessage(), result.getException());
            return null;
        }
        return (PackageLite) result.getResult();
    }

    private static PackageInstaller.SessionParams makeSessionParams(PackageLite pkgLite) {
        PackageInstaller.SessionParams sessionParams = new PackageInstaller.SessionParams(1);
        sessionParams.setInstallAsInstantApp(false);
        sessionParams.setInstallerPackageName("android");
        sessionParams.setAppPackageName(pkgLite.getPackageName());
        sessionParams.setInstallLocation(pkgLite.getInstallLocation());
        try {
            sessionParams.setSize(InstallLocationUtils.calculateInstalledSize(pkgLite, (String) null));
        } catch (IOException e) {
            sessionParams.setSize(new File(pkgLite.getBaseApkPath()).length());
        }
        return sessionParams;
    }

    /* loaded from: classes.dex */
    public static class LocalIntentReceiver {
        private IIntentSender.Stub mLocalSender = new IIntentSender.Stub() { // from class: com.android.server.pm.InstallerUtil.LocalIntentReceiver.1
            public void send(int code, Intent intent, String resolvedType, IBinder whitelistToken, IIntentReceiver finishedReceiver, String requiredPermission, Bundle options) {
                LocalIntentReceiver.this.mResult.offer(intent);
            }
        };
        private final LinkedBlockingQueue<Intent> mResult;

        public LocalIntentReceiver(int capacity) {
            this.mResult = new LinkedBlockingQueue<>(capacity);
        }

        public IntentSender getIntentSender() {
            return new IntentSender(this.mLocalSender);
        }

        public List<Intent> getResultsNoWait() {
            ArrayList<Intent> results = new ArrayList<>();
            try {
                this.mResult.drainTo(results);
            } catch (Exception e) {
            }
            return results;
        }

        public Intent getResult() {
            Intent intent = null;
            try {
                intent = this.mResult.poll(30L, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Slog.e(InstallerUtil.TAG, "LocalIntentReceiver poll timeout in 30 seconds.");
            }
            return intent != null ? intent : new Intent();
        }
    }
}
