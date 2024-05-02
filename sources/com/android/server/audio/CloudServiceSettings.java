package com.android.server.audio;

import android.content.Context;
import android.media.AudioSystem;
import android.os.SystemProperties;
import android.util.Base64;
import android.util.Log;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.json.JSONArray;
import org.json.JSONObject;
import vendor.xiaomi.hardware.misys.V1_0.IMiSys;
import vendor.xiaomi.hardware.misys.V3_0.MiSys;
/* loaded from: classes.dex */
public class CloudServiceSettings {
    private static final String CHARSET_NAME = "UTF-8";
    public static final String FILE_DOWNLOAD = "fileDownload";
    public static final String PARAMETERS_SET = "parameterSet";
    public static final String PROPERTY_SET = "propertySet";
    private static final String SECRET = "sys_audio_secret";
    public static final String SETTINGSPRIVIDER_SET = "settingsProviderSet";
    private static final String TRANSFORMATION_AES = "AES";
    private static final String TRANSFORMATION_AES_E_PAD = "AES/ECB/ZeroBytePadding";
    private static final String VERSION_CODE = "versionCode";
    private static HashMap<String, List<String>> mSettingsKV = new HashMap<>();
    private IMiSys iMisys_V1;
    private vendor.xiaomi.hardware.misys.V2_0.IMiSys iMisys_V2;
    private Context mContext;
    private final String TAG = "CloudServiceSettings";
    private final String mModuleName = "app_misound_feature_support";
    public String mVersionCode = "";
    private String DEBUG_SETTINGS = "";
    public HashMap<String, Setting> mSettings = new HashMap<>();

