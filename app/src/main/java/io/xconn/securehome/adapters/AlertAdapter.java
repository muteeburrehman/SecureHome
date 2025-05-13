package io.xconn.securehome.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;

import java.util.List;

import io.xconn.securehome.R;
import io.xconn.securehome.models.AlertModel;

/**
 * Adapter for displaying alert items in a RecyclerView
 */
public class AlertAdapter extends RecyclerView.Adapter<AlertAdapter.AlertViewHolder> {

    private final Context context;
    private final List<AlertModel> alertsList;

    public AlertAdapter(Context context, List<AlertModel> alertsList) {
        this.context = context;
        this.alertsList = alertsList;
    }

    @NonNull
    @Override
    public AlertViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_alert, parent, false);
        return new AlertViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AlertViewHolder holder, int position) {
        AlertModel alert = alertsList.get(position);

        // Set alert details
        holder.tvTitle.setText(alert.getTitle());
        holder.tvDescription.setText(alert.getDescription());
        holder.tvUser.setText("Reported by: " + alert.getUserName());
        holder.tvTime.setText(alert.getFormattedTimestamp());

        // Set alert type chip
        String typeText = "Unknown";
        int chipBackgroundColor;

        switch (alert.getType()) {
            case AlertModel.TYPE_EMERGENCY:
                typeText = "Emergency";
                chipBackgroundColor = context.getResources().getColor(R.color.dangerColor);
                break;
            case AlertModel.TYPE_SECURITY:
                typeText = "Security";
                chipBackgroundColor = context.getResources().getColor(R.color.colorWarning);
                break;
            case AlertModel.TYPE_SYSTEM:
                typeText = "System";
                chipBackgroundColor = context.getResources().getColor(R.color.colorInfo);
                break;
            default:
                chipBackgroundColor = context.getResources().getColor(R.color.colorInfo);
                break;
        }

        holder.chipType.setText(typeText);
        holder.chipType.setChipBackgroundColorResource(getColorResourceIdForChip(alert.getType()));

        // Set priority indication using MaterialCardView's stroke properties
        if (AlertModel.PRIORITY_HIGH.equals(alert.getPriority())) {
            holder.cardView.setStrokeColor(context.getResources().getColor(R.color.dangerColor));
            holder.cardView.setStrokeWidth(2);
        } else {
            holder.cardView.setStrokeWidth(0);
        }

        // Set resolution status
        holder.chipResolved.setVisibility(alert.isResolved() ? View.VISIBLE : View.GONE);

        // Set alert icon based on type
        switch (alert.getType()) {
            case AlertModel.TYPE_EMERGENCY:
                holder.ivAlertIcon.setImageResource(R.drawable.alerts);
                break;
            case AlertModel.TYPE_SECURITY:
                holder.ivAlertIcon.setImageResource(R.drawable.secure_home);
                break;
            case AlertModel.TYPE_SYSTEM:
                holder.ivAlertIcon.setImageResource(android.R.drawable.ic_dialog_info);
                break;
            default:
                holder.ivAlertIcon.setImageResource(R.drawable.alerts);
                break;
        }
    }

    private int getColorResourceIdForChip(String alertType) {
        switch (alertType) {
            case AlertModel.TYPE_EMERGENCY:
                return R.color.dangerColor;
            case AlertModel.TYPE_SECURITY:
                return R.color.colorWarning;
            case AlertModel.TYPE_SYSTEM:
                return R.color.colorInfo;
            default:
                return R.color.colorInfo;
        }
    }

    @Override
    public int getItemCount() {
        return alertsList.size();
    }

    static class AlertViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDescription, tvUser, tvTime;
        Chip chipType, chipResolved;
        ImageView ivAlertIcon;
        MaterialCardView cardView;  // Changed from CardView to MaterialCardView

        public AlertViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_alert_title);
            tvDescription = itemView.findViewById(R.id.tv_alert_description);
            tvUser = itemView.findViewById(R.id.tv_alert_user);
            tvTime = itemView.findViewById(R.id.tv_alert_time);
            chipType = itemView.findViewById(R.id.chip_alert_type);
            chipResolved = itemView.findViewById(R.id.chip_resolved);
            ivAlertIcon = itemView.findViewById(R.id.iv_alert_icon);
            cardView = itemView.findViewById(R.id.card_view);
        }
    }
}