package com.android.server.location;

import android.util.Base64;
import android.util.Log;
import java.io.UnsupportedEncodingException;
import java.util.Random;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
/* loaded from: classes.dex */
public class EnCode {
    private static byte[] mkeyRandom = null;
    private static String moutPutString = null;

    private EnCode() {
    }

    public static final String initRandom() throws Exception {
        if (mkeyRandom == null) {
            Random ranDom = new Random();
            byte[] keyRandom = new byte[16];
            for (int i = 0; i < 16; i++) {
                int number = ranDom.nextInt(20);
                keyRandom[i] = (byte) (number + 97);
            }
            mkeyRandom = keyRandom;
            try {
                byte[] encryptedRandom = RsaUtil.encryptByPublicKey(keyRandom);
                String outPutRandom = Base64.encodeToString(encryptedRandom, 0);
                moutPutString = outPutRandom.replace("\n", "").replace("\r", "");
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Log.e("test", "mkeyRandom init done  here\n");
        }
        return moutPutString;
    }

    public static final byte[] encBytes(byte[] srcBytes, byte[] key, byte[] newIv) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
        IvParameterSpec iv = new IvParameterSpec(newIv);
        cipher.init(1, skeySpec, iv);
        byte[] encrypted = cipher.doFinal(srcBytes);
        return encrypted;
    }

    public static final String encText(String sSrc) throws Exception {
        String outPutTxt = null;
        byte[] ivk = {48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48};
        try {
            byte[] srcBytes = sSrc.getBytes("UTF-8");
            byte[] bArr = mkeyRandom;
            if (bArr == null) {
                return null;
            }
            byte[] encryptedTxt = encBytes(srcBytes, bArr, ivk);
            outPutTxt = Base64.encodeToString(encryptedTxt, 0);
            return outPutTxt.replace("\n", "").replace("\r", "");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return outPutTxt;
        } catch (Exception e2) {
            e2.printStackTrace();
            return outPutTxt;
        }
    }
}
