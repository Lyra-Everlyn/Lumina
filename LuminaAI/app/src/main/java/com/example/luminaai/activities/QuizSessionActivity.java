package com.example.luminaai.activities;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.luminaai.R;
import com.example.luminaai.api.AiApiClient;
import com.example.luminaai.models.QuizQuestion;
import com.example.luminaai.repository.QuizRepository;
import com.google.android.material.button.MaterialButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Request;
import okhttp3.Response;

public class QuizSessionActivity extends AppCompatActivity {

    private TextView tvQuizSessionTopic, tvQuizProgress, tvQuestionContent;
    private ImageButton btnBackToQuizHome, btnBookmarkQuestion;
    private RadioGroup rgAnswers;
    private RadioButton rbOptionA, rbOptionB, rbOptionC, rbOptionD;
    private MaterialButton btnPreviousQuestion, btnNextQuestion;

    private String topic, difficulty;
    private int currentUserId;
    private List<QuizQuestion> questionList = new ArrayList<>();
    private int currentIndex = 0;

    // Biến cờ khóa giúp ngăn chặn việc listener kích hoạt nhầm khi hệ thống tự động load giao diện
    private boolean isSettingValues = false;

    private QuizRepository quizRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz_session);

        // Ánh xạ Views
        tvQuizSessionTopic = findViewById(R.id.tvQuizSessionTopic);
        tvQuizProgress = findViewById(R.id.tvQuizProgress);
        tvQuestionContent = findViewById(R.id.tvQuestionContent);
        btnBackToQuizHome = findViewById(R.id.btnBackToQuizHome);
        btnBookmarkQuestion = findViewById(R.id.btnBookmarkQuestion);
        rgAnswers = findViewById(R.id.rgAnswers);
        rbOptionA = findViewById(R.id.rbOptionA);
        rbOptionB = findViewById(R.id.rbOptionB);
        rbOptionC = findViewById(R.id.rbOptionC);
        rbOptionD = findViewById(R.id.rbOptionD);
        btnPreviousQuestion = findViewById(R.id.btnPreviousQuestion);
        btnNextQuestion = findViewById(R.id.btnNextQuestion);

        quizRepository = new QuizRepository(this);

        // Nhận Intent
        if (getIntent() != null) {
            topic = getIntent().getStringExtra("QUIZ_TOPIC");
            difficulty = getIntent().getStringExtra("QUIZ_DIFFICULTY");
            currentUserId = getIntent().getIntExtra("CURRENT_USER_ID", 1);

            if (topic != null) {
                tvQuizSessionTopic.setText(topic + " (" + difficulty + ")");
            }
        }

        btnBackToQuizHome.setOnClickListener(v -> finish());

        // Gọi AI tạo câu hỏi
        fetchQuizQuestionsFromAI();

        // Xử lý nút Next / Submit
        btnNextQuestion.setOnClickListener(v -> handleNextQuestion());

        // Xử lý nút Previous
        btnPreviousQuestion.setOnClickListener(v -> handlePreviousQuestion());

        // Xử lý nút Bookmark câu hỏi hiện tại
        btnBookmarkQuestion.setOnClickListener(v -> {
            if (!questionList.isEmpty()) {
                QuizQuestion currentQ = questionList.get(currentIndex);
                boolean newState = !currentQ.isBookmarked();
                currentQ.setBookmarked(newState);
                updateBookmarkIcon(newState);
                Toast.makeText(this, newState ? "Question bookmarked!" : "Removed bookmark", Toast.LENGTH_SHORT).show();
            }
        });

        // Lắng nghe lựa chọn đáp án của user (có kiểm tra biến cờ isSettingValues)
        rgAnswers.setOnCheckedChangeListener((group, checkedId) -> {
            if (isSettingValues || questionList.isEmpty()) return;

            QuizQuestion currentQ = questionList.get(currentIndex);
            if (checkedId == R.id.rbOptionA) currentQ.setSelectedAnswer("A");
            else if (checkedId == R.id.rbOptionB) currentQ.setSelectedAnswer("B");
            else if (checkedId == R.id.rbOptionC) currentQ.setSelectedAnswer("C");
            else if (checkedId == R.id.rbOptionD) currentQ.setSelectedAnswer("D");
            else if (checkedId == -1) currentQ.setSelectedAnswer("");
        });
    }

    private void fetchQuizQuestionsFromAI() {
        tvQuestionContent.setText("AI is generating your quiz questions, please wait...");
        btnNextQuestion.setEnabled(false);
        btnPreviousQuestion.setEnabled(false);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            String aiRawResponse = "";
            try {
                JSONObject jsonBody = new JSONObject();
                jsonBody.put("model", "llama-3.3-70b-versatile");

                JSONArray messagesArray = new JSONArray();
                JSONObject systemObj = new JSONObject();
                systemObj.put("role", "system");
                systemObj.put("content", "You are an educational assistant. Generate 5 multiple-choice questions for the topic: '" + topic + "' with '" + difficulty + "' difficulty.\n" +
                        "YOU MUST RETURN ONLY A VALID JSON ARRAY. No markdown code blocks, no introductory text.\n" +
                        "JSON structure format:\n" +
                        "[\n" +
                        "  {\n" +
                        "    \"question\": \"Question text here?\",\n" +
                        "    \"optionA\": \"First option\",\n" +
                        "    \"optionB\": \"Second option\",\n" +
                        "    \"optionC\": \"Third option\",\n" +
                        "    \"optionD\": \"Fourth option\",\n" +
                        "    \"correct\": \"A\"\n" +
                        "  }\n" +
                        "]");
                messagesArray.put(systemObj);
                jsonBody.put("messages", messagesArray);

                AiApiClient apiClient = AiApiClient.getInstance();
                Request request = apiClient.buildAiRequest(jsonBody.toString());

                try (Response response = apiClient.getClient().newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String resStr = response.body().string();
                        JSONObject jsonRes = new JSONObject(resStr);
                        aiRawResponse = jsonRes.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            final String finalResponse = aiRawResponse;
            handler.post(() -> {
                parseAndLoadQuestions(finalResponse);
            });
        });
    }

    private void parseAndLoadQuestions(String jsonText) {
        try {
            String cleanJson = jsonText.replaceAll("```json", "").replaceAll("```", "").trim();
            JSONArray jsonArray = new JSONArray(cleanJson);

            questionList.clear();
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                QuizQuestion q = new QuizQuestion(
                        obj.getString("question"),
                        obj.getString("optionA"),
                        obj.getString("optionB"),
                        obj.getString("optionC"),
                        obj.getString("optionD"),
                        obj.getString("correct").toUpperCase()
                );
                questionList.add(q);
            }

            if (!questionList.isEmpty()) {
                currentIndex = 0;
                displayQuestion(currentIndex);
                btnNextQuestion.setEnabled(true);
                btnPreviousQuestion.setEnabled(true);
            } else {
                Toast.makeText(this, "Failed to parse questions. Try again.", Toast.LENGTH_SHORT).show();
                finish();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error parsing AI response. Please retry.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void displayQuestion(int index) {
        if (questionList.isEmpty()) return;

        isSettingValues = true;

        QuizQuestion q = questionList.get(index);
        tvQuizProgress.setText((index + 1) + " / " + questionList.size());
        tvQuestionContent.setText(q.getQuestionText());

        rbOptionA.setText("A. " + q.getOptionA());
        rbOptionB.setText("B. " + q.getOptionB());
        rbOptionC.setText("C. " + q.getOptionC());
        rbOptionD.setText("D. " + q.getOptionD());

        // Xóa sạch check cũ để tránh lưu dính dữ liệu câu trước
        rgAnswers.clearCheck();

        // Khôi phục lại đáp án đã chọn của câu hỏi này (nếu có)
        if (q.getSelectedAnswer().equals("A")) {
            rbOptionA.setChecked(true);
        } else if (q.getSelectedAnswer().equals("B")) {
            rbOptionB.setChecked(true);
        } else if (q.getSelectedAnswer().equals("C")) {
            rbOptionC.setChecked(true);
        } else if (q.getSelectedAnswer().equals("D")) {
            rbOptionD.setChecked(true);
        }

        updateBookmarkIcon(q.isBookmarked());

        if (index == questionList.size() - 1) {
            btnNextQuestion.setText("Submit Quiz");
        } else {
            btnNextQuestion.setText("Next");
        }

        // Tắt cờ khóa sau khi hoàn tất thiết lập giao diện
        isSettingValues = false;
    }

    private void handleNextQuestion() {
        if (currentIndex < questionList.size() - 1) {
            currentIndex++;
            displayQuestion(currentIndex);
        } else {
            finishQuizSession();
        }
    }

    private void handlePreviousQuestion() {
        if (currentIndex > 0) {
            currentIndex--;
            displayQuestion(currentIndex);
        }
    }

    private void updateBookmarkIcon(boolean isBookmarked) {
        if (isBookmarked) {
            btnBookmarkQuestion.setImageResource(R.drawable.ic_bookmark);
        } else {
            btnBookmarkQuestion.setImageResource(R.drawable.ic_bookmark_outline);
        }
    }

    private void finishQuizSession() {
        int score = 0;
        for (QuizQuestion q : questionList) {
            if (q.getSelectedAnswer().equals(q.getCorrectAnswer())) {
                score++;
            }
        }

        int total = questionList.size();

        quizRepository.saveQuizSession(currentUserId, topic, difficulty, score, total, questionList);
        Toast.makeText(this, "Quiz Completed! Your Score: " + score + "/" + total, Toast.LENGTH_LONG).show();
        finish();
    }
}