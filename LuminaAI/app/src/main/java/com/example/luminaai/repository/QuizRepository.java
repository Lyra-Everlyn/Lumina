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
                    qValues.put("questionText", q.getQuestionText());
                    qValues.put("optionA", q.getOptionA());
                    qValues.put("optionB", q.getOptionB());
                    qValues.put("optionC", q.getOptionC());
                    qValues.put("optionD", q.getOptionD());
                    qValues.put("correctAnswer", q.getCorrectAnswer());
                    qValues.put("selectedAnswer", q.getSelectedAnswer());
                    qValues.put("isBookmarked", q.isBookmarked() ? 1 : 0);
                    db.insert("QuizQuestions", null, qValues);
                }
            }
            db.setTransactionSuccessful();
        } catch (Exception e) { e.printStackTrace(); }
        finally { db.endTransaction(); db.close(); }
        return sessionId;
    }

    // LẤY DANH SÁCH CÂU HỎI ĐỂ XEM LẠI
    public List<QuizQuestion> getQuestionsBySessionId(long sessionId) {
        List<QuizQuestion> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM QuizQuestions WHERE quizSessionId = ?", new String[]{String.valueOf(sessionId)});

        if (cursor.moveToFirst()) {
            do {
                QuizQuestion q = new QuizQuestion(
                        cursor.getString(cursor.getColumnIndexOrThrow("questionText")),
                        cursor.getString(cursor.getColumnIndexOrThrow("optionA")),
                        cursor.getString(cursor.getColumnIndexOrThrow("optionB")),
                        cursor.getString(cursor.getColumnIndexOrThrow("optionC")),
                        cursor.getString(cursor.getColumnIndexOrThrow("optionD")),
                        cursor.getString(cursor.getColumnIndexOrThrow("correctAnswer"))
                );
                q.setSelectedAnswer(cursor.getString(cursor.getColumnIndexOrThrow("selectedAnswer")));
                q.setBookmarked(cursor.getInt(cursor.getColumnIndexOrThrow("isBookmarked")) == 1);
                list.add(q);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return list;
    }

    // LƯU & LẤY NHẬN XÉT CỦA AI
    public void updateAiFeedback(long sessionId, String feedback) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("aiFeedback", feedback);
        db.update("QuizSessions", values, "quizSessionId = ?", new String[]{String.valueOf(sessionId)});
        db.close();
    }

    public String getAiFeedback(long sessionId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String feedback = "";
        Cursor cursor = db.rawQuery("SELECT aiFeedback FROM QuizSessions WHERE quizSessionId = ?", new String[]{String.valueOf(sessionId)});
        if (cursor.moveToFirst()) feedback = cursor.getString(0);
        cursor.close();
        db.close();
        return feedback;
    }
}