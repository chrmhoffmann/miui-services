package com.android.server.location.gnss.datacollect;

import java.util.Objects;
/* loaded from: classes.dex */
public class UseGnssAppBean {
    public long changeToBackTime;
    public long changeToforeTime;
    public long glpDuring;
    public boolean isAppForeground;
    public boolean isRunning;
    public String packageName;
    public String provider;
    public long removeTime;
    public String reportInterval;
    public long requestTime;

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        UseGnssAppBean that = (UseGnssAppBean) o;
        if (this.requestTime == that.requestTime && this.changeToforeTime == that.changeToforeTime && this.changeToBackTime == that.changeToBackTime && this.removeTime == that.removeTime && this.glpDuring == that.glpDuring && this.isAppForeground == that.isAppForeground && this.isRunning == that.isRunning && Objects.equals(this.packageName, that.packageName) && Objects.equals(this.reportInterval, that.reportInterval) && Objects.equals(this.provider, that.provider)) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return Objects.hash(this.packageName, this.reportInterval, Long.valueOf(this.requestTime), Long.valueOf(this.changeToforeTime), Long.valueOf(this.changeToBackTime), Long.valueOf(this.removeTime), Long.valueOf(this.glpDuring), Boolean.valueOf(this.isAppForeground), Boolean.valueOf(this.isRunning), this.provider);
    }
}
