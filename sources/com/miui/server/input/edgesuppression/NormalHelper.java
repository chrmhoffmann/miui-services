package com.miui.server.input.edgesuppression;

import com.miui.server.input.edgesuppression.SuppressionRect;
import java.util.ArrayList;
import java.util.Iterator;
/* loaded from: classes.dex */
public class NormalHelper extends BaseEdgeSuppression {
    private static volatile NormalHelper mInstance;

    private NormalHelper() {
    }

    public static NormalHelper getInstance() {
        if (mInstance == null) {
            synchronized (NormalHelper.class) {
                if (mInstance == null) {
                    mInstance = new NormalHelper();
                }
            }
        }
        return mInstance;
    }

    @Override // com.miui.server.input.edgesuppression.BaseEdgeSuppression
    public ArrayList<Integer> getEdgeSuppressionData(int rotation, int widthOfScreen, int heightOfScreen) {
        ArrayList<SuppressionRect> list = new ArrayList<>();
        list.addAll(getEdgeSuppressionRectData(2, this.mAbsoluteSize, widthOfScreen, heightOfScreen));
        list.addAll(getEdgeSuppressionRectData(1, this.mConditionSize, widthOfScreen, heightOfScreen));
        list.addAll(getCornerData(rotation, widthOfScreen, heightOfScreen, this.mConnerWidth, this.mConnerHeight));
        this.mSendList.clear();
        Iterator<SuppressionRect> it = list.iterator();
        while (it.hasNext()) {
            SuppressionRect target = it.next();
            this.mSendList.addAll(target.getList());
        }
        return this.mSendList;
    }

    public ArrayList<SuppressionRect> getEdgeSuppressionRectData(int type, int size, int widthOfScreen, int heightOfScreen) {
        ArrayList<SuppressionRect> result = new ArrayList<>(4);
        result.add(new SuppressionRect.TopRect(this.mIsHorizontal, type, widthOfScreen, size));
        result.add(new SuppressionRect.BottomRect(this.mIsHorizontal, type, heightOfScreen, widthOfScreen, size));
        result.add(new SuppressionRect.LeftRect(type, heightOfScreen, size));
        result.add(new SuppressionRect.RightRect(type, heightOfScreen, widthOfScreen, size));
        return result;
    }
}
