package io.xconn.securehome.services;

import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.xconn.securehome.models.Device;
import io.xconn.securehome.models.DeviceEnergy;
import io.xconn.securehome.utils.EnergyAnalyticsGenerator;

/**
 * Service to manage energy analytics data and provide real-time updates
 */
public class EnergyAnalyticsService {

    // Singleton instance
    private static EnergyAnalyticsService instance;

    // Data storage
    private final Map<Integer, List<DeviceEnergy>> deviceEnergyMap = new HashMap<>();
    private final Map<Integer, List<Device>> homeDevicesMap = new HashMap<>();

    // For real-time updates
    private final Handler updateHandler;
    private final Map<Integer, EnergyUpdateCallback> updateCallbacks = new HashMap<>();
    private static final long UPDATE_INTERVAL = 5000; // 5 seconds

    // Energy cost rate ($ per kWh)
    private double energyRate = 0.14;

    /**
     * Interface for energy data update callbacks
     */
    public interface EnergyUpdateCallback {
        void onEnergyDataUpdated(List<DeviceEnergy> updatedDevices,
                                 double totalWattage,
                                 double totalUnits,
                                 double totalCost);
    }

    /**
     * Private constructor for singleton pattern
     */
    private EnergyAnalyticsService() {
        updateHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * Get the singleton instance
     * @return EnergyAnalyticsService instance
     */
    public static synchronized EnergyAnalyticsService getInstance() {
        if (instance == null) {
            instance = new EnergyAnalyticsService();
        }
        return instance;
    }

    /**
     * Generate energy data for a list of devices
     * @param homeId ID of the home
     * @param devices List of devices
     * @return List of device energy data
     */
    public List<DeviceEnergy> generateEnergyDataForDevices(int homeId, List<Device> devices) {
        // Store the devices for this home
        homeDevicesMap.put(homeId, new ArrayList<>(devices));

        // Generate energy data
        List<DeviceEnergy> energyData = EnergyAnalyticsGenerator.generateDeviceEnergyList(devices);

        // Cache the energy data
        deviceEnergyMap.put(homeId, energyData);

        return energyData;
    }

    /**
     * Get energy data for a specific home
     * @param homeId ID of the home
     * @return List of device energy data or null if not found
     */
    public List<DeviceEnergy> getEnergyDataForHome(int homeId) {
        return deviceEnergyMap.get(homeId);
    }

    /**
     * Simulate a refresh of energy data (add small fluctuations)
     * @param homeId ID of the home
     * @return Updated list of device energy data
     */
    public synchronized List<DeviceEnergy> refreshEnergyData(int homeId) {
        List<DeviceEnergy> energyData = deviceEnergyMap.get(homeId);

        if (energyData == null) {
            // If no data exists yet for this home
            return new ArrayList<>();
        }

        // Add fluctuations to each device
        for (DeviceEnergy deviceEnergy : energyData) {
            EnergyAnalyticsGenerator.generateFluctuation(deviceEnergy, 5.0);
        }

        // Calculate aggregated metrics
        double totalWattage = 0;
        double totalUnits = 0;

        for (DeviceEnergy deviceEnergy : energyData) {
            totalWattage += deviceEnergy.getWattage();
            totalUnits += deviceEnergy.getUnits();
        }

        double totalCost = EnergyAnalyticsGenerator.calculateCost(totalUnits, energyRate);

        // Call the callback if registered
        if (updateCallbacks.containsKey(homeId)) {
            updateCallbacks.get(homeId).onEnergyDataUpdated(
                    energyData, totalWattage, totalUnits, totalCost);
        }

        return energyData;
    }

    /**
     * Start real-time energy monitoring for a home
     * @param homeId ID of the home to monitor
     * @param callback Callback to receive updates
     */
    public void startRealTimeMonitoring(int homeId, EnergyUpdateCallback callback) {
        // Register the callback
        updateCallbacks.put(homeId, callback);

        // Schedule periodic updates
        updateHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                refreshEnergyData(homeId);

                // Schedule next update if callback still registered
                if (updateCallbacks.containsKey(homeId)) {
                    updateHandler.postDelayed(this, UPDATE_INTERVAL);
                }
            }
        }, UPDATE_INTERVAL);
    }

    /**
     * Stop real-time energy monitoring for a home
     * @param homeId ID of the home to stop monitoring
     */
    public void stopRealTimeMonitoring(int homeId) {
        updateCallbacks.remove(homeId);
    }

    /**
     * Stop all monitoring and clean up resources
     */
    public void shutdown() {
        updateCallbacks.clear();
        updateHandler.removeCallbacksAndMessages(null);
    }

    /**
     * Set the energy rate ($ per kWh)
     * @param rate New rate
     */
    public void setEnergyRate(double rate) {
        if (rate > 0) {
            this.energyRate = rate;
        }
    }

    /**
     * Get the current energy rate
     * @return Current rate in $ per kWh
     */
    public double getEnergyRate() {
        return energyRate;
    }

    /**
     * Calculate total energy metrics for a home
     * @param homeId ID of the home
     * @return Map containing totalWattage, totalUnits, and totalCost
     */
    public Map<String, Double> calculateTotalEnergyMetrics(int homeId) {
        Map<String, Double> metrics = new HashMap<>();
        List<DeviceEnergy> energyData = deviceEnergyMap.get(homeId);

        if (energyData == null || energyData.isEmpty()) {
            metrics.put("totalWattage", 0.0);
            metrics.put("totalUnits", 0.0);
            metrics.put("totalCost", 0.0);
            return metrics;
        }

        double totalWattage = 0;
        double totalUnits = 0;

        for (DeviceEnergy deviceEnergy : energyData) {
            totalWattage += deviceEnergy.getWattage();
            totalUnits += deviceEnergy.getUnits();
        }

        double totalCost = EnergyAnalyticsGenerator.calculateCost(totalUnits, energyRate);

        metrics.put("totalWattage", totalWattage);
        metrics.put("totalUnits", totalUnits);
        metrics.put("totalCost", totalCost);

        return metrics;
    }
}