package com.android.server.location.gnss.hal;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.ILocationListener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationRequest;
import android.os.Binder;
import android.os.IRemoteCallback;
import android.os.RemoteException;
import android.os.SystemClock;
import com.android.internal.util.FunctionalUtils;
import com.android.internal.util.Preconditions;
import com.android.server.location.gnss.GnssEventTrackingStub;
import com.android.server.wm.MiuiFreeformPinManagerService;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
/* loaded from: classes.dex */
public class Gpo4Client {
    private static final String TAG = "Gpo4Client";
    private static final long TIME_CHECK_NAV_APP = 3000;
    private static final long TIME_MAX_TRAFFIC_IGNORE = 300000;
    private static final int TIME_POST_DELAY_CHECK_NLP = 250;
    private static final int TIME_POST_TIMEOUT_OBTAIN_NLP = 2000;
    private static volatile Gpo4Client instance;
    private Context mContext;
    private LocationManager mLocationManager;
    private ScheduledThreadPoolExecutor scheduledThreadPoolExecutor;
    private final GpoUtil mGpoUtil = GpoUtil.getInstance();
    private Location mLastNlpLocation = null;
    private ScheduledFuture<?> futureCheckRequest = null;
    private ScheduledFuture<?> futureCheckNavigation1 = null;
    private ScheduledFuture<?> futureCheckNavigation2 = null;
    private final Map<Integer, LocationRequestRecorder> mGlpRequestMap = new ConcurrentHashMap();
    private final Map<Integer, LocationRequestRecorder> mNlpRequestMap = new ConcurrentHashMap();
    private final Map<Integer, String> mNavAppInfo = new ConcurrentHashMap();
    private final AtomicBoolean isFeatureEnabled = new AtomicBoolean(false);
    private final AtomicBoolean isScreenOn = new AtomicBoolean(true);
    private final AtomicLong mRequestNlpTime = new AtomicLong(0);
    private final AtomicLong mObtainNlpTime = new AtomicLong(0);
    private final AtomicLong mLastTrafficTime = new AtomicLong(0);
    private final AtomicLong mLastNlpTime = new AtomicLong(0);
    private final LocationListener mPassiveLocationListener = new LocationListener() { // from class: com.android.server.location.gnss.hal.Gpo4Client$$ExternalSyntheticLambda3
        @Override // android.location.LocationListener
        public final void onLocationChanged(Location location) {
            Gpo4Client.this.m1004lambda$new$2$comandroidserverlocationgnsshalGpo4Client(location);
        }
    };
    private final LocationListener mNetworkLocationListener = new LocationListener() { // from class: com.android.server.location.gnss.hal.Gpo4Client$$ExternalSyntheticLambda4
        @Override // android.location.LocationListener
        public final void onLocationChanged(Location location) {
            Gpo4Client.this.m1005lambda$new$3$comandroidserverlocationgnsshalGpo4Client(location);
        }
    };

    public static Gpo4Client getInstance() {
        if (instance == null) {
            synchronized (Gpo4Client.class) {
                if (instance == null) {
                    instance = new Gpo4Client();
                }
            }
        }
        return instance;
    }

    private Gpo4Client() {
    }

    public void init(Context context) {
        this.mGpoUtil.logv(TAG, "init");
        this.mContext = context;
        this.mLocationManager = (LocationManager) Preconditions.checkNotNull((LocationManager) context.getSystemService("location"));
        registerPassiveLocationUpdates();
        ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(1);
        this.scheduledThreadPoolExecutor = scheduledThreadPoolExecutor;
        scheduledThreadPoolExecutor.setRemoveOnCancelPolicy(true);
        this.scheduledThreadPoolExecutor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        this.scheduledThreadPoolExecutor.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
    }

    public void deinit() {
        this.mGpoUtil.logv(TAG, "deinit");
        unregisterPassiveLocationUpdates();
        this.scheduledThreadPoolExecutor.shutdown();
        this.scheduledThreadPoolExecutor = null;
    }

    public void disableGnssSwitch() {
        this.isFeatureEnabled.set(false);
    }

    public void updateScreenState(boolean on) {
        this.isScreenOn.set(on);
    }

    private void checkFeatureSwitch() {
        if (this.mGlpRequestMap.isEmpty()) {
            this.isFeatureEnabled.set(!this.mGpoUtil.checkHeavyUser() && this.mGpoUtil.isNetworkConnected() && this.isScreenOn.get() && (this.mLastTrafficTime.get() == 0 || SystemClock.elapsedRealtime() - this.mLastTrafficTime.get() >= TIME_MAX_TRAFFIC_IGNORE));
            this.mGpoUtil.logi(TAG, "isGpoFeatureEnabled ? " + this.isFeatureEnabled.get(), false);
        }
    }

