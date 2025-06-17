package com.example.calendar.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.calendar.R;
import com.example.calendar.model.CalendarDay;
import java.util.List;

public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder> {
    private List<CalendarDay> days;
    private OnDayClickListener listener;

    public interface OnDayClickListener {
        void onDayClick(CalendarDay day);
    }

    public CalendarAdapter(List<CalendarDay> days) {
        this.days = days;
    }

    public void setOnDayClickListener(OnDayClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public CalendarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_calendar_day, parent, false);
        return new CalendarViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CalendarViewHolder holder, int position) {
        CalendarDay day = days.get(position);
        holder.bind(day);
    }

    @Override
    public int getItemCount() {
        return days != null ? days.size() : 0;
    }

    class CalendarViewHolder extends RecyclerView.ViewHolder {
        private final TextView dateText;
        private final View selectedIndicator;

        CalendarViewHolder(View itemView) {
            super(itemView);
            dateText = itemView.findViewById(R.id.dateText);
            selectedIndicator = itemView.findViewById(R.id.selectedIndicator);
        }

        void bind(CalendarDay day) {
            dateText.setText(String.valueOf(day.getDayOfMonth()));
            
            if (day.isToday()) {
                selectedIndicator.setVisibility(View.VISIBLE);
                selectedIndicator.setBackgroundResource(R.drawable.today_circle_background);
            } else if (day.isSelected()) {
                selectedIndicator.setVisibility(View.VISIBLE);
                selectedIndicator.setBackgroundResource(R.drawable.selected_circle_background);
            } else {
                selectedIndicator.setVisibility(View.INVISIBLE);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDayClick(day);
                }
            });
        }
    }
} 