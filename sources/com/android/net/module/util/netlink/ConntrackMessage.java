package com.android.net.module.util.netlink;

import android.system.OsConstants;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;
/* loaded from: classes.dex */
public class ConntrackMessage extends NetlinkMessage {
    public static final short CTA_IP_V4_DST = 2;
    public static final short CTA_IP_V4_SRC = 1;
    public static final short CTA_PROTO_DST_PORT = 3;
    public static final short CTA_PROTO_NUM = 1;
    public static final short CTA_PROTO_SRC_PORT = 2;
    public static final short CTA_STATUS = 3;
    public static final short CTA_TIMEOUT = 7;
    public static final short CTA_TUPLE_IP = 1;
    public static final short CTA_TUPLE_ORIG = 1;
    public static final short CTA_TUPLE_PROTO = 2;
    public static final short CTA_TUPLE_REPLY = 2;
    public static final int DYING_MASK = 542;
    public static final int ESTABLISHED_MASK = 30;
    public static final int IPS_ASSURED = 4;
    public static final int IPS_CONFIRMED = 8;
    public static final int IPS_DST_NAT = 32;
    public static final int IPS_DST_NAT_DONE = 256;
    public static final int IPS_DYING = 512;
    public static final int IPS_EXPECTED = 1;
    public static final int IPS_FIXED_TIMEOUT = 1024;
    public static final int IPS_HELPER = 8192;
    public static final int IPS_HW_OFFLOAD = 32768;
    public static final int IPS_OFFLOAD = 16384;
    public static final int IPS_SEEN_REPLY = 2;
    public static final int IPS_SEQ_ADJUST = 64;
    public static final int IPS_SRC_NAT = 16;
    public static final int IPS_SRC_NAT_DONE = 128;
    public static final int IPS_TEMPLATE = 2048;
    public static final int IPS_UNTRACKED = 4096;
    public static final int STRUCT_SIZE = 20;
    public final StructNfGenMsg nfGenMsg;
    public final int status;
    public final int timeoutSec;
    public final Tuple tupleOrig;
    public final Tuple tupleReply;

    /* loaded from: classes.dex */
    public static class Tuple {
        public final Inet4Address dstIp;
        public final short dstPort;
        public final byte protoNum;
        public final Inet4Address srcIp;
        public final short srcPort;

        public Tuple(TupleIpv4 ip, TupleProto proto) {
            this.srcIp = ip.src;
            this.dstIp = ip.dst;
            this.srcPort = proto.srcPort;
            this.dstPort = proto.dstPort;
            this.protoNum = proto.protoNum;
        }

        public boolean equals(Object o) {
            if (!(o instanceof Tuple)) {
                return false;
            }
            Tuple that = (Tuple) o;
            return Objects.equals(this.srcIp, that.srcIp) && Objects.equals(this.dstIp, that.dstIp) && this.srcPort == that.srcPort && this.dstPort == that.dstPort && this.protoNum == that.protoNum;
        }

        public int hashCode() {
            return Objects.hash(this.srcIp, this.dstIp, Short.valueOf(this.srcPort), Short.valueOf(this.dstPort), Byte.valueOf(this.protoNum));
        }

        public String toString() {
            Inet4Address inet4Address = this.srcIp;
            String dstIpStr = "null";
            String srcIpStr = inet4Address == null ? dstIpStr : inet4Address.getHostAddress();
            Inet4Address inet4Address2 = this.dstIp;
            if (inet4Address2 != null) {
                dstIpStr = inet4Address2.getHostAddress();
            }
            String protoStr = NetlinkConstants.stringForProtocol(this.protoNum);
            return "Tuple{" + protoStr + ": " + srcIpStr + ":" + Short.toUnsignedInt(this.srcPort) + " -> " + dstIpStr + ":" + Short.toUnsignedInt(this.dstPort) + "}";
        }
    }

    /* loaded from: classes.dex */
    public static class TupleIpv4 {
        public final Inet4Address dst;
        public final Inet4Address src;

