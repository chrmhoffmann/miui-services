package miui.android.animation.controller;

import android.app.UiModeManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.ArrayMap;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import com.android.server.wm.MiuiFreeformPinManagerService;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import java.util.WeakHashMap;
import miui.android.animation.Folme;
import miui.android.animation.IAnimTarget;
import miui.android.animation.IHoverStyle;
import miui.android.animation.ViewTarget;
import miui.android.animation.base.AnimConfig;
import miui.android.animation.internal.AnimValueUtils;
import miui.android.animation.listener.TransitionListener;
import miui.android.animation.listener.UpdateInfo;
import miui.android.animation.property.FloatProperty;
import miui.android.animation.property.ViewProperty;
import miui.android.animation.property.ViewPropertyExt;
import miui.android.animation.utils.CommonUtils;
import miui.android.animation.utils.EaseManager;
import miui.android.animation.utils.KeyUtils;
import miui.android.animation.utils.LogUtils;
import miui.android.folme.R;
/* loaded from: classes.dex */
public class FolmeHover extends FolmeBase implements IHoverStyle {
    private static final int CORNER_DIS = 36;
    private static final float DEFAULT_CORNER = 0.5f;
    private static final float DEFAULT_SCALE = 1.15f;
    private static final int SCALE_DIS = 12;
    private static WeakHashMap<View, InnerViewHoverListener> sHoverRecord = new WeakHashMap<>();
    private WeakReference<View> mChildView;
    private WeakReference<View> mHoverView;
    private boolean mIsEnter;
    private WeakReference<View> mParentView;
    private boolean mSetTint;
    private Rect mViewRect;
    private float mTranslateDist = Float.MAX_VALUE;
    private AnimConfig mMoveConfig = new AnimConfig().setEase(EaseManager.getStyle(-2, 0.9f, 0.4f));
    private AnimConfig mEnterConfig = new AnimConfig();
    private AnimConfig mExitConfig = new AnimConfig();
    private Map<IHoverStyle.HoverType, Boolean> mScaleSetMap = new ArrayMap();
    private Map<IHoverStyle.HoverType, Boolean> mTranslationSetMap = new ArrayMap();
    private IHoverStyle.HoverEffect mCurrentEffect = IHoverStyle.HoverEffect.NORMAL;
    private boolean isSetAutoTranslation = false;
    private boolean mClearTint = false;
    private int[] mLocation = new int[2];
    private float mRadius = MiuiFreeformPinManagerService.EDGE_AREA;
    private int mTargetWidth = 0;
    private int mTargetHeight = 0;
    private String HoverMoveType = "MOVE";
    private TransitionListener mDefListener = new TransitionListener() { // from class: miui.android.animation.controller.FolmeHover.1
        @Override // miui.android.animation.listener.TransitionListener
        public void onBegin(Object toTag, Collection<UpdateInfo> updateList) {
            if (toTag.equals(IHoverStyle.HoverType.ENTER)) {
                AnimState.alignState(FolmeHover.this.mState.getState(IHoverStyle.HoverType.EXIT), updateList);
            }
        }
    };

    public FolmeHover(IAnimTarget... targets) {
        super(targets);
        initDist(targets.length > 0 ? targets[0] : null);
        updateHoverState(this.mCurrentEffect);
        this.mEnterConfig.setEase(EaseManager.getStyle(-2, 0.99f, 0.15f));
        this.mEnterConfig.addListeners(this.mDefListener);
        this.mExitConfig.setEase(-2, 0.99f, 0.3f).setSpecial(ViewProperty.ALPHA, -2L, 0.9f, 0.2f);
    }

    private void setAutoScale() {
        this.mScaleSetMap.put(IHoverStyle.HoverType.ENTER, true);
        this.mScaleSetMap.put(IHoverStyle.HoverType.EXIT, true);
        this.mState.getState(IHoverStyle.HoverType.EXIT).add(ViewProperty.SCALE_X, 1.0d).add(ViewProperty.SCALE_Y, 1.0d);
    }

