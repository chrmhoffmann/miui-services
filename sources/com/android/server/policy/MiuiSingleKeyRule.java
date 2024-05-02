package com.android.server.policy;

import android.content.ContentResolver;
import android.os.Handler;
import com.android.server.policy.SingleKeyGestureDetector;
/* loaded from: classes.dex */
public abstract class MiuiSingleKeyRule extends SingleKeyGestureDetector.SingleKeyRule {
    private final String mAction;
    private final int mKeyCode;
    private MiuiShortcutObserver mMiuiShortcutObserver;

    public /* bridge */ /* synthetic */ boolean equals(Object obj) {
        return super.equals(obj);
    }

    public /* bridge */ /* synthetic */ String toString() {
        return super.toString();
    }

    public MiuiSingleKeyRule(int keyCode, int supportedGestures, Handler handler, ContentResolver contentResolver, String action) {
        super(keyCode, supportedGestures);
        this.mAction = action;
        this.mKeyCode = keyCode;
        if (handler != null) {
            MiuiShortcutObserver miuiShortcutObserver = new MiuiShortcutObserver(handler) { // from class: com.android.server.policy.MiuiSingleKeyRule.1
                @Override // com.android.server.policy.MiuiShortcutObserver, android.database.ContentObserver
                public void onChange(boolean selfChange) {
                    super.onChange(selfChange);
                    if (hasListener()) {
                        notifySingleRuleChanged(MiuiSingleKeyRule.this.getInstance());
                    }
                }
            };
            this.mMiuiShortcutObserver = miuiShortcutObserver;
            miuiShortcutObserver.initObserver(contentResolver, action);
        }
    }

    public MiuiSingleKeyRule getInstance() {
        return this;
    }

    public MiuiShortcutObserver getObserver() {
        return this.mMiuiShortcutObserver;
    }

    public int getPrimaryKey() {
        return this.mKeyCode;
    }

    public String getFunction() {
        return this.mMiuiShortcutObserver.getFunction();
    }

    public String getAction() {
        return this.mAction;
    }

    int getMaxMultiPressCount() {
        return 2;
    }

    void onPress(long downTime) {
    }
}
