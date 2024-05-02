package com.miui.server.input.edgesuppression;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.XmlResourceParser;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.UserHandle;
import android.provider.MiuiSettings;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Slog;
import android.view.WindowManager;
import com.android.internal.util.XmlUtils;
import com.android.server.input.InputOneTrackUtil;
import com.android.server.input.MiuiInputThread;
import com.android.server.wm.DisplayFrames;
import com.android.server.wm.MiuiFreeformPinManagerService;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import miui.util.FeatureParser;
import miui.util.HoldSensorWrapper;
import miui.util.ITouchFeature;
import miui.util.LaySensorWrapper;
/* loaded from: classes.dex */
public class EdgeSuppressionManager {
    public static final String HORIZONTAL_EDGE_SUPPRESSION_SIZE = "horizontal_edge_suppression_size";
    private static final int INDEX_OF_DEFAULT = 2;
    private static final int INDEX_OF_MAX = 4;
    private static final int INDEX_OF_MIN = 0;
    private static final int INDEX_OF_STRONG = 3;
    private static final int INDEX_OF_WAKE = 1;
    private static final String KEY_SCREEN_EDGE_MODE_CUSTOM = "custom_suppression";
    private static final String KEY_SCREEN_EDGE_MODE_DEFAULT = "default_suppression";
    private static final String KEY_SCREEN_EDGE_MODE_DIY = "diy_suppression";
    private static final String KEY_SCREEN_EDGE_MODE_STRONG = "strong_suppression";
    private static final String KEY_SCREEN_EDGE_MODE_WAKE = "wake_suppression";
    public static final String REASON_OF_CONFIGURATION = "configuration";
    public static final String REASON_OF_GAMEBOOSTER = "gameBooster";
    public static final String REASON_OF_HOLDSENSOR = "holdSensor";
    public static final String REASON_OF_LAYSENSOR = "laySensor";
    public static final String REASON_OF_ROTATION = "rotation";
    public static final String REASON_OF_SCREENON = "screenOn";
    public static final String REASON_OF_SETTINGS = "settings";
    private static final String SUPPORT_SENSOR = "support_edgesuppression_with_sensor";
    private static final String TAG = "EdgeSuppressionManager";
    private static final String TRACK_EDGE_TYPE_DEFAULT = "none";
    private static final String TRACK_EVENT_TIME = "edge_suppression_track_time";
    public static final String VERTICAL_EDGE_SUPPRESSION_SIZE = "vertical_edge_suppression_size";
    private static volatile EdgeSuppressionManager sInstance;
    private int mAbsoluteSize;
    private BaseEdgeSuppression mBaseEdgeSuppression;
    private int mConditionSize;
    private final ConfigLoader mConfigLoader;
    private final Context mContext;
    private float mEdgeModeSize;
    private boolean mFolded;
    private final Handler mHandler;
    private HoldSensorWrapper mHoldSensorWrapper;
    private int mLastEdgeModeSize;
    private String mLastEdgeModeType;
    private LaySensorWrapper mLaySensorWrapper;
    private int mMaxAdjustValue;
    private int mScreenHeight;
    private int mScreenWidth;
    public static final boolean IS_SUPPORT_EDGE_MODE = ITouchFeature.getInstance().hasSupportEdgeMode();
    public static final boolean SHOULD_REMOVE_EDGE_SETTINGS = ITouchFeature.getInstance().shouldRemoveEdgeSettings();
    public String mEdgeModeType = KEY_SCREEN_EDGE_MODE_DEFAULT;
    private final EdgeSuppressionInfo[] mEdgeSuppressionInfoList = new EdgeSuppressionInfo[8];
    private final EdgeSuppressionInfo[] mSubScreenEdgeSuppressionList = new EdgeSuppressionInfo[2];
    private volatile int mHoldSensorState = -1;
    private boolean mIsUserSetted = true;
    private String mTrackEventTime = "1970-01-01 00:00:00";
    private SimpleDateFormat mSimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private ScheduledExecutorService mScheduledExecutorService = Executors.newScheduledThreadPool(1);
    final HoldSensorWrapper.HoldSensorChangeListener mHoldListener = new HoldSensorWrapper.HoldSensorChangeListener() { // from class: com.miui.server.input.edgesuppression.EdgeSuppressionManager$$ExternalSyntheticLambda0
        public final void onSensorChanged(int i) {
            EdgeSuppressionManager.this.m2197xe6415df(i);
        }
    };
    private volatile int mLaySensorState = -1;
    final LaySensorWrapper.LaySensorChangeListener mLayListener = new LaySensorWrapper.LaySensorChangeListener() { // from class: com.miui.server.input.edgesuppression.EdgeSuppressionManager$$ExternalSyntheticLambda1
        public final void onSensorChanged(int i) {
            EdgeSuppressionManager.this.m2198xa2a2857e(i);
        }
    };
    private boolean mSupportSensor = FeatureParser.getBoolean(SUPPORT_SENSOR, false);

