package com.example.luminaai.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.luminaai.R;
import com.example.luminaai.adapters.RecentChatAdapter;
import com.example.luminaai.helpers.SessionManager;
import com.example.luminaai.models.ChatSession;
import com.example.luminaai.repository.UserRepository;

import java.util.List;

public class HomeFragment extends Fragment {

    private TextView tvHelloUser, tvQuestionsCount, tvNoRecentChats;
    private RecyclerView recyclerRecentHome;
    private UserRepository userRepository;
    private SessionManager sessionManager;
    private RecentChatAdapter recentChatAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        userRepository = new UserRepository(requireContext());
        sessionManager = new SessionManager(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        tvHelloUser = view.findViewById(R.id.tvHelloUser);
        tvQuestionsCount = view.findViewById(R.id.tvQuestionsCount);
        tvNoRecentChats = view.findViewById(R.id.tvNoRecentChats);
        recyclerRecentHome = view.findViewById(R.id.recyclerRecentHome);

        recyclerRecentHome.setLayoutManager(new LinearLayoutManager(getContext()));

        loadUserData();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUserData();
    }

    private void loadUserData() {
        String email = sessionManager.getEmail();
        if (email != null && !email.isEmpty()) {

            String fullName = userRepository.getUserFullName(email);
            tvHelloUser.setText("Hello, " + fullName);

            int totalQuestions = userRepository.getTotalQuestionsAsked(email);
            tvQuestionsCount.setText(String.valueOf(totalQuestions));

            List<ChatSession> recentChatList = userRepository.getRecentChatSessions(email, 3);

            if (recentChatList.isEmpty()) {
                recyclerRecentHome.setVisibility(View.GONE);
                tvNoRecentChats.setVisibility(View.VISIBLE);
            } else {
                recyclerRecentHome.setVisibility(View.VISIBLE);
                tvNoRecentChats.setVisibility(View.GONE);

                recentChatAdapter = new RecentChatAdapter(recentChatList, session -> {
                });
                recyclerRecentHome.setAdapter(recentChatAdapter);
            }
        } else {
            tvHelloUser.setText("Hello, Student");
            tvQuestionsCount.setText("0");
            recyclerRecentHome.setVisibility(View.GONE);
            tvNoRecentChats.setVisibility(View.VISIBLE);
        }
    }
}