package com.miui.server.xspace;

import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.app.AppGlobals;
import android.app.ProfilerInfo;
import android.app.UserSwitchObserver;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageDeleteObserver;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerInternal;
import android.content.pm.ParceledListSlice;
import android.content.pm.ProviderInfo;
import android.content.pm.UserInfo;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.BaseBundle;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.IUserManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.MiuiSettings;
import android.provider.Settings;
import android.server.am.SplitScreenReporter;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.ArraySet;
import android.util.Pair;
import android.util.Slog;
import com.android.internal.util.ArrayUtils;
import com.android.server.LocalServices;
import com.android.server.am.ActivityManagerService;
import com.android.server.am.ProcessUtils;
import com.android.server.inputmethod.InputMethodManagerServiceImpl;
import com.android.server.pm.UserManagerService;
import com.android.server.wm.ActivityTaskSupervisor;
import com.android.server.wm.ActivityTaskSupervisorImpl;
import com.android.server.wm.SafeActivityOptions;
import com.android.server.wm.ScreenRotationAnimationImpl;
import com.android.server.wm.WindowManagerService;
import com.miui.base.MiuiStubRegistry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import miui.os.Build;
import miui.securityspace.CrossUserUtils;
import miui.securityspace.XSpaceConstant;
import miui.securityspace.XSpaceIntentCompat;
import miui.securityspace.XSpaceUserHandle;
import miui.securityspace.XSpaceUtils;
/* loaded from: classes.dex */
public class XSpaceManagerServiceImpl extends XSpaceManagerServiceStub {
    private static final String ACTION_START_DUAL_ANIMATION = "miui.intent.action.START_DUAL_ANIMATION";
    private static final String ACTION_WEIBO_SDK_REQ = "com.sina.weibo.sdk.action.ACTION_SDK_REQ_ACTIVITY";
    public static final String ACTION_XSPACE_RESOLVER_ACTIVITY_FROM_CORE = "miui.intent.action.ACTION_XSPACE_RESOLVER_ACTIVITY_FROM_CORE";
    private static final String EXTRA_XSPACE_CACHED_USERID = "android.intent.extra.xspace_cached_uid";
    private static final String EXTRA_XSPACE_RESOLVE_INTENT_AGAIN = "android.intent.extra.xspace_resolve_intent_again";
    private static final String EXTRA_XSPACE_USERID_SELECTED = "android.intent.extra.xspace_userid_selected";
    private static final int MAX_COMPETE_XSPACE_NOTIFICATION_TIMES = 1;
    private static final String ORG_IFAA_AIDL_MANAGER_PKGNAME = "org.ifaa.aidl.manager";
    private static final String PACKAGE_ANDROID_INSTALLER = "com.android.packageinstaller";
    private static final String PACKAGE_GOOGLE_INSTALLER = "com.google.android.packageinstaller";
    private static final String PACKAGE_LINKER = "@";
    private static final String PACKAGE_MIUI_INSTALLER = "com.miui.packageinstaller";
    private static final String PACKAGE_SECURITYADD = "com.miui.securityadd";
    private static final String PACKAGE_SETTING = "com.android.settings";
    private static final String PACKAGE_XMSF = "com.xiaomi.xmsf";
    private static final String SYSTEM_PROP_XSPACE_CREATED = "persist.sys.xspace_created";
    private static final String TAG = "XSpaceManagerServiceImpl";
    private static final String WEIXIN_SDK_REQ_CLASSNAME = ".wxapi.WXEntryActivity";
    private static final String XIAOMI_GAMECENTER_SDK_PKGNAME = "com.xiaomi.gamecenter.sdk.service";
    private static final String XSPACE_ANIMATION_STATUS = "xspace_animation_status";
    private static final int XSPACE_ANIMATION_STATUS_DEFAULT = 2;
    private static final int XSPACE_ANIMATION_STATUS_OFF = 0;
    private static final int XSPACE_ANIMATION_STATUS_ON = 1;
    private static final int XSPACE_APP_LIST_INIT_NUMBER;
    private static final String XSPACE_CLOUD_CONTROL_STATUS = "dual_animation_switch";
    private static final String XSPACE_SERVICE_COMPONENT = "com.miui.securitycore/com.miui.xspace.service.XSpaceService";
    private static BaseUserSwitchingDialog mDialog;
    private static UserManagerService mUserManager;
    private static final List<String> mediaAccessRequired;
    private static final ArrayList<String> sAddUserPackagesBlackList;
    private static final ArrayList<String> sCrossUserAimPackagesWhiteList;
    private static final ArrayList<String> sCrossUserCallingPackagesWhiteList;
    private static final ArrayList<String> sCrossUserDisableComponentActionWhiteList;
    private static final ArrayList<String> sPublicActionList;
    private static int sSwitchUserCallingUid;
    private static final ArrayList<String> sXSpaceApplicationList;
    private static final ArrayList<String> sXSpaceInstalledPackagesSelfLocked;
    private static final ArrayList<String> sXspaceAnimWhiteList;
    private Context mContext;
    private long mLastTime;
    private ContentResolver mResolver;
    private static final ArrayList<String> HISTORY_PACKAGE = new ArrayList<>();
    private static final String PACKAGE_ALIPAY = "com.eg.android.AlipayGphone";
    private static final HashMap<String, Integer> sCachedCallingRelationSelfLocked = new HashMap<>();
    private static UserSwitchObserver mUserSwitchObserver = new UserSwitchObserver() { // from class: com.miui.server.xspace.XSpaceManagerServiceImpl.4
        public void onUserSwitchComplete(int newUserId) throws RemoteException {
            if (XSpaceManagerServiceImpl.mDialog != null) {
                XSpaceManagerServiceImpl.mDialog.dismiss();
            }
        }
    };
    private boolean mIsXSpaceCreated = false;
    private boolean mIsXSpaceActived = false;
    private boolean mAnimStatus = true;
    private final PackageChangedListener mPackageChangedListener = new PackageChangedListener();

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<XSpaceManagerServiceImpl> {

        /* compiled from: XSpaceManagerServiceImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final XSpaceManagerServiceImpl INSTANCE = new XSpaceManagerServiceImpl();
        }

        public XSpaceManagerServiceImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public XSpaceManagerServiceImpl provideNewInstance() {
            return new XSpaceManagerServiceImpl();
        }
    }

    static {
        ArrayList<String> arrayList = new ArrayList<>();
        sCrossUserCallingPackagesWhiteList = arrayList;
        ArrayList<String> arrayList2 = new ArrayList<>();
        sCrossUserDisableComponentActionWhiteList = arrayList2;
        ArrayList arrayList3 = new ArrayList();
        mediaAccessRequired = arrayList3;
        arrayList.add("android");
        arrayList.add(PACKAGE_SETTING);
        arrayList.add("com.android.systemui");
        arrayList.add(ActivityTaskSupervisorImpl.MIUI_APP_LOCK_PACKAGE_NAME);
        arrayList.add(InputMethodManagerServiceImpl.MIUI_HOME);
        arrayList.add("com.android.shell");
        arrayList.add("com.mi.android.globallauncher");
        arrayList.add("com.lbe.security.miui");
        arrayList2.add("android.nfc.action.TECH_DISCOVERED");
        arrayList3.add("com.android.fileexplorer");
        arrayList3.add("com.miui.huanji");
        ArrayList<String> arrayList4 = new ArrayList<>();
        sPublicActionList = arrayList4;
        arrayList4.add("android.intent.action.SEND");
        arrayList4.add("android.intent.action.SEND_MULTIPLE");
        arrayList4.add("android.intent.action.SENDTO");
        arrayList4.add("android.content.pm.action.REQUEST_PERMISSIONS_FOR_OTHER");
        ArrayList<String> arrayList5 = new ArrayList<>();
        sCrossUserAimPackagesWhiteList = arrayList5;
        arrayList5.add(PACKAGE_XMSF);
        arrayList5.addAll(XSpaceConstant.REQUIRED_APPS);
        arrayList5.remove(XIAOMI_GAMECENTER_SDK_PKGNAME);
        arrayList5.add("com.google.android.gsf");
        arrayList5.add("com.google.android.gsf.login");
        arrayList5.add("com.google.android.gms");
        arrayList5.add("com.google.android.play.games");
        ArrayList<String> arrayList6 = new ArrayList<>();
        sXspaceAnimWhiteList = arrayList6;
        arrayList6.add(InputMethodManagerServiceImpl.MIUI_HOME);
        arrayList6.add("com.mi.android.globallauncher");
        ArrayList<String> arrayList7 = new ArrayList<>();
        sAddUserPackagesBlackList = arrayList7;
        arrayList7.add("com.android.contacts");
        ArrayList<String> arrayList8 = new ArrayList<>();
        sXSpaceInstalledPackagesSelfLocked = arrayList8;
        arrayList8.add(XIAOMI_GAMECENTER_SDK_PKGNAME);
        arrayList8.add(ORG_IFAA_AIDL_MANAGER_PKGNAME);
        XSPACE_APP_LIST_INIT_NUMBER = arrayList8.size();
        ArrayList<String> arrayList9 = new ArrayList<>();
        sXSpaceApplicationList = arrayList9;
        arrayList9.add("com.android.vending");
    }

    public Intent checkXSpaceControl(Context context, ActivityInfo aInfo, Intent intent, boolean fromActivity, int requestCode, int userId, String callingPackage, int callingUserId, SafeActivityOptions safeActivityOptions) {
        String aimPkg;
        Intent intent2 = intent;
        if (!isPublicIntent(intent2, callingPackage, aInfo)) {
            return intent2;
        }
        String action = intent.getAction();
        if (aInfo != null) {
            aimPkg = aInfo.packageName;
        } else {
            aimPkg = getAimPkg(intent2);
        }
        Slog.w(TAG, "checkXSpaceControl, from:" + callingPackage + ", to:" + aimPkg + ", with act:" + action + ", callingUserId:" + callingUserId + ", toUserId:" + userId);
        if (callingUserId != 0 && callingUserId != 999) {
            return intent2;
        }
        if (callingUserId == 0 && userId == 0) {
            ArrayList<String> arrayList = sXSpaceInstalledPackagesSelfLocked;
            if (!arrayList.contains(aimPkg) && !arrayList.contains(callingPackage)) {
                return intent2;
            }
        }
        if ((intent.getFlags() & 1048576) != 0) {
            return intent2;
        }
        if (checkDualAnimationEnable(aimPkg, userId, callingPackage, safeActivityOptions)) {
            return creatDualAnimIntent(intent2);
        }
        if (intent.getComponent() != null) {
            PackageManagerInternal packageManagerInternal = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
            ArraySet<String> disabledComponents = packageManagerInternal.getDisabledComponents(aimPkg, 999);
            if (disabledComponents != null && disabledComponents.contains(intent.getComponent().getClassName())) {
                return intent2;
            }
        }
        ArrayList<String> arrayList2 = sCrossUserCallingPackagesWhiteList;
        if ((arrayList2.contains(callingPackage) && !sPublicActionList.contains(action)) || sCrossUserAimPackagesWhiteList.contains(aimPkg)) {
            return intent2;
        }
        if (sCrossUserDisableComponentActionWhiteList.contains(action) && !isComponentEnabled(aInfo, 999)) {
            return intent2;
        }
        if (intent2.hasExtra(EXTRA_XSPACE_USERID_SELECTED)) {
            Slog.w(TAG, "from XSpace ResolverActivity");
            intent2.removeExtra(EXTRA_XSPACE_RESOLVE_INTENT_AGAIN);
            XSpaceIntentCompat.prepareToLeaveUser(intent2, callingUserId);
            putCachedCallingRelation(aimPkg, callingPackage, callingUserId);
        } else {
            int cachedToUserId = getToUserIdFromCachedCallingRelation(aimPkg, callingPackage);
            if (cachedToUserId != -10000) {
                Slog.w(TAG, "using cached calling relation");
                intent2.putExtra(EXTRA_XSPACE_CACHED_USERID, cachedToUserId);
            } else {
                ArrayList<String> arrayList3 = sXSpaceInstalledPackagesSelfLocked;
                synchronized (arrayList3) {
                    try {
                        try {
                        } catch (Throwable th) {
                            th = th;
                        }
                    } catch (Throwable th2) {
                        th = th2;
                    }
                    try {
                        if (arrayList3.contains(aimPkg)) {
                            if (intent2.hasExtra("android.intent.extra.auth_to_call_xspace") && checkCallXSpacePermission(callingPackage)) {
                                Slog.i(TAG, "call XSpace directly");
                                return intent2;
                            }
                            if (!PACKAGE_ALIPAY.equals(aimPkg) && !XIAOMI_GAMECENTER_SDK_PKGNAME.equals(aimPkg) && !TextUtils.equals(PACKAGE_XMSF, callingPackage) && !TextUtils.equals(ACTION_WEIBO_SDK_REQ, action)) {
                                if (!TextUtils.equals(aimPkg + WEIXIN_SDK_REQ_CLASSNAME, intent.getComponent() == null ? "" : intent.getComponent().getClassName())) {
                                    Slog.w(TAG, "pop up ResolverActivity");
                                    intent2 = getResolverActivity(intent2, aimPkg, requestCode, callingPackage);
                                }
                            }
                            Slog.i(TAG, "call XSpace directly");
                            return intent2;
                        } else if (arrayList3.contains(callingPackage) || (arrayList2.contains(aimPkg) && userId == 999)) {
                            Slog.w(TAG, "XSpace installed App to normal App");
                            if (!sAddUserPackagesBlackList.contains(aimPkg)) {
                                XSpaceIntentCompat.prepareToLeaveUser(intent2, callingUserId);
                            }
                            if (TextUtils.equals(action, "android.settings.APPLICATION_DETAILS_SETTINGS")) {
                                intent2.putExtra("miui.intent.extra.USER_ID", callingUserId);
                            }
                            intent2.putExtra(EXTRA_XSPACE_CACHED_USERID, 0);
                            intent2.putExtra("userId", userId);
                            intent2.putExtra("calling_relation", true);
                        }
                        return intent2;
                    } catch (Throwable th3) {
                        th = th3;
                        throw th;
                    }
                }
            }
        }
        return intent2;
    }

    /* JADX WARN: Removed duplicated region for block: B:17:0x0051  */
    /* JADX WARN: Removed duplicated region for block: B:22:0x0084  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public android.content.pm.ActivityInfo resolveXSpaceIntent(android.content.pm.ActivityInfo r18, android.content.Intent r19, com.android.server.wm.ActivityTaskSupervisor r20, android.app.ProfilerInfo r21, java.lang.String r22, int r23, int r24, java.lang.String r25, int r26) {
        /*
            r17 = this;
            r9 = r17
            r1 = r18
            r10 = r19
            r11 = r24
            r12 = r25
            if (r11 == 0) goto L10
            r0 = 999(0x3e7, float:1.4E-42)
            if (r11 != r0) goto L16
        L10:
            boolean r0 = r9.isPublicIntent(r10, r12, r1)
            if (r0 != 0) goto L17
        L16:
            return r1
        L17:
            boolean r0 = r9.shouldResolveAgain(r10, r12)
            if (r0 == 0) goto L48
            android.content.pm.IPackageManager r2 = android.app.AppGlobals.getPackageManager()     // Catch: android.os.RemoteException -> L2f
            r4 = 0
            r5 = -1
            r3 = r19
            r7 = r24
            android.content.pm.ResolveInfo r0 = r2.resolveIntent(r3, r4, r5, r7)     // Catch: android.os.RemoteException -> L2f
            android.content.pm.ActivityInfo r0 = r0.activityInfo     // Catch: android.os.RemoteException -> L2f
            goto L49
        L2f:
            r0 = move-exception
            java.lang.StringBuilder r2 = new java.lang.StringBuilder
            r2.<init>()
            java.lang.String r3 = "Can not get aInfo, intent = "
            java.lang.StringBuilder r2 = r2.append(r3)
            java.lang.StringBuilder r2 = r2.append(r10)
            java.lang.String r2 = r2.toString()
            java.lang.String r3 = "XSpaceManagerServiceImpl"
            android.util.Slog.e(r3, r2, r0)
        L48:
            r0 = r1
        L49:
            int r13 = r9.getCachedUserId(r10, r12)
            r1 = -10000(0xffffffffffffd8f0, float:NaN)
            if (r13 == r1) goto L84
            int r14 = android.os.Binder.getCallingUid()
            long r15 = android.os.Binder.clearCallingIdentity()
            r1 = r17
            r2 = r19
            r3 = r20
            r4 = r22
            r5 = r23
            r6 = r21
            r7 = r13
            r8 = r14
            android.content.pm.ActivityInfo r0 = r1.resolveActivity(r2, r3, r4, r5, r6, r7, r8)
            android.os.Binder.restoreCallingIdentity(r15)
            r1 = 0
            java.lang.String r2 = "calling_relation"
            boolean r1 = r10.getBooleanExtra(r2, r1)
            if (r1 == 0) goto L81
            if (r0 == 0) goto L81
            java.lang.String r1 = r0.packageName
            r2 = r26
            r9.putCachedCallingRelation(r1, r12, r2)
            goto L86
        L81:
            r2 = r26
            goto L86
        L84:
            r2 = r26
        L86:
            return r0
        */
        throw new UnsupportedOperationException("Method not decompiled: com.miui.server.xspace.XSpaceManagerServiceImpl.resolveXSpaceIntent(android.content.pm.ActivityInfo, android.content.Intent, com.android.server.wm.ActivityTaskSupervisor, android.app.ProfilerInfo, java.lang.String, int, int, java.lang.String, int):android.content.pm.ActivityInfo");
    }

