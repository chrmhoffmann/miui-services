package com.xiaomi.mirror.service;

import android.app.ActivityTaskManager;
import android.app.AppOpsManager;
import android.app.IActivityTaskManager;
import android.app.IUriGrantsManager;
import android.app.UriGrantsManager;
import android.content.ClipData;
import android.content.ContentProvider;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.ShellCallback;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Slog;
import android.util.SparseArray;
import android.view.IWindow;
import android.view.PointerIcon;
import com.android.server.LocalServices;
import com.android.server.uri.UriGrantsManagerInternal;
import com.android.server.wm.DragDropControllerStub;
import com.android.server.wm.MiuiMirrorDragDropController;
import com.miui.server.input.MiuiCursorPositionListenerService;
import com.xiaomi.mirror.ICursorPositionChangedListener;
import com.xiaomi.mirror.IDragListener;
import com.xiaomi.mirror.IMirrorDelegate;
import com.xiaomi.mirror.IMirrorService;
import com.xiaomi.mirror.IMirrorStateListener;
import com.xiaomi.mirror.MirrorMenu;
import com.xiaomi.mirror.service.MirrorService;
import java.io.FileDescriptor;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
/* loaded from: classes.dex */
public class MirrorService extends IMirrorService.Stub {
    private static final String TAG = "MirrorService";
    private static ThreadLocal<Boolean> sAllowGrant = new ThreadLocal<Boolean>() { // from class: com.xiaomi.mirror.service.MirrorService.1
        @Override // java.lang.ThreadLocal
        public Boolean initialValue() {
            return false;
        }
    };
    private Context mContext;
    private Bitmap mCurrentShadowImage;
    private MiuiCursorPositionListenerService.OnCursorPositionChangedListener mCursorPositionListener;
    private IMirrorDelegate mDelegate;
    private String mDelegatePackageName;
    private int mDelegatePid;
    private int mDelegateUid;
    private boolean mDragAcceptable;
    private boolean mIsInMouseShareMode;
    private int mLastNotifiedUid;
    private IBinder mPermissionOwner;
    private UriGrantsManagerInternal mUgmInternal;
    private RemoteCallbackList<IMirrorStateListener> mCallbacks = new RemoteCallbackList<>();
    private final IBinder.DeathRecipient mDeathRecipient = new IBinder.DeathRecipient() { // from class: com.xiaomi.mirror.service.MirrorService$$ExternalSyntheticLambda0
        @Override // android.os.IBinder.DeathRecipient
        public final void binderDied() {
            MirrorService.this.delegateLost();
        }
    };
    private final SparseArray<CursorPositionChangedListenerRecord> mCursorPositionChangedListeners = new SparseArray<>();
    private final ArrayList<CursorPositionChangedListenerRecord> mTempCursorPositionChangedListenersToNotify = new ArrayList<>();
    private boolean mNeedFinishAnimator = true;
    private final SparseArray<DragListenerRecord> mDragListeners = new SparseArray<>();
    private final ArrayList<DragListenerRecord> mTempDragListenersToNotify = new ArrayList<>();
    private IUriGrantsManager mUgm = UriGrantsManager.getService();
    private MiuiMirrorDragDropController mMiuiMirrorDragDropController = new MiuiMirrorDragDropController();
    private IActivityTaskManager mAtms = ActivityTaskManager.getService();

    public static void init(Context context) {
        ServiceManager.addService("miui.mirror_service", new MirrorService(context));
    }

    public static MirrorService get() {
        return (MirrorService) ServiceManager.getService("miui.mirror_service");
    }

    private MirrorService(Context context) {
        this.mContext = context;
        UriGrantsManagerInternal uriGrantsManagerInternal = (UriGrantsManagerInternal) LocalServices.getService(UriGrantsManagerInternal.class);
        this.mUgmInternal = uriGrantsManagerInternal;
        this.mPermissionOwner = uriGrantsManagerInternal.newUriPermissionOwner("mirror");
    }

    public MiuiMirrorDragDropController getDragDropController() {
        return this.mMiuiMirrorDragDropController;
    }

    public void resetDragAndDropController() {
        this.mMiuiMirrorDragDropController = new MiuiMirrorDragDropController();
    }

    public int getDelegatePid() {
        return this.mDelegatePid;
    }

