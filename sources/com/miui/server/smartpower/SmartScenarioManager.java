package com.miui.server.smartpower;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.util.ArraySet;
import android.util.LongSparseArray;
import android.util.SparseArray;
import com.android.server.am.AppStateManager;
import com.android.server.am.SmartPowerService;
import com.miui.server.smartpower.SmartScenarioManager;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Predicate;
/* loaded from: classes.dex */
public class SmartScenarioManager {
    public static final boolean DEBUG = SmartPowerService.DEBUG;
    public static final int INTERACTIVE_ACTION_TYPE_BACKGROUND = 8;
    public static final int INTERACTIVE_ACTION_TYPE_CHARGING = 2048;
    public static final int INTERACTIVE_ACTION_TYPE_DOWNLOAD = 256;
    public static final int INTERACTIVE_ACTION_TYPE_FREEFORM = 16;
    public static final int INTERACTIVE_ACTION_TYPE_MUSICPLAY = 512;
    public static final int INTERACTIVE_ACTION_TYPE_NAVIGATION = 4096;
    public static final int INTERACTIVE_ACTION_TYPE_OVERLAY = 32;
    public static final int INTERACTIVE_ACTION_TYPE_TOPAPP = 2;
    public static final int INTERACTIVE_ACTION_TYPE_TOPGAME = 4;
    public static final int INTERACTIVE_ACTION_TYPE_VIDEOCALL = 128;
    public static final int INTERACTIVE_ACTION_TYPE_VIDEOPLAY = 1024;
    public static final int INTERACTIVE_ACTION_TYPE_VOICECALL = 64;
    public static final int INTERACTIVE_ADDITIONAL_SCENARIO_ACTION_MASK = 8184;
    public static final int INTERACTIVE_MAIN_SCENARIO_ACTION_MASK = 6;
    private static final int RESOURCE_SCENARIO_DOWNLOAD = 128;
    private static final int RESOURCE_SCENARIO_MUSIC = 2;
    private static final int RESOURCE_SCENARIO_NAVIGATION = 64;
    private static final int RESOURCE_SCENARIO_VIDEO = 4;
    private static final int RESOURCE_SCENARIO_VIDEOCALL = 48;
    private static final int RESOURCE_SCENARIO_VOICECALL = 16;
    public static final String TAG = "SmartPower.Scene";
    private Context mContext;
    private Handler mHandler;
    private final BroadcastReceiver mReceiver;
    private final SparseArray<ScenarioItem> mCurrentScenarios = new SparseArray<>();
    private final ArraySet<ClientConfig> mClients = new ArraySet<>();
    private final ArraySet<String> mInterestBgPkgs = new ArraySet<>();
    private boolean mCharging = false;

    /* loaded from: classes.dex */
    public interface ISmartScenarioCallback {
        void onCurrentScenarioChanged(long j, long j2);
    }

    public static String actionTypeToString(int action) {
        String typeStr = "";
        if ((action & 2) != 0) {
            typeStr = typeStr + "top app, ";
        }
        if ((action & 4) != 0) {
            typeStr = typeStr + "top game, ";
        }
        if ((action & 8) != 0) {
            typeStr = typeStr + "background, ";
        }
        if ((action & 16) != 0) {
            typeStr = typeStr + "freeform, ";
        }
        if ((action & 32) != 0) {
            typeStr = typeStr + "overlay, ";
        }
        if ((action & 64) != 0) {
            typeStr = typeStr + "voice call, ";
        }
        if ((action & 128) != 0) {
            typeStr = typeStr + "video call, ";
        }
        if ((action & 256) != 0) {
            typeStr = typeStr + "download, ";
        }
        if ((action & 512) != 0) {
            typeStr = typeStr + "music play, ";
        }
        if ((action & 1024) != 0) {
            typeStr = typeStr + "video play, ";
        }
        if ((action & 2048) != 0) {
            typeStr = typeStr + "charging, ";
        }
        if ((action & 4096) != 0) {
            return typeStr + "navigation, ";
        }
        return typeStr;
    }

    public static int calculateScenarioAction(int behavier) {
        if (behavierContainsScenario(behavier, 48)) {
            return 128;
        }
        if (behavierContainsScenario(behavier, 16)) {
            return 64;
        }
        if (behavierContainsScenario(behavier, 64)) {
            return 4096;
        }
        if (behavierContainsScenario(behavier, 4)) {
            return 1024;
        }
        if (behavierContainsScenario(behavier, 2)) {
            return 512;
        }
        if (behavierContainsScenario(behavier, 128)) {
            return 256;
        }
        return 0;
    }

