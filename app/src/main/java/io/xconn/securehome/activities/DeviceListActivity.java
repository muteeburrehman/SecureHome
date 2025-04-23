package io.xconn.securehome.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import io.xconn.securehome.R;
import io.xconn.securehome.adapters.DeviceAdapter;
import io.xconn.securehome.models.Device;
import io.xconn.securehome.repository.DeviceRepository;
import io.xconn.securehome.activities.DeviceScheduleActivity;

public class DeviceListActivity extends AppCompatActivity implements
        DeviceAdapter.OnDeviceListener,
        DeviceRepository.OnStatusUpdateListener {

    private RecyclerView recyclerView;
    private DeviceAdapter adapter;
    private ProgressBar progressBar;
    private Button btnAddDevice;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView tvHomeOwner, tvEmptyDevices;
    private TextView tvActiveDevices, tvInactiveDevices, tvScheduledDevices;
    private DeviceRepository deviceRepository;
    private int homeId;
    private String homeOwner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        tvHomeOwner = findViewById(R.id.tvHomeOwner);
        recyclerView = findViewById(R.id.recyclerViewDevices);
        progressBar = findViewById(R.id.progressBar);
        btnAddDevice = findViewById(R.id.btnAddDevice);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        tvEmptyDevices = findViewById(R.id.tvEmptyDevices);

        // Initialize device count TextViews
        tvActiveDevices = findViewById(R.id.tvActiveDevices);
        tvInactiveDevices = findViewById(R.id.tvInactiveDevices);
        tvScheduledDevices = findViewById(R.id.tvScheduledDevices);

        // Set home owner text
        tvHomeOwner.setText(String.format("Home: %s", homeOwner));

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DeviceAdapter(this);
        recyclerView.setAdapter(adapter);

        // Setup click listener for add device button
        btnAddDevice.setOnClickListener(v -> {
            Intent intent = new Intent(DeviceListActivity.this, AddDeviceActivity.class);
            intent.putExtra("HOME_ID", homeId);
            intent.putExtra("HOME_OWNER", homeOwner);
            startActivity(intent);
        });

        // Setup SwipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener(() -> {
            deviceRepository.fetchDevices(homeId);
        });

        // Observe LiveData
        observeViewModel();

        // Initial load
        deviceRepository.fetchDevices(homeId);
    }

    private void observeViewModel() {
        // Observe devices
        deviceRepository.getDevices().observe(this, devices -> {
            adapter.setDevices(devices);

            // Calculate counts
            int activeCount = 0;
            int inactiveCount = 0;
            int scheduledCount = 0;

            for (Device device : devices) {
                if (device.isStatus()) {
                    activeCount++;
                } else {
                    inactiveCount++;
                }

                if (device.hasSchedules()) {
                    scheduledCount++;
                }
            }

            // Update UI with counts
            tvActiveDevices.setText(String.valueOf(activeCount));
            tvInactiveDevices.setText(String.valueOf(inactiveCount));
            tvScheduledDevices.setText(String.valueOf(scheduledCount));

            if (devices.isEmpty()) {
                tvEmptyDevices.setVisibility(View.VISIBLE);
            } else {
                tvEmptyDevices.setVisibility(View.GONE);
            }
        });

        // Observe loading state
        deviceRepository.getIsLoading().observe(this, isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);

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
    }

    @Override
    public void onError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}