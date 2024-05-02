package com.xiaomi.mirror;

import android.net.Uri;
import android.view.PointerIcon;
import com.miui.base.MiuiStubRegistry;
import com.xiaomi.mirror.service.MirrorService;
/* loaded from: classes.dex */
public class MirrorServiceImpl implements MirrorServiceStub {

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<MirrorServiceImpl> {

        /* compiled from: MirrorServiceImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final MirrorServiceImpl INSTANCE = new MirrorServiceImpl();
        }

        public MirrorServiceImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public MirrorServiceImpl provideNewInstance() {
            return new MirrorServiceImpl();
        }
    }

    public boolean isEnabled() {
        MirrorService mirrorService = MirrorService.get();
        return (mirrorService == null || mirrorService.getDelegatePid() == 0) ? false : true;
    }

    public boolean isGrantAllowed(Uri uri, String targetPkg) {
        return "com.xiaomi.mirror.remoteprovider".equals(uri.getAuthority()) || targetPkg.equals(MirrorService.get().getDelegatePackageName());
    }

    public void setAllowGrant(boolean allowGrant) {
        MirrorService.get().setAllowGrant(allowGrant);
    }

    public boolean dragDropActiveLocked() {
        return MirrorService.get().getDragDropController().dragDropActiveLocked();
    }

    public void sendDragStartedIfNeededLocked(Object window) {
        MirrorService.get().getDragDropController().sendDragStartedIfNeededLocked(window);
    }

    public boolean getAllowGrant() {
        return MirrorService.get().getAllowGrant();
    }

    public void notifyPointerIconChanged(int iconId, PointerIcon customIcon) {
        MirrorService.get().notifyPointerIconChanged(iconId, customIcon);
    }
}
