package com.android.server.padkeyboard;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.util.Slog;
import com.android.server.padkeyboard.iic.CommunicationUtil;
import com.android.server.wm.MiuiFreeformPinManagerService;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.Calendar;
/* loaded from: classes.dex */
public class MiuiKeyboardUtil {
    public static final String BIN_FILE_NAME = "bin";
    public static final String ROOT_FILE_NAME = "KB2102";
    private static final String TAG = "CommonUtil";
    private static final float[] angle_cos = {1.0f, 0.999848f, 0.999391f, 0.99863f, 0.997564f, 0.996195f, 0.994522f, 0.992546f, 0.990268f, 0.987688f, 0.984808f, 0.981627f, 0.978148f, 0.97437f, 0.970296f, 0.965926f, 0.961262f, 0.956305f, 0.951057f, 0.945519f, 0.939693f, 0.93358f, 0.927184f, 0.920505f, 0.913545f, 0.906308f, 0.898794f, 0.891007f, 0.882948f, 0.87462f, 0.866025f, 0.857167f, 0.848048f, 0.838671f, 0.829038f, 0.819152f, 0.809017f, 0.798635f, 0.788011f, 0.777146f, 0.766044f, 0.75471f, 0.743145f, 0.731354f, 0.71934f, 0.707107f, 0.694658f, 0.681998f, 0.669131f, 0.656059f, 0.642788f, 0.62932f, 0.615661f, 0.601815f, 0.587785f, 0.573576f, 0.559193f, 0.544639f, 0.529919f, 0.515038f, 0.5f, 0.48481f, 0.469472f, 0.453991f, 0.438371f, 0.422618f, 0.406737f, 0.390731f, 0.374607f, 0.358368f, 0.34202f, 0.325568f, 0.309017f, 0.292372f, 0.275637f, 0.258819f, 0.241922f, 0.224951f, 0.207912f, 0.190809f, 0.173648f, 0.156434f, 0.139173f, 0.121869f, 0.104528f, 0.087156f, 0.069756f, 0.052336f, 0.034899f, 0.017452f, -0.0f};
    private static final float[] angle_sin = {MiuiFreeformPinManagerService.EDGE_AREA, 0.017452f, 0.034899f, 0.052336f, 0.069756f, 0.087156f, 0.104528f, 0.121869f, 0.139173f, 0.156434f, 0.173648f, 0.190809f, 0.207912f, 0.224951f, 0.241922f, 0.258819f, 0.275637f, 0.292372f, 0.309017f, 0.325568f, 0.34202f, 0.358368f, 0.374607f, 0.390731f, 0.406737f, 0.422618f, 0.438371f, 0.453991f, 0.469472f, 0.48481f, 0.5f, 0.515038f, 0.529919f, 0.544639f, 0.559193f, 0.573576f, 0.587785f, 0.601815f, 0.615662f, 0.62932f, 0.642788f, 0.656059f, 0.669131f, 0.681998f, 0.694658f, 0.707107f, 0.71934f, 0.731354f, 0.743145f, 0.75471f, 0.766044f, 0.777146f, 0.788011f, 0.798636f, 0.809017f, 0.819152f, 0.829038f, 0.838671f, 0.848048f, 0.857167f, 0.866025f, 0.87462f, 0.882948f, 0.891007f, 0.898794f, 0.906308f, 0.913545f, 0.920505f, 0.927184f, 0.93358f, 0.939693f, 0.945519f, 0.951057f, 0.956305f, 0.961262f, 0.965926f, 0.970296f, 0.97437f, 0.978148f, 0.981627f, 0.984808f, 0.987688f, 0.990268f, 0.992546f, 0.994522f, 0.996195f, 0.997564f, 0.99863f, 0.999391f, 0.999848f, 1.0f};

    private MiuiKeyboardUtil() {
    }

    public static synchronized byte getSum(byte[] data, int start, int length) {
        byte sum;
        synchronized (MiuiKeyboardUtil.class) {
            sum = 0;
            for (int i = start; i < start + length; i++) {
                sum = (byte) (data[i] + sum);
            }
        }
        return sum;
    }

    public static synchronized boolean checkSum(byte[] data, int start, int length, byte sum) {
        boolean z;
        synchronized (MiuiKeyboardUtil.class) {
            byte dsum = getSum(data, start, length);
            z = dsum == sum;
        }
        return z;
    }

    public static float invSqrt(float num) {
        float xHalf = 0.5f * num;
        int temp = Float.floatToIntBits(num);
        float num2 = Float.intBitsToFloat(1597463007 - (temp >> 1));
        return num2 * (1.5f - ((xHalf * num2) * num2));
    }

