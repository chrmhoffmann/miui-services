package com.android.server.wm;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.util.RotationUtils;
import android.util.proto.ProtoOutputStream;
import android.view.SurfaceControl;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ClipRectAnimation;
import android.view.animation.Interpolator;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;
import android.view.animation.Transformation;
import com.android.server.wm.LocalAnimationAdapter;
import com.android.server.wm.MiuiRotationAnimationUtils;
import com.android.server.wm.ScreenRotationAnimationImpl;
import com.miui.base.MiuiStubRegistry;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;
import miui.os.Build;
/* loaded from: classes.dex */
public class ScreenRotationAnimationImpl implements ScreenRotationAnimationStub {
    public static final int BLACK_SURFACE_INVALID_POSITION = -10000;
    public static final int COVER_EGE = 800;
    public static final int COVER_OFFSET = 500;
    private static String[] OPAQUE_APPNAME_LIST = null;
    private static final float SCALE_FACTOR = 0.94f;
    private static final String TAG = "ScreenRotationAnimationImpl";
    public static final int TYPE_APP = 2;
    public static final int TYPE_BACKGROUND = 3;
    public static final int TYPE_SCREEN_SHOT = 1;
    public static float[] sTmpFloats = new float[9];
    private int mAlphaDuration;
    private Interpolator mAlphaInterpolator;
    private Context mContext;
    private boolean mEnableRotationAnimation;
    private int mFirstPhaseDuration;
    private Interpolator mFirstPhaseInterpolator;
    private int mLongDuration;
    private Interpolator mLongEaseInterpolator;
    private int mMiddleDuration;
    private Interpolator mMiddleEaseInterpolater;
    private int mMiuiScreenRotationMode;
    private int mScaleDelayTime;
    private int mScaleDuration;
    private float mScaleFactor;
    private int mScreenRotationDuration;
    private int mShortAlphaDuration;
    private Interpolator mShortAlphaInterpolator;
    private int mShortDuration;
    private Interpolator mShortEaseInterpolater;
    SurfaceControl mSurfaceControlCoverBt;
    SurfaceControl mSurfaceControlCoverLt;
    SurfaceControl mSurfaceControlCoverRt;
    SurfaceControl mSurfaceControlCoverTp;
    private WindowManagerService mWms;
    private Interpolator mSinEaseOutInterpolator = new MiuiRotationAnimationUtils.SineEaseOutInterpolater();
    private final Interpolator mQuartEaseOutInterpolator = new MiuiRotationAnimationUtils.EaseQuartOutInterpolator();
    private final Interpolator mSinEaseInOutInterpolator = new MiuiRotationAnimationUtils.EaseSineInOutInterpolator();

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class TmpValues {
        final Transformation transformation = new Transformation();
        final float[] floats = new float[9];
    }

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<ScreenRotationAnimationImpl> {

        /* compiled from: ScreenRotationAnimationImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final ScreenRotationAnimationImpl INSTANCE = new ScreenRotationAnimationImpl();
        }

        public ScreenRotationAnimationImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public ScreenRotationAnimationImpl provideNewInstance() {
            return new ScreenRotationAnimationImpl();
        }
    }

    public void init(Context context, WindowManagerService wms) {
        this.mContext = context;
        this.mWms = wms;
        this.mMiuiScreenRotationMode = context.getResources().getInteger(17694731);
        this.mLongEaseInterpolator = new PhysicBasedInterpolator(this.mContext.getResources().getFloat(17105064), this.mContext.getResources().getFloat(17105065));
        this.mMiddleEaseInterpolater = new PhysicBasedInterpolator(this.mContext.getResources().getFloat(17105064), this.mContext.getResources().getFloat(17105066));
        this.mShortEaseInterpolater = new PhysicBasedInterpolator(this.mContext.getResources().getFloat(17105064), this.mContext.getResources().getFloat(17105067));
        this.mAlphaInterpolator = this.mSinEaseOutInterpolator;
        this.mShortAlphaInterpolator = new MiuiRotationAnimationUtils.SineEaseInInterpolater();
        this.mFirstPhaseInterpolator = this.mMiuiScreenRotationMode == 2 ? this.mSinEaseOutInterpolator : this.mShortEaseInterpolater;
        this.mLongDuration = this.mContext.getResources().getInteger(17694728);
        this.mMiddleDuration = this.mContext.getResources().getInteger(17694730);
        this.mShortDuration = this.mContext.getResources().getInteger(17694735);
        this.mScaleDuration = this.mContext.getResources().getInteger(17694732);
        this.mScaleDelayTime = this.mContext.getResources().getInteger(17694733);
        this.mAlphaDuration = this.mContext.getResources().getInteger(17694727);
        this.mShortAlphaDuration = this.mContext.getResources().getInteger(17694734);
        this.mFirstPhaseDuration = this.mMiuiScreenRotationMode == 2 ? this.mScaleDuration : this.mShortDuration;
        this.mScaleFactor = this.mContext.getResources().getFloat(17105068);
        this.mScreenRotationDuration = this.mScaleDelayTime + this.mLongDuration;
        this.mEnableRotationAnimation = this.mContext.getResources().getBoolean(17891646);
        if (Build.IS_TABLET) {
            OPAQUE_APPNAME_LIST = new String[]{"com.miui.video", "com.miui.mediaviewer", "com.youku.phone"};
        } else {
            OPAQUE_APPNAME_LIST = new String[]{"com.miui.video", "com.miui.mediaviewer", "tv.danmaku.bili", "com.youku.phone"};
        }
    }

    public int getScreenRotationAnimationMode() {
        return this.mMiuiScreenRotationMode;
    }

    public boolean getIsCancelRotationAnimation() {
        return !this.mEnableRotationAnimation && this.mWms.isKeyguardShowingAndNotOccluded();
    }

    public Animation loadExitAnimation(int width, int height, int originRotation, int curRotation) {
        return createRotationExitAnimation(width, height, originRotation, curRotation);
    }

    public Animation loadEnterAnimation(int width, int height, int originRotation, int curRotation, SurfaceControl surfaceControlBg, SurfaceControl.Transaction t) {
        t.hide(surfaceControlBg);
        return createRotationEnterAnimation(width, height, originRotation, curRotation);
    }

    public Animation loadRotation180Exit() {
        return createRotation180Exit();
    }

    public Animation loadRotation180Enter() {
        return createRotation180Enter();
    }

    public Animation createRotationExitAnimation(int width, int height, int originRotation, int curRotation) {
        int curHeight;
        int curWidth;
        AnimationSet set = new AnimationSet(false);
        float scale = height / width;
        int delta = RotationUtils.deltaRotation(originRotation, curRotation);
        if (curRotation == 1 || curRotation == 3) {
            curWidth = height;
            curHeight = width;
            if (this.mMiuiScreenRotationMode == 2) {
                this.mAlphaDuration = this.mScaleDuration;
            }
        } else {
            curWidth = width;
            curHeight = height;
            if (this.mMiuiScreenRotationMode == 2) {
                this.mAlphaInterpolator = this.mShortAlphaInterpolator;
                this.mAlphaDuration = this.mShortAlphaDuration;
            }
        }
        int curHeight2 = curHeight;
        int curHeight3 = curWidth;
        RotateAnimation rotateAnimation = new RotateAnimation(MiuiFreeformPinManagerService.EDGE_AREA, delta == 1 ? 90.0f : -90.0f, 1, 0.5f, 1, 0.5f);
        rotateAnimation.setInterpolator(this.mMiddleEaseInterpolater);
        rotateAnimation.setDuration(this.mMiddleDuration);
        rotateAnimation.setFillAfter(true);
        rotateAnimation.setFillBefore(true);
        rotateAnimation.setFillEnabled(true);
        set.addAnimation(rotateAnimation);
        AlphaAnimation alphaAnimation = new AlphaAnimation(1.0f, (float) MiuiFreeformPinManagerService.EDGE_AREA);
        alphaAnimation.setInterpolator(this.mAlphaInterpolator);
        alphaAnimation.setDuration(this.mAlphaDuration);
        alphaAnimation.setFillAfter(true);
        alphaAnimation.setFillBefore(true);
        alphaAnimation.setFillEnabled(true);
        set.addAnimation(alphaAnimation);
        ShotClipAnimation shotClipAnimation = new ShotClipAnimation(curHeight3, curHeight2, 1.0f, scale, 1, 0.5f, 1, 0.5f, 0, 0, curHeight3, curHeight2, ((int) (curHeight3 - (curHeight2 / scale))) / 2, ((int) (curHeight2 - (curHeight3 / scale))) / 2, ((int) (curHeight3 + (curHeight2 / scale))) / 2, ((int) (curHeight2 + (curHeight3 / scale))) / 2);
        shotClipAnimation.setDuration(this.mShortDuration);
        shotClipAnimation.setInterpolator(this.mShortEaseInterpolater);
        shotClipAnimation.setFillAfter(true);
        shotClipAnimation.setFillBefore(false);
        shotClipAnimation.setFillEnabled(true);
        set.addAnimation(shotClipAnimation);
        ScreenScaleAnimation screenScaleAnimation = new ScreenScaleAnimation(this.mScaleFactor, 1.0f, 1, 0.5f, 1, 0.5f);
        screenScaleAnimation.setDuration(this.mScreenRotationDuration);
        screenScaleAnimation.setFirstPhaseDuration(this.mFirstPhaseDuration);
        screenScaleAnimation.setSecPhaseDuration(this.mLongDuration);
        screenScaleAnimation.setAnimationInterpolator(this.mLongEaseInterpolator, this.mFirstPhaseInterpolator);
        screenScaleAnimation.setScaleBreakOffset((this.mScaleDelayTime * 1.0f) / this.mScreenRotationDuration);
        screenScaleAnimation.setScaleDelayTime(this.mScaleDelayTime);
        screenScaleAnimation.setFillAfter(true);
        screenScaleAnimation.setFillBefore(true);
        screenScaleAnimation.setFillEnabled(true);
        set.addAnimation(screenScaleAnimation);
        return set;
    }

    public Animation createRotationEnterAnimation(int width, int height, int originRotation, int curRotation) {
        int curWidth;
        int curHeight;
        AnimationSet set = new AnimationSet(false);
        float scale = height / width;
        int delta = RotationUtils.deltaRotation(originRotation, curRotation);
        if (curRotation == 1 || curRotation == 3) {
            curWidth = height;
            curHeight = width;
        } else {
            curWidth = width;
            curHeight = height;
        }
        RotateAnimation rotateAnimation = new RotateAnimation(delta == 3 ? 90.0f : -90.0f, MiuiFreeformPinManagerService.EDGE_AREA, 1, 0.5f, 1, 0.5f);
        rotateAnimation.setInterpolator(this.mMiddleEaseInterpolater);
        rotateAnimation.setDuration(this.mMiddleDuration);
        rotateAnimation.setFillAfter(true);
        rotateAnimation.setFillBefore(true);
        rotateAnimation.setFillEnabled(true);
        set.addAnimation(rotateAnimation);
        Animation clipRectAnimation = new ClipRectAnimation(((int) (curWidth - (curHeight / scale))) / 2, ((int) (curHeight - (curWidth / scale))) / 2, ((int) (curWidth + (curHeight / scale))) / 2, ((int) (curHeight + (curWidth / scale))) / 2, 0, 0, curWidth, curHeight);
        clipRectAnimation.setInterpolator(this.mShortEaseInterpolater);
        clipRectAnimation.setDuration(this.mShortDuration);
        clipRectAnimation.setFillAfter(true);
        clipRectAnimation.setFillBefore(true);
        clipRectAnimation.setFillEnabled(true);
        set.addAnimation(clipRectAnimation);
        ScaleAnimation scaleAnimation = new ScaleAnimation(scale, 1.0f, scale, 1.0f, 1, 0.5f, 1, 0.5f);
        scaleAnimation.setInterpolator(this.mShortEaseInterpolater);
        scaleAnimation.setDuration(this.mShortDuration);
        scaleAnimation.setFillAfter(false);
        scaleAnimation.setFillBefore(false);
        scaleAnimation.setFillEnabled(true);
        set.addAnimation(scaleAnimation);
        ScreenScaleAnimation screenScaleAnimation = new ScreenScaleAnimation(this.mScaleFactor, 1.0f, 1, 0.5f, 1, 0.5f);
        screenScaleAnimation.setDuration(this.mScreenRotationDuration);
        screenScaleAnimation.setFirstPhaseDuration(this.mFirstPhaseDuration);
        screenScaleAnimation.setSecPhaseDuration(this.mLongDuration);
        screenScaleAnimation.setAnimationInterpolator(this.mLongEaseInterpolator, this.mFirstPhaseInterpolator);
        screenScaleAnimation.setScaleBreakOffset((this.mScaleDelayTime * 1.0f) / this.mScreenRotationDuration);
        screenScaleAnimation.setScaleDelayTime(this.mScaleDelayTime);
        screenScaleAnimation.setFillAfter(true);
        screenScaleAnimation.setFillBefore(true);
        screenScaleAnimation.setFillEnabled(true);
        set.addAnimation(screenScaleAnimation);
        return set;
    }

    public LocalAnimationAdapter.AnimationSpec createScreenRotationSpec(Animation animation, int originalWidth, int originalHeight) {
        return new ScreenRotationSpec(animation, originalWidth, originalHeight);
    }

    public void kill(SurfaceControl.Transaction t) {
        SurfaceControl surfaceControl = this.mSurfaceControlCoverLt;
        if (surfaceControl != null) {
            if (surfaceControl.isValid()) {
                t.remove(this.mSurfaceControlCoverLt);
            }
            this.mSurfaceControlCoverLt = null;
        }
        SurfaceControl surfaceControl2 = this.mSurfaceControlCoverTp;
        if (surfaceControl2 != null) {
            if (surfaceControl2.isValid()) {
                t.remove(this.mSurfaceControlCoverTp);
            }
            this.mSurfaceControlCoverTp = null;
        }
        SurfaceControl surfaceControl3 = this.mSurfaceControlCoverRt;
        if (surfaceControl3 != null) {
            if (surfaceControl3.isValid()) {
                t.remove(this.mSurfaceControlCoverRt);
            }
            this.mSurfaceControlCoverRt = null;
        }
        SurfaceControl surfaceControl4 = this.mSurfaceControlCoverBt;
        if (surfaceControl4 != null) {
            if (surfaceControl4.isValid()) {
                t.remove(this.mSurfaceControlCoverBt);
            }
            this.mSurfaceControlCoverBt = null;
        }
    }

    /* loaded from: classes.dex */
    private static class ScreenRotationSpec implements LocalAnimationAdapter.AnimationSpec {
        private Animation mAnimation;
        private int mHeight;
        private final ThreadLocal<TmpValues> mThreadLocalTmps = ThreadLocal.withInitial(new Supplier() { // from class: com.android.server.wm.ScreenRotationAnimationImpl$ScreenRotationSpec$$ExternalSyntheticLambda0
            @Override // java.util.function.Supplier
            public final Object get() {
                return new ScreenRotationAnimationImpl.TmpValues();
            }
        });
        private int mWidth;

        public ScreenRotationSpec(Animation animation, int originalWidth, int originalHeight) {
            this.mAnimation = animation;
            this.mWidth = originalWidth;
            this.mHeight = originalHeight;
        }

        public long getDuration() {
            return this.mAnimation.computeDurationHint();
        }

        public void apply(SurfaceControl.Transaction t, SurfaceControl leash, long currentPlayTime) {
            TmpValues tmp = this.mThreadLocalTmps.get();
            tmp.transformation.clear();
            this.mAnimation.getTransformation(currentPlayTime, tmp.transformation);
            t.setMatrix(leash, tmp.transformation.getMatrix(), tmp.floats);
            t.setAlpha(leash, tmp.transformation.getAlpha());
            if (tmp.transformation.hasClipRect()) {
                t.setCornerRadius(leash, AppTransitionInjector.DISPLAY_ROUND_CORNER_RADIUS);
                t.setWindowCrop(leash, new Rect(tmp.transformation.getClipRect().left, tmp.transformation.getClipRect().top, tmp.transformation.getClipRect().right, tmp.transformation.getClipRect().bottom));
                return;
            }
            t.setCornerRadius(leash, AppTransitionInjector.DISPLAY_ROUND_CORNER_RADIUS);
            t.setWindowCrop(leash, new Rect(0, 0, this.mWidth, this.mHeight));
        }

        public void dump(PrintWriter pw, String prefix) {
            pw.print(prefix);
            pw.println(this.mAnimation);
        }

        public void dumpDebugInner(ProtoOutputStream proto) {
            long token = proto.start(1146756268033L);
            proto.write(1138166333441L, this.mAnimation.toString());
            proto.end(token);
        }
    }

    /* loaded from: classes.dex */
    public static class ScreenScaleAnimation extends Animation {
        private float mFinalScale;
        private int mFirstPhaseDuration;
        private Interpolator mLongEaseInterpolator;
        private float mMiddleScale;
        private float mPivotX;
        private int mPivotXType;
        private float mPivotXValue;
        private float mPivotY;
        private int mPivotYType;
        private float mPivotYValue;
        private int mScaleDelayTime;
        private int mSecPhaseDuration;
        private Interpolator mShortEaseInterpolator;
        private float mScale = 1.0f;
        private float mNextPhaseScale = 1.0f;
        private boolean isGetCurScale = true;
        private float mScaleBreakOffset = MiuiFreeformPinManagerService.EDGE_AREA;

        public ScreenScaleAnimation(float middleScale, float finalScale, int pivotXType, float pivotXValue, int pivotYType, float pivotYValue) {
            this.mPivotXType = 0;
            this.mPivotYType = 0;
            this.mPivotXValue = MiuiFreeformPinManagerService.EDGE_AREA;
            this.mPivotYValue = MiuiFreeformPinManagerService.EDGE_AREA;
            this.mMiddleScale = middleScale;
            this.mFinalScale = finalScale;
            this.mPivotXValue = pivotXValue;
            this.mPivotXType = pivotXType;
            this.mPivotYValue = pivotYValue;
            this.mPivotYType = pivotYType;
            initializePivotPoint();
        }

        private void initializePivotPoint() {
            if (this.mPivotXType == 0) {
                this.mPivotX = this.mPivotXValue;
            }
            if (this.mPivotYType == 0) {
                this.mPivotY = this.mPivotYValue;
            }
        }

        public void setFirstPhaseDuration(int firstPhaseDuration) {
            this.mFirstPhaseDuration = firstPhaseDuration;
        }

        public void setSecPhaseDuration(int secPhaseDuration) {
            this.mSecPhaseDuration = secPhaseDuration;
        }

        public void setAnimationInterpolator(Interpolator longEaseInterpolator, Interpolator shortEaseInterpolator) {
            this.mLongEaseInterpolator = longEaseInterpolator;
            this.mShortEaseInterpolator = shortEaseInterpolator;
        }

        public void setScaleBreakOffset(float scaleBreakOffset) {
            this.mScaleBreakOffset = scaleBreakOffset;
        }

        public void setScaleDelayTime(int scaleDelayTime) {
            this.mScaleDelayTime = scaleDelayTime;
        }

        @Override // android.view.animation.Animation
        public boolean getTransformation(long currentTime, Transformation outTransformation) {
            float normalizedTime;
            long startOffset = getStartOffset();
            long duration = getDuration();
            long startTime = getStartTime();
            if (duration != 0) {
                normalizedTime = ((float) (currentTime - (startTime + startOffset))) / ((float) duration);
            } else {
                normalizedTime = currentTime < startTime ? 0.0f : 1.0f;
            }
            boolean fillAfter = getFillAfter();
            boolean fillBefore = getFillBefore();
            boolean fillEnabled = isFillEnabled();
            if (!fillEnabled) {
                normalizedTime = Math.max(Math.min(normalizedTime, 1.0f), (float) MiuiFreeformPinManagerService.EDGE_AREA);
            }
            if ((normalizedTime >= MiuiFreeformPinManagerService.EDGE_AREA || fillBefore) && (normalizedTime <= 1.0f || fillAfter)) {
                if (fillEnabled) {
                    normalizedTime = Math.max(Math.min(normalizedTime, 1.0f), (float) MiuiFreeformPinManagerService.EDGE_AREA);
                }
                if (normalizedTime <= this.mScaleBreakOffset) {
                    float interpolatedTime = this.mShortEaseInterpolator.getInterpolation((((int) duration) * normalizedTime) / this.mFirstPhaseDuration);
                    float f = this.mFinalScale;
                    this.mScale = f + ((this.mMiddleScale - f) * interpolatedTime);
                } else {
                    if (this.isGetCurScale) {
                        this.mNextPhaseScale = this.mScale;
                        this.isGetCurScale = false;
                    }
                    Interpolator interpolator = this.mLongEaseInterpolator;
                    int i = this.mSecPhaseDuration;
                    float interpolatedTime2 = interpolator.getInterpolation(((((int) duration) * normalizedTime) / i) - ((this.mScaleDelayTime * 1.0f) / i));
                    float f2 = this.mNextPhaseScale;
                    this.mScale = f2 + ((this.mFinalScale - f2) * interpolatedTime2);
                }
            }
            boolean more = super.getTransformation(currentTime, outTransformation);
            return more;
        }

        @Override // android.view.animation.Animation
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            float scale = getScaleFactor();
            if (this.mPivotX == MiuiFreeformPinManagerService.EDGE_AREA && this.mPivotY == MiuiFreeformPinManagerService.EDGE_AREA) {
                Matrix matrix = t.getMatrix();
                float f = this.mScale;
                matrix.setScale(f, f);
                return;
            }
            Matrix matrix2 = t.getMatrix();
            float f2 = this.mScale;
            matrix2.setScale(f2, f2, this.mPivotX * scale, this.mPivotY * scale);
        }

        @Override // android.view.animation.Animation
        public void initialize(int width, int height, int parentWidth, int parentHeight) {
            super.initialize(width, height, parentWidth, parentHeight);
            this.mPivotX = resolveSize(this.mPivotXType, this.mPivotXValue, width, parentWidth);
            this.mPivotY = resolveSize(this.mPivotYType, this.mPivotYValue, height, parentHeight);
        }
    }

