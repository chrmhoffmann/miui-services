package com.android.server.media.projection;

import android.media.projection.MediaProjectionInfo;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import com.android.server.media.projection.MediaProjectionManagerService;
import com.miui.base.MiuiStubRegistry;
import java.util.Iterator;
import java.util.LinkedList;
/* loaded from: classes.dex */
public class MediaProjectionManagerServiceStubImpl implements MediaProjectionManagerServiceStub {
    private static final String TAG = "MediaProjectionManagerServiceStubImpl";
    private final LinkedList<MediaProjectionManagerService.MediaProjection> mMediaProjectionClients = new LinkedList<>();

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<MediaProjectionManagerServiceStubImpl> {

        /* compiled from: MediaProjectionManagerServiceStubImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final MediaProjectionManagerServiceStubImpl INSTANCE = new MediaProjectionManagerServiceStubImpl();
        }

        public MediaProjectionManagerServiceStubImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public MediaProjectionManagerServiceStubImpl provideNewInstance() {
            return new MediaProjectionManagerServiceStubImpl();
        }
    }

    public boolean isSupportMutilMediaProjection() {
        return true;
    }

    public void handleForegroundServicesChanged(int pid, int uid, int serviceTypes) {
        Log.d(TAG, "handleForegroundServicesChanged");
        Iterator<MediaProjectionManagerService.MediaProjection> it = this.mMediaProjectionClients.iterator();
        while (it.hasNext()) {
            MediaProjectionManagerService.MediaProjection projection = it.next();
            if (projection != null && projection.uid == uid && projection.pid == pid && projection.requiresForegroundService() && (serviceTypes & 32) == 0 && !isUcar(projection.packageName)) {
                it.remove();
                projection.stop();
            }
        }
    }

    private boolean isUcar(String packageName) {
        if (TextUtils.equals(packageName, "com.miui.carlink")) {
            return true;
        }
        return false;
    }

    public void addMediaProjection(MediaProjectionManagerService.MediaProjection mp) {
        Log.d(TAG, "addMediaProjection");
        removeMediaProjection(mp);
        this.mMediaProjectionClients.add(0, mp);
    }

    public void removeMediaProjection(MediaProjectionManagerService.MediaProjection mp) {
        Log.d(TAG, "removeMediaProjection");
        this.mMediaProjectionClients.remove(mp);
    }

    public boolean isValidMediaProjection(IBinder token) {
        Iterator<MediaProjectionManagerService.MediaProjection> it = this.mMediaProjectionClients.iterator();
        while (it.hasNext()) {
            MediaProjectionManagerService.MediaProjection projection = it.next();
            if (projection.asBinder() == token) {
                return true;
            }
        }
        return false;
    }

    public MediaProjectionInfo getActiveProjectionInfo() {
        if (this.mMediaProjectionClients.size() == 0) {
            return null;
        }
        return this.mMediaProjectionClients.get(0).getProjectionInfo();
    }

    public void stopActiveProjection() {
        Log.d(TAG, "stopActiveProjection");
        if (this.mMediaProjectionClients.size() > 0) {
            this.mMediaProjectionClients.get(0).stop();
        }
    }

    public void stopProjections() {
        Log.d(TAG, "stopProjections");
        Iterator<MediaProjectionManagerService.MediaProjection> it = this.mMediaProjectionClients.iterator();
        while (it.hasNext()) {
            MediaProjectionManagerService.MediaProjection projection = it.next();
            it.remove();
            if (projection != null) {
                projection.stop();
            }
        }
    }
}