    public static float calculatePKAngle(float x1, float x2, float z1, float z2) {
        float angle1;
        float angle2;
        float resultAngle;
        float quadrant = x1 * x2;
        float cos1 = z1 / ((float) Math.sqrt((x1 * x1) + (z1 * z1)));
        float cos2 = z2 / ((float) Math.sqrt((x2 * x2) + (z2 * z2)));
        if (Math.abs(cos1 - 1.0d) < 0.01d) {
            angle1 = MiuiFreeformPinManagerService.EDGE_AREA;
        } else if (Math.abs(cos1 + 1.0d) < 0.01d) {
            angle1 = 180.0f;
        } else {
            angle1 = (float) ((Math.acos(cos1) * 180.0d) / 3.141592653589793d);
        }
        if (Math.abs(cos2 - 1.0d) < 0.01d) {
            angle2 = MiuiFreeformPinManagerService.EDGE_AREA;
        } else if (Math.abs(cos2 + 1.0d) < 0.01d) {
            angle2 = 180.0f;
        } else {
            angle2 = (float) ((Math.acos(cos2) * 180.0d) / 3.141592653589793d);
        }
        Slog.i(TAG, "angle1 = " + angle1 + " angle2 = " + angle2);
        float angleOffset = angle1 - angle2;
        float angleSum = angle1 + angle2;
        float angleOffset2 = angle1 - (180.0f - angle2);
        if (quadrant > MiuiFreeformPinManagerService.EDGE_AREA) {
            resultAngle = ((x1 >= MiuiFreeformPinManagerService.EDGE_AREA || angleOffset <= MiuiFreeformPinManagerService.EDGE_AREA) && (x1 <= MiuiFreeformPinManagerService.EDGE_AREA || angleOffset >= MiuiFreeformPinManagerService.EDGE_AREA)) ? 180.0f + Math.abs(angleOffset) : 180.0f - Math.abs(angleOffset);
        } else if (x1 < MiuiFreeformPinManagerService.EDGE_AREA) {
            if (angleOffset2 > MiuiFreeformPinManagerService.EDGE_AREA) {
                resultAngle = 360.0f - (angleSum - 180.0f);
            } else {
                resultAngle = 180.0f - angleSum;
            }
        } else if (angleOffset2 > MiuiFreeformPinManagerService.EDGE_AREA) {
            resultAngle = (-180.0f) + angleSum;
        } else {
            resultAngle = 180.0f + angleSum;
        }
        Slog.i(TAG, "result Angle = " + resultAngle);
        return resultAngle;
    }

    public static int calculatePKAngleV2(float kX, float kY, float kZ, float pX, float pY, float pZ) {
        float deviation2;
        float deviation1;
        float recipNorm = invSqrt((kX * kX) + (kY * kY) + (kZ * kZ));
        float kX2 = kX * recipNorm;
        float kY2 = kY * recipNorm;
        float kZ2 = kZ * recipNorm;
        float recipNorm2 = invSqrt((pX * pX) + (pY * pY) + (pZ * pZ));
        float pX2 = pX * recipNorm2;
        float pY2 = pY * recipNorm2;
        float pZ2 = pZ * recipNorm2;
        float min_deviation = 100.0f;
        int min_deviation_angle = 0;
        for (int i = 0; i <= 360; i++) {
            if (i > 90) {
                if (i > 90 && i <= 180) {
                    float[] fArr = angle_cos;
                    float[] fArr2 = angle_sin;
                    deviation2 = ((-fArr[180 - i]) * kZ2) + (fArr2[180 - i] * kX2) + pZ2;
                    deviation1 = (((-fArr[180 - i]) * kX2) - (fArr2[180 - i] * kZ2)) + pX2;
                } else if (i > 180 && i <= 270) {
                    float[] fArr3 = angle_cos;
                    float[] fArr4 = angle_sin;
                    deviation2 = ((-fArr3[i - 180]) * kZ2) + ((-fArr4[i - 180]) * kX2) + pZ2;
                    deviation1 = (((-fArr3[i - 180]) * kX2) - ((-fArr4[i - 180]) * kZ2)) + pX2;
                } else {
                    float[] fArr5 = angle_cos;
                    float[] fArr6 = angle_sin;
                    deviation2 = (fArr5[360 - i] * kZ2) + ((-fArr6[360 - i]) * kX2) + pZ2;
                    deviation1 = ((fArr5[360 - i] * kX2) - ((-fArr6[360 - i]) * kZ2)) + pX2;
                }
            } else {
                float f = angle_cos[i];
                float f2 = angle_sin[i];
                deviation2 = (f * kZ2) + (f2 * kX2) + pZ2;
                deviation1 = ((f * kX2) - (f2 * kZ2)) + pX2;
            }
            if (Math.abs(deviation1) + Math.abs(deviation2) < min_deviation) {
                float min_deviation2 = Math.abs(deviation1) + Math.abs(deviation2);
                min_deviation_angle = i;
                min_deviation = min_deviation2;
            }
        }
        float accel_angle_error = Math.abs(pY2) > Math.abs(kY2) ? Math.abs(pY2) : Math.abs(kY2);
        if (accel_angle_error > 0.98f) {
            return -1;
        }
        return min_deviation_angle;
    }

