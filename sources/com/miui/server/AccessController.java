package com.miui.server;

import android.app.KeyguardManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.MiuiSettings;
import android.security.MiuiLockPatternUtils;
import android.server.am.SplitScreenReporter;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Pair;
import android.util.Slog;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockPatternView;
import com.android.internal.widget.LockscreenCredential;
import com.android.server.wm.FoldablePackagePolicy;
import com.miui.server.input.stylus.MiuiStylusPageKeyListener;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.json.JSONObject;
/* loaded from: classes.dex */
public class AccessController {
    private static final String ACCESS_CONTROL = "access_control.key";
    private static final String ACCESS_CONTROL_PASSWORD_TYPE_KEY = "access_control_password_type.key";
    private static final String APPLOCK_WHILTE = "applock_whilte";
    public static final boolean DEBUG = false;
    private static final String GAMEBOOSTER_ANTIMSG = "gamebooster_antimsg";
    public static final String PACKAGE_CAMERA = "com.android.camera";
    public static final String PACKAGE_GALLERY = "com.miui.gallery";
    public static final String PACKAGE_MEITU_CAMERA = "com.mlab.cam";
    public static final String PACKAGE_SYSTEMUI = "com.android.systemui";
    private static final String PASSWORD_TYPE_PATTERN = "pattern";
    public static final String SKIP_INTERCEPT_ACTIVITY_GALLERY_EDIT = "com.miui.gallery.editor.photo.screen.home.ScreenEditorActivity";
    public static final String SKIP_INTERCEPT_ACTIVITY_GALLERY_EXTRA = "com.miui.gallery.activity.ExternalPhotoPageActivity";
    public static final String SKIP_INTERCEPT_ACTIVITY_GALLERY_EXTRA_TRANSLUCENT = "com.miui.gallery.activity.TranslucentExternalPhotoPageActivity";
    private static final String SYSTEM_DIRECTORY = "/system/";
    private static final String TAG = "AccessController";
    private static final long UPDATE_EVERY_DELAY = 43200000;
    private static final long UPDATE_FIRT_DELAY = 180000;
    private static final int UPDATE_WHITE_LIST = 1;
    private static final String WECHAT_VIDEO_ACTIVITY_CLASSNAME = "com.tencent.mm.plugin.voip.ui.VideoActivity";
    private static Method mPasswordToHash;
    private Context mContext;
    private final Object mFileWriteLock = new Object();
    private KeyguardManager mKeyguardManager;
    private LockPatternUtils mLockPatternUtils;
    private WorkHandler mWorkHandler;
    private static ArrayMap<String, ArrayList<Intent>> mSkipList = new ArrayMap<>();
    private static ArrayMap<String, ArrayList<Intent>> mAntimsgInterceptList = new ArrayMap<>();

