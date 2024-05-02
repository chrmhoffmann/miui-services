package com.miui.server.smartpower;

import android.content.Context;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.EventLog;
import android.util.Slog;
import com.android.server.power.PowerManagerServiceStub;
import java.util.ArrayList;
import java.util.Iterator;
/* loaded from: classes.dex */
public class AppWakelockResource extends AppPowerResource {
    private static final int BLOOEAN_DISABLE = 0;
    private static final int BLOOEAN_ENABLE = 1;
    private static final int MSG_SET_WAKELOCK_STATE = 1;
    private Handler mHandler;
    private final Object mLock = new Object();
    private final ArrayList<WakeLock> mWakeLocks = new ArrayList<>();

    public AppWakelockResource(Context context, Looper looper) {
        this.mHandler = new MyHandler(looper);
        this.mType = 4;
    }

    @Override // com.miui.server.smartpower.AppPowerResource
    public ArrayList getActiveUids() {
        ArrayList<Integer> uids = new ArrayList<>();
        synchronized (this.mLock) {
            Iterator<WakeLock> it = this.mWakeLocks.iterator();
            while (it.hasNext()) {
                WakeLock wakeLock = it.next();
                uids.add(Integer.valueOf(wakeLock.mOwnerUid));
            }
        }
        return uids;
    }

    @Override // com.miui.server.smartpower.AppPowerResource
    public boolean isAppResourceActive(int uid) {
        synchronized (this.mLock) {
            Iterator<WakeLock> it = this.mWakeLocks.iterator();
            while (it.hasNext()) {
                WakeLock wakeLock = it.next();
                if (wakeLock.mOwnerUid == uid) {
                    return true;
                }
            }
            return false;
        }
    }

    @Override // com.miui.server.smartpower.AppPowerResource
    public boolean isAppResourceActive(int uid, int pid) {
        synchronized (this.mLock) {
            Iterator<WakeLock> it = this.mWakeLocks.iterator();
            while (it.hasNext()) {
                WakeLock wakeLock = it.next();
                if (wakeLock.mOwnerPid == pid) {
                    return true;
                }
            }
            return false;
        }
    }

    @Override // com.miui.server.smartpower.AppPowerResource
    public void releaseAppPowerResource(int uid) {
        if (UserHandle.isApp(uid)) {
            Message msg = this.mHandler.obtainMessage(1);
            msg.arg1 = uid;
            msg.arg2 = 0;
            this.mHandler.sendMessage(msg);
        }
    }

    @Override // com.miui.server.smartpower.AppPowerResource
    public void resumeAppPowerResource(int uid) {
        if (UserHandle.isApp(uid)) {
            Message msg = this.mHandler.obtainMessage(1);
            msg.arg1 = uid;
            msg.arg2 = 1;
            this.mHandler.sendMessage(msg);
        }
    }

    public void setWakeLockState(int uid, boolean enable) {
        try {
            PowerManagerServiceStub.get().setUidPartialWakeLockDisabledState(uid, (String) null, !enable);
            String reason = enable ? "resume" : "release";
            EventLog.writeEvent((int) SmartPowerSettings.EVENT_TAGS, "wakelock u:" + uid + " s:" + reason);
            if (DEBUG) {
                Slog.d(AppPowerResourceManager.TAG, "setWakeLockState " + uid + " s:" + reason);
            }
        } catch (Exception e) {
            Slog.e(AppPowerResourceManager.TAG, "releaseAppPowerResource", e);
        }
    }

    public void acquireWakelock(IBinder lock, int flags, String tag, int ownerUid, int ownerPid) {
        WakeLock wakeLock;
        synchronized (this.mLock) {
            int index = findWakeLockIndexLocked(lock);
            if (index >= 0) {
                wakeLock = this.mWakeLocks.get(index);
                wakeLock.updateProperties(flags, tag);
            } else {
                wakeLock = new WakeLock(lock, flags, tag, ownerUid, ownerPid);
                try {
                    lock.linkToDeath(wakeLock, 0);
                    this.mWakeLocks.add(wakeLock);
                } catch (RemoteException e) {
                    throw new IllegalArgumentException("Wake lock is already dead.");
                }
            }
            if (DEBUG) {
                Slog.d(AppPowerResourceManager.TAG, "onAcquireWakelock: " + wakeLock.toString());
            }
        }
    }

