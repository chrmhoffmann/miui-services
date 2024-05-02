package com.android.server.wm;

import android.graphics.Rect;
import android.view.SurfaceControl;
import android.view.animation.Animation;
import android.view.animation.Transformation;
/* loaded from: classes.dex */
class AnimationDimmer extends Animation {
    float alpha;
    boolean isVisible;
    private Rect mClipRect;
    float mCornerRadius;
    SurfaceControl mDimLayer;
    private float mFromAlpha;
    private WindowContainer mHost;
    private float mToAlpha;

    public AnimationDimmer(float fromAlpha, float toAlpha, Rect rect, WindowContainer host, float cornerRadius) {
        this(fromAlpha, toAlpha, rect, host);
        this.mCornerRadius = cornerRadius;
    }

    public AnimationDimmer(float fromAlpha, float toAlpha, Rect rect, WindowContainer host) {
        this(fromAlpha, toAlpha, host);
        this.mClipRect = rect;
    }

    public AnimationDimmer(float fromAlpha, float toAlpha, WindowContainer host) {
        this.alpha = 0.1f;
        this.mCornerRadius = MiuiFreeformPinManagerService.EDGE_AREA;
        this.mFromAlpha = fromAlpha;
        this.mToAlpha = toAlpha;
        this.mHost = host;
    }

    private SurfaceControl makeDimLayer() {
        WindowContainer windowContainer = this.mHost;
        if ((windowContainer instanceof ActivityRecord) || (windowContainer instanceof Task)) {
            return windowContainer.makeChildSurface((WindowContainer) null).setParent(this.mHost.getSurfaceControl()).setColorLayer().setName("Transition Dim Layer for - " + this.mHost.getName()).build();
        }
        return null;
    }

    private void dim(SurfaceControl.Transaction t, WindowContainer container, int relativeLayer, float alpha) {
        synchronized (this) {
            if (this.mDimLayer == null) {
                this.mDimLayer = makeDimLayer();
            }
            SurfaceControl surfaceControl = this.mDimLayer;
            if (surfaceControl == null) {
                return;
            }
            if (container != null) {
                t.setRelativeLayer(surfaceControl, container.getSurfaceControl(), relativeLayer);
            } else {
                t.setLayer(surfaceControl, Integer.MAX_VALUE);
            }
            if (Math.abs(this.mCornerRadius) > 1.0E-6d) {
                t.setCornerRadius(this.mDimLayer, this.mCornerRadius);
            }
            t.setAlpha(this.mDimLayer, alpha);
            this.alpha = alpha;
            t.show(this.mDimLayer);
            this.isVisible = true;
        }
    }

    public void stopDim(SurfaceControl.Transaction t) {
        synchronized (this) {
            SurfaceControl surfaceControl = this.mDimLayer;
            if (surfaceControl != null && surfaceControl.isValid()) {
                t.hide(this.mDimLayer);
                t.remove(this.mDimLayer);
                this.mDimLayer = null;
            }
        }
        this.isVisible = false;
        this.mClipRect = null;
        this.mCornerRadius = MiuiFreeformPinManagerService.EDGE_AREA;
    }

    public void dimAbove(SurfaceControl.Transaction t, WindowContainer container) {
        dim(t, container, 1, this.mFromAlpha);
    }

    void setAlpha(SurfaceControl.Transaction t, float alpha) {
        if (!this.isVisible) {
            return;
        }
        synchronized (this) {
            SurfaceControl surfaceControl = this.mDimLayer;
            if (surfaceControl != null && surfaceControl.isValid()) {
                t.setAlpha(this.mDimLayer, alpha);
            }
        }
    }

    @Override // android.view.animation.Animation
    protected void applyTransformation(float interpolatedTime, Transformation t) {
        float tmpAlpha = this.mFromAlpha;
        this.alpha = ((this.mToAlpha - tmpAlpha) * interpolatedTime) + tmpAlpha;
    }

    public void stepTransitionDim(SurfaceControl.Transaction t) {
        setAlpha(t, this.alpha);
    }

    public void dimBelow(SurfaceControl.Transaction t, WindowContainer container) {
        dim(t, container, -1, this.mFromAlpha);
    }

    public SurfaceControl getDimmer() {
        return this.mDimLayer;
    }

    public Rect getClipRect() {
        return this.mClipRect;
    }
}
