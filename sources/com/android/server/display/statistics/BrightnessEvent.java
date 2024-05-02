package com.android.server.display.statistics;

import android.os.Parcel;
import android.os.Parcelable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/* loaded from: classes.dex */
public class BrightnessEvent implements Parcelable {
    public static final Parcelable.Creator<BrightnessEvent> CREATOR = new Parcelable.Creator<BrightnessEvent>() { // from class: com.android.server.display.statistics.BrightnessEvent.1
        @Override // android.os.Parcelable.Creator
        public BrightnessEvent createFromParcel(Parcel source) {
            return new BrightnessEvent(source);
        }

        @Override // android.os.Parcelable.Creator
        public BrightnessEvent[] newArray(int size) {
            return new BrightnessEvent[size];
        }
    };
    public static final int EVENT_AUTO_CHANGED_BRIGHTNESS = 1;
    public static final int EVENT_AUTO_MANUAL_CHANGED_BRIGHTNESS = 2;
    public static final int EVENT_BRIGHTNESS_RELEVANT_SWITCH = 5;
    public static final int EVENT_BRIGHTNESS_USAGE_DURATION = 6;
    public static final int EVENT_EXTRA_DATA = 7;
    public static final int EVENT_MANUAL_CHANGED_BRIGHTNESS = 0;
    public static final int EVENT_SUNLIGHT_CHANGED_BRIGHTNESS = 4;
    public static final int EVENT_WINDOW_CHANGED_BRIGHTNESS = 3;
    private int affect_factor_flag;
    private List<SwitchStatEntry> all_stats_entries;
    private float ambient_lux;
    private int ambient_lux_span;
    private Map<Integer, Long> brightness_usage_map;
    private int current_user_id;
    private float display_gray_scale;
    private String extra;
    private boolean is_default_config;
    private boolean low_power_mode_flag;
    private int orientation;
    private int previous_brightness;
    private int previous_brightness_span;
    private int screen_brightness;
    private int screen_brightness_span;
    private String spline;
    private long time_stamp;
    private String top_package;
    private int type;
    private float user_data_point;

    public BrightnessEvent() {
        this.ambient_lux = -1.0f;
        this.spline = "";
        this.top_package = "";
        this.extra = "";
        this.is_default_config = true;
        this.user_data_point = -1.0f;
        this.all_stats_entries = new ArrayList();
        this.affect_factor_flag = -1;
        this.screen_brightness_span = -1;
        this.previous_brightness_span = -1;
        this.ambient_lux_span = -1;
        this.brightness_usage_map = new HashMap();
        this.display_gray_scale = -1.0f;
    }

    private BrightnessEvent(Parcel source) {
        this.ambient_lux = -1.0f;
        this.spline = "";
        this.top_package = "";
        this.extra = "";
        this.is_default_config = true;
        this.user_data_point = -1.0f;
        this.all_stats_entries = new ArrayList();
        this.affect_factor_flag = -1;
        this.screen_brightness_span = -1;
        this.previous_brightness_span = -1;
        this.ambient_lux_span = -1;
        this.brightness_usage_map = new HashMap();
        this.display_gray_scale = -1.0f;
        this.type = source.readInt();
        this.ambient_lux = source.readFloat();
        this.screen_brightness = source.readInt();
        this.orientation = source.readInt();
        this.spline = source.readString();
        this.top_package = source.readString();
        this.time_stamp = source.readLong();
        this.extra = source.readString();
        this.previous_brightness = source.readInt();
        this.is_default_config = source.readBoolean();
        this.user_data_point = source.readFloat();
        this.low_power_mode_flag = source.readBoolean();
        this.current_user_id = source.readInt();
        source.readTypedList(this.all_stats_entries, SwitchStatEntry.CREATOR);
        this.affect_factor_flag = source.readInt();
        this.screen_brightness_span = source.readInt();
        this.previous_brightness_span = source.readInt();
        this.ambient_lux_span = source.readInt();
        source.readMap(this.brightness_usage_map, HashMap.class.getClassLoader());
        this.display_gray_scale = source.readFloat();
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.type);
        dest.writeFloat(this.ambient_lux);
        dest.writeInt(this.screen_brightness);
        dest.writeInt(this.orientation);
        dest.writeString(this.spline);
        dest.writeString(this.top_package);
        dest.writeLong(this.time_stamp);
        dest.writeString(this.extra);
        dest.writeInt(this.previous_brightness);
        dest.writeBoolean(this.is_default_config);
        dest.writeFloat(this.user_data_point);
        dest.writeBoolean(this.low_power_mode_flag);
        dest.writeInt(this.current_user_id);
        dest.writeTypedList(this.all_stats_entries);
        dest.writeInt(this.affect_factor_flag);
        dest.writeInt(this.screen_brightness_span);
        dest.writeInt(this.previous_brightness_span);
        dest.writeInt(this.ambient_lux_span);
        dest.writeMap(this.brightness_usage_map);
        dest.writeFloat(this.display_gray_scale);
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    public BrightnessEvent setEventType(int type) {
        this.type = type;
        return this;
    }

