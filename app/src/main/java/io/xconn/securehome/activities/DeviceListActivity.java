package io.xconn.securehome.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.snackbar.Snackbar;

import java.util.List;
import java.util.Map;

import io.xconn.securehome.R;
import io.xconn.securehome.adapters.DeviceAdapter;
import io.xconn.securehome.models.Device;
import io.xconn.securehome.repository.DeviceRepository;
import io.xconn.securehome.utils.NetworkChangeReceiver;
import io.xconn.securehome.utils.ScheduleCheckerUtility;
import io.xconn.securehome.utils.ServerCheckUtility;

public class DeviceListActivity extends AppCompatActivity implements
        DeviceAdapter.OnDeviceListener,
        DeviceRepository.OnStatusUpdateListener,
        NetworkChangeReceiver.NetworkChangeListener {

    private static final String TAG = "DeviceListActivity";

    private RecyclerView recyclerView;
    private DeviceAdapter adapter;
    private ProgressBar progressBar;
    private Button btnAddDevice, btnAddFirstDevice;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView tvHomeOwner, tvEmptyDevices;
    private DeviceRepository deviceRepository;
    private int homeId;
    private String homeOwner;
    private NetworkChangeReceiver networkChangeReceiver;
    private boolean isNetworkCheckPending = false;
    private boolean isActivityInitialized = false;

    // UI elements for device counters
    private TextView tvActiveDevices, tvInactiveDevices, tvScheduledDevices;
    private View layoutEmptyState, layoutLoadingState;
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            // Setup network change receiver
            networkChangeReceiver = new NetworkChangeReceiver(this, this);

            // Use lifecycle observers if available
            try {
                ProcessLifecycleOwner.get().getLifecycle().addObserver(networkChangeReceiver);
            } catch (Exception e) {
                Log.e(TAG, "Error adding lifecycle observer, using manual registration", e);
                // Fallback - manually register the receiver
                networkChangeReceiver.register();
            }

            // Check if server is configured first
            if (!ServerCheckUtility.checkServerConfigured(this)) {
                // The utility will handle redirection, so we just need to mark pending check
                isNetworkCheckPending = true;
                return;
            }

            setContentView(R.layout.activity_device_list);
            initializeActivity();
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            Toast.makeText(this, "Error initializing app", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initializeActivity() {
        try {
            // Get home ID from intent
            homeId = getIntent().getIntExtra("HOME_ID", -1);
            homeOwner = getIntent().getStringExtra("HOME_OWNER");

            if (homeId == -1) {
                Toast.makeText(this, "Invalid home ID", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            deviceRepository = new DeviceRepository(this);

            // Initialize UI components
            initializeUI();

            // Setup app bar
            setupAppBar();

            // Setup RecyclerView
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            adapter = new DeviceAdapter(this);
            recyclerView.setAdapter(adapter);

            // Setup click listeners
            setupClickListeners();

            // Setup SwipeRefreshLayout
            swipeRefreshLayout.setOnRefreshListener(() -> {
                deviceRepository.fetchDevices(homeId);
            });

            // Observe LiveData
            observeViewModel();

            // Initial load
            deviceRepository.fetchDevices(homeId);

            isActivityInitialized = true;
        } catch (Exception e) {
            Log.e(TAG, "Error in initializeActivity", e);
            Toast.makeText(this, "Error initializing device list", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initializeUI() {
        tvHomeOwner = findViewById(R.id.tvHomeOwner);
        recyclerView = findViewById(R.id.recyclerViewDevices);
        progressBar = findViewById(R.id.progressBar);
        btnAddDevice = findViewById(R.id.btnAddDevice);
        btnAddFirstDevice = findViewById(R.id.btnAddFirstDevice);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        tvEmptyDevices = findViewById(R.id.tvEmptyDevices);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);
        layoutLoadingState = findViewById(R.id.layoutLoadingState);
        toolbar = findViewById(R.id.toolbar);

        // Initialize counter views
        tvActiveDevices = findViewById(R.id.tvActiveDevices);
        tvInactiveDevices = findViewById(R.id.tvInactiveDevices);
        tvScheduledDevices = findViewById(R.id.tvScheduledDevices);
    }

    private void setupAppBar() {
        // Set up the toolbar
        setSupportActionBar(toolbar);

        // Remove default title
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        // Set home owner text in the app bar
        if (homeOwner != null) {
            tvHomeOwner.setText(String.format("Home: %s", homeOwner));
        } else {
            tvHomeOwner.setText("Home");
        }
    }

    private void setupClickListeners() {
        // Setup click listener for add device button
        btnAddDevice.setOnClickListener(v -> navigateToAddDevice());

        // Setup click listener for add first device button (in empty state)
        btnAddFirstDevice.setOnClickListener(v -> navigateToAddDevice());

        // Setup FAB if it exists
        View fabAddDevice = findViewById(R.id.fabAddDevice);
        if (fabAddDevice != null) {
            fabAddDevice.setOnClickListener(v -> navigateToAddDevice());
        }
    }

    private void navigateToAddDevice() {
        try {
            Intent intent = new Intent(DeviceListActivity.this, AddDeviceActivity.class);
            intent.putExtra("HOME_ID", homeId);
            intent.putExtra("HOME_OWNER", homeOwner);
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error navigating to add device", e);
            Toast.makeText(this, "Error opening add device screen", Toast.LENGTH_SHORT).show();
        }
    }

    private void observeViewModel() {
        // Observe devices
        deviceRepository.getDevices().observe(this, devices -> {
            try {
                adapter.setDevices(devices);

                // Update UI state based on devices list
                if (devices.isEmpty()) {
                    layoutEmptyState.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    layoutEmptyState.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                }

                // Update device counters for active/inactive
                updateDeviceCounters(devices);

                // Check which devices have schedules
                if (!devices.isEmpty()) {
                    checkDevicesWithSchedules(devices);
                } else {
                    tvScheduledDevices.setText("0");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error updating device list", e);
            }
        });

        // Observe loading state
        deviceRepository.getIsLoading().observe(this, isLoading -> {
            try {
                layoutLoadingState.setVisibility(isLoading ? View.VISIBLE : View.GONE);

                if (!isLoading) {
                    swipeRefreshLayout.setRefreshing(false);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error updating loading state", e);
            }
        });

        // Observe error messages
        deviceRepository.getErrorMessage().observe(this, errorMessage -> {
            try {
                if (errorMessage != null && !errorMessage.isEmpty()) {
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error showing error message", e);
            }
        });

        // Observe status update messages
        deviceRepository.getStatusUpdateMessage().observe(this, message -> {
            try {
                if (message != null && !message.isEmpty()) {
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error showing status update message", e);
            }
        });
    }

    private void updateDeviceCounters(List<Device> devices) {
        try {
            int activeCount = 0;
            int inactiveCount = 0;

            for (Device device : devices) {
                if (device.isStatus()) {
                    activeCount++;
                } else {
                    inactiveCount++;
                }
            }

            // Update the UI
            tvActiveDevices.setText(String.valueOf(activeCount));
            tvInactiveDevices.setText(String.valueOf(inactiveCount));
        } catch (Exception e) {
            Log.e(TAG, "Error updating device counters", e);
        }
    }

    private void checkDevicesWithSchedules(List<Device> devices) {
        try {
            // Show loading state
            ScheduleCheckerUtility.checkDevicesWithSchedules(this, homeId, devices,
                    new ScheduleCheckerUtility.ScheduleCheckListener() {
                        @Override
                        public void onAllDevicesChecked(Map<Integer, Boolean> deviceScheduleMap) {
                            try {
                                // Count devices with schedules
                                int scheduledCount = 0;
                                for (Boolean hasSchedules : deviceScheduleMap.values()) {
                                    if (hasSchedules) {
                                        scheduledCount++;
                                    }
                                }

                                // Update the UI on the main thread
                                final int finalCount = scheduledCount;
                                runOnUiThread(() -> {
                                    try {
                                        tvScheduledDevices.setText(String.valueOf(finalCount));
                                    } catch (Exception e) {
                                        Log.e(TAG, "Error updating scheduled devices count", e);
                                    }
                                });
                            } catch (Exception e) {
                                Log.e(TAG, "Error processing schedule check results", e);
                            }
                        }

                        @Override
                        public void onError(String message) {
                            // Just log the error, don't show to user since it's not critical
                            Log.e(TAG, "Schedule check error: " + message);
                            runOnUiThread(() -> {
                                try {
                                    tvScheduledDevices.setText("?");
                                } catch (Exception e) {
                                    Log.e(TAG, "Error updating scheduled devices count on error", e);
                                }
                            });
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error checking devices with schedules", e);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        try {
            // If we're returning from ServerDiscoveryActivity, check again
            if (isNetworkCheckPending) {
                isNetworkCheckPending = false;
                // Check if server is now configured
                if (ServerCheckUtility.checkServerConfigured(this)) {
                    // Now initialize the activity if it hasn't been initialized yet
                    if (!isActivityInitialized) {
                        setContentView(R.layout.activity_device_list);
                        initializeActivity();
                    }
                } else {
                    // Still not configured, we'll wait
                    isNetworkCheckPending = true;
                    return;
                }
            }

            // Refresh the list when coming back to this screen
            if (deviceRepository != null) {
                deviceRepository.fetchDevices(homeId);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onResume", e);
        }
    }

    @Override
    public void onDeviceToggle(Device device, boolean newStatus) {
        try {
            if (deviceRepository != null) {
                deviceRepository.updateDeviceStatus(homeId, device.getId(), newStatus, this);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error toggling device", e);
            Toast.makeText(this, "Error toggling device status", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDeviceSchedule(Device device) {
        try {
            Intent intent = new Intent(this, DeviceScheduleActivity.class);
            intent.putExtra("HOME_ID", homeId);
            intent.putExtra("DEVICE_ID", device.getId());
            intent.putExtra("DEVICE_NAME", device.getName());
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error navigating to device schedule", e);
            Toast.makeText(this, "Error opening schedule screen", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onStatusUpdated(String message) {
        try {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            // Refresh the devices to update the counters
            if (deviceRepository != null) {
                deviceRepository.fetchDevices(homeId);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling status update", e);
        }
    }

    @Override
    public void onError(String message) {
        try {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing error message", e);
        }
    }

    @Override
    public void onNetworkChanged(boolean isConnected) {
        try {
            if (isConnected) {
                // When network comes back, verify server configuration
                if (ServerCheckUtility.isServerConfigured(this)) {
                    // If we're in a pending state, this will be handled in onResume
                    // Otherwise, refresh the data
                    if (!isNetworkCheckPending && deviceRepository != null && isActivityInitialized) {
                        deviceRepository.fetchDevices(homeId);
                    }
                } else if (isActivityInitialized) {
                    // Server not configured, show snackbar message
                    showNetworkMessage("Server not configured. Please set up server connection.");
                    // Redirect to server discovery
                    ServerCheckUtility.checkServerConfigured(this);
                }
            } else if (isActivityInitialized) {
                // Show a message to the user about the network disconnection
                showNetworkMessage("Network connection lost. Device controls may not work.");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling network change", e);
        }
    }

    private void showNetworkMessage(String message) {
        try {
            View contentView = findViewById(android.R.id.content);
            if (contentView != null) {
                Snackbar.make(
                        contentView,
                        message,
                        Snackbar.LENGTH_LONG
                ).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error showing network message", e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Clean up network receiver
        try {
            ProcessLifecycleOwner.get().getLifecycle().removeObserver(networkChangeReceiver);
        } catch (NoClassDefFoundError e) {
            // Manually unregister if we used the fallback method
            if (networkChangeReceiver != null) {
                networkChangeReceiver.unregister();
            }
        }
    }
}