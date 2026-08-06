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
                // Hủy bookmark thực tế trong SQLite (cập nhật isBookmarked = 0)
                if (db != null && db.isOpen()) {
                    android.content.ContentValues values = new android.content.ContentValues();
                    values.put("isBookmarked", 0);
                    db.update("ChatMessages", values, "messageId = ?", new String[]{String.valueOf(item.getMessageId())});
                }

                // Xóa khỏi danh sách giao diện và cập nhật lại RecyclerView
                bookmarkList.remove(position);
                adapter.notifyItemRemoved(position);
                checkEmptyState();

                Toast.makeText(getContext(), "Removed from bookmarks", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onOpenChatSession(BookmarkItem item) {
                // Trỏ đến đoạn chat tương ứng: Chuyển sang ChatFragment và truyền sessionId qua Bundle
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

    private void loadBookmarkedData() {
        if (db == null || !db.isOpen()) return;
        bookmarkList.clear();

        try {
            // Truy vấn lấy các tin nhắn được bookmark của user hiện tại thông qua bảng ChatSessions
            String query = "SELECT m.messageId, m.sessionId, s.sessionTitle, m.messageText, m.createdAt " +
                    "FROM ChatMessages m " +
                    "INNER JOIN ChatSessions s ON m.sessionId = s.sessionId " +
                    "WHERE s.userId = ? AND m.isBookmarked = 1 " +
                    "ORDER BY m.createdAt DESC";

            Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(currentUserId)});

            if (cursor.moveToFirst()) {
                do {
                    long msgId = cursor.getLong(0);
                    long sessionId = cursor.getLong(1);
                    String sessionTitle = cursor.getString(2);
                    String messageText = cursor.getString(3);
                    long createdAt = cursor.getLong(4);

                    bookmarkList.add(new BookmarkItem(msgId, sessionId, sessionTitle, messageText, createdAt));
                } while (cursor.moveToNext());
            }
            cursor.close();
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