package com.miui.server.migard;

import android.os.ShellCommand;
import java.io.PrintWriter;
/* loaded from: classes.dex */
class MiGardShellCommand extends ShellCommand {
    MiGardService mService;

    public MiGardShellCommand(MiGardService service) {
        this.mService = service;
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    public int onCommand(String cmd) {
        PrintWriter pw = getOutPrintWriter();
        if (cmd == null) {
            return handleDefaultCommands(cmd);
        }
        char c = 65535;
        try {
            switch (cmd.hashCode()) {
                case -1729603354:
                    if (cmd.equals("trace-buffer-size")) {
                        c = 3;
                        break;
                    }
                    break;
                case -487393236:
                    if (cmd.equals("dump-trace")) {
                        c = 2;
                        break;
                    }
                    break;
                case 1340897306:
                    if (cmd.equals("start-trace")) {
                        c = 0;
                        break;
                    }
                    break;
                case 1857979322:
                    if (cmd.equals("stop-trace")) {
                        c = 1;
                        break;
                    }
                    break;
            }
        } catch (Exception e) {
            pw.println(e);
        }
        switch (c) {
            case 0:
                boolean async = Boolean.parseBoolean(getNextArgRequired());
                this.mService.startDefaultTrace(async);
                return 0;
            case 1:
                boolean compressed = Boolean.parseBoolean(getNextArgRequired());
                this.mService.stopTrace(compressed);
                return 0;
            case 2:
                boolean compressed2 = Boolean.parseBoolean(getNextArgRequired());
                this.mService.dumpTrace(compressed2);
                return 0;
            case 3:
                this.mService.setTraceBufferSize(Integer.parseInt(getNextArgRequired()));
                return 0;
            default:
                if (!this.mService.mMemCleaner.onShellCommand(cmd, getNextArgRequired(), pw)) {
                    return handleDefaultCommands(cmd);
                }
                return 0;
        }
    }

    public void onHelp() {
        PrintWriter pw = getOutPrintWriter();
        pw.println("MiGardService commands:");
        pw.println();
        pw.println("start-trace [async=true|false]");
        pw.println();
        pw.println("stop-trace [compressed=true|false]");
        pw.println();
        pw.println("dump-trace [compressed=true|false]");
        pw.println();
        pw.println("trace-buffer-size [size KB]");
        pw.println();
        this.mService.mMemCleaner.onShellHelp(pw);
    }
}
