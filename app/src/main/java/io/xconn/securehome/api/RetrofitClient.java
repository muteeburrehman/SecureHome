package io.xconn.securehome.api;

import io.xconn.securehome.network.ApiConfig;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;  // Added this import
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static RetrofitClient instance;
    private final Retrofit retrofit;

    private RetrofitClient() {
        // Setup logging interceptor
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);  // Set logging level

        // Create OkHttpClient with logging interceptor
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(logging)  // Add the logging interceptor
                .build();

        // Initialize Retrofit with the client
        retrofit = new Retrofit.Builder()
                .baseUrl(ApiConfig.BASE_URL)  // Set the base URL
                .client(client)  // Add OkHttp client
                .addConverterFactory(GsonConverterFactory.create())  // Use Gson converter
                .build();
    }

    // Singleton pattern to get RetrofitClient instance
    public static synchronized RetrofitClient getInstance() {
        if (instance == null) {
            instance = new RetrofitClient();
        }
        return instance;
    }

    // Method to return an instance of ApiService
    public ApiService getApi() {
        return retrofit.create(ApiService.class);
    }
}