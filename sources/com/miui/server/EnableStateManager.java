package com.miui.server;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.SystemProperties;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import miui.os.Build;
import org.json.JSONArray;
import org.json.JSONObject;
/* loaded from: classes.dex */
public class EnableStateManager {
    public static final String HAS_EVER_ENABLED = "com.xiaomi.market.hasEverEnabled_";
    public static final String HAS_UPDATE_APPLICATION_STATE = "has_update_application_state";
    public static final String LAST_REGION = "com.xiaomi.market.lastRegion";
    private static Context mContext;
    private static final String TAG = EnableStateManager.class.getSimpleName();
    private static Map<String, List<String>> mCloudEnableSettings = new HashMap();
    private static List<String> sEnableStateControlledPkgList = new ArrayList();
    private static List<String> sShouldKeepStatePackages = new ArrayList();
    private static boolean receiverRegistered = false;

    private EnableStateManager() {
    }

    public static void updateApplicationEnableState(Context context) {
        if (!Build.IS_INTERNATIONAL_BUILD) {
            return;
        }
        if (mContext == null) {
            if (context != null) {
                mContext = context;
            } else {
                Log.i(TAG, "no context");
                return;
            }
        }
        Settings.System.putString(mContext.getContentResolver(), HAS_UPDATE_APPLICATION_STATE, "true");
        updateApplicationEnableStateInner(false);
        registerReceiverIfNeed();
    }

    /* JADX WARN: Type inference failed for: r0v2, types: [com.miui.server.EnableStateManager$1] */
    private static void registerReceiverIfNeed() {
        if (receiverRegistered) {
            return;
        }
        receiverRegistered = true;
        new Thread() { // from class: com.miui.server.EnableStateManager.1
            @Override // java.lang.Thread, java.lang.Runnable
            public void run() {
                IntentFilter filter = new IntentFilter();
                filter.addAction("android.intent.action.PACKAGE_ADDED");
                filter.addDataScheme("package");
                EnableStateManager.mContext.registerReceiver(new PackageAddedReceiver(), filter);
            }
        }.start();
    }

    private static void updateApplicationEnableStateInner(boolean shouldKeep) {
        Log.i(TAG, "updateConfigFromFile");
        updateConfigFromFile();
        for (String pkgName : sEnableStateControlledPkgList) {
            if (isAppInstalled(pkgName)) {
                updateEnableState(pkgName, shouldKeep);
            }
        }
    }

