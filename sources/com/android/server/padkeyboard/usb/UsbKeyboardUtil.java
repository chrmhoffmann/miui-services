package com.android.server.padkeyboard.usb;

import com.android.server.padkeyboard.MiuiKeyboardUtil;
import com.android.server.padkeyboard.iic.CommunicationUtil;
/* loaded from: classes.dex */
public class UsbKeyboardUtil {
    public static final int COMMAND_8031_BOOT = 18;
    public static final int COMMAND_8031_RESET = 3;
    public static final int COMMAND_8031_STATUS = 161;
    public static final int COMMAND_8031_VERSION = 1;
    public static final int COMMAND_BACK_LIGHT_ENABLE = 35;
    public static final int COMMAND_CAPS_LOCK = 38;
    public static final int COMMAND_FIRMWARE_RECOVER = 32;
    public static final int COMMAND_KEYBOARD_ENABLE = 34;
    public static final int COMMAND_MIAUTH_INIT = 49;
    public static final int COMMAND_MIAUTH_STEP3_TYPE1 = 50;
    public static final int COMMAND_POWER_STATE = 37;
    public static final int COMMAND_READ_KEYBOARD = 82;
    public static final int COMMAND_TOUCH_PAD_ENABLE = 33;
    public static final int COMMAND_TOUCH_PAD_SENSITIVITY = 36;
    public static final int COMMAND_WRITE_CMD_ACK = 48;
    public static final int DEVICE_PRODUCT_ID = 16380;
    public static final int DEVICE_VENDOR_ID = 12806;
    public static final int KB_TYPE_HIGH = 3;
    public static final int KB_TYPE_HIGH_OLD = 1;
    public static final int KB_TYPE_LOW = 2;
    public static final int OBJECT_FLASH = 57;
    public static final int OBJECT_KEYBOARD = 56;
    public static final int OBJECT_MCU = 24;
    public static final int PACKET_32 = 78;
    public static final int PACKET_64 = 79;
    public static final int SOURCE_HOST = 128;
    public static final int VALUE_BACK_LIGHT_DISABLE = 0;
    public static final int VALUE_BACK_LIGHT_ENABLE = 1;
    public static final int VALUE_CAPS_LOCK = 1;
    public static final int VALUE_FIRMWARE_RECOVER = 54;
    public static final int VALUE_KB_TYPE_HIGH = 1;
    public static final int VALUE_KB_TYPE_LOW = 0;
    public static final int VALUE_KEYBOARD_DISABLE = 0;
    public static final int VALUE_KEYBOARD_ENABLE = 1;
    public static final int VALUE_POWER_STATE = 0;
    public static final int VALUE_TOUCH_PAD_DISABLE = 0;
    public static final int VALUE_TOUCH_PAD_ENABLE = 1;
    public static final int VERSION_KEYBOARD = 49;
    public static final int VERSION_MCU = 48;

    private UsbKeyboardUtil() {
    }

    public static byte[] commandGetConnectState() {
        byte[] bytes = {CommunicationUtil.SEND_REPORT_ID_SHORT_DATA, 49, CommunicationUtil.PAD_ADDRESS, CommunicationUtil.KEYBOARD_ADDRESS, CommunicationUtil.COMMAND_CHECK_MCU_STATUS, 1, 1, MiuiKeyboardUtil.getSum(bytes, 0, 7)};
        return bytes;
    }

    public static byte[] commandGetVersionInfo() {
        byte[] bytes = {CommunicationUtil.SEND_REPORT_ID_SHORT_DATA, 48, CommunicationUtil.PAD_ADDRESS, CommunicationUtil.MCU_ADDRESS, 1, 0, MiuiKeyboardUtil.getSum(bytes, 0, 6)};
        return bytes;
    }

    public static byte[] commandGetBootInfo() {
        byte[] bytes = {CommunicationUtil.SEND_REPORT_ID_SHORT_DATA, 48, CommunicationUtil.PAD_ADDRESS, CommunicationUtil.MCU_ADDRESS, CommunicationUtil.COMMAND_MCU_BOOT, 1, 0, MiuiKeyboardUtil.getSum(bytes, 0, 7)};
        return bytes;
    }

    public static byte[] commandGetResetInfo() {
        byte[] bytes = {CommunicationUtil.SEND_REPORT_ID_SHORT_DATA, 48, CommunicationUtil.PAD_ADDRESS, CommunicationUtil.MCU_ADDRESS, 3, 4, CommunicationUtil.RESPONSE_TYPE, CommunicationUtil.SEND_REPORT_ID_SHORT_DATA, CommunicationUtil.KEYBOARD_ADDRESS, 48, MiuiKeyboardUtil.getSum(bytes, 0, 10)};
        return bytes;
    }

    public static byte[] commandGetKeyboardStatus() {
        byte[] bytes = {CommunicationUtil.SEND_REPORT_ID_SHORT_DATA, 49, CommunicationUtil.PAD_ADDRESS, CommunicationUtil.KEYBOARD_ADDRESS, CommunicationUtil.COMMAND_READ_KB_STATUS, 0, MiuiKeyboardUtil.getSum(bytes, 0, 6)};
        return bytes;
    }

    public static byte[] commandGetKeyboardStatus(int target) {
        byte[] bytes = {CommunicationUtil.SEND_REPORT_ID_SHORT_DATA, 49, CommunicationUtil.PAD_ADDRESS, CommunicationUtil.KEYBOARD_ADDRESS, 48, 1, (byte) target, MiuiKeyboardUtil.getSum(bytes, 0, 7)};
        return bytes;
    }

    public static byte[] commandWriteKeyboardStatus(int target, int value) {
        byte[] bytes = {CommunicationUtil.SEND_REPORT_ID_SHORT_DATA, 49, CommunicationUtil.PAD_ADDRESS, CommunicationUtil.KEYBOARD_ADDRESS, (byte) target, 1, (byte) value, MiuiKeyboardUtil.getSum(bytes, 0, 7)};
        return bytes;
    }

    public static byte[] commandGetKeyboardVersion() {
        byte[] bytes = new byte[64];
        bytes[0] = CommunicationUtil.SEND_REPORT_ID_SHORT_DATA;
        bytes[1] = 48;
        bytes[2] = CommunicationUtil.PAD_ADDRESS;
        bytes[3] = CommunicationUtil.KEYBOARD_ADDRESS;
        bytes[4] = 1;
        bytes[5] = 1;
        bytes[6] = 1;
        bytes[7] = MiuiKeyboardUtil.getSum(bytes, 0, 7);
        return bytes;
    }

    public static byte[] commandMiDevAuthInit() {
        byte[] bytes = {CommunicationUtil.SEND_REPORT_ID_LONG_DATA, 49, CommunicationUtil.PAD_ADDRESS, CommunicationUtil.KEYBOARD_ADDRESS, 49, 6, 77, 73, CommunicationUtil.KEYBOARD_COLOR_BLACK, 85, 84, 72, MiuiKeyboardUtil.getSum(bytes, 0, 12)};
        return bytes;
    }

    public static byte[] commandMiAuthStep3Type1(byte[] keyMeta, byte[] challenge) {
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
        bytes[26] = MiuiKeyboardUtil.getSum(bytes, 0, 26);
        return bytes;
    }
}