    /* renamed from: lambda$new$0$com-miui-server-input-edgesuppression-EdgeSuppressionManager */
    public /* synthetic */ void m2197xe6415df(int status) {
        if (status != this.mHoldSensorState) {
            Slog.i(TAG, "Hold status is " + status);
            this.mHoldSensorState = status;
            notifyEdgeSuppressionChanged(1, REASON_OF_HOLDSENSOR);
        }
    }

    /* renamed from: lambda$new$1$com-miui-server-input-edgesuppression-EdgeSuppressionManager */
    public /* synthetic */ void m2198xa2a2857e(int status) {
        if (status != this.mLaySensorState) {
            Slog.i(TAG, "Lay status is " + status);
            this.mLaySensorState = status;
            notifyEdgeSuppressionChanged(1, REASON_OF_LAYSENSOR);
        }
    }

    private EdgeSuppressionManager(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService("window");
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getRealMetrics(metrics);
        this.mScreenWidth = Math.min(metrics.widthPixels - 1, metrics.heightPixels - 1);
        this.mScreenHeight = Math.max(metrics.widthPixels - 1, metrics.heightPixels - 1);
        this.mContext = context;
        EdgeSuppressionHandler edgeSuppressionHandler = new EdgeSuppressionHandler(MiuiInputThread.getThread().getLooper());
        this.mHandler = edgeSuppressionHandler;
        ConfigLoader configLoader = new ConfigLoader();
        this.mConfigLoader = configLoader;
        configLoader.initEdgeSuppressionConfig();
        initParamForOldSettings();
        BaseEdgeSuppression edgeSuppressionMode = EdgeSuppressionFactory.getEdgeSuppressionMode(EdgeSuppressionFactory.TYPE_NORMAL);
        this.mBaseEdgeSuppression = edgeSuppressionMode;
        edgeSuppressionMode.initInputMethodData(context, (int) this.mEdgeModeSize);
        MiuiEdgeSuppressionObserver mMiuiEdgeSuppressionObserver = new MiuiEdgeSuppressionObserver(edgeSuppressionHandler);
        mMiuiEdgeSuppressionObserver.registerObserver();
        if (IS_SUPPORT_EDGE_MODE) {
            this.mScheduledExecutorService.scheduleAtFixedRate(new Runnable() { // from class: com.miui.server.input.edgesuppression.EdgeSuppressionManager$$ExternalSyntheticLambda2
                @Override // java.lang.Runnable
                public final void run() {
                    EdgeSuppressionManager.this.m2199x36e0f51d();
                }
            }, 5L, TimeUnit.DAYS.toMinutes(1L), TimeUnit.MINUTES);
        }
    }

    public static EdgeSuppressionManager getInstance(Context context) {
        if (sInstance == null) {
            synchronized (EdgeSuppressionManager.class) {
                if (sInstance == null) {
                    sInstance = new EdgeSuppressionManager(context);
                }
            }
        }
        return sInstance;
    }

    public void handleEdgeSuppressionData() {
        int targetId;
        if (IS_SUPPORT_EDGE_MODE) {
            int rotation = this.mContext.getDisplay().getRotation();
            EdgeSuppressionInfo info = getEdgeSuppressionInfo(this.mEdgeModeSize, rotation);
            if (MiuiSettings.System.IS_FOLD_DEVICE && this.mFolded) {
                targetId = 1;
            } else {
                targetId = 0;
            }
            Slog.i(TAG, "handleEdgeSuppressionData:targetId = " + targetId + " " + info.toString());
            this.mBaseEdgeSuppression.updateInterNalParam(info, this.mHoldSensorState, rotation, targetId, this.mScreenWidth, this.mScreenHeight);
            this.mBaseEdgeSuppression.syncDataToKernel();
        }
    }

