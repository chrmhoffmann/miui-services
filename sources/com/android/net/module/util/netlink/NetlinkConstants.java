package com.android.net.module.util.netlink;

import android.system.OsConstants;
import com.android.server.padkeyboard.iic.CommunicationUtil;
import java.nio.ByteBuffer;
/* loaded from: classes.dex */
public class NetlinkConstants {
    private static final char[] HEX_DIGITS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
    public static final int IFF_LOWER_UP = 65536;
    public static final int IFF_UP = 1;
    public static final int INET_DIAG_MEMINFO = 1;
    public static final short IPCTNL_MSG_CT_DELETE = 2;
    public static final short IPCTNL_MSG_CT_GET = 1;
    public static final short IPCTNL_MSG_CT_GET_CTRZERO = 3;
    public static final short IPCTNL_MSG_CT_GET_DYING = 6;
    public static final short IPCTNL_MSG_CT_GET_STATS = 5;
    public static final short IPCTNL_MSG_CT_GET_STATS_CPU = 4;
    public static final short IPCTNL_MSG_CT_GET_UNCONFIRMED = 7;
    public static final short IPCTNL_MSG_CT_NEW = 0;
    public static final short NFNL_SUBSYS_CTNETLINK = 1;
    public static final int NLA_ALIGNTO = 4;
    public static final short NLMSG_DONE = 3;
    public static final short NLMSG_ERROR = 2;
    public static final short NLMSG_MAX_RESERVED = 15;
    public static final short NLMSG_NOOP = 1;
    public static final short NLMSG_OVERRUN = 4;
    public static final int RTMGRP_IPV4_IFADDR = 16;
    public static final int RTMGRP_IPV6_IFADDR = 256;
    public static final int RTMGRP_IPV6_ROUTE = 1024;
    public static final int RTMGRP_LINK = 1;
    public static final int RTMGRP_ND_USEROPT = 524288;
    public static final short RTM_DELADDR = 21;
    public static final short RTM_DELLINK = 17;
    public static final short RTM_DELNEIGH = 29;
    public static final short RTM_DELROUTE = 25;
    public static final short RTM_DELRULE = 33;
    public static final int RTM_F_CLONED = 512;
    public static final short RTM_GETADDR = 22;
    public static final short RTM_GETLINK = 18;
    public static final short RTM_GETNEIGH = 30;
    public static final short RTM_GETROUTE = 26;
    public static final short RTM_GETRULE = 34;
    public static final short RTM_NEWADDR = 20;
    public static final short RTM_NEWLINK = 16;
    public static final short RTM_NEWNDUSEROPT = 68;
    public static final short RTM_NEWNEIGH = 28;
    public static final short RTM_NEWROUTE = 24;
    public static final short RTM_NEWRULE = 32;
    public static final short RTM_SETLINK = 19;
    public static final int RTNLGRP_ND_USEROPT = 20;
    public static final short RTN_UNICAST = 1;
    public static final short RTPROT_KERNEL = 2;
    public static final short RTPROT_RA = 9;
    public static final short RT_SCOPE_UNIVERSE = 0;
    public static final int SOCKDIAG_MSG_HEADER_SIZE = 88;
    public static final short SOCK_DIAG_BY_FAMILY = 20;

    private NetlinkConstants() {
    }

    public static final int alignedLengthOf(short length) {
        int intLength = 65535 & length;
        return alignedLengthOf(intLength);
    }

    public static final int alignedLengthOf(int length) {
        if (length <= 0) {
            return 0;
        }
        return (((length + 4) - 1) / 4) * 4;
    }

    public static String stringForAddressFamily(int family) {
        return family == OsConstants.AF_INET ? "AF_INET" : family == OsConstants.AF_INET6 ? "AF_INET6" : family == OsConstants.AF_NETLINK ? "AF_NETLINK" : family == OsConstants.AF_UNSPEC ? "AF_UNSPEC" : String.valueOf(family);
    }

    public static String stringForProtocol(int protocol) {
        return protocol == OsConstants.IPPROTO_TCP ? "IPPROTO_TCP" : protocol == OsConstants.IPPROTO_UDP ? "IPPROTO_UDP" : String.valueOf(protocol);
    }

