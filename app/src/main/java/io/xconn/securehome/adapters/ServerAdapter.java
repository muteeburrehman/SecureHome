package io.xconn.securehome.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import io.xconn.securehome.R;
import io.xconn.securehome.models.ServerInfo;

public class ServerAdapter extends RecyclerView.Adapter<ServerAdapter.ServerViewHolder> {
    private List<ServerInfo> servers;
    private ServerSelectionListener listener;

    public interface ServerSelectionListener {
        void onServerSelected(ServerInfo server);
    }

    public ServerAdapter(List<ServerInfo> servers, ServerSelectionListener listener) {
        this.servers = servers;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ServerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_server, parent, false);
        return new ServerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ServerViewHolder holder, int position) {
        ServerInfo server = servers.get(position);
        holder.serverName.setText(server.getHostname());
        holder.serverAddress.setText(server.getIpAddress() + ":" + server.getPort());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onServerSelected(server);
            }
        });
    }

    @Override
    public int getItemCount() {
        return servers.size();
    }

    static class ServerViewHolder extends RecyclerView.ViewHolder {
        TextView serverName;
        TextView serverAddress;

        public ServerViewHolder(@NonNull View itemView) {
            super(itemView);
            serverName = itemView.findViewById(R.id.server_name);
            serverAddress = itemView.findViewById(R.id.server_address);
        }
    }
}