package com.android.server.lights.view;

import android.animation.FloatArrayEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import com.android.server.lights.interpolater.ExpoEaseOutInterpolater;
import com.android.server.wm.MiuiFreeformPinManagerService;
import com.miui.internal.dynamicanimation.animation.DynamicAnimation;
import com.miui.internal.dynamicanimation.animation.FloatValueHolder;
import com.miui.internal.dynamicanimation.animation.SpringAnimation;
import com.miui.internal.dynamicanimation.animation.SpringForce;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
/* loaded from: classes.dex */
public class MessageView extends View {
    private static final float LEFT_PENDDING = 708.0f;
    private static final float RIGHT_PENDDING = 118.0f;
    public static final int STAR_MAX = 100;
    public static final int WAVE_LINE_NUM = 9;
    public static final int WAVE_LINE_RANGE = 8;
    public static final int WAVE_THICK = 2;
    private float mAlphaTarX;
    private float mCtrlTarX;
    private LineData mDataList;
    private PictureData mLightBlue;
    private PictureData mLightHigh;
    private LinearGradient mLinearGradient;
    private Paint mPaint;
    private SpringAnimation mSpringAnimation;
    private Star[] mStarArr;

    public MessageView(Context context) {
        this(context, null);
    }

    public MessageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MessageView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public MessageView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mLightHigh = new PictureData();
        this.mLightBlue = new PictureData();
        this.mDataList = new LineData();
        this.mStarArr = new Star[100];
        init();
    }

    private void init() {
        setBackgroundColor(0);
        if (this.mLinearGradient == null) {
            this.mLinearGradient = new LinearGradient((float) MiuiFreeformPinManagerService.EDGE_AREA, (float) MiuiFreeformPinManagerService.EDGE_AREA, (float) MiuiFreeformPinManagerService.EDGE_AREA, 2250.0f, new int[]{Color.parseColor("#ff6e02"), Color.parseColor("#ffff00"), Color.parseColor("#ff6e02")}, (float[]) null, Shader.TileMode.REPEAT);
        }
        for (int i = 0; i < 9; i++) {
            this.mDataList.mLeftPath.add(new Path());
            this.mDataList.mRightPath.add(new Path());
            Paint topPaint = new Paint(1);
            topPaint.setStyle(Paint.Style.STROKE);
            topPaint.setStrokeWidth(2.0f);
            topPaint.setColor(-1);
            this.mDataList.mPaint.add(topPaint);
        }
        Paint paint = new Paint(1);
        this.mPaint = paint;
        paint.setStyle(Paint.Style.STROKE);
        this.mPaint.setStrokeWidth(2.0f);
        this.mPaint.setColor(-1);
        InputStream inLightHigh = null;
        InputStream inLightBlue = null;
        InputStream inStar = null;
        try {
            inLightHigh = getResources().openRawResource(285737242);
            inLightBlue = getResources().openRawResource(285737241);
            inStar = getResources().openRawResource(285737425);
            this.mLightHigh.mBitmap = BitmapFactory.decodeStream(inLightHigh);
            this.mLightBlue.mBitmap = BitmapFactory.decodeStream(inLightBlue);
            this.mLightHigh.mMatrix = new Matrix();
            this.mLightBlue.mMatrix = new Matrix();
            this.mLightHigh.mMatrix.setScale(1.0f, 1.0f);
            this.mLightBlue.mMatrix.setScale(1.0f, 1.0f);
            for (int i2 = 0; i2 < 100; i2++) {
                this.mStarArr[i2] = new Star(getContext());
            }
            setVisibility(8);
        } finally {
            if (inLightHigh != null) {
                try {
                    inLightHigh.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (inLightBlue != null) {
                inLightBlue.close();
            }
            if (inStar != null) {
                inStar.close();
            }
        }
    }

    @Override // android.view.View
    protected void onDraw(Canvas canvas) {
        int width = getWidth();
        for (int i = 0; i < 9; i++) {
            this.mDataList.mLeftPath.get(i).reset();
            this.mDataList.mLeftPath.get(i).moveTo(LEFT_PENDDING, this.mDataList.mConfig.y);
            this.mDataList.mRightPath.get(i).reset();
            this.mDataList.mRightPath.get(i).moveTo(width - RIGHT_PENDDING, this.mDataList.mConfig.y);
        }
        for (int index = 0; index <= this.mDataList.mConfig.waveWidth; index++) {
            for (int i2 = 0; i2 < 9; i2++) {
                float waveRate = (this.mDataList.mConfig.waveHeight - (i2 * 8)) / this.mDataList.mConfig.waveHeight;
                float waveAlpha = (float) ((((this.mAlphaTarX / 100.0f) * 1.5f) - Math.abs(waveRate)) - 0.3d);
                float endX = (((((float) Math.sin(((((index - this.mDataList.mConfig.waveCenter) / (this.mDataList.mConfig.waveWidth / 360.0f)) + 90.0f) * 3.141592653589793d) / 180.0d)) + 1.0f) * this.mDataList.mConfig.waveHeight) * waveRate) / 2.0f;
                this.mDataList.mLeftPath.get(i2).lineTo(endX + LEFT_PENDDING, this.mDataList.mConfig.y + index);
                this.mDataList.mRightPath.get(i2).lineTo((width - endX) - RIGHT_PENDDING, this.mDataList.mConfig.y + index);
                if (waveAlpha < MiuiFreeformPinManagerService.EDGE_AREA) {
                    waveAlpha = MiuiFreeformPinManagerService.EDGE_AREA;
                } else if (waveAlpha > 1.0f) {
                    waveAlpha = 1.0f;
                }
                this.mDataList.mPaint.get(i2).setAlpha((int) (255.0f * waveAlpha));
            }
        }
        for (int i3 = 0; i3 < 9; i3++) {
            canvas.drawPath(this.mDataList.mLeftPath.get(i3), this.mDataList.mPaint.get(i3));
            canvas.drawPath(this.mDataList.mRightPath.get(i3), this.mDataList.mPaint.get(i3));
        }
        this.mLightHigh.mMatrix.setScale((float) this.mLightHigh.mScaleX, (float) this.mLightHigh.mScaleY);
        Bitmap bitmapHigh = Bitmap.createBitmap(this.mLightHigh.mBitmap, 0, 0, this.mLightHigh.mBitmap.getWidth(), this.mLightHigh.mBitmap.getHeight(), this.mLightHigh.mMatrix, true);
        this.mLightBlue.mMatrix.setScale((float) this.mLightBlue.mScaleX, (float) this.mLightBlue.mScaleY);
        Bitmap bitmapBlue = Bitmap.createBitmap(this.mLightBlue.mBitmap, 0, 0, this.mLightBlue.mBitmap.getWidth(), this.mLightBlue.mBitmap.getHeight(), this.mLightBlue.mMatrix, true);
        if (this.mLightHigh.mAlpha > 1.0d) {
            this.mLightHigh.mAlpha = 1.0d;
        } else if (this.mLightHigh.mAlpha < 0.0d) {
            this.mLightHigh.mAlpha = 0.0d;
        }
        this.mPaint.setAlpha((int) (this.mLightHigh.mAlpha * 255.0d));
        int top = (int) ((getHeight() - (this.mLightHigh.mBitmap.getHeight() * this.mLightHigh.mScaleY)) / 2.0d);
        int left = (int) (708.0d - ((this.mLightHigh.mBitmap.getWidth() * this.mLightHigh.mScaleX) / 2.0d));
        int leftLeft = (int) ((getWidth() - RIGHT_PENDDING) - ((this.mLightHigh.mBitmap.getWidth() * this.mLightHigh.mScaleX) / 2.0d));
        canvas.drawBitmap(bitmapHigh, left, top, this.mPaint);
        canvas.drawBitmap(bitmapHigh, leftLeft, top, this.mPaint);
        if (this.mLightBlue.mAlpha > 1.0d) {
            this.mLightBlue.mAlpha = 1.0d;
        } else if (this.mLightBlue.mAlpha < 0.0d) {
            this.mLightBlue.mAlpha = 0.0d;
        }
        this.mPaint.setAlpha((int) (this.mLightBlue.mAlpha * 255.0d));
        int top2 = (int) ((getHeight() - (this.mLightBlue.mBitmap.getHeight() * this.mLightBlue.mScaleY)) / 2.0d);
        int left2 = (int) (708.0d - ((this.mLightBlue.mBitmap.getWidth() * this.mLightBlue.mScaleX) / 2.0d));
        int leftLeft2 = (int) ((getWidth() - RIGHT_PENDDING) - ((this.mLightBlue.mBitmap.getWidth() * this.mLightBlue.mScaleX) / 2.0d));
        canvas.drawBitmap(bitmapBlue, left2, top2, this.mPaint);
        canvas.drawBitmap(bitmapBlue, leftLeft2, top2, this.mPaint);
        Star[] starArr = this.mStarArr;
        int length = starArr.length;
        char c = 0;
        int i4 = 0;
        while (i4 < length) {
            Star star = starArr[i4];
            this.mPaint.setAlpha((int) star.prop[2]);
            canvas.drawBitmap(star.mStarBitmap, star.prop[c] + LEFT_PENDDING, star.prop[1] + (getHeight() / 2), this.mPaint);
            canvas.drawBitmap(star.mStarBitmap, (getWidth() - RIGHT_PENDDING) - star.prop[0], star.prop[1] + (getHeight() / 2), this.mPaint);
            i4++;
            width = width;
            c = 0;
        }
    }

    public void startAnimation() {
        Star[] starArr;
        setVisibility(0);
        SpringForce springForce = new SpringForce((float) MiuiFreeformPinManagerService.EDGE_AREA);
        springForce.setDampingRatio(0.98f);
        springForce.setStiffness(12.18f);
        FloatValueHolder valueHolder = new FloatValueHolder();
        SpringAnimation spring = new SpringAnimation(valueHolder).setStartVelocity(2000.0f).setStartValue((float) MiuiFreeformPinManagerService.EDGE_AREA).setSpring(springForce);
        this.mSpringAnimation = spring;
        spring.addUpdateListener(new DynamicAnimation.OnAnimationUpdateListener() { // from class: com.android.server.lights.view.MessageView.1
            public void onAnimationUpdate(DynamicAnimation dynamicAnimation, float v, float v1) {
                MessageView.this.mCtrlTarX = v;
                MessageView messageView = MessageView.this;
                messageView.mAlphaTarX = (float) Math.max(Math.min(messageView.mCtrlTarX / 2.0d, 100.0d), 0.0d);
                float per = MessageView.this.mAlphaTarX / 100.0f;
                MessageView.this.mLightHigh.mScaleX = (per * 0.2d) + 0.8d;
                MessageView.this.mLightHigh.mScaleY = (per * 0.4d) + 0.6d;
                MessageView.this.mLightHigh.mAlpha = Math.min(((1.0d - Math.pow(1.0f - per, 3.0d)) * 1.5d) - 0.2d, 2.0d);
                MessageView.this.mLightBlue.mScaleX = (per * 0.8d) + 0.2d;
                MessageView.this.mLightBlue.mScaleY = (per * 0.4d) + 0.6d;
                MessageView.this.mLightBlue.mAlpha = (1.0d - (Math.pow(1.0f - per, 2.0d) * 1.1d)) - 0.1d;
                MessageView.this.invalidate();
            }
        });
        this.mSpringAnimation.addEndListener(new DynamicAnimation.OnAnimationEndListener() { // from class: com.android.server.lights.view.MessageView.2
            public void onAnimationEnd(DynamicAnimation dynamicAnimation, boolean b, float v, float v1) {
                Star[] starArr2;
                for (Star star : MessageView.this.mStarArr) {
                    star.stop();
                }
                MessageView.this.setVisibility(8);
            }
        });
        for (Star star : this.mStarArr) {
            star.boom();
        }
        this.mSpringAnimation.start();
    }

    public boolean isAnimationRunning() {
        SpringAnimation springAnimation = this.mSpringAnimation;
        if (springAnimation != null) {
            return springAnimation.isRunning();
        }
        return false;
    }

    public void stopAnimation() {
        Star[] starArr;
        for (Star star : this.mStarArr) {
            if (star != null) {
                star.stop();
            }
        }
        SpringAnimation springAnimation = this.mSpringAnimation;
        if (springAnimation != null) {
            springAnimation.skipToEnd();
        }
        setVisibility(8);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class WaveConfig {
        public static final int EDGE_X = 700;
        public int x = 0;
        public int y = EDGE_X;
        public int acc = 120;
        public int width = 2250;
        public int waveCenter = 425;
        public int waveWidth = 850;
        public int waveHeight = 32;

        WaveConfig() {
            MessageView.this = this$0;
        }
    }

    /* loaded from: classes.dex */
    public class LineData {
        public WaveConfig mConfig;
        public List<Path> mLeftPath = new ArrayList();
        public List<Path> mRightPath = new ArrayList();
        public List<Paint> mPaint = new ArrayList();

        public LineData() {
            MessageView.this = this$0;
            this.mConfig = new WaveConfig();
        }
    }

    /* loaded from: classes.dex */
    public class PictureData {
        public double mAlpha;
        private Bitmap mBitmap;
        private Matrix mMatrix;
        public double mScaleX = 1.0d;
        public double mScaleY = 1.0d;

        PictureData() {
            MessageView.this = this$0;
        }
    }

    /* loaded from: classes.dex */
    public class Star {
        private Context mContext;
        private Matrix mMatrix;
        private Bitmap mStarBitmap;
        private Bitmap mStarBitmapOri;
        private ValueAnimator mValueAnimator;
        private float[] prop = new float[5];

        public Star(Context context) {
            MessageView.this = this$0;
            this.mContext = context;
            InputStream inStar = null;
            InputStream inStarOri = null;
            try {
                inStar = context.getResources().openRawResource(285737425);
                this.mStarBitmap = BitmapFactory.decodeStream(inStar);
                inStarOri = this.mContext.getResources().openRawResource(285737425);
                this.mStarBitmapOri = BitmapFactory.decodeStream(inStarOri);
                Matrix matrix = new Matrix();
                this.mMatrix = matrix;
                matrix.setScale(1.0f, 1.0f);
                if (inStar != null) {
                    try {
                        inStar.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                        return;
                    }
                }
                if (inStarOri != null) {
                    inStarOri.close();
                }
            } catch (Throwable th) {
                if (inStar != null) {
                    try {
                        inStar.close();
                    } catch (IOException e2) {
                        e2.printStackTrace();
                        throw th;
                    }
                }
                if (inStarOri != null) {
                    inStarOri.close();
                }
                throw th;
            }
        }

        public void boom() {
            float initY = (float) ((Math.random() - 0.5d) * 720.0d);
            float angle = (float) ((initY / 10.0f) + ((Math.random() - 0.5d) * 60.0d));
            float dis = (float) ((Math.random() * 150.0d) + 50.0d);
            float scale = (float) ((Math.random() * 0.5d) + 0.5d);
            int duration = (int) (((dis / 200.0f) + (Math.random() * 5.0d)) * 1000.0d);
            float[] start = {MiuiFreeformPinManagerService.EDGE_AREA, initY, 255.0f, scale, scale};
            float endX = (float) ((Math.random() > 0.5d ? 1 : -1) * Math.cos((angle * 3.141592653589793d) / 180.0d) * dis);
            float endY = (float) (initY + (Math.sin((angle * 3.141592653589793d) / 180.0d) * dis));
            float[] end = {endX, endY, MiuiFreeformPinManagerService.EDGE_AREA, scale / 2.0f, scale / 2.0f};
            ValueAnimator ofObject = ValueAnimator.ofObject(new FloatArrayEvaluator(), start, end);
            this.mValueAnimator = ofObject;
            ofObject.setDuration(duration);
            this.mValueAnimator.setInterpolator(new ExpoEaseOutInterpolater());
            this.mValueAnimator.setRepeatCount(-1);
            this.mValueAnimator.setRepeatCount(1);
            this.mValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.android.server.lights.view.MessageView.Star.1
                @Override // android.animation.ValueAnimator.AnimatorUpdateListener
                public void onAnimationUpdate(ValueAnimator animation) {
                    Star.this.prop = (float[]) animation.getAnimatedValue();
                    Star.this.mMatrix.setScale(Star.this.prop[3], Star.this.prop[4]);
                    Star star = Star.this;
                    star.mStarBitmap = Bitmap.createBitmap(star.mStarBitmapOri, 0, 0, Star.this.mStarBitmapOri.getWidth(), Star.this.mStarBitmapOri.getHeight(), Star.this.mMatrix, true);
                }
            });
            if (this.mValueAnimator.isRunning()) {
                this.mValueAnimator.cancel();
            }
            this.mValueAnimator.start();
        }

        public void stop() {
            this.mValueAnimator.cancel();
        }
    }
}
