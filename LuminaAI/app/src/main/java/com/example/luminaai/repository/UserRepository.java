package com.example.luminaai.repository;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.luminaai.models.ChatSession;
import com.example.luminaai.sqlite.DbHelper;

import java.util.ArrayList;
import java.util.List;

public class UserRepository {
    private DbHelper dbHelper;

    public UserRepository(Context context) {
        dbHelper = new DbHelper(context);
    }

    public boolean registerUser(String fullName, String email, String password) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("userFullName", fullName);
        values.put("userEmail", email);
        values.put("userPassword", password);

        long result = db.insert("Users", null, values);
        db.close();
        return result != -1;
    }

    public boolean checkEmailExist(String email) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM Users WHERE userEmail = ?", new String[]{email});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        db.close();
        return exists;
    }

    public boolean registerGoogleUserIfNotExists(String fullName, String email, String hashedPassword) {
        if (checkEmailExist(email)) {
            return true;
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("userFullName", fullName);
        values.put("userEmail", email);
        values.put("userPassword", hashedPassword);

        long result = db.insert("Users", null, values);
        db.close();
        return result != -1;
    }

    public boolean checkUserLogin(String email, String hashedPassword) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM Users WHERE userEmail = ? AND userPassword = ?", new String[]{email, hashedPassword});
        boolean isValid = cursor.getCount() > 0;
        cursor.close();
        db.close();
        return isValid;
    }

    public boolean updateUserPreferences(String email, String eduLevel, String expStyle) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put("userEduLevel", eduLevel);
        values.put("userExpStyle", expStyle);

        int rowsAffected = db.update("Users", values, "userEmail = ?", new String[]{email});
        db.close();

        return rowsAffected > 0;
    }

    public boolean isUserSetupCompleted(String email) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT userEduLevel, userExpStyle FROM Users WHERE userEmail = ?", new String[]{email});

        boolean isCompleted = false;
        if (cursor.moveToFirst()) {
            String edu = cursor.getString(0);
            String exp = cursor.getString(1);

            if (edu != null && !edu.isEmpty() && exp != null && !exp.isEmpty()) {
                isCompleted = true;
            }
        }
        cursor.close();
        db.close();

        return isCompleted;
    }

    public String getUserFullName(String email) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT userFullName FROM Users WHERE userEmail = ?", new String[]{email});
        String fullName = "User";
        if (cursor.moveToFirst()) {
            fullName = cursor.getString(0);
        }
        cursor.close();
        db.close();
        return fullName;
    }


    public int getUserIdByEmail(String email) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT userId FROM Users WHERE userEmail = ?", new String[]{email});
        int userId = -1; // -1 nghĩa là không tìm thấy
        if (cursor.moveToFirst()) {
            userId = cursor.getInt(0);
        }
        cursor.close();
        db.close();
        return userId;
    }

    public int getTotalQuestionsAsked(String email) {
        int userId = getUserIdByEmail(email);
        if (userId == -1) return 0;

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT COUNT(*) FROM ChatMessages " +
                "INNER JOIN ChatSessions ON ChatMessages.sessionId = ChatSessions.sessionId " +
                "WHERE ChatSessions.userId = ? AND ChatMessages.isUser = 1";

        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(userId)});
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        db.close();
        return count;
    }

    public String getUserEduLevel(String email) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT userEduLevel FROM Users WHERE userEmail = ?", new String[]{email});
        String level = "High School";
        if (cursor.moveToFirst()) {
            String val = cursor.getString(0);
            if (val != null && !val.isEmpty()) {
                level = val;
            }
        }
        cursor.close();
        db.close();
        return level;
    }

    public String getUserExpStyle(String email) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT userExpStyle FROM Users WHERE userEmail = ?", new String[]{email});
        String style = "Short";
        if (cursor.moveToFirst()) {
            String val = cursor.getString(0);
            if (val != null && !val.isEmpty()) {
                style = val;
            }
        }
        cursor.close();
        db.close();
        return style;
    }

    public List<ChatSession> getRecentChatSessions(String email, int limit) {
        List<ChatSession> list = new ArrayList<>();
        int userId = getUserIdByEmail(email);
        if (userId == -1) return list;

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT sessionId, sessionTitle, createdAt FROM ChatSessions WHERE userId = ? ORDER BY createdAt DESC LIMIT ?",
                new String[]{String.valueOf(userId), String.valueOf(limit)}
        );

        if (cursor.moveToFirst()) {
            do {
                long id = cursor.getLong(0);
                String title = cursor.getString(1);
                long time = cursor.getLong(2);

                String subject = "ACADEMIC";

                list.add(new ChatSession(id, title, subject, time));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }
}