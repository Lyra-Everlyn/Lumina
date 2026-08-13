package com.example.luminaai.fragments;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.luminaai.R;
import com.example.luminaai.activities.LoginActivity;
import com.example.luminaai.adapters.ChatAdapter;
import com.example.luminaai.adapters.RecentChatAdapter;
import com.example.luminaai.api.AiApiClient;
import com.example.luminaai.helpers.SessionManager;
import com.example.luminaai.models.ChatMessage;
import com.example.luminaai.models.ChatSession;
import com.example.luminaai.repository.SubjectRepository;
import com.example.luminaai.repository.UserRepository;
import com.example.luminaai.sqlite.DbHelper;
import com.google.android.material.button.MaterialButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Request;
import okhttp3.Response;

public class ChatFragment extends Fragment {

    private DrawerLayout drawerLayout;
    private RecyclerView recyclerViewChat, recyclerRecentChats;
    private EditText inputChatMessage;
    private MaterialButton btnSendMessage;
    private ImageView btnOpenHistory;

    private List<ChatMessage> messageList;
    private ChatAdapter chatAdapter;

    private List<ChatSession> recentChatList;
    private RecentChatAdapter recentChatAdapter;

    private DbHelper dbHelper;
    private SQLiteDatabase db;
    private SubjectRepository subjectRepository;
    private long currentSessionId = -1;
    private int currentUserId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        if (getArguments() != null) {
            currentSessionId = getArguments().getLong("SELECTED_SESSION_ID", -1);
        }

        dbHelper = new DbHelper(requireContext());
        subjectRepository = new SubjectRepository(requireContext());
        try {
            db = dbHelper.getWritableDatabase();
        } catch (Exception e) {
            e.printStackTrace();
        }

        final SessionManager[] sessionManager = {new SessionManager(requireContext())};
        final UserRepository[] userRepository = {new UserRepository(requireContext())};

        final String[] userEmail = {sessionManager[0].getEmail()};
        currentUserId = userRepository[0].getUserIdByEmail(userEmail[0]);