    public String getDelegatePackageName() {
        return this.mDelegatePackageName;
    }

    public void addMirrorStateListener(IMirrorStateListener listener) {
        this.mCallbacks.register(listener);
        try {
            listener.onDelegateStateChanged(this.mDelegate != null);
        } catch (RemoteException e) {
        }
    }

    public void removeMirrorStateListener(IMirrorStateListener listener) {
        this.mCallbacks.register(listener);
    }

    public void registerDelegate(IMirrorDelegate delegate, String packageName) throws RemoteException {
        this.mContext.enforceCallingPermission("com.xiaomi.mirror.permission.REGISTER_MIRROR_DELEGATE", "registerDelegate");
        if (this.mDelegate != null) {
            Slog.d(TAG, "already delegating!");
        } else if (delegate == null) {
            throw new NullPointerException();
        } else {
            int uid = getCallingUid();
            ((AppOpsManager) this.mContext.getSystemService(AppOpsManager.class)).checkPackage(uid, packageName);
            delegate.asBinder().linkToDeath(this.mDeathRecipient, 0);
            this.mDelegate = delegate;
            this.mDelegateUid = uid;
            this.mDelegatePid = getCallingPid();
            this.mDelegatePackageName = packageName;
            broadcastDelegateStateChanged();
        }
    }

    public void unregisterDelegate(IMirrorDelegate delegate) {
        this.mContext.enforceCallingPermission("com.xiaomi.mirror.permission.REGISTER_MIRROR_DELEGATE", "unregisterDelegate");
        if (this.mDelegate == null) {
            Slog.d(TAG, "already undelegated");
        } else if (delegate.asBinder() != this.mDelegate.asBinder()) {
            Slog.w(TAG, "binder doesn't match!");
        } else if (this.mDelegate.asBinder().unlinkToDeath(this.mDeathRecipient, 0)) {
            delegateLost();
        }
    }

    public void grantUriPermissionsToPackage(List<Uri> uris, int sourceUid, String targetPkg, int targetUserId, int mode) {
        this.mContext.enforceCallingPermission("com.xiaomi.mirror.permission.REGISTER_MIRROR_DELEGATE", "grantUriPermissionsToPackage");
        for (Uri uri : uris) {
            grantUri(uri, sourceUid, targetPkg, targetUserId, mode);
        }
    }

    public void revokePermissions(ClipData data, int sourceUid) {
        this.mContext.enforceCallingPermission("com.xiaomi.mirror.permission.REGISTER_MIRROR_DELEGATE", "revokeDragDataPermissions");
        revokePermissionsInternal(data, sourceUid);
    }

    public IBinder performDrag(IWindow window, int flags, int sourceDisplayId, ClipData data, Bitmap shadow) {
        Throwable th;
        int callerPid = getCallingPid();
        int callerUid = getCallingUid();
        long ident = Binder.clearCallingIdentity();
        try {
            IBinder token = this.mMiuiMirrorDragDropController.performDrag(callerPid, callerUid, window, flags, sourceDisplayId, data);
            if (token != null) {
                try {
                    updateShadow(shadow);
                    this.mDragAcceptable = false;
                    this.mLastNotifiedUid = 0;
                } catch (Throwable th2) {
                    th = th2;
                    Binder.restoreCallingIdentity(ident);
                    throw th;
                }
            }
            Binder.restoreCallingIdentity(ident);
            return token;
        } catch (Throwable th3) {
            th = th3;
        }
    }

