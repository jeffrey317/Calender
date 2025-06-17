package com.example.calendar.ui.calories;

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.calendar.R;
import com.example.calendar.data.AppDatabase;
import com.example.calendar.data.Meal;
import com.example.calendar.data.MealDao;
import com.example.calendar.data.MealType;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import android.content.Intent;
import android.widget.Toast;
import com.example.calendar.LoginActivity;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import android.util.Log;

public class CalorieFragment extends Fragment {
    private static final String TAG = "CalorieFragment";
    private MealDao mealDao;
    
    // UI Components
    private TextView greetingText;
    private ImageButton logoutButton;
    private TextView dateText;
    private ImageButton changeDateButton;
    private TextView breakfastCalories;
    private TextView lunchCalories;
    private TextView dinnerCalories;
    private TextView snackCalories;
    private TextView totalCaloriesText;
    private TextView consumedCalories;


    private PieChart progressChart;
    private RecyclerView mealsRecyclerView;
    
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
    private Date currentDate = new Date();


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calorie, container, false);
        
        // Initialize database
        mealDao = AppDatabase.getDatabase(requireContext()).mealDao();
        
        // Initialize views
        initializeViews(view);
        
        // Setup greeting with Google account name
        setupGreeting();
        
        // Setup logout button
        setupLogoutButton();
        
        // Setup RecyclerView
        mealsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        
        // Setup progress chart
        setupProgressChart();
        
        // Setup date picker
        setupDatePicker();
        
        // Load today's data
        loadCaloriesForDate();
        
        return view;
    }
    
    private void initializeViews(View view) {
        greetingText = view.findViewById(R.id.greetingText);
        logoutButton = view.findViewById(R.id.logoutButton);
        dateText = view.findViewById(R.id.dateText);
        changeDateButton = view.findViewById(R.id.changeDateButton);
        breakfastCalories = view.findViewById(R.id.breakfastCalories);
        lunchCalories = view.findViewById(R.id.lunchCalories);
        dinnerCalories = view.findViewById(R.id.dinnerCalories);
        snackCalories = view.findViewById(R.id.snackCalories);
        totalCaloriesText = view.findViewById(R.id.totalCaloriesText);
        consumedCalories = view.findViewById(R.id.consumedCalories);


        progressChart = view.findViewById(R.id.progressChart);
        mealsRecyclerView = view.findViewById(R.id.mealsRecyclerView);
    }
    
    private void setupGreeting() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = auth.getCurrentUser();
        
        String userName = "Guest";
        if (currentUser != null) {
            String displayName = currentUser.getDisplayName();
            if (displayName != null && !displayName.isEmpty()) {
                userName = displayName;
            } else if (currentUser.getEmail() != null) {
                // Extract name from email if display name is not available
                String email = currentUser.getEmail();
                userName = email.substring(0, email.indexOf("@"));
            }
        }
        
        greetingText.setText(String.format("Hello, %s! Complete your daily nutrition", userName));
    }
    
    private void setupLogoutButton() {
        logoutButton.setOnClickListener(v -> {
            // Show confirmation dialog
            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> logout())
                .setNegativeButton("Cancel", null)
                .show();
        });
    }
    
    private void logout() {
        // Sign out from Firebase
        FirebaseAuth.getInstance().signOut();
        
        // Redirect to login activity
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        
        // Finish the current activity
        if (getActivity() != null) {
            getActivity().finish();
        }
        
        Toast.makeText(getContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();
    }
    
    private void loadCaloriesForDate() {
        // Get current user ID
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) return;
        
        String userId = currentUser.getUid();
        
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                // Get start and end of selected date
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(currentDate);
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                Date startDate = calendar.getTime();
                
                calendar.set(Calendar.HOUR_OF_DAY, 23);
                calendar.set(Calendar.MINUTE, 59);
                calendar.set(Calendar.SECOND, 59);
                Date endDate = calendar.getTime();
                
                // Get meals for selected date and current user
                List<Meal> dateMeals = mealDao.getMealsByDateRange(userId, startDate, endDate);
                
                // Calculate total calories and group by meal type (including snack)
                Map<MealType, Integer> mealCalories = new HashMap<>();
                mealCalories.put(MealType.BREAKFAST, 0);
                mealCalories.put(MealType.LUNCH, 0);
                mealCalories.put(MealType.DINNER, 0);
                mealCalories.put(MealType.OTHER, 0); // OTHER represents snack
                
                int totalCalories = 0;
                for (Meal meal : dateMeals) {
                    int calories = meal.getCalories();
                    totalCalories += calories;
                    
                    // Group calories by meal type (including snack)
                    MealType type = meal.getMealType();
                    mealCalories.put(type, mealCalories.get(type) + calories);
                }
                
                // Update UI on main thread
                int finalTotalCalories = totalCalories;
                Map<MealType, Integer> finalMealCalories = new HashMap<>(mealCalories);
                requireActivity().runOnUiThread(() -> {
                    updateUI(finalTotalCalories, finalMealCalories, dateMeals);
                });
                
            } catch (Exception e) {
                Log.e(TAG, "Error loading calories for date", e);
            }
        });
    }
    
    private void updateUI(int totalCalories, Map<MealType, Integer> mealCalories, List<Meal> dateMeals) {
        // Update date
        dateText.setText(dateFormat.format(currentDate));
        
        // Update meal cards
        breakfastCalories.setText(String.valueOf(mealCalories.get(MealType.BREAKFAST)));
        lunchCalories.setText(String.valueOf(mealCalories.get(MealType.LUNCH)));
        dinnerCalories.setText(String.valueOf(mealCalories.get(MealType.DINNER)));
        snackCalories.setText(String.valueOf(mealCalories.get(MealType.OTHER)));
        
        // Update total calories
        totalCaloriesText.setText(String.valueOf(totalCalories));
        consumedCalories.setText(String.valueOf(totalCalories));

        

        
        // Update progress chart
        updateProgressChart(mealCalories);
        
        // Show all meals including snacks
        mealsRecyclerView.setAdapter(new CalorieMealAdapter(dateMeals));
    }
    

    
    private void setupDatePicker() {
        View.OnClickListener dateClickListener = v -> showDatePicker();
        
        dateText.setOnClickListener(dateClickListener);
        changeDateButton.setOnClickListener(dateClickListener);
    }
    
    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(currentDate);
        
        DatePickerDialog datePickerDialog = new DatePickerDialog(
            requireContext(),
            (view, year, month, dayOfMonth) -> {
                Calendar selectedCalendar = Calendar.getInstance();
                selectedCalendar.set(year, month, dayOfMonth);
                currentDate = selectedCalendar.getTime();
                
                // Reload data for the new date
                loadCaloriesForDate();
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        );
        
        datePickerDialog.show();
    }
    
    private void setupProgressChart() {
        progressChart.setUsePercentValues(false);
        progressChart.getDescription().setEnabled(false);
        progressChart.setExtraOffsets(0, 0, 0, 0);
        
        // Create donut chart (hole in center)
        progressChart.setDrawHoleEnabled(true);
        progressChart.setHoleRadius(70f);
        progressChart.setTransparentCircleRadius(75f);
        progressChart.setDrawCenterText(false);
        
        progressChart.setRotationAngle(270);
        progressChart.setRotationEnabled(false);
        progressChart.setHighlightPerTapEnabled(false);
        
        // Hide legend
        Legend legend = progressChart.getLegend();
        legend.setEnabled(false);
    }
    
    private void updateProgressChart(Map<MealType, Integer> mealCalories) {
        ArrayList<PieEntry> entries = new ArrayList<>();
        
        // Add entries for each meal type
        int breakfastCalories = mealCalories.get(MealType.BREAKFAST);
        int lunchCalories = mealCalories.get(MealType.LUNCH);
        int dinnerCalories = mealCalories.get(MealType.DINNER);
        
        if (breakfastCalories > 0) {
            entries.add(new PieEntry(breakfastCalories, "Breakfast"));
        }
        if (lunchCalories > 0) {
            entries.add(new PieEntry(lunchCalories, "Lunch"));
        }
        if (dinnerCalories > 0) {
            entries.add(new PieEntry(dinnerCalories, "Dinner"));
        }
        
        // If no meals recorded, show a placeholder
        if (entries.isEmpty()) {
            entries.add(new PieEntry(1, "No Records"));
        }
        
        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setDrawIcons(false);
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(5f);
        
        // Set colors to match meal cards
        ArrayList<Integer> colors = new ArrayList<>();
        colors.add(Color.rgb(56, 178, 172)); // Teal for breakfast
        colors.add(Color.rgb(251, 191, 36)); // Yellow for lunch  
        colors.add(Color.rgb(59, 130, 246)); // Blue for dinner
        colors.add(Color.rgb(156, 163, 175)); // Gray for no data
        dataSet.setColors(colors);
        
        dataSet.setDrawValues(false);
        
        PieData data = new PieData(dataSet);
        progressChart.setData(data);
        progressChart.invalidate();
    }
} 