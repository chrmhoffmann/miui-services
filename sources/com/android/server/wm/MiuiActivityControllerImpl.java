package com.android.server.wm;

import android.app.IMiuiActivityObserver;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Slog;
import java.util.ArrayList;
import java.util.Iterator;
/* loaded from: classes.dex */
public class MiuiActivityControllerImpl implements MiuiActivityController {
    private static final String PREFIX_TAG = "MiuiLog-ActivityObserver:";
    private static final String SUB_SETTINGS_ACT = "com.android.settings/.SubSettings";
    private static final String SUB_SETTINGS_FRAGMENT_NAME = ":settings:show_fragment";
    private static final String TAG = "MiuiActivityController";
    private final H mH;
    private static final boolean DEBUG_MESSAGES = SystemProperties.getBoolean("debug.miui.activity.log", false);
    private static final boolean MIUI_ACTIVITY_EMBEDDING_ENABLED = SystemProperties.getBoolean("ro.config.miui_activity_embedding_enable", false);
    static final MiuiActivityControllerImpl INSTANCE = new MiuiActivityControllerImpl();
    private final Intent mSendIntent = new Intent();
    private final RemoteCallbackList<IMiuiActivityObserver> mActivityObservers = new RemoteCallbackList<>();

    /* loaded from: classes.dex */
    public final class H extends Handler {
        static final int ACTIVITY_DESTROYED = 5;
        static final int ACTIVITY_IDLE = 1;
        static final int ACTIVITY_PAUSED = 3;
        static final int ACTIVITY_RESUMED = 2;
        static final int ACTIVITY_STOPPED = 4;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public H(Looper looper) {
            super(looper, null, true);
            MiuiActivityControllerImpl.this = this$0;
        }

        String codeToString(int code) {
            if (MiuiActivityControllerImpl.DEBUG_MESSAGES) {
                switch (code) {
                    case 1:
                        return "ACTIVITY_IDLE";
                    case 2:
                        return "ACTIVITY_RESUMED";
                    case 3:
                        return "ACTIVITY_PAUSED";
                    case 4:
                        return "ACTIVITY_STOPPED";
                    case 5:
                        return "ACTIVITY_DESTROYED";
                }
            }
            return Integer.toString(code);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            int what = msg.what;
            switch (what) {
                case 1:
                case 2:
                case 3:
                case 4:
                case 5:
                    if (msg.obj instanceof ActivityRecord) {
                        ActivityRecord record = (ActivityRecord) msg.obj;
                        handleActivityStateChanged(what, record);
                        return;
                    }
                    return;
                default:
                    return;
            }
        }

        private synchronized void handleActivityStateChanged(int what, ActivityRecord record) {
            int i = MiuiActivityControllerImpl.this.mActivityObservers.beginBroadcast();
            while (true) {
                int i2 = i - 1;
                if (i > 0) {
                    IMiuiActivityObserver observer = (IMiuiActivityObserver) MiuiActivityControllerImpl.this.mActivityObservers.getBroadcastItem(i2);
                    if (observer != null) {
                        try {
                            Object cookie = MiuiActivityControllerImpl.this.mActivityObservers.getBroadcastCookie(i2);
                            if (cookie instanceof Intent) {
                                if (canDispatchNow(record, (Intent) cookie)) {
                                    dispatchEvent(what, observer, record);
                                } else {
                                    MiuiActivityControllerImpl.logMessage(MiuiActivityControllerImpl.PREFIX_TAG, " No need to dispatch the event, ignore it!");
                                }
                            } else {
                                dispatchEvent(what, observer, record);
                            }
                        } catch (Exception e) {
                            Slog.e(MiuiActivityControllerImpl.TAG, "MiuiLog-ActivityObserver: There was something wrong : " + e.getMessage());
                        }
                    }
                    i = i2;
                } else {
                    MiuiActivityControllerImpl.this.mActivityObservers.finishBroadcast();
                }
            }
        }

        private boolean canDispatchNow(ActivityRecord record, Intent intent) {
            boolean needFilterPackage;
            boolean needFilterActivity;
            if (record != null && intent != null) {
                ArrayList<String> packages = intent.getStringArrayListExtra("packages");
                ArrayList<ComponentName> activities = intent.getParcelableArrayListExtra("activities");
                if (packages == null || packages.isEmpty()) {
                    needFilterPackage = false;
                } else {
                    needFilterPackage = true;
                }
                if (activities == null || activities.isEmpty()) {
                    needFilterActivity = false;
                } else {
                    needFilterActivity = true;
                }
                if (!needFilterPackage && !needFilterActivity) {
                    return true;
                }
                if (needFilterPackage) {
                    if (packages.contains(record.packageName)) {
                        return true;
                    }
                    MiuiActivityControllerImpl.logMessage(MiuiActivityControllerImpl.PREFIX_TAG, "The package " + record.packageName + " is not matched");
                } else {
                    MiuiActivityControllerImpl.logMessage(MiuiActivityControllerImpl.PREFIX_TAG, "Don't need to check package");
                }
                if (needFilterActivity) {
                    ComponentName realActivity = record.mActivityComponent;
                    if (realActivity != null) {
                        Iterator<ComponentName> it = activities.iterator();
                        while (it.hasNext()) {
                            ComponentName activity = it.next();
                            if (realActivity.equals(activity)) {
                                return true;
                            }
                        }
                        MiuiActivityControllerImpl.logMessage(MiuiActivityControllerImpl.PREFIX_TAG, "The activity " + realActivity + " is not matched");
                    } else {
                        MiuiActivityControllerImpl.logMessage(MiuiActivityControllerImpl.PREFIX_TAG, "The realActivity is null");
                    }
                } else {
                    MiuiActivityControllerImpl.logMessage(MiuiActivityControllerImpl.PREFIX_TAG, "Don't need to check activity");
                }
                return false;
            }
            MiuiActivityControllerImpl.logMessage(MiuiActivityControllerImpl.PREFIX_TAG, "Record or intent is null");
            return false;
        }

