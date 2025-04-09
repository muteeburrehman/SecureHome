package io.xconn.securehome.api;

import android.content.Context;

import io.xconn.securehome.network.ApiConfig;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static RetrofitClient instance;
    private final Retrofit retrofit;

    private RetrofitClient(String baseUrl) {
        // Setup logging interceptor
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        // Create OkHttpClient with logging interceptor
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .build();

        // Initialize Retrofit with the client
        retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    // Get instance using the saved IP/port
    public static synchronized RetrofitClient getInstance(Context context) {
        String baseUrl = ApiConfig.getBaseUrl(context);
        if (baseUrl == null) {
            throw new IllegalStateException("Server URL not configured. Please set server IP first.");
        }

        // Always create a new instance to use the latest base URL
        instance = new RetrofitClient(baseUrl);
        return instance;
    }

    // Create a specific instance for a given URL
    public static RetrofitClient createInstance(String baseUrl) {
        return new RetrofitClient(baseUrl);
    }

    // Method to return an instance of ApiService
    public ApiService getApi() {
        return retrofit.create(ApiService.class);
    }
}