package com.example.luminaai;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.luminaai.models.ChatSession;
import com.example.luminaai.repository.UserRepository;
import com.example.luminaai.sqlite.DbHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class LuminaAppTest {

    private DbHelper dbHelper;
    private SQLiteDatabase db;
    private UserRepository userRepository;
    private final String testEmail = "teststudent@gmail.com";

    @Before
    public void setUp() {
        // Initialize DbHelper and UserRepository with application context
        dbHelper = new DbHelper(ApplicationProvider.getApplicationContext());
        db = dbHelper.getWritableDatabase();
        userRepository = new UserRepository(ApplicationProvider.getApplicationContext());

        // Register test account if it does not exist in SQLite yet
        if (!userRepository.checkEmailExist(testEmail)) {
            userRepository.registerUser("Test Student", testEmail, "123456");
        }
    }

    @After
    public void tearDown() {
        if (db != null && db.isOpen()) {
            db.close();
        }
    }

    /**
     * TEST CASE TC-CHAT-001: Verify text question sending flow,
     * creating/retrieving chat session, and saving AI/User messages to SQLite.
     */
    @Test
    public void testChatSendingFlowAndDatabaseStorage_TC_CHAT_001() {
        int userId = userRepository.getUserIdByEmail(testEmail);
        assertTrue("User ID must exist", userId != -1);

        // 1. Simulate sending a new message when no session is selected (currentSessionId == -1)
        String userQuestion = "What is the formula for quadratic equations?";

        ContentValues sessionValues = new ContentValues();
        sessionValues.put("userId", userId);
        sessionValues.put("sessionTitle", userQuestion);
        sessionValues.put("createdAt", System.currentTimeMillis());
        long newSessionId = db.insert("ChatSessions", null, sessionValues);
        assertTrue("A new chat session must be created", newSessionId != -1);

        // 2. Insert user message into ChatMessages table
        ContentValues userMsgValues = new ContentValues();
        userMsgValues.put("sessionId", newSessionId);
        userMsgValues.put("messageText", userQuestion);
        userMsgValues.put("isUser", 1); // User message
        userMsgValues.put("createdAt", System.currentTimeMillis());
        long userMsgId = db.insert("ChatMessages", null, userMsgValues);
        assertTrue("User message must be saved successfully", userMsgId != -1);

        // 3. Simulate receiving AI response (saving to ChatMessages)
        String mockAiResponse = "[Subject: Mathematics | Level: High School | Style: Short] $$x = \\frac{-b \\pm \\sqrt{b^2 - 4ac}}{2a}$$";
        ContentValues aiMsgValues = new ContentValues();
        aiMsgValues.put("sessionId", newSessionId);
        aiMsgValues.put("messageText", mockAiResponse);
        aiMsgValues.put("isUser", 0); // AI message
        aiMsgValues.put("createdAt", System.currentTimeMillis());
        long aiMsgId = db.insert("ChatMessages", null, aiMsgValues);
        assertTrue("AI response message must be saved successfully", aiMsgId != -1);

        // 4. Verify message retrieval for this session
        Cursor cursor = db.rawQuery("SELECT messageText, isUser FROM ChatMessages WHERE sessionId = ? ORDER BY createdAt ASC", new String[]{String.valueOf(newSessionId)});

        assertTrue("Cursor must find messages", cursor.moveToFirst());
        assertEquals("First message should be user question", userQuestion, cursor.getString(0));
        assertEquals("First message isUser flag should be 1", 1, cursor.getInt(1));

        assertTrue("Cursor must move to AI response", cursor.moveToNext());
        assertEquals("Second message should be AI response", mockAiResponse, cursor.getString(0));
        assertEquals("Second message isUser flag should be 0", 0, cursor.getInt(1));

        cursor.close();
    }

    /**
     * TEST CASE TC-HIST-002: Verify chat session creation (ChatSessions)
     * and recent chat history retrieval from SQLite by userId, including subject extraction logic.
     */
    @Test
    public void testRecentChatHistoryRetrieval_TC_HIST_002() {
        int userId = userRepository.getUserIdByEmail(testEmail);
        assertTrue("User ID must be valid and exist in the database", userId != -1);

        // 1. Insert a mock chat session into the ChatSessions table
        ContentValues sessionValues = new ContentValues();
        sessionValues.put("userId", userId);
        sessionValues.put("sessionTitle", "Solve quadratic equation");
        sessionValues.put("createdAt", System.currentTimeMillis());
        long sessionId = db.insert("ChatSessions", null, sessionValues);
        assertTrue("Session ID must be successfully created", sessionId != -1);

        // 2. Insert AI response containing standard metadata [Subject: Mathematics | ...] into ChatMessages
        ContentValues msgValues = new ContentValues();
        msgValues.put("sessionId", sessionId);
        msgValues.put("messageText", "[Subject: Mathematics | Level: High School | Style: Short] The solution is x = 2 and x = 3.");
        msgValues.put("isUser", 0); // AI message
        msgValues.put("createdAt", System.currentTimeMillis());
        long msgId = db.insert("ChatMessages", null, msgValues);
        assertTrue("AI Message ID must be successfully created", msgId != -1);

        // 3. Call getRecentChatSessions from UserRepository to verify
        List<ChatSession> recentChats = userRepository.getRecentChatSessions(testEmail, 5);

        assertNotNull("Returned chat list must not be null", recentChats);
        assertFalse("Chat list must contain at least one element", recentChats.isEmpty());

        // 4. Validate results
        ChatSession latestSession = recentChats.get(0);
        assertEquals("Session title must match", "Solve quadratic equation", latestSession.getSessionTitle());
        assertEquals("System must successfully extract the subject as Mathematics", "Mathematics", latestSession.getSubjectName());
    }

    /**
     * TEST CASE TC-BOOK-003: Verify marking or unmarking
     * important answers (isBookmarked), updating directly in the ChatMessages table.
     */
    @Test
    public void testBookmarkMessageUpdate_TC_BOOK_003() {
        int userId = userRepository.getUserIdByEmail(testEmail);

        // 1. Create a sample session
        ContentValues sessionValues = new ContentValues();
        sessionValues.put("userId", userId);
        sessionValues.put("sessionTitle", "Java OOP Principles");
        sessionValues.put("createdAt", System.currentTimeMillis());
        long sessionId = db.insert("ChatSessions", null, sessionValues);

        // 2. Create an AI message with initial isBookmarked status = 0 (Not bookmarked)
        ContentValues msgValues = new ContentValues();
        msgValues.put("sessionId", sessionId);
        msgValues.put("messageText", "Encapsulation, Inheritance, Polymorphism, Abstraction.");
        msgValues.put("isUser", 0);
        msgValues.put("isBookmarked", 0);
        msgValues.put("createdAt", System.currentTimeMillis());
        long messageId = db.insert("ChatMessages", null, msgValues);
        assertTrue("Message ID must exist", messageId != -1);

        // 3. Simulate user action: Click Bookmark button (Update isBookmarked = 1)
        ContentValues updateValues = new ContentValues();
        updateValues.put("isBookmarked", 1);
        int rowsAffected = db.update("ChatMessages", updateValues, "messageId = ?", new String[]{String.valueOf(messageId)});

        assertEquals("Exactly 1 row should be updated in the database", 1, rowsAffected);

        // 4. Verify actual stored value in SQLite
        Cursor cursor = db.rawQuery("SELECT isBookmarked FROM ChatMessages WHERE messageId = ?", new String[]{String.valueOf(messageId)});
        assertTrue("Cursor must move to the data row", cursor.moveToFirst());
        int bookmarkStatus = cursor.getInt(0);
        cursor.close();

        assertEquals("Bookmark status after update must be 1 (Bookmarked)", 1, bookmarkStatus);
    }
}