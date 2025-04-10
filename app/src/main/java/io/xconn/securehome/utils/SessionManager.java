package io.xconn.securehome.utils;

import android.content.Context;
import android.content.SharedPreferences;
import io.xconn.securehome.api.FirebaseAuthManager;

public class SessionManager {
    private static final String PREF_NAME = "SecureHomePrefs";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_USER_EMAIL = "userEmail";

    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;
    private Context context;

    public SessionManager(Context context) {
        this.context = context;
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    public void setLoggedIn(boolean isLoggedIn) {
        editor.putBoolean(KEY_IS_LOGGED_IN, isLoggedIn);
        editor.apply();
    }

    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public void saveUserEmail(String email) {
        editor.putString(KEY_USER_EMAIL, email);
        editor.apply();
    }

    public String getUserEmail() {
        return prefs.getString(KEY_USER_EMAIL, "");
    }

    /**
     * Legacy method for compatibility with existing code
     */
    public void clearSession() {
        logout();
    }

    public void logout() {
        editor.clear();
        editor.apply();

        // Also sign out from Firebase
        FirebaseAuthManager.getInstance().logout();
    }
}