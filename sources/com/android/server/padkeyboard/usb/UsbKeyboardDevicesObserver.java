package com.android.server.padkeyboard.usb;

import android.os.FileObserver;
import android.util.Slog;
import com.android.server.padkeyboard.MiuiPadKeyboardManager;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
/* loaded from: classes.dex */
public class UsbKeyboardDevicesObserver extends FileObserver {
    public static final String FILE_PATH = "/sys/bus/platform/drivers/xiaomi-keyboard/soc:xiaomi_keyboard/xiaomi_keyboard_conn_status";
    private static final List<File> sFileList;
    private static final File sKeyboard;
    private KeyboardActionListener mKeyboardActionListener;

    /* loaded from: classes.dex */
    public interface KeyboardActionListener {
        void onKeyboardAction();
    }

    static {
        ArrayList arrayList = new ArrayList();
        sFileList = arrayList;
        File file = new File(FILE_PATH);
        sKeyboard = file;
        arrayList.add(file);
    }

    public UsbKeyboardDevicesObserver(KeyboardActionListener keyboardActionListener) {
        super(sFileList, 770);
        this.mKeyboardActionListener = keyboardActionListener;
        enableKeyboardDevice();
    }

    @Override // android.os.FileObserver
    public void onEvent(int event, String path) {
        int i = event & 4095;
        KeyboardActionListener keyboardActionListener = this.mKeyboardActionListener;
        if (keyboardActionListener != null) {
            keyboardActionListener.onKeyboardAction();
        }
    }

    public void enableKeyboardDevice() {
        writeKeyboardDevice("enable_keyboard");
    }

    public void resetKeyboardDevice() {
        writeKeyboardDevice("reset");
    }

    public void resetKeyboardHost() {
        writeKeyboardDevice("host_reset");
    }

    public void writeKeyboardDevice(String command) {
        File file = sKeyboard;
        if (file.exists()) {
            try {
                FileOutputStream out = new FileOutputStream(file);
                out.write(command.getBytes());
                out.flush();
                out.close();
                return;
            } catch (IOException e) {
                Slog.i(MiuiPadKeyboardManager.TAG, "write xiaomi_keyboard_conn_status error : " + e.toString());
                e.printStackTrace();
                return;
            }
        }
        Slog.i(MiuiPadKeyboardManager.TAG, "xiaomi_keyboard_conn_status not exists");
    }

    @Override // android.os.FileObserver
    public void startWatching() {
        Slog.i(MiuiPadKeyboardManager.TAG, "startWatching");
        super.startWatching();
    }

    @Override // android.os.FileObserver
    public void stopWatching() {
        Slog.i(MiuiPadKeyboardManager.TAG, "stopWatching");
        super.stopWatching();
    }
}
