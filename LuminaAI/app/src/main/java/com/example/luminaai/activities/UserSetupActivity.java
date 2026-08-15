package com.example.luminaai.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.luminaai.R;
import com.example.luminaai.helpers.SessionManager;
import com.example.luminaai.repository.UserRepository;
import com.google.android.material.button.MaterialButton;

public class UserSetupActivity extends AppCompatActivity {

    AutoCompleteTextView autoCompleteEduLevel, autoCompleteExpStyle;
    MaterialButton btnSaveSetup;
    TextView tvSkipSetup;

    private UserRepository userRepository;
    private SessionManager sessionManager;
    private boolean isEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_setup);

        autoCompleteEduLevel = findViewById(R.id.autoCompleteEduLevel);
        autoCompleteExpStyle = findViewById(R.id.autoCompleteExpStyle);
        btnSaveSetup = findViewById(R.id.btnSaveSetup);
        tvSkipSetup = findViewById(R.id.tvSkipSetup);

        userRepository = new UserRepository(this);
        sessionManager = new SessionManager(this);

        isEditMode = getIntent().getBooleanExtra("EDIT_MODE", false);


        String[] eduLevels = new String[]{"Middle School", "High School", "University"};
        ArrayAdapter<String> eduAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, eduLevels);
        autoCompleteEduLevel.setAdapter(eduAdapter);

        String[] expStyles = new String[]{"Short", "Detailed", "Step-by-step"};
        ArrayAdapter<String> styleAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, expStyles);
        autoCompleteExpStyle.setAdapter(styleAdapter);


        String currentUserEmail = sessionManager.getEmail();
        if (isEditMode) {
            btnSaveSetup.setText("Update Changes");

            if (tvSkipSetup != null) {
                tvSkipSetup.setVisibility(View.VISIBLE);
                tvSkipSetup.setText("Cancel");
            }

        } else {
            if (tvSkipSetup != null) {
                tvSkipSetup.setVisibility(View.VISIBLE);
                tvSkipSetup.setText("Back to login");
            }
        }

        btnSaveSetup.setOnClickListener(v -> {
            String eduLevel = autoCompleteEduLevel.getText().toString().trim();
            String expStyle = autoCompleteExpStyle.getText().toString().trim();

            if (eduLevel.isEmpty() || expStyle.isEmpty()) {
                Toast.makeText(this, "Please select both options!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!currentUserEmail.isEmpty()) {
                boolean isUpdated = userRepository.updateUserPreferences(currentUserEmail, eduLevel, expStyle);

                if (isUpdated) {
                    Toast.makeText(this, isEditMode ? "Preferences updated successfully!" : "Preferences saved successfully!", Toast.LENGTH_SHORT).show();

                    if (isEditMode) {
                        finish();
                    } else {
                        Intent intent = new Intent(UserSetupActivity.this, DashboardActivity.class);
                        startActivity(intent);
                        finish();
                    }
                } else {
                    Toast.makeText(this, "Failed to save preferences. Please try again.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Session expired! Please login again.", Toast.LENGTH_SHORT).show();
                sessionManager.logoutUser();
                Intent intent = new Intent(UserSetupActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });

        if (tvSkipSetup != null) {
            tvSkipSetup.setOnClickListener(v -> {
                if (isEditMode) {
                    finish();
                } else {
                    sessionManager.logoutUser();
                    com.google.firebase.auth.FirebaseAuth.getInstance().signOut();

                    Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(UserSetupActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                }
            });
        }
    }
}