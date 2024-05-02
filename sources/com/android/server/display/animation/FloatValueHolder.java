package com.android.server.display.animation;

import com.android.server.wm.MiuiFreeformPinManagerService;
/* loaded from: classes.dex */
public final class FloatValueHolder {
    private float mValue = MiuiFreeformPinManagerService.EDGE_AREA;

    public FloatValueHolder() {
    }

    public FloatValueHolder(float value) {
        setValue(value);
    }

    public void setValue(float value) {
        this.mValue = value;
    }

    public float getValue() {
        return this.mValue;
    }
}
