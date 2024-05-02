package com.android.server.location.gnss.hal;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
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
import com.android.server.location.LocationDumpLogStub;
import com.android.server.location.gnss.GnssEventTrackingStub;
import com.android.server.wm.MiuiFreeformPinManagerService;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
/* loaded from: classes.dex */
public class Gpo5Client {
    private static final long MAX_RECOED_MOVING_TIME_SIZE = 300000;
    private static final int MAX_RECORD_STEP_TIME_SIZE = 100;
    private static final float MAX_STEPS = 5.0f;
    private static final float MAX_TRAFFIC_SPEED = 3.0f;
    private static final long MIN_RECOED_MOVING_TIME_SIZE = 10000;
    private static final int SENSOR_SENSITIVE = 0;
    private static final int SENSOR_TYPE_OEM_USER_BEHAVIOR = 33171097;
    private static final String TAG = "Gpo5Client";
    private static final long TIME_CHECK_NAV_APP = 3000;
    private static final int TIME_POST_DELAY_CHECK_NLP = 250;
    private static final int TIME_POST_TIMEOUT_OBTAIN_NLP = 2000;
    private static final float USER_BEHAVIOR_TRAFFIC = 4.0f;
    private static volatile Gpo5Client instance;
    private Context mContext;
    private LocationManager mLocationManager;
    private SensorManager mSensorManager;
    private Sensor mUserBehaviorDetector;
    private Sensor mUserStepsDetector;
    private ScheduledThreadPoolExecutor scheduledThreadPoolExecutor;
    private Location mFineLocation = null;
    private boolean isUserBehaviorSensorRegistered = false;
    private boolean mSensorSupported = false;
    private boolean isClientAlive = true;
    private Location mLastNlpLocation = null;
    private ScheduledFuture<?> futureCheckRequest = null;
    private ScheduledFuture<?> futureCheckNavigation1 = null;
    private ScheduledFuture<?> futureCheckNavigation2 = null;
    private ScheduledFuture<?> futureReturnFineLocation = null;
    private final Map<Integer, LocationRequestRecorder> mGlpRequestMap = new ConcurrentHashMap();
    private final Map<Integer, LocationRequestRecorder> mNlpRequestMap = new ConcurrentHashMap();
    private final Map<Integer, String> mNavAppInfo = new ConcurrentHashMap();
    private final Map<Integer, Object> mCacheLocationCallbackMap = new ConcurrentHashMap();
    private final GpoUtil mGpoUtil = GpoUtil.getInstance();
    private final AtomicBoolean isFeatureEnabled = new AtomicBoolean(false);
    private final AtomicBoolean isScreenOn = new AtomicBoolean(true);
    private final AtomicLong mRequestNlpTime = new AtomicLong(0);
    private final AtomicLong mObtainNlpTime = new AtomicLong(0);
    private final AtomicLong mLastNlpTime = new AtomicLong(0);
    private final AtomicLong mLastGnssTrafficTime = new AtomicLong(0);
    private final AtomicLong mLastSensorTrafficTime = new AtomicLong(0);
    private final AtomicInteger mCurrentSteps = new AtomicInteger(0);
    private final AtomicInteger mInitialSteps = new AtomicInteger(0);
    private final BlockingDeque<Long> mStepTimeRecorder = new LinkedBlockingDeque(100);
    private final AtomicBoolean isInsideSmallArea = new AtomicBoolean(false);
    private final AtomicBoolean mStartEngineWhileScreenLocked = new AtomicBoolean(false);
    private final SensorEventListener mUserBehaviorListener = new SensorEventListener() { // from class: com.android.server.location.gnss.hal.Gpo5Client.1
        @Override // android.hardware.SensorEventListener
        public void onSensorChanged(SensorEvent event) {
            if (event.values[0] == Gpo5Client.USER_BEHAVIOR_TRAFFIC) {
                Gpo5Client.this.mGpoUtil.logv(Gpo5Client.TAG, "Sensor report user behavior: " + event.values[0]);
                Gpo5Client.this.mLastSensorTrafficTime.set(SystemClock.elapsedRealtime());
                if (Gpo5Client.this.mGpoUtil.getEngineStatus() == 4) {
                    return;
                }
                Gpo5Client.this.mGpoUtil.logi(Gpo5Client.TAG, "User is on Traffic.", true);
                Gpo5Client.this.handleLeaveSmallArea();
            }
        }

        @Override // android.hardware.SensorEventListener
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };
    private final SensorEventListener mUserStepsListener = new SensorEventListener() { // from class: com.android.server.location.gnss.hal.Gpo5Client.2
        @Override // android.hardware.SensorEventListener
        public void onSensorChanged(SensorEvent event) {
            Gpo5Client.this.mCurrentSteps.set((int) event.values[0]);
            if (Gpo5Client.this.isUserBehaviorSensorRegistered && Gpo5Client.this.mCurrentSteps.get() - Gpo5Client.this.mInitialSteps.get() >= Gpo5Client.MAX_STEPS) {
                Gpo5Client.this.mGpoUtil.logi(Gpo5Client.TAG, "User moves outside SmallArea.", true);
                Gpo5Client.this.handleLeaveSmallArea();
            }
            try {
                if (Gpo5Client.this.mStepTimeRecorder.size() >= 100) {
                    Gpo5Client.this.mStepTimeRecorder.takeLast();
                }
                Gpo5Client.this.mStepTimeRecorder.putFirst(Long.valueOf(SystemClock.elapsedRealtime()));
            } catch (InterruptedException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }
        }

        @Override // android.hardware.SensorEventListener
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };
    private final LocationListener mPassiveLocationListener = new LocationListener() { // from class: com.android.server.location.gnss.hal.Gpo5Client$$ExternalSyntheticLambda0
        @Override // android.location.LocationListener
        public final void onLocationChanged(Location location) {
            Gpo5Client.this.m1018lambda$new$3$comandroidserverlocationgnsshalGpo5Client(location);
        }
    };
    private final LocationListener mNetworkLocationListener = new LocationListener() { // from class: com.android.server.location.gnss.hal.Gpo5Client$$ExternalSyntheticLambda1
        @Override // android.location.LocationListener
        public final void onLocationChanged(Location location) {
            Gpo5Client.this.m1019lambda$new$4$comandroidserverlocationgnsshalGpo5Client(location);
        }
    };