    private EdgeSuppressionInfo getEdgeSuppressionInfo(float edgeModeSize, int rotation) {
        if (this.mLaySensorState != -1) {
            switch (this.mLaySensorState) {
                case 0:
                    edgeModeSize = this.mConfigLoader.mConditionLevel[2];
                    break;
                case 1:
                    edgeModeSize = this.mConfigLoader.mConditionLevel[3];
                    break;
                case 2:
                    edgeModeSize = MiuiFreeformPinManagerService.EDGE_AREA;
                    break;
            }
        }
        this.mConditionSize = (int) edgeModeSize;
        this.mAbsoluteSize = getAbsoluteSize(edgeModeSize);
        EdgeSuppressionInfo info = this.mConfigLoader.getInfo(this.mEdgeSuppressionInfoList, this.mEdgeModeType, rotation);
        if (this.mSupportSensor || KEY_SCREEN_EDGE_MODE_CUSTOM.equals(this.mEdgeModeType)) {
            info.setAbsoluteSize(this.mAbsoluteSize);
            info.setConditionSize(this.mConditionSize);
        }
        if (MiuiSettings.System.IS_FOLD_DEVICE && this.mFolded) {
            int screenState = (rotation == 0 || rotation == 2) ? 0 : 1;
            EdgeSuppressionInfo edgeSuppressionInfo = this.mSubScreenEdgeSuppressionList[screenState];
            if (edgeSuppressionInfo == null) {
                edgeSuppressionInfo = info;
            }
            info = edgeSuppressionInfo;
        }
        Slog.i(TAG, "mScreenWidth = " + this.mScreenWidth + " mScreenHeight = " + this.mScreenHeight);
        return info;
    }

    private int getAbsoluteSize(float conditionSize) {
        int result = this.mConfigLoader.mAbsoluteLevel[0];
        if (Float.compare(conditionSize, this.mConfigLoader.mConditionLevel[0]) == 1 && Float.compare(conditionSize, this.mConfigLoader.mConditionLevel[1]) != 1) {
            return this.mConfigLoader.mAbsoluteLevel[1];
        }
        if (Float.compare(conditionSize, this.mConfigLoader.mConditionLevel[1]) == 1 && Float.compare(conditionSize, this.mConfigLoader.mConditionLevel[2]) != 1) {
            return this.mConfigLoader.mAbsoluteLevel[2];
        }
        if (Float.compare(conditionSize, this.mConfigLoader.mConditionLevel[2]) == 1 && Float.compare(conditionSize, this.mConfigLoader.mConditionLevel[3]) != 1) {
            return this.mConfigLoader.mAbsoluteLevel[3];
        }
        if (Float.compare(conditionSize, this.mConfigLoader.mConditionLevel[3]) == 1 && Float.compare(conditionSize, this.mConfigLoader.mConditionLevel[4]) != 1) {
            return this.mConfigLoader.mAbsoluteLevel[4];
        }
        return result;
    }

    public void handleEdgeModeChange(String reason) {
        notifyEdgeSuppressionChanged(1, reason);
    }

    public void handleEdgeModeChange(String reason, boolean folded, DisplayFrames displayFrames) {
        this.mFolded = folded;
        this.mScreenWidth = Math.min(displayFrames.mDisplayWidth - 1, displayFrames.mDisplayHeight - 1);
        this.mScreenHeight = Math.max(displayFrames.mDisplayWidth - 1, displayFrames.mDisplayHeight - 1);
        notifyEdgeSuppressionChanged(1, reason);
    }

    public void notifyEdgeSuppressionChanged(int action, String reason) {
        Message msg = this.mHandler.obtainMessage(action);
        Bundle bundle = new Bundle();
        bundle.putString(EdgeSuppressionHandler.MSG_DATA_REASON, reason);
        msg.setData(bundle);
        if (this.mHandler.hasMessages(action)) {
            this.mHandler.removeMessages(action);
        } else {
            this.mHandler.sendMessage(msg);
        }
    }

    private void initParamForOldSettings() {
        int[] intArray = FeatureParser.getIntArray("edge_suppresson_condition");
        if (intArray != null) {
            this.mMaxAdjustValue = intArray[0];
        }
    }

