package com.android.server.display;

import android.content.Context;
import android.database.ContentObserver;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemProperties;
import android.provider.MiuiSettings;
import android.util.AtomicFile;
import android.util.Pair;
import android.util.Slog;
import android.util.TypedXmlPullParser;
import android.util.TypedXmlSerializer;
import android.util.Xml;
import com.android.internal.os.BackgroundThread;
import com.android.server.display.MiuiDisplayCloudController;
import com.android.server.wm.MiuiSizeCompatService;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes.dex */
public class MiuiDisplayCloudController {
    private static final String AUTO_BRIGHTNESS_STATISTICS_EVENT_ENABLE = "automatic_brightness_statistics_event_enable";
    private static final String AUTO_BRIGHTNESS_STATISTICS_EVENT_MODULE_NAME = "AutomaticBrightnessStatisticsEvent";
    private static final String BCBC_APP_CONFIG = "bcbc_app_config";
    private static final String BCBC_FEATURE_MODULE_NAME = "BCBCFeature";
    private static final String CLOUD_BACKUP_FILE_ATTRIBUTE_ENABLE = "enabled";
    private static final String CLOUD_BACKUP_FILE_ATTRIBUTE_ITEM = "item";
    private static final String CLOUD_BACKUP_FILE_ATTRIBUTE_PACKAGE = "package";
    private static final String CLOUD_BACKUP_FILE_AUTO_BRIGHTNESS_STATISTICS_EVENT_ENABLE = "automatic-brightness-statistics-event-enable";
    private static final String CLOUD_BACKUP_FILE_AUTO_BRIGHTNESS_STATISTICS_EVENT_ENABLE_TAG = "automatic_brightness_statistics_event_enable";
    private static final String CLOUD_BACKUP_FILE_BCBC_TAG = "bcbc";
    private static final String CLOUD_BACKUP_FILE_BCBC_TAG_APP = "bcbc-app";
    private static final String CLOUD_BACKUP_FILE_CATEGORY_TAG_GAME = "game-category";
    private static final String CLOUD_BACKUP_FILE_CATEGORY_TAG_IMAGE = "image-category";
    private static final String CLOUD_BACKUP_FILE_CATEGORY_TAG_MAP = "map-category";
    private static final String CLOUD_BACKUP_FILE_CATEGORY_TAG_READER = "reader-category";
    private static final String CLOUD_BACKUP_FILE_CATEGORY_TAG_UNDEFINED = "undefined-category";
    private static final String CLOUD_BACKUP_FILE_CATEGORY_TAG_VIDEO = "video-category";
    private static final String CLOUD_BACKUP_FILE_NAME = "display_cloud_backup.xml";
    private static final String CLOUD_BACKUP_FILE_OVERRIDE_BRIGHTNESS_POLICY_ENABLE = "override-brightness-policy-enable";
    private static final String CLOUD_BACKUP_FILE_OVERRIDE_BRIGHTNESS_POLICY_TAG = "override_brightness_policy_enable";
    private static final String CLOUD_BACKUP_FILE_POINT_LIGHT_SOURCE_DETECTOR_ENABLE = "point-light-source-detector-enabled";
    private static final String CLOUD_BACKUP_FILE_POINT_LIGHT_SOURCE_DETECTOR_TAG = "point_light_source_detector_enable";
    private static final String CLOUD_BACKUP_FILE_RESOLUTION_SWITCH_TAG = "resolution_switch";
    private static final String CLOUD_BACKUP_FILE_RESOLUTION_SWITCH_TAG_PROCESS_BLACK = "resolution_switch_process_black";
    private static final String CLOUD_BACKUP_FILE_RESOLUTION_SWITCH_TAG_PROCESS_PROTECT = "resolution_switch_process_protect";
    private static final String CLOUD_BACKUP_FILE_ROOT_ELEMENT = "display-config";
    private static final String CLOUD_BACKUP_FILE_SHORT_TERM_MODEL_ENABLE = "short-term-model-enabled";
    private static final String CLOUD_BACKUP_FILE_SHORT_TERM_MODEL_TAG = "short-term";
    private static final String CLOUD_BACKUP_FILE_TEMP_CRT_TAG = "temperatureControl";
    private static final String CLOUD_BACKUP_FILE_TOUCH_COVER_PROTECTION_GAME_TAG = "touch_cover_protection_game";
    private static final String CLOUD_BACKUP_FILE_TOUCH_COVER_PROTECTION_GAME_TAG_APP = "touch_cover_protection_game_app";
    private static final boolean DEBUG;
    private static final String OVERRIDE_BRIGHTNESS_POLICY_ENABLE = "override_brightness_policy_enable";
    private static final String OVERRIDE_BRIGHTNESS_POLICY_MODULE_NAME = "overrideBrightnessPolicy";
    private static final String POINT_LIGHT_SOURCE_DETECTOR_ENABLE = "point_light_source_detector_enable";
    private static final String POINT_LIGHT_SOURCE_MODULE_NAME = "pointLightSourceDetector";
    private static final String PROCESS_RESOLUTION_SWITCH_BLACK_LIST = "process_resolution_switch_black_list";
    private static final String PROCESS_RESOLUTION_SWITCH_LIST = "process_resolution_switch_list";
    private static final String PROCESS_RESOLUTION_SWITCH_PROTECT_LIST = "process_resolution_switch_protect_list";
    private static final String RESOLUTION_SWITCH_PROCESS_LIST_BACKUP_FILE = "resolution_switch_process_list_backup.xml";
    private static final String RESOLUTION_SWITCH_PROCESS_LIST_MODEULE_NAME = "resolutionSwitchProcessList";
    private static final String SHORT_TERM_MODEL_APP_CONFIG = "short_term_model_app_config";
    private static final String SHORT_TERM_MODEL_ENABLE = "short_term_model_enable";
    private static final String SHORT_TERM_MODEL_GAME_APP_LIST = "short_term_model_game_app_list";
    private static final String SHORT_TERM_MODEL_GLOBAL_APP_LIST = "short_term_model_global_app_list";
    private static final String SHORT_TERM_MODEL_IMAGE_APP_LIST = "short_term_model_image_app_list";
    private static final String SHORT_TERM_MODEL_MAP_APP_LIST = "short_term_model_map_app_list";
    private static final String SHORT_TERM_MODEL_MODULE_NAME = "shortTermModel";
    private static final String SHORT_TERM_MODEL_READER_APP_LIST = "short_term_model_reader_app_list";
    private static final String SHORT_TERM_MODEL_VIDEO_APP_LIST = "short_term_model_video_app_list";
    private static final String SHORT_TREM_MODEL_APP_MODULE_NAME = "shortTermModelAppList";
    private static final String TAG = "MiuiDisplayCloudController";
    private static final String TEMP_CTRL_CONFIG = "temperature_control_config";
    private static final String TEMP_CTRL_MODULE_NAME = "temperature_control";
    private static final String TEMP_CTRL_STRATEGY_TAG = "strategy";
    private static final String TOUCH_COVER_PROTECTION_GAME_APP_LIST = "touch_cover_protection_game_app_list";
    private static final String TOUCH_COVER_PROTECTION_GAME_MODE = "TouchCoverProtectionGameMode";
    private boolean mAutoBrightnessStatisticsEventEnable;
    private Context mContext;
    private AtomicFile mFile;
    private Handler mHandler;
    private boolean mOverrideBrightnessPolicyEnable;
    private boolean mPointLightSourceDetectorEnable;
    private boolean mShortTermModelEnable;
    private List<String> mShortTermModelGameList = new ArrayList();
    private List<String> mShortTermModelVideoList = new ArrayList();
    private List<String> mShortTermModelMapList = new ArrayList();
    private List<String> mShortTermModelImageList = new ArrayList();
    private List<String> mShortTermModelReaderList = new ArrayList();
    private List<String> mShortTermModelGlobalList = new ArrayList();
    private Map<Integer, List<String>> mShortTermModelAppMapper = new HashMap();
    private List<String> mShortTermModelCloudAppCategoryList = new ArrayList();
    private List<String> mBCBCAppList = new ArrayList();
    private List<String> mTouchCoverProtectionGameList = new ArrayList();
    private Map<String, Map<String, Object>> mTemperatureControllerMapper = new HashMap();
    private ArrayList<Observer> mObservers = new ArrayList<>();
    private List<String> mResolutionSwitchProcessProtectList = new ArrayList();
    private List<String> mResolutionSwitchProcessBlackList = new ArrayList();

