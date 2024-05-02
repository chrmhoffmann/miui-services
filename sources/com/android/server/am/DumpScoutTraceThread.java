package com.android.server.am;

import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.os.FileUtils;
import android.util.Slog;
import com.android.server.ScoutHelper;
import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import miui.mqsas.scout.ScoutUtils;
import org.json.JSONException;
import org.json.JSONObject;
/* loaded from: classes.dex */
public class DumpScoutTraceThread extends Thread {
    private static final String APP_LOG_DIR = "/data/miuilog/stability/scout/app";
    private static int NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors();
    public static final int SOCKET_BUFFER_SIZE = 256;
    public static final String SOCKET_NAME = "scouttrace";
    public static final String TAG = "DumpScoutTraceThread";
    private ActivityManagerServiceImpl mAMSImpl;

    public DumpScoutTraceThread(String name, ActivityManagerServiceImpl AMSImpl) {
        super(name);
        this.mAMSImpl = AMSImpl;
    }

    @Override // java.lang.Thread, java.lang.Runnable
    public void run() {
        int[] pids;
        int i = NUMBER_OF_CORES;
        ThreadPoolExecutor threadPool = new ThreadPoolExecutor(i, i, 1L, TimeUnit.SECONDS, new LinkedBlockingQueue());
        threadPool.allowCoreThreadTimeOut(true);
        LocalServerSocket socketServer = null;
        try {
            try {
                socketServer = new LocalServerSocket(SOCKET_NAME);
                Slog.w(TAG, "Already create local socket");
                while (true) {
                    LocalSocket socketClient = socketServer.accept();
                    Slog.e(TAG, "waiting mqs client socket to connect...");
                    InputStream inputStream = socketClient.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                    String scoutInfo = reader.readLine();
                    if (scoutInfo != null) {
                        JSONObject jsonObject = new JSONObject(scoutInfo);
                        final int pid = Integer.parseInt(jsonObject.getString("pid"));
                        Slog.e(TAG, "get data from socket " + pid);
                        String pidlist = jsonObject.getString("pidlist");
                        if (pidlist.equals("null")) {
                            pids = null;
                        } else {
                            String[] pidArray = pidlist.split("#");
                            int[] pids2 = new int[pidArray.length];
                            for (int i2 = 0; i2 < pidArray.length; i2++) {
                                pids2[i2] = Integer.parseInt(pidArray[i2]);
                            }
                            pids = pids2;
                        }
                        final String path = jsonObject.getString("path");
                        final String timestamp = jsonObject.getString("timestamp");
                        final String fileNamePerfix = jsonObject.getString("fileNamePerfix");
                        final boolean isSystemBlock = Boolean.parseBoolean(jsonObject.getString("isSystemBlock"));
                        final int[] finalPids = pids;
                        threadPool.execute(new Runnable() { // from class: com.android.server.am.DumpScoutTraceThread$$ExternalSyntheticLambda0
                            @Override // java.lang.Runnable
                            public final void run() {
                                DumpScoutTraceThread.this.m361lambda$run$0$comandroidserveramDumpScoutTraceThread(isSystemBlock, fileNamePerfix, timestamp, path, pid, finalPids);
                            }
                        });
                    }
                }
            } catch (IOException | JSONException e) {
                Slog.w(TAG, "Exception creating scout dump scout trace file:", e);
                Slog.w(TAG, "DumpScoutTraceThread finally shutdown");
                threadPool.shutdown();
                if (socketServer != null) {
                    try {
                        socketServer.close();
                    } catch (IOException e2) {
                        e2.printStackTrace();
                    }
                }
            }
        } catch (Throwable e3) {
            Slog.w(TAG, "DumpScoutTraceThread finally shutdown");
            threadPool.shutdown();
            if (socketServer != null) {
                try {
                    socketServer.close();
                } catch (IOException e4) {
                    e4.printStackTrace();
                }
            }
            throw e3;
        }
    }

