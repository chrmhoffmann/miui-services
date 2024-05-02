package com.android.server.wm;

import java.io.PrintWriter;
/* loaded from: classes.dex */
public interface IController {
    void initialize();

    default void dumpLocked(PrintWriter pw, String prefix) {
    }

    default void setWindowManager(WindowManagerService wms) {
    }
}
