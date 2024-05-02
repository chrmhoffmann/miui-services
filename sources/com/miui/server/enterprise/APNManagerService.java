package com.miui.server.enterprise;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.UserHandle;
import android.provider.Telephony;
import android.telephony.SubscriptionInfo;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Slog;
import com.android.server.display.TemperatureController;
import com.miui.enterprise.IAPNManager;
import com.miui.enterprise.sdk.APNConfig;
import com.miui.enterprise.settings.EnterpriseSettings;
import com.miui.server.input.util.MiuiCustomizeShortCutUtils;
import java.util.ArrayList;
import java.util.List;
import miui.telephony.SubscriptionManager;
import miui.telephony.TelephonyConstants;
/* loaded from: classes.dex */
public class APNManagerService extends IAPNManager.Stub {
    private static final String TAG = "APNManager";
    private Context mContext;
    private TelephonyManager mTelephonyManager;
    private static final Uri PREFERAPN_URI = Uri.parse("content://telephony/carriers/preferapn");
    private static final Uri DEFAULTAPN_URI = Uri.parse("content://telephony/carriers/restore");

    public APNManagerService(Context context) {
        this.mContext = context;
        this.mTelephonyManager = (TelephonyManager) context.getSystemService("phone");
    }

    private ContentResolver getContentResolver() {
        return this.mContext.getContentResolverForUser(new UserHandle(0));
    }

    private String buildNumericAndNameSelection(String numeric, String name) {
        return buildNumericSelection(numeric) + " and " + buildNameSelection(name);
    }

    private String buildNumericSelection(String numeric) {
        return "numeric=\"" + numeric + "\"";
    }

    private String buildNameSelection(String name) {
        return "name=\"" + name + "\"";
    }

    private String getNumeric() {
        int subId = SubscriptionManager.getDefault().getDefaultDataSubscriptionId();
        SubscriptionInfo subscriptionInfo = android.telephony.SubscriptionManager.from(this.mContext).getActiveSubscriptionInfo(subId);
        String mccmnc = subscriptionInfo == null ? "" : this.mTelephonyManager.getSimOperator(subscriptionInfo.getSubscriptionId());
        return TelephonyManager.getTelephonyProperty(SubscriptionManager.getDefault().getDefaultDataSlotId(), TelephonyConstants.PROPERTY_APN_SIM_OPERATOR_NUMERIC, mccmnc);
    }

    private void closeCursor(Cursor cursor) {
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
    }

    public List<APNConfig> getAPNListForNumeric(String numeric) {
        ServiceUtils.checkPermission(this.mContext);
        Cursor cursor = getContentResolver().query(Telephony.Carriers.CONTENT_URI, new String[]{TemperatureController.STRATEGY_NAME, "apn", "user", "password", "authtype", "bearer"}, buildNumericSelection(numeric), null, "name ASC");
        List<APNConfig> configs = new ArrayList<>();
        if (cursor != null) {
            try {
                Slog.d(TAG, "Query result size: " + cursor.getCount());
                cursor.moveToFirst();
                while (!cursor.isAfterLast()) {
                    APNConfig configItem = new APNConfig();
                    configItem.mName = cursor.getString(0);
                    configItem.mApn = cursor.getString(1);
                    configItem.mUser = cursor.getString(2);
                    configItem.mPassword = cursor.getString(3);
                    configItem.mAuthType = cursor.getInt(4);
                    configItem.mBearer = cursor.getInt(5);
                    configs.add(configItem);
                    cursor.moveToNext();
                }
            } finally {
                closeCursor(cursor);
            }
        }
        return configs;
    }

