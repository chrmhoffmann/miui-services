package com.android.server.location;

import android.os.SystemProperties;
import com.miui.base.MiuiStubRegistry;
import java.io.PrintWriter;
import java.util.Properties;
/* loaded from: classes.dex */
public class LocationDumpLogImpl implements LocationDumpLogStub {
    private static final String DUMP_TAG_GLP = "=MI GLP= ";
    private static final String DUMP_TAG_GLP_EN = "=MI GLP EN=";
    private static final String DUMP_TAG_LMS = "=MI LMS= ";
    private static final String DUMP_TAG_NMEA = "=MI NMEA=";
    private static final GnssLocalLog mdumpLms = new GnssLocalLog(1000);
    private static final GnssLocalLog mdumpGlp = new GnssLocalLog(1000);
    private static final GnssLocalLog mdumpNmea = new GnssLocalLog(20000);
    private int defaultNetworkProviderName = 17040027;
    private int defaultFusedProviderName = 17040001;
    private int defaultGeocoderProviderName = 17040002;
    private int defaultGeofenceProviderName = 17040003;
    private boolean mRecordLoseCount = true;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<LocationDumpLogImpl> {

        /* compiled from: LocationDumpLogImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final LocationDumpLogImpl INSTANCE = new LocationDumpLogImpl();
        }

        public LocationDumpLogImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public LocationDumpLogImpl provideNewInstance() {
            return new LocationDumpLogImpl();
        }
    }

    public void addToBugreport(int type, String log) {
        switchTypeToDump(type).log(switchTypeToLogTag(type) + log);
    }

    public void setLength(int type, int length) {
        switchTypeToDump(type).setLength(length);
    }

    public String getConfig(Properties properties, String config, String defaultConfig) {
        return properties.getProperty(config, defaultConfig);
    }

    public boolean getRecordLoseLocation() {
        return this.mRecordLoseCount;
    }

    public void setRecordLoseLocation(boolean newValue) {
        this.mRecordLoseCount = newValue;
    }

    public int getDefaultProviderName(String type) {
        if (isXOptMode() && !isCnVersion()) {
            this.defaultNetworkProviderName = 17040028;
            this.defaultFusedProviderName = 17040028;
        }
        char c = 65535;
        switch (type.hashCode()) {
            case 97798435:
                if (type.equals("fused")) {
                    c = 1;
                    break;
                }
                break;
            case 1837067124:
                if (type.equals("geocoder")) {
                    c = 2;
                    break;
                }
                break;
            case 1839549312:
                if (type.equals("geofence")) {
                    c = 3;
                    break;
                }
                break;
            case 1843485230:
                if (type.equals("network")) {
                    c = 0;
                    break;
                }
                break;
        }
        switch (c) {
            case 0:
                return this.defaultNetworkProviderName;
            case 1:
                return this.defaultFusedProviderName;
            case 2:
                return this.defaultGeocoderProviderName;
            default:
                return this.defaultGeofenceProviderName;
        }
    }

    public void dump(int type, PrintWriter pw) {
        switchTypeToDump(type).dump(pw);
    }

    private boolean isXOptMode() {
        return !SystemProperties.getBoolean("persist.sys.miui_optimization", true);
    }

    private boolean isCnVersion() {
        return "CN".equalsIgnoreCase(SystemProperties.get("ro.miui.build.region"));
    }

    private GnssLocalLog switchTypeToDump(int type) {
        switch (type) {
            case 1:
                return mdumpLms;
            case 2:
            case 3:
                return mdumpGlp;
            default:
                return mdumpNmea;
        }
    }

    private String switchTypeToLogTag(int type) {
        switch (type) {
            case 1:
                return DUMP_TAG_LMS;
            case 2:
                return DUMP_TAG_GLP;
            case 3:
                return DUMP_TAG_GLP_EN;
            default:
                return DUMP_TAG_NMEA;
        }
    }

    public void clearData() {
        mdumpGlp.clearData();
        mdumpNmea.clearData();
    }
}
