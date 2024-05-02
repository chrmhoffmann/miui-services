package com.android.net.module.util.netlink;

import android.system.OsConstants;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
/* loaded from: classes.dex */
public class RtNetlinkNeighborMessage extends NetlinkMessage {
    public static final short NDA_CACHEINFO = 3;
    public static final short NDA_DST = 1;
    public static final short NDA_IFINDEX = 8;
    public static final short NDA_LLADDR = 2;
    public static final short NDA_MASTER = 9;
    public static final short NDA_PORT = 6;
    public static final short NDA_PROBES = 4;
    public static final short NDA_UNSPEC = 0;
    public static final short NDA_VLAN = 5;
    public static final short NDA_VNI = 7;
    private StructNdMsg mNdmsg = null;
    private InetAddress mDestination = null;
    private byte[] mLinkLayerAddr = null;
    private int mNumProbes = 0;
    private StructNdaCacheInfo mCacheInfo = null;

    public static RtNetlinkNeighborMessage parse(StructNlMsgHdr header, ByteBuffer byteBuffer) {
        RtNetlinkNeighborMessage neighMsg = new RtNetlinkNeighborMessage(header);
        StructNdMsg parse = StructNdMsg.parse(byteBuffer);
        neighMsg.mNdmsg = parse;
        if (parse == null) {
            return null;
        }
        int baseOffset = byteBuffer.position();
        StructNlAttr nlAttr = StructNlAttr.findNextAttrOfType((short) 1, byteBuffer);
        if (nlAttr != null) {
            neighMsg.mDestination = nlAttr.getValueAsInetAddress();
        }
        byteBuffer.position(baseOffset);
        StructNlAttr nlAttr2 = StructNlAttr.findNextAttrOfType((short) 2, byteBuffer);
        if (nlAttr2 != null) {
            neighMsg.mLinkLayerAddr = nlAttr2.nla_value;
        }
        byteBuffer.position(baseOffset);
        StructNlAttr nlAttr3 = StructNlAttr.findNextAttrOfType((short) 4, byteBuffer);
        if (nlAttr3 != null) {
            neighMsg.mNumProbes = nlAttr3.getValueAsInt(0);
        }
        byteBuffer.position(baseOffset);
        StructNlAttr nlAttr4 = StructNlAttr.findNextAttrOfType((short) 3, byteBuffer);
        if (nlAttr4 != null) {
            neighMsg.mCacheInfo = StructNdaCacheInfo.parse(nlAttr4.getValueAsByteBuffer());
        }
        int kAdditionalSpace = NetlinkConstants.alignedLengthOf(neighMsg.mHeader.nlmsg_len - 28);
        if (byteBuffer.remaining() < kAdditionalSpace) {
            byteBuffer.position(byteBuffer.limit());
        } else {
            byteBuffer.position(baseOffset + kAdditionalSpace);
        }
        return neighMsg;
    }

    public static byte[] newGetNeighborsRequest(int seqNo) {
        byte[] bytes = new byte[28];
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        byteBuffer.order(ByteOrder.nativeOrder());
        StructNlMsgHdr nlmsghdr = new StructNlMsgHdr();
        nlmsghdr.nlmsg_len = 28;
        nlmsghdr.nlmsg_type = (short) 30;
        nlmsghdr.nlmsg_flags = (short) 769;
        nlmsghdr.nlmsg_seq = seqNo;
        nlmsghdr.pack(byteBuffer);
        StructNdMsg ndmsg = new StructNdMsg();
        ndmsg.pack(byteBuffer);
        return bytes;
    }