    public CloudServiceSettings(Context ctx) {
        this.iMisys_V1 = null;
        this.iMisys_V2 = null;
        this.mContext = ctx;
        fetchDataAll();
        try {
            this.iMisys_V1 = IMiSys.getService(true);
            this.iMisys_V2 = vendor.xiaomi.hardware.misys.V2_0.IMiSys.getService(true);
            int misysInit = MiSys.init();
            if (misysInit == 0) {
                Log.e("CloudServiceSettings", "MiSys V3_0 init failed ==> return");
            }
        } catch (Exception e) {
            Log.d("CloudServiceSettings", "fail to get Misys" + e);
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Removed duplicated region for block: B:22:0x00db A[Catch: Exception -> 0x00e1, TRY_LEAVE, TryCatch #0 {Exception -> 0x00e1, blocks: (B:4:0x0018, B:6:0x0020, B:7:0x004e, B:9:0x005d, B:10:0x0061, B:12:0x0067, B:13:0x0079, B:15:0x007f, B:17:0x00b5, B:18:0x00c1, B:21:0x00d7, B:22:0x00db), top: B:26:0x0018 }] */
    /* JADX WARN: Removed duplicated region for block: B:9:0x005d A[Catch: Exception -> 0x00e1, TryCatch #0 {Exception -> 0x00e1, blocks: (B:4:0x0018, B:6:0x0020, B:7:0x004e, B:9:0x005d, B:10:0x0061, B:12:0x0067, B:13:0x0079, B:15:0x007f, B:17:0x00b5, B:18:0x00c1, B:21:0x00d7, B:22:0x00db), top: B:26:0x0018 }] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public void fetchDataAll() {
        /*
            r10 = this;
            java.util.HashMap<java.lang.String, java.util.List<java.lang.String>> r0 = com.android.server.audio.CloudServiceSettings.mSettingsKV
            r0.clear()
            r0 = 0
            android.content.Context r1 = r10.mContext
            android.content.ContentResolver r1 = r1.getContentResolver()
            java.lang.String r2 = "debug_audio_cloud"
            java.lang.String r1 = android.provider.Settings.Global.getString(r1, r2)
            r10.DEBUG_SETTINGS = r1
            java.lang.String r2 = "CloudServiceSettings"
            if (r1 == 0) goto L4e
            java.lang.String r3 = ""
            boolean r1 = r3.equals(r1)     // Catch: java.lang.Exception -> Le1
            if (r1 != 0) goto L4e
            java.lang.StringBuilder r1 = new java.lang.StringBuilder     // Catch: java.lang.Exception -> Le1
            r1.<init>()     // Catch: java.lang.Exception -> Le1
            java.lang.String r3 = "debug settings "
            java.lang.StringBuilder r1 = r1.append(r3)     // Catch: java.lang.Exception -> Le1
            java.lang.String r3 = r10.DEBUG_SETTINGS     // Catch: java.lang.Exception -> Le1
            java.lang.StringBuilder r1 = r1.append(r3)     // Catch: java.lang.Exception -> Le1
            java.lang.String r1 = r1.toString()     // Catch: java.lang.Exception -> Le1
            android.util.Log.d(r2, r1)     // Catch: java.lang.Exception -> Le1
            java.util.HashMap<java.lang.String, com.android.server.audio.CloudServiceSettings$Setting> r1 = r10.mSettings     // Catch: java.lang.Exception -> Le1
            r1.clear()     // Catch: java.lang.Exception -> Le1
            java.util.ArrayList r1 = new java.util.ArrayList     // Catch: java.lang.Exception -> Le1
            r1.<init>()     // Catch: java.lang.Exception -> Le1
            r0 = r1
            android.provider.MiuiSettings$SettingsCloudData$CloudData r1 = new android.provider.MiuiSettings$SettingsCloudData$CloudData     // Catch: java.lang.Exception -> Le1
            java.lang.String r3 = r10.DEBUG_SETTINGS     // Catch: java.lang.Exception -> Le1
            r1.<init>(r3)     // Catch: java.lang.Exception -> Le1
            r0.add(r1)     // Catch: java.lang.Exception -> Le1
            goto L5b
        L4e:
            android.content.Context r1 = r10.mContext     // Catch: java.lang.Exception -> Le1
            android.content.ContentResolver r1 = r1.getContentResolver()     // Catch: java.lang.Exception -> Le1
            java.lang.String r3 = "app_misound_feature_support"
            java.util.List r1 = android.provider.MiuiSettings.SettingsCloudData.getCloudDataList(r1, r3)     // Catch: java.lang.Exception -> Le1
            r0 = r1
        L5b:
            if (r0 == 0) goto Ldb
            java.util.Iterator r1 = r0.iterator()     // Catch: java.lang.Exception -> Le1
        L61:
            boolean r3 = r1.hasNext()     // Catch: java.lang.Exception -> Le1
            if (r3 == 0) goto Ld7
            java.lang.Object r3 = r1.next()     // Catch: java.lang.Exception -> Le1
            android.provider.MiuiSettings$SettingsCloudData$CloudData r3 = (android.provider.MiuiSettings.SettingsCloudData.CloudData) r3     // Catch: java.lang.Exception -> Le1
            org.json.JSONObject r4 = r3.json()     // Catch: java.lang.Exception -> Le1
            java.util.Set r4 = r4.keySet()     // Catch: java.lang.Exception -> Le1
            java.util.Iterator r4 = r4.iterator()     // Catch: java.lang.Exception -> Le1
        L79:
            boolean r5 = r4.hasNext()     // Catch: java.lang.Exception -> Le1
            if (r5 == 0) goto Ld6
            java.lang.Object r5 = r4.next()     // Catch: java.lang.Exception -> Le1
            java.lang.String r5 = (java.lang.String) r5     // Catch: java.lang.Exception -> Le1
            org.json.JSONObject r6 = r3.json()     // Catch: java.lang.Exception -> Le1
            java.lang.String r6 = r6.getString(r5)     // Catch: java.lang.Exception -> Le1
            java.lang.StringBuilder r7 = new java.lang.StringBuilder     // Catch: java.lang.Exception -> Le1
            r7.<init>()     // Catch: java.lang.Exception -> Le1
            java.lang.String r8 = "key :"
            java.lang.StringBuilder r7 = r7.append(r8)     // Catch: java.lang.Exception -> Le1
            java.lang.StringBuilder r7 = r7.append(r5)     // Catch: java.lang.Exception -> Le1
            java.lang.String r8 = ", value:"
            java.lang.StringBuilder r7 = r7.append(r8)     // Catch: java.lang.Exception -> Le1
            java.lang.StringBuilder r7 = r7.append(r6)     // Catch: java.lang.Exception -> Le1
            java.lang.String r7 = r7.toString()     // Catch: java.lang.Exception -> Le1
            android.util.Log.d(r2, r7)     // Catch: java.lang.Exception -> Le1
            java.util.HashMap<java.lang.String, java.util.List<java.lang.String>> r7 = com.android.server.audio.CloudServiceSettings.mSettingsKV     // Catch: java.lang.Exception -> Le1
            boolean r7 = r7.containsKey(r5)     // Catch: java.lang.Exception -> Le1
            if (r7 == 0) goto Lc1
            java.util.HashMap<java.lang.String, java.util.List<java.lang.String>> r7 = com.android.server.audio.CloudServiceSettings.mSettingsKV     // Catch: java.lang.Exception -> Le1
            java.lang.Object r7 = r7.get(r5)     // Catch: java.lang.Exception -> Le1
            java.util.List r7 = (java.util.List) r7     // Catch: java.lang.Exception -> Le1
            r7.add(r6)     // Catch: java.lang.Exception -> Le1
            goto Ld5
        Lc1:
            java.util.ArrayList r7 = new java.util.ArrayList     // Catch: java.lang.Exception -> Le1
            r8 = 1
            java.lang.String[] r8 = new java.lang.String[r8]     // Catch: java.lang.Exception -> Le1
            r9 = 0
            r8[r9] = r6     // Catch: java.lang.Exception -> Le1
            java.util.List r8 = java.util.Arrays.asList(r8)     // Catch: java.lang.Exception -> Le1
            r7.<init>(r8)     // Catch: java.lang.Exception -> Le1
            java.util.HashMap<java.lang.String, java.util.List<java.lang.String>> r8 = com.android.server.audio.CloudServiceSettings.mSettingsKV     // Catch: java.lang.Exception -> Le1
            r8.put(r5, r7)     // Catch: java.lang.Exception -> Le1
        Ld5:
            goto L79
        Ld6:
            goto L61
        Ld7:
            r10.parseSettings()     // Catch: java.lang.Exception -> Le1
            goto Le0
        Ldb:
            java.lang.String r1 = "null data"
            android.util.Log.d(r2, r1)     // Catch: java.lang.Exception -> Le1
        Le0:
            goto Lf8
        Le1:
            r1 = move-exception
            java.lang.StringBuilder r3 = new java.lang.StringBuilder
            r3.<init>()
            java.lang.String r4 = "fail to fetch data "
            java.lang.StringBuilder r3 = r3.append(r4)
            java.lang.StringBuilder r3 = r3.append(r1)
            java.lang.String r3 = r3.toString()
            android.util.Log.d(r2, r3)
        Lf8:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.audio.CloudServiceSettings.fetchDataAll():void");
    }

    private void parseSettings() {
        this.mVersionCode = "";
        if (mSettingsKV.containsKey(VERSION_CODE)) {
            this.mVersionCode = mSettingsKV.get(VERSION_CODE).get(0);
        }
        try {
            for (String str : mSettingsKV.getOrDefault(FILE_DOWNLOAD, Collections.emptyList())) {
                JSONArray array = new JSONArray(str);
                for (int i = 0; i < array.length(); i++) {
                    Setting s = new FileDownloadSetting(array.getJSONObject(i));
                    Log.d("CloudServiceSettings", "parsed FileDownloadSettings " + array.getJSONObject(i));
                    updateOrPut(s);
                }
            }
            for (String str2 : mSettingsKV.getOrDefault(PARAMETERS_SET, Collections.emptyList())) {
                JSONArray array2 = new JSONArray(str2);
                for (int i2 = 0; i2 < array2.length(); i2++) {
                    Setting s2 = new ParametersSetting(array2.getJSONObject(i2));
                    Log.d("CloudServiceSettings", "parsed ParametersSettings " + array2.getJSONObject(i2));
                    updateOrPut(s2);
                }
            }
            for (String str3 : mSettingsKV.getOrDefault(PROPERTY_SET, Collections.emptyList())) {
                JSONArray array3 = new JSONArray(str3);
                for (int i3 = 0; i3 < array3.length(); i3++) {
                    Setting s3 = new PropertySetting(array3.getJSONObject(i3));
                    Log.d("CloudServiceSettings", "parsed PropertySetting " + array3.getJSONObject(i3));
                    updateOrPut(s3);
                }
            }
            for (String str4 : mSettingsKV.getOrDefault(SETTINGSPRIVIDER_SET, Collections.emptyList())) {
                JSONArray array4 = new JSONArray(str4);
                for (int i4 = 0; i4 < array4.length(); i4++) {
                    Setting s4 = new SettingsProviderSettings(array4.getJSONObject(i4));
                    Log.d("CloudServiceSettings", "parsed SettingsProviderSettings " + array4.getJSONObject(i4));
                    updateOrPut(s4);
                }
            }
        } catch (Exception e) {
            Log.e("CloudServiceSettings", "fail to parse setting " + e);
        }
    }

    private void updateOrPut(Setting s) {
        if (s != null) {
            if (this.mSettings.containsKey(s.mSettingName)) {
                Log.d("CloudServiceSettings", "update " + s.mSettingName);
                this.mSettings.get(s.mSettingName).updateTo(s);
                return;
            }
            Log.d("CloudServiceSettings", "record " + s.mSettingName);
            this.mSettings.put(s.mSettingName, s);
        }
    }

    /* loaded from: classes.dex */
    public class Setting {
        private static final String NAME = "name";
        private static final String SECRET = "secret";
        private String mLastSetsVersionCode;
        protected boolean mSecreted;
        public String mSettingName;
        private String mVersionCode;
        protected boolean mSuccess = false;
        protected HashSet<StringBuilder> mSecretedItem = new HashSet<>();

        public Setting(JSONObject json) {
            CloudServiceSettings.this = this$0;
            this.mSecreted = false;
            try {
                this.mSettingName = json.getString("name");
                this.mVersionCode = json.optString(CloudServiceSettings.VERSION_CODE, "");
                this.mSecreted = json.optBoolean(SECRET, false);
            } catch (Exception e) {
                Log.e("CloudServiceSettings", "fail to parse Setting");
            }
        }

        public boolean set() {
            decrypt();
            if (this.mSuccess) {
                Log.d("CloudServiceSettings", this.mSettingName + " already sets");
            }
            if (!this.mSuccess || "".equals(this.mVersionCode) || this.mVersionCode.compareTo(this.mLastSetsVersionCode) > 0) {
                this.mLastSetsVersionCode = this.mVersionCode;
                return true;
            }
            return false;
        }

        public boolean updateTo(Setting s) {
            if (this.mSettingName.equals(s.mSettingName)) {
                this.mVersionCode = s.mVersionCode;
                this.mSecreted = s.mSecreted;
                this.mSecretedItem = s.mSecretedItem;
                return true;
            }
            Log.e("CloudServiceSettings", this.mSettingName + " update to " + s.mSettingName + " fail, item not match!");
            return false;
        }

        protected void decrypt() {
            Iterator it = this.mSecretedItem.iterator();
            while (it.hasNext()) {
                StringBuilder sb = it.next();
                Log.d("CloudServiceSettings", "decryptPassword :" + sb.toString());
                String dec = CloudServiceSettings.decryptPassword(sb.toString());
                sb.delete(0, sb.length());
                sb.append(dec);
                it.remove();
            }
        }
    }

    /* loaded from: classes.dex */
    public class FileDownloadSetting extends Setting {
        private static final String DATA = "data";
        private static final String ENCODING = "encoding";
        private static final String PATH = "absolutePath";
        private StringBuilder mAbsolutePath;
        private StringBuilder mData;
        private String mEncoding;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        FileDownloadSetting(JSONObject json) {
            super(json);
            CloudServiceSettings.this = this$0;
            try {
                this.mAbsolutePath = new StringBuilder(json.getString(PATH));
                this.mData = new StringBuilder(json.getString(DATA));
                if (this.mSecreted) {
                    this.mSecretedItem.add(this.mAbsolutePath);
                    this.mSecretedItem.add(this.mData);
                }
                this.mEncoding = json.optString(ENCODING, "B64");
            } catch (Exception e) {
                Log.e("CloudServiceSettings", "fail to parse FileDownloadSetting");
            }
        }

        /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
        @Override // com.android.server.audio.CloudServiceSettings.Setting
        public boolean set() {
            boolean z;
            boolean z2 = false;
            if (!super.set()) {
                return false;
            }
            if (CloudServiceSettings.this.iMisys_V1 == null || CloudServiceSettings.this.iMisys_V2 == null) {
                Log.d("CloudServiceSettings", "fail to write file, misys is null");
                return false;
            }
            if (!this.mSecreted) {
                Log.d("CloudServiceSettings", "start downloading: " + this.mSettingName + ", mAbsolutePath: " + ((Object) this.mAbsolutePath) + ", mData: " + ((Object) this.mData) + ", mEncoding: " + this.mEncoding);
            }
            if (this.mAbsolutePath.length() == 0) {
                Log.d("CloudServiceSettings", "error path, return !");
                return false;
            }
            StringBuilder sb = this.mAbsolutePath;
            String path = sb.substring(0, sb.lastIndexOf("/"));
            StringBuilder sb2 = this.mAbsolutePath;
            String fileName = sb2.substring(sb2.lastIndexOf("/") + 1);
            Log.d("CloudServiceSettings", "path :" + path + " fileName: " + fileName);
            try {
                if (CloudServiceSettings.this.iMisys_V2.IsExists(path, fileName)) {
                    Log.d("CloudServiceSettings", "file already exits, override!");
                    CloudServiceSettings.this.iMisys_V1.EraseFileOrDirectory(path, fileName);
                }
                ByteArrayOutputStream byteData = new ByteArrayOutputStream();
                String str = this.mEncoding;
                switch (str.hashCode()) {
                    case 65152:
                        if (str.equals("B64")) {
                            z = false;
                            break;
                        }
                        z = true;
                        break;
                    case 69461:
                        if (str.equals("FDS")) {
                            z = true;
                            break;
                        }
                        z = true;
                        break;
                    default:
                        z = true;
                        break;
                }
                switch (z) {
                    case false:
                        byte[] dataTowrite = Base64.decode(this.mData.toString(), 0);
                        if (dataTowrite != null) {
                            byteData.write(dataTowrite, 0, dataTowrite.length);
                            break;
                        }
                        break;
                    case true:
                        URL url = new URL(this.mData.toString());
                        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                        urlConnection.setConnectTimeout(2000000);
                        urlConnection.setReadTimeout(2000000);
                        byte[] dataTowrite2 = new byte[1024];
                        InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                        while (true) {
                            int len = in.read(dataTowrite2);
                            if (len != -1) {
                                byteData.write(dataTowrite2, 0, len);
                            } else {
                                in.close();
                                break;
                            }
                        }
                    default:
                        Log.d("CloudServiceSettings", "fail to download " + this.mSettingName + " encoding type not recognized");
                        break;
                }
                int writeResult = MiSys.writeToFile(byteData.toByteArray(), path, fileName, byteData.toByteArray().length);
                if (writeResult == 1) {
                    z2 = true;
                }
                this.mSuccess = z2;
            } catch (Exception e) {
                Log.d("CloudServiceSettings", "fail to write data: " + e);
            }
            return this.mSuccess;
        }

        @Override // com.android.server.audio.CloudServiceSettings.Setting
        public boolean updateTo(Setting s) {
            if (super.updateTo(s) && (s instanceof FileDownloadSetting)) {
                FileDownloadSetting fs = (FileDownloadSetting) s;
                this.mAbsolutePath = fs.mAbsolutePath;
                this.mData = fs.mData;
                this.mEncoding = fs.mEncoding;
                return true;
            }
            Log.e("CloudServiceSettings", "fail to update FileDownloadSetting: " + this.mSettingName);
            return false;
        }
    }

    /* loaded from: classes.dex */
    public class ParametersSetting extends Setting {
        private static final String KVPAIRS = "kvpairs";
        private StringBuilder mKvpairs;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        ParametersSetting(JSONObject json) {
            super(json);
            CloudServiceSettings.this = this$0;
            try {
                this.mKvpairs = new StringBuilder(json.getString(KVPAIRS));
                if (this.mSecreted) {
                    this.mSecretedItem.add(this.mKvpairs);
                }
            } catch (Exception e) {
                Log.e("CloudServiceSettings", "fail to parse ParametersSetting");
            }
        }

        @Override // com.android.server.audio.CloudServiceSettings.Setting
        public boolean set() {
            boolean z = false;
            if (!super.set()) {
                return false;
            }
            if (AudioSystem.setParameters(this.mKvpairs.toString()) == 0) {
                z = true;
            }
            this.mSuccess = z;
            return this.mSuccess;
        }

        @Override // com.android.server.audio.CloudServiceSettings.Setting
        public boolean updateTo(Setting s) {
            if (super.updateTo(s) && (s instanceof ParametersSetting)) {
                ParametersSetting ps = (ParametersSetting) s;
                this.mKvpairs = ps.mKvpairs;
                return true;
            }
            Log.e("CloudServiceSettings", "fail to update ParametersSetting: " + this.mSettingName);
            return false;
        }
    }

    /* loaded from: classes.dex */
    public class PropertySetting extends Setting {
        private static final String KEY = "key";
        private static final String VALUE = "value";
        private StringBuilder mKey;
        private StringBuilder mValue;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        PropertySetting(JSONObject json) {
            super(json);
            CloudServiceSettings.this = this$0;
            try {
                this.mKey = new StringBuilder(json.getString(KEY));
                this.mValue = new StringBuilder(json.getString("value"));
                if (this.mSecreted) {
                    this.mSecretedItem.add(this.mKey);
                    this.mSecretedItem.add(this.mValue);
                }
            } catch (Exception e) {
                Log.e("CloudServiceSettings", "fail to parse FileDownloadSetting");
            }
        }

        @Override // com.android.server.audio.CloudServiceSettings.Setting
        public boolean set() {
            if (!super.set()) {
                return false;
            }
            try {
                SystemProperties.set(this.mKey.toString(), this.mValue.toString());
                this.mSuccess = true;
            } catch (Exception e) {
                Log.d("CloudServiceSettings", "fail to set property, key: " + this.mKey.toString() + " value: " + this.mValue.toString() + ", Exception: " + e);
            }
            return this.mSuccess;
        }

        @Override // com.android.server.audio.CloudServiceSettings.Setting
        public boolean updateTo(Setting s) {
            if (super.updateTo(s) && (s instanceof PropertySetting)) {
                PropertySetting ps = (PropertySetting) s;
                this.mKey = ps.mKey;
                this.mValue = ps.mValue;
                return true;
            }
            Log.e("CloudServiceSettings", "fail to update PropertySetting: " + this.mSettingName);
            return false;
        }
    }

    /* loaded from: classes.dex */
    public class SettingsProviderSettings extends Setting {
        private static final String KEY = "key";
        private static final String SCOPE = "scope";
        private static final String VALUE = "value";
        private StringBuilder mKey;
        private StringBuilder mScope;
        private StringBuilder mValue;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        SettingsProviderSettings(JSONObject json) {
            super(json);
            CloudServiceSettings.this = this$0;
            try {
                this.mKey = new StringBuilder(json.getString(KEY));
                this.mValue = new StringBuilder(json.getString("value"));
                this.mScope = new StringBuilder(json.getString(SCOPE));
                if (this.mSecreted) {
                    this.mSecretedItem.add(this.mKey);
                    this.mSecretedItem.add(this.mValue);
                    this.mSecretedItem.add(this.mScope);
                }
            } catch (Exception e) {
                Log.e("CloudServiceSettings", "fail to parse FileDownloadSetting");
            }
        }

        /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
        /* JADX WARN: Code restructure failed: missing block: B:14:0x003b, code lost:
            if (r2.equals("global") != false) goto L16;
         */
        @Override // com.android.server.audio.CloudServiceSettings.Setting
        /*
            Code decompiled incorrectly, please refer to instructions dump.
            To view partially-correct add '--show-bad-code' argument
        */
        public boolean set() {
            /*
                r5 = this;
                boolean r0 = super.set()
                r1 = 0
                if (r0 != 0) goto L8
                return r1
            L8:
                com.android.server.audio.CloudServiceSettings r0 = com.android.server.audio.CloudServiceSettings.this
                android.content.Context r0 = com.android.server.audio.CloudServiceSettings.m494$$Nest$fgetmContext(r0)
                android.content.ContentResolver r0 = r0.getContentResolver()
                java.lang.StringBuilder r2 = r5.mScope
                java.lang.String r2 = r2.toString()
                r3 = -1
                int r4 = r2.hashCode()
                switch(r4) {
                    case -1243020381: goto L35;
                    case -906273929: goto L2b;
                    case -887328209: goto L21;
                    default: goto L20;
                }
            L20:
                goto L3e
            L21:
                java.lang.String r1 = "system"
                boolean r1 = r2.equals(r1)
                if (r1 == 0) goto L20
                r1 = 1
                goto L3f
            L2b:
                java.lang.String r1 = "secure"
                boolean r1 = r2.equals(r1)
                if (r1 == 0) goto L20
                r1 = 2
                goto L3f
            L35:
                java.lang.String r4 = "global"
                boolean r2 = r2.equals(r4)
                if (r2 == 0) goto L20
                goto L3f
            L3e:
                r1 = r3
            L3f:
                switch(r1) {
                    case 0: goto L89;
                    case 1: goto L76;
                    case 2: goto L63;
                    default: goto L42;
                }
            L42:
                java.lang.StringBuilder r1 = new java.lang.StringBuilder
                r1.<init>()
                java.lang.String r2 = "fail to set: "
                java.lang.StringBuilder r1 = r1.append(r2)
                java.lang.String r2 = r5.mSettingName
                java.lang.StringBuilder r1 = r1.append(r2)
                java.lang.String r2 = " error scope!"
                java.lang.StringBuilder r1 = r1.append(r2)
                java.lang.String r1 = r1.toString()
                java.lang.String r2 = "CloudServiceSettings"
                android.util.Log.e(r2, r1)
                goto L9c
            L63:
                java.lang.StringBuilder r1 = r5.mKey
                java.lang.String r1 = r1.toString()
                java.lang.StringBuilder r2 = r5.mValue
                java.lang.String r2 = r2.toString()
                boolean r1 = android.provider.Settings.Secure.putString(r0, r1, r2)
                r5.mSuccess = r1
                goto L9c
            L76:
                java.lang.StringBuilder r1 = r5.mKey
                java.lang.String r1 = r1.toString()
                java.lang.StringBuilder r2 = r5.mValue
                java.lang.String r2 = r2.toString()
                boolean r1 = android.provider.Settings.System.putString(r0, r1, r2)
                r5.mSuccess = r1
                goto L9c
            L89:
                java.lang.StringBuilder r1 = r5.mKey
                java.lang.String r1 = r1.toString()
                java.lang.StringBuilder r2 = r5.mValue
                java.lang.String r2 = r2.toString()
                boolean r1 = android.provider.Settings.Global.putString(r0, r1, r2)
                r5.mSuccess = r1
            L9c:
                boolean r1 = r5.mSuccess
                return r1
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.server.audio.CloudServiceSettings.SettingsProviderSettings.set():boolean");
        }

        @Override // com.android.server.audio.CloudServiceSettings.Setting
        public boolean updateTo(Setting s) {
            if (super.updateTo(s) && (s instanceof SettingsProviderSettings)) {
                SettingsProviderSettings ss = (SettingsProviderSettings) s;
                this.mKey = ss.mKey;
                this.mValue = ss.mValue;
                this.mScope = ss.mScope;
                return true;
            }
            Log.e("CloudServiceSettings", "fail to update SettingsProviderSettings: " + this.mSettingName);
            return false;
        }
    }

    public static String decryptPassword(String content) {
        try {
            SecretKey key = new SecretKeySpec(SECRET.getBytes(), TRANSFORMATION_AES);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION_AES_E_PAD);
            cipher.init(2, key);
            byte[] bytes = cipher.doFinal(Base64.decode(content.getBytes(), 0));
            String res = new String(bytes, CHARSET_NAME);
            return res;
        } catch (IOException | InvalidKeyException | NoSuchAlgorithmException | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException e) {
            e.printStackTrace();
            return "";
        }
    }
}
