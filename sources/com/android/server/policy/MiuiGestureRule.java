package com.android.server.policy;

import android.content.ContentResolver;
import android.os.Handler;
/* loaded from: classes.dex */
public class MiuiGestureRule {
    private final String mAction;
    private MiuiShortcutObserver mMiuiShortcutObserver;

    public MiuiGestureRule(Handler handler, ContentResolver contentResolver, String action) {
        this.mAction = action;
        if (handler != null) {
            MiuiShortcutObserver miuiShortcutObserver = new MiuiShortcutObserver(handler) { // from class: com.android.server.policy.MiuiGestureRule.1
                @Override // com.android.server.policy.MiuiShortcutObserver, android.database.ContentObserver
                public void onChange(boolean selfChange) {
                    super.onChange(selfChange);
                    if (hasListener()) {
                        notifyGestureRuleChanged(MiuiGestureRule.this.getInstance());
                    }
                }
            };
            this.mMiuiShortcutObserver = miuiShortcutObserver;
            miuiShortcutObserver.initObserver(contentResolver, action);
        }
    }

    public MiuiGestureRule getInstance() {
        return this;
    }

    public MiuiShortcutObserver getObserver() {
        return this.mMiuiShortcutObserver;
    }

    public String getFunction() {
        return this.mMiuiShortcutObserver.getFunction();
    }

    public String getAction() {
        return this.mAction;
    }
}
