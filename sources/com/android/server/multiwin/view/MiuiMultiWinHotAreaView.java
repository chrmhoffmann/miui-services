package com.android.server.multiwin.view;

import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.util.AttributeSet;
import android.util.Slog;
import android.view.DragEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewRootImpl;
import android.widget.RelativeLayout;
import com.android.server.multiwin.animation.MiuiMultiWinSplitBarController;
import com.android.server.multiwin.animation.SplitAnimationAdapter;
import com.android.server.multiwin.listener.DragAnimationListener;
import com.android.server.multiwin.listener.MiuiMultiWinHotAreaConfigListener;
import com.android.server.wm.MiuiFreeformPinManagerService;
import com.android.server.wm.PhysicBasedInterpolator;
/* loaded from: classes.dex */
public class MiuiMultiWinHotAreaView extends MiuiMultiWinClipImageView implements ViewRootImpl.IDragEndInformation {
    public static final int BOTTOM_SPLIT = 4;
    public static final int FREE_FORM = 5;
    public static final int LEFT_SPLIT = 1;
    public static final int NONE_SPLIT = 0;
    public static final int RIGHT_SPLIT = 2;
    public static final boolean SHOW_SPLIT_BAR = false;
    private static final String TAG = "MiuiMultiWinHotAreaView";
    public static final int TOP_SPLIT = 3;
    public static final int TYPE_ANIM_DROP_DISAPPEAR = 6;
    public static final int TYPE_ANIM_DROP_SPLIT_SCREEN = 4;
    public static final int TYPE_ANIM_ENTER_SPLIT_ALPHA = 5;
    public static final int TYPE_ANIM_EXIT_SPLIT_ALPHA = 3;
    public static final int TYPE_ANIM_FREEFORM_DROP_FREEFORM = 2;
    public static final int TYPE_ANIM_SPLIT_DROP_SPLIT = 9;
    public static final int TYPE_ANIM_SPLIT_ENTER_FREEFORM = 7;
    public static final int TYPE_ANIM_SPLIT_EXIT_FREEFORM = 8;
    public static final int TYPE_ANIM_SPLIT_EXIT_FREEFORM_SCALED = 10;
    public static final boolean USE_BLUR = false;
    private ValueAnimator mDismissAnimator;
    DragAnimationListener mDragAnimationListener;
    private int mDragSplitMode;
    private boolean mHasPendingDropView;
    protected ViewGroup mHotAreaLayout;
    private MiuiMultiWinHotAreaPendingDropView mHotAreaPendingDropView;
    private int mInitialDragSplitMode;
    protected boolean mIsDropOnThisView;
    private boolean mIsLandScape;
    protected MiuiMultiWinHotAreaConfigListener mMiuiMultiWinHotAreaConfigListener;
    protected Rect mNavBarBound;
    protected int mNavBarPos;
    SplitAnimationAdapter mSplitAnimation;
    protected MiuiMultiWinSplitBarController mSplitBarController;
    protected int mSplitMode;

    public MiuiMultiWinHotAreaView(Context context) {
        this(context, null, 0);
    }

