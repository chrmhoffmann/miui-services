package com.miui.server.rescue;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.hardware.usb.IUsbManager;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Slog;
import com.android.server.SystemService;
import com.miui.server.rescue.ContactsHelper;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/* loaded from: classes.dex */
public class BrokenScreenRescueService extends SystemService {
    private static final String BROLEN_SCREEN_RESCUE_SERVICE_RUNTIME = "brokenscreenrescue_start_time";
    public static final boolean DEBUG = false;
    public static final int MSG_RECEIVE_SMS = 1;
    public static final int MSG_UPDATE_CONTACTS = 2;
    private static final String PIN_ERROR_COUNT = "brokenscreenrescue_pin_error_count";
    private static final String PIN_REGEX = "^\\*#\\*#(([0-9a-zA-Z\\S\\s]{4,16}))#\\*#\\*$";
    public static final String SERVICE = "BrokenScreenRescueService";
    private static final String SERVICE_ENABLE_PROP = "persist.sys.brokenscreenservice.disable";
    public static final String TAG = "BrokenScreenRescueService";
    private ArrayList<ContactsHelper.ContactsBean> mContacts;
    private ContentObserver mContactsContentObserver;
    private boolean mContentEvent;
    private Context mContext;
    private Handler mHandler;
    private HandlerThread mHandlerThread;
    private String mLastMsmUri;
    private LockServicesCompat mLockServicesCompat;
    private long mLockStartTime;
    private long mLockTime;
    private ContentObserver mMsgContentObserver;
    private String mMsgbody;
    private ContentResolver mResolver;
    private IUsbManager mUsbManager;
    private ContactsHelper mContactsHelper = new ContactsHelper();
    private Pattern mPinPattern = Pattern.compile(PIN_REGEX);
    private long mlastMsmEventTime = SystemClock.elapsedRealtime();

    public boolean checkDisableBrokenScreenService() {
        return SystemProperties.getBoolean(SERVICE_ENABLE_PROP, false);
    }

    private void unlockAndSetUsb(String passwd) {
        Exception e;
        try {
            long now = SystemClock.elapsedRealtime();
            long elapsedTime = now - this.mLockStartTime;
            int errorCount = Settings.System.getInt(this.mContext.getContentResolver(), PIN_ERROR_COUNT, 0);
            Settings.System.putLong(this.mContext.getContentResolver(), BROLEN_SCREEN_RESCUE_SERVICE_RUNTIME, now);
            if (elapsedTime < 0) {
                this.mLockStartTime = now;
            }
            if (errorCount > 20) {
                Slog.w("BrokenScreenRescueService", "Broken Screen Rescue Service is locked!\nIf you want restart the service, you need to reboot.");
                return;
            }
            if (checkPinErrorCountAndLock(errorCount, now)) {
                elapsedTime = 0;
            }
            if (elapsedTime < this.mLockTime) {
                Slog.w("BrokenScreenRescueService", "Service is locked, left " + ((this.mLockTime - elapsedTime) / 1000) + " seconds unlock.");
                return;
            }
            this.mLockTime = 0L;
            try {
                int ret = this.mLockServicesCompat.verifyCredentials(passwd, 0, 0);
                if (ret != 0) {
                    int errorCount2 = errorCount + 1;
                    Settings.System.putInt(this.mContext.getContentResolver(), PIN_ERROR_COUNT, errorCount2);
                    if (checkPinErrorCountAndLock(errorCount2, now)) {
                        if (ret > 0 && errorCount2 % 5 != 0) {
                            this.mLockTime = ret;
                            Settings.System.putInt(this.mContext.getContentResolver(), PIN_ERROR_COUNT, errorCount2 - 1);
                        }
                        Slog.w("BrokenScreenRescueService", "Service is locked, left " + ((this.mLockTime - 0) / 1000) + " seconds unlock.");
                        return;
                    }
                    return;
                }
                Settings.System.putInt(this.mContext.getContentResolver(), PIN_ERROR_COUNT, 0);
                this.mLockTime = 0L;
                Settings.Global.putInt(this.mContext.getContentResolver(), "adb_enabled", 1);
                this.mUsbManager.setCurrentFunctions(4L);
                AutoUsbDebuggingManager usbDebuggingManager = new AutoUsbDebuggingManager(this.mHandlerThread.getLooper());
                usbDebuggingManager.setAdbEnabled(true);
            } catch (Exception e2) {
                e = e2;
                e.fillInStackTrace();
                Slog.e("BrokenScreenRescueService", "stackTrace", e);
            }
        } catch (Exception e3) {
            e = e3;
        }
    }

    private boolean checkPinErrorCountAndLock(int errorCount, long now) {
        if (errorCount != 0 && errorCount % 5 == 0 && this.mLockTime == 0) {
            this.mLockStartTime = now;
            long pow = ((long) Math.pow(2.0d, errorCount / 5)) * 60 * 1000;
            this.mLockTime = pow;
            if (pow < 0) {
                this.mLockTime = Long.MAX_VALUE;
                return true;
            }
            return true;
        }
        return false;
    }

