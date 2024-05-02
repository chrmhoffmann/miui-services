package com.android.server.display;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Animatable2;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.MathUtils;
import android.util.Slog;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.MotionEvent;
import android.view.SurfaceControl;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewRootImpl;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.server.display.animation.DynamicAnimation;
import com.android.server.display.animation.FloatPropertyCompat;
import com.android.server.display.animation.SpringAnimation;
import com.android.server.display.animation.SpringForce;
import com.android.server.wm.MiuiFreeformPinManagerService;
/* loaded from: classes.dex */
public class SwipeUpWindow {
    private static final float BG_INIT_ALPHA = 0.6f;
    private static final boolean DEBUG = false;
    private static final float DEFAULT_DAMPING = 0.95f;
    private static final float DEFAULT_STIFFNESS = 322.27f;
    private static final float DISTANCE_DEBOUNCE = 200.0f;
    private static final int ICON_HEIGHT = 28;
    private static final float ICON_OFFSET = 30.0f;
    private static final float ICON_TIP_SHOW_DAMPING = 1.0f;
    private static final float ICON_TIP_SHOW_STIFFNESS = 39.478416f;
    private static final float ICON_VIEW_BOTTOM = 117.0f;
    private static final int ICON_WIDTH = 28;
    private static final int LOCK_SATE_START_DELAY = 1000;
    private static final float LOCK_STATE_LONG_DAMPING = 1.0f;
    private static final float LOCK_STATE_LONG_STIFFNESS = 6.86f;
    private static final int MSG_ICON_LOCK_ANIMATION = 102;
    private static final int MSG_ICON_TIP_HIDE_ANIMATION = 103;
    private static final int MSG_LOCK_STATE_WITH_LONG_ANIMATION = 101;
    private static final int MSG_RELEASE_WINDOW = 105;
    private static final int MSG_SCREEN_OFF = 104;
    private static final int SCREEN_HEIGHT = 2520;
    private static final int SCREEN_OFF_DELAY = 6000;
    private static final int SCREEN_WIDTH = 1080;
    private static final float SCROLL_DAMPING = 0.9f;
    private static final float SCROLL_STIFFNESS = 986.96045f;
    private static final float SHOW_STATE_DAMPING = 1.0f;
    private static final float SHOW_STATE_STIFFNESS = 157.91367f;
    public static final String TAG = "SwipeUpWindow";
    private static final int TIP_HEIGHT = 25;
    private static final float TIP_INIT_ALPHA = 0.4f;
    private static final float TIP_OFFSET = 50.0f;
    private static final int TIP_TEXT_SIZE = 19;
    private static final float TIP_VIEW_BOTTOM = 63.0f;
    private static final float UNLOCK_DISTANCE = 504.0f;
    private BlackLinearGradientView mBlackLinearGradientView;
    private Context mContext;
    private SpringAnimation mGradientShadowSpringAnimation;
    private Handler mHandler;
    private boolean mIconAndTipisShowing;
    private AnimatedVectorDrawable mIconDrawable;
    private DualSpringAnimation mIconSpringAnimation;
    private ImageView mIconView;
    private PowerManager mPowerManager;
    private DynamicAnimation.OnAnimationEndListener mPreOnAnimationEndListener;
    private boolean mScrollAnimationNeedInit;
    private FrameLayout mSwipeUpFrameLayout;
    private WindowManager.LayoutParams mSwipeUpLayoutParams;
    private boolean mSwipeUpWindowShowing;
    private DualSpringAnimation mTipSpringAnimation;
    private TextView mTipView;
    private VelocityTracker mVelocityTracker;
    private WindowManager mWindowManager;
    private int mPreTopColor = -1;
    private int mPreBottomColor = -1;
    private int mPreBlurLevel = -1;
    private float mStartPer = 1.0f;
    private DynamicAnimation.OnAnimationEndListener mWakeStateAnimationEndListener = new DynamicAnimation.OnAnimationEndListener() { // from class: com.android.server.display.SwipeUpWindow.3
        @Override // com.android.server.display.animation.DynamicAnimation.OnAnimationEndListener
        public void onAnimationEnd(DynamicAnimation animation, boolean canceled, float value, float velocity) {
            if (canceled) {
                return;
            }
            Message msg = SwipeUpWindow.this.mHandler.obtainMessage(101);
            SwipeUpWindow.this.mHandler.sendMessageDelayed(msg, 1000L);
        }
    };
    private DynamicAnimation.OnAnimationEndListener mLockStateLongAnimationEndListener = new DynamicAnimation.OnAnimationEndListener() { // from class: com.android.server.display.SwipeUpWindow.4
        @Override // com.android.server.display.animation.DynamicAnimation.OnAnimationEndListener
        public void onAnimationEnd(DynamicAnimation animation, boolean canceled, float value, float velocity) {
            if (canceled) {
                return;
            }
            SwipeUpWindow.this.mHandler.sendEmptyMessage(102);
        }
    };
    private DynamicAnimation.OnAnimationEndListener mLockStateShortAnimationEndListener = new DynamicAnimation.OnAnimationEndListener() { // from class: com.android.server.display.SwipeUpWindow.5
        @Override // com.android.server.display.animation.DynamicAnimation.OnAnimationEndListener
        public void onAnimationEnd(DynamicAnimation animation, boolean canceled, float value, float velocity) {
            if (canceled) {
                return;
            }
            SwipeUpWindow.this.mHandler.sendEmptyMessage(104);
        }
    };
    private DynamicAnimation.OnAnimationEndListener mUnlockStateAnimationEndListener = new DynamicAnimation.OnAnimationEndListener() { // from class: com.android.server.display.SwipeUpWindow.6
        @Override // com.android.server.display.animation.DynamicAnimation.OnAnimationEndListener
        public void onAnimationEnd(DynamicAnimation animation, boolean canceled, float value, float velocity) {
            if (canceled) {
                return;
            }
            SwipeUpWindow.this.mHandler.sendEmptyMessage(105);
        }
    };
    private View.OnTouchListener mOnTouchListener = new View.OnTouchListener() { // from class: com.android.server.display.SwipeUpWindow.7
        private float startTouchY;

        @Override // android.view.View.OnTouchListener
        public boolean onTouch(View v, MotionEvent event) {
            SwipeUpWindow.this.initVelocityTrackerIfNotExists();
            switch (event.getAction()) {
                case 0:
                    if (SwipeUpWindow.this.mVelocityTracker == null) {
                        SwipeUpWindow.this.mVelocityTracker = VelocityTracker.obtain();
                    } else {
                        SwipeUpWindow.this.mVelocityTracker.clear();
                    }
                    SwipeUpWindow.this.mVelocityTracker.addMovement(event);
                    SwipeUpWindow swipeUpWindow = SwipeUpWindow.this;
                    swipeUpWindow.mStartPer = swipeUpWindow.mAnimationState.getCurrentState();
                    this.startTouchY = event.getRawY();
                    SwipeUpWindow.this.resetIconAnimation();
                    SwipeUpWindow.this.mScrollAnimationNeedInit = true;
                    return true;
                case 1:
                case 3:
                    VelocityTracker velocityTracker = SwipeUpWindow.this.mVelocityTracker;
                    velocityTracker.computeCurrentVelocity(1000);
                    float curVelocitY = velocityTracker.getYVelocity();
                    SwipeUpWindow.this.recycleVelocityTracker();
                    if (SwipeUpWindow.this.mStartPer != SwipeUpWindow.this.mAnimationState.getPerState()) {
                        float distance = event.getRawY() - this.startTouchY;
                        if (curVelocitY < -1000.0f && (-distance) >= 200.0f) {
                            SwipeUpWindow.this.setUnlockState();
                            return true;
                        } else if (curVelocitY > 1000.0f && distance >= 200.0f) {
                            SwipeUpWindow.this.setLockStateWithShortAnimation();
                            return true;
                        } else if ((-distance) > SwipeUpWindow.UNLOCK_DISTANCE) {
                            SwipeUpWindow.this.setUnlockState();
                            return true;
                        } else if (distance > SwipeUpWindow.UNLOCK_DISTANCE) {
                            SwipeUpWindow.this.setLockStateWithShortAnimation();
                            return true;
                        } else {
                            SwipeUpWindow.this.setWakeState();
                            return true;
                        }
                    }
                    return true;
                case 2:
                    SwipeUpWindow.this.mHandler.removeMessages(101);
                    SwipeUpWindow.this.mVelocityTracker.addMovement(event);
                    float offsetY = event.getRawY() - this.startTouchY;
                    float per = SwipeUpWindow.this.mStartPer + (offsetY / (SwipeUpWindow.this.mScreenHeight / 3));
                    SwipeUpWindow swipeUpWindow2 = SwipeUpWindow.this;
                    float frictionY = MathUtils.min((float) MiuiFreeformPinManagerService.EDGE_AREA, swipeUpWindow2.afterFrictionValue(offsetY / swipeUpWindow2.mScreenHeight, 1.0f));
                    float alpha = MathUtils.min(1.0f, (3.0f * per) + 1.0f);
                    float tipY = SwipeUpWindow.this.mTipView.getTop() + (150.0f * frictionY);
                    float tipAlpha = SwipeUpWindow.TIP_INIT_ALPHA * alpha;
                    float iconY = (50.0f * frictionY) + SwipeUpWindow.this.mIconView.getTop();
                    if (!SwipeUpWindow.this.mScrollAnimationNeedInit) {
                        SwipeUpWindow.this.mGradientShadowSpringAnimation.animateToFinalPosition(per);
                        SwipeUpWindow.this.mIconSpringAnimation.animateToFinalPosition(iconY, alpha);
                        SwipeUpWindow.this.mTipSpringAnimation.animateToFinalPosition(tipY, tipAlpha);
                        return true;
                    }
                    SwipeUpWindow.this.mScrollAnimationNeedInit = false;
                    SwipeUpWindow.this.startGradientShadowAnimation(SwipeUpWindow.SCROLL_STIFFNESS, 0.9f, per);
                    SwipeUpWindow.this.mIconSpringAnimation.cancel();
                    SwipeUpWindow.this.mTipSpringAnimation.cancel();
                    SwipeUpWindow swipeUpWindow3 = SwipeUpWindow.this;
                    SpringAnimation creatSpringAnimation = swipeUpWindow3.creatSpringAnimation(swipeUpWindow3.mIconView, SpringAnimation.Y, SwipeUpWindow.SCROLL_STIFFNESS, 0.9f, iconY);
                    SwipeUpWindow swipeUpWindow4 = SwipeUpWindow.this;
                    swipeUpWindow3.mIconSpringAnimation = new DualSpringAnimation(creatSpringAnimation, swipeUpWindow4.creatSpringAnimation(swipeUpWindow4.mIconView, SpringAnimation.ALPHA, SwipeUpWindow.SCROLL_STIFFNESS, 0.9f, alpha));
                    SwipeUpWindow swipeUpWindow5 = SwipeUpWindow.this;
                    SpringAnimation creatSpringAnimation2 = swipeUpWindow5.creatSpringAnimation(swipeUpWindow5.mTipView, SpringAnimation.Y, SwipeUpWindow.SCROLL_STIFFNESS, 0.9f, tipY);
                    SwipeUpWindow swipeUpWindow6 = SwipeUpWindow.this;
                    swipeUpWindow5.mTipSpringAnimation = new DualSpringAnimation(creatSpringAnimation2, swipeUpWindow6.creatSpringAnimation(swipeUpWindow6.mTipView, SpringAnimation.ALPHA, SwipeUpWindow.SCROLL_STIFFNESS, 0.9f, tipAlpha));
                    SwipeUpWindow.this.mIconSpringAnimation.start();
                    SwipeUpWindow.this.mTipSpringAnimation.start();
                    return true;
                default:
                    return true;
            }
        }
    };
    private int mScreenHeight = SCREEN_HEIGHT;
    private int mScreenWidth = SCREEN_WIDTH;
    private AnimationState mAnimationState = new AnimationState();

