package io.xconn.securehome.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import io.xconn.securehome.R;
import io.xconn.securehome.api.FirebaseAuthManager;
import io.xconn.securehome.api.FirebaseDatabaseManager;
import io.xconn.securehome.models.AlertModel;
import io.xconn.securehome.models.UserModel;
import io.xconn.securehome.services.EmailService;

/**
 * Activity for sending emergency contact messages
 * These messages will be sent via email and also stored as alerts
 */
public class EmergencyContactActivity extends AppCompatActivity {

    private static final String TAG = "EmergencyContactAct";

    private EditText etEmergencyTitle;
    private EditText etEmergencyDescription;
    private Button btnSendEmergency;
    private ProgressBar progressBar;
    private TextView tvEmergencyHelp;
    private FirebaseAuthManager authManager;
    private FirebaseFirestore db;
    private UserModel currentUser;
    private EmailService emailService;
    private FirebaseDatabaseManager dbManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emergency_contact);

        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.alert));

        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Emergency Contact");
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // Initialize Firebase components
        authManager = FirebaseAuthManager.getInstance(this);
        db = FirebaseFirestore.getInstance();
        emailService = new EmailService(this);
        dbManager = FirebaseDatabaseManager.getInstance();

        // Initialize UI components
        initViews();

        // Get current user
        getCurrentUser();

        // Set up listeners
        setupListeners();
    }

    private void initViews() {
        etEmergencyTitle = findViewById(R.id.et_emergency_title);
        etEmergencyDescription = findViewById(R.id.et_emergency_description);
        btnSendEmergency = findViewById(R.id.btn_send_emergency);
        progressBar = findViewById(R.id.progress_bar);
        tvEmergencyHelp = findViewById(R.id.tv_emergency_help);
    }

    private void getCurrentUser() {
        FirebaseUser firebaseUser = authManager.getCurrentUser();
        if (firebaseUser != null) {
            authManager.getCurrentUserModel(new FirebaseAuthManager.AuthCallback() {
                @Override
                public void onSuccess(UserModel userModel) {
                    currentUser = userModel;
                }

                @Override
                public void onFailure(Exception e) {
                    showError("Failed to get user info: " + e.getMessage());
                }
            });
        } else {
            showError("You must be logged in to use this feature");
            finish();
        }
    }

    private void setupListeners() {
        btnSendEmergency.setOnClickListener(v -> validateAndSubmitEmergency());
    }

    private void validateAndSubmitEmergency() {
        String title = etEmergencyTitle.getText().toString().trim();
        String description = etEmergencyDescription.getText().toString().trim();

        // Validate inputs
        if (TextUtils.isEmpty(title)) {
            etEmergencyTitle.setError("Title is required");
            etEmergencyTitle.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(description)) {
            etEmergencyDescription.setError("Description is required");
            etEmergencyDescription.requestFocus();
            return;
        }

        if (currentUser == null) {
            showError("User information not available");
            return;
        }

        // Show progress and disable button
        progressBar.setVisibility(View.VISIBLE);
        btnSendEmergency.setEnabled(false);

        // Create alert model
        AlertModel alert = new AlertModel(
                null, // ID will be set by Firestore
                title,
                description,
                currentUser.getUserId(),
                currentUser.getDisplayName(),
                System.currentTimeMillis(),
                AlertModel.TYPE_EMERGENCY,
                AlertModel.PRIORITY_HIGH,
                false
        );

        // Save to Firestore
        saveAlertToFirestore(alert);
    }

    private void saveAlertToFirestore(AlertModel alert) {
        db.collection("alerts")
                .add(alert.toMap())
                .addOnSuccessListener(documentReference -> {
                    String alertId = documentReference.getId();

                    // Update alert with the ID
                    documentReference.update("alertId", alertId)
                            .addOnSuccessListener(aVoid -> {
                                // Send email notification to admins
                                sendEmailToAdmins(alert);

                                // Show success message
                                showSuccess("Emergency report submitted successfully");

                                // Reset UI
                                resetForm();
                            })
                            .addOnFailureListener(e -> {
                                showError("Failed to update alert ID: " + e.getMessage());
                                hideProgress();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to save alert: " + e.getMessage(), e);
                    showError("Failed to save alert: " + e.getMessage());
                    hideProgress();
                });
    }

    private void sendEmailToAdmins(AlertModel alert) {
        // Use the Firebase Database Manager instead of directly querying Firestore
        dbManager.getAdminEmails(adminEmails -> {
            if (adminEmails != null && !adminEmails.isEmpty()) {
                Log.d(TAG, "Found " + adminEmails.size() + " admin emails to notify");

                // Send to each admin
                for (String adminEmail : adminEmails) {
                    Log.d(TAG, "Sending emergency alert to admin: " + adminEmail);

                    // Direct method call to email service
                    sendEmergencyEmail(
                            adminEmail,
                            currentUser.getDisplayName(),
                            currentUser.getEmail(),
                            alert.getTitle(),
                            alert.getDescription()
                    );
                }

                // Always notify the default admin email as a fallback
                String defaultAdmin = "muteeb285@gmail.com"; // Hardcoded as in FirebaseDatabaseManager
                if (!adminEmails.contains(defaultAdmin)) {
                    Log.d(TAG, "Also sending to default admin email: " + defaultAdmin);
                    sendEmergencyEmail(
                            defaultAdmin,
                            currentUser.getDisplayName(),
                            currentUser.getEmail(),
                            alert.getTitle(),
                            alert.getDescription()
                    );
                }
            } else {
                Log.w(TAG, "No admin emails found, using fallback");
                // Fallback to direct hardcoded email if no admins found
                sendEmergencyEmail(
                        "muteeb285@gmail.com", // Hardcoded fallback
                        currentUser.getDisplayName(),
                        currentUser.getEmail(),
                        alert.getTitle(),
                        alert.getDescription()
                );
            }
            hideProgress();
        });
    }

    private void sendEmergencyEmail(String adminEmail, String userName, String userEmail,
                                    String alertTitle, String alertDescription) {
        // Try/catch to prevent one email failure from affecting others
        try {
            emailService.sendEmergencyAlertToAdmin(
                    adminEmail,
                    userName,
                    userEmail,
                    alertTitle,
                    alertDescription
            );
            Log.d(TAG, "Emergency email sent to: " + adminEmail);
        } catch (Exception e) {
            Log.e(TAG, "Failed to send emergency email to " + adminEmail + ": " + e.getMessage(), e);
            // Continue with other emails - don't block on failure
        }
    }

    private void resetForm() {
        etEmergencyTitle.setText("");
        etEmergencyDescription.setText("");
        hideProgress();
    }

    private void hideProgress() {
        progressBar.setVisibility(View.GONE);
        btnSendEmergency.setEnabled(true);
    }

    private void showError(String message) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show();
    }

    private void showSuccess(String message) {
        Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG);
        snackbar.getView().setBackgroundColor(getResources().getColor(R.color.colorSuccess));
        snackbar.show();
    }
}