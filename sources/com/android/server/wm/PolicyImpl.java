package com.android.server.wm;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MiuiSettings;
import android.util.Slog;
import com.android.server.MiuiBgThread;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
/* loaded from: classes.dex */
public abstract class PolicyImpl {
    private static final String AUTHORITY = "com.miui.android.sm.policy";
    private static final Uri AUTHORITY_URI;
    private static final int INDEX_CONFIGURATION_NAME = 3;
    private static final int INDEX_CONFIGURATION_VALUE = 4;
    private static final int INDEX_PACKAGE_NAME = 2;
    private static final String POLICY_ITEM = "policy_item";
    private static final String POLICY_LIST = "policy_list";
    private static final String POLICY_LIST_SELECTION = "policyName=?";
    private static final Uri POLICY_LIST_URI;
    private static final int RETRY_NUMBER = 3;
    private static final long RETRY_TIME_OUT = 600000;
    private static final String TAG = "PolicyImpl";
    private final ContentResolver mContentResolver;
    private final Context mContext;
    private final PackageConfigurationController mController;
    private List<PackageConfiguration> mList;
    private final String mName;
    private PolicyItem mPolicyItem;
    private int mRetryNumber = 3;
    private static final String[] POLICY_LIST_PROJECTION = {"policyVersion"};
    private static final Map<String, List<Consumer<ConcurrentHashMap<String, String>>>> CALLBACKS = new ConcurrentHashMap();

    abstract int getLocalVersion();

    abstract void getPolicyDataMapFromCloud(String str, ConcurrentHashMap<String, String> concurrentHashMap);

    abstract void getPolicyDataMapFromLocal(String str, ConcurrentHashMap<String, String> concurrentHashMap);

    static {
        Uri parse = Uri.parse(AUTHORITY);
        AUTHORITY_URI = parse;
        POLICY_LIST_URI = Uri.withAppendedPath(parse, POLICY_LIST);
    }

    public PolicyImpl(PackageConfigurationController controller, String policyName) {
        this.mController = controller;
        this.mName = policyName;
        Context context = controller.mAtmService.mContext;
        this.mContext = context;
        this.mContentResolver = context.getContentResolver();
    }

    public String getPolicyName() {
        return this.mName;
    }

