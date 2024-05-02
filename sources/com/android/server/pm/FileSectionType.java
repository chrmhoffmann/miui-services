package com.android.server.pm;
/* compiled from: ProfileTranscoder.java */
/* loaded from: classes.dex */
enum FileSectionType {
    DEX_FILES(0),
    EXTRA_DESCRIPTORS(1),
    CLASSES(2),
    METHODS(3),
    AGGREGATION_COUNT(4);
    
    private final long mValue;

    FileSectionType(long value) {
        this.mValue = value;
    }

    public long getValue() {
        return this.mValue;
    }
}
