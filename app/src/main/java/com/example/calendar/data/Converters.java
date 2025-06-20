package com.example.calendar.data;

import androidx.room.TypeConverter;
import java.util.Date;

public class Converters {
    @TypeConverter
    public static Date fromTimestamp(Long value) {
        return value == null ? null : new Date(value);
    }

    @TypeConverter
    public static Long dateToTimestamp(Date date) {
        return date == null ? null : date.getTime();
    }

    @TypeConverter
    public static MealType fromString(String value) {
        return value == null ? null : MealType.valueOf(value);
    }

    @TypeConverter
    public static String mealTypeToString(MealType mealType) {
        return mealType == null ? null : mealType.name();
    }
} 