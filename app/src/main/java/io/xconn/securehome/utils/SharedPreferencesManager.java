package io.xconn.securehome.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Utility class for managing shared preferences
 */
public class SharedPreferencesManager {
    private static final String PREF_NAME = "io.xconn.securehome.PREFERENCES";
    private static SharedPreferencesManager instance;
    private final SharedPreferences preferences;

    private SharedPreferencesManager(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Get the singleton instance
     * @param context Application context
     * @return SharedPreferencesManager instance
     */
    public static synchronized SharedPreferencesManager getInstance(Context context) {
        if (instance == null) {
            instance = new SharedPreferencesManager(context);
        }
        return instance;
    }

    /**
     * Save a string value to preferences
     * @param key Preference key
     * @param value Value to save
     */
    public void saveString(String key, String value) {
        preferences.edit().putString(key, value).apply();
    }

    /**
     * Get a string value from preferences
     * @param key Preference key
     * @param defaultValue Default value if key doesn't exist
     * @return The saved string or defaultValue
     */
    public String getString(String key, String defaultValue) {
        return preferences.getString(key, defaultValue);
    }

    /**
     * Save a boolean value to preferences
     * @param key Preference key
     * @param value Value to save
     */
    public void saveBoolean(String key, boolean value) {
        preferences.edit().putBoolean(key, value).apply();
    }

    /**
     * Get a boolean value from preferences
     * @param key Preference key
     * @param defaultValue Default value if key doesn't exist
     * @return The saved boolean or defaultValue
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        return preferences.getBoolean(key, defaultValue);
    }

    /**
     * Save an integer value to preferences
     * @param key Preference key
     * @param value Value to save
     */
    public void saveInt(String key, int value) {
        preferences.edit().putInt(key, value).apply();
    }

    /**
     * Get an integer value from preferences
     * @param key Preference key
     * @param defaultValue Default value if key doesn't exist
     * @return The saved integer or defaultValue
     */
    public int getInt(String key, int defaultValue) {
        return preferences.getInt(key, defaultValue);
    }

    /**
     * Save a long value to preferences
     * @param key Preference key
     * @param value Value to save
     */
    public void saveLong(String key, long value) {
        preferences.edit().putLong(key, value).apply();
    }

    /**
     * Get a long value from preferences
     * @param key Preference key
     * @param defaultValue Default value if key doesn't exist
     * @return The saved long or defaultValue
     */
    public long getLong(String key, long defaultValue) {
        return preferences.getLong(key, defaultValue);
    }

    /**
     * Remove a value from preferences
     * @param key Preference key to remove
     */
    public void remove(String key) {
        preferences.edit().remove(key).apply();
    }

    /**
     * Clear all preferences
     */
    public void clearAll() {
        preferences.edit().clear().apply();
    }
}
