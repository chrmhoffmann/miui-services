package com.android.server.biometrics.sensors.face;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import android.os.IHwBinder;
import android.os.Looper;
import android.os.Message;
import android.os.NativeHandle;
import android.os.RemoteException;
import android.os.SharedMemory;
import android.system.ErrnoException;
import android.util.Slog;
import android.view.Surface;
import com.android.server.MiuiFgThread;
import com.android.server.biometrics.sensors.face.IMiFaceProprietaryClientCallback;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
/* loaded from: classes.dex */
public class MiuiFaceHidl {
    private static final int ENROLL_WIDTH = 580;
    private static final int FRAME_HEIGHT = 480;
    private static final int FRAME_SIZE = 481280;
    private static final int FRAME_WIDTH = 640;
    public static final int MSG_GET_IMG_FRAME = 10001;
    private static final String TAG = "MiuiFaceHidl";
    private static Canvas mCanvas;
    private static RectF mDetectRect;
    private static RectF mEnrollRect;
    private static Surface mEnrollSurface;
    private static volatile ArrayList mFrameList;
    private static Boolean mGetFrame;
    private static volatile MiuiFaceHidl sInstance;
    ThreadPoolExecutor mExecutor;
    private Handler mHandler;
    static double scale = 1.2083333333333333d;
    private static final int ENROLL_HEIGHT = (int) (1.2083333333333333d * 640.0d);
    private IMiFace mMiuiFace = null;
    private IHwBinder.DeathRecipient mDeathRecipient = new IHwBinder.DeathRecipient() { // from class: com.android.server.biometrics.sensors.face.MiuiFaceHidl.1
        public void serviceDied(long cookie) {
            if (MiuiFaceHidl.this.mMiuiFace == null) {
                return;
            }
            try {
                MiuiFaceHidl.this.mMiuiFace.unlinkToDeath(MiuiFaceHidl.this.mDeathRecipient);
                MiuiFaceHidl.this.mMiuiFace = null;
                MiuiFaceHidl miuiFaceHidl = MiuiFaceHidl.this;
                miuiFaceHidl.mMiuiFace = miuiFaceHidl.getMiFace();
            } catch (RemoteException e) {
                e.printStackTrace();
            } catch (ErrnoException e2) {
                e2.printStackTrace();
            }
        }
    };
    private IMiFaceProprietaryClientCallback callback = new IMiFaceProprietaryClientCallback.Stub() { // from class: com.android.server.biometrics.sensors.face.MiuiFaceHidl.2
        @Override // com.android.server.biometrics.sensors.face.IMiFaceProprietaryClientCallback
        public void onFrameEvent() {
            try {
                Slog.d(MiuiFaceHidl.TAG, "callback onFrameEvent.");
                MiuiFaceHidl.this.getPreviewBuffer();
            } catch (ErrnoException e) {
                e.printStackTrace();
            }
        }
    };

    private void MiuiFaceHidl() {
    }

    public static MiuiFaceHidl getInstance() {
        if (sInstance == null) {
            synchronized (MiuiFaceHidl.class) {
                if (sInstance == null) {
                    sInstance = new MiuiFaceHidl();
                }
            }
        }
        return sInstance;
    }

    /* loaded from: classes.dex */
    public class MyHandler extends Handler {
        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        MyHandler(Looper looper) {
            super(looper);
            MiuiFaceHidl.this = this$0;
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            try {
                switch (msg.what) {
                    case MiuiFaceHidl.MSG_GET_IMG_FRAME /* 10001 */:
                        MiuiFaceHidl.showImage((Bitmap) msg.obj);
                        return;
                    default:
                        return;
                }
            } catch (Exception e) {
                Slog.e(MiuiFaceHidl.TAG, "get Image Frame failed !" + e);
            }
        }
    }

    public IMiFace getMiFace() throws RemoteException, ErrnoException {
        if (this.mMiuiFace == null) {
            this.mMiuiFace = IMiFace.getService();
            this.mHandler = new MyHandler(MiuiFgThread.getHandler().getLooper());
            this.mExecutor = new ThreadPoolExecutor(3, Integer.MAX_VALUE, 500L, TimeUnit.MILLISECONDS, new SynchronousQueue());
            mFrameList = new ArrayList();
            this.mMiuiFace.linkToDeath(this.mDeathRecipient, 0L);
        }
        return this.mMiuiFace;
    }

    public boolean getPreviewBuffer() throws ErrnoException {
        mGetFrame = true;
        startPreview();
        return true;
    }

    public boolean setDetectArea(int left, int top, int right, int bottom) throws RemoteException {
        IMiFace miFace = IMiFace.getService();
        int teeLeft = (right - left) - 290;
        int teeRight = (right - left) + 290;
        boolean res = miFace.setDetectArea(teeLeft > 0 ? teeLeft : left, top, teeRight, bottom);
        mDetectRect = new RectF(left, top, right, bottom);
        Slog.e(TAG, "setDetectArea left: " + left + ", top: " + top + ", right: " + right + ", bottom: " + bottom + ", res: " + res);
        return res;
    }

