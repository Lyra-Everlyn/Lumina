package com.example.luminaai.activities;

import android.content.Intent;
import android.graphics.Color;
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
import androidx.core.content.ContextCompat;

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

    private boolean isSettingValues = false;

    // Các biến phục vụ chế độ Review Mode
    private boolean isReviewMode = false;
    private long reviewSessionId = -1;

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

            isReviewMode = getIntent().getBooleanExtra("IS_REVIEW_MODE", false);
            reviewSessionId = getIntent().getLongExtra("REVIEW_SESSION_ID", -1);

            if (topic != null) {
                String titleSuffix = isReviewMode ? " (Review)" : " (" + difficulty + ")";
                tvQuizSessionTopic.setText(topic + titleSuffix);
            }
        }

        btnBackToQuizHome.setOnClickListener(v -> finish());

        // Phân nhánh logic: Làm bài mới (Gọi AI) hoặc Xem lại bài cũ (Load SQLite)
        if (isReviewMode && reviewSessionId != -1) {
            loadQuizForReview();
        } else {
            fetchQuizQuestionsFromAI();
        }

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

        // Lắng nghe lựa chọn đáp án của user
        rgAnswers.setOnCheckedChangeListener((group, checkedId) -> {
            if (isSettingValues || questionList.isEmpty() || isReviewMode) return; // Chặn đổi đáp án nếu là Review Mode

            QuizQuestion currentQ = questionList.get(currentIndex);
            if (checkedId == R.id.rbOptionA) currentQ.setSelectedAnswer("A");
            else if (checkedId == R.id.rbOptionB) currentQ.setSelectedAnswer("B");
            else if (checkedId == R.id.rbOptionC) currentQ.setSelectedAnswer("C");
            else if (checkedId == R.id.rbOptionD) currentQ.setSelectedAnswer("D");
            else if (checkedId == -1) currentQ.setSelectedAnswer("");
        });
    }

    private void loadQuizForReview() {
        // Load danh sách câu hỏi từ SQLite
        questionList = quizRepository.getQuestionsBySessionId(reviewSessionId);

        // Khóa không cho người dùng click chọn lại đáp án
        rbOptionA.setClickable(false);
        rbOptionB.setClickable(false);
        rbOptionC.setClickable(false);
        rbOptionD.setClickable(false);

        if (!questionList.isEmpty()) {
            currentIndex = 0;
            displayQuestion(currentIndex);
        } else {
            Toast.makeText(this, "Could not load quiz details.", Toast.LENGTH_SHORT).show();
            finish();
        }
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

        // Reset text color về màu mặc định trước khi tô màu
        int defaultColor = ContextCompat.getColor(this, R.color.text_dark);
        rbOptionA.setTextColor(defaultColor);
        rbOptionB.setTextColor(defaultColor);
        rbOptionC.setTextColor(defaultColor);
        rbOptionD.setTextColor(defaultColor);

        rgAnswers.clearCheck();

        // Khôi phục lại đáp án đã chọn
        String selected = q.getSelectedAnswer() != null ? q.getSelectedAnswer() : "";
        if (selected.equals("A")) rbOptionA.setChecked(true);
        else if (selected.equals("B")) rbOptionB.setChecked(true);
        else if (selected.equals("C")) rbOptionC.setChecked(true);
        else if (selected.equals("D")) rbOptionD.setChecked(true);

        // Nếu là chế độ xem lại, tiến hành tô màu đáp án
        if (isReviewMode) {
            String correct = q.getCorrectAnswer();
            int colorCorrect = Color.parseColor("#4CAF50"); // Xanh lá
            int colorWrong = Color.parseColor("#F44336");   // Đỏ

            // 1. Tô xanh lá cho đáp án đúng
            if (correct.equals("A")) rbOptionA.setTextColor(colorCorrect);
            else if (correct.equals("B")) rbOptionB.setTextColor(colorCorrect);
            else if (correct.equals("C")) rbOptionC.setTextColor(colorCorrect);
            else if (correct.equals("D")) rbOptionD.setTextColor(colorCorrect);

            // 2. Tô đỏ nếu đáp án user chọn bị sai
            if (!selected.isEmpty() && !selected.equals(correct)) {
                if (selected.equals("A")) rbOptionA.setTextColor(colorWrong);
                else if (selected.equals("B")) rbOptionB.setTextColor(colorWrong);
                else if (selected.equals("C")) rbOptionC.setTextColor(colorWrong);
                else if (selected.equals("D")) rbOptionD.setTextColor(colorWrong);
            }
        }

        updateBookmarkIcon(q.isBookmarked());

        if (index == questionList.size() - 1) {
            btnNextQuestion.setText(isReviewMode ? "View Feedback" : "Submit Quiz");
        } else {
            btnNextQuestion.setText("Next");
        }

        isSettingValues = false;
    }

    private void handleNextQuestion() {
        // Validation: Nếu KHÔNG PHẢI review mode thì bắt buộc chọn đáp án
        if (!isReviewMode) {
            QuizQuestion currentQ = questionList.get(currentIndex);
            if (currentQ.getSelectedAnswer() == null || currentQ.getSelectedAnswer().isEmpty()) {
                Toast.makeText(this, "Please select an answer before proceeding!", Toast.LENGTH_SHORT).show();
                return;
            }
        }

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
        int total = questionList.size();
        JSONArray wrongAnswersArray = new JSONArray();

        // Tính điểm và gom câu sai
        for (QuizQuestion q : questionList) {
            if (q.getSelectedAnswer().equals(q.getCorrectAnswer())) {
                score++;
            } else {
                try {
                    JSONObject wrongObj = new JSONObject();
                    wrongObj.put("question", q.getQuestionText());
                    wrongObj.put("userChose", q.getSelectedAnswer());
                    wrongObj.put("correctIs", q.getCorrectAnswer());
                    wrongAnswersArray.put(wrongObj);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        Intent intent = new Intent(this, QuizResultActivity.class);
        intent.putExtra("SCORE", score);
        intent.putExtra("TOTAL", total);
        intent.putExtra("TOPIC", topic);
        intent.putExtra("DIFFICULTY", difficulty);
        intent.putExtra("IS_REVIEW_MODE", isReviewMode);

        if (isReviewMode) {
            // Chế độ xem lại -> Truyền ID cũ sang để lấy Feedback cũ
            intent.putExtra("REVIEW_SESSION_ID", reviewSessionId);
        } else {
            // Chế độ làm bài -> Lưu vào DB, lấy ID mới, truyền json câu sai qua để gọi AI
            long newSessionId = quizRepository.saveQuizSession(currentUserId, topic, difficulty, score, total, questionList);
            intent.putExtra("REVIEW_SESSION_ID", newSessionId);
            intent.putExtra("WRONG_ANSWERS", wrongAnswersArray.toString());
            Toast.makeText(this, "Quiz Completed! Your Score: " + score + "/" + total, Toast.LENGTH_SHORT).show();
        }

        startActivity(intent);
        finish();
    }
}