package com.miui.server.smartpower;

import android.content.Context;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.ArrayMap;
import android.util.EventLog;
import android.util.Slog;
import android.util.SparseArray;
import java.util.ArrayList;
import java.util.Iterator;
/* loaded from: classes.dex */
public class AppAudioResource extends AppPowerResource {
    private static final long AUDIO_INACTIVE_DELAY_TIME = 2000;
    public static final int PLAYER_STATE_ZERO_PLAYER = 100;
    private final SparseArray<AudioRecord> mActivePidsMap = new SparseArray<>();
    private AudioManager mAudioManager;
    private Handler mHandler;
    private long mLastMusicPlayPid;
    private long mLastMusicPlayTimeStamp;

    public AppAudioResource(Context context, Looper looper) {
        this.mType = 1;
        this.mAudioManager = (AudioManager) context.getSystemService("audio");
        this.mHandler = new Handler(looper);
    }

    private AudioRecord getAudioRecord(int pid) {
        AudioRecord audioRecord;
        synchronized (this.mActivePidsMap) {
            audioRecord = this.mActivePidsMap.get(pid);
        }
        return audioRecord;
    }

    private AudioRecord getAudioRecord(int uid, String clientId) {
        synchronized (this.mActivePidsMap) {
            for (int i = 0; i < this.mActivePidsMap.size(); i++) {
                AudioRecord record = this.mActivePidsMap.valueAt(i);
                if (record.mOwnerUid == uid && record.containsClientId(clientId)) {
                    return record;
                }
            }
            return null;
        }
    }

    private AudioRecord getAudioRecord(int uid, int riid) {
        synchronized (this.mActivePidsMap) {
            for (int i = 0; i < this.mActivePidsMap.size(); i++) {
                AudioRecord record = this.mActivePidsMap.valueAt(i);
                if (record.mOwnerUid == uid && record.containsRecorder(riid)) {
                    return record;
                }
            }
            return null;
        }
    }

    private AudioRecord getOrCreateAudioRecord(int uid, int pid) {
        AudioRecord record;
        synchronized (this.mActivePidsMap) {
            record = getAudioRecord(pid);
            if (record == null) {
                record = new AudioRecord(uid, pid);
                this.mActivePidsMap.put(pid, record);
            }
        }
        return record;
    }

    @Override // com.miui.server.smartpower.AppPowerResource
    public ArrayList<Integer> getActiveUids() {
        return null;
    }

    @Override // com.miui.server.smartpower.AppPowerResource
    public boolean isAppResourceActive(int uid) {
        synchronized (this.mActivePidsMap) {
            for (int i = 0; i < this.mActivePidsMap.size(); i++) {
                AudioRecord record = this.mActivePidsMap.valueAt(i);
                if (record != null && record.mOwnerUid == uid && record.mActive) {
                    return true;
                }
            }
            return false;
        }
    }

    @Override // com.miui.server.smartpower.AppPowerResource
    public boolean isAppResourceActive(int uid, int pid) {
        AudioRecord record = getAudioRecord(pid);
        if (record != null) {
            return record.mActive;
        }
        return false;
    }

    public long getLastMusicPlayTimeStamp(int pid) {
        if (this.mLastMusicPlayPid == pid) {
            return this.mLastMusicPlayTimeStamp;
        }
        return 0L;
    }

    @Override // com.miui.server.smartpower.AppPowerResource
    public void releaseAppPowerResource(int uid) {
        synchronized (this.mActivePidsMap) {
            for (int i = 0; i < this.mActivePidsMap.size(); i++) {
                AudioRecord record = this.mActivePidsMap.valueAt(i);
                if (record != null && record.mOwnerUid == uid && !record.mActive && record.isZeroAudioRecord()) {
                    EventLog.writeEvent((int) SmartPowerSettings.EVENT_TAGS, "power zero audio " + record.mOwnerUid + " " + record.mOwnerPid);
                }
            }
        }
    }

    @Override // com.miui.server.smartpower.AppPowerResource
    public void resumeAppPowerResource(int uid) {
    }

    public void playbackStateChanged(int uid, int pid, int oldState, int newState) {
        AudioRecord record = getAudioRecord(pid);
        if (record != null) {
            record.playbackStateChanged(oldState, newState);
        }
    }

