package com.android.server;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.provider.Settings;
import android.util.Log;
import android.util.NtpTrustedTime;
import com.miui.base.MiuiStubRegistry;
import java.util.ArrayList;
import miui.os.Build;
/* loaded from: classes.dex */
public class NetworkTimeUpdateServiceImpl implements NetworkTimeUpdateServiceStub {
    private static String CN_NTP_SERVER = "pool.ntp.org";
    private static final boolean DBG = true;
    private static final String TAG = "NetworkTimeUpdateService";
    private Context mContext;
    private String mDefaultNtpServer;
    private ArrayList<String> mNtpServers = new ArrayList<>();
    private NtpTrustedTime mTime;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<NetworkTimeUpdateServiceImpl> {

        /* compiled from: NetworkTimeUpdateServiceImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final NetworkTimeUpdateServiceImpl INSTANCE = new NetworkTimeUpdateServiceImpl();
        }

        public NetworkTimeUpdateServiceImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public NetworkTimeUpdateServiceImpl provideNewInstance() {
            return new NetworkTimeUpdateServiceImpl();
        }
    }

    public void initNtpServers(Context context, NtpTrustedTime trustedTime) {
        this.mTime = trustedTime;
        initDefaultNtpServer(context);
        this.mNtpServers.add(this.mDefaultNtpServer);
        String[] globalNtpServers = context.getResources().getStringArray(285409358);
        String[] chinaNtpServers = context.getResources().getStringArray(285409297);
        for (String ntpServer : globalNtpServers) {
            this.mNtpServers.add(ntpServer);
        }
        if (!Build.IS_GLOBAL_BUILD) {
            for (String ntpServer2 : chinaNtpServers) {
                this.mNtpServers.add(ntpServer2);
            }
        }
        Log.d(TAG, "the servers are " + this.mNtpServers);
    }

    public boolean switchNtpServer(int tryCounter, NtpTrustedTime trustedTime) {
        if (!refreshNtpServer(tryCounter)) {
            return this.mTime.forceRefresh();
        }
        return false;
    }

    private void initDefaultNtpServer(Context context) {
        if (context == null) {
            return;
        }
        this.mContext = context;
        ContentResolver resolver = context.getContentResolver();
        Resources res = this.mContext.getResources();
        String defaultServer = res.getString(17040033);
        String secureServer = Settings.Global.getString(resolver, "ntp_server");
        if (Build.IS_GLOBAL_BUILD) {
            this.mDefaultNtpServer = secureServer != null ? secureServer : defaultServer;
        } else {
            this.mDefaultNtpServer = CN_NTP_SERVER;
        }
    }

    private void setNtpServer(String ntpServer) {
        Context context = this.mContext;
        if (context != null) {
            Settings.Global.putString(context.getContentResolver(), "ntp_server", ntpServer);
        }
    }

    private boolean refreshNtpServer(int tryCounter) {
        int index = tryCounter % this.mNtpServers.size();
        String ntpServer = this.mNtpServers.get(index);
        Log.d(TAG, "tryCounter = " + tryCounter + ",ntpServers = " + ntpServer);
        setNtpServer(ntpServer);
        boolean result = this.mTime.forceRefresh();
        setNtpServer(this.mDefaultNtpServer);
        return result;
    }
}
