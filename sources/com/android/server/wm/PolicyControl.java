package com.android.server.wm;

import android.app.ActivityManager;
import android.content.Context;
import android.provider.Settings;
import android.util.ArraySet;
import android.util.Slog;
import android.view.WindowManager;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
/* loaded from: classes.dex */
class PolicyControl {
    private static final boolean DEBUG = false;
    static final String NAME_IMMERSIVE_FULL = "immersive.full";
    private static final String NAME_IMMERSIVE_NAVIGATION = "immersive.navigation";
    private static final String NAME_IMMERSIVE_PRECONFIRMATIONS = "immersive.preconfirms";
    private static final String NAME_IMMERSIVE_STATUS = "immersive.status";
    private static final String TAG = "PolicyControl";
    private static Filter sImmersiveNavigationFilter;
    private static Filter sImmersivePreconfirmationsFilter;
    private static Filter sImmersiveStatusFilter;
    private static String sSettingValue;

    PolicyControl() {
    }

    public static int getSystemUiVisibility(WindowState win, WindowManager.LayoutParams attrs) {
        WindowManager.LayoutParams attrs2 = attrs != null ? attrs : win.getAttrs();
        int vis = attrs2.systemUiVisibility | attrs2.subtreeSystemUiVisibility;
        Filter filter = sImmersiveStatusFilter;
        if (filter != null && filter.matches(attrs2)) {
            int vis2 = vis | 4100;
            if (attrs2.isFullscreen()) {
                vis2 |= 1024;
            }
            vis = vis2 & (-257);
        }
        Filter filter2 = sImmersiveNavigationFilter;
        if (filter2 != null && filter2.matches(attrs2)) {
            int vis3 = vis | 4098;
            if (attrs2.isFullscreen()) {
                vis3 |= 512;
            }
            return vis3 & (-257);
        }
        return vis;
    }

    public static int getWindowFlags(WindowState win, WindowManager.LayoutParams attrs) {
        WindowManager.LayoutParams attrs2 = attrs != null ? attrs : win.getAttrs();
        int flags = attrs2.flags;
        Filter filter = sImmersiveStatusFilter;
        if (filter != null && filter.matches(attrs2)) {
            flags = (flags | 1024) & (-67110913);
        }
        Filter filter2 = sImmersiveNavigationFilter;
        if (filter2 != null && filter2.matches(attrs2)) {
            return flags & (-134217729);
        }
        return flags;
    }

    static int adjustClearableFlags(WindowState win, int clearableFlags) {
        WindowManager.LayoutParams attrs = win != null ? win.getAttrs() : null;
        Filter filter = sImmersiveStatusFilter;
        if (filter != null && filter.matches(attrs)) {
            return clearableFlags & (-5);
        }
        return clearableFlags;
    }

    public static boolean disableImmersiveConfirmation(String pkg) {
        Filter filter = sImmersivePreconfirmationsFilter;
        return (filter != null && filter.matches(pkg)) || ActivityManager.isRunningInTestHarness();
    }

    public static boolean reloadFromSetting(Context context) {
        String value = null;
        try {
            value = Settings.Global.getStringForUser(context.getContentResolver(), "policy_control", -2);
            String str = sSettingValue;
            if (str != value && (str == null || !str.equals(value))) {
                setFilters(value);
                sSettingValue = value;
                return true;
            }
            return false;
        } catch (Throwable t) {
            Slog.w(TAG, "Error loading policy control, value=" + value, t);
            return false;
        }
    }

    public static void dump(String prefix, PrintWriter pw) {
        dump("sImmersiveStatusFilter", sImmersiveStatusFilter, prefix, pw);
        dump("sImmersiveNavigationFilter", sImmersiveNavigationFilter, prefix, pw);
        dump("sImmersivePreconfirmationsFilter", sImmersivePreconfirmationsFilter, prefix, pw);
    }

    private static void dump(String name, Filter filter, String prefix, PrintWriter pw) {
        pw.print(prefix);
        pw.print("PolicyControl.");
        pw.print(name);
        pw.print('=');
        if (filter == null) {
            pw.println("null");
            return;
        }
        filter.dump(pw);
        pw.println();
    }