    static {
        ArrayList<Pair<String, String>> passList = new ArrayList<>();
        passList.add(new Pair<>("com.tencent.mobileqq", "com.tencent.av.ui.VideoInviteLock"));
        passList.add(new Pair<>("com.tencent.mobileqq", "com.tencent.av.ui.VideoInviteFull"));
        passList.add(new Pair<>("com.tencent.mm", WECHAT_VIDEO_ACTIVITY_CLASSNAME));
        passList.add(new Pair<>("com.tencent.mm", "com.tencent.mm.plugin.multitalk.ui.MultiTalkMainUI"));
        passList.add(new Pair<>("com.tencent.mm", "com.tencent.mm.plugin.base.stub.UIEntryStub"));
        passList.add(new Pair<>("com.tencent.mm", "com.tencent.mm.plugin.webview.ui.tools.SDKOAuthUI"));
        passList.add(new Pair<>("com.tencent.mm", "com.tencent.mm.plugin.base.stub.WXPayEntryActivity"));
        passList.add(new Pair<>("com.tencent.mm", "com.tencent.mm.plugin.wallet_index.ui.OrderHandlerUI"));
        passList.add(new Pair<>("com.whatsapp", "com.whatsapp.VoipActivity"));
        passList.add(new Pair<>("com.whatsapp", "com.whatsapp.voipcalling.VoipActivityV2"));
        passList.add(new Pair<>("jp.naver.line.android", "jp.naver.line.android.freecall.FreeCallActivity"));
        passList.add(new Pair<>("com.bbm", "com.bbm.ui.voice.activities.IncomingCallActivity"));
        passList.add(new Pair<>("com.xiaomi.channel", "com.xiaomi.channel.voip.VoipCallActivity"));
        passList.add(new Pair<>("com.facebook.orca", "com.facebook.rtc.activities.WebrtcIncallActivity"));
        passList.add(new Pair<>("com.bsb.hike", "com.bsb.hike.voip.view.VoIPActivity"));
        passList.add(new Pair<>("com.eg.android.AlipayGphone", "com.alipay.android.app.TransProcessPayActivity"));
        passList.add(new Pair<>("com.eg.android.AlipayGphone", "com.alipay.mobile.security.login.ui.AlipayUserLoginActivity"));
        passList.add(new Pair<>("com.eg.android.AlipayGphone", "com.alipay.mobile.bill.detail.ui.EmptyActivity_"));
        passList.add(new Pair<>("com.xiaomi.smarthome", "com.xiaomi.smarthome.miio.activity.ClientAllLockedActivity"));
        passList.add(new Pair<>("com.android.settings", "com.android.settings.FallbackHome"));
        passList.add(new Pair<>("com.android.mms", "com.android.mms.ui.DummyActivity"));
        passList.add(new Pair<>("com.android.mms", "com.android.mms.ui.ComposeMessageRouterActivity"));
        passList.add(new Pair<>("com.xiaomi.jr", "com.xiaomi.jr.EntryActivity"));
        Iterator<Pair<String, String>> it = passList.iterator();
        while (it.hasNext()) {
            Pair<String, String> pair = it.next();
            ArrayList<Intent> intents = mSkipList.get(pair.first);
            if (intents == null) {
                intents = new ArrayList<>(1);
                mSkipList.put((String) pair.first, intents);
            }
            Intent intent = new Intent();
            intent.setComponent(new ComponentName((String) pair.first, (String) pair.second));
            intents.add(intent);
        }
        ArrayList<Pair<String, String>> interceptList = new ArrayList<>();
        interceptList.add(new Pair<>("com.tencent.mobileqq", "com.tencent.av.ui.VideoInviteLock"));
        interceptList.add(new Pair<>("com.tencent.mobileqq", "com.tencent.av.ui.VideoInviteFull"));
        interceptList.add(new Pair<>("com.tencent.mm", WECHAT_VIDEO_ACTIVITY_CLASSNAME));
        Iterator<Pair<String, String>> it2 = interceptList.iterator();
        while (it2.hasNext()) {
            Pair<String, String> pair2 = it2.next();
            ArrayList<Intent> intents2 = mAntimsgInterceptList.get(pair2.first);
            if (intents2 == null) {
                intents2 = new ArrayList<>(1);
                mAntimsgInterceptList.put((String) pair2.first, intents2);
            }
            Intent intent2 = new Intent();
            intent2.setComponent(new ComponentName((String) pair2.first, (String) pair2.second));
            intents2.add(intent2);
        }
        try {
            if (Build.VERSION.SDK_INT > 30) {
                Log.i(TAG, "Nowhere to call this method, passwordToHash should not init");
                return;
            }
            if (Build.VERSION.SDK_INT > 28) {
                mPasswordToHash = LockPatternUtils.class.getDeclaredMethod("legacyPasswordToHash", byte[].class, Integer.TYPE);
            } else if (Build.VERSION.SDK_INT == 28) {
                mPasswordToHash = LockPatternUtils.class.getDeclaredMethod("legacyPasswordToHash", String.class, Integer.TYPE);
            } else {
                mPasswordToHash = LockPatternUtils.class.getDeclaredMethod("passwordToHash", String.class, Integer.TYPE);
            }
            mPasswordToHash.setAccessible(true);
        } catch (Exception e) {
            Log.e(TAG, " passwordToHash static invoke error", e);
        }
    }

