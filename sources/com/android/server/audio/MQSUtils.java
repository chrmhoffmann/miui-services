package com.android.server.audio;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.AudioPlaybackConfiguration;
import android.media.Spatializer;
import android.os.Bundle;
import android.os.SystemProperties;
import android.provider.Settings;
import android.server.am.SplitScreenReporter;
import android.util.Slog;
import com.android.server.MiuiBatteryStatsService;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import miui.os.Build;
/* loaded from: classes.dex */
public class MQSUtils {
    private static final String AUDIO_EVENT_ID = "31000000086";
    private static final String AUDIO_EVENT_NAME = "audio_button";
    private static final int AUDIO_VISUAL_ENABLED = 1;
    private static final int FLAG_NON_ANONYMOUS = 2;
    private static final String HAPTIC_FEEDBACK_INFINITE_INTENSITY = "haptic_feedback_infinite_intensity";
    private static final String IS_HEARING_ASSIST_SUPPORT = "sound_transmit_ha_support";
    private static final String IS_TRANSMIT_SUPPORT = "sound_transmit_support";
    private static final String KEY_PARAM_RINGTONE_DEVICE = "set_spk_ring_filter_mask";
    private static final String ONE_TRACK_ACTION = "onetrack.action.TRACK_EVENT";
    private static final String PACKAGE_NAME = "com.miui.analytics";
    private static final String PARAM_RINGTONE_DEVICE_OFF = "set_spk_ring_filter_mask=3";
    private static final String PARAM_RINGTONE_DEVICE_ON = "set_spk_ring_filter_mask=0";
    private static final int SOUND_ASSIST_ENABLED = 1;
    private static final String SUPPORT_HEARING_ASSIST = "sound_transmit_ha_support=true";
    private static final String SUPPORT_TRANSMIT = "sound_transmit_support=true";
    private static final String TAG = "MiSound.MQSUtils";
    private static final String VIBRATOR_EVENT_ID = "31000000089";
    private static final String VIBRATOR_EVENT_NAME = "haptic_status";
    private static int day = 0;
    private static int month = 0;
    private static final String none = "none";
    private static int year;
    private Context mContext;
    private static String mMqsModuleId = "mqs_audio_data_21031000";
    private static float VIBRATION_DEFAULT_INFINITE_INTENSITY = 1.0f;
    private static final Set<String> mMusicWhiteList = new HashSet(Arrays.asList("com.netease.cloudmusic", "com.tencent.qqmusic", "com.iloen.melon", "mp3.player.freemusic", "com.kugou.android", "cn.kuwo.player", "com.google.android.apps.youtube.music", "com.tencent.blackkey", "cmccwm.mobilemusic", "com.migu.music.mini", "com.ting.mp3.android", "com.blueocean.musicplayer", "com.tencent.ibg.joox", "com.kugou.android.ringtone", "com.shoujiduoduo.dj", "com.spotify.music", "com.shoujiduoduo.ringtone", "com.hiby.music", "com.miui.player", "com.google.android.music", "com.tencent.ibg.joox", "com.skysoft.kkbox.android", "com.sofeh.android.musicstudio3", "com.gamestar.perfectpiano", "com.opalastudios.pads", "com.magix.android.mmjam", "com.musicplayer.playermusic", "com.gaana", "com.maxmpz.audioplayer", "com.melodis.midomiMusicIdentifier.freemium", "com.mixvibes.remixlive", "com.starmakerinteractive.starmaker", "com.smule.singandroid", "com.djit.apps.stream", "tunein.service", "com.shazam.android", "com.jangomobile.android", "com.pandoralite", "com.tube.hqmusic", "com.amazon.avod.thirdpartyclient", "com.atmusic.app", "com.rubycell.pianisthd", "com.agminstruments.drumpadmachine", "com.playermusic.musicplayerapp", "com.famousbluemedia.piano", "com.apple.android.music", "mb32r.musica.gratis.music.player.free.download", "com.famousbluemedia.yokee", "com.ss.android.ugc.trill"));

