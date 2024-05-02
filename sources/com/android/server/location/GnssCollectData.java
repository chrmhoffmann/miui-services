package com.android.server.location;

import android.provider.BaseColumns;
/* loaded from: classes.dex */
public class GnssCollectData {
    private double B1Top4MeanCn0;
    private double L1Top4MeanCn0;
    private double L5Top4MeanCn0;
    private int PDRNumber;
    private int SAPNumber;
    private long TTFF;
    private int id;
    private int loseTimes;
    private long runTime;
    private long startTime;
    private int totalNumber;

    public GnssCollectData() {
        this.startTime = 0L;
        this.TTFF = -1L;
        this.runTime = 0L;
        this.loseTimes = 0;
        this.SAPNumber = 0;
        this.PDRNumber = 0;
        this.totalNumber = 0;
        this.L1Top4MeanCn0 = 0.0d;
        this.L5Top4MeanCn0 = 0.0d;
        this.B1Top4MeanCn0 = 0.0d;
    }

    public GnssCollectData(long startTime, long TTFF, long runTime, int loseTimes, int SAPNumber, int PDRNumber, int totalNumber, double L1Top4MeanCn0, double L5Top4MeanCn0, double B1Top4MeanCn0) {
        this.startTime = startTime;
        this.TTFF = TTFF;
        this.runTime = runTime;
        this.loseTimes = loseTimes;
        this.SAPNumber = SAPNumber;
        this.PDRNumber = PDRNumber;
        this.totalNumber = totalNumber;
        this.L1Top4MeanCn0 = L1Top4MeanCn0;
        this.L5Top4MeanCn0 = L5Top4MeanCn0;
        this.B1Top4MeanCn0 = B1Top4MeanCn0;
    }

    public int getId() {
        return this.id;
    }

    public long getStartTime() {
        return this.startTime;
    }

    public long getTtff() {
        return this.TTFF;
    }

    public long getRunTime() {
        return this.runTime;
    }

    public int getLoseTimes() {
        return this.loseTimes;
    }

    public int getSapNumber() {
        return this.SAPNumber;
    }

    public int getPdrNumber() {
        return this.PDRNumber;
    }

    public int getTotalNumber() {
        return this.totalNumber;
    }

    public double getL1Top4MeanCn0() {
        return this.L1Top4MeanCn0;
    }

    public double getL5Top4MeanCn0() {
        return this.L5Top4MeanCn0;
    }

    public double getB1Top4MeanCn0() {
        return this.B1Top4MeanCn0;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public void setTtff(long TTFF) {
        this.TTFF = TTFF;
    }

    public void setRunTime(long runTime) {
        this.runTime = getRunTime();
    }

    public void setLoseTimes(int loseTimes) {
        this.loseTimes = loseTimes;
    }

    public void setSapNumber(int SAPNumber) {
        this.SAPNumber = SAPNumber;
    }

    public void setPdrNumber(int PDRNumber) {
        this.PDRNumber = PDRNumber;
    }

    public void setTotalNumber(int totalNumber) {
        this.totalNumber = totalNumber;
    }

    public void setL1Top4MeanCn0(double L1Top4MeanCn0) {
        this.L1Top4MeanCn0 = L1Top4MeanCn0;
    }

    public void setL5Top4MeanCn0(double L5Top4MeanCn0) {
        this.L5Top4MeanCn0 = L5Top4MeanCn0;
    }

    public void setB1Top4MeanCn0(double B1Top4MeanCn0) {
        this.B1Top4MeanCn0 = B1Top4MeanCn0;
    }

    /* loaded from: classes.dex */
    public static class CollectDbEntry implements BaseColumns {
        public static final String COLUMN_NAME_B1TOP4MEANCN0 = "B1Top4MeanCn0";
        public static final String COLUMN_NAME_L1TOP4MEANCN0 = "L1Top4MeanCn0";
        public static final String COLUMN_NAME_L5TOP4MEANCN0 = "L5Top4MeanCn0";
        public static final String COLUMN_NAME_LOSETIMES = "loseTimes";
        public static final String COLUMN_NAME_PDRNUMBER = "PDRNumber";
        public static final String COLUMN_NAME_RUNTIME = "runTime";
        public static final String COLUMN_NAME_SAPNUMBER = "SAPNumber";
        public static final String COLUMN_NAME_STARTTIME = "startTime";
        public static final String COLUMN_NAME_TOTALNUMBER = "totalNumber";
        public static final String COLUMN_NAME_TTFF = "TTFF";
        public static final String TABLE_NAME = "GnssCollectData";

        private CollectDbEntry() {
        }
    }
}
