package com.miui.server.migard.utils;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
/* loaded from: classes.dex */
public final class SecretUtils {
    private SecretUtils() {
    }

    public static byte[] encryptWithAES(byte[] byteToEncrypt, byte[] secret) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(secret, "AES");
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(1, secretKey);
            cipher.getOutputSize(byteToEncrypt.length);
            return cipher.doFinal(byteToEncrypt);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static byte[] decryptWithAES(byte[] byteToDecrypt, byte[] secret) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(secret, "AES");
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(2, secretKey);
            return cipher.doFinal(byteToDecrypt);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static byte[] encryptWithXOR(byte[] byteToEncrypt, byte[] secret) {
        int length = byteToEncrypt.length;
        byte[] data = new byte[length];
        int keyIndex = 0;
        for (int i = 0; i < length; i++) {
            data[i] = (byte) (byteToEncrypt[i] ^ secret[keyIndex]);
            keyIndex++;
            if (keyIndex == secret.length) {
                keyIndex = 0;
            }
        }
        return data;
    }
}
