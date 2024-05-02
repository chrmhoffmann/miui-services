package com.android.server.location;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
/* loaded from: classes.dex */
public class GnssCollectDataDbHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "gnssCollectData.db";
    private static final int DB_VERSION = 1;
    private static final String SQL_CREATE_ENTRIES = "CREATE TABLE IF NOT EXISTS GnssCollectData (_id INTEGER PRIMARY KEY,startTime INTEGER,TTFF INTEGER,runTime INTEGER,loseTimes INTEGER,SAPNumber INTEGER,PDRNumber INTEGER,totalNumber INTEGER,L1Top4MeanCn0 REAL,L5Top4MeanCn0 REAL,B1Top4MeanCn0 REAL)";
    private static final String SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS GnssCollectData";
    private static final String STR_COMMA = ",";
    private static final String STR_INTEGER = " INTEGER";
    private static final String STR_REAL = " REAL";
    private static GnssCollectDataDbHelper mGnssCollectDataHelper;

    public static GnssCollectDataDbHelper getInstance(Context context) {
        if (mGnssCollectDataHelper == null) {
            mGnssCollectDataHelper = new GnssCollectDataDbHelper(context);
        }
        return mGnssCollectDataHelper;
    }

    private GnssCollectDataDbHelper(Context context) {
        super(context, DB_NAME, (SQLiteDatabase.CursorFactory) null, 1);
    }

    @Override // android.database.sqlite.SQLiteOpenHelper
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    @Override // android.database.sqlite.SQLiteOpenHelper
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }

    @Override // android.database.sqlite.SQLiteOpenHelper
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }
}