        public TupleIpv4(Inet4Address src, Inet4Address dst) {
            this.src = src;
            this.dst = dst;
        }
    }

    /* loaded from: classes.dex */
    public static class TupleProto {
        public final short dstPort;
        public final byte protoNum;
        public final short srcPort;

        public TupleProto(byte protoNum, short srcPort, short dstPort) {
            this.protoNum = protoNum;
            this.srcPort = srcPort;
            this.dstPort = dstPort;
        }
    }

    public static byte[] newIPv4TimeoutUpdateRequest(int proto, Inet4Address src, int sport, Inet4Address dst, int dport, int timeoutSec) {
        StructNlAttr ctaTupleOrig = new StructNlAttr((short) 1, new StructNlAttr((short) 1, new StructNlAttr((short) 1, (InetAddress) src), new StructNlAttr((short) 2, (InetAddress) dst)), new StructNlAttr((short) 2, new StructNlAttr((short) 1, (byte) proto), new StructNlAttr((short) 2, (short) sport, ByteOrder.BIG_ENDIAN), new StructNlAttr((short) 3, (short) dport, ByteOrder.BIG_ENDIAN)));
        StructNlAttr ctaTimeout = new StructNlAttr((short) 7, timeoutSec, ByteOrder.BIG_ENDIAN);
        int payloadLength = ctaTupleOrig.getAlignedLength() + ctaTimeout.getAlignedLength();
        byte[] bytes = new byte[payloadLength + 20];
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        byteBuffer.order(ByteOrder.nativeOrder());
        ConntrackMessage ctmsg = new ConntrackMessage();
        ctmsg.mHeader.nlmsg_len = bytes.length;
        ctmsg.mHeader.nlmsg_type = (short) 256;
        ctmsg.mHeader.nlmsg_flags = (short) 261;
        ctmsg.mHeader.nlmsg_seq = 1;
        ctmsg.pack(byteBuffer);
        ctaTupleOrig.pack(byteBuffer);
        ctaTimeout.pack(byteBuffer);
        return bytes;
    }

    public static ConntrackMessage parse(StructNlMsgHdr header, ByteBuffer byteBuffer) {
        int status;
        int timeoutSec;
        Tuple tupleOrig;
        Tuple tupleReply;
        StructNfGenMsg nfGenMsg = StructNfGenMsg.parse(byteBuffer);
        if (nfGenMsg == null) {
            return null;
        }
        int baseOffset = byteBuffer.position();
        StructNlAttr nlAttr = StructNlAttr.findNextAttrOfType((short) 3, byteBuffer);
        if (nlAttr == null) {
            status = 0;
        } else {
            int status2 = nlAttr.getValueAsBe32(0);
            status = status2;
        }
        byteBuffer.position(baseOffset);
        StructNlAttr nlAttr2 = StructNlAttr.findNextAttrOfType((short) 7, byteBuffer);
        if (nlAttr2 == null) {
            timeoutSec = 0;
        } else {
            int timeoutSec2 = nlAttr2.getValueAsBe32(0);
            timeoutSec = timeoutSec2;
        }
        byteBuffer.position(baseOffset);
        StructNlAttr nlAttr3 = StructNlAttr.findNextAttrOfType(StructNlAttr.makeNestedType((short) 1), byteBuffer);
        if (nlAttr3 == null) {
            tupleOrig = null;
        } else {
            Tuple tupleOrig2 = parseTuple(nlAttr3.getValueAsByteBuffer());
            tupleOrig = tupleOrig2;
        }
        byteBuffer.position(baseOffset);
        StructNlAttr nlAttr4 = StructNlAttr.findNextAttrOfType(StructNlAttr.makeNestedType((short) 2), byteBuffer);
        if (nlAttr4 == null) {
            tupleReply = null;
        } else {
            Tuple tupleReply2 = parseTuple(nlAttr4.getValueAsByteBuffer());
            tupleReply = tupleReply2;
        }
        byteBuffer.position(baseOffset);
        int kAdditionalSpace = NetlinkConstants.alignedLengthOf(header.nlmsg_len - 20);
        if (byteBuffer.remaining() >= kAdditionalSpace) {
            byteBuffer.position(baseOffset + kAdditionalSpace);
            return new ConntrackMessage(header, nfGenMsg, tupleOrig, tupleReply, status, timeoutSec);
        }
        return null;
    }

