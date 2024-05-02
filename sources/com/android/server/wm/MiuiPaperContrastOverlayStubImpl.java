package com.android.server.wm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.Uri;
import android.provider.MiuiSettings;
import android.provider.Settings;
import android.util.Slog;
import com.miui.base.MiuiStubRegistry;
/* loaded from: classes.dex */
public class MiuiPaperContrastOverlayStubImpl implements MiuiPaperContrastOverlayStub {
    private static final String TAG = "MiuiPaperContrastOverlayStubImpl";
    Context mContext;
    private boolean mFoldDeviceReady;
    MiuiPaperContrastOverlay mMiuiPaperSurface;
    WindowManagerService mWmService;
    private final Uri mTextureEyeCareLevelUri = Settings.System.getUriFor("screen_texture_eyecare_level");
    private final Uri mPaperModeTypeUri = Settings.System.getUriFor("screen_mode_type");
    private final Uri mPaperModeEnableUri = Settings.System.getUriFor("screen_paper_mode_enabled");
    private final Uri mSecurityModeVtbUri = Settings.Secure.getUriFor("vtb_boosting");
    private final Uri mSecurityModeGbUri = Settings.Secure.getUriFor("gb_boosting");

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<MiuiPaperContrastOverlayStubImpl> {

        /* compiled from: MiuiPaperContrastOverlayStubImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final MiuiPaperContrastOverlayStubImpl INSTANCE = new MiuiPaperContrastOverlayStubImpl();
        }

        public MiuiPaperContrastOverlayStubImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public MiuiPaperContrastOverlayStubImpl provideNewInstance() {
            return new MiuiPaperContrastOverlayStubImpl();
        }
    }

    MiuiPaperContrastOverlayStubImpl() {
    }

    public void init(WindowManagerService wms, Context context) {
        this.mContext = context;
        this.mWmService = wms;
        registerObserver(context);
        updateTextureEyeCareLevel(getSecurityCenterStatus());
    }

    public void updateTextureEyeCareWhenAodShow(boolean aodShowing) {
        updateTextureEyeCareLevel(!aodShowing && getSecurityCenterStatus());
    }

    public void updateTextureEyeCareLevel(final boolean eyecareEnable) {
        this.mWmService.mH.post(new Runnable() { // from class: com.android.server.wm.MiuiPaperContrastOverlayStubImpl$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                MiuiPaperContrastOverlayStubImpl.this.m1830x4f06bc58(eyecareEnable);
            }
        });
    }

    /* renamed from: lambda$updateTextureEyeCareLevel$0$com-android-server-wm-MiuiPaperContrastOverlayStubImpl */
    public /* synthetic */ void m1830x4f06bc58(boolean eyecareEnable) {
        synchronized (this.mWmService.mWindowMap) {
            this.mWmService.openSurfaceTransaction();
            int paperModeType = Settings.System.getIntForUser(this.mContext.getContentResolver(), "screen_mode_type", 0, -2);
            boolean paperModeEnable = MiuiSettings.System.getBooleanForUser(this.mContext.getContentResolver(), "screen_paper_mode_enabled", false, -2);
            Slog.d(TAG, "Update paper-mode, param is " + eyecareEnable);
            if (eyecareEnable && paperModeEnable && paperModeType == 1) {
                if (this.mMiuiPaperSurface == null) {
                    this.mMiuiPaperSurface = MiuiPaperContrastOverlay.getInstance(this.mWmService, this.mContext);
                }
                if (MiuiPaperContrastOverlay.IS_FOLDABLE_DEVICE && this.mFoldDeviceReady) {
                    this.mMiuiPaperSurface.changeDeviceReady();
                }
                this.mMiuiPaperSurface.changeShowLayerStatus(true);
                Slog.d(TAG, "Show paper-mode surface.");
                this.mMiuiPaperSurface.showPaperModeSurface();
            } else {
                MiuiPaperContrastOverlay miuiPaperContrastOverlay = this.mMiuiPaperSurface;
                if (miuiPaperContrastOverlay != null) {
                    miuiPaperContrastOverlay.changeShowLayerStatus(false);
                    this.mMiuiPaperSurface.hidePaperModeSurface();
                }
                this.mMiuiPaperSurface = null;
            }
            this.mWmService.closeSurfaceTransaction("updateTextureEyeCareLevel");
        }
    }

    public void registerObserver(Context context) {
        ContentObserver observer = new ContentObserver(null) { // from class: com.android.server.wm.MiuiPaperContrastOverlayStubImpl.1
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange, Uri uri) {
                boolean textureEyecareEnable = MiuiPaperContrastOverlayStubImpl.this.getSecurityCenterStatus();
                if (MiuiPaperContrastOverlayStubImpl.this.mTextureEyeCareLevelUri.equals(uri) || MiuiPaperContrastOverlayStubImpl.this.mPaperModeTypeUri.equals(uri) || MiuiPaperContrastOverlayStubImpl.this.mPaperModeEnableUri.equals(uri) || MiuiPaperContrastOverlayStubImpl.this.mSecurityModeVtbUri.equals(uri) || MiuiPaperContrastOverlayStubImpl.this.mSecurityModeGbUri.equals(uri)) {
                    Slog.d(MiuiPaperContrastOverlayStubImpl.TAG, "registerObserver uri:" + uri + ", enable:" + textureEyecareEnable);
                    MiuiPaperContrastOverlayStubImpl.this.mFoldDeviceReady = true;
                    MiuiPaperContrastOverlayStubImpl.this.updateTextureEyeCareLevel(textureEyecareEnable);
                }
            }
        };
        context.getContentResolver().registerContentObserver(this.mTextureEyeCareLevelUri, false, observer, -1);
        context.getContentResolver().registerContentObserver(this.mPaperModeTypeUri, false, observer, -1);
        context.getContentResolver().registerContentObserver(this.mPaperModeEnableUri, false, observer, -1);
        context.getContentResolver().registerContentObserver(this.mSecurityModeVtbUri, false, observer, -1);
        context.getContentResolver().registerContentObserver(this.mSecurityModeGbUri, false, observer, -1);
        observer.onChange(false);
        this.mContext.registerReceiver(new UserSwitchReceivere(), new IntentFilter("android.intent.action.USER_SWITCHED"));
    }

    public boolean getSecurityCenterStatus() {
        return Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "gb_boosting", 0, -2) == 0 && Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "vtb_boosting", 0, -2) == 0;
    }

    /* loaded from: classes.dex */
    public class UserSwitchReceivere extends BroadcastReceiver {
        private UserSwitchReceivere() {
            MiuiPaperContrastOverlayStubImpl.this = r1;
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            MiuiPaperContrastOverlayStubImpl miuiPaperContrastOverlayStubImpl = MiuiPaperContrastOverlayStubImpl.this;
            miuiPaperContrastOverlayStubImpl.updateTextureEyeCareLevel(miuiPaperContrastOverlayStubImpl.getSecurityCenterStatus());
        }
    }
}
