package com.miui.server;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Slog;
import com.android.internal.os.BackgroundThread;
import com.miui.server.ISplashPackageCheckListener;
import com.miui.server.ISplashScreenService;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
/* loaded from: classes.dex */
public final class SplashScreenServiceDelegate {
    private static final String ACTION_DEBUG_OFF = "miui.intent.action.ad.DEBUG_OFF";
    private static final String ACTION_DEBUG_ON = "miui.intent.action.ad.DEBUG_ON";
    private static final int CORE_SIZE = 16;
    private static final long DELAY_BIND_AFTER_BOOT_COMPLETE = 120000;
    private static final String KEY_API_VERSION = "apiVersion";
    private static final long MAX_DELAY_TIME = 3600000;
    private static final String MIUI_GENERAL_PERMISSION = "miui.permission.USE_INTERNAL_GENERAL_API";
    private static final int MSG_REBIND = 1;
    private static final int REQUEST_SPLASH_SCREEN_TIMOUT = 2000;
    public static final String SPLASHSCREEN_ACTIVITY = "com.miui.systemAdSolution.splashscreen.SplashActivity";
    private static final String SPLASHSCREEN_CLASS = "com.miui.systemAdSolution.splashscreen.SplashScreenService";
    public static final String SPLASHSCREEN_GLOBAL_PACKAGE = "com.miui.msa.global";
    public static final String SPLASHSCREEN_PACKAGE = "com.miui.systemAdSolution";
    private static final String TAG = "SplashScreenServiceDelegate";
    private static final int VALUE_API_VERSION = 2;
    private static boolean sDebug;
    private Context mContext;
    private long mDelayTime;
    private ThreadPoolExecutor mExecutor;
    private int mRebindCount;
    private int mSeverity;
    private ISplashScreenService mSplashScreenService;
    private long mStartTime;
    private Map<String, SplashPackageCheckInfo> mSplashPackageCheckInfoMap = new ConcurrentHashMap();
    private BroadcastReceiver mReceiver = new BroadcastReceiver() { // from class: com.miui.server.SplashScreenServiceDelegate.1
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                return;
            }
            String action = intent.getAction();
            if ("android.intent.action.BOOT_COMPLETED".equals(action)) {
                SplashScreenServiceDelegate.this.logI("Boot completed, delay to bind SplashScreenService", true);
                SplashScreenServiceDelegate.this.delayToBindServiceAfterBootCompleted();
            } else if (SplashScreenServiceDelegate.ACTION_DEBUG_ON.equals(action)) {
                SplashScreenServiceDelegate.this.logI("Debug On", true);
                SplashScreenServiceDelegate.sDebug = true;
            } else if (SplashScreenServiceDelegate.ACTION_DEBUG_OFF.equals(action)) {
                SplashScreenServiceDelegate.this.logI("Debug Off", true);
                SplashScreenServiceDelegate.sDebug = false;
            }
        }
    };
    private final ServiceConnection mSplashScreenConnection = new ServiceConnection() { // from class: com.miui.server.SplashScreenServiceDelegate.2
        @Override // android.content.ServiceConnection
        public void onServiceConnected(ComponentName name, IBinder service) {
            SplashScreenServiceDelegate.this.logI("SplashScreenService connected!");
            SplashScreenServiceDelegate.this.mSplashScreenService = ISplashScreenService.Stub.asInterface(service);
            SplashScreenServiceDelegate.this.mStartTime = System.currentTimeMillis();
            SplashScreenServiceDelegate.this.mRebindCount = 0;
            SplashScreenServiceDelegate.this.mHandler.removeMessages(1);
            try {
                SplashScreenServiceDelegate.this.mSplashScreenService.asBinder().linkToDeath(SplashScreenServiceDelegate.this.mDeathHandler, 0);
            } catch (Exception e) {
                SplashScreenServiceDelegate.this.logE("linkToDeath exception", e);
            }
            asyncSetSplashPackageCheckListener();
        }

        @Override // android.content.ServiceConnection
        public void onServiceDisconnected(ComponentName name) {
            SplashScreenServiceDelegate.this.logI("SplashScreenService disconnected!");
            SplashScreenServiceDelegate.this.mSplashScreenService = null;
            if (SplashScreenServiceDelegate.this.mContext != null) {
                SplashScreenServiceDelegate.this.mContext.unbindService(SplashScreenServiceDelegate.this.mSplashScreenConnection);
            }
        }

        private void asyncSetSplashPackageCheckListener() {
            BackgroundThread.getHandler().post(new Runnable() { // from class: com.miui.server.SplashScreenServiceDelegate.2.1
                @Override // java.lang.Runnable
                public void run() {
                    ISplashScreenService sss = SplashScreenServiceDelegate.this.mSplashScreenService;
                    if (sss != null) {
                        try {
                            SplashScreenServiceDelegate.this.logI("Set splash package check listener");
                            sss.setSplashPackageListener(SplashScreenServiceDelegate.this.mSplashPackageCheckListener);
                        } catch (Exception e) {
                            SplashScreenServiceDelegate.this.logE("asyncSetSplashPackageCheckListener exception", e);
                        }
                    }
                }
            });
        }
    };
    private IBinder.DeathRecipient mDeathHandler = new IBinder.DeathRecipient() { // from class: com.miui.server.SplashScreenServiceDelegate.3
        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            SplashScreenServiceDelegate.this.logI("SplashScreenService binderDied!");
            SplashScreenServiceDelegate.this.delayToRebindService();
        }
    };
    private ISplashPackageCheckListener mSplashPackageCheckListener = new ISplashPackageCheckListener.Stub() { // from class: com.miui.server.SplashScreenServiceDelegate.4
        @Override // com.miui.server.ISplashPackageCheckListener
        public void updateSplashPackageCheckInfoList(List<SplashPackageCheckInfo> splashPackageCheckInfos) throws RemoteException {
            try {
                SplashScreenServiceDelegate.this.logI("updateSplashPackageCheckInfoList");
                SplashScreenServiceDelegate.this.mSplashPackageCheckInfoMap.clear();
                if (splashPackageCheckInfos != null && !splashPackageCheckInfos.isEmpty()) {
                    for (SplashPackageCheckInfo info : splashPackageCheckInfos) {
                        updateSplashPackageCheckInfo(info);
                    }
                }
            } catch (Exception e) {
                SplashScreenServiceDelegate.this.logE("updateSplashPackageCheckInfoList exception", e);
            }
        }

        @Override // com.miui.server.ISplashPackageCheckListener
        public void updateSplashPackageCheckInfo(SplashPackageCheckInfo splashPackageCheckInfo) throws RemoteException {
            try {
                if (SplashScreenServiceDelegate.this.checkSplashPackageCheckInfo(splashPackageCheckInfo)) {
                    SplashScreenServiceDelegate.this.logI("Valid " + splashPackageCheckInfo);
                    SplashScreenServiceDelegate.this.keepSplashPackageCheckInfo(splashPackageCheckInfo);
                } else {
                    SplashScreenServiceDelegate.this.logI("Invalid " + splashPackageCheckInfo);
                }
            } catch (Exception e) {
                SplashScreenServiceDelegate.this.logE("updateSplashPackageCheckInfo exception", e);
            }
        }
    };
    private final Handler mHandler = new Handler(Looper.getMainLooper()) { // from class: com.miui.server.SplashScreenServiceDelegate.6
        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    SplashScreenServiceDelegate.this.bindService();
                    return;
                default:
                    return;
            }
        }
    };

    public SplashScreenServiceDelegate(Context context) {
        this.mContext = context;
        sDebug = Build.IS_DEBUGGABLE || TextUtils.equals(Build.TYPE, "userdebug");
        logI("Debug " + sDebug);
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(16, 16, 1L, TimeUnit.MINUTES, new LinkedBlockingQueue(), new ThreadPoolExecutor.DiscardPolicy());
        this.mExecutor = threadPoolExecutor;
        threadPoolExecutor.allowCoreThreadTimeOut(true);
        registerReceiver();
    }

    private void registerReceiver() {
        logI("Register BOOT_COMPLETED receiver", true);
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.BOOT_COMPLETED");
        this.mContext.registerReceiverAsUser(this.mReceiver, UserHandle.OWNER, filter, null, null);
        logI("Register debugger receiver", true);
        IntentFilter filter2 = new IntentFilter();
        filter2.addAction(ACTION_DEBUG_ON);
        this.mContext.registerReceiverAsUser(this.mReceiver, UserHandle.OWNER, filter2, MIUI_GENERAL_PERMISSION, null);
        IntentFilter filter3 = new IntentFilter();
        filter3.addAction(ACTION_DEBUG_OFF);
        this.mContext.registerReceiverAsUser(this.mReceiver, UserHandle.OWNER, filter3, MIUI_GENERAL_PERMISSION, null);
        delayToRebindService(600000L, false);
    }

    public void bindService() {
        if (this.mContext != null && this.mSplashScreenService == null) {
            try {
                Intent intent = new Intent();
                if (miui.os.Build.IS_INTERNATIONAL_BUILD) {
                    intent.setClassName(SPLASHSCREEN_GLOBAL_PACKAGE, SPLASHSCREEN_CLASS);
                } else {
                    intent.setClassName(SPLASHSCREEN_PACKAGE, SPLASHSCREEN_CLASS);
                }
                intent.putExtra(KEY_API_VERSION, 2);
                if (!this.mContext.bindServiceAsUser(intent, this.mSplashScreenConnection, 5, UserHandle.OWNER)) {
                    logW("Can't bound to SplashScreenService, com.miui.systemAdSolution.splashscreen.SplashScreenService");
                    delayToRebindService();
                    return;
                }
                logI("SplashScreenService started");
            } catch (Exception e) {
                logE("Can not start splash screen service!", e);
            }
        }
    }

    public void delayToBindServiceAfterBootCompleted() {
        delayToRebindService(120000L, false);
    }

    public void delayToRebindService() {
        delayToRebindService(calcDelayTime(), true);
    }

    private void delayToRebindService(long delayTime, boolean increaseRebindCount) {
        this.mHandler.removeMessages(1);
        Message msg = this.mHandler.obtainMessage(1);
        this.mHandler.sendMessageDelayed(msg, delayTime);
        if (increaseRebindCount) {
            this.mRebindCount++;
        }
        logI("SplashScreenService rebind count: " + this.mRebindCount);
    }

    private long calcDelayTime() {
        int severity;
        long aliveTime = System.currentTimeMillis() - this.mStartTime;
        if (aliveTime < SecurityManagerService.LOCK_TIME_OUT) {
            severity = 1;
        } else if (aliveTime < 3600000) {
            severity = 2;
        } else {
            severity = 3;
        }
        if (severity == this.mSeverity) {
            if (severity == 1) {
                this.mDelayTime *= 2;
            } else if (severity == 2) {
                this.mDelayTime += 10000;
            } else {
                this.mDelayTime = 10000L;
            }
        } else {
            this.mDelayTime = 10000L;
        }
        long j = this.mDelayTime;
        long j2 = j + (this.mRebindCount * j);
        this.mDelayTime = j2;
        this.mDelayTime = Math.min(j2, 3600000L);
        this.mSeverity = severity;
        logI("Restart SplashScreenService delay time " + this.mDelayTime);
        return this.mDelayTime;
    }

    public void keepSplashPackageCheckInfo(SplashPackageCheckInfo splashPackageCheckInfo) {
        this.mSplashPackageCheckInfoMap.put(splashPackageCheckInfo.getSplashPackageName(), splashPackageCheckInfo);
    }

    private boolean isSplashPackage(String packageName) {
        if (TextUtils.isEmpty(packageName)) {
            return false;
        }
        SplashPackageCheckInfo info = this.mSplashPackageCheckInfoMap.get(packageName);
        if (info == null) {
            logI("None for " + packageName);
            return false;
        } else if (info.isExpired()) {
            logI(info + " is expired, remove it");
            this.mSplashPackageCheckInfoMap.remove(packageName);
            return false;
        } else {
            boolean mt = info.matchTime();
            if (!mt) {
                logI("Mismatch time for " + packageName);
            }
            return mt;
        }
    }

    public boolean checkSplashPackageCheckInfo(SplashPackageCheckInfo splashPackageCheckInfo) {
        return splashPackageCheckInfo != null && !TextUtils.isEmpty(splashPackageCheckInfo.getSplashPackageName()) && !splashPackageCheckInfo.isExpired() && isPackageInstalled(splashPackageCheckInfo.getSplashPackageName());
    }

    private boolean isPackageInstalled(String packageName) {
        try {
            PackageInfo pi = this.mContext.getPackageManager().getPackageInfo(packageName, 0);
            if (pi == null) {
                return false;
            }
            return pi.applicationInfo != null;
        } catch (Exception e) {
            return false;
        }
    }

    public Intent requestSplashScreen(final Intent intent, final ActivityInfo aInfo) {
        final ISplashScreenService sss = this.mSplashScreenService;
        String packageName = getPackageName(aInfo);
        if (sss != null && isSplashPackage(packageName)) {
            long startTime = System.currentTimeMillis();
            logI(packageName + " requestSplashScreen startTime: " + startTime);
            Callable<Intent> call = new Callable<Intent>() { // from class: com.miui.server.SplashScreenServiceDelegate.5
                @Override // java.util.concurrent.Callable
                public Intent call() throws Exception {
                    Intent finalIntent = sss.requestSplashScreen(intent, aInfo);
                    return finalIntent != null ? finalIntent : intent;
                }
            };
            try {
                try {
                    Future<Intent> future = this.mExecutor.submit(call);
                    return future.get(2000L, TimeUnit.MILLISECONDS);
                } catch (TimeoutException e) {
                    logE("requestSplashScreen timeout exception", e);
                    return intent;
                } catch (Exception e2) {
                    logE("requestSplashScreen exception", e2);
                    return intent;
                }
            } finally {
                logCost("requestSplashScreen ", startTime, packageName);
            }
        }
        return intent;
    }

    public void activityIdle(ActivityInfo aInfo) {
    }

    public void destroyActivity(ActivityInfo aInfo) {
        ISplashScreenService sss = this.mSplashScreenService;
        String packageName = getPackageName(aInfo);
        if (sss != null && isSplashPackage(packageName)) {
            long startTime = System.currentTimeMillis();
            try {
                try {
                    sss.destroyActivity(aInfo);
                } catch (Exception e) {
                    logE("destroyActivity exception", e);
                }
            } finally {
                logCost("destroyActivity", startTime, packageName);
            }
        }
    }

    private String getPackageName(ActivityInfo aInfo) {
        if (aInfo == null || aInfo.applicationInfo == null) {
            return null;
        }
        return aInfo.applicationInfo.packageName;
    }

    private void logCost(String prefix, long startTime, String packageName) {
        if (sDebug) {
            logI(prefix + " " + (System.currentTimeMillis() - startTime) + "ms, " + packageName);
        }
    }

    public void logI(String msg) {
        logI(msg, false);
    }

    public void logI(String msg, boolean force) {
        if (sDebug || force) {
            Slog.i(TAG, msg);
        }
    }

    private void logW(String msg) {
        Slog.w(TAG, msg);
    }

    public void logE(String msg, Throwable tr) {
        Slog.e(TAG, msg, tr);
    }
}
