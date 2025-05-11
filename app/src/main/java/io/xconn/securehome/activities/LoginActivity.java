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
import io.xconn.securehome.models.UserModel;
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

        // Initialize views first, regardless of login status
        initializeViews();
        setupListeners();

        authManager = FirebaseAuthManager.getInstance(this);
        sessionManager = new SessionManager(this);

        // Now check login status after views are initialized
        if (authManager.isUserLoggedIn()) {
            checkUserStatusAndNavigate();
        }
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
        authManager.login(email, password, null, new FirebaseAuthManager.AuthCallback() {
            @Override
            public void onSuccess(UserModel userModel) {
                showLoading(false);
                if (userModel.isApproved() || userModel.isAdmin()) {
                    handleLoginSuccess(userModel);
                } else if (userModel.isPending()) {
                    // User is pending approval, send to pending screen
                    sessionManager.createLoginSession(
                            userModel.getUserId(),
                            userModel.getEmail(),
                            userModel.getRole(),
                            userModel.getApprovalStatus()
                    );
                    navigateToPendingScreen();
                } else {
                    // User was rejected
                    Toast.makeText(LoginActivity.this,
                            "Your account has been rejected. Please contact an administrator.",
                            Toast.LENGTH_LONG).show();
                    authManager.logout(); // Log out since they can't access the app
                }
            }

            @Override
            public void onFailure(Exception e) {
                showLoading(false);
                Toast.makeText(LoginActivity.this,
                        "Authentication failed: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleLoginSuccess(UserModel userModel) {
        sessionManager.createLoginSession(
                userModel.getUserId(),
                userModel.getEmail(),
                userModel.getRole(),
                userModel.getApprovalStatus()
        );
        navigateToMainActivity();
    }

    private void navigateToPendingScreen() {
        Intent intent = new Intent(LoginActivity.this, PendingApprovalActivity.class);
        startActivity(intent);
        finish();
    }

    private void checkUserStatusAndNavigate() {
        showLoading(true);
        authManager.getCurrentUserModel(new FirebaseAuthManager.AuthCallback() {
            @Override
            public void onSuccess(UserModel userModel) {
                showLoading(false);
                if (userModel.isApproved() || userModel.isAdmin()) {
                    // Update session data just in case
                    sessionManager.createLoginSession(
                            userModel.getUserId(),
                            userModel.getEmail(),
                            userModel.getRole(),
                            userModel.getApprovalStatus()
                    );
                    navigateToMainActivity();
                } else if (userModel.isPending()) {
                    // User is pending approval
                    sessionManager.createLoginSession(
                            userModel.getUserId(),
                            userModel.getEmail(),
                            userModel.getRole(),
                            userModel.getApprovalStatus()
                    );
                    navigateToPendingScreen();
                } else {
                    // User was rejected
                    Toast.makeText(LoginActivity.this,
                            "Your account has been rejected. Please contact an administrator.",
                            Toast.LENGTH_LONG).show();
                    authManager.logout(); // Log them out
                }
            }

            @Override
            public void onFailure(Exception e) {
                showLoading(false);
                // Clear session and stay on login screen
                sessionManager.clearSession();
                Toast.makeText(LoginActivity.this,
                        "Error checking user status: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void navigateToMainActivity() {
        startActivity(new Intent(LoginActivity.this, MainActivity.class));
        finish();
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        loginButton.setEnabled(!show);
        emailInput.setEnabled(!show);
        passwordInput.setEnabled(!show);
        registerLink.setEnabled(!show);
    }
}