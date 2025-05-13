package io.xconn.securehome.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import io.xconn.securehome.R;
import io.xconn.securehome.adapters.AlertAdapter;
import io.xconn.securehome.api.FirebaseAuthManager;
import io.xconn.securehome.models.AlertModel;
import io.xconn.securehome.models.UserModel;

/**
 * Activity for displaying security and emergency alerts
 * Only available to admin users
 */
public class AlertsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private AlertAdapter adapter;
    private ProgressBar progressBar;
    private TextView tvNoAlerts;
    private SwipeRefreshLayout swipeRefreshLayout;
    private FirebaseFirestore db;
    private FirebaseAuthManager authManager;
    private List<AlertModel> alertsList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alerts);

        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.alert));


        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Security Alerts");
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // Initialize Firebase components
        db = FirebaseFirestore.getInstance();
        authManager = FirebaseAuthManager.getInstance(this);

        // Check if user is admin
        checkUserPermissions();

        // Initialize views
        initViews();

        // Set up the adapter for the RecyclerView
        setupRecyclerView();

        // Set up refresh listener
        swipeRefreshLayout.setOnRefreshListener(this::loadAlerts);

        // Load alerts
        loadAlerts();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recycler_alerts);
        progressBar = findViewById(R.id.progress_bar);
        tvNoAlerts = findViewById(R.id.tv_no_alerts);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh);
    }

    private void setupRecyclerView() {
        adapter = new AlertAdapter(this, alertsList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void checkUserPermissions() {
        authManager.getCurrentUserModel(new FirebaseAuthManager.AuthCallback() {
            @Override
            public void onSuccess(UserModel userModel) {
                if (!authManager.isAdmin(userModel)) {
                    // User is not an admin, show error and exit
                    showError("Access denied. Admin privileges required.");
                    finish();
                }
            }

            @Override
            public void onFailure(Exception e) {
                showError("Failed to verify user permissions: " + e.getMessage());
                finish();
            }
        });
    }

    private void loadAlerts() {
        showProgress(true);

        // Clear existing alerts
        alertsList.clear();

        // Query alerts from Firestore, ordered by timestamp (newest first)
        db.collection("alerts")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        showNoAlerts(true);
                    } else {
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            AlertModel alert = document.toObject(AlertModel.class);
                            alertsList.add(alert);
                        }
                        adapter.notifyDataSetChanged();
                        showNoAlerts(false);
                    }
                    showProgress(false);
                    swipeRefreshLayout.setRefreshing(false);
                })
                .addOnFailureListener(e -> {
                    showError("Failed to load alerts: " + e.getMessage());
                    showProgress(false);
                    swipeRefreshLayout.setRefreshing(false);
                });
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void showNoAlerts(boolean show) {
        tvNoAlerts.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void showError(String message) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show();
    }
}