    public SwipeUpWindow(Context context, Looper looper) {
        this.mContext = context;
        this.mHandler = new SwipeUpWindowHandler(looper);
        this.mWindowManager = (WindowManager) this.mContext.getSystemService("window");
        this.mPowerManager = (PowerManager) this.mContext.getSystemService("power");
        updateSettings(false);
    }

    public void removeSwipeUpWindow() {
    }

    public void releaseSwipeWindow() {
        if (!this.mSwipeUpWindowShowing) {
            return;
        }
        Slog.i(TAG, "release swipe up window");
        this.mSwipeUpWindowShowing = false;
        updateSettings(false);
        this.mHandler.removeCallbacksAndMessages(null);
        SpringAnimation springAnimation = this.mGradientShadowSpringAnimation;
        if (springAnimation != null) {
            springAnimation.cancel();
            this.mGradientShadowSpringAnimation = null;
        }
        FrameLayout frameLayout = this.mSwipeUpFrameLayout;
        if (frameLayout != null) {
            this.mWindowManager.removeViewImmediate(frameLayout);
            this.mSwipeUpFrameLayout = null;
        }
    }

    public void showSwipeUpWindow() {
        if (this.mSwipeUpWindowShowing) {
            return;
        }
        Slog.i(TAG, "show swipe up window");
        updateSettings(true);
        if (this.mSwipeUpFrameLayout == null) {
            initSwipeUpWindow();
        }
        this.mSwipeUpWindowShowing = true;
        this.mWindowManager.addView(this.mSwipeUpFrameLayout, this.mSwipeUpLayoutParams);
    }

