package com.example.calendar.ui.calendar;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import com.example.calendar.R;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class CalendarCardWidget extends LinearLayout {
    private GridLayout calendarGrid;
    private TextView monthYearText;
    private ImageButton prevMonthBtn;
    private ImageButton nextMonthBtn;
    
    private Calendar currentDate;
    private Calendar selectedDate;
    private OnDateSelectedListener onDateSelectedListener;
    
    private SimpleDateFormat monthYearFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
    
    public interface OnDateSelectedListener {
        void onDateSelected(Date date);
    }
    
    public CalendarCardWidget(Context context) {
        super(context);
        init();
    }
    
    public CalendarCardWidget(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    public CalendarCardWidget(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    
    private void init() {
        LayoutInflater.from(getContext()).inflate(R.layout.widget_calendar_card, this, true);
        
        calendarGrid = findViewById(R.id.calendarGrid);
        monthYearText = findViewById(R.id.monthYearText);
        prevMonthBtn = findViewById(R.id.prevMonthBtn);
        nextMonthBtn = findViewById(R.id.nextMonthBtn);
        
        // Initialize with today's date
        currentDate = Calendar.getInstance();
        selectedDate = Calendar.getInstance();
        
        setupClickListeners();
        updateCalendar();
    }
    
    private void setupClickListeners() {
        prevMonthBtn.setOnClickListener(v -> {
            currentDate.add(Calendar.MONTH, -1);
            updateCalendar();
        });
        
        nextMonthBtn.setOnClickListener(v -> {
            currentDate.add(Calendar.MONTH, 1);
            updateCalendar();
        });
    }
    
    private void updateCalendar() {
        // Update month/year text
        monthYearText.setText(monthYearFormat.format(currentDate.getTime()).toUpperCase());
        
        // Clear existing views
        calendarGrid.removeAllViews();
        
        // Get calendar info
        Calendar firstDayOfMonth = (Calendar) currentDate.clone();
        firstDayOfMonth.set(Calendar.DAY_OF_MONTH, 1);
        int firstDayOfWeek = firstDayOfMonth.get(Calendar.DAY_OF_WEEK);
        int daysInMonth = currentDate.getActualMaximum(Calendar.DAY_OF_MONTH);
        
        // Add empty cells for days before first day of month
        for (int i = 1; i < firstDayOfWeek; i++) {
            calendarGrid.addView(createDayView(0, false, false, false));
        }
        
        // Add day views
        Calendar today = Calendar.getInstance();
        for (int day = 1; day <= daysInMonth; day++) {
            boolean isToday = isSameDay(currentDate, today, day);
            boolean isSelected = isSameDay(currentDate, selectedDate, day);
            boolean isCurrentMonth = true;
            
            calendarGrid.addView(createDayView(day, isCurrentMonth, isToday, isSelected));
        }
        
        // Add empty cells to fill remaining grid
        int totalCells = 42; // 6 rows * 7 days
        int usedCells = (firstDayOfWeek - 1) + daysInMonth;
        for (int i = usedCells; i < totalCells; i++) {
            calendarGrid.addView(createDayView(0, false, false, false));
        }
    }
    
    private boolean isSameDay(Calendar cal1, Calendar cal2, int dayOfMonth) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
               dayOfMonth == cal2.get(Calendar.DAY_OF_MONTH);
    }
    
    private TextView createDayView(int day, boolean isCurrentMonth, boolean isToday, boolean isSelected) {
        TextView dayView = new TextView(getContext());
        
        // Set layout params
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = dpToPx(40);
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        params.setMargins(dpToPx(2), dpToPx(2), dpToPx(2), dpToPx(2));
        dayView.setLayoutParams(params);
        
        // Set day text
        if (day > 0) {
            dayView.setText(String.valueOf(day));
        }
        
        // Style the view
        dayView.setGravity(Gravity.CENTER);
        dayView.setTextSize(14f);
        dayView.setClickable(day > 0);
        dayView.setFocusable(day > 0);
        
        if (day > 0) {
            // Set text color based on state
            if (isCurrentMonth) {
                dayView.setTextColor(Color.parseColor("#374151"));
            } else {
                dayView.setTextColor(Color.parseColor("#9CA3AF"));
            }
            
            // Set background based on state
            if (isSelected) {
                dayView.setBackgroundResource(R.drawable.selected_day_background);
                dayView.setTextColor(Color.WHITE);
                dayView.setTypeface(dayView.getTypeface(), Typeface.BOLD);
            } else if (isToday) {
                dayView.setBackgroundResource(R.drawable.today_day_background);
                dayView.setTypeface(dayView.getTypeface(), Typeface.BOLD);
            } else {
                dayView.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.calendar_day_background));
            }
            
            // Set click listener
            final int selectedDay = day;
            dayView.setOnClickListener(v -> {
                selectedDate.set(Calendar.YEAR, currentDate.get(Calendar.YEAR));
                selectedDate.set(Calendar.MONTH, currentDate.get(Calendar.MONTH));
                selectedDate.set(Calendar.DAY_OF_MONTH, selectedDay);
                
                if (onDateSelectedListener != null) {
                    onDateSelectedListener.onDateSelected(selectedDate.getTime());
                }
                
                updateCalendar(); // Refresh to show selection
            });
        } else {
            // Empty cell
            dayView.setTextColor(Color.TRANSPARENT);
            dayView.setClickable(false);
            dayView.setFocusable(false);
        }
        
        return dayView;
    }
    
    private int dpToPx(int dp) {
        float density = getContext().getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
    
    public void setOnDateSelectedListener(OnDateSelectedListener listener) {
        this.onDateSelectedListener = listener;
    }
    
    public void setCurrentDate(Date date) {
        currentDate.setTime(date);
        selectedDate.setTime(date);
        updateCalendar();
    }
    
    public Date getSelectedDate() {
        return selectedDate.getTime();
    }
    
    public Date getCurrentDate() {
        return currentDate.getTime();
    }
} 