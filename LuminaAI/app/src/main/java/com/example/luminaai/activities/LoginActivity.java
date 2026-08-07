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

import com.example.luminaai.helpers.PasswordUtils;
import com.example.luminaai.helpers.SessionManager;
import com.example.luminaai.repository.UserRepository;
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

import com.example.luminaai.R;

public class LoginActivity extends AppCompatActivity {
    EditText inputLoginEmail, inputLoginPassword;
    Button btnLogin, btnLoginGoogle;
    TextView tvSignUpAccount;

    private UserRepository userRepository;
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private static final int RC_SIGN_IN = 9001;
    private SessionManager sessionManager;


    @Override
    protected void onCreate(@Nullable Bundle saveInstanceState){
        super.onCreate(saveInstanceState);
        setContentView(R.layout.activity_login);

        inputLoginEmail = findViewById(R.id.inputLoginEmail);
        inputLoginPassword = findViewById(R.id.inputLoginPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnLoginGoogle = findViewById(R.id.btnLoginGoogle);
        tvSignUpAccount = findViewById(R.id.tvSignUpAccount);

        userRepository = new UserRepository(this);
        mAuth = FirebaseAuth.getInstance();
        sessionManager = new SessionManager(this);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);


        tvSignUpAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent signup = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(signup);
            }
        });

        btnLoginGoogle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signInWithGoogle();
            }
        });

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = inputLoginEmail.getText().toString().trim();
                String password = inputLoginPassword.getText().toString().trim();

                if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                    Toast.makeText(LoginActivity.this, "Please enter your full email and password!", Toast.LENGTH_SHORT).show();
                    return;
                }

                String hashedPassword = PasswordUtils.hashPassword(password);
                boolean emailExist = userRepository.checkEmailExist(email);

                if (!emailExist){
                    Toast.makeText(LoginActivity.this, "The account does not exist.", Toast.LENGTH_SHORT).show();
                }

                if (userRepository.checkUserLogin(email, hashedPassword)) {
                    sessionManager.createLoginSession(email);
                    Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();

                    Intent intent;
                    if (userRepository.isUserSetupCompleted(email)) {
                        intent = new Intent(LoginActivity.this, DashboardActivity.class);
                    } else {
                        intent = new Intent(LoginActivity.this, UserSetupActivity.class);
                    }
                    startActivity(intent);
                    finish();
                }
                else {
                    Toast.makeText(LoginActivity.this, "Incorrect password!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void signInWithGoogle() {
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

                            sessionManager.createLoginSession(email);
                            Toast.makeText(LoginActivity.this, "Welcome " + name, Toast.LENGTH_SHORT).show();

                            Intent intent;
                            if (userRepository.isUserSetupCompleted(email)) {
                                intent = new Intent(LoginActivity.this, DashboardActivity.class);
                            } else {
                                intent = new Intent(LoginActivity.this, UserSetupActivity.class);
                            }
                            startActivity(intent);
                            finish();
                        }
                    } else {
                        Toast.makeText(LoginActivity.this, "Authentication Failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}