    private void initSwipeUpWindow() {
        this.mSwipeUpFrameLayout = new SwipeUpFrameLayout(new ContextThemeWrapper(this.mContext, 16973831));
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(-1, -1, 2026, 25297156, -3);
        this.mSwipeUpLayoutParams = layoutParams;
        layoutParams.inputFeatures |= 2;
        this.mSwipeUpLayoutParams.layoutInDisplayCutoutMode = 3;
        this.mSwipeUpLayoutParams.gravity = 8388659;
        this.mSwipeUpLayoutParams.setTitle(TAG);
        this.mSwipeUpLayoutParams.screenOrientation = 1;
        this.mSwipeUpFrameLayout.setOnTouchListener(this.mOnTouchListener);
        this.mSwipeUpFrameLayout.setLongClickable(true);
        this.mSwipeUpFrameLayout.setFocusable(true);
        initBlackLinearGradientView();
        initIconView();
        initTipView();
        initGradientShadowAnimation();
    }

    private void initBlackLinearGradientView() {
        FrameLayout.LayoutParams backViewParams = new FrameLayout.LayoutParams(-1, -1, 17);
        BlackLinearGradientView blackLinearGradientView = new BlackLinearGradientView(this.mContext);
        this.mBlackLinearGradientView = blackLinearGradientView;
        blackLinearGradientView.setBackgroundResource(0);
        this.mSwipeUpFrameLayout.addView(this.mBlackLinearGradientView, backViewParams);
    }

