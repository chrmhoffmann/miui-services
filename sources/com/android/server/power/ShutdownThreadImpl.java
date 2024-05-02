package com.android.server.power;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.drawable.AnimatedRotateDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.server.am.SplitScreenReporter;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import com.android.server.LocalServices;
import com.android.server.lights.LightsManager;
import com.android.server.lights.MiuiLightsService;
import com.miui.base.MiuiStubRegistry;
import java.io.File;
import miui.content.res.ThemeResources;
import miui.os.Build;
import miui.util.SystemAnalytics;
/* loaded from: classes.dex */
public class ShutdownThreadImpl extends ShutdownThreadStub {
    private static final String CUSTOMIZED_REGION;
    private static final String CUST_VAR;
    private static final boolean IS_CUSTOMIZATION;
    private static final boolean IS_CUSTOMIZATION_TEST;
    private static final boolean IS_CUSTOMIZED_REGION;
    private static final String OPCUST_ROOT_PATH;
    private static final String OPERATOR_ANIMATION_DISABLE_FLAG;
    private static final String OPERATOR_MUSIC_DISABLE_FLAG;
    private static final String OPERATOR_SHUTDOWN_ANIMATION_FILE;
    private static final String OPERATOR_SHUTDOWN_MUSIC_FILE;
    private static final String SHUTDOWN_ACTION_PROPERTY_MIUI = "sys.shutdown.miui";
    private static final String SHUTDOWN_ACTION_PROPERTY_MIUI_MUSIC = "sys.shutdown.miuimusic";
    private static final String TAG = "ShutdownThreadImpl";
    private static boolean sIsShutdownMusicPlaying;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<ShutdownThreadImpl> {

        /* compiled from: ShutdownThreadImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final ShutdownThreadImpl INSTANCE = new ShutdownThreadImpl();
        }

        public ShutdownThreadImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public ShutdownThreadImpl provideNewInstance() {
            return new ShutdownThreadImpl();
        }
    }

    static {
        String str;
        boolean z = false;
        IS_CUSTOMIZATION_TEST = Build.IS_CM_CUSTOMIZATION_TEST || Build.IS_CU_CUSTOMIZATION_TEST || Build.IS_CT_CUSTOMIZATION_TEST;
        if (Build.IS_CM_CUSTOMIZATION || Build.IS_CU_CUSTOMIZATION || Build.IS_CT_CUSTOMIZATION) {
            z = true;
        }
        IS_CUSTOMIZATION = z;
        String str2 = SystemProperties.get("ro.miui.customized.region", "");
        CUSTOMIZED_REGION = str2;
        boolean z2 = !TextUtils.isEmpty(str2);
        IS_CUSTOMIZED_REGION = z2;
        if (!z2) {
            str2 = Build.getCustVariant();
        }
        CUST_VAR = str2;
        if (Build.HAS_CUST_PARTITION) {
            str = "/product/opcust/" + str2 + "/";
        } else {
            str = "/data/miui/cust/" + str2 + "/";
        }
        OPCUST_ROOT_PATH = str;
        OPERATOR_SHUTDOWN_ANIMATION_FILE = str + "theme/operator/boots/shutdownanimation.zip";
        OPERATOR_SHUTDOWN_MUSIC_FILE = str + "theme/operator/boots/shutdownaudio.mp3";
        OPERATOR_ANIMATION_DISABLE_FLAG = ThemeResources.THEME_MAGIC_PATH + "disable_operator_animation";
        OPERATOR_MUSIC_DISABLE_FLAG = ThemeResources.THEME_MAGIC_PATH + "disable_operator_audio";
    }

    static boolean needVibrator() {
        return false;
    }

    void showShutdownAnimOrDialog(Context context, boolean isReboot, ProgressDialog pd) {
        showShutdownAnimOrDialog(context, isReboot);
    }

    void showShutdownAnimOrDialog(Context context, boolean isReboot) {
        if (isCustomizedShutdownAnim()) {
            SystemProperties.set("service.bootanim.exit", "0");
            SystemProperties.set("ctl.start", "bootanim");
            showShutdownAnimation(context, isReboot);
            return;
        }
        showShutdownDialog(context, isReboot);
    }

    boolean isCustomizedShutdownAnim() {
        if ((IS_CUSTOMIZATION || IS_CUSTOMIZATION_TEST || IS_CUSTOMIZED_REGION) && checkAnimationFileExist()) {
            return !"mx_at".equals(CUSTOMIZED_REGION) || "AT".equals(SystemProperties.get("persist.radio.op.name", "AT"));
        }
        return false;
    }

    static void showShutdownDialog(Context context, boolean isReboot) {
        Dialog bootMsgDialog = new Dialog(context, 16973933);
        View view = LayoutInflater.from(bootMsgDialog.getContext()).inflate(285999147, (ViewGroup) null);
        view.setSystemUiVisibility(1024);
        bootMsgDialog.setContentView(view);
        bootMsgDialog.setCancelable(false);
        WindowManager.LayoutParams lp = bootMsgDialog.getWindow().getAttributes();
        lp.screenOrientation = 1;
        lp.layoutInDisplayCutoutMode = 1;
        bootMsgDialog.getWindow().setAttributes(lp);
        bootMsgDialog.getWindow().setType(2021);
        bootMsgDialog.getWindow().clearFlags(65536);
        bootMsgDialog.show();
        if (isReboot) {
            ImageView shutdownImage = (ImageView) view.findViewById(285868194);
            if (shutdownImage != null) {
                shutdownImage.setVisibility(0);
                AnimatedRotateDrawable animationDrawable = shutdownImage.getDrawable();
                animationDrawable.setFramesCount(context.getResources().getInteger(285933648));
                animationDrawable.setFramesDuration(context.getResources().getInteger(285933649));
                animationDrawable.start();
            }
        } else {
            LightsManager lightmanager = (LightsManager) LocalServices.getService(LightsManager.class);
            MiuiLightsService.LightImpl light = lightmanager.getLight(0);
            light.setBrightness(0, true);
        }
        SystemProperties.set("sys.in_shutdown_progress", SplitScreenReporter.ACTION_ENTER_SPLIT);
    }

    static boolean checkAnimationFileExist() {
        return !new File(OPERATOR_ANIMATION_DISABLE_FLAG).exists() && new File(OPERATOR_SHUTDOWN_ANIMATION_FILE).exists();
    }

    static void showShutdownAnimation(Context context, boolean isReboot) {
        playShutdownMusic(context, isReboot);
    }

    static String getShutdownMusicFilePath(Context context, boolean isReboot) {
        return null;
    }

    private static String getShutdownMusicFilePathInner(Context context, boolean isReboot) {
        if (new File(OPERATOR_MUSIC_DISABLE_FLAG).exists()) {
            return null;
        }
        String str = OPERATOR_SHUTDOWN_MUSIC_FILE;
        if (!new File(str).exists()) {
            return null;
        }
        return str;
    }

    static void playShutdownMusic(Context context, boolean isReboot) {
        SystemProperties.set(SHUTDOWN_ACTION_PROPERTY_MIUI, "shutdown");
        String shutdownMusicPath = getShutdownMusicFilePathInner(context, isReboot);
        Log.d(TAG, "shutdown music: " + shutdownMusicPath + " " + isSilentMode(context));
        if (!isSilentMode(context) && shutdownMusicPath != null) {
            SystemProperties.set(SHUTDOWN_ACTION_PROPERTY_MIUI_MUSIC, "shutdown_music");
        }
    }

    private static void playShutdownMusicImpl(String shutdownMusicPath) {
        final Object actionDoneSync = new Object();
        sIsShutdownMusicPlaying = true;
        try {
            MediaPlayer mediaPlayer = new MediaPlayer();
            mediaPlayer.reset();
            mediaPlayer.setDataSource(shutdownMusicPath);
            mediaPlayer.prepare();
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() { // from class: com.android.server.power.ShutdownThreadImpl.1
                @Override // android.media.MediaPlayer.OnCompletionListener
                public void onCompletion(MediaPlayer mp) {
                    synchronized (actionDoneSync) {
                        ShutdownThreadImpl.sIsShutdownMusicPlaying = false;
                        actionDoneSync.notifyAll();
                    }
                }
            });
            mediaPlayer.start();
        } catch (Exception e) {
            sIsShutdownMusicPlaying = false;
            Log.d(TAG, "play shutdown music error:" + e);
        }
        long endTimeForMusic = SystemClock.elapsedRealtime() + 5000;
        synchronized (actionDoneSync) {
            while (true) {
                if (!sIsShutdownMusicPlaying) {
                    break;
                }
                long delay = endTimeForMusic - SystemClock.elapsedRealtime();
                if (delay <= 0) {
                    Log.d(TAG, "play shutdown music timeout");
                    break;
                }
                try {
                    actionDoneSync.wait(delay);
                } catch (InterruptedException e2) {
                }
            }
            if (!sIsShutdownMusicPlaying) {
                Log.d(TAG, "play shutdown music complete");
            }
        }
    }

    private static boolean isSilentMode(Context context) {
        AudioManager audio = (AudioManager) context.getSystemService("audio");
        return audio.isSilentMode();
    }

    static void recordShutdownTime(Context context, boolean reboot) {
        SystemAnalytics.Action action = new SystemAnalytics.Action();
        action.addParam(SplitScreenReporter.STR_ACTION, reboot ? "reboot" : "shutdown");
        action.addParam(SplitScreenReporter.STR_DEAL_TIME, System.currentTimeMillis());
        SystemAnalytics.trackSystem(context, "systemserver_bootshuttime", action);
    }
}
