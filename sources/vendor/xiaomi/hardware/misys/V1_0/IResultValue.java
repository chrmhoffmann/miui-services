package vendor.xiaomi.hardware.misys.V1_0;

import java.util.ArrayList;
/* loaded from: classes.dex */
public final class IResultValue {
    public static final int MISYS_E2BIG = 7;
    public static final int MISYS_EACCES = 13;
    public static final int MISYS_EAGAIN = 11;
    public static final int MISYS_EBADF = 9;
    public static final int MISYS_EBUSY = 16;
    public static final int MISYS_ECHILD = 10;
    public static final int MISYS_EEXIST = 17;
    public static final int MISYS_EFAULT = 14;
    public static final int MISYS_EFBIG = 27;
    public static final int MISYS_EINTR = 4;
    public static final int MISYS_EINVAL = 22;
    public static final int MISYS_EIO = 5;
    public static final int MISYS_EISDIR = 21;
    public static final int MISYS_EMFILE = 24;
    public static final int MISYS_EMLINK = 31;
    public static final int MISYS_ENFILE = 23;
    public static final int MISYS_ENODEV = 19;
    public static final int MISYS_ENOEXEC = 8;
    public static final int MISYS_ENOMEM = 12;
    public static final int MISYS_ENOSPC = 28;
    public static final int MISYS_ENOTBLK = 15;
    public static final int MISYS_ENOTDIR = 20;
    public static final int MISYS_ENOTTY = 25;
    public static final int MISYS_ENXIO = 6;
    public static final int MISYS_EPERM = 1;
    public static final int MISYS_EPIPE = 32;
    public static final int MISYS_EROFS = 30;
    public static final int MISYS_ESPIPE = 29;
    public static final int MISYS_ESRCH = 3;
    public static final int MISYS_ETXTBSY = 26;
    public static final int MISYS_EXDEV = 18;
    public static final int MISYS_NOENT = 2;
    public static final int MISYS_SUCCESS = 0;
    public static final int MISYS_UNKNOWN = 1024;

    public static final String toString(int o) {
        if (o == 0) {
            return "MISYS_SUCCESS";
        }
        if (o == 1) {
            return "MISYS_EPERM";
        }
        if (o == 2) {
            return "MISYS_NOENT";
        }
        if (o == 3) {
            return "MISYS_ESRCH";
        }
        if (o == 4) {
            return "MISYS_EINTR";
        }
        if (o == 5) {
            return "MISYS_EIO";
        }
        if (o == 6) {
            return "MISYS_ENXIO";
        }
        if (o == 7) {
            return "MISYS_E2BIG";
        }
        if (o == 8) {
            return "MISYS_ENOEXEC";
        }
        if (o == 9) {
            return "MISYS_EBADF";
        }
        if (o == 10) {
            return "MISYS_ECHILD";
        }
        if (o == 11) {
            return "MISYS_EAGAIN";
        }
        if (o == 12) {
            return "MISYS_ENOMEM";
        }
        if (o == 13) {
            return "MISYS_EACCES";
        }
        if (o == 14) {
            return "MISYS_EFAULT";
        }
        if (o == 15) {
            return "MISYS_ENOTBLK";
        }
        if (o == 16) {
            return "MISYS_EBUSY";
        }
        if (o == 17) {
            return "MISYS_EEXIST";
        }
        if (o == 18) {
            return "MISYS_EXDEV";
        }
        if (o == 19) {
            return "MISYS_ENODEV";
        }
        if (o == 20) {
            return "MISYS_ENOTDIR";
        }
        if (o == 21) {
            return "MISYS_EISDIR";
        }
        if (o == 22) {
            return "MISYS_EINVAL";
        }
        if (o == 23) {
            return "MISYS_ENFILE";
        }
        if (o == 24) {
            return "MISYS_EMFILE";
        }
        if (o == 25) {
            return "MISYS_ENOTTY";
        }
        if (o == 26) {
            return "MISYS_ETXTBSY";
        }
        if (o == 27) {
            return "MISYS_EFBIG";
        }
        if (o == 28) {
            return "MISYS_ENOSPC";
        }
        if (o == 29) {
            return "MISYS_ESPIPE";
        }
        if (o == 30) {
            return "MISYS_EROFS";
        }
        if (o == 31) {
            return "MISYS_EMLINK";
        }
        if (o == 32) {
            return "MISYS_EPIPE";
        }
        if (o == 1024) {
            return "MISYS_UNKNOWN";
        }
        return "0x" + Integer.toHexString(o);
    }