    /* loaded from: classes.dex */
    private static final class PackageAddedReceiver extends BroadcastReceiver {
        private PackageAddedReceiver() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            if (EnableStateManager.sEnableStateControlledPkgList.contains(EnableStateManager.getPackageName(intent))) {
                EnableStateManager.updateApplicationEnableState(context);
            }
        }
    }

    public static String getPackageName(Intent intent) {
        Uri uri;
        if (intent == null || (uri = intent.getData()) == null) {
            return null;
        }
        return uri.getSchemeSpecificPart();
    }

    private static boolean isAppInstalled(String pkgName) {
        PackageManager pm;
        try {
            pm = mContext.getPackageManager();
        } catch (Exception e) {
            Log.e(TAG, e.toString(), e);
        }
        if (pm.getApplicationInfo(pkgName, 0) == null) {
            return false;
        }
        return true;
    }

    private static void updateEnableState(String pkgName, boolean shouldKeep) {
        try {
            String region = Build.getRegion();
            String str = TAG;
            Log.d(str, "region: " + region);
            if (TextUtils.isEmpty(region)) {
                return;
            }
            String lastRegion = getString(LAST_REGION);
            if (!TextUtils.isEmpty(lastRegion) && !TextUtils.equals(lastRegion, region)) {
                shouldKeep = false;
            }
            Set<String> regionList = getEnableSettings(pkgName, shouldKeep);
            Log.d(str, "enable " + pkgName + " in " + regionList.toString());
            if (!regionList.contains(region) && !regionList.contains("all")) {
                tryDisablePkg(pkgName);
                setString(LAST_REGION, region);
            }
            tryEnablePkg(pkgName);
            setString(LAST_REGION, region);
        } catch (Exception e) {
            Log.e(TAG, e.toString(), e);
        }
    }

    private static void tryDisablePkg(String pkgName) {
        try {
            PackageManager pm = mContext.getPackageManager();
            int state = mContext.getPackageManager().getApplicationEnabledSetting(pkgName);
            if (state == 0 || state == 1) {
                pm.setApplicationEnabledSetting(pkgName, 2, 0);
            }
        } catch (Exception e) {
            Log.e(TAG, e.toString(), e);
        }
    }

    private static void tryEnablePkg(String pkgName) {
        try {
            PackageManager pm = mContext.getPackageManager();
            int state = mContext.getPackageManager().getApplicationEnabledSetting(pkgName);
            if (state == 2) {
                pm.setApplicationEnabledSetting(pkgName, 1, 0);
            }
        } catch (Exception e) {
            Log.e(TAG, e.toString(), e);
        }
    }

    private static Set<String> getEnableSettings(String pkgName, boolean shouldKeep) {
        Set<String> result = new HashSet<>();
        String region = Build.getRegion();
        List<String> regions = mCloudEnableSettings.get(pkgName);
        if (regions != null) {
            result.addAll(regions);
        }
        String[] apkPresetRegions = getStringArray(mContext, pkgName, "enable_regions");
        if (apkPresetRegions != null && apkPresetRegions.length > 0) {
            result.addAll(Arrays.asList(apkPresetRegions));
        }
        if (sShouldKeepStatePackages.contains(pkgName)) {
            Set<String> hasEnableRegions = getRegionSetByPkgName(HAS_EVER_ENABLED + pkgName);
            if (hasEnableRegions.size() > 0) {
                result.addAll(hasEnableRegions);
            }
            String str = TAG;
            Log.d(str, "shouldKeep: " + shouldKeep + "\n is " + pkgName + " enable " + isPackageEnabled(mContext, pkgName));
            if (shouldKeep && isPackageEnabled(mContext, pkgName)) {
                Log.d(str, "add " + pkgName + " at " + region);
                result.add(region);
            }
            setRegionSetByPkgName(HAS_EVER_ENABLED + pkgName, result);
        }
        return result;
    }

    private static String[] getStringArray(Context context, String pkgName, String resName) {
        try {
            Context fContext = context.createPackageContext(pkgName, 0);
            int id = fContext.getResources().getIdentifier(resName, "array", pkgName);
            return fContext.getResources().getStringArray(id);
        } catch (Exception e) {
            Log.e(TAG, e.toString(), e);
            return new String[0];
        }
    }

    private static void updateConfigFromFile() {
        mCloudEnableSettings.clear();
        sEnableStateControlledPkgList.clear();
        sShouldKeepStatePackages.clear();
        BufferedReader reader = null;
        try {
            try {
                try {
                    reader = new BufferedReader(new InputStreamReader(mContext.getResources().openRawResource(286130178)));
                    StringBuilder sb = new StringBuilder();
                    while (true) {
                        String temp = reader.readLine();
                        if (temp == null) {
                            break;
                        }
                        sb.append(temp);
                    }
                    JSONObject json = new JSONObject(sb.toString());
                    putCustomizedJSONObject(json);
                    Iterator<String> pkgNameList = json.keys();
                    while (pkgNameList.hasNext()) {
                        String pkgName = pkgNameList.next();
                        JSONObject settingJson = json.optJSONObject(pkgName);
                        if (settingJson != null) {
                            List<String> enableRegionList = new ArrayList<>();
                            String rsa4 = SystemProperties.get("ro.com.miui.rsa.feature", "");
                            if (TextUtils.isEmpty(rsa4)) {
                                String rsa = SystemProperties.get("ro.com.miui.rsa", "");
                                JSONObject rsaEnableObject = settingJson.optJSONObject("rsa_enable_list");
                                getEnableRegionListFromRSA(rsaEnableObject, rsa, enableRegionList);
                            } else {
                                getEnableRegionListFromRSA(settingJson.optJSONObject("rsa4_enable_list"), rsa4, enableRegionList);
                            }
                            JSONArray enableRegionArray = settingJson.optJSONArray("enable_list");
                            if (enableRegionArray != null) {
                                for (int index = 0; index < enableRegionArray.length(); index++) {
                                    String region = enableRegionArray.optString(index);
                                    if (!enableRegionList.contains(region)) {
                                        enableRegionList.add(region);
                                    }
                                }
                            }
                            mCloudEnableSettings.put(pkgName, enableRegionList);
                            boolean shouldKeep = settingJson.optBoolean("shouldKeep", false);
                            if (shouldKeep) {
                                sShouldKeepStatePackages.add(pkgName);
                            }
                        }
                        sEnableStateControlledPkgList.add(pkgName);
                    }
                    reader.close();
                } catch (Exception e) {
                    Log.e(TAG, e.toString(), e);
                    reader.close();
                }
            } catch (Exception e2) {
                Log.e(TAG, e2.toString(), e2);
            }
        } catch (Throwable th) {
            try {
                reader.close();
            } catch (Exception e3) {
                Log.e(TAG, e3.toString(), e3);
            }
            throw th;
        }
    }

    private static void getEnableRegionListFromRSA(JSONObject rsaEnableObject, String systemTier, List<String> enableRegionList) {
        if (rsaEnableObject == null || TextUtils.isEmpty(systemTier)) {
            return;
        }
        Iterator<String> rsaList = rsaEnableObject.keys();
        while (rsaList.hasNext()) {
            String rsa = rsaList.next();
            if (TextUtils.equals(rsa, systemTier) || (!TextUtils.isEmpty(systemTier) && TextUtils.equals(rsa, "tier_all"))) {
                JSONArray rsaRegionArray = rsaEnableObject.optJSONArray(rsa);
                if (rsaRegionArray == null) {
                    return;
                }
                for (int index = 0; index < rsaRegionArray.length(); index++) {
                    String region = rsaRegionArray.optString(index);
                    if (!enableRegionList.contains(region)) {
                        enableRegionList.add(region);
                    }
                }
            }
        }
    }

    private static boolean isPackageEnabled(Context context, String packageName) {
        try {
            PackageManager pm = context.getPackageManager();
            int state = pm.getApplicationEnabledSetting(packageName);
            Log.d(TAG, packageName + " state is " + state);
            switch (state) {
                case 0:
                case 2:
                case 3:
                case 4:
                    return pm.getApplicationInfo(packageName, 0).enabled;
                case 1:
                    return true;
                default:
                    return false;
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            return true;
        }
    }

    private static void setString(String key, String value) {
        Settings.System.putString(mContext.getContentResolver(), key, value);
    }

    private static String getString(String key) {
        return Settings.System.getString(mContext.getContentResolver(), key);
    }

    public static void setRegionSetByPkgName(String packageName, Set<String> regions) {
        String tempResult = "";
        Iterator<String> iter = regions.iterator();
        if (iter == null) {
            return;
        }
        while (iter.hasNext()) {
            if (TextUtils.isEmpty(tempResult)) {
                tempResult = tempResult + iter.next();
            } else {
                tempResult = tempResult + ";" + iter.next();
            }
        }
        Log.d(TAG, "setRegionSetByPkgName: " + tempResult);
        Settings.System.putString(mContext.getContentResolver(), packageName, tempResult);
    }

    public static Set<String> getRegionSetByPkgName(String packageName) {
        String hasEnableRegions = Settings.System.getString(mContext.getContentResolver(), packageName);
        Log.d(TAG, "getRegionSetByPkgName: " + hasEnableRegions);
        if (TextUtils.isEmpty(hasEnableRegions)) {
            return new HashSet();
        }
        String[] hasEnableRegionList = hasEnableRegions.split(";");
        Set<String> hasEnableRegionSet = new HashSet<>();
        for (String hasEnableRegion : hasEnableRegionList) {
            hasEnableRegionSet.add(hasEnableRegion);
        }
        return hasEnableRegionSet;
    }

    private static void putCustomizedJSONObject(JSONObject jsonObject) {
        String region = SystemProperties.get("ro.miui.customized.region", "");
        if (!TextUtils.isEmpty(region)) {
            BufferedReader reader = null;
            try {
                try {
                    try {
                        reader = new BufferedReader(new InputStreamReader(mContext.getResources().openRawResource(286130195)));
                        StringBuilder sb = new StringBuilder();
                        while (true) {
                            String temp = reader.readLine();
                            if (temp == null) {
                                break;
                            }
                            sb.append(temp);
                        }
                        JSONObject json = new JSONObject(sb.toString());
                        Iterator<String> pkgNameList = json.keys();
                        while (pkgNameList.hasNext()) {
                            String pkgName = pkgNameList.next();
                            Log.d(TAG, "op_enable_list pkgName: " + pkgName);
                            JSONObject settingJson = json.getJSONObject(pkgName);
                            jsonObject.putOpt(pkgName, settingJson);
                        }
                        reader.close();
                    } catch (Exception e) {
                        Log.e(TAG, e.toString(), e);
                        reader.close();
                    }
                } catch (Throwable th) {
                    try {
                        reader.close();
                    } catch (Exception e2) {
                        Log.e(TAG, e2.toString(), e2);
                    }
                    throw th;
                }
            } catch (Exception e3) {
                Log.e(TAG, e3.toString(), e3);
            }
        }
    }
}
