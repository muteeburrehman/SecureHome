package io.xconn.securehome.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import io.xconn.securehome.R;
import io.xconn.securehome.models.Device;
import io.xconn.securehome.services.DeviceConnectionService;

public class AddDeviceDialog extends DialogFragment {

    private TextInputEditText deviceNameInput;
    private AutoCompleteTextView deviceTypeDropdown;
    private TextInputEditText deviceIpInput;
    private TextInputEditText devicePortInput;
    private TextInputEditText usernameInput;
    private TextInputEditText passwordInput;
    private MaterialButton testConnectionButton;
    private MaterialButton cancelButton;
    private MaterialButton addButton;

    private DeviceConnectionService connectionService;
    private DeviceAddListener listener;

    public interface DeviceAddListener {
        void onDeviceAdded(Device device);
    }

    public AddDeviceDialog() {
        // Required empty public constructor
    }

    public static AddDeviceDialog newInstance() {
        return new AddDeviceDialog();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            listener = (DeviceAddListener) getParentFragment();
        } catch (ClassCastException e) {
            throw new ClassCastException(getParentFragment().toString()
                    + " must implement DeviceAddListener");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        return dialog;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.add_device_dialog, container, false);

        connectionService = new DeviceConnectionService();

        // Initialize views
        deviceNameInput = view.findViewById(R.id.deviceNameInput);
        deviceTypeDropdown = view.findViewById(R.id.deviceTypeDropdown);
        deviceIpInput = view.findViewById(R.id.deviceIpInput);
        devicePortInput = view.findViewById(R.id.devicePortInput);
        usernameInput = view.findViewById(R.id.usernameInput);
        passwordInput = view.findViewById(R.id.passwordInput);
        testConnectionButton = view.findViewById(R.id.testConnectionButton);
        cancelButton = view.findViewById(R.id.cancelButton);
        addButton = view.findViewById(R.id.addButton);

        // Setup device type dropdown
        setupDeviceTypeDropdown();

        // Add input validation
        setupInputValidation();

        // Setup button click listeners
        setupButtonListeners();

        return view;
    }

    private void setupDeviceTypeDropdown() {
        String[] deviceTypes = new String[Device.DeviceType.values().length];
        for (int i = 0; i < Device.DeviceType.values().length; i++) {
            deviceTypes[i] = Device.DeviceType.values()[i].getDisplayName();
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                R.layout.dropdown_item,
                deviceTypes
        );

        deviceTypeDropdown.setAdapter(adapter);
        deviceTypeDropdown.setText(deviceTypes[0], false);
    }

    private void setupInputValidation() {
        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                validateInputs();
            }
        };

        deviceNameInput.addTextChangedListener(textWatcher);
        deviceIpInput.addTextChangedListener(textWatcher);
    }

    private void validateInputs() {
        boolean isValid = !deviceNameInput.getText().toString().trim().isEmpty()
                && !deviceIpInput.getText().toString().trim().isEmpty();

        testConnectionButton.setEnabled(isValid);
    }

    private void setupButtonListeners() {
        testConnectionButton.setOnClickListener(v -> testConnection());

        cancelButton.setOnClickListener(v -> dismiss());

        addButton.setOnClickListener(v -> addDevice());
    }

    private void testConnection() {
        String ip = deviceIpInput.getText().toString().trim();
        String portStr = devicePortInput.getText().toString().trim();
        int port = portStr.isEmpty() ? 0 : Integer.parseInt(portStr);

        // Create a temporary device for testing
        Device tempDevice = new Device(
                deviceNameInput.getText().toString().trim(),
                Device.DeviceType.fromDisplayName(deviceTypeDropdown.getText().toString())
        );
        tempDevice.setIpAddress(ip);
        tempDevice.setPort(port);

        // Show loading indicator
        testConnectionButton.setEnabled(false);
        testConnectionButton.setText("Testing...");

        // Test connection
        connectionService.testConnection(tempDevice, new DeviceConnectionService.ConnectionCallback() {
            @Override
            public void onConnectionSuccess() {
                if (getActivity() == null) return;

                getActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Connection successful!", Toast.LENGTH_SHORT).show();
                    testConnectionButton.setText("Test Connection");
                    testConnectionButton.setEnabled(true);
                    addButton.setEnabled(true);
                });
            }

            @Override
            public void onConnectionFailure(String errorMessage) {
                if (getActivity() == null) return;

                getActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Connection failed: " + errorMessage,
                            Toast.LENGTH_SHORT).show();
                    testConnectionButton.setText("Test Connection");
                    testConnectionButton.setEnabled(true);
                    addButton.setEnabled(false);
                });
            }
        });
    }

    private void addDevice() {
        // Get input values
        String name = deviceNameInput.getText().toString().trim();
        Device.DeviceType type = Device.DeviceType.fromDisplayName(deviceTypeDropdown.getText().toString());
        String ip = deviceIpInput.getText().toString().trim();
        String portStr = devicePortInput.getText().toString().trim();
        int port = portStr.isEmpty() ? 0 : Integer.parseInt(portStr);
        String username = usernameInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        // Create new device
        Device newDevice = new Device(name, type, ip, port, username, password);

        // Connect to device
        connectionService.connectToDevice(newDevice, new DeviceConnectionService.ConnectionCallback() {
            @Override
            public void onConnectionSuccess() {
                if (getActivity() == null) return;

                getActivity().runOnUiThread(() -> {
                    // Notify listener
                    listener.onDeviceAdded(newDevice);

                    // Dismiss dialog
                    dismiss();

                    // Show success message
                    Toast.makeText(requireContext(), "Device added successfully!",
                            Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onConnectionFailure(String errorMessage) {
                if (getActivity() == null) return;

                getActivity().runOnUiThread(() -> {
                    // Show error message but still add device
                    Toast.makeText(requireContext(),
                            "Device added but connection failed: " + errorMessage,
                            Toast.LENGTH_LONG).show();

                    // Notify listener
                    listener.onDeviceAdded(newDevice);

                    // Dismiss dialog
                    dismiss();
                });
            }
        });
    }
}