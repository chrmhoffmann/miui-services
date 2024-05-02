package com.android.server.location;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.server.am.SplitScreenReporter;
import android.util.Log;
import com.android.server.location.GnssCollectData;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
/* loaded from: classes.dex */
public class GnssCollectDataDbDao {
    private static GnssCollectDataDbDao mGnssCollectDataDbDao;
    private GnssCollectDataDbHelper mGnssCollectDataDbHelper;
    private static final String TAG = "GnssCollectDataDbDao";
    private static final boolean DEBUG = Log.isLoggable(TAG, 3);
    private static final boolean VERBOSE = Log.isLoggable(TAG, 2);
    private static final String[] COLLECT_COLUMNS = {"_id", GnssCollectData.CollectDbEntry.COLUMN_NAME_STARTTIME, GnssCollectData.CollectDbEntry.COLUMN_NAME_TTFF, GnssCollectData.CollectDbEntry.COLUMN_NAME_RUNTIME, GnssCollectData.CollectDbEntry.COLUMN_NAME_LOSETIMES, GnssCollectData.CollectDbEntry.COLUMN_NAME_SAPNUMBER, GnssCollectData.CollectDbEntry.COLUMN_NAME_PDRNUMBER, GnssCollectData.CollectDbEntry.COLUMN_NAME_TOTALNUMBER, GnssCollectData.CollectDbEntry.COLUMN_NAME_L1TOP4MEANCN0, GnssCollectData.CollectDbEntry.COLUMN_NAME_L5TOP4MEANCN0, GnssCollectData.CollectDbEntry.COLUMN_NAME_B1TOP4MEANCN0};
    public static final GnssCollectData EMPTY_COLLECT = new GnssCollectData();
    public static final List<GnssCollectData> EMPTY_COLLECT_LIST = new ArrayList();

    public static GnssCollectDataDbDao getInstance(Context context) {
        if (mGnssCollectDataDbDao == null) {
            mGnssCollectDataDbDao = new GnssCollectDataDbDao(context);
        }
        return mGnssCollectDataDbDao;
    }

    private GnssCollectDataDbDao(Context context) {
        this.mGnssCollectDataDbHelper = getGnssCollectDataDbHelper(context);
    }

    private GnssCollectDataDbHelper getGnssCollectDataDbHelper(Context context) {
        GnssCollectDataDbHelper gnssCollectDataDbHelper;
        synchronized (GnssCollectDataDbDao.class) {
            if (this.mGnssCollectDataDbHelper == null) {
                this.mGnssCollectDataDbHelper = GnssCollectDataDbHelper.getInstance(context);
            }
            gnssCollectDataDbHelper = this.mGnssCollectDataDbHelper;
        }
        return gnssCollectDataDbHelper;
    }

    public void insertGnssCollectData(GnssCollectData gnssCollectData) {
        synchronized (GnssCollectDataDbDao.class) {
            insertGnssCollectData(gnssCollectData.getStartTime(), gnssCollectData.getTtff(), gnssCollectData.getRunTime(), gnssCollectData.getLoseTimes(), gnssCollectData.getSapNumber(), gnssCollectData.getPdrNumber(), gnssCollectData.getTotalNumber(), gnssCollectData.getL1Top4MeanCn0(), gnssCollectData.getL5Top4MeanCn0(), gnssCollectData.getB1Top4MeanCn0());
        }
    }

