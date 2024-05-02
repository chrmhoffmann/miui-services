package com.android.server.audio;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.media.AudioPlaybackConfiguration;
import android.media.AudioRecordingConfiguration;
import android.media.AudioServiceInjector;
import android.media.AudioSystem;
import android.media.MiuiAudioRecord;
import android.media.audiopolicy.AudioMix;
import android.media.audiopolicy.AudioMixingRule;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;
import com.android.internal.notification.SystemNotificationChannels;
import com.android.server.am.BroadcastQueueImpl;
import com.android.server.audio.AudioDeviceBroker;
import com.android.server.audio.AudioParameterClient;
import com.android.server.audio.AudioService;
import com.android.server.audio.dolbyeffect.DolbyEffectController;
import com.android.server.content.SyncManagerStubImpl;
import com.android.server.pm.CloudControlPreinstallService;
import com.miui.base.MiuiStubRegistry;
import com.miui.server.SecurityManagerService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import miui.android.animation.internal.AnimTask;
import miui.tipclose.TipHelperProxy;
import miui.util.AudioManagerHelper;
/* loaded from: classes.dex */
public class AudioServiceStubImpl implements AudioServiceStub {
    public static final String ACTION_VOLUME_BOOST = "miui.intent.action.VOLUME_BOOST";
    private static final String AUDIO_CAMERA_BT_RECORD_SUPPORT = "ro.vendor.audio.camera.bt.record.support";
    private static final String AUDIO_DOLBY_CONTROL_SUPPORT = "vendor.audio.dolby.control.support";
    private static final String CAMERA_AUDIO_HEADSET_STATE = "audio_headset_state";
    private static final String CAMERA_PACKAGE_NAME = "com.android.camera";
    public static final String EXTRA_BOOST_STATE = "volume_boost_state";
    static List<String> HIFI_NOT_SUPPORT_DEVICE_LIST = null;
    private static final String KEY_PERSIST_CUMULATIVE_PLAYBACK_MS = "key_persist_cumulative_playback_ms";
    private static final String KEY_PERSIST_NOTIFICATION_DATE = "key_persist_notification_date";
    private static final String KEY_PERSIST_PLAYBACK_CONTINUOUS_MS = "key_persist_playback_continuous_ms";
    private static final String KEY_PERSIST_PLAYBACK_HIGH_VOICE = "key_persist_playback_high_voice";
    private static final String KEY_PERSIST_PLAYBACK_START_MS = "key_persist_playback_start_ms";
    public static final int LONG_TIME_PROMPT = 2;
    public static final int MAX_VOLUME_LONG_TIME = 3;
    private static final int MAX_VOLUME_VALID_TIME_INTERVAL = 604800000;
    private static final int MIUI_MAX_MUSIC_VOLUME_STEP = 15;
    private static final int MUSIC_ACTIVE_CONTINUOUS_MS_MAX = 3600000;
    public static final int MUSIC_ACTIVE_CONTINUOUS_POLL_PERIOD_MS = 60000;
    private static final int MUSIC_ACTIVE_DATA_REPORT_INTERVAL = 600000;
    private static final int MUSIC_ACTIVE_RETRY_POLL_PERIOD_MS = 30000;
    private static final int NOTE_USB_HEADSET_PLUG = 1397122662;
    public static final int READ_MUSIC_PLAY_BACK_DELAY = 10000;
    private static final String TAG = "AudioServiceStubImpl";
    private int mAudioMode;
    private AudioQueryWeatherService mAudioQueryWeatherService;
    private CloudServiceThread mCloudService;
    private Context mContext;
    private NotificationManager mNm;
    public static boolean mIsAudioPlaybackTriggerSupported = true;
    private static final String AUDIO_PARAMETER_DEFAULT = "sound_transmit_enable=0";
    private static final String[] DEFAULT_AUDIO_PARAMETERS = {AUDIO_PARAMETER_DEFAULT, AUDIO_PARAMETER_DEFAULT};
    private static final String AUDIO_PARAMETER_WT = "sound_transmit_enable=1";
    private static final String AUDIO_PARAMETER_HA = "sound_transmit_enable=6";
    private static final String[] TRANSMIT_AUDIO_PARAMETERS = {AUDIO_PARAMETER_WT, AUDIO_PARAMETER_HA};
    private boolean isSupportPollAudioMicStatus = true;
    private final boolean mIsSupportedCameraRecord = "true".equals(SystemProperties.get(AUDIO_CAMERA_BT_RECORD_SUPPORT));
    private final boolean mIsSupportedDolbyEffectControl = "true".equals(SystemProperties.get(AUDIO_DOLBY_CONTROL_SUPPORT));
    private float[] mPrescaleAbsoluteVolume = {0.5f, 0.7f, 0.85f, 0.9f, 0.95f};
    private String[] mRegisterContentName = {"zen_mode", "notification_sound", "calendar_alert", "notes_alert", "sms_received_sound", "sms_received_sound_slot_1", "sms_received_sound_slot_2", "random_note_mode_random_sound_number", "random_note_mode_sequence_sound_number", "random_note_mode_sequence_time_interval_ms", "random_note_mode_mute_time_interval_ms"};
    private boolean mVolumeBoostEnabled = false;
    int mReceiveNotificationDevice = BroadcastQueueImpl.FLAG_IMMUTABLE;
    private int mNotificationTimes = 0;
    private int cameraToastServiceRiid = -1;
    private LocalDate mNotificationDate = null;
    private long mMusicPlaybackContinuousMs = 0;
    private long mMusicPlaybackContinuousMsTotal = 0;
    public long mPlaybackStartTime = 0;
    public long mPlaybackEndTime = 0;
    private long mStartMs = 0;
    private long mContinuousMs = 0;
    private int mHighLevel = 0;
    public long mCumulativePlaybackStartTime = 0;
    public long mCumulativePlaybackEndTime = 0;
    public boolean markBluetoothheadsetstub = false;
    private final List<AudioParameterClient> mAudioParameterClientList = new ArrayList();
    private AudioParameterClient.ClientDeathListener mParameterClientDeathListener = null;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<AudioServiceStubImpl> {

        /* compiled from: AudioServiceStubImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final AudioServiceStubImpl INSTANCE = new AudioServiceStubImpl();
        }

        public AudioServiceStubImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public AudioServiceStubImpl provideNewInstance() {
            return new AudioServiceStubImpl();
        }
    }

    static {
        ArrayList arrayList = new ArrayList();
        HIFI_NOT_SUPPORT_DEVICE_LIST = arrayList;
        arrayList.add("scorpio");
        HIFI_NOT_SUPPORT_DEVICE_LIST.add("lithium");
    }

    public void startAudioQueryWeatherService(Context context) {
        this.mContext = context;
        AudioQueryWeatherService audioQueryWeatherService = new AudioQueryWeatherService(this.mContext);
        this.mAudioQueryWeatherService = audioQueryWeatherService;
        audioQueryWeatherService.onCreate();
        CloudServiceThread cloudServiceThread = new CloudServiceThread(this.mContext);
        this.mCloudService = cloudServiceThread;
        cloudServiceThread.start();
    }

    public void startPollAudioMicStatus(Context context) {
        Log.d(TAG, "startPollAudioMicStatus");
        if (this.isSupportPollAudioMicStatus) {
            AudioDeviceMoniter.getInstance(context).startPollAudioMicStatus();
        }
    }

    public boolean isSupportSteplessVolume(int stream, String callingPackage) {
        return stream == 3 && !isCtsVerifier(callingPackage);
    }

    public int getMusicStreamVolumeStep(int maxVolume) {
        return maxVolume / 15;
    }

    public int getAbsoluteVolumeIndex(int index, int indexMax) {
        int step = indexMax / AnimTask.MAX_PAGE_SIZE;
        if (index == 0) {
            return 0;
        }
        if (index > 0 && index <= step * 5) {
            BigDecimal bd = new BigDecimal(Math.ceil((index - step) / step));
            int pos = bd.intValue();
            return ((int) (indexMax * this.mPrescaleAbsoluteVolume[pos])) / 10;
        }
        return (indexMax + 5) / 10;
    }

    public int getRingerMode(Context context, int mode) {
        int miuiMode = AudioManagerHelper.getValidatedRingerMode(context, mode);
        Log.d(TAG, "getRingerMode originMode" + mode + " destMode=" + miuiMode);
        return miuiMode;
    }

    public boolean musicVolumeAdjustmentAllowed(int zenMode, int streamAlias, ContentResolver cr) {
        if (streamAlias == 3 && zenMode == 1) {
            return isMuteMusicFromMIUI(cr);
        }
        return false;
    }

    public String getMuteMusicAtSilentKey() {
        return "mute_music_at_silent";
    }

    public boolean enableVoiceVolumeBoost(int dir, boolean isMax, int device, int alias, String pkg, int mode, Context context) {
        if (isCtsVerifier(pkg) || !needEnableVoiceVolumeBoost(dir, isMax, device, alias, mode)) {
            return false;
        }
        if (dir == 1 && !this.mVolumeBoostEnabled) {
            setVolumeBoost(true, context);
            return true;
        } else if (dir != -1 || !this.mVolumeBoostEnabled) {
            return false;
        } else {
            setVolumeBoost(false, context);
            return true;
        }
    }

    public void updateVolumeBoostState(int audioMode, int modeOwnerPid, Context context) {
        if (audioMode == 2 && this.mVolumeBoostEnabled) {
            setVolumeBoost(false, context);
        } else if (audioMode == 0) {
            TipHelperProxy.getInstance().hideTipForPhone();
        }
        this.mAudioMode = audioMode;
        Log.d(TAG, "updateVolumeBoostState audiomode " + this.mAudioMode);
    }

    public void onUpdateAudioMode(int audioMode, String requesterPackage, Context context) {
        this.mAudioMode = audioMode;
        if (audioMode == 2 || audioMode == 3) {
            TipHelperProxy.getInstance().showTipForPhone(false, requesterPackage);
        } else if (audioMode == 0) {
            TipHelperProxy.getInstance().hideTipForPhone();
        }
        Log.d(TAG, "onUpdateAudioMode audiomode " + this.mAudioMode + " package:" + requesterPackage);
    }

    public void enableHifiVolume(int stream, int direction, int oldIndex, int indexMax, Context context) {
        if (shouldAdjustHiFiVolume(stream, direction, oldIndex, indexMax, context)) {
            adjustHiFiVolume(direction, context);
        }
    }

    public void setHifiVolume(Context context, int volume) {
        AudioManagerHelper.setHiFiVolume(context, volume);
    }

    public void showDeviceConnectNotification(final Context context, int device, final boolean isShow) {
        if ((this.mReceiveNotificationDevice & device) != 0 && context != null) {
            if (this.mNm == null) {
                this.mNm = (NotificationManager) context.getSystemService("notification");
            }
            new Handler().post(new Runnable() { // from class: com.android.server.audio.AudioServiceStubImpl.1
                @Override // java.lang.Runnable
                public void run() {
                    if (isShow) {
                        AudioServiceStubImpl.this.createNotification(context);
                    } else {
                        AudioServiceStubImpl.this.mNm.cancel(AudioServiceStubImpl.NOTE_USB_HEADSET_PLUG);
                    }
                }
            });
        }
    }

    public void showVisualEffectNotification(Context context, int uid, int event) {
        AudioServiceInjector.showNotification(uid, event, context);
    }

    public void showVisualEffect(Context context, String action, List<AudioPlaybackConfiguration> apcList, Handler handler) {
        AudioServiceInjector.startAudioVisualIfsatisfiedWith(action, apcList, handler);
        reportAudioStatus(context, apcList);
    }

    public void registerContentObserverForMiui(ContentResolver contentResolver, boolean notifyForDescendents, ContentObserver observer) {
        int i = 0;
        while (true) {
            String[] strArr = this.mRegisterContentName;
            if (i < strArr.length) {
                contentResolver.registerContentObserver(Settings.System.getUriFor(strArr[i]), notifyForDescendents, observer);
                i++;
            } else {
                return;
            }
        }
    }

    public IBinder createMiuiAudioRecord(ParcelFileDescriptor sharedMem, long size) {
        return new MiuiAudioRecord(sharedMem.getFileDescriptor(), size);
    }

    public IBinder createAudioRecordForLoopbackWithClient(ParcelFileDescriptor sharedMem, long size, IBinder token) {
        return new MiuiAudioRecord(sharedMem.getFileDescriptor(), size, token);
    }

    public String getNotificationUri(String type) {
        int SunriseTimeHours = this.mAudioQueryWeatherService.getSunriseTimeHours();
        int SunriseTimeMins = this.mAudioQueryWeatherService.getSunriseTimeMins();
        int SunsetTimeHours = this.mAudioQueryWeatherService.getSunsetTimeHours();
        int SunsetTimeMins = this.mAudioQueryWeatherService.getSunsetTimeMins();
        AudioServiceInjector.setDefaultTimeZoneStatus(this.mAudioQueryWeatherService.getDefaultTimeZoneStatus());
        AudioServiceInjector.setSunriseAndSunsetTime(SunriseTimeHours, SunriseTimeMins, SunsetTimeHours, SunsetTimeMins);
        AudioServiceInjector.checkSunriseAndSunsetTimeUpdate(this.mContext);
        return AudioServiceInjector.getNotificationUri(type);
    }

    public void foldInit() {
        FoldHelper.init();
    }

    public void foldEnable() {
        FoldHelper.enable();
    }

    public void foldDisable() {
        FoldHelper.disable();
    }

    public void startMqsServer(Context context) {
        MQSserver.getInstance(context);
    }

    public static boolean isBluetoothHeadsetDevice(BluetoothClass bluetoothClass) {
        if (bluetoothClass == null) {
            return false;
        }
        int deviceClass = bluetoothClass.getDeviceClass();
        return deviceClass == 1048 || deviceClass == 1028;
    }

    public void setBluetoothHeadset(BluetoothDevice btDevice) {
        BluetoothClass bluetoothClass = btDevice.getBluetoothClass();
        if (bluetoothClass == null) {
            Log.w(TAG, "bluetoothClass is null");
            return;
        }
        this.markBluetoothheadsetstub = isBluetoothHeadsetDevice(bluetoothClass);
        int deviceClass = bluetoothClass.getDeviceClass();
        int majorClass = bluetoothClass.getMajorDeviceClass();
        Log.d(TAG, "majorClass:" + Integer.toHexString(majorClass) + ", deviceClass:" + Integer.toHexString(deviceClass) + ", markBluetoothhead:" + this.markBluetoothheadsetstub);
    }

    public boolean getBluetoothHeadset() {
        return this.markBluetoothheadsetstub;
    }

    public void setStreamMusicOrBluetoothScoIndex(Context context, final int index, final int stream, final int device) {
        new Thread(new Runnable() { // from class: com.android.server.audio.AudioServiceStubImpl.2
            @Override // java.lang.Runnable
            public void run() {
                int i;
                try {
                    Log.d(AudioServiceStubImpl.TAG, "setStreamMusicOrBluetoothScoIndex : stream=" + stream + " index=" + index + " device=" + device);
                    if (stream == 3 && (AudioSystem.DEVICE_OUT_ALL_A2DP_SET.contains(Integer.valueOf(device)) || (i = device) == 2 || i == 67108864 || i == 4 || i == 8)) {
                        SystemProperties.set("sys.volume.stream.music.device." + AudioServiceStubImpl.this.getDeviceToStringForStreamMusic(device), Integer.toString(index / 10));
                    } else if (stream != 6 || !AudioSystem.DEVICE_OUT_ALL_SCO_SET.contains(Integer.valueOf(device))) {
                        Log.e(AudioServiceStubImpl.TAG, "other device");
                    } else {
                        SystemProperties.set("sys.volume.stream.bluetoothsco", Integer.toString(index));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(AudioServiceStubImpl.TAG, "erroe for setStreamMusicOrBluetoothScoIndex");
                }
            }
        }).start();
    }

    public String getDeviceToStringForStreamMusic(int device) {
        Log.d(TAG, "getDeviceToString : device=" + device);
        switch (device) {
            case 2:
                return "speaker";
            case 4:
                return "wired.headset";
            case 8:
                return "wired.headphone";
            case 128:
                return "bluetooth";
            case BroadcastQueueImpl.FLAG_IMMUTABLE /* 67108864 */:
                return "usb.headset";
            default:
                Log.d(TAG, "getDeviceToString : other devices");
                return "other devices";
        }
    }

    private void reportAudioStatus(Context context, List<AudioPlaybackConfiguration> apcList) {
        MQSUtils mqs = new MQSUtils(context);
        mqs.reportAudioVisualDailyUse(apcList);
        if (mqs.needToReport()) {
            mqs.reportAudioButtonStatus();
            mqs.reportVibrateStatus();
        }
    }

    public void createNotification(Context context) {
        this.mNotificationTimes++;
        Intent it = new Intent();
        it.setClassName("com.miui.misound", "com.miui.misound.HeadsetSettingsActivity");
        PendingIntent pit = PendingIntent.getActivity(context, 0, it, BroadcastQueueImpl.FLAG_IMMUTABLE);
        String channel = SystemNotificationChannels.USB;
        Notification.Builder builder = new Notification.Builder(context, channel).setSmallIcon(17303662).setWhen(0L).setOngoing(true).setDefaults(0).setColor(context.getColor(17170460)).setCategory("sys").setVisibility(1).setContentIntent(pit).setContentTitle(context.getString(286196246)).setContentText(context.getString(286196244));
        Notification notify = builder.build();
        this.mNm.notify(NOTE_USB_HEADSET_PLUG, notify);
    }

    private void adjustHiFiVolume(int direction, Context context) {
        int currentHiFiVolume = AudioManagerHelper.getHiFiVolume(context);
        if (direction == -1) {
            AudioManagerHelper.setHiFiVolume(context, currentHiFiVolume - 10);
        } else if (direction == 1 && currentHiFiVolume < 100) {
            AudioManagerHelper.setHiFiVolume(context, currentHiFiVolume + 10);
        }
    }

    private boolean shouldAdjustHiFiVolume(int streamType, int direction, int streamIndex, int maxIndex, Context context) {
        if (!HIFI_NOT_SUPPORT_DEVICE_LIST.contains(Build.DEVICE) && streamType == 3 && AudioManagerHelper.isHiFiMode(context)) {
            int currentHiFiVolume = AudioManagerHelper.getHiFiVolume(context);
            boolean adjustDownHiFiVolume = direction == -1 && currentHiFiVolume > 0;
            boolean adjustUpHiFiVolume = direction == 1 && streamIndex == maxIndex;
            return adjustDownHiFiVolume || adjustUpHiFiVolume;
        }
        return false;
    }

    private boolean needEnableVoiceVolumeBoost(int dir, boolean isMax, int device, int alias, int mode) {
        Log.d(TAG, "needEnableVoiceVolumeBoost" + mode + " ismax=" + isMax + " device=" + device + " alias=" + alias + " dir=" + dir);
        if (mode != 2 || alias != 0 || device != 1 || !"manual".equals(SystemProperties.get("ro.vendor.audio.voice.volume.boost"))) {
            return false;
        }
        if (dir == 1 && isMax) {
            return true;
        }
        return dir == -1 && isMax;
    }

    private void setVolumeBoost(boolean boostEnabled, Context context) {
        AudioManager am = (AudioManager) context.getSystemService("audio");
        String params = "voice_volume_boost=" + (boostEnabled ? "true" : "false");
        Log.d(TAG, "params:" + params);
        am.setParameters(params);
        this.mVolumeBoostEnabled = boostEnabled;
        sendVolumeBoostBroadcast(boostEnabled, context);
    }

    private void sendVolumeBoostBroadcast(boolean boostEnabled, Context context) {
        long ident = Binder.clearCallingIdentity();
        try {
            Intent intent = new Intent(ACTION_VOLUME_BOOST);
            intent.putExtra(EXTRA_BOOST_STATE, boostEnabled);
            context.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private boolean isCtsVerifier(String callingPackage) {
        if (callingPackage != null && (callingPackage.startsWith("com.android.cts") || "android.media.cts".equals(callingPackage))) {
            return true;
        }
        if (callingPackage != null && callingPackage.startsWith("com.google.android.gts")) {
            return true;
        }
        if (callingPackage != null && callingPackage.startsWith("android.media.audio.cts")) {
            return true;
        }
        return false;
    }

    public void updateNotificationMode(Context context) {
        AudioServiceInjector.updateNotificationMode(context);
    }

    private boolean isMuteMusicFromMIUI(ContentResolver cr) {
        int muteMusic = Settings.System.getIntForUser(cr, "mute_music_at_silent", 0, -3);
        return muteMusic == 1;
    }

    public void adjustDefaultStreamVolumeForMiui(int[] defaultStreamVolume) {
        AudioServiceInjector.adjustDefaultStreamVolume(defaultStreamVolume);
    }

    public boolean isAudioPlaybackTriggerSupported() {
        return mIsAudioPlaybackTriggerSupported;
    }

    public void onShowHearingProtectionNotification(Context cx, int msgId) {
        if (msgId == 3) {
            long j = this.mCumulativePlaybackEndTime;
            long j2 = this.mCumulativePlaybackStartTime;
            long timeInterval = j - j2;
            if (j2 != 0 && timeInterval > 604800000) {
                Log.d(TAG, "MAX_VOLUME_LONG_TIME notification condition is not met ");
                return;
            }
        }
        if (msgId == 2) {
            LocalDate localDate = LocalDate.now();
            if (localDate.equals(this.mNotificationDate)) {
                Log.d(TAG, "do not remind on the day");
                return;
            } else {
                Log.d(TAG, "first reminder of the day");
                this.mNotificationDate = localDate;
            }
        }
        startHearingProtectionService(cx, msgId);
    }

    private void startHearingProtectionService(Context cx, int msgId) {
        try {
            Intent intent = new Intent();
            intent.setAction("com.miui.misound.hearingprotection.notification");
            intent.setComponent(new ComponentName("com.miui.misound", "com.miui.misound.hearingprotection.HearingProtectionService"));
            intent.putExtra("notificationId", msgId);
            cx.startForegroundService(intent);
        } catch (Exception e) {
            Log.e(TAG, "fail to start HearingProtectionService");
        }
    }

    private ComponentName transportDataToService(Context cx, long beginTime, long endTime, boolean isHigh) {
        try {
            Intent intent = new Intent();
            intent.setAction("com.miui.misound.write.data");
            intent.setComponent(new ComponentName("com.miui.misound", "com.miui.misound.hearingprotection.HearingProtectionService"));
            intent.putExtra("beginMillis", beginTime);
            intent.putExtra("endMillis", endTime);
            intent.putExtra("isHighPitch", isHigh);
            return cx.startForegroundService(intent);
        } catch (Exception e) {
            Log.e(TAG, "fail to transport data to service");
            return null;
        }
    }

    public void readPlaybackMsSettings(Context cx) {
        this.mStartMs = Settings.Global.getLong(cx.getContentResolver(), KEY_PERSIST_PLAYBACK_START_MS, 0L);
        this.mContinuousMs = Settings.Global.getLong(cx.getContentResolver(), KEY_PERSIST_PLAYBACK_CONTINUOUS_MS, 0L);
        this.mHighLevel = Settings.Global.getInt(cx.getContentResolver(), KEY_PERSIST_PLAYBACK_HIGH_VOICE, 0);
        this.mCumulativePlaybackStartTime = Settings.Global.getLong(this.mContext.getContentResolver(), KEY_PERSIST_CUMULATIVE_PLAYBACK_MS, 0L);
        String dateTime = Settings.Global.getString(cx.getContentResolver(), KEY_PERSIST_NOTIFICATION_DATE);
        if (dateTime != null) {
            this.mNotificationDate = LocalDate.parse(dateTime, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        }
        Log.i(TAG, "readPlaybackMsSettings: startMs: " + this.mStartMs + " continuousMs: " + this.mContinuousMs + " highLevel: " + this.mHighLevel + " mNotificationDate=" + this.mNotificationDate + " mCumulativePlaybackStartTime: " + this.mCumulativePlaybackStartTime);
    }

    public boolean insertPlaybackMsToHealth(Context cx) {
        if (this.mStartMs != 0) {
            long j = this.mContinuousMs;
            if (j != 0) {
                ComponentName componentName = transportDataToService(cx, j, j + j, this.mHighLevel == 1);
                if (componentName == null) {
                    return false;
                }
                persistPlaybackMsToSettings(cx, true, 0, "readPlaybackMsSettings");
                return true;
            }
        }
        return true;
    }

    public void persistPlaybackMsToSettings(Context cx, boolean isNeedClearData, int highLevel, String from) {
        Log.i(TAG, "persistPlaybackMsToSettings: isNeedClearData: " + isNeedClearData + " mPlaybackStartTime: " + this.mPlaybackStartTime + " mMusicPlaybackContinuousMsTotal: " + this.mMusicPlaybackContinuousMsTotal + " highLevel: " + highLevel + " from: " + from);
        long j = 0;
        Settings.Global.putLong(cx.getContentResolver(), KEY_PERSIST_PLAYBACK_START_MS, isNeedClearData ? 0L : this.mPlaybackStartTime);
        ContentResolver contentResolver = cx.getContentResolver();
        if (!isNeedClearData) {
            j = this.mMusicPlaybackContinuousMsTotal;
        }
        Settings.Global.putLong(contentResolver, KEY_PERSIST_PLAYBACK_CONTINUOUS_MS, j);
        Settings.Global.putInt(cx.getContentResolver(), KEY_PERSIST_PLAYBACK_HIGH_VOICE, highLevel);
    }

    private void persistCumulativePlaybackStartMsToSettings() {
        Log.d(TAG, "persistCumulativePlaybackStartMsToSettings: mCumulativePlaybackStartTime: " + this.mCumulativePlaybackStartTime);
        Settings.Global.putLong(this.mContext.getContentResolver(), KEY_PERSIST_CUMULATIVE_PLAYBACK_MS, this.mCumulativePlaybackStartTime);
    }

    private void persistNotificationDateToSettings(Context cx, LocalDate localDate) {
        Log.d(TAG, "persistNotificationDateToSettings: localDate: " + localDate);
        Settings.Global.putString(cx.getContentResolver(), KEY_PERSIST_NOTIFICATION_DATE, localDate.toString());
    }

    /* JADX WARN: Multi-variable type inference failed */
    public boolean onCheckMusicPlaybackContinuous(Context cx, int device, boolean isHigh, Set<Integer> safeMediaVolumeDevices) {
        long j;
        boolean isBluetoothHeadset = getBluetoothHeadset();
        boolean isBluetoothspeaker = (device == 128 || device == 256) && !isBluetoothHeadset;
        if (!safeMediaVolumeDevices.contains(Integer.valueOf(device))) {
            j = 0;
        } else if (!isBluetoothspeaker) {
            if (AudioSystem.isStreamActive(3, 0)) {
                this.mMusicPlaybackContinuousMs += SecurityManagerService.LOCK_TIME_OUT;
                this.mMusicPlaybackContinuousMsTotal += SecurityManagerService.LOCK_TIME_OUT;
                Log.d(TAG, "music isActive ,start loop =" + this.mMusicPlaybackContinuousMs + "，MUSIC_ACTIVE_CONTINUOUS_MS_MAX = " + MUSIC_ACTIVE_CONTINUOUS_MS_MAX);
                if (this.mMusicPlaybackContinuousMs >= SyncManagerStubImpl.SYNC_DELAY_ON_DISALLOW_METERED) {
                    Log.d(TAG, "music isActive max ,post warning dialog");
                    this.mMusicPlaybackContinuousMs = 0L;
                    persistNotificationDateToSettings(cx, LocalDate.now());
                    onShowHearingProtectionNotification(cx, 2);
                }
                if (this.mMusicPlaybackContinuousMsTotal >= 600000) {
                    Log.d(TAG, "need to report hearing data");
                    long j2 = this.mPlaybackStartTime;
                    transportDataToService(cx, j2, j2 + this.mMusicPlaybackContinuousMsTotal, isHigh);
                    this.mPlaybackStartTime = System.currentTimeMillis();
                    this.mMusicPlaybackContinuousMsTotal = 0L;
                }
                persistPlaybackMsToSettings(cx, false, isHigh, "music isActive ,start loop");
                return true;
            }
            boolean isRencentActive = AudioSystem.isStreamActive(3, (int) MUSIC_ACTIVE_RETRY_POLL_PERIOD_MS);
            if (isRencentActive) {
                this.mMusicPlaybackContinuousMs += SecurityManagerService.LOCK_TIME_OUT;
                this.mMusicPlaybackContinuousMsTotal += SecurityManagerService.LOCK_TIME_OUT;
                Log.d(TAG, "isRencentActive true,need retry again");
                persistPlaybackMsToSettings(cx, false, isHigh ? 1 : 0, "isRencentActive true");
                return true;
            }
            Log.d(TAG, "isRencentActive false,reset time " + this.mMusicPlaybackContinuousMs);
            updatePlaybackTime(false);
            long j3 = this.mPlaybackStartTime;
            transportDataToService(cx, j3, j3 + this.mMusicPlaybackContinuousMsTotal, isHigh);
            persistPlaybackMsToSettings(cx, true, 0, "isRencentActive false");
            this.mMusicPlaybackContinuousMs = 0L;
            this.mMusicPlaybackContinuousMsTotal = 0L;
            return false;
        } else {
            j = 0;
        }
        Log.d(TAG, "device is not support,reset time calculation ");
        updatePlaybackTime(false);
        long j4 = this.mPlaybackStartTime;
        transportDataToService(cx, j4, j4 + this.mMusicPlaybackContinuousMsTotal, isHigh);
        persistPlaybackMsToSettings(cx, true, 0, "device is not support");
        this.mMusicPlaybackContinuousMs = j;
        this.mMusicPlaybackContinuousMsTotal = j;
        return false;
    }

    public void updateCumulativePlaybackTime(boolean start) {
        long systemTime = System.currentTimeMillis();
        Log.d(TAG, " updateCumulativePlaybackTime start=" + start + " systemTime=" + systemTime);
        if (start) {
            this.mCumulativePlaybackStartTime = systemTime;
            persistCumulativePlaybackStartMsToSettings();
            return;
        }
        this.mCumulativePlaybackEndTime = systemTime;
    }

    public void updatePlaybackTime(boolean start) {
        long systemTime = System.currentTimeMillis();
        if (start) {
            this.mPlaybackStartTime = systemTime;
        } else {
            this.mPlaybackEndTime = systemTime;
        }
        Log.d(TAG, " updatePlaybackTime start=" + start + " systemTime=" + systemTime + " throd=" + Math.abs(this.mPlaybackEndTime - this.mPlaybackStartTime));
    }

    public boolean onTrigger(List<AudioPlaybackConfiguration> configs) {
        if (configs == null) {
            return false;
        }
        for (AudioPlaybackConfiguration config : configs) {
            if (isMusicPlayerActive(config)) {
                return true;
            }
        }
        return false;
    }

    private boolean isMusicPlayerActive(AudioPlaybackConfiguration apc) {
        if (apc == null) {
            return false;
        }
        return (apc.getAudioAttributes().getUsage() == 1 || apc.getAudioAttributes().getVolumeControlStream() == 3) && apc.getPlayerState() == 2;
    }

    public void customMinStreamVolume(int[] minStreamVolume) {
        AudioServiceInjector.customMinStreamVolume(minStreamVolume);
    }

    public int[] getAudioPolicyMatchUids(HashMap<IBinder, AudioService.AudioPolicyProxy> policys) {
        List<Integer> uidList = new ArrayList<>();
        for (AudioService.AudioPolicyProxy policy : policys.values()) {
            if (policy.mProjection != null) {
                Iterator it = policy.getMixes().iterator();
                while (it.hasNext()) {
                    AudioMix mix = (AudioMix) it.next();
                    int[] matchUidArray = getIntPredicates(4, mix, new ToIntFunction() { // from class: com.android.server.audio.AudioServiceStubImpl$$ExternalSyntheticLambda0
                        @Override // java.util.function.ToIntFunction
                        public final int applyAsInt(Object obj) {
                            int intProp;
                            intProp = ((AudioMixingRule.AudioMixMatchCriterion) obj).getIntProp();
                            return intProp;
                        }
                    });
                    List<Integer> listCollect = (List) Arrays.stream(matchUidArray).boxed().collect(Collectors.toList());
                    uidList.addAll(listCollect);
                }
            }
        }
        int[] arrays = uidList.stream().mapToInt(new ToIntFunction() { // from class: com.android.server.audio.AudioServiceStubImpl$$ExternalSyntheticLambda1
            @Override // java.util.function.ToIntFunction
            public final int applyAsInt(Object obj) {
                return ((Integer) obj).intValue();
            }
        }).toArray();
        return arrays;
    }

    private int[] getIntPredicates(final int rule, AudioMix mix, ToIntFunction<AudioMixingRule.AudioMixMatchCriterion> getPredicate) {
        return mix.getRule().getCriteria().stream().filter(new Predicate() { // from class: com.android.server.audio.AudioServiceStubImpl$$ExternalSyntheticLambda2
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return AudioServiceStubImpl.lambda$getIntPredicates$1(rule, (AudioMixingRule.AudioMixMatchCriterion) obj);
            }
        }).mapToInt(getPredicate).toArray();
    }

    public static /* synthetic */ boolean lambda$getIntPredicates$1(int rule, AudioMixingRule.AudioMixMatchCriterion criterion) {
        return criterion.getRule() == rule;
    }

    public void startCameraRecordService(Context context, AudioRecordingConfiguration audioConfig, int eventType, int riidNow) {
        if (this.mIsSupportedCameraRecord && audioConfig != null) {
            if (riidNow == this.cameraToastServiceRiid) {
                Log.d(TAG, "the riid is exist, do not startCameraRecordService again  " + riidNow);
                return;
            }
            this.cameraToastServiceRiid = riidNow;
            int cameraAudioHeadsetState = Settings.Global.getInt(context.getContentResolver(), CAMERA_AUDIO_HEADSET_STATE, -1);
            if (audioConfig.getClientPackageName().equals("com.android.camera") && cameraAudioHeadsetState == 1 && audioConfig.getClientAudioSource() == 5) {
                try {
                    Intent intent = new Intent();
                    intent.setComponent(new ComponentName("com.miui.audiomonitor", "com.miui.audiomonitor.MiuiCameraBTRecordService"));
                    intent.putExtra("packageName", "com.android.camera");
                    if (audioConfig.getAudioDevice() != null) {
                        int deviceType = audioConfig.getAudioDevice().getType();
                        intent.putExtra("deviceType", deviceType);
                        Log.d(TAG, String.format("packageName %s deviceType %d eventType %d", "com.android.camera", Integer.valueOf(deviceType), Integer.valueOf(eventType)));
                    }
                    intent.putExtra("eventType", eventType);
                    context.startForegroundService(intent);
                } catch (Exception e) {
                    Log.e(TAG, "fail to startCameraRecordService ");
                }
            }
        }
    }

    public void stopCameraRecordService(Context context, AudioRecordingConfiguration audioConfig, int riid) {
        if (this.mIsSupportedCameraRecord) {
            Log.d(TAG, "stopCameraRecordService riidNow " + riid);
            if (audioConfig != null && audioConfig.getClientPackageName().equals("com.android.camera")) {
                this.cameraToastServiceRiid = -1;
            }
        }
    }

    public void handleSpeakerChanged(Context context, int pid, boolean speakerOn) {
        Log.d(TAG, "handleSpeakerChanged audiomode " + this.mAudioMode);
        int i = this.mAudioMode;
        if (i == 2 || i == 3) {
            AudioServiceInjector.handleSpeakerChanged(pid, speakerOn, Binder.getCallingUid());
        }
    }

    public void updateAudioParameterClients(IBinder binder, String targetParameter) {
        if (Arrays.asList(TRANSMIT_AUDIO_PARAMETERS).contains(targetParameter)) {
            addAudioParameterClient(binder, targetParameter);
        } else if (Arrays.asList(DEFAULT_AUDIO_PARAMETERS).contains(targetParameter)) {
            removeAudioParameterClient(binder, targetParameter, true);
        }
    }

    private AudioParameterClient getAudioParameterClient(IBinder binder, String targetParameter) {
        synchronized (this.mAudioParameterClientList) {
            Iterator<AudioParameterClient> iterator = this.mAudioParameterClientList.listIterator();
            while (iterator.hasNext()) {
                AudioParameterClient client = iterator.next();
                if (client.getBinder() == binder && client.getTargetParameter().equals(targetParameter)) {
                    return client;
                }
            }
            return null;
        }
    }

    private AudioParameterClient removeAudioParameterClient(IBinder binder, String targetParameter, boolean unregister) {
        AudioParameterClient client = getAudioParameterClient(binder, targetParameter);
        synchronized (this.mAudioParameterClientList) {
            if (client != null) {
                this.mAudioParameterClientList.remove(client);
                if (unregister) {
                    client.unregisterDeathRecipient();
                }
                return client;
            }
            return null;
        }
    }

    private AudioParameterClient addAudioParameterClient(IBinder binder, String targetParameter) {
        AudioParameterClient client;
        synchronized (this.mAudioParameterClientList) {
            client = removeAudioParameterClient(binder, targetParameter, false);
            if (this.mParameterClientDeathListener == null) {
                this.mParameterClientDeathListener = new AudioParameterClient.ClientDeathListener() { // from class: com.android.server.audio.AudioServiceStubImpl$$ExternalSyntheticLambda3
                    @Override // com.android.server.audio.AudioParameterClient.ClientDeathListener
                    public final void onBinderDied(IBinder iBinder, String str) {
                        AudioServiceStubImpl.this.m491x843eb950(iBinder, str);
                    }
                };
            }
            if (client == null) {
                client = new AudioParameterClient(binder, targetParameter);
                client.setClientDiedListener(this.mParameterClientDeathListener);
                client.registerDeathRecipient();
            }
            this.mAudioParameterClientList.add(0, client);
        }
        return client;
    }

    /* renamed from: lambda$addAudioParameterClient$2$com-android-server-audio-AudioServiceStubImpl */
    public /* synthetic */ void m491x843eb950(IBinder diedBinder, String diedTargetParameter) {
        int i = 0;
        while (true) {
            String[] strArr = TRANSMIT_AUDIO_PARAMETERS;
            if (i < strArr.length) {
                if (strArr[i].equals(diedTargetParameter)) {
                    removeAudioParameterClient(diedBinder, diedTargetParameter, true);
                    if (this.mAudioParameterClientList.size() > 0) {
                        AudioSystem.setParameters(this.mAudioParameterClientList.get(0).getTargetParameter());
                    } else {
                        AudioSystem.setParameters(DEFAULT_AUDIO_PARAMETERS[i]);
                    }
                }
                i++;
            } else {
                return;
            }
        }
    }

    public void startDolbyEffectController(Context context) {
        if (this.mIsSupportedDolbyEffectControl) {
            Log.d(TAG, "startDolbyEffectControl");
            DolbyEffectController.getInstance(context).init();
        }
    }

    public void notifyBtStateToDolbyEffectController(Context context, AudioDeviceBroker.BtDeviceInfo btInfo) {
        if (this.mIsSupportedDolbyEffectControl) {
            Bundle bundle = new Bundle();
            bundle.putString(CloudControlPreinstallService.ConnectEntity.DEVICE, btInfo.mDevice.getAddress());
            bundle.putString("profile", BluetoothProfile.getProfileName(btInfo.mProfile));
            bundle.putString("state", BluetoothProfile.getConnectionStateName(btInfo.mState));
            DolbyEffectController.getInstance(context).btStateChangedFromDeviceBroker(bundle);
        }
    }

    public void notifyVolumeChangedToDolbyEffectController(Context context, Intent volumeChanged) {
        if (this.mIsSupportedDolbyEffectControl) {
            DolbyEffectController.getInstance(context).receiveVolumeChanged(volumeChanged);
        }
    }
}
