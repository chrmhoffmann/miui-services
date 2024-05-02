package com.android.server.wm;

import android.content.ClipData;
import android.content.ClipDescription;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Slog;
import android.view.DragEvent;
import com.android.internal.view.IDragAndDropPermissions;
import com.android.server.LocalServices;
import com.android.server.pm.UserManagerInternal;
import com.xiaomi.mirror.service.MirrorService;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.function.Consumer;
/* loaded from: classes.dex */
public class MiuiMirrorDragState {
    private static final int DRAG_FLAGS_URI_ACCESS = 3;
    private static final int DRAG_FLAGS_URI_PERMISSIONS = 195;
    private static final String TAG = "MiuiMirrorDragState";
    boolean mCrossProfileCopyAllowed;
    DisplayContent mCurrentDisplayContent;
    int mCurrentDisplayId;
    float mCurrentX;
    float mCurrentY;
    ClipData mData;
    ClipDescription mDataDescription;
    final MiuiMirrorDragDropController mDragDropController;
    boolean mDragInProgress;
    boolean mDragResult;
    int mFlags;
    private boolean mIsClosing;
    IBinder mLocalWin;
    int mPid;
    final WindowManagerService mService;
    int mSourceDisplayId;
    int mSourceUserId;
    WindowState mTargetWindow;
    IBinder mToken;
    int mUid;
    private MirrorService mMirrorService = MirrorService.get();
    ArrayList<WindowState> mNotifiedWindows = new ArrayList<>();
    ArrayList<Integer> mBroadcastDisplayIds = new ArrayList<>();

    public MiuiMirrorDragState(WindowManagerService service, MiuiMirrorDragDropController controller, int flags, IBinder localWin) {
        this.mService = service;
        this.mDragDropController = controller;
        this.mFlags = flags;
        this.mLocalWin = localWin;
    }

    public boolean isClosing() {
        return this.mIsClosing;
    }

    public void closeLocked() {
        closeLocked(false);
    }

    public void closeLocked(boolean fromDelegate) {
        this.mIsClosing = true;
        if (this.mDragInProgress) {
            broadcastDragEndedLocked();
            if (!fromDelegate) {
                this.mMirrorService.notifyDragResult(this.mDragResult);
            }
            this.mDragInProgress = false;
        }
        this.mFlags = 0;
        this.mLocalWin = null;
        this.mToken = null;
        this.mData = null;
        this.mNotifiedWindows = null;
        this.mBroadcastDisplayIds = null;
        this.mDragDropController.onDragStateClosedLocked(this);
    }

    public void prepareDragStartedLocked() {
        ClipData clipData = this.mData;
        this.mDataDescription = clipData != null ? clipData.getDescription() : null;
        this.mNotifiedWindows.clear();
        this.mBroadcastDisplayIds.clear();
        this.mDragInProgress = true;
        this.mSourceUserId = UserHandle.getUserId(this.mUid);
        UserManagerInternal userManager = (UserManagerInternal) LocalServices.getService(UserManagerInternal.class);
        this.mCrossProfileCopyAllowed = true ^ userManager.getUserRestriction(this.mSourceUserId, "no_cross_profile_copy_paste");
        this.mMirrorService.notifyDragStart(this.mUid, this.mPid, this.mData, this.mFlags);
    }

