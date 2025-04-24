package io.xconn.securehome.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.List;
import java.util.Map;

import io.xconn.securehome.R;
import io.xconn.securehome.adapters.DeviceAdapter;
import io.xconn.securehome.models.Device;
import io.xconn.securehome.repository.DeviceRepository;
import io.xconn.securehome.repository.ScheduleRepository;
import io.xconn.securehome.utils.ScheduleCheckerUtility;
import io.xconn.securehome.utils.ServerCheckUtility;

public class DeviceListActivity extends AppCompatActivity implements
        DeviceAdapter.OnDeviceListener,
        DeviceRepository.OnStatusUpdateListener {

    private RecyclerView recyclerView;
    private DeviceAdapter adapter;
    private ProgressBar progressBar;
    private Button btnAddDevice, btnAddFirstDevice;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView tvHomeOwner, tvEmptyDevices;
    private DeviceRepository deviceRepository;
    private int homeId;
    private String homeOwner;

    // UI elements for device counters
    private TextView tvActiveDevices, tvInactiveDevices, tvScheduledDevices;
    private View layoutEmptyState, layoutLoadingState;
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Check if server is configured first
        if (!ServerCheckUtility.checkServerConfigured(this)) {
            // The utility will handle redirection, so we just need to finish this activity
            finish();
            return;
        }
        setContentView(R.layout.activity_device_list);

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
        tvHomeOwner.setText(String.format("Home: %s", homeOwner));
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
        Intent intent = new Intent(DeviceListActivity.this, AddDeviceActivity.class);
        intent.putExtra("HOME_ID", homeId);
        intent.putExtra("HOME_OWNER", homeOwner);
        startActivity(intent);
    }

    private void observeViewModel() {
        // Observe devices
        deviceRepository.getDevices().observe(this, devices -> {
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
        });

        // Observe loading state
        deviceRepository.getIsLoading().observe(this, isLoading -> {
            layoutLoadingState.setVisibility(isLoading ? View.VISIBLE : View.GONE);

            if (!isLoading) {
                swipeRefreshLayout.setRefreshing(false);
            }
        });

        // Observe error messages
        deviceRepository.getErrorMessage().observe(this, errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });

        // Observe status update messages
        deviceRepository.getStatusUpdateMessage().observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateDeviceCounters(List<Device> devices) {
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
    }

    private void checkDevicesWithSchedules(List<Device> devices) {
        // Show loading state
        ScheduleCheckerUtility.checkDevicesWithSchedules(this, homeId, devices,
                new ScheduleCheckerUtility.ScheduleCheckListener() {
                    @Override
                    public void onAllDevicesChecked(Map<Integer, Boolean> deviceScheduleMap) {
                        // Count devices with schedules
                        int scheduledCount = (int) deviceScheduleMap.values().stream().filter(hasSchedules -> hasSchedules).count();

                        // Update the UI on the main thread
                        runOnUiThread(() -> {
                            tvScheduledDevices.setText(String.valueOf(scheduledCount));
                        });
                    }

                    @Override
                    public void onError(String message) {
                        // Just log the error, don't show to user since it's not critical
                        runOnUiThread(() -> {
                            tvScheduledDevices.setText("?");
                        });
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh the list when coming back to this screen
        deviceRepository.fetchDevices(homeId);
    }

    @Override
    public void onDeviceToggle(Device device, boolean newStatus) {
        deviceRepository.updateDeviceStatus(homeId, device.getId(), newStatus, this);
    }

    @Override
    public void onDeviceSchedule(Device device) {
        Intent intent = new Intent(this, DeviceScheduleActivity.class);
        intent.putExtra("HOME_ID", homeId);
        intent.putExtra("DEVICE_ID", device.getId());
        intent.putExtra("DEVICE_NAME", device.getName());
        startActivity(intent);
    }

    @Override
    public void onStatusUpdated(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        // Refresh the devices to update the counters
        deviceRepository.fetchDevices(homeId);
    }

    @Override
    public void onError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}