    private void setAutoTranslation() {
        this.isSetAutoTranslation = true;
        this.mTranslationSetMap.put(IHoverStyle.HoverType.ENTER, true);
        this.mTranslationSetMap.put(IHoverStyle.HoverType.EXIT, true);
        this.mState.getState(IHoverStyle.HoverType.EXIT).add(ViewProperty.TRANSLATION_X, 0.0d).add(ViewProperty.TRANSLATION_Y, 0.0d);
    }

    private void setAutoRound() {
    }

    private void setTintColor() {
        if (this.mSetTint || this.mClearTint) {
            return;
        }
        int tintColor = Color.argb(20, 0, 0, 0);
        Object target = this.mState.getTarget().getTargetObject();
        if (target instanceof View) {
            View view = (View) target;
            int colorRes = R.color.miuix_folme_color_touch_tint;
            UiModeManager mgr = (UiModeManager) view.getContext().getSystemService("uimode");
            if (mgr != null && mgr.getNightMode() == 2) {
                colorRes = R.color.miuix_folme_color_touch_tint_dark;
            }
            tintColor = view.getResources().getColor(colorRes);
        }
        FloatProperty propFg = ViewPropertyExt.FOREGROUND;
        this.mState.getState(IHoverStyle.HoverType.ENTER).add(propFg, tintColor);
        this.mState.getState(IHoverStyle.HoverType.EXIT).add(propFg, 0.0d);
    }

    @Override // miui.android.animation.IHoverStyle
    public IHoverStyle setParentView(View parent) {
        WeakReference<View> weakReference = this.mParentView;
        View parentView = weakReference != null ? weakReference.get() : null;
        if (parent == parentView) {
            return this;
        }
        this.mParentView = new WeakReference<>(parent);
        return this;
    }

    @Override // miui.android.animation.IHoverStyle
    public IHoverStyle setAlpha(float alpha, IHoverStyle.HoverType... type) {
        this.mState.getState(getType(type)).add(ViewProperty.ALPHA, alpha);
        return this;
    }

    @Override // miui.android.animation.IHoverStyle
    public IHoverStyle setScale(float scale, IHoverStyle.HoverType... type) {
        IHoverStyle.HoverType relType = getType(type);
        this.mScaleSetMap.put(relType, true);
        this.mState.getState(relType).add(ViewProperty.SCALE_X, scale).add(ViewProperty.SCALE_Y, scale);
        return this;
    }

    @Override // miui.android.animation.IHoverStyle
    public IHoverStyle setTranslate(float translate, IHoverStyle.HoverType... type) {
        this.isSetAutoTranslation = false;
        IHoverStyle.HoverType relType = getType(type);
        this.mTranslationSetMap.put(relType, true);
        this.mState.getState(relType).add(ViewProperty.TRANSLATION_X, translate).add(ViewProperty.TRANSLATION_Y, translate);
        return this;
    }

    @Override // miui.android.animation.IHoverStyle
    public IHoverStyle setCorner(float radius) {
        this.mRadius = radius;
        Object target = this.mState.getTarget().getTargetObject();
        if (target instanceof View) {
            ((View) target).setTag(KeyUtils.KEY_FOLME_SET_HOVER, Float.valueOf(radius));
        }
        return this;
    }

    @Override // miui.android.animation.IHoverStyle
    public IHoverStyle setTint(int color) {
        boolean z = true;
        this.mSetTint = true;
        if (color != 0) {
            z = false;
        }
        this.mClearTint = z;
        this.mState.getState(IHoverStyle.HoverType.ENTER).add(ViewPropertyExt.FOREGROUND, color);
        return this;
    }

    @Override // miui.android.animation.IHoverStyle
    public IHoverStyle setTint(float a, float r, float g, float b) {
        return setTint(Color.argb((int) (a * 255.0f), (int) (r * 255.0f), (int) (g * 255.0f), (int) (255.0f * b)));
    }