    public void broadcastDragStartedLocked(int displayId, final float touchX, final float touchY) {
        this.mCurrentDisplayId = displayId;
        this.mCurrentDisplayContent = this.mService.mRoot.getDisplayContent(displayId);
        this.mCurrentX = touchX;
        this.mCurrentY = touchY;
        if (!this.mBroadcastDisplayIds.contains(Integer.valueOf(displayId))) {
            this.mBroadcastDisplayIds.add(Integer.valueOf(displayId));
            this.mCurrentDisplayContent.forAllWindows(new Consumer() { // from class: com.android.server.wm.MiuiMirrorDragState$$ExternalSyntheticLambda0
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    MiuiMirrorDragState.this.m1761xe1418d32(touchX, touchY, (WindowState) obj);
                }
            }, false);
        }
    }

    /* renamed from: lambda$broadcastDragStartedLocked$0$com-android-server-wm-MiuiMirrorDragState */
    public /* synthetic */ void m1761xe1418d32(float touchX, float touchY, WindowState w) {
        sendDragStartedLocked(w, touchX, touchY, this.mDataDescription);
    }

    private void broadcastDragEndedLocked() {
        float y;
        float y2;
        int myPid = Process.myPid();
        Iterator<WindowState> it = this.mNotifiedWindows.iterator();
        while (it.hasNext()) {
            WindowState ws = it.next();
            if (!this.mDragResult && ws.mSession.mPid == this.mPid) {
                float x = this.mCurrentX;
                float y3 = this.mCurrentY;
                y = y3;
                y2 = x;
            } else {
                y = 0.0f;
                y2 = 0.0f;
            }
            DragEvent evt = DragEvent.obtain(4, y2, y, MiuiFreeformPinManagerService.EDGE_AREA, MiuiFreeformPinManagerService.EDGE_AREA, null, null, null, null, null, this.mDragResult);
            try {
                ws.mClient.dispatchDragEvent(evt);
            } catch (RemoteException e) {
                Slog.w(TAG, "Unable to drag-end window " + ws);
            }
            if (myPid != ws.mSession.mPid) {
                evt.recycle();
            }
        }
        this.mNotifiedWindows.clear();
        this.mBroadcastDisplayIds.clear();
    }

    private void sendDragStartedLocked(WindowState newWin, float touchX, float touchY, ClipDescription desc) {
        if (this.mDragInProgress && isValidDropTarget(newWin)) {
            DragEvent event = obtainDragEvent(newWin, 1, touchX, touchY, null, desc, null, null, false);
            try {
                try {
                    newWin.mClient.dispatchDragEvent(event);
                    this.mNotifiedWindows.add(newWin);
                    if (Process.myPid() == newWin.mSession.mPid) {
                        return;
                    }
                } catch (RemoteException e) {
                    Slog.w(TAG, "Unable to drag-start window " + newWin);
                    if (Process.myPid() == newWin.mSession.mPid) {
                        return;
                    }
                }
                event.recycle();
            } catch (Throwable th) {
                if (Process.myPid() != newWin.mSession.mPid) {
                    event.recycle();
                }
                throw th;
            }
        }
    }

    private boolean isValidDropTarget(WindowState targetWin) {
        IBinder iBinder;
        if (targetWin == null || !targetWin.isPotentialDragTarget(false)) {
            return false;
        }
        if (!targetWindowSupportsGlobalDrag(targetWin) && ((iBinder = this.mLocalWin) == null || iBinder != targetWin.mClient.asBinder())) {
            return false;
        }
        return this.mCrossProfileCopyAllowed || this.mSourceUserId == UserHandle.getUserId(targetWin.getOwningUid());
    }

    private boolean targetWindowSupportsGlobalDrag(WindowState targetWin) {
        return targetWin.mActivityRecord == null || targetWin.mActivityRecord.mTargetSdk >= 24;
    }

    public void sendDragStartedIfNeededLocked(WindowState newWin) {
        if (!this.mDragInProgress || isWindowNotified(newWin)) {
            return;
        }
        sendDragStartedLocked(newWin, this.mCurrentX, this.mCurrentY, this.mDataDescription);
    }

    private boolean isWindowNotified(WindowState newWin) {
        Iterator<WindowState> it = this.mNotifiedWindows.iterator();
        while (it.hasNext()) {
            WindowState ws = it.next();
            if (ws == newWin) {
                return true;
            }
        }
        return false;
    }

    public void notifyMoveLocked(int displayId, float x, float y) {
        if (displayId != this.mCurrentDisplayId || this.mCurrentDisplayContent == null) {
            broadcastDragStartedLocked(displayId, x, y);
        } else {
            this.mCurrentX = x;
            this.mCurrentY = y;
        }
        WindowState touchedWin = this.mCurrentDisplayContent.getTouchableWinAtPointLocked(this.mCurrentX, this.mCurrentY);
        if (touchedWin != null && !isWindowNotified(touchedWin)) {
            touchedWin = null;
        }
        try {
            int myPid = Process.myPid();
            WindowState windowState = this.mTargetWindow;
            if (touchedWin != windowState && windowState != null) {
                DragEvent evt = obtainDragEvent(windowState, 6, MiuiFreeformPinManagerService.EDGE_AREA, MiuiFreeformPinManagerService.EDGE_AREA, null, null, null, null, false);
                this.mTargetWindow.mClient.dispatchDragEvent(evt);
                if (myPid != this.mTargetWindow.mSession.mPid) {
                    evt.recycle();
                }
            }
            if (touchedWin != null) {
                DragEvent evt2 = obtainDragEvent(touchedWin, 2, this.mCurrentX, this.mCurrentY, null, null, null, null, false);
                touchedWin.mClient.dispatchDragEvent(evt2);
                if (myPid != touchedWin.mSession.mPid) {
                    evt2.recycle();
                }
            }
        } catch (RemoteException e) {
            Slog.w(TAG, "can't send drag notification to windows");
        }
        this.mTargetWindow = touchedWin;
    }

    /* JADX WARN: Code restructure failed: missing block: B:33:0x00ba, code lost:
        if (r9 != r14.mSession.mPid) goto L34;
     */
    /* JADX WARN: Code restructure failed: missing block: B:34:0x00bc, code lost:
        r2.recycle();
     */
    /* JADX WARN: Code restructure failed: missing block: B:45:0x00f2, code lost:
        if (r4 == r14.mSession.mPid) goto L46;
     */
    /* JADX WARN: Code restructure failed: missing block: B:46:0x00f5, code lost:
        r21.mToken = r3;
     */
    /* JADX WARN: Code restructure failed: missing block: B:47:0x00f7, code lost:
        return;
     */
    /* JADX WARN: Removed duplicated region for block: B:51:0x00ff  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public void notifyDropLocked(int r22, float r23, float r24) {
        /*
            Method dump skipped, instructions count: 259
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wm.MiuiMirrorDragState.notifyDropLocked(int, float, float):void");
    }

    public boolean isInProgress() {
        return this.mDragInProgress;
    }

    private static DragEvent obtainDragEvent(WindowState win, int action, float x, float y, Object localState, ClipDescription description, ClipData data, IDragAndDropPermissions dragAndDropPermissions, boolean result) {
        float winX = win.translateToWindowX(x);
        float winY = win.translateToWindowY(y);
        return DragEvent.obtain(action, winX, winY, MiuiFreeformPinManagerService.EDGE_AREA, MiuiFreeformPinManagerService.EDGE_AREA, localState, description, data, null, dragAndDropPermissions, result);
    }
}