    static void setFilters(String value) {
        sImmersiveStatusFilter = null;
        sImmersiveNavigationFilter = null;
        sImmersivePreconfirmationsFilter = null;
        if (value != null) {
            String[] nvps = value.split(":");
            for (String nvp : nvps) {
                int i = nvp.indexOf(61);
                if (i != -1) {
                    String n = nvp.substring(0, i);
                    String v = nvp.substring(i + 1);
                    if (n.equals(NAME_IMMERSIVE_FULL)) {
                        Filter f = Filter.parse(v);
                        sImmersiveNavigationFilter = f;
                        sImmersiveStatusFilter = f;
                        if (sImmersivePreconfirmationsFilter == null) {
                            sImmersivePreconfirmationsFilter = f;
                        }
                    } else if (n.equals(NAME_IMMERSIVE_STATUS)) {
                        sImmersiveStatusFilter = Filter.parse(v);
                    } else if (n.equals(NAME_IMMERSIVE_NAVIGATION)) {
                        Filter f2 = Filter.parse(v);
                        sImmersiveNavigationFilter = f2;
                        if (sImmersivePreconfirmationsFilter == null) {
                            sImmersivePreconfirmationsFilter = f2;
                        }
                    } else if (n.equals(NAME_IMMERSIVE_PRECONFIRMATIONS)) {
                        sImmersivePreconfirmationsFilter = Filter.parse(v);
                    }
                }
            }
        }
    }

    /* loaded from: classes.dex */
    public static class Filter {
        private static final String ALL = "*";
        private static final String APPS = "apps";
        private final ArraySet<String> mBlacklist;
        private final ArraySet<String> mWhitelist;

        private Filter(ArraySet<String> whitelist, ArraySet<String> blacklist) {
            this.mWhitelist = whitelist;
            this.mBlacklist = blacklist;
        }

        boolean matches(WindowManager.LayoutParams attrs) {
            if (attrs == null) {
                return false;
            }
            boolean isApp = attrs.type >= 1 && attrs.type <= 99;
            if ((isApp && this.mBlacklist.contains(APPS)) || onBlacklist(attrs.packageName)) {
                return false;
            }
            if (isApp && this.mWhitelist.contains(APPS)) {
                return true;
            }
            return onWhitelist(attrs.packageName);
        }

        boolean matches(String packageName) {
            return !onBlacklist(packageName) && onWhitelist(packageName);
        }

        private boolean onBlacklist(String packageName) {
            return this.mBlacklist.contains(packageName) || this.mBlacklist.contains(ALL);
        }

        private boolean onWhitelist(String packageName) {
            return this.mWhitelist.contains(ALL) || this.mWhitelist.contains(packageName);
        }

        void dump(PrintWriter pw) {
            pw.print("Filter[");
            dump("whitelist", this.mWhitelist, pw);
            pw.print(',');
            dump("blacklist", this.mBlacklist, pw);
            pw.print(']');
        }

        private void dump(String name, ArraySet<String> set, PrintWriter pw) {
            pw.print(name);
            pw.print("=(");
            int n = set.size();
            for (int i = 0; i < n; i++) {
                if (i > 0) {
                    pw.print(',');
                }
                pw.print(set.valueAt(i));
            }
            pw.print(')');
        }

        public String toString() {
            StringWriter sw = new StringWriter();
            dump(new PrintWriter((Writer) sw, true));
            return sw.toString();
        }

        static Filter parse(String value) {
            if (value == null) {
                return null;
            }
            ArraySet<String> whitelist = new ArraySet<>();
            ArraySet<String> blacklist = new ArraySet<>();
            for (String token : value.split(",")) {
                String token2 = token.trim();
                if (token2.startsWith("-") && token2.length() > 1) {
                    blacklist.add(token2.substring(1));
                } else {
                    whitelist.add(token2);
                }
            }
            return new Filter(whitelist, blacklist);
        }
    }
}
