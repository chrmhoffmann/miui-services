package com.android.server.wm;

import android.content.ClipData;
import android.util.Slog;
import android.view.SurfaceControl;
import com.miui.base.MiuiStubRegistry;
import com.xiaomi.mirror.service.MirrorService;
/* loaded from: classes.dex */
public class DragDropControllerImpl implements DragDropControllerStub {
    private static final String TAG = DragDropControllerImpl.class.getSimpleName();
    private DragDropController mDragDropController;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<DragDropControllerImpl> {

        /* compiled from: DragDropControllerImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final DragDropControllerImpl INSTANCE = new DragDropControllerImpl();
        }

        public DragDropControllerImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public DragDropControllerImpl provideNewInstance() {
            return new DragDropControllerImpl();
        }
    }

    public void initDragDropController(DragDropController dragDropController) {
        this.mDragDropController = dragDropController;
    }

    public void notifyDragStart(ClipData data, int uid, int pid, int flag) {
        MirrorService.get().notifyDragStart(data, uid, pid, flag);
    }

    public void notifyDragFinish(boolean dragResult) {
        DragState dragState = this.mDragDropController.getDragState();
        String packageName = "";
        if (dragState.mDropWindow != null) {
            packageName = dragState.mDropWindow.getOwningPackage();
        }
        MirrorService.get().notifyDragFinish(packageName, dragResult);
    }

    public boolean setDragSurfaceVisible(boolean visible) {
        DragState dragState = this.mDragDropController.getDragState();
        if (dragState == null) {
            return false;
        }
        try {
            SurfaceControl.Transaction transaction = new SurfaceControl.Transaction();
            if (visible) {
                transaction.show(dragState.mSurfaceControl);
            } else {
                transaction.hide(dragState.mSurfaceControl);
            }
            transaction.apply();
            transaction.close();
            return true;
        } catch (Exception e) {
            Slog.e(TAG, e.getMessage());
            return false;
        }
    }

    public boolean cancelCurrentDrag() {
        DragState dragState = this.mDragDropController.getDragState();
        if (dragState == null) {
            return false;
        }
        this.mDragDropController.cancelDragAndDrop(dragState.mToken, true);
        return true;
    }

    public void notifyDragDropResultToOneTrack(WindowState dragWindow, WindowState dropWindow, boolean result) {
        OneTrackDragDropHelper.getInstance().notifyDragDropResult(dragWindow, dropWindow, result);
    }
}
