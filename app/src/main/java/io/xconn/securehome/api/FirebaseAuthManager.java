package io.xconn.securehome.api;

import android.content.Context;

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

public class FirebaseAuthManager {
    private static FirebaseAuthManager instance;
    private FirebaseAuth mAuth;
    private FirebaseDatabaseManager dbManager;
    private Context context; // Added context field

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
        // Check if the email is the predefined admin email
        final boolean isAdmin = dbManager.isAdminEmail(email);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        // Set the user's display name
                        FirebaseUser user = task.getResult().getUser();
                        if (user != null) {
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(displayName)
                                    .build();

                            user.updateProfile(profileUpdates)
                                    .addOnCompleteListener(profileTask -> {
                                        // Create user profile in Firestore after profile is updated
                                        String role = isAdmin ? UserModel.ROLE_ADMIN : UserModel.ROLE_USER;
                                        String status = isAdmin ? UserModel.STATUS_APPROVED : UserModel.STATUS_PENDING;

                                        UserModel userModel = new UserModel(
                                                user.getUid(),
                                                email,
                                                displayName,
                                                role,
                                                status,
                                                System.currentTimeMillis()
                                        );

                                        dbManager.saveUserToDatabase(userModel, dbTask -> {
                                            if (dbTask.isSuccessful()) {
                                                // Send notifications if this is a regular user (pending approval)
                                                if (!isAdmin) {
                                                    // Send notification to admin via FCM
                                                    NotificationService.sendRegistrationRequestToAdmin(
                                                            displayName, email, user.getUid());

                                                    // Find admin emails to send email notifications
                                                    dbManager.getAdminEmails(adminEmails -> {
                                                        if (adminEmails != null && !adminEmails.isEmpty()) {
                                                            EmailService emailService = new EmailService(context); // Now context is defined
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

                                            // Make sure we invoke the external auth listener
                                            if (authListener != null) {
                                                authListener.onComplete(task);
                                            }
                                        });
                                    });
                        } else {
                            if (callback != null) {
                                callback.onFailure(new Exception("User creation succeeded but user object is null"));
                            }
                            if (authListener != null) {
                                authListener.onComplete(task);
                            }
                        }
                    } else {
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
                            // Check user approval status in Firestore
                            dbManager.getUserById(user.getUid(), userTask -> {
                                if (userTask.isSuccessful() && userTask.getResult() != null && userTask.getResult().exists()) {
                                    DocumentSnapshot document = userTask.getResult();
                                    UserModel userModel = document.toObject(UserModel.class);

                                    if (callback != null) {
                                        callback.onSuccess(userModel);
                                    }
                                } else {
                                    // If no user document exists, create one (for migration purposes or first admin)
                                    boolean isAdmin = dbManager.isAdminEmail(email);
                                    UserModel userModel = new UserModel(
                                            user.getUid(),
                                            email,
                                            user.getDisplayName() != null ? user.getDisplayName() : "User",
                                            isAdmin ? UserModel.ROLE_ADMIN : UserModel.ROLE_USER,
                                            isAdmin ? UserModel.STATUS_APPROVED : UserModel.STATUS_PENDING,
                                            System.currentTimeMillis()
                                    );

                                    dbManager.saveUserToDatabase(userModel, dbTask -> {
                                        if (callback != null) {
                                            if (dbTask.isSuccessful()) {
                                                callback.onSuccess(userModel);
                                            } else {
                                                callback.onFailure(dbTask.getException() != null ?
                                                        dbTask.getException() :
                                                        new Exception("Failed to save user data"));
                                            }
                                        }
                                    });
                                }

                                // Make sure we invoke the external auth listener
                                if (authListener != null) {
                                    authListener.onComplete(task);
                                }
                            });
                        } else {
                            if (callback != null) {
                                callback.onFailure(new Exception("Login succeeded but user object is null"));
                            }
                            if (authListener != null) {
                                authListener.onComplete(task);
                            }
                        }
                    } else {
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
        mAuth.signOut();
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