package com.android.server.wm;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.os.Handler;
import android.os.IRemoteCallback;
import android.util.ArraySet;
import android.view.SurfaceControl;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateWithClipAnimation;
import com.android.internal.policy.ScreenDecorationsUtils;
import com.miui.base.MiuiStubRegistry;
import com.miui.server.AccessController;
import java.util.ArrayList;
/* loaded from: classes.dex */
public class AppTransitionImpl implements AppTransitionStub {
    static final ArrayList<String> WHITE_LIST_ALLOW_CUSTOM_TASK_ANIMATION = new ArrayList<String>() { // from class: com.android.server.wm.AppTransitionImpl.1
        {
            add("com.android.incallui");
            add("com.android.contacts");
            add(AccessController.PACKAGE_CAMERA);
        }
    };

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<AppTransitionImpl> {

        /* compiled from: AppTransitionImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final AppTransitionImpl INSTANCE = new AppTransitionImpl();
        }

        public AppTransitionImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public AppTransitionImpl provideNewInstance() {
            return new AppTransitionImpl();
        }
    }

    public boolean isMiuiAppTransitionType(int nextAppTransitionType) {
        return nextAppTransitionType == 103 || nextAppTransitionType == 105 || nextAppTransitionType == 104 || nextAppTransitionType == 106;
    }

    public boolean isNeedgetDefaultNextAppTransitionStartRect(int nextAppTransitionType) {
        return nextAppTransitionType == 103 || nextAppTransitionType == 105;
    }

    public Animation loadMiuiAnimation(int nextAppTransitionType, boolean enter, Rect appFrame, Rect positionRect, float radius, Handler handler, boolean scaleBackToScreenCenter, IRemoteCallback animationStartCallback, IRemoteCallback animationFinishCallback) {
        Animation a = null;
        if (nextAppTransitionType == 103 || nextAppTransitionType == 105) {
            a = AppTransitionInjector.createScaleUpAnimation(enter, appFrame, positionRect, radius);
            if (enter) {
                AppTransitionInjector.addAnimationListener(a, handler, animationStartCallback, animationFinishCallback);
            }
        } else if (nextAppTransitionType == 104 || nextAppTransitionType == 106) {
            if (scaleBackToScreenCenter) {
                a = AppTransitionInjector.createBackActivityScaledToScreenCenter(enter, appFrame);
            } else {
                a = AppTransitionInjector.createScaleDownAnimation(enter, appFrame, positionRect, radius);
            }
            if (!enter) {
                AppTransitionInjector.addAnimationListener(a, handler, animationStartCallback, animationFinishCallback);
            }
        }
        return a;
    }

    public Animation loadMiuiDefaultAnimationNotCheck(WindowManager.LayoutParams lp, int transit, boolean enter, Rect frame, WindowContainer container, WindowManagerService service) {
        return AppTransitionInjector.loadDefaultAnimationNotCheck(lp, transit, enter, frame, container, service);
    }

    public boolean useDefaultAnimationAttr(WindowManager.LayoutParams lp, int resId, int transit, boolean enter, boolean isFreeForm) {
        return AppTransitionInjector.useDefaultAnimationAttr(lp, resId, transit, enter, isFreeForm);
    }

    public boolean shouldActivityTransitionRoundCorner(int nextAppTransitionType, boolean nextAppTransitionScaleUp) {
        return nextAppTransitionType == 105 || nextAppTransitionType == 103 || (nextAppTransitionType == 104 && !nextAppTransitionScaleUp) || (nextAppTransitionType == 106 && !nextAppTransitionScaleUp);
    }

    public void calculateMiuiActivityThumbnailSpec(Rect appRect, Rect thumbnailRect, Matrix curSpec, float alpha, float radius, SurfaceControl.Transaction t, SurfaceControl leash) {
        if (appRect == null || thumbnailRect == null || curSpec == null || leash == null) {
            return;
        }
        if (t == null) {
            return;
        }
        float[] tmp = new float[9];
        curSpec.getValues(tmp);
        float curTranslateX = tmp[2];
        float curTranslateY = tmp[5];
        float curScaleX = tmp[0];
        float curScaleY = tmp[4];
        float curWidth = appRect.width() * curScaleX;
        float curHeight = appRect.height() * curScaleY;
        t.setWindowCrop(leash, new Rect(0, 0, (int) curWidth, (int) curHeight));
        t.setPosition(leash, curTranslateX, curTranslateY);
        t.setAlpha(leash, alpha);
    }

    public void calculateScaleUpDownThumbnailSpec(Rect appClipRect, Rect thumbnailRect, Matrix curSpec, SurfaceControl.Transaction t, SurfaceControl leash) {
        if (appClipRect == null || thumbnailRect == null || curSpec == null || leash == null || t == null) {
            return;
        }
        float[] tmp = new float[9];
        t.setMatrix(leash, curSpec, tmp);
        t.setWindowCrop(leash, appClipRect);
        t.setAlpha(leash, 1.0f);
    }

    public Animation loadFullSplitAnimation(int transit, boolean enter, Rect frame, int orientation, ActivityRecord ar, boolean splitMode) {
        return AppTransitionInjector.loadFullSplitAnimation(transit, enter, frame, orientation, ar, splitMode);
    }

    public Animation loadSplitFullAnimation(int transit, boolean enter, Rect frame, int orientation, ActivityRecord ar, boolean splitMode) {
        return AppTransitionInjector.loadSplitFullAnimation(transit, enter, frame, orientation, ar, splitMode);
    }

    public Animation loadSplitAnimation(int transit, boolean enter, Rect frame, int orientation, ActivityRecord ar) {
        return AppTransitionInjector.loadSplitAnimation(transit, enter, frame, orientation, ar);
    }

    public boolean stepSplitDimmerIfNeeded(Animation a, SurfaceControl.Transaction t) {
        if (a instanceof SplitDimmer) {
            ((SplitDimmer) a).stepTransitionDim(t);
            return true;
        }
        return false;
    }

    public Animation dimSplitDimmerAboveIfNeeded(Animation a, WindowContainer wc) {
        if (a instanceof SplitDimmer) {
            SplitDimmer dimmer = (SplitDimmer) a;
            dimmer.dimAbove(wc.getPendingTransaction(), wc);
            return dimmer;
        }
        return null;
    }

    public void stopSplitDimmer(Animation a, WindowContainer wc) {
        if (a instanceof SplitDimmer) {
            SplitDimmer dimmer = (SplitDimmer) a;
            if (!dimmer.isVisible) {
                return;
            }
            dimmer.stopDim(wc.getPendingTransaction());
        }
    }

    public Animation loadKeyguardUnOccludeAnimation(Context context, Animation animation) {
        float radius = ScreenDecorationsUtils.getWindowCornerRadius(context);
        if (radius <= MiuiFreeformPinManagerService.EDGE_AREA) {
            return animation;
        }
        AnimationSet set = new AnimationSet(false);
        set.addAnimation(animation);
        set.addAnimation(new RadiusAnimation(radius, radius));
        set.setHasRoundedCorners(true);
        return set;
    }

    public boolean allowCustomTaskAnimation(String packagename) {
        if (WHITE_LIST_ALLOW_CUSTOM_TASK_ANIMATION.contains(packagename)) {
            return true;
        }
        return false;
    }

    public boolean isUseFloatingAnimation(Animation a) {
        return AppTransitionInjector.isUseFloatingAnimation(a);
    }

    /* JADX WARN: Removed duplicated region for block: B:27:0x007c  */
    /* JADX WARN: Removed duplicated region for block: B:28:0x0081  */
    /* JADX WARN: Removed duplicated region for block: B:31:0x008e  */
    /* JADX WARN: Removed duplicated region for block: B:41:0x0133  */
    /* JADX WARN: Removed duplicated region for block: B:58:0x0174  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public android.view.animation.Animation updateAnimationIfNeed(android.view.animation.Animation r24, android.view.WindowManager.LayoutParams r25, int r26, boolean r27, android.graphics.Rect r28, boolean r29, com.android.server.wm.WindowContainer r30, com.android.server.wm.WindowManagerService r31) {
        /*
            Method dump skipped, instructions count: 384
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wm.AppTransitionImpl.updateAnimationIfNeed(android.view.animation.Animation, android.view.WindowManager$LayoutParams, int, boolean, android.graphics.Rect, boolean, com.android.server.wm.WindowContainer, com.android.server.wm.WindowManagerService):android.view.animation.Animation");
    }

    public boolean setFloatingCornerRadius(Animation a, SurfaceControl.Transaction t, SurfaceControl leash) {
        if (a instanceof TranslateWithClipAnimation) {
            t.setCornerRadius(leash, ((TranslateWithClipAnimation) a).getCornerRadius());
            return true;
        }
        return false;
    }

    public boolean stepAnimationDimmerIfNeeded(Animation a, SurfaceControl.Transaction t) {
        if (a instanceof AnimationDimmer) {
            ((AnimationDimmer) a).stepTransitionDim(t);
            return true;
        }
        return false;
    }

    public Animation dimAnimationDimmerAboveIfNeeded(Animation a, WindowContainer wc) {
        if (a instanceof AnimationDimmer) {
            AnimationDimmer dimmer = (AnimationDimmer) a;
            dimmer.dimAbove(wc.getPendingTransaction(), wc);
            return dimmer;
        }
        return null;
    }

    public void stopAnimationDimmer(Animation a, WindowContainer wc) {
        if (a instanceof AnimationDimmer) {
            AnimationDimmer dimmer = (AnimationDimmer) a;
            if (!dimmer.isVisible) {
                return;
            }
            dimmer.stopDim(wc.getPendingTransaction());
        }
    }

    public void switchTransitType(ArraySet<WindowContainer> openingWcs, ArraySet<WindowContainer> closingWcs, WindowManagerService service) {
        if (!openingWcs.isEmpty() && !closingWcs.isEmpty()) {
            WindowContainer openContainer = null;
            WindowContainer closeContainer = null;
            int i = 0;
            while (true) {
                if (i >= openingWcs.size()) {
                    break;
                }
                WindowContainer openContainer2 = openingWcs.valueAt(i);
                openContainer = openContainer2;
                openContainer.mNeedReplaceTaskAnimation = false;
                if (!isSwitchAnimationType(openContainer, service)) {
                    i++;
                } else {
                    openContainer.mNeedReplaceTaskAnimation = true;
                    break;
                }
            }
            int i2 = 0;
            while (true) {
                if (i2 >= closingWcs.size()) {
                    break;
                }
                WindowContainer closeContainer2 = closingWcs.valueAt(i2);
                closeContainer = closeContainer2;
                closeContainer.mNeedReplaceTaskAnimation = false;
                if (!isSwitchAnimationType(closeContainer, service)) {
                    i2++;
                } else {
                    closeContainer.mNeedReplaceTaskAnimation = true;
                    break;
                }
            }
            if (openContainer == null || closeContainer == null) {
                return;
            }
            if (!openContainer.mNeedReplaceTaskAnimation || !closeContainer.mNeedReplaceTaskAnimation) {
                openContainer.mNeedReplaceTaskAnimation = false;
                closeContainer.mNeedReplaceTaskAnimation = false;
            }
        }
    }

    private boolean isSwitchAnimationType(WindowContainer container, WindowManagerService service) {
        PackageManager pm = service.mContext.getPackageManager();
        try {
            if ((container instanceof Task) && container.getTopMostActivity() != null) {
                ActivityInfo info = pm.getActivityInfo(container.getTopMostActivity().mActivityComponent, 128);
                if (info.metaData != null) {
                    return info.metaData.getBoolean("switchanimationtype");
                }
                return false;
            }
            return false;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }

    public Animation dimAnimationDimmerBelowIfNeeded(Animation a, WindowContainer wc) {
        if (a instanceof AnimationDimmer) {
            AnimationDimmer dimmer = (AnimationDimmer) a;
            dimmer.dimBelow(wc.getPendingTransaction(), wc);
            Rect frame = dimmer.getClipRect();
            SurfaceControl sc = dimmer.getDimmer();
            if (frame != null && sc != null) {
                wc.getPendingTransaction().setWindowCrop(sc, frame.right - frame.left, frame.bottom - frame.top);
                wc.getPendingTransaction().setPosition(sc, frame.left, frame.top);
            }
            return dimmer;
        }
        return null;
    }
}
