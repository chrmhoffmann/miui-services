package com.android.server.wifi;

import android.util.Log;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.Objects;
/* loaded from: classes.dex */
public class MiuiTcpInfo {
    public static final int ESTABLISHED_STATE = 1;
    public static final int SYN_RECV_STATE = 3;
    public static final int SYN_SENT_STATE = 2;
    private static final String TAG = "TcpInfo";
    final int mLost;
    final int mRetans;
    final int mRetransmits;
    final int mSegsIn;
    final int mSegsOut;
    final int mTotalRetrans;
    final int mUnacked;
    static final int LOST_OFFSET = getFieldOffset(Field.LOST);
    static final int RETRANSMITS_OFFSET = getFieldOffset(Field.RETRANSMITS);
    static final int RETRANS_OFFSET = getFieldOffset(Field.RETRANS);
    static final int SEGS_IN_OFFSET = getFieldOffset(Field.SEGS_IN);
    static final int SEGS_OUT_OFFSET = getFieldOffset(Field.SEGS_OUT);
    static final int UNACKED_OFFSET = getFieldOffset(Field.UNACKED);
    static final int TORALRETRANS_OFFSET = getFieldOffset(Field.TOTAL_RETRANS);

    /* loaded from: classes.dex */
    public enum Field {
        STATE(1),
        CASTATE(1),
        RETRANSMITS(1),
        PROBES(1),
        BACKOFF(1),
        OPTIONS(1),
        WSCALE(1),
        DELIVERY_RATE_APP_LIMITED(1),
        RTO(4),
        ATO(4),
        SND_MSS(4),
        RCV_MSS(4),
        UNACKED(4),
        SACKED(4),
        LOST(4),
        RETRANS(4),
        FACKETS(4),
        LAST_DATA_SENT(4),
        LAST_ACK_SENT(4),
        LAST_DATA_RECV(4),
        LAST_ACK_RECV(4),
        PMTU(4),
        RCV_SSTHRESH(4),
        RTT(4),
        RTTVAR(4),
        SND_SSTHRESH(4),
        SND_CWND(4),
        ADVMSS(4),
        REORDERING(4),
        RCV_RTT(4),
        RCV_SPACE(4),
        TOTAL_RETRANS(4),
        PACING_RATE(8),
        MAX_PACING_RATE(8),
        BYTES_ACKED(8),
        BYTES_RECEIVED(8),
        SEGS_OUT(4),
        SEGS_IN(4),
        NOTSENT_BYTES(4),
        MIN_RTT(4),
        DATA_SEGS_IN(4),
        DATA_SEGS_OUT(4),
        DELIVERY_RATE(8),
        BUSY_TIME(8),
        RWND_LIMITED(8),
        SNDBUF_LIMITED(8),
        DELIVERED(4),
        DELIVERED_CE(4),
        BYTES_SEND(8),
        BYTES_RETRANS(8),
        DACK_DUPS(4),
        RECORD_SEEN(4),
        RCV_OOOPACK(4),
        SND_WND(4);
        
        public final int size;

        Field(int s) {
            this.size = s;
        }
    }

    private static int getFieldOffset(Field needle) {
        Field[] values;
        int offset = 0;
        for (Field field : Field.values()) {
            if (field == needle) {
                return offset;
            }
            offset += field.size;
        }
        throw new IllegalArgumentException("Unknown field");
    }

    private MiuiTcpInfo(ByteBuffer bytes, int infolen) {
        int i = SEGS_IN_OFFSET;
        if (Field.SEGS_IN.size + i > infolen) {
            throw new IllegalArgumentException("Length " + infolen + " is less than required.");
        }
        int start = bytes.position();
        this.mSegsOut = bytes.getInt(SEGS_OUT_OFFSET + start);
        this.mSegsIn = bytes.getInt(i + start);
        this.mLost = bytes.getInt(LOST_OFFSET + start);
        this.mRetransmits = bytes.get(RETRANSMITS_OFFSET + start);
        this.mRetans = bytes.getInt(RETRANS_OFFSET + start);
        this.mUnacked = bytes.get(UNACKED_OFFSET + start);
        this.mTotalRetrans = bytes.getInt(TORALRETRANS_OFFSET + start);
        bytes.position(Math.min(infolen + start, bytes.limit()));
    }

    MiuiTcpInfo() {
        this.mRetransmits = 0;
        this.mRetans = 0;
        this.mLost = 0;
        this.mSegsOut = 0;
        this.mSegsIn = 0;
        this.mUnacked = 0;
        this.mTotalRetrans = 0;
    }

    public static MiuiTcpInfo parse(ByteBuffer bytes, int infolen) {
        try {
            return new MiuiTcpInfo(bytes, infolen);
        } catch (IllegalArgumentException | IndexOutOfBoundsException | BufferOverflowException | BufferUnderflowException e) {
            Log.e(TAG, "parsing error.", e);
            return null;
        }
    }

    private static String decodeWscale(byte num) {
        return String.valueOf((num >> 4) & 15) + ":" + String.valueOf(num & 15);
    }

    static String getTcpStateName(int state) {
        switch (state) {
            case 1:
                return "TCP_ESTABLISHED";
            case 2:
                return "TCP_SYN_SENT";
            case 3:
                return "TCP_SYN_RECV";
            case 4:
                return "TCP_FIN_WAIT1";
            case 5:
                return "TCP_FIN_WAIT2";
            case 6:
                return "TCP_TIME_WAIT";
            case 7:
                return "TCP_CLOSE";
            case 8:
                return "TCP_CLOSE_WAIT";
            case 9:
                return "TCP_LAST_ACK";
            case 10:
                return "TCP_LISTEN";
            case 11:
                return "TCP_CLOSING";
            default:
                return "UNKNOWN:" + Integer.toString(state);
        }
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof MiuiTcpInfo)) {
            return false;
        }
        MiuiTcpInfo other = (MiuiTcpInfo) obj;
        return this.mSegsOut == other.mSegsOut && this.mRetransmits == other.mRetransmits && this.mLost == other.mLost;
    }

    public int hashCode() {
        return Objects.hash(Integer.valueOf(this.mLost), Integer.valueOf(this.mRetransmits), Integer.valueOf(this.mSegsOut), Integer.valueOf(this.mRetans));
    }

    public String toString() {
        return "TcpInfo{lost=" + this.mLost + ", retransmit=" + this.mRetransmits + ", sent=" + this.mSegsOut + "}";
    }
}
