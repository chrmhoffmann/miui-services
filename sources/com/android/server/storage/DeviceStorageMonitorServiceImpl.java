package com.android.server.storage;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.miui.Shell;
import android.os.Environment;
import android.os.FileUtils;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.util.DataUnit;
import android.util.Slog;
import com.android.server.LocalServices;
import com.android.server.am.ProcessPolicy;
import com.miui.base.MiuiStubRegistry;
import java.io.File;
/* loaded from: classes.dex */
public class DeviceStorageMonitorServiceImpl extends DeviceStorageMonitorServiceStub {
    private static final int LEVEL_DEEP_CLEAN = 3;
    private static final int LEVEL_NORMAL_CLEAN = 2;
    private static final String TAG = "DeviceStorageMonitorService";
    private final long FULL_THRESHOLD = DataUnit.MEBIBYTES.toBytes(50);
    private Context mContext;
    private boolean mLowStorage;
    private static final String[] EXTERNAL_SUB_DIRS = {"downloaded_rom", "ramdump", "MIUI/debug_log", "step_log", "MIUI//music/album", "MIUI/music/avatar", "MIUI/music/lyric", "MIUI/.cache/resource", "MIUI/Gallery/cloud/.cache", "MIUI/Gallery/cloud/.microthumbnailFile", "MIUI/assistant", "DuoKan/Cache", "DuoKan/Downloads/Covers", "browser/MediaCache"};
    private static final String[] DATA_SUB_DIRS = {ProcessPolicy.REASON_ANR, "tombstones", "system/dropbox", "system/app_screenshot", "system/nativedebug", "mqsas"};

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<DeviceStorageMonitorServiceImpl> {

        /* compiled from: DeviceStorageMonitorServiceImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final DeviceStorageMonitorServiceImpl INSTANCE = new DeviceStorageMonitorServiceImpl();
        }

        public DeviceStorageMonitorServiceImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public DeviceStorageMonitorServiceImpl provideNewInstance() {
            return new DeviceStorageMonitorServiceImpl();
        }
    }

    public boolean isMIUI() {
        return true;
    }

    public void onBootPhase(Context ctx, int phase) {
        this.mContext = ctx;
        if (phase == 1000) {
            final DeviceStorageMonitorInternal localService = (DeviceStorageMonitorInternal) LocalServices.getService(DeviceStorageMonitorInternal.class);
            BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() { // from class: com.android.server.storage.DeviceStorageMonitorServiceImpl.1
                @Override // android.content.BroadcastReceiver
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    if (DeviceStorageMonitorServiceImpl.this.mLowStorage && "android.intent.action.USER_PRESENT".equals(action)) {
                        localService.checkMemory();
                    } else if ("android.intent.action.BOOT_COMPLETED".equals(action)) {
                        localService.checkMemory();
                    }
                }
            };
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.intent.action.USER_PRESENT");
            filter.addAction("android.intent.action.BOOT_COMPLETED");
            ctx.registerReceiver(mBroadcastReceiver, filter);
        }
    }

    private boolean shouldCheckVolume(VolumeInfo vol) {
        return Environment.getDataDirectory().equals(vol.getPath());
    }

    public long getStorageFullBytes(StorageManager storageManager, VolumeInfo vol) {
        long fullBytes = super.getStorageFullBytes(storageManager, vol);
        if (!shouldCheckVolume(vol)) {
            return fullBytes;
        }
        long lowBytes = storageManager.getStorageLowBytes(vol.getPath());
        return Math.min(lowBytes, Math.max(this.FULL_THRESHOLD, fullBytes));
    }

    public Intent getManageStorageIntent(boolean isFull) {
        Intent lowStorageIntent = new Intent("com.miui.securitycenter.LunchCleanMaster");
        lowStorageIntent.putExtra("level", isFull ? 3 : 2);
        return lowStorageIntent;
    }

    public void enteringLowStorage(VolumeInfo vol, boolean isLow, boolean isFull, long usableBytes) {
        if (!shouldCheckVolume(vol)) {
            return;
        }
        Slog.i(TAG, "Enter low storage state, isFull=" + isFull + ", usableBytes=" + usableBytes);
        this.mLowStorage = isLow || isFull;
        startCleanerActivity(isFull);
        if (isFull) {
            deleteUnimportantFiles();
        }
    }

    public void leavingLowStorage(VolumeInfo vol, long usableBytes) {
        if (!shouldCheckVolume(vol)) {
            return;
        }
        this.mLowStorage = false;
        Slog.i(TAG, "Leave low storage state, usableBytes=" + usableBytes);
    }

    public void startCleanerActivity(boolean isFull) {
        try {
            Slog.i(TAG, "Running out of disk space, launching CleanMaster");
            Intent lowStorageIntent = new Intent("com.miui.securitycenter.action.START_LOW_MEMORY_CLEAN");
            lowStorageIntent.putExtra("level", isFull ? 3 : 2);
            lowStorageIntent.addFlags(268435456);
            this.mContext.startActivity(lowStorageIntent);
        } catch (Exception e) {
            Slog.i(TAG, "Failed to start CleanMaster " + e);
        }
    }

    private void deleteUnimportantFiles() {
        String[] strArr;
        String[] strArr2;
        try {
            Slog.i(TAG, "Storage space is extremely low, deleting unimportant files");
            File dataDir = Environment.getDataDirectory();
            File externalDir = Environment.getExternalStorageDirectory();
            for (String subDir : EXTERNAL_SUB_DIRS) {
                File dir = new File(externalDir, subDir);
                Slog.i(TAG, "Deleting " + dir);
                Shell.remove(dir.getAbsolutePath());
            }
            for (String subDir2 : DATA_SUB_DIRS) {
                File dir2 = new File(dataDir, subDir2);
                if (dir2.exists()) {
                    Slog.i(TAG, "Deleting " + dir2);
                    FileUtils.deleteContents(dir2);
                }
            }
        } catch (Throwable e) {
            Slog.w(TAG, "deleteUnimportantFiles failed", e);
        }
    }
}
