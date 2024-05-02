package miui.android.animation;

import android.widget.TextView;
import miui.android.animation.base.AnimConfig;
/* loaded from: classes.dex */
public interface IVarFontStyle extends IStateContainer {
    IVarFontStyle fromTo(int i, int i2, AnimConfig... animConfigArr);

    IVarFontStyle setTo(int i);

    IVarFontStyle to(int i, AnimConfig... animConfigArr);

    IVarFontStyle useAt(TextView textView, int i, int i2);
}
