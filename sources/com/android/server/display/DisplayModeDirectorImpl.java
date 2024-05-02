package com.android.server.display;

import android.app.ActivityManager;
import android.util.Slog;
import android.util.SparseArray;
import com.android.server.display.DisplayModeDirector;
import com.miui.base.MiuiStubRegistry;
import com.miui.base.MiuiStubUtil;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
/* loaded from: classes.dex */
public class DisplayModeDirectorImpl implements DisplayModeDirectorStub {
    private static final int HISTORY_COUNT_FOR_HIGH_RAM_DEVICE = 30;
    private static final int HISTORY_COUNT_FOR_LOW_RAM_DEVICE = 10;
    private static final String TAG = "DisplayModeDirectorImpl";
    private final SimpleDateFormat mDateFormat = new SimpleDateFormat("MM-dd HH:mm:ss.SSS");
    private DisplayModeDirector mDisplayModeDirector;
    private DisplayModeDirectorEntry[] mDisplayModeDirectorHistory;
    private int mHistoryCount;
    private int mHistoryIndex;
    private Object mLock;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<DisplayModeDirectorImpl> {

        /* compiled from: DisplayModeDirectorImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final DisplayModeDirectorImpl INSTANCE = new DisplayModeDirectorImpl();
        }

        public DisplayModeDirectorImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public DisplayModeDirectorImpl provideNewInstance() {
            return new DisplayModeDirectorImpl();
        }
    }

    public DisplayModeDirectorImpl() {
        this.mHistoryCount = ActivityManager.isLowRamDeviceStatic() ? 10 : 30;
        this.mHistoryIndex = 0;
    }

    public void init(DisplayModeDirector modeDirector, Object lock) {
        this.mLock = lock;
        this.mDisplayModeDirector = modeDirector;
        this.mDisplayModeDirectorHistory = new DisplayModeDirectorEntry[this.mHistoryCount];
    }

    public static DisplayModeDirectorImpl getInstance() {
        return (DisplayModeDirectorImpl) MiuiStubUtil.getImpl(DisplayModeDirectorStub.class);
    }

    public void onDesiredDisplayModeSpecsChanged(int displayId, DisplayModeDirector.DesiredDisplayModeSpecs desiredDisplayModeSpecs, SparseArray<DisplayModeDirector.Vote> votes) {
        boolean noVotes = votes == null || votes.size() == 0;
        if (!noVotes) {
            addToHistory(displayId, desiredDisplayModeSpecs, votes);
        }
        Slog.i(TAG, "onDesiredDisplayModeSpecsChanged:" + desiredDisplayModeSpecs + " noVotes=" + noVotes);
    }

    public void dumpLocked(PrintWriter pw) {
        pw.println("History of DisplayMoDirector");
        int i = 0;
        while (true) {
            int i2 = this.mHistoryCount;
            if (i < i2) {
                int index = (this.mHistoryIndex + i) % i2;
                DisplayModeDirectorEntry displayModeDirectorEntry = this.mDisplayModeDirectorHistory[index];
                if (displayModeDirectorEntry != null) {
                    pw.println(displayModeDirectorEntry.toString());
                }
                i++;
            } else {
                return;
            }
        }
    }

    public void notifyDisplayModeSpecsChanged() {
        DisplayModeDirector displayModeDirector = this.mDisplayModeDirector;
        if (displayModeDirector != null) {
            displayModeDirector.notifyDesiredDisplayModeSpecsChanged();
        }
    }

    private DisplayModeDirectorEntry addToHistory(int displayId, DisplayModeDirector.DesiredDisplayModeSpecs desiredDisplayModeSpecs, SparseArray<DisplayModeDirector.Vote> votes) {
        DisplayModeDirectorEntry displayModeDirectorEntry;
        synchronized (this.mLock) {
            DisplayModeDirectorEntry[] displayModeDirectorEntryArr = this.mDisplayModeDirectorHistory;
            int i = this.mHistoryIndex;
            DisplayModeDirectorEntry displayModeDirectorEntry2 = displayModeDirectorEntryArr[i];
            if (displayModeDirectorEntry2 != null) {
                displayModeDirectorEntry2.timesTamp = System.currentTimeMillis();
                this.mDisplayModeDirectorHistory[this.mHistoryIndex].displayId = displayId;
                this.mDisplayModeDirectorHistory[this.mHistoryIndex].desiredDisplayModeSpecs = desiredDisplayModeSpecs;
                this.mDisplayModeDirectorHistory[this.mHistoryIndex].votes = votes;
            } else {
                displayModeDirectorEntryArr[i] = new DisplayModeDirectorEntry(displayId, desiredDisplayModeSpecs, votes);
            }
            DisplayModeDirectorEntry[] displayModeDirectorEntryArr2 = this.mDisplayModeDirectorHistory;
            int i2 = this.mHistoryIndex;
            displayModeDirectorEntry = displayModeDirectorEntryArr2[i2];
            this.mHistoryIndex = (i2 + 1) % this.mHistoryCount;
        }
        return displayModeDirectorEntry;
    }

    /* loaded from: classes.dex */
    public class DisplayModeDirectorEntry {
        private DisplayModeDirector.DesiredDisplayModeSpecs desiredDisplayModeSpecs;
        private int displayId;
        private long timesTamp = System.currentTimeMillis();
        private SparseArray<DisplayModeDirector.Vote> votes;

        public DisplayModeDirectorEntry(int displayId, DisplayModeDirector.DesiredDisplayModeSpecs desiredDisplayModeSpecs, SparseArray<DisplayModeDirector.Vote> votes) {
            DisplayModeDirectorImpl.this = this$0;
            this.displayId = displayId;
            this.desiredDisplayModeSpecs = desiredDisplayModeSpecs;
            this.votes = votes;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(DisplayModeDirectorImpl.this.mDateFormat.format(Long.valueOf(this.timesTamp)) + "  Display " + this.displayId + ":\n");
            stringBuilder.append("  mDesiredDisplayModeSpecs:" + this.desiredDisplayModeSpecs + "\n");
            stringBuilder.append("  mVotes:\n");
            for (int p = 15; p >= 0; p--) {
                DisplayModeDirector.Vote vote = this.votes.get(p);
                if (vote != null) {
                    stringBuilder.append("      " + DisplayModeDirector.Vote.priorityToString(p) + " -> " + vote + "\n");
                }
            }
            return stringBuilder.toString();
        }
    }
}
