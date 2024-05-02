package com.android.server.location;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
/* loaded from: classes.dex */
public class ThreadPoolUtil {
    private static ThreadPoolUtil mThreadPoolUtil;
    private int corePoolSize;
    private ThreadPoolExecutor executor;
    private int maximumPoolSize;
    private long keepAliveTime = 1;
    private TimeUnit unit = TimeUnit.HOURS;

    public static ThreadPoolUtil getInstance() {
        if (mThreadPoolUtil == null) {
            synchronized (ThreadPoolUtil.class) {
                if (mThreadPoolUtil == null) {
                    mThreadPoolUtil = new ThreadPoolUtil();
                }
            }
        }
        return mThreadPoolUtil;
    }

    private ThreadPoolUtil() {
        int availableProcessors = (Runtime.getRuntime().availableProcessors() * 2) + 1;
        this.corePoolSize = availableProcessors;
        this.maximumPoolSize = availableProcessors;
        initThreadPool();
    }

    private void initThreadPool() {
        this.executor = new ThreadPoolExecutor(this.corePoolSize, this.maximumPoolSize, this.keepAliveTime, this.unit, new LinkedBlockingQueue(), new DefaultThreadFactory(5, "mi-gnss-pool-"), new ThreadPoolExecutor.AbortPolicy());
    }

    public void execute(Runnable runnable) {
        if (runnable == null) {
            return;
        }
        if (this.executor == null) {
            initThreadPool();
        }
        this.executor.execute(runnable);
    }

    public void remove(Runnable runnable) {
        if (runnable == null) {
            return;
        }
        if (this.executor == null) {
            initThreadPool();
        }
        this.executor.remove(runnable);
    }

    public void setCorePoolSize(int corePoolSize) {
        this.corePoolSize = corePoolSize;
    }

    public void setMaximumPoolSize(int maximumPoolSize) {
        this.maximumPoolSize = maximumPoolSize;
    }

    public void setKeepAliveTime(long keepAliveTime) {
        this.keepAliveTime = keepAliveTime;
    }

    public void setUnit(TimeUnit unit) {
        this.unit = unit;
    }

    /* loaded from: classes.dex */
    public static class DefaultThreadFactory implements ThreadFactory {
        private static final AtomicInteger POOL_NUMBER = new AtomicInteger(1);
        private final ThreadGroup group;
        private final String namePrefix;
        private final AtomicInteger threadNumber;
        private final int threadPriority;

        private DefaultThreadFactory(int threadPriority, String threadNamePrefix) {
            this.threadNumber = new AtomicInteger(1);
            this.threadPriority = threadPriority;
            this.group = Thread.currentThread().getThreadGroup();
            this.namePrefix = threadNamePrefix + POOL_NUMBER.getAndIncrement() + "-thread-";
        }

        @Override // java.util.concurrent.ThreadFactory
        public Thread newThread(Runnable runnable) {
            Thread t = new Thread(this.group, runnable, this.namePrefix + this.threadNumber.getAndIncrement(), 0L);
            if (t.isDaemon()) {
                t.setDaemon(false);
            }
            t.setPriority(this.threadPriority);
            return t;
        }
    }
}
