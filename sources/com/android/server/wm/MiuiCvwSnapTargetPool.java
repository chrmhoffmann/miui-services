package com.android.server.wm;

import android.content.res.Configuration;
import android.graphics.Rect;
import android.view.DisplayInfo;
import com.android.server.wm.MiuiCvwGestureController;
import com.android.server.wm.MiuiCvwSnapTargetPool;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.function.Consumer;
/* loaded from: classes.dex */
public class MiuiCvwSnapTargetPool {
    private static final float DEFAULT_LARGE_GENERAL_MINI_RATIO = 0.75f;
    static final float DEFAULT_LARGE_GENERAL_RATIO = 0.75f;
    private static final float DEFAULT_LARGE_GENERAL_WIDTH = 1200.0f;
    private static final float DEFAULT_MAX_DISPLAY_LANDSCAPE_HEIGHT = 1127.0f;
    private static final float DEFAULT_MAX_DISPLAY_PORTRAIT_HEIGHT = 1425.0f;
    private static final float DEFAULT_MINI_MIN_DISPLAY_LANDSCAPE_HEIGHT = 320.0f;
    private static final float DEFAULT_MINI_MIN_DISPLAY_PORTRAIT_HEIGHT = 409.0f;
    private static final float DEFAULT_MIN_DISPLAY_LANDSCAPE_HEIGHT = 661.88f;
    private static final float DEFAULT_MIN_DISPLAY_PORTRAIT_HEIGHT = 840.0f;
    private static final float DEFAULT_XLARGE_GENERAL_MINI_RATIO = 1.3333334f;
    static final float DEFAULT_XLARGE_GENERAL_RATIO = 1.3333334f;
    private static final float DEFAULT_XLARGE_GENERAL_WIDTH = 2133.33f;
    private static final float DEFAULT_XLARGE_WIDE_MINI_RATIO = 0.5625f;
    static final float DEFAULT_XLARGE_WIDE_RATIO = 0.5625f;
    private static final float DEFAULT_XLARGE_WIDE_WIDTH = 960.0f;
    private static final float MAX_LARGE_GENERAL_HEIGHT = 1425.0f;
    private static final float MAX_XLARGE_GENERAL_HEIGHT = 1200.0f;
    private static final float MAX_XLARGE_WIDE_HEIGHT = 1425.0f;
    private static final float MINI_LARGE_GENERAL_HEIGHT = 409.0f;
    private static final float MINI_XLARGE_GENERAL_HEIGHT = 409.0f;
    private static final float MINI_XLARGE_WIDE_HEIGHT = 409.0f;
    private static final float MIN_LARGE_GENERAL_HEIGHT = 840.0f;
    private static final float MIN_XLARGE_GENERAL_HEIGHT = 840.0f;
    private static final float MIN_XLARGE_WIDE_HEIGHT = 840.0f;
    private static final String TAG = MiuiCvwSnapTargetPool.class.getSimpleName();
    private final DisplayContent mDisplayContent;
    private int mDisplayHeight;
    public SnapTarget mDisplayHorizontalSnapTarget;
    private float mDisplayRatio;
    public SnapTarget mDisplayVerticalSnapTarget;
    private int mDisplayWidth;
    private SnapTarget mFirstSnapTarget;
    public SnapTarget mLargeGeneralSnapTarget;
    private SnapTarget mLastSnapTarget;
    private float mRangeRatio;
    private final float mScreenDensity;
    public SnapTarget mXlargeGeneralSnapTarget;
    public SnapTarget mXlargeWideSnapTarget;
    private final ArrayList<SnapTarget> mTargets = new ArrayList<>();
    private float mDisplayLandscapeRatio = 1.0f;
    private float mDisplayPortraitRatio = 1.0f;
    private boolean mFreeSnapMode = false;

    public MiuiCvwSnapTargetPool(DisplayContent displayContent) {
        this.mDisplayContent = displayContent;
        DisplayInfo displayInfo = displayContent.getDisplayInfo();
        this.mDisplayWidth = displayInfo.logicalWidth;
        this.mDisplayHeight = displayInfo.logicalHeight;
        this.mScreenDensity = displayInfo.logicalDensityDpi / 160.0f;
        initTargets();
    }

    public void updateTargets(Configuration configuration) {
        int longSide = Math.max(this.mDisplayContent.mBaseDisplayHeight, this.mDisplayContent.mBaseDisplayWidth);
        int shortSide = Math.min(this.mDisplayContent.mBaseDisplayHeight, this.mDisplayContent.mBaseDisplayWidth);
        if (configuration.orientation == 1) {
            this.mDisplayHeight = longSide;
            this.mDisplayWidth = shortSide;
            this.mDisplayRatio = this.mDisplayPortraitRatio;
            return;
        }
        this.mDisplayHeight = shortSide;
        this.mDisplayWidth = longSide;
        this.mDisplayRatio = this.mDisplayLandscapeRatio;
    }

