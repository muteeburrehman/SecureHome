package io.xconn.securehome.alerts;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

import io.xconn.securehome.R;
import io.xconn.securehome.adapters.AlertsAdapter;
import io.xconn.securehome.models.AlertModel;

/**
 * Fragment for displaying security and system alerts
 */
public class AlertsFragment extends Fragment implements AlertsAdapter.AlertInteractionListener {

    private static final String TAG = "AlertsFragment";

    // UI components
    private RecyclerView alertsRecyclerView;
    private LinearLayout emptyStateView;
    private ChipGroup filterChipGroup;
    private Chip chipAll, chipSecurity, chipFire, chipSystem;
    private ExtendedFloatingActionButton markAllReadFab;

    // Data
    private List<AlertModel> alertsList;
    private List<AlertModel> filteredAlertsList;
    private AlertsAdapter alertsAdapter;

    // Firebase
    private FirebaseFirestore db;
    private String currentUserId;

    // Filter state
    private String currentFilter = "all";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_alerts, container, false);
        initializeViews(rootView);
        setupFirebase();
        setupRecyclerView();
        setupFilterChips();
        setupMarkAllReadButton();
        loadAlerts();
        return rootView;
    }

    private void initializeViews(View view) {
        alertsRecyclerView = view.findViewById(R.id.alertsRecyclerView);
        emptyStateView = view.findViewById(R.id.emptyStateView);
        filterChipGroup = view.findViewById(R.id.filterChipGroup);
        chipAll = view.findViewById(R.id.chipAll);
        chipSecurity = view.findViewById(R.id.chipSecurity);
        chipFire = view.findViewById(R.id.chipFire);
        chipSystem = view.findViewById(R.id.chipSystem);
        markAllReadFab = view.findViewById(R.id.markAllReadFab);
    }

    private void setupFirebase() {
        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
    }

    private void setupRecyclerView() {
        alertsList = new ArrayList<>();
        filteredAlertsList = new ArrayList<>();

        alertsAdapter = new AlertsAdapter(getContext(), filteredAlertsList, this);
        alertsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        alertsRecyclerView.setAdapter(alertsAdapter);
    }

    private void setupFilterChips() {
        filterChipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipAll) {
                currentFilter = "all";
            } else if (checkedId == R.id.chipSecurity) {
                currentFilter = AlertModel.TYPE_SECURITY;
            } else if (checkedId == R.id.chipFire) {
                currentFilter = AlertModel.TYPE_FIRE_GAS;
            } else if (checkedId == R.id.chipSystem) {
                currentFilter = AlertModel.TYPE_SYSTEM;
            }

            filterAlerts();
        });
    }

    private void setupMarkAllReadButton() {
        markAllReadFab.setOnClickListener(v -> {
            if (filteredAlertsList.isEmpty()) {
                Toast.makeText(getContext(), "No alerts to mark as read", Toast.LENGTH_SHORT).show();
                return;
            }

            markAllAlertsAsRead();
        });
    }

    private void loadAlerts() {
        if (currentUserId == null) {
            updateEmptyState(true);
            return;
        }

        db.collection("alerts")
                .whereEqualTo("userId", currentUserId)
                .whereEqualTo("dismissed", false)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        processAlertsQueryResult(task.getResult());
                    } else {
                        Log.e(TAG, "Error loading alerts", task.getException());
                        Toast.makeText(getContext(), "Failed to load alerts", Toast.LENGTH_SHORT).show();
                        updateEmptyState(true);
                    }
                });
    }

    private void processAlertsQueryResult(QuerySnapshot querySnapshot) {
        alertsList.clear();

        if (querySnapshot != null && !querySnapshot.isEmpty()) {
            for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                AlertModel alert = document.toObject(AlertModel.class);
                if (alert != null) {
                    alertsList.add(alert);
                }
            }
        }

        filterAlerts();
    }

    private void filterAlerts() {
        filteredAlertsList.clear();

        if (currentFilter.equals("all")) {
            filteredAlertsList.addAll(alertsList);
        } else {
            for (AlertModel alert : alertsList) {
                if (alert.getType().equals(currentFilter)) {
                    filteredAlertsList.add(alert);
                }
            }
        }

        alertsAdapter.notifyDataSetChanged();
        updateEmptyState(filteredAlertsList.isEmpty());
    }

    private void updateEmptyState(boolean isEmpty) {
        if (isEmpty) {
            alertsRecyclerView.setVisibility(View.GONE);
            emptyStateView.setVisibility(View.VISIBLE);
        } else {
            alertsRecyclerView.setVisibility(View.VISIBLE);
            emptyStateView.setVisibility(View.GONE);
        }
    }

    private void markAllAlertsAsRead() {
        // First update the UI
        alertsAdapter.markAllAsRead();

        // Then update Firestore
        List<String> alertIds = new ArrayList<>();
        for (AlertModel alert : filteredAlertsList) {
            alertIds.add(alert.getId());
        }

        // Use a batch operation to update all alerts at once
        updateAlertsReadStatusInBatch(alertIds, true);
    }

    private void updateAlertsReadStatusInBatch(List<String> alertIds, boolean read) {
        if (alertIds.isEmpty()) return;

        db.runBatch(batch -> {
            for (String alertId : alertIds) {
                batch.update(db.collection("alerts").document(alertId), "read", read);
            }
        }).addOnSuccessListener(aVoid -> {
            String message = read ? "All alerts marked as read" : "Alert status updated";
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error updating alerts", e);
            Toast.makeText(getContext(), "Failed to update alerts", Toast.LENGTH_SHORT).show();
        });
    }

    // AlertsAdapter.AlertInteractionListener implementation
    @Override
    public void onAlertClicked(AlertModel alert, int position) {
        if (!alert.isRead()) {
            // Mark as read in adapter
            alertsAdapter.markAsRead(position);

            // Update in Firestore
            updateAlertReadStatus(alert.getId(), true);
        }

        // You can also navigate to detail view if needed
        navigateToAlertDetail(alert);
    }

    @Override
    public void onViewDetailsClicked(AlertModel alert, int position) {
        if (!alert.isRead()) {
            // Mark as read in adapter
            alertsAdapter.markAsRead(position);

            // Update in Firestore
            updateAlertReadStatus(alert.getId(), true);
        }

        // Navigate to alert detail view
        navigateToAlertDetail(alert);
    }

    @Override
    public void onDismissClicked(AlertModel alert, int position) {
        // Update the UI first
        alertsAdapter.removeAlert(position);

        // Check if list is now empty to show empty state
        if (filteredAlertsList.isEmpty()) {
            updateEmptyState(true);
        }

        // Update in Firestore
        updateAlertDismissedStatus(alert.getId(), true);
    }

    private void updateAlertReadStatus(String alertId, boolean read) {
        db.collection("alerts").document(alertId)
                .update("read", read)
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating alert read status", e);
                });
    }

    private void updateAlertDismissedStatus(String alertId, boolean dismissed) {
        db.collection("alerts").document(alertId)
                .update("dismissed", dismissed)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Alert dismissed", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error dismissing alert", e);
                    Toast.makeText(getContext(), "Failed to dismiss alert", Toast.LENGTH_SHORT).show();
                });
    }

    private void navigateToAlertDetail(AlertModel alert) {
        // TODO: Implement navigation to alert detail
        // For now, just show a toast with alert title
        Toast.makeText(getContext(), "Viewing: " + alert.getTitle(), Toast.LENGTH_SHORT).show();

        // You could navigate to a detail fragment or activity like this:
        // Bundle args = new Bundle();
        // args.putString("alert_id", alert.getId());
        // Navigation.findNavController(requireView()).navigate(R.id.action_alertsFragment_to_alertDetailFragment, args);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reload alerts when returning to this fragment
        loadAlerts();
    }
}