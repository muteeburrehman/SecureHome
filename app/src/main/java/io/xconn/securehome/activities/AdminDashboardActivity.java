package io.xconn.securehome.activities;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import io.xconn.securehome.R;
import io.xconn.securehome.adapters.UserAdapter;
import io.xconn.securehome.api.FirebaseDatabaseManager;
import io.xconn.securehome.models.UserModel;
import io.xconn.securehome.services.EmailService;
import io.xconn.securehome.services.NotificationService;

public class AdminDashboardActivity extends AppCompatActivity implements UserAdapter.OnUserActionListener {

    private RecyclerView userRecyclerView;
    private ProgressBar progressBar;
    private TextView emptyView;
    private TabLayout tabLayout;
    private Toolbar toolbar;

    private UserAdapter userAdapter;
    private List<UserModel> userList;
    private FirebaseDatabaseManager dbManager;

    private static final int TAB_ALL = 0;
    private static final int TAB_PENDING = 1;
    private static final int TAB_APPROVED = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimary));

        dbManager = FirebaseDatabaseManager.getInstance();

        initializeViews();
        setupRecyclerView();
        setupTabLayout();

        // Load all users by default
        loadUsers(TAB_ALL);
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("User Management");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        userRecyclerView = findViewById(R.id.user_recycler_view);
        progressBar = findViewById(R.id.progress_bar);
        emptyView = findViewById(R.id.empty_view);
        tabLayout = findViewById(R.id.tab_layout);
    }

    private void setupRecyclerView() {
        userList = new ArrayList<>();
        userAdapter = new UserAdapter(this, userList, this);
        userRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        userRecyclerView.setAdapter(userAdapter);
    }

    private void setupTabLayout() {
        tabLayout.addTab(tabLayout.newTab().setText("All Users"));
        tabLayout.addTab(tabLayout.newTab().setText("Pending"));
        tabLayout.addTab(tabLayout.newTab().setText("Approved"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                loadUsers(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                loadUsers(tab.getPosition());
            }
        });
    }

    private void loadUsers(int tabPosition) {
        showLoading(true);
        emptyView.setText("Loading users...");

        switch (tabPosition) {
            case TAB_ALL:
                dbManager.getAllUsers(task -> {
                    if (task.isSuccessful()) {
                        handleUsersLoaded(task.getResult());
                    } else {
                        handleLoadError(task.getException());
                    }
                });
                break;
            case TAB_PENDING:
                dbManager.getUsersByApprovalStatus(UserModel.STATUS_PENDING, task -> {
                    if (task.isSuccessful()) {
                        handleUsersLoaded(task.getResult());
                    } else {
                        handleLoadError(task.getException());
                    }
                });
                break;
            case TAB_APPROVED:
                dbManager.getUsersByApprovalStatus(UserModel.STATUS_APPROVED, task -> {
                    if (task.isSuccessful()) {
                        handleUsersLoaded(task.getResult());
                    } else {
                        handleLoadError(task.getException());
                    }
                });
                break;
        }
    }



    private void handleUsersLoaded(com.google.firebase.firestore.QuerySnapshot result) {
        showLoading(false);
        userList.clear();
        int currentTab = tabLayout.getSelectedTabPosition();
        String tabName = currentTab == TAB_ALL ? "ALL" : (currentTab == TAB_PENDING ? "PENDING" : "APPROVED");

        Log.d("AdminDashboard", "handleUsersLoaded for tab: " + tabName);

        if (result != null && !result.isEmpty()) {
            Log.d("AdminDashboard", "Query returned " + result.size() + " documents for tab " + tabName);
            for (DocumentSnapshot document : result.getDocuments()) {
                UserModel user = document.toObject(UserModel.class);
                if (user != null) {
                    if (user.getUserId() == null) { // Good practice
                        user.setUserId(document.getId());
                    }
                    Log.d("AdminDashboard", "Fetched User -> Email: " + user.getEmail() +
                            ", Role: " + user.getRole() +
                            ", Status: " + user.getApprovalStatus());
                    userList.add(user);
                } else {
                    Log.w("AdminDashboard", "Document " + document.getId() + " could not be converted to UserModel.");
                }
            }
        } else {
            Log.d("AdminDashboard", "Query for tab " + tabName + " returned null or empty result.");
        }

        userAdapter.updateUserList(userList); // Make sure this is called after populating userList
        updateEmptyView();
    }

    private void handleLoadError(Exception e) {
        showLoading(false);
        userList.clear();
        userAdapter.updateUserList(userList); // Update adapter even on error
        int currentTab = tabLayout.getSelectedTabPosition();
        String tabName = currentTab == TAB_ALL ? "ALL" : (currentTab == TAB_PENDING ? "PENDING" : "APPROVED");
        Log.e("AdminDashboard", "Error loading users for tab " + tabName, e);

        emptyView.setText("Error loading users: " + (e != null ? e.getMessage() : "Unknown error"));
        emptyView.setVisibility(View.VISIBLE);
        userRecyclerView.setVisibility(View.GONE);
        Toast.makeText(this, "Failed to load users: " +
                (e != null ? e.getMessage() : "Unknown error"), Toast.LENGTH_SHORT).show();
    }

    private void updateEmptyView() {
        if (userList.isEmpty()) {
            switch (tabLayout.getSelectedTabPosition()) {
                case TAB_ALL:
                    emptyView.setText("No users found");
                    break;
                case TAB_PENDING:
                    emptyView.setText("No pending users found");
                    break;
                case TAB_APPROVED:
                    emptyView.setText("No approved users found");
                    break;
            }
            emptyView.setVisibility(View.VISIBLE);
            userRecyclerView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            userRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onApproveClick(UserModel user, int position) {
        showLoading(true);
        dbManager.updateUserApprovalStatus(user.getUserId(), UserModel.STATUS_APPROVED, task -> {
            showLoading(false);
            if (task.isSuccessful()) {
                // Update the user object in our list
                user.setApprovalStatus(UserModel.STATUS_APPROVED);
                userAdapter.updateUserStatus(position, UserModel.STATUS_APPROVED);

                // Send approval notification via FCM
                NotificationService.sendApprovalNotificationToUser(user.getUserId(), user.getDisplayName());

                // Send approval email
                EmailService emailService = new EmailService(this);
                emailService.sendApprovalNotificationToUser(user.getEmail(), user.getDisplayName());

                Toast.makeText(this, "User approved successfully", Toast.LENGTH_SHORT).show();

                // If we're in the pending tab, refresh to show accurate list
                if (tabLayout.getSelectedTabPosition() == TAB_PENDING) {
                    loadUsers(TAB_PENDING);
                }
            } else {
                Toast.makeText(this, "Failed to approve user: " +
                                (task.getException() != null ? task.getException().getMessage() : "Unknown error"),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }


    @Override
    public void onRejectClick(UserModel user, int position) {
        // Show dialog to get rejection reason
        final EditText reasonInput = new EditText(this);
        reasonInput.setHint("Reason for rejection (optional)");

        new AlertDialog.Builder(this)
                .setTitle("Reject User")
                .setMessage("Please provide a reason for rejection (optional):")
                .setView(reasonInput)
                .setPositiveButton("Reject", (dialog, which) -> {
                    String reason = reasonInput.getText().toString().trim();
                    performUserRejection(user, position, reason);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performUserRejection(UserModel user, int position, String reason) {
        showLoading(true);
        dbManager.updateUserApprovalStatus(user.getUserId(), UserModel.STATUS_REJECTED, task -> {
            showLoading(false);
            if (task.isSuccessful()) {
                // Update the user object in our list
                user.setApprovalStatus(UserModel.STATUS_REJECTED);
                userAdapter.updateUserStatus(position, UserModel.STATUS_REJECTED);

                // Send rejection notification via FCM
                NotificationService.sendRejectionNotificationToUser(user.getUserId(), user.getDisplayName(), reason);

                // Send rejection email
                EmailService emailService = new EmailService(this);
                emailService.sendRejectionNotificationToUser(user.getEmail(), user.getDisplayName(), reason);

                Toast.makeText(this, "User rejected", Toast.LENGTH_SHORT).show();

                // If we're in a filtered tab, refresh to show accurate list
                int currentTab = tabLayout.getSelectedTabPosition();
                if (currentTab == TAB_PENDING || currentTab == TAB_APPROVED) {
                    loadUsers(currentTab);
                }
            } else {
                Toast.makeText(this, "Failed to reject user: " +
                                (task.getException() != null ? task.getException().getMessage() : "Unknown error"),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
    @Override
    public void onRemoveClick(UserModel user, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Remove User")
                .setMessage("Are you sure you want to permanently remove this user?")
                .setPositiveButton("Remove", (dialog, which) -> {
                    performUserRemoval(user, position);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performUserRemoval(UserModel user, int position) {
        showLoading(true);
        dbManager.deleteUser(user.getUserId(), task -> {
            showLoading(false);
            if (task.isSuccessful()) {
                userAdapter.removeUser(position);
                Toast.makeText(this, "User removed successfully", Toast.LENGTH_SHORT).show();
                updateEmptyView();
            } else {
                Toast.makeText(this, "Failed to remove user: " +
                                (task.getException() != null ? task.getException().getMessage() : "Unknown error"),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload users when returning to the activity
        loadUsers(tabLayout.getSelectedTabPosition());
    }
}