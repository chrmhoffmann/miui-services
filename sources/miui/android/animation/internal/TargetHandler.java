package miui.android.animation.internal;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import java.util.ArrayList;
import java.util.List;
import miui.android.animation.IAnimTarget;
import miui.android.animation.ViewTarget;
import miui.android.animation.listener.UpdateInfo;
import miui.android.animation.property.ViewPropertyExt;
import miui.android.animation.styles.ForegroundColorStyle;
import miui.android.animation.utils.LogUtils;
/* loaded from: classes.dex */
public final class TargetHandler extends Handler {
    public static final int ANIM_MSG_END = 2;
    public static final int ANIM_MSG_REMOVE_WAIT = 3;
    public static final int ANIM_MSG_REPLACED = 5;
    public static final int ANIM_MSG_START_TAG = 0;
    public static final int ANIM_MSG_UPDATE_LISTENER = 4;
    private static final int MASS_UPDATE_THRESHOLD = 40000;
    private final IAnimTarget mTarget;
    private final List<TransitionInfo> mTransList = new ArrayList();
    public final long threadId = Thread.currentThread().getId();

    public TargetHandler(IAnimTarget target) {
        this.mTarget = target;
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    @Override // android.os.Handler
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case 0:
                TransitionInfo info = TransitionInfo.sMap.remove(Integer.valueOf(msg.arg1));
                if (info != null) {
                    onStart(info);
                    return;
                }
                return;
            case 1:
            default:
                return;
            case 2:
                TransitionInfo info2 = TransitionInfo.sMap.remove(Integer.valueOf(msg.arg1));
                if (info2 != null) {
                    onEnd(info2, msg.arg2);
                    break;
                }
                break;
            case 3:
                this.mTarget.animManager.mWaitState.clear();
                return;
            case 4:
                TransitionInfo info3 = TransitionInfo.sMap.remove(Integer.valueOf(msg.arg1));
                if (info3 != null) {
                    this.mTarget.getNotifier().removeListeners(info3.key);
                    this.mTarget.getNotifier().addListeners(info3.key, info3.config);
                    return;
                }
                return;
            case 5:
                break;
        }
        TransitionInfo info4 = TransitionInfo.sMap.remove(Integer.valueOf(msg.arg1));
        if (info4 != null) {
            onReplaced(info4);
        }
    }

    public boolean isInTargetThread() {
        return Looper.myLooper() == getLooper();
    }

    private void onStart(TransitionInfo info) {
        if (LogUtils.isLogEnabled()) {
            LogUtils.debug(">>> onStart, " + this.mTarget + ", info.key = " + info.key, new Object[0]);
        }
        info.target.getNotifier().addListeners(info.key, info.config);
        info.target.getNotifier().notifyBegin(info.key, info.tag);
        List<UpdateInfo> updateList = info.updateList;
        if (!updateList.isEmpty() && updateList.size() <= 4000) {
            info.target.getNotifier().notifyPropertyBegin(info.key, info.tag, updateList);
        }
        notifyStartOrEnd(info, true);
    }

    private static void notifyStartOrEnd(TransitionInfo info, boolean notifyStart) {
        if (info.getAnimCount() > 4000) {
            return;
        }
        List<UpdateInfo> updateList = info.updateList;
        for (UpdateInfo update : updateList) {
            if (update.property == ViewPropertyExt.FOREGROUND) {
                if (notifyStart) {
                    ForegroundColorStyle.start(info.target, update);
                } else {
                    ForegroundColorStyle.end(info.target, update);
                }
            }
        }
    }

    public void update(boolean toPage) {
        this.mTarget.animManager.getTransitionInfos(this.mTransList);
        for (TransitionInfo transInfo : this.mTransList) {
            update(toPage, transInfo);
        }
        this.mTransList.clear();
    }

    private void update(boolean toPage, TransitionInfo info) {
        List<UpdateInfo> updateList = info.updateList;
        if (!updateList.isEmpty()) {
            setValueAndNotify(info.target, info.key, info.tag, updateList, toPage);
        }
    }

    private static void setValueAndNotify(IAnimTarget target, Object key, Object tag, List<UpdateInfo> updateList, boolean toPage) {
        if (!toPage || (target instanceof ViewTarget)) {
            updateValueAndVelocity(target, updateList);
        }
        if (updateList.size() > MASS_UPDATE_THRESHOLD) {
            target.getNotifier().notifyMassUpdate(key, tag);
            return;
        }
        target.getNotifier().notifyPropertyEnd(key, tag, updateList);
        target.getNotifier().notifyUpdate(key, tag, updateList);
    }

    private void onEnd(TransitionInfo info, int reason) {
        if (LogUtils.isLogEnabled()) {
            LogUtils.debug("<<< onEnd, " + this.mTarget + ", info.key = " + info.key, new Object[0]);
        }
        update(false, info);
        notifyStartOrEnd(info, false);
        if (reason == 4) {
            info.target.getNotifier().notifyCancelAll(info.key, info.tag);
        } else {
            info.target.getNotifier().notifyEndAll(info.key, info.tag);
        }
        info.target.getNotifier().removeListeners(info.key);
    }

    private void onReplaced(TransitionInfo info) {
        if (LogUtils.isLogEnabled()) {
            LogUtils.debug("<<< onReplaced, " + this.mTarget + ", info.key = " + info.key, new Object[0]);
        }
        if (info.getAnimCount() <= 4000) {
            this.mTarget.getNotifier().notifyPropertyEnd(info.key, info.tag, info.updateList);
        }
        this.mTarget.getNotifier().notifyCancelAll(info.key, info.tag);
        this.mTarget.getNotifier().removeListeners(info.key);
    }

    private static void updateValueAndVelocity(IAnimTarget target, List<UpdateInfo> updateList) {
        for (UpdateInfo update : updateList) {
            if (!AnimValueUtils.isInvalid(update.animInfo.value)) {
                update.setTargetValue(target);
            }
        }
    }
}
