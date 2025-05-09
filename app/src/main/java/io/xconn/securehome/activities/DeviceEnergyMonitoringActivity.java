package io.xconn.securehome.activities;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.xconn.securehome.R;
import io.xconn.securehome.adapters.DeviceEnergyAdapter;
import io.xconn.securehome.api.ApiService;
import io.xconn.securehome.api.RetrofitClient;
import io.xconn.securehome.models.Device;
import io.xconn.securehome.models.DeviceEnergy;
import io.xconn.securehome.models.Home;
import io.xconn.securehome.services.EnergyAnalyticsService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DeviceEnergyMonitoringActivity extends AppCompatActivity
        implements EnergyAnalyticsService.EnergyUpdateCallback {

    private static final String TAG = "DeviceEnergyMonitoring";
    private LineChart chartEnergyUsage;
    private TextView tvTotalEnergy, tvTotalCost, tvTotalWattage;
    private Button btnRefresh;
    private Spinner spinnerHomes;
    private RecyclerView recyclerDevices;
    private Toolbar toolbar;

    private static final int MAX_DATA_POINTS = 20;
    private int xAxisValue = 0;
    private List<String> xAxisLabels = new ArrayList<>();

    private ApiService apiService;
    private EnergyAnalyticsService energyService;
    private List<Home> homes = new ArrayList<>();
    private List<Device> devices = new ArrayList<>();
    private List<DeviceEnergy> deviceEnergyList = new ArrayList<>();
    private DeviceEnergyAdapter deviceEnergyAdapter;

    private int selectedHomeId = -1;
    private boolean isMonitoringActive = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_energy_monitoring);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimary));


        // Initialize API
        apiService = RetrofitClient.getInstance(this).getApi();
        energyService = EnergyAnalyticsService.getInstance();

        // Initialize Views
        initializeViews();
        setupToolbar();

        // Setup RecyclerView
        setupRecyclerView();

        // Setup Graph
        setupEnergyGraph();

        // Load Homes
        loadHomes();

        // Refresh Button
        btnRefresh.setOnClickListener(v -> {
            if (selectedHomeId != -1) {
                refreshEnergyData();
            } else {
                Toast.makeText(this, "Please select a home first", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupToolbar() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setTitle("Energy Monitoring");
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initializeViews() {
        chartEnergyUsage = findViewById(R.id.chartEnergyUsage);
        tvTotalEnergy = findViewById(R.id.tvTotalEnergy);
        tvTotalCost = findViewById(R.id.tvTotalCost);
        tvTotalWattage = findViewById(R.id.tvTotalWattage);
        btnRefresh = findViewById(R.id.btnRefresh);
        spinnerHomes = findViewById(R.id.spinnerHomes);
        recyclerDevices = findViewById(R.id.recyclerDevices);
    }

    private void setupRecyclerView() {
        recyclerDevices.setLayoutManager(new LinearLayoutManager(this));
        deviceEnergyAdapter = new DeviceEnergyAdapter(deviceEnergyList);
        recyclerDevices.setAdapter(deviceEnergyAdapter);
    }

    private void setupEnergyGraph() {
        // Disable description
        chartEnergyUsage.getDescription().setEnabled(false);

        // Enable touch gestures
        chartEnergyUsage.setTouchEnabled(true);
        chartEnergyUsage.setDragEnabled(true);
        chartEnergyUsage.setScaleEnabled(true);

        // Configure X Axis
        XAxis xAxis = chartEnergyUsage.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(Color.BLACK);
        xAxis.setGranularity(1f);

        // Configure Left Y Axis
        YAxis leftAxis = chartEnergyUsage.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setTextColor(Color.BLACK);

        // Configure Right Y Axis
        YAxis rightAxis = chartEnergyUsage.getAxisRight();
        rightAxis.setEnabled(false);

        // Configure Legend
        Legend legend = chartEnergyUsage.getLegend();
        legend.setTextColor(Color.BLACK);
        legend.setForm(Legend.LegendForm.LINE);

        // Initial empty data
        LineData data = new LineData();
        chartEnergyUsage.setData(data);
        chartEnergyUsage.invalidate();
    }

    private void loadHomes() {
        apiService.getHomes().enqueue(new Callback<List<Home>>() {
            @Override
            public void onResponse(Call<List<Home>> call, Response<List<Home>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    homes = response.body();
                    setupHomeSpinner();
                } else {
                    Toast.makeText(DeviceEnergyMonitoringActivity.this,
                            "Failed to load homes", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Home>> call, Throwable t) {
                Toast.makeText(DeviceEnergyMonitoringActivity.this,
                        "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Failed to load homes", t);
            }
        });
    }

    private void setupHomeSpinner() {
        List<String> homeNames = new ArrayList<>();
        for (Home home : homes) {
            homeNames.add(home.getOwner() + "'s Home");
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, homeNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerHomes.setAdapter(adapter);

        spinnerHomes.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Stop existing monitoring if active
                if (selectedHomeId != -1 && isMonitoringActive) {
                    energyService.stopRealTimeMonitoring(selectedHomeId);
                    isMonitoringActive = false;
                }

                selectedHomeId = homes.get(position).getId();
                loadDevicesForHome(selectedHomeId);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedHomeId = -1;
            }
        });
    }

    private void loadDevicesForHome(int homeId) {
        apiService.getDevices(homeId).enqueue(new Callback<List<Device>>() {
            @Override
            public void onResponse(Call<List<Device>> call, Response<List<Device>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    devices = response.body();
                    generateInitialEnergyData();
                    startRealTimeMonitoring();
                } else {
                    Toast.makeText(DeviceEnergyMonitoringActivity.this,
                            "Failed to load devices", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Device>> call, Throwable t) {
                Toast.makeText(DeviceEnergyMonitoringActivity.this,
                        "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Failed to load devices", t);
            }
        });
    }

    private void generateInitialEnergyData() {
        // Generate initial energy data for devices
        deviceEnergyList.clear(); // Clear existing data first
        deviceEnergyList.addAll(energyService.generateEnergyDataForDevices(selectedHomeId, devices));

        // Update the adapter with new data
        deviceEnergyAdapter.notifyDataSetChanged();

        // Calculate and display total metrics
        updateTotalMetrics();

        // Reset chart
        resetChart();

        // Initialize chart with first data point
        addDataPointToChart();
    }

    private void resetChart() {
        // Clear existing data
        xAxisValue = 0;
        xAxisLabels.clear();

        LineData data = new LineData();
        chartEnergyUsage.setData(data);
        chartEnergyUsage.invalidate();
    }

    private void startRealTimeMonitoring() {
        // Stop any previous monitoring
        if (isMonitoringActive) {
            energyService.stopRealTimeMonitoring(selectedHomeId);
        }

        // Start monitoring for the selected home
        energyService.startRealTimeMonitoring(selectedHomeId, this);
        isMonitoringActive = true;

        Log.d(TAG, "Started real-time monitoring for home ID: " + selectedHomeId);
    }

    private void refreshEnergyData() {
        if (selectedHomeId != -1) {
            deviceEnergyList.clear(); // Clear existing data first
            deviceEnergyList.addAll(energyService.refreshEnergyData(selectedHomeId));
            deviceEnergyAdapter.notifyDataSetChanged();
            addDataPointToChart();
            updateTotalMetrics();
        }
    }

    private void updateTotalMetrics() {
        Map<String, Double> metrics = energyService.calculateTotalEnergyMetrics(selectedHomeId);

        double totalWattage = metrics.get("totalWattage");
        double totalUnits = metrics.get("totalUnits");
        double totalCost = metrics.get("totalCost");

        tvTotalWattage.setText(String.format(Locale.US, "Total Power: %.1f W", totalWattage));
        tvTotalEnergy.setText(String.format(Locale.US, "Total Consumption: %.2f kWh", totalUnits));
        tvTotalCost.setText(String.format(Locale.US, "Estimated Cost: $%.2f", totalCost));
    }

    private void addDataPointToChart() {
        LineData data = chartEnergyUsage.getData();

        if (data == null) {
            data = new LineData();
            chartEnergyUsage.setData(data);
        }

        ILineDataSet set = data.getDataSetByIndex(0);

        // If dataset doesn't exist, create it
        if (set == null) {
            set = createSet();
            data.addDataSet(set);
        }

        // Calculate total wattage
        double totalWattage = 0;
        for (DeviceEnergy device : deviceEnergyList) {
            totalWattage += device.getWattage();
        }

        // Add new data point
        data.addEntry(new Entry(xAxisValue, (float) totalWattage), 0);

        // Add time label
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        String timeLabel = sdf.format(new Date());
        xAxisLabels.add(timeLabel);

        // Keep only MAX_DATA_POINTS
        if (set.getEntryCount() > MAX_DATA_POINTS) {
            data.removeEntry(0, 0);
            xAxisLabels.remove(0);
        }

        // Update X-axis labels
        chartEnergyUsage.getXAxis().setValueFormatter(new IndexAxisValueFormatter(xAxisLabels));

        // Notify chart data has changed
        data.notifyDataChanged();
        chartEnergyUsage.notifyDataSetChanged();
        chartEnergyUsage.setVisibleXRangeMaximum(MAX_DATA_POINTS);
        chartEnergyUsage.moveViewToX(data.getEntryCount());

        // Increment x-axis value
        xAxisValue++;
    }

    private LineDataSet createSet() {
        LineDataSet set = new LineDataSet(null, "Power Usage (W)");
        set.setColor(Color.BLUE);
        set.setLineWidth(2f);
        set.setCircleColor(Color.BLUE);
        set.setCircleRadius(4f);
        set.setFillColor(Color.BLUE);
        set.setDrawValues(false);
        set.setHighlightEnabled(true);
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        return set;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (selectedHomeId != -1 && isMonitoringActive) {
            Log.d(TAG, "onPause: Stopping real-time monitoring");
            energyService.stopRealTimeMonitoring(selectedHomeId);
            isMonitoringActive = false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (selectedHomeId != -1) {
            Log.d(TAG, "onResume: Resuming real-time monitoring");
            energyService.startRealTimeMonitoring(selectedHomeId, this);
            isMonitoringActive = true;
        }
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy: Cleaning up resources");
        if (selectedHomeId != -1 && isMonitoringActive) {
            energyService.stopRealTimeMonitoring(selectedHomeId);
            isMonitoringActive = false;
        }
        super.onDestroy();
    }

    @Override
    public void onEnergyDataUpdated(List<DeviceEnergy> updatedDevices,
                                    double totalWattage,
                                    double totalUnits,
                                    double totalCost) {
        runOnUiThread(() -> {
            Log.d(TAG, "Energy data updated - devices: " + updatedDevices.size() +
                    ", totalWattage: " + totalWattage);

            // Update device list
            deviceEnergyList.clear();
            deviceEnergyList.addAll(updatedDevices);
            deviceEnergyAdapter.notifyDataSetChanged();

            // Update total metrics
            tvTotalWattage.setText(String.format(Locale.US, "Total Power: %.1f W", totalWattage));
            tvTotalEnergy.setText(String.format(Locale.US, "Total Consumption: %.2f kWh", totalUnits));
            tvTotalCost.setText(String.format(Locale.US, "Estimated Cost: $%.2f", totalCost));

            // Add new data point to chart
            addDataPointToChart();
        });
    }
}