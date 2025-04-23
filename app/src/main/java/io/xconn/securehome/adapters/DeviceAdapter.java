package io.xconn.securehome.adapters;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import com.google.android.material.switchmaterial.SwitchMaterial;

import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import io.xconn.securehome.R;
import io.xconn.securehome.models.Device;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder> {
    private List<Device> devices = new ArrayList<>();
    private final OnDeviceListener listener;

    public DeviceAdapter(OnDeviceListener listener) {
        this.listener = listener;
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
        holder.bind(device, listener);
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }

    public void setDevices(List<Device> devices) {
        this.devices = devices;
        notifyDataSetChanged();
    }

    public interface OnDeviceListener {
        void onDeviceToggle(Device device, boolean newStatus);
        void onDeviceSchedule(Device device);
    }

    static class DeviceViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvDeviceName;
        private final TextView tvDeviceStatus;
        private final SwitchMaterial switchStatus;
        private final Button btnSchedule;
        private final ImageView ivDeviceIcon;

        public DeviceViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDeviceName = itemView.findViewById(R.id.tvDeviceName);
            tvDeviceStatus = itemView.findViewById(R.id.tvDeviceStatus);
            switchStatus = itemView.findViewById(R.id.switchStatus);
            btnSchedule = itemView.findViewById(R.id.btnSchedule);
            ivDeviceIcon = itemView.findViewById(R.id.ivDeviceIcon);
        }

        public void bind(Device device, OnDeviceListener listener) {
            tvDeviceName.setText(device.getName());

            // Update status text
            tvDeviceStatus.setText(device.isStatus() ? "Currently On" : "Currently Off");

            // Set switch without triggering listener
            switchStatus.setOnCheckedChangeListener(null);
            switchStatus.setChecked(device.isStatus());

            // Set up switch listener
            switchStatus.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (buttonView.isPressed()) { // Only trigger if user actually toggled it
                    listener.onDeviceToggle(device, isChecked);
                }
            });

            // Set up schedule button listener
            btnSchedule.setOnClickListener(v -> listener.onDeviceSchedule(device));

            // Update icon based on status
            ivDeviceIcon.setImageResource(device.isStatus() ?
                    R.drawable.ic_device_on : R.drawable.ic_device_off);
        }
    }
}