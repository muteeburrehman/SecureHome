package io.xconn.securehome.utils;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class NotificationPermissionManager {
    private static final String PERMISSION = Manifest.permission.POST_NOTIFICATIONS;
    private AppCompatActivity activity;
    private OnNotificationPermissionResultCallback callback;
    private ActivityResultLauncher<String> requestPermissionLauncher;

    // Interface for permission result callback
    public interface OnNotificationPermissionResultCallback {
        void onPermissionGranted();
        void onPermissionDenied();
    }

    public NotificationPermissionManager(AppCompatActivity activity) {
        this.activity = activity;
        // Move launcher registration to a separate method
        initPermissionLauncher();
    }

    // Initialize permission launcher separately
    private void initPermissionLauncher() {
        if (activity != null) {
            requestPermissionLauncher = activity.registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    isGranted -> {
                        if (isGranted) {
                            // Permission granted
                            if (callback != null) {
                                callback.onPermissionGranted();
                            }
                        } else {
                            // Permission denied
                            if (callback != null) {
                                callback.onPermissionDenied();
                            }
                        }
                        // Clear the callback after use
                        callback = null;
                    }
            );
        }
    }

    /**
     * Check and request notification permission if needed
     * @param callback Callback to handle permission result
     */
    public void checkAndRequestNotificationPermission(OnNotificationPermissionResultCallback callback) {
        // Ensure launcher is initialized
        if (requestPermissionLauncher == null) {
            initPermissionLauncher();
        }

        // Only request for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            this.callback = callback;

            // Check if permission is already granted
            if (ContextCompat.checkSelfPermission(
                    activity,
                    PERMISSION
            ) == PackageManager.PERMISSION_GRANTED) {
                // Permission already granted
                if (callback != null) {
                    callback.onPermissionGranted();
                }
            } else {
                // Request permission
                requestPermissionLauncher.launch(PERMISSION);
            }
        } else {
            // For older Android versions, consider permission granted
            if (callback != null) {
                callback.onPermissionGranted();
            }
        }
    }

    /**
     * Check if notification permission is granted
     * @return true if permission is granted or device is below Android 13
     */
    public boolean isNotificationPermissionGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(
                    activity,
                    PERMISSION
            ) == PackageManager.PERMISSION_GRANTED;
        }
        return true; // Always true for devices below Android 13
    }
}