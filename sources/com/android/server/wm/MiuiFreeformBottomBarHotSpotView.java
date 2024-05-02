package com.android.server.wm;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.res.Configuration;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.MiuiMultiWindowUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;
/* loaded from: classes.dex */
public class MiuiFreeformBottomBarHotSpotView extends FrameLayout {
    private static final String TAG = "MiuiFreeformBottomBarHotSpotView";
    private int mBottomBarHotSpotLong;
    private int mBottomBarHotSpotShort;
    private FrameLayout mBottomBarLeftHotSpot;
    private View mBottomBarLeftHotSpotBg;
    private View mBottomBarLeftHotSpotIcon;
    private TextView mBottomBarLeftHotSpotTextView;
    private FrameLayout mBottomBarRightHotSpot;
    private View mBottomBarRightHotSpotBg;
    private View mBottomBarRightHotSpotIcon;
    private TextView mBottomBarRightHotSpotTextView;
    private boolean mIsDarkMode;
    private Configuration mLastConfiguration;
    private int mLastHotSpotStatus;
    private int mLastShowingHotSpot;
    private DisplayMetrics mOutMetrics;
    private WindowManager mWindowManager;
    private static boolean DEBUG = MiuiFreeFormGestureController.DEBUG;
    private static int ANIMATIN_MAX_DURATION = 250;
    public static int BOTTOM_BAR_HOTSPOT_STATUS_HIDE = 0;
    public static int BOTTOM_BAR_HOTSPOT_STATUS_SHOW = 1;
    public static int BOTTOM_BAR_HOTSPOT_STATUS_BIGGER = 2;
    public static int BOTTOM_BAR_HOTSPOT_STATUS_TIGGER = 3;

    public MiuiFreeformBottomBarHotSpotView(Context context) {
        this(context, null);
    }