    private boolean checkDualAnimationEnable(String packageName, int userId, String callingPackage, SafeActivityOptions safeActivityOptions) {
        if (userId != 999 || !sXspaceAnimWhiteList.contains(callingPackage)) {
            return false;
        }
        if (safeActivityOptions != null) {
            Bundle bundle = safeActivityOptions.getActivityOptionsBundle();
            ActivityOptions activityOptions = bundle != null ? ActivityOptions.fromBundle(bundle) : null;
            if (activityOptions != null && activityOptions.getLaunchWindowingMode() == 5) {
                return false;
            }
        }
        if (!DateUtils.isToday(this.mLastTime)) {
            this.mLastTime = System.currentTimeMillis();
            HISTORY_PACKAGE.clear();
        }
        if (packageName != null && !packageName.equals(PACKAGE_SETTING)) {
            ArrayList<String> arrayList = HISTORY_PACKAGE;
            if (arrayList.contains(packageName) || !this.mAnimStatus) {
                return false;
            }
            arrayList.add(packageName);
            return true;
        }
        return false;
    }

    private Intent creatDualAnimIntent(Intent intent) {
        Intent result = new Intent(ACTION_START_DUAL_ANIMATION);
        result.setPackage(PACKAGE_SECURITYADD);
        result.addFlags(8388608);
        result.putExtra(EXTRA_XSPACE_CACHED_USERID, 0);
        intent.putExtra("android.intent.extra.auth_to_call_xspace", true);
        result.putExtra("android.intent.extra.INTENT", intent);
        return result;
    }

