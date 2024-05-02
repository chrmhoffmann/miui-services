package com.android.server.wm;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.graphics.BLASTBufferQueue;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Binder;
import android.os.Build;
import android.os.Process;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.Slog;
import android.view.Surface;
import android.view.SurfaceControl;
import com.android.server.content.MiSyncConstants;
import com.android.server.policy.MiuiPhoneWindowManager;
import com.miui.base.MiuiStubRegistry;
import miui.android.animation.controller.AnimState;
import miui.view.MiuiSecurityPermissionHandler;
/* loaded from: classes.dex */
public class MIUIWatermark implements MIUIWatermarkStub {
    private static final boolean DEBUG = false;
    private static final String TAG = "Watermark";
    private static final int degree = 30;
    private static volatile String mIMEI = null;
    private static int mTextSize = 0;
    private static final int textSize = 13;
    private String mAccountName;
    private BLASTBufferQueue mBlastBufferQueue;
    private MiuiPhoneWindowManager.MIUIWatermarkCallback mCallback;
    private boolean mDrawNeeded;
    private volatile boolean mEnableMIUIWatermark;
    private Surface mSurface;
    private SurfaceControl mSurfaceControl;
    private SurfaceControl.Transaction mTransaction;
    private WindowManagerService mWmService;
    private final Paint mTextPaint = new Paint(1);
    private int mLastDW = 0;
    private int mLastDH = 0;
    private boolean mImeiThreadisRuning = false;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<MIUIWatermark> {

        /* compiled from: MIUIWatermark$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final MIUIWatermark INSTANCE = new MIUIWatermark();
        }

        public MIUIWatermark provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public MIUIWatermark provideNewInstance() {
            return new MIUIWatermark();
        }
    }

    MIUIWatermark() {
    }

    private void doCreateSurfaceLocked() {
        Account account = loadAccountId(this.mWmService.mContext);
        this.mAccountName = account != null ? account.name : null;
        mIMEI = getImei(this.mWmService.mContext);
        this.mTextPaint.setTextSize(mTextSize);
        this.mTextPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, 0));
        this.mTextPaint.setColor(1358954495);
        this.mTextPaint.setShadowLayer(1.0f, 2.0f, 2.0f, 1342177280);
        try {
            DisplayContent dc = this.mWmService.getDefaultDisplayContentLocked();
            SurfaceControl ctrl = dc.makeOverlay().setName("MIUIWatermarkSurface").setBLASTLayer().setFormat(-3).setCallsite("MIUIWatermarkSurface").build();
            this.mTransaction.setLayer(ctrl, AnimState.VIEW_SIZE);
            this.mTransaction.setPosition(ctrl, MiuiFreeformPinManagerService.EDGE_AREA, MiuiFreeformPinManagerService.EDGE_AREA);
            this.mTransaction.show(ctrl);
            InputMonitor.setTrustedOverlayInputInfo(ctrl, this.mTransaction, dc.getDisplayId(), "MIUIWatermark");
            this.mTransaction.apply();
            this.mSurfaceControl = ctrl;
            BLASTBufferQueue bLASTBufferQueue = new BLASTBufferQueue("MIUIWatermarkSurface", this.mSurfaceControl, 1, 1, 1);
            this.mBlastBufferQueue = bLASTBufferQueue;
            this.mSurface = bLASTBufferQueue.createSurface();
        } catch (Surface.OutOfResourcesException e) {
            Log.d(TAG, "createrSurface e" + e);
        }
    }

    public void positionSurface(int dw, int dh) {
        SurfaceControl surfaceControl = this.mSurfaceControl;
        if (surfaceControl != null) {
            if (this.mLastDW != dw || this.mLastDH != dh) {
                this.mLastDW = dw;
                this.mLastDH = dh;
                this.mTransaction.setBufferSize(surfaceControl, dw, dh);
                this.mDrawNeeded = true;
            }
        }
    }

    public void showWaterMarker() {
        SurfaceControl surfaceControl = this.mSurfaceControl;
        if (surfaceControl != null) {
            this.mTransaction.show(surfaceControl);
        }
    }

    public void hideWaterMarker() {
        SurfaceControl surfaceControl = this.mSurfaceControl;
        if (surfaceControl != null) {
            this.mTransaction.hide(surfaceControl);
        }
    }

    public void updateText(Context context) {
        Account account = loadAccountId(context);
        if (account != null) {
            this.mAccountName = account.name;
        }
        String imei = getImei(context);
        if (imei != null) {
            mIMEI = imei;
        }
        this.mDrawNeeded = true;
    }

    public void drawIfNeeded() {
        String str;
        Rect dirty;
        int dw;
        int accountNameLength;
        String str2 = TAG;
        SurfaceControl surfaceControl = this.mSurfaceControl;
        if (surfaceControl == null || !this.mDrawNeeded) {
            return;
        }
        this.mDrawNeeded = false;
        int dw2 = this.mLastDW;
        int dh = this.mLastDH;
        this.mBlastBufferQueue.update(surfaceControl, dw2, dh, 1);
        Rect dirty2 = new Rect(0, 0, dw2, dh);
        Canvas c = null;
        try {
            c = this.mSurface.lockCanvas(dirty2);
        } catch (Surface.OutOfResourcesException | IllegalArgumentException e) {
            Slog.w(str2, "Failed to lock canvas", e);
        }
        if (c != null) {
            c.drawColor(0, PorterDuff.Mode.CLEAR);
            double radians = Math.toRadians(30.0d);
            int x = (int) (dw2 * 0.6d);
            int y = (int) (dh * 0.05d);
            if (dw2 > dh) {
                x = (int) (dw2 * 0.3d);
            }
            int incDeltx = (int) (mTextSize * Math.tan(radians));
            int incDelty = mTextSize * (-1);
            int columnDeltX = dw2 / 2;
            int columnDeltY = (int) (dh * 0.139d);
            int deltLine = (int) (dh * 0.24d);
            if ("cupid".equals(Build.DEVICE)) {
                y += 180;
                deltLine -= 50;
            }
            int x2 = x;
            int x3 = dh / 2;
            c.rotate(330.0f, dw2 / 2, x3);
            int i = 0;
            int accountNameLength2 = 10;
            int accountNameLength3 = y;
            int y2 = x2;
            while (i < 4) {
                String str3 = this.mAccountName;
                if (str3 != null) {
                    accountNameLength2 = str3.length();
                    dw = dw2;
                    dirty = dirty2;
                    str = str2;
                    c.drawText(this.mAccountName, y2, accountNameLength3, this.mTextPaint);
                    if (mIMEI != null) {
                        c.drawText(mIMEI, y2 - (((mIMEI.length() - accountNameLength2) * incDeltx) / 2), accountNameLength3 - incDelty, this.mTextPaint);
                    }
                } else {
                    str = str2;
                    dw = dw2;
                    dirty = dirty2;
                    if (mIMEI != null) {
                        c.drawText(mIMEI, y2 - (((mIMEI.length() - accountNameLength2) * incDeltx) / 2), accountNameLength3 - incDelty, this.mTextPaint);
                    }
                }
                String str4 = this.mAccountName;
                if (str4 != null) {
                    c.drawText(str4, y2 + columnDeltX, accountNameLength3 + columnDeltY, this.mTextPaint);
                    if (mIMEI != null) {
                        c.drawText(mIMEI, (y2 + columnDeltX) - (((mIMEI.length() - accountNameLength2) * incDeltx) / 2), (accountNameLength3 + columnDeltY) - incDelty, this.mTextPaint);
                    }
                } else if (mIMEI != null) {
                    c.drawText(mIMEI, (y2 + columnDeltX) - (((mIMEI.length() - accountNameLength2) * incDeltx) / 2), (accountNameLength3 + columnDeltY) - incDelty, this.mTextPaint);
                }
                if (dh == 0) {
                    accountNameLength = accountNameLength2;
                } else if (deltLine == 0) {
                    accountNameLength = accountNameLength2;
                } else {
                    int incDelty2 = dh / deltLine;
                    y2 = (int) (y2 - ((dh * Math.tan(radians)) / incDelty2));
                    accountNameLength3 += deltLine;
                    i++;
                    incDeltx = incDeltx;
                    dw2 = dw;
                    dirty2 = dirty;
                    str2 = str;
                    incDelty = incDelty;
                    columnDeltX = columnDeltX;
                    accountNameLength2 = accountNameLength2;
                }
                Log.d(str, "dh: " + dh + "deltLine: " + deltLine);
            }
            this.mSurface.unlockCanvasAndPost(c);
        }
    }

    private static Account loadAccountId(Context context) {
        Account[] accounts = AccountManager.get(context).getAccountsByType(MiSyncConstants.Config.XIAOMI_ACCOUNT_TYPE);
        if (accounts != null && accounts.length > 0) {
            return accounts[0];
        }
        return null;
    }

    /* JADX WARN: Type inference failed for: r1v3, types: [com.android.server.wm.MIUIWatermark$1] */
    private void getImeiInfo(final Context context) {
        String imei = getImeiID(context);
        if (imei != null) {
            setImei(imei);
            return;
        }
        synchronized (this) {
            if (this.mImeiThreadisRuning) {
                return;
            }
            this.mImeiThreadisRuning = true;
            new Thread() { // from class: com.android.server.wm.MIUIWatermark.1
                @Override // java.lang.Thread, java.lang.Runnable
                public void run() {
                    int time = 10;
                    while (true) {
                        if (time <= 0) {
                            break;
                        }
                        try {
                            try {
                                String imei2 = MIUIWatermark.getImeiID(context);
                                if (imei2 != null) {
                                    MIUIWatermark.this.setImei(imei2);
                                    break;
                                } else {
                                    sleep(2000L);
                                    time--;
                                }
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                                synchronized (MIUIWatermark.this) {
                                    MIUIWatermark.this.mImeiThreadisRuning = false;
                                    return;
                                }
                            }
                        } catch (Throwable th) {
                            synchronized (MIUIWatermark.this) {
                                MIUIWatermark.this.mImeiThreadisRuning = false;
                                throw th;
                            }
                        }
                    }
                    Slog.d(MIUIWatermark.TAG, "Failed to get imei");
                    synchronized (MIUIWatermark.this) {
                        MIUIWatermark.this.mImeiThreadisRuning = false;
                    }
                }
            }.start();
        }
    }

    public static String getImeiID(Context context) {
        TelephonyManager telephonyMgr = (TelephonyManager) context.getSystemService("phone");
        String imei = telephonyMgr == null ? null : telephonyMgr.getDeviceId();
        if (MiuiSecurityPermissionHandler.noImei() && TextUtils.isEmpty(imei)) {
            return MiuiSecurityPermissionHandler.getWifiMacAddress(context);
        }
        return imei;
    }

    public synchronized void setImei(String imei) {
        mIMEI = imei;
        this.mDrawNeeded = true;
    }

    private String getImei(Context context) {
        getImeiInfo(context);
        return mIMEI;
    }

    public synchronized void init(final WindowManagerService wms, final String msg) {
        if (Binder.getCallingPid() != Process.myPid()) {
            return;
        }
        this.mWmService = wms;
        this.mTransaction = (SurfaceControl.Transaction) wms.mTransactionFactory.get();
        if (this.mCallback == null && (wms.mPolicy instanceof MiuiPhoneWindowManager)) {
            this.mCallback = new MiuiPhoneWindowManager.MIUIWatermarkCallback() { // from class: com.android.server.wm.MIUIWatermark.2
                @Override // com.android.server.policy.MiuiPhoneWindowManager.MIUIWatermarkCallback
                public void onShowWatermark() {
                    if (MIUIWatermark.this.mSurfaceControl != null) {
                        MIUIWatermark.this.updateText(wms.mContext);
                        MIUIWatermark.this.showWaterMarker();
                        return;
                    }
                    MIUIWatermark.this.mEnableMIUIWatermark = true;
                    MIUIWatermark.this.createSurfaceLocked(msg);
                }

                @Override // com.android.server.policy.MiuiPhoneWindowManager.MIUIWatermarkCallback
                public void onHideWatermark() {
                    MIUIWatermark.this.hideWaterMarker();
                }
            };
            wms.mPolicy.registerMIUIWatermarkCallback(this.mCallback);
        }
        createSurfaceLocked(msg);
    }

    public void createSurfaceLocked(String msg) {
        if (this.mEnableMIUIWatermark && this.mSurfaceControl == null) {
            mTextSize = dp2px(this.mWmService.mContext, 13.0f);
            this.mWmService.openSurfaceTransaction();
            try {
                Slog.i(TAG, "create water mark: " + msg);
                doCreateSurfaceLocked();
            } finally {
                this.mWmService.closeSurfaceTransaction("createWatermarkInTransaction");
            }
        }
    }

    private static int dp2px(Context context, float dpValue) {
        float scale = context.getResources().getDisplayMetrics().density;
        return (int) ((dpValue * scale) + 0.5f);
    }
}
