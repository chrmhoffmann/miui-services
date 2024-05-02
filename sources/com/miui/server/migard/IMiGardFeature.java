package com.miui.server.migard;

import java.io.PrintWriter;
/* loaded from: classes.dex */
public interface IMiGardFeature {
    void dump(PrintWriter printWriter);

    boolean onShellCommand(String str, String str2, PrintWriter printWriter);

    void onShellHelp(PrintWriter printWriter);
}
