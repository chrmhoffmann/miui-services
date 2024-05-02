package miui.android.animation.controller;

import miui.android.animation.IAnimTarget;
import miui.android.animation.IStateContainer;
import miui.android.animation.base.AnimConfig;
import miui.android.animation.property.FloatProperty;
/* loaded from: classes.dex */
public abstract class FolmeBase implements IStateContainer {
    IFolmeStateStyle mState;

    public FolmeBase(IAnimTarget... targets) {
        this.mState = StateComposer.composeStyle(targets);
    }

    @Override // miui.android.animation.IStateContainer
    public void clean() {
        IFolmeStateStyle iFolmeStateStyle = this.mState;
        if (iFolmeStateStyle != null) {
            iFolmeStateStyle.clean();
        }
    }

    @Override // miui.android.animation.ICancelableStyle
    public void end(Object... properties) {
        IFolmeStateStyle iFolmeStateStyle = this.mState;
        if (iFolmeStateStyle != null) {
            iFolmeStateStyle.end(properties);
        }
    }

    @Override // miui.android.animation.ICancelableStyle
    public void cancel() {
        IFolmeStateStyle iFolmeStateStyle = this.mState;
        if (iFolmeStateStyle != null) {
            iFolmeStateStyle.cancel();
        }
    }

    @Override // miui.android.animation.ICancelableStyle
    public void cancel(FloatProperty... properties) {
        IFolmeStateStyle iFolmeStateStyle = this.mState;
        if (iFolmeStateStyle != null) {
            iFolmeStateStyle.cancel(properties);
        }
    }

    @Override // miui.android.animation.ICancelableStyle
    public void cancel(String... propertyNames) {
        IFolmeStateStyle iFolmeStateStyle = this.mState;
        if (iFolmeStateStyle != null) {
            iFolmeStateStyle.cancel(propertyNames);
        }
    }

    @Override // miui.android.animation.IStateContainer
    public void enableDefaultAnim(boolean enable) {
        IFolmeStateStyle iFolmeStateStyle = this.mState;
        if (iFolmeStateStyle != null) {
            iFolmeStateStyle.enableDefaultAnim(enable);
        }
    }

    @Override // miui.android.animation.IStateContainer
    public void addConfig(Object tag, AnimConfig... configs) {
        IFolmeStateStyle iFolmeStateStyle = this.mState;
        if (iFolmeStateStyle != null) {
            iFolmeStateStyle.addConfig(tag, configs);
        }
    }
}
