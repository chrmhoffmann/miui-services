package com.miui.server.input.edgesuppression;

import android.util.Slog;
/* loaded from: classes.dex */
public class EdgeSuppressionFactory {
    private static final String TAG = "EdgeSuppressionManager";
    public static final String TYPE_INTELLIGENT = "intelligent";
    public static final String TYPE_NORMAL = "normal";

    private EdgeSuppressionFactory() {
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    public static BaseEdgeSuppression getEdgeSuppressionMode(String type) {
        char c;
        switch (type.hashCode()) {
            case -1039745817:
                if (type.equals(TYPE_NORMAL)) {
                    c = 1;
                    break;
                }
                c = 65535;
                break;
            case 1134120567:
                if (type.equals(TYPE_INTELLIGENT)) {
                    c = 0;
                    break;
                }
                c = 65535;
                break;
            default:
                c = 65535;
                break;
        }
        switch (c) {
            case 0:
                return IntelligentHelper.getInstance();
            case 1:
                return NormalHelper.getInstance();
            default:
                Slog.e(TAG, "Wrong type parameter for getEdgeSuppressionMode");
                return NormalHelper.getInstance();
        }
    }
}
