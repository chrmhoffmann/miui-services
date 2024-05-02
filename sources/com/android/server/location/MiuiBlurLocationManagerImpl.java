package com.android.server.location;

import android.app.AppOpsManager;
import android.content.Context;
import android.location.LastLocationRequest;
import android.location.Location;
import android.location.LocationResult;
import android.location.util.identity.CallerIdentity;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.UserHandle;
import android.telephony.CellIdentity;
import android.telephony.CellIdentityCdma;
import android.telephony.CellIdentityGsm;
import android.telephony.CellIdentityLte;
import android.telephony.CellIdentityNr;
import android.telephony.CellIdentityWcdma;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoNr;
import android.telephony.CellInfoWcdma;
import android.telephony.CellSignalStrengthNr;
import android.util.Log;
import com.android.internal.app.ILocationBlurry;
import com.android.internal.os.BackgroundThread;
import com.android.server.am.ActivityManagerServiceImpl;
import com.android.server.location.provider.LocationProviderManager;
import com.miui.base.MiuiStubRegistry;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import miui.os.Build;
import miui.security.SvStatusData;
/* loaded from: classes.dex */
public class MiuiBlurLocationManagerImpl extends MiuiBlurLocationManagerStub {
    private static final boolean DEBUG = false;
    private static final String KEY_CALLBACK = "key_callback";
    private static final String KEY_CID = "key_cid";
    private static final String KEY_LAC = "key_lac";
    private static final String KEY_LAST_BLUR_LATITUDE = "key_last_blur_latitude";
    private static final String KEY_LAST_BLUR_LONGITUDE = "key_last_blur_longitude";
    private static final String KEY_LATITUDE = "key_latitude";
    private static final String KEY_LONGITUDE = "key_longitude";
    private static final String KEY_MCC = "key_mcc";
    private static final String KEY_MNC = "key_mnc";
    private static final int MSG_BLUR_LOCATION = 171202;
    private static final int MSG_UPDATE_CELL_INFO = 1;
    private static final int MSG_UPDATE_SATELLITE = 2;
    private static final int MSG_UPDATE_SATELLITE_SILENT = 3;
    private static final String TAG = "MiuiBlurLocationManager";
    private AppOpsManager mAppOps;
    private volatile ILocationBlurry mBlurLocationManager;
    private final Messenger mBlurMessenger;
    private Context mContext;
    private final Handler mHandler;
    private volatile Location mLastBlurLocation;
    private LocationManagerService mLocationService;
    private SvStatusData mSvStatusData;
    private volatile BaseCellLocation mStashBlurLocationInfo = new BaseCellLocation();
    private final Handler mBlurGpsHandler = new Handler(BackgroundThread.get().getLooper()) { // from class: com.android.server.location.MiuiBlurLocationManagerImpl.2
        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            if (msg.obj instanceof CallerIdentity) {
                MiuiBlurLocationManagerImpl.this.handleGpsLocationChangedLocked("gps", (CallerIdentity) msg.obj);
            }
        }
    };

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<MiuiBlurLocationManagerImpl> {

        /* compiled from: MiuiBlurLocationManagerImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final MiuiBlurLocationManagerImpl INSTANCE = new MiuiBlurLocationManagerImpl();
        }

        public MiuiBlurLocationManagerImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public MiuiBlurLocationManagerImpl provideNewInstance() {
            return new MiuiBlurLocationManagerImpl();
        }
    }

    public MiuiBlurLocationManagerImpl() {
        Handler handler = new Handler(BackgroundThread.get().getLooper()) { // from class: com.android.server.location.MiuiBlurLocationManagerImpl.1
            @Override // android.os.Handler
            public void handleMessage(Message msg) {
                Bundle bundle = msg.getData();
                if (bundle == null) {
                    Log.e(MiuiBlurLocationManagerImpl.TAG, "msg from client is null");
                    return;
                }
                switch (msg.what) {
                    case 1:
                        int lac = bundle.getInt(MiuiBlurLocationManagerImpl.KEY_LAC, 0);
                        int cid = bundle.getInt(MiuiBlurLocationManagerImpl.KEY_CID, 0);
                        double longitude = bundle.getDouble(MiuiBlurLocationManagerImpl.KEY_LONGITUDE, 0.0d);
                        double latitude = bundle.getDouble(MiuiBlurLocationManagerImpl.KEY_LATITUDE, 0.0d);
                        MiuiBlurLocationManagerImpl.this.mStashBlurLocationInfo.set(lac, cid, longitude, latitude);
                        return;
                    case 2:
                        MiuiBlurLocationManagerImpl.this.mSvStatusData = new SvStatusData(bundle.getInt("key_svcount", 0), bundle.getIntArray("key_svidWithFlags"), bundle.getFloatArray("key_cn0s"), bundle.getFloatArray("key_svElevations"), bundle.getFloatArray("key_svAzimuths"), bundle.getFloatArray("key_svCarrierFreqs"), bundle.getFloatArray("key_basebandCn0s"));
                        return;
                    default:
                        return;
                }
            }
        };
        this.mHandler = handler;
        this.mBlurMessenger = new Messenger(handler);
    }

    public void init(LocationManagerService service, Context context) {
        if (service != null && this.mLocationService == null) {
            this.mLocationService = service;
        }
        if (context != null && this.mContext == null) {
            this.mContext = context;
        }
    }

    private void initBlurLocationData() {
        if (this.mBlurLocationManager == null) {
            Log.i(TAG, "ILocationBlurry has not been register yet");
        } else if (this.mHandler.hasMessages(MSG_BLUR_LOCATION)) {
            Log.i(TAG, "blurLocationData return for too frequently.");
        } else {
            this.mHandler.sendEmptyMessageDelayed(MSG_BLUR_LOCATION, 300000L);
            try {
                Bundle input = new Bundle();
                input.putBinder(KEY_CALLBACK, this.mBlurMessenger.getBinder());
                this.mBlurLocationManager.pushBlurryCellLocation(input);
                Log.i(TAG, "MIUILOG- getBlurryCellLocation now");
            } catch (Exception e) {
                Log.e(TAG, "MIUILOG- getBlurryCellLocation exception", e);
            }
        }
    }

    public void registerLocationBlurryManager(ILocationBlurry manager) {
        if (!Build.IS_INTERNATIONAL_BUILD && manager != null) {
            Log.i(TAG, "registerLocationBlurryManager invoke");
            this.mBlurLocationManager = manager;
            initBlurLocationData();
        }
    }

    private String getS(CharSequence charSequence) {
        if (charSequence == null) {
            return null;
        }
        return charSequence.toString();
    }

    public Location getBlurryLocation(Location location, int uid, String pkgName) {
        if (location == null || !isBlurLocationMode(uid, pkgName)) {
            return location;
        }
        location.setLatitude(this.mStashBlurLocationInfo.latitude);
        location.setLongitude(this.mStashBlurLocationInfo.longitude);
        location.setExtras(null);
        this.mLastBlurLocation = location;
        return location;
    }

    public CellIdentity getBlurryCellLocation(CellIdentity location) {
        if (Build.IS_INTERNATIONAL_BUILD || location == null || this.mBlurLocationManager == null || !initBlurLocationReady()) {
            return location;
        }
        if (location instanceof CellIdentityGsm) {
            CellIdentityGsm identityGsm = (CellIdentityGsm) location;
            return new CellIdentityGsm(this.mStashBlurLocationInfo.lac, this.mStashBlurLocationInfo.cid, identityGsm.getArfcn(), identityGsm.getBsic(), identityGsm.getMccString(), identityGsm.getMncString(), getS(identityGsm.getOperatorAlphaLong()), getS(identityGsm.getOperatorAlphaShort()), identityGsm.getAdditionalPlmns());
        } else if (location instanceof CellIdentityCdma) {
            CellIdentityCdma identityCdma = (CellIdentityCdma) location;
            return new CellIdentityCdma(this.mStashBlurLocationInfo.lac, identityCdma.getSystemId(), this.mStashBlurLocationInfo.cid, identityCdma.getLongitude(), identityCdma.getLatitude(), getS(identityCdma.getOperatorAlphaLong()), getS(identityCdma.getOperatorAlphaShort()));
        } else if (location instanceof CellIdentityLte) {
            CellIdentityLte identityLte = (CellIdentityLte) location;
            return new CellIdentityLte(this.mStashBlurLocationInfo.cid, identityLte.getPci(), this.mStashBlurLocationInfo.lac, identityLte.getEarfcn(), identityLte.getBands(), identityLte.getBandwidth(), identityLte.getMccString(), identityLte.getMncString(), getS(identityLte.getOperatorAlphaLong()), getS(identityLte.getOperatorAlphaShort()), identityLte.getAdditionalPlmns(), identityLte.getClosedSubscriberGroupInfo());
        } else if (location instanceof CellIdentityWcdma) {
            CellIdentityWcdma identityWcdma = (CellIdentityWcdma) location;
            return new CellIdentityWcdma(this.mStashBlurLocationInfo.lac, this.mStashBlurLocationInfo.cid, identityWcdma.getPsc(), identityWcdma.getUarfcn(), identityWcdma.getMccString(), identityWcdma.getMncString(), getS(identityWcdma.getOperatorAlphaLong()), getS(identityWcdma.getOperatorAlphaShort()), identityWcdma.getAdditionalPlmns(), identityWcdma.getClosedSubscriberGroupInfo());
        } else if (!(location instanceof CellIdentityNr)) {
            return location;
        } else {
            CellIdentityNr identityNr = (CellIdentityNr) location;
            return new CellIdentityNr(this.mStashBlurLocationInfo.cid, this.mStashBlurLocationInfo.lac, identityNr.getNrarfcn(), identityNr.getBands(), identityNr.getMccString(), identityNr.getMncString(), identityNr.getNci(), getS(identityNr.getOperatorAlphaLong()), getS(identityNr.getOperatorAlphaShort()), identityNr.getAdditionalPlmns());
        }
    }

    public CellIdentity getBlurryCellLocation(CellIdentity location, int uid, String pkgName) {
        if (location == null || !isBlurLocationMode(uid, pkgName)) {
            return location;
        }
        return getBlurryCellLocation(location);
    }

    public List<CellInfo> getBlurryCellInfos(List<CellInfo> cellInfoList) {
        Iterator<CellInfo> it;
        if (Build.IS_INTERNATIONAL_BUILD || cellInfoList == null || cellInfoList.size() == 0 || this.mBlurLocationManager == null || !initBlurLocationReady()) {
            return cellInfoList;
        }
        List<CellInfo> results = new ArrayList<>();
        Iterator<CellInfo> it2 = cellInfoList.iterator();
        while (it2.hasNext()) {
            CellInfo info = it2.next();
            if (info instanceof CellInfoGsm) {
                CellIdentityGsm identityGsm = ((CellInfoGsm) info).getCellIdentity();
                CellInfoGsm result = new CellInfoGsm((CellInfoGsm) info);
                it = it2;
                result.setCellIdentity(new CellIdentityGsm(this.mStashBlurLocationInfo.lac, this.mStashBlurLocationInfo.cid, identityGsm.getArfcn(), identityGsm.getBsic(), identityGsm.getMccString(), identityGsm.getMncString(), getS(identityGsm.getOperatorAlphaLong()), getS(identityGsm.getOperatorAlphaShort()), identityGsm.getAdditionalPlmns()));
                results.add(result);
            } else {
                it = it2;
                if (info instanceof CellInfoCdma) {
                    CellIdentityCdma identityCdma = ((CellInfoCdma) info).getCellIdentity();
                    CellInfoCdma result2 = new CellInfoCdma((CellInfoCdma) info);
                    result2.setCellIdentity(new CellIdentityCdma(this.mStashBlurLocationInfo.lac, identityCdma.getSystemId(), this.mStashBlurLocationInfo.cid, identityCdma.getLongitude(), identityCdma.getLatitude(), getS(identityCdma.getOperatorAlphaLong()), getS(identityCdma.getOperatorAlphaShort())));
                    results.add(result2);
                } else if (info instanceof CellInfoLte) {
                    CellIdentityLte identityLte = ((CellInfoLte) info).getCellIdentity();
                    CellInfoLte result3 = new CellInfoLte((CellInfoLte) info);
                    result3.setCellIdentity(new CellIdentityLte(this.mStashBlurLocationInfo.cid, identityLte.getPci(), this.mStashBlurLocationInfo.lac, identityLte.getEarfcn(), identityLte.getBands(), identityLte.getBandwidth(), identityLte.getMccString(), identityLte.getMncString(), getS(identityLte.getOperatorAlphaLong()), getS(identityLte.getOperatorAlphaShort()), identityLte.getAdditionalPlmns(), identityLte.getClosedSubscriberGroupInfo()));
                    results.add(result3);
                } else if (info instanceof CellInfoWcdma) {
                    CellIdentityWcdma identityWcdma = ((CellInfoWcdma) info).getCellIdentity();
                    CellInfoWcdma result4 = new CellInfoWcdma((CellInfoWcdma) info);
                    result4.setCellIdentity(new CellIdentityWcdma(this.mStashBlurLocationInfo.lac, this.mStashBlurLocationInfo.cid, identityWcdma.getPsc(), identityWcdma.getUarfcn(), identityWcdma.getMccString(), identityWcdma.getMncString(), getS(identityWcdma.getOperatorAlphaLong()), getS(identityWcdma.getOperatorAlphaShort()), identityWcdma.getAdditionalPlmns(), identityWcdma.getClosedSubscriberGroupInfo()));
                    results.add(result4);
                } else if (info instanceof CellInfoNr) {
                    CellInfoNr infoNr = (CellInfoNr) info;
                    CellIdentityNr identityNr = (CellIdentityNr) infoNr.getCellIdentity();
                    CellIdentityNr resultNr = new CellIdentityNr(this.mStashBlurLocationInfo.cid, this.mStashBlurLocationInfo.lac, identityNr.getNrarfcn(), identityNr.getBands(), identityNr.getMccString(), identityNr.getMncString(), identityNr.getNci(), getS(identityNr.getOperatorAlphaLong()), getS(identityNr.getOperatorAlphaShort()), identityNr.getAdditionalPlmns());
                    results.add(new CellInfoNr(infoNr.getCellConnectionStatus(), infoNr.isRegistered(), infoNr.getTimeStamp(), resultNr, (CellSignalStrengthNr) infoNr.getCellSignalStrength()));
                } else {
                    results.add(info);
                }
            }
            it2 = it;
        }
        return results;
    }

    public List<CellInfo> getBlurryCellInfos(List<CellInfo> location, int uid, String pkgName) {
        if (location == null || location.size() == 0 || !isBlurLocationMode(uid, pkgName)) {
            return location;
        }
        return getBlurryCellInfos(location);
    }

    /* loaded from: classes.dex */
    public static class BaseCellLocation {
        private int cid;
        private int lac;
        private double latitude;
        private double longitude;

        private BaseCellLocation() {
        }

        public void set(int lac, int cid, double longitude, double latitude) {
            this.lac = lac;
            this.cid = cid;
            this.longitude = longitude;
            this.latitude = latitude;
        }

        public boolean isValidInfo() {
            return (this.lac == 0 || this.cid == 0 || this.longitude == 0.0d || this.latitude == 0.0d) ? false : true;
        }

        public String toString() {
            return "BaseCellLocation{lac=" + this.lac + ", cid=" + this.cid + ", longitude=" + this.longitude + ", latitude=" + this.latitude + '}';
        }
    }

    private AppOpsManager getAppOps() {
        if (this.mAppOps == null) {
            this.mAppOps = (AppOpsManager) this.mContext.getSystemService("appops");
        }
        return this.mAppOps;
    }

    private void logIfDebug(String info) {
    }

    public boolean isBlurLocationMode(int uid, String pkgName) {
        return !Build.IS_INTERNATIONAL_BUILD && UserHandle.getAppId(uid) >= 10000 && this.mBlurLocationManager != null && noteOpUseMyIdentity(uid, pkgName) && initBlurLocationReady();
    }

    private boolean noteOpUseMyIdentity(int uid, String pkgName) {
        long identity = Binder.clearCallingIdentity();
        try {
            return getAppOps().noteOpNoThrow(10036, uid, pkgName, "MiuiBlueLocationManager#noteOpUseMyIdentity", (String) null) != 0;
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public boolean initBlurLocationReady() {
        if (!this.mStashBlurLocationInfo.isValidInfo()) {
            initBlurLocationData();
        }
        boolean validInfo = this.mStashBlurLocationInfo.isValidInfo();
        if (!validInfo) {
            this.mHandler.removeMessages(MSG_BLUR_LOCATION);
        }
        return validInfo;
    }

    public void updateSvStatusData(int svCount, int[] svidWithFlags, float[] cn0s, float[] svElevations, float[] svAzimuths, float[] svCarrierFreqs, float[] basebandCn0s) {
        if (!Build.IS_INTERNATIONAL_BUILD && this.mBlurLocationManager != null && svCount >= 20) {
            if (!this.mHandler.hasMessages(3)) {
                this.mSvStatusData = new SvStatusData(svCount, svidWithFlags, cn0s, svElevations, svAzimuths, svCarrierFreqs, basebandCn0s);
                Bundle data = new Bundle();
                data.putInt("key_svcount", svCount);
                data.putIntArray("key_svidWithFlags", svidWithFlags);
                data.putFloatArray("key_cn0s", cn0s);
                data.putFloatArray("key_svElevations", svElevations);
                data.putFloatArray("key_svAzimuths", svAzimuths);
                data.putFloatArray("key_svCarrierFreqs", svCarrierFreqs);
                data.putFloatArray("key_basebandCn0s", basebandCn0s);
                data.putBinder(KEY_CALLBACK, this.mBlurMessenger.getBinder());
                try {
                    this.mBlurLocationManager.sendSvStatusData(data);
                } catch (RemoteException e) {
                }
                Handler handler = this.mHandler;
                handler.sendMessageDelayed(handler.obtainMessage(3), 900000L);
            }
        }
    }

    public SvStatusData getSvStatusData() {
        return this.mSvStatusData;
    }

    public void mayNotifyGpsListener(String currentProvider, LocationResult location) {
        LocationProviderManager manager;
        if ("gps".equals(currentProvider) || "passive".equals(currentProvider) || (manager = this.mLocationService.getLocationProviderManager("gps")) == null) {
            return;
        }
        synchronized (manager.mLock) {
            long clearCallingIdentity = Binder.clearCallingIdentity();
            try {
                manager.mayNotifyGpsBlurListener(location);
                Binder.restoreCallingIdentity(clearCallingIdentity);
            } catch (Exception e) {
                e.printStackTrace();
                Binder.restoreCallingIdentity(clearCallingIdentity);
            }
        }
    }

    public Location getLocationIfBlurMode(LastLocationRequest request, CallerIdentity identity, int permissionLevel) {
        Location current;
        if (!isBlurLocationMode(identity)) {
            return null;
        }
        Location blurGpsLocation = null;
        Iterator it = this.mLocationService.mProviderManagers.iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            LocationProviderManager lpm = (LocationProviderManager) it.next();
            if (!"gps".equals(lpm.getName()) && (current = lpm.getLastLocation(request, identity, permissionLevel)) != null) {
                blurGpsLocation = new Location(current);
                break;
            }
        }
        if (blurGpsLocation != null) {
            blurGpsLocation.setProvider("gps");
            this.mLastBlurLocation = blurGpsLocation;
        }
        return blurGpsLocation;
    }

    public void handleGpsLocationChangedLocked(String provider, CallerIdentity identity) {
        LocationProviderManager gpsManager;
        if (!"gps".equals(provider) || this.mBlurGpsHandler.hasMessages(identity.getUid()) || !isBlurLocationMode(identity.getUid(), identity.getPackageName()) || (gpsManager = this.mLocationService.getLocationProviderManager("gps")) == null || this.mLastBlurLocation == null) {
            return;
        }
        this.mLastBlurLocation.setProvider("gps");
        synchronized (gpsManager.mLock) {
            long clearCallingIdentity = Binder.clearCallingIdentity();
            try {
                if (gpsManager.onReportBlurLocation(LocationResult.create(new Location[]{this.mLastBlurLocation}), identity)) {
                    Handler handler = this.mBlurGpsHandler;
                    handler.sendMessageDelayed(handler.obtainMessage(identity.getUid(), identity), ActivityManagerServiceImpl.BOOST_DURATION);
                }
                Binder.restoreCallingIdentity(clearCallingIdentity);
            } catch (Exception e) {
                e.printStackTrace();
                Binder.restoreCallingIdentity(clearCallingIdentity);
            }
        }
    }

    public LocationResult getBlurryLocation(LocationResult location, CallerIdentity identity) {
        if (location == null || !isBlurLocationMode(identity.getUid(), identity.getPackageName())) {
            return location;
        }
        LocationResult blurResult = location.deepCopy();
        int size = blurResult.size();
        for (int i = 0; i < size; i++) {
            Location blur = blurResult.get(i);
            blur.setLatitude(this.mStashBlurLocationInfo.latitude);
            blur.setLongitude(this.mStashBlurLocationInfo.longitude);
        }
        return blurResult;
    }
}
