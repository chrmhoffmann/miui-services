package com.miui.server;

import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.AlertDialog;
import android.app.AppGlobals;
import android.app.IMiuiProcessObserver;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.database.ContentObserver;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.UserHandle;
import android.provider.MiuiSettings;
import android.server.am.SplitScreenReporter;
import android.text.TextUtils;
import android.util.AtomicFile;
import android.util.Log;
import android.util.Slog;
import android.view.Display;
import com.android.internal.util.FastXmlSerializer;
import com.android.server.MiuiFgThread;
import com.android.server.am.ProcessUtils;
import com.android.server.display.TemperatureController;
import com.android.server.wm.MiuiFreeFormGestureDetector;
import com.android.server.wm.MiuiFreeformPinManagerService;
import com.android.server.wm.MiuiSizeCompatService;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import miui.util.CustomizeUtil;
import org.json.JSONObject;
/* loaded from: classes.dex */
public final class MiuiCompatModePackages {
    private static final String ATTR_CONFIG_NOTIFY_SUGGEST_APPS = "notifySuggestApps";
    private static final String MODULE_CUTOUT_MODE = "cutout_mode";
    private static final int MSG_DONT_SHOW_AGAIN = 105;
    private static final int MSG_ON_APP_LAUNCH = 104;
    private static final int MSG_READ = 101;
    private static final int MSG_REGISTER_OBSERVER = 102;
    private static final int MSG_UNREGISTER_OBSERVER = 103;
    private static final int MSG_UPDATE_CLOUD_DATA = 108;
    private static final int MSG_WRITE = 100;
    private static final int MSG_WRITE_CUTOUT_MODE = 107;
    private static final int MSG_WRITE_SPECIAL_MODE = 106;
    private static final String TAG = "MiuiCompatModePackages";
    private static final String TAG_NAME_CONFIG = "config";
    private static final Uri URI_CLOUD_ALL_DATA_NOTIFY = Uri.parse("content://com.android.settings.cloud.CloudSettings/cloud_all_data/notify");
    private AlertDialog mAlertDialog;
    private final ContentObserver mCloudDataObserver;
    private final Context mContext;
    private final AtomicFile mCutoutModeFile;
    private float mDefaultAspect;
    private final AtomicFile mFile;
    private final CompatHandler mHandler;
    private final Handler mMainHandler;
    private IMiuiProcessObserver mProcessObserver;
    private final HashSet<String> mRestrictList;
    private final AtomicFile mSpecialModeFile;
    private final HashSet<String> mSupportNotchList;
    private final Object mLock = new Object();
    private final HashMap<String, Integer> mPackages = new HashMap<>();
    private final HashMap<String, Integer> mDefaultType = new HashMap<>();
    private final HashMap<String, Integer> mNotchConfig = new HashMap<>();
    private final HashMap<String, Integer> mNotchSpecialModePackages = new HashMap<>();
    private final HashMap<String, Integer> mUserCutoutModePackages = new HashMap<>();
    private final HashMap<String, Integer> mCloudCutoutModePackages = new HashMap<>();
    private final HashSet<String> mSuggestList = new HashSet<>();
    private boolean mNotifySuggestApps = true;
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() { // from class: com.miui.server.MiuiCompatModePackages.3
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String pkg;
            String action = intent.getAction();
            Uri data = intent.getData();
            if (data != null && (pkg = data.getSchemeSpecificPart()) != null) {
                if ("android.intent.action.PACKAGE_ADDED".equals(action)) {
                    MiuiCompatModePackages.this.handleUpdatePackage(pkg);
                } else if ("android.intent.action.PACKAGE_REMOVED".equals(action) && !intent.getBooleanExtra("android.intent.extra.REPLACING", false)) {
                    MiuiCompatModePackages.this.handleRemovePackage(pkg);
                }
            }
        }
    };

    /* loaded from: classes.dex */
    public final class CompatHandler extends Handler {
        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public CompatHandler(Looper looper) {
            super(looper, null, true);
            MiuiCompatModePackages.this = r2;
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 100:
                    MiuiCompatModePackages.this.saveCompatModes();
                    return;
                case 101:
                    MiuiCompatModePackages.this.readCutoutModeConfig();
                    MiuiCompatModePackages.this.readSpecialModeConfig();
                    MiuiCompatModePackages.this.readPackagesConfig();
                    MiuiCompatModePackages.this.readSuggestApps();
                    return;
                case 102:
                    MiuiCompatModePackages.this.handleRegisterObservers();
                    return;
                case 103:
                    MiuiCompatModePackages.this.handleUnregisterObservers();
                    return;
                case 104:
                    String str = (String) msg.obj;
                    return;
                case 105:
                    if (msg.obj != null && ((Boolean) msg.obj).booleanValue()) {
                        MiuiCompatModePackages.this.handleDontShowAgain();
                        return;
                    }
                    return;
                case 106:
                    MiuiCompatModePackages.this.saveSpecialModeFile();
                    return;
                case 107:
                    MiuiCompatModePackages.this.saveCutoutModeFile();
                    return;
                case MiuiCompatModePackages.MSG_UPDATE_CLOUD_DATA /* 108 */:
                    MiuiCompatModePackages.this.updateCloudData();
                    return;
                default:
                    return;
            }
        }
    }

    public MiuiCompatModePackages(Context context) {
        HashSet<String> hashSet = new HashSet<>();
        this.mRestrictList = hashSet;
        HashSet<String> hashSet2 = new HashSet<>();
        this.mSupportNotchList = hashSet2;
        this.mContext = context;
        hashSet.add("android.dpi.cts");
        if (Build.VERSION.SDK_INT < 28) {
            hashSet2.add("android");
            hashSet2.add("com.android.systemui");
            hashSet2.add("android.view.cts");
            hashSet2.add("com.google.android.projection.gearhead");
            hashSet2.add("com.google.android.apps.books");
            hashSet2.add("com.subcast.radio.android.prod");
            hashSet2.add("com.waze");
            hashSet2.add("tunein.player");
            hashSet2.add("com.google.android.apps.maps");
            hashSet2.add("com.google.android.music");
            hashSet2.add("com.stitcher.app");
            hashSet2.add("org.npr.one");
            hashSet2.add("com.gaana");
            hashSet2.add("com.quanticapps.quranandroid");
            hashSet2.add("com.itunestoppodcastplayer.app");
            if ("sirius".equals(Build.DEVICE) || "dipper".equals(Build.DEVICE) || "sakura".equals(Build.DEVICE)) {
                hashSet2.add("com.tencent.tmgp.pubgmhdce");
            }
        }
        File systemDir = new File(Environment.getDataDirectory(), "system");
        this.mFile = new AtomicFile(new File(systemDir, "miui-packages-compat.xml"));
        this.mSpecialModeFile = new AtomicFile(new File(systemDir, "miui-specail-mode-v2.xml"));
        this.mCutoutModeFile = new AtomicFile(new File(systemDir, "cutout-mode.xml"));
        CompatHandler compatHandler = new CompatHandler(MiuiFgThread.getHandler().getLooper());
        this.mHandler = compatHandler;
        compatHandler.sendEmptyMessage(101);
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.PACKAGE_ADDED");
        filter.addAction("android.intent.action.PACKAGE_REMOVED");
        filter.addDataScheme("package");
        context.registerReceiverAsUser(this.mBroadcastReceiver, UserHandle.ALL, filter, null, MiuiFgThread.getHandler());
        getDeviceAspect();
        this.mCloudDataObserver = new ContentObserver(compatHandler) { // from class: com.miui.server.MiuiCompatModePackages.1
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                MiuiCompatModePackages.this.updateCloudDataAsync();
            }
        };
        Handler handler = new Handler(Looper.getMainLooper());
        this.mMainHandler = handler;
        handler.post(new Runnable() { // from class: com.miui.server.MiuiCompatModePackages.2
            @Override // java.lang.Runnable
            public void run() {
                MiuiCompatModePackages.this.mContext.getContentResolver().registerContentObserver(MiuiCompatModePackages.URI_CLOUD_ALL_DATA_NOTIFY, false, MiuiCompatModePackages.this.mCloudDataObserver, -1);
                MiuiCompatModePackages.this.mCloudDataObserver.onChange(false);
            }
        });
    }

    public void updateCloudDataAsync() {
        this.mHandler.removeMessages(MSG_UPDATE_CLOUD_DATA);
        this.mHandler.sendEmptyMessage(MSG_UPDATE_CLOUD_DATA);
    }

    public void updateCloudData() {
        Log.d(TAG, "updateCloudData");
        synchronized (this.mLock) {
            this.mCloudCutoutModePackages.clear();
        }
        List<MiuiSettings.SettingsCloudData.CloudData> dataList = MiuiSettings.SettingsCloudData.getCloudDataList(this.mContext.getContentResolver(), MODULE_CUTOUT_MODE);
        if (dataList == null || dataList.size() == 0) {
            return;
        }
        try {
            HashMap<String, Integer> pkgs = new HashMap<>();
            for (MiuiSettings.SettingsCloudData.CloudData data : dataList) {
                String json = data.toString();
                if (!TextUtils.isEmpty(json)) {
                    JSONObject jsonObject = new JSONObject(json);
                    String pkg = jsonObject.optString(SplitScreenReporter.STR_PKG);
                    int mode = jsonObject.optInt("mode");
                    if (!TextUtils.isEmpty(pkg)) {
                        pkgs.put(pkg, Integer.valueOf(mode));
                    }
                }
            }
            synchronized (this.mLock) {
                this.mCloudCutoutModePackages.putAll(pkgs);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:53:0x0084 A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public void readSpecialModeConfig() {
        /*
            r11 = this;
            r0 = 0
            android.util.AtomicFile r1 = r11.mSpecialModeFile     // Catch: java.lang.Throwable -> L95 java.lang.Exception -> L97
            java.io.FileInputStream r1 = r1.openRead()     // Catch: java.lang.Throwable -> L95 java.lang.Exception -> L97
            r0 = r1
            org.xmlpull.v1.XmlPullParser r1 = android.util.Xml.newPullParser()     // Catch: java.lang.Throwable -> L95 java.lang.Exception -> L97
            java.nio.charset.Charset r2 = java.nio.charset.StandardCharsets.UTF_8     // Catch: java.lang.Throwable -> L95 java.lang.Exception -> L97
            java.lang.String r2 = r2.name()     // Catch: java.lang.Throwable -> L95 java.lang.Exception -> L97
            r1.setInput(r0, r2)     // Catch: java.lang.Throwable -> L95 java.lang.Exception -> L97
            int r2 = r1.getEventType()     // Catch: java.lang.Throwable -> L95 java.lang.Exception -> L97
        L19:
            r3 = 1
            r4 = 2
            if (r2 == r4) goto L25
            if (r2 == r3) goto L25
            int r3 = r1.next()     // Catch: java.lang.Throwable -> L95 java.lang.Exception -> L97
            r2 = r3
            goto L19
        L25:
            if (r2 != r3) goto L2f
            if (r0 == 0) goto L2e
            r0.close()     // Catch: java.io.IOException -> L2d
            goto L2e
        L2d:
            r3 = move-exception
        L2e:
            return
        L2f:
            java.util.HashMap r5 = new java.util.HashMap     // Catch: java.lang.Throwable -> L95 java.lang.Exception -> L97
            r5.<init>()     // Catch: java.lang.Throwable -> L95 java.lang.Exception -> L97
            java.lang.String r6 = r1.getName()     // Catch: java.lang.Throwable -> L95 java.lang.Exception -> L97
            java.lang.String r7 = "special-mode"
            boolean r7 = r7.equals(r6)     // Catch: java.lang.Throwable -> L95 java.lang.Exception -> L97
            if (r7 == 0) goto L81
            int r7 = r1.next()     // Catch: java.lang.Throwable -> L95 java.lang.Exception -> L97
            r2 = r7
        L45:
            if (r2 != r4) goto L7a
            java.lang.String r7 = r1.getName()     // Catch: java.lang.Throwable -> L95 java.lang.Exception -> L97
            r6 = r7
            int r7 = r1.getDepth()     // Catch: java.lang.Throwable -> L95 java.lang.Exception -> L97
            if (r7 != r4) goto L7a
            java.lang.String r7 = "pkg"
            boolean r7 = r7.equals(r6)     // Catch: java.lang.Throwable -> L95 java.lang.Exception -> L97
            if (r7 == 0) goto L7a
            java.lang.String r7 = "name"
            r8 = 0
            java.lang.String r7 = r1.getAttributeValue(r8, r7)     // Catch: java.lang.Throwable -> L95 java.lang.Exception -> L97
            if (r7 == 0) goto L7a
            java.lang.String r9 = "mode"
            java.lang.String r8 = r1.getAttributeValue(r8, r9)     // Catch: java.lang.Throwable -> L95 java.lang.Exception -> L97
            r9 = 0
            if (r8 == 0) goto L73
            int r10 = java.lang.Integer.parseInt(r8)     // Catch: java.lang.NumberFormatException -> L72 java.lang.Throwable -> L95 java.lang.Exception -> L97
            r9 = r10
            goto L73
        L72:
            r10 = move-exception
        L73:
            java.lang.Integer r10 = java.lang.Integer.valueOf(r9)     // Catch: java.lang.Throwable -> L95 java.lang.Exception -> L97
            r5.put(r7, r10)     // Catch: java.lang.Throwable -> L95 java.lang.Exception -> L97
        L7a:
            int r7 = r1.next()     // Catch: java.lang.Throwable -> L95 java.lang.Exception -> L97
            r2 = r7
            if (r2 != r3) goto L45
        L81:
            java.lang.Object r3 = r11.mLock     // Catch: java.lang.Throwable -> L95 java.lang.Exception -> L97
            monitor-enter(r3)     // Catch: java.lang.Throwable -> L95 java.lang.Exception -> L97
            java.util.HashMap<java.lang.String, java.lang.Integer> r4 = r11.mNotchSpecialModePackages     // Catch: java.lang.Throwable -> L92
            r4.putAll(r5)     // Catch: java.lang.Throwable -> L92
            monitor-exit(r3)     // Catch: java.lang.Throwable -> L92
            if (r0 == 0) goto La6
            r0.close()     // Catch: java.io.IOException -> L90
        L8f:
            goto La6
        L90:
            r1 = move-exception
            goto L8f
        L92:
            r4 = move-exception
            monitor-exit(r3)     // Catch: java.lang.Throwable -> L92
            throw r4     // Catch: java.lang.Throwable -> L95 java.lang.Exception -> L97
        L95:
            r1 = move-exception
            goto La7
        L97:
            r1 = move-exception
            java.lang.String r2 = "MiuiCompatModePackages"
            java.lang.String r3 = "Error reading compat-packages"
            android.util.Slog.w(r2, r3, r1)     // Catch: java.lang.Throwable -> L95
            if (r0 == 0) goto La6
            r0.close()     // Catch: java.io.IOException -> L90
            goto L8f
        La6:
            return
        La7:
            if (r0 == 0) goto Lae
            r0.close()     // Catch: java.io.IOException -> Lad
            goto Lae
        Lad:
            r2 = move-exception
        Lae:
            throw r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.miui.server.MiuiCompatModePackages.readSpecialModeConfig():void");
    }

    /* JADX WARN: Removed duplicated region for block: B:51:0x0084 A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public void readCutoutModeConfig() {
        /*
            r11 = this;
            r0 = 0
            android.util.AtomicFile r1 = r11.mCutoutModeFile     // Catch: java.lang.Throwable -> L95 java.lang.Exception -> L97
            java.io.FileInputStream r1 = r1.openRead()     // Catch: java.lang.Throwable -> L95 java.lang.Exception -> L97
            r0 = r1
            org.xmlpull.v1.XmlPullParser r1 = android.util.Xml.newPullParser()     // Catch: java.lang.Throwable -> L95 java.lang.Exception -> L97
            java.nio.charset.Charset r2 = java.nio.charset.StandardCharsets.UTF_8     // Catch: java.lang.Throwable -> L95 java.lang.Exception -> L97
            java.lang.String r2 = r2.name()     // Catch: java.lang.Throwable -> L95 java.lang.Exception -> L97
            r1.setInput(r0, r2)     // Catch: java.lang.Throwable -> L95 java.lang.Exception -> L97
            int r2 = r1.getEventType()     // Catch: java.lang.Throwable -> L95 java.lang.Exception -> L97
        L19:
            r3 = 1
            r4 = 2
            if (r2 == r4) goto L25
            if (r2 == r3) goto L25
            int r3 = r1.next()     // Catch: java.lang.Throwable -> L95 java.lang.Exception -> L97
            r2 = r3
            goto L19
        L25:
            if (r2 != r3) goto L2f
            if (r0 == 0) goto L2e
            r0.close()     // Catch: java.io.IOException -> L2d
            goto L2e
        L2d:
            r3 = move-exception
        L2e:
            return
        L2f:
            java.util.HashMap r5 = new java.util.HashMap     // Catch: java.lang.Throwable -> L95 java.lang.Exception -> L97
            r5.<init>()     // Catch: java.lang.Throwable -> L95 java.lang.Exception -> L97
            java.lang.String r6 = r1.getName()     // Catch: java.lang.Throwable -> L95 java.lang.Exception -> L97
            java.lang.String r7 = "cutout-mode"
            boolean r7 = r7.equals(r6)     // Catch: java.lang.Throwable -> L95 java.lang.Exception -> L97
            if (r7 == 0) goto L81
            int r7 = r1.next()     // Catch: java.lang.Throwable -> L95 java.lang.Exception -> L97
            r2 = r7
        L45:
            if (r2 != r4) goto L7a
            java.lang.String r7 = r1.getName()     // Catch: java.lang.Throwable -> L95 java.lang.Exception -> L97
            r6 = r7
            int r7 = r1.getDepth()     // Catch: java.lang.Throwable -> L95 java.lang.Exception -> L97
            if (r7 != r4) goto L7a
            java.lang.String r7 = "pkg"
            boolean r7 = r7.equals(r6)     // Catch: java.lang.Throwable -> L95 java.lang.Exception -> L97
            if (r7 == 0) goto L7a
            java.lang.String r7 = "name"
            r8 = 0
            java.lang.String r7 = r1.getAttributeValue(r8, r7)     // Catch: java.lang.Throwable -> L95 java.lang.Exception -> L97
            if (r7 == 0) goto L7a
            java.lang.String r9 = "mode"
            java.lang.String r8 = r1.getAttributeValue(r8, r9)     // Catch: java.lang.Throwable -> L95 java.lang.Exception -> L97
            r9 = 0
            if (r8 == 0) goto L7a
            int r10 = java.lang.Integer.parseInt(r8)     // Catch: java.lang.NumberFormatException -> L79 java.lang.Throwable -> L95 java.lang.Exception -> L97
            r9 = r10
            java.lang.Integer r10 = java.lang.Integer.valueOf(r9)     // Catch: java.lang.NumberFormatException -> L79 java.lang.Throwable -> L95 java.lang.Exception -> L97
            r5.put(r7, r10)     // Catch: java.lang.NumberFormatException -> L79 java.lang.Throwable -> L95 java.lang.Exception -> L97
            goto L7a
        L79:
            r10 = move-exception
        L7a:
            int r7 = r1.next()     // Catch: java.lang.Throwable -> L95 java.lang.Exception -> L97
            r2 = r7
            if (r2 != r3) goto L45
        L81:
            java.lang.Object r3 = r11.mLock     // Catch: java.lang.Throwable -> L95 java.lang.Exception -> L97
            monitor-enter(r3)     // Catch: java.lang.Throwable -> L95 java.lang.Exception -> L97
            java.util.HashMap<java.lang.String, java.lang.Integer> r4 = r11.mUserCutoutModePackages     // Catch: java.lang.Throwable -> L92
            r4.putAll(r5)     // Catch: java.lang.Throwable -> L92
            monitor-exit(r3)     // Catch: java.lang.Throwable -> L92
            if (r0 == 0) goto La6
            r0.close()     // Catch: java.io.IOException -> L90
        L8f:
            goto La6
        L90:
            r1 = move-exception
            goto L8f
        L92:
            r4 = move-exception
            monitor-exit(r3)     // Catch: java.lang.Throwable -> L92
            throw r4     // Catch: java.lang.Throwable -> L95 java.lang.Exception -> L97
        L95:
            r1 = move-exception
            goto La7
        L97:
            r1 = move-exception
            java.lang.String r2 = "MiuiCompatModePackages"
            java.lang.String r3 = "Error reading compat-packages"
            android.util.Slog.w(r2, r3, r1)     // Catch: java.lang.Throwable -> L95
            if (r0 == 0) goto La6
            r0.close()     // Catch: java.io.IOException -> L90
            goto L8f
        La6:
            return
        La7:
            if (r0 == 0) goto Lae
            r0.close()     // Catch: java.io.IOException -> Lad
            goto Lae
        Lad:
            r2 = move-exception
        Lae:
            throw r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.miui.server.MiuiCompatModePackages.readCutoutModeConfig():void");
    }

    /* JADX WARN: Removed duplicated region for block: B:61:0x009e A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public void readPackagesConfig() {
        /*
            r11 = this;
            r0 = 0
            android.util.AtomicFile r1 = r11.mFile     // Catch: java.lang.Throwable -> Laf java.lang.Exception -> Lb1
            java.io.FileInputStream r1 = r1.openRead()     // Catch: java.lang.Throwable -> Laf java.lang.Exception -> Lb1
            r0 = r1
            org.xmlpull.v1.XmlPullParser r1 = android.util.Xml.newPullParser()     // Catch: java.lang.Throwable -> Laf java.lang.Exception -> Lb1
            java.nio.charset.Charset r2 = java.nio.charset.StandardCharsets.UTF_8     // Catch: java.lang.Throwable -> Laf java.lang.Exception -> Lb1
            java.lang.String r2 = r2.name()     // Catch: java.lang.Throwable -> Laf java.lang.Exception -> Lb1
            r1.setInput(r0, r2)     // Catch: java.lang.Throwable -> Laf java.lang.Exception -> Lb1
            int r2 = r1.getEventType()     // Catch: java.lang.Throwable -> Laf java.lang.Exception -> Lb1
        L19:
            r3 = 1
            r4 = 2
            if (r2 == r4) goto L25
            if (r2 == r3) goto L25
            int r3 = r1.next()     // Catch: java.lang.Throwable -> Laf java.lang.Exception -> Lb1
            r2 = r3
            goto L19
        L25:
            if (r2 != r3) goto L2f
            if (r0 == 0) goto L2e
            r0.close()     // Catch: java.io.IOException -> L2d
            goto L2e
        L2d:
            r3 = move-exception
        L2e:
            return
        L2f:
            java.util.HashMap r5 = new java.util.HashMap     // Catch: java.lang.Throwable -> Laf java.lang.Exception -> Lb1
            r5.<init>()     // Catch: java.lang.Throwable -> Laf java.lang.Exception -> Lb1
            java.lang.String r6 = r1.getName()     // Catch: java.lang.Throwable -> Laf java.lang.Exception -> Lb1
            java.lang.String r7 = "compat-packages"
            boolean r7 = r7.equals(r6)     // Catch: java.lang.Throwable -> Laf java.lang.Exception -> Lb1
            if (r7 == 0) goto L9b
            int r7 = r1.next()     // Catch: java.lang.Throwable -> Laf java.lang.Exception -> Lb1
            r2 = r7
        L45:
            if (r2 != r4) goto L94
            java.lang.String r7 = r1.getName()     // Catch: java.lang.Throwable -> Laf java.lang.Exception -> Lb1
            r6 = r7
            int r7 = r1.getDepth()     // Catch: java.lang.Throwable -> Laf java.lang.Exception -> Lb1
            if (r7 != r4) goto L94
            java.lang.String r7 = "pkg"
            boolean r7 = r7.equals(r6)     // Catch: java.lang.Throwable -> Laf java.lang.Exception -> Lb1
            r8 = 0
            if (r7 == 0) goto L7b
            java.lang.String r7 = "name"
            java.lang.String r7 = r1.getAttributeValue(r8, r7)     // Catch: java.lang.Throwable -> Laf java.lang.Exception -> Lb1
            if (r7 == 0) goto L94
            java.lang.String r9 = "mode"
            java.lang.String r8 = r1.getAttributeValue(r8, r9)     // Catch: java.lang.Throwable -> Laf java.lang.Exception -> Lb1
            r9 = 0
            if (r8 == 0) goto L73
            int r10 = java.lang.Integer.parseInt(r8)     // Catch: java.lang.NumberFormatException -> L72 java.lang.Throwable -> Laf java.lang.Exception -> Lb1
            r9 = r10
            goto L73
        L72:
            r10 = move-exception
        L73:
            java.lang.Integer r10 = java.lang.Integer.valueOf(r9)     // Catch: java.lang.Throwable -> Laf java.lang.Exception -> Lb1
            r5.put(r7, r10)     // Catch: java.lang.Throwable -> Laf java.lang.Exception -> Lb1
            goto L94
        L7b:
            java.lang.String r7 = "config"
            boolean r7 = r7.equals(r6)     // Catch: java.lang.Throwable -> Laf java.lang.Exception -> Lb1
            if (r7 == 0) goto L94
            java.lang.String r7 = "notifySuggestApps"
            java.lang.String r7 = r1.getAttributeValue(r8, r7)     // Catch: java.lang.Throwable -> Laf java.lang.Exception -> Lb1
            java.lang.Boolean r8 = java.lang.Boolean.valueOf(r7)     // Catch: java.lang.Throwable -> Laf java.lang.Exception -> Lb1
            boolean r8 = r8.booleanValue()     // Catch: java.lang.Throwable -> Laf java.lang.Exception -> Lb1
            r11.mNotifySuggestApps = r8     // Catch: java.lang.Throwable -> Laf java.lang.Exception -> Lb1
        L94:
            int r7 = r1.next()     // Catch: java.lang.Throwable -> Laf java.lang.Exception -> Lb1
            r2 = r7
            if (r2 != r3) goto L45
        L9b:
            java.lang.Object r3 = r11.mLock     // Catch: java.lang.Throwable -> Laf java.lang.Exception -> Lb1
            monitor-enter(r3)     // Catch: java.lang.Throwable -> Laf java.lang.Exception -> Lb1
            java.util.HashMap<java.lang.String, java.lang.Integer> r4 = r11.mPackages     // Catch: java.lang.Throwable -> Lac
            r4.putAll(r5)     // Catch: java.lang.Throwable -> Lac
            monitor-exit(r3)     // Catch: java.lang.Throwable -> Lac
            if (r0 == 0) goto Lc0
            r0.close()     // Catch: java.io.IOException -> Laa
        La9:
            goto Lc0
        Laa:
            r1 = move-exception
            goto La9
        Lac:
            r4 = move-exception
            monitor-exit(r3)     // Catch: java.lang.Throwable -> Lac
            throw r4     // Catch: java.lang.Throwable -> Laf java.lang.Exception -> Lb1
        Laf:
            r1 = move-exception
            goto Lc1
        Lb1:
            r1 = move-exception
            java.lang.String r2 = "MiuiCompatModePackages"
            java.lang.String r3 = "Error reading compat-packages"
            android.util.Slog.w(r2, r3, r1)     // Catch: java.lang.Throwable -> Laf
            if (r0 == 0) goto Lc0
            r0.close()     // Catch: java.io.IOException -> Laa
            goto La9
        Lc0:
            return
        Lc1:
            if (r0 == 0) goto Lc8
            r0.close()     // Catch: java.io.IOException -> Lc7
            goto Lc8
        Lc7:
            r2 = move-exception
        Lc8:
            throw r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.miui.server.MiuiCompatModePackages.readPackagesConfig():void");
    }

    public void readSuggestApps() {
        String[] arr = this.mContext.getResources().getStringArray(285409370);
        Collections.addAll(this.mSuggestList, arr);
    }

    private float getPackageMode(String packageName) {
        Integer mode;
        synchronized (this.mLock) {
            mode = this.mPackages.get(packageName);
        }
        return mode != null ? mode.intValue() : getDefaultMode(packageName);
    }

    private int getSpecialMode(String packageName) {
        Integer mode;
        synchronized (this.mLock) {
            mode = this.mNotchSpecialModePackages.get(packageName);
        }
        if (mode != null) {
            return mode.intValue();
        }
        return 0;
    }

    private void scheduleWrite() {
        this.mHandler.removeMessages(100);
        Message msg = this.mHandler.obtainMessage(100);
        this.mHandler.sendMessageDelayed(msg, 10000L);
    }

    private void scheduleWriteSpecialMode() {
        this.mHandler.removeMessages(106);
        Message msg = this.mHandler.obtainMessage(106);
        this.mHandler.sendMessageDelayed(msg, 10000L);
    }

    private void scheduleWriteCutoutMode() {
        this.mHandler.removeMessages(107);
        Message msg = this.mHandler.obtainMessage(107);
        this.mHandler.sendMessageDelayed(msg, 10000L);
    }

    void saveCompatModes() {
        HashMap<String, Integer> pkgs = new HashMap<>();
        synchronized (this.mLock) {
            pkgs.putAll(this.mPackages);
        }
        FileOutputStream fos = null;
        try {
            try {
                try {
                    fos = this.mFile.startWrite();
                    FastXmlSerializer fastXmlSerializer = new FastXmlSerializer();
                    fastXmlSerializer.setOutput(fos, StandardCharsets.UTF_8.name());
                    fastXmlSerializer.startDocument(null, true);
                    fastXmlSerializer.setFeature(MiuiSizeCompatService.FAST_XML, true);
                    fastXmlSerializer.startTag(null, "compat-packages");
                    fastXmlSerializer.startTag(null, TAG_NAME_CONFIG);
                    fastXmlSerializer.attribute(null, ATTR_CONFIG_NOTIFY_SUGGEST_APPS, String.valueOf(this.mNotifySuggestApps));
                    fastXmlSerializer.endTag(null, TAG_NAME_CONFIG);
                    for (Map.Entry<String, Integer> entry : pkgs.entrySet()) {
                        String pkg = entry.getKey();
                        int mode = entry.getValue().intValue();
                        boolean restrict = mode > 0;
                        if (restrict != isDefaultRestrict(pkg) && getDefaultAspectType(pkg) != 1) {
                            fastXmlSerializer.startTag(null, SplitScreenReporter.STR_PKG);
                            fastXmlSerializer.attribute(null, TemperatureController.STRATEGY_NAME, pkg);
                            fastXmlSerializer.attribute(null, "mode", Integer.toString(mode));
                            fastXmlSerializer.endTag(null, SplitScreenReporter.STR_PKG);
                        }
                    }
                    fastXmlSerializer.endTag(null, "compat-packages");
                    fastXmlSerializer.endDocument();
                    this.mFile.finishWrite(fos);
                } catch (Throwable th) {
                    if (fos != null) {
                        try {
                            fos.close();
                        } catch (IOException e) {
                        }
                    }
                    throw th;
                }
            } catch (Exception e1) {
                Slog.w(TAG, "Error writing compat packages", e1);
                if (fos != null) {
                    this.mFile.failWrite(fos);
                }
                if (fos == null) {
                    return;
                }
                fos.close();
            }
            if (fos == null) {
                return;
            }
            fos.close();
        } catch (IOException e2) {
        }
    }

    void saveSpecialModeFile() {
        HashMap<String, Integer> pkgs = new HashMap<>();
        synchronized (this.mLock) {
            pkgs.putAll(this.mNotchSpecialModePackages);
        }
        FileOutputStream fos = null;
        try {
            try {
                try {
                    fos = this.mSpecialModeFile.startWrite();
                    FastXmlSerializer fastXmlSerializer = new FastXmlSerializer();
                    fastXmlSerializer.setOutput(fos, StandardCharsets.UTF_8.name());
                    fastXmlSerializer.startDocument(null, true);
                    fastXmlSerializer.setFeature(MiuiSizeCompatService.FAST_XML, true);
                    fastXmlSerializer.startTag(null, "special-mode");
                    for (Map.Entry<String, Integer> entry : pkgs.entrySet()) {
                        String pkg = entry.getKey();
                        int mode = entry.getValue().intValue();
                        boolean special = mode > 0;
                        if (special) {
                            fastXmlSerializer.startTag(null, SplitScreenReporter.STR_PKG);
                            fastXmlSerializer.attribute(null, TemperatureController.STRATEGY_NAME, pkg);
                            fastXmlSerializer.attribute(null, "mode", Integer.toString(mode));
                            fastXmlSerializer.endTag(null, SplitScreenReporter.STR_PKG);
                        }
                    }
                    fastXmlSerializer.endTag(null, "special-mode");
                    fastXmlSerializer.endDocument();
                    this.mSpecialModeFile.finishWrite(fos);
                } catch (Throwable th) {
                    if (fos != null) {
                        try {
                            fos.close();
                        } catch (IOException e) {
                        }
                    }
                    throw th;
                }
            } catch (Exception e1) {
                Slog.w(TAG, "Error writing compat packages", e1);
                if (fos != null) {
                    this.mSpecialModeFile.failWrite(fos);
                }
                if (fos == null) {
                    return;
                }
                fos.close();
            }
            if (fos == null) {
                return;
            }
            fos.close();
        } catch (IOException e2) {
        }
    }

    void saveCutoutModeFile() {
        HashMap<String, Integer> pkgs = new HashMap<>();
        synchronized (this.mLock) {
            pkgs.putAll(this.mUserCutoutModePackages);
        }
        FileOutputStream fos = null;
        try {
            try {
                try {
                    fos = this.mCutoutModeFile.startWrite();
                    FastXmlSerializer fastXmlSerializer = new FastXmlSerializer();
                    fastXmlSerializer.setOutput(fos, StandardCharsets.UTF_8.name());
                    fastXmlSerializer.startDocument(null, true);
                    fastXmlSerializer.setFeature(MiuiSizeCompatService.FAST_XML, true);
                    fastXmlSerializer.startTag(null, "cutout-mode");
                    for (Map.Entry<String, Integer> entry : pkgs.entrySet()) {
                        String pkg = entry.getKey();
                        int mode = entry.getValue().intValue();
                        fastXmlSerializer.startTag(null, SplitScreenReporter.STR_PKG);
                        fastXmlSerializer.attribute(null, TemperatureController.STRATEGY_NAME, pkg);
                        fastXmlSerializer.attribute(null, "mode", Integer.toString(mode));
                        fastXmlSerializer.endTag(null, SplitScreenReporter.STR_PKG);
                    }
                    fastXmlSerializer.endTag(null, "cutout-mode");
                    fastXmlSerializer.endDocument();
                    this.mCutoutModeFile.finishWrite(fos);
                    if (fos == null) {
                        return;
                    }
                    fos.close();
                } catch (Exception e1) {
                    Slog.w(TAG, "Error writing cutout packages", e1);
                    if (fos != null) {
                        this.mCutoutModeFile.failWrite(fos);
                    }
                    if (fos == null) {
                        return;
                    }
                    fos.close();
                }
            } catch (IOException e) {
            }
        } catch (Throwable th) {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e2) {
                }
            }
            throw th;
        }
    }

    private boolean isDefaultRestrict(String pkg) {
        int type = getDefaultAspectType(pkg);
        return type == 4 || type == 5;
    }

    private float getDeviceAspect() {
        float f = this.mDefaultAspect;
        float ratio = MiuiFreeformPinManagerService.EDGE_AREA;
        if (f <= MiuiFreeformPinManagerService.EDGE_AREA) {
            Display display = this.mContext.getDisplay();
            Point point = new Point();
            display.getRealSize(point);
            int min = Math.min(point.x, point.y);
            int max = Math.max(point.x, point.y);
            if (min != 0) {
                ratio = (max * 1.0f) / min;
            }
            this.mDefaultAspect = ratio;
        }
        return this.mDefaultAspect;
    }

    private int getDefaultMode(String pkg) {
        return isDefaultRestrict(pkg) ? 1 : 0;
    }

    private void removePackage(String packageName) {
        boolean realRemove = false;
        synchronized (this.mLock) {
            this.mDefaultType.remove(packageName);
            if (this.mPackages.containsKey(packageName)) {
                this.mPackages.remove(packageName);
                realRemove = true;
            }
        }
        if (realRemove) {
            scheduleWrite();
        }
    }

    private void removeSpecialModePackage(String packageName) {
        boolean realRemove = false;
        synchronized (this.mLock) {
            this.mNotchConfig.remove(packageName);
            if (this.mNotchSpecialModePackages.containsKey(packageName)) {
                this.mNotchSpecialModePackages.remove(packageName);
                realRemove = true;
            }
        }
        if (realRemove) {
            scheduleWriteSpecialMode();
        }
    }

    public void handleRemovePackage(String packageName) {
        removePackage(packageName);
        removeSpecialModePackage(packageName);
    }

    public void handleUpdatePackage(String packageName) {
        synchronized (this.mLock) {
            this.mDefaultType.remove(packageName);
            this.mNotchConfig.remove(packageName);
        }
        boolean isDefaultRestrict = isDefaultRestrict(packageName);
        boolean isRestrict = isRestrictAspect(packageName);
        if (isDefaultRestrict == isRestrict || getDefaultAspectType(packageName) == 1) {
            Slog.i(TAG, "package " + packageName + " updated, removing config");
            removePackage(packageName);
        }
    }

    public float getAspectRatio(String pkg) {
        if (isRestrictAspect(pkg)) {
            return CustomizeUtil.RESTRICT_ASPECT_RATIO;
        }
        return 3.0f;
    }

    public int getNotchConfig(String packageName) {
        Integer mode;
        int config = 0;
        synchronized (this.mLock) {
            if (this.mNotchSpecialModePackages.containsKey(packageName) && (mode = this.mNotchSpecialModePackages.get(packageName)) != null) {
                config = mode.intValue() != 0 ? 128 : 0;
            }
        }
        return config | getDefaultNotchConfig(packageName);
    }

    private int getDefaultNotchConfig(String packageName) {
        synchronized (this.mLock) {
            if (this.mNotchConfig.containsKey(packageName)) {
                return this.mNotchConfig.get(packageName).intValue();
            }
            int type = resolveNotchConfig(packageName);
            synchronized (this.mLock) {
                this.mNotchConfig.put(packageName, Integer.valueOf(type));
            }
            return type;
        }
    }

    private int resolveNotchConfig(String packageName) {
        Bundle metadata;
        if (this.mSupportNotchList.contains(packageName)) {
            return 1792;
        }
        ApplicationInfo ai = null;
        try {
            ai = AppGlobals.getPackageManager().getApplicationInfo(packageName, 128L, 0);
        } catch (RemoteException e) {
        }
        if (ai == null || (metadata = ai.metaData) == null) {
            return 0;
        }
        String notch = metadata.getString("notch.config");
        if (TextUtils.isEmpty(notch)) {
            return 0;
        }
        int config = 0 | 256;
        if (notch.contains("portrait")) {
            config |= 512;
        }
        if (notch.contains("landscape")) {
            return config | 1024;
        }
        return config;
    }

    public void setNotchSpecialMode(String pkg, boolean special) {
        boolean oldSpecail = isNotchSpecailMode(pkg);
        if (special != oldSpecail) {
            synchronized (this.mLock) {
                this.mNotchSpecialModePackages.put(pkg, Integer.valueOf(special ? 1 : 0));
            }
            scheduleWriteSpecialMode();
            ((ActivityManager) this.mContext.getSystemService("activity")).forceStopPackage(pkg);
        }
    }

    private boolean isNotchSpecailMode(String pkg) {
        return getSpecialMode(pkg) != 0;
    }

    public void setCutoutMode(String pkg, int mode) {
        synchronized (this.mLock) {
            this.mUserCutoutModePackages.put(pkg, Integer.valueOf(mode));
        }
        scheduleWriteCutoutMode();
        ((ActivityManager) this.mContext.getSystemService("activity")).forceStopPackage(pkg);
    }

    public int getCutoutMode(String pkg) {
        synchronized (this.mLock) {
            if (this.mUserCutoutModePackages.containsKey(pkg)) {
                return this.mUserCutoutModePackages.get(pkg).intValue();
            }
            synchronized (this.mLock) {
                if (this.mCloudCutoutModePackages.containsKey(pkg)) {
                    return this.mCloudCutoutModePackages.get(pkg).intValue();
                }
                int flag = getDefaultNotchConfig(pkg);
                if ((flag & 1792) != 1792) {
                    return 0;
                }
                return 1;
            }
        }
    }

    public int getDefaultAspectType(String packageName) {
        synchronized (this.mLock) {
            if (this.mDefaultType.containsKey(packageName)) {
                return this.mDefaultType.get(packageName).intValue();
            }
            int type = resolveDefaultAspectType(packageName);
            synchronized (this.mLock) {
                this.mDefaultType.put(packageName, Integer.valueOf(type));
            }
            return type;
        }
    }

    private int resolveDefaultAspectType(String packageName) {
        if (MiuiFreeFormGestureDetector.FAMILYSMILE_PACKAGE.equals(packageName) || MiuiFreeFormGestureDetector.PARENTALCONTROLS_PACKAGE.equals(packageName)) {
            return 1;
        }
        if (this.mRestrictList.contains(packageName)) {
            return 4;
        }
        ApplicationInfo ai = null;
        try {
            ai = AppGlobals.getPackageManager().getApplicationInfo(packageName, 128L, 0);
        } catch (RemoteException e) {
        }
        if (ai == null) {
            return 0;
        }
        Bundle metadata = ai.metaData;
        float aspect = MiuiFreeformPinManagerService.EDGE_AREA;
        if (metadata != null) {
            aspect = metadata.getFloat("android.max_aspect");
        }
        if (aspect >= getDeviceAspect()) {
            return 1;
        }
        if (this.mSuggestList.contains(packageName)) {
            return 3;
        }
        return 5;
    }

    public boolean isRestrictAspect(String packageName) {
        return getPackageMode(packageName) != MiuiFreeformPinManagerService.EDGE_AREA;
    }

    public void setRestrictAspect(String pkg, boolean restrict) {
        boolean curRestrict = isRestrictAspect(pkg);
        if (restrict != curRestrict) {
            synchronized (this.mLock) {
                this.mPackages.put(pkg, Integer.valueOf(restrict ? 1 : 0));
            }
            scheduleWrite();
            ((ActivityManager) this.mContext.getSystemService("activity")).forceStopPackage(pkg);
        }
    }

    public void handleRegisterObservers() {
        if (!this.mNotifySuggestApps) {
            return;
        }
        this.mProcessObserver = new AppLaunchObserver() { // from class: com.miui.server.MiuiCompatModePackages.4
            @Override // com.miui.server.MiuiCompatModePackages.AppLaunchObserver
            protected void onFirstLaunch(String packageName) {
                MiuiCompatModePackages.this.mHandler.removeMessages(104);
                MiuiCompatModePackages.this.mHandler.sendMessageDelayed(Message.obtain(MiuiCompatModePackages.this.mHandler, 104, packageName), 500L);
            }
        };
        try {
            Slog.i(TAG, "registering process observer...");
            ActivityManagerNative.getDefault().registerProcessObserver(this.mProcessObserver);
        } catch (RemoteException e) {
            this.mProcessObserver = null;
            Slog.e(TAG, "error when registering process observer", e);
        }
    }

    public void handleUnregisterObservers() {
        if (this.mProcessObserver != null) {
            Slog.i(TAG, "unregistering process observer...");
            try {
                try {
                    ActivityManagerNative.getDefault().unregisterProcessObserver(this.mProcessObserver);
                } catch (RemoteException e) {
                    Slog.e(TAG, "error when unregistering process observer", e);
                }
            } finally {
                this.mProcessObserver = null;
            }
        }
    }

    public void handleDontShowAgain() {
        this.mNotifySuggestApps = false;
        this.mHandler.sendEmptyMessage(103);
        this.mHandler.sendEmptyMessage(100);
    }

    private void gotoMaxAspectSettings() {
        try {
            Intent intent = new Intent();
            intent.setClassName("com.android.settings", "com.android.settings.SubSettings");
            intent.putExtra(":settings:show_fragment", "com.android.settings.MaxAspectRatioSettings");
            intent.addFlags(268435456);
            this.mContext.startActivityAsUser(intent, UserHandle.CURRENT);
        } catch (Exception e) {
            Slog.e(TAG, "error when goto max aspect settings", e);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static abstract class AppLaunchObserver extends IMiuiProcessObserver {
        private HashSet<Integer> mRunningFgActivityProcesses;

        protected abstract void onFirstLaunch(String str);

        private AppLaunchObserver() {
            this.mRunningFgActivityProcesses = new HashSet<>();
        }

        public void onForegroundActivitiesChanged(int pid, int uid, boolean foregroundActivities) {
            if (foregroundActivities && !this.mRunningFgActivityProcesses.contains(Integer.valueOf(pid))) {
                this.mRunningFgActivityProcesses.add(Integer.valueOf(pid));
                String packageName = ProcessUtils.getPackageNameByPid(pid);
                onFirstLaunch(packageName);
            }
        }

        public void onForegroundServicesChanged(int pid, int uid, int serviceTypes) {
        }

        public void onImportanceChanged(int pid, int uid, int importance) {
        }

        public void onProcessDied(int pid, int uid) {
            this.mRunningFgActivityProcesses.remove(Integer.valueOf(pid));
        }

        public void onProcessStateChanged(int pid, int uid, int procState) {
        }
    }
}
