package io.xconn.securehome.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import io.xconn.securehome.R;
import io.xconn.securehome.api.FirebaseAuthManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserProfileChangeRequest;

public class RegistrationActivity extends AppCompatActivity {
    private EditText fullNameInput, emailInput, passwordInput;
    private Button registerButton;
    private TextView loginLink;
    private ProgressBar progressBar;
    private FirebaseAuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        authManager = FirebaseAuthManager.getInstance();
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

        if (fullName.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);
        authManager.register(email, password, fullName, task -> {
            showLoading(false);
            if (task.isSuccessful()) {
                Toast.makeText(RegistrationActivity.this,
                        "Registration successful. Please login.", Toast.LENGTH_LONG).show();
                finish(); // Return to login activity
            } else {
                Toast.makeText(RegistrationActivity.this,
                        "Registration failed: " + task.getException().getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        registerButton.setEnabled(!show);
    }
}