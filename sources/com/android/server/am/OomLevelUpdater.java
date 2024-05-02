package com.android.server.am;

import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.SystemProperties;
import android.util.Slog;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import miui.os.Build;
/* loaded from: classes.dex */
class OomLevelUpdater {
    private static final String CAMERA_LEVEL = "18432:0,23040:100,27648:200,32256:250,276480:900,362880:950";
    private static final String CAMERA_SECOND_LEVEL = "18432:0,23040:100,27648:200,125000:300,276480:900,362880:950";
    static final byte LMK_TARGET = 0;
    private static final String TAG = "OomLevelUpdater";
    private OutputStream sLmkdOutputStream;
    private LocalSocket sLmkdSocket;
    private final Object sLock = new Object();
    private static final String DEVICE = Build.DEVICE.toLowerCase();
    private static final String CAMERA_PROP_LEVEL = SystemProperties.get("ro.lmk.camera.level", "");
    private static final String DEFAULT_LEVEL = SystemProperties.get("sys.lmk.minfree_levels", "");

    OomLevelUpdater() {
    }

    private boolean openLmkdSocket() {
        if (this.sLmkdSocket != null) {
            return true;
        }
        try {
            LocalSocket localSocket = new LocalSocket(3);
            this.sLmkdSocket = localSocket;
            localSocket.connect(new LocalSocketAddress("lmkd", LocalSocketAddress.Namespace.RESERVED));
            this.sLmkdOutputStream = this.sLmkdSocket.getOutputStream();
            return this.sLmkdSocket != null;
        } catch (IOException e) {
            Slog.e(TAG, "lmk socket open failed");
            this.sLmkdSocket = null;
            return false;
        }
    }

    private void writeLmkd(ByteBuffer buf) {
        synchronized (this.sLock) {
            if (this.sLmkdSocket == null) {
                return;
            }
            try {
                this.sLmkdOutputStream.write(buf.array(), 0, buf.position());
            } catch (IOException e) {
                Slog.w(TAG, "Error writing to lowmemorykiller socket");
                try {
                    this.sLmkdSocket.close();
                } catch (IOException e2) {
                    Slog.e(TAG, "Error close to lowmemorykiller socket");
                }
                this.sLmkdSocket = null;
            }
        }
    }

    void updateOOMLevel(boolean isCameraForeground) {
        String cameraLevel;
        synchronized (this.sLock) {
            if (this.sLmkdSocket == null && !openLmkdSocket()) {
                Slog.w(TAG, "fail to update OOM Level for open socket fail");
                return;
            }
            Slog.i(TAG, "updateOOMLevel: E " + isCameraForeground);
            String minfreeLevel = DEFAULT_LEVEL;
            if ("".equals(minfreeLevel)) {
                return;
            }
            if (DEVICE.equals("cmi")) {
                cameraLevel = CAMERA_PROP_LEVEL;
                if (cameraLevel.equals("")) {
                    cameraLevel = CAMERA_SECOND_LEVEL;
                }
            } else {
                cameraLevel = CAMERA_PROP_LEVEL;
                if (cameraLevel.equals("")) {
                    cameraLevel = CAMERA_LEVEL;
                }
            }
            if (isCameraForeground) {
                minfreeLevel = cameraLevel;
            }
            String[] adjLevel = minfreeLevel.split(",");
            if (adjLevel.length == 0) {
                return;
            }
            ByteBuffer buf = ByteBuffer.allocate(((adjLevel.length * 2) + 1) * 4);
            buf.putInt(0);
            for (String str : adjLevel) {
                String[] temp = str.split(":");
                buf.putInt(Integer.parseInt(temp[0]));
                buf.putInt(Integer.parseInt(temp[1]));
            }
            writeLmkd(buf);
            Slog.i(TAG, "updateOOMLevel: X");
        }
    }
}
