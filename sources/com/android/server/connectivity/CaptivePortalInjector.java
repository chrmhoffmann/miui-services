package com.android.server.connectivity;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.IMiuiCaptivePortal;
import android.net.INetworkMonitor;
import android.net.Network;
import android.net.NetworkMonitorManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Log;
import com.android.server.MiuiConfigCaptivePortal;
import com.android.server.wm.ActivityStarterInjector;
/* loaded from: classes.dex */
public class CaptivePortalInjector {
    private static final String TAG = "CaptivePortalInjector";
    private static final int TYPE_SLAVE_WIFI = 30;

    /* loaded from: classes.dex */
    public static class NetworkAgentInfoInner {
        private static boolean mExplicitlySelected;
        private static Object mNM;
        private Network mNetwork;
        private int mNetworkType;

        public NetworkAgentInfoInner(NetworkAgentInfo nai) {
            mNM = nai.networkMonitor();
            this.mNetworkType = nai.networkInfo.getType();
            this.mNetwork = nai.network;
            mExplicitlySelected = nai.networkAgentConfig.explicitlySelected;
        }

        public static void setNetworkMonitor(Object obj) {
            mNM = obj;
        }

        public static void setExplicitlySelected(boolean explicitlySelected) {
            mExplicitlySelected = explicitlySelected;
        }
    }

    public static final PendingIntent getCaptivePortalPendingIntent(Context context, PendingIntent pi, final NetworkAgentInfoInner naii) {
        boolean isSupportDualWifi = supportDualWifi(context);
        if (naii != null) {
            boolean z = true;
            if ((naii.mNetworkType == 1 || (isSupportDualWifi && naii.mNetworkType == 30)) && pi != null && pi.getIntent() != null) {
                Intent intent = pi.getIntent();
                intent.putExtra("miui.intent.extra.CAPTIVE_PORTAL", (IBinder) new IMiuiCaptivePortal.Stub() { // from class: com.android.server.connectivity.CaptivePortalInjector.1
                    public void appResponse(int response) {
                        if (NetworkAgentInfoInner.mNM instanceof INetworkMonitor) {
                            long token2 = Binder.clearCallingIdentity();
                            try {
                                try {
                                    INetworkMonitor nm = (INetworkMonitor) NetworkAgentInfoInner.mNM;
                                    nm.notifyCaptivePortalAppFinished(response);
                                } catch (RemoteException e) {
                                    Log.d(CaptivePortalInjector.TAG, "notifyCaptivePortalAppFinished failure");
                                    e.printStackTrace();
                                }
                            } finally {
                                Binder.restoreCallingIdentity(token2);
                            }
                        } else if (NetworkAgentInfoInner.mNM instanceof NetworkMonitorManager) {
                            NetworkMonitorManager nmm = (NetworkMonitorManager) NetworkAgentInfoInner.mNM;
                            nmm.notifyCaptivePortalAppFinished(response);
                        } else {
                            Log.e(CaptivePortalInjector.TAG, "Unknown type of nai.networkMonitor()");
                        }
                    }
                });
                intent.putExtra("miui.intent.extra.NETWORK", naii.mNetwork);
                intent.putExtra("miui.intent.extra.EXPLICIT_SELECTED", NetworkAgentInfoInner.mExplicitlySelected);
                if (isSupportDualWifi) {
                    if (naii.mNetworkType != 30) {
                        z = false;
                    }
                    intent.putExtra("miui.intent.extra.IS_SLAVE", z);
                }
                if (pi.isActivity()) {
                    return PendingIntent.getActivity(context, 0, intent, ActivityStarterInjector.FLAG_ASSOCIATED_SETTINGS_AV);
                }
                return PendingIntent.getBroadcast(context, 0, intent, ActivityStarterInjector.FLAG_ASSOCIATED_SETTINGS_AV);
            }
            return pi;
        }
        return pi;
    }

    public static final PendingIntent getCaptivePortalPendingIntent(Context context, PendingIntent pi, Intent intent, final NetworkAgentInfoInner naii) {
        boolean isSupportDualWifi = supportDualWifi(context);
        if (naii != null) {
            boolean z = true;
            if ((naii.mNetworkType == 1 || (isSupportDualWifi && naii.mNetworkType == 30)) && pi != null && intent != null) {
                intent.putExtra("miui.intent.extra.CAPTIVE_PORTAL", (IBinder) new IMiuiCaptivePortal.Stub() { // from class: com.android.server.connectivity.CaptivePortalInjector.2
                    public void appResponse(int response) {
                        if (NetworkAgentInfoInner.mNM instanceof INetworkMonitor) {
                            long token2 = Binder.clearCallingIdentity();
                            try {
                                try {
                                    INetworkMonitor nm = (INetworkMonitor) NetworkAgentInfoInner.mNM;
                                    nm.notifyCaptivePortalAppFinished(response);
                                } catch (RemoteException e) {
                                    Log.d(CaptivePortalInjector.TAG, "notifyCaptivePortalAppFinished failure");
                                    e.printStackTrace();
                                }
                            } finally {
                                Binder.restoreCallingIdentity(token2);
                            }
                        } else if (NetworkAgentInfoInner.mNM instanceof NetworkMonitorManager) {
                            NetworkMonitorManager nmm = (NetworkMonitorManager) NetworkAgentInfoInner.mNM;
                            nmm.notifyCaptivePortalAppFinished(response);
                        } else {
                            Log.e(CaptivePortalInjector.TAG, "Unknown type of nai.networkMonitor()");
                        }
                    }
                });
                intent.putExtra("miui.intent.extra.NETWORK", naii.mNetwork);
                intent.putExtra("miui.intent.extra.EXPLICIT_SELECTED", NetworkAgentInfoInner.mExplicitlySelected);
                if (isSupportDualWifi) {
                    if (naii.mNetworkType != 30) {
                        z = false;
                    }
                    intent.putExtra("miui.intent.extra.IS_SLAVE", z);
                }
                long token = Binder.clearCallingIdentity();
                try {
                    if (pi.isActivity()) {
                        pi = PendingIntent.getActivity(context, 0, intent, ActivityStarterInjector.FLAG_ASSOCIATED_SETTINGS_AV);
                    } else {
                        pi = PendingIntent.getBroadcast(context, 0, intent, ActivityStarterInjector.FLAG_ASSOCIATED_SETTINGS_AV);
                    }
                } finally {
                    Binder.restoreCallingIdentity(token);
                }
            }
        }
        return pi;
    }

    static final boolean enableDataAndWifiRoam(Context context) {
        return MiuiConfigCaptivePortal.enableDataAndWifiRoam(context);
    }

    private static boolean supportDualWifi(Context context) {
        boolean support = false;
        if (context == null) {
            return false;
        }
        String cloudvalue = Settings.System.getString(context.getContentResolver(), "cloud_slave_wifi_support");
        if (!"off".equals(cloudvalue)) {
            if (SystemProperties.getInt("ro.vendor.net.enable_dual_wifi", 0) == 1) {
                support = true;
            }
        } else {
            support = false;
        }
        Log.d(TAG, "supportDualWifi:" + support);
        return support;
    }
}
