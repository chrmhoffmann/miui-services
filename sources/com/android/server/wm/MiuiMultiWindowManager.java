package com.android.server.wm;

import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemProperties;
import android.util.ArraySet;
import android.util.DisplayMetrics;
import android.util.Slog;
import android.view.MotionEvent;
import android.window.TaskSnapshot;
import com.google.android.collect.Sets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
/* loaded from: classes.dex */
public class MiuiMultiWindowManager {
    private static final float FLOAT_FRECISION = 0.001f;
    private static final int GUTTER_IN_DP = 24;
    private static final int HALF_DIVISOR = 2;
    public static final String HEIGHT_COLUMNS = "heightColumns";
    public static final boolean IS_MIUI_MULTIWINDOW_SUPPORTED = true;
    public static final boolean IS_NOTCH_PROP = false;
    public static final boolean IS_TABLET = true;
    private static final int MAGIC_WINDOW_SAVITY_LEFT = 80;
    public static final String MIUI_SPLIT_SCREEN_PRIMARY_BOUNDS = "primaryBounds";
    public static final String MIUI_SPLIT_SCREEN_PRIMARY_POSITION = "primaryPosition";
    public static final int MIUI_SPLIT_SCREEN_RATIO_DEFAULT = 0;
    public static final int MIUI_SPLIT_SCREEN_RATIO_PRAIMARY_LESS_THAN_DEFAULT = 1;
    public static final int MIUI_SPLIT_SCREEN_RATIO_PRAIMARY_MORE_THAN_DEFAULT = 2;
    public static final int MIUI_SPLIT_SCREEN_RATIO_PRIMARY_FULL = 3;
    public static final int MIUI_SPLIT_SCREEN_RATIO_SECONDARY_FULL = 4;
    public static final String MIUI_SPLIT_SCREEN_RATIO_VALUES = "splitRatios";
    public static final String MIUI_SPLIT_SCREEN_SECONDARY_BOUNDS = "secondaryBounds";
    public static final String TAG = "MIUIMultiWindowManager";
    public static final String WIDTH_COLUMNS = "widthColumns";
    private static int sDividerWindowWidth;
    private static int sNavigationBarHeight;
    private static int sNavigationBarWidth;
    private static int sStatusBarHeight;
    private int mCaptionViewHeight;
    private int mDargbarWidth;
    private int mGestureNavHotArea;
    private boolean mHasSideinScreen;
    private boolean mIsStatusBarPermenantlyShowing;
    private int mSafeSideWidth;
    final ActivityTaskManagerService mService;
    private static final Object M_LOCK = new Object();
    private static volatile MiuiMultiWindowManager sSingleInstance = null;
    private static String sLaunchPkg = "";
    private Rect mDragedFreeFromPos = new Rect();
    boolean mIsAddSplitBar = false;

    private MiuiMultiWindowManager(ActivityTaskManagerService service) {
        this.mService = service;
    }

    private int allowedForegroundFreeForms(int displayId) {
        return 1;
    }

