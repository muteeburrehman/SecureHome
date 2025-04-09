package io.xconn.securehome.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;

import io.xconn.securehome.MainActivity;
import io.xconn.securehome.R;
import io.xconn.securehome.network.ApiConfig;
import io.xconn.securehome.utils.SessionManager;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {
    private static final int SPLASH_TIMEOUT = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler().postDelayed(() -> {
            Intent intent;

            // First check if server is configured
            if (!ApiConfig.hasServerConfig(this)) {
                // No server configuration yet, go to server discovery screen
                intent = new Intent(SplashActivity.this, ServerDiscoveryActivity.class);
            } else {
                // Server is configured, check login state
                SessionManager sessionManager = new SessionManager(this);
                if (sessionManager.isLoggedIn()) {
                    // User is logged in, go to main activity
                    intent = new Intent(SplashActivity.this, MainActivity.class);
                } else {
                    // User is not logged in, go to login screen
                    intent = new Intent(SplashActivity.this, LoginActivity.class);
                }
            }

            startActivity(intent);
            finish();
        }, SPLASH_TIMEOUT);
    }
}