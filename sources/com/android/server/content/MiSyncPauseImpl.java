package com.android.server.content;

import android.text.TextUtils;
import android.util.Log;
import android.util.Slog;
import java.io.IOException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlSerializer;
/* loaded from: classes.dex */
public class MiSyncPauseImpl implements MiSyncPause {
    private static final long PAUSE_TIME_MAX_INTERVAL = 86400000;
    private static final long PAUSE_TIME_START_DELTA = 60000;
    private static final String TAG = "Sync";
    private static final int VERSION = 1;
    private static final String XML_ATTR_ACCOUNT_NAME = "account_name";
    private static final String XML_ATTR_PAUSE_END_TIME = "end_time";
    private static final String XML_ATTR_UID = "uid";
    private static final String XML_ATTR_VERSION = "version";
    public static final String XML_FILE_NAME = "mi_pause";
    public static final int XML_FILE_VERSION = 1;
    private static final String XML_TAG_ITEM = "sync_pause_item";
    private String mAccountName;
    private int mUid;
    private long mPauseStartTimeMills = 0;
    private long mPauseEndTimeMills = 0;

    public MiSyncPauseImpl(int uid, String accountName) {
        this.mUid = uid;
        this.mAccountName = accountName;
    }

    public int getUid() {
        return this.mUid;
    }

    public String getAccountName() {
        return this.mAccountName;
    }

    public long getPauseEndTime() {
        return this.mPauseEndTimeMills;
    }

    public long getResumeTimeLeft() {
        long currentTimeMills = System.currentTimeMillis();
        if (currentTimeMills <= this.mPauseStartTimeMills - 60000) {
            return 0L;
        }
        long j = this.mPauseEndTimeMills;
        if (j > currentTimeMills) {
            return j - currentTimeMills;
        }
        return 0L;
    }

    public boolean setPauseToTime(long pauseTimeMillis) {
        if (pauseTimeMillis == 0) {
            if (Log.isLoggable(TAG, 3)) {
                Log.d(TAG, "Resume syncs");
            }
            this.mPauseStartTimeMills = 0L;
            this.mPauseEndTimeMills = 0L;
            return true;
        }
        long currentTimeMillis = System.currentTimeMillis();
        if (pauseTimeMillis < currentTimeMillis || pauseTimeMillis - currentTimeMillis > PAUSE_TIME_MAX_INTERVAL) {
            if (Log.isLoggable(TAG, 3)) {
                Log.d(TAG, "Illegal time");
                return false;
            }
            return false;
        }
        this.mPauseStartTimeMills = currentTimeMillis;
        this.mPauseEndTimeMills = pauseTimeMillis;
        return true;
    }

    public void writeToXML(XmlSerializer out) throws IOException {
        out.startTag(null, XML_TAG_ITEM);
        out.attribute(null, "version", Integer.toString(1));
        out.attribute(null, XML_ATTR_UID, Integer.toString(this.mUid));
        out.attribute(null, XML_ATTR_ACCOUNT_NAME, this.mAccountName);
        out.attribute(null, XML_ATTR_PAUSE_END_TIME, Long.toString(this.mPauseEndTimeMills));
        out.endTag(null, XML_TAG_ITEM);
    }

    public static MiSyncPauseImpl readFromXML(XmlPullParser parser) {
        String tagName = parser.getName();
        if (!XML_TAG_ITEM.equals(tagName)) {
            return null;
        }
        String itemVersionString = parser.getAttributeValue(null, "version");
        if (TextUtils.isEmpty(itemVersionString)) {
            Slog.e(TAG, "the version in mi pause is null");
            return null;
        }
        try {
            int itemVersion = Integer.parseInt(itemVersionString);
            if (itemVersion < 1) {
                return null;
            }
            String uidString = parser.getAttributeValue(null, XML_ATTR_UID);
            String accountName = parser.getAttributeValue(null, XML_ATTR_ACCOUNT_NAME);
            String pauseEndTimeMillsString = parser.getAttributeValue(null, XML_ATTR_PAUSE_END_TIME);
            if (TextUtils.isEmpty(uidString) || TextUtils.isEmpty(accountName) || TextUtils.isEmpty(pauseEndTimeMillsString)) {
                Slog.e(TAG, "the item in mi pause is null");
                return null;
            }
            try {
                int uid = Integer.parseInt(uidString);
                long pauseEndTimeMills = Long.parseLong(pauseEndTimeMillsString);
                MiSyncPauseImpl miSyncPause = new MiSyncPauseImpl(uid, accountName);
                miSyncPause.setPauseToTime(pauseEndTimeMills);
                return miSyncPause;
            } catch (NumberFormatException e) {
                Slog.e(TAG, "error parsing item for mi pause", e);
                return null;
            }
        } catch (NumberFormatException e2) {
            Slog.e(TAG, "error parsing version for mi pause", e2);
            return null;
        }
    }
}