    private void initIconView() {
        ImageView imageView = new ImageView(this.mContext);
        this.mIconView = imageView;
        imageView.setImageResource(285737079);
        this.mIconView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        this.mIconView.setAlpha(MiuiFreeformPinManagerService.EDGE_AREA);
        this.mIconView.setBackgroundResource(0);
        FrameLayout.LayoutParams iconViewParams = new FrameLayout.LayoutParams(transformDpToPx(28.0f), transformDpToPx(28.0f), 49);
        iconViewParams.setMargins(0, this.mScreenHeight - transformDpToPx(145.0f), 0, 0);
        this.mSwipeUpFrameLayout.addView(this.mIconView, iconViewParams);
        if (this.mIconView.getDrawable() instanceof AnimatedVectorDrawable) {
            this.mIconDrawable = (AnimatedVectorDrawable) this.mIconView.getDrawable();
        } else {
            Slog.i(TAG, "icon drawable get incompatible class");
        }
    }

    private void initTipView() {
        TextView textView = new TextView(this.mContext);
        this.mTipView = textView;
        textView.setGravity(17);
        this.mTipView.setText(286196608);
        this.mTipView.setTextSize(2, 19.0f);
        Typeface typeface = Typeface.create("mipro-medium", 0);
        this.mTipView.setTypeface(typeface);
        this.mTipView.setTextColor(1728053247);
        this.mTipView.setAlpha(MiuiFreeformPinManagerService.EDGE_AREA);
        this.mTipView.setBackgroundResource(0);
        FrameLayout.LayoutParams tipViewParams = new FrameLayout.LayoutParams(-2, -2, 49);
        tipViewParams.setMargins(0, this.mScreenHeight - transformDpToPx(88.0f), 0, 0);
        this.mSwipeUpFrameLayout.addView(this.mTipView, tipViewParams);
    }

