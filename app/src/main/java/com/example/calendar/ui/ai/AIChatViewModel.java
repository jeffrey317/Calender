package com.example.calendar.ui.ai;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.calendar.data.Meal;
import com.example.calendar.data.MealDao;
import com.example.calendar.data.AppDatabase;
import com.example.calendar.data.FoodNutritionService;
import com.example.calendar.data.FoodCalories;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Calendar;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AIChatViewModel extends AndroidViewModel {
    private static final String TAG = "AIChatViewModel";
    private final MutableLiveData<List<ChatMessage>> chatMessages = new MutableLiveData<>(new ArrayList<>());
    private final MealDao mealDao;
    private boolean hasShownWelcome = false;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final FoodNutritionService nutritionService;

    public AIChatViewModel(Application application) {
        super(application);
        mealDao = AppDatabase.getDatabase(application).mealDao();
        nutritionService = new FoodNutritionService();
        showWelcomeMessage();
    }

    private void showWelcomeMessage() {
        if (!hasShownWelcome) {
            addAIResponse("Hi, You can inquire about what I ate on the specified day and calories. For example, What are the calories for 2025-06-09 breakfast?\n\nI can also help you learn about nutrition! Try asking: 'What are the nutritional benefits of apples?' or 'nutrition facts for eggs'");
            hasShownWelcome = true;
        }
    }

    public LiveData<List<ChatMessage>> getChatMessages() {
        return chatMessages;
    }

    public void sendMessage(String message) {
        try {
            Log.d(TAG, "Received user message: " + message);
            addUserMessage(message);
            

            
            processUserMessage(message);
        } catch (Exception e) {
            Log.e(TAG, "Error in sendMessage", e);
            addAIResponse("Sorry, an error occurred while processing the message. Please try again later.");
        }
    }

    private void addUserMessage(String message) {
        List<ChatMessage> currentMessages = chatMessages.getValue();
        if (currentMessages == null) {
            currentMessages = new ArrayList<>();
        }
        currentMessages.add(new ChatMessage(message, true));
        chatMessages.setValue(currentMessages);
    }

    private void processUserMessage(String message) {
        String originalMessage = message;
        message = message.toLowerCase();
        
        try {
            Log.d(TAG, "Processing message: " + message);
            
            // Check if this is a nutrition information query first
            if (isNutritionQuery(message)) {
                String foodName = extractFoodNameFromNutritionQuery(originalMessage);
                if (foodName != null && !foodName.isEmpty()) {
                    handleNutritionQuery(foodName);
                    return;
                } else {
                    addAIResponse("Please specify which food you'd like to know about. For example: 'What are the nutritional benefits of apples?'");
                    return;
                }
            }
            
            // Try to extract date from message
            Date queryDate = null;
            boolean isToday = message.contains("today");
            boolean isYesterday = message.contains("yesterday");
            
            // Check for specific date patterns (e.g., "9 June 2025", "June 9 2025", "2025-06-09")
            if (!isToday && !isYesterday) {
                queryDate = extractDateFromMessage(message);
            }
            
            String mealType = null;
            boolean askingCalories = message.contains("calories") || 
                                   message.contains("how many calories");
            
            if (message.contains("breakfast")) {
                mealType = "Breakfast";
            } else if (message.contains("lunch")) {
                mealType = "Lunch";
            } else if (message.contains("dinner")) {
                mealType = "Dinner";
            }
            
            Log.d(TAG, "Detected meal type: " + mealType + ", asking calories: " + askingCalories);
            
            if (mealType != null) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                String dateStr;
                
                if (queryDate != null) {
                    dateStr = dateFormat.format(queryDate);
                } else if (isYesterday) {
                    Calendar calendar = Calendar.getInstance();
                    calendar.add(Calendar.DAY_OF_YEAR, -1);
                    dateStr = dateFormat.format(calendar.getTime());
                } else {
                    dateStr = dateFormat.format(new Date());
                }
                
                Log.d(TAG, "Querying for meal: " + dateStr + ", type: " + mealType);
                
                // Add a debug response to show which date is being queried
                String dateDescription;
                if (queryDate != null) {
                    SimpleDateFormat displayFormat = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
                    dateDescription = displayFormat.format(queryDate);
                } else if (isYesterday) {
                    dateDescription = "yesterday";
                } else {
                    dateDescription = "today";
                }
                
                addAIResponse("🔍 Searching for " + mealType + " on " + dateDescription + "...\n");
                
                queryMealInfo(dateStr, mealType, askingCalories);
                return;
            }
            
            // Check if user wants to see all meals
            if (message.contains("show all meals")) {
                showAllMealsDebug();
                return;
            }
            
            // Default response
            addAIResponse("I can help you with:\n\n" +
                         "📋 Meal Records:\n" +
                         "• 'What did I have for breakfast today?'\n" +
                         "• 'How many calories did I have for lunch today?'\n" +
                         "• 'What did I eat for dinner yesterday?'\n" +
                         "• 'What are the calories for June 9, 2025 lunch?'\n" +
                         "• Type 'show all meals' to see all records\n\n" +
                         "🥗 Nutrition Information:\n" +
                         "• 'What are the nutritional benefits of apples?'\n" +
                         "• 'Nutrition facts for eggs'\n" +
                         "• 'Tell me about banana nutrition'");
            
        } catch (Exception e) {
            Log.e(TAG, "Error in processUserMessage", e);
            addAIResponse("Sorry, an error occurred while processing the message. Please try again later.");
        }
    }

    private Date extractDateFromMessage(String message) {
        try {
            // Try different date patterns
            String[][] patterns = {
                {"\\b(\\d{1,2})\\s*(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)[a-z]*\\s*(\\d{4})\\b", "d MMM yyyy"},
                {"\\b(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)[a-z]*\\s*(\\d{1,2})\\s*(\\d{4})\\b", "MMM d yyyy"},
                {"\\b(\\d{4})[-/](\\d{1,2})[-/](\\d{1,2})\\b", "yyyy-MM-dd"}
            };
            
            for (String[] pattern : patterns) {
                Pattern regex = Pattern.compile(pattern[0], Pattern.CASE_INSENSITIVE);
                Matcher matcher = regex.matcher(message);
                
                if (matcher.find()) {
                    String dateStr = matcher.group().replaceAll("\\s+", " ");
                    SimpleDateFormat format = new SimpleDateFormat(pattern[1], Locale.ENGLISH);
                    return format.parse(dateStr);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing date from message", e);
        }
        return null;
    }

    private void queryMealInfo(String dateStr, String mealType, boolean askingCalories) {
        try {
            Log.d(TAG, "Starting meal query for date: " + dateStr + ", type: " + mealType);
            final String mealTypeEnum = convertMealType(mealType);
            
            // Convert date string to start and end of day
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date date = dateFormat.parse(dateStr);
            
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            
            // Set start time to 00:00:00
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            final Date startDate = calendar.getTime();
            
            // Set end time to 23:59:59
            calendar.set(Calendar.HOUR_OF_DAY, 23);
            calendar.set(Calendar.MINUTE, 59);
            calendar.set(Calendar.SECOND, 59);
            calendar.set(Calendar.MILLISECOND, 999);
            final Date endDate = calendar.getTime();
            
            Log.d(TAG, "Querying meals between " + dateFormat.format(startDate) + " and " + dateFormat.format(endDate));
            
            AppDatabase.databaseWriteExecutor.execute(() -> {
                try {
                    Log.d(TAG, "Executing database query");
                    // Get current user ID
                    com.google.firebase.auth.FirebaseUser currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
                    String userId = currentUser != null ? currentUser.getUid() : "";
                    List<Meal> meals = mealDao.getMealsByDateRange(userId, startDate, endDate);
                    Log.d(TAG, "Query completed. Found " + (meals != null ? meals.size() : 0) + " meals");
                    
                    // Filter meals by type
                    List<Meal> typedMeals = new ArrayList<>();
                    if (meals != null) {
                        for (Meal meal : meals) {
                            if (meal.getMealType().name().equals(mealTypeEnum)) {
                                typedMeals.add(meal);
                            }
                        }
                    }
                    
                    if (!typedMeals.isEmpty()) {
                        // 檢查是否需要更新卡路里
                        boolean needsCalorieUpdate = false;
                        for (Meal meal : typedMeals) {
                            if (meal.getCalories() == 0 && meal.getNotes() != null && !meal.getNotes().isEmpty()) {
                                needsCalorieUpdate = true;
                                break;
                            }
                        }
                        
                        if (needsCalorieUpdate && askingCalories) {
                            // 更新卡路里並返回結果
                            updateMealCaloriesAndRespond(typedMeals, mealType, askingCalories);
                        } else {
                            // 直接返回結果
                            generateMealResponse(typedMeals, mealType, askingCalories);
                        }
                    } else {
                        String response = "No " + mealType + " record found for today. Would you like to add one?\nToday's " + mealType + " hasn't been recorded yet. Would you like to add a record?";
                        mainHandler.post(() -> addAIResponse(response));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error in database query", e);
                    mainHandler.post(() -> addAIResponse("Sorry, an error occurred while retrieving data. Please try again later."));
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error in queryMealInfo", e);
            addAIResponse("Sorry, an error occurred while processing the query. Please try again later.");
        }
    }
    
    private void updateMealCaloriesAndRespond(List<Meal> meals, String mealType, boolean askingCalories) {
        if (meals.isEmpty()) return;
        
        // Get the first meal to update calories (assuming meals in the same meal are recorded together)
        Meal mealToUpdate = meals.get(0);
        String foodContent = mealToUpdate.getNotes();
        
        if (foodContent != null && !foodContent.isEmpty()) {
            // Use FoodCalories class for accurate calorie calculation
            int calories = FoodCalories.getCalories(foodContent);
            
            AppDatabase.databaseWriteExecutor.execute(() -> {
                try {
                    // Update meal calories
                    mealToUpdate.setCalories(calories);
                    mealDao.updateMeal(mealToUpdate);
                    Log.d(TAG, "Updated meal calories to: " + calories);
                    
                    // Get updated meal list
                    List<Meal> updatedMeals = new ArrayList<>(meals);
                    for (int i = 0; i < updatedMeals.size(); i++) {
                        if (updatedMeals.get(i).getId() == mealToUpdate.getId()) {
                            updatedMeals.set(i, mealToUpdate);
                            break;
                        }
                    }
                    
                    mainHandler.post(() -> generateMealResponse(updatedMeals, mealType, askingCalories));
                } catch (Exception e) {
                    Log.e(TAG, "Error updating meal calories", e);
                    mainHandler.post(() -> generateMealResponse(meals, mealType, askingCalories));
                }
            });
        } else {
            generateMealResponse(meals, mealType, askingCalories);
        }
    }
    
    private void generateMealResponse(List<Meal> typedMeals, String mealType, boolean askingCalories) {
        Log.d(TAG, "Generating meal response for " + typedMeals.size() + " meals");
        StringBuilder sb = new StringBuilder();
        
        if (askingCalories) {
            sb.append(mealType).append(" calories:\n\n");
            
            int totalCalories = 0;
            
            for (Meal meal : typedMeals) {
                if (meal.getNotes() != null && !meal.getNotes().isEmpty()) {
                    String[] foods = meal.getNotes().split("、|,");
                    for (String food : foods) {
                        food = food.trim();
                        if (!food.isEmpty()) {
                            // Calculate calories for individual food
                            int individualCalories = getIndividualFoodCalories(food);
                            sb.append("• ").append(food).append(": ").append(individualCalories).append(" calories\n");
                            totalCalories += individualCalories;
                            Log.d(TAG, "Added food: " + food + " with " + individualCalories + " calories");
                        }
                    }
                }
            }
            
            sb.append("\nTotal calories: ").append(totalCalories).append(" calories");
        } else {
            sb.append("Today's ").append(mealType).append(" record:\n\n");
            for (Meal meal : typedMeals) {
                if (meal.getNotes() != null && !meal.getNotes().isEmpty()) {
                    sb.append("Food: ").append(meal.getNotes()).append("\n");
                }
            }
        }
        
        addAIResponse(sb.toString());
    }
    
    private int getIndividualFoodCalories(String foodName) {
        Log.d(TAG, "Calculating calories for food: " + foodName);
        return FoodCalories.getCalories(foodName);
    }

    private void showAllMealsDebug() {
        Log.d(TAG, "Debug: Showing all meals");
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                // Get current user ID
                com.google.firebase.auth.FirebaseUser currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
                String userId = currentUser != null ? currentUser.getUid() : "";
                List<Meal> allMeals = mealDao.getAllMeals(userId);
                Log.d(TAG, "Debug: Found " + (allMeals != null ? allMeals.size() : 0) + " total meals");
                
                StringBuilder sb = new StringBuilder();
                sb.append("🔍 Debug: All Meals Record 所有餐點記錄\n\n");
                
                if (allMeals == null || allMeals.isEmpty()) {
                    sb.append("No meals found in database.\n沒有找到任何餐點記錄。");
                } else {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                    
                    for (int i = 0; i < allMeals.size(); i++) {
                        Meal meal = allMeals.get(i);
                        sb.append(String.format("📋 Record %d:\n", i + 1));
                        sb.append(String.format("Date: %s\n", dateFormat.format(meal.getDate())));
                        sb.append(String.format("Type: %s\n", meal.getMealType().name()));
                        sb.append(String.format("Food: %s\n", meal.getNotes() != null ? meal.getNotes() : "N/A"));
                        sb.append(String.format("Calories: %d\n", meal.getCalories()));
                        sb.append("\n");
                    }
                }
                
                String response = sb.toString();
                mainHandler.post(() -> addAIResponse(response));
                
            } catch (Exception e) {
                Log.e(TAG, "Error in showAllMealsDebug", e);
                mainHandler.post(() -> addAIResponse("Error retrieving meals data for debug.\n獲取餐點數據時發生錯誤。"));
            }
        });
    }

    private String convertMealType(String mealType) {
        switch (mealType) {
            case "Breakfast":
                return "BREAKFAST";
            case "Lunch":
                return "LUNCH";
            case "Dinner":
                return "DINNER";
            default:
                return "OTHER";
        }
    }

    private void addAIResponse(String response) {
        try {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                updateChatMessages(response);
            } else {
                mainHandler.post(() -> updateChatMessages(response));
            }
            Log.d(TAG, "AI response added: " + response);
        } catch (Exception e) {
            Log.e(TAG, "Error adding AI response", e);
        }
    }

    private void updateChatMessages(String response) {
        List<ChatMessage> currentMessages = chatMessages.getValue();
        if (currentMessages == null) {
            currentMessages = new ArrayList<>();
        }
        currentMessages.add(new ChatMessage(response, false));
        chatMessages.setValue(currentMessages);
    }

    private boolean isNutritionQuery(String message) {
        String[] nutritionKeywords = {
            "nutritional benefits",
            "nutrition facts",
            "nutritional value",
            "nutrients in",
            "nutritional information",
            "nutrition information",
            "tell me about",
            "nutritional content",
            "what nutrients",
            "nutrition of",
            "nutritional data"
        };
        
        for (String keyword : nutritionKeywords) {
            if (message.contains(keyword)) {
                return true;
            }
        }
        
        return false;
    }

    private String extractFoodNameFromNutritionQuery(String message) {
        String lowerMessage = message.toLowerCase();
        
        // Common patterns for nutrition queries
        String[] patterns = {
            "nutritional benefits of ",
            "nutrition facts for ",
            "nutritional value of ",
            "nutrients in ",
            "nutritional information for ",
            "nutrition information for ",
            "tell me about ",
            "nutritional content of ",
            "what nutrients are in ",
            "nutrition of ",
            "nutritional data for "
        };
        
        for (String pattern : patterns) {
            int startIndex = lowerMessage.indexOf(pattern);
            if (startIndex != -1) {
                String foodPart = message.substring(startIndex + pattern.length()).trim();
                // Remove common suffixes like "?", ".", "!"
                foodPart = foodPart.replaceAll("[?!.]$", "").trim();
                return foodPart;
            }
        }
        
        return null;
    }

    private void handleNutritionQuery(String foodName) {
        addAIResponse("🔍 Looking up nutrition information for " + foodName + "...");
        
        nutritionService.getDetailedNutritionInfo(foodName, new FoodNutritionService.DetailedNutritionCallback() {
            @Override
            public void onResult(String nutritionInfo) {
                mainHandler.post(() -> {
                    addAIResponse(nutritionInfo);
                });
            }

            @Override
            public void onError(String error) {
                mainHandler.post(() -> {
                    addAIResponse("Sorry, I couldn't find detailed nutrition information for " + foodName + ". Please try with a different food name or check the spelling.");
                });
            }
        });
    }
} 