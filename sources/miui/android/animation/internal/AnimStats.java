package miui.android.animation.internal;

import miui.android.animation.utils.ObjectPool;
/* loaded from: classes.dex */
class AnimStats implements ObjectPool.IPoolObject {
    public int animCount;
    public int cancelCount;
    public int endCount;
    public int failCount;
    public int initCount;
    public int startCount;
    public int updateCount;

    public void add(AnimStats stats) {
        this.animCount += stats.animCount;
        this.startCount += stats.startCount;
        this.initCount += stats.initCount;
        this.failCount += stats.failCount;
        this.updateCount += stats.updateCount;
        this.cancelCount += stats.cancelCount;
        this.endCount += stats.endCount;
    }

    public boolean isStarted() {
        return this.initCount > 0;
    }

    public boolean isRunning() {
        return !isStarted() || (this.cancelCount + this.endCount) + this.failCount < this.animCount;
    }

    @Override // miui.android.animation.utils.ObjectPool.IPoolObject
    public void clear() {
        this.animCount = 0;
        this.startCount = 0;
        this.initCount = 0;
        this.failCount = 0;
        this.updateCount = 0;
        this.cancelCount = 0;
        this.endCount = 0;
    }

    public String toString() {
        return "AnimStats{animCount = " + this.animCount + ", startCount=" + this.startCount + ", startedCount = " + this.initCount + ", failCount=" + this.failCount + ", updateCount=" + this.updateCount + ", cancelCount=" + this.cancelCount + ", endCount=" + this.endCount + '}';
    }
}
