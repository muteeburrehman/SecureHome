package io.xconn.securehome.services;

import android.content.Context;
import android.util.Log;

import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.FirebaseFunctionsException;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import io.xconn.securehome.models.UserModel;
import io.xconn.securehome.utils.SharedPreferencesManager;

/**
 * Service responsible for handling push notifications using Firebase Cloud Messaging
 */
public class NotificationService {
    private static final String TAG = "NotificationService";
    private static final String PREF_FCM_TOKEN = "fcm_token";
    private static final Executor executor = Executors.newSingleThreadExecutor();

    /**
     * Initialize the notification service by requesting and storing FCM token
     * @param context Application context
     */
    public static void initialize(Context context) {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                        return;
                    }

                    // Get new FCM registration token
                    String token = task.getResult();

                    // Save token to shared preferences
                    SharedPreferencesManager.getInstance(context).saveString(PREF_FCM_TOKEN, token);

                    // Update token in Firestore
                    updateUserToken(context, token);

                    Log.d(TAG, "FCM Token: " + token);
                });
    }

    /**
     * Update the user's FCM token in Firestore
     */
    private static void updateUserToken(Context context, String token) {
        String userId = SharedPreferencesManager.getInstance(context).getString("user_id", null);
        if (userId == null) return;

        updateToken(userId, token);
    }

    /**
     * Update token for a specific user ID
     * @param userId User ID to update
     * @param token New FCM token
     */
    public static void updateToken(String userId, String token) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> updates = new HashMap<>();
        updates.put("token", token);

        db.collection("users")
                .document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "User FCM token updated successfully"))
                .addOnFailureListener(e -> Log.e(TAG, "Error updating user FCM token", e));
    }

    /**
     * Send registration request notification to admin users via Cloud Functions
     * @param userName Name of the user requesting registration
     * @param email Email of the user requesting registration
     * @param userId ID of the user requesting registration
     */
    public static void sendRegistrationRequestToAdmin(String userName, String email, String userId) {
        executor.execute(() -> {
            // Create data for the Cloud Function
            Map<String, Object> data = new HashMap<>();
            data.put("role", UserModel.ROLE_ADMIN);
            data.put("title", "New Registration Request");
            data.put("body", userName + " (" + email + ") has requested to join the system");

            Map<String, String> additionalData = new HashMap<>();
            additionalData.put("type", "registration_request");
            additionalData.put("userId", userId);
            data.put("data", additionalData);

            // Call the Cloud Function
            FirebaseFunctions.getInstance()
                    .getHttpsCallable("sendNotificationByRole")
                    .call(data)
                    .addOnSuccessListener(result -> {
                        Log.d(TAG, "Registration notification sent to admins: " + result.getData());
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to send registration notification", e);
                        if (e instanceof FirebaseFunctionsException) {
                            FirebaseFunctionsException ffe = (FirebaseFunctionsException) e;
                            Log.e(TAG, "Function error code: " + ffe.getCode());
                            Log.e(TAG, "Function error details: " + ffe.getDetails());
                        }
                    });
        });
    }

    /**
     * Send approval notification to a user via Cloud Functions
     * @param userId ID of the approved user
     * @param userName Name of the approved user
     */
    public static void sendApprovalNotificationToUser(String userId, String userName) {
        executor.execute(() -> {
            // Create data for the Cloud Function
            Map<String, Object> data = new HashMap<>();
            data.put("userId", userId);
            data.put("title", "Account Approved");
            data.put("body", "Your account has been approved. You can now use all features of the system.");

            // Call the Cloud Function
            FirebaseFunctions.getInstance()
                    .getHttpsCallable("sendNotification")
                    .call(data)
                    .addOnSuccessListener(result -> {
                        Log.d(TAG, "Approval notification sent to user: " + result.getData());
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to send approval notification", e);
                    });
        });
    }

    /**
     * Send rejection notification to a user via Cloud Functions
     * @param userId ID of the rejected user
     * @param userName Name of the rejected user
     * @param reason Optional reason for rejection
     */
    public static void sendRejectionNotificationToUser(String userId, String userName, String reason) {
        executor.execute(() -> {
            // Create message body based on reason
            String message = "Your account registration has been rejected";
            if (reason != null && !reason.isEmpty()) {
                message += ": " + reason;
            }

            // Create data for the Cloud Function
            Map<String, Object> data = new HashMap<>();
            data.put("userId", userId);
            data.put("title", "Account Rejected");
            data.put("body", message);

            // Call the Cloud Function
            FirebaseFunctions.getInstance()
                    .getHttpsCallable("sendNotification")
                    .call(data)
                    .addOnSuccessListener(result -> {
                        Log.d(TAG, "Rejection notification sent to user: " + result.getData());
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to send rejection notification", e);
                    });
        });
    }
}