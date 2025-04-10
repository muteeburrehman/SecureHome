package io.xconn.securehome.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import io.xconn.securehome.MainActivity;
import io.xconn.securehome.R;
import io.xconn.securehome.api.FirebaseAuthManager;
import io.xconn.securehome.utils.SessionManager;

public class LoginActivity extends AppCompatActivity {
    private EditText emailInput, passwordInput;
    private Button loginButton;
    private TextView registerLink;
    private ProgressBar progressBar;
    private SessionManager sessionManager;
    private FirebaseAuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        authManager = FirebaseAuthManager.getInstance();
        sessionManager = new SessionManager(this);

        // If user is already logged in, go straight to main activity
        if (authManager.isUserLoggedIn()) {
            navigateToMainActivity();
            return;
        }

        initializeViews();
        setupListeners();
    }

    private void initializeViews() {
        emailInput = findViewById(R.id.email);
        passwordInput = findViewById(R.id.password);
        loginButton = findViewById(R.id.loginbtn);
        registerLink = findViewById(R.id.registerText);
        progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.GONE);
    }

    private void setupListeners() {
        loginButton.setOnClickListener(v -> performLogin());
        registerLink.setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, RegistrationActivity.class)));
    }

    private void performLogin() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            emailInput.setError("Email is required");
            emailInput.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            passwordInput.setError("Password is required");
            passwordInput.requestFocus();
            return;
        }

        showLoading(true);
        authManager.login(email, password, task -> {
            showLoading(false);
            if (task.isSuccessful()) {
                handleLoginSuccess(email);
            } else {
                Toast.makeText(LoginActivity.this,
                        "Authentication failed: " + task.getException().getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleLoginSuccess(String email) {
        sessionManager.setLoggedIn(true);
        sessionManager.saveUserEmail(email);
        navigateToMainActivity();
    }

    private void navigateToMainActivity() {
        startActivity(new Intent(LoginActivity.this, MainActivity.class));
        finish();
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        loginButton.setEnabled(!show);
    }
}