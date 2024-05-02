package miui.android.animation.controller;

import android.app.UiModeManager;
import android.graphics.Color;
import android.util.ArrayMap;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewParent;
import android.widget.AbsListView;
import android.widget.TextView;
import com.android.server.wm.MiuiFreeformPinManagerService;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Map;
import java.util.WeakHashMap;
import miui.android.animation.IAnimTarget;
import miui.android.animation.ITouchStyle;
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
import miui.android.animation.utils.FolmeResColors;
import miui.android.animation.utils.KeyUtils;
import miui.android.animation.utils.LogUtils;
/* loaded from: classes.dex */
public class FolmeTouch extends FolmeBase implements ITouchStyle {
    private static final float DEFAULT_SCALE = 0.9f;
    private static final int SCALE_DIS = 10;
    private static WeakHashMap<View, InnerViewTouchListener> sTouchRecord = new WeakHashMap<>();
    private boolean mClickInvoked;
    private View.OnClickListener mClickListener;
    private int mDownWeight;
    private float mDownX;
    private float mDownY;
    private FolmeFont mFontStyle;
    private boolean mIsDown;
    private WeakReference<View> mListView;
    private boolean mLongClickInvoked;
    private View.OnLongClickListener mLongClickListener;
    private LongClickTask mLongClickTask;
    private float mScaleDist;
    private boolean mSetTint;
    private int mTouchIndex;
    private WeakReference<View> mTouchView;
    private int mUpWeight;
    private int[] mLocation = new int[2];
    private Map<ITouchStyle.TouchType, Boolean> mScaleSetMap = new ArrayMap();
    private AnimConfig mDownConfig = new AnimConfig();
    private AnimConfig mUpConfig = new AnimConfig();
    private boolean mClearTint = false;
    private TransitionListener mDefListener = new TransitionListener() { // from class: miui.android.animation.controller.FolmeTouch.1
        @Override // miui.android.animation.listener.TransitionListener
        public void onBegin(Object toTag, Collection<UpdateInfo> updateList) {
            if (toTag.equals(ITouchStyle.TouchType.DOWN)) {
                AnimState.alignState(FolmeTouch.this.mState.getState(ITouchStyle.TouchType.UP), updateList);
            }
        }
    };

    public FolmeTouch(IAnimTarget... targets) {
        super(targets);
        initScaleDist(targets.length > 0 ? targets[0] : null);
        FloatProperty propScaleX = ViewProperty.SCALE_X;
        FloatProperty propScaleY = ViewProperty.SCALE_Y;
        this.mState.getState(ITouchStyle.TouchType.UP).add(propScaleX, 1.0d).add(propScaleY, 1.0d);
        setTintColor();
        this.mDownConfig.setEase(EaseManager.getStyle(-2, 0.99f, 0.15f));
        this.mDownConfig.addListeners(this.mDefListener);
        this.mUpConfig.setEase(-2, 0.99f, 0.3f).setSpecial(ViewProperty.ALPHA, -2L, 0.9f, 0.2f);
    }

    private void setTintColor() {
        if (this.mSetTint || this.mClearTint) {
            return;
        }
        int tintColor = Color.argb(20, 0, 0, 0);
        Object target = this.mState.getTarget().getTargetObject();
        if (target instanceof View) {
            View view = (View) target;
            int colorRes = FolmeResColors.MIUIX_FOLME_COLOR_TOUCH_TINT;
            UiModeManager mgr = (UiModeManager) view.getContext().getSystemService("uimode");
            if (mgr != null && mgr.getNightMode() == 2) {
                colorRes = FolmeResColors.MIUIX_FOLME_COLOR_TOUCH_TINT_DARK;
            }
            tintColor = view.getResources().getColor(colorRes);
        }
        FloatProperty propFg = ViewPropertyExt.FOREGROUND;
        this.mState.getState(ITouchStyle.TouchType.DOWN).add(propFg, tintColor);
        this.mState.getState(ITouchStyle.TouchType.UP).add(propFg, 0.0d);
    }

    private void initScaleDist(IAnimTarget target) {
        View view = target instanceof ViewTarget ? ((ViewTarget) target).getTargetObject() : null;
        if (view != null) {
            this.mScaleDist = TypedValue.applyDimension(1, 10.0f, view.getResources().getDisplayMetrics());
        }
    }

