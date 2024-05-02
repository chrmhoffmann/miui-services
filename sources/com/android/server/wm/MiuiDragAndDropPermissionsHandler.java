package com.android.server.wm;

import android.content.ClipData;
import android.net.Uri;
import android.os.IBinder;
import android.os.RemoteException;
import java.util.ArrayList;
import java.util.List;
/* loaded from: classes.dex */
class MiuiDragAndDropPermissionsHandler extends DragAndDropPermissionsHandler {
    private OnPermissionReleaseListener mOnPermissionReleaseListener;
    private OnPermissionTakeListener mOnPermissionTakeListener;
    private List<Uri> mUris;

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public interface OnPermissionReleaseListener {
        void onPermissionRelease(List<Uri> list);
    }

    /* loaded from: classes.dex */
    interface OnPermissionTakeListener {
        void onPermissionTake(List<Uri> list);
    }

    public MiuiDragAndDropPermissionsHandler(WindowManagerGlobalLock lock, ClipData clipData, int sourceUid, String targetPackage, int mode, int sourceUserId, int targetUserId) {
        super(lock, clipData, sourceUid, targetPackage, mode, sourceUserId, targetUserId);
        ArrayList arrayList = new ArrayList();
        this.mUris = arrayList;
        clipData.collectUris(arrayList);
    }

    public void take(IBinder activityToken) throws RemoteException {
        super.take(activityToken);
        OnPermissionTakeListener onPermissionTakeListener = this.mOnPermissionTakeListener;
        if (onPermissionTakeListener != null) {
            onPermissionTakeListener.onPermissionTake(this.mUris);
        }
    }

    public void release() throws RemoteException {
        super.release();
        OnPermissionReleaseListener onPermissionReleaseListener = this.mOnPermissionReleaseListener;
        if (onPermissionReleaseListener != null) {
            onPermissionReleaseListener.onPermissionRelease(this.mUris);
        }
    }

    public void setOnPermissionTakeListener(OnPermissionTakeListener listener) {
        this.mOnPermissionTakeListener = listener;
    }

    public void setOnPermissionReleaseListener(OnPermissionReleaseListener listener) {
        this.mOnPermissionReleaseListener = listener;
    }
}
