package io.xconn.securehome.adapters;


import android.app.TimePickerDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.Locale;

import io.xconn.securehome.R;
import io.xconn.securehome.maincontroller.Device;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder> {

    private List<Device> deviceList;
    private Context context;

    public DeviceAdapter(List<Device> deviceList, Context context) {
        this.deviceList = deviceList;
        this.context = context;
    }

    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_device, parent, false);
        return new DeviceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
        Device device = deviceList.get(position);
        holder.deviceName.setText(device.getName());
        holder.deviceStatus.setText("Status: " + (device.isOn() ? "On" : "Off"));
        holder.deviceSwitch.setChecked(device.isOn());

        // Display the start and end times
        holder.startTimeText.setText("Start Time: " + device.getStartTime());
        holder.endTimeText.setText("End Time: " + device.getEndTime());

        // Handle the toggle switch
        holder.deviceSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            device.setOn(isChecked);
            holder.deviceStatus.setText("Status: " + (isChecked ? "On" : "Off"));
        });

        // Handle the "Set Start Time" button
        holder.setStartTimeButton.setOnClickListener(v -> showTimePickerDialog(device, true, holder));

        // Handle the "Set End Time" button
        holder.setEndTimeButton.setOnClickListener(v -> showTimePickerDialog(device, false, holder));
    }

    @Override
    public int getItemCount() {
        return deviceList.size();
    }

    static class DeviceViewHolder extends RecyclerView.ViewHolder {
        TextView deviceName, deviceStatus, startTimeText, endTimeText;
        Switch deviceSwitch;
        Button setStartTimeButton, setEndTimeButton;

        public DeviceViewHolder(@NonNull View itemView) {
            super(itemView);
            deviceName = itemView.findViewById(R.id.deviceName);
            deviceStatus = itemView.findViewById(R.id.deviceStatus);
            deviceSwitch = itemView.findViewById(R.id.deviceSwitch);
            setStartTimeButton = itemView.findViewById(R.id.setStartTimeButton);
            setEndTimeButton = itemView.findViewById(R.id.setEndTimeButton);
            startTimeText = itemView.findViewById(R.id.startTimeText);
            endTimeText = itemView.findViewById(R.id.endTimeText);
        }
    }

    private void showTimePickerDialog(Device device, boolean isStartTime, DeviceViewHolder holder) {
        TimePickerDialog timePickerDialog = new TimePickerDialog(
                context,
                (view, hourOfDay, minute) -> {
                    // Format the selected time
                    String time = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);

                    // Update the device's start or end time
                    if (isStartTime) {
                        device.setStartTime(time);
                        holder.startTimeText.setText("Start Time: " + time);
                    } else {
                        device.setEndTime(time);
                        holder.endTimeText.setText("End Time: " + time);
                    }
                },
                12, 0, true // Default time (12:00)
        );
        timePickerDialog.show();
    }
}