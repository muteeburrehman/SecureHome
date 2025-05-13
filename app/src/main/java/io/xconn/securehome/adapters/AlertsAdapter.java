package io.xconn.securehome.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

import io.xconn.securehome.R;
import io.xconn.securehome.models.AlertModel;

/**
 * Adapter for displaying alerts in a RecyclerView
 */
public class AlertsAdapter extends RecyclerView.Adapter<AlertsAdapter.AlertViewHolder> {

    private final List<AlertModel> alertsList;
    private final Context context;
    private final AlertInteractionListener listener;

    public interface AlertInteractionListener {
        void onAlertClicked(AlertModel alert, int position);
        void onViewDetailsClicked(AlertModel alert, int position);
        void onDismissClicked(AlertModel alert, int position);
    }

    public AlertsAdapter(Context context, List<AlertModel> alertsList, AlertInteractionListener listener) {
        this.context = context;
        this.alertsList = alertsList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AlertViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_alert, parent, false);
        return new AlertViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AlertViewHolder holder, int position) {
        AlertModel alert = alertsList.get(position);

        // Set alert details
        holder.alertIconImageView.setImageResource(alert.getIconResourceId());
        holder.alertTypeTextView.setText(alert.getTypeDisplayName());
        holder.alertTimeTextView.setText(alert.getTimeAgo());
        holder.alertTitleTextView.setText(alert.getTitle());
        holder.alertDescriptionTextView.setText(alert.getDescription());

        // Show/hide unread indicator
        holder.unreadIndicator.setVisibility(alert.isRead() ? View.INVISIBLE : View.VISIBLE);

        // Style based on alert type
        styleAlertCard(holder, alert);

        // Set click listeners
        holder.alertCardView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAlertClicked(alert, position);
            }
        });

        holder.detailsButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onViewDetailsClicked(alert, position);
            }
        });

        holder.dismissButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDismissClicked(alert, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return alertsList != null ? alertsList.size() : 0;
    }

    /**
     * Apply styling to the alert card based on alert type
     */
    private void styleAlertCard(AlertViewHolder holder, AlertModel alert) {
        int strokeColor;

        switch (alert.getType()) {
            case AlertModel.TYPE_FIRE_GAS:
                strokeColor = context.getResources().getColor(R.color.dangerColor);
                holder.alertTypeTextView.setTextColor(context.getResources().getColor(R.color.dangerColor));
                break;
            case AlertModel.TYPE_SECURITY:
                strokeColor = context.getResources().getColor(R.color.colorAccent);
                holder.alertTypeTextView.setTextColor(context.getResources().getColor(R.color.colorAccent));
                break;
            default:
                strokeColor = context.getResources().getColor(R.color.strokecol);
                holder.alertTypeTextView.setTextColor(context.getResources().getColor(R.color.colorAccent));
                break;
        }

        holder.alertCardView.setStrokeColor(strokeColor);
    }

    /**
     * Add a new alert to the list and notify adapter
     */
    public void addAlert(AlertModel alert) {
        alertsList.add(0, alert); // Add to the beginning of the list
        notifyItemInserted(0);
    }

    /**
     * Update an existing alert
     */
    public void updateAlert(AlertModel updatedAlert, int position) {
        alertsList.set(position, updatedAlert);
        notifyItemChanged(position);
    }

    /**
     * Remove an alert
     */
    public void removeAlert(int position) {
        alertsList.remove(position);
        notifyItemRemoved(position);
    }

    /**
     * Mark an alert as read
     */
    public void markAsRead(int position) {
        AlertModel alert = alertsList.get(position);
        alert.setRead(true);
        notifyItemChanged(position);
    }

    /**
     * Mark all alerts as read
     */
    public void markAllAsRead() {
        for (int i = 0; i < alertsList.size(); i++) {
            alertsList.get(i).setRead(true);
        }
        notifyDataSetChanged();
    }

    /**
     * ViewHolder class for alert items
     */
    static class AlertViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView alertCardView;
        ImageView alertIconImageView;
        TextView alertTypeTextView;
        TextView alertTimeTextView;
        TextView alertTitleTextView;
        TextView alertDescriptionTextView;
        MaterialButton detailsButton;
        MaterialButton dismissButton;
        View unreadIndicator;

        AlertViewHolder(@NonNull View itemView) {
            super(itemView);
            alertCardView = itemView.findViewById(R.id.alertCardView);
            alertIconImageView = itemView.findViewById(R.id.alertIconImageView);
            alertTypeTextView = itemView.findViewById(R.id.alertTypeTextView);
            alertTimeTextView = itemView.findViewById(R.id.alertTimeTextView);
            alertTitleTextView = itemView.findViewById(R.id.alertTitleTextView);
            alertDescriptionTextView = itemView.findViewById(R.id.alertDescriptionTextView);
            detailsButton = itemView.findViewById(R.id.detailsButton);
            dismissButton = itemView.findViewById(R.id.dismissButton);
            unreadIndicator = itemView.findViewById(R.id.unreadIndicator);
        }
    }
}