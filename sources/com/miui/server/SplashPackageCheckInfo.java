package com.miui.server;

import android.os.Parcel;
import android.os.Parcelable;
import java.text.SimpleDateFormat;
import java.util.Date;
/* loaded from: classes.dex */
public class SplashPackageCheckInfo implements Parcelable {
    public static final Parcelable.Creator<SplashPackageCheckInfo> CREATOR = new Parcelable.Creator<SplashPackageCheckInfo>() { // from class: com.miui.server.SplashPackageCheckInfo.1
        @Override // android.os.Parcelable.Creator
        public SplashPackageCheckInfo createFromParcel(Parcel source) {
            return new SplashPackageCheckInfo(source);
        }

        @Override // android.os.Parcelable.Creator
        public SplashPackageCheckInfo[] newArray(int size) {
            return new SplashPackageCheckInfo[size];
        }
    };
    public static final long IGNORE = -1;
    private long mEndCheckTime;
    private String mSplashPackageName;
    private long mStartCheckTime;

    public SplashPackageCheckInfo(String splashPackageName, long startCheckTime, long endCheckTime) {
        this.mSplashPackageName = splashPackageName;
        this.mStartCheckTime = startCheckTime;
        this.mEndCheckTime = endCheckTime;
    }

    private SplashPackageCheckInfo(Parcel source) {
        this.mSplashPackageName = source.readString();
        this.mStartCheckTime = source.readLong();
        this.mEndCheckTime = source.readLong();
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int parcelableFlags) {
        dest.writeString(this.mSplashPackageName);
        dest.writeLong(this.mStartCheckTime);
        dest.writeLong(this.mEndCheckTime);
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    public String getSplashPackageName() {
        return this.mSplashPackageName;
    }

    public boolean matchTime() {
        long currentTime = System.currentTimeMillis();
        long j = this.mStartCheckTime;
        if (j == -1 || j < currentTime) {
            long j2 = this.mEndCheckTime;
            if (j2 == -1 || j2 > currentTime) {
                return true;
            }
        }
        return false;
    }

    public boolean isExpired() {
        long j = this.mEndCheckTime;
        return j != -1 && j < System.currentTimeMillis();
    }

    public String toString() {
        return "SplashPackageCheckInfo[" + this.mSplashPackageName + ", " + getDateString(this.mStartCheckTime) + ", " + getDateString(this.mEndCheckTime) + "]";
    }

    private static String getDateString(long time) {
        if (time == -1) {
            return "IGNORE";
        }
        try {
            Date date = new Date(time);
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
        } catch (Exception e) {
            return "ERROR";
        }
    }
}