    public static Gpo5Client getInstance() {
        if (instance == null) {
            synchronized (Gpo5Client.class) {
                if (instance == null) {
                    instance = new Gpo5Client();
                }
            }
        }
        return instance;
    }

    private Gpo5Client() {
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
        registerUserStepsListener();
    }

    public boolean checkSensorSupport() {
        SensorManager sensorManager = (SensorManager) Preconditions.checkNotNull((SensorManager) this.mContext.getSystemService("sensor"));
        this.mSensorManager = sensorManager;
        this.mUserBehaviorDetector = sensorManager.getDefaultSensor(SENSOR_TYPE_OEM_USER_BEHAVIOR, false);
        Sensor defaultSensor = this.mSensorManager.getDefaultSensor(19, false);
        this.mUserStepsDetector = defaultSensor;
        if (this.mUserBehaviorDetector != null && defaultSensor != null) {
            this.mSensorSupported = true;
            this.mGpoUtil.logi(TAG, "This Device Support Sensor id 33171097 ? " + this.mSensorSupported, true);
            return this.mSensorSupported;
        }
        return this.mSensorSupported;
    }

    public void deinit() {
        this.mGpoUtil.logv(TAG, "deinit");
        this.isClientAlive = false;
        unregisterPassiveLocationUpdates();
        this.scheduledThreadPoolExecutor.shutdown();
        this.scheduledThreadPoolExecutor = null;
        GnssScoringModelStub.getInstance().init(false);
        unregisterUserStepsListener();
        unregisterUserBehaviorListener();
    }

    public void handleLeaveSmallArea() {
        if (!this.isClientAlive) {
            return;
        }
        this.mFineLocation = null;
        GnssScoringModelStub.getInstance().startScoringModel(true);
        if (this.mGpoUtil.getEngineStatus() == 1 || this.mGpoUtil.getEngineStatus() == 3) {
            this.mGpoUtil.doStartEngineByInstance();
            LocationDumpLogStub.getInstance().setRecordLoseLocation(false);
        }
    }

    public void disableGnssSwitch() {
        this.isFeatureEnabled.set(false);
    }

    private void registerUserBehaviorListener() {
        if (!this.mSensorSupported || !this.isClientAlive) {
            return;
        }
        if (this.mStartEngineWhileScreenLocked.get() || this.mGpoUtil.getEngineStatus() == 2) {
            this.mGpoUtil.logv(TAG, "gnss engine has been started, do not control.");
            this.mStartEngineWhileScreenLocked.set(false);
            this.isInsideSmallArea.set(false);
            return;
        }
        this.mGpoUtil.logv(TAG, "register User Behavior Listener");
        this.mSensorManager.registerListener(this.mUserBehaviorListener, this.mUserBehaviorDetector, 0);
        this.isUserBehaviorSensorRegistered = true;
        this.mInitialSteps.set(this.mCurrentSteps.get());
        this.mGpoUtil.logv(TAG, "initial steps is " + this.mInitialSteps.get());
        this.isInsideSmallArea.set(true);
    }

