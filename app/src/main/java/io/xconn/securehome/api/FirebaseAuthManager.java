package io.xconn.securehome.api;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

public class FirebaseAuthManager {
    private static FirebaseAuthManager instance;
    private FirebaseAuth mAuth;

    private FirebaseAuthManager() {
        mAuth = FirebaseAuth.getInstance();
    }

    public static synchronized FirebaseAuthManager getInstance() {
        if (instance == null) {
            instance = new FirebaseAuthManager();
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

    public void register(String email, String password, String displayName, OnCompleteListener<AuthResult> listener) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        // Set the user's display name
                        FirebaseUser user = task.getResult().getUser();
                        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                .setDisplayName(displayName)
                                .build();

                        user.updateProfile(profileUpdates);
                    }
                    if (listener != null) {
                        listener.onComplete(task);
                    }
                });
    }

    public void login(String email, String password, OnCompleteListener<AuthResult> listener) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(listener);
    }

    public void logout() {
        mAuth.signOut();
    }
}