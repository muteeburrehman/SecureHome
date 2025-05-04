package io.xconn.securehome.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import java.util.regex.Pattern;

import io.xconn.securehome.R;
import io.xconn.securehome.api.RetrofitClient;
import io.xconn.securehome.api.response.ServerInfoResponse;
import io.xconn.securehome.network.ApiConfig;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ServerConfigActivity extends AppCompatActivity {
    private static final String TAG = "ServerConfigActivity";
    private static final int DEFAULT_PORT = 8000;

    private EditText ipAddressEditText;
    private EditText portEditText;
    private Button connectButton;
    private Button testConnectionButton;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server_config);

        // Set up action bar with back button
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle("Server Configuration");
        }

        initializeViews();
        loadExistingConfig();
        setupListeners();
    }

    private void initializeViews() {
        ipAddressEditText = findViewById(R.id.ip_address_edit_text);
        portEditText = findViewById(R.id.port_edit_text);
        connectButton = findViewById(R.id.connect_button);
        testConnectionButton = findViewById(R.id.test_connection_button);
        progressBar = findViewById(R.id.progress_bar);
    }

    private void loadExistingConfig() {
        // Check if we already have a saved configuration
        if (ApiConfig.hasServerConfig(this)) {
            String baseUrl = ApiConfig.getBaseUrl(this);
            if (baseUrl != null && baseUrl.startsWith("http://")) {
                // Parse IP and port from baseUrl
                // Format: http://ip:port/
                String urlWithoutProtocol = baseUrl.substring(7); // Remove "http://"
                String urlWithoutTrailingSlash = urlWithoutProtocol.substring(0, urlWithoutProtocol.length() - 1); // Remove trailing slash

                String[] parts = urlWithoutTrailingSlash.split(":");
                if (parts.length == 2) {
                    ipAddressEditText.setText(parts[0]);
                    portEditText.setText(parts[1]);
                }
            }
        } else {
            // Set default port if no existing configuration
            portEditText.setText(String.valueOf(DEFAULT_PORT));
        }
    }

    private void setupListeners() {
        connectButton.setOnClickListener(v -> saveServerConfig());
        testConnectionButton.setOnClickListener(v -> testConnection());
    }

    private void saveServerConfig() {
        String ipAddress = ipAddressEditText.getText().toString().trim();
        String portStr = portEditText.getText().toString().trim();

        if (!validateInput(ipAddress, portStr)) {
            return;
        }

        int port = Integer.parseInt(portStr);

        // Save the configuration
        ApiConfig.saveServerInfo(this, ipAddress, port);
        Toast.makeText(this, "Server configuration saved", Toast.LENGTH_SHORT).show();

        // Proceed to login screen
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    private void testConnection() {
        String ipAddress = ipAddressEditText.getText().toString().trim();
        String portStr = portEditText.getText().toString().trim();

        if (!validateInput(ipAddress, portStr)) {
            return;
        }

        int port = Integer.parseInt(portStr);

        showLoading(true);

        // Create a temporary Retrofit client for testing
        String url = "http://" + ipAddress + ":" + port + "/";
        RetrofitClient client = RetrofitClient.createInstance(url);

        client.getApi().getServerInfo().enqueue(new Callback<ServerInfoResponse>() {
            @Override
            public void onResponse(@NonNull Call<ServerInfoResponse> call,
                                   @NonNull Response<ServerInfoResponse> response) {
                showLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    ServerInfoResponse serverInfo = response.body();
                    String message = "Connected successfully to " + serverInfo.getServer_hostname();
                    Toast.makeText(ServerConfigActivity.this, message, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ServerConfigActivity.this,
                            "Connection failed: Server responded with an error",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ServerInfoResponse> call, @NonNull Throwable t) {
                showLoading(false);
                Toast.makeText(ServerConfigActivity.this,
                        "Connection failed: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean validateInput(String ipAddress, String portStr) {
        // Validate IP address
        if (TextUtils.isEmpty(ipAddress)) {
            ipAddressEditText.setError("IP address is required");
            return false;
        }

        // Simple IP address format validation
        Pattern pattern = Pattern.compile("^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$");
        if (!pattern.matcher(ipAddress).matches()) {
            ipAddressEditText.setError("Invalid IP address format");
            return false;
        }

        // Validate port
        if (TextUtils.isEmpty(portStr)) {
            portEditText.setError("Port is required");
            return false;
        }

        try {
            int port = Integer.parseInt(portStr);
            if (port <= 0 || port > 65535) {
                portEditText.setError("Port must be between 1 and 65535");
                return false;
            }
        } catch (NumberFormatException e) {
            portEditText.setError("Invalid port number");
            return false;
        }

        return true;
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        connectButton.setEnabled(!show);
        testConnectionButton.setEnabled(!show);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}