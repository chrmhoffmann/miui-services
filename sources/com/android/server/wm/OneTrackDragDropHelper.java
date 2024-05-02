package com.android.server.wm;

import android.app.ActivityThread;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.os.UserHandle;
import android.util.Slog;
import com.android.server.MiuiBatteryStatsService;
import com.android.server.MiuiBgThread;
import miui.os.Build;
/* loaded from: classes.dex */
public class OneTrackDragDropHelper {
    private static final String APP_ID = "31000000554";
    private static final String EVENT_NAME = "drag_drop";
    private static final int FLAG_NON_ANONYMOUS = 2;
    private static final int FLAG_NOT_LIMITED_BY_USER_EXPERIENCE_PLAN = 1;
    private static final int MSG_DRAG_DROP = 0;
    private static final String ONETRACK_ACTION = "onetrack.action.TRACK_EVENT";
    private static final String ONETRACK_PACKAGE_NAME = "com.miui.analytics";
    private static final String PACKAGE = "android";
    private static final String TAG = OneTrackDragDropHelper.class.getSimpleName();
    private static OneTrackDragDropHelper sInstance;
    private final Context mContext = ActivityThread.currentApplication();
    private final Handler mHandler = new Handler(MiuiBgThread.get().getLooper()) { // from class: com.android.server.wm.OneTrackDragDropHelper.1
        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    OneTrackDragDropHelper.this.reportOneTrack((DragDropData) msg.obj);
                    return;
                default:
                    return;
            }
        }
    };

    public static synchronized OneTrackDragDropHelper getInstance() {
        OneTrackDragDropHelper oneTrackDragDropHelper;
        synchronized (OneTrackDragDropHelper.class) {
            if (sInstance == null) {
                sInstance = new OneTrackDragDropHelper();
            }
            oneTrackDragDropHelper = sInstance;
        }
        return oneTrackDragDropHelper;
    }

    private OneTrackDragDropHelper() {
    }

    public void notifyDragDropResult(WindowState dragWindow, WindowState dropWindow, boolean result) {
        DragDropData dragDropData = new DragDropData(dragWindow, dropWindow, result);
        this.mHandler.obtainMessage(0, dragDropData).sendToTarget();
    }

    public void reportOneTrack(DragDropData dragDropData) {
        Intent intent = new Intent("onetrack.action.TRACK_EVENT");
        intent.setPackage("com.miui.analytics");
        intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_APP_ID, APP_ID);
        intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_PACKAGE, PACKAGE);
        intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_EVENT_NAME, EVENT_NAME);
        intent.putExtra("drag_package", dragDropData.dragPackage);
        intent.putExtra("drag_window", dragDropData.dragWindow);
        intent.putExtra("drop_package", dragDropData.dropPackage);
        intent.putExtra("drop_window", dragDropData.dropWindow);
        intent.putExtra("result", dragDropData.result);
        if (!Build.IS_INTERNATIONAL_BUILD) {
            intent.setFlags(3);
        }
        try {
            this.mContext.startServiceAsUser(intent, UserHandle.CURRENT);
        } catch (Exception e) {
            Slog.e(TAG, "Upload DragDropData exception!", e);
        }
    }

    /* loaded from: classes.dex */
    public static class DragDropData {
        public final String dragPackage;
        public final String dragWindow;
        public final String dropPackage;
        public final String dropWindow;
        public final boolean result;

        public DragDropData(WindowState dragWindow, WindowState dropWindow, boolean result) {
            this.dragWindow = dragWindow.getName();
            String str = "";
            this.dragPackage = dragWindow.getOwningPackage() == null ? str : dragWindow.getOwningPackage();
            if (dropWindow == null) {
                this.dropWindow = str;
                this.dropPackage = str;
            } else {
                this.dropWindow = dropWindow.getName();
                this.dropPackage = dropWindow.getOwningPackage() != null ? dropWindow.getOwningPackage() : str;
            }
            this.result = result;
        }

        public String toString() {
            return "DragDropData{dragPackage='" + this.dragPackage + "', dragWindow='" + this.dragWindow + "', dropPackage='" + this.dropPackage + "', dropWindow='" + this.dropWindow + "', result=" + this.result + '}';
        }
    }
}