    @Override // miui.android.animation.IHoverStyle
    public IHoverStyle setBackgroundColor(float a, float r, float g, float b) {
        return setBackgroundColor(Color.argb((int) (a * 255.0f), (int) (r * 255.0f), (int) (g * 255.0f), (int) (255.0f * b)));
    }

    @Override // miui.android.animation.IHoverStyle
    public IHoverStyle setBackgroundColor(int color) {
        FloatProperty propBg = ViewPropertyExt.BACKGROUND;
        this.mState.getState(IHoverStyle.HoverType.ENTER).add(propBg, color);
        this.mState.getState(IHoverStyle.HoverType.EXIT).add(propBg, (int) AnimValueUtils.getValueOfTarget(this.mState.getTarget(), propBg, 0.0d));
        return this;
    }

    private void clearScale() {
        if (isScaleSet(IHoverStyle.HoverType.ENTER)) {
            this.mState.getState(IHoverStyle.HoverType.ENTER).remove(ViewProperty.SCALE_X);
            this.mState.getState(IHoverStyle.HoverType.ENTER).remove(ViewProperty.SCALE_Y);
        }
        if (isScaleSet(IHoverStyle.HoverType.EXIT)) {
            this.mState.getState(IHoverStyle.HoverType.EXIT).remove(ViewProperty.SCALE_X);
            this.mState.getState(IHoverStyle.HoverType.EXIT).remove(ViewProperty.SCALE_Y);
        }
        this.mScaleSetMap.clear();
    }

    private void clearTranslation() {
        this.isSetAutoTranslation = false;
        if (isTranslationSet(IHoverStyle.HoverType.ENTER)) {
            this.mState.getState(IHoverStyle.HoverType.ENTER).remove(ViewProperty.TRANSLATION_X);
            this.mState.getState(IHoverStyle.HoverType.ENTER).remove(ViewProperty.TRANSLATION_Y);
        }
        if (isTranslationSet(IHoverStyle.HoverType.EXIT)) {
            this.mState.getState(IHoverStyle.HoverType.EXIT).remove(ViewProperty.TRANSLATION_X);
            this.mState.getState(IHoverStyle.HoverType.EXIT).remove(ViewProperty.TRANSLATION_Y);
        }
        this.mTranslationSetMap.clear();
    }

    private void clearRound() {
    }

    @Override // miui.android.animation.IHoverStyle
    public IHoverStyle clearTintColor() {
        this.mClearTint = true;
        FloatProperty propFg = ViewPropertyExt.FOREGROUND;
        this.mState.getState(IHoverStyle.HoverType.ENTER).remove(propFg);
        this.mState.getState(IHoverStyle.HoverType.EXIT).remove(propFg);
        return this;
    }

    @Override // miui.android.animation.IHoverStyle
    public IHoverStyle setTintMode(int mode) {
        this.mEnterConfig.setTintMode(mode);
        this.mExitConfig.setTintMode(mode);
        return this;
    }

    @Override // miui.android.animation.IHoverStyle
    public IHoverStyle setEffect(IHoverStyle.HoverEffect effect) {
        updateHoverState(effect);
        return this;
    }

    @Override // miui.android.animation.IHoverStyle
    public void handleHoverOf(View view, AnimConfig... config) {
        doHandleHoverOf(view, config);
    }

    private void doHandleHoverOf(View view, AnimConfig... config) {
        handleViewHover(view, config);
        if (setHoverView(view) && LogUtils.isLogEnabled()) {
            LogUtils.debug("handleViewHover for " + view, new Object[0]);
        }
    }

