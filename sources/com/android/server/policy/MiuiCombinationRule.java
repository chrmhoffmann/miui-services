package com.android.server.policy;

import android.content.ContentResolver;
import android.os.Handler;
import com.android.server.policy.KeyCombinationManager;
/* loaded from: classes.dex */
public abstract class MiuiCombinationRule extends KeyCombinationManager.TwoKeysCombinationRule {
    private final String mAction;
    private MiuiShortcutObserver mMiuiShortcutObserver;

    public /* bridge */ /* synthetic */ boolean equals(Object obj) {
        return super.equals(obj);
    }

    public /* bridge */ /* synthetic */ String toString() {
        return super.toString();
    }

    public MiuiCombinationRule(int primaryKey, int secondKey, Handler handler, ContentResolver contentResolver, String action) {
        super(primaryKey, secondKey);
        this.mAction = action;
        if (handler != null) {
            MiuiShortcutObserver miuiShortcutObserver = new MiuiShortcutObserver(handler) { // from class: com.android.server.policy.MiuiCombinationRule.1
                @Override // com.android.server.policy.MiuiShortcutObserver, android.database.ContentObserver
                public void onChange(boolean selfChange) {
                    super.onChange(selfChange);
                    if (hasListener()) {
                        notifyCombinationRuleChanged(MiuiCombinationRule.this.getInstance());
                    }
                }
            };
            this.mMiuiShortcutObserver = miuiShortcutObserver;
            miuiShortcutObserver.initObserver(contentResolver, action);
        }
    }

    public MiuiCombinationRule getInstance() {
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

    public boolean preCondition() {
        return getFunction() != null && !"none".equals(getFunction());
    }
}
