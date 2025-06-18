package com.example.calendar.data;

import android.content.Context;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FoodNutritionService {
    private static final String TAG = "FoodNutritionService";
    private static final String API_KEY = "DEMO_KEY"; // 使用DEMO_KEY進行測試，可以免費使用
    private static final String BASE_URL = "https://api.nal.usda.gov/fdc/v1/foods/search";
    
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Map<String, Integer> localCache = new HashMap<>();
    private final Map<String, String> chineseToEnglishMap = new HashMap<>();
    
    public interface NutritionCallback {
        void onResult(int calories);
        void onError(String error);
    }
    
    public interface DetailedNutritionCallback {
        void onResult(String nutritionInfo);
        void onError(String error);
    }
    
    public FoodNutritionService() {
        initializeChineseMapping();
        initializeLocalFoodData();
    }
    
    private void initializeChineseMapping() {
        // 中文到英文食物名稱映射
        chineseToEnglishMap.put("火腿", "ham");
        chineseToEnglishMap.put("火腿三文治", "ham sandwich");
        chineseToEnglishMap.put("三文治", "sandwich");
        chineseToEnglishMap.put("牛奶", "milk");
        chineseToEnglishMap.put("雞蛋", "egg");
        chineseToEnglishMap.put("麵包", "bread");
        chineseToEnglishMap.put("白飯", "white rice");
        chineseToEnglishMap.put("炒飯", "fried rice");
        chineseToEnglishMap.put("炒麵", "fried noodles");
        chineseToEnglishMap.put("雞肉", "chicken");
        chineseToEnglishMap.put("豬肉", "pork");
        chineseToEnglishMap.put("牛肉", "beef");
        chineseToEnglishMap.put("魚", "fish");
        chineseToEnglishMap.put("生菜", "lettuce");
        chineseToEnglishMap.put("番茄", "tomato");
        chineseToEnglishMap.put("可樂", "cola");
        chineseToEnglishMap.put("橙汁", "orange juice");
        chineseToEnglishMap.put("咖啡", "coffee");
        chineseToEnglishMap.put("茶", "tea");
    }
    
    private void initializeLocalFoodData() {
        // 本地緩存常見食物的卡路里數據（備用方案）
        localCache.put("ham sandwich", 350);
        localCache.put("ham", 145);
        localCache.put("milk", 125);
        localCache.put("bread", 265);
        localCache.put("egg", 155);
        localCache.put("white rice", 130);
        localCache.put("fried rice", 200);
        localCache.put("chicken", 165);
        localCache.put("chicken wings", 283);  // USDA accurate value
        localCache.put("chicken breast", 165);
        localCache.put("pork", 242);
        localCache.put("beef", 250);
        localCache.put("fish", 100);
        localCache.put("lettuce", 15);
        localCache.put("tomato", 18);
        localCache.put("cola", 42);
        localCache.put("orange juice", 45);
        localCache.put("coffee", 1);
    }
    
    public void calculateCalories(String foodDescription, NutritionCallback callback) {
        if (foodDescription == null || foodDescription.isEmpty()) {
            callback.onResult(0);
            return;
        }
        
        executor.execute(() -> {
            try {
                int totalCalories = 0;
                String[] foods = foodDescription.split("、");
                
                for (String food : foods) {
                    food = food.trim();
                    int calories = getCaloriesForFood(food);
                    totalCalories += calories;
                    Log.d(TAG, "Food: " + food + ", Calories: " + calories);
                }
                
                Log.d(TAG, "Total calories calculated: " + totalCalories);
                callback.onResult(totalCalories);
                
            } catch (Exception e) {
                Log.e(TAG, "Error calculating calories", e);
                callback.onError("計算卡路里時發生錯誤");
            }
        });
    }
    
    private int getCaloriesForFood(String foodName) {
        String normalizedFoodName = foodName.toLowerCase().trim();
        
        // 1. 暫時禁用本地緩存 - 直接使用API獲取準確數據
        /*
        if (localCache.containsKey(normalizedFoodName)) {
            Log.d(TAG, "Found in local cache: " + normalizedFoodName);
            return localCache.get(normalizedFoodName);
        }
        */
        
        // 2. 直接調用API（因為用戶使用英文）
        try {
            Log.d(TAG, "Calling API for: " + normalizedFoodName);
            int apiResult = fetchCaloriesFromAPI(normalizedFoodName);
            if (apiResult > 0) {
                Log.d(TAG, "API returned: " + apiResult + " calories for " + normalizedFoodName);
                return apiResult;
            }
        } catch (Exception e) {
            Log.e(TAG, "API call failed for " + normalizedFoodName, e);
        }
        
        // 3. 嘗試中文映射（備用方案）
        String englishName = chineseToEnglishMap.get(normalizedFoodName);
        if (englishName == null) {
            // 部分匹配
            for (Map.Entry<String, String> entry : chineseToEnglishMap.entrySet()) {
                if (normalizedFoodName.contains(entry.getKey())) {
                    englishName = entry.getValue();
                    break;
                }
            }
        }
        
        if (englishName != null) {
            try {
                Log.d(TAG, "Trying mapped name: " + englishName);
                int apiResult = fetchCaloriesFromAPI(englishName);
                if (apiResult > 0) {
                    return apiResult;
                }
            } catch (Exception e) {
                Log.e(TAG, "API call failed for mapped name " + englishName, e);
            }
        }
        
        // 4. 默認估算值
        Log.d(TAG, "Using estimate for: " + normalizedFoodName);
        return estimateCalories(foodName);
    }
    
    private int fetchCaloriesFromAPI(String foodName) throws IOException {
        String urlString = BASE_URL + "?query=" + foodName + "&api_key=" + API_KEY + "&pageSize=1";
        
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        
        try {
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                InputStream inputStream = connection.getInputStream();
                String response = readInputStream(inputStream);
                
                try {
                    JSONObject jsonResponse = new JSONObject(response);
                    JSONArray foods = jsonResponse.getJSONArray("foods");
                    
                    if (foods.length() > 0) {
                        JSONObject firstFood = foods.getJSONObject(0);
                        JSONArray nutrients = firstFood.getJSONArray("foodNutrients");
                        
                        for (int i = 0; i < nutrients.length(); i++) {
                            JSONObject nutrient = nutrients.getJSONObject(i);
                            // 查找能量（卡路里）營養素，ID為1008
                            if (nutrient.getInt("nutrientId") == 1008) {
                                double calories = nutrient.getDouble("value");
                                int caloriesPer100g = (int) Math.round(calories);
                                
                                // 緩存結果
                                localCache.put(foodName, caloriesPer100g);
                                
                                return caloriesPer100g;
                            }
                        }
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "JSON parsing error for " + foodName, e);
                }
            }
        } finally {
            connection.disconnect();
        }
        
        return 0;
    }
    
    private String readInputStream(InputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder result = new StringBuilder();
        String line;
        
        while ((line = reader.readLine()) != null) {
            result.append(line);
        }
        
        reader.close();
        return result.toString();
    }
    
    private int estimateCalories(String foodName) {
        String lowerFoodName = foodName.toLowerCase();
        
        // 基於英文食物類型的估算
        if (lowerFoodName.contains("rice") || lowerFoodName.contains("noodle") || lowerFoodName.contains("pasta") || lowerFoodName.contains("bread")) {
            return 150; // 主食類
        } else if (lowerFoodName.contains("chicken") || lowerFoodName.contains("beef") || lowerFoodName.contains("pork") || 
                   lowerFoodName.contains("fish") || lowerFoodName.contains("meat") || lowerFoodName.contains("egg")) {
            return 200; // 蛋白質類
        } else if (lowerFoodName.contains("vegetable") || lowerFoodName.contains("lettuce") || lowerFoodName.contains("tomato") || 
                   lowerFoodName.contains("carrot") || lowerFoodName.contains("broccoli") || lowerFoodName.contains("spinach")) {
            return 25;  // 蔬菜類
        } else if (lowerFoodName.contains("milk") || lowerFoodName.contains("juice") || lowerFoodName.contains("drink")) {
            return 50;  // 飲品類
        } else if (lowerFoodName.contains("apple") || lowerFoodName.contains("banana") || lowerFoodName.contains("orange") || 
                   lowerFoodName.contains("fruit")) {
            return 60;  // 水果類
        } else {
            Log.d(TAG, "No specific category found for: " + foodName + ", using default 150");
            return 150; // 提高默認估算，因為100太低了
        }
        
        // 原來的中文邏輯作為備用
        //if (lowerFoodName.contains("飯") || lowerFoodName.contains("麵")) {
          //  return 150; // 主食類
        //} else if (lowerFoodName.contains("肉") || lowerFoodName.contains("魚") || lowerFoodName.contains("蛋")) {
            //return 200; // 蛋白質類
        //} else if (lowerFoodName.contains("菜") || lowerFoodName.contains("瓜")) {
         //   return 25;  // 蔬菜類
        //} else if (lowerFoodName.contains("奶") || lowerFoodName.contains("汁")) {
          //  return 50;  // 飲品類
        //}
        
       // return 150;
    }

    public void getDetailedNutritionInfo(String foodName, DetailedNutritionCallback callback) {
        if (foodName == null || foodName.isEmpty()) {
            callback.onError("Food name cannot be empty");
            return;
        }

        executor.execute(() -> {
            try {
                // Convert Chinese to English if needed
                String englishName = chineseToEnglishMap.get(foodName.toLowerCase());
                if (englishName == null) {
                    // Try partial matching
                    for (Map.Entry<String, String> entry : chineseToEnglishMap.entrySet()) {
                        if (foodName.toLowerCase().contains(entry.getKey())) {
                            englishName = entry.getValue();
                            break;
                        }
                    }
                }
                
                // Use the English name if found, otherwise use the original name
                String queryName = englishName != null ? englishName : foodName;
                
                String nutritionInfo = fetchDetailedNutritionFromAPI(queryName);
                if (nutritionInfo != null) {
                    callback.onResult(nutritionInfo);
                } else {
                    callback.onError("Sorry, I couldn't find detailed nutrition information for " + foodName);
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error getting detailed nutrition info", e);
                callback.onError("An error occurred while retrieving nutrition information");
            }
        });
    }

    private String fetchDetailedNutritionFromAPI(String foodName) {
        try {
            String urlString = BASE_URL + "?query=" + foodName + "&api_key=" + API_KEY + "&pageSize=1";
            
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            
            try {
                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    InputStream inputStream = connection.getInputStream();
                    String response = readInputStream(inputStream);
                    
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        JSONArray foods = jsonResponse.getJSONArray("foods");
                        
                        if (foods.length() > 0) {
                            JSONObject firstFood = foods.getJSONObject(0);
                            return formatNutritionResponse(firstFood);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "JSON parsing error for detailed nutrition: " + foodName, e);
                    }
                }
            } finally {
                connection.disconnect();
            }
        } catch (IOException e) {
            Log.e(TAG, "API call failed for detailed nutrition: " + foodName, e);
        }
        
        return null;
    }

    private String formatNutritionResponse(JSONObject foodData) {
        try {
            String description = foodData.getString("description");
            JSONArray nutrients = foodData.getJSONArray("foodNutrients");
            
            // Create nutrient map for easy lookup
            Map<String, Double> nutrientMap = new HashMap<>();
            for (int i = 0; i < nutrients.length(); i++) {
                JSONObject nutrient = nutrients.getJSONObject(i);
                String nutrientName = nutrient.getString("nutrientName");
                double value = nutrient.optDouble("value", 0.0);
                nutrientMap.put(nutrientName, value);
            }
            
            StringBuilder response = new StringBuilder();
            response.append("🥗 ").append(description).append("\n");
            response.append("[USDA Data]\n\n");
            
            // Quick Facts - always show basic macronutrients
            response.append("📊 QUICK FACTS:\n");
            response.append("• Calories: ").append(formatNutrientValue(nutrientMap, "Energy", "kcal")).append("\n");
            response.append("• Protein: ").append(formatNutrientValue(nutrientMap, "Protein", "g")).append("\n");
            response.append("• Total Fat: ").append(formatNutrientValue(nutrientMap, "Total lipid (fat)", "g")).append("\n");
            response.append("• Carbohydrates: ").append(formatNutrientValue(nutrientMap, "Carbohydrate, by difference", "g")).append("\n\n");
            
            // Get the most significant nutrients for this food
            String[] significantNutrients = getSignificantNutrients(nutrientMap, description.toLowerCase());
            
            response.append("🔍 KEY NUTRIENTS:\n");
            for (int i = 0; i < significantNutrients.length; i += 2) {
                String nutrientName = significantNutrients[i];
                String unit = significantNutrients[i + 1];
                String value = formatNutrientValue(nutrientMap, nutrientName, unit);
                
                if (!value.equals("N/A")) {
                    String displayName = getDisplayName(nutrientName);
                    response.append("• ").append(displayName).append(": ").append(value).append("\n");
                }
            }
            
            return response.toString();
            
        } catch (JSONException e) {
            Log.e(TAG, "Error formatting nutrition response", e);
            return null;
        }
    }

    private String[] getSignificantNutrients(Map<String, Double> nutrientMap, String foodDescription) {
        // Determine food category and return relevant nutrients
        if (foodDescription.contains("apple") || foodDescription.contains("fruit")) {
            return new String[]{
                "Vitamin C, total ascorbic acid", "mg",
                "Fiber, total dietary", "g",
                "Potassium, K", "mg",
                "Sugars, total including NLEA", "g",
                "Vitamin A, RAE", "mcg",
                "Folate, total", "mcg"
            };
        } else if (foodDescription.contains("egg")) {
            return new String[]{
                "Choline, total", "mg",
                "Vitamin D (D2 + D3)", "mcg",
                "Vitamin B-12", "mcg",
                "Selenium, Se", "mcg",
                "Cholesterol", "mg",
                "Folate, total", "mcg"
            };
        } else if (foodDescription.contains("milk") || foodDescription.contains("dairy")) {
            return new String[]{
                "Calcium, Ca", "mg",
                "Vitamin D (D2 + D3)", "mcg",
                "Vitamin B-12", "mcg",
                "Riboflavin", "mg",
                "Phosphorus, P", "mg",
                "Vitamin A, RAE", "mcg"
            };
        } else if (foodDescription.contains("chicken") || foodDescription.contains("meat")) {
            return new String[]{
                "Niacin", "mg",
                "Vitamin B-6", "mg",
                "Phosphorus, P", "mg",
                "Selenium, Se", "mcg",
                "Vitamin B-12", "mcg",
                "Zinc, Zn", "mg"
            };
        } else if (foodDescription.contains("spinach") || foodDescription.contains("leafy") || foodDescription.contains("green")) {
            return new String[]{
                "Iron, Fe", "mg",
                "Folate, total", "mcg",
                "Vitamin K (phylloquinone)", "mcg",
                "Vitamin A, RAE", "mcg",
                "Vitamin C, total ascorbic acid", "mg",
                "Magnesium, Mg", "mg"
            };
        } else if (foodDescription.contains("banana")) {
            return new String[]{
                "Potassium, K", "mg",
                "Vitamin B-6", "mg",
                "Vitamin C, total ascorbic acid", "mg",
                "Fiber, total dietary", "g",
                "Sugars, total including NLEA", "g",
                "Magnesium, Mg", "mg"
            };
        } else if (foodDescription.contains("salmon") || foodDescription.contains("fish")) {
            return new String[]{
                "Vitamin D (D2 + D3)", "mcg",
                "Vitamin B-12", "mcg",
                "Selenium, Se", "mcg",
                "Niacin", "mg",
                "Phosphorus, P", "mg",
                "Vitamin B-6", "mg"
            };
        } else if (foodDescription.contains("cheese")) {
            return new String[]{
                "Calcium, Ca", "mg",
                "Vitamin A, RAE", "mcg",
                "Vitamin B-12", "mcg",
                "Phosphorus, P", "mg",
                "Zinc, Zn", "mg",
                "Riboflavin", "mg"
            };
        } else if (foodDescription.contains("bread") || foodDescription.contains("wheat") || foodDescription.contains("grain")) {
            return new String[]{
                "Thiamin", "mg",
                "Niacin", "mg",
                "Folate, total", "mcg",
                "Iron, Fe", "mg",
                "Fiber, total dietary", "g",
                "Manganese, Mn", "mg"
            };
        } else {
            // Default nutrients for unknown foods - show the most common important ones
            return new String[]{
                "Vitamin C, total ascorbic acid", "mg",
                "Iron, Fe", "mg",
                "Calcium, Ca", "mg",
                "Potassium, K", "mg",
                "Vitamin A, RAE", "mcg",
                "Fiber, total dietary", "g"
            };
        }
    }

    private String getDisplayName(String nutrientName) {
        switch (nutrientName) {
            case "Vitamin C, total ascorbic acid": return "Vitamin C";
            case "Fiber, total dietary": return "Dietary Fiber";
            case "Potassium, K": return "Potassium";
            case "Sugars, total including NLEA": return "Total Sugars";
            case "Vitamin A, RAE": return "Vitamin A";
            case "Folate, total": return "Folate";
            case "Choline, total": return "Choline";
            case "Vitamin D (D2 + D3)": return "Vitamin D";
            case "Vitamin B-12": return "Vitamin B12";
            case "Selenium, Se": return "Selenium";
            case "Cholesterol": return "Cholesterol";
            case "Calcium, Ca": return "Calcium";
            case "Riboflavin": return "Vitamin B2 (Riboflavin)";
            case "Phosphorus, P": return "Phosphorus";
            case "Niacin": return "Vitamin B3 (Niacin)";
            case "Vitamin B-6": return "Vitamin B6";
            case "Zinc, Zn": return "Zinc";
            case "Iron, Fe": return "Iron";
            case "Vitamin K (phylloquinone)": return "Vitamin K";
            case "Magnesium, Mg": return "Magnesium";
            case "Thiamin": return "Vitamin B1 (Thiamin)";
            case "Manganese, Mn": return "Manganese";
            default: return nutrientName;
        }
    }

    private String formatNutrientValue(Map<String, Double> nutrientMap, String nutrientName, String unit) {
        Double value = nutrientMap.get(nutrientName);
        if (value == null || value == 0.0) {
            return "N/A";
        }
        
        // Format to 2 decimal places, but remove trailing zeros
        String formatted = String.format("%.2f", value);
        formatted = formatted.replaceAll("0*$", "").replaceAll("\\.$", "");
        return formatted + " " + unit;
    }
} 