package com.miui.server.input;

import android.app.ActivityThread;
import android.app.ContextImpl;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Slog;
import com.android.server.input.MiuiInputThread;
import com.miui.server.input.MiuiInputSettingsConnection;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
/* loaded from: classes.dex */
public class MiuiInputSettingsConnection {
    private static final String INPUT_SETTINGS_COMPONENT = "com.miui.miinput/com.miui.miinput.service.MiuiInputSettingsService";
    public static final int MSG_CLIENT_TRIGGER_QUICK_NOTE = 1;
    public static final int MSG_SERVER_ADD_STYLUS_MASK = 1;
    public static final int MSG_SERVER_REMOVE_STYLUS_MASK = 2;
    private static final String TAG = "MiuiInputSettingsConnection";
    private static final int TIMEOUT_TIME = 15000;
    private static volatile MiuiInputSettingsConnection sInstance;
    private final BroadcastReceiver mBroadcastReceiver;
    private final Context mContext;
    private Messenger mMessenger;
    private ServiceConnection mServiceConnection;
    private final Runnable mTimerRunnable = new Runnable() { // from class: com.miui.server.input.MiuiInputSettingsConnection$$ExternalSyntheticLambda0
        @Override // java.lang.Runnable
        public final void run() {
            MiuiInputSettingsConnection.this.resetConnection();
        }
    };
    private final Handler mHandler = new Handler(MiuiInputThread.getThread().getLooper());
    private final Messenger mCallbackMessenger = new Messenger(new CallbackHandler(MiuiInputThread.getThread().getLooper()));
    private final Map<Message, OnMessageSendFinishCallback> mCachedMessageList = new HashMap();
    private final Map<Integer, Consumer<Message>> mCallbackMap = new HashMap();

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    @interface MessageWhatClient {
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    @interface MessageWhatServer {
    }

    /* loaded from: classes.dex */
    public interface OnMessageSendFinishCallback {
        public static final int STATUS_SEND_FAIL_EXCEPTION = 1;
        public static final int STATUS_SEND_FAIL_NOT_FOUND_SERVICE = 1;
        public static final int STATUS_SEND_SUCCESS = 0;

        @Retention(RetentionPolicy.SOURCE)
        /* loaded from: classes.dex */
        public @interface MessageSendFinishStatus {
        }

        void onMessageSendFinish(int i);
    }

    private MiuiInputSettingsConnection() {
        BroadcastReceiver broadcastReceiver = new BroadcastReceiver() { // from class: com.miui.server.input.MiuiInputSettingsConnection.1
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                synchronized (MiuiInputSettingsConnection.this) {
                    if ("android.intent.action.USER_SWITCHED".equals(intent.getAction())) {
                        Slog.w(MiuiInputSettingsConnection.TAG, "User switched, request reset connection");
                        MiuiInputSettingsConnection.this.resetConnection();
                    }
                }
            }
        };
        this.mBroadcastReceiver = broadcastReceiver;
        ContextImpl systemContext = ActivityThread.currentActivityThread().getSystemContext();
        this.mContext = systemContext;
        IntentFilter filter = new IntentFilter("android.intent.action.USER_SWITCHED");
        systemContext.registerReceiver(broadcastReceiver, filter, 2);
    }

    public static MiuiInputSettingsConnection getInstance() {
        if (sInstance == null) {
            synchronized (MiuiInputSettingsConnection.class) {
                if (sInstance == null) {
                    sInstance = new MiuiInputSettingsConnection();
                }
            }
        }
        return sInstance;
    }

    public void sendMessageToInputSettings(int messageWhatServer) {
        Message message = Message.obtain();
        sendMessageToInputSettings(messageWhatServer, message, null);
    }

    public void sendMessageToInputSettings(int messageWhatServer, int arg1) {
        Message message = Message.obtain();
        message.arg1 = arg1;
        sendMessageToInputSettings(messageWhatServer, message, null);
    }

    public synchronized void sendMessageToInputSettings(int messageWhatServer, Message message, OnMessageSendFinishCallback callback) {
        message.what = messageWhatServer;
        message.replyTo = this.mCallbackMessenger;
        if (this.mServiceConnection == null && this.mMessenger == null) {
            ComponentName serviceComponent = ComponentName.unflattenFromString(INPUT_SETTINGS_COMPONENT);
            Intent serviceIntent = new Intent();
            serviceIntent.setComponent(serviceComponent);
            ServiceConnection connection = new InputSettingsServiceConnection();
            if (this.mContext.bindServiceAsUser(serviceIntent, connection, 1140850689, UserHandle.CURRENT)) {
                Slog.i(TAG, "service bind success");
                this.mServiceConnection = connection;
            } else {
                Slog.e(TAG, "service bind fail");
                this.mContext.unbindService(connection);
            }
        }
        if (this.mServiceConnection == null) {
            executeCallback(callback, 1);
        } else if (this.mMessenger == null) {
            Slog.i(TAG, "cache message " + messageWhatServerToString(message.what));
            this.mCachedMessageList.put(message, callback);
        } else {
            try {
                Slog.i(TAG, "send message " + messageWhatServerToString(message.what) + " to settings");
                this.mMessenger.send(message);
                executeCallback(callback, 0);
                resetTimer();
            } catch (RemoteException e) {
                executeCallback(callback, 1);
                Slog.e(TAG, e.getMessage(), e);
            }
        }
    }

