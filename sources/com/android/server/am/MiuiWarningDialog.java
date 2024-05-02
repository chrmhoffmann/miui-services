package com.android.server.am;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.BidiFormatter;
import android.view.WindowManager;
import android.widget.TextView;
import com.android.server.am.MiuiWarnings;
import com.android.server.inputmethod.InputMethodManagerServiceImpl;
/* loaded from: classes.dex */
public class MiuiWarningDialog extends BaseErrorDialog {
    private static final int BUTTON_CANCEL = 2;
    private static final int BUTTON_OK = 1;
    private static final String TAG = "MiuiWarningDialog";
    private MiuiWarnings.WarningCallback mCallback;
    private final Handler mHandler;

    public MiuiWarningDialog(String packageLabel, Context context, MiuiWarnings.WarningCallback callback) {
        super(context);
        Handler handler = new Handler() { // from class: com.android.server.am.MiuiWarningDialog.1
            @Override // android.os.Handler
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case 1:
                        MiuiWarningDialog.this.mCallback.onCallback(true);
                        return;
                    case 2:
                        MiuiWarningDialog.this.mCallback.onCallback(false);
                        return;
                    default:
                        return;
                }
            }
        };
        this.mHandler = handler;
        this.mCallback = callback;
        Resources res = context.getResources();
        BidiFormatter bidi = BidiFormatter.getInstance();
        setCancelable(false);
        setTitle(res.getString(286196359, bidi.unicodeWrap(packageLabel)));
        setButton(-1, res.getText(286196358), handler.obtainMessage(1));
        setButton(-2, res.getText(286196357), handler.obtainMessage(2));
        getWindow().setType(2010);
        WindowManager.LayoutParams attrs = getWindow().getAttributes();
        attrs.setTitle("MiuiWarning:" + packageLabel);
        attrs.privateFlags = 272;
        getWindow().setAttributes(attrs);
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int id = getContext().getResources().getIdentifier("alertTitle", "id", InputMethodManagerServiceImpl.MIUIXPACKAGE);
        TextView titleView = (TextView) findViewById(id);
        if (titleView != null) {
            titleView.setSingleLine(false);
        }
    }
}
