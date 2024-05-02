package com.android.net.module.util.netlink;

import android.system.OsConstants;
import com.android.server.padkeyboard.iic.CommunicationUtil;
import java.nio.ByteBuffer;
/* loaded from: classes.dex */
public class NetlinkMessage {
    private static final String TAG = "NetlinkMessage";
    protected final StructNlMsgHdr mHeader;

    public static NetlinkMessage parse(ByteBuffer byteBuffer, int nlFamily) {
        NetlinkMessage parsed;
        int startPosition = byteBuffer != null ? byteBuffer.position() : -1;
        StructNlMsgHdr nlmsghdr = StructNlMsgHdr.parse(byteBuffer);
        if (nlmsghdr == null) {
            return null;
        }
        int messageLength = NetlinkConstants.alignedLengthOf(nlmsghdr.nlmsg_len);
        int payloadLength = messageLength - 16;
        if (payloadLength < 0 || payloadLength > byteBuffer.remaining()) {
            byteBuffer.position(byteBuffer.limit());
            return null;
        } else if (nlmsghdr.nlmsg_type <= 15) {
            return parseCtlMessage(nlmsghdr, byteBuffer, payloadLength);
        } else {
            if (nlFamily == OsConstants.NETLINK_ROUTE) {
                parsed = parseRtMessage(nlmsghdr, byteBuffer);
            } else if (nlFamily == OsConstants.NETLINK_INET_DIAG) {
                parsed = parseInetDiagMessage(nlmsghdr, byteBuffer);
            } else if (nlFamily == OsConstants.NETLINK_NETFILTER) {
                parsed = parseNfMessage(nlmsghdr, byteBuffer);
            } else {
                parsed = null;
            }
            byteBuffer.position(startPosition + messageLength);
            return parsed;
        }
    }

    public NetlinkMessage(StructNlMsgHdr nlmsghdr) {
        this.mHeader = nlmsghdr;
    }

    public StructNlMsgHdr getHeader() {
        return this.mHeader;
    }

    public String toString() {
        return "NetlinkMessage{" + this.mHeader.toString() + "}";
    }

    private static NetlinkMessage parseCtlMessage(StructNlMsgHdr nlmsghdr, ByteBuffer byteBuffer, int payloadLength) {
        switch (nlmsghdr.nlmsg_type) {
            case 2:
                return NetlinkErrorMessage.parse(nlmsghdr, byteBuffer);
            default:
                byteBuffer.position(byteBuffer.position() + payloadLength);
                return new NetlinkMessage(nlmsghdr);
        }
    }

    private static NetlinkMessage parseRtMessage(StructNlMsgHdr nlmsghdr, ByteBuffer byteBuffer) {
        switch (nlmsghdr.nlmsg_type) {
            case 16:
            case 17:
                return RtNetlinkLinkMessage.parse(nlmsghdr, byteBuffer);
            case 20:
            case 21:
                return RtNetlinkAddressMessage.parse(nlmsghdr, byteBuffer);
            case 24:
            case 25:
                return RtNetlinkRouteMessage.parse(nlmsghdr, byteBuffer);
            case 28:
            case 29:
            case 30:
                return RtNetlinkNeighborMessage.parse(nlmsghdr, byteBuffer);
            case CommunicationUtil.SEND_COMMAND_BYTE_LONG /* 68 */:
                return NduseroptMessage.parse(nlmsghdr, byteBuffer);
            default:
                return null;
        }
    }

    private static NetlinkMessage parseInetDiagMessage(StructNlMsgHdr nlmsghdr, ByteBuffer byteBuffer) {
        switch (nlmsghdr.nlmsg_type) {
            case 20:
                return InetDiagMessage.parse(nlmsghdr, byteBuffer);
            default:
                return null;
        }
    }

    private static NetlinkMessage parseNfMessage(StructNlMsgHdr nlmsghdr, ByteBuffer byteBuffer) {
        switch (nlmsghdr.nlmsg_type) {
            case 256:
            case 258:
                return ConntrackMessage.parse(nlmsghdr, byteBuffer);
            case 257:
            default:
                return null;
        }
    }
}