    /* loaded from: classes.dex */
    public static class ShotClipAnimation extends Animation {
        private int mCurHeight;
        private int mCurWidth;
        protected final Rect mFromRect;
        private float mFromScale;
        private Interpolator mInterpolator;
        private float mPivotX;
        private int mPivotXType;
        private float mPivotXValue;
        private float mPivotY;
        private int mPivotYType;
        private float mPivotYValue;
        private int mShotBottom;
        private int mShotLeft;
        private int mShotRight;
        private float mShotScale;
        private int mShotTop;
        protected final Rect mToRect;
        private float mToScale;

        public ShotClipAnimation(int curWidth, int curHeight, float fromScale, float toScale, int pivotXType, float pivotXValue, int pivotYType, float pivotYValue, int fromL, int fromT, int fromR, int fromB, int toL, int toT, int toR, int toB) {
            this.mPivotXType = 0;
            this.mPivotYType = 0;
            this.mPivotXValue = MiuiFreeformPinManagerService.EDGE_AREA;
            this.mPivotYValue = MiuiFreeformPinManagerService.EDGE_AREA;
            this.mFromRect = new Rect(fromL, fromT, fromR, fromB);
            this.mToRect = new Rect(toL, toT, toR, toB);
            this.mFromScale = fromScale;
            this.mToScale = toScale;
            this.mPivotXValue = pivotXValue;
            this.mPivotXType = pivotXType;
            this.mPivotYValue = pivotYValue;
            this.mPivotYType = pivotYType;
            this.mCurWidth = curWidth;
            this.mCurHeight = curHeight;
            initializePivotPoint();
        }

