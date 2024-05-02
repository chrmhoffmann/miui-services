package com.miui.server.xspace;

import android.content.Context;
import android.os.Bundle;
import com.android.server.am.ActivityManagerService;
/* loaded from: classes.dex */
public class MiuiUserSwitchingDialog extends BaseUserSwitchingDialog {
    @Override // com.miui.server.xspace.BaseUserSwitchingDialog, android.app.Dialog
    public /* bridge */ /* synthetic */ void show() {
        super.show();
    }

    public MiuiUserSwitchingDialog(ActivityManagerService service, Context context, int userId) {
        super(service, context, 286261267, userId);
    }

    @Override // com.miui.server.xspace.BaseUserSwitchingDialog, android.app.AlertDialog, android.app.Dialog
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
}
