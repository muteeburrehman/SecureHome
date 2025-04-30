package io.xconn.securehome.network;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class ApiConfig {
    private static final String TAG = "ApiConfig";
    private static final String PREF_NAME = "server_config";
    private static final String KEY_SERVER_IP = "server_ip";
    private static final String KEY_SERVER_PORT = "server_port";
    private static final String DEFAULT_PORT = "8000";

    // Get the base URL if available
    public static String getBaseUrl(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String ip = prefs.getString(KEY_SERVER_IP, null);

        // Handle port that might be stored as either String or Integer
        String port;
        try {
            port = prefs.getString(KEY_SERVER_PORT, DEFAULT_PORT);
        } catch (ClassCastException e) {
            // If stored as Integer, get as Integer and convert to String
            port = String.valueOf(prefs.getInt(KEY_SERVER_PORT, Integer.parseInt(DEFAULT_PORT)));
        }

        if (ip == null || ip.isEmpty() || port == null || port.isEmpty()) {
            return null; // Missing IP or Port
        }

        return "http://" + ip + ":" + port + "/";
    }

    // Save the server information
    public static void saveServerInfo(Context context, String ip, int port) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_SERVER_IP, ip);
        // Store port consistently as a String to avoid type issues
        editor.putString(KEY_SERVER_PORT, String.valueOf(port));
        editor.apply();

        Log.d(TAG, "Updated server config: IP=" + ip + ", PORT=" + port);
    }

    // Check if we have valid server configuration
    public static boolean hasServerConfig(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String ip = prefs.getString(KEY_SERVER_IP, null);

        // Handle port that might be stored as either String or Integer
        String port;
        try {
            port = prefs.getString(KEY_SERVER_PORT, null);
        } catch (ClassCastException e) {
            // If stored as Integer, get as Integer and convert to String, or null if not found
            try {
                port = String.valueOf(prefs.getInt(KEY_SERVER_PORT, -1));
                if (port.equals("-1")) {
                    port = null;
                }
            } catch (Exception e2) {
                Log.e(TAG, "Error accessing port value", e2);
                port = null;
            }
        }

        return ip != null && !ip.isEmpty() &&
                port != null && !port.isEmpty();
    }

    // Clear server configuration
    public static void clearServerConfig(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(KEY_SERVER_IP);
        editor.remove(KEY_SERVER_PORT);
        editor.apply();

        Log.d(TAG, "Cleared server configuration");
    }
}