package io.xconn.securehome.maincontroller;

import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.cardview.widget.CardView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import io.xconn.securehome.R;
import io.xconn.securehome.activities.EnergyMonitoringActivity;
import io.xconn.securehome.activities.RecognitionActivity;
import io.xconn.securehome.activities.RegisterActivity;
import io.xconn.securehome.activities.RegistrationActivity;

/**
 * Activities Fragment displays various activity options for the smart home system,
 * including Energy Monitoring, Facial Recognition Registration, and other features.
 */
public class ActivitiesFragment extends Fragment {

    // UI Components
    private CardView energyMonitoringCard;
    private CardView facialRecognitionCard;
    private CardView dummyOptionCard;

    public ActivitiesFragment() {
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
        View view = inflater.inflate(R.layout.fragment_activities, container, false);

        // Initialize UI elements
        initializeViews(view);

        // Set up click listeners
        setupClickListeners();

        return view;
    }

    /**
     * Initialize all view components from the layout
     */
    private void initializeViews(View view) {
        energyMonitoringCard = view.findViewById(R.id.card_energy_monitoring);
        facialRecognitionCard = view.findViewById(R.id.card_facial_recognition);
//        dummyOptionCard = view.findViewById(R.id.card_dummy_option);
    }

    /**
     * Set up click listeners for all cards
     */
    private void setupClickListeners() {
        // Energy Monitoring card click
        energyMonitoringCard.setOnClickListener(v -> {
            // Launch Energy Monitoring activity or dialog
            Intent energyIntent = new Intent(getActivity(), EnergyMonitoringActivity.class);
            startActivity(energyIntent);
        });

        // Facial Recognition Registration card click
        facialRecognitionCard.setOnClickListener(v -> {
            // Launch Facial Recognition Registration activity
            Intent registrationIntent = new Intent(getActivity(), RegisterActivity.class);
            startActivity(registrationIntent);
        });

        // Dummy Option card click
//        dummyOptionCard.setOnClickListener(v -> {
//            // Launch dummy option activity or show functionality
//            Intent dummyIntent = new Intent(getActivity(), DummyOptionActivity.class);
//            startActivity(dummyIntent);
//        });
    }
}