package com.example.calendar.data;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import java.util.Date;
import java.util.List;

@Dao
public interface MealDao {
    @Query("SELECT * FROM meal WHERE date = :date")
    LiveData<List<Meal>> getMealsForDate(Date date);

    @Query("SELECT * FROM meal WHERE date BETWEEN :startDate AND :endDate")
    LiveData<List<Meal>> getMealsForDateRange(Date startDate, Date endDate);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertMeal(Meal meal);

    @Update
    void updateMeal(Meal meal);

    @Delete
    void deleteMeal(Meal meal);

    @Query("SELECT * FROM meal WHERE date BETWEEN :startDate AND :endDate AND userId = :userId")
    List<Meal> getMealsByDateRange(String userId, Date startDate, Date endDate);

    @Query("SELECT * FROM meal WHERE userId = :userId AND date >= :date AND date < datetime(:date, '+1 day') AND mealType = :type")
    List<Meal> getMealsByDateAndType(String userId, String date, String type);

    @Query("SELECT * FROM meal WHERE userId = :userId")
    List<Meal> getAllMeals(String userId);
} 