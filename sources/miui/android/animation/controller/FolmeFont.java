package miui.android.animation.controller;

import android.widget.TextView;
import miui.android.animation.Folme;
import miui.android.animation.IAnimTarget;
import miui.android.animation.IVarFontStyle;
import miui.android.animation.ViewTarget;
import miui.android.animation.base.AnimConfig;
import miui.android.animation.font.FontWeightProperty;
import miui.android.animation.utils.CommonUtils;
import miui.android.animation.utils.EaseManager;
/* loaded from: classes.dex */
public class FolmeFont extends FolmeBase implements IVarFontStyle {
    private AnimConfig mDefaultTo;
    private int mInitValue;
    private boolean mIsInitSet;
    private FontWeightProperty mProperty;

    /* loaded from: classes.dex */
    public enum FontType {
        INIT,
        TARGET
    }

    public FolmeFont() {
        super(new IAnimTarget[0]);
        AnimConfig animConfig = new AnimConfig();
        this.mDefaultTo = animConfig;
        animConfig.setEase(EaseManager.getStyle(0, 350.0f, 0.9f, 0.86f));
    }

    @Override // miui.android.animation.IVarFontStyle
    public IVarFontStyle useAt(TextView textView, int initFontType, int fromFontWeight) {
        this.mState = new FolmeState(Folme.getTarget(textView, ViewTarget.sCreator));
        this.mProperty = new FontWeightProperty(textView, initFontType);
        this.mInitValue = fromFontWeight;
        this.mState.getState(FontType.INIT).add(this.mProperty, fromFontWeight);
        this.mIsInitSet = false;
        return this;
    }

    @Override // miui.android.animation.controller.FolmeBase, miui.android.animation.IStateContainer
    public void clean() {
        super.clean();
        this.mState = null;
        this.mProperty = null;
        this.mInitValue = 0;
    }

    @Override // miui.android.animation.IVarFontStyle
    public IVarFontStyle to(int toFontWeight, AnimConfig... config) {
        if (this.mState != null) {
            if (!this.mIsInitSet) {
                this.mIsInitSet = true;
                this.mState.setTo(FontType.INIT);
            }
            AnimConfig[] configArray = (AnimConfig[]) CommonUtils.mergeArray(config, this.mDefaultTo);
            if (this.mInitValue == toFontWeight) {
                this.mState.to(FontType.INIT, configArray);
            } else {
                this.mState.getState(FontType.TARGET).add(this.mProperty, toFontWeight);
                this.mState.to(FontType.TARGET, configArray);
            }
        }
        return this;
    }

    @Override // miui.android.animation.IVarFontStyle
    public IVarFontStyle setTo(int toFontWeight) {
        if (this.mState != null) {
            this.mState.getState(FontType.TARGET).add(this.mProperty, toFontWeight);
            this.mState.setTo(FontType.TARGET);
        }
        return this;
    }

    @Override // miui.android.animation.IVarFontStyle
    public IVarFontStyle fromTo(int fromFontWeight, int toFontWeight, AnimConfig... config) {
        if (this.mState != null) {
            this.mState.getState(FontType.INIT).add(this.mProperty, fromFontWeight);
            this.mState.getState(FontType.TARGET).add(this.mProperty, toFontWeight);
            this.mState.fromTo(FontType.INIT, FontType.TARGET, config);
        }
        return this;
    }
}
