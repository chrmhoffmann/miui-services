package com.android.server.wm;

import android.view.animation.Interpolator;
/* loaded from: classes.dex */
public class PhysicBasedInterpolator implements Interpolator {
    private float c;
    private float c2;
    private float k;
    private float r;
    private float w;
    private float mInitial = -1.0f;
    private float m = 1.0f;
    private float c1 = -1.0f;

    public PhysicBasedInterpolator(float damping, float response) {
        double pow = Math.pow(6.283185307179586d / response, 2.0d);
        float f = this.m;
        float f2 = (float) (pow * f);
        this.k = f2;
        float f3 = (float) (((damping * 12.566370614359172d) * f) / response);
        this.c = f3;
        float f4 = this.m;
        float sqrt = ((float) Math.sqrt(((f * 4.0f) * f2) - (f3 * f3))) / (f4 * 2.0f);
        this.w = sqrt;
        float f5 = -((this.c / 2.0f) * f4);
        this.r = f5;
        this.c2 = (MiuiFreeformPinManagerService.EDGE_AREA - (f5 * this.mInitial)) / sqrt;
    }

    @Override // android.animation.TimeInterpolator
    public float getInterpolation(float input) {
        float res = (float) ((Math.pow(2.718281828459045d, this.r * input) * ((this.c1 * Math.cos(this.w * input)) + (this.c2 * Math.sin(this.w * input)))) + 1.0d);
        if (res > 1.0f) {
            return 1.0f;
        }
        return res;
    }

    /* loaded from: classes.dex */
    public static final class Builder {
        private float mDamping = 0.95f;
        private float mResponse = 0.6f;

        public Builder setDamping(float damping) {
            this.mDamping = damping;
            return this;
        }

        public Builder setResponse(float response) {
            this.mResponse = response;
            return this;
        }

        public PhysicBasedInterpolator build() {
            return new PhysicBasedInterpolator(this.mDamping, this.mResponse);
        }
    }
}
