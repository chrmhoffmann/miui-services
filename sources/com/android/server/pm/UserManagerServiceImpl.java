package com.android.server.pm;

import android.content.Context;
import android.provider.Settings;
import com.miui.base.MiuiStubRegistry;
/* loaded from: classes.dex */
public class UserManagerServiceImpl implements UserManagerServiceStub {

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<UserManagerServiceImpl> {

        /* compiled from: UserManagerServiceImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final UserManagerServiceImpl INSTANCE = new UserManagerServiceImpl();
        }

        public UserManagerServiceImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public UserManagerServiceImpl provideNewInstance() {
            return new UserManagerServiceImpl();
        }
    }

    public int checkAndGetNewUserId(int flags, int defUserId) {
        if ((134217728 & flags) != 0) {
            return 110;
        }
        return defUserId;
    }

    public boolean isInMaintenanceMode(Context context) {
        try {
            return Settings.Global.getInt(context.getContentResolver(), "maintenance_mode_user_id") == 110;
        } catch (Settings.SettingNotFoundException e) {
            return false;
        }
    }
}