    public static String hexify(byte[] bytes) {
        return bytes == null ? "(null)" : toHexString(bytes, 0, bytes.length);
    }

    public static String hexify(ByteBuffer buffer) {
        return buffer == null ? "(null)" : toHexString(buffer.array(), buffer.position(), buffer.remaining());
    }

    private static String stringForCtlMsgType(short nlmType) {
        switch (nlmType) {
            case 1:
                return "NLMSG_NOOP";
            case 2:
                return "NLMSG_ERROR";
            case 3:
                return "NLMSG_DONE";
            case 4:
                return "NLMSG_OVERRUN";
            default:
                return "unknown control message type: " + String.valueOf((int) nlmType);
        }
    }

    private static String stringForRtMsgType(short nlmType) {
        switch (nlmType) {
            case 16:
                return "RTM_NEWLINK";
            case 17:
                return "RTM_DELLINK";
            case 18:
                return "RTM_GETLINK";
            case 19:
                return "RTM_SETLINK";
            case 20:
                return "RTM_NEWADDR";
            case 21:
                return "RTM_DELADDR";
            case 22:
                return "RTM_GETADDR";
            case 24:
                return "RTM_NEWROUTE";
            case 25:
                return "RTM_DELROUTE";
            case 26:
                return "RTM_GETROUTE";
            case 28:
                return "RTM_NEWNEIGH";
            case 29:
                return "RTM_DELNEIGH";
            case 30:
                return "RTM_GETNEIGH";
            case 32:
                return "RTM_NEWRULE";
            case 33:
                return "RTM_DELRULE";
            case 34:
                return "RTM_GETRULE";
            case CommunicationUtil.SEND_COMMAND_BYTE_LONG /* 68 */:
                return "RTM_NEWNDUSEROPT";
            default:
                return "unknown RTM type: " + String.valueOf((int) nlmType);
        }
    }

    private static String stringForInetDiagMsgType(short nlmType) {
        switch (nlmType) {
            case 20:
                return "SOCK_DIAG_BY_FAMILY";
            default:
                return "unknown SOCK_DIAG type: " + String.valueOf((int) nlmType);
        }
    }

    private static String stringForNfMsgType(short nlmType) {
        byte subsysId = (byte) (nlmType >> 8);
        byte msgType = (byte) nlmType;
        switch (subsysId) {
            case 1:
                switch (msgType) {
                    case 0:
                        return "IPCTNL_MSG_CT_NEW";
                    case 1:
                        return "IPCTNL_MSG_CT_GET";
                    case 2:
                        return "IPCTNL_MSG_CT_DELETE";
                    case 3:
                        return "IPCTNL_MSG_CT_GET_CTRZERO";
                    case 4:
                        return "IPCTNL_MSG_CT_GET_STATS_CPU";
                    case 5:
                        return "IPCTNL_MSG_CT_GET_STATS";
                    case 6:
                        return "IPCTNL_MSG_CT_GET_DYING";
                    case 7:
                        return "IPCTNL_MSG_CT_GET_UNCONFIRMED";
                }
        }
        return "unknown NETFILTER type: " + String.valueOf((int) nlmType);
    }

    public static String stringForNlMsgType(short nlmType, int nlFamily) {
        return nlmType <= 15 ? stringForCtlMsgType(nlmType) : nlFamily == OsConstants.NETLINK_ROUTE ? stringForRtMsgType(nlmType) : nlFamily == OsConstants.NETLINK_INET_DIAG ? stringForInetDiagMsgType(nlmType) : nlFamily == OsConstants.NETLINK_NETFILTER ? stringForNfMsgType(nlmType) : "unknown type: " + String.valueOf((int) nlmType);
    }

    public static String toHexString(byte[] array, int offset, int length) {
        char[] buf = new char[length * 2];
        int bufIndex = 0;
        for (int i = offset; i < offset + length; i++) {
            byte b = array[i];
            int bufIndex2 = bufIndex + 1;
            char[] cArr = HEX_DIGITS;
            buf[bufIndex] = cArr[(b >>> 4) & 15];
            bufIndex = bufIndex2 + 1;
            buf[bufIndex2] = cArr[b & 15];
        }
        return new String(buf);
    }
}