    public List<APNConfig> getAPNList() {
        ServiceUtils.checkPermission(this.mContext);
        String numeric = getNumeric();
        Cursor cursor = getContentResolver().query(Telephony.Carriers.CONTENT_URI, new String[]{TemperatureController.STRATEGY_NAME, "apn", "user", "password", "authtype", "bearer", "mcc", "mnc", "numeric", "carrier_enabled", "current", "mmsc", "mmsport", "mmsproxy", "mvno_match_data", "mvno_type", "port", "protocol", "proxy", "roaming_protocol", "server", "sub_id", MiuiCustomizeShortCutUtils.ATTRIBUTE_TYPE}, buildNumericSelection(numeric), null, "name ASC");
        List<APNConfig> configs = new ArrayList<>();
        if (cursor != null) {
            try {
                Slog.d(TAG, "Query result size: " + cursor.getCount());
                cursor.moveToFirst();
                while (!cursor.isAfterLast()) {
                    APNConfig configItem = new APNConfig();
                    configItem.mName = cursor.getString(0);
                    configItem.mApn = cursor.getString(1);
                    configItem.mUser = cursor.getString(2);
                    configItem.mPassword = cursor.getString(3);
                    configItem.mAuthType = cursor.getInt(4);
                    configItem.mBearer = cursor.getInt(5);
                    configItem.mMcc = cursor.getString(6);
                    configItem.mMnc = cursor.getString(7);
                    configItem.mNumeric = cursor.getString(8);
                    configItem.mCarrier_enabled = cursor.getInt(9);
                    configItem.mCurrent = cursor.getInt(10);
                    configItem.mMmsc = cursor.getString(11);
                    configItem.mMmsport = cursor.getString(12);
                    configItem.mMmsproxy = cursor.getString(13);
                    configItem.mMvno_match_data = cursor.getString(14);
                    configItem.mMvno_type = cursor.getString(15);
                    configItem.mPort = cursor.getString(16);
                    configItem.mProtocol = cursor.getString(17);
                    configItem.mProxy = cursor.getString(18);
                    configItem.mRoaming_protocol = cursor.getString(19);
                    configItem.mServer = cursor.getString(20);
                    configItem.mSub_id = cursor.getString(21);
                    configItem.mType = cursor.getString(22);
                    configs.add(configItem);
                    cursor.moveToNext();
                }
            } finally {
                closeCursor(cursor);
            }
        }
        return configs;
    }

    public APNConfig getAPNForNumeric(String numeric, String name) {
        ServiceUtils.checkPermission(this.mContext);
        Cursor cursor = getContentResolver().query(Telephony.Carriers.CONTENT_URI, new String[]{TemperatureController.STRATEGY_NAME, "apn", "user", "password", "authtype", "bearer"}, buildNumericAndNameSelection(numeric, name), null, "name ASC");
        if (cursor != null) {
            try {
                if (cursor.getCount() > 0) {
                    cursor.moveToFirst();
                    APNConfig configItem = new APNConfig();
                    configItem.mName = cursor.getString(0);
                    configItem.mApn = cursor.getString(1);
                    configItem.mUser = cursor.getString(2);
                    configItem.mPassword = cursor.getString(3);
                    configItem.mAuthType = cursor.getInt(4);
                    configItem.mBearer = cursor.getInt(5);
                    return configItem;
                }
            } finally {
                closeCursor(cursor);
            }
        }
        return null;
    }

    public APNConfig getAPN(String name) {
        ServiceUtils.checkPermission(this.mContext);
        Cursor cursor = getContentResolver().query(Telephony.Carriers.CONTENT_URI, new String[]{TemperatureController.STRATEGY_NAME, "apn", "user", "password", "authtype", "bearer", "mcc", "mnc", "numeric", "carrier_enabled", "current", "mmsc", "mmsport", "mmsproxy", "mvno_match_data", "mvno_type", "port", "protocol", "proxy", "roaming_protocol", "server", "sub_id", MiuiCustomizeShortCutUtils.ATTRIBUTE_TYPE}, buildNameSelection(name), null, "name ASC");
        if (cursor != null) {
            try {
                if (cursor.getCount() > 0) {
                    cursor.moveToFirst();
                    APNConfig configItem = new APNConfig();
                    configItem.mName = cursor.getString(0);
                    configItem.mApn = cursor.getString(1);
                    configItem.mUser = cursor.getString(2);
                    configItem.mPassword = cursor.getString(3);
                    configItem.mAuthType = cursor.getInt(4);
                    configItem.mBearer = cursor.getInt(5);
                    configItem.mMcc = cursor.getString(6);
                    configItem.mMnc = cursor.getString(7);
                    configItem.mNumeric = cursor.getString(8);
                    configItem.mCarrier_enabled = cursor.getInt(9);
                    configItem.mCurrent = cursor.getInt(10);
                    configItem.mMmsc = cursor.getString(11);
                    configItem.mMmsport = cursor.getString(12);
                    configItem.mMmsproxy = cursor.getString(13);
                    configItem.mMvno_match_data = cursor.getString(14);
                    configItem.mMvno_type = cursor.getString(15);
                    configItem.mPort = cursor.getString(16);
                    configItem.mProtocol = cursor.getString(17);
                    configItem.mProxy = cursor.getString(18);
                    configItem.mRoaming_protocol = cursor.getString(19);
                    configItem.mServer = cursor.getString(20);
                    configItem.mSub_id = cursor.getString(21);
                    configItem.mType = cursor.getString(22);
                    return configItem;
                }
            } finally {
                closeCursor(cursor);
            }
        }
        return null;
    }

