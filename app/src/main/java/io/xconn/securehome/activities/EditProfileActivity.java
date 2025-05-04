package io.xconn.securehome.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

import io.xconn.securehome.R;
import io.xconn.securehome.api.FirebaseAuthManager;

public class EditProfileActivity extends AppCompatActivity {
    private EditText fullNameInput, emailInput;
    private Button updateProfileButton, cancelButton;
    private ProgressBar progressBar;
    private FirebaseAuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        authManager = FirebaseAuthManager.getInstance();

        // If user is not logged in, go back to login activity
        if (!authManager.isUserLoggedIn()) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        loadUserData();
        setupListeners();
    }

    private void initializeViews() {
        fullNameInput = findViewById(R.id.full_name);
        emailInput = findViewById(R.id.email);
        updateProfileButton = findViewById(R.id.update_profile_btn);
        cancelButton = findViewById(R.id.cancel_btn);
        progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.GONE);
    }

    private void loadUserData() {
        FirebaseUser currentUser = authManager.getCurrentUser();
        if (currentUser != null) {
            // Set the current display name
            if (currentUser.getDisplayName() != null) {
                fullNameInput.setText(currentUser.getDisplayName());
            }

            // Set the current email
            if (currentUser.getEmail() != null) {
                emailInput.setText(currentUser.getEmail());
                // Email is harder to change in Firebase, so disable it
                emailInput.setEnabled(false);
                emailInput.setAlpha(0.7f);
            }
        }
    }

    private void setupListeners() {
        updateProfileButton.setOnClickListener(v -> validateAndUpdateProfile());
        cancelButton.setOnClickListener(v -> finish());
    }

    private void validateAndUpdateProfile() {
        String fullName = fullNameInput.getText().toString().trim();

        if (TextUtils.isEmpty(fullName)) {
            fullNameInput.setError("Name is required");
            fullNameInput.requestFocus();
            return;
        }

        showLoading(true);
        updateUserProfile(fullName);
    }

    private void updateUserProfile(String fullName) {
        FirebaseUser user = authManager.getCurrentUser();
        if (user == null) {
            showLoading(false);
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(fullName)
                .build();

        user.updateProfile(profileUpdates)
                .addOnCompleteListener(task -> {
                    showLoading(false);
                    if (task.isSuccessful()) {
                        Toast.makeText(EditProfileActivity.this,
                                "Profile updated successfully",
                                Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(EditProfileActivity.this,
                                "Failed to update profile: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        updateProfileButton.setEnabled(!show);
        cancelButton.setEnabled(!show);
    }
}