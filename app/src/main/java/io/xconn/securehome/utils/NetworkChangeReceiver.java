package io.xconn.securehome.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

/**
 * Receiver for handling network connectivity changes
 * Can be registered to activities or fragments to respond to network changes
 */
public class NetworkChangeReceiver implements DefaultLifecycleObserver {
    private static final String TAG = "NetworkChangeReceiver";

    public interface NetworkChangeListener {
        void onNetworkChanged(boolean isConnected);
    }

    private final Context context;
    private final NetworkChangeListener listener;
    private BroadcastReceiver broadcastReceiver;
    private ConnectivityManager.NetworkCallback networkCallback;
    private ConnectivityManager connectivityManager;
    private final Handler mainHandler;
    private boolean lastKnownState = false;
    private boolean hasReportedInitialState = false;

    public NetworkChangeReceiver(Context context, NetworkChangeListener listener) {
        this.context = context;
        this.listener = listener;
        this.connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public void onCreate(@NonNull LifecycleOwner owner) {
        DefaultLifecycleObserver.super.onCreate(owner);
        registerNetworkCallback();
    }

    @Override
    public void onDestroy(@NonNull LifecycleOwner owner) {
        DefaultLifecycleObserver.super.onDestroy(owner);
        unregisterNetworkCallback();
    }

    public void registerNetworkCallback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // For Android 7.0 (API 24) and above
            if (connectivityManager != null) {
                networkCallback = new ConnectivityManager.NetworkCallback() {
                    @Override
                    public void onAvailable(@NonNull Network network) {
                        super.onAvailable(network);
                        notifyNetworkChange(true);
                    }

                    @Override
                    public void onLost(@NonNull Network network) {
                        super.onLost(network);
                        notifyNetworkChange(false);
                    }

                    @Override
                    public void onCapabilitiesChanged(@NonNull Network network,
                                                      @NonNull NetworkCapabilities capabilities) {
                        super.onCapabilitiesChanged(network, capabilities);
                        try {
                            boolean isConnected = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
                            notifyNetworkChange(isConnected);
                        } catch (Exception e) {
                            Log.e(TAG, "Error in onCapabilitiesChanged", e);
                        }
                    }
                };

                try {
                    NetworkRequest.Builder builder = new NetworkRequest.Builder();
                    connectivityManager.registerDefaultNetworkCallback(networkCallback);

                    // Report initial state
                    boolean initialState = checkNetworkConnectivity();
                    notifyNetworkChange(initialState);
                } catch (Exception e) {
                    Log.e(TAG, "Error registering network callback", e);
                    // Fall back to broadcast receiver
                    registerBroadcastReceiver();
                }
            } else {
                // If connectivity manager is null, fall back to broadcast receiver
                registerBroadcastReceiver();
            }
        } else {
            // For Android versions below 7.0
            registerBroadcastReceiver();
        }
    }

    private void registerBroadcastReceiver() {
        if (broadcastReceiver == null) {
            broadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    try {
                        boolean isConnected = checkNetworkConnectivity();
                        notifyNetworkChange(isConnected);
                    } catch (Exception e) {
                        Log.e(TAG, "Error in broadcast receiver", e);
                    }
                }
            };
            IntentFilter filter = getIntentFilter();
            try {
                context.registerReceiver(broadcastReceiver, filter);

                // Report initial state
                boolean initialState = checkNetworkConnectivity();
                notifyNetworkChange(initialState);
            } catch (Exception e) {
                Log.e(TAG, "Error registering broadcast receiver", e);
            }
        }
    }

    public void unregisterNetworkCallback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (connectivityManager != null && networkCallback != null) {
                try {
                    connectivityManager.unregisterNetworkCallback(networkCallback);
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "Error unregistering network callback", e);
                }
                networkCallback = null;
            }
        }

        if (broadcastReceiver != null) {
            try {
                context.unregisterReceiver(broadcastReceiver);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Error unregistering broadcast receiver", e);
            }
            broadcastReceiver = null;
        }
    }

    // Added method to fix missing getIntentFilter
    public static IntentFilter getIntentFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        return filter;
    }

    private boolean checkNetworkConnectivity() {
        if (connectivityManager == null) {
            return false;
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Network network = connectivityManager.getActiveNetwork();
                if (network == null) {
                    return false;
                }

                NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
                return capabilities != null &&
                        (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR));
            } else {
                android.net.NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
                return networkInfo != null && networkInfo.isConnected();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking network connectivity", e);
            return false;
        }
    }

    private void notifyNetworkChange(final boolean isConnected) {
        // Only notify if state changed or this is the first notification
        if (isConnected != lastKnownState || !hasReportedInitialState) {
            lastKnownState = isConnected;
            hasReportedInitialState = true;

            if (listener != null) {
                // Always notify on main thread
                mainHandler.post(() -> {
                    try {
                        listener.onNetworkChanged(isConnected);
                    } catch (Exception e) {
                        Log.e(TAG, "Error notifying network change", e);
                    }
                });
            }
        }
    }

    // Add this method for manual registration when not using lifecycle
    public void register() {
        registerNetworkCallback();
    }

    // Add this method for manual unregistration when not using lifecycle
    public void unregister() {
        unregisterNetworkCallback();
    }
}