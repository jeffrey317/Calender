package com.example.calendar.ui.gallery;

import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.calendar.R;
import com.example.calendar.data.Meal;
import com.example.calendar.data.MealType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PhotoGalleryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_DATE_HEADER = 0;
    private static final int TYPE_PHOTO_ITEM = 1;
    
    private List<GalleryItem> items = new ArrayList<>();
    
    public void setData(Map<String, List<Meal>> groupedMeals) {
        items.clear();
        
        for (Map.Entry<String, List<Meal>> entry : groupedMeals.entrySet()) {
            // 添加日期標題
            items.add(new GalleryItem(entry.getKey(), null, true));
            
            // 添加該日期的所有餐點圖片
            for (Meal meal : entry.getValue()) {
                items.add(new GalleryItem(entry.getKey(), meal, false));
            }
        }
        
        notifyDataSetChanged();
    }
    
    @Override
    public int getItemViewType(int position) {
        return items.get(position).isHeader ? TYPE_DATE_HEADER : TYPE_PHOTO_ITEM;
    }
    
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_DATE_HEADER) {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_date_header, parent, false);
            return new DateHeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_photo_gallery, parent, false);
            return new PhotoViewHolder(view);
        }
    }
    
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        GalleryItem item = items.get(position);
        
        if (holder instanceof DateHeaderViewHolder) {
            ((DateHeaderViewHolder) holder).bind(item.dateKey);
        } else if (holder instanceof PhotoViewHolder) {
            ((PhotoViewHolder) holder).bind(item.meal);
        }
    }
    
    @Override
    public int getItemCount() {
        return items.size();
    }
    
    static class DateHeaderViewHolder extends RecyclerView.ViewHolder {
        TextView dateText;
        
        DateHeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            dateText = itemView.findViewById(R.id.dateText);
        }
        
        void bind(String date) {
            dateText.setText(date);
        }
    }
    
    static class PhotoViewHolder extends RecyclerView.ViewHolder {
        ImageView photoImage;
        TextView mealTypeText;
        TextView mealNotesText;
        
        PhotoViewHolder(@NonNull View itemView) {
            super(itemView);
            photoImage = itemView.findViewById(R.id.photoImage);
            mealTypeText = itemView.findViewById(R.id.mealTypeText);
            mealNotesText = itemView.findViewById(R.id.mealNotesText);
        }
        
        void bind(Meal meal) {
            // 設置圖片
            if (meal.getImagePath() != null) {
                photoImage.setImageBitmap(BitmapFactory.decodeFile(meal.getImagePath()));
            }
            
            // 設置餐點類型
            String mealType = getMealTypeText(meal.getMealType());
            mealTypeText.setText(mealType);
            
            // 設置食物內容
            if (meal.getNotes() != null && !meal.getNotes().isEmpty()) {
                mealNotesText.setText(meal.getNotes());
                mealNotesText.setVisibility(View.VISIBLE);
            } else {
                mealNotesText.setVisibility(View.GONE);
            }
        }
        
        private String getMealTypeText(MealType mealType) {
            if (mealType == null) return "";
            
            switch (mealType) {
                case BREAKFAST:
                    return "Breakfast";
                case LUNCH:
                    return "Lunch";
                case DINNER:
                    return "Dinner";
                case SNACK:
                    return "Snack";
                default:
                    return "";
            }
        }
    }
    
    static class GalleryItem {
        String dateKey;
        Meal meal;
        boolean isHeader;
        
        GalleryItem(String dateKey, Meal meal, boolean isHeader) {
            this.dateKey = dateKey;
            this.meal = meal;
            this.isHeader = isHeader;
        }
    }
} 