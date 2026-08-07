package com.example.luminaai.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.luminaai.helpers.EmailValidator;
import com.example.luminaai.helpers.PasswordUtils;
import com.example.luminaai.repository.UserRepository;

import com.example.luminaai.R;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;


public class RegisterActivity extends AppCompatActivity {
    EditText inputFullName, inputRegisterEmail, inputRegisterPassword;
    Button btnSignUp, btnSignUpGoogle;
    TextView tvBackToLogin;

    private UserRepository userRepository;
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private static final int RC_SIGN_IN = 9001;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        inputFullName = findViewById(R.id.inputFullName);
        inputRegisterEmail = findViewById(R.id.inputRegisterEmail);
        inputRegisterPassword = findViewById(R.id.inputRegisterPassword);
        btnSignUp = findViewById(R.id.btnSignUp);
        btnSignUpGoogle = findViewById(R.id.btnSignUpGoogle);
        tvBackToLogin = findViewById(R.id.tvBackToLogin);

        userRepository = new UserRepository(this);
        mAuth = FirebaseAuth.getInstance();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);


        tvBackToLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String fullName = inputFullName.getText().toString().trim();
                String email = inputRegisterEmail.getText().toString().trim();
                String password = inputRegisterPassword.getText().toString().trim();

                if (TextUtils.isEmpty(fullName) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                    Toast.makeText(RegisterActivity.this, "Please fill in all the information!", Toast.LENGTH_SHORT).show();
                    return;
                }

                if(!EmailValidator.isValidEmail(email)){
                    Toast.makeText(RegisterActivity.this, "Invalid email!", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (userRepository.checkEmailExist(email)) {
                    Toast.makeText(RegisterActivity.this, "This email address has already been registered!", Toast.LENGTH_SHORT).show();
                    return;
                }

                String checkPasswordResult = PasswordUtils.checkPasswordStrength(password);
                if (!"Ok".equals(checkPasswordResult)){
                    Toast.makeText(RegisterActivity.this, checkPasswordResult, Toast.LENGTH_SHORT).show();
                    return;
                }

                String hashedPassword = PasswordUtils.hashPassword(password);
                boolean isInserted = userRepository.registerUser(fullName, email, hashedPassword);

                if (isInserted) {
                    Toast.makeText(RegisterActivity.this, "Registration successful!", Toast.LENGTH_SHORT).show();
                    Intent intent;
                    if (userRepository.isUserSetupCompleted(email)) {
                        intent = new Intent(RegisterActivity.this, DashboardActivity.class);
                    } else {
                        intent = new Intent(RegisterActivity.this, UserSetupActivity.class);
                    }
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(RegisterActivity.this, "Registration failed, please try again!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnSignUpGoogle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signInWithGoogleProvider();
            }
        });
    }

    private void signInWithGoogleProvider() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Toast.makeText(this, "Google sign in failed", Toast.LENGTH_SHORT).show();
            }
        }
    }
    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            String email = user.getEmail();
                            String name = user.getDisplayName();
                            String timestamp = String.valueOf(System.currentTimeMillis());
                            String rawGooglePassword = email + timestamp;
                            String hashedGooglePassword = PasswordUtils.hashPassword(rawGooglePassword);

                            userRepository.registerGoogleUserIfNotExists(name, email, hashedGooglePassword);

                            Toast.makeText(RegisterActivity.this, "Registration with Google successful!", Toast.LENGTH_SHORT).show();

                            Intent intent;
                            if (userRepository.isUserSetupCompleted(email)) {
                                intent = new Intent(RegisterActivity.this, DashboardActivity.class);
                            } else {
                                intent = new Intent(RegisterActivity.this, UserSetupActivity.class);
                            }
                            startActivity(intent);
                            finish();
                        }
                    } else {
                        Toast.makeText(RegisterActivity.this, "Google Registration Failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
