package com.android.server.am;

import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;
import android.util.Slog;
import com.miui.base.MiuiStubRegistry;
import com.miui.whetstone.WhetstonePackageState;
import com.miui.whetstone.server.WhetstoneActivityManagerService;
/* loaded from: classes.dex */
public class ActiveServiceManagementImpl implements ActiveServiceManagementStub {
    String activeWallpaperPackageName;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<ActiveServiceManagementImpl> {

        /* compiled from: ActiveServiceManagementImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final ActiveServiceManagementImpl INSTANCE = new ActiveServiceManagementImpl();
        }

        public ActiveServiceManagementImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public ActiveServiceManagementImpl provideNewInstance() {
            return new ActiveServiceManagementImpl();
        }
    }

    ActiveServiceManagementImpl() {
    }

    public boolean canRestartServiceLocked(ServiceRecord record) {
        if ((record.appInfo.flags & 8) == 0 && UserHandle.getAppId(record.appInfo.uid) > 2000 && !record.packageName.equals(this.activeWallpaperPackageName)) {
            AutoStartManagerService.getInstance();
            if (!AutoStartManagerService.canRestartServiceLocked(record.packageName, record.appInfo.uid, "ActiveServiceManagementImpl#canRestartServiceLocked", record.getComponentName(), !record.stopIfKilled)) {
                return false;
            }
            String str = "";
            if (WhetstoneActivityManagerService.getSingletonService() != null) {
                WhetstoneActivityManagerService singletonService = WhetstoneActivityManagerService.getSingletonService();
                String str2 = record.packageName;
                int callingUserId = UserHandle.getCallingUserId();
                String className = record.name != null ? record.name.getClassName() : str;
                String str3 = record.processName;
                Object[] objArr = new Object[1];
                objArr[0] = record.intent != null ? record.intent.getIntent() : null;
                if (singletonService.checkPackageState(str2, "Restart: AMS", 2, callingUserId, className, str3, objArr) != 1) {
                    StringBuilder append = new StringBuilder().append("Permission denied by Whetstone, cannot re-start service from ").append(record.packageName).append("/");
                    if (record.name != null) {
                        str = record.name.getClassName();
                    }
                    Slog.w("WhetstonePackageState", append.append(str).append(" in ").append(record.processName).append(", UserId: ").append(UserHandle.getCallingUserId()).toString());
                    return false;
                }
            }
            if (WhetstonePackageState.DEBUG) {
                StringBuilder append2 = new StringBuilder().append("restart service from ").append(record.packageName).append("/");
                if (record.name != null) {
                    str = record.name.getClassName();
                }
                Slog.d("WhetstonePackageState", append2.append(str).append(" in ").append(record.processName).append(", UserId: ").append(UserHandle.getCallingUserId()).toString());
                return true;
            }
            return true;
        }
        return true;
    }

    public boolean canBindService(Context context, Intent service, int userId) {
        return AutoStartManagerService.getInstance().isAllowStartService(context, service, userId);
    }

    public void updateWallPaperPackageName(String packageName) {
        this.activeWallpaperPackageName = packageName;
    }
}
