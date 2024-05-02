package com.android.net.module.util.netlink;

import android.util.Log;
import com.android.net.module.util.Struct;
import com.android.net.module.util.structs.RdnssOption;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.StringJoiner;
/* loaded from: classes.dex */
public class StructNdOptRdnss extends NdOption {
    public static final byte MIN_OPTION_LEN = 3;
    private static final String TAG = StructNdOptRdnss.class.getSimpleName();
    public static final int TYPE = 25;
    public final RdnssOption header;
    public final Inet6Address[] servers;

    public StructNdOptRdnss(Inet6Address[] servers, long lifetime) {
        super((byte) 25, (servers.length * 2) + 1);
        Objects.requireNonNull(servers, "Recursive DNS Servers address array must not be null");
        if (servers.length == 0) {
            throw new IllegalArgumentException("DNS server address array must not be empty");
        }
        this.header = new RdnssOption((byte) 25, (byte) ((servers.length * 2) + 1), (short) 0, lifetime);
        this.servers = (Inet6Address[]) servers.clone();
    }

    public static StructNdOptRdnss parse(ByteBuffer buf) {
        if (buf == null || buf.remaining() < 24) {
            return null;
        }
        try {
            RdnssOption header = (RdnssOption) Struct.parse(RdnssOption.class, buf);
            if (header.type != 25) {
                throw new IllegalArgumentException("Invalid type " + ((int) header.type));
            }
            if (header.length < 3 || header.length % 2 == 0) {
                throw new IllegalArgumentException("Invalid length " + ((int) header.length));
            }
            int numOfDnses = (header.length - 1) / 2;
            Inet6Address[] servers = new Inet6Address[numOfDnses];
            for (int i = 0; i < numOfDnses; i++) {
                byte[] rawAddress = new byte[16];
                buf.get(rawAddress);
                servers[i] = (Inet6Address) InetAddress.getByAddress(rawAddress);
            }
            return new StructNdOptRdnss(servers, header.lifetime);
        } catch (IllegalArgumentException | UnknownHostException | BufferUnderflowException e) {
            Log.d(TAG, "Invalid RDNSS option: " + e);
            return null;
        }
    }

    @Override // com.android.net.module.util.netlink.NdOption
    public void writeToByteBuffer(ByteBuffer buf) {
        this.header.writeToByteBuffer(buf);
        int i = 0;
        while (true) {
            Inet6Address[] inet6AddressArr = this.servers;
            if (i < inet6AddressArr.length) {
                buf.put(inet6AddressArr[i].getAddress());
                i++;
            } else {
                return;
            }
        }
    }

    public ByteBuffer toByteBuffer() {
        ByteBuffer buf = ByteBuffer.allocate(Struct.getSize(RdnssOption.class) + (this.servers.length * 16));
        writeToByteBuffer(buf);
        buf.flip();
        return buf;
    }

    @Override // com.android.net.module.util.netlink.NdOption
    public String toString() {
        StringJoiner sj = new StringJoiner(",", "[", "]");
        int i = 0;
        while (true) {
            Inet6Address[] inet6AddressArr = this.servers;
            if (i < inet6AddressArr.length) {
                sj.add(inet6AddressArr[i].getHostAddress());
                i++;
            } else {
                return String.format("NdOptRdnss(%s,servers:%s)", this.header.toString(), sj.toString());
            }
        }
    }
}
