package com.android.server.input.shoulderkey;

import android.content.Context;
import android.media.SoundPool;
import android.util.ArrayMap;
import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import libcore.io.IoUtils;
/* loaded from: classes.dex */
public class ShoulderKeyUtil {
    private static final String SHOULDERKEY_STATE_FILE = "/dev/gamekey";
    private static final String TAG = "ShoulderKeyUtil";
    private static final int TYPE_SYSTEM = 1;
    private static Context mContext;
    private static SoundPool mSoundPool;
    private static final ArrayMap<String, Integer> SOUNDS_MAP = new ArrayMap<>();
    private static final ArrayList<Integer> LOADED_SOUND_IDS = new ArrayList<>();
    private static boolean mIsSoundPooLoadComplete = false;

    public static boolean getShoulderKeySwitchStatus(int position) {
        File file = new File(SHOULDERKEY_STATE_FILE);
        if (file.exists()) {
            byte[] filecontent = new byte[4];
            FileInputStream in = null;
            try {
                try {
                    in = new FileInputStream(file);
                    in.read(filecontent);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e2) {
                    e2.printStackTrace();
                }
                IoUtils.closeQuietly(in);
                return position == 0 ? filecontent[0] != 0 : position == 1 && filecontent[1] != 0;
            } catch (Throwable th) {
                IoUtils.closeQuietly(in);
                throw th;
            }
        }
        return false;
    }

