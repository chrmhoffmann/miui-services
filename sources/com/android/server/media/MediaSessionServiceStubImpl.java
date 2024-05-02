package com.android.server.media;

import android.app.AppGlobals;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Log;
import android.view.KeyEvent;
import com.miui.base.MiuiStubRegistry;
import com.miui.whetstone.PowerKeeperPolicy;
/* loaded from: classes.dex */
public class MediaSessionServiceStubImpl implements MediaSessionServiceStub {
    private static final String TAG = "MediaSessionServiceStubImpl";

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<MediaSessionServiceStubImpl> {

        /* compiled from: MediaSessionServiceStubImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final MediaSessionServiceStubImpl INSTANCE = new MediaSessionServiceStubImpl();
        }

        public MediaSessionServiceStubImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public MediaSessionServiceStubImpl provideNewInstance() {
            return new MediaSessionServiceStubImpl();
        }
    }

    public boolean startVoiceAssistant(Context context) {
        Intent intent = new Intent("android.intent.action.ASSIST");
        intent.setPackage("com.miui.voiceassist");
        try {
            ResolveInfo info = AppGlobals.getPackageManager().resolveIntent(intent, intent.resolveTypeIfNeeded(context.getContentResolver()), 65536L, 0);
            if (info != null && info.activityInfo != null && "com.miui.voiceassist".equals(info.activityInfo.packageName) && "com.xiaomi.voiceassistant.CTAAlertActivity".equals(info.activityInfo.name)) {
                intent.putExtra("voice_assist_start_from_key", "headset");
                intent.setClassName("com.miui.voiceassist", "com.xiaomi.voiceassistant.VoiceService");
                context.startForegroundServiceAsUser(intent, UserHandle.CURRENT);
                return true;
            }
            Log.i(TAG, "startVoiceAssistant can't find service");
            return false;
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException", e);
            return false;
        }
    }

    public void notifyPowerKeeperKeyEvent(int uid, KeyEvent event) {
        if (uid == 1002) {
            Bundle b = new Bundle();
            b.putInt("eventcode", event.getKeyCode());
            b.putBoolean("down", event.getAction() == 0);
            PowerKeeperPolicy.getInstance().notifyKeyEvent(b);
        }
    }
}
