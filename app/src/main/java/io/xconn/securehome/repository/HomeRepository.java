// app/src/main/java/io/xconn/securehome/repository/HomeRepository.java
package io.xconn.securehome.repository;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import io.xconn.securehome.api.ApiService;
import io.xconn.securehome.api.RetrofitClient;
import io.xconn.securehome.models.Home;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeRepository {
    private static final String TAG = "HomeRepository";
    private final ApiService apiService;
    private final Context context;
    private final MutableLiveData<List<Home>> homesLiveData = new MutableLiveData<>();
    private final MutableLiveData<Home> currentHomeLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoadingLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessageLiveData = new MutableLiveData<>();

    public HomeRepository(Context context) {
        this.context = context;
        this.apiService = RetrofitClient.getInstance(context).getApi();
        homesLiveData.setValue(new ArrayList<>());
        isLoadingLiveData.setValue(false);
    }

    public LiveData<List<Home>> getHomes() {
        return homesLiveData;
    }

    public LiveData<Home> getCurrentHome() {
        return currentHomeLiveData;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoadingLiveData;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessageLiveData;
    }

    public void setCurrentHome(Home home) {
        currentHomeLiveData.setValue(home);
    }

    public void fetchHomes() {
        isLoadingLiveData.setValue(true);

        apiService.getHomes().enqueue(new Callback<List<Home>>() {
            @Override
            public void onResponse(Call<List<Home>> call, Response<List<Home>> response) {
                isLoadingLiveData.setValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    homesLiveData.setValue(response.body());
                    Log.d(TAG, "Homes fetched successfully: " + response.body().size());
                } else {
                    errorMessageLiveData.setValue("Failed to fetch homes: " + response.message());
                    Log.e(TAG, "Error fetching homes: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<List<Home>> call, Throwable t) {
                isLoadingLiveData.setValue(false);
                errorMessageLiveData.setValue("Network error: " + t.getMessage());
                Log.e(TAG, "Network error when fetching homes", t);
            }
        });
    }

    public void addHome(Home home, OnHomeAddedListener listener) {
        isLoadingLiveData.setValue(true);

        apiService.createHome(home).enqueue(new Callback<Home>() {
            @Override
            public void onResponse(Call<Home> call, Response<Home> response) {
                isLoadingLiveData.setValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    Home newHome = response.body();
                    List<Home> currentHomes = homesLiveData.getValue();
                    if (currentHomes != null) {
                        currentHomes.add(newHome);
                        homesLiveData.setValue(currentHomes);
                    }
                    currentHomeLiveData.setValue(newHome);
                    listener.onHomeAdded(newHome);
                    Log.d(TAG, "Home added successfully: " + newHome.getOwner());
                } else {
                    String errorMessage = "Failed to add home: " +
                            (response.errorBody() != null ? response.errorBody().toString() : response.message());
                    errorMessageLiveData.setValue(errorMessage);
                    listener.onError(errorMessage);
                    Log.e(TAG, errorMessage);
                }
            }

            @Override
            public void onFailure(Call<Home> call, Throwable t) {
                isLoadingLiveData.setValue(false);
                String errorMessage = "Network error: " + t.getMessage();
                errorMessageLiveData.setValue(errorMessage);
                listener.onError(errorMessage);
                Log.e(TAG, "Network error when adding home", t);
            }
        });
    }

    public interface OnHomeAddedListener {
        void onHomeAdded(Home home);
        void onError(String message);
    }
}