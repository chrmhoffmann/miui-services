package com.android.net.module.util.netlink;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
/* loaded from: classes.dex */
public class StructInetDiagReqV2 {
    public static final int INET_DIAG_REQ_V2_ALL_STATES = -1;
    public static final int STRUCT_SIZE = 56;
    private final StructInetDiagSockId mId;
    private final byte mIdiagExt;
    private final byte mPad;
    private final byte mSdiagFamily;
    private final byte mSdiagProtocol;
    private final int mState;

    public StructInetDiagReqV2(int protocol, InetSocketAddress local, InetSocketAddress remote, int family) {
        this(protocol, local, remote, family, 0, 0, -1);
    }

    public StructInetDiagReqV2(int protocol, InetSocketAddress local, InetSocketAddress remote, int family, int pad, int extension, int state) throws NullPointerException {
        this.mSdiagFamily = (byte) family;
        this.mSdiagProtocol = (byte) protocol;
        if ((local == null) != (remote != null ? false : true)) {
            throw new NullPointerException("Local and remote must be both null or both non-null");
        }
        this.mId = (local == null || remote == null) ? null : new StructInetDiagSockId(local, remote);
        this.mPad = (byte) pad;
        this.mIdiagExt = (byte) extension;
        this.mState = state;
    }

    public void pack(ByteBuffer byteBuffer) {
        byteBuffer.put(this.mSdiagFamily);
        byteBuffer.put(this.mSdiagProtocol);
        byteBuffer.put(this.mIdiagExt);
        byteBuffer.put(this.mPad);
        byteBuffer.putInt(this.mState);
        StructInetDiagSockId structInetDiagSockId = this.mId;
        if (structInetDiagSockId != null) {
            structInetDiagSockId.pack(byteBuffer);
        }
    }

    public String toString() {
        String familyStr = NetlinkConstants.stringForAddressFamily(this.mSdiagFamily);
        String protocolStr = NetlinkConstants.stringForAddressFamily(this.mSdiagProtocol);
        StringBuilder append = new StringBuilder().append("StructInetDiagReqV2{ sdiag_family{").append(familyStr).append("}, sdiag_protocol{").append(protocolStr).append("}, idiag_ext{").append((int) this.mIdiagExt).append(")}, pad{").append((int) this.mPad).append("}, idiag_states{").append(Integer.toHexString(this.mState)).append("}, ");
        StructInetDiagSockId structInetDiagSockId = this.mId;
        return append.append(structInetDiagSockId != null ? structInetDiagSockId.toString() : "inet_diag_sockid=null").append("}").toString();
    }
}
