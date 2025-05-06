package io.xconn.securehome;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;

import io.xconn.securehome.activities.AdminDashboardActivity;
import io.xconn.securehome.activities.ChangePasswordActivity;
import io.xconn.securehome.activities.EditProfileActivity;
import io.xconn.securehome.activities.HomeListFragment;
import io.xconn.securehome.activities.LoginActivity;
import io.xconn.securehome.activities.PendingApprovalActivity;
import io.xconn.securehome.activities.ServerConfigActivity;
import io.xconn.securehome.api.FirebaseAuthManager;
import io.xconn.securehome.maincontroller.ActivitiesFragment;
import io.xconn.securehome.maincontroller.DashboardFragment;
import io.xconn.securehome.maincontroller.Esp32CamFragment;
import io.xconn.securehome.models.UserModel;
import io.xconn.securehome.utils.NetworkChangeReceiver;
import io.xconn.securehome.utils.ServerCheckUtility;
import io.xconn.securehome.utils.SessionManager;

public class MainActivity extends AppCompatActivity implements
        NavigationView.OnNavigationItemSelectedListener,
        NetworkChangeReceiver.NetworkChangeListener {

    private SessionManager sessionManager;
    private FirebaseAuthManager authManager;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private BottomNavigationView bottomNavigationView;
    private Toolbar toolbar;
    private NetworkChangeReceiver networkChangeReceiver;
    private boolean isNetworkCheckPending = false;
    private TextView userNameHeaderView; // Added for user name display in nav header

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize managers
        sessionManager = new SessionManager(this);
        authManager = FirebaseAuthManager.getInstance();

        // Setup network change receiver
        networkChangeReceiver = new NetworkChangeReceiver(this, this);
        networkChangeReceiver.register();

        // Check login state using Firebase
        if (!authManager.isUserLoggedIn()) {
            redirectToLogin();
            return;
        }

        // Check user approval status
        checkUserStatus();

        // Check if server is configured
        if (!ServerCheckUtility.checkServerConfigured(this)) {
            // The ServerCheckUtility will handle redirection to ServerConfigActivity
            isNetworkCheckPending = true;
            return;
        }

        // Setup UI components
        setupToolbar();
        setupNavigationDrawer();
        setupBottomNavigation();

        // Load default fragment (Dashboard) if first launch
        if (savedInstanceState == null) {
            switchFragment(new DashboardFragment(), "DASHBOARD");
            bottomNavigationView.setSelectedItemId(R.id.nav_dashboard);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // If we're returning from ServerConfigActivity, check again
        if (isNetworkCheckPending) {
            isNetworkCheckPending = false;
            if (ServerCheckUtility.isServerConfigured(this)) {
                // Now initialize the activity components
                setupToolbar();
                setupNavigationDrawer();
                setupBottomNavigation();

                // Load default fragment
                switchFragment(new DashboardFragment(), "DASHBOARD");
                bottomNavigationView.setSelectedItemId(R.id.nav_dashboard);
            } else {
                // Still not configured, we'll wait
                isNetworkCheckPending = true;
            }
        }

        // Check user status whenever we resume the activity
        if (authManager.isUserLoggedIn()) {
            checkUserStatus();
        }
    }

    private void setupToolbar() {
        toolbar = findViewById(R.id.toolbarNav);
        setSupportActionBar(toolbar);
    }

    private void setupNavigationDrawer() {
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Update navigation header with user info
        View headerView = navigationView.getHeaderView(0);
        userNameHeaderView = headerView.findViewById(R.id.user_name);

        // Update header with user info if available
        updateNavigationHeader();

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
    }

    private void updateNavigationHeader() {
        if (authManager.getCurrentUser() != null && userNameHeaderView != null) {
            // Get user info from Firebase and update the header
            authManager.getCurrentUserModel(new FirebaseAuthManager.AuthCallback() {
                @Override
                public void onSuccess(UserModel userModel) {
                    if (userModel != null && userModel.getDisplayName() != null) {
                        userNameHeaderView.setText("Welcome, " + userModel.getDisplayName());
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    // Just log the error but don't block UI
                    e.printStackTrace();
                }
            });
        }
    }

    private void setupBottomNavigation() {
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            String title = "";

            if (item.getItemId() == R.id.nav_dashboard) {
                selectedFragment = new DashboardFragment();
                title = "DASHBOARD";
            } else if (item.getItemId() == R.id.nav_devices) {
                selectedFragment = new HomeListFragment();
                title = "HOMES";
            } else if (item.getItemId() == R.id.nav_activities) {
                selectedFragment = new ActivitiesFragment();
                title = "ACTIVITIES";
            } else if (item.getItemId() == R.id.nav_alerts) {
                selectedFragment = new Esp32CamFragment();
                title = "Esp32CAM";
            }

            if (selectedFragment != null) {
                switchFragment(selectedFragment, title);
                return true;
            }
            return false;
        });
    }

    private void checkUserStatus() {
        authManager.getCurrentUserModel(new FirebaseAuthManager.AuthCallback() {
            @Override
            public void onSuccess(UserModel userModel) {
                if (userModel != null) {
                    if (userModel.isAdmin() || userModel.isApproved()) {
                        // Update session data
                        sessionManager.createLoginSession(
                                userModel.getUserId(),
                                userModel.getEmail(),
                                userModel.getRole(),
                                userModel.getApprovalStatus()
                        );

                        // Update navigation menu based on user role
                        updateMenuForUserRole(userModel);

                        // Update the header if it exists
                        if (userNameHeaderView != null) {
                            userNameHeaderView.setText("Welcome, " + userModel.getDisplayName());
                        }
                    } else {
                        // User is pending approval
                        startActivity(new Intent(MainActivity.this, PendingApprovalActivity.class));
                        finish();
                    }
                }
            }

            @Override
            public void onFailure(Exception e) {
                // Error getting user data
                e.printStackTrace();
                Snackbar.make(
                        findViewById(android.R.id.content),
                        "Error retrieving user data. Please try again.",
                        Snackbar.LENGTH_LONG
                ).show();
            }
        });
    }

    private void updateMenuForUserRole(UserModel userModel) {
        // Update menu visibility based on user role
        if (navigationView != null && navigationView.getMenu() != null) {
            MenuItem adminMenuItem = navigationView.getMenu().findItem(R.id.nav_admin);
            if (adminMenuItem != null) {
                adminMenuItem.setVisible(userModel.isAdmin());
            }
        }

        // Invalidate options menu to refresh
        invalidateOptionsMenu();
    }

    private void switchFragment(Fragment fragment, String title) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
        toolbar.setTitle(title);
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void redirectToServerConfig() {
        startActivity(new Intent(this, ServerConfigActivity.class));
        // Don't finish this activity so we can return to it
    }

    private void logout() {
        // Use both authManager and sessionManager to ensure complete logout
        authManager.logout();
        sessionManager.clearSession();
        redirectToLogin();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);

        // Show/hide admin menu item based on role
        MenuItem adminMenuItem = menu.findItem(R.id.menu_admin);
        if (adminMenuItem != null) {
            adminMenuItem.setVisible(sessionManager.isAdmin());
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_logout || itemId == R.id.menu_logout) {
            logout();
            return true;
        } else if (itemId == R.id.action_server_config) {
            redirectToServerConfig();
            return true;
        } else if (itemId == R.id.menu_admin) {
            startActivity(new Intent(MainActivity.this, AdminDashboardActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_profile) {
            startActivity(new Intent(this, EditProfileActivity.class));
        } else if (id == R.id.nav_auto) {
            // Handle auto on-off
        } else if (id == R.id.nav_settings) {
            // Navigate to settings
        } else if (id == R.id.nav_cp) {
            startActivity(new Intent(this, ChangePasswordActivity.class));
        } else if (id == R.id.nav_faq) {
            // Open FAQ
        } else if (id == R.id.nav_reportbug) {
            // Navigate to report issue
        } else if (id == R.id.nav_pp) {
            // Open privacy policy
        } else if (id == R.id.nav_logout) {
            logout();
        } else if (id == R.id.nav_server_config || id == R.id.action_server_config) {
            redirectToServerConfig();
        } else if (id == R.id.nav_admin) {
            // Admin dashboard
            startActivity(new Intent(this, AdminDashboardActivity.class));
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onNetworkChanged(boolean isConnected) {
        if (isConnected) {
            // When network comes back, verify server configuration
            if (!ServerCheckUtility.isServerConfigured(this)) {
                Snackbar.make(
                        findViewById(android.R.id.content),
                        "Server configuration not found. Please configure it.",
                        Snackbar.LENGTH_LONG
                ).setAction("Configure", v -> redirectToServerConfig()).show();
            }
        } else {
            // Notify user about network disconnection
            Snackbar.make(
                    findViewById(android.R.id.content),
                    "Network connection lost. Some features may not work.",
                    Snackbar.LENGTH_LONG
            ).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (networkChangeReceiver != null) {
            networkChangeReceiver.unregister();
        }
    }
}