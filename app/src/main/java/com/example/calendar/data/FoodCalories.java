package com.example.calendar.data;

import java.util.HashMap;
import java.util.Map;

public class FoodCalories {
    private static final Map<String, Integer> caloriesMap = new HashMap<>();
    
    static {
        // Breakfast foods
        caloriesMap.put("egg sandwich", 320);
        caloriesMap.put("ham sandwich", 350);
        caloriesMap.put("sandwich", 300);
        caloriesMap.put("egg", 155);
        caloriesMap.put("ham", 145);
        caloriesMap.put("bread", 265);
        caloriesMap.put("milk", 125);
        caloriesMap.put("coffee", 5);
        caloriesMap.put("tea", 2);
        caloriesMap.put("toast", 280);
        caloriesMap.put("cereal", 150);
        caloriesMap.put("oatmeal", 150);
        caloriesMap.put("bacon", 540);
        caloriesMap.put("sausage", 300);
        caloriesMap.put("pancake", 220);
        caloriesMap.put("waffle", 250);
        
        // Main dishes
        caloriesMap.put("rice", 130);
        caloriesMap.put("fried rice", 200);
        caloriesMap.put("noodles", 250);
        caloriesMap.put("pasta", 220);
        caloriesMap.put("pizza", 285);
        caloriesMap.put("burger", 540);
        caloriesMap.put("chicken rice", 480);
        caloriesMap.put("beef curry", 550);
        caloriesMap.put("curry beef rice", 680);
        caloriesMap.put("baked pork chop rice", 422);
        caloriesMap.put("curry", 450);
        caloriesMap.put("salad", 150);
        
        // Proteins
        caloriesMap.put("chicken", 165);
        caloriesMap.put("chicken wings", 283);  // Specific USDA value for chicken wings
        caloriesMap.put("chicken breast", 165);  // Specify this is for chicken breast
        caloriesMap.put("pork", 242);
        caloriesMap.put("pork chop", 500);
        caloriesMap.put("beef", 250);
        caloriesMap.put("fish", 100);
        caloriesMap.put("salmon", 208);
        caloriesMap.put("tuna", 132);
        caloriesMap.put("shrimp", 99);
        
        // Common vegetables (English)
        caloriesMap.put("lettuce", 15);
        caloriesMap.put("tomato", 18);
        caloriesMap.put("onion", 40);
        caloriesMap.put("carrot", 41);
        caloriesMap.put("broccoli", 25);
        caloriesMap.put("spinach", 23);
        
        // Common fruits (English)
        caloriesMap.put("apple", 52);     // 100g
        caloriesMap.put("banana", 89);    // 100g
        caloriesMap.put("orange", 47);    // 100g
        caloriesMap.put("grapes", 62);    // 100g
        caloriesMap.put("strawberry", 32); // 100g
    
        
        // Common beverages (English)
        caloriesMap.put("coke", 42);
        caloriesMap.put("cola", 42);
        caloriesMap.put("orange juice", 45);
        caloriesMap.put("juice", 45);
        caloriesMap.put("water", 0);
        caloriesMap.put("soda", 42);
        
        // Milkshakes and smoothies
        caloriesMap.put("milkshake", 340);  // Base milkshake
        caloriesMap.put("banana milkshake", 380);  // Banana adds ~40 calories
        caloriesMap.put("chocolate milkshake", 420);
        caloriesMap.put("strawberry milkshake", 360);
        caloriesMap.put("vanilla milkshake", 340);
        
        // Add foods that were missing - showing 333 calories per 100g in AI chat
        // Please replace "unknown_food" with the actual food name you entered
        // caloriesMap.put("your_food_name_here", 333);
    }
    
    public static int getCalories(String food) {
        // Convert to lowercase and remove extra spaces
        String normalizedFood = food.toLowerCase().trim();
        
        // Try exact match first
        if (caloriesMap.containsKey(normalizedFood)) {
            return caloriesMap.get(normalizedFood);
        }
        
        // Try matching parts of the food name - prioritize longer matches
        String[] foodParts = normalizedFood.split("\\s+");
        String bestMatch = null;
        int longestMatchLength = 0;
        
        for (String key : caloriesMap.keySet()) {
            boolean allPartsMatch = true;
            for (String part : foodParts) {
                if (!key.contains(part)) {
                    allPartsMatch = false;
                    break;
                }
            }
            if (allPartsMatch && key.length() > longestMatchLength) {
                bestMatch = key;
                longestMatchLength = key.length();
            }
        }
        
        if (bestMatch != null) {
            return caloriesMap.get(bestMatch);
        }
        
        return 0;
    }
} 