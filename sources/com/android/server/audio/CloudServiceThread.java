package com.android.server.audio;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.provider.MiuiSettings;
import android.provider.Settings;
import android.util.Log;
import com.android.server.audio.CloudServiceSettings;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class CloudServiceThread extends Thread {
    private CloudServiceSettings mCloudServiceSettings;
    private Context mContext;
    private final String TAG = "CloudServiceThread";
    private boolean mStop = false;
    private String mCurrentVersion = "";
    private Object mLock = new Object();
    private Uri mDebugTriggerUri = Settings.Global.getUriFor("test_audio_cloud");
    private ContentObserver mCloudObserver = new ContentObserver(null) { // from class: com.android.server.audio.CloudServiceThread.1
        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange);
            Log.d("CloudServiceThread", "cloud data changed");
            synchronized (CloudServiceThread.this.mLock) {
                Log.d("CloudServiceThread", "enter before notify");
                CloudServiceThread.this.mLock.notifyAll();
                Log.d("CloudServiceThread", "enter after notify");
            }
        }
    };

    public CloudServiceThread(Context ctx) {
        this.mContext = ctx;
        loadSettings();
        this.mContext.getContentResolver().registerContentObserver(MiuiSettings.SettingsCloudData.getCloudDataNotifyUri(), true, this.mCloudObserver);
        this.mContext.getContentResolver().registerContentObserver(this.mDebugTriggerUri, true, this.mCloudObserver);
    }

    @Override // java.lang.Thread, java.lang.Runnable
    public void run() {
        while (!this.mStop) {
            Log.d("CloudServiceThread", "enter run");
            if ("".equals(this.mCloudServiceSettings.mVersionCode) || this.mCloudServiceSettings.mVersionCode.compareTo(this.mCurrentVersion) > 0) {
                this.mCurrentVersion = this.mCloudServiceSettings.mVersionCode;
                Log.d("CloudServiceThread", "update current version " + this.mCurrentVersion);
                triggerSettings();
            }
            synchronized (this.mLock) {
                Log.d("CloudServiceThread", "enter wait");
                try {
                    this.mLock.wait();
                } catch (Exception e) {
                    Log.d("CloudServiceThread", "enter wait exception");
                }
            }
            this.mCloudServiceSettings.fetchDataAll();
        }
    }

    private void loadSettings() {
        this.mCloudServiceSettings = new CloudServiceSettings(this.mContext);
    }

    private void triggerSettings() {
        for (CloudServiceSettings.Setting s : this.mCloudServiceSettings.mSettings.values()) {
            if (s.set()) {
                Log.d("CloudServiceThread", s.mSettingName + " set successfully!");
            } else {
                Log.d("CloudServiceThread", s.mSettingName + " didn't set !");
            }
        }
    }
}