    private void calcMiuiFreeFormBounds(Task task, Rect outBounds, Rect oldStackBounds, int oldDisplayWidth, int oldDisplayHeight, int newDisplayWidth, int newDisplayHeight) {
        int width = oldDisplayWidth;
        int height = oldDisplayHeight;
        int minOldDisplaySide = Math.min(oldDisplayWidth, oldDisplayHeight);
        int maxOldDisplaySide = Math.max(oldDisplayWidth, oldDisplayHeight);
        int minNewDisplaySide = Math.min(newDisplayWidth, newDisplayHeight);
        int maxNewDisplaySide = Math.max(newDisplayWidth, newDisplayHeight);
        Rect defaultBounds = new Rect();
        calcDefaultFreeFormBounds(defaultBounds, task.getDisplayArea().mDisplayContent);
        Point defaultdragBarCenter = getDragBarCenterPoint(defaultBounds, task);
        if (minOldDisplaySide == minNewDisplaySide && maxOldDisplaySide == maxNewDisplaySide) {
            if (!this.mDragedFreeFromPos.isEmpty() && (this.mDragedFreeFromPos.width() != defaultBounds.width() || this.mDragedFreeFromPos.height() != defaultBounds.height())) {
                width = newDisplayHeight;
                height = newDisplayWidth;
            }
            Point dragBarCenter = getDragBarCenterPoint(oldStackBounds, task);
            defaultBounds.offset(((int) (((dragBarCenter.x * 1.0f) * newDisplayWidth) / width)) - defaultdragBarCenter.x, ((int) (((dragBarCenter.y * 1.0f) * newDisplayHeight) / height)) - defaultdragBarCenter.y);
            outBounds.set(relocateOffScreenWindow(defaultBounds, task));
            this.mDragedFreeFromPos.set(outBounds);
        } else if (Math.abs(((maxNewDisplaySide * 1.0f) / maxOldDisplaySide) - ((maxOldDisplaySide * 1.0f) / minOldDisplaySide)) < FLOAT_FRECISION) {
            defaultBounds.offset(((int) ((((oldStackBounds.left + (oldStackBounds.width() / 2)) * newDisplayWidth) * 1.0f) / width)) - defaultdragBarCenter.x, ((int) (((oldStackBounds.top + (((this.mCaptionViewHeight * minOldDisplaySide) * 1.0f) / (minOldDisplaySide * 2))) * newDisplayHeight) / height)) - defaultdragBarCenter.y);
            outBounds.set(relocateOffScreenWindow(defaultBounds, task));
            this.mDragedFreeFromPos.set(outBounds);
        } else if ((width == newDisplayWidth && height < newDisplayHeight) || (width < newDisplayWidth && height == newDisplayHeight)) {
            defaultBounds.offset(((newDisplayWidth - width) + oldStackBounds.right) - defaultBounds.right, oldStackBounds.top - defaultBounds.top);
            outBounds.set(relocateOffScreenWindow(defaultBounds, task));
            this.mDragedFreeFromPos.set(outBounds);
        } else if ((width == newDisplayHeight && height < newDisplayWidth) || (height == newDisplayWidth && width < newDisplayHeight)) {
            if (width < newDisplayHeight) {
                defaultBounds.offset(((newDisplayHeight - width) + oldStackBounds.right) - defaultBounds.right, oldStackBounds.top - defaultBounds.top);
            } else {
                defaultBounds.offset(oldStackBounds.right - defaultBounds.right, oldStackBounds.top - defaultBounds.top);
            }
            Point defaultdragBarCenter2 = getDragBarCenterPoint(defaultBounds, task);
            defaultBounds.offset(((int) (((defaultdragBarCenter2.x * 1.0f) * newDisplayWidth) / width)) - defaultdragBarCenter2.x, ((int) (((defaultdragBarCenter2.y * 1.0f) * newDisplayHeight) / height)) - defaultdragBarCenter2.y);
            outBounds.set(relocateOffScreenWindow(defaultBounds, task));
            this.mDragedFreeFromPos.set(outBounds);
        } else {
            outBounds.set(defaultBounds);
            this.mDragedFreeFromPos.setEmpty();
        }
    }

    private void calcMiuiMultiWindowStackBoundsDefault(Task task, Rect outBounds) {
        if (task == null) {
            return;
        }
        if (task.inSplitScreenPrimaryWindowingMode()) {
            calcMiuiSplitStackBounds(task.getDisplayArea().mDisplayContent, 0, outBounds, null);
        } else if (task.inSplitScreenSecondaryWindowingMode()) {
            calcMiuiSplitStackBounds(task.getDisplayArea().mDisplayContent, 0, null, outBounds);
        }
    }

    public static void calcMiuiSplitStackBounds(DisplayContent display, int splitRatio, Rect primaryOutBounds, Rect secondaryOutBounds) {
        if (display == null) {
            return;
        }
        Bundle bundle = getSplitGearsByDisplay(display);
        float[] splitRatios = bundle.getFloatArray(MIUI_SPLIT_SCREEN_RATIO_VALUES);
        int tempSplitRatio = splitRatio;
        if (splitRatio != 0 && splitRatios != null && splitRatios.length == 1) {
            tempSplitRatio = 0;
        }
        float primaryRatio = calcPrimaryRatio(tempSplitRatio);
        if (primaryRatio == MiuiFreeformPinManagerService.EDGE_AREA) {
            if (primaryOutBounds != null) {
                primaryOutBounds.setEmpty();
            }
            if (secondaryOutBounds != null) {
                secondaryOutBounds.setEmpty();
                return;
            }
            return;
        }
        int primaryPos = bundle.getInt(MIUI_SPLIT_SCREEN_PRIMARY_POSITION);
        if (primaryPos == 1) {
            calcLeftRightSplitStackBounds(display, primaryRatio, primaryOutBounds, secondaryOutBounds);
        } else if (primaryPos == 0) {
            calcTopBottomSplitStackBounds(display, primaryRatio, primaryOutBounds, secondaryOutBounds);
        }
    }

    private void calcMiuiSplitStackForConfigChange(Task task, Rect outBounds) {
        if (task.inSplitScreenPrimaryWindowingMode()) {
            calcMiuiSplitStackBounds(task.getDisplayArea().mDisplayContent, 0, outBounds, null);
        } else if (task.inSplitScreenSecondaryWindowingMode()) {
            calcMiuiSplitStackBounds(task.getDisplayArea().mDisplayContent, 0, null, outBounds);
        }
    }

