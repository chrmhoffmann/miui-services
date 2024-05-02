package com.miui.server.sptm;

import java.util.LinkedList;
import java.util.List;
/* loaded from: classes.dex */
public class SpeedTestModeState {
    public static final int FAST_SWITCH_OPS = 3;
    public static final int QUICK_SWITCH_OPS = 2;
    public static final int SLOW_SWITCH_OPS = 1;
    private static final List<String> SPTM_ENABLE_APP_LIST;
    private SpeedTestModeController mSpeedTestModeController;
    private int mFastSwitchCount = 0;
    private int mQuickSwitchCount = 0;
    private int mSpecifyAppCount = 0;
    private SpeedTestModeServiceImpl mSpeedTestModeService = SpeedTestModeServiceImpl.getInstance();

    static {
        LinkedList linkedList = new LinkedList();
        SPTM_ENABLE_APP_LIST = linkedList;
        linkedList.add("com.tencent.mobileqq");
        linkedList.add("com.tencent.mm");
        linkedList.add("com.sina.weibo");
        linkedList.add("com.taobao.taobao");
    }

    public SpeedTestModeState(SpeedTestModeController controller) {
        this.mSpeedTestModeController = controller;
    }

    public void addAppSwitchOps(int ops) {
        if (ops == 3) {
            this.mFastSwitchCount++;
        } else if (ops == 2) {
            this.mQuickSwitchCount++;
        } else if (ops == 1) {
            setMode(false);
            reset();
        }
    }

    public void addAppSwitchOps(String pkgName) {
        List<String> list;
        int appSize;
        int i;
        if (this.mSpeedTestModeService.getSPTMCloudEnable() && (i = this.mSpecifyAppCount) < (appSize = (list = SPTM_ENABLE_APP_LIST).size()) && pkgName.equals(list.get(i))) {
            int i2 = this.mSpecifyAppCount + 1;
            this.mSpecifyAppCount = i2;
            if (i2 == appSize) {
                setMode(true);
            }
        }
    }

    private void reset() {
        this.mFastSwitchCount = 0;
        this.mQuickSwitchCount = 0;
        this.mSpecifyAppCount = 0;
    }

    private void setMode(boolean isSPTMode) {
        this.mSpeedTestModeController.setSpeedTestMode(isSPTMode);
    }
}
