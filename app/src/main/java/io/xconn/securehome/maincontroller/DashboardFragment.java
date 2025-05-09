package io.xconn.securehome.maincontroller;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.appcompat.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import io.xconn.securehome.activities.DeviceEnergyMonitoringActivity;

import com.google.android.material.card.MaterialCardView;
import io.xconn.securehome.R;

public class DashboardFragment extends Fragment {
    // UI Components
    private ImageView fgStatusImageView;
    private TextView homeFgStatusTextView;
    private TextView tempTextView, humidityTextView, aqiTextView;

    // Port switches and images
    private SwitchCompat switch1, switch2, switch3, switch4;
    private ImageView imageView1, imageView2, imageView3, imageView4;

    // Cards for section control
    private MaterialCardView fgnCard, airmCard, alertCard, logoCard, electricityCard;

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
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);
        initializeViews(view);
        setupListeners();
        loadInitialData();
        return view;
    }

    private void initializeViews(View view) {
        fgnCard = view.findViewById(R.id.fgn);
        fgStatusImageView = view.findViewById(R.id.fg_status);
        homeFgStatusTextView = view.findViewById(R.id.home_fg_status);
        airmCard = view.findViewById(R.id.airm);
        tempTextView = view.findViewById(R.id.temp);
        humidityTextView = view.findViewById(R.id.humidity);
        aqiTextView = view.findViewById(R.id.aqi);
        alertCard = view.findViewById(R.id.alertCard);
        logoCard = view.findViewById(R.id.logoCard);
        electricityCard = view.findViewById(R.id.electricityCard);
        imageView1 = view.findViewById(R.id.imageView1);
        imageView2 = view.findViewById(R.id.imageView2);
        imageView3 = view.findViewById(R.id.imageView3);
        imageView4 = view.findViewById(R.id.imageView4);
        switch1 = view.findViewById(R.id.switch1);
        switch2 = view.findViewById(R.id.switch2);
        switch3 = view.findViewById(R.id.switch3);
        switch4 = view.findViewById(R.id.switch4);
    }

    private void setupListeners() {
        setupSwitchListener(switch1, "Port 1", imageView1);
        setupSwitchListener(switch2, "Port 2", imageView2);
        setupSwitchListener(switch3, "Port 3", imageView3);
        setupSwitchListener(switch4, "Port 4", imageView4);
        fgnCard.setOnClickListener(v -> openFireGasDetailView());
        airmCard.setOnClickListener(v -> openAirMonitoringDetailView());
        alertCard.setOnClickListener(v -> openAlertsView());
        logoCard.setOnClickListener(v -> openFacesRegisteredView());
        electricityCard.setOnClickListener(v -> openEnergyAnalyticsView());
    }

    private void setupSwitchListener(SwitchCompat switchView, String portName, ImageView portImage) {
        switchView.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updatePortState(portName, isChecked);
            portImage.setImageResource(isChecked ? R.mipmap.switch_on : R.mipmap.switch_off);
        });
    }

    @SuppressLint("SetTextI18n")
    private void loadInitialData() {
        updateFireGasStatus(false);
        tempTextView.setText("Alerts");
        humidityTextView.setText("Faces Registered");
        aqiTextView.setText("Energy Analytics");
        initializePortState(switch1, imageView1);
        initializePortState(switch2, imageView2);
        initializePortState(switch3, imageView3);
        initializePortState(switch4, imageView4);
    }

    private void initializePortState(SwitchCompat switchView, ImageView imageView) {
        switchView.setOnCheckedChangeListener(null);
        switchView.setChecked(false);
        imageView.setImageResource(R.mipmap.switch_off);
        setupSwitchListener(switchView, "Port", imageView);
    }

    @SuppressLint("SetTextI18n")
    private void updateFireGasStatus(boolean danger) {
        if (danger) {
            fgStatusImageView.setImageResource(R.mipmap.danger_alert);
            homeFgStatusTextView.setText("DANGER: Fire or Gas detected!");
            homeFgStatusTextView.setTextColor(getResources().getColor(R.color.dangerColor));
        } else {
            fgStatusImageView.setImageResource(R.drawable.secure_home);
            homeFgStatusTextView.setText("Shifting from traditional ways to advanced secure technologies.");
            homeFgStatusTextView.setTextColor(getResources().getColor(R.color.textColor));
        }
    }

    private void updatePortState(String portName, boolean isOn) {
        Toast.makeText(getContext(), portName + " turned " + (isOn ? "ON" : "OFF"), Toast.LENGTH_SHORT).show();
    }

    private void openFireGasDetailView() {
        Toast.makeText(getContext(), "Opening SecureHome detailed view", Toast.LENGTH_SHORT).show();
    }

    private void openAirMonitoringDetailView() {
        Toast.makeText(getContext(), "Opening SecureHome Analytics detailed view", Toast.LENGTH_SHORT).show();
    }

    private void openAlertsView() {
        Toast.makeText(getContext(), "Opening Alerts View", Toast.LENGTH_SHORT).show();
    }

    private void openFacesRegisteredView() {
        Toast.makeText(getContext(), "Opening Faces Registered View", Toast.LENGTH_SHORT).show();
    }

    private void openEnergyAnalyticsView() {
        Intent intent = new Intent(getActivity(), DeviceEnergyMonitoringActivity.class);
        startActivity(intent);
        if (getActivity() != null) {
            getActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshDashboardData();
    }

    private void refreshDashboardData() {
        // TODO: Implement data refresh from sensors/backend
    }
}