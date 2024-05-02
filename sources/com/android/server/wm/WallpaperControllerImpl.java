package com.android.server.wm;

import android.content.Context;
import android.provider.Settings;
import android.util.Slog;
import com.miui.base.MiuiStubRegistry;
import miui.os.Build;
/* loaded from: classes.dex */
public class WallpaperControllerImpl implements WallpaperControllerStub {
    private static final boolean DEBUG = false;
    private static final String SCROLL_DESKTOP_WALLPAPER = "pref_key_wallpaper_screen_scrolled_span";
    private static final String TAG = "WallpaperControllerImpl";
    private static final float WALLPAPER_OFFSET_CENTER = 0.5f;
    private static final float WALLPAPER_OFFSET_DEFAULT = -1.0f;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<WallpaperControllerImpl> {

        /* compiled from: WallpaperControllerImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final WallpaperControllerImpl INSTANCE = new WallpaperControllerImpl();
        }

        public WallpaperControllerImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public WallpaperControllerImpl provideNewInstance() {
            throw new RuntimeException("Impl class com.android.server.wm.WallpaperControllerImpl is marked as singleton");
        }
    }

    public float getLastWallpaperX(Context context) {
        if (context != null) {
            if (!Build.IS_TABLET || Settings.Secure.getInt(context.getContentResolver(), SCROLL_DESKTOP_WALLPAPER, -1) == 1) {
                return -1.0f;
            }
            return 0.5f;
        }
        Slog.e(TAG, "getLastWallpaperX: fail, context null");
        return -1.0f;
    }
}
