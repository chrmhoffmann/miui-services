package com.android.server;

import com.android.server.biometrics.sensors.face.MiuiFaceHidl;
import java.util.HashMap;
/* loaded from: classes.dex */
class AppOpsService {
    static final HashMap<Integer, String> sOpInControl;

    AppOpsService() {
    }

    static {
        HashMap<Integer, String> hashMap = new HashMap<>();
        sOpInControl = hashMap;
        hashMap.put(20, "android.permission.SEND_SMS");
        hashMap.put(15, "android.permission.WRITE_SMS");
        hashMap.put(22, "android.permission.WRITE_SMS");
        hashMap.put(10010, "android.permission.WRITE_SMS");
        hashMap.put(14, "android.permission.READ_SMS");
        hashMap.put(16, "android.permission.RECEIVE_SMS");
        hashMap.put(17, "android.permission.RECEIVE_SMS");
        hashMap.put(21, "android.permission.READ_SMS");
        hashMap.put(10004, "android.permission.INTERNET");
        hashMap.put(10011, "android.permission.WRITE_SMS");
        hashMap.put(10006, "android.permission.WRITE_SMS");
        hashMap.put(18, "android.permission.RECEIVE_MMS");
        hashMap.put(19, "android.permission.RECEIVE_MMS");
        hashMap.put(13, "android.permission.CALL_PHONE");
        hashMap.put(54, "android.permission.PROCESS_OUTGOING_CALLS");
        hashMap.put(5, "android.permission.WRITE_CONTACTS");
        hashMap.put(4, "android.permission.READ_CONTACTS");
        hashMap.put(7, "android.permission.WRITE_CALL_LOG");
        hashMap.put(10013, "android.permission.WRITE_CALL_LOG");
        hashMap.put(6, "android.permission.READ_CALL_LOG");
        hashMap.put(0, "android.permission.ACCESS_COARSE_LOCATION");
        hashMap.put(1, "android.permission.ACCESS_FINE_LOCATION");
        hashMap.put(2, "android.permission.ACCESS_FINE_LOCATION");
        hashMap.put(10, "android.permission.ACCESS_COARSE_LOCATION");
        hashMap.put(12, "android.permission.ACCESS_COARSE_LOCATION");
        hashMap.put(41, "android.permission.ACCESS_FINE_LOCATION");
        hashMap.put(42, "android.permission.ACCESS_FINE_LOCATION");
        hashMap.put(51, "android.permission.READ_PHONE_STATE");
        hashMap.put(8, "android.permission.READ_CALENDAR");
        hashMap.put(9, "android.permission.WRITE_CALENDAR");
        hashMap.put(10015, "ACCESS_XIAOMI_ACCOUNT");
        hashMap.put(62, "android.permission.GET_ACCOUNTS");
        hashMap.put(52, "com.android.voicemail.permission.ADD_VOICEMAIL");
        hashMap.put(53, "android.permission.USE_SIP");
        hashMap.put(26, "android.permission.CAMERA");
        hashMap.put(27, "android.permission.RECORD_AUDIO");
        hashMap.put(59, "android.permission.READ_EXTERNAL_STORAGE");
        hashMap.put(60, "android.permission.WRITE_EXTERNAL_STORAGE");
        hashMap.put(23, "android.permission.WRITE_SETTINGS");
        hashMap.put(10003, "android.permission.CHANGE_NETWORK_STATE");
        hashMap.put(Integer.valueOf((int) MiuiFaceHidl.MSG_GET_IMG_FRAME), "android.permission.CHANGE_WIFI_STATE");
        hashMap.put(10002, "android.permission.BLUETOOTH_ADMIN");
        hashMap.put(10016, "android.permission.NFC");
        hashMap.put(10017, "com.android.launcher.permission.INSTALL_SHORTCUT");
        hashMap.put(24, "android.permission.SYSTEM_ALERT_WINDOW");
        hashMap.put(56, "android.permission.BODY_SENSORS");
        hashMap.put(10008, "AUTO_START");
    }
}
