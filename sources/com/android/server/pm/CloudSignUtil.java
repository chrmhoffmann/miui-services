package com.android.server.pm;

import android.text.TextUtils;
import com.android.server.pm.CloudControlPreinstallService;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.BiConsumer;
/* loaded from: classes.dex */
public class CloudSignUtil {
    private CloudSignUtil() {
    }

    public static String getNonceStr() {
        return UUID.randomUUID().toString();
    }

    public static String getSign(TreeMap<String, Object> paramsMap, String nonceStr) {
        final StringBuilder unEncryptedStr = new StringBuilder();
        paramsMap.forEach(new BiConsumer() { // from class: com.android.server.pm.CloudSignUtil$$ExternalSyntheticLambda0
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                unEncryptedStr.append((String) obj).append("&").append(obj2);
            }
        });
        unEncryptedStr.append("#").append(nonceStr);
        return md5(unEncryptedStr.toString());
    }

    public static String getSign(String imeiMd5, String device, String miuiVersion, String channel, String region, boolean isCn, String lang, String nonceStr, String sku) {
        Map<String, Object> map = new TreeMap<>();
        map.put("imeiMd5", imeiMd5);
        map.put(CloudControlPreinstallService.ConnectEntity.DEVICE, device);
        map.put(CloudControlPreinstallService.ConnectEntity.MIUI_VERSION, miuiVersion);
        map.put(CloudControlPreinstallService.ConnectEntity.CHANNEL, channel);
        map.put(CloudControlPreinstallService.ConnectEntity.REGION, region);
        map.put(CloudControlPreinstallService.ConnectEntity.IS_CN, Boolean.valueOf(isCn));
        map.put(CloudControlPreinstallService.ConnectEntity.LANG, lang);
        if (!TextUtils.isEmpty(sku)) {
            map.put(CloudControlPreinstallService.ConnectEntity.SKU, sku);
        }
        StringBuilder unEncryptedStr = new StringBuilder();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            unEncryptedStr.append(entry.getKey()).append("&").append(entry.getValue());
        }
        unEncryptedStr.append("#").append(nonceStr);
        return md5(unEncryptedStr.toString());
    }

    public static String md5(String unEncryptedStr) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(unEncryptedStr.getBytes());
            byte[] secretBytes = md.digest();
            String md5code = new BigInteger(1, secretBytes).toString(16);
            StringBuilder md5CodeSb = new StringBuilder();
            for (int i = 0; i < 32 - md5code.length(); i++) {
                md5CodeSb.append("0");
            }
            md5CodeSb.append(md5code);
            return md5CodeSb.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        } catch (Exception e2) {
            e2.printStackTrace();
            return null;
        }
    }
}
