package io.xconn.securehome.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import io.xconn.securehome.R;
import io.xconn.securehome.api.AuthManager;
import io.xconn.securehome.api.response.RegisterResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegistrationActivity extends AppCompatActivity {
    private EditText fullNameInput, emailInput, phoneInput, passwordInput;
    private Button registerButton;
    private TextView loginLink;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        initializeViews();
        setupListeners();
    }

    private void initializeViews() {
        fullNameInput = findViewById(R.id.full_name);
        emailInput = findViewById(R.id.reg_email);
        phoneInput = findViewById(R.id.phone);
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
        String phone = phoneInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (fullName.isEmpty() || email.isEmpty() || phone.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);
        AuthManager.getInstance().register(RegistrationActivity.this, fullName, email, phone, password, new Callback<RegisterResponse>() {
            @Override
            public void onResponse(@NonNull Call<RegisterResponse> call, @NonNull Response<RegisterResponse> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(RegistrationActivity.this,
                            "Registration successful. Please login.", Toast.LENGTH_LONG).show();
                    finish(); // Return to login activity
                } else {
                    Toast.makeText(RegistrationActivity.this,
                            "Registration failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<RegisterResponse> call, @NonNull Throwable t) {
                showLoading(false);
                Toast.makeText(RegistrationActivity.this,
                        "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        registerButton.setEnabled(!show);
    }
}