    public void recordAudioFocus(int uid, int pid, String clientId, boolean request) {
        AudioRecord record = getOrCreateAudioRecord(uid, pid);
        record.recordAudioFocus(request, clientId);
    }

    public void recordAudioFocusLoss(int uid, String clientId, int focusLoss) {
        AudioRecord record = getAudioRecord(uid, clientId);
        if (record != null) {
            record.recordAudioFocusLoss(clientId, focusLoss);
        }
    }

    public void onPlayerTrack(int uid, int pid, int piid, int sessionId) {
        AudioRecord record = getOrCreateAudioRecord(uid, pid);
        record.onPlayerTrack(piid, sessionId);
    }

    public void onPlayerRlease(int uid, int pid, int piid) {
        AudioRecord record = getAudioRecord(pid);
        if (record != null) {
            record.onPlayerRlease(piid);
        }
    }

    public void onPlayerEvent(int uid, int pid, int piid, int event) {
        AudioRecord record = getAudioRecord(pid);
        if (record != null) {
            record.onPlayerEvent(piid, event);
        }
    }

    public void onRecorderTrack(int uid, int pid, int riid) {
        AudioRecord record = getOrCreateAudioRecord(uid, pid);
        record.onRecorderTrack(riid);
    }

    public void onRecorderRlease(int uid, int riid) {
        AudioRecord record = getAudioRecord(uid, riid);
        if (record != null) {
            record.onRecorderRlease(riid);
        }
    }

    public void onRecorderEvent(int uid, int riid, int event) {
        AudioRecord record = getAudioRecord(uid, riid);
        if (record != null) {
            record.onRecorderEvent(riid, event);
        }
    }

    public void reportTrackStatus(int uid, int pid, int sessionId, boolean isMuted) {
        AudioRecord record = getAudioRecord(pid);
        if (record != null) {
            record.reportTrackStatus(sessionId, isMuted);
        }
    }

    public void uidAudioStatusChanged(int uid, boolean active) {
        if (!active) {
            synchronized (this.mActivePidsMap) {
                for (int i = 0; i < this.mActivePidsMap.size(); i++) {
                    AudioRecord record = this.mActivePidsMap.valueAt(i);
                    if (record != null && record.mOwnerUid == uid) {
                        record.uidAudioStatusChanged(active);
                    }
                }
            }
        }
    }

    public void uidVideoStatusChanged(int uid, boolean active) {
    }

    public void updateUidStatus(int uid, int behavier) {
        boolean active = isAppResourceActive(uid);
        reportResourceStatus(uid, active, behavier);
    }

    /* loaded from: classes.dex */
    public class AudioRecord {
        Runnable mInactiveDelayTask;
        int mOwnerPid;
        int mOwnerUid;
        boolean mActive = false;
        boolean mCurrentActive = false;
        int mBehavier = 0;
        int mCurrentBehavier = 0;
        int mPlaybackState = -1;
        final ArrayMap<String, Integer> mFocusedClientIds = new ArrayMap<>();
        int mFocusLoss = 0;
        final ArrayMap<Integer, PlayerRecord> mPlayerRecords = new ArrayMap<>();
        final ArrayMap<Integer, RecorderRecord> mRecorderRecords = new ArrayMap<>();

        AudioRecord(int uid, int pid) {
            AppAudioResource.this = r2;
            this.mOwnerUid = uid;
            this.mOwnerPid = pid;
        }

        boolean containsRecorder(int riid) {
            synchronized (this.mRecorderRecords) {
                for (RecorderRecord recorder : this.mRecorderRecords.values()) {
                    if (recorder.mRiid == riid) {
                        return true;
                    }
                }
                return false;
            }
        }

        boolean containsClientId(String clientId) {
            boolean containsKey;
            synchronized (this.mFocusedClientIds) {
                containsKey = this.mFocusedClientIds.containsKey(clientId);
            }
            return containsKey;
        }

        void playbackStateChanged(int oldState, int newState) {
            if (this.mPlaybackState != newState) {
                this.mPlaybackState = newState;
                sendUpdateAudioStatusMsg();
                if (newState == 2) {
                    AppAudioResource.this.mLastMusicPlayPid = this.mOwnerPid;
                    AppAudioResource.this.mLastMusicPlayTimeStamp = SystemClock.uptimeMillis();
                }
            }
        }

