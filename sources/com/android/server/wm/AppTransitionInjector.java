package com.android.server.wm;

import android.content.Context;
import android.content.Intent;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.IRemoteCallback;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.server.am.SplitScreenReporter;
import android.util.ArraySet;
import android.util.MiuiSplitUtils;
import android.util.Slog;
import android.view.Display;
import android.view.RoundedCorner;
import android.view.SurfaceControl;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.ScaleAnimation;
import android.view.animation.Transformation;
import android.view.animation.TranslateAnimation;
import android.view.animation.TranslateWithClipAnimation;
import android.view.animation.TranslateXAnimation;
import android.view.animation.TranslateXWithClipAnimation;
import android.view.animation.TranslateYAnimation;
import com.android.internal.util.function.pooled.PooledLambda;
import com.android.server.inputmethod.InputMethodManagerServiceImpl;
import com.miui.server.AccessController;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.function.Consumer;
import miui.security.ISecurityManager;
/* loaded from: classes.dex */
public class AppTransitionInjector {
    static final int APP_TRANSITION_SPECS_PENDING_TIMEOUT;
    static final int BEZIER_ALLPAPER_OPEN_DURATION = 400;
    private static final ArrayList<String> BLACK_LIST_NOT_ALLOWED_SNAPSHOT;
    private static final HashSet<String> BLACK_LIST_NOT_ALLOWED_SNAPSHOT_COMPONENT;
    private static final Interpolator CUBIC_EASE_OUT_INTERPOLATOR;
    private static final int DEFAULT_ACTIVITY_SCALE_DOWN_ALPHA_DELAY = 100;
    private static final int DEFAULT_ACTIVITY_SCALE_DOWN_ALPHA_DURATION = 200;
    private static final int DEFAULT_ACTIVITY_SCALE_DOWN_DURATION = 450;
    private static final int DEFAULT_ACTIVITY_SCALE_UP_ALPHA_DURATION = 100;
    private static final int DEFAULT_ACTIVITY_SCALE_UP_DURATION = 500;
    private static final int DEFAULT_ACTIVITY_TRANSITION_DURATION = 500;
    private static final int DEFAULT_ALPHA_DURATION = 210;
    private static final int DEFAULT_ALPHA_OFFSET = 40;
    private static final int DEFAULT_ANIMATION_DURATION = 300;
    static final int DEFAULT_APP_TRANSITION_ROUND_CORNER_RADIUS = 60;
    private static final float DEFAULT_BACK_TO_SCREEN_CENTER_SCALE = 0.6f;
    private static final float DEFAULT_ENTER_ACTIVITY_END_ALPHA = 1.0f;
    private static final float DEFAULT_ENTER_ACTIVITY_START_ALPHA = 0.0f;
    private static final float DEFAULT_EXIT_ACTIVITY_END_ALPHA = 0.8f;
    private static final float DEFAULT_EXIT_ACTIVITY_START_ALPHA = 1.0f;
    private static final int DEFAULT_LAUNCH_FORM_HOME_DURATION = 300;
    private static final float DEFAULT_REENTER_ACTIVITY_END_ALPHA = 1.0f;
    private static final float DEFAULT_REENTER_ACTIVITY_START_ALPHA = 0.8f;
    private static final float DEFAULT_RETURN_ACTIVITY_END_ALPHA = 0.0f;
    private static final float DEFAULT_RETURN_ACTIVITY_START_ALPHA = 1.0f;
    private static final int DEFAULT_TASK_TRANSITION_DURATION = 650;
    private static final float DEFAULT_WALLPAPER_EXIT_SCALE_X = 0.4f;
    private static final float DEFAULT_WALLPAPER_EXIT_SCALE_Y = 0.4f;
    static final int DEFAULT_WALLPAPER_OPEN_DURATION = 300;
    private static final int DEFAULT_WALLPAPER_TRANSITION_DURATION = 550;
    private static final int DEFAULT_WALLPAPER_TRANSITION_R_CTS_DURATION = 400;
    private static final ArrayList<String> IGNORE_LAUNCHED_FROM_SYSTEM_SURFACE;
    private static final float LAUNCHER_DEFAULT_ALPHA = 1.0f;
    private static final float LAUNCHER_DEFAULT_SCALE = 1.0f;
    private static final float LAUNCHER_TRANSITION_ALPHA = 0.0f;
    private static final float LAUNCHER_TRANSITION_SCALE = 0.8f;
    static final int NEXT_TRANSIT_TYPE_BACK_HOME = 102;
    static final int NEXT_TRANSIT_TYPE_BACK_WITH_SCALED_THUMB = 106;
    static final int NEXT_TRANSIT_TYPE_LAUNCH_BACK_ROUNDED_VIEW = 104;
    static final int NEXT_TRANSIT_TYPE_LAUNCH_FROM_HOME = 101;
    static final int NEXT_TRANSIT_TYPE_LAUNCH_FROM_ROUNDED_VIEW = 103;
    static final int NEXT_TRANSIT_TYPE_LAUNCH_WITH_SCALED_THUMB = 105;
    static final int PENDING_EXECUTE_APP_TRANSITION_TIMEOUT = 100;
    private static final Interpolator QUART_EASE_OUT_INTERPOLATOR;
    private static final Interpolator QUINT_EASE_OUT_INTERPOLATOR;
    private static final Interpolator SCALE_DOWN_PHYSIC_BASED_INTERPOLATOR;
    private static final Interpolator SCALE_UP_PHYSIC_BASED_INTERPOLATOR;
    private static final int SPLIT_ANIMATION_DELAY = 450;
    private static final int SPLIT_ANIMATION_DURATION = 400;
    private static final int SPLIT_TRANSLATE_LEFT_OFFSET = -300;
    private static final String TAG = "AppTransitionInjector";
    static final int THUMBNAIL_ANIMATION_TIMEOUT_DURATION = 1000;
    private static final ArrayList<String> WHITE_LIST_ALLOW_CUSTOM_ANIMATION;
    static final ArrayList<String> WHITE_LIST_ALLOW_CUSTOM_APPLICATION_TRANSITION;
    private static Interpolator sActivityTransitionInterpolator;
    private static Rect sMiuiAnimSupportInset;
    static int DISPLAY_ROUND_CORNER_RADIUS = 60;
    static final boolean IS_E10 = "beryllium".equals(Build.PRODUCT);

    static {
        APP_TRANSITION_SPECS_PENDING_TIMEOUT = "cactus".equals(Build.DEVICE) ? ScreenRotationAnimationImpl.COVER_EGE : 100;
        CUBIC_EASE_OUT_INTERPOLATOR = new CubicEaseOutInterpolator();
        SCALE_UP_PHYSIC_BASED_INTERPOLATOR = new PhysicBasedInterpolator(0.95f, 0.8f);
        SCALE_DOWN_PHYSIC_BASED_INTERPOLATOR = new PhysicBasedInterpolator(0.95f, 0.78f);
        QUART_EASE_OUT_INTERPOLATOR = new QuartEaseOutInterpolator();
        QUINT_EASE_OUT_INTERPOLATOR = new QuintEaseOutInterpolator();
        sActivityTransitionInterpolator = null;
        sMiuiAnimSupportInset = new Rect();
        WHITE_LIST_ALLOW_CUSTOM_ANIMATION = new ArrayList<String>() { // from class: com.android.server.wm.AppTransitionInjector.1
            {
                add("com.android.quicksearchbox");
                add("com.android.systemui");
                add("com.miui.newhome");
            }
        };
        HashSet<String> hashSet = new HashSet<>();
        BLACK_LIST_NOT_ALLOWED_SNAPSHOT_COMPONENT = hashSet;
        hashSet.add("com.tencent.mm/.plugin.offline.ui.WalletOfflineCoinPurseUI");
        ArrayList<String> arrayList = new ArrayList<>();
        BLACK_LIST_NOT_ALLOWED_SNAPSHOT = arrayList;
        ArrayList<String> arrayList2 = new ArrayList<>();
        IGNORE_LAUNCHED_FROM_SYSTEM_SURFACE = arrayList2;
        arrayList.add(AccessController.PACKAGE_CAMERA);
        arrayList.add("com.android.browser");
        arrayList.add(ActivityTaskSupervisorImpl.MIUI_APP_LOCK_PACKAGE_NAME);
        arrayList.add("com.mi.globalbrowser");
        arrayList.add(InputMethodManagerServiceImpl.MIUI_HOME);
        arrayList.add("com.mi.android.globallauncher");
        arrayList2.add("com.mfashiongallery.emag");
        arrayList2.add(AccessController.PACKAGE_CAMERA);
        WHITE_LIST_ALLOW_CUSTOM_APPLICATION_TRANSITION = new ArrayList<String>() { // from class: com.android.server.wm.AppTransitionInjector.3
            {
                add("com.android.incallui");
            }
        };
    }

    public static Animation createScaleUpAnimation(boolean enter, Rect appFrame, Rect positionRect, float radius) {
        if (enter) {
            Animation alphaAnimation = new AlphaAnimation((float) MiuiFreeformPinManagerService.EDGE_AREA, 1.0f);
            Animation radiusAnimation = new RadiusAnimation(radius, DISPLAY_ROUND_CORNER_RADIUS);
            Animation scaleWithClipAnimation = new ScaleWithClipAnimation(positionRect, appFrame);
            AnimationSet set = new AnimationSet(false);
            set.addAnimation(alphaAnimation);
            set.addAnimation(radiusAnimation);
            set.addAnimation(scaleWithClipAnimation);
            alphaAnimation.setInterpolator(QUART_EASE_OUT_INTERPOLATOR);
            Interpolator interpolator = SCALE_UP_PHYSIC_BASED_INTERPOLATOR;
            radiusAnimation.setInterpolator(interpolator);
            scaleWithClipAnimation.setInterpolator(interpolator);
            set.setZAdjustment(1);
            alphaAnimation.setDuration(100L);
            radiusAnimation.setDuration(500L);
            scaleWithClipAnimation.setDuration(500L);
            return set;
        }
        AnimationSet set2 = new AnimationSet(false);
        Animation alphaAnimation2 = new AlphaAnimation(1.0f, 0.8f);
        set2.addAnimation(alphaAnimation2);
        alphaAnimation2.setInterpolator(SCALE_UP_PHYSIC_BASED_INTERPOLATOR);
        set2.setZAdjustment(-1);
        alphaAnimation2.setDuration(500L);
        set2.setFillAfter(true);
        return set2;
    }