    private static boolean behavierContainsScenario(int behavier, int scenario) {
        return (behavier & scenario) == scenario;
    }

    public SmartScenarioManager(Context context, Looper looper) {
        BroadcastReceiver broadcastReceiver = new BroadcastReceiver() { // from class: com.miui.server.smartpower.SmartScenarioManager.1
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                if (intent.getAction().equals("android.intent.action.BATTERY_CHANGED")) {
                    boolean charging = true;
                    boolean present = intent.getBooleanExtra("present", true);
                    boolean plugged = intent.getIntExtra("plugged", 0) != 0;
                    if (!present || !plugged) {
                        charging = false;
                    }
                    if (charging != SmartScenarioManager.this.mCharging) {
                        SmartScenarioManager.this.mCharging = charging;
                        SmartScenarioManager smartScenarioManager = SmartScenarioManager.this;
                        smartScenarioManager.onAppActionChanged(2048, null, smartScenarioManager.mCharging);
                    }
                }
            }
        };
        this.mReceiver = broadcastReceiver;
        this.mContext = context;
        this.mHandler = new Handler(looper);
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.BATTERY_CHANGED");
        this.mContext.registerReceiver(broadcastReceiver, filter);
    }

    public void onAppActionChanged(final int action, AppStateManager.AppState app, final boolean add) {
        synchronized (this.mInterestBgPkgs) {
            if (app != null && action == 8) {
                if (!this.mInterestBgPkgs.contains(app.getPackageName())) {
                    return;
                }
            }
            boolean changed = false;
            synchronized (this.mCurrentScenarios) {
                ScenarioItem scenarioItem = this.mCurrentScenarios.get(action);
                if (add && scenarioItem == null) {
                    scenarioItem = new ScenarioItem(action);
                    this.mCurrentScenarios.append(action, scenarioItem);
                    changed = true;
                }
                if (scenarioItem != null) {
                    scenarioItem.onAppActionChanged(app, add);
                    if (!add && scenarioItem.mInteractiveApps.size() == 0) {
                        this.mCurrentScenarios.remove(action);
                        changed = true;
                    }
                }
            }
            if (changed) {
                synchronized (this.mClients) {
                    this.mClients.forEach(new Consumer() { // from class: com.miui.server.smartpower.SmartScenarioManager$$ExternalSyntheticLambda0
                        @Override // java.util.function.Consumer
                        public final void accept(Object obj) {
                            ((SmartScenarioManager.ClientConfig) obj).onAppActionChanged(action, null, add);
                        }
                    });
                }
            }
        }
    }

    public ClientConfig createClientConfig(String clientName, ISmartScenarioCallback callback) {
        return new ClientConfig(clientName, callback);
    }

    public void registClientConfig(ClientConfig client) {
        synchronized (this.mClients) {
            this.mClients.add(client);
        }
        parseCurrentScenarioId(client);
    }

    public void parseCurrentScenarioId(ClientConfig client) {
        synchronized (this.mCurrentScenarios) {
            for (int i = 0; i < this.mCurrentScenarios.size(); i++) {
                ScenarioItem scenarioItem = this.mCurrentScenarios.valueAt(i);
                scenarioItem.parseCurrentScenarioId(client);
            }
        }
    }

    public void dump(PrintWriter pw, String[] args, int opti) {
        pw.println("interactive apps:");
        synchronized (this.mCurrentScenarios) {
            for (int i = 0; i < this.mCurrentScenarios.size(); i++) {
                pw.println("        " + this.mCurrentScenarios.valueAt(i));
            }
        }
        pw.println("");
        synchronized (this.mClients) {
            for (int i2 = 0; i2 < this.mClients.size(); i2++) {
                this.mClients.valueAt(i2).dump(pw, args, opti);
            }
        }
    }

    /* loaded from: classes.dex */
    public class ScenarioItem {
        private int mAction;
        private final ArrayList<ScenarioAppChild> mInteractiveApps;

        private ScenarioItem(int action) {
            SmartScenarioManager.this = r1;
            this.mInteractiveApps = new ArrayList<>();
            this.mAction = action;
        }

        public int getAction() {
            return this.mAction;
        }

        public void parseCurrentScenarioId(ClientConfig client) {
            client.onAppActionChanged(this.mAction, null, true);
            synchronized (this.mInteractiveApps) {
                Iterator<ScenarioAppChild> it = this.mInteractiveApps.iterator();
                while (it.hasNext()) {
                    ScenarioAppChild app = it.next();
                    client.onAppActionChanged(this.mAction, app.getPackageName(), true);
                }
            }
        }

        public void onAppActionChanged(final AppStateManager.AppState app, final boolean add) {
            boolean changed = false;
            synchronized (this.mInteractiveApps) {
                if (add) {
                    boolean contains = false;
                    Iterator<ScenarioAppChild> it = this.mInteractiveApps.iterator();
                    while (true) {
                        if (!it.hasNext()) {
                            break;
                        }
                        ScenarioAppChild child = it.next();
                        if (child.mApp == app) {
                            contains = true;
                            break;
                        }
                    }
                    if (!contains) {
                        this.mInteractiveApps.add(new ScenarioAppChild(app));
                        changed = true;
                    }
                } else if (this.mInteractiveApps.removeIf(new Predicate() { // from class: com.miui.server.smartpower.SmartScenarioManager$ScenarioItem$$ExternalSyntheticLambda0
                    @Override // java.util.function.Predicate
                    public final boolean test(Object obj) {
                        return SmartScenarioManager.ScenarioItem.lambda$onAppActionChanged$0(AppStateManager.AppState.this, (SmartScenarioManager.ScenarioAppChild) obj);
                    }
                })) {
                    changed = true;
                }
            }
            if (changed) {
                synchronized (SmartScenarioManager.this.mClients) {
                    SmartScenarioManager.this.mClients.forEach(new Consumer() { // from class: com.miui.server.smartpower.SmartScenarioManager$ScenarioItem$$ExternalSyntheticLambda1
                        @Override // java.util.function.Consumer
                        public final void accept(Object obj) {
                            SmartScenarioManager.ScenarioItem.this.m2428x190b0759(app, add, (SmartScenarioManager.ClientConfig) obj);
                        }
                    });
                }
            }
        }

        public static /* synthetic */ boolean lambda$onAppActionChanged$0(AppStateManager.AppState app, ScenarioAppChild item) {
            return item.mApp == app;
        }

        /* renamed from: lambda$onAppActionChanged$1$com-miui-server-smartpower-SmartScenarioManager$ScenarioItem */
        public /* synthetic */ void m2428x190b0759(AppStateManager.AppState app, boolean add, ClientConfig v) {
            String packageName = app == null ? null : app.getPackageName();
            v.onAppActionChanged(this.mAction, packageName, add);
        }

        public String toString() {
            return SmartScenarioManager.actionTypeToString(this.mAction) + " : " + this.mInteractiveApps;
        }
    }

    /* loaded from: classes.dex */
    public class ScenarioAppChild {
        private AppStateManager.AppState mApp;
        private String mPackageName;

        private ScenarioAppChild(AppStateManager.AppState app) {
            SmartScenarioManager.this = r1;
            this.mPackageName = null;
            this.mApp = app;
            if (app != null) {
                this.mPackageName = app.getPackageName();
            }
        }

        public String getPackageName() {
            return this.mPackageName;
        }

        public String toString() {
            return this.mPackageName;
        }
    }

    /* loaded from: classes.dex */
    public class ClientConfig {
        private long mAdditionalScenarioDescription;
        private ISmartScenarioCallback mCallback;
        String mClientName;
        private long mMainSenarioDescription;
        private final LongSparseArray<ScenarioIdInfo> mMainScenarioIdMap = new LongSparseArray<>();
        private final LongSparseArray<ScenarioIdInfo> mAdditionalScenarioIdMap = new LongSparseArray<>();

        public ClientConfig(String clientName, ISmartScenarioCallback callback) {
            SmartScenarioManager.this = this$0;
            this.mClientName = clientName;
            this.mCallback = callback;
        }

        public void addMainScenarioIdConfig(long id, String packageName, int action) {
            synchronized (this.mMainScenarioIdMap) {
                this.mMainScenarioIdMap.put(id, new ScenarioIdInfo(packageName, action, id));
            }
        }

        public void addAdditionalScenarioIdConfig(long id, String packageName, int action) {
            if (action == 8) {
                synchronized (SmartScenarioManager.this.mInterestBgPkgs) {
                    SmartScenarioManager.this.mInterestBgPkgs.add(packageName);
                }
            }
            synchronized (this.mAdditionalScenarioIdMap) {
                this.mAdditionalScenarioIdMap.put(id, new ScenarioIdInfo(packageName, action, id));
            }
        }

        public void onAppActionChanged(int action, String packageName, boolean add) {
            if ((action & 6) != 0) {
                onMainScenarioChanged(action, packageName, add);
            } else if ((action & SmartScenarioManager.INTERACTIVE_ADDITIONAL_SCENARIO_ACTION_MASK) != 0) {
                onAdditionalScenarioChanged(action, packageName, add);
            }
        }

        private void onMainScenarioChanged(int action, String packageName, boolean add) {
            long currentScenarioDescription = this.mMainSenarioDescription;
            synchronized (this.mMainScenarioIdMap) {
                for (int i = 0; i < this.mMainScenarioIdMap.size(); i++) {
                    ScenarioIdInfo info = this.mMainScenarioIdMap.valueAt(i);
                    if ((info.mAction & action) != 0 && ((packageName != null && packageName.equals(info.mPackageName)) || (packageName == null && info.mPackageName == null))) {
                        if (add) {
                            currentScenarioDescription |= info.mId;
                        } else {
                            currentScenarioDescription &= ~info.mId;
                        }
                    }
                }
            }
            if (currentScenarioDescription != this.mMainSenarioDescription) {
                this.mMainSenarioDescription = currentScenarioDescription;
                reportSenarioChanged(currentScenarioDescription, this.mAdditionalScenarioDescription);
            }
        }

        private void reportSenarioChanged(final long mainScenario, final long additionalScenario) {
            SmartScenarioManager.this.mHandler.post(new Runnable() { // from class: com.miui.server.smartpower.SmartScenarioManager.ClientConfig.1
                @Override // java.lang.Runnable
                public void run() {
                    if (ClientConfig.this.mMainSenarioDescription != mainScenario || ClientConfig.this.mAdditionalScenarioDescription != additionalScenario) {
                        return;
                    }
                    ClientConfig.this.mCallback.onCurrentScenarioChanged(ClientConfig.this.mMainSenarioDescription, ClientConfig.this.mAdditionalScenarioDescription);
                }
            });
        }

        private void onAdditionalScenarioChanged(int action, String packageName, boolean add) {
            long currentScenarioDescription = this.mAdditionalScenarioDescription;
            synchronized (this.mAdditionalScenarioIdMap) {
                for (int i = 0; i < this.mAdditionalScenarioIdMap.size(); i++) {
                    ScenarioIdInfo info = this.mAdditionalScenarioIdMap.valueAt(i);
                    if ((info.mAction & action) != 0 && ((packageName != null && packageName.equals(info.mPackageName)) || (packageName == null && info.mPackageName == null))) {
                        if (add) {
                            currentScenarioDescription |= info.mId;
                        } else {
                            currentScenarioDescription &= ~info.mId;
                        }
                    }
                }
            }
            if (currentScenarioDescription != this.mAdditionalScenarioDescription) {
                this.mAdditionalScenarioDescription = currentScenarioDescription;
                reportSenarioChanged(this.mMainSenarioDescription, currentScenarioDescription);
            }
        }

        public void dump(PrintWriter pw, String[] args, int opti) {
            pw.println("client " + this.mClientName + ":");
            pw.println("    main scenario:");
            synchronized (this.mMainScenarioIdMap) {
                for (int i = 0; i < this.mMainScenarioIdMap.size(); i++) {
                    ScenarioIdInfo info = this.mMainScenarioIdMap.valueAt(i);
                    if ((info.mId & this.mMainSenarioDescription) != 0) {
                        pw.println("        " + info);
                    }
                }
            }
            pw.println("    addtional scenario:");
            synchronized (this.mAdditionalScenarioIdMap) {
                for (int i2 = 0; i2 < this.mAdditionalScenarioIdMap.size(); i2++) {
                    ScenarioIdInfo info2 = this.mAdditionalScenarioIdMap.valueAt(i2);
                    if ((info2.mId & this.mAdditionalScenarioDescription) != 0) {
                        pw.println("        " + info2);
                    }
                }
            }
        }
    }

    /* loaded from: classes.dex */
    public class ScenarioIdInfo {
        private int mAction;
        private long mId;
        private String mPackageName;

        public ScenarioIdInfo(String packageName, int action, long id) {
            SmartScenarioManager.this = this$0;
            this.mPackageName = packageName;
            this.mAction = action;
            this.mId = id;
        }

        public String toString() {
            return SmartScenarioManager.actionTypeToString(this.mAction) + " " + this.mPackageName;
        }
    }
}
