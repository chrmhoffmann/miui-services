package com.android.server;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.server.am.SplitScreenReporter;
import android.util.Slog;
import android.util.SparseArray;
import android.util.Xml;
import com.android.internal.util.XmlUtils;
import com.android.server.display.TemperatureController;
import com.miui.server.input.util.MiuiCustomizeShortCutUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes.dex */
public class AppOpsPolicy {
    public static final int CONTROL_NOSHOW = 1;
    public static final int CONTROL_SHOW = 0;
    public static final int CONTROL_UNKNOWN = 2;
    static final boolean DEBUG = false;
    static final String TAG = "AppOpsPolicy";
    final Context mContext;
    final File mFile;
    HashMap<String, PolicyPkg> mPolicy = new HashMap<>();

    public static int stringToControl(String show) {
        if ("true".equalsIgnoreCase(show)) {
            return 0;
        }
        if ("false".equalsIgnoreCase(show)) {
            return 1;
        }
        return 2;
    }

    public AppOpsPolicy(File file, Context context) {
        this.mFile = file;
        this.mContext = context;
    }

    /* loaded from: classes.dex */
    public static final class PolicyPkg extends SparseArray<PolicyOp> {
        public int mode;
        public String packageName;
        public int show;
        public String type;

        public PolicyPkg(String packageName, int mode, int show, String type) {
            this.packageName = packageName;
            this.mode = mode;
            this.show = show;
            this.type = type;
        }

        @Override // android.util.SparseArray
        public String toString() {
            return "PolicyPkg [packageName=" + this.packageName + ", mode=" + this.mode + ", show=" + this.show + ", type=" + this.type + "]";
        }
    }

    /* loaded from: classes.dex */
    public static final class PolicyOp {
        public int mode;
        public int op;
        public int show;

        public PolicyOp(int op, int mode, int show) {
            this.op = op;
            this.mode = mode;
            this.show = show;
        }

        public String toString() {
            return "PolicyOp [op=" + this.op + ", mode=" + this.mode + ", show=" + this.show + "]";
        }
    }