    public MQSUtils(Context context) {
        this.mContext = context;
    }

    public boolean needToReport() {
        Calendar calendar = Calendar.getInstance();
        if (year == calendar.get(1) && month == calendar.get(2) + 1 && day == calendar.get(5)) {
            return false;
        }
        year = calendar.get(1);
        month = calendar.get(2) + 1;
        day = calendar.get(5);
        Slog.d(TAG, "needToReport year: " + year + "month: " + month + "day: " + day);
        return true;
    }

    private boolean checkWirelessTransmissionSupport() {
        AudioManager audioManager = (AudioManager) this.mContext.getSystemService("audio");
        String transmitSupport = audioManager.getParameters(IS_TRANSMIT_SUPPORT);
        return transmitSupport != null && transmitSupport.length() >= 1 && SUPPORT_TRANSMIT.equals(transmitSupport);
    }

    private boolean checkHearingAssistSupport() {
        AudioManager audioManager = (AudioManager) this.mContext.getSystemService("audio");
        String hearingAssistSupport = audioManager.getParameters(IS_HEARING_ASSIST_SUPPORT);
        return hearingAssistSupport != null && hearingAssistSupport.length() >= 1 && SUPPORT_HEARING_ASSIST.equals(hearingAssistSupport);
    }

    private boolean checkVoiceprintNoiseReductionSupport() {
        return SystemProperties.getBoolean("ro.vendor.audio.voip.assistant", false);
    }

    private String checkAudioVisualStatus() {
        if (!SystemProperties.getBoolean("ro.vendor.audio.sfx.audiovisual", false)) {
            Slog.d(TAG, "device not support AudioVisual");
            return none;
        } else if (Settings.Global.getInt(this.mContext.getContentResolver(), "audio_visual_screen_lock_on", 0) == 1) {
            return "open";
        } else {
            return "close";
        }
    }

    private String checkHarmankardonStatus() {
        if (!SystemProperties.getBoolean("ro.vendor.audio.sfx.harmankardon", false)) {
            Slog.d(TAG, "device not support harmankardon");
            return none;
        }
        int flag = Settings.Global.getInt(this.mContext.getContentResolver(), "settings_system_harman_kardon_enable", 0);
        if (1 == flag) {
            return "open";
        }
        return "close";
    }

    private String checkMultiAppVolumeStatus() {
        int status = Settings.Global.getInt(this.mContext.getContentResolver(), "sound_assist_key", 0);
        if (status == 1) {
            return "open";
        }
        return "close";
    }

    private String checkHIFIStatus() {
        if (!SystemProperties.getBoolean("ro.vendor.audio.hifi", false)) {
            Slog.d(TAG, "device not support HIFI");
            return none;
        }
        boolean status = SystemProperties.getBoolean("persist.vendor.audio.hifi", false);
        Slog.i(TAG, "HiFi Switch status is :" + status);
        if (status) {
            return "open";
        }
        return "close";
    }

    private String checkMultiSoundStatus() {
        int status = Settings.Global.getInt(this.mContext.getContentResolver(), "key_ignore_music_focus_req", 0);
        if (status == 1) {
            return "open";
        }
        return "close";
    }

    private String checkAllowSpeakerToRing() {
        if (!SystemProperties.getBoolean("ro.vendor.audio.ring.filter", false)) {
            Slog.d(TAG, "device not support AllowSpeakerToRing");
            return none;
        }
        AudioManager mAudioManager = (AudioManager) this.mContext.getSystemService("audio");
        String paramStr = mAudioManager.getParameters(KEY_PARAM_RINGTONE_DEVICE);
        if (PARAM_RINGTONE_DEVICE_ON.equals(paramStr)) {
            return "open";
        }
        if (PARAM_RINGTONE_DEVICE_OFF.equals(paramStr)) {
            return "close";
        }
        return "Undefined";
    }

