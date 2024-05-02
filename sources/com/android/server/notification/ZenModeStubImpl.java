package com.android.server.notification;

import android.app.KeyguardManager;
import android.content.Context;
import android.provider.Settings;
import com.miui.base.MiuiStubRegistry;
import com.miui.server.input.stylus.MiuiStylusPageKeyListener;
import miui.securityspace.XSpaceUserHandle;
/* loaded from: classes.dex */
public class ZenModeStubImpl implements ZenModeStub {

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<ZenModeStubImpl> {

        /* compiled from: ZenModeStubImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final ZenModeStubImpl INSTANCE = new ZenModeStubImpl();
        }

        public ZenModeStubImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public ZenModeStubImpl provideNewInstance() {
            return new ZenModeStubImpl();
        }
    }

    public String hideTelNumbers(String str) {
        StringBuilder sb = new StringBuilder();
        int index = str.indexOf("tel:");
        if (index >= 0) {
            sb.append(str.substring(index, index + 7));
            for (int i = index + 7; i < str.length(); i++) {
                sb.append('*');
            }
            return sb.toString();
        }
        return str;
    }

    public boolean shouldInterceptWhenUnLocked(Context context, boolean isForce) {
        if (context == null) {
            return false;
        }
        if (Settings.System.getInt(context.getContentResolver(), "zen_mode_intercepted_when_unlocked", 1) == 0) {
            if (isForce) {
                return false;
            }
            KeyguardManager mKgm = (KeyguardManager) context.getSystemService(MiuiStylusPageKeyListener.SCENE_KEYGUARD);
            if (mKgm != null && !mKgm.inKeyguardRestrictedInputMode()) {
                return false;
            }
        }
        return true;
    }

    public int getRingerModeAffectedStreams(Context context, int streams, int zenmode) {
        int streams2 = streams | 38;
        if (zenmode == 2) {
            return streams2 | 2072;
        }
        int streams3 = streams2 & (-2073);
        int muteMusic = Settings.System.getIntForUser(context.getContentResolver(), "mute_music_at_silent", 0, -3);
        if (muteMusic == 1) {
            return streams3 | 2056;
        }
        return streams3 & (-2057);
    }

    public boolean isXSpaceUserId(int userId) {
        return XSpaceUserHandle.isXSpaceUserId(userId);
    }
}
