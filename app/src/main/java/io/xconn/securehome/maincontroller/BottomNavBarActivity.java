package io.xconn.securehome.maincontroller;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;

import io.xconn.securehome.R;
import io.xconn.securehome.activities.LoginActivity;
import io.xconn.securehome.utils.SessionManager;

public class BottomNavBarActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private BottomNavigationView bottomNavigationView;
    private SessionManager sessionManager;
    private Toolbar toolbar;

    // Fragments
    private DashboardFragment dashboardFragment;
    private DevicesFragment devicesFragment;
    private ActivitiesFragment activitiesFragment;
    private AlertsFragment alertsFragment;
    private Fragment activeFragment;
    private FragmentManager fragmentManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bottom_nav_bar);

        // Initialize SessionManager
        sessionManager = new SessionManager(this);

        // Setup Toolbar
        toolbar = findViewById(R.id.toolbarNav);
        setSupportActionBar(toolbar);

        // Setup DrawerLayout
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Initialize ActionBarDrawerToggle for hamburger icon
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Setup Bottom Navigation
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        fragmentManager = getSupportFragmentManager();

        // Initialize Fragments
        dashboardFragment = new DashboardFragment();
        devicesFragment = new DevicesFragment();
        activitiesFragment = new ActivitiesFragment();
        alertsFragment = new AlertsFragment();

        // Restore Fragment on Configuration Change
        if (savedInstanceState == null) {
            activeFragment = dashboardFragment;
            fragmentManager.beginTransaction()
                    .add(R.id.fragment_container, dashboardFragment, "DASHBOARD")
                    .add(R.id.fragment_container, devicesFragment, "DEVICES").hide(devicesFragment)
                    .add(R.id.fragment_container, activitiesFragment, "ACTIVITIES").hide(activitiesFragment)
                    .add(R.id.fragment_container, alertsFragment, "ALERTS").hide(alertsFragment)
                    .commit();
        } else {
            // Restore active fragment
            activeFragment = fragmentManager.findFragmentByTag(savedInstanceState.getString("ACTIVE_FRAGMENT"));
        }

        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            String title = "Secure Home";

            if (item.getItemId() == R.id.nav_dashboard) {
                selectedFragment = dashboardFragment;
                title = "DASHBOARD";
            } else if (item.getItemId() == R.id.nav_devices) {
                selectedFragment = devicesFragment;
                title = "DEVICES";
            } else if (item.getItemId() == R.id.nav_activities) {
                selectedFragment = activitiesFragment;
                title = "ACTIVITIES";
            } else if (item.getItemId() == R.id.nav_alerts) {
                selectedFragment = alertsFragment;
                title = "ALERTS";
            }

            if (selectedFragment != null && selectedFragment != activeFragment) {
                fragmentManager.beginTransaction()
                        .hide(activeFragment)
                        .show(selectedFragment)
                        .commit();
                activeFragment = selectedFragment;
                toolbar.setTitle(title);
            }

            return true;
        });

        bottomNavigationView.setSelectedItemId(R.id.nav_dashboard);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("ACTIVE_FRAGMENT", activeFragment.getTag());
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else if (bottomNavigationView.getSelectedItemId() != R.id.nav_dashboard) {
            bottomNavigationView.setSelectedItemId(R.id.nav_dashboard);
        } else {
            super.onBackPressed();
        }
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

    private void logout() {
        sessionManager.clearSession();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
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
            // TODO: Navigate to change password
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