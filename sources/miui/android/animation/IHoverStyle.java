package miui.android.animation;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.view.MotionEvent;
import android.view.View;
import miui.android.animation.base.AnimConfig;
/* loaded from: classes.dex */
public interface IHoverStyle extends IStateContainer {

    /* loaded from: classes.dex */
    public enum HoverEffect {
        NORMAL,
        FLOATED,
        FLOATED_WRAPPED
    }

    /* loaded from: classes.dex */
    public enum HoverType {
        ENTER,
        EXIT
    }

    void addMagicPoint(Point point);

    void clearMagicPoint();

    IHoverStyle clearTintColor();

    void handleHoverOf(View view, AnimConfig... animConfigArr);

    void hoverEnter(AnimConfig... animConfigArr);

    void hoverExit(AnimConfig... animConfigArr);

    void hoverMove(View view, MotionEvent motionEvent, AnimConfig... animConfigArr);

    void ignoreHoverOf(View view);

    boolean isMagicView();

    void onMotionEvent(MotionEvent motionEvent);

    void onMotionEventEx(View view, MotionEvent motionEvent, AnimConfig... animConfigArr);

    IHoverStyle setAlpha(float f, HoverType... hoverTypeArr);

    IHoverStyle setBackgroundColor(float f, float f2, float f3, float f4);

    IHoverStyle setBackgroundColor(int i);

    IHoverStyle setCorner(float f);

    IHoverStyle setEffect(HoverEffect hoverEffect);

    void setHoverEnter();

    void setHoverExit();

    void setMagicView(boolean z);

    IHoverStyle setParentView(View view);

    void setPointerHide(boolean z);

    void setPointerShape(Bitmap bitmap);

    void setPointerShapeType(int i);

    IHoverStyle setScale(float f, HoverType... hoverTypeArr);

    IHoverStyle setTint(float f, float f2, float f3, float f4);

    IHoverStyle setTint(int i);

    IHoverStyle setTintMode(int i);

    IHoverStyle setTranslate(float f, HoverType... hoverTypeArr);

    void setWrapped(boolean z);
}
