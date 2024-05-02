package com.android.server.wifi;

import android.content.Context;
import android.content.Intent;
import android.net.MacAddress;
import android.net.util.SocketUtils;
import android.os.Handler;
import android.os.Message;
import android.os.MessageQueue;
import android.os.UserHandle;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.util.Log;
import com.android.internal.util.HexDump;
import com.android.net.module.util.Inet4AddressUtils;
import com.android.net.module.util.netlink.NetlinkSocket;
import com.android.net.module.util.netlink.RtNetlinkNeighborMessage;
import com.android.net.module.util.netlink.StructNdMsg;
import com.android.server.am.ProcessStarter;
import com.android.server.wifi.ArpPacket;
import java.io.BufferedReader;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.Inet4Address;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import libcore.util.EmptyArray;
/* loaded from: classes.dex */
public class ArpDetect {
    private static final int ARP_DETECT_TIMEOUT_MS = 8000;
    private static final int ARP_GATEWAY_DETECT_TIMES = 6;
    private static final int ARP_PACKET_INTERVAL_NORMAL_MS = 500;
    private static final int ARP_PACKET_INTERVAL_QUICK_MS = 50;
    private static final String CLOUD_MULTI_GW_DETECT_ENABLED = "cloud_multi_gateway_detect_enabled";
    private static final boolean DEBUG = false;
    private static final String EXTRA_ARP_DETECT_GATEWAY_SIZE = "extra_arp_detect_gateway_size";
    private static final int FD_EVENTS = 5;
    private static final int MAX_GATEWAY_LIST_LEN = 4;
    private static final int MAX_PACKET_LEN = 1500;
    public static final String MULTI_GATEWAY_DETECT_STATE_CHANGED = "android.net.wifi.GATEWAY_DETECT_STATE_CHANGED";
    private static final int MULTI_GW_RECOVERY_TIMEOUT_MS = 120000;
    private static final String TAG = "ArpDetect";
    private static final int UNREGISTER_THIS_FD = 0;
    private static ArpDetect sIntance;
    private FileDescriptor mArpRecvSock;
    private FileDescriptor mArpSendSock;
    private Context mContext;
    private Inet4Address mGatewayAddress;
    private ArrayList<byte[]> mGatewayMacList;
    private Handler mHandler;
    private String mIfaceName;
    private SocketAddress mInterfaceBroadcastAddr;
    private Inet4Address mLocalIpAddress;
    private byte[] mLocalMacAddress;
    private int mNetworkId;
    private MessageQueue mQueue;
    private MessageHandler msgHandler;
    private static final Inet4Address IPV4_ADDR_ANY = Inet4AddressUtils.intToInet4AddressHTH(0);
    public static int STATE_MULTI_GW_RECOVERY_STOPED = 0;
    public static int STATE_MULTI_GW_RECOVERY_STARTED = 1;
    private final int CMD_STOP_RECV_ARP_PACKET = 100;
    private final int CMD_MULTI_GW_RECOVERY_STOP = 101;
    private boolean isMultiGwFind = false;
    private boolean stopDetect = false;
    private int mGatewayChangeCount = 0;
    private int mCurRecoveryState = STATE_MULTI_GW_RECOVERY_STOPED;

    public ArpDetect(Context context) {
        this.mContext = context;
    }

    public static ArpDetect makeInstance(Context context) {
        ArpDetect arpDetect = new ArpDetect(context);
        sIntance = arpDetect;
        return arpDetect;
    }

    public static ArpDetect getInstance() {
        return sIntance;
    }

    public void logd(String str) {
    }

    public String hidenPrivateInfo(String str) {
        if (str == null || str.length() < 7) {
            return str;
        }
        String newStr = str.substring(0, 3) + "****" + str.substring(str.length() - 3);
        return newStr;
    }

    private NetworkInterface getNetworkInterfaceByName(String name) {
        try {
            return NetworkInterface.getByName(name);
        } catch (NullPointerException | SocketException e) {
            Log.e(TAG, "get NetworkInterface faile:", e);
            return null;
        }
    }

