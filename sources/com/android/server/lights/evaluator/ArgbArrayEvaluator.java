package com.android.server.lights.evaluator;

import android.animation.ArgbEvaluator;
import android.animation.TypeEvaluator;
/* loaded from: classes.dex */
public class ArgbArrayEvaluator implements TypeEvaluator<int[]> {
    ArgbEvaluator mArgbEvaluator = new ArgbEvaluator();

    public int[] evaluate(float fraction, int[] startValue, int[] endValue) {
        if (startValue.length != endValue.length) {
            throw new ArrayIndexOutOfBoundsException();
        }
        int[] values = new int[startValue.length];
        for (int i = 0; i < startValue.length; i++) {
            values[i] = ((Integer) this.mArgbEvaluator.evaluate(fraction, Integer.valueOf(startValue[i]), Integer.valueOf(endValue[i]))).intValue();
        }
        return values;
    }
}
