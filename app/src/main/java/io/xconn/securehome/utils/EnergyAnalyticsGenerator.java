package io.xconn.securehome.utils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import io.xconn.securehome.models.Device;
import io.xconn.securehome.models.DeviceEnergy;

/**
 * Utility class to generate fake energy analytics data
 * for demo and testing purposes
 */
public class EnergyAnalyticsGenerator {

    private static final Random random = new Random();
    private static final Map<String, DeviceEnergyProfile> deviceProfiles = initializeProfiles();

    /**
     * Represents typical usage patterns for different device types
     */
    private static class DeviceEnergyProfile {
        final double minWattage;
        final double maxWattage;
        final double minVoltage;
        final double maxVoltage;
        final double dailyUsageHours;  // Average hours used per day

        DeviceEnergyProfile(double minWattage, double maxWattage,
                            double minVoltage, double maxVoltage,
                            double dailyUsageHours) {
            this.minWattage = minWattage;
            this.maxWattage = maxWattage;
            this.minVoltage = minVoltage;
            this.maxVoltage = maxVoltage;
            this.dailyUsageHours = dailyUsageHours;
        }
    }

    /**
     * Initialize device energy profiles for various device types
     */
    private static Map<String, DeviceEnergyProfile> initializeProfiles() {
        Map<String, DeviceEnergyProfile> profiles = new HashMap<>();

        // Common household devices with realistic power consumption ranges
        profiles.put("light", new DeviceEnergyProfile(5, 60, 110, 125, 5.5));           // LED to incandescent
        profiles.put("outlet", new DeviceEnergyProfile(50, 1800, 110, 125, 8.0));       // Various appliances
        profiles.put("hvac", new DeviceEnergyProfile(500, 3500, 220, 240, 6.0));        // AC/Heating
        profiles.put("water_heater", new DeviceEnergyProfile(1000, 4500, 220, 240, 3.0)); // Water heater
        profiles.put("entertainment", new DeviceEnergyProfile(80, 400, 110, 125, 4.0)); // TV, gaming, etc.
        profiles.put("water_pump", new DeviceEnergyProfile(200, 1200, 110, 240, 2.0));  // Pool or well pump
        profiles.put("thermostat", new DeviceEnergyProfile(5, 15, 24, 28, 24.0));       // Smart thermostat
        profiles.put("refrigerator", new DeviceEnergyProfile(100, 400, 110, 125, 24.0)); // Always on, cycles
        profiles.put("kitchen", new DeviceEnergyProfile(500, 2400, 110, 240, 3.0));     // Kitchen appliances
        profiles.put("laundry", new DeviceEnergyProfile(500, 3000, 220, 240, 1.5));     // Washer/dryer

        // Default for unknown device types
        profiles.put("default", new DeviceEnergyProfile(50, 200, 110, 125, 4.0));

        return profiles;
    }

    /**
     * Generate a single DeviceEnergy object with realistic data based on device type
     * @param device The device to generate energy data for
     * @return DeviceEnergy object with simulated metrics
     */
    public static DeviceEnergy generateDeviceEnergy(Device device) {
        // Get the appropriate profile for this device type or use default
        String deviceType = device.getType();
        if (deviceType == null) {
            deviceType = "default";
        }

        DeviceEnergyProfile profile = deviceProfiles.getOrDefault(deviceType, deviceProfiles.get("default"));

        // Generate base metrics
        double wattage = generateRandomInRange(profile.minWattage, profile.maxWattage);
        double voltage = generateRandomInRange(profile.minVoltage, profile.maxVoltage);
        double current = wattage / voltage; // Amperes = Watts / Volts

        // Calculate monthly consumption - kWh
        // Formula: (Watts × Hours per day × 30 days) ÷ 1000
        double monthlyUnits = (wattage * profile.dailyUsageHours * 30) / 1000.0;

        // Time-of-day adjustments (devices are used differently throughout the day)
        applyTimeOfDayAdjustment(deviceType, Calendar.getInstance(), wattage);

        return new DeviceEnergy(
                device.getId(),
                device.getName(),
                deviceType,
                wattage,
                voltage,
                current,
                monthlyUnits
        );
    }

    /**
     * Generate energy data for multiple devices
     * @param devices List of devices to generate data for
     * @return List of DeviceEnergy objects with simulated data
     */
    public static List<DeviceEnergy> generateDeviceEnergyList(List<Device> devices) {
        List<DeviceEnergy> energyList = new ArrayList<>();

        for (Device device : devices) {
            energyList.add(generateDeviceEnergy(device));
        }

        return energyList;
    }

