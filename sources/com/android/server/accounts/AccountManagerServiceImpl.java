package com.android.server.accounts;

import android.content.pm.PackageManager;
import com.miui.base.MiuiStubRegistry;
/* loaded from: classes.dex */
public class AccountManagerServiceImpl implements AccountManagerServiceStub {

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<AccountManagerServiceImpl> {

        /* compiled from: AccountManagerServiceImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final AccountManagerServiceImpl INSTANCE = new AccountManagerServiceImpl();
        }

        public AccountManagerServiceImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public AccountManagerServiceImpl provideNewInstance() {
            return new AccountManagerServiceImpl();
        }
    }

    public boolean isForceRemove(boolean removalAllowed) {
        return AccountManagerServiceInjector.isForceRemove(removalAllowed);
    }

    public boolean isTrustedAccountSignature(PackageManager pm, String accountType, int serviceUid, int callingUid) {
        return AccountManagerServiceInjector.isTrustedAccountSignature(pm, accountType, serviceUid, callingUid);
    }
}