    /* loaded from: classes.dex */
    public interface Observer {
        void update();
    }

    static {
        boolean z = false;
        if (SystemProperties.getInt("debug.miui.display.cloud.dbg", 0) != 0) {
            z = true;
        }
        DEBUG = z;
    }

    public MiuiDisplayCloudController(Looper looper, Context context) {
        this.mContext = context;
        this.mHandler = new Handler(looper);
        initialization();
        registerMiuiBrightnessCloudDataObserver();
    }

    private void initialization() {
        this.mShortTermModelEnable = false;
        for (int i = 0; i < 6; i++) {
            this.mShortTermModelAppMapper.put(Integer.valueOf(i), new ArrayList());
        }
        this.mShortTermModelCloudAppCategoryList.add(SHORT_TERM_MODEL_GAME_APP_LIST);
        this.mShortTermModelCloudAppCategoryList.add(SHORT_TERM_MODEL_VIDEO_APP_LIST);
        this.mShortTermModelCloudAppCategoryList.add(SHORT_TERM_MODEL_MAP_APP_LIST);
        this.mShortTermModelCloudAppCategoryList.add(SHORT_TERM_MODEL_IMAGE_APP_LIST);
        this.mShortTermModelCloudAppCategoryList.add(SHORT_TERM_MODEL_READER_APP_LIST);
        this.mShortTermModelCloudAppCategoryList.add(SHORT_TERM_MODEL_GLOBAL_APP_LIST);
        this.mFile = getFile(CLOUD_BACKUP_FILE_NAME);
        loadLocalCloudBackup();
    }

    private void registerMiuiBrightnessCloudDataObserver() {
        this.mContext.getContentResolver().registerContentObserver(MiuiSettings.SettingsCloudData.getCloudDataNotifyUri(), true, new AnonymousClass1(this.mHandler));
    }

    /* renamed from: com.android.server.display.MiuiDisplayCloudController$1 */
    /* loaded from: classes.dex */
    public class AnonymousClass1 extends ContentObserver {
        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        AnonymousClass1(Handler handler) {
            super(handler);
            MiuiDisplayCloudController.this = this$0;
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange) {
            boolean changed = MiuiDisplayCloudController.this.updateDataFromCloudControl();
            if (changed) {
                BackgroundThread.getHandler().post(new Runnable() { // from class: com.android.server.display.MiuiDisplayCloudController$1$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        MiuiDisplayCloudController.AnonymousClass1.this.m576x40af1d2b();
                    }
                });
                MiuiDisplayCloudController.this.notifyAllObservers();
            }
        }

