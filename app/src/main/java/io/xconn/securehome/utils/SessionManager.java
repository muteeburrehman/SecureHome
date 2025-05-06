package io.xconn.securehome.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;
    private Context context;

    private static final String PREF_NAME = "SecureHomeSession";
    private static final String IS_LOGGED_IN = "IsLoggedIn";
    private static final String USER_EMAIL = "UserEmail";
    private static final String USER_ROLE = "UserRole";
    private static final String USER_ID = "UserId";
    private static final String APPROVAL_STATUS = "ApprovalStatus";

    public SessionManager(Context context) {
        this.context = context;
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    public void createLoginSession(String userId, String email, String role, String approvalStatus) {
        editor.putBoolean(IS_LOGGED_IN, true);
        editor.putString(USER_ID, userId);
        editor.putString(USER_EMAIL, email);
        editor.putString(USER_ROLE, role);
        editor.putString(APPROVAL_STATUS, approvalStatus);
        editor.commit();
    }

    public void clearSession() {
        editor.clear();
        editor.commit();
    }

    public boolean isLoggedIn() {
        return prefs.getBoolean(IS_LOGGED_IN, false);
    }

    public void setLoggedIn(boolean isLoggedIn) {
        editor.putBoolean(IS_LOGGED_IN, isLoggedIn);
        editor.commit();
    }

    public String getUserEmail() {
        return prefs.getString(USER_EMAIL, "");
    }

    public void saveUserEmail(String email) {
        editor.putString(USER_EMAIL, email);
        editor.commit();
    }

    public String getUserId() {
        return prefs.getString(USER_ID, "");
    }

    public void saveUserId(String userId) {
        editor.putString(USER_ID, userId);
        editor.commit();
    }

    public String getUserRole() {
        return prefs.getString(USER_ROLE, "");
    }

    public void saveUserRole(String role) {
        editor.putString(USER_ROLE, role);
        editor.commit();
    }

    public boolean isAdmin() {
        return "admin".equals(getUserRole());
    }

    public String getApprovalStatus() {
        return prefs.getString(APPROVAL_STATUS, "");
    }

    public void saveApprovalStatus(String status) {
        editor.putString(APPROVAL_STATUS, status);
        editor.commit();
    }

    public boolean isApproved() {
        return "approved".equals(getApprovalStatus());
    }
}