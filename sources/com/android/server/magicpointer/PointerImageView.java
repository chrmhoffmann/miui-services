package com.android.server.magicpointer;

import android.content.Context;
import android.graphics.Outline;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.ImageView;
/* loaded from: classes.dex */
public class PointerImageView extends ImageView {
    private float radius;

    public PointerImageView(Context context) {
        super(context);
    }

    public PointerImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PointerImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public PointerImageView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public float getRadius() {
        return this.radius;
    }

    public void setRadius(final float radius) {
        this.radius = radius;
        setOutlineProvider(new ViewOutlineProvider() { // from class: com.android.server.magicpointer.PointerImageView.1
            @Override // android.view.ViewOutlineProvider
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), radius);
            }
        });
        setClipToOutline(true);
    }
}
