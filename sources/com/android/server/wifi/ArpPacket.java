package com.android.server.wifi;

import android.system.OsConstants;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
/* loaded from: classes.dex */
class ArpPacket {
    public static final int ARP_ETHER_IPV4_LEN = 42;
    public static final int ARP_HWTYPE_ETHER = 1;
    public static final int ARP_PAYLOAD_LEN = 28;
    public static final int ETHER_ADDR_LEN = 6;
    public static final int ETHER_HEADER_LEN = 14;
    public static final int IPV4_ADDR_LEN = 4;
    public static final short OPCODE_ARP_REQUEST = 1;
    public static final short OPCODE_ARP_RESPONSE = 2;
    public final short mOpCode;
    public final byte[] mSenderHwAddress;
    public final Inet4Address mSenderIp;
    public final Inet4Address mTargetIp;
    public final byte[] mTtargetHwAddress;
    public static final Inet4Address INADDR_ANY = (Inet4Address) Inet4Address.ANY;
    public static final byte[] ETHER_BROADCAST = {-1, -1, -1, -1, -1, -1};
    public static final byte[] ETHER_ANY = {0, 0, 0, 0, 0, 0};

    ArpPacket(short opCode, byte[] senderHwAddress, Inet4Address senderIp, byte[] targetHwAddress, Inet4Address targetIp) {
        this.mOpCode = opCode;
        this.mSenderHwAddress = senderHwAddress;
        this.mSenderIp = senderIp;
        this.mTtargetHwAddress = targetHwAddress;
        this.mTargetIp = targetIp;
    }

    public static ByteBuffer buildArpPacket(byte[] dstMac, byte[] srcMac, byte[] sendHwAddress, byte[] senderIp, byte[] targetHwAddress, byte[] targetIp, short opCode) {
        ByteBuffer buf = ByteBuffer.allocate(42);
        buf.clear();
        buf.order(ByteOrder.BIG_ENDIAN);
        buf.put(dstMac);
        buf.put(srcMac);
        buf.putShort((short) OsConstants.ETH_P_ARP);
        buf.putShort((short) 1);
        buf.putShort((short) OsConstants.ETH_P_IP);
        buf.put((byte) 6);
        buf.put((byte) 4);
        buf.putShort(opCode);
        buf.put(sendHwAddress);
        buf.put(senderIp);
        buf.put(targetHwAddress);
        buf.put(targetIp);
        buf.flip();
        return buf;
    }

    public static ArpPacket parseArpPacket(byte[] recvbuf, int length) throws ParseException {
        if (length >= 42) {
            try {
                if (recvbuf.length >= length) {
                    ByteBuffer buffer = ByteBuffer.wrap(recvbuf, 0, length).order(ByteOrder.BIG_ENDIAN);
                    byte[] l2dst = new byte[6];
                    byte[] l2src = new byte[6];
                    buffer.get(l2dst);
                    buffer.get(l2src);
                    short etherType = buffer.getShort();
                    if (etherType != OsConstants.ETH_P_ARP) {
                        throw new ParseException("Incorrect Ether Type: " + ((int) etherType));
                    }
                    short hwType = buffer.getShort();
                    if (hwType != 1) {
                        throw new ParseException("Incorrect HW Type: " + ((int) hwType));
                    }
                    short protoType = buffer.getShort();
                    if (protoType != OsConstants.ETH_P_IP) {
                        throw new ParseException("Incorrect Protocol Type: " + ((int) protoType));
                    }
                    byte hwAddrLength = buffer.get();
                    if (hwAddrLength != 6) {
                        throw new ParseException("Incorrect HW address length: " + ((int) hwAddrLength));
                    }
                    byte ipAddrLength = buffer.get();
                    if (ipAddrLength != 4) {
                        throw new ParseException("Incorrect Protocol address length: " + ((int) ipAddrLength));
                    }
                    short opCode = buffer.getShort();
                    if (opCode != 1 && opCode != 2) {
                        throw new ParseException("Incorrect opCode: " + ((int) opCode));
                    }
                    byte[] senderHwAddress = new byte[6];
                    byte[] senderIp = new byte[4];
                    buffer.get(senderHwAddress);
                    buffer.get(senderIp);
                    byte[] targetHwAddress = new byte[6];
                    byte[] targetIp = new byte[4];
                    buffer.get(targetHwAddress);
                    buffer.get(targetIp);
                    return new ArpPacket(opCode, senderHwAddress, (Inet4Address) InetAddress.getByAddress(senderIp), targetHwAddress, (Inet4Address) InetAddress.getByAddress(targetIp));
                }
            } catch (IllegalArgumentException e) {
                throw new ParseException("Invalid MAC address representation");
            } catch (IndexOutOfBoundsException e2) {
                throw new ParseException("Invalid index when wrapping a byte array into a buffer");
            } catch (UnknownHostException e3) {
                throw new ParseException("Invalid IP address of Host");
            } catch (BufferUnderflowException e4) {
                throw new ParseException("Invalid buffer position");
            }
        }
        throw new ParseException("Invalid packet length: " + length);
    }

    /* loaded from: classes.dex */
    public static class ParseException extends Exception {
        ParseException(String message) {
            super(message);
        }
    }
}
