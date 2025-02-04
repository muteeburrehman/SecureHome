package io.xconn.securehome.api;

import io.xconn.securehome.api.request.LoginRequest;
import io.xconn.securehome.api.request.RegisterRequest;
import io.xconn.securehome.api.response.LoginResponse;
import io.xconn.securehome.api.response.RegisterResponse;
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

    public void register(String fullName, String email, String phoneNumber, String password,
                         Callback<RegisterResponse> callback) {
        RegisterRequest request = new RegisterRequest(fullName, email, phoneNumber, password);
        RetrofitClient.getInstance().getApi().registerUser(request).enqueue(callback);
    }

    public void login(String email, String password, Callback<LoginResponse> callback) {
        LoginRequest request = new LoginRequest(email, password);
        RetrofitClient.getInstance().getApi().loginUser(request).enqueue(callback);
    }
}