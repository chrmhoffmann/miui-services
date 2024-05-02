package com.android.server.wm;

import android.util.SparseIntArray;
import com.miui.base.MiuiStubRegistry;
import java.io.PrintWriter;
/* loaded from: classes.dex */
public class MirrorActiveUidsImpl extends MirrorActiveUidsStub {
    private final SparseIntArray mNumAppVisibleWindowForUserMap = new SparseIntArray();

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<MirrorActiveUidsImpl> {

        /* compiled from: MirrorActiveUidsImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final MirrorActiveUidsImpl INSTANCE = new MirrorActiveUidsImpl();
        }

        public MirrorActiveUidsImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public MirrorActiveUidsImpl provideNewInstance() {
            return new MirrorActiveUidsImpl();
        }
    }

    public boolean hasVisibleWindowForUser(int uid) {
        return this.mNumAppVisibleWindowForUserMap.get(uid) > 0;
    }

    public void onVisibleWindowForUserChanged(int uid, boolean visible) {
        int index = this.mNumAppVisibleWindowForUserMap.indexOfKey(uid);
        int i = 1;
        if (index >= 0) {
            int valueAt = this.mNumAppVisibleWindowForUserMap.valueAt(index);
            if (!visible) {
                i = -1;
            }
            int num = valueAt + i;
            if (num > 0) {
                this.mNumAppVisibleWindowForUserMap.setValueAt(index, num);
            } else {
                this.mNumAppVisibleWindowForUserMap.removeAt(index);
            }
        } else if (visible) {
            this.mNumAppVisibleWindowForUserMap.append(uid, 1);
        }
    }

    public void dump(PrintWriter pw, String prefix) {
        pw.print(prefix + "mNumAppVisibleWindowForUserMap:[");
        for (int i = this.mNumAppVisibleWindowForUserMap.size() - 1; i >= 0; i--) {
            pw.print(" " + this.mNumAppVisibleWindowForUserMap.keyAt(i) + ":" + this.mNumAppVisibleWindowForUserMap.valueAt(i));
        }
        pw.println("]");
    }
}