    @Override // miui.android.animation.controller.FolmeBase, miui.android.animation.IStateContainer
    public void clean() {
        super.clean();
        FolmeFont folmeFont = this.mFontStyle;
        if (folmeFont != null) {
            folmeFont.clean();
        }
        this.mScaleSetMap.clear();
        WeakReference<View> weakReference = this.mTouchView;
        if (weakReference != null) {
            resetView(weakReference);
            this.mTouchView = null;
        }
        WeakReference<View> weakReference2 = this.mListView;
        if (weakReference2 != null) {
            View listView = resetView(weakReference2);
            if (listView != null) {
                listView.setTag(KeyUtils.KEY_FOLME_LISTVIEW_TOUCH_LISTENER, null);
            }
            this.mListView = null;
        }
        resetTouchStatus();
    }

    private View resetView(WeakReference<View> viewHolder) {
        View view = viewHolder.get();
        if (view != null) {
            view.setOnTouchListener(null);
        }
        return view;
    }

    public void setFontStyle(FolmeFont style) {
        this.mFontStyle = style;
    }

    @Override // miui.android.animation.ITouchStyle
    public ITouchStyle setTintMode(int mode) {
        this.mDownConfig.setTintMode(mode);
        this.mUpConfig.setTintMode(mode);
        return this;
    }

