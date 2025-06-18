package com.example.calendar.ui.calories;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.calendar.R;
import com.example.calendar.data.Meal;
import com.example.calendar.data.MealType;
import java.util.List;
import java.util.Locale;

public class CalorieMealAdapter extends RecyclerView.Adapter<CalorieMealAdapter.ViewHolder> {
    private List<Meal> meals;

    public CalorieMealAdapter(List<Meal> meals) {
        this.meals = meals;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_calorie_meal, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Meal meal = meals.get(position);
        
        // Set meal type
        String mealTypeText;
        switch (meal.getMealType()) {
            case BREAKFAST:
                mealTypeText = "Breakfast";
                break;
            case LUNCH:
                mealTypeText = "Lunch";
                break;
            case DINNER:
                mealTypeText = "Dinner";
                break;
            case SNACK:
                mealTypeText = "Snack";
                break;
            default:
                mealTypeText = "Other";
                break;
        }
        
        holder.mealTypeText.setText(mealTypeText);
        holder.foodContentText.setText(meal.getNotes());
        holder.caloriesText.setText(String.format(Locale.getDefault(), "%d calories", meal.getCalories()));
    }

    @Override
    public int getItemCount() {
        return meals != null ? meals.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView mealTypeText;
        TextView foodContentText;
        TextView caloriesText;

        ViewHolder(View itemView) {
            super(itemView);
            mealTypeText = itemView.findViewById(R.id.mealTypeText);
            foodContentText = itemView.findViewById(R.id.foodContentText);
            caloriesText = itemView.findViewById(R.id.caloriesText);
        }
    }
} 