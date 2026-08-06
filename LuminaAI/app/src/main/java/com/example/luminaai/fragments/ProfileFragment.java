package com.example.luminaai.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.luminaai.R;
import com.example.luminaai.activities.LoginActivity;
import com.example.luminaai.activities.UserSetupActivity;
import com.example.luminaai.helpers.SessionManager;
import com.example.luminaai.repository.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.android.material.button.MaterialButton;

public class ProfileFragment extends Fragment {

    private TextView tvProfileName, tvProfileEmail, tvProfileEduLevel, tvProfileExpStyle;
    private MaterialButton btnEditPreferences, btnLogout;

    private UserRepository userRepository;
    private SessionManager sessionManager;
    private String currentUserEmail;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        tvProfileName = view.findViewById(R.id.tvProfileName);
        tvProfileEmail = view.findViewById(R.id.tvProfileEmail);
        tvProfileEduLevel = view.findViewById(R.id.tvProfileEduLevel);
        tvProfileExpStyle = view.findViewById(R.id.tvProfileExpStyle);
        btnEditPreferences = view.findViewById(R.id.btnEditPreferences);
        btnLogout = view.findViewById(R.id.btnLogout);

        userRepository = new UserRepository(requireContext());
        sessionManager = new SessionManager(requireContext());

        currentUserEmail = sessionManager.getEmail();

        // Load thông tin người dùng lên giao diện
        loadUserData();

        // Xử lý sự kiện chỉnh sửa thông tin (Tự nguyện - bật cờ EDIT_MODE)
        btnEditPreferences.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), UserSetupActivity.class);
            intent.putExtra("EDIT_MODE", true);
            startActivity(intent);
        });

        // Xử lý sự kiện đăng xuất
        btnLogout.setOnClickListener(v -> {
            sessionManager.logoutUser();
            FirebaseAuth.getInstance().signOut();

            Toast.makeText(getContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(getContext(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            if (getActivity() != null) {
                getActivity().finish();
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUserData();
    }

    private void loadUserData() {
        if (currentUserEmail != null && !currentUserEmail.isEmpty()) {
            String fullName = userRepository.getUserFullName(currentUserEmail);
            String eduLevel = userRepository.getUserEduLevel(currentUserEmail);
            String expStyle = userRepository.getUserExpStyle(currentUserEmail);

            tvProfileName.setText(fullName);
            tvProfileEmail.setText(currentUserEmail);
            tvProfileEduLevel.setText(eduLevel);
            tvProfileExpStyle.setText(expStyle);
        } else {
            tvProfileName.setText("Student");
            tvProfileEmail.setText("No email found");
            tvProfileEduLevel.setText("Not set");
            tvProfileExpStyle.setText("Not set");
        }
    }
}