    public boolean setEnrollArea(int left, int top, int right, int bottom) throws RemoteException {
        IMiFace miFace = IMiFace.getService();
        boolean res = miFace.setEnrollArea(left, top, right, bottom);
        mEnrollRect = new RectF(left, top, right, bottom);
        Slog.e(TAG, "setEnrollArea left: " + left + ", top: " + top + ", right: " + right + ", bottom: " + bottom + ", res: " + res);
        return res;
    }

    public boolean setEnrollStep(int steps) throws RemoteException {
        IMiFace miFace = IMiFace.getService();
        boolean res = miFace.setEnrollStep(steps);
        Slog.e(TAG, "setEnrollStep steps: " + steps + ", res: " + res);
        return res;
    }

    public boolean setPreviewNotifyCallback() throws RemoteException, ErrnoException {
        this.mMiuiFace = getMiFace();
        mGetFrame = true;
        boolean res = this.mMiuiFace.setPreviewNotifyCallback(this.callback);
        return res;
    }

    public void setEnrollSurface(Surface surface) {
        mEnrollSurface = surface;
    }

    public void stopGetFrame() {
        mGetFrame = false;
        mCanvas = null;
    }

    public void startPreview() throws ErrnoException {
        this.mExecutor.execute(new Runnable() { // from class: com.android.server.biometrics.sensors.face.MiuiFaceHidl.3
            @Override // java.lang.Runnable
            public void run() {
                Exception e;
                try {
                    if (MiuiFaceHidl.mGetFrame.booleanValue()) {
                        byte[] value = new byte[MiuiFaceHidl.FRAME_SIZE];
                        byte[] saved = MiuiFaceHidl.intToBytesLittle(1);
                        System.arraycopy(saved, 0, value, 4, saved.length);
                        int length = saved.length;
                        byte[] signed = MiuiFaceHidl.intToBytesLittle(1);
                        System.arraycopy(signed, 0, value, 8, signed.length);
                        byte[] pos = MiuiFaceHidl.intToBytesLittle(signed.length + length);
                        System.arraycopy(pos, 0, value, 0, pos.length);
                        SharedMemory sm = MiuiFaceHidl.byteArr2Sm(value);
                        FileDescriptor fd = sm.getFileDescriptor();
                        NativeHandle handle = new NativeHandle(fd, false);
                        IMiFace miFace = MiuiFaceHidl.this.getMiFace();
                        boolean res = miFace.getPreviewBuffer(handle);
                        Slog.e(MiuiFaceHidl.TAG, "startPreview！");
                        if (!res) {
                            return;
                        }
                        byte[] img = MiuiFaceHidl.sm2Byte(sm, MiuiFaceHidl.FRAME_SIZE);
                        Bitmap rotateBitMap = MiuiFaceHidl.rotateBitmap(-90, MiuiFaceHidl.nv12ToBitmap(img, MiuiFaceHidl.FRAME_WIDTH, MiuiFaceHidl.FRAME_HEIGHT));
                        MiuiFaceHidl.showImage(rotateBitMap);
                        Message msg = Message.obtain();
                        msg.what = MiuiFaceHidl.MSG_GET_IMG_FRAME;
                        msg.obj = rotateBitMap;
                        try {
                            if (!MiuiFaceHidl.this.mHandler.hasMessages(MiuiFaceHidl.MSG_GET_IMG_FRAME)) {
                                MiuiFaceHidl.this.mHandler.sendMessage(msg);
                            }
                            sm.close();
                            MiuiFaceHidl.mGetFrame = false;
                        } catch (Exception e2) {
                            e = e2;
                            Slog.e(MiuiFaceHidl.TAG, "get image frame from sharedmemory failed! e = " + e);
                        }
                    }
                } catch (Exception e3) {
                    e = e3;
                }
            }
        });
    }