    private void initGradientShadowAnimation() {
        this.mGradientShadowSpringAnimation = new SpringAnimation(this.mAnimationState, new FloatPropertyCompat<AnimationState>("perState") { // from class: com.android.server.display.SwipeUpWindow.1
            public float getValue(AnimationState object) {
                return object.getPerState();
            }

            public void setValue(AnimationState object, float value) {
                object.setPerState(value);
            }
        });
        SpringForce springForce = new SpringForce();
        springForce.setStiffness(322.27f);
        springForce.setDampingRatio(0.95f);
        this.mGradientShadowSpringAnimation.setSpring(springForce);
        this.mGradientShadowSpringAnimation.setMinimumVisibleChange(0.00390625f);
        this.mGradientShadowSpringAnimation.addUpdateListener(new DynamicAnimation.OnAnimationUpdateListener() { // from class: com.android.server.display.SwipeUpWindow.2
            @Override // com.android.server.display.animation.DynamicAnimation.OnAnimationUpdateListener
            public void onAnimationUpdate(DynamicAnimation animation, float value, float velocity) {
                SwipeUpWindow.this.updateBlackView();
            }
        });
    }

    public void startSwipeUpAnimation() {
        if (!this.mSwipeUpWindowShowing) {
            return;
        }
        this.mPreTopColor = -16777216;
        this.mPreBottomColor = -16777216;
        this.mPreBlurLevel = -1;
        this.mAnimationState.setPerState(1.0f);
        updateBlur(1.0f);
        setWakeState();
        this.mHandler.removeMessages(104);
        Message msg = this.mHandler.obtainMessage(104);
        this.mHandler.sendMessageDelayed(msg, 6000L);
    }

    public void setWakeState() {
        startGradientShadowAnimation(SHOW_STATE_STIFFNESS, 1.0f, MiuiFreeformPinManagerService.EDGE_AREA, this.mWakeStateAnimationEndListener, 0.00390625f);
        resetIconAnimation();
        if (this.mIconView.getAlpha() <= MiuiFreeformPinManagerService.EDGE_AREA) {
            prepareIconAndTipAnimation();
        }
        playIconAndTipShowAnimation();
    }

    public void setLockStateWithLongAnimation() {
        startGradientShadowAnimation(LOCK_STATE_LONG_STIFFNESS, 1.0f, 1.0f, this.mLockStateLongAnimationEndListener, 0.00390625f);
    }

    public void setLockStateWithShortAnimation() {
        startGradientShadowAnimation(322.27f, 0.95f, 1.0f, this.mLockStateShortAnimationEndListener, 0.00390625f);
        playIconAndTipHideAnimation();
    }

    public void setUnlockState() {
        this.mHandler.removeCallbacksAndMessages(null);
        startGradientShadowAnimation(322.27f, 0.95f, -1.0f, this.mUnlockStateAnimationEndListener, 1.0f);
        playIconAndTipHideAnimation();
    }

    private void startGradientShadowAnimation(float finalPosition) {
        startGradientShadowAnimation(322.27f, 0.95f, finalPosition);
    }

    public void startGradientShadowAnimation(float stiffness, float dampingRatio, float finalPosition) {
        startGradientShadowAnimation(stiffness, dampingRatio, finalPosition, null, 0.00390625f);
    }

    private void startGradientShadowAnimation(float stiffness, float dampingRatio, float finalPosition, DynamicAnimation.OnAnimationEndListener onAnimationEndListener, float minimumVisibleChange) {
        this.mGradientShadowSpringAnimation.cancel();
        SpringForce springForce = new SpringForce();
        springForce.setStiffness(stiffness);
        springForce.setDampingRatio(dampingRatio);
        springForce.setFinalPosition(finalPosition);
        this.mGradientShadowSpringAnimation.setSpring(springForce);
        DynamicAnimation.OnAnimationEndListener onAnimationEndListener2 = this.mPreOnAnimationEndListener;
        if (onAnimationEndListener2 != null) {
            this.mGradientShadowSpringAnimation.removeEndListener(onAnimationEndListener2);
        }
        if (onAnimationEndListener != null) {
            this.mGradientShadowSpringAnimation.addEndListener(onAnimationEndListener);
        }
        this.mPreOnAnimationEndListener = onAnimationEndListener;
        this.mGradientShadowSpringAnimation.setMinimumVisibleChange(minimumVisibleChange);
        this.mGradientShadowSpringAnimation.start();
    }

