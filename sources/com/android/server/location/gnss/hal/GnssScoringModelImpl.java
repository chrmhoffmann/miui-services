package com.android.server.location.gnss.hal;

import android.os.SystemClock;
import com.android.server.wm.MiuiFreeformPinManagerService;
import com.miui.base.MiuiStubRegistry;
/* loaded from: classes.dex */
public class GnssScoringModelImpl implements GnssScoringModelStub {
    private static final long IGNORE_RUNNING_TIME = 300000;
    private static final int NO_FIELD_SCORE = 4;
    private static final long NO_FIELD_TIME = 19000;
    private static final String TAG = "GnssScoringModel";
    private static final long WEEK_FIELD_TIME = 61000;
    private final GpoUtil mGpoUtil = GpoUtil.getInstance();
    private long mStartTime = 0;
    private long mLastLocationTime = 0;
    private boolean mFeatureSwitch = false;
    private boolean mModelRunning = false;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<GnssScoringModelImpl> {

        /* compiled from: GnssScoringModelImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final GnssScoringModelImpl INSTANCE = new GnssScoringModelImpl();
        }

        public GnssScoringModelImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public GnssScoringModelImpl provideNewInstance() {
            return new GnssScoringModelImpl();
        }
    }

    public void init(boolean state) {
        this.mFeatureSwitch = state;
        this.mGpoUtil.logi(TAG, "set GnssScoringModel running: " + state, true);
    }

    public void startScoringModel(boolean on) {
        if (!this.mFeatureSwitch) {
            return;
        }
        this.mGpoUtil.logv(TAG, "start Scoring Model ? " + on);
        boolean z = on && (this.mLastLocationTime == 0 || SystemClock.elapsedRealtime() - this.mLastLocationTime > IGNORE_RUNNING_TIME);
        this.mModelRunning = z;
        if (z) {
            this.mStartTime = SystemClock.elapsedRealtime();
        }
    }

    public void updateFixTime(long time) {
        if (!this.mFeatureSwitch) {
            return;
        }
        this.mGpoUtil.logv(TAG, "update gnss fix time.");
        this.mModelRunning = false;
        this.mLastLocationTime = time;
    }

    public void reportSvStatus2Score(float[] basebandCn0DbHzs) {
        if (!this.mFeatureSwitch || !this.mModelRunning) {
            return;
        }
        int sum = 0;
        int length = basebandCn0DbHzs.length;
        for (int i = 0; i < length; i++) {
            float cn0 = basebandCn0DbHzs[i];
            sum += cn0 > MiuiFreeformPinManagerService.EDGE_AREA ? 1 : 0;
        }
        switchEngineStateWithScore(sum * 2, SystemClock.elapsedRealtime() - this.mStartTime);
    }

    private void switchEngineStateWithScore(int score, long time) {
        if (time >= setLimitTime(score) && this.mGpoUtil.getEngineStatus() == 2) {
            this.mGpoUtil.doStopEngineByInstance();
            this.mGpoUtil.logi(TAG, "Gnss Score: " + score + ", using time: " + (time / 1000), true);
        }
    }

    private long setLimitTime(int score) {
        long limitTime;
        this.mGpoUtil.logv(TAG, "The score is : " + score);
        if (score <= 4) {
            limitTime = NO_FIELD_TIME;
        } else {
            limitTime = WEEK_FIELD_TIME;
        }
        this.mGpoUtil.logv(TAG, "The Limit time is :  " + limitTime);
        return limitTime;
    }
}
