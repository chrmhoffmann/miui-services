package miui.android.animation.controller;

import java.lang.reflect.Method;
import miui.android.animation.IAnimTarget;
import miui.android.animation.utils.StyleComposer;
/* loaded from: classes.dex */
public class StateComposer {
    private static final String METHOD_GET_STATE = "getState";
    private static final StyleComposer.IInterceptor<IFolmeStateStyle> sInterceptor = new StyleComposer.IInterceptor<IFolmeStateStyle>() { // from class: miui.android.animation.controller.StateComposer.1
        @Override // miui.android.animation.utils.StyleComposer.IInterceptor
        public boolean shouldIntercept(Method method, Object[] args) {
            return method.getName().equals(StateComposer.METHOD_GET_STATE);
        }

        public Object onMethod(Method method, Object[] args, IFolmeStateStyle[] styles) {
            if (styles.length > 0 && args.length > 0) {
                AnimState state = styles[0].getState(args[0]);
                for (int i = 1; i < styles.length; i++) {
                    styles[i].addState(state);
                }
                return state;
            }
            return null;
        }
    };

    private StateComposer() {
    }

    public static IFolmeStateStyle composeStyle(IAnimTarget... targets) {
        if (targets == null || targets.length == 0) {
            return null;
        }
        if (targets.length == 1) {
            return new FolmeState(targets[0]);
        }
        FolmeState[] array = new FolmeState[targets.length];
        for (int i = 0; i < targets.length; i++) {
            array[i] = new FolmeState(targets[i]);
        }
        return (IFolmeStateStyle) StyleComposer.compose(IFolmeStateStyle.class, sInterceptor, array);
    }
}
