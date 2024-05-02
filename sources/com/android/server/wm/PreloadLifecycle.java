package com.android.server.wm;

import com.android.server.am.ActivityManagerServiceImpl;
import com.android.server.am.PreloadAppControllerImpl;
import miui.process.LifecycleConfig;
/* loaded from: classes.dex */
public class PreloadLifecycle {
    public static final int NO_PRELOAD = 0;
    public static final int ON_FREEZE = 6;
    public static final int ON_STOP = 4;
    public static final int PRELOAD_SOON = 2;
    public static final int PRE_FREEZE = 5;
    public static final int PRE_KILL = 7;
    public static final int PRE_STOP = 3;
    public static final int START_PRELOAD = 1;
    protected boolean mAlreadyPreloaded;
    protected LifecycleConfig mConfig;
    private int mDisplayId;
    protected long mFreezeTimeout;
    protected boolean mIgnoreMemory;
    protected long mKillTimeout;
    protected String mPackageName;
    protected long mStopTimeout;
    protected int mUid;

    public PreloadLifecycle(String packageName) {
        this(false, packageName, null);
    }

    public PreloadLifecycle(boolean ignoreMemory, String packageName, LifecycleConfig config) {
        this.mIgnoreMemory = ignoreMemory;
        this.mPackageName = packageName;
        this.mConfig = config;
        initTimeout(config);
    }

    public void initTimeout(LifecycleConfig config) {
        this.mStopTimeout = config.getStopTimeout();
        this.mKillTimeout = config.getKillTimeout();
        this.mFreezeTimeout = this.mStopTimeout + 500;
    }

    public long getPreloadNextTimeout() {
        return Math.max(this.mFreezeTimeout, (long) ActivityManagerServiceImpl.BOOST_DURATION);
    }

    public long getStopTimeout() {
        return this.mStopTimeout;
    }

    public void setStopTimeout(long mStopTimeout) {
        this.mStopTimeout = mStopTimeout;
    }

    public long getFreezeTimeout() {
        return this.mFreezeTimeout;
    }

    public void setFreezeTimeout(long mFreezeTimeout) {
        this.mFreezeTimeout = mFreezeTimeout;
    }

    public long getKillTimeout() {
        return this.mKillTimeout;
    }

    public void setKillTimeout(long mKillTimeout) {
        this.mKillTimeout = mKillTimeout;
    }

    public int getUid() {
        return this.mUid;
    }

    public void setUid(int mUid) {
        this.mUid = mUid;
    }

    public String getPackageName() {
        return this.mPackageName;
    }

    public void setPackageName(String mPackageName) {
        this.mPackageName = mPackageName;
    }

    public int[] getPreloadSchedAffinity() {
        if (this.mConfig.getSchedAffinity() == null || this.mConfig.getSchedAffinity().length == 0) {
            return PreloadAppControllerImpl.sPreloadSchedAffinity;
        }
        return this.mConfig.getSchedAffinity();
    }

    public boolean isIgnoreMemory() {
        return this.mIgnoreMemory;
    }

    public void setIgnoreMemory(boolean ignoreMemory) {
        this.mIgnoreMemory = ignoreMemory;
    }

    public boolean isAlreadyPreloaded() {
        return this.mAlreadyPreloaded;
    }

    public void setAlreadyPreloaded(boolean alreadyPreloaded) {
        this.mAlreadyPreloaded = alreadyPreloaded;
    }

    public LifecycleConfig getConfig() {
        return this.mConfig;
    }

    public void setConfig(LifecycleConfig mConfig) {
        this.mConfig = mConfig;
    }

    public int getPreloadType() {
        LifecycleConfig lifecycleConfig = this.mConfig;
        if (lifecycleConfig == null) {
            return -1;
        }
        return lifecycleConfig.getType();
    }

    public int getDisplayId() {
        return this.mDisplayId;
    }

    public void setDisplayId(int displayId) {
        this.mDisplayId = displayId;
    }

    public boolean equals(Object obj) {
        String str;
        if (obj == null) {
            return false;
        }
        PreloadLifecycle lifecycle = (PreloadLifecycle) obj;
        if (this.mUid != lifecycle.mUid || (str = this.mPackageName) == null || !str.equals(lifecycle.mPackageName)) {
            return false;
        }
        return true;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("mPackageName=" + this.mPackageName);
        sb.append(",mStopTimeout=" + this.mStopTimeout);
        sb.append(",mKillTimeout=" + this.mKillTimeout);
        sb.append(",type=" + getPreloadType());
        return sb.toString();
    }
}
