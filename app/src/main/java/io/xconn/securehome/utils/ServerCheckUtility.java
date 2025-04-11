package io.xconn.securehome.utils;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import io.xconn.securehome.activities.ServerDiscoveryActivity;
import io.xconn.securehome.network.ApiConfig;

/**
 * Utility class to handle server IP configuration checks
 * and redirect to ServerDiscoveryActivity when needed
 */
public class ServerCheckUtility {

    /**
     * Checks if server configuration exists, if not redirects to ServerDiscoveryActivity
     * @param activity The activity context
     * @return true if server is configured, false otherwise
     */
    public static boolean checkServerConfigured(AppCompatActivity activity) {
        if (!ApiConfig.hasServerConfig(activity)) {
            Toast.makeText(activity, "Server not configured. Please discover and select a server first.",
                    Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(activity, ServerDiscoveryActivity.class);
            activity.startActivity(intent);
            return false;
        }
        return true;
    }

    /**
     * Checks if server configuration exists for fragments, if not redirects to ServerDiscoveryActivity
     * @param fragment The fragment requesting the check
     * @return true if server is configured, false otherwise
     */
    public static boolean checkServerConfigured(Fragment fragment) {
        Context context = fragment.getContext();
        if (context == null || fragment.getActivity() == null) {
            return false;
        }

        if (!ApiConfig.hasServerConfig(context)) {
            Toast.makeText(context, "Server not configured. Please discover and select a server first.",
                    Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(context, ServerDiscoveryActivity.class);
            fragment.startActivity(intent);
            return false;
        }
        return true;
    }
}