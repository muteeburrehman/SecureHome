package io.xconn.securehome.maincontroller;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.view.animation.AnimationUtils;

import io.xconn.securehome.activities.AdminDashboardActivity;
import io.xconn.securehome.activities.DeviceEnergyMonitoringActivity;
import io.xconn.securehome.activities.EmergencyContactActivity;
import io.xconn.securehome.activities.AlertsActivity;
import io.xconn.securehome.api.FirebaseAuthManager;
import io.xconn.securehome.models.UserModel;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.snackbar.Snackbar;
import io.xconn.securehome.R;

/**
 * Dashboard Fragment - Main control center for SecureHome application
 * Displays security status and provides access to main features
 */
public class DashboardFragment extends Fragment {
    // UI Components
    private ImageView fgStatusImageView;
    private TextView homeFgStatusTextView;
    private TextView tempTextView, humidityTextView, aqiTextView;
    private Button btnEmergency;

    // Cards for section control
    private MaterialCardView fgnCard, airmCard, alertCard, logoCard, electricityCard;
    private View rootView;

    // Auth manager for user permission checks
    private FirebaseAuthManager authManager;

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
        rootView = inflater.inflate(R.layout.fragment_dashboard, container, false);

        // Initialize Firebase Auth Manager
        authManager = FirebaseAuthManager.getInstance(requireContext());

        // Initialize UI components and setup interactions
        initializeViews(rootView);
        setupCardListeners();
        setupButtonListeners();
        loadInitialData();
        applyAnimations();

