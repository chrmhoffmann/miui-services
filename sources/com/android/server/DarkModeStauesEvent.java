package com.android.server;

import java.util.ArrayList;
import java.util.List;
/* loaded from: classes.dex */
public class DarkModeStauesEvent implements DarkModeEvent {
    private String mAppEnable;
    private List<String> mAppList = new ArrayList();
    private String mAppName;
    private String mAppPkg;
    private String mAutoSwitch;
    private String mBeginTime;
    private String mContrastStatus;
    private String mDarkModeStatus;
    private String mEndTime;
    private String mEventName;
    private String mSettingChannel;
    private String mStatusAfterClick;
    private int mSuggest;
    private int mSuggestClick;
    private int mSuggestEnable;
    private int mSuggestOpenInSetting;
    private String mTimeModePattern;
    private String mTimeModeStatus;
    private String mTip;
    private String mWallPaperStatus;

    @Override // com.android.server.DarkModeEvent
    public String getEventName() {
        return this.mEventName;
    }

    public DarkModeStauesEvent setEventName(String eventName) {
        this.mEventName = eventName;
        return this;
    }

    public String getDarkModeStatus() {
        return this.mDarkModeStatus;
    }

    public DarkModeStauesEvent setDarkModeStatus(String darkModeStatus) {
        this.mDarkModeStatus = darkModeStatus;
        return this;
    }

    public String getTimeModeStatus() {
        return this.mTimeModeStatus;
    }

    public DarkModeStauesEvent setTimeModeStatus(String timeModeStatus) {
        this.mTimeModeStatus = timeModeStatus;
        return this;
    }

    public String getTimeModePattern() {
        return this.mTimeModePattern;
    }

    public DarkModeStauesEvent setTimeModePattern(String timeModePattern) {
        this.mTimeModePattern = timeModePattern;
        return this;
    }

    public String getBeginTime() {
        return this.mBeginTime;
    }

    public DarkModeStauesEvent setBeginTime(String beginTime) {
        this.mBeginTime = beginTime;
        return this;
    }

    public String getEndTime() {
        return this.mEndTime;
    }

    public DarkModeStauesEvent setEndTime(String endTime) {
        this.mEndTime = endTime;
        return this;
    }

    public String getWallPaperStatus() {
        return this.mWallPaperStatus;
    }

    public DarkModeStauesEvent setWallPaperStatus(String wallPaperStatus) {
        this.mWallPaperStatus = wallPaperStatus;
        return this;
    }

    public String getContrastStatus() {
        return this.mContrastStatus;
    }

    public DarkModeStauesEvent setContrastStatus(String montrastStatus) {
        this.mContrastStatus = montrastStatus;
        return this;
    }

    public List<String> getAppList() {
        return this.mAppList;
    }

    public DarkModeStauesEvent setAppList(List<String> appList) {
        this.mAppList = appList;
        return this;
    }

    public String getAppPkg() {
        return this.mAppPkg;
    }

    public DarkModeStauesEvent setAppPkg(String appPkg) {
        this.mAppPkg = appPkg;
        return this;
    }

    public String getAppName() {
        return this.mAppName;
    }

    public DarkModeStauesEvent setAppName(String appName) {
        this.mAppName = appName;
        return this;
    }

    public String getAppEnable() {
        return this.mAppEnable;
    }

    public DarkModeStauesEvent setAppEnable(String enable) {
        this.mAppEnable = enable;
        return this;
    }

    public String getAutoSwitch() {
        return this.mAutoSwitch;
    }

    public DarkModeStauesEvent setAutoSwitch(String autoSwitch) {
        this.mAutoSwitch = autoSwitch;
        return this;
    }

    public String getStatusAfterClick() {
        return this.mStatusAfterClick;
    }

    public DarkModeStauesEvent setStatusAfterClick(String statusAfterClick) {
        this.mStatusAfterClick = statusAfterClick;
        return this;
    }

    public String getSettingChannel() {
        return this.mSettingChannel;
    }

    public DarkModeStauesEvent setSettingChannel(String settingChannel) {
        this.mSettingChannel = settingChannel;
        return this;
    }

    @Override // com.android.server.DarkModeEvent
    public String getTip() {
        return this.mTip;
    }

    public DarkModeStauesEvent setTip(String tip) {
        this.mTip = tip;
        return this;
    }

    public int getSuggest() {
        return this.mSuggest;
    }

    public DarkModeStauesEvent setSuggest(int suggest) {
        this.mSuggest = suggest;
        return this;
    }

    public int getSuggestClick() {
        return this.mSuggestClick;
    }

    public DarkModeStauesEvent setSuggestClick(int suggestClick) {
        this.mSuggestClick = suggestClick;
        return this;
    }

    public int getSuggestEnable() {
        return this.mSuggestEnable;
    }

    public DarkModeStauesEvent setSuggestEnable(int suggestEnable) {
        this.mSuggestEnable = suggestEnable;
        return this;
    }

    public int getSuggestOpenInSetting() {
        return this.mSuggestOpenInSetting;
    }

    public DarkModeStauesEvent setSuggestOpenInSetting(int suggestOpenInSetting) {
        this.mSuggestOpenInSetting = suggestOpenInSetting;
        return this;
    }

    @Override // com.android.server.DarkModeEvent
    public DarkModeEvent clone() {
        DarkModeEvent event = new DarkModeStauesEvent().setEventName(getEventName()).setTip(getTip()).setAppEnable(getAppEnable()).setAppList(new ArrayList(getAppList())).setAppName(getAppName()).setAppPkg(getAppPkg()).setAutoSwitch(getAutoSwitch()).setBeginTime(getBeginTime()).setContrastStatus(getContrastStatus()).setDarkModeStatus(getDarkModeStatus()).setEndTime(getEndTime()).setSuggest(getSuggest()).setSuggestClick(getSuggestClick()).setSettingChannel(getSettingChannel()).setStatusAfterClick(getStatusAfterClick()).setSuggestEnable(getSuggestEnable()).setSuggestOpenInSetting(getSuggestOpenInSetting()).setTimeModePattern(getTimeModePattern()).setTimeModeStatus(getTimeModeStatus()).setWallPaperStatus(getWallPaperStatus());
        return event;
    }

    public String toString() {
        return "DarkModeStauesEvent{mEventName='" + this.mEventName + "', mTip='" + this.mTip + "', mDarkModeStatus='" + this.mDarkModeStatus + "', mTimeModeStatus='" + this.mTimeModeStatus + "', mTimeModePattern='" + this.mTimeModePattern + "', mBeginTime='" + this.mBeginTime + "', mEndTime='" + this.mEndTime + "', mWallPaperStatus='" + this.mWallPaperStatus + "', mContrastStatus='" + this.mContrastStatus + "', mAppList=" + this.mAppList + ", mAppPkg='" + this.mAppPkg + "', mAppName='" + this.mAppName + "', mAppEnable='" + this.mAppEnable + "', mAutoSwitch='" + this.mAutoSwitch + "', mStatusAfterClick='" + this.mStatusAfterClick + "', mSettingChannel='" + this.mSettingChannel + "', mSuggest=" + this.mSuggest + ", mSuggestClick=" + this.mSuggestClick + ", mSuggestEnable=" + this.mSuggestEnable + ", mSuggestOpenInSetting=" + this.mSuggestOpenInSetting + '}';
    }
}
