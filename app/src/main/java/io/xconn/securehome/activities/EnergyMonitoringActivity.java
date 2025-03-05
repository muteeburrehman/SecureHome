package io.xconn.securehome.activities;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.util.ArrayList;
import java.util.Random;

import io.xconn.securehome.R;

public class EnergyMonitoringActivity extends AppCompatActivity {
    private LineChart chartEnergyUsage;
    private TextView tvEnergyUsage, tvEnergyCost;
    private Button btnRefresh;

    private Handler handler;
    private static final int MAX_DATA_POINTS = 20;
    private int xAxis = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_energy_monitoring);

        // Initialize Views
        chartEnergyUsage = findViewById(R.id.chartEnergyUsage);
        tvEnergyUsage = findViewById(R.id.tvEnergyUsage);
        tvEnergyCost = findViewById(R.id.tvEnergyCost);
        btnRefresh = findViewById(R.id.btnRefresh);

        // Setup Graph
        setupEnergyGraph();

        // Start Real-time Monitoring
        startRealTimeMonitoring();

        // Refresh Button
        btnRefresh.setOnClickListener(v -> updateEnergyMetrics());
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

        // Configure Left Y Axis
        YAxis leftAxis = chartEnergyUsage.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setAxisMinimum(0f);

        // Hide Right Y Axis
        chartEnergyUsage.getAxisRight().setEnabled(false);

        // Configure Legend
        Legend legend = chartEnergyUsage.getLegend();
        legend.setForm(Legend.LegendForm.LINE);
        legend.setTextColor(Color.BLACK);
    }

    private void startRealTimeMonitoring() {
        handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                updateEnergyMetrics();
                handler.postDelayed(this, 5000); // Update every 5 seconds
            }
        });
    }

    private void updateEnergyMetrics() {
        // Simulate energy metrics
        double energyConsumption = generateRealisticMetric(200, 400);
        double energyCost = generateRealisticMetric(30, 60);

        // Update TextViews
        tvEnergyUsage.setText(String.format("Current Usage: %.1f kWh", energyConsumption));
        tvEnergyCost.setText(String.format("Estimated Cost: $%.2f", energyCost));

        // Update Chart
        updateChartData(energyConsumption, energyCost);
    }

    private void updateChartData(double energyConsumption, double energyCost) {
        // Energy Consumption Series
        LineDataSet consumptionDataSet = getOrCreateDataSet(
                chartEnergyUsage.getData(),
                "Energy Consumption (kWh)",
                Color.BLUE
        );
        consumptionDataSet.addEntry(new Entry(xAxis, (float) energyConsumption));

        // Energy Cost Series
        LineDataSet costDataSet = getOrCreateDataSet(
                chartEnergyUsage.getData(),
                "Energy Cost ($)",
                Color.RED
        );
        costDataSet.addEntry(new Entry(xAxis, (float) energyCost));

        // Manage data points
        if (consumptionDataSet.getEntryCount() > MAX_DATA_POINTS) {
            consumptionDataSet.removeFirst();
            costDataSet.removeFirst();
        }

        // Update chart data
        LineData lineData = chartEnergyUsage.getData();
        if (lineData == null) {
            lineData = new LineData();
            chartEnergyUsage.setData(lineData);
        }

        lineData.notifyDataChanged();
        chartEnergyUsage.notifyDataSetChanged();
        chartEnergyUsage.invalidate();

        xAxis++;
    }

    private LineDataSet getOrCreateDataSet(LineData data, String label, int color) {
        ILineDataSet existingDataSet = data != null ? data.getDataSetByLabel(label, false) : null;

        if (existingDataSet == null) {
            LineDataSet dataSet = new LineDataSet(new ArrayList<>(), label);
            dataSet.setColor(color);
            dataSet.setDrawCircles(true);
            dataSet.setDrawValues(false);
            dataSet.setLineWidth(2f);
            dataSet.setCircleColor(color);
            dataSet.setCircleRadius(4f);

            if (data != null) {
                data.addDataSet(dataSet);
            }
            return dataSet;
        }

        return (LineDataSet) existingDataSet;
    }

    private double generateRealisticMetric(double min, double max) {
        return min + (max - min) * new Random().nextDouble();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
    }
}