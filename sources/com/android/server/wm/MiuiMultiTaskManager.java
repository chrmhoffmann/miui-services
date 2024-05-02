package com.android.server.wm;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.SystemProperties;
import com.miui.base.MiuiStubRegistry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
/* loaded from: classes.dex */
public class MiuiMultiTaskManager implements MiuiMultiTaskManagerStub {
    public static final String FLAG_LAUNCH_APP_IN_ONE_TASK_GROUP = "miui_launch_app_in_one_task_group";
    public static final String TASK_RETURN_TO_TARGET = "miui_task_return_to_target";
    private static String[] sSupportUI = {"com.tencent.mm.plugin.webview.ui.tools.WebViewUI"};
    private static final boolean FEATURE_SUPPORT = SystemProperties.getBoolean("miui.multitask.enable", false);
    private static HashMap<String, LaunchAppInfo> sTargetMap = new HashMap<>();

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<MiuiMultiTaskManager> {

        /* compiled from: MiuiMultiTaskManager$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final MiuiMultiTaskManager INSTANCE = new MiuiMultiTaskManager();
        }

        public MiuiMultiTaskManager provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public MiuiMultiTaskManager provideNewInstance() {
            return new MiuiMultiTaskManager();
        }
    }

    static {
        init();
    }

    /* loaded from: classes.dex */
    public static class LaunchAppInfo {
        private ComponentName returnTarget;
        private ArrayList<String> supports;

        public LaunchAppInfo(ArrayList<String> supports, ComponentName returnTarget) {
            this.supports = supports;
            this.returnTarget = returnTarget;
        }
    }

    private static void init() {
        int i = 0;
        while (true) {
            String[] strArr = sSupportUI;
            if (i < strArr.length) {
                HashMap<String, LaunchAppInfo> hashMap = sTargetMap;
                String str = strArr[i];
                hashMap.put(str, getLaunchAppInfoByName(str));
                i++;
            } else {
                return;
            }
        }
    }

    private static LaunchAppInfo getLaunchAppInfoByName(String name) {
        if ("com.tencent.mm.plugin.webview.ui.tools.WebViewUI".equals(name)) {
            ArrayList<String> supports = new ArrayList<>();
            supports.add("com.tencent.mm.ui.LauncherUI");
            supports.add("com.tencent.mm.ui.chatting.ChattingUI");
            supports.add("com.tencent.mm.plugin.sns.ui.SnsTimeLineUI");
            supports.add("com.tencent.mm.plugin.readerapp.ui.ReaderAppUI");
            supports.add("com.tencent.mm.ui.conversation.BizConversationUI");
            supports.add("com.tencent.mm.plugin.webview.ui.tools.WebViewUI");
            ComponentName returnTarget = new ComponentName("com.tencent.mm", "com.tencent.mm.ui.LauncherUI");
            return new LaunchAppInfo(supports, returnTarget);
        }
        return null;
    }

    public void updateMultiTaskInfoIfNeed(Task stack, ActivityInfo aInfo, Intent intent) {
        if (FEATURE_SUPPORT && isVersionSupport() && aInfo != null && sTargetMap.containsKey(aInfo.name) && intent != null) {
            LaunchAppInfo info = sTargetMap.get(aInfo.name);
            ActivityRecord topr = stack != null ? stack.topRunningActivityLocked() : null;
            try {
                if (info.supports != null && info.returnTarget != null && topr != null && info.supports.contains(topr.info.name)) {
                    intent.setFlags(intent.getFlags() & (-134217729));
                    intent.addFlags(32768);
                    intent.addFlags(524288);
                    intent.putExtra(FLAG_LAUNCH_APP_IN_ONE_TASK_GROUP, true);
                    intent.putExtra(TASK_RETURN_TO_TARGET, info.returnTarget);
                }
            } catch (Exception e) {
            }
        }
    }

    static boolean isMultiTaskSupport(ActivityRecord record) {
        String[] strArr;
        if (!FEATURE_SUPPORT) {
            return false;
        }
        for (String className : sSupportUI) {
            if (record != null && record.info != null && Objects.equals(className, record.info.name)) {
                return true;
            }
        }
        return false;
    }

    static boolean checkMultiTaskAffinity(ActivityRecord target, ActivityRecord checkRecord) {
        String[] strArr;
        if (!FEATURE_SUPPORT) {
            return false;
        }
        for (String className : sSupportUI) {
            if (checkRecord != null && checkRecord.info != null && Objects.equals(className, checkRecord.info.name) && target != null && Objects.equals(target.packageName, checkRecord.packageName)) {
                return true;
            }
        }
        return false;
    }

    static boolean isVersionSupport() {
        return Build.VERSION.SDK_INT >= 21;
    }
}
