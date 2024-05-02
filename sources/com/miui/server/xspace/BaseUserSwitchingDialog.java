package com.miui.server.xspace;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Slog;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import com.android.server.am.ActivityManagerService;
import com.android.server.am.ActivityManagerServiceImpl;
/* loaded from: classes.dex */
public class BaseUserSwitchingDialog extends AlertDialog {
    static final int MSG_START_USER = 1;
    private static final String TAG = "BaseUserSwitchingDialog";
    private static final int WINDOW_SHOWN_TIMEOUT_MS = 3000;
    private final Handler mHandler = new Handler() { // from class: com.miui.server.xspace.BaseUserSwitchingDialog.2
        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    BaseUserSwitchingDialog.this.startUser();
                    return;
                default:
                    return;
            }
        }
    };
    ViewTreeObserver.OnWindowShownListener mOnWindowShownListener = new ViewTreeObserver.OnWindowShownListener() { // from class: com.miui.server.xspace.BaseUserSwitchingDialog.1
        public void onWindowShown() {
            BaseUserSwitchingDialog.this.startUser();
        }
    };
    private final ActivityManagerService mService;
    private boolean mStartedUser;
    protected final int mUserId;

    public BaseUserSwitchingDialog(ActivityManagerService service, Context context, int styleId, int userId) {
        super(context, styleId);
        this.mService = service;
        this.mUserId = userId;
    }

    @Override // android.app.AlertDialog, android.app.Dialog
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(false);
        getWindow().setType(2010);
        WindowManager.LayoutParams attrs = getWindow().getAttributes();
        attrs.privateFlags = 272;
        getWindow().setAttributes(attrs);
    }

    @Override // android.app.Dialog
    public void show() {
        super.show();
        View view = getWindow().getDecorView();
        if (view != null) {
            view.getViewTreeObserver().addOnWindowShownListener(this.mOnWindowShownListener);
        }
        Handler handler = this.mHandler;
        handler.sendMessageDelayed(handler.obtainMessage(1), ActivityManagerServiceImpl.BOOST_DURATION);
    }

    void startUser() {
        synchronized (this) {
            if (!this.mStartedUser) {
                try {
                    ReflectUtil.callObjectMethod(ReflectUtil.getObjectField(this.mService, "mUserController"), Void.TYPE, "startUserInForeground", new Class[]{Integer.TYPE}, Integer.valueOf(this.mUserId));
                    this.mStartedUser = true;
                    View view = getWindow().getDecorView();
                    if (view != null) {
                        view.getViewTreeObserver().removeOnWindowShownListener(this.mOnWindowShownListener);
                    }
                    this.mHandler.removeMessages(1);
                } catch (Exception exception) {
                    Slog.e(TAG, "Call startUserInForeground fail." + exception);
                }
            }
        }
    }
}
