package com.android.server.wifi;

import android.content.Context;
import android.content.Intent;
import android.os.UEventObserver;
import android.os.UserHandle;
import android.util.Log;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Calendar;
import java.util.LinkedList;
/* loaded from: classes.dex */
public class WifiDrvUEventObserver extends UEventObserver {
    private static final int COEX_FDD_MODE = 3;
    private static final int COEX_HBD_MODE = 2;
    private static final String COEX_MODE_EVENT = "coex_mode";
    private static final int COEX_NO_BT = 0;
    private static final int COEX_TDD_MODE = 1;
    private static final boolean DEBUG = false;
    private static final String EXTRA_WBH_MODE_TYPE = "extra_wbh_mode_type";
    private static final int MAX_REC_SIZE = 100;
    private static final String MTK_TRX_ABNORMAL_EVENT = "abnormaltrx";
    private static final String TAG = "WifiDrvUEventObserver";
    private static final String UEVENT_PATH = "DEVPATH=/devices/virtual/misc/wlan";
    public static final String WBH_MODE_CHANGED = "android.net.wifi.COEX_MODE_STATE_CHANGED";
    private Context mContext;
    private LinkedList<UeventRecord> mUeventRecord = new LinkedList<>();
    private int lastCoexMode = 0;

    public WifiDrvUEventObserver(Context context) {
        this.mContext = context;
    }

    public void onUEvent(UEventObserver.UEvent event) {
        try {
            String coexMode = event.get(COEX_MODE_EVENT);
            String mtkTrxAbnormal = event.get(MTK_TRX_ABNORMAL_EVENT);
            if (coexMode != null) {
                int curCoexMode = Integer.parseInt(coexMode);
                int i = this.lastCoexMode;
                if (curCoexMode != i && (curCoexMode >= 2 || i >= 2)) {
                    UeventRecord uRec = new UeventRecord(COEX_MODE_EVENT, coexMode);
                    addUeventRec(uRec);
                    Intent intent = new Intent(WBH_MODE_CHANGED);
                    intent.putExtra(EXTRA_WBH_MODE_TYPE, curCoexMode);
                    this.mContext.sendBroadcastAsUser(intent, UserHandle.SYSTEM);
                }
                this.lastCoexMode = curCoexMode;
            }
            if (mtkTrxAbnormal != null) {
                UeventRecord uRec2 = new UeventRecord(MTK_TRX_ABNORMAL_EVENT, mtkTrxAbnormal);
                addUeventRec(uRec2);
            }
        } catch (NumberFormatException e) {
            Log.e(TAG, "Could not parse event " + event);
        }
    }

    public void start() {
        Log.d(TAG, "startObserving");
        startObserving(UEVENT_PATH);
    }

    public void stop() {
        Log.d(TAG, "stopObserving");
        stopObserving();
    }

    /* loaded from: classes.dex */
    public static class UeventRecord {
        private String mEvent;
        private String mInfo;
        private long mTime = System.currentTimeMillis();

        public UeventRecord(String event, String info) {
            this.mEvent = event;
            this.mInfo = info;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("time=");
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(this.mTime);
            sb.append(String.format("%tm-%td %tH:%tM:%tS.%tL", c, c, c, c, c, c));
            sb.append(" event=");
            String str = this.mEvent;
            String str2 = "<null>";
            if (str == null) {
                str = str2;
            }
            sb.append(str);
            sb.append(" info=");
            String str3 = this.mInfo;
            if (str3 != null) {
                str2 = str3;
            }
            sb.append(str2);
            return sb.toString();
        }
    }

    private int getUeventRecCount() {
        LinkedList<UeventRecord> linkedList = this.mUeventRecord;
        if (linkedList == null) {
            return 0;
        }
        return linkedList.size();
    }

    private void addUeventRec(UeventRecord rec) {
        LinkedList<UeventRecord> linkedList;
        if (rec == null || (linkedList = this.mUeventRecord) == null) {
            return;
        }
        if (linkedList.size() >= 100) {
            this.mUeventRecord.removeFirst();
        }
        this.mUeventRecord.addLast(rec);
    }

    private UeventRecord getUeventRec(int index) {
        if (this.mUeventRecord == null || index >= getUeventRecCount()) {
            return null;
        }
        return this.mUeventRecord.get(index);
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("WifiDrvUEvent:");
        pw.println(" total records=" + getUeventRecCount());
        for (int i = 0; i < getUeventRecCount(); i++) {
            pw.println(" rec[" + i + "]: " + getUeventRec(i));
            pw.flush();
        }
    }
}
