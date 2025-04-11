package io.xconn.securehome.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import io.xconn.securehome.R;
import io.xconn.securehome.adapters.HomeAdapter;
import io.xconn.securehome.models.Home;
import io.xconn.securehome.repository.HomeRepository;

public class HomeListActivity extends AppCompatActivity implements HomeAdapter.OnHomeClickListener {
    private RecyclerView recyclerView;
    private HomeAdapter adapter;
    private ProgressBar progressBar;
    private Button btnAddHome;
    private SwipeRefreshLayout swipeRefreshLayout;
    private HomeRepository homeRepository;
    private boolean isHomesLoaded = false;  // To prevent unnecessary fetch onResume

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_list);

        homeRepository = new HomeRepository(this);

        // Initialize UI components
        recyclerView = findViewById(R.id.recyclerViewHomes);
        progressBar = findViewById(R.id.progressBar);
        btnAddHome = findViewById(R.id.btnAddHome);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HomeAdapter(this);
        recyclerView.setAdapter(adapter);

        // Setup click listener for add home button
        btnAddHome.setOnClickListener(v -> {
            // Load AddHomeFragment into a container
            AddHomeFragment addHomeFragment = new AddHomeFragment();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, addHomeFragment)
                    .addToBackStack(null)
                    .commit();
        });

        // Setup SwipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener(() -> {
            homeRepository.fetchHomes();  // Refresh data
        });

        // Observe LiveData
        observeViewModel();

        // Initial load
        if (!isHomesLoaded) {
            homeRepository.fetchHomes();
        }
    }

    private void observeViewModel() {
        // Observe homes
        homeRepository.getHomes().observe(this, homes -> {
            adapter.setHomes(homes);

            if (homes.isEmpty()) {
                // Show empty view or message
                findViewById(R.id.tvEmptyHomes).setVisibility(View.VISIBLE);
            } else {
                findViewById(R.id.tvEmptyHomes).setVisibility(View.GONE);
            }

            isHomesLoaded = true;  // Set flag to prevent reload in onResume
        });

        // Observe loading state
        homeRepository.getIsLoading().observe(this, isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);

            if (!isLoading) {
                swipeRefreshLayout.setRefreshing(false);  // Stop refresh animation
            }
        });

        // Observe error messages
        homeRepository.getErrorMessage().observe(this, errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Only refresh the list if data was not already loaded
        if (!isHomesLoaded) {
            homeRepository.fetchHomes();
        }
    }

    @Override
    public void onHomeClick(Home home) {
        // Navigate to DeviceListActivity
        Intent intent = new Intent(this, DeviceListActivity.class);
        intent.putExtra("HOME_ID", home.getId());
        intent.putExtra("HOME_OWNER", home.getOwner());
        startActivity(intent);
    }
}
