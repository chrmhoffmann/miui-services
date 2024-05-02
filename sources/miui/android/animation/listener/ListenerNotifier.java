package miui.android.animation.listener;

import android.util.ArrayMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import miui.android.animation.IAnimTarget;
import miui.android.animation.base.AnimConfig;
import miui.android.animation.property.IIntValueProperty;
import miui.android.animation.utils.CommonUtils;
import miui.android.animation.utils.ObjectPool;
/* loaded from: classes.dex */
public class ListenerNotifier {
    final Map<Object, List<TransitionListener>> mListenerMap = new ArrayMap();
    final IAnimTarget mTarget;
    static final BeginNotifier sBegin = new BeginNotifier();
    static final PropertyBeginNotifier sPropertyBegin = new PropertyBeginNotifier();
    static final MassUpdateNotifier sMassUpdate = new MassUpdateNotifier();
    static final UpdateNotifier sUpdate = new UpdateNotifier();
    static final PropertyEndNotifier sPropertyEnd = new PropertyEndNotifier();
    static final CancelNotifier sCancelAll = new CancelNotifier();
    static final EndNotifier sEndAll = new EndNotifier();

    /* loaded from: classes.dex */
    public interface INotifier {
        void doNotify(Object obj, TransitionListener transitionListener, Collection<UpdateInfo> collection, UpdateInfo updateInfo);
    }

    /* loaded from: classes.dex */
    public static class BeginNotifier implements INotifier {
        BeginNotifier() {
        }

        @Override // miui.android.animation.listener.ListenerNotifier.INotifier
        public void doNotify(Object tag, TransitionListener listener, Collection<UpdateInfo> updateList, UpdateInfo update) {
            listener.onBegin(tag);
        }
    }

    /* loaded from: classes.dex */
    public static class PropertyBeginNotifier implements INotifier {
        PropertyBeginNotifier() {
        }

        @Override // miui.android.animation.listener.ListenerNotifier.INotifier
        public void doNotify(Object tag, TransitionListener listener, Collection<UpdateInfo> updateList, UpdateInfo update) {
            listener.onBegin(tag, updateList);
        }
    }

    /* loaded from: classes.dex */
    public static class MassUpdateNotifier implements INotifier {
        static final List<UpdateInfo> sEmptyList = new ArrayList();

        MassUpdateNotifier() {
        }

        @Override // miui.android.animation.listener.ListenerNotifier.INotifier
        public void doNotify(Object tag, TransitionListener listener, Collection<UpdateInfo> updateList, UpdateInfo update) {
            listener.onUpdate(tag, sEmptyList);
        }
    }

    /* loaded from: classes.dex */
    public static class UpdateNotifier implements INotifier {
        UpdateNotifier() {
        }

        @Override // miui.android.animation.listener.ListenerNotifier.INotifier
        public void doNotify(Object tag, TransitionListener listener, Collection<UpdateInfo> updateList, UpdateInfo update) {
            if (updateList != null && updateList.size() <= 4000) {
                for (UpdateInfo updateInfo : updateList) {
                    notifySingleProperty(tag, listener, updateInfo);
                }
            }
            listener.onUpdate(tag, updateList);
        }

        private void notifySingleProperty(Object tag, TransitionListener listener, UpdateInfo update) {
            listener.onUpdate(tag, update.property, update.getFloatValue(), update.isCompleted);
            if (update.useInt) {
                listener.onUpdate(tag, (IIntValueProperty) update.property, update.getIntValue(), (float) update.velocity, update.isCompleted);
            } else {
                listener.onUpdate(tag, update.property, update.getFloatValue(), (float) update.velocity, update.isCompleted);
            }
        }
    }

    /* loaded from: classes.dex */
    public static class PropertyEndNotifier implements INotifier {
        PropertyEndNotifier() {
        }

        @Override // miui.android.animation.listener.ListenerNotifier.INotifier
        public void doNotify(Object tag, TransitionListener listener, Collection<UpdateInfo> updateList, UpdateInfo update) {
            for (UpdateInfo info : updateList) {
                if (info.isCompleted && info.animInfo.justEnd) {
                    info.animInfo.justEnd = false;
                    if (info.animInfo.op == 3) {
                        listener.onComplete(tag, info);
                    } else {
                        listener.onCancel(tag, info);
                    }
                }
            }
        }
    }

    /* loaded from: classes.dex */
    public static class CancelNotifier implements INotifier {
        CancelNotifier() {
        }

        @Override // miui.android.animation.listener.ListenerNotifier.INotifier
        public void doNotify(Object tag, TransitionListener listener, Collection<UpdateInfo> updateList, UpdateInfo update) {
            listener.onCancel(tag);
        }
    }

    /* loaded from: classes.dex */
    public static class EndNotifier implements INotifier {
        EndNotifier() {
        }

        @Override // miui.android.animation.listener.ListenerNotifier.INotifier
        public void doNotify(Object tag, TransitionListener listener, Collection<UpdateInfo> updateList, UpdateInfo update) {
            listener.onComplete(tag);
        }
    }

    public ListenerNotifier(IAnimTarget target) {
        this.mTarget = target;
    }

    public boolean addListeners(Object key, AnimConfig config) {
        if (config.listeners.isEmpty()) {
            return false;
        }
        List<TransitionListener> listeners = getListenerSet(key);
        CommonUtils.addTo(config.listeners, listeners);
        return true;
    }

    public void removeListeners(Object key) {
        List<TransitionListener> listeners = this.mListenerMap.remove(key);
        ObjectPool.release(listeners);
    }

    private List<TransitionListener> getListenerSet(Object key) {
        List<TransitionListener> list = this.mListenerMap.get(key);
        if (list == null) {
            List<TransitionListener> list2 = (List) ObjectPool.acquire(ArrayList.class, new Object[0]);
            this.mListenerMap.put(key, list2);
            return list2;
        }
        return list;
    }

    public void notifyBegin(Object key, Object tag) {
        notify(key, tag, sBegin, null, null);
    }

    public void notifyPropertyBegin(Object key, Object tag, Collection<UpdateInfo> updateList) {
        notify(key, tag, sPropertyBegin, updateList, null);
    }

    public void notifyMassUpdate(Object key, Object tag) {
        notify(key, tag, sMassUpdate, null, null);
    }

    public void notifyUpdate(Object key, Object tag, Collection<UpdateInfo> updateList) {
        notify(key, tag, sUpdate, updateList, null);
    }

    public void notifyPropertyEnd(Object key, Object tag, Collection<UpdateInfo> updateList) {
        notify(key, tag, sPropertyEnd, updateList, null);
    }

    public void notifyCancelAll(Object key, Object tag) {
        notify(key, tag, sCancelAll, null, null);
    }

    public void notifyEndAll(Object key, Object tag) {
        notify(key, tag, sEndAll, null, null);
    }

    private void notify(Object key, Object tag, INotifier notifier, Collection<UpdateInfo> updateList, UpdateInfo update) {
        List<TransitionListener> list = this.mListenerMap.get(key);
        if (list != null && !list.isEmpty()) {
            notifyListenerSet(tag, list, notifier, updateList, update);
        }
    }

    private static void notifyListenerSet(Object tag, List<TransitionListener> listeners, INotifier notifier, Collection<UpdateInfo> updateList, UpdateInfo update) {
        Set<TransitionListener> listenerSet = (Set) ObjectPool.acquire(HashSet.class, new Object[0]);
        for (TransitionListener listener : listeners) {
            if (listenerSet.add(listener)) {
                notifier.doNotify(tag, listener, updateList, update);
            }
        }
        ObjectPool.release(listenerSet);
    }
}
