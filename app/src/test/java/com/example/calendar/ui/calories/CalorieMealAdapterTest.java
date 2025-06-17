package com.example.calendar.ui.calories;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.os.Build;

import com.example.calendar.R;
import com.example.calendar.data.Meal;
import com.example.calendar.data.MealType;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Build.VERSION_CODES.O_MR1})
public class CalorieMealAdapterTest {

    @Mock
    private View mockItemView;
    
    @Mock
    private TextView mockMealTypeText;
    
    @Mock
    private TextView mockFoodContentText;
    
    @Mock
    private TextView mockCaloriesText;

    private CalorieMealAdapter adapter;
    private List<Meal> testMeals;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);

        // Setup mock views
        when(mockItemView.findViewById(R.id.mealTypeText)).thenReturn(mockMealTypeText);
        when(mockItemView.findViewById(R.id.foodContentText)).thenReturn(mockFoodContentText);
        when(mockItemView.findViewById(R.id.caloriesText)).thenReturn(mockCaloriesText);

        // Create test meals
        testMeals = new ArrayList<>();
        testMeals.add(createTestMeal(MealType.BREAKFAST, "Oatmeal with fruits", 300));
        testMeals.add(createTestMeal(MealType.LUNCH, "Chicken salad", 450));
        testMeals.add(createTestMeal(MealType.DINNER, "Grilled fish", 400));
        testMeals.add(createTestMeal(MealType.OTHER, "Apple snack", 100));

        adapter = new CalorieMealAdapter(testMeals);
    }

    private Meal createTestMeal(MealType type, String notes, int calories) {
        Date date = new Date();
        return new Meal("test_user", date, type, "", "", null, null, notes, calories);
    }

    @Test
    public void testGetItemCount() {
        assertEquals(4, adapter.getItemCount());
        
        adapter = new CalorieMealAdapter(null);
        assertEquals(0, adapter.getItemCount());
        
        adapter = new CalorieMealAdapter(new ArrayList<>());
        assertEquals(0, adapter.getItemCount());
    }

    @Test
    public void testOnBindViewHolder() {
        CalorieMealAdapter.ViewHolder holder = new CalorieMealAdapter.ViewHolder(mockItemView);
        
        // Test breakfast meal
        adapter.onBindViewHolder(holder, 0);
        verify(mockMealTypeText).setText("Breakfast");
        verify(mockFoodContentText).setText("Oatmeal with fruits");
        verify(mockCaloriesText).setText("300 calories");
        
        // Test lunch meal
        adapter.onBindViewHolder(holder, 1);
        verify(mockMealTypeText).setText("Lunch");
        verify(mockFoodContentText).setText("Chicken salad");
        verify(mockCaloriesText).setText("450 calories");
        
        // Test dinner meal
        adapter.onBindViewHolder(holder, 2);
        verify(mockMealTypeText).setText("Dinner");
        verify(mockFoodContentText).setText("Grilled fish");
        verify(mockCaloriesText).setText("400 calories");
        
        // Test snack meal
        adapter.onBindViewHolder(holder, 3);
        verify(mockMealTypeText).setText("Snack");
        verify(mockFoodContentText).setText("Apple snack");
        verify(mockCaloriesText).setText("100 calories");
    }
} 