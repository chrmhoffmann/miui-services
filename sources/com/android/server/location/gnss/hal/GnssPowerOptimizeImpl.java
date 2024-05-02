package com.android.server.location.gnss.hal;

import android.content.Context;
import android.location.Location;
import com.android.server.location.gnss.hal.GpoUtil;
import com.miui.base.MiuiStubRegistry;
/* loaded from: classes.dex */
public class GnssPowerOptimizeImpl implements GnssPowerOptimizeStub {
    private static final String TAG = "GnssPowerOptimize";
    private Context mContext;
    private final GpoUtil mGpoUtil = GpoUtil.getInstance();
    private Gpo5Client mGpo5Client = null;
    private Gpo4Client mGpo4Client = null;
    private boolean isFeatureSupport = false;
    private int mGpoVersion = 0;
    private final GpoUtil.IDataEvent mIDataEvent = new GpoUtil.IDataEvent() { // from class: com.android.server.location.gnss.hal.GnssPowerOptimizeImpl.1
        @Override // com.android.server.location.gnss.hal.GpoUtil.IDataEvent
        public void updateFeatureSwitch(int version) {
            if (GnssPowerOptimizeImpl.this.mGpoVersion != version && GnssPowerOptimizeImpl.this.getEngineStatus() == 4) {
                GnssPowerOptimizeImpl.this.mGpoUtil.logi(GnssPowerOptimizeImpl.TAG, "Update Gpo Client" + version, true);
                GnssPowerOptimizeImpl gnssPowerOptimizeImpl = GnssPowerOptimizeImpl.this;
                gnssPowerOptimizeImpl.init(gnssPowerOptimizeImpl.mContext);
            }
        }

        @Override // com.android.server.location.gnss.hal.GpoUtil.IDataEvent
        public void updateScreenState(boolean on) {
            GnssPowerOptimizeImpl.this.mGpoUtil.logi(GnssPowerOptimizeImpl.TAG, "Screen On ? " + on, true);
            if (GnssPowerOptimizeImpl.this.mGpo5Client == null) {
                if (GnssPowerOptimizeImpl.this.mGpo4Client != null) {
                    GnssPowerOptimizeImpl.this.mGpo4Client.updateScreenState(on);
                    return;
                }
                return;
            }
            GnssPowerOptimizeImpl.this.mGpo5Client.updateScreenState(on);
        }

        @Override // com.android.server.location.gnss.hal.GpoUtil.IDataEvent
        public void updateDefaultNetwork(int type) {
        }
    };

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<GnssPowerOptimizeImpl> {

        /* compiled from: GnssPowerOptimizeImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final GnssPowerOptimizeImpl INSTANCE = new GnssPowerOptimizeImpl();
        }

        public GnssPowerOptimizeImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public GnssPowerOptimizeImpl provideNewInstance() {
            return new GnssPowerOptimizeImpl();
        }
    }

    public void init(Context context) {
        this.mGpoUtil.logv(TAG, "init ");
        this.mContext = context;
        this.mGpoUtil.initOnce(context, this.mIDataEvent);
        this.mGpoVersion = this.mGpoUtil.getGpoVersion();
        this.mGpoUtil.logi(TAG, "Gpo Version: " + this.mGpoVersion, true);
        switch (this.mGpoVersion) {
            case 4:
                break;
            default:
                this.mGpoUtil.logv(TAG, "Not Support GPO.");
                this.isFeatureSupport = false;
                deinitGpo5();
                deinitGpo4();
                return;
            case 5:
                this.isFeatureSupport = true;
                Gpo5Client gpo5Client = Gpo5Client.getInstance();
                this.mGpo5Client = gpo5Client;
                gpo5Client.init(this.mContext);
                if (this.mGpo5Client.checkSensorSupport()) {
                    GnssScoringModelStub.getInstance().init(true);
                    return;
                }
                this.mGpoVersion--;
                this.mGpoUtil.logi(TAG, "Sensor do not support, downgrade to Version: " + this.mGpoVersion, true);
                deinitGpo5();
                break;
        }
        this.isFeatureSupport = true;
        Gpo4Client gpo4Client = Gpo4Client.getInstance();
        this.mGpo4Client = gpo4Client;
        gpo4Client.init(this.mContext);
    }

    private void deinitGpo5() {
        Gpo5Client gpo5Client = this.mGpo5Client;
        if (gpo5Client != null) {
            gpo5Client.deinit();
            this.mGpo5Client = null;
        }
        GnssScoringModelStub.getInstance().init(false);
    }

    private void deinitGpo4() {
        Gpo4Client gpo4Client = this.mGpo4Client;
        if (gpo4Client != null) {
            gpo4Client.deinit();
            this.mGpo4Client = null;
        }
    }

    public void disableGnssSwitch() {
        if (!this.isFeatureSupport) {
            return;
        }
        this.mGpoUtil.logv(TAG, "disableGnssSwitch");
        Gpo5Client gpo5Client = this.mGpo5Client;
        if (gpo5Client == null) {
            Gpo4Client gpo4Client = this.mGpo4Client;
            if (gpo4Client != null) {
                gpo4Client.disableGnssSwitch();
                return;
            }
            return;
        }
        gpo5Client.disableGnssSwitch();
    }

    public boolean isBlackListControlEnabled() {
        return this.mGpoVersion == 2;
    }

    public void saveLocationRequestId(int uid, String pkn, String provider, int listenerHashCode, Object callbackType) {
        if (!this.isFeatureSupport || "passive".equalsIgnoreCase(provider)) {
            return;
        }
        this.mGpoUtil.logv(TAG, "saveLocationRequestId: " + listenerHashCode + ", " + uid + provider);
        Gpo5Client gpo5Client = this.mGpo5Client;
        if (gpo5Client != null) {
            gpo5Client.saveLocationRequestId(uid, pkn, provider, listenerHashCode, callbackType);
            return;
        }
        Gpo4Client gpo4Client = this.mGpo4Client;
        if (gpo4Client != null) {
            gpo4Client.saveLocationRequestId(uid, pkn, provider, listenerHashCode, callbackType);
        }
    }

    public void removeLocationRequestId(int listenerHashCode) {
        if (!this.isFeatureSupport) {
            return;
        }
        this.mGpoUtil.logv(TAG, "removeLocationRequestId: " + listenerHashCode);
        Gpo5Client gpo5Client = this.mGpo5Client;
        if (gpo5Client == null) {
            Gpo4Client gpo4Client = this.mGpo4Client;
            if (gpo4Client != null) {
                gpo4Client.removeLocationRequestId(listenerHashCode);
                return;
            }
            return;
        }
        gpo5Client.removeLocationRequestId(listenerHashCode);
    }

    public boolean blockEngineStart() {
        if (!this.isFeatureSupport) {
            return false;
        }
        this.mGpoUtil.logv(TAG, "blockEngineStart");
        Gpo5Client gpo5Client = this.mGpo5Client;
        if (gpo5Client != null) {
            return gpo5Client.blockEngineStart();
        }
        Gpo4Client gpo4Client = this.mGpo4Client;
        return gpo4Client != null && gpo4Client.blockEngineStart();
    }

    public void clearLocationRequest() {
        if (!this.isFeatureSupport) {
            return;
        }
        this.mGpoUtil.logv(TAG, "clearLocationRequest");
        Gpo5Client gpo5Client = this.mGpo5Client;
        if (gpo5Client == null) {
            Gpo4Client gpo4Client = this.mGpo4Client;
            if (gpo4Client != null) {
                gpo4Client.clearLocationRequest();
                return;
            }
            return;
        }
        gpo5Client.clearLocationRequest();
    }

    public void reportLocation2Gpo(Location location) {
        if (!this.isFeatureSupport) {
            return;
        }
        this.mGpoUtil.logv(TAG, "reportLocation2Gpo");
        Gpo5Client gpo5Client = this.mGpo5Client;
        if (gpo5Client == null) {
            Gpo4Client gpo4Client = this.mGpo4Client;
            if (gpo4Client != null) {
                gpo4Client.reportLocation2Gpo(location);
                return;
            }
            return;
        }
        gpo5Client.reportLocation2Gpo(location);
    }

    public void setGpoVersionValue(int newValue) {
        this.mGpoUtil.setGpoVersionValue(newValue);
    }

    public int getEngineStatus() {
        return this.mGpoUtil.getEngineStatus();
    }

    public void setEngineStatus(int status) {
        this.mGpoUtil.setEngineStatus(status);
    }

    public boolean engineStoppedByGpo() {
        if (!this.isFeatureSupport) {
            return false;
        }
        this.mGpoUtil.logv(TAG, "engineStoppedByGpo");
        return this.mGpoUtil.engineStoppedByGpo();
    }

    public void recordEngineUsageDaily(long time) {
        if (!this.isFeatureSupport) {
            return;
        }
        this.mGpoUtil.logv(TAG, "recordEngineUsageDaily: " + time);
        this.mGpoUtil.recordEngineUsageDaily(time);
    }
}
