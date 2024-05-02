package miui.android.animation.controller;

import java.util.Collection;
import miui.android.animation.IAnimTarget;
import miui.android.animation.IVisibleStyle;
import miui.android.animation.base.AnimConfig;
import miui.android.animation.listener.TransitionListener;
import miui.android.animation.listener.UpdateInfo;
import miui.android.animation.property.FloatProperty;
import miui.android.animation.property.ViewProperty;
import miui.android.animation.utils.CommonUtils;
import miui.android.animation.utils.EaseManager;
/* loaded from: classes.dex */
public class FolmeVisible extends FolmeBase implements IVisibleStyle {
    private final AnimConfig mDefConfig = new AnimConfig().addListeners(new TransitionListener() { // from class: miui.android.animation.controller.FolmeVisible.1
        @Override // miui.android.animation.listener.TransitionListener
        public void onBegin(Object toTag, Collection<UpdateInfo> updateList) {
            if (toTag.equals(IVisibleStyle.VisibleType.SHOW) && FolmeVisible.this.mSetBound) {
                AnimState.alignState(FolmeVisible.this.mState.getState(IVisibleStyle.VisibleType.HIDE), updateList);
            }
        }
    });
    private boolean mHasMove;
    private boolean mHasScale;
    private boolean mSetBound;

    public FolmeVisible(IAnimTarget... targets) {
        super(targets);
        useAutoAlpha(true);
    }

    @Override // miui.android.animation.controller.FolmeBase, miui.android.animation.IStateContainer
    public void clean() {
        super.clean();
        this.mHasScale = false;
        this.mHasMove = false;
    }

    @Override // miui.android.animation.IVisibleStyle
    public IVisibleStyle setBound(int left, int top, int width, int height) {
        this.mSetBound = true;
        this.mState.getState(IVisibleStyle.VisibleType.SHOW).add(ViewProperty.X, left).add(ViewProperty.Y, top).add(ViewProperty.WIDTH, width).add(ViewProperty.HEIGHT, height);
        return this;
    }

    @Override // miui.android.animation.IVisibleStyle
    public IVisibleStyle useAutoAlpha(boolean useAutoAlpha) {
        FloatProperty autoAlpha = ViewProperty.AUTO_ALPHA;
        FloatProperty alpha = ViewProperty.ALPHA;
        if (useAutoAlpha) {
            this.mState.getState(IVisibleStyle.VisibleType.SHOW).remove(alpha).add(autoAlpha, 1.0d);
            this.mState.getState(IVisibleStyle.VisibleType.HIDE).remove(alpha).add(autoAlpha, 0.0d);
        } else {
            this.mState.getState(IVisibleStyle.VisibleType.SHOW).remove(autoAlpha).add(alpha, 1.0d);
            this.mState.getState(IVisibleStyle.VisibleType.HIDE).remove(autoAlpha).add(alpha, 0.0d);
        }
        return this;
    }

    @Override // miui.android.animation.IVisibleStyle
    public IVisibleStyle setFlags(long flag) {
        this.mState.setFlags(flag);
        return this;
    }

    @Override // miui.android.animation.IVisibleStyle
    public IVisibleStyle setAlpha(float alpha, IVisibleStyle.VisibleType... type) {
        this.mState.getState(getType(type)).add(ViewProperty.AUTO_ALPHA, alpha);
        return this;
    }

    @Override // miui.android.animation.IVisibleStyle
    public IVisibleStyle setScale(float scale, IVisibleStyle.VisibleType... type) {
        this.mHasScale = true;
        this.mState.getState(getType(type)).add(ViewProperty.SCALE_Y, scale).add(ViewProperty.SCALE_X, scale);
        return this;
    }

    private IVisibleStyle.VisibleType getType(IVisibleStyle.VisibleType... type) {
        return type.length > 0 ? type[0] : IVisibleStyle.VisibleType.HIDE;
    }

