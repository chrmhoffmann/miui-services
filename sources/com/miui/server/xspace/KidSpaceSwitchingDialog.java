package com.miui.server.xspace;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import com.android.server.am.ActivityManagerService;
/* loaded from: classes.dex */
public class KidSpaceSwitchingDialog extends BaseUserSwitchingDialog {
    @Override // com.miui.server.xspace.BaseUserSwitchingDialog, android.app.Dialog
    public /* bridge */ /* synthetic */ void show() {
        super.show();
    }

    public KidSpaceSwitchingDialog(ActivityManagerService service, Context context, int userId) {
        super(service, context, 286261257, userId);
    }

    @Override // com.miui.server.xspace.BaseUserSwitchingDialog, android.app.AlertDialog, android.app.Dialog
    public void onCreate(Bundle savedInstanceState) {
        int i;
        super.onCreate(savedInstanceState);
        Window win = getWindow();
        int i2 = 0;
        win.getDecorView().setPadding(0, 0, 0, 0);
        WindowManager.LayoutParams lp = win.getAttributes();
        lp.width = -1;
        lp.height = -1;
        lp.layoutInDisplayCutoutMode = 1;
        boolean isEnterKidMode = this.mUserId != 0;
        if (!isEnterKidMode) {
            i2 = 1;
        }
        lp.screenOrientation = i2;
        win.setAttributes(lp);
        win.getDecorView().setSystemUiVisibility(3846);
        View view = LayoutInflater.from(getContext()).inflate(285999129, (ViewGroup) null);
        ImageView imageView = (ImageView) view.findViewById(285868123);
        if (isEnterKidMode) {
            i = 285737071;
        } else {
            i = 285737072;
        }
        imageView.setImageResource(i);
        setContentView(view);
    }
}
