package io.xconn.securehome.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import io.xconn.securehome.R;
import io.xconn.securehome.api.RetrofitClient;
import io.xconn.securehome.api.response.ServerInfoResponse;
import io.xconn.securehome.network.ApiConfig;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ServerConfigActivity extends AppCompatActivity {
    private static final String TAG = "ServerConfigActivity";

    private EditText serverIpInput, serverPortInput;
    private Button connectButton;
    private ProgressBar progressBar;
    private TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server_config);

        // Check if we already have a server configuration
        if (ApiConfig.hasServerConfig(this)) {
            // Attempt to validate existing config before proceeding
            testExistingServerConnection();
        }

        initializeViews();
        setupListeners();
    }

    private void initializeViews() {
        serverIpInput = findViewById(R.id.server_ip);
        serverPortInput = findViewById(R.id.server_port);
        connectButton = findViewById(R.id.connect_button);
        progressBar = findViewById(R.id.progressBar);
        statusText = findViewById(R.id.status_text);

        // Set default port
        serverPortInput.setText("8000");
        progressBar.setVisibility(View.GONE);
    }

    private void setupListeners() {
        connectButton.setOnClickListener(v -> attemptServerConnection());
    }

    private void attemptServerConnection() {
        String ip = serverIpInput.getText().toString().trim();
        String portStr = serverPortInput.getText().toString().trim();

        if (ip.isEmpty()) {
            serverIpInput.setError("Server IP is required");
            serverIpInput.requestFocus();
            return;
        }

        int port;
        try {
            port = Integer.parseInt(portStr);
            if (port <= 0 || port > 65535) {
                throw new NumberFormatException("Port out of range");
            }
        } catch (NumberFormatException e) {
            serverPortInput.setError("Enter a valid port number (1-65535)");
            serverPortInput.requestFocus();
            return;
        }

        // Save the server info temporarily
        ApiConfig.saveServerInfo(this, ip, port);

        // Show loading
        showLoading(true);
        statusText.setText("Connecting to server...");

        // Create a temporary instance of RetrofitClient with the provided URL
        String baseUrl = "http://" + ip + ":" + port + "/";
        RetrofitClient client = RetrofitClient.createInstance(baseUrl);

        // Test the connection by fetching server info
        client.getApi().getServerInfo().enqueue(new Callback<ServerInfoResponse>() {
            @Override
            public void onResponse(@NonNull Call<ServerInfoResponse> call, @NonNull Response<ServerInfoResponse> response) {
                showLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    ServerInfoResponse serverInfo = response.body();
                    handleSuccessfulConnection(serverInfo);
                } else {
                    handleConnectionFailure("Server responded with an error. Check the URL and try again.");
                }
            }

            @Override
            public void onFailure(@NonNull Call<ServerInfoResponse> call, @NonNull Throwable t) {
                showLoading(false);
                handleConnectionFailure("Could not connect to server: " + t.getMessage());
                Log.e(TAG, "Connection failure", t);
            }
        });
    }

    private void testExistingServerConnection() {
        showLoading(true);
        statusText.setText("Checking existing server connection...");

        try {
            RetrofitClient.getInstance(this).getApi().getServerInfo()
                    .enqueue(new Callback<ServerInfoResponse>() {
                        @Override
                        public void onResponse(@NonNull Call<ServerInfoResponse> call,
                                               @NonNull Response<ServerInfoResponse> response) {
                            showLoading(false);

                            if (response.isSuccessful() && response.body() != null) {
                                // Existing connection works, proceed to login
                                navigateToLoginActivity();
                            } else {
                                // Server responded but with error, may need reconfiguration
                                statusText.setText("Existing server configuration needs update.");
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<ServerInfoResponse> call, @NonNull Throwable t) {
                            showLoading(false);
                            statusText.setText("Cannot connect with saved settings. Please reconfigure.");
                            Log.e(TAG, "Failed to connect with existing config", t);
                        }
                    });
        } catch (IllegalStateException e) {
            // This can happen if the baseUrl is corrupt somehow
            showLoading(false);
            statusText.setText("Invalid server configuration. Please reconfigure.");
            ApiConfig.clearServerConfig(this);
        }
    }

    private void handleSuccessfulConnection(ServerInfoResponse serverInfo) {
        String message = "Connected to server: " + serverInfo.getServer_hostname();
        statusText.setText(message);
        Toast.makeText(this, "Server connection successful!", Toast.LENGTH_SHORT).show();

        // Now we have verified the server is working, save the config permanently
        String ip = serverIpInput.getText().toString().trim();
        int port = Integer.parseInt(serverPortInput.getText().toString().trim());
        ApiConfig.saveServerInfo(this, ip, port);

        // Proceed to login screen
        navigateToLoginActivity();
    }

    private void handleConnectionFailure(String errorMessage) {
        statusText.setText("Connection failed: " + errorMessage);
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();

        // Clear the temporary config
        ApiConfig.clearServerConfig(this);
    }

    private void navigateToLoginActivity() {
        Intent intent = new Intent(ServerConfigActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        connectButton.setEnabled(!show);
        serverIpInput.setEnabled(!show);
        serverPortInput.setEnabled(!show);
    }
}