    @Override // miui.android.animation.IVisibleStyle
    public IVisibleStyle setMove(int moveX, int moveY) {
        return setMove(moveX, moveY, IVisibleStyle.VisibleType.HIDE);
    }

    @Override // miui.android.animation.IVisibleStyle
    public IVisibleStyle setMove(int moveX, int moveY, IVisibleStyle.VisibleType... type) {
        boolean z = Math.abs(moveX) > 0 || Math.abs(moveY) > 0;
        this.mHasMove = z;
        if (z) {
            this.mState.getState(getType(type)).add(ViewProperty.X, moveX, 1).add(ViewProperty.Y, moveY, 1);
        }
        return this;
    }

    @Override // miui.android.animation.IVisibleStyle
    public IVisibleStyle setShowDelay(long delay) {
        this.mState.getState(IVisibleStyle.VisibleType.SHOW).getConfig().delay = delay;
        return this;
    }

    @Override // miui.android.animation.IVisibleStyle
    public void show(AnimConfig... config) {
        this.mState.to(IVisibleStyle.VisibleType.SHOW, getConfig(IVisibleStyle.VisibleType.SHOW, config));
    }

    @Override // miui.android.animation.IVisibleStyle
    public void hide(AnimConfig... config) {
        this.mState.to(IVisibleStyle.VisibleType.HIDE, getConfig(IVisibleStyle.VisibleType.HIDE, config));
    }

    @Override // miui.android.animation.IVisibleStyle
    public IVisibleStyle setShow() {
        this.mState.setTo(IVisibleStyle.VisibleType.SHOW);
        return this;
    }

    @Override // miui.android.animation.IVisibleStyle
    public IVisibleStyle setHide() {
        this.mState.setTo(IVisibleStyle.VisibleType.HIDE);
        return this;
    }

    private AnimConfig[] getConfig(IVisibleStyle.VisibleType type, AnimConfig... config) {
        EaseManager.EaseStyle easeStyle;
        EaseManager.EaseStyle easeStyle2;
        EaseManager.EaseStyle easeStyle3;
        EaseManager.EaseStyle easeStyle4;
        boolean z = this.mHasScale;
        if (!z && !this.mHasMove) {
            AnimConfig animConfig = this.mDefConfig;
            if (type == IVisibleStyle.VisibleType.SHOW) {
                easeStyle4 = EaseManager.getStyle(16, 300.0f);
            } else {
                easeStyle4 = EaseManager.getStyle(-2, 1.0f, 0.15f);
            }
            animConfig.setEase(easeStyle4);
        } else if (z && !this.mHasMove) {
            AnimConfig animConfig2 = this.mDefConfig;
            if (type == IVisibleStyle.VisibleType.SHOW) {
                easeStyle3 = EaseManager.getStyle(-2, 0.6f, 0.35f);
            } else {
                easeStyle3 = EaseManager.getStyle(-2, 0.75f, 0.2f);
            }
            animConfig2.setEase(easeStyle3);
        } else if (!z) {
            AnimConfig animConfig3 = this.mDefConfig;
            if (type == IVisibleStyle.VisibleType.SHOW) {
                easeStyle2 = EaseManager.getStyle(-2, 0.75f, 0.35f);
            } else {
                easeStyle2 = EaseManager.getStyle(-2, 0.75f, 0.25f);
            }
            animConfig3.setEase(easeStyle2);
        } else {
            AnimConfig animConfig4 = this.mDefConfig;
            if (type == IVisibleStyle.VisibleType.SHOW) {
                easeStyle = EaseManager.getStyle(-2, 0.65f, 0.35f);
            } else {
                easeStyle = EaseManager.getStyle(-2, 0.75f, 0.25f);
            }
            animConfig4.setEase(easeStyle);
        }
        return (AnimConfig[]) CommonUtils.mergeArray(config, this.mDefConfig);
    }
}