    private String checkEarsCompensationStatus() {
        if (!SystemProperties.getBoolean("ro.vendor.audio.sfx.earadj", false)) {
            Slog.d(TAG, "device not support EarsCompensation");
            return none;
        }
        String status = SystemProperties.get("persist.vendor.audio.ears.compensation.state", "");
        Slog.i(TAG, "ears compensation status is :" + status);
        if (status.equals(SplitScreenReporter.ACTION_ENTER_SPLIT)) {
            return "open";
        }
        return "close";
    }

    private String checkDolbySwitchStatus() {
        if (!SystemProperties.getBoolean("ro.vendor.audio.dolby.dax.support", false)) {
            Slog.d(TAG, "device not support dolby audio");
            return none;
        }
        boolean status = SystemProperties.getBoolean("persist.vendor.audio.misound.disable", false);
        Slog.i(TAG, "Dolby Switch status is :" + status);
        if (status) {
            return "open";
        }
        return "close";
    }

    private String check3DAudioSwitchStatus() {
        AudioManager mAudioManager = (AudioManager) this.mContext.getSystemService("audio");
        Spatializer mSpatializer = mAudioManager.getSpatializer();
        boolean z = true;
        if (mSpatializer.getImmersiveAudioLevel() > 0) {
            boolean spatializer_enabled = SystemProperties.getBoolean("ro.audio.spatializer_enabled", false);
            if (!spatializer_enabled) {
                Slog.d(TAG, "device not support 3D audio");
                return none;
            }
            if (Settings.Global.getInt(this.mContext.getContentResolver(), "spatial_audio_feature_enable", 0) != 1) {
                z = false;
            }
            boolean status = z;
            Slog.i(TAG, "3D Audio Switch status is :" + status);
            return status ? "open" : "close";
        }
        int flag = SystemProperties.getInt("ro.vendor.audio.feature.spatial", 0);
        if (flag == 0 || 1 == flag) {
            Slog.d(TAG, "device not support 3D audio");
            return none;
        }
        boolean status2 = SystemProperties.getBoolean("persist.vendor.audio.3dsurround.enable", false);
        Slog.i(TAG, "3D Audio Switch status is :" + status2);
        return status2 ? "open" : "close";
    }

    private String checkSpatialAudioSwitchStatus() {
        AudioManager mAudioManager = (AudioManager) this.mContext.getSystemService("audio");
        Spatializer mSpatializer = mAudioManager.getSpatializer();
        boolean status = false;
        if (mSpatializer.getImmersiveAudioLevel() > 0) {
            int mode = mSpatializer.getDesiredHeadTrackingMode();
            if (mode == 1) {
                status = true;
            }
            Slog.i(TAG, "Spatial Audio Switch status is :" + status);
            return status ? "open" : "close";
        }
        int flag = SystemProperties.getInt("ro.vendor.audio.feature.spatial", 0);
        if (flag == 0 || 2 == flag) {
            Slog.d(TAG, "device not support spatial audio");
            return none;
        }
        boolean status2 = SystemProperties.getBoolean("persist.vendor.audio.spatial.enable", false);
        Slog.i(TAG, "Spatial Audio Switch status is :" + status2);
        return status2 ? "open" : "close";
    }

    private String checkMisoundStatus() {
        String status = SystemProperties.get("persist.vendor.audio.sfx.hd.music.state", "");
        Slog.i(TAG, "misound status is :" + status);
        if (status.equals(SplitScreenReporter.ACTION_ENTER_SPLIT)) {
            return "open";
        }
        if (status.equals("0")) {
            return "close";
        }
        return none;
    }

    private String checkMisoundHeadphoneType() {
        String type = SystemProperties.get("persist.vendor.audio.sfx.hd.type", "");
        Slog.i(TAG, "misound headphone type is :" + type);
        return type;
    }

