package com.android.server.wm;

import android.app.TaskSnapshotHelperImpl;
import android.app.TaskSnapshotHelperStub;
import android.content.Context;
import android.text.TextUtils;
import android.util.Slog;
import android.window.TaskSnapshot;
import com.android.server.wm.TaskSnapshotPersister;
import com.miui.base.MiuiStubRegistry;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
/* loaded from: classes.dex */
public class TaskSnapshotPersisterInjectorImpl extends TaskSnapshotPersisterInjectorStub {
    public static final String BITMAP_EXTENSION = ".jpg";
    public static final String LOW_RES_FILE_POSTFIX = "_reduced";
    public static final String PROTO_EXTENSION = ".proto";
    public static final String SNAPSHOTS_DIRNAME_QS = "qs_snapshots";
    private static final String TAG = "TaskSnapshot_PInjector";

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<TaskSnapshotPersisterInjectorImpl> {

        /* compiled from: TaskSnapshotPersisterInjectorImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final TaskSnapshotPersisterInjectorImpl INSTANCE = new TaskSnapshotPersisterInjectorImpl();
        }

        public TaskSnapshotPersisterInjectorImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public TaskSnapshotPersisterInjectorImpl provideNewInstance() {
            return new TaskSnapshotPersisterInjectorImpl();
        }
    }

    TaskSnapshotPersisterInjectorImpl() {
    }

    public boolean couldPersist(TaskSnapshotPersister.DirectoryResolver resolver, ActivityRecord r) {
        return couldPersist(resolver, r, true);
    }

    public boolean couldPersist(TaskSnapshotPersister.DirectoryResolver resolver, ActivityRecord r, boolean forceUpdate) {
        String str;
        Exception e;
        if (r == null || r.getTask() == null || r.mActivityComponent == null) {
            return false;
        }
        String pkg = r.mActivityComponent.getPackageName();
        Context context = r.mWmService != null ? r.mWmService.mContext : null;
        if (!TaskSnapshotHelperStub.get().ensureEnable(context, pkg, r.shortComponentName)) {
            return false;
        }
        Task task = r.getTask();
        File file = getBitmapFileQS(resolver, task.mUserId, pkg, false);
        if (!file.exists()) {
            Slog.w(TAG, "couldPersist()...return true (shot not exist)! file=" + file.getName());
            return true;
        }
        File file2 = getProtoFileQS(resolver, task.mUserId, r.mActivityComponent.getPackageName());
        if (!file2.exists()) {
            Slog.w(TAG, "couldPersist()...return true (proto not exist)! file=" + file2.getName());
            return true;
        }
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(file2));
            try {
                String val = br.readLine();
                long beforeTime = Long.parseLong(val);
                long current = System.currentTimeMillis();
                try {
                    TaskSnapshotHelperStub taskSnapshotHelperStub = TaskSnapshotHelperStub.get();
                    str = TAG;
                    try {
                        if (!taskSnapshotHelperStub.checkExpired(pkg, current, beforeTime) && !forceUpdate) {
                            closeQuitely(br);
                            return false;
                        }
                        Slog.w(str, "couldPersist()...return true (expired) forceUpdate = " + forceUpdate);
                        closeQuitely(br);
                        return true;
                    } catch (Exception e2) {
                        e = e2;
                        br = br;
                        try {
                            Slog.w(str, "couldPersist()...exception:" + e.getMessage());
                            closeQuitely(br);
                            return false;
                        } catch (Throwable th) {
                            th = th;
                            closeQuitely(br);
                            throw th;
                        }
                    } catch (Throwable th2) {
                        th = th2;
                        br = br;
                        closeQuitely(br);
                        throw th;
                    }
                } catch (Exception e3) {
                    e = e3;
                    str = TAG;
                    br = br;
                } catch (Throwable th3) {
                    th = th3;
                    br = br;
                }
            } catch (Exception e4) {
                e = e4;
                str = TAG;
            } catch (Throwable th4) {
                th = th4;
            }
        } catch (Exception e5) {
            e = e5;
            str = TAG;
        } catch (Throwable th5) {
            th = th5;
        }
    }

    public boolean writeProto(TaskSnapshotPersister.DirectoryResolver resolver, TaskSnapshot snapshot, int userId) {
        BufferedWriter bw = null;
        try {
            try {
                File file = getProtoFileQS(resolver, userId, checkName(snapshot));
                bw = new BufferedWriter(new FileWriter(file));
                bw.write(String.valueOf(System.currentTimeMillis()));
            } catch (Exception e) {
                Slog.e(TAG, "Unable to open for proto write. " + e.getMessage());
            }
            return true;
        } finally {
            closeQuitely(bw);
        }
    }

    public boolean createDirectory(TaskSnapshotPersister.DirectoryResolver resolver, int userId) {
        File dirQS = getDirectoryQS(resolver, userId);
        if (!dirQS.exists() && !dirQS.mkdir()) {
            Slog.e(TAG, "Fail to create dir!");
            return false;
        }
        return true;
    }

    public File getBitmapFileQS(TaskSnapshotPersister.DirectoryResolver resolver, TaskSnapshot snapshot, int userId, boolean lowRes) {
        return getBitmapFileQS(resolver, userId, checkName(snapshot), lowRes);
    }

    public File getBitmapFileQS(TaskSnapshotPersister.DirectoryResolver resolver, int userId, String name, boolean lowRes) {
        return new File(getDirectoryQS(resolver, userId), name + (lowRes ? LOW_RES_FILE_POSTFIX : "") + BITMAP_EXTENSION);
    }

    public File getProtoFileQS(TaskSnapshotPersister.DirectoryResolver resolver, int userId, String name) {
        return new File(getDirectoryQS(resolver, userId), name + PROTO_EXTENSION);
    }

    public File getDirectoryQS(TaskSnapshotPersister.DirectoryResolver resolver, int userId) {
        return new File(resolver.getSystemDirectoryForUser(userId), SNAPSHOTS_DIRNAME_QS);
    }

    public String checkName(TaskSnapshot snapshot) {
        if (snapshot == null || snapshot.getTopActivityComponent() == null) {
            return "";
        }
        String name = snapshot.getTopActivityComponent().getPackageName();
        if (!TextUtils.isEmpty(name) && TaskSnapshotHelperImpl.QUICK_START_NAME_WITH_ACTIVITY_LIST.contains(name) && snapshot.getClassNameQS() != null) {
            return name + snapshot.getClassNameQS();
        }
        return name;
    }

    public void closeQuitely(Closeable stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (Exception e) {
                Slog.e(TAG, "closeQuitely()...close file failed!" + e.getMessage());
            }
        }
    }
}