        /* renamed from: lambda$onChange$0$com-android-server-display-MiuiDisplayCloudController$1 */
        public /* synthetic */ void m576x40af1d2b() {
            MiuiDisplayCloudController.this.syncLocalBackupFromCloud();
        }
    }

    public void syncLocalBackupFromCloud() {
        String str;
        IOException e;
        TypedXmlSerializer out;
        AtomicFile atomicFile;
        boolean z;
        if (this.mFile == null) {
            return;
        }
        FileOutputStream outputStream = null;
        try {
            Slog.d(TAG, "Start syncing local backup from cloud.");
            FileOutputStream outputStream2 = this.mFile.startWrite();
            try {
                out = Xml.resolveSerializer(outputStream2);
                out.startDocument((String) null, true);
                out.setFeature(MiuiSizeCompatService.FAST_XML, true);
                out.startTag((String) null, CLOUD_BACKUP_FILE_ROOT_ELEMENT);
                out.startTag((String) null, CLOUD_BACKUP_FILE_SHORT_TERM_MODEL_TAG);
                atomicFile = this.mFile;
                z = this.mShortTermModelEnable;
                str = TAG;
            } catch (IOException e2) {
                e = e2;
                str = TAG;
                outputStream = outputStream2;
            }
            try {
                writeFeatureEnableToXml(atomicFile, outputStream2, out, CLOUD_BACKUP_FILE_ATTRIBUTE_ENABLE, CLOUD_BACKUP_FILE_SHORT_TERM_MODEL_ENABLE, z);
                writeShortTermModelAppListToXml(this.mFile, outputStream2, out);
                out.endTag((String) null, CLOUD_BACKUP_FILE_SHORT_TERM_MODEL_TAG);
                out.startTag((String) null, CLOUD_BACKUP_FILE_BCBC_TAG);
                writeElementOfAppListToXml(this.mFile, outputStream2, out, this.mBCBCAppList, CLOUD_BACKUP_FILE_ATTRIBUTE_PACKAGE, CLOUD_BACKUP_FILE_BCBC_TAG_APP);
                out.endTag((String) null, CLOUD_BACKUP_FILE_BCBC_TAG);
                out.startTag((String) null, CLOUD_BACKUP_FILE_RESOLUTION_SWITCH_TAG);
                writeElementOfAppListToXml(this.mFile, outputStream2, out, this.mResolutionSwitchProcessProtectList, CLOUD_BACKUP_FILE_ATTRIBUTE_ITEM, CLOUD_BACKUP_FILE_RESOLUTION_SWITCH_TAG_PROCESS_PROTECT);
                writeElementOfAppListToXml(this.mFile, outputStream2, out, this.mResolutionSwitchProcessBlackList, CLOUD_BACKUP_FILE_ATTRIBUTE_ITEM, CLOUD_BACKUP_FILE_RESOLUTION_SWITCH_TAG_PROCESS_BLACK);
                out.endTag((String) null, CLOUD_BACKUP_FILE_RESOLUTION_SWITCH_TAG);
                out.startTag((String) null, "point_light_source_detector_enable");
                writeFeatureEnableToXml(this.mFile, outputStream2, out, CLOUD_BACKUP_FILE_ATTRIBUTE_ENABLE, CLOUD_BACKUP_FILE_POINT_LIGHT_SOURCE_DETECTOR_ENABLE, this.mPointLightSourceDetectorEnable);
                out.endTag((String) null, "point_light_source_detector_enable");
                out.startTag((String) null, "override_brightness_policy_enable");
                writeFeatureEnableToXml(this.mFile, outputStream2, out, CLOUD_BACKUP_FILE_ATTRIBUTE_ENABLE, CLOUD_BACKUP_FILE_OVERRIDE_BRIGHTNESS_POLICY_ENABLE, this.mOverrideBrightnessPolicyEnable);
                out.endTag((String) null, "override_brightness_policy_enable");
                out.startTag((String) null, "automatic_brightness_statistics_event_enable");
                writeFeatureEnableToXml(this.mFile, outputStream2, out, CLOUD_BACKUP_FILE_ATTRIBUTE_ENABLE, CLOUD_BACKUP_FILE_AUTO_BRIGHTNESS_STATISTICS_EVENT_ENABLE, this.mAutoBrightnessStatisticsEventEnable);
                out.endTag((String) null, "automatic_brightness_statistics_event_enable");
                out.startTag((String) null, CLOUD_BACKUP_FILE_TEMP_CRT_TAG);
                writeTemperatureControlConfig(out, this.mTemperatureControllerMapper, TEMP_CTRL_STRATEGY_TAG);
                out.endTag((String) null, CLOUD_BACKUP_FILE_TEMP_CRT_TAG);
                out.startTag((String) null, CLOUD_BACKUP_FILE_TOUCH_COVER_PROTECTION_GAME_TAG);
                writeElementOfAppListToXml(this.mFile, outputStream2, out, this.mTouchCoverProtectionGameList, CLOUD_BACKUP_FILE_ATTRIBUTE_PACKAGE, CLOUD_BACKUP_FILE_TOUCH_COVER_PROTECTION_GAME_TAG_APP);
                out.endTag((String) null, CLOUD_BACKUP_FILE_TOUCH_COVER_PROTECTION_GAME_TAG);
                out.endTag((String) null, CLOUD_BACKUP_FILE_ROOT_ELEMENT);
                out.endDocument();
                outputStream2.flush();
                this.mFile.finishWrite(outputStream2);
            } catch (IOException e3) {
                e = e3;
                outputStream = outputStream2;
                this.mFile.failWrite(outputStream);
                Slog.e(str, "Failed to write local backup" + e);
            }
        } catch (IOException e4) {
            e = e4;
            str = TAG;
        }
    }

    private void writeFeatureEnableToXml(AtomicFile writeFile, FileOutputStream outStream, TypedXmlSerializer out, String attribute, String tag, boolean enable) {
        try {
            out.startTag((String) null, tag);
            out.attributeBoolean((String) null, attribute, enable);
            out.endTag((String) null, tag);
        } catch (IOException e) {
            writeFile.failWrite(outStream);
            Slog.e(TAG, "Failed to write local backup of feature enable" + e);
        }
    }

    private void writeShortTermModelAppListToXml(AtomicFile writeFile, FileOutputStream outStream, TypedXmlSerializer out) {
        for (int category = 0; category < 6; category++) {
            switch (category) {
                case 0:
                    writeElementOfAppListToXml(writeFile, outStream, out, this.mShortTermModelGlobalList, CLOUD_BACKUP_FILE_ATTRIBUTE_PACKAGE, CLOUD_BACKUP_FILE_CATEGORY_TAG_UNDEFINED);
                    break;
                case 1:
                    writeElementOfAppListToXml(writeFile, outStream, out, this.mShortTermModelGameList, CLOUD_BACKUP_FILE_ATTRIBUTE_PACKAGE, CLOUD_BACKUP_FILE_CATEGORY_TAG_GAME);
                    break;
                case 2:
                    writeElementOfAppListToXml(writeFile, outStream, out, this.mShortTermModelVideoList, CLOUD_BACKUP_FILE_ATTRIBUTE_PACKAGE, CLOUD_BACKUP_FILE_CATEGORY_TAG_VIDEO);
                    break;
                case 3:
                    writeElementOfAppListToXml(writeFile, outStream, out, this.mShortTermModelMapList, CLOUD_BACKUP_FILE_ATTRIBUTE_PACKAGE, CLOUD_BACKUP_FILE_CATEGORY_TAG_MAP);
                    break;
                case 4:
                    writeElementOfAppListToXml(writeFile, outStream, out, this.mShortTermModelImageList, CLOUD_BACKUP_FILE_ATTRIBUTE_PACKAGE, CLOUD_BACKUP_FILE_CATEGORY_TAG_IMAGE);
                    break;
                case 5:
                    writeElementOfAppListToXml(writeFile, outStream, out, this.mShortTermModelReaderList, CLOUD_BACKUP_FILE_ATTRIBUTE_PACKAGE, CLOUD_BACKUP_FILE_CATEGORY_TAG_READER);
                    break;
            }
        }
    }

    private void writeElementOfAppListToXml(AtomicFile writeFile, FileOutputStream outStream, TypedXmlSerializer out, List<String> list, String attribute, String tag) {
        try {
            for (String str : list) {
                out.startTag((String) null, tag);
                out.attribute((String) null, attribute, str);
                out.endTag((String) null, tag);
            }
        } catch (IOException e) {
            writeFile.failWrite(outStream);
            Slog.e(TAG, "Failed to write element of app list to xml" + e);
        }
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    private void readCloudDataFromXml(InputStream stream) {
        try {
            Slog.d(TAG, "Start reading cloud data from xml.");
            TypedXmlPullParser parser = Xml.resolvePullParser(stream);
            while (true) {
                int type = parser.next();
                char c = 1;
                if (type != 1) {
                    if (type != 3 && type != 4) {
                        String tag = parser.getName();
                        switch (tag.hashCode()) {
                            case -1934021512:
                                if (tag.equals(CLOUD_BACKUP_FILE_POINT_LIGHT_SOURCE_DETECTOR_ENABLE)) {
                                    c = '\n';
                                    break;
                                }
                                c = 65535;
                                break;
                            case -1856325139:
                                if (tag.equals(CLOUD_BACKUP_FILE_SHORT_TERM_MODEL_ENABLE)) {
                                    c = 0;
                                    break;
                                }
                                c = 65535;
                                break;
                            case -1571076377:
                                if (tag.equals(CLOUD_BACKUP_FILE_RESOLUTION_SWITCH_TAG_PROCESS_PROTECT)) {
                                    c = '\b';
                                    break;
                                }
                                c = 65535;
                                break;
                            case -1535712689:
                                if (tag.equals(CLOUD_BACKUP_FILE_CATEGORY_TAG_MAP)) {
                                    c = 3;
                                    break;
                                }
                                c = 65535;
                                break;
                            case -1367734768:
                                if (tag.equals(CLOUD_BACKUP_FILE_CATEGORY_TAG_VIDEO)) {
                                    c = 2;
                                    break;
                                }
                                c = 65535;
                                break;
                            case -1167375239:
                                if (tag.equals(CLOUD_BACKUP_FILE_CATEGORY_TAG_GAME)) {
                                    break;
                                }
                                c = 65535;
                                break;
                            case -688988814:
                                if (tag.equals(CLOUD_BACKUP_FILE_TOUCH_COVER_PROTECTION_GAME_TAG_APP)) {
                                    c = 14;
                                    break;
                                }
                                c = 65535;
                                break;
                            case -436205034:
                                if (tag.equals(CLOUD_BACKUP_FILE_BCBC_TAG_APP)) {
                                    c = 7;
                                    break;
                                }
                                c = 65535;
                                break;
                            case 456725192:
                                if (tag.equals(CLOUD_BACKUP_FILE_CATEGORY_TAG_READER)) {
                                    c = 5;
                                    break;
                                }
                                c = 65535;
                                break;
                            case 1089152535:
                                if (tag.equals(CLOUD_BACKUP_FILE_RESOLUTION_SWITCH_TAG_PROCESS_BLACK)) {
                                    c = '\t';
                                    break;
                                }
                                c = 65535;
                                break;
                            case 1181635811:
                                if (tag.equals(CLOUD_BACKUP_FILE_OVERRIDE_BRIGHTNESS_POLICY_ENABLE)) {
                                    c = 11;
                                    break;
                                }
                                c = 65535;
                                break;
                            case 1778573798:
                                if (tag.equals(CLOUD_BACKUP_FILE_AUTO_BRIGHTNESS_STATISTICS_EVENT_ENABLE)) {
                                    c = '\f';
                                    break;
                                }
                                c = 65535;
                                break;
                            case 1787798387:
                                if (tag.equals(TEMP_CTRL_STRATEGY_TAG)) {
                                    c = '\r';
                                    break;
                                }
                                c = 65535;
                                break;
                            case 1855674587:
                                if (tag.equals(CLOUD_BACKUP_FILE_CATEGORY_TAG_UNDEFINED)) {
                                    c = 6;
                                    break;
                                }
                                c = 65535;
                                break;
                            case 2014899504:
                                if (tag.equals(CLOUD_BACKUP_FILE_CATEGORY_TAG_IMAGE)) {
                                    c = 4;
                                    break;
                                }
                                c = 65535;
                                break;
                            default:
                                c = 65535;
                                break;
                        }
                        switch (c) {
                            case 0:
                                this.mShortTermModelEnable = parser.getAttributeBoolean((String) null, CLOUD_BACKUP_FILE_ATTRIBUTE_ENABLE, false);
                                break;
                            case 1:
                                saveAppListFromXml(parser, CLOUD_BACKUP_FILE_ATTRIBUTE_PACKAGE, this.mShortTermModelGameList);
                                break;
                            case 2:
                                saveAppListFromXml(parser, CLOUD_BACKUP_FILE_ATTRIBUTE_PACKAGE, this.mShortTermModelVideoList);
                                break;
                            case 3:
                                saveAppListFromXml(parser, CLOUD_BACKUP_FILE_ATTRIBUTE_PACKAGE, this.mShortTermModelMapList);
                                break;
                            case 4:
                                saveAppListFromXml(parser, CLOUD_BACKUP_FILE_ATTRIBUTE_PACKAGE, this.mShortTermModelImageList);
                                break;
                            case 5:
                                saveAppListFromXml(parser, CLOUD_BACKUP_FILE_ATTRIBUTE_PACKAGE, this.mShortTermModelReaderList);
                                break;
                            case 6:
                                saveAppListFromXml(parser, CLOUD_BACKUP_FILE_ATTRIBUTE_PACKAGE, this.mShortTermModelGlobalList);
                                break;
                            case 7:
                                saveAppListFromXml(parser, CLOUD_BACKUP_FILE_ATTRIBUTE_PACKAGE, this.mBCBCAppList);
                                break;
                            case '\b':
                                saveAppListFromXml(parser, CLOUD_BACKUP_FILE_ATTRIBUTE_ITEM, this.mResolutionSwitchProcessProtectList);
                                break;
                            case '\t':
                                saveAppListFromXml(parser, CLOUD_BACKUP_FILE_ATTRIBUTE_ITEM, this.mResolutionSwitchProcessBlackList);
                                break;
                            case '\n':
                                this.mPointLightSourceDetectorEnable = parser.getAttributeBoolean((String) null, CLOUD_BACKUP_FILE_ATTRIBUTE_ENABLE, false);
                                break;
                            case 11:
                                this.mOverrideBrightnessPolicyEnable = parser.getAttributeBoolean((String) null, CLOUD_BACKUP_FILE_ATTRIBUTE_ENABLE, false);
                                break;
                            case '\f':
                                this.mAutoBrightnessStatisticsEventEnable = parser.getAttributeBoolean((String) null, CLOUD_BACKUP_FILE_ATTRIBUTE_ENABLE, false);
                                break;
                            case '\r':
                                saveTemperatureControlConfig(parser, this.mTemperatureControllerMapper);
                                break;
                            case 14:
                                saveAppListFromXml(parser, CLOUD_BACKUP_FILE_ATTRIBUTE_PACKAGE, this.mTouchCoverProtectionGameList);
                                break;
                        }
                    }
                } else {
                    notifyResolutionSwitchListChanged();
                    return;
                }
            }
        } catch (IOException | XmlPullParserException e) {
            Slog.e(TAG, "Failed to parse local cloud backup file" + e);
        }
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    private void loadResolutionSwitchListFromXml(InputStream stream) {
        try {
            Slog.d(TAG, "Start loading resolution switch process list from xml.");
            TypedXmlPullParser parser = Xml.resolvePullParser(stream);
            int currentTag = 0;
            while (true) {
                int type = parser.next();
                if (type != 1) {
                    if (type != 4 && type != 3) {
                        String tag = parser.getName();
                        char c = 65535;
                        switch (tag.hashCode()) {
                            case -1571076377:
                                if (tag.equals(CLOUD_BACKUP_FILE_RESOLUTION_SWITCH_TAG_PROCESS_PROTECT)) {
                                    c = 0;
                                    break;
                                }
                                break;
                            case 3242771:
                                if (tag.equals(CLOUD_BACKUP_FILE_ATTRIBUTE_ITEM)) {
                                    c = 2;
                                    break;
                                }
                                break;
                            case 1089152535:
                                if (tag.equals(CLOUD_BACKUP_FILE_RESOLUTION_SWITCH_TAG_PROCESS_BLACK)) {
                                    c = 1;
                                    break;
                                }
                                break;
                        }
                        switch (c) {
                            case 0:
                                currentTag = 1;
                                break;
                            case 1:
                                currentTag = 2;
                                break;
                            case 2:
                                if (currentTag == 1) {
                                    this.mResolutionSwitchProcessProtectList.add(parser.nextText());
                                    break;
                                } else if (currentTag == 2) {
                                    this.mResolutionSwitchProcessBlackList.add(parser.nextText());
                                    break;
                                }
                                break;
                        }
                    }
                } else {
                    notifyResolutionSwitchListChanged();
                    return;
                }
            }
        } catch (IOException | XmlPullParserException e) {
            Slog.e(TAG, "Failed to parse local cloud backup file" + e);
        }
    }

    private void saveAppListFromXml(TypedXmlPullParser parser, String attribute, List<String> list) {
        if (parser.getAttributeValue((String) null, attribute) != null) {
            list.add(parser.getAttributeValue((String) null, attribute));
        }
    }

    public boolean updateDataFromCloudControl() {
        boolean updated = false | updateShortTermModelState();
        return updated | updateBCBCAppList() | updateResolutionSwitchList() | updatePointLightSourceDetectorEnable() | updateTouchProtectionGameList() | updateOverrideBrightnessPolicyEnable() | updateAutoBrightnessStatisticsEventEnableState() | updateTemperatureControllerConfig();
    }

    private void loadLocalCloudBackup() {
        AtomicFile atomicFile = this.mFile;
        if (atomicFile != null && atomicFile.exists()) {
            FileInputStream inputStream = null;
            try {
                try {
                    inputStream = this.mFile.openRead();
                    readCloudDataFromXml(inputStream);
                    saveShortTermModelAppComponent(null);
                    notifyAllObservers();
                } catch (IOException e) {
                    this.mFile.delete();
                    Slog.e(TAG, "Failed to read local cloud backup" + e);
                }
                return;
            } finally {
            }
        }
        FileInputStream inputStream2 = null;
        AtomicFile file = new AtomicFile(Environment.buildPath(Environment.getProductDirectory(), new String[]{"etc/displayconfig", RESOLUTION_SWITCH_PROCESS_LIST_BACKUP_FILE}));
        try {
            try {
                inputStream2 = file.openRead();
                loadResolutionSwitchListFromXml(inputStream2);
            } catch (IOException e2) {
                file.delete();
                Slog.e(TAG, "Failed to read local cloud backup" + e2);
            }
        } finally {
        }
    }

    private AtomicFile getFile(String fileName) {
        return new AtomicFile(new File(Environment.getDataSystemDeDirectory(), fileName));
    }

    private boolean updateShortTermModelState() {
        return !DEBUG && updateShortTermModelEnable() && updateShortTermModelAppList();
    }

    private boolean updateShortTermModelAppList() {
        MiuiSettings.SettingsCloudData.CloudData data = MiuiSettings.SettingsCloudData.getCloudDataSingle(this.mContext.getContentResolver(), SHORT_TREM_MODEL_APP_MODULE_NAME, SHORT_TERM_MODEL_APP_CONFIG, (String) null, false);
        if (data == null || data.json() == null) {
            Slog.w(TAG, "Failed to update short term model apps from cloud.");
            return false;
        }
        JSONArray jsonArray = data.json().optJSONArray(SHORT_TERM_MODEL_APP_CONFIG);
        if (jsonArray != null) {
            JSONObject jsonObject = jsonArray.optJSONObject(0);
            Slog.d(TAG, "Update short term model apps.");
            saveShortTermModelAppComponent(jsonObject);
            return true;
        }
        return true;
    }

    private boolean updateShortTermModelEnable() {
        MiuiSettings.SettingsCloudData.CloudData data = MiuiSettings.SettingsCloudData.getCloudDataSingle(this.mContext.getContentResolver(), SHORT_TERM_MODEL_MODULE_NAME, SHORT_TERM_MODEL_ENABLE, (String) null, false);
        if (data == null || data.json() == null) {
            Slog.w(TAG, "Failed to update short term model enable from cloud.");
            return false;
        }
        this.mShortTermModelEnable = data.json().optBoolean(SHORT_TERM_MODEL_ENABLE);
        Slog.d(TAG, "Update short term model enable: " + this.mShortTermModelEnable);
        return true;
    }

    private boolean updateAutoBrightnessStatisticsEventEnableState() {
        if (DEBUG) {
            return false;
        }
        return updateStatisticsAutoBrightnessEventEnable();
    }

    private boolean updateStatisticsAutoBrightnessEventEnable() {
        MiuiSettings.SettingsCloudData.CloudData data = MiuiSettings.SettingsCloudData.getCloudDataSingle(this.mContext.getContentResolver(), AUTO_BRIGHTNESS_STATISTICS_EVENT_MODULE_NAME, "automatic_brightness_statistics_event_enable", (String) null, false);
        if (data == null || data.json() == null) {
            Slog.w(TAG, "Failed to upload automatic brightness statistics event enable from cloud.");
            return false;
        }
        this.mAutoBrightnessStatisticsEventEnable = data.json().optBoolean("automatic_brightness_statistics_event_enable");
        Slog.d(TAG, "Update automatic brightness statistics event enable: " + this.mAutoBrightnessStatisticsEventEnable);
        return true;
    }

    private void saveShortTermModelAppComponent(JSONObject jsonObject) {
        for (String str : this.mShortTermModelCloudAppCategoryList) {
            char c = 65535;
            switch (str.hashCode()) {
                case -1793386205:
                    if (str.equals(SHORT_TERM_MODEL_GAME_APP_LIST)) {
                        c = 0;
                        break;
                    }
                    break;
                case -1689699578:
                    if (str.equals(SHORT_TERM_MODEL_VIDEO_APP_LIST)) {
                        c = 1;
                        break;
                    }
                    break;
                case -1617342267:
                    if (str.equals(SHORT_TERM_MODEL_MAP_APP_LIST)) {
                        c = 2;
                        break;
                    }
                    break;
                case 970256626:
                    if (str.equals(SHORT_TERM_MODEL_READER_APP_LIST)) {
                        c = 4;
                        break;
                    }
                    break;
                case 1692934694:
                    if (str.equals(SHORT_TERM_MODEL_IMAGE_APP_LIST)) {
                        c = 3;
                        break;
                    }
                    break;
                case 2029224466:
                    if (str.equals(SHORT_TERM_MODEL_GLOBAL_APP_LIST)) {
                        c = 5;
                        break;
                    }
                    break;
            }
            switch (c) {
                case 0:
                    saveObjectAsListIfNeeded(jsonObject, str, this.mShortTermModelGameList);
                    this.mShortTermModelAppMapper.put(1, this.mShortTermModelGameList);
                    break;
                case 1:
                    saveObjectAsListIfNeeded(jsonObject, str, this.mShortTermModelVideoList);
                    this.mShortTermModelAppMapper.put(2, this.mShortTermModelVideoList);
                    break;
                case 2:
                    saveObjectAsListIfNeeded(jsonObject, str, this.mShortTermModelMapList);
                    this.mShortTermModelAppMapper.put(3, this.mShortTermModelMapList);
                    break;
                case 3:
                    saveObjectAsListIfNeeded(jsonObject, str, this.mShortTermModelImageList);
                    this.mShortTermModelAppMapper.put(4, this.mShortTermModelImageList);
                    break;
                case 4:
                    saveObjectAsListIfNeeded(jsonObject, str, this.mShortTermModelReaderList);
                    this.mShortTermModelAppMapper.put(5, this.mShortTermModelReaderList);
                    break;
                case 5:
                    saveObjectAsListIfNeeded(jsonObject, str, this.mShortTermModelGlobalList);
                    this.mShortTermModelAppMapper.put(0, this.mShortTermModelGlobalList);
                    break;
            }
        }
    }

    private void saveObjectAsListIfNeeded(JSONObject jsonObject, String str, List<String> list) {
        if (jsonObject == null || list == null) {
            return;
        }
        JSONArray appArray = jsonObject.optJSONArray(str);
        if (appArray != null) {
            list.clear();
            for (int i = 0; i < appArray.length(); i++) {
                Object obj = appArray.opt(i);
                if (obj != null) {
                    list.add((String) obj);
                }
            }
            return;
        }
        Slog.d(TAG, "Such category apps are removed.");
    }

    public boolean isShortTermModelEnable() {
        return this.mShortTermModelEnable;
    }

    public boolean isAutoBrightnessStatisticsEventEnable() {
        return this.mAutoBrightnessStatisticsEventEnable;
    }

    public Map<Integer, List<String>> getShortTermModelAppMapper() {
        return this.mShortTermModelAppMapper;
    }

    public void dump(PrintWriter pw) {
        pw.println();
        pw.println("MiuiDisplayCloudController Configuration:");
        pw.println("  mShortTermModelEnable=" + this.mShortTermModelEnable);
        pw.println("  mShortTermModelAppMapper=" + this.mShortTermModelAppMapper);
        pw.println("  mBCBCAppList=" + this.mBCBCAppList);
        pw.println("  mTouchCoverProtectionGameList=" + this.mTouchCoverProtectionGameList);
        pw.println("  mResolutionSwitchProcessProtectList=" + this.mResolutionSwitchProcessProtectList);
        pw.println("  mResolutionSwitchProcessBlackList=" + this.mResolutionSwitchProcessBlackList);
        pw.println("  mPointLightSourceDetectorEnable=" + this.mPointLightSourceDetectorEnable);
        pw.println("  mOverrideBrightnessPolicyEnable=" + this.mOverrideBrightnessPolicyEnable);
        pw.println("  mAutoBrightnessStatisticsEventEnable=" + this.mAutoBrightnessStatisticsEventEnable);
        pw.println("  mTemperatureControllerMapper=" + this.mTemperatureControllerMapper);
    }

    public List<String> getBCBCAppList() {
        return this.mBCBCAppList;
    }

    private boolean updateBCBCAppList() {
        MiuiSettings.SettingsCloudData.CloudData data = MiuiSettings.SettingsCloudData.getCloudDataSingle(this.mContext.getContentResolver(), BCBC_FEATURE_MODULE_NAME, BCBC_APP_CONFIG, (String) null, false);
        if (data == null || data.json() == null) {
            Slog.w(TAG, "Failed to update BCBC apps from cloud.");
        } else {
            JSONArray appArray = data.json().optJSONArray(BCBC_APP_CONFIG);
            if (appArray != null) {
                this.mBCBCAppList.clear();
                Slog.d(TAG, "Update BCBC apps.");
                for (int i = 0; i < appArray.length(); i++) {
                    Object obj = appArray.opt(i);
                    if (obj != null) {
                        this.mBCBCAppList.add((String) obj);
                    }
                }
                return true;
            }
        }
        return false;
    }

    private boolean updateResolutionSwitchList() {
        MiuiSettings.SettingsCloudData.CloudData data = MiuiSettings.SettingsCloudData.getCloudDataSingle(this.mContext.getContentResolver(), RESOLUTION_SWITCH_PROCESS_LIST_MODEULE_NAME, PROCESS_RESOLUTION_SWITCH_LIST, (String) null, false);
        if (data == null || data.json() == null) {
            Slog.w(TAG, "Failed to update Resolution switch list from cloud.");
        } else {
            JSONArray appArray = data.json().optJSONArray(PROCESS_RESOLUTION_SWITCH_LIST);
            if (appArray != null) {
                JSONObject jsonObject = appArray.optJSONObject(0);
                Slog.d(TAG, "Update Resolution switch list from cloud");
                saveObjectAsListIfNeeded(jsonObject, PROCESS_RESOLUTION_SWITCH_PROTECT_LIST, this.mResolutionSwitchProcessProtectList);
                saveObjectAsListIfNeeded(jsonObject, PROCESS_RESOLUTION_SWITCH_BLACK_LIST, this.mResolutionSwitchProcessBlackList);
                notifyResolutionSwitchListChanged();
                return true;
            }
        }
        return false;
    }

    public List<String> getTouchCoverProtectionGameList() {
        return this.mTouchCoverProtectionGameList;
    }

    private boolean updateTouchProtectionGameList() {
        MiuiSettings.SettingsCloudData.CloudData data = MiuiSettings.SettingsCloudData.getCloudDataSingle(this.mContext.getContentResolver(), TOUCH_COVER_PROTECTION_GAME_MODE, TOUCH_COVER_PROTECTION_GAME_APP_LIST, (String) null, false);
        if (data == null || data.json() == null) {
            Slog.w(TAG, "Failed to update game apps from cloud.");
        } else {
            JSONArray appArray = data.json().optJSONArray(TOUCH_COVER_PROTECTION_GAME_APP_LIST);
            if (appArray != null) {
                this.mTouchCoverProtectionGameList.clear();
                Slog.d(TAG, "Update game apps.");
                for (int i = 0; i < appArray.length(); i++) {
                    Object obj = appArray.opt(i);
                    if (obj != null) {
                        this.mTouchCoverProtectionGameList.add((String) obj);
                    }
                }
                return true;
            }
        }
        return false;
    }

    private void notifyResolutionSwitchListChanged() {
        DisplayManagerServiceStub.getInstance().updateResolutionSwitchList(this.mResolutionSwitchProcessProtectList, this.mResolutionSwitchProcessBlackList);
    }

    private boolean updatePointLightSourceDetectorEnable() {
        if (DEBUG) {
            return false;
        }
        MiuiSettings.SettingsCloudData.CloudData data = MiuiSettings.SettingsCloudData.getCloudDataSingle(this.mContext.getContentResolver(), POINT_LIGHT_SOURCE_MODULE_NAME, "point_light_source_detector_enable", (String) null, false);
        if (data == null || data.json() == null) {
            Slog.w(TAG, "Failed to update point light source detector enable from cloud.");
            return false;
        }
        this.mPointLightSourceDetectorEnable = data.json().optBoolean("point_light_source_detector_enable");
        Slog.d(TAG, "Update point light source detector enable: " + this.mPointLightSourceDetectorEnable);
        return true;
    }

    private boolean updateTemperatureControllerConfig() {
        MiuiSettings.SettingsCloudData.CloudData data;
        JSONException e;
        MiuiSettings.SettingsCloudData.CloudData data2 = MiuiSettings.SettingsCloudData.getCloudDataSingle(this.mContext.getContentResolver(), TEMP_CTRL_MODULE_NAME, TEMP_CTRL_CONFIG, (String) null, false);
        if (data2 == null || data2.json() == null) {
            Slog.w(TAG, "Failed to update temperature controller config from cloud.");
            return false;
        }
        JSONArray appArray = data2.json().optJSONArray(TEMP_CTRL_CONFIG);
        if (appArray != null) {
            Slog.d(TAG, "Update temperature controller config.");
            int i = 0;
            while (i < appArray.length()) {
                JSONObject obj = appArray.optJSONObject(i);
                if (obj == null) {
                    data = data2;
                } else {
                    try {
                        List<Pair> list = readStrategyFromJson(obj);
                        Map<String, Map<String, Object>> map = new HashMap<>();
                        for (Pair<String, JSONObject> p : list) {
                            JSONObject strategy = (JSONObject) p.second;
                            Map<String, Object> item = new HashMap<>();
                            List<Pair> list2 = list;
                            data = data2;
                            try {
                                item.put("brightness", strategy.getString("brightness"));
                                item.put(TemperatureController.STRATEGY_TEMPERATURE, strategy.getString(TemperatureController.STRATEGY_TEMPERATURE));
                                item.put(TemperatureController.STRATEGY_UP_THRESHOLD, Integer.valueOf(strategy.getInt(TemperatureController.STRATEGY_UP_THRESHOLD)));
                                item.put(TemperatureController.STRATEGY_DOWN_THRESHOLD, Integer.valueOf(strategy.getInt(TemperatureController.STRATEGY_DOWN_THRESHOLD)));
                                item.put(TemperatureController.STRATEGY_AVERAGE_TIMES, Integer.valueOf(strategy.getInt(TemperatureController.STRATEGY_AVERAGE_TIMES)));
                                map.put((String) p.first, item);
                                list = list2;
                                data2 = data;
                            } catch (JSONException e2) {
                                e = e2;
                                Slog.e(TAG, "Failed to parse temperature controller config json" + e);
                                i++;
                                data2 = data;
                            }
                        }
                        data = data2;
                        this.mTemperatureControllerMapper.clear();
                        this.mTemperatureControllerMapper.putAll(map);
                        return true;
                    } catch (JSONException e3) {
                        e = e3;
                        data = data2;
                    }
                }
                i++;
                data2 = data;
            }
            return false;
        }
        return false;
    }

    private ArrayList<Pair> readStrategyFromJson(JSONObject json) throws JSONException {
        ArrayList<Pair> list = new ArrayList<>();
        if (json != null) {
            Iterator<String> it = json.keys();
            while (it.hasNext()) {
                String headerkey = it.next();
                JSONObject obj = json.getJSONObject(headerkey);
                list.add(new Pair(headerkey, obj));
            }
        }
        return list;
    }

    private void writeTemperatureControlConfig(TypedXmlSerializer out, Map<String, Map<String, Object>> map, String tag) throws IOException {
        for (Map.Entry<String, Map<String, Object>> entry : map.entrySet()) {
            out.startTag((String) null, tag);
            String name = entry.getKey();
            Map item = entry.getValue();
            out.attribute((String) null, TemperatureController.STRATEGY_NAME, name);
            out.attribute((String) null, "brightness", (String) item.get("brightness"));
            out.attribute((String) null, TemperatureController.STRATEGY_TEMPERATURE, (String) item.get(TemperatureController.STRATEGY_TEMPERATURE));
            out.attributeInt((String) null, TemperatureController.STRATEGY_DOWN_THRESHOLD, ((Integer) item.get(TemperatureController.STRATEGY_DOWN_THRESHOLD)).intValue());
            out.attributeInt((String) null, TemperatureController.STRATEGY_UP_THRESHOLD, ((Integer) item.get(TemperatureController.STRATEGY_UP_THRESHOLD)).intValue());
            out.attributeInt((String) null, TemperatureController.STRATEGY_AVERAGE_TIMES, ((Integer) item.get(TemperatureController.STRATEGY_AVERAGE_TIMES)).intValue());
            out.endTag((String) null, tag);
        }
    }

    public void saveTemperatureControlConfig(TypedXmlPullParser parser, Map<String, Map<String, Object>> map) {
        try {
            Map<String, Object> item = new HashMap<>();
            String name = parser.getAttributeValue((String) null, TemperatureController.STRATEGY_NAME);
            item.put("brightness", parser.getAttributeValue((String) null, "brightness"));
            item.put(TemperatureController.STRATEGY_TEMPERATURE, parser.getAttributeValue((String) null, TemperatureController.STRATEGY_TEMPERATURE));
            item.put(TemperatureController.STRATEGY_UP_THRESHOLD, Integer.valueOf(parser.getAttributeInt((String) null, TemperatureController.STRATEGY_UP_THRESHOLD)));
            item.put(TemperatureController.STRATEGY_DOWN_THRESHOLD, Integer.valueOf(parser.getAttributeInt((String) null, TemperatureController.STRATEGY_DOWN_THRESHOLD)));
            item.put(TemperatureController.STRATEGY_AVERAGE_TIMES, Integer.valueOf(parser.getAttributeInt((String) null, TemperatureController.STRATEGY_AVERAGE_TIMES)));
            map.put(name, item);
        } catch (XmlPullParserException e) {
            Slog.e(TAG, "Failed to parse temperatue controller config from xml.");
        }
    }

    public boolean isPointLightSourceDetectorEnable() {
        return this.mPointLightSourceDetectorEnable;
    }

    private boolean updateOverrideBrightnessPolicyEnable() {
        MiuiSettings.SettingsCloudData.CloudData data = MiuiSettings.SettingsCloudData.getCloudDataSingle(this.mContext.getContentResolver(), OVERRIDE_BRIGHTNESS_POLICY_MODULE_NAME, "override_brightness_policy_enable", (String) null, false);
        if (data == null || data.json() == null) {
            Slog.w(TAG, "Failed to update override brightness policy enable from cloud.");
            return false;
        }
        this.mOverrideBrightnessPolicyEnable = data.json().optBoolean("override_brightness_policy_enable");
        Slog.d(TAG, "Update override brightness policy enable: " + this.mOverrideBrightnessPolicyEnable);
        return true;
    }

    public boolean isOverrideBrightnessPolicyEnable() {
        return this.mOverrideBrightnessPolicyEnable;
    }

    public Map<String, Map<String, Object>> getTemperatureControllerMapper() {
        return this.mTemperatureControllerMapper;
    }

    public void registerObserver(Observer observer) {
        Objects.requireNonNull(observer, "observer may not be null");
        if (!this.mObservers.contains(observer)) {
            this.mObservers.add(observer);
        }
    }

    public void unregisterObserver(Observer observer) {
        Objects.requireNonNull(observer, "observer may not be null");
        this.mObservers.remove(observer);
    }

    public void notifyAllObservers() {
        Iterator<Observer> it = this.mObservers.iterator();
        while (it.hasNext()) {
            Observer observer = it.next();
            observer.update();
        }
    }
}
