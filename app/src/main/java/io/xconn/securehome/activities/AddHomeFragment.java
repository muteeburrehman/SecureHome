package io.xconn.securehome.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import io.xconn.securehome.R;
import io.xconn.securehome.models.Home;
import io.xconn.securehome.repository.HomeRepository;
import io.xconn.securehome.activities.DeviceListActivity;
import io.xconn.securehome.utils.ServerCheckUtility;

public class AddHomeFragment extends Fragment implements HomeRepository.OnHomeAddedListener {
    private EditText etOwner, etIpAddress, etPort;
    private Button btnAddHome;
    private ProgressBar progressBar;
    private HomeRepository homeRepository;
    private View rootView;
    private boolean isServerConfigured = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Do NOT initialize homeRepository here
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_add_home, container, false);
        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Check if server is configured first
        isServerConfigured = ServerCheckUtility.checkServerConfigured(this);

        // Only continue initialization if server is configured
        if (isServerConfigured) {
            homeRepository = new HomeRepository(requireContext());
            initializeComponents();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Check again when fragment resumes - this handles returning from ServerDiscoveryActivity
        if (!isServerConfigured) {
            isServerConfigured = ServerCheckUtility.checkServerConfigured(this);
            if (isServerConfigured) {
                homeRepository = new HomeRepository(requireContext());
                initializeComponents();
            }
        }
    }

    private void initializeComponents() {
        // Initialize UI components
        etOwner = rootView.findViewById(R.id.etOwner);
        etIpAddress = rootView.findViewById(R.id.etIpAddress);
        etPort = rootView.findViewById(R.id.etPort);
        btnAddHome = rootView.findViewById(R.id.btnAddHome);
        progressBar = rootView.findViewById(R.id.progressBar);

        // Set up loading observer
        homeRepository.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            btnAddHome.setEnabled(!isLoading);
        });

        // Set up error observer
        homeRepository.getErrorMessage().observe(getViewLifecycleOwner(), errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show();
            }
        });

        // Set up button click listener
        btnAddHome.setOnClickListener(v -> addHome());
    }

    private void addHome() {
        String owner = etOwner.getText().toString().trim();
        String ipAddress = etIpAddress.getText().toString().trim();
        String portStr = etPort.getText().toString().trim();

        // Validate input
        if (owner.isEmpty() || ipAddress.isEmpty() || portStr.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // No need to parse to int if we're going to use a string
        // Just validate that it contains only digits
        if (!portStr.matches("\\d+")) {
            Toast.makeText(requireContext(), "Port must be a valid number", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create and add new home with port as string
        Home newHome = new Home(owner, ipAddress, portStr);
        homeRepository.addHome(newHome, this);
    }

    @Override
    public void onHomeAdded(Home home) {
        Toast.makeText(requireContext(), "Home added successfully", Toast.LENGTH_SHORT).show();

        // Navigate to device list activity
        Intent intent = new Intent(requireContext(), DeviceListActivity.class);
        intent.putExtra("HOME_ID", home.getId());
        intent.putExtra("HOME_OWNER", home.getOwner());
        startActivity(intent);
    }

    @Override
    public void onError(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
    }
}