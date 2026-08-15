package com.example.luminaai.sqlite;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

public class DbHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "Lumina_Base";
    // Nâng cấp DB_VERSION lên 4 để hỗ trợ lưu chi tiết đáp án và AI Feedback
    public static final int DB_VERSION = 4;

    // --- 1. USER TABLE ---
    protected static final String USER_TABLE = "Users";
    protected static final String USER_ID = "userId";
    protected static final String USER_FULLNAME = "userFullName";
    protected static final String USER_EMAIL = "userEmail";
    protected static final String USER_PASSWORD = "userPassword";
    protected static final String USER_EDU_LEVEL = "userEduLevel";
    protected static final String USER_EXP_STYLE = "userExpStyle";

    // --- 2. SUBJECT TABLE ---
    protected static final String SUBJECT_TABLE = "Subjects";
    protected static final String SUBJECT_ID = "subjectId";
    protected static final String SUBJECT_NAME = "subjectName";

    // --- 3. CHAT SESSIONS TABLE ---
    protected static final String SESSION_TABLE = "ChatSessions";
    protected static final String SESSION_ID = "sessionId";
    protected static final String SESSION_USER_ID = "userId";
    protected static final String SESSION_TITLE = "sessionTitle";
    protected static final String SESSION_TIMESTAMP = "createdAt";

    // --- 4. CHAT MESSAGES TABLE ---
    protected static final String MESSAGE_TABLE = "ChatMessages";
    protected static final String MESSAGE_ID = "messageId";
    protected static final String MESSAGE_SESSION_ID = "sessionId";
    protected static final String MESSAGE_TEXT = "messageText";
    protected static final String MESSAGE_IS_USER = "isUser";
    protected static final String MESSAGE_TIMESTAMP = "createdAt";
    protected static final String MESSAGE_IS_BOOKMARKED = "isBookmarked";

    // --- 5. QUIZ SESSIONS TABLE (Lịch sử làm bài) ---
    protected static final String QUIZ_SESSION_TABLE = "QuizSessions";
    protected static final String QUIZ_SESSION_ID = "quizSessionId";
    protected static final String QUIZ_USER_ID = "userId";
    protected static final String QUIZ_TOPIC = "topic";
    protected static final String QUIZ_DIFFICULTY = "difficulty";
    protected static final String QUIZ_SCORE = "score";
    protected static final String QUIZ_TOTAL_QUESTIONS = "totalQuestions";
    protected static final String QUIZ_AI_FEEDBACK = "aiFeedback";
    protected static final String QUIZ_TIMESTAMP = "createdAt";

    // --- 6. QUIZ QUESTIONS TABLE ---
    protected static final String QUIZ_QUESTION_TABLE = "QuizQuestions";
    protected static final String QUIZ_QUESTION_ID = "questionId";
    protected static final String QUIZ_Q_SESSION_ID = "quizSessionId";
    protected static final String QUIZ_QUESTION_TEXT = "questionText";
    protected static final String QUIZ_OPTION_A = "optionA";
    protected static final String QUIZ_OPTION_B = "optionB";
    protected static final String QUIZ_OPTION_C = "optionC";
    protected static final String QUIZ_OPTION_D = "optionD";
    protected static final String QUIZ_CORRECT_ANSWER = "correctAnswer";
    protected static final String QUIZ_SELECTED_ANSWER = "selectedAnswer";
    protected static final String QUIZ_IS_BOOKMARKED = "isBookmarked";

    // --- TẠO BẢNG QUERIES ---
    private static final String CREATE_USER_TABLE = "CREATE TABLE " + USER_TABLE + "("
            + USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + USER_FULLNAME + " NVARCHAR(60) NOT NULL, "
            + USER_EMAIL + " NVARCHAR(60) NOT NULL, "
            + USER_PASSWORD + " VARCHAR(255) NOT NULL, "
            + USER_EDU_LEVEL + " VARCHAR(50), "
            + USER_EXP_STYLE + " VARCHAR(50)" + ")";

    private static final String CREATE_SUBJECT_TABLE = "CREATE TABLE " + SUBJECT_TABLE + "("
            + SUBJECT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + SUBJECT_NAME + " VARCHAR(100) UNIQUE NOT NULL" + ")";

    private static final String CREATE_SESSION_TABLE = "CREATE TABLE " + SESSION_TABLE + "("
            + SESSION_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + SESSION_USER_ID + " INTEGER NOT NULL, "
            + SESSION_TITLE + " NVARCHAR(255) NOT NULL, "
            + SESSION_TIMESTAMP + " INTEGER, "
            + "FOREIGN KEY(" + SESSION_USER_ID + ") REFERENCES " + USER_TABLE + "(" + USER_ID + ") ON DELETE CASCADE" + ")";

    private static final String CREATE_MESSAGE_TABLE = "CREATE TABLE " + MESSAGE_TABLE + "("
            + MESSAGE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + MESSAGE_SESSION_ID + " INTEGER NOT NULL, "
            + MESSAGE_TEXT + " TEXT NOT NULL, "
            + MESSAGE_IS_USER + " INTEGER NOT NULL DEFAULT 1, "
            + MESSAGE_TIMESTAMP + " INTEGER, "
            + MESSAGE_IS_BOOKMARKED + " INTEGER DEFAULT 0, "
            + "FOREIGN KEY(" + MESSAGE_SESSION_ID + ") REFERENCES " + SESSION_TABLE + "(" + SESSION_ID + ") ON DELETE CASCADE" + ")";

    private static final String CREATE_QUIZ_SESSION_TABLE = "CREATE TABLE " + QUIZ_SESSION_TABLE + "("
            + QUIZ_SESSION_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + QUIZ_USER_ID + " INTEGER NOT NULL, "
            + QUIZ_TOPIC + " VARCHAR(100) NOT NULL, "
            + QUIZ_DIFFICULTY + " VARCHAR(20) NOT NULL, "
            + QUIZ_SCORE + " INTEGER, "
            + QUIZ_TOTAL_QUESTIONS + " INTEGER, "
            + QUIZ_AI_FEEDBACK + " TEXT, "
            + QUIZ_TIMESTAMP + " INTEGER, "
            + "FOREIGN KEY(" + QUIZ_USER_ID + ") REFERENCES " + USER_TABLE + "(" + USER_ID + ") ON DELETE CASCADE" + ")";

    private static final String CREATE_QUIZ_QUESTION_TABLE = "CREATE TABLE " + QUIZ_QUESTION_TABLE + "("
            + QUIZ_QUESTION_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + QUIZ_Q_SESSION_ID + " INTEGER NOT NULL, "
            + QUIZ_QUESTION_TEXT + " TEXT NOT NULL, "
            + QUIZ_OPTION_A + " TEXT, "
            + QUIZ_OPTION_B + " TEXT, "
            + QUIZ_OPTION_C + " TEXT, "
            + QUIZ_OPTION_D + " TEXT, "
            + QUIZ_CORRECT_ANSWER + " VARCHAR(5), "
            + QUIZ_SELECTED_ANSWER + " VARCHAR(5), "
            + QUIZ_IS_BOOKMARKED + " INTEGER DEFAULT 0, "
            + "FOREIGN KEY(" + QUIZ_Q_SESSION_ID + ") REFERENCES " + QUIZ_SESSION_TABLE + "(" + QUIZ_SESSION_ID + ") ON DELETE CASCADE" + ")";

    public DbHelper(@Nullable Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    public DbHelper(@Nullable Context context, @Nullable String name, @Nullable SQLiteDatabase.CursorFactory factory, int version) {
        super(context, DB_NAME, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_USER_TABLE);
        db.execSQL(CREATE_SUBJECT_TABLE);
        db.execSQL(CREATE_SESSION_TABLE);
        db.execSQL(CREATE_MESSAGE_TABLE);
        db.execSQL(CREATE_QUIZ_SESSION_TABLE);
        db.execSQL(CREATE_QUIZ_QUESTION_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("DROP TABLE IF EXISTS QuestionHistory");
            db.execSQL(CREATE_SESSION_TABLE);
            db.execSQL(CREATE_MESSAGE_TABLE);
        }

        if (oldVersion < 3) {
            db.execSQL("CREATE TABLE IF NOT EXISTS QuizSessions (" +
                    "quizSessionId INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "userId INTEGER NOT NULL, " +
                    "topic VARCHAR(100) NOT NULL, " +
                    "difficulty VARCHAR(20) NOT NULL, " +
                    "score INTEGER, " +
                    "totalQuestions INTEGER, " +
                    "createdAt INTEGER, " +
                    "FOREIGN KEY(userId) REFERENCES Users(userId) ON DELETE CASCADE)");

            db.execSQL("CREATE TABLE IF NOT EXISTS QuizQuestions (" +
                    "questionId INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "quizSessionId INTEGER NOT NULL, " +
                    "questionText TEXT NOT NULL, " +
                    "isBookmarked INTEGER DEFAULT 0, " +
                    "FOREIGN KEY(quizSessionId) REFERENCES QuizSessions(quizSessionId) ON DELETE CASCADE)");
        }

        if (oldVersion < 4) {
            db.execSQL("DROP TABLE IF EXISTS " + QUIZ_QUESTION_TABLE);
            db.execSQL("DROP TABLE IF EXISTS " + QUIZ_SESSION_TABLE);
            db.execSQL(CREATE_QUIZ_SESSION_TABLE);
            db.execSQL(CREATE_QUIZ_QUESTION_TABLE);
        }
    }
}