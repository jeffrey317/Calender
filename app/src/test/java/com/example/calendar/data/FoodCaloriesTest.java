package com.example.calendar.data;

import org.junit.Test;
import static org.junit.Assert.*;

public class FoodCaloriesTest {

    @Test
    public void getCalories_knownFood_returnsCorrectCalories() {
        // Test known food items
        assertEquals(155, FoodCalories.getCalories("egg"));
        assertEquals(130, FoodCalories.getCalories("rice"));
        assertEquals(165, FoodCalories.getCalories("chicken"));
    }

    @Test
    public void getCalories_unknownFood_returnsZero() {
        assertEquals(0, FoodCalories.getCalories("unknown_food_item"));
    }

    @Test
    public void getCalories_caseSensitive_handlesCorrectly() {
        // Test case insensitivity
        assertEquals(155, FoodCalories.getCalories("EGG"));
        assertEquals(155, FoodCalories.getCalories("Egg"));
        assertEquals(155, FoodCalories.getCalories("egg"));
    }

    @Test
    public void getCalories_whitespace_handlesCorrectly() {
        // Test whitespace handling
        assertEquals(155, FoodCalories.getCalories("  egg  "));
        assertEquals(155, FoodCalories.getCalories("egg  "));
        assertEquals(155, FoodCalories.getCalories("  egg"));
    }
} 