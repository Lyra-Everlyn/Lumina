package com.example.luminaai.repository;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.luminaai.models.QuizQuestion;
import com.example.luminaai.models.QuizSession;
import com.example.luminaai.sqlite.DbHelper;

import java.util.ArrayList;
import java.util.List;

public class QuizRepository {
    private DbHelper dbHelper;

    public QuizRepository(Context context) {
        dbHelper = new DbHelper(context);
    }

    public List<QuizSession> getQuizHistoryByUserId(int userId) {
        List<QuizSession> historyList = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String query = "SELECT quizSessionId, topic, difficulty, score, totalQuestions, createdAt " +
                "FROM QuizSessions WHERE userId = ? ORDER BY createdAt DESC";

        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(userId)});

        if (cursor.moveToFirst()) {
            do {
                long id = cursor.getLong(0);
                String topic = cursor.getString(1);
                String difficulty = cursor.getString(2);
                int score = cursor.getInt(3);
                int totalQuestions = cursor.getInt(4);
                long createdAt = cursor.getLong(5);

                historyList.add(new QuizSession(id, topic, difficulty, score, totalQuestions, createdAt));
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return historyList;
    }

    public long saveQuizSession(int userId, String topic, String difficulty, int score, int totalQuestions, List<QuizQuestion> questions) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long sessionId = -1;
        try {
            db.beginTransaction();

            ContentValues sessionValues = new ContentValues();
            sessionValues.put("userId", userId);
            sessionValues.put("topic", topic);
            sessionValues.put("difficulty", difficulty);
            sessionValues.put("score", score);
            sessionValues.put("totalQuestions", totalQuestions);
            sessionValues.put("createdAt", System.currentTimeMillis());

            sessionId = db.insert("QuizSessions", null, sessionValues);

            if (sessionId != -1 && questions != null) {
                for (QuizQuestion q : questions) {
                    ContentValues qValues = new ContentValues();
                    qValues.put("quizSessionId", sessionId);
                    qValues.put("questionText", q.getQuestionText() + " [Correct answer: " + q.getCorrectAnswer() + "]");
                    qValues.put("isBookmarked", q.isBookmarked() ? 1 : 0);
                    db.insert("QuizQuestions", null, qValues);
                }
            }
            db.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.endTransaction();
            db.close();
        }
        return sessionId;
    }
}