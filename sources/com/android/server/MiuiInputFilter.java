package com.android.server;

import android.content.Context;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.server.am.SplitScreenReporter;
import android.view.InputEvent;
import android.view.InputFilter;
import android.view.KeyEvent;
import android.view.MotionEvent;
import com.android.server.wm.MiuiFreeformPinManagerService;
import java.util.ArrayList;
import java.util.List;
import miui.util.FeatureParser;
/* loaded from: classes.dex */
public class MiuiInputFilter extends InputFilter {
    static int[][] ENTERED_LISTEN_COMBINATION_KEYS = null;
    private static int MIDDLE_KEYCODE = 0;
    static int[][] NOT_ENTERED_LISTEN_COMBINATION_KEYS = null;
    private static final String PERSIST_SYS_BACKTOUCH_PROPERTY = "persist.sys.backtouch";
    private static final String PERSIST_SYS_HANDSWAP_PROPERTY = "persist.sys.handswap";
    private static boolean isDpadDevice;
    private static float sEdgeDistance;
    private boolean mCitTestEnabled;
    private ClickableRect mClickingRect;
    private Context mContext;
    private boolean mInstalled;
    private int mSampleDura;
    private MotionEvent.PointerCoords[] mTempPointerCoords;
    private MotionEvent.PointerProperties[] mTempPointerProperties;
    private boolean mWasInside;
    private final double MAX_COS = Math.cos(0.3490658503988659d);
    private List<KeyData> mPendingKeys = new ArrayList();
    private List<ClickableRect> mOutsideClickableRects = new ArrayList();
    private ArrayList<PointF> mPoints = new ArrayList<>();
    private H mHandler = new H(DisplayThread.get().getLooper());

    static {
        boolean z = FeatureParser.getBoolean("middle_keycode_is_dpad_center", false);
        isDpadDevice = z;
        int i = z ? 23 : 3;
        MIDDLE_KEYCODE = i;
        NOT_ENTERED_LISTEN_COMBINATION_KEYS = new int[][]{new int[]{i, 4}, new int[]{i, 82}};
        ENTERED_LISTEN_COMBINATION_KEYS = new int[][]{new int[]{i, 4}, new int[]{i, 82}, new int[]{4, i}, new int[]{82, i}};
    }

    public boolean isInstalled() {
        return this.mInstalled;
    }

    /* loaded from: classes.dex */
    public static class KeyData {
        boolean isSended;
        KeyEvent keyEvent;
        int policyFlags;

        KeyData() {
        }
    }

