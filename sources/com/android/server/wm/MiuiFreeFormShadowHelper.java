package com.android.server.wm;

import android.util.MiuiMultiWindowUtils;
import android.view.SurfaceControl;
/* loaded from: classes.dex */
public class MiuiFreeFormShadowHelper {
    private static final String TAG = "MiuiFreeFormShadowHelper";
    private MiuiFreeFormGestureController mGestureController;

    public MiuiFreeFormShadowHelper(MiuiFreeFormGestureController gestureController) {
        this.mGestureController = gestureController;
    }

    public void setShadowSettingsInTransaction(MiuiFreeFormActivityStack stack, int shadowType, float length, float[] color, float offsetX, float offsetY, float outset, int numOfLayers) {
        if (stack != null && stack.mTaskAnimationAdapter != null) {
            synchronized (stack.mTaskAnimationAdapter.mLock) {
                try {
                    try {
                        SurfaceControl leash = stack.getFreeFormConrolSurface();
                        if (leash != null && leash.isValid() && MiuiMultiWindowUtils.isSupportMiuiShadow()) {
                            Class<?>[] parameterTypes = {SurfaceControl.class, Integer.TYPE, Float.TYPE, float[].class, Float.TYPE, Float.TYPE, Float.TYPE, Integer.TYPE};
                            Object[] values = {leash, Integer.valueOf(shadowType), Float.valueOf(length), color, Float.valueOf(offsetX), Float.valueOf(offsetY), Float.valueOf(outset), Integer.valueOf(numOfLayers)};
                            MiuiMultiWindowUtils.callObjectMethod(this.mGestureController.mGestureListener.mGestureAnimator.mGestureTransaction, SurfaceControl.Transaction.class, "setShadowSettings", parameterTypes, values);
                        }
                    } catch (Throwable th) {
                        th = th;
                        throw th;
                    }
                } catch (Throwable th2) {
                    th = th2;
                }
            }
        }
    }

    public void setShadowSettingsInTransactionForSurfaceControl(SurfaceControl surface, int shadowType, float length, float[] color, float offsetX, float offsetY, float outset, int numOfLayers) {
        if (surface != null && surface.isValid() && MiuiMultiWindowUtils.isSupportMiuiShadow()) {
            Class<?>[] parameterTypes = {SurfaceControl.class, Integer.TYPE, Float.TYPE, float[].class, Float.TYPE, Float.TYPE, Float.TYPE, Integer.TYPE};
            Object[] values = {surface, Integer.valueOf(shadowType), Float.valueOf(length), color, Float.valueOf(offsetX), Float.valueOf(offsetY), Float.valueOf(outset), Integer.valueOf(numOfLayers)};
            MiuiMultiWindowUtils.callObjectMethod(this.mGestureController.mGestureListener.mGestureAnimator.mGestureTransaction, SurfaceControl.Transaction.class, "setShadowSettings", parameterTypes, values);
        }
    }

    public void resetShadowSettings(MiuiFreeFormActivityStack stack, boolean isHideShadowEarly) {
        synchronized (this.mGestureController.mService.mGlobalLock) {
            if (stack != null) {
                if (stack.mTask != null && stack.mTask.getSurfaceControl() != null && stack.mTask.getSurfaceControl().isValid()) {
                    SurfaceControl.Transaction t = new SurfaceControl.Transaction();
                    if (MiuiMultiWindowUtils.isSupportMiuiShadow()) {
                        if (!isHideShadowEarly) {
                            stack.mHasSetShadow = false;
                        }
                        Class<?>[] parameterTypes = {SurfaceControl.class, Integer.TYPE, Float.TYPE, float[].class, Float.TYPE, Float.TYPE, Float.TYPE, Integer.TYPE};
                        Object[] values = {stack.mTask.getSurfaceControl(), 0, Float.valueOf((float) MiuiFreeformPinManagerService.EDGE_AREA), MiuiMultiWindowUtils.MIUI_FREEFORM_RESET_COLOR, 0, 0, 0, 1};
                        MiuiMultiWindowUtils.callObjectMethod(t, SurfaceControl.Transaction.class, "setShadowSettings", parameterTypes, values);
                    } else if (isHideShadowEarly) {
                        stack.mTask.mIsBack = true;
                    } else {
                        t.setShadowRadius(stack.mTask.getSurfaceControl(), MiuiFreeformPinManagerService.EDGE_AREA);
                    }
                    t.apply();
                }
            }
        }
    }

    public void updateMiuiFreeformShadow(Task task) {
        WindowState topWindow;
        MiuiFreeFormActivityStack mffas;
        if (task.isRootTask() && task.isAttached() && (topWindow = task.getTopVisibleAppMainWindow()) != null && topWindow.isDrawn() && topWindow.isVisibleNow() && (mffas = this.mGestureController.mMiuiFreeFormManagerService.getMiuiFreeFormActivityStackForMiuiFB(task.mTaskId)) != null && !mffas.mHasSetShadow) {
            mffas.mHasSetShadow = true;
            SurfaceControl.Transaction t = new SurfaceControl.Transaction();
            Class<?>[] parameterTypes = {SurfaceControl.class, Integer.TYPE, Float.TYPE, float[].class, Float.TYPE, Float.TYPE, Float.TYPE, Integer.TYPE};
            Object[] values = {task.getSurfaceControl(), 1, Float.valueOf(400.0f), MiuiMultiWindowUtils.MIUI_FREEFORM_AMBIENT_COLOR, 0, 0, 0, 1};
            MiuiMultiWindowUtils.callObjectMethod(t, SurfaceControl.Transaction.class, "setShadowSettings", parameterTypes, values);
            t.apply();
        }
    }
}
