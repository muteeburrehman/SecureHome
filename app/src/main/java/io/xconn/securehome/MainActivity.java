package io.xconn.securehome;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.view.Menu;
import android.view.MenuItem;
import androidx.appcompat.app.AppCompatActivity;
import io.xconn.securehome.activities.LoginActivity;
import io.xconn.securehome.activities.RecognitionActivity;
import io.xconn.securehome.activities.RegisterActivity;
import io.xconn.securehome.utils.SessionManager;

public class MainActivity extends AppCompatActivity {
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sessionManager = new SessionManager(this);
        if (!sessionManager.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        Button registerBtn = findViewById(R.id.buttonregister);
        Button recognizeBtn = findViewById(R.id.buttonrecognize);

        registerBtn.setOnClickListener(view -> startActivity(new Intent(
                MainActivity.this, RegisterActivity.class)));

        recognizeBtn.setOnClickListener(view -> startActivity(new Intent(
                MainActivity.this, RecognitionActivity.class)));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            logout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void logout() {
        sessionManager.clearSession();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}