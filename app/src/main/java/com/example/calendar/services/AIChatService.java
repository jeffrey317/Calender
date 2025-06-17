package com.example.calendar.services;

import android.os.Handler;
import android.os.Looper;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import okhttp3.*;

public class AIChatService {
    private static final String OPENAI_API_ENDPOINT = "https://api.openai.com/v1/chat/completions";
    private final String apiKey;
    private final OkHttpClient client;
    private final String systemPrompt;
    private final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final Handler mainHandler;

    public AIChatService(String apiKey) {
        this.apiKey = apiKey;
        this.client = new OkHttpClient();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.systemPrompt = String.join("\n",
            "You are an AI nutritionist assistant who can help users record and analyze their eating habits.",
            "You can:",
            "1. Provide healthy eating advice",
            "2. Analyze whether a user's meal is balanced",
            "3. Provide improvement advice based on a user's eating habits",
            "4. Answer questions about nutrition and health from users",
            "Please talk to users in a friendly and professional manner."
        );
    }

    public CompletableFuture<String> chat(String userMessage) {
        CompletableFuture<String> future = new CompletableFuture<>();

        String jsonBody = String.format(
            "{\"model\": \"gpt-3.5-turbo\", \"messages\": [{\"role\": \"system\", \"content\": \"%s\"}, {\"role\": \"user\", \"content\": \"%s\"}]}",
            systemPrompt.replace("\"", "\\\""),
            userMessage.replace("\"", "\\\"")
        );

        Request request = new Request.Builder()
            .url(OPENAI_API_ENDPOINT)
            .addHeader("Authorization", "Bearer " + apiKey)
            .post(RequestBody.create(jsonBody, JSON))
            .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> future.completeExceptionally(e));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (!response.isSuccessful() || responseBody == null) {
                        mainHandler.post(() -> future.completeExceptionally(
                            new IOException("Unexpected response " + response)
                        ));
                        return;
                    }

                    String responseData = responseBody.string();
                    // Simple JSON parsing - in production, use proper JSON library
                    String content = extractContentFromResponse(responseData);
                    mainHandler.post(() -> future.complete(content));
                }
            }
        });

        return future;
    }

    private String extractContentFromResponse(String jsonResponse) {
        try {
            // Simple JSON parsing - in production, use proper JSON library like Gson or Jackson
            int contentStart = jsonResponse.indexOf("\"content\":\"") + 11;
            int contentEnd = jsonResponse.indexOf("\"", contentStart);
            if (contentStart > 10 && contentEnd > contentStart) {
                return jsonResponse.substring(contentStart, contentEnd)
                    .replace("\\n", "\n")
                    .replace("\\\"", "\"");
            }
        } catch (Exception e) {
            // Fall back to error message if parsing fails
        }
        return "Sorry, I couldn't process the response. Please try again.";
    }
} 