    private void initArpSendSocket() {
        try {
            NetworkInterface iface = getNetworkInterfaceByName(this.mIfaceName);
            if (iface == null) {
                return;
            }
            int ifaceIndex = iface.getIndex();
            this.mArpSendSock = Os.socket(OsConstants.AF_PACKET, OsConstants.SOCK_RAW | OsConstants.SOCK_NONBLOCK, 0);
            SocketAddress addr = SocketUtils.makePacketSocketAddress(OsConstants.ETH_P_ARP, ifaceIndex);
            Os.bind(this.mArpSendSock, addr);
        } catch (ErrnoException | SocketException e) {
            Log.e(TAG, "Error creating ARP send socket", e);
            closeSocket(this.mArpSendSock);
            this.mArpSendSock = null;
        }
    }

    private void initArpRecvSocket() {
        try {
            NetworkInterface iface = getNetworkInterfaceByName(this.mIfaceName);
            if (iface == null) {
                return;
            }
            int ifaceIndex = iface.getIndex();
            this.mArpRecvSock = Os.socket(OsConstants.AF_PACKET, OsConstants.SOCK_RAW | OsConstants.SOCK_NONBLOCK, 0);
            SocketAddress addr = SocketUtils.makePacketSocketAddress(OsConstants.ETH_P_ARP, ifaceIndex);
            Os.bind(this.mArpRecvSock, addr);
        } catch (ErrnoException | SocketException e) {
            Log.e(TAG, "Error creating ARP recv socket", e);
            closeSocket(this.mArpRecvSock);
            this.mArpRecvSock = null;
        }
    }

    private boolean initSocket() {
        NetworkInterface iface;
        if (this.mArpSendSock == null) {
            initArpSendSocket();
        }
        if (this.mArpRecvSock == null) {
            initArpRecvSocket();
        }
        if (this.mInterfaceBroadcastAddr == null && (iface = getNetworkInterfaceByName(this.mIfaceName)) != null) {
            int ifaceIndex = iface.getIndex();
            this.mInterfaceBroadcastAddr = SocketUtils.makePacketSocketAddress(OsConstants.ETH_P_IP, ifaceIndex, ArpPacket.ETHER_BROADCAST);
        }
        if (this.mArpSendSock == null || this.mArpRecvSock == null || this.mInterfaceBroadcastAddr == null) {
            Log.e(TAG, "initArpSocket fail");
            return false;
        }
        return true;
    }

    public void startGatewayDetect(int networkId, String ifaceName, Inet4Address localIpAddress, Inet4Address gatewayeIpAddress, byte[] localMacAddress) {
        if (ifaceName == null || localIpAddress == null || gatewayeIpAddress == null || localMacAddress == null || localMacAddress.length == 0) {
            Log.e(TAG, "Invalid param");
            return;
        }
        stopGatewayDetect();
        Log.d(TAG, "Start ArpDetect on " + ifaceName + " for netId: " + networkId);
        this.mNetworkId = networkId;
        this.mIfaceName = ifaceName;
        this.mLocalIpAddress = localIpAddress;
        this.mGatewayAddress = gatewayeIpAddress;
        this.mLocalMacAddress = localMacAddress;
        this.mGatewayMacList = new ArrayList<>();
        registerArpRecvQueue();
        MessageHandler messageHandler = new MessageHandler();
        this.msgHandler = messageHandler;
        messageHandler.sendEmptyMessageDelayed(100, 8000L);
        setMultiGwRecoveryState(STATE_MULTI_GW_RECOVERY_STARTED);
        startMultiGatewayDetect(this.mLocalMacAddress, this.mLocalIpAddress, this.mGatewayAddress, 6);
    }

