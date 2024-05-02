package com.android.server.wm;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
/* loaded from: classes.dex */
public final class PolicyItem implements Serializable {
    private static final long serialVersionUID = 202006110800L;
    private final List<PackageConfiguration> mList = new ArrayList();
    private int mScpmVersion = 0;
    private String mCurrentVersion = "";
    private int mLocalVersion = 0;

    public PolicyItem(Set<String> policySet) {
        policySet.forEach(new Consumer() { // from class: com.android.server.wm.PolicyItem$$ExternalSyntheticLambda0
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                PolicyItem.this.m1874lambda$new$0$comandroidserverwmPolicyItem((String) obj);
            }
        });
    }

    /* renamed from: lambda$new$0$com-android-server-wm-PolicyItem */
    public /* synthetic */ void m1874lambda$new$0$comandroidserverwmPolicyItem(String str) {
        PackageConfiguration pkgConfig = new PackageConfiguration(str);
        this.mList.add(pkgConfig);
    }

    public List<PackageConfiguration> getPackageConfigurationList() {
        return this.mList;
    }

    public int getScpmVersion() {
        return this.mScpmVersion;
    }

    public void setScpmVersion(int scpmVersion) {
        this.mScpmVersion = scpmVersion;
    }

    String getCurrentVersion() {
        return this.mCurrentVersion;
    }

    public void setCurrentVersion(String currentVersion) {
        this.mCurrentVersion = currentVersion;
    }

    public int getLocalVersion() {
        return this.mLocalVersion;
    }

    public void setLocalVersion(int localVersion) {
        this.mLocalVersion = localVersion;
    }

    boolean isMismatch(Set<String> policySet) {
        if (this.mList.isEmpty() || this.mList.size() != policySet.size()) {
            return true;
        }
        for (PackageConfiguration packageConfiguration : this.mList) {
            if (!policySet.contains(packageConfiguration.mName)) {
                return true;
            }
        }
        return false;
    }
}
