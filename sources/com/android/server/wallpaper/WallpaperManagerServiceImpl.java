package com.android.server.wallpaper;

import android.app.IWallpaperManagerCallback;
import android.app.WallpaperInfo;
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.Binder;
import android.os.Environment;
import android.os.FileUtils;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.SELinux;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Slog;
import com.android.server.wallpaper.WallpaperManagerService;
import com.miui.base.MiuiStubRegistry;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
/* loaded from: classes.dex */
public class WallpaperManagerServiceImpl implements WallpaperManagerServiceStub {
    private static final int FRAMEWORK_VERSION_TO_WALLPAPER = 1;
    private static final String KEY_FRAMEWORK_VERSION_TO_WALLPAPER = "framework_version_to_wallpaper";
    private static final List<String> MIUI_WALLPAPER_COMPONENTS;
    private static final int MSG_CREATE_BLURWALLPAPER = 1;
    private static final List<String> SUPER_WALLPAPER_PACKAGE_NAMES;
    private static final String TAG = "WallpaperManagerServiceImpl";
    private Handler blurWallpaperHandler;
    private HandlerThread blurWallpaperThread;
    private Context mContext;
    private WallpaperManagerService mService;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<WallpaperManagerServiceImpl> {

        /* compiled from: WallpaperManagerServiceImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final WallpaperManagerServiceImpl INSTANCE = new WallpaperManagerServiceImpl();
        }

        public WallpaperManagerServiceImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public WallpaperManagerServiceImpl provideNewInstance() {
            return new WallpaperManagerServiceImpl();
        }
    }

    static {
        ArrayList arrayList = new ArrayList();
        MIUI_WALLPAPER_COMPONENTS = arrayList;
        ArrayList arrayList2 = new ArrayList();
        SUPER_WALLPAPER_PACKAGE_NAMES = arrayList2;
        arrayList.add("ComponentInfo{com.miui.miwallpaper/com.miui.miwallpaper.wallpaperservice.ImageWallpaper}");
        arrayList2.add("com.miui.miwallpaper.snowmountain");
        arrayList2.add("com.miui.miwallpaper.geometry");
        arrayList2.add("com.miui.miwallpaper.saturn");
        arrayList2.add("com.miui.miwallpaper.earth");
        arrayList2.add("com.miui.miwallpaper.mars");
    }

    public boolean isMiuiWallpaperComponent(ComponentName componentName) {
        return MIUI_WALLPAPER_COMPONENTS.contains(componentName.toString());
    }

    public boolean isSuperWallpaper(String packageName) {
        return SUPER_WALLPAPER_PACKAGE_NAMES.contains(packageName);
    }

    public void setWallpaperManagerService(WallpaperManagerService service, Context context) {
        this.mService = service;
        this.mContext = context;
    }

    public void handleWallpaperObserverEvent(WallpaperManagerService.WallpaperData wallpaper) {
        if (this.mService == null) {
            return;
        }
        Slog.v(TAG, "handleWallpaperObserverEvent");
        this.blurWallpaperHandler.removeMessages(1);
        Message message = Message.obtain(this.blurWallpaperHandler, 1, wallpaper);
        message.sendToTarget();
    }

    /* JADX WARN: Code restructure failed: missing block: B:41:0x00e3, code lost:
        if (r0.isRecycled() != false) goto L42;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public void createBlurWallpaper(boolean r12) {
        /*
            Method dump skipped, instructions count: 302
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wallpaper.WallpaperManagerServiceImpl.createBlurWallpaper(boolean):void");
    }

    private void saveBlurWallpaperBitmapLocked(Bitmap blurImg, String fileName) {
        Slog.d(TAG, "saveBlurWallpaperBitmapLocked begin");
        long timeBegin = System.currentTimeMillis();
        try {
            File dir = Environment.getUserSystemDirectory(getWallpaperUserId());
            if (!dir.exists() && dir.mkdir()) {
                FileUtils.setPermissions(dir.getPath(), 505, -1, -1);
            }
            File file = new File(dir, fileName);
            ParcelFileDescriptor fd = ParcelFileDescriptor.open(file, 939524096);
            if (SELinux.restorecon(file)) {
                ParcelFileDescriptor.AutoCloseOutputStream mWallpaperStream = new ParcelFileDescriptor.AutoCloseOutputStream(fd);
                blurImg.compress(Bitmap.CompressFormat.PNG, 90, mWallpaperStream);
                try {
                    mWallpaperStream.close();
                } catch (IOException e) {
                }
                if (fd != null) {
                    try {
                        fd.close();
                    } catch (IOException e2) {
                    }
                }
                long takenTime = System.currentTimeMillis() - timeBegin;
                Slog.d(TAG, "saveBlurWallpaperBitmapLocked takenTime = " + takenTime);
                try {
                    mWallpaperStream.close();
                } catch (IOException e3) {
                }
                if (fd != null) {
                    try {
                        fd.close();
                    } catch (IOException e4) {
                    }
                }
            }
            if (fd != null) {
                try {
                    fd.close();
                } catch (IOException e5) {
                }
            }
        } catch (FileNotFoundException e6) {
            Slog.w(TAG, "Error setting wallpaper", e6);
        }
    }

    /* JADX WARN: Unsupported multi-entry loop pattern (BACK_EDGE: B:11:0x0025 -> B:24:0x003f). Please submit an issue!!! */
    private Bitmap getDefaultWallpaperLocked(Point size) {
        Context context = this.mContext;
        if (context == null) {
            return null;
        }
        InputStream is = WallpaperManager.openDefaultWallpaper(context, 1);
        Bitmap mBitmap = null;
        try {
        } catch (IOException e) {
            Slog.w(TAG, "Error getting wallpaper", e);
        }
        if (is != null) {
            try {
                try {
                    mBitmap = scaleWallpaperBitmapToScreenSize(BitmapFactory.decodeStream(is, null, new BitmapFactory.Options()), size);
                    is.close();
                } catch (OutOfMemoryError e2) {
                    Slog.w(TAG, "Can't decode stream", e2);
                    mBitmap = null;
                    is.close();
                }
            } catch (Throwable th) {
                try {
                    is.close();
                } catch (IOException e3) {
                    Slog.w(TAG, "Error getting wallpaper", e3);
                }
                throw th;
            }
        }
        return mBitmap;
    }

    /* JADX WARN: Unsupported multi-entry loop pattern (BACK_EDGE: B:9:0x0025 -> B:14:0x0035). Please submit an issue!!! */
    private Bitmap getCurrentWallpaperLocked(Point size) {
        Bitmap mBitmap = null;
        ParcelFileDescriptor fd = getWallpaper();
        try {
        } catch (IOException e) {
            Slog.w(TAG, "Error getting wallpaper", e);
        }
        if (fd == null) {
            return null;
        }
        try {
            try {
                BitmapFactory.Options options = new BitmapFactory.Options();
                mBitmap = scaleWallpaperBitmapToScreenSize(BitmapFactory.decodeFileDescriptor(fd.getFileDescriptor(), null, options), size);
                fd.close();
            } catch (OutOfMemoryError e2) {
                Slog.w(TAG, "Can't decode file", e2);
                fd.close();
            }
            return mBitmap;
        } catch (Throwable th) {
            try {
                fd.close();
            } catch (IOException e3) {
                Slog.w(TAG, "Error getting wallpaper", e3);
            }
            throw th;
        }
    }

    private ParcelFileDescriptor getWallpaper() {
        ParcelFileDescriptor mParcelFile;
        File wallpaperFile;
        WallpaperManagerService wallpaperManagerService = this.mService;
        if (wallpaperManagerService == null) {
            return null;
        }
        synchronized (wallpaperManagerService.getLock()) {
            int wallpaperUserId = getWallpaperUserId();
            try {
                wallpaperFile = new File(Environment.getUserSystemDirectory(wallpaperUserId), "wallpaper");
            } catch (FileNotFoundException e) {
                Slog.w(TAG, "Error getting wallpaper", e);
                mParcelFile = null;
            }
            if (!wallpaperFile.exists()) {
                return null;
            }
            mParcelFile = ParcelFileDescriptor.open(wallpaperFile, 268435456);
            return mParcelFile;
        }
    }

    private Bitmap scaleWallpaperBitmapToScreenSize(Bitmap bitmap, Point size) {
        if (bitmap == null) {
            return null;
        }
        Matrix m = new Matrix();
        int bitmap_height = bitmap.getHeight();
        int bitmap_width = bitmap.getWidth();
        int bitmapLongSideLength = Math.max(bitmap_height, bitmap_width);
        int bitmapShortSideLength = Math.min(bitmap_height, bitmap_width);
        int longSideLength = Math.max(size.x, size.y);
        int shortSideLength = Math.min(size.x, size.y);
        if (longSideLength != 0 && shortSideLength != 0 && bitmapLongSideLength != longSideLength) {
            if (bitmapLongSideLength == shortSideLength * 2 && bitmapShortSideLength == longSideLength) {
                return bitmap;
            }
            int sidelength = (((float) bitmapLongSideLength) / ((float) shortSideLength)) / (((float) bitmapShortSideLength) / ((float) longSideLength)) == 2.0f ? longSideLength * 2 : longSideLength;
            float scale = sidelength / bitmapLongSideLength;
            m.setScale(scale, scale);
            Bitmap bmp = Bitmap.createBitmap(bitmap, 0, 0, bitmap_width, bitmap_height, m, true);
            return bmp;
        }
        return bitmap;
    }

    private Drawable loadThumbnailWithoutTheme(WallpaperInfo info, PackageManager pm) {
        int thumbnailResource = 0;
        if (info != null) {
            thumbnailResource = info.getThumbnailResource();
        }
        if (thumbnailResource < 0 || info == null) {
            return null;
        }
        String packageName = info.getPackageName();
        Drawable dr = pm.getDrawable(packageName, thumbnailResource, info.getServiceInfo().applicationInfo);
        return dr;
    }

    public ParcelFileDescriptor getBlurWallpaper(IWallpaperManagerCallback cb) {
        WallpaperManagerService wallpaperManagerService = this.mService;
        if (wallpaperManagerService == null) {
            return null;
        }
        synchronized (wallpaperManagerService.getLock()) {
            int wallpaperUserId = getWallpaperUserId();
            WallpaperManagerService.WallpaperData wallpaper = (WallpaperManagerService.WallpaperData) this.mService.getWallpaperMap().get(wallpaperUserId);
            if (wallpaper == null || wallpaper.getCallbacks() == null) {
                return null;
            }
            if (cb != null && !wallpaper.getCallbacks().isContainIBinder(cb)) {
                wallpaper.getCallbacks().register(cb);
            }
            try {
                File f = new File(Environment.getUserSystemDirectory(wallpaperUserId), "blurwallpaper");
                if (!f.exists()) {
                    return null;
                }
                ParcelFileDescriptor mParcelFile = ParcelFileDescriptor.open(f, 268435456);
                return mParcelFile;
            } catch (FileNotFoundException e) {
                Slog.w(TAG, "Error getting wallpaper", e);
                return null;
            }
        }
    }

    public void wallpaperManagerServiceReady() {
        createBlurWallpaper(false);
        Slog.i(TAG, "create blur wallpaper");
    }

    public int getWallpaperUserId() {
        WallpaperManagerService wallpaperManagerService;
        int callingUid = Binder.getCallingUid();
        if (callingUid == 1000 && (wallpaperManagerService = this.mService) != null) {
            return wallpaperManagerService.getCurrentUserId();
        }
        return UserHandle.getUserId(callingUid);
    }

    public void notifyBlurCallbacksLocked(WallpaperManagerService.WallpaperData wallpaper) {
        RemoteCallbackList<IWallpaperManagerCallback> callbacks = wallpaper.getCallbacks();
        int n = callbacks.beginBroadcast();
        for (int i = 0; i < n; i++) {
            try {
                callbacks.getBroadcastItem(i).onWallpaperChanged();
            } catch (RemoteException e) {
            }
        }
        callbacks.finishBroadcast();
    }

    public void initBlurWallpaperThread() {
        if (this.blurWallpaperThread != null) {
            return;
        }
        HandlerThread handlerThread = new HandlerThread("BlurWallpaperThread");
        this.blurWallpaperThread = handlerThread;
        handlerThread.start();
        this.blurWallpaperHandler = new Handler(this.blurWallpaperThread.getLooper()) { // from class: com.android.server.wallpaper.WallpaperManagerServiceImpl.1
            @Override // android.os.Handler
            public void handleMessage(Message msg) {
                WallpaperManagerService.WallpaperData wallpaper = (WallpaperManagerService.WallpaperData) msg.obj;
                if (wallpaper == null) {
                    return;
                }
                Slog.d(WallpaperManagerServiceImpl.TAG, "onEvent createBlurBitmap Blur wallpaper Begin");
                long timeBegin = System.currentTimeMillis();
                WallpaperManagerServiceImpl.this.createBlurWallpaper(true);
                long takenTime = System.currentTimeMillis() - timeBegin;
                Slog.d(WallpaperManagerServiceImpl.TAG, "onEvent createBlurBitmap Blur takenTime = " + takenTime);
                synchronized (WallpaperManagerServiceImpl.this.mService.getLock()) {
                    WallpaperManagerServiceImpl.this.notifyBlurCallbacksLocked(wallpaper);
                }
            }
        };
    }

    public void checkAndConfigFrameworkVersionToWallpaper(Context context) {
        if (context == null) {
            Slog.e(TAG, "checkAndConfigFrameworkVersionToWallpaper: context null");
            return;
        }
        int version = Settings.Global.getInt(context.getContentResolver(), KEY_FRAMEWORK_VERSION_TO_WALLPAPER, -1);
        if (version != 1) {
            Settings.Global.putInt(context.getContentResolver(), KEY_FRAMEWORK_VERSION_TO_WALLPAPER, 1);
        }
    }
}