    public void stopGatewayDetect() {
        Log.d(TAG, "Stop ArpDetect on " + this.mIfaceName);
        if (!this.stopDetect) {
            unregisterArpRecvQueue();
        }
        setMultiGwRecoveryState(STATE_MULTI_GW_RECOVERY_STOPED);
        FileDescriptor fileDescriptor = this.mArpSendSock;
        if (fileDescriptor != null) {
            closeSocket(fileDescriptor);
            this.mArpSendSock = null;
        }
        FileDescriptor fileDescriptor2 = this.mArpRecvSock;
        if (fileDescriptor2 != null) {
            closeSocket(fileDescriptor2);
            this.mArpRecvSock = null;
        }
        ArrayList<byte[]> arrayList = this.mGatewayMacList;
        if (arrayList != null) {
            arrayList.clear();
            this.mGatewayMacList = null;
        }
        this.msgHandler = null;
        this.mInterfaceBroadcastAddr = null;
        this.isMultiGwFind = false;
        this.mGatewayChangeCount = 0;
    }

    public boolean isMultiGatewayNetwork(int netId) {
        return netId == this.mNetworkId && isMultiGatewayExist();
    }

    public boolean isRecoveryOngoing(int netId) {
        if (isMultiGatewayNetwork(netId) && this.mCurRecoveryState != STATE_MULTI_GW_RECOVERY_STOPED) {
            return true;
        }
        return false;
    }

    public boolean isRecoveredOneRound(int netId) {
        ArrayList<byte[]> arrayList;
        return isMultiGatewayNetwork(netId) && (arrayList = this.mGatewayMacList) != null && this.mGatewayChangeCount >= arrayList.size() - 1;
    }

    public boolean setCurGatewayPerm() {
        boolean status = false;
        if (!isMultiGatewayExist() || !isRecoveryOngoing(this.mNetworkId)) {
            return false;
        }
        NetworkInterface iface = getNetworkInterfaceByName(this.mIfaceName);
        if (iface == null) {
            Log.d(TAG, "get iface fail");
            return false;
        }
        int ifaceIndex = iface.getIndex();
        byte[] curGatewayMac = getCurGatewayMacFromRoute();
        Log.d(TAG, "set current gateway " + hidenPrivateInfo(MacAddress.stringAddrFromByteAddr(curGatewayMac)) + " permanent for " + this.mIfaceName);
        if (curGatewayMac != null && curGatewayMac.length > 0 && !isPermArpRoute(this.mGatewayAddress.getHostAddress(), MacAddress.stringAddrFromByteAddr(curGatewayMac), this.mIfaceName)) {
            status = updateNeighbor(this.mGatewayAddress, StructNdMsg.NUD_PERMANENT, ifaceIndex, curGatewayMac);
        }
        setMultiGwRecoveryState(STATE_MULTI_GW_RECOVERY_STOPED);
        return status;
    }

    public boolean tryOtherGateway() {
        ArrayList<byte[]> arrayList;
        if (!isMultiGatewayExist() || !isRecoveryOngoing(this.mNetworkId)) {
            return false;
        }
        ArrayList<byte[]> arrayList2 = this.mGatewayMacList;
        if (arrayList2 != null && this.mGatewayChangeCount >= arrayList2.size()) {
            resetGatewayNeighState();
            setMultiGwRecoveryState(STATE_MULTI_GW_RECOVERY_STOPED);
            return false;
        }
        ArrayList<byte[]> arrayList3 = this.mGatewayMacList;
        if (arrayList3 != null && arrayList3.size() < 2) {
            Log.e(TAG, "Only one gateway device");
            return false;
        }
        Log.d(TAG, "try other gateway for " + this.mIfaceName);
        NetworkInterface iface = getNetworkInterfaceByName(this.mIfaceName);
        if (iface == null) {
            Log.d(TAG, "get iface fail");
            return false;
        }
        int ifaceIndex = iface.getIndex();
        int nextGatewayIndex = getNextGatewayIndex();
        if (nextGatewayIndex < 0 || (arrayList = this.mGatewayMacList) == null) {
            return false;
        }
        byte[] nextGatewayMac = arrayList.get(nextGatewayIndex);
        boolean status = updateNeighbor(this.mGatewayAddress, StructNdMsg.NUD_PERMANENT, ifaceIndex, nextGatewayMac);
        this.mGatewayChangeCount++;
        Log.d(TAG, "New gateway index " + nextGatewayIndex + " ,mac " + hidenPrivateInfo(HexDump.toHexString(nextGatewayMac)));
        return status;
    }