    /* loaded from: classes.dex */
    public class H extends Handler {
        public static final int MSG_DOUBLE_CLICK_DELAY = 1;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public H(Looper looper) {
            super(looper);
            MiuiInputFilter.this = r1;
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    MiuiInputFilter.this.flushPending();
                    break;
            }
            super.handleMessage(msg);
        }
    }

    /* loaded from: classes.dex */
    private static class ClickableRect {
        public Runnable mClickListener;
        public Rect mRect;

        public ClickableRect(Rect rect, Runnable listener) {
            this.mRect = rect;
            this.mClickListener = listener;
        }
    }

    public MiuiInputFilter(Context context) {
        super(DisplayThread.get().getLooper());
        this.mContext = context;
        sEdgeDistance = context.getResources().getDisplayMetrics().density * 20.0f;
    }

    public void addOutsideClickableRect(Rect rect, Runnable listener) {
        this.mOutsideClickableRects.add(new ClickableRect(rect, listener));
    }

    public void removeOutsideClickableRect(Runnable listener) {
        for (int i = this.mOutsideClickableRects.size() - 1; i >= 0; i--) {
            if (this.mOutsideClickableRects.get(i).mClickListener == listener) {
                this.mOutsideClickableRects.remove(i);
            }
        }
    }

    public void updateOutsideClickableRect(Rect rect, Runnable listener) {
        boolean containListener = false;
        for (int i = this.mOutsideClickableRects.size() - 1; i >= 0; i--) {
            if (this.mOutsideClickableRects.get(i).mClickListener == listener) {
                this.mOutsideClickableRects.remove(i);
                containListener = true;
            }
        }
        if (containListener) {
            this.mOutsideClickableRects.add(new ClickableRect(rect, listener));
        }
    }

    public void setCitTestEnabled(boolean enabled) {
        this.mCitTestEnabled = enabled;
    }

    private void processMotionEventForBackTouch(MotionEvent event, int policyFlags) {
        switch (event.getAction()) {
            case 1:
                this.mSampleDura = 0;
                this.mPoints.clear();
                return;
            case 2:
                PointF curPointF = new PointF(event.getRawX(), event.getRawY());
                int i = this.mSampleDura + 1;
                this.mSampleDura = i;
                if (i >= 5) {
                    this.mPoints.add(curPointF);
                    this.mSampleDura = 0;
                }
                if (this.mPoints.size() >= 3) {
                    changeVolumeForBackTouch(policyFlags);
                    this.mPoints.clear();
                    return;
                }
                return;
            default:
                return;
        }
    }

    private void changeVolumeForBackTouch(int policyFlags) {
        PointF firstP = this.mPoints.get(0);
        PointF secondP = this.mPoints.get(1);
        PointF thirdP = this.mPoints.get(2);
        float volumeChange = MiuiFreeformPinManagerService.EDGE_AREA;
        double cosTheta = (((secondP.x - firstP.x) * (thirdP.x - secondP.x)) + ((secondP.y - firstP.y) * (thirdP.y - secondP.y))) / (Math.hypot(secondP.x - firstP.x, secondP.y - firstP.y) * Math.hypot(thirdP.x - secondP.x, thirdP.y - secondP.y));
        if (Math.abs(cosTheta) < this.MAX_COS) {
            volumeChange = ((secondP.x - firstP.x) * (thirdP.y - secondP.y)) - ((thirdP.x - secondP.x) * (secondP.y - firstP.y));
        }
        if (volumeChange != MiuiFreeformPinManagerService.EDGE_AREA) {
            long time = SystemClock.uptimeMillis();
            KeyEvent evt = new KeyEvent(time, time, 0, volumeChange > MiuiFreeformPinManagerService.EDGE_AREA ? 24 : 25, 0);
            sendInputEvent(evt, policyFlags);
            long time2 = SystemClock.uptimeMillis();
            KeyEvent evt2 = new KeyEvent(time2, time2, 1, volumeChange > MiuiFreeformPinManagerService.EDGE_AREA ? 24 : 25, 0);
            sendInputEvent(evt2, policyFlags);
        }
    }

    public void onInputEvent(InputEvent event, int policyFlags) {
        if ((event instanceof MotionEvent) && event.isFromSource(4098)) {
            if (event.getDevice() != null && "backtouch".equals(event.getDevice().getName()) && !this.mCitTestEnabled) {
                processMotionEventForBackTouch((MotionEvent) event, policyFlags);
                return;
            } else {
                super.onInputEvent((MotionEvent) event, policyFlags);
                return;
            }
        }
        super.onInputEvent(event, policyFlags);
    }

    public void onInstalled() {
        super.onInstalled();
        this.mInstalled = true;
    }

    public void onUninstalled() {
        super.onUninstalled();
        this.mInstalled = false;
        clearPendingList();
    }

    static float processCoordinate(float coordValue, float offset, float scale, float scalePivot) {
        return (scalePivot - ((scalePivot - coordValue) * scale)) - offset;
    }

    private ClickableRect findClickableRect(float x, float y) {
        for (ClickableRect c : this.mOutsideClickableRects) {
            if (c.mRect.contains((int) x, (int) y)) {
                return c;
            }
        }
        return null;
    }

    private MotionEvent.PointerCoords[] getTempPointerCoordsWithMinSize(int size) {
        MotionEvent.PointerCoords[] pointerCoordsArr = this.mTempPointerCoords;
        int oldSize = pointerCoordsArr != null ? pointerCoordsArr.length : 0;
        if (oldSize < size) {
            MotionEvent.PointerCoords[] oldTempPointerCoords = this.mTempPointerCoords;
            MotionEvent.PointerCoords[] pointerCoordsArr2 = new MotionEvent.PointerCoords[size];
            this.mTempPointerCoords = pointerCoordsArr2;
            if (oldTempPointerCoords != null) {
                System.arraycopy(oldTempPointerCoords, 0, pointerCoordsArr2, 0, oldSize);
            }
        }
        for (int i = oldSize; i < size; i++) {
            this.mTempPointerCoords[i] = new MotionEvent.PointerCoords();
        }
        return this.mTempPointerCoords;
    }

    private MotionEvent.PointerProperties[] getTempPointerPropertiesWithMinSize(int size) {
        MotionEvent.PointerProperties[] pointerPropertiesArr = this.mTempPointerProperties;
        int oldSize = pointerPropertiesArr != null ? pointerPropertiesArr.length : 0;
        if (oldSize < size) {
            MotionEvent.PointerProperties[] oldTempPointerProperties = this.mTempPointerProperties;
            MotionEvent.PointerProperties[] pointerPropertiesArr2 = new MotionEvent.PointerProperties[size];
            this.mTempPointerProperties = pointerPropertiesArr2;
            if (oldTempPointerProperties != null) {
                System.arraycopy(oldTempPointerProperties, 0, pointerPropertiesArr2, 0, oldSize);
            }
        }
        for (int i = oldSize; i < size; i++) {
            this.mTempPointerProperties[i] = new MotionEvent.PointerProperties();
        }
        return this.mTempPointerProperties;
    }

    private boolean needDelayKey(boolean isSecondKey) {
        return isSecondKey;
    }

    /* JADX WARN: Code restructure failed: missing block: B:27:0x0079, code lost:
        if (r7 == false) goto L28;
     */
    /* JADX WARN: Code restructure failed: missing block: B:51:0x00df, code lost:
        if (r3 != false) goto L52;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private synchronized void onKeyEvent(android.view.KeyEvent r14, int r15) {
        /*
            Method dump skipped, instructions count: 318
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.MiuiInputFilter.onKeyEvent(android.view.KeyEvent, int):void");
    }

    boolean checkKeyNeedListen(int keyCode) {
        int[][] listenCombinationKeys = getListenCombinationKeys();
        for (int[] iArr : listenCombinationKeys) {
            if (iArr[0] == keyCode) {
                return true;
            }
        }
        return false;
    }

    int[][] getListenCombinationKeys() {
        return ENTERED_LISTEN_COMBINATION_KEYS;
    }

    boolean checkSecondKey(int secondKeyCode) {
        int[][] listenCombinationKeys = getListenCombinationKeys();
        int firstKeyCode = this.mPendingKeys.get(0).keyEvent.getKeyCode();
        for (int[] keySequence : listenCombinationKeys) {
            if (keySequence[0] == firstKeyCode && keySequence[1] == secondKeyCode) {
                return true;
            }
        }
        return false;
    }

    synchronized void triggerCombinationClick() {
        String handswap = SystemProperties.get(PERSIST_SYS_HANDSWAP_PROPERTY, "0");
        SplitScreenReporter.ACTION_ENTER_SPLIT.equals(handswap);
    }

    synchronized void flushPending() {
        for (int i = 0; i < this.mPendingKeys.size(); i++) {
            KeyData keyData = this.mPendingKeys.get(i);
            if (!keyData.isSended) {
                sendInputEvent(keyData.keyEvent, this.mPendingKeys.get(i).policyFlags);
            }
        }
        clearPendingList();
    }

    synchronized void addPendingData(KeyEvent keyEvent, int policyFlags, int index, boolean delayEnhance, boolean isSended) {
        this.mHandler.removeMessages(1);
        KeyData keyData = new KeyData();
        keyData.keyEvent = keyEvent;
        keyData.policyFlags = policyFlags;
        keyData.isSended = isSended;
        if (index < 0) {
            this.mPendingKeys.add(keyData);
        } else {
            this.mPendingKeys.add(index, keyData);
        }
    }

    synchronized void clearPendingList() {
        this.mHandler.removeMessages(1);
        this.mPendingKeys.clear();
    }
}
