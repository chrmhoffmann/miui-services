package com.android.server.location;

import android.util.Base64;
import java.io.ByteArrayOutputStream;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
/* loaded from: classes.dex */
public class RsaUtil {
    public static final String KEY_ALGORITHM = "RSA/ECB/PKCS1Padding";
    private static final int MAX_ENCRYPT_BLOCK = 117;
    public static final String publicKeyStr = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCsThShFHYm1FvbAD2x37QhCW5u\nwm75AZwxNqlZJRL+dc67b7U6y2aBpt5TACYs7eKvslYO7WNVAc8smgt3NL7GyBeR\n1cBaowUTcXkOQYzahqhd3Y0qbz6bvGeakzSeCYXQh4kknkdt64K/EI4QvyKTKmdz\nCVOG8VFnc7fuH+uWSQIDAQAB\n";

    public static byte[] encryptByPublicKey(byte[] data) {
        byte[] cache;
        byte[] encryptedData = new byte[0];
        try {
            PublicKey publicKey = getPublicKey();
            Cipher cipher = Cipher.getInstance(KEY_ALGORITHM);
            cipher.init(1, publicKey);
            int inputLen = data.length;
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            int offSet = 0;
            int i = 0;
            while (inputLen - offSet > 0) {
                if (inputLen - offSet > MAX_ENCRYPT_BLOCK) {
                    cache = cipher.doFinal(data, offSet, MAX_ENCRYPT_BLOCK);
                } else {
                    cache = cipher.doFinal(data, offSet, inputLen - offSet);
                }
                out.write(cache, 0, cache.length);
                i++;
                offSet = i * MAX_ENCRYPT_BLOCK;
            }
            encryptedData = out.toByteArray();
            out.close();
            return encryptedData;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return encryptedData;
        } catch (NoSuchPaddingException e2) {
            e2.printStackTrace();
            return encryptedData;
        } catch (Exception e3) {
            e3.printStackTrace();
            return encryptedData;
        }
    }

    public static PublicKey getPublicKey() {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            byte[] base64Bytes = Base64.decode(publicKeyStr.getBytes(), 0);
            PublicKey publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(base64Bytes));
            return publicKey;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        } catch (InvalidKeySpecException e2) {
            e2.printStackTrace();
            return null;
        }
    }
}
