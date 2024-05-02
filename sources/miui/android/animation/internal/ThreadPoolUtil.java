package miui.android.animation.internal;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
/* loaded from: classes.dex */
public class ThreadPoolUtil {
    private static final int CPU_COUNT;
    private static final int KEEP_ALIVE = 30;
    private static final int KEEP_POOL_SIZE;
    public static final int MAX_SPLIT_COUNT;
    private static final ThreadPoolExecutor sCacheThread;
    private static final Executor sSingleThread;

    static {
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        CPU_COUNT = availableProcessors;
        int i = (availableProcessors * 2) + 1;
        MAX_SPLIT_COUNT = i;
        int i2 = availableProcessors < 4 ? 0 : (availableProcessors / 2) + 1;
        KEEP_POOL_SIZE = i2;
        sCacheThread = new ThreadPoolExecutor(i2, i + 3, 30L, TimeUnit.SECONDS, new SynchronousQueue(), getThreadFactory("AnimThread"), new RejectedExecutionHandler() { // from class: miui.android.animation.internal.ThreadPoolUtil.1
            @Override // java.util.concurrent.RejectedExecutionHandler
            public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                ThreadPoolUtil.sSingleThread.execute(r);
            }
        });
        sSingleThread = Executors.newSingleThreadExecutor(getThreadFactory("WorkThread"));
    }

    private static ThreadFactory getThreadFactory(final String factoryName) {
        return new ThreadFactory() { // from class: miui.android.animation.internal.ThreadPoolUtil.2
            final AtomicInteger threadNumber = new AtomicInteger(1);

            @Override // java.util.concurrent.ThreadFactory
            public Thread newThread(Runnable runnable) {
                Thread thread = new Thread(runnable, factoryName + "-" + this.threadNumber.getAndIncrement());
                thread.setPriority(5);
                return thread;
            }
        };
    }

    public static void post(Runnable task) {
        sCacheThread.execute(task);
    }

    public static void getSplitCount(int animCount, int[] splitInfo) {
        int splitCount = Math.max(animCount / AnimTask.MAX_SINGLE_TASK_SIZE, 1);
        if (splitCount > MAX_SPLIT_COUNT) {
            splitCount = MAX_SPLIT_COUNT;
        }
        int singleCount = (int) Math.ceil(animCount / splitCount);
        splitInfo[0] = splitCount;
        splitInfo[1] = singleCount;
    }
}
