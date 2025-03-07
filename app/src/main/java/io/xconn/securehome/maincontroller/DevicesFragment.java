package io.xconn.securehome.maincontroller;

import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import io.xconn.securehome.R;
import io.xconn.securehome.adapters.DeviceAdapter;
import io.xconn.securehome.dialogs.AddDeviceDialog;
import io.xconn.securehome.models.Device;
import io.xconn.securehome.services.DeviceConnectionService;

public class DevicesFragment extends Fragment implements AddDeviceDialog.DeviceAddListener {

    private List<Device> allDevicesList = new ArrayList<>();
    private List<Device> filteredDevicesList = new ArrayList<>();
    private DeviceAdapter deviceAdapter;
    private RecyclerView deviceRecyclerView;
    private LinearLayout emptyStateView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ChipGroup filterChipGroup;
    private ExtendedFloatingActionButton addDeviceFab;
    private DeviceConnectionService connectionService;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_devices, container, false);

        // Initialize views
        deviceRecyclerView = view.findViewById(R.id.deviceRecyclerView);
        emptyStateView = view.findViewById(R.id.emptyStateView);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        filterChipGroup = view.findViewById(R.id.filterChipGroup);

        // Initialize connection service
        connectionService = new DeviceConnectionService();

        // Setup RecyclerView
        deviceRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        deviceAdapter = new DeviceAdapter(filteredDevicesList, this);
        deviceAdapter.setOnDeviceStatusChangeListener(this::onDeviceStatusChanged);
        deviceRecyclerView.setAdapter(deviceAdapter);

        // Setup FAB
        addDeviceFab = view.findViewById(R.id.addDeviceFab);
        addDeviceFab.setOnClickListener(v -> showAddDeviceDialog());

        // Setup empty state button
        view.findViewById(R.id.addFirstDeviceButton).setOnClickListener(v -> showAddDeviceDialog());

        // Setup swipe refresh
        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary, R.color.colorAccent);
        swipeRefreshLayout.setOnRefreshListener(this::refreshDevices);

        // Setup filter chips
        setupFilterChips();

        // Load devices
        loadDevices();

        // Check if empty state should be shown
        updateEmptyState();

        return view;
    }

    private void showAddDeviceDialog() {
        AddDeviceDialog dialog = AddDeviceDialog.newInstance();
        dialog.show(getChildFragmentManager(), "AddDeviceDialog");
    }

    @Override
    public void onDeviceAdded(Device device) {
        // Add the new device to the list
        allDevicesList.add(device);

        // Apply current filter
        applyDeviceFilter(getCurrentDeviceType());

        // Update empty state
        updateEmptyState();

        // Show success message
        Snackbar.make(deviceRecyclerView, "Device added successfully", Snackbar.LENGTH_SHORT).show();
    }

    private void onDeviceStatusChanged(Device device, boolean isOn) {
        // Update device status in the service
        connectionService.updateDeviceStatus(device, isOn, new DeviceConnectionService.StatusUpdateCallback() {
            @Override
            public void onStatusUpdateSuccess() {
                if (getActivity() == null) return;

                getActivity().runOnUiThread(() -> {
                    // Find and update the device in our lists
                    int index = allDevicesList.indexOf(device);
                    if (index >= 0) {
                        allDevicesList.get(index).setOn(isOn);
                    }

                    // Also update in filtered list if present
                    int filteredIndex = filteredDevicesList.indexOf(device);
                    if (filteredIndex >= 0) {
                        filteredDevicesList.get(filteredIndex).setOn(isOn);
                        deviceAdapter.notifyItemChanged(filteredIndex);
                    }
                });
            }

            @Override
            public void onStatusUpdateFailure(String errorMessage) {
                if (getActivity() == null) return;

                getActivity().runOnUiThread(() -> {
                    // Revert the switch state in UI
                    device.setOn(!isOn); // Revert status

                    int filteredIndex = filteredDevicesList.indexOf(device);
                    if (filteredIndex >= 0) {
                        deviceAdapter.notifyItemChanged(filteredIndex);
                    }

                    // Show error message
                    Snackbar.make(deviceRecyclerView,
                            "Failed to update device: " + errorMessage,
                            Snackbar.LENGTH_LONG).show();
                });
            }
        });
    }

    private void setupFilterChips() {
        filterChipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            Device.DeviceType filterType = Device.DeviceType.OTHER;

            if (checkedId == R.id.chipAll) {
                filterType = null; // No filter, show all
            } else if (checkedId == R.id.chipLights) {
                filterType = Device.DeviceType.LIGHT;
            } else if (checkedId == R.id.chipOutlets) {
                filterType = Device.DeviceType.OUTLET;
            } else if (checkedId == R.id.chipThermostats) {
                filterType = Device.DeviceType.THERMOSTAT;
            } else if (checkedId == R.id.chipCameras) {
                filterType = Device.DeviceType.CAMERA;
            }

            applyDeviceFilter(filterType);
        });
    }

    private Device.DeviceType getCurrentDeviceType() {
        int checkedId = filterChipGroup.getCheckedChipId();

        if (checkedId == R.id.chipLights) {
            return Device.DeviceType.LIGHT;
        } else if (checkedId == R.id.chipOutlets) {
            return Device.DeviceType.OUTLET;
        } else if (checkedId == R.id.chipThermostats) {
            return Device.DeviceType.THERMOSTAT;
        } else if (checkedId == R.id.chipCameras) {
            return Device.DeviceType.CAMERA;
        } else {
            return null; // All devices
        }
    }

    private void applyDeviceFilter(Device.DeviceType filterType) {
        filteredDevicesList.clear();

        if (filterType == null) {
            // Show all devices
            filteredDevicesList.addAll(allDevicesList);
        } else {
            // Filter by device type
            for (Device device : allDevicesList) {
                if (device.getType() == filterType) {
                    filteredDevicesList.add(device);
                }
            }
        }

        deviceAdapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void updateEmptyState() {
        // Show empty state if there are no devices
        if (allDevicesList.isEmpty()) {
            emptyStateView.setVisibility(View.VISIBLE);
            deviceRecyclerView.setVisibility(View.GONE);
        } else if (filteredDevicesList.isEmpty()) {
            // Show a message for no devices matching the filter
            emptyStateView.setVisibility(View.GONE);
            deviceRecyclerView.setVisibility(View.VISIBLE);
            Snackbar.make(deviceRecyclerView,
                    "No devices match the selected filter",
                    Snackbar.LENGTH_SHORT).show();
        } else {
            emptyStateView.setVisibility(View.GONE);
            deviceRecyclerView.setVisibility(View.VISIBLE);
        }

        // Update FAB visibility
        addDeviceFab.show();
    }

    private void refreshDevices() {
        // Show refreshing indicator
        swipeRefreshLayout.setRefreshing(true);

        // Refresh connection status for all devices
        List<Device> devicesToRefresh = new ArrayList<>(allDevicesList);
        refreshDeviceConnectionStatus(devicesToRefresh, 0);
    }

    private void refreshDeviceConnectionStatus(List<Device> devices, int index) {
        if (index >= devices.size()) {
            // All devices refreshed
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    swipeRefreshLayout.setRefreshing(false);
                    deviceAdapter.notifyDataSetChanged();
                });
            }
            return;
        }

        Device device = devices.get(index);
        connectionService.checkDeviceStatus(device, new DeviceConnectionService.ConnectionCallback() {
            @Override
            public void onConnectionSuccess() {
                device.setConnected(true);
                refreshDeviceConnectionStatus(devices, index + 1);
            }

            @Override
            public void onConnectionFailure(String errorMessage) {
                device.setConnected(false);
                refreshDeviceConnectionStatus(devices, index + 1);
            }
        });
    }

    private void loadDevices() {
        // In a real app, this would load devices from a database or shared preferences
        // For demonstration, we'll create some sample devices

        // Clear existing devices
        allDevicesList.clear();

        // Add sample devices if the list is empty (e.g., first launch)
        // This would be replaced with your actual data loading logic
        if (allDevicesList.isEmpty()) {
            // You can remove this in production and load from your data source instead
            addSampleDevices();
        }

        // Apply filter (initially show all)
        applyDeviceFilter(null);
    }

    private void addSampleDevices() {
        // This is just for testing - you should remove this in production
        Device light1 = new Device("Living Room Light", Device.DeviceType.LIGHT);
        light1.setIpAddress("192.168.1.101");
        light1.setConnected(true);

        Device outlet1 = new Device("Kitchen Outlet", Device.DeviceType.OUTLET);
        outlet1.setIpAddress("192.168.1.102");
        outlet1.setConnected(true);

        Device thermostat = new Device("Main Thermostat", Device.DeviceType.THERMOSTAT);
        thermostat.setIpAddress("192.168.1.103");
        thermostat.setConnected(true);

        allDevicesList.add(light1);
        allDevicesList.add(outlet1);
        allDevicesList.add(thermostat);
    }

    // Method to handle device deletion (can be called from adapter)
    public void removeDevice(Device device) {
        int position = filteredDevicesList.indexOf(device);
        if (position >= 0) {
            // Remove from both lists
            filteredDevicesList.remove(position);
            allDevicesList.remove(device);

            // Update UI
            deviceAdapter.notifyItemRemoved(position);
            updateEmptyState();

            // Show undo option
            Snackbar.make(deviceRecyclerView,
                            "Device removed",
                            Snackbar.LENGTH_LONG)
                    .setAction("UNDO", v -> {
                        // Add back the device
                        allDevicesList.add(device);
                        applyDeviceFilter(getCurrentDeviceType());
                    })
                    .show();
        }
    }
}