package io.xconn.securehome;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;

import io.xconn.securehome.activities.AddHomeFragment;
import io.xconn.securehome.activities.LoginActivity;
import io.xconn.securehome.api.FirebaseAuthManager;
import io.xconn.securehome.maincontroller.ActivitiesFragment;
import io.xconn.securehome.maincontroller.DashboardFragment;
import io.xconn.securehome.maincontroller.Esp32CamFragment;
import io.xconn.securehome.utils.SessionManager;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private SessionManager sessionManager;
    private FirebaseAuthManager authManager;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private BottomNavigationView bottomNavigationView;
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize managers
        sessionManager = new SessionManager(this);
        authManager = FirebaseAuthManager.getInstance();

        // Check login state using Firebase
        if (!authManager.isUserLoggedIn()) {
            redirectToLogin();
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

    private void setupToolbar() {
        toolbar = findViewById(R.id.toolbarNav);
        setSupportActionBar(toolbar);
    }

    private void setupNavigationDrawer() {
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Update navigation header with user info if needed
        if (authManager.getCurrentUser() != null) {
            // If you have a header view with user info fields, you can update them here
            // View headerView = navigationView.getHeaderView(0);
            // TextView userNameView = headerView.findViewById(R.id.user_name);
            // userNameView.setText(authManager.getCurrentUser().getDisplayName());
        }

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
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
                selectedFragment = new AddHomeFragment();
                title = "HOME";
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

    private void switchFragment(Fragment fragment, String title) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
        toolbar.setTitle(title);
    }

    private void redirectToLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    private void logout() {
        // Use SessionManager logout method that also signs out from Firebase
        sessionManager.logout();
        redirectToLogin();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            logout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_profile) {
            // TODO: Navigate to profile
        } else if (id == R.id.nav_auto) {
            // TODO: Handle auto on-off
        } else if (id == R.id.nav_settings) {
            // TODO: Navigate to settings
        } else if (id == R.id.nav_cp) {
            // TODO: Navigate to change password - could use Firebase password reset
        } else if (id == R.id.nav_faq) {
            // TODO: Open FAQ
        } else if (id == R.id.nav_reportbug) {
            // TODO: Navigate to report issue
        } else if (id == R.id.nav_pp) {
            // TODO: Open privacy policy
        } else if (id == R.id.nav_logout) {
            logout();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }
}