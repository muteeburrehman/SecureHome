package io.xconn.securehome.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import io.xconn.securehome.R;
import io.xconn.securehome.models.DeviceEnergy;

public class DeviceEnergyAdapter extends RecyclerView.Adapter<DeviceEnergyAdapter.ViewHolder> {

    private List<DeviceEnergy> deviceEnergyList;

    public DeviceEnergyAdapter(List<DeviceEnergy> deviceEnergyList) {
        this.deviceEnergyList = deviceEnergyList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_device_energy, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DeviceEnergy deviceEnergy = deviceEnergyList.get(position);

        holder.tvDeviceName.setText(deviceEnergy.getDeviceName());
        holder.tvPower.setText(String.format("%.1f W", deviceEnergy.getWattage()));
        holder.tvVoltage.setText(String.format("%.1f V", deviceEnergy.getVoltage()));
        holder.tvCurrent.setText(String.format("%.2f A", deviceEnergy.getCurrent()));
        holder.tvUnits.setText(String.format("%.2f kWh", deviceEnergy.getUnits()));

        // Set device icon based on type
        setDeviceIcon(holder.ivDeviceIcon, deviceEnergy.getDeviceType());
    }

    private void setDeviceIcon(ImageView imageView, String deviceType) {
        switch (deviceType) {
            case "light":
                imageView.setImageResource(R.drawable.ic_device_light);
                break;
            case "outlet":
                imageView.setImageResource(R.drawable.ic_outlets);
                break;
            case "hvac":
                imageView.setImageResource(R.drawable.ic_hvac);
                break;
            case "water_heater":
                imageView.setImageResource(R.drawable.ic_water_heater);
                break;
            case "thermostat":
                imageView.setImageResource(R.drawable.ic_thermostat);
                break;
            case "water_pump":
                imageView.setImageResource(R.drawable.ic_water_pump);
                break;
            default:
                imageView.setImageResource(R.drawable.ic_device);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return deviceEnergyList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivDeviceIcon;
        TextView tvDeviceName, tvPower, tvVoltage, tvCurrent, tvUnits;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivDeviceIcon = itemView.findViewById(R.id.ivDeviceIcon);
            tvDeviceName = itemView.findViewById(R.id.tvDeviceName);
            tvPower = itemView.findViewById(R.id.tvPower);
            tvVoltage = itemView.findViewById(R.id.tvVoltage);
            tvCurrent = itemView.findViewById(R.id.tvCurrent);
            tvUnits = itemView.findViewById(R.id.tvUnits);
        }
    }
}