    public void registerSensors() {
        if (this.mHoldSensorWrapper == null) {
            this.mHoldSensorWrapper = new HoldSensorWrapper(this.mContext);
        }
        if (this.mLaySensorWrapper == null) {
            this.mLaySensorWrapper = new LaySensorWrapper(this.mContext);
        }
        this.mHoldSensorWrapper.registerListener(this.mHoldListener);
        this.mLaySensorWrapper.registerListener(this.mLayListener);
    }

    public void unRegisterSensors() {
        if (this.mHoldSensorWrapper != null) {
            this.mHoldSensorState = -1;
            this.mHoldSensorWrapper.unregisterAllListener();
        }
        if (this.mLaySensorWrapper != null) {
            this.mLaySensorState = -1;
            this.mLaySensorWrapper.unregisterAllListener();
        }
    }

    public void finishedWakingUp() {
        if (IS_SUPPORT_EDGE_MODE) {
            if (this.mSupportSensor && KEY_SCREEN_EDGE_MODE_DEFAULT.equals(this.mEdgeModeType)) {
                registerSensors();
            }
            notifyEdgeSuppressionChanged(1, REASON_OF_SCREENON);
        }
    }

    public void finishedGoingToSleep() {
        if (this.mSupportSensor && KEY_SCREEN_EDGE_MODE_DEFAULT.equals(this.mEdgeModeType)) {
            unRegisterSensors();
        }
    }

    public int[] getConditionLevel() {
        return this.mConfigLoader.mConditionLevel;
    }

    public int[] getAbsoluteLevel() {
        return this.mConfigLoader.mAbsoluteLevel;
    }

    public int getScreenWidth() {
        return this.mScreenWidth;
    }

