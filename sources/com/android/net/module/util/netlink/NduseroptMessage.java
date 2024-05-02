package com.android.net.module.util.netlink;

import android.system.OsConstants;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
/* loaded from: classes.dex */
public class NduseroptMessage extends NetlinkMessage {
    static final int NDUSEROPT_SRCADDR = 1;
    public static final int STRUCT_SIZE = 16;
    public final byte family;
    public final byte icmp_code;
    public final byte icmp_type;
    public final int ifindex;
    public final NdOption option;
    public final int opts_len;
    public final InetAddress srcaddr;

    NduseroptMessage(StructNlMsgHdr header, ByteBuffer buf) throws UnknownHostException {
        super(header);
        buf.order(ByteOrder.nativeOrder());
        int start = buf.position();
        byte b = buf.get();
        this.family = b;
        buf.get();
        int unsignedInt = Short.toUnsignedInt(buf.getShort());
        this.opts_len = unsignedInt;
        int i = buf.getInt();
        this.ifindex = i;
        this.icmp_type = buf.get();
        this.icmp_code = buf.get();
        buf.position(buf.position() + 6);
        buf.order(ByteOrder.BIG_ENDIAN);
        try {
            ByteBuffer slice = buf.slice();
            slice.limit(unsignedInt);
            this.option = NdOption.parse(slice);
            int newPosition = start + 16 + unsignedInt;
            if (newPosition >= buf.limit()) {
                throw new IllegalArgumentException("ND option extends past end of buffer");
            }
            buf.position(newPosition);
            StructNlAttr nla = StructNlAttr.parse(buf);
            if (nla == null || nla.nla_type != 1 || nla.nla_value == null) {
                throw new IllegalArgumentException("Invalid source address in ND useropt");
            }
            if (b == OsConstants.AF_INET6) {
                this.srcaddr = Inet6Address.getByAddress((String) null, nla.nla_value, i);
            } else {
                this.srcaddr = InetAddress.getByAddress(nla.nla_value);
            }
        } catch (Throwable th) {
            int newPosition2 = start + 16 + this.opts_len;
            if (newPosition2 >= buf.limit()) {
                throw new IllegalArgumentException("ND option extends past end of buffer");
            }
            buf.position(newPosition2);
            throw th;
        }
    }

    public static NduseroptMessage parse(StructNlMsgHdr header, ByteBuffer buf) {
        if (buf == null || buf.remaining() < 16) {
            return null;
        }
        ByteOrder oldOrder = buf.order();
        try {
            return new NduseroptMessage(header, buf);
        } catch (IllegalArgumentException | UnknownHostException | BufferUnderflowException e) {
            return null;
        } finally {
            buf.order(oldOrder);
        }
    }

    @Override // com.android.net.module.util.netlink.NetlinkMessage
    public String toString() {
        return String.format("Nduseroptmsg(%d, %d, %d, %d, %d, %s)", Byte.valueOf(this.family), Integer.valueOf(this.opts_len), Integer.valueOf(this.ifindex), Integer.valueOf(Byte.toUnsignedInt(this.icmp_type)), Integer.valueOf(Byte.toUnsignedInt(this.icmp_code)), this.srcaddr.getHostAddress());
    }
}
