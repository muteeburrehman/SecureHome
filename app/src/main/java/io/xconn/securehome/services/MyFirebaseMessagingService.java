package io.xconn.securehome.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

import io.xconn.securehome.R;
import io.xconn.securehome.MainActivity;
import io.xconn.securehome.activities.AdminDashboardActivity;
import io.xconn.securehome.utils.SharedPreferencesManager;

/**
 * Service for handling Firebase Cloud Messaging (FCM) messages
 */
public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";
    private static final String CHANNEL_ID = "securehome_notifications";
    private static final String CHANNEL_NAME = "SecureHome Notifications";
    private static final String CHANNEL_DESC = "Notifications from the SecureHome system";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        // Check if message contains a notification payload
        if (remoteMessage.getNotification() != null) {
            String title = remoteMessage.getNotification().getTitle();
            String body = remoteMessage.getNotification().getBody();
            Log.d(TAG, "Notification Title: " + title);
            Log.d(TAG, "Notification Body: " + body);

            // Handle data payload
            Map<String, String> data = remoteMessage.getData();
            handleNotification(title, body, data);
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        Log.d(TAG, "Refreshed token: " + token);

        // Save new token to preferences
        SharedPreferencesManager.getInstance(this).saveString("fcm_token", token);

        // Update token in database
        String userId = SharedPreferencesManager.getInstance(this).getString("user_id", null);
        if (userId != null) {
            NotificationService.updateToken(userId, token);
        }
    }

    /**
     * Handle incoming notification
     * @param title Notification title
     * @param body Notification body
     * @param data Additional data payload
     */
    private void handleNotification(String title, String body, Map<String, String> data) {
        Intent intent;

        // Choose the target activity based on notification type
        if (data != null && data.containsKey("type")) {
            String type = data.get("type");

            if ("registration_request".equals(type)) {
                // Direct admin to the admin dashboard for registration requests
                intent = new Intent(this, AdminDashboardActivity.class);
                intent.putExtra("openTab", "pendingUsers"); // Tell the activity to open the pending users tab

                if (data.containsKey("userId")) {
                    intent.putExtra("highlightUserId", data.get("userId"));
                }
            } else {
                // Default to main activity
                intent = new Intent(this, MainActivity.class);
            }
        } else {
            // Default to main activity
            intent = new Intent(this, MainActivity.class);
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // Create pending intent
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        // Build notification
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_notification) // Make sure you have this icon
                        .setContentTitle(title)
                        .setContentText(body)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // For Android Oreo and above, create notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(CHANNEL_DESC);
            notificationManager.createNotificationChannel(channel);
        }

        // Generate unique notification ID
        int notificationId = (int) System.currentTimeMillis();

        // Show notification
        notificationManager.notify(notificationId, notificationBuilder.build());
    }
}