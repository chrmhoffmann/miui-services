package com.miui.server.sentinel;

import android.content.Context;
import android.content.res.Resources;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.Binder;
import android.os.SystemProperties;
import android.util.Slog;
import com.android.server.audio.AudioServiceStubImpl;
import java.io.BufferedInputStream;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
/* loaded from: classes.dex */
public class MiuiSentinelService extends Binder {
    public static final String HOST_NAME = "data/localsocket/prochunter_native_socket";
    public static final int MAX_BUFF_SIZE = 9000;
    public static final int REPORT_NATIVEHEAP_LEAKTOMQS = 6;
    public static final String SERVICE_NAME = "miui.sentinel.service";
    private static final String TAG = "MiuiSentinelService";
    private Context mContext;
    public static final boolean DEBUG = SystemProperties.getBoolean("debug.sys.mss", false);
    private static final HashMap<String, Integer> APP_JAVAHEAP_WHITE_LIST = new HashMap<>();
    private static final HashMap<String, Integer> APP_NATIVEHEAP_WHITE_LIST = new HashMap<>();

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class MiuiSentinelServiceThread extends Thread {
        private MiuiSentinelServiceThread() {
            MiuiSentinelService.this = r1;
        }

        @Override // java.lang.Thread, java.lang.Runnable
        public void run() {
            try {
                LocalSocket localSocket = MiuiSentinelService.this.createSystemServerSocketForProchunter();
                if (localSocket != null) {
                    Slog.e(MiuiSentinelService.TAG, "local socekt fd is:" + localSocket.getFileDescriptor());
                    while (true) {
                        MiuiSentinelService.this.recvMessage(localSocket.getFileDescriptor());
                    }
                }
            } catch (Exception e) {
                Slog.e(MiuiSentinelService.TAG, "prochunter_native_sockets connection catch Exception");
                e.printStackTrace();
            }
        }
    }

    private void handlerTrigger(SocketPacket socketPacket) {
        int type = MiuiSentinelEvent.getEventType(socketPacket.getEvent_type());
        switch (type) {
            case 1:
            case 4:
            case 5:
            case 6:
            case 8:
            case 9:
            case 10:
            case 11:
            case 13:
                return;
            case 2:
                Slog.d(TAG, "begin judgmentNativeHeapLeakException");
                MiuiSentinelMemoryManager.getInstance().judgmentNativeHeapLeakException(socketPacket);
                return;
            case 3:
                Slog.d(TAG, "begin judgmentJavaHeapLeakException");
                MiuiSentinelMemoryManager.getInstance().judgmentJavaHeapLeakException(socketPacket);
                return;
            case 7:
                Slog.d(TAG, "begin judgmentRssLeakException");
                MiuiSentinelMemoryManager.getInstance().judgmentRssLeakException(socketPacket);
                return;
            case 12:
            default:
                Slog.e(TAG, "receive invalid event");
                return;
        }
    }

    private void handlerTrackMessage(TrackPacket trackPacket) {
        try {
            MiuiSentinelMemoryManager.getInstance().outPutTrackLog(trackPacket);
        } catch (IOException e) {
            Slog.e(TAG, "Track stack output to file failed", new Throwable());
        }
        ConcurrentHashMap<String, NativeHeapUsageInfo> tracklist = MiuiSentinelMemoryManager.getInstance().getTrackList();
        String key = trackPacket.getPid() + "#" + trackPacket.getProcess_name();
        NativeHeapUsageInfo nativeHeapUsageInfo = tracklist.get(key);
        if (nativeHeapUsageInfo != null) {
            nativeHeapUsageInfo.setStackTrace(trackPacket.getData());
            tracklist.remove(key);
            MiuiSentinelMemoryManager.getInstance().sendMessage(nativeHeapUsageInfo, 6);
        }
    }

    private void initWhilteList(Context context) {
        Resources r = context.getResources();
        String[] javaheaps = r.getStringArray(285409293);
        String[] nativeheaps = r.getStringArray(285409294);
        if (javaheaps == null || javaheaps.length == 0 || nativeheaps == null || nativeheaps.length == 0) {
            Slog.e(TAG, "initwhileList is failed");
        }
        for (String javaheap : javaheaps) {
            String[] split = javaheap.split(",");
            Integer threshold = Integer.valueOf(Integer.parseInt(split[1]));
            APP_JAVAHEAP_WHITE_LIST.put(split[0], threshold);
        }
        for (String nativeheap : nativeheaps) {
            String[] split2 = nativeheap.split(",");
            Integer threshold2 = Integer.valueOf(Integer.parseInt(split2[1]));
            APP_NATIVEHEAP_WHITE_LIST.put(split2[0], threshold2);
        }
    }

    public MiuiSentinelService(Context context) {
        this.mContext = null;
        this.mContext = context;
        initWhilteList(context);
        MiuiSentinelServiceThread miuiSentinelServiceThread = new MiuiSentinelServiceThread();
        miuiSentinelServiceThread.start();
    }

    public static HashMap<String, Integer> getAppNativeheapWhiteList() {
        return APP_NATIVEHEAP_WHITE_LIST;
    }

