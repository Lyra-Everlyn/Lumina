package com.example.luminaai.repository;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.luminaai.sqlite.DbHelper;

import java.util.ArrayList;
import java.util.List;

public class SubjectRepository {
    private DbHelper dbHelper;

    public SubjectRepository(Context context) {
        dbHelper = new DbHelper(context);
    }

    public List<String> getAllSubjectNames() {
        List<String> subjectList = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT subjectName FROM Subjects ORDER BY subjectName ASC";
        Cursor cursor = db.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            do {
                subjectList.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();

        return subjectList;
    }

    public void addSubjectIfNotExists(String subjectName) {
        if (subjectName == null || subjectName.trim().isEmpty() || subjectName.equals("Academic")) {
            return;
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        String cleanSubject = subjectName.trim();
        Cursor cursor = db.rawQuery("SELECT subjectId FROM Subjects WHERE subjectName = ? COLLATE NOCASE", new String[]{cleanSubject});

        if (cursor.getCount() == 0) {
            ContentValues values = new ContentValues();
            values.put("subjectName", cleanSubject);
            db.insert("Subjects", null, values);
        }

        cursor.close();
        db.close();
    }
}