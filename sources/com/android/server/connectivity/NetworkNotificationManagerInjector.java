package com.android.server.connectivity;

import android.app.BroadcastOptions;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.UserHandle;
import android.text.TextUtils;
import com.android.server.MiuiConfigCaptivePortal;
/* loaded from: classes.dex */
class NetworkNotificationManagerInjector {
    NetworkNotificationManagerInjector() {
    }

    public void showLogin(Context context, Intent intent, String ssid) {
        WifiManager wifiManager = (WifiManager) context.getSystemService("wifi");
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        Intent loginIntent = new Intent();
        BroadcastOptions options = BroadcastOptions.makeBasic();
        options.setBackgroundActivityStartsAllowed(true);
        loginIntent.addFlags(268435456);
        loginIntent.setPackage("com.android.htmlviewer");
        loginIntent.setAction("com.miui.action.OPEN_WIFI_LOGIN");
        loginIntent.putExtra("miui.intent.extra.OPEN_WIFI_SSID", ssid);
        loginIntent.setData(Uri.parse(MiuiConfigCaptivePortal.getCaptivePortalServer(context, intent.getDataString()).toString()));
        if (wifiInfo != null && TextUtils.equals(ssid, wifiInfo.getSSID())) {
            loginIntent.putExtra("miui.intent.extra.BSSID", wifiInfo.getBSSID());
        }
        loginIntent.putExtra("miui.intent.extra.CAPTIVE_PORTAL", intent.getIBinderExtra("miui.intent.extra.CAPTIVE_PORTAL"));
        loginIntent.putExtra("miui.intent.extra.NETWORK", intent.getParcelableExtra("miui.intent.extra.NETWORK"));
        loginIntent.putExtra("miui.intent.extra.EXPLICIT_SELECTED", Boolean.valueOf(intent.getBooleanExtra("miui.intent.extra.EXPLICIT_SELECTED", false)));
        loginIntent.addFlags(16777216);
        context.sendBroadcast(loginIntent, null, options.toBundle());
        Intent cacheIntent = new Intent(loginIntent);
        cacheIntent.setPackage("com.android.settings");
        cacheIntent.setAction("miui.intent.CACHE_OPENWIFI");
        context.sendStickyBroadcastAsUser(cacheIntent, UserHandle.ALL);
    }
}