    public static Animation createScaleDownAnimation(boolean enter, Rect appFrame, Rect targetPositionRect, float radius) {
        if (enter) {
            AnimationSet set = new AnimationSet(false);
            Animation alphaAnimation = new AlphaAnimation(0.8f, 1.0f);
            set.addAnimation(alphaAnimation);
            alphaAnimation.setInterpolator(SCALE_DOWN_PHYSIC_BASED_INTERPOLATOR);
            set.setZAdjustment(-1);
            alphaAnimation.setDuration(450L);
            return set;
        }
        Animation alphaAnimation2 = new AlphaAnimation(1.0f, (float) MiuiFreeformPinManagerService.EDGE_AREA);
        alphaAnimation2.setStartOffset(100L);
        Animation radiusAnimation = new RadiusAnimation(DISPLAY_ROUND_CORNER_RADIUS, radius);
        Animation scaleWithClipAnimation = new ScaleWithClipAnimation(appFrame, targetPositionRect);
        AnimationSet set2 = new AnimationSet(true);
        set2.addAnimation(alphaAnimation2);
        set2.addAnimation(radiusAnimation);
        set2.addAnimation(scaleWithClipAnimation);
        alphaAnimation2.setInterpolator(QUART_EASE_OUT_INTERPOLATOR);
        Interpolator interpolator = SCALE_DOWN_PHYSIC_BASED_INTERPOLATOR;
        radiusAnimation.setInterpolator(interpolator);
        scaleWithClipAnimation.setInterpolator(interpolator);
        set2.setZAdjustment(1);
        alphaAnimation2.setDuration(200L);
        radiusAnimation.setDuration(450L);
        scaleWithClipAnimation.setDuration(450L);
        return set2;
    }

    static Animation createLaunchActivityFromRoundedViewAnimation(int transit, boolean enter, Rect appFrame, Rect positionRect, float radius) {
        int appWidth = appFrame.width();
        int appHeight = appFrame.height();
        int startX = positionRect.left;
        int startY = positionRect.top;
        int startWidth = positionRect.width();
        int startHeight = positionRect.height();
        if (!enter) {
            AnimationSet set = new AnimationSet(false);
            Animation alphaAnimation = new AlphaAnimation(1.0f, 0.8f);
            set.addAnimation(alphaAnimation);
            alphaAnimation.setInterpolator(SCALE_UP_PHYSIC_BASED_INTERPOLATOR);
            set.setZAdjustment(-1);
            alphaAnimation.setDuration(500L);
            set.setFillAfter(true);
            return set;
        }
        Animation alphaAnimation2 = new AlphaAnimation((float) MiuiFreeformPinManagerService.EDGE_AREA, 1.0f);
        Animation radiusAnimation = new RadiusAnimation(radius, DISPLAY_ROUND_CORNER_RADIUS);
        float scaleX = startWidth / appWidth;
        float scaleY = startHeight / appHeight;
        ScaleAnimation scaleAnimation = new ScaleAnimation(scaleX, 1.0f, scaleY, 1.0f, startX / (1.0f - scaleX), startY / (1.0f - scaleY));
        scaleAnimation.initialize(appWidth, appHeight, appWidth, appHeight);
        AnimationSet set2 = new AnimationSet(false);
        set2.addAnimation(alphaAnimation2);
        set2.addAnimation(radiusAnimation);
        set2.addAnimation(scaleAnimation);
        alphaAnimation2.setInterpolator(QUART_EASE_OUT_INTERPOLATOR);
        Interpolator interpolator = SCALE_UP_PHYSIC_BASED_INTERPOLATOR;
        radiusAnimation.setInterpolator(interpolator);
        scaleAnimation.setInterpolator(interpolator);
        set2.setZAdjustment(1);
        alphaAnimation2.setDuration(100L);
        radiusAnimation.setDuration(500L);
        scaleAnimation.setDuration(500L);
        return set2;
    }

    static Animation createBackActivityFromRoundedViewAnimation(boolean enter, Rect appFrame, Rect targetPositionRect, float radius) {
        Rect positionRect = new Rect(targetPositionRect);
        if (enter) {
            AnimationSet set = new AnimationSet(false);
            Animation alphaAnimation = new AlphaAnimation(0.8f, 1.0f);
            set.addAnimation(alphaAnimation);
            alphaAnimation.setInterpolator(SCALE_DOWN_PHYSIC_BASED_INTERPOLATOR);
            set.setZAdjustment(-1);
            alphaAnimation.setDuration(200L);
            return set;
        }
        Animation alphaAnimation2 = new AlphaAnimation(1.0f, (float) MiuiFreeformPinManagerService.EDGE_AREA);
        alphaAnimation2.setStartOffset(100L);
        Animation radiusAnimation = new RadiusAnimation(DISPLAY_ROUND_CORNER_RADIUS, radius);
        float scaleX = positionRect.width() / appFrame.width();
        Animation scaleXAnimation = new ScaleXAnimation(1.0f, scaleX, positionRect.left / (1.0f - scaleX));
        float scaleY = positionRect.height() / appFrame.height();
        Animation scaleYAnimation = new ScaleYAnimation(1.0f, scaleY, positionRect.top / (1.0f - scaleY));
        AnimationSet set2 = new AnimationSet(true);
        set2.addAnimation(alphaAnimation2);
        set2.addAnimation(radiusAnimation);
        set2.addAnimation(scaleXAnimation);
        set2.addAnimation(scaleYAnimation);
        alphaAnimation2.setInterpolator(QUART_EASE_OUT_INTERPOLATOR);
        Interpolator interpolator = SCALE_DOWN_PHYSIC_BASED_INTERPOLATOR;
        radiusAnimation.setInterpolator(interpolator);
        scaleXAnimation.setInterpolator(interpolator);
        scaleYAnimation.setInterpolator(interpolator);
        set2.setZAdjustment(1);
        alphaAnimation2.setDuration(200L);
        radiusAnimation.setDuration(450L);
        scaleXAnimation.setDuration(450L);
        scaleYAnimation.setDuration(450L);
        return set2;
    }

    public static Animation createBackActivityScaledToScreenCenter(boolean enter, Rect appFrame) {
        Rect positionRect = new Rect();
        int scaleWith = (int) (appFrame.width() * 0.6f);
        int scaleHeight = (int) (appFrame.height() * 0.6f);
        positionRect.left = appFrame.left + ((appFrame.width() - scaleWith) / 2);
        positionRect.top = appFrame.top + ((appFrame.height() - scaleHeight) / 2);
        positionRect.right = appFrame.right - ((appFrame.width() - scaleWith) / 2);
        positionRect.bottom = appFrame.bottom - ((appFrame.height() - scaleHeight) / 2);
        if (enter) {
            AnimationSet set = new AnimationSet(true);
            Animation alphaAnimation = new AlphaAnimation(0.8f, 1.0f);
            set.addAnimation(alphaAnimation);
            set.setInterpolator(SCALE_DOWN_PHYSIC_BASED_INTERPOLATOR);
            set.setZAdjustment(-1);
            set.setDuration(450L);
            return set;
        }
        Animation alphaAnimation2 = new AlphaAnimation(1.0f, (float) MiuiFreeformPinManagerService.EDGE_AREA);
        float scaleX = positionRect.width() / appFrame.width();
        Animation scaleXAnimation = new ScaleXAnimation(1.0f, scaleX, positionRect.left / (1.0f - scaleX));
        float scaleY = positionRect.height() / appFrame.height();
        Animation scaleYAnimation = new ScaleYAnimation(1.0f, scaleY, positionRect.top / (1.0f - scaleY));
        AnimationSet set2 = new AnimationSet(true);
        set2.addAnimation(alphaAnimation2);
        set2.addAnimation(scaleXAnimation);
        set2.addAnimation(scaleYAnimation);
        set2.setInterpolator(SCALE_DOWN_PHYSIC_BASED_INTERPOLATOR);
        set2.setZAdjustment(1);
        set2.setDuration(450L);
        return set2;
    }

    public static void addAnimationListener(Animation a, Handler handler, IRemoteCallback mAnimationStartCallback, IRemoteCallback mAnimationFinishCallback) {
        a.setAnimationListener(new AnonymousClass2(mAnimationStartCallback, handler, mAnimationFinishCallback));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.android.server.wm.AppTransitionInjector$2 */
    /* loaded from: classes.dex */
    public class AnonymousClass2 implements Animation.AnimationListener {
        final /* synthetic */ IRemoteCallback val$exitFinishCallback;
        final /* synthetic */ IRemoteCallback val$exitStartCallback;
        final /* synthetic */ Handler val$handler;

        AnonymousClass2(IRemoteCallback iRemoteCallback, Handler handler, IRemoteCallback iRemoteCallback2) {
            this.val$exitStartCallback = iRemoteCallback;
            this.val$handler = handler;
            this.val$exitFinishCallback = iRemoteCallback2;
        }

        @Override // android.view.animation.Animation.AnimationListener
        public void onAnimationStart(Animation animation) {
            if (this.val$exitStartCallback != null) {
                this.val$handler.sendMessage(PooledLambda.obtainMessage(new Consumer() { // from class: com.android.server.wm.AppTransitionInjector$2$$ExternalSyntheticLambda0
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        AppTransitionInjector.doAnimationCallback((IRemoteCallback) obj);
                    }
                }, this.val$exitStartCallback));
            }
        }

        @Override // android.view.animation.Animation.AnimationListener
        public void onAnimationEnd(Animation animation) {
            if (this.val$exitFinishCallback != null) {
                this.val$handler.sendMessage(PooledLambda.obtainMessage(new Consumer() { // from class: com.android.server.wm.AppTransitionInjector$2$$ExternalSyntheticLambda1
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        AppTransitionInjector.doAnimationCallback((IRemoteCallback) obj);
                    }
                }, this.val$exitFinishCallback));
            }
        }

