package com.android.server.audio;

import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.util.Log;
/* loaded from: classes.dex */
public class AudioDeviceMoniter {
    private static final String ACTION_DISABLE_POLLING = "com.android.nfc.action.DISABLE_POLLING";
    private static final String ACTION_ENABLE_NFC_POLLING = "com.android.nfc.action.ENABLE_POLLING";
    public static final int AUDIO_MIC_STATE_OFF = 0;
    public static final int AUDIO_MIC_STATE_ON = 1;
    private static final String PROPERTY_MIC_STATUS = "vendor.audio.mic.status";
    private static final String TAG = "AudioDeviceMoniter";
    private static volatile AudioDeviceMoniter sInstance;
    private Intent mAction;
    private Context mContext;
    private int mCurrentAudioMicState;

    private AudioDeviceMoniter(Context context) {
        Log.d(TAG, "AudioDeviceMoniter init...");
        this.mContext = context;
        Log.d(TAG, "AudioDeviceMoniter init done");
    }

    public static AudioDeviceMoniter getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new AudioDeviceMoniter(context);
        }
        return sInstance;
    }

    public final void startPollAudioMicStatus() {
        Thread MicPollThread = new Thread() { // from class: com.android.server.audio.AudioDeviceMoniter.1
            @Override // java.lang.Thread, java.lang.Runnable
            public void run() {
                AudioDeviceMoniter.this.pollMic();
            }
        };
        MicPollThread.start();
    }

    public void pollMic() {
        int state;
        while (true) {
            String AudioMicState = SystemProperties.get(PROPERTY_MIC_STATUS);
            if (AudioMicState.equals("on")) {
                state = 1;
                this.mAction = new Intent(ACTION_DISABLE_POLLING);
            } else if (AudioMicState.equals("off")) {
                state = 0;
                this.mAction = new Intent(ACTION_ENABLE_NFC_POLLING);
            } else {
                Log.w(TAG, "unexpected value for AudioMicState");
                return;
            }
            if (state != this.mCurrentAudioMicState) {
                Log.d(TAG, "mic status be changed to " + state + ", sent broadcase to nfc...");
                this.mContext.sendBroadcastAsUser(this.mAction, UserHandle.ALL);
                this.mCurrentAudioMicState = state;
            }
            SystemClock.sleep(500L);
        }
    }
}
