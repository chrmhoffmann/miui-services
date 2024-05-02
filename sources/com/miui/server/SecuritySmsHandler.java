package com.miui.server;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.telephony.SmsApplication;
import com.android.server.wm.ActivityStarterInjector;
import miui.provider.ExtraTelephony;
import miui.telephony.SubscriptionManager;
import miui.telephony.TelephonyManager;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class SecuritySmsHandler {
    private Context mContext;
    private Handler mHandler;
    private int mInterceptSmsCallerUid = 0;
    private String mInterceptSmsCallerPkgName = null;
    private int mInterceptSmsCount = 0;
    private String mInterceptSmsSenderNum = null;
    private Object mInterceptSmsLock = new Object();
    private BroadcastReceiver mNormalMsgResultReceiver = new BroadcastReceiver() { // from class: com.miui.server.SecuritySmsHandler.1
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.i("SecurityManagerService", "mNormalMsgResultReceiver sms dispatched, action:" + action);
            if ("android.provider.Telephony.SMS_DELIVER".equals(action)) {
                intent.setComponent(null);
                intent.setFlags(ActivityStarterInjector.FLAG_ASSOCIATED_SETTINGS_AV);
                intent.setAction("android.provider.Telephony.SMS_RECEIVED");
                SecuritySmsHandler.this.dispatchIntent(intent, "android.permission.RECEIVE_SMS", 16, null);
                Log.i("SecurityManagerService", "mNormalMsgResultReceiver dispatch SMS_RECEIVED_ACTION");
            } else if ("android.provider.Telephony.WAP_PUSH_DELIVER".equals(action)) {
                intent.setComponent(null);
                intent.setFlags(ActivityStarterInjector.FLAG_ASSOCIATED_SETTINGS_AV);
                intent.setAction("android.provider.Telephony.WAP_PUSH_RECEIVED");
                SecuritySmsHandler.this.dispatchIntent(intent, "android.permission.RECEIVE_SMS", 16, null);
                Log.i("SecurityManagerService", "mNormalMsgResultReceiver dispatch WAP_PUSH_RECEIVED_ACTION");
            }
        }
    };
    private BroadcastReceiver mInterceptedSmsResultReceiver = new BroadcastReceiver() { // from class: com.miui.server.SecuritySmsHandler.2
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            int slotId = SecuritySmsHandler.getSlotIdFromIntent(intent);
            Log.i("SecurityManagerService", "mInterceptedSmsResultReceiver sms dispatched, action:" + action);
            if ("android.provider.Telephony.SMS_RECEIVED".equals(action)) {
                int resultCode = getResultCode();
                if (resultCode == -1) {
                    Log.i("SecurityManagerService", "mInterceptedSmsResultReceiver SMS_RECEIVED_ACTION not aborted");
                    SmsMessage[] msgs = Telephony.Sms.Intents.getMessagesFromIntent(intent);
                    StringBuilder sb = new StringBuilder();
                    for (SmsMessage smsMessage : msgs) {
                        sb.append(smsMessage.getDisplayMessageBody());
                    }
                    String address = msgs[0].getOriginatingAddress();
                    String body = sb.toString();
                    int blockType = SecuritySmsHandler.this.checkByAntiSpam(address, body, slotId);
                    if (blockType != 0) {
                        intent.putExtra("blockType", blockType);
                        if (ExtraTelephony.getRealBlockType(blockType) >= 3) {
                            Log.i("SecurityManagerService", "mInterceptedSmsResultReceiver: This sms is intercepted by AntiSpam");
                            SecuritySmsHandler.this.dispatchSmsToAntiSpam(intent);
                            return;
                        }
                        SecuritySmsHandler.this.dispatchNormalSms(intent);
                    }
                }
            }
        }
    };

    public SecuritySmsHandler(Context context, Handler handler) {
        this.mHandler = handler;
        this.mContext = context;
    }

    /* JADX WARN: Removed duplicated region for block: B:42:0x0141  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public boolean checkSmsBlocked(android.content.Intent r25) {
        /*
            Method dump skipped, instructions count: 374
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.miui.server.SecuritySmsHandler.checkSmsBlocked(android.content.Intent):boolean");
    }

    public boolean startInterceptSmsBySender(String pkgName, String sender, int count) {
        if (Build.VERSION.SDK_INT < 19) {
            return false;
        }
        this.mContext.enforceCallingOrSelfPermission("com.miui.permission.MANAGE_SMS_INTERCEPT", "SecurityManagerService");
        int callerUid = Binder.getCallingUid();
        synchronized (this.mInterceptSmsLock) {
            if (this.mInterceptSmsCallerUid != 0) {
                return false;
            }
            this.mInterceptSmsCallerUid = callerUid;
            this.mInterceptSmsCallerPkgName = pkgName;
            this.mInterceptSmsSenderNum = sender;
            this.mInterceptSmsCount = count;
            return true;
        }
    }

    public boolean stopInterceptSmsBySender() {
        if (Build.VERSION.SDK_INT < 19) {
            return false;
        }
        this.mContext.enforceCallingOrSelfPermission("com.miui.permission.MANAGE_SMS_INTERCEPT", "SecurityManagerService");
        int callerUid = Binder.getCallingUid();
        synchronized (this.mInterceptSmsLock) {
            int i = this.mInterceptSmsCallerUid;
            if (i == 0) {
                return true;
            }
            if (i != callerUid) {
                return false;
            }
            releaseSmsIntercept();
            return true;
        }
    }

    private boolean checkWithInterceptedSender(String sender) {
        boolean result = false;
        synchronized (this.mInterceptSmsLock) {
            Log.i("SecurityManagerService", String.format("checkWithInterceptedSender: callerUid:%d, senderNum:%s, count:%d", Integer.valueOf(this.mInterceptSmsCallerUid), this.mInterceptSmsSenderNum, Integer.valueOf(this.mInterceptSmsCount)));
            if (this.mInterceptSmsCallerUid != 0 && TextUtils.equals(this.mInterceptSmsSenderNum, sender)) {
                int i = this.mInterceptSmsCount;
                if (i > 0) {
                    this.mInterceptSmsCount = i - 1;
                    result = true;
                }
                if (this.mInterceptSmsCount == 0) {
                    releaseSmsIntercept();
                }
            }
        }
        return result;
    }

    public int checkByAntiSpam(String address, String content, int slotId) {
        long token = Binder.clearCallingIdentity();
        int blockType = ExtraTelephony.getSmsBlockType(this.mContext, address, content, slotId);
        Binder.restoreCallingIdentity(token);
        Log.i("SecurityManagerService", "checkByAntiSpam : blockType = " + blockType);
        return blockType;
    }

    private void releaseSmsIntercept() {
        this.mInterceptSmsCallerUid = 0;
        this.mInterceptSmsCallerPkgName = null;
        this.mInterceptSmsSenderNum = null;
        this.mInterceptSmsCount = 0;
    }

    private void dispatchToInterceptApp(Intent intent) {
        Log.i("SecurityManagerService", "dispatchToInterceptApp");
        intent.setFlags(0);
        intent.setComponent(null);
        intent.setPackage(this.mInterceptSmsCallerPkgName);
        intent.setAction("android.provider.Telephony.SMS_RECEIVED");
        dispatchIntent(intent, "android.permission.RECEIVE_SMS", 16, this.mInterceptedSmsResultReceiver);
    }

    public void dispatchSmsToAntiSpam(Intent intent) {
        Log.i("SecurityManagerService", "dispatchSmsToAntiSpam");
        intent.setComponent(null);
        intent.setPackage("com.android.mms");
        intent.setAction("android.provider.Telephony.SMS_DELIVER");
        dispatchIntent(intent, "android.permission.RECEIVE_SMS", 16, null);
    }

    private void dispatchMmsToAntiSpam(Intent intent) {
        Log.i("SecurityManagerService", "dispatchMmsToAntiSpam");
        intent.setComponent(null);
        intent.setPackage("com.android.mms");
        intent.setAction("android.provider.Telephony.WAP_PUSH_DELIVER");
        dispatchIntent(intent, "android.permission.RECEIVE_SMS", 16, null);
    }

    public void dispatchNormalSms(Intent intent) {
        Log.i("SecurityManagerService", "dispatchNormalSms");
        intent.setPackage(null);
        ComponentName componentName = SmsApplication.getDefaultSmsApplication(this.mContext, true);
        if (componentName != null) {
            intent.setComponent(componentName);
            Log.i("SecurityManagerService", String.format("Delivering SMS to: %s", componentName.getPackageName()));
        }
        intent.addFlags(ActivityStarterInjector.FLAG_ASSOCIATED_SETTINGS_AV);
        intent.setAction("android.provider.Telephony.SMS_DELIVER");
        dispatchIntent(intent, "android.permission.RECEIVE_SMS", 16, this.mNormalMsgResultReceiver);
    }

    private void dispatchNormalMms(Intent intent) {
        Log.i("SecurityManagerService", "dispatchNormalMms");
        intent.setPackage(null);
        ComponentName componentName = SmsApplication.getDefaultMmsApplication(this.mContext, true);
        if (componentName != null) {
            intent.setComponent(componentName);
            Log.i("SecurityManagerService", String.format("Delivering MMS to: %s", componentName.getPackageName()));
        }
        intent.addFlags(ActivityStarterInjector.FLAG_ASSOCIATED_SETTINGS_AV);
        intent.setAction("android.provider.Telephony.WAP_PUSH_DELIVER");
        dispatchIntent(intent, "android.permission.RECEIVE_SMS", 16, this.mNormalMsgResultReceiver);
    }

    public void dispatchIntent(Intent intent, String permission, int appOp, BroadcastReceiver resultReceiver) {
        this.mContext.sendOrderedBroadcast(intent, permission, appOp, resultReceiver, this.mHandler, -1, (String) null, (Bundle) null);
    }

    public static int getSlotIdFromIntent(Intent intent) {
        int slotId = 0;
        if (TelephonyManager.getDefault().getPhoneCount() > 1 && (slotId = intent.getIntExtra(SubscriptionManager.SLOT_KEY, 0)) < 0) {
            Log.e("SecurityManagerService", "getSlotIdFromIntent slotId < 0");
        }
        return slotId;
    }
}
