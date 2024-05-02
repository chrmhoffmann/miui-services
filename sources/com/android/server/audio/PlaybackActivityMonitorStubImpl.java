package com.android.server.audio;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.AudioPlaybackConfiguration;
import android.media.IAudioService;
import android.net.Uri;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.util.Log;
import com.miui.base.MiuiStubRegistry;
import java.util.HashMap;
import java.util.List;
/* loaded from: classes.dex */
public class PlaybackActivityMonitorStubImpl implements PlaybackActivityMonitorStub {
    private static final String APPS_VOLUME_AUTHORITY = "com.miui.misound.AppVolumeProvider";
    private static final Uri APPS_VOLUME_CONTENT_URI;
    private static final String APPS_VOLUME_PATH = "assistant";
    private static final Uri APPS_VOLUME_URI;
    private static final int LOAD_APPS_VOLUME_DELAY = 5000;
    private static final String TAG = "PlaybackActivityMonitorStubImpl";
    private boolean mOpenSoundAssist = false;
    private Object objKeyLock = new Object();
    private boolean mIsForceIgnoreGranted = false;
    private final HashMap<String, Float> mMusicVolumeMap = new HashMap<>();
    private int mRouteCastUID = -1;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<PlaybackActivityMonitorStubImpl> {

        /* compiled from: PlaybackActivityMonitorStubImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final PlaybackActivityMonitorStubImpl INSTANCE = new PlaybackActivityMonitorStubImpl();
        }

        public PlaybackActivityMonitorStubImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public PlaybackActivityMonitorStubImpl provideNewInstance() {
            return new PlaybackActivityMonitorStubImpl();
        }
    }

    static {
        Uri parse = Uri.parse("content://com.miui.misound.AppVolumeProvider");
        APPS_VOLUME_CONTENT_URI = parse;
        APPS_VOLUME_URI = parse.buildUpon().appendPath(APPS_VOLUME_PATH).build();
    }

    public boolean isSoundAssistOpen() {
        boolean z;
        synchronized (this.objKeyLock) {
            z = this.mOpenSoundAssist;
        }
        return z;
    }

    public void setSoundAssistStatus(boolean open) {
        synchronized (this.objKeyLock) {
            this.mOpenSoundAssist = open;
        }
    }

    public boolean isForceIgnoreGranted() {
        return this.mIsForceIgnoreGranted;
    }

    public void setForceIgnoreGrantedStatus(boolean open) {
        this.mIsForceIgnoreGranted = open;
    }

    public float getPlayerVolume(String pkg) {
        float floatValue;
        if (this.mOpenSoundAssist && pkg != null) {
            synchronized (this.mMusicVolumeMap) {
                floatValue = this.mMusicVolumeMap.getOrDefault(pkg, Float.valueOf(1.0f)).floatValue();
            }
            return floatValue;
        }
        return 1.0f;
    }

    public float getPlayerVolume(Context context, AudioPlaybackConfiguration apc) {
        float floatValue;
        if (!this.mOpenSoundAssist || context == null || apc == null) {
            return 1.0f;
        }
        PackageManager pm = context.getPackageManager();
        String pkg = pm.getNameForUid(apc.getClientUid());
        synchronized (this.mMusicVolumeMap) {
            floatValue = this.mMusicVolumeMap.getOrDefault(pkg, Float.valueOf(1.0f)).floatValue();
        }
        return floatValue;
    }

    public HashMap<String, Float> getPkgVolumes() {
        HashMap<String, Float> hashMap;
        synchronized (this.mMusicVolumeMap) {
            hashMap = this.mMusicVolumeMap;
        }
        return hashMap;
    }

    private void initPlayerVolume(String pkg, float volume) {
        if (pkg != null) {
            synchronized (this.mMusicVolumeMap) {
                this.mMusicVolumeMap.put(pkg, Float.valueOf(volume));
            }
        }
    }

    private boolean isMusicPlayerActive(AudioPlaybackConfiguration apc) {
        if (apc == null) {
            return false;
        }
        return apc.getAudioAttributes().getUsage() == 1 || apc.getAudioAttributes().getVolumeControlStream() == 3;
    }

    private String getPkgName(AudioPlaybackConfiguration apc, Context context) {
        if (apc == null || context == null) {
            return null;
        }
        PackageManager pm = context.getPackageManager();
        return pm.getNameForUid(apc.getClientUid());
    }

    public void resetPlayerVolume(Context context) {
        if (context == null) {
            return;
        }
        AudioManager am = (AudioManager) context.getSystemService("audio");
        List<AudioPlaybackConfiguration> apcList = am.getActivePlaybackConfigurations();
        Log.d(TAG, "resetPlayerVolume size=" + apcList.size());
        for (AudioPlaybackConfiguration apc : apcList) {
            if (apc.getClientUid() > 10000 && isMusicPlayerActive(apc)) {
                try {
                    String pkgName = getPkgName(apc, context);
                    Log.d(TAG, "resetPlayerVolume pkgName=" + pkgName);
                    apc.getPlayerProxy().setVolume(getPlayerVolume(pkgName));
                } catch (Exception e) {
                    Log.e(TAG, "setPlayerVolume error in resetPlayerVolume", e);
                }
            }
        }
    }

    public boolean loadAppsVolume(ContentResolver cr) {
        Cursor cs = null;
        if (cr == null) {
            return false;
        }
        try {
            cs = cr.query(APPS_VOLUME_URI, null, null, null, null);
        } catch (Exception e) {
            Log.e(TAG, "loadAppsVolume: query exception", e);
        }
        if (cs == null) {
            return false;
        }
        while (cs.moveToNext()) {
            try {
                String pkgName = cs.getString(cs.getColumnIndex("_package_name"));
                float volume = cs.getInt(cs.getColumnIndex("_volume")) / 100.0f;
                Log.d(TAG, "loadAppsVolume pkgName " + pkgName + " volume=" + volume);
                initPlayerVolume(pkgName, volume);
            } catch (Exception e2) {
                Log.e(TAG, "loadAppsVolume: init player Volume error", e2);
                return false;
            } finally {
                cs.close();
            }
        }
        return true;
    }

    public void startPlayerVolumeService(Context context, int streamType, int flags) {
        if (this.mOpenSoundAssist && context != null) {
            try {
                Intent intent = new Intent();
                intent.setAction("com.miui.misound.playervolume.VolumeUIService");
                intent.setComponent(new ComponentName("com.miui.misound", "com.miui.misound.playervolume.VolumeUIService"));
                intent.putExtra("streamType", streamType);
                intent.putExtra("flags", flags);
                context.startForegroundService(intent);
            } catch (Exception e) {
                Log.e(TAG, "fail to start VolumeUIService");
            }
        }
    }

    public void setPlayerVolume(AudioPlaybackConfiguration apc, float volume, String from, Context context) {
        if (!this.mOpenSoundAssist || context == null) {
            return;
        }
        synchronized (this.mMusicVolumeMap) {
            if (apc.getClientUid() < 10000) {
                Log.e(TAG, "setPlayerVolume skip system uid=" + apc.getClientUid());
                return;
            }
            String pkg = getPkgName(apc, context);
            if (pkg != null) {
                this.mMusicVolumeMap.put(pkg, Float.valueOf(volume));
            }
            try {
                if (!isMusicPlayerActive(apc)) {
                    apc.getPlayerProxy().setVolume(1.0f);
                    Log.e(TAG, "set volume is only for active music player" + from);
                } else {
                    apc.getPlayerProxy().setVolume(volume);
                }
            } catch (Exception e) {
                Log.e(TAG, "setPlayerVolume ", e);
            }
            AudioManager am = (AudioManager) context.getSystemService("audio");
            List<AudioPlaybackConfiguration> apcList = am.getActivePlaybackConfigurations();
            for (AudioPlaybackConfiguration apcNew : apcList) {
                if (!apc.equals(apcNew) && apc.getClientUid() == apcNew.getClientUid()) {
                    try {
                        if (!isMusicPlayerActive(apcNew)) {
                            apcNew.getPlayerProxy().setVolume(1.0f);
                        } else {
                            apcNew.getPlayerProxy().setVolume(volume);
                        }
                    } catch (Exception e2) {
                        Log.e(TAG, "setPlayerVolume in same process error", e2);
                    }
                }
            }
        }
    }

    public void updatePlayerVolumeByApc(AudioPlaybackConfiguration apc, String from, Context context) {
        String pkgName;
        if (this.mOpenSoundAssist && (pkgName = getPkgName(apc, context)) != null) {
            float vol = getPlayerVolume(pkgName);
            setPlayerVolume(apc, vol, from, context);
        }
    }

    public void loadSoundAssistSettings(ContentResolver cr) {
        if (cr == null) {
            return;
        }
        boolean z = false;
        this.mOpenSoundAssist = Settings.Global.getInt(cr, "sound_assist_key", 0) == 1;
        if (Settings.Global.getInt(cr, "key_ignore_music_focus_req", 0) == 1) {
            z = true;
        }
        this.mIsForceIgnoreGranted = z;
    }

    public void setRouteCastUID(String keyValuePairs) {
        if (keyValuePairs.startsWith("routecast=off")) {
            this.mRouteCastUID = -1;
        } else if (keyValuePairs.startsWith("routecast=on")) {
            try {
                String[] params = keyValuePairs.split(";");
                for (String str : params) {
                    if (str.startsWith("uid=")) {
                        this.mRouteCastUID = Integer.parseInt(str.substring(4));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                this.mRouteCastUID = -1;
            }
        }
    }

    public boolean ignoreFocusRequest(FocusRequester frWinner, int uid) {
        if (frWinner == null) {
            return false;
        }
        if (!interruptMusicPlayback(uid)) {
            return true;
        }
        int usage = frWinner.getAudioAttributes().getUsage();
        boolean usageMatched = usage == 1 || usage == 5;
        Log.d(TAG, " mRouteCast: " + this.mRouteCastUID + " uid: " + uid);
        boolean openSmallWindowMediaProjection = false;
        int clientUid = frWinner.getClientUid();
        int i = this.mRouteCastUID;
        if (clientUid == i || i == uid) {
            usageMatched = usageMatched || usage == 4;
            openSmallWindowMediaProjection = true;
        }
        if (!usageMatched) {
            return false;
        }
        return this.mIsForceIgnoreGranted || openSmallWindowMediaProjection;
    }

    public boolean interruptMusicPlayback(int uid) {
        IBinder b = ServiceManager.getService("audio");
        IAudioService audioService = IAudioService.Stub.asInterface(b);
        try {
            int[] uidArray = audioService.getAudioPolicyMatchUids();
            boolean hasUid = false;
            if (uidArray != null && uidArray.length > 0) {
                int i = 0;
                while (true) {
                    if (i >= uidArray.length) {
                        break;
                    }
                    Log.d(TAG, "getAudioPolicyMatchUids=" + uidArray[i]);
                    if (uidArray[i] != uid) {
                        i++;
                    } else {
                        hasUid = true;
                        break;
                    }
                }
            }
            Log.d(TAG, "interruptMusicPlayback uid=" + uid + " hasUid=" + hasUid);
            return !hasUid;
        } catch (RemoteException e) {
            return true;
        }
    }
}
