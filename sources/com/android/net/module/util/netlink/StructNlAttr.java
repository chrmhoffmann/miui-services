package com.android.net.module.util.netlink;

import android.net.MacAddress;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
/* loaded from: classes.dex */
public class StructNlAttr {
    public static final int NLA_F_NESTED = 32768;
    public static final int NLA_HEADERLEN = 4;
    public short nla_len;
    public short nla_type;
    public byte[] nla_value;

    public static short makeNestedType(short type) {
        return (short) (32768 | type);
    }

    public static StructNlAttr peek(ByteBuffer byteBuffer) {
        if (byteBuffer == null || byteBuffer.remaining() < 4) {
            return null;
        }
        int baseOffset = byteBuffer.position();
        StructNlAttr struct = new StructNlAttr();
        ByteOrder originalOrder = byteBuffer.order();
        byteBuffer.order(ByteOrder.nativeOrder());
        try {
            struct.nla_len = byteBuffer.getShort();
            struct.nla_type = byteBuffer.getShort();
            byteBuffer.order(originalOrder);
            byteBuffer.position(baseOffset);
            if (struct.nla_len < 4) {
                return null;
            }
            return struct;
        } catch (Throwable th) {
            byteBuffer.order(originalOrder);
            throw th;
        }
    }

    public static StructNlAttr parse(ByteBuffer byteBuffer) {
        StructNlAttr struct = peek(byteBuffer);
        if (struct == null || byteBuffer.remaining() < struct.getAlignedLength()) {
            return null;
        }
        int baseOffset = byteBuffer.position();
        byteBuffer.position(baseOffset + 4);
        int valueLen = (struct.nla_len & 65535) - 4;
        if (valueLen > 0) {
            byte[] bArr = new byte[valueLen];
            struct.nla_value = bArr;
            byteBuffer.get(bArr, 0, valueLen);
            byteBuffer.position(struct.getAlignedLength() + baseOffset);
        }
        return struct;
    }

    public static StructNlAttr findNextAttrOfType(short attrType, ByteBuffer byteBuffer) {
        while (byteBuffer != null && byteBuffer.remaining() > 0) {
            StructNlAttr nlAttr = peek(byteBuffer);
            if (nlAttr != null) {
                if (nlAttr.nla_type == attrType) {
                    return parse(byteBuffer);
                }
                if (byteBuffer.remaining() >= nlAttr.getAlignedLength()) {
                    byteBuffer.position(byteBuffer.position() + nlAttr.getAlignedLength());
                } else {
                    return null;
                }
            } else {
                return null;
            }
        }
        return null;
    }

    public StructNlAttr() {
        this.nla_len = (short) 4;
    }

    public StructNlAttr(short type, byte value) {
        this.nla_len = (short) 4;
        this.nla_type = type;
        setValue(new byte[1]);
        this.nla_value[0] = value;
    }

    public StructNlAttr(short type, short value) {
        this(type, value, ByteOrder.nativeOrder());
    }

    public StructNlAttr(short type, short value, ByteOrder order) {
        this.nla_len = (short) 4;
        this.nla_type = type;
        setValue(new byte[2]);
        ByteBuffer buf = getValueAsByteBuffer();
        ByteOrder originalOrder = buf.order();
        try {
            buf.order(order);
            buf.putShort(value);
        } finally {
            buf.order(originalOrder);
        }
    }

    public StructNlAttr(short type, int value) {
        this(type, value, ByteOrder.nativeOrder());
    }

    public StructNlAttr(short type, int value, ByteOrder order) {
        this.nla_len = (short) 4;
        this.nla_type = type;
        setValue(new byte[4]);
        ByteBuffer buf = getValueAsByteBuffer();
        ByteOrder originalOrder = buf.order();
        try {
            buf.order(order);
            buf.putInt(value);
        } finally {
            buf.order(originalOrder);
        }
    }

    public StructNlAttr(short type, byte[] value) {
        this.nla_len = (short) 4;
        this.nla_type = type;
        setValue(value);
    }

    public StructNlAttr(short type, InetAddress ip) {
        this.nla_len = (short) 4;
        this.nla_type = type;
        setValue(ip.getAddress());
    }

    public StructNlAttr(short type, MacAddress mac) {
        this.nla_len = (short) 4;
        this.nla_type = type;
        setValue(mac.toByteArray());
    }

    public StructNlAttr(short type, String string) {
        this.nla_len = (short) 4;
        this.nla_type = type;
        byte[] value = null;
        try {
            byte[] stringBytes = string.getBytes("UTF-8");
            value = Arrays.copyOf(stringBytes, stringBytes.length + 1);
        } catch (UnsupportedEncodingException e) {
        } catch (Throwable th) {
            setValue(null);
            throw th;
        }
        setValue(value);
    }

