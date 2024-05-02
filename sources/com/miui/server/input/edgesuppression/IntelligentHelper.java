package com.miui.server.input.edgesuppression;

import com.miui.server.input.edgesuppression.SuppressionRect;
import java.util.ArrayList;
import java.util.Iterator;
/* loaded from: classes.dex */
public class IntelligentHelper extends BaseEdgeSuppression {
    private static final int INDEX_ABSOLUTE_FOURTH = 3;
    private static final int INDEX_ABSOLUTE_ONE = 0;
    private static final int INDEX_ABSOLUTE_THREE = 2;
    private static final int INDEX_ABSOLUTE_TWO = 1;
    private static final int INDEX_CONDITION_FOURTH = 7;
    private static final int INDEX_CONDITION_ONE = 4;
    private static final int INDEX_CONDITION_THREE = 6;
    private static final int INDEX_CONDITION_TWO = 5;
    private static volatile IntelligentHelper mInstance;
    private final ArrayList<SuppressionRect> mRectList = new ArrayList<>(8);

    private IntelligentHelper() {
    }

    public static IntelligentHelper getInstance() {
        if (mInstance == null) {
            synchronized (IntelligentHelper.class) {
                if (mInstance == null) {
                    mInstance = new IntelligentHelper();
                }
            }
        }
        return mInstance;
    }

    @Override // com.miui.server.input.edgesuppression.BaseEdgeSuppression
    public ArrayList<Integer> getEdgeSuppressionData(int rotation, int widthOfScreen, int heightOfScreen) {
        if (this.mRectList.isEmpty()) {
            initArrayList();
        }
        if (this.mIsHorizontal) {
            setHorizontalRectData(this.mAbsoluteSize, 2);
            setHorizontalRectData(this.mConditionSize, 1);
        } else {
            setPortraitRectData(this.mAbsoluteSize, 2);
            setPortraitRectData(this.mConditionSize, 1);
        }
        ArrayList<SuppressionRect> cornerList = getCornerData(rotation, widthOfScreen, heightOfScreen, this.mConnerWidth, this.mConnerHeight);
        this.mSendList.clear();
        getArrayList(this.mRectList);
        getArrayList(cornerList);
        return this.mSendList;
    }

    private void setHorizontalRectData(int size, int type) {
        switch (this.mHoldSensorState) {
            case -1:
            case 0:
            case 3:
                setHorizontalDataWithoutSensor(size, type);
                return;
            case 1:
                setRectValue(this.mRectList.get(getRectPosition(type, 0)), type, 0, 0, 0, this.mScreenWidth, size);
                setRectValue(this.mRectList.get(getRectPosition(type, 1)), type, 1, 0, 0, 0, 0);
                this.mRectList.set(getRectPosition(type, 2), new SuppressionRect.LeftTopHalfRect(type, this.mScreenHeight, size));
                this.mRectList.set(getRectPosition(type, 3), new SuppressionRect.RightTopHalfRect(type, this.mScreenHeight, this.mScreenWidth, size));
                return;
            case 2:
                setRectValue(this.mRectList.get(getRectPosition(type, 0)), type, 0, 0, 0, 0, 0);
                setRectValue(this.mRectList.get(getRectPosition(type, 1)), type, 1, 0, this.mScreenHeight - size, this.mScreenWidth, this.mScreenHeight);
                this.mRectList.set(getRectPosition(type, 2), new SuppressionRect.LeftBottomHalfRect(type, this.mScreenHeight, size));
                this.mRectList.set(getRectPosition(type, 3), new SuppressionRect.RightBottomHalfRect(type, this.mScreenHeight, this.mScreenWidth, size));
                return;
            default:
                return;
        }
    }

    private void setHorizontalDataWithoutSensor(int size, int type) {
        setRectValue(this.mRectList.get(getRectPosition(type, 0)), type, 0, 0, 0, this.mScreenWidth, size);
        setRectValue(this.mRectList.get(getRectPosition(type, 1)), type, 1, 0, this.mScreenHeight - size, this.mScreenWidth, this.mScreenHeight);
        setRectValue(this.mRectList.get(getRectPosition(type, 2)), type, 2, 0, 0, size, this.mScreenHeight);
        setRectValue(this.mRectList.get(getRectPosition(type, 3)), type, 3, this.mScreenWidth - size, 0, this.mScreenWidth, this.mScreenHeight);
    }

    private void setPortraitRectData(int size, int type) {
        setRectValue(this.mRectList.get(getRectPosition(type, 0)), type, 0, 0, 0, 0, 0);
        setRectValue(this.mRectList.get(getRectPosition(type, 1)), type, 1, 0, 0, 0, 0);
        if (type == 2) {
            this.mRectList.set(getRectPosition(type, 2), new SuppressionRect.LeftRect(type, this.mScreenHeight, size));
            this.mRectList.set(getRectPosition(type, 3), new SuppressionRect.RightRect(type, this.mScreenHeight, this.mScreenWidth, size));
        } else if (type == 1) {
            switch (this.mHoldSensorState) {
                case -1:
                case 0:
                case 3:
                    this.mRectList.set(getRectPosition(type, 2), new SuppressionRect.LeftRect(type, this.mScreenHeight, size));
                    this.mRectList.set(getRectPosition(type, 3), new SuppressionRect.RightRect(type, this.mScreenHeight, this.mScreenWidth, size));
                    return;
                case 1:
                    this.mRectList.set(getRectPosition(type, 2), new SuppressionRect.LeftTopHalfRect(type, this.mScreenHeight, size));
                    this.mRectList.set(getRectPosition(type, 3), new SuppressionRect.RightTopHalfRect(type, this.mScreenHeight, this.mScreenWidth, size));
                    return;
                case 2:
                    this.mRectList.set(getRectPosition(type, 2), new SuppressionRect.LeftBottomHalfRect(type, this.mScreenHeight, size));
                    this.mRectList.set(getRectPosition(type, 3), new SuppressionRect.RightBottomHalfRect(type, this.mScreenHeight, this.mScreenWidth, size));
                    return;
                default:
                    return;
            }
        }
    }

    private int getRectPosition(int type, int position) {
        int i = 2;
        switch (position) {
            case 0:
                int result = type == 2 ? 0 : 4;
                return result;
            case 1:
                int result2 = type == 2 ? 1 : 5;
                return result2;
            case 2:
                if (type != 2) {
                    i = 6;
                }
                int result3 = i;
                return result3;
            case 3:
                int result4 = type == 2 ? 3 : 7;
                return result4;
            default:
                return 0;
        }
    }

    private void initArrayList() {
        for (int i = 0; i < 8; i++) {
            this.mRectList.add(new SuppressionRect());
        }
    }

    private void getArrayList(ArrayList<SuppressionRect> arrayList) {
        Iterator<SuppressionRect> it = arrayList.iterator();
        while (it.hasNext()) {
            SuppressionRect suppressionRect = it.next();
            this.mSendList.addAll(suppressionRect.getList());
        }
    }
}
