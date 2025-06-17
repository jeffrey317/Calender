package com.example.calendar.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.calendar.model.CalorieEntry;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CalorieDbHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "calorie_tracker.db";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_CALORIES = "calories";
    private static final String COLUMN_ID = "_id";
    private static final String COLUMN_CALORIES = "calories";
    private static final String COLUMN_DESCRIPTION = "description";
    private static final String COLUMN_DATE = "date";

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    public CalorieDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_CALORIES + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_CALORIES + " INTEGER, " +
                COLUMN_DESCRIPTION + " TEXT, " +
                COLUMN_DATE + " TEXT)";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CALORIES);
        onCreate(db);
    }

    // Save a new calorie entry
    public long saveCalorieEntry(CalorieEntry entry) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(COLUMN_CALORIES, entry.getCalories());
        values.put(COLUMN_DESCRIPTION, entry.getDescription());
        values.put(COLUMN_DATE, dateFormat.format(entry.getDate()));

        long id = db.insert(TABLE_CALORIES, null, values);
        db.close();
        return id;
    }

    // Get all calorie entries
    public List<CalorieEntry> getAllCalorieEntries() {
        List<CalorieEntry> entries = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_CALORIES + " ORDER BY " + COLUMN_DATE + " DESC";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                CalorieEntry entry = new CalorieEntry(
                    cursor.getInt(cursor.getColumnIndex(COLUMN_CALORIES)),
                    cursor.getString(cursor.getColumnIndex(COLUMN_DESCRIPTION))
                );
                entry.setId(cursor.getLong(cursor.getColumnIndex(COLUMN_ID)));
                try {
                    String dateStr = cursor.getString(cursor.getColumnIndex(COLUMN_DATE));
                    entry.setDate(dateFormat.parse(dateStr));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                entries.add(entry);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return entries;
    }
} 