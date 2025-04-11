// app/src/main/java/io/xconn/securehome/ui/home/AddHomeFragment.java
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

public class AddHomeFragment extends Fragment implements HomeRepository.OnHomeAddedListener {
    private EditText etOwner, etIpAddress, etPort;
    private Button btnAddHome;
    private ProgressBar progressBar;
    private HomeRepository homeRepository;
    private View rootView;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        homeRepository = new HomeRepository(requireContext());
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

        int port;
        try {
            port = Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), "Port must be a valid number", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create and add new home
        Home newHome = new Home(owner, ipAddress, port);
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