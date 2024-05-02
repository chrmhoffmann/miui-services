package com.android.server;

import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.os.StatFs;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.system.Os;
import android.system.OsConstants;
import android.util.Log;
import android.util.Slog;
import com.android.server.BootKeeperStubImpl;
import com.android.server.MiuiBatteryStatsService;
import com.miui.base.MiuiStubRegistry;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.channels.FileChannel;
import miui.os.Build;
/* loaded from: classes.dex */
public class BootKeeperStubImpl implements BootKeeperStub {
    private static final String TAG = "BootKeeper";
    private BOOTKEEPER_STRATEGY mBootStrategy;
    private boolean mReleaseSpaceTrigger = false;
    private static final boolean ENABLE_BOOTKEEPER = SystemProperties.getBoolean("persist.sys.bootkeeper.support", false);
    private static long MIN_BOOT_BYTES = 10485760;
    private static long SPACE_FREE_EACH = 10485760;
    private static long INIT_PLACEHOLDER_SIZE = 62914560;
    private static String PLACE_HOLDER_FILE = "/data/system/placeholder";

    /* loaded from: classes.dex */
    public enum BOOTKEEPER_STRATEGY {
        ALL,
        CLIP
    }

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<BootKeeperStubImpl> {

        /* compiled from: BootKeeperStubImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final BootKeeperStubImpl INSTANCE = new BootKeeperStubImpl();
        }

        public BootKeeperStubImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public BootKeeperStubImpl provideNewInstance() {
            throw new RuntimeException("Impl class com.android.server.BootKeeperStubImpl is marked as singleton");
        }
    }

    BootKeeperStubImpl() {
        initialize();
    }

    private void initialize() {
        this.mBootStrategy = BOOTKEEPER_STRATEGY.CLIP;
    }

    public void beforeBoot() {
        if (!ENABLE_BOOTKEEPER) {
            return;
        }
        onBootStart();
    }

    public void afterBoot(Context context) {
        if (!ENABLE_BOOTKEEPER) {
            return;
        }
        onBootSuccess();
        boolean z = this.mReleaseSpaceTrigger;
        if (z) {
            BootKeeperOneTrack.reportReleaseSpaceOneTrack(context, z);
        }
    }

    private void onBootStart() {
        long availableSpaceBytes = getAvailableSpace();
        if (availableSpaceBytes < MIN_BOOT_BYTES) {
            if (releaseSpace()) {
                Slog.d(TAG, "release space success:");
                this.mReleaseSpaceTrigger = true;
                return;
            }
            this.mReleaseSpaceTrigger = false;
            Slog.d(TAG, " The placeholder file does not exist, so space cannot be freed");
            return;
        }
        this.mReleaseSpaceTrigger = false;
    }

    private long getAvailableSpace() {
        StatFs statFs = new StatFs(Environment.getDataDirectory().getAbsolutePath());
        long availableSpaceBytes = statFs.getAvailableBytes();
        return availableSpaceBytes;
    }

    private long getFreeSpace() {
        StatFs statFs = new StatFs(Environment.getDataDirectory().getAbsolutePath());
        long freeSpaceBytes = statFs.getFreeBytes();
        return freeSpaceBytes;
    }

    private long getTotalSpace() {
        StatFs statFs = new StatFs(Environment.getDataDirectory().getAbsolutePath());
        long totalBytes = statFs.getTotalBytes();
        return totalBytes;
    }

    private void onBootSuccess() {
        File file = new File(PLACE_HOLDER_FILE);
        if (!file.exists()) {
            Slog.d(TAG, "placeholder file doesn't exists, now start to create" + PLACE_HOLDER_FILE);
            long availableSpaceBytes = getAvailableSpace();
            if (availableSpaceBytes < INIT_PLACEHOLDER_SIZE) {
                Slog.e(TAG, "available space is not enough to create placeholder file");
                return;
            }
            Slog.d(TAG, "start create " + PLACE_HOLDER_FILE);
            try {
                FileDescriptor fd = Os.open(PLACE_HOLDER_FILE, OsConstants.O_CREAT | OsConstants.O_RDWR, 438);
                Os.posix_fallocate(fd, 0L, INIT_PLACEHOLDER_SIZE);
            } catch (Exception ex) {
                Log.e(TAG, "write file fail:", ex);
            }
            Slog.d(TAG, "create PlaceHolderFile size=" + file.length());
            return;
        }
        takeUpSpace(file);
    }

    private boolean releaseSpace() {
        File file = new File(PLACE_HOLDER_FILE);
        if (!file.exists()) {
            return false;
        }
        switch (AnonymousClass1.$SwitchMap$com$android$server$BootKeeperStubImpl$BOOTKEEPER_STRATEGY[this.mBootStrategy.ordinal()]) {
            case 1:
                if (file.length() <= 0) {
                    Slog.e(TAG, "canot release space,placeholder size is 0");
                }
                try {
                    FileWriter fileWriter = new FileWriter(file);
                    fileWriter.write("");
                    fileWriter.flush();
                    fileWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Slog.d(TAG, "releaseSpace: clear placeholder content");
                return true;
            case 2:
                if (!scale(file, -SPACE_FREE_EACH)) {
                    Slog.e(TAG, "releaseSpace fail:");
                    return false;
                }
                Slog.d(TAG, "releaseSpace: " + SPACE_FREE_EACH);
                return true;
            default:
                Slog.e(TAG, "releaseSpace: unKnow Boot keeper strategy");
                return false;
        }
    }

    /* renamed from: com.android.server.BootKeeperStubImpl$1 */
    /* loaded from: classes.dex */
    public static /* synthetic */ class AnonymousClass1 {
        static final /* synthetic */ int[] $SwitchMap$com$android$server$BootKeeperStubImpl$BOOTKEEPER_STRATEGY;

        static {
            int[] iArr = new int[BOOTKEEPER_STRATEGY.values().length];
            $SwitchMap$com$android$server$BootKeeperStubImpl$BOOTKEEPER_STRATEGY = iArr;
            try {
                iArr[BOOTKEEPER_STRATEGY.ALL.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$android$server$BootKeeperStubImpl$BOOTKEEPER_STRATEGY[BOOTKEEPER_STRATEGY.CLIP.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
        }
    }

    private boolean scale(File file, long deltaSpace) {
        FileOutputStream out;
        FileChannel inChannel;
        boolean status = true;
        FileChannel inChannel2 = null;
        FileOutputStream out2 = null;
        try {
            try {
                try {
                    out = new FileOutputStream(file, true);
                    inChannel = out.getChannel();
                    if (deltaSpace < 0) {
                        inChannel.truncate(file.length() + deltaSpace);
                    } else {
                        byte[] buffer = new byte[4096];
                        int n = (int) (deltaSpace / buffer.length);
                        for (int i = 0; i < n; i++) {
                            out.write(buffer, 0, buffer.length);
                        }
                        int i2 = buffer.length;
                        long diff = deltaSpace - (i2 * n);
                        if (diff > 0) {
                            byte[] extraBuffer = new byte[4096];
                            out.write(extraBuffer, 0, extraBuffer.length);
                        }
                    }
                } catch (Throwable th) {
                    if (0 != 0) {
                        try {
                            out2.flush();
                            out2.getFD().sync();
                            inChannel2.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    throw th;
                }
            } catch (Exception e2) {
                Slog.e(TAG, "scale file fail", e2);
                status = false;
                if (0 == 0) {
                    return false;
                }
                out2.flush();
                out2.getFD().sync();
                inChannel2.close();
            }
            if (inChannel == null) {
                return true;
            }
            out.flush();
            out.getFD().sync();
            inChannel.close();
            return status;
        } catch (IOException e3) {
            e3.printStackTrace();
            return false;
        }
    }

    void takeUpSpace(File placeholder) {
        long placeHolderSize = placeholder.length();
        long diff = INIT_PLACEHOLDER_SIZE - placeHolderSize;
        if (diff > 0) {
            long availableSpaceBytes = getAvailableSpace();
            Slog.d(TAG, "availableSpaceBytes:" + availableSpaceBytes);
            switch (AnonymousClass1.$SwitchMap$com$android$server$BootKeeperStubImpl$BOOTKEEPER_STRATEGY[this.mBootStrategy.ordinal()]) {
                case 1:
                    if (availableSpaceBytes > diff) {
                        scale(placeholder, diff);
                        Slog.d(TAG, "take up space:" + diff);
                        return;
                    }
                    scale(placeholder, availableSpaceBytes);
                    Slog.d(TAG, "take up space:" + availableSpaceBytes);
                    return;
                case 2:
                    if (availableSpaceBytes > diff) {
                        scale(placeholder, diff);
                        Slog.d(TAG, "take up space:" + diff);
                        return;
                    }
                    return;
                default:
                    Slog.e(TAG, "releaseSpace: unKnow Boot keeper strategy");
                    return;
            }
        }
    }

    /* loaded from: classes.dex */
    private static class BootKeeperOneTrack {
        private static final String EXTRA_APP_ID = "31000401440";
        private static final String EXTRA_PACKAGE_NAME = "com.android.server";
        private static final int FLAG_NON_ANONYMOUS = 2;
        private static final String INTENT_ACTION_ONETRACK = "onetrack.action.TRACK_EVENT";
        private static final String INTENT_PACKAGE_ONETRACK = "com.miui.analytics";
        private static final String RELEASE_SAPCE_ARG = "release_space";
        private static final String RELEASE_SPACE_EVENT_NAME = "release_space";
        private static final String TAG = "BootKeeperOneTrack";

        private BootKeeperOneTrack() {
        }

        public static void reportReleaseSpaceOneTrack(final Context context, final boolean trigger) {
            if (context == null || Build.IS_INTERNATIONAL_BUILD) {
                return;
            }
            MiuiBgThread.getHandler().post(new Runnable() { // from class: com.android.server.BootKeeperStubImpl$BootKeeperOneTrack$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    BootKeeperStubImpl.BootKeeperOneTrack.lambda$reportReleaseSpaceOneTrack$0(trigger, context);
                }
            });
        }

        public static /* synthetic */ void lambda$reportReleaseSpaceOneTrack$0(boolean trigger, Context context) {
            Intent intent = new Intent("onetrack.action.TRACK_EVENT");
            intent.setPackage("com.miui.analytics");
            intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_APP_ID, EXTRA_APP_ID);
            intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_EVENT_NAME, "release_space");
            intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_PACKAGE, EXTRA_PACKAGE_NAME);
            intent.putExtra("release_space", trigger);
            intent.setFlags(2);
            try {
                context.startServiceAsUser(intent, UserHandle.CURRENT);
            } catch (IllegalStateException e) {
                Slog.w(TAG, "Failed to upload BootKeeper release space  event.");
            } catch (SecurityException e2) {
                Slog.w(TAG, "Unable to start service.");
            }
        }
    }
}
