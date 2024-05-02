package miui.android.animation.internal;

import android.animation.FloatEvaluator;
import android.animation.IntEvaluator;
import android.animation.TypeEvaluator;
import com.android.server.wm.MiuiFreeformPinManagerService;
import java.util.List;
import miui.android.animation.ViewTarget;
import miui.android.animation.base.AnimSpecialConfig;
import miui.android.animation.listener.UpdateInfo;
import miui.android.animation.property.ColorProperty;
import miui.android.animation.property.FloatProperty;
import miui.android.animation.property.IIntValueProperty;
import miui.android.animation.property.ViewPropertyExt;
import miui.android.animation.styles.PropertyStyle;
import miui.android.animation.utils.CommonUtils;
import miui.android.animation.utils.EaseManager;
import miui.android.animation.utils.LogUtils;
/* loaded from: classes.dex */
public class AnimRunnerTask {
    static final ThreadLocal<AnimData> animDataLocal = new ThreadLocal<>();

    AnimRunnerTask() {
    }

    public static void doAnimationFrame(AnimTask animTask, long totalT, long deltaT, boolean updateTarget, boolean toPage) {
        int idx;
        int n;
        UpdateInfo update;
        boolean isViewTarget;
        AnimData data = (AnimData) CommonUtils.getLocal(animDataLocal, AnimData.class);
        data.logEnabled = LogUtils.isLogEnabled();
        long averageDelta = AnimRunner.getInst().getAverageDelta();
        for (AnimTask task = animTask; task != null; task = task.remove()) {
            task.animStats.updateCount = 0;
            boolean needSetup = !task.animStats.isStarted();
            List<UpdateInfo> updateList = task.info.updateList;
            boolean isViewTarget2 = task.info.target instanceof ViewTarget;
            int idx2 = task.startPos;
            int n2 = idx2 + task.getAnimCount();
            int idx3 = idx2;
            while (idx3 < n2) {
                UpdateInfo update2 = updateList.get(idx3);
                AnimSpecialConfig sc = task.info.config.getSpecialConfig(update2.property.getName());
                data.from(update2, task.info.config, sc);
                if (!needSetup) {
                    update = update2;
                    n = n2;
                    idx = idx3;
                } else {
                    update = update2;
                    n = n2;
                    idx = idx3;
                    setup(task, data, task.info, sc, totalT, deltaT);
                }
                if (data.op == 1) {
                    startAnim(task, data, task.info, totalT, deltaT);
                }
                if (data.op != 2) {
                    isViewTarget = isViewTarget2;
                } else {
                    isViewTarget = isViewTarget2;
                    updateAnimation(task, data, task.info, totalT, deltaT, averageDelta);
                }
                UpdateInfo update3 = update;
                data.to(update3);
                if (updateTarget && toPage && !isViewTarget && !AnimValueUtils.isInvalid(data.value)) {
                    update3.setTargetValue(task.info.target);
                }
                idx3 = idx + 1;
                n2 = n;
                isViewTarget2 = isViewTarget;
            }
        }
    }

    static void setup(AnimTask task, AnimData data, TransitionInfo info, AnimSpecialConfig sc, long totalT, long deltaT) {
        if (AnimValueUtils.isInvalid(data.startValue)) {
            double startValue = data.startValue;
            data.startValue = AnimValueUtils.getValue(info.target, data.property, startValue);
        }
        data.initTime = totalT - deltaT;
        task.animStats.initCount++;
        if (data.op != 2 || data.delay > 0) {
            data.setOp((byte) 1);
            float fromSpeed = AnimConfigUtils.getFromSpeed(info.config, sc);
            if (fromSpeed != Float.MAX_VALUE) {
                data.velocity = fromSpeed;
                return;
            }
            return;
        }
        data.startTime = totalT - deltaT;
        data.delay = 0L;
        task.animStats.startCount--;
        setStartData(task, data);
    }

    static void startAnim(AnimTask task, AnimData data, TransitionInfo info, long totalT, long deltaT) {
        if (data.delay > 0) {
            if (data.logEnabled) {
                LogUtils.debug("StartTask, tag = " + task.info.key + ", property = " + data.property.getName() + ", delay = " + data.delay + ", initTime = " + data.initTime + ", totalT = " + totalT, new Object[0]);
            }
            if (totalT < data.initTime + data.delay) {
                return;
            }
            double startValue = AnimValueUtils.getValue(info.target, data.property, Double.MAX_VALUE);
            if (startValue != Double.MAX_VALUE) {
                data.startValue = startValue;
            }
        }
        AnimStats animStats = task.animStats;
        animStats.startCount--;
        if (!initAnimation(task, data, totalT, deltaT)) {
            return;
        }
        setStartData(task, data);
    }

    private static void setStartData(AnimTask task, AnimData data) {
        data.progress = 0.0d;
        data.reset();
        if (data.logEnabled) {
            LogUtils.debug("+++++ start anim, target = " + task.info.target + ", tag = " + task.info.key + ", property = " + data.property.getName() + ", op = " + ((int) data.op) + ", ease = " + data.ease + ", delay = " + data.delay + ", start value = " + data.startValue + ", target value = " + data.targetValue + ", value = " + data.value + ", progress = " + data.progress + ", velocity = " + data.velocity, new Object[0]);
        }
    }

