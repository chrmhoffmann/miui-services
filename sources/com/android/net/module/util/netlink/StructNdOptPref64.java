package com.android.net.module.util.netlink;

import android.net.IpPrefix;
import android.util.Log;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Objects;
/* loaded from: classes.dex */
public class StructNdOptPref64 extends NdOption {
    public static final byte LENGTH = 2;
    public static final int STRUCT_SIZE = 16;
    private static final String TAG = StructNdOptPref64.class.getSimpleName();
    public static final int TYPE = 38;
    public final int lifetime;
    public final IpPrefix prefix;

    static int plcToPrefixLength(int plc) {
        switch (plc) {
            case 0:
                return 96;
            case 1:
                return 64;
            case 2:
                return 56;
            case 3:
                return 48;
            case 4:
                return 40;
            case 5:
                return 32;
            default:
                throw new IllegalArgumentException("Invalid prefix length code " + plc);
        }
    }

    static int prefixLengthToPlc(int prefixLength) {
        switch (prefixLength) {
            case 32:
                return 5;
            case 40:
                return 4;
            case 48:
                return 3;
            case 56:
                return 2;
            case 64:
                return 1;
            case 96:
                return 0;
            default:
                throw new IllegalArgumentException("Invalid prefix length " + prefixLength);
        }
    }

    static short getScaledLifetimePlc(int lifetime, int prefixLengthCode) {
        return (short) ((65528 & lifetime) | (prefixLengthCode & 7));
    }

    public StructNdOptPref64(IpPrefix prefix, int lifetime) {
        super((byte) 38, 2);
        Objects.requireNonNull(prefix, "prefix must not be null");
        if (!(prefix.getAddress() instanceof Inet6Address)) {
            throw new IllegalArgumentException("Must be an IPv6 prefix: " + prefix);
        }
        prefixLengthToPlc(prefix.getPrefixLength());
        this.prefix = prefix;
        if (lifetime < 0 || lifetime > 65528) {
            throw new IllegalArgumentException("Invalid lifetime " + lifetime);
        }
        this.lifetime = 65528 & lifetime;
    }

    private StructNdOptPref64(ByteBuffer buf) {
        super(buf.get(), Byte.toUnsignedInt(buf.get()));
        if (this.type != 38) {
            throw new IllegalArgumentException("Invalid type " + ((int) this.type));
        }
        if (this.length != 2) {
            throw new IllegalArgumentException("Invalid length " + this.length);
        }
        int scaledLifetimePlc = Short.toUnsignedInt(buf.getShort());
        this.lifetime = 65528 & scaledLifetimePlc;
        byte[] addressBytes = new byte[16];
        buf.get(addressBytes, 0, 12);
        try {
            InetAddress addr = InetAddress.getByAddress(addressBytes);
            this.prefix = new IpPrefix(addr, plcToPrefixLength(scaledLifetimePlc & 7));
        } catch (UnknownHostException e) {
            throw new AssertionError("16-byte array not valid InetAddress?");
        }
    }

    public static StructNdOptPref64 parse(ByteBuffer buf) {
        if (buf.remaining() < 16) {
            return null;
        }
        try {
            return new StructNdOptPref64(buf);
        } catch (IllegalArgumentException e) {
            Log.d(TAG, "Invalid PREF64 option: " + e);
            return null;
        }
    }

    @Override // com.android.net.module.util.netlink.NdOption
    public void writeToByteBuffer(ByteBuffer buf) {
        super.writeToByteBuffer(buf);
        buf.putShort(getScaledLifetimePlc(this.lifetime, prefixLengthToPlc(this.prefix.getPrefixLength())));
        buf.put(this.prefix.getRawAddress(), 0, 12);
    }

    public ByteBuffer toByteBuffer() {
        ByteBuffer buf = ByteBuffer.allocate(16);
        writeToByteBuffer(buf);
        buf.flip();
        return buf;
    }

    @Override // com.android.net.module.util.netlink.NdOption
    public String toString() {
        return String.format("NdOptPref64(%s, %d)", this.prefix, Integer.valueOf(this.lifetime));
    }
}
