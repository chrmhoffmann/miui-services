package com.android.server.am;
/* loaded from: classes.dex */
public interface MiuiMemoryServiceInternal {
    void interruptProcsCompaction();

    void performCompaction(String str, int i);

    void runGlobalCompaction(int i);

    void runProcCompaction(ProcessRecord processRecord, int i);

    void runProcsCompaction(int i);

    void writeLmkd(boolean z);
}