    private String checkMisoundEqStatus() {
        String status;
        String eq = SystemProperties.get("persist.vendor.audio.sfx.hd.eq", "");
        Slog.i(TAG, "misound eq is :" + eq);
        if (eq.equals("")) {
            status = "null";
        } else if (eq.equals("0.000000,0.000000,0.000000,0.000000,0.000000,0.000000,0.000000")) {
            status = "close";
        } else {
            status = "open";
        }
        Slog.i(TAG, "misound eq status is :" + status);
        return status;
    }

    /* JADX WARN: Can't wrap try/catch for region: R(22:(4:(2:90|91)(2:94|(2:96|97)(26:98|99|169|100|101|(3:103|164|104)(2:107|108)|109|154|110|111|(2:113|114)(1:115)|116|117|(2:119|120)(1:121)|122|123|(2:125|126)(1:127)|128|129|(2:131|132)(1:133)|134|(1:136)|137|156|138|176))|156|138|176)|169|100|101|(0)(0)|109|154|110|111|(0)(0)|116|117|(0)(0)|122|123|(0)(0)|128|129|(0)(0)|134|(0)|137) */
    /* JADX WARN: Code restructure failed: missing block: B:140:0x0220, code lost:
        r0 = e;
     */
    /* JADX WARN: Removed duplicated region for block: B:103:0x01a8  */
    /* JADX WARN: Removed duplicated region for block: B:107:0x01be  */
    /* JADX WARN: Removed duplicated region for block: B:113:0x01cc  */
    /* JADX WARN: Removed duplicated region for block: B:115:0x01d1 A[Catch: Exception -> 0x0220, TryCatch #0 {Exception -> 0x0220, blocks: (B:110:0x01c4, B:114:0x01cd, B:115:0x01d1, B:116:0x01d5, B:120:0x01de, B:121:0x01e2, B:122:0x01e6, B:126:0x01ef, B:127:0x01f3, B:128:0x01f7, B:132:0x0200, B:133:0x0204, B:134:0x0208, B:136:0x0211), top: B:154:0x01c4 }] */
    /* JADX WARN: Removed duplicated region for block: B:119:0x01dd  */
    /* JADX WARN: Removed duplicated region for block: B:121:0x01e2 A[Catch: Exception -> 0x0220, TryCatch #0 {Exception -> 0x0220, blocks: (B:110:0x01c4, B:114:0x01cd, B:115:0x01d1, B:116:0x01d5, B:120:0x01de, B:121:0x01e2, B:122:0x01e6, B:126:0x01ef, B:127:0x01f3, B:128:0x01f7, B:132:0x0200, B:133:0x0204, B:134:0x0208, B:136:0x0211), top: B:154:0x01c4 }] */
    /* JADX WARN: Removed duplicated region for block: B:125:0x01ee  */
    /* JADX WARN: Removed duplicated region for block: B:127:0x01f3 A[Catch: Exception -> 0x0220, TryCatch #0 {Exception -> 0x0220, blocks: (B:110:0x01c4, B:114:0x01cd, B:115:0x01d1, B:116:0x01d5, B:120:0x01de, B:121:0x01e2, B:122:0x01e6, B:126:0x01ef, B:127:0x01f3, B:128:0x01f7, B:132:0x0200, B:133:0x0204, B:134:0x0208, B:136:0x0211), top: B:154:0x01c4 }] */
    /* JADX WARN: Removed duplicated region for block: B:131:0x01ff  */
    /* JADX WARN: Removed duplicated region for block: B:133:0x0204 A[Catch: Exception -> 0x0220, TryCatch #0 {Exception -> 0x0220, blocks: (B:110:0x01c4, B:114:0x01cd, B:115:0x01d1, B:116:0x01d5, B:120:0x01de, B:121:0x01e2, B:122:0x01e6, B:126:0x01ef, B:127:0x01f3, B:128:0x01f7, B:132:0x0200, B:133:0x0204, B:134:0x0208, B:136:0x0211), top: B:154:0x01c4 }] */
    /* JADX WARN: Removed duplicated region for block: B:136:0x0211 A[Catch: Exception -> 0x0220, TRY_LEAVE, TryCatch #0 {Exception -> 0x0220, blocks: (B:110:0x01c4, B:114:0x01cd, B:115:0x01d1, B:116:0x01d5, B:120:0x01de, B:121:0x01e2, B:122:0x01e6, B:126:0x01ef, B:127:0x01f3, B:128:0x01f7, B:132:0x0200, B:133:0x0204, B:134:0x0208, B:136:0x0211), top: B:154:0x01c4 }] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public void reportAudioButtonStatus() {
        /*
            Method dump skipped, instructions count: 593
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.audio.MQSUtils.reportAudioButtonStatus():void");
    }

    public void reportAudioVisualDailyUse(List<AudioPlaybackConfiguration> currentPlayback) {
        String status = checkAudioVisualStatus();
        status.equals("open");
    }

    private String checkHapticStatus() {
        int status = Settings.System.getIntForUser(this.mContext.getContentResolver(), "haptic_feedback_enabled", 1000, -2);
        if (status == 1) {
            return "open";
        }
        if (status == 0) {
            return "close";
        }
        return none;
    }

    private float getHapticFeedbackFloatLevel() {
        return Settings.System.getFloatForUser(this.mContext.getContentResolver(), HAPTIC_FEEDBACK_INFINITE_INTENSITY, VIBRATION_DEFAULT_INFINITE_INTENSITY, -2);
    }

    public void reportVibrateStatus() {
        if (!isReportXiaomiServer()) {
            return;
        }
        Slog.i(TAG, "reportVibrateStatus start.");
        int vibrate_ring = Settings.System.getInt(this.mContext.getContentResolver(), "vibrate_when_ringing", 1);
        int vibrate_silent = Settings.System.getInt(this.mContext.getContentResolver(), "vibrate_in_silent", 1);
        String haptic_status = checkHapticStatus();
        float haptic_level = getHapticFeedbackFloatLevel();
        try {
            Intent intent = new Intent("onetrack.action.TRACK_EVENT");
            if (!isGlobalBuild()) {
                intent.setFlags(2);
            }
            intent.setPackage("com.miui.analytics");
            intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_APP_ID, VIBRATOR_EVENT_ID);
            intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_EVENT_NAME, VIBRATOR_EVENT_NAME);
            intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_PACKAGE, "android");
            Bundle params = new Bundle();
            if (vibrate_ring == 1) {
                intent.putExtra("status_ring", true);
            } else {
                intent.putExtra("status_ring", false);
            }
            if (vibrate_silent == 1) {
                intent.putExtra("status_silent", true);
            } else {
                intent.putExtra("status_silent", false);
            }
            if (haptic_status.equals("open")) {
                intent.putExtra("haptic_intensity_status", true);
                intent.putExtra("haptic_intensity_position", haptic_level);
            }
            intent.putExtras(params);
            if (!Build.IS_INTERNATIONAL_BUILD) {
                intent.setFlags(2);
            }
            this.mContext.startService(intent);
        } catch (Exception e) {
            e.printStackTrace();
            Slog.d(TAG, "erroe for reportVibrate");
        }
    }

    private boolean isGlobalBuild() {
        return Build.IS_INTERNATIONAL_BUILD;
    }

    private boolean isNotAllowedRegion() {
        String region = SystemProperties.get("ro.miui.region", "");
        Slog.i(TAG, "the region is :" + region);
        return region.equals("IN") || region.equals("");
    }

    private boolean isReportXiaomiServer() {
        String region = SystemProperties.get("ro.miui.region", "");
        Slog.i(TAG, "the region is :" + region);
        return region.equals("CN") || region.equals("RU");
    }
}
