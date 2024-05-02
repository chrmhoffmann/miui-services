package miui.android.animation.internal;

import miui.android.animation.IAnimTarget;
import miui.android.animation.base.AnimConfigLink;
import miui.android.animation.controller.AnimState;
import miui.android.animation.internal.TransitionInfo;
import miui.android.animation.listener.UpdateInfo;
import miui.android.animation.property.FloatProperty;
/* loaded from: classes.dex */
public class PredictTask {
    private static final TransitionInfo.IUpdateInfoCreator sCreator = new TransitionInfo.IUpdateInfoCreator() { // from class: miui.android.animation.internal.PredictTask.1
        @Override // miui.android.animation.internal.TransitionInfo.IUpdateInfoCreator
        public UpdateInfo getUpdateInfo(FloatProperty property) {
            return new UpdateInfo(property);
        }
    };

    public static long predictDuration(IAnimTarget target, AnimState from, AnimState to, AnimConfigLink configLink) {
        TransitionInfo transInfo = new TransitionInfo(target, from, to, configLink);
        transInfo.initUpdateList(sCreator);
        transInfo.setupTasks(true);
        long deltaT = AnimRunner.getInst().getAverageDelta();
        long totalT = deltaT;
        while (true) {
            for (AnimTask task : transInfo.animTasks) {
                AnimRunnerTask.doAnimationFrame(task, totalT, deltaT, false, true);
            }
            AnimStats stats = transInfo.getAnimStats();
            if (stats.isRunning()) {
                totalT += deltaT;
            } else {
                return totalT;
            }
        }
    }
}
