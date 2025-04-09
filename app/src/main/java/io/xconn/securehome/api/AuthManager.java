package io.xconn.securehome.api;

import android.content.Context;

import io.xconn.securehome.api.request.LoginRequest;
import io.xconn.securehome.api.request.RegisterRequest;
import io.xconn.securehome.api.response.LoginResponse;
import io.xconn.securehome.api.response.RegisterResponse;
import io.xconn.securehome.network.ApiConfig;
import retrofit2.Callback;

public class AuthManager {
    private static AuthManager instance;
    private String authToken;

    private AuthManager() {}

    public static synchronized AuthManager getInstance() {
        if (instance == null) {
            instance = new AuthManager();
        }
        return instance;
    }

    public void setAuthToken(String token) {
        this.authToken = token;
    }

    public String getAuthToken() {
        return authToken;
    }

    public boolean isServerConfigured(Context context) {
        return ApiConfig.hasServerConfig(context);
    }

    public void register(Context context, String fullName, String email, String phoneNumber, String password,
                         Callback<RegisterResponse> callback) {
        if (!isServerConfigured(context)) {
            // Handle the case where server is not yet configured
            if (callback != null) {
                callback.onFailure(null, new IllegalStateException("Server not configured. Please set up the server connection first."));
            }
            return;
        }

        RegisterRequest request = new RegisterRequest(fullName, email, phoneNumber, password);
        RetrofitClient.getInstance(context).getApi().registerUser(request).enqueue(callback);
    }

    public void login(Context context, String email, String password, Callback<LoginResponse> callback) {
        if (!isServerConfigured(context)) {
            // Handle the case where server is not yet configured
            if (callback != null) {
                callback.onFailure(null, new IllegalStateException("Server not configured. Please set up the server connection first."));
            }
            return;
        }

        LoginRequest request = new LoginRequest(email, password);
        RetrofitClient.getInstance(context).getApi().loginUser(request).enqueue(callback);
    }
}