    public void addAPNForNumeric(String numeric, APNConfig config) {
        ServiceUtils.checkPermission(this.mContext);
        if (TextUtils.isEmpty(numeric)) {
            Slog.e(TAG, "addAPNForNumeric:: Invalidate numeric");
            return;
        }
        ContentValues values = new ContentValues();
        values.put(TemperatureController.STRATEGY_NAME, config.mName);
        values.put("apn", config.mApn);
        values.put("user", config.mUser);
        values.put("password", config.mPassword);
        values.put("authtype", Integer.valueOf(config.mAuthType));
        values.put("bearer", Integer.valueOf(config.mBearer));
        values.put("mcc", numeric.substring(0, 3));
        values.put("mnc", numeric.substring(3, 5));
        values.put("numeric", numeric);
        Uri uri = getContentResolver().insert(Telephony.Carriers.CONTENT_URI, values);
        Slog.d(TAG, "addAPNForNumeric:: New apn config: " + uri);
    }

    public boolean addAPN(APNConfig config) {
        String numeric;
        ServiceUtils.checkPermission(this.mContext);
        if (TextUtils.isEmpty(config.mNumeric)) {
            numeric = getNumeric();
        } else {
            numeric = config.mNumeric;
        }
        if (TextUtils.isEmpty(numeric)) {
            Slog.e(TAG, "addAPN:: Invalidate numeric");
            return false;
        }
        ContentValues values = new ContentValues();
        values.put(TemperatureController.STRATEGY_NAME, config.mName);
        values.put("apn", config.mApn);
        values.put("user", config.mUser);
        values.put("password", config.mPassword);
        values.put("authtype", Integer.valueOf(config.mAuthType));
        values.put("bearer", Integer.valueOf(config.mBearer));
        values.put("mcc", TextUtils.isEmpty(config.mMcc) ? numeric.substring(0, 3) : config.mMcc);
        values.put("mnc", TextUtils.isEmpty(config.mMnc) ? numeric.substring(3, 5) : config.mMnc);
        values.put("numeric", numeric);
        if (config.mCarrier_enabled != -1) {
            values.put("carrier_enabled", Integer.valueOf(config.mCarrier_enabled));
        }
        if (config.mCurrent != -1) {
            values.put("current", Integer.valueOf(config.mCarrier_enabled));
        }
        if (config.mMmsc != null) {
            values.put("mmsc", config.mMmsc);
        }
        if (config.mMmsport != null) {
            values.put("mmsport", config.mMmsport);
        }
        if (config.mMmsproxy != null) {
            values.put("mmsproxy", config.mMmsproxy);
        }
        if (config.mMvno_match_data != null) {
            values.put("mvno_match_data", config.mMvno_match_data);
        }
        if (config.mMvno_type != null) {
            values.put("mvno_type", config.mMvno_type);
        }
        if (config.mPort != null) {
            values.put("port", config.mPort);
        }
        if (config.mProtocol != null) {
            values.put("protocol", config.mProtocol);
        }
        if (config.mProxy != null) {
            values.put("proxy", config.mProxy);
        }
        if (config.mRoaming_protocol != null) {
            values.put("roaming_protocol", config.mRoaming_protocol);
        }
        if (config.mServer != null) {
            values.put("server", config.mServer);
        }
        if (config.mSub_id != null) {
            values.put("sub_id", config.mSub_id);
        }
        if (config.mType != null) {
            values.put(MiuiCustomizeShortCutUtils.ATTRIBUTE_TYPE, config.mType);
        }
        Uri uri = getContentResolver().insert(Telephony.Carriers.CONTENT_URI, values);
        Slog.d(TAG, "addAPN:: New apn config: " + uri);
        return uri != null;
    }

