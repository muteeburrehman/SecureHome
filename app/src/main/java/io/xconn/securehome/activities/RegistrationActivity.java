package io.xconn.securehome.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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

public class RegistrationActivity extends AppCompatActivity {
    private EditText fullNameInput, emailInput, passwordInput;
    private Button registerButton;
    private TextView loginLink;
    private ProgressBar progressBar;
    private FirebaseAuthManager authManager;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        authManager = FirebaseAuthManager.getInstance();
        sessionManager = new SessionManager(this); // Initialize SessionManager

        initializeViews();
        setupListeners();
    }

    private void initializeViews() {
        fullNameInput = findViewById(R.id.full_name);
        emailInput = findViewById(R.id.reg_email);
        passwordInput = findViewById(R.id.reg_password);
        registerButton = findViewById(R.id.create_acc);
        loginLink = findViewById(R.id.loginbtn);
        progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.GONE);
    }

    private void setupListeners() {
        registerButton.setOnClickListener(v -> performRegistration());
        loginLink.setOnClickListener(v -> {
            finish(); // Return to login activity
        });
    }

    private void performRegistration() {
        String fullName = fullNameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        // Input validation
        if (fullName.isEmpty()) {
            fullNameInput.setError("Name is required");
            fullNameInput.requestFocus();
            return;
        }

        if (email.isEmpty()) {
            emailInput.setError("Email is required");
            emailInput.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            passwordInput.setError("Password is required");
            passwordInput.requestFocus();
            return;
        }

        if (password.length() < 6) {
            passwordInput.setError("Password must be at least 6 characters");
            passwordInput.requestFocus();
            return;
        }

        showLoading(true);

        authManager.register(email, password, fullName, null, new FirebaseAuthManager.AuthCallback() {
            @Override
            public void onSuccess(UserModel userModel) {
                // Call showLoading(false) IMMEDIATELY
                showLoading(false);
                // Log for debugging
                Log.d("RegistrationActivity", "Registration onSuccess. User: " + userModel.getEmail() +
                        ", Approved: " + userModel.isApproved() +
                        ", Admin: " + userModel.isAdmin() +
                        ", Status: " + userModel.getApprovalStatus());

                sessionManager.createLoginSession(
                        userModel.getUserId(),
                        userModel.getEmail(),
                        userModel.getRole(),
                        userModel.getApprovalStatus()
                );

                if (userModel.isApproved() || userModel.isAdmin()) {
                    Toast.makeText(RegistrationActivity.this,
                            "Account created successfully. You can now login.",
                            Toast.LENGTH_LONG).show();
                    Log.d("RegistrationActivity", "Navigating to MainActivity.");
                    Intent intent = new Intent(RegistrationActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish(); // Finish RegistrationActivity
                } else { // User is pending
                    // The Toast here might be slightly redundant if PendingApprovalActivity shows a clear message.
                    // You can keep it for immediate feedback or remove it if PendingApprovalActivity is sufficient.
                    Toast.makeText(RegistrationActivity.this,
                            "Registration successful. Your account is pending admin approval.",
                            Toast.LENGTH_LONG).show();
                    Log.d("RegistrationActivity", "Navigating to PendingApprovalActivity.");
                    Intent intent = new Intent(RegistrationActivity.this, PendingApprovalActivity.class);
                    startActivity(intent);
                    finish(); // Finish RegistrationActivity
                }
            }

            @Override
            public void onFailure(Exception e) {
                // Call showLoading(false) IMMEDIATELY
                showLoading(false);
                Log.e("RegistrationActivity", "Registration onFailure", e);
                showErrorMessage("Registration failed: " + e.getMessage());
            }
        });
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        registerButton.setEnabled(!isLoading);
        fullNameInput.setEnabled(!isLoading);
        emailInput.setEnabled(!isLoading);
        passwordInput.setEnabled(!isLoading);
        loginLink.setEnabled(!isLoading);
    }

    private void showErrorMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}