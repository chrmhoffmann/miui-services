package com.android.server.display.statistics;

import android.os.Parcel;
import android.os.Parcelable;
/* loaded from: classes.dex */
public class AdvancedEvent implements Parcelable {
    public static final int BRIGHTNESS_CHANGE_STATE_DECREASE = 1;
    public static final int BRIGHTNESS_CHANGE_STATE_EQUAL = 2;
    public static final int BRIGHTNESS_CHANGE_STATE_INCREASE = 0;
    public static final int BRIGHTNESS_CHANGE_STATE_RESET = 3;
    public static final Parcelable.Creator<AdvancedEvent> CREATOR = new Parcelable.Creator<AdvancedEvent>() { // from class: com.android.server.display.statistics.AdvancedEvent.1
        @Override // android.os.Parcelable.Creator
        public AdvancedEvent createFromParcel(Parcel in) {
            return new AdvancedEvent(in);
        }

        @Override // android.os.Parcelable.Creator
        public AdvancedEvent[] newArray(int size) {
            return new AdvancedEvent[size];
        }
    };
    public static final int EVENT_AUTO_BRIGHTNESS_ANIMATION_INFO = 1;
    public static final int EVENT_SCHEDULE_ADVANCED_EVENT = 2;
    private float auto_brightness_animation_duration;
    private int brightness_changed_state;
    private int current_animate_value;
    private float default_spline_error;
    private String extra;
    private int interrupt_brightness_animation_times;
    private float long_term_model_spline_error;
    private int target_animate_value;
    private int type;
    private int user_brightness;
    private int user_reset_brightness_mode_times;

    public AdvancedEvent() {
        this.current_animate_value = -1;
        this.target_animate_value = -1;
        this.user_brightness = -1;
        this.auto_brightness_animation_duration = -1.0f;
        this.long_term_model_spline_error = -1.0f;
        this.default_spline_error = -1.0f;
        this.brightness_changed_state = -1;
        this.extra = "";
    }

    private AdvancedEvent(Parcel in) {
        this.current_animate_value = -1;
        this.target_animate_value = -1;
        this.user_brightness = -1;
        this.auto_brightness_animation_duration = -1.0f;
        this.long_term_model_spline_error = -1.0f;
        this.default_spline_error = -1.0f;
        this.brightness_changed_state = -1;
        this.extra = "";
        this.type = in.readInt();
        this.interrupt_brightness_animation_times = in.readInt();
        this.user_reset_brightness_mode_times = in.readInt();
        this.current_animate_value = in.readInt();
        this.target_animate_value = in.readInt();
        this.user_brightness = in.readInt();
        this.auto_brightness_animation_duration = in.readFloat();
        this.long_term_model_spline_error = in.readFloat();
        this.default_spline_error = in.readFloat();
        this.brightness_changed_state = in.readInt();
        this.extra = in.readString();
    }

    public AdvancedEvent setEventType(int type) {
        this.type = type;
        return this;
    }

    public int getEventType() {
        return this.type;
    }

    public AdvancedEvent setInterruptBrightnessAnimationTimes(int times) {
        this.interrupt_brightness_animation_times = times;
        return this;
    }

    public int getInterruptBrightnessAnimationTimes() {
        return this.interrupt_brightness_animation_times;
    }

    public AdvancedEvent setUserResetBrightnessModeTimes(int times) {
        this.user_reset_brightness_mode_times = times;
        return this;
    }

    public int getUserResetBrightnessModeTimes() {
        return this.user_reset_brightness_mode_times;
    }

    public AdvancedEvent setCurrentAnimateValue(int value) {
        this.current_animate_value = value;
        return this;
    }

    public int getCurrentAnimateValue() {
        return this.current_animate_value;
    }

    public AdvancedEvent setTargetAnimateValue(int value) {
        this.target_animate_value = value;
        return this;
    }

    public int getTargetAnimateValue() {
        return this.target_animate_value;
    }

    public AdvancedEvent setUserBrightness(int value) {
        this.user_brightness = value;
        return this;
    }

    public int getUserBrightness() {
        return this.user_brightness;
    }

    public AdvancedEvent setAutoBrightnessAnimationDuration(float duration) {
        this.auto_brightness_animation_duration = duration;
        return this;
    }

    public float getAutoBrightnessAnimationDuration() {
        return this.auto_brightness_animation_duration;
    }

    public AdvancedEvent setLongTermModelSplineError(float error) {
        this.long_term_model_spline_error = error;
        return this;
    }

    public float getLongTermModelSplineError() {
        return this.long_term_model_spline_error;
    }

    public AdvancedEvent setDefaultSplineError(float error) {
        this.default_spline_error = error;
        return this;
    }

    public float getDefaultSplineError() {
        return this.default_spline_error;
    }

    public AdvancedEvent setBrightnessChangedState(int state) {
        this.brightness_changed_state = state;
        return this;
    }

    public int getBrightnessChangedState() {
        return this.brightness_changed_state;
    }

    public AdvancedEvent setExtra(String extra) {
        this.extra = extra;
        return this;
    }

    public String getExtra() {
        return this.extra;
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.type);
        dest.writeInt(this.interrupt_brightness_animation_times);
        dest.writeInt(this.user_reset_brightness_mode_times);
        dest.writeInt(this.current_animate_value);
        dest.writeInt(this.target_animate_value);
        dest.writeInt(this.user_brightness);
        dest.writeFloat(this.auto_brightness_animation_duration);
        dest.writeFloat(this.long_term_model_spline_error);
        dest.writeFloat(this.default_spline_error);
        dest.writeInt(this.brightness_changed_state);
        dest.writeString(this.extra);
    }

    public String convertToString() {
        return "type:" + this.type + ", interrupt_brightness_animation_times:" + this.interrupt_brightness_animation_times + ", user_reset_brightness_mode_times:" + this.user_reset_brightness_mode_times + ", auto_brightness_animation_duration:" + this.auto_brightness_animation_duration + ", current_animate_value:" + this.current_animate_value + ", target_animate_value:" + this.target_animate_value + ", user_brightness:" + this.user_brightness + ", long_term_model_spline_error:" + this.long_term_model_spline_error + ", default_spline_error:" + this.default_spline_error + ", brightness_changed_state:" + getBrightnessChangedState(this.brightness_changed_state) + ", extra:" + this.extra;
    }

    private String getBrightnessChangedState(int state) {
        switch (state) {
            case 0:
                return "brightness increase";
            case 1:
                return "brightness decrease";
            case 2:
                return "brightness equal";
            default:
                return "brightness reset";
        }
    }
}
