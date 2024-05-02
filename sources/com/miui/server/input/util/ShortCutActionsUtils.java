package com.miui.server.input.util;

import android.app.ActivityOptions;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.MiuiSettings;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.MiuiMultiWindowUtils;
import android.widget.Toast;
import com.android.internal.accessibility.util.AccessibilityUtils;
import com.android.internal.util.MiuiScreenshotHelper;
import com.android.server.LocalServices;
import com.android.server.MiuiBatteryStatsService;
import com.android.server.MiuiBgThread;
import com.android.server.audio.AudioServiceStubImpl;
import com.android.server.input.MiuiInputThread;
import com.android.server.policy.BaseMiuiPhoneWindowManager;
import com.android.server.policy.FindDevicePowerOffLocateManager;
import com.android.server.policy.MiuiInputLog;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.wm.ActivityTaskManagerServiceImpl;
import com.miui.server.input.stylus.MiuiStylusPageKeyListener;
import com.miui.server.input.util.ShortCutActionsUtils;
import com.miui.server.smartpower.SmartPowerPolicyManager;
import com.miui.server.stability.DumpSysInfoUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import miui.os.Build;
import miui.securityspace.CrossUserUtils;
import miui.util.AudioManagerHelper;
import miui.util.HapticFeedbackUtil;
/* loaded from: classes.dex */
public class ShortCutActionsUtils {
    private static final String ACCESSIBILITY_CLASS_NAME_ENVIRONMENT_SPEECH_RECOGNITION = "com.miui.accessibility.environment.sound.recognition.TransparentEsrSettings";
    private static final String ACCESSIBILITY_CLASS_NAME_ENVIRONMENT_SPEECH_RECOGNITION_ENABLED = "com.miui.accessibility.environment.sound.recognition.EnvSoundRecognitionService";
    private static final String ACCESSIBILITY_CLASS_NAME_HEAR_SOUND = "com.miui.accessibility.asr.component.message.MessageActivity";
    private static final String ACCESSIBILITY_CLASS_NAME_HEAR_SOUND_SUBTUTLE = "com.miui.accessibility.asr.component.floatwindow.TransparentCaptionActivity";
    private static final String ACCESSIBILITY_CLASS_NAME_TALK_BACK = "com.google.android.marvin.talkback/com.google.android.marvin.talkback.TalkBackService";
    private static final String ACCESSIBILITY_CLASS_NAME_VOICE_CONTROL = "com.miui.accessibility.voiceaccess.settings.TransparentVoiceAccessSettings";
    private static final String ACCESSIBILITY_CLASS_NAME_VOICE_CONTROL_ENABLED = "com.miui.accessibility.voiceaccess.VoiceAccessAccessibilityService";
    private static final String ACCESSIBILITY_PACKAGE_NAME = "com.miui.accessibility";
    private static final String ACTION_INTENT_CONTENT = "content";
    private static final String ACTION_INTENT_DUAL = "dual";
    private static final String ACTION_INTENT_KEY = "packageName";
    private static final String ACTION_INTENT_SHORTCUT = "shortcut";
    private static final String ACTION_INTENT_TITLE = "title";
    public static final String ACTION_PANEL_OPERATION = "action_panels_operation";
    public static final String ACTION_POWER_UP = "power_up";
    public static final String EXTRA_ACTION_SOURCE = "event_source";
    public static final String EXTRA_KEY_ACTION = "extra_key_action";
    public static final String EXTRA_KEY_EVENT_TIME = "extra_key_event_time";
    public static final String EXTRA_LONG_PRESS_POWER_FUNCTION = "extra_long_press_power_function";
    public static final String EXTRA_POWER_GUIDE = "powerGuide";
    public static final String EXTRA_TORCH_ENABLED = "extra_torch_enabled";
    private static final String KEY_ACTION = "key_action";
    public static final String KEY_OPERATION = "operation";
    public static final String NOTES_CONTENT_ITEM_TYPE = "vnd.android.cursor.item/text_note";
    public static final String PACKAGE_SMART_HOME = "com.miui.smarthomeplus";
    public static final String PARTIAL_SCREENSHOT_POINTS = "partial.screenshot.points";
    public static final String REASON_OF_DOUBLE_CLICK_HOME_KEY = "double_click_home";
    public static final String REASON_OF_DOUBLE_CLICK_POWER_KEY = "power_double_tap";
    public static final String REASON_OF_DOUBLE_CLICK_VOLUME_DOWN = "double_click_volume_down";
    public static final String REASON_OF_TRIGGERED_BY_AI_KEY = "ai_key";
    public static final String REASON_OF_TRIGGERED_BY_PROXIMITY_SENSOR = "proximity_sensor";
    public static final String REASON_OF_TRIGGERED_BY_STABILIZER = "stabilizer";
    public static final String REASON_OF_TRIGGERED_TORCH = "triggered_by_runnable";
    public static final String REASON_OF_TRIGGERED_TORCH_BY_POWER = "triggered_by_power";
    public static final String REVERSE_NOTIFICATION_PANEL = "reverse_notifications_panel";
    public static final String REVERSE_QUICK_SETTINGS_PANEL = "reverse_quick_settings_panel";
    private static final String SETTINGS_CLASS_NAME_KEY_DOWNLOAD_DIALOG_ACTIVITY = "com.android.settings.KeyDownloadDialogActivity";
    private static final String SETTINGS_PACKAGE_NAME = "com.android.settings";
    private static final String TAG = "ShortCutActionsUtils";
    private static final int XIAO_POWER_GUIDE_VERSIONCODE = 3;
    private static ShortCutActionsUtils shortCutActionsUtils;
    private Context mContext;
    private String mCotaDeviceRegion;
    private String mCustomizedDeviceRegion;
    private Handler mHandler;
    private HapticFeedbackUtil mHapticFeedbackUtil;
    private boolean mHasCameraFlash;
    private final boolean mIsVoiceCapable;
    private PowerManager mPowerManager;
    private MiuiScreenshotHelper mScreenshotHelper;
    private List<String> mGAGuideCustomizedRegionList = new ArrayList();
    private WindowManagerPolicy mWindowManagerPolicy = (WindowManagerPolicy) LocalServices.getService(WindowManagerPolicy.class);

