package com.android.server.am;

import android.app.AppGlobals;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.ParceledListSlice;
import android.content.pm.ResolveInfo;
import android.os.Binder;
import android.os.Handler;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Log;
import com.android.internal.os.BackgroundThread;
import com.android.server.am.PendingIntentRecord;
import com.android.server.inputmethod.InputMethodManagerServiceImpl;
import com.miui.base.MiuiStubRegistry;
import com.miui.server.greeze.GreezeManagerService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import miui.greeze.IGreezeManager;
/* loaded from: classes.dex */
public class PendingIntentRecordImpl implements PendingIntentRecordStub {
    private static final String TAG = "PendingIntentRecordImpl";
    private static final int sDefaultClearTime = 5000;
    private static final ArraySet<String> sIgnorePackages;
    private static final Object sLock = new Object();
    private static final Set<String> sPendingPackages = new ArraySet();
    private static final Handler sMessageHandler = BackgroundThread.getHandler();
    private static final Runnable sClearPendingCallback = new Runnable() { // from class: com.android.server.am.PendingIntentRecordImpl$$ExternalSyntheticLambda0
        @Override // java.lang.Runnable
        public final void run() {
            PendingIntentRecordImpl.lambda$static$0();
        }
    };
    private IGreezeManager mIGreezeManager = null;
    private List<String> mSystemUIApp = new ArrayList(Arrays.asList("com.android.systemui", "com.miui.notification"));
    private List<String> mLauncherApp = new ArrayList(Arrays.asList(InputMethodManagerServiceImpl.MIUI_HOME, "com.mi.android.globallauncher"));

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<PendingIntentRecordImpl> {

        /* compiled from: PendingIntentRecordImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final PendingIntentRecordImpl INSTANCE = new PendingIntentRecordImpl();
        }

        public PendingIntentRecordImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public PendingIntentRecordImpl provideNewInstance() {
            return new PendingIntentRecordImpl();
        }
    }

    public static PendingIntentRecordImpl getInstance() {
        return (PendingIntentRecordImpl) PendingIntentRecordStub.get();
    }

    static {
        ArraySet<String> arraySet = new ArraySet<>();
        sIgnorePackages = arraySet;
        arraySet.add("com.android.systemui");
        arraySet.add("com.miui.notification");
        arraySet.add("com.android.keyguard");
        arraySet.add(InputMethodManagerServiceImpl.MIUI_HOME);
        arraySet.add("com.mi.android.globallauncher");
        arraySet.add("com.xiaomi.xmsf");
        arraySet.add("com.google.android.wearable.app.cn");
        arraySet.add("com.xiaomi.mirror");
        arraySet.add("com.miui.personalassistant");
    }

    public static /* synthetic */ void lambda$static$0() {
        synchronized (sLock) {
            sPendingPackages.clear();
        }
    }

    private IGreezeManager getGreeze() {
        if (this.mIGreezeManager == null) {
            this.mIGreezeManager = IGreezeManager.Stub.asInterface(ServiceManager.getService(GreezeManagerService.SERVICE_NAME));
        }
        return this.mIGreezeManager;
    }

    private void notifyGreeze(String targetPkg, String pkg, PendingIntentRecord.Key key) {
        if ((!this.mSystemUIApp.contains(pkg) && (!this.mLauncherApp.contains(pkg) || key.type == 2)) || getGreeze() == null) {
            return;
        }
        Log.d(TAG, "notifyGreeze pendingtent from " + pkg + " -> " + targetPkg);
        try {
            int uid = getUidByPackageName(targetPkg);
            if (getGreeze().isUidFrozen(uid)) {
                int[] uids = {uid};
                getGreeze().thawUids(uids, 1000, "notifi");
            }
        } catch (Exception e) {
            Log.e(TAG, "notifyGreeze error");
        }
    }

    private int getUidByPackageName(String pkgName) {
        IPackageManager pm = AppGlobals.getPackageManager();
        try {
            ApplicationInfo appinfo = pm.getApplicationInfo(pkgName, 0L, 0);
            if (appinfo != null) {
                return appinfo.uid;
            }
            return -1;
        } catch (Exception e) {
            Log.i(TAG, "not find packageName :" + pkgName);
            return -1;
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:21:0x0043 A[RETURN] */
    /* JADX WARN: Removed duplicated region for block: B:22:0x0044 A[Catch: Exception -> 0x008b, TryCatch #2 {Exception -> 0x008b, blocks: (B:4:0x0006, B:6:0x0014, B:8:0x001c, B:9:0x001f, B:12:0x0028, B:14:0x0030, B:19:0x003c, B:22:0x0044, B:23:0x0046, B:28:0x0053, B:32:0x0065, B:33:0x0073, B:36:0x007a, B:34:0x0074, B:35:0x0079), top: B:41:0x0006 }] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public void preSendPendingIntentInner(com.android.server.am.PendingIntentRecord.Key r13, android.content.Intent r14) {
        /*
            r12 = this;
            if (r14 == 0) goto L94
            if (r13 != 0) goto L6
            goto L94
        L6:
            int r0 = android.os.Binder.getCallingPid()     // Catch: java.lang.Exception -> L8b
            java.lang.String r1 = com.android.server.am.ProcessUtils.getPackageNameByPid(r0)     // Catch: java.lang.Exception -> L8b
            java.lang.String r2 = getTargetPkg(r13, r14)     // Catch: java.lang.Exception -> L8b
            if (r1 == 0) goto L1f
            java.lang.String r3 = "android"
            boolean r3 = r3.equals(r1)     // Catch: java.lang.Exception -> L8b
            if (r3 != 0) goto L1f
            r12.notifyGreeze(r2, r1, r13)     // Catch: java.lang.Exception -> L8b
        L1f:
            android.util.ArraySet<java.lang.String> r3 = com.android.server.am.PendingIntentRecordImpl.sIgnorePackages     // Catch: java.lang.Exception -> L8b
            boolean r3 = r3.contains(r1)     // Catch: java.lang.Exception -> L8b
            if (r3 != 0) goto L28
            return
        L28:
            java.lang.String r3 = "com.miui.home"
            boolean r3 = r3.equals(r1)     // Catch: java.lang.Exception -> L8b
            if (r3 != 0) goto L3b
            java.lang.String r3 = "com.mi.android.globallauncher"
            boolean r3 = r3.equals(r1)     // Catch: java.lang.Exception -> L8b
            if (r3 == 0) goto L39
            goto L3b
        L39:
            r3 = 0
            goto L3c
        L3b:
            r3 = 1
        L3c:
            r10 = r3
            boolean r3 = android.text.TextUtils.isEmpty(r2)     // Catch: java.lang.Exception -> L8b
            if (r3 == 0) goto L44
            return
        L44:
            int r3 = r13.type     // Catch: java.lang.Exception -> L8b
            switch(r3) {
                case 1: goto L51;
                case 2: goto L4e;
                case 3: goto L49;
                case 4: goto L4a;
                default: goto L49;
            }     // Catch: java.lang.Exception -> L8b
        L49:
            return
        L4a:
            r3 = 8
            r11 = r3
            goto L53
        L4e:
            r3 = 1
            r11 = r3
            goto L53
        L51:
            r3 = 2
            r11 = r3
        L53:
            com.android.server.am.SmartPowerService r3 = com.android.server.am.SmartPowerService.getInstance()     // Catch: java.lang.Exception -> L8b
            r3.onSendPendingIntent(r13, r1, r2, r14)     // Catch: java.lang.Exception -> L8b
            miui.security.WakePathChecker r3 = miui.security.WakePathChecker.getInstance()     // Catch: java.lang.Exception -> L8b
            if (r10 == 0) goto L63
            java.lang.String r4 = "appwidget"
            goto L65
        L63:
            java.lang.String r4 = "notification"
        L65:
            int r7 = android.os.UserHandle.myUserId()     // Catch: java.lang.Exception -> L8b
            int r8 = r13.userId     // Catch: java.lang.Exception -> L8b
            r9 = 1
            r5 = r2
            r6 = r11
            r3.recordWakePathCall(r4, r5, r6, r7, r8, r9)     // Catch: java.lang.Exception -> L8b
            java.lang.Object r3 = com.android.server.am.PendingIntentRecordImpl.sLock     // Catch: java.lang.Exception -> L8b
            monitor-enter(r3)     // Catch: java.lang.Exception -> L8b
            java.util.Set<java.lang.String> r4 = com.android.server.am.PendingIntentRecordImpl.sPendingPackages     // Catch: java.lang.Throwable -> L88
            r4.add(r2)     // Catch: java.lang.Throwable -> L88
            monitor-exit(r3)     // Catch: java.lang.Throwable -> L88
            android.os.Handler r3 = com.android.server.am.PendingIntentRecordImpl.sMessageHandler     // Catch: java.lang.Exception -> L8b
            java.lang.Runnable r4 = com.android.server.am.PendingIntentRecordImpl.sClearPendingCallback     // Catch: java.lang.Exception -> L8b
            r3.removeCallbacks(r4)     // Catch: java.lang.Exception -> L8b
            r5 = 5000(0x1388, double:2.4703E-320)
            r3.postDelayed(r4, r5)     // Catch: java.lang.Exception -> L8b
            goto L93
        L88:
            r4 = move-exception
            monitor-exit(r3)     // Catch: java.lang.Throwable -> L88
            throw r4     // Catch: java.lang.Exception -> L8b
        L8b:
            r0 = move-exception
            java.lang.String r1 = "PendingIntentRecordImpl"
            java.lang.String r2 = "preSendInner error"
            android.util.Log.e(r1, r2, r0)
        L93:
            return
        L94:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.am.PendingIntentRecordImpl.preSendPendingIntentInner(com.android.server.am.PendingIntentRecord$Key, android.content.Intent):void");
    }

    public void exemptTemporarily(String packageName) {
        exemptTemporarily(packageName, false);
    }

    public static void exemptTemporarily(String packageName, boolean ignoreSource) {
        if (TextUtils.isEmpty(packageName)) {
            return;
        }
        try {
            int callingPid = Binder.getCallingPid();
            if (!ignoreSource) {
                String pkg = ProcessUtils.getPackageNameByPid(callingPid);
                if (!sIgnorePackages.contains(pkg)) {
                    return;
                }
            }
            synchronized (sLock) {
                sPendingPackages.add(packageName);
            }
            Handler handler = sMessageHandler;
            Runnable runnable = sClearPendingCallback;
            handler.removeCallbacks(runnable);
            handler.postDelayed(runnable, 5000L);
        } catch (Exception e) {
            Log.e(TAG, "exempt temporarily error!", e);
        }
    }

    public static boolean containsPendingIntent(String packageName) {
        boolean contains;
        synchronized (sLock) {
            contains = sPendingPackages.contains(packageName);
        }
        return contains;
    }

    private static String getTargetPkg(PendingIntentRecord.Key key, Intent intent) throws Exception {
        ComponentName component;
        String targetPkg = intent.getPackage();
        if (targetPkg == null && (component = intent.getComponent()) != null) {
            targetPkg = component.getPackageName();
        }
        if (targetPkg == null) {
            IPackageManager pm = AppGlobals.getPackageManager();
            int userId = UserHandle.getCallingUserId();
            if (key.type == 4) {
                ResolveInfo resolveIntent = pm.resolveService(intent, (String) null, 1024L, userId);
                if (resolveIntent != null && resolveIntent.serviceInfo != null) {
                    targetPkg = resolveIntent.serviceInfo.packageName;
                }
            } else {
                ParceledListSlice<ResolveInfo> qeury = pm.queryIntentReceivers(intent, (String) null, 1024L, userId);
                if (qeury == null) {
                    return null;
                }
                List<ResolveInfo> receivers = null;
                ParceledListSlice<ResolveInfo> parceledList = qeury;
                if (parceledList != null) {
                    receivers = parceledList.getList();
                }
                if (receivers != null && receivers.size() == 1) {
                    ResolveInfo resolveInfo = receivers.get(0);
                    if (resolveInfo.activityInfo != null) {
                        targetPkg = resolveInfo.activityInfo.packageName;
                    }
                }
            }
        }
        if (targetPkg == null) {
            return key.packageName;
        }
        return targetPkg;
    }

    public boolean checkRunningCompatibility(Intent service, String resolvedType, int callingUid, int callingPid, int userId) {
        return ActivityManagerServiceStub.get().checkRunningCompatibility(service, resolvedType, callingUid, callingPid, userId);
    }
}