    /* loaded from: classes.dex */
    public class WorkHandler extends Handler {
        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public WorkHandler(Looper looper) {
            super(looper);
            AccessController.this = r1;
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    AccessController.this.updateWhiteList();
                    return;
                default:
                    return;
            }
        }
    }

    public AccessController(Context context, Looper looper) {
        this.mContext = context;
        this.mKeyguardManager = (KeyguardManager) context.getSystemService(MiuiStylusPageKeyListener.SCENE_KEYGUARD);
        WorkHandler workHandler = new WorkHandler(looper);
        this.mWorkHandler = workHandler;
        workHandler.sendEmptyMessageDelayed(1, UPDATE_FIRT_DELAY);
        this.mLockPatternUtils = new LockPatternUtils(context);
    }

    public boolean filterIntentLocked(boolean isSkipIntent, String packageName, Intent intent) {
        ArrayList<Intent> intents;
        String fullName;
        if (intent == null) {
            return false;
        }
        synchronized (this) {
            if (isSkipIntent) {
                intents = mSkipList.get(packageName);
            } else {
                intents = mAntimsgInterceptList.get(packageName);
            }
            if (intents == null) {
                return false;
            }
            String action = intent.getAction();
            ComponentName component = intent.getComponent();
            if (action != null) {
                Iterator<Intent> it = intents.iterator();
                while (it.hasNext()) {
                    Intent i = it.next();
                    if (action.equals(i.getAction())) {
                        return true;
                    }
                }
            }
            if (component != null) {
                String cls = component.getClassName();
                if (cls == null) {
                    return false;
                }
                if (cls.charAt(0) == '.') {
                    fullName = component.getPackageName() + cls;
                } else {
                    fullName = cls;
                }
                if (!isSkipIntent && WECHAT_VIDEO_ACTIVITY_CLASSNAME.equals(cls) && (intent.getFlags() & (-268435457)) == 0) {
                    return false;
                }
                Iterator<Intent> it2 = intents.iterator();
                while (it2.hasNext()) {
                    Intent i2 = it2.next();
                    ComponentName c = i2.getComponent();
                    if (c != null && fullName.equals(c.getClassName())) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    public boolean skipActivity(Intent intent, String callingPkg) {
        if (intent != null) {
            try {
                ComponentName componentName = intent.getComponent();
                if (componentName != null) {
                    String packageName = componentName.getPackageName();
                    String activity = componentName.getClassName();
                    if (!isOpenedPkg(callingPkg) || TextUtils.isEmpty(packageName) || TextUtils.isEmpty(activity) || !PACKAGE_GALLERY.equals(packageName) || !isOpenedActivity(activity)) {
                        return false;
                    }
                    return intent.getBooleanExtra("skip_interception", false);
                }
            } catch (Throwable e) {
                Slog.e(TAG, "can not getStringExtra" + e);
            }
        }
        return false;
    }

    private static boolean isOpenedPkg(String callingPkg) {
        return PACKAGE_GALLERY.equals(callingPkg) || "com.android.systemui".equals(callingPkg) || PACKAGE_CAMERA.equals(callingPkg) || PACKAGE_MEITU_CAMERA.equals(callingPkg);
    }

    private static boolean isOpenedActivity(String activity) {
        return SKIP_INTERCEPT_ACTIVITY_GALLERY_EXTRA.equals(activity) || SKIP_INTERCEPT_ACTIVITY_GALLERY_EXTRA_TRANSLUCENT.equals(activity) || SKIP_INTERCEPT_ACTIVITY_GALLERY_EDIT.equals(activity);
    }

    private void setAccessControlPattern(String pattern, int userId) {
        byte[] hash = null;
        if (pattern != null) {
            List<LockPatternView.Cell> stringToPattern = MiuiLockPatternUtils.stringToPattern(pattern);
            hash = MiuiLockPatternUtils.patternToHash(stringToPattern);
        }
        String filePath = getFilePathForUser(userId, ACCESS_CONTROL);
        writeFile(filePath, hash);
    }

    public void setAccessControlPassword(String passwordType, String password, int userId) {
        if (PASSWORD_TYPE_PATTERN.equals(passwordType)) {
            setAccessControlPattern(password, userId);
            setAccessControlPasswordType(passwordType, userId);
            return;
        }
        byte[] hash = null;
        if (password != null) {
            hash = passwordToHash(password, userId);
        }
        String filePath = getFilePathForUser(userId, ACCESS_CONTROL);
        writeFile(filePath, hash);
        setAccessControlPasswordType(passwordType, userId);
    }

    private boolean checkAccessControlPattern(String pattern, int userId) {
        if (pattern == null) {
            return false;
        }
        List<LockPatternView.Cell> stringToPattern = MiuiLockPatternUtils.stringToPattern(pattern);
        byte[] hash = MiuiLockPatternUtils.patternToHash(stringToPattern);
        String filePath = getFilePathForUser(userId, ACCESS_CONTROL);
        byte[] readFile = readFile(filePath);
        return Arrays.equals(readFile, hash);
    }

    public boolean checkAccessControlPassword(String passwordType, String password, int userId) {
        if (password == null || passwordType == null) {
            return false;
        }
        if (PASSWORD_TYPE_PATTERN.equals(passwordType)) {
            return checkAccessControlPattern(password, userId);
        }
        byte[] hash = passwordToHash(password, userId);
        String filePath = getFilePathForUser(userId, ACCESS_CONTROL);
        byte[] readFile = readFile(filePath);
        return Arrays.equals(readFile, hash);
    }

    private boolean haveAccessControlPattern(int userId) {
        boolean z;
        String filePath = getFilePathForUser(userId, ACCESS_CONTROL);
        synchronized (this.mFileWriteLock) {
            File file = new File(filePath);
            z = file.exists() && file.length() > 0;
        }
        return z;
    }

    public boolean haveAccessControlPassword(int userId) {
        boolean z;
        String filePathType = getFilePathForUser(userId, ACCESS_CONTROL_PASSWORD_TYPE_KEY);
        String filePathPassword = getFilePathForUser(userId, ACCESS_CONTROL);
        synchronized (this.mFileWriteLock) {
            File fileType = new File(filePathType);
            File filePassword = new File(filePathPassword);
            z = fileType.exists() && filePassword.exists() && fileType.length() > 0 && filePassword.length() > 0;
        }
        return z;
    }

    public String getAccessControlPasswordType(int userId) {
        String filePath = getFilePathForUser(userId, ACCESS_CONTROL_PASSWORD_TYPE_KEY);
        if (filePath == null) {
            return PASSWORD_TYPE_PATTERN;
        }
        return readTypeFile(filePath);
    }

    public void updatePasswordTypeForPattern(int userId) {
        if (haveAccessControlPattern(userId) && !haveAccessControlPasswordType(userId)) {
            setAccessControlPasswordType(PASSWORD_TYPE_PATTERN, userId);
            Log.d(TAG, "update password type succeed");
        }
    }

    private void setAccessControlPasswordType(String passwordType, int userId) {
        String filePath = getFilePathForUser(userId, ACCESS_CONTROL_PASSWORD_TYPE_KEY);
        writeTypeFile(filePath, passwordType);
    }

    private boolean haveAccessControlPasswordType(int userId) {
        boolean z;
        String filePath = getFilePathForUser(userId, ACCESS_CONTROL_PASSWORD_TYPE_KEY);
        synchronized (this.mFileWriteLock) {
            File file = new File(filePath);
            z = file.exists() && file.length() > 0;
        }
        return z;
    }

    private byte[] readFile(String name) {
        byte[] stored;
        String str;
        String str2;
        synchronized (this.mFileWriteLock) {
            RandomAccessFile raf = null;
            stored = null;
            try {
                raf = new RandomAccessFile(name, FoldablePackagePolicy.POLICY_VALUE_RESTART_LIST);
                stored = new byte[(int) raf.length()];
                raf.readFully(stored, 0, stored.length);
                raf.close();
                try {
                    raf.close();
                } catch (IOException e) {
                    str = TAG;
                    str2 = "Error closing file " + e;
                    Slog.e(str, str2);
                    return stored;
                }
            } catch (IOException e2) {
                Slog.e(TAG, "Cannot read file " + e2);
                if (raf != null) {
                    try {
                        raf.close();
                    } catch (IOException e3) {
                        str = TAG;
                        str2 = "Error closing file " + e3;
                        Slog.e(str, str2);
                        return stored;
                    }
                }
            }
        }
        return stored;
    }

    private String readTypeFile(String name) {
        String stored;
        String str;
        String str2;
        synchronized (this.mFileWriteLock) {
            RandomAccessFile raf = null;
            stored = null;
            try {
                raf = new RandomAccessFile(name, FoldablePackagePolicy.POLICY_VALUE_RESTART_LIST);
                stored = raf.readLine();
                raf.close();
                try {
                    raf.close();
                } catch (IOException e) {
                    str = TAG;
                    str2 = "Error closing file " + e;
                    Slog.e(str, str2);
                    return stored;
                }
            } catch (IOException e2) {
                Slog.e(TAG, "Cannot read file " + e2);
                if (raf != null) {
                    try {
                        raf.close();
                    } catch (IOException e3) {
                        str = TAG;
                        str2 = "Error closing file " + e3;
                        Slog.e(str, str2);
                        return stored;
                    }
                }
            }
        }
        return stored;
    }

    private void writeFile(String name, byte[] hash) {
        String str;
        String str2;
        synchronized (this.mFileWriteLock) {
            RandomAccessFile raf = null;
            try {
                raf = new RandomAccessFile(name, "rw");
                raf.setLength(0L);
                if (hash != null) {
                    raf.write(hash, 0, hash.length);
                }
                raf.close();
                try {
                    raf.close();
                } catch (IOException e) {
                    str = TAG;
                    str2 = "Error closing file " + e;
                    Slog.e(str, str2);
                }
            } catch (IOException e2) {
                Slog.e(TAG, "Error writing to file " + e2);
                if (raf != null) {
                    try {
                        raf.close();
                    } catch (IOException e3) {
                        str = TAG;
                        str2 = "Error closing file " + e3;
                        Slog.e(str, str2);
                    }
                }
            }
        }
    }

    private void writeTypeFile(String name, String passwordType) {
        String str;
        String str2;
        synchronized (this.mFileWriteLock) {
            RandomAccessFile raf = null;
            try {
                raf = new RandomAccessFile(name, "rw");
                raf.setLength(0L);
                if (passwordType != null) {
                    raf.writeBytes(passwordType);
                }
                raf.close();
                try {
                    raf.close();
                } catch (IOException e) {
                    str = TAG;
                    str2 = "Error closing type file " + e;
                    Slog.e(str, str2);
                }
            } catch (IOException e2) {
                Slog.e(TAG, "Error writing type to file " + e2);
                if (raf != null) {
                    try {
                        raf.close();
                    } catch (IOException e3) {
                        str = TAG;
                        str2 = "Error closing type file " + e3;
                        Slog.e(str, str2);
                    }
                }
            }
        }
    }

    private String getFilePathForUser(int userId, String fileName) {
        String dataSystemDirectory = Environment.getDataDirectory().getAbsolutePath() + SYSTEM_DIRECTORY;
        if (userId == 0) {
            return dataSystemDirectory + fileName;
        }
        return new File(Environment.getUserSystemDirectory(userId), fileName).getAbsolutePath();
    }

    public void updateWhiteList() {
        try {
            ContentResolver resolver = this.mContext.getContentResolver();
            this.mWorkHandler.removeMessages(1);
            this.mWorkHandler.sendEmptyMessageDelayed(1, UPDATE_EVERY_DELAY);
            List<MiuiSettings.SettingsCloudData.CloudData> appLockList = MiuiSettings.SettingsCloudData.getCloudDataList(resolver, APPLOCK_WHILTE);
            List<MiuiSettings.SettingsCloudData.CloudData> gameAntimsgList = MiuiSettings.SettingsCloudData.getCloudDataList(resolver, GAMEBOOSTER_ANTIMSG);
            updateWhiteList(appLockList, mSkipList);
            updateWhiteList(gameAntimsgList, mAntimsgInterceptList);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateWhiteList(List<MiuiSettings.SettingsCloudData.CloudData> dataList, ArrayMap<String, ArrayList<Intent>> list) {
        if (dataList == null) {
            return;
        }
        try {
            if (dataList.size() == 0) {
                return;
            }
            ArrayMap<String, ArrayList<Intent>> cloudList = new ArrayMap<>();
            for (MiuiSettings.SettingsCloudData.CloudData data : dataList) {
                String json = data.toString();
                if (!TextUtils.isEmpty(json)) {
                    JSONObject jsonObject = new JSONObject(json);
                    String pkg = jsonObject.optString(SplitScreenReporter.STR_PKG);
                    String cls = jsonObject.optString("cls");
                    String action = jsonObject.optString("act");
                    Intent intent = new Intent();
                    if (!TextUtils.isEmpty(action)) {
                        intent.setAction(action);
                    } else {
                        intent.setComponent(new ComponentName(pkg, cls));
                    }
                    ArrayList<Intent> intents = cloudList.get(pkg);
                    if (intents == null) {
                        intents = new ArrayList<>(1);
                        cloudList.put(pkg, intents);
                    }
                    intents.add(intent);
                }
            }
            if (cloudList.size() > 0) {
                synchronized (this) {
                    list.clear();
                    list.putAll((ArrayMap<? extends String, ? extends ArrayList<Intent>>) cloudList);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private byte[] passwordToHash(String password, int userId) {
        Object hash;
        if (TextUtils.isEmpty(password)) {
            return null;
        }
        try {
            if (Build.VERSION.SDK_INT > 30) {
                Method getSalt = LockPatternUtils.class.getDeclaredMethod("getSalt", Integer.TYPE);
                getSalt.setAccessible(true);
                String salt = (String) getSalt.invoke(this.mLockPatternUtils, Integer.valueOf(userId));
                hash = LockscreenCredential.legacyPasswordToHash(password.getBytes(), salt.getBytes());
            } else if (Build.VERSION.SDK_INT > 28) {
                hash = mPasswordToHash.invoke(this.mLockPatternUtils, password.getBytes(), Integer.valueOf(userId));
            } else {
                hash = mPasswordToHash.invoke(this.mLockPatternUtils, password, Integer.valueOf(userId));
            }
            if (hash != null) {
                if (Build.VERSION.SDK_INT >= 28) {
                    return hash.getBytes(StandardCharsets.UTF_8);
                }
                return hash;
            }
        } catch (Exception e) {
            Log.e(TAG, " passwordToHash invoke error", e);
        }
        return null;
    }
}
