package com.android.server.wm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.graphics.BLASTBufferQueue;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.Slog;
import android.view.Surface;
import android.view.SurfaceControl;
import com.miui.base.MiuiStubRegistry;
import miui.android.animation.controller.AnimState;
/* loaded from: classes.dex */
public class TalkbackWatermark implements TalkbackWatermarkStub {
    private static final boolean DEBUG = false;
    private static final String TAG = "TalkbackWatermark";
    private BLASTBufferQueue mBlastBufferQueue;
    private BroadcastReceiver mBroadcast;
    private int mDetPx;
    private int mPaddingPx;
    private String mString1;
    private String mString2;
    private Surface mSurface;
    private int mTextSizePx;
    private float mTitleSizePx;
    private WindowManagerService mWms;
    private final float mTitleSizeDp = 25.45f;
    private final float mTextSizeDp = 20.0f;
    private final float mDetDp = 20.37f;
    private final float mPaddingDp = 12.36f;
    private final float mShadowRadius = 1.0f;
    private final float mShadowDx = 2.0f;
    private final float mShadowDy = 2.0f;
    private final float mYProportionTop = 0.4f;
    private final float mXProportion = 0.5f;
    private SurfaceControl mSurfaceControl = null;
    private SurfaceControl.Transaction mTransaction = null;
    private int mLastDW = 0;
    private int mLastDH = 0;
    private boolean mDrawNeeded = false;
    private boolean mHasDrawn = false;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<TalkbackWatermark> {

        /* compiled from: TalkbackWatermark$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final TalkbackWatermark INSTANCE = new TalkbackWatermark();
        }

        public TalkbackWatermark provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public TalkbackWatermark provideNewInstance() {
            return new TalkbackWatermark();
        }
    }

    TalkbackWatermark() {
    }

    public void init(WindowManagerService wms) {
        final Uri talkbackWatermarkEnableUri = Settings.Secure.getUriFor("talkback_watermark_enable");
        wms.mContext.getContentResolver().registerContentObserver(talkbackWatermarkEnableUri, false, new ContentObserver(new Handler(Looper.getMainLooper())) { // from class: com.android.server.wm.TalkbackWatermark.1
            {
                TalkbackWatermark.this = this;
            }

            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange, Uri uri) {
                if (talkbackWatermarkEnableUri.equals(uri)) {
                    TalkbackWatermark.this.updateWaterMark();
                }
            }
        }, -1);
        this.mWms = wms;
        setupBroadcast();
    }

    private synchronized void setupBroadcast() {
        if (this.mBroadcast == null) {
            this.mBroadcast = new BroadcastReceiver() { // from class: com.android.server.wm.TalkbackWatermark.2
                {
                    TalkbackWatermark.this = this;
                }

                @Override // android.content.BroadcastReceiver
                public void onReceive(Context context, Intent intent) {
                    char c;
                    String action = intent.getAction();
                    switch (action.hashCode()) {
                        case -19011148:
                            if (action.equals("android.intent.action.LOCALE_CHANGED")) {
                                c = 0;
                                break;
                            }
                        default:
                            c = 65535;
                            break;
                    }
                    switch (c) {
                        case 0:
                            TalkbackWatermark.this.refresh();
                            return;
                        default:
                            return;
                    }
                }
            };
        }
    }

    public void updateWaterMark() {
        this.mWms.mH.post(new Runnable() { // from class: com.android.server.wm.TalkbackWatermark$$ExternalSyntheticLambda3
            @Override // java.lang.Runnable
            public final void run() {
                TalkbackWatermark.this.m1894lambda$updateWaterMark$0$comandroidserverwmTalkbackWatermark();
            }
        });
    }

    /* renamed from: lambda$updateWaterMark$0$com-android-server-wm-TalkbackWatermark */
    public /* synthetic */ void m1894lambda$updateWaterMark$0$comandroidserverwmTalkbackWatermark() {
        boolean z = true;
        if (Settings.Secure.getIntForUser(this.mWms.mContext.getContentResolver(), "talkback_watermark_enable", 1, -2) == 0) {
            z = false;
        }
        boolean enabled = z;
        this.mWms.openSurfaceTransaction();
        Slog.d(TAG, "talkback-test enabled =" + enabled);
        try {
            Slog.d(TAG, "talkback-test enabled =" + enabled);
            if (enabled) {
                doCreateSurface(this.mWms);
                showInternal();
            } else {
                dismissInternal();
            }
        } finally {
            this.mWms.closeSurfaceTransaction("updateTalkbackWatermark");
        }
    }

    private synchronized void doCreateSurface(WindowManagerService wms) {
        DisplayContent dc = wms.getDefaultDisplayContentLocked();
        float constNum = dc.mRealDisplayMetrics.densityDpi / 160.0f;
        this.mTextSizePx = (int) (20.0f * constNum);
        this.mDetPx = (int) (20.37f * constNum);
        this.mPaddingPx = (int) (12.36f * constNum);
        this.mTitleSizePx = (int) (25.45f * constNum);
        try {
            SurfaceControl ctrl = dc.makeOverlay().setName("TalkbackWatermarkSurface").setBLASTLayer().setBufferSize(1, 1).setFormat(-3).setCallsite("TalkbackWatermarkSurface").build();
            SurfaceControl.Transaction transaction = (SurfaceControl.Transaction) wms.mTransactionFactory.get();
            this.mTransaction = transaction;
            transaction.setLayer(ctrl, AnimState.VIEW_SIZE);
            this.mTransaction.setPosition(ctrl, MiuiFreeformPinManagerService.EDGE_AREA, MiuiFreeformPinManagerService.EDGE_AREA);
            this.mTransaction.show(ctrl);
            InputMonitor.setTrustedOverlayInputInfo(ctrl, this.mTransaction, dc.getDisplayId(), "TalkbackWatermarkSurface");
            this.mTransaction.apply();
            this.mSurfaceControl = ctrl;
            BLASTBufferQueue bLASTBufferQueue = new BLASTBufferQueue("TalkbackWatermarkSurface", this.mSurfaceControl, 1, 1, 1);
            this.mBlastBufferQueue = bLASTBufferQueue;
            this.mSurface = bLASTBufferQueue.createSurface();
        } catch (Surface.OutOfResourcesException e) {
            Slog.w(TAG, "createrSurface e" + e);
        }
    }

    public synchronized void positionSurface(int dw, int dh) {
        if (this.mSurfaceControl == null) {
            return;
        }
        if (this.mLastDW != dw || this.mLastDH != dh) {
            this.mLastDW = dw;
            this.mLastDH = dh;
            SurfaceControl.Transaction transaction = SurfaceControl.getGlobalTransaction();
            if (transaction != null) {
                transaction.setBufferSize(this.mSurfaceControl, dw, dh);
            }
            this.mDrawNeeded = true;
        }
    }

    private void drawIfNeeded() {
        if (this.mDrawNeeded) {
            int dw = this.mLastDW;
            int dh = this.mLastDH;
            this.mBlastBufferQueue.update(this.mSurfaceControl, dw, dh, 1);
            Rect dirty = new Rect(0, 0, dw, dh);
            Canvas c = null;
            try {
                c = this.mSurface.lockCanvas(dirty);
            } catch (Surface.OutOfResourcesException | IllegalArgumentException e) {
                Slog.w(TAG, "Failed to lock canvas", e);
            }
            if (c == null || c.getWidth() != dw || c.getHeight() != dh) {
                this.mSurface.unlockCanvasAndPost(c);
                return;
            }
            this.mDrawNeeded = false;
            c.drawColor(0, PorterDuff.Mode.CLEAR);
            int x = (int) (dw * 0.5f);
            int y = ((int) (dh * 0.4f)) + 60;
            Paint paint = new Paint(1);
            paint.setTextSize(this.mTitleSizePx);
            paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, 0));
            paint.setColor(-5000269);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setShadowLayer(1.0f, 2.0f, 2.0f, -16777216);
            c.drawText(this.mString1, x, y, paint);
            paint.setTextSize(this.mTextSizePx);
            TextPaint textPaint = new TextPaint(paint);
            StaticLayout staticLayout = new StaticLayout(this.mString2, textPaint, c.getWidth() - this.mPaddingPx, Layout.Alignment.ALIGN_NORMAL, 1.0f, MiuiFreeformPinManagerService.EDGE_AREA, false);
            c.save();
            c.translate(x, this.mDetPx + y);
            staticLayout.draw(c);
            c.restore();
            this.mSurface.unlockCanvasAndPost(c);
            this.mHasDrawn = true;
        }
    }

    public synchronized void show() {
        updateWaterMark();
        this.mWms.mH.post(new Runnable() { // from class: com.android.server.wm.TalkbackWatermark$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                TalkbackWatermark.this.m1893lambda$show$1$comandroidserverwmTalkbackWatermark();
            }
        });
    }

    /* renamed from: lambda$show$1$com-android-server-wm-TalkbackWatermark */
    public /* synthetic */ void m1893lambda$show$1$comandroidserverwmTalkbackWatermark() {
        this.mWms.mContext.registerReceiver(this.mBroadcast, new IntentFilter("android.intent.action.LOCALE_CHANGED"));
    }

    public synchronized void setVisible(boolean visible) {
        if (visible) {
            showInternal();
        } else {
            hideInternal();
        }
    }

    public synchronized void dismiss() {
        if (this.mSurfaceControl == null) {
            return;
        }
        this.mWms.mH.post(new Runnable() { // from class: com.android.server.wm.TalkbackWatermark$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                TalkbackWatermark.this.m1892lambda$dismiss$2$comandroidserverwmTalkbackWatermark();
            }
        });
        Slog.d(TAG, "talkback-test dismiss");
        this.mWms.mH.post(new TalkbackWatermark$$ExternalSyntheticLambda2(this));
    }

    /* renamed from: lambda$dismiss$2$com-android-server-wm-TalkbackWatermark */
    public /* synthetic */ void m1892lambda$dismiss$2$comandroidserverwmTalkbackWatermark() {
        this.mWms.mContext.unregisterReceiver(this.mBroadcast);
    }

    /* JADX WARN: Code restructure failed: missing block: B:11:0x0046, code lost:
        if (r6.mLastDH == 0) goto L13;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private synchronized void showInternal() {
        /*
            r6 = this;
            monitor-enter(r6)
            android.view.SurfaceControl r0 = r6.mSurfaceControl     // Catch: java.lang.Throwable -> L6b
            if (r0 != 0) goto L7
            monitor-exit(r6)
            return
        L7:
            com.android.server.wm.WindowManagerService r0 = r6.mWms     // Catch: java.lang.Throwable -> L6b
            r0.openSurfaceTransaction()     // Catch: java.lang.Throwable -> L6b
            com.android.server.wm.WindowManagerService r0 = r6.mWms     // Catch: java.lang.Throwable -> L6b
            android.content.Context r0 = r0.mContext     // Catch: java.lang.Throwable -> L6b
            android.content.res.Resources r0 = r0.getResources()     // Catch: java.lang.Throwable -> L6b
            r1 = 286196615(0x110f0387, float:1.1281798E-28)
            java.lang.String r0 = r0.getString(r1)     // Catch: java.lang.Throwable -> L6b
            r6.mString1 = r0     // Catch: java.lang.Throwable -> L6b
            com.android.server.wm.WindowManagerService r0 = r6.mWms     // Catch: java.lang.Throwable -> L6b
            android.content.Context r0 = r0.mContext     // Catch: java.lang.Throwable -> L6b
            android.content.res.Resources r0 = r0.getResources()     // Catch: java.lang.Throwable -> L6b
            r1 = 286196616(0x110f0388, float:1.1281799E-28)
            java.lang.String r0 = r0.getString(r1)     // Catch: java.lang.Throwable -> L6b
            r6.mString2 = r0     // Catch: java.lang.Throwable -> L6b
            android.view.SurfaceControl$Transaction r0 = android.view.SurfaceControl.getGlobalTransaction()     // Catch: java.lang.Throwable -> L62
            com.android.server.wm.WindowManagerService r1 = r6.mWms     // Catch: java.lang.Throwable -> L62
            com.android.server.wm.DisplayContent r1 = r1.getDefaultDisplayContentLocked()     // Catch: java.lang.Throwable -> L62
            android.view.DisplayInfo r2 = r1.getDisplayInfo()     // Catch: java.lang.Throwable -> L62
            int r3 = r2.logicalWidth     // Catch: java.lang.Throwable -> L62
            int r4 = r2.logicalHeight     // Catch: java.lang.Throwable -> L62
            int r5 = r6.mLastDW     // Catch: java.lang.Throwable -> L62
            if (r5 == 0) goto L4b
            int r5 = r6.mLastDH     // Catch: java.lang.Throwable -> L49
            if (r5 != 0) goto L4e
            goto L4b
        L49:
            r0 = move-exception
            goto L63
        L4b:
            r6.positionSurface(r3, r4)     // Catch: java.lang.Throwable -> L62
        L4e:
            r6.drawIfNeeded()     // Catch: java.lang.Throwable -> L62
            if (r0 == 0) goto L58
            android.view.SurfaceControl r5 = r6.mSurfaceControl     // Catch: java.lang.Throwable -> L49
            r0.show(r5)     // Catch: java.lang.Throwable -> L49
        L58:
            com.android.server.wm.WindowManagerService r0 = r6.mWms     // Catch: java.lang.Throwable -> L6b
            java.lang.String r1 = "updateTalkbackWatermark"
            r0.closeSurfaceTransaction(r1)     // Catch: java.lang.Throwable -> L6b
            monitor-exit(r6)
            return
        L62:
            r0 = move-exception
        L63:
            com.android.server.wm.WindowManagerService r1 = r6.mWms     // Catch: java.lang.Throwable -> L6b
            java.lang.String r2 = "updateTalkbackWatermark"
            r1.closeSurfaceTransaction(r2)     // Catch: java.lang.Throwable -> L6b
            throw r0     // Catch: java.lang.Throwable -> L6b
        L6b:
            r0 = move-exception
            monitor-exit(r6)
            throw r0
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wm.TalkbackWatermark.showInternal():void");
    }

    private synchronized void hideInternal() {
        Throwable th;
        if (this.mSurfaceControl == null) {
            return;
        }
        this.mWms.openSurfaceTransaction();
        try {
            SurfaceControl.Transaction transaction = SurfaceControl.getGlobalTransaction();
            if (transaction != null) {
                try {
                    transaction.hide(this.mSurfaceControl);
                } catch (Throwable th2) {
                    th = th2;
                    this.mWms.closeSurfaceTransaction("updateTalkbackWatermark");
                    throw th;
                }
            }
            this.mWms.closeSurfaceTransaction("updateTalkbackWatermark");
        } catch (Throwable th3) {
            th = th3;
        }
    }

    public synchronized void dismissInternal() {
        if (this.mSurfaceControl == null) {
            return;
        }
        Slog.d(TAG, "talkback-test dismissInternal");
        hideInternal();
        this.mSurface.destroy();
        this.mBlastBufferQueue.destroy();
        this.mWms.openSurfaceTransaction();
        this.mSurfaceControl.reparent(null);
        this.mSurfaceControl.release();
        this.mWms.closeSurfaceTransaction("updateTalkbackWatermark");
        this.mBlastBufferQueue = null;
        this.mSurfaceControl = null;
        this.mSurface = null;
        this.mHasDrawn = false;
        this.mLastDH = 0;
        this.mLastDW = 0;
    }

    public synchronized void refresh() {
        Slog.d(TAG, "talkback-test refresh");
        this.mWms.mH.post(new TalkbackWatermark$$ExternalSyntheticLambda2(this));
        this.mDrawNeeded = true;
        updateWaterMark();
    }
}