    private static void calcLeftRightSplitStackBounds(DisplayContent display, float primaryRatio, Rect primaryOutBounds, Rect secondaryOutBounds) {
        int displayWidth = display.mDisplayContent.getDisplayInfo().logicalWidth;
        int displayHeight = display.mDisplayContent.getDisplayInfo().logicalHeight;
        if (primaryOutBounds != null) {
            primaryOutBounds.set(0, 0, ((int) ((displayWidth - 0) * primaryRatio)) - (sDividerWindowWidth / 2), displayHeight);
        }
        if (secondaryOutBounds != null) {
            secondaryOutBounds.set(((int) ((displayWidth - 0) * primaryRatio)) + (sDividerWindowWidth / 2), 0, displayWidth - 0, displayHeight);
        }
        if (0 != 0 && display.mDisplayContent.getDisplayInfo().rotation == 1) {
            if (primaryOutBounds != null) {
                primaryOutBounds.offset(0, 0);
            }
            if (secondaryOutBounds != null) {
                secondaryOutBounds.offset(0, 0);
            }
        }
    }

    private static float calcPrimaryRatio(int splitRatio) {
        if (splitRatio == 0) {
            return 0.5f;
        }
        if (splitRatio == 1) {
            return 0.33333334f;
        }
        if (splitRatio == 2) {
            return 0.6666667f;
        }
        return MiuiFreeformPinManagerService.EDGE_AREA;
    }

    private static void calcTopBottomSplitStackBounds(DisplayContent display, float primaryRatio, Rect primaryOutBounds, Rect secondaryOutBounds) {
        int displayWidth = display.mDisplayContent.getDisplayInfo().logicalWidth;
        int displayHeight = display.mDisplayContent.getDisplayInfo().logicalHeight;
        if (primaryOutBounds != null) {
            primaryOutBounds.set(0, 0, displayWidth, (((int) ((displayHeight - 0) * primaryRatio)) - (sDividerWindowWidth / 2)) + 0);
        }
        if (secondaryOutBounds != null) {
            secondaryOutBounds.set(0, ((int) ((displayHeight - 0) * primaryRatio)) + (sDividerWindowWidth / 2) + 0, displayWidth, displayHeight);
        }
    }

    private int dipToPixelWithoutRog(int dip, float densityDpiWithoutRog) {
        return (int) ((dip * densityDpiWithoutRog) / 160.0f);
    }

    public static void exitMiuiMultiStack(Task rootTask) {
        if (rootTask == null) {
            return;
        }
        rootTask.setWindowingMode(1);
    }

    public static void exitMiuiMultiStack(Task stack, boolean isAnimate, boolean isShowRecents, boolean isEnteringSplitScreenMode, boolean isDeferEnsuringVisibility, boolean isCreating) {
        if (stack == null) {
            return;
        }
        stack.setWindowingMode(1, isCreating);
    }

    private static int getColumnsByWidth(int widthInDp) {
        if (widthInDp <= 0 || widthInDp >= 320) {
            if (widthInDp < 320 || widthInDp >= 600) {
                if (widthInDp >= 600 && widthInDp < 840) {
                    return 8;
                }
                if (widthInDp >= 840) {
                    return 12;
                }
                return 1;
            }
            return 4;
        }
        return 2;
    }

    private static float getDensityDpiWithoutRog() {
        int srcDpi = SystemProperties.getInt("ro.sf.real_lcd_density", SystemProperties.getInt("ro.sf.lcd_density", 0));
        int dpi = SystemProperties.getInt("persist.sys.dpi", srcDpi);
        int rogDpi = SystemProperties.getInt("persist.sys.realdpi", srcDpi);
        if (dpi <= 0) {
            dpi = srcDpi;
        }
        if (rogDpi <= 0) {
            rogDpi = srcDpi;
        }
        return ((srcDpi * 1.0f) * rogDpi) / dpi;
    }

    public static MiuiMultiWindowManager getInstance(ActivityTaskManagerService service) {
        if (sSingleInstance == null) {
            synchronized (M_LOCK) {
                if (sSingleInstance == null) {
                    sSingleInstance = new MiuiMultiWindowManager(service);
                }
            }
        }
        return sSingleInstance;
    }

