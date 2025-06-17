package com.example.calendar.ui.calendar;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.GridLayout;
import android.widget.TextView;
import androidx.cardview.widget.CardView;
import com.example.calendar.R;
import com.example.calendar.model.CalendarDay;
import com.google.android.material.elevation.ElevationOverlayProvider;
import java.util.Calendar;
import java.util.List;

public class CustomCalendarView extends CardView {
    private GridLayout gridLayout;
    private List<CalendarDay> days;
    private Calendar calendar;
    private OnDayClickListener listener;
    private View selectedView;
    private ElevationOverlayProvider elevationOverlayProvider;

    public interface OnDayClickListener {
        void onDayClick(CalendarDay day);
    }

    public CustomCalendarView(Context context) {
        super(context);
        init(context);
    }

    public CustomCalendarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        inflate(context, R.layout.widget_calendar_card, this);
        gridLayout = findViewById(R.id.calendarGrid);
        calendar = Calendar.getInstance();
        elevationOverlayProvider = new ElevationOverlayProvider(context);
        
        // Set material design properties
        setRadius(getResources().getDimensionPixelSize(R.dimen.calendar_card_radius));
        setElevation(getResources().getDimensionPixelSize(R.dimen.calendar_card_elevation));
    }

    public void setDays(List<CalendarDay> days) {
        this.days = days;
        updateCalendarGrid();
    }

    public void setOnDayClickListener(OnDayClickListener listener) {
        this.listener = listener;
    }

    private void updateCalendarGrid() {
        gridLayout.removeAllViews();
        
        // Add day headers
        String[] weekDays = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        for (String weekDay : weekDays) {
            TextView header = new TextView(getContext());
            header.setText(weekDay);
            header.setTextAppearance(android.R.style.TextAppearance_Material_Caption);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            header.setLayoutParams(params);
            header.setTextAlignment(TEXT_ALIGNMENT_CENTER);
            gridLayout.addView(header);
        }

        // Add calendar days
        for (final CalendarDay day : days) {
            View dayView = inflate(getContext(), R.layout.item_calendar_day, null);
            TextView dateText = dayView.findViewById(R.id.dateText);
            View selectedIndicator = dayView.findViewById(R.id.selectedIndicator);
            
            dateText.setText(String.valueOf(day.getDayOfMonth()));
            
            // Apply material states
            dayView.setBackgroundResource(R.drawable.calendar_day_background);
            
            if (day.isToday()) {
                dateText.setTextAppearance(android.R.style.TextAppearance_Material_Body1);
                selectedIndicator.setVisibility(View.VISIBLE);
                selectedIndicator.setBackgroundResource(R.drawable.today_circle_background);
            }
            
            dayView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDayClick(day);
                }
                animateSelection(v);
            });

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            dayView.setLayoutParams(params);
            
            gridLayout.addView(dayView);
        }
    }

    private void animateSelection(View newSelection) {
        // Remove previous selection
        if (selectedView != null) {
            selectedView.findViewById(R.id.selectedIndicator).setVisibility(View.INVISIBLE);
        }

        // Animate new selection
        Animation selectionAnim = AnimationUtils.loadAnimation(getContext(), R.anim.calendar_day_selected);
        newSelection.startAnimation(selectionAnim);
        
        View indicator = newSelection.findViewById(R.id.selectedIndicator);
        indicator.setVisibility(View.VISIBLE);
        indicator.setBackgroundResource(R.drawable.selected_circle_background);
        
        selectedView = newSelection;
    }

    public void animateMonthChange(boolean forward) {
        Animation exitAnim = AnimationUtils.loadAnimation(getContext(), 
            forward ? R.anim.slide_out_left : R.anim.slide_out_right);
        Animation enterAnim = AnimationUtils.loadAnimation(getContext(), 
            forward ? R.anim.calendar_month_enter : R.anim.slide_in_left);
        
        exitAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                gridLayout.startAnimation(enterAnim);
                updateCalendarGrid();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
        
        gridLayout.startAnimation(exitAnim);
    }
} 