    public void updateBlackView() {
        float per = this.mAnimationState.getPerState();
        float topAlpha = constraintAlpha(per + 1.0f);
        float bottomAlpha = constraintAlpha(0.6f + per);
        int topColor = calculateBlackAlpha(topAlpha);
        int bottomColor = calculateBlackAlpha(bottomAlpha);
        if (topColor != this.mPreTopColor || bottomColor != this.mPreBottomColor) {
            this.mPreTopColor = topColor;
            this.mPreBottomColor = bottomColor;
            this.mBlackLinearGradientView.setLinearGradientColor(new int[]{topColor, bottomColor});
        }
        float bulrLevel = constraintBlur(MathUtils.min((float) MiuiFreeformPinManagerService.EDGE_AREA, per) + 1.0f);
        updateBlur(bulrLevel);
    }

    private int calculateBlackAlpha(float alpha) {
        int blackAlpha = (int) (255.0f * alpha);
        return (blackAlpha << 24) | 0;
    }

    private void updateBlur(float level) {
        setBackgroudBlur((int) (100.0f * level));
    }

    private void setBackgroudBlur(int blurRadius) {
        FrameLayout frameLayout = this.mSwipeUpFrameLayout;
        if (frameLayout == null || this.mPreBlurLevel == blurRadius) {
            return;
        }
        this.mPreBlurLevel = blurRadius;
        ViewRootImpl viewRootImpl = frameLayout.getViewRootImpl();
        if (viewRootImpl == null) {
            Slog.d(TAG, "mViewRootImpl is null");
            return;
        }
        SurfaceControl surfaceControl = viewRootImpl.getSurfaceControl();
        SurfaceControl.Transaction transaction = new SurfaceControl.Transaction();
        transaction.setBackgroundBlurRadius(surfaceControl, blurRadius);
        transaction.show(surfaceControl);
        transaction.apply();
    }

    private float constraintAlpha(float alpha) {
        if (alpha > 1.0f) {
            return 1.0f;
        }
        return alpha < MiuiFreeformPinManagerService.EDGE_AREA ? MiuiFreeformPinManagerService.EDGE_AREA : alpha;
    }

    private float constraintBlur(float level) {
        if (level > 1.0f) {
            return 1.0f;
        }
        return level < MiuiFreeformPinManagerService.EDGE_AREA ? MiuiFreeformPinManagerService.EDGE_AREA : level;
    }

    public int transformDpToPx(Context ctx, float dp) {
        return (int) TypedValue.applyDimension(1, dp, ctx.getResources().getDisplayMetrics());
    }

    public int transformDpToPx(float dp) {
        return (int) TypedValue.applyDimension(1, dp, this.mContext.getResources().getDisplayMetrics());
    }

    public void cancelScreenOffDelay() {
        this.mHandler.removeCallbacksAndMessages(null);
        SpringAnimation springAnimation = this.mGradientShadowSpringAnimation;
        if (springAnimation != null) {
            DynamicAnimation.OnAnimationEndListener onAnimationEndListener = this.mPreOnAnimationEndListener;
            if (onAnimationEndListener != null) {
                springAnimation.removeEndListener(onAnimationEndListener);
                this.mPreOnAnimationEndListener = null;
            }
            this.mGradientShadowSpringAnimation.cancel();
        }
    }

    public void handleScreenOff() {
        this.mHandler.removeCallbacksAndMessages(null);
        this.mPowerManager.goToSleep(SystemClock.uptimeMillis(), 3, 0);
    }

    private void updateSettings(boolean bool) {
        Settings.Secure.putIntForUser(this.mContext.getContentResolver(), "swipe_up_is_showing", bool ? 1 : 0, -2);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class SwipeUpWindowHandler extends Handler {
        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public SwipeUpWindowHandler(Looper looper) {
            super(looper);
            SwipeUpWindow.this = r1;
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 101:
                    SwipeUpWindow.this.setLockStateWithLongAnimation();
                    return;
                case 102:
                    SwipeUpWindow.this.playIconAnimation();
                    return;
                case 103:
                    SwipeUpWindow.this.playIconAndTipHideAnimation();
                    return;
                case 104:
                    SwipeUpWindow.this.handleScreenOff();
                    return;
                case 105:
                    SwipeUpWindow.this.releaseSwipeWindow();
                    return;
                default:
                    return;
            }
        }
    }

    public void initVelocityTrackerIfNotExists() {
        if (this.mVelocityTracker == null) {
            this.mVelocityTracker = VelocityTracker.obtain();
        }
    }

