package io.xconn.securehome.utils;

/**
 * Utility class to manage different ESP32 endpoints for different devices
 */
public class Esp32EndpointManager {
    // Array of available device endpoint pairs (ON/OFF)
    private static final String[][] DEVICE_ENDPOINTS = {
            {"/D1ON", "/D1OFF"},           // 1st device
            {"/D2ON", "/D2OFF"},           // 2nd device
            {"/D3ON", "/D3OFF"},           // 3rd device
            {"/D4ON", "/D4OFF"},           // 4th device
            {"/D5ON", "/D5OFF"},           // 5th device
            {"/D6ON", "/D6OFF"},           // 6th device
            {"/D7ON", "/D7OFF"},           // 7th device
            {"/D8ON", "/D8OFF"},           // 8th device
            {"/D9ON", "/D9OFF"},           // 9th device
            {"/D10ON", "/D10OFF"},         // 10th device
            {"/D11ON", "/D11OFF"},         // 11th device
            {"/D12ON", "/D12OFF"},         // 12th device
            {"/D13ON", "/D13OFF"},         // 13th device
            {"/D14ON", "/D14OFF"},         // 14th device
            {"/D15ON", "/D15OFF"},         // 15th device
            {"/D16ON", "/D16OFF"},         // 16th device
            {"/D17ON", "/D17OFF"},         // 17th device
            {"/D18ON", "/D18OFF"},         // 18th device
            {"/D19ON", "/D19OFF"},         // 19th device
            {"/D20ON", "/D20OFF"},         // 20th device
            {"/D21ON", "/D21OFF"},         // 21st device
            {"/D22ON", "/D22OFF"},         // 22nd device
            {"/D23ON", "/D23OFF"}          // 23rd device

    };

    /**
     * Gets the appropriate ON endpoint URL for a device based on its index
     * @param deviceIndex The index of the device (0-22)
     * @return The corresponding ON endpoint URL
     */
    public static String getOnEndpoint(int deviceIndex) {
        // Ensure index is within bounds
        int safeIndex = Math.min(Math.max(deviceIndex, 0), DEVICE_ENDPOINTS.length - 1);
        return DEVICE_ENDPOINTS[safeIndex][0];
    }

    /**
     * Gets the appropriate OFF endpoint URL for a device based on its index
     * @param deviceIndex The index of the device (0-22)
     * @return The corresponding OFF endpoint URL
     */
    public static String getOffEndpoint(int deviceIndex) {
        // Ensure index is within bounds
        int safeIndex = Math.min(Math.max(deviceIndex, 0), DEVICE_ENDPOINTS.length - 1);
        return DEVICE_ENDPOINTS[safeIndex][1];
    }
}