    public void insertGnssCollectData(long startTime, long TTFF, long runTime, int loseTimes, int SAPNumber, int PDRNumber, int totalNumber, double L1Top4MeanCn0, double L5Top4MeanCn0, double B1Top4MeanCn0) {
        SQLiteDatabase sqLiteDatabaseWritable;
        synchronized (GnssCollectDataDbDao.class) {
            try {
                sqLiteDatabaseWritable = this.mGnssCollectDataDbHelper.getWritableDatabase();
            } catch (SQLiteException e) {
                e.printStackTrace();
            }
            try {
                ContentValues values = new ContentValues();
                values.put(GnssCollectData.CollectDbEntry.COLUMN_NAME_STARTTIME, Long.valueOf(startTime));
                values.put(GnssCollectData.CollectDbEntry.COLUMN_NAME_TTFF, Long.valueOf(TTFF));
                values.put(GnssCollectData.CollectDbEntry.COLUMN_NAME_RUNTIME, Long.valueOf(runTime));
                values.put(GnssCollectData.CollectDbEntry.COLUMN_NAME_LOSETIMES, Integer.valueOf(loseTimes));
                values.put(GnssCollectData.CollectDbEntry.COLUMN_NAME_SAPNUMBER, Integer.valueOf(SAPNumber));
                values.put(GnssCollectData.CollectDbEntry.COLUMN_NAME_PDRNUMBER, Integer.valueOf(PDRNumber));
                values.put(GnssCollectData.CollectDbEntry.COLUMN_NAME_TOTALNUMBER, Integer.valueOf(totalNumber));
                values.put(GnssCollectData.CollectDbEntry.COLUMN_NAME_L1TOP4MEANCN0, Double.valueOf(L1Top4MeanCn0));
                values.put(GnssCollectData.CollectDbEntry.COLUMN_NAME_L5TOP4MEANCN0, Double.valueOf(L5Top4MeanCn0));
                values.put(GnssCollectData.CollectDbEntry.COLUMN_NAME_B1TOP4MEANCN0, Double.valueOf(B1Top4MeanCn0));
                long newRowId = sqLiteDatabaseWritable.insert(GnssCollectData.CollectDbEntry.TABLE_NAME, null, values);
                Log.v(TAG, "insertGnssCollectData " + (newRowId != -1 ? "successfully" : "failed"));
                if (sqLiteDatabaseWritable != null) {
                    sqLiteDatabaseWritable.close();
                }
            } catch (Throwable th) {
                if (sqLiteDatabaseWritable != null) {
                    try {
                        sqLiteDatabaseWritable.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                }
                throw th;
            }
        }
    }

    public void deleteAllGnssCollectData() {
        synchronized (GnssCollectDataDbDao.class) {
            try {
                SQLiteDatabase sqLiteDatabaseWritable = this.mGnssCollectDataDbHelper.getWritableDatabase();
                try {
                    int rowId = sqLiteDatabaseWritable.delete(GnssCollectData.CollectDbEntry.TABLE_NAME, null, null);
                    Log.v(TAG, "deleteAllGnssCollectData " + (rowId != 0 ? "successfully" : "failed"));
                    if (sqLiteDatabaseWritable != null) {
                        sqLiteDatabaseWritable.close();
                    }
                } catch (Throwable th) {
                    if (sqLiteDatabaseWritable != null) {
                        try {
                            sqLiteDatabaseWritable.close();
                        } catch (Throwable th2) {
                            th.addSuppressed(th2);
                        }
                    }
                    throw th;
                }
            } catch (SQLiteException e) {
                e.printStackTrace();
            }
        }
    }

    public ArrayList<String> filterStartTime() {
        ArrayList<String> item;
        synchronized (GnssCollectDataDbDao.class) {
            SQLiteDatabase sqLiteDatabaseReadable = this.mGnssCollectDataDbHelper.getReadableDatabase();
            item = new ArrayList<>();
            for (int i = 0; i < 24; i++) {
                String[] selectionArgs = {String.valueOf(i)};
                Cursor cursor = sqLiteDatabaseReadable.query(GnssCollectData.CollectDbEntry.TABLE_NAME, COLLECT_COLUMNS, "startTime == ? ", selectionArgs, null, null, null);
                item.add("ST" + i);
                item.add(String.valueOf(((Cursor) Objects.requireNonNull(cursor)).getCount()));
                if (cursor != null) {
                    cursor.close();
                }
            }
            sqLiteDatabaseReadable.close();
        }
        return item;
    }

    public ArrayList<String> filterTTFF() {
        ArrayList<String> item;
        synchronized (GnssCollectDataDbDao.class) {
            SQLiteDatabase sqLiteDatabaseReadable = this.mGnssCollectDataDbHelper.getReadableDatabase();
            item = new ArrayList<>();
            String[] selectionArgs = {"0", "5"};
            String[] strArr = COLLECT_COLUMNS;
            Cursor cursor = sqLiteDatabaseReadable.query(GnssCollectData.CollectDbEntry.TABLE_NAME, strArr, "TTFF > ? AND TTFF <= ?", selectionArgs, null, null, null);
            item.add("TTFF0-5");
            item.add(String.valueOf(((Cursor) Objects.requireNonNull(cursor)).getCount()));
            if (cursor != null) {
                cursor.close();
            }
            String[] selectionArgs2 = {"5", "10"};
            Cursor cursor2 = sqLiteDatabaseReadable.query(GnssCollectData.CollectDbEntry.TABLE_NAME, strArr, "TTFF > ? AND TTFF <= ?", selectionArgs2, null, null, null);
            item.add("TTFF5-10");
            item.add(String.valueOf(((Cursor) Objects.requireNonNull(cursor2)).getCount()));
            if (cursor2 != null) {
                cursor2.close();
            }
            String[] selectionArgs3 = {"10", "20"};
            Cursor cursor3 = sqLiteDatabaseReadable.query(GnssCollectData.CollectDbEntry.TABLE_NAME, strArr, "TTFF > ? AND TTFF <= ?", selectionArgs3, null, null, null);
            item.add("TTFF10-20");
            item.add(String.valueOf(((Cursor) Objects.requireNonNull(cursor3)).getCount()));
            if (cursor3 != null) {
                cursor3.close();
            }
            String[] selectionArgs4 = {"20", "30"};
            Cursor cursor4 = sqLiteDatabaseReadable.query(GnssCollectData.CollectDbEntry.TABLE_NAME, strArr, "TTFF > ? AND TTFF <= ?", selectionArgs4, null, null, null);
            item.add("TTFF20-30");
            item.add(String.valueOf(((Cursor) Objects.requireNonNull(cursor4)).getCount()));
            if (cursor4 != null) {
                cursor4.close();
            }
            Cursor cursor5 = sqLiteDatabaseReadable.query(GnssCollectData.CollectDbEntry.TABLE_NAME, strArr, "TTFF > 30 ", null, null, null, null);
            item.add("TTFF30+");
            item.add(String.valueOf(((Cursor) Objects.requireNonNull(cursor5)).getCount()));
            if (cursor5 != null) {
                cursor5.close();
            }
            sqLiteDatabaseReadable.close();
        }
        return item;
    }

    public ArrayList<String> filterRunTime() {
        ArrayList<String> item;
        synchronized (GnssCollectDataDbDao.class) {
            SQLiteDatabase sqLiteDatabaseReadable = this.mGnssCollectDataDbHelper.getReadableDatabase();
            item = new ArrayList<>();
            String[] selectionArgs = {"0", "10"};
            String[] strArr = COLLECT_COLUMNS;
            Cursor cursor = sqLiteDatabaseReadable.query(GnssCollectData.CollectDbEntry.TABLE_NAME, strArr, "runTime > ? AND runTime <= ?", selectionArgs, null, null, null);
            item.add("RT0-10");
            item.add(String.valueOf(((Cursor) Objects.requireNonNull(cursor)).getCount()));
            if (cursor != null) {
                cursor.close();
            }
            String[] selectionArgs2 = {"10", "60"};
            Cursor cursor2 = sqLiteDatabaseReadable.query(GnssCollectData.CollectDbEntry.TABLE_NAME, strArr, "runTime > ? AND runTime <= ?", selectionArgs2, null, null, null);
            item.add("RT10-60");
            item.add(String.valueOf(((Cursor) Objects.requireNonNull(cursor2)).getCount()));
            if (cursor2 != null) {
                cursor2.close();
            }
            String[] selectionArgs3 = {"60", "600"};
            Cursor cursor3 = sqLiteDatabaseReadable.query(GnssCollectData.CollectDbEntry.TABLE_NAME, strArr, "runTime > ? AND runTime <= ?", selectionArgs3, null, null, null);
            item.add("RT60-600");
            item.add(String.valueOf(((Cursor) Objects.requireNonNull(cursor3)).getCount()));
            if (cursor3 != null) {
                cursor3.close();
            }
            String[] selectionArgs4 = {"600", "3600"};
            Cursor cursor4 = sqLiteDatabaseReadable.query(GnssCollectData.CollectDbEntry.TABLE_NAME, strArr, "runTime > ? AND runTime <= ?", selectionArgs4, null, null, null);
            item.add("RT600-3600");
            item.add(String.valueOf(((Cursor) Objects.requireNonNull(cursor4)).getCount()));
            if (cursor4 != null) {
                cursor4.close();
            }
            Cursor cursor5 = sqLiteDatabaseReadable.query(GnssCollectData.CollectDbEntry.TABLE_NAME, strArr, "runTime > 3600 ", null, null, null, null);
            item.add("RT3600+");
            item.add(String.valueOf(((Cursor) Objects.requireNonNull(cursor5)).getCount()));
            if (cursor5 != null) {
                cursor5.close();
            }
            sqLiteDatabaseReadable.close();
        }
        return item;
    }

    public ArrayList<String> filterLoseTimes() {
        ArrayList<String> item;
        synchronized (GnssCollectDataDbDao.class) {
            SQLiteDatabase sqLiteDatabaseReadable = this.mGnssCollectDataDbHelper.getReadableDatabase();
            item = new ArrayList<>();
            String[] selectionArgs = {"0"};
            String[] strArr = COLLECT_COLUMNS;
            Cursor cursor = sqLiteDatabaseReadable.query(GnssCollectData.CollectDbEntry.TABLE_NAME, strArr, "loseTimes == ? ", selectionArgs, null, null, null);
            item.add("LT0");
            item.add(String.valueOf(((Cursor) Objects.requireNonNull(cursor)).getCount()));
            if (cursor != null) {
                cursor.close();
            }
            String[] selectionArgs2 = {SplitScreenReporter.ACTION_ENTER_SPLIT};
            Cursor cursor2 = sqLiteDatabaseReadable.query(GnssCollectData.CollectDbEntry.TABLE_NAME, strArr, "loseTimes == ? ", selectionArgs2, null, null, null);
            item.add("LT1");
            item.add(String.valueOf(((Cursor) Objects.requireNonNull(cursor2)).getCount()));
            if (cursor2 != null) {
                cursor2.close();
            }
            String[] selectionArgs3 = {SplitScreenReporter.ACTION_EXIT_SPLIT};
            Cursor cursor3 = sqLiteDatabaseReadable.query(GnssCollectData.CollectDbEntry.TABLE_NAME, strArr, "loseTimes == ? ", selectionArgs3, null, null, null);
            item.add("LT2");
            item.add(String.valueOf(((Cursor) Objects.requireNonNull(cursor3)).getCount()));
            if (cursor3 != null) {
                cursor3.close();
            }
            String[] selectionArgs4 = {"3"};
            Cursor cursor4 = sqLiteDatabaseReadable.query(GnssCollectData.CollectDbEntry.TABLE_NAME, strArr, "loseTimes == ? ", selectionArgs4, null, null, null);
            item.add("LT3");
            item.add(String.valueOf(((Cursor) Objects.requireNonNull(cursor4)).getCount()));
            if (cursor4 != null) {
                cursor4.close();
            }
            Cursor cursor5 = sqLiteDatabaseReadable.query(GnssCollectData.CollectDbEntry.TABLE_NAME, strArr, "loseTimes > 3 ", null, null, null, null);
            item.add("LT3+");
            item.add(String.valueOf(((Cursor) Objects.requireNonNull(cursor5)).getCount()));
            if (cursor5 != null) {
                cursor5.close();
            }
            sqLiteDatabaseReadable.close();
        }
        return item;
    }

    public ArrayList<String> sumSPT() {
        ArrayList<String> item;
        synchronized (GnssCollectDataDbDao.class) {
            SQLiteDatabase sqLiteDatabaseReadable = this.mGnssCollectDataDbHelper.getReadableDatabase();
            item = new ArrayList<>();
            Cursor cursor = sqLiteDatabaseReadable.rawQuery("select sum(SAPNumber) from GnssCollectData", null);
            item.add("SAP");
            if (cursor.moveToFirst()) {
                item.add(String.valueOf(cursor.getInt(0)));
            } else {
                item.add("0");
            }
            if (cursor != null) {
                cursor.close();
            }
            Cursor cursor2 = sqLiteDatabaseReadable.rawQuery("select sum(PDRNumber) from GnssCollectData", null);
            item.add("PDR");
            if (cursor2.moveToFirst()) {
                item.add(String.valueOf(cursor2.getInt(0)));
            } else {
                item.add("0");
            }
            if (cursor2 != null) {
                cursor2.close();
            }
            Cursor cursor3 = sqLiteDatabaseReadable.rawQuery("select sum(totalNumber) from GnssCollectData", null);
            item.add("Total");
            if (cursor3.moveToFirst()) {
                item.add(String.valueOf(cursor3.getInt(0)));
            } else {
                item.add("0");
            }
            if (cursor3 != null) {
                cursor3.close();
            }
            sqLiteDatabaseReadable.close();
        }
        return item;
    }

    public ArrayList<String> filterL1Top4MeanCn0() {
        ArrayList<String> item;
        synchronized (GnssCollectDataDbDao.class) {
            SQLiteDatabase sqLiteDatabaseReadable = this.mGnssCollectDataDbHelper.getReadableDatabase();
            item = new ArrayList<>();
            String[] selectionArgs = {"0", "20"};
            String[] strArr = COLLECT_COLUMNS;
            Cursor cursor = sqLiteDatabaseReadable.query(GnssCollectData.CollectDbEntry.TABLE_NAME, strArr, "L1Top4MeanCn0 > ? AND L1Top4MeanCn0 <= ?", selectionArgs, null, null, null);
            item.add("L1T0-20");
            item.add(String.valueOf(((Cursor) Objects.requireNonNull(cursor)).getCount()));
            if (cursor != null) {
                cursor.close();
            }
            String[] selectionArgs2 = {"20", "30"};
            Cursor cursor2 = sqLiteDatabaseReadable.query(GnssCollectData.CollectDbEntry.TABLE_NAME, strArr, "L1Top4MeanCn0 > ? AND L1Top4MeanCn0 <= ?", selectionArgs2, null, null, null);
            item.add("L1T20-30");
            item.add(String.valueOf(((Cursor) Objects.requireNonNull(cursor2)).getCount()));
            if (cursor2 != null) {
                cursor2.close();
            }
            String[] selectionArgs3 = {"30", "40"};
            Cursor cursor3 = sqLiteDatabaseReadable.query(GnssCollectData.CollectDbEntry.TABLE_NAME, strArr, "L1Top4MeanCn0 > ? AND L1Top4MeanCn0 <= ?", selectionArgs3, null, null, null);
            item.add("L1T30-40");
            item.add(String.valueOf(((Cursor) Objects.requireNonNull(cursor3)).getCount()));
            if (cursor3 != null) {
                cursor3.close();
            }
            Cursor cursor4 = sqLiteDatabaseReadable.query(GnssCollectData.CollectDbEntry.TABLE_NAME, strArr, "L1Top4MeanCn0 > 40 ", null, null, null, null);
            item.add("L1T40+");
            item.add(String.valueOf(((Cursor) Objects.requireNonNull(cursor4)).getCount()));
            if (cursor4 != null) {
                cursor4.close();
            }
            sqLiteDatabaseReadable.close();
        }
        return item;
    }

    public ArrayList<String> filterL5Top4MeanCn0() {
        ArrayList<String> item;
        synchronized (GnssCollectDataDbDao.class) {
            SQLiteDatabase sqLiteDatabaseReadable = this.mGnssCollectDataDbHelper.getReadableDatabase();
            item = new ArrayList<>();
            String[] selectionArgs = {"0", "20"};
            String[] strArr = COLLECT_COLUMNS;
            Cursor cursor = sqLiteDatabaseReadable.query(GnssCollectData.CollectDbEntry.TABLE_NAME, strArr, "L5Top4MeanCn0 > ? AND L5Top4MeanCn0 <= ?", selectionArgs, null, null, null);
            item.add("L5T0-20");
            item.add(String.valueOf(((Cursor) Objects.requireNonNull(cursor)).getCount()));
            if (cursor != null) {
                cursor.close();
            }
            String[] selectionArgs2 = {"20", "30"};
            Cursor cursor2 = sqLiteDatabaseReadable.query(GnssCollectData.CollectDbEntry.TABLE_NAME, strArr, "L5Top4MeanCn0 > ? AND L5Top4MeanCn0 <= ?", selectionArgs2, null, null, null);
            item.add("L5T20-30");
            item.add(String.valueOf(((Cursor) Objects.requireNonNull(cursor2)).getCount()));
            if (cursor2 != null) {
                cursor2.close();
            }
            String[] selectionArgs3 = {"30", "40"};
            Cursor cursor3 = sqLiteDatabaseReadable.query(GnssCollectData.CollectDbEntry.TABLE_NAME, strArr, "L5Top4MeanCn0 > ? AND L5Top4MeanCn0 <= ?", selectionArgs3, null, null, null);
            item.add("L5T30-40");
            item.add(String.valueOf(((Cursor) Objects.requireNonNull(cursor3)).getCount()));
            if (cursor3 != null) {
                cursor3.close();
            }
            Cursor cursor4 = sqLiteDatabaseReadable.query(GnssCollectData.CollectDbEntry.TABLE_NAME, strArr, "L5Top4MeanCn0 > 40 ", null, null, null, null);
            item.add("L5T40+");
            item.add(String.valueOf(((Cursor) Objects.requireNonNull(cursor4)).getCount()));
            if (cursor4 != null) {
                cursor4.close();
            }
            sqLiteDatabaseReadable.close();
        }
        return item;
    }

    public ArrayList<String> filterB1Top4MeanCn0() {
        ArrayList<String> item;
        synchronized (GnssCollectDataDbDao.class) {
            SQLiteDatabase sqLiteDatabaseReadable = this.mGnssCollectDataDbHelper.getReadableDatabase();
            item = new ArrayList<>();
            String[] selectionArgs = {"0", "20"};
            String[] strArr = COLLECT_COLUMNS;
            Cursor cursor = sqLiteDatabaseReadable.query(GnssCollectData.CollectDbEntry.TABLE_NAME, strArr, "B1Top4MeanCn0 > ? AND B1Top4MeanCn0 <= ?", selectionArgs, null, null, null);
            item.add("B1T0-20");
            item.add(String.valueOf(((Cursor) Objects.requireNonNull(cursor)).getCount()));
            if (cursor != null) {
                cursor.close();
            }
            String[] selectionArgs2 = {"20", "30"};
            Cursor cursor2 = sqLiteDatabaseReadable.query(GnssCollectData.CollectDbEntry.TABLE_NAME, strArr, "B1Top4MeanCn0 > ? AND B1Top4MeanCn0 <= ?", selectionArgs2, null, null, null);
            item.add("B1T20-30");
            item.add(String.valueOf(((Cursor) Objects.requireNonNull(cursor2)).getCount()));
            if (cursor2 != null) {
                cursor2.close();
            }
            String[] selectionArgs3 = {"30", "40"};
            Cursor cursor3 = sqLiteDatabaseReadable.query(GnssCollectData.CollectDbEntry.TABLE_NAME, strArr, "B1Top4MeanCn0 > ? AND B1Top4MeanCn0 <= ?", selectionArgs3, null, null, null);
            item.add("B1T30-40");
            item.add(String.valueOf(((Cursor) Objects.requireNonNull(cursor3)).getCount()));
            if (cursor3 != null) {
                cursor3.close();
            }
            Cursor cursor4 = sqLiteDatabaseReadable.query(GnssCollectData.CollectDbEntry.TABLE_NAME, strArr, "B1Top4MeanCn0 > 40 ", null, null, null, null);
            item.add("B1T40+");
            item.add(String.valueOf(((Cursor) Objects.requireNonNull(cursor4)).getCount()));
            if (cursor4 != null) {
                cursor4.close();
            }
            sqLiteDatabaseReadable.close();
        }
        return item;
    }

    public void updateGnssCollectData(int id, long startTime) {
        SQLiteDatabase sqLiteDatabaseWritable;
        synchronized (GnssCollectDataDbDao.class) {
            try {
                sqLiteDatabaseWritable = this.mGnssCollectDataDbHelper.getWritableDatabase();
            } catch (SQLiteException e) {
                e.printStackTrace();
            }
            try {
                String[] selectionArgs = {String.valueOf(id)};
                ContentValues values = new ContentValues();
                values.put(GnssCollectData.CollectDbEntry.COLUMN_NAME_STARTTIME, Long.valueOf(startTime));
                sqLiteDatabaseWritable.update(GnssCollectData.CollectDbEntry.TABLE_NAME, values, "_id = ?", selectionArgs);
                if (sqLiteDatabaseWritable != null) {
                    sqLiteDatabaseWritable.close();
                }
            } catch (Throwable th) {
                if (sqLiteDatabaseWritable != null) {
                    try {
                        sqLiteDatabaseWritable.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                }
                throw th;
            }
        }
    }
}
