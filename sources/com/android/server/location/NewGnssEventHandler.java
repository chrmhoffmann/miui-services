package com.android.server.location;

import android.content.Context;
import android.location.LocationManager;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;
/* loaded from: classes.dex */
public class NewGnssEventHandler {
    private static final String TAG = "GnssNps";
    private static NewGnssEventHandler mInstance;
    private Context mAppContext;
    private NewNotifyManager mNotifyManager;
    private List<String> packageNameListNavNow = new ArrayList();
    LocationManager locationManager = null;

    public static synchronized NewGnssEventHandler getInstance(Context context) {
        NewGnssEventHandler newGnssEventHandler;
        synchronized (NewGnssEventHandler.class) {
            if (mInstance == null) {
                mInstance = new NewGnssEventHandler(context);
            }
            newGnssEventHandler = mInstance;
        }
        return newGnssEventHandler;
    }

    private NewGnssEventHandler(Context context) {
        this.mAppContext = context;
        this.mNotifyManager = new NewNotifyManager(context);
    }

    public synchronized void handleUpdateGnssStatus() {
        boolean gnssEnable = getGnssEnableStatus();
        Log.d(TAG, "gnss status update: " + gnssEnable);
        if (gnssEnable) {
            if (this.packageNameListNavNow.size() > 0) {
                String lastForegroundNavApp = getLastForegroundNavApp();
                if (lastForegroundNavApp != null) {
                    this.mNotifyManager.showNavigationNotification(lastForegroundNavApp);
                } else {
                    NewNotifyManager newNotifyManager = this.mNotifyManager;
                    List<String> list = this.packageNameListNavNow;
                    newNotifyManager.showNavigationNotification(list.get(list.size() - 1));
                }
            }
        } else {
            this.mNotifyManager.removeNotification();
        }
    }

    public synchronized void handleStart(String packageName) {
        this.packageNameListNavNow.add(packageName);
        if (getGnssEnableStatus()) {
            this.mNotifyManager.showNavigationNotification(packageName);
        }
    }

    public synchronized void handleStop(String packageName) {
        if (this.mNotifyManager != null && this.packageNameListNavNow.contains(packageName)) {
            this.packageNameListNavNow.remove(packageName);
            if (this.packageNameListNavNow.size() != 0 && getGnssEnableStatus()) {
                if (this.packageNameListNavNow.size() != 1 && moreThanOneNavApp()) {
                    String lastForegroundNavApp = getLastForegroundNavApp();
                    if (lastForegroundNavApp != null) {
                        this.mNotifyManager.showNavigationNotification(lastForegroundNavApp);
                    } else {
                        NewNotifyManager newNotifyManager = this.mNotifyManager;
                        List<String> list = this.packageNameListNavNow;
                        newNotifyManager.showNavigationNotification(list.get(list.size() - 1));
                    }
                }
                this.mNotifyManager.showNavigationNotification(this.packageNameListNavNow.get(0));
                return;
            }
            this.mNotifyManager.removeNotification();
        }
    }

    public synchronized void handlerUpdateFixStatus(boolean fixing) {
        if (fixing) {
            this.mNotifyManager.removeNotification();
        } else if (getGnssEnableStatus() && this.packageNameListNavNow.size() > 0) {
            String lastForegroundNavApp = getLastForegroundNavApp();
            if (lastForegroundNavApp != null) {
                this.mNotifyManager.showNavigationNotification(lastForegroundNavApp);
            } else {
                NewNotifyManager newNotifyManager = this.mNotifyManager;
                List<String> list = this.packageNameListNavNow;
                newNotifyManager.showNavigationNotification(list.get(list.size() - 1));
            }
        }
    }

    public boolean moreThanOneNavApp() {
        if (this.packageNameListNavNow.size() <= 1) {
            return false;
        }
        String appNameFirst = this.packageNameListNavNow.get(0);
        for (int i = 1; i < this.packageNameListNavNow.size(); i++) {
            if (this.packageNameListNavNow.get(i).hashCode() != appNameFirst.hashCode()) {
                return true;
            }
        }
        return false;
    }

    public String getLastForegroundNavApp() {
        return null;
    }

    private boolean getGnssEnableStatus() {
        Context context = this.mAppContext;
        if (context == null) {
            return false;
        }
        if (this.locationManager == null) {
            this.locationManager = (LocationManager) context.getSystemService("location");
        }
        return this.locationManager.isProviderEnabled("gps");
    }
}
