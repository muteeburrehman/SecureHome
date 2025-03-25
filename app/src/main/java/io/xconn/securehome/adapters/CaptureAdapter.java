package io.xconn.securehome.adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import io.xconn.securehome.R;
import io.xconn.securehome.models.CaptureModel;

public class CaptureAdapter extends RecyclerView.Adapter<CaptureAdapter.CaptureViewHolder> {

    private List<CaptureModel> captures = new ArrayList<>();
    private OnCaptureClickListener listener;

    public CaptureAdapter() {
    }

    public void setCaptures(List<CaptureModel> captures) {
        this.captures = captures;
        notifyDataSetChanged();
    }

    public void setOnCaptureClickListener(OnCaptureClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public CaptureViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_capture, parent, false);
        return new CaptureViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CaptureViewHolder holder, int position) {
        CaptureModel capture = captures.get(position);

        // Load image thumbnail
        Bitmap bitmap = BitmapFactory.decodeFile(capture.getImagePath());
        if (bitmap != null) {
            holder.imageView.setImageBitmap(bitmap);
        }

        // Set timestamp
        holder.timestampText.setText(capture.getTimestamp());

        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCaptureClick(capture);
            }
        });
    }

    @Override
    public int getItemCount() {
        return captures.size();
    }

    public static class CaptureViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView timestampText;

        public CaptureViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.captureImageView);
            timestampText = itemView.findViewById(R.id.captureTimestampText);
        }
    }

    public interface OnCaptureClickListener {
        void onCaptureClick(CaptureModel capture);
    }
}