    public MiuiMultiWinHotAreaView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MiuiMultiWinHotAreaView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mHasPendingDropView = false;
        this.mSplitMode = 0;
        this.mDragSplitMode = 0;
        this.mInitialDragSplitMode = 0;
        this.mIsLandScape = false;
    }

    public void playDismissAnimation() {
        if (this.mDismissAnimator != null) {
            return;
        }
        ValueAnimator ofPropertyValuesHolder = ValueAnimator.ofPropertyValuesHolder(PropertyValuesHolder.ofFloat("alpha", 1.0f, MiuiFreeformPinManagerService.EDGE_AREA));
        this.mDismissAnimator = ofPropertyValuesHolder;
        ofPropertyValuesHolder.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.android.server.multiwin.view.MiuiMultiWinHotAreaView$$ExternalSyntheticLambda0
            @Override // android.animation.ValueAnimator.AnimatorUpdateListener
            public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                MiuiMultiWinHotAreaView.this.m1055x925491bb(valueAnimator);
            }
        });
        this.mDismissAnimator.setDuration(250L);
        this.mDismissAnimator.setInterpolator(new PhysicBasedInterpolator(0.9f, 0.8f));
        this.mDismissAnimator.start();
    }

    /* renamed from: lambda$playDismissAnimation$0$com-android-server-multiwin-view-MiuiMultiWinHotAreaView */
    public /* synthetic */ void m1055x925491bb(ValueAnimator param1ValueAnimator) {
        Object alphaObj = param1ValueAnimator.getAnimatedValue("alpha");
        float alpha = 1.0f;
        if (alphaObj instanceof Float) {
            alpha = ((Float) alphaObj).floatValue();
        }
        setAlpha(alpha);
    }

    public void setPendingDrpoViewFlag(boolean hasPendingDrpoView) {
        this.mHasPendingDropView = hasPendingDrpoView;
    }

    public int getDragSplitMode() {
        return this.mDragSplitMode;
    }

    public Rect getDropBound() {
        return new Rect(getLeft(), getTop(), getRight(), getBottom());
    }

    public int getInitialDragSplitMode() {
        return this.mInitialDragSplitMode;
    }

    public int getSplitMode() {
        return this.mSplitMode;
    }

    private void addHotAreaPendingDropView() {
        MiuiMultiWinHotAreaPendingDropView miuiMultiWinHotAreaPendingDropView = this.mHotAreaPendingDropView;
        if (miuiMultiWinHotAreaPendingDropView != null && miuiMultiWinHotAreaPendingDropView.getParent() != null) {
            return;
        }
        ViewParent dropViewParent = getParent();
        if (dropViewParent instanceof ViewGroup) {
            ViewGroup dropViewGroup = (ViewGroup) dropViewParent;
            Rect pushAcceptBound = new Rect(getLeft(), getTop(), getRight(), getBottom());
            pushAcceptBound.left += dropViewGroup.getLeft();
            pushAcceptBound.top += dropViewGroup.getTop();
            pushAcceptBound.right += dropViewGroup.getLeft();
            pushAcceptBound.bottom += dropViewGroup.getTop();
            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(-1, -1);
            MiuiMultiWinHotAreaPendingDropView miuiMultiWinHotAreaPendingDropView2 = new MiuiMultiWinHotAreaPendingDropView(getContext(), this.mSplitMode, pushAcceptBound);
            this.mHotAreaPendingDropView = miuiMultiWinHotAreaPendingDropView2;
            miuiMultiWinHotAreaPendingDropView2.setContentDescription("MiuiMultiWinHotAreaPendingDropView");
            this.mHotAreaPendingDropView.setLayoutParams(layoutParams);
            this.mHotAreaPendingDropView.setDragAnimationListener(this.mDragAnimationListener);
            dropViewGroup.addView(this.mHotAreaPendingDropView);
            this.mHotAreaPendingDropView.setHasRemovedSelf(false);
            return;
        }
        Slog.w(TAG, "addHotAreaPendingDropView failed cause parent view group is null!");
    }

    void handleDragEnded(DragEvent paramDragEvent) {
        Slog.d(TAG, "handleDragEnded view=" + this + " desc=" + ((Object) getContentDescription()));
        DragAnimationListener dragAnimationListener = this.mDragAnimationListener;
        if (dragAnimationListener != null) {
            dragAnimationListener.onDragEnded(this.mIsDropOnThisView);
        }
    }

    public void handleDragEntered(DragEvent dragEvent, int dragSurfaceAnimTyp) {
        Slog.d(TAG, "handleDragEntered view=" + this + " desc=" + ((Object) getContentDescription()));
        SplitAnimationAdapter splitAnimationAdapter = this.mSplitAnimation;
        if (splitAnimationAdapter != null) {
            splitAnimationAdapter.playColorShowAnimation();
            this.mSplitAnimation.playScaleDownAnimation();
        }
        DragAnimationListener dragAnimationListener = this.mDragAnimationListener;
        if (dragAnimationListener != null) {
            dragAnimationListener.onDragEntered(this, dragEvent, this.mSplitMode, dragSurfaceAnimTyp);
        }
        if (this.mHasPendingDropView) {
            addHotAreaPendingDropView();
        }
    }

    public void handleDragExited(DragEvent dragEvent) {
        Slog.d(TAG, "handleDragExited view=" + this + " desc=" + ((Object) getContentDescription()));
        SplitAnimationAdapter splitAnimationAdapter = this.mSplitAnimation;
        if (splitAnimationAdapter != null) {
            splitAnimationAdapter.playColorHideAnimation();
            this.mSplitAnimation.playScaleUpAnimation();
        }
        DragAnimationListener dragAnimationListener = this.mDragAnimationListener;
        if (dragAnimationListener != null) {
            dragAnimationListener.onDragExited(this);
        }
    }

    public void handleDragLocation(DragEvent dragEvent) {
        Slog.d(TAG, "handleDragLocation view=" + this + " desc=" + ((Object) getContentDescription()));
        DragAnimationListener dragAnimationListener = this.mDragAnimationListener;
        if (dragAnimationListener != null) {
            dragAnimationListener.onDragLocation();
        }
    }

    void handleDragStarted(DragEvent dragEvent) {
        Slog.d(TAG, "handleDragStarted view=" + this + " desc=" + ((Object) getContentDescription()));
        DragAnimationListener dragAnimationListener = this.mDragAnimationListener;
        if (dragAnimationListener != null) {
            dragAnimationListener.onDragStarted();
        }
    }

    public void initSplitScaleAnimation() {
        Slog.d(TAG, "initSplitScaleAnimation");
        int i = this.mSplitMode;
        if (i == 1 || i == 2 || i == 3 || i == 4) {
            this.mSplitAnimation = new SplitAnimationAdapter(this, (ColorDrawable) getForeground(), this.mSplitMode, getContext());
        }
    }

    public void handleDrop(DragEvent dragEvent, int dragSurfaceAnimType, boolean isUpDownSplitDrop) {
        Slog.d(TAG, "handleDrop view=" + this + " desc=" + ((Object) getContentDescription()));
        this.mIsDropOnThisView = true;
        SplitAnimationAdapter splitAnimationAdapter = this.mSplitAnimation;
        if (splitAnimationAdapter != null) {
            splitAnimationAdapter.playColorHideAnimation();
        }
        DragAnimationListener dragAnimationListener = this.mDragAnimationListener;
        if (dragAnimationListener != null) {
            dragAnimationListener.onDrop(this, dragEvent, this.mSplitMode, getDropBound(), dragSurfaceAnimType, isUpDownSplitDrop);
        }
    }

    @Override // com.android.server.multiwin.view.MiuiMultiWinClipImageView
    public boolean isLandScape() {
        return this.mIsLandScape;
    }

    @Override // android.view.View
    public boolean onDragEvent(DragEvent dragEvent) {
        int i = 5;
        switch (dragEvent.getAction()) {
            case 1:
                handleDragStarted(dragEvent);
                return true;
            case 2:
                handleDragLocation(dragEvent);
                return true;
            case 3:
                int animType = this.mSplitMode == 5 ? 4 : 6;
                handleDrop(dragEvent, animType, false);
                return true;
            case 4:
                boolean isHandle = dragEvent.getResult();
                handleDragEnded(dragEvent);
                return isHandle;
            case 5:
                if (this.mSplitMode == 5) {
                    i = 3;
                }
                int animType2 = i;
                handleDragEntered(dragEvent, animType2);
                return true;
            case 6:
                handleDragExited(dragEvent);
                return true;
            default:
                return true;
        }
    }

    public void setDragAnimationListener(DragAnimationListener listener) {
        this.mDragAnimationListener = listener;
    }

    public void setDragSplitMode(int dragSplitMode) {
        this.mDragSplitMode = dragSplitMode;
    }

    public void setHotAreaLayout(ViewGroup hotAreaLayout) {
        this.mHotAreaLayout = hotAreaLayout;
    }

    public void setMiuiMultiWinHotAreaConfigListener(MiuiMultiWinHotAreaConfigListener listener) {
        this.mMiuiMultiWinHotAreaConfigListener = listener;
    }

    public void setInitialDragSplitMode(int initialDragSplitMode) {
        this.mInitialDragSplitMode = initialDragSplitMode;
    }

    @Override // com.android.server.multiwin.view.MiuiMultiWinClipImageView
    public void setIsLandScape(boolean isLandScape) {
        this.mIsLandScape = isLandScape;
    }

    public void setNavBarInfo(Rect navBarBound, int navBarPos) {
        this.mNavBarBound = navBarBound;
        this.mNavBarPos = navBarPos;
    }

    public void setSplitBarController(MiuiMultiWinSplitBarController splitBarController) {
        this.mSplitBarController = splitBarController;
    }

    public void setSplitMode(int paramInt) {
        this.mSplitMode = paramInt;
    }

    public void measureAndLayoutHotArea() {
        ViewGroup viewGroup = this.mHotAreaLayout;
        if (viewGroup != null) {
            int widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(viewGroup.getMeasuredWidth(), 1073741824);
            int heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(this.mHotAreaLayout.getMeasuredHeight(), 1073741824);
            this.mHotAreaLayout.measure(widthMeasureSpec, heightMeasureSpec);
            ViewGroup viewGroup2 = this.mHotAreaLayout;
            viewGroup2.layout(0, 0, viewGroup2.getMeasuredWidth(), this.mHotAreaLayout.getMeasuredHeight());
        }
    }

    public int[] getDragEndLocation() {
        int[] loc = new int[2];
        getLocationOnScreen(loc);
        return loc;
    }

    public int getDragEndWidth() {
        return getWidth();
    }

    public int getDragEndHeight() {
        return getHeight();
    }
}
