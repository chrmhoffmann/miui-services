package com.miui.server.input.gesture.multifingergesture;
/* loaded from: classes.dex */
public class MiuiMultiFingerGestureRect {
    private int mHeight;
    private int mWidth;

    public MiuiMultiFingerGestureRect() {
    }

    public MiuiMultiFingerGestureRect(int width, int height) {
        this.mWidth = width;
        this.mHeight = height;
    }

    public int getWidth() {
        return this.mWidth;
    }

    public void setWidth(int width) {
        this.mWidth = width;
    }

    public int getHeight() {
        return this.mHeight;
    }

    public void setHeight(int height) {
        this.mHeight = height;
    }
}