    /* renamed from: lambda$run$0$com-android-server-am-DumpScoutTraceThread */
    public /* synthetic */ void m361lambda$run$0$comandroidserveramDumpScoutTraceThread(boolean isSystemBlock, String fileNamePerfix, String timestamp, String path, int pid, int[] finalPids) {
        File[] pathArray;
        if (isSystemBlock) {
            String systemFileName = fileNamePerfix + timestamp + "-system_server-trace";
            String systemFilePath = path + "/" + systemFileName;
            this.mAMSImpl.dumpSystemTraces(systemFilePath);
        } else {
            Slog.w(TAG, "Don't need to dump system_server trace.");
        }
        boolean result = ScoutHelper.CheckDState(TAG, pid).booleanValue();
        String selfFileName = fileNamePerfix + timestamp + "-self-trace";
        String selfFilePath = path + "/" + selfFileName;
        File selfTraceFile = this.mAMSImpl.dumpOneProcessTraces(pid, selfFilePath, "App Scout Exception");
        if (selfTraceFile != null) {
            Slog.d(TAG, "Dump scout self trace file successfully!");
        } else {
            Slog.w(TAG, "Dump scout self trace file fail!");
        }
        if (finalPids != null && finalPids.length > 0) {
            for (int temppid : finalPids) {
                ScoutHelper.CheckDState(TAG, temppid);
            }
            String fileName = fileNamePerfix + timestamp + "-other-trace";
            String filePath = path + "/" + fileName;
            ArrayList<Integer> javapids = new ArrayList<>(3);
            ArrayList<Integer> nativePids = new ArrayList<>(3);
            int i = 0;
            while (true) {
                boolean result2 = result;
                if (i >= finalPids.length) {
                    break;
                }
                int adj = ScoutHelper.getOomAdjOfPid(TAG, finalPids[i]);
                String selfFileName2 = selfFileName;
                int isJavaOrNativeProcess = ScoutHelper.checkIsJavaOrNativeProcess(adj);
                if (isJavaOrNativeProcess != 0) {
                    if (isJavaOrNativeProcess == 1) {
                        javapids.add(Integer.valueOf(finalPids[i]));
                    } else if (isJavaOrNativeProcess == 2) {
                        nativePids.add(Integer.valueOf(finalPids[i]));
                    }
                }
                i++;
                result = result2;
                selfFileName = selfFileName2;
            }
            File appTraceFile = this.mAMSImpl.dumpAppStackTraces(javapids, null, nativePids, "App Scout Exception", filePath);
            if (appTraceFile != null) {
                Slog.d(TAG, "Dump scout other trace file successfully !");
            } else {
                Slog.w(TAG, "Dump scout other trace file fail !");
            }
        } else {
            Slog.w(TAG, "dumpAPPStackTraces: pids is null");
        }
        if (ScoutUtils.isLibraryTest()) {
            final String walkstackFileName = fileNamePerfix + timestamp + "_walkstack";
            File appScoutDir = new File(APP_LOG_DIR);
            if (appScoutDir.exists() && (pathArray = appScoutDir.listFiles(new FilenameFilter() { // from class: com.android.server.am.DumpScoutTraceThread.1
                @Override // java.io.FilenameFilter
                public boolean accept(File file, String name) {
                    if (name.contains(walkstackFileName)) {
                        return true;
                    }
                    return false;
                }
            })) != null && pathArray.length > 0) {
                File walkstackFileOld = new File(pathArray[0].getAbsolutePath());
                File walkstackFile = new File(path + "/" + fileNamePerfix + timestamp + "-miui-self-trace");
                try {
                    if (walkstackFile.createNewFile()) {
                        FileUtils.setPermissions(walkstackFile.getAbsolutePath(), 384, -1, -1);
                        if (FileUtils.copyFile(walkstackFileOld, walkstackFile)) {
                            Slog.i(TAG, "Success copying walkstack trace to path" + walkstackFile.getAbsolutePath());
                            walkstackFileOld.delete();
                        } else {
                            Slog.w(TAG, "Fail to copy walkstack trace to path" + walkstackFile.getAbsolutePath());
                        }
                    }
                } catch (IOException e) {
                    Slog.w(TAG, "Exception occurs while copying walkstack trace file:", e);
                }
            }
        }
    }
}
