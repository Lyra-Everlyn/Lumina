package com.example.luminaai.activities;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.luminaai.R;
import com.example.luminaai.api.AiApiClient;
import com.example.luminaai.repository.QuizRepository;
import com.google.android.material.button.MaterialButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Request;
import okhttp3.Response;

public class QuizResultActivity extends AppCompatActivity {

    private TextView tvFinalScore, tvAiFeedback;
    private MaterialButton btnReturnToDashboard;

    private int score, total;
    private String topic, difficulty, wrongAnswersJson;

    private boolean isReviewMode = false;
    private long reviewSessionId = -1;

    private QuizRepository quizRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz_result);

        tvFinalScore = findViewById(R.id.tvFinalScore);
        tvAiFeedback = findViewById(R.id.tvAiFeedback);
        btnReturnToDashboard = findViewById(R.id.btnReturnToDashboard);

        quizRepository = new QuizRepository(this);

        // Lấy dữ liệu từ Intent
        if (getIntent() != null) {
            score = getIntent().getIntExtra("SCORE", 0);
            total = getIntent().getIntExtra("TOTAL", 5);
            topic = getIntent().getStringExtra("TOPIC");
            difficulty = getIntent().getStringExtra("DIFFICULTY");
            wrongAnswersJson = getIntent().getStringExtra("WRONG_ANSWERS");

            isReviewMode = getIntent().getBooleanExtra("IS_REVIEW_MODE", false);
            reviewSessionId = getIntent().getLongExtra("REVIEW_SESSION_ID", -1);

            tvFinalScore.setText(score + " / " + total);
        }

        btnReturnToDashboard.setOnClickListener(v -> finish());

        // Phân nhánh logic cho Feedback
        if (isReviewMode && reviewSessionId != -1) {
            // Load lại nhận xét từ SQLite
            String savedFeedback = quizRepository.getAiFeedback(reviewSessionId);
            if (savedFeedback != null && !savedFeedback.isEmpty()) {
                tvAiFeedback.setText(savedFeedback);
            } else {
                tvAiFeedback.setText("No AI feedback available for this session.");
            }
        } else {
            // Gọi AI phân tích và truyền sessionId để lưu lại
            generateAiFeedback(reviewSessionId);
        }
    }

    private void generateAiFeedback(long sessionId) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            String aiResponse = "";
            try {
                // Tạo câu Prompt phân tích
                String promptText = "The student just completed a quiz on '" + topic + "' (" + difficulty + ") and scored " + score + "/" + total + ".\n";
                if (score == total) {
                    promptText += "They got a perfect score! Give them a short, encouraging congratulation and a tip for advanced study.";
                } else {
                    promptText += "Here are the questions they answered incorrectly in JSON format: " + wrongAnswersJson + "\n";
                    promptText += "Provide a short, constructive, and encouraging feedback paragraph (max 3-4 sentences). Focus on explaining the core concepts they misunderstood based on their mistakes. DO NOT list the questions, just summarize the concepts they need to review.";
                }

                JSONObject jsonBody = new JSONObject();
                jsonBody.put("model", "llama-3.3-70b-versatile");

                JSONArray messagesArray = new JSONArray();
                JSONObject systemObj = new JSONObject();
                systemObj.put("role", "system");
                systemObj.put("content", "You are an empathetic and expert educational tutor. Respond directly to the student.");
                messagesArray.put(systemObj);

                JSONObject userObj = new JSONObject();
                userObj.put("role", "user");
                userObj.put("content", promptText);
                messagesArray.put(userObj);

                jsonBody.put("messages", messagesArray);

                AiApiClient apiClient = AiApiClient.getInstance();
                Request request = apiClient.buildAiRequest(jsonBody.toString());

                try (Response response = apiClient.getClient().newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String resStr = response.body().string();
                        JSONObject jsonRes = new JSONObject(resStr);
                        aiResponse = jsonRes.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                aiResponse = "Oops, I couldn't analyze your results right now. Keep practicing!";
            }

            final String finalResponse = aiResponse;
            handler.post(() -> {
                // Hiển thị lên giao diện
                tvAiFeedback.setText(finalResponse);

                // Lưu nhận xét này vào Database để sau này còn xem lại
                if (sessionId != -1) {
                    quizRepository.updateAiFeedback(sessionId, finalResponse);
                }
            });
        });
    }
}