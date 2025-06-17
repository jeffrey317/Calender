package com.example.calendar.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.example.calendar.MainActivity;
import com.example.calendar.R;
import com.example.calendar.data.AppDatabase;
import com.example.calendar.data.Meal;
import com.example.calendar.data.MealDao;
import com.example.calendar.ui.meals.MealAdapter;
import com.example.calendar.ui.calendar.CalendarCardWidget;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainContentFragment extends Fragment {
    private static final String TAG = "MainContentFragment";
    private CalendarCardWidget calendarCardWidget;
    private RecyclerView mealsRecyclerView;
    private MealAdapter mealAdapter;
    private MealDao mealDao;
    private SimpleDateFormat monthYearFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    private FloatingActionButton addMealFab;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView called");
        View view = inflater.inflate(R.layout.fragment_main_content, container, false);

        try {
            // Initialize database
            mealDao = AppDatabase.getDatabase(requireContext()).mealDao();
            Log.d(TAG, "Database initialized");

            // Setup UI components
            setupRecyclerView(view);
            setupCalendarView(view);
            setupAddMealFab(view);
            Log.d(TAG, "UI components setup completed");
            
            // Load meals for current date
            Date today = new Date();
            Log.d(TAG, "Loading meals for today: " + dateFormat.format(today));
            loadMealsForDate(today);
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreateView", e);
            Toast.makeText(requireContext(), "Error initializing app", Toast.LENGTH_SHORT).show();
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called");
        if (addMealFab != null) {
            Log.d(TAG, "Ensuring FAB is visible in onResume");
            addMealFab.setVisibility(View.VISIBLE);
        }
    }

    private void setupAddMealFab(View view) {
        try {
            Log.d(TAG, "Setting up FAB click listener");
            addMealFab = view.findViewById(R.id.addMealFab);
            if (addMealFab != null) {
                Log.d(TAG, "FAB found in view");
                addMealFab.setOnClickListener(v -> {
                    try {
                        Log.d(TAG, "FAB clicked");
                        if (getActivity() == null) {
                            Log.e(TAG, "Activity is null");
                            return;
                        }
                        
                        if (!(getActivity() instanceof MainActivity)) {
                            Log.e(TAG, "Activity is not MainActivity: " + getActivity().getClass().getName());
                            return;
                        }
                        
                        Log.d(TAG, "Showing add meal dialog");
                        ((MainActivity) getActivity()).showAddEditMealDialog(null);
                    } catch (Exception e) {
                        Log.e(TAG, "Error in FAB click handler", e);
                        Toast.makeText(requireContext(), "Error showing meal dialog", Toast.LENGTH_SHORT).show();
                    }
                });
                addMealFab.setVisibility(View.VISIBLE);
                Log.d(TAG, "FAB setup completed");
            } else {
                Log.e(TAG, "FAB not found in view");
                throw new IllegalStateException("FAB not found in view");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up FAB", e);
            Toast.makeText(requireContext(), "Error setting up add button", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupRecyclerView(View view) {
        mealsRecyclerView = view.findViewById(R.id.mealsRecyclerView);
        mealsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        
        mealAdapter = new MealAdapter(meal -> {
            if (getActivity() != null) {
                ((MainActivity) getActivity()).showAddEditMealDialog(meal);
            }
        });
        
        mealsRecyclerView.setAdapter(mealAdapter);
    }

    private void setupCalendarView(View view) {
        calendarCardWidget = view.findViewById(R.id.calendarCardWidget);
        
        // Set initial date to today
        Date today = new Date();
        Log.d(TAG, "Setting initial calendar date to: " + dateFormat.format(today));
        calendarCardWidget.setCurrentDate(today);
        

        
        calendarCardWidget.setOnDateSelectedListener(selectedDate -> {
            Log.d(TAG, "Date selected: " + dateFormat.format(selectedDate));
            loadMealsForDate(selectedDate);
            

        });
    }

    public void setSelectedDate(Date date) {
        if (calendarCardWidget != null) {
            Log.d(TAG, "Setting selected date to: " + dateFormat.format(date));
            // First update the calendar view
            calendarCardWidget.setCurrentDate(date);
            // Then load meals for the selected date
            loadMealsForDate(date);

        }
    }

    public void loadMealsForDate(Date date) {
        try {
            Log.d(TAG, "loadMealsForDate called for: " + dateFormat.format(date));
            
            // Get current user ID
            FirebaseAuth auth = FirebaseAuth.getInstance();
            FirebaseUser currentUser = auth.getCurrentUser();
            if (currentUser == null) {
                Log.e(TAG, "No current user found");
                return;
            }
            
            String userId = currentUser.getUid();
            
            // Convert to start and end of day for proper date range query
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            Date startDate = calendar.getTime();
            
            calendar.set(Calendar.HOUR_OF_DAY, 23);
            calendar.set(Calendar.MINUTE, 59);
            calendar.set(Calendar.SECOND, 59);
            calendar.set(Calendar.MILLISECOND, 999);
            Date endDate = calendar.getTime();
            
            Log.d(TAG, "Querying meals for user " + userId + " between " + dateFormat.format(startDate) + " and " + dateFormat.format(endDate));
            
            AppDatabase.databaseWriteExecutor.execute(() -> {
                try {
                    List<Meal> meals = mealDao.getMealsByDateRange(userId, startDate, endDate);
                    Log.d(TAG, "Found " + meals.size() + " meals for the selected date");
                    
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            try {
                                mealAdapter.setMeals(meals);
                                View emptyStateText = getView().findViewById(R.id.emptyStateText);
                                if (meals.isEmpty()) {
                                    Log.d(TAG, "No meals found for the selected date");
                                    mealsRecyclerView.setVisibility(View.GONE);
                                    emptyStateText.setVisibility(View.VISIBLE);
                                } else {
                                    mealsRecyclerView.setVisibility(View.VISIBLE);
                                    emptyStateText.setVisibility(View.GONE);
                                    for (Meal meal : meals) {
                                        Log.d(TAG, "Meal: type=" + meal.getMealType() + ", date=" + dateFormat.format(meal.getDate()));
                                    }
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error updating UI with meals", e);
                            }
                        });
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error querying meals", e);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error in loadMealsForDate", e);
        }
    }
} 