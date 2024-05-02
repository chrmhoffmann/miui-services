package com.miui.server.input.edgesuppression;
/* loaded from: classes.dex */
public class EdgeSuppressionInfo {
    private int mAbsoluteSize;
    private int mConditionSize;
    private int mConnerHeight;
    private int mConnerWidth;
    private boolean mIsHorizontal;
    private String mType;

    public EdgeSuppressionInfo(int absoluteSize, int conditionSize, int connerWidth, int connerHeight, boolean isHorizontal, String type) {
        this.mAbsoluteSize = absoluteSize;
        this.mConditionSize = conditionSize;
        this.mConnerWidth = connerWidth;
        this.mConnerHeight = connerHeight;
        this.mIsHorizontal = isHorizontal;
        this.mType = type;
    }

    public int getAbsoluteSize() {
        return this.mAbsoluteSize;
    }

    public void setAbsoluteSize(int absoluteSize) {
        this.mAbsoluteSize = absoluteSize;
    }

    public int getConditionSize() {
        return this.mConditionSize;
    }

    public void setConditionSize(int conditionSize) {
        this.mConditionSize = conditionSize;
    }

    public int getConnerWidth() {
        return this.mConnerWidth;
    }

    public void setConnerWidth(int connerWidth) {
        this.mConnerWidth = connerWidth;
    }

    public int getConnerHeight() {
        return this.mConnerHeight;
    }

    public void setConnerHeight(int connerHeight) {
        this.mConnerHeight = connerHeight;
    }

    public boolean isHorizontal() {
        return this.mIsHorizontal;
    }

    public void setIsHorizontal(boolean isHorizontal) {
        this.mIsHorizontal = isHorizontal;
    }

    public String getType() {
        return this.mType;
    }

    public void setType(String type) {
        this.mType = type;
    }

    public String toString() {
        return "EdgeSuppressionInfo{mAbsoluteSize=" + this.mAbsoluteSize + ", mConditionSize=" + this.mConditionSize + ", mConnerWidth=" + this.mConnerWidth + ", mConnerHeight=" + this.mConnerHeight + ", mIsHorizontal=" + this.mIsHorizontal + ", mType='" + this.mType + "'}";
    }
}