        if (currentUserId == -1) {
            Toast.makeText(getContext(), "User authentication error. Please log in again!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(getActivity(), LoginActivity.class));
            if (getActivity() != null) {
                getActivity().finish();
            }
            return new View(requireContext());
        }

        DrawerLayout rootDrawer = new DrawerLayout(requireContext());
        rootDrawer.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        rootDrawer.setId(View.generateViewId());
        this.drawerLayout = rootDrawer;

        View chatMainView = inflater.inflate(R.layout.fragment_chat, rootDrawer, false);
        rootDrawer.addView(chatMainView);

        View historyDrawerView = inflater.inflate(R.layout.nav_drawer_chat_history, rootDrawer, false);
        DrawerLayout.LayoutParams drawerParams = new DrawerLayout.LayoutParams(
                750,
                ViewGroup.LayoutParams.MATCH_PARENT,
                Gravity.START
        );
        historyDrawerView.setLayoutParams(drawerParams);
        rootDrawer.addView(historyDrawerView);

        recyclerViewChat = chatMainView.findViewById(R.id.recyclerViewChat);
        inputChatMessage = chatMainView.findViewById(R.id.inputChatMessage);
        btnSendMessage = chatMainView.findViewById(R.id.btnSendMessage);
        btnOpenHistory = chatMainView.findViewById(R.id.btnOpenHistory);
        recyclerRecentChats = historyDrawerView.findViewById(R.id.recyclerRecentChats);

        if (btnOpenHistory != null) {
            btnOpenHistory.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        }

        messageList = new ArrayList<>();
        chatAdapter = new ChatAdapter(messageList, (message, position) -> {
            boolean newState = !message.isBookmarked();
            message.setBookmarked(newState);

            if (db != null && db.isOpen()) {
                ContentValues values = new ContentValues();
                values.put("isBookmarked", newState ? 1 : 0);
                db.update("ChatMessages", values, "messageId = ?", new String[]{String.valueOf(message.getId())});
            }

            chatAdapter.notifyItemChanged(position);

            String toastMsg = newState ? "Bookmarked" : "Removed from bookmarks";
            Toast.makeText(getContext(), toastMsg, Toast.LENGTH_SHORT).show();
        });

        LinearLayoutManager chatLayoutManager = new LinearLayoutManager(getContext());
        chatLayoutManager.setStackFromEnd(true);
        recyclerViewChat.setLayoutManager(chatLayoutManager);
        recyclerViewChat.setAdapter(chatAdapter);

        recentChatList = new ArrayList<>();
        recentChatAdapter = new RecentChatAdapter(recentChatList, session -> {
            currentSessionId = session.getSessionId();
            loadChatMessages(currentSessionId);
            drawerLayout.closeDrawer(GravityCompat.START);
        });
        recyclerRecentChats.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerRecentChats.setAdapter(recentChatAdapter);

        loadRecentChats();

        if (currentSessionId != -1) {
            loadChatMessages(currentSessionId);
        }

        // Click gửi tin nhắn
        btnSendMessage.setOnClickListener(v -> {
            String messageText = inputChatMessage.getText().toString().trim();
            if (!messageText.isEmpty() && db != null && db.isOpen()) {
                inputChatMessage.setText("");

                btnSendMessage.setEnabled(false);

                // Khởi tạo Tiêu đề đoạn chat lên Danh sách lịch sử chat gần đây (nếu là tin nhắn đầu tiên)
                if (currentSessionId == -1) {
                    ContentValues sessionValues = new ContentValues();
                    sessionValues.put("userId", currentUserId);
                    sessionValues.put("sessionTitle", messageText);
                    sessionValues.put("createdAt", System.currentTimeMillis());
                    currentSessionId = db.insert("ChatSessions", null, sessionValues);

                    loadRecentChats();
                }

                // Nếu là tin nhắn thứ N
                ContentValues userMsgValues = new ContentValues();
                userMsgValues.put("sessionId", currentSessionId);
                userMsgValues.put("messageText", messageText);
                userMsgValues.put("isUser", 1);
                userMsgValues.put("createdAt", System.currentTimeMillis());

                long userMsgId = db.insert("ChatMessages", null, userMsgValues);
                messageList.add(new ChatMessage(userMsgId, messageText, true, false));
                chatAdapter.notifyItemInserted(messageList.size() - 1);
                recyclerViewChat.scrollToPosition(messageList.size() - 1);

                // GỌI GROQ API QUA BACKGROUND THREAD SỬ DỤNG AIAPICLIENT
                ExecutorService executorService = Executors.newSingleThreadExecutor();
                Handler mainHandler = new Handler(Looper.getMainLooper());

                executorService.execute(() -> {
                    String aiResponse = "";
                    try {
                        userRepository[0] = new UserRepository(requireContext());
                        sessionManager[0] = new SessionManager(requireContext());
                        userEmail[0] = sessionManager[0].getEmail();

                        String dbUserLevel = userRepository[0].getUserEduLevel(userEmail[0]);
                        String dbUserStyle = userRepository[0].getUserExpStyle(userEmail[0]);

                        JSONObject jsonBody = new JSONObject();
                        jsonBody.put("model", "llama-3.3-70b-versatile");

                        JSONArray messagesArray = new JSONArray();

                        JSONObject systemObj = new JSONObject();
                        systemObj.put("role", "system");
                        systemObj.put("content",
                                "You are Lumina, a professional educational AI assistant.\n" +
                                        "- Target Education Level (from Database): " + dbUserLevel + ".\n" +
                                        "- Target Response Style (from Database): " + dbUserStyle + ".\n" +
                                        "- FORMATTING RULES FOR MATH:\n" +
                                        "  + YOU MUST USE DOUBLE DOLLAR SIGNS `$$` FOR ALL MATHEMATICAL FORMULAS, EQUATIONS, AND VARIABLES. This applies to BOTH inline math (e.g., The answer is $$4$$) and block math.\n" +
                                        "  + NEVER use single dollar signs `$`. NEVER use \\( or \\) or normal text parentheses.\n" +
                                        "  + For code snippets, use standard Markdown code blocks.\n" +
                                        "- SUBJECT IDENTIFICATION & TAGGING: You MUST start your response with this exact metadata header: [Subject: <Field> | Level: " + dbUserLevel + " | Style: " + dbUserStyle + "]\n" +
                                        "- STRICT RULE: Only answer academic questions. Otherwise, refuse politely.\n" +
                                        "- TOKEN OPTIMIZATION: Be concise and direct."
                        );
                        messagesArray.put(systemObj);

                        JSONObject messageObj = new JSONObject();
                        messageObj.put("role", "user");
                        messageObj.put("content", messageText);
                        messagesArray.put(messageObj);

                        jsonBody.put("messages", messagesArray);

                        // Giao tiếp với API thông qua Singleton AiApiClient
                        AiApiClient apiClient = AiApiClient.getInstance();
                        Request request = apiClient.buildAiRequest(jsonBody.toString());

                        try (Response response = apiClient.getClient().newCall(request).execute()) {
                            if (response.isSuccessful() && response.body() != null) {
                                String responseStr = response.body().string();
                                JSONObject jsonResponse = new JSONObject(responseStr);
                                JSONArray choices = jsonResponse.getJSONArray("choices");
                                JSONObject firstChoice = choices.getJSONObject(0);
                                JSONObject messageResponse = firstChoice.getJSONObject("message");
                                aiResponse = messageResponse.getString("content");
                            } else {
                                aiResponse = "Error from server (Error code: " + response.code() + ")";
                            }
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        aiResponse = "Connection error: " + e.getMessage();
                    }

                    final String finalAiResponse = aiResponse;

                    // Cập nhật UI, lưu SQLite và mở khóa lại nút gửi tin nhắn trên Main Thread
                    mainHandler.post(() -> {
                        if (getContext() != null && db != null && db.isOpen()) {
                            ContentValues aiMsgValues = new ContentValues();
                            aiMsgValues.put("sessionId", currentSessionId);
                            aiMsgValues.put("messageText", finalAiResponse);
                            aiMsgValues.put("isUser", 0);
                            aiMsgValues.put("createdAt", System.currentTimeMillis());

                            long aiMsgId = db.insert("ChatMessages", null, aiMsgValues);
                            messageList.add(new ChatMessage(aiMsgId, finalAiResponse, false, false));

                            chatAdapter.notifyItemInserted(messageList.size() - 1);
                            recyclerViewChat.scrollToPosition(messageList.size() - 1);

                            // Cập nhật lại danh sách lịch sử chat bên Drawer khi có câu trả lời mới
                            loadRecentChats();
                        }

                        btnSendMessage.setEnabled(true);
                    });
                });
            }
        });

        // Mở đoạn chat mới
        View btnNewChat = historyDrawerView.findViewById(R.id.btnNewChat);
        btnNewChat.setOnClickListener(v -> {
            currentSessionId = -1;
            messageList.clear();
            chatAdapter.notifyDataSetChanged();
            drawerLayout.closeDrawer(GravityCompat.START);
        });

        // Đóng tab sanh sách lịch sử chat
        View btnCloseDrawer = historyDrawerView.findViewById(R.id.btnCloseDrawer);
        btnCloseDrawer.setOnClickListener(v -> drawerLayout.closeDrawer(GravityCompat.START));

        return rootDrawer;
    }

    // Tách môn học do AI nhận diện, nhận qua phản hồi quả AI
    private String extractSubjectFromResponse(String responseText) {
        if (responseText == null) return "General";
        try {
            Pattern pattern = Pattern.compile("\\[Subject:\\s*(.*?)\\s*\\|");
            Matcher matcher = pattern.matcher(responseText);
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Academic";
    }

    // Helper load danh sách các đoạn chat
    private void loadRecentChats() {
        if (db == null || !db.isOpen()) return;
        recentChatList.clear();
        try {
            Cursor cursor = db.rawQuery(
                    "SELECT s.sessionId, s.sessionTitle, s.createdAt, " +
                            "(SELECT m.messageText FROM ChatMessages m WHERE m.sessionId = s.sessionId AND m.isUser = 0 ORDER BY m.createdAt DESC LIMIT 1) as latestAiMsg " +
                            "FROM ChatSessions s WHERE s.userId = ? ORDER BY s.createdAt DESC",
                    new String[]{String.valueOf(currentUserId)}
            );

            if (cursor.moveToFirst()) {
                do {
                    long id = cursor.getLong(0);
                    String title = cursor.getString(1);
                    long createdAt = cursor.getLong(2);
                    String latestAiMsg = cursor.getString(3);

                    String subjectName = extractSubjectFromResponse(latestAiMsg);
                    subjectRepository.addSubjectIfNotExists(subjectName);

                    recentChatList.add(new ChatSession(id, title, subjectName, createdAt));
                } while (cursor.moveToNext());
            }
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        recentChatAdapter.notifyDataSetChanged();
    }

    // Helper load tin nhắn trong 1 đoạn chat
    private void loadChatMessages(long sessionId) {
        if (db == null || !db.isOpen()) return;
        messageList.clear();
        try {
            Cursor cursor = db.rawQuery("SELECT messageId, messageText, isUser, isBookmarked FROM ChatMessages WHERE sessionId = ? ORDER BY createdAt ASC", new String[]{String.valueOf(sessionId)});

            if (cursor.moveToFirst()) {
                do {
                    long id = cursor.getLong(0);
                    String text = cursor.getString(1);
                    boolean isUser = cursor.getInt(2) == 1;
                    boolean isBookmarked = cursor.getInt(3) == 1;

                    messageList.add(new ChatMessage(id, text, isUser, isBookmarked));
                } while (cursor.moveToNext());
            }
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        chatAdapter.notifyDataSetChanged();

        if (!messageList.isEmpty()) {
            recyclerViewChat.scrollToPosition(messageList.size() - 1);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
}