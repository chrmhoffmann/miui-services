package com.miui.server.smartpower;

import android.content.Context;
import android.location.ILocationListener;
import android.os.IBinder;
import android.os.Looper;
import com.android.server.am.SmartPowerService;
import com.miui.server.smartpower.AppPowerResource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
/* loaded from: classes.dex */
public class AppPowerResourceManager {
    public static final boolean DEBUG = SmartPowerService.DEBUG;
    public static final int RESOURCE_BEHAVIOR_CAMERA = 32;
    public static final int RESOURCE_BEHAVIOR_GPS = 64;
    public static final int RESOURCE_BEHAVIOR_INCALL = 16;
    public static final int RESOURCE_BEHAVIOR_NETWORK = 128;
    public static final int RESOURCE_BEHAVIOR_PLAYER = 4;
    public static final int RESOURCE_BEHAVIOR_PLAYER_PLAYBACK = 2;
    public static final int RESOURCE_BEHAVIOR_RECORDER = 8;
    public static final int RES_TYPE_AUDIO = 1;
    public static final int RES_TYPE_BLE = 5;
    public static final int RES_TYPE_CAMERA = 6;
    public static final int RES_TYPE_GPS = 3;
    public static final int RES_TYPE_NET = 2;
    public static final int RES_TYPE_WAKELOCK = 4;
    public static final String TAG = "SmartPower.AppResource";
    private AppAudioResource mAudioResource;
    private AppBluetoothResource mBluetoothResource;
    private AppCameraResource mCameraResource;
    private HashMap<Integer, AppPowerResource> mDeviceResouceMap = new HashMap<>();
    private AppGPSResource mGPSResource;
    private AppNetworkResource mNetWorkResource;
    private AppWakelockResource mWakelockResource;

    public AppPowerResourceManager(Context context, Looper looper) {
        this.mAudioResource = new AppAudioResource(context, looper);
        this.mNetWorkResource = new AppNetworkResource(context, looper);
        this.mGPSResource = new AppGPSResource(context, looper);
        this.mWakelockResource = new AppWakelockResource(context, looper);
        this.mBluetoothResource = new AppBluetoothResource(context, looper);
        this.mCameraResource = new AppCameraResource(context, looper);
        addDeviceResource(this.mAudioResource.mType, this.mAudioResource);
        addDeviceResource(this.mNetWorkResource.mType, this.mNetWorkResource);
        addDeviceResource(this.mGPSResource.mType, this.mGPSResource);
        addDeviceResource(this.mWakelockResource.mType, this.mWakelockResource);
        addDeviceResource(this.mBluetoothResource.mType, this.mBluetoothResource);
        addDeviceResource(this.mCameraResource.mType, this.mCameraResource);
    }

    protected void addDeviceResource(int type, AppPowerResource resource) {
        this.mDeviceResouceMap.put(Integer.valueOf(type), resource);
    }

    public void playbackStateChanged(int uid, int pid, int oldState, int newState) {
        this.mAudioResource.playbackStateChanged(uid, pid, oldState, newState);
    }

    public void recordAudioFocus(int uid, int pid, String clientId, boolean request) {
        this.mAudioResource.recordAudioFocus(uid, pid, clientId, request);
    }

    public void recordAudioFocusLoss(int uid, String clientId, int focusLoss) {
        this.mAudioResource.recordAudioFocusLoss(uid, clientId, focusLoss);
    }

    public void onPlayerTrack(int uid, int pid, int piid, int sessionId) {
        this.mAudioResource.onPlayerTrack(uid, pid, piid, sessionId);
    }

    public void onPlayerRlease(int uid, int pid, int piid) {
        this.mAudioResource.onPlayerRlease(uid, pid, piid);
    }

    public void onPlayerEvent(int uid, int pid, int piid, int event) {
        this.mAudioResource.onPlayerEvent(uid, pid, piid, event);
    }

    public void onRecorderTrack(int uid, int pid, int riid) {
        this.mAudioResource.onRecorderTrack(uid, pid, riid);
    }

    public void onRecorderRlease(int uid, int riid) {
        this.mAudioResource.onRecorderRlease(uid, riid);
    }

    public void onRecorderEvent(int uid, int riid, int event) {
        this.mAudioResource.onRecorderEvent(uid, riid, event);
    }

    public void uidAudioStatusChanged(int uid, boolean active) {
        this.mAudioResource.uidAudioStatusChanged(uid, active);
    }

    public void uidVideoStatusChanged(int uid, boolean active) {
        this.mAudioResource.uidVideoStatusChanged(uid, active);
    }

    public void onAquireLocation(int uid, int pid, ILocationListener listener) {
        this.mGPSResource.onAquireLocation(uid, pid, listener);
    }

