package io.xconn.securehome.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;

import io.xconn.securehome.R;
import io.xconn.securehome.api.FirebaseAuthManager;

public class ChangePasswordActivity extends AppCompatActivity {
    private EditText currentPasswordInput, newPasswordInput, confirmPasswordInput;
    private Button changePasswordButton, cancelButton;
    private ProgressBar progressBar;
    private FirebaseAuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        authManager = FirebaseAuthManager.getInstance();

        // If user is not logged in, go back to login activity
        if (!authManager.isUserLoggedIn()) {
            navigateToLoginActivity();
            return;
        }

        initializeViews();
        setupListeners();
    }

    private void initializeViews() {
        currentPasswordInput = findViewById(R.id.current_password);
        newPasswordInput = findViewById(R.id.new_password);
        confirmPasswordInput = findViewById(R.id.confirm_password);
        changePasswordButton = findViewById(R.id.change_password_btn);
        cancelButton = findViewById(R.id.cancel_btn);
        progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.GONE);
    }

    private void setupListeners() {
        changePasswordButton.setOnClickListener(v -> validateAndChangePassword());
        cancelButton.setOnClickListener(v -> finish());
    }

    private void validateAndChangePassword() {
        String currentPassword = currentPasswordInput.getText().toString().trim();
        String newPassword = newPasswordInput.getText().toString().trim();
        String confirmPassword = confirmPasswordInput.getText().toString().trim();

        // Validate inputs
        if (TextUtils.isEmpty(currentPassword)) {
            currentPasswordInput.setError("Current password is required");
            currentPasswordInput.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(newPassword)) {
            newPasswordInput.setError("New password is required");
            newPasswordInput.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(confirmPassword)) {
            confirmPasswordInput.setError("Confirm password is required");
            confirmPasswordInput.requestFocus();
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            confirmPasswordInput.setError("Passwords don't match");
            confirmPasswordInput.requestFocus();
            return;
        }

        // Minimum password length validation
        if (newPassword.length() < 6) {
            newPasswordInput.setError("Password must be at least 6 characters");
            newPasswordInput.requestFocus();
            return;
        }

        // Check if new password is different from the current password
        if (currentPassword.equals(newPassword)) {
            newPasswordInput.setError("New password must be different from current password");
            newPasswordInput.requestFocus();
            return;
        }

        showLoading(true);
        changePassword(currentPassword, newPassword);
    }

    private void changePassword(String currentPassword, String newPassword) {
        FirebaseUser user = authManager.getCurrentUser();
        if (user == null) {
            showLoading(false);
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get auth credentials from the user for re-authentication
        String email = user.getEmail();
        if (email == null) {
            showLoading(false);
            Toast.makeText(this, "Unable to retrieve user email", Toast.LENGTH_SHORT).show();
            return;
        }

        AuthCredential credential = EmailAuthProvider.getCredential(email, currentPassword);

        // Re-authenticate the user
        user.reauthenticate(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // User re-authenticated successfully, update password
                        updatePassword(user, newPassword);
                    } else {
                        showLoading(false);
                        Toast.makeText(ChangePasswordActivity.this,
                                "Current password is incorrect",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updatePassword(FirebaseUser user, String newPassword) {
        user.updatePassword(newPassword)
                .addOnCompleteListener(task -> {
                    showLoading(false);
                    if (task.isSuccessful()) {
                        Toast.makeText(ChangePasswordActivity.this,
                                "Password updated successfully",
                                Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(ChangePasswordActivity.this,
                                "Failed to update password: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void navigateToLoginActivity() {
        startActivity(new Intent(ChangePasswordActivity.this, LoginActivity.class));
        finish();
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        changePasswordButton.setEnabled(!show);
        cancelButton.setEnabled(!show);
    }
}