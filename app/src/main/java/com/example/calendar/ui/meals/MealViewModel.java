package com.example.calendar.ui.meals;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import com.example.calendar.data.AppDatabase;
import com.example.calendar.data.Meal;
import com.example.calendar.data.MealDao;
import java.util.List;
import java.util.Date;
import java.util.ArrayList;

public class MealViewModel extends AndroidViewModel {
    private MealDao mealDao;
    private final MutableLiveData<Date> selectedDate = new MutableLiveData<>();
    private LiveData<List<Meal>> mealsForDate;

    public MealViewModel(Application application) {
        super(application);
        AppDatabase database = AppDatabase.getDatabase(application);
        mealDao = database.mealDao();
        
        // Transform the date into a LiveData of meals
        mealsForDate = Transformations.switchMap(selectedDate, date -> {
            MutableLiveData<List<Meal>> liveData = new MutableLiveData<>();
            AppDatabase.databaseWriteExecutor.execute(() -> {
                // Get current user ID
                com.google.firebase.auth.FirebaseUser currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
                String userId = currentUser != null ? currentUser.getUid() : "";
                
                // Get meals for the date range (start of day to end of day)
                Date startDate = date; // Start of day
                Date endDate = new Date(date.getTime() + 24*60*60*1000); // End of day
                List<Meal> meals = mealDao.getMealsByDateRange(userId, startDate, endDate);
                liveData.postValue(meals);
            });
            return liveData;
        });
    }

    public void insertMeal(Meal meal) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            mealDao.insertMeal(meal);
        });
    }

    public void updateMeal(Meal meal) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            mealDao.updateMeal(meal);
        });
    }

    public void deleteMeal(Meal meal) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            mealDao.deleteMeal(meal);
        });
    }

    public LiveData<List<Meal>> getMealsForDate(Date date) {
        selectedDate.setValue(date);
        return mealsForDate;
    }
} 