package com.android.server.wm;

import android.os.SystemProperties;
import android.util.MiuiMultiWindowAdapter;
import android.util.Slog;
/* loaded from: classes.dex */
public class MiuiFreeFormCameraStrategy {
    public static final int CAMERA_INITIATED = 1;
    public static final int CAMERA_RELEASED = 0;
    private static final int DEFAULT_CAMERA_ORIENTATION = -1;
    private static final String FREEFORM_CAMERA_ORIENTATION_KEY = "persist.vendor.freeform.cam.orientation";
    private static final String FREEFORM_CAMERA_PROP_KEY = "persist.vendor.freeform.3appcam";
    private int mCameraStateGlobal;
    private int mCameraStateInFreeForm;
    private MiuiFreeFormManagerService mService;
    private final String TAG = "MiuiFreeFormCameraStrategy";
    private String mPackageUseCamera = "";

    public MiuiFreeFormCameraStrategy(MiuiFreeFormManagerService service) {
        this.mCameraStateInFreeForm = 0;
        this.mCameraStateGlobal = 0;
        this.mService = service;
        setFreeFormCameraProp(0);
        setCameraOrientation(-1);
        this.mCameraStateInFreeForm = 0;
        this.mCameraStateGlobal = 0;
    }

    public void onCameraStateChanged(String packageName, int cameraState) {
        int i;
        this.mCameraStateGlobal = cameraState;
        this.mPackageUseCamera = cameraState == 1 ? packageName : "";
        if (this.mService.isInFreeForm(packageName) && !MiuiMultiWindowAdapter.USE_DEFAULT_CAMERA_PIPELINE_APP.contains(packageName) && (i = this.mCameraStateGlobal) != this.mCameraStateInFreeForm) {
            setFreeFormCameraProp(i);
            if (this.mCameraStateGlobal == 1) {
                Slog.d("MiuiFreeFormCameraStrategy", "camera opened!");
                setCameraOrientation();
                return;
            }
            Slog.d("MiuiFreeFormCameraStrategy", "camera released!");
            setCameraOrientation(-1);
        }
    }

    public void rotateCameraIfNeeded() {
        if (this.mCameraStateGlobal == 0) {
            return;
        }
        if (this.mService.isInFreeForm(this.mPackageUseCamera) && !MiuiMultiWindowAdapter.USE_DEFAULT_CAMERA_PIPELINE_APP.contains(this.mPackageUseCamera)) {
            if (this.mCameraStateInFreeForm != 1) {
                setFreeFormCameraProp(1);
                setCameraOrientation();
                return;
            }
            return;
        }
        setFreeFormCameraProp(0);
        setCameraOrientation(-1);
    }

    public boolean openCameraInFreeForm(String packageName) {
        return this.mService.isInFreeForm(packageName) && this.mPackageUseCamera.equals(packageName);
    }

    public void setCameraOrientation(int orientation) {
        if (this.mCameraStateInFreeForm == 0 && orientation != -1) {
            return;
        }
        try {
            SystemProperties.set(FREEFORM_CAMERA_ORIENTATION_KEY, String.format("%d", Integer.valueOf(orientation)));
        } catch (Exception e) {
            Slog.d("MiuiFreeFormCameraStrategy", "set persist.vendor.freeform.cam.orientation error! " + e.toString());
        }
    }

    public void setCameraOrientation() {
        setCameraOrientation(this.mService.mActivityTaskManagerService.mWindowManager.getDefaultDisplayContentLocked().mDisplay.getRotation());
    }

    public void setFreeFormCameraProp(int cameraProp) {
        this.mCameraStateInFreeForm = cameraProp;
        try {
            SystemProperties.set(FREEFORM_CAMERA_PROP_KEY, String.format("%d", Integer.valueOf(cameraProp)));
        } catch (Exception e) {
            Slog.d("MiuiFreeFormCameraStrategy", "set persist.vendor.freeform.3appcam error! " + e.toString());
        }
    }
}