    static boolean isOnTouchView(View view, int[] location, MotionEvent event) {
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

    private boolean setTouchView(View view) {
        WeakReference<View> weakReference = this.mTouchView;
        View touchView = weakReference != null ? weakReference.get() : null;
        if (touchView == view) {
            return false;
        }
        this.mTouchView = new WeakReference<>(view);
        return true;
    }

    @Override // miui.android.animation.ITouchStyle
    public void bindViewOfListItem(final View view, final AnimConfig... config) {
        if (!setTouchView(view)) {
            return;
        }
        CommonUtils.runOnPreDraw(view, new Runnable() { // from class: miui.android.animation.controller.FolmeTouch.2
            @Override // java.lang.Runnable
            public void run() {
                FolmeTouch.this.bindListView(view, false, config);
            }
        });
    }

    @Override // miui.android.animation.ITouchStyle
    public void ignoreTouchOf(View view) {
        InnerViewTouchListener touchListener = sTouchRecord.get(view);
        if (touchListener != null && touchListener.removeTouch(this)) {
            sTouchRecord.remove(view);
        }
    }

    @Override // miui.android.animation.ITouchStyle
    public void handleTouchOf(View view, AnimConfig... config) {
        handleTouchOf(view, false, config);
    }

    @Override // miui.android.animation.ITouchStyle
    public void handleTouchOf(View view, View.OnClickListener click, AnimConfig... config) {
        doHandleTouchOf(view, click, null, false, config);
    }

    @Override // miui.android.animation.ITouchStyle
    public void handleTouchOf(View view, View.OnClickListener click, View.OnLongClickListener longClick, AnimConfig... config) {
        doHandleTouchOf(view, click, longClick, false, config);
    }

    @Override // miui.android.animation.ITouchStyle
    public void handleTouchOf(View view, boolean clickListenerSet, AnimConfig... config) {
        doHandleTouchOf(view, null, null, clickListenerSet, config);
    }

    private void doHandleTouchOf(final View view, View.OnClickListener clickListener, View.OnLongClickListener longClick, final boolean clickListenerSet, final AnimConfig... config) {
        setClickAndLongClickListener(clickListener, longClick);
        handleViewTouch(view, config);
        if (setTouchView(view)) {
            if (LogUtils.isLogEnabled()) {
                LogUtils.debug("handleViewTouch for " + view, new Object[0]);
            }
            final boolean isClickable = view.isClickable();
            view.setClickable(true);
            Runnable task = new Runnable() { // from class: miui.android.animation.controller.FolmeTouch.3
                @Override // java.lang.Runnable
                public void run() {
                    if (!clickListenerSet && FolmeTouch.this.bindListView(view, true, config)) {
                        FolmeTouch.this.resetViewTouch(view, isClickable);
                    }
                }
            };
            CommonUtils.runOnPreDraw(view, task);
        }
    }

    private void setClickAndLongClickListener(View.OnClickListener click, View.OnLongClickListener longClick) {
        View targetView = null;
        IAnimTarget target = this.mState.getTarget();
        if (target instanceof ViewTarget) {
            targetView = ((ViewTarget) target).getTargetObject();
        }
        if (targetView == null) {
            return;
        }
        if (this.mClickListener != null && click == null) {
            targetView.setOnClickListener(null);
        } else if (click != null) {
            targetView.setOnClickListener(new View.OnClickListener() { // from class: miui.android.animation.controller.FolmeTouch.4
                @Override // android.view.View.OnClickListener
                public void onClick(View v) {
                    FolmeTouch.this.invokeClick(v);
                }
            });
        }
        this.mClickListener = click;
        if (this.mLongClickListener != null && longClick == null) {
            targetView.setOnLongClickListener(null);
        } else if (longClick != null) {
            targetView.setOnLongClickListener(new View.OnLongClickListener() { // from class: miui.android.animation.controller.FolmeTouch.5
                @Override // android.view.View.OnLongClickListener
                public boolean onLongClick(View v) {
                    if (!FolmeTouch.this.mLongClickInvoked) {
                        FolmeTouch.this.invokeLongClick(v);
                        return true;
                    }
                    return false;
                }
            });
        }
        this.mLongClickListener = longClick;
    }

    public boolean bindListView(View view, boolean setTouchListener, AnimConfig... config) {
        ListViewInfo info;
        if (this.mState.getTarget() == null || (info = getListViewInfo(view)) == null || info.listView == null) {
            return false;
        }
        if (LogUtils.isLogEnabled()) {
            LogUtils.debug("handleListViewTouch for " + view, new Object[0]);
        }
        handleListViewTouch(info.listView, view, setTouchListener, config);
        return true;
    }

    /* loaded from: classes.dex */
    public static class ListViewInfo {
        View itemView;
        AbsListView listView;

        private ListViewInfo() {
        }
    }

    private ListViewInfo getListViewInfo(View view) {
        ListViewInfo info = new ListViewInfo();
        AbsListView listView = null;
        View itemView = view;
        ViewParent parent = view.getParent();
        while (true) {
            if (parent == null) {
                break;
            } else if (parent instanceof AbsListView) {
                listView = (AbsListView) parent;
                break;
            } else {
                if (parent instanceof View) {
                    itemView = (View) parent;
                }
                parent = parent.getParent();
            }
        }
        if (listView != null) {
            this.mListView = new WeakReference<>(info.listView);
            info.listView = listView;
            info.itemView = itemView;
        }
        return info;
    }

    public static ListViewTouchListener getListViewTouchListener(AbsListView listView) {
        return (ListViewTouchListener) listView.getTag(KeyUtils.KEY_FOLME_LISTVIEW_TOUCH_LISTENER);
    }

    private void handleListViewTouch(AbsListView listView, View touchView, boolean setTouchListener, AnimConfig... config) {
        ListViewTouchListener listener = getListViewTouchListener(listView);
        if (listener == null) {
            listener = new ListViewTouchListener(listView);
            listView.setTag(KeyUtils.KEY_FOLME_LISTVIEW_TOUCH_LISTENER, listener);
        }
        if (setTouchListener) {
            listView.setOnTouchListener(listener);
        }
        listener.putListener(touchView, new InnerListViewTouchListener(this, config));
    }

    /* loaded from: classes.dex */
    public static class InnerListViewTouchListener implements View.OnTouchListener {
        private AnimConfig[] mConfigs;
        private WeakReference<FolmeTouch> mFolmeTouchRef;

        InnerListViewTouchListener(FolmeTouch folmeTouch, AnimConfig... configs) {
            this.mFolmeTouchRef = new WeakReference<>(folmeTouch);
            this.mConfigs = configs;
        }

        @Override // android.view.View.OnTouchListener
        public boolean onTouch(View v, MotionEvent event) {
            WeakReference<FolmeTouch> weakReference = this.mFolmeTouchRef;
            FolmeTouch folmeTouch = weakReference == null ? null : weakReference.get();
            if (folmeTouch != null) {
                if (event == null) {
                    folmeTouch.onEventUp(this.mConfigs);
                    return false;
                }
                folmeTouch.handleMotionEvent(v, event, this.mConfigs);
                return false;
            }
            return false;
        }
    }

    private void handleViewTouch(View view, AnimConfig... config) {
        InnerViewTouchListener touchListener = sTouchRecord.get(view);
        if (touchListener == null) {
            touchListener = new InnerViewTouchListener();
            sTouchRecord.put(view, touchListener);
        }
        view.setOnTouchListener(touchListener);
        touchListener.addTouch(this, config);
    }

    /* loaded from: classes.dex */
    public static class InnerViewTouchListener implements View.OnTouchListener {
        private WeakHashMap<FolmeTouch, AnimConfig[]> mTouchMap;

        private InnerViewTouchListener() {
            this.mTouchMap = new WeakHashMap<>();
        }

        void addTouch(FolmeTouch folmeTouch, AnimConfig... configs) {
            this.mTouchMap.put(folmeTouch, configs);
        }

        boolean removeTouch(FolmeTouch folmeTouch) {
            this.mTouchMap.remove(folmeTouch);
            return this.mTouchMap.isEmpty();
        }

        @Override // android.view.View.OnTouchListener
        public boolean onTouch(View view, MotionEvent event) {
            for (Map.Entry<FolmeTouch, AnimConfig[]> entry : this.mTouchMap.entrySet()) {
                FolmeTouch folmeTouch = entry.getKey();
                AnimConfig[] configs = entry.getValue();
                folmeTouch.handleMotionEvent(view, event, configs);
            }
            return false;
        }
    }

    public void resetViewTouch(View view, boolean isClickable) {
        view.setClickable(isClickable);
        view.setOnTouchListener(null);
    }

    @Override // miui.android.animation.ITouchStyle
    public void onMotionEventEx(View view, MotionEvent event, AnimConfig... config) {
        handleMotionEvent(view, event, config);
    }

    @Override // miui.android.animation.ITouchStyle
    public void onMotionEvent(MotionEvent event) {
        handleMotionEvent(null, event, new AnimConfig[0]);
    }

    private void onEventDown(AnimConfig... config) {
        if (LogUtils.isLogEnabled()) {
            LogUtils.debug("onEventDown, touchDown", new Object[0]);
        }
        this.mIsDown = true;
        touchDown(config);
    }

    private void onEventMove(MotionEvent event, View view, AnimConfig... config) {
        if (this.mIsDown) {
            if (!isOnTouchView(view, this.mLocation, event)) {
                touchUp(config);
                resetTouchStatus();
            } else if (this.mLongClickTask != null && !isInTouchSlop(view, event)) {
                this.mLongClickTask.stop(this);
            }
        }
    }

    public void onEventUp(AnimConfig... config) {
        if (this.mIsDown) {
            if (LogUtils.isLogEnabled()) {
                LogUtils.debug("onEventUp, touchUp", new Object[0]);
            }
            touchUp(config);
            resetTouchStatus();
        }
    }

    private void resetTouchStatus() {
        LongClickTask longClickTask = this.mLongClickTask;
        if (longClickTask != null) {
            longClickTask.stop(this);
        }
        this.mIsDown = false;
        this.mTouchIndex = 0;
        this.mDownX = MiuiFreeformPinManagerService.EDGE_AREA;
        this.mDownY = MiuiFreeformPinManagerService.EDGE_AREA;
    }

    private void recordDownEvent(MotionEvent event) {
        if (this.mClickListener != null || this.mLongClickListener != null) {
            this.mTouchIndex = event.getActionIndex();
            this.mDownX = event.getRawX();
            this.mDownY = event.getRawY();
            this.mClickInvoked = false;
            this.mLongClickInvoked = false;
            startLongClickTask();
        }
    }

    /* loaded from: classes.dex */
    public static final class LongClickTask implements Runnable {
        private WeakReference<FolmeTouch> mTouchRef;

        private LongClickTask() {
        }

        void start(FolmeTouch touch) {
            View targetView;
            IAnimTarget target = touch.mState.getTarget();
            if ((target instanceof ViewTarget) && (targetView = ((ViewTarget) target).getTargetObject()) != null) {
                this.mTouchRef = new WeakReference<>(touch);
                targetView.postDelayed(this, ViewConfiguration.getLongPressTimeout());
            }
        }

        void stop(FolmeTouch touch) {
            View targetView;
            IAnimTarget target = touch.mState.getTarget();
            if ((target instanceof ViewTarget) && (targetView = ((ViewTarget) target).getTargetObject()) != null) {
                targetView.removeCallbacks(this);
            }
        }

        @Override // java.lang.Runnable
        public void run() {
            View targetView;
            FolmeTouch touch = this.mTouchRef.get();
            if (touch != null) {
                IAnimTarget target = touch.mState.getTarget();
                if ((target instanceof ViewTarget) && (targetView = (View) target.getTargetObject()) != null && touch.mLongClickListener != null) {
                    targetView.performLongClick();
                    touch.invokeLongClick(targetView);
                }
            }
        }
    }

    private void startLongClickTask() {
        if (this.mLongClickListener == null) {
            return;
        }
        if (this.mLongClickTask == null) {
            this.mLongClickTask = new LongClickTask();
        }
        this.mLongClickTask.start(this);
    }

    public void invokeLongClick(View view) {
        if (!this.mLongClickInvoked) {
            this.mLongClickInvoked = true;
            this.mLongClickListener.onLongClick(view);
        }
    }

    private void handleClick(View view, MotionEvent event) {
        if (this.mIsDown && this.mClickListener != null && this.mTouchIndex == event.getActionIndex()) {
            IAnimTarget target = this.mState.getTarget();
            if ((target instanceof ViewTarget) && isInTouchSlop(view, event)) {
                View targetView = ((ViewTarget) target).getTargetObject();
                targetView.performClick();
                invokeClick(targetView);
            }
        }
    }

    public void invokeClick(View targetView) {
        if (!this.mClickInvoked && !this.mLongClickInvoked) {
            this.mClickInvoked = true;
            this.mClickListener.onClick(targetView);
        }
    }

    private boolean isInTouchSlop(View view, MotionEvent event) {
        float x = event.getRawX();
        float y = event.getRawY();
        return CommonUtils.getDistance(this.mDownX, this.mDownY, x, y) < ((double) CommonUtils.getTouchSlop(view));
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    public void handleMotionEvent(View view, MotionEvent event, AnimConfig... config) {
        switch (event.getActionMasked()) {
            case 0:
                recordDownEvent(event);
                onEventDown(config);
                return;
            case 1:
                handleClick(view, event);
                break;
            case 2:
                onEventMove(event, view, config);
                return;
        }
        onEventUp(config);
    }

    @Override // miui.android.animation.ITouchStyle
    public ITouchStyle useVarFont(TextView view, int fontType, int fromFontWeight, int toFontWeight) {
        FolmeFont folmeFont = this.mFontStyle;
        if (folmeFont != null) {
            this.mUpWeight = fromFontWeight;
            this.mDownWeight = toFontWeight;
            folmeFont.useAt(view, fontType, fromFontWeight);
        }
        return this;
    }

    @Override // miui.android.animation.ITouchStyle
    public ITouchStyle setAlpha(float alpha, ITouchStyle.TouchType... type) {
        this.mState.getState(getType(type)).add(ViewProperty.ALPHA, alpha);
        return this;
    }

    @Override // miui.android.animation.ITouchStyle
    public ITouchStyle setScale(float scale, ITouchStyle.TouchType... type) {
        ITouchStyle.TouchType relType = getType(type);
        this.mScaleSetMap.put(relType, true);
        this.mState.getState(relType).add(ViewProperty.SCALE_X, scale).add(ViewProperty.SCALE_Y, scale);
        return this;
    }

    private boolean isScaleSet(ITouchStyle.TouchType type) {
        return Boolean.TRUE.equals(this.mScaleSetMap.get(type));
    }

    private ITouchStyle.TouchType getType(ITouchStyle.TouchType... type) {
        return type.length > 0 ? type[0] : ITouchStyle.TouchType.DOWN;
    }

    @Override // miui.android.animation.ITouchStyle
    public ITouchStyle clearTintColor() {
        this.mClearTint = true;
        FloatProperty propFg = ViewPropertyExt.FOREGROUND;
        this.mState.getState(ITouchStyle.TouchType.DOWN).remove(propFg);
        this.mState.getState(ITouchStyle.TouchType.UP).remove(propFg);
        return this;
    }

    @Override // miui.android.animation.ITouchStyle
    public ITouchStyle setTint(int color) {
        boolean z = true;
        this.mSetTint = true;
        if (color != 0) {
            z = false;
        }
        this.mClearTint = z;
        this.mState.getState(ITouchStyle.TouchType.DOWN).add(ViewPropertyExt.FOREGROUND, color);
        return this;
    }

    @Override // miui.android.animation.ITouchStyle
    public ITouchStyle setTint(float a, float r, float g, float b) {
        return setTint(Color.argb((int) (a * 255.0f), (int) (r * 255.0f), (int) (g * 255.0f), (int) (255.0f * b)));
    }

    @Override // miui.android.animation.ITouchStyle
    public ITouchStyle setBackgroundColor(int color) {
        FloatProperty propBg = ViewPropertyExt.BACKGROUND;
        this.mState.getState(ITouchStyle.TouchType.DOWN).add(propBg, color);
        this.mState.getState(ITouchStyle.TouchType.UP).add(propBg, (int) AnimValueUtils.getValueOfTarget(this.mState.getTarget(), propBg, 0.0d));
        return this;
    }

    @Override // miui.android.animation.ITouchStyle
    public ITouchStyle setBackgroundColor(float a, float r, float g, float b) {
        return setBackgroundColor(Color.argb((int) (a * 255.0f), (int) (r * 255.0f), (int) (g * 255.0f), (int) (255.0f * b)));
    }

    @Override // miui.android.animation.ITouchStyle
    public void touchDown(AnimConfig... config) {
        setCorner(MiuiFreeformPinManagerService.EDGE_AREA);
        setTintColor();
        AnimConfig[] configArray = getDownConfig(config);
        FolmeFont folmeFont = this.mFontStyle;
        if (folmeFont != null) {
            folmeFont.to(this.mDownWeight, configArray);
        }
        AnimState state = this.mState.getState(ITouchStyle.TouchType.DOWN);
        if (!isScaleSet(ITouchStyle.TouchType.DOWN)) {
            IAnimTarget target = this.mState.getTarget();
            float maxSize = Math.max(target.getValue(ViewProperty.WIDTH), target.getValue(ViewProperty.HEIGHT));
            float scaleValue = Math.max((maxSize - this.mScaleDist) / maxSize, 0.9f);
            state.add(ViewProperty.SCALE_X, scaleValue).add(ViewProperty.SCALE_Y, scaleValue);
        }
        this.mState.to(state, configArray);
    }

    @Override // miui.android.animation.ITouchStyle
    public void touchUp(AnimConfig... config) {
        AnimConfig[] configArray = getUpConfig(config);
        FolmeFont folmeFont = this.mFontStyle;
        if (folmeFont != null) {
            folmeFont.to(this.mUpWeight, configArray);
        }
        this.mState.to(this.mState.getState(ITouchStyle.TouchType.UP), configArray);
    }

    @Override // miui.android.animation.controller.FolmeBase, miui.android.animation.ICancelableStyle
    public void cancel() {
        super.cancel();
        FolmeFont folmeFont = this.mFontStyle;
        if (folmeFont != null) {
            folmeFont.cancel();
        }
    }

    @Override // miui.android.animation.ITouchStyle
    public void setTouchDown() {
        setTintColor();
        this.mState.setTo(ITouchStyle.TouchType.DOWN);
    }

    @Override // miui.android.animation.ITouchStyle
    public void setTouchUp() {
        this.mState.setTo(ITouchStyle.TouchType.UP);
    }

    private AnimConfig[] getDownConfig(AnimConfig... config) {
        return (AnimConfig[]) CommonUtils.mergeArray(config, this.mDownConfig);
    }

    private AnimConfig[] getUpConfig(AnimConfig... config) {
        return (AnimConfig[]) CommonUtils.mergeArray(config, this.mUpConfig);
    }

    private void setCorner(float radius) {
        Object target = this.mState.getTarget().getTargetObject();
        if (target instanceof View) {
            ((View) target).setTag(KeyUtils.KEY_FOLME_SET_HOVER, Float.valueOf(radius));
        }
    }
}