    private void unregisterUserBehaviorListener() {
        if (this.isUserBehaviorSensorRegistered) {
            this.mGpoUtil.logv(TAG, "unregister User Behavior Listener");
            this.mSensorManager.unregisterListener(this.mUserBehaviorListener);
            this.isUserBehaviorSensorRegistered = false;
            this.isInsideSmallArea.set(false);
            this.mStartEngineWhileScreenLocked.set(false);
        }
    }

    private void registerUserStepsListener() {
        if (!this.mSensorSupported) {
            return;
        }
        this.mGpoUtil.logv(TAG, "register User Steps Listener");
        this.mSensorManager.registerListener(this.mUserStepsListener, this.mUserStepsDetector, 0);
    }

    private void unregisterUserStepsListener() {
        if (!this.mSensorSupported) {
            return;
        }
        this.mGpoUtil.logv(TAG, "unregister User Steps Listener");
        this.mSensorManager.unregisterListener(this.mUserStepsListener);
    }

    private void returnFineLocation2App() {
        try {
            this.futureReturnFineLocation = this.scheduledThreadPoolExecutor.scheduleAtFixedRate(new Runnable() { // from class: com.android.server.location.gnss.hal.Gpo5Client$$ExternalSyntheticLambda5
                @Override // java.lang.Runnable
                public final void run() {
                    Gpo5Client.this.m1021x7d0e65b5();
                }
            }, 0L, 1000L, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* renamed from: lambda$returnFineLocation2App$0$com-android-server-location-gnss-hal-Gpo5Client */
    public /* synthetic */ void m1021x7d0e65b5() {
        if (this.mFineLocation == null) {
            this.mGpoUtil.logv(TAG, "cancel scheduleAtFixedRate");
            this.futureReturnFineLocation.cancel(true);
        } else if (!this.isUserBehaviorSensorRegistered) {
            this.mGpoUtil.logv(TAG, "cancel scheduleAtFixedRate");
            this.futureReturnFineLocation.cancel(true);
        } else {
            for (Map.Entry<Integer, Object> entry : this.mCacheLocationCallbackMap.entrySet()) {
                this.mGpoUtil.logv(TAG, "return fine location to navigation app");
                delieverLocation(entry.getValue(), Arrays.asList(this.mFineLocation));
            }
        }
    }

    public void updateScreenState(boolean on) {
        this.isScreenOn.set(on);
        if (!on) {
            unregisterUserBehaviorListener();
        } else {
            registerUserBehaviorListener();
        }
    }

    private void checkFeatureSwitch() {
        if (this.mGlpRequestMap.isEmpty()) {
            this.isFeatureEnabled.set(!this.mGpoUtil.checkHeavyUser() && this.mGpoUtil.isNetworkConnected() && this.isScreenOn.get() && !isUserMovingRecently());
            this.mGpoUtil.logi(TAG, "isGpoFeatureEnabled ? " + this.isFeatureEnabled.get(), false);
        }
    }

    private boolean isUserMovingRecently() {
        long curTime = SystemClock.elapsedRealtime();
        boolean walkRecently = this.mStepTimeRecorder.size() >= 100 && curTime - this.mStepTimeRecorder.getFirst().longValue() <= MAX_RECOED_MOVING_TIME_SIZE && this.mStepTimeRecorder.getFirst().longValue() - this.mStepTimeRecorder.getLast().longValue() <= MAX_RECOED_MOVING_TIME_SIZE;
        boolean trafficRecently = (this.mLastGnssTrafficTime.get() != 0 && curTime - this.mLastGnssTrafficTime.get() <= MAX_RECOED_MOVING_TIME_SIZE) || (this.mLastSensorTrafficTime.get() != 0 && curTime - this.mLastSensorTrafficTime.get() <= 10000);
        this.mGpoUtil.logv(TAG, "CurrentTime is " + curTime + ", mLastGnssTrafficTime is " + this.mLastGnssTrafficTime.get() + ", mLastSensorTrafficTime is " + this.mLastSensorTrafficTime.get());
        this.mGpoUtil.logi(TAG, "Recently user walk ? " + walkRecently + ", traffic ? " + trafficRecently, true);
        return walkRecently || trafficRecently;
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
            this.scheduledThreadPoolExecutor.schedule(new Runnable() { // from class: com.android.server.location.gnss.hal.Gpo5Client$$ExternalSyntheticLambda3
                @Override // java.lang.Runnable
                public final void run() {
                    Gpo5Client.this.m1023x35748907(listenerHashCode, uid);
                }
            }, 250L, TimeUnit.MILLISECONDS);
        }
    }

    /* renamed from: lambda$saveLocationRequestId$2$com-android-server-location-gnss-hal-Gpo5Client */
    public /* synthetic */ void m1023x35748907(int listenerHashCode, int uid) {
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
                this.futureCheckRequest = this.scheduledThreadPoolExecutor.schedule(new Runnable() { // from class: com.android.server.location.gnss.hal.Gpo5Client$$ExternalSyntheticLambda2
                    @Override // java.lang.Runnable
                    public final void run() {
                        Gpo5Client.this.m1022x35eaef06();
                    }
                }, 2000L, TimeUnit.MILLISECONDS);
            }
        }
    }

