package com.android.server.clipboard;

import android.app.AppOpsManager;
import android.content.ClipData;
import android.content.ClipboardRuleInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManagerInternal;
import android.os.Binder;
import android.os.ServiceManager;
import android.os.Trace;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import com.android.server.LocalServices;
import com.android.server.am.ActivityManagerServiceStub;
import com.miui.base.MiuiStubRegistry;
import com.miui.server.greeze.GreezeManagerService;
import java.util.List;
import java.util.Map;
import miui.greeze.IGreezeManager;
import miui.os.Build;
/* loaded from: classes.dex */
public class ClipboardServiceStubImpl extends ClipboardServiceStub {
    private static final String MIUI_INPUT_NO_NEED_SHOW_POP = "miui_input_no_need_show_pop";
    private static final String TAG = "ClipboardServiceI";
    private AppOpsManager mAppOps;
    private Context mContext;
    private IGreezeManager mIGreezeManager = null;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<ClipboardServiceStubImpl> {

        /* compiled from: ClipboardServiceStubImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final ClipboardServiceStubImpl INSTANCE = new ClipboardServiceStubImpl();
        }

        public ClipboardServiceStubImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public ClipboardServiceStubImpl provideNewInstance() {
            return new ClipboardServiceStubImpl();
        }
    }

    public void init(Context context, AppOpsManager appOps) {
        this.mContext = context;
        this.mAppOps = appOps;
    }

    public int clipboardAccessResult(ClipData clip, String callerPkg, int callerUid, int callerUserId, int primaryClipUid, boolean userCall) {
        int resultMode;
        notifyGreeze(primaryClipUid);
        CharSequence firstClipData = firstClipData(clip);
        boolean firstClipDataEmpty = firstClipData == null || firstClipData.toString().trim().length() == 0;
        if (!Build.IS_INTERNATIONAL_BUILD && !firstClipDataEmpty && !userCall && primaryClipUid != callerUid) {
            if (ClipboardChecker.getInstance().isAiClipboardEnable(this.mContext)) {
                String defaultIme = Settings.Secure.getStringForUser(this.mContext.getContentResolver(), "default_input_method", callerUserId);
                if (!TextUtils.isEmpty(defaultIme)) {
                    String imePkg = ComponentName.unflattenFromString(defaultIme).getPackageName();
                    if (imePkg.equals(callerPkg)) {
                        ClipboardChecker.getInstance().applyReadClipboardOperation(true, callerUid, callerPkg, 1);
                        return 0;
                    }
                }
                int resultMode2 = this.mAppOps.checkOp(29, callerUid, callerPkg);
                boolean isSystem = UserHandle.getAppId(callerUid) < 10000;
                if (isSystem) {
                    resultMode = resultMode2;
                } else {
                    PackageManagerInternal pmi = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
                    resultMode = resultMode2;
                    ApplicationInfo appInfo = pmi.getApplicationInfo(callerPkg, 0L, 1000, UserHandle.getUserId(callerUid));
                    isSystem = (appInfo == null || (appInfo.flags & 1) == 0) ? false : true;
                }
                int matchResult = ClipboardChecker.getInstance().matchClipboardRule(callerPkg, callerUid, firstClipData, clip.hashCode(), isSystem);
                if (resultMode == 0 || resultMode == 4) {
                    if (callerUid != 1000 && isSystem) {
                        if (matchResult == 3) {
                            return 0;
                        }
                        if (Build.IS_DEVELOPMENT_VERSION && matchResult != 0) {
                            ClipboardChecker.getInstance().applyReadClipboardOperation(false, callerUid, callerPkg, 5);
                            Log.i(TAG, "MIUILOG- Permission Denied when system app read clipboard, caller " + callerUid);
                            return 1;
                        }
                        ClipboardChecker.getInstance().applyReadClipboardOperation(true, callerUid, callerPkg, 5);
                        return 0;
                    }
                    ClipboardChecker.getInstance().applyReadClipboardOperation(true, callerUid, callerPkg, 1);
                    return 0;
                } else if (primaryClipUid == 1000 && clip.getDescription() != null && TextUtils.equals(MIUI_INPUT_NO_NEED_SHOW_POP, clip.getDescription().getLabel())) {
                    ClipboardChecker.getInstance().applyReadClipboardOperation(false, callerUid, callerPkg, 5);
                    Log.i(TAG, "MIUILOG- Permission Denied when read clipboard [CloudSet], caller " + callerUid);
                    return 1;
                } else if (matchResult == 0) {
                    ClipboardChecker.getInstance().updateClipItemData(clip);
                    ClipboardChecker.getInstance().applyReadClipboardOperation(true, callerUid, callerPkg, 5);
                    return 0;
                } else if (matchResult == 1) {
                    ClipboardChecker.getInstance().applyReadClipboardOperation(false, callerUid, callerPkg, 5);
                    Log.i(TAG, "MIUILOG- Permission Denied when read clipboard [Mismatch], caller " + callerUid);
                    return 1;
                } else {
                    ClipboardChecker.getInstance().stashClip(callerUid, clip);
                    return 3;
                }
            }
        }
        if (Build.IS_INTERNATIONAL_BUILD || (!firstClipDataEmpty && userCall)) {
            ClipboardChecker.getInstance().applyReadClipboardOperation(true, callerUid, callerPkg, userCall ? 6 : 1);
            return 0;
        }
        return 0;
    }

    public ClipData waitUserChoice(String callerPkg, int callerUid) {
        Trace.beginSection("ai_read_clipboard");
        try {
            if (this.mAppOps.noteOp(29, callerUid, callerPkg) == 0) {
                return ClipboardChecker.getInstance().getStashClip(callerUid);
            }
            Trace.endSection();
            ClipboardChecker.getInstance().removeStashClipLater(callerUid);
            return this.EMPTY_CLIP;
        } finally {
            Trace.endSection();
            ClipboardChecker.getInstance().removeStashClipLater(callerUid);
        }
    }

    private IGreezeManager getGreeze() {
        if (this.mIGreezeManager == null) {
            this.mIGreezeManager = IGreezeManager.Stub.asInterface(ServiceManager.getService(GreezeManagerService.SERVICE_NAME));
        }
        return this.mIGreezeManager;
    }

    private void notifyGreeze(int uid) {
        int[] uids = {uid};
        try {
            if (getGreeze().isUidFrozen(uid)) {
                getGreeze().thawUids(uids, 1000, "clip");
            }
        } catch (Exception e) {
            Log.d(TAG, "ClipGreez err:");
        }
    }

    public boolean isUidFocused(int uid) {
        return ClipboardChecker.getInstance().hasStash(uid);
    }

    public ClipData getStashClipboardData() {
        checkPermissionPkg();
        return ClipboardChecker.getInstance().getClipItemData();
    }

    public void updateClipboardRuleData(List<ClipboardRuleInfo> ruleInfoList) {
        checkPermissionPkg();
        ClipboardChecker.getInstance().updateClipboardPatterns(ruleInfoList);
    }

    private void checkPermissionPkg() {
        String callingPackageName = ActivityManagerServiceStub.get().getPackageNameByPid(Binder.getCallingPid());
        if (!"com.lbe.security.miui".equals(callingPackageName)) {
            throw new SecurityException("Permission Denial: attempt to assess internal clipboard from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid() + ", pkg=" + callingPackageName);
        }
    }

    public Map getClipboardClickRuleData() {
        return ClipboardChecker.getInstance().getClipboardClickTrack();
    }

    private CharSequence firstClipData(ClipData clipData) {
        CharSequence paste = null;
        if (clipData != null) {
            long identity = Binder.clearCallingIdentity();
            for (int i = 0; i < clipData.getItemCount() && (paste = clipData.getItemAt(i).getText()) == null; i++) {
                try {
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
        }
        return paste;
    }
}
