package com.android.server.location.gnss.operators;

import android.content.Context;
import android.os.Bundle;
import android.os.SystemProperties;
import android.provider.Settings;
import com.android.server.location.gnss.GnssConfiguration;
import com.miui.base.MiuiStubRegistry;
/* loaded from: classes.dex */
public class GnssForKtCustomImpl implements GnssForKtCustomStub {
    private static final int GPS_POSITION_MODE_MS_BASED = 1;
    private static final int GPS_POSITION_MODE_STANDALONE = 0;
    private static final String PRODUCT_DEVICE_MODE = "ro.product.mod_device";
    private static final String SUFFIX_KT_GLOBAL = "kt_global";
    private static final String TAG = "GnssForKtCustomImpl";
    private static boolean mIsNeedSetSuplHostPort;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<GnssForKtCustomImpl> {

        /* compiled from: GnssForKtCustomImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final GnssForKtCustomImpl INSTANCE = new GnssForKtCustomImpl();
        }

        public GnssForKtCustomImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public GnssForKtCustomImpl provideNewInstance() {
            return new GnssForKtCustomImpl();
        }
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Code restructure failed: missing block: B:23:0x0050, code lost:
        if (r8.equals("activateAGPS") != false) goto L28;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public void onExtraCommand(android.content.Context r7, java.lang.String r8, android.os.Bundle r9, int r10, com.android.server.location.gnss.GnssConfiguration r11, com.android.server.location.gnss.hal.GnssNative r12) {
        /*
            r6 = this;
            r0 = 0
            com.android.server.location.gnss.operators.GnssForKtCustomImpl.mIsNeedSetSuplHostPort = r0
            int r1 = r8.hashCode()
            r2 = 2
            r3 = 1
            r4 = -1
            switch(r1) {
                case -1476023501: goto L53;
                case -1195326436: goto L4a;
                case -1146931529: goto L40;
                case -270008426: goto L36;
                case -75324903: goto L2c;
                case 219485981: goto L22;
                case 630737852: goto L18;
                case 1984784677: goto Le;
                default: goto Ld;
            }
        Ld:
            goto L5d
        Le:
            java.lang.String r0 = "setMode"
            boolean r0 = r8.equals(r0)
            if (r0 == 0) goto Ld
            r0 = 5
            goto L5e
        L18:
            java.lang.String r0 = "setNativeServer"
            boolean r0 = r8.equals(r0)
            if (r0 == 0) goto Ld
            r0 = 7
            goto L5e
        L22:
            java.lang.String r0 = "deactivateAGPS"
            boolean r0 = r8.equals(r0)
            if (r0 == 0) goto Ld
            r0 = r2
            goto L5e
        L2c:
            java.lang.String r0 = "getMode"
            boolean r0 = r8.equals(r0)
            if (r0 == 0) goto Ld
            r0 = 4
            goto L5e
        L36:
            java.lang.String r0 = "deactivateGPS"
            boolean r0 = r8.equals(r0)
            if (r0 == 0) goto Ld
            r0 = 3
            goto L5e
        L40:
            java.lang.String r0 = "activateGPS"
            boolean r0 = r8.equals(r0)
            if (r0 == 0) goto Ld
            r0 = r3
            goto L5e
        L4a:
            java.lang.String r1 = "activateAGPS"
            boolean r1 = r8.equals(r1)
            if (r1 == 0) goto Ld
            goto L5e
        L53:
            java.lang.String r0 = "setOllehServer"
            boolean r0 = r8.equals(r0)
            if (r0 == 0) goto Ld
            r0 = 6
            goto L5e
        L5d:
            r0 = r4
        L5e:
            switch(r0) {
                case 0: goto Lcf;
                case 1: goto Lcb;
                case 2: goto Lc7;
                case 3: goto Lc3;
                case 4: goto Lbf;
                case 5: goto Lbb;
                case 6: goto L7d;
                case 7: goto L7a;
                default: goto L61;
            }
        L61:
            java.lang.StringBuilder r0 = new java.lang.StringBuilder
            r0.<init>()
            java.lang.String r1 = "ktSendExtraCommand: unknown command "
            java.lang.StringBuilder r0 = r0.append(r1)
            java.lang.StringBuilder r0 = r0.append(r8)
            java.lang.String r0 = r0.toString()
            java.lang.String r1 = "GnssForKtCustomImpl"
            android.util.Log.w(r1, r0)
            goto Ld3
        L7a:
            com.android.server.location.gnss.operators.GnssForKtCustomImpl.mIsNeedSetSuplHostPort = r3
            goto Ld3
        L7d:
            java.lang.String r0 = "host"
            java.lang.String r1 = ""
            java.lang.String r0 = r9.getString(r0, r1)
            java.lang.String r1 = "port"
            int r1 = r9.getInt(r1, r4)
            boolean r5 = r0.isEmpty()
            if (r5 != 0) goto Ld3
            if (r1 == r4) goto Ld3
            r12.setAgpsServer(r3, r0, r1)
            com.android.server.location.LocationDumpLogStub r3 = com.android.server.location.LocationDumpLogStub.getInstance()
            java.lang.StringBuilder r4 = new java.lang.StringBuilder
            r4.<init>()
            java.lang.String r5 = "kt-server: setOllehServer, "
            java.lang.StringBuilder r4 = r4.append(r5)
            java.lang.StringBuilder r4 = r4.append(r0)
            java.lang.String r5 = ":"
            java.lang.StringBuilder r4 = r4.append(r5)
            java.lang.StringBuilder r4 = r4.append(r1)
            java.lang.String r4 = r4.toString()
            r3.addToBugreport(r2, r4)
            goto Ld3
        Lbb:
            r6.ktSetMode(r9, r11)
            goto Ld3
        Lbf:
            r6.ktGetMode(r9, r10)
            goto Ld3
        Lc3:
            r6.deactivateGPS(r7)
            goto Ld3
        Lc7:
            r6.deactivateAGPS(r7)
            goto Ld3
        Lcb:
            r6.activateGPS(r7)
            goto Ld3
        Lcf:
            r6.activateAGPS(r7)
        Ld3:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.location.gnss.operators.GnssForKtCustomImpl.onExtraCommand(android.content.Context, java.lang.String, android.os.Bundle, int, com.android.server.location.gnss.GnssConfiguration, com.android.server.location.gnss.hal.GnssNative):void");
    }

    public boolean isNeedSetSuplHostPort() {
        return mIsNeedSetSuplHostPort;
    }

    public boolean isKtGlobalSystem() {
        String mDeviceType = SystemProperties.get(PRODUCT_DEVICE_MODE, "null");
        return mDeviceType.endsWith(SUFFIX_KT_GLOBAL);
    }

    private void activateGPS(Context context) {
        Settings.Secure.putInt(context.getContentResolver(), "location_mode", 3);
    }

    private void activateAGPS(Context context) {
        Settings.Global.putInt(context.getContentResolver(), "assisted_gps_enabled", 1);
    }

    private void deactivateAGPS(Context context) {
        Settings.Global.putInt(context.getContentResolver(), "assisted_gps_enabled", 0);
    }

    private void deactivateGPS(Context context) {
        Settings.Secure.putInt(context.getContentResolver(), "location_mode", 0);
    }

    private void ktGetMode(Bundle extras, int suplMode) {
        if (suplMode == 0) {
            extras.putInt("mode", 0);
        } else if (suplMode == 1) {
            extras.putInt("mode", 1);
        }
    }

    private void ktSetMode(Bundle extras, GnssConfiguration gnssConfiguration) {
        if (gnssConfiguration != null) {
            int mode = extras.getInt("mode", -1);
            if (mode == 0) {
                gnssConfiguration.setSuplMode(0);
            } else if (mode == 1) {
                gnssConfiguration.setSuplMode(1);
            }
        }
    }
}
