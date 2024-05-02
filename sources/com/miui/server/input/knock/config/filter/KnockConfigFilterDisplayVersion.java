package com.miui.server.input.knock.config.filter;

import com.miui.server.input.knock.config.KnockConfig;
import java.util.ArrayList;
import java.util.List;
/* loaded from: classes.dex */
public class KnockConfigFilterDisplayVersion implements KnockConfigFilter {
    private final int mDisplayVersion;

    public KnockConfigFilterDisplayVersion(String displayVersion) {
        this.mDisplayVersion = Integer.parseInt(displayVersion);
    }

    @Override // com.miui.server.input.knock.config.filter.KnockConfigFilter
    public List<KnockConfig> knockConfigFilter(List<KnockConfig> knockConfigsList) {
        List<KnockConfig> knockConfigs = new ArrayList<>();
        if (knockConfigsList != null) {
            for (KnockConfig knockConfig : knockConfigsList) {
                if (this.mDisplayVersion == knockConfig.displayVersion) {
                    knockConfigs.add(knockConfig);
                }
            }
        }
        return knockConfigs;
    }
}