    public StructNlAttr(short type, StructNlAttr... nested) {
        this();
        this.nla_type = makeNestedType(type);
        int payloadLength = 0;
        for (StructNlAttr nla : nested) {
            payloadLength += nla.getAlignedLength();
        }
        setValue(new byte[payloadLength]);
        ByteBuffer buf = getValueAsByteBuffer();
        for (StructNlAttr nla2 : nested) {
            nla2.pack(buf);
        }
    }

    public int getAlignedLength() {
        return NetlinkConstants.alignedLengthOf(this.nla_len);
    }

    public short getValueAsBe16(short defaultValue) {
        ByteBuffer byteBuffer = getValueAsByteBuffer();
        if (byteBuffer == null || byteBuffer.remaining() != 2) {
            return defaultValue;
        }
        ByteOrder originalOrder = byteBuffer.order();
        try {
            byteBuffer.order(ByteOrder.BIG_ENDIAN);
            return byteBuffer.getShort();
        } finally {
            byteBuffer.order(originalOrder);
        }
    }

    public int getValueAsBe32(int defaultValue) {
        ByteBuffer byteBuffer = getValueAsByteBuffer();
        if (byteBuffer == null || byteBuffer.remaining() != 4) {
            return defaultValue;
        }
        ByteOrder originalOrder = byteBuffer.order();
        try {
            byteBuffer.order(ByteOrder.BIG_ENDIAN);
            return byteBuffer.getInt();
        } finally {
            byteBuffer.order(originalOrder);
        }
    }

    public ByteBuffer getValueAsByteBuffer() {
        byte[] bArr = this.nla_value;
        if (bArr == null) {
            return null;
        }
        ByteBuffer byteBuffer = ByteBuffer.wrap(bArr);
        byteBuffer.order(ByteOrder.nativeOrder());
        return byteBuffer;
    }

    public byte getValueAsByte(byte defaultValue) {
        ByteBuffer byteBuffer = getValueAsByteBuffer();
        if (byteBuffer == null || byteBuffer.remaining() != 1) {
            return defaultValue;
        }
        return getValueAsByteBuffer().get();
    }

    public Integer getValueAsInteger() {
        ByteBuffer byteBuffer = getValueAsByteBuffer();
        if (byteBuffer == null || byteBuffer.remaining() != 4) {
            return null;
        }
        return Integer.valueOf(byteBuffer.getInt());
    }

    public int getValueAsInt(int defaultValue) {
        Integer value = getValueAsInteger();
        return value != null ? value.intValue() : defaultValue;
    }

    public InetAddress getValueAsInetAddress() {
        byte[] bArr = this.nla_value;
        if (bArr == null) {
            return null;
        }
        try {
            return InetAddress.getByAddress(bArr);
        } catch (UnknownHostException e) {
            return null;
        }
    }

    public MacAddress getValueAsMacAddress() {
        byte[] bArr = this.nla_value;
        if (bArr == null) {
            return null;
        }
        try {
            return MacAddress.fromBytes(bArr);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public String getValueAsString() {
        byte[] bArr = this.nla_value;
        if (bArr == null) {
            return null;
        }
        int length = bArr.length;
        short s = this.nla_len;
        if (length < (s - 4) - 1) {
            return null;
        }
        try {
            byte[] array = Arrays.copyOf(bArr, (s - 4) - 1);
            return new String(array, "UTF-8");
        } catch (UnsupportedEncodingException | NegativeArraySizeException e) {
            return null;
        }
    }

    public void pack(ByteBuffer byteBuffer) {
        ByteOrder originalOrder = byteBuffer.order();
        int originalPosition = byteBuffer.position();
        byteBuffer.order(ByteOrder.nativeOrder());
        try {
            byteBuffer.putShort(this.nla_len);
            byteBuffer.putShort(this.nla_type);
            byte[] bArr = this.nla_value;
            if (bArr != null) {
                byteBuffer.put(bArr);
            }
            byteBuffer.order(originalOrder);
            byteBuffer.position(getAlignedLength() + originalPosition);
        } catch (Throwable th) {
            byteBuffer.order(originalOrder);
            throw th;
        }
    }

    private void setValue(byte[] value) {
        this.nla_value = value;
        this.nla_len = (short) ((value != null ? value.length : 0) + 4);
    }

    public String toString() {
        return "StructNlAttr{ nla_len{" + ((int) this.nla_len) + "}, nla_type{" + ((int) this.nla_type) + "}, nla_value{" + NetlinkConstants.hexify(this.nla_value) + "}, }";
    }
}
