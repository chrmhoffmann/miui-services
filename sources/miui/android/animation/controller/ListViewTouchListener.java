package miui.android.animation.controller;

import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.AbsListView;
import java.util.Map;
import java.util.WeakHashMap;
/* loaded from: classes.dex */
public class ListViewTouchListener implements View.OnTouchListener {
    private int mTouchSlop;
    private WeakHashMap<View, View.OnTouchListener> mListeners = new WeakHashMap<>();
    private Rect mRect = new Rect();
    private float mDownX = Float.MAX_VALUE;
    private float mDownY = Float.MAX_VALUE;

    public ListViewTouchListener(AbsListView v) {
        this.mTouchSlop = ViewConfiguration.get(v.getContext()).getScaledTouchSlop();
    }

    @Override // android.view.View.OnTouchListener
    public boolean onTouch(View v, MotionEvent event) {
        boolean isScrolling = false;
        switch (event.getActionMasked()) {
            case 0:
                this.mDownX = event.getRawX();
                this.mDownY = event.getRawY();
                break;
            case 1:
            default:
                this.mDownY = Float.MAX_VALUE;
                this.mDownX = Float.MAX_VALUE;
                break;
            case 2:
                isScrolling = event.getRawY() - this.mDownY > ((float) this.mTouchSlop) || event.getRawX() - this.mDownX > ((float) this.mTouchSlop);
                break;
        }
        notifyItemListeners((AbsListView) v, event, isScrolling);
        return false;
    }

    public void putListener(View view, View.OnTouchListener touchListener) {
        this.mListeners.put(view, touchListener);
    }

    private void notifyItemListeners(AbsListView listView, MotionEvent event, boolean isScrolling) {
        View touchedView = getTouchedItemView(listView, event);
        for (Map.Entry<View, View.OnTouchListener> entry : this.mListeners.entrySet()) {
            View view = entry.getKey();
            boolean isOnTouchView = !isScrolling && view == touchedView;
            entry.getValue().onTouch(view, isOnTouchView ? event : null);
        }
    }

    private View getTouchedItemView(AbsListView listView, MotionEvent event) {
        int x = (int) event.getX();
        int y = (int) event.getY();
        int n = listView.getChildCount();
        for (int i = 0; i < n; i++) {
            View child = listView.getChildAt(i);
            child.getLocalVisibleRect(this.mRect);
            this.mRect.offset(child.getLeft(), child.getTop());
            if (this.mRect.contains(x, y)) {
                return child;
            }
        }
        return null;
    }
}
