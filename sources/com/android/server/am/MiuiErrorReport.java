package com.android.server.am;

import android.app.ApplicationErrorReport;
import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;
import android.util.Log;
import android.util.Slog;
import org.json.JSONObject;
/* loaded from: classes.dex */
public class MiuiErrorReport {
    private static final String ACTION_FC_PREVIEW = "miui.intent.action.FC_PREVIEW";
    private static final String ERROR_TYPE_FC = "fc";
    private static final String EXTRA_FC_PREVIEW = "extra_fc_report";
    private static final String JSON_ERROR_TYPE = "error_type";
    private static final String JSON_EXCEPTION_CLASS = "exception_class";
    private static final String JSON_EXCEPTION_SOURCE_METHOD = "exception_source_method";
    private static final String JSON_PACKAGE_NAME = "package_name";
    private static final String JSON_STACK_TRACK = "stack_track";
    private static final String TAG = "MiuiErrorReport";

    public static void startFcPreviewActivity(Context context, String packageName, ApplicationErrorReport.CrashInfo crashInfo) {
        if (crashInfo == null) {
            return;
        }
        Intent intent = new Intent();
        intent.setAction(ACTION_FC_PREVIEW);
        intent.putExtra(EXTRA_FC_PREVIEW, getExceptionData(packageName, crashInfo).toString());
        intent.setFlags(268435456);
        try {
            context.startActivityAsUser(intent, UserHandle.CURRENT);
        } catch (Exception e) {
            Slog.i(TAG, "Failed to start preview activity", e);
        }
    }

    public static JSONObject getExceptionData(String packageName, ApplicationErrorReport.CrashInfo crashInfo) {
        JSONObject json = new JSONObject();
        try {
            json.put(JSON_PACKAGE_NAME, packageName);
            json.put(JSON_ERROR_TYPE, ERROR_TYPE_FC);
            json.put(JSON_EXCEPTION_CLASS, crashInfo.exceptionClassName);
            json.put(JSON_EXCEPTION_SOURCE_METHOD, crashInfo.throwClassName + "." + crashInfo.throwMethodName);
            json.put(JSON_STACK_TRACK, crashInfo.stackTrace);
        } catch (Exception e) {
            Log.w(TAG, "Fail to getExceptionData", e);
        }
        return json;
    }
}
