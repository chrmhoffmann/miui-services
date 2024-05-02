package com.miui.server.greeze;
/* loaded from: classes.dex */
public class AurogonAppInfo {
    public long freezeTime;
    public boolean hasIcon;
    public boolean isFreezed;
    public boolean isFreezedByGame;
    public boolean isFreezedByLaunchMode;
    public int level;
    public String mPackageName;
    public int mUid;
    public int mUserId;
    public long unFreezeTime;

    public AurogonAppInfo(int uid, String packageName) {
        this.mUid = uid;
        this.mPackageName = packageName;
    }

    public boolean equals(Object o) {
        if (o == null || !(o instanceof AurogonAppInfo)) {
            return false;
        }
        AurogonAppInfo app = (AurogonAppInfo) o;
        return this.mUid == app.mUid;
    }

    public String toString() {
        return this.mUid + " " + this.mPackageName + " " + this.level;
    }
}
