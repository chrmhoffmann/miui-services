package com.android.server.pm;

import android.os.ParcelFileDescriptor;
import com.android.server.pm.Installer;
import java.util.List;
/* loaded from: classes.dex */
public interface IMiuiInstaller {
    int getDataFD(String str, ParcelFileDescriptor[] parcelFileDescriptorArr) throws Installer.InstallerException;

    int listDataDir(String str, long j, long j2, List<String> list, long[] jArr) throws Installer.InstallerException;

    int transferData(String str, String str2, String str3, boolean z, int i, int i2, int i3, String str4) throws Installer.InstallerException;
}