    public void checkSendSecurityPasswd() {
        Matcher matcher = this.mPinPattern.matcher(this.mMsgbody);
        if (matcher.find() && matcher.groupCount() == 2) {
            String pin = matcher.group(1);
            unlockAndSetUsb(pin);
        }
    }

    public boolean matchContacts() {
        Uri inboxUri = Uri.parse("content://sms/inbox");
        Cursor c = this.mContext.getContentResolver().query(inboxUri, null, null, null, "date desc");
        if (c != null && c.moveToFirst()) {
            String address = c.getString(c.getColumnIndex("address"));
            String body = c.getString(c.getColumnIndex("body"));
            String address2 = only11Number(address);
            Matcher matcher = this.mPinPattern.matcher(body);
            if (!matcher.find() || matcher.groupCount() != 2) {
                c.close();
                return false;
            }
            if (this.mContentEvent) {
                this.mContacts = this.mContactsHelper.getContactsList(this.mContext);
                this.mContentEvent = false;
            }
            Iterator<ContactsHelper.ContactsBean> it = this.mContacts.iterator();
            while (it.hasNext()) {
                ContactsHelper.ContactsBean contact = it.next();
                Iterator<String> it2 = contact.getNumList().iterator();
                while (it2.hasNext()) {
                    String number = it2.next();
                    if (only11Number(number).equals(address2)) {
                        this.mMsgbody = body;
                        c.close();
                        return true;
                    }
                }
            }
            c.close();
        }
        return false;
    }

    private String only11Number(String number) {
        if (TextUtils.isDigitsOnly(number)) {
            if (number.length() > 11) {
                return number.substring(number.length() - 11);
            }
            return number;
        }
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < number.length(); i++) {
            if (number.charAt(i) >= '0' && number.charAt(i) <= '9') {
                str.append(number.charAt(i));
            }
        }
        int i2 = str.length();
        if (i2 > 11) {
            return str.substring(str.length() - 11);
        }
        return str.toString();
    }

    public BrokenScreenRescueService(Context context) {
        super(context);
        this.mUsbManager = null;
        this.mHandlerThread = null;
        HandlerThread handlerThread = new HandlerThread("BrokenScreenHandler");
        this.mHandlerThread = handlerThread;
        handlerThread.start();
        this.mContext = context;
        this.mResolver = context.getContentResolver();
        this.mUsbManager = IUsbManager.Stub.asInterface(ServiceManager.getService("usb"));
        this.mLockServicesCompat = new LockServicesCompat(this.mContext);
        long now = SystemClock.elapsedRealtime();
        if (Settings.System.getLong(this.mResolver, BROLEN_SCREEN_RESCUE_SERVICE_RUNTIME, 0L) > now) {
            Settings.System.putInt(this.mResolver, PIN_ERROR_COUNT, 0);
        }
        Handler handler = new Handler(this.mHandlerThread.getLooper()) { // from class: com.miui.server.rescue.BrokenScreenRescueService.1
            @Override // android.os.Handler
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:
                        if (BrokenScreenRescueService.this.checkDisableBrokenScreenService()) {
                            Slog.w("BrokenScreenRescueService", "Broken Screen Rescue Service has been disable!");
                            return;
                        }
                        long now2 = SystemClock.elapsedRealtime();
                        if (BrokenScreenRescueService.this.mlastMsmEventTime + 1000 > now2) {
                            return;
                        }
                        BrokenScreenRescueService.this.mlastMsmEventTime = now2;
                        if (BrokenScreenRescueService.this.matchContacts()) {
                            BrokenScreenRescueService.this.checkSendSecurityPasswd();
                            return;
                        }
                        return;
                    case 2:
                        BrokenScreenRescueService.this.mContentEvent = true;
                        return;
                    default:
                        Slog.e("BrokenScreenRescueService", "Handler default msg " + msg.what);
                        return;
                }
            }
        };
        this.mHandler = handler;
        handler.sendEmptyMessage(2);
        this.mMsgContentObserver = new ContentObserver(this.mHandler) { // from class: com.miui.server.rescue.BrokenScreenRescueService.2
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange, Uri uri) {
                if (uri.toString().equals("content://sms/raw") || uri.toString().equals(BrokenScreenRescueService.this.mLastMsmUri)) {
                    return;
                }
                BrokenScreenRescueService.this.mLastMsmUri = uri.toString();
                BrokenScreenRescueService.this.mHandler.sendEmptyMessage(1);
            }
        };
        this.mContactsContentObserver = new ContentObserver(this.mHandler) { // from class: com.miui.server.rescue.BrokenScreenRescueService.3
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                super.onChange(selfChange);
                BrokenScreenRescueService.this.mHandler.sendEmptyMessage(2);
            }
        };
        this.mResolver.registerContentObserver(Uri.parse("content://sms/"), true, this.mMsgContentObserver);
        this.mResolver.registerContentObserver(ContactsContract.Contacts.CONTENT_URI, true, this.mContactsContentObserver);
    }

    public void onStart() {
    }
}