    /* JADX WARN: Unsupported multi-entry loop pattern (BACK_EDGE: B:36:0x008e -> B:69:0x0177). Please submit an issue!!! */
    void readPolicy() {
        XmlPullParser parser;
        int type;
        synchronized (this.mFile) {
            try {
                try {
                    FileInputStream stream = new FileInputStream(this.mFile);
                    try {
                        try {
                            try {
                                try {
                                    try {
                                        parser = Xml.newPullParser();
                                        parser.setInput(stream, null);
                                        while (true) {
                                            type = parser.next();
                                            if (type == 2 || type == 1) {
                                                break;
                                            }
                                        }
                                    } catch (XmlPullParserException e) {
                                        Slog.w(TAG, "Failed parsing " + e);
                                        if (0 == 0) {
                                            this.mPolicy.clear();
                                        }
                                        stream.close();
                                    }
                                } catch (IOException e2) {
                                    Slog.w(TAG, "Failed parsing " + e2);
                                    if (0 == 0) {
                                        this.mPolicy.clear();
                                    }
                                    stream.close();
                                }
                            } catch (IndexOutOfBoundsException e3) {
                                Slog.w(TAG, "Failed parsing " + e3);
                                if (0 == 0) {
                                    this.mPolicy.clear();
                                }
                                stream.close();
                            } catch (NullPointerException e4) {
                                Slog.w(TAG, "Failed parsing " + e4);
                                if (0 == 0) {
                                    this.mPolicy.clear();
                                }
                                stream.close();
                            }
                        } catch (IllegalStateException e5) {
                            Slog.w(TAG, "Failed parsing " + e5);
                            if (0 == 0) {
                                this.mPolicy.clear();
                            }
                            stream.close();
                        } catch (NumberFormatException e6) {
                            Slog.w(TAG, "Failed parsing " + e6);
                            if (0 == 0) {
                                this.mPolicy.clear();
                            }
                            stream.close();
                        }
                    } catch (IOException e7) {
                    }
                    if (type != 2) {
                        throw new IllegalStateException("no start tag found");
                    }
                    int outerDepth = parser.getDepth();
                    while (true) {
                        int type2 = parser.next();
                        if (type2 == 1 || (type2 == 3 && parser.getDepth() <= outerDepth)) {
                            break;
                        } else if (type2 != 3 && type2 != 4) {
                            String tagName = parser.getName();
                            if (!tagName.equals("user-app") && !tagName.equals("system-app")) {
                                if (tagName.equals("application")) {
                                    readApplicationPolicy(parser);
                                } else {
                                    Slog.w(TAG, "Unknown element under <appops-policy>: " + parser.getName());
                                    XmlUtils.skipCurrentTag(parser);
                                }
                            }
                            readDefaultPolicy(parser, tagName);
                        }
                    }
                    if (1 == 0) {
                        this.mPolicy.clear();
                    }
                    stream.close();
                } catch (FileNotFoundException e8) {
                    Slog.i(TAG, "App ops policy file (" + this.mFile.getPath() + ") not found; Skipping.");
                }
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    private void readDefaultPolicy(XmlPullParser parser, String packageName) throws NumberFormatException, XmlPullParserException, IOException {
        if (!"user-app".equalsIgnoreCase(packageName) && !"system-app".equalsIgnoreCase(packageName)) {
            return;
        }
        int show = stringToControl(parser.getAttributeValue(null, "show"));
        if (0 == 2 && show == 2) {
            return;
        }
        PolicyPkg pkg = this.mPolicy.get(packageName);
        if (pkg == null) {
            this.mPolicy.put(packageName, new PolicyPkg(packageName, 0, show, packageName));
            return;
        }
        Slog.w(TAG, "Duplicate policy found for package: " + packageName + " of type: " + packageName);
        pkg.mode = 0;
        pkg.show = show;
    }

    private void readApplicationPolicy(XmlPullParser parser) throws NumberFormatException, XmlPullParserException, IOException {
        int outerDepth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if (type != 1) {
                if (type != 3 || parser.getDepth() > outerDepth) {
                    if (type != 3 && type != 4) {
                        String tagName = parser.getName();
                        if (tagName.equals(SplitScreenReporter.STR_PKG)) {
                            readPkgPolicy(parser);
                        } else {
                            Slog.w(TAG, "Unknown element under <application>: " + parser.getName());
                            XmlUtils.skipCurrentTag(parser);
                        }
                    }
                } else {
                    return;
                }
            } else {
                return;
            }
        }
    }

    private void readPkgPolicy(XmlPullParser parser) throws NumberFormatException, XmlPullParserException, IOException {
        String appType;
        String packageName = parser.getAttributeValue(null, TemperatureController.STRATEGY_NAME);
        if (packageName != null && (appType = parser.getAttributeValue(null, MiuiCustomizeShortCutUtils.ATTRIBUTE_TYPE)) != null) {
            int show = stringToControl(parser.getAttributeValue(null, "show"));
            String key = packageName + "." + appType;
            PolicyPkg pkg = this.mPolicy.get(key);
            if (pkg != null) {
                Slog.w(TAG, "Duplicate policy found for package: " + packageName + " of type: " + appType);
                pkg.mode = 0;
                pkg.show = show;
            } else {
                pkg = new PolicyPkg(packageName, 0, show, appType);
                this.mPolicy.put(key, pkg);
            }
            int outerDepth = parser.getDepth();
            while (true) {
                int type = parser.next();
                if (type != 1) {
                    if (type != 3 || parser.getDepth() > outerDepth) {
                        if (type != 3 && type != 4) {
                            String tagName = parser.getName();
                            if (!tagName.equals("op")) {
                                Slog.w(TAG, "Unknown element under <pkg>: " + parser.getName());
                                XmlUtils.skipCurrentTag(parser);
                            } else {
                                readOpPolicy(parser, pkg);
                            }
                        }
                    } else {
                        return;
                    }
                } else {
                    return;
                }
            }
        }
    }

    private void readOpPolicy(XmlPullParser parser, PolicyPkg pkg) throws NumberFormatException, XmlPullParserException, IOException {
        if (pkg == null) {
            return;
        }
        String opName = parser.getAttributeValue(null, TemperatureController.STRATEGY_NAME);
        if (opName == null) {
            Slog.w(TAG, "Op name is null");
        } else if (-1 != -1) {
            int show = stringToControl(parser.getAttributeValue(null, "show"));
            if (0 == 2 && show == 2) {
                return;
            }
            PolicyOp op = pkg.get(-1);
            if (op != null) {
                Slog.w(TAG, "Duplicate policy found for package: " + pkg.packageName + " type: " + pkg.type + " op: " + op.op);
                op.mode = 0;
                op.show = show;
                return;
            }
            pkg.put(-1, new PolicyOp(-1, 0, show));
        } else {
            Slog.w(TAG, "Unknown Op: " + opName);
        }
    }

    void debugPoilcy() {
        for (Map.Entry<String, PolicyPkg> entry : this.mPolicy.entrySet()) {
            String key = entry.getKey();
            PolicyPkg pkg = this.mPolicy.get(key);
            if (pkg != null) {
                for (int i = 0; i < pkg.size(); i++) {
                    pkg.valueAt(i);
                }
            }
        }
    }

    private String getAppType(String packageName) {
        ApplicationInfo appInfo;
        Context context = this.mContext;
        if (context != null) {
            try {
                appInfo = context.getPackageManager().getApplicationInfo(packageName, 0);
            } catch (PackageManager.NameNotFoundException e) {
                appInfo = null;
            }
            if (appInfo == null) {
                return null;
            }
            if ((appInfo.flags & 1) != 0) {
                return "system-app";
            }
            return "user-app";
        }
        Slog.e(TAG, "Context is null");
        return null;
    }

    public boolean isControlAllowed(int code, String packageName) {
        PolicyPkg pkg;
        int show = 2;
        if (this.mPolicy == null) {
            return true;
        }
        String type = getAppType(packageName);
        if (type != null && (pkg = this.mPolicy.get(type)) != null && pkg.show != 2) {
            show = pkg.show;
        }
        String key = packageName;
        if (type != null) {
            key = key + "." + type;
        }
        PolicyPkg pkg2 = this.mPolicy.get(key);
        if (pkg2 != null) {
            if (pkg2.show != 2) {
                show = pkg2.show;
            }
            PolicyOp op = pkg2.get(code);
            if (op != null && op.show != 2) {
                show = op.show;
            }
        }
        if (show != 1) {
            return true;
        }
        return false;
    }

    public int getDefualtMode(int code, String packageName) {
        PolicyPkg pkg;
        int mode = 2;
        if (this.mPolicy == null) {
            return 2;
        }
        String type = getAppType(packageName);
        if (type != null && (pkg = this.mPolicy.get(type)) != null && pkg.mode != 2) {
            mode = pkg.mode;
        }
        String key = packageName;
        if (type != null) {
            key = key + "." + type;
        }
        PolicyPkg pkg2 = this.mPolicy.get(key);
        if (pkg2 != null) {
            if (pkg2.mode != 2) {
                mode = pkg2.mode;
            }
            PolicyOp op = pkg2.get(code);
            if (op != null && op.mode != 2) {
                return op.mode;
            }
            return mode;
        }
        return mode;
    }
}
