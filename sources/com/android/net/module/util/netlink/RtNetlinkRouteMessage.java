package com.android.net.module.util.netlink;

import android.net.IpPrefix;
import android.system.OsConstants;
import com.android.net.module.util.NetworkStackConstants;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.nio.ByteBuffer;
/* loaded from: classes.dex */
public class RtNetlinkRouteMessage extends NetlinkMessage {
    public static final short RTA_DST = 1;
    public static final short RTA_GATEWAY = 5;
    public static final short RTA_OIF = 4;
    private StructRtMsg mRtmsg = null;
    private IpPrefix mDestination = null;
    private InetAddress mGateway = null;
    private int mIfindex = 0;

    private RtNetlinkRouteMessage(StructNlMsgHdr header) {
        super(header);
    }

    public int getInterfaceIndex() {
        return this.mIfindex;
    }

    public StructRtMsg getRtMsgHeader() {
        return this.mRtmsg;
    }

    public IpPrefix getDestination() {
        return this.mDestination;
    }

    public InetAddress getGateway() {
        return this.mGateway;
    }

    private static boolean matchRouteAddressFamily(InetAddress address, int family) {
        return ((address instanceof Inet4Address) && family == OsConstants.AF_INET) || ((address instanceof Inet6Address) && family == OsConstants.AF_INET6);
    }

    public static RtNetlinkRouteMessage parse(StructNlMsgHdr header, ByteBuffer byteBuffer) {
        RtNetlinkRouteMessage routeMsg = new RtNetlinkRouteMessage(header);
        StructRtMsg parse = StructRtMsg.parse(byteBuffer);
        routeMsg.mRtmsg = parse;
        if (parse == null) {
            return null;
        }
        int rtmFamily = parse.family;
        int baseOffset = byteBuffer.position();
        StructNlAttr nlAttr = StructNlAttr.findNextAttrOfType((short) 1, byteBuffer);
        if (nlAttr != null) {
            InetAddress destination = nlAttr.getValueAsInetAddress();
            if (destination == null || !matchRouteAddressFamily(destination, rtmFamily)) {
                return null;
            }
            routeMsg.mDestination = new IpPrefix(destination, routeMsg.mRtmsg.dstLen);
        } else if (rtmFamily == OsConstants.AF_INET) {
            routeMsg.mDestination = new IpPrefix(NetworkStackConstants.IPV4_ADDR_ANY, 0);
        } else if (rtmFamily != OsConstants.AF_INET6) {
            return null;
        } else {
            routeMsg.mDestination = new IpPrefix(NetworkStackConstants.IPV6_ADDR_ANY, 0);
        }
        byteBuffer.position(baseOffset);
        StructNlAttr nlAttr2 = StructNlAttr.findNextAttrOfType((short) 5, byteBuffer);
        if (nlAttr2 != null) {
            InetAddress valueAsInetAddress = nlAttr2.getValueAsInetAddress();
            routeMsg.mGateway = valueAsInetAddress;
            if (valueAsInetAddress == null || !matchRouteAddressFamily(valueAsInetAddress, rtmFamily)) {
                return null;
            }
        }
        byteBuffer.position(baseOffset);
        StructNlAttr nlAttr3 = StructNlAttr.findNextAttrOfType((short) 4, byteBuffer);
        if (nlAttr3 != null) {
            routeMsg.mIfindex = nlAttr3.getValueAsInt(0);
        }
        return routeMsg;
    }

    protected void pack(ByteBuffer byteBuffer) {
        getHeader().pack(byteBuffer);
        this.mRtmsg.pack(byteBuffer);
        StructNlAttr destination = new StructNlAttr((short) 1, this.mDestination.getAddress());
        destination.pack(byteBuffer);
        InetAddress inetAddress = this.mGateway;
        if (inetAddress != null) {
            StructNlAttr gateway = new StructNlAttr((short) 5, inetAddress.getAddress());
            gateway.pack(byteBuffer);
        }
        int i = this.mIfindex;
        if (i != 0) {
            StructNlAttr ifindex = new StructNlAttr((short) 4, i);
            ifindex.pack(byteBuffer);
        }
    }

    @Override // com.android.net.module.util.netlink.NetlinkMessage
    public String toString() {
        StringBuilder append = new StringBuilder().append("RtNetlinkRouteMessage{ nlmsghdr{").append(this.mHeader.toString(Integer.valueOf(OsConstants.NETLINK_ROUTE))).append("}, Rtmsg{").append(this.mRtmsg.toString()).append("}, destination{").append(this.mDestination.getAddress().getHostAddress()).append("}, gateway{");
        InetAddress inetAddress = this.mGateway;
        return append.append(inetAddress == null ? "" : inetAddress.getHostAddress()).append("}, ifindex{").append(this.mIfindex).append("} }").toString();
    }
}
