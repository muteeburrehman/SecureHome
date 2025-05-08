package io.xconn.securehome.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Color;
import com.google.android.material.switchmaterial.SwitchMaterial;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.xconn.securehome.R;
import io.xconn.securehome.models.Device;
import io.xconn.securehome.repository.DeviceRepository;
import io.xconn.securehome.utils.Esp32EndpointManager;

public class AddDeviceActivity extends AppCompatActivity {
    private static final String TAG = "AddDeviceActivity";
    private static final int MAX_DEVICES_ALLOWED = 23;

    private EditText etDeviceName;
    private Spinner spinnerDeviceType;
    private SwitchMaterial switchInitialStatus;
    private Button btnAddDevice;
    private ProgressBar progressBar;
    private TextView tvHomeInfo;
    private TextView tvEndpointInfo;
    private DeviceRepository deviceRepository;
    private int homeId;
    private String homeOwner;
    private int selectedEndpointIndex = 0; // Default to the first endpoint
    private List<Device> currentDevices;

    // Device type to endpoint index mapping
    private final Map<String, Integer> deviceTypeToEndpointMap = new HashMap<>();
    private final List<String> deviceTypes = new ArrayList<>();

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

        // Initialize repository
        deviceRepository = new DeviceRepository(this);

        // Initialize UI components
        initializeUI();

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

        // Set up device type spinner with endpoint mapping
        setupDeviceTypes();

        // Setup device type spinner
        setupDeviceTypeSpinner();