    private ShortCutActionsUtils(final Context context) {
        this.mContext = context;
        this.mScreenshotHelper = new MiuiScreenshotHelper(this.mContext);
        Handler handler = MiuiInputThread.getHandler();
        this.mHandler = handler;
        handler.post(new Runnable() { // from class: com.miui.server.input.util.ShortCutActionsUtils$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                ShortCutActionsUtils.this.m2286lambda$new$0$commiuiserverinpututilShortCutActionsUtils(context);
            }
        });
        this.mPowerManager = (PowerManager) this.mContext.getSystemService("power");
        this.mIsVoiceCapable = context.getResources().getBoolean(17891832);
        this.mHasCameraFlash = Build.hasCameraFlash(this.mContext);
        initGoogleAssistantGuideRegion();
    }

    /* renamed from: lambda$new$0$com-miui-server-input-util-ShortCutActionsUtils */
    public /* synthetic */ void m2286lambda$new$0$commiuiserverinpututilShortCutActionsUtils(Context context) {
        this.mHapticFeedbackUtil = new HapticFeedbackUtil(context, false);
    }

    private void initGoogleAssistantGuideRegion() {
        this.mCustomizedDeviceRegion = SystemProperties.get("ro.miui.customized.region");
        this.mCotaDeviceRegion = SystemProperties.get("persist.sys.cota.carrier");
        if (this.mGAGuideCustomizedRegionList.size() == 0) {
            this.mGAGuideCustomizedRegionList.add("es_vodafone");
            this.mGAGuideCustomizedRegionList.add("mx_at");
        }
    }

    public static ShortCutActionsUtils getInstance(Context context) {
        if (shortCutActionsUtils == null) {
            shortCutActionsUtils = new ShortCutActionsUtils(context);
        }
        return shortCutActionsUtils;
    }

    public boolean triggerFunction(String function, String shortcut, Bundle extras, boolean hapticFeedback, String effectKey) {
        ShortcutOneTrack.reportShortCutActionToOneTrack(this.mContext, shortcut, function);
        boolean triggered = false;
        if (effectKey == null) {
            effectKey = "virtual_key_longpress";
        }
        if (!this.mWindowManagerPolicy.isUserSetupComplete()) {
            if ("dump_log".equals(function) || ("dump_log_or_secret_code".equals(function) && this.mIsVoiceCapable)) {
                boolean triggered2 = launchDumpLog();
                triggerHapticFeedback(triggered2, shortcut, function, hapticFeedback, effectKey);
                return triggered2;
            }
            MiuiInputLog.major("user setup not complete");
            return false;
        }
        if ("screen_shot".equals(function)) {
            triggered = takeScreenshot(1);
        } else if ("partial_screen_shot".equals(function)) {
            triggered = extras != null ? takePartialScreenshot(extras.getFloatArray(PARTIAL_SCREENSHOT_POINTS)) : takeScreenshot(3);
        } else if ("screenshot_without_anim".equals(function)) {
            triggered = takeScreenshot(4);
        } else if ("stylus_partial_screenshot".equals(function)) {
            triggered = takeScreenshot(5);
        } else if ("launch_voice_assistant".equals(function)) {
            triggered = launchVoiceAssistant(shortcut, extras);
            effectKey = "screen_button_voice_assist";
        } else if ("launch_ai_shortcut".equals(function)) {
            triggered = launchAiShortcut();
        } else if ("launch_alipay_scanner".equals(function)) {
            triggered = launchAlipayScanner(shortcut);
        } else if ("launch_alipay_payment_code".equals(function)) {
            triggered = launchAlipayPaymentCode(shortcut);
        } else if ("launch_alipay_health_code".equals(function)) {
            triggered = launchAlipayHealthCode(shortcut);
        } else if ("launch_wechat_scanner".equals(function)) {
            triggered = launchWexinScanner(shortcut);
        } else if ("launch_wechat_payment_code".equals(function)) {
            triggered = lauchWeixinPaymentCode(shortcut);
        } else if ("turn_on_torch".equals(function)) {
            triggered = launchTorch(extras);
        } else if ("launch_calculator".equals(function)) {
            triggered = launchCalculator();
        } else if ("launch_camera".equals(function)) {
            triggered = launchCamera(shortcut);
        } else if ("dump_log".equals(function)) {
            triggered = launchDumpLog();
        } else if (MiuiCustomizeShortCutUtils.LAUNCH_CONTROL_CENTER.equals(function)) {
            triggered = launchControlCenter();
        } else if (MiuiCustomizeShortCutUtils.LAUNCH_NOTIFICATION_CENTER.equals(function)) {
            triggered = launchNotificationCenter();
        } else if ("mute".equals(function)) {
            triggered = mute();
        } else if ("launch_google_search".equals(function)) {
            triggered = launchGoogleSearch(shortcut);
            effectKey = "screen_button_voice_assist";
        } else if ("go_to_sleep".equals(function)) {
            triggered = goToSleep(shortcut);
        } else if ("dump_log_or_secret_code".equals(function)) {
            triggered = launchDumpLogOrContact(shortcut);
        } else if ("au_pay".equals(function)) {
            triggered = launchAuPay();
        } else if ("google_pay".equals(function)) {
            triggered = launchGooglePay();
        } else if ("mi_pay".equals(function)) {
            triggered = launchMiPay(shortcut);
        } else if ("note".equals(function)) {
            triggered = launchMiNotes(shortcut, extras);
        } else if ("launch_smarthome".equals(function)) {
            triggered = launchSmartHomeService(shortcut, extras);
        } else if ("find_device_locate".equals(function)) {
            triggered = findDeviceLocate();
        } else if ("launch_sound_recorder".equals(function)) {
            triggered = launchSoundRecorder();
        } else if ("launch_camera_capture".equals(function)) {
            triggered = launchCamera(shortcut, "CAPTURE");
        } else if ("launch_camera_video".equals(function)) {
            triggered = launchCamera(shortcut, "VIDEO");
        } else if ("vibrate".equals(function)) {
            triggered = vibrate();
        } else if ("launch_screen_recorder".equals(function)) {
            triggered = launchScreenRecorder();
        } else if ("launch_google_assistant_guide".equals(function)) {
            triggered = showGoogleAssistantGuide();
        } else if ("miui_talkback".equals(function)) {
            triggered = setAccessibilityTalkBackState();
        } else if ("voice_control".equals(function)) {
            triggered = launchAccessibilityVoiceControl();
        } else if ("environment_speech_recognition".equals(function)) {
            triggered = launchAccessibilityEnvironmentSpeechRecognition();
        } else if ("hear_sound".equals(function)) {
            triggered = launchAccessibilityHearSound();
        } else if ("hear_sound_subtitle".equals(function)) {
            triggered = launchAccessibilityHearSoundSubtitle();
        } else if ("launch_global_power_guide".equals(function)) {
            triggered = launchGlobalPowerGuide();
        } else if ("split_ltr".equals(function)) {
            ActivityTaskManagerServiceImpl.getInstance().toggleSplitByGesture(true);
        } else if ("split_rtl".equals(function)) {
            ActivityTaskManagerServiceImpl.getInstance().toggleSplitByGesture(false);
        }
        triggerHapticFeedback(triggered, shortcut, function, hapticFeedback, effectKey);
        return triggered;
    }

    private boolean launchGlobalPowerGuide() {
        Intent intent = new Intent();
        intent.setClassName("com.miui.miinput", "com.miui.miinput.gesture.GlobalPowerGuideActivity");
        intent.setFlags(268468224);
        return launchApp(intent);
    }

    private void triggerHapticFeedback(boolean triggered, String shortcut, String function, boolean hapticFeedback, String effectKey) {
        HapticFeedbackUtil hapticFeedbackUtil;
        if (triggered && hapticFeedback && (hapticFeedbackUtil = this.mHapticFeedbackUtil) != null) {
            hapticFeedbackUtil.performHapticFeedback(effectKey, false);
        }
        MiuiInputLog.defaults("shortcut:" + shortcut + " trigger function:" + function + " result:" + triggered);
    }

    private boolean setAccessibilityTalkBackState() {
        ComponentName componentName = ComponentName.unflattenFromString(ACCESSIBILITY_CLASS_NAME_TALK_BACK);
        AccessibilityUtils.setAccessibilityServiceState(this.mContext, componentName, !isAccessibilityFunctionEnabled(componentName.getClassName()));
        return true;
    }

    private boolean launchAccessibilityVoiceControl() {
        Intent intent = new Intent();
        intent.setClassName(ACCESSIBILITY_PACKAGE_NAME, ACCESSIBILITY_CLASS_NAME_VOICE_CONTROL);
        intent.putExtra("OPEN_VOICE_ACCESS", !isAccessibilityFunctionEnabled(ACCESSIBILITY_CLASS_NAME_VOICE_CONTROL_ENABLED) ? "open" : "close");
        return launchApp(intent);
    }

    private boolean launchAccessibilityEnvironmentSpeechRecognition() {
        Intent intent = new Intent();
        intent.setClassName(ACCESSIBILITY_PACKAGE_NAME, ACCESSIBILITY_CLASS_NAME_ENVIRONMENT_SPEECH_RECOGNITION);
        intent.putExtra("OPEN_ESR", !isAccessibilityFunctionEnabled(ACCESSIBILITY_CLASS_NAME_ENVIRONMENT_SPEECH_RECOGNITION_ENABLED) ? "open" : "close");
        return launchApp(intent);
    }

    private boolean launchAccessibilityHearSound() {
        Intent intent = new Intent();
        intent.setClassName(ACCESSIBILITY_PACKAGE_NAME, ACCESSIBILITY_CLASS_NAME_HEAR_SOUND);
        return launchApp(intent);
    }

    private boolean launchAccessibilityHearSoundSubtitle() {
        Intent intent = new Intent();
        intent.setClassName(ACCESSIBILITY_PACKAGE_NAME, ACCESSIBILITY_CLASS_NAME_HEAR_SOUND_SUBTUTLE);
        return launchApp(intent);
    }

    private boolean isAccessibilityFunctionEnabled(String componentClassName) {
        boolean enabled = false;
        if (componentClassName == null) {
            MiuiInputLog.error("Accessibility function componentClassName is null");
            return false;
        }
        List<ComponentName> list = new ArrayList<>(AccessibilityUtils.getEnabledServicesFromSettings(this.mContext, -2));
        for (ComponentName component : list) {
            if (component != null && componentClassName.equals(component.getClassName())) {
                enabled = true;
            }
        }
        MiuiInputLog.major("Accessibility function componentClassName:" + componentClassName + " enabled=" + enabled);
        return enabled;
    }

    private boolean launchGoogleSearch(String action) {
        if (this.mWindowManagerPolicy == null) {
            this.mWindowManagerPolicy = (WindowManagerPolicy) LocalServices.getService(WindowManagerPolicy.class);
        }
        if (this.mWindowManagerPolicy instanceof BaseMiuiPhoneWindowManager) {
            Bundle args = new Bundle();
            setGoogleSearchInvocationTypeKey(action, args);
            args.putInt("android.intent.extra.ASSIST_INPUT_DEVICE_ID", -1);
            return this.mWindowManagerPolicy.launchAssistAction(null, args);
        }
        return false;
    }

    private void setGoogleSearchInvocationTypeKey(String action, Bundle args) {
        if (TextUtils.isEmpty(action)) {
            return;
        }
        char c = 65535;
        switch (action.hashCode()) {
            case -1259120794:
                if (action.equals("long_press_power_key")) {
                    c = 0;
                    break;
                }
                break;
            case 1524450206:
                if (action.equals("long_press_home_key")) {
                    c = 1;
                    break;
                }
                break;
        }
        switch (c) {
            case 0:
                args.putInt("invocation_type", 6);
                return;
            case 1:
                args.putInt("invocation_type", 5);
                return;
            default:
                args.putInt("invocation_type", 2);
                return;
        }
    }

    private boolean showGoogleAssistantGuide() {
        Intent intent = new Intent();
        intent.setClassName("com.miui.miinput", "com.miui.miinput.gesture.GoogleAssistantGuideDialogActivity");
        intent.setFlags(268468224);
        return launchApp(intent);
    }

    public boolean isNeedShowGoogleGuideForOperator() {
        return this.mGAGuideCustomizedRegionList.contains(this.mCustomizedDeviceRegion) || "VF".equals(this.mCotaDeviceRegion);
    }

    public void setCotaDeviceRegion() {
        this.mCotaDeviceRegion = SystemProperties.get("persist.sys.cota.carrier");
    }

    private boolean findDeviceLocate() {
        FindDevicePowerOffLocateManager.sendFindDeviceLocateBroadcast(this.mContext, FindDevicePowerOffLocateManager.IMPERCEPTIBLE_POWER_PRESS);
        return true;
    }

    public boolean triggerFunction(String function, String shortcut, Bundle extras, boolean hapticFeedback) {
        return triggerFunction(function, shortcut, extras, hapticFeedback, "mesh_heavy");
    }

    private boolean launchSmartHomeService(String shortcut, Bundle extra) {
        Intent smartHomeIntent = new Intent();
        smartHomeIntent.setComponent(new ComponentName("com.miui.smarthomeplus", "com.miui.smarthomeplus.UWBEntryService"));
        if ("long_press_power_key".equals(shortcut) && extra != null && !TextUtils.isEmpty(extra.getString(EXTRA_LONG_PRESS_POWER_FUNCTION))) {
            smartHomeIntent = getLongPressPowerKeyFunctionIntent("com.miui.smarthomeplus", extra.getString(EXTRA_LONG_PRESS_POWER_FUNCTION));
        }
        smartHomeIntent.putExtra(EXTRA_ACTION_SOURCE, shortcut);
        try {
            this.mContext.startServiceAsUser(smartHomeIntent, UserHandle.CURRENT);
            return true;
        } catch (Exception e) {
            MiuiInputLog.error(e.toString());
            return true;
        }
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    private Intent getLongPressPowerKeyFunctionIntent(String intentName, String action) {
        boolean z;
        Intent intent = null;
        char c = 65535;
        switch (intentName.hashCode()) {
            case 298563857:
                if (intentName.equals("com.miui.smarthomeplus")) {
                    z = true;
                    break;
                }
                z = true;
                break;
            case 1566545774:
                if (intentName.equals("android.intent.action.ASSIST")) {
                    z = false;
                    break;
                }
                z = true;
                break;
            default:
                z = true;
                break;
        }
        switch (z) {
            case false:
                intent = new Intent("android.intent.action.ASSIST");
                intent.setComponent(null);
                intent.putExtra("versionCode", 3);
                break;
            case true:
                intent = new Intent();
                intent.setComponent(new ComponentName("com.miui.smarthomeplus", "com.miui.smarthomeplus.UWBEntryService"));
                break;
        }
        if (intent != null) {
            switch (action.hashCode()) {
                case -1259120794:
                    if (action.equals("long_press_power_key")) {
                        c = 0;
                        break;
                    }
                    break;
                case 858558485:
                    if (action.equals(ACTION_POWER_UP)) {
                        c = 1;
                        break;
                    }
                    break;
            }
            switch (c) {
                case 0:
                    intent.putExtra(KEY_ACTION, 0);
                    intent.putExtra("long_press_event_time", SystemClock.uptimeMillis());
                    break;
                case 1:
                    intent.putExtra(KEY_ACTION, 1);
                    intent.putExtra("key_event_time", SystemClock.uptimeMillis());
                    break;
            }
        }
        return intent;
    }

    public boolean launchControlCenter() {
        Intent intent = new Intent(ACTION_PANEL_OPERATION);
        intent.putExtra(KEY_OPERATION, REVERSE_QUICK_SETTINGS_PANEL);
        this.mContext.sendBroadcast(intent);
        return true;
    }

    public boolean launchNotificationCenter() {
        Intent intent = new Intent(ACTION_PANEL_OPERATION);
        intent.putExtra(KEY_OPERATION, REVERSE_NOTIFICATION_PANEL);
        this.mContext.sendBroadcast(intent);
        return true;
    }

    public boolean mute() {
        boolean isSilenceModeOn = MiuiSettings.SoundMode.isSilenceModeOn(this.mContext);
        MiuiSettings.SoundMode.setSilenceModeOn(this.mContext, !isSilenceModeOn);
        return true;
    }

    public boolean launchAiShortcut() {
        try {
            MiuiInputLog.major("knock launch ai shortcut");
            Intent intent = new Intent();
            intent.putExtra("is_show_dialog", "false");
            intent.putExtra("from", "knock");
            intent.setFlags(268435456);
            ComponentName componentName = ComponentName.unflattenFromString("com.miui.voiceassist/com.xiaomi.voiceassistant.AiSettings.AiShortcutActivity");
            intent.setComponent(componentName);
            this.mContext.startActivityAsUser(intent, UserHandle.CURRENT);
            return true;
        } catch (IllegalStateException e) {
            MiuiInputLog.error("IllegalStateException", e);
            return false;
        } catch (SecurityException e2) {
            MiuiInputLog.error("SecurityException", e2);
            return false;
        } catch (RuntimeException e3) {
            MiuiInputLog.error("RuntimeException", e3);
            return false;
        }
    }

    private boolean launchVoiceAssistant(String shortcut, Bundle extra) {
        try {
            Intent intent = new Intent("android.intent.action.ASSIST");
            if ("long_press_power_key".equals(shortcut) && extra != null && !TextUtils.isEmpty(extra.getString(EXTRA_LONG_PRESS_POWER_FUNCTION))) {
                intent = getLongPressPowerKeyFunctionIntent("android.intent.action.ASSIST", extra.getString(EXTRA_LONG_PRESS_POWER_FUNCTION));
                intent.putExtra(EXTRA_POWER_GUIDE, extra.getBoolean(EXTRA_POWER_GUIDE, false));
            }
            if (extra != null && REASON_OF_TRIGGERED_BY_AI_KEY.equals(shortcut)) {
                intent.setPackage("com.miui.voiceassist");
                intent.putExtra(KEY_ACTION, extra.getInt(EXTRA_KEY_ACTION));
                intent.putExtra("key_event_time", extra.getLong(EXTRA_KEY_EVENT_TIME));
            }
            intent.putExtra("voice_assist_start_from_key", shortcut);
            intent.putExtra("app.send.wakeup.command", System.currentTimeMillis());
            ComponentName componentName = ComponentName.unflattenFromString(this.mContext.getResources().getString(286195876));
            intent.setComponent(componentName);
            MiuiInputLog.major("launchVoiceAssistant startForegroundServiceAsUser");
            return this.mContext.startForegroundServiceAsUser(intent, UserHandle.CURRENT) != null;
        } catch (Exception e) {
            MiuiInputLog.error("Exception", e);
            return false;
        }
    }

    private boolean launchMiNotes(String shortcut, Bundle extras) {
        ActivityOptions activityOptions;
        Intent intent = new Intent("com.miui.pad.notes.action.INSERT_OR_EDIT");
        intent.setType(NOTES_CONTENT_ITEM_TYPE);
        intent.putExtra(ACTION_INTENT_SHORTCUT, shortcut);
        intent.setPackage("com.miui.notes");
        if (extras != null) {
            String scene = extras.getString("scene", null);
            if (scene == null) {
                return launchAppSimple(intent, null);
            }
            intent.putExtra("scene", scene);
            intent.addFlags(335544320);
            if (MiuiStylusPageKeyListener.SCENE_KEYGUARD.equals(scene) || MiuiStylusPageKeyListener.SCENE_OFF_SCREEN.equals(scene)) {
                intent.putExtra("StartActivityWhenLocked", true);
                return launchAppSimple(intent, null);
            }
            Bundle bundle = null;
            if (MiuiStylusPageKeyListener.SCENE_APP.equals(scene) && (activityOptions = MiuiMultiWindowUtils.getActivityOptions(this.mContext, "com.miui.notes", true, false)) != null) {
                bundle = activityOptions.toBundle();
            }
            return launchAppSimple(intent, bundle);
        }
        return launchAppSimple(intent, null);
    }

    private boolean launchAppSimple(Intent intent, Bundle bundle) {
        try {
            this.mContext.startActivityAsUser(intent, bundle, UserHandle.CURRENT);
            return true;
        } catch (ActivityNotFoundException e) {
            MiuiInputLog.error("launchAppSimple ActivityNotFoundException", e);
            return false;
        } catch (IllegalStateException e2) {
            MiuiInputLog.error("launchAppSimple IllegalStateException", e2);
            return false;
        }
    }

    private boolean launchMiPay(String eventSource) {
        Intent nfcIntent = new Intent();
        nfcIntent.setFlags(536870912);
        nfcIntent.putExtra("StartActivityWhenLocked", true);
        nfcIntent.setAction("com.miui.intent.action.DOUBLE_CLICK");
        nfcIntent.putExtra(EXTRA_ACTION_SOURCE, eventSource);
        nfcIntent.setPackage("com.miui.tsmclient");
        return launchApp(nfcIntent);
    }

    private boolean launchTorch(Bundle extra) {
        boolean z = false;
        if (!this.mHasCameraFlash) {
            MiuiInputLog.major("not have camera flash");
            return false;
        }
        boolean isOpen = Settings.Global.getInt(this.mContext.getContentResolver(), "torch_state", 0) != 0;
        Intent intent = new Intent("miui.intent.action.TOGGLE_TORCH");
        if (extra != null) {
            intent.putExtra("miui.intent.extra.IS_ENABLE", extra.getBoolean(EXTRA_TORCH_ENABLED, false));
        } else {
            if (!isOpen) {
                z = true;
            }
            intent.putExtra("miui.intent.extra.IS_ENABLE", z);
        }
        this.mContext.sendBroadcast(intent);
        return true;
    }

    private boolean launchCamera(String shortcut) {
        Intent cameraIntent = new Intent();
        cameraIntent.setFlags(SmartPowerPolicyManager.WHITE_LIST_TYPE_PROVIDER_DEFAULT);
        boolean z = true;
        cameraIntent.putExtra("ShowCameraWhenLocked", true);
        BaseMiuiPhoneWindowManager baseMiuiPhoneWindowManager = this.mWindowManagerPolicy;
        if (baseMiuiPhoneWindowManager instanceof BaseMiuiPhoneWindowManager) {
            if (!baseMiuiPhoneWindowManager.getKeyguardActive() && !REASON_OF_DOUBLE_CLICK_POWER_KEY.equals(shortcut)) {
                z = false;
            }
            cameraIntent.putExtra("StartActivityWhenLocked", z);
        }
        cameraIntent.setAction("android.media.action.STILL_IMAGE_CAMERA");
        ComponentName mCameraComponentName = ComponentName.unflattenFromString(this.mContext.getResources().getString(286195871));
        cameraIntent.setComponent(mCameraComponentName);
        cameraIntent.putExtra("com.android.systemui.camera_launch_source", shortcut);
        return launchApp(cameraIntent);
    }

    private boolean launchCamera(String shortcut, String mode) {
        Intent cameraIntent = new Intent();
        cameraIntent.setFlags(8388608);
        cameraIntent.putExtra("ShowCameraWhenLocked", true);
        cameraIntent.putExtra("StartActivityWhenLocked", true);
        cameraIntent.setAction("android.media.action.VOICE_COMMAND");
        ComponentName mCameraComponentName = ComponentName.unflattenFromString(this.mContext.getResources().getString(286195871));
        cameraIntent.setComponent(mCameraComponentName);
        cameraIntent.putExtra("android.intent.extra.CAMERA_MODE", mode);
        cameraIntent.putExtra("android.intent.extra.REFERRER", Uri.parse("android-app://com.android.camera"));
        cameraIntent.putExtra("com.android.systemui.camera_launch_source", shortcut);
        return launchApp(cameraIntent);
    }

    private boolean launchScreenRecorder() {
        Intent intent = new Intent();
        intent.setAction("miui.intent.screenrecorder.RECORDER_SERVICE");
        intent.setPackage("com.miui.screenrecorder");
        intent.putExtra("is_start_immediately", false);
        this.mContext.startService(intent);
        return true;
    }

    public boolean vibrate() {
        AudioManagerHelper.toggleVibrateSetting(this.mContext);
        return true;
    }

    private boolean launchSoundRecorder() {
        Intent intent = new Intent();
        ComponentName comp = new ComponentName("com.android.soundrecorder", "com.android.soundrecorder.SoundRecorder");
        intent.setComponent(comp);
        return launchApp(intent);
    }

    private boolean launchMusicReader() {
        Intent intent = new Intent("android.intent.action.VIEW");
        ComponentName comp = new ComponentName("com.miui.player", "com.miui.player.ui.MusicBrowserActivity");
        intent.setData(Uri.parse("miui-music://radar?miref=com.miui.knock"));
        intent.setComponent(comp);
        return launchApp(intent);
    }

    private boolean launchWexinScanner(String shortcut) {
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.addCategory("android.intent.category.LAUNCHER");
        intent.setComponent(new ComponentName("com.tencent.mm", "com.tencent.mm.plugin.scanner.ui.BaseScanUI"));
        if (launchApp(intent)) {
            return true;
        }
        Intent dialogIntent = new Intent();
        dialogIntent.setClassName(SETTINGS_PACKAGE_NAME, SETTINGS_CLASS_NAME_KEY_DOWNLOAD_DIALOG_ACTIVITY);
        dialogIntent.putExtra(ACTION_INTENT_SHORTCUT, shortcut);
        dialogIntent.putExtra("packageName", "com.tencent.mm");
        dialogIntent.putExtra(ACTION_INTENT_DUAL, false);
        dialogIntent.putExtra(ACTION_INTENT_TITLE, this.mContext.getString(286195789));
        dialogIntent.putExtra(ACTION_INTENT_CONTENT, this.mContext.getString(286195790));
        dialogIntent.setFlags(268468224);
        launchApp(dialogIntent);
        return false;
    }

    private boolean launchAlipayScanner(String shortcut) {
        Intent intent = new Intent();
        intent.setAction("android.intent.action.VIEW");
        intent.setComponent(new ComponentName("com.eg.android.AlipayGphone", "com.alipay.mobile.scan.as.main.MainCaptureActivity"));
        intent.setFlags(268468224);
        Bundle bundle = new Bundle();
        bundle.putString("app_id", "10000007");
        Bundle bundleTemp = new Bundle();
        bundleTemp.putString("source", ACTION_INTENT_SHORTCUT);
        bundleTemp.putString("appId", "10000007");
        bundleTemp.putBoolean("REALLY_STARTAPP", true);
        bundleTemp.putString("showOthers", "YES");
        bundleTemp.putBoolean("startFromExternal", true);
        bundleTemp.putBoolean("REALLY_DOSTARTAPP", true);
        bundleTemp.putString("sourceId", ACTION_INTENT_SHORTCUT);
        bundleTemp.putString("ap_framework_sceneId", "20000001");
        bundle.putBundle("mExtras", bundleTemp);
        intent.putExtras(bundle);
        if (launchApp(intent)) {
            return true;
        }
        Intent dialogIntent = new Intent();
        dialogIntent.setClassName(SETTINGS_PACKAGE_NAME, SETTINGS_CLASS_NAME_KEY_DOWNLOAD_DIALOG_ACTIVITY);
        dialogIntent.putExtra(ACTION_INTENT_SHORTCUT, shortcut);
        dialogIntent.putExtra("packageName", "com.eg.android.AlipayGphone");
        dialogIntent.putExtra(ACTION_INTENT_DUAL, false);
        dialogIntent.putExtra(ACTION_INTENT_TITLE, this.mContext.getString(286195785));
        dialogIntent.putExtra(ACTION_INTENT_CONTENT, this.mContext.getString(286195790));
        dialogIntent.setFlags(268468224);
        launchApp(dialogIntent);
        return false;
    }

    private boolean lauchWeixinPaymentCode(String shortcut) {
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.setComponent(new ComponentName("com.tencent.mm", "com.tencent.mm.plugin.offline.ui.WalletOfflineCoinPurseUI"));
        intent.putExtra("key_entry_scene", 2);
        if (launchApp(intent)) {
            return true;
        }
        Intent dialogIntent = new Intent();
        dialogIntent.setClassName(SETTINGS_PACKAGE_NAME, SETTINGS_CLASS_NAME_KEY_DOWNLOAD_DIALOG_ACTIVITY);
        dialogIntent.putExtra(ACTION_INTENT_SHORTCUT, shortcut);
        dialogIntent.putExtra("packageName", "com.tencent.mm");
        dialogIntent.putExtra(ACTION_INTENT_DUAL, false);
        dialogIntent.putExtra(ACTION_INTENT_TITLE, this.mContext.getString(286195788));
        dialogIntent.putExtra(ACTION_INTENT_CONTENT, this.mContext.getString(286195790));
        dialogIntent.setFlags(268468224);
        launchApp(dialogIntent);
        return false;
    }

    private boolean launchAlipayPaymentCode(String shortcut) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.eg.android.AlipayGphone", "com.eg.android.AlipayGphone.FastStartActivity"));
        intent.setAction("android.intent.action.VIEW");
        intent.setFlags(343932928);
        intent.setData(Uri.parse("alipayss://platformapi/startapp?appId=20000056&source=shortcut"));
        if (launchApp(intent)) {
            return true;
        }
        Intent dialogIntent = new Intent();
        dialogIntent.setClassName(SETTINGS_PACKAGE_NAME, SETTINGS_CLASS_NAME_KEY_DOWNLOAD_DIALOG_ACTIVITY);
        dialogIntent.putExtra(ACTION_INTENT_SHORTCUT, shortcut);
        dialogIntent.putExtra("packageName", "com.eg.android.AlipayGphone");
        dialogIntent.putExtra(ACTION_INTENT_DUAL, false);
        dialogIntent.putExtra(ACTION_INTENT_TITLE, this.mContext.getString(286195784));
        dialogIntent.putExtra(ACTION_INTENT_CONTENT, this.mContext.getString(286195790));
        dialogIntent.setFlags(268468224);
        launchApp(dialogIntent);
        return false;
    }

    private boolean launchAlipayHealthCode(String shortcut) {
        Intent intent = new Intent();
        intent.setAction("android.intent.action.VIEW");
        intent.setPackage("com.eg.android.AlipayGphone");
        intent.setFlags(343932928);
        intent.setData(Uri.parse("alipays://platformapi/startapp?appId=68687564&chInfo=ch_xiaomi_quick&sceneCode=KF_CHANGSHANG&shareUserId=2088831085791813&partnerId=ch_xiaomi_quick&pikshemo=YES"));
        if (launchApp(intent)) {
            return true;
        }
        Intent dialogIntent = new Intent();
        dialogIntent.setClassName(SETTINGS_PACKAGE_NAME, SETTINGS_CLASS_NAME_KEY_DOWNLOAD_DIALOG_ACTIVITY);
        dialogIntent.putExtra(ACTION_INTENT_SHORTCUT, shortcut);
        dialogIntent.putExtra("packageName", "com.eg.android.AlipayGphone");
        dialogIntent.putExtra(ACTION_INTENT_DUAL, false);
        dialogIntent.putExtra(ACTION_INTENT_TITLE, this.mContext.getString(286195783));
        dialogIntent.putExtra(ACTION_INTENT_CONTENT, this.mContext.getString(286195790));
        dialogIntent.setFlags(268468224);
        launchApp(dialogIntent);
        return false;
    }

    private boolean launchCalculator() {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.miui.calculator", "com.miui.calculator.cal.CalculatorActivity"));
        intent.setFlags(SmartPowerPolicyManager.WHITE_LIST_TYPE_PROVIDER_DEFAULT);
        return launchApp(intent);
    }

    private boolean launchDumpLog() {
        Intent dumpLogIntent = new Intent();
        dumpLogIntent.setPackage("com.miui.bugreport");
        dumpLogIntent.setAction("com.miui.bugreport.service.action.DUMPLOG");
        this.mContext.sendBroadcastAsUser(dumpLogIntent, UserHandle.CURRENT);
        showToast(286196555, 0);
        DumpSysInfoUtil.captureDumpLog();
        return true;
    }

    private void showToast(final int resourceId, final int length) {
        this.mHandler.post(new Runnable() { // from class: com.miui.server.input.util.ShortCutActionsUtils$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                ShortCutActionsUtils.this.m2287x5725795f(resourceId, length);
            }
        });
    }

    /* renamed from: lambda$showToast$1$com-miui-server-input-util-ShortCutActionsUtils */
    public /* synthetic */ void m2287x5725795f(int resourceId, int length) {
        Context context = this.mContext;
        Toast.makeText(context, context.getString(resourceId), length).show();
    }

    private boolean launchDumpLogOrContact(String shortcut) {
        if (!this.mIsVoiceCapable) {
            MiuiInputLog.major("mIsVoiceCapable false, so lunch emergencyDialer");
            Intent intent = new Intent();
            ComponentName componentName = ComponentName.unflattenFromString("com.android.phone/com.android.phone.EmergencyDialer");
            intent.setComponent(componentName);
            intent.putExtra(ACTION_INTENT_SHORTCUT, shortcut);
            return launchApp(intent);
        }
        launchDumpLog();
        return true;
    }

    private boolean goToSleep(String shortcut) {
        long currentTime = SystemClock.uptimeMillis();
        if (this.mPowerManager != null) {
            MiuiInputLog.major("goToSleep, reason = " + shortcut);
            this.mPowerManager.goToSleep(currentTime);
            return true;
        }
        return false;
    }

    private boolean takeScreenshot(int type) {
        this.mScreenshotHelper.takeScreenshot(type, true, false, this.mHandler, (Consumer) null);
        return true;
    }

    private boolean launchAuPay() {
        Intent intent = new Intent();
        intent.setAction("android.intent.action.VIEW");
        intent.setClassName("jp.auone.wallet", "jp.auone.wallet.ui.main.DeviceCredentialSchemeActivity");
        intent.putExtra("shortcut_start", "jp.auone.wallet.qr");
        intent.setFlags(536870912);
        if (!launchApp(intent)) {
            showToast(286195786, 0);
            return false;
        }
        return true;
    }

    private boolean takePartialScreenshot(float[] pathList) {
        this.mScreenshotHelper.takeScreenshotPartial(3, true, false, pathList, (int) AudioServiceStubImpl.READ_MUSIC_PLAY_BACK_DELAY, this.mHandler, (Consumer) null);
        return true;
    }

    private boolean launchGooglePay() {
        return false;
    }

    private boolean launchApp(Intent intent) {
        return launchApp(intent, null);
    }

    private boolean launchApp(Intent intent, Bundle bundle) {
        if ((intent.getFlags() & SmartPowerPolicyManager.WHITE_LIST_TYPE_PROVIDER_DEFAULT) != 0) {
            intent.addFlags(335544320);
        } else {
            intent.addFlags(343932928);
        }
        String packageName = null;
        if (!TextUtils.isEmpty(intent.getPackage())) {
            packageName = intent.getPackage();
        } else if (intent.getComponent() != null && !TextUtils.isEmpty(intent.getComponent().getPackageName())) {
            packageName = intent.getComponent().getPackageName();
        }
        if (packageName == null) {
            MiuiInputLog.major("package name is null");
            return false;
        }
        List<ResolveInfo> list = this.mContext.getPackageManager().queryIntentActivitiesAsUser(intent, 65536, CrossUserUtils.getCurrentUserId());
        if (list != null && list.size() > 0) {
            try {
                this.mContext.startActivityAsUser(intent, bundle, UserHandle.CURRENT);
                return true;
            } catch (ActivityNotFoundException e) {
                MiuiInputLog.error("ActivityNotFoundException", e);
            } catch (IllegalStateException e2) {
                MiuiInputLog.error("IllegalStateException", e2);
            }
        } else {
            MiuiInputLog.major("launch app fail  package:" + packageName);
        }
        return false;
    }

    /* loaded from: classes.dex */
    public static class ShortcutOneTrack {
        private static final String DEVICE_REGION = Build.getRegion();
        private static final int FLAG_NON_ANONYMOUS = 2;

        private ShortcutOneTrack() {
        }

        public static void reportShortCutActionToOneTrack(final Context context, final String shortcut, final String function) {
            if (DEVICE_REGION.equals("IN")) {
                return;
            }
            MiuiBgThread.getHandler().post(new Runnable() { // from class: com.miui.server.input.util.ShortCutActionsUtils$ShortcutOneTrack$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    ShortCutActionsUtils.ShortcutOneTrack.lambda$reportShortCutActionToOneTrack$0(shortcut, function, context);
                }
            });
        }

        public static /* synthetic */ void lambda$reportShortCutActionToOneTrack$0(String shortcut, String function, Context context) {
            Intent intent = new Intent(MiuiBatteryStatsService.TrackBatteryUsbInfo.ACTION_TRACK_EVENT);
            intent.setPackage(MiuiBatteryStatsService.TrackBatteryUsbInfo.ANALYTICS_PACKAGE);
            intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_APP_ID, "31000000145");
            intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_EVENT_NAME, ShortCutActionsUtils.ACTION_INTENT_SHORTCUT);
            intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_PACKAGE, "com.xiaomi.shortcut");
            intent.putExtra("action_type", shortcut);
            intent.putExtra("function", function);
            if (!Build.IS_INTERNATIONAL_BUILD) {
                intent.setFlags(2);
            }
            try {
                context.startServiceAsUser(intent, UserHandle.CURRENT);
            } catch (IllegalStateException e) {
                MiuiInputLog.defaults("Failed to upload shortcut event!");
            }
        }
    }
}
