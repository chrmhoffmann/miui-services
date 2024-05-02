package com.android.server.input.config;

import android.os.Parcel;
/* loaded from: classes.dex */
public class InputDebugConfig extends BaseInputConfig {
    public static final int CONFIG_TYPE = 1;
    private static volatile InputDebugConfig instance;
    private int mInputReaderAll;
    private int mInputTransportAll;
    private int mInputdispatcherAll;
    private int mInputdispatcherDetail;
    private int mInputdispatcherMajor;
    private int DEBUG_INPUT_EVENT_MAJAR = 1;
    private int DEBUG_INPUT_EVENT_DETAIL = 2;
    private int DEBUG_INPUT_DISPATCHER_ALL = 8;
    private int DEBUG_INPUT_READER_ALL = 16;
    private int DEBUG_INPUT_TRANSPORT_ALL = 32;

    private InputDebugConfig() {
    }

    public static InputDebugConfig getInstance() {
        if (instance == null) {
            synchronized (InputDebugConfig.class) {
                if (instance == null) {
                    instance = new InputDebugConfig();
                }
            }
        }
        return instance;
    }

    public void setInputDebugFromDump(int debugLog) {
        this.mInputdispatcherMajor = this.DEBUG_INPUT_EVENT_MAJAR & debugLog;
        this.mInputdispatcherDetail = this.DEBUG_INPUT_EVENT_DETAIL & debugLog;
        this.mInputdispatcherAll = this.DEBUG_INPUT_DISPATCHER_ALL & debugLog;
        this.mInputReaderAll = this.DEBUG_INPUT_READER_ALL & debugLog;
        this.mInputTransportAll = this.DEBUG_INPUT_TRANSPORT_ALL & debugLog;
    }

    @Override // com.android.server.input.config.BaseInputConfig
    protected void writeToParcel(Parcel dest) {
        dest.writeInt(this.mInputdispatcherMajor);
        dest.writeInt(this.mInputdispatcherDetail);
        dest.writeInt(this.mInputdispatcherAll);
        dest.writeInt(this.mInputReaderAll);
        dest.writeInt(this.mInputTransportAll);
    }

    @Override // com.android.server.input.config.BaseInputConfig
    public int getConfigType() {
        return 1;
    }

    public void setInputDispatcherMajor(int cmdSetting, int inputDispatcherMajor) {
        int cmdMajor = this.DEBUG_INPUT_EVENT_MAJAR & cmdSetting;
        if (cmdMajor == 0) {
            this.mInputdispatcherMajor = inputDispatcherMajor;
        } else {
            this.mInputdispatcherMajor = cmdMajor;
        }
    }
}