    private boolean shouldResolveAgain(Intent intent, String callingPackage) {
        String aimPkg = getAimPkg(intent);
        Intent newIntent = new Intent(intent);
        try {
            if (TextUtils.equals(aimPkg, callingPackage) || !newIntent.hasExtra(EXTRA_XSPACE_RESOLVE_INTENT_AGAIN)) {
                return false;
            }
            intent.removeExtra(EXTRA_XSPACE_RESOLVE_INTENT_AGAIN);
            return true;
        } catch (Exception e) {
            Slog.w(TAG, "Private intent: ", e);
            return false;
        }
    }

    private int getCachedUserId(Intent intent, String callingPackage) {
        String aimPkg = getAimPkg(intent);
        if (TextUtils.equals(aimPkg, callingPackage) || !intent.hasExtra(EXTRA_XSPACE_CACHED_USERID)) {
            return ScreenRotationAnimationImpl.BLACK_SURFACE_INVALID_POSITION;
        }
        int cachedUserId = intent.getIntExtra(EXTRA_XSPACE_CACHED_USERID, ScreenRotationAnimationImpl.BLACK_SURFACE_INVALID_POSITION);
        intent.removeExtra(EXTRA_XSPACE_CACHED_USERID);
        return cachedUserId;
    }

    private boolean isPublicIntent(Intent intent, String callingPackage, ActivityInfo aInfo) {
        String aimPkg;
        if (TextUtils.isEmpty(callingPackage)) {
            return false;
        }
        if (aInfo != null) {
            aimPkg = aInfo.packageName;
        } else {
            aimPkg = getAimPkg(intent);
        }
        if (TextUtils.equals(aimPkg, callingPackage)) {
            return false;
        }
        Intent newIntent = new Intent(intent);
        try {
            BaseBundle.setShouldDefuse(false);
            newIntent.hasExtra("");
            return true;
        } catch (Throwable e) {
            try {
                Slog.w(TAG, "Private intent", e);
                return false;
            } finally {
                BaseBundle.setShouldDefuse(true);
            }
        }
    }

