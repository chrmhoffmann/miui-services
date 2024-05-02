package com.android.server.audio;

import android.content.AsyncQueryHandler;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabaseCorruptException;
import android.database.sqlite.SQLiteDiskIOException;
import android.database.sqlite.SQLiteFullException;
import android.icu.util.Calendar;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.server.am.SplitScreenReporter;
import android.text.TextUtils;
import android.util.Log;
/* loaded from: classes.dex */
public class AudioQueryWeatherService {
    private static final String CITY_CONTENT_URI = "content://weather/selected_city";
    private static final int CITY_QUERY_TOKEN = 1001;
    private static final boolean DEBUG = true;
    private static final int FLAG_LOCATION_TRUE = 1;
    private static final int MAX_SUNRISE_TIME = 10;
    private static final int MAX_SUNSET_TIME = 22;
    private static final int MIN_SUNRISE_TIME = 3;
    private static final int MIN_SUNSET_TIME = 15;
    private static final int NO_FIND_OUT_RESULT = -1;
    private static final String TAG = "AudioQueryWeatherService";
    private static final String WEATHER_CONTENT_URI = "content://weather/actualWeatherData";
    private static final int WEATHER_QUERY_TOKEN = 1000;
    private BroadcastReceiver mBootCompleteReceiver;
    private final ContentResolver mContentResolver;
    private Context mContext;
    private boolean mInternationalLocation;
    private LocationObserver mLocationObserver;
    private boolean mNextSunriseSunsetTime = false;
    private QueryHandler mQueryHandler;
    private int mSunriseTimeHours;
    private int mSunriseTimeMins;
    private int mSunsetTimeHours;
    private int mSunsetTimeMins;
    private BroadcastReceiver mUpdateTimeReceiver;
    private static final String[] WEATHER_PROJECTION = {"city_id", "sunrise", "sunset"};
    private static final String[] CITY_PROJECTION = {"posID", "position", "flag", "belongings"};

    public AudioQueryWeatherService(Context context) {
        Log.d(TAG, "construct!!!");
        this.mContext = context;
        this.mContentResolver = context.getContentResolver();
        if (this.mQueryHandler == null) {
            this.mQueryHandler = new QueryHandler(this.mContext);
        }
        this.mSunriseTimeHours = 0;
        this.mSunsetTimeHours = 0;
        this.mSunriseTimeMins = 0;
        this.mSunsetTimeMins = 0;
        this.mInternationalLocation = true;
    }

