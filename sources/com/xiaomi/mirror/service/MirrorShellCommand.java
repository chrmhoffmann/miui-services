package com.xiaomi.mirror.service;

import android.os.ShellCommand;
import java.io.PrintWriter;
/* loaded from: classes.dex */
public class MirrorShellCommand extends ShellCommand {
    public int onCommand(String cmd) {
        boolean z;
        if (cmd == null) {
            return handleDefaultCommands(cmd);
        }
        PrintWriter pw = getOutPrintWriter();
        try {
            switch (cmd.hashCode()) {
                case -356192864:
                    if (cmd.equals("reset-drag-and-drop")) {
                        z = false;
                        break;
                    }
                default:
                    z = true;
                    break;
            }
            switch (z) {
                case false:
                    MirrorService.get().resetDragAndDropController();
                    pw.println("Reset successfully");
                    return 0;
                default:
                    return -1;
            }
        } finally {
            pw.println();
        }
    }

    public void onHelp() {
        PrintWriter pw = getOutPrintWriter();
        pw.println("No help.");
    }
}
