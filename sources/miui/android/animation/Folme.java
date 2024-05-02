package miui.android.animation;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AbsListView;
import android.widget.TextView;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import miui.android.animation.controller.FolmeFont;
import miui.android.animation.controller.FolmeHover;
import miui.android.animation.controller.FolmeTouch;
import miui.android.animation.controller.FolmeVisible;
import miui.android.animation.controller.ListViewTouchListener;
import miui.android.animation.controller.StateComposer;
import miui.android.animation.internal.ThreadPoolUtil;
import miui.android.animation.property.FloatProperty;
import miui.android.animation.utils.CommonUtils;
import miui.android.animation.utils.KeyUtils;
import miui.android.animation.utils.LogUtils;
/* loaded from: classes.dex */
public class Folme {
    private static final long DELAY_TIME = 20000;
    private static final int MSG_TARGET = 1;
    private static AtomicReference<Float> sTimeRatio = new AtomicReference<>(Float.valueOf(1.0f));
    private static final ConcurrentHashMap<IAnimTarget, FolmeImpl> sImplMap = new ConcurrentHashMap<>();
    private static final Handler sMainHandler = new Handler(Looper.getMainLooper()) { // from class: miui.android.animation.Folme.2
        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                Folme.clearTargets();
                Folme.sendToTargetMessage(true);
                return;
            }
            super.handleMessage(msg);
        }
    };

    /* loaded from: classes.dex */
    public interface FontType {
        public static final int MITYPE = 1;
        public static final int MITYPE_MONO = 2;
        public static final int MIUI = 0;
    }

    /* loaded from: classes.dex */
    public interface FontWeight {
        public static final int BOLD = 8;
        public static final int DEMI_BOLD = 6;
        public static final int EXTRA_LIGHT = 1;
        public static final int HEAVY = 9;
        public static final int LIGHT = 2;
        public static final int MEDIUM = 5;
        public static final int NORMAL = 3;
        public static final int REGULAR = 4;
        public static final int SEMI_BOLD = 7;
        public static final int THIN = 0;
    }

    static {
        ThreadPoolUtil.post(new Runnable() { // from class: miui.android.animation.Folme.1
            @Override // java.lang.Runnable
            public void run() {
                LogUtils.getLogEnableInfo();
            }
        });
    }

    public static void useSystemAnimatorDurationScale(Context context) {
        float scale = Settings.Global.getFloat(context.getContentResolver(), "animator_duration_scale", 1.0f);
        sTimeRatio.set(Float.valueOf(scale));
    }

    public static void setAnimPlayRatio(float ratio) {
        sTimeRatio.set(Float.valueOf(ratio));
    }

    public static float getTimeRatio() {
        return sTimeRatio.get().floatValue();
    }

    public static <T> void post(T targetObject, Runnable task) {
        IAnimTarget target = getTarget(targetObject, null);
        if (target != null) {
            target.post(task);
        }
    }

    public static Collection<IAnimTarget> getTargets() {
        return sImplMap.keySet();
    }

    public static IVarFontStyle useVarFontAt(TextView view, int fontType, int initFontWeight) {
        return new FolmeFont().useAt(view, fontType, initFontWeight);
    }

    /* loaded from: classes.dex */
    public static class FolmeImpl implements IFolme {
        private IHoverStyle mHover;
        private IStateStyle mState;
        private IAnimTarget[] mTargets;
        private ITouchStyle mTouch;
        private IVisibleStyle mVisible;

        private FolmeImpl(IAnimTarget... target) {
            this.mTargets = target;
            Folme.sendToTargetMessage(false);
        }

        void clean() {
            ITouchStyle iTouchStyle = this.mTouch;
            if (iTouchStyle != null) {
                iTouchStyle.clean();
            }
            IVisibleStyle iVisibleStyle = this.mVisible;
            if (iVisibleStyle != null) {
                iVisibleStyle.clean();
            }
            IStateStyle iStateStyle = this.mState;
            if (iStateStyle != null) {
                iStateStyle.clean();
            }
            IHoverStyle iHoverStyle = this.mHover;
            if (iHoverStyle != null) {
                iHoverStyle.clean();
            }
        }

        void end() {
            ITouchStyle iTouchStyle = this.mTouch;
            if (iTouchStyle != null) {
                iTouchStyle.end(new Object[0]);
            }
            IVisibleStyle iVisibleStyle = this.mVisible;
            if (iVisibleStyle != null) {
                iVisibleStyle.end(new Object[0]);
            }
            IStateStyle iStateStyle = this.mState;
            if (iStateStyle != null) {
                iStateStyle.end(new Object[0]);
            }
            IHoverStyle iHoverStyle = this.mHover;
            if (iHoverStyle != null) {
                iHoverStyle.end(new Object[0]);
            }
        }

        @Override // miui.android.animation.IFolme
        public IHoverStyle hover() {
            if (this.mHover == null) {
                this.mHover = new FolmeHover(this.mTargets);
            }
            return this.mHover;
        }

        @Override // miui.android.animation.IFolme
        public ITouchStyle touch() {
            if (this.mTouch == null) {
                FolmeTouch touch = new FolmeTouch(this.mTargets);
                FolmeFont fontStyle = new FolmeFont();
                touch.setFontStyle(fontStyle);
                this.mTouch = touch;
            }
            return this.mTouch;
        }

        @Override // miui.android.animation.IFolme
        public IVisibleStyle visible() {
            if (this.mVisible == null) {
                this.mVisible = new FolmeVisible(this.mTargets);
            }
            return this.mVisible;
        }

        @Override // miui.android.animation.IFolme
        public IStateStyle state() {
            if (this.mState == null) {
                this.mState = StateComposer.composeStyle(this.mTargets);
            }
            return this.mState;
        }
    }

    public static IStateStyle useValue(Object... targetObj) {
        IFolme folme;
        if (targetObj.length > 0) {
            folme = useAt(getTarget(targetObj[0], ValueTarget.sCreator));
        } else {
            ValueTarget target = new ValueTarget();
            target.setFlags(1L);
            folme = useAt(target);
        }
        return folme.state();
    }

    public static IFolme useAt(IAnimTarget target) {
        ConcurrentHashMap<IAnimTarget, FolmeImpl> concurrentHashMap = sImplMap;
        FolmeImpl mc = concurrentHashMap.get(target);
        if (mc == null) {
            FolmeImpl mc2 = new FolmeImpl(new IAnimTarget[]{target});
            FolmeImpl prev = concurrentHashMap.putIfAbsent(target, mc2);
            return prev != null ? prev : mc2;
        }
        return mc;
    }

    public static IFolme useAt(View... views) {
        if (views.length == 0) {
            throw new IllegalArgumentException("useAt can not be applied to empty views array");
        }
        if (views.length == 1) {
            return useAt(getTarget(views[0], ViewTarget.sCreator));
        }
        IAnimTarget[] targets = new IAnimTarget[views.length];
        FolmeImpl impl = fillTargetArrayAndGetImpl(views, targets);
        if (impl == null) {
            impl = new FolmeImpl(targets);
            for (IAnimTarget target : targets) {
                FolmeImpl prevImpl = sImplMap.put(target, impl);
                if (prevImpl != null) {
                    prevImpl.clean();
                }
            }
        }
        return impl;
    }

    private static FolmeImpl fillTargetArrayAndGetImpl(View[] views, IAnimTarget[] targets) {
        boolean createImpl = false;
        FolmeImpl impl = null;
        for (int i = 0; i < views.length; i++) {
            targets[i] = getTarget(views[i], ViewTarget.sCreator);
            FolmeImpl cur = sImplMap.get(targets[i]);
            if (impl == null) {
                impl = cur;
            } else if (impl != cur) {
                createImpl = true;
            }
        }
        if (createImpl) {
            return null;
        }
        return impl;
    }

    @SafeVarargs
    public static <T> void clean(T... targetObjects) {
        if (CommonUtils.isArrayEmpty(targetObjects)) {
            for (IAnimTarget target : sImplMap.keySet()) {
                cleanAnimTarget(target);
            }
            return;
        }
        for (T targetObject : targetObjects) {
            doClean(targetObject);
        }
    }

    public static <T> void end(T... targetObjects) {
        FolmeImpl impl;
        for (T targetObject : targetObjects) {
            IAnimTarget target = getTarget(targetObject, null);
            if (target != null && (impl = sImplMap.get(target)) != null) {
                impl.end();
            }
        }
    }

    public static void onListViewTouchEvent(AbsListView listView, MotionEvent event) {
        ListViewTouchListener listener = FolmeTouch.getListViewTouchListener(listView);
        if (listener != null) {
            listener.onTouch(listView, event);
        }
    }

    private static <T> void doClean(T targetObject) {
        IAnimTarget target = getTarget(targetObject, null);
        cleanAnimTarget(target);
    }

    private static void cleanAnimTarget(IAnimTarget target) {
        if (target != null) {
            FolmeImpl impl = sImplMap.remove(target);
            target.animManager.clear();
            if (impl != null) {
                impl.clean();
            }
        }
    }

    public static <T> ValueTarget getValueTarget(T targetObject) {
        return (ValueTarget) getTarget(targetObject, ValueTarget.sCreator);
    }

    public static <T> IAnimTarget getTarget(T targetObject, ITargetCreator<T> creator) {
        IAnimTarget target;
        if (targetObject == null) {
            return null;
        }
        if (targetObject instanceof IAnimTarget) {
            return (IAnimTarget) targetObject;
        }
        for (IAnimTarget target2 : sImplMap.keySet()) {
            Object obj = target2.getTargetObject();
            if (obj != null && obj.equals(targetObject)) {
                return target2;
            }
        }
        if (creator == null || (target = creator.createTarget(targetObject)) == null) {
            return null;
        }
        useAt(target);
        return target;
    }

    public static void setDraggingState(View view, boolean isDragging) {
        if (isDragging) {
            view.setTag(KeyUtils.KEY_FOLME_IN_DRAGGING, true);
        } else {
            view.setTag(KeyUtils.KEY_FOLME_IN_DRAGGING, null);
        }
    }

    public static boolean isInDraggingState(View view) {
        return view.getTag(KeyUtils.KEY_FOLME_IN_DRAGGING) != null;
    }

    public static void getTargets(Collection<IAnimTarget> targets) {
        for (IAnimTarget target : sImplMap.keySet()) {
            if (!target.isValid() || (target.hasFlags(1L) && !target.animManager.isAnimRunning(new FloatProperty[0]))) {
                clean(target);
            } else {
                targets.add(target);
            }
        }
    }

    public static IAnimTarget getTargetById(int id) {
        for (IAnimTarget target : sImplMap.keySet()) {
            if (target.id == id) {
                return target;
            }
        }
        return null;
    }

    public static void clearTargets() {
        for (IAnimTarget target : sImplMap.keySet()) {
            if (!target.isValid() || (target.hasFlags(1L) && !target.animManager.isAnimRunning(new FloatProperty[0]))) {
                clean(target);
            }
        }
    }

    public static void sendToTargetMessage(boolean fromAuto) {
        clearTargetMessage();
        if (fromAuto && LogUtils.isLogEnabled()) {
            for (IAnimTarget target : sImplMap.keySet()) {
                LogUtils.debug("exist target:" + target.getTargetObject(), new Object[0]);
            }
        }
        if (sImplMap.size() > 0) {
            sMainHandler.sendEmptyMessageDelayed(1, 20000L);
        } else {
            clearTargetMessage();
        }
    }

    private static void clearTargetMessage() {
        Handler handler = sMainHandler;
        if (handler.hasMessages(1)) {
            handler.removeMessages(1);
        }
    }
}
