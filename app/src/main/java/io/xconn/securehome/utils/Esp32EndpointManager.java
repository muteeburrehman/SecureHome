package io.xconn.securehome.utils;

/**
 * Utility class to manage different ESP32 endpoints for different devices
 */
public class Esp32EndpointManager {
    // Array of available device endpoint pairs (ON/OFF)
    private static final String[][] DEVICE_ENDPOINTS = {
            {"/unlock","/lock"},           // Default/first device
            {"/RedON", "/RedOFF"},               // Second device
            {"/BlueON", "/BlueOFF"},             // Third device
            {"/YellowON", "/YellowOFF"},         // Fourth device
            {"/PurpleON", "/PurpleOFF"},         // Fifth device
            {"/OrangeON", "/OrangeOFF"},         // Sixth device
            {"/WhiteON", "/WhiteOFF"},           // Seventh device
            {"/BlackON", "/BlackOFF"},           // Eighth device
            {"/CyanON", "/CyanOFF"},             // Ninth device
            {"/MagentaON", "/MagentaOFF"},       // Tenth device
            {"/BrownON", "/BrownOFF"},           // 11th device
            {"/GrayON", "/GrayOFF"},             // 12th device
            {"/PinkON", "/PinkOFF"},             // 13th device
            {"/LimeON", "/LimeOFF"},             // 14th device
            {"/TealON", "/TealOFF"},             // 15th device
            {"/NavyON", "/NavyOFF"},             // 16th device
            {"/SilverON", "/SilverOFF"},         // 17th device
            {"/GoldON", "/GoldOFF"},             // 18th device
            {"/DeviceAON", "/DeviceAOFF"},       // 19th device
            {"/DeviceBON", "/DeviceBOFF"},       // 20th device
            {"/DeviceCON", "/DeviceCOFF"},       // 21st device
            {"/DeviceDON", "/DeviceDOFF"},       // 22nd device
            {"/DeviceEON", "/DeviceEOFF"}        // 23rd device
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