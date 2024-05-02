package com.android.server.alarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkRequest;
import android.os.Binder;
import android.os.SystemClock;
import android.util.Log;
import com.android.server.alarm.AlarmManagerServiceStubImpl;
import com.android.server.alarm.AlarmStore;
import com.android.server.am.ProcessUtils;
import com.miui.base.MiuiStubRegistry;
import com.miui.server.SecurityManagerService;
import com.miui.whetstone.client.WhetstoneClientManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import miui.os.Build;
/* loaded from: classes.dex */
public class AlarmManagerServiceStubImpl implements AlarmManagerServiceStub {
    private static final int DEFAULT_ALIGN_PERIOD = 300000;
    private static final int FOREGROUND_APP_ADJ = 0;
    private static final int MIN_ALIGN_PERIOD = 235000;
    private static final long PENDING_DELAY_TIME = 259200000;
    private static final int PERCEPTIBLE_APP_ADJ = 200;
    private static final String TAG = "AlarmManager";
    private static final String XMSF_HEART_BEAT = "*walarm*:com.xiaomi.push.PING_TIMER";
    private AlarmManagerService mAlarmService;
    private static final boolean DEBUG = Build.isDebuggable();
    private static final Object sLock = new Object();
    private volatile boolean mNetAvailable = true;
    private volatile boolean mScreenOn = true;
    private volatile boolean mPendingAllowed = false;
    private long mCurAlignPeriod = 300000;
    private long mBaseAlignTime = SystemClock.elapsedRealtime();
    private int mXmsfUid = -1;
    private List<String> ADJUST_WHITE_LIST = new ArrayList();
    private List<String> PERSIST_PACKAGES = new ArrayList();
    Runnable mAdjustTask = new AnonymousClass1();

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<AlarmManagerServiceStubImpl> {

        /* compiled from: AlarmManagerServiceStubImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final AlarmManagerServiceStubImpl INSTANCE = new AlarmManagerServiceStubImpl();
        }

        public AlarmManagerServiceStubImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public AlarmManagerServiceStubImpl provideNewInstance() {
            return new AlarmManagerServiceStubImpl();
        }
    }

    public String[] filterPersistPackages(String[] pkgList) {
        List<String> filteredPkgList = new ArrayList<>();
        if (pkgList != null && pkgList.length > 0) {
            for (String pkg : pkgList) {
                if (!this.PERSIST_PACKAGES.contains(pkg)) {
                    filteredPkgList.add(pkg);
                }
            }
        }
        return (String[]) filteredPkgList.toArray(new String[0]);
    }

    /* JADX WARN: Code restructure failed: missing block: B:57:0x011b, code lost:
        if (r7 <= ((3 * r14) / 2)) goto L59;
     */
    /* JADX WARN: Code restructure failed: missing block: B:62:0x012a, code lost:
        if (r7 > (r14 * r19)) goto L63;
     */
    /* JADX WARN: Removed duplicated region for block: B:69:0x013d  */
    /* JADX WARN: Removed duplicated region for block: B:75:0x017a  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public boolean alignAlarmLocked(com.android.server.alarm.Alarm r22) {
        /*
            Method dump skipped, instructions count: 390
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.alarm.AlarmManagerServiceStubImpl.alignAlarmLocked(com.android.server.alarm.Alarm):boolean");
    }

    public boolean checkAlarmIsAllowedSend(Context context, Alarm alarm) {
        if (alarm == null || alarm.operation == null) {
            return true;
        }
        return WhetstoneClientManager.isAlarmAllowedLocked(Binder.getCallingPid(), alarm.operation.getCreatorUid(), alarm.statsTag, CheckIfAlarmGenralRistrictApply(alarm.operation.getCreatorUid(), Binder.getCallingPid()));
    }

    public boolean adjustAlarmLocked(Alarm a) {
        if ((a.flags & 15) != 0 || a.alarmClock != null || AlarmManagerService.isTimeTickAlarm(a) || this.ADJUST_WHITE_LIST.contains(a.sourcePackage)) {
            return false;
        }
        long temp = 0;
        if (!this.mScreenOn || !this.mNetAvailable) {
            if (DEBUG) {
                String stringBuilder = "pending alarm: " + a.packageName + ", tag: " + a.statsTag + ", origWhen: " + a.getWhenElapsed();
                Log.d(TAG, stringBuilder);
            }
            temp = a.getRequestedElapsed() + PENDING_DELAY_TIME;
        }
        return a.setPolicyElapsed(5, temp);
    }

    public static boolean CheckIfAlarmGenralRistrictApply(int uid, int pid) {
        if (uid <= 10000) {
            return false;
        }
        int curAdj = ProcessUtils.getCurAdjByPid(pid);
        int procState = ProcessUtils.getProcStateByPid(pid);
        boolean hasForegroundActivities = ProcessUtils.hasForegroundActivities(pid);
        if (hasForegroundActivities || (curAdj >= 0 && curAdj <= 200 && procState != 11)) {
            return false;
        }
        return true;
    }

    /* renamed from: com.android.server.alarm.AlarmManagerServiceStubImpl$1 */
    /* loaded from: classes.dex */
    public class AnonymousClass1 implements Runnable {
        AnonymousClass1() {
            AlarmManagerServiceStubImpl.this = this$0;
        }

        @Override // java.lang.Runnable
        public void run() {
            synchronized (AlarmManagerServiceStubImpl.this.mAlarmService.mLock) {
                AlarmStore store = AlarmManagerServiceStubImpl.this.mAlarmService.mAlarmStore;
                if (store != null && store.updateAlarmDeliveries(new AlarmStore.AlarmDeliveryCalculator() { // from class: com.android.server.alarm.AlarmManagerServiceStubImpl$1$$ExternalSyntheticLambda0
                    public final boolean updateAlarmDelivery(Alarm alarm) {
                        return AlarmManagerServiceStubImpl.AnonymousClass1.this.m205xfe4b542(alarm);
                    }
                })) {
                    AlarmManagerServiceStubImpl.this.mAlarmService.rescheduleKernelAlarmsLocked();
                }
            }
        }

        /* renamed from: lambda$run$0$com-android-server-alarm-AlarmManagerServiceStubImpl$1 */
        public /* synthetic */ boolean m205xfe4b542(Alarm a) {
            return AlarmManagerServiceStubImpl.this.adjustAlarmLocked(a);
        }
    }

    public void updateAlarmPendingState() {
        if (this.mAlarmService.mHandler != null) {
            this.mAlarmService.mHandler.removeCallbacks(this.mAdjustTask);
            this.mAlarmService.mHandler.postDelayed(this.mAdjustTask, (long) SecurityManagerService.LOCK_TIME_OUT);
        }
    }

    /* loaded from: classes.dex */
    private class ScreenReceiver extends BroadcastReceiver {
        private ScreenReceiver() {
            AlarmManagerServiceStubImpl.this = r1;
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            boolean change = false;
            synchronized (AlarmManagerServiceStubImpl.sLock) {
                boolean z = false;
                if ("android.intent.action.SCREEN_ON".equals(action)) {
                    AlarmManagerServiceStubImpl.this.mScreenOn = true;
                } else if ("android.intent.action.SCREEN_OFF".equals(action)) {
                    AlarmManagerServiceStubImpl.this.mScreenOn = false;
                }
                if (!AlarmManagerServiceStubImpl.this.mScreenOn || !AlarmManagerServiceStubImpl.this.mNetAvailable) {
                    z = true;
                }
                boolean allow = z;
                if (AlarmManagerServiceStubImpl.this.mPendingAllowed != allow) {
                    AlarmManagerServiceStubImpl.this.mPendingAllowed = allow;
                    change = true;
                }
            }
            if (change) {
                AlarmManagerServiceStubImpl.this.updateAlarmPendingState();
            }
        }
    }

    /* loaded from: classes.dex */
    private class NetworkCallbackImpl extends ConnectivityManager.NetworkCallback {
        private NetworkCallbackImpl() {
            AlarmManagerServiceStubImpl.this = r1;
        }

        @Override // android.net.ConnectivityManager.NetworkCallback
        public void onAvailable(Network network) {
            boolean change = false;
            synchronized (AlarmManagerServiceStubImpl.sLock) {
                boolean z = true;
                AlarmManagerServiceStubImpl.this.mNetAvailable = true;
                if (AlarmManagerServiceStubImpl.this.mScreenOn && AlarmManagerServiceStubImpl.this.mNetAvailable) {
                    z = false;
                }
                boolean allow = z;
                if (AlarmManagerServiceStubImpl.this.mPendingAllowed != allow) {
                    AlarmManagerServiceStubImpl.this.mPendingAllowed = allow;
                    change = true;
                }
            }
            if (change) {
                AlarmManagerServiceStubImpl.this.updateAlarmPendingState();
            }
        }

        @Override // android.net.ConnectivityManager.NetworkCallback
        public void onLost(Network network) {
            boolean change = false;
            synchronized (AlarmManagerServiceStubImpl.sLock) {
                boolean z = false;
                AlarmManagerServiceStubImpl.this.mNetAvailable = false;
                if (!AlarmManagerServiceStubImpl.this.mScreenOn || !AlarmManagerServiceStubImpl.this.mNetAvailable) {
                    z = true;
                }
                boolean allow = z;
                if (AlarmManagerServiceStubImpl.this.mPendingAllowed != allow) {
                    AlarmManagerServiceStubImpl.this.mPendingAllowed = allow;
                    change = true;
                }
            }
            if (change) {
                AlarmManagerServiceStubImpl.this.updateAlarmPendingState();
            }
        }
    }

    public void init(AlarmManagerService service) {
        this.ADJUST_WHITE_LIST = new ArrayList(Arrays.asList(service.getContext().getResources().getStringArray(285409280)));
        this.PERSIST_PACKAGES = new ArrayList(Arrays.asList(service.getContext().getResources().getStringArray(285409282)));
        if (Build.IS_INTERNATIONAL_BUILD) {
            List<String> ADJUST_WHITE_LIST_GLOBAL = new ArrayList<>(Arrays.asList(service.getContext().getResources().getStringArray(285409281)));
            for (String pkgName : ADJUST_WHITE_LIST_GLOBAL) {
                this.ADJUST_WHITE_LIST.add(pkgName);
            }
        }
        this.mAlarmService = service;
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.SCREEN_ON");
        filter.addAction("android.intent.action.SCREEN_OFF");
        service.getContext().registerReceiver(new ScreenReceiver(), filter);
        NetworkCallbackImpl networkCallback = new NetworkCallbackImpl();
        NetworkRequest.Builder builder = new NetworkRequest.Builder();
        NetworkRequest request = builder.build();
        ConnectivityManager connectivityManager = (ConnectivityManager) service.getContext().getSystemService("connectivity");
        connectivityManager.registerNetworkCallback(request, networkCallback);
    }
}
