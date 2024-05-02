package com.android.server.wm;

import android.content.ClipData;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.ServiceManager;
import android.util.Slog;
import android.view.IWindow;
import com.xiaomi.mirror.service.MirrorService;
/* loaded from: classes.dex */
public class MiuiMirrorDragDropController {
    private static final long DRAG_TIMEOUT_MS = 5000;
    static final int MSG_DRAG_END_TIMEOUT = 0;
    private static final String TAG = "MiuiMirrorDragDrop";
    private MiuiMirrorDragState mDragState;
    private final Handler mHandler;
    private WindowManagerService mService = ServiceManager.getService("window");

    public boolean dragDropActiveLocked() {
        MiuiMirrorDragState miuiMirrorDragState = this.mDragState;
        return miuiMirrorDragState != null && !miuiMirrorDragState.isClosing();
    }

    public int lastTargetUid() {
        MiuiMirrorDragState miuiMirrorDragState = this.mDragState;
        if (miuiMirrorDragState == null || miuiMirrorDragState.mTargetWindow == null) {
            return -1;
        }
        return this.mDragState.mTargetWindow.getOwningUid();
    }

    public MiuiMirrorDragDropController() {
        WindowManagerService windowManagerService = this.mService;
        this.mHandler = new DragHandler(windowManagerService, windowManagerService.mH.getLooper());
    }

    public void sendDragStartedIfNeededLocked(Object window) {
        this.mDragState.sendDragStartedIfNeededLocked((WindowState) window);
    }

    public IBinder performDrag(int callerPid, int callerUid, IWindow window, int flags, int displayId, ClipData data) {
        IBinder winBinder;
        IBinder dragToken = new Binder();
        synchronized (this.mService.mGlobalLock) {
            if (dragDropActiveLocked()) {
                Slog.w(TAG, "Mirror drag already in progress");
                MiuiMirrorDragState miuiMirrorDragState = this.mDragState;
                if (miuiMirrorDragState != null && !miuiMirrorDragState.isInProgress()) {
                    this.mDragState.closeLocked();
                }
                return null;
            } else if (this.mService.mDragDropController.dragDropActiveLocked()) {
                Slog.w(TAG, "Normal drag in progress");
                MiuiMirrorDragState miuiMirrorDragState2 = this.mDragState;
                if (miuiMirrorDragState2 != null && !miuiMirrorDragState2.isInProgress()) {
                    this.mDragState.closeLocked();
                }
                return null;
            } else {
                if (callerPid == MirrorService.get().getDelegatePid()) {
                    Slog.d(TAG, "drag from mirror");
                    winBinder = null;
                } else {
                    WindowState callingWin = this.mService.windowForClientLocked((Session) null, window, false);
                    if (callingWin == null) {
                        Slog.w(TAG, "Bad requesting window " + window);
                        MiuiMirrorDragState miuiMirrorDragState3 = this.mDragState;
                        if (miuiMirrorDragState3 != null && !miuiMirrorDragState3.isInProgress()) {
                            this.mDragState.closeLocked();
                        }
                        return null;
                    }
                    winBinder = window.asBinder();
                }
                MiuiMirrorDragState miuiMirrorDragState4 = new MiuiMirrorDragState(this.mService, this, flags, winBinder);
                this.mDragState = miuiMirrorDragState4;
                miuiMirrorDragState4.mPid = callerPid;
                this.mDragState.mUid = callerUid;
                this.mDragState.mToken = dragToken;
                this.mDragState.mSourceDisplayId = displayId;
                this.mDragState.mData = data;
                this.mDragState.prepareDragStartedLocked();
                MiuiMirrorDragState miuiMirrorDragState5 = this.mDragState;
                if (miuiMirrorDragState5 != null && !miuiMirrorDragState5.isInProgress()) {
                    this.mDragState.closeLocked();
                }
                return dragToken;
            }
        }
    }

