package io.xconn.securehome.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import io.xconn.securehome.R;
import io.xconn.securehome.api.FirebaseAuthManager;
import io.xconn.securehome.utils.SessionManager;

public class PendingApprovalActivity extends AppCompatActivity {

    private TextView messageText;
    private Button logoutButton;
    private SessionManager sessionManager;
    private FirebaseAuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pending_approval);

        sessionManager = new SessionManager(this);
        authManager = FirebaseAuthManager.getInstance(this);

        initializeViews();
        setupListeners();

        String email = sessionManager.getUserEmail();
        messageText.setText("Your account (" + email + ") is pending approval from an administrator. " +
                "You will be able to access the app once your account has been approved.");
    }

    private void initializeViews() {
        messageText = findViewById(R.id.pending_message);
        logoutButton = findViewById(R.id.logout_button);
    }

    private void setupListeners() {
        logoutButton.setOnClickListener(v -> {
            authManager.logout();
            sessionManager.clearSession();
            startActivity(new Intent(PendingApprovalActivity.this, LoginActivity.class));
            finish();
        });
    }
}