    public int getEventType() {
        return this.type;
    }

    public BrightnessEvent setAmbientLux(float ambientLux) {
        this.ambient_lux = ambientLux;
        return this;
    }

    public float getAmbientLux() {
        return this.ambient_lux;
    }

    public BrightnessEvent setScreenBrightness(int screenBrightness) {
        this.screen_brightness = screenBrightness;
        return this;
    }

    public int getScreenBrightness() {
        return this.screen_brightness;
    }

    public BrightnessEvent setSpline(String spline) {
        this.spline = spline;
        return this;
    }

    public String getSpline() {
        return this.spline;
    }

    public BrightnessEvent setOrientation(int orientation) {
        this.orientation = orientation;
        return this;
    }

    public int getOrientation() {
        return this.orientation;
    }

    public BrightnessEvent setForegroundPackage(String top_package) {
        this.top_package = top_package;
        return this;
    }

    public String getForegroundPackage() {
        return this.top_package;
    }

    public BrightnessEvent setTimeStamp(long timeStamp) {
        this.time_stamp = timeStamp;
        return this;
    }

    public long getTimeStamp() {
        return this.time_stamp;
    }

    public BrightnessEvent setExtra(String string) {
        this.extra = string;
        return this;
    }

    public String getExtra() {
        return this.extra;
    }

    public static String timestamp2String(long time) {
        Date d = new Date(time);
        SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:sss");
        return sf.format(d);
    }

    public BrightnessEvent setPreviousBrightness(int previousBrightness) {
        this.previous_brightness = previousBrightness;
        return this;
    }

    public int getPreviousBrightness() {
        return this.previous_brightness;
    }

    public BrightnessEvent setUserDataPoint(float userDataPoint) {
        this.user_data_point = userDataPoint;
        return this;
    }

    public float getUserDataPoint() {
        return this.user_data_point;
    }

    public BrightnessEvent setIsDefaultConfig(boolean defaultConfig) {
        this.is_default_config = defaultConfig;
        return this;
    }

    public boolean isDefaultConfig() {
        return this.is_default_config;
    }

    public BrightnessEvent setLowPowerModeFlag(boolean enable) {
        this.low_power_mode_flag = enable;
        return this;
    }

    public boolean getLowPowerModeFlag() {
        return this.low_power_mode_flag;
    }

    public BrightnessEvent setUserId(int userId) {
        this.current_user_id = userId;
        return this;
    }

    public int getUserId() {
        return this.current_user_id;
    }

    public BrightnessEvent setSwitchStats(List<SwitchStatEntry> all_stats_events) {
        this.all_stats_entries = all_stats_events;
        return this;
    }

    public List<SwitchStatEntry> getSwitchStats() {
        return this.all_stats_entries;
    }

    public BrightnessEvent setAffectFactorFlag(int flag) {
        this.affect_factor_flag = flag;
        return this;
    }

    public int getAffectFactorFlag() {
        return this.affect_factor_flag;
    }

    public BrightnessEvent setCurBrightnessSpanIndex(int span) {
        this.screen_brightness_span = span;
        return this;
    }

    public int getCurBrightnessSpanIndex() {
        return this.screen_brightness_span;
    }

    public BrightnessEvent setPreBrightnessSpanIndex(int span) {
        this.previous_brightness_span = span;
        return this;
    }

    public int getPreBrightnessSpanIndex() {
        return this.previous_brightness_span;
    }

    public BrightnessEvent setLuxSpanIndex(int span) {
        this.ambient_lux_span = span;
        return this;
    }