    public void releaseWakelock(IBinder lock, int flags) {
        synchronized (this.mLock) {
            int index = findWakeLockIndexLocked(lock);
            if (index < 0) {
                return;
            }
            WakeLock wakeLock = this.mWakeLocks.get(index);
            if (DEBUG) {
                Slog.d(AppPowerResourceManager.TAG, "onReleaseWakelock: " + wakeLock.toString());
            }
            try {
                wakeLock.mLock.unlinkToDeath(wakeLock, 0);
            } catch (Exception e) {
                Slog.w(AppPowerResourceManager.TAG, "unlinkToDeath failed wakelock=" + wakeLock.toString() + " lock=" + lock, e);
            }
            removeWakeLockLocked(wakeLock, index);
        }
    }

    private int findWakeLockIndexLocked(IBinder lock) {
        int count = this.mWakeLocks.size();
        for (int i = 0; i < count; i++) {
            if (this.mWakeLocks.get(i).mLock == lock) {
                return i;
            }
        }
        return -1;
    }

    public void handleWakeLockDeath(WakeLock wakeLock) {
        synchronized (this.mLock) {
            int index = this.mWakeLocks.indexOf(wakeLock);
            if (index < 0) {
                return;
            }
            removeWakeLockLocked(wakeLock, index);
        }
    }

    private void removeWakeLockLocked(WakeLock wakeLock, int index) {
        this.mWakeLocks.remove(index);
    }

    /* loaded from: classes.dex */
    public final class WakeLock implements IBinder.DeathRecipient {
        public int mFlags;
        public final IBinder mLock;
        public final int mOwnerPid;
        public final int mOwnerUid;
        public String mTag;

        public WakeLock(IBinder lock, int flags, String tag, int ownerUid, int ownerPid) {
            AppWakelockResource.this = this$0;
            this.mLock = lock;
            this.mFlags = flags;
            this.mTag = tag;
            this.mOwnerUid = ownerUid;
            this.mOwnerPid = ownerPid;
        }

        public void updateProperties(int flags, String tag) {
            this.mFlags = flags;
            this.mTag = tag;
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            AppWakelockResource.this.handleWakeLockDeath(this);
        }

        private String getLockLevelString() {
            switch (this.mFlags & 65535) {
                case 1:
                    return "PARTIAL_WAKE_LOCK             ";
                case 6:
                    return "SCREEN_DIM_WAKE_LOCK          ";
                case 10:
                    return "SCREEN_BRIGHT_WAKE_LOCK       ";
                case 26:
                    return "FULL_WAKE_LOCK                ";
                case 32:
                    return "PROXIMITY_SCREEN_OFF_WAKE_LOCK";
                case 64:
                    return "DOZE_WAKE_LOCK                ";
                case 128:
                    return "DRAW_WAKE_LOCK                ";
                default:
                    return "???                           ";
            }
        }

        private String getLockFlagsString() {
            String result = "";
            if ((this.mFlags & 268435456) != 0) {
                result = result + " ACQUIRE_CAUSES_WAKEUP";
            }
            if ((this.mFlags & 536870912) != 0) {
                return result + " ON_AFTER_RELEASE";
            }
            return result;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(getLockLevelString());
            sb.append(" '");
            sb.append(this.mTag);
            sb.append("'");
            sb.append(getLockFlagsString());
            sb.append(" (uid=");
            sb.append(this.mOwnerUid);
            if (this.mOwnerPid != 0) {
                sb.append(" pid=");
                sb.append(this.mOwnerPid);
            }
            sb.append(")");
            return sb.toString();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class MyHandler extends Handler {
        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        MyHandler(Looper looper) {
            super(looper);
            AppWakelockResource.this = r1;
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1:
                    AppWakelockResource.this.setWakeLockState(msg.arg1, msg.arg2 != 0);
                    return;
                default:
                    return;
            }
        }
    }
}
