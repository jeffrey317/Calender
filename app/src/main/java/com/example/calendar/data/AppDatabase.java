package com.example.calendar.data;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.util.Log;

@Database(entities = {Meal.class}, version = 3, exportSchema = false)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase INSTANCE;
    private static final String DATABASE_NAME = "calendar_db";
    private static final String TAG = "AppDatabase";
    
    // Create an ExecutorService with a cached thread pool for better performance
    public static final ExecutorService databaseWriteExecutor =
            Executors.newCachedThreadPool();

    public abstract MealDao mealDao();

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    try {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, DATABASE_NAME)
                            .fallbackToDestructiveMigration()
                                .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
                                .allowMainThreadQueries() // Only for debugging, remove in production
                            .build();
                    } catch (Exception e) {
                        Log.e(TAG, "Error creating database", e);
                        throw new RuntimeException("Database creation failed", e);
                    }
                }
            }
        }
        return INSTANCE;
    }
} 