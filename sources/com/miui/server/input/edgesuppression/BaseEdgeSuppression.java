package com.miui.server.input.edgesuppression;

import android.content.Context;
import android.provider.Settings;
import android.util.Slog;
import java.util.ArrayList;
import miui.util.ITouchFeature;
/* loaded from: classes.dex */
public abstract class BaseEdgeSuppression {
    protected static final int ABSOLUTE = 2;
    protected static final int CONDITION = 1;
    private static final int CORNER = 0;
    protected static final int EMPTY_POINT = 0;
    private static final int MAX_SHRINK_SIZE = 45;
    private static final int MIN_SHRINK_SIZE = 10;
    protected static final int RECT_FIRST = 0;
    protected static final int RECT_FOURTH = 3;
    protected static final int RECT_SECOND = 1;
    protected static final int RECT_THIRD = 2;
    protected static final String TAG = "EdgeSuppressionManager";
    protected int mAbsoluteSize;
    protected int mHoldSensorState;
    protected boolean mIsHorizontal;
    protected int mRotation;
    protected int mScreenHeight;
    protected int mScreenWidth;
    protected int mTargetId;
    protected ArrayList<Integer> mSendList = new ArrayList<>();
    protected int mConditionSize = 0;
    protected int mConnerWidth = 0;
    protected int mConnerHeight = 0;

    abstract ArrayList<Integer> getEdgeSuppressionData(int i, int i2, int i3);

    private boolean hasParamChanged(EdgeSuppressionInfo info, int widthOfScreen, int heightOfScreen) {
        return (this.mConditionSize == info.getConditionSize() && this.mConnerWidth == info.getConnerWidth() && this.mConnerHeight == info.getConnerHeight() && this.mScreenWidth == widthOfScreen && this.mConnerHeight == heightOfScreen) ? false : true;
    }

    public void updateInterNalParam(EdgeSuppressionInfo info, int holdSensorState, int rotation, int targetId, int widthOfScreen, int heightOfScreen) {
        this.mRotation = rotation;
        this.mTargetId = targetId;
        this.mHoldSensorState = holdSensorState;
        if (hasParamChanged(info, widthOfScreen, heightOfScreen)) {
            this.mConditionSize = info.getConditionSize();
            this.mAbsoluteSize = info.getAbsoluteSize();
            this.mConnerWidth = info.getConnerWidth();
            this.mConnerHeight = info.getConnerHeight();
            this.mIsHorizontal = info.isHorizontal();
            this.mScreenWidth = widthOfScreen;
            this.mScreenHeight = heightOfScreen;
            this.mSendList = getEdgeSuppressionData(rotation, widthOfScreen, heightOfScreen);
        }
    }

    public void syncDataToKernel() {
        Slog.i(TAG, this.mSendList.toString());
        ITouchFeature iTouchFeature = ITouchFeature.getInstance();
        int i = this.mTargetId;
        ArrayList<Integer> arrayList = this.mSendList;
        iTouchFeature.setEdgeMode(i, 15, arrayList, arrayList.size());
    }