    public void registerCallbackListener(int messageWhatClient, Consumer<Message> consumer) {
        synchronized (this.mCallbackMap) {
            if (this.mCallbackMap.containsKey(Integer.valueOf(messageWhatClient))) {
                Slog.e(TAG, "callback for what = " + messageWhatClientToString(messageWhatClient) + ", already registered");
            } else {
                this.mCallbackMap.put(Integer.valueOf(messageWhatClient), consumer);
            }
        }
    }

    public void unRegisterCallbackListener(int messageWhatClient) {
        synchronized (this.mCallbackMap) {
            if (!this.mCallbackMap.containsKey(Integer.valueOf(messageWhatClient))) {
                Slog.e(TAG, "callback for what = " + messageWhatClientToString(messageWhatClient) + ", not registered");
            } else {
                this.mCallbackMap.remove(Integer.valueOf(messageWhatClient));
            }
        }
    }

    public boolean isBindToInputSettings() {
        return this.mServiceConnection != null;
    }

    public void resetTimer() {
        this.mHandler.removeCallbacks(this.mTimerRunnable);
        this.mHandler.postDelayed(this.mTimerRunnable, 15000L);
    }

    private void removeTimer() {
        this.mHandler.removeCallbacks(this.mTimerRunnable);
    }

    public void resetConnection() {
        Slog.i(TAG, "request reset connection, connect = " + (this.mServiceConnection != null));
        ServiceConnection serviceConnection = this.mServiceConnection;
        if (serviceConnection == null) {
            return;
        }
        this.mContext.unbindService(serviceConnection);
        this.mServiceConnection = null;
        this.mMessenger = null;
        removeTimer();
    }

    /* loaded from: classes.dex */
    public class InputSettingsServiceConnection implements ServiceConnection {
        private InputSettingsServiceConnection() {
            MiuiInputSettingsConnection.this = r1;
        }

        @Override // android.content.ServiceConnection
        public void onServiceConnected(ComponentName name, IBinder service) {
            Slog.w(MiuiInputSettingsConnection.TAG, "service connected");
            synchronized (MiuiInputSettingsConnection.this) {
                MiuiInputSettingsConnection.this.mMessenger = new Messenger(service);
                MiuiInputSettingsConnection.this.mCachedMessageList.forEach(new BiConsumer() { // from class: com.miui.server.input.MiuiInputSettingsConnection$InputSettingsServiceConnection$$ExternalSyntheticLambda0
                    @Override // java.util.function.BiConsumer
                    public final void accept(Object obj, Object obj2) {
                        MiuiInputSettingsConnection.InputSettingsServiceConnection.this.m2174xe3135f13((Message) obj, (MiuiInputSettingsConnection.OnMessageSendFinishCallback) obj2);
                    }
                });
                MiuiInputSettingsConnection.this.resetTimer();
                MiuiInputSettingsConnection.this.mCachedMessageList.clear();
            }
        }

        /* renamed from: lambda$onServiceConnected$0$com-miui-server-input-MiuiInputSettingsConnection$InputSettingsServiceConnection */
        public /* synthetic */ void m2174xe3135f13(Message message, OnMessageSendFinishCallback callback) {
            try {
                Slog.i(MiuiInputSettingsConnection.TAG, "send cached message " + MiuiInputSettingsConnection.messageWhatServerToString(message.what) + " to settings");
                MiuiInputSettingsConnection.this.mMessenger.send(message);
                MiuiInputSettingsConnection.this.executeCallback(callback, 0);
            } catch (RemoteException e) {
                MiuiInputSettingsConnection.this.executeCallback(callback, 1);
                Slog.e(MiuiInputSettingsConnection.TAG, e.getMessage(), e);
            }
        }

        @Override // android.content.ServiceConnection
        public void onServiceDisconnected(ComponentName name) {
            Slog.w(MiuiInputSettingsConnection.TAG, "service disconnected");
            MiuiInputSettingsConnection.this.resetConnection();
        }
    }

    public void executeCallback(OnMessageSendFinishCallback callback, int status) {
        if (callback == null) {
            return;
        }
        callback.onMessageSendFinish(status);
    }

    /* loaded from: classes.dex */
    public class CallbackHandler extends Handler {
        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public CallbackHandler(Looper looper) {
            super(looper);
            MiuiInputSettingsConnection.this = this$0;
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            Consumer<Message> messageConsumer;
            Slog.i(MiuiInputSettingsConnection.TAG, "receive message from settings " + MiuiInputSettingsConnection.messageWhatClientToString(msg.what));
            MiuiInputSettingsConnection.this.resetTimer();
            synchronized (MiuiInputSettingsConnection.this.mCallbackMap) {
                messageConsumer = (Consumer) MiuiInputSettingsConnection.this.mCallbackMap.get(Integer.valueOf(msg.what));
            }
            if (messageConsumer == null) {
                Slog.w(MiuiInputSettingsConnection.TAG, "not found callback for what = " + MiuiInputSettingsConnection.messageWhatClientToString(msg.what));
            } else {
                messageConsumer.accept(msg);
            }
        }
    }

    public static String messageWhatServerToString(int messageWhatServer) {
        switch (messageWhatServer) {
            case 1:
                return "MSG_SERVER_ADD_STYLUS_MASK";
            case 2:
                return "MSG_SERVER_REMOVE_STYLUS_MASK";
            default:
                return "NONE";
        }
    }

    public static String messageWhatClientToString(int messageWhatClient) {
        if (messageWhatClient == 1) {
            return "MSG_CLIENT_TRIGGER_QUICK_NOTE";
        }
        return "NONE";
    }
}
