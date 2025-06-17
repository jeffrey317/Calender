package com.example.calendar.model;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class CalendarDay {
    private int dayNumber;
    private Calendar date;
    private boolean isCurrentMonth;
    private boolean isToday;
    private boolean isSelected;
    private List<Event> events;

    public CalendarDay() {
        this.events = new ArrayList<>();
    }

    public CalendarDay(int dayNumber, Calendar date, boolean isCurrentMonth) {
        this.dayNumber = dayNumber;
        this.date = date;
        this.isCurrentMonth = isCurrentMonth;
        this.events = new ArrayList<>();
        
        // Check if this is today
        Calendar today = Calendar.getInstance();
        Calendar thisDay = Calendar.getInstance();
        thisDay.setTime(date.getTime());
        
        this.isToday = today.get(Calendar.YEAR) == thisDay.get(Calendar.YEAR) &&
                      today.get(Calendar.DAY_OF_YEAR) == thisDay.get(Calendar.DAY_OF_YEAR);
    }

    public int getDayNumber() {
        return dayNumber;
    }

    public void setDayNumber(int dayNumber) {
        this.dayNumber = dayNumber;
    }

    public int getDayOfMonth() {
        return date.get(Calendar.DAY_OF_MONTH);
    }

    public Calendar getDate() {
        return date;
    }

    public boolean isCurrentMonth() {
        return isCurrentMonth;
    }

    public void setCurrentMonth(boolean currentMonth) {
        isCurrentMonth = currentMonth;
    }

    public boolean isToday() {
        return isToday;
    }

    public void setToday(boolean today) {
        isToday = today;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    public List<Event> getEvents() {
        return events;
    }

    public void setEvents(List<Event> events) {
        this.events = events;
    }

    public void addEvent(Event event) {
        this.events.add(event);
    }

    public boolean hasEvents() {
        return events != null && !events.isEmpty();
    }

    public int getEventCount() {
        return events != null ? events.size() : 0;
    }
} 