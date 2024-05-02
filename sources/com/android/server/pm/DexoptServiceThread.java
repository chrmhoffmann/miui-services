package com.android.server.pm;

import android.content.pm.ApplicationInfo;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Slog;
import com.android.server.pm.dex.DexoptOptions;
import com.android.server.pm.dex.PackageDexUsage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
/* loaded from: classes.dex */
public class DexoptServiceThread extends Thread {
    public static final String HOST_NAME = "mi_dexopt";
    public static final int SOCKET_BUFFER_SIZE = 256;
    public static final String TAG = "DexoptServiceThread";
    private static final long WAIT_DEXOPT_TIMEOUT = 180000;
    private static volatile Handler sAsynDexOptHandler;
    private static HandlerThread sAsynDexOptThread;
    private DexOptHelper mDexOptHelper;
    private PackageDexOptimizer mPdo;
    private PackageManagerService mPms;
    private static Object mWaitLock = new Object();
    private static Object sAsynDexOptLock = new Object();
    public int secondaryId = 0;
    private int mDexoptResult = 0;
    private List<String> mDexoptPackageNameList = new ArrayList();
    public Map<Integer, String> mDexoptSecondaryPath = new HashMap();
    public Map<String, Integer> mSecondaryMap = new HashMap();
    private int mDexoptSecondaryResult = 0;

    public DexoptServiceThread(PackageManagerService pms, PackageDexOptimizer pdo) {
        this.mPms = pms;
        this.mPdo = pdo;
        this.mDexOptHelper = new DexOptHelper(this.mPms);
    }