    public static Bundle getSplitGearsByDisplay(DisplayContent display) {
        Bundle bundle = new Bundle();
        if (display != null && display.mDisplayContent != null) {
            float densityWithoutRog = getDensityDpiWithoutRog();
            int widthInDp = (int) ((display.mDisplayContent.getDisplayInfo().logicalWidth * 160) / densityWithoutRog);
            int heightInDp = (int) ((display.mDisplayContent.getDisplayInfo().logicalHeight * 160) / densityWithoutRog);
            int widthColumns = getColumnsByWidth(widthInDp);
            int heightColumns = getColumnsByWidth(heightInDp);
            if (widthColumns == 4 && heightColumns > 4) {
                bundle.putInt(MIUI_SPLIT_SCREEN_PRIMARY_POSITION, 0);
                bundle.putFloatArray(MIUI_SPLIT_SCREEN_RATIO_VALUES, new float[]{0.333333f, 0.5f, 0.666667f});
            } else if (widthColumns > 4 && heightColumns == 4) {
                bundle.putInt(MIUI_SPLIT_SCREEN_PRIMARY_POSITION, 1);
                float[] ratios = widthColumns >= 8 ? new float[]{0.333333f, 0.5f, 0.666667f} : new float[]{0.5f};
                bundle.putFloatArray(MIUI_SPLIT_SCREEN_RATIO_VALUES, ratios);
            } else if (widthColumns == 8 && heightColumns == 8) {
                bundle.putInt(MIUI_SPLIT_SCREEN_PRIMARY_POSITION, 1);
                bundle.putFloatArray(MIUI_SPLIT_SCREEN_RATIO_VALUES, new float[]{0.5f});
            } else if (widthColumns == 8 && heightColumns == 12) {
                bundle.putInt(MIUI_SPLIT_SCREEN_PRIMARY_POSITION, 0);
                bundle.putFloatArray(MIUI_SPLIT_SCREEN_RATIO_VALUES, new float[]{0.333333f, 0.5f, 0.666667f});
            } else if (widthColumns == 12 && heightColumns == 8) {
                bundle.putInt(MIUI_SPLIT_SCREEN_PRIMARY_POSITION, 1);
                bundle.putFloatArray(MIUI_SPLIT_SCREEN_RATIO_VALUES, new float[]{0.333333f, 0.5f, 0.666667f});
            } else {
                int ratio = widthInDp < heightInDp ? 0 : 1;
                bundle.putInt(MIUI_SPLIT_SCREEN_PRIMARY_POSITION, ratio);
                bundle.putFloatArray(MIUI_SPLIT_SCREEN_RATIO_VALUES, new float[]{0.5f});
            }
            bundle.putInt(WIDTH_COLUMNS, widthColumns);
            bundle.putInt(HEIGHT_COLUMNS, heightColumns);
            return bundle;
        }
        return bundle;
    }

    private Rect getStableRect(DisplayContent displayContent) {
        int displayWidth = displayContent.getDisplayInfo().logicalWidth;
        int displayHeight = displayContent.getDisplayInfo().logicalHeight;
        Rect stableRect = new Rect(0, 0, displayWidth, displayHeight);
        stableRect.top = displayContent.mDisplayInfo.displayCutout == null ? sStatusBarHeight : Math.max(displayContent.mDisplayInfo.displayCutout.getSafeInsetTop(), sStatusBarHeight);
        int naviPos = displayContent.getDisplayPolicy().navigationBarPosition(displayContent.mDisplayInfo.rotation);
        if (naviPos == 4) {
            stableRect.bottom -= sNavigationBarHeight;
        } else if (naviPos != 2) {
            return stableRect;
        } else {
            if (displayContent.mDisplayInfo.displayCutout == null) {
                stableRect.right -= sNavigationBarWidth;
                return stableRect;
            }
            stableRect.right = (stableRect.right - sNavigationBarWidth) - displayContent.mDisplayInfo.displayCutout.getSafeInsetRight();
        }
        return stableRect;
    }

    private void initMiuiFreeformWindowParam(DisplayContent displayContent) {
        DisplayMetrics displayMetrics = displayContent.getDisplayMetrics();
        this.mCaptionViewHeight = WindowManagerService.dipToPixel(36, displayMetrics);
        this.mDargbarWidth = WindowManagerService.dipToPixel(70, displayMetrics);
        this.mGestureNavHotArea = WindowManagerService.dipToPixel(18, displayMetrics);
    }

    private void loadDimens(int displayId) {
        sDividerWindowWidth = this.mService.mUiContext.getResources().getDimensionPixelSize(285671523);
        sStatusBarHeight = this.mService.mUiContext.getResources().getDimensionPixelSize(17105574);
        sNavigationBarWidth = this.mService.mUiContext.getResources().getDimensionPixelSize(17105382);
        sNavigationBarHeight = this.mService.mUiContext.getResources().getDimensionPixelSize(17105377);
    }

