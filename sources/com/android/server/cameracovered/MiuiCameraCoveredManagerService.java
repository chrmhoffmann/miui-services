package com.android.server.cameracovered;

import android.cameracovered.IMiuiCameraCoveredManager;
import android.content.Context;
import android.os.Binder;
import android.provider.MiuiSettings;
import android.text.TextUtils;
import android.util.Slog;
import com.android.server.LocalServices;
import com.android.server.wm.WindowManagerInternal;
import com.android.server.wm.WindowManagerService;
import java.util.ArrayList;
import miui.cameraanimation.CameraBlackCoveredManager;
import miui.os.DeviceFeature;
import org.json.JSONArray;
import org.json.JSONException;
/* loaded from: classes.dex */
public final class MiuiCameraCoveredManagerService extends IMiuiCameraCoveredManager.Stub {
    public static final String SERVICE_NAME = "camera_covered_service";
    public static final String TAG = "CameraCoveredManager";
    private CameraBlackCoveredManager mCameraBlackCoveredManager;
    private Context mContext;
    private WindowManagerInternal mWindowManagerService = (WindowManagerInternal) LocalServices.getService(WindowManagerInternal.class);
    private WindowManagerService mWms;
    private static String Test_Dynamic_Cutout = "testDynamicCutout";
    private static String Packages = "packages";

    public MiuiCameraCoveredManagerService(Context context) {
        this.mContext = context;
    }

    public void systemBooted() {
        try {
            if (DeviceFeature.SUPPORT_FRONTCAMERA_CIRCLE_BLACK) {
                CameraBlackCoveredManager cameraBlackCoveredManager = new CameraBlackCoveredManager(this.mContext);
                this.mCameraBlackCoveredManager = cameraBlackCoveredManager;
                cameraBlackCoveredManager.systemReady();
            }
        } catch (Exception e) {
            Slog.d(TAG, e.toString());
        }
    }

    public String getTopStackPackageName(int windowingMode) {
        WindowManagerInternal windowManagerInternal = this.mWindowManagerService;
        if (windowManagerInternal != null) {
            return windowManagerInternal.getTopStackPackageName(windowingMode);
        }
        return null;
    }

    public void setCoveredPackageName(String packageName) {
        CameraBlackCoveredManager cameraBlackCoveredManager = this.mCameraBlackCoveredManager;
        if (cameraBlackCoveredManager != null) {
            cameraBlackCoveredManager.setCoverdPackageName(packageName);
        } else {
            Slog.e(TAG, "SetCoveredPackageName but not support camera covered feature!");
        }
    }

    public void hideCoveredBlackView() {
        CameraBlackCoveredManager cameraBlackCoveredManager = this.mCameraBlackCoveredManager;
        if (cameraBlackCoveredManager != null) {
            cameraBlackCoveredManager.hideCoveredBlackView();
        } else {
            Slog.e(TAG, "HideCoveredBlackView but not support camera covered feture!");
        }
    }

    public void cupMuraCoveredAnimation(int type) {
        CameraBlackCoveredManager cameraBlackCoveredManager = this.mCameraBlackCoveredManager;
        if (cameraBlackCoveredManager != null) {
            cameraBlackCoveredManager.cupMuraCoveredAnimation(type);
        } else {
            Slog.e(TAG, "cupMuraCoveredAnimation but not support camera covered feture!");
        }
    }

    public void hbmCoveredAnimation(int type) {
        CameraBlackCoveredManager cameraBlackCoveredManager = this.mCameraBlackCoveredManager;
        if (cameraBlackCoveredManager != null) {
            cameraBlackCoveredManager.hbmCoveredAnimation(type);
        } else {
            Slog.e(TAG, "HbmCoveredAnimation but not support camera covered feture!");
        }
    }

    public void addCoveredBlackView() {
        CameraBlackCoveredManager cameraBlackCoveredManager = this.mCameraBlackCoveredManager;
        if (cameraBlackCoveredManager != null) {
            cameraBlackCoveredManager.startCameraAnimation(true);
        } else {
            Slog.e(TAG, "AddCoveredBlackView but not support camera covered feture!");
        }
    }

    public boolean needDisableCutout(String packageName) {
        new ArrayList();
        long ident = Binder.clearCallingIdentity();
        try {
            try {
            } catch (JSONException e) {
                Slog.e(TAG, "exception when updateForceRestartAppList: ", e);
            }
            if (this.mContext.getContentResolver() != null && packageName != null) {
                String data = MiuiSettings.SettingsCloudData.getCloudDataString(this.mContext.getContentResolver(), Test_Dynamic_Cutout, Packages, (String) null);
                if (!TextUtils.isEmpty(data)) {
                    JSONArray apps = new JSONArray(data);
                    Slog.d(TAG, "needDisableCutout: packageName: " + packageName);
                    for (int i = 0; i < apps.length(); i++) {
                        if (apps.getString(i).equals(packageName)) {
                            Binder.restoreCallingIdentity(ident);
                            return true;
                        }
                    }
                    return false;
                }
                return false;
            }
            Slog.d(TAG, "mContext.getContentResolver() or packageName is null");
            return false;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }
}
