package io.xconn.securehome.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import io.xconn.securehome.R;
import io.xconn.securehome.adapters.ServerAdapter;
import io.xconn.securehome.api.RetrofitClient;
import io.xconn.securehome.api.response.ServerInfoResponse;
import io.xconn.securehome.models.ServerInfo;
import io.xconn.securehome.network.ApiConfig;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ServerDiscoveryActivity extends AppCompatActivity implements ServerAdapter.ServerSelectionListener {
    private static final String TAG = "ServerDiscovery";
    private static final int DEFAULT_PORT = 8000;
    private static final int SCAN_TIMEOUT = 5000; // 5 seconds timeout

    private ProgressBar progressBar;
    private TextView statusText;
    private Button rescanButton;
    private RecyclerView serversRecyclerView;
    private ServerAdapter serverAdapter;
    private List<ServerInfo> discoveredServers = new ArrayList<>();

    private ExecutorService executorService;
    private Handler uiHandler = new Handler();
    private boolean isScanning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server_discovery);

        initializeViews();
        setupRecyclerView();
        setupListeners();

        // Start scanning immediately
        startNetworkScan();
    }

    private void initializeViews() {
        progressBar = findViewById(R.id.progressBar);
        statusText = findViewById(R.id.status_text);
        rescanButton = findViewById(R.id.rescan_button);
        serversRecyclerView = findViewById(R.id.servers_recycler_view);
    }

    private void setupRecyclerView() {
        serverAdapter = new ServerAdapter(discoveredServers, this);
        serversRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        serversRecyclerView.setAdapter(serverAdapter);
    }

    private void setupListeners() {
        rescanButton.setOnClickListener(v -> startNetworkScan());
    }

    private void startNetworkScan() {
        if (isScanning) return;

        isScanning = true;
        discoveredServers.clear();
        serverAdapter.notifyDataSetChanged();

        showLoading(true);
        statusText.setText("Scanning network for servers...");

        // Get subnet from local IP
        String subnet = getLocalSubnet();
        if (subnet == null) {
            statusText.setText("Could not determine local network. Please check WiFi connection.");
            showLoading(false);
            isScanning = false;
            return;
        }

        // Create executor for parallel scanning
        executorService = Executors.newFixedThreadPool(10);

        // Count how many IPs we've checked
        AtomicInteger checkedIpCount = new AtomicInteger(0);

        // Start scanning 254 possible hosts on subnet
        for (int i = 1; i <= 254; i++) {
            final String ipToCheck = subnet + "." + i;

            executorService.execute(() -> {
                checkServerAtIp(ipToCheck);

                // Update progress on UI thread
                int checked = checkedIpCount.incrementAndGet();
                uiHandler.post(() -> {
                    statusText.setText("Scanning: " + checked + "/254 IPs checked");

                    // When all IPs are checked
                    if (checked >= 254) {
                        finishScanning();
                    }
                });
            });
        }

        // Set timeout to stop scanning
        uiHandler.postDelayed(this::finishScanning, SCAN_TIMEOUT);
    }

    private void finishScanning() {
        if (!isScanning) return;

        // Stop scanning
        isScanning = false;

        if (executorService != null) {
            executorService.shutdownNow();
        }

        uiHandler.post(() -> {
            showLoading(false);

            if (discoveredServers.isEmpty()) {
                statusText.setText("No SecureHome servers found on the network. " +
                        "Make sure the server is running and connected to the same WiFi network.");
            } else {
                statusText.setText("Found " + discoveredServers.size() +
                        " possible servers. Please select one to connect:");
            }
        });
    }

    private void checkServerAtIp(String ip) {
        try {
            String url = "http://" + ip + ":" + DEFAULT_PORT + "/";
            RetrofitClient client = RetrofitClient.createInstance(url);

            client.getApi().getServerInfo().enqueue(new Callback<ServerInfoResponse>() {
                @Override
                public void onResponse(@NonNull Call<ServerInfoResponse> call,
                                       @NonNull Response<ServerInfoResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        ServerInfoResponse serverInfo = response.body();
                        // Valid server found
                        ServerInfo server = new ServerInfo(
                                serverInfo.getServer_hostname(),
                                ip,
                                DEFAULT_PORT
                        );

                        // Update UI on main thread
                        uiHandler.post(() -> {
                            discoveredServers.add(server);
                            serverAdapter.notifyDataSetChanged();

                            // Update status text
                            statusText.setText("Found " + discoveredServers.size() +
                                    " possible server(s). Please select one to connect:");
                        });
                    }
                }

                @Override
                public void onFailure(@NonNull Call<ServerInfoResponse> call, @NonNull Throwable t) {
                    // Not a valid server, just ignore
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error checking IP: " + ip, e);
        }
    }

    private String getLocalSubnet() {
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();

                // Skip loopback and inactive interfaces
                if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                    continue;
                }

                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    String hostAddress = address.getHostAddress();

                    // Find IPv4 address (not IPv6)
                    if (!hostAddress.contains(":")) {
                        // Extract subnet (first three octets)
                        String[] parts = hostAddress.split("\\.");
                        if (parts.length == 4) {
                            return parts[0] + "." + parts[1] + "." + parts[2];
                        }
                    }
                }
            }
        } catch (SocketException e) {
            Log.e(TAG, "Error getting network interfaces", e);
        }

        return null;
    }

    @Override
    public void onServerSelected(ServerInfo server) {
        // Save the selected server configuration
        ApiConfig.saveServerInfo(this, server.getIpAddress(), server.getPort());
        Toast.makeText(this, "Connected to server: " + server.getHostname(), Toast.LENGTH_SHORT).show();

        // Proceed to login screen
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        rescanButton.setEnabled(!show);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdownNow();
        }
    }
}