    public static String Bytes2Hex(byte[] data, int len) {
        return Bytes2HexString(data, len, ",");
    }

    public static String Bytes2HexString(byte[] data, int len, String s) {
        StringBuilder stringBuilder = new StringBuilder();
        if (len > data.length) {
            len = data.length;
        }
        for (int i = 0; i < len; i++) {
            stringBuilder.append(String.format("%02x", Byte.valueOf(data[i]))).append(s);
        }
        return stringBuilder.toString();
    }

    public static String Bytes2RevertHexString(byte[] data) {
        StringBuilder hexBuilder = new StringBuilder();
        for (int i = 0; i < data.length; i++) {
            hexBuilder.append(String.format("%02x", Byte.valueOf(data[i])));
        }
        String hexStr = hexBuilder.toString();
        StringBuilder result = new StringBuilder();
        for (int i2 = 0; i2 < hexStr.length(); i2 += 2) {
            result.append(hexStr.substring((hexStr.length() - 2) - i2, hexStr.length() - i2));
        }
        return result.toString();
    }

    public static String Bytes2String(byte[] data) {
        try {
            return new String(data, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return "";
        }
    }

    public static String Hex2String(String hex) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < hex.length() - 1; i += 2) {
            String output = hex.substring(i, i + 2);
            int decimal = Integer.parseInt(output, 16);
            stringBuilder.append((char) decimal);
        }
        return stringBuilder.toString();
    }

    public static String String2HexString(String str) {
        char[] chars = str.toCharArray();
        StringBuilder stringBuilder = new StringBuilder();
        for (char c : chars) {
            stringBuilder.append(Integer.toHexString(c));
        }
        return stringBuilder.toString();
    }

    public static byte[] int2Bytes(int data) {
        byte[] b = new byte[4];
        for (int i = 0; i < 4; i++) {
            b[i] = (byte) (data >> (24 - (i * 8)));
        }
        return b;
    }

    public static int[] getYearMonthDayByTimestamp(long timestamp) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        int[] ret = {calendar.get(1), calendar.get(2) + 1, calendar.get(5)};
        return ret;
    }

    public static void operationWait(int ms) {
        try {
            Thread.currentThread();
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
    }

    public static int byte2int(byte data) {
        return data & 255;
    }

    public static boolean isExist(String file) {
        File fileFile = new File(file);
        return fileFile.exists();
    }

    public static void makeRemountDirFile(Context context) {
        String rootPath = getRootPath(context);
        if (rootPath.isEmpty()) {
            return;
        }
        String basepath = rootPath + File.separator + ROOT_FILE_NAME;
        String binFilePath = basepath + File.separator + BIN_FILE_NAME;
        cratePath(basepath);
        cratePath(binFilePath);
    }

    private static String getRootPath(Context context) {
        File dirFile;
        if (Build.VERSION.SDK_INT >= 19) {
            Environment.initForCurrentUser();
            dirFile = Environment.getExternalStorageDirectory();
        } else {
            String exStorageState = Environment.getExternalStorageState();
            if (exStorageState == null || exStorageState.equals("mounted") || exStorageState.equals("mounted_ro")) {
                dirFile = Environment.getExternalStorageDirectory();
            } else {
                dirFile = context.getExternalFilesDir(null);
            }
        }
        if (dirFile == null) {
            dirFile = context.getFilesDir();
        }
        String path = dirFile.getAbsolutePath();
        return path;
    }

    private static boolean cratePath(String path) {
        String[] dirs = path.split("/");
        String fullPath = "";
        for (String str : dirs) {
            try {
                String dir = str.trim();
                if (!dir.equalsIgnoreCase("")) {
                    fullPath = fullPath + "/" + dir;
                    File fileDir = new File(fullPath);
                    if (!fileDir.exists()) {
                        boolean mkdir = fileDir.mkdir();
                        Log.i(TAG, " mkdir:" + mkdir);
                    }
                }
            } catch (Exception e) {
                Log.i(TAG, "cratePath failed！" + e.getMessage());
                return false;
            }
        }
        return true;
    }

    private static boolean deleteFile(String filePath) {
        File file = new File(filePath);
        if (file.isFile() && file.exists()) {
            return file.delete();
        }
        return false;
    }

    private static boolean deleteDirectory(String filePath) {
        if (!filePath.endsWith(File.separator)) {
            filePath = filePath + File.separator;
        }
        File dirFile = new File(filePath);
        if (!dirFile.exists() || !dirFile.isDirectory()) {
            return false;
        }
        boolean flag = true;
        File[] files = dirFile.listFiles();
        for (int i = 0; i < files.length; i++) {
            if (files[i].isFile()) {
                flag = deleteFile(files[i].getAbsolutePath());
                if (!flag) {
                    break;
                }
            } else {
                flag = deleteDirectory(files[i].getAbsolutePath());
                if (!flag) {
                    break;
                }
            }
        }
        if (flag) {
            return dirFile.delete();
        }
        return false;
    }

    public static byte[] commandMiDevAuthInitForIIC() {
        byte[] temp = new byte[68];
        temp[0] = -86;
        temp[1] = CommunicationUtil.KEYBOARD_COLOR_WHITE;
        temp[2] = 50;
        temp[3] = 0;
        temp[4] = CommunicationUtil.SEND_REPORT_ID_LONG_DATA;
        temp[5] = 49;
        temp[6] = CommunicationUtil.PAD_ADDRESS;
        temp[7] = CommunicationUtil.KEYBOARD_ADDRESS;
        temp[8] = CommunicationUtil.AUTH_COMMAND.AUTH_START.getCommand();
        temp[9] = 6;
        temp[10] = 77;
        temp[11] = 73;
        temp[12] = CommunicationUtil.KEYBOARD_COLOR_BLACK;
        temp[13] = 85;
        temp[14] = 84;
        temp[15] = 72;
        temp[16] = getSum(temp, 4, 13);
        Slog.i(TAG, "send init auth = " + Bytes2Hex(temp, temp.length));
        return temp;
    }

    public static byte[] commandMiAuthStep3Type1ForIIC(byte[] keyMeta, byte[] challenge) {
        byte[] temp = new byte[68];
        temp[0] = -86;
        temp[1] = CommunicationUtil.KEYBOARD_COLOR_WHITE;
        temp[2] = 50;
        temp[3] = 0;
        temp[4] = CommunicationUtil.SEND_REPORT_ID_LONG_DATA;
        temp[5] = 49;
        temp[6] = CommunicationUtil.PAD_ADDRESS;
        temp[7] = CommunicationUtil.KEYBOARD_ADDRESS;
        temp[8] = CommunicationUtil.AUTH_COMMAND.AUTH_STEP3.getCommand();
        temp[9] = 20;
        if (keyMeta != null && keyMeta.length == 4) {
            System.arraycopy(keyMeta, 0, temp, 10, 4);
        }
        if (challenge == null || challenge.length != 16) {
            temp[14] = getSum(temp, 4, 11);
        } else {
            System.arraycopy(challenge, 0, temp, 14, 16);
            temp[30] = getSum(temp, 4, 27);
        }
        Slog.i(TAG, "send 3-1 auth = " + Bytes2Hex(temp, temp.length));
        return temp;
    }

    public static byte[] commandMiDevAuthInitForUSB() {
        byte[] bytes = {CommunicationUtil.SEND_REPORT_ID_LONG_DATA, 49, CommunicationUtil.PAD_ADDRESS, CommunicationUtil.KEYBOARD_ADDRESS, 49, 6, 77, 73, CommunicationUtil.KEYBOARD_COLOR_BLACK, 85, 84, 72, getSum(bytes, 0, 12)};
        return bytes;
    }

    public static byte[] commandMiAuthStep3Type1ForUSB(byte[] keyMeta, byte[] challenge) {
        byte[] bytes = new byte[27];
        bytes[0] = CommunicationUtil.SEND_REPORT_ID_LONG_DATA;
        bytes[1] = 49;
        bytes[2] = CommunicationUtil.PAD_ADDRESS;
        bytes[3] = CommunicationUtil.KEYBOARD_ADDRESS;
        bytes[4] = 50;
        bytes[5] = 20;
        if (keyMeta != null && keyMeta.length == 4) {
            System.arraycopy(keyMeta, 0, bytes, 6, 4);
        }
        if (challenge != null && challenge.length == 16) {
            System.arraycopy(challenge, 0, bytes, 10, 16);
        }
        bytes[26] = getSum(bytes, 0, 26);
        return bytes;
    }
}
