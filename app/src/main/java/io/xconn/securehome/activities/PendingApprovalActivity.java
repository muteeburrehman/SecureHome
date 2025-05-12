package io.xconn.securehome.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import io.xconn.securehome.R;
import io.xconn.securehome.api.FirebaseAuthManager;
import io.xconn.securehome.utils.SessionManager;

public class PendingApprovalActivity extends AppCompatActivity {

    private TextView messageText;
    private SessionManager sessionManager;
    private FirebaseAuthManager authManager;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pending_approval);

        sessionManager = new SessionManager(this);
        authManager = FirebaseAuthManager.getInstance(this);
        handler = new Handler(Looper.getMainLooper());

        initializeViews();

        String email = sessionManager.getUserEmail();
        messageText.setText("Your account (" + email + ") is pending approval from an administrator. " +
                "You will be automatically logged out.");

        // Schedule automatic logout after 5 seconds
        handler.postDelayed(this::performAutoLogout, 5000);
    }

    private void initializeViews() {
        messageText = findViewById(R.id.pending_message);
    }

    private void performAutoLogout() {
        authManager.logout();
        sessionManager.clearSession();
        startActivity(new Intent(PendingApprovalActivity.this, LoginActivity.class));
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove any pending callbacks to prevent memory leaks
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
    }
}