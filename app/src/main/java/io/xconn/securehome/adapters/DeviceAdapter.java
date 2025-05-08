package io.xconn.securehome.adapters;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.motion.widget.MotionLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;
import com.github.angads25.toggle.widget.LabeledSwitch;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.slider.Slider;

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
        void onDeviceFavorite(Device device, boolean isFavorite);
        void onDeviceSettings(Device device);
    }

    static class DeviceViewHolder extends RecyclerView.ViewHolder {
        // Main card components
        private final MaterialCardView cardDevice;
        private final MotionLayout deviceCardMotionLayout;
        private final View cardBackground;

        // Device info components
        private final TextView tvDeviceName;
        private final TextView tvDeviceType;
        private final Chip chipDeviceStatus;
        private final ImageView ivDeviceIcon;
        private final LottieAnimationView deviceStatusAnimation;

        // Control components
        private final LabeledSwitch interactiveSwitch;
        private final MaterialButton btnSchedule;
        private final MaterialButton btnFavorite;
        private final MaterialButton btnSettings;
        private final MaterialButtonToggleGroup deviceQuickActions;

        // Removed usage stats container, tvUsageStat, and ivUsageIndicator

        // Expanded content (may be null if not in expanded layout)
        private final View expandedContentLayout;
        private final Slider brightnessSlider;
        private final MaterialButtonToggleGroup colorSelectionGroup;

        public DeviceViewHolder(@NonNull View itemView) {
            super(itemView);

            // Main card components
            cardDevice = itemView.findViewById(R.id.cardDevice);
            deviceCardMotionLayout = itemView.findViewById(R.id.deviceCardMotionLayout);
            cardBackground = itemView.findViewById(R.id.cardBackground);

            // Device info components
            tvDeviceName = itemView.findViewById(R.id.tvDeviceName);
            tvDeviceType = itemView.findViewById(R.id.tvDeviceType);
            chipDeviceStatus = itemView.findViewById(R.id.chipDeviceStatus);
            ivDeviceIcon = itemView.findViewById(R.id.ivDeviceIcon);
            deviceStatusAnimation = itemView.findViewById(R.id.deviceStatusAnimation);

            // Control components
            interactiveSwitch = itemView.findViewById(R.id.interactiveSwitch);
            btnSchedule = itemView.findViewById(R.id.btnSchedule);
            btnFavorite = itemView.findViewById(R.id.btnFavorite);
            btnSettings = itemView.findViewById(R.id.btnSettings);
            deviceQuickActions = itemView.findViewById(R.id.deviceQuickActions);

            // Removed usage stats references

            // Expanded content (optional)
            expandedContentLayout = itemView.findViewById(R.id.expandedContentLayout);
            brightnessSlider = itemView.findViewById(R.id.brightnessSlider);
            colorSelectionGroup = itemView.findViewById(R.id.colorSelectionGroup);
        }

        public void bind(final Device device, final OnDeviceListener listener) {
            // Set basic device info
            tvDeviceName.setText(device.getName());

            // Set device type based on name (this is a simple example, adjust according to your needs)
            String deviceTypeName = deriveDeviceTypeFromName(device.getName());
            tvDeviceType.setText(deviceTypeName);

            // Update status and visuals
            boolean isOn = device.isStatus();

            // Update status chip
            chipDeviceStatus.setText(isOn ? "ON" : "OFF");
            int statusColor = isOn ? R.color.device_active : R.color.device_inactive;
            chipDeviceStatus.setChipBackgroundColor(
                    ColorStateList.valueOf(ContextCompat.getColor(itemView.getContext(), statusColor))
            );

            // Update device icon
            int iconRes = getDeviceIconResource(deviceTypeName, isOn);
            ivDeviceIcon.setImageResource(iconRes);

            // Handle pulse animation for active devices
            if (isOn) {
                deviceStatusAnimation.setVisibility(View.VISIBLE);
                deviceStatusAnimation.playAnimation();
            } else {
                deviceStatusAnimation.setVisibility(View.INVISIBLE);
                deviceStatusAnimation.pauseAnimation();
            }

            // Set up interactive switch without triggering listener
            interactiveSwitch.setOnToggledListener(null);
            interactiveSwitch.setOn(isOn);

            // Set up switch listener
            interactiveSwitch.setOnToggledListener((toggleableView, isChecked) -> {
                listener.onDeviceToggle(device, isChecked);
            });

            // Set up quick action buttons
            btnSchedule.setOnClickListener(v -> listener.onDeviceSchedule(device));
            btnFavorite.setOnClickListener(v -> listener.onDeviceFavorite(device, !btnFavorite.isChecked()));
            btnSettings.setOnClickListener(v -> listener.onDeviceSettings(device));

            // Set up card expansion behavior
            if (deviceCardMotionLayout != null) {
                deviceCardMotionLayout.setTransitionListener(new MotionLayout.TransitionListener() {
                    @Override
                    public void onTransitionStarted(MotionLayout motionLayout, int startId, int endId) {}

                    @Override
                    public void onTransitionChange(MotionLayout motionLayout, int startId, int endId, float progress) {}

                    @Override
                    public void onTransitionCompleted(MotionLayout motionLayout, int currentId) {
                        // Toggle expanded content visibility based on motion state
                        if (expandedContentLayout != null) {
                            boolean isExpanded = currentId == R.id.end;
                            expandedContentLayout.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
                        }
                    }

                    @Override
                    public void onTransitionTrigger(MotionLayout motionLayout, int triggerId, boolean positive, float progress) {}
                });

                // Make card clickable to expand/collapse
                cardDevice.setOnClickListener(v -> {
                    int currentState = deviceCardMotionLayout.getCurrentState();
                    int targetState = (currentState == R.id.start) ? R.id.end : R.id.start;
                    deviceCardMotionLayout.transitionToState(targetState);
                });
            }

            // Removed usage stats setup

            // Set up expanded controls if available
            if (brightnessSlider != null) {
                brightnessSlider.setValue(isOn ? 80f : 0f); // Default value
                brightnessSlider.addOnChangeListener((slider, value, fromUser) -> {
                    if (fromUser && value > 0 && !device.isStatus()) {
                        // If slider is moved and device was off, turn it on
                        listener.onDeviceToggle(device, true);
                    } else if (fromUser && value == 0 && device.isStatus()) {
                        // If slider is set to 0 and device was on, turn it off
                        listener.onDeviceToggle(device, false);
                    }
                });
            }

            // Set default color selection if available
            if (colorSelectionGroup != null) {
                // Select default color based on device type or status
                Button defaultColorButton = (Button) colorSelectionGroup.getChildAt(isOn ? 0 : 2);
                if (defaultColorButton != null) {
                    defaultColorButton.performClick();
                }
            }
        }

        /**
         * Helper method to derive a device type from the device name
         */
        private String deriveDeviceTypeFromName(String name) {
            String nameLower = name.toLowerCase();
            if (nameLower.contains("light") || nameLower.contains("lamp") || nameLower.contains("bulb")) {
                return "Smart Light";
            } else if (nameLower.contains("thermostat") || nameLower.contains("heat") || nameLower.contains("ac")) {
                return "Thermostat";
            } else if (nameLower.contains("door") || nameLower.contains("lock")) {
                return "Smart Lock";
            } else if (nameLower.contains("camera") || nameLower.contains("cam")) {
                return "Camera";
            } else if (nameLower.contains("plug") || nameLower.contains("outlet") || nameLower.contains("socket")) {
                return "Smart Plug";
            } else if (nameLower.contains("speaker") || nameLower.contains("sound")) {
                return "Smart Speaker";
            } else {
                return "Smart Device";
            }
        }

        /**
         * Helper method to get the appropriate icon resource based on device type and status
         */
        private int getDeviceIconResource(String deviceType, boolean isOn) {
            String typeLower = deviceType.toLowerCase();

            if (typeLower.contains("light")) {
                return isOn ? R.drawable.ic_light_on : R.drawable.ic_light_off;
            } else if (typeLower.contains("thermostat")) {
                return isOn ? R.drawable.ic_thermostat_on : R.drawable.ic_thermostat_off;
            } else if (typeLower.contains("lock")) {
                return isOn ? R.drawable.ic_lock_on : R.drawable.ic_lock_off;
            } else if (typeLower.contains("camera")) {
                return isOn ? R.drawable.ic_camera_on : R.drawable.ic_camera_off;
            } else if (typeLower.contains("plug")) {
                return isOn ? R.drawable.ic_plug_on : R.drawable.ic_plug_off;
            } else if (typeLower.contains("speaker")) {
                return isOn ? R.drawable.ic_speaker_on : R.drawable.ic_speaker_off;
            } else {
                // Default icons
                return isOn ? R.drawable.ic_device_on : R.drawable.ic_device_off;
            }
        }
    }
}