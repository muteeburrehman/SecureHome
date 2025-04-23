package io.xconn.securehome.adapters;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import io.xconn.securehome.R;
import io.xconn.securehome.models.Home;

public class HomeAdapter extends RecyclerView.Adapter<HomeAdapter.HomeViewHolder> {
    private List<Home> homes = new ArrayList<>();
    private final OnHomeClickListener listener;

    public HomeAdapter(OnHomeClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public HomeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_home, parent, false);
        return new HomeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HomeViewHolder holder, int position) {
        Home home = homes.get(position);
        holder.bind(home, listener);
    }

    @Override
    public int getItemCount() {
        return homes.size();
    }

    public void setHomes(List<Home> homes) {
        this.homes = homes;
        notifyDataSetChanged();
    }

    public interface OnHomeClickListener {
        void onHomeClick(Home home);
    }

    static class HomeViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvOwner, tvIpAddress, tvDeviceCount;

        public HomeViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOwner = itemView.findViewById(R.id.tvOwner);
            tvIpAddress = itemView.findViewById(R.id.tvIpAddress);
            tvDeviceCount = itemView.findViewById(R.id.tvDeviceCount);
        }

        public void bind(Home home, OnHomeClickListener listener) {
            tvOwner.setText(home.getOwner());
            tvIpAddress.setText(String.format("%s:%s", home.getIpAddress(), home.getPort()));
            // Note: We don't have device count from the API response
            // We could either fetch it separately or hide this view
            tvDeviceCount.setVisibility(View.GONE);

            itemView.setOnClickListener(v -> listener.onHomeClick(home));
        }
    }
}