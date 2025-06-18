package com.example.calendar.data;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.util.Log;

@Database(entities = {Meal.class}, version = 4, exportSchema = false)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase INSTANCE;
    private static final String DATABASE_NAME = "calendar_db";
    private static final String TAG = "AppDatabase";
    
    // Create an ExecutorService with a cached thread pool for better performance
    public static final ExecutorService databaseWriteExecutor =
            Executors.newCachedThreadPool();

    public abstract MealDao mealDao();

    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // Migration from version 1 to 2
            // Add any necessary migration steps here if needed
        }
    };

    private static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // Update all 'OTHER' meal types to 'SNACK'
            database.execSQL("UPDATE meal SET meal_type = 'SNACK' WHERE meal_type = 'OTHER'");
        }
    };

    private static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // Recreate the meal table with proper structure
            database.execSQL("DROP TABLE IF EXISTS meal");
            database.execSQL("CREATE TABLE IF NOT EXISTS `meal` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`userId` TEXT, " +
                "`date` INTEGER, " +
                "`mealType` TEXT, " +
                "`imagePath` TEXT, " +
                "`location` TEXT, " +
                "`latitude` REAL, " +
                "`longitude` REAL, " +
                "`notes` TEXT, " +
                "`calories` INTEGER NOT NULL DEFAULT 0)");
        }
    };

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    try {
                        INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                AppDatabase.class, DATABASE_NAME)
                                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                                .fallbackToDestructiveMigration() // Clear and recreate if migration fails
                                .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
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