package com.android.server.wm;

import android.os.MiPerf;
import android.util.Slog;
import com.miui.base.MiuiStubRegistry;
import java.util.HashMap;
/* loaded from: classes.dex */
public class MiuiMiPerfStubImpl implements MiuiMiPerfStub {
    private static final String TAG = "MiuiMiPerfStubImpl";
    private static HashMap<String, HashMap<String, String>> mMiperfXmlMap = MiPerf.miPerfGetXmlMap();
    private boolean isRecoverWrite = false;
    private boolean isRelease = false;
    private boolean isCommon = false;
    private boolean isDebug = false;
    private String lastPackName = " ";
    private String lastActName = " ";

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<MiuiMiPerfStubImpl> {

        /* compiled from: MiuiMiPerfStubImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final MiuiMiPerfStubImpl INSTANCE = new MiuiMiPerfStubImpl();
        }

        public MiuiMiPerfStubImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public MiuiMiPerfStubImpl provideNewInstance() {
            return new MiuiMiPerfStubImpl();
        }
    }

    public MiuiMiPerfStubImpl() {
        Slog.d(TAG, "MiuiMiPerfStubImpl is Initialized!");
    }

    public String getSystemBoostMode(String packageName, String activityName) {
        this.isCommon = false;
        if (mMiperfXmlMap.containsKey(packageName)) {
            Object act_map = mMiperfXmlMap.get(packageName);
            HashMap<String, String> map_tmp = (HashMap) act_map;
            if (map_tmp.containsKey(activityName)) {
                if (this.isDebug) {
                    Slog.d(TAG, "Match BoostMode successfully, BoostMode is " + map_tmp.get(activityName));
                }
                return map_tmp.get(activityName);
            } else if (map_tmp.containsKey("Common")) {
                this.isCommon = true;
                return map_tmp.get("Common");
            }
        }
        if (this.isDebug) {
            Slog.d(TAG, "Match BoostMode failed, there is no MiPerfBoost!");
            return "BOOSTMODE_NULL";
        }
        return "BOOSTMODE_NULL";
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Code restructure failed: missing block: B:29:0x005c, code lost:
        if (r1.equals("BOOSTMODE_NULL") != false) goto L34;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private void miPerfSystemBoostNotify(int r7, java.lang.String r8, java.lang.String r9, java.lang.String r10) {
        /*
            r6 = this;
            r0 = 0
            java.lang.String r1 = r6.getSystemBoostMode(r8, r9)
            boolean r2 = r6.isCommon
            if (r2 == 0) goto Lb
            java.lang.String r9 = "Common"
        Lb:
            boolean r2 = r6.isRecoverWrite
            r3 = 0
            if (r2 == 0) goto L1f
            java.lang.String r2 = r6.lastPackName
            if (r8 != r2) goto L18
            java.lang.String r2 = r6.lastActName
            if (r9 == r2) goto L1f
        L18:
            java.lang.String r2 = "WRITENODE_RECOVER"
            android.os.MiPerf.miPerfSystemBoostAcquire(r7, r8, r9, r2)
            r6.isRecoverWrite = r3
        L1f:
            boolean r2 = r6.isRelease
            if (r2 == 0) goto L2a
            java.lang.String r2 = "PERFLOCK_RELEASE"
            android.os.MiPerf.miPerfSystemBoostAcquire(r7, r8, r9, r2)
            r6.isRelease = r3
        L2a:
            java.lang.String r2 = r6.lastPackName
            if (r8 != r2) goto L34
            java.lang.String r2 = r6.lastActName
            if (r9 != r2) goto L34
            java.lang.String r1 = "BOOSTMODE_NULL"
        L34:
            r6.lastPackName = r8
            r6.lastActName = r9
            r2 = -1
            int r4 = r1.hashCode()
            r5 = 1
            switch(r4) {
                case -1721155661: goto L5f;
                case -754587008: goto L56;
                case 1186582712: goto L4c;
                case 1394821899: goto L42;
                default: goto L41;
            }
        L41:
            goto L69
        L42:
            java.lang.String r3 = "PERFLOCK_ACQUIRE"
            boolean r3 = r1.equals(r3)
            if (r3 == 0) goto L41
            r3 = r5
            goto L6a
        L4c:
            java.lang.String r3 = "WRITENODE_ACQUIRE"
            boolean r3 = r1.equals(r3)
            if (r3 == 0) goto L41
            r3 = 2
            goto L6a
        L56:
            java.lang.String r4 = "BOOSTMODE_NULL"
            boolean r4 = r1.equals(r4)
            if (r4 == 0) goto L41
            goto L6a
        L5f:
            java.lang.String r3 = "MULTIPLEMODE"
            boolean r3 = r1.equals(r3)
            if (r3 == 0) goto L41
            r3 = 3
            goto L6a
        L69:
            r3 = r2
        L6a:
            java.lang.String r2 = "MiuiMiPerfStubImpl"
            switch(r3) {
                case 0: goto L79;
                case 1: goto L76;
                case 2: goto L73;
                case 3: goto L70;
                default: goto L6f;
            }
        L6f:
            goto L83
        L70:
            r6.isRecoverWrite = r5
            goto L83
        L73:
            r6.isRecoverWrite = r5
            goto L83
        L76:
            r6.isRelease = r5
            goto L83
        L79:
            boolean r3 = r6.isDebug
            if (r3 == 0) goto L82
            java.lang.String r3 = "miPerfSystemBoostNotify: no MiPerfBoost!"
            android.util.Slog.d(r2, r3)
        L82:
            return
        L83:
            int r0 = android.os.MiPerf.miPerfSystemBoostAcquire(r7, r8, r9, r1)
            java.lang.StringBuilder r3 = new java.lang.StringBuilder
            r3.<init>()
            java.lang.String r4 = "miPerfSystemBoostNotify: return = "
            java.lang.StringBuilder r3 = r3.append(r4)
            java.lang.StringBuilder r3 = r3.append(r0)
            java.lang.String r4 = " System BoostScenes = "
            java.lang.StringBuilder r3 = r3.append(r4)
            java.lang.StringBuilder r3 = r3.append(r10)
            java.lang.String r4 = " BoostMode = "
            java.lang.StringBuilder r3 = r3.append(r4)
            java.lang.StringBuilder r3 = r3.append(r1)
            java.lang.String r4 = ", pid = "
            java.lang.StringBuilder r3 = r3.append(r4)
            java.lang.StringBuilder r3 = r3.append(r7)
            java.lang.String r4 = ", packagename = "
            java.lang.StringBuilder r3 = r3.append(r4)
            java.lang.StringBuilder r3 = r3.append(r8)
            java.lang.String r4 = ", activityname = "
            java.lang.StringBuilder r3 = r3.append(r4)
            java.lang.StringBuilder r3 = r3.append(r9)
            java.lang.String r3 = r3.toString()
            android.util.Slog.d(r2, r3)
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wm.MiuiMiPerfStubImpl.miPerfSystemBoostNotify(int, java.lang.String, java.lang.String, java.lang.String):void");
    }

    public void onAfterActivityResumed(ActivityRecord resumedActivity) {
        if (resumedActivity.app == null || resumedActivity.info == null) {
            return;
        }
        int pid = resumedActivity.app.getPid();
        String activityName = resumedActivity.info.name;
        String packageName = resumedActivity.info.packageName;
        long startMiperfTime = System.currentTimeMillis();
        miPerfSystemBoostNotify(pid, packageName, activityName, "onAfterActivityResumed");
        long durationMiperf = System.currentTimeMillis() - startMiperfTime;
        if (durationMiperf > 50) {
            Slog.w(TAG, "Call miPerfSystemBoostNotify is timeout, took " + durationMiperf + "ms.");
        }
    }
}
