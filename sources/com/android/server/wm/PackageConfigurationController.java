package com.android.server.wm;

import android.content.Context;
import android.os.Process;
import android.util.MiuiAppSizeCompatModeStub;
import android.util.Slog;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
/* loaded from: classes.dex */
public class PackageConfigurationController extends Thread {
    private static final String COMMAND_OPTION_FORCE_UPDATE = "ForceUpdate";
    private static final String COMMAND_OPTION_POLICY_RESET = "PolicyReset";
    private static final String PACKAGE_CONFIGURATION_COMMAND = "-packageconfiguration";
    private static final String PREFIX_ACTION_POLICY_UPDATED = "sec.app.policy.UPDATE.";
    private static final String SET_POLICY_DISABLED_COMMAND = "-setPolicyDisabled";
    private static final String TAG = "PackageConfigurationController";
    final ActivityTaskManagerService mAtmService;
    private final Context mContext;
    boolean mPolicyDisabled;
    private final ArrayList<String> mLogs = new ArrayList<>();
    private final Map<String, PolicyImpl> mPolicyImplMap = new ConcurrentHashMap();
    private final Set<String> mPolicyRequestQueue = new HashSet();
    private final Set<String> mTmpPolicyRequestQueue = new HashSet();

    public PackageConfigurationController(ActivityTaskManagerService atmService) {
        super("PackageConfigurationUpdateThread");
        this.mAtmService = atmService;
        this.mContext = atmService.mContext;
    }

    private void initialize() {
        Slog.d(TAG, "initialize");
        this.mPolicyImplMap.forEach(new BiConsumer() { // from class: com.android.server.wm.PackageConfigurationController$$ExternalSyntheticLambda1
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                PackageConfigurationController.this.m1861x7411b24e((String) obj, (PolicyImpl) obj2);
            }
        });
        scheduleUpdatePolicyItem(null, 300000L);
    }

    /* renamed from: lambda$initialize$0$com-android-server-wm-PackageConfigurationController */
    public /* synthetic */ void m1861x7411b24e(String str, PolicyImpl policy) {
        policy.init();
        scheduleUpdatePolicyItem(PREFIX_ACTION_POLICY_UPDATED + str, 0L);
    }

    public boolean executeShellCommand(String command, String[] args, final PrintWriter pw) {
        if (!MiuiAppSizeCompatModeStub.get().isEnabled()) {
            Slog.d(TAG, "MiuiAppSizeCompatMode not enabled");
            return false;
        }
        synchronized (this) {
            if (PACKAGE_CONFIGURATION_COMMAND.equals(command)) {
                if (args.length == 1) {
                    pw.println();
                    if (COMMAND_OPTION_FORCE_UPDATE.equals(args[0])) {
                        pw.println("Started the update.");
                        this.mPolicyImplMap.forEach(new BiConsumer() { // from class: com.android.server.wm.PackageConfigurationController$$ExternalSyntheticLambda2
                            @Override // java.util.function.BiConsumer
                            public final void accept(Object obj, Object obj2) {
                                PackageConfigurationController.lambda$executeShellCommand$1(pw, (String) obj, (PolicyImpl) obj2);
                            }
                        });
                    }
                }
                return true;
            } else if (SET_POLICY_DISABLED_COMMAND.equals(command)) {
                if (args.length == 1) {
                    if (args[0] == null) {
                        return true;
                    }
                    boolean newPolicyDisabled = Boolean.parseBoolean(args[0]);
                    if (this.mPolicyDisabled != newPolicyDisabled) {
                        this.mPolicyDisabled = newPolicyDisabled;
                        this.mPolicyImplMap.forEach(new BiConsumer() { // from class: com.android.server.wm.PackageConfigurationController$$ExternalSyntheticLambda3
                            @Override // java.util.function.BiConsumer
                            public final void accept(Object obj, Object obj2) {
                                String str = (String) obj;
                                ((PolicyImpl) obj2).propagateToCallbacks();
                            }
                        });
                    }
                }
                return true;
            } else {
                for (Object item : this.mPolicyImplMap.values()) {
                    if (((PolicyImpl) item).executeShellCommandLocked(command, args, pw)) {
                        return true;
                    }
                }
                return false;
            }
        }
    }

    public static /* synthetic */ void lambda$executeShellCommand$1(PrintWriter pw, String str, PolicyImpl policy) {
        policy.updatePolicyItem(true);
        pw.println(str + " update forced.");
    }

    public void registerPolicy(PolicyImpl impl) {
        this.mPolicyImplMap.put(impl.getPolicyName(), impl);
        this.mTmpPolicyRequestQueue.add(impl.getPolicyName());
    }

    @Override // java.lang.Thread, java.lang.Runnable
    public void run() {
        Process.setThreadPriority(10);
        initialize();
        synchronized (this) {
            while (true) {
                try {
                    try {
                        for (String str : this.mPolicyRequestQueue) {
                            PolicyImpl policyImpl = this.mPolicyImplMap.get(str);
                            if (policyImpl != null) {
                                policyImpl.updatePolicyItem(false);
                            }
                        }
                        this.mPolicyRequestQueue.clear();
                        wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } catch (Throwable th) {
                    throw th;
                }
            }
        }
    }

    public void scheduleUpdatePolicyItem(String policyRequest, long delayMillis) {
        if (policyRequest != null) {
            this.mTmpPolicyRequestQueue.add(policyRequest);
        }
        this.mAtmService.mH.postDelayed(new Runnable() { // from class: com.android.server.wm.PackageConfigurationController$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                PackageConfigurationController.this.m1862xb7bb1716();
            }
        }, delayMillis);
    }

    /* renamed from: lambda$scheduleUpdatePolicyItem$3$com-android-server-wm-PackageConfigurationController */
    public /* synthetic */ void m1862xb7bb1716() {
        synchronized (this) {
            try {
                this.mPolicyRequestQueue.addAll(this.mTmpPolicyRequestQueue);
                this.mTmpPolicyRequestQueue.clear();
                notifyAll();
            } catch (Exception e) {
            }
        }
    }

    public void startThread() {
        if (this.mPolicyImplMap.isEmpty()) {
            return;
        }
        start();
    }
}
