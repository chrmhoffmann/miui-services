package com.miui.server.input.knock.config.filter;

import com.miui.server.input.knock.config.KnockConfig;
import java.util.ArrayList;
import java.util.List;
/* loaded from: classes.dex */
public class KnockConfigFilterDeviceName implements KnockConfigFilter {
    private final String mDeviceName;

    public KnockConfigFilterDeviceName(String deviceName) {
        this.mDeviceName = deviceName;
    }

    @Override // com.miui.server.input.knock.config.filter.KnockConfigFilter
    public List<KnockConfig> knockConfigFilter(List<KnockConfig> knockConfigList) {
        return findKnockConfig(knockConfigList);
    }

    private List<KnockConfig> findKnockConfig(List<KnockConfig> knockConfigList) {
        List<KnockConfig> knockConfigs = new ArrayList<>();
        if (knockConfigList != null && this.mDeviceName != null) {
            for (KnockConfig knockConfig : knockConfigList) {
                if (findKnockConfigDeviceName(knockConfig)) {
                    knockConfigs.add(knockConfig);
                }
            }
        }
        return knockConfigs;
    }

    private boolean findKnockConfigDeviceName(KnockConfig knockConfig) {
        if (knockConfig.deviceName != null && knockConfig.deviceName.size() != 0) {
            for (int i = 0; i < knockConfig.deviceName.size(); i++) {
                if (this.mDeviceName.equals(knockConfig.deviceName.get(i))) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }
}
