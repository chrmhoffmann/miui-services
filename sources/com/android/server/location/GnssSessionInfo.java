package com.android.server.location;

import android.util.Log;
import java.util.Calendar;
/* loaded from: classes.dex */
public class GnssSessionInfo {
    public static final int STATE_FIX = 2;
    public static final int STATE_INIT = 0;
    public static final int STATE_LOSE = 4;
    public static final int STATE_SAVE = 5;
    public static final int STATE_START = 1;
    public static final int STATE_STOP = 3;
    public static final int STATE_UNKNOWN = 100;
    private static final String TAG = "GnssSessionInfo";
    private int mAllFixed;
    private int mCurrentState;
    private long mEndTime;
    private int mLostTimes;
    private int mPDREngaged;
    private String mPackName;
    private int mSAPEngaged;
    private long mStartTime;
    private long mTTFF;
    private double mTop4B1Cn0Sum;
    private int mTop4B1Cn0Times;
    private double mTop4L1Cn0Sum;
    private int mTop4L1Cn0Times;
    private double mTop4L5Cn0Sum;
    private int mTop4L5Cn0Times;

    public GnssSessionInfo() {
        this.mPackName = null;
        this.mStartTime = 0L;
        this.mEndTime = 0L;
        this.mTTFF = -1L;
        this.mLostTimes = 0;
        this.mSAPEngaged = 0;
        this.mPDREngaged = 0;
        this.mAllFixed = 0;
        this.mTop4L1Cn0Sum = 0.0d;
        this.mTop4L1Cn0Times = 0;
        this.mTop4L5Cn0Sum = 0.0d;
        this.mTop4L5Cn0Times = 0;
        this.mTop4B1Cn0Sum = 0.0d;
        this.mTop4B1Cn0Times = 0;
        this.mCurrentState = 100;
        this.mCurrentState = 100;
        this.mPackName = null;
        this.mStartTime = 0L;
        this.mEndTime = 0L;
        this.mTTFF = -1L;
        this.mLostTimes = 0;
        this.mSAPEngaged = 0;
        this.mPDREngaged = 0;
        this.mAllFixed = 0;
        this.mTop4L1Cn0Sum = 0.0d;
        this.mTop4L1Cn0Times = 0;
        this.mTop4L5Cn0Sum = 0.0d;
        this.mTop4L5Cn0Times = 0;
        this.mTop4B1Cn0Sum = 0.0d;
        this.mTop4B1Cn0Times = 0;
    }

    public GnssSessionInfo(String pack, long starttime, long endtime, long ttff, int lost, int state) {
        this.mPackName = null;
        this.mStartTime = 0L;
        this.mEndTime = 0L;
        this.mTTFF = -1L;
        this.mLostTimes = 0;
        this.mSAPEngaged = 0;
        this.mPDREngaged = 0;
        this.mAllFixed = 0;
        this.mTop4L1Cn0Sum = 0.0d;
        this.mTop4L1Cn0Times = 0;
        this.mTop4L5Cn0Sum = 0.0d;
        this.mTop4L5Cn0Times = 0;
        this.mTop4B1Cn0Sum = 0.0d;
        this.mTop4B1Cn0Times = 0;
        this.mCurrentState = 100;
        this.mPackName = pack;
        this.mStartTime = starttime;
        this.mEndTime = endtime;
        this.mTTFF = ttff;
        this.mLostTimes = lost;
        this.mCurrentState = state;
    }

    public void setStart(String pack) {
        this.mStartTime = getCurrentTime();
        this.mPackName = pack;
        this.mCurrentState = 1;
    }

    public void setStart() {
        this.mStartTime = getCurrentTime();
        this.mCurrentState = 1;
    }

    public void setEnd() {
        this.mEndTime = getCurrentTime();
        this.mCurrentState = 3;
    }

    public void setTtffManually(long ttff) {
        this.mTTFF = ttff;
        this.mCurrentState = 2;
    }

    public void setLostTimes() {
        this.mLostTimes++;
        this.mCurrentState = 4;
    }

    public void setLostTimes(int lostTimes) {
        this.mLostTimes = lostTimes;
    }

    public void setTtffAuto() {
        long cur = getCurrentTime();
        long j = this.mStartTime;
        if (cur > j) {
            this.mTTFF = (cur - j) / 1000;
        } else {
            this.mTTFF = -1L;
        }
        this.mCurrentState = 2;
    }

