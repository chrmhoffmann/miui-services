package miui.android.animation.internal;

import miui.android.animation.base.AnimConfig;
import miui.android.animation.base.AnimSpecialConfig;
import miui.android.animation.utils.EaseManager;
/* loaded from: classes.dex */
public class AnimConfigUtils {
    private AnimConfigUtils() {
    }

    public static EaseManager.EaseStyle getEase(AnimConfig config, AnimSpecialConfig sc) {
        EaseManager.EaseStyle ease;
        if (sc != null && sc.ease != null && sc.ease != AnimConfig.sDefEase) {
            ease = sc.ease;
        } else {
            ease = config.ease;
        }
        return ease == null ? AnimConfig.sDefEase : ease;
    }

    public static long getDelay(AnimConfig config, AnimSpecialConfig sc) {
        return Math.max(config.delay, sc != null ? sc.delay : 0L);
    }

    public static int getTintMode(AnimConfig config, AnimSpecialConfig sc) {
        return Math.max(config.tintMode, sc != null ? sc.tintMode : 0);
    }

    public static float getFromSpeed(AnimConfig config, AnimSpecialConfig sc) {
        if (sc != null && !AnimValueUtils.isInvalid(sc.fromSpeed)) {
            return sc.fromSpeed;
        }
        return config.fromSpeed;
    }

    public static float chooseSpeed(float speed1, float speed2) {
        if (AnimValueUtils.isInvalid(speed1)) {
            return speed2;
        }
        if (AnimValueUtils.isInvalid(speed2)) {
            return speed1;
        }
        return Math.max(speed1, speed2);
    }
}