    public static HashMap<String, Integer> getAppJavaheapWhiteList() {
        return APP_JAVAHEAP_WHITE_LIST;
    }

    public LocalSocket createSystemServerSocketForProchunter() {
        LocalSocket serverSocket = null;
        try {
            serverSocket = new LocalSocket(1);
            serverSocket.bind(new LocalSocketAddress(HOST_NAME, LocalSocketAddress.Namespace.ABSTRACT));
            serverSocket.setSendBufferSize(AudioServiceStubImpl.READ_MUSIC_PLAY_BACK_DELAY);
            serverSocket.setReceiveBufferSize(AudioServiceStubImpl.READ_MUSIC_PLAY_BACK_DELAY);
            Slog.e(TAG, "prochunter socket create success");
            return serverSocket;
        } catch (Exception e) {
            e.printStackTrace();
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException e2) {
                }
                return null;
            }
            return serverSocket;
        }
    }

    public void recvMessage(FileDescriptor fd) {
        if (DEBUG) {
            Slog.e(TAG, "begin recv message");
        }
        BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(fd));
        byte[] splitbuffer = new byte[8];
        try {
            bufferedInputStream.read(splitbuffer, 0, 4);
            int type = SocketPacket.readInt(splitbuffer);
            if (type > 10) {
                SocketPacket socketPacket = parseSocketPacket(bufferedInputStream);
                if (MiuiSentinelMemoryManager.getInstance().filterMessages(socketPacket)) {
                    handlerTrigger(socketPacket);
                }
                return;
            }
            TrackPacket trackPacket = parseTrackPacket(bufferedInputStream);
            handlerTrackMessage(trackPacket);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e2) {
            e2.printStackTrace();
        }
    }

    private SocketPacket parseSocketPacket(BufferedInputStream bufferedInputStream) {
        SocketPacket socketPacket = new SocketPacket();
        StringBuilder sb = new StringBuilder();
        byte[] databuff = new byte[MAX_BUFF_SIZE];
        byte[] splitbuffer = new byte[64];
        try {
            bufferedInputStream.read(splitbuffer, 0, 4);
            socketPacket.setPid(SocketPacket.readInt(splitbuffer));
            bufferedInputStream.read(splitbuffer, 0, 8);
            socketPacket.setGrowsize(SocketPacket.readLong(splitbuffer));
            bufferedInputStream.read(splitbuffer, 0, 30);
            socketPacket.setEvent_type(SocketPacket.readString(splitbuffer, 0, 30));
            bufferedInputStream.read(splitbuffer, 0, 64);
            socketPacket.setProcess_name(SocketPacket.readString(splitbuffer, 0, 64));
            while (bufferedInputStream.available() > 0) {
                int readsize = bufferedInputStream.read(databuff);
                if (readsize == 9000) {
                    sb.append(SocketPacket.readString(databuff, 0, MAX_BUFF_SIZE));
                } else {
                    sb.append(SocketPacket.readString(databuff, 0, readsize));
                }
            }
            socketPacket.setData(sb.toString());
            if (DEBUG) {
                Slog.e(TAG, "SocketPacket data = " + socketPacket.toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e2) {
            e2.printStackTrace();
        }
        return socketPacket;
    }

    private TrackPacket parseTrackPacket(BufferedInputStream bufferedInputStream) {
        TrackPacket trackPacket = new TrackPacket();
        StringBuilder sb = new StringBuilder();
        byte[] databuff = new byte[MAX_BUFF_SIZE];
        byte[] splitbuffer = new byte[64];
        try {
            bufferedInputStream.read(splitbuffer, 0, 4);
            trackPacket.setReport_id(SocketPacket.readInt(splitbuffer));
            bufferedInputStream.read(splitbuffer, 0, 4);
            trackPacket.setReport_argsz(SocketPacket.readInt(splitbuffer));
            bufferedInputStream.read(splitbuffer, 0, 64);
            trackPacket.setProcess_name(SocketPacket.readString(splitbuffer, 0, 64));
            bufferedInputStream.read(splitbuffer, 0, 4);
            trackPacket.setPid(SocketPacket.readInt(splitbuffer));
            bufferedInputStream.read(splitbuffer, 0, 4);
            trackPacket.setTid(SocketPacket.readInt(splitbuffer));
            bufferedInputStream.read(splitbuffer, 0, 12);
            trackPacket.setTimestamp(SocketPacket.readInt(splitbuffer));
            bufferedInputStream.read(splitbuffer, 0, 8);
            trackPacket.setReport_sz(SocketPacket.readInt(splitbuffer));
            while (bufferedInputStream.available() > 0) {
                int readsize = bufferedInputStream.read(databuff);
                if (readsize == 9000) {
                    sb.append(SocketPacket.readString(databuff, 0, MAX_BUFF_SIZE));
                } else {
                    sb.append(SocketPacket.readString(databuff, 0, readsize));
                }
            }
            trackPacket.setData(sb.toString());
            if (DEBUG) {
                Slog.e(TAG, "Track stack: " + trackPacket.getData());
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e2) {
            e2.printStackTrace();
        }
        return trackPacket;
    }
}
