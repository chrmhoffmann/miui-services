package com.miui.server.migard;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import com.miui.server.migard.utils.LogUtils;
import java.util.ArrayList;
import java.util.List;
/* loaded from: classes.dex */
public class ScreenStatusManager extends BroadcastReceiver {
    private static final String TAG = ScreenStatusManager.class.getSimpleName();
    private static ScreenStatusManager sInstance = new ScreenStatusManager();
    private List<IScreenChangedCallback> mCallbacks = new ArrayList();
    private final IntentFilter mFilter;

    /* loaded from: classes.dex */
    public interface IScreenChangedCallback {
        String getCallbackName();

        void onScreenOff();

        void onUserPresent();
    }

    public static ScreenStatusManager getInstance() {
        return sInstance;
    }

    public IntentFilter getFilter() {
        return this.mFilter;
    }

    private ScreenStatusManager() {
        IntentFilter intentFilter = new IntentFilter();
        this.mFilter = intentFilter;
        intentFilter.addAction("android.intent.action.SCREEN_OFF");
        intentFilter.addAction("android.intent.action.USER_PRESENT");
    }

    @Override // android.content.BroadcastReceiver
    public final void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) {
            return;
        }
        char c = 65535;
        switch (action.hashCode()) {
            case -2128145023:
                if (action.equals("android.intent.action.SCREEN_OFF")) {
                    c = 0;
                    break;
                }
                break;
            case 823795052:
                if (action.equals("android.intent.action.USER_PRESENT")) {
                    c = 1;
                    break;
                }
                break;
        }
        switch (c) {
            case 0:
                LogUtils.d(TAG, "screen off");
                for (IScreenChangedCallback cb : this.mCallbacks) {
                    cb.onScreenOff();
                }
                return;
            case 1:
                LogUtils.d(TAG, "user present");
                for (IScreenChangedCallback cb2 : this.mCallbacks) {
                    cb2.onUserPresent();
                }
                return;
            default:
                return;
        }
    }

    public void registerCallback(IScreenChangedCallback cb) {
        LogUtils.d(TAG, "Register callback, name:" + cb.getCallbackName());
        if (!this.mCallbacks.contains(cb)) {
            this.mCallbacks.add(cb);
        }
    }

    public void unregisterCallback(IScreenChangedCallback cb) {
        LogUtils.d(TAG, "Unregister callback, name:" + cb.getCallbackName());
        if (this.mCallbacks.contains(cb)) {
            this.mCallbacks.remove(cb);
        }
    }
}
