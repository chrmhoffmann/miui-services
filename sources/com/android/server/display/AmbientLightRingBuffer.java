package com.android.server.display;

import android.os.SystemClock;
import com.android.server.wm.MiuiFreeformPinManagerService;
/* loaded from: classes.dex */
public class AmbientLightRingBuffer {
    private static final long AMBIENT_LIGHT_PREDICTION_TIME_MILLIS = 100;
    private static final float BUFFER_SLACK = 1.5f;
    private long mAssistDarkeningDelayTime;
    private long mBrighteningLightDebounceConfig;
    private int mCapacity;
    private int mCount;
    private long mDarkeningLightDebounceConfig;
    private int mEnd;
    private float[] mRingLux;
    private long[] mRingTime;
    private int mStart;
    private final int mWeightingIntercept;

    public AmbientLightRingBuffer(long lightSensorRate, int ambientLightHorizon) {
        int ceil = (int) Math.ceil((ambientLightHorizon * BUFFER_SLACK) / ((float) lightSensorRate));
        this.mCapacity = ceil;
        this.mRingLux = new float[ceil];
        this.mRingTime = new long[ceil];
        this.mWeightingIntercept = ambientLightHorizon;
    }

    public float getLux(int index) {
        return this.mRingLux[offsetOf(index)];
    }

    public long getTime(int index) {
        return this.mRingTime[offsetOf(index)];
    }

    public void push(long time, float lux) {
        int next = this.mEnd;
        int i = this.mCount;
        int i2 = this.mCapacity;
        if (i == i2) {
            int newSize = i2 * 2;
            float[] newRingLux = new float[newSize];
            long[] newRingTime = new long[newSize];
            int i3 = this.mStart;
            int length = i2 - i3;
            System.arraycopy(this.mRingLux, i3, newRingLux, 0, length);
            System.arraycopy(this.mRingTime, this.mStart, newRingTime, 0, length);
            int i4 = this.mStart;
            if (i4 != 0) {
                System.arraycopy(this.mRingLux, 0, newRingLux, length, i4);
                System.arraycopy(this.mRingTime, 0, newRingTime, length, this.mStart);
            }
            this.mRingLux = newRingLux;
            this.mRingTime = newRingTime;
            next = this.mCapacity;
            this.mCapacity = newSize;
            this.mStart = 0;
        }
        this.mRingTime[next] = time;
        this.mRingLux[next] = lux;
        int i5 = next + 1;
        this.mEnd = i5;
        if (i5 == this.mCapacity) {
            this.mEnd = 0;
        }
        this.mCount++;
    }

    public void prune(long horizon) {
        if (this.mCount == 0) {
            return;
        }
        while (true) {
            int i = this.mCount;
            if (i <= 1) {
                break;
            }
            int next = this.mStart + 1;
            int i2 = this.mCapacity;
            if (next >= i2) {
                next -= i2;
            }
            if (this.mRingTime[next] > horizon) {
                break;
            }
            this.mStart = next;
            this.mCount = i - 1;
        }
        long[] jArr = this.mRingTime;
        int i3 = this.mStart;
        if (jArr[i3] < horizon) {
            jArr[i3] = horizon;
        }
    }

    public int size() {
        return this.mCount;
    }

    public void clear() {
        this.mStart = 0;
        this.mEnd = 0;
        this.mCount = 0;
    }

    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append('[');
        int i = 0;
        while (true) {
            int i2 = this.mCount;
            if (i < i2) {
                long next = i + 1 < i2 ? getTime(i + 1) : SystemClock.uptimeMillis();
                if (i != 0) {
                    buf.append(", ");
                }
                buf.append(getLux(i));
                buf.append(" / ");
                buf.append(next - getTime(i));
                buf.append("ms");
                i++;
            } else {
                buf.append(']');
                return buf.toString();
            }
        }
    }

    private int offsetOf(int index) {
        if (index >= this.mCount || index < 0) {
            throw new ArrayIndexOutOfBoundsException(index);
        }
        int index2 = index + this.mStart;
        int i = this.mCapacity;
        if (index2 >= i) {
            return index2 - i;
        }
        return index2;
    }

    public float calculateAmbientLux(long now, long horizon) {
        int N = size();
        if (N == 0) {
            return -1.0f;
        }
        int endIndex = 0;
        long horizonStartTime = now - horizon;
        for (int i = 0; i < N - 1 && getTime(i + 1) <= horizonStartTime; i++) {
            endIndex++;
        }
        float sum = MiuiFreeformPinManagerService.EDGE_AREA;
        float totalWeight = MiuiFreeformPinManagerService.EDGE_AREA;
        long endTime = AMBIENT_LIGHT_PREDICTION_TIME_MILLIS;
        for (int i2 = N - 1; i2 >= endIndex; i2--) {
            long eventTime = getTime(i2);
            if (i2 == endIndex && eventTime < horizonStartTime) {
                eventTime = horizonStartTime;
            }
            long startTime = eventTime - now;
            float weight = calculateWeight(startTime, endTime);
            float lux = getLux(i2);
            totalWeight += weight;
            sum += lux * weight;
            endTime = startTime;
        }
        return sum / totalWeight;
    }

    private float calculateWeight(long startDelta, long endDelta) {
        return weightIntegral(endDelta) - weightIntegral(startDelta);
    }

    private float weightIntegral(long x) {
        return ((float) x) * ((((float) x) * 0.5f) + this.mWeightingIntercept);
    }

    public long nextAmbientLightBrighteningTransition(long time, float brighteningThreshold) {
        int N = size();
        long earliestValidTime = time;
        for (int i = N - 1; i >= 0 && getLux(i) > brighteningThreshold; i--) {
            earliestValidTime = getTime(i);
        }
        return this.mBrighteningLightDebounceConfig + earliestValidTime;
    }

    public long nextAmbientLightDarkeningTransition(long time, float darkeningThreshold) {
        int N = size();
        long earliestValidTime = time;
        for (int i = N - 1; i >= 0 && getLux(i) < darkeningThreshold; i--) {
            earliestValidTime = getTime(i);
        }
        return this.mDarkeningLightDebounceConfig + earliestValidTime + this.mAssistDarkeningDelayTime;
    }

    public void setBrighteningDebounce(long brighteningDebounce) {
        this.mBrighteningLightDebounceConfig = brighteningDebounce;
    }

    public void setDarkeningDebounce(long darkeningDebounce) {
        this.mDarkeningLightDebounceConfig = darkeningDebounce;
    }

    public void adjustAssistDarkeningDelayTime(long time) {
        this.mAssistDarkeningDelayTime = Math.max(0L, time);
    }

    public long getAssistDarkeningDelayTime() {
        return this.mAssistDarkeningDelayTime;
    }
}
