package com.miui.server.input.stylus;

import android.content.Context;
import android.hardware.input.InputManager;
import android.util.Slog;
import android.view.InputDevice;
import java.util.HashMap;
import java.util.Map;
import miui.util.ITouchFeature;
/* loaded from: classes.dex */
public class MiuiStylusDeviceListener implements InputManager.InputDeviceListener {
    private static final String TAG = "MiuiStylusDeviceListener";
    private final InputManager mInputManager;
    private final Map<Integer, Integer> mStylusDeviceList = new HashMap();
    private final ITouchFeature mTouchFeature = ITouchFeature.getInstance();

    public MiuiStylusDeviceListener(Context context) {
        InputManager inputManager = (InputManager) context.getSystemService("input");
        this.mInputManager = inputManager;
        inputManager.registerInputDeviceListener(this, null);
    }

    @Override // android.hardware.input.InputManager.InputDeviceListener
    public void onInputDeviceAdded(int i) {
        int isXiaomiStylus;
        InputDevice inputDevice = this.mInputManager.getInputDevice(i);
        if (inputDevice != null && (isXiaomiStylus = inputDevice.isXiaomiStylus()) > 0 && !this.mStylusDeviceList.containsKey(Integer.valueOf(i))) {
            this.mStylusDeviceList.put(Integer.valueOf(i), Integer.valueOf(isXiaomiStylus));
            int value = isXiaomiStylus | 16;
            Slog.w(TAG, "Stylus device add device id : " + i + " version : " + isXiaomiStylus + " value :" + value);
            this.mTouchFeature.setTouchMode(0, 20, value);
        }
    }

    @Override // android.hardware.input.InputManager.InputDeviceListener
    public void onInputDeviceRemoved(int i) {
        Integer isXiaomiStylus;
        if (!this.mStylusDeviceList.containsKey(Integer.valueOf(i)) || (isXiaomiStylus = this.mStylusDeviceList.remove(Integer.valueOf(i))) == null) {
            return;
        }
        Slog.w(TAG, "Last device remove device id : " + i + " version : " + isXiaomiStylus);
        this.mTouchFeature.setTouchMode(0, 20, isXiaomiStylus.intValue());
    }

    @Override // android.hardware.input.InputManager.InputDeviceListener
    public void onInputDeviceChanged(int i) {
    }
}
