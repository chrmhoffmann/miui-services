package com.miui.server.input.knock.view;

import com.miui.server.input.knock.view.KnockGesturePathView;
/* loaded from: classes.dex */
public interface KnockPathListener {
    KnockGesturePathView.KnockPointerState getPointerPathData();

    void hideView();
}
