package com.miui.server.sentinel;
/* loaded from: classes.dex */
public class MiuiSentinelEvent {
    public static final int EVENT_DMABUFF_LEAK = 16;
    public static final int EVENT_FD_LEAK = 19;
    public static final int EVENT_HEAP_TRACK = 22;
    public static final int EVENT_JAVAHEAP_LEAK = 17;
    public static final int EVENT_KGSL_LEAK = 15;
    public static final int EVENT_NATIVEHEAP_LEAK = 18;
    public static final int EVENT_OPENFILE_AMOUNT = 9;
    public static final int EVENT_OPENFILE_FDSIZE = 8;
    public static final int EVENT_PROCKTHREAD_AMOUNT = 12;
    public static final int EVENT_PROCNATIVE_AMOUNT = 11;
    public static final int EVENT_PROCRUNTIME_AMOUNT = 10;
    public static final int EVENT_PROCTOTAL_AMOUNT = 13;
    public static final int EVENT_PROC_LEAK = 21;
    public static final int EVENT_RESIDENTSIZE = 7;
    public static final int EVENT_RSS_LEAK = 14;
    public static final int EVENT_THREAD_AMOUNT = 4;
    public static final int EVENT_THREAD_LEAK = 20;
    public static final int EVENT_VSSASGMEM = 5;
    public static final int EVENT_VSSDALVIK = 3;
    public static final int EVENT_VSSDMABUFF = 6;
    public static final int EVENT_VSSHEAP = 2;
    public static final int EVENT_VSSKGSL = 1;

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    public static int getEventType(String type) {
        boolean z;
        switch (type.hashCode()) {
            case -1924613904:
                if (type.equals("Event_VssAshmem")) {
                    z = true;
                    break;
                }
                z = true;
                break;
            case -1855221894:
                if (type.equals("Event_VssDalvik")) {
                    z = true;
                    break;
                }
                z = true;
                break;
            case -1844516948:
                if (type.equals("Event_VssDmaBuf")) {
                    z = true;
                    break;
                }
                z = true;
                break;
            case -1765012682:
                if (type.equals("Event_ResidentSize")) {
                    z = true;
                    break;
                }
                z = true;
                break;
            case -747534493:
                if (type.equals("Event_OpenFileAmount")) {
                    z = true;
                    break;
                }
                z = true;
                break;
            case -694544515:
                if (type.equals("Event_VssHeap")) {
                    z = true;
                    break;
                }
                z = true;
                break;
            case -694452666:
                if (type.equals("Event_VssKgsl")) {
                    z = false;
                    break;
                }
                z = true;
                break;
            case -643098422:
                if (type.equals("Event_OpenFileFDSize")) {
                    z = true;
                    break;
                }
                z = true;
                break;
            case -88036086:
                if (type.equals("Event_ProcNativeAmount")) {
                    z = true;
                    break;
                }
                z = true;
                break;
            case -60729249:
                if (type.equals("Event_ProcRuntimeAmoun")) {
                    z = true;
                    break;
                }
                z = true;
                break;
            case 757310369:
                if (type.equals("Event_ProcTotalAmount")) {
                    z = true;
                    break;
                }
                z = true;
                break;
            case 940424338:
                if (type.equals("Event_ProcKthreadAmount")) {
                    z = true;
                    break;
                }
                z = true;
                break;
            case 1708698823:
                if (type.equals("Event_ThreadAmount")) {
                    z = true;
                    break;
                }
                z = true;
                break;
            default:
                z = true;
                break;
        }
        switch (z) {
            case false:
                return 1;
            case true:
                return 2;
            case true:
                return 3;
            case true:
                return 4;
            case true:
                return 5;
            case true:
                return 6;
            case true:
                return 7;
            case true:
                return 8;
            case true:
                return 9;
            case true:
                return 10;
            case true:
                return 11;
            case true:
                return 12;
            case true:
                return 13;
            default:
                return -1;
        }
    }
}
