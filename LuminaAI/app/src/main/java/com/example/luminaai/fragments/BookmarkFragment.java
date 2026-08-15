package com.example.luminaai.fragments;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.luminaai.R;
import com.example.luminaai.activities.DashboardActivity;
import com.example.luminaai.adapters.BookmarkAdapter;
import com.example.luminaai.helpers.SessionManager;
import com.example.luminaai.models.BookmarkItem;
import com.example.luminaai.repository.UserRepository;
import com.example.luminaai.sqlite.DbHelper;

import java.util.ArrayList;
import java.util.List;

public class BookmarkFragment extends Fragment {

    private RecyclerView recyclerView;
    private TextView tvNoBookmarks;
    private BookmarkAdapter adapter;
    private List<BookmarkItem> bookmarkList;

    private DbHelper dbHelper;
    private SQLiteDatabase db;
    private int currentUserId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_bookmark, container, false);

        recyclerView = view.findViewById(R.id.recyclerBookmarks);
        tvNoBookmarks = view.findViewById(R.id.tvNoBookmarks);

        dbHelper = new DbHelper(requireContext());
        try {
            db = dbHelper.getWritableDatabase();
        } catch (Exception e) {
            e.printStackTrace();
        }

        SessionManager sessionManager = new SessionManager(requireContext());
        UserRepository userRepository = new UserRepository(requireContext());
        currentUserId = userRepository.getUserIdByEmail(sessionManager.getEmail());

        bookmarkList = new ArrayList<>();
        adapter = new BookmarkAdapter(bookmarkList, new BookmarkAdapter.OnBookmarkListener() {
            @Override
            public void onRemoveBookmark(BookmarkItem item, int position) {
                if (db != null && db.isOpen()) {
                    android.content.ContentValues values = new android.content.ContentValues();
                    values.put("isBookmarked", 0);

                    // Kiểm tra xem item bị hủy bookmark là Chat hay Quiz dựa vào type hoặc ID
                    if ("quiz".equals(item.getType())) {
                        db.update("QuizQuestions", values, "questionId = ?", new String[]{String.valueOf(item.getMessageId())});
                    } else {
                        db.update("ChatMessages", values, "messageId = ?", new String[]{String.valueOf(item.getMessageId())});
                    }
                }

                bookmarkList.remove(position);
                adapter.notifyItemRemoved(position);
                checkEmptyState();

                Toast.makeText(getContext(), "Removed from bookmarks", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onOpenChatSession(BookmarkItem item) {
                // Nếu là Quiz bookmark thì có thể hiển thị thông báo hoặc xử lý riêng, còn nếu là chat thì mở session chat
                if ("quiz".equals(item.getType())) {
                    Toast.makeText(getContext(), "This is a saved quiz question.", Toast.LENGTH_SHORT).show();
                    return;
                }

                Bundle bundle = new Bundle();
                bundle.putLong("SELECTED_SESSION_ID", item.getSessionId());

                ChatFragment chatFragment = new ChatFragment();
                chatFragment.setArguments(bundle);

                DashboardActivity activity = (DashboardActivity) requireActivity();
                activity.selectBottomNavItem(R.id.nav_chat);

                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, chatFragment)
                        .addToBackStack(null)
                        .commit();
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        loadBookmarkedData();

        return view;
    }

    // Load danh sách bookmark (bao gồm cả Chat và Quiz Questions)
    private void loadBookmarkedData() {
        if (db == null || !db.isOpen()) return;
        bookmarkList.clear();

        try {
            // 1. Lấy Chat Messages được Bookmark
            String chatQuery = "SELECT m.messageId, m.sessionId, s.sessionTitle, m.messageText, m.createdAt " +
                    "FROM ChatMessages m " +
                    "INNER JOIN ChatSessions s ON m.sessionId = s.sessionId " +
                    "WHERE s.userId = ? AND m.isBookmarked = 1";

            Cursor chatCursor = db.rawQuery(chatQuery, new String[]{String.valueOf(currentUserId)});
            if (chatCursor.moveToFirst()) {
                do {
                    long msgId = chatCursor.getLong(0);
                    long sessionId = chatCursor.getLong(1);
                    String sessionTitle = chatCursor.getString(2);
                    String messageText = chatCursor.getString(3);
                    long createdAt = chatCursor.getLong(4);

                    BookmarkItem item = new BookmarkItem(msgId, sessionId, sessionTitle, messageText, createdAt);
                    item.setType("chat"); // Đánh dấu loại chat
                    bookmarkList.add(item);
                } while (chatCursor.moveToNext());
            }
            chatCursor.close();

            // 2. Lấy Quiz Questions được Bookmark từ bảng QuizQuestions và QuizSessions
            String quizQuery = "SELECT q.questionId, q.quizSessionId, s.topic, q.questionText, s.createdAt " +
                    "FROM QuizQuestions q " +
                    "INNER JOIN QuizSessions s ON q.quizSessionId = s.quizSessionId " +
                    "WHERE s.userId = ? AND q.isBookmarked = 1";

            Cursor quizCursor = db.rawQuery(quizQuery, new String[]{String.valueOf(currentUserId)});
            if (quizCursor.moveToFirst()) {
                do {
                    long qId = quizCursor.getLong(0);
                    long sessionQuizId = quizCursor.getLong(1);
                    String topic = quizCursor.getString(2);
                    String questionText = "[Quiz - " + topic + "] " + quizCursor.getString(3);
                    long createdAt = quizCursor.getLong(4);

                    BookmarkItem item = new BookmarkItem(qId, sessionQuizId, "Quiz Review: " + topic, questionText, createdAt);
                    item.setType("quiz"); // Đánh dấu loại quiz để adapter hoặc hàm xóa nhận biết
                    bookmarkList.add(item);
                } while (quizCursor.moveToNext());
            }
            quizCursor.close();

            // Sắp xếp danh sách gộp theo thời gian mới nhất lên đầu
            bookmarkList.sort((o1, o2) -> Long.compare(o2.getCreatedAt(), o1.getCreatedAt()));

        } catch (Exception e) {
            e.printStackTrace();
        }

        adapter.notifyDataSetChanged();
        checkEmptyState();
    }

    private void checkEmptyState() {
        if (bookmarkList.isEmpty()) {
            tvNoBookmarks.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            tvNoBookmarks.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }
}