package io.xconn.securehome.services;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;
import java.util.UUID;

import io.xconn.securehome.R;
import io.xconn.securehome.MainActivity;
import io.xconn.securehome.activities.AdminDashboardActivity;
import io.xconn.securehome.utils.SharedPreferencesManager;

/**
 * Comprehensive Firebase Cloud Messaging (FCM) service for SecureHome application.
 * Handles token management, notification display, and custom routing.
 */
public class MyFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = "MyFirebaseMessagingService";
    private static final String CHANNEL_ID = "securehome_notifications";
    private static final String CHANNEL_NAME = "SecureHome Notifications";
    private static final String CHANNEL_DESC = "Notifications from the SecureHome system";

    /**
     * Called when a new FCM token is generated.
     *
     * @param token The new token
     */
    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "New FCM Token generated: " + token);

        // Save the token to SharedPreferences
        SharedPreferencesManager.getInstance(this).saveString("fcm_token", token);

        // Update token in the backend
        updateTokenOnServer(token);
    }

    /**
     * Called when a message is received.
     *
     * @param remoteMessage The received RemoteMessage
     */
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        // Log message details for debugging
        logMessageDetails(remoteMessage);

        // Handle the incoming message
        handleIncomingMessage(remoteMessage);
    }

    /**
     * Log detailed information about the received message
     *
     * @param remoteMessage The received RemoteMessage
     */
    private void logMessageDetails(RemoteMessage remoteMessage) {
        Log.d(TAG, "Message received from: " + (remoteMessage.getFrom() != null ? remoteMessage.getFrom() : "Unknown"));

        // Log notification payload
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Notification Title: " + remoteMessage.getNotification().getTitle());
            Log.d(TAG, "Notification Body: " + remoteMessage.getNotification().getBody());
        }

        // Log data payload
        if (!remoteMessage.getData().isEmpty()) {
            Log.d(TAG, "Data Payload: " + remoteMessage.getData());
        }
    }

    /**
     * Handle incoming message based on its type and payload
     *
     * @param remoteMessage The received RemoteMessage
     */
    private void handleIncomingMessage(RemoteMessage remoteMessage) {
        // Determine notification details
        String title = remoteMessage.getNotification() != null
                ? remoteMessage.getNotification().getTitle()
                : "SecureHome Notification";

        String body = remoteMessage.getNotification() != null
                ? remoteMessage.getNotification().getBody()
                : "You have a new notification";

        // Get data payload
        Map<String, String> data = remoteMessage.getData();

        // Determine target activity and intent
        Intent intent = determineTargetIntent(data);

        // Create notification
        createAndShowNotification(title, body, intent, data);
    }

    /**
     * Determine the target activity based on notification type
     *
     * @param data Notification data payload
     * @return Intent for the target activity
     */
    private Intent determineTargetIntent(Map<String, String> data) {
        Intent intent;

        if (data != null && data.containsKey("type")) {
            String type = data.get("type");

            switch (type) {
                case "registration_request":
                    intent = new Intent(this, AdminDashboardActivity.class);
                    intent.putExtra("openTab", "pendingUsers");

                    if (data.containsKey("userId")) {
                        intent.putExtra("highlightUserId", data.get("userId"));
                    }
                    break;

                case "security_alert":
                    intent = new Intent(this, MainActivity.class);
                    intent.putExtra("openFragment", "securityAlerts");
                    break;

                default:
                    intent = new Intent(this, MainActivity.class);
            }
        } else {
            intent = new Intent(this, MainActivity.class);
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    /**
     * Create and show the notification
     *
     * @param title Notification title
     * @param body Notification body
     * @param intent Target intent
     * @param data Notification data payload
     */
    private void createAndShowNotification(String title, String body, Intent intent, Map<String, String> data) {
        // Create notification channel
        createNotificationChannel();

        // Create pending intent
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                generateUniqueRequestCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Build notification
        NotificationCompat.Builder builder = buildNotification(title, body, pendingIntent, data);

        // Show notification
        showNotification(builder);
    }

    /**
     * Create notification channel for Android Oreo and above
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription(CHANNEL_DESC);

            NotificationManager notificationManager =
                    getSystemService(NotificationManager.class);

            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    /**
     * Build the notification
     *
     * @param title Notification title
     * @param body Notification body
     * @param pendingIntent Pending intent for notification tap
     * @param data Notification data payload
     * @return NotificationCompat.Builder
     */
    private NotificationCompat.Builder buildNotification(
            String title,
            String body,
            PendingIntent pendingIntent,
            Map<String, String> data
    ) {
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);

        // Add priority based on notification type
        if (data != null && "security_alert".equals(data.get("type"))) {
            builder.setPriority(NotificationCompat.PRIORITY_HIGH);
        } else {
            builder.setPriority(NotificationCompat.PRIORITY_DEFAULT);
        }

        return builder;
    }

    /**
     * Show the notification
     *
     * @param builder Notification builder
     */
    private void showNotification(NotificationCompat.Builder builder) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        notificationManager.notify(generateUniqueNotificationId(), builder.build());
    }

    /**
     * Update token on the server
     *
     * @param token FCM token
     */
    private void updateTokenOnServer(String token) {
        // Get user ID from SharedPreferences
        String userId = SharedPreferencesManager.getInstance(this).getString("user_id", null);

        if (userId != null) {
            // Update token via NotificationService
            NotificationService.updateToken(userId, token);
        }
    }

    /**
     * Generate a unique request code for PendingIntent
     *
     * @return Unique integer
     */
    private int generateUniqueRequestCode() {
        return (int) System.currentTimeMillis();
    }

    /**
     * Generate a unique notification ID
     *
     * @return Unique integer
     */
    private int generateUniqueNotificationId() {
        return (int) System.currentTimeMillis();
    }

    /**
     * Retrieve the current FCM token
     *
     * @param context Application context
     * @return Current FCM token or null
     */
    public static String getCurrentToken(Context context) {
        return SharedPreferencesManager.getInstance(context).getString("fcm_token", null);
    }
}