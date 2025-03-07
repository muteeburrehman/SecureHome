package io.xconn.securehome.services;

import android.os.AsyncTask;
import android.util.Log;
import io.xconn.securehome.models.Device;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class DeviceConnectionService {
    private static final String TAG = "DeviceConnectionService";
    private static final int DEFAULT_PORT = 80;
    private static final int CONNECTION_TIMEOUT = 5000; // 5 seconds

    // Connection callback interface
    public interface ConnectionCallback {
        void onConnectionSuccess();
        void onConnectionFailure(String errorMessage);
    }

    // Status update callback interface - moved from inner class to outer class
    public interface StatusUpdateCallback {
        void onStatusUpdateSuccess();
        void onStatusUpdateFailure(String errorMessage);
    }

    /**
     * Test connection to a device
     * @param device The device to test connection to
     * @param callback Callback for connection result
     */
    public void testConnection(Device device, ConnectionCallback callback) {
        if (device.getIpAddress() == null || device.getIpAddress().isEmpty()) {
            callback.onConnectionFailure("IP address cannot be empty");
            return;
        }

        new ConnectionTestTask(callback).execute(device);
    }

    /**
     * Connect to a device and establish persistent connection
     * @param device The device to connect to
     * @param callback Callback for connection result
     */
    public void connectToDevice(Device device, ConnectionCallback callback) {
        if (device.getIpAddress() == null || device.getIpAddress().isEmpty()) {
            callback.onConnectionFailure("IP address cannot be empty");
            return;
        }

        new ConnectDeviceTask(callback).execute(device);
    }

    /**
     * Send command to a connected device
     * @param device The device to send command to
     * @param command The command to send
     * @param callback Callback for command result
     */
    public void sendCommand(Device device, String command, ConnectionCallback callback) {
        // Implementation would depend on your specific protocol
        // This is a placeholder for the actual implementation
        Log.d(TAG, "Sending command to device: " + command);

        // For demonstration, we'll just check if the device is connected
        if (device.isConnected()) {
            callback.onConnectionSuccess();
        } else {
            callback.onConnectionFailure("Device is not connected");
        }
    }

    /**
     * Update device on/off status - moved from inner class to outer class
     * @param device The device to update
     * @param isOn The new status
     * @param callback Callback for status update result
     */
    public void updateDeviceStatus(Device device, boolean isOn, StatusUpdateCallback callback) {
        if (!device.isConnected()) {
            callback.onStatusUpdateFailure("Device is not connected");
            return;
        }

        // Command to send based on device status
        String command = isOn ? "turn_on" : "turn_off";

        // Use an AsyncTask similar to the connection tasks
        new UpdateDeviceStatusTask(callback).execute(device, command);
    }

    /**
     * Check device connection status - moved from inner class to outer class
     * @param device The device to check status for
     * @param callback Callback for connection status
     */
    public void checkDeviceStatus(Device device, ConnectionCallback callback) {
        if (device.getIpAddress() == null || device.getIpAddress().isEmpty()) {
            callback.onConnectionFailure("IP address cannot be empty");
            return;
        }

        new ConnectionTestTask(callback).execute(device);
    }

    /**
     * AsyncTask to test connection to a device
     */
    private static class ConnectionTestTask extends AsyncTask<Device, Void, Boolean> {
        private ConnectionCallback callback;
        private String errorMessage;

        ConnectionTestTask(ConnectionCallback callback) {
            this.callback = callback;
        }

        @Override
        protected Boolean doInBackground(Device... devices) {
            Device device = devices[0];
            Socket socket = new Socket();

            try {
                int port = device.getPort() > 0 ? device.getPort() : DEFAULT_PORT;
                socket.connect(new InetSocketAddress(device.getIpAddress(), port), CONNECTION_TIMEOUT);
                return true;
            } catch (SocketTimeoutException e) {
                errorMessage = "Connection timed out";
                return false;
            } catch (IOException e) {
                errorMessage = "Connection failed: " + e.getMessage();
                return false;
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error closing socket", e);
                }
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                callback.onConnectionSuccess();
            } else {
                callback.onConnectionFailure(errorMessage);
            }
        }
    }

    /**
     * AsyncTask to establish persistent connection to a device
     */
    private static class ConnectDeviceTask extends AsyncTask<Device, Void, Boolean> {
        private ConnectionCallback callback;
        private String errorMessage;

        ConnectDeviceTask(ConnectionCallback callback) {
            this.callback = callback;
        }

        @Override
        protected Boolean doInBackground(Device... devices) {
            Device device = devices[0];
            Socket socket = new Socket();

            try {
                int port = device.getPort() > 0 ? device.getPort() : DEFAULT_PORT;
                socket.connect(new InetSocketAddress(device.getIpAddress(), port), CONNECTION_TIMEOUT);

                // In a real implementation, you would keep this socket open
                // and store it somewhere for future communication
                // For this example, we'll just close it and mark the device as connected

                device.setConnected(true);
                return true;
            } catch (SocketTimeoutException e) {
                errorMessage = "Connection timed out";
                return false;
            } catch (IOException e) {
                errorMessage = "Connection failed: " + e.getMessage();
                return false;
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error closing socket", e);
                }
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                callback.onConnectionSuccess();
            } else {
                callback.onConnectionFailure(errorMessage);
            }
        }
    }

    /**
     * AsyncTask to update device status - moved from inner class to outer class
     */
    private static class UpdateDeviceStatusTask extends AsyncTask<Object, Void, Boolean> {
        private StatusUpdateCallback callback;
        private String errorMessage;

        UpdateDeviceStatusTask(StatusUpdateCallback callback) {
            this.callback = callback;
        }

        @Override
        protected Boolean doInBackground(Object... params) {
            Device device = (Device) params[0];
            String command = (String) params[1];

            try {
                // In a real implementation, you would send the command to the device
                // For this example, we'll just simulate a successful command

                // Simulate network delay
                Thread.sleep(500);

                // 90% success rate for demonstration
                if (Math.random() > 0.1) {
                    return true;
                } else {
                    errorMessage = "Failed to send command to device";
                    return false;
                }
            } catch (Exception e) {
                errorMessage = "Error: " + e.getMessage();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                callback.onStatusUpdateSuccess();
            } else {
                callback.onStatusUpdateFailure(errorMessage);
            }
        }
    }
}