        private void initializePivotPoint() {
            if (this.mPivotXType == 0) {
                this.mPivotX = this.mPivotXValue;
            }
            if (this.mPivotYType == 0) {
                this.mPivotY = this.mPivotYValue;
            }
        }

        @Override // android.view.animation.Animation
        public void setInterpolator(Interpolator interpolator) {
            this.mInterpolator = interpolator;
        }

        @Override // android.view.animation.Animation
        public boolean getTransformation(long currentTime, Transformation outTransformation) {
            float normalizedTime;
            long startOffset = getStartOffset();
            long duration = getDuration();
            long startTime = getStartTime();
            if (duration != 0) {
                normalizedTime = ((float) (currentTime - (startTime + startOffset))) / ((float) duration);
            } else {
                normalizedTime = currentTime < startTime ? 0.0f : 1.0f;
            }
            boolean fillAfter = getFillAfter();
            boolean fillBefore = getFillBefore();
            boolean fillEnabled = isFillEnabled();
            if (!fillEnabled) {
                normalizedTime = Math.max(Math.min(normalizedTime, 1.0f), (float) MiuiFreeformPinManagerService.EDGE_AREA);
            }
            if ((normalizedTime >= MiuiFreeformPinManagerService.EDGE_AREA || fillBefore) && (normalizedTime <= 1.0f || fillAfter)) {
                if (fillEnabled) {
                    normalizedTime = Math.max(Math.min(normalizedTime, 1.0f), (float) MiuiFreeformPinManagerService.EDGE_AREA);
                }
                float interpolatedTime = this.mInterpolator.getInterpolation(normalizedTime);
                int shortLength = Math.min(this.mCurWidth, this.mCurHeight);
                float f = this.mToScale;
                float appScale = f + ((this.mFromScale - f) * interpolatedTime);
                int appLeft = this.mToRect.left + ((int) ((this.mFromRect.left - this.mToRect.left) * interpolatedTime));
                int appTop = this.mToRect.top + ((int) ((this.mFromRect.top - this.mToRect.top) * interpolatedTime));
                int appRight = this.mToRect.right + ((int) ((this.mFromRect.right - this.mToRect.right) * interpolatedTime));
                int appBottom = this.mToRect.bottom + ((int) ((this.mFromRect.bottom - this.mToRect.bottom) * interpolatedTime));
                int i = this.mCurWidth;
                int i2 = this.mCurHeight;
                int appLength = i > i2 ? appRight - appLeft : appBottom - appTop;
                float f2 = (appLength * appScale) / (shortLength * 1.0f);
                this.mShotScale = f2;
                int shotClipLength = (int) ((shortLength * appScale) / f2);
                if (i > i2) {
                    this.mShotLeft = (i - shotClipLength) / 2;
                    this.mShotTop = 0;
                    this.mShotRight = (i + shotClipLength) / 2;
                    this.mShotBottom = i2;
                } else {
                    this.mShotLeft = 0;
                    this.mShotTop = (i2 - shotClipLength) / 2;
                    this.mShotRight = i;
                    this.mShotBottom = (i2 + shotClipLength) / 2;
                }
            }
            boolean more = super.getTransformation(currentTime, outTransformation);
            return more;
        }

