package com.android.server.location.provider;

import android.location.GnssStatus;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import com.android.server.location.gnss.GnssLocationProvider;
import com.android.server.location.gnss.map.AmapExtraCommand;
import com.android.server.location.provider.LocationProviderManager;
import com.miui.base.MiuiStubRegistry;
import java.util.ArrayList;
import miui.util.ObjectReference;
import miui.util.ReflectionUtils;
/* loaded from: classes.dex */
public class AmapCustomImpl implements AmapCustomStub {
    private static boolean DEBUG = Build.IS_DEBUGGABLE;
    private static final String TAG = "AmapCustomImpl";
    private ArrayList<Float> mCn0s = new ArrayList<>();
    private long mUpdateSvStatusTime = 0;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<AmapCustomImpl> {

        /* compiled from: AmapCustomImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final AmapCustomImpl INSTANCE = new AmapCustomImpl();
        }

        public AmapCustomImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public AmapCustomImpl provideNewInstance() {
            return new AmapCustomImpl();
        }
    }

    public void setLastSvStatus(GnssStatus gnssStatus) {
        synchronized (this.mCn0s) {
            this.mCn0s.clear();
            for (int i = 0; i < gnssStatus.getSatelliteCount(); i++) {
                this.mCn0s.add(Float.valueOf(gnssStatus.getCn0DbHz(i)));
            }
            this.mUpdateSvStatusTime = SystemClock.elapsedRealtime();
        }
    }

    public boolean onSpecialExtraCommand(LocationProviderManager manager, int uid, String command, Bundle extras) {
        if (command == null || extras == null) {
            Log.e(TAG, "Exception: command/bundle is null");
            return false;
        }
        if (DEBUG) {
            Log.d(TAG, "provider " + manager.getName() + " , " + command);
        }
        boolean isHandled = false;
        if (AmapExtraCommand.isSupported(command, extras)) {
            isHandled = true;
            if ("gps".equals(manager.getName()) && AmapExtraCommand.GPS_TIMEOUT_CMD.equals(command)) {
                handleAmapGpsTimeoutCmd(manager, uid, command, extras);
            }
        }
        return isHandled;
    }

    private void handleAmapGpsTimeoutCmd(LocationProviderManager manager, int uid, String command, Bundle extras) {
        fetchRegistrationState(manager, extras);
        fetchPowerPolicyState(extras);
        ObjectReference<MockableLocationProvider> mockRef = ReflectionUtils.tryGetObjectField(manager, "mProvider", MockableLocationProvider.class);
        MockableLocationProvider mockProvider = mockRef != null ? (MockableLocationProvider) mockRef.get() : null;
        if (mockProvider != null) {
            extras.putInt(AmapExtraCommand.GNSS_REAL_KEY, !mockProvider.isMock() ? 1 : 0);
            fetchGnssState(mockProvider.getProvider(), extras);
        }
        extras.putString(AmapExtraCommand.VERSION_KEY, AmapExtraCommand.VERSION_NAME);
        if (DEBUG) {
            Log.d(TAG, "response: " + extras.toString());
        }
    }

    /* JADX WARN: Code restructure failed: missing block: B:18:0x0043, code lost:
        r16 = 0;
     */
    /* JADX WARN: Code restructure failed: missing block: B:19:0x004b, code lost:
        if (r13.isActive() == false) goto L21;
     */
    /* JADX WARN: Code restructure failed: missing block: B:20:0x004d, code lost:
        r15 = 1;
     */
    /* JADX WARN: Code restructure failed: missing block: B:21:0x0050, code lost:
        r15 = 0;
     */
    /* JADX WARN: Code restructure failed: missing block: B:22:0x0052, code lost:
        r5 = r15;
        r6 = r13.getPermissionLevel();
        r15 = r13.getLastDeliveredLocation();
     */
    /* JADX WARN: Code restructure failed: missing block: B:23:0x005c, code lost:
        if (r15 == null) goto L28;
     */
    /* JADX WARN: Code restructure failed: missing block: B:25:0x0066, code lost:
        r7 = java.lang.System.currentTimeMillis() - r15.getTime();
     */
    /* JADX WARN: Code restructure failed: missing block: B:26:0x0069, code lost:
        r0 = th;
     */
    /* JADX WARN: Code restructure failed: missing block: B:31:0x0073, code lost:
        r0 = miui.util.ReflectionUtils.tryGetObjectField(r13, "mIsUsingHighPower", java.lang.Boolean.class);
     */
    /* JADX WARN: Code restructure failed: missing block: B:32:0x0083, code lost:
        if (((java.lang.Boolean) r0.get()).booleanValue() == false) goto L34;
     */
    /* JADX WARN: Code restructure failed: missing block: B:33:0x0085, code lost:
        r16 = 1;
     */
    /* JADX WARN: Code restructure failed: missing block: B:34:0x0087, code lost:
        r9 = r16;
        r2 = r13.isForeground() ? 1 : 0;
        r10 = r2;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private void fetchRegistrationState(com.android.server.location.provider.LocationProviderManager r23, android.os.Bundle r24) {
        /*
            r22 = this;
            r1 = r24
            java.lang.String r0 = "mRegistrations"
            java.lang.Class<android.util.ArrayMap> r2 = android.util.ArrayMap.class
            r3 = r23
            miui.util.ObjectReference r2 = miui.util.ReflectionUtils.tryGetObjectField(r3, r0, r2)
            if (r2 == 0) goto L15
            java.lang.Object r0 = r2.get()
            android.util.ArrayMap r0 = (android.util.ArrayMap) r0
            goto L16
        L15:
            r0 = 0
        L16:
            r4 = r0
            if (r4 != 0) goto L21
            java.lang.String r0 = "AmapCustomImpl"
            java.lang.String r5 = "Exception: mRegistrations is null"
            android.util.Log.e(r0, r5)
            return
        L21:
            r5 = -1
            r6 = -1
            r7 = -1
            r9 = -1
            r10 = -1
            monitor-enter(r4)
            int r0 = r4.size()     // Catch: java.lang.Throwable -> Lb9
            java.lang.String r11 = "listenerHashcode"
            int r11 = r1.getInt(r11)     // Catch: java.lang.Throwable -> Lb9
            r12 = 0
        L33:
            if (r12 >= r0) goto L98
            java.lang.Object r13 = r4.valueAt(r12)     // Catch: java.lang.Throwable -> Lb9
            com.android.server.location.provider.LocationProviderManager$Registration r13 = (com.android.server.location.provider.LocationProviderManager.Registration) r13     // Catch: java.lang.Throwable -> Lb9
            r14 = r22
            int r15 = r14.listenerHashCode(r13)     // Catch: java.lang.Throwable -> L96
            if (r15 != r11) goto L8f
            boolean r15 = r13.isActive()     // Catch: java.lang.Throwable -> L96
            r16 = 0
            r17 = 1
            if (r15 == 0) goto L50
            r15 = r17
            goto L52
        L50:
            r15 = r16
        L52:
            r5 = r15
            int r15 = r13.getPermissionLevel()     // Catch: java.lang.Throwable -> L96
            r6 = r15
            android.location.Location r15 = r13.getLastDeliveredLocation()     // Catch: java.lang.Throwable -> L96
            if (r15 == 0) goto L6d
            long r18 = java.lang.System.currentTimeMillis()     // Catch: java.lang.Throwable -> L69
            long r20 = r15.getTime()     // Catch: java.lang.Throwable -> L69
            long r7 = r18 - r20
            goto L6d
        L69:
            r0 = move-exception
            r19 = r2
            goto Lbe
        L6d:
            r18 = r0
            java.lang.String r0 = "mIsUsingHighPower"
            r19 = r2
            java.lang.Class<java.lang.Boolean> r2 = java.lang.Boolean.class
            miui.util.ObjectReference r0 = miui.util.ReflectionUtils.tryGetObjectField(r13, r0, r2)     // Catch: java.lang.Throwable -> Lc0
            java.lang.Object r2 = r0.get()     // Catch: java.lang.Throwable -> Lc0
            java.lang.Boolean r2 = (java.lang.Boolean) r2     // Catch: java.lang.Throwable -> Lc0
            boolean r2 = r2.booleanValue()     // Catch: java.lang.Throwable -> Lc0
            if (r2 == 0) goto L87
            r16 = r17
        L87:
            r9 = r16
            boolean r2 = r13.isForeground()     // Catch: java.lang.Throwable -> Lc0
            r10 = r2
            goto L9e
        L8f:
            r18 = r0
            r19 = r2
            int r12 = r12 + 1
            goto L33
        L96:
            r0 = move-exception
            goto Lbc
        L98:
            r14 = r22
            r18 = r0
            r19 = r2
        L9e:
            monitor-exit(r4)     // Catch: java.lang.Throwable -> Lc0
            java.lang.String r0 = "app_active"
            r1.putInt(r0, r5)
            java.lang.String r0 = "app_permission"
            r1.putInt(r0, r6)
            java.lang.String r0 = "app_last_report_second"
            r1.putLong(r0, r7)
            java.lang.String r0 = "app_power_mode"
            r1.putInt(r0, r9)
            java.lang.String r0 = "app_forground"
            r1.putInt(r0, r10)
            return
        Lb9:
            r0 = move-exception
            r14 = r22
        Lbc:
            r19 = r2
        Lbe:
            monitor-exit(r4)     // Catch: java.lang.Throwable -> Lc0
            throw r0
        Lc0:
            r0 = move-exception
            goto Lbe
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.location.provider.AmapCustomImpl.fetchRegistrationState(com.android.server.location.provider.LocationProviderManager, android.os.Bundle):void");
    }

    private int listenerHashCode(LocationProviderManager.Registration registration) {
        String listenerId = registration.getIdentity().getListenerId();
        int length = listenerId.length();
        int index = listenerId.lastIndexOf(64, length - 1);
        if (index == -1 || length - index < 1) {
            return -1;
        }
        try {
            int listenerHashCode = Integer.parseInt(listenerId.substring(index + 1));
            return listenerHashCode;
        } catch (Exception e) {
            Log.e(TAG, "caught exception from Integer.parseInt " + e.getMessage());
            return -1;
        }
    }

    private void fetchPowerPolicyState(Bundle extras) {
        extras.putInt(AmapExtraCommand.APP_CTRL_KEY, -1);
        extras.putString(AmapExtraCommand.APP_CTRL_LOG_KEY, "The device is not supported");
    }

    private void fetchGnssState(AbstractLocationProvider provider, Bundle extras) {
        AbstractLocationProvider provider2;
        if (provider != null && (provider instanceof DelegateLocationProvider)) {
            ObjectReference<AbstractLocationProvider> deleRef = ReflectionUtils.tryGetObjectField(provider, "mDelegate", AbstractLocationProvider.class);
            AbstractLocationProvider deleProvider = deleRef != null ? (AbstractLocationProvider) deleRef.get() : null;
            provider2 = deleProvider;
        } else {
            provider2 = provider;
        }
        if (provider2 != null && (provider2 instanceof GnssLocationProvider)) {
            ObjectReference<Long> fixtime = ReflectionUtils.tryGetObjectField((GnssLocationProvider) provider2, "mLastFixTime", Long.class);
            ObjectReference<Boolean> started = ReflectionUtils.tryGetObjectField((GnssLocationProvider) provider2, "mStarted", Boolean.class);
            boolean booleanValue = ((Boolean) started.get()).booleanValue();
            int isStart = booleanValue ? 1 : 0;
            extras.putInt(AmapExtraCommand.GNSS_STATUS_KEY, isStart);
            long deltaTime = SystemClock.elapsedRealtime() - ((Long) fixtime.get()).longValue();
            extras.putLong(AmapExtraCommand.GNSS_LAST_RPT_TIME_KEY, deltaTime);
            synchronized (this.mCn0s) {
                int over0Cnt = -1;
                int over20Cnt = -1;
                int svCnt = -1;
                try {
                    try {
                        long deltaMs = SystemClock.elapsedRealtime() - this.mUpdateSvStatusTime;
                        if (booleanValue && deltaMs <= 5000) {
                            over0Cnt = 0;
                            over20Cnt = 0;
                            svCnt = this.mCn0s.size();
                            int i = 0;
                            while (i < svCnt) {
                                float cn0 = this.mCn0s.get(i).floatValue();
                                AbstractLocationProvider provider3 = provider2;
                                ObjectReference<Long> fixtime2 = fixtime;
                                if (cn0 > 0.0d) {
                                    over0Cnt++;
                                }
                                if (cn0 > 20.0d) {
                                    over20Cnt++;
                                }
                                i++;
                                provider2 = provider3;
                                fixtime = fixtime2;
                            }
                        }
                        extras.putInt(AmapExtraCommand.SAT_ALL_CNT_KEY, svCnt);
                        extras.putInt(AmapExtraCommand.SAT_SNR_OVER0_CNT_KEY, over0Cnt);
                        extras.putInt(AmapExtraCommand.SAT_SNR_OVER20_CNT_KEY, over20Cnt);
                        return;
                    } catch (Throwable th) {
                        th = th;
                        throw th;
                    }
                } catch (Throwable th2) {
                    th = th2;
                    throw th;
                }
            }
        }
        Log.e(TAG, "Exception: bad argument for provider");
    }
}
