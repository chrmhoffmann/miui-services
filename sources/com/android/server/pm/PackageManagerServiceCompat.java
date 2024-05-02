package com.android.server.pm;

import android.content.IntentFilter;
import android.text.TextUtils;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import com.android.server.pm.pkg.component.ParsedIntentInfo;
import com.android.server.pm.pkg.component.ParsedMainComponent;
import java.util.ArrayList;
import java.util.List;
import miui.security.WakePathComponent;
/* loaded from: classes.dex */
public class PackageManagerServiceCompat {
    public static List<WakePathComponent> getWakePathComponents(String packageName) {
        List<WakePathComponent> ret = new ArrayList<>();
        if (TextUtils.isEmpty(packageName)) {
            return ret;
        }
        PackageManagerService service = PackageManagerServiceStub.get().getService();
        synchronized (service.mLock) {
            AndroidPackage pkg = (AndroidPackage) service.mPackages.get(packageName);
            if (pkg == null) {
                return ret;
            }
            parsePkgCompentLock(ret, pkg.getActivities(), 3);
            parsePkgCompentLock(ret, pkg.getReceivers(), 1);
            parsePkgCompentLock(ret, pkg.getProviders(), 4);
            parsePkgCompentLock(ret, pkg.getServices(), 2);
            return ret;
        }
    }

    private static void parsePkgCompentLock(List<WakePathComponent> wakePathComponents, List<? extends ParsedMainComponent> components, int componentType) {
        if (wakePathComponents == null || components == null) {
            return;
        }
        for (int i = components.size() - 1; i >= 0; i--) {
            if (components.get(i).isExported()) {
                WakePathComponent wakePathComponent = new WakePathComponent();
                wakePathComponent.setType(componentType);
                wakePathComponent.setClassname(components.get(i).getClassName());
                for (int j = components.get(i).getIntents().size() - 1; j >= 0; j--) {
                    IntentFilter intentFilter = ((ParsedIntentInfo) components.get(i).getIntents().get(j)).getIntentFilter();
                    for (int k = intentFilter.countActions() - 1; k >= 0; k--) {
                        wakePathComponent.addIntentAction(intentFilter.getAction(k));
                    }
                }
                wakePathComponents.add(wakePathComponent);
            }
        }
    }
}
