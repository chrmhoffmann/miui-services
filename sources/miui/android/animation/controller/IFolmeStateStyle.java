package miui.android.animation.controller;

import miui.android.animation.IAnimTarget;
import miui.android.animation.IStateStyle;
/* loaded from: classes.dex */
public interface IFolmeStateStyle extends IStateStyle {
    void addState(AnimState animState);

    AnimState getState(Object obj);

    IAnimTarget getTarget();
}
