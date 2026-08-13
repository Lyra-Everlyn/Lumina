package com.example.luminaai.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.luminaai.R;
import com.example.luminaai.helpers.SessionManager;
import com.example.luminaai.repository.UserRepository; // Nhớ thêm dòng import này

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                SessionManager sessionManager = new SessionManager(SplashActivity.this);
                UserRepository userRepository = new UserRepository(SplashActivity.this);
                Intent intent;

                if (sessionManager.isLoggedIn()) {
                    String email = sessionManager.getEmail();

                    if (email != null && !email.isEmpty()) {
                        if (userRepository.isUserSetupCompleted(email)) {
                            intent = new Intent(SplashActivity.this, DashboardActivity.class);
                        } else {
                            intent = new Intent(SplashActivity.this, UserSetupActivity.class);
                        }
                    } else {
                        sessionManager.logoutUser();
                        intent = new Intent(SplashActivity.this, LoginActivity.class);
                    }
                } else {
                    intent = new Intent(SplashActivity.this, LoginActivity.class);
                }

                startActivity(intent);
                finish();
            }
        }, 1500);
    }
}