package com.example.calendar.ui.gallery;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.calendar.R;
import com.example.calendar.data.AppDatabase;
import com.example.calendar.data.Meal;
import com.example.calendar.data.MealDao;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PhotoGalleryFragment extends Fragment {
    private static final String TAG = "PhotoGalleryFragment";
    private RecyclerView recyclerView;
    private PhotoGalleryAdapter adapter;
    private MealDao mealDao;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.ENGLISH);

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_photo_gallery, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        mealDao = AppDatabase.getDatabase(requireContext()).mealDao();
        
        recyclerView = view.findViewById(R.id.photoRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        
        adapter = new PhotoGalleryAdapter();
        recyclerView.setAdapter(adapter);
        
        loadPhotos();
    }

    private void loadPhotos() {
        // Get current user ID
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) return;
        
        String userId = currentUser.getUid();
        
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                // 獲取當前用戶所有有圖片的餐點
                List<Meal> allMeals = mealDao.getAllMeals(userId);
                List<Meal> mealsWithPhotos = new ArrayList<>();
                
                for (Meal meal : allMeals) {
                    if (meal.getImagePath() != null && !meal.getImagePath().isEmpty()) {
                        mealsWithPhotos.add(meal);
                    }
                }
                
                // 按日期分組
                Map<String, List<Meal>> groupedMeals = groupMealsByDate(mealsWithPhotos);
                
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        adapter.setData(groupedMeals);
                    });
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error loading photos", e);
            }
        });
    }

    private Map<String, List<Meal>> groupMealsByDate(List<Meal> meals) {
        Map<String, List<Meal>> grouped = new LinkedHashMap<>();
        
        for (Meal meal : meals) {
            String dateKey = dateFormat.format(meal.getDate());
            
            if (!grouped.containsKey(dateKey)) {
                grouped.put(dateKey, new ArrayList<>());
            }
            
            grouped.get(dateKey).add(meal);
        }
        
        return grouped;
    }

    @Override
    public void onResume() {
        super.onResume();
        // 重新加載數據以確保最新狀態
        loadPhotos();
    }
} 