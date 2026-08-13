package com.example.luminaai.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.luminaai.R;
import com.example.luminaai.adapters.QuizHistoryAdapter;
import com.example.luminaai.helpers.SessionManager;
import com.example.luminaai.models.QuizSession;
import com.example.luminaai.repository.QuizRepository;
import com.example.luminaai.repository.SubjectRepository;
import com.example.luminaai.repository.UserRepository;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;

import java.util.ArrayList;
import java.util.List;

public class QuizFragment extends Fragment {

    private AutoCompleteTextView autoCompleteTopic;
    private MaterialButtonToggleGroup toggleDifficulty;
    private MaterialButton btnGenerateQuiz;
    private RecyclerView recyclerQuizHistory;
    private TextView tvNoQuizHistory;

    private SubjectRepository subjectRepository;
    private QuizRepository quizRepository;

    private QuizHistoryAdapter historyAdapter;
    private List<QuizSession> historyList;
    private int currentUserId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_quiz, container, false);

        // 1. Ánh xạ Views
        autoCompleteTopic = view.findViewById(R.id.autoCompleteTopic);
        toggleDifficulty = view.findViewById(R.id.toggleDifficulty);
        btnGenerateQuiz = view.findViewById(R.id.btnGenerateQuiz);
        recyclerQuizHistory = view.findViewById(R.id.recyclerQuizHistory);
        tvNoQuizHistory = view.findViewById(R.id.tvNoQuizHistory);

        // 2. Khởi tạo Repository & User logic
        subjectRepository = new SubjectRepository(requireContext());
        quizRepository = new QuizRepository(requireContext());

        SessionManager sessionManager = new SessionManager(requireContext());
        UserRepository userRepository = new UserRepository(requireContext());
        currentUserId = userRepository.getUserIdByEmail(sessionManager.getEmail());

        // 3. Setup giao diện
        setupTopicDropdown();
        setupHistoryRecyclerView();

        // 4. Xử lý sự kiện bấm nút Tạo Quiz
        btnGenerateQuiz.setOnClickListener(v -> generateQuiz());

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadQuizHistory();
    }

    private void setupTopicDropdown() {
        List<String> topics = subjectRepository.getAllSubjectNames();

        if (topics.isEmpty()) {
            topics.add("Java Programming");
            topics.add("Lua Scripting");
            topics.add("Mathematics");
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                topics
        );

        autoCompleteTopic.setAdapter(adapter);
        autoCompleteTopic.setOnClickListener(v -> autoCompleteTopic.showDropDown());
    }

    private void setupHistoryRecyclerView() {
        historyList = new ArrayList<>();
        historyAdapter = new QuizHistoryAdapter(historyList);

        recyclerQuizHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerQuizHistory.setAdapter(historyAdapter);
    }

    private void loadQuizHistory() {
        historyList.clear();
        historyList.addAll(quizRepository.getQuizHistoryByUserId(currentUserId));
        historyAdapter.notifyDataSetChanged();

        if (historyList.isEmpty()) {
            tvNoQuizHistory.setVisibility(View.VISIBLE);
            recyclerQuizHistory.setVisibility(View.GONE);
        } else {
            tvNoQuizHistory.setVisibility(View.GONE);
            recyclerQuizHistory.setVisibility(View.VISIBLE);
        }
    }

    private void generateQuiz() {
        String selectedTopic = autoCompleteTopic.getText().toString().trim();

        if (TextUtils.isEmpty(selectedTopic)) {
            Toast.makeText(getContext(), "Please select or type a topic", Toast.LENGTH_SHORT).show();
            return;
        }

        subjectRepository.addSubjectIfNotExists(selectedTopic);

        int checkedId = toggleDifficulty.getCheckedButtonId();
        String difficulty = "Medium";
        if (checkedId == R.id.btnEasy) difficulty = "Easy";
        else if (checkedId == R.id.btnHard) difficulty = "Hard";

        Toast.makeText(getContext(), "Generating " + difficulty + " quiz for '" + selectedTopic + "'", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(getActivity(), com.example.luminaai.activities.QuizSessionActivity.class);
        intent.putExtra("QUIZ_TOPIC", selectedTopic);
        intent.putExtra("QUIZ_DIFFICULTY", difficulty);
        intent.putExtra("CURRENT_USER_ID", currentUserId);
        startActivity(intent);
    }
}