    private static void scale(Rect rect, float xscale, float yscale) {
        if (Float.compare(xscale, 1.0f) != 0) {
            rect.left = (int) ((rect.left * xscale) + 0.5f);
            rect.right = (int) ((rect.right * xscale) + 0.5f);
        }
        if (yscale != 1.0f) {
            rect.top = (int) ((rect.top * yscale) + 0.5f);
            rect.bottom = (int) ((rect.bottom * yscale) + 0.5f);
        }
    }

    private Set<Integer> setFreeformStackVisible(DisplayContent display, Set<Integer> paramSet) {
        HashSet<Integer> processedStackIdSet = new HashSet<>();
        TaskDisplayArea taskDisplayArea = display.getDefaultTaskDisplayArea();
        for (int stackNdx = taskDisplayArea.getChildCount() - 1; stackNdx >= 0; stackNdx--) {
            Task stack = taskDisplayArea.getChildAt(stackNdx);
            if (!stack.isAlwaysOnTop() && paramSet.contains(Integer.valueOf(stack.getRootTaskId()))) {
                stack.setAlwaysOnTop(true);
                processedStackIdSet.add(Integer.valueOf(stack.getRootTaskId()));
            }
        }
        return processedStackIdSet;
    }

    public boolean blockSwipeFromTop(MotionEvent event, DisplayContent display) {
        WindowState statusBar;
        if (display == null || !isPhoneLandscape(display) || (statusBar = display.getDisplayPolicy().getStatusBar()) == null || statusBar.isVisible()) {
            return false;
        }
        boolean shouldBlock = false;
        TaskDisplayArea taskDisplayArea = display.getDefaultTaskDisplayArea();
        for (int stackNdx = taskDisplayArea.getChildCount() - 1; stackNdx >= 0; stackNdx--) {
            Task taskStack = taskDisplayArea.getChildAt(stackNdx);
            if (!taskStack.inFreeformWindowingMode()) {
                return false;
            }
            if (taskStack.isVisible()) {
                Rect bounds = taskStack.getBounds();
                if (bounds.top <= statusBar.getFrame().bottom) {
                    shouldBlock |= event.getX() >= ((float) (bounds.left + (bounds.width() / 3))) && ((float) (bounds.right - (bounds.width() / 3))) >= event.getX();
                }
            }
        }
        return shouldBlock;
    }

    void calcDefaultFreeFormBounds(Rect outBounds, DisplayContent displayContent) {
        int i;
        if (displayContent != null && outBounds != null) {
            int displayWidth = displayContent.getDisplayInfo().logicalWidth;
            int displayHeight = displayContent.getDisplayInfo().logicalHeight;
            if (allowedForegroundFreeForms(displayContent.mDisplayId) == 1) {
                float densityWithoutRog = getDensityDpiWithoutRog();
                int widthColumns = getColumnsByWidth((int) ((displayWidth * 160) / densityWithoutRog));
                int heightColumns = getColumnsByWidth((int) ((displayHeight * 160) / densityWithoutRog));
                int minSide = Math.min(displayWidth, displayHeight);
                int gutterPixel = dipToPixelWithoutRog(24, densityWithoutRog);
                int marginPixel = dipToPixelWithoutRog(24, densityWithoutRog);
                initMiuiFreeformWindowParam(displayContent);
                Rect stableRect = getStableRect(displayContent);
                if (widthColumns == 4 && heightColumns > 4) {
                    outBounds.left = marginPixel;
                    outBounds.right = displayWidth - marginPixel;
                    int freeFormHeight = this.mCaptionViewHeight + ((int) (((outBounds.width() * 1.0f) * 16.0f) / 9.0f));
                    outBounds.top = (displayHeight - freeFormHeight) / 2;
                    outBounds.bottom = outBounds.top + freeFormHeight;
                } else if (widthColumns > 4 && heightColumns == 4) {
                    int v12 = this.mHasSideinScreen ? this.mSafeSideWidth : 0;
                    outBounds.right = stableRect.right - marginPixel;
                    int v14 = displayHeight - 2130706432;
                    outBounds.left = outBounds.right - v14;
                    int windowHeight = (displayHeight - sStatusBarHeight) - (v14 * 2);
                    if (outBounds.width() < windowHeight) {
                        windowHeight = outBounds.width();
                    }
                    int topPosition = (displayHeight - windowHeight) / 2;
                    if (this.mIsStatusBarPermenantlyShowing) {
                        i = Math.max(sStatusBarHeight + v12, topPosition);
                    } else {
                        i = Math.max((sStatusBarHeight / 2) + v12, topPosition);
                    }
                    outBounds.top = i;
                    outBounds.bottom = outBounds.top + windowHeight;
                } else if (widthColumns == 8 && heightColumns == 8) {
                    int v9_1 = dipToPixelWithoutRog(32, densityWithoutRog);
                    int freeFormWidth = (16 - v9_1) - 4;
                    outBounds.right = stableRect.right - v9_1;
                    outBounds.left = outBounds.right - freeFormWidth;
                    int freeFormHeight2 = ((int) (((freeFormWidth * 1.0f) * 16.0f) / 9.0f)) + this.mCaptionViewHeight;
                    outBounds.top = (displayHeight - freeFormHeight2) / 2;
                    outBounds.bottom = outBounds.top + freeFormHeight2;
                } else if ((widthColumns == 8 && heightColumns == 12) || (widthColumns == 12 && heightColumns == 8)) {
                    int v12_2 = (minSide - gutterPixel) / 2;
                    outBounds.right = stableRect.right - dipToPixelWithoutRog(32, densityWithoutRog);
                    outBounds.left = outBounds.right - v12_2;
                    int v13_2 = ((int) (((v12_2 * 1.0f) * 16.0f) / 9.0f)) + this.mCaptionViewHeight;
                    outBounds.top = (displayHeight - v13_2) / 2;
                    outBounds.bottom = outBounds.top + v13_2;
                }
                if (widthColumns <= 4 || heightColumns != 4) {
                    outBounds.top = Math.max(outBounds.top, sStatusBarHeight);
                    outBounds.bottom = Math.min(outBounds.bottom, displayHeight - sStatusBarHeight);
                }
            }
        }
    }

