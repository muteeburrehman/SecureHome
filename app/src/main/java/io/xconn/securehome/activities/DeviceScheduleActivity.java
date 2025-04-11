package io.xconn.securehome.activities;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import io.xconn.securehome.R;
import io.xconn.securehome.adapters.ScheduleAdapter;
import io.xconn.securehome.models.Schedule;
import io.xconn.securehome.repository.ScheduleRepository;

public class DeviceScheduleActivity extends AppCompatActivity implements
        ScheduleAdapter.OnScheduleListener,
        ScheduleRepository.OnScheduleAddedListener,
        ScheduleRepository.OnScheduleDeletedListener {

    private RecyclerView recyclerView;
    private ScheduleAdapter adapter;
    private Button btnAddSchedule;
    private ProgressBar progressBar;
    private TextView tvDeviceName, tvEmptySchedules;
    private ScheduleRepository scheduleRepository;
    private int homeId;
    private int deviceId;
    private String deviceName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_schedule);

        // Get device and home IDs from intent
        homeId = getIntent().getIntExtra("HOME_ID", -1);
        deviceId = getIntent().getIntExtra("DEVICE_ID", -1);
        deviceName = getIntent().getStringExtra("DEVICE_NAME");

        if (homeId == -1 || deviceId == -1) {
            Toast.makeText(this, "Invalid device or home ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        scheduleRepository = new ScheduleRepository(this);

        // Initialize UI components
        tvDeviceName = findViewById(R.id.tvDeviceName);
        recyclerView = findViewById(R.id.recyclerViewSchedules);
        btnAddSchedule = findViewById(R.id.btnAddSchedule);
        progressBar = findViewById(R.id.progressBar);
        tvEmptySchedules = findViewById(R.id.tvEmptySchedules);

        // Set device name text
        tvDeviceName.setText(String.format("Device: %s", deviceName));

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ScheduleAdapter(this);
        recyclerView.setAdapter(adapter);

        // Setup click listener for add schedule button
        btnAddSchedule.setOnClickListener(v -> showAddScheduleDialog());

        // Observe LiveData
        observeViewModel();

        // Initial load
        scheduleRepository.fetchSchedules(homeId, deviceId);
    }

    private void observeViewModel() {
        // Observe schedules
        scheduleRepository.getSchedules().observe(this, deviceSchedule -> {
            if (deviceSchedule != null && deviceSchedule.getSchedules() != null) {
                adapter.setSchedules(deviceSchedule.getSchedules());

                if (deviceSchedule.getSchedules().isEmpty()) {
                    tvEmptySchedules.setVisibility(View.VISIBLE);
                } else {
                    tvEmptySchedules.setVisibility(View.GONE);
                }
            } else {
                adapter.setSchedules(new ArrayList<>());
                tvEmptySchedules.setVisibility(View.VISIBLE);
            }
        });

        // Observe loading state
        scheduleRepository.getIsLoading().observe(this, isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });

        // Observe error messages
        scheduleRepository.getErrorMessage().observe(this, errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });

        // Observe success messages
        scheduleRepository.getSuccessMessage().observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAddScheduleDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_schedule, null);
        TextView tvSelectedTime = dialogView.findViewById(R.id.tvSelectedTime);
        Button btnSelectTime = dialogView.findViewById(R.id.btnSelectTime);
        Switch switchOperation = dialogView.findViewById(R.id.switchOperation);

        // Changed from CheckBox to Chip with updated IDs
        Chip chipMonday = dialogView.findViewById(R.id.chipMonday);
        Chip chipTuesday = dialogView.findViewById(R.id.chipTuesday);
        Chip chipWednesday = dialogView.findViewById(R.id.chipWednesday);
        Chip chipThursday = dialogView.findViewById(R.id.chipThursday);
        Chip chipFriday = dialogView.findViewById(R.id.chipFriday);
        Chip chipSaturday = dialogView.findViewById(R.id.chipSaturday);
        Chip chipSunday = dialogView.findViewById(R.id.chipSunday);

        final String[] selectedTime = {""};

        btnSelectTime.setOnClickListener(v -> {
            Calendar currentTime = Calendar.getInstance();
            int hour = currentTime.get(Calendar.HOUR_OF_DAY);
            int minute = currentTime.get(Calendar.MINUTE);

            TimePickerDialog timePicker = new TimePickerDialog(
                    DeviceScheduleActivity.this,
                    (view, hourOfDay, selectedMinute) -> {
                        selectedTime[0] = String.format("%02d:%02d", hourOfDay, selectedMinute);
                        tvSelectedTime.setText(selectedTime[0]);
                    },
                    hour,
                    minute,
                    true);

            timePicker.show();
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Add Schedule")
                .setView(dialogView)
                .setPositiveButton("Add", (dialogInterface, i) -> {
                    // Get selected days using Chip components
                    List<Integer> selectedDays = new ArrayList<>();
                    if (chipSunday.isChecked()) selectedDays.add(0);
                    if (chipMonday.isChecked()) selectedDays.add(1);
                    if (chipTuesday.isChecked()) selectedDays.add(2);
                    if (chipWednesday.isChecked()) selectedDays.add(3);
                    if (chipThursday.isChecked()) selectedDays.add(4);
                    if (chipFriday.isChecked()) selectedDays.add(5);
                    if (chipSaturday.isChecked()) selectedDays.add(6);

                    // Validate input
                    if (selectedTime[0].isEmpty()) {
                        Toast.makeText(this, "Please select a time", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (selectedDays.isEmpty()) {
                        Toast.makeText(this, "Please select at least one day", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Create and add schedule
                    Schedule schedule = new Schedule(
                            selectedTime[0],
                            switchOperation.isChecked(),
                            selectedDays);

                    scheduleRepository.addSchedule(homeId, deviceId, schedule, this);
                })
                .setNegativeButton("Cancel", null)
                .create();

        dialog.show();
    }

    @Override
    public void onDeleteSchedule(Schedule schedule) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Schedule")
                .setMessage("Are you sure you want to delete this schedule?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    scheduleRepository.deleteSchedule(homeId, deviceId, schedule.getId(), this);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onScheduleAdded(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onScheduleDeleted(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}