    private void putCachedCallingRelation(String aimPkg, String callingPackage, int cachedUserId) {
        HashMap<String, Integer> hashMap = sCachedCallingRelationSelfLocked;
        synchronized (hashMap) {
            String callingRelationKey = aimPkg + PACKAGE_LINKER + callingPackage;
            hashMap.put(callingRelationKey, Integer.valueOf(cachedUserId));
            Slog.w(TAG, "putCachedCallingRelationm, callingRelationKey:" + callingRelationKey + ", cachedUserId:" + cachedUserId);
        }
    }

    private int getToUserIdFromCachedCallingRelation(String aimPkg, String callingPackage) {
        int cachedToUserId = ScreenRotationAnimationImpl.BLACK_SURFACE_INVALID_POSITION;
        HashMap<String, Integer> hashMap = sCachedCallingRelationSelfLocked;
        synchronized (hashMap) {
            String callingRelationKey = callingPackage + PACKAGE_LINKER + aimPkg;
            Integer toUserIdObj = hashMap.get(callingRelationKey);
            if (toUserIdObj != null) {
                cachedToUserId = toUserIdObj.intValue();
                hashMap.remove(callingRelationKey);
                Slog.w(TAG, "got callingRelationKey :" + callingRelationKey + ", cachedToUserId:" + cachedToUserId);
            }
        }
        return cachedToUserId;
    }

    private String getAimPkg(Intent intent) {
        ComponentName componentName;
        String aimPkg = intent.getPackage();
        if (aimPkg == null && (componentName = intent.getComponent()) != null) {
            return componentName.getPackageName();
        }
        return aimPkg;
    }

