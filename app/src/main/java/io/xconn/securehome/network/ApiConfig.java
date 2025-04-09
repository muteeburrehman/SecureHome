package io.xconn.securehome.network;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class ApiConfig {
    private static final String TAG = "ApiConfig";
    private static final String PREF_NAME = "server_config";
    private static final String KEY_SERVER_IP = "server_ip";
    private static final String KEY_SERVER_PORT = "server_port";
    private static final int DEFAULT_PORT = 8000;

    // Get the base URL if available
    public static String getBaseUrl(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String ip = prefs.getString(KEY_SERVER_IP, null);
        int port = prefs.getInt(KEY_SERVER_PORT, DEFAULT_PORT);

        if (ip == null) {
            return null; // No IP has been saved yet
        }

        return "http://" + ip + ":" + port + "/";
    }

    // Save the server information
    public static void saveServerInfo(Context context, String ip, int port) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_SERVER_IP, ip);
        editor.putInt(KEY_SERVER_PORT, port);
        editor.apply();

        Log.d(TAG, "Updated server config: IP=" + ip + ", PORT=" + port);
    }

    // Check if we have server configuration
    public static boolean hasServerConfig(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.contains(KEY_SERVER_IP);
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