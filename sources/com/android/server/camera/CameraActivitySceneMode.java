package com.android.server.camera;

import android.content.ComponentName;
import android.os.SystemProperties;
import android.util.Log;
import com.miui.base.MiuiStubRegistry;
import java.util.ArrayList;
import java.util.List;
import miui.process.IActivityChangeListener;
import miui.process.ProcessManager;
/* loaded from: classes.dex */
public class CameraActivitySceneMode implements CameraActivitySceneStub {
    private static volatile CameraActivitySceneMode sIntance;
    private IActivityChangeListener.Stub mActivityChangeListener = new IActivityChangeListener.Stub() { // from class: com.android.server.camera.CameraActivitySceneMode.1
        public void onActivityChanged(ComponentName preName, ComponentName curName) {
            if (curName == null) {
                return;
            }
            String curActivityName = curName.toString();
            CameraActivitySceneMode.this.decideActivitySceneMode(curActivityName);
        }
    };
    private static final String TAG = CameraActivitySceneMode.class.getSimpleName();
    private static final String ACTIVITY_WECHAT_VIDEO = "com.tencent.mm.plugin.voip.ui.VideoActivity";
    private static final String ACTIVITY_WECHAT_SCAN = "com.tencent.mm.plugin.scanner.ui.BaseScanUI";
    private static final String ACTIVITY_ALIPAY_COMMON_SCAN = "com.alipay.mobile.scan.as.main.MainCaptureActivity";
    private static final String ACTIVITY_ALIPAY_HEALTH_SCAN = "com.alipay.mobile.scan.as.tool.ToolsCaptureActivity";
    private static final String[] mTargetActivityList = {ACTIVITY_WECHAT_VIDEO, ACTIVITY_WECHAT_SCAN, ACTIVITY_ALIPAY_COMMON_SCAN, ACTIVITY_ALIPAY_HEALTH_SCAN};
    private static final String[] mTargetPackageList = new String[0];

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<CameraActivitySceneMode> {

        /* compiled from: CameraActivitySceneMode$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final CameraActivitySceneMode INSTANCE = new CameraActivitySceneMode();
        }

        public CameraActivitySceneMode provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public CameraActivitySceneMode provideNewInstance() {
            return new CameraActivitySceneMode();
        }
    }

    private void registerActivityChangeListener() {
        if (this.mActivityChangeListener != null) {
            List<String> targetActivities = new ArrayList<>();
            List<String> targetPackages = new ArrayList<>();
            int i = 0;
            while (true) {
                String[] strArr = mTargetActivityList;
                if (i >= strArr.length) {
                    break;
                }
                targetActivities.add(strArr[i]);
                i++;
            }
            int i2 = 0;
            while (true) {
                String[] strArr2 = mTargetPackageList;
                if (i2 < strArr2.length) {
                    targetPackages.add(strArr2[i2]);
                    i2++;
                } else {
                    ProcessManager.registerActivityChangeListener(targetPackages, targetActivities, this.mActivityChangeListener);
                    return;
                }
            }
        }
    }

    public void decideActivitySceneMode(String activityName) {
        SystemProperties.set("persist.vendor.vcb.video", "false");
        SystemProperties.set("persist.vendor.vcb.activity", Integer.toString(0));
        int i = 0;
        while (true) {
            CharSequence[] charSequenceArr = mTargetActivityList;
            if (i < charSequenceArr.length) {
                if (!activityName.contains(charSequenceArr[i])) {
                    i++;
                } else {
                    Log.i(TAG, "Activity is(" + charSequenceArr[i] + ")");
                    SystemProperties.set("persist.vendor.vcb.activity", charSequenceArr[i]);
                    if (activityName.contains(ACTIVITY_WECHAT_VIDEO)) {
                        SystemProperties.set("persist.vendor.vcb.video", "true");
                        return;
                    }
                    return;
                }
            } else {
                return;
            }
        }
    }

    public void initSystemBooted() {
        SystemProperties.set("persist.vendor.vcb.video", "false");
        SystemProperties.set("persist.vendor.vcb.activity", Integer.toString(0));
        registerActivityChangeListener();
    }

    public static CameraActivitySceneMode getInstance() {
        if (sIntance == null) {
            synchronized (CameraActivitySceneMode.class) {
                if (sIntance == null) {
                    sIntance = new CameraActivitySceneMode();
                }
            }
        }
        return sIntance;
    }
}