        private void dispatchEvent(int event, IMiuiActivityObserver observer, ActivityRecord record) throws RemoteException {
            boolean isEmbedded;
            Intent intent = MiuiActivityControllerImpl.this.mSendIntent;
            intent.setComponent(record.mActivityComponent);
            MiuiActivityControllerImpl.this.handleSpecialExtras(intent, record);
            switch (event) {
                case 1:
                    observer.activityIdle(intent);
                    return;
                case 2:
                    intent.putExtra("appBounds", record.getConfiguration().windowConfiguration.getAppBounds());
                    if (MiuiActivityControllerImpl.MIUI_ACTIVITY_EMBEDDING_ENABLED) {
                        synchronized (record.mAtmService.mGlobalLock) {
                            isEmbedded = record.isEmbedded();
                        }
                        if (isEmbedded && MiuiEmbeddingWindowServiceStub.get().isEmbeddingEnabledForPackage(record.packageName)) {
                            intent.putExtra("windowMode", 13);
                        }
                    }
                    observer.activityResumed(intent);
                    return;
                case 3:
                    observer.activityPaused(intent);
                    return;
                case 4:
                    observer.activityStopped(intent);
                    return;
                case 5:
                    observer.activityDestroyed(intent);
                    return;
                default:
                    return;
            }
        }
    }

    private MiuiActivityControllerImpl() {
        HandlerThread handlerThread = new HandlerThread(TAG, -2);
        handlerThread.start();
        this.mH = new H(handlerThread.getLooper());
    }

    public void handleSpecialExtras(Intent intent, ActivityRecord record) {
        boolean isSubSettings = false;
        if (record != null) {
            try {
                if (SUB_SETTINGS_ACT.equals(record.shortComponentName) && record.intent != null) {
                    String fragment = record.intent.getStringExtra(SUB_SETTINGS_FRAGMENT_NAME);
                    if (!TextUtils.isEmpty(fragment)) {
                        intent.putExtra(SUB_SETTINGS_FRAGMENT_NAME, fragment);
                        isSubSettings = true;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (!isSubSettings) {
            intent.removeExtra(SUB_SETTINGS_FRAGMENT_NAME);
        }
        if (record != null && record.intent != null) {
            intent.setSender(record.intent.getSender());
        }
    }

    public void registerActivityObserver(IMiuiActivityObserver observer, Intent intent) {
        this.mActivityObservers.register(observer, intent);
    }

    public void unregisterActivityObserver(IMiuiActivityObserver observer) {
        this.mActivityObservers.unregister(observer);
    }

    public void activityIdle(ActivityRecord record) {
        sendMessage(1, record);
    }

    public void activityResumed(ActivityRecord record) {
        sendMessage(2, record);
    }

    public void activityPaused(ActivityRecord record) {
        sendMessage(3, record);
    }

    public void activityStopped(ActivityRecord record) {
        sendMessage(4, record);
    }

    public void activityDestroyed(ActivityRecord record) {
        sendMessage(5, record);
    }

    private void sendMessage(int what, Object obj) {
        sendMessage(what, obj, 0, 0, false);
    }

    private void sendMessage(int what, Object obj, int arg1) {
        sendMessage(what, obj, arg1, 0, false);
    }

    private void sendMessage(int what, Object obj, int arg1, int arg2) {
        sendMessage(what, obj, arg1, arg2, false);
    }

    private void sendMessage(int what, Object obj, int arg1, int arg2, boolean async) {
        int size = this.mActivityObservers.getRegisteredCallbackCount();
        logMessage(PREFIX_TAG, "SendMessage " + what + " " + this.mH.codeToString(what) + ": " + arg1 + " / " + obj + " observer size: " + size);
        if (size <= 0) {
            return;
        }
        Message msg = Message.obtain();
        msg.what = what;
        msg.obj = obj;
        msg.arg1 = arg1;
        msg.arg2 = arg2;
        if (async) {
            msg.setAsynchronous(true);
        }
        this.mH.sendMessage(msg);
    }

    public static void logMessage(String prefix, String msg) {
        if (DEBUG_MESSAGES) {
            Slog.d(TAG, prefix + msg);
        }
    }
}