    public void newSessionReset() {
        this.mPackName = null;
        this.mStartTime = 0L;
        this.mEndTime = 0L;
        this.mTTFF = -1L;
        this.mLostTimes = 0;
        this.mCurrentState = 0;
        this.mSAPEngaged = 0;
        this.mPDREngaged = 0;
        this.mAllFixed = 0;
        this.mTop4L1Cn0Sum = 0.0d;
        this.mTop4L1Cn0Times = 0;
        this.mTop4L5Cn0Sum = 0.0d;
        this.mTop4L5Cn0Times = 0;
        this.mTop4B1Cn0Sum = 0.0d;
        this.mTop4B1Cn0Times = 0;
    }

    public boolean checkValidity() {
        int i = this.mCurrentState;
        if (i == 1 && this.mStartTime == 0) {
            return false;
        }
        if (i == 3 && this.mEndTime == 0) {
            return false;
        }
        if (i == 4 && this.mLostTimes == 0) {
            return false;
        }
        return ((i == 2 && this.mTTFF == -1) || i == 100 || i == 0) ? false : true;
    }

    private long getCurrentTime() {
        return System.currentTimeMillis();
    }

    public long getTtff() {
        return this.mTTFF;
    }

    public String getPackName() {
        return this.mPackName;
    }

    public int getStartTimeInHour() {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(this.mStartTime);
        return c.get(11);
    }

    public long getRunTime() {
        long j = this.mEndTime;
        long j2 = this.mStartTime;
        if (j >= j2) {
            return (j - j2) / 1000;
        }
        return -1L;
    }

    public int getLoseTimes() {
        return this.mLostTimes;
    }

    public int getAllFixTimes() {
        return this.mAllFixed;
    }

    public int getSapTimes() {
        return this.mSAPEngaged;
    }

    public int getPdrTimes() {
        return this.mPDREngaged;
    }

    public double getL1Top4Cn0Mean() {
        int i = this.mTop4L1Cn0Times;
        if (i != 0) {
            return this.mTop4L1Cn0Sum / i;
        }
        return 0.0d;
    }

    public double getL5Top4Cn0Mean() {
        int i = this.mTop4L5Cn0Times;
        if (i != 0) {
            return this.mTop4L5Cn0Sum / i;
        }
        return 0.0d;
    }

    public double getB1Top4Cn0Mean() {
        int i = this.mTop4B1Cn0Times;
        if (i != 0) {
            return this.mTop4B1Cn0Sum / i;
        }
        return 0.0d;
    }

    public String toString() {
        return "state = " + this.mCurrentState + " start time = " + this.mStartTime + " ttff = " + this.mTTFF + " end time = " + this.mEndTime + " lose times = " + this.mLostTimes + "fixed number " + this.mAllFixed + " sap number " + this.mSAPEngaged + " pdr number " + this.mPDREngaged;
    }

    public void parseNmea(String nmea) {
        int i = this.mCurrentState;
        if (i != 2 && i != 4) {
            return;
        }
        this.mAllFixed++;
        if (nmea.startsWith("$PQWP6")) {
            PQWP6 p6 = new PQWP6(nmea);
            p6.parse();
            if (p6.isSapEnaged()) {
                this.mSAPEngaged++;
            }
            if (p6.isPdrEngaged()) {
                this.mPDREngaged++;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class PQWP6 {
        private String mNmea;
        private String[] sub1;
        private String[] sub2;
        private boolean sapEnaged = false;
        private boolean pdrEnaged = false;

        public PQWP6(String nmea) {
            GnssSessionInfo.this = r1;
            this.mNmea = nmea;
        }

        public void parse() {
            String[] split = this.mNmea.split(",");
            this.sub1 = split;
            if (split.length != 3) {
                Log.e(GnssSessionInfo.TAG, "Error nmea is: " + this.mNmea);
                return;
            }
            String[] split2 = split[2].split("\\*");
            this.sub2 = split2;
            boolean z = false;
            this.sapEnaged = ((Integer.parseInt(split2[0], 16) >> 1) & 1) == 1;
            if (((Integer.parseInt(this.sub2[0], 16) >> 2) & 1) == 1) {
                z = true;
            }
            this.pdrEnaged = z;
        }

        public boolean isSapEnaged() {
            return this.sapEnaged;
        }

        public boolean isPdrEngaged() {
            return this.pdrEnaged;
        }
    }

    public void setL1Cn0(double currentTop4AvgCn0) {
        this.mTop4L1Cn0Times++;
        this.mTop4L1Cn0Sum += currentTop4AvgCn0;
    }

    public void setL5Cn0(double currentTop4AvgCn0) {
        this.mTop4L5Cn0Times++;
        this.mTop4L5Cn0Sum += currentTop4AvgCn0;
    }

    public void setB1Cn0(double currentTop4AvgCn0) {
        this.mTop4B1Cn0Times++;
        this.mTop4B1Cn0Sum += currentTop4AvgCn0;
    }
}
