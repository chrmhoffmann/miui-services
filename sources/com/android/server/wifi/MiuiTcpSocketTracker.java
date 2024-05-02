package com.android.server.wifi;

import android.content.Context;
import android.net.INetd;
import android.net.MarkMaskParcel;
import android.net.Network;
import android.net.util.SocketUtils;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.provider.DeviceConfig;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.system.StructTimeval;
import android.util.Log;
import android.util.LongSparseArray;
import android.util.SparseArray;
import com.android.net.module.util.netlink.InetDiagMessage;
import com.android.net.module.util.netlink.NetlinkConstants;
import com.android.net.module.util.netlink.NetlinkSocket;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
/* loaded from: classes.dex */
public class MiuiTcpSocketTracker {
    private static final int[] ADDRESS_FAMILIES = {OsConstants.AF_INET6, OsConstants.AF_INET};
    public static final String CONFIG_MIN_PACKETS_THRESHOLD = "tcp_min_packets_threshold";
    public static final String CONFIG_TCP_PACKETS_FAIL_PERCENTAGE = "tcp_packets_fail_percentage";
    public static final int DEFAULT_DATA_STALL_MIN_PACKETS_THRESHOLD = 10;
    public static final int DEFAULT_NLMSG_DONE_PACKET_SIZE = 20;
    public static final int DEFAULT_TCP_PACKETS_FAIL_PERCENTAGE = 80;
    private static final int IDIAG_COOKIE_OFFSET = 44;
    private static final int IDIAG_UID2COOKIE_OFFSET = 12;
    private static final int NULL_MASK = 0;
    private static final String TAG = "MiuiTcpSocketTracker";
    public static final int TCP_ESTABLISHED = 1;
    public static final int TCP_MONITOR_STATE_FILTER = 14;
    public static final int TCP_SYN_RECV = 3;
    public static final int TCP_SYN_SENT = 2;
    private static final int UNKNOWN_MARK = -1;
    private final Dependencies mDependencies;
    private int mLatestReceivedCount;
    private TcpStat mLatestTcpStats;
    private final INetd mNetd;
    private final Network mNetwork;
    private final int mNetworkMark;
    private final int mNetworkMask;
    private int mSentSinceLastRecv;
    private String msgRecord;
    private final SparseArray<byte[]> mSockDiagMsg = new SparseArray<>();
    private int mMinPacketsThreshold = 10;
    private int mTcpPacketsFailRateThreshold = 80;
    private final LongSparseArray<SocketInfo> mSocketInfos = new LongSparseArray<>();
    private final LongSparseArray<SocketInfo> mToRemovedSocketInfos = new LongSparseArray<>();
    private int mLatestPacketFailPercentage = 0;
    protected final DeviceConfig.OnPropertiesChangedListener mConfigListener = new DeviceConfig.OnPropertiesChangedListener() { // from class: com.android.server.wifi.MiuiTcpSocketTracker.1
        public void onPropertiesChanged(DeviceConfig.Properties properties) {
            MiuiTcpSocketTracker miuiTcpSocketTracker = MiuiTcpSocketTracker.this;
            miuiTcpSocketTracker.mMinPacketsThreshold = miuiTcpSocketTracker.mDependencies.getDeviceConfigPropertyInt("connectivity", MiuiTcpSocketTracker.CONFIG_MIN_PACKETS_THRESHOLD, 10);
            MiuiTcpSocketTracker miuiTcpSocketTracker2 = MiuiTcpSocketTracker.this;
            miuiTcpSocketTracker2.mTcpPacketsFailRateThreshold = miuiTcpSocketTracker2.mDependencies.getDeviceConfigPropertyInt("connectivity", MiuiTcpSocketTracker.CONFIG_TCP_PACKETS_FAIL_PERCENTAGE, 80);
        }
    };