        @Override // android.view.animation.Animation
        protected void applyTransformation(float interpolatedTime, Transformation tr) {
            float scale = getScaleFactor();
            tr.setClipRect(this.mShotLeft, this.mShotTop, this.mShotRight, this.mShotBottom);
            if (this.mPivotX == MiuiFreeformPinManagerService.EDGE_AREA && this.mPivotY == MiuiFreeformPinManagerService.EDGE_AREA) {
                Matrix matrix = tr.getMatrix();
                float f = this.mShotScale;
                matrix.setScale(f, f);
                return;
            }
            Matrix matrix2 = tr.getMatrix();
            float f2 = this.mShotScale;
            matrix2.setScale(f2, f2, this.mPivotX * scale, this.mPivotY * scale);
        }

        @Override // android.view.animation.Animation
        public void initialize(int width, int height, int parentWidth, int parentHeight) {
            super.initialize(width, height, parentWidth, parentHeight);
            this.mPivotX = resolveSize(this.mPivotXType, this.mPivotXValue, width, parentWidth);
            this.mPivotY = resolveSize(this.mPivotYType, this.mPivotYValue, height, parentHeight);
        }
    }

    public int getDisplayRoundCornerRadius() {
        return AppTransitionInjector.DISPLAY_ROUND_CORNER_RADIUS;
    }