    public void updateRangeRatio(final float ratio) {
        MiuiCvwGestureController.Slog.d(TAG, "updateBorderRatio ratio:" + ratio);
        this.mRangeRatio = ratio;
        this.mTargets.forEach(new Consumer() { // from class: com.android.server.wm.MiuiCvwSnapTargetPool$$ExternalSyntheticLambda0
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ((MiuiCvwSnapTargetPool.SnapTarget) obj).updateRangeRatio(ratio);
            }
        });
    }

    private void initTargets() {
        int longSide = Math.max(this.mDisplayContent.mBaseDisplayHeight, this.mDisplayContent.mBaseDisplayWidth);
        int shortSide = Math.min(this.mDisplayContent.mBaseDisplayHeight, this.mDisplayContent.mBaseDisplayWidth);
        float displayVerticalAspectRatio = shortSide / (longSide * 1.0f);
        this.mDisplayPortraitRatio = displayVerticalAspectRatio;
        float displayHorizontalAspectRatio = longSide / (shortSide * 1.0f);
        this.mDisplayLandscapeRatio = displayHorizontalAspectRatio;
        SnapTarget calculateSnapTarget = calculateSnapTarget(displayVerticalAspectRatio, displayVerticalAspectRatio, displayVerticalAspectRatio, shortSide, 1425.0f, 840.0f, 409.0f);
        this.mDisplayVerticalSnapTarget = calculateSnapTarget;
        this.mTargets.add(calculateSnapTarget);
        SnapTarget calculateSnapTarget2 = calculateSnapTarget(displayHorizontalAspectRatio, displayHorizontalAspectRatio, displayVerticalAspectRatio, longSide, DEFAULT_MAX_DISPLAY_LANDSCAPE_HEIGHT, DEFAULT_MIN_DISPLAY_LANDSCAPE_HEIGHT, DEFAULT_MINI_MIN_DISPLAY_LANDSCAPE_HEIGHT);
        this.mDisplayHorizontalSnapTarget = calculateSnapTarget2;
        this.mTargets.add(calculateSnapTarget2);
        SnapTarget calculateSnapTarget3 = calculateSnapTarget(0.5625f, 0.5625f, displayVerticalAspectRatio, DEFAULT_XLARGE_WIDE_WIDTH, 1425.0f, 840.0f, 409.0f);
        this.mXlargeGeneralSnapTarget = calculateSnapTarget3;
        this.mTargets.add(calculateSnapTarget3);
        SnapTarget calculateSnapTarget4 = calculateSnapTarget(1.3333334f, 1.3333334f, displayVerticalAspectRatio, DEFAULT_XLARGE_GENERAL_WIDTH, 1200.0f, 840.0f, 409.0f);
        this.mLargeGeneralSnapTarget = calculateSnapTarget4;
        this.mTargets.add(calculateSnapTarget4);
        SnapTarget calculateSnapTarget5 = calculateSnapTarget(0.75f, 0.75f, displayVerticalAspectRatio, 1200.0f, 1425.0f, 840.0f, 409.0f);
        this.mXlargeWideSnapTarget = calculateSnapTarget5;
        this.mTargets.add(calculateSnapTarget5);
        Collections.sort(this.mTargets);
        this.mFirstSnapTarget = this.mTargets.get(0);
        ArrayList<SnapTarget> arrayList = this.mTargets;
        this.mLastSnapTarget = arrayList.get(arrayList.size() - 1);
        this.mDisplayRatio = displayVerticalAspectRatio;
        this.mRangeRatio = displayVerticalAspectRatio;
    }

    private SnapTarget calculateSnapTarget(float aspectRatio, float miniAspectRatio, float displayVerticalAspectRatio, float orgW, float maxH, float minH, float miniH) {
        float orgHeight = orgW / aspectRatio;
        float maxWidth = aspectRatio * maxH;
        float minWidth = aspectRatio * minH;
        float miniWidth = miniAspectRatio * miniH;
        SnapTarget target = SnapTarget.create(aspectRatio, miniAspectRatio, displayVerticalAspectRatio);
        target.setWidthAndHeight(orgW, orgHeight);
        target.setMaxWidthAndHeight(maxWidth, maxH);
        target.setMiniWidthAndHeight(miniWidth, miniH);
        target.setMinWidthAndHeight(minWidth, minH);
        MiuiCvwGestureController.Slog.d(TAG, "calculateSnapTarget target:" + target.toString());
        return target;
    }

    public float getMaxHeight() {
        float maxH = MiuiFreeformPinManagerService.EDGE_AREA;
        Iterator<SnapTarget> it = this.mTargets.iterator();
        while (it.hasNext()) {
            SnapTarget target = it.next();
            if (maxH < target.getMaxHeight()) {
                maxH = target.getMaxHeight();
            }
        }
        return maxH;
    }

    public float getMaxWidth() {
        float maxW = MiuiFreeformPinManagerService.EDGE_AREA;
        Iterator<SnapTarget> it = this.mTargets.iterator();
        while (it.hasNext()) {
            SnapTarget target = it.next();
            if (maxW < target.getMaxWidth()) {
                maxW = target.getMaxWidth();
            }
        }
        return maxW;
    }

    public SnapTarget getSnapTargetByRawRatio(float ratio) {
        SnapTarget snapTarget = null;
        Iterator<SnapTarget> it = this.mTargets.iterator();
        while (it.hasNext()) {
            SnapTarget target = it.next();
            if (Math.abs(target.getRawRatio() - ratio) < 0.01d) {
                snapTarget = target;
            }
        }
        if (snapTarget == null) {
            MiuiCvwGestureController.Slog.e(TAG, "ratio : " + ratio);
            SnapTarget snapTarget2 = this.mTargets.get(0);
            return snapTarget2;
        }
        return snapTarget;
    }

    public SnapTarget getSnapTargetFullScreen() {
        if (this.mDisplayHeight >= this.mDisplayWidth) {
            return this.mDisplayVerticalSnapTarget;
        }
        return this.mDisplayHorizontalSnapTarget;
    }

    public boolean isLandscape() {
        return this.mDisplayHeight < this.mDisplayWidth;
    }

    public SnapTarget getSnapTargetLandscapeFullScreen() {
        return this.mDisplayHorizontalSnapTarget;
    }

    public SnapTarget getSnapTargetPortraitFullScreen() {
        return this.mDisplayVerticalSnapTarget;
    }

    public float getMinRawRatio() {
        return this.mFirstSnapTarget.getRawRatio();
    }

    public float getMaxRawRatio() {
        return this.mLastSnapTarget.getRawRatio();
    }

    public float getDisplayRatio() {
        return this.mDisplayRatio;
    }

    public float getRangeRatio() {
        return this.mRangeRatio;
    }

    public float getDisplayPortraitAspectRatio() {
        return this.mDisplayPortraitRatio;
    }

    public float getDisplayHorizontalAspectRatio() {
        return this.mDisplayLandscapeRatio;
    }

    /* loaded from: classes.dex */
    public static class SnapTarget implements Comparable<SnapTarget> {
        private float borderRatio;
        public final Rect bounds = new Rect();
        private float height;
        private float maxHeight;
        private float maxWidth;
        private float minHeight;
        private float minWidth;
        private float miniHeight;
        private float miniRatio;
        private float miniWidth;
        private float ratio;
        private float width;

        public static SnapTarget create(float ratio, float miniRatio, float borderRatio) {
            return new SnapTarget(ratio, miniRatio, borderRatio);
        }

        public SnapTarget(float ratio, float miniRatio, float borderRatio) {
            this.ratio = ratio;
            this.miniRatio = miniRatio;
            this.borderRatio = borderRatio;
        }

        public void updateRangeRatio(float ratio) {
            this.borderRatio = ratio;
        }

        public void setMaxWidthAndHeight(float width, float height) {
            this.maxWidth = width;
            this.maxHeight = height;
        }

        public void setWidthAndHeight(float width, float height) {
            this.width = width;
            this.height = height;
        }

        public void setMiniWidthAndHeight(float minWidth, float minHeight) {
            this.miniWidth = minWidth;
            this.miniHeight = minHeight;
        }

        public void setMinWidthAndHeight(float miniWidth, float miniHeight) {
            this.minWidth = miniWidth;
            this.minHeight = miniHeight;
        }

        public float getRawRatio() {
            return this.ratio / this.borderRatio;
        }

        public float getRatio() {
            return this.ratio;
        }

        public float getWidth() {
            return this.width;
        }

        public float getHeight() {
            return this.height;
        }

        public float getMaxWidth() {
            return this.maxWidth;
        }

        public float getMaxHeight() {
            return this.maxHeight;
        }

        public float getMiniWidth() {
            return this.miniWidth;
        }

        public float getMiniHeight() {
            return this.miniHeight;
        }

        public float getMinWidth() {
            return this.minWidth;
        }

        public float getMinHeight() {
            return this.minHeight;
        }

        public String toString() {
            return "SnapTarget ratio: " + this.ratio + ", miniRatio :" + this.miniRatio + ", borderRatio " + this.borderRatio + ",width:" + this.width + ",height:" + this.height + ",minWidth:" + this.minWidth + ",minHeight:" + this.minHeight + ",miniWidth:" + this.miniWidth + ",miniHeight:" + this.miniHeight;
        }

        public int compareTo(SnapTarget o) {
            return Float.compare(this.ratio, o.ratio);
        }
    }

    public void dump(PrintWriter pw, String prefix) {
        String innerPrefix = prefix + "  ";
        pw.println("MiuiCvwSnapTargetPool:");
        pw.print(innerPrefix);
        pw.println("mTargets: ");
        for (int i = 0; i < this.mTargets.size(); i++) {
            pw.print(innerPrefix);
            pw.println("Target #" + i + ":" + this.mTargets.get(i));
        }
    }
}
