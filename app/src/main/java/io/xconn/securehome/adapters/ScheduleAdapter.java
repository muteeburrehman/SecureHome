package io.xconn.securehome.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

import io.xconn.securehome.R;
import io.xconn.securehome.models.Schedule;

public class ScheduleAdapter extends RecyclerView.Adapter<ScheduleAdapter.ScheduleViewHolder> {

    private List<Schedule> schedules;
    private final OnScheduleListener listener;

    public ScheduleAdapter(OnScheduleListener listener) {
        this.listener = listener;
    }

    public void setSchedules(List<Schedule> schedules) {
        this.schedules = schedules;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ScheduleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_schedule, parent, false);
        return new ScheduleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ScheduleViewHolder holder, int position) {
        Schedule schedule = schedules.get(position);

        // Use the renamed method `isOn()`
        String operationText = schedule.isOn() ? "ON" : "OFF";
        String timeText = schedule.getTime();
        String daysText = getDaysString(schedule.getDays());

        holder.tvTime.setText(String.format(Locale.getDefault(), "%s - %s", timeText, operationText));
        holder.tvDays.setText(daysText);

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteSchedule(schedule);
            }
        });
    }

    @Override
    public int getItemCount() {
        return schedules != null ? schedules.size() : 0;
    }

    static class ScheduleViewHolder extends RecyclerView.ViewHolder {
        TextView tvTime, tvDays;
        ImageButton btnDelete;

        public ScheduleViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvDays = itemView.findViewById(R.id.tvDays);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }

    private String getDaysString(List<Integer> days) {
        String[] dayNames = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        StringBuilder sb = new StringBuilder();
        for (Integer day : days) {
            if (day >= 0 && day < 7) {
                sb.append(dayNames[day]).append(" ");
            }
        }
        return sb.toString().trim();
    }

    public interface OnScheduleListener {
        void onDeleteSchedule(Schedule schedule);
    }
}