    private Intent getResolverActivity(Intent intent, String aimPkg, int requestCode, String callingPackage) {
        Intent resolverActivityIntent = new Intent(ACTION_XSPACE_RESOLVER_ACTIVITY_FROM_CORE);
        if (requestCode < 0) {
            if ((intent.getFlags() & 33554432) != 0) {
                resolverActivityIntent.addFlags(33554432);
            }
        } else {
            intent.addFlags(33554432);
        }
        intent.putExtra("miui.intent.extra.xspace_resolver_activity_calling_package", callingPackage);
        intent.putExtra(EXTRA_XSPACE_USERID_SELECTED, true);
        resolverActivityIntent.putExtra("android.intent.extra.xspace_resolver_activity_original_intent", intent);
        resolverActivityIntent.putExtra("android.intent.extra.xspace_resolver_activity_aim_package", aimPkg);
        resolverActivityIntent.setClassName("com.miui.securitycore", "com.miui.xspace.ui.activity.XSpaceResolveActivity");
        resolverActivityIntent.putExtra(EXTRA_XSPACE_RESOLVE_INTENT_AGAIN, true);
        return resolverActivityIntent;
    }

    private boolean checkCallXSpacePermission(String callingPkg) {
        ApplicationInfo appInfo;
        try {
            appInfo = AppGlobals.getPackageManager().getApplicationInfo(callingPkg, 0L, 0);
        } catch (RemoteException e) {
            Slog.e(TAG, "Failed to read package info of: " + callingPkg, e);
        }
        if ((appInfo.flags & 1) <= 0) {
            if (appInfo.uid > 1000) {
                return false;
            }
        }
        return true;
    }