    public boolean resetGatewayNeighState() {
        if (!isMultiGatewayExist()) {
            return false;
        }
        NetworkInterface iface = getNetworkInterfaceByName(this.mIfaceName);
        if (iface == null) {
            Log.d(TAG, "get iface fail");
            return false;
        }
        int ifaceIndex = iface.getIndex();
        byte[] curGatewayMac = getCurGatewayMacFromRoute();
        if (curGatewayMac == null || curGatewayMac.length <= 0 || !isPermArpRoute(this.mGatewayAddress.getHostAddress(), MacAddress.stringAddrFromByteAddr(curGatewayMac), this.mIfaceName)) {
            return false;
        }
        Log.d(TAG, "reset current gateway neigh state for " + this.mIfaceName);
        boolean status = updateNeighbor(this.mGatewayAddress, (short) 2, ifaceIndex, curGatewayMac);
        return status;
    }

    private boolean isMultiGatewayExist() {
        return this.isMultiGwFind;
    }

    private static boolean updateNeighbor(Inet4Address ipAddr, short nudState, int ifIndex, byte[] macAddr) {
        if (ipAddr == null || macAddr == null) {
            Log.e(TAG, "update neigh fail,param is null");
            return false;
        } else if (ipAddr.getHostAddress().equals(IPV4_ADDR_ANY.getHostAddress()) || Arrays.equals(macAddr, ArpPacket.ETHER_ANY)) {
            Log.e(TAG, "update neigh fail,invaild ip or mac addr");
            return false;
        } else {
            byte[] msg = RtNetlinkNeighborMessage.newNewNeighborMessage(1, ipAddr, nudState, ifIndex, macAddr);
            try {
                NetlinkSocket.sendOneShotKernelMessage(OsConstants.NETLINK_ROUTE, msg);
                return true;
            } catch (ErrnoException e) {
                Log.e(TAG, "sendOneShotKernelMessage error:" + e);
                return false;
            }
        }
    }

