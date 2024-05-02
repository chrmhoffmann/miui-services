package com.android.server.pm;
/* compiled from: ProfileTranscoder.java */
/* loaded from: classes.dex */
class WritableFileSection {
    final byte[] mContents;
    final int mExpectedInflateSize;
    final boolean mNeedsCompression;
    final FileSectionType mType;

    public WritableFileSection(FileSectionType type, int expectedInflateSize, byte[] contents, boolean needsCompression) {
        this.mType = type;
        this.mExpectedInflateSize = expectedInflateSize;
        this.mContents = contents;
        this.mNeedsCompression = needsCompression;
    }
}
