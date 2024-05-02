package miui.android.animation.internal;

import miui.android.animation.base.AnimConfig;
import miui.android.animation.base.AnimSpecialConfig;
import miui.android.animation.listener.UpdateInfo;
import miui.android.animation.property.FloatProperty;
import miui.android.animation.utils.EaseManager;
/* loaded from: classes.dex */
public class AnimData {
    public long delay;
    public EaseManager.EaseStyle ease;
    public int frameCount;
    public long initTime;
    public boolean isCompleted;
    boolean justEnd;
    boolean logEnabled;
    public byte op;
    public double progress;
    public FloatProperty property;
    public long startTime;
    public int tintMode;
    public double velocity;
    public double startValue = Double.MAX_VALUE;
    public double targetValue = Double.MAX_VALUE;
    public double value = Double.MAX_VALUE;

    public void reset() {
        this.isCompleted = false;
        this.frameCount = 0;
        this.justEnd = false;
    }

    public void setOp(byte op) {
        this.op = op;
        this.isCompleted = op == 0 || op > 2;
    }

    public void from(UpdateInfo update, AnimConfig config, AnimSpecialConfig sc) {
        this.property = update.property;
        this.velocity = update.velocity;
        this.frameCount = update.frameCount;
        this.op = update.animInfo.op;
        this.initTime = update.animInfo.initTime;
        this.startTime = update.animInfo.startTime;
        this.progress = update.animInfo.progress;
        this.startValue = update.animInfo.startValue;
        this.targetValue = update.animInfo.targetValue;
        this.value = update.animInfo.value;
        this.isCompleted = update.isCompleted;
        this.justEnd = update.animInfo.justEnd;
        this.tintMode = AnimConfigUtils.getTintMode(config, sc);
        this.ease = AnimConfigUtils.getEase(config, sc);
        this.delay = AnimConfigUtils.getDelay(config, sc);
    }

    public void to(UpdateInfo update) {
        update.frameCount = this.frameCount;
        update.animInfo.op = this.op;
        update.animInfo.delay = this.delay;
        update.animInfo.tintMode = this.tintMode;
        update.animInfo.initTime = this.initTime;
        update.animInfo.startTime = this.startTime;
        update.animInfo.progress = this.progress;
        update.animInfo.startValue = this.startValue;
        update.animInfo.targetValue = this.targetValue;
        update.isCompleted = this.isCompleted;
        update.animInfo.value = this.value;
        update.velocity = this.velocity;
        update.animInfo.justEnd = this.justEnd;
        clear();
    }

    void clear() {
        this.property = null;
        this.ease = null;
    }
}
