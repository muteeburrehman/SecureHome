package io.xconn.securehome.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.List;

import io.xconn.securehome.R;
import io.xconn.securehome.maincontroller.DevicesFragment;
import io.xconn.securehome.models.Device;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder> {

    private List<Device> devices;
    private DevicesFragment devicesFragment; // Updated from Context to DevicesFragment
    private OnDeviceStatusChangeListener statusChangeListener;

    public interface OnDeviceStatusChangeListener {
        void onDeviceStatusChanged(Device device, boolean isOn);
    }

    // Constructor updated to take DevicesFragment instead of Context
    public DeviceAdapter(List<Device> devices, DevicesFragment devicesFragment) {
        this.devices = devices;
        this.devicesFragment = devicesFragment;
    }

    public void setOnDeviceStatusChangeListener(OnDeviceStatusChangeListener listener) {
        this.statusChangeListener = listener;
    }

    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_device, parent, false);
        return new DeviceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
        Device device = devices.get(position);

        // Set device name
        holder.deviceName.setText(device.getName());

        // Set device status
        String statusText = device.isOn() ? "Status: On" : "Status: Off";
        holder.deviceStatus.setText(statusText);

        // Set IP address
        holder.deviceIpAddress.setText(device.getIpAddress());

        // Set device icon based on type
        int iconResource = getIconResourceForDeviceType(device.getType());
        holder.deviceIcon.setImageResource(iconResource);

        // Set connection status
        if (device.isConnected()) {
            holder.connectionStatus.setImageResource(R.drawable.ic_wifi);
            holder.connectionStatus.setAlpha(1.0f);
        } else {
            holder.connectionStatus.setImageResource(R.drawable.ic_wifi_off);
            holder.connectionStatus.setAlpha(0.5f);
        }

        // Set switch state without triggering listener
        holder.deviceSwitch.setOnCheckedChangeListener(null);
        holder.deviceSwitch.setChecked(device.isOn());

        // Setup switch listener
        holder.deviceSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (statusChangeListener != null) {
                statusChangeListener.onDeviceStatusChanged(device, isChecked);
            }
        });

        // Setup expandable schedule section
        holder.scheduleHeader.setOnClickListener(v -> {
            if (holder.scheduleContent.getVisibility() == View.VISIBLE) {
                holder.scheduleContent.setVisibility(View.GONE);
                holder.expandCollapseIcon.setImageResource(R.drawable.ic_expand);
            } else {
                holder.scheduleContent.setVisibility(View.VISIBLE);
                holder.expandCollapseIcon.setImageResource(R.drawable.ic_collapse);
            }
        });

        // Setup start and end time buttons
        holder.setStartTimeButton.setOnClickListener(v -> {
            // Show time picker dialog to set start time
            // This would be implemented in a real app
        });

        holder.setEndTimeButton.setOnClickListener(v -> {
            // Show time picker dialog to set end time
            // This would be implemented in a real app
        });

        // Setup edit button
        holder.editDeviceButton.setOnClickListener(v -> {
            // Show edit device dialog
            // This would be implemented in a real app
        });

        // Setup remove button (Fixed issue)
        holder.removeDeviceButton.setOnClickListener(v -> {
            if (devicesFragment != null) {
                devicesFragment.removeDevice(device); // No casting needed
            }
        });
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }

    private int getIconResourceForDeviceType(Device.DeviceType type) {
        switch (type) {
            case LIGHT:
                return R.drawable.ic_lightbulb;
            case OUTLET:
                return R.drawable.ic_outlets;
            case THERMOSTAT:
                return R.drawable.ic_thermostat;
            case CAMERA:
                return R.drawable.ic_camera;
            case OTHER:
            default:
                return R.drawable.devices;
        }
    }

    static class DeviceViewHolder extends RecyclerView.ViewHolder {
        TextView deviceName;
        TextView deviceStatus;
        TextView deviceIpAddress;
        ImageView deviceIcon;
        ImageView connectionStatus;
        ImageView expandCollapseIcon;
        SwitchMaterial deviceSwitch;
        LinearLayout scheduleHeader;
        LinearLayout scheduleContent;
        MaterialButton setStartTimeButton;
        MaterialButton setEndTimeButton;
        MaterialButton editDeviceButton;
        MaterialButton removeDeviceButton;

        DeviceViewHolder(View itemView) {
            super(itemView);
            deviceName = itemView.findViewById(R.id.deviceName);
            deviceStatus = itemView.findViewById(R.id.deviceStatus);
            deviceIpAddress = itemView.findViewById(R.id.deviceIpAddress);
            deviceIcon = itemView.findViewById(R.id.deviceIcon);
            connectionStatus = itemView.findViewById(R.id.connectionStatus);
            expandCollapseIcon = itemView.findViewById(R.id.expandCollapseIcon);
            deviceSwitch = itemView.findViewById(R.id.deviceSwitch);
            scheduleHeader = itemView.findViewById(R.id.scheduleHeader);
            scheduleContent = itemView.findViewById(R.id.scheduleContent);
            setStartTimeButton = itemView.findViewById(R.id.setStartTimeButton);
            setEndTimeButton = itemView.findViewById(R.id.setEndTimeButton);
            editDeviceButton = itemView.findViewById(R.id.editDeviceButton);
            removeDeviceButton = itemView.findViewById(R.id.removeDeviceButton);
        }
    }
}
