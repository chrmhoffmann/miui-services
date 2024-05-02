package com.android.server.am;

import com.android.internal.app.IMiuiSysUser;
/* loaded from: classes.dex */
public class MiuiSysUserServiceHelper {
    public static final String TAG = "MIUI_SYS_USER";
    private static IMiuiSysUser sysUser;

    public static void setMiuiSysUser(IMiuiSysUser obj) {
        sysUser = obj;
    }
}
