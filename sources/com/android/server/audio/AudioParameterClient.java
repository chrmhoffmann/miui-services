package com.android.server.audio;

import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import java.util.NoSuchElementException;
/* loaded from: classes.dex */
public class AudioParameterClient implements IBinder.DeathRecipient {
    private static final String TAG = "AudioParameterClient";
    private ClientDeathListener callback;
    private final IBinder clientBinder;
    private String targetParameter;

    /* loaded from: classes.dex */
    public interface ClientDeathListener {
        void onBinderDied(IBinder iBinder, String str);
    }

    public AudioParameterClient(IBinder binder, String targetParameter) {
        this.clientBinder = binder;
        this.targetParameter = targetParameter;
    }

    public void setClientDiedListener(ClientDeathListener callback) {
        this.callback = callback;
    }

    public boolean registerDeathRecipient() {
        try {
            this.clientBinder.linkToDeath(this, 0);
            return true;
        } catch (RemoteException e) {
            Log.w(TAG, "AudioParameterClient could not link to binder death " + this.targetParameter);
            e.printStackTrace();
            return false;
        }
    }

    public void unregisterDeathRecipient() {
        try {
            this.clientBinder.unlinkToDeath(this, 0);
        } catch (NoSuchElementException e) {
            Log.w(TAG, "AudioParameterClient could not not unregistered to binder " + this.targetParameter);
        }
    }

    @Override // android.os.IBinder.DeathRecipient
    public void binderDied() {
        Log.d(TAG, "AudioParameterClient binderDied " + this.targetParameter);
        ClientDeathListener clientDeathListener = this.callback;
        if (clientDeathListener != null) {
            clientDeathListener.onBinderDied(this.clientBinder, this.targetParameter);
        }
    }

    public IBinder getBinder() {
        return this.clientBinder;
    }

    public String getTargetParameter() {
        return this.targetParameter;
    }
}
