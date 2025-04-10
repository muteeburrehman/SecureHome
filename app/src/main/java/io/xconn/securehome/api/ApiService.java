package io.xconn.securehome.api;

import io.xconn.securehome.api.response.ServerInfoResponse;
import okhttp3.MultipartBody;
import retrofit2.Call;
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


    @GET("api/server-info/")
    Call<ServerInfoResponse> getServerInfo();
}