    public static byte[] newNewNeighborMessage(int seqNo, InetAddress ip, short nudState, int ifIndex, byte[] llAddr) {
        StructNlMsgHdr nlmsghdr = new StructNlMsgHdr();
        nlmsghdr.nlmsg_type = (short) 28;
        nlmsghdr.nlmsg_flags = (short) 261;
        nlmsghdr.nlmsg_seq = seqNo;
        RtNetlinkNeighborMessage msg = new RtNetlinkNeighborMessage(nlmsghdr);
        StructNdMsg structNdMsg = new StructNdMsg();
        msg.mNdmsg = structNdMsg;
        structNdMsg.ndm_family = (byte) (ip instanceof Inet6Address ? OsConstants.AF_INET6 : OsConstants.AF_INET);
        msg.mNdmsg.ndm_ifindex = ifIndex;
        msg.mNdmsg.ndm_state = nudState;
        msg.mDestination = ip;
        msg.mLinkLayerAddr = llAddr;
        byte[] bytes = new byte[msg.getRequiredSpace()];
        nlmsghdr.nlmsg_len = bytes.length;
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        byteBuffer.order(ByteOrder.nativeOrder());
        msg.pack(byteBuffer);
        return bytes;
    }

    private RtNetlinkNeighborMessage(StructNlMsgHdr header) {
        super(header);
    }

    public StructNdMsg getNdHeader() {
        return this.mNdmsg;
    }

    public InetAddress getDestination() {
        return this.mDestination;
    }

    public byte[] getLinkLayerAddress() {
        return this.mLinkLayerAddr;
    }

    public int getProbes() {
        return this.mNumProbes;
    }

    public StructNdaCacheInfo getCacheInfo() {
        return this.mCacheInfo;
    }

    private int getRequiredSpace() {
        int spaceRequired = 28;
        InetAddress inetAddress = this.mDestination;
        if (inetAddress != null) {
            spaceRequired = 28 + NetlinkConstants.alignedLengthOf(inetAddress.getAddress().length + 4);
        }
        byte[] bArr = this.mLinkLayerAddr;
        if (bArr != null) {
            return spaceRequired + NetlinkConstants.alignedLengthOf(bArr.length + 4);
        }
        return spaceRequired;
    }

    private static void packNlAttr(short nlType, byte[] nlValue, ByteBuffer byteBuffer) {
        StructNlAttr nlAttr = new StructNlAttr();
        nlAttr.nla_type = nlType;
        nlAttr.nla_value = nlValue;
        nlAttr.nla_len = (short) (nlAttr.nla_value.length + 4);
        nlAttr.pack(byteBuffer);
    }

    public void pack(ByteBuffer byteBuffer) {
        getHeader().pack(byteBuffer);
        this.mNdmsg.pack(byteBuffer);
        InetAddress inetAddress = this.mDestination;
        if (inetAddress != null) {
            packNlAttr((short) 1, inetAddress.getAddress(), byteBuffer);
        }
        byte[] bArr = this.mLinkLayerAddr;
        if (bArr != null) {
            packNlAttr((short) 2, bArr, byteBuffer);
        }
    }

    @Override // com.android.net.module.util.netlink.NetlinkMessage
    public String toString() {
        InetAddress inetAddress = this.mDestination;
        String str = "";
        String ipLiteral = inetAddress == null ? str : inetAddress.getHostAddress();
        StringBuilder append = new StringBuilder().append("RtNetlinkNeighborMessage{ nlmsghdr{").append(this.mHeader == null ? str : this.mHeader.toString(Integer.valueOf(OsConstants.NETLINK_ROUTE))).append("}, ndmsg{");
        StructNdMsg structNdMsg = this.mNdmsg;
        StringBuilder append2 = append.append(structNdMsg == null ? str : structNdMsg.toString()).append("}, destination{").append(ipLiteral).append("} linklayeraddr{").append(NetlinkConstants.hexify(this.mLinkLayerAddr)).append("} probes{").append(this.mNumProbes).append("} cacheinfo{");
        StructNdaCacheInfo structNdaCacheInfo = this.mCacheInfo;
        if (structNdaCacheInfo != null) {
            str = structNdaCacheInfo.toString();
        }
        return append2.append(str).append("} }").toString();
    }
}
