// app/src/main/java/io/xconn/securehome/repository/ScheduleRepository.java
package io.xconn.securehome.repository;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import io.xconn.securehome.api.ApiService;
import io.xconn.securehome.api.RetrofitClient;
import io.xconn.securehome.api.response.MessageResponse;
import io.xconn.securehome.models.DeviceSchedule;
import io.xconn.securehome.models.Schedule;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ScheduleRepository {
    private static final String TAG = "ScheduleRepository";
    private final ApiService apiService;
    private final Context context;
    private final MutableLiveData<DeviceSchedule> schedulesLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoadingLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessageLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> successMessageLiveData = new MutableLiveData<>();

    public ScheduleRepository(Context context) {
        this.context = context;
        this.apiService = RetrofitClient.getInstance(context).getApi();
        isLoadingLiveData.setValue(false);
    }

    public LiveData<DeviceSchedule> getSchedules() {
        return schedulesLiveData;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoadingLiveData;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessageLiveData;
    }

    public LiveData<String> getSuccessMessage() {
        return successMessageLiveData;
    }

    public void fetchSchedules(int homeId, int deviceId) {
        isLoadingLiveData.setValue(true);

        apiService.getDeviceSchedules(homeId, deviceId).enqueue(new Callback<DeviceSchedule>() {
            @Override
            public void onResponse(Call<DeviceSchedule> call, Response<DeviceSchedule> response) {
                isLoadingLiveData.setValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    schedulesLiveData.setValue(response.body());
                    Log.d(TAG, "Schedules fetched successfully");
                } else {
                    errorMessageLiveData.setValue("Failed to fetch schedules: " + response.message());
                    Log.e(TAG, "Error fetching schedules: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<DeviceSchedule> call, Throwable t) {
                isLoadingLiveData.setValue(false);
                errorMessageLiveData.setValue("Network error: " + t.getMessage());
                Log.e(TAG, "Network error when fetching schedules", t);
            }
        });
    }

    public void addSchedule(int homeId, int deviceId, Schedule schedule, OnScheduleAddedListener listener) {
        isLoadingLiveData.setValue(true);

        apiService.createSchedule(homeId, deviceId, schedule).enqueue(new Callback<MessageResponse>() {
            @Override
            public void onResponse(Call<MessageResponse> call, Response<MessageResponse> response) {
                isLoadingLiveData.setValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    successMessageLiveData.setValue(response.body().getMessage());
                    // Refresh schedules after adding
                    fetchSchedules(homeId, deviceId);
                    listener.onScheduleAdded(response.body().getMessage());
                    Log.d(TAG, "Schedule added successfully");
                } else {
                    String errorMessage = "Failed to add schedule: " + response.message();
                    errorMessageLiveData.setValue(errorMessage);
                    listener.onError(errorMessage);
                    Log.e(TAG, errorMessage);
                }
            }

            @Override
            public void onFailure(Call<MessageResponse> call, Throwable t) {
                isLoadingLiveData.setValue(false);
                String errorMessage = "Network error: " + t.getMessage();
                errorMessageLiveData.setValue(errorMessage);
                listener.onError(errorMessage);
                Log.e(TAG, "Network error when adding schedule", t);
            }
        });
    }

    public void deleteSchedule(int homeId, int deviceId, int scheduleId, OnScheduleDeletedListener listener) {
        isLoadingLiveData.setValue(true);

        apiService.deleteSchedule(homeId, deviceId, scheduleId).enqueue(new Callback<MessageResponse>() {
            @Override
            public void onResponse(Call<MessageResponse> call, Response<MessageResponse> response) {
                isLoadingLiveData.setValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    successMessageLiveData.setValue(response.body().getMessage());
                    // Refresh schedules after deletion
                    fetchSchedules(homeId, deviceId);
                    listener.onScheduleDeleted(response.body().getMessage());
                    Log.d(TAG, "Schedule deleted successfully");
                } else {
                    String errorMessage = "Failed to delete schedule: " + response.message();
                    errorMessageLiveData.setValue(errorMessage);
                    listener.onError(errorMessage);
                    Log.e(TAG, errorMessage);
                }
            }

            @Override
            public void onFailure(Call<MessageResponse> call, Throwable t) {
                isLoadingLiveData.setValue(false);
                String errorMessage = "Network error: " + t.getMessage();
                errorMessageLiveData.setValue(errorMessage);
                listener.onError(errorMessage);
                Log.e(TAG, "Network error when deleting schedule", t);
            }
        });
    }

    public interface OnScheduleAddedListener {
        void onScheduleAdded(String message);
        void onError(String message);
    }

    public interface OnScheduleDeletedListener {
        void onScheduleDeleted(String message);
        void onError(String message);
    }
}