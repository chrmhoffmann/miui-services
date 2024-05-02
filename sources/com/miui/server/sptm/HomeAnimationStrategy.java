package com.miui.server.sptm;

import android.content.Context;
import android.provider.Settings;
import android.util.Slog;
import com.miui.server.sptm.PreLoadStrategy;
import com.miui.server.sptm.SpeedTestModeServiceImpl;
import java.util.HashSet;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class HomeAnimationStrategy implements SpeedTestModeServiceImpl.Strategy {
    private static final double HOME_ANIMATION_RATIO_DELTA = 0.25d;
    private static final String KEY_ANIMATION_RATIO = "transition_animation_duration_ratio";
    private static final double MAX_HOME_ANIMATION_RATIO = 1.0d;
    private static final double MIN_HOME_ANIMATION_RATIO = 0.6000000238418579d;
    private Context mContext;
    private SpeedTestModeServiceImpl mSpeedTestModeService = SpeedTestModeServiceImpl.getInstance();
    private double mCurHomeAnimationRatio = MAX_HOME_ANIMATION_RATIO;
    private volatile int mContinuesSPTCount = 0;
    private int mAppStartedCountInSPTMode = 0;
    private boolean mIsInSpeedTestMode = false;
    private HashSet<String> mStartedAppInLastRound = new HashSet<>();

    public HomeAnimationStrategy(Context context) {
        this.mContext = context;
        setAnimationRatio(Double.valueOf((double) MAX_HOME_ANIMATION_RATIO));
    }

    @Override // com.miui.server.sptm.SpeedTestModeServiceImpl.Strategy
    public void onNewEvent(int eventType) {
        if (eventType == 5 && this.mContinuesSPTCount != 0) {
            setAnimationRatio(Double.valueOf((double) MAX_HOME_ANIMATION_RATIO));
            this.mContinuesSPTCount = 0;
        }
    }

    @Override // com.miui.server.sptm.SpeedTestModeServiceImpl.Strategy
    public void onAppStarted(PreLoadStrategy.AppStartRecord r) {
        if (this.mIsInSpeedTestMode) {
            int i = this.mAppStartedCountInSPTMode + 1;
            this.mAppStartedCountInSPTMode = i;
            double limitHomeAnimationRatio = limitHomeAnimationRatio(MAX_HOME_ANIMATION_RATIO - ((i + this.mContinuesSPTCount) * HOME_ANIMATION_RATIO_DELTA));
            this.mCurHomeAnimationRatio = limitHomeAnimationRatio;
            setAnimationRatio(Double.valueOf(limitHomeAnimationRatio));
        }
        if (r.isColdStart && SpeedTestModeServiceImpl.SPEED_TEST_APP_LIST.contains(r.packageName)) {
            this.mStartedAppInLastRound.add(r.packageName);
        }
    }

    @Override // com.miui.server.sptm.SpeedTestModeServiceImpl.Strategy
    public void onSpeedTestModeChanged(boolean isEnable) {
        this.mAppStartedCountInSPTMode = 0;
        this.mIsInSpeedTestMode = isEnable;
        if (!isEnable) {
            if (this.mStartedAppInLastRound.size() >= 14) {
                this.mContinuesSPTCount++;
            } else {
                this.mContinuesSPTCount = 0;
            }
            if (SpeedTestModeServiceImpl.DEBUG) {
                Slog.d(SpeedTestModeServiceImpl.TAG, String.format("In last round, started: %s, curRound: %s", Integer.valueOf(this.mStartedAppInLastRound.size()), Integer.valueOf(this.mContinuesSPTCount)));
            }
            this.mStartedAppInLastRound.clear();
            double limitHomeAnimationRatio = limitHomeAnimationRatio(MAX_HOME_ANIMATION_RATIO - (this.mContinuesSPTCount * HOME_ANIMATION_RATIO_DELTA));
            this.mCurHomeAnimationRatio = limitHomeAnimationRatio;
            setAnimationRatio(Double.valueOf(limitHomeAnimationRatio));
        }
    }

    public float getWindowAnimatorDurationOverride() {
        if (this.mContinuesSPTCount != 0 || this.mIsInSpeedTestMode) {
            return 0.3f;
        }
        return 1.0f;
    }

    private static double limitHomeAnimationRatio(double radio) {
        return Math.min((double) MAX_HOME_ANIMATION_RATIO, Math.max((double) MIN_HOME_ANIMATION_RATIO, radio));
    }

    private void setAnimationRatio(Double value) {
        if (this.mSpeedTestModeService.getAnimationCloudEnable()) {
            Settings.Global.putString(this.mContext.getContentResolver(), KEY_ANIMATION_RATIO, String.valueOf(value));
        } else {
            Settings.Global.putString(this.mContext.getContentResolver(), KEY_ANIMATION_RATIO, String.valueOf((double) MAX_HOME_ANIMATION_RATIO));
        }
    }
}
