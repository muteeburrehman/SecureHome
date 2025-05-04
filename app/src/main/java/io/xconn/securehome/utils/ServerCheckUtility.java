package io.xconn.securehome.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import io.xconn.securehome.activities.ServerConfigActivity;
import io.xconn.securehome.network.ApiConfig;

/**
 * Utility class for checking server configuration status
 * and handling redirection to the server configuration screen when needed
 */
public class ServerCheckUtility {
    private static final String TAG = "ServerCheckUtility";

    /**
     * Checks if the server is configured. If not, redirects to the ServerConfigActivity.
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
            Log.d(TAG, "Server not configured, redirecting to config screen");
            redirectToServerConfig(activity);
            return false;
        }

        return true;
    }

    /**
     * Redirects to the server configuration screen
     *
     * @param activity The calling activity
     */
    public static void redirectToServerConfig(Activity activity) {
        if (activity == null) {
            return;
        }

        Intent intent = new Intent(activity, ServerConfigActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        activity.startActivity(intent);
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