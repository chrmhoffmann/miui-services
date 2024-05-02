package com.miui.server.xspace;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.provider.Settings;
import android.util.Slog;
import com.android.server.wm.ScreenRotationAnimationImpl;
import com.miui.server.AccessController;
import java.util.ArrayList;
import java.util.List;
import miui.util.OldmanUtil;
/* loaded from: classes.dex */
public class SecSpaceManagerService {
    public static final String TAG = "SecSpaceManagerService";
    private static List<String> sDataTransferPackageNames;
    public static int SECOND_USER_ID = ScreenRotationAnimationImpl.BLACK_SURFACE_INVALID_POSITION;
    public static int KID_SPACE_ID = ScreenRotationAnimationImpl.BLACK_SURFACE_INVALID_POSITION;

    static {
        ArrayList arrayList = new ArrayList();
        sDataTransferPackageNames = arrayList;
        arrayList.add("com.android.fileexplorer");
        sDataTransferPackageNames.add("com.mi.android.globalFileexplorer");
        sDataTransferPackageNames.add("com.miui.securitycore");
        sDataTransferPackageNames.add(AccessController.PACKAGE_GALLERY);
        sDataTransferPackageNames.add("com.android.providers.media.module");
        sDataTransferPackageNames.add("com.google.android.providers.media.module");
    }

    public static void init(Context context) {
        Slog.d(TAG, "init SecSpaceManagerService");
        if (OldmanUtil.IS_ELDER_MODE) {
            return;
        }
        int secondSpaceId = getSecondSpaceId(context);
        SECOND_USER_ID = secondSpaceId;
        if (secondSpaceId != -10000) {
            startSecSpace(context);
        }
        int kidSpaceId = getKidSpaceId(context);
        KID_SPACE_ID = kidSpaceId;
        if (kidSpaceId != -10000) {
            Slog.d(TAG, "start KidModeSpaceService");
            startKidSpace(context);
        }
        registerContentObserver(context);
    }

    private static void startSecSpace(Context context) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.miui.securitycore", "com.miui.securityspace.service.SecondSpaceService"));
        context.startService(intent);
    }

    private static void startKidSpace(Context context) {
        try {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName("com.miui.securitycore", "com.miui.securityspace.service.KidModeSpaceService"));
            context.startService(intent);
        } catch (Exception e) {
            Slog.e(TAG, "start KidModeSpaceService", e);
        }
    }

    private static void registerContentObserver(final Context context) {
        ContentObserver contentObserver = new ContentObserver(null) { // from class: com.miui.server.xspace.SecSpaceManagerService.1
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                SecSpaceManagerService.SECOND_USER_ID = SecSpaceManagerService.getSecondSpaceId(context);
            }
        };
        context.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("second_user_id"), true, contentObserver, 0);
    }

    public static int getSecondSpaceId(Context context) {
        return Settings.Secure.getInt(context.getContentResolver(), "second_user_id", ScreenRotationAnimationImpl.BLACK_SURFACE_INVALID_POSITION);
    }

    private static int getKidSpaceId(Context context) {
        return Settings.Secure.getIntForUser(context.getContentResolver(), "kid_user_id", ScreenRotationAnimationImpl.BLACK_SURFACE_INVALID_POSITION, 0);
    }

    public static boolean isDataTransferProcess(int userId, String packageName) {
        return userId == SECOND_USER_ID && sDataTransferPackageNames.contains(packageName);
    }
}