        // Set up add button
        btnAddDevice.setOnClickListener(v -> addDevice());
    }

    private void initializeUI() {
        etDeviceName = findViewById(R.id.etDeviceName);
        spinnerDeviceType = findViewById(R.id.spinnerDeviceType);
        switchInitialStatus = findViewById(R.id.switchInitialStatus);
        btnAddDevice = findViewById(R.id.btnAddDevice);
        progressBar = findViewById(R.id.progressBar);
        tvHomeInfo = findViewById(R.id.tvHomeInfo);
        tvEndpointInfo = findViewById(R.id.tvEndpointInfo);
    }

    private void setupDeviceTypes() {
        // Define device types and their corresponding endpoint indices
        // These should match the order in Esp32EndpointManager
        deviceTypes.clear();
        deviceTypeToEndpointMap.clear();

        // Add device types with their endpoint indices
        addDeviceType("Smart Lock", 0);         // Uses "/unlock" and "/lock"
        addDeviceType("Red Light", 1);          // Uses "/RedON" and "/RedOFF"
        addDeviceType("Blue Light", 2);         // Uses "/BlueON" and "/BlueOFF"
        addDeviceType("Yellow Light", 3);       // Uses "/YellowON" and "/YellowOFF"
        addDeviceType("Purple Light", 4);       // Uses "/PurpleON" and "/PurpleOFF"
        addDeviceType("Orange Light", 5);       // Uses "/OrangeON" and "/OrangeOFF"
        addDeviceType("White Light", 6);        // Uses "/WhiteON" and "/WhiteOFF"
        addDeviceType("Black Light", 7);        // Uses "/BlackON" and "/BlackOFF"
        addDeviceType("Cyan Light", 8);         // Uses "/CyanON" and "/CyanOFF"
        addDeviceType("Magenta Light", 9);      // Uses "/MagentaON" and "/MagentaOFF"
        addDeviceType("Brown Light", 10);       // Uses "/BrownON" and "/BrownOFF"
        addDeviceType("Gray Light", 11);        // Uses "/GrayON" and "/GrayOFF"
        addDeviceType("Pink Light", 12);        // Uses "/PinkON" and "/PinkOFF"
        addDeviceType("Lime Light", 13);        // Uses "/LimeON" and "/LimeOFF"
        addDeviceType("Teal Light", 14);        // Uses "/TealON" and "/TealOFF"
        addDeviceType("Navy Light", 15);        // Uses "/NavyON" and "/NavyOFF"
        addDeviceType("Silver Light", 16);      // Uses "/SilverON" and "/SilverOFF"
        addDeviceType("Gold Light", 17);        // Uses "/GoldON" and "/GoldOFF"
        addDeviceType("Device A", 18);          // Uses "/DeviceAON" and "/DeviceAOFF"
        addDeviceType("Device B", 19);          // Uses "/DeviceBON" and "/DeviceBOFF"
        addDeviceType("Device C", 20);          // Uses "/DeviceCON" and "/DeviceCOFF"
        addDeviceType("Device D", 21);          // Uses "/DeviceDON" and "/DeviceDOFF"
        addDeviceType("Device E", 22);          // Uses "/DeviceEON" and "/DeviceEOFF"
    }

    private void addDeviceType(String typeName, int endpointIndex) {
        deviceTypes.add(typeName);
        deviceTypeToEndpointMap.put(typeName, endpointIndex);
    }

    private void setupDeviceTypeSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, deviceTypes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDeviceType.setAdapter(adapter);

        spinnerDeviceType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedType = deviceTypes.get(position);
                selectedEndpointIndex = deviceTypeToEndpointMap.get(selectedType);
                updateEndpointInfo(selectedType, selectedEndpointIndex);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }

    private void updateEndpointInfo(String deviceType, int endpointIndex) {
        String onEndpoint = Esp32EndpointManager.getOnEndpoint(endpointIndex);
        String offEndpoint = Esp32EndpointManager.getOffEndpoint(endpointIndex);

        String details = String.format("This device will use endpoints: %s / %s",
                onEndpoint, offEndpoint);

        tvEndpointInfo.setText(details);
        tvEndpointInfo.setVisibility(View.VISIBLE);
    }

    private void updateDeviceCountUI(List<Device> devices) {
        int deviceCount = devices != null ? devices.size() : 0;

        // Update the home info text to include device count
        if (tvHomeInfo != null) {
            tvHomeInfo.setText(String.format("Adding device to: %s (%d/%d devices)",
                    homeOwner, deviceCount, MAX_DEVICES_ALLOWED));
        }

        // Disable add button if max devices reached
        if (deviceCount >= MAX_DEVICES_ALLOWED) {
            btnAddDevice.setEnabled(false);
            btnAddDevice.setText("Maximum Devices Reached");
            btnAddDevice.setTextColor(Color.WHITE);
            Toast.makeText(this, "Maximum limit of " + MAX_DEVICES_ALLOWED + " devices reached", Toast.LENGTH_LONG).show();
        } else {
            btnAddDevice.setEnabled(true);
            btnAddDevice.setText("Add Device");
        }
    }

    private void addDevice() {
        String deviceName = etDeviceName.getText().toString().trim();
        boolean initialStatus = switchInitialStatus.isChecked();

        if (deviceName.isEmpty()) {
            Toast.makeText(this, "Please enter a device name", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check device limit
        if (currentDevices != null && currentDevices.size() >= MAX_DEVICES_ALLOWED) {
            Toast.makeText(this, "Cannot add more devices. Maximum limit of " + MAX_DEVICES_ALLOWED + " devices reached.", Toast.LENGTH_LONG).show();
            return;
        }

        // Show loading indicator
        progressBar.setVisibility(View.VISIBLE);
        btnAddDevice.setEnabled(false);

        // Create device with the selected endpoint index
        Device newDevice = new Device(deviceName, initialStatus, homeId);
        newDevice.setEndpointIndex(selectedEndpointIndex);

        Log.d(TAG, "Adding new device: " + newDevice.getName() +
                " with endpoint index: " + selectedEndpointIndex);

        // Add device
        deviceRepository.addDevice(homeId, newDevice, new DeviceRepository.OnOperationListener() {
            @Override
            public void onSuccess(String message) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnAddDevice.setEnabled(true);
                    Toast.makeText(AddDeviceActivity.this, message, Toast.LENGTH_SHORT).show();
                    finish();
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnAddDevice.setEnabled(true);
                    Toast.makeText(AddDeviceActivity.this, message, Toast.LENGTH_LONG).show();
                });
            }
        });
    }
}