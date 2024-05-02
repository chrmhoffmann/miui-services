package miui.android.animation.internal;

import android.util.Log;
import java.util.concurrent.atomic.AtomicInteger;
import miui.android.animation.listener.UpdateInfo;
import miui.android.animation.utils.CommonUtils;
import miui.android.animation.utils.LinkNode;
/* loaded from: classes.dex */
public class AnimTask extends LinkNode<AnimTask> implements Runnable {
    public static final int MAX_PAGE_SIZE = 150;
    public static final int MAX_SINGLE_TASK_SIZE = 4000;
    public static final int MAX_TO_PAGE_SIZE = 500;
    public static final byte OP_CANCEL = 4;
    public static final byte OP_END = 3;
    public static final byte OP_FAILED = 5;
    public static final byte OP_INVALID = 0;
    public static final byte OP_START = 1;
    public static final byte OP_UPDATE = 2;
    public static final AtomicInteger sTaskCount = new AtomicInteger();
    public final AnimStats animStats = new AnimStats();
    public volatile long deltaT;
    public volatile TransitionInfo info;
    public volatile int startPos;
    public volatile boolean toPage;
    public volatile long totalT;

    public static boolean isRunning(byte op) {
        return op == 1 || op == 2;
    }

    public void setup(int pos, int amount) {
        this.animStats.clear();
        this.animStats.animCount = amount;
        this.startPos = pos;
    }

    public void start(long totalT, long deltaT, boolean toPage) {
        this.totalT = totalT;
        this.deltaT = deltaT;
        this.toPage = toPage;
        ThreadPoolUtil.post(this);
    }

    public int getAnimCount() {
        return this.animStats.animCount;
    }

    public int getTotalAnimCount() {
        int count = 0;
        for (AnimTask task = this; task != null; task = (AnimTask) task.next) {
            count += task.animStats.animCount;
        }
        return count;
    }

    public void updateAnimStats() {
        int n = this.startPos + this.animStats.animCount;
        for (int i = this.startPos; i < n; i++) {
            UpdateInfo update = this.info.updateList.get(i);
            if (update.animInfo.op == 0 || update.animInfo.op == 1) {
                this.animStats.startCount++;
            } else {
                this.animStats.initCount++;
                switch (update.animInfo.op) {
                    case 3:
                        this.animStats.endCount++;
                        continue;
                    case 4:
                        this.animStats.cancelCount++;
                        continue;
                    case 5:
                        this.animStats.failCount++;
                        continue;
                }
            }
        }
    }

    @Override // java.lang.Runnable
    public void run() {
        try {
            AnimRunnerTask.doAnimationFrame(this, this.totalT, this.deltaT, true, this.toPage);
        } catch (Exception e) {
            Log.d(CommonUtils.TAG, "doAnimationFrame failed", e);
        }
        if (sTaskCount.decrementAndGet() == 0) {
            AnimRunner.sRunnerHandler.sendEmptyMessage(2);
        }
    }
}