    public void executeDebugModeLocked(PrintWriter pw, String[] pkgNames, String value, String configurationName) {
        if (CALLBACKS.isEmpty()) {
            pw.println("Can not execute, There is no registered callback.");
        } else if (this.mController.mPolicyDisabled) {
            pw.println("Policy is disabled.");
        } else if (isDisabledConfiguration(configurationName)) {
            pw.println("Configuration is disabled.");
        } else {
            ConcurrentHashMap<String, String> tmpPolicyDataMap = new ConcurrentHashMap<>();
            List<String> tmpPolicyRemoveList = new ArrayList<>();
            for (int i = 0; i < pkgNames.length; i++) {
                if (("fullscreenpackage".equals(configurationName) || "fullscreencomponent".equals(configurationName)) && (value.equals("--remove") || value.equals("0"))) {
                    tmpPolicyRemoveList.add(pkgNames[i]);
                } else {
                    tmpPolicyDataMap.put(pkgNames[i], value);
                }
            }
            Iterator<PackageConfiguration> it = this.mList.iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                PackageConfiguration pkgConfig = it.next();
                if (pkgConfig.mName.equals(configurationName)) {
                    ConcurrentHashMap<String, String> dataMap = pkgConfig.getPolicyDataMap();
                    for (String name : tmpPolicyRemoveList) {
                        dataMap.put(name, "-1");
                    }
                    dataMap.putAll(tmpPolicyDataMap);
                }
            }
            this.mPolicyItem.setCurrentVersion("Modified");
            propagateToCallbacks();
        }
    }

    public boolean executeShellCommandLocked(String command, String[] args, PrintWriter pw) {
        return false;
    }

    private void registerDataObserver() {
        updateDataMapFromCloud();
        this.mContentResolver.registerContentObserver(MiuiSettings.SettingsCloudData.getCloudDataNotifyUri(), true, new ContentObserver(MiuiBgThread.getHandler()) { // from class: com.android.server.wm.PolicyImpl.1
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                Slog.w(PolicyImpl.TAG, "AppContinuity policyDataMapFromCloud onChange--");
                PolicyImpl.this.updateDataMapFromCloud();
            }
        });
    }

    void updateDataMapFromCloud() {
        Map<String, List<Consumer<ConcurrentHashMap<String, String>>>> map = CALLBACKS;
        if (map.isEmpty()) {
            return;
        }
        HashSet<String> callbackNames = new HashSet<>(map.keySet());
        if (this.mPolicyItem == null) {
            this.mPolicyItem = new PolicyItem(callbackNames);
        }
        this.mList = this.mPolicyItem.getPackageConfigurationList();
        this.mPolicyItem.setLocalVersion(getLocalVersion());
        this.mList.forEach(new Consumer() { // from class: com.android.server.wm.PolicyImpl$$ExternalSyntheticLambda6
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                PolicyImpl.this.m1873lambda$updateDataMapFromCloud$0$comandroidserverwmPolicyImpl((PackageConfiguration) obj);
            }
        });
        this.mPolicyItem.setCurrentVersion(this.mPolicyItem.getLocalVersion() + "(CLOUD)");
        propagateToCallbacks();
    }

    /* renamed from: lambda$updateDataMapFromCloud$0$com-android-server-wm-PolicyImpl */
    public /* synthetic */ void m1873lambda$updateDataMapFromCloud$0$comandroidserverwmPolicyImpl(PackageConfiguration pkgConfig) {
        ConcurrentHashMap<String, String> policyMap = pkgConfig.getPolicyDataMap();
        getPolicyDataMapFromCloud(pkgConfig.mName, policyMap);
    }

    int getScpmVersionFromQuery() {
        Cursor cursor = this.mContentResolver.query(POLICY_LIST_URI, POLICY_LIST_PROJECTION, POLICY_LIST_SELECTION, new String[]{this.mName}, null);
        if (cursor == null) {
            return 0;
        }
        if (cursor.getCount() <= 0) {
            cursor.close();
            return 0;
        }
        cursor.moveToNext();
        String cursorValue = cursor.getString(0);
        cursor.close();
        return Integer.parseInt(cursorValue);
    }

    public void init() {
        Map<String, List<Consumer<ConcurrentHashMap<String, String>>>> map = CALLBACKS;
        if (map.isEmpty()) {
            return;
        }
        HashSet<String> callbackNames = new HashSet<>(map.keySet());
        if (this.mPolicyItem == null) {
            this.mPolicyItem = new PolicyItem(callbackNames);
        }
        this.mList = this.mPolicyItem.getPackageConfigurationList();
        this.mPolicyItem.setLocalVersion(getLocalVersion());
        int version = this.mPolicyItem.getScpmVersion() < this.mPolicyItem.getLocalVersion() ? 1 : 0;
        if (version != 0) {
            this.mList.forEach(new Consumer() { // from class: com.android.server.wm.PolicyImpl$$ExternalSyntheticLambda5
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    PolicyImpl.this.m1871lambda$init$1$comandroidserverwmPolicyImpl((PackageConfiguration) obj);
                }
            });
            this.mPolicyItem.setCurrentVersion(this.mPolicyItem.getLocalVersion() + "(LOCAL)");
        }
        propagateToCallbacks();
        registerDataObserver();
    }

    /* renamed from: lambda$init$1$com-android-server-wm-PolicyImpl */
    public /* synthetic */ void m1871lambda$init$1$comandroidserverwmPolicyImpl(PackageConfiguration pkgConfig) {
        ConcurrentHashMap<String, String> policyMap = pkgConfig.getPolicyDataMap();
        getPolicyDataMapFromLocal(pkgConfig.mName, policyMap);
    }

    boolean isDisabledConfiguration(String configurationName) {
        return false;
    }

    public void propagateToCallbacks() {
        this.mList.forEach(new Consumer() { // from class: com.android.server.wm.PolicyImpl$$ExternalSyntheticLambda4
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                PolicyImpl.this.m1872lambda$propagateToCallbacks$3$comandroidserverwmPolicyImpl((PackageConfiguration) obj);
            }
        });
    }

    /* renamed from: lambda$propagateToCallbacks$3$com-android-server-wm-PolicyImpl */
    public /* synthetic */ void m1872lambda$propagateToCallbacks$3$comandroidserverwmPolicyImpl(PackageConfiguration pkgConfig) {
        final List<Consumer<ConcurrentHashMap<String, String>>> consumerMap = CALLBACKS.get(pkgConfig.mName);
        if (consumerMap == null) {
            return;
        }
        final ConcurrentHashMap<String, String> policyMap = new ConcurrentHashMap<>();
        if (!this.mController.mPolicyDisabled && !isDisabledConfiguration(pkgConfig.mName)) {
            policyMap.putAll(pkgConfig.getPolicyDataMap());
        }
        this.mController.mAtmService.mH.post(new Runnable() { // from class: com.android.server.wm.PolicyImpl$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                PolicyImpl.lambda$propagateToCallbacks$2(consumerMap, policyMap);
            }
        });
    }

    public static /* synthetic */ void lambda$propagateToCallbacks$2(List consumerMap, ConcurrentHashMap policyMap) {
        for (int i = 0; i < consumerMap.size(); i++) {
            try {
                Consumer<ConcurrentHashMap<String, String>> consumer = (Consumer) consumerMap.get(i);
                consumer.accept(policyMap);
            } catch (Exception e) {
                Slog.d(TAG, "propagateToCallbacks", e);
                return;
            }
        }
    }

    public void registerCallback(String callbackName, Consumer<ConcurrentHashMap<String, String>> consumer) {
        Map<String, List<Consumer<ConcurrentHashMap<String, String>>>> map = CALLBACKS;
        List<Consumer<ConcurrentHashMap<String, String>>> callbackList = map.get(callbackName);
        if (callbackList == null) {
            callbackList = new ArrayList();
        }
        callbackList.add(consumer);
        map.put(callbackName, callbackList);
    }

    boolean updatePackageConfigurationsIfNeeded() {
        boolean updated = false;
        int localVersion = this.mPolicyItem.getLocalVersion();
        int scpmVersion = this.mPolicyItem.getScpmVersion();
        int scpmVersionFromQuery = getScpmVersionFromQuery();
        if (scpmVersionFromQuery == 0) {
            int i = this.mRetryNumber - 1;
            this.mRetryNumber = i;
            if (i > 0) {
                this.mController.scheduleUpdatePolicyItem(this.mName, RETRY_TIME_OUT);
                return false;
            }
        }
        if (scpmVersionFromQuery > scpmVersion && scpmVersionFromQuery > localVersion) {
            int size = this.mList.size();
            StringBuilder selections = new StringBuilder();
            String[] selectionArgs = new String[size];
            for (int i2 = 0; i2 < size; i2++) {
                if (i2 > 0) {
                    selections.append(" or ");
                }
                selections.append("data1=?");
                selectionArgs[i2] = this.mList.get(i2).mName;
            }
            Uri policyItemUri = Uri.withAppendedPath(Uri.withAppendedPath(AUTHORITY_URI, POLICY_ITEM), this.mName);
            Cursor cursor = this.mContentResolver.query(policyItemUri, null, selections.toString(), selectionArgs, null);
            if (cursor != null) {
                try {
                    this.mList.forEach(new Consumer() { // from class: com.android.server.wm.PolicyImpl$$ExternalSyntheticLambda1
                        @Override // java.util.function.Consumer
                        public final void accept(Object obj) {
                            ((PackageConfiguration) obj).updatePrepared();
                        }
                    });
                    while (cursor.moveToNext()) {
                        final String packageName = cursor.getString(2);
                        final String key = cursor.getString(3);
                        final String value = cursor.getString(4);
                        this.mList.forEach(new Consumer() { // from class: com.android.server.wm.PolicyImpl$$ExternalSyntheticLambda2
                            @Override // java.util.function.Consumer
                            public final void accept(Object obj) {
                                PolicyImpl.lambda$updatePackageConfigurationsIfNeeded$5(key, packageName, value, (PackageConfiguration) obj);
                            }
                        });
                    }
                    this.mList.forEach(new Consumer() { // from class: com.android.server.wm.PolicyImpl$$ExternalSyntheticLambda3
                        @Override // java.util.function.Consumer
                        public final void accept(Object obj) {
                            ((PackageConfiguration) obj).updateCompleted();
                        }
                    });
                    this.mPolicyItem.setScpmVersion(scpmVersionFromQuery);
                    this.mPolicyItem.setCurrentVersion(scpmVersionFromQuery + "(SCPM)");
                    updated = true;
                    propagateToCallbacks();
                } catch (Throwable th) {
                    if (cursor != null) {
                        try {
                            cursor.close();
                        } catch (Throwable th2) {
                            th.addSuppressed(th2);
                        }
                    }
                    throw th;
                }
            }
            if (cursor != null) {
                cursor.close();
            }
        }
        return updated;
    }

    public static /* synthetic */ void lambda$updatePackageConfigurationsIfNeeded$5(String key, String packageName, String value, PackageConfiguration pkgConfig) {
        if (pkgConfig.mName.equals(key)) {
            pkgConfig.updateFromScpm(packageName, value);
        }
    }

    public void updatePolicyItem(boolean forced) {
        if (CALLBACKS.isEmpty()) {
            return;
        }
        if (forced) {
            this.mPolicyItem.setScpmVersion(0);
        }
        updatePackageConfigurationsIfNeeded();
    }
}