    public static void showImage(Bitmap bitmap) {
        synchronized (mFrameList) {
            if (!mGetFrame.booleanValue()) {
                return;
            }
            if (mDetectRect == null) {
                mDetectRect = new RectF(70.0f, 156.0f, 400.0f, 500.0f);
            }
            Canvas lockCanvas = mEnrollSurface.lockCanvas(new Rect(0, 0, ENROLL_WIDTH, 500));
            mCanvas = lockCanvas;
            lockCanvas.setDensity(440);
            mCanvas.drawColor(0, PorterDuff.Mode.CLEAR);
            mCanvas.drawBitmap(bitmap, new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight()), new Rect(0, 0, ENROLL_WIDTH, (int) (ENROLL_HEIGHT * 0.6f)), new Paint());
            mEnrollSurface.unlockCanvasAndPost(mCanvas);
        }
    }

    public static void saveBitmap(Bitmap bitmap, String name) {
        if (bitmap == null) {
            return;
        }
        String bitmapFileName = System.currentTimeMillis() + ".png";
        try {
            File file = new File("/data/system/files/", bitmapFileName);
            if (!file.exists()) {
                file.createNewFile();
            }
            Slog.d(TAG, "path: " + file.getAbsolutePath() + " " + file.getName());
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void saveFile(byte[] bitmap, String name) {
        if (bitmap == null) {
            return;
        }
        String fileName = System.currentTimeMillis() + "_" + name + ".yuv";
        try {
            File file = new File("/data/system/files/", fileName);
            if (!file.exists()) {
                file.createNewFile();
            }
            Slog.d(TAG, "path: " + file.getAbsolutePath() + " " + file.getName());
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(bitmap);
            fos.flush();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Bitmap rotateBitmap(int angle, Bitmap bitmap) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        matrix.postScale(-1.0f, 1.0f);
        double d = scale;
        matrix.postScale((float) d, (float) d);
        Bitmap resizedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        if (resizedBitmap != bitmap && bitmap != null && !bitmap.isRecycled()) {
            bitmap.recycle();
        }
        return resizedBitmap;
    }

    public static byte[] intToByte(int value) {
        byte v2 = (byte) (value & 255);
        byte v2_2 = (byte) ((value >> 8) & 255);
        byte v2_3 = (byte) ((value >> 16) & 255);
        byte[] b = {v2, v2_2, v2_3, (byte) ((value >> 24) & 255)};
        return b;
    }

    public static byte[] intToBytesBig(int value) {
        byte v2 = (byte) ((value >> 24) & 255);
        byte v2_2 = (byte) ((value >> 16) & 255);
        byte v2_3 = (byte) ((value >> 8) & 255);
        byte[] src = {v2, v2_2, v2_3, (byte) (value & 255)};
        return src;
    }

    public static byte[] intToBytesLittle(int value) {
        byte v2 = (byte) ((value >> 24) & 255);
        byte[] src = {(byte) (value & 255), v2_3, v2_2, v2};
        byte v2_2 = (byte) ((value >> 16) & 255);
        byte v2_3 = (byte) ((value >> 8) & 255);
        return src;
    }

    public static SharedMemory byteArr2Sm(byte[] msg) {
        try {
            SharedMemory sharedMemory = SharedMemory.create("miuifacehidl", 524288);
            ByteBuffer buffer = sharedMemory.mapReadWrite();
            buffer.putInt(msg.length);
            buffer.put(msg);
            return sharedMemory;
        } catch (Exception e) {
            Slog.e(TAG, "str2Sm error ! e = ", e);
            return null;
        }
    }

    public static byte[] sm2Byte(SharedMemory sm, int len) {
        byte[] buffArr = null;
        try {
            ByteBuffer buffer = sm.mapReadWrite();
            buffArr = new byte[len];
            buffer.get(buffArr, 0, len);
            return buffArr;
        } catch (Exception e) {
            Slog.e(TAG, "sm2Byte error ! e = " + e);
            return buffArr;
        }
    }

    public static Bitmap nv12ToBitmap(byte[] data, int w, int h) {
        return spToBitmap(data, w, h, 0, 1);
    }

    public static Bitmap nv21ToBitmap(byte[] data, int w, int h) {
        return spToBitmap(data, w, h, 1, 0);
    }

    private static Bitmap spToBitmap(byte[] data, int w, int h, int uOff, int vOff) {
        int i;
        int i2;
        int yPos = w * h;
        int[] colors = new int[yPos];
        int yPos2 = 0;
        int uvPos = yPos;
        int j = 0;
        while (j < h) {
            int i3 = 0;
            while (i3 < w) {
                int y1 = data[yPos2] & 255;
                int u = (data[uvPos + uOff] & 255) - 128;
                int v = (data[uvPos + vOff] & 255) - 128;
                int y1192 = y1 * 1192;
                int r = (v * 1634) + y1192;
                int g = (y1192 - (v * 833)) - (u * 400);
                int b = (u * 2066) + y1192;
                int b2 = 262143;
                if (r < 0) {
                    i = 0;
                } else {
                    i = r > 262143 ? 262143 : r;
                }
                int r2 = i;
                if (g < 0) {
                    i2 = 0;
                } else {
                    i2 = g > 262143 ? 262143 : g;
                }
                int g2 = i2;
                if (b < 0) {
                    b2 = 0;
                } else if (b <= 262143) {
                    b2 = b;
                }
                int plane = yPos;
                int plane2 = b2 >> 10;
                colors[yPos2] = (plane2 & 255) | ((r2 << 6) & 16711680) | ((g2 >> 2) & 65280);
                int yPos3 = yPos2 + 1;
                if ((yPos2 & 1) == 1) {
                    uvPos += 2;
                }
                i3++;
                yPos2 = yPos3;
                yPos = plane;
            }
            int plane3 = yPos;
            int plane4 = j & 1;
            if (plane4 == 0) {
                uvPos -= w;
            }
            j++;
            yPos = plane3;
        }
        return Bitmap.createBitmap(colors, w, h, Bitmap.Config.RGB_565);
    }

    public static String byteToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder("");
        for (byte b : bytes) {
            String v2 = Integer.toHexString(b & 255);
            String v3 = v2.length() == 1 ? "0" + v2 : v2;
            sb.append(v3);
        }
        return sb.toString().toUpperCase().trim();
    }
}
