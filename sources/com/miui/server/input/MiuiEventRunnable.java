package com.miui.server.input;

import android.text.TextUtils;
import android.util.Slog;
import android.view.MotionEvent;
/* loaded from: classes.dex */
public class MiuiEventRunnable implements Runnable {
    private String mName;
    private int mSeq;

    @Override // java.lang.Runnable
    public void run() {
        if (!TextUtils.isEmpty(this.mName)) {
            Slog.i("MiuiInputMonitor", "Global Monitor is time out, Event seq = " + this.mSeq + " in : " + this.mName + " more than 2500ms ");
        }
    }

    public void setData(MotionEvent event, String name) {
        this.mSeq = event.getSequenceNumber();
        this.mName = name;
    }
}
