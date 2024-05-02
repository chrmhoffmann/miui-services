package com.android.server.clipboard;

import android.content.ClipData;
import android.content.ClipboardRuleInfo;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import com.android.internal.app.IAppOpsService;
import com.android.internal.os.BackgroundThread;
import com.android.server.appop.AppOpsService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import miui.os.Build;
/* loaded from: classes.dex */
public class ClipboardChecker {
    private static final int DEFAULT_FUNCTION;
    public static final int MATCH_APP_AND_RULE = 0;
    public static final int MATCH_APP_MISMATCH_RULE = 1;
    private static final boolean MIUI12_5_PRIVACY_ENABLE;
    private static final String MI_LAB_AI_CLIPBOARD_ENABLE = "mi_lab_ai_clipboard_enable";
    public static final int SYSTEM_SPECIAL_ALLOW = 3;
    private static final String TAG = "ClipboardServiceI";
    public static final int UNKNOWN = 2;
    private static final Set<String> sAllowClipboardSet;
    private static volatile ClipboardChecker sInstance;
    private boolean mAiClipboardEnable;
    private AppOpsService mAppOpsService;
    private ClipData mClipItemData;
    private int mMatchHistoryClipData = 0;
    private final List<Integer> mMatchHistoryCaller = new ArrayList();
    private boolean mFirstObserve = true;
    private final Object mLock = new Object();
    private final Map<String, List<Pattern>> mPatternMap = new HashMap();
    private final Map<String, String> mTraceClickInfoMap = new HashMap();
    private SparseArray<ClipData> mStashUidClip = new SparseArray<>();
    private Handler mHandler = new Handler(BackgroundThread.get().getLooper()) { // from class: com.android.server.clipboard.ClipboardChecker.1
        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            synchronized (ClipboardChecker.this.mLock) {
                ClipboardChecker.this.mStashUidClip.remove(msg.what);
            }
        }
    };

    static {
        boolean z = false;
        if (!Build.IS_INTERNATIONAL_BUILD && Build.VERSION.SDK_INT >= 29 && SystemProperties.getInt("ro.miui.ui.version.code", 0) >= 11) {
            z = true;
        }
        MIUI12_5_PRIVACY_ENABLE = z;
        DEFAULT_FUNCTION = !miui.os.Build.IS_STABLE_VERSION ? 1 : 0;
        HashSet hashSet = new HashSet();
        sAllowClipboardSet = hashSet;
        hashSet.add("com.android.browser");
    }

    public static ClipboardChecker getInstance() {
        if (sInstance == null) {
            synchronized (ClipboardChecker.class) {
                if (sInstance == null) {
                    sInstance = new ClipboardChecker();
                }
            }
        }
        return sInstance;
    }

    private ClipboardChecker() {
    }

    public int matchClipboardRule(String pkgName, int callerUid, CharSequence content, int clipCode, boolean isSystem) {
        if (content == null) {
            return 2;
        }
        if (isSystem && sAllowClipboardSet.contains(pkgName)) {
            return 3;
        }
        if (TextUtils.isEmpty(pkgName) || !this.mPatternMap.containsKey(pkgName)) {
            return 2;
        }
        for (Pattern pattern : this.mPatternMap.get(pkgName)) {
            if (pattern.matcher(content).find()) {
                if (isSystem && "com.android.quicksearchbox".equals(pkgName) && this.mMatchHistoryClipData == clipCode && this.mMatchHistoryCaller.contains(Integer.valueOf(callerUid))) {
                    Log.i(TAG, "MIUILOG- Permission Denied when read clipboard repeatedly, caller " + callerUid);
                    return 1;
                }
                if (this.mMatchHistoryClipData != clipCode) {
                    this.mMatchHistoryClipData = clipCode;
                    this.mMatchHistoryCaller.clear();
                }
                this.mMatchHistoryCaller.add(Integer.valueOf(callerUid));
                return 0;
            }
        }
        return 1;
    }

    public void updateClipboardPatterns(List<ClipboardRuleInfo> ruleInfoList) {
        synchronized (this) {
            if (ruleInfoList != null) {
                if (ruleInfoList.size() > 0) {
                    Map<String, List<Pattern>> patternMap = null;
                    Map<String, String> traceClickInfoMap = null;
                    for (ClipboardRuleInfo item : ruleInfoList) {
                        try {
                            if (item.getType() == 1) {
                                List<Pattern> patternList = new ArrayList<>();
                                for (String ruleItem : item.getRuleInfo()) {
                                    patternList.add(Pattern.compile(ruleItem));
                                }
                                if (patternMap == null) {
                                    patternMap = new HashMap<>();
                                }
                                patternMap.put(item.getPkgName(), patternList);
                            } else if (item.getType() == 2 && item.getRuleInfo().size() > 0) {
                                if (traceClickInfoMap == null) {
                                    traceClickInfoMap = new HashMap<>();
                                }
                                traceClickInfoMap.put(item.getPkgName(), (String) item.getRuleInfo().get(0));
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "updateClipboardPatterns error", e);
                            return;
                        }
                    }
                    if (patternMap != null) {
                        this.mPatternMap.clear();
                        this.mPatternMap.putAll(patternMap);
                    }
                    if (traceClickInfoMap != null) {
                        this.mTraceClickInfoMap.clear();
                        this.mTraceClickInfoMap.putAll(traceClickInfoMap);
                    }
                }
            }
        }
    }

    public void applyReadClipboardOperation(boolean allow, int uid, String pkgName, int type) {
        getAppOpsService().onApplyOperation(uid, pkgName, 29, !allow ? 1 : 0, type);
    }

    private AppOpsService getAppOpsService() {
        if (this.mAppOpsService == null) {
            IBinder b = ServiceManager.getService("appops");
            this.mAppOpsService = IAppOpsService.Stub.asInterface(b);
        }
        return this.mAppOpsService;
    }

    public ClipData getClipItemData() {
        return this.mClipItemData;
    }

    public ClipData getStashClip(int uid) {
        ClipData clipData;
        synchronized (this) {
            clipData = this.mStashUidClip.get(uid, ClipboardServiceStub.get().EMPTY_CLIP);
        }
        return clipData;
    }

    public void stashClip(int uid, ClipData content) {
        this.mClipItemData = content;
        synchronized (this.mLock) {
            this.mStashUidClip.put(uid, content);
            this.mHandler.removeMessages(uid);
        }
    }

    public void removeStashClipLater(int uid) {
        if (this.mHandler.hasMessages(uid) || !hasStash(uid)) {
            return;
        }
        this.mHandler.sendEmptyMessageDelayed(uid, 1000L);
    }

    public boolean hasStash(int uid) {
        return this.mStashUidClip.indexOfKey(uid) >= 0;
    }

    public void updateClipItemData(ClipData content) {
        this.mClipItemData = content;
    }

    public Map getClipboardClickTrack() {
        return this.mTraceClickInfoMap;
    }

    public boolean isAiClipboardEnable(Context context) {
        boolean z = MIUI12_5_PRIVACY_ENABLE;
        if (z && this.mFirstObserve) {
            long identity = Binder.clearCallingIdentity();
            try {
                final ContentResolver resolver = context.getContentResolver();
                this.mAiClipboardEnable = Settings.Secure.getIntForUser(resolver, MI_LAB_AI_CLIPBOARD_ENABLE, DEFAULT_FUNCTION, -2) == 1;
                resolver.registerContentObserver(Settings.Secure.getUriFor(MI_LAB_AI_CLIPBOARD_ENABLE), false, new ContentObserver(BackgroundThread.getHandler()) { // from class: com.android.server.clipboard.ClipboardChecker.2
                    @Override // android.database.ContentObserver
                    public void onChange(boolean selfChange, Uri uri) {
                        super.onChange(selfChange, uri);
                        ClipboardChecker clipboardChecker = ClipboardChecker.this;
                        boolean z2 = true;
                        if (Settings.Secure.getIntForUser(resolver, ClipboardChecker.MI_LAB_AI_CLIPBOARD_ENABLE, ClipboardChecker.DEFAULT_FUNCTION, -2) != 1) {
                            z2 = false;
                        }
                        clipboardChecker.mAiClipboardEnable = z2;
                    }
                }, -1);
                Binder.restoreCallingIdentity(identity);
                this.mFirstObserve = false;
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(identity);
                throw th;
            }
        }
        return z && this.mAiClipboardEnable;
    }
}