        void recordAudioFocus(boolean request, String clientId) {
            synchronized (this.mFocusedClientIds) {
                if (request) {
                    this.mFocusedClientIds.put(clientId, 0);
                } else {
                    this.mFocusedClientIds.remove(clientId);
                }
                if (this.mFocusedClientIds.size() > 0) {
                    return;
                }
                sendUpdateAudioStatusMsg();
            }
        }

        void recordAudioFocusLoss(String clientId, int focusLoss) {
            synchronized (this.mFocusedClientIds) {
                this.mFocusedClientIds.put(clientId, Integer.valueOf(focusLoss));
            }
        }

        void onPlayerTrack(int piid, int sessionId) {
            synchronized (this.mPlayerRecords) {
                if (!this.mPlayerRecords.containsKey(Integer.valueOf(piid))) {
                    PlayerRecord record = new PlayerRecord(piid, sessionId);
                    this.mPlayerRecords.put(Integer.valueOf(piid), record);
                    sendUpdateAudioStatusMsg();
                }
            }
        }

        void onPlayerRlease(int piid) {
            synchronized (this.mPlayerRecords) {
                this.mPlayerRecords.remove(Integer.valueOf(piid));
            }
            sendUpdateAudioStatusMsg();
        }

        void onPlayerEvent(int piid, int event) {
            PlayerRecord record = getPlayerRecord(piid);
            if (record != null) {
                record.mEvent = event;
                if (this.mPlaybackState < 0) {
                    sendUpdateAudioStatusMsg();
                }
            }
        }

        void reportTrackStatus(int sessionId, boolean isMuted) {
            synchronized (this.mPlayerRecords) {
                for (PlayerRecord r : this.mPlayerRecords.values()) {
                    if (sessionId == r.mSessionId) {
                        r.mEvent = isMuted ? 100 : 5;
                        sendUpdateAudioStatusMsg();
                        return;
                    }
                }
            }
        }

        void onRecorderTrack(int riid) {
            synchronized (this.mRecorderRecords) {
                if (!this.mRecorderRecords.containsKey(Integer.valueOf(riid))) {
                    RecorderRecord record = new RecorderRecord(riid);
                    this.mRecorderRecords.put(Integer.valueOf(riid), record);
                }
            }
        }

        void onRecorderRlease(int riid) {
            synchronized (this.mRecorderRecords) {
                this.mRecorderRecords.remove(Integer.valueOf(riid));
            }
        }

        void onRecorderEvent(int riid, int event) {
            RecorderRecord record = getRecorderRecord(riid);
            if (record != null) {
                record.mEvent = event;
                sendUpdateAudioStatusMsg();
            }
        }

        void uidAudioStatusChanged(boolean active) {
            if (!active) {
                synchronized (this.mPlayerRecords) {
                    for (PlayerRecord player : this.mPlayerRecords.values()) {
                        if (player.mEvent == 5 || player.mEvent == 2) {
                            player.mEvent = 3;
                        }
                    }
                }
                sendUpdateAudioStatusMsg();
            }
        }

        private void sendUpdateAudioStatusMsg() {
            AppAudioResource.this.mHandler.post(new Runnable() { // from class: com.miui.server.smartpower.AppAudioResource.AudioRecord.1
                @Override // java.lang.Runnable
                public void run() {
                    AudioRecord.this.updateAudioStatus();
                }
            });
        }

