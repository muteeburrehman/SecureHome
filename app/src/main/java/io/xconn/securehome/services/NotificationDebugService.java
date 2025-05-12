package io.xconn.securehome.services;

import android.util.Log;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import java.util.Map;

/**
 * A comprehensive debugging service for Firebase Cloud Messaging (FCM) notifications.
 * This service provides enhanced logging for notification and token-related events.
 */
public class NotificationDebugService extends FirebaseMessagingService {
    private static final String DEBUG_TAG = "NotificationDebug";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // Enhanced logging for comprehensive debugging
        Log.d(DEBUG_TAG, "============= NOTIFICATION RECEIVED =============");

        // Log message origin
        Log.d(DEBUG_TAG, "Message Origin: " + (remoteMessage.getFrom() != null ? remoteMessage.getFrom() : "Unknown"));

        // Notification Payload Details
        if (remoteMessage.getNotification() != null) {
            Log.d(DEBUG_TAG, "Notification Details:");
            Log.d(DEBUG_TAG, "Title: " + remoteMessage.getNotification().getTitle());
            Log.d(DEBUG_TAG, "Body: " + remoteMessage.getNotification().getBody());

            // Additional notification details
            Log.d(DEBUG_TAG, "Channel ID: " + remoteMessage.getNotification().getChannelId());
            Log.d(DEBUG_TAG, "Click Action: " + remoteMessage.getNotification().getClickAction());
        } else {
            Log.d(DEBUG_TAG, "No standard notification payload");
        }

        // Data Payload Details
        Map<String, String> dataPayload = remoteMessage.getData();
        if (dataPayload != null && !dataPayload.isEmpty()) {
            Log.d(DEBUG_TAG, "Data Payload Details:");
            for (Map.Entry<String, String> entry : dataPayload.entrySet()) {
                Log.d(DEBUG_TAG, entry.getKey() + ": " + entry.getValue());
            }
        } else {
            Log.d(DEBUG_TAG, "No data payload");
        }

        // Log additional context
        Log.d(DEBUG_TAG, "Notification Priority: " + remoteMessage.getPriority());
        Log.d(DEBUG_TAG, "Sent Time: " + remoteMessage.getSentTime());

        // Conditional handling - decide whether to pass to the parent implementation
        try {
            super.onMessageReceived(remoteMessage);
        } catch (Exception e) {
            Log.e(DEBUG_TAG, "Error in parent onMessageReceived: " + e.getMessage(), e);
        }

        Log.d(DEBUG_TAG, "============= END NOTIFICATION DEBUG =============");
    }

    @Override
    public void onNewToken(String token) {
        // Enhanced token logging
        Log.d(DEBUG_TAG, "============= NEW FCM TOKEN =============");
        Log.d(DEBUG_TAG, "New Token: " + token);
        Log.d(DEBUG_TAG, "Token Length: " + token.length());

        // Additional token analysis
        Log.d(DEBUG_TAG, "Token Hash: " + token.hashCode());
        Log.d(DEBUG_TAG, "Contains Alphanumeric: " + containsAlphanumeric(token));

        // Call parent implementation
        try {
            super.onNewToken(token);
        } catch (Exception e) {
            Log.e(DEBUG_TAG, "Error in parent onNewToken: " + e.getMessage(), e);
        }

        Log.d(DEBUG_TAG, "============= TOKEN UPDATE COMPLETE =============");
    }

    /**
     * Helper method to check if token contains alphanumeric characters
     * @param token FCM token to analyze
     * @return true if token contains alphanumeric characters, false otherwise
     */
    private boolean containsAlphanumeric(String token) {
        return token != null && token.matches(".*[a-zA-Z0-9].*");
    }

    /**
     * Optional method to log detailed token information
     * This can be called manually or integrated into your token refresh strategy
     *
     * @param token FCM token to log
     */
    public void logTokenDetails(String token) {
        if (token == null) {
            Log.w(DEBUG_TAG, "Null token provided for logging");
            return;
        }

        Log.d(DEBUG_TAG, "============= TOKEN DETAILS =============");
        Log.d(DEBUG_TAG, "Full Token: " + token);
        Log.d(DEBUG_TAG, "Token Length: " + token.length());
        Log.d(DEBUG_TAG, "Starts With: " + token.substring(0, Math.min(token.length(), 10)) + "...");
        Log.d(DEBUG_TAG, "Contains Unique Chars: " + (token.chars().distinct().count()));
        Log.d(DEBUG_TAG, "============= END TOKEN DETAILS =============");
    }
}