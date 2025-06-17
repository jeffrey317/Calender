package com.example.calendar.model;

import java.util.Date;

public class CalorieEntry {
    private long id;
    private int calories;
    private String description;
    private Date date;

    public CalorieEntry(int calories, String description) {
        this.calories = calories;
        this.description = description;
        this.date = new Date();
    }

    // Getters and setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getCalories() {
        return calories;
    }

    public void setCalories(int calories) {
        this.calories = calories;
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
} 