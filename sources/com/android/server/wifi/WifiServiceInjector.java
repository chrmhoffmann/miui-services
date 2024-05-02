package com.android.server.wifi;

import android.content.Context;
import android.location.LocationPolicyManager;
import android.os.Binder;
import android.os.Message;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.WorkSource;
import android.text.TextUtils;
import android.util.Log;
import java.io.UnsupportedEncodingException;
import java.util.BitSet;
/* loaded from: classes.dex */
public class WifiServiceInjector {
    private static final String SUPPLICANT_CONFIG_FILE = "/data/misc/wifi/wpa_supplicant.conf";
    private static final String TAG = "WifiServiceInjector";
    private static final String WIFI_CONFIG_HEADER = "network={";

    public static boolean CheckIfBackgroundScanAllowed(Context ctx, WorkSource workSource) {
        int realOwner = workSource != null ? workSource.get(0) : Binder.getCallingUid();
        if (!UserHandle.isApp(realOwner)) {
            return true;
        }
        try {
            ctx.enforceCallingPermission("android.permission.ACCESS_COARSE_LOCATION", null);
            return LocationPolicyManager.isAllowedByLocationPolicy(ctx, realOwner, 2);
        } catch (SecurityException e) {
            return true;
        }
    }

    public static void handleClientMessage(Message msg) {
    }

    /* JADX WARN: Removed duplicated region for block: B:71:0x0151  */
    /* JADX WARN: Removed duplicated region for block: B:72:0x0156 A[ORIG_RETURN, RETURN] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private static java.lang.String readWifiConfigFromSupplicantFile(java.lang.String r15, java.lang.String r16) {
        /*
            Method dump skipped, instructions count: 356
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.WifiServiceInjector.readWifiConfigFromSupplicantFile(java.lang.String, java.lang.String):java.lang.String");
    }

    private static void replyToMessage(Message message, int arg1, Object obj) {
        try {
            Message reply = Message.obtain();
            reply.what = message.what;
            reply.arg1 = arg1;
            reply.obj = obj;
            message.replyTo.send(reply);
        } catch (RemoteException e) {
            Log.d(TAG, "replyToMessage Failed");
        }
    }

    private static String encodeUtf8SSID(String ssid) {
        try {
            String hex = toHex(ssid.getBytes("UTF-8"));
            return hex;
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "encodeUtf8 to hex failed when read wifi data from wpa_supplicant" + e);
            return "";
        }
    }

    private static String encodeGbkSSID(String ssid) {
        try {
            String hex = toHex(ssid.getBytes("GBK"));
            return hex;
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "encodeGbk to hex failed when read wifi data from wpa_supplicant" + e);
            return "";
        }
    }

    private static String toHex(byte[] octets) {
        StringBuilder sb = new StringBuilder(octets.length * 2);
        for (byte o : octets) {
            sb.append(String.format("%02x", Integer.valueOf(o & 255)));
        }
        return sb.toString();
    }

    private static byte[] hexToBytes(String text) {
        if ((text.length() & 1) == 1) {
            throw new NumberFormatException("Odd length hex string: " + text.length());
        }
        byte[] data = new byte[text.length() >> 1];
        int position = 0;
        for (int n = 0; n < text.length(); n += 2) {
            data[position] = (byte) (((fromHex(text.charAt(n), false) & 15) << 4) | (fromHex(text.charAt(n + 1), false) & 15));
            position++;
        }
        return data;
    }

    private static int fromHex(char ch, boolean lenient) throws NumberFormatException {
        if (ch <= '9' && ch >= '0') {
            return ch - '0';
        }
        if (ch >= 'a' && ch <= 'f') {
            return (ch + '\n') - 97;
        }
        if (ch <= 'F' && ch >= 'A') {
            return (ch + '\n') - 65;
        }
        if (lenient) {
            return -1;
        }
        throw new NumberFormatException("Bad hex-character: " + ch);
    }

    private static boolean isGBK(String hex) {
        byte[] bytes = hexToBytes(hex);
        boolean isAllASCII = true;
        int i = 0;
        while (i < bytes.length) {
            int byte1 = bytes[i] & 255;
            if (byte1 >= 129 && byte1 < 255 && i + 1 < bytes.length) {
                int byte2 = bytes[i + 1] & 255;
                if (byte2 < 64 || byte2 >= 255 || byte2 == 127) {
                    return false;
                }
                isAllASCII = false;
                i++;
            } else if (byte1 >= 128) {
                return false;
            }
            i++;
        }
        return !isAllASCII;
    }

    private static boolean isUTF8(String hex) {
        int nBytes;
        byte[] bytes = hexToBytes(hex);
        int nBytes2 = 0;
        boolean isAllASCII = true;
        for (byte b : bytes) {
            int chr = b & 255;
            if ((chr & 128) != 0) {
                isAllASCII = false;
            }
            if (nBytes2 != 0) {
                if ((chr & 192) != 128) {
                    return false;
                }
                nBytes2--;
            } else if (chr < 128) {
                continue;
            } else {
                if (chr >= 252 && chr <= 253) {
                    nBytes = 6;
                } else if (chr >= 248) {
                    nBytes = 5;
                } else if (chr >= 240) {
                    nBytes = 4;
                } else if (chr >= 224) {
                    nBytes = 3;
                } else if (chr < 192) {
                    return false;
                } else {
                    nBytes = 2;
                }
                nBytes2 = nBytes - 1;
            }
        }
        return nBytes2 <= 0 && !isAllASCII;
    }

    private static String parseKeyMgmt(BitSet set, String[] strings) {
        StringBuffer buf = new StringBuffer();
        int nextSetBit = -1;
        BitSet set2 = set.get(0, strings.length);
        while (true) {
            int nextSetBit2 = set2.nextSetBit(nextSetBit + 1);
            nextSetBit = nextSetBit2;
            if (nextSetBit2 == -1) {
                break;
            }
            buf.append(strings[nextSetBit]).append(' ');
        }
        if (set2.cardinality() > 0) {
            buf.setLength(buf.length() - 1);
        }
        return buf.toString().replace('_', '-');
    }

    private static String removeDoubleQuotes(String string) {
        if (TextUtils.isEmpty(string)) {
            return "";
        }
        int length = string.length();
        if (length > 1 && string.charAt(0) == '\"' && string.charAt(length - 1) == '\"') {
            return string.substring(1, length - 1);
        }
        return string;
    }
}
