package io.xconn.securehome.api;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentSnapshot;

import io.xconn.securehome.models.UserModel;
import io.xconn.securehome.services.EmailService;
import io.xconn.securehome.services.NotificationService;
import io.xconn.securehome.utils.SharedPreferencesManager;

public class FirebaseAuthManager {
    private static FirebaseAuthManager instance;
    private FirebaseAuth mAuth;
    private FirebaseDatabaseManager dbManager;
    private Context context; // Added context field

    private static final String TAG = "FirebaseAuthManager"; // Add a TAG for logging


    // Modified constructor to accept Context
    private FirebaseAuthManager(Context context) {
        this.context = context;
        mAuth = FirebaseAuth.getInstance();
        dbManager = FirebaseDatabaseManager.getInstance();
    }

    // Modified getInstance method to require Context
    public static synchronized FirebaseAuthManager getInstance(Context context) {
        if (instance == null) {
            instance = new FirebaseAuthManager(context);
        }
        return instance;
    }

    public boolean isUserLoggedIn() {
        return mAuth.getCurrentUser() != null;
    }

    public FirebaseUser getCurrentUser() {
        return mAuth.getCurrentUser();
    }

    public String getUserId() {
        FirebaseUser user = getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    public interface AuthCallback {
        void onSuccess(UserModel userModel);
        void onFailure(Exception e);
    }

    public void register(String email, String password, String displayName, OnCompleteListener<AuthResult> authListener, final AuthCallback callback) {
        final boolean isAdmin = dbManager.isAdminEmail(email);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        FirebaseUser user = task.getResult().getUser();
                        if (user != null) {
                            final String finalUserId = user.getUid(); // Get UID for SharedPreferences

                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(displayName)
                                    .build();

                            user.updateProfile(profileUpdates)
                                    .addOnCompleteListener(profileTask -> {
                                        String role = isAdmin ? UserModel.ROLE_ADMIN : UserModel.ROLE_USER;
                                        String status = isAdmin ? UserModel.STATUS_APPROVED : UserModel.STATUS_PENDING;

                                        UserModel userModel = new UserModel(
                                                finalUserId, // Use finalUserId
                                                email,
                                                displayName,
                                                role,
                                                status,
                                                System.currentTimeMillis()
                                        );

                                        dbManager.saveUserToDatabase(userModel, dbTask -> {
                                            if (dbTask.isSuccessful()) {
                                                // *** FIX 1: SAVE USER_ID and INITIALIZE NOTIFICATION SERVICE ***
                                                SharedPreferencesManager.getInstance(context).saveString("user_id", finalUserId);
                                                Log.d(TAG, "register: User ID saved to SharedPreferences: " + finalUserId);
                                                NotificationService.initialize(context);
                                                Log.d(TAG, "register: Called NotificationService.initialize()");
                                                // *** END OF FIX 1 ***

                                                if (!isAdmin) {
                                                    NotificationService.sendRegistrationRequestToAdmin(
                                                            displayName, email, finalUserId); // Use finalUserId
                                                    dbManager.getAdminEmails(adminEmails -> {
                                                        if (adminEmails != null && !adminEmails.isEmpty()) {
                                                            EmailService emailService = new EmailService(context);
                                                            for (String adminEmail : adminEmails) {
                                                                emailService.sendRegistrationRequestToAdmin(
                                                                        adminEmail, displayName, email);
                                                            }
                                                        }
                                                    });
                                                }

                                                if (callback != null) {
                                                    callback.onSuccess(userModel);
                                                }
                                            } else {
                                                if (callback != null) {
                                                    callback.onFailure(dbTask.getException() != null ?
                                                            dbTask.getException() :
                                                            new Exception("Failed to save user data"));
                                                }
                                            }
                                            if (authListener != null) {
                                                authListener.onComplete(task);
                                            }
                                        });
                                    });
                        } else {
                            // ... (existing failure handling)
                            if (callback != null) {
                                callback.onFailure(new Exception("User creation succeeded but user object is null"));
                            }
                            if (authListener != null) {
                                authListener.onComplete(task);
                            }
                        }
                    } else {
                        // ... (existing failure handling)
                        if (callback != null) {
                            callback.onFailure(task.getException() != null ?
                                    task.getException() :
                                    new Exception("Registration failed"));
                        }
                        if (authListener != null) {
                            authListener.onComplete(task);
                        }
                    }
                });
    }

    public void login(String email, String password, OnCompleteListener<AuthResult> authListener, final AuthCallback callback) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        FirebaseUser user = task.getResult().getUser();
                        if (user != null) {
                            final String finalUserId = user.getUid(); // Get UID for SharedPreferences

                            dbManager.getUserById(finalUserId, userTask -> { // Use finalUserId
                                if (userTask.isSuccessful() && userTask.getResult() != null && userTask.getResult().exists()) {
                                    DocumentSnapshot document = userTask.getResult();
                                    UserModel userModel = document.toObject(UserModel.class);

                                    // *** FIX 2: SAVE USER_ID and INITIALIZE NOTIFICATION SERVICE (EXISTING USER) ***
                                    SharedPreferencesManager.getInstance(context).saveString("user_id", finalUserId);
                                    Log.d(TAG, "login: User ID saved to SharedPreferences (existing user): " + finalUserId);
                                    NotificationService.initialize(context);
                                    Log.d(TAG, "login: Called NotificationService.initialize() (existing user)");
                                    // *** END OF FIX 2 ***

                                    if (callback != null) {
                                        callback.onSuccess(userModel);
                                    }
                                } else {
                                    // If no user document exists, create one
                                    boolean isAdminLogin = dbManager.isAdminEmail(email);
                                    UserModel userModel = new UserModel(
                                            finalUserId, // Use finalUserId
                                            email,
                                            user.getDisplayName() != null ? user.getDisplayName() : "User",
                                            isAdminLogin ? UserModel.ROLE_ADMIN : UserModel.ROLE_USER,
                                            isAdminLogin ? UserModel.STATUS_APPROVED : UserModel.STATUS_PENDING,
                                            System.currentTimeMillis()
                                    );

                                    dbManager.saveUserToDatabase(userModel, dbTask -> {
                                        // *** FIX 3: SAVE USER_ID and INITIALIZE NOTIFICATION SERVICE (NEW USER DOC ON LOGIN) ***
                                        if (dbTask.isSuccessful()) {
                                            SharedPreferencesManager.getInstance(context).saveString("user_id", finalUserId);
                                            Log.d(TAG, "login: User ID saved to SharedPreferences (new user doc on login): " + finalUserId);
                                            NotificationService.initialize(context);
                                            Log.d(TAG, "login: Called NotificationService.initialize() (new user doc on login)");

                                            if (callback != null) {
                                                callback.onSuccess(userModel);
                                            }
                                        } else {
                                            if (callback != null) {
                                                callback.onFailure(dbTask.getException() != null ?
                                                        dbTask.getException() :
                                                        new Exception("Failed to save user data"));
                                            }
                                        }
                                        // *** END OF FIX 3 ***
                                    });
                                }
                                if (authListener != null) {
                                    authListener.onComplete(task);
                                }
                            });
                        } else {
                            // ... (existing failure handling)
                            if (callback != null) {
                                callback.onFailure(new Exception("Login succeeded but user object is null"));
                            }
                            if (authListener != null) {
                                authListener.onComplete(task);
                            }
                        }
                    } else {
                        // ... (existing failure handling)
                        if (callback != null) {
                            callback.onFailure(task.getException() != null ?
                                    task.getException() :
                                    new Exception("Authentication failed"));
                        }
                        if (authListener != null) {
                            authListener.onComplete(task);
                        }
                    }
                });
    }
    public void logout() {
        // *** FIX 4: CONSIDER CLEARING USER_ID FROM PREFERENCES ON LOGOUT ***
        String userId = SharedPreferencesManager.getInstance(context).getString("user_id", null);
        if (userId != null) {
            Log.d(TAG, "logout: Clearing user_id: " + userId + " from SharedPreferences.");
            SharedPreferencesManager.getInstance(context).remove("user_id");
            // Optionally, also clear the FCM token from SharedPreferences if you stored it there directly,
            // though NotificationService.initialize() would fetch a new one on next login.
            // SharedPreferencesManager.getInstance(context).remove("fcm_token");
        }
        // You might also want to remove the token from the user's document in Firestore
        // if (userId != null) {
        //    NotificationService.updateToken(userId, null); // or an empty string
        // }
        // *** END OF FIX 4 ***
        mAuth.signOut();
        Log.d(TAG, "User logged out.");
    }

    public void getCurrentUserModel(AuthCallback callback) {
        FirebaseUser user = getCurrentUser();
        if (user != null) {
            dbManager.getUserById(user.getUid(), task -> {
                if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                    UserModel userModel = task.getResult().toObject(UserModel.class);
                    if (callback != null) {
                        callback.onSuccess(userModel);
                    }
                } else {
                    if (callback != null) {
                        callback.onFailure(task.getException() != null ?
                                task.getException() : new Exception("User document not found"));
                    }
                }
            });
        } else if (callback != null) {
            callback.onFailure(new Exception("User not logged in"));
        }
    }

    public boolean isAdmin(UserModel userModel) {
        return userModel != null && userModel.isAdmin();
    }

    public boolean isUserApproved(UserModel userModel) {
        return userModel != null && userModel.isApproved();
    }
}