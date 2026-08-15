package com.example.luminaai.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.luminaai.R;
import com.example.luminaai.models.QuizSession;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class QuizHistoryAdapter extends RecyclerView.Adapter<QuizHistoryAdapter.QuizViewHolder> {

    private List<QuizSession> quizList;
    private OnQuizItemClickListener listener;

    public interface OnQuizItemClickListener {
        void onQuizClick(QuizSession session);
    }

    public QuizHistoryAdapter(List<QuizSession> quizList, OnQuizItemClickListener listener) {
        this.quizList = quizList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public QuizViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_quiz_history, parent, false);
        return new QuizViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull QuizViewHolder holder, int position) {
        QuizSession quiz = quizList.get(position);

        holder.tvQuizTopic.setText(quiz.getTopic());

        // Format ngày tháng (vd: Oct 24, 2026)
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        String dateString = sdf.format(new Date(quiz.getCreatedAt()));

        holder.tvQuizDifficultyAndDate.setText(quiz.getDifficulty() + " • " + dateString);

        // Format điểm (vd: 8/10)
        holder.tvQuizScore.setText(quiz.getScore() + "/" + quiz.getTotalQuestions());

        // 4. Gắn sự kiện Click vào toàn bộ Item (CardView)
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onQuizClick(quiz);
            }
        });
    }

    @Override
    public int getItemCount() {
        return quizList != null ? quizList.size() : 0;
    }

    public static class QuizViewHolder extends RecyclerView.ViewHolder {
        TextView tvQuizTopic, tvQuizDifficultyAndDate, tvQuizScore;

        public QuizViewHolder(@NonNull View itemView) {
            super(itemView);
            tvQuizTopic = itemView.findViewById(R.id.tvQuizTopic);
            tvQuizDifficultyAndDate = itemView.findViewById(R.id.tvQuizDifficultyAndDate);
            tvQuizScore = itemView.findViewById(R.id.tvQuizScore);
        }
    }
}