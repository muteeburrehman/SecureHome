package io.xconn.securehome.activities;

import android.content.Intent;
import android.os.Bundle;


import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import io.xconn.securehome.R;

public class FaceRegister extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_register);

        // Initialize the register button
        MaterialButton registerButton = findViewById(R.id.buttonRegister);
        registerButton.setOnClickListener(view -> {
            // Navigate to RegisterActivity using lambda expression for cleaner code
            Intent intent = new Intent(FaceRegister.this, RegisterActivity.class);
            startActivity(intent);
        });


    }

    // Optional: Handle back button press
    @Override
    public void onBackPressed() {
        // You could override this method to customize back button behavior
        // For example, show a confirmation dialog before exiting
        super.onBackPressed();
    }
}