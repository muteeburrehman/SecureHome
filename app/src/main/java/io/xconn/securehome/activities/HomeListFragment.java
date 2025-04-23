package io.xconn.securehome.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import io.xconn.securehome.R;
import io.xconn.securehome.activities.DeviceListActivity;
import io.xconn.securehome.adapters.HomeAdapter;
import io.xconn.securehome.models.Home;
import io.xconn.securehome.repository.HomeRepository;

public class HomeListFragment extends Fragment implements HomeAdapter.OnHomeClickListener {
    private static final String TAG = "HomeListFragment";
    private RecyclerView recyclerView;
    private HomeAdapter adapter;
    private ProgressBar progressBar;
    private Button btnAddHome;
    private SwipeRefreshLayout swipeRefreshLayout;
    private HomeRepository homeRepository;
    private View rootView;
    private boolean isHomesLoaded = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView called");
        rootView = inflater.inflate(R.layout.fragment_home_list, container, false);
        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated called");

        homeRepository = new HomeRepository(requireContext());

        // Initialize UI components
        recyclerView = rootView.findViewById(R.id.recyclerViewHomes);
        progressBar = rootView.findViewById(R.id.progressBar);
        btnAddHome = rootView.findViewById(R.id.btnAddHome);
        swipeRefreshLayout = rootView.findViewById(R.id.swipeRefreshLayout);

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new HomeAdapter(this);
        recyclerView.setAdapter(adapter);

        // Setup click listener for add home button
        btnAddHome.setOnClickListener(v -> {
            Log.d(TAG, "Add Home button clicked");
            navigateToAddHomeFragment();
        });

        // Setup SwipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener(() -> {
            Log.d(TAG, "SwipeRefresh triggered");
            homeRepository.fetchHomes();  // Refresh data
        });

        // Observe LiveData
        observeViewModel();

        // Initial load
        if (!isHomesLoaded) {
            Log.d(TAG, "Initial data loading");
            homeRepository.fetchHomes();
        }
    }

    private void navigateToAddHomeFragment() {
        Log.d(TAG, "Navigating to AddHomeFragment");
        // Import the fragment from your activities package if needed
        Fragment addHomeFragment = new io.xconn.securehome.activities.AddHomeFragment();

        // Replace current fragment with AddHomeFragment
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, addHomeFragment)
                .addToBackStack("home_list")
                .commit();
    }

    private void observeViewModel() {
        // Observe homes
        homeRepository.getHomes().observe(getViewLifecycleOwner(), homes -> {
            Log.d(TAG, "Home list updated, count: " + homes.size());
            adapter.setHomes(homes);

            if (homes.isEmpty()) {
                // Show empty view or message
                rootView.findViewById(R.id.tvEmptyHomes).setVisibility(View.VISIBLE);
            } else {
                rootView.findViewById(R.id.tvEmptyHomes).setVisibility(View.GONE);
            }

            isHomesLoaded = true;  // Set flag to prevent reload in onResume
        });

        // Observe loading state
        homeRepository.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);

            if (!isLoading) {
                swipeRefreshLayout.setRefreshing(false);  // Stop refresh animation
            }
        });

        // Observe error messages
        homeRepository.getErrorMessage().observe(getViewLifecycleOwner(), errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called");
        // Only refresh the list if data was not already loaded or when returning from AddHomeFragment
        if (!isHomesLoaded || getParentFragmentManager().getBackStackEntryCount() > 0) {
            Log.d(TAG, "Refreshing data in onResume");
            homeRepository.fetchHomes();
            // Clear back stack if returning from add home
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            }
        }
    }

    @Override
    public void onHomeClick(Home home) {
        Log.d(TAG, "Home clicked: " + home.getId());
        // Navigate to DeviceListActivity
        Intent intent = new Intent(requireContext(), DeviceListActivity.class);
        intent.putExtra("HOME_ID", home.getId());
        intent.putExtra("HOME_OWNER", home.getOwner());
        startActivity(intent);
    }
}