package com.android.server;

import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.os.IVold;
import android.os.IVoldTaskListener;
import android.os.PersistableBundle;
import android.os.UserHandle;
import android.util.Slog;
import com.miui.base.MiuiStubRegistry;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
/* loaded from: classes.dex */
public class MiuiCldImpl implements MiuiCldStub {
    private static final int CLD_ERROR = 2;
    private static final int CLD_FREQUENT = 1;
    private static final int CLD_NOT_SUPPORT = -1;
    private static final int CLD_SUCCESS = 0;
    private static final long DEFAULT_MINIMUM_CLD_INTERVAL = 86400000;
    private static final String LAST_CLD_FILE = "last-cld";
    private static final String MIUI_CLD_PROCESSED_DONE = "miui.intent.action.MIUI_CLD_PROCESSED_DONE";
    private static final String TAG = "MiuiCldImpl";
    private final IVoldTaskListener mCldListener = new IVoldTaskListener.Stub() { // from class: com.android.server.MiuiCldImpl.1
        public void onStatus(int status, PersistableBundle extras) {
        }

        public void onFinished(int status, PersistableBundle extras) {
            if (MiuiCldImpl.this.needPostCldResult()) {
                MiuiCldImpl.this.postCldResult(extras);
            }
        }
    };
    private Context mContext;
    private File mLastCldFile;
    private volatile IVold mVold;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<MiuiCldImpl> {

        /* compiled from: MiuiCldImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final MiuiCldImpl INSTANCE = new MiuiCldImpl();
        }

        public MiuiCldImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public MiuiCldImpl provideNewInstance() {
            return new MiuiCldImpl();
        }
    }

    MiuiCldImpl() {
    }

    public void initCldListener(IVold vold, Context context) {
        this.mVold = vold;
        this.mContext = context;
        File dataDir = Environment.getDataDirectory();
        File systemDir = new File(dataDir, "system");
        this.mLastCldFile = new File(systemDir, LAST_CLD_FILE);
        try {
            Method setCldListener = IVold.class.getDeclaredMethod("setCldListener", IVoldTaskListener.class);
            setCldListener.invoke(this.mVold, this.mCldListener);
        } catch (Exception e) {
            Slog.wtf(TAG, "Failed on initCldListener", e);
        }
    }

    public int triggerCld() {
        int result = 2;
        long timeSinceLast = System.currentTimeMillis() - this.mLastCldFile.lastModified();
        if (!this.mLastCldFile.exists() || timeSinceLast > DEFAULT_MINIMUM_CLD_INTERVAL) {
            try {
                FileOutputStream fos = new FileOutputStream(this.mLastCldFile);
                fos.write(1);
                result = 0;
                Slog.i(TAG, "Trigger cld success");
                fos.close();
                return 0;
            } catch (IOException e) {
                Slog.wtf(TAG, "Failed recording last-cld", e);
                return result;
            }
        }
        Slog.i(TAG, "Last cld run in " + (timeSinceLast / 1000) + "s ago. donot run cld too frequently");
        return 1;
    }

    public int getCldFragLevel() {
        try {
            Method method = IVold.class.getDeclaredMethod("getCldFragLevel", new Class[0]);
            int fragLevel = ((Integer) method.invoke(this.mVold, new Object[0])).intValue();
            Slog.i(TAG, "frag level: " + fragLevel);
            return fragLevel;
        } catch (Exception e) {
            Slog.wtf(TAG, e);
            return -1;
        }
    }

    boolean needPostCldResult() {
        boolean post = false;
        try {
            FileInputStream fis = new FileInputStream(this.mLastCldFile);
            try {
                boolean z = true;
                if (1 != fis.read()) {
                    z = false;
                }
                post = z;
                fis.close();
            } catch (Throwable th) {
                try {
                    fis.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
                throw th;
            }
        } catch (FileNotFoundException e) {
            Slog.i(TAG, "File last-cld not exist");
        } catch (IOException e2) {
            Slog.wtf(TAG, "Failed reading last-cld", e2);
        }
        return post;
    }

    void postCldResult(PersistableBundle extras) {
        int frag_level = extras.getInt("frag_level", -1);
        Intent intent = new Intent(MIUI_CLD_PROCESSED_DONE);
        intent.putExtra("frag_level", frag_level);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
        Slog.i(TAG, "Broadcast cld processed done complete with level " + frag_level);
        try {
            FileOutputStream fos = new FileOutputStream(this.mLastCldFile);
            fos.write(0);
            fos.close();
        } catch (IOException e) {
            Slog.wtf(TAG, "Failed recording last-cld", e);
        }
    }
}