    public boolean isOpaqueHasVideo(DisplayContent displayContent) {
        ActivityRecord topSplitscreen;
        ActivityRecord bottomSplitscreen;
        ActivityRecord fullScreen;
        Task splitPrimaryTask = displayContent.getDefaultTaskDisplayArea().getTopRootTaskInWindowingMode(3);
        ActivityRecord freeForm = null;
        if (splitPrimaryTask == null) {
            topSplitscreen = null;
        } else {
            topSplitscreen = splitPrimaryTask.topRunningActivity();
        }
        Task splitSecondaryTask = displayContent.getDefaultTaskDisplayArea().getTopRootTaskInWindowingMode(4);
        if (splitSecondaryTask == null) {
            bottomSplitscreen = null;
        } else {
            bottomSplitscreen = splitSecondaryTask.topRunningActivity();
        }
        Task fullScreenTask = displayContent.getDefaultTaskDisplayArea().getTopRootTaskInWindowingMode(1);
        if (fullScreenTask == null) {
            fullScreen = null;
        } else {
            fullScreen = fullScreenTask.topRunningActivity();
        }
        Task freeFormTask = displayContent.getDefaultTaskDisplayArea().getTopRootTaskInWindowingMode(5);
        if (freeFormTask != null) {
            freeForm = freeFormTask.topRunningActivity();
        }
        String topSplitscreenPkgName = "";
        String bottomSplitscreenPkgName = "";
        String fullScreenPkgName = "";
        String freeFormPkgName = "";
        if (topSplitscreen != null && topSplitscreen.isVisible()) {
            topSplitscreenPkgName = topSplitscreen.packageName;
        }
        if (bottomSplitscreen != null && bottomSplitscreen.isVisible()) {
            bottomSplitscreenPkgName = bottomSplitscreen.packageName;
        }
        if (fullScreen != null && fullScreen.isVisible()) {
            fullScreenPkgName = fullScreen.packageName;
        }
        if (freeForm != null && freeForm.isVisible()) {
            freeFormPkgName = freeForm.packageName;
        }
        final String activityName = "" + topSplitscreenPkgName + bottomSplitscreenPkgName + fullScreenPkgName + freeFormPkgName;
        Stream stream = Arrays.stream(OPAQUE_APPNAME_LIST);
        Objects.requireNonNull(activityName);
        boolean isOpaque = stream.noneMatch(new Predicate() { // from class: com.android.server.wm.ScreenRotationAnimationImpl$$ExternalSyntheticLambda0
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return activityName.contains((String) obj);
            }
        });
        return isOpaque;
    }

    public Animation createRotation180Exit() {
        AnimationSet set = new AnimationSet(false);
        int screenrotationduration = MiuiRotationAnimationUtils.getRotationDuration();
        RotateAnimation rotateAnimation = new RotateAnimation(MiuiFreeformPinManagerService.EDGE_AREA, -180.0f, 1, 0.5f, 1, 0.5f);
        rotateAnimation.setInterpolator(this.mQuartEaseOutInterpolator);
        rotateAnimation.setDuration(screenrotationduration);
        rotateAnimation.setFillAfter(true);
        rotateAnimation.setFillBefore(true);
        rotateAnimation.setFillEnabled(true);
        set.addAnimation(rotateAnimation);
        AlphaAnimation alphaAnimation = new AlphaAnimation(1.0f, (float) MiuiFreeformPinManagerService.EDGE_AREA);
        alphaAnimation.setInterpolator(this.mQuartEaseOutInterpolator);
        alphaAnimation.setDuration(screenrotationduration / 2);
        alphaAnimation.setFillAfter(true);
        alphaAnimation.setFillBefore(true);
        alphaAnimation.setFillEnabled(true);
        set.addAnimation(alphaAnimation);
        ScreenScale180Animation scalephase1 = new ScreenScale180Animation(1.0f, SCALE_FACTOR, 1.0f, SCALE_FACTOR, 1, 0.5f, 1, 0.5f);
        scalephase1.setInterpolator(this.mQuartEaseOutInterpolator);
        scalephase1.setDuration(screenrotationduration / 3);
        scalephase1.setFillAfter(false);
        scalephase1.setFillBefore(false);
        scalephase1.setFillEnabled(true);
        set.addAnimation(scalephase1);
        ScreenScale180Animation scalephase2 = new ScreenScale180Animation(SCALE_FACTOR, 1.0f, SCALE_FACTOR, 1.0f, 1, 0.5f, 1, 0.5f);
        scalephase2.setInterpolator(this.mSinEaseInOutInterpolator);
        scalephase2.setStartOffset(screenrotationduration / 3);
        scalephase2.setDuration((screenrotationduration * 2) / 3);
        scalephase2.setFillAfter(true);
        scalephase2.setFillBefore(false);
        scalephase2.setFillEnabled(true);
        set.addAnimation(scalephase2);
        return set;
    }

    public Animation createRotation180Enter() {
        AnimationSet set = new AnimationSet(false);
        int screenrotationduration = MiuiRotationAnimationUtils.getRotationDuration();
        RotateAnimation rotateAnimation = new RotateAnimation(180.0f, MiuiFreeformPinManagerService.EDGE_AREA, 1, 0.5f, 1, 0.5f);
        rotateAnimation.setInterpolator(this.mQuartEaseOutInterpolator);
        rotateAnimation.setDuration(screenrotationduration);
        rotateAnimation.setFillAfter(true);
        rotateAnimation.setFillBefore(true);
        rotateAnimation.setFillEnabled(true);
        set.addAnimation(rotateAnimation);
        ScreenScale180Animation scalephase1 = new ScreenScale180Animation(1.0f, SCALE_FACTOR, 1.0f, SCALE_FACTOR, 1, 0.5f, 1, 0.5f);
        scalephase1.setInterpolator(this.mQuartEaseOutInterpolator);
        scalephase1.setDuration(screenrotationduration / 3);
        scalephase1.setFillAfter(false);
        scalephase1.setFillBefore(false);
        scalephase1.setFillEnabled(true);
        set.addAnimation(scalephase1);
        ScreenScale180Animation scalephase2 = new ScreenScale180Animation(SCALE_FACTOR, 1.0f, SCALE_FACTOR, 1.0f, 1, 0.5f, 1, 0.5f);
        scalephase2.setInterpolator(this.mSinEaseInOutInterpolator);
        scalephase2.setStartOffset(screenrotationduration / 3);
        scalephase2.setDuration((screenrotationduration * 2) / 3);
        scalephase2.setFillAfter(true);
        scalephase2.setFillBefore(false);
        scalephase2.setFillEnabled(true);
        set.addAnimation(scalephase2);
        return set;
    }

    /* loaded from: classes.dex */
    public static class ScreenScale180Animation extends ScaleAnimation {
        public ScreenScale180Animation(float fromX, float toX, float fromY, float toY, int pivotXType, float pivotXValue, int pivotYType, float pivotYValue) {
            super(fromX, toX, fromY, toY, pivotXType, pivotXValue, pivotYType, pivotYValue);
        }

        @Override // android.view.animation.Animation
        public boolean getTransformation(long currentTime, Transformation outTransformation) {
            if (getDuration() != 0) {
                return super.getTransformation(currentTime, outTransformation);
            }
            return false;
        }
    }
}