    private static void initSoundPool(Context context) {
        mContext = context;
        SoundPool soundPool = new SoundPool(10, 1, 0);
        mSoundPool = soundPool;
        soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() { // from class: com.android.server.input.shoulderkey.ShoulderKeyUtil.1
            @Override // android.media.SoundPool.OnLoadCompleteListener
            public void onLoadComplete(SoundPool soundPool2, int sampleId, int status) {
                if (status == 0) {
                    Log.d(ShoulderKeyUtil.TAG, "SoundPool Load Complete, sampleId:" + sampleId);
                    ShoulderKeyUtil.LOADED_SOUND_IDS.add(Integer.valueOf(sampleId));
                    ShoulderKeyUtil.checkSoundPoolLoadCompleted();
                }
            }
        });
    }

    public static void checkSoundPoolLoadCompleted() {
        if (LOADED_SOUND_IDS.size() == 16) {
            mIsSoundPooLoadComplete = true;
        }
    }

    public static void loadSoundResource(Context context) {
        initSoundPool(context);
        ArrayMap<String, Integer> arrayMap = SOUNDS_MAP;
        arrayMap.put(CommonSoundKeys.SOUND_SHOULDER_CLASSIC_CLOSE_L, Integer.valueOf(load(286130183)));
        arrayMap.put(CommonSoundKeys.SOUND_SHOULDER_CLASSIC_CLOSE_R, Integer.valueOf(load(286130184)));
        arrayMap.put(CommonSoundKeys.SOUND_SHOULDER_CLASSIC_OPEN_L, Integer.valueOf(load(286130185)));
        arrayMap.put(CommonSoundKeys.SOUND_SHOULDER_CLASSIC_OPEN_R, Integer.valueOf(load(286130186)));
        arrayMap.put(CommonSoundKeys.SOUND_SHOULDER_BULLET_CLOSE_L, Integer.valueOf(load(286130187)));
        arrayMap.put(CommonSoundKeys.SOUND_SHOULDER_BULLET_CLOSE_R, Integer.valueOf(load(286130188)));
        arrayMap.put(CommonSoundKeys.SOUND_SHOULDER_BULLET_OPEN_L, Integer.valueOf(load(286130189)));
        arrayMap.put(CommonSoundKeys.SOUND_SHOULDER_BULLET_OPEN_R, Integer.valueOf(load(286130190)));
        arrayMap.put(CommonSoundKeys.SOUND_SHOULDER_CURRENT_CLOSE_L, Integer.valueOf(load(286130191)));
        arrayMap.put(CommonSoundKeys.SOUND_SHOULDER_CURRENT_CLOSE_R, Integer.valueOf(load(286130192)));
        arrayMap.put(CommonSoundKeys.SOUND_SHOULDER_CURRENT_OPEN_L, Integer.valueOf(load(286130193)));
        arrayMap.put(CommonSoundKeys.SOUND_SHOULDER_CURRENT_OPEN_R, Integer.valueOf(load(286130194)));
        arrayMap.put(CommonSoundKeys.SOUND_SHOULDER_WIND_CLOSE_L, Integer.valueOf(load(286130179)));
        arrayMap.put(CommonSoundKeys.SOUND_SHOULDER_WIND_CLOSE_R, Integer.valueOf(load(286130180)));
        arrayMap.put(CommonSoundKeys.SOUND_SHOULDER_WIND_OPEN_L, Integer.valueOf(load(286130181)));
        arrayMap.put(CommonSoundKeys.SOUND_SHOULDER_WIND_OPEN_R, Integer.valueOf(load(286130182)));
    }

    private static int load(int resId) {
        SoundPool soundPool = mSoundPool;
        if (soundPool == null) {
            return -1;
        }
        return soundPool.load(mContext, resId, 1);
    }

    public static void playSound(String soundId, boolean isLoop) {
        if (!mIsSoundPooLoadComplete) {
            return;
        }
        ArrayMap<String, Integer> arrayMap = SOUNDS_MAP;
        if (arrayMap.indexOfKey(soundId) >= 0) {
            mSoundPool.play(arrayMap.get(soundId).intValue(), 1.0f, 1.0f, 1, isLoop ? -1 : 0, 0.95f);
        }
    }

    public static void pause(int streamID) {
        mSoundPool.pause(streamID);
    }

    public static void releaseSoundResource() {
        if (mSoundPool != null) {
            Log.d(TAG, "SoundPool release");
            mIsSoundPooLoadComplete = false;
            SOUNDS_MAP.clear();
            LOADED_SOUND_IDS.clear();
            mSoundPool.release();
            mSoundPool = null;
        }
    }

    /* loaded from: classes.dex */
    private class CommonSoundKeys {
        public static final String SOUND_SHOULDER_BULLET_CLOSE_L = "bullet-0-0";
        public static final String SOUND_SHOULDER_BULLET_CLOSE_R = "bullet-1-0";
        public static final String SOUND_SHOULDER_BULLET_OPEN_L = "bullet-0-1";
        public static final String SOUND_SHOULDER_BULLET_OPEN_R = "bullet-1-1";
        public static final String SOUND_SHOULDER_CLASSIC_CLOSE_L = "classic-0-0";
        public static final String SOUND_SHOULDER_CLASSIC_CLOSE_R = "classic-1-0";
        public static final String SOUND_SHOULDER_CLASSIC_OPEN_L = "classic-0-1";
        public static final String SOUND_SHOULDER_CLASSIC_OPEN_R = "classic-1-1";
        public static final String SOUND_SHOULDER_CURRENT_CLOSE_L = "current-0-0";
        public static final String SOUND_SHOULDER_CURRENT_CLOSE_R = "current-1-0";
        public static final String SOUND_SHOULDER_CURRENT_OPEN_L = "current-0-1";
        public static final String SOUND_SHOULDER_CURRENT_OPEN_R = "current-1-1";
        public static final String SOUND_SHOULDER_WIND_CLOSE_L = "wind-0-0";
        public static final String SOUND_SHOULDER_WIND_CLOSE_R = "wind-1-0";
        public static final String SOUND_SHOULDER_WIND_OPEN_L = "wind-0-1";
        public static final String SOUND_SHOULDER_WIND_OPEN_R = "wind-1-1";

        private CommonSoundKeys() {
            ShoulderKeyUtil.this = r1;
        }
    }
}