    @Override // java.lang.Thread, java.lang.Runnable
    public void run() {
        LocalServerSocket serverSocket = null;
        ExecutorService threadExecutor = Executors.newCachedThreadPool();
        try {
            try {
                Slog.w(TAG, "Create local socket: mi_dexopt");
                serverSocket = new LocalServerSocket(HOST_NAME);
                while (true) {
                    Slog.i(TAG, "Waiting dexopt client connected...");
                    LocalSocket clientSocket = serverSocket.accept();
                    clientSocket.setReceiveBufferSize(256);
                    clientSocket.setSendBufferSize(256);
                    Slog.i(TAG, "There is a dexopt client is accepted:" + clientSocket.toString());
                    threadExecutor.execute(new ConnectionHandler(clientSocket));
                }
            } catch (Exception e) {
                Slog.e(TAG, "mi_dexopt connection catch Exception: " + e);
                Slog.w(TAG, "mi_dexopt connection finally shutdown.");
                if (threadExecutor != null) {
                    threadExecutor.shutdown();
                }
                if (serverSocket != null) {
                    try {
                        serverSocket.close();
                    } catch (IOException e2) {
                        e2.printStackTrace();
                    }
                }
            }
        } catch (Throwable th) {
            Slog.w(TAG, "mi_dexopt connection finally shutdown.");
            if (threadExecutor != null) {
                threadExecutor.shutdown();
            }
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException e3) {
                    e3.printStackTrace();
                }
            }
            throw th;
        }
    }

    /* loaded from: classes.dex */
    private class ConnectionHandler implements Runnable {
        private LocalSocket mSocket;
        private boolean mIsContinue = true;
        private InputStreamReader mInput = null;

        public ConnectionHandler(LocalSocket clientSocket) {
            DexoptServiceThread.this = r1;
            this.mSocket = clientSocket;
        }

        public void terminate() {
            Slog.d(DexoptServiceThread.TAG, "dexopt trigger terminate!");
            this.mIsContinue = false;
        }

        @Override // java.lang.Runnable
        public void run() {
            int result;
            Slog.i(DexoptServiceThread.TAG, "mi_dexopt new connection:" + this.mSocket.toString());
            BufferedReader bufferedReader = null;
            while (this.mIsContinue) {
                try {
                    try {
                        try {
                            try {
                                this.mInput = new InputStreamReader(this.mSocket.getInputStream());
                                bufferedReader = new BufferedReader(this.mInput);
                                String data = bufferedReader.readLine();
                                String[] temp = data.split(":");
                                int len = temp.length;
                                int secondaryId = 0;
                                String packageName = temp[0];
                                if (len == 3) {
                                    secondaryId = Integer.parseInt(temp[1].trim());
                                    result = Integer.parseInt(temp[2].trim());
                                } else {
                                    result = Integer.parseInt(temp[1].trim());
                                }
                                synchronized (DexoptServiceThread.mWaitLock) {
                                    if (packageName == null) {
                                        try {
                                            Slog.e(DexoptServiceThread.TAG, "Received packageName error");
                                        } catch (Throwable th) {
                                            throw th;
                                        }
                                    } else if (secondaryId == 0) {
                                        if (DexoptServiceThread.this.mDexoptPackageNameList.contains(packageName)) {
                                            DexoptServiceThread.this.mDexoptResult = result;
                                            DexoptServiceThread.this.mDexoptPackageNameList.remove(packageName);
                                            Slog.d(DexoptServiceThread.TAG, "packageName : " + packageName + " result : " + DexoptServiceThread.this.mDexoptResult + " finished, notify next.");
                                            DexoptServiceThread.mWaitLock.notifyAll();
                                        }
                                    } else if (secondaryId > 0 && DexoptServiceThread.this.mDexoptSecondaryPath.containsKey(Integer.valueOf(secondaryId))) {
                                        DexoptServiceThread.this.mDexoptSecondaryResult = result;
                                        DexoptServiceThread.this.mDexoptSecondaryPath.remove(Integer.valueOf(secondaryId));
                                        Slog.d(DexoptServiceThread.TAG, "packageName : " + packageName + " secondaryId : " + secondaryId + " result : " + DexoptServiceThread.this.mDexoptSecondaryResult + " finished, notify next.");
                                        DexoptServiceThread.mWaitLock.notifyAll();
                                    }
                                }
                            } catch (Throwable th2) {
                                if (bufferedReader != null) {
                                    try {
                                        bufferedReader.close();
                                    } catch (IOException e) {
                                    }
                                }
                                throw th2;
                            }
                        } catch (IOException e2) {
                            Slog.e(DexoptServiceThread.TAG, "mi_dexopt connection:" + this.mSocket.toString() + " IOException");
                            if (bufferedReader == null) {
                                return;
                            }
                            bufferedReader.close();
                        }
                    } catch (IOException e3) {
                        return;
                    }
                } catch (Exception e4) {
                    terminate();
                    Slog.e(DexoptServiceThread.TAG, "mi_dexopt connection:" + this.mSocket.toString() + " Exception");
                    if (bufferedReader == null) {
                        return;
                    }
                    bufferedReader.close();
                }
            }
            if (bufferedReader != null) {
                bufferedReader.close();
            }
        }
    }

    private static Handler getAsynDexOptHandler() {
        if (sAsynDexOptHandler == null) {
            synchronized (sAsynDexOptLock) {
                if (sAsynDexOptHandler == null) {
                    HandlerThread handlerThread = new HandlerThread("asyn_dexopt_thread");
                    sAsynDexOptThread = handlerThread;
                    handlerThread.start();
                    sAsynDexOptHandler = new Handler(sAsynDexOptThread.getLooper());
                }
            }
        }
        return sAsynDexOptHandler;
    }

    private void waitDexOptRequest() {
        synchronized (mWaitLock) {
            Slog.d(TAG, "Start to wait dexopt result.");
            long startTime = System.currentTimeMillis();
            try {
                mWaitLock.wait(WAIT_DEXOPT_TIMEOUT);
            } catch (InterruptedException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }
            long endTime = System.currentTimeMillis();
            Slog.d(TAG, "Dexopt finished, waiting time(" + (endTime - startTime) + "ms), result:" + this.mDexoptResult);
        }
    }

    public void performDexOptAsyncTask(final DexoptOptions options) {
        Runnable dexoptTask = new Runnable() { // from class: com.android.server.pm.DexoptServiceThread.1
            @Override // java.lang.Runnable
            public void run() {
                String dexoptPackagename = options.getPackageName();
                if (!DexoptServiceThread.this.mDexoptPackageNameList.contains(dexoptPackagename)) {
                    DexoptServiceThread.this.mDexoptPackageNameList.add(dexoptPackagename);
                }
                DexoptServiceThread dexoptServiceThread = DexoptServiceThread.this;
                dexoptServiceThread.mDexoptResult = dexoptServiceThread.performDexOptInternal(options);
                if (DexoptServiceThread.this.mDexoptResult != 1) {
                    synchronized (DexoptServiceThread.mWaitLock) {
                        DexoptServiceThread.this.mDexoptPackageNameList.remove(dexoptPackagename);
                        DexoptServiceThread.mWaitLock.notifyAll();
                    }
                }
            }
        };
        getAsynDexOptHandler().post(dexoptTask);
        waitDexOptRequest();
    }

    public int performDexOptInternal(DexoptOptions options) {
        try {
            Method declaredMethod = this.mDexOptHelper.getClass().getDeclaredMethod("performDexOptInternal", DexoptOptions.class);
            declaredMethod.setAccessible(true);
            return ((Integer) declaredMethod.invoke(this.mDexOptHelper, options)).intValue();
        } catch (Exception e) {
            Slog.w(TAG, "Exception: " + e);
            return 0;
        }
    }

    public void performDexOptSecondary(final ApplicationInfo info, final String path, final PackageDexUsage.DexUseInfo dexUseInfo, final DexoptOptions options) {
        Runnable task = new Runnable() { // from class: com.android.server.pm.DexoptServiceThread.2
            @Override // java.lang.Runnable
            public void run() {
                DexoptServiceThread.this.secondaryId++;
                if (!DexoptServiceThread.this.mDexoptSecondaryPath.containsValue(path)) {
                    DexoptServiceThread.this.mDexoptSecondaryPath.put(Integer.valueOf(DexoptServiceThread.this.secondaryId), path);
                } else {
                    for (Integer num : DexoptServiceThread.this.mDexoptSecondaryPath.keySet()) {
                        int getSecondaryId = num.intValue();
                        if (DexoptServiceThread.this.mDexoptSecondaryPath.get(Integer.valueOf(getSecondaryId)).equals(path)) {
                            DexoptServiceThread.this.secondaryId = getSecondaryId;
                        }
                    }
                }
                DexoptServiceThread dexoptServiceThread = DexoptServiceThread.this;
                dexoptServiceThread.mDexoptSecondaryResult = dexoptServiceThread.dexOptSecondaryDexPath(info, path, dexoptServiceThread.secondaryId, dexUseInfo, options);
                if (DexoptServiceThread.this.mDexoptResult != 1) {
                    synchronized (DexoptServiceThread.mWaitLock) {
                        DexoptServiceThread.this.mDexoptSecondaryPath.remove(Integer.valueOf(DexoptServiceThread.this.secondaryId));
                        DexoptServiceThread.mWaitLock.notifyAll();
                    }
                }
            }
        };
        getAsynDexOptHandler().post(task);
        waitDexOptRequest();
    }

    public int dexOptSecondaryDexPath(ApplicationInfo info, String path, int secondaryId, PackageDexUsage.DexUseInfo dexUseInfo, DexoptOptions options) {
        Class[] arg = {ApplicationInfo.class, String.class, Integer.TYPE, PackageDexUsage.DexUseInfo.class, DexoptOptions.class};
        try {
            Method declaredMethod = this.mPdo.getClass().getDeclaredMethod("dexOptSecondaryDexPath", arg);
            declaredMethod.setAccessible(true);
            return ((Integer) declaredMethod.invoke(this.mPdo, info, path, Integer.valueOf(secondaryId), dexUseInfo, options)).intValue();
        } catch (Exception e) {
            Slog.w(TAG, "Exception: " + e);
            return 0;
        }
    }

    public int getDexoptSecondaryResult() {
        return this.mDexoptSecondaryResult;
    }

    public int getDexOptResult() {
        return this.mDexoptResult;
    }
}