    public MiuiTcpSocketTracker(Dependencies dps, Network network) {
        int[] iArr;
        this.mDependencies = dps;
        this.mNetwork = network;
        this.mNetd = dps.getNetd();
        MarkMaskParcel parcel = getNetworkMarkMask();
        this.mNetworkMark = parcel != null ? parcel.mark : -1;
        this.mNetworkMask = parcel != null ? parcel.mask : 0;
        if (!dps.isTcpInfoParsingSupported()) {
            return;
        }
        for (int family : ADDRESS_FAMILIES) {
            this.mSockDiagMsg.put(family, InetDiagMessage.inetDiagReqV2(OsConstants.IPPROTO_TCP, null, null, family, (short) 769, 0, 2, 14));
        }
        this.mDependencies.addDeviceConfigChangedListener(this.mConfigListener);
    }

    private MarkMaskParcel getNetworkMarkMask() {
        try {
            int netId = this.mNetwork.getNetId();
            return this.mNetd.getFwmarkForNetwork(netId);
        } catch (Exception e) {
            Log.e(TAG, "Get netId is not available in this API level, ", e);
            return null;
        }
    }

    /* JADX WARN: Code restructure failed: missing block: B:17:0x0055, code lost:
        android.util.Log.e(com.android.server.wifi.MiuiTcpSocketTracker.TAG, "Badly formatted data.");
     */
    /* JADX WARN: Code restructure failed: missing block: B:18:0x005a, code lost:
        r21 = r5;
        r22 = r12;
        r17 = r14;
        r16 = r15;
        r15 = r2;
        r12 = r6;
     */
    /* JADX WARN: Removed duplicated region for block: B:107:0x01e7 A[EDGE_INSN: B:61:0x01e5->B:107:0x01e7 ?: BREAK  , SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:63:0x01f3 A[LOOP:1: B:11:0x003d->B:63:0x01f3, LOOP_END] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public boolean pollSocketsInfo(java.util.Set<java.lang.Integer> r24) {
        /*
            Method dump skipped, instructions count: 597
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.MiuiTcpSocketTracker.pollSocketsInfo(java.util.Set):boolean");
    }

    public String getTcpInfo() {
        return this.msgRecord;
    }

    public static void closeSocketQuietly(FileDescriptor fd) {
        try {
            SocketUtils.closeSocket(fd);
        } catch (IOException e) {
        }
    }

    private void cleanupSocketInfo(long time) {
        int size = this.mSocketInfos.size();
        List<Long> toRemove = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            long key = this.mSocketInfos.keyAt(i);
            if (this.mSocketInfos.get(key).updateTime < time) {
                toRemove.add(Long.valueOf(key));
            }
        }
        for (Long key2 : toRemove) {
            this.mToRemovedSocketInfos.put(key2.longValue(), this.mSocketInfos.get(key2.longValue()));
            this.mSocketInfos.remove(key2.longValue());
        }
    }

    public TcpStat getInvalidTcpInfo() {
        int size = this.mToRemovedSocketInfos.size();
        TcpStat stat = new TcpStat();
        for (int i = 0; i < size; i++) {
            long key = this.mToRemovedSocketInfos.keyAt(i);
            stat.retransmit += this.mToRemovedSocketInfos.get(key).tcpInfo.mRetransmits;
            stat.unacked += this.mToRemovedSocketInfos.get(key).tcpInfo.mUnacked;
            stat.retans += this.mToRemovedSocketInfos.get(key).tcpInfo.mRetans;
            stat.lostCount += this.mToRemovedSocketInfos.get(key).tcpInfo.mLost;
        }
        return stat;
    }

    SocketInfo parseSockInfo(ByteBuffer bytes, int family, int nlmsgLen, long time) {
        int remainingDataSize = (bytes.position() + nlmsgLen) - 88;
        MiuiTcpInfo tcpInfo = null;
        int mark = 0;
        while (bytes.position() < remainingDataSize) {
            RoutingAttribute rtattr = new RoutingAttribute(bytes.getShort(), bytes.getShort());
            short dataLen = rtattr.getDataLength();
            if (rtattr.rtaType == 2) {
                tcpInfo = MiuiTcpInfo.parse(bytes, dataLen);
            } else if (rtattr.rtaType == 15) {
                mark = bytes.getInt();
            } else {
                skipRemainingAttributesBytesAligned(bytes, dataLen);
            }
        }
        SocketInfo info = new SocketInfo(tcpInfo, family, mark, time);
        return info;
    }

    static boolean enoughBytesRemainForValidNlMsg(ByteBuffer bytes) {
        return bytes.remaining() >= 16;
    }

    private static boolean isValidInetDiagMsgSize(int nlMsgLen) {
        return nlMsgLen >= 88;
    }

    public int getLatestPacketFailPercentage() {
        if (!this.mDependencies.isTcpInfoParsingSupported()) {
            return -1;
        }
        return this.mLatestPacketFailPercentage;
    }

    public TcpStat getLatestTcpStats() {
        return this.mLatestTcpStats;
    }

    private TcpStat calculateLatestPacketsStat(SocketInfo current, long cookies) {
        TcpStat stat = new TcpStat();
        SocketInfo previous = this.mSocketInfos.get(cookies);
        if ((current.fwmark & this.mNetworkMask) != this.mNetworkMark) {
            Log.d(TAG, "skip mismatch tcpInfo:" + current.tcpInfo);
            return null;
        } else if (current.tcpInfo == null) {
            Log.d(TAG, "Current tcpInfo is null.");
            return null;
        } else {
            stat.sentCount = current.tcpInfo.mSegsOut;
            stat.lostCount = current.tcpInfo.mLost;
            stat.retans = current.tcpInfo.mRetans;
            stat.retransmit = current.tcpInfo.mRetransmits;
            stat.unacked = current.tcpInfo.mUnacked;
            stat.totalretrans = current.tcpInfo.mTotalRetrans;
            stat.recvCount = current.tcpInfo.mSegsIn;
            if (previous != null && previous.tcpInfo != null) {
                stat.sentCount -= previous.tcpInfo.mSegsOut;
                stat.totalretrans -= previous.tcpInfo.mTotalRetrans;
                stat.recvCount -= previous.tcpInfo.mSegsIn;
            }
            return stat;
        }
    }

    public int getSentSinceLastRecv() {
        if (!this.mDependencies.isTcpInfoParsingSupported()) {
            return -1;
        }
        return this.mSentSinceLastRecv;
    }

    public int getLatestReceivedCount() {
        if (!this.mDependencies.isTcpInfoParsingSupported()) {
            return -1;
        }
        return this.mLatestReceivedCount;
    }

    private int getMinPacketsThreshold() {
        return this.mMinPacketsThreshold;
    }

    private int getTcpPacketsFailRateThreshold() {
        return this.mTcpPacketsFailRateThreshold;
    }

    private void skipRemainingAttributesBytesAligned(ByteBuffer buffer, short len) {
        int cur = buffer.position();
        buffer.position(NetlinkConstants.alignedLengthOf(len) + cur);
    }

    /* loaded from: classes.dex */
    public class TcpStat {
        public int socketCnt = 0;
        public int sentCount = 0;
        public int recvCount = 0;
        public int lostCount = 0;
        public int retans = 0;
        public int retransmit = 0;
        public int unacked = 0;
        public int totalretrans = 0;