    private static Tuple parseTuple(ByteBuffer byteBuffer) {
        if (byteBuffer == null) {
            return null;
        }
        TupleIpv4 tupleIpv4 = null;
        TupleProto tupleProto = null;
        int baseOffset = byteBuffer.position();
        StructNlAttr nlAttr = StructNlAttr.findNextAttrOfType(StructNlAttr.makeNestedType((short) 1), byteBuffer);
        if (nlAttr != null) {
            tupleIpv4 = parseTupleIpv4(nlAttr.getValueAsByteBuffer());
        }
        if (tupleIpv4 == null) {
            return null;
        }
        byteBuffer.position(baseOffset);
        StructNlAttr nlAttr2 = StructNlAttr.findNextAttrOfType(StructNlAttr.makeNestedType((short) 2), byteBuffer);
        if (nlAttr2 != null) {
            tupleProto = parseTupleProto(nlAttr2.getValueAsByteBuffer());
        }
        if (tupleProto == null) {
            return null;
        }
        return new Tuple(tupleIpv4, tupleProto);
    }

    private static Inet4Address castToInet4Address(InetAddress address) {
        if (address == null || !(address instanceof Inet4Address)) {
            return null;
        }
        return (Inet4Address) address;
    }

    private static TupleIpv4 parseTupleIpv4(ByteBuffer byteBuffer) {
        if (byteBuffer == null) {
            return null;
        }
        Inet4Address src = null;
        Inet4Address dst = null;
        int baseOffset = byteBuffer.position();
        StructNlAttr nlAttr = StructNlAttr.findNextAttrOfType((short) 1, byteBuffer);
        if (nlAttr != null) {
            src = castToInet4Address(nlAttr.getValueAsInetAddress());
        }
        if (src == null) {
            return null;
        }
        byteBuffer.position(baseOffset);
        StructNlAttr nlAttr2 = StructNlAttr.findNextAttrOfType((short) 2, byteBuffer);
        if (nlAttr2 != null) {
            dst = castToInet4Address(nlAttr2.getValueAsInetAddress());
        }
        if (dst == null) {
            return null;
        }
        return new TupleIpv4(src, dst);
    }

    private static TupleProto parseTupleProto(ByteBuffer byteBuffer) {
        if (byteBuffer == null) {
            return null;
        }
        byte protoNum = 0;
        short srcPort = 0;
        short dstPort = 0;
        int baseOffset = byteBuffer.position();
        StructNlAttr nlAttr = StructNlAttr.findNextAttrOfType((short) 1, byteBuffer);
        if (nlAttr != null) {
            protoNum = nlAttr.getValueAsByte((byte) 0);
        }
        if (protoNum != OsConstants.IPPROTO_TCP && protoNum != OsConstants.IPPROTO_UDP) {
            return null;
        }
        byteBuffer.position(baseOffset);
        StructNlAttr nlAttr2 = StructNlAttr.findNextAttrOfType((short) 2, byteBuffer);
        if (nlAttr2 != null) {
            srcPort = nlAttr2.getValueAsBe16((short) 0);
        }
        if (srcPort == 0) {
            return null;
        }
        byteBuffer.position(baseOffset);
        StructNlAttr nlAttr3 = StructNlAttr.findNextAttrOfType((short) 3, byteBuffer);
        if (nlAttr3 != null) {
            dstPort = nlAttr3.getValueAsBe16((short) 0);
        }
        if (dstPort == 0) {
            return null;
        }
        return new TupleProto(protoNum, srcPort, dstPort);
    }

    private ConntrackMessage() {
        super(new StructNlMsgHdr());
        this.nfGenMsg = new StructNfGenMsg((byte) OsConstants.AF_INET);
        this.tupleOrig = null;
        this.tupleReply = null;
        this.status = 0;
        this.timeoutSec = 0;
    }

