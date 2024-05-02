package com.android.server.wm;

import android.graphics.Bitmap;
/* loaded from: classes.dex */
public class MiuiFreeFormFloatIconInfo {
    private int mFloatPivotHeight;
    private int mFloatPivotWidth;
    private Float mFloatPivotX;
    private Float mFloatPivotY;
    private Float mFloatVelocityX;
    private Float mFloatVelocityY;
    private Bitmap mIconBitMap;
    private int mTaskId;

    public int getFloatPivotHeight() {
        return this.mFloatPivotHeight;
    }

    public int getFloatPivotWidth() {
        return this.mFloatPivotWidth;
    }

    public Float getFloatPivotX() {
        return this.mFloatPivotX;
    }

    public Float getFloatPivotY() {
        return this.mFloatPivotY;
    }

    public Float getFloatVelocityX() {
        return this.mFloatVelocityX;
    }

    public Float getFloatVelocityY() {
        return this.mFloatVelocityY;
    }

    public Bitmap getIconBitmap() {
        return this.mIconBitMap;
    }

    public int getTaskId() {
        return this.mTaskId;
    }

    public void setFloatPivotHeight(int paramInt) {
        this.mFloatPivotHeight = paramInt;
    }

    public void setFloatPivotWidth(int paramInt) {
        this.mFloatPivotWidth = paramInt;
    }

    public void setFloatPivotX(Float paramFloat) {
        this.mFloatPivotX = paramFloat;
    }

    public void setFloatPivotY(Float paramFloat) {
        this.mFloatPivotY = paramFloat;
    }

    public void setFloatVelocityX(Float paramFloat) {
        this.mFloatVelocityX = paramFloat;
    }

    public void setFloatVelocityY(Float paramFloat) {
        this.mFloatVelocityY = paramFloat;
    }

    public void setIconBitmap(Bitmap paramBitmap) {
        this.mIconBitMap = paramBitmap;
    }

    public void setTaskId(int paramInt) {
        this.mTaskId = paramInt;
    }
}
