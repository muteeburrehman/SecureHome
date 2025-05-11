package io.xconn.securehome.api;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.xconn.securehome.models.UserModel;

public class FirebaseDatabaseManager {
    private static FirebaseDatabaseManager instance;
    private FirebaseFirestore db;

    private static final String USERS_COLLECTION = "users";
    private static final String ADMIN_EMAIL = "muteeb285@gmail.com"; // Default admin email

    private FirebaseDatabaseManager() {
        db = FirebaseFirestore.getInstance();
    }

    public static synchronized FirebaseDatabaseManager getInstance() {
        if (instance == null) {
            instance = new FirebaseDatabaseManager();
        }
        return instance;
    }

    // Create or update user profile in Firestore
    public void saveUserToDatabase(UserModel user, OnCompleteListener<Void> listener) {
        // Instead of manually mapping fields, we'll use the UserModel as is,
        // letting Firebase use the getters/setters automatically
        db.collection(USERS_COLLECTION)
                .document(user.getUserId())
                .set(user)
                .addOnCompleteListener(listener);
    }

    // Check if a user is the predefined admin
    public boolean isAdminEmail(String email) {
        return ADMIN_EMAIL.equalsIgnoreCase(email);
    }

    // Get user data by ID
    public void getUserById(String userId, OnCompleteListener<DocumentSnapshot> listener) {
        db.collection(USERS_COLLECTION)
                .document(userId)
                .get()
                .addOnCompleteListener(listener);
    }

    // Get user data by email
    public void getUserByEmail(String email, OnCompleteListener<QuerySnapshot> listener) {
        db.collection(USERS_COLLECTION)
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .addOnCompleteListener(listener);
    }

    // Get all users
    public void getAllUsers(OnCompleteListener<QuerySnapshot> listener) {
        db.collection(USERS_COLLECTION)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(listener);
    }

    // Get users by approval status
    public void getUsersByApprovalStatus(String status, OnCompleteListener<QuerySnapshot> listener) {
        db.collection(USERS_COLLECTION)
                .whereEqualTo("approvalStatus", status)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(listener);
    }

    // Update user approval status
    public void updateUserApprovalStatus(String userId, String status, OnCompleteListener<Void> listener) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("approvalStatus", status);

        db.collection(USERS_COLLECTION)
                .document(userId)
                .update(updates)
                .addOnCompleteListener(listener);
    }

    // Delete a user from database
    public void deleteUser(String userId, OnCompleteListener<Void> listener) {
        db.collection(USERS_COLLECTION)
                .document(userId)
                .delete()
                .addOnCompleteListener(listener);
    }

    public void getAdminEmails(AdminEmailsCallback callback) {
        db.collection(USERS_COLLECTION)
                .whereEqualTo("role", UserModel.ROLE_ADMIN)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<String> adminEmails = new ArrayList<>();
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        UserModel admin = document.toObject(UserModel.class);
                        if (admin != null && admin.getEmail() != null) {
                            adminEmails.add(admin.getEmail());
                        }
                    }
                    // Add the default admin email if it's not already in the list
                    if (!adminEmails.contains(ADMIN_EMAIL)) {
                        adminEmails.add(ADMIN_EMAIL);
                    }
                    callback.onEmailsRetrieved(adminEmails);
                })
                .addOnFailureListener(e -> {
                    Log.e("FirebaseDatabaseManager", "Error getting admin emails", e);
                    List<String> fallbackList = new ArrayList<>();
                    fallbackList.add(ADMIN_EMAIL);
                    callback.onEmailsRetrieved(fallbackList);
                });
    }

    public interface AdminEmailsCallback {
        void onEmailsRetrieved(List<String> adminEmails);
    }
}