    private void startMultiGatewayDetect(final byte[] senderMac, final Inet4Address senderIp, final Inet4Address targetIp, final int retryTimes) {
        Log.d(TAG, "Start multiple Gatway Detect..");
        Thread thread = new Thread(new Runnable() { // from class: com.android.server.wifi.ArpDetect.1
            @Override // java.lang.Runnable
            public void run() {
                try {
                    Log.d(ArpDetect.TAG, "arp detect for " + ArpDetect.this.hidenPrivateInfo(targetIp.getHostAddress()));
                    ArpDetect.this.sendArpPacket(senderMac, senderIp, ArpPacket.ETHER_ANY, targetIp, retryTimes / 2, 50);
                    Thread.sleep(100L);
                    ArpDetect.this.sendArpPacket(senderMac, senderIp, ArpPacket.ETHER_ANY, targetIp, retryTimes / 2, 500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Thread.currentThread().interrupt();
                }
            }
        });
        thread.start();
    }

    public boolean sendArpPacket(byte[] senderMac, Inet4Address senderIp, byte[] targetMac, Inet4Address targetIp, int retryTimes, int interval) {
        ByteBuffer packet = ArpPacket.buildArpPacket(ArpPacket.ETHER_BROADCAST, senderMac, senderMac, senderIp.getAddress(), targetMac, targetIp.getAddress(), (short) 1);
        return transmitPacket(packet, retryTimes, interval);
    }

    private boolean transmitPacket(ByteBuffer buf, int retryTimes, int interval) {
        for (int i = 1; i <= retryTimes; i++) {
            try {
                if (!this.stopDetect) {
                    Os.sendto(this.mArpSendSock, buf.array(), 0, buf.limit(), 0, this.mInterfaceBroadcastAddr);
                    Thread.sleep(interval);
                } else {
                    return true;
                }
            } catch (ErrnoException | IOException | InterruptedException e) {
                Log.e(TAG, "send packet error: ", e);
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return true;
    }

    private byte[] getCurGatewayMacFromRoute() {
        String curGatewayMac = getMacAddrFromRoute(this.mGatewayAddress.getHostAddress(), this.mIfaceName);
        if (curGatewayMac == null) {
            return EmptyArray.BYTE;
        }
        return MacAddress.byteAddrFromStringAddr(curGatewayMac);
    }

    private int getNextGatewayIndex() {
        ArrayList<byte[]> arrayList;
        int nextGatewayIndex;
        int curGatewayIndex = -1;
        byte[] curGatewayMac = getCurGatewayMacFromRoute();
        if (curGatewayMac == null || curGatewayMac.length <= 0 || (arrayList = this.mGatewayMacList) == null || arrayList.size() < 2) {
            return -1;
        }
        for (int i = 0; i < this.mGatewayMacList.size(); i++) {
            if (Arrays.equals(this.mGatewayMacList.get(i), curGatewayMac)) {
                curGatewayIndex = i;
            }
        }
        if (curGatewayIndex < this.mGatewayMacList.size() - 1) {
            nextGatewayIndex = curGatewayIndex + 1;
        } else {
            nextGatewayIndex = 0;
        }
        Log.d(TAG, "Current gateway index " + curGatewayIndex + " ,mac " + hidenPrivateInfo(HexDump.toHexString(curGatewayMac)));
        return nextGatewayIndex;
    }

    private boolean isGatewayMacSaved(byte[] macAddr) {
        ArrayList<byte[]> arrayList;
        ArrayList<byte[]> arrayList2 = this.mGatewayMacList;
        if ((arrayList2 == null || arrayList2.size() > 0) && (arrayList = this.mGatewayMacList) != null && arrayList.size() > 0) {
            for (int i = 0; i < this.mGatewayMacList.size(); i++) {
                if (Arrays.equals(this.mGatewayMacList.get(i), macAddr)) {
                    return true;
                }
            }
        }
        return false;
    }

    /* JADX WARN: Code restructure failed: missing block: B:21:0x005c, code lost:
        android.util.Log.d(com.android.server.wifi.ArpDetect.TAG, "neigh state is permanent");
     */
    /* JADX WARN: Code restructure failed: missing block: B:23:0x0063, code lost:
        r2.close();
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private boolean isPermArpRoute(java.lang.String r12, java.lang.String r13, java.lang.String r14) {
        /*
            r11 = this;
            java.lang.String r0 = "ArpDetect"
            r1 = 0
            if (r12 == 0) goto Lb8
            if (r13 == 0) goto Lb8
            if (r14 != 0) goto Lb
            goto Lb8
        Lb:
            r2 = 0
            java.io.BufferedReader r3 = new java.io.BufferedReader     // Catch: java.lang.Throwable -> L71 java.io.IOException -> L73 java.io.FileNotFoundException -> L90
            java.io.FileReader r4 = new java.io.FileReader     // Catch: java.lang.Throwable -> L71 java.io.IOException -> L73 java.io.FileNotFoundException -> L90
            java.lang.String r5 = "/proc/net/arp"
            r4.<init>(r5)     // Catch: java.lang.Throwable -> L71 java.io.IOException -> L73 java.io.FileNotFoundException -> L90
            r3.<init>(r4)     // Catch: java.lang.Throwable -> L71 java.io.IOException -> L73 java.io.FileNotFoundException -> L90
            r2 = r3
            java.lang.String r3 = r2.readLine()     // Catch: java.lang.Throwable -> L71 java.io.IOException -> L73 java.io.FileNotFoundException -> L90
        L1d:
            java.lang.String r4 = r2.readLine()     // Catch: java.lang.Throwable -> L71 java.io.IOException -> L73 java.io.FileNotFoundException -> L90
            r3 = r4
            if (r4 == 0) goto L6a
            java.lang.String r4 = "[ ]+"
            java.lang.String[] r4 = r3.split(r4)     // Catch: java.lang.Throwable -> L71 java.io.IOException -> L73 java.io.FileNotFoundException -> L90
            int r5 = r4.length     // Catch: java.lang.Throwable -> L71 java.io.IOException -> L73 java.io.FileNotFoundException -> L90
            r6 = 6
            if (r5 >= r6) goto L2f
            goto L1d
        L2f:
            r5 = r4[r1]     // Catch: java.lang.Throwable -> L71 java.io.IOException -> L73 java.io.FileNotFoundException -> L90
            r6 = 2
            r6 = r4[r6]     // Catch: java.lang.Throwable -> L71 java.io.IOException -> L73 java.io.FileNotFoundException -> L90
            r7 = 3
            r7 = r4[r7]     // Catch: java.lang.Throwable -> L71 java.io.IOException -> L73 java.io.FileNotFoundException -> L90
            r8 = 5
            r8 = r4[r8]     // Catch: java.lang.Throwable -> L71 java.io.IOException -> L73 java.io.FileNotFoundException -> L90
            java.lang.String r9 = "0x6"
            boolean r9 = r6.equals(r9)     // Catch: java.lang.Throwable -> L71 java.io.IOException -> L73 java.io.FileNotFoundException -> L90
            if (r9 == 0) goto L69
            boolean r9 = r14.equals(r8)     // Catch: java.lang.Throwable -> L71 java.io.IOException -> L73 java.io.FileNotFoundException -> L90
            if (r9 == 0) goto L69
            boolean r9 = r12.equals(r5)     // Catch: java.lang.Throwable -> L71 java.io.IOException -> L73 java.io.FileNotFoundException -> L90
            if (r9 == 0) goto L69
            java.lang.String r9 = r13.toLowerCase()     // Catch: java.lang.Throwable -> L71 java.io.IOException -> L73 java.io.FileNotFoundException -> L90
            java.lang.String r10 = r7.toLowerCase()     // Catch: java.lang.Throwable -> L71 java.io.IOException -> L73 java.io.FileNotFoundException -> L90
            boolean r9 = r9.equals(r10)     // Catch: java.lang.Throwable -> L71 java.io.IOException -> L73 java.io.FileNotFoundException -> L90
            if (r9 == 0) goto L69
            java.lang.String r9 = "neigh state is permanent"
            android.util.Log.d(r0, r9)     // Catch: java.lang.Throwable -> L71 java.io.IOException -> L73 java.io.FileNotFoundException -> L90
            r0 = 1
            r2.close()     // Catch: java.io.IOException -> L67
            goto L68
        L67:
            r1 = move-exception
        L68:
            return r0
        L69:
            goto L1d
        L6a:
            r2.close()     // Catch: java.io.IOException -> L6f
        L6e:
            goto Lad
        L6f:
            r0 = move-exception
            goto Lad
        L71:
            r0 = move-exception
            goto Lae
        L73:
            r3 = move-exception
            java.lang.StringBuilder r4 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> L71
            r4.<init>()     // Catch: java.lang.Throwable -> L71
            java.lang.String r5 = "Could not read /proc/net/arp "
            java.lang.StringBuilder r4 = r4.append(r5)     // Catch: java.lang.Throwable -> L71
            java.lang.StringBuilder r4 = r4.append(r3)     // Catch: java.lang.Throwable -> L71
            java.lang.String r4 = r4.toString()     // Catch: java.lang.Throwable -> L71
            android.util.Log.e(r0, r4)     // Catch: java.lang.Throwable -> L71
            if (r2 == 0) goto L6e
            r2.close()     // Catch: java.io.IOException -> L6f
            goto L6e
        L90:
            r3 = move-exception
            java.lang.StringBuilder r4 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> L71
            r4.<init>()     // Catch: java.lang.Throwable -> L71
            java.lang.String r5 = "Could not open /proc/net/arp "
            java.lang.StringBuilder r4 = r4.append(r5)     // Catch: java.lang.Throwable -> L71
            java.lang.StringBuilder r4 = r4.append(r3)     // Catch: java.lang.Throwable -> L71
            java.lang.String r4 = r4.toString()     // Catch: java.lang.Throwable -> L71
            android.util.Log.e(r0, r4)     // Catch: java.lang.Throwable -> L71
            if (r2 == 0) goto L6e
            r2.close()     // Catch: java.io.IOException -> L6f
            goto L6e
        Lad:
            return r1
        Lae:
            if (r2 == 0) goto Lb6
            r2.close()     // Catch: java.io.IOException -> Lb4
            goto Lb6
        Lb4:
            r1 = move-exception
            goto Lb7
        Lb6:
        Lb7:
            throw r0
        Lb8:
            return r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.ArpDetect.isPermArpRoute(java.lang.String, java.lang.String, java.lang.String):boolean");
    }

    private String getMacAddrFromRoute(String ipAddress, String ifaceName) {
        if (ipAddress == null || ifaceName == null) {
            return null;
        }
        String macAddress = null;
        BufferedReader reader = null;
        try {
            try {
                try {
                    try {
                        reader = new BufferedReader(new FileReader("/proc/net/arp"));
                        reader.readLine();
                        while (true) {
                            String line = reader.readLine();
                            if (line == null) {
                                break;
                            }
                            String[] tokens = line.split("[ ]+");
                            if (tokens.length >= 6) {
                                String ip = tokens[0];
                                String mac = tokens[3];
                                String curIfaceName = tokens[5];
                                if (ipAddress.equals(ip) && ifaceName.equals(curIfaceName)) {
                                    macAddress = mac;
                                    break;
                                }
                            }
                        }
                        if (macAddress == null) {
                            Log.e(TAG, "Did not find remoteAddress {" + ipAddress + "} in /proc/net/arp");
                        }
                        reader.close();
                    } catch (FileNotFoundException e) {
                        Log.e(TAG, "Could not open /proc/net/arp to lookup mac address");
                        if (reader != null) {
                            reader.close();
                        }
                    }
                } catch (IOException e2) {
                    Log.e(TAG, "Could not read /proc/net/arp to lookup mac address");
                    if (reader != null) {
                        reader.close();
                    }
                }
            } catch (IOException e3) {
            }
            return macAddress;
        } catch (Throwable th) {
            if (0 != 0) {
                try {
                    reader.close();
                } catch (IOException e4) {
                }
            }
            throw th;
        }
    }

    private void checkIsMultiGwFind() {
        ArrayList<byte[]> arrayList = this.mGatewayMacList;
        if (arrayList != null && arrayList.size() >= 2) {
            Log.d(TAG, "MultiGateway detected!");
            this.isMultiGwFind = true;
            this.msgHandler.sendEmptyMessageDelayed(101, ProcessStarter.REDUCE_DIED_PROC_COUNT_DELAY);
            for (int i = 0; i < this.mGatewayMacList.size(); i++) {
                Log.d(TAG, "mGatewayMacList[" + i + "] : " + hidenPrivateInfo(HexDump.toHexString(this.mGatewayMacList.get(i))) + " ,size:" + this.mGatewayMacList.size());
            }
            Intent intent = new Intent(MULTI_GATEWAY_DETECT_STATE_CHANGED);
            intent.putExtra(EXTRA_ARP_DETECT_GATEWAY_SIZE, this.mGatewayMacList.size());
            this.mContext.sendBroadcastAsUser(intent, UserHandle.SYSTEM);
        }
    }

    private boolean handlePacket() {
        ArrayList<byte[]> arrayList;
        byte[] mPacket = new byte[MAX_PACKET_LEN];
        try {
            int length = Os.read(this.mArpRecvSock, mPacket, 0, mPacket.length);
            ArpPacket packet = ArpPacket.parseArpPacket(mPacket, length);
            if (packet != null && packet.mOpCode == 2) {
                logd("Received Arp response from IP " + hidenPrivateInfo(packet.mSenderIp.getHostAddress()) + ", mac " + hidenPrivateInfo(HexDump.toHexString(packet.mSenderHwAddress)) + " --> target IP " + hidenPrivateInfo(packet.mTargetIp.getHostAddress()) + ", mac " + hidenPrivateInfo(HexDump.toHexString(packet.mTtargetHwAddress)));
                if (packet.mSenderIp.equals(this.mGatewayAddress) && !Arrays.equals(packet.mSenderHwAddress, this.mLocalMacAddress) && !isGatewayMacSaved(packet.mSenderHwAddress) && (arrayList = this.mGatewayMacList) != null && arrayList.size() < 4) {
                    this.mGatewayMacList.add(packet.mSenderHwAddress);
                    checkIsMultiGwFind();
                    return true;
                }
                return true;
            }
            return true;
        } catch (ErrnoException | InterruptedIOException e) {
            e.printStackTrace();
            Log.e(TAG, "handlePacket error");
            return false;
        } catch (ArpPacket.ParseException e2) {
            Log.e(TAG, "Can't parse ARP packet: ", e2);
            return true;
        }
    }

    private boolean registerArpRecvQueue() {
        Log.d(TAG, "enter registerArpRecvQueue");
        this.stopDetect = false;
        if (!initSocket()) {
            return false;
        }
        Handler handler = new Handler();
        this.mHandler = handler;
        MessageQueue queue = handler.getLooper().getQueue();
        this.mQueue = queue;
        queue.addOnFileDescriptorEventListener(this.mArpRecvSock, 5, new MessageQueue.OnFileDescriptorEventListener() { // from class: com.android.server.wifi.ArpDetect$$ExternalSyntheticLambda0
            @Override // android.os.MessageQueue.OnFileDescriptorEventListener
            public final int onFileDescriptorEvents(FileDescriptor fileDescriptor, int i) {
                return ArpDetect.this.m1461lambda$registerArpRecvQueue$0$comandroidserverwifiArpDetect(fileDescriptor, i);
            }
        });
        return true;
    }

    /* renamed from: lambda$registerArpRecvQueue$0$com-android-server-wifi-ArpDetect */
    public /* synthetic */ int m1461lambda$registerArpRecvQueue$0$comandroidserverwifiArpDetect(FileDescriptor fd, int events) {
        if (this.stopDetect || !handlePacket()) {
            unregisterArpRecvQueue();
            return 0;
        }
        return 5;
    }

    public void unregisterArpRecvQueue() {
        FileDescriptor fileDescriptor;
        MessageHandler messageHandler = this.msgHandler;
        if (messageHandler != null) {
            messageHandler.removeMessages(100);
        }
        MessageQueue messageQueue = this.mQueue;
        if (messageQueue != null && (fileDescriptor = this.mArpRecvSock) != null) {
            messageQueue.removeOnFileDescriptorEventListener(fileDescriptor);
            Log.d(TAG, "removed socket listen");
        }
        closeSocket(this.mArpRecvSock);
        this.mArpRecvSock = null;
        this.mQueue = null;
        this.mHandler = null;
        this.stopDetect = true;
    }

    private void closeSocket(FileDescriptor fd) {
        if (fd != null) {
            try {
                SocketUtils.closeSocket(fd);
            } catch (IOException e) {
                Log.e(TAG, "close socket fail");
            }
        }
    }

    public void setMultiGwRecoveryState(int state) {
        MessageHandler messageHandler;
        if (state == this.mCurRecoveryState) {
            return;
        }
        this.mCurRecoveryState = state;
        if (state == STATE_MULTI_GW_RECOVERY_STOPED && (messageHandler = this.msgHandler) != null) {
            messageHandler.removeMessages(101);
        }
    }

    /* loaded from: classes.dex */
    public class MessageHandler extends Handler {
        MessageHandler() {
            ArpDetect.this = this$0;
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 100:
                    ArpDetect.this.logd("stop recv arp packet");
                    ArpDetect.this.unregisterArpRecvQueue();
                    return;
                case 101:
                    ArpDetect.this.resetGatewayNeighState();
                    ArpDetect.this.setMultiGwRecoveryState(ArpDetect.STATE_MULTI_GW_RECOVERY_STOPED);
                    return;
                default:
                    return;
            }
        }
    }
}