        public void updateAudioStatus() {
            int mode;
            boolean active = false;
            int behavier = 0;
            boolean inactiveDelay = false;
            int i = this.mPlaybackState;
            if (i < 0) {
                synchronized (this.mPlayerRecords) {
                    if (this.mPlayerRecords.size() > 0) {
                        behavier = 0 | 4;
                    }
                    for (PlayerRecord player : this.mPlayerRecords.values()) {
                        if (player.mEvent != 5 && player.mEvent != 2) {
                        }
                        active = true;
                    }
                    inactiveDelay = !active;
                }
            } else {
                behavier = 0 | 2;
                if (i != 2) {
                    active = true;
                }
            }
            if (!active) {
                synchronized (this.mFocusedClientIds) {
                    if (this.mFocusedClientIds.size() > 0) {
                        Iterator<Integer> it = this.mFocusedClientIds.values().iterator();
                        while (true) {
                            if (!it.hasNext()) {
                                break;
                            }
                            int focusLoss = it.next().intValue();
                            if (focusLoss == -2) {
                                active = true;
                                break;
                            }
                        }
                    }
                }
            }
            synchronized (this.mRecorderRecords) {
                if (this.mRecorderRecords.size() > 0) {
                    behavier |= 8;
                }
                for (RecorderRecord recorder : this.mRecorderRecords.values()) {
                    if (recorder.mEvent == 0) {
                        active = true;
                    }
                }
            }
            if ((behavier & 4) != 0 && (behavier & 8) != 0 && ((mode = AppAudioResource.this.mAudioManager.getMode()) == 2 || mode == 3)) {
                behavier |= 16;
            }
            if (active != this.mCurrentActive || behavier != this.mCurrentBehavier) {
                this.mCurrentActive = active;
                this.mCurrentBehavier = behavier;
                if (inactiveDelay) {
                    if (this.mInactiveDelayTask == null) {
                        this.mInactiveDelayTask = new Runnable() { // from class: com.miui.server.smartpower.AppAudioResource.AudioRecord.2
                            @Override // java.lang.Runnable
                            public void run() {
                                AudioRecord.this.realUpdateAudioStatus();
                                AudioRecord.this.mInactiveDelayTask = null;
                            }
                        };
                        AppAudioResource.this.mHandler.postDelayed(this.mInactiveDelayTask, AppAudioResource.AUDIO_INACTIVE_DELAY_TIME);
                        return;
                    }
                    return;
                }
                if (this.mInactiveDelayTask != null) {
                    AppAudioResource.this.mHandler.removeCallbacks(this.mInactiveDelayTask);
                    this.mInactiveDelayTask = null;
                }
                realUpdateAudioStatus();
            }
        }

        public void realUpdateAudioStatus() {
            boolean z = this.mActive;
            boolean z2 = this.mCurrentActive;
            if (z != z2 || this.mBehavier != this.mCurrentBehavier) {
                this.mActive = z2;
                int i = this.mCurrentBehavier;
                this.mBehavier = i;
                AppAudioResource.this.updateUidStatus(this.mOwnerUid, i);
                AppAudioResource.this.reportResourceStatus(this.mOwnerUid, this.mOwnerPid, this.mActive, this.mBehavier);
                if (AppPowerResource.DEBUG) {
                    Slog.d(AppPowerResourceManager.TAG, this.mOwnerUid + " " + this.mOwnerPid + " audio active " + this.mActive + " " + this.mBehavier);
                }
                EventLog.writeEvent((int) SmartPowerSettings.EVENT_TAGS, "audio u:" + this.mOwnerUid + " p:" + this.mOwnerPid + " s:" + this.mActive);
            }
        }

        private PlayerRecord getPlayerRecord(int piid) {
            PlayerRecord playerRecord;
            synchronized (this.mPlayerRecords) {
                playerRecord = this.mPlayerRecords.get(Integer.valueOf(piid));
            }
            return playerRecord;
        }

        private RecorderRecord getRecorderRecord(int riid) {
            RecorderRecord recorderRecord;
            synchronized (this.mRecorderRecords) {
                recorderRecord = this.mRecorderRecords.get(Integer.valueOf(riid));
            }
            return recorderRecord;
        }

        public boolean isZeroAudioRecord() {
            synchronized (this.mPlayerRecords) {
                for (PlayerRecord r : this.mPlayerRecords.values()) {
                    if (r.mEvent == 100) {
                        return true;
                    }
                }
                return false;
            }
        }

        /* loaded from: classes.dex */
        public class PlayerRecord {
            int mEvent;
            int mPiid;
            int mSessionId;

            PlayerRecord(int piid, int sessionId) {
                AudioRecord.this = this$1;
                this.mPiid = piid;
                this.mSessionId = sessionId;
            }
        }

        /* loaded from: classes.dex */
        public class RecorderRecord {
            int mEvent;
            int mRiid;

            RecorderRecord(int piid) {
                AudioRecord.this = this$1;
                this.mRiid = piid;
            }
        }
    }
}