    public void recycleVelocityTracker() {
        VelocityTracker velocityTracker = this.mVelocityTracker;
        if (velocityTracker != null) {
            velocityTracker.recycle();
            this.mVelocityTracker = null;
        }
    }

    public float afterFrictionValue(float value, float range) {
        if (range == MiuiFreeformPinManagerService.EDGE_AREA) {
            return MiuiFreeformPinManagerService.EDGE_AREA;
        }
        float t = value >= MiuiFreeformPinManagerService.EDGE_AREA ? 1.0f : -1.0f;
        float per = MathUtils.min(MathUtils.abs(value) / range, 1.0f);
        return (((((per * per) * per) / 3.0f) - (per * per)) + per) * t * range;
    }

    public void playIconAnimation() {
        AnimatedVectorDrawable animatedVectorDrawable = this.mIconDrawable;
        if (animatedVectorDrawable == null) {
            return;
        }
        animatedVectorDrawable.registerAnimationCallback(new Animatable2.AnimationCallback() { // from class: com.android.server.display.SwipeUpWindow.8
            @Override // android.graphics.drawable.Animatable2.AnimationCallback
            public void onAnimationEnd(Drawable drawable) {
                super.onAnimationEnd(drawable);
                SwipeUpWindow.this.mHandler.sendEmptyMessage(103);
            }
        });
        this.mIconDrawable.start();
    }

    public void resetIconAnimation() {
        if (this.mIconDrawable != null && Build.VERSION.SDK_INT >= 23) {
            this.mIconDrawable.reset();
        }
    }

    private void prepareIconAndTipAnimation() {
        ImageView imageView = this.mIconView;
        imageView.setY(imageView.getTop() + transformDpToPx(ICON_OFFSET));
        this.mIconView.setAlpha(MiuiFreeformPinManagerService.EDGE_AREA);
        TextView textView = this.mTipView;
        textView.setY(textView.getTop() + transformDpToPx(50.0f));
        this.mTipView.setAlpha(MiuiFreeformPinManagerService.EDGE_AREA);
    }

    private void playIconAndTipShowAnimation() {
        DualSpringAnimation dualSpringAnimation = this.mIconSpringAnimation;
        if (dualSpringAnimation != null) {
            dualSpringAnimation.cancel();
        }
        DualSpringAnimation dualSpringAnimation2 = this.mTipSpringAnimation;
        if (dualSpringAnimation2 != null) {
            dualSpringAnimation2.cancel();
        }
        this.mIconSpringAnimation = new DualSpringAnimation(creatSpringAnimation(this.mIconView, SpringAnimation.Y, ICON_TIP_SHOW_STIFFNESS, 1.0f, this.mIconView.getTop()), creatSpringAnimation(this.mIconView, SpringAnimation.ALPHA, ICON_TIP_SHOW_STIFFNESS, 1.0f, 1.0f));
        this.mTipSpringAnimation = new DualSpringAnimation(creatSpringAnimation(this.mTipView, SpringAnimation.Y, ICON_TIP_SHOW_STIFFNESS, 1.0f, this.mTipView.getTop()), creatSpringAnimation(this.mTipView, SpringAnimation.ALPHA, ICON_TIP_SHOW_STIFFNESS, 1.0f, 1.0f));
        this.mIconSpringAnimation.start();
        this.mTipSpringAnimation.start();
    }

    public void playIconAndTipHideAnimation() {
        DualSpringAnimation dualSpringAnimation = this.mIconSpringAnimation;
        if (dualSpringAnimation != null) {
            dualSpringAnimation.cancel();
        }
        DualSpringAnimation dualSpringAnimation2 = this.mTipSpringAnimation;
        if (dualSpringAnimation2 != null) {
            dualSpringAnimation2.cancel();
        }
        this.mIconSpringAnimation = new DualSpringAnimation(creatSpringAnimation(this.mIconView, SpringAnimation.Y, this.mIconView.getTop()), creatSpringAnimation(this.mIconView, SpringAnimation.ALPHA, MiuiFreeformPinManagerService.EDGE_AREA));
        this.mTipSpringAnimation = new DualSpringAnimation(creatSpringAnimation(this.mTipView, SpringAnimation.Y, this.mTipView.getTop()), creatSpringAnimation(this.mTipView, SpringAnimation.ALPHA, MiuiFreeformPinManagerService.EDGE_AREA));
        this.mIconSpringAnimation.start();
        this.mTipSpringAnimation.start();
    }

