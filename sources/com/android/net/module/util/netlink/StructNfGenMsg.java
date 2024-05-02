package com.android.net.module.util.netlink;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;
/* loaded from: classes.dex */
public class StructNfGenMsg {
    public static final int NFNETLINK_V0 = 0;
    public static final int STRUCT_SIZE = 4;
    public final byte nfgen_family;
    public final short res_id;
    public final byte version;

    public static StructNfGenMsg parse(ByteBuffer byteBuffer) {
        Objects.requireNonNull(byteBuffer);
        if (!hasAvailableSpace(byteBuffer)) {
            return null;
        }
        byte nfgen_family = byteBuffer.get();
        byte version = byteBuffer.get();
        ByteOrder originalOrder = byteBuffer.order();
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        short res_id = byteBuffer.getShort();
        byteBuffer.order(originalOrder);
        return new StructNfGenMsg(nfgen_family, version, res_id);
    }

    public StructNfGenMsg(byte family, byte ver, short id) {
        this.nfgen_family = family;
        this.version = ver;
        this.res_id = id;
    }

    public StructNfGenMsg(byte family) {
        this.nfgen_family = family;
        this.version = (byte) 0;
        this.res_id = (short) 0;
    }

    public void pack(ByteBuffer byteBuffer) {
        byteBuffer.put(this.nfgen_family);
        byteBuffer.put(this.version);
        ByteOrder originalOrder = byteBuffer.order();
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        byteBuffer.putShort(this.res_id);
        byteBuffer.order(originalOrder);
    }

    private static boolean hasAvailableSpace(ByteBuffer byteBuffer) {
        return byteBuffer.remaining() >= 4;
    }

    public String toString() {
        String familyStr = NetlinkConstants.stringForAddressFamily(this.nfgen_family);
        return "NfGenMsg{ nfgen_family{" + familyStr + "}, version{" + Byte.toUnsignedInt(this.version) + "}, res_id{" + Short.toUnsignedInt(this.res_id) + "} }";
    }
}
