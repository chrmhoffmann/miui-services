package com.android.server.inputmethod;
/* loaded from: classes.dex */
public class BaseInputMethodSwitcher implements MiuiInputMethodStub {
    public static final int NUMBER_PASSWORD = 18;
    public static final int TEXT_MASK = 4095;
    public static final int TEXT_PASSWORD = 129;
    public static final int TEXT_VISIBLE_PASSWORD = 145;
    public static final int TEXT_WEB_PASSWORD = 225;
    public static final int WEB_EDIT_TEXT = 160;
    protected InputMethodManagerService mService;

    public void init(InputMethodManagerService service) {
        this.mService = service;
    }

    public static boolean isPasswdInputType(int inputType) {
        return (inputType & 160) == 160 ? (inputType & 4095) == 225 : (inputType & 4095) == 129 || (inputType & 4095) == 145 || (inputType & 4095) == 18;
    }
}