    public MiuiFreeformBottomBarHotSpotView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MiuiFreeformBottomBarHotSpotView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public MiuiFreeformBottomBarHotSpotView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mLastShowingHotSpot = -1;
        this.mLastHotSpotStatus = BOTTOM_BAR_HOTSPOT_STATUS_HIDE;
        this.mOutMetrics = new DisplayMetrics();
        this.mLastConfiguration = new Configuration();
        this.mBottomBarHotSpotShort = context.getResources().getDimensionPixelSize(285671449);
        this.mBottomBarHotSpotLong = context.getResources().getDimensionPixelSize(285671448);
        this.mWindowManager = (WindowManager) this.mContext.getSystemService("window");
        this.mIsDarkMode = isInDarkMode(context.getResources().getConfiguration());
    }

    private boolean isInDarkMode(Configuration configuration) {
        return configuration != null && (configuration.uiMode & 32) == 32;
    }

    @Override // android.view.View
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mBottomBarLeftHotSpot = (FrameLayout) findViewById(285868069);
        this.mBottomBarLeftHotSpotIcon = findViewById(285868067);
        this.mBottomBarLeftHotSpotBg = findViewById(285868066);
        this.mBottomBarLeftHotSpotTextView = (TextView) findViewById(285868068);
        this.mBottomBarRightHotSpot = (FrameLayout) findViewById(285868073);
        this.mBottomBarRightHotSpotIcon = findViewById(285868071);
        this.mBottomBarRightHotSpotBg = findViewById(285868070);
        this.mBottomBarRightHotSpotTextView = (TextView) findViewById(285868072);
        resetState();
    }

    @Override // android.view.View
    protected void onConfigurationChanged(Configuration newConfig) {
        int changes = this.mLastConfiguration.updateFrom(newConfig);
        boolean languageChange = (changes & 4) != 0;
        boolean isInDarkMode = isInDarkMode(newConfig);
        boolean darkModeChange = false;
        if (this.mIsDarkMode != isInDarkMode) {
            this.mIsDarkMode = isInDarkMode;
            darkModeChange = true;
        }
        if (languageChange || darkModeChange) {
            this.mBottomBarLeftHotSpotBg.setBackground(getResources().getDrawable(285737043));
            this.mBottomBarRightHotSpotBg.setBackground(getResources().getDrawable(285737044));
            this.mBottomBarLeftHotSpotIcon.setBackground(getResources().getDrawable(285737045));
            this.mBottomBarRightHotSpotIcon.setBackground(getResources().getDrawable(285737045));
            this.mBottomBarLeftHotSpotTextView.setTextColor(getResources().getColor(285605921));
            this.mBottomBarRightHotSpotTextView.setTextColor(getResources().getColor(285605921));
            this.mBottomBarLeftHotSpotTextView.setText(getResources().getString(286195842));
            this.mBottomBarRightHotSpotTextView.setText(getResources().getString(286195842));
        }
    }

    private void resetState() {
        this.mBottomBarLeftHotSpot.setAlpha(MiuiFreeformPinManagerService.EDGE_AREA);
        this.mBottomBarLeftHotSpotIcon.setAlpha(MiuiFreeformPinManagerService.EDGE_AREA);
        this.mBottomBarLeftHotSpotBg.setAlpha(MiuiFreeformPinManagerService.EDGE_AREA);
        this.mBottomBarLeftHotSpotTextView.setAlpha(MiuiFreeformPinManagerService.EDGE_AREA);
        this.mBottomBarRightHotSpot.setAlpha(MiuiFreeformPinManagerService.EDGE_AREA);
        this.mBottomBarRightHotSpotIcon.setAlpha(MiuiFreeformPinManagerService.EDGE_AREA);
        this.mBottomBarRightHotSpotBg.setAlpha(MiuiFreeformPinManagerService.EDGE_AREA);
        this.mBottomBarRightHotSpotTextView.setAlpha(MiuiFreeformPinManagerService.EDGE_AREA);
        this.mLastHotSpotStatus = BOTTOM_BAR_HOTSPOT_STATUS_HIDE;
        this.mLastShowingHotSpot = -1;
        setVisibility(8);
    }

    public void updateState(int hotSpotNum, int targetState, float x, float y) {
        int i = this.mLastShowingHotSpot;
        if (hotSpotNum != i) {
            toHideState(i);
            this.mLastShowingHotSpot = hotSpotNum;
        }
        if (targetState == BOTTOM_BAR_HOTSPOT_STATUS_HIDE) {
            toHideState(hotSpotNum);
        } else if (targetState == BOTTOM_BAR_HOTSPOT_STATUS_SHOW) {
            toShowState(hotSpotNum);
        } else if (targetState == BOTTOM_BAR_HOTSPOT_STATUS_TIGGER) {
            toTriggerState(hotSpotNum);
        } else if (targetState == BOTTOM_BAR_HOTSPOT_STATUS_BIGGER) {
            toBiggerState(hotSpotNum, x, y);
        }
    }

    private void toBiggerState(int hotSpotNum, float x, float y) {
        float rate = getRate(hotSpotNum, x, y);
        if (hotSpotNum == 1) {
            FrameLayout frameLayout = this.mBottomBarLeftHotSpot;
            int i = this.mBottomBarHotSpotLong;
            updateViewSize(frameLayout, i - ((int) ((i - this.mBottomBarHotSpotShort) * rate)));
            if (this.mLastHotSpotStatus != BOTTOM_BAR_HOTSPOT_STATUS_BIGGER) {
                setViewAlpha(this.mBottomBarLeftHotSpot, 0.6f);
                setViewAlpha(this.mBottomBarLeftHotSpotBg, 1.0f);
            }
        } else if (hotSpotNum == 2) {
            FrameLayout frameLayout2 = this.mBottomBarRightHotSpot;
            int i2 = this.mBottomBarHotSpotLong;
            updateViewSize(frameLayout2, i2 - ((int) ((i2 - this.mBottomBarHotSpotShort) * rate)));
            if (this.mLastHotSpotStatus != BOTTOM_BAR_HOTSPOT_STATUS_BIGGER) {
                setViewAlpha(this.mBottomBarRightHotSpot, 0.6f);
                setViewAlpha(this.mBottomBarRightHotSpotBg, 1.0f);
            }
        }
        this.mLastHotSpotStatus = BOTTOM_BAR_HOTSPOT_STATUS_BIGGER;
    }

    private float getRate(int hotSpotNum, float x, float y) {
        float xRate = MiuiFreeformPinManagerService.EDGE_AREA;
        float yRate = MiuiFreeformPinManagerService.EDGE_AREA;
        boolean isPortrait = isPortrait();
        if (isPortrait) {
            if (hotSpotNum == 1 || hotSpotNum == 2) {
                yRate = ((y - MiuiMultiWindowUtils.FREEFORM_BOTTOM_HOTSPOT_TRIGGER_PORTRAIT_VERTICAL_TOP_MARGIN) - MiuiMultiWindowUtils.FREEFORM_BOTTOM_HOTSPOT_TRIGGER_PORTRAIT_RADIUS) / (((MiuiMultiWindowUtils.FREEFORM_BOTTOM_HOTSPOT_REMINDER_PORTRAIT_VERTICAL_TOP_MARGIN + MiuiMultiWindowUtils.FREEFORM_BOTTOM_HOTSPOT_REMINDER_PORTRAIT_RADIUS) - MiuiMultiWindowUtils.FREEFORM_BOTTOM_HOTSPOT_TRIGGER_PORTRAIT_VERTICAL_TOP_MARGIN) - MiuiMultiWindowUtils.FREEFORM_BOTTOM_HOTSPOT_TRIGGER_PORTRAIT_RADIUS);
            }
        } else if (hotSpotNum == 1) {
            xRate = (x - (MiuiMultiWindowUtils.FREEFORM_BOTTOM_HOTSPOT_TRIGGER_LANDCAPE_HORIZONTAL_TOP_MARGIN + MiuiMultiWindowUtils.FREEFORM_BOTTOM_HOTSPOT_TRIGGER_LANDCAPE_RADIUS)) / ((MiuiMultiWindowUtils.FREEFORM_BOTTOM_HOTSPOT_REMINDER_LANDCAPE_RADIUS + MiuiMultiWindowUtils.FREEFORM_BOTTOM_HOTSPOT_REMINDER_LANDCAPE_HORIZONTAL_TOP_MARGIN) - (MiuiMultiWindowUtils.FREEFORM_BOTTOM_HOTSPOT_TRIGGER_LANDCAPE_HORIZONTAL_TOP_MARGIN + MiuiMultiWindowUtils.FREEFORM_BOTTOM_HOTSPOT_TRIGGER_LANDCAPE_RADIUS));
            yRate = (y - (MiuiMultiWindowUtils.FREEFORM_BOTTOM_HOTSPOT_TRIGGER_LANDCAPE_VERTICAL_TOP_MARGIN + MiuiMultiWindowUtils.FREEFORM_BOTTOM_HOTSPOT_TRIGGER_LANDCAPE_RADIUS)) / ((MiuiMultiWindowUtils.FREEFORM_BOTTOM_HOTSPOT_REMINDER_LANDCAPE_VERTICAL_TOP_MARGIN + MiuiMultiWindowUtils.FREEFORM_BOTTOM_HOTSPOT_REMINDER_LANDCAPE_RADIUS) - (MiuiMultiWindowUtils.FREEFORM_BOTTOM_HOTSPOT_TRIGGER_LANDCAPE_VERTICAL_TOP_MARGIN + MiuiMultiWindowUtils.FREEFORM_BOTTOM_HOTSPOT_TRIGGER_LANDCAPE_RADIUS));
        } else if (hotSpotNum == 2) {
            xRate = ((getDisplayWidth() - (MiuiMultiWindowUtils.FREEFORM_BOTTOM_HOTSPOT_TRIGGER_LANDCAPE_HORIZONTAL_TOP_MARGIN + MiuiMultiWindowUtils.FREEFORM_BOTTOM_HOTSPOT_TRIGGER_LANDCAPE_RADIUS)) - x) / ((MiuiMultiWindowUtils.FREEFORM_BOTTOM_HOTSPOT_REMINDER_LANDCAPE_RADIUS + MiuiMultiWindowUtils.FREEFORM_BOTTOM_HOTSPOT_REMINDER_LANDCAPE_HORIZONTAL_TOP_MARGIN) - (MiuiMultiWindowUtils.FREEFORM_BOTTOM_HOTSPOT_TRIGGER_LANDCAPE_HORIZONTAL_TOP_MARGIN + MiuiMultiWindowUtils.FREEFORM_BOTTOM_HOTSPOT_TRIGGER_LANDCAPE_RADIUS));
            yRate = (y - (MiuiMultiWindowUtils.FREEFORM_BOTTOM_HOTSPOT_TRIGGER_LANDCAPE_VERTICAL_TOP_MARGIN + MiuiMultiWindowUtils.FREEFORM_BOTTOM_HOTSPOT_TRIGGER_LANDCAPE_RADIUS)) / ((MiuiMultiWindowUtils.FREEFORM_BOTTOM_HOTSPOT_REMINDER_LANDCAPE_VERTICAL_TOP_MARGIN + MiuiMultiWindowUtils.FREEFORM_BOTTOM_HOTSPOT_REMINDER_LANDCAPE_RADIUS) - (MiuiMultiWindowUtils.FREEFORM_BOTTOM_HOTSPOT_TRIGGER_LANDCAPE_VERTICAL_TOP_MARGIN + MiuiMultiWindowUtils.FREEFORM_BOTTOM_HOTSPOT_TRIGGER_LANDCAPE_RADIUS));
        }
        return Math.max((float) MiuiFreeformPinManagerService.EDGE_AREA, Math.max(xRate, yRate));
    }

    private void toShowState(int hotSpotNum) {
        Log.d(TAG, "show() hotSpotNum: " + hotSpotNum);
        if (hotSpotNum != this.mLastShowingHotSpot || this.mLastHotSpotStatus != BOTTOM_BAR_HOTSPOT_STATUS_SHOW) {
            if (hotSpotNum == 1) {
                setViewAlpha(this.mBottomBarLeftHotSpot, 0.6f);
                setViewAlpha(this.mBottomBarLeftHotSpotIcon, 1.0f);
                setViewAlpha(this.mBottomBarLeftHotSpotBg, 1.0f);
                setViewAlpha(this.mBottomBarLeftHotSpotTextView, MiuiFreeformPinManagerService.EDGE_AREA);
            } else if (hotSpotNum == 2) {
                setViewAlpha(this.mBottomBarRightHotSpot, 0.6f);
                setViewAlpha(this.mBottomBarRightHotSpotIcon, 1.0f);
                setViewAlpha(this.mBottomBarRightHotSpotBg, 1.0f);
                setViewAlpha(this.mBottomBarRightHotSpotTextView, MiuiFreeformPinManagerService.EDGE_AREA);
            }
            this.mLastHotSpotStatus = BOTTOM_BAR_HOTSPOT_STATUS_SHOW;
        }
    }

    private void toHideState(int hotSpotNum) {
        Log.d(TAG, "hide() hotSpotNum: " + hotSpotNum);
        if (hotSpotNum != this.mLastShowingHotSpot || this.mLastHotSpotStatus != BOTTOM_BAR_HOTSPOT_STATUS_HIDE) {
            toShowState(hotSpotNum);
            if (hotSpotNum == 1) {
                setViewAlpha(this.mBottomBarLeftHotSpot, MiuiFreeformPinManagerService.EDGE_AREA);
                setViewAlpha(this.mBottomBarLeftHotSpotIcon, MiuiFreeformPinManagerService.EDGE_AREA);
                setViewAlpha(this.mBottomBarLeftHotSpotBg, MiuiFreeformPinManagerService.EDGE_AREA);
                setViewAlpha(this.mBottomBarLeftHotSpotTextView, MiuiFreeformPinManagerService.EDGE_AREA);
            } else if (hotSpotNum == 2) {
                setViewAlpha(this.mBottomBarRightHotSpot, MiuiFreeformPinManagerService.EDGE_AREA);
                setViewAlpha(this.mBottomBarRightHotSpotIcon, MiuiFreeformPinManagerService.EDGE_AREA);
                setViewAlpha(this.mBottomBarRightHotSpotBg, MiuiFreeformPinManagerService.EDGE_AREA);
                setViewAlpha(this.mBottomBarRightHotSpotTextView, MiuiFreeformPinManagerService.EDGE_AREA);
            } else if (hotSpotNum == -1) {
                toHideState(1);
                toHideState(2);
            }
            this.mLastHotSpotStatus = BOTTOM_BAR_HOTSPOT_STATUS_HIDE;
        }
    }

    private void toTriggerState(int hotSpotNum) {
        Log.d(TAG, "toLargeState() hotSpotNum: " + hotSpotNum);
        if (hotSpotNum != this.mLastShowingHotSpot || this.mLastHotSpotStatus != BOTTOM_BAR_HOTSPOT_STATUS_TIGGER) {
            if (hotSpotNum == 1) {
                updateViewSize(this.mBottomBarLeftHotSpot, this.mBottomBarHotSpotLong);
                setViewAlpha(this.mBottomBarLeftHotSpot, 1.0f);
                setViewAlpha(this.mBottomBarLeftHotSpotIcon, 1.0f);
                setViewAlpha(this.mBottomBarLeftHotSpotBg, 1.0f);
                setViewAlpha(this.mBottomBarLeftHotSpotTextView, 1.0f);
            } else if (hotSpotNum == 2) {
                updateViewSize(this.mBottomBarRightHotSpot, this.mBottomBarHotSpotLong);
                setViewAlpha(this.mBottomBarRightHotSpot, 1.0f);
                setViewAlpha(this.mBottomBarRightHotSpotIcon, 1.0f);
                setViewAlpha(this.mBottomBarRightHotSpotBg, 1.0f);
                setViewAlpha(this.mBottomBarRightHotSpotTextView, 1.0f);
            }
            this.mLastHotSpotStatus = BOTTOM_BAR_HOTSPOT_STATUS_TIGGER;
        }
    }

    private void updateViewSize(View view, int size) {
        view.getLayoutParams().height = size;
        view.getLayoutParams().width = size;
        view.requestLayout();
    }

    private void setViewAlpha(View view, float targetAlpha) {
        if (view.getAlpha() == targetAlpha) {
            view.animate().cancel();
            view.setAlpha(targetAlpha);
            return;
        }
        long duration = ANIMATIN_MAX_DURATION * Math.abs(targetAlpha - view.getAlpha());
        view.animate().alpha(targetAlpha).setDuration(duration).start();
    }

    private int getDisplayWidth() {
        this.mWindowManager.getDefaultDisplay().getRealMetrics(this.mOutMetrics);
        return this.mOutMetrics.widthPixels;
    }

    private boolean isPortrait() {
        return getResources().getConfiguration().orientation == 1;
    }

    public void show() {
        animate().cancel();
        setAlpha(MiuiFreeformPinManagerService.EDGE_AREA);
        setVisibility(0);
        animate().alpha(1.0f).setDuration(ANIMATIN_MAX_DURATION);
    }

    public void hide() {
        animate().cancel();
        animate().alpha(MiuiFreeformPinManagerService.EDGE_AREA).setDuration(ANIMATIN_MAX_DURATION).setListener(new AnimatorListenerAdapter() { // from class: com.android.server.wm.MiuiFreeformBottomBarHotSpotView.1
            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationEnd(Animator animation) {
                MiuiFreeformBottomBarHotSpotView.this.setVisibility(8);
                MiuiFreeformBottomBarHotSpotView.this.setAlpha(1.0f);
                MiuiFreeformBottomBarHotSpotView.this.animate().setListener(null);
            }
        });
    }
}
