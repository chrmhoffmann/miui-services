package com.android.server;

import android.content.Context;
import android.content.Intent;
import android.net.UidRangeParcel;
import android.os.Binder;
import android.os.IBinder;
import android.os.INetworkManagementService;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.ServiceSpecificException;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.server.am.SplitScreenReporter;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import com.android.internal.net.IOemNetd;
import com.android.internal.net.IOemNetdUnsolicitedEventListener;
import com.android.server.audio.AudioServiceStubImpl;
import com.android.server.wm.ActivityTaskSupervisorImpl;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
/* loaded from: classes.dex */
public class MiuiNetworkManagementService {
    private static final int ALLOW = 0;
    private static final int MIUI_FIREWALL_RESPONSE_CODE = 699;
    private static final int POWER_SAVE_IDLETIMER_LABEL = 118;
    private static final int REJECT = 1;
    private static final String TAG = "NetworkManagement";
    private static final int TYPE_ALL = 3;
    private static final int TYPE_MOBILE = 0;
    private static final int TYPE_ROAMING = 2;
    private static final int TYPE_WIFI = 1;
    private static MiuiNetworkManagementService sInstance;
    private IBinder mBinder;
    private final Context mContext;
    private Set<Integer> mListenedIdleTimerType;
    private OemNetdUnsolicitedEventListener mListenter;
    private SparseArray<Set<MiuiFireRule>> mMiuiFireRuleCache;
    private IOemNetd mNetd;
    private NetworkEventObserver mObserver;
    private INetworkManagementService mNms = null;
    private final Object mLock = new Object();
    private int mCurrentNetworkState = -1;
    private int mStateSetterUid = -1;
    private int mStateSetterPid = -1;

    /* loaded from: classes.dex */
    public interface NetworkEventObserver {
        void uidDataActivityChanged(String str, int i, boolean z, long j);
    }

    public static synchronized MiuiNetworkManagementService Init(Context context) {
        MiuiNetworkManagementService miuiNetworkManagementService;
        synchronized (MiuiNetworkManagementService.class) {
            miuiNetworkManagementService = new MiuiNetworkManagementService(context);
            sInstance = miuiNetworkManagementService;
        }
        return miuiNetworkManagementService;
    }

    public static synchronized MiuiNetworkManagementService getInstance() {
        MiuiNetworkManagementService miuiNetworkManagementService;
        synchronized (MiuiNetworkManagementService.class) {
            miuiNetworkManagementService = sInstance;
        }
        return miuiNetworkManagementService;
    }

    private MiuiNetworkManagementService(Context context) {
        this.mContext = context;
        HashSet hashSet = new HashSet();
        this.mListenedIdleTimerType = hashSet;
        hashSet.add(1);
    }

    public void setOemNetd(IBinder ib) throws RemoteException {
        this.mBinder = ib;
        this.mNetd = IOemNetd.Stub.asInterface(ib);
        OemNetdUnsolicitedEventListener oemNetdUnsolicitedEventListener = new OemNetdUnsolicitedEventListener();
        this.mListenter = oemNetdUnsolicitedEventListener;
        this.mNetd.registerOemUnsolicitedEventListener(oemNetdUnsolicitedEventListener);
    }