    public static final String dumpBitfield(int o) {
        ArrayList<String> list = new ArrayList<>();
        int flipped = 0;
        list.add("MISYS_SUCCESS");
        if ((o & 1) == 1) {
            list.add("MISYS_EPERM");
            flipped = 0 | 1;
        }
        if ((o & 2) == 2) {
            list.add("MISYS_NOENT");
            flipped |= 2;
        }
        if ((o & 3) == 3) {
            list.add("MISYS_ESRCH");
            flipped |= 3;
        }
        if ((o & 4) == 4) {
            list.add("MISYS_EINTR");
            flipped |= 4;
        }
        if ((o & 5) == 5) {
            list.add("MISYS_EIO");
            flipped |= 5;
        }
        if ((o & 6) == 6) {
            list.add("MISYS_ENXIO");
            flipped |= 6;
        }
        if ((o & 7) == 7) {
            list.add("MISYS_E2BIG");
            flipped |= 7;
        }
        if ((o & 8) == 8) {
            list.add("MISYS_ENOEXEC");
            flipped |= 8;
        }
        if ((o & 9) == 9) {
            list.add("MISYS_EBADF");
            flipped |= 9;
        }
        if ((o & 10) == 10) {
            list.add("MISYS_ECHILD");
            flipped |= 10;
        }
        if ((o & 11) == 11) {
            list.add("MISYS_EAGAIN");
            flipped |= 11;
        }
        if ((o & 12) == 12) {
            list.add("MISYS_ENOMEM");
            flipped |= 12;
        }
        if ((o & 13) == 13) {
            list.add("MISYS_EACCES");
            flipped |= 13;
        }
        if ((o & 14) == 14) {
            list.add("MISYS_EFAULT");
            flipped |= 14;
        }
        if ((o & 15) == 15) {
            list.add("MISYS_ENOTBLK");
            flipped |= 15;
        }
        if ((o & 16) == 16) {
            list.add("MISYS_EBUSY");
            flipped |= 16;
        }
        if ((o & 17) == 17) {
            list.add("MISYS_EEXIST");
            flipped |= 17;
        }
        if ((o & 18) == 18) {
            list.add("MISYS_EXDEV");
            flipped |= 18;
        }
        if ((o & 19) == 19) {
            list.add("MISYS_ENODEV");
            flipped |= 19;
        }
        if ((o & 20) == 20) {
            list.add("MISYS_ENOTDIR");
            flipped |= 20;
        }
        if ((o & 21) == 21) {
            list.add("MISYS_EISDIR");
            flipped |= 21;
        }
        if ((o & 22) == 22) {
            list.add("MISYS_EINVAL");
            flipped |= 22;
        }
        if ((o & 23) == 23) {
            list.add("MISYS_ENFILE");
            flipped |= 23;
        }
        if ((o & 24) == 24) {
            list.add("MISYS_EMFILE");
            flipped |= 24;
        }
        if ((o & 25) == 25) {
            list.add("MISYS_ENOTTY");
            flipped |= 25;
        }
        if ((o & 26) == 26) {
            list.add("MISYS_ETXTBSY");
            flipped |= 26;
        }
        if ((o & 27) == 27) {
            list.add("MISYS_EFBIG");
            flipped |= 27;
        }
        if ((o & 28) == 28) {
            list.add("MISYS_ENOSPC");
            flipped |= 28;
        }
        if ((o & 29) == 29) {
            list.add("MISYS_ESPIPE");
            flipped |= 29;
        }
        if ((o & 30) == 30) {
            list.add("MISYS_EROFS");
            flipped |= 30;
        }
        if ((o & 31) == 31) {
            list.add("MISYS_EMLINK");
            flipped |= 31;
        }
        if ((o & 32) == 32) {
            list.add("MISYS_EPIPE");
            flipped |= 32;
        }
        if ((o & 1024) == 1024) {
            list.add("MISYS_UNKNOWN");
            flipped |= 1024;
        }
        if (o != flipped) {
            list.add("0x" + Integer.toHexString((~flipped) & o));
        }
        return String.join(" | ", list);
    }
}
