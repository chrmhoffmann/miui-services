package com.android.server.pm;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.os.Build;
import android.util.Log;
import com.android.server.padkeyboard.iic.CommunicationUtil;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
/* loaded from: classes.dex */
public class ProfileTranscoder {
    private static final int HOT = 1;
    private static final int INLINE_CACHE_MEGAMORPHIC_ENCODING = 7;
    private static final int INLINE_CACHE_MISSING_TYPES_ENCODING = 6;
    static final int MIN_SUPPORTED_SDK = 31;
    private static final int POST_STARTUP = 4;
    private static final String PROFILE_META_LOCATION = "dexopt/baseline.profm";
    private static final String PROFILE_SOURCE_LOCATION = "dexopt/baseline.prof";
    private static final int STARTUP = 2;
    private static final String TAG = "ProfileTranscode";
    private static final String TEMP_PRIMARY = "primary.prof";
    private final String mApkName;
    private final AssetManager mAssetManager;
    private final String mBasePath;
    private boolean mDeviceSupportTranscode;
    private final File mTarget;
    static final byte[] MAGIC_PROF = {112, 114, 111, 0};
    static final byte[] MAGIC_PROFM = {112, 114, 109, 0};
    static final byte[] V015_S = {48, 49, CommunicationUtil.COMMAND_AUTH_7, 0};
    static final byte[] V010_P = {48, 49, 48, 0};
    static final byte[] METADATA_V001_N = {48, 48, 49, 0};
    static final byte[] METADATA_V002 = {48, 48, 50, 0};
    private byte[] mTranscodedProfile = null;
    private DexProfileData[] mProfile = null;
    private final byte[] mDesiredVersion = desiredVersion();
    private Encoding mEncoding = new Encoding();

    public ProfileTranscoder(File target, String apkName, String basePath, AssetManager assetManager) {
        this.mDeviceSupportTranscode = false;
        this.mTarget = target;
        this.mApkName = apkName;
        this.mBasePath = basePath;
        this.mAssetManager = assetManager;
        this.mDeviceSupportTranscode = isSupportTranscode();
    }

    public ProfileTranscoder read() {
        AssetFileDescriptor profile_fd;
        InputStream is;
        if (!this.mDeviceSupportTranscode) {
            return this;
        }
        try {
            profile_fd = this.mAssetManager.openFd(PROFILE_SOURCE_LOCATION);
        } catch (Exception e) {
            Log.d(TAG, "read profile exception: " + e);
            this.mProfile = null;
        }
        try {
            AssetFileDescriptor meta_fd = this.mAssetManager.openFd(PROFILE_META_LOCATION);
            try {
                is = profile_fd.createInputStream();
            } catch (Exception e2) {
                Log.d(TAG, "read   exception: " + e2);
                this.mProfile = null;
            }
            try {
                byte[] baselineVersion = readHeader(is, MAGIC_PROF);
                this.mProfile = readProfile(is, baselineVersion, this.mApkName);
                if (is != null) {
                    is.close();
                }
                DexProfileData[] profile = this.mProfile;
                if (profile != null) {
                    try {
                        is = meta_fd.createInputStream();
                        try {
                            byte[] metaVersion = readHeader(is, MAGIC_PROFM);
                            this.mProfile = readMeta(is, metaVersion, this.mDesiredVersion, profile);
                            if (is != null) {
                                is.close();
                            }
                        } finally {
                        }
                    } catch (Exception e3) {
                        Log.d(TAG, "read meta profile exception: " + e3);
                        this.mProfile = null;
                    }
                }
                if (meta_fd != null) {
                    meta_fd.close();
                }
                if (profile_fd != null) {
                    profile_fd.close();
                }
                return this;
            } finally {
            }
        } catch (Throwable th) {
            if (PROFILE_SOURCE_LOCATION != 0) {
                try {
                    PROFILE_SOURCE_LOCATION.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
            }
            throw th;
        }
    }

    public ProfileTranscoder transcodeIfNeeded() {
        ByteArrayOutputStream os;
        boolean success;
        DexProfileData[] profile = this.mProfile;
        byte[] desiredVersion = this.mDesiredVersion;
        if (profile == null || desiredVersion == null) {
            return this;
        }
        try {
            os = new ByteArrayOutputStream();
            writeHeader(os, desiredVersion);
            success = transcodeAndWriteBody(os, desiredVersion, profile);
        } catch (Exception e) {
            this.mTranscodedProfile = null;
            Log.d(TAG, "transcodeIfNeeded Exception:" + e);
        }
        if (!success) {
            this.mProfile = null;
            os.close();
            return this;
        }
        this.mTranscodedProfile = os.toByteArray();
        os.close();
        this.mProfile = null;
        return this;
    }

    /* JADX WARN: Type inference failed for: r3v1, types: [com.android.server.pm.DexProfileData[], byte[]] */
    public boolean write() {
        byte[] transcodedProfile = this.mTranscodedProfile;
        if (transcodedProfile == null) {
            return false;
        }
        File temp = new File(this.mBasePath, TEMP_PRIMARY);
        try {
            InputStream bis = new ByteArrayInputStream(transcodedProfile);
            try {
                OutputStream os = new FileOutputStream(temp);
                this.mEncoding.writeAll(bis, os);
                zipTarget(temp);
                os.close();
                bis.close();
                return true;
            } catch (Throwable th) {
                try {
                    bis.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
                throw th;
            }
        } catch (Exception e) {
            Log.d(TAG, "write Exception: " + e);
            return false;
        } finally {
            this.mTranscodedProfile = null;
            this.mProfile = null;
            temp.delete();
        }
    }

    private void zipTarget(File file) throws Exception {
        InputStream is = new FileInputStream(file);
        try {
            ZipOutputStream os = new ZipOutputStream(new FileOutputStream(this.mTarget));
            os.putNextEntry(new ZipEntry(file.getName()));
            this.mEncoding.writeAll(is, os);
            os.close();
            is.close();
        } catch (Throwable th) {
            try {
                is.close();
            } catch (Throwable th2) {
                th.addSuppressed(th2);
            }
            throw th;
        }
    }

    private boolean isSupportTranscode() {
        byte[] bArr = this.mDesiredVersion;
        return bArr != null && Arrays.equals(bArr, V015_S);
    }

    private byte[] desiredVersion() {
        if (Build.VERSION.SDK_INT >= 31) {
            return V015_S;
        }
        return null;
    }

    byte[] readHeader(InputStream is, byte[] magic) throws Exception {
        byte[] fileMagic = this.mEncoding.read(is, magic.length);
        if (!Arrays.equals(magic, fileMagic)) {
            throw error("Invalid magic");
        }
        return this.mEncoding.read(is, V010_P.length);
    }

    void writeHeader(OutputStream os, byte[] version) throws Exception {
        os.write(MAGIC_PROF);
        os.write(version);
    }

    boolean transcodeAndWriteBody(OutputStream os, byte[] desiredVersion, DexProfileData[] data) throws Exception {
        if (Arrays.equals(desiredVersion, V015_S)) {
            writeProfileForS(os, data);
            return true;
        }
        return false;
    }

    private void writeProfileForS(OutputStream os, DexProfileData[] profileData) throws Exception {
        writeProfileSections(os, profileData);
    }

    private void writeProfileSections(OutputStream os, DexProfileData[] profileData) throws Exception {
        List<WritableFileSection> sections = new ArrayList<>(3);
        List<byte[]> sectionContents = new ArrayList<>(3);
        sections.add(writeDexFileSection(profileData));
        sections.add(createCompressibleClassSection(profileData));
        sections.add(createCompressibleMethodsSection(profileData));
        long offset = V015_S.length + MAGIC_PROF.length;
        long offset2 = offset + 4 + (sections.size() * 16);
        this.mEncoding.writeUInt32(os, sections.size());
        for (int i = 0; i < sections.size(); i++) {
            WritableFileSection section = sections.get(i);
            this.mEncoding.writeUInt32(os, section.mType.getValue());
            this.mEncoding.writeUInt32(os, offset2);
            if (section.mNeedsCompression) {
                long inflatedSize = section.mContents.length;
                byte[] compressed = this.mEncoding.compress(section.mContents);
                sectionContents.add(compressed);
                this.mEncoding.writeUInt32(os, compressed.length);
                this.mEncoding.writeUInt32(os, inflatedSize);
                offset2 += compressed.length;
            } else {
                sectionContents.add(section.mContents);
                this.mEncoding.writeUInt32(os, section.mContents.length);
                this.mEncoding.writeUInt32(os, 0L);
                offset2 += section.mContents.length;
            }
        }
        for (int i2 = 0; i2 < sectionContents.size(); i2++) {
            os.write(sectionContents.get(i2));
        }
    }

    private WritableFileSection writeDexFileSection(DexProfileData[] profileData) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int expectedSize = 0 + 2;
        try {
            this.mEncoding.writeUInt16(out, profileData.length);
            for (DexProfileData profile : profileData) {
                this.mEncoding.writeUInt32(out, profile.dexChecksum);
                this.mEncoding.writeUInt32(out, profile.mTypeIdCount);
                this.mEncoding.writeUInt32(out, profile.numMethodIds);
                String profileKey = generateDexKey(profile.apkName, profile.dexName, V015_S);
                int keyLength = this.mEncoding.utf8Length(profileKey);
                this.mEncoding.writeUInt16(out, keyLength);
                expectedSize = expectedSize + 4 + 4 + 4 + 2 + (keyLength * 1);
                this.mEncoding.writeString(out, profileKey);
            }
            byte[] contents = out.toByteArray();
            if (expectedSize != contents.length) {
                throw error("Expected size " + expectedSize + ", does not match actual size " + contents.length);
            }
            WritableFileSection writableFileSection = new WritableFileSection(FileSectionType.DEX_FILES, expectedSize, contents, false);
            out.close();
            return writableFileSection;
        } catch (Throwable th) {
            try {
                out.close();
            } catch (Throwable th2) {
                th.addSuppressed(th2);
            }
            throw th;
        }
    }

    private WritableFileSection createCompressibleClassSection(DexProfileData[] profileData) throws Exception {
        int expectedSize = 0;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (int i = 0; i < profileData.length; i++) {
            try {
                DexProfileData profile = profileData[i];
                this.mEncoding.writeUInt16(out, i);
                this.mEncoding.writeUInt16(out, profile.classSetSize);
                expectedSize = expectedSize + 2 + 2 + (profile.classSetSize * 2);
                writeClasses(out, profile);
            } catch (Throwable th) {
                try {
                    out.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
                throw th;
            }
        }
        byte[] contents = out.toByteArray();
        if (expectedSize != contents.length) {
            throw error("Expected size " + expectedSize + ", does not match actual size " + contents.length);
        }
        WritableFileSection writableFileSection = new WritableFileSection(FileSectionType.CLASSES, expectedSize, contents, true);
        out.close();
        return writableFileSection;
    }

    private WritableFileSection createCompressibleMethodsSection(DexProfileData[] profileData) throws Exception {
        int expectedSize = 0;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (int i = 0; i < profileData.length; i++) {
            try {
                DexProfileData profile = profileData[i];
                int methodFlags = computeMethodFlags(profile);
                byte[] bitmapContents = createMethodBitmapRegion(profile);
                byte[] methodRegionContents = createMethodsWithInlineCaches(profile);
                this.mEncoding.writeUInt16(out, i);
                int followingDataSize = bitmapContents.length + 2 + methodRegionContents.length;
                this.mEncoding.writeUInt32(out, followingDataSize);
                this.mEncoding.writeUInt16(out, methodFlags);
                out.write(bitmapContents);
                out.write(methodRegionContents);
                expectedSize = expectedSize + 2 + 4 + followingDataSize;
            } catch (Throwable th) {
                try {
                    out.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
                throw th;
            }
        }
        byte[] contents = out.toByteArray();
        if (expectedSize != contents.length) {
            throw error("Expected size " + expectedSize + ", does not match actual size " + contents.length);
        }
        WritableFileSection writableFileSection = new WritableFileSection(FileSectionType.METHODS, expectedSize, contents, true);
        out.close();
        return writableFileSection;
    }

    private byte[] createMethodBitmapRegion(DexProfileData profile) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            writeMethodBitmap(out, profile);
            byte[] byteArray = out.toByteArray();
            out.close();
            return byteArray;
        } catch (Throwable th) {
            try {
                out.close();
            } catch (Throwable th2) {
                th.addSuppressed(th2);
            }
            throw th;
        }
    }

    private byte[] createMethodsWithInlineCaches(DexProfileData profile) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            writeMethodsWithInlineCaches(out, profile);
            byte[] byteArray = out.toByteArray();
            out.close();
            return byteArray;
        } catch (Throwable th) {
            try {
                out.close();
            } catch (Throwable th2) {
                th.addSuppressed(th2);
            }
            throw th;
        }
    }

    private int computeMethodFlags(DexProfileData profileData) {
        int methodFlags = 0;
        for (Map.Entry<Integer, Integer> entry : profileData.methods.entrySet()) {
            int flagValue = entry.getValue().intValue();
            methodFlags |= flagValue;
        }
        return methodFlags;
    }

    private int getMethodBitmapStorageSize(int numMethodIds) {
        int methodBitmapBits = numMethodIds * 2;
        return roundUpToByte(methodBitmapBits) / 8;
    }

    private int roundUpToByte(int bits) {
        return ((bits + 8) - 1) & (-8);
    }

    private void setMethodBitmapBit(byte[] bitmap, int flag, int methodIndex, DexProfileData dexData) throws Exception {
        int bitIndex = methodFlagBitmapIndex(flag, methodIndex, dexData.numMethodIds);
        int bitmapIndex = bitIndex / 8;
        byte value = (byte) (bitmap[bitmapIndex] | (1 << (bitIndex % 8)));
        bitmap[bitmapIndex] = value;
    }

    private void writeMethodsWithInlineCaches(OutputStream os, DexProfileData dexData) throws Exception {
        int lastMethodIndex = 0;
        for (Map.Entry<Integer, Integer> entry : dexData.methods.entrySet()) {
            int methodId = entry.getKey().intValue();
            int flags = entry.getValue().intValue();
            if ((flags & 1) != 0) {
                int diffWithTheLastMethodIndex = methodId - lastMethodIndex;
                this.mEncoding.writeUInt16(os, diffWithTheLastMethodIndex);
                this.mEncoding.writeUInt16(os, 0);
                lastMethodIndex = methodId;
            }
        }
    }

    private void writeClasses(OutputStream os, DexProfileData dexData) throws Exception {
        int lastClassIndex = 0;
        for (int i : dexData.classes) {
            Integer classIndex = Integer.valueOf(i);
            int diffWithTheLastClassIndex = classIndex.intValue() - lastClassIndex;
            this.mEncoding.writeUInt16(os, diffWithTheLastClassIndex);
            lastClassIndex = classIndex.intValue();
        }
    }

    private void writeMethodBitmap(OutputStream os, DexProfileData dexData) throws Exception {
        byte[] bitmap = new byte[getMethodBitmapStorageSize(dexData.numMethodIds)];
        for (Map.Entry<Integer, Integer> entry : dexData.methods.entrySet()) {
            int methodIndex = entry.getKey().intValue();
            int flagValue = entry.getValue().intValue();
            if ((flagValue & 2) != 0) {
                setMethodBitmapBit(bitmap, 2, methodIndex, dexData);
            }
            if ((flagValue & 4) != 0) {
                setMethodBitmapBit(bitmap, 4, methodIndex, dexData);
            }
        }
        os.write(bitmap);
    }

    DexProfileData[] readProfile(InputStream is, byte[] version, String apkName) throws Exception {
        if (!Arrays.equals(version, V010_P)) {
            throw error("Unsupported version");
        }
        int numberOfDexFiles = this.mEncoding.readUInt8(is);
        long uncompressedDataSize = this.mEncoding.readUInt32(is);
        long compressedDataSize = this.mEncoding.readUInt32(is);
        byte[] uncompressedData = this.mEncoding.readCompressed(is, (int) compressedDataSize, (int) uncompressedDataSize);
        if (is.read() > 0) {
            throw error("Content found after the end of file");
        }
        InputStream dataStream = new ByteArrayInputStream(uncompressedData);
        try {
            DexProfileData[] readUncompressedBody = readUncompressedBody(dataStream, apkName, numberOfDexFiles);
            dataStream.close();
            return readUncompressedBody;
        } catch (Throwable th) {
            try {
                dataStream.close();
            } catch (Throwable th2) {
                th.addSuppressed(th2);
            }
            throw th;
        }
    }

    DexProfileData[] readMeta(InputStream is, byte[] metadataVersion, byte[] desiredProfileVersion, DexProfileData[] profile) throws Exception {
        if (Arrays.equals(metadataVersion, METADATA_V001_N)) {
            throw error("Requires new Baseline Profile Metadata. Please rebuild the APK with Android Gradle Plugin 7.2 Canary 7 or higher");
        }
        if (Arrays.equals(metadataVersion, METADATA_V002)) {
            return readMetadataV002(is, desiredProfileVersion, profile);
        }
        throw error("Unsupported meta version");
    }

    DexProfileData[] readMetadataV002(InputStream is, byte[] desiredProfileVersion, DexProfileData[] profile) throws Exception {
        int dexFileCount = this.mEncoding.readUInt16(is);
        long uncompressed = this.mEncoding.readUInt32(is);
        long compressed = this.mEncoding.readUInt32(is);
        byte[] contents = this.mEncoding.readCompressed(is, (int) compressed, (int) uncompressed);
        if (is.read() > 0) {
            throw error("Content found after the end of file");
        }
        InputStream dataStream = new ByteArrayInputStream(contents);
        try {
            DexProfileData[] readMetadataV002Body = readMetadataV002Body(dataStream, desiredProfileVersion, dexFileCount, profile);
            dataStream.close();
            return readMetadataV002Body;
        } catch (Throwable th) {
            try {
                dataStream.close();
            } catch (Throwable th2) {
                th.addSuppressed(th2);
            }
            throw th;
        }
    }

    private DexProfileData[] readMetadataV002Body(InputStream is, byte[] desiredProfileVersion, int dexFileCount, DexProfileData[] profile) throws Exception {
        if (is.available() == 0) {
            return new DexProfileData[0];
        }
        if (dexFileCount != profile.length) {
            throw error("Mismatched number of dex files found in metadata");
        }
        for (int i = 0; i < dexFileCount; i++) {
            this.mEncoding.readUInt16(is);
            int profileKeySize = this.mEncoding.readUInt16(is);
            String profileKey = this.mEncoding.readString(is, profileKeySize);
            long typeIdCount = this.mEncoding.readUInt32(is);
            int classIdSetSize = this.mEncoding.readUInt16(is);
            DexProfileData data = findByDexName(profile, profileKey);
            if (data == null) {
                throw error("Missing profile key: " + profileKey);
            }
            data.mTypeIdCount = typeIdCount;
            readClasses(is, classIdSetSize);
        }
        return profile;
    }

    private DexProfileData findByDexName(DexProfileData[] profile, String profileKey) {
        if (profile.length <= 0) {
            return null;
        }
        String dexName = extractKey(profileKey);
        for (int i = 0; i < profile.length; i++) {
            if (profile[i].dexName.equals(dexName)) {
                return profile[i];
            }
        }
        return null;
    }

    private String generateDexKey(String apkName, String dexName, byte[] version) {
        if (apkName.length() <= 0) {
            return enforceSeparator(dexName, "!");
        }
        if (dexName.equals("classes.dex")) {
            return apkName;
        }
        if (dexName.contains("!") || dexName.contains(":")) {
            return enforceSeparator(dexName, "!");
        }
        return dexName.endsWith(".apk") ? dexName : apkName + "!" + dexName;
    }

    private String enforceSeparator(String value, String separator) {
        if ("!".equals(separator)) {
            return value.replace(":", "!");
        }
        if (":".equals(separator)) {
            return value.replace("!", ":");
        }
        return value;
    }

    private String extractKey(String profileKey) {
        int index = profileKey.indexOf("!");
        if (index < 0) {
            index = profileKey.indexOf(":");
        }
        if (index > 0) {
            return profileKey.substring(index + 1);
        }
        return profileKey;
    }

    private DexProfileData[] readUncompressedBody(InputStream is, String apkName, int numberOfDexFiles) throws Exception {
        if (is.available() == 0) {
            return new DexProfileData[0];
        }
        DexProfileData[] lines = new DexProfileData[numberOfDexFiles];
        for (int i = 0; i < numberOfDexFiles; i++) {
            int dexNameSize = this.mEncoding.readUInt16(is);
            int classSetSize = this.mEncoding.readUInt16(is);
            long hotMethodRegionSize = this.mEncoding.readUInt32(is);
            long dexChecksum = this.mEncoding.readUInt32(is);
            long numMethodIds = this.mEncoding.readUInt32(is);
            lines[i] = new DexProfileData(apkName, this.mEncoding.readString(is, dexNameSize), dexChecksum, 0L, classSetSize, (int) hotMethodRegionSize, (int) numMethodIds, new int[classSetSize], new TreeMap());
        }
        for (DexProfileData data : lines) {
            readHotMethodRegion(is, data);
            data.classes = readClasses(is, data.classSetSize);
            readMethodBitmap(is, data);
        }
        return lines;
    }

    private void readHotMethodRegion(InputStream is, DexProfileData data) throws Exception {
        int expectedBytesAvailableAfterRead = is.available() - data.hotMethodRegionSize;
        int lastMethodIndex = 0;
        while (is.available() > expectedBytesAvailableAfterRead) {
            int diffWithLastMethodDexIndex = this.mEncoding.readUInt16(is);
            int methodDexIndex = lastMethodIndex + diffWithLastMethodDexIndex;
            data.methods.put(Integer.valueOf(methodDexIndex), 1);
            for (int inlineCacheSize = this.mEncoding.readUInt16(is); inlineCacheSize > 0; inlineCacheSize--) {
                skipInlineCache(is);
            }
            lastMethodIndex = methodDexIndex;
        }
        if (is.available() != expectedBytesAvailableAfterRead) {
            throw error("Read too much data during profile line parse");
        }
    }

    private void skipInlineCache(InputStream is) throws Exception {
        this.mEncoding.readUInt16(is);
        int dexPcMapSize = this.mEncoding.readUInt8(is);
        if (dexPcMapSize == 6 || dexPcMapSize == 7) {
            return;
        }
        while (dexPcMapSize > 0) {
            this.mEncoding.readUInt8(is);
            for (int numClasses = this.mEncoding.readUInt8(is); numClasses > 0; numClasses--) {
                this.mEncoding.readUInt16(is);
            }
            dexPcMapSize--;
        }
    }

    private int[] readClasses(InputStream is, int classSetSize) throws Exception {
        int[] classes = new int[classSetSize];
        int lastClassIndex = 0;
        for (int k = 0; k < classSetSize; k++) {
            int diffWithTheLastClassIndex = this.mEncoding.readUInt16(is);
            int classDexIndex = lastClassIndex + diffWithTheLastClassIndex;
            classes[k] = classDexIndex;
            lastClassIndex = classDexIndex;
        }
        return classes;
    }

    private void readMethodBitmap(InputStream is, DexProfileData data) throws Exception {
        int methodBitmapStorageSize = this.mEncoding.bitsToBytes(data.numMethodIds * 2);
        byte[] methodBitmap = this.mEncoding.read(is, methodBitmapStorageSize);
        BitSet bs = BitSet.valueOf(methodBitmap);
        for (int methodIndex = 0; methodIndex < data.numMethodIds; methodIndex++) {
            int newFlags = readFlagsFromBitmap(bs, methodIndex, data.numMethodIds);
            if (newFlags != 0) {
                Integer current = data.methods.get(Integer.valueOf(methodIndex));
                if (current == null) {
                    current = 0;
                }
                data.methods.put(Integer.valueOf(methodIndex), Integer.valueOf(current.intValue() | newFlags));
            }
        }
    }

    private int readFlagsFromBitmap(BitSet bs, int methodIndex, int numMethodIds) throws Exception {
        int result = 0;
        if (bs.get(methodFlagBitmapIndex(2, methodIndex, numMethodIds))) {
            result = 0 | 2;
        }
        if (bs.get(methodFlagBitmapIndex(4, methodIndex, numMethodIds))) {
            return result | 4;
        }
        return result;
    }

    private int methodFlagBitmapIndex(int flag, int methodIndex, int numMethodIds) throws Exception {
        switch (flag) {
            case 1:
                throw error("HOT methods are not stored in the bitmap");
            case 2:
                return methodIndex;
            case 3:
            default:
                throw error("Unexpected flag: " + flag);
            case 4:
                return methodIndex + numMethodIds;
        }
    }

    Exception error(String message) {
        return new Exception(message);
    }
}