        return rootView;
    }

    /**
     * Initialize all view references from the layout
     */
    private void initializeViews(View view) {
        // Cards
        fgnCard = view.findViewById(R.id.fgn);
        airmCard = view.findViewById(R.id.airm);
        alertCard = view.findViewById(R.id.alertCard);
        logoCard = view.findViewById(R.id.logoCard);
        electricityCard = view.findViewById(R.id.electricityCard);

        // Buttons
        btnEmergency = view.findViewById(R.id.btnEmergency);

        // Text and images
        fgStatusImageView = view.findViewById(R.id.fg_status);
        homeFgStatusTextView = view.findViewById(R.id.home_fg_status);
        tempTextView = view.findViewById(R.id.temp);
        humidityTextView = view.findViewById(R.id.humidity);
        aqiTextView = view.findViewById(R.id.aqi);
    }

    /**
     * Set up click listeners for all card elements
     */
    private void setupCardListeners() {
        fgnCard.setOnClickListener(v -> openFireGasDetailView());
        airmCard.setOnClickListener(v -> openAirMonitoringDetailView());
        alertCard.setOnClickListener(v -> openAlertsView());
        logoCard.setOnClickListener(v -> openFacesRegisteredView());
        electricityCard.setOnClickListener(v -> openEnergyAnalyticsView());
    }

    /**
     * Set up click listeners for buttons
     */
    private void setupButtonListeners() {
        btnEmergency.setOnClickListener(v -> openEmergencyContactView());
    }

    /**
     * Load initial data and set default text values
     */
    @SuppressLint("SetTextI18n")
    private void loadInitialData() {
        updateFireGasStatus(false);
        tempTextView.setText("Alerts");
        humidityTextView.setText("Access Control");
        aqiTextView.setText("Energy Analytics");
    }

    /**
     * Apply animations to UI elements
     */
    private void applyAnimations() {
        // Apply fade-in animations to cards
        fgnCard.startAnimation(AnimationUtils.loadAnimation(getContext(), android.R.anim.fade_in));
        airmCard.startAnimation(AnimationUtils.loadAnimation(getContext(), android.R.anim.fade_in));
        alertCard.startAnimation(AnimationUtils.loadAnimation(getContext(), android.R.anim.fade_in));
        logoCard.startAnimation(AnimationUtils.loadAnimation(getContext(), android.R.anim.fade_in));
        electricityCard.startAnimation(AnimationUtils.loadAnimation(getContext(), android.R.anim.fade_in));
    }

    /**
     * Update the fire/gas status display based on sensor data
     */
    @SuppressLint("SetTextI18n")
    private void updateFireGasStatus(boolean danger) {
        if (danger) {
            fgStatusImageView.setImageResource(R.mipmap.danger_alert);
            homeFgStatusTextView.setText("DANGER: Fire or Gas detected!");
            homeFgStatusTextView.setTextColor(getResources().getColor(R.color.dangerColor));

            // Show a persistent alert when danger is detected
            Snackbar snackbar = Snackbar.make(rootView, "EMERGENCY: Fire or Gas detected!", Snackbar.LENGTH_INDEFINITE)
                    .setAction("DISMISS", v -> {})
                    .setActionTextColor(getResources().getColor(R.color.white));
            snackbar.getView().setBackgroundColor(getResources().getColor(R.color.dangerColor));
            snackbar.show();
        } else {
            fgStatusImageView.setImageResource(R.drawable.secure_home);
            homeFgStatusTextView.setText("Your home is secure and protected.");
            homeFgStatusTextView.setTextColor(getResources().getColor(R.color.textColor));
        }
    }

    // Navigation methods
    private void openFireGasDetailView() {
        showFeedback("Opening Home Security Status");
        // TODO: Implement navigation to Fire/Gas detailed view
    }

    private void openAirMonitoringDetailView() {
        showFeedback("Opening Analytics Dashboard");
        // TODO: Implement navigation to Analytics Dashboard
    }

    private void openAlertsView() {
        // Check if user is admin before allowing access to Alerts
        if (authManager.isUserLoggedIn()) {
            authManager.getCurrentUserModel(new FirebaseAuthManager.AuthCallback() {
                @Override
                public void onSuccess(UserModel userModel) {
                    if (authManager.isAdmin(userModel)) {
                        showFeedback("Opening Security Alerts");

                        // Navigate to AlertsActivity for admin users
                        Intent intent = new Intent(getActivity(), AlertsActivity.class);
                        startActivity(intent);
                        if (getActivity() != null) {
                            getActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                        }
                    } else {
                        // User is not an admin, show access denied message
                        showFeedback("Access Denied: Admin permission required");
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    showFeedback("Error checking permissions. Please try again.");
                }
            });
        } else {
            showFeedback("Please login to access this feature");
        }
    }

    private void openFacesRegisteredView() {
        // Check if user is admin before allowing access to Alerts
        if (authManager.isUserLoggedIn()) {
            authManager.getCurrentUserModel(new FirebaseAuthManager.AuthCallback() {
                @Override
                public void onSuccess(UserModel userModel) {
                    if (authManager.isAdmin(userModel)) {
                        showFeedback("Opening Access Control");

                        // Navigate to AlertsActivity for admin users
                        Intent intent = new Intent(getActivity(), AdminDashboardActivity.class);
                        startActivity(intent);
                        if (getActivity() != null) {
                            getActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                        }
                    } else {
                        // User is not an admin, show access denied message
                        showFeedback("Access Denied: Admin permission required");
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    showFeedback("Error checking permissions. Please try again.");
                }
            });
        } else {
            showFeedback("Please login to access this feature");
        }

    }

    private void openEnergyAnalyticsView() {
        showFeedback("Opening Energy Analytics");
        Intent intent = new Intent(getActivity(), DeviceEnergyMonitoringActivity.class);
        startActivity(intent);
        if (getActivity() != null) {
            getActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        }
    }

    private void openEmergencyContactView() {
        showFeedback("Opening Emergency Contact");
        Intent intent = new Intent(getActivity(), EmergencyContactActivity.class);
        startActivity(intent);
        if (getActivity() != null) {
            getActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        }
    }

    /**
     * Display feedback message to user
     */
    private void showFeedback(String message) {
        // Using Snackbar instead of Toast for better UI
        Snackbar.make(rootView, message, Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshDashboardData();
    }

    /**
     * Refresh dashboard data from sensors/backend
     */
    private void refreshDashboardData() {
        // TODO: Implement data refresh from sensors/backend
        // This could include checking for security alerts, updating status, etc.
    }

    /**
     * Public method to update danger status from outside the fragment
     * (can be called from your API/background service)
     */
    public void updateDangerStatus(boolean dangerDetected) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> updateFireGasStatus(dangerDetected));
        }
    }
}