    /**
     * Apply realistic time-of-day adjustments to power consumption
     * @param deviceType Type of device
     * @param calendar Current time
     * @param wattage Base wattage to adjust
     * @return Adjusted wattage
     */
    private static double applyTimeOfDayAdjustment(String deviceType, Calendar calendar, double wattage) {
        int hourOfDay = calendar.get(Calendar.HOUR_OF_DAY);

        // Default adjustment - no change
        double adjustment = 1.0;

        switch (deviceType) {
            case "light":
                // Lights use more in evening, less during day
                if (hourOfDay >= 18 || hourOfDay <= 6) {
                    adjustment = 1.5; // More usage in evening/night
                } else {
                    adjustment = 0.3; // Less during daylight
                }
                break;

            case "entertainment":
                // Entertainment systems used more in evening
                if (hourOfDay >= 18 && hourOfDay <= 23) {
                    adjustment = 1.8; // Prime time usage
                } else if (hourOfDay >= 0 && hourOfDay <= 6) {
                    adjustment = 0.2; // Minimal overnight
                }
                break;

            case "kitchen":
                // Kitchen appliances - peaks at breakfast, lunch, dinner
                if ((hourOfDay >= 6 && hourOfDay <= 9) ||
                        (hourOfDay >= 11 && hourOfDay <= 13) ||
                        (hourOfDay >= 17 && hourOfDay <= 20)) {
                    adjustment = 1.7; // Meal times
                } else {
                    adjustment = 0.4; // Less use between meals
                }
                break;

            case "hvac":
                // HVAC - varies with outside temperature (assumed to be related to time of day)
                if (hourOfDay >= 12 && hourOfDay <= 17) {
                    adjustment = 1.5; // Peak cooling during hottest part of day
                } else if (hourOfDay >= 0 && hourOfDay <= 5) {
                    adjustment = 0.7; // Reduced overnight
                }
                break;

            // Add other specific device types as needed
        }

        return wattage * adjustment;
    }

    /**
     * Generate random fluctuations in energy metrics to simulate real-time changes
     * @param deviceEnergy The device energy data to fluctuate
     * @param fluctuationPercent Maximum percentage of fluctuation
     * @return Updated DeviceEnergy with fluctuations
     */
    public static DeviceEnergy generateFluctuation(DeviceEnergy deviceEnergy, double fluctuationPercent) {
        // Default 5% fluctuation if not specified
        if (fluctuationPercent <= 0) {
            fluctuationPercent = 5.0;
        }

        double fluctuationFactor = 1.0 + ((random.nextDouble() * 2 * fluctuationPercent) - fluctuationPercent) / 100.0;

        // Fluctuate wattage within the specified percentage
        double newWattage = deviceEnergy.getWattage() * fluctuationFactor;

        // Small voltage fluctuation (voltage is more stable)
        double voltageFluctuation = 1.0 + ((random.nextDouble() * 4.0) - 2.0) / 100.0;
        double newVoltage = deviceEnergy.getVoltage() * voltageFluctuation;

        // Recalculate current based on new wattage and voltage
        double newCurrent = newWattage / newVoltage;

        // Units change very slowly (accumulated over time)
        double unitsIncrement = deviceEnergy.getWattage() * 0.000001 * (1 + random.nextDouble());
        double newUnits = deviceEnergy.getUnits() + unitsIncrement;

        // Update and return
        deviceEnergy.setWattage(newWattage);
        deviceEnergy.setVoltage(newVoltage);
        deviceEnergy.setCurrent(newCurrent);
        deviceEnergy.setUnits(newUnits);

        return deviceEnergy;
    }

    /**
     * Generate a random double within the specified range
     * @param min Minimum value
     * @param max Maximum value
     * @return Random value between min and max
     */
    public static double generateRandomInRange(double min, double max) {
        return min + (max - min) * random.nextDouble();
    }

    /**
     * Calculate estimated cost based on energy consumption and rate
     * @param kWh Energy consumption in kilowatt-hours
     * @param rate Cost per kWh (default: $0.14)
     * @return Estimated cost
     */
    public static double calculateCost(double kWh, double rate) {
        if (rate <= 0) {
            // Use average US electricity cost if not provided
            rate = 0.14;
        }
        return kWh * rate;
    }

    /**
     * Generate historical data points for charting
     * @param deviceType Type of device
     * @param days Number of days of history
     * @param pointsPerDay Data points per day
     * @return Map with timestamps and wattage values
     */
    public static Map<Long, Double> generateHistoricalData(String deviceType, int days, int pointsPerDay) {
        Map<Long, Double> historicalData = new HashMap<>();
        DeviceEnergyProfile profile = deviceProfiles.getOrDefault(deviceType, deviceProfiles.get("default"));

        // Start from days ago
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -days);

        // For each day in history
        for (int day = 0; day < days; day++) {
            // For each point in the day
            for (int point = 0; point < pointsPerDay; point++) {
                // Calculate time for this point
                int hourOfDay = (24 * point) / pointsPerDay;
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                calendar.set(Calendar.MINUTE, (60 * point) % 60);

                // Base wattage for this device type
                double baseWattage = generateRandomInRange(profile.minWattage, profile.maxWattage);

                // Apply time of day patterns
                double adjustedWattage = applyTimeOfDayAdjustment(deviceType, calendar, baseWattage);

                // Add some randomness (±15%)
                double finalWattage = adjustedWattage * (0.85 + random.nextDouble() * 0.3);

                // Store in map with timestamp
                historicalData.put(calendar.getTimeInMillis(), finalWattage);
            }

            // Move to next day
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        return historicalData;
    }
}