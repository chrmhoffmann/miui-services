package com.android.server.location.gnss.datacollect;

import java.util.Objects;
/* loaded from: classes.dex */
public class GnssPowerBean {
    String start_power;
    String start_time;
    String stop_power;
    String stop_time;

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        GnssPowerBean bean = (GnssPowerBean) o;
        if (Objects.equals(this.start_time, bean.start_time) && Objects.equals(this.stop_time, bean.stop_time) && Objects.equals(this.start_power, bean.start_power) && Objects.equals(this.stop_power, bean.stop_power)) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return Objects.hash(this.start_time, this.stop_time, this.start_power, this.stop_power);
    }
}