    private SpringAnimation creatSpringAnimation(View view, DynamicAnimation.ViewProperty viewProperty, float finalPosition) {
        return creatSpringAnimation(view, viewProperty, 322.27f, 0.95f, finalPosition);
    }

    public SpringAnimation creatSpringAnimation(View view, DynamicAnimation.ViewProperty viewProperty, float stiffness, float damping, float finalPosition) {
        SpringAnimation springAnimation = new SpringAnimation(view, viewProperty);
        SpringForce springForce = new SpringForce(finalPosition);
        springForce.setStiffness(stiffness);
        springForce.setDampingRatio(damping);
        springAnimation.setSpring(springForce);
        springAnimation.setMinimumVisibleChange(0.00390625f);
        return springAnimation;
    }

    /* loaded from: classes.dex */
    public class SwipeUpFrameLayout extends FrameLayout {
        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public SwipeUpFrameLayout(Context context) {
            super(context);
            SwipeUpWindow.this = this$0;
        }

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public SwipeUpFrameLayout(Context context, AttributeSet attrs) {
            super(context, attrs);
            SwipeUpWindow.this = this$0;
        }

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public SwipeUpFrameLayout(Context context, AttributeSet attrs, int defStyleAttr) {
            super(context, attrs, defStyleAttr);
            SwipeUpWindow.this = this$0;
        }

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public SwipeUpFrameLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
            super(context, attrs, defStyleAttr, defStyleRes);
            SwipeUpWindow.this = this$0;
        }

        @Override // android.view.View
        public void onWindowFocusChanged(boolean hasWindowFocus) {
            super.onWindowFocusChanged(hasWindowFocus);
            SwipeUpWindow.this.startSwipeUpAnimation();
        }
    }

    /* loaded from: classes.dex */
    public final class AnimationState {
        public static final float STATE_LOCK = 1.0f;
        public static final float STATE_UNLOCK = -1.0f;
        public static final float STATE_WAKE = 0.0f;
        private float perState;

        public AnimationState() {
            SwipeUpWindow.this = r1;
            this.perState = 1.0f;
        }

        public AnimationState(float curState) {
            SwipeUpWindow.this = r1;
            this.perState = curState;
        }

        public float getPerState() {
            return this.perState;
        }

        public void setPerState(float perState) {
            this.perState = perState;
        }

        public float getCurrentState() {
            float f = this.perState;
            if (f >= 1.0f) {
                return 1.0f;
            }
            if (f > -1.0f) {
                return MiuiFreeformPinManagerService.EDGE_AREA;
            }
            return -1.0f;
        }
    }

    /* loaded from: classes.dex */
    public final class DualSpringAnimation {
        private SpringAnimation mSpringAnimationAlpha;
        private SpringAnimation mSpringAnimationY;

        public DualSpringAnimation(SpringAnimation springAnimationY, SpringAnimation springAnimationAlpha) {
            SwipeUpWindow.this = r1;
            this.mSpringAnimationY = springAnimationY;
            this.mSpringAnimationAlpha = springAnimationAlpha;
        }

        public void start() {
            this.mSpringAnimationY.start();
            this.mSpringAnimationAlpha.start();
        }

        public void cancel() {
            this.mSpringAnimationY.cancel();
            this.mSpringAnimationAlpha.cancel();
        }

        public void skipToEnd() {
            this.mSpringAnimationY.skipToEnd();
            this.mSpringAnimationAlpha.skipToEnd();
        }

        public void animateToFinalPosition(float y, float alpha) {
            this.mSpringAnimationY.animateToFinalPosition(y);
            this.mSpringAnimationAlpha.animateToFinalPosition(alpha);
        }

        public SpringAnimation getmSpringAnimationY() {
            return this.mSpringAnimationY;
        }

        public void setmSpringAnimationY(SpringAnimation mSpringAnimationY) {
            this.mSpringAnimationY = mSpringAnimationY;
        }

        public SpringAnimation getmSpringAnimationAlpha() {
            return this.mSpringAnimationAlpha;
        }

        public void setmSpringAnimationAlpha(SpringAnimation mSpringAnimationAlpha) {
            this.mSpringAnimationAlpha = mSpringAnimationAlpha;
        }
    }
}
