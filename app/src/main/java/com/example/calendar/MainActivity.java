package com.example.calendar;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.CalendarView;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.example.calendar.data.AppDatabase;
import com.example.calendar.data.Meal;
import com.example.calendar.data.MealDao;
import com.example.calendar.ui.meals.DateSearchDialog;
import com.example.calendar.ui.meals.MealAdapter;
import com.example.calendar.ui.meals.AddEditMealDialog;
import com.example.calendar.ui.MainContentFragment;
import com.example.calendar.ui.ai.AIChatFragment;
import com.example.calendar.ui.gallery.PhotoGalleryFragment;
import com.example.calendar.ui.calories.CalorieFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import android.util.Log;
import java.util.ArrayList;
import android.widget.Button;
import com.example.calendar.ui.calendar.CalendarCardWidget;
import com.example.calendar.ui.calendar.CustomCalendarView;
import com.example.calendar.model.CalendarDay;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private AppDatabase database;
    private MealDao mealDao;
    private AIChatFragment aiChatFragment;
    private MainContentFragment mainContentFragment;
    private PhotoGalleryFragment photoGalleryFragment;

    private SimpleDateFormat monthYearFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    private Calendar currentCalendar = Calendar.getInstance();
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        
        // Check if user is signed in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            // User not signed in, redirect to login
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }

            // Initialize database in background
            AppDatabase.databaseWriteExecutor.execute(() -> {
        try {
            database = AppDatabase.getDatabase(this);
            mealDao = database.mealDao();

                    runOnUiThread(() -> {
            // Initialize fragments
            mainContentFragment = new MainContentFragment();
            aiChatFragment = new AIChatFragment();
            photoGalleryFragment = new PhotoGalleryFragment();

            // Show calorie fragment by default (home page)
            showCalorieFragment();

            // Setup bottom navigation
            setupBottomNavigation();
            
            // Set calorie tab as selected by default
            BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
            bottomNav.setSelectedItemId(R.id.action_calorie);
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Error initializing database", e);
                    runOnUiThread(() -> 
                        Toast.makeText(this, "Error initializing app", Toast.LENGTH_SHORT).show()
                    );
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            Toast.makeText(this, "Error initializing app", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        bottomNav.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.action_calendar) {
                showMainContent();
                return true;
            } else if (item.getItemId() == R.id.action_search) {
                showDateSearchDialog();
                return true;
            } else if (item.getItemId() == R.id.action_ai_chat) {
                showAIChat();
                return true;
            } else if (item.getItemId() == R.id.action_photo_gallery) {
                showPhotoGallery();
                return true;
            } else if (item.getItemId() == R.id.action_calorie) {
                showCalorieFragment();
                return true;
            }
            return false;
        });
    }

    private void showMainContent() {
        Log.d(TAG, "Showing MainContentFragment");
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.fragmentContainer, mainContentFragment)
            .commit();
        Log.d(TAG, "MainContentFragment transaction committed");
    }

    private void showAIChat() {
        Log.d(TAG, "Showing AIChatFragment");
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.fragmentContainer, aiChatFragment)
            .commit();
        Log.d(TAG, "AIChatFragment transaction committed");
    }

    private void showPhotoGallery() {
        Log.d(TAG, "Showing PhotoGalleryFragment");
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.fragmentContainer, photoGalleryFragment)
            .commit();
        Log.d(TAG, "PhotoGalleryFragment transaction committed");
    }

    private void showCalorieFragment() {
        Log.d(TAG, "Showing CalorieFragment");
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.fragmentContainer, new CalorieFragment())
            .commit();
        Log.d(TAG, "CalorieFragment transaction committed");
    }

    private void showDateSearchDialog() {
        DateSearchDialog dialog = new DateSearchDialog();
        dialog.setOnDateSelectedListener(date -> {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            
            // Set start time to 00:00
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            Date startDate = calendar.getTime();
            
            // Set end time to 23:59:59
            calendar.set(Calendar.HOUR_OF_DAY, 23);
            calendar.set(Calendar.MINUTE, 59);
            calendar.set(Calendar.SECOND, 59);
            calendar.set(Calendar.MILLISECOND, 999);
            Date endDate = calendar.getTime();

            // Query in background
            AppDatabase.databaseWriteExecutor.execute(() -> {
                FirebaseUser currentUser = mAuth.getCurrentUser();
                String userId = currentUser != null ? currentUser.getUid() : "";
                List<Meal> meals = mealDao.getMealsByDateRange(userId, startDate, endDate);
                runOnUiThread(() -> {
                    // Switch to main content and show calendar headers
                    showMainContent();
                    if (mainContentFragment != null) {
                        // Update calendar view's selected date first
                        mainContentFragment.setSelectedDate(startDate);
                        // Then load meals for that date
                        mainContentFragment.loadMealsForDate(startDate);

                    }
                    if (meals.isEmpty()) {
                        Toast.makeText(this, "No records for this date", Toast.LENGTH_SHORT).show();
                    }
                });
            });
        });
        dialog.show(getSupportFragmentManager(), "dateSearch");
    }

    public void showAddEditMealDialog(Meal meal) {
        try {
            Log.d(TAG, "showAddEditMealDialog called with meal: " + (meal != null ? meal.getMealType() : "new meal"));
            
            AddEditMealDialog dialog = new AddEditMealDialog();
            if (meal != null) {
                dialog.setMeal(meal);
            }
            dialog.setOnMealSaveListener(savedMeal -> {
                // Save meal to database
                AppDatabase.databaseWriteExecutor.execute(() -> {
                    try {
                        if (meal == null) {
                            // For new meal, set current date
                            Date now = new Date();
                            savedMeal.setDate(now);
                            Log.d(TAG, "Saving new meal: type=" + savedMeal.getMealType() + ", date=" + dateFormat.format(now));
                            mealDao.insertMeal(savedMeal);
                        } else {
                            Log.d(TAG, "Updating meal: type=" + savedMeal.getMealType() + ", date=" + dateFormat.format(savedMeal.getDate()));
                            mealDao.updateMeal(savedMeal);
                        }
                        
                        // Reload meals for current date
                        runOnUiThread(() -> {
                            Toast.makeText(this, meal == null ? "Meal added" : "Meal updated", Toast.LENGTH_SHORT).show();
                            if (mainContentFragment != null) {
                                Date mealDate = savedMeal.getDate();
                                Log.d(TAG, "Reloading meals for date: " + dateFormat.format(mealDate));
                                mainContentFragment.loadMealsForDate(mealDate);
                            }
                        });
                    } catch (Exception e) {
                        Log.e(TAG, "Error saving meal", e);
                        runOnUiThread(() -> 
                            Toast.makeText(this, "Error saving meal", Toast.LENGTH_SHORT).show()
                        );
                    }
                });
            });
            dialog.show(getSupportFragmentManager(), "addEditMeal");
            Log.d(TAG, "Dialog shown");
        } catch (Exception e) {
            Log.e(TAG, "Error showing dialog", e);
            Toast.makeText(this, "Error showing meal dialog", Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            logout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private void logout() {
        // Sign out from Firebase
        mAuth.signOut();
        
        // Redirect to login activity
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
        
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
    }

    private void initializeCalendar() {
        CalendarCardWidget calendarWidget = findViewById(R.id.calendarCardWidget);
        if (calendarWidget != null) {
            calendarWidget.setCurrentDate(new Date());
            calendarWidget.setOnDateSelectedListener(date -> {
                // Handle date selection
                if (mainContentFragment != null) {
                    mainContentFragment.setSelectedDate(date);
                }
            });
        }
    }
}