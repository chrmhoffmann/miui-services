package com.android.server.wm;

import android.graphics.Rect;
import android.view.SurfaceControl;
import android.view.animation.Animation;
import android.view.animation.Transformation;
/* loaded from: classes.dex */
public class SplitDimmer extends Animation {
    private static final String TAG = "SplitDimmer";
    float alpha;
    boolean isVisible;
    private Rect mClipRect;
    SurfaceControl mDimLayer;
    private float mFromAlpha;
    private WindowContainer mHost;
    private float mToAlpha;

    public SplitDimmer(float fromAlpha, float toAlpha, Rect rect, WindowContainer host) {
        this(fromAlpha, toAlpha, host);
        this.mClipRect = rect;
    }

    public SplitDimmer(float fromAlpha, float toAlpha, WindowContainer host) {
        this.alpha = 0.1f;
        this.mFromAlpha = fromAlpha;
        this.mToAlpha = toAlpha;
        this.mHost = host;
    }

    private SurfaceControl makeDimLayer() {
        return this.mHost.makeChildSurface((WindowContainer) null).setParent(this.mHost.getSurfaceControl()).setColorLayer().setName("Split Dim Layer for - " + this.mHost.getName()).build();
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
            Rect rect = this.mClipRect;
            if (rect != null) {
                t.setWindowCrop(this.mDimLayer, rect.right - this.mClipRect.left, this.mClipRect.bottom - this.mClipRect.top);
                t.setPosition(this.mDimLayer, this.mClipRect.left, this.mClipRect.top);
            }
            t.setAlpha(this.mDimLayer, alpha);
            this.alpha = alpha;
            t.show(this.mDimLayer);
            this.isVisible = true;
        }
    }

    public void dimAbove(SurfaceControl.Transaction t, WindowContainer container) {
        dim(t, container, 1, this.mFromAlpha);
    }

    public void stopDim(SurfaceControl.Transaction t) {
        synchronized (this) {
            SurfaceControl surfaceControl = this.mDimLayer;
            if (surfaceControl != null && surfaceControl.isValid()) {
                t.hide(this.mDimLayer);
                this.isVisible = false;
                t.remove(this.mDimLayer);
                this.mDimLayer = null;
                this.mClipRect = null;
            }
        }
    }

    void setAlpha(SurfaceControl.Transaction t, float alpha) {
        synchronized (this) {
            SurfaceControl surfaceControl = this.mDimLayer;
            if (surfaceControl == null) {
                return;
            }
            if (!this.isVisible) {
                return;
            }
            t.setAlpha(surfaceControl, alpha);
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
}
