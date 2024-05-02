package com.android.server.policy;

import android.content.ContentResolver;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Settings;
import java.util.HashSet;
import java.util.Iterator;
/* loaded from: classes.dex */
public class MiuiShortcutObserver extends ContentObserver {
    private String mAction;
    private ContentResolver mContentResolver;
    private String mFunction;
    private final HashSet<MiuiShortcutListener> mMiuiShortcutListeners = new HashSet<>();

    /* loaded from: classes.dex */
    public interface MiuiShortcutListener {
        void onCombinationChanged(MiuiCombinationRule miuiCombinationRule);

        void onGestureChanged(MiuiGestureRule miuiGestureRule);

        void onSingleChanged(MiuiSingleKeyRule miuiSingleKeyRule);
    }

    public MiuiShortcutObserver(Handler handler) {
        super(handler);
    }

    public void initObserver(ContentResolver contentResolver, String action) {
        if (contentResolver != null) {
            this.mContentResolver = contentResolver;
            this.mAction = action;
            contentResolver.registerContentObserver(Settings.System.getUriFor(action), false, this, -1);
            onChange(false);
        }
    }

    @Override // android.database.ContentObserver
    public void onChange(boolean selfChange) {
        this.mFunction = Settings.System.getStringForUser(this.mContentResolver, this.mAction, -2);
        super.onChange(selfChange);
    }

    public void notifyCombinationRuleChanged(MiuiCombinationRule rule) {
        synchronized (this.mMiuiShortcutListeners) {
            Iterator<MiuiShortcutListener> it = this.mMiuiShortcutListeners.iterator();
            while (it.hasNext()) {
                MiuiShortcutListener listener = it.next();
                listener.onCombinationChanged(rule);
            }
        }
    }

    public void notifySingleRuleChanged(MiuiSingleKeyRule rule) {
        synchronized (this.mMiuiShortcutListeners) {
            Iterator<MiuiShortcutListener> it = this.mMiuiShortcutListeners.iterator();
            while (it.hasNext()) {
                MiuiShortcutListener listener = it.next();
                listener.onSingleChanged(rule);
            }
        }
    }

    public void notifyGestureRuleChanged(MiuiGestureRule rule) {
        synchronized (this.mMiuiShortcutListeners) {
            Iterator<MiuiShortcutListener> it = this.mMiuiShortcutListeners.iterator();
            while (it.hasNext()) {
                MiuiShortcutListener listener = it.next();
                listener.onGestureChanged(rule);
            }
        }
    }

    public boolean hasListener() {
        boolean z;
        synchronized (this.mMiuiShortcutListeners) {
            z = this.mMiuiShortcutListeners.size() != 0;
        }
        return z;
    }

    public void registerShortcutListener(MiuiShortcutListener listener) {
        if (listener != null) {
            synchronized (this.mMiuiShortcutListeners) {
                this.mMiuiShortcutListeners.add(listener);
            }
            onChange(false);
        }
    }

    public void unRegisterShortcutListener(MiuiShortcutListener listener) {
        if (listener != null) {
            synchronized (this.mMiuiShortcutListeners) {
                this.mMiuiShortcutListeners.remove(listener);
            }
        }
    }

    public String getFunction() {
        return this.mFunction;
    }

    public void onDestroy() {
        ContentResolver contentResolver = this.mContentResolver;
        if (contentResolver != null) {
            contentResolver.unregisterContentObserver(this);
            this.mContentResolver = null;
        }
    }
}
