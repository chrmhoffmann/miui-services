package com.android.server;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Slog;
/* loaded from: classes.dex */
public class DarkModeSunTimeHelper {
    private static final String DATA_TYPE = "dataType";
    private static final String LOCAL_ID = "localId";
    private static final String PARAM_DATA_TYPE = "1";
    private static final String PARAM_LOCAL_ID = "1123332";
    private static final String TAG = "DarkModeSunTimeHelper";
    public static final String WEATHER_CITY_URI_STRING = "content://weather/selected_city";
    private static final String WEATHER_URI_STRING = "content://weather/hourlyData/{dataType}/{localId}";

    public static SunTime getSunRiseSunSetTime(Context context) {
        SunTime sunTime = getSunRiseSunSetTimeByDB(context);
        if (sunTime == null) {
            return getDefaultSunriseSunsetTime();
        }
        return sunTime;
    }

    public static SunTime getSunRiseSunSetTimeByDB(Context context) {
        Cursor cursor = null;
        try {
            try {
                Uri uri = Uri.parse(WEATHER_URI_STRING).buildUpon().appendQueryParameter(DATA_TYPE, "1").appendQueryParameter(LOCAL_ID, PARAM_LOCAL_ID).build();
                cursor = context.getContentResolver().query(uri, new String[]{"sunrise", "sunset"}, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    long sunrise = cursor.getLong(cursor.getColumnIndex("sunrise"));
                    long sunset = cursor.getLong(cursor.getColumnIndex("sunset"));
                    if (checkSuntimeIllegal(sunrise, sunset)) {
                        return new SunTime(formatToMinuteOfDay(sunrise, "sunrise"), formatToMinuteOfDay(sunset, "sunset"));
                    }
                    return null;
                }
            } catch (Exception e) {
                Slog.e(TAG, "get suntime error: " + e);
            }
            closeCursor(cursor);
            Slog.i(TAG, "get sunrise and sunset time fail");
            return null;
        } finally {
            closeCursor(cursor);
        }
    }

    private static SunTime getDefaultSunriseSunsetTime() {
        return new SunTime(360, 1080);
    }

    private static void closeCursor(Cursor cursor) {
        if (cursor != null) {
            cursor.close();
        }
    }

    private static boolean checkSuntimeIllegal(long sunrise, long sunset) {
        if (sunrise == sunset) {
            Slog.i(TAG, "get sunrise and sunset time fail, sunrise == sunset");
            return false;
        }
        int sunriseMinute = (int) ((sunrise / 1000) / 60);
        int sunsetMinute = (int) ((sunset / 1000) / 60);
        if (DarkModeTimeModeHelper.isSuntimeIllegal(sunriseMinute, sunsetMinute)) {
            Slog.i(TAG, "get sunrise and sunset time illegal");
            return false;
        }
        return true;
    }

    private static int formatToMinuteOfDay(long time, String type) {
        int hour = (int) (((time / 1000) / 60) / 60);
        int min = (int) (((time / 1000) / 60) - (hour * 60));
        Slog.i(TAG, "type = " + type + " hour = " + hour + " min = " + min);
        return (hour * 60) + min;
    }

    /* loaded from: classes.dex */
    public static class SunTime {
        private int sunrise;
        private int sunset;

        public SunTime() {
        }

        public SunTime(int sunrise, int sunset) {
            this.sunrise = sunrise;
            this.sunset = sunset;
        }

        public void setSunrise(int sunrise) {
            this.sunrise = sunrise;
        }

        public void setSunset(int sunset) {
            this.sunset = sunset;
        }

        public int getSunrise() {
            return this.sunrise;
        }

        public int getSunset() {
            return this.sunset;
        }
    }
}
