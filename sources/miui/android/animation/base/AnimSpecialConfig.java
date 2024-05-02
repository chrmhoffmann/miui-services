package miui.android.animation.base;

import miui.android.animation.property.FloatProperty;
import miui.android.animation.utils.EaseManager;
/* loaded from: classes.dex */
public class AnimSpecialConfig extends AnimConfig {
    public double maxValue;
    public double minValue;

    public AnimSpecialConfig() {
        super(true);
    }

    public AnimSpecialConfig setMinAndMax(double min, double max) {
        this.minValue = min;
        this.maxValue = max;
        return this;
    }

    @Override // miui.android.animation.base.AnimConfig
    public AnimConfig setSpecial(String name, long delay, float... velocity) {
        super.setSpecial(this, (EaseManager.EaseStyle) null, delay, velocity);
        return this;
    }

    @Override // miui.android.animation.base.AnimConfig
    public AnimConfig setSpecial(String name, EaseManager.EaseStyle ease, float... velocity) {
        super.setSpecial(this, ease, -1L, velocity);
        return this;
    }

    @Override // miui.android.animation.base.AnimConfig
    public AnimConfig setSpecial(String name, EaseManager.EaseStyle ease, long delay, float... velocity) {
        super.setSpecial(this, ease, delay, velocity);
        return this;
    }

    @Override // miui.android.animation.base.AnimConfig
    public AnimConfig setSpecial(FloatProperty property, long delay, float... velocity) {
        super.setSpecial(this, (EaseManager.EaseStyle) null, delay, velocity);
        return this;
    }

    @Override // miui.android.animation.base.AnimConfig
    public AnimConfig setSpecial(FloatProperty property, EaseManager.EaseStyle ease, float... velocity) {
        super.setSpecial(this, ease, -1L, velocity);
        return this;
    }

    @Override // miui.android.animation.base.AnimConfig
    public AnimConfig setSpecial(FloatProperty property, EaseManager.EaseStyle ease, long delay, float... velocity) {
        super.setSpecial(this, ease, delay, velocity);
        return this;
    }

    @Override // miui.android.animation.base.AnimConfig
    @Deprecated
    public AnimSpecialConfig getSpecialConfig(FloatProperty property) {
        return this;
    }

    @Override // miui.android.animation.base.AnimConfig
    @Deprecated
    public AnimSpecialConfig queryAndCreateSpecial(FloatProperty property) {
        return this;
    }

    @Override // miui.android.animation.base.AnimConfig
    @Deprecated
    public AnimSpecialConfig queryAndCreateSpecial(String name) {
        return this;
    }

    @Override // miui.android.animation.base.AnimConfig
    @Deprecated
    public AnimSpecialConfig getSpecialConfig(String name) {
        return this;
    }

    @Override // miui.android.animation.base.AnimConfig
    @Deprecated
    public AnimConfig setSpecial(FloatProperty property, AnimSpecialConfig sc) {
        return this;
    }

    @Override // miui.android.animation.base.AnimConfig
    @Deprecated
    public AnimConfig setSpecial(String name, AnimSpecialConfig sc) {
        return this;
    }
}