    public void injectDragEvent(int action, int displayId, float newX, float newY) {
        long ident = Binder.clearCallingIdentity();
        try {
            this.mMiuiMirrorDragDropController.injectDragEvent(action, displayId, newX, newY);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public void reportDropResult(IWindow window, boolean consumed) {
        int pid = Binder.getCallingPid();
        long ident = Binder.clearCallingIdentity();
        try {
            this.mMiuiMirrorDragDropController.reportDropResult(pid, window, consumed);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public void cancelDragAndDrop(IBinder dragToken) {
        long ident = Binder.clearCallingIdentity();
        try {
            this.mMiuiMirrorDragDropController.cancelDragAndDrop(dragToken);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public void updateShadow(Bitmap shadow) {
        if (this.mDelegate != null && shadow != null) {
            long ident = Binder.clearCallingIdentity();
            try {
                try {
                    this.mDelegate.onShadowChanged(shadow);
                } catch (RemoteException e) {
                    Slog.w(TAG, "notify drag shadow failed");
                }
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
    }

    public void setDragAcceptable(boolean acceptable) {
        int uid;
        if (!this.mMiuiMirrorDragDropController.dragDropActiveLocked() || (uid = Binder.getCallingUid()) != this.mMiuiMirrorDragDropController.lastTargetUid()) {
            return;
        }
        if (this.mLastNotifiedUid == uid && this.mDragAcceptable == acceptable) {
            return;
        }
        this.mLastNotifiedUid = uid;
        this.mDragAcceptable = acceptable;
        if (this.mDelegate != null) {
            long ident = Binder.clearCallingIdentity();
            try {
                try {
                    this.mDelegate.onAcceptableChanged(uid, acceptable);
                } catch (RemoteException e) {
                    Slog.w(TAG, "notify drag acceptable failed");
                }
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
    }

    public void onRemoteMenuActionCall(MirrorMenu menu, int displayId) throws RemoteException {
        if (this.mDelegate != null) {
            int uid = Binder.getCallingUid();
            if (menu.getUri() != null) {
                grantUri(menu.getUri(), uid, this.mDelegatePackageName, this.mDelegateUid, 3);
            }
            long ident = Binder.clearCallingIdentity();
            try {
                this.mDelegate.onRemoteMenuActionCall(menu, displayId);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) {
        new MirrorShellCommand().exec(this, in, out, err, args, callback, resultReceiver);
    }

    public void setAllowGrant(boolean allowGrant) {
        sAllowGrant.set(Boolean.valueOf(allowGrant));
    }

    public boolean getAllowGrant() {
        return sAllowGrant.get().booleanValue();
    }

    private void broadcastDelegateStateChanged() {
        boolean hasDelegate = this.mDelegate != null;
        long ident = Binder.clearCallingIdentity();
        try {
            for (int i = this.mCallbacks.beginBroadcast() - 1; i >= 0; i--) {
                try {
                    this.mCallbacks.getBroadcastItem(i).onDelegateStateChanged(hasDelegate);
                } catch (RemoteException e) {
                }
            }
            this.mCallbacks.finishBroadcast();
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public void delegateLost() {
        this.mDelegate = null;
        this.mDelegateUid = 0;
        this.mDelegatePid = 0;
        this.mDelegatePackageName = null;
        this.mMiuiMirrorDragDropController.shutdownDragAndDropIfNeeded();
        broadcastDelegateStateChanged();
        resetSystemProperties();
    }

    private void resetSystemProperties() {
        Settings.Secure.putInt(this.mContext.getContentResolver(), "miui_mirror_dnd_mode", 0);
        Settings.Secure.putInt(this.mContext.getContentResolver(), "screen_project_in_screening", 0);
        Settings.Secure.putInt(this.mContext.getContentResolver(), "screen_project_hang_up_on", 0);
    }

    public void notifyDragStart(int sourceUid, int sourcePid, ClipData clipData, int flags) {
        if (this.mDelegate != null) {
            grantPermissions(sourceUid, clipData, flags);
            long ident = Binder.clearCallingIdentity();
            try {
                try {
                    this.mDelegate.onDragStart(clipData, sourceUid, sourcePid, flags);
                } catch (RemoteException e) {
                    Slog.w(TAG, "notify drag start failed");
                }
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
    }

    public boolean tryToShareDrag(String targetPkg, int taskId, ClipData data) {
        if (this.mDelegate != null) {
            long ident = Binder.clearCallingIdentity();
            try {
                return this.mDelegate.tryToShareDrag(targetPkg, taskId, data);
            } catch (RemoteException e) {
                Slog.w(TAG, "try to share drag failed", e);
                return false;
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
        return false;
    }

    public void notifyDragResult(boolean result) {
        if (this.mDelegate != null) {
            long ident = Binder.clearCallingIdentity();
            try {
                try {
                    this.mDelegate.onDragResult(result);
                } catch (RemoteException e) {
                    Slog.w(TAG, "notify drag cancel failed");
                }
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
    }

    public void onDelegatePermissionReleased(List<Uri> uris) {
        if (this.mDelegate != null) {
            long ident = Binder.clearCallingIdentity();
            try {
                try {
                    this.mDelegate.onDelegatePermissionReleased(uris);
                } catch (RemoteException e) {
                    Slog.w(TAG, "notify permission release failed");
                }
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
    }

    public void notifyPointerIconChanged(int iconId, PointerIcon customIcon) {
        if (this.mDelegate != null) {
            long ident = Binder.clearCallingIdentity();
            try {
                try {
                    this.mDelegate.onPointerIconChanged(iconId, customIcon);
                } catch (RemoteException e) {
                    Slog.w(TAG, "notify pointer icon change failed");
                }
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
    }

    private void grantPermissions(int sourceUid, ClipData data, int flags) {
        int mode = flags & 195;
        int N = data.getItemCount();
        for (int i = 0; i < N; i++) {
            grantItem(data.getItemAt(i), sourceUid, this.mDelegatePackageName, UserHandle.getUserId(this.mDelegateUid), mode);
        }
    }

    private void grantItem(ClipData.Item item, int sourceUid, String targetPkg, int targetUserId, int mode) {
        if (item.getUri() != null) {
            grantUri(item.getUri(), sourceUid, targetPkg, targetUserId, mode);
        }
        Intent intent = item.getIntent();
        if (intent != null && intent.getData() != null) {
            grantUri(intent.getData(), sourceUid, targetPkg, targetUserId, mode);
        }
    }

    private void grantUri(Uri uri, int sourceUid, String targetPkg, int targetUserId, int mode) {
        if (uri == null || !"content".equals(uri.getScheme())) {
            return;
        }
        long ident = Binder.clearCallingIdentity();
        setAllowGrant(true);
        try {
            this.mUgm.grantUriPermissionFromOwner(this.mPermissionOwner, sourceUid, targetPkg, ContentProvider.getUriWithoutUserId(uri), mode, ContentProvider.getUserIdFromUri(uri, UserHandle.getUserId(sourceUid)), targetUserId);
        } catch (RemoteException e) {
        } catch (Throwable th) {
            setAllowGrant(false);
            Binder.restoreCallingIdentity(ident);
            throw th;
        }
        setAllowGrant(false);
        Binder.restoreCallingIdentity(ident);
    }

    private void revokePermissionsInternal(ClipData data, int sourceUid) {
        int N = data.getItemCount();
        for (int i = 0; i < N; i++) {
            revokeItem(data.getItemAt(i), sourceUid);
        }
    }

    private void revokeItem(ClipData.Item item, int sourceUid) {
        if (item.getUri() != null) {
            revokeUri(item.getUri(), sourceUid);
        }
        Intent intent = item.getIntent();
        if (intent != null && intent.getData() != null) {
            revokeUri(intent.getData(), sourceUid);
        }
    }

    private void revokeUri(Uri uri, int sourceUid) {
        if (uri == null || !"content".equals(uri.getScheme())) {
            return;
        }
        long ident = Binder.clearCallingIdentity();
        try {
            this.mUgmInternal.revokeUriPermissionFromOwner(this.mPermissionOwner, ContentProvider.getUriWithoutUserId(uri), 1, ContentProvider.getUserIdFromUri(uri, UserHandle.getUserId(sourceUid)));
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public void registerCursorPositionChangedListener(ICursorPositionChangedListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener must not be null");
        }
        if (!this.mIsInMouseShareMode) {
            throw new IllegalArgumentException("current is not in mouse share mode");
        }
        synchronized (this.mCursorPositionChangedListeners) {
            int callingPid = Binder.getCallingPid();
            if (this.mCursorPositionChangedListeners.get(callingPid) != null) {
                throw new SecurityException("The calling process has alreadyregistered an CursorPositionChangedListener.");
            }
            CursorPositionChangedListenerRecord record = new CursorPositionChangedListenerRecord(callingPid, listener);
            try {
                IBinder binder = listener.asBinder();
                binder.linkToDeath(record, 0);
                Slog.d(TAG, "a cursor listener register, from " + callingPid + ", current size = " + this.mCursorPositionChangedListeners.size());
                this.mCursorPositionChangedListeners.put(callingPid, record);
                populateCursorPositionListenerLocked();
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void populateCursorPositionListenerLocked() {
        if (this.mCursorPositionListener != null || this.mCursorPositionChangedListeners.size() == 0) {
            return;
        }
        Slog.d(TAG, "register cursor position listener add to MiuiCursorPositionListenerService");
        this.mCursorPositionListener = new MiuiCursorPositionListenerService.OnCursorPositionChangedListener() { // from class: com.xiaomi.mirror.service.MirrorService$$ExternalSyntheticLambda4
            @Override // com.miui.server.input.MiuiCursorPositionListenerService.OnCursorPositionChangedListener
            public final void onCursorPositionChanged(int i, float f, float f2) {
                MirrorService.this.deliverCursorPositionChanged(i, f, f2);
            }
        };
        MiuiCursorPositionListenerService.getInstance().registerOnCursorPositionChangedListener(this.mCursorPositionListener);
    }

    public void unregisterCursorPositionChangedListener(ICursorPositionChangedListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener must not be null");
        }
        synchronized (this.mCursorPositionChangedListeners) {
            int callingPid = Binder.getCallingPid();
            CursorPositionChangedListenerRecord record = this.mCursorPositionChangedListeners.get(callingPid);
            if (record == null) {
                throw new SecurityException("The calling process not register an CursorPositionChangedListener.");
            }
            Slog.d(TAG, "a cursor listener unregister, from " + callingPid + ", current size = " + this.mCursorPositionChangedListeners.size());
            onCursorPositionChangedListenerDied(callingPid);
            unPopulateCursorPositionListenerLocked();
        }
    }

    private void unPopulateCursorPositionListenerLocked() {
        if (this.mCursorPositionListener == null || this.mCursorPositionChangedListeners.size() > 0) {
            return;
        }
        Slog.d(TAG, "unregister cursor position listener from MiuiCursorPositionListenerService");
        MiuiCursorPositionListenerService.getInstance().unregisterOnCursorPositionChangedListener(this.mCursorPositionListener);
        this.mCursorPositionListener = null;
    }

    public void onCursorPositionChangedListenerDied(int pid) {
        synchronized (this.mCursorPositionChangedListeners) {
            this.mCursorPositionChangedListeners.remove(pid);
        }
    }

    public void deliverCursorPositionChanged(final int deviceId, final float x, final float y) {
        if (!this.mIsInMouseShareMode) {
            return;
        }
        this.mTempCursorPositionChangedListenersToNotify.clear();
        synchronized (this.mCursorPositionChangedListeners) {
            int size = this.mCursorPositionChangedListeners.size();
            for (int i = 0; i < size; i++) {
                this.mTempCursorPositionChangedListenersToNotify.add(this.mCursorPositionChangedListeners.valueAt(i));
            }
        }
        this.mTempCursorPositionChangedListenersToNotify.forEach(new Consumer() { // from class: com.xiaomi.mirror.service.MirrorService$$ExternalSyntheticLambda1
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ((MirrorService.CursorPositionChangedListenerRecord) obj).notifyCursorPositionChanged(deviceId, x, y);
            }
        });
    }

    /* loaded from: classes.dex */
    public final class CursorPositionChangedListenerRecord implements IBinder.DeathRecipient {
        private final ICursorPositionChangedListener mListener;
        private final int mPid;

        public CursorPositionChangedListenerRecord(int mPid, ICursorPositionChangedListener mListener) {
            MirrorService.this = r1;
            this.mPid = mPid;
            this.mListener = mListener;
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            MirrorService.this.onCursorPositionChangedListenerDied(this.mPid);
        }

        public void notifyCursorPositionChanged(int deviceId, float x, float y) {
            try {
                this.mListener.onCursorPositionChanged(deviceId, x, y);
            } catch (RemoteException e) {
                binderDied();
            }
        }
    }

    public boolean setDragSurfaceVisible(boolean visible) {
        return DragDropControllerStub.get().setDragSurfaceVisible(visible);
    }

    public boolean cancelCurrentDrag() {
        return DragDropControllerStub.get().cancelCurrentDrag();
    }

    public void setFinishAnimatorNeeded(boolean needFinishAnimator) {
        this.mNeedFinishAnimator = needFinishAnimator;
    }

    public boolean isNeedFinishAnimator() {
        return this.mNeedFinishAnimator;
    }

    public void registerDragListener(IDragListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener must not be null");
        }
        if (!this.mIsInMouseShareMode) {
            throw new IllegalArgumentException("current is not in mouse share mode");
        }
        synchronized (this.mDragListeners) {
            int callingPid = Binder.getCallingPid();
            if (this.mDragListeners.get(callingPid) != null) {
                throw new SecurityException("The calling process has alreadyregistered a drag listener.");
            }
            DragListenerRecord record = new DragListenerRecord(callingPid, listener);
            try {
                IBinder binder = listener.asBinder();
                binder.linkToDeath(record, 0);
                this.mDragListeners.put(callingPid, record);
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void unregisterDragListener(IDragListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener must not be null");
        }
        synchronized (this.mDragListeners) {
            int callingPid = Binder.getCallingPid();
            DragListenerRecord record = this.mDragListeners.get(callingPid);
            if (record == null) {
                throw new SecurityException("The calling process not register an drag listener");
            }
            onDragListenerDied(callingPid);
        }
    }

    public void onDragListenerDied(int pid) {
        synchronized (this.mDragListeners) {
            this.mDragListeners.remove(pid);
        }
    }

    public void notifyDragStart(final ClipData data, final int uid, final int pid, final int flag) {
        this.mNeedFinishAnimator = true;
        if (!this.mIsInMouseShareMode) {
            return;
        }
        this.mTempDragListenersToNotify.clear();
        synchronized (this.mDragListeners) {
            int size = this.mDragListeners.size();
            for (int i = 0; i < size; i++) {
                this.mTempDragListenersToNotify.add(this.mDragListeners.valueAt(i));
            }
        }
        this.mTempDragListenersToNotify.forEach(new Consumer() { // from class: com.xiaomi.mirror.service.MirrorService$$ExternalSyntheticLambda2
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ((MirrorService.DragListenerRecord) obj).notifyDragStart(data, uid, pid, flag);
            }
        });
    }

    public void notifyDragFinish(final String packageName, final boolean dragResult) {
        if (!this.mIsInMouseShareMode) {
            return;
        }
        this.mTempDragListenersToNotify.clear();
        synchronized (this.mDragListeners) {
            int size = this.mDragListeners.size();
            for (int i = 0; i < size; i++) {
                this.mTempDragListenersToNotify.add(this.mDragListeners.valueAt(i));
            }
        }
        this.mTempDragListenersToNotify.forEach(new Consumer() { // from class: com.xiaomi.mirror.service.MirrorService$$ExternalSyntheticLambda3
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ((MirrorService.DragListenerRecord) obj).notifyDragFinish(packageName, dragResult);
            }
        });
    }

    /* loaded from: classes.dex */
    public final class DragListenerRecord implements IBinder.DeathRecipient {
        private final IDragListener mListener;
        private final int mPid;

        public DragListenerRecord(int mPid, IDragListener mListener) {
            MirrorService.this = r1;
            this.mPid = mPid;
            this.mListener = mListener;
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            MirrorService.this.onDragListenerDied(this.mPid);
        }

        public void notifyDragStart(ClipData data, int uid, int pid, int flag) {
            try {
                this.mListener.onDragStart(data, uid, pid, flag, MirrorService.this.mCurrentShadowImage);
            } catch (RemoteException e) {
                binderDied();
            }
        }

        public void notifyDragFinish(String packageName, boolean dragResult) {
            try {
                this.mListener.onDragFinish(packageName, dragResult);
            } catch (RemoteException e) {
                binderDied();
            }
        }
    }

    public void notifyShadowImage(Bitmap shadowImage) {
        this.mCurrentShadowImage = shadowImage;
    }

    public void notifyMouseShareModeState(boolean isInMouseShareMode) {
        this.mIsInMouseShareMode = isInMouseShareMode;
        long ident = Binder.clearCallingIdentity();
        try {
            for (int i = this.mCallbacks.beginBroadcast() - 1; i >= 0; i--) {
                try {
                    this.mCallbacks.getBroadcastItem(i).onMouseShareModeStateChanged(isInMouseShareMode);
                } catch (RemoteException e) {
                }
            }
            this.mCallbacks.finishBroadcast();
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public boolean isInMouseShareMode() {
        return this.mIsInMouseShareMode;
    }
}