    private static boolean initAnimation(AnimTask task, AnimData data, long totalT, long deltaT) {
        if (!setValues(data)) {
            if (data.logEnabled) {
                LogUtils.logThread(CommonUtils.TAG, "StartTask, set start value failed, break, tag = " + task.info.key + ", property = " + data.property.getName() + ", start value = " + data.startValue + ", target value = " + data.targetValue + ", value = " + data.value);
            }
            finishProperty(task, data);
            return false;
        } else if (isValueInvalid(data)) {
            if (data.logEnabled) {
                LogUtils.logThread(CommonUtils.TAG, "StartTask, values invalid, break, tag = " + task.info.key + ", property = " + data.property.getName() + ", startValue = " + data.startValue + ", targetValue = " + data.targetValue + ", value = " + data.value + ", velocity = " + data.velocity);
            }
            data.reset();
            finishProperty(task, data);
            return false;
        } else {
            data.startTime = totalT - deltaT;
            data.frameCount = 0;
            data.setOp((byte) 2);
            return true;
        }
    }

    private static boolean setValues(AnimData data) {
        if (!AnimValueUtils.isInvalid(data.value)) {
            if (AnimValueUtils.isInvalid(data.startValue)) {
                data.startValue = data.value;
            }
            return true;
        } else if (!AnimValueUtils.isInvalid(data.startValue)) {
            data.value = data.startValue;
            return true;
        } else {
            return false;
        }
    }

    private static void finishProperty(AnimTask task, AnimData data) {
        data.setOp((byte) 5);
        task.animStats.failCount++;
    }

    private static boolean isValueInvalid(AnimData data) {
        return data.startValue == data.targetValue && Math.abs(data.velocity) < 16.66666603088379d;
    }

    private static void updateAnimation(AnimTask task, AnimData data, TransitionInfo info, long totalT, long deltaT, long averageDelta) {
        task.animStats.updateCount++;
        data.frameCount++;
        if (data.property == ViewPropertyExt.FOREGROUND || data.property == ViewPropertyExt.BACKGROUND || (data.property instanceof ColorProperty)) {
            double startValue = data.startValue;
            double targetValue = data.targetValue;
            data.startValue = 0.0d;
            data.targetValue = 1.0d;
            data.value = data.progress;
            PropertyStyle.doAnimationFrame(info.target, data, totalT, deltaT, averageDelta);
            data.progress = regulateProgress((float) data.value);
            data.startValue = startValue;
            data.targetValue = targetValue;
            Integer animValue = (Integer) CommonUtils.sArgbEvaluator.evaluate((float) data.progress, Integer.valueOf((int) data.startValue), Integer.valueOf((int) data.targetValue));
            data.value = animValue.doubleValue();
        } else {
            PropertyStyle.doAnimationFrame(info.target, data, totalT, deltaT, averageDelta);
            if (!EaseManager.isPhysicsStyle(data.ease.style)) {
                data.value = evaluateValue(data, (float) data.progress);
            }
        }
        if (data.op == 3) {
            data.justEnd = true;
            task.animStats.endCount++;
        }
        if (data.logEnabled) {
            LogUtils.debug("----- update anim, target = " + task.info.target + ", tag = " + task.info.key + ", property = " + data.property.getName() + ", op = " + ((int) data.op) + ", init time = " + data.initTime + ", start time = " + data.startTime + ", start value = " + data.startValue + ", target value = " + data.targetValue + ", value = " + data.value + ", progress = " + data.progress + ", velocity = " + data.velocity + ", delta = " + deltaT, new Object[0]);
        }
    }

    private static float regulateProgress(float progress) {
        if (progress > 1.0f) {
            return 1.0f;
        }
        if (progress < MiuiFreeformPinManagerService.EDGE_AREA) {
            return MiuiFreeformPinManagerService.EDGE_AREA;
        }
        return progress;
    }

    private static double evaluateValue(AnimData data, float progress) {
        TypeEvaluator evaluator = getEvaluator(data.property);
        if (evaluator instanceof IntEvaluator) {
            Integer value = ((IntEvaluator) evaluator).evaluate(progress, Integer.valueOf((int) data.startValue), Integer.valueOf((int) data.targetValue));
            return value.doubleValue();
        }
        Float value2 = ((FloatEvaluator) evaluator).evaluate(progress, (Number) Float.valueOf((float) data.startValue), (Number) Float.valueOf((float) data.targetValue));
        return value2.doubleValue();
    }

    private static TypeEvaluator getEvaluator(FloatProperty property) {
        if (property == ViewPropertyExt.BACKGROUND && (property instanceof ColorProperty)) {
            return CommonUtils.sArgbEvaluator;
        }
        if (property instanceof IIntValueProperty) {
            return new IntEvaluator();
        }
        return new FloatEvaluator();
    }
}
