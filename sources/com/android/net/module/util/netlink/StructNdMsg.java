package com.android.net.module.util.netlink;

import android.system.OsConstants;
import com.android.server.padkeyboard.iic.CommunicationUtil;
import java.nio.ByteBuffer;
/* loaded from: classes.dex */
public class StructNdMsg {
    public static final short NUD_DELAY = 8;
    public static final short NUD_FAILED = 32;
    public static final short NUD_INCOMPLETE = 1;
    public static final short NUD_NOARP = 64;
    public static final short NUD_NONE = 0;
    public static final short NUD_PERMANENT = 128;
    public static final short NUD_PROBE = 16;
    public static final short NUD_REACHABLE = 2;
    public static final short NUD_STALE = 4;
    public static final int STRUCT_SIZE = 12;
    public byte ndm_family = (byte) OsConstants.AF_UNSPEC;
    public byte ndm_flags;
    public int ndm_ifindex;
    public short ndm_state;
    public byte ndm_type;
    public static byte NTF_USE = 1;
    public static byte NTF_SELF = 2;
    public static byte NTF_MASTER = 4;
    public static byte NTF_PROXY = 8;
    public static byte NTF_ROUTER = CommunicationUtil.PAD_ADDRESS;

    public static String stringForNudState(short nudState) {
        switch (nudState) {
            case 0:
                return "NUD_NONE";
            case 1:
                return "NUD_INCOMPLETE";
            case 2:
                return "NUD_REACHABLE";
            case 4:
                return "NUD_STALE";
            case 8:
                return "NUD_DELAY";
            case 16:
                return "NUD_PROBE";
            case 32:
                return "NUD_FAILED";
            case 64:
                return "NUD_NOARP";
            case 128:
                return "NUD_PERMANENT";
            default:
                return "unknown NUD state: " + String.valueOf((int) nudState);
        }
    }

    public static boolean isNudStateConnected(short nudState) {
        return (nudState & 194) != 0;
    }

    public static boolean isNudStateValid(short nudState) {
        return isNudStateConnected(nudState) || (nudState & 28) != 0;
    }

    private static String stringForNudFlags(byte flags) {
        StringBuilder sb = new StringBuilder();
        if ((NTF_USE & flags) != 0) {
            sb.append("NTF_USE");
        }
        if ((NTF_SELF & flags) != 0) {
            if (sb.length() > 0) {
                sb.append("|");
            }
            sb.append("NTF_SELF");
        }
        if ((NTF_MASTER & flags) != 0) {
            if (sb.length() > 0) {
                sb.append("|");
            }
            sb.append("NTF_MASTER");
        }
        if ((NTF_PROXY & flags) != 0) {
            if (sb.length() > 0) {
                sb.append("|");
            }
            sb.append("NTF_PROXY");
        }
        if ((NTF_ROUTER & flags) != 0) {
            if (sb.length() > 0) {
                sb.append("|");
            }
            sb.append("NTF_ROUTER");
        }
        return sb.toString();
    }

    private static boolean hasAvailableSpace(ByteBuffer byteBuffer) {
        return byteBuffer != null && byteBuffer.remaining() >= 12;
    }

    public static StructNdMsg parse(ByteBuffer byteBuffer) {
        if (!hasAvailableSpace(byteBuffer)) {
            return null;
        }
        StructNdMsg struct = new StructNdMsg();
        struct.ndm_family = byteBuffer.get();
        byteBuffer.get();
        byteBuffer.getShort();
        struct.ndm_ifindex = byteBuffer.getInt();
        struct.ndm_state = byteBuffer.getShort();
        struct.ndm_flags = byteBuffer.get();
        struct.ndm_type = byteBuffer.get();
        return struct;
    }

    public void pack(ByteBuffer byteBuffer) {
        byteBuffer.put(this.ndm_family);
        byteBuffer.put((byte) 0);
        byteBuffer.putShort((short) 0);
        byteBuffer.putInt(this.ndm_ifindex);
        byteBuffer.putShort(this.ndm_state);
        byteBuffer.put(this.ndm_flags);
        byteBuffer.put(this.ndm_type);
    }

    public boolean nudConnected() {
        return isNudStateConnected(this.ndm_state);
    }

    public boolean nudValid() {
        return isNudStateValid(this.ndm_state);
    }

    public String toString() {
        String stateStr = "" + ((int) this.ndm_state) + " (" + stringForNudState(this.ndm_state) + ")";
        String flagsStr = "" + ((int) this.ndm_flags) + " (" + stringForNudFlags(this.ndm_flags) + ")";
        return "StructNdMsg{ family{" + NetlinkConstants.stringForAddressFamily(this.ndm_family) + "}, ifindex{" + this.ndm_ifindex + "}, state{" + stateStr + "}, flags{" + flagsStr + "}, type{" + ((int) this.ndm_type) + "} }";
    }
}
