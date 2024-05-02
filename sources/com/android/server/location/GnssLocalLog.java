package com.android.server.location;

import android.util.Log;
import java.io.PrintWriter;
import java.util.Calendar;
import java.util.Iterator;
import java.util.LinkedList;
/* loaded from: classes.dex */
public class GnssLocalLog {
    private static final String TAG = "GnssLocalLog";
    private int mMaxLines;
    private long mNow;
    private String mRandomString;
    private String mGlpEn = "=MI GLP EN=";
    private String mNmea = "=MI NMEA=";
    private String mRandomInit = "=MI Random=";
    private LinkedList<String> mLog = new LinkedList<>();

    public GnssLocalLog(int maxLines) {
        this.mRandomString = null;
        this.mMaxLines = maxLines;
        try {
            this.mRandomString = EnCode.initRandom();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized int setLength(int length) {
        if (length > 0) {
            this.mMaxLines = length;
        }
        return length;
    }

    public synchronized void clearData() {
        this.mLog.clear();
    }

    public synchronized void log(String msg) {
        if (this.mMaxLines > 0) {
            this.mNow = System.currentTimeMillis();
            StringBuilder sb = new StringBuilder();
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(this.mNow);
            sb.append(String.format("%tm-%td %tH:%tM:%tS.%tL", c, c, c, c, c, c));
            this.mLog.add(sb.toString() + " - " + msg);
            while (this.mLog.size() > this.mMaxLines) {
                this.mLog.remove();
            }
        }
    }

    public synchronized void dump(PrintWriter pw) {
        Iterator<String> itr = this.mLog.listIterator(0);
        pw.println(this.mRandomInit + this.mRandomString);
        while (itr.hasNext()) {
            try {
                String log = itr.next();
                if (isPresence(log, this.mGlpEn)) {
                    pw.println(this.mGlpEn + EnCode.encText(getTime(log, this.mGlpEn) + getRawString(log, this.mGlpEn)));
                } else if (isPresence(log, this.mNmea)) {
                    pw.println(this.mNmea + EnCode.encText(getTime(log, this.mNmea) + getRawString(log, this.mNmea)));
                } else {
                    pw.println(log);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error encountered on encrypt the log.", e);
            }
        }
    }

    private boolean isPresence(String max, String min) {
        return max.contains(min);
    }

    private String getRawString(String rawString, String keyWord) {
        return rawString.substring(rawString.indexOf(keyWord) + keyWord.length());
    }

    private String getTime(String raw, String keyWord) {
        int index = raw.indexOf(keyWord);
        if (index == -1) {
            Log.e(TAG, "no keyWord " + keyWord + " here\n");
            return "get time error ";
        }
        return raw.substring(0, index);
    }
}
