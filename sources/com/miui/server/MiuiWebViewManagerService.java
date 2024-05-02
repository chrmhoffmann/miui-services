package com.miui.server;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import java.util.List;
import miui.webview.IMiuiWebViewManager;
/* loaded from: classes.dex */
public class MiuiWebViewManagerService extends IMiuiWebViewManager.Stub {
    private static final String EXEMPT_APP = "com.android.thememanager";
    private static final String SEND_TIME_KEY = "sendTime";
    private static final String TAG = "MiuiWebViewManagerService";
    private int MSG_RESTART_WEBVIEW = 127;
    private final Context mContext;
    private Handler mHandler;
    private HandlerThread mHandlerThread;

    public MiuiWebViewManagerService(Context context) {
        this.mContext = context;
        HandlerThread handlerThread = new HandlerThread("MiuiWebViewWorker");
        this.mHandlerThread = handlerThread;
        handlerThread.start();
        this.mHandler = new Handler(this.mHandlerThread.getLooper()) { // from class: com.miui.server.MiuiWebViewManagerService.1
            @Override // android.os.Handler
            public void handleMessage(Message msg) {
                if (msg.what == MiuiWebViewManagerService.this.MSG_RESTART_WEBVIEW) {
                    long send = msg.getData().getLong(MiuiWebViewManagerService.SEND_TIME_KEY);
                    ActivityManager am = (ActivityManager) MiuiWebViewManagerService.this.mContext.getSystemService("activity");
                    List<String> pkgs = am.collectWebViewProcesses();
                    int killed = 0;
                    if (pkgs != null) {
                        for (int i = 0; i < pkgs.size(); i++) {
                            String pkgName = pkgs.get(i);
                            if (pkgName != null) {
                                String[] splitInfo = pkgName.split("#");
                                String barePkgName = splitInfo[0];
                                int pid = Integer.valueOf(splitInfo[1]).intValue();
                                if (!"com.android.thememanager".equals(barePkgName)) {
                                    am.forceStopPackage(barePkgName);
                                    Log.d(MiuiWebViewManagerService.TAG, "kill pkgName: " + barePkgName + " pid: " + pid);
                                    killed++;
                                }
                            }
                        }
                        Log.d(MiuiWebViewManagerService.TAG, "restart webview procs: " + pkgs.size() + " with timeUsage: " + (System.currentTimeMillis() - send) + "ms killed: " + killed);
                    }
                }
            }
        };
    }

    public void restartWebViewProcesses() {
        Message msg = Message.obtain();
        msg.what = this.MSG_RESTART_WEBVIEW;
        Bundle data = new Bundle();
        data.putLong(SEND_TIME_KEY, System.currentTimeMillis());
        msg.setData(data);
        this.mHandler.sendMessage(msg);
        Log.i(TAG, "restartWebViewProcesses called");
    }
}
