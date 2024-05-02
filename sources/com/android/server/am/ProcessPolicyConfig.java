package com.android.server.am;

import java.util.ArrayList;
/* loaded from: classes.dex */
public class ProcessPolicyConfig {
    static final ArrayList<String> sDelayBootPersistentAppList;
    static final ArrayList<String> sImportantProcessList;
    static final ArrayList<String> sNeedTraceProcessList;
    static final ArrayList<String> sProcessCleanProtectedList;

    static {
        ArrayList<String> arrayList = new ArrayList<>();
        sNeedTraceProcessList = arrayList;
        ArrayList<String> arrayList2 = new ArrayList<>();
        sDelayBootPersistentAppList = arrayList2;
        ArrayList<String> arrayList3 = new ArrayList<>();
        sImportantProcessList = arrayList3;
        ArrayList<String> arrayList4 = new ArrayList<>();
        sProcessCleanProtectedList = arrayList4;
        arrayList.add("com.android.phone");
        arrayList.add("com.miui.whetstone");
        arrayList.add("com.android.nfc");
        arrayList.add("com.fingerprints.serviceext");
        arrayList2.add("com.securespaces.android.ssm.service");
        arrayList3.add("com.mobiletools.systemhelper:register");
        arrayList4.add("com.miui.screenrecorder");
    }
}