    public void onCreate() {
        Log.d(TAG, "onCreate!!!");
        if (this.mBootCompleteReceiver == null) {
            this.mBootCompleteReceiver = new BroadcastReceiver() { // from class: com.android.server.audio.AudioQueryWeatherService.1
                @Override // android.content.BroadcastReceiver
                public void onReceive(Context context, Intent intent) {
                    Log.d(AudioQueryWeatherService.TAG, "receive the boot complete : " + intent);
                    AudioQueryWeatherService.this.mNextSunriseSunsetTime = false;
                    AudioQueryWeatherService.this.startCityQuery();
                }
            };
            IntentFilter intentFilterBootComplete = new IntentFilter("android.intent.action.BOOT_COMPLETED");
            intentFilterBootComplete.setPriority(1000);
            this.mContext.registerReceiver(this.mBootCompleteReceiver, intentFilterBootComplete);
        }
        if (this.mUpdateTimeReceiver == null) {
            this.mUpdateTimeReceiver = new BroadcastReceiver() { // from class: com.android.server.audio.AudioQueryWeatherService.2
                @Override // android.content.BroadcastReceiver
                public void onReceive(Context context, Intent intent) {
                    Log.d(AudioQueryWeatherService.TAG, "receive sunrise.sunset.time broadcast : " + intent);
                    AudioQueryWeatherService.this.mNextSunriseSunsetTime = true;
                    AudioQueryWeatherService.this.startCityQuery();
                }
            };
            IntentFilter intentFilterUpdateTime = new IntentFilter("com.android.media.update.sunrise.sunset.time");
            intentFilterUpdateTime.setPriority(1000);
            this.mContext.registerReceiver(this.mUpdateTimeReceiver, intentFilterUpdateTime);
        }
        LocationObserver locationObserver = new LocationObserver();
        this.mLocationObserver = locationObserver;
        locationObserver.onCreate();
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class LocationObserver extends ContentObserver {
        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        LocationObserver() {
            super(new Handler());
            AudioQueryWeatherService.this = r3;
            r3.mContentResolver.registerContentObserver(Uri.parse("content://weather/selected_city"), false, this);
        }

        public void onCreate() {
            Log.d(AudioQueryWeatherService.TAG, "LocationObserver:onCreate!");
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange) {
            Log.d(AudioQueryWeatherService.TAG, "location change:" + selfChange);
            AudioQueryWeatherService.this.mNextSunriseSunsetTime = false;
            AudioQueryWeatherService.this.startCityQuery();
        }
    }

    public void startCityQuery() {
        synchronized (AudioQueryWeatherService.class) {
            QueryHandler queryHandler = this.mQueryHandler;
            if (queryHandler != null) {
                queryHandler.cancelOperation(CITY_QUERY_TOKEN);
                this.mQueryHandler.cancelOperation(1000);
                Uri contentUri = Uri.parse("content://weather/selected_city").buildUpon().build();
                this.mQueryHandler.startQuery(CITY_QUERY_TOKEN, null, contentUri, CITY_PROJECTION, null, null, null);
            }
        }
    }

    private void CalculateLocationAndQuery(int flag, String cityId, String belongings) {
        String LocationJudgment = this.mContext.getResources().getString(286196123);
        if (flag == 1) {
            if (belongings != null && belongings.indexOf(LocationJudgment) != -1) {
                this.mInternationalLocation = false;
            } else {
                this.mInternationalLocation = true;
            }
            if (!this.mInternationalLocation && !TextUtils.isEmpty(cityId)) {
                startWeatherQuery(cityId);
                return;
            }
            return;
        }
        this.mInternationalLocation = true;
    }

    public void updateCityInfo(Cursor cursor) {
        String cityId = null;
        String belongings = null;
        int flag = -1;
        try {
            try {
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (cursor == null) {
                Log.d(TAG, "location: weather maybe uninstalled");
                this.mInternationalLocation = true;
                return;
            }
            try {
                if (cursor.getCount() != 0 && cursor.moveToFirst()) {
                    cityId = cursor.getString(cursor.getColumnIndex("posID"));
                    belongings = cursor.getString(cursor.getColumnIndex("belongings"));
                    flag = cursor.getInt(cursor.getColumnIndex("flag"));
                    Log.d(TAG, "updateCityInfo flag:" + flag);
                }
                cursor.close();
            } catch (Exception e2) {
                e2.printStackTrace();
                cursor.close();
            }
            CalculateLocationAndQuery(flag, cityId, belongings);
        } catch (Throwable th) {
            try {
                cursor.close();
            } catch (Exception e3) {
                e3.printStackTrace();
            }
            throw th;
        }
    }

    private void startWeatherQuery(String cityId) {
        Log.d(TAG, "startWeatherQuery start!");
        synchronized (AudioQueryWeatherService.class) {
            QueryHandler queryHandler = this.mQueryHandler;
            if (queryHandler != null) {
                queryHandler.cancelOperation(1000);
                Uri contentUri = Uri.parse(WEATHER_CONTENT_URI).buildUpon().appendPath(SplitScreenReporter.ACTION_EXIT_SPLIT).appendPath("0").build();
                String[] selectionArgs = {cityId};
                this.mQueryHandler.startQuery(1000, null, contentUri, WEATHER_PROJECTION, null, selectionArgs, null);
            }
        }
    }

    private void CalculateSunriseAndSunsetTime(long sunriseTime, long sunsetTime) {
        Calendar SunCalendar = Calendar.getInstance();
        if (sunriseTime != 0 && sunsetTime != 0) {
            SunCalendar.setTimeInMillis(sunriseTime);
            this.mSunriseTimeHours = SunCalendar.get(11);
            this.mSunriseTimeMins = SunCalendar.get(12);
            if (this.mSunriseTimeHours < 3) {
                this.mSunriseTimeHours = 3;
                this.mSunriseTimeMins = 0;
            }
            if (this.mSunriseTimeHours > 10) {
                this.mSunriseTimeHours = 11;
                this.mSunriseTimeMins = 0;
            }
            SunCalendar.setTimeInMillis(sunsetTime);
            this.mSunsetTimeHours = SunCalendar.get(11);
            this.mSunsetTimeMins = SunCalendar.get(12);
            if (this.mSunsetTimeHours < 15) {
                this.mSunsetTimeHours = 15;
                this.mSunsetTimeMins = 0;
            }
            if (this.mSunsetTimeHours > 22) {
                this.mSunsetTimeHours = 23;
                this.mSunsetTimeMins = 0;
            }
            Log.d(TAG, "updateWeatherInfo sunriseHour=" + this.mSunriseTimeHours + " sunriseMin=" + this.mSunriseTimeMins + " mNextSunriseSunsetTime=" + this.mNextSunriseSunsetTime);
            Log.d(TAG, "updateWeatherInfo sunsetHour=" + this.mSunsetTimeHours + " sunsetMin=" + this.mSunsetTimeMins);
            return;
        }
        this.mInternationalLocation = true;
    }

    public void updateWeatherInfo(Cursor cursor) {
        Log.d(TAG, "updateWeatherInfo start!");
        if (cursor == null) {
            Log.d(TAG, "weather maybe uninstalled");
            this.mInternationalLocation = true;
            return;
        }
        long sunriseTime = 0;
        long sunsetTime = 0;
        try {
            try {
                try {
                    if (cursor.getCount() != 0 && cursor.moveToFirst()) {
                        if (this.mNextSunriseSunsetTime) {
                            cursor.moveToNext();
                        }
                        sunriseTime = cursor.getLong(cursor.getColumnIndex("sunrise"));
                        sunsetTime = cursor.getLong(cursor.getColumnIndex("sunset"));
                    } else {
                        this.mInternationalLocation = true;
                    }
                    CalculateSunriseAndSunsetTime(sunriseTime, sunsetTime);
                    cursor.close();
                } catch (Throwable th) {
                    try {
                        cursor.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    throw th;
                }
            } catch (Exception e2) {
                this.mInternationalLocation = true;
                e2.printStackTrace();
                cursor.close();
            }
        } catch (Exception e3) {
            e3.printStackTrace();
        }
    }

    public boolean getDefaultTimeZoneStatus() {
        return this.mInternationalLocation;
    }

    public int getSunriseTimeHours() {
        return this.mSunriseTimeHours;
    }

    public int getSunsetTimeHours() {
        return this.mSunsetTimeHours;
    }

    public int getSunriseTimeMins() {
        return this.mSunriseTimeMins;
    }

    public int getSunsetTimeMins() {
        return this.mSunsetTimeMins;
    }

    /* loaded from: classes.dex */
    public final class QueryHandler extends AsyncQueryHandler {

        /* loaded from: classes.dex */
        protected class CatchingWorkerHandler extends AsyncQueryHandler.WorkerHandler {
            /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
            public CatchingWorkerHandler(Looper looper) {
                super(this$1, looper);
                QueryHandler.this = this$1;
            }

            @Override // android.content.AsyncQueryHandler.WorkerHandler, android.os.Handler
            public void handleMessage(Message msg) {
                try {
                    super.handleMessage(msg);
                } catch (SQLiteDatabaseCorruptException e) {
                    Log.d(AudioQueryWeatherService.TAG, "Exception on background", e);
                } catch (SQLiteDiskIOException e2) {
                    Log.d(AudioQueryWeatherService.TAG, "Exception background worker thread", e2);
                } catch (SQLiteFullException e3) {
                    Log.d(AudioQueryWeatherService.TAG, "Exception worker thread", e3);
                }
            }
        }

        @Override // android.content.AsyncQueryHandler
        protected Handler createHandler(Looper looper) {
            return new CatchingWorkerHandler(looper);
        }

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public QueryHandler(Context context) {
            super(context.getContentResolver());
            AudioQueryWeatherService.this = r1;
        }

        @Override // android.content.AsyncQueryHandler
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            if (token == AudioQueryWeatherService.CITY_QUERY_TOKEN) {
                AudioQueryWeatherService.this.updateCityInfo(cursor);
            } else {
                AudioQueryWeatherService.this.updateWeatherInfo(cursor);
            }
        }
    }
}