    public void calcMiuiMultiWindowStackBoundsForConfigChange(Task stack, Rect outBounds, Rect oldStackBounds, int oldDisplayWidth, int oldDisplayHeight, int newDisplayWidth, int newDisplayHeight, boolean isModeChanged) {
        if (stack.inSplitScreenWindowingMode()) {
            calcMiuiSplitStackForConfigChange(stack, outBounds);
        }
        if (stack.inFreeformWindowingMode() && allowedForegroundFreeForms(stack.getDisplayArea().mDisplayContent.mDisplayId) == 1) {
            if (this.mDragedFreeFromPos.isEmpty()) {
                calcDefaultFreeFormBounds(outBounds, stack.getDisplayArea().mDisplayContent);
                return;
            }
            if (stack.matchParentBounds() || isModeChanged) {
                oldStackBounds.set(this.mDragedFreeFromPos);
            }
            calcMiuiFreeFormBounds(stack, outBounds, oldStackBounds, oldDisplayWidth, oldDisplayHeight, newDisplayWidth, newDisplayHeight);
        }
    }

    public void updateDragFreeFormPos(final Rect bounds, DisplayContent dc) {
        if (dc != null && bounds != null && allowedForegroundFreeForms(dc.mDisplayId) == 1) {
            synchronized (this.mService.getGlobalLock()) {
                dc.forAllTaskDisplayAreas(new Consumer() { // from class: com.android.server.wm.MiuiMultiWindowManager$$ExternalSyntheticLambda1
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        ((TaskDisplayArea) obj).forAllRootTasks(new Consumer() { // from class: com.android.server.wm.MiuiMultiWindowManager$$ExternalSyntheticLambda0
                            @Override // java.util.function.Consumer
                            public final void accept(Object obj2) {
                                MiuiMultiWindowManager.lambda$updateDragFreeFormPos$0(r1, (Task) obj2);
                            }
                        });
                    }
                });
            }
        }
    }

    public static /* synthetic */ void lambda$updateDragFreeFormPos$0(Rect bounds, Task rootTask) {
        if (rootTask.inFreeformWindowingMode() && !bounds.equals(rootTask.getBounds())) {
            rootTask.resize(bounds, false, true);
        }
    }

    public Point getDragBarCenterPoint(Rect originalWindowBounds, Task stack) {
        DisplayContent display = stack.getDisplayArea().mDisplayContent;
        if (display == null) {
            Slog.w(TAG, "getDragBarCenterPoint: Invalid activityDisplay " + display);
            return new Point();
        }
        DisplayContent displayContent = display.mDisplayContent;
        if (displayContent == null) {
            Slog.w(TAG, "getDragBarCenterPoint: displayContent " + displayContent);
            return new Point();
        }
        initMiuiFreeformWindowParam(displayContent);
        return new Point(originalWindowBounds.left + (originalWindowBounds.width() / 2), originalWindowBounds.top + (this.mCaptionViewHeight / 2));
    }

    public Task getFilteredTopStack(DisplayContent display, List<Integer> paramList) {
        Task task = null;
        synchronized (this.mService.getGlobalLock()) {
            if (display == null) {
                Slog.i(TAG, "getFilteredTopStack activityDisplay null, no TopStack");
                return null;
            }
            TaskDisplayArea taskDisplayArea = display.getDefaultTaskDisplayArea();
            for (int stackNdx = taskDisplayArea.getChildCount() - 1; stackNdx >= 0; stackNdx--) {
                task = (Task) taskDisplayArea.getChildAt(stackNdx);
                if (paramList == null || !paramList.contains(Integer.valueOf(task.getWindowingMode()))) {
                    break;
                }
            }
            return task;
        }
    }

    public Bundle getMiuiMultiWindowState() {
        Bundle result = new Bundle();
        synchronized (this.mService.getGlobalLock()) {
            DisplayContent displayContent = this.mService.mRootWindowContainer.getTopFocusedDisplayContent();
            WindowState focus = displayContent.mCurrentFocus;
            if (focus == null) {
                return result;
            }
            if (focus.inFreeformWindowingMode()) {
                result.putBoolean("float_ime_state", isPhoneLandscape(displayContent));
            } else if (focus.inSplitScreenWindowingMode()) {
                result.putBoolean("is_leftright_split", false);
            }
            result.putParcelable("ime_target_rect", focus.getBounds());
            return result;
        }
    }

    public int getNavBarBoundOnScreen(DisplayContent displayContent, Rect outBound) {
        int displayWidth = displayContent.getDisplayInfo().logicalWidth;
        int displayHeight = displayContent.getDisplayInfo().logicalHeight;
        int naviPos = displayContent.getDisplayPolicy().navigationBarPosition(displayContent.getRotation());
        if (naviPos == 4) {
            outBound.left = 0;
            outBound.top = displayHeight - sNavigationBarHeight;
            outBound.right = displayWidth;
            outBound.bottom = displayHeight;
        } else if (naviPos == 2) {
            outBound.left = displayWidth - sNavigationBarWidth;
            outBound.top = 0;
            outBound.right = displayWidth;
            outBound.bottom = displayHeight;
        } else if (naviPos == 1) {
            outBound.left = 0;
            outBound.top = 0;
            outBound.right = sNavigationBarWidth;
            outBound.bottom = sNavigationBarHeight;
        } else {
            outBound.left = 0;
            outBound.top = 0;
            outBound.right = 0;
            outBound.bottom = 0;
        }
        return naviPos;
    }

    public int getNotchBoundOnScreen(DisplayContent displayContent, Rect outBound) {
        return -1;
    }

    public Rect[] getRectForScreenShotForDrag(int splitRatio) {
        Task task = getSplitScreenTopStack();
        if (task != null && task.getDisplayArea().mDisplayContent != null) {
            Rect[] dragBounds = {new Rect(), new Rect()};
            calcMiuiSplitStackBounds(task.getDisplayArea().mDisplayContent, splitRatio, dragBounds[0], dragBounds[1]);
            return dragBounds;
        }
        return null;
    }

    public Bundle getSplitGearsByDisplayId(int displayId) {
        return getSplitGearsByDisplay(this.mService.mRootWindowContainer.getDisplayContent(displayId));
    }

    public Task getSplitScreenPrimaryStack() {
        DisplayContent defaultDisplay = this.mService.mRootWindowContainer.getDefaultDisplay();
        TaskDisplayArea mTaskDisplayArea = defaultDisplay.getDefaultTaskDisplayArea();
        return mTaskDisplayArea.getTopRootTaskInWindowingMode(100);
    }

    public Task getSplitScreenTopStack() {
        return getFilteredTopStack(this.mService.mRootWindowContainer.getDefaultDisplay(), Arrays.asList(5, 2, 102));
    }

    public Bundle getSplitStacksPos(int displayId, int splitRatio) {
        synchronized (this.mService.getGlobalLock()) {
            DisplayContent display = this.mService.mRootWindowContainer.getDisplayContent(displayId);
            if (display == null) {
                return null;
            }
            Rect primaryOutBounds = new Rect();
            Rect secondaryOutBounds = new Rect();
            calcMiuiSplitStackBounds(display, splitRatio, primaryOutBounds, secondaryOutBounds);
            Bundle result = new Bundle();
            result.putParcelable(MIUI_SPLIT_SCREEN_PRIMARY_BOUNDS, primaryOutBounds);
            result.putParcelable(MIUI_SPLIT_SCREEN_SECONDARY_BOUNDS, secondaryOutBounds);
            result.putInt(MIUI_SPLIT_SCREEN_PRIMARY_POSITION, getSplitGearsByDisplay(display).getInt(MIUI_SPLIT_SCREEN_PRIMARY_POSITION, 0));
            return result;
        }
    }

    public boolean isPhoneLandscape(DisplayContent displayContent) {
        if (displayContent == null) {
            return false;
        }
        int width = displayContent.getDisplayInfo().logicalWidth;
        int i = displayContent.getDisplayInfo().logicalHeight;
        float densityWithoutRog = getDensityDpiWithoutRog();
        if (getColumnsByWidth((int) ((width * 160) / densityWithoutRog)) < 8 || getColumnsByWidth((int) ((width * 160) / densityWithoutRog)) != 4) {
            return false;
        }
        return true;
    }

    public void onConfigurationChanged(int displayId) {
        loadDimens(displayId);
    }

    public void onSystemReady() {
        loadDimens(0);
    }

    public Rect relocateOffScreenWindow(Rect originalWindowBounds, Task task) {
        Rect bounds;
        DisplayContent activityDisplay = task.getDisplayArea().mDisplayContent;
        if (activityDisplay == null) {
            Slog.w(TAG, "relocateOffScreenWindow: Invalid activityDisplay " + activityDisplay);
            return new Rect();
        }
        DisplayContent displayContent = activityDisplay.mDisplayContent;
        if (displayContent == null) {
            Slog.w(TAG, "relocateOffScreenWindow: Invalid displayContent " + displayContent);
            return new Rect();
        }
        initMiuiFreeformWindowParam(displayContent);
        int captionSpareWidth = (originalWindowBounds.width() - this.mDargbarWidth) / 2;
        Rect stableRect = getStableRect(displayContent);
        if (isPhoneLandscape(displayContent)) {
            int top = this.mHasSideinScreen ? this.mSafeSideWidth : 0;
            int height = this.mIsStatusBarPermenantlyShowing ? sStatusBarHeight : sStatusBarHeight / 2;
            bounds = new Rect(stableRect.left - captionSpareWidth, top + height, (stableRect.right - captionSpareWidth) - this.mDargbarWidth, (stableRect.bottom - this.mCaptionViewHeight) - this.mGestureNavHotArea);
        } else {
            bounds = new Rect(stableRect.left - captionSpareWidth, stableRect.top, (stableRect.right - captionSpareWidth) - this.mDargbarWidth, (stableRect.bottom - this.mCaptionViewHeight) - this.mGestureNavHotArea);
        }
        int validLeft = Math.min(Math.max(originalWindowBounds.left, bounds.left), bounds.right);
        int validTop = Math.min(Math.max(originalWindowBounds.top, bounds.top), bounds.bottom);
        if (originalWindowBounds.left != validLeft || originalWindowBounds.top != validTop) {
            originalWindowBounds.offsetTo(validLeft, validTop);
        }
        return originalWindowBounds;
    }

    public void removeSplitScreenDividerBar(int windowMode, boolean immediately) {
    }

    public void resizeMiuiSplitStacks(int splitRatio, boolean isEnsureVisible) {
    }

    public void setCallingPackage(String callingPkg) {
        sLaunchPkg = callingPkg;
    }

    public void setSplitBarVisibility(boolean visibility) {
    }

    public TaskSnapshot getTaskSnapshot(Task task) {
        if (task == null) {
            Slog.w(TAG, "getTaskSnapshot: task=" + task + " not found");
            return null;
        }
        ActivityRecord topActivity = task.getTopActivity(false, true);
        if (topActivity != null && topActivity.isVisible()) {
            takeTaskSnapshot(topActivity.token, true);
        }
        TaskSnapshot taskSnapshot = this.mService.mWindowManager.getTaskSnapshotController().getSnapshot(task.mTaskId, task.mUserId, false, false);
        return taskSnapshot;
    }

    public void takeTaskSnapshot(IBinder binder, boolean alwaysTake) {
        synchronized (this.mService.mWindowManager.getGlobalLock()) {
            ActivityRecord activityRecord = this.mService.mWindowManager.mRoot.getActivityRecord(binder);
            if (activityRecord != null && alwaysTake) {
                Task parent = activityRecord.getParent();
                if (parent instanceof Task) {
                    ArraySet tasks = Sets.newArraySet(new Task[]{parent});
                    this.mService.mWindowManager.getTaskSnapshotController().snapshotTasks(tasks);
                } else {
                    Slog.v("MiuiMultiWindowWMSImpl", "takeTaskSnapshot has no tasks");
                }
            } else {
                Slog.v("MiuiMultiWindowWMSImpl", "takeTaskSnapshot appWindowToken is null");
            }
        }
    }

    public boolean isSplitPrimaryScreen(Task task) {
        if (task == null) {
            return false;
        }
        Rect displayBounds = task.getDisplayContent().getBounds();
        Rect taskBounds = task.getBounds();
        return taskBounds.width() == displayBounds.width() ? taskBounds.top == 0 : taskBounds.height() == displayBounds.height() && taskBounds.left == 0;
    }
}
