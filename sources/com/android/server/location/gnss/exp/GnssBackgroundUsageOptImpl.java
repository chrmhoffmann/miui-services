package com.android.server.location.gnss.exp;

import android.location.ILocationListener;
import android.location.LocationRequest;
import android.location.util.identity.CallerIdentity;
import android.os.Build;
import android.os.SystemProperties;
import android.util.Log;
import com.android.server.location.gnss.GnssEventTrackingStub;
import com.android.server.location.gnss.exp.GnssBackgroundUsageOptStub;
import com.miui.base.MiuiStubRegistry;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
/* loaded from: classes.dex */
public class GnssBackgroundUsageOptImpl implements GnssBackgroundUsageOptStub {
    private static final int BACKGROUND_USAGE_OPT_TIME = 10000;
    private static final String TAG = "GnssBackgroundUsageOpt";
    private static boolean mIsSpecifiedDevice;
    private static final HashSet<String> sDevices;
    private GnssBackgroundUsageOptStub.IRemoveRequest mIRemoveRequest;
    private GnssBackgroundUsageOptStub.IRestoreRequest mIRestoreRequest;
    private boolean mOldForeground;
    private int mOldUid;
    private final boolean D = SystemProperties.getBoolean("persist.sys.gnss_dc.test", false);
    private final Map<Integer, GnssRequsetBean> mRequestMap = new ConcurrentHashMap();
    private final Map<Integer, Thread> mRemoveThreadMap = new ConcurrentHashMap();

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<GnssBackgroundUsageOptImpl> {

        /* compiled from: GnssBackgroundUsageOptImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final GnssBackgroundUsageOptImpl INSTANCE = new GnssBackgroundUsageOptImpl();
        }

        public GnssBackgroundUsageOptImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public GnssBackgroundUsageOptImpl provideNewInstance() {
            return new GnssBackgroundUsageOptImpl();
        }
    }

    static {
        HashSet<String> hashSet = new HashSet<>();
        sDevices = hashSet;
        hashSet.add("ingres");
    }

    GnssBackgroundUsageOptImpl() {
        mIsSpecifiedDevice = sDevices.contains(Build.DEVICE.toLowerCase());
        Log.d(TAG, "Is Specified Device:" + mIsSpecifiedDevice);
    }

    public void request(String provider, CallerIdentity identity, LocationRequest locationRequest, boolean foreground, int permissionLevel, boolean hasLocationPermissions, ILocationListener listener) {
        if (mIsSpecifiedDevice && hasLocationPermissions && "gps".equals(provider)) {
            GnssRequsetBean requsetBean = new GnssRequsetBean();
            requsetBean.identity = identity;
            requsetBean.listener = listener;
            requsetBean.provider = provider;
            requsetBean.removeByOpt = false;
            requsetBean.locationRequest = locationRequest;
            requsetBean.permissionLevel = permissionLevel;
            this.mRequestMap.put(Integer.valueOf(identity.getUid()), requsetBean);
            if (this.D) {
                Log.d(TAG, "request uid:" + identity.getUid() + " pkg:" + identity.getPackageName());
            }
            if (!foreground) {
                remove(identity.getUid());
            }
        }
    }

    public void onAppForegroundChanged(int uid, boolean foreground) {
        GnssRequsetBean bean = this.mRequestMap.get(Integer.valueOf(uid));
        if (bean == null) {
            return;
        }
        if (this.mOldUid == uid && this.mOldForeground == foreground) {
            if (this.D) {
                Log.d(TAG, "uid:" + uid + " foreground do not changed...");
                return;
            }
            return;
        }
        Thread removeThread = this.mRemoveThreadMap.get(Integer.valueOf(uid));
        if (!foreground) {
            remove(uid);
        } else if (bean.removeByOpt) {
            Log.d(TAG, "change to foreground remove by opt and now restore...");
            restore(bean.locationRequest, bean.identity, bean.permissionLevel, bean.listener);
        } else if (removeThread != null) {
            removeThread.interrupt();
            Log.d(TAG, "remove Thread not null, interrupt it...");
        }
        this.mOldUid = uid;
        this.mOldForeground = foreground;
    }

    public void remove(String provider, int uid, String pkgName) {
        GnssRequsetBean bean = this.mRequestMap.get(Integer.valueOf(uid));
        if (pkgName != null && bean != null && pkgName.equals(bean.identity.getPackageName()) && !bean.removeByOpt) {
            this.mRequestMap.remove(Integer.valueOf(uid));
            if (this.D) {
                Log.d(TAG, "pkgName:" + pkgName + " provider:" + provider + " remove by user... and now mRequestMap:" + this.mRequestMap);
            }
        }
    }

    public void registerRequestCallback(GnssBackgroundUsageOptStub.IRemoveRequest iRemoveRequest) {
        this.mIRemoveRequest = iRemoveRequest;
    }

    public void registerRestoreCallback(GnssBackgroundUsageOptStub.IRestoreRequest iRestoreRequest) {
        this.mIRestoreRequest = iRestoreRequest;
    }

    private void remove(final int uid) {
        if (this.mRemoveThreadMap.get(Integer.valueOf(uid)) == null) {
            Thread thread = new Thread(new Runnable() { // from class: com.android.server.location.gnss.exp.GnssBackgroundUsageOptImpl$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    GnssBackgroundUsageOptImpl.this.m997x953cb14e(uid);
                }
            });
            thread.start();
            this.mRemoveThreadMap.put(Integer.valueOf(uid), thread);
        }
    }

    /* renamed from: lambda$remove$0$com-android-server-location-gnss-exp-GnssBackgroundUsageOptImpl */
    public /* synthetic */ void m997x953cb14e(int uid) {
        StringBuilder sb;
        GnssRequsetBean bean = this.mRequestMap.get(Integer.valueOf(uid));
        if (bean != null) {
            try {
                Thread.sleep(10000L);
                try {
                    try {
                        this.mIRemoveRequest.onRemoveListener(bean.listener);
                        bean.removeByOpt = true;
                        this.mRequestMap.put(Integer.valueOf(uid), bean);
                        sb = new StringBuilder();
                    } catch (Exception e) {
                        Log.e(TAG, "remove by opt, exception-->" + e);
                        bean.removeByOpt = true;
                        this.mRequestMap.put(Integer.valueOf(uid), bean);
                        sb = new StringBuilder();
                    }
                    Log.d(TAG, sb.append("remove by opt, uid:").append(uid).toString());
                    GnssEventTrackingStub.getInstance().recordGnssBackgroundOptTime();
                } catch (Throwable th) {
                    bean.removeByOpt = true;
                    this.mRequestMap.put(Integer.valueOf(uid), bean);
                    Log.d(TAG, "remove by opt, uid:" + uid);
                    GnssEventTrackingStub.getInstance().recordGnssBackgroundOptTime();
                    throw th;
                }
            } catch (InterruptedException e2) {
                Log.e(TAG, "current remove thread has been interrupted...");
                this.mRemoveThreadMap.remove(Integer.valueOf(uid));
                return;
            }
        } else if (this.D) {
            Log.d(TAG, "remove by opt interrupt, uid:" + uid);
        }
        this.mRemoveThreadMap.remove(Integer.valueOf(uid));
    }

    private void restore(LocationRequest request, CallerIdentity identity, int permissionLevel, ILocationListener listener) {
        try {
            this.mIRestoreRequest.onRestore(request, identity, permissionLevel, listener);
            Log.d(TAG, "restore by opt,uid:" + identity.getUid() + " pkg:" + identity.getPackageName());
        } catch (Exception e) {
            Log.e(TAG, "restore exception-->" + e);
        }
    }
}