    public void deleteAPNForNumeric(String numeric, String name) {
        ServiceUtils.checkPermission(this.mContext);
        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(numeric)) {
            Slog.e(TAG, "neither name or numeric can't be null");
            return;
        }
        int count = getContentResolver().delete(Telephony.Carriers.CONTENT_URI, buildNumericAndNameSelection(numeric, name), null);
        Slog.d(TAG, "Delete apn " + count + "rows");
    }

    public boolean deleteAPN(String name) {
        ServiceUtils.checkPermission(this.mContext);
        if (TextUtils.isEmpty(name)) {
            Slog.e(TAG, "neither name can't be null");
            return false;
        }
        int count = getContentResolver().delete(Telephony.Carriers.CONTENT_URI, buildNameSelection(name), null);
        Slog.d(TAG, "Delete apn " + count + "rows");
        return count > 0;
    }

    public void editAPNForNumeric(String numeric, String name, APNConfig config) {
        ServiceUtils.checkPermission(this.mContext);
        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(numeric)) {
            Slog.e(TAG, "neither name or numeric can't be null");
            return;
        }
        ContentValues values = new ContentValues();
        values.put(TemperatureController.STRATEGY_NAME, config.mName);
        values.put("apn", config.mApn);
        values.put("user", config.mUser);
        values.put("password", config.mPassword);
        values.put("authtype", Integer.valueOf(config.mAuthType));
        values.put("bearer", Integer.valueOf(config.mBearer));
        values.put("mcc", numeric.substring(0, 3));
        values.put("mnc", numeric.substring(3, 5));
        values.put("numeric", numeric);
        int count = getContentResolver().update(Telephony.Carriers.CONTENT_URI, values, buildNumericAndNameSelection(numeric, name), null);
        Slog.d(TAG, "Update apn " + count + "rows");
    }

    public boolean editAPN(String name, APNConfig config) {
        String numeric;
        ServiceUtils.checkPermission(this.mContext);
        if (TextUtils.isEmpty(config.mNumeric)) {
            numeric = getNumeric();
        } else {
            numeric = config.mNumeric;
        }
        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(numeric)) {
            Slog.e(TAG, "neither name or numeric can't be null");
            return false;
        }
        ContentValues values = new ContentValues();
        values.put(TemperatureController.STRATEGY_NAME, config.mName);
        values.put("apn", config.mApn);
        values.put("user", config.mUser);
        values.put("password", config.mPassword);
        values.put("authtype", Integer.valueOf(config.mAuthType));
        values.put("bearer", Integer.valueOf(config.mBearer));
        values.put("mcc", TextUtils.isEmpty(config.mMcc) ? numeric.substring(0, 3) : config.mMcc);
        values.put("mnc", TextUtils.isEmpty(config.mMnc) ? numeric.substring(3, 5) : config.mMnc);
        values.put("numeric", numeric);
        if (config.mCarrier_enabled != -1) {
            values.put("carrier_enabled", Integer.valueOf(config.mCarrier_enabled));
        }
        if (config.mCurrent != -1) {
            values.put("current", Integer.valueOf(config.mCarrier_enabled));
        }
        if (config.mMmsc != null) {
            values.put("mmsc", config.mMmsc);
        }
        if (config.mMmsport != null) {
            values.put("mmsport", config.mMmsport);
        }
        if (config.mMmsproxy != null) {
            values.put("mmsproxy", config.mMmsproxy);
        }
        if (config.mMvno_match_data != null) {
            values.put("mvno_match_data", config.mMvno_match_data);
        }
        if (config.mMvno_type != null) {
            values.put("mvno_type", config.mMvno_type);
        }
        if (config.mPort != null) {
            values.put("port", config.mPort);
        }
        if (config.mProtocol != null) {
            values.put("protocol", config.mProtocol);
        }
        if (config.mProxy != null) {
            values.put("proxy", config.mProxy);
        }
        if (config.mRoaming_protocol != null) {
            values.put("roaming_protocol", config.mRoaming_protocol);
        }
        if (config.mServer != null) {
            values.put("server", config.mServer);
        }
        if (config.mSub_id != null) {
            values.put("sub_id", config.mSub_id);
        }
        if (config.mType != null) {
            values.put(MiuiCustomizeShortCutUtils.ATTRIBUTE_TYPE, config.mType);
        }
        int count = getContentResolver().update(Telephony.Carriers.CONTENT_URI, values, buildNumericAndNameSelection(numeric, name), null);
        return count > 0;
    }

    public void activeAPNForNumeric(String numeric, String name) {
        ServiceUtils.checkPermission(this.mContext);
        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(numeric)) {
            Slog.e(TAG, "neither name or numeric can't be null");
            return;
        }
        Cursor cursor = getContentResolver().query(Telephony.Carriers.CONTENT_URI, new String[]{"_id"}, buildNumericAndNameSelection(numeric, name), null, "name ASC");
        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
            int apnId = cursor.getInt(0);
            ContentValues values = new ContentValues();
            values.put("apn_id", Integer.valueOf(apnId));
            int result = getContentResolver().update(PREFERAPN_URI, values, null, null);
            Slog.d(TAG, "active apn(" + apnId + "), result: " + result);
            cursor.close();
            return;
        }
        Slog.d(TAG, "No such config: " + name);
    }

    public boolean activeAPN(String name) {
        ServiceUtils.checkPermission(this.mContext);
        String numeric = getNumeric();
        boolean z = false;
        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(numeric)) {
            Slog.e(TAG, "neither name or numeric can't be null");
            return false;
        }
        Cursor cursor = getContentResolver().query(Telephony.Carriers.CONTENT_URI, new String[]{"_id"}, buildNumericAndNameSelection(numeric, name), null, "name ASC");
        if (cursor != null) {
            try {
                if (cursor.getCount() > 0) {
                    cursor.moveToFirst();
                    int apnId = cursor.getInt(0);
                    ContentValues values = new ContentValues();
                    values.put("apn_id", Integer.valueOf(apnId));
                    int result = getContentResolver().update(PREFERAPN_URI, values, null, null);
                    Slog.d(TAG, "active apn(" + apnId + "), result: " + result);
                    if (result > 0) {
                        z = true;
                    }
                    return z;
                }
            } finally {
                closeCursor(cursor);
            }
        }
        Slog.d(TAG, "No such config: " + name);
        return false;
    }

    public boolean resetAPN() {
        ServiceUtils.checkPermission(this.mContext);
        ContentResolver resolver = getContentResolver();
        int result = resolver.delete(getUriForCurrSubId(DEFAULTAPN_URI), null, null);
        return result != 0;
    }

    public void setAPNActiveMode(int mode) {
        ServiceUtils.checkPermission(this.mContext);
        EnterpriseSettings.putInt(this.mContext, "ep_apn_switch_mode", mode, 0);
    }

    public int getAPNActiveMode() {
        ServiceUtils.checkPermission(this.mContext);
        return EnterpriseSettings.getInt(this.mContext, "ep_apn_switch_mode", 0, 0);
    }

    public List<String> queryApn(String selections) {
        Cursor cursor = getContentResolver().query(Telephony.Carriers.CONTENT_URI, new String[]{TemperatureController.STRATEGY_NAME}, selections, null, "name ASC");
        List<String> result = new ArrayList<>();
        while (cursor != null) {
            try {
                if (!cursor.moveToNext()) {
                    break;
                }
                result.add(cursor.getString(0));
            } finally {
                closeCursor(cursor);
            }
        }
        return result;
    }

    private Uri getUriForCurrSubId(Uri uri) {
        int subId = SubscriptionManager.getDefault().getDefaultDataSubscriptionId();
        SubscriptionInfo subscriptionInfo = android.telephony.SubscriptionManager.from(this.mContext).getActiveSubscriptionInfo(subId);
        int subId0 = subscriptionInfo != null ? subscriptionInfo.getSubscriptionId() : -1;
        if (android.telephony.SubscriptionManager.isValidSubscriptionId(subId0)) {
            return Uri.withAppendedPath(uri, "subId/" + String.valueOf(subId0));
        }
        return uri;
    }
}
