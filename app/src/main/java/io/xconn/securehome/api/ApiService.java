package io.xconn.securehome.api;

import io.xconn.securehome.api.request.LoginRequest;
import io.xconn.securehome.api.request.RegisterRequest;
import io.xconn.securehome.api.response.LoginResponse;
import io.xconn.securehome.api.response.RegisterResponse;
import io.xconn.securehome.api.response.ServerInfoResponse;
import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import io.xconn.securehome.api.response.RecognitionResponse;

public interface ApiService {
    @Multipart
    @POST("upload/")
    Call<Void> uploadImage(@Part MultipartBody.Part image);

    @Multipart
    @POST("recognize/")
    Call<RecognitionResponse> recognizeFace(@Part MultipartBody.Part image);

    // New authentication endpoints
    @POST("auth/register/")
    Call<RegisterResponse> registerUser(@Body RegisterRequest request);

    @POST("auth/login/")
    Call<LoginResponse> loginUser(@Body LoginRequest request);

    @GET("api/server-info/")
    Call<ServerInfoResponse> getServerInfo();
}