    /* renamed from: miui.android.animation.controller.FolmeHover$2 */
    /* loaded from: classes.dex */
    public static /* synthetic */ class AnonymousClass2 {
        static final /* synthetic */ int[] $SwitchMap$miui$android$animation$IHoverStyle$HoverEffect;

        static {
            int[] iArr = new int[IHoverStyle.HoverEffect.values().length];
            $SwitchMap$miui$android$animation$IHoverStyle$HoverEffect = iArr;
            try {
                iArr[IHoverStyle.HoverEffect.NORMAL.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$miui$android$animation$IHoverStyle$HoverEffect[IHoverStyle.HoverEffect.FLOATED.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$miui$android$animation$IHoverStyle$HoverEffect[IHoverStyle.HoverEffect.FLOATED_WRAPPED.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
        }
    }

    private void updateHoverState(IHoverStyle.HoverEffect effect) {
        switch (AnonymousClass2.$SwitchMap$miui$android$animation$IHoverStyle$HoverEffect[effect.ordinal()]) {
            case 1:
                if (this.mCurrentEffect == IHoverStyle.HoverEffect.FLOATED) {
                    clearScale();
                    clearTranslation();
                } else if (this.mCurrentEffect == IHoverStyle.HoverEffect.FLOATED_WRAPPED) {
                    clearScale();
                    clearTranslation();
                    clearRound();
                }
                setTintColor();
                this.mCurrentEffect = effect;
                return;
            case 2:
                if (this.mCurrentEffect == IHoverStyle.HoverEffect.FLOATED_WRAPPED) {
                    clearRound();
                }
                setTintColor();
                setAutoScale();
                setAutoTranslation();
                this.mCurrentEffect = effect;
                return;
            case 3:
                if (this.mCurrentEffect == IHoverStyle.HoverEffect.NORMAL || this.mCurrentEffect == IHoverStyle.HoverEffect.FLOATED) {
                    clearTintColor();
                }
                setAutoScale();
                setAutoTranslation();
                setAutoRound();
                this.mCurrentEffect = effect;
                return;
            default:
                return;
        }
    }

    private boolean setHoverView(View view) {
        WeakReference<View> weakReference = this.mHoverView;
        View hoverView = weakReference != null ? weakReference.get() : null;
        if (hoverView == view) {
            return false;
        }
        this.mHoverView = new WeakReference<>(view);
        return true;
    }

    private void handleViewHover(View view, AnimConfig... config) {
        InnerViewHoverListener hoverListener = sHoverRecord.get(view);
        if (hoverListener == null) {
            hoverListener = new InnerViewHoverListener();
            sHoverRecord.put(view, hoverListener);
        }
        view.setOnHoverListener(hoverListener);
        hoverListener.addHover(this, config);
    }

    /* loaded from: classes.dex */
    public static class InnerViewHoverListener implements View.OnHoverListener {
        private WeakHashMap<FolmeHover, AnimConfig[]> mHoverMap;

        private InnerViewHoverListener() {
            this.mHoverMap = new WeakHashMap<>();
        }

        void addHover(FolmeHover folmeHover, AnimConfig... configs) {
            this.mHoverMap.put(folmeHover, configs);
        }

        boolean removeHover(FolmeHover folmeHover) {
            this.mHoverMap.remove(folmeHover);
            return this.mHoverMap.isEmpty();
        }

        @Override // android.view.View.OnHoverListener
        public boolean onHover(View view, MotionEvent event) {
            for (Map.Entry<FolmeHover, AnimConfig[]> entry : this.mHoverMap.entrySet()) {
                FolmeHover folmeHover = entry.getKey();
                AnimConfig[] configs = entry.getValue();
                folmeHover.handleMotionEvent(view, event, configs);
            }
            return false;
        }
    }

    public void handleMotionEvent(View view, MotionEvent event, AnimConfig... config) {
        switch (event.getActionMasked()) {
            case 7:
                onEventMove(view, event, config);
                return;
            case 8:
            default:
                return;
            case 9:
                onEventEnter(event, config);
                return;
            case 10:
                onEventExit(event, config);
                return;
        }
    }

    private void onEventEnter(MotionEvent event, AnimConfig... config) {
        if (LogUtils.isLogEnabled()) {
            LogUtils.debug("onEventEnter, touchEnter", new Object[0]);
        }
        hoverEnter(config);
    }

    private void onEventMove(View view, MotionEvent event, AnimConfig... config) {
        if (this.mIsEnter && view != null && isTranslationSet(IHoverStyle.HoverType.ENTER) && this.isSetAutoTranslation) {
            actualTranslatDist(view, event);
        }
    }

    private void onEventExit(MotionEvent event, AnimConfig... config) {
        if (this.mIsEnter) {
            if (LogUtils.isLogEnabled()) {
                LogUtils.debug("onEventExit, touchExit", new Object[0]);
            }
            hoverExit(event, config);
            resetTouchStatus();
        }
    }

    private void resetTouchStatus() {
        this.mIsEnter = false;
    }

    static boolean isOnHoverView(View view, int[] location, MotionEvent event) {
        if (view == null) {
            return true;
        }
        view.getLocationOnScreen(location);
        int x = (int) event.getRawX();
        int y = (int) event.getRawY();
        if (x >= location[0] && x <= location[0] + view.getWidth() && y >= location[1] && y <= location[1] + view.getHeight()) {
            return true;
        }
        return false;
    }

    @Override // miui.android.animation.IHoverStyle
    public void ignoreHoverOf(View view) {
        InnerViewHoverListener hoverListener = sHoverRecord.get(view);
        if (hoverListener != null && hoverListener.removeHover(this)) {
            sHoverRecord.remove(view);
        }
    }

    @Override // miui.android.animation.IHoverStyle
    public void onMotionEventEx(View view, MotionEvent event, AnimConfig... config) {
        handleMotionEvent(view, event, config);
    }

    @Override // miui.android.animation.IHoverStyle
    public void onMotionEvent(MotionEvent event) {
        handleMotionEvent(null, event, new AnimConfig[0]);
    }

    @Override // miui.android.animation.IHoverStyle
    public void hoverEnter(AnimConfig... config) {
        this.mIsEnter = true;
        if (this.mCurrentEffect == IHoverStyle.HoverEffect.FLOATED_WRAPPED) {
            WeakReference<View> weakReference = this.mHoverView;
            View hoverView = weakReference != null ? weakReference.get() : null;
            if (hoverView != null) {
                this.mViewRect = new Rect(hoverView.getLeft(), hoverView.getTop(), hoverView.getRight(), hoverView.getBottom());
                setMagicView(hoverView, true);
                setWrapped(hoverView, true);
                isHideHover();
            }
        }
        setCorner(this.mRadius);
        setTintColor();
        AnimConfig[] configArray = getEnterConfig(config);
        AnimState state = this.mState.getState(IHoverStyle.HoverType.ENTER);
        if (isScaleSet(IHoverStyle.HoverType.ENTER)) {
            IAnimTarget target = this.mState.getTarget();
            float maxSize = Math.max(target.getValue(ViewProperty.WIDTH), target.getValue(ViewProperty.HEIGHT));
            float scaleValue = Math.min((12.0f + maxSize) / maxSize, (float) DEFAULT_SCALE);
            state.add(ViewProperty.SCALE_X, scaleValue).add(ViewProperty.SCALE_Y, scaleValue);
        }
        WeakReference<View> weakReference2 = this.mParentView;
        if (weakReference2 != null) {
            Folme.useAt(weakReference2.get()).state().add((FloatProperty) ViewProperty.SCALE_X, 1.0f).add((FloatProperty) ViewProperty.SCALE_Y, 1.0f).to(configArray);
        }
        this.mState.to(state, configArray);
    }

    @Override // miui.android.animation.IHoverStyle
    public void hoverMove(View view, MotionEvent event, AnimConfig... config) {
        onEventMove(view, event, config);
    }

    private void hoverExit(MotionEvent event, AnimConfig... config) {
        if (this.mParentView != null && !isOnHoverView(this.mHoverView.get(), this.mLocation, event)) {
            Folme.useAt(this.mParentView.get()).hover().hoverEnter(new AnimConfig[0]);
        }
        if (isTranslationSet(IHoverStyle.HoverType.EXIT) && this.isSetAutoTranslation) {
            this.mState.getState(IHoverStyle.HoverType.EXIT).add(ViewProperty.TRANSLATION_X, 0.0d).add(ViewProperty.TRANSLATION_Y, 0.0d);
        }
        hoverExit(config);
    }

    @Override // miui.android.animation.IHoverStyle
    public void hoverExit(AnimConfig... config) {
        AnimConfig[] configArray = getExitConfig(config);
        this.mState.to(this.mState.getState(IHoverStyle.HoverType.EXIT), configArray);
    }

    @Override // miui.android.animation.IHoverStyle
    public void setHoverEnter() {
        setTintColor();
        this.mState.setTo(IHoverStyle.HoverType.ENTER);
    }

    @Override // miui.android.animation.IHoverStyle
    public void setHoverExit() {
        this.mState.setTo(IHoverStyle.HoverType.EXIT);
    }

    @Override // miui.android.animation.IHoverStyle
    public boolean isMagicView() {
        Object target = this.mState.getTarget().getTargetObject();
        if (target instanceof View) {
            return isMagicView((View) target);
        }
        return false;
    }

    @Override // miui.android.animation.IHoverStyle
    public void setMagicView(boolean isMagicView) {
        Object target = this.mState.getTarget().getTargetObject();
        if (target instanceof View) {
            setMagicView((View) target, isMagicView);
        }
    }

    @Override // miui.android.animation.IHoverStyle
    public void setWrapped(boolean isWrapped) {
        Object target = this.mState.getTarget().getTargetObject();
        if (target instanceof View) {
            setWrapped((View) target, isWrapped);
        }
    }

    @Override // miui.android.animation.IHoverStyle
    public void setPointerHide(boolean isWrapped) {
        Object target = this.mState.getTarget().getTargetObject();
        if (target instanceof View) {
            setPointerHide((View) target, isWrapped);
        }
    }

    @Override // miui.android.animation.IHoverStyle
    public void setPointerShape(Bitmap pointerShape) {
        Object target = this.mState.getTarget().getTargetObject();
        if (target instanceof View) {
            setPointerShape((View) target, pointerShape);
        }
    }

    @Override // miui.android.animation.IHoverStyle
    public void setPointerShapeType(int pointerShapeType) {
        Object target = this.mState.getTarget().getTargetObject();
        if (target instanceof View) {
            setPointerShapeType((View) target, pointerShapeType);
        }
    }

    @Override // miui.android.animation.IHoverStyle
    public void addMagicPoint(Point magicPoint) {
        Object target = this.mState.getTarget().getTargetObject();
        if (target instanceof View) {
            addMagicPoint((View) target, magicPoint);
        }
    }

    @Override // miui.android.animation.IHoverStyle
    public void clearMagicPoint() {
        Object target = this.mState.getTarget().getTargetObject();
        if (target instanceof View) {
            clearMagicPoint((View) target);
        }
    }

    private boolean isScaleSet(IHoverStyle.HoverType type) {
        return Boolean.TRUE.equals(this.mScaleSetMap.get(type));
    }

    private boolean isTranslationSet(IHoverStyle.HoverType type) {
        return Boolean.TRUE.equals(this.mTranslationSetMap.get(type));
    }

    private IHoverStyle.HoverType getType(IHoverStyle.HoverType... type) {
        return type.length > 0 ? type[0] : IHoverStyle.HoverType.ENTER;
    }

    @Override // miui.android.animation.controller.FolmeBase, miui.android.animation.IStateContainer
    public void clean() {
        super.clean();
        this.mScaleSetMap.clear();
        WeakReference<View> weakReference = this.mHoverView;
        if (weakReference != null) {
            resetView(weakReference);
            this.mHoverView = null;
        }
        WeakReference<View> weakReference2 = this.mChildView;
        if (weakReference2 != null) {
            resetView(weakReference2);
            this.mChildView = null;
        }
        WeakReference<View> weakReference3 = this.mParentView;
        if (weakReference3 != null) {
            resetView(weakReference3);
            this.mParentView = null;
        }
    }

    private float perFromVal(float val, float from, float to) {
        return (val - from) / (to - from);
    }

    private float valFromPer(float per, float from, float to) {
        return ((to - from) * per) + from;
    }

    private void initDist(IAnimTarget target) {
        View view = target instanceof ViewTarget ? ((ViewTarget) target).getTargetObject() : null;
        if (view != null) {
            float maxSize = Math.max(target.getValue(ViewProperty.WIDTH), target.getValue(ViewProperty.HEIGHT));
            float scaleValue = Math.min((12.0f + maxSize) / maxSize, (float) DEFAULT_SCALE);
            this.mTargetWidth = view.getWidth();
            int height = view.getHeight();
            this.mTargetHeight = height;
            int gravityW = this.mTargetWidth - (20 * 2);
            int gravityH = height - (20 * 2);
            float f = MiuiFreeformPinManagerService.EDGE_AREA;
            float wPer = perFromVal(gravityW, MiuiFreeformPinManagerService.EDGE_AREA, 360.0f);
            float ox = valFromPer(Math.max((float) MiuiFreeformPinManagerService.EDGE_AREA, Math.min(1.0f, wPer)), 15.0f, MiuiFreeformPinManagerService.EDGE_AREA);
            float ox2 = Math.min(15.0f, ox);
            float hPer = perFromVal(gravityH, MiuiFreeformPinManagerService.EDGE_AREA, 360.0f);
            float oy = valFromPer(Math.max((float) MiuiFreeformPinManagerService.EDGE_AREA, Math.min(1.0f, hPer)), 15.0f, MiuiFreeformPinManagerService.EDGE_AREA);
            float oy2 = Math.min(15.0f, oy);
            if (scaleValue != 1.0f) {
                f = Math.min(ox2, oy2);
            }
            this.mTranslateDist = f;
            int i = this.mTargetWidth;
            int i2 = this.mTargetHeight;
            if (i == i2 && i < 100 && i2 < 100) {
                setCorner((int) (i * 0.5f));
            } else {
                setCorner(36.0f);
            }
        }
    }

    private void actualTranslatDist(View view, MotionEvent event) {
        float curx = event.getRawX();
        float cury = event.getRawY();
        view.getLocationOnScreen(this.mLocation);
        float curViewCentreX = this.mLocation[0] + (view.getWidth() * 0.5f);
        float curViewCentreY = this.mLocation[1] + (view.getHeight() * 0.5f);
        float ox = (curx - curViewCentreX) / view.getWidth();
        float oy = (cury - curViewCentreY) / view.getHeight();
        float f = 1.0f;
        float ox2 = Math.max(-1.0f, Math.min(1.0f, ox));
        float oy2 = Math.max(-1.0f, Math.min(1.0f, oy));
        float f2 = this.mTranslateDist;
        float ox3 = ox2 * (f2 == Float.MAX_VALUE ? 1.0f : f2);
        if (f2 != Float.MAX_VALUE) {
            f = f2;
        }
        AnimState state = this.mState.getState(this.HoverMoveType).add(ViewProperty.TRANSLATION_X, ox3).add(ViewProperty.TRANSLATION_Y, oy2 * f);
        this.mState.to(state, this.mMoveConfig);
    }

    private View resetView(WeakReference<View> viewHolder) {
        View view = viewHolder.get();
        if (view != null) {
            view.setOnHoverListener(null);
        }
        return view;
    }

    private AnimConfig[] getEnterConfig(AnimConfig... config) {
        return (AnimConfig[]) CommonUtils.mergeArray(config, this.mEnterConfig);
    }

    private AnimConfig[] getExitConfig(AnimConfig... config) {
        return (AnimConfig[]) CommonUtils.mergeArray(config, this.mExitConfig);
    }

    public boolean isHideHover() {
        boolean z;
        return this.mTargetWidth < 100 && this.mTargetHeight < 100 && (!(z = this.isSetAutoTranslation) || (z && (this.mCurrentEffect == IHoverStyle.HoverEffect.FLOATED || this.mCurrentEffect == IHoverStyle.HoverEffect.FLOATED_WRAPPED)));
    }

    private static boolean isMagicView(View view) {
        try {
            Class<?> forName = Class.forName("android.view.View");
            Method sGet = forName.getMethod("isMagicView", new Class[0]);
            return ((Boolean) sGet.invoke(view, new Object[0])).booleanValue();
        } catch (Exception e) {
            Log.e("", "isMagicView failed , e:" + e.toString());
            return false;
        }
    }

    private static void addMagicRect(View view, Rect rect) {
        try {
            Class<?>[] parameterTypes = {Rect.class};
            Class<?> forName = Class.forName("android.view.View");
            Method sGet = forName.getMethod("addMagicRect", parameterTypes);
            sGet.invoke(view, rect);
        } catch (Exception e) {
            Log.e("", "addMagicRect failed , e:" + e.toString());
        }
    }

    private static void setMagicView(View view, boolean isMagicView) {
        try {
            Class<?>[] parameterTypes = {Boolean.TYPE};
            Class<?> forName = Class.forName("android.view.View");
            Method sGet = forName.getMethod("setMagicView", parameterTypes);
            sGet.invoke(view, Boolean.valueOf(isMagicView));
        } catch (Exception e) {
            Log.e("", "setMagicView failed , e:" + e.toString());
        }
    }

    private static void setWrapped(View view, boolean isWrapped) {
        try {
            Class<?>[] parameterTypes = {Boolean.TYPE};
            Class<?> forName = Class.forName("android.view.View");
            Method sGet = forName.getMethod("setWrapped", parameterTypes);
            sGet.invoke(view, Boolean.valueOf(isWrapped));
        } catch (Exception e) {
            Log.e("", "setWrapped failed , e:" + e.toString());
        }
    }

    private static void setPointerHide(View view, boolean isWrapped) {
        try {
            Class<?>[] parameterTypes = {Boolean.TYPE};
            Class<?> forName = Class.forName("android.view.View");
            Method sGet = forName.getMethod("setPointerHide", parameterTypes);
            sGet.invoke(view, Boolean.valueOf(isWrapped));
        } catch (Exception e) {
            Log.e("", "setPointerHide failed , e:" + e.toString());
        }
    }

    private static void setPointerShape(View view, Bitmap pointerShape) {
        try {
            Class<?>[] parameterTypes = {Bitmap.class};
            Class<?> forName = Class.forName("android.view.View");
            Method sGet = forName.getMethod("setPointerShape", parameterTypes);
            sGet.invoke(view, pointerShape);
        } catch (Exception e) {
            Log.e("", "setPointerShape failed , e:" + e.toString());
        }
    }

    private static void setPointerShapeType(View view, int pointerShapeType) {
        try {
            Class<?>[] parameterTypes = {Integer.TYPE};
            Class<?> forName = Class.forName("android.view.View");
            Method sGet = forName.getMethod("setPointerShapeType", parameterTypes);
            sGet.invoke(view, Integer.valueOf(pointerShapeType));
        } catch (Exception e) {
            Log.e("", "setPointerShapeType failed , e:" + e.toString());
        }
    }

    private static void addMagicPoint(View view, Point magicPoint) {
        try {
            Class<?>[] parameterTypes = {Point.class};
            Class<?> forName = Class.forName("android.view.View");
            Method sGet = forName.getMethod("addMagicPoint", parameterTypes);
            sGet.invoke(view, magicPoint);
        } catch (Exception e) {
            Log.e("", "addMagicPoint failed , e:" + e.toString());
        }
    }

    private static void clearMagicPoint(View view) {
        try {
            Class<?> forName = Class.forName("android.view.View");
            Method sGet = forName.getMethod("clearMagicPoint", new Class[0]);
            sGet.invoke(view, new Object[0]);
        } catch (Exception e) {
            Log.e("", "clearMagicPoint failed , e:" + e.toString());
        }
    }
}
