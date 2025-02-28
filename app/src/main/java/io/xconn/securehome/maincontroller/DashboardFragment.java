package io.xconn.securehome.maincontroller;

import android.annotation.SuppressLint;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.appcompat.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import pl.droidsonroids.gif.GifImageView;

import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;

import io.xconn.securehome.R;

/**
 * Dashboard Fragment handles the main control interface for the smart home system.
 * Displays and controls fire/gas detection, air monitoring, and electrical ports.
 */
public class DashboardFragment extends Fragment {

    // UI Components
    private ImageView fgStatusImageView;
    private TextView homeFgStatusTextView;
    private GifImageView weatherAnimationView;
    private TextView tempTextView, humidityTextView, aqiTextView;

    // Port switches and images
    private SwitchCompat switch1, switch2, switch3, switch4;
    private ImageView imageView1, imageView2, imageView3, imageView4;

    // Cards for section control
    private MaterialCardView fgnCard, airmCard;

    public DashboardFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        // Initialize all UI components
        initializeViews(view);

        // Set up listeners
        setupListeners();

        // Initialize with default data
        loadInitialData();

        return view;
    }

    /**
     * Initialize all view references from the layout
     */
    private void initializeViews(View view) {
        // Fire-Gas Detection section
        fgnCard = view.findViewById(R.id.fgn);
        fgStatusImageView = view.findViewById(R.id.fg_status);
        homeFgStatusTextView = view.findViewById(R.id.home_fg_status);

        // Air Monitoring section
        airmCard = view.findViewById(R.id.airm);

        tempTextView = view.findViewById(R.id.temp);
        humidityTextView = view.findViewById(R.id.humidity);
        aqiTextView = view.findViewById(R.id.aqi);

        // Electrical Ports section
        imageView1 = view.findViewById(R.id.imageView1);
        imageView2 = view.findViewById(R.id.imageView2);
        imageView3 = view.findViewById(R.id.imageView3);
        imageView4 = view.findViewById(R.id.imageView4);

        switch1 = view.findViewById(R.id.switch1);
        switch2 = view.findViewById(R.id.switch2);
        switch3 = view.findViewById(R.id.switch3);
        switch4 = view.findViewById(R.id.switch4);
    }

    /**
     * Set up event listeners for interactive elements
     */
    private void setupListeners() {
        // Set up switch listeners
        setupSwitchListener(switch1, "Port 1", imageView1);
        setupSwitchListener(switch2, "Port 2", imageView2);
        setupSwitchListener(switch3, "Port 3", imageView3);
        setupSwitchListener(switch4, "Port 4", imageView4);

        // Set up card click listeners for expanded views
        fgnCard.setOnClickListener(v -> openFireGasDetailView());
        airmCard.setOnClickListener(v -> openAirMonitoringDetailView());
    }

    /**
     * Configure switch listener with associated image update
     */
    private void setupSwitchListener(SwitchCompat switchView, String portName, ImageView portImage) {
        switchView.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Update device state through API/Firebase
            updatePortState(portName, isChecked);

            // Update UI to reflect current state
            portImage.setImageResource(isChecked ? R.mipmap.switch_on : R.mipmap.switch_off);
        });
    }

    /**
     * Load initial data from sensors/database
     */
    @SuppressLint("SetTextI18n")
    private void loadInitialData() {
        // Set initial fire/gas status (normally would come from sensor or API)
        updateFireGasStatus(false);

        // Set initial air monitoring values (normally would come from sensors or API)
        tempTextView.setText("24°C");
        humidityTextView.setText("42%");
        aqiTextView.setText("AQI: 35 Good");



        // Set initial port states (normally would come from database)
        initializePortState(switch1, imageView1);
        initializePortState(switch2, imageView2);
        initializePortState(switch3, imageView3);
        initializePortState(switch4, imageView4);
    }

    /**
     * Set initial state for a port without triggering the listener
     */
    private void initializePortState(SwitchCompat switchView, ImageView imageView) {
        // Remove listener temporarily
        switchView.setOnCheckedChangeListener(null);

        // Set initial state
        switchView.setChecked(false);
        imageView.setImageResource(R.mipmap.switch_off);

        // Restore listener manually
        setupSwitchListener(switchView, "Port", imageView);
    }

    /**
     * Update fire/gas detection status
     */
    @SuppressLint("SetTextI18n")
    private void updateFireGasStatus(boolean danger) {
        if (danger) {
            fgStatusImageView.setImageResource(R.mipmap.danger_alert);
            homeFgStatusTextView.setText("DANGER: Fire or Gas detected!");
            homeFgStatusTextView.setTextColor(getResources().getColor(R.color.dangerColor));
        } else {
            fgStatusImageView.setImageResource(R.mipmap.safe_status);
            homeFgStatusTextView.setText("All systems normal. No fire or gas detected.");
            homeFgStatusTextView.setTextColor(getResources().getColor(R.color.textColor));
        }
    }

    /**
     * Set weather animation based on current conditions
     */
    private void setWeatherAnimation(String condition) {
        int resourceId;
        switch (condition.toLowerCase()) {
            case "home":
                resourceId = R.raw.home_animation;
                break;
            case "electricity":
                resourceId = R.raw.electricity_animation;
                break;
            case "sunny":
            default:
                resourceId = R.raw.splashscreen;
                break;
        }
        Glide.with(this).load(resourceId).into(weatherAnimationView);
    }

    /**
     * Update port state in database/IOT device
     */
    private void updatePortState(String portName, boolean isOn) {
        // TODO: Implement communication with backend/IOT device
        // This would typically use Firebase, MQTT, or other IoT communication protocol

        // For now, just show a toast for feedback
        Toast.makeText(getContext(),
                portName + " turned " + (isOn ? "ON" : "OFF"),
                Toast.LENGTH_SHORT).show();
    }

    /**
     * Navigate to detailed fire/gas view
     */
    private void openFireGasDetailView() {
        // TODO: Navigate to detailed fire/gas view
        Toast.makeText(getContext(), "Opening Fire-Gas detailed view", Toast.LENGTH_SHORT).show();
    }

    /**
     * Navigate to detailed air monitoring view
     */
    private void openAirMonitoringDetailView() {
        // TODO: Navigate to detailed air monitoring view
        Toast.makeText(getContext(), "Opening Air Monitoring detailed view", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh data from sensors/database
        refreshDashboardData();
    }

    /**
     * Refresh all dashboard data from sensors/database
     */
    private void refreshDashboardData() {
        // TODO: Implement data refresh from sensors/backend
    }
}
