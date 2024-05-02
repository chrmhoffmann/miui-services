package com.miui.server.sptm;

import com.miui.server.sptm.PreLoadStrategy;
import com.miui.server.sptm.SpeedTestModeServiceImpl;
/* loaded from: classes.dex */
public class GreezeStrategy implements SpeedTestModeServiceImpl.Strategy {
    @Override // com.miui.server.sptm.SpeedTestModeServiceImpl.Strategy
    public void onNewEvent(int eventType) {
    }

    @Override // com.miui.server.sptm.SpeedTestModeServiceImpl.Strategy
    public void onAppStarted(PreLoadStrategy.AppStartRecord r) {
    }

    @Override // com.miui.server.sptm.SpeedTestModeServiceImpl.Strategy
    public void onSpeedTestModeChanged(boolean isEnable) {
    }
}
