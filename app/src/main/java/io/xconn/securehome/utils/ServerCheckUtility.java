package io.xconn.securehome.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import io.xconn.securehome.activities.ServerDiscoveryActivity;
import io.xconn.securehome.network.ApiConfig;

/**
 * Utility class for checking server configuration status
 * and handling redirection to the server discovery screen when needed
 */
public class ServerCheckUtility {
    private static final String TAG = "ServerCheckUtility";

    /**
     * Checks if the server is configured. If not, redirects to the ServerDiscoveryActivity.
     *
     * @param activity The calling activity
     * @return true if server is configured, false otherwise
     */
    public static boolean checkServerConfigured(Activity activity) {
        if (activity == null) {
            return false;
        }

        boolean isConfigured = false;

        try {
            isConfigured = ApiConfig.hasServerConfig(activity);
        } catch (Exception e) {
            Log.e(TAG, "Error checking server configuration", e);
            // In case of error, assume server is not configured
            isConfigured = false;
        }

        if (!isConfigured) {
            Log.d(TAG, "Server not configured, redirecting to discovery");
            Intent intent = new Intent(activity, ServerDiscoveryActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            activity.startActivity(intent);
            return false;
        }

        return true;
    }

    /**
     * Silent check for server configuration without redirecting
     *
     * @param context Any context (doesn't have to be an Activity)
     * @return true if server is configured, false otherwise
     */
    public static boolean isServerConfigured(Context context) {
        if (context == null) {
            return false;
        }

        try {
            return ApiConfig.hasServerConfig(context);
        } catch (Exception e) {
            Log.e(TAG, "Error checking server configuration", e);
            return false;
        }
    }
}