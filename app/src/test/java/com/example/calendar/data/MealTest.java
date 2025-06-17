package com.example.calendar.data;

import android.os.Build;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
import java.util.Date;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Build.VERSION_CODES.O_MR1})
public class MealTest {

    @Test
    public void testMealCreation() {
        Date date = new Date();
        Meal meal = new Meal("test_user", date, MealType.BREAKFAST, "", "", null, null, "Test meal", 300);
        
        assertEquals("test_user", meal.getUserId());
        assertEquals(date, meal.getDate());
        assertEquals(MealType.BREAKFAST, meal.getMealType());
        assertEquals("Test meal", meal.getNotes());
        assertEquals(300, meal.getCalories());
        assertNull(meal.getLatitude());
        assertNull(meal.getLongitude());
        assertEquals("", meal.getImagePath());
        assertEquals("", meal.getLocation());
    }

    @Test
    public void testMealSetters() {
        Date date = new Date();
        Meal meal = new Meal("test_user", date, MealType.BREAKFAST, "", "", null, null, "Test meal", 300);
        
        Date newDate = new Date(date.getTime() + 1000);
        meal.setDate(newDate);
        meal.setMealType(MealType.LUNCH);
        meal.setNotes("Updated meal");
        meal.setCalories(400);
        meal.setLatitude(12.34);
        meal.setLongitude(56.78);
        meal.setImagePath("test.jpg");
        meal.setLocation("Test Location");
        meal.setUserId("new_user");
        
        assertEquals("new_user", meal.getUserId());
        assertEquals(newDate, meal.getDate());
        assertEquals(MealType.LUNCH, meal.getMealType());
        assertEquals("Updated meal", meal.getNotes());
        assertEquals(400, meal.getCalories());
        assertEquals(Double.valueOf(12.34), meal.getLatitude());
        assertEquals(Double.valueOf(56.78), meal.getLongitude());
        assertEquals("test.jpg", meal.getImagePath());
        assertEquals("Test Location", meal.getLocation());
    }

    @Test
    public void mealType_conversion_isCorrect() {
        assertEquals(MealType.BREAKFAST, MealType.valueOf("BREAKFAST"));
        assertEquals(MealType.LUNCH, MealType.valueOf("LUNCH"));
        assertEquals(MealType.DINNER, MealType.valueOf("DINNER"));
        assertEquals(MealType.OTHER, MealType.valueOf("OTHER"));
    }
} 