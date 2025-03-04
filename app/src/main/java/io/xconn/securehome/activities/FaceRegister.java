package io.xconn.securehome.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import io.xconn.securehome.R;

public class FaceRegister extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_register);

        Button registerButton = findViewById(R.id.buttonregister);
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to RecognitionActivity
                Intent intent = new Intent(FaceRegister.this, RegisterActivity.class);
                startActivity(intent);
            }
        });
    }
}
