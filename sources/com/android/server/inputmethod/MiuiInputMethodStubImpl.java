package com.android.server.inputmethod;

import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodInfo;
import com.android.server.inputmethod.InputMethodSubtypeSwitchingController;
import com.miui.base.MiuiStubRegistry;
import java.util.List;
/* loaded from: classes.dex */
public class MiuiInputMethodStubImpl implements MiuiInputMethodStub {
    public static final String TAG = "StylusInputMethodSwitcher";
    private BaseInputMethodSwitcher securityInputMethodSwitcher;
    private BaseInputMethodSwitcher stylusInputMethodSwitcher;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<MiuiInputMethodStubImpl> {

        /* compiled from: MiuiInputMethodStubImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final MiuiInputMethodStubImpl INSTANCE = new MiuiInputMethodStubImpl();
        }

        public MiuiInputMethodStubImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public MiuiInputMethodStubImpl provideNewInstance() {
            return new MiuiInputMethodStubImpl();
        }
    }

    public void init(InputMethodManagerService service) {
        this.stylusInputMethodSwitcher = new StylusInputMethodSwitcher();
        this.securityInputMethodSwitcher = new SecurityInputMethodSwitcher();
        this.stylusInputMethodSwitcher.init(service);
        this.securityInputMethodSwitcher.init(service);
    }

    public void onSystemRunningLocked() {
        this.stylusInputMethodSwitcher.onSystemRunningLocked();
        this.securityInputMethodSwitcher.onSystemRunningLocked();
    }

    public void onSwitchUserLocked(int newUserId) {
        this.stylusInputMethodSwitcher.onSwitchUserLocked(newUserId);
        this.securityInputMethodSwitcher.onSwitchUserLocked(newUserId);
    }

    public boolean mayChangeInputMethodLocked(EditorInfo attribute) {
        return this.securityInputMethodSwitcher.mayChangeInputMethodLocked(attribute) || this.stylusInputMethodSwitcher.mayChangeInputMethodLocked(attribute);
    }

    public boolean shouldHideImeSwitcherLocked() {
        return this.stylusInputMethodSwitcher.shouldHideImeSwitcherLocked() || this.securityInputMethodSwitcher.shouldHideImeSwitcherLocked();
    }

    public void removeMethod(List<InputMethodSubtypeSwitchingController.ImeSubtypeListItem> imList) {
        this.stylusInputMethodSwitcher.removeMethod(imList);
        this.securityInputMethodSwitcher.removeMethod(imList);
    }

    public List<InputMethodInfo> filterMethodLocked(List<InputMethodInfo> methodInfos) {
        return this.securityInputMethodSwitcher.filterMethodLocked(this.stylusInputMethodSwitcher.filterMethodLocked(methodInfos));
    }
}
