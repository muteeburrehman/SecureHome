// app/src/main/java/io/xconn/securehome/api/ApiService.java
package io.xconn.securehome.api;

import java.util.List;

import io.xconn.securehome.api.request.DeviceStatusUpdateRequest;
import io.xconn.securehome.api.response.MessageResponse;
import io.xconn.securehome.api.response.RecognitionResponse;
import io.xconn.securehome.api.response.ServerInfoResponse;
import io.xconn.securehome.models.Device;
import io.xconn.securehome.models.DeviceSchedule;
import io.xconn.securehome.models.Home;
import io.xconn.securehome.models.Schedule;
import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;

public interface ApiService {
    // Original endpoints
    @Multipart
    @POST("upload/")
    Call<Void> uploadImage(@Part MultipartBody.Part image);

    @Multipart
    @POST("recognize/")
    Call<RecognitionResponse> recognizeFace(@Part MultipartBody.Part image);

    @GET("api/server-info/")
    Call<ServerInfoResponse> getServerInfo();

    // Home endpoints
    @POST("homes/")
    Call<Home> createHome(@Body Home home);

    @GET("homes/")
    Call<List<Home>> getHomes();

    // Device endpoints
    @POST("homes/{homeId}/devices/")
    Call<Device> createDevice(@Path("homeId") int homeId, @Body Device device);

    @GET("homes/{homeId}/devices/")
    Call<List<Device>> getDevices(@Path("homeId") int homeId);

    @PUT("homes/{homeId}/devices/{deviceId}/status")
    Call<ResponseBody> updateDeviceStatus(
            @Path("homeId") int homeId,
            @Path("deviceId") int deviceId,
            @Body DeviceStatusUpdateRequest status);

    @GET("homes/{homeId}/devices/{deviceId}/status")
    Call<ResponseBody> getDeviceStatus(
            @Path("homeId") int homeId,
            @Path("deviceId") int deviceId);

    // Schedule endpoints
    @POST("homes/{homeId}/devices/{deviceId}/schedule")
    Call<MessageResponse> createSchedule(
            @Path("homeId") int homeId,
            @Path("deviceId") int deviceId,
            @Body Schedule schedule);

    @GET("homes/{homeId}/devices/{deviceId}/schedule")
    Call<DeviceSchedule> getDeviceSchedules(
            @Path("homeId") int homeId,
            @Path("deviceId") int deviceId);

    @DELETE("homes/{homeId}/devices/{deviceId}/schedule/{scheduleId}")
    Call<MessageResponse> deleteSchedule(
            @Path("homeId") int homeId,
            @Path("deviceId") int deviceId,
            @Path("scheduleId") int scheduleId);
}