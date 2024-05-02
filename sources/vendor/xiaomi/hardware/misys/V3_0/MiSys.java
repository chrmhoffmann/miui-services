package vendor.xiaomi.hardware.misys.V3_0;

import android.util.Log;
/* loaded from: classes.dex */
public class MiSys {
    private static final String TAG = "MiSys@3.0-Java";

    public static native long getFileSize(String str, String str2);

    public static native int init();

    public static native byte[] readFromFile(String str, String str2, long j);

    public static native boolean setProp(String str, String str2);

    public static native int writeToFile(byte[] bArr, String str, String str2, long j);

    static {
        try {
            Log.i(TAG, "Load libmisys_jni");
            System.loadLibrary("misys_jni");
        } catch (UnsatisfiedLinkError e) {
            Log.w(TAG, "can't loadLibrary libmisys_jni, try libmisys_jni.xiaomi", e);
            try {
                System.loadLibrary("misys_jni.xiaomi");
            } catch (UnsatisfiedLinkError e1) {
                Log.e(TAG, "failed to load misys_jni finally!!", e1);
            }
        }
    }
}
