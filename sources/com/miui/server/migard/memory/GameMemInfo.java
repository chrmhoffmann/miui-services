package com.miui.server.migard.memory;

import android.os.Parcel;
import android.os.Parcelable;
/* loaded from: classes.dex */
public class GameMemInfo implements Parcelable {
    public static final Parcelable.Creator<GameMemInfo> CREATOR = new Parcelable.Creator<GameMemInfo>() { // from class: com.miui.server.migard.memory.GameMemInfo.1
        @Override // android.os.Parcelable.Creator
        public GameMemInfo createFromParcel(Parcel source) {
            GameMemInfo meminfo = new GameMemInfo();
            meminfo.pkgName = source.readString();
            meminfo.prevSize = source.readLong();
            meminfo.lastIndex = source.readInt();
            source.readLongArray(meminfo.sizeHistory);
            return meminfo;
        }

        @Override // android.os.Parcelable.Creator
        public GameMemInfo[] newArray(int size) {
            return new GameMemInfo[size];
        }
    };
    public static final long INVALID_SIZE = -1;
    private static final int MAX_CNT = 20;
    public static final int POLICY_AVG = 1;
    public static final int POLICY_MAX = 2;
    public static final int POLICY_PRE = 0;
    boolean hasUpdated;
    int lastIndex;
    String pkgName;
    long prevSize;
    long[] sizeHistory;

    GameMemInfo() {
        this(null);
    }

    public GameMemInfo(String pkg) {
        this.sizeHistory = new long[20];
        this.pkgName = pkg;
        this.prevSize = -1L;
        this.lastIndex = 0;
        this.hasUpdated = false;
        for (int i = 0; i < 20; i++) {
            this.sizeHistory[i] = -1;
        }
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.pkgName);
        dest.writeLong(this.prevSize);
        dest.writeInt(this.lastIndex);
        dest.writeLongArray(this.sizeHistory);
    }

    public void readFromParcel(Parcel in) {
        this.pkgName = in.readString();
        this.prevSize = in.readLong();
        this.lastIndex = in.readInt();
        in.readLongArray(this.sizeHistory);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{package:");
        sb.append(this.pkgName);
        sb.append(" history_size:");
        for (int i = 0; i < 20; i++) {
            long j = this.sizeHistory[i];
            if (j > 0) {
                sb.append(j);
                sb.append(",");
            }
        }
        sb.append("}");
        return sb.toString();
    }

    public void addHistoryItem(long size) {
        if (size > 0) {
            long[] jArr = this.sizeHistory;
            int i = this.lastIndex;
            if (size == jArr[i]) {
                return;
            }
            int idx = (i + 1) % 20;
            this.prevSize = size;
            jArr[idx] = size;
            this.lastIndex = idx;
            this.hasUpdated = true;
        }
    }

    public long getPredSize(int policy) {
        if (policy < 0 || policy > 2) {
            return -1L;
        }
        if (policy == 0) {
            return this.prevSize;
        }
        long max = 0;
        long sum = 0;
        int cnt = 0;
        for (int i = 0; i < 20; i++) {
            long[] jArr = this.sizeHistory;
            long j = jArr[i];
            if (j > 0) {
                cnt++;
                sum += j;
                if (j > max) {
                    max = jArr[i];
                }
            }
        }
        if (policy == 2) {
            return max;
        }
        return sum / cnt;
    }

    public static byte[] marshall(GameMemInfo parceable) {
        Parcel parcel = Parcel.obtain();
        parcel.setDataPosition(0);
        parceable.writeToParcel(parcel, 0);
        byte[] bytes = parcel.marshall();
        parcel.recycle();
        return bytes;
    }

    public static GameMemInfo unmarshall(byte[] bytes) {
        if (bytes.length == 0) {
            return null;
        }
        GameMemInfo meminfo = new GameMemInfo();
        Parcel parcel = Parcel.obtain();
        parcel.unmarshall(bytes, 0, bytes.length);
        parcel.setDataPosition(0);
        meminfo.readFromParcel(parcel);
        parcel.recycle();
        return meminfo;
    }
}