    public ArrayList<SuppressionRect> getCornerData(int rotation, int widthOfScreen, int heightOfScreen, int connerWidth, int connerHeight) {
        ArrayList<SuppressionRect> result = new ArrayList<>(4);
        for (int i = 0; i < 4; i++) {
            result.add(new SuppressionRect());
        }
        switch (rotation) {
            case 0:
                setRectValue(result.get(0), 0, 0, 0, 0, 0, 0);
                setRectValue(result.get(1), 0, 1, 0, 0, 0, 0);
                setRectValue(result.get(2), 0, 2, 0, heightOfScreen - connerHeight, connerWidth, heightOfScreen);
                setRectValue(result.get(3), 0, 3, widthOfScreen - connerWidth, heightOfScreen - connerHeight, widthOfScreen, heightOfScreen);
                break;
            case 1:
                setRectValue(result.get(0), 0, 0, 0, 0, connerWidth, connerHeight);
                setRectValue(result.get(1), 0, 1, 0, 0, 0, 0);
                setRectValue(result.get(2), 0, 2, 0, heightOfScreen - connerHeight, connerWidth, heightOfScreen);
                setRectValue(result.get(3), 0, 3, 0, 0, 0, 0);
                break;
            case 2:
                setRectValue(result.get(0), 0, 0, 0, 0, connerWidth, connerHeight);
                setRectValue(result.get(1), 0, 1, widthOfScreen - connerWidth, 0, widthOfScreen, connerHeight);
                setRectValue(result.get(2), 0, 2, 0, 0, 0, 0);
                setRectValue(result.get(3), 0, 3, 0, 0, 0, 0);
                break;
            case 3:
                setRectValue(result.get(0), 0, 0, 0, 0, 0, 0);
                setRectValue(result.get(1), 0, 1, widthOfScreen - connerWidth, 0, widthOfScreen, connerHeight);
                setRectValue(result.get(2), 0, 2, 0, 0, 0, 0);
                setRectValue(result.get(3), 0, 3, widthOfScreen - connerWidth, heightOfScreen - connerHeight, widthOfScreen, heightOfScreen);
                break;
        }
        return result;
    }

    public void setRectValue(SuppressionRect suppressionRect, int type, int position, int startX, int startY, int endX, int endY) {
        suppressionRect.setValue(type, position, startX, startY, endX, endY);
    }

    public void initInputMethodData(Context context, int size) {
        int verticalSize = Settings.Global.getInt(context.getContentResolver(), EdgeSuppressionManager.VERTICAL_EDGE_SUPPRESSION_SIZE, -1);
        int horizontalSize = Settings.Global.getInt(context.getContentResolver(), EdgeSuppressionManager.HORIZONTAL_EDGE_SUPPRESSION_SIZE, -1);
        if (verticalSize == -1 && horizontalSize == -1 && EdgeSuppressionManager.IS_SUPPORT_EDGE_MODE && !EdgeSuppressionManager.SHOULD_REMOVE_EDGE_SETTINGS) {
            Settings.Global.putInt(context.getContentResolver(), EdgeSuppressionManager.VERTICAL_EDGE_SUPPRESSION_SIZE, size);
            Settings.Global.putInt(context.getContentResolver(), EdgeSuppressionManager.HORIZONTAL_EDGE_SUPPRESSION_SIZE, size);
        }
    }

    public void setInputMethodSize(Context context) {
        boolean shouldOverWrite = true;
        int verticalSize = Settings.Global.getInt(context.getContentResolver(), EdgeSuppressionManager.VERTICAL_EDGE_SUPPRESSION_SIZE, -1);
        int horizontalSize = Settings.Global.getInt(context.getContentResolver(), EdgeSuppressionManager.HORIZONTAL_EDGE_SUPPRESSION_SIZE, -1);
        if (!EdgeSuppressionManager.IS_SUPPORT_EDGE_MODE || EdgeSuppressionManager.SHOULD_REMOVE_EDGE_SETTINGS) {
            verticalSize = -1;
            horizontalSize = -1;
        } else if (verticalSize > MAX_SHRINK_SIZE || horizontalSize > MAX_SHRINK_SIZE) {
            verticalSize = MAX_SHRINK_SIZE;
            horizontalSize = MAX_SHRINK_SIZE;
        } else if (verticalSize < 10 || horizontalSize < 10) {
            verticalSize = 10;
            horizontalSize = 10;
        } else {
            shouldOverWrite = false;
        }
        if (shouldOverWrite) {
            Settings.Global.putInt(context.getContentResolver(), EdgeSuppressionManager.VERTICAL_EDGE_SUPPRESSION_SIZE, verticalSize);
            Settings.Global.putInt(context.getContentResolver(), EdgeSuppressionManager.HORIZONTAL_EDGE_SUPPRESSION_SIZE, horizontalSize);
        }
        Slog.i(TAG, "InputMethod ShrinkSize: ShouldOverWrite = " + shouldOverWrite + " verticalSize = " + verticalSize + " horizontalSize = " + horizontalSize);
    }
}
