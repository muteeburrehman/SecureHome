package io.xconn.securehome.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import io.xconn.securehome.R;
import io.xconn.securehome.models.UserModel;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private Context context;
    private List<UserModel> userList;
    private OnUserActionListener listener;

    public interface OnUserActionListener {
        void onApproveClick(UserModel user, int position);
        void onRejectClick(UserModel user, int position);
        void onRemoveClick(UserModel user, int position);
    }

    public UserAdapter(Context context, List<UserModel> userList, OnUserActionListener listener) {
        this.context = context;
        this.userList = userList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        UserModel user = userList.get(position);

        holder.nameText.setText(user.getDisplayName());
        holder.emailText.setText(user.getEmail());

        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
        String date = sdf.format(new Date(user.getCreatedAt()));
        holder.dateText.setText("Registered: " + date);

        // Show/hide buttons based on user status
        if (user.getApprovalStatus().equals(UserModel.STATUS_PENDING)) {
            holder.approveButton.setVisibility(View.VISIBLE);
            holder.rejectButton.setVisibility(View.VISIBLE);
            holder.removeButton.setVisibility(View.GONE);
            holder.statusText.setText("Status: Pending");
            holder.statusText.setTextColor(context.getResources().getColor(R.color.colorWarning));
        } else if (user.getApprovalStatus().equals(UserModel.STATUS_APPROVED)) {
            holder.approveButton.setVisibility(View.GONE);
            holder.rejectButton.setVisibility(View.GONE);
            holder.removeButton.setVisibility(View.VISIBLE);
            holder.statusText.setText("Status: Approved");
            holder.statusText.setTextColor(context.getResources().getColor(R.color.colorSuccess));
        } else if (user.getApprovalStatus().equals(UserModel.STATUS_REJECTED)) {
            holder.approveButton.setVisibility(View.VISIBLE);
            holder.rejectButton.setVisibility(View.GONE);
            holder.removeButton.setVisibility(View.VISIBLE);
            holder.statusText.setText("Status: Rejected");
            holder.statusText.setTextColor(context.getResources().getColor(R.color.red_active));
        }

        // Hide actions if it's an admin user
        if (user.isAdmin()) {
            holder.approveButton.setVisibility(View.GONE);
            holder.rejectButton.setVisibility(View.GONE);
            holder.removeButton.setVisibility(View.GONE);
            holder.statusText.setText("Role: Admin");
            holder.statusText.setTextColor(context.getResources().getColor(R.color.colorAccent));
        }

        // Set up click listeners
        holder.approveButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onApproveClick(user, position);
            }
        });

        holder.rejectButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRejectClick(user, position);
            }
        });

        holder.removeButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRemoveClick(user, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public void updateUserList(List<UserModel> newUserList) {
        this.userList = newUserList;
        notifyDataSetChanged();
    }

    public void updateUserStatus(int position, String newStatus) {
        if (position >= 0 && position < userList.size()) {
            UserModel user = userList.get(position);
            user.setApprovalStatus(newStatus);
            notifyItemChanged(position);
        }
    }

    public void removeUser(int position) {
        if (position >= 0 && position < userList.size()) {
            userList.remove(position);
            notifyItemRemoved(position);
        }
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView nameText, emailText, dateText, statusText;
        Button approveButton, rejectButton, removeButton;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.user_name);
            emailText = itemView.findViewById(R.id.user_email);
            dateText = itemView.findViewById(R.id.user_date);
            statusText = itemView.findViewById(R.id.user_status);
            approveButton = itemView.findViewById(R.id.btn_approve);
            rejectButton = itemView.findViewById(R.id.btn_reject);
            removeButton = itemView.findViewById(R.id.btn_remove);
        }
    }
}