    private int compareTimeOfNow(String dateOldString, Date dateNow) {
        if (TextUtils.isEmpty(dateOldString)) {
            dateOldString = "1970-01-01 00:00:00";
        }
        Date dateOld = new Date();
        try {
            dateOld = this.mSimpleDateFormat.parse(dateOldString);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return (int) ((dateNow.getTime() - dateOld.getTime()) / 86400000);
    }

    /* renamed from: trackEdgeSuppressionAndUpdateTrackTime */
    public void m2199x36e0f51d() {
        Date dateNow = new Date();
        if (compareTimeOfNow(this.mTrackEventTime, dateNow) >= 1) {
            InputOneTrackUtil.getInstance(this.mContext).trackEdgeSuppressionEvent(this.mIsUserSetted, this.mLastEdgeModeSize, this.mLastEdgeModeType, this.mConditionSize, this.mEdgeModeType);
            String updateTime = this.mSimpleDateFormat.format(dateNow);
            Settings.System.putStringForUser(this.mContext.getContentResolver(), TRACK_EVENT_TIME, updateTime, UserHandle.myUserId());
        }
    }

    public void resetUserSetted() {
        this.mLastEdgeModeType = TRACK_EDGE_TYPE_DEFAULT;
        this.mLastEdgeModeSize = 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class MiuiEdgeSuppressionObserver extends ContentObserver {
        ContentResolver contentResolver;
        boolean isTypeChanged = true;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public MiuiEdgeSuppressionObserver(Handler handler) {
            super(handler);
            EdgeSuppressionManager.this = this$0;
            this.contentResolver = this$0.mContext.getContentResolver();
        }

        void registerObserver() {
            if (EdgeSuppressionManager.IS_SUPPORT_EDGE_MODE) {
                this.contentResolver.registerContentObserver(Settings.System.getUriFor("edge_type"), false, this, -1);
                onChange(false, Settings.System.getUriFor("edge_type"));
                this.contentResolver.registerContentObserver(Settings.System.getUriFor("edge_size"), false, this, -1);
                onChange(false, Settings.System.getUriFor("edge_size"));
                this.contentResolver.registerContentObserver(Settings.System.getUriFor(EdgeSuppressionManager.TRACK_EVENT_TIME), false, this, -1);
                onChange(false, Settings.System.getUriFor(EdgeSuppressionManager.TRACK_EVENT_TIME));
                EdgeSuppressionManager.this.resetUserSetted();
            }
            this.contentResolver.registerContentObserver(Settings.Global.getUriFor(EdgeSuppressionManager.VERTICAL_EDGE_SUPPRESSION_SIZE), false, this, -1);
            this.contentResolver.registerContentObserver(Settings.Global.getUriFor(EdgeSuppressionManager.HORIZONTAL_EDGE_SUPPRESSION_SIZE), false, this, -1);
            onChange(false, Settings.Global.getUriFor(EdgeSuppressionManager.VERTICAL_EDGE_SUPPRESSION_SIZE));
            onChange(false, Settings.Global.getUriFor(EdgeSuppressionManager.HORIZONTAL_EDGE_SUPPRESSION_SIZE));
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            if (Settings.System.getUriFor("edge_size").equals(uri)) {
                EdgeSuppressionManager edgeSuppressionManager = EdgeSuppressionManager.this;
                edgeSuppressionManager.mLastEdgeModeSize = (int) edgeSuppressionManager.mEdgeModeSize;
                EdgeSuppressionManager edgeSuppressionManager2 = EdgeSuppressionManager.this;
                edgeSuppressionManager2.mEdgeModeSize = Settings.System.getFloatForUser(this.contentResolver, "edge_size", edgeSuppressionManager2.mEdgeModeSize, -2);
                if (EdgeSuppressionManager.this.mEdgeModeSize <= 1.0f) {
                    EdgeSuppressionManager.this.mEdgeModeSize *= EdgeSuppressionManager.this.mMaxAdjustValue;
                }
                if (!this.isTypeChanged) {
                    EdgeSuppressionManager.this.mLastEdgeModeType = EdgeSuppressionManager.KEY_SCREEN_EDGE_MODE_CUSTOM;
                } else {
                    this.isTypeChanged = false;
                }
                EdgeSuppressionManager.this.notifyEdgeSuppressionChanged(1, EdgeSuppressionManager.REASON_OF_SETTINGS);
            } else if (Settings.System.getUriFor("edge_type").equals(uri)) {
                this.isTypeChanged = true;
                EdgeSuppressionManager edgeSuppressionManager3 = EdgeSuppressionManager.this;
                edgeSuppressionManager3.mLastEdgeModeType = edgeSuppressionManager3.mEdgeModeType;
                EdgeSuppressionManager.this.mEdgeModeType = Settings.System.getStringForUser(this.contentResolver, "edge_type", -2);
                if (EdgeSuppressionManager.this.mEdgeModeType == null) {
                    EdgeSuppressionManager.this.mIsUserSetted = false;
                    EdgeSuppressionManager.this.mEdgeModeType = EdgeSuppressionManager.KEY_SCREEN_EDGE_MODE_DEFAULT;
                } else if (EdgeSuppressionManager.this.mEdgeModeType.equals(EdgeSuppressionManager.KEY_SCREEN_EDGE_MODE_DIY)) {
                    EdgeSuppressionManager.this.mEdgeModeType = EdgeSuppressionManager.KEY_SCREEN_EDGE_MODE_CUSTOM;
                }
                if (EdgeSuppressionManager.this.mSupportSensor && EdgeSuppressionManager.KEY_SCREEN_EDGE_MODE_DEFAULT.equals(EdgeSuppressionManager.this.mEdgeModeType)) {
                    EdgeSuppressionManager.this.mBaseEdgeSuppression = EdgeSuppressionFactory.getEdgeSuppressionMode(EdgeSuppressionFactory.TYPE_INTELLIGENT);
                    EdgeSuppressionManager.this.registerSensors();
                    Slog.i(EdgeSuppressionManager.TAG, "mBaseEdgeSuppression: intelligent");
                    return;
                }
                EdgeSuppressionManager.this.mBaseEdgeSuppression = EdgeSuppressionFactory.getEdgeSuppressionMode(EdgeSuppressionFactory.TYPE_NORMAL);
                Slog.i(EdgeSuppressionManager.TAG, "mBaseEdgeSuppression: normal");
                EdgeSuppressionManager.this.unRegisterSensors();
            } else if (Settings.Global.getUriFor(EdgeSuppressionManager.VERTICAL_EDGE_SUPPRESSION_SIZE).equals(uri)) {
                EdgeSuppressionManager.this.mBaseEdgeSuppression.setInputMethodSize(EdgeSuppressionManager.this.mContext);
            } else if (Settings.Global.getUriFor(EdgeSuppressionManager.HORIZONTAL_EDGE_SUPPRESSION_SIZE).equals(uri)) {
                EdgeSuppressionManager.this.mBaseEdgeSuppression.setInputMethodSize(EdgeSuppressionManager.this.mContext);
            } else if (Settings.System.getUriFor(EdgeSuppressionManager.TRACK_EVENT_TIME).equals(uri)) {
                EdgeSuppressionManager.this.mTrackEventTime = Settings.System.getStringForUser(this.contentResolver, EdgeSuppressionManager.TRACK_EVENT_TIME, UserHandle.myUserId());
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class EdgeSuppressionHandler extends Handler {
        public static final String MSG_DATA_REASON = "reason";
        public static final int MSG_SEND_DATA = 1;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public EdgeSuppressionHandler(Looper looper) {
            super(looper);
            EdgeSuppressionManager.this = r1;
        }

        /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            char c;
            String reason = msg.getData().getString(MSG_DATA_REASON, "unKnow");
            switch (reason.hashCode()) {
                case -1747621543:
                    if (reason.equals(EdgeSuppressionManager.REASON_OF_HOLDSENSOR)) {
                        c = 1;
                        break;
                    }
                    c = 65535;
                    break;
                case -411607189:
                    if (reason.equals(EdgeSuppressionManager.REASON_OF_SCREENON)) {
                        c = 3;
                        break;
                    }
                    c = 65535;
                    break;
                case -68605122:
                    if (reason.equals(EdgeSuppressionManager.REASON_OF_GAMEBOOSTER)) {
                        c = 5;
                        break;
                    }
                    c = 65535;
                    break;
                case -40300674:
                    if (reason.equals(EdgeSuppressionManager.REASON_OF_ROTATION)) {
                        c = 0;
                        break;
                    }
                    c = 65535;
                    break;
                case 1434631203:
                    if (reason.equals(EdgeSuppressionManager.REASON_OF_SETTINGS)) {
                        c = 4;
                        break;
                    }
                    c = 65535;
                    break;
                case 1932752118:
                    if (reason.equals(EdgeSuppressionManager.REASON_OF_CONFIGURATION)) {
                        c = 6;
                        break;
                    }
                    c = 65535;
                    break;
                case 2103973502:
                    if (reason.equals(EdgeSuppressionManager.REASON_OF_LAYSENSOR)) {
                        c = 2;
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
                    Slog.i(EdgeSuppressionManager.TAG, "Send EdgeSuppression data because rotation");
                    EdgeSuppressionManager.this.handleEdgeSuppressionData();
                    break;
                case 1:
                    Slog.i(EdgeSuppressionManager.TAG, "Send EdgeSuppression data because holdSensor,status = " + EdgeSuppressionManager.this.mHoldSensorState);
                    EdgeSuppressionManager.this.handleEdgeSuppressionData();
                    break;
                case 2:
                    Slog.i(EdgeSuppressionManager.TAG, "Send EdgeSuppression data because laySensor,status = " + EdgeSuppressionManager.this.mLaySensorState);
                    EdgeSuppressionManager.this.handleEdgeSuppressionData();
                    break;
                case 3:
                    Slog.i(EdgeSuppressionManager.TAG, "Send EdgeSuppression data because screen turn on");
                    EdgeSuppressionManager.this.handleEdgeSuppressionData();
                    break;
                case 4:
                    Slog.i(EdgeSuppressionManager.TAG, "Send EdgeSuppression data because User Settings");
                    EdgeSuppressionManager.this.handleEdgeSuppressionData();
                    break;
                case 5:
                    Slog.i(EdgeSuppressionManager.TAG, "Send EdgeSuppression data because Game Booster");
                    EdgeSuppressionManager.this.handleEdgeSuppressionData();
                    break;
                case 6:
                    Slog.i(EdgeSuppressionManager.TAG, "Send EdgeSuppression data because configuration");
                    EdgeSuppressionManager.this.handleEdgeSuppressionData();
                    break;
            }
            super.handleMessage(msg);
        }
    }

    /* loaded from: classes.dex */
    public class ConfigLoader {
        private static final String ATTRIBUTE_ABSOLUTE_SIZE = "absoluteSize";
        private static final String ATTRIBUTE_CONDITION_SIZE = "conditionSize";
        private static final String ATTRIBUTE_CONNER_HEIGHT = "connerHeight";
        private static final String ATTRIBUTE_CONNER_WIDTH = "connerWidth";
        private static final String ATTRIBUTE_DISPLAY_ID = "displayId";
        private static final String ATTRIBUTE_IS_HORIZONTAL = "isHorizontal";
        private static final String ATTRIBUTE_MAX_ABSOLUTE_SIZE = "maxAbsoluteSize";
        private static final String ATTRIBUTE_MAX_CONDITION_SIZE = "maxConditionSize";
        private static final String ATTRIBUTE_MIN_ABSOLUTE_SIZE = "minAbsoluteSize";
        private static final String ATTRIBUTE_MIN_CONDITION_SIZE = "minConditionSize";
        private static final String ATTRIBUTE_TYPE = "type";
        private static final String CONFIG_FILE = "edge_suppression_config";
        private static final String TAG_CONFIG_ITEM = "edgesuppressionitem";
        private static final String TAG_FILE = "edgesuppressions";
        int[] mConditionLevel = {0, 0, 0, 0, 0};
        int[] mAbsoluteLevel = {0, 0, 0, 0, 0};

        ConfigLoader() {
            EdgeSuppressionManager.this = this$0;
        }

        /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
        public void initEdgeSuppressionConfig() {
            XmlResourceParser xmlResourceParser;
            Exception e;
            int resId;
            boolean z;
            char c;
            int resId2 = EdgeSuppressionManager.this.mContext.getResources().getIdentifier(CONFIG_FILE, "xml", "android.miui");
            if (resId2 == 0 || (xmlResourceParser = EdgeSuppressionManager.this.mContext.getResources().getXml(resId2)) == null) {
                return;
            }
            Slog.i(EdgeSuppressionManager.TAG, "xmlResourceParser is not null, file = edge_suppression_config");
            try {
                XmlUtils.beginDocument(xmlResourceParser, TAG_FILE);
                int index = 0;
                XmlUtils.nextElement(xmlResourceParser);
                while (xmlResourceParser.getEventType() != 3 && TAG_CONFIG_ITEM.equals(xmlResourceParser.getName())) {
                    String absoluteSize = xmlResourceParser.getAttributeValue(null, ATTRIBUTE_ABSOLUTE_SIZE);
                    String conditionSize = xmlResourceParser.getAttributeValue(null, ATTRIBUTE_CONDITION_SIZE);
                    String connerWidth = xmlResourceParser.getAttributeValue(null, ATTRIBUTE_CONNER_WIDTH);
                    String connerHeight = xmlResourceParser.getAttributeValue(null, ATTRIBUTE_CONNER_HEIGHT);
                    String isHorizontal = xmlResourceParser.getAttributeValue(null, ATTRIBUTE_IS_HORIZONTAL);
                    String type = xmlResourceParser.getAttributeValue(null, "type");
                    String maxAbsoluteSize = xmlResourceParser.getAttributeValue(null, ATTRIBUTE_MAX_ABSOLUTE_SIZE);
                    String minAbsoluteSize = xmlResourceParser.getAttributeValue(null, ATTRIBUTE_MIN_ABSOLUTE_SIZE);
                    String maxConditionSize = xmlResourceParser.getAttributeValue(null, ATTRIBUTE_MAX_CONDITION_SIZE);
                    String minConditionSize = xmlResourceParser.getAttributeValue(null, ATTRIBUTE_MIN_CONDITION_SIZE);
                    String displayId = xmlResourceParser.getAttributeValue(null, ATTRIBUTE_DISPLAY_ID);
                    if (MiuiSettings.System.IS_FOLD_DEVICE) {
                        resId = resId2;
                        z = true;
                    } else {
                        switch (type.hashCode()) {
                            case -480291787:
                                if (type.equals(EdgeSuppressionManager.KEY_SCREEN_EDGE_MODE_DEFAULT)) {
                                    c = 0;
                                    break;
                                }
                                c = 65535;
                                break;
                            case 1018504075:
                                if (type.equals(EdgeSuppressionManager.KEY_SCREEN_EDGE_MODE_STRONG)) {
                                    c = 2;
                                    break;
                                }
                                c = 65535;
                                break;
                            case 1567494936:
                                if (type.equals(EdgeSuppressionManager.KEY_SCREEN_EDGE_MODE_WAKE)) {
                                    c = 1;
                                    break;
                                }
                                c = 65535;
                                break;
                            case 1611383141:
                                try {
                                    if (type.equals(EdgeSuppressionManager.KEY_SCREEN_EDGE_MODE_CUSTOM)) {
                                        c = 3;
                                        break;
                                    }
                                    c = 65535;
                                    break;
                                } catch (Exception e2) {
                                    e = e2;
                                    Slog.i(EdgeSuppressionManager.TAG, "init EdgeSuppression Config fail!" + e.toString());
                                    return;
                                }
                            default:
                                c = 65535;
                                break;
                        }
                        switch (c) {
                            case 0:
                                this.mAbsoluteLevel[2] = Integer.parseInt(absoluteSize);
                                this.mConditionLevel[2] = Integer.parseInt(conditionSize);
                                EdgeSuppressionManager.this.mEdgeModeSize = this.mConditionLevel[2];
                                break;
                            case 1:
                                this.mAbsoluteLevel[1] = Integer.parseInt(absoluteSize);
                                this.mConditionLevel[1] = Integer.parseInt(conditionSize);
                                break;
                            case 2:
                                this.mAbsoluteLevel[3] = Integer.parseInt(absoluteSize);
                                this.mConditionLevel[3] = Integer.parseInt(conditionSize);
                                break;
                            case 3:
                                this.mAbsoluteLevel[0] = Integer.parseInt(minAbsoluteSize);
                                this.mAbsoluteLevel[4] = Integer.parseInt(maxAbsoluteSize);
                                this.mConditionLevel[0] = Integer.parseInt(minConditionSize);
                                this.mConditionLevel[4] = Integer.parseInt(maxConditionSize);
                                break;
                        }
                        int index2 = index + 1;
                        resId = resId2;
                        z = true;
                        try {
                            EdgeSuppressionManager.this.mEdgeSuppressionInfoList[index] = new EdgeSuppressionInfo(EdgeSuppressionManager.KEY_SCREEN_EDGE_MODE_CUSTOM.equals(type) ? 0 : Integer.parseInt(absoluteSize), EdgeSuppressionManager.KEY_SCREEN_EDGE_MODE_CUSTOM.equals(type) ? 0 : Integer.parseInt(conditionSize), Integer.parseInt(connerWidth), Integer.parseInt(connerHeight), Integer.parseInt(isHorizontal) == 1, type);
                            index = index2;
                        } catch (Exception e3) {
                            e = e3;
                            Slog.i(EdgeSuppressionManager.TAG, "init EdgeSuppression Config fail!" + e.toString());
                            return;
                        }
                    }
                    if (MiuiSettings.System.IS_FOLD_DEVICE) {
                        if (TextUtils.isEmpty(displayId)) {
                            this.mAbsoluteLevel[2] = Integer.parseInt(absoluteSize);
                            this.mConditionLevel[2] = Integer.parseInt(conditionSize);
                            EdgeSuppressionManager.this.mEdgeModeSize = this.mConditionLevel[2];
                            int index3 = index + 1;
                            EdgeSuppressionManager.this.mEdgeSuppressionInfoList[index] = new EdgeSuppressionInfo(Integer.parseInt(absoluteSize), Integer.parseInt(conditionSize), Integer.parseInt(connerWidth), Integer.parseInt(connerHeight), Integer.parseInt(isHorizontal) == z ? z : false, type);
                            index = index3;
                        } else {
                            EdgeSuppressionManager.this.mSubScreenEdgeSuppressionList[Integer.parseInt(isHorizontal)] = new EdgeSuppressionInfo(Integer.parseInt(absoluteSize), Integer.parseInt(conditionSize), Integer.parseInt(connerWidth), Integer.parseInt(connerHeight), Integer.parseInt(isHorizontal) == z ? z : false, type);
                        }
                    }
                    XmlUtils.nextElement(xmlResourceParser);
                    resId2 = resId;
                }
            } catch (Exception e4) {
                e = e4;
            }
        }

        public EdgeSuppressionInfo getInfo(EdgeSuppressionInfo[] list, String type, int rotation) {
            if (type != null && list.length > 0) {
                boolean isHorizontal = false;
                if (rotation == 1 || rotation == 3) {
                    isHorizontal = true;
                }
                for (EdgeSuppressionInfo target : list) {
                    if (type.equals(target.getType()) && isHorizontal == target.isHorizontal()) {
                        return target;
                    }
                }
            }
            Slog.i(EdgeSuppressionManager.TAG, "There is not available info");
            return new EdgeSuppressionInfo(0, 0, 0, 0, false, EdgeSuppressionManager.KEY_SCREEN_EDGE_MODE_DEFAULT);
        }
    }
}
