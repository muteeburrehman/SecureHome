package io.xconn.securehome.services;

import android.content.Context;
import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import io.xconn.securehome.models.UserModel;
import io.xconn.securehome.utils.SharedPreferencesManager;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Service responsible for handling push notifications using Firebase Cloud Messaging
 */
public class NotificationService {
    private static final String TAG = "NotificationService";
    private static final String FCM_API_URL = "https://fcm.googleapis.com/fcm/send";
    // Replace with your actual server key
    private static final String FCM_SERVER_KEY = "AAAAnp54E4E:APA91bF09wKMyPx9H-CrlwzbvIsbJ2oeyjHHwUlVyLRci4RhxH3t18js-br9INWW1gYDAmTYZLi3kusD-RPXncPqsKQV3BHEma8oUzP8qLErDZWmkOUTvLNQ-8ewcxSH3D8Y2rTCowie";

    private static final OkHttpClient client = new OkHttpClient();
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json");
    private static final Executor executor = Executors.newSingleThreadExecutor();

    private static final String PREF_FCM_TOKEN = "fcm_token";

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
     * Send registration request notification to admin users
     * @param userName Name of the user requesting registration
     * @param email Email of the user requesting registration
     * @param userId ID of the user requesting registration
     */
    public static void sendRegistrationRequestToAdmin(String userName, String email, String userId) {
        executor.execute(() -> {
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            // Get all admin users
            db.collection("users")
                    .whereEqualTo("role", UserModel.ROLE_ADMIN)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        List<String> adminTokens = new ArrayList<>();

                        for (QueryDocumentSnapshot document : querySnapshot) {
                            String token = document.getString("token");
                            if (token != null) {
                                adminTokens.add(token);
                                Log.d(TAG, "Admin token found: " + token);
                            }
                        }

                        // Send notification to all admins
                        for (String token : adminTokens) {
                            sendNotificationToToken(
                                    "New Registration Request",
                                    userName + " (" + email + ") has requested to join the system",
                                    token,
                                    createAdminRequestData(userId)
                            );
                        }
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to retrieve admin tokens", e));
        });
    }

    /**
     * Send approval notification to a user
     * @param userId ID of the approved user
     * @param userName Name of the approved user
     */
    public static void sendApprovalNotificationToUser(String userId, String userName) {
        executor.execute(() -> {
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            db.collection("users")
                    .document(userId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String token = documentSnapshot.getString("token");
                            if (token != null) {
                                sendNotificationToToken(
                                        "Account Approved",
                                        "Your account has been approved. You can now use all features of the system.",
                                        token,
                                        null
                                );
                            } else {
                                Log.d(TAG, "No token found for user: " + userId);
                            }
                        }
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to retrieve user token", e));
        });
    }

    /**
     * Send rejection notification to a user
     * @param userId ID of the rejected user
     * @param userName Name of the rejected user
     * @param reason Optional reason for rejection
     */
    public static void sendRejectionNotificationToUser(String userId, String userName, String reason) {
        executor.execute(() -> {
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            db.collection("users")
                    .document(userId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String token = documentSnapshot.getString("token");
                            if (token != null) {
                                String message = "Your account registration has been rejected";
                                if (reason != null && !reason.isEmpty()) {
                                    message += ": " + reason;
                                }

                                sendNotificationToToken(
                                        "Account Rejected",
                                        message,
                                        token,
                                        null
                                );
                            }
                        }
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to retrieve user token", e));
        });
    }

    /**
     * Create data payload for admin request notification
     * @param userId User ID of the requester
     * @return Map of data key-values
     */
    private static Map<String, String> createAdminRequestData(String userId) {
        Map<String, String> data = new HashMap<>();
        data.put("type", "registration_request");
        data.put("userId", userId);
        return data;
    }

    /**
     * Send a notification to a specific FCM token
     * @param title Notification title
     * @param body Notification body
     * @param token FCM token to send to
     * @param data Additional data payload (can be null)
     */
    private static void sendNotificationToToken(String title, String body, String token, Map<String, String> data) {
        try {
            JSONObject notification = new JSONObject();
            notification.put("title", title);
            notification.put("body", body);
            notification.put("sound", "default");

            JSONObject message = new JSONObject();
            message.put("to", token);
            message.put("notification", notification);

            // Add data payload if provided
            if (data != null && !data.isEmpty()) {
                JSONObject dataJson = new JSONObject();
                for (Map.Entry<String, String> entry : data.entrySet()) {
                    dataJson.put(entry.getKey(), entry.getValue());
                }
                message.put("data", dataJson);
            }

            // Create request
            RequestBody requestBody = RequestBody.create(JSON_MEDIA_TYPE, message.toString());
            Request request = new Request.Builder()
                    .url(FCM_API_URL)
                    .post(requestBody)
                    .addHeader("Authorization", "key=" + FCM_SERVER_KEY)
                    .addHeader("Content-Type", "application/json")
                    .build();

            // Execute request
            Response response = client.newCall(request).execute();
            if (!response.isSuccessful()) {
                Log.e(TAG, "Failed to send notification: " + response);
            } else {
                Log.d(TAG, "Notification sent successfully to: " + token);
            }
        } catch (JSONException | IOException e) {
            Log.e(TAG, "Error sending notification", e);
        }
    }
}