        public TcpStat() {
            MiuiTcpSocketTracker.this = this$0;
        }

        void accumulate(TcpStat stat) {
            if (stat == null) {
                return;
            }
            this.sentCount += stat.sentCount;
            this.recvCount += stat.recvCount;
            this.lostCount += stat.lostCount;
            this.retans += stat.retans;
            this.retransmit += stat.retransmit;
            this.unacked += stat.unacked;
            this.totalretrans += stat.totalretrans;
        }

        public String printMsg() {
            return " failpercent=" + MiuiTcpSocketTracker.this.mLatestPacketFailPercentage + " { out=" + this.sentCount + " in=" + this.recvCount + " lost=" + this.lostCount + " inretrans=" + this.retans + " total retrans=" + this.totalretrans + " tmo=" + this.retransmit + " inflight=" + this.unacked + "}";
        }

        public int getFailPercent() {
            int i = this.sentCount;
            if (i <= 0) {
                return -1;
            }
            int failpercent = 100;
            int failpercent2 = ((((this.lostCount + this.retans) + this.retransmit) + this.unacked) * 100) / i;
            if (failpercent2 <= 100) {
                failpercent = failpercent2;
            }
            return failpercent;
        }
    }

    /* loaded from: classes.dex */
    public class SocketInfo {
        public static final int INIT_MARK_VALUE = 0;
        public final int fwmark;
        public final int ipFamily;
        public final MiuiTcpInfo tcpInfo;
        public final long updateTime;

