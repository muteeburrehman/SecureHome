package io.xconn.securehome.api;

import android.content.Context;

import io.xconn.securehome.network.ApiConfig;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static RetrofitClient mainInstance = null;
    private final ApiService apiService;
    private static final String ESP32_BASE_URL = "http://esp32.local";

    // Constructor for normal API usage with saved configuration
    private RetrofitClient(Context context) {
        String baseUrl = ApiConfig.getBaseUrl(context);
        if (baseUrl == null) {
            throw new IllegalStateException("Server URL not configured");
        }

        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();

        apiService = retrofit.create(ApiService.class);
    }

    // Constructor for creating with a specific URL (for testing connections)
    private RetrofitClient(String baseUrl) {
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();

        apiService = retrofit.create(ApiService.class);
    }

    /**
     * Get the singleton instance of RetrofitClient configured with the saved server settings
     * @param context Application context
     * @return RetrofitClient instance
     */
    public static synchronized RetrofitClient getInstance(Context context) {
        if (mainInstance == null) {
            mainInstance = new RetrofitClient(context);
        }
        return mainInstance;
    }

    /**
     * Create a temporary instance for testing connections to a specific server
     * @param baseUrl The base URL to test
     * @return A new RetrofitClient instance for testing
     */
    public static RetrofitClient createInstance(String baseUrl) {
        return new RetrofitClient(baseUrl);
    }

    /**
     * Get the API service for making calls to the main server
     * @return ApiService instance
     */
    public ApiService getApi() {
        return apiService;
    }

    /**
     * Get an API service for making calls specifically to the ESP32 device
     * @return ApiService instance configured for ESP32
     */
    public static ApiService getEsp32Api() {
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .build();

        Retrofit esp32Retrofit = new Retrofit.Builder()
                .baseUrl(ESP32_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();

        return esp32Retrofit.create(ApiService.class);
    }
}