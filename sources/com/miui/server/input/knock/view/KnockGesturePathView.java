package com.miui.server.input.knock.view;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.os.Handler;
import android.util.Slog;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import com.android.server.am.ActivityManagerServiceImpl;
import com.android.server.input.MiuiInputThread;
import java.util.ArrayList;
/* loaded from: classes.dex */
public class KnockGesturePathView extends View implements KnockPathListener {
    private static final float BLUR_SIZE = 5.0f;
    private static final int FLAG_SHOW_FOR_ALL_USERS = 16;
    private static final int MIN_DISTANCE_SHOW_VIEW = 100;
    private static final String TAG = "KnockGesturePathView";
    private int mDataPointCount;
    private float mDownX;
    private float mDownY;
    private volatile boolean mIsAdded;
    private WindowManager.LayoutParams mLayoutParams;
    private final Paint mMaskLine;
    private final Paint mPloyLine;
    private final ArrayList<KnockPointerState> mPointers = new ArrayList<>();
    private boolean mVisibility = true;
    private volatile boolean mDistanceShowView = false;
    private final Runnable mRemoveWindowRunnable = new Runnable() { // from class: com.miui.server.input.knock.view.KnockGesturePathView.1
        @Override // java.lang.Runnable
        public void run() {
            if (!KnockGesturePathView.this.mIsAdded) {
                return;
            }
            KnockGesturePathView.this.mIsAdded = false;
            try {
                KnockGesturePathView.this.mWindowManager.removeViewImmediate(KnockGesturePathView.this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };
    private final WindowManager mWindowManager = (WindowManager) this.mContext.getSystemService(WindowManager.class);
    private final Handler mHandler = new Handler(MiuiInputThread.getThread().getLooper());
    private Path mPath = new Path();

    public KnockGesturePathView(Context context) {
        super(context);
        setLayerType(2, null);
        initLayoutParam();
        Paint paint = new Paint();
        this.mPloyLine = paint;
        paint.setFlags(1);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStyle(Paint.Style.STROKE);
        paint.setARGB(255, 34, 224, 255);
        paint.setStrokeWidth(4.5f);
        Paint paint2 = new Paint();
        this.mMaskLine = paint2;
        paint2.setFlags(1);
        paint2.setStyle(Paint.Style.STROKE);
        paint2.setStrokeCap(Paint.Cap.ROUND);
        paint2.setStrokeJoin(Paint.Join.ROUND);
        paint2.setMaskFilter(new BlurMaskFilter(BLUR_SIZE, BlurMaskFilter.Blur.SOLID));
        paint2.setARGB(255, 34, 224, 255);
    }

    private void initLayoutParam() {
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(-1, -1);
        this.mLayoutParams = layoutParams;
        layoutParams.type = 2015;
        this.mLayoutParams.flags = 1304;
        this.mLayoutParams.layoutInDisplayCutoutMode = 1;
        if (ActivityManager.isHighEndGfx()) {
            this.mLayoutParams.flags |= 16777216;
            this.mLayoutParams.privateFlags |= 2;
        }
        this.mLayoutParams.format = -3;
        WindowManager.LayoutParams layoutParams2 = this.mLayoutParams;
        layoutParams2.inputFeatures = 1 | layoutParams2.inputFeatures;
        this.mLayoutParams.privateFlags |= 16;
        this.mLayoutParams.setTitle("knock_drawing");
    }

    @Override // android.view.View
    protected void onAttachedToWindow() {
        Slog.i(TAG, "KnockGesturePathView -> onAttachedToWindow");
        if (this.mParent == null) {
            Slog.i(TAG, "onAttachedToWindow mParent null");
        } else {
            super.onAttachedToWindow();
        }
    }

    @Override // android.view.View
    protected void onDetachedFromWindow() {
        Slog.i(TAG, "KnockGesturePathView -> onDetachedFromWindow");
        super.onDetachedFromWindow();
    }

    @Override // android.view.View
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(0, PorterDuff.Mode.CLEAR);
        if (this.mVisibility && this.mDataPointCount > 5) {
            canvas.drawPath(this.mPath, this.mPloyLine);
            int mLastLightCount = 15;
            int i = this.mDataPointCount;
            if (i < 15) {
                mLastLightCount = this.mDataPointCount;
            }
            float m = 4.5f;
            for (int i2 = i - mLastLightCount; i2 < this.mDataPointCount - 1; i2++) {
                float f = 0.8f + m;
                m = f;
                this.mMaskLine.setStrokeWidth(f);
                canvas.drawLine(getPointerPathData().mTraceX[i2].floatValue(), getPointerPathData().mTraceY[i2].floatValue(), getPointerPathData().mTraceX[i2 + 1].floatValue(), getPointerPathData().mTraceY[i2 + 1].floatValue(), this.mMaskLine);
            }
        }
    }

    private void tryAddToWindow() {
        this.mHandler.post(new Runnable() { // from class: com.miui.server.input.knock.view.KnockGesturePathView$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                KnockGesturePathView.this.m2255xb231b2c8();
            }
        });
    }

    /* renamed from: lambda$tryAddToWindow$0$com-miui-server-input-knock-view-KnockGesturePathView */
    public /* synthetic */ void m2255xb231b2c8() {
        if (!this.mIsAdded && this.mDistanceShowView) {
            this.mIsAdded = true;
            try {
                this.mWindowManager.addView(this, this.mLayoutParams);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void tryRemoveWindow() {
        hideView();
        this.mHandler.postDelayed(this.mRemoveWindowRunnable, ActivityManagerServiceImpl.BOOST_DURATION);
    }

    public void removeViewImmediate() {
        this.mHandler.removeCallbacks(this.mRemoveWindowRunnable);
        this.mHandler.post(this.mRemoveWindowRunnable);
    }

    public void onPointerEvent(MotionEvent event) {
        int action = event.getAction();
        switch (action) {
            case 0:
                this.mDownX = event.getX();
                this.mDownY = event.getY();
                this.mHandler.removeCallbacks(this.mRemoveWindowRunnable);
                this.mDistanceShowView = false;
                this.mVisibility = true;
                this.mPointers.clear();
                KnockPointerState ps = new KnockPointerState();
                ps.addTrace(event.getX(), event.getY(), false);
                this.mPointers.add(ps);
                Path path = this.mPath;
                if (path == null) {
                    this.mPath = new Path();
                } else {
                    path.reset();
                }
                this.mPath.moveTo(this.mDownX, this.mDownY);
                this.mDataPointCount = getPointerPathData().getTraceCount();
                if (this.mIsAdded) {
                    flush();
                    return;
                }
                return;
            case 1:
            case 3:
                if (this.mPointers.size() == 0) {
                    return;
                }
                this.mDataPointCount = getPointerPathData().getTraceCount();
                pathLink(this.mPath);
                hideView();
                tryRemoveWindow();
                return;
            case 2:
                if (this.mPointers.size() == 0) {
                    return;
                }
                KnockPointerState pointerState = this.mPointers.get(0);
                if (event.getHistorySize() > 0) {
                    for (int i = 0; i < event.getHistorySize(); i++) {
                        pointerState.addTrace(event.getHistoricalAxisValue(0, i), event.getHistoricalAxisValue(1, i), false);
                        this.mDataPointCount = getPointerPathData().getTraceCount();
                        pathLink(this.mPath);
                    }
                }
                float nowX = event.getX();
                float nowY = event.getY();
                if (!this.mDistanceShowView && Math.sqrt(Math.pow(nowX - this.mDownX, 2.0d) + Math.pow(nowY - this.mDownY, 2.0d)) > 100.0d) {
                    this.mDistanceShowView = true;
                    tryAddToWindow();
                }
                pointerState.addTrace(nowX, nowY, true);
                this.mDataPointCount = getPointerPathData().getTraceCount();
                pathLink(this.mPath);
                flush();
                return;
            case 4:
            default:
                return;
            case 5:
                hideView();
                return;
        }
    }

    @Override // com.miui.server.input.knock.view.KnockPathListener
    public KnockPointerState getPointerPathData() {
        if (this.mPointers.size() == 0) {
            return null;
        }
        return this.mPointers.get(0);
    }

    @Override // com.miui.server.input.knock.view.KnockPathListener
    public void hideView() {
        this.mVisibility = false;
        flush();
    }

    private void flush() {
        this.mHandler.post(new Runnable() { // from class: com.miui.server.input.knock.view.KnockGesturePathView$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                KnockGesturePathView.this.invalidate();
            }
        });
    }

    private void pathLink(Path path) {
        if (this.mDataPointCount < 2) {
            return;
        }
        float preX = getPointerPathData().mTraceX[this.mDataPointCount - 2].floatValue();
        float preY = getPointerPathData().mTraceY[this.mDataPointCount - 2].floatValue();
        float endX = (getPointerPathData().mTraceX[this.mDataPointCount - 1].floatValue() + preX) / 2.0f;
        float endY = (getPointerPathData().mTraceY[this.mDataPointCount - 1].floatValue() + preY) / 2.0f;
        path.quadTo(preX, preY, endX, endY);
    }

    /* loaded from: classes.dex */
    public static class KnockPointerState {
        public int mTraceCount;
        public Float[] mTraceX = new Float[32];
        public Float[] mTraceY = new Float[32];
        public boolean[] mTraceCurrent = new boolean[32];

        public void addTrace(float x, float y, boolean current) {
            Float[] fArr = this.mTraceX;
            int traceCapacity = fArr.length;
            int i = this.mTraceCount;
            if (i == traceCapacity) {
                int traceCapacity2 = traceCapacity * 2;
                Float[] newTraceX = new Float[traceCapacity2];
                System.arraycopy(fArr, 0, newTraceX, 0, i);
                this.mTraceX = newTraceX;
                Float[] newTraceY = new Float[traceCapacity2];
                System.arraycopy(this.mTraceY, 0, newTraceY, 0, this.mTraceCount);
                this.mTraceY = newTraceY;
                boolean[] newTraceCurrent = new boolean[traceCapacity2];
                System.arraycopy(this.mTraceCurrent, 0, newTraceCurrent, 0, this.mTraceCount);
                this.mTraceCurrent = newTraceCurrent;
            }
            this.mTraceX[this.mTraceCount] = Float.valueOf(x);
            this.mTraceY[this.mTraceCount] = Float.valueOf(y);
            boolean[] zArr = this.mTraceCurrent;
            int i2 = this.mTraceCount;
            zArr[i2] = current;
            this.mTraceCount = i2 + 1;
        }

        public int getTraceCount() {
            return this.mTraceCount;
        }
    }
}
