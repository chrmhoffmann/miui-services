package com.android.net.module.util.netlink;

import java.nio.ByteBuffer;
/* loaded from: classes.dex */
public class NetlinkErrorMessage extends NetlinkMessage {
    private StructNlMsgErr mNlMsgErr = null;

    public static NetlinkErrorMessage parse(StructNlMsgHdr header, ByteBuffer byteBuffer) {
        NetlinkErrorMessage errorMsg = new NetlinkErrorMessage(header);
        StructNlMsgErr parse = StructNlMsgErr.parse(byteBuffer);
        errorMsg.mNlMsgErr = parse;
        if (parse == null) {
            return null;
        }
        return errorMsg;
    }

    NetlinkErrorMessage(StructNlMsgHdr header) {
        super(header);
    }

    public StructNlMsgErr getNlMsgError() {
        return this.mNlMsgErr;
    }

    @Override // com.android.net.module.util.netlink.NetlinkMessage
    public String toString() {
        String str = "";
        StringBuilder append = new StringBuilder().append("NetlinkErrorMessage{ nlmsghdr{").append(this.mHeader == null ? str : this.mHeader.toString()).append("}, nlmsgerr{");
        StructNlMsgErr structNlMsgErr = this.mNlMsgErr;
        if (structNlMsgErr != null) {
            str = structNlMsgErr.toString();
        }
        return append.append(str).append("} }").toString();
    }
}