    public int getLuxSpanIndex() {
        return this.ambient_lux_span;
    }

    public BrightnessEvent setBrightnessUsageMap(Map<Integer, Long> brightness_usage_map) {
        this.brightness_usage_map = brightness_usage_map;
        return this;
    }

    public Map<Integer, Long> getBrightnessUsageMap() {
        return this.brightness_usage_map;
    }

    public BrightnessEvent setDisplayGrayScale(float gray) {
        this.display_gray_scale = gray;
        return this;
    }

    public float getDisplayGrayScale() {
        return this.display_gray_scale;
    }

    public String toSimpleString() {
        return "{" + this.type + "," + this.ambient_lux + "," + this.screen_brightness + "," + this.orientation + "," + this.top_package + "," + this.extra + ", " + this.previous_brightness + "," + this.is_default_config + "," + this.user_data_point + "," + this.low_power_mode_flag + "," + this.current_user_id + "}";
    }

    public String toString() {
        return "{type:" + this.type + ",orientation:" + this.orientation + ",top_package:" + this.top_package + ",screen_brightness:" + this.screen_brightness + ",previous_brightness:" + this.previous_brightness + ",ambient_lux:" + this.ambient_lux + ",user_data_point:" + this.user_data_point + ",is_default_config:" + this.is_default_config + ",screen_brightness_span:" + this.screen_brightness_span + ",previous_brightness_span:" + this.previous_brightness_span + ",ambient_lux_span:" + this.ambient_lux_span + ",spline:" + this.spline + ",all_stats_entries:" + this.all_stats_entries + ",affect_factor_flag:" + this.affect_factor_flag + ",brightness_usage_map:" + this.brightness_usage_map + ",display_gray_scale:" + this.display_gray_scale + ",low_power_mode_flag:" + this.low_power_mode_flag + ",current_user_id:" + this.current_user_id + ",extra:" + this.extra + ",time_stamp:" + timestamp2String(this.time_stamp) + "}";
    }

    /* loaded from: classes.dex */
    public static class SwitchStatEntry implements Parcelable {
        public static final Parcelable.Creator<SwitchStatEntry> CREATOR = new Parcelable.Creator<SwitchStatEntry>() { // from class: com.android.server.display.statistics.BrightnessEvent.SwitchStatEntry.1
            @Override // android.os.Parcelable.Creator
            public SwitchStatEntry createFromParcel(Parcel in) {
                return new SwitchStatEntry(in);
            }

            @Override // android.os.Parcelable.Creator
            public SwitchStatEntry[] newArray(int size) {
                return new SwitchStatEntry[size];
            }
        };
        public static final int TYPE_BOOLEAN = 0;
        public static final int TYPE_INTEGER = 1;
        public boolean b_value;
        public int i_value;
        public String key;
        public int type;

        public SwitchStatEntry(int type, String key, int i_value) {
            if (type == 0) {
                throw new IllegalArgumentException("Type and value are incompatible,the expected type is TYPE_INTEGER.");
            }
            this.type = type;
            this.key = key;
            this.i_value = i_value;
        }

        public SwitchStatEntry(int type, String key, boolean b_value) {
            if (type == 1) {
                throw new IllegalArgumentException("type and value are incompatible,the expected type is TYPE_BOOLEAN.");
            }
            this.type = type;
            this.key = key;
            this.b_value = b_value;
            this.i_value = -1;
        }

        public String toString() {
            return "{type:" + typeToString(this.type) + ", key:" + this.key + ", value:" + (this.type == 0 ? Boolean.valueOf(this.b_value) : Integer.valueOf(this.i_value)) + "}";
        }

        public String typeToString(int type) {
            return type == 0 ? "TYPE_BOOLEAN" : "TYPE_INTEGER";
        }

        protected SwitchStatEntry(Parcel in) {
            this.type = in.readInt();
            this.key = in.readString();
            this.b_value = in.readBoolean();
            this.i_value = in.readInt();
        }

        @Override // android.os.Parcelable
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(this.type);
            dest.writeString(this.key);
            dest.writeBoolean(this.b_value);
            dest.writeInt(this.i_value);
        }

        @Override // android.os.Parcelable
        public int describeContents() {
            return 0;
        }
    }
}