        SocketInfo(MiuiTcpInfo info, int family, int mark, long time) {
            MiuiTcpSocketTracker.this = this$0;
            this.tcpInfo = info;
            this.ipFamily = family;
            this.updateTime = time;
            this.fwmark = mark;
        }

        public String toString() {
            return "SocketInfo {Type:" + ipTypeToString(this.ipFamily) + ", " + this.tcpInfo + ", mark:" + this.fwmark + " updated at " + this.updateTime + "}";
        }

        private String ipTypeToString(int type) {
            if (type == OsConstants.AF_INET) {
                return "IP";
            }
            if (type == OsConstants.AF_INET6) {
                return "IPV6";
            }
            return "UNKNOWN";
        }
    }

    /* loaded from: classes.dex */
    public class RoutingAttribute {
        public static final int HEADER_LENGTH = 4;
        public static final int INET_DIAG_INFO = 2;
        public static final int INET_DIAG_MARK = 15;
        public final short rtaLen;
        public final short rtaType;

        RoutingAttribute(short len, short type) {
            MiuiTcpSocketTracker.this = this$0;
            this.rtaLen = len;
            this.rtaType = type;
        }

        public short getDataLength() {
            return (short) (this.rtaLen - 4);
        }
    }

    /* loaded from: classes.dex */
    public static class Dependencies {
        private static final int DEFAULT_RECV_BUFSIZE = 60000;
        private static final long IO_TIMEOUT = 1000;
        private final Context mContext;

        public Dependencies(Context context) {
            this.mContext = context;
        }

        public FileDescriptor connectToKernel() throws ErrnoException, SocketException {
            FileDescriptor fd = Os.socket(OsConstants.AF_NETLINK, OsConstants.SOCK_DGRAM | OsConstants.SOCK_CLOEXEC, OsConstants.NETLINK_INET_DIAG);
            Os.connect(fd, SocketUtils.makeNetlinkSocketAddress(0, 0));
            return fd;
        }

        public void sendPollingRequest(FileDescriptor fd, byte[] msg) throws ErrnoException, InterruptedIOException {
            Os.setsockoptTimeval(fd, OsConstants.SOL_SOCKET, OsConstants.SO_SNDTIMEO, StructTimeval.fromMillis(IO_TIMEOUT));
            Os.write(fd, msg, 0, msg.length);
        }

        public int getDeviceConfigPropertyInt(String namespace, String name, int defaultValue) {
            String value1 = DeviceConfig.getProperty(namespace, name);
            String value = value1 != null ? value1 : null;
            if (value == null) {
                return defaultValue;
            }
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }

        public boolean isTcpInfoParsingSupported() {
            int devApiLevel = Build.VERSION.SDK_INT + (!"REL".equals(Build.VERSION.CODENAME) ? 1 : 0);
            return devApiLevel > 29;
        }

        public ByteBuffer recvMessage(FileDescriptor fd) throws ErrnoException, IllegalArgumentException, InterruptedIOException {
            return NetlinkSocket.recvMessage(fd, 60000, IO_TIMEOUT);
        }

        public INetd getNetd() {
            return INetd.Stub.asInterface((IBinder) this.mContext.getSystemService("netd"));
        }

        public void addDeviceConfigChangedListener(DeviceConfig.OnPropertiesChangedListener listener) {
            DeviceConfig.addOnPropertiesChangedListener("connectivity", AsyncTask.THREAD_POOL_EXECUTOR, listener);
        }

        public Context getContext() {
            return this.mContext;
        }
    }
}
