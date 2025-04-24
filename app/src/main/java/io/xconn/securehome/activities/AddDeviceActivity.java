package io.xconn.securehome.activities;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import com.google.android.material.switchmaterial.SwitchMaterial;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

import io.xconn.securehome.R;
import io.xconn.securehome.models.Device;
import io.xconn.securehome.repository.DeviceRepository;

public class AddDeviceActivity extends AppCompatActivity implements DeviceRepository.OnDeviceAddedListener {
    private static final int MAX_DEVICES_ALLOWED = 21;

    private EditText etDeviceName;
    private SwitchMaterial switchInitialStatus;
    private Button btnAddDevice;
    private ProgressBar progressBar;
    private TextView tvHomeInfo;
    private DeviceRepository deviceRepository;
    private int homeId;
    private String homeOwner;
    private List<Device> currentDevices;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_device);

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
        etDeviceName = findViewById(R.id.etDeviceName);
        switchInitialStatus = findViewById(R.id.switchInitialStatus);
        btnAddDevice = findViewById(R.id.btnAddDevice);
        progressBar = findViewById(R.id.progressBar);
        tvHomeInfo = findViewById(R.id.tvHomeInfo);

        // Set home info text
        tvHomeInfo.setText(String.format("Adding device to: %s", homeOwner));

        // Set up loading observer
        deviceRepository.getIsLoading().observe(this, isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            btnAddDevice.setEnabled(!isLoading);
        });

        // Set up error observer
        deviceRepository.getErrorMessage().observe(this, errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });

        // Set up device list observer - check device limit
        deviceRepository.getDevices().observe(this, devices -> {
            currentDevices = devices;
            updateDeviceCountUI(devices);
        });

        // Initial fetch of devices
        deviceRepository.fetchDevices(homeId);

        // Set up button click listener
        btnAddDevice.setOnClickListener(v -> addDevice());
    }

    private void updateDeviceCountUI(List<Device> devices) {
        int deviceCount = devices != null ? devices.size() : 0;

        // Update the home info text to include device count
//        if (tvHomeInfo != null) {
//            tvHomeInfo.setText(String.format("Adding device to: %s (%d/%d devices)",
//                    homeOwner, deviceCount, MAX_DEVICES_ALLOWED));
//        }

        // Disable add button if max devices reached
        if (deviceCount >= MAX_DEVICES_ALLOWED) {
            btnAddDevice.setEnabled(false);
            btnAddDevice.setText("Maximum Devices Reached");
            btnAddDevice.setTextColor(Color.WHITE);
            Toast.makeText(this, "Maximum limit of 21 devices reached", Toast.LENGTH_LONG).show();
        } else {
            btnAddDevice.setEnabled(true);
            btnAddDevice.setText("Add Device");
        }
    }

    private void addDevice() {
        String deviceName = etDeviceName.getText().toString().trim();
        boolean initialStatus = switchInitialStatus.isChecked();

        // Validate input
        if (deviceName.isEmpty()) {
            Toast.makeText(this, "Please enter a device name", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check device limit
        if (currentDevices != null && currentDevices.size() >= MAX_DEVICES_ALLOWED) {
            Toast.makeText(this, "Cannot add more devices. Maximum limit of 21 devices reached.", Toast.LENGTH_LONG).show();
            return;
        }

        // Create and add new device
        Device newDevice = new Device(deviceName, initialStatus, homeId);
        deviceRepository.addDevice(homeId, newDevice, this);
    }

    @Override
    public void onDeviceAdded(Device device) {
        Toast.makeText(this, "Device added successfully", Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    public void onError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}