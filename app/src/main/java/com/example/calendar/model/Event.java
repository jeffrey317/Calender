package com.example.calendar.model;

import java.util.Date;

public class Event {
    private String id;
    private String title;
    private String description;
    private Date date;
    private int colorType; // 0-5 for different event colors
    private boolean isAllDay;

    public Event() {
    }

    public Event(String id, String title, String description, Date date, int colorType, boolean isAllDay) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.date = date;
        this.colorType = colorType;
        this.isAllDay = isAllDay;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public int getColorType() {
        return colorType;
    }

    public void setColorType(int colorType) {
        this.colorType = colorType;
    }

    public boolean isAllDay() {
        return isAllDay;
    }

    public void setAllDay(boolean allDay) {
        isAllDay = allDay;
    }

    public int getEventColor() {
        switch (colorType) {
            case 0: return com.example.calendar.R.color.event_red;
            case 1: return com.example.calendar.R.color.event_green;
            case 2: return com.example.calendar.R.color.event_blue;
            case 3: return com.example.calendar.R.color.event_orange;
            case 4: return com.example.calendar.R.color.event_purple;
            case 5: return com.example.calendar.R.color.event_teal;
            default: return com.example.calendar.R.color.calendar_selected;
        }
    }
} 