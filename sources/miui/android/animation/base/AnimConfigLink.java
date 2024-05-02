package miui.android.animation.base;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import miui.android.animation.internal.AnimConfigUtils;
import miui.android.animation.utils.EaseManager;
/* loaded from: classes.dex */
public class AnimConfigLink {
    private static final AtomicInteger sIdGenerator = new AtomicInteger();
    private final int id = sIdGenerator.getAndIncrement();
    public final List<AnimConfig> configList = new ArrayList();
    private final AnimConfig mHeadConfig = new AnimConfig();

    public static AnimConfigLink linkConfig(AnimConfig... configs) {
        AnimConfigLink link = new AnimConfigLink();
        for (AnimConfig config : configs) {
            link.add(config, new boolean[0]);
        }
        return link;
    }

    public void add(AnimConfig config, boolean... copy) {
        if (config != null && !this.configList.contains(config)) {
            if (copy.length > 0 && copy[0]) {
                AnimConfig c = new AnimConfig(config);
                this.configList.add(c);
                return;
            }
            this.configList.add(config);
        }
    }

    public void add(AnimConfigLink configLink, boolean... copy) {
        if (configLink == null) {
            return;
        }
        for (AnimConfig config : configLink.configList) {
            add(config, copy);
        }
    }

    public int size() {
        return this.configList.size();
    }

    public void remove(AnimConfig config) {
        if (config != null) {
            this.configList.remove(config);
            if (this.configList.isEmpty()) {
                this.mHeadConfig.clear();
                this.configList.add(this.mHeadConfig);
            }
        }
    }

    public void copy(AnimConfigLink configLink) {
        doClear();
        if (configLink != null) {
            this.configList.addAll(configLink.configList);
        }
    }

    public void addTo(AnimConfig config) {
        for (AnimConfig c : this.configList) {
            config.delay = Math.max(config.delay, c.delay);
            EaseManager.EaseStyle configEase = config.ease;
            EaseManager.EaseStyle curEase = c.ease;
            config.setEase((curEase == null || curEase == AnimConfig.sDefEase) ? configEase : curEase);
            config.listeners.addAll(c.listeners);
            config.flags |= c.flags;
            config.fromSpeed = AnimConfigUtils.chooseSpeed(config.fromSpeed, c.fromSpeed);
            config.minDuration = Math.max(config.minDuration, c.minDuration);
            config.tintMode = Math.max(config.tintMode, c.tintMode);
            config.addSpecialConfigs(c);
        }
    }

    public void clear() {
        doClear();
        this.configList.add(this.mHeadConfig);
    }

    private void doClear() {
        this.configList.clear();
        this.mHeadConfig.clear();
    }

    public AnimConfig getHead() {
        if (this.configList.isEmpty()) {
            this.configList.add(this.mHeadConfig);
        }
        return this.configList.get(0);
    }

    public String toString() {
        return "AnimConfigLink{id = " + this.id + ", configList=" + Arrays.toString(this.configList.toArray()) + '}';
    }
}
