package com.android.server.connectivity;

import android.content.Context;
/* loaded from: classes.dex */
public class VpnInjector {
    static boolean isSpecialUser(Context context, int parentUserId, int userId) {
        return parentUserId >= 0 && userId >= 0 && parentUserId == 0 && userId == 999;
    }
}