    private ConntrackMessage(StructNlMsgHdr header, StructNfGenMsg nfGenMsg, Tuple tupleOrig, Tuple tupleReply, int status, int timeoutSec) {
        super(header);
        this.nfGenMsg = nfGenMsg;
        this.tupleOrig = tupleOrig;
        this.tupleReply = tupleReply;
        this.status = status;
        this.timeoutSec = timeoutSec;
    }

    public void pack(ByteBuffer byteBuffer) {
        this.mHeader.pack(byteBuffer);
        this.nfGenMsg.pack(byteBuffer);
    }

    public short getMessageType() {
        return (short) (getHeader().nlmsg_type & (-257));
    }

    public static String stringForIpConntrackStatus(int flags) {
        StringBuilder sb = new StringBuilder();
        if ((flags & 1) != 0) {
            sb.append("IPS_EXPECTED");
        }
        if ((flags & 2) != 0) {
            if (sb.length() > 0) {
                sb.append("|");
            }
            sb.append("IPS_SEEN_REPLY");
        }
        if ((flags & 4) != 0) {
            if (sb.length() > 0) {
                sb.append("|");
            }
            sb.append("IPS_ASSURED");
        }
        if ((flags & 8) != 0) {
            if (sb.length() > 0) {
                sb.append("|");
            }
            sb.append("IPS_CONFIRMED");
        }
        if ((flags & 16) != 0) {
            if (sb.length() > 0) {
                sb.append("|");
            }
            sb.append("IPS_SRC_NAT");
        }
        if ((flags & 32) != 0) {
            if (sb.length() > 0) {
                sb.append("|");
            }
            sb.append("IPS_DST_NAT");
        }
        if ((flags & 64) != 0) {
            if (sb.length() > 0) {
                sb.append("|");
            }
            sb.append("IPS_SEQ_ADJUST");
        }
        if ((flags & 128) != 0) {
            if (sb.length() > 0) {
                sb.append("|");
            }
            sb.append("IPS_SRC_NAT_DONE");
        }
        if ((flags & 256) != 0) {
            if (sb.length() > 0) {
                sb.append("|");
            }
            sb.append("IPS_DST_NAT_DONE");
        }
        if ((flags & 512) != 0) {
            if (sb.length() > 0) {
                sb.append("|");
            }
            sb.append("IPS_DYING");
        }
        if ((flags & 1024) != 0) {
            if (sb.length() > 0) {
                sb.append("|");
            }
            sb.append("IPS_FIXED_TIMEOUT");
        }
        if ((flags & 2048) != 0) {
            if (sb.length() > 0) {
                sb.append("|");
            }
            sb.append("IPS_TEMPLATE");
        }
        if ((flags & 4096) != 0) {
            if (sb.length() > 0) {
                sb.append("|");
            }
            sb.append("IPS_UNTRACKED");
        }
        if ((flags & 8192) != 0) {
            if (sb.length() > 0) {
                sb.append("|");
            }
            sb.append("IPS_HELPER");
        }
        if ((flags & 16384) != 0) {
            if (sb.length() > 0) {
                sb.append("|");
            }
            sb.append("IPS_OFFLOAD");
        }
        if ((32768 & flags) != 0) {
            if (sb.length() > 0) {
                sb.append("|");
            }
            sb.append("IPS_HW_OFFLOAD");
        }
        return sb.toString();
    }

    @Override // com.android.net.module.util.netlink.NetlinkMessage
    public String toString() {
        return "ConntrackMessage{nlmsghdr{" + (this.mHeader == null ? "" : this.mHeader.toString(Integer.valueOf(OsConstants.NETLINK_NETFILTER))) + "}, nfgenmsg{" + this.nfGenMsg + "}, tuple_orig{" + this.tupleOrig + "}, tuple_reply{" + this.tupleReply + "}, status{" + this.status + "(" + stringForIpConntrackStatus(this.status) + ")}, timeout_sec{" + Integer.toUnsignedLong(this.timeoutSec) + "}}";
    }
}
