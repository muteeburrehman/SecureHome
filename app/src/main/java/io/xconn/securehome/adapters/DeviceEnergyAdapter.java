package io.xconn.securehome.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

import io.xconn.securehome.R;
import io.xconn.securehome.models.DeviceEnergy;

public class DeviceEnergyAdapter extends RecyclerView.Adapter<DeviceEnergyAdapter.DeviceEnergyViewHolder> {

    private List<DeviceEnergy> deviceEnergyList;

    public DeviceEnergyAdapter(List<DeviceEnergy> deviceEnergyList) {
        this.deviceEnergyList = deviceEnergyList;
    }

    @NonNull
    @Override
    public DeviceEnergyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_device_energy, parent, false);
        return new DeviceEnergyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceEnergyViewHolder holder, int position) {
        DeviceEnergy deviceEnergy = deviceEnergyList.get(position);
        holder.bind(deviceEnergy);
    }

    @Override
    public int getItemCount() {
        return deviceEnergyList != null ? deviceEnergyList.size() : 0;
    }

    public void updateData(List<DeviceEnergy> newData) {
        this.deviceEnergyList = newData;
        notifyDataSetChanged();
    }

    static class DeviceEnergyViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvDeviceName;
        private final TextView tvDeviceType;
        private final TextView tvPower;       // Changed from tvWattage to tvPower
        private final TextView tvVoltage;
        private final TextView tvCurrent;
        private final TextView tvUnits;
        private final ImageView ivDeviceIcon;

        public DeviceEnergyViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDeviceName = itemView.findViewById(R.id.tvDeviceName);
            tvDeviceType = itemView.findViewById(R.id.tvDeviceType);
            tvPower = itemView.findViewById(R.id.tvPower);       // Changed from R.id.tvWattage to R.id.tvPower
            tvVoltage = itemView.findViewById(R.id.tvVoltage);
            tvCurrent = itemView.findViewById(R.id.tvCurrent);
            tvUnits = itemView.findViewById(R.id.tvUnits);
            ivDeviceIcon = itemView.findViewById(R.id.ivDeviceIcon);
        }

        public void bind(DeviceEnergy deviceEnergy) {
            tvDeviceName.setText(deviceEnergy.getDeviceName());
            tvDeviceType.setText(formatDeviceType(deviceEnergy.getDeviceType()));
            tvPower.setText(String.format(Locale.US, "%.1f W", deviceEnergy.getWattage()));  // Changed from tvWattage to tvPower
            tvVoltage.setText(String.format(Locale.US, "%.1f V", deviceEnergy.getVoltage()));
            tvCurrent.setText(String.format(Locale.US, "%.2f A", deviceEnergy.getCurrent()));
            tvUnits.setText(String.format(Locale.US, "%.2f kWh", deviceEnergy.getUnits()));

            // Set device icon based on type
            int iconRes = getDeviceIconResource(deviceEnergy.getDeviceType());
            if (ivDeviceIcon != null && iconRes != 0) {
                ivDeviceIcon.setImageResource(iconRes);
            }
        }

        private String formatDeviceType(String deviceType) {
            if (deviceType == null || deviceType.isEmpty()) {
                return "Unknown Device";
            }

            // Convert snake_case to Title Case
            String[] words = deviceType.split("_");
            StringBuilder formatted = new StringBuilder();

            for (String word : words) {
                if (word.length() > 0) {
                    formatted.append(Character.toUpperCase(word.charAt(0)))
                            .append(word.substring(1).toLowerCase())
                            .append(" ");
                }
            }

            return formatted.toString().trim();
        }

        private int getDeviceIconResource(String deviceType) {
            if (deviceType == null) {
                return R.drawable.ic_device_on; // Default icon
            }

            switch (deviceType.toLowerCase()) {
                case "light":
                    return R.drawable.ic_light_on;
                case "outlet":
                    return R.drawable.ic_plug_on;
                case "hvac":
                case "thermostat":
                    return R.drawable.ic_thermostat_on;
                case "water_heater":
                case "water_pump":
                    return R.drawable.ic_water_on; // Assuming you have this icon
                case "entertainment":
                    return R.drawable.ic_speaker_on;
                case "refrigerator":
                case "kitchen":
                    return R.drawable.ic_kitchen_on; // Assuming you have this icon
                case "laundry":
                    return R.drawable.ic_laundry_on; // Assuming you have this icon
                default:
                    return R.drawable.ic_device_on;
            }
        }
    }
}