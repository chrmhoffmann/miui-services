package com.android.server.wm;

import android.os.SystemClock;
import java.util.Arrays;
import java.util.LinkedList;
/* loaded from: classes.dex */
public class VelocityMonitor {
    private static final long MAX_DELTA = 100;
    private static final int MAX_RECORD_COUNT = 10;
    private static final long MIN_DELTA = 30;
    private static final long TIME_THRESHOLD = 200;
    private static final int UNIT_SECOND = 1000;
    private LinkedList<MoveRecord> mHistory = new LinkedList<>();
    private float[] mVelocity;

    /* loaded from: classes.dex */
    public static class MoveRecord {
        long timeStamp;
        double[] values;

        private MoveRecord() {
        }
    }

    public void update(float... value) {
        if (value == null || value.length == 0) {
            return;
        }
        MoveRecord record = getMoveRecord();
        record.values = new double[value.length];
        for (int i = 0; i < value.length; i++) {
            record.values[i] = value[i];
        }
        addAndUpdate(record);
    }

    public void update(double... value) {
        if (value == null || value.length == 0) {
            return;
        }
        MoveRecord record = getMoveRecord();
        record.values = value;
        addAndUpdate(record);
    }

    private MoveRecord getMoveRecord() {
        MoveRecord record = new MoveRecord();
        record.timeStamp = SystemClock.uptimeMillis();
        return record;
    }

    private void addAndUpdate(MoveRecord record) {
        this.mHistory.add(record);
        if (this.mHistory.size() > 10) {
            this.mHistory.remove(0);
        }
        updateVelocity();
    }

    public float getVelocity(int idx) {
        float[] fArr;
        long now = SystemClock.uptimeMillis();
        return ((this.mHistory.size() <= 0 || Math.abs(now - this.mHistory.getLast().timeStamp) <= 200) && (fArr = this.mVelocity) != null && fArr.length > idx) ? fArr[idx] : MiuiFreeformPinManagerService.EDGE_AREA;
    }

    public void clear() {
        this.mHistory.clear();
        clearVelocity();
    }

    private void clearVelocity() {
        float[] fArr = this.mVelocity;
        if (fArr != null) {
            Arrays.fill(fArr, (float) MiuiFreeformPinManagerService.EDGE_AREA);
        }
    }

    private void updateVelocity() {
        int size = this.mHistory.size();
        if (size >= 2) {
            MoveRecord lastRecord = this.mHistory.getLast();
            MoveRecord lastRecord1 = this.mHistory.get(size - 2);
            float[] fArr = this.mVelocity;
            if (fArr == null || fArr.length < lastRecord.values.length) {
                this.mVelocity = new float[lastRecord.values.length];
            }
            for (int i = 0; i < lastRecord.values.length; i++) {
                this.mVelocity[i] = calVelocity(i, lastRecord, lastRecord1);
            }
            return;
        }
        clearVelocity();
    }

    private float calVelocity(int idx, MoveRecord lastRecord, MoveRecord lastRecord1) {
        long deltaT;
        MoveRecord record;
        VelocityMonitor velocityMonitor = this;
        double lastValue = lastRecord.values[idx];
        long lastTime = lastRecord.timeStamp;
        double lastValue1 = lastRecord1.values[idx];
        long lastTime1 = lastRecord1.timeStamp;
        double v1 = getVelocity(lastValue, lastValue1, lastTime - lastTime1);
        float velocity = Float.MAX_VALUE;
        long deltaT2 = 0;
        MoveRecord record2 = null;
        int i = velocityMonitor.mHistory.size() - 2;
        while (true) {
            if (i < 0) {
                deltaT = deltaT2;
                record = record2;
                break;
            }
            MoveRecord record3 = velocityMonitor.mHistory.get(i);
            long deltaT3 = record3.timeStamp;
            deltaT = lastTime - deltaT3;
            if (deltaT <= MIN_DELTA || deltaT >= MAX_DELTA) {
                i--;
                v1 = v1;
                deltaT2 = deltaT;
                record2 = record3;
                velocityMonitor = this;
            } else {
                double v12 = v1;
                float v2 = getVelocity(lastValue, record3.values[idx], deltaT);
                if (v12 * v2 > 0.0d) {
                    velocity = (float) (v2 > MiuiFreeformPinManagerService.EDGE_AREA ? Math.max(v12, v2) : Math.min(v12, v2));
                    record = record3;
                } else {
                    velocity = v2;
                    record = record3;
                }
            }
        }
        if (velocity == Float.MAX_VALUE && record != null) {
            float velocity2 = getVelocity(lastValue, record.values[idx], deltaT);
            return velocity2;
        }
        return velocity;
    }

    private float getVelocity(double value1, double value2, long deltaT) {
        return (float) (deltaT == 0 ? 0.0d : (value1 - value2) / (((float) deltaT) / 1000.0f));
    }
}