        @Override // android.view.animation.Animation.AnimationListener
        public void onAnimationRepeat(Animation animation) {
        }
    }

    public static void doAnimationCallback(IRemoteCallback callback) {
        try {
            callback.sendResult((Bundle) null);
        } catch (RemoteException e) {
        }
    }

    public static boolean isUseFreeFormAnimation(int transit) {
        if (transit == 24 || transit == 6 || transit == 25 || transit == 7) {
            return false;
        }
        return true;
    }

    public static Animation loadFreeFormAnimation(WindowManagerService service, int transit, boolean enter, Rect frame, WindowContainer container) {
        if (container instanceof WindowState) {
            container = ((WindowState) container).getTask();
        }
        if (container instanceof ActivityRecord) {
            container = ((ActivityRecord) container).getTask();
        }
        if (container == null || !(container instanceof Task)) {
            return null;
        }
        int rootTaskId = ((Task) container).getRootTaskId();
        MiuiFreeFormActivityStackStub freemAsstub = service.mAtmService.mMiuiFreeFormManagerService.getMiuiFreeFormActivityStack(rootTaskId);
        if (freemAsstub == null || !(freemAsstub instanceof MiuiFreeFormActivityStack)) {
            return null;
        }
        MiuiFreeFormActivityStack freemAs = (MiuiFreeFormActivityStack) freemAsstub;
        boolean z = true;
        if (transit == 8 || transit == 10 || transit == 9 || transit == 11 || transit == 0) {
            if (freemAs.mNeedAnimation) {
                Animation a = createFreeFormAppOpenAndExitAnimation(service, enter, frame, freemAs);
                return a;
            }
            freemAs.mNeedAnimation = true;
            return null;
        } else if (transit == 6 || transit == 7) {
            return null;
        } else {
            if (transit != 50 && transit != 51) {
                return null;
            }
            if (transit != 51) {
                z = false;
            }
            Animation a2 = createFloatWindowShowHideAnimation(z, frame, freemAs);
            return a2;
        }
    }

    static Animation createFreeFormActivityOpenAndExitAnimation(boolean enter) {
        Animation translateAnimation;
        if (enter) {
            translateAnimation = new AlphaAnimation((float) MiuiFreeformPinManagerService.EDGE_AREA, 1.0f);
        } else {
            translateAnimation = new AlphaAnimation(1.0f, (float) MiuiFreeformPinManagerService.EDGE_AREA);
        }
        translateAnimation.setDuration(500L);
        return translateAnimation;
    }

    static Animation createFreeFormAppOpenAndExitAnimation(WindowManagerService service, boolean enter, Rect frame, MiuiFreeFormActivityStack freemAs) {
        AnimationSet set = null;
        if (freemAs != null) {
            set = new AnimationSet(false);
            if (enter) {
                Animation alphaAnimation = new AlphaAnimation((float) MiuiFreeformPinManagerService.EDGE_AREA, 1.0f);
                alphaAnimation.setDuration(500L);
                alphaAnimation.setInterpolator(new PhysicBasedInterpolator(0.85f, 0.7f));
                float windowHeight = frame.height() * freemAs.mFreeFormScale;
                float windowWidth = frame.width() * freemAs.mFreeFormScale;
                Animation scaleAnimation = new ScaleAnimation(0.6f, 1.0f, 0.6f, 1.0f, 0, windowWidth / 2.0f, 0, windowHeight / 2.0f);
                scaleAnimation.setDuration(500L);
                scaleAnimation.setInterpolator(new PhysicBasedInterpolator(0.85f, 0.7f));
                set.addAnimation(alphaAnimation);
                set.addAnimation(scaleAnimation);
                set.setZAdjustment(1);
            } else {
                Animation alphaAnimation2 = new AlphaAnimation(1.0f, (float) MiuiFreeformPinManagerService.EDGE_AREA);
                alphaAnimation2.setDuration(500L);
                alphaAnimation2.setInterpolator(new PhysicBasedInterpolator(0.85f, 0.7f));
                float windowHeight2 = frame.height() * freemAs.mFreeFormScale;
                float windowWidth2 = frame.width() * freemAs.mFreeFormScale;
                Animation scaleAnimation2 = new ScaleAnimation(1.0f, 0.6f, 1.0f, 0.6f, 0, windowWidth2 / 2.0f, 0, windowHeight2 / 2.0f);
                scaleAnimation2.setDuration(500L);
                scaleAnimation2.setInterpolator(new PhysicBasedInterpolator(0.85f, 0.7f));
                set.addAnimation(alphaAnimation2);
                set.addAnimation(scaleAnimation2);
                set.setZAdjustment(1);
            }
        }
        return set;
    }

    public static boolean isUseFloatingAnimation(Animation a) {
        if (a instanceof AnimationSet) {
            for (Animation animation : ((AnimationSet) a).getAnimations()) {
                if (animation instanceof TranslateWithClipAnimation) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    static Animation createFloatWindowShowHideAnimation(boolean enter, Rect frame, MiuiFreeFormActivityStack freemAs) {
        Slog.e(TAG, "createFloatWindowShowHideAnimation enter = " + enter);
        float freeFormScale = 1.0f;
        boolean needSetRoundedCorners = false;
        if (freemAs != null) {
            needSetRoundedCorners = true;
            freeFormScale = freemAs.mFreeFormScale;
        }
        AnimationSet set = new AnimationSet(false);
        float f = MiuiFreeformPinManagerService.EDGE_AREA;
        if (enter) {
            Animation alphaAnimation = new AlphaAnimation((float) MiuiFreeformPinManagerService.EDGE_AREA, 1.0f);
            alphaAnimation.setDuration(350L);
            alphaAnimation.setInterpolator(new DecelerateInterpolator(1.0f));
            float windowHeight = frame.height() * freeFormScale;
            float windowWidth = frame.width() * freeFormScale;
            float maxSide = windowHeight > windowWidth ? windowHeight : windowWidth;
            if (maxSide > 80.0f) {
                f = (maxSide - 80.0f) / maxSide;
            }
            float scaleEnd = f;
            Animation scaleAnimation = new ScaleAnimation(scaleEnd, 1.0f, scaleEnd, 1.0f, 0, windowWidth / 2.0f, 0, windowHeight / 2.0f);
            scaleAnimation.setDuration(350L);
            scaleAnimation.setInterpolator(new DecelerateInterpolator(1.0f));
            set.addAnimation(alphaAnimation);
            set.addAnimation(scaleAnimation);
            set.setZAdjustment(1);
            if (needSetRoundedCorners) {
                set.setHasRoundedCorners(true);
            }
        } else {
            Animation alphaAnimation2 = new AlphaAnimation(1.0f, (float) MiuiFreeformPinManagerService.EDGE_AREA);
            alphaAnimation2.setDuration(200L);
            alphaAnimation2.setInterpolator(new DecelerateInterpolator(1.5f));
            float windowHeight2 = frame.height() * freeFormScale;
            float windowWidth2 = frame.width() * freeFormScale;
            float maxSide2 = windowHeight2 > windowWidth2 ? windowHeight2 : windowWidth2;
            if (maxSide2 > 80.0f) {
                f = (maxSide2 - 80.0f) / maxSide2;
            }
            float scaleEnd2 = f;
            Animation scaleAnimation2 = new ScaleAnimation(1.0f, scaleEnd2, 1.0f, scaleEnd2, 0, windowWidth2 / 2.0f, 0, windowHeight2 / 2.0f);
            scaleAnimation2.setDuration(200L);
            scaleAnimation2.setInterpolator(new DecelerateInterpolator(1.5f));
            set.addAnimation(alphaAnimation2);
            set.addAnimation(scaleAnimation2);
            set.setZAdjustment(1);
            if (needSetRoundedCorners) {
                set.setHasRoundedCorners(true);
            }
        }
        return set;
    }

    static Animation createWallPaperOpenAnimation(boolean enter, Rect appFrame, Rect positionRect) {
        return createWallPaperOpenAnimation(enter, appFrame, positionRect, (Rect) null);
    }

    static Animation createWallPaperOpenAnimation(boolean enter, Rect appFrame, Rect positionRect, Rect startRect) {
        return createWallPaperOpenAnimation(enter, appFrame, positionRect, startRect, 1);
    }

    static Animation createWallPaperOpenAnimation(boolean enter, Rect appFrame, Rect positionRect, int orientation) {
        return createWallPaperOpenAnimation(enter, appFrame, positionRect, null, orientation);
    }

    static Animation createWallPaperOpenAnimation(boolean enter, Rect appFrame, Rect positionRect, Rect startRect, int orientation) {
        return createTransitionAnimation(enter, appFrame, positionRect, null, orientation, null);
    }

    static Animation createTransitionAnimation(boolean enter, Rect appFrame, Rect targetPositionRect, Rect startRect, int orientation, Point inertia) {
        int insetBottom;
        int targetX;
        int insetRight;
        int targetY;
        int startCenterX;
        float startScaleX;
        int startY;
        int startX;
        Animation translateXAnimation;
        Animation translateYAnimation;
        AnimationSet set;
        Animation alphaAnimation;
        Rect positionRect = new Rect(targetPositionRect);
        boolean hasStartRect = startRect != null && !startRect.isEmpty();
        boolean isPortrait = orientation == 1;
        int appWidth = appFrame.width();
        int appHeight = appFrame.height();
        boolean canFindPosition = !positionRect.isEmpty();
        int insetLeft = sMiuiAnimSupportInset.left;
        int insetTop = sMiuiAnimSupportInset.top;
        int insetRight2 = sMiuiAnimSupportInset.right;
        int insetBottom2 = sMiuiAnimSupportInset.bottom;
        if (hasStartRect) {
            startRect.offset(-appFrame.left, -appFrame.top);
        }
        if (canFindPosition) {
            positionRect.offset(-appFrame.left, -appFrame.top);
        }
        int targetWidth = canFindPosition ? positionRect.width() - (insetLeft + insetRight2) : (int) (appWidth * 0.4f);
        int targetHeight = canFindPosition ? positionRect.height() - (insetTop + insetBottom2) : (int) (appHeight * 0.4f);
        int startX2 = hasStartRect ? startRect.left : 0;
        int startY2 = hasStartRect ? startRect.top : 0;
        int startCenterX2 = hasStartRect ? startX2 + (startRect.width() / 2) : appFrame.left + (appFrame.width() / 2);
        int startCenterY = hasStartRect ? startY2 + (startRect.height() / 2) : appFrame.top + (appFrame.height() / 2);
        if (canFindPosition) {
            insetBottom = insetBottom2;
            targetX = positionRect.left + insetLeft;
        } else {
            insetBottom = insetBottom2;
            targetX = (int) ((appWidth * 0.6f) / 2.0f);
        }
        if (canFindPosition) {
            insetRight = insetRight2;
            targetY = positionRect.top + insetTop;
        } else {
            insetRight = insetRight2;
            targetY = (int) ((appHeight * 0.6f) / 2.0f);
        }
        if (hasStartRect) {
            startCenterX = startCenterX2;
            startScaleX = startRect.width() / appWidth;
        } else {
            startCenterX = startCenterX2;
            startScaleX = 1.0f;
        }
        float startScaleY = hasStartRect ? startRect.height() / appHeight : 1.0f;
        float scaleX = targetWidth / appWidth;
        float scaleY = targetHeight / appHeight;
        if (enter) {
            AnimationSet set2 = new AnimationSet(true);
            Animation scaleAnimation = new ScaleAnimation(0.8f, 1.0f, 0.8f, 1.0f, appWidth / 2.0f, appHeight / 2.0f);
            scaleAnimation.setDuration(300L);
            if (!IS_E10) {
                set2.addAnimation(scaleAnimation);
            }
            set2.setInterpolator(CUBIC_EASE_OUT_INTERPOLATOR);
            set2.setZAdjustment(-1);
            return set2;
        }
        int targetWidth2 = targetWidth;
        if (canFindPosition) {
            AnimationSet set3 = new AnimationSet(false);
            Animation alphaAnimation2 = new AlphaAnimation(1.0f, (float) MiuiFreeformPinManagerService.EDGE_AREA);
            alphaAnimation2.setStartOffset(40L);
            alphaAnimation2.setDuration(210L);
            Interpolator interpolator = CUBIC_EASE_OUT_INTERPOLATOR;
            alphaAnimation2.setInterpolator(interpolator);
            if (inertia == null) {
                translateXAnimation = new TranslateXAnimation(startX2, appFrame.left + targetX);
                translateYAnimation = new TranslateYAnimation(startY2, appFrame.top + targetY);
                Animation scaleXAnimation = new ScaleXAnimation(startScaleX, scaleX);
                scaleXAnimation.setInterpolator(isPortrait ? interpolator : QUINT_EASE_OUT_INTERPOLATOR);
                scaleXAnimation.setDuration(300L);
                Animation scaleYAnimation = new ScaleYAnimation(startScaleY, scaleY);
                scaleYAnimation.setInterpolator(QUART_EASE_OUT_INTERPOLATOR);
                startY = startY2;
                startX = startX2;
                scaleYAnimation.setDuration(300L);
                translateXAnimation.setDuration(300L);
                translateYAnimation.setDuration(300L);
                set3.addAnimation(scaleXAnimation);
                set3.addAnimation(scaleYAnimation);
                set = set3;
                alphaAnimation = alphaAnimation2;
            } else {
                startY = startY2;
                startX = startX2;
                float endCenterX = positionRect.left + insetLeft + (targetWidth2 / 2);
                float endCenterY = positionRect.top + insetTop + (targetHeight / 2);
                alphaAnimation = alphaAnimation2;
                set = set3;
                translateXAnimation = new BezierXAnimation(startScaleX, scaleX, startCenterX, endCenterX, inertia, appWidth);
                translateYAnimation = new BezierYAnimation(startScaleY, scaleY, startCenterY, endCenterY, inertia, appHeight);
                translateXAnimation.setDuration(400L);
                translateYAnimation.setDuration(400L);
            }
            translateXAnimation.setInterpolator(isPortrait ? QUART_EASE_OUT_INTERPOLATOR : QUINT_EASE_OUT_INTERPOLATOR);
            translateYAnimation.setInterpolator(QUART_EASE_OUT_INTERPOLATOR);
            set.addAnimation(alphaAnimation);
            set.addAnimation(translateXAnimation);
            set.addAnimation(translateYAnimation);
            set.setZAdjustment(1);
            return set;
        }
        int startX3 = startX2;
        AnimationSet set4 = new AnimationSet(true);
        Animation scaleAnimation2 = new ScaleAnimation(startScaleX, 0.4f, startScaleY, 0.4f);
        Animation translateAnimation = new TranslateAnimation(startX3, targetX, startY2, targetY);
        Animation alphaAnimation3 = new AlphaAnimation(1.0f, (float) MiuiFreeformPinManagerService.EDGE_AREA);
        set4.addAnimation(scaleAnimation2);
        set4.addAnimation(translateAnimation);
        set4.addAnimation(alphaAnimation3);
        set4.setDuration(300L);
        set4.setInterpolator(CUBIC_EASE_OUT_INTERPOLATOR);
        if (IS_E10) {
            set4.setZAdjustment(1);
        }
        return set4;
    }

    static Animation createDummyAnimation(float alpha) {
        Animation dummyAnimation = new AlphaAnimation(alpha, alpha);
        dummyAnimation.setDuration(300L);
        return dummyAnimation;
    }

    static Animation createTaskOpenCloseTransition(boolean enter, Rect appFrame, boolean isOpenOrClose) {
        Animation scaleAnimation;
        Animation translateAnimation;
        AnimationSet set = new AnimationSet(true);
        int width = appFrame.width();
        int heigth = appFrame.height();
        if (isOpenOrClose) {
            if (enter) {
                translateAnimation = new TranslateAnimation(1, 1.0f, 1, MiuiFreeformPinManagerService.EDGE_AREA, 1, MiuiFreeformPinManagerService.EDGE_AREA, 1, MiuiFreeformPinManagerService.EDGE_AREA);
                scaleAnimation = new ScaleAnimation(0.9f, 1.0f, 0.9f, 1.0f, width / 2, heigth / 2);
                scaleAnimation.setStartOffset(150L);
            } else {
                scaleAnimation = new ScaleAnimation(1.0f, 0.9f, 1.0f, 0.9f, width / 2, heigth / 2);
                translateAnimation = new TranslateAnimation(1, MiuiFreeformPinManagerService.EDGE_AREA, 1, -1.0f, 1, MiuiFreeformPinManagerService.EDGE_AREA, 1, MiuiFreeformPinManagerService.EDGE_AREA);
            }
        } else if (enter) {
            translateAnimation = new TranslateAnimation(1, -1.0f, 1, MiuiFreeformPinManagerService.EDGE_AREA, 1, MiuiFreeformPinManagerService.EDGE_AREA, 1, MiuiFreeformPinManagerService.EDGE_AREA);
            scaleAnimation = new ScaleAnimation(0.9f, 1.0f, 0.9f, 1.0f, width / 2, heigth / 2);
            scaleAnimation.setStartOffset(150L);
        } else {
            scaleAnimation = new ScaleAnimation(1.0f, 0.9f, 1.0f, 0.9f, width / 2, heigth / 2);
            translateAnimation = new TranslateAnimation(1, MiuiFreeformPinManagerService.EDGE_AREA, 1, 1.0f, 1, MiuiFreeformPinManagerService.EDGE_AREA, 1, MiuiFreeformPinManagerService.EDGE_AREA);
        }
        sActivityTransitionInterpolator = new ActivityTranstionInterpolator(0.6f, 0.99f);
        set.addAnimation(scaleAnimation);
        set.addAnimation(translateAnimation);
        set.setInterpolator(sActivityTransitionInterpolator);
        set.setDuration(650L);
        set.setHasRoundedCorners(true);
        return set;
    }

    static Animation createActivityOpenCloseTransition(boolean enter, Rect appFrame, boolean isOpenOrClose, float freeformScale, WindowContainer container) {
        Animation translateAnimation;
        AnimationSet set = new AnimationSet(true);
        if (isOpenOrClose) {
            if (enter) {
                translateAnimation = new TranslateAnimation(1, freeformScale, 1, MiuiFreeformPinManagerService.EDGE_AREA, 1, MiuiFreeformPinManagerService.EDGE_AREA, 1, MiuiFreeformPinManagerService.EDGE_AREA);
                set.setZAdjustment(1);
            } else {
                Animation translateAnimation2 = new TranslateAnimation(1, MiuiFreeformPinManagerService.EDGE_AREA, 1, -0.25f, 1, MiuiFreeformPinManagerService.EDGE_AREA, 1, MiuiFreeformPinManagerService.EDGE_AREA);
                if (freeformScale < 1.0f) {
                    Animation alphaAnimation = new AlphaAnimation(1.0f, 1.0f);
                    set.addAnimation(alphaAnimation);
                    translateAnimation = translateAnimation2;
                } else if (container == null) {
                    Animation alphaAnimation2 = new AlphaAnimation(1.0f, 0.5f);
                    set.addAnimation(alphaAnimation2);
                    translateAnimation = translateAnimation2;
                } else if (container.mNeedReplaceTaskAnimation) {
                    Animation alphaAnimation3 = new AlphaAnimation(1.0f, 0.5f);
                    set.addAnimation(alphaAnimation3);
                    set.setTaskAnimationLevel(true);
                    container.mNeedReplaceTaskAnimation = false;
                    translateAnimation = translateAnimation2;
                } else if (!container.fillsParent()) {
                    translateAnimation = translateAnimation2;
                } else {
                    AnimationDimmer animationDimmer = new AnimationDimmer(MiuiFreeformPinManagerService.EDGE_AREA, 0.5f, appFrame, container);
                    set.addAnimation(animationDimmer);
                    translateAnimation = translateAnimation2;
                }
            }
        } else if (enter) {
            Animation translateAnimation3 = new TranslateAnimation(1, -0.25f, 1, MiuiFreeformPinManagerService.EDGE_AREA, 1, MiuiFreeformPinManagerService.EDGE_AREA, 1, MiuiFreeformPinManagerService.EDGE_AREA);
            if (freeformScale < 1.0f) {
                Animation alphaAnimation4 = new AlphaAnimation(1.0f, 1.0f);
                set.addAnimation(alphaAnimation4);
                translateAnimation = translateAnimation3;
            } else if (container == null) {
                Animation alphaAnimation5 = new AlphaAnimation(0.5f, 1.0f);
                set.addAnimation(alphaAnimation5);
                translateAnimation = translateAnimation3;
            } else if (container.mNeedReplaceTaskAnimation) {
                Animation alphaAnimation6 = new AlphaAnimation(0.5f, 1.0f);
                set.addAnimation(alphaAnimation6);
                set.setTaskAnimationLevel(true);
                container.mNeedReplaceTaskAnimation = false;
                translateAnimation = translateAnimation3;
            } else if (!container.fillsParent()) {
                translateAnimation = translateAnimation3;
            } else {
                AnimationDimmer animationDimmer2 = new AnimationDimmer(0.5f, MiuiFreeformPinManagerService.EDGE_AREA, appFrame, container);
                set.addAnimation(animationDimmer2);
                translateAnimation = translateAnimation3;
            }
        } else {
            translateAnimation = new TranslateAnimation(1, MiuiFreeformPinManagerService.EDGE_AREA, 1, freeformScale, 1, MiuiFreeformPinManagerService.EDGE_AREA, 1, MiuiFreeformPinManagerService.EDGE_AREA);
            set.setZAdjustment(1);
        }
        sActivityTransitionInterpolator = new ActivityTranstionInterpolator(0.8f, 0.95f);
        set.addAnimation(translateAnimation);
        set.setInterpolator(sActivityTransitionInterpolator);
        set.setDuration(500L);
        set.setHasRoundedCorners(true);
        if (isOpenOrClose != enter) {
            set.setHasRoundedCorners(false);
        }
        return set;
    }

    static Animation createActivityOpenCloseTransitionForDimmer(boolean enter, Rect appFrame, boolean isOpenOrClose, WindowContainer container, WindowManagerService service) {
        AnimationSet set = new AnimationSet(true);
        AnimationDimmer animationDimmer = null;
        appFrame.set(0, 0, appFrame.width(), appFrame.height());
        if (isOpenOrClose) {
            if (enter) {
                TranslateAnimation translateAnimation = new TranslateAnimation(1, 1.0f, 1, MiuiFreeformPinManagerService.EDGE_AREA, 1, MiuiFreeformPinManagerService.EDGE_AREA, 1, MiuiFreeformPinManagerService.EDGE_AREA);
                if (container instanceof ActivityRecord) {
                    animationDimmer = new AnimationDimmer(MiuiFreeformPinManagerService.EDGE_AREA, 0.5f, appFrame, ((ActivityRecord) container).getTask());
                }
                if (container instanceof Task) {
                    animationDimmer = new AnimationDimmer(MiuiFreeformPinManagerService.EDGE_AREA, 0.5f, appFrame, container);
                }
                animationDimmer.setZAdjustment(translateAnimation.getZAdjustment() - 1);
                set.addAnimation(translateAnimation);
                set.addAnimation(animationDimmer);
            }
        } else if (!enter) {
            TranslateAnimation translateAnimation2 = new TranslateAnimation(1, MiuiFreeformPinManagerService.EDGE_AREA, 1, 1.0f, 1, MiuiFreeformPinManagerService.EDGE_AREA, 1, MiuiFreeformPinManagerService.EDGE_AREA);
            if (container instanceof ActivityRecord) {
                animationDimmer = new AnimationDimmer(0.5f, MiuiFreeformPinManagerService.EDGE_AREA, appFrame, ((ActivityRecord) container).getTask());
            }
            if (container instanceof Task) {
                animationDimmer = new AnimationDimmer(0.5f, MiuiFreeformPinManagerService.EDGE_AREA, appFrame, container);
            }
            animationDimmer.setZAdjustment(translateAnimation2.getZAdjustment() - 1);
            set.addAnimation(translateAnimation2);
            set.addAnimation(animationDimmer);
        }
        ActivityTranstionInterpolator activityTranstionInterpolator = new ActivityTranstionInterpolator(0.8f, 0.95f);
        sActivityTransitionInterpolator = activityTranstionInterpolator;
        set.setInterpolator(activityTranstionInterpolator);
        set.setDuration(500L);
        set.setHasRoundedCorners(true);
        return set;
    }

    static Animation createWallerOpenCloseTransitionAnimation(boolean enter, Rect appFrame, boolean isOpenOrClose) {
        AnimationSet set = new AnimationSet(true);
        appFrame.width();
        appFrame.height();
        if (enter) {
            Animation scaleAnimation = new ScaleAnimation(0.5f, 1.0f, 0.5f, 1.0f, 1, 0.5f, 1, 0.5f);
            Animation alphaAnimation = new AlphaAnimation((float) MiuiFreeformPinManagerService.EDGE_AREA, 1.0f);
            set.addAnimation(alphaAnimation);
            set.addAnimation(scaleAnimation);
        } else {
            Animation alphaAnimation2 = new AlphaAnimation(1.0f, (float) MiuiFreeformPinManagerService.EDGE_AREA);
            set.addAnimation(alphaAnimation2);
        }
        ActivityTranstionInterpolator activityTranstionInterpolator = new ActivityTranstionInterpolator(0.65f, 0.95f);
        sActivityTransitionInterpolator = activityTranstionInterpolator;
        set.setInterpolator(activityTranstionInterpolator);
        set.setDuration(isCTS() ? 400L : 550L);
        set.setHasRoundedCorners(true);
        return set;
    }

    public static Animation loadAnimationNotCheckForDimmer(int transit, boolean enter, Rect frame, WindowContainer container, WindowManagerService service) {
        switch (transit) {
            case 6:
                Animation defaultAnimation = createActivityOpenCloseTransitionForDimmer(enter, frame, true, container, service);
                return defaultAnimation;
            case 7:
                Animation defaultAnimation2 = createActivityOpenCloseTransitionForDimmer(enter, frame, false, container, service);
                return defaultAnimation2;
            default:
                return null;
        }
    }

    static void calculateMiuiThumbnailSpec(Rect appRect, Rect thumbnailRect, Matrix curSpec, float alpha, SurfaceControl.Transaction t, SurfaceControl leash) {
        if (appRect == null || thumbnailRect == null || curSpec == null || leash == null) {
            return;
        }
        if (t == null) {
            return;
        }
        float[] tmp = new float[9];
        curSpec.getValues(tmp);
        float curScaleX = tmp[0];
        float curScaleY = tmp[4];
        float curWidth = appRect.width() * curScaleX;
        float curHeight = appRect.height() * curScaleY;
        float newScaleX = curWidth / ((thumbnailRect.width() - sMiuiAnimSupportInset.left) - sMiuiAnimSupportInset.right);
        float newScaleY = curHeight / ((thumbnailRect.height() - sMiuiAnimSupportInset.top) - sMiuiAnimSupportInset.bottom);
        float curTranslateX = tmp[2] - (sMiuiAnimSupportInset.left * newScaleX);
        float curTranslateY = tmp[5] - (sMiuiAnimSupportInset.top * newScaleY);
        t.setMatrix(leash, newScaleX, tmp[3], tmp[1], newScaleY);
        t.setPosition(leash, curTranslateX, curTranslateY);
        t.setAlpha(leash, alpha);
    }

    static void calculateMiuiActivityThumbnailSpec(Rect appRect, Rect thumbnailRect, Matrix curSpec, float alpha, float radius, SurfaceControl.Transaction t, SurfaceControl leash) {
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

    static void calculateScaleUpDownThumbnailSpec(Rect appClipRect, Rect thumbnailRect, Matrix curSpec, SurfaceControl.Transaction t, SurfaceControl leash) {
        if (appClipRect == null || thumbnailRect == null || curSpec == null || leash == null || t == null) {
            return;
        }
        float[] tmp = new float[9];
        t.setMatrix(leash, curSpec, tmp);
        t.setWindowCrop(leash, appClipRect);
        t.setAlpha(leash, 1.0f);
    }

    static void calculateGestureThumbnailSpec(Rect appRect, Rect thumbnailRect, Matrix curSpec, float alpha, SurfaceControl.Transaction t, SurfaceControl leash) {
        if (appRect == null || thumbnailRect == null || curSpec == null || leash == null) {
            return;
        }
        if (t == null) {
            return;
        }
        Matrix tmpMatrix = new Matrix(curSpec);
        tmpMatrix.postTranslate(appRect.left, appRect.top);
        float[] tmp = new float[9];
        tmpMatrix.getValues(tmp);
        float curScaleX = tmp[0];
        float curScaleY = tmp[4];
        float curWidth = appRect.width() * curScaleX;
        float curHeight = appRect.height() * curScaleY;
        float newScaleX = curWidth / ((thumbnailRect.width() - sMiuiAnimSupportInset.left) - sMiuiAnimSupportInset.right);
        float newScaleY = curHeight / ((thumbnailRect.height() - sMiuiAnimSupportInset.top) - sMiuiAnimSupportInset.bottom);
        float curThumbnailHeight = newScaleX * ((thumbnailRect.height() - sMiuiAnimSupportInset.top) - sMiuiAnimSupportInset.bottom);
        float curTranslateX = tmp[2] - (sMiuiAnimSupportInset.left * newScaleX);
        float curTranslateY = (tmp[5] - (sMiuiAnimSupportInset.top * newScaleY)) + ((curHeight - curThumbnailHeight) / 2.0f);
        t.setMatrix(leash, newScaleX, tmp[3], tmp[1], newScaleX);
        t.setPosition(leash, curTranslateX, curTranslateY);
        t.setAlpha(leash, alpha);
    }

    static void setMiuiAnimSupportInset(Rect inset) {
        if (inset == null) {
            sMiuiAnimSupportInset.setEmpty();
        } else {
            sMiuiAnimSupportInset.set(inset);
        }
    }

    static boolean allowCustomAnimation(ArraySet<ActivityRecord> closingApps) {
        WindowState win;
        if (closingApps == null) {
            return false;
        }
        int size = closingApps.size();
        for (int i = 0; i < size; i++) {
            ActivityRecord atoken = closingApps.valueAt(i);
            if (atoken != null && (win = atoken.findMainWindow()) != null && WHITE_LIST_ALLOW_CUSTOM_ANIMATION.contains(win.mAttrs.packageName)) {
                return true;
            }
        }
        return false;
    }

    static Animation loadDefaultAnimation(WindowManager.LayoutParams lp, int transit, boolean enter, Rect frame) {
        if (!useDefaultAnimationAttr(lp)) {
            return null;
        }
        Animation defaultAnimation = loadDefaultAnimationNotCheck(lp, transit, enter, frame, null, null);
        return defaultAnimation;
    }

    static Animation loadAnimationSafely(Context context, int resId) {
        try {
            return AnimationUtils.loadAnimation(context, resId);
        } catch (Exception e) {
            Slog.w(TAG, "Unable to load animation resource", e);
            return null;
        }
    }

    private static int updateToTranslucentAnimIfNeeded(int transit) {
        if (transit == 24) {
            return 17432594;
        }
        if (transit == 25) {
            return 17432593;
        }
        return 0;
    }

    public static Animation loadDefaultAnimationNotCheck(WindowManager.LayoutParams lp, int transit, boolean enter, Rect frame, WindowContainer container, WindowManagerService service) {
        boolean isSwitchAnimationType;
        MiuiFreeFormActivityStack freemAs = null;
        float freeformScale = 1.0f;
        if ((container instanceof ActivityRecord) && container != null && service != null) {
            int rootTaskId = ((ActivityRecord) container).getRootTaskId();
            MiuiFreeFormActivityStackStub mffasStub = service.mAtmService.mMiuiFreeFormManagerService.getMiuiFreeFormActivityStack(rootTaskId);
            if (mffasStub != null && (mffasStub instanceof MiuiFreeFormActivityStack)) {
                freemAs = (MiuiFreeFormActivityStack) mffasStub;
                freeformScale = freemAs.mFreeFormScale;
            }
        }
        boolean z = false;
        if (container == null) {
            isSwitchAnimationType = false;
        } else {
            isSwitchAnimationType = container.mNeedReplaceTaskAnimation;
        }
        if (isSwitchAnimationType) {
            if (transit == 8 || transit == 10) {
                transit = 6;
            } else if (transit == 9 || transit == 11) {
                transit = 7;
            }
        }
        switch (transit) {
            case 6:
                Animation defaultAnimation = createActivityOpenCloseTransition(enter, frame, true, freeformScale, container);
                return defaultAnimation;
            case 7:
                Animation defaultAnimation2 = createActivityOpenCloseTransition(enter, frame, false, freeformScale, container);
                return defaultAnimation2;
            case 8:
            case 10:
                Animation defaultAnimation3 = createTaskOpenCloseTransition(enter, frame, true);
                return defaultAnimation3;
            case 9:
            case 11:
                Animation defaultAnimation4 = createTaskOpenCloseTransition(enter, frame, false);
                return defaultAnimation4;
            case 12:
                if (!enter) {
                    return null;
                }
                Animation defaultAnimation5 = createWallerOpenCloseTransitionAnimation(enter, frame, false);
                return defaultAnimation5;
            case 24:
                if (freemAs == null || service == null) {
                    return null;
                }
                Animation defaultAnimation6 = loadAnimationSafely(service.mContext, updateToTranslucentAnimIfNeeded(transit));
                return defaultAnimation6;
            case 25:
                if (freemAs == null || service == null) {
                    return null;
                }
                Animation defaultAnimation7 = loadAnimationSafely(service.mContext, updateToTranslucentAnimIfNeeded(transit));
                return defaultAnimation7;
            case 50:
            case 51:
                Slog.e(TAG, "DEBUG_ROTAloadAnimationccc:  frame = " + frame + "   transit = " + transit + " container " + container);
                if (transit == 51) {
                    z = true;
                }
                Animation defaultAnimation8 = createFloatWindowShowHideAnimation(z, frame, null);
                return defaultAnimation8;
            default:
                return null;
        }
    }

    static boolean useDefaultAnimationAttr(WindowManager.LayoutParams lp) {
        if (lp == null) {
            return false;
        }
        if (lp.packageName == null || (lp.windowAnimations & (-16777216)) == 16777216) {
            return true;
        }
        return "android".equals(lp.packageName);
    }

    static boolean useDefaultAnimationAttr(WindowManager.LayoutParams lp, int resId) {
        if (lp == null) {
            return false;
        }
        if (lp.packageName == null || ((-16777216) & resId) == 16777216) {
            return true;
        }
        return "android".equals(lp.packageName);
    }

    public static boolean useDefaultAnimationAttr(WindowManager.LayoutParams lp, int resId, int transit, boolean enter, boolean isFreeForm) {
        if (isFreeForm && (transit == 6 || transit == 24 || transit == 7 || transit == 25 || transit == 50 || transit == 51)) {
            return true;
        }
        if (lp == null) {
            return false;
        }
        if (lp.packageName == null || ((-16777216) & resId) == 16777216 || (enter && transit == 12 && resId == 0)) {
            return true;
        }
        return "android".equals(lp.packageName);
    }

    static long recalculateClipRevealTranslateYDuration(long duration) {
        return duration - 50;
    }

    /* loaded from: classes.dex */
    private static class CubicEaseOutInterpolator implements Interpolator {
        private CubicEaseOutInterpolator() {
        }

        @Override // android.animation.TimeInterpolator
        public float getInterpolation(float t) {
            float t2 = t - 1.0f;
            return (t2 * t2 * t2) + 1.0f;
        }
    }

    /* loaded from: classes.dex */
    private static class QuartEaseOutInterpolator implements Interpolator {
        private QuartEaseOutInterpolator() {
        }

        @Override // android.animation.TimeInterpolator
        public float getInterpolation(float t) {
            float t2 = t - 1.0f;
            return -((((t2 * t2) * t2) * t2) - 1.0f);
        }
    }

    /* loaded from: classes.dex */
    private static class QuintEaseOutInterpolator implements Interpolator {
        private QuintEaseOutInterpolator() {
        }

        @Override // android.animation.TimeInterpolator
        public float getInterpolation(float t) {
            float t2 = t - 1.0f;
            return (t2 * t2 * t2 * t2 * t2) + 1.0f;
        }
    }

    /* loaded from: classes.dex */
    public static class ActivityTranstionInterpolator implements Interpolator {
        private static float mDamping = 1.0f;
        private static float mResponse = 1.0f;
        private static float initial = 1.0f;
        private static float m = 1.0f;
        private static float k = 1.0f;
        private static float c = 1.0f;
        private static float w = 1.0f;
        private static float r = 1.0f;
        private static float c1 = 1.0f;
        private static float c2 = 1.0f;

        public ActivityTranstionInterpolator(float response, float damping) {
            mDamping = damping;
            mResponse = response;
            initial = -1.0f;
            double pow = Math.pow(6.283185307179586d / response, 2.0d);
            float f = m;
            float f2 = (float) (pow * f);
            k = f2;
            float f3 = (float) (((mDamping * 12.566370614359172d) * f) / mResponse);
            c = f3;
            float f4 = m;
            float sqrt = ((float) Math.sqrt(((f * 4.0f) * f2) - (f3 * f3))) / (f4 * 2.0f);
            w = sqrt;
            float f5 = -((c / 2.0f) * f4);
            r = f5;
            float f6 = initial;
            c1 = f6;
            c2 = (MiuiFreeformPinManagerService.EDGE_AREA - (f5 * f6)) / sqrt;
        }

        @Override // android.animation.TimeInterpolator
        public float getInterpolation(float input) {
            return (float) ((Math.pow(2.718281828459045d, r * input) * ((c1 * Math.cos(w * input)) + (c2 * Math.sin(w * input)))) + 1.0d);
        }
    }

    /* loaded from: classes.dex */
    private static class ScaleTaskAnimation extends Animation {
        private boolean mEnter;
        private float mFromX;
        private float mFromY;
        private float mPivotX;
        private float mPivotY;
        private float mToX;
        private float mToY;

        public ScaleTaskAnimation(float fromX, float toX, float fromY, float toY, float pivotX, float pivotY, boolean enter) {
            this.mFromX = fromX;
            this.mToX = toX;
            this.mFromY = fromY;
            this.mToY = toY;
            this.mPivotX = pivotX;
            this.mPivotY = pivotY;
            this.mEnter = enter;
        }

        @Override // android.view.animation.Animation
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            float scale;
            if (this.mEnter) {
                scale = (float) Math.max(1.0d - (((1.0f - interpolatedTime) * 0.08d) * 3.0d), 0.92d);
            } else {
                scale = (float) Math.max(1.0d - ((interpolatedTime * 0.08d) * 3.0d), 0.92d);
            }
            if (scale > 1.0d) {
                scale = 1.0f;
            }
            t.getMatrix().setScale(scale, scale, this.mPivotX, this.mPivotY);
        }
    }

    /* loaded from: classes.dex */
    private static class ScaleWallPaperAnimation extends Animation {
        private boolean mEnter;
        private float mPivotX;
        private float mPivotY;

        public ScaleWallPaperAnimation(float pivotX, float pivotY, boolean enter) {
            this.mPivotX = pivotX;
            this.mPivotY = pivotY;
            this.mEnter = enter;
        }

        @Override // android.view.animation.Animation
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            float scale = 1.0f;
            if (this.mEnter) {
                scale = (float) (1.0d - (Math.pow(Math.max(0.0d, 1.0d - (Math.abs(interpolatedTime - 0.5d) / 0.5d)), 0.35d) * 0.1d));
            }
            if (scale > 1.0d) {
                scale = 1.0f;
            }
            t.getMatrix().setScale(scale, scale, this.mPivotX, this.mPivotY);
        }
    }

    /* loaded from: classes.dex */
    public static class ScaleXAnimation extends Animation {
        private float mFromX;
        private float mPivotX;
        private float mToX;

        public ScaleXAnimation(float fromX, float toX) {
            this.mFromX = fromX;
            this.mToX = toX;
            this.mPivotX = MiuiFreeformPinManagerService.EDGE_AREA;
        }

        public ScaleXAnimation(float fromX, float toX, float pivotX) {
            this.mFromX = fromX;
            this.mToX = toX;
            this.mPivotX = pivotX;
        }

        @Override // android.view.animation.Animation
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            float sx = 1.0f;
            float scale = getScaleFactor();
            float f = this.mFromX;
            if (f != 1.0f || this.mToX != 1.0f) {
                sx = f + ((this.mToX - f) * interpolatedTime);
            }
            if (this.mPivotX == MiuiFreeformPinManagerService.EDGE_AREA) {
                t.getMatrix().setScale(sx, 1.0f);
            } else {
                t.getMatrix().setScale(sx, 1.0f, this.mPivotX * scale, MiuiFreeformPinManagerService.EDGE_AREA);
            }
        }
    }

    /* loaded from: classes.dex */
    public static class ScaleYAnimation extends Animation {
        private float mFromY;
        private float mPivotY;
        private float mToY;

        public ScaleYAnimation(float fromY, float toY) {
            this.mFromY = fromY;
            this.mToY = toY;
            this.mPivotY = MiuiFreeformPinManagerService.EDGE_AREA;
        }

        public ScaleYAnimation(float fromY, float toY, float pivotY) {
            this.mFromY = fromY;
            this.mToY = toY;
            this.mPivotY = pivotY;
        }

        @Override // android.view.animation.Animation
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            float sy = 1.0f;
            float scale = getScaleFactor();
            float f = this.mFromY;
            if (f != 1.0f || this.mToY != 1.0f) {
                sy = f + ((this.mToY - f) * interpolatedTime);
            }
            if (this.mPivotY == MiuiFreeformPinManagerService.EDGE_AREA) {
                t.getMatrix().setScale(1.0f, sy);
            } else {
                t.getMatrix().setScale(1.0f, sy, MiuiFreeformPinManagerService.EDGE_AREA, this.mPivotY * scale);
            }
        }
    }

    /* loaded from: classes.dex */
    public static class BezierXAnimation extends Animation {
        private float mEndX;
        private float mFromX;
        private float mInertiaX;
        private float mPivotX;
        private float mStartX;
        private float mToX;
        private int mWidth;

        public BezierXAnimation(float fromX, float toX, float startX, float endX, Point inertia, int width) {
            this(fromX, toX, MiuiFreeformPinManagerService.EDGE_AREA, startX, endX, inertia, width);
        }

        public BezierXAnimation(float fromX, float toX, float pivotX, float startX, float endX, Point inertia, int width) {
            this.mStartX = startX;
            this.mEndX = endX;
            this.mInertiaX = (float) (startX + (inertia.x * 1.2d));
            this.mFromX = fromX;
            this.mToX = toX;
            this.mPivotX = pivotX;
            this.mWidth = width;
        }

        @Override // android.view.animation.Animation
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            float sx = 1.0f;
            float scale = getScaleFactor();
            float f = this.mFromX;
            if (f != 1.0f || this.mToX != 1.0f) {
                sx = f + ((this.mToX - f) * interpolatedTime);
            }
            if (this.mPivotX == MiuiFreeformPinManagerService.EDGE_AREA) {
                t.getMatrix().setScale(sx, 1.0f);
            } else {
                t.getMatrix().setScale(sx, 1.0f, this.mPivotX * scale, MiuiFreeformPinManagerService.EDGE_AREA);
            }
            double targetX = (Math.pow(1.0f - interpolatedTime, 2.0d) * this.mStartX) + (Math.pow(interpolatedTime, 2.0d) * this.mEndX) + (interpolatedTime * 2.0f * (1.0f - interpolatedTime) * this.mInertiaX);
            t.getMatrix().postTranslate(((float) targetX) - ((this.mWidth * sx) / 2.0f), MiuiFreeformPinManagerService.EDGE_AREA);
        }
    }

    /* loaded from: classes.dex */
    public static class BezierYAnimation extends Animation {
        private float mEndY;
        private float mFromY;
        private int mHeight;
        private float mInertiaY;
        private float mPivotY;
        private float mStartY;
        private float mToY;

        public BezierYAnimation(float fromY, float toY, float startY, float endY, Point inertia, int height) {
            this(fromY, toY, MiuiFreeformPinManagerService.EDGE_AREA, startY, endY, inertia, height);
        }

        public BezierYAnimation(float fromY, float toY, float pivotY, float startY, float endY, Point inertia, int height) {
            this.mFromY = fromY;
            this.mToY = toY;
            this.mPivotY = pivotY;
            this.mStartY = startY;
            this.mEndY = endY;
            this.mInertiaY = (float) (startY + (inertia.y * 1.5d));
            this.mHeight = height;
        }

        @Override // android.view.animation.Animation
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            float sy = 1.0f;
            float scale = getScaleFactor();
            float f = this.mFromY;
            if (f != 1.0f || this.mToY != 1.0f) {
                sy = f + ((this.mToY - f) * interpolatedTime);
            }
            if (this.mPivotY == MiuiFreeformPinManagerService.EDGE_AREA) {
                t.getMatrix().setScale(1.0f, sy);
            } else {
                t.getMatrix().setScale(1.0f, sy, MiuiFreeformPinManagerService.EDGE_AREA, this.mPivotY * scale);
            }
            double targetY = (Math.pow(1.0f - interpolatedTime, 2.0d) * this.mStartY) + (Math.pow(interpolatedTime, 2.0d) * this.mEndY) + ((1.0f - interpolatedTime) * 2.0f * interpolatedTime * this.mInertiaY);
            t.getMatrix().postTranslate(MiuiFreeformPinManagerService.EDGE_AREA, ((float) targetY) - ((this.mHeight * sy) / 2.0f));
        }
    }

    public static Rect getMiuiAnimSupportInset() {
        return sMiuiAnimSupportInset;
    }

    public static boolean disableSnapshot(String pkg) {
        if (BLACK_LIST_NOT_ALLOWED_SNAPSHOT.contains(pkg)) {
            return true;
        }
        return false;
    }

    public static boolean disableSnapshotByComponent(ActivityRecord mActivity) {
        if (BLACK_LIST_NOT_ALLOWED_SNAPSHOT_COMPONENT.contains(mActivity.shortComponentName)) {
            return true;
        }
        return false;
    }

    public static boolean ignoreLaunchedFromSystemSurface(String pkg) {
        if (IGNORE_LAUNCHED_FROM_SYSTEM_SURFACE.contains(pkg)) {
            return true;
        }
        return false;
    }

    public static boolean disableSnapshotForApplock(String pkg, int uid) {
        boolean z;
        IBinder b = ServiceManager.getService("security");
        if (b == null) {
            return false;
        }
        try {
            int userId = UserHandle.getUserId(uid);
            ISecurityManager service = ISecurityManager.Stub.asInterface(b);
            if (service.haveAccessControlPassword(userId) && service.getApplicationAccessControlEnabledAsUser(pkg, userId)) {
                if (!service.checkAccessControlPassAsUser(pkg, (Intent) null, userId)) {
                    z = true;
                    boolean result = z;
                    return result;
                }
            }
            z = false;
            boolean result2 = z;
            return result2;
        } catch (Exception e) {
            Slog.e(TAG, "start window checkAccessControlPass error: ", e);
            return false;
        }
    }

    public static void initDisplayRoundCorner(Context context) {
        Display display;
        int result = 60;
        if (context != null && (display = context.getDisplayNoVerify()) != null && display.getRoundedCorner(0) != null) {
            float topLeftRadius = getRoundedCornerRadius(display.getRoundedCorner(0));
            float topRightRadius = getRoundedCornerRadius(display.getRoundedCorner(1));
            float bottomRightRadius = getRoundedCornerRadius(display.getRoundedCorner(2));
            float bottomLeftRadius = getRoundedCornerRadius(display.getRoundedCorner(3));
            result = (int) minRadius(minRadius(topLeftRadius, topRightRadius), minRadius(bottomLeftRadius, bottomRightRadius));
            if (result <= MiuiFreeformPinManagerService.EDGE_AREA) {
                result = 60;
            }
        }
        DISPLAY_ROUND_CORNER_RADIUS = result;
    }

    private static float minRadius(float aRadius, float bRadius) {
        return aRadius == MiuiFreeformPinManagerService.EDGE_AREA ? bRadius : bRadius == MiuiFreeformPinManagerService.EDGE_AREA ? aRadius : Math.min(aRadius, bRadius);
    }

    private static float getRoundedCornerRadius(RoundedCorner roundedCorner) {
        return roundedCorner == null ? MiuiFreeformPinManagerService.EDGE_AREA : roundedCorner.getRadius();
    }

    public static boolean isCTS() {
        return !SystemProperties.getBoolean("persist.sys.miui_optimization", !SplitScreenReporter.ACTION_ENTER_SPLIT.equals(SystemProperties.get("ro.miui.cts")));
    }

    public static ArraySet<WindowContainer> getUnHierarchicalAnimationTargets(ArraySet<ActivityRecord> openingApps, ArraySet<ActivityRecord> closingApps, boolean visible) {
        LinkedList<WindowContainer> candidates = new LinkedList<>();
        ArraySet<ActivityRecord> apps = visible ? openingApps : closingApps;
        for (int i = 0; i < apps.size(); i++) {
            ActivityRecord app = apps.valueAt(i);
            if (app.shouldApplyAnimation(visible)) {
                candidates.add(app);
            }
        }
        return new ArraySet<>(candidates);
    }

    public static Animation loadSplitFullAnimation(int transit, boolean enter, Rect frame, int orientation, ActivityRecord container, boolean isSplitMode) {
        boolean isActOpen = MiuiSplitUtils.isActOpen(transit);
        AnimationSet set = new AnimationSet(true);
        Rect rect = null;
        if (isActOpen) {
            if (enter) {
                set.addAnimation(new TranslateXAnimation(frame.right, (float) MiuiFreeformPinManagerService.EDGE_AREA));
            } else {
                if (isSplitMode) {
                    rect = getDimmerRect(container, orientation, frame);
                }
                set.addAnimation(new SplitDimmer(MiuiFreeformPinManagerService.EDGE_AREA, 0.15f, rect, container));
            }
        } else if (enter) {
            if (isSplitMode) {
                rect = getDimmerRect(container, orientation, frame);
            }
            set.addAnimation(new SplitDimmer(0.15f, MiuiFreeformPinManagerService.EDGE_AREA, rect, container));
        } else {
            set.addAnimation(new TranslateXAnimation((float) MiuiFreeformPinManagerService.EDGE_AREA, frame.right));
        }
        set.setInterpolator(new DecelerateInterpolator(1.5f));
        set.setDuration(400L);
        return set;
    }

    public static Animation loadFullSplitAnimation(int transit, boolean enter, Rect frame, int orientation, ActivityRecord container, boolean isSplitMode) {
        if (!MiuiSplitUtils.isActOpen(transit)) {
            return null;
        }
        Animation a = null;
        if (!enter || !isSplitMode) {
            if (!enter && !isSplitMode) {
                a = new TranslateXAnimation((float) MiuiFreeformPinManagerService.EDGE_AREA, frame.right);
            } else if (enter && !isSplitMode) {
                a = new SplitDimmer(0.15f, MiuiFreeformPinManagerService.EDGE_AREA, null, container);
            }
        } else if (container != null && container.isTopRunningActivity()) {
            int rightWeight = MiuiSplitUtils.getSplitRightWeight(orientation);
            if (MiuiSplitUtils.isRtlLocale() || isRtlLayout(container)) {
                Rect rtlRect = new Rect(0, 0, rightWeight + 1, frame.bottom);
                a = new TranslateXWithClipAnimation(rightWeight, (float) MiuiFreeformPinManagerService.EDGE_AREA, rtlRect);
            } else {
                a = new TranslateXAnimation(rightWeight, (float) MiuiFreeformPinManagerService.EDGE_AREA);
            }
            a.setStartOffset(450L);
        } else {
            AnimationSet set = new AnimationSet(true);
            Rect rect = getDimmerRect(container, orientation, frame);
            SplitDimmer delayDimmer = new SplitDimmer(MiuiFreeformPinManagerService.EDGE_AREA, 0.15f, rect, container);
            delayDimmer.setStartOffset(450L);
            delayDimmer.setDuration(400L);
            SplitDimmer dimmer = new SplitDimmer(0.15f, MiuiFreeformPinManagerService.EDGE_AREA, rect, container);
            dimmer.setDuration(400L);
            set.addAnimation(dimmer);
            set.addAnimation(delayDimmer);
            set.setInterpolator(new DecelerateInterpolator(1.5f));
            return set;
        }
        if (a != null) {
            a.setInterpolator(new DecelerateInterpolator(1.5f));
            a.setDuration(400L);
        }
        return a;
    }

    static Rect getDimmerRect(ActivityRecord container, int orientation, Rect frame) {
        boolean isRtl = MiuiSplitUtils.isRtlLocale() || isRtlLayout(container);
        int rightWeight = MiuiSplitUtils.getSplitRightWeight(orientation);
        Rect rtlRect = new Rect(0, 0, rightWeight, frame.bottom);
        Rect rect = new Rect(MiuiSplitUtils.getSplitLeftWeight(orientation), 0, frame.right, frame.bottom);
        return isRtl ? rtlRect : rect;
    }

    public static Animation loadSplitAnimation(int transit, boolean enter, Rect frame, int orientation, ActivityRecord container) {
        Animation a = null;
        boolean isActOpen = MiuiSplitUtils.isActOpen(transit);
        if (MiuiSplitUtils.isActClose(transit) || isActOpen) {
            if (MiuiSplitUtils.isRtlLocale() || isRtlLayout(container)) {
                a = createSplitActivityOpenAndExitAnimationForRtl(enter, isActOpen, frame, orientation, container);
            } else {
                a = createSplitActivityOpenAndExitAnimation(enter, isActOpen, frame, orientation, container);
            }
            Slog.d(TAG, "createSplitActivityOpenAndExitAnimation: transit: " + transit);
        }
        return a;
    }

    public static boolean isRtlLayout(ActivityRecord r) {
        return (r == null || r.getConfiguration() == null || r.getConfiguration().getLayoutDirection() != 1) ? false : true;
    }

    static Animation createSplitActivityOpenAndExitAnimationForRtl(boolean enter, boolean isOpen, Rect frame, int orientation, ActivityRecord container) {
        TranslateXWithClipAnimation translateXWithClipAnimation;
        AnimationSet set = new AnimationSet(true);
        int rightWeight = MiuiSplitUtils.getSplitRightWeight(orientation);
        Rect rtlRect = new Rect(0, 0, rightWeight + 1, frame.bottom);
        if (isOpen) {
            if (enter) {
                translateXWithClipAnimation = new TranslateXWithClipAnimation(rightWeight, (float) MiuiFreeformPinManagerService.EDGE_AREA, rtlRect);
            } else {
                TranslateXWithClipAnimation translateXAnimation = new TranslateXAnimation((float) MiuiFreeformPinManagerService.EDGE_AREA, -rightWeight);
                set.addAnimation(new SplitDimmer(MiuiFreeformPinManagerService.EDGE_AREA, 0.15f, rtlRect, container));
                translateXWithClipAnimation = translateXAnimation;
            }
        } else if (enter) {
            TranslateXWithClipAnimation translateXAnimation2 = new TranslateXAnimation(-rightWeight, (float) MiuiFreeformPinManagerService.EDGE_AREA);
            set.addAnimation(new SplitDimmer(0.15f, MiuiFreeformPinManagerService.EDGE_AREA, rtlRect, container));
            translateXWithClipAnimation = translateXAnimation2;
        } else {
            translateXWithClipAnimation = new TranslateXWithClipAnimation((float) MiuiFreeformPinManagerService.EDGE_AREA, rightWeight, rtlRect);
        }
        set.addAnimation(translateXWithClipAnimation);
        set.setInterpolator(new DecelerateInterpolator(1.5f));
        set.setDuration(400L);
        return set;
    }

    static Animation createSplitActivityOpenAndExitAnimation(boolean enter, boolean isOpen, Rect frame, int orientation, ActivityRecord container) {
        TranslateXAnimation translateXAnimation;
        AnimationSet set = new AnimationSet(true);
        int rightWeight = MiuiSplitUtils.getSplitRightWeight(orientation);
        if (isOpen) {
            if (enter) {
                translateXAnimation = new TranslateXAnimation(rightWeight, (float) MiuiFreeformPinManagerService.EDGE_AREA);
            } else {
                Rect rect = new Rect(MiuiSplitUtils.getSplitLeftWeight(orientation), 0, frame.right, frame.bottom);
                TranslateXAnimation translateXWithClipAnimation = new TranslateXWithClipAnimation((float) MiuiFreeformPinManagerService.EDGE_AREA, -300.0f, rect);
                set.addAnimation(new SplitDimmer(MiuiFreeformPinManagerService.EDGE_AREA, 0.15f, rect, container));
                translateXAnimation = translateXWithClipAnimation;
            }
        } else if (enter) {
            Rect rect2 = new Rect(MiuiSplitUtils.getSplitLeftWeight(orientation), 0, frame.right, frame.bottom);
            TranslateXAnimation translateXWithClipAnimation2 = new TranslateXWithClipAnimation(-300.0f, (float) MiuiFreeformPinManagerService.EDGE_AREA, rect2);
            set.addAnimation(new SplitDimmer(0.15f, MiuiFreeformPinManagerService.EDGE_AREA, rect2, container));
            translateXAnimation = translateXWithClipAnimation2;
        } else {
            translateXAnimation = new TranslateXAnimation((float) MiuiFreeformPinManagerService.EDGE_AREA, rightWeight);
        }
        set.addAnimation(translateXAnimation);
        set.setInterpolator(new DecelerateInterpolator(1.5f));
        set.setDuration(400L);
        return set;
    }
}