    public boolean enableWmmer(boolean enabled) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            return this.mNetd.enableWmmer(enabled);
        } catch (RemoteException | ServiceSpecificException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean enableLimitter(boolean enabled) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            return this.mNetd.enableLimitter(enabled);
        } catch (RemoteException | ServiceSpecificException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateWmm(int uid, int wmm) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            return this.mNetd.updateWmm(uid, wmm);
        } catch (RemoteException | ServiceSpecificException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean whiteListUid(int uid, boolean add) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            return this.mNetd.whiteListUid(uid, add);
        } catch (RemoteException | ServiceSpecificException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean setLimit(boolean enabled, long rate) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            return this.mNetd.setLimit(enabled, rate);
        } catch (RemoteException | ServiceSpecificException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean enableIptablesRestore(boolean enabled) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            return this.mNetd.enableIptablesRestore(enabled);
        } catch (RemoteException | ServiceSpecificException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean listenUidDataActivity(int protocol, int uid, int type, int timeout, boolean listen) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            this.mNetd.listenUidDataActivity(protocol, uid, type, timeout, listen);
            if (listen) {
                this.mListenedIdleTimerType.add(Integer.valueOf(type));
            } else {
                this.mListenedIdleTimerType.remove(Integer.valueOf(type));
            }
            return false;
        } catch (RemoteException | ServiceSpecificException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateIface(String iface) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            this.mNetd.updateIface(iface);
            if (iface.startsWith("wlan")) {
                this.mListenedIdleTimerType.add(1);
            }
            return false;
        } catch (RemoteException | ServiceSpecificException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean addMiuiFirewallSharedUid(int uid) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            return this.mNetd.addMiuiFirewallSharedUid(uid);
        } catch (RemoteException | ServiceSpecificException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void setPidForPackage(String packageName, int pid, int uid) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            this.mNetd.setPidForPackage(packageName, pid, uid);
        } catch (RemoteException | ServiceSpecificException e) {
            e.printStackTrace();
        }
    }

    public boolean setMiuiFirewallRule(String packageName, int uid, int rule, int type) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            setMiuiFirewallRule(packageName, uid, rule, type, Binder.getCallingUid(), Binder.getCallingPid());
            return this.mNetd.setMiuiFirewallRule(packageName, uid, rule, type);
        } catch (RemoteException | ServiceSpecificException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static UidRangeParcel makeUidRangeParcel(int start, int stop) {
        UidRangeParcel range = new UidRangeParcel(start, stop);
        return range;
    }

    private void closeSocketsForFirewall(String fwType, int[] uids, boolean isWhitelistUid) {
        int[] exemptUids;
        UidRangeParcel[] ranges;
        if (fwType.equals("fw_doze") && isWhitelistUid) {
            ranges = new UidRangeParcel[]{makeUidRangeParcel(AudioServiceStubImpl.READ_MUSIC_PLAY_BACK_DELAY, Integer.MAX_VALUE)};
            exemptUids = uids;
        } else {
            ranges = new UidRangeParcel[uids.length];
            for (int i = 0; i < uids.length; i++) {
                ranges[i] = makeUidRangeParcel(uids[i], uids[i]);
            }
            exemptUids = new int[0];
        }
        try {
            Class<?> clz = Class.forName("com.android.internal.net.IOemNetd$Stub");
            Method asInterface = clz.getDeclaredMethod("asInterface", IBinder.class);
            Object IOemNetdObj = asInterface.invoke(null, this.mBinder);
            Method socketDestroy = IOemNetdObj.getClass().getDeclaredMethod("socketDestroy", UidRangeParcel[].class, int[].class);
            if (socketDestroy != null) {
                socketDestroy.invoke(IOemNetdObj, ranges, exemptUids);
            }
        } catch (Exception e) {
            Log.e(TAG, "closeSocketsForFirewall Exception" + fwType + ": " + e.toString());
        }
    }

    public void doDesSocketForUid(String fwType, int[] uids, boolean isWhitelistUid) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            Class<?> clz = Class.forName("com.android.internal.net.IOemNetd$Stub");
            Method asInterface = clz.getDeclaredMethod("asInterface", IBinder.class);
            Object IOemNetdObj = asInterface.invoke(null, this.mBinder);
            Method doFireWallForUid = IOemNetdObj.getClass().getDeclaredMethod("doFireWallForUid", String.class, int[].class, Boolean.TYPE);
            if (doFireWallForUid != null) {
                doFireWallForUid.invoke(IOemNetdObj, fwType, uids, Boolean.valueOf(isWhitelistUid));
            }
        } catch (Exception e) {
            Log.e(TAG, "doDesSocketForUid Exception: " + e.toString());
        }
        closeSocketsForFirewall(fwType, uids, isWhitelistUid);
    }

    public void doRestoreSockForUid(String fwType) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            Class<?> clz = Class.forName("com.android.internal.net.IOemNetd$Stub");
            Method asInterface = clz.getDeclaredMethod("asInterface", IBinder.class);
            Object IOemNetdObj = asInterface.invoke(null, this.mBinder);
            Method doRestoreSockForUid = IOemNetdObj.getClass().getDeclaredMethod("doRestoreSockForUid", String.class);
            if (doRestoreSockForUid != null) {
                doRestoreSockForUid.invoke(IOemNetdObj, fwType);
            }
        } catch (Exception e) {
            Log.e(TAG, "doRestoreSockForUid Exception: " + e.toString());
        }
    }

    public void modifySuspendBaseTime() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            IBinder mBinder = ServiceManager.getService("suspend_control");
            Class<?> clz = Class.forName("android.system.suspend.ISuspendControlService$Stub");
            Method asInterface = clz.getDeclaredMethod("asInterface", IBinder.class);
            Object ISuspendControlObj = asInterface.invoke(null, mBinder);
            Method modifySuspendTime = ISuspendControlObj.getClass().getDeclaredMethod("modifySuspendTime", new Class[0]);
            if (modifySuspendTime != null) {
                modifySuspendTime.invoke(ISuspendControlObj, new Object[0]);
            }
            Log.e(TAG, "modify Suspend Base Time");
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "SuspendControlService reflection Exception");
        }
    }

    public boolean setCurrentNetworkState(int state) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            setCurrentNetworkState(state, Binder.getCallingUid(), Binder.getCallingPid());
            return this.mNetd.setCurrentNetworkState(state);
        } catch (RemoteException | ServiceSpecificException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean enableRps(String iface, boolean enable) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            return this.mNetd.enableRps(iface, enable);
        } catch (RemoteException | ServiceSpecificException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void setNetworkEventObserver(NetworkEventObserver observer) {
        this.mObserver = observer;
    }

    boolean filterExtendEvent(int code, String raw, String[] cooked) {
        return false;
    }

    public boolean miuiNotifyInterfaceClassActivity(int type, boolean isActive, long tsNanos, int uid, boolean fromRadio) {
        String lable;
        if (this.mListenedIdleTimerType.contains(Integer.valueOf(type)) && (lable = convertTypeToLable(type)) != null) {
            Log.d(TAG, lable + " <====> " + type);
            NetworkEventObserver networkEventObserver = this.mObserver;
            if (networkEventObserver != null) {
                networkEventObserver.uidDataActivityChanged(lable, uid, isActive, tsNanos);
                return true;
            }
            return true;
        }
        return false;
    }

    private String convertTypeToLable(int type) {
        if (type == 1) {
            String lable = SystemProperties.get("wifi.interface", "wlan0");
            return lable;
        } else if (type <= POWER_SAVE_IDLETIMER_LABEL) {
            return null;
        } else {
            String lable2 = String.valueOf(type);
            return lable2;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class OemNetdUnsolicitedEventListener extends IOemNetdUnsolicitedEventListener.Stub {
        private OemNetdUnsolicitedEventListener() {
            MiuiNetworkManagementService.this = r1;
        }

        @Override // com.android.internal.net.IOemNetdUnsolicitedEventListener
        public void onRegistered() throws RemoteException {
            Log.d(MiuiNetworkManagementService.TAG, "onRegistered");
        }

        @Override // com.android.internal.net.IOemNetdUnsolicitedEventListener
        public void onFirewallBlocked(int code, String packageName) throws RemoteException {
            Log.d(MiuiNetworkManagementService.TAG, String.format("code=%d, pkg=%s", Integer.valueOf(code), packageName));
            if (MiuiNetworkManagementService.MIUI_FIREWALL_RESPONSE_CODE == code && packageName != null) {
                Intent intent = new Intent("miui.intent.action.FIREWALL");
                intent.setPackage(ActivityTaskSupervisorImpl.MIUI_APP_LOCK_PACKAGE_NAME);
                intent.putExtra(SplitScreenReporter.STR_PKG, packageName);
                MiuiNetworkManagementService.this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL, "com.miui.permission.FIREWALL");
            }
        }
    }

    public boolean enableQos(boolean enabled) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            return this.mNetd.enableQos(enabled);
        } catch (RemoteException | ServiceSpecificException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean setQos(int protocol, int uid, int tos, boolean add) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            return this.mNetd.setQos(protocol, uid, tos, add);
        } catch (RemoteException | ServiceSpecificException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean enableMobileTrafficLimit(boolean enabled, String iface) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            return this.mNetd.enableMobileTrafficLimit(enabled, iface);
        } catch (RemoteException | ServiceSpecificException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean setMobileTrafficLimit(boolean enabled, long rate) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            return this.mNetd.setMobileTrafficLimit(enabled, rate);
        } catch (RemoteException | ServiceSpecificException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean whiteListUidForMobileTraffic(int uid, boolean add) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            return this.mNetd.whiteListUidForMobileTraffic(uid, add);
        } catch (RemoteException | ServiceSpecificException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void setMiuiSlmBpfUid(int uid) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            this.mNetd.setMiuiSlmBpfUid(uid);
        } catch (RemoteException | ServiceSpecificException e) {
            e.printStackTrace();
        }
    }

    public long getMiuiSlmVoipUdpAddress(int uid) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            return this.mNetd.getMiuiSlmVoipUdpAddress(uid);
        } catch (RemoteException | ServiceSpecificException e) {
            e.printStackTrace();
            return 0L;
        }
    }

    public int getMiuiSlmVoipUdpPort(int uid) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            return this.mNetd.getMiuiSlmVoipUdpPort(uid);
        } catch (RemoteException | ServiceSpecificException e) {
            e.printStackTrace();
            return 0;
        }
    }

    private void getNmsService() {
        if (this.mNms == null) {
            this.mNms = INetworkManagementService.Stub.asInterface(ServiceManager.getService("network_management"));
        }
    }

    public void updateAurogonUidRule(int uid, boolean allow) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
    }

    public void closeSocketForAurogon(int[] uids) {
        try {
            if (this.mNms == null) {
                getNmsService();
            }
            if (this.mNms != null) {
                Slog.d(TAG, "call socket destroy!");
            }
        } catch (Exception e) {
            Slog.d(TAG, "failed to close socket for aurogon!");
        }
    }

    public int enableAutoForward(String addr, int fwmark, boolean enabled) {
        try {
            IOemNetd iOemNetd = this.mNetd;
            if (iOemNetd == null) {
                return -1;
            }
            return iOemNetd.enableAutoForward(addr, fwmark, enabled);
        } catch (RemoteException | ServiceSpecificException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public void dump(PrintWriter pw) {
        if (pw == null) {
            return;
        }
        pw.println("========================================MiuiFireRule DUMP BEGIN========================================");
        try {
            synchronized (this.mLock) {
                if (this.mCurrentNetworkState == -1) {
                    pw.println("Didn't cache the current network state!");
                } else {
                    pw.println("Current Network State: " + this.mCurrentNetworkState + ", it's setter uid:" + this.mStateSetterUid + " pid:" + this.mStateSetterPid);
                }
                SparseArray<Set<MiuiFireRule>> sparseArray = this.mMiuiFireRuleCache;
                if (sparseArray != null && sparseArray.size() != 0) {
                    for (int i = 0; i < this.mMiuiFireRuleCache.size(); i++) {
                        Set<MiuiFireRule> rules = this.mMiuiFireRuleCache.valueAt(i);
                        if (rules != null && !rules.isEmpty()) {
                            pw.println("Rule type: " + this.mMiuiFireRuleCache.keyAt(i));
                            for (MiuiFireRule rule : rules) {
                                pw.println("  Rule detail: " + rule.toString());
                            }
                        }
                    }
                }
                pw.println("Didn't cache the miui fire rule!");
            }
        } catch (Exception e) {
            Slog.e(TAG, "dump", e);
        }
        pw.println("========================================MiuiFireRule DUMP END========================================");
    }

    private void setMiuiFirewallRule(String packageName, int uid, int rule, int type, int callerUid, int callerPid) {
        synchronized (this.mLock) {
            if (this.mMiuiFireRuleCache == null) {
                this.mMiuiFireRuleCache = new SparseArray<>(4);
            }
            MiuiFireRule fireRule = new MiuiFireRule(packageName, uid, rule, callerUid, callerPid);
            Set<MiuiFireRule> typeRule = this.mMiuiFireRuleCache.get(type, new HashSet());
            if ((rule == 0 && type == 2) || (rule != 0 && type != 2)) {
                typeRule.add(fireRule);
            } else {
                typeRule.remove(fireRule);
            }
            this.mMiuiFireRuleCache.put(type, typeRule);
        }
    }

    private void setCurrentNetworkState(int state, int callerUid, int callerPid) {
        synchronized (this.mLock) {
            this.mCurrentNetworkState = state;
            this.mStateSetterUid = callerUid;
            this.mStateSetterPid = callerPid;
        }
    }

    /* loaded from: classes.dex */
    public static class MiuiFireRule {
        String pkgName;
        int rule;
        int setterPid;
        int setterUid;
        int uid;

        public MiuiFireRule(String pkgName, int uid, int rule, int setterUid, int setterPid) {
            this.pkgName = pkgName;
            this.uid = uid;
            this.rule = rule;
            this.setterUid = setterUid;
            this.setterPid = setterPid;
        }

        public String toString() {
            return "{pkgName='" + this.pkgName + "', uid=" + this.uid + ", rule=" + this.rule + ", setterUid=" + this.setterUid + ", setterPid=" + this.setterPid + '}';
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            MiuiFireRule fireRule = (MiuiFireRule) o;
            return Objects.equals(this.pkgName, fireRule.pkgName);
        }

        public int hashCode() {
            return Objects.hash(this.pkgName);
        }
    }
}