    /* renamed from: lambda$saveLocationRequestId$1$com-android-server-location-gnss-hal-Gpo5Client */
    public /* synthetic */ void m1022x35eaef06() {
        if (this.mObtainNlpTime.get() <= this.mRequestNlpTime.get()) {
            unregisterNetworkLocationUpdates();
        }
    }

    /* renamed from: lambda$new$3$com-android-server-location-gnss-hal-Gpo5Client */
    public /* synthetic */ void m1018lambda$new$3$comandroidserverlocationgnsshalGpo5Client(Location location) {
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
        LocationManager locationManager = this.mLocationManager;
        if (locationManager != null) {
            locationManager.requestLocationUpdates("passive", 1000L, MiuiFreeformPinManagerService.EDGE_AREA, this.mPassiveLocationListener, this.mContext.getMainLooper());
        }
    }

    private void unregisterPassiveLocationUpdates() {
        LocationManager locationManager = this.mLocationManager;
        if (locationManager != null) {
            locationManager.removeUpdates(this.mPassiveLocationListener);
        }
    }

    /* renamed from: lambda$new$4$com-android-server-location-gnss-hal-Gpo5Client */
    public /* synthetic */ void m1019lambda$new$4$comandroidserverlocationgnsshalGpo5Client(Location location) {
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
        return ((Boolean) Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.location.gnss.hal.Gpo5Client$$ExternalSyntheticLambda7
            public final Object getOrThrow() {
                return Gpo5Client.this.m1020xfe6c2b96();
            }
        })).booleanValue();
    }

    /* renamed from: lambda$registerNetworkLocationUpdates$5$com-android-server-location-gnss-hal-Gpo5Client */
    public /* synthetic */ Boolean m1020xfe6c2b96() throws Exception {
        this.mGpoUtil.logv(TAG, "request NLP.");
        this.mRequestNlpTime.set(SystemClock.elapsedRealtime());
        this.mLocationManager.requestLocationUpdates(LocationRequest.createFromDeprecatedProvider("network", 1000L, 1000.0f, true), this.mNetworkLocationListener, this.mContext.getMainLooper());
        return true;
    }

    private void unregisterNetworkLocationUpdates() {
        Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.location.gnss.hal.Gpo5Client$$ExternalSyntheticLambda6
            public final void runOrThrow() {
                Gpo5Client.this.m1024x30925ef0();
            }
        });
    }

    /* renamed from: lambda$unregisterNetworkLocationUpdates$6$com-android-server-location-gnss-hal-Gpo5Client */
    public /* synthetic */ void m1024x30925ef0() throws Exception {
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
            return this.scheduledThreadPoolExecutor.schedule(new Runnable() { // from class: com.android.server.location.gnss.hal.Gpo5Client$$ExternalSyntheticLambda4
                @Override // java.lang.Runnable
                public final void run() {
                    Gpo5Client.this.m1017x5bed735c();
                }
            }, timeout, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /* renamed from: lambda$checkAppUsage$7$com-android-server-location-gnss-hal-Gpo5Client */
    public /* synthetic */ void m1017x5bed735c() {
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
        long time = SystemClock.elapsedRealtime();
        if (location != null && location.isComplete() && location.getSpeed() >= MAX_TRAFFIC_SPEED) {
            this.mLastGnssTrafficTime.set(time);
        }
        GnssScoringModelStub.getInstance().updateFixTime(time);
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
            Gpo5Client.this = r1;
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
