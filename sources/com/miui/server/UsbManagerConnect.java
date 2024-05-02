package com.miui.server;

import android.net.LocalSocket;
import android.util.Slog;
import java.io.IOException;
import java.io.OutputStream;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public abstract class UsbManagerConnect implements Runnable {
    static final int BUFFER_SIZE = 4096;
    private static final String TAG = "UsbManagerConnect";
    OutputStream mOutputStream;
    LocalSocket mSocket;
    protected final int MSG_TO_PC = 0;
    protected final int MSG_LOCAL = 1;
    protected final int MSG_SHARE_NET = 2;

    abstract void listenToSocket() throws IOException;

    public synchronized void closeSocket() {
        OutputStream outputStream = this.mOutputStream;
        if (outputStream != null) {
            try {
                outputStream.close();
            } catch (IOException ex) {
                Slog.e(TAG, "Failed closing output stream: " + ex);
            }
        }
        LocalSocket localSocket = this.mSocket;
        if (localSocket != null) {
            try {
                localSocket.close();
            } catch (IOException ex2) {
                Slog.e(TAG, "Failed closing socket: " + ex2);
            }
        }
    }

    public synchronized void sendResponse(String msg) {
        OutputStream outputStream = this.mOutputStream;
        if (outputStream != null) {
            try {
                outputStream.write(msg.getBytes());
            } catch (IOException ex) {
                Slog.e(TAG, "Failed to write response:", ex);
            }
        }
    }

    synchronized void sendResponse(int msgType, int msgId, byte[] byteMsg) {
        OutputStream outputStream = this.mOutputStream;
        if (outputStream != null && byteMsg != null) {
            try {
                outputStream.write(String.format("%04x", Integer.valueOf(msgId)).getBytes());
                this.mOutputStream.write(String.format("%04x", Integer.valueOf(msgType)).getBytes());
                this.mOutputStream.write(String.format("%08x", Integer.valueOf(byteMsg.length)).getBytes());
                this.mOutputStream.write(byteMsg);
                this.mOutputStream.flush();
            } catch (IOException ex) {
                Slog.e(TAG, "Failed to write response:", ex);
            }
        }
    }

    String getErrMsg(String functionName, String reason) {
        return "FAIL, function name: " + functionName + ", reason: " + reason;
    }

    byte[] getLargeMsg(byte[] msg) {
        byte[] msgLenByte = String.format("%08x", Integer.valueOf(msg.length)).getBytes();
        byte[] largeMsg = new byte[msgLenByte.length + msg.length];
        System.arraycopy(msgLenByte, 0, largeMsg, 0, msgLenByte.length);
        int startPos = 0 + msgLenByte.length;
        System.arraycopy(msg, 0, largeMsg, startPos, msg.length);
        return largeMsg;
    }
}
