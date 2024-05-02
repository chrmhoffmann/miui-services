package com.miui.server.migard.utils;

import android.text.TextUtils;
import android.util.Slog;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
/* loaded from: classes.dex */
public final class FileUtils {
    private static final int BUFFER_SIZE = 4096;
    private static final String MAGIC_CODE = "MI^G^arD";
    private static final String TAG = FileUtils.class.getSimpleName();

    private FileUtils() {
    }

    public static String getFileName(String path) {
        String trimPath = path == null ? null : path.trim();
        if (trimPath == null || TextUtils.isEmpty(trimPath)) {
            return "";
        }
        String tmpPath = trimPath;
        if (tmpPath.endsWith(File.separator)) {
            tmpPath = tmpPath.substring(0, tmpPath.length() - 1);
        }
        String fileName = tmpPath.substring(tmpPath.lastIndexOf(File.separator) + 1);
        return fileName;
    }

    /* JADX WARN: Unsupported multi-entry loop pattern (BACK_EDGE: B:7:0x001d -> B:21:0x003e). Please submit an issue!!! */
    public static void writeToSys(String path, String content) {
        File file = new File(path);
        FileWriter writer = null;
        try {
            try {
            } catch (Throwable th) {
                if (writer != null) {
                    try {
                        writer.close();
                    } catch (IOException e) {
                        LogUtils.e(TAG, "", e);
                    }
                }
                throw th;
            }
        } catch (IOException e2) {
            LogUtils.e(TAG, "", e2);
        }
        if (file.exists()) {
            try {
                writer = new FileWriter(path);
                writer.write(content);
                writer.close();
            } catch (IOException e3) {
                LogUtils.e(TAG, "", e3);
                if (writer != null) {
                    writer.close();
                }
            }
        }
    }

    public static String readFromSys(String path) {
        return readFromSys(path, null);
    }

