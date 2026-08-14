package com.example.luminaai.api;

import com.example.luminaai.BuildConfig;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.MediaType;
import java.util.concurrent.TimeUnit;

public class AiApiClient {

    // Lấy Key và URL từ BuildConfig (Cực kỳ bảo mật và chuyên nghiệp)
    private static final String API_KEY = BuildConfig.GROQ_API_KEY;
    private static final String API_URL = BuildConfig.GROQ_URL;

    private static AiApiClient instance;
    private OkHttpClient client;

    private AiApiClient() {
        client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    public static synchronized AiApiClient getInstance() {
        if (instance == null) {
            instance = new AiApiClient();
        }
        return instance;
    }

    // Hàm gọi AI chung, Chat hay Quiz đều dùng được hàm này
    public Request buildAiRequest(String jsonBody) {
        MediaType JSON = MediaType.get("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(jsonBody, JSON);

        return new Request.Builder()
                .url(API_URL)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();
    }

    public OkHttpClient getClient() {
        return client;
    }
}