    public void reportDropResult(int pid, IWindow window, boolean consumed) {
        IBinder token = window == null ? null : window.asBinder();
        boolean fromDelegate = pid == MirrorService.get().getDelegatePid();
        synchronized (this.mService.mGlobalLock) {
            MiuiMirrorDragState miuiMirrorDragState = this.mDragState;
            if (miuiMirrorDragState == null) {
                Slog.w(TAG, "Drop result given but no drag in progress");
                return;
            }
            if (token != null) {
                if (miuiMirrorDragState.mToken != token) {
                    Slog.w(TAG, "Invalid drop-result claim by " + window);
                    throw new IllegalStateException("reportDropResult() by non-recipient");
                }
                this.mHandler.removeMessages(0, window.asBinder());
                WindowState callingWin = this.mService.windowForClientLocked((Session) null, window, false);
                if (callingWin != null && callingWin.getTask() != null) {
                    if (!consumed) {
                        consumed = MirrorService.get().tryToShareDrag(callingWin.getOwningPackage(), callingWin.getTask().mTaskId, this.mDragState.mData);
                    }
                }
                Slog.w(TAG, "Bad result-reporting window " + window);
                this.mDragState.closeLocked(fromDelegate);
                return;
            } else if (!fromDelegate) {
                Slog.w(TAG, "Try to report a result without a delegate");
                return;
            }
            this.mDragState.mDragResult = consumed;
            this.mDragState.closeLocked(fromDelegate);
        }
    }

    public void shutdownDragAndDropIfNeeded() {
        synchronized (this.mService.mGlobalLock) {
            MiuiMirrorDragState miuiMirrorDragState = this.mDragState;
            if (miuiMirrorDragState != null && miuiMirrorDragState.isInProgress()) {
                this.mDragState.mDragResult = false;
                this.mDragState.closeLocked();
            }
        }
    }

    public void cancelDragAndDrop(IBinder dragToken) {
        synchronized (this.mService.mGlobalLock) {
            MiuiMirrorDragState miuiMirrorDragState = this.mDragState;
            if (miuiMirrorDragState == null) {
                Slog.w(TAG, "cancelDragAndDrop() without prepareDrag()");
                throw new IllegalStateException("cancelDragAndDrop() without prepareDrag()");
            } else if (miuiMirrorDragState.mToken != dragToken) {
                Slog.w(TAG, "cancelDragAndDrop() does not match prepareDrag()");
                throw new IllegalStateException("cancelDragAndDrop() does not match prepareDrag()");
            } else {
                this.mDragState.mDragResult = false;
                this.mDragState.closeLocked();
            }
        }
    }

    public void injectDragEvent(int action, int displayId, float newX, float newY) {
        synchronized (this.mService.mGlobalLock) {
            if (!dragDropActiveLocked()) {
                return;
            }
            switch (action) {
                case 2:
                    this.mDragState.notifyMoveLocked(displayId, newX, newY);
                    break;
                case 3:
                    this.mDragState.notifyDropLocked(displayId, newX, newY);
                    break;
                case 4:
                    this.mDragState.closeLocked();
                    break;
                case 5:
                    this.mDragState.broadcastDragStartedLocked(displayId, newX, newY);
                    break;
                case 6:
                    this.mDragState.notifyMoveLocked(displayId, -1.0f, -1.0f);
                    break;
            }
        }
    }

    void sendHandlerMessage(int what, Object arg) {
        this.mHandler.obtainMessage(what, arg).sendToTarget();
    }

    public void sendTimeoutMessage(int what, Object arg) {
        this.mHandler.removeMessages(what, arg);
        Message msg = this.mHandler.obtainMessage(what, arg);
        this.mHandler.sendMessageDelayed(msg, DRAG_TIMEOUT_MS);
    }

    public void onDragStateClosedLocked(MiuiMirrorDragState dragState) {
        if (this.mDragState != dragState) {
            Slog.wtf(TAG, "Unknown drag state is closed");
        } else {
            this.mDragState = null;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class DragHandler extends Handler {
        private final WindowManagerService mService;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        DragHandler(WindowManagerService service, Looper looper) {
            super(looper);
            MiuiMirrorDragDropController.this = r1;
            this.mService = service;
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            if (msg.what == 0) {
                synchronized (this.mService.mGlobalLock) {
                    if (MiuiMirrorDragDropController.this.mDragState != null) {
                        MiuiMirrorDragDropController.this.mDragState.mDragResult = false;
                        MiuiMirrorDragDropController.this.mDragState.closeLocked();
                    }
                }
            }
        }
    }
}