    public static String readFromSys(String path, FileReadCallback callback) {
        StringBuilder info = new StringBuilder();
        BufferedReader reader = null;
        try {
            try {
                reader = new BufferedReader(new FileReader(path));
                while (true) {
                    String temp = reader.readLine();
                    if (temp != null) {
                        if (callback != null) {
                            callback.onRead(temp);
                        }
                        info.append(temp);
                    } else {
                        try {
                            break;
                        } catch (IOException e) {
                            LogUtils.e(TAG, "", e);
                        }
                    }
                }
                reader.close();
            } catch (Exception e2) {
                LogUtils.e(TAG, "", e2);
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e3) {
                        LogUtils.e(TAG, "", e3);
                    }
                }
            }
            return info.toString();
        } catch (Throwable th) {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e4) {
                    LogUtils.e(TAG, "", e4);
                }
            }
            throw th;
        }
    }

    public static void writeToFile(String filePath, String strContent, boolean isAppend) {
        File file = new File(filePath);
        BufferedWriter writer = null;
        try {
            try {
                try {
                    writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, isAppend)));
                    writer.write(strContent);
                    writer.close();
                } catch (IOException e) {
                    LogUtils.e(TAG, "", e);
                    if (writer != null) {
                        writer.close();
                    }
                }
            } catch (Throwable th) {
                if (writer != null) {
                    try {
                        writer.close();
                    } catch (IOException e2) {
                        LogUtils.e(TAG, "", e2);
                    }
                }
                throw th;
            }
        } catch (IOException e3) {
            LogUtils.e(TAG, "", e3);
        }
    }

    public static byte[] readFileByBytes(InputStream in) {
        ByteArrayOutputStream out = null;
        try {
            try {
                out = new ByteArrayOutputStream();
                byte[] buffer = new byte[4096];
                while (true) {
                    int len = in.read(buffer);
                    if (len != -1) {
                        out.write(buffer, 0, len);
                    } else {
                        try {
                            break;
                        } catch (IOException e) {
                            LogUtils.e(TAG, "", e);
                        }
                    }
                }
                out.close();
                return out.toByteArray();
            } catch (IOException e2) {
                LogUtils.e(TAG, "", e2);
                byte[] bArr = new byte[0];
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e3) {
                        LogUtils.e(TAG, "", e3);
                    }
                }
                return bArr;
            }
        } catch (Throwable th) {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e4) {
                    LogUtils.e(TAG, "", e4);
                }
            }
            throw th;
        }
    }

    public static void writeFileByBytes(String filePath, byte[] bytes, boolean append) {
        OutputStream out = null;
        File destFile = new File(filePath);
        if (!destFile.exists()) {
            try {
                destFile.createNewFile();
            } catch (IOException e) {
                LogUtils.e(TAG, "", e);
            }
        }
        try {
            try {
                try {
                    out = new BufferedOutputStream(new FileOutputStream(destFile, append));
                    out.write(bytes);
                    out.close();
                } catch (IOException e2) {
                    LogUtils.e(TAG, "", e2);
                    if (out != null) {
                        out.close();
                    }
                }
            } catch (Throwable th) {
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e3) {
                        LogUtils.e(TAG, "", e3);
                    }
                }
                throw th;
            }
        } catch (IOException e4) {
            LogUtils.e(TAG, "", e4);
        }
    }

    public static void readSysToFile(InputStream in, String dir, String name) {
        String filePath = dir + "/" + name;
        Slog.i("GameTrace", "readSysToFile filePath: " + filePath);
        FileOutputStream out = null;
        File destFile = new File(filePath);
        if (!destFile.exists()) {
            try {
                destFile.createNewFile();
            } catch (IOException e) {
                LogUtils.e(TAG, "", e);
            }
        }
        try {
            try {
                if (destFile.exists()) {
                    try {
                        out = new FileOutputStream(destFile);
                        byte[] buf = new byte[4096];
                        while (true) {
                            int len = in.read(buf);
                            if (len < 0) {
                                break;
                            }
                            out.write(buf, 0, len);
                        }
                        out.flush();
                        out.close();
                    } catch (Exception e2) {
                        LogUtils.e(TAG, "", e2);
                        if (out != null) {
                            out.close();
                        }
                    }
                }
            } catch (Throwable th) {
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e3) {
                        LogUtils.e(TAG, "", e3);
                    }
                }
                throw th;
            }
        } catch (IOException e4) {
            LogUtils.e(TAG, "", e4);
        }
    }

    public static void readSysToZip(InputStream in, String dir, String name) {
        String zipPath = dir + "/" + name + ".zip";
        Slog.i("GameTrace", "readSysToZip zipPath: " + zipPath);
        ZipOutputStream out = null;
        File destFile = new File(zipPath);
        if (!destFile.exists()) {
            try {
                destFile.createNewFile();
            } catch (IOException e) {
                LogUtils.e(TAG, "", e);
            }
        }
        if (destFile.exists()) {
            try {
                try {
                    try {
                        out = new ZipOutputStream(new FileOutputStream(destFile));
                        out.putNextEntry(new ZipEntry(name));
                        byte[] buf = new byte[4096];
                        while (true) {
                            int len = in.read(buf);
                            if (len >= 0) {
                                out.write(buf, 0, len);
                            } else {
                                out.flush();
                                out.closeEntry();
                                out.close();
                                return;
                            }
                        }
                    } catch (Exception e2) {
                        LogUtils.e(TAG, "", e2);
                        if (out != null) {
                            out.close();
                        }
                    }
                } catch (IOException e3) {
                    LogUtils.e(TAG, "", e3);
                }
            } catch (Throwable th) {
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e4) {
                        LogUtils.e(TAG, "", e4);
                    }
                }
                throw th;
            }
        }
    }

    public static void readSysToCryptoZip(InputStream in, String dir, String name, String key, boolean aes) {
        String zipPath = dir + "/" + name + ".zip";
        ZipOutputStream out = null;
        Slog.i("GameTrace", "readSysToCryptoZip zipPath: " + zipPath);
        File destFile = new File(zipPath);
        if (!destFile.exists()) {
            try {
                destFile.createNewFile();
                destFile.setReadable(true, false);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            try {
                if (destFile.exists()) {
                    try {
                        out = new ZipOutputStream(new FileOutputStream(destFile));
                        out.putNextEntry(new ZipEntry(name));
                        out.write(MAGIC_CODE.getBytes());
                        byte[] buf = new byte[key.length()];
                        if (aes) {
                            while (true) {
                                int len = in.read(buf);
                                if (len < 0) {
                                    break;
                                }
                                out.write(SecretUtils.encryptWithAES(buf, key.getBytes()), 0, len);
                            }
                            Slog.i("GameTrace", "readSysToCryptoZip aes:" + aes + "encryptWithAES");
                        } else {
                            while (true) {
                                int len2 = in.read(buf);
                                if (len2 < 0) {
                                    break;
                                }
                                out.write(SecretUtils.encryptWithXOR(buf, key.getBytes()), 0, len2);
                            }
                            Slog.i("GameTrace", "readSysToCryptoZip aes:" + aes + "encryptWithXOR");
                        }
                        out.flush();
                        out.closeEntry();
                        out.close();
                    } catch (Exception e2) {
                        e2.printStackTrace();
                        if (out != null) {
                            out.close();
                        }
                    }
                }
            } catch (Throwable th) {
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e3) {
                        e3.printStackTrace();
                    }
                }
                throw th;
            }
        } catch (IOException e4) {
            e4.printStackTrace();
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:32:0x00c7 A[Catch: IOException -> 0x00cb, TRY_ENTER, TryCatch #5 {IOException -> 0x00cb, blocks: (B:32:0x00c7, B:36:0x00cf, B:42:0x00e0, B:44:0x00e5, B:48:0x00ef, B:50:0x00f4), top: B:62:0x0023 }] */
    /* JADX WARN: Removed duplicated region for block: B:36:0x00cf A[Catch: IOException -> 0x00cb, TRY_LEAVE, TryCatch #5 {IOException -> 0x00cb, blocks: (B:32:0x00c7, B:36:0x00cf, B:42:0x00e0, B:44:0x00e5, B:48:0x00ef, B:50:0x00f4), top: B:62:0x0023 }] */
    /* JADX WARN: Removed duplicated region for block: B:70:? A[RETURN, SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public static void readAllSystraceToZip(java.lang.String r9, java.lang.String r10, java.lang.String r11) {
        /*
            Method dump skipped, instructions count: 269
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.miui.server.migard.utils.FileUtils.readAllSystraceToZip(java.lang.String, java.lang.String, java.lang.String):void");
    }

    public static boolean readOneTraceToZip(File tracefile, ZipOutputStream zipstream) {
        FileInputStream fis = null;
        BufferedInputStream bis = null;
        byte[] bufs = new byte[10240];
        try {
            try {
                ZipEntry zipEntry = new ZipEntry(tracefile.getName());
                zipstream.putNextEntry(zipEntry);
                fis = new FileInputStream(tracefile);
                bis = new BufferedInputStream(fis, 10240);
                while (true) {
                    int read = bis.read(bufs, 0, 10240);
                    if (read != -1) {
                        zipstream.write(bufs, 0, read);
                    } else {
                        zipstream.flush();
                        try {
                            bis.close();
                            fis.close();
                            return true;
                        } catch (IOException e) {
                            e.printStackTrace();
                            return true;
                        }
                    }
                }
            } catch (IOException e2) {
                e2.printStackTrace();
                if (bis != null) {
                    try {
                        bis.close();
                    } catch (IOException e3) {
                        e3.printStackTrace();
                        return false;
                    }
                }
                if (fis != null) {
                    fis.close();
                }
                return false;
            }
        } catch (Throwable th) {
            if (bis != null) {
                try {
                    bis.close();
                } catch (IOException e4) {
                    e4.printStackTrace();
                    throw th;
                }
            }
            if (fis != null) {
                fis.close();
            }
            throw th;
        }
    }

    public static void delOldZipFile(File path) {
        String[] tmpList;
        Slog.i("GameTrace", "start  delOldZipFile");
        if (path.exists() && path.isDirectory() && (tmpList = path.list()) != null) {
            for (String aTempList : tmpList) {
                File tmpFile = new File(path, aTempList);
                if (tmpFile.isFile() && tmpFile.getName().endsWith(".zip")) {
                    tmpFile.delete();
                } else if (tmpFile.isDirectory()) {
                    delOldZipFile(tmpFile);
                }
            }
        }
    }

    public static void delTmpTraceFile(File path) {
        String[] tmpList;
        Slog.i("GameTrace", "start  delTraceFile");
        if (path.exists() && path.isDirectory() && (tmpList = path.list()) != null) {
            for (String aTempList : tmpList) {
                File tmpFile = new File(path, aTempList);
                if (tmpFile.isFile() && tmpFile.getName().endsWith("_trace")) {
                    tmpFile.delete();
                } else if (tmpFile.isDirectory()) {
                    delTmpTraceFile(tmpFile);
                }
            }
        }
    }

    public static void deleteFile(String path) {
        File f = new File(path);
        if (f.exists() && f.isFile()) {
            f.delete();
        }
    }

    /* JADX WARN: Unsupported multi-entry loop pattern (BACK_EDGE: B:7:0x0014 -> B:21:0x0028). Please submit an issue!!! */
    public static void truncateSysFile(String path) {
        FileWriter fw = null;
        try {
            try {
                try {
                    new FileWriter(new File(path)).close();
                } catch (Exception e) {
                    LogUtils.e(TAG, "", e);
                    if (0 != 0) {
                        fw.close();
                    }
                }
            } catch (IOException e2) {
                LogUtils.e(TAG, "", e2);
            }
        } catch (Throwable th) {
            if (0 != 0) {
                try {
                    fw.close();
                } catch (IOException e3) {
                    LogUtils.e(TAG, "", e3);
                }
            }
            throw th;
        }
    }
}