    public void init(final Context context) {
        this.mContext = context;
        this.mResolver = context.getContentResolver();
        initXSpaceAppList();
        this.mResolver.registerContentObserver(Settings.Secure.getUriFor("xspace_enabled"), false, new ContentObserver(new Handler()) { // from class: com.miui.server.xspace.XSpaceManagerServiceImpl.1
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                super.onChange(selfChange);
                XSpaceManagerServiceImpl xSpaceManagerServiceImpl = XSpaceManagerServiceImpl.this;
                xSpaceManagerServiceImpl.mIsXSpaceActived = MiuiSettings.Secure.getBoolean(xSpaceManagerServiceImpl.mResolver, "xspace_enabled", false);
                Slog.w(XSpaceManagerServiceImpl.TAG, "update XSpace status, active:" + XSpaceManagerServiceImpl.this.mIsXSpaceActived);
            }
        });
        ContentObserver animListener = new ContentObserver(new Handler()) { // from class: com.miui.server.xspace.XSpaceManagerServiceImpl.2
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                super.onChange(selfChange);
                int settingStatus = Settings.Global.getInt(XSpaceManagerServiceImpl.this.mContext.getContentResolver(), XSpaceManagerServiceImpl.XSPACE_ANIMATION_STATUS, 2);
                if (settingStatus == 2) {
                    XSpaceManagerServiceImpl.this.mAnimStatus = MiuiSettings.SettingsCloudData.getCloudDataBoolean(context.getContentResolver(), XSpaceManagerServiceImpl.XSPACE_CLOUD_CONTROL_STATUS, Locale.getDefault().toString(), true);
                } else if (settingStatus == 1) {
                    XSpaceManagerServiceImpl.this.mAnimStatus = true;
                } else {
                    XSpaceManagerServiceImpl.this.mAnimStatus = false;
                }
                Slog.d(XSpaceManagerServiceImpl.TAG, "update XSpace Animation status, mAnimStatus = " + XSpaceManagerServiceImpl.this.mAnimStatus);
            }
        };
        this.mResolver.registerContentObserver(Settings.Global.getUriFor(XSPACE_ANIMATION_STATUS), false, animListener);
        animListener.onChange(false);
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.PACKAGE_ADDED");
        filter.addAction("android.intent.action.PACKAGE_REMOVED_INTERNAL");
        filter.addDataScheme("package");
        this.mContext.registerReceiverAsUser(this.mPackageChangedListener, UserHandle.of(999), filter, null, null);
        Slog.w(TAG, "XSpace init, active:" + this.mIsXSpaceActived);
        boolean hasXSpaceUser = CrossUserUtils.hasXSpaceUser(context);
        this.mIsXSpaceCreated = hasXSpaceUser;
        if (hasXSpaceUser) {
            SystemProperties.set(SYSTEM_PROP_XSPACE_CREATED, SplitScreenReporter.ACTION_ENTER_SPLIT);
            startXSpaceService(context, null);
        }
    }

    private void initXSpaceAppList() {
        ParceledListSlice<PackageInfo> slice = null;
        try {
            slice = AppGlobals.getPackageManager().getInstalledPackages(0L, 999);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        if (slice != null) {
            List<PackageInfo> appList = slice.getList();
            synchronized (sXSpaceInstalledPackagesSelfLocked) {
                for (PackageInfo pkgInfo : appList) {
                    ApplicationInfo appInfo = pkgInfo.applicationInfo;
                    if (!isSystemApp(appInfo) && !XSpaceConstant.GMS_RELATED_APPS.contains(pkgInfo.packageName)) {
                        sXSpaceInstalledPackagesSelfLocked.add(pkgInfo.packageName);
                    }
                }
            }
        }
        ArrayList<String> arrayList = sXSpaceInstalledPackagesSelfLocked;
        int size = arrayList.size();
        int i = XSPACE_APP_LIST_INIT_NUMBER;
        if (size > i) {
            this.mIsXSpaceActived = true;
        }
        MiuiSettings.Secure.putBoolean(this.mResolver, "xspace_enabled", this.mIsXSpaceActived);
        Slog.d(TAG, "initXSpaceAppList sXSpaceInstalledPackagesSelfLocked =" + arrayList.toString() + "    XSPACE_APP_LIST_INIT_NUMBER =" + i);
        Slog.w(TAG, "Reset XSpace enable, active:" + this.mIsXSpaceActived);
    }

    private boolean isSystemApp(ApplicationInfo appInfo) {
        return (appInfo.flags & 1) > 0 || appInfo.uid <= 1000;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class PackageChangedListener extends BroadcastReceiver {
        private PackageChangedListener() {
            XSpaceManagerServiceImpl.this = r1;
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            int userId = intent.getIntExtra("android.intent.extra.user_handle", ScreenRotationAnimationImpl.BLACK_SURFACE_INVALID_POSITION);
            if (userId == -10000) {
                Slog.w(XSpaceManagerServiceImpl.TAG, "Intent broadcast does not contain user handle: " + intent);
                return;
            }
            String action = intent.getAction();
            UserHandle user = new UserHandle(userId);
            String packageName = XSpaceManagerServiceImpl.this.getPackageName(intent);
            char c = 65535;
            switch (action.hashCode()) {
                case -1558050662:
                    if (action.equals("android.intent.action.PACKAGE_REMOVED_INTERNAL")) {
                        c = 1;
                        break;
                    }
                    break;
                case 1544582882:
                    if (action.equals("android.intent.action.PACKAGE_ADDED")) {
                        c = 0;
                        break;
                    }
                    break;
            }
            switch (c) {
                case 0:
                    XSpaceManagerServiceImpl.this.onPackageCallback(packageName, user, "android.intent.action.PACKAGE_ADDED");
                    return;
                case 1:
                    XSpaceManagerServiceImpl.this.onPackageCallback(packageName, user, "android.intent.action.PACKAGE_REMOVED");
                    return;
                default:
                    return;
            }
        }
    }

    public String getPackageName(Intent intent) {
        Uri uri = intent.getData();
        if (uri != null) {
            String pkg = uri.getSchemeSpecificPart();
            return pkg;
        }
        return null;
    }

    public void onPackageCallback(String packageName, UserHandle user, String action) {
        ApplicationInfo appInfo;
        Slog.w(TAG, "update XSpace App: packageName:" + packageName + ", user:" + user + ", action:" + action);
        if (XSpaceUserHandle.isXSpaceUser(user) && ("android.intent.action.PACKAGE_ADDED".equals(action) || "android.intent.action.PACKAGE_REMOVED".equals(action))) {
            this.mIsXSpaceCreated = true;
            SystemProperties.set(SYSTEM_PROP_XSPACE_CREATED, SplitScreenReporter.ACTION_ENTER_SPLIT);
            if ("android.intent.action.PACKAGE_ADDED".equals(action)) {
                setXSpaceApplicationHidden();
                if (TextUtils.equals(XIAOMI_GAMECENTER_SDK_PKGNAME, packageName) || TextUtils.equals(ORG_IFAA_AIDL_MANAGER_PKGNAME, packageName)) {
                    ArrayList<String> arrayList = sXSpaceInstalledPackagesSelfLocked;
                    synchronized (arrayList) {
                        updateXSpaceStatusLocked(true);
                        arrayList.add(packageName);
                    }
                }
                boolean isSystemApp = false;
                try {
                    appInfo = AppGlobals.getPackageManager().getApplicationInfo(packageName, 0L, 999);
                } catch (RemoteException e) {
                    Slog.d(TAG, "PMS died", e);
                }
                if (appInfo == null) {
                    return;
                }
                isSystemApp = isSystemApp(appInfo);
                if (isSystemApp || XSpaceConstant.GMS_RELATED_APPS.contains(packageName)) {
                    Slog.d(TAG, "XSpace ignore system or GMS package: " + packageName);
                    return;
                }
                ArrayList<String> arrayList2 = sXSpaceInstalledPackagesSelfLocked;
                synchronized (arrayList2) {
                    updateXSpaceStatusLocked(true);
                    arrayList2.add(packageName);
                }
                return;
            }
            MiuiSettings.XSpace.resetDefaultSetting(this.mContext, packageName);
            ArrayList<String> arrayList3 = sXSpaceInstalledPackagesSelfLocked;
            synchronized (arrayList3) {
                arrayList3.remove(packageName);
                updateXSpaceStatusLocked(false);
            }
            List<PackageInfo> appList = this.mContext.getPackageManager().getInstalledPackagesAsUser(64, 999);
            if (appList == null || appList.isEmpty()) {
                return;
            }
            boolean needRemoveGMS = true;
            Iterator<PackageInfo> it = appList.iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                PackageInfo packageInfo = it.next();
                if (XSpaceUtils.isGMSRequired(this.mContext, packageInfo.packageName)) {
                    needRemoveGMS = false;
                    break;
                }
            }
            if (needRemoveGMS) {
                disableGMSApps();
            }
        } else if (user.getIdentifier() == 0 && "android.intent.action.PACKAGE_ADDED".equals(action) && MiuiSettings.XSpace.sCompeteXSpaceApps.contains(packageName) && needSetInstallXSpaceGuideTaskForCompete(this.mContext) && !CrossUserUtils.hasXSpaceUser(this.mContext)) {
            startXSpaceService(this.mContext, "param_intent_value_compete_install_xspace_guide");
        }
    }

    private void disableGMSApps() {
        IPackageManager iPackageManager = AppGlobals.getPackageManager();
        Iterator it = XSpaceConstant.GMS_RELATED_APPS.iterator();
        while (it.hasNext()) {
            String pkgName = (String) it.next();
            if (XSpaceUserHandle.isAppInXSpace(this.mContext, pkgName)) {
                try {
                    iPackageManager.deletePackageAsUser(pkgName, -1, (IPackageDeleteObserver) null, 999, 4);
                } catch (RemoteException e) {
                    Slog.e(TAG, "Unable to delete package: " + pkgName, e);
                } catch (IllegalArgumentException e2) {
                    Slog.e(TAG, "Unknown package: " + pkgName, e2);
                }
            }
        }
    }

    private void setXSpaceApplicationHidden() {
        if (!Build.IS_INTERNATIONAL_BUILD) {
            return;
        }
        try {
            Iterator<String> it = sXSpaceApplicationList.iterator();
            while (it.hasNext()) {
                String pkgName = it.next();
                if (!AppGlobals.getPackageManager().getApplicationHiddenSettingAsUser(pkgName, 999)) {
                    AppGlobals.getPackageManager().setApplicationHiddenSettingAsUser(pkgName, true, 999);
                }
            }
        } catch (RemoteException e) {
            Slog.d(TAG, "hidden xspace error", e);
        }
    }

    private void updateXSpaceStatusLocked(boolean isXSpaceActive) {
        ArrayList<String> arrayList = sXSpaceInstalledPackagesSelfLocked;
        int size = arrayList.size();
        int i = XSPACE_APP_LIST_INIT_NUMBER;
        if (size == i) {
            Slog.d(TAG, "updateXSpaceStatusLocked sXSpaceInstalledPackagesSelfLocked =" + arrayList.toString() + "    XSPACE_APP_LIST_INIT_NUMBER =" + i);
            Slog.w(TAG, "update XSpace Enable = " + isXSpaceActive);
            MiuiSettings.Secure.putBoolean(this.mResolver, "xspace_enabled", isXSpaceActive);
        }
    }

    private void startXSpaceService(Context context, String extra) {
        Intent intent = new Intent();
        intent.setComponent(ComponentName.unflattenFromString(XSPACE_SERVICE_COMPONENT));
        if (extra != null) {
            intent.putExtra("param_intent_key_has_extra", extra);
        }
        context.startService(intent);
    }

    private boolean needSetBootXSpaceGuideTaskForCompete(Context context) {
        return MiuiSettings.XSpace.getGuideNotificationTimes(context, "key_xspace_boot_guide_times") < 1;
    }

    private boolean needSetInstallXSpaceGuideTaskForCompete(Context context) {
        return MiuiSettings.XSpace.getGuideNotificationTimes(context, "key_xspace_compete_guide_times") < 1;
    }

    private boolean isComponentEnabled(ActivityInfo info, int userId) {
        if (info == null) {
            return true;
        }
        boolean enabled = false;
        ComponentName compname = new ComponentName(info.packageName, info.name);
        try {
            if (AppGlobals.getPackageManager().getActivityInfo(compname, 0L, userId) != null) {
                enabled = true;
            }
        } catch (RemoteException e) {
            enabled = false;
        }
        if (!enabled) {
            Slog.d(TAG, "Component not enabled: " + compname + "  in user " + userId);
        }
        return enabled;
    }

    public void setXSpaceCreated(int userId, boolean isXSpaceCreated) {
        if (userId == 999) {
            this.mIsXSpaceCreated = isXSpaceCreated;
        }
    }

    public boolean isXSpaceActive() {
        return this.mIsXSpaceActived;
    }

    public int[] computeGids(int userId, int[] gids, String packageName) {
        if (mediaAccessRequired.contains(packageName) && gids != null) {
            gids = ArrayUtils.appendInt(gids, 1023);
        }
        int i = 0;
        if (userId != 0 && SecSpaceManagerService.isDataTransferProcess(userId, packageName)) {
            int[] gids2 = ArrayUtils.appendInt(gids, UserHandle.getUserGid(0));
            if (this.mIsXSpaceCreated) {
                return ArrayUtils.appendInt(gids2, XSpaceUserHandle.XSPACE_SHARED_USER_GID);
            }
            return gids2;
        } else if (XSpaceConstant.XSPACE_PACKAGES_SHARED_GID.contains(packageName)) {
            return ArrayUtils.appendInt(gids, XSpaceUserHandle.XSPACE_SHARED_USER_GID);
        } else {
            if (!this.mIsXSpaceCreated || gids == null) {
                return gids;
            }
            if (XSpaceUserHandle.isXSpaceUserId(userId)) {
                int length = gids.length;
                while (i < length) {
                    int gid = gids[i];
                    if (gid != XSpaceUserHandle.XSPACE_SHARED_USER_GID) {
                        i++;
                    } else {
                        return ArrayUtils.appendInt(gids, XSpaceUserHandle.OWNER_SHARED_USER_GID);
                    }
                }
                return gids;
            } else if (userId == 0) {
                int length2 = gids.length;
                while (i < length2) {
                    int gid2 = gids[i];
                    if (gid2 != XSpaceUserHandle.OWNER_SHARED_USER_GID) {
                        i++;
                    } else {
                        return ArrayUtils.appendInt(gids, XSpaceUserHandle.XSPACE_SHARED_USER_GID);
                    }
                }
                return gids;
            } else {
                return gids;
            }
        }
    }

    public boolean checkExternalStorageForXSpace(Context context, int uid, String pkgName) {
        if (!XSpaceUserHandle.isUidBelongtoXSpace(uid)) {
            return false;
        }
        try {
            ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(pkgName, 0);
            return (appInfo.flags & 1) > 0;
        } catch (PackageManager.NameNotFoundException e) {
            Slog.e(TAG, "Failed to get package info for " + pkgName, e);
            return false;
        }
    }

    public boolean shouldInstallInXSpace(UserHandle installUser, int userId) {
        if (installUser == null && XSpaceUserHandle.isXSpaceUserId(userId)) {
            return false;
        }
        return true;
    }

    public int getUserTypeCount(int[] userIds) {
        int result = userIds.length;
        for (int i : userIds) {
            if (i == 999) {
                result--;
            }
        }
        return result;
    }

    public int checkAndGetXSpaceUserId(int flags, int defUserId) {
        return XSpaceUserHandle.checkAndGetXSpaceUserId(flags, defUserId);
    }

    public void handleWindowManagerAndUserLru(Context context, int userId, int userIdOrig, int oldUserId, WindowManagerService mWindowManager, int[] mCurrentProfileIds) {
        mWindowManager.setCurrentUser(userIdOrig, mCurrentProfileIds);
        int privacyUserId = Settings.Secure.getInt(context.getContentResolver(), "second_user_id", ScreenRotationAnimationImpl.BLACK_SURFACE_INVALID_POSITION);
        int kidSpaceUserId = Settings.Secure.getInt(context.getContentResolver(), "kid_user_id", ScreenRotationAnimationImpl.BLACK_SURFACE_INVALID_POSITION);
        Slog.d(TAG, "privacyUserId :" + privacyUserId + " userId:" + userId + " userIdOrig:" + userIdOrig + " oldUserId:" + oldUserId + " kidSpaceUserId:" + kidSpaceUserId);
        if (UserHandle.getAppId(sSwitchUserCallingUid) == 1000 && ((privacyUserId == oldUserId && userId == 0) || ((oldUserId == 0 && userId == privacyUserId) || ((kidSpaceUserId == oldUserId && userId == 0) || (oldUserId == 0 && userId == kidSpaceUserId))))) {
            Slog.d(TAG, "switch without lock");
        } else {
            mWindowManager.lockNow((Bundle) null);
        }
    }

    public boolean showSwitchingDialog(final ActivityManagerService ams, final Context context, final int currentUserId, final int targetUserId, final Handler handler, final int userSwitchUiMessage) {
        setSwitchUserCallingUid(Binder.getCallingUid());
        String pkgName = ProcessUtils.getPackageNameByPid(Binder.getCallingPid());
        if (pkgName != null && (pkgName.equals("com.android.systemui") || pkgName.equals("com.android.keyguard"))) {
            return false;
        }
        handler.post(new Runnable() { // from class: com.miui.server.xspace.XSpaceManagerServiceImpl.3
            @Override // java.lang.Runnable
            public void run() {
                int i;
                int secondSpaceId = Settings.Secure.getIntForUser(context.getContentResolver(), "second_user_id", ScreenRotationAnimationImpl.BLACK_SURFACE_INVALID_POSITION, 0);
                int kidSpaceUserId = Settings.Secure.getIntForUser(context.getContentResolver(), "kid_user_id", ScreenRotationAnimationImpl.BLACK_SURFACE_INVALID_POSITION, 0);
                int i2 = targetUserId;
                if ((i2 == secondSpaceId && currentUserId == 0) || ((i = currentUserId) == secondSpaceId && i2 == 0)) {
                    XSpaceManagerServiceImpl.mDialog = new SecondSpaceSwitchingDialog(ams, context, targetUserId);
                    XSpaceManagerServiceImpl.mDialog.show();
                } else if ((i2 == kidSpaceUserId && i == 0) || (i == kidSpaceUserId && i2 == 0)) {
                    XSpaceManagerServiceImpl.mDialog = new KidSpaceSwitchingDialog(ams, context, targetUserId);
                    XSpaceManagerServiceImpl.mDialog.show();
                } else {
                    UserInfo currentUserInfo = XSpaceManagerServiceImpl.getUserManager().getUserInfo(currentUserId);
                    UserInfo targetUserInfo = XSpaceManagerServiceImpl.getUserManager().getUserInfo(targetUserId);
                    Pair<UserInfo, UserInfo> userNames = new Pair<>(currentUserInfo, targetUserInfo);
                    Handler handler2 = handler;
                    handler2.sendMessage(handler2.obtainMessage(userSwitchUiMessage, userNames));
                }
            }
        });
        ams.registerUserSwitchObserver(mUserSwitchObserver, TAG);
        return true;
    }

    private void setSwitchUserCallingUid(int uid) {
        sSwitchUserCallingUid = uid;
    }

    public static UserManagerService getUserManager() {
        if (mUserManager == null) {
            IBinder b = ServiceManager.getService("user");
            mUserManager = IUserManager.Stub.asInterface(b);
        }
        return mUserManager;
    }

    public boolean needCheckUser(ProviderInfo cpi, String processName, int userId, boolean checkUser) {
        return CrossUserUtils.needCheckUser(cpi, processName, userId, checkUser);
    }

    public int getDefaultUserId() {
        try {
            IUserManager um = IUserManager.Stub.asInterface(ServiceManager.getService("user"));
            for (UserInfo userInfo : um.getUsers(false, false, false)) {
                if (XSpaceUserHandle.isXSpaceUser(userInfo)) {
                    return ActivityManager.getCurrentUser();
                }
            }
            return -1;
        } catch (RemoteException e) {
            return -1;
        }
    }

    public int getXSpaceUserId() {
        return 999;
    }

    public boolean shouldCrossXSpace(String packageName, int userId) {
        boolean z = false;
        if (userId == 999) {
            long origId = Binder.clearCallingIdentity();
            try {
                if (AppGlobals.getPackageManager().getPackageInfo(packageName, 0L, userId) == null) {
                    PackageInfo packageInfo = AppGlobals.getPackageManager().getPackageInfo(packageName, 0L, 0);
                    if (packageInfo != null && packageInfo.applicationInfo != null) {
                        if (packageInfo.applicationInfo.enabled) {
                            z = true;
                        }
                    }
                    Binder.restoreCallingIdentity(origId);
                    return z;
                }
            } catch (RemoteException e) {
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(origId);
                throw th;
            }
            Binder.restoreCallingIdentity(origId);
        }
        return false;
    }

    private ActivityInfo resolveActivity(Intent intent, ActivityTaskSupervisor stack, String resolvedType, int startFlags, ProfilerInfo profilerInfo, int userId, int callingUid) {
        Exception exception;
        try {
        } catch (Exception e) {
            exception = e;
        }
        try {
            ActivityInfo activityInfo = (ActivityInfo) ReflectUtil.callObjectMethod(stack, ActivityInfo.class, "resolveActivity", new Class[]{Intent.class, String.class, Integer.TYPE, ProfilerInfo.class, Integer.TYPE, Integer.TYPE}, intent, resolvedType, Integer.valueOf(startFlags), profilerInfo, Integer.valueOf(userId), Integer.valueOf(callingUid));
            return activityInfo;
        } catch (Exception e2) {
            exception = e2;
            Slog.e(TAG, "Call resolveIntent fail.", exception);
            return null;
        }
    }
}
