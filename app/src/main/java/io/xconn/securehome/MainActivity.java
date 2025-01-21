package io.xconn.securehome;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;



public class MainActivity extends AppCompatActivity {


    Button registerBtn,recognizeBtn;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        registerBtn = findViewById(R.id.buttonregister);
        recognizeBtn = findViewById(R.id.buttonrecognize);

        registerBtn.setOnClickListener(view -> startActivity(new Intent(
                MainActivity.this, RegisterActivity.class)));

        recognizeBtn.setOnClickListener(view -> startActivity(new Intent(
                MainActivity.this, RecognitionActivity.class)));
    }
}