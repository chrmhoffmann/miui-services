package miui.android.animation.internal;

import java.util.Collection;
import miui.android.animation.IAnimTarget;
import miui.android.animation.base.AnimConfig;
import miui.android.animation.base.AnimConfigLink;
import miui.android.animation.controller.AnimState;
import miui.android.animation.listener.ListenerNotifier;
import miui.android.animation.listener.UpdateInfo;
/* loaded from: classes.dex */
public class NotifyManager {
    private AnimConfig mConfig = new AnimConfig();
    ListenerNotifier mNotifier;
    ListenerNotifier mSetToNotifier;
    IAnimTarget mTarget;

    public NotifyManager(IAnimTarget target) {
        this.mTarget = target;
        this.mSetToNotifier = new ListenerNotifier(target);
        this.mNotifier = new ListenerNotifier(target);
    }

    public void setToNotify(AnimState state, AnimConfigLink oneTimeConfigs) {
        if (oneTimeConfigs == null) {
            return;
        }
        Object tag = state.getTag();
        this.mConfig.copy(state.getConfig());
        oneTimeConfigs.addTo(this.mConfig);
        if (!this.mSetToNotifier.addListeners(tag, this.mConfig)) {
            this.mConfig.clear();
            return;
        }
        this.mSetToNotifier.notifyBegin(tag, tag);
        Collection<UpdateInfo> updates = this.mTarget.animManager.mUpdateMap.values();
        this.mSetToNotifier.notifyPropertyBegin(tag, tag, updates);
        this.mSetToNotifier.notifyUpdate(tag, tag, updates);
        this.mSetToNotifier.notifyPropertyEnd(tag, tag, updates);
        this.mSetToNotifier.notifyEndAll(tag, tag);
        this.mSetToNotifier.removeListeners(tag);
        this.mConfig.clear();
    }

    public ListenerNotifier getNotifier() {
        return this.mNotifier;
    }
}
