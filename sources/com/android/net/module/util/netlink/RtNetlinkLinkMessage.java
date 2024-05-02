package com.android.net.module.util.netlink;

import android.net.MacAddress;
import android.system.OsConstants;
import java.nio.ByteBuffer;
/* loaded from: classes.dex */
public class RtNetlinkLinkMessage extends NetlinkMessage {
    public static final short IFLA_ADDRESS = 1;
    public static final short IFLA_IFNAME = 3;
    public static final short IFLA_MTU = 4;
    private StructIfinfoMsg mIfinfomsg = null;
    private int mMtu = 0;
    private MacAddress mHardwareAddress = null;
    private String mInterfaceName = null;

    private RtNetlinkLinkMessage(StructNlMsgHdr header) {
        super(header);
    }

    public int getMtu() {
        return this.mMtu;
    }

    public StructIfinfoMsg getIfinfoHeader() {
        return this.mIfinfomsg;
    }

    public MacAddress getHardwareAddress() {
        return this.mHardwareAddress;
    }

    public String getInterfaceName() {
        return this.mInterfaceName;
    }

    public static RtNetlinkLinkMessage parse(StructNlMsgHdr header, ByteBuffer byteBuffer) {
        RtNetlinkLinkMessage linkMsg = new RtNetlinkLinkMessage(header);
        StructIfinfoMsg parse = StructIfinfoMsg.parse(byteBuffer);
        linkMsg.mIfinfomsg = parse;
        if (parse == null) {
            return null;
        }
        int baseOffset = byteBuffer.position();
        StructNlAttr nlAttr = StructNlAttr.findNextAttrOfType((short) 4, byteBuffer);
        if (nlAttr != null) {
            linkMsg.mMtu = nlAttr.getValueAsInt(0);
        }
        byteBuffer.position(baseOffset);
        StructNlAttr nlAttr2 = StructNlAttr.findNextAttrOfType((short) 1, byteBuffer);
        if (nlAttr2 != null) {
            linkMsg.mHardwareAddress = nlAttr2.getValueAsMacAddress();
        }
        byteBuffer.position(baseOffset);
        StructNlAttr nlAttr3 = StructNlAttr.findNextAttrOfType((short) 3, byteBuffer);
        if (nlAttr3 != null) {
            linkMsg.mInterfaceName = nlAttr3.getValueAsString();
        }
        return linkMsg;
    }

    protected void pack(ByteBuffer byteBuffer) {
        getHeader().pack(byteBuffer);
        this.mIfinfomsg.pack(byteBuffer);
        int i = this.mMtu;
        if (i != 0) {
            StructNlAttr mtu = new StructNlAttr((short) 4, i);
            mtu.pack(byteBuffer);
        }
        MacAddress macAddress = this.mHardwareAddress;
        if (macAddress != null) {
            StructNlAttr hardwareAddress = new StructNlAttr((short) 1, macAddress);
            hardwareAddress.pack(byteBuffer);
        }
        String str = this.mInterfaceName;
        if (str != null) {
            StructNlAttr ifname = new StructNlAttr((short) 3, str);
            ifname.pack(byteBuffer);
        }
    }

    @Override // com.android.net.module.util.netlink.NetlinkMessage
    public String toString() {
        return "RtNetlinkLinkMessage{ nlmsghdr{" + this.mHeader.toString(Integer.valueOf(OsConstants.NETLINK_ROUTE)) + "}, Ifinfomsg{" + this.mIfinfomsg.toString() + "}, Hardware Address{" + this.mHardwareAddress + "}, MTU{" + this.mMtu + "}, Ifname{" + this.mInterfaceName + "} }";
    }
}
