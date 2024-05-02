package com.miui.server.enterprise;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.os.ServiceManager;
import android.util.Slog;
import com.miui.enterprise.IEnterpriseManager;
import com.miui.enterprise.signature.EnterpriseCer;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
/* loaded from: classes.dex */
public class EnterpriseManagerService extends IEnterpriseManager.Stub {
    private static final String TAG = "Enterprise";
    private APNManagerService mAPNManagerService;
    private ApplicationManagerService mApplicationManagerService;
    private EnterpriseCer mCert;
    private Context mContext;
    private DeviceManagerService mDeviceManagerService;
    private PhoneManagerService mPhoneManagerService;
    private RestrictionsManagerService mRestrictionsManagerService;
    private Map<String, IBinder> mServices = new HashMap();
    private int mCurrentUserId = 0;
    private BroadcastReceiver mLifecycleReceiver = new BroadcastReceiver() { // from class: com.miui.server.enterprise.EnterpriseManagerService.1
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() == null) {
                return;
            }
            String action = intent.getAction();
            char c = 65535;
            switch (action.hashCode()) {
                case -2061058799:
                    if (action.equals("android.intent.action.USER_REMOVED")) {
                        c = 3;
                        break;
                    }
                    break;
                case -755112654:
                    if (action.equals("android.intent.action.USER_STARTED")) {
                        c = 1;
                        break;
                    }
                    break;
                case 66199991:
                    if (action.equals("com.miui.enterprise.ACTION_CERT_UPDATE")) {
                        c = 4;
                        break;
                    }
                    break;
                case 798292259:
                    if (action.equals("android.intent.action.BOOT_COMPLETED")) {
                        c = 0;
                        break;
                    }
                    break;
                case 959232034:
                    if (action.equals("android.intent.action.USER_SWITCHED")) {
                        c = 2;
                        break;
                    }
                    break;
            }
            switch (c) {
                case 0:
                    EnterpriseManagerService.this.bootComplete();
                    return;
                case 1:
                    int userId = intent.getIntExtra("android.intent.extra.user_handle", 0);
                    EnterpriseManagerService.this.onUserStarted(userId);
                    return;
                case 2:
                    int userId2 = intent.getIntExtra("android.intent.extra.user_handle", 0);
                    EnterpriseManagerService.this.onUserSwitched(userId2);
                    return;
                case 3:
                    int userId3 = intent.getIntExtra("android.intent.extra.user_handle", 0);
                    EnterpriseManagerService.this.onUserRemoved(userId3);
                    return;
                case 4:
                    EnterpriseManagerService.this.mCert = intent.getParcelableExtra("extra_ent_cert");
                    return;
                default:
                    return;
            }
        }
    };

    public static void init(Context context) {
        Slog.i(TAG, "Init enterprise service");
        ServiceManager.addService("EnterpriseManager", new EnterpriseManagerService(context));
    }

    private EnterpriseManagerService(Context context) {
        this.mContext = context;
        this.mAPNManagerService = new APNManagerService(context);
        this.mApplicationManagerService = new ApplicationManagerService(context);
        this.mDeviceManagerService = new DeviceManagerService(context);
        this.mPhoneManagerService = new PhoneManagerService(context);
        this.mRestrictionsManagerService = new RestrictionsManagerService(context);
        this.mServices.put("apn_manager", this.mAPNManagerService);
        this.mServices.put("application_manager", this.mApplicationManagerService);
        this.mServices.put("device_manager", this.mDeviceManagerService);
        this.mServices.put("phone_manager", this.mPhoneManagerService);
        this.mServices.put("restrictions_manager", this.mRestrictionsManagerService);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.BOOT_COMPLETED");
        intentFilter.addAction("android.intent.action.USER_STARTED");
        intentFilter.addAction("android.intent.action.USER_SWITCHED");
        intentFilter.addAction("android.intent.action.USER_REMOVED");
        intentFilter.addAction("com.miui.enterprise.ACTION_CERT_UPDATE");
        context.registerReceiver(this.mLifecycleReceiver, intentFilter);
    }

    public void bootComplete() {
        this.mApplicationManagerService.bootComplete();
        this.mRestrictionsManagerService.bootComplete();
        new Thread(new Runnable() { // from class: com.miui.server.enterprise.EnterpriseManagerService.2
            @Override // java.lang.Runnable
            public void run() {
                EnterpriseManagerService.this.loadEnterpriseCert();
            }
        }).start();
    }

    public void onUserStarted(int userId) {
    }

    public void onUserSwitched(int userId) {
        this.mCurrentUserId = userId;
    }

    public void onUserRemoved(int userId) {
    }

    /* JADX WARN: Unsupported multi-entry loop pattern (BACK_EDGE: B:9:0x0026 -> B:21:0x0036). Please submit an issue!!! */
    public void loadEnterpriseCert() {
        File certFile = new File("/data/system/entcert");
        if (!certFile.exists()) {
            return;
        }
        InputStream inputStream = null;
        try {
            try {
                try {
                    inputStream = new FileInputStream(certFile);
                    this.mCert = new EnterpriseCer(inputStream);
                    inputStream.close();
                } catch (IOException e) {
                    Slog.e(TAG, "Something wrong", e);
                }
            } catch (IOException e2) {
                Slog.e(TAG, "Something wrong", e2);
                if (inputStream != null) {
                    inputStream.close();
                }
            }
        } catch (Throwable th) {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e3) {
                    Slog.e(TAG, "Something wrong", e3);
                }
            }
            throw th;
        }
    }

    public boolean isSignatureVerified() {
        return this.mCert != null;
    }

    public EnterpriseCer getEnterpriseCert() {
        return this.mCert;
    }

    public IBinder getService(String serviceName) {
        checkEnterprisePermission();
        return this.mServices.get(serviceName);
    }

    private void checkEnterprisePermission() {
    }
}