    public void onReleaseLocation(int uid, int pid, ILocationListener listener) {
        this.mGPSResource.onReleaseLocation(uid, pid, listener);
    }

    public void onAcquireWakelock(IBinder lock, int flags, String tag, int ownerUid, int ownerPid) {
        this.mWakelockResource.acquireWakelock(lock, flags, tag, ownerUid, ownerPid);
    }

    public void onReleaseWakelock(IBinder lock, int flags) {
        this.mWakelockResource.releaseWakelock(lock, flags);
    }

    public void onBluetoothEvent(boolean isConnect, int bleType, int uid, int pid, int data) {
        this.mBluetoothResource.onBluetoothEvent(isConnect, bleType, uid, pid, data);
    }

    public void reportTrackStatus(int uid, int pid, int sessionId, boolean isMuted) {
        this.mAudioResource.reportTrackStatus(uid, pid, sessionId, isMuted);
    }

    public void notifyCameraForegroundState(String cameraId, boolean isForeground, String caller, int callerUid, int callerPid) {
        this.mCameraResource.notifyCameraForegroundState(cameraId, isForeground, caller, callerUid, callerPid);
    }

    public void releaseAppPowerResources(int uid, ArrayList<Integer> resourceTypes) {
        Iterator<Integer> it = resourceTypes.iterator();
        while (it.hasNext()) {
            int type = it.next().intValue();
            AppPowerResource res = this.mDeviceResouceMap.get(Integer.valueOf(type));
            if (res != null) {
                res.releaseAppPowerResource(uid);
            }
        }
    }

    public void resumeAppPowerResources(int uid, ArrayList<Integer> resourceTypes) {
        Iterator<Integer> it = resourceTypes.iterator();
        while (it.hasNext()) {
            int type = it.next().intValue();
            AppPowerResource res = this.mDeviceResouceMap.get(Integer.valueOf(type));
            if (res != null) {
                res.resumeAppPowerResource(uid);
            }
        }
    }

    public void releaseAppAllPowerResources(int uid) {
        for (AppPowerResource res : this.mDeviceResouceMap.values()) {
            if (res != null) {
                res.releaseAppPowerResource(uid);
            }
        }
    }

    public void resumeAppAllPowerResources(int uid) {
        for (AppPowerResource res : this.mDeviceResouceMap.values()) {
            if (res != null) {
                res.resumeAppPowerResource(uid);
            }
        }
    }

    public boolean isAppResActive(int uid, int resourceType) {
        AppPowerResource res = this.mDeviceResouceMap.get(Integer.valueOf(resourceType));
        if (res != null) {
            return res.isAppResourceActive(uid);
        }
        return false;
    }

    public boolean isAppResActive(int uid, int pid, int resourceType) {
        AppPowerResource res = this.mDeviceResouceMap.get(Integer.valueOf(resourceType));
        if (res != null) {
            return res.isAppResourceActive(uid, pid);
        }
        return false;
    }

    public ArrayList getActiveUids(int resourceType) {
        AppPowerResource res = this.mDeviceResouceMap.get(Integer.valueOf(resourceType));
        if (res != null) {
            return res.getActiveUids();
        }
        return null;
    }

    public long getLastMusicPlayTimeStamp(int pid) {
        return this.mAudioResource.getLastMusicPlayTimeStamp(pid);
    }

    public void registerCallback(int type, AppPowerResource.IAppPowerResourceCallback callback, int uid) {
        AppPowerResource res = this.mDeviceResouceMap.get(Integer.valueOf(type));
        if (res != null) {
            res.registerCallback(callback, uid);
        }
    }

    public void unRegisterCallback(int type, AppPowerResource.IAppPowerResourceCallback callback, int uid) {
        AppPowerResource res = this.mDeviceResouceMap.get(Integer.valueOf(type));
        if (res != null) {
            res.unRegisterCallback(callback, uid);
        }
    }

    public void registerCallback(int type, AppPowerResource.IAppPowerResourceCallback callback, int uid, int pid) {
        AppPowerResource res = this.mDeviceResouceMap.get(Integer.valueOf(type));
        if (res != null) {
            res.registerCallback(callback, uid, pid);
        }
    }

    public void unRegisterCallback(int type, AppPowerResource.IAppPowerResourceCallback callback, int uid, int pid) {
        AppPowerResource res = this.mDeviceResouceMap.get(Integer.valueOf(type));
        if (res != null) {
            res.unRegisterCallback(callback, uid, pid);
        }
    }

    public static String typeToString(int type) {
        switch (type) {
            case 1:
                return "audio";
            case 2:
                return "net";
            case 3:
                return "gps";
            case 4:
                return "wakelock";
            case 5:
                return "bluetooth";
            case 6:
                return "camera";
            default:
                return Integer.toString(type);
        }
    }
}
