package com.android.server;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerCompat;
import android.content.pm.UserInfo;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.text.TextUtils;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
/* loaded from: classes.dex */
public class CloudUtils {
    private static final String BLACK_LIST = "cloud_slave_wifi_only_blacklist";
    private final Context mContext;
    private final Handler mHandler;
    private String mLocalBlackListPackageNames = "com.android.htmlviewer,com.tencent.cmocmna";
    private Object mLock = new Object();
    private Set<Integer> mBlacklistUids = new HashSet();
    private Set<String> mBlacklistPackageNames = new HashSet();

    public CloudUtils(Context context, String tag) {
        this.mContext = context;
        HandlerThread thread = new HandlerThread(tag);
        thread.start();
        this.mHandler = new Handler(thread.getLooper());
        registerSlaveOnlyBlacklistChangedObserver();
        initBroadcastReceiver();
    }

    public boolean isUidInSlaveOnlyBlackList(int uid) {
        Set<Integer> set = this.mBlacklistUids;
        if (set == null || set.isEmpty()) {
            return false;
        }
        return this.mBlacklistUids.contains(Integer.valueOf(uid));
    }

    private void initBroadcastReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.PACKAGE_ADDED");
        intentFilter.addAction("android.intent.action.PACKAGE_REMOVED");
        intentFilter.addDataScheme("package");
        BroadcastReceiver receiver = new BroadcastReceiver() { // from class: com.android.server.CloudUtils.1
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                String packageName = intent.getData().getSchemeSpecificPart();
                if (TextUtils.isEmpty(packageName)) {
                    return;
                }
                synchronized (CloudUtils.this.mLock) {
                    if (CloudUtils.this.mBlacklistPackageNames.contains(packageName)) {
                        int uid = intent.getIntExtra("android.intent.extra.UID", -1);
                        if (intent.getAction().equals("android.intent.action.PACKAGE_ADDED")) {
                            CloudUtils.this.mBlacklistUids.add(Integer.valueOf(uid));
                        } else if (intent.getAction().equals("android.intent.action.PACKAGE_REMOVED")) {
                            CloudUtils.this.mBlacklistUids.remove(Integer.valueOf(uid));
                        }
                    }
                }
            }
        };
        this.mContext.registerReceiver(receiver, intentFilter);
    }

    private void registerSlaveOnlyBlacklistChangedObserver() {
        final ContentObserver observer = new ContentObserver(Handler.getMain()) { // from class: com.android.server.CloudUtils.2
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                UserManager um = (UserManager) CloudUtils.this.mContext.getSystemService("user");
                PackageManager pm = CloudUtils.this.mContext.getPackageManager();
                List<UserInfo> users = um.getUsers();
                synchronized (CloudUtils.this.mLock) {
                    CloudUtils cloudUtils = CloudUtils.this;
                    cloudUtils.mBlacklistPackageNames = cloudUtils.getBlackListPackageNames(cloudUtils.mContext);
                    CloudUtils.this.mBlacklistUids.clear();
                    if (!CloudUtils.this.mBlacklistPackageNames.isEmpty()) {
                        for (UserInfo user : users) {
                            List<PackageInfo> apps = PackageManagerCompat.getInstalledPackagesAsUser(pm, 0, user.id);
                            for (PackageInfo app : apps) {
                                if (app.packageName != null && app.applicationInfo != null && CloudUtils.this.mBlacklistPackageNames.contains(app.packageName)) {
                                    int uid = UserHandle.getUid(user.id, app.applicationInfo.uid);
                                    CloudUtils.this.mBlacklistUids.add(Integer.valueOf(uid));
                                }
                            }
                        }
                    }
                }
            }
        };
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(BLACK_LIST), false, observer, -2);
        this.mHandler.post(new Runnable() { // from class: com.android.server.CloudUtils.3
            @Override // java.lang.Runnable
            public void run() {
                observer.onChange(false);
            }
        });
    }

    public Set<String> getBlackListPackageNames(Context context) {
        String[] packages;
        String blacklistString = Settings.System.getStringForUser(context.getContentResolver(), BLACK_LIST, -2);
        if (blacklistString == null || TextUtils.isEmpty(blacklistString)) {
            blacklistString = this.mLocalBlackListPackageNames;
        }
        Set<String> blacklist = new HashSet<>();
        if (!TextUtils.isEmpty(blacklistString) && (packages = blacklistString.split(",")) != null) {
            for (String str : packages) {
                blacklist.add(str);
            }
        }
        return blacklist;
    }
}