    public void saveLocationRequestId(final int uid, String pkn, String provider, final int listenerHashCode, Object callbackType) {
        checkFeatureSwitch();
        if (!this.isFeatureEnabled.get()) {
            return;
        }
        if ("network".equalsIgnoreCase(provider) && !this.mNlpRequestMap.containsKey(Integer.valueOf(listenerHashCode))) {
            this.mGpoUtil.logi(TAG, "saveNLP: " + listenerHashCode, false);
            this.mNlpRequestMap.put(Integer.valueOf(listenerHashCode), new LocationRequestRecorder(uid, provider, listenerHashCode, callbackType, false, 0L));
        }
        if ("gps".equalsIgnoreCase(provider) && !this.mGlpRequestMap.containsKey(Integer.valueOf(listenerHashCode))) {
            this.mGpoUtil.logi(TAG, "saveGLP: " + listenerHashCode, false);
            this.mNavAppInfo.put(Integer.valueOf(uid), pkn);
            this.mGlpRequestMap.put(Integer.valueOf(listenerHashCode), new LocationRequestRecorder(uid, provider, listenerHashCode, callbackType, false, 0L));
            this.scheduledThreadPoolExecutor.schedule(new Runnable() { // from class: com.android.server.location.gnss.hal.Gpo4Client$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    Gpo4Client.this.m1008x104b3c5(listenerHashCode, uid);
                }
            }, 250L, TimeUnit.MILLISECONDS);
        }
    }

    /* renamed from: lambda$saveLocationRequestId$1$com-android-server-location-gnss-hal-Gpo4Client */
    public /* synthetic */ void m1008x104b3c5(int listenerHashCode, int uid) {
        if (this.mGlpRequestMap.containsKey(Integer.valueOf(listenerHashCode))) {
            boolean existNlp = false;
            Iterator<LocationRequestRecorder> it = this.mNlpRequestMap.values().iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                LocationRequestRecorder r = it.next();
                if (r.existUid(uid)) {
                    existNlp = true;
                    break;
                }
            }
            this.mGpoUtil.logi(TAG, "LocationRequest of " + uid + " existNlp ? " + existNlp, false);
            if (!existNlp && registerNetworkLocationUpdates()) {
                this.futureCheckRequest = this.scheduledThreadPoolExecutor.schedule(new Runnable() { // from class: com.android.server.location.gnss.hal.Gpo4Client$$ExternalSyntheticLambda6
                    @Override // java.lang.Runnable
                    public final void run() {
                        Gpo4Client.this.m1007x17b19c4();
                    }
                }, 2000L, TimeUnit.MILLISECONDS);
            }
        }
    }

    /* renamed from: lambda$saveLocationRequestId$0$com-android-server-location-gnss-hal-Gpo4Client */
    public /* synthetic */ void m1007x17b19c4() {
        if (this.mObtainNlpTime.get() <= this.mRequestNlpTime.get()) {
            unregisterNetworkLocationUpdates();
        }
    }

    /* renamed from: lambda$new$2$com-android-server-location-gnss-hal-Gpo4Client */
    public /* synthetic */ void m1004lambda$new$2$comandroidserverlocationgnsshalGpo4Client(Location location) {
        if (this.isFeatureEnabled.get() && "network".equals(location.getProvider()) && this.mGpoUtil.getEngineStatus() != 2 && location.getAccuracy() <= 150.0f) {
            Location gLocation = this.mGpoUtil.convertNlp2Glp(location);
            this.mLastNlpLocation = gLocation;
            this.mLastNlpTime.set(SystemClock.elapsedRealtime());
            for (Map.Entry<Integer, LocationRequestRecorder> entry : this.mGlpRequestMap.entrySet()) {
                if (!entry.getValue().getNlpReturned()) {
                    entry.getValue().setNlpReturned(true);
                    List<Location> locations = Arrays.asList(gLocation);
                    String logInfo = "use nlp " + gLocation.toString() + ", listenerHashCode is " + entry.getKey().toString() + ", callbackType is ILocationListener " + (entry.getValue().getCallbackType() instanceof ILocationListener);
                    this.mGpoUtil.logv(TAG, logInfo);
                    this.mGpoUtil.logEn(logInfo);
                    delieverLocation(entry.getValue().getCallbackType(), locations);
                    entry.getValue().setNlpReturnTime(SystemClock.elapsedRealtime());
                    this.futureCheckNavigation2 = checkAppUsage(TIME_POST_DELAY_CHECK_NLP);
                    ScheduledFuture<?> scheduledFuture = this.futureCheckRequest;
                    if (scheduledFuture != null) {
                        scheduledFuture.cancel(true);
                    }
                } else {
                    return;
                }
            }
        }
    }

    private void delieverLocation(Object callbackType, List<Location> locations) {
        if (callbackType instanceof ILocationListener) {
            try {
                ((ILocationListener) callbackType).onLocationChanged(locations, (IRemoteCallback) null);
                return;
            } catch (RemoteException e) {
                this.mGlpRequestMap.clear();
                this.mGpoUtil.loge(TAG, "onLocationChanged RemoteException");
                return;
            }
        }
        Intent intent = new Intent();
        intent.putExtra("location", locations.get(0));
        try {
            ((PendingIntent) callbackType).send(this.mContext, 0, intent);
        } catch (PendingIntent.CanceledException e2) {
            this.mGlpRequestMap.clear();
            this.mGpoUtil.loge(TAG, "PendingInent send CanceledException");
        }
    }

    private void registerPassiveLocationUpdates() {
        this.mLocationManager.requestLocationUpdates("passive", 1000L, MiuiFreeformPinManagerService.EDGE_AREA, this.mPassiveLocationListener, this.mContext.getMainLooper());
    }

    private void unregisterPassiveLocationUpdates() {
        this.mLocationManager.removeUpdates(this.mPassiveLocationListener);
    }

    /* renamed from: lambda$new$3$com-android-server-location-gnss-hal-Gpo4Client */
    public /* synthetic */ void m1005lambda$new$3$comandroidserverlocationgnsshalGpo4Client(Location location) {
        this.mObtainNlpTime.set(SystemClock.elapsedRealtime());
    }

    private boolean registerNetworkLocationUpdates() {
        if (!this.mGpoUtil.isNetworkConnected() || !this.mLocationManager.isProviderEnabled("network")) {
            return false;
        }
        if (this.mLastNlpLocation != null && SystemClock.elapsedRealtime() - this.mLastNlpTime.get() <= 2000) {
            for (Map.Entry<Integer, LocationRequestRecorder> entry : this.mGlpRequestMap.entrySet()) {
                if (entry.getValue().getNlpReturned()) {
                    break;
                }
                entry.getValue().setNlpReturned(true);
                List<Location> locations = Arrays.asList(this.mLastNlpLocation);
                String logInfo = "use cached nlp " + this.mLastNlpLocation.toString() + ", listenerHashCode is " + entry.getKey().toString() + ", callbackType is ILocationListener " + (entry.getValue().getCallbackType() instanceof ILocationListener);
                this.mGpoUtil.logv(TAG, logInfo);
                this.mGpoUtil.logEn(logInfo);
                delieverLocation(entry.getValue().getCallbackType(), locations);
                entry.getValue().setNlpReturnTime(SystemClock.elapsedRealtime());
                this.futureCheckNavigation2 = checkAppUsage(TIME_POST_DELAY_CHECK_NLP);
                ScheduledFuture<?> scheduledFuture = this.futureCheckRequest;
                if (scheduledFuture != null) {
                    scheduledFuture.cancel(true);
                }
            }
            return false;
        }
        return ((Boolean) Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.location.gnss.hal.Gpo4Client$$ExternalSyntheticLambda5
            public final Object getOrThrow() {
                return Gpo4Client.this.m1006xc9fc5654();
            }
        })).booleanValue();
    }

    /* renamed from: lambda$registerNetworkLocationUpdates$4$com-android-server-location-gnss-hal-Gpo4Client */
    public /* synthetic */ Boolean m1006xc9fc5654() throws Exception {
        this.mGpoUtil.logv(TAG, "request NLP.");
        this.mRequestNlpTime.set(SystemClock.elapsedRealtime());
        this.mLocationManager.requestLocationUpdates(LocationRequest.createFromDeprecatedProvider("network", 1000L, 1000.0f, true), this.mNetworkLocationListener, this.mContext.getMainLooper());
        return true;
    }

    private void unregisterNetworkLocationUpdates() {
        Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.location.gnss.hal.Gpo4Client$$ExternalSyntheticLambda2
            public final void runOrThrow() {
                Gpo4Client.this.m1009xfc2289ae();
            }
        });
    }

    /* renamed from: lambda$unregisterNetworkLocationUpdates$5$com-android-server-location-gnss-hal-Gpo4Client */
    public /* synthetic */ void m1009xfc2289ae() throws Exception {
        this.mLocationManager.removeUpdates(this.mNetworkLocationListener);
    }

    public void removeLocationRequestId(int listenerHashCode) {
        if (this.mNlpRequestMap.containsKey(Integer.valueOf(listenerHashCode))) {
            this.mGpoUtil.logi(TAG, "removeNLP: " + listenerHashCode, false);
            this.mNlpRequestMap.remove(Integer.valueOf(listenerHashCode));
        }
        if (this.mGlpRequestMap.containsKey(Integer.valueOf(listenerHashCode))) {
            this.mGpoUtil.logi(TAG, "removeGLP: " + listenerHashCode, false);
            LocationRequestRecorder recorder = this.mGlpRequestMap.get(Integer.valueOf(listenerHashCode));
            this.mGlpRequestMap.remove(Integer.valueOf(listenerHashCode));
            if (recorder != null) {
                int uid = recorder.getUid();
                long time = SystemClock.elapsedRealtime() - recorder.getNlpReturnTime();
                GnssEventTrackingStub.getInstance().recordNavAppTime(this.mNavAppInfo.get(Integer.valueOf(uid)), time >= 3000 ? time : 0L);
                this.mNavAppInfo.remove(Integer.valueOf(uid));
                return;
            }
            this.mNavAppInfo.clear();
        }
    }

    public boolean blockEngineStart() {
        boolean res = this.isFeatureEnabled.get() && this.mGpoUtil.getEngineStatus() != 2;
        this.mGpoUtil.logi(TAG, "blockEngineStart ? " + res, false);
        this.futureCheckNavigation1 = checkAppUsage(TIME_POST_TIMEOUT_OBTAIN_NLP);
        return res;
    }

    private ScheduledFuture<?> checkAppUsage(int timeout) {
        try {
            this.mGpoUtil.logv(TAG, "checkAppUsage");
            return this.scheduledThreadPoolExecutor.schedule(new Runnable() { // from class: com.android.server.location.gnss.hal.Gpo4Client$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    Gpo4Client.this.m1003x277d9e1a();
                }
            }, timeout, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /* renamed from: lambda$checkAppUsage$6$com-android-server-location-gnss-hal-Gpo4Client */
    public /* synthetic */ void m1003x277d9e1a() {
        if (this.mGpoUtil.getEngineStatus() == 1) {
            this.mGpoUtil.logv(TAG, "navigating app, restart gnss engine");
            this.mGpoUtil.doStartEngineByInstance();
        }
    }

    public void clearLocationRequest() {
        this.mGpoUtil.logv(TAG, "clearLocationRequest");
        ScheduledFuture<?> scheduledFuture = this.futureCheckNavigation1;
        if (scheduledFuture != null) {
            scheduledFuture.cancel(true);
        }
        ScheduledFuture<?> scheduledFuture2 = this.futureCheckNavigation2;
        if (scheduledFuture2 != null) {
            scheduledFuture2.cancel(true);
        }
        ScheduledFuture<?> scheduledFuture3 = this.futureCheckRequest;
        if (scheduledFuture3 != null) {
            scheduledFuture3.cancel(true);
        }
    }

    public void reportLocation2Gpo(Location location) {
        if (location != null && location.isComplete() && location.getSpeed() >= 3.0f) {
            this.mLastTrafficTime.set(SystemClock.elapsedRealtime());
        }
    }

    /* loaded from: classes.dex */
    public class LocationRequestRecorder {
        private Object callbackType;
        private int listenerHashCode;
        private long nlpReturnTime;
        private boolean nlpReturned;
        private String provider;
        private int uid;

        public LocationRequestRecorder(int uid, String provider, int listenerHashCode, Object callbackType, boolean nlpReturned, long nlpReturnTime) {
            Gpo4Client.this = r1;
            this.uid = uid;
            this.provider = provider;
            this.listenerHashCode = listenerHashCode;
            this.callbackType = callbackType;
            this.nlpReturned = nlpReturned;
            this.nlpReturnTime = nlpReturnTime;
        }

        public boolean existUid(int value) {
            return this.uid == value;
        }

        public int getUid() {
            return this.uid;
        }

        public String getProvider() {
            return this.provider;
        }

        public boolean existListenerHashCode(int value) {
            return this.listenerHashCode == value;
        }

        public Object getCallbackType() {
            return this.callbackType;
        }

        public void setNlpReturned(boolean value) {
            this.nlpReturned = value;
        }

        public boolean getNlpReturned() {
            return this.nlpReturned;
        }

        public void setNlpReturnTime(long value) {
            this.nlpReturnTime = value;
        }

        public long getNlpReturnTime() {
            